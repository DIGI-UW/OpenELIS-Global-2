#!/bin/bash
#
# populate-equipment-usage.sh
#
# Populates demo data for equipment usage management in the OpenELIS database.
# This script runs psql commands inside the Docker container.
#
# Usage:
#   ./scripts/populate-equipment-usage.sh [OPTIONS]
#
# Options:
#   --container     Docker container name (default: openelisglobal-database)
#   -d, --database  Database name (default: clinlims)
#   -U, --user      Database user (default: clinlims)
#   -c, --clean     Clean existing equipment data before inserting
#   --help          Show this help message
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
BLUE='\033[0;34m'
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

echo -e "${GREEN}=== Equipment Usage Demo Data Population Script ===${NC}"
echo "Container: ${CONTAINER}"
echo "Database: ${DB_NAME}"
echo "User: ${DB_USER}"
echo ""

# Function to execute SQL via Docker
execute_sql() {
    docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
}

# Clean existing data if requested
if [ "$CLEAN_FIRST" = true ]; then
    echo -e "${YELLOW}Cleaning existing equipment usage data...${NC}"
    execute_sql <<'EOF'
-- Clean in reverse dependency order
DELETE FROM equipment_usage_audit;
DELETE FROM equipment_usage_entry;
DELETE FROM equipment;

-- Reset sequences
SELECT setval('equipment_seq', 1, false);
SELECT setval('equipment_usage_entry_seq', 1, false);
SELECT setval('equipment_usage_audit_seq', 1, false);
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
    echo ""
fi

echo -e "${YELLOW}Inserting demo equipment...${NC}"
execute_sql <<'EOF'
-- Demo Equipment - Laboratory instruments for different departments
INSERT INTO equipment (id, name, serial_number, manufacturer, model_number, department, purchase_date, is_active, created_date, modified_date, last_updated)
VALUES
    -- Hematology Department
    (nextval('equipment_seq'), 'Olympus BX53 Microscope', 'BX53-001-2023', 'Olympus', 'BX53', 'Hematology', '2023-01-15', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (nextval('equipment_seq'), 'Sysmex XN-1000 Hematology Analyzer', 'XN1000-006-2023', 'Sysmex', 'XN-1000', 'Hematology', '2023-02-28', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Biochemistry Department
    (nextval('equipment_seq'), 'Eppendorf 5424R Centrifuge', 'EP5424R-002-2023', 'Eppendorf', '5424R', 'Biochemistry', '2023-03-20', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Immunology Department
    (nextval('equipment_seq'), 'BioTek ELx800 Plate Reader', 'ELx800-003-2022', 'BioTek', 'ELx800', 'Immunology', '2022-11-10', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (nextval('equipment_seq'), 'Abbott Architect i2000SR', 'AI2000SR-005-2022', 'Abbott', 'Architect i2000SR', 'Immunology', '2022-08-15', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Microbiology Department
    (nextval('equipment_seq'), 'Thermo Scientific Heratherm Incubator', 'HT-004-2023', 'Thermo Scientific', 'Heratherm IMH100', 'Microbiology', '2023-05-08', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Chemistry Department
    (nextval('equipment_seq'), 'Beckman Coulter DxC 700 AU', 'DxC700-007-2023', 'Beckman Coulter', 'DxC 700 AU', 'Chemistry', '2023-06-12', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Molecular Biology Department
    (nextval('equipment_seq'), 'Applied Biosystems StepOnePlus', 'SOPlus-008-2022', 'Applied Biosystems', 'StepOnePlus Real-Time PCR', 'Molecular Biology', '2022-09-25', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
EOF
echo -e "${GREEN}Demo equipment inserted.${NC}"

echo -e "${YELLOW}Inserting recent usage entries (today)...${NC}"
execute_sql <<'EOF'
-- Recent usage - Today's entries
INSERT INTO equipment_usage_entry (id, equipment_id, operator_name, login_time, logout_time, activities_done, equipment_status, department, entry_status, approval_date, approval_signature, created_date, modified_date, last_updated)
VALUES
    -- Today: Approved microscope usage
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'BX53-001-2023'),
     'Dr. Sarah Johnson',
     CURRENT_DATE + INTERVAL '08:30:00',
     CURRENT_DATE + INTERVAL '12:15:00',
     'Performed complete blood count analysis for 25 patient samples. Conducted white cell differential counts. Equipment functioning normally throughout the session.',
     'FUNCTIONAL',
     'Hematology',
     'APPROVED',
     CURRENT_DATE + INTERVAL '13:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE + INTERVAL '13:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE + INTERVAL '08:30:00',
     CURRENT_DATE + INTERVAL '13:00:00',
     CURRENT_DATE + INTERVAL '13:00:00'),

    -- Today: Centrifuge needing calibration (submitted for approval)
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'EP5424R-002-2023'),
     'Mark Thompson',
     CURRENT_DATE + INTERVAL '09:00:00',
     CURRENT_DATE + INTERVAL '11:30:00',
     'Centrifuged serum samples for chemistry panel testing. Processed 40 samples at 3000 RPM for 10 minutes each. Minor vibration noticed during final run - equipment may need calibration.',
     'CALIBRATION_REQUIRED',
     'Biochemistry',
     'SUBMITTED',
     NULL,
     NULL,
     CURRENT_DATE + INTERVAL '09:00:00',
     CURRENT_DATE + INTERVAL '11:30:00',
     CURRENT_DATE + INTERVAL '11:30:00'),

    -- Today: Draft ELISA setup (in progress)
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'ELx800-003-2022'),
     'Dr. Lisa Chen',
     CURRENT_DATE + INTERVAL '14:00:00',
     NULL,
     'Setting up ELISA assays for hepatitis B surface antigen testing. Currently in progress - 24 samples loaded.',
     'FUNCTIONAL',
     'Immunology',
     'DRAFT',
     NULL,
     NULL,
     CURRENT_DATE + INTERVAL '14:00:00',
     CURRENT_DATE + INTERVAL '14:00:00',
     CURRENT_DATE + INTERVAL '14:00:00'),

    -- Today: Chemistry analyzer usage
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'DxC700-007-2023'),
     'Jennifer Martinez',
     CURRENT_DATE + INTERVAL '07:45:00',
     CURRENT_DATE + INTERVAL '10:30:00',
     'Comprehensive metabolic panel testing for emergency department patients. Processed 18 STAT samples with excellent precision and accuracy.',
     'FUNCTIONAL',
     'Chemistry',
     'APPROVED',
     CURRENT_DATE + INTERVAL '11:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE + INTERVAL '11:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE + INTERVAL '07:45:00',
     CURRENT_DATE + INTERVAL '11:00:00',
     CURRENT_DATE + INTERVAL '11:00:00');
