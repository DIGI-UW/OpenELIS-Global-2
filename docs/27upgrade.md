# FHIR BLOBs

Hapi-fhir-jpaserver is used alongside OpenELIS to provide FHIR support. Older
versions of the project made extensive use of the Postgres BLOB data type. This
data type cannot be handled in the same way as "normal" Postgres data types.
Because of this, upgrading a large DB with many BLOBs takes a long time, is very
memory intensive, and is prone to failing if enough memory isn't available. Due
to these reasons, it is recommended to upgrade your hapi-fhir-jpaserver to at
least 6.6.0 before upgrading your DB, so you can run the reindex command which
moves all resources (smaller than a configurable limit) into the normal table
structure.

## Instructions for updating FHIR BLOBs

1.  Stop the existing containers besides the database

    1.  `sudo docker stop openelisglobal-webapp autoheal-oe external-fhir-api`

1.  Collect metrics around the data to see what you're working with

    1.  `sudo docker exec -it openelisglobal-database psql -Uclinlims`

    1.  `SELECT COUNT(*) FROM clinlims.hfj_res_ver;`

    1.  `SELECT pg_table_size('pg_largeobject');`

1.  Run the script for upgrading hapi-fhir-jpaserver data structure in the
    database

    1.  `wget https://github.com/hapifhir/hapi-fhir/releases/download/v6.2.0/hapi-fhir-6.2.0-cli.tar.bz2`

    1.  `bzip2 -d hapi-fhir-6.2.0-cli.tar.bz2`

    1.  `tar xf hapi-fhir-6.2.0-cli.tar`

    1.  `./hapi-fhir-cli migrate-database -d POSTGRES_9_4 -u "jdbc:postgresql://localhost:15432/clinlims currentSchema=clinlims" -n "clinlims" --no-column-shrink -p <password>`

1.  Collect metrics around the data to see that data loss has not occurred

    1.  `sudo docker exec -it openelisglobal-database psql -Uclinlims`

    1.  `SELECT COUNT(*) FROM clinlims.hfj_res_ver;`

    1.  `SELECT pg_table_size('pg_largeobject');`

1.  Run 2.7 OE installer

    1.  `wget <online-path-to-2.7-installer>`

    1.  `tar -xzf <installer-tar.gz>`

    1.  `cd <installer-dir>`

    1.  `sudo python3 OpenELIS.py`

    1.  `No` to logical db backup, docker cleans, and backup script

1.  Modify the `docker-compose.yml`

    1.  Change db container image from `14.4` to `9.5`

1.  Start the containers. From this point until the db migration is started,
    these commands can be run in the background while normal use of the
    application(s) goes ahead

    1.  `sudo docker-compose up -d`

1.  Create files to submit to the updated FHIR store to trigger a reindex of the
    various data types from BLOBs to regular column data. Resource types that
    are commonly used, leading to the bulk of the BLOBs are: `Task`, `Patient`,
    `ServiceRequest`, `DiagnosticReport`, `Observation`, `Specimen`,
    `Practitioner`, `Organization`, `Location`, `QuestionnaireResponse`

```
{
  "resourceType": "Parameters",
  "parameter": [ {
    "name": "url",
    "valueString": "<ResourceType>?"
  }, {
    "name": "optimizeStorage",
    "valueString": "ALL_VERSIONS"
  } ]
}
```

1.  Send each optimize request as a POST request to the FHIR server

    1.  `sudo curl -X POST -H "Content-Type: application/json" -d '@task-optimize.json' --cert /etc/openelis-global/cert.pem --key /etc/openelis-global/key.pem -k 'https://localhost:8444/fhir/$reindex'`

1.  Wait for the reindexes to succeed (checking the logs of the FHIR container
    should give some indication that they are running or not)

1.  Run the vacuum lob command to clean up the database (first run is a test)

    1.  `sudo docker exec -it openelisglobal-database vacuumlo -Uclinlims --dry-run`

    1.  `sudo docker exec -it openelisglobal-database vacuumlo -Uclinlims`

