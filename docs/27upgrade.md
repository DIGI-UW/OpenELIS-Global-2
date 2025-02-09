# FHIR BLOBs

Hapi-fhir-jpaserver is used alongside OpenELIS to provide FHIR support. Older versions of the project made extensive use of the Postgres BLOB data type. This data type cannot be handled in the same way as "normal" Postgres data types. Because of this, upgrading a large DB with many BLOBs takes a long time, is very memory intensive, and is prone to failing if enough memory isn't available. Due to these reasons, it is recommended to upgrade your hapi-fhir-jpaserver to at least 6.6.0 before upgrading your DB, so you can run the reindex command which moves all resources (smaller than a configurable limit) into the normal table structure.

## Instructions for Updating FHIR BLOBs

1. Stop all containers except the database:
```bash
sudo docker stop openelisglobal-webapp autoheal-oe external-fhir-api
```

2. Collect initial metrics:
```bash
sudo docker exec -it openelisglobal-database psql -Uclinlims
SELECT COUNT(*) FROM clinlims.hfj_res_ver;
SELECT pg_table_size('pg_largeobject');
```

3. Run the hapi-fhir-jpaserver upgrade script:
```bash
wget https://github.com/hapifhir/hapi-fhir/releases/download/v6.2.0/hapi-fhir-6.2.0-cli.tar.bz2
bzip2 -d hapi-fhir-6.2.0-cli.tar.bz2
tar xf hapi-fhir-6.2.0-cli.tar
./hapi-fhir-cli migrate-database -d POSTGRES_9_4 -u "jdbc:postgresql://localhost:15432/clinlims currentSchema=clinlims" -n "clinlims" --no-column-shrink -p <password>
```

4. Verify data integrity:
```bash
sudo docker exec -it openelisglobal-database psql -Uclinlims
SELECT COUNT(*) FROM clinlims.hfj_res_ver;
SELECT pg_table_size('pg_largeobject');
```

5. Install OpenELIS 2.7:
```bash
wget <online-path-to-2.7-installer>
tar -xzf <installer-tar.gz>
cd <installer-dir>
sudo python3 OpenELIS.py
```
   - Select "No" for logical db backup, docker cleans, and backup script

6. Update the `docker-compose.yml`:
   - Change database container image from `14.4` to `9.5`

7. Start containers:
```bash
sudo docker-compose up -d
```

8. Create reindex files for each resource type. Common types include:
- Task
- Patient
- ServiceRequest
- DiagnosticReport
- Observation
- Specimen
- Practitioner
- Organization
- Location
- QuestionnaireResponse

Use this template for each resource type:
```json
{
  "resourceType": "Parameters",
  "parameter": [{
    "name": "url",
    "valueString": "<ResourceType>?"
  }, {
    "name": "optimizeStorage",
    "valueString": "ALL_VERSIONS"
  }]
}
```

9. Submit optimize requests:
```bash
sudo curl -X POST -H "Content-Type: application/json" -d '@task-optimize.json' --cert /etc/openelis-global/cert.pem --key /etc/openelis-global/key.pem -k 'https://localhost:8444/fhir/$reindex'
```

10. Monitor FHIR container logs for reindex completion

11. Clean up the database:
```bash
sudo docker exec -it openelisglobal-database vacuumlo -Uclinlims --dry-run
sudo docker exec -it openelisglobal-database vacuumlo -Uclinlims
```

12. Verify data integrity again:
```bash
sudo docker exec -it openelisglobal-database psql -Uclinlims
SELECT COUNT(*) FROM clinlims.hfj_res_ver;
SELECT pg_table_size('pg_largeobject');
```

13. Run pg_upgrade (see section below)

14. Final data verification:
```bash
sudo docker exec -it openelisglobal-database psql -Uclinlims
SELECT COUNT(*) FROM clinlims.hfj_res_ver;
SELECT pg_table_size('pg_largeobject');
```

# Migrate with pg_upgrade

This method provides faster Postgres database upgrades.

**Important:** Use a recoverable script session (e.g., `screen`) when working on remote servers. Reconnect using `screen -x` if disconnected.

## Prerequisites

1. Ensure data backups are current and recovery procedures are tested
2. Allocate at least 128GB memory (SSD swap drive recommended)
3. Old database files are preserved until verification is complete
4. Ensure sufficient disk space for database file duplication at `/var/lib/openelis-global/data`

## Migrating OE 9.5 Database to 14.4 (Dockerized Environment)

1. Stop containers:
```bash
sudo docker stop autoheal-oe external-fhir-api openelisglobal-webapp openelisglobal-database
```

2. Remove database container:
```bash
sudo docker rm openelisglobal-database
```

