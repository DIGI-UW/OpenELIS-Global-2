#!/bin/bash

#get location of this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  scriptDir="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
scriptDir="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

read -p "Enter primary server ip address (where secondary server can reach this server): " PRIMARY_IP
read -p "Enter secondary server's ip address: " SECONDARY_IP
if [[ -n "$SECONDARY_IP" ]]; then
  CONFIGURE_SECONDARY=true
  read -p "Enter secondary server's user: " SECONDARY_USER
  read -sp "Enter secondary server's user password: " SECONDARY_PASSWORD
else
  CONFIGURE_SECONDARY=false
fi


#add permission for secondary server to connect to primary in replication mode
ACCESS_STRING="host    replication          backup               ${SECONDARY_IP}/32                   md5"
if grep -Fxq "${ACCESS_STRING}" /var/lib/openelis-global/data/pg_hba.conf
then
    echo "permission to connect to databse already exists for remote user. Continuing..."
else
    echo "${ACCESS_STRING}" >> /var/lib/openelis-global/data/pg_hba.conf
fi

#discard previous recovery.confs in case this is a secondary server becoming the primary
#restart the db container so that it can be configured
#take down all others so this occurs without interruption
docker exec openelisglobal-database psql -Upostgres -c "ALTER SYSTEM SET primary_conninfo = '';"
rm /var/lib/openelis-global/data/standby.signal
docker stop external-fhir-api openelisglobal-webapp autoheal-oe openelisglobal-database
docker start openelisglobal-database
echo "waiting for DB container to start..."
until [ "`docker inspect -f {{.State.Health.Status}} openelisglobal-database`"=="healthy" ]; do
    sleep 2;
done;
echo "docker DB container started"

docker exec  openelisglobal-database chown postgres:postgres /backups/archive;
#configure the primary db as the primary and restart so the changes take effect
cp ${scriptDir}/configurePrimary.sql /var/lib/openelis-global/data/configurePrimary.sql
chmod 777 /var/lib/openelis-global/data/configurePrimary.sql
docker exec openelisglobal-database psql -Uadmin -dclinlims -f /var/lib/postgresql/data/configurePrimary.sql
docker restart openelisglobal-database

if [[ "$CONFIGURE_SECONDARY" = true ]]; then
  dpkg -s sshpass &> /dev/null

  if [ $? -ne 0 ]; then
    apt install sshpass -y
  fi
  if [ $? -ne 0 ]; then
    echo "failed to install a dependency, exiting"
    exit -1
  fi

  #copy the primary directory so the secondary has access to the same configuration
  # should we exclude the data directory since it gets copied by pg_basebackup?
  #tar --exclude='/var/lib/openelis-global/data/*'  czfp ${scriptDir}/openelis-global.tar.gz /var/lib/openelis-global/
  tar czfp ${scriptDir}/openelis-global.tar.gz /var/lib/openelis-global/
  sshpass -p "${SECONDARY_PASSWORD}" scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -q ${scriptDir}/openelis-global.tar.gz ${SECONDARY_USER}@${SECONDARY_IP}:~/
  rm ${scriptDir}/openelis-global.tar.gz

  sshpass -p "${SECONDARY_PASSWORD}" scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -q ${scriptDir}/configureSecondary.sh ${SECONDARY_USER}@${SECONDARY_IP}:~/

  #on the secondary server unpack the same configuration and copy it to the required location being mindful of owner
  sshpass -p "${SECONDARY_PASSWORD}" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -q ${SECONDARY_USER}@${SECONDARY_IP} << EOF
    export
    echo ${SECONDARY_PASSWORD} | sudo -S ./configureSecondary.sh ${PRIMARY_IP} ${SECONDARY_IP} ${SECONDARY_USER} ${SECONDARY_PASSWORD}
    exit
  EOF
fi

docker compose up -d
