#!/bin/bash

# Load Test Fixtures for OpenELIS Global
# Single unified script for loading ALL E2E test fixtures
# Supports both Docker and direct psql connections
#
# Usage: ./load-test-fixtures.sh [--reset] [--no-verify] [--profile=PROFILE]
#
# Fixture profiles (--profile=PROFILE):
#   harness  - Core fixtures + analyzer cleanup + HARN-* lane fixtures (default)
#   core     - Foundational + storage fixtures + minimal analyzer safety net + core demo patient
#
# Files loaded (in order):
#   1. e2e-foundational-data.sql - Providers, Organizations (base data for ALL tests)
#   2. Profile fixtures (core/harness specific)
#   3. storage-e2e.xml (DBUnit XML) - Storage hierarchy + E2E test data
#      Converted to SQL on-demand (*.generated.sql files never committed)
#   4. fixtures/analyzer-harness-lane-data.sql - Only for --profile=harness (HARN-* demo accessions)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
FOUNDATIONAL_SQL_FILE="$SCRIPT_DIR/e2e-foundational-data.sql"
ANALYZER_MINIMAL_SQL_FILE="$SCRIPT_DIR/analyzer-minimal.sql"
FILE_IMPORT_E2E_SQL="$SCRIPT_DIR/fixtures/file-import-e2e.sql"
ANALYZER_HARNESS_LANE_SQL_FILE="$SCRIPT_DIR/fixtures/analyzer-harness-lane-data.sql"
RESET_SCRIPT="$SCRIPT_DIR/reset-test-database.sh"

RESET=false
VERIFY=true
PROFILE="harness"

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
        --profile=*)
            PROFILE="${1#*=}"
            if [[ ! "$PROFILE" =~ ^(core|harness)$ ]]; then
                echo "ERROR: Invalid fixture profile: $PROFILE"
                echo "Valid profiles: core, harness"
                exit 1
            fi
            shift
            ;;
        --analyzers=*)
            # TRANSITIONAL COMPAT — remove in follow-up PR after develop's YAML
            # is updated to use --profile. GitHub workflow_run resolves YAML
            # against the default branch, so during the prereq PR's own CI
            # run the stale develop YAML still invokes this script with the
            # old flag. Accept it for one merge cycle, then drop.
            echo "WARNING: --analyzers is deprecated; mapping to --profile=harness for transition."
            PROFILE="harness"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--reset] [--no-verify] [--profile=core|harness]"
            exit 1
            ;;
    esac
done

echo "======================================"
echo "Loading Test Fixtures"
echo "======================================"
echo ""
echo "Foundational SQL: $FOUNDATIONAL_SQL_FILE"
echo "Fixture profile: $PROFILE"
echo "Storage fixtures: DBUnit XML -> Generated SQL (on-demand)"
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

# Check if Python is available (needed for XML->SQL generation)
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 not found. Required for XML->SQL conversion."
    exit 1
fi

# Storage fixture paths
STORAGE_XML="$SCRIPT_DIR/testdata/storage-e2e.xml"
STORAGE_SQL="$SCRIPT_DIR/testdata/storage-e2e.generated.sql"
XML_TO_SQL_SCRIPT="$SCRIPT_DIR/testdata/xml-to-sql.py"

# Check if XML source exists
if [ ! -f "$STORAGE_XML" ]; then
    echo "ERROR: Storage XML fixture not found: $STORAGE_XML"
    exit 1
fi

# Check if converter script exists
if [ ! -f "$XML_TO_SQL_SCRIPT" ]; then
    echo "ERROR: XML->SQL converter not found: $XML_TO_SQL_SCRIPT"
    exit 1
fi

# Generate SQL from DBUnit XML (on-demand, never committed)
echo "Generating SQL from DBUnit XML..."
python3 "$XML_TO_SQL_SCRIPT" "$STORAGE_XML" "$STORAGE_SQL" \
    --on-conflict-do-nothing
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to generate SQL from XML"
    exit 1