EOF
echo -e "${GREEN}Today's usage entries inserted.${NC}"

echo -e "${YELLOW}Inserting yesterday's usage entries...${NC}"
execute_sql <<'EOF'
-- Yesterday's entries
INSERT INTO equipment_usage_entry (id, equipment_id, operator_name, login_time, logout_time, activities_done, equipment_status, department, entry_status, approval_date, approval_signature, created_date, modified_date, last_updated)
VALUES
    -- Yesterday: Incubator session
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'HT-004-2023'),
     'James Rodriguez',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '07:45:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '16:30:00',
     'Incubated bacterial cultures for antimicrobial susceptibility testing. Maintained temperature at 35°C for optimal growth conditions. 48 culture plates processed successfully.',
     'FUNCTIONAL',
     'Microbiology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '17:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '1 day' + INTERVAL '17:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '07:45:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '17:00:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '17:00:00'),

    -- Yesterday: Immunoassay testing
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'AI2000SR-005-2022'),
     'Dr. Maria Garcia',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '10:15:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '14:45:00',
     'Performed immunoassay testing for thyroid function panel (TSH, T3, T4). Processed 35 patient samples. Reagent cartridge replaced during session.',
     'FUNCTIONAL',
     'Immunology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '15:15:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '1 day' + INTERVAL '15:15:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '10:15:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '15:15:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '15:15:00'),

    -- Yesterday: PCR setup and analysis
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'SOPlus-008-2022'),
     'Dr. Ahmed Hassan',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '13:30:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '17:45:00',
     'Real-time PCR for COVID-19 detection. Setup and analysis of 96-well plate with patient specimens. All controls performed within acceptable ranges.',
     'FUNCTIONAL',
     'Molecular Biology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '18:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '1 day' + INTERVAL '18:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '13:30:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '18:00:00',
     CURRENT_DATE - INTERVAL '1 day' + INTERVAL '18:00:00');
EOF
echo -e "${GREEN}Yesterday's usage entries inserted.${NC}"

