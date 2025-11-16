FROM hapiproject/hapi:v6.6.0-tomcat

USER root
RUN apt-get update && apt-get install -y curl

COPY ./tomcat/hapi_server.xml /opt/bitnami/tomcat/conf/server.xml

RUN chown tomcat:tomcat /opt/bitnami/tomcat/conf/server.xml \
    && chmod 640 /opt/bitnami/tomcat/conf/server.xml

# Create groups and custom user, set ownership and folder permissions
RUN groupadd tomcat; \
    groupadd tomcat-ssl-cert -g 8443; \
    useradd -M -s /bin/bash -u 8443 tomcat_admin; \
    usermod -a -G tomcat,tomcat-ssl-cert tomcat_admin; \
    chown -R tomcat_admin:tomcat /opt/bitnami/tomcat; \
    chown -R tomcat_admin:tomcat /target; \
    chmod g-w,o-rwx /opt/bitnami/tomcat; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf; \
    chmod o-rwx /opt/bitnami/tomcat/logs; \
    chmod o-rwx /opt/bitnami/tomcat/temp; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/bin; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/webapps; \
    chmod 770 /opt/bitnami/tomcat/conf/catalina.policy; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/catalina.properties; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/context.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/logging.properties; \
    chmod 640 /opt/bitnami/tomcat/conf/server.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/tomcat-users.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/web.xml;\
    chown -R tomcat_admin:tomcat /bitnami/tomcat/

# Ensure the target directory exists
RUN mkdir -p /opt/openelis/config

# Copy your HAPI application YAML into the image
COPY ./fhir/hapi_application.yaml /opt/openelis/config/hapi_application.yaml

# Ensure proper permissions so tomcat can read it
RUN chown tomcat_admin:tomcat /opt/openelis/config/hapi_application.yaml \
    && chmod 600 /opt/openelis/config/hapi_application.yaml

# Optionally, set SPRING_CONFIG_LOCATION so the container automatically picks it up
ENV SPRING_CONFIG_LOCATION=file:///opt/openelis/config/hapi_application.yaml
USER tomcat_admin
