#!/bin/bash


PRIMARY_IP=$1
SECONDARY_IP=$2
SECONDARY_USER=$3
SECONDARY_PASSWORD=$4

echo "primary ip: ${PRIMARY_IP}"
echo "secondary ip: ${SECONDARY_IP}"
echo "secondary user: ${SECONDARY_USER}"
docker stop external-fhir-api openelisglobal-webapp autoheal-oe openelisglobal-database;
tar xzf openelis-global.tar.gz -C /;
chown 8443:8443 /var/lib/openelis-global/logs;
chown 8443:8443 /var/lib/openelis-global/tomcatLogs;
chown 8443:8443 /var/lib/openelis-global/secrets/common.properties;
chown 8443:8443 /var/lib/openelis-global/secrets/extra.properties;
chown 8443:8443 /var/lib/openelis-global/secrets/hapi_application.yaml;
chown 8443:8443 /var/lib/openelis-global/secrets/datasource.password;
docker start openelisglobal-database;
docker exec  openelisglobal-database chown postgres:postgres /backups;
docker exec  openelisglobal-database chown postgres:postgres /backups/archive;
docker exec  openelisglobal-database mkdir /backups/basebackup;
docker exec  openelisglobal-database chown postgres:postgres /backups/basebackup;
#still on the secondary server, take the base backup of the primary server and copy it to the right location
BACKUP_USER_PASSWORD=`cat /var/lib/openelis-global/secrets/backup_datasource.password`;
PGPASS="${PRIMARY_IP}:5432:*:backup:${BACKUP_USER_PASSWORD}";
echo ${PGPASS} >> /var/lib/openelis-global/database/.pgpass;
chmod 0600 /var/lib/openelis-global/database/.pgpass;
docker exec  openelisglobal-database chown postgres:postgres /var/lib/postgresql/.pgpass;

docker exec -u postgres:postgres openelisglobal-database pg_basebackup -D /backups/basebackup -h ${PRIMARY_IP} -p 5432 -X stream -c fast -U backup -W -R;
docker kill openelisglobal-database;
rm -r /var/lib/openelis-global/data2;
mv /var/lib/openelis-global/data /var/lib/openelis-global/data2;
mv /var/lib/openelis-global/backups/basebackup /var/lib/openelis-global/data;
docker start openelisglobal-database;