echo -e "${YELLOW}Inserting historical entries with maintenance issues...${NC}"
execute_sql <<'EOF'
-- Historical entries with various equipment statuses
INSERT INTO equipment_usage_entry (id, equipment_id, operator_name, login_time, logout_time, activities_done, equipment_status, department, entry_status, approval_date, approval_signature, created_date, modified_date, last_updated)
VALUES
    -- 3 days ago: Hematology analyzer under maintenance
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'XN1000-006-2023'),
     'Robert Kim',
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '08:00:00',
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '10:30:00',
     'Attempted hematology analysis but encountered error codes during processing. Performed basic maintenance cleaning. Equipment requires technical service.',
     'UNDER_MAINTENANCE',
     'Hematology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '11:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '3 days' + INTERVAL '11:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '08:00:00',
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '11:00:00',
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '11:00:00'),

    -- 5 days ago: Microscope fault (later repaired)
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'BX53-001-2023'),
     'Dr. Emily Zhang',
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '13:20:00',
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '13:45:00',
     'Attempted microscopic examination but light source failed during operation. Session terminated early. Equipment tagged for repair.',
     'FAULTY',
     'Hematology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '14:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '5 days' + INTERVAL '14:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '13:20:00',
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '14:00:00',
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '14:00:00'),

    -- 4 days ago: Microscope after repair
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'BX53-001-2023'),
     'Dr. Patricia Williams',
     CURRENT_DATE - INTERVAL '4 days' + INTERVAL '09:15:00',
     CURRENT_DATE - INTERVAL '4 days' + INTERVAL '12:30:00',
     'Post-repair testing and validation. Light source replacement successful. Performed optical calibration and quality control checks. Equipment fully operational.',
     'FUNCTIONAL',
     'Hematology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '4 days' + INTERVAL '13:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '4 days' + INTERVAL '13:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '4 days' + INTERVAL '09:15:00',
     CURRENT_DATE - INTERVAL '4 days' + INTERVAL '13:00:00',
     CURRENT_DATE - INTERVAL '4 days' + INTERVAL '13:00:00'),

    -- 1 week ago: Normal centrifuge operation
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'EP5424R-002-2023'),
     'Jennifer Wilson',
     CURRENT_DATE - INTERVAL '7 days' + INTERVAL '09:30:00',
     CURRENT_DATE - INTERVAL '7 days' + INTERVAL '12:00:00',
     'Routine centrifugation for lipid panel samples. Processed 28 samples successfully. Equipment performed within normal parameters.',
     'FUNCTIONAL',
     'Biochemistry',
     'APPROVED',
     CURRENT_DATE - INTERVAL '7 days' + INTERVAL '12:30:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '7 days' + INTERVAL '12:30:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '7 days' + INTERVAL '09:30:00',
     CURRENT_DATE - INTERVAL '7 days' + INTERVAL '12:30:00',
     CURRENT_DATE - INTERVAL '7 days' + INTERVAL '12:30:00'),

    -- 6 days ago: PCR quality control
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'SOPlus-008-2022'),
     'Dr. Michael Chen',
     CURRENT_DATE - INTERVAL '6 days' + INTERVAL '14:45:00',
     CURRENT_DATE - INTERVAL '6 days' + INTERVAL '18:30:00',
     'Weekly quality control run for PCR system. Performed calibration checks and validation with known positive and negative controls. All parameters within specifications.',
     'FUNCTIONAL',
     'Molecular Biology',
     'APPROVED',
     CURRENT_DATE - INTERVAL '6 days' + INTERVAL '19:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '6 days' + INTERVAL '19:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '6 days' + INTERVAL '14:45:00',
     CURRENT_DATE - INTERVAL '6 days' + INTERVAL '19:00:00',
     CURRENT_DATE - INTERVAL '6 days' + INTERVAL '19:00:00'),

    -- 2 days ago: Chemistry analyzer maintenance
    (nextval('equipment_usage_entry_seq'),
     (SELECT id FROM equipment WHERE serial_number = 'DxC700-007-2023'),
     'Maintenance Team',
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '06:00:00',
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '08:30:00',
     'Preventive maintenance performed. Replaced filters, cleaned optical components, and updated calibration standards. System performance verification completed.',
     'FUNCTIONAL',
     'Chemistry',
     'APPROVED',
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '09:00:00',
     'Approved by Lab Manager at ' || TO_CHAR(CURRENT_DATE - INTERVAL '2 days' + INTERVAL '09:00:00', 'YYYY-MM-DD HH24:MI:SS'),
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '06:00:00',
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '09:00:00',
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '09:00:00');
EOF
echo -e "${GREEN}Historical usage entries inserted.${NC}"

