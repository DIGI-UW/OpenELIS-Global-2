#!/bin/bash
#
# populate-notebook-users.sh
#
# Creates a demo user for each Notebook workflow with access limited to their
# corresponding lab unit/location.
#
# Usage:
#   ./scripts/populate-notebook-users.sh [OPTIONS]
#
# Options:
#   --container       Docker container name (default: openelisglobal-database)
#   -d, --database    Database name (default: clinlims)
#   -U, --user        Database user (default: clinlims)
#   -c, --clean       Remove all notebook demo users (revert changes)
#   --clean-install   Clean and then create fresh users
#   --dry-run         Show SQL without executing
#   --help            Show this help message
#
# Example:
#   ./scripts/populate-notebook-users.sh              # Create users
#   ./scripts/populate-notebook-users.sh --clean      # Remove users only
#   ./scripts/populate-notebook-users.sh --clean-install  # Remove and recreate
#

set -e

# Default values
CONTAINER="${CONTAINER:-openelisglobal-database}"
DB_NAME="${DB_NAME:-clinlims}"
DB_USER="${DB_USER:-clinlims}"
CLEAN_ONLY=false
CLEAN_INSTALL=false
DRY_RUN=false

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
            CLEAN_ONLY=true
            shift
            ;;
        --clean-install)
            CLEAN_INSTALL=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            head -25 "$0" | tail -22
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Check if docker is available
if [ "$DRY_RUN" = false ]; then
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
fi

echo -e "${GREEN}=== Notebook Users Population Script ===${NC}"
echo "Container: ${CONTAINER}"
echo "Database: ${DB_NAME}"
echo "Dry Run: ${DRY_RUN}"
echo ""

# Function to execute SQL via Docker
execute_sql() {
    if [ "$DRY_RUN" = true ]; then
        cat
    else
        docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
    fi
}

# Define all notebooks: "LabUnitName|Username|FirstName|UserID"
NOTEBOOKS=(
    "Immunology|immuno|Immunology|1000"
    "Pathology Laboratory|patho|Pathology|1001"
    "Bacteriology|bacterio|Bacteriology|1002"
    "Malaria and Neglected Tropical Disease (MNTD) Laboratory|mntd|MNTD|1003"
    "Pharmaceuticals Laboratory|pharma|Pharmaceuticals|1004"
    "Virology and Vaccine Unit|viro|Virology|1005"
    "Tuberculosis Laboratory|tb|Tuberculosis|1006"
    "Medical Laboratory|medlab|Medical Lab|1007"
    "Biorepository Laboratory|biorepo|Biorepository|1008"
    "Genomics & Bioinformatics Laboratory|genomics|Genomics|1009"
    "Traditional & Modern Medicine Research Lab|tradmed|Traditional Med|1010"
    "Bioanalytical Laboratory|bioanalyt|Bioanalytical|1011"
    "Bioequivalence Laboratory|bioeq|Bioequivalence|1012"
)

# Clean function
do_clean() {
    echo -e "${YELLOW}Cleaning notebook demo users...${NC}"
    execute_sql <<'EOF'
-- Clean notebook demo users (IDs 1000-1099)
DELETE FROM clinlims.system_user_section WHERE system_user_id BETWEEN 1000 AND 1099;
DELETE FROM clinlims.system_user_role WHERE system_user_id BETWEEN 1000 AND 1099;
DELETE FROM clinlims.lab_unit_roles WHERE system_user_id BETWEEN 1000 AND 1099;
DELETE FROM clinlims.user_lab_unit_roles WHERE system_user_id BETWEEN 1000 AND 1099;
DELETE FROM clinlims.system_user WHERE id BETWEEN 1000 AND 1099;
DELETE FROM clinlims.login_user WHERE id BETWEEN 1000 AND 1099;
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
}

# Handle --clean (clean only, then exit)
if [ "$CLEAN_ONLY" = true ]; then
    do_clean
    echo ""
    echo -e "${GREEN}Notebook demo users removed.${NC}"
    exit 0
fi

# Handle --clean-install (clean first, then continue to populate)
if [ "$CLEAN_INSTALL" = true ]; then
    do_clean
    echo ""
