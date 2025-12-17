#!/bin/bash
#
# populate-user-data.sh
#
# Imports users, departments, lab units, and measurement units into an OpenELIS database.
# Uses data exported by extract-user-data.sh or can insert sample data.
#
# Usage:
#   ./scripts/populate-user-data.sh [OPTIONS]
#
# Options:
#   --container     Docker container name (default: openelisglobal-database)
#   -d, --database  Database name (default: clinlims)
#   -U, --user      Database user (default: clinlims)
#   -i, --input     Input SQL file (from extract-user-data.sh)
#   -c, --clean     Clean existing user data before inserting (DANGEROUS!)
#   --sample        Insert sample/demo data instead of importing from file
#   --help          Show this help message
#
# Examples:
#   # Import from exported file
#   ./scripts/populate-user-data.sh -i user-data-export.sql
#
#   # Insert sample data for testing
#   ./scripts/populate-user-data.sh --sample
#
#   # Clean and reimport
#   ./scripts/populate-user-data.sh -c -i user-data-export.sql
#

set -e

# Default values
CONTAINER="${CONTAINER:-openelisglobal-database}"
DB_NAME="${DB_NAME:-clinlims}"
DB_USER="${DB_USER:-clinlims}"
INPUT_FILE=""
CLEAN_FIRST=false
INSERT_SAMPLE=false

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
        -i|--input)
            INPUT_FILE="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN_FIRST=true
            shift
            ;;
        --sample)
            INSERT_SAMPLE=true
            shift
            ;;
        --help)
            head -30 "$0" | tail -27
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Validate arguments
if [ "$INSERT_SAMPLE" = false ] && [ -z "$INPUT_FILE" ]; then
    echo -e "${RED}Error: Must specify either --input <file> or --sample${NC}"
    echo "Use --help for usage information."
    exit 1
fi

if [ -n "$INPUT_FILE" ] && [ ! -f "$INPUT_FILE" ]; then
    echo -e "${RED}Error: Input file not found: ${INPUT_FILE}${NC}"
    exit 1
fi

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

echo -e "${GREEN}=== User Data Population Script ===${NC}"
echo "Container: ${CONTAINER}"
echo "Database: ${DB_NAME}"
echo "User: ${DB_USER}"
if [ -n "$INPUT_FILE" ]; then
    echo "Input: ${INPUT_FILE}"
else
    echo "Mode: Sample data"
fi
echo ""

# Function to execute SQL via Docker
execute_sql() {
    docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
}

# Clean existing data if requested
if [ "$CLEAN_FIRST" = true ]; then
    echo -e "${YELLOW}WARNING: This will delete existing user data!${NC}"
    read -p "Are you sure you want to continue? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "Aborted."
        exit 0
    fi

    echo -e "${YELLOW}Cleaning existing user data...${NC}"
    execute_sql <<'EOF'
-- Clean in reverse dependency order
DELETE FROM clinlims.lab_unit_roles;
DELETE FROM clinlims.lab_roles;
DELETE FROM clinlims.lab_unit_role_map;
DELETE FROM clinlims.user_lab_unit_roles;
DELETE FROM clinlims.system_user_module;
DELETE FROM clinlims.system_user_section;
DELETE FROM clinlims.system_user_role;
DELETE FROM clinlims.system_role_module;
DELETE FROM clinlims.system_user WHERE id > 1;  -- Keep admin user
DELETE FROM clinlims.login_user WHERE id > 1;   -- Keep admin user

-- Reset sequences
SELECT setval('clinlims.login_user_seq', 2, false);
SELECT setval('clinlims.system_user_seq', 2, false);
SELECT setval('clinlims.system_user_module_seq', 1, false);
SELECT setval('clinlims.system_user_section_seq', 1, false);
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
    echo ""
fi

