# Retain the HAPI application without inheriting its retired Bullseye runtime.
FROM hapiproject/hapi:v6.6.0-tomcat AS hapi

FROM tomcat:9.0.121-jre17-temurin-noble

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd tomcat \
    && groupadd -g 8443 tomcat-ssl-cert \
    && useradd -M -s /bin/bash -u 8443 -g tomcat -G tomcat-ssl-cert tomcat_admin \
    && mkdir -p "$CATALINA_HOME/target" /opt/openelis/config \
    && printf '\norg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.Digester$EnvironmentPropertySource\n' >> "$CATALINA_HOME/conf/catalina.properties"

COPY --from=hapi --chown=tomcat_admin:tomcat /opt/bitnami/tomcat/webapps/ROOT.war /usr/local/tomcat/webapps/ROOT.war
COPY --chown=tomcat_admin:tomcat ./tomcat/hapi_server.xml /usr/local/tomcat/conf/server.xml
COPY --chown=tomcat_admin:tomcat ./fhir/hapi_application.yaml /opt/openelis/config/hapi_application.yaml
COPY ./fhir/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

RUN chown -R tomcat_admin:tomcat "$CATALINA_HOME" \
    && chmod g-w,o-rwx "$CATALINA_HOME" "$CATALINA_HOME/conf" "$CATALINA_HOME/bin" "$CATALINA_HOME/webapps" \
    && chmod o-rwx "$CATALINA_HOME/logs" "$CATALINA_HOME/temp" "$CATALINA_HOME/work" "$CATALINA_HOME/target" \
    && chmod g-w,o-rwx "$CATALINA_HOME"/conf/* \
    && chmod 640 "$CATALINA_HOME/conf/server.xml" \
    && chmod 600 /opt/openelis/config/hapi_application.yaml \
    && chmod 755 /usr/local/bin/docker-entrypoint.sh

ENV SPRING_CONFIG_LOCATION=file:///opt/openelis/config/hapi_application.yaml
EXPOSE 8443
USER tomcat_admin
ENTRYPOINT ["docker-entrypoint.sh"]
