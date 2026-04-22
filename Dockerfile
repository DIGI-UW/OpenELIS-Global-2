# syntax=docker/dockerfile:1
#
# OpenELIS Global 2 webapp Dockerfile — consolidated multi-stage.
#
# Stages:
#   build-base   — common Maven + dataexport build
#   build-prod   — production build (spotless gated, no tools/)
#   build-dev    — dev build (spotless skipped, includes tools/)
#   runtime-base — common Tomcat 10 runtime + hardening + entrypoint
#   prod         — (DEFAULT) production runtime; copies WAR from build-prod;
#                  full /var/lib/openelis-global/* scaffolding + oe_server.xml
#   dev          — dev runtime; copies WAR from build-dev; relaxed (server.xml
#                  mounted at runtime, /var/lib/... provided by compose volumes)
#
# Usage:
#   docker build -t oe:prod .                  # default — prod
#   docker build --target prod -t oe:prod .
#   docker build --target dev  -t oe:dev  .
#
# Mirrors the same consolidation pattern PR #3349 applied to frontend/Dockerfile.
##

##
# Stage: build-base — common Maven setup + dataexport build
##
FROM maven:3-eclipse-temurin-21 AS build-base

# APT hardening: Azure mirror fallback + retries (from PR #3439/#3440)
RUN --mount=target=/var/lib/apt/lists,type=cache,sharing=locked \
    --mount=target=/var/cache/apt,type=cache,sharing=locked \
    rm -f /etc/apt/apt.conf.d/docker-clean \
    && sed -i 's|http://archive.ubuntu.com|http://azure.archive.ubuntu.com|g; s|http://security.ubuntu.com|http://azure.archive.ubuntu.com|g' \
        /etc/apt/sources.list /etc/apt/sources.list.d/*.list 2>/dev/null || true \
    && apt-get -o Acquire::Retries=5 -o Acquire::http::Timeout=60 \
        --allow-releaseinfo-change -y update \
    && apt-get -o Acquire::Retries=5 -o Acquire::http::Timeout=60 \
        -y --no-install-recommends install \
        git apache2-utils

# OE Default Password setup
ARG DEFAULT_PW="adminADMIN!"
COPY ./install/createDefaultPassword.sh /build/install/createDefaultPassword.sh
WORKDIR /build
RUN ./install/createDefaultPassword.sh -c -p ${DEFAULT_PW}

##
# Build DataExport
##
COPY ./dataexport /build/dataexport
WORKDIR /build/dataexport/dataexport-core
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn clean install -DskipTests
WORKDIR /build/dataexport/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn clean install -DskipTests \
    && mkdir -p /build/dataexport-m2/org \
    && cp -r /root/.m2/repository/org/itech /build/dataexport-m2/org/

##
# Stage: build-prod — production webapp build (spotless gated)
##
FROM build-base AS build-prod

WORKDIR /build
COPY ./pom.xml /build/pom.xml
# Cache-restore pattern: BuildKit cache mount starts empty when layers are
# restored from GHA cache; re-seed dataexport artifacts before mvn resolution.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    [ -d /root/.m2/repository/org/itech ] || { mkdir -p /root/.m2/repository/org && cp -r /build/dataexport-m2/org/itech /root/.m2/repository/org/; } \
    && mvn dependency:go-offline

ARG SKIP_SPOTLESS="false"
COPY ./src /build/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    [ -d /root/.m2/repository/org/itech ] || { mkdir -p /root/.m2/repository/org && cp -r /build/dataexport-m2/org/itech /root/.m2/repository/org/; } \
    && mvn clean install -Dmaven.test.skip=true -DskipITs=true -Dspotless.check.skip=${SKIP_SPOTLESS}

##
# Stage: build-dev — development webapp build (spotless skipped, tools/ included)
##
FROM build-base AS build-dev

WORKDIR /build
COPY ./pom.xml /build/pom.xml
# tools/ is included in dev so developers can iterate on analyzer-mock-server,
# DBBackup, DBUtils, etc. without a separate build context.
COPY ./tools /build/tools
# Same cache-restore pattern as build-prod.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    [ -d /root/.m2/repository/org/itech ] || { mkdir -p /root/.m2/repository/org && cp -r /build/dataexport-m2/org/itech /root/.m2/repository/org/; } \
    && mvn dependency:go-offline

COPY ./src /build/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    [ -d /root/.m2/repository/org/itech ] || { mkdir -p /root/.m2/repository/org && cp -r /build/dataexport-m2/org/itech /root/.m2/repository/org/; } \
    && mvn clean install -DskipTests -Dmaven.test.skip=true -Dspotless.check.skip=true

##
# Stage: runtime-base — common Tomcat 10 runtime + hardening
##
FROM tomcat:10-jre21 AS runtime-base

COPY install/createDefaultPassword.sh ./

# Clean out unneccessary files from tomcat (especially pre-existing applications)
RUN rm -rf /usr/local/tomcat/webapps/* \
    /usr/local/tomcat/conf/Catalina/localhost/manager.xml

# Deploy the ROOT.war (placeholder redirect)
COPY install/tomcat-resources/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Rewrite catalina.properties / logging.properties / ServerInfo.properties.
# catalina.properties contains:
#   org.apache.catalina.STRICT_SERVLET_COMPLIANCE=true
#   org.apache.catalina.connector.RECYCLE_FACADES=true
#   org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=false
#   org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=false
#   org.apache.coyote.USE_CUSTOM_STATUS_MSG_IN_HEADER=false
COPY install/tomcat-resources/catalina.properties /usr/local/tomcat/conf/catalina.properties
COPY install/tomcat-resources/logging.properties /usr/local/tomcat/conf/logging.properties

RUN mkdir -p /usr/local/tomcat/lib/org/apache/catalina/util
COPY install/tomcat-resources/ServerInfo.properties /usr/local/tomcat/lib/org/apache/catalina/util/ServerInfo.properties

# Tomcat user/group + permission hardening.
# GID AND UID must be kept the same as setupTomcat.sh (default certificate group).
RUN groupadd tomcat; \
    groupadd tomcat-ssl-cert -g 8443; \
    useradd -M -s /bin/bash -u 8443 tomcat_admin; \
    usermod -a -G tomcat,tomcat-ssl-cert tomcat_admin; \
    chown -R tomcat_admin:tomcat $CATALINA_HOME; \
    chmod g-w,o-rwx $CATALINA_HOME; \
    chmod g-w,o-rwx $CATALINA_HOME/conf; \
    chmod o-rwx $CATALINA_HOME/logs; \
    chmod o-rwx $CATALINA_HOME/temp; \
    chmod g-w,o-rwx $CATALINA_HOME/bin; \
    chmod g-w,o-rwx $CATALINA_HOME/webapps; \
    chmod 770 $CATALINA_HOME/conf/catalina.policy; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/catalina.properties; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/context.xml; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/logging.properties; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/server.xml; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/tomcat-users.xml; \
    chmod g-w,o-rwx $CATALINA_HOME/conf/web.xml

COPY install/openelis_healthcheck.sh /healthcheck.sh
RUN chown tomcat_admin:tomcat /healthcheck.sh; \
    chmod 770 /healthcheck.sh

COPY install/docker-entrypoint.sh /docker-entrypoint.sh
RUN chown tomcat_admin:tomcat /docker-entrypoint.sh; \
    chmod 770 /docker-entrypoint.sh

USER root
ENTRYPOINT [ "/docker-entrypoint.sh" ]

##
# Stage: dev — dev runtime (copies WAR from build-dev; server.xml expected at runtime via mount)
##
FROM runtime-base AS dev

COPY --from=build-dev /build/target/OpenELIS-Global.war /usr/local/tomcat/webapps/OpenELIS-Global.war

# /var/lib/openelis-global/* directories and oe_server.xml are provided by
# docker compose volume mounts in the dev flow (see compose.override.yaml).

##
# Stage: prod — (DEFAULT) production runtime (full hardening, baked server.xml)
##
FROM runtime-base AS prod

COPY --from=build-prod /build/target/OpenELIS-Global.war /usr/local/tomcat/webapps/OpenELIS-Global.war

# Create runtime directories and hand them to tomcat_admin. These must exist
# inside the image for prod deployments where there are no bind mounts.
RUN mkdir -p /var/lib/openelis-global/logs/ \
    && chown -R tomcat_admin:tomcat /var/lib/openelis-global/logs/ \
    && mkdir -p /var/lib/openelis-global/properties/ \
    && chown -R tomcat_admin:tomcat /var/lib/openelis-global/properties/ \
    && mkdir -p /var/lib/openelis-global/configuration/ \
    && chown -R tomcat_admin:tomcat /var/lib/openelis-global/configuration/

# Bake oe_server.xml in prod (dev mounts this at runtime via compose).
COPY ./tomcat/oe_server.xml /usr/local/tomcat/conf/server.xml
