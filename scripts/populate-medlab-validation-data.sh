#!/bin/bash
#
# populate-medlab-validation-data.sh
#
# Populates test patients and electronic orders for MedLab manifest import validation.
# This script runs psql commands inside the Docker container.
#
# Usage:
#   ./scripts/populate-medlab-validation-data.sh [OPTIONS]
#
# Options:
#   --container     Docker container name (default: openelisglobal-database)
#   -d, --database  Database name (default: clinlims)
#   -U, --user      Database user (default: clinlims)
#   -c, --clean     Clean existing test data before inserting
#   --help          Show this help message
#
# After running this script, import test-data/medlab_manifest_validation.csv
# to test the validation workflow.
#

set -e

# Default values
CONTAINER="${CONTAINER:-openelisglobal-database}"
DB_NAME="${DB_NAME:-clinlims}"
DB_USER="${DB_USER:-clinlims}"
CLEAN_FIRST=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --container)
            CONTAINER="$2"
            shift 2
            ;;
        -d|--database)
            DB_NAME="$2"
            shift 2
            ;;
        -U|--user)
            DB_USER="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN_FIRST=true
            shift
            ;;
        --help)
            head -22 "$0" | tail -19
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Check if docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: docker command not found. Please install Docker.${NC}"
    exit 1
fi

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo -e "${RED}Error: Container '${CONTAINER}' is not running.${NC}"
    echo "Running containers:"
    docker ps --format '  {{.Names}}'
    exit 1
fi

echo -e "${GREEN}=== MedLab Validation Test Data Population Script ===${NC}"
echo "Container: ${CONTAINER}"
echo "Database: ${DB_NAME}"
echo "User: ${DB_USER}"
echo ""

# Function to execute SQL via Docker
execute_sql() {
    docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
}

# Clean existing test data if requested
if [ "$CLEAN_FIRST" = true ]; then
    echo -e "${YELLOW}Cleaning existing MedLab test data...${NC}"
    execute_sql <<'EOF'
-- Clean in reverse order (orders depend on patients)
DELETE FROM electronic_order WHERE external_id LIKE 'ORD-VALID-%' OR external_id LIKE 'ORD-MISMATCH-%';
DELETE FROM patient WHERE external_id LIKE 'PAT-VALID-%';
DELETE FROM person WHERE first_name IN ('John', 'Jane', 'Bob', 'Alice')
  AND last_name IN ('Doe', 'Smith', 'Wilson', 'Brown');
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
    echo ""
fi

echo -e "${YELLOW}Creating Person records...${NC}"
execute_sql <<'EOF'
-- Person records (required for Patient)
-- Using DO block to handle sequence and avoid conflicts

DO $$
DECLARE
    john_id INTEGER;
    jane_id INTEGER;
    bob_id INTEGER;
    alice_id INTEGER;
BEGIN
    -- Check if persons already exist
    SELECT id INTO john_id FROM person WHERE first_name = 'John' AND last_name = 'Doe' LIMIT 1;
    SELECT id INTO jane_id FROM person WHERE first_name = 'Jane' AND last_name = 'Smith' LIMIT 1;
    SELECT id INTO bob_id FROM person WHERE first_name = 'Bob' AND last_name = 'Wilson' LIMIT 1;
    SELECT id INTO alice_id FROM person WHERE first_name = 'Alice' AND last_name = 'Brown' LIMIT 1;

    -- Insert only if not exists
    IF john_id IS NULL THEN
        INSERT INTO person (id, first_name, last_name, lastupdated)
        VALUES (nextval('person_seq'), 'John', 'Doe', NOW());
        RAISE NOTICE 'Created person: John Doe';
    ELSE
        RAISE NOTICE 'Person already exists: John Doe (id=%)', john_id;
    END IF;

    IF jane_id IS NULL THEN
        INSERT INTO person (id, first_name, last_name, lastupdated)
        VALUES (nextval('person_seq'), 'Jane', 'Smith', NOW());
        RAISE NOTICE 'Created person: Jane Smith';
    ELSE
        RAISE NOTICE 'Person already exists: Jane Smith (id=%)', jane_id;
    END IF;

    IF bob_id IS NULL THEN
        INSERT INTO person (id, first_name, last_name, lastupdated)
        VALUES (nextval('person_seq'), 'Bob', 'Wilson', NOW());
        RAISE NOTICE 'Created person: Bob Wilson';
    ELSE
        RAISE NOTICE 'Person already exists: Bob Wilson (id=%)', bob_id;
    END IF;

    IF alice_id IS NULL THEN
        INSERT INTO person (id, first_name, last_name, lastupdated)
        VALUES (nextval('person_seq'), 'Alice', 'Brown', NOW());
        RAISE NOTICE 'Created person: Alice Brown';
    ELSE
        RAISE NOTICE 'Person already exists: Alice Brown (id=%)', alice_id;
    END IF;
