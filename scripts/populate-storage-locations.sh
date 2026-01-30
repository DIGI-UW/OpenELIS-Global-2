#!/bin/bash
#
# populate-storage-locations.sh
#
# Populates sample data for storage locations in the OpenELIS database.
# This script runs psql commands inside the Docker container.
#
# Usage:
#   ./scripts/populate-storage-locations.sh [OPTIONS]
#
# Options:
#   --container     Docker container name (default: openelisglobal-database)
#   -d, --database  Database name (default: clinlims)
#   -U, --user      Database user (default: clinlims)
#   -c, --clean     Clean existing storage data before inserting
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

echo -e "${GREEN}=== Storage Location Data Population Script ===${NC}"
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
    echo -e "${YELLOW}Cleaning existing storage location data...${NC}"
    execute_sql <<'EOF'
-- Clean in reverse hierarchy order (children first)
DELETE FROM sample_storage_movement;
DELETE FROM sample_storage_assignment;
DELETE FROM storage_box;
DELETE FROM storage_rack;
DELETE FROM storage_shelf;
DELETE FROM storage_device;
DELETE FROM storage_room;

-- Reset sequences
SELECT setval('storage_room_seq', 1, false);
SELECT setval('storage_device_seq', 1, false);
SELECT setval('storage_shelf_seq', 1, false);
SELECT setval('storage_rack_seq', 1, false);
SELECT setval('storage_box_seq', 1, false);
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
    echo ""
fi

echo -e "${YELLOW}Inserting storage rooms...${NC}"
execute_sql <<'EOF'
-- Storage Rooms (Top-level physical locations)
INSERT INTO storage_room (id, fhir_uuid, name, code, description, active, sys_user_id, last_updated)
VALUES
    (1, gen_random_uuid(), 'Main Laboratory', 'MAIN-LAB', 'Primary laboratory storage facility with controlled temperature zones', true, '1', NOW()),
    (2, gen_random_uuid(), 'Sample Processing Room', 'PROC-ROOM', 'Room dedicated to sample processing and aliquoting', true, '1', NOW()),
    (3, gen_random_uuid(), 'Archive Storage', 'ARCHIVE', 'Long-term sample archival storage', true, '1', NOW()),
    (4, gen_random_uuid(), 'Quality Control Room', 'QC-ROOM', 'QC sample storage and testing area', true, '1', NOW()),
    (5, gen_random_uuid(), 'Receiving Area', 'RECV', 'Sample receiving and initial processing', true, '1', NOW())
ON CONFLICT (code) DO NOTHING;
EOF
echo -e "${GREEN}Rooms inserted.${NC}"

echo -e "${YELLOW}Inserting storage devices...${NC}"
execute_sql <<'EOF'
-- Storage Devices (Freezers, Refrigerators, Cabinets)
INSERT INTO storage_device (id, fhir_uuid, name, code, type, temperature_setting, capacity_limit, active, parent_room_id, sys_user_id, last_updated)
VALUES
    -- Main Laboratory devices
    (10, gen_random_uuid(), 'Ultra-Low Freezer 1', 'ULF01', 'freezer', -80.0, 1000, true, 1, '1', NOW()),
    (11, gen_random_uuid(), 'Ultra-Low Freezer 2', 'ULF02', 'freezer', -80.0, 1000, true, 1, '1', NOW()),
    (12, gen_random_uuid(), 'Standard Freezer 1', 'FRZ01', 'freezer', -20.0, 500, true, 1, '1', NOW()),
    (13, gen_random_uuid(), 'Refrigerator 1', 'REF01', 'refrigerator', 4.0, 300, true, 1, '1', NOW()),
    (14, gen_random_uuid(), 'Refrigerator 2', 'REF02', 'refrigerator', 4.0, 300, true, 1, '1', NOW()),

    -- Sample Processing Room devices
    (20, gen_random_uuid(), 'Processing Refrigerator', 'PREF01', 'refrigerator', 4.0, 200, true, 2, '1', NOW()),
    (21, gen_random_uuid(), 'Processing Cabinet', 'PCAB01', 'cabinet', NULL, 100, true, 2, '1', NOW()),

    -- Archive Storage devices
    (30, gen_random_uuid(), 'Archive Freezer 1', 'AFRZ01', 'freezer', -80.0, 2000, true, 3, '1', NOW()),
    (31, gen_random_uuid(), 'Archive Freezer 2', 'AFRZ02', 'freezer', -80.0, 2000, true, 3, '1', NOW()),
    (32, gen_random_uuid(), 'Archive Freezer 3', 'AFRZ03', 'freezer', -20.0, 1500, true, 3, '1', NOW()),

    -- QC Room devices
    (40, gen_random_uuid(), 'QC Refrigerator', 'QCREF01', 'refrigerator', 4.0, 200, true, 4, '1', NOW()),
    (41, gen_random_uuid(), 'QC Freezer', 'QCFRZ01', 'freezer', -20.0, 200, true, 4, '1', NOW()),

    -- Receiving Area devices
    (50, gen_random_uuid(), 'Receiving Refrigerator', 'RREF01', 'refrigerator', 4.0, 150, true, 5, '1', NOW()),
    (51, gen_random_uuid(), 'Temporary Storage Cabinet', 'RCAB01', 'cabinet', NULL, 100, true, 5, '1', NOW())
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}Devices inserted.${NC}"

