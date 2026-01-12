#!/bin/bash
#
# populate-sample-types.sh
#
# Populates common sample types for OpenELIS development/demo environments.
# This script runs psql commands inside the Docker container.
#
# Usage:
#   ./scripts/populate-sample-types.sh [OPTIONS]
#
# Options:
#   --container     Docker container name (default: openelisglobal-database)
#   -d, --database  Database name (default: clinlims)
#   -U, --user      Database user (default: clinlims)
#   --help          Show this help message
#

set -e

# Default values
CONTAINER="${CONTAINER:-openelisglobal-database}"
DB_NAME="${DB_NAME:-clinlims}"
DB_USER="${DB_USER:-clinlims}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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
        --help)
            head -20 "$0" | tail -15
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

echo -e "${GREEN}=== Sample Types Population Script ===${NC}"
echo "Container: ${CONTAINER}"
echo "Database: ${DB_NAME}"
echo "User: ${DB_USER}"
echo ""

# Function to execute SQL via Docker
execute_sql() {
    docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
}

echo -e "${YELLOW}Inserting sample types (skipping existing)...${NC}"
execute_sql <<'EOF'
-- Insert localization entries and sample types
-- Uses DO block to handle conditional inserts cleanly

DO $$
DECLARE
    loc_id INTEGER;
BEGIN
    -- Whole Blood
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Whole Blood') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Whole Blood', 'Sang total')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Whole Blood', NOW(), 'H', 'WB', 'Y', 'sample.type.wholeBlood', loc_id);
        RAISE NOTICE 'Added: Whole Blood';
    ELSE
        RAISE NOTICE 'Skipped: Whole Blood (already exists)';
    END IF;

    -- Serum
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Serum') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Serum', 'Sérum')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Serum', NOW(), 'H', 'SER', 'Y', 'sample.type.serum', loc_id);
        RAISE NOTICE 'Added: Serum';
    ELSE
        RAISE NOTICE 'Skipped: Serum (already exists)';
    END IF;

    -- Plasma
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Plasma') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Plasma', 'Plasma')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Plasma', NOW(), 'H', 'PLA', 'Y', 'sample.type.plasma', loc_id);
        RAISE NOTICE 'Added: Plasma';
    ELSE
        RAISE NOTICE 'Skipped: Plasma (already exists)';
    END IF;

    -- Urine
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Urine') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Urine', 'Urine')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Urine', NOW(), 'H', 'URI', 'Y', 'sample.type.urine', loc_id);
        RAISE NOTICE 'Added: Urine';
    ELSE
        RAISE NOTICE 'Skipped: Urine (already exists)';
    END IF;

    -- Stool
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Stool') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Stool', 'Selle')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Stool', NOW(), 'H', 'STL', 'Y', 'sample.type.stool', loc_id);
        RAISE NOTICE 'Added: Stool';
    ELSE
        RAISE NOTICE 'Skipped: Stool (already exists)';
    END IF;

    -- Cerebrospinal Fluid (CSF)
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Cerebrospinal Fluid') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Cerebrospinal Fluid', 'Liquide céphalo-rachidien')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Cerebrospinal Fluid', NOW(), 'H', 'CSF', 'Y', 'sample.type.csf', loc_id);
        RAISE NOTICE 'Added: Cerebrospinal Fluid';
    ELSE
        RAISE NOTICE 'Skipped: Cerebrospinal Fluid (already exists)';
    END IF;

    -- Swab
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Swab') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Swab', 'Écouvillon')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Swab', NOW(), 'H', 'SWB', 'Y', 'sample.type.swab', loc_id);
        RAISE NOTICE 'Added: Swab';
    ELSE
        RAISE NOTICE 'Skipped: Swab (already exists)';
    END IF;

    -- Sputum
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Sputum') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Sputum', 'Crachat')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Sputum', NOW(), 'H', 'SPT', 'Y', 'sample.type.sputum', loc_id);
        RAISE NOTICE 'Added: Sputum';
    ELSE
        RAISE NOTICE 'Skipped: Sputum (already exists)';
    END IF;

    -- Dried Blood Spot (DBS)
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Dried Blood Spot') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Dried Blood Spot', 'Goutte de sang séché')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Dried Blood Spot', NOW(), 'H', 'DBS', 'Y', 'sample.type.dbs', loc_id);
        RAISE NOTICE 'Added: Dried Blood Spot';
    ELSE
        RAISE NOTICE 'Skipped: Dried Blood Spot (already exists)';
    END IF;

    -- Saliva
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Saliva') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Saliva', 'Salive')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Saliva', NOW(), 'H', 'SAL', 'Y', 'sample.type.saliva', loc_id);
        RAISE NOTICE 'Added: Saliva';
    ELSE
        RAISE NOTICE 'Skipped: Saliva (already exists)';
    END IF;

    -- Tissue
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Tissue') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Tissue', 'Tissu')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Tissue', NOW(), 'H', 'TIS', 'Y', 'sample.type.tissue', loc_id);
        RAISE NOTICE 'Added: Tissue';
    ELSE
        RAISE NOTICE 'Skipped: Tissue (already exists)';
    END IF;

    -- Bone Marrow
    IF NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE description = 'Bone Marrow') THEN
        INSERT INTO clinlims.localization (id, lastupdated, description, english, french)
        VALUES (nextval('localization_seq'), NOW(), 'sampleType name', 'Bone Marrow', 'Moelle osseuse')
        RETURNING id INTO loc_id;

        INSERT INTO clinlims.type_of_sample (id, description, lastupdated, domain, local_abbrev, is_active, display_key, name_localization_id)
        VALUES (nextval('type_of_sample_seq'), 'Bone Marrow', NOW(), 'H', 'BM', 'Y', 'sample.type.boneMarrow', loc_id);
        RAISE NOTICE 'Added: Bone Marrow';
    ELSE
        RAISE NOTICE 'Skipped: Bone Marrow (already exists)';
    END IF;

END $$;
EOF

echo -e "${GREEN}Sample types inserted.${NC}"
echo ""

# Print summary
echo -e "${YELLOW}Current sample types in database:${NC}"
execute_sql -t <<'EOF'
SELECT description, local_abbrev,
       CASE WHEN is_active = 'Y' THEN 'Active' ELSE 'Inactive' END as status
FROM clinlims.type_of_sample
WHERE domain = 'H'
ORDER BY description;
EOF

echo ""
echo -e "${GREEN}=== Sample Types Population Complete ===${NC}"
