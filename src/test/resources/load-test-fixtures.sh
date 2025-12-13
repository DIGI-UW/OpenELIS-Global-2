#!/bin/bash

# Load Test Fixtures for OpenELIS Global
# Single unified script for loading ALL E2E test fixtures
# Supports both Docker and direct psql connections
# Usage: ./load-test-fixtures.sh [--reset] [--no-verify]
#
# Files loaded (in order):
#   1. e2e-foundational-data.sql - Providers, Organizations (base data for ALL tests)
#   2. DBUnit datasets (storage-e2e.xml, user-role.xml) - Storage hierarchy + E2E test data
#      (same source-of-truth as JUnit tests, loaded via DbUnitFixtureLoader)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
FOUNDATIONAL_SQL_FILE="$SCRIPT_DIR/e2e-foundational-data.sql"
RESET_SCRIPT="$SCRIPT_DIR/reset-test-database.sh"

RESET=false
VERIFY=true

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --reset)
            RESET=true
            shift
            ;;
        --no-verify)
            VERIFY=false
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--reset] [--no-verify]"
            exit 1
            ;;
    esac
done

echo "======================================"
echo "Loading Test Fixtures"
echo "======================================"
echo ""
echo "Foundational SQL: $FOUNDATIONAL_SQL_FILE"
echo "Storage fixtures: DBUnit datasets (storage-e2e.xml, user-role.xml)"
if [ "$RESET" = true ]; then
    echo "Reset: Enabled (will reset test data before loading)"
fi
if [ "$VERIFY" = true ]; then
    echo "Verify: Enabled (will verify fixtures after loading)"
fi
echo ""

# Check if foundational SQL file exists
if [ ! -f "$FOUNDATIONAL_SQL_FILE" ]; then
    echo "ERROR: Foundational SQL file not found: $FOUNDATIONAL_SQL_FILE"
    exit 1
fi

# Check if Maven is available (needed for DBUnit loader)
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven (mvn) not found. Required for DBUnit fixture loader."
    exit 1
fi

# Reset database if requested
if [ "$RESET" = true ]; then
    if [ ! -f "$RESET_SCRIPT" ]; then
        echo "ERROR: Reset script not found: $RESET_SCRIPT"
        exit 1
    fi
    echo "Resetting test database..."
    bash "$RESET_SCRIPT" --force
    echo ""
fi

# Dependency check function with retry logic
check_dependencies() {
    local USE_DOCKER=$1
    local DB_USER=$2
    local DB_NAME=$3
    local DB_HOST=$4
    local DB_PORT=$5

    local MAX_RETRIES=10
    local RETRY_DELAY=3
    local RETRY_COUNT=0

    echo "Checking dependencies..."

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if [ "$USE_DOCKER" = true ]; then
            TYPE_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM type_of_sample;" 2>/dev/null | tr -d '[:space:]' || echo "0")
            # Check for required statuses: Entered (any type), Not Tested (ANALYSIS), Finalized (ANALYSIS)
            # Note: Entered may be EXTERNAL_ORDER or SAMPLE depending on database initialization
            STATUS_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM status_of_sample WHERE (name = 'Entered' OR (name IN ('Not Tested', 'Finalized') AND status_type = 'ANALYSIS'));" 2>/dev/null | tr -d '[:space:]' || echo "0")
            # Check storage hierarchy exists (from DBUnit fixtures)
            ROOM_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" 2>/dev/null | tr -d '[:space:]' || echo "0")
        else
            TYPE_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM type_of_sample;" 2>/dev/null | tr -d '[:space:]' || echo "0")
            STATUS_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM status_of_sample WHERE (name = 'Entered' OR (name IN ('Not Tested', 'Finalized') AND status_type = 'ANALYSIS'));" 2>/dev/null | tr -d '[:space:]' || echo "0")
            # Check storage hierarchy exists (from DBUnit fixtures)
            ROOM_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" 2>/dev/null | tr -d '[:space:]' || echo "0")
        fi

        # Check if all dependencies are met
        # Note: ROOM_COUNT check is optional (will be loaded by DBUnit loader if missing)
        if [ "$TYPE_COUNT" -ge 3 ] && [ "$STATUS_COUNT" -ge 3 ]; then
            if [ "$ROOM_COUNT" -ge 3 ]; then
                echo "✅ Dependencies verified (type_of_sample: $TYPE_COUNT rows, status_of_sample: required statuses present, storage hierarchy: $ROOM_COUNT rooms)"
            else
                echo "✅ Dependencies verified (type_of_sample: $TYPE_COUNT rows, status_of_sample: required statuses present)"
                echo "   Note: Storage hierarchy will be loaded by DBUnit loader"
            fi
            echo ""
            return 0
        fi

        # If not all dependencies met, retry
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            echo "⚠️  Dependencies not ready (attempt $RETRY_COUNT/$MAX_RETRIES):"
            echo "   type_of_sample: $TYPE_COUNT rows (need 3+)"
            echo "   status_of_sample: $STATUS_COUNT matching rows (need 3+)"
            echo "   storage hierarchy: $ROOM_COUNT rooms (need 3+)"
            echo "   Waiting ${RETRY_DELAY}s for Liquibase to complete..."
            sleep $RETRY_DELAY
        fi
    done

    # Final check - report specific errors
    if [ "$TYPE_COUNT" -lt 3 ]; then
        echo "ERROR: type_of_sample table has fewer than 3 rows ($TYPE_COUNT). Required for test fixtures."
        echo "Please ensure database is properly initialized with sample types."
        exit 1
    fi

    if [ "$STATUS_COUNT" -lt 3 ]; then
        echo "ERROR: status_of_sample table missing required statuses. Found $STATUS_COUNT matching rows, need at least 3."
        echo "Required statuses: 'Entered' (any type), 'Not Tested' (ANALYSIS), 'Finalized' (ANALYSIS)"
        echo "Please ensure database is properly initialized with status values."
        exit 1
    fi

    # Note: Storage hierarchy check removed - it will be loaded by DBUnit loader
    # (ROOM_COUNT check is informational only, not a hard requirement)
}

