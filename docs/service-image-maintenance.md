# Database and FHIR Image Maintenance

The database image uses PostgreSQL 14.24 on Debian Bookworm. The FHIR image runs
the unchanged HAPI 6.6.0 WAR on Tomcat 9 with Java 17 on Ubuntu Noble. The old
HAPI image supplies only the WAR during the build, not the final operating
system, Tomcat installation, or entrypoint. Configuration stays at
`/opt/openelis/config/hapi_application.yaml`; TLS settings and runtime user 8443
are preserved. The Tomcat installation is now `/usr/local/tomcat`. Deployments
that override the server configuration by mounting a file at the old Bitnami
path (`/opt/bitnami/tomcat/conf/server.xml`, as `dev.docker-compose.yml` and the
installer template do) keep working: the image entrypoint
(`fhir/docker-entrypoint.sh`) applies such a file to the Tomcat configuration
directory before start-up. Deployments that pass `-Dhapi.ssl.*` through
`CATALINA_OPTS` use the baked `server.xml` as before.

This addresses the expired Bullseye package metadata that prevented clean image
builds. It does not constitute a HAPI application upgrade or a PostgreSQL
major-version upgrade. Both still need their own planned upgrades; pinning
versions does not replace regular dependency maintenance.

## Validate Images

Run from the repository root with Docker and jq available:

```bash
docker build --platform linux/amd64 -f db/Dockerfile -t openelis-base-refresh-db:local .
docker build --platform linux/amd64 -f fhir/Dockerfile -t openelis-base-refresh-fhir:local .
bash scripts/smoke-test-service-images.sh
```

The smoke test creates only uniquely named containers, a network, and a
certificate volume. It exposes no host ports, removes its resources on exit, and
prints the directory containing its logs and response evidence. It checks fresh
OpenELIS database initialization, non-root FHIR startup, mutual TLS, FHIR R4
metadata, and saving/reading a synthetic patient across both services' restart.
It is an infrastructure test, not browser acceptance or a new CI pipeline. The
existing end-to-end workflow remains the application gate.

## Existing Deployments

Back up and rehearse the update on a copy before replacing an existing database
container. Keep the database volume; do not reset production data. An
operating-system change also changes locale libraries. Existing indexes that
depend on locale-sensitive text ordering may need rebuilding before normal
traffic resumes. Review both the database's default locale and any explicit
column/index collations. Refreshing a recorded collation version alone does not
rebuild affected indexes. Follow the PostgreSQL
[collation maintenance guidance](https://www.postgresql.org/docs/14/sql-altercollation.html).

The fresh-database smoke test does not establish that an existing site's indexes
are safe after that library change. Site-specific backup, index maintenance,
restore, and rollback validation are deployment responsibilities. Do not disable
package signature or expiration checks to retain an obsolete base image.

### What the library change looks like on an OpenELIS database

The move from Debian 11 to Debian 12 takes glibc from 2.31 to 2.36. PostgreSQL
14 records the glibc version only for named collations in `pg_collation`; it
does not track the database's default collation (`en_US.utf8` in every OpenELIS
install), so indexes on text columns keep working silently on the new image and
can still hide ordering inconsistencies.

The database image handles this itself, without holding up start-up. Its
entrypoint (`db/docker-entrypoint-collation.sh`) hands over to the stock
entrypoint immediately and, on an existing data directory, runs the check in the
background once the server is accepting connections: it compares the recorded
collation versions with the ones the operating system provides and, only when
they differ, rebuilds the affected databases' indexes with
`REINDEX DATABASE ... CONCURRENTLY` before recording the new versions
(`ALTER COLLATION ... REFRESH VERSION`). Nothing is locked against reads or
writes, and the database is reachable throughout.

Running online rather than before start-up is deliberate. The application
container starts at the same time as the database and aborts its deployment if
the database is unreachable when its schema migration runs, so a rebuild that
held start-up would take the application down for as long as the rebuild lasted,
and that time grows with the size of the database.

Empty volumes and already-refreshed volumes do nothing at all, so the check
costs nothing on later starts. Failures are logged with the statements to run by
hand and never affect the database: the recorded versions are refreshed only
after a rebuild has actually succeeded, so an interrupted run is retried on the
next start. The check connects over the local socket as `postgres`, using
`POSTGRES_PASSWORD` where local authentication requires a password. Set
`OE_DB_SKIP_COLLATION_REINDEX=true` on the database service to skip it and run
the same statements by hand in a maintenance window:

```sql
-- as the postgres superuser, connected to clinlims
SELECT collname, collversion, pg_collation_actual_version(oid)
  FROM pg_collation WHERE collname IN ('en_US.utf8', 'en_US');
REINDEX DATABASE clinlims CONCURRENTLY;
ALTER COLLATION "en_US.utf8" REFRESH VERSION;
ALTER COLLATION "en_US" REFRESH VERSION;
```

A concurrent rebuild builds each replacement index alongside the original, so
the volume needs room for the largest index twice over while it runs; it also
skips the system catalogues, which is harmless because their text columns use
the `C` collation and are unaffected by the library change. Stopping the
container mid-rebuild is safe, and the next start rebuilds again, but PostgreSQL
may leave invalid indexes named `..._ccnew` behind; they are ignored by queries
and can be dropped with `DROP INDEX CONCURRENTLY`. Measured on a development
database with 913 indexes (11 MB) the rebuild took a few seconds, and the time
grows with index size. Take a backup before the upgrade regardless, and rehearse
on a copy of the production volume first. Rolling back to the previous image on
the same volume works (same PostgreSQL major); the old image then sees recorded
version 2.36 against its own 2.31 and the check would rebuild again on the next
start of the new image.