fi

echo -e "${YELLOW}Creating notebook users...${NC}"
echo ""

# Generate and execute SQL for each notebook
for notebook_entry in "${NOTEBOOKS[@]}"; do
    IFS='|' read -r lab_unit username first_name user_id <<< "$notebook_entry"

    echo -e "${CYAN}Creating user: ${username}${NC}"
    echo "  Lab Unit: ${lab_unit}"

    execute_sql <<EOF
DO \$\$
DECLARE
    v_test_section_id INTEGER;
    v_lab_unit_role_map_id INTEGER;
BEGIN
    -- Find the test section ID for this lab unit
    SELECT id INTO v_test_section_id
    FROM clinlims.test_section
    WHERE name = '${lab_unit}'
    LIMIT 1;

    IF v_test_section_id IS NULL THEN
        RAISE NOTICE 'Test section not found for: ${lab_unit} - skipping';
        RETURN;
    END IF;

    -- Create login user
    INSERT INTO clinlims.login_user (id, login_name, password, password_expired_dt, account_locked, account_disabled, is_admin, user_time_out)
    VALUES (${user_id}, '${username}', '\$2a\$10\$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2027-12-31', 'N', 'N', 'N', '480')
    ON CONFLICT (id) DO UPDATE SET login_name = EXCLUDED.login_name;

    -- Create system user
    INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, initials, is_active, is_employee, lastupdated)
    VALUES (${user_id}, '${username}', '${first_name}', 'User', UPPER(LEFT('${username}', 3)), 'Y', 'Y', NOW())
    ON CONFLICT (id) DO UPDATE SET first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name;

    -- Find or create lab_unit_role_map entry for this test section
    SELECT lab_unit_role_map_id INTO v_lab_unit_role_map_id
    FROM clinlims.lab_unit_role_map
    WHERE lab_unit = v_test_section_id::VARCHAR
    LIMIT 1;

    IF v_lab_unit_role_map_id IS NULL THEN
        INSERT INTO clinlims.lab_unit_role_map (lab_unit)
        VALUES (v_test_section_id::VARCHAR)
        RETURNING lab_unit_role_map_id INTO v_lab_unit_role_map_id;
    END IF;

    -- Create user_lab_unit_roles entry (parent record)
    INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated)
    VALUES (${user_id}, NOW())
    ON CONFLICT (system_user_id) DO NOTHING;

    -- Link user to their lab unit
    INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id)
    VALUES (${user_id}, v_lab_unit_role_map_id)
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Created user: ${username} for ${lab_unit}';
END \$\$;
EOF

    echo ""
done

# Update sequences
echo -e "${CYAN}Updating sequences...${NC}"
execute_sql <<'EOF'
SELECT setval('clinlims.login_user_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM clinlims.login_user)::bigint, 1099::bigint) + 1, false);
SELECT setval('clinlims.system_user_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM clinlims.system_user)::bigint, 1099::bigint) + 1, false);
EOF

echo ""
echo -e "${GREEN}=== Notebook Users Created ===${NC}"
echo ""

# Print summary
if [ "$DRY_RUN" = false ]; then
    echo -e "${CYAN}Summary:${NC}"
    execute_sql -t <<'EOF'
SELECT
    su.login_name AS "Username",
    ts.name AS "Lab Unit"
FROM clinlims.system_user su
JOIN clinlims.user_lab_unit_roles ulur ON su.id = ulur.system_user_id
JOIN clinlims.lab_unit_roles lur ON ulur.system_user_id = lur.system_user_id
JOIN clinlims.lab_unit_role_map lurm ON lur.lab_unit_role_map_id = lurm.lab_unit_role_map_id
JOIN clinlims.test_section ts ON lurm.lab_unit ~ '^\d+$' AND lurm.lab_unit::INTEGER = ts.id
WHERE su.id BETWEEN 1000 AND 1099
ORDER BY su.id;
EOF
fi

echo ""
echo "Default password: adminADMIN!"
echo "Total users: ${#NOTEBOOKS[@]}"