# Import from file or insert sample data
if [ -n "$INPUT_FILE" ]; then
    echo -e "${YELLOW}Importing data from: ${INPUT_FILE}${NC}"
    docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$INPUT_FILE"
    echo -e "${GREEN}Import complete.${NC}"
else
    # Insert sample data
    echo -e "${YELLOW}Inserting sample data...${NC}"

    echo -e "${CYAN}Inserting organization types...${NC}"
    execute_sql <<'EOF'
-- Organization Types
INSERT INTO clinlims.organization_type (id, short_name, description, name_display_key, lastupdated)
VALUES
    (100, 'hospital', 'Hospital/Health Facility', 'org.type.hospital', NOW()),
    (101, 'clinic', 'Clinic/Health Center', 'org.type.clinic', NOW()),
    (102, 'lab', 'Laboratory', 'org.type.lab', NOW()),
    (103, 'dept', 'Department', 'org.type.dept', NOW()),
    (104, 'referral', 'Referral Laboratory', 'org.type.referral', NOW())
ON CONFLICT (id) DO NOTHING;
EOF

    echo -e "${CYAN}Inserting organizations...${NC}"
    execute_sql <<'EOF'
-- Organizations (Labs and Health Facilities)
INSERT INTO clinlims.organization (id, name, short_name, city, is_active, code, lastupdated)
VALUES
    (100, 'Central Reference Laboratory', 'CRL', 'Capital City', 'Y', 'CRL001', NOW()),
    (101, 'Regional Hospital Laboratory', 'RHL', 'North Region', 'Y', 'RHL001', NOW()),
    (102, 'District Health Center', 'DHC', 'East District', 'Y', 'DHC001', NOW()),
    (103, 'Community Clinic', 'CC', 'South Village', 'Y', 'CC001', NOW()),
    (104, 'National Public Health Lab', 'NPHL', 'Capital City', 'Y', 'NPHL001', NOW())
ON CONFLICT (id) DO NOTHING;

-- Link organizations to types
INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
VALUES
    (100, 102), -- CRL is a lab
    (101, 102), -- RHL is a lab
    (101, 100), -- RHL is part of hospital
    (102, 101), -- DHC is a clinic
    (103, 101), -- CC is a clinic
    (104, 102), -- NPHL is a lab
    (104, 104)  -- NPHL is also a referral lab
ON CONFLICT DO NOTHING;
EOF

    echo -e "${CYAN}Inserting localization entries...${NC}"
    execute_sql <<'EOF'
-- Localization entries for test section names
INSERT INTO clinlims.localization (id, description, english, french, lastupdated)
VALUES
    (1000, 'test section name', 'Hematology', 'Hematologie', NOW()),
    (1001, 'test section name', 'Chemistry', 'Chimie', NOW()),
    (1002, 'test section name', 'Microbiology', 'Microbiologie', NOW()),
    (1003, 'test section name', 'Immunology', 'Immunologie', NOW()),
    (1004, 'test section name', 'Parasitology', 'Parasitologie', NOW()),
    (1005, 'test section name', 'Molecular Biology', 'Biologie Moleculaire', NOW()),
    (1006, 'test section name', 'Blood Bank', 'Banque de Sang', NOW()),
    (1007, 'test section name', 'Urinalysis', 'Analyse Urine', NOW()),
    (1008, 'test section name', 'Serology', 'Serologie', NOW()),
    (1009, 'test section name', 'Cytology', 'Cytologie', NOW()),
    (1010, 'test section name', 'Histopathology', 'Histopathologie', NOW()),
    (1011, 'test section name', 'Quality Control', 'Controle Qualite', NOW()),
    (1012, 'test section name', 'Sample Reception', 'Reception Echantillons', NOW())
ON CONFLICT (id) DO NOTHING;
EOF

    echo -e "${CYAN}Inserting test sections (departments)...${NC}"
    execute_sql <<'EOF'
