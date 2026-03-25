#!/bin/bash
# Load DBUnit Dataset
#
# Purpose: Load DBUnit XML datasets into the database for manual/E2E testing
# This is the single source of truth loader - uses DBUnit XML files from testdata/
#
# Usage:
#   ./load-dbunit-dataset.sh <dataset-name> [operation]
#
# Examples:
#   ./load-dbunit-dataset.sh analyzer-mapping-test-data
#   ./load-dbunit-dataset.sh analyzer-mapping-test-data REFRESH
#   ./load-dbunit-dataset.sh storage-e2e CLEAN_INSERT
#
# Operations:
#   CLEAN_INSERT  - Delete existing rows, then insert (default)
#   INSERT        - Insert only (fails if rows exist)
#   REFRESH       - Update existing, insert new
#   UPDATE        - Update existing only
#   DELETE        - Delete rows in dataset
#
# Prerequisites:
#   - Maven installed and on PATH
#   - Test classes compiled (mvn test-compile)
#   - Database accessible at localhost:15432

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Arguments
DATASET_NAME="${1:-}"
OPERATION="${2:-CLEAN_INSERT}"

if [ -z "$DATASET_NAME" ]; then
    echo -e "${RED}Error: Dataset name required${NC}"
    echo ""
    echo "Usage: $0 <dataset-name> [operation]"
    echo ""
    echo "Available datasets:"
    ls -1 "${SCRIPT_DIR}/testdata/"*.xml 2>/dev/null | xargs -n1 basename | sed 's/.xml$//' | while read name; do
        echo "  - $name"
    done
    echo ""
    echo "Operations: CLEAN_INSERT (default), INSERT, REFRESH, UPDATE, DELETE"
    exit 1
fi

# Add .xml extension if not present
if [[ ! "$DATASET_NAME" == *.xml ]]; then
    DATASET_PATH="testdata/${DATASET_NAME}.xml"
else
    DATASET_PATH="testdata/${DATASET_NAME}"
fi

# Verify dataset exists
FULL_PATH="${SCRIPT_DIR}/${DATASET_PATH}"
if [ ! -f "$FULL_PATH" ]; then
    echo -e "${RED}Error: Dataset not found: ${FULL_PATH}${NC}"
    exit 1
fi

echo -e "${GREEN}Loading DBUnit dataset: ${DATASET_NAME}${NC}"
echo "Operation: ${OPERATION}"
echo ""

# Check if test classes are compiled
if [ ! -d "${PROJECT_ROOT}/target/test-classes" ]; then
    echo -e "${YELLOW}Test classes not compiled. Compiling...${NC}"
    cd "$PROJECT_ROOT"
    mvn test-compile -DskipTests -q
fi

# Run the loader
cd "$PROJECT_ROOT"
mvn exec:java \
    -Dexec.mainClass="org.openelisglobal.testutils.DbUnitDatasetLoader" \
    -Dexec.classpathScope=test \
    -Dexec.args="${DATASET_PATH} ${OPERATION}" \
    -q

echo ""
echo -e "${GREEN}Done!${NC}"