echo -e "${YELLOW}Inserting audit trail entries...${NC}"
execute_sql <<'EOF'
-- Equipment usage audit entries (tracking changes to usage entries)
INSERT INTO equipment_usage_audit (id, usage_entry_id, change_type, changed_field, old_value, new_value, changed_by, changed_date)
VALUES
    -- Hematology analyzer usage entry status change
    (nextval('equipment_usage_audit_seq'),
     (SELECT id FROM equipment_usage_entry WHERE equipment_id = (SELECT id FROM equipment WHERE serial_number = 'XN1000-006-2023') AND entry_status = 'APPROVED' AND equipment_status = 'UNDER_MAINTENANCE'),
     'STATUS_UPDATE',
     'equipment_status',
     'FUNCTIONAL',
     'UNDER_MAINTENANCE',
     1,
     CURRENT_DATE - INTERVAL '3 days' + INTERVAL '11:00:00'),

    -- Microscope usage entry status change (fault)
    (nextval('equipment_usage_audit_seq'),
     (SELECT id FROM equipment_usage_entry WHERE equipment_id = (SELECT id FROM equipment WHERE serial_number = 'BX53-001-2023') AND equipment_status = 'FAULTY'),
     'STATUS_UPDATE',
     'equipment_status',
     'FUNCTIONAL',
     'FAULTY',
     1,
     CURRENT_DATE - INTERVAL '5 days' + INTERVAL '14:00:00'),

    -- Centrifuge usage entry status change (calibration needed)
    (nextval('equipment_usage_audit_seq'),
     (SELECT id FROM equipment_usage_entry WHERE equipment_id = (SELECT id FROM equipment WHERE serial_number = 'EP5424R-002-2023') AND equipment_status = 'CALIBRATION_REQUIRED'),
     'STATUS_UPDATE',
     'equipment_status',
     'FUNCTIONAL',
     'CALIBRATION_REQUIRED',
     1,
     CURRENT_DATE + INTERVAL '11:30:00');
EOF
echo -e "${GREEN}Audit trail entries inserted.${NC}"

echo ""
echo -e "${GREEN}=== Equipment Usage Demo Data Population Complete ===${NC}"
echo ""

# Print summary
echo -e "${YELLOW}Summary:${NC}"
execute_sql -t <<'EOF'
SELECT
    'Equipment' as entity, COUNT(*)::text as count FROM equipment
UNION ALL
SELECT 'Usage Entries', COUNT(*)::text FROM equipment_usage_entry
UNION ALL
SELECT 'Audit Records', COUNT(*)::text FROM equipment_usage_audit;
EOF

echo ""
echo -e "${BLUE}Equipment by Department:${NC}"
execute_sql -t <<'EOF'
SELECT
    department,
    COUNT(*) as equipment_count,
    STRING_AGG(name, ', ' ORDER BY name) as equipment_names
FROM equipment
GROUP BY department
ORDER BY department;
EOF

echo ""
echo -e "${BLUE}Usage Entries by Status:${NC}"
execute_sql -t <<'EOF'
SELECT
    entry_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 1) || '%' as percentage
FROM equipment_usage_entry
GROUP BY entry_status
ORDER BY entry_status;
EOF

echo ""
echo -e "${BLUE}Equipment Status Distribution:${NC}"
execute_sql -t <<'EOF'
SELECT
    equipment_status,
    COUNT(*) as count
FROM equipment_usage_entry
GROUP BY equipment_status
ORDER BY equipment_status;
EOF

echo ""
echo -e "${GREEN}Demo data created successfully!${NC}"
echo ""
echo -e "${YELLOW}Features demonstrated:${NC}"
echo "• 8 pieces of laboratory equipment across 6 departments"
echo "• 12 usage log entries spanning the past week"
echo "• All entry statuses: DRAFT, SUBMITTED, APPROVED"
echo "• All equipment statuses: FUNCTIONAL, UNDER_MAINTENANCE, FAULTY, CALIBRATION_REQUIRED"
echo "• 5 audit trail records showing status changes"
echo "• Realistic usage scenarios and maintenance workflows"
echo ""
echo -e "${YELLOW}Sample queries to explore the data:${NC}"
echo ""
echo "View all equipment:"
echo "  docker exec -i ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \\"
echo "    \"SELECT name, serial_number, manufacturer, department FROM equipment ORDER BY department, name;\""
echo ""
echo "View recent usage entries:"
echo "  docker exec -i ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \\"
echo "    \"SELECT e.name as equipment, eu.operator_name, eu.login_time, eu.entry_status"
echo "     FROM equipment_usage_entry eu JOIN equipment e ON eu.equipment_id = e.id"
echo "     ORDER BY eu.login_time DESC LIMIT 10;\""
echo ""
echo "View equipment status changes:"
echo "  docker exec -i ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \\"
echo "    \"SELECT e.name as equipment, ea.changed_date, ea.old_value, ea.new_value, ea.reason"
echo "     FROM equipment_usage_audit ea JOIN equipment e ON ea.equipment_id = e.id"
echo "     ORDER BY ea.changed_date DESC;\""