-- Test Sections (Departments/Lab Units)
INSERT INTO clinlims.test_section (id, name, description, is_active, sort_order, name_localization_id, display_key, lastupdated)
VALUES
    (100, 'Hematology', 'Hematology Department', 'Y', 1, 1000, 'testsection.Hematology', NOW()),
    (101, 'Chemistry', 'Clinical Chemistry Department', 'Y', 2, 1001, 'testsection.Chemistry', NOW()),
    (102, 'Microbiology', 'Microbiology Department', 'Y', 3, 1002, 'testsection.Microbiology', NOW()),
    (103, 'Immunology', 'Immunology/Serology Department', 'Y', 4, 1003, 'testsection.Immunology', NOW()),
    (104, 'Parasitology', 'Parasitology Department', 'Y', 5, 1004, 'testsection.Parasitology', NOW()),
    (105, 'Molecular', 'Molecular Biology/PCR Department', 'Y', 6, 1005, 'testsection.Molecular', NOW()),
    (106, 'BloodBank', 'Blood Bank Department', 'Y', 7, 1006, 'testsection.BloodBank', NOW()),
    (107, 'Urinalysis', 'Urinalysis Department', 'Y', 8, 1007, 'testsection.Urinalysis', NOW()),
    (108, 'Serology', 'Serology Department', 'Y', 9, 1008, 'testsection.Serology', NOW()),
    (109, 'Cytology', 'Cytology Department', 'Y', 10, 1009, 'testsection.Cytology', NOW()),
    (110, 'Histopathology', 'Histopathology Department', 'Y', 11, 1010, 'testsection.Histopathology', NOW()),
    (111, 'QC', 'Quality Control Department', 'Y', 12, 1011, 'testsection.QC', NOW()),
    (112, 'Reception', 'Sample Reception', 'Y', 13, 1012, 'testsection.Reception', NOW())
ON CONFLICT (id) DO NOTHING;
EOF

    echo -e "${CYAN}Inserting units of measure...${NC}"
    execute_sql <<'EOF'
-- Units of Measure
INSERT INTO clinlims.unit_of_measure (id, name, description, lastupdated)
VALUES
    (100, 'g/dL', 'Grams per deciliter', NOW()),
    (101, 'mg/dL', 'Milligrams per deciliter', NOW()),
    (102, 'mmol/L', 'Millimoles per liter', NOW()),
    (103, 'U/L', 'Units per liter', NOW()),
    (104, 'IU/L', 'International units per liter', NOW()),
    (105, 'mEq/L', 'Milliequivalents per liter', NOW()),
    (106, 'ng/mL', 'Nanograms per milliliter', NOW()),
    (107, 'pg/mL', 'Picograms per milliliter', NOW()),
    (108, 'cells/uL', 'Cells per microliter', NOW()),
    (109, '%', 'Percentage', NOW()),
    (110, 'x10^3/uL', 'Thousands per microliter', NOW()),
    (111, 'x10^6/uL', 'Millions per microliter', NOW()),
    (112, 'x10^9/L', 'Billions per liter', NOW()),
    (113, 'x10^12/L', 'Trillions per liter', NOW()),
    (114, 'fL', 'Femtoliters', NOW()),
    (115, 'pg', 'Picograms', NOW()),
    (116, 'g/L', 'Grams per liter', NOW()),
    (117, 'mg/L', 'Milligrams per liter', NOW()),
    (118, 'ug/dL', 'Micrograms per deciliter', NOW()),
    (119, 'ug/L', 'Micrograms per liter', NOW()),
    (120, 'mIU/mL', 'Milli-international units per mL', NOW()),
    (121, 'uIU/mL', 'Micro-international units per mL', NOW()),
    (122, 'copies/mL', 'Copies per milliliter', NOW()),
    (123, 'log copies/mL', 'Log copies per milliliter', NOW()),
    (124, 'sec', 'Seconds', NOW()),
    (125, 'ratio', 'Ratio', NOW()),
    (126, 'titer', 'Titer', NOW()),
    (127, 'mm/hr', 'Millimeters per hour', NOW()),
    (128, 'mOsm/kg', 'Milliosmoles per kilogram', NOW()),
    (129, 'CFU/mL', 'Colony forming units per mL', NOW())