echo -e "${YELLOW}Inserting storage shelves...${NC}"
execute_sql <<'EOF'
-- Storage Shelves (within devices)
INSERT INTO storage_shelf (id, fhir_uuid, label, code, capacity_limit, active, parent_device_id, sys_user_id, last_updated)
VALUES
    -- Ultra-Low Freezer 1 shelves (Device 10)
    (100, gen_random_uuid(), 'Shelf 1', 'S1', 50, true, 10, '1', NOW()),
    (101, gen_random_uuid(), 'Shelf 2', 'S2', 50, true, 10, '1', NOW()),
    (102, gen_random_uuid(), 'Shelf 3', 'S3', 50, true, 10, '1', NOW()),
    (103, gen_random_uuid(), 'Shelf 4', 'S4', 50, true, 10, '1', NOW()),
    (104, gen_random_uuid(), 'Shelf 5', 'S5', 50, true, 10, '1', NOW()),

    -- Ultra-Low Freezer 2 shelves (Device 11)
    (110, gen_random_uuid(), 'Shelf 1', 'S1', 50, true, 11, '1', NOW()),
    (111, gen_random_uuid(), 'Shelf 2', 'S2', 50, true, 11, '1', NOW()),
    (112, gen_random_uuid(), 'Shelf 3', 'S3', 50, true, 11, '1', NOW()),
    (113, gen_random_uuid(), 'Shelf 4', 'S4', 50, true, 11, '1', NOW()),
    (114, gen_random_uuid(), 'Shelf 5', 'S5', 50, true, 11, '1', NOW()),

    -- Standard Freezer 1 shelves (Device 12)
    (120, gen_random_uuid(), 'Top Shelf', 'TOP', 30, true, 12, '1', NOW()),
    (121, gen_random_uuid(), 'Middle Shelf', 'MID', 30, true, 12, '1', NOW()),
    (122, gen_random_uuid(), 'Bottom Shelf', 'BOT', 30, true, 12, '1', NOW()),

    -- Refrigerator 1 shelves (Device 13)
    (130, gen_random_uuid(), 'Shelf A', 'A', 25, true, 13, '1', NOW()),
    (131, gen_random_uuid(), 'Shelf B', 'B', 25, true, 13, '1', NOW()),
    (132, gen_random_uuid(), 'Shelf C', 'C', 25, true, 13, '1', NOW()),

    -- Refrigerator 2 shelves (Device 14)
    (140, gen_random_uuid(), 'Shelf A', 'A', 25, true, 14, '1', NOW()),
    (141, gen_random_uuid(), 'Shelf B', 'B', 25, true, 14, '1', NOW()),
    (142, gen_random_uuid(), 'Shelf C', 'C', 25, true, 14, '1', NOW()),

    -- Processing Refrigerator shelves (Device 20)
    (200, gen_random_uuid(), 'Active Samples', 'ACT', 20, true, 20, '1', NOW()),
    (201, gen_random_uuid(), 'Pending Samples', 'PEND', 20, true, 20, '1', NOW()),

    -- Processing Cabinet shelves (Device 21)
    (210, gen_random_uuid(), 'Supplies Shelf', 'SUP', 15, true, 21, '1', NOW()),

    -- Archive Freezer 1 shelves (Device 30)
    (300, gen_random_uuid(), 'Archive Shelf 1', 'A1', 100, true, 30, '1', NOW()),
    (301, gen_random_uuid(), 'Archive Shelf 2', 'A2', 100, true, 30, '1', NOW()),
    (302, gen_random_uuid(), 'Archive Shelf 3', 'A3', 100, true, 30, '1', NOW()),
    (303, gen_random_uuid(), 'Archive Shelf 4', 'A4', 100, true, 30, '1', NOW()),

    -- Archive Freezer 2 shelves (Device 31)
    (310, gen_random_uuid(), 'Archive Shelf 1', 'A1', 100, true, 31, '1', NOW()),
    (311, gen_random_uuid(), 'Archive Shelf 2', 'A2', 100, true, 31, '1', NOW()),
    (312, gen_random_uuid(), 'Archive Shelf 3', 'A3', 100, true, 31, '1', NOW()),
    (313, gen_random_uuid(), 'Archive Shelf 4', 'A4', 100, true, 31, '1', NOW()),

    -- QC Refrigerator shelves (Device 40)
    (400, gen_random_uuid(), 'QC Controls', 'CTRL', 20, true, 40, '1', NOW()),
    (401, gen_random_uuid(), 'QC Samples', 'SMPL', 20, true, 40, '1', NOW()),

    -- QC Freezer shelves (Device 41)
    (410, gen_random_uuid(), 'Frozen Controls', 'FCTRL', 20, true, 41, '1', NOW()),

    -- Receiving Refrigerator shelves (Device 50)
    (500, gen_random_uuid(), 'Incoming Samples', 'IN', 30, true, 50, '1', NOW()),
    (501, gen_random_uuid(), 'Processing Queue', 'QUEUE', 30, true, 50, '1', NOW())
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}Shelves inserted.${NC}"

