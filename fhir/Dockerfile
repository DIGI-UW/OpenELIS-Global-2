FROM hapiproject/hapi:v6.6.0-tomcat

USER root
RUN apt-get update && apt-get install -y curl

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
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/server.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/tomcat-users.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/web.xml;\
    chown -R tomcat_admin:tomcat /bitnami/tomcat/

USER tomcat_admin
