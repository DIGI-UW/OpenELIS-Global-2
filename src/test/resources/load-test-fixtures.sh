#!/bin/bash

# Load Test Fixtures for OpenELIS Global
# Single unified script for loading storage + E2E test fixtures
# Supports both Docker and direct psql connections
# Usage: ./load-test-fixtures.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/storage-test-data.sql"

echo "======================================"
echo "Loading Test Fixtures"
echo "======================================"
echo ""
echo "SQL File: $SQL_FILE"
echo ""

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo "ERROR: SQL file not found: $SQL_FILE"
    exit 1
fi

# Determine execution method: Docker or direct psql
USE_DOCKER=false
if command -v docker &> /dev/null; then
    if docker ps | grep -q openelisglobal-database; then
        USE_DOCKER=true
        echo "Using Docker container: openelisglobal-database"
    fi
fi

if [ "$USE_DOCKER" = true ]; then
    # Load via Docker
    echo "Loading fixtures via Docker..."
    docker exec -i openelisglobal-database psql -U clinlims -d clinlims < "$SQL_FILE"

    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Fixtures loaded successfully!"
        echo ""
        echo "Verifying fixture data..."
        echo ""

        # Verify storage hierarchy
        docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "
            SELECT
                'Storage Hierarchy' AS category,
                'Rooms' AS type, COUNT(*) AS count FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')
            UNION ALL
            SELECT '', 'Devices', COUNT(*) FROM storage_device WHERE id BETWEEN 10 AND 20
            UNION ALL
            SELECT '', 'Shelves', COUNT(*) FROM storage_shelf WHERE id BETWEEN 20 AND 30
            UNION ALL
            SELECT '', 'Racks', COUNT(*) FROM storage_rack WHERE id BETWEEN 30 AND 40
            UNION ALL
            SELECT '', 'Positions', COUNT(*) FROM storage_position WHERE id BETWEEN 100 AND 10000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        echo ""

        # Verify E2E test data
        docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "
            SELECT
                'E2E Test Data' AS category,
                'Patients' AS type, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Sample Items', COUNT(*) FROM sample_item WHERE id BETWEEN 10000 AND 20000
            UNION ALL
            SELECT '', 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000
            UNION ALL
            SELECT '', 'Analyses', COUNT(*) FROM analysis WHERE id BETWEEN 20000 AND 30000
            UNION ALL
            SELECT '', 'Results', COUNT(*) FROM result WHERE id BETWEEN 30000 AND 40000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        echo ""
        echo "======================================"
        echo "✅ Verification complete!"
        echo "======================================"
        echo ""
        echo "Test data ready for:"
        echo "  - Manual testing"
        echo "  - E2E testing (Cypress)"
        echo "  - Integration testing"
        echo ""
    else
        echo ""
        echo "======================================"
        echo "❌ Error loading fixtures"
        echo "======================================"
        exit 1
    fi
else
    # Use direct psql connection
    if ! command -v psql &> /dev/null; then
        echo "ERROR: psql not found. Please install PostgreSQL client."
        echo "Alternatively, ensure Docker is running with openelisglobal-database container."
        exit 1
    fi

    # Database connection parameters
    DB_USER="${DB_USER:-clinlims}"
    DB_NAME="${DB_NAME:-clinlims}"
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-5432}"

    echo "Using direct psql connection"
    echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
    echo "User: $DB_USER"
    echo ""
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
        echo "Verifying fixture data..."
        echo ""

        # Verify storage hierarchy
        psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "
            SELECT
                'Storage Hierarchy' AS category,
                'Rooms' AS type, COUNT(*) AS count FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')
            UNION ALL
            SELECT '', 'Devices', COUNT(*) FROM storage_device WHERE id BETWEEN 10 AND 20
            UNION ALL
            SELECT '', 'Shelves', COUNT(*) FROM storage_shelf WHERE id BETWEEN 20 AND 30
            UNION ALL
            SELECT '', 'Racks', COUNT(*) FROM storage_rack WHERE id BETWEEN 30 AND 40
            UNION ALL
            SELECT '', 'Positions', COUNT(*) FROM storage_position WHERE id BETWEEN 100 AND 10000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        echo ""

        # Verify E2E test data
        psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "
            SELECT
                'E2E Test Data' AS category,
                'Patients' AS type, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Sample Items', COUNT(*) FROM sample_item WHERE id BETWEEN 10000 AND 20000
            UNION ALL
            SELECT '', 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000
            UNION ALL
            SELECT '', 'Analyses', COUNT(*) FROM analysis WHERE id BETWEEN 20000 AND 30000
            UNION ALL
            SELECT '', 'Results', COUNT(*) FROM result WHERE id BETWEEN 30000 AND 40000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        echo ""
        echo "======================================"
        echo "✅ Verification complete!"
        echo "======================================"
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
fi