echo -e "${YELLOW}Inserting storage racks...${NC}"
execute_sql <<'EOF'
-- Storage Racks (within shelves)
INSERT INTO storage_rack (id, fhir_uuid, label, code, active, parent_shelf_id, sys_user_id, last_updated)
VALUES
    -- Racks in Ultra-Low Freezer 1, Shelf 1 (Shelf 100)
    (1000, gen_random_uuid(), 'Rack 1', 'R1', true, 100, '1', NOW()),
    (1001, gen_random_uuid(), 'Rack 2', 'R2', true, 100, '1', NOW()),
    (1002, gen_random_uuid(), 'Rack 3', 'R3', true, 100, '1', NOW()),

    -- Racks in Ultra-Low Freezer 1, Shelf 2 (Shelf 101)
    (1010, gen_random_uuid(), 'Rack 1', 'R1', true, 101, '1', NOW()),
    (1011, gen_random_uuid(), 'Rack 2', 'R2', true, 101, '1', NOW()),
    (1012, gen_random_uuid(), 'Rack 3', 'R3', true, 101, '1', NOW()),

    -- Racks in Ultra-Low Freezer 1, Shelf 3 (Shelf 102)
    (1020, gen_random_uuid(), 'Rack 1', 'R1', true, 102, '1', NOW()),
    (1021, gen_random_uuid(), 'Rack 2', 'R2', true, 102, '1', NOW()),

    -- Racks in Standard Freezer (Shelf 120)
    (1200, gen_random_uuid(), 'Tray A', 'TA', true, 120, '1', NOW()),
    (1201, gen_random_uuid(), 'Tray B', 'TB', true, 120, '1', NOW()),

    -- Racks in Refrigerator 1 (Shelf 130)
    (1300, gen_random_uuid(), 'Position 1', 'P1', true, 130, '1', NOW()),
    (1301, gen_random_uuid(), 'Position 2', 'P2', true, 130, '1', NOW()),
    (1302, gen_random_uuid(), 'Position 3', 'P3', true, 130, '1', NOW()),

    -- Racks in Refrigerator 1 (Shelf 131)
    (1310, gen_random_uuid(), 'Position 1', 'P1', true, 131, '1', NOW()),
    (1311, gen_random_uuid(), 'Position 2', 'P2', true, 131, '1', NOW()),

    -- Racks in Processing Refrigerator (Shelf 200)
    (2000, gen_random_uuid(), 'Active Tray 1', 'AT1', true, 200, '1', NOW()),
    (2001, gen_random_uuid(), 'Active Tray 2', 'AT2', true, 200, '1', NOW()),

    -- Racks in Processing Refrigerator (Shelf 201)
    (2010, gen_random_uuid(), 'Pending Tray 1', 'PT1', true, 201, '1', NOW()),
    (2011, gen_random_uuid(), 'Pending Tray 2', 'PT2', true, 201, '1', NOW()),

    -- Racks in Archive Freezer 1 (Shelf 300)
    (3000, gen_random_uuid(), 'Archive Rack 1', 'AR1', true, 300, '1', NOW()),
    (3001, gen_random_uuid(), 'Archive Rack 2', 'AR2', true, 300, '1', NOW()),
    (3002, gen_random_uuid(), 'Archive Rack 3', 'AR3', true, 300, '1', NOW()),
    (3003, gen_random_uuid(), 'Archive Rack 4', 'AR4', true, 300, '1', NOW()),

    -- Racks in Archive Freezer 1 (Shelf 301)
    (3010, gen_random_uuid(), 'Archive Rack 1', 'AR1', true, 301, '1', NOW()),
    (3011, gen_random_uuid(), 'Archive Rack 2', 'AR2', true, 301, '1', NOW()),
    (3012, gen_random_uuid(), 'Archive Rack 3', 'AR3', true, 301, '1', NOW()),

    -- Racks in QC Refrigerator (Shelf 400)
    (4000, gen_random_uuid(), 'Control Rack A', 'CRA', true, 400, '1', NOW()),
    (4001, gen_random_uuid(), 'Control Rack B', 'CRB', true, 400, '1', NOW()),

    -- Racks in Receiving (Shelf 500)
    (5000, gen_random_uuid(), 'Incoming Rack 1', 'IR1', true, 500, '1', NOW()),
    (5001, gen_random_uuid(), 'Incoming Rack 2', 'IR2', true, 500, '1', NOW()),
    (5002, gen_random_uuid(), 'Incoming Rack 3', 'IR3', true, 500, '1', NOW())
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}Racks inserted.${NC}"

