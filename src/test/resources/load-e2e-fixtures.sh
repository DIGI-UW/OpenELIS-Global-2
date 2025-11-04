#!/bin/bash

# Load E2E Test Fixtures (Storage Hierarchy + Patients + Samples)
# This script loads complete test fixtures for manual/E2E testing
# Usage: ./load-e2e-fixtures.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/storage-test-data.sql"

echo "======================================"
echo "Loading E2E Test Fixtures"
echo "======================================"
echo ""
echo "SQL File: $SQL_FILE"
echo ""

# Check if running in Docker environment
if command -v docker &> /dev/null; then
  if docker ps | grep -q openelisglobal-database; then
    echo "Loading fixtures via Docker..."
    docker exec -i openelisglobal-database psql -U clinlims -d clinlims < "$SQL_FILE"
    echo ""
    echo "✅ Fixtures loaded successfully!"
    echo ""
    echo "Verifying..."
    docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
      SELECT 'Patients' AS type, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
      UNION ALL
      SELECT 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'
      UNION ALL
      SELECT 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000;
    "
  else
    echo "ERROR: Docker container 'openelisglobal-database' not found"
    exit 1
  fi
else
  echo "ERROR: Docker not found. Please install Docker or use psql directly."
  exit 1
fi

