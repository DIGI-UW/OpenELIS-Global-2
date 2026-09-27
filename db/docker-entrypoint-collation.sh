#!/usr/bin/env bash
# Rebuilds indexes once, online, when the operating system's collation library changed under an
# existing cluster.
#
# Moving the image to another Debian release changes glibc, and with it the sort order of locale
# collations such as en_US.utf8. PostgreSQL 14 records the glibc version for the collations in
# pg_collation but not for a database's default collation, so after the change indexes on text
# columns keep being used silently even though they were built under the old ordering.
#
# The rebuild runs in the background, after PostgreSQL is already accepting connections, and uses
# REINDEX DATABASE ... CONCURRENTLY so no table is locked against reads or writes. This matters
# because the application container starts at the same time as the database and gives up if the
# database is unreachable when its schema migration runs: a rebuild that held start-up would take
# the application down for as long as it lasted, and the larger the database the longer that is.
# Empty volumes (first initialisation) and already-refreshed volumes do nothing at all.
#
# Every failure is logged with the statements to run by hand and is otherwise ignored; the recorded
# collation versions are only refreshed once a rebuild has actually succeeded, so an interrupted run
# is retried on the next start. Set OE_DB_SKIP_COLLATION_REINDEX=true to skip the check entirely.
set -uo pipefail

PGDATA="${PGDATA:-/var/lib/postgresql/data}"
export PGPASSWORD="${PGPASSWORD:-${POSTGRES_PASSWORD:-}}"
READY_TIMEOUT="${OE_DB_COLLATION_READY_TIMEOUT:-600}"

log() { printf '%s [collation-check] %s\n' "$(date -u '+%Y-%m-%d %H:%M:%S UTC')" "$*"; }

as_postgres() {
  if [ "$(id -u)" = '0' ]; then gosu postgres "$@"; else "$@"; fi
}

manual_hint() {
  log "run the rebuild by hand as the postgres superuser instead:"
  log "  REINDEX DATABASE <db> CONCURRENTLY;"
  log "  ALTER COLLATION \"en_US.utf8\" REFRESH VERSION; ALTER COLLATION \"en_US\" REFRESH VERSION;"
}

stale_collations_of() {
  as_postgres psql -X -At -v ON_ERROR_STOP=1 -d "$1" -c \
    "SELECT quote_ident(collname) FROM pg_collation
      WHERE collprovider = 'c' AND collversion IS NOT NULL
        AND collversion IS DISTINCT FROM pg_collation_actual_version(oid)" 2>&1
}

rebuild_when_ready() {
  local waited=0 dbs db qdb stale coll started
  until as_postgres pg_isready -q; do
    waited=$((waited + 2))
    if [ "$waited" -ge "$READY_TIMEOUT" ]; then
      log "PostgreSQL was not accepting connections after ${READY_TIMEOUT}s; skipping the check"
      manual_hint; return 0
    fi
    sleep 2
  done

  if ! dbs=$(as_postgres psql -X -At -v ON_ERROR_STOP=1 -c \
      "SELECT datname FROM pg_database WHERE datallowconn AND NOT datistemplate ORDER BY datname" 2>&1); then
    log "cannot query the cluster as the postgres role ($dbs); skipping the check"
    manual_hint; return 0
  fi

  while IFS= read -r db; do
    [ -z "$db" ] && continue
    if ! stale=$(stale_collations_of "$db"); then
      log "$db: cannot read pg_collation ($stale); skipping this database"; manual_hint; continue
    fi
    if [ -z "$stale" ]; then
      log "$db: collation versions match, nothing to do"; continue
    fi
    log "$db: collation library changed for $(echo "$stale" | tr '\n' ' '); rebuilding all indexes online"
    started=$SECONDS
    qdb=$(as_postgres psql -X -At -d "$db" -c 'SELECT quote_ident(current_database())')
    if ! as_postgres psql -X -q -v ON_ERROR_STOP=1 -d "$db" -c "REINDEX DATABASE CONCURRENTLY $qdb" 2>&1; then
      log "$db: rebuild failed; collation versions left as they were so the next start retries"
      manual_hint; continue
    fi
    while IFS= read -r coll; do
      [ -z "$coll" ] && continue
      as_postgres psql -X -q -v ON_ERROR_STOP=1 -d "$db" -c "ALTER COLLATION $coll REFRESH VERSION" 2>&1 \
        || log "$db: could not refresh $coll"
    done <<< "$stale"
    log "$db: indexes rebuilt and collation versions refreshed in $((SECONDS - started))s"
  done <<< "$dbs"
}

if [ "${1:-}" = 'postgres' ] && [ -s "$PGDATA/PG_VERSION" ] && [ "${OE_DB_SKIP_COLLATION_REINDEX:-false}" != 'true' ]; then
  log "existing cluster found (PostgreSQL $(cat "$PGDATA/PG_VERSION") data directory); the collation check will run once the server is up"
  rebuild_when_ready &
fi

exec docker-entrypoint.sh "$@"
