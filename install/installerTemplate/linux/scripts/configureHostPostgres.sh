#!/bin/bash
#grabbed from https://gist.github.com/averagehuman/fcabcd0847a36ced38a9
################################################################################
# Rather than run postgres in its own container, we want to run it on
# the (Ubuntu) host and allow:
#
#    + peer connections on the host
#    + local md5 connections from any docker container
#
# THIS IS COPY/PASTED FROM COMMAND LINE INPUT AND IS UNTESTED AS A SINGLE SCRIPT
################################################################################

postgres_dir=$1
echo "Postgres main directory is ${postgres_dir}"
#get apt tools so we can use ifconfig
apt-get --assume-yes install net-tools
# Determine the docker bridge IP address (assumed to be docker0)
bridge_ip=$(ifconfig docker0 | grep "inet" | awk '{print $2}' | sed "s/.*://")

# subnet for container interfaces
docker_subnet="172.20.1.0/24"

# update postgresql.conf to listen only on the bridge interface
sed -i.orig "s/^[#]\?listen_addresses .*/listen_addresses = '*'/g" ${postgres_dir}postgresql.conf

# update pg_hba.conf to allow connections from the subnet
echo "host    clinlims             clinlims             localhost                   md5" >> ${postgres_dir}pg_hba.conf
echo "host    replication          backup               localhost                   md5" >> ${postgres_dir}pg_hba.conf
echo "local   replication          backup                                           trust" >> ${postgres_dir}pg_hba.conf
echo "host    clinlims             clinlims             ${docker_subnet}            md5" >> ${postgres_dir}pg_hba.conf

# update ufw firewall rules (postgres assumed to be runing on port 5432)
#ufw allow in from ${docker_subnet} to ${bridge_ip} port 5432

service postgresql restart

echo "Restart of postgres and ufw services is now required"
