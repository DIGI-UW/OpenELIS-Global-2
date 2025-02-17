# Backups during upgrade

During upgrade, backups are placed in the `rollback` directory of the installer
directory

## Logical backup

A logical backup is an `.sql` file listing all the instructions that need to be
run to recreate the DB from a fresh state. They should be used when migrating DB
versions as internal file structures will likely not be the same in different DB
versions.

### Restoring a logical backup created during upgrade

It is best practice to ensure that your backup works in a non-production
environment db BEFORE you delete the current database. proceed with caution so
data is not lost.

1. `sudo docker kill openelisglobal-webapp external-fhir-api`
1. replace file name
   `sudo docker cp ./rollback/<backup-name.sql> openelisglobal-database:backup.sql`
1. `sudo docker exec openelisglobal-database psql -U admin -d postgres -f backup.sql`
1. `sudo docker-compose up -d`

## Physical backups

A physical backup is a copy of all the database files in the structure that they
are found on disk. It is not recommended to use them when migrating database
versions as the file structure is likely to change between versions.

### Restoring a physical backup created during upgrade

previous state is preserved in `/var/lib/openelis-global/data-old/` in case
restoring does not go as planned. Move this directory back in place if you'd
like to continue using it.

1. `sudo docker kill openelisglobal-database`
1. `sudo mv /var/lib/openelis-global/data/ /var/lib/openelis-global/data-old/`
1. replace directory name
   `sudo cp -r ./rollback/<backup-directory-name> /var/lib/openelis-global/data/`
1. `sudo docker start openelisglobal-database`

# Continuous Backups

Currently backups are being handled by a
[script](https://github.com/I-TECH-UW/OpenELIS-Global-2/blob/develop/install/installerTemplate/linux/templates/DatabaseBackup.pl)
that takes a logical backup periodically and copies them offsite. This script
must be configured to have a destination. This script will be getting replaced
in 2.7 to use physical backups with continuous WAL archiving that are similarly
copied offsite (for reading on logical backups vs physical backups here is a
good
[intro](https://dev.to/supabase/til-postgres-logical-vs-physical-backups-218b).
This will still require configuration to choose an offsite location and files
will not be automatically deleted from the local server, so it is important to
have someone go in to the local server, identify which files are no longer
necessary to keep and remove them from the server. It is recommended for
organizations to look at the backup mechanism/files and test the recovery
process regularly. If the supplied system isn't considered robust enough for an
organization (hot standby required, point in time recovery required, automatic
failover, 0 data loss, etc.) it is recommended to configure a separate backup
system to ensure it meets organization requirements. In particular,
[barman](https://pgbarman.org/) is recommended for managing backups.
