#!/bin/bash
set -euo pipefail

# Runs only for an empty PostgreSQL data directory; existing roles are untouched.
: "${DB_PASSWORD:?Set DB_PASSWORD for the application database role}"
: "${DB_SUPERUSER_PASSWORD:?Set DB_SUPERUSER_PASSWORD for the database administrator}"

psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -f /docker-entrypoint-initdb.d/permissions.psql
