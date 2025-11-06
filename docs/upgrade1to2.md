# OpenELIS-Global 1 to 2 Conversion

# Notes:

Be sure to create a backup BEFORE attempting this!

## Choosing a Path

Path 1 is going to be the easiest and most reliable method of upgrading from
version 1 to 2, but requires having a spare machine to place OpenELIS-Global 2
on.

Path 2 is a less reliable way to move from version 1 to 2, but can be done
without using a spare machine.

## Backups

Before attempting any upgrade paths, be sure you have accessible backups of your
currently running version of OpenELIS-Global 1 available on another
drive/server.

For path 1 this involves checking to make sure you are able to successfully
restore from your backup file

For path 2, it is recommended to have a backup of your entire system, not just
the database.

## Testing New Installation

- Ensure you can log in (check more than just admin account)
- Ensure old data can be properly found (ex. Search for an old patient)

# Path 1: Migrate Server to New Machine

This is the recommended method of upgrading

**ENSURE YOU BACK UP YOUR DATABASE FIRST**

## Dump Database on Old Server

1. make a logical backup of your database
   1. `sudo -u postgres pg_dump -d clinlims > OpenELIS-backup.sql`

## Install OpenELIS-Global 2 on New Server

1. Follow normal install instructions for OpenELIS-Global 2
   1. Currently located [here](install.md)

## Pause Docker containers on New Server

1. Run the following command
   1. `sudo docker kill openelisglobal-webapp external-fhir-api`

## Restore Database Dump on New Server

1. Copy OpenELIS-backup.sql from old server onto new server
1. Copy OpenELIS-backup.sql into the docker db container
   1. `sudo docker cp OpenELIS-backup.sql openelisglobal-database:/OpenELIS-backup.sql`
1. Recreate the database and import the dump
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dpostgres -c 'DROP DATABASE clinlims;'`
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dpostgres -c 'CREATE DATABASE clinlims;'`
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dpostgres -c 'ALTER DATABASE clinlims OWNER TO clinlims;'`
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dclinlims -f /OpenELIS-backup.sql`

## Migrate Passwords (If Unmigrated) on New Server

1. Follow password migration instructions
   1. Currently located [here](password.md)

## Migrate Encrypted Values on New Server

1. `sudo docker exec -it openelisglobal-database psql -Uclinlims -dclinlims`
1. `SELECT id, name, value FROM clinlims.site_information WHERE encrypted = 't';`
1. record the "value" column for each row
1. `UPDATE clinlims.site_information SET value = '' WHERE encrypted = 't';`
1. `\q`
1. re-add the values using the front end once the server is back up and running

## Run Liquibase on New Server

1. Download the
   [old liquibase files](https://github.com/DIGI-UW/Liquibase-Outdated) and if
   there is a custom branch, change to it
   1. `git clone https://github.com/DIGI-UW/Liquibase-Outdated.git`
   1. `cd Liquibase-Outdated`
   1. `git checkout <branch>`
1. Run the liquibase command
   1. put the correct connection values in `./liquibase.properties`
   1. `java -jar -Dfile.encoding=utf-8 ./lib/liquibase-1.9.5.jar --defaultsFile=./liquibase.properties --url=jdbc:postgresql://localhost:5432/clinlims --contexts=<context> update`
   1. if it complains about md5 checksums, run
      1. `sudo docker exec -it openelisglobal-database psql -Uclinlims -dclinlims -c "UPDATE clinlims.databasechangelog SET md5sum = NULL;"`
   1. if Liquibase exits citing that a changeset failed, then that changeset
      will need to be updated in the proper branch to accommodate that DB.
      Fixing a changeset can occur in many ways and should be done by a
      developer who understands OpenELIS and its relationship with the DB.
      1. If the changeset is doing an operation that is non-essential for your
         context (ie updating an Organization that doesn't exist in your DB),
         your context can be dropped from that changesets list of contexts
      1. If the changeset relies on a previous changeset to run first (dropping
         a constraint from a table that is missing one), that previous changeset
         can be added to your context
      1. If the changeset is updating based on some value that is different in
         this DB, the SQL should be updated to accommodate both values (ie test
         section called Microbiology vs Microbiologie)

## Update Installation on New Server

1. Run upgrade script from OE2 Installer directory
   1. `sudo python2 setup_OpenELIS.py`

# Path 2: Update Server in Place

**ENSURE YOU HAVE BACKED UP YOUR DATABASE AND SERVER FIRST**

## Backup Postgres

1. Run the following command
   1. `sudo -u postgres pg_dump > OpenELIS-backup.sql`

## Uninstall Tomcat from Server

## Upgrade Ubuntu to Target Version

1. Update packages
   1. `sudo apt-get update`
   2. `sudo apt-get upgrade`
2. Update current distribution 3. `sudo apt-get dist-upgrade`
3. Upgrade to next version 4. `sudo apt-get install update-manager-core` 5.
   `sudo do-release-upgrade` 6. Will run for a while...
4. Check if running target version 7. `lsb_release -a`
5. If version does not match target version, repeat from step 1

## Update/Install Installation

1. Follow normal install/upgrade instructions for OpenELIS-Global 2
   1. Currently located [here](install.md)

## Restore Database Dump into new DB

1. Copy OpenELIS-backup.sql into the docker db container
   1. `sudo docker cp OpenELIS-backup.sql openelisglobal-database:/OpenELIS-backup.sql`
1. Recreate the database and import the dump
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dpostgres -c 'DROP DATABASE clinlims;'`
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dpostgres -c 'CREATE DATABASE clinlims;'`
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dpostgres -c 'ALTER DATABASE clinlims OWNER TO clinlims;'`
   1. `sudo docker exec openelisglobal-database psql -Uadmin -dclinlims -f /OpenELIS-backup.sql`

## Migrate Passwords (If Unmigrated)

1. Follow password migration instructions
   1. Currently located [here](password.md)

## Migrate Encrypted Values on New Server

1. `sudo docker exec -it openelisglobal-database psql -Uclinlims -dclinlims`
1. `SELECT id, name, value FROM clinlims.site_information WHERE encrypted = 't';`
1. record the "value" column for each row
1. `UPDATE clinlims.site_information SET value = '' WHERE encrypted = 't';`
1. `\q`
1. re-add the values using the front end once the server is back up and running

## Run Liquibase on New Server

1. Download the
   [old liquibase files](https://github.com/DIGI-UW/Liquibase-Outdated) and if
   there is a custom branch, change to it
   1. `git clone https://github.com/DIGI-UW/Liquibase-Outdated.git`
   1. `cd Liquibase-Outdated`
   1. `git checkout <branch>`
1. Run the liquibase command
   1. put the correct connection values in `./liquibase.properties`
   1. `java -jar -Dfile.encoding=utf-8 ./lib/liquibase-1.9.5.jar --defaultsFile=./liquibase.properties --url=jdbc:postgresql://localhost:5432/clinlims --contexts=<context> update`
   1. if it complains about md5 checksums, run
      1. `sudo docker exec -it openelisglobal-database psql -Uclinlims -dclinlims -c "UPDATE clinlims.databasechangelog SET md5sum = NULL;"`

## Update Installation

1. Run upgrade script from OE2 Installer directory
   1. `sudo python setup_OpenELIS.py`