1.  Collect metrics around the data to see that data loss has not occurred.
    `The pg_largeobject` should be MUCH smaller, but `hfj_res_ver` should be
    similar to before.

    1.  `sudo docker exec -it openelisglobal-database psql -Uclinlims`

    1.  `SELECT COUNT(*) FROM clinlims.hfj_res_ver;`

    1.  `SELECT pg_table_size('pg_largeobject');`

1.  Run the `pg_upgrade` as in the "Migrate with pg_upgrade" section below

1.  Collect metrics around the data to see that data loss has not occurred.

    1.  `sudo docker exec -it openelisglobal-database psql -Uclinlims`

    1.  `SELECT COUNT(*) FROM clinlims.hfj_res_ver;`

    1.  `SELECT pg_table_size('pg_largeobject');`

# **Migrate with pg_upgrade**

Much faster method of upgrading postgres dbs

If you are running these commands on a remote server, it is recommended to use a
recoverable script session. For example, running `screen`. If you disconnect,
just reconnect and run `screen -x` to recover active session.

## NOTES:

1.  It is paramount that data backups are up to date and recovery is tested
    before attempting a database migration.
1.  This can be a very memory intensive process. It is recommended to increase
    memory on the server that this is running on to 128 GB or more. This can be
    accomplished without too much of a performance hit by adding an SSD as a
    swap drive.
1.  This process doesn't delete the old database files until the new database is
    up, running, and tested. Ensure that OE is running properly and the data is
    there BEFORE deleting the old machine.
1.  Because of point number 3, you will require room on the server for a copy of
    all the database files located at `/var/lib/openelis-global/data`

## **Migrating OE 9.5 database to 14.4 database in dockerized environments**

1.  stop containers so no changes happen while migration is occurring

    1.  `sudo docker stop autoheal-oe external-fhir-api openelisglobal-webapp openelisglobal-database`

1.  remove db container so auto restart doesn’t occur

    1.  `sudo docker rm openelisglobal-database`1.

1.  create folders for first step db migration to take place (this can be done
    on a separate machine with docker installed if the main server lacks
    resources to run the upgrade).

    1.  `sudo mkdir /var/lib/openelis-global/db`

    1.  `sudo mkdir /var/lib/openelis-global/db/9.5`

    1.  `sudo mkdir /var/lib/openelis-global/db/14`

    1.  `sudo mkdir /var/lib/openelis-global/db/14/data`

1.  copy current db to the upgrade location (copy will preserve the old data so
    that we can more easily revert if something goes wrong)

    1.  If you are performing the upgrade on the machine where the database is
        installed:

    1.  `sudo cp -r /var/lib/openelis-global/data /var/lib/openelis-global/db/9.5/data`

    1.  If you are performing the upgrade on another machine where the database
        is not installed:

    1.  `sudo tar cf /var/lib/openelis-global/data.tar.gz -C /var/lib/openelis-global/ data`

    1.  `sudo scp /var/lib/openelis-global/data.tar.gz username@destination:/var/lib/openelis-global/db/9.5/data.tar.gz`

    1.  `ssh username@destination`

    1.  `tar xzf /var/lib/openelis-global/db/9.5/data.tar.gz -C /var/lib/openelis-global/db/9.5/`

1.  run the 9.5 to 14 migration

    1.  `sudo docker pull ctsteele/postgres-migration:9.5-14`

    1.  `sudo docker run -it --rm -v /var/lib/openelis-global/db/:/var/lib/postgresql/ ctsteele/postgres-migration:9.5-14 --link`

1.  replace old db with new db

    1.  `sudo mv /var/lib/openelis-global/data /var/lib/openelis-global/data2`

    1.  `sudo mv /var/lib/openelis-global/db/14/data /var/lib/openelis-global/data`

1.  ensure file permissions and db access permissions are correct

    1.  `sudo chown -R tomcat2:tomcat2 /var/lib/openelis-global/data`

    1.  edit `/var/lib/openelis-global/data/pg_hba.conf` to include all the same
        entries that are in `/var/lib/openelis-global/data2/pg_hba.conf`

1.  run the setup script for the new version with updated db, ignoring db backup
    couldn’t occur step

    1.  `sudo python3 setup_OpenELIS.py`

1.  ensure systems start up and that data is present

