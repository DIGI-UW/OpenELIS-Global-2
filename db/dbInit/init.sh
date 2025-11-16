#!/bin/bash
set -e

# Set default values if not provided
: "${DB_PASSWORD:=clinlims}"         # default database name
: "${DB_SUPERUSER_PASSWORD:=superuser}"         # default admin password
# Substitute environment variables in SQL template
envsubst < /docker-entrypoint-initdb.d/1-pgsqlPermissions.sql.template > /docker-entrypoint-initdb.d/1-pgsqlPermissions.sql

# Execute the generated SQL
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /docker-entrypoint-initdb.d/1-pgsqlPermissions.sql