echo -e "${YELLOW}Inserting storage boxes...${NC}"
execute_sql <<'EOF'
-- Storage Boxes (gridded containers within racks)
-- Note: No ON CONFLICT clause due to deferrable unique constraints in storage_box table
INSERT INTO storage_box (id, fhir_uuid, label, type, rows, columns, position_schema_hint, code, active, parent_rack_id, sys_user_id, last_updated)
VALUES
    -- 96-well plates in Ultra-Low Freezer 1, Rack 1000
    (10000, gen_random_uuid(), 'Plate ULF1-S1-R1-001', '96-well', 8, 12, 'letter-number', 'P001', true, 1000, '1', NOW()),
    (10001, gen_random_uuid(), 'Plate ULF1-S1-R1-002', '96-well', 8, 12, 'letter-number', 'P002', true, 1000, '1', NOW()),
    (10002, gen_random_uuid(), 'Plate ULF1-S1-R1-003', '96-well', 8, 12, 'letter-number', 'P003', true, 1000, '1', NOW()),

    -- 96-well plates in Ultra-Low Freezer 1, Rack 1001
    (10010, gen_random_uuid(), 'Plate ULF1-S1-R2-001', '96-well', 8, 12, 'letter-number', 'P001', true, 1001, '1', NOW()),
    (10011, gen_random_uuid(), 'Plate ULF1-S1-R2-002', '96-well', 8, 12, 'letter-number', 'P002', true, 1001, '1', NOW()),

    -- 384-well plates in Ultra-Low Freezer 1, Rack 1002
    (10020, gen_random_uuid(), 'HTP Plate ULF1-S1-R3-001', '384-well', 16, 24, 'letter-number', 'HTP001', true, 1002, '1', NOW()),
    (10021, gen_random_uuid(), 'HTP Plate ULF1-S1-R3-002', '384-well', 16, 24, 'letter-number', 'HTP002', true, 1002, '1', NOW()),

    -- 9x9 sample boxes in Ultra-Low Freezer 1, Rack 1010
    (10100, gen_random_uuid(), 'Box ULF1-S2-R1-001', '9x9', 9, 9, 'number-number', 'B001', true, 1010, '1', NOW()),
    (10101, gen_random_uuid(), 'Box ULF1-S2-R1-002', '9x9', 9, 9, 'number-number', 'B002', true, 1010, '1', NOW()),
    (10102, gen_random_uuid(), 'Box ULF1-S2-R1-003', '9x9', 9, 9, 'number-number', 'B003', true, 1010, '1', NOW()),
    (10103, gen_random_uuid(), 'Box ULF1-S2-R1-004', '9x9', 9, 9, 'number-number', 'B004', true, 1010, '1', NOW()),

    -- 10x10 sample boxes in Ultra-Low Freezer 1, Rack 1011
    (10110, gen_random_uuid(), 'Box ULF1-S2-R2-001', '10x10', 10, 10, 'number-number', 'B001', true, 1011, '1', NOW()),
    (10111, gen_random_uuid(), 'Box ULF1-S2-R2-002', '10x10', 10, 10, 'number-number', 'B002', true, 1011, '1', NOW()),
    (10112, gen_random_uuid(), 'Box ULF1-S2-R2-003', '10x10', 10, 10, 'number-number', 'B003', true, 1011, '1', NOW()),

    -- Standard freezer boxes, Rack 1200
    (12000, gen_random_uuid(), 'SF Box A1', '9x9', 9, 9, 'letter-number', 'SFA1', true, 1200, '1', NOW()),
    (12001, gen_random_uuid(), 'SF Box A2', '9x9', 9, 9, 'letter-number', 'SFA2', true, 1200, '1', NOW()),

    -- Refrigerator boxes, Rack 1300
    (13000, gen_random_uuid(), 'Ref Box 1', '5x5', 5, 5, 'number-number', 'RB1', true, 1300, '1', NOW()),
    (13001, gen_random_uuid(), 'Ref Box 2', '5x5', 5, 5, 'number-number', 'RB2', true, 1300, '1', NOW()),
    (13002, gen_random_uuid(), 'Ref Box 3', '5x5', 5, 5, 'number-number', 'RB3', true, 1300, '1', NOW()),

    -- Processing active plates, Rack 2000
    (20000, gen_random_uuid(), 'Active Plate 1', '96-well', 8, 12, 'letter-number', 'AP1', true, 2000, '1', NOW()),
    (20001, gen_random_uuid(), 'Active Plate 2', '96-well', 8, 12, 'letter-number', 'AP2', true, 2000, '1', NOW()),

    -- Processing pending boxes, Rack 2010
    (20100, gen_random_uuid(), 'Pending Box 1', '9x9', 9, 9, 'number-number', 'PB1', true, 2010, '1', NOW()),
    (20101, gen_random_uuid(), 'Pending Box 2', '9x9', 9, 9, 'number-number', 'PB2', true, 2010, '1', NOW()),

    -- Archive boxes, Rack 3000
    (30000, gen_random_uuid(), 'Archive 2024-001', '10x10', 10, 10, 'number-number', 'A2024-001', true, 3000, '1', NOW()),
    (30001, gen_random_uuid(), 'Archive 2024-002', '10x10', 10, 10, 'number-number', 'A2024-002', true, 3000, '1', NOW()),
    (30002, gen_random_uuid(), 'Archive 2024-003', '10x10', 10, 10, 'number-number', 'A2024-003', true, 3000, '1', NOW()),
    (30003, gen_random_uuid(), 'Archive 2024-004', '10x10', 10, 10, 'number-number', 'A2024-004', true, 3000, '1', NOW()),

    -- Archive boxes, Rack 3001
    (30010, gen_random_uuid(), 'Archive 2024-005', '10x10', 10, 10, 'number-number', 'A2024-005', true, 3001, '1', NOW()),
    (30011, gen_random_uuid(), 'Archive 2024-006', '10x10', 10, 10, 'number-number', 'A2024-006', true, 3001, '1', NOW()),
    (30012, gen_random_uuid(), 'Archive 2024-007', '10x10', 10, 10, 'number-number', 'A2024-007', true, 3001, '1', NOW()),

    -- Archive boxes, Rack 3010
    (30100, gen_random_uuid(), 'Archive 2023-001', '10x10', 10, 10, 'number-number', 'A2023-001', true, 3010, '1', NOW()),
    (30101, gen_random_uuid(), 'Archive 2023-002', '10x10', 10, 10, 'number-number', 'A2023-002', true, 3010, '1', NOW()),

    -- QC control boxes, Rack 4000
    (40000, gen_random_uuid(), 'QC Control Plate A', '96-well', 8, 12, 'letter-number', 'QCA', true, 4000, '1', NOW()),
    (40001, gen_random_uuid(), 'QC Control Plate B', '96-well', 8, 12, 'letter-number', 'QCB', true, 4000, '1', NOW()),

    -- Receiving incoming boxes, Rack 5000
    (50000, gen_random_uuid(), 'Incoming Today 1', '9x9', 9, 9, 'number-number', 'INC1', true, 5000, '1', NOW()),
    (50001, gen_random_uuid(), 'Incoming Today 2', '9x9', 9, 9, 'number-number', 'INC2', true, 5000, '1', NOW()),
    (50002, gen_random_uuid(), 'Incoming Today 3', '9x9', 9, 9, 'number-number', 'INC3', true, 5000, '1', NOW()),

    -- Receiving queue boxes, Rack 5001
    (50010, gen_random_uuid(), 'Queue Box 1', '9x9', 9, 9, 'number-number', 'Q1', true, 5001, '1', NOW()),
    (50011, gen_random_uuid(), 'Queue Box 2', '9x9', 9, 9, 'number-number', 'Q2', true, 5001, '1', NOW());
