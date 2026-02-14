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
DATASET_004="testdata/analyzer-mapping-test-data.xml"
DATASET_011="testdata/madagascar-analyzer-test-data.xml"
DATASET_GLOBAL="testdata/global-analyzer-inventory.xml"
DATASET=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Options
OPERATION="REFRESH"
VERIFY=true
LOAD_MODE="004"  # default: Feature 004 only

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
        --dataset-004)
            LOAD_MODE="004"
            shift
            ;;
        --dataset-011)
            LOAD_MODE="011"
            shift
            ;;
        --all)
            LOAD_MODE="ALL"
            shift
            ;;
        --all-plugins)
            LOAD_MODE="GLOBAL"
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Operation Options:"
            echo "  --reset       Use CLEAN_INSERT (delete existing, then insert)"
            echo "  --refresh     Use REFRESH (update existing, insert new) - DEFAULT"
            echo "  --no-verify   Skip post-load verification"
            echo ""
            echo "Dataset Options:"
            echo "  --dataset-004 Load Feature 004 fixtures (IDs 1000-1004) - DEFAULT"
            echo "  --dataset-011 Load Feature 011 fixtures (IDs 2000-2012)"
            echo "  --all         Load both Feature 004 and 011 fixtures"
            echo "  --all-plugins Load global analyzer inventory (IDs 3000-3035, all 36 plugins)"
            echo ""
            echo "  --help        Show this help message"
            echo ""
            echo "Data Sources:"
            echo "  Feature 004: ${DATASET_004}"
            echo "  Feature 011: ${DATASET_011}"
            echo "  Global:      ${DATASET_GLOBAL}"
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
echo -e "${GREEN}[INFO]${NC} Load Mode: ${LOAD_MODE}"
echo -e "${GREEN}[INFO]${NC} Operation: ${OPERATION}"
echo ""

# Check if test classes are compiled
if [ ! -d "${PROJECT_ROOT}/target/test-classes" ]; then
    echo -e "${YELLOW}[INFO]${NC} Test classes not compiled. Compiling..."
    cd "$PROJECT_ROOT"
    mvn test-compile -DskipTests -q
fi

# Determine which datasets to load
case $LOAD_MODE in
    "004")
        DATASETS=("$DATASET_004")
        ;;
    "011")
        DATASETS=("$DATASET_011")
        ;;
    "ALL")
        DATASETS=("$DATASET_004" "$DATASET_011")
        ;;
    "GLOBAL")
        DATASETS=("$DATASET_GLOBAL")
        ;;
    *)
        echo -e "${RED}[ERROR]${NC} Invalid LOAD_MODE: ${LOAD_MODE}"
        exit 1
        ;;
esac

# Run the DBUnit loader for each dataset.
# Dataset 011 (madagascar): use INSERT to avoid REFRESH column-case issues with PostgreSQL/DBUnit.
# Do not use CLEAN_INSERT (would delete analyzer rows still referenced by 004 analyzer_field).
cd "$PROJECT_ROOT"
for DATASET in "${DATASETS[@]}"; do
    OP="$OPERATION"
    if [[ "$DATASET" == *"madagascar-analyzer-test-data"* ]]; then
        OP="INSERT"
    fi
    echo -e "${GREEN}[INFO]${NC} Loading dataset: ${DATASET} (operation: ${OP})"
    mvn exec:java \
        -Dexec.mainClass="org.openelisglobal.testutils.DbUnitDatasetLoader" \
        -Dexec.classpathScope=test \
        -Dexec.args="${DATASET} ${OP}" \
        -q
    echo ""
done

# Verify if requested
if [ "$VERIFY" = true ]; then
    echo ""
    echo -e "${GREEN}[INFO]${NC} Verifying loaded data..."

    DB_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E '^openelisglobal-database$|analyzer-harness.*-db-' | head -1)
    if [ -n "$DB_CONTAINER" ]; then
        echo ""
        echo "Feature 004 Data (IDs 1000-1004):"
        docker exec "$DB_CONTAINER" psql -U clinlims -d clinlims -c "
SELECT 'analyzers' as entity, COUNT(*) as count FROM analyzer WHERE id BETWEEN 1000 AND 1004
UNION ALL SELECT 'configurations', COUNT(*) FROM analyzer_configuration WHERE id LIKE 'CONFIG-%'
UNION ALL SELECT 'fields', COUNT(*) FROM analyzer_field WHERE id LIKE 'FIELD-%'
UNION ALL SELECT 'mappings', COUNT(*) FROM analyzer_field_mapping WHERE id LIKE 'MAPPING-%'
UNION ALL SELECT 'qual_mappings', COUNT(*) FROM qualitative_result_mapping WHERE id LIKE 'QUAL-%'
UNION ALL SELECT 'unit_mappings', COUNT(*) FROM unit_mapping WHERE id LIKE 'UNIT-%'
UNION ALL SELECT 'errors', COUNT(*) FROM analyzer_error WHERE id LIKE 'ERR-%';
" 2>/dev/null

        echo ""
        echo "Feature 011 Data (IDs 2000-2011):"
        docker exec "$DB_CONTAINER" psql -U clinlims -d clinlims -c "
SELECT 'analyzers' as entity, COUNT(*) as count FROM analyzer WHERE id BETWEEN 2000 AND 2011
UNION ALL SELECT 'serial_configs', COUNT(*) FROM serial_port_configuration WHERE id LIKE 'SERIAL-20%'
UNION ALL SELECT 'file_configs', COUNT(*) FROM file_import_configuration WHERE id LIKE 'FILE-20%';
" 2>/dev/null
    else
        echo -e "${YELLOW}[WARN]${NC} Cannot verify - database container not accessible"
    fi
fi

echo ""
echo -e "${GREEN}[INFO]${NC} Analyzer test data loading complete!"
echo ""
echo "Next steps:"
echo "  1. Start mock server: docker compose -f docker-compose.analyzer-test.yml up -d"
echo "  2. Access OpenELIS: https://localhost/"
echo "  3. Navigate to Analyzer Dashboard to view loaded analyzers"
echo ""
case $LOAD_MODE in
    "004")
        echo "Loaded: Feature 004 fixtures (IDs 1000-1004)"
        ;;
    "011")
        echo "Loaded: Feature 011 fixtures (IDs 2000-2011)"
        echo "  - 11 analyzers"
        echo "  - 5 serial port configurations"
        echo "  - 3 file import configurations"
        ;;
    "ALL")
        echo "Loaded: Both Feature 004 and 011 fixtures"
        ;;
esac
echo ""
