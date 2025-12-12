#!/bin/bash
# Load Analyzer Test Data Fixtures
#
# Purpose: Load analyzer-related test data into OpenELIS database
# Reference: specs/004-astm-analyzer-mapping/plan.md
#
# Usage:
#   ./load-analyzer-test-data.sh [options]
#
# Options:
#   --reset       Clear existing test data before loading
#   --no-verify   Skip post-load verification
#   --help        Show this help message
#
# Prerequisites:
#   - Docker container 'openelisglobal-database' running
#   - PostgreSQL client available (via Docker exec)

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/analyzer-test-data.sql"
CONTAINER_NAME="openelisglobal-database"
DB_NAME="clinlims"
DB_USER="clinlims"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Options
RESET=false
VERIFY=true

# Parse arguments
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
        --help)
            echo "Usage: $0 [--reset] [--no-verify] [--help]"
            echo ""
            echo "Options:"
            echo "  --reset       Clear existing test data before loading"
            echo "  --no-verify   Skip post-load verification"
            echo "  --help        Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_container() {
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log_error "Database container '${CONTAINER_NAME}' is not running"
        log_info "Start with: docker compose -f dev.docker-compose.yml up -d"
        exit 1
    fi
}

check_sql_file() {
    if [[ ! -f "$SQL_FILE" ]]; then
        log_error "SQL file not found: $SQL_FILE"
        exit 1
    fi
}

reset_test_data() {
    log_info "Resetting existing test data (IDs >= 1000)..."

    docker exec -i ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} << 'EOF'
-- Delete in reverse dependency order
DELETE FROM analyzer_error WHERE analyzer_id >= 1000;
DELETE FROM qualitative_result_mapping WHERE analyzer_field_id IN (
    SELECT id FROM analyzer_field WHERE analyzer_id >= 1000
);
DELETE FROM unit_mapping WHERE analyzer_field_id IN (
    SELECT id FROM analyzer_field WHERE analyzer_id >= 1000
);
DELETE FROM analyzer_field_mapping WHERE analyzer_id >= 1000;
DELETE FROM analyzer_field WHERE analyzer_id >= 1000;
DELETE FROM analyzer_configuration WHERE analyzer_id >= 1000;
DELETE FROM analyzer WHERE id >= 1000;
EOF

    log_info "Test data reset complete"
}

load_sql() {
    log_info "Loading analyzer test data from: $(basename $SQL_FILE)"

    docker exec -i ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} < "$SQL_FILE"

    if [[ $? -eq 0 ]]; then
        log_info "SQL file loaded successfully"
    else
        log_error "Failed to load SQL file"
        exit 1
    fi
}

verify_data() {
    log_info "Verifying loaded data..."

    docker exec -i ${CONTAINER_NAME} psql -U ${DB_USER} -d ${DB_NAME} << 'EOF'
\echo '--- Analyzer Count ---'
SELECT COUNT(*) as analyzer_count FROM analyzer WHERE id >= 1000;

\echo '--- Analyzer Configuration Count ---'
SELECT COUNT(*) as config_count FROM analyzer_configuration WHERE analyzer_id >= 1000;

\echo '--- Analyzer Field Count ---'
SELECT COUNT(*) as field_count FROM analyzer_field WHERE analyzer_id >= 1000;

\echo '--- Analyzer Mapping Count ---'
SELECT COUNT(*) as mapping_count FROM analyzer_field_mapping WHERE analyzer_id >= 1000;

\echo '--- Qualitative Mapping Count ---'
SELECT COUNT(*) as qual_mapping_count FROM qualitative_result_mapping
WHERE analyzer_field_id IN (SELECT id FROM analyzer_field WHERE analyzer_id >= 1000);

\echo '--- Unit Mapping Count ---'
SELECT COUNT(*) as unit_mapping_count FROM unit_mapping
WHERE analyzer_field_id IN (SELECT id FROM analyzer_field WHERE analyzer_id >= 1000);

\echo '--- Analyzer Error Count ---'
SELECT COUNT(*) as error_count FROM analyzer_error WHERE analyzer_id >= 1000;

\echo '--- Errors by Status ---'
SELECT status, COUNT(*) as count FROM analyzer_error
WHERE analyzer_id >= 1000 GROUP BY status ORDER BY status;

\echo '--- Errors by Type ---'
SELECT error_type, COUNT(*) as count FROM analyzer_error
WHERE analyzer_id >= 1000 GROUP BY error_type ORDER BY error_type;
EOF
}

# Main execution
echo ""
echo "======================================"
echo "  OpenELIS Analyzer Test Data Loader"
echo "======================================"
echo ""

# Pre-flight checks
check_container
check_sql_file

# Reset if requested
if [[ "$RESET" == "true" ]]; then
    reset_test_data
fi

# Load data
load_sql

# Verify if not skipped
if [[ "$VERIFY" == "true" ]]; then
    echo ""
    verify_data
fi

echo ""
log_info "Analyzer test data loading complete!"
echo ""
echo "Next steps:"
echo "  1. Start ASTM mock server: docker compose -f docker-compose.astm-test.yml up -d"
echo "  2. Access OpenELIS: https://localhost/"
echo "  3. Navigate to Analyzers to test mappings"
echo ""
