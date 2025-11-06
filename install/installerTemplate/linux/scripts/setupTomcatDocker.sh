#!/bin/bash

#
# This file exists because we must partially setup the host so docker is able to read the
# certificates used for ssl. By default these are the snakeoil certs, but replacing these files in
# /etc/tomcat/ssl/ will let us use any certificates we can drop in there
#

#create tomcat_admin user and group if not exists
#MUST BE SAME UID AND GID AS IN DOCKERFILE
groupadd -g 8443 tomcat-ssl-cert || echo "tomcat-ssl-cert group already exists. Continuing"
useradd -u 8443 -g tomcat-ssl-cert tomcat_admin || echo "tomcat_admin user already exists. Continuing"

#MUST BE SAME DESTINATIONS AS IN docker-compose.yaml
DEST_KEYSTORE_FILE=/etc/openelis-global/keystore
DEST_TRUSTSTORE_FILE=/etc/openelis-global/truststore

chmod 644 ${DEST_KEYSTORE_FILE}
chmod 644 ${DEST_TRUSTSTORE_FILE}