fi

echo ""

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

    local MAX_RETRIES=20
    local RETRY_DELAY=3
    local RETRY_COUNT=0

    local TYPE_COUNT
    local STATUS_COUNT
    local ROOM_COUNT
    local STORAGE_ROOM_TABLE_EXISTS
    local ANALYZER_TYPE_TABLE_EXISTS
    local PROVIDER_FHIR_UUID_EXISTS
    local PROVIDER_ACTIVE_EXISTS
    local ORGANIZATION_FHIR_UUID_EXISTS

    echo "Checking dependencies..."

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if [ "$USE_DOCKER" = true ]; then
            TYPE_COUNT=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM type_of_sample;" 2>/dev/null | tr -d '[:space:]' || echo "0")
            # Minimum required status for fixtures is 'Entered' (used by samples/sample_items).
            # Some environments may not seed analysis statuses ('Not Tested', 'Finalized') consistently.
            STATUS_COUNT=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM status_of_sample WHERE name = 'Entered';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            STORAGE_ROOM_TABLE_EXISTS=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT to_regclass('clinlims.storage_room') IS NOT NULL;" 2>/dev/null | tr -d '[:space:]' || echo "f")
            ANALYZER_TYPE_TABLE_EXISTS=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT to_regclass('clinlims.analyzer_type') IS NOT NULL;" 2>/dev/null | tr -d '[:space:]' || echo "f")
            PROVIDER_FHIR_UUID_EXISTS=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'clinlims' AND table_name = 'provider' AND column_name = 'fhir_uuid';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            PROVIDER_ACTIVE_EXISTS=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'clinlims' AND table_name = 'provider' AND column_name = 'active';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            ORGANIZATION_FHIR_UUID_EXISTS=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'clinlims' AND table_name = 'organization' AND column_name = 'fhir_uuid';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            # Check storage hierarchy exists (from DBUnit fixtures) after the table itself is present.
            ROOM_COUNT=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" 2>/dev/null | tr -d '[:space:]' || echo "0")
        else
            TYPE_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM type_of_sample;" 2>/dev/null | tr -d '[:space:]' || echo "0")
            STATUS_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM status_of_sample WHERE name = 'Entered';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            STORAGE_ROOM_TABLE_EXISTS=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT to_regclass('clinlims.storage_room') IS NOT NULL;" 2>/dev/null | tr -d '[:space:]' || echo "f")
            ANALYZER_TYPE_TABLE_EXISTS=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT to_regclass('clinlims.analyzer_type') IS NOT NULL;" 2>/dev/null | tr -d '[:space:]' || echo "f")
            PROVIDER_FHIR_UUID_EXISTS=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'clinlims' AND table_name = 'provider' AND column_name = 'fhir_uuid';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            PROVIDER_ACTIVE_EXISTS=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'clinlims' AND table_name = 'provider' AND column_name = 'active';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            ORGANIZATION_FHIR_UUID_EXISTS=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'clinlims' AND table_name = 'organization' AND column_name = 'fhir_uuid';" 2>/dev/null | tr -d '[:space:]' || echo "0")
            # Check storage hierarchy exists (from DBUnit fixtures) after the table itself is present.
            ROOM_COUNT=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" 2>/dev/null | tr -d '[:space:]' || echo "0")
        fi

        # Foundational fixtures require current-schema columns and storage tables.
        if [ "$TYPE_COUNT" -ge 3 ] && [ "$STATUS_COUNT" -ge 1 ] \
            && [ "$STORAGE_ROOM_TABLE_EXISTS" = "t" ] \
            && [ "$ANALYZER_TYPE_TABLE_EXISTS" = "t" ] \
            && [ "$PROVIDER_FHIR_UUID_EXISTS" -ge 1 ] \
            && [ "$PROVIDER_ACTIVE_EXISTS" -ge 1 ] \
            && [ "$ORGANIZATION_FHIR_UUID_EXISTS" -ge 1 ]; then
            echo "Dependencies verified (type_of_sample: $TYPE_COUNT rows, status_of_sample: required statuses present, provider/organization FHIR columns present, storage_room and analyzer_type tables present)"
            if [ "$ROOM_COUNT" -lt 3 ]; then
                echo "   Note: storage_room table is ready; DBUnit loader will populate fixture rows"
            fi
            echo ""
            return 0
        fi

        # If not all dependencies met, retry
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            echo "WARNING: Dependencies not ready (attempt $RETRY_COUNT/$MAX_RETRIES):"
            echo "   type_of_sample: $TYPE_COUNT rows (need 3+)"
            echo "   status_of_sample: $STATUS_COUNT 'Entered' rows (need 1+)"
            echo "   provider.fhir_uuid column present: $PROVIDER_FHIR_UUID_EXISTS"
            echo "   provider.active column present: $PROVIDER_ACTIVE_EXISTS"
            echo "   organization.fhir_uuid column present: $ORGANIZATION_FHIR_UUID_EXISTS"
            echo "   storage_room table present: $STORAGE_ROOM_TABLE_EXISTS"
            echo "   analyzer_type table present: $ANALYZER_TYPE_TABLE_EXISTS"
            echo "   storage hierarchy rows: $ROOM_COUNT"
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

    if [ "$STATUS_COUNT" -lt 1 ]; then
        echo "ERROR: status_of_sample table missing required status 'Entered'. Found $STATUS_COUNT rows."
        echo "Please ensure database is properly initialized with status values."
        exit 1
    fi

    if [ "$PROVIDER_FHIR_UUID_EXISTS" -lt 1 ] || [ "$PROVIDER_ACTIVE_EXISTS" -lt 1 ]; then
        echo "ERROR: provider schema is not ready for foundational fixtures."
        echo "Required columns missing: fhir_uuid count=$PROVIDER_FHIR_UUID_EXISTS, active count=$PROVIDER_ACTIVE_EXISTS."
        echo "Please ensure Liquibase has finished before loading fixtures."
        exit 1
    fi

    if [ "$ORGANIZATION_FHIR_UUID_EXISTS" -lt 1 ]; then
        echo "ERROR: organization schema is not ready for foundational fixtures."
        echo "Required column missing: fhir_uuid count=$ORGANIZATION_FHIR_UUID_EXISTS."
        echo "Please ensure Liquibase has finished before loading fixtures."
        exit 1
    fi

    if [ "$STORAGE_ROOM_TABLE_EXISTS" != "t" ]; then
        echo "ERROR: storage schema is not ready for DBUnit fixtures."
        echo "Required table missing: storage_room."
        echo "Please ensure Liquibase has finished before loading fixtures."
        exit 1
    fi

    if [ "$ANALYZER_TYPE_TABLE_EXISTS" != "t" ]; then
        echo "ERROR: analyzer schema is not ready for analyzer fixtures."
        echo "Required table missing: analyzer_type."
        echo "Please ensure Liquibase has finished before loading fixtures."
        exit 1
    fi
}