1.  optionally delete old db (or move to a secure backup server)

    1.  `sudo rm /var/lib/openelis-global/db /var/lib/openelis-global/data2`

## **Migrating OE 9.5 database to 14.4 database in non dockerized environment into dockerized environment (untested)**

1.  stop containers so no changes happen while migration is occurring

1.  `sudo docker stop autoheal-oe external-fhir-api openelisglobal-webapp`

1.  stop postgres instance

1.  `sudo -upostgres /usr/lib/postgresql/9/bin/pg_ctl -D /var/lib/postgresql/9/data -l logfile stop`

1.  check that upgrade can occur

1.  `time /usr/lib/postgresql/14/bin/pg_upgrade --old-bindir /usr/lib/postgresql/9/bin --new-bindir /usr/lib/postgresql/14/bin --old-datadir /var/lib/postgresql/9/data --new-datadir /var/lib/openelis-global/data --link --check`

1.  run the upgrade

1.  `time /usr/lib/postgresql/14/bin/pg_upgrade --old-bindir /usr/lib/postgresql/9/bin --new-bindir /usr/lib/postgresql/14/bin --old-datadir /var/lib/postgresql/9/data --new-datadir /var/lib/openelis-global/data --link`

1.  run the setup script for the new version with updated db, ignoring db backup
    couldn’t occur step

1.  `sudo setup_OpenELIS.py`

1.  ensure systems start up and that data is present

# **Migrate with pg_dump**

This approach is mentioned as being the preferred option in postgres docs, but
is VERY slow when restoring BLOBs

## **Migrating OE 9.5 database to 14.4 database in dockerized environments**

1.  Run the following commands to create the backup for restoring into OE 14,
    and the backup for 9.5 in case something goes wrong

    1.  `sudo docker exec openelisglobal-database pg_dump -j 8 -d clinlims --verbose -U admin -F c -f /backups/95db.backup`

    1.  `sudo docker kill openelisglobal-database && sudo mv /var/lib/openelis-global/data /var/lib/openelis-global/data2`

1.  install new OE with setup script - ignore db missing when prompted

1.  Run the following to pause non-db connections and restore the backup before
    bringing the containers up again (must run in OE installer directory).
    Restoring a db can take a long time for large dbs. To avoid a long downtime,
    this can be done in a separate container

    1.  `sudo docker kill external-fhir-api openelisglobal-webapp && sudo docker rm external-fhir-api openelisglobal-webapp`

    1.  `sudo docker exec openelisglobal-database pg_restore -j 8 -d clinlims -U postgres -v -Fc -c /backups/95db.backup`

    1.  `sudo docker-compose up -d`

1.  confirm that OpenELIS is working and that the data is there by accessing the
    front end

1.  optionally, delete old data

    1.  `rm /var/lib/openelis-global/data2`

    1.  `rm /var/lib/openelis-global/backups/95db.backup`

## **Migrating OE 9.5 database to 14.4 database in non dockerized environment into dockerized environment (untested)**

1.  Run the following commands to create the backup for restoring into OE 14

    1.  `pg_dump -d clinlims -h localhost -p 5432 –verbose -U clinlims -F c -f /var/lib/openelis-global/backups/95db.backup`

    1.  [enter password for clinlims]

1.  Modify `setup.ini` to have docker db on port other than `5432` and use
    docker db

    1.  `sudo vi /etc/openelis-global/setup.ini`

1.  install new OE with setup script

1.  Run the following to pause non-db connections and restore the backup before
    bringing the containers up again (must run in OE installer directory)

    1.  `sudo docker kill external-fhir-api openelisglobal-webapp && sudo docker rm external-fhir-api openelisglobal-webapp`

    1.  `sudo docker exec openelisglobal-database pg_restore -d clinlims -U postgres -v -Fc -c /backups/95db.backup`

    1.  `sudo docker-compose up -d`

1.  confirm that OpenELIS is working and that the data is there by accessing the
    front end

1.  optionally, delete old data

    1.  `sudo rm /var/lib/openelis-global/backups/95db.backup`

1.  uninstall native postgres if you want to move the docker db onto port 5432