END $$;
EOF
echo -e "${GREEN}Person records created.${NC}"

echo -e "${YELLOW}Creating Patient records...${NC}"
execute_sql <<'EOF'
-- Patient records linked to persons

DO $$
DECLARE
    person_john INTEGER;
    person_jane INTEGER;
    person_bob INTEGER;
    person_alice INTEGER;
    pat_exists INTEGER;
BEGIN
    -- Get person IDs
    SELECT id INTO person_john FROM person WHERE first_name = 'John' AND last_name = 'Doe' LIMIT 1;
    SELECT id INTO person_jane FROM person WHERE first_name = 'Jane' AND last_name = 'Smith' LIMIT 1;
    SELECT id INTO person_bob FROM person WHERE first_name = 'Bob' AND last_name = 'Wilson' LIMIT 1;
    SELECT id INTO person_alice FROM person WHERE first_name = 'Alice' AND last_name = 'Brown' LIMIT 1;

    -- Patient 1: John Doe (PAT-VALID-001)
    SELECT id INTO pat_exists FROM patient WHERE external_id = 'PAT-VALID-001' LIMIT 1;
    IF pat_exists IS NULL AND person_john IS NOT NULL THEN
        INSERT INTO patient (id, person_id, external_id, national_id, gender, lastupdated)
        VALUES (nextval('patient_seq'), person_john, 'PAT-VALID-001', 'NID-JD-12345', 'M', NOW());
        RAISE NOTICE 'Created patient: John Doe (PAT-VALID-001)';
    ELSE
        RAISE NOTICE 'Patient PAT-VALID-001 already exists or person not found';
    END IF;

    -- Patient 2: Jane Smith (PAT-VALID-002)
    SELECT id INTO pat_exists FROM patient WHERE external_id = 'PAT-VALID-002' LIMIT 1;
    IF pat_exists IS NULL AND person_jane IS NOT NULL THEN
        INSERT INTO patient (id, person_id, external_id, national_id, gender, lastupdated)
        VALUES (nextval('patient_seq'), person_jane, 'PAT-VALID-002', 'NID-JS-67890', 'F', NOW());
        RAISE NOTICE 'Created patient: Jane Smith (PAT-VALID-002)';
    ELSE
        RAISE NOTICE 'Patient PAT-VALID-002 already exists or person not found';
    END IF;

    -- Patient 3: Bob Wilson (PAT-VALID-003) - for mismatch testing
    SELECT id INTO pat_exists FROM patient WHERE external_id = 'PAT-VALID-003' LIMIT 1;
    IF pat_exists IS NULL AND person_bob IS NOT NULL THEN
        INSERT INTO patient (id, person_id, external_id, national_id, gender, lastupdated)
        VALUES (nextval('patient_seq'), person_bob, 'PAT-VALID-003', 'NID-BW-11111', 'M', NOW());
        RAISE NOTICE 'Created patient: Bob Wilson (PAT-VALID-003)';
    ELSE
        RAISE NOTICE 'Patient PAT-VALID-003 already exists or person not found';
    END IF;

    -- Patient 4: Alice Brown (PAT-VALID-004) - searchable by national ID
    SELECT id INTO pat_exists FROM patient WHERE external_id = 'PAT-VALID-004' LIMIT 1;
    IF pat_exists IS NULL AND person_alice IS NOT NULL THEN
        INSERT INTO patient (id, person_id, external_id, national_id, gender, lastupdated)
        VALUES (nextval('patient_seq'), person_alice, 'PAT-VALID-004', 'NID-AB-22222', 'F', NOW());
        RAISE NOTICE 'Created patient: Alice Brown (PAT-VALID-004, national ID: NID-AB-22222)';
    ELSE
        RAISE NOTICE 'Patient PAT-VALID-004 already exists or person not found';
    END IF;
END $$;
EOF
echo -e "${GREEN}Patient records created.${NC}"

echo -e "${YELLOW}Creating Electronic Order records...${NC}"
execute_sql <<'EOF'
-- Electronic Order records linked to patients

DO $$
DECLARE
    pat_john INTEGER;
    pat_jane INTEGER;
    pat_bob INTEGER;
    ord_exists INTEGER;
