#!/bin/bash

# Load Storage Test Data Fixtures
# Usage: ./load-storage-test-data.sh

set -e

echo "======================================"
echo "Storage Test Data Loader"
echo "======================================"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "ERROR: psql not found. Please install PostgreSQL client."
    exit 1
fi

# Database connection parameters
DB_USER="${DB_USER:-clinlims}"
DB_NAME="${DB_NAME:-clinlims}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/storage-test-data.sql"

echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
echo "User: $DB_USER"
echo "SQL File: $SQL_FILE"
echo ""

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo "ERROR: SQL file not found: $SQL_FILE"
    exit 1
fi

echo "Loading test data..."
echo ""

# Execute SQL script
psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -f "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "======================================"
    echo "✅ Test data loaded successfully!"
    echo "======================================"
    echo ""
    echo "Test Hierarchy Summary:"
    echo "- 3 Rooms (Main, Secondary, Inactive)"
    echo "- 4 Devices (Freezer, Refrigerator, Cabinet, Inactive Freezer)"
    echo "- 4 Shelves"
    echo "- 4 Racks"
    echo "- 100+ Positions (mix of occupied/unoccupied)"
    echo ""
    echo "Ready for integration testing!"
    echo ""
    echo "Quick Test:"
    echo "  curl -k https://localhost/rest/storage/rooms"
    echo "  curl -k https://localhost/rest/storage/positions?rackId=30&occupied=false"
    echo ""
else
    echo ""
    echo "======================================"
    echo "❌ Error loading test data"
    echo "======================================"
    echo ""
    echo "Troubleshooting:"
    echo "1. Verify PostgreSQL is running"
    echo "2. Check database credentials"
    echo "3. Ensure storage tables exist (run Liquibase migrations first)"
    echo ""
    exit 1
fi