# Helper: Load a SQL file via Docker or psql (reduces duplication)
# Usage: load_sql_file <file_path> <label> [fatal]
# If third arg is "fatal", exit on error; otherwise warn and continue.
load_sql_file() {
    local sql_file="$1"
    local label="$2"
    local fatal="${3:-nonfatal}"

    if [ ! -f "$sql_file" ]; then
        if [ "$fatal" = "fatal" ]; then
            echo "ERROR: $label not found: $sql_file"
            exit 1
        else
            echo "WARNING: $label not found: $sql_file (skipping)"
            return 0
        fi
    fi

    echo "Loading $label..."
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims -v ON_ERROR_STOP=1 < "$sql_file"
    else
        psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -f "$sql_file"
    fi

    if [ $? -eq 0 ]; then
        echo "OK: $label loaded"
    else
        if [ "$fatal" = "fatal" ]; then
            echo ""
            echo "======================================"
            echo "ERROR loading $label"
            echo "======================================"
            exit 1
        else
            echo "WARNING: $label had errors (non-fatal)"
        fi
    fi
    echo ""
}

# Normalize sequences against current table maxima after fixture loading.
# This prevents generated fixture SQL from rewinding sequences below
# pre-existing rows that were intentionally preserved.
normalize_sequences() {
    local sequence_sql="
SELECT setval('storage_room_seq', CAST((SELECT COALESCE(MAX(id), 1000) + 1 FROM storage_room) AS BIGINT), false);
SELECT setval('storage_device_seq', CAST((SELECT COALESCE(MAX(id), 1000) + 1 FROM storage_device) AS BIGINT), false);
SELECT setval('storage_shelf_seq', CAST((SELECT COALESCE(MAX(id), 1000) + 1 FROM storage_shelf) AS BIGINT), false);
SELECT setval('storage_rack_seq', CAST((SELECT COALESCE(MAX(id), 1000) + 1 FROM storage_rack) AS BIGINT), false);
SELECT setval('storage_box_seq', CAST((SELECT COALESCE(MAX(id), 10000) + 1 FROM storage_box) AS BIGINT), false);
SELECT setval('sample_storage_assignment_seq', CAST((SELECT COALESCE(MAX(id), 10000) + 1 FROM sample_storage_assignment) AS BIGINT), false);
SELECT setval('sample_storage_movement_seq', CAST((SELECT COALESCE(MAX(id), 10000) + 1 FROM sample_storage_movement) AS BIGINT), false);
SELECT setval('person_seq', CAST((SELECT COALESCE(MAX(id), 2000) + 1 FROM person) AS BIGINT), false);
SELECT setval('patient_seq', CAST((SELECT COALESCE(MAX(id), 2000) + 1 FROM patient) AS BIGINT), false);
SELECT setval('sample_seq', CAST((SELECT COALESCE(MAX(id), 2000) + 1 FROM sample) AS BIGINT), false);
SELECT setval('sample_human_seq', CAST((SELECT COALESCE(MAX(id), 2000) + 1 FROM sample_human) AS BIGINT), false);
SELECT setval('sample_item_seq', CAST((SELECT COALESCE(MAX(id), 10100) + 1 FROM sample_item) AS BIGINT), false);
SELECT setval('analysis_seq', CAST((SELECT COALESCE(MAX(id), 20000) + 1 FROM analysis) AS BIGINT), false);
SELECT setval('result_seq', CAST((SELECT COALESCE(MAX(id), 30000) + 1 FROM result) AS BIGINT), false);
"

    echo "Normalizing sequences after fixture load..."
    if [ "$USE_DOCKER" = true ]; then
        echo "$sequence_sql" | docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims
    else
        echo "$sequence_sql" | psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT"
    fi

    if [ $? -eq 0 ]; then
        echo "OK: sequences normalized"
    else
        echo "ERROR: failed to normalize sequences after fixture load"
        exit 1
    fi
    echo ""
}