3. Create migration directories:
```bash
sudo mkdir -p /var/lib/openelis-global/db/9.5
sudo mkdir -p /var/lib/openelis-global/db/14/data
```

4. Copy current database:

For local migration:
```bash
sudo cp -r /var/lib/openelis-global/data /var/lib/openelis-global/db/9.5/data
```

For remote migration:
```bash
sudo tar cf /var/lib/openelis-global/data.tar.gz -C /var/lib/openelis-global/ data
sudo scp /var/lib/openelis-global/data.tar.gz username@destination:/var/lib/openelis-global/db/9.5/data.tar.gz
ssh username@destination
tar xzf /var/lib/openelis-global/db/9.5/data.tar.gz -C /var/lib/openelis-global/db/9.5/
```

5. Run migration:
```bash
sudo docker pull ctsteele/postgres-migration:9.5-14
sudo docker run -it --rm -v /var/lib/openelis-global/db/:/var/lib/postgresql/ ctsteele/postgres-migration:9.5-14 --link
```

6. Replace database:
```bash
sudo mv /var/lib/openelis-global/data /var/lib/openelis-global/data2
sudo mv /var/lib/openelis-global/db/14/data /var/lib/openelis-global/data
```

7. Set permissions:
```bash
sudo chown -R tomcat2:tomcat2 /var/lib/openelis-global/data
```
   - Copy entries from `/var/lib/openelis-global/data2/pg_hba.conf` to `/var/lib/openelis-global/data/pg_hba.conf`

8. Run setup:
```bash
sudo python3 setup_OpenELIS.py
```

9. Verify system functionality

10. Optional: Remove old data:
```bash
sudo rm -r /var/lib/openelis-global/db /var/lib/openelis-global/data2
```

## Migrating OE 9.5 to 14.4 (Non-Dockerized to Dockerized Environment)

1. Stop containers:
```bash
sudo docker stop autoheal-oe external-fhir-api openelisglobal-webapp
```

2. Stop Postgres:
```bash
sudo -u postgres /usr/lib/postgresql/9/bin/pg_ctl -D /var/lib/postgresql/9/data -l logfile stop
```

3. Test upgrade:
```bash
time /usr/lib/postgresql/14/bin/pg_upgrade \
  --old-bindir /usr/lib/postgresql/9/bin \
  --new-bindir /usr/lib/postgresql/14/bin \
  --old-datadir /var/lib/postgresql/9/data \
  --new-datadir /var/lib/openelis-global/data \
  --link --check
```

4. Run upgrade:
```bash
time /usr/lib/postgresql/14/bin/pg_upgrade \
  --old-bindir /usr/lib/postgresql/9/bin \
  --new-bindir /usr/lib/postgresql/14/bin \
  --old-datadir /var/lib/postgresql/9/data \
  --new-datadir /var/lib/openelis-global/data \
  --link
```

5. Run setup:
```bash
sudo setup_OpenELIS.py
```

6. Verify system functionality

# Migrate with pg_dump

This is the Postgres-recommended approach but is very slow for BLOB restoration.

## Migrating OE 9.5 to 14.4 (Dockerized Environment)

1. Create backups:
```bash
sudo docker exec openelisglobal-database pg_dump -j 8 -d clinlims --verbose -U admin -F c -f /backups/95db.backup
sudo docker kill openelisglobal-database && sudo mv /var/lib/openelis-global/data /var/lib/openelis-global/data2
```

2. Run setup script (ignore database missing prompt)

3. Restore database:
```bash
sudo docker kill external-fhir-api openelisglobal-webapp && sudo docker rm external-fhir-api openelisglobal-webapp
sudo docker exec openelisglobal-database pg_restore -j 8 -d clinlims -U postgres -v -Fc -c /backups/95db.backup
sudo docker-compose up -d
```

4. Verify functionality

5. Optional: Remove old data:
```bash
rm /var/lib/openelis-global/data2
rm /var/lib/openelis-global/backups/95db.backup
```

## Migrating OE 9.5 to 14.4 (Non-Dockerized to Dockerized Environment)

1. Create backup:
```bash
pg_dump -d clinlims -h localhost -p 5432 --verbose -U clinlims -F c -f /var/lib/openelis-global/backups/95db.backup
```

2. Update setup.ini for Docker database port

3. Run setup script

4. Restore database:
```bash
sudo docker kill external-fhir-api openelisglobal-webapp && sudo docker rm external-fhir-api openelisglobal-webapp
sudo docker exec openelisglobal-database pg_restore -d clinlims -U postgres -v -Fc -c /backups/95db.backup
sudo docker-compose up -d
```

5. Verify functionality

6. Optional cleanup:
```bash
sudo rm /var/lib/openelis-global/backups/95db.backup
```

7. Optionally uninstall native Postgres if moving Docker database to port 5432
