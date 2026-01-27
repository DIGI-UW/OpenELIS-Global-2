#!/bin/bash
# Load Analyzer Test Data Fixtures
#
# Purpose: Load analyzer-related test data into OpenELIS database
# Reference: specs/004-astm-analyzer-mapping/plan.md
#
# This script uses DBUnit XML as the SINGLE SOURCE OF TRUTH for test data.
# The DBUnit dataset is: testdata/analyzer-mapping-test-data.xml
#
# Usage:
#   ./load-analyzer-test-data.sh [options]
#
# Options:
#   --reset       Use CLEAN_INSERT (delete existing, then insert)
#   --refresh     Use REFRESH (update existing, insert new) - DEFAULT
#   --no-verify   Skip post-load verification
#   --help        Show this help message
#
# Prerequisites:
#   - Maven installed and on PATH
#   - Test classes compiled (or script will compile them)
#   - Database accessible at localhost:15432

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
DATASET="testdata/analyzer-mapping-test-data.xml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Options
OPERATION="REFRESH"
VERIFY=true

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --reset)
            OPERATION="CLEAN_INSERT"
            shift
            ;;
        --refresh)
            OPERATION="REFRESH"
            shift
            ;;
        --no-verify)
            VERIFY=false
            shift
            ;;
        --help)
            echo "Usage: $0 [--reset] [--refresh] [--no-verify] [--help]"
            echo ""
            echo "Options:"
            echo "  --reset       Use CLEAN_INSERT (delete existing, then insert)"
            echo "  --refresh     Use REFRESH (update existing, insert new) - DEFAULT"
            echo "  --no-verify   Skip post-load verification"
            echo "  --help        Show this help message"
            echo ""
            echo "Data Source: ${DATASET} (DBUnit XML - single source of truth)"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo ""
echo "======================================"
echo "  OpenELIS Analyzer Test Data Loader"
echo "======================================"
echo ""
echo -e "${GREEN}[INFO]${NC} Dataset: ${DATASET}"
echo -e "${GREEN}[INFO]${NC} Operation: ${OPERATION}"
echo ""

# Check if test classes are compiled
if [ ! -d "${PROJECT_ROOT}/target/test-classes" ]; then
    echo -e "${YELLOW}[INFO]${NC} Test classes not compiled. Compiling..."
    cd "$PROJECT_ROOT"
    mvn test-compile -DskipTests -q
fi

# Run the DBUnit loader
cd "$PROJECT_ROOT"
mvn exec:java \
    -Dexec.mainClass="org.openelisglobal.testutils.DbUnitDatasetLoader" \
    -Dexec.classpathScope=test \
    -Dexec.args="${DATASET} ${OPERATION}" \
    -q

# Verify if requested
if [ "$VERIFY" = true ]; then
    echo ""
    echo -e "${GREEN}[INFO]${NC} Verifying loaded data..."

    # Check if we can connect to the database via Docker
    if docker ps --format '{{.Names}}' | grep -q 'openelisglobal-database'; then
        docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT 'analyzers' as entity, COUNT(*) as count FROM analyzer WHERE id >= 1000
UNION ALL SELECT 'configurations', COUNT(*) FROM analyzer_configuration WHERE id LIKE 'CONFIG-%'
UNION ALL SELECT 'fields', COUNT(*) FROM analyzer_field WHERE id LIKE 'FIELD-%'
UNION ALL SELECT 'mappings', COUNT(*) FROM analyzer_field_mapping WHERE id LIKE 'MAPPING-%'
UNION ALL SELECT 'qual_mappings', COUNT(*) FROM qualitative_result_mapping WHERE id LIKE 'QUAL-%'
UNION ALL SELECT 'unit_mappings', COUNT(*) FROM unit_mapping WHERE id LIKE 'UNIT-%'
UNION ALL SELECT 'errors', COUNT(*) FROM analyzer_error WHERE id LIKE 'ERR-%';
" 2>/dev/null
    else
        echo -e "${YELLOW}[WARN]${NC} Cannot verify - database container not accessible"
    fi
fi

echo ""
echo -e "${GREEN}[INFO]${NC} Analyzer test data loading complete!"
echo ""
echo "Next steps:"
echo "  1. Start ASTM mock server: docker compose -f docker-compose.astm-test.yml up -d"
echo "  2. Access OpenELIS: https://localhost/"
echo "  3. Navigate to Analyzers to test mappings"
echo ""