# Load profile fixtures based on --profile
# Runs BEFORE storage-e2e.xml — any fixture that FK-references storage patients
# (e.g. analyzer-harness-lane-data.sql's sample_human → patient 1000) belongs
# in load_profile_lane_fixtures() instead, which runs AFTER storage.
load_profile_fixtures() {
    # Core baseline shared by both profiles
    load_sql_file "$ANALYZER_MINIMAL_SQL_FILE" "analyzer-minimal.sql (3 generic types)" "fatal"
    # NOTE: The demo patient John TEST-Smith / E2E-PAT-001 is provided by
    # testdata/storage-e2e.xml (patient id 1000). Do not also load it from
    # a separate SQL fixture — the external_id/national_id columns are
    # unique and a duplicate insert would conflict.

    # Analyzer cleanup/deactivation is part of both lanes today.
    if [ -f "$FILE_IMPORT_E2E_SQL" ]; then
        load_sql_file "$FILE_IMPORT_E2E_SQL" "file-import-e2e.sql (cleanup + dashboard deactivation)"
    fi
}

# Runs AFTER storage-e2e.xml, for fixtures that FK-reference storage patients.
load_profile_lane_fixtures() {
    if [ "$PROFILE" = "harness" ]; then
        load_sql_file "$ANALYZER_HARNESS_LANE_SQL_FILE" "analyzer harness lane fixtures (HARN-* accessions)" "fatal"
    fi
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
        docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "
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
        docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "
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
        ROOM_COUNT=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');" | tr -d '[:space:]')
        SAMPLE_COUNT=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'DEV0100%';" | tr -d '[:space:]')
        PATIENT_COUNT=$(docker exec "${DB_CONTAINER:-openelisglobal-database}" psql -U clinlims -d clinlims -t -c "SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';" | tr -d '[:space:]')
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
        echo "WARNING: Expected 3 test rooms, found $ROOM_COUNT"
    fi
    if [ "$SAMPLE_COUNT" -lt 10 ]; then
        echo "WARNING: Expected 10+ test samples, found $SAMPLE_COUNT"
    fi
    if [ "$PATIENT_COUNT" -lt 3 ]; then
        echo "WARNING: Expected 3 test patients, found $PATIENT_COUNT"
    fi
}