ON CONFLICT (id) DO NOTHING;
EOF

    echo -e "${CYAN}Inserting sample users...${NC}"
    execute_sql <<'EOF'
-- Sample Login Users (password is 'adminADMIN!' hashed with bcrypt)
-- NOTE: In production, use proper password hashing!
INSERT INTO clinlims.login_user (id, login_name, password, password_expired_dt, account_locked, account_disabled, is_admin, user_time_out)
VALUES
    (100, 'labtech1', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (101, 'labtech2', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (102, 'supervisor1', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (103, 'pathologist1', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (104, 'reception1', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (105, 'qc_officer', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (106, 'data_entry', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'N', '480'),
    (107, 'lab_manager', '$2a$10$hxMhXa.Y3GQfT3pxJ5hxwOEMy.vW8zz9jHEULyH2r9SqvzKZqXKHG', '2026-12-31', 'N', 'N', 'Y', '480')
ON CONFLICT (id) DO NOTHING;

-- System Users (profile information)
INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, initials, is_active, is_employee, lastupdated)
VALUES
    (100, 'labtech1', 'John', 'Smith', 'JS', 'Y', 'Y', NOW()),
    (101, 'labtech2', 'Jane', 'Doe', 'JD', 'Y', 'Y', NOW()),
    (102, 'supervisor1', 'Robert', 'Johnson', 'RJ', 'Y', 'Y', NOW()),
    (103, 'pathologist1', 'Dr. Sarah', 'Williams', 'SW', 'Y', 'Y', NOW()),
    (104, 'reception1', 'Mary', 'Brown', 'MB', 'Y', 'Y', NOW()),
    (105, 'qc_officer', 'Michael', 'Davis', 'MD', 'Y', 'Y', NOW()),
    (106, 'data_entry', 'Emily', 'Wilson', 'EW', 'Y', 'Y', NOW()),
    (107, 'lab_manager', 'David', 'Taylor', 'DT', 'Y', 'Y', NOW())
ON CONFLICT (id) DO NOTHING;
EOF

    echo -e "${CYAN}Assigning user roles...${NC}"
    execute_sql <<'EOF'
-- Assign roles to users (assumes standard role IDs exist)
-- These role IDs should match your system_role table

-- Get the role IDs dynamically
DO $$
DECLARE
    results_role_id INTEGER;
    validation_role_id INTEGER;
    reception_role_id INTEGER;
    reports_role_id INTEGER;
    audit_role_id INTEGER;
BEGIN
    -- Get role IDs (adjust names to match your system)
    SELECT id INTO results_role_id FROM clinlims.system_role WHERE TRIM(name) = 'Results entry' LIMIT 1;
    SELECT id INTO validation_role_id FROM clinlims.system_role WHERE TRIM(name) = 'Validation' LIMIT 1;
    SELECT id INTO reception_role_id FROM clinlims.system_role WHERE TRIM(name) = 'Reception' LIMIT 1;
    SELECT id INTO reports_role_id FROM clinlims.system_role WHERE TRIM(name) = 'Reports' LIMIT 1;
    SELECT id INTO audit_role_id FROM clinlims.system_role WHERE TRIM(name) = 'Audit Trail' LIMIT 1;

    -- Lab Technicians - results entry
    IF results_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (100, results_role_id) ON CONFLICT DO NOTHING;
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (101, results_role_id) ON CONFLICT DO NOTHING;
    END IF;

    -- Supervisor - results and validation
    IF results_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (102, results_role_id) ON CONFLICT DO NOTHING;
    END IF;
    IF validation_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (102, validation_role_id) ON CONFLICT DO NOTHING;
    END IF;

    -- Pathologist - validation
    IF validation_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (103, validation_role_id) ON CONFLICT DO NOTHING;
    END IF;

    -- Reception - reception role
    IF reception_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (104, reception_role_id) ON CONFLICT DO NOTHING;
    END IF;

    -- QC Officer - results and reports
    IF results_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (105, results_role_id) ON CONFLICT DO NOTHING;
    END IF;
    IF reports_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (105, reports_role_id) ON CONFLICT DO NOTHING;
    END IF;

    -- Data Entry - reception
    IF reception_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (106, reception_role_id) ON CONFLICT DO NOTHING;
    END IF;

    -- Lab Manager - all roles
    IF results_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (107, results_role_id) ON CONFLICT DO NOTHING;
    END IF;
    IF validation_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (107, validation_role_id) ON CONFLICT DO NOTHING;
    END IF;
    IF reports_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (107, reports_role_id) ON CONFLICT DO NOTHING;
    END IF;
    IF audit_role_id IS NOT NULL THEN
        INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (107, audit_role_id) ON CONFLICT DO NOTHING;
    END IF;
END $$;
EOF

    echo -e "${CYAN}Assigning department permissions...${NC}"
    execute_sql <<'EOF'
-- Assign department/section permissions to users
INSERT INTO clinlims.system_user_section (id, has_view, has_assign, has_complete, has_release, has_cancel, system_user_id, test_section_id)
VALUES
    -- Lab Tech 1 - Hematology and Chemistry
    (1000, 'Y', 'Y', 'Y', 'N', 'N', 100, 100),
    (1001, 'Y', 'Y', 'Y', 'N', 'N', 100, 101),

    -- Lab Tech 2 - Microbiology and Parasitology
    (1002, 'Y', 'Y', 'Y', 'N', 'N', 101, 102),
    (1003, 'Y', 'Y', 'Y', 'N', 'N', 101, 104),

    -- Supervisor - Multiple departments with release
    (1004, 'Y', 'Y', 'Y', 'Y', 'Y', 102, 100),
    (1005, 'Y', 'Y', 'Y', 'Y', 'Y', 102, 101),
    (1006, 'Y', 'Y', 'Y', 'Y', 'Y', 102, 102),

    -- Pathologist - Cytology and Histopathology with release
    (1007, 'Y', 'Y', 'Y', 'Y', 'Y', 103, 109),
    (1008, 'Y', 'Y', 'Y', 'Y', 'Y', 103, 110),

    -- Reception - Reception only
    (1009, 'Y', 'Y', 'N', 'N', 'N', 104, 112),

    -- QC Officer - QC department
    (1010, 'Y', 'Y', 'Y', 'Y', 'N', 105, 111),

    -- Data Entry - Reception view only
    (1011, 'Y', 'N', 'N', 'N', 'N', 106, 112),

    -- Lab Manager - All departments
    (1012, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 100),
    (1013, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 101),
    (1014, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 102),
    (1015, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 103),
    (1016, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 104),
    (1017, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 105),
    (1018, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 106),
    (1019, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 107),
    (1020, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 108),
    (1021, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 109),
    (1022, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 110),
    (1023, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 111),
    (1024, 'Y', 'Y', 'Y', 'Y', 'Y', 107, 112)
ON CONFLICT (id) DO NOTHING;
EOF

    echo -e "${CYAN}Setting up lab unit role mappings...${NC}"
    execute_sql <<'EOF'
-- User Lab Unit Roles containers
INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated)
VALUES
    (100, NOW()),
    (101, NOW()),
    (102, NOW()),
    (103, NOW()),
    (105, NOW()),
    (107, NOW())
ON CONFLICT (system_user_id) DO NOTHING;

-- Lab Unit Role Maps
INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit)
VALUES
    (100, 'Hematology'),
    (101, 'Chemistry'),
    (102, 'Microbiology'),
    (103, 'Parasitology'),
    (104, 'Cytology'),
    (105, 'Histopathology'),
    (106, 'QC'),
    (107, 'All Departments')
ON CONFLICT (lab_unit_role_map_id) DO NOTHING;

-- Lab Roles within each unit
INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role)
VALUES
    (100, 'Results entry'),
    (100, 'Validation'),
    (101, 'Results entry'),
    (101, 'Validation'),
    (102, 'Results entry'),
    (102, 'Validation'),
    (103, 'Results entry'),
    (104, 'Validation'),
    (105, 'Validation'),
    (106, 'Results entry'),
    (106, 'Reports'),
    (107, 'Results entry'),
    (107, 'Validation'),
    (107, 'Reports'),
    (107, 'Audit Trail')
ON CONFLICT DO NOTHING;

-- Assign lab units to users
INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id)
VALUES
    (100, 100),  -- Lab Tech 1 -> Hematology
    (100, 101),  -- Lab Tech 1 -> Chemistry
    (101, 102),  -- Lab Tech 2 -> Microbiology
    (101, 103),  -- Lab Tech 2 -> Parasitology
    (102, 100),  -- Supervisor -> Hematology
    (102, 101),  -- Supervisor -> Chemistry
    (102, 102),  -- Supervisor -> Microbiology
    (103, 104),  -- Pathologist -> Cytology
    (103, 105),  -- Pathologist -> Histopathology
    (105, 106),  -- QC Officer -> QC
    (107, 107)   -- Lab Manager -> All Departments
ON CONFLICT DO NOTHING;
EOF

    echo -e "${CYAN}Updating sequences...${NC}"
    execute_sql <<'EOF'
-- Update sequences to avoid ID conflicts
SELECT setval('clinlims.login_user_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.login_user), 0), 200) + 1, false);
SELECT setval('clinlims.system_user_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.system_user), 0), 200) + 1, false);
SELECT setval('clinlims.organization_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.organization), 0), 200) + 1, false);
SELECT setval('clinlims.organization_type_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.organization_type), 0), 200) + 1, false);
SELECT setval('clinlims.test_section_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.test_section), 0), 200) + 1, false);
SELECT setval('clinlims.unit_of_measure_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.unit_of_measure), 0), 200) + 1, false);
SELECT setval('clinlims.system_user_section_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.system_user_section), 0), 2000) + 1, false);
SELECT setval('clinlims.localization_seq', GREATEST(COALESCE((SELECT MAX(id) FROM clinlims.localization), 0), 2000) + 1, false);
EOF

    echo -e "${GREEN}Sample data inserted.${NC}"
fi

echo ""
echo -e "${GREEN}=== Data Population Complete ===${NC}"
echo ""

# Print summary
echo -e "${CYAN}Summary:${NC}"
execute_sql -t <<'EOF'
SELECT 'Organization Types' as entity, COUNT(*)::text as count FROM clinlims.organization_type
UNION ALL SELECT 'Organizations', COUNT(*)::text FROM clinlims.organization
UNION ALL SELECT 'Test Sections (Depts)', COUNT(*)::text FROM clinlims.test_section
UNION ALL SELECT 'Units of Measure', COUNT(*)::text FROM clinlims.unit_of_measure
UNION ALL SELECT 'Login Users', COUNT(*)::text FROM clinlims.login_user
UNION ALL SELECT 'System Users', COUNT(*)::text FROM clinlims.system_user
UNION ALL SELECT 'User Role Assignments', COUNT(*)::text FROM clinlims.system_user_role
UNION ALL SELECT 'User Section Perms', COUNT(*)::text FROM clinlims.system_user_section;
EOF

echo ""
echo -e "${GREEN}User data populated successfully!${NC}"
echo ""
echo "Sample users created (if using --sample):"
echo "  labtech1, labtech2, supervisor1, pathologist1,"
echo "  reception1, qc_officer, data_entry, lab_manager"
echo ""
echo "Default password for sample users: adminADMIN!"
echo "(Users should change passwords on first login)"
