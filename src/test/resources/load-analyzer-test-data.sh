#!/bin/bash
# Load Analyzer Test Data Script
# Loads analyzer-test-data.sql into the database container
# Task Reference: Test Data Management (Phase 3)

set -e

# Get the database container name (adjust if your container name differs)
DB_CONTAINER="openelisglobal-database"

# Check if container is running
if ! docker ps | grep -q "$DB_CONTAINER"; then
  echo "Error: Database container '$DB_CONTAINER' is not running"
  echo "Please start the database container first:"
  echo "  docker compose -f dev.docker-compose.yml up -d $DB_CONTAINER"
  exit 1
fi

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/analyzer-test-data.sql"

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
  echo "Error: SQL file not found: $SQL_FILE"
  exit 1
fi

echo "Loading analyzer test data from $SQL_FILE..."

# Load SQL file into database
docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims < "$SQL_FILE"

if [ $? -eq 0 ]; then
  echo "✓ Analyzer test data loaded successfully"
  
  # Verify data was loaded
  echo "Verifying data load..."
  docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims -c "
    SELECT 
      (SELECT COUNT(*) FROM analyzer WHERE id IN (1000, 1001, 1002)) as analyzers,
      (SELECT COUNT(*) FROM analyzer_configuration WHERE id LIKE 'CONFIG-%') as configurations,
      (SELECT COUNT(*) FROM analyzer_field WHERE id LIKE 'FIELD-%') as fields,
      (SELECT COUNT(*) FROM analyzer_field_mapping WHERE id LIKE 'MAPPING-%') as mappings,
      (SELECT COUNT(*) FROM analyzer_error WHERE id LIKE 'ERROR-%') as errors;
  "
else
  echo "✗ Error loading analyzer test data"
  exit 1
fi