# Determine execution method: Docker or direct psql
USE_DOCKER=false
DB_CONTAINER=""
if command -v docker &> /dev/null; then
    DB_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E '^openelisglobal-database$|analyzer-harness.*-db-' | head -1)
    if [ -n "$DB_CONTAINER" ]; then
        USE_DOCKER=true
        echo "Using Docker container: $DB_CONTAINER"
    fi
fi

# Set up psql connection parameters (used when USE_DOCKER=false)
if [ "$USE_DOCKER" = false ]; then
    if ! command -v psql &> /dev/null; then
        echo "ERROR: psql not found. Please install PostgreSQL client."
        echo "Alternatively, ensure Docker is running (openelisglobal-database or analyzer-harness DB container)."
        exit 1
    fi

    DB_USER="${DB_USER:-clinlims}"
    DB_NAME="${DB_NAME:-clinlims}"
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-5432}"
    DB_PASSWORD="${DB_PASSWORD:-${PGPASSWORD:-clinlims}}"

    echo "Using direct psql connection"
    echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
    echo "User: $DB_USER"
fi
echo ""

# Check dependencies before loading
if [ "$USE_DOCKER" = true ]; then
    check_dependencies true "" "" "" ""
else
    check_dependencies false "$DB_USER" "$DB_NAME" "$DB_HOST" "$DB_PORT"
fi

# 1. Load foundational data (providers, organizations)
load_sql_file "$FOUNDATIONAL_SQL_FILE" "foundational fixtures (providers, organizations)" "fatal"

# 2. Load profile fixtures (analyzer types, file-import cleanup)
load_profile_fixtures

# 3. Load storage hierarchy + E2E test data via generated SQL
#    (Creates patient id 1000 referenced by lane fixtures below.)
load_sql_file "$STORAGE_SQL" "storage fixtures (generated SQL)" "fatal"

# 4. Load profile lane fixtures (after storage — these FK-ref storage patients)
load_profile_lane_fixtures

normalize_sequences

echo "======================================"
echo "All fixtures loaded successfully!"
echo "======================================"
echo ""

if [ "$VERIFY" = true ]; then
    if [ "$USE_DOCKER" = true ]; then
        verify_fixtures true "" "" "" ""
    else
        verify_fixtures false "$DB_USER" "$DB_NAME" "$DB_HOST" "$DB_PORT"
    fi
    echo "======================================"
    echo "Verification complete!"
    echo "======================================"
fi

echo ""
echo "Test data ready for:"
echo "  - Manual testing"
echo "  - E2E testing (Playwright)"
echo "  - Integration testing"
echo ""