# Verification function
verify_fixtures() {
    local USE_DOCKER=$1
    local DB_USER=$2
    local DB_NAME=$3
    local DB_HOST=$4
    local DB_PORT=$5

    echo "Verifying fixture data..."
    echo ""

    if [ "$USE_DOCKER" = true ]; then
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
            SELECT '', 'Boxes', COUNT(*) FROM storage_box WHERE id BETWEEN 100 AND 10000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        echo ""

        # Verify E2E test data
        docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "
            SELECT
                'E2E Test Data' AS category,
                'Patients' AS type, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'DEV0100%'
            UNION ALL
            SELECT '', 'Sample Items', COUNT(*) FROM sample_item WHERE id BETWEEN 10000 AND 20000
            UNION ALL
            SELECT '', 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000
            UNION ALL
            SELECT '', 'Analyses', COUNT(*) FROM analysis WHERE id BETWEEN 20000 AND 30000
            UNION ALL
            SELECT '', 'Results', COUNT(*) FROM result WHERE id BETWEEN 30000 AND 40000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        # Check specific counts
        ROOM_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" | tr -d '[:space:]')
        SAMPLE_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'DEV0100%';" | tr -d '[:space:]')
        PATIENT_COUNT=$(docker exec openelisglobal-database psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';" | tr -d '[:space:]')
    else
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
            SELECT '', 'Boxes', COUNT(*) FROM storage_box WHERE id BETWEEN 100 AND 10000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        echo ""

        # Verify E2E test data
        psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "
            SELECT
                'E2E Test Data' AS category,
                'Patients' AS type, COUNT(*) AS count FROM patient WHERE external_id LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'DEV0100%'
            UNION ALL
            SELECT '', 'Sample Items', COUNT(*) FROM sample_item WHERE id BETWEEN 10000 AND 20000
            UNION ALL
            SELECT '', 'Storage Assignments', COUNT(*) FROM sample_storage_assignment WHERE id >= 1000
            UNION ALL
            SELECT '', 'Analyses', COUNT(*) FROM analysis WHERE id BETWEEN 20000 AND 30000
            UNION ALL
            SELECT '', 'Results', COUNT(*) FROM result WHERE id BETWEEN 30000 AND 40000;
        " | sed 's/^[[:space:]]*//' | grep -v '^$'

        # Check specific counts
        ROOM_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" | tr -d '[:space:]')
        SAMPLE_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'DEV0100%';" | tr -d '[:space:]')
        PATIENT_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';" | tr -d '[:space:]')
    fi

    echo ""

    # Validate counts
    if [ "$ROOM_COUNT" -lt 3 ]; then
        echo "⚠️  WARNING: Expected 3 test rooms, found $ROOM_COUNT"
    fi
    if [ "$SAMPLE_COUNT" -lt 10 ]; then
        echo "⚠️  WARNING: Expected 10+ test samples, found $SAMPLE_COUNT"
    fi
    if [ "$PATIENT_COUNT" -lt 3 ]; then
        echo "⚠️  WARNING: Expected 3 test patients, found $PATIENT_COUNT"
    fi
}

# Determine execution method: Docker or direct psql
USE_DOCKER=false
if command -v docker &> /dev/null; then
    if docker ps | grep -q openelisglobal-database; then
        USE_DOCKER=true
        echo "Using Docker container: openelisglobal-database"
    fi
fi

if [ "$USE_DOCKER" = true ]; then
    # Check dependencies before loading
    check_dependencies true "" "" "" ""

    # Load foundational data first (providers, organizations)
    echo "Loading foundational fixtures via Docker..."
    docker exec -i openelisglobal-database psql -U clinlims -d clinlims < "$FOUNDATIONAL_SQL_FILE"

    if [ $? -ne 0 ]; then
        echo ""
        echo "======================================"
        echo "❌ Error loading foundational fixtures"
        echo "======================================"
        exit 1
    fi

    echo "✅ Foundational data loaded (providers, organizations)"
    echo ""

    # Load storage hierarchy + E2E test data via DBUnit (same source as JUnit tests)
    echo "Loading storage fixtures via DBUnit loader..."
    (cd "$PROJECT_ROOT" && mvn -q exec:java \
        -Dexec.mainClass="org.openelisglobal.testutils.DbUnitFixtureLoader" \
        -Dexec.args="--docker testdata/user-role.xml testdata/storage-e2e.xml" \
        -Dexec.classpathScope=test)

    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ All fixtures loaded successfully!"
        echo ""

        if [ "$VERIFY" = true ]; then
            verify_fixtures true "" "" "" ""
            echo "======================================"
            echo "✅ Verification complete!"
            echo "======================================"
        fi

        echo ""
        echo "Test data ready for:"
        echo "  - Manual testing"
        echo "  - E2E testing (Cypress)"
        echo "  - Integration testing"
        echo ""
    else
        echo ""
        echo "======================================"
        echo "❌ Error loading storage fixtures"
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
    DB_PASSWORD="${DB_PASSWORD:-${PGPASSWORD:-clinlims}}"

    echo "Using direct psql connection"
    echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
    echo "User: $DB_USER"
    echo ""

    # Check dependencies before loading
    check_dependencies false "$DB_USER" "$DB_NAME" "$DB_HOST" "$DB_PORT"

    # Load foundational data first
    echo "Loading foundational test data..."
    echo ""
    psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -f "$FOUNDATIONAL_SQL_FILE"

    if [ $? -ne 0 ]; then
        echo ""
        echo "======================================"
        echo "❌ Error loading foundational data"
        echo "======================================"
        exit 1
    fi

    echo "✅ Foundational data loaded (providers, organizations)"
    echo ""

    # Load storage hierarchy + E2E test data via DBUnit (same source as JUnit tests)
    echo "Loading storage fixtures via DBUnit loader..."
    (cd "$PROJECT_ROOT" && mvn -q exec:java \
        -Dexec.mainClass="org.openelisglobal.testutils.DbUnitFixtureLoader" \
        -Dexec.args="--jdbc-url jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME --user $DB_USER --password $DB_PASSWORD testdata/user-role.xml testdata/storage-e2e.xml" \
        -Dexec.classpathScope=test)

    if [ $? -eq 0 ]; then
        echo ""
        echo "======================================"
        echo "✅ All test data loaded successfully!"
        echo "======================================"
        echo ""

        if [ "$VERIFY" = true ]; then
            verify_fixtures false "$DB_USER" "$DB_NAME" "$DB_HOST" "$DB_PORT"
            echo "======================================"
            echo "✅ Verification complete!"
            echo "======================================"
        fi
    else
        echo ""
        echo "======================================"
        echo "❌ Error loading storage test data"
        echo "======================================"
        echo ""
        echo "Troubleshooting:"
        echo "1. Verify PostgreSQL is running"
        echo "2. Check database credentials"
        echo "3. Ensure storage tables exist (run Liquibase migrations first)"
        echo "4. Verify Maven can compile test classes"
        echo ""
        exit 1
    fi
fi