EOF
echo -e "${GREEN}Boxes inserted.${NC}"

echo -e "${YELLOW}Updating sequences to avoid ID conflicts...${NC}"
execute_sql <<'EOF'
-- Update sequences to be higher than all inserted IDs
SELECT setval('storage_room_seq', 1000, false);
SELECT setval('storage_device_seq', 1000, false);
SELECT setval('storage_shelf_seq', 1000, false);
SELECT setval('storage_rack_seq', 10000, false);
SELECT setval('storage_box_seq', 100000, false);
EOF
echo -e "${GREEN}Sequences updated.${NC}"

echo ""
echo -e "${GREEN}=== Data Population Complete ===${NC}"
echo ""

# Print summary
echo -e "${YELLOW}Summary:${NC}"
execute_sql -t <<'EOF'
SELECT
    'Rooms' as entity, COUNT(*)::text as count FROM storage_room
UNION ALL
SELECT 'Devices', COUNT(*)::text FROM storage_device
UNION ALL
SELECT 'Shelves', COUNT(*)::text FROM storage_shelf
UNION ALL
SELECT 'Racks', COUNT(*)::text FROM storage_rack
UNION ALL
SELECT 'Boxes', COUNT(*)::text FROM storage_box;
EOF

echo ""
echo -e "${GREEN}Storage location hierarchy created successfully!${NC}"
echo ""
echo "Example query to verify hierarchy:"
echo "  docker exec -i ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \\"
echo "    \"SELECT r.name as room, d.name as device, s.label as shelf,"
echo "            rk.label as rack, b.label as box, b.type"
echo "     FROM storage_box b"
echo "     JOIN storage_rack rk ON b.parent_rack_id = rk.id"
echo "     JOIN storage_shelf s ON rk.parent_shelf_id = s.id"
echo "     JOIN storage_device d ON s.parent_device_id = d.id"
echo "     JOIN storage_room r ON d.parent_room_id = r.id"
echo "     LIMIT 10;\""