BEGIN
    -- Get patient IDs
    SELECT id INTO pat_john FROM patient WHERE external_id = 'PAT-VALID-001' LIMIT 1;
    SELECT id INTO pat_jane FROM patient WHERE external_id = 'PAT-VALID-002' LIMIT 1;
    SELECT id INTO pat_bob FROM patient WHERE external_id = 'PAT-VALID-003' LIMIT 1;

    -- Order 1: For John Doe (ORD-VALID-001)
    SELECT id INTO ord_exists FROM electronic_order WHERE external_id = 'ORD-VALID-001' LIMIT 1;
    IF ord_exists IS NULL AND pat_john IS NOT NULL THEN
        INSERT INTO electronic_order (id, external_id, patient_id, status_id, order_timestamp, lastupdated)
        VALUES (nextval('electronic_order_seq'), 'ORD-VALID-001', pat_john, '1', NOW(), NOW());
        RAISE NOTICE 'Created order: ORD-VALID-001 for John Doe';
    ELSE
        RAISE NOTICE 'Order ORD-VALID-001 already exists or patient not found';
    END IF;

    -- Order 2: For Jane Smith (ORD-VALID-002)
    SELECT id INTO ord_exists FROM electronic_order WHERE external_id = 'ORD-VALID-002' LIMIT 1;
    IF ord_exists IS NULL AND pat_jane IS NOT NULL THEN
        INSERT INTO electronic_order (id, external_id, patient_id, status_id, order_timestamp, lastupdated)
        VALUES (nextval('electronic_order_seq'), 'ORD-VALID-002', pat_jane, '1', NOW(), NOW());
        RAISE NOTICE 'Created order: ORD-VALID-002 for Jane Smith';
    ELSE
        RAISE NOTICE 'Order ORD-VALID-002 already exists or patient not found';
    END IF;

    -- Order 3: For Bob Wilson (ORD-MISMATCH-001) - used to test patient-order mismatch
    -- CSV will reference this order with John Doe's ID, but order belongs to Bob Wilson
    SELECT id INTO ord_exists FROM electronic_order WHERE external_id = 'ORD-MISMATCH-001' LIMIT 1;
    IF ord_exists IS NULL AND pat_bob IS NOT NULL THEN
        INSERT INTO electronic_order (id, external_id, patient_id, status_id, order_timestamp, lastupdated)
        VALUES (nextval('electronic_order_seq'), 'ORD-MISMATCH-001', pat_bob, '1', NOW(), NOW());
        RAISE NOTICE 'Created order: ORD-MISMATCH-001 for Bob Wilson (for mismatch testing)';
    ELSE
        RAISE NOTICE 'Order ORD-MISMATCH-001 already exists or patient not found';
    END IF;
END $$;
EOF
echo -e "${GREEN}Electronic Order records created.${NC}"

echo ""
echo -e "${GREEN}=== Data Population Complete ===${NC}"
echo ""

# Print summary
echo -e "${YELLOW}Summary:${NC}"
execute_sql -t <<'EOF'
SELECT 'Test Persons' as entity, COUNT(*)::text as count
FROM person
WHERE first_name IN ('John', 'Jane', 'Bob', 'Alice')
  AND last_name IN ('Doe', 'Smith', 'Wilson', 'Brown')
UNION ALL
SELECT 'Test Patients', COUNT(*)::text
FROM patient WHERE external_id LIKE 'PAT-VALID-%'
UNION ALL
SELECT 'Test Orders', COUNT(*)::text
FROM electronic_order WHERE external_id LIKE 'ORD-VALID-%' OR external_id LIKE 'ORD-MISMATCH-%';
EOF

echo ""
echo -e "${CYAN}Test Data Created:${NC}"
echo ""
echo "  Patients:"
echo "    - PAT-VALID-001 (John Doe)     - searchable by external ID"
echo "    - PAT-VALID-002 (Jane Smith)   - searchable by external ID"
echo "    - PAT-VALID-003 (Bob Wilson)   - for mismatch testing"
echo "    - PAT-VALID-004 (Alice Brown)  - searchable by national ID (NID-AB-22222)"
echo ""
echo "  Orders:"
echo "    - ORD-VALID-001    -> John Doe"
echo "    - ORD-VALID-002    -> Jane Smith"
echo "    - ORD-MISMATCH-001 -> Bob Wilson (for mismatch testing)"
echo ""
echo -e "${GREEN}Next Step:${NC}"
echo "  Import the CSV file to test validation:"
echo "    test-data/medlab_manifest_validation.csv"
echo ""
echo -e "${YELLOW}CSV Validation Scenarios:${NC}"
echo "  - 7 valid rows (will be imported)"
echo "  - 4 warning rows (imported with warnings)"
echo "  - 11 error rows (will NOT be imported)"
echo ""
echo "Verification query:"
echo "  docker exec -i ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \\"
echo "    \"SELECT p.external_id, p.national_id, per.first_name, per.last_name"
echo "     FROM patient p"
echo "     JOIN person per ON p.person_id = per.id"
echo "     WHERE p.external_id LIKE 'PAT-VALID-%';\""
