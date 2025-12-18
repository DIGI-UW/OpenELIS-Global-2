#!/bin/bash
#
# populate-tradmed-storage-locations.sh
#
# Populates storage locations specifically for the Traditional Medicine workflow.
# This includes rooms, devices, shelves, and racks for:
# - Refrigerated samples (2-8°C)
# - Dried/Ambient samples (room temperature)
# - Preserved/Ethanol samples
# - Frozen samples (-20°C or colder)
# - Herbarium reference specimens
#
# Usage:
#   ./scripts/populate-tradmed-storage-locations.sh [OPTIONS]
#
# Options:
#   --container     Docker container name (default: openelisglobal-database)
#   -d, --database  Database name (default: clinlims)
#   -U, --user      Database user (default: clinlims)
#   -c, --clean     Clean existing Traditional Medicine storage data before inserting
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
            head -22 "$0" | tail -18
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

echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Traditional Medicine Storage Location Population Script     ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
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
    echo -e "${YELLOW}Cleaning existing Traditional Medicine storage data...${NC}"
    execute_sql <<'EOF'
-- Clean Traditional Medicine specific storage locations (IDs starting with 8xxx, 9xxx)
DELETE FROM storage_box WHERE parent_rack_id IN (SELECT id FROM storage_rack WHERE id >= 8000 AND id < 10000);
DELETE FROM storage_rack WHERE id >= 8000 AND id < 10000;
DELETE FROM storage_shelf WHERE id >= 800 AND id < 1000;
DELETE FROM storage_device WHERE id >= 80 AND id < 100;
DELETE FROM storage_room WHERE id >= 8 AND id < 20;
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
    echo ""
fi

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Inserting Traditional Medicine storage rooms...${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
execute_sql <<'EOF'
-- Traditional Medicine Storage Rooms
-- Using ID range 8-19 to avoid conflicts with general storage (1-7)
-- Note: code is limited to varchar(10)
INSERT INTO storage_room (id, fhir_uuid, name, code, description, active, sys_user_id, last_updated)
VALUES
    -- Main Traditional Medicine Laboratory
    (8, gen_random_uuid(), 'Traditional Medicine Laboratory', 'TM-LAB',
     'Primary laboratory for Traditional Medicine sample processing and storage', true, '1', NOW()),

    -- Herbarium for Reference Specimens
    (9, gen_random_uuid(), 'Herbarium Collection', 'TM-HERB',
     'Climate-controlled room for mounting and storing botanical reference specimens', true, '1', NOW()),

    -- Dried Sample Storage
    (10, gen_random_uuid(), 'Dried Sample Storage', 'TM-DRY',
     'Ambient temperature storage for dried plant and mineral samples', true, '1', NOW()),

    -- Cold Storage Room
    (11, gen_random_uuid(), 'Cold Storage Room', 'TM-COLD',
     'Temperature-controlled room for refrigerated and frozen samples', true, '1', NOW()),

    -- Preserved Sample Storage
    (12, gen_random_uuid(), 'Preservation Room', 'TM-PRES',
     'Room for ethanol-preserved and chemically treated specimens', true, '1', NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    active = EXCLUDED.active,
    last_updated = NOW();
EOF
echo -e "${GREEN}✓ Rooms inserted.${NC}"

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Inserting Traditional Medicine storage devices...${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
execute_sql <<'EOF'
-- Traditional Medicine Storage Devices
-- Using ID range 80-99 to avoid conflicts
INSERT INTO storage_device (id, fhir_uuid, name, code, type, temperature_setting, capacity_limit, active, parent_room_id, sys_user_id, last_updated)
VALUES
    -- Traditional Medicine Laboratory (Room 8) devices
    (80, gen_random_uuid(), 'TradMed Refrigerator 1', 'TM-REF01', 'refrigerator', 4.0, 200, true, 8, '1', NOW()),
    (81, gen_random_uuid(), 'TradMed Refrigerator 2', 'TM-REF02', 'refrigerator', 4.0, 200, true, 8, '1', NOW()),
    (82, gen_random_uuid(), 'TradMed Deep Freezer', 'TM-FRZ01', 'freezer', -20.0, 300, true, 8, '1', NOW()),
    (83, gen_random_uuid(), 'TradMed Ultra-Low Freezer', 'TM-ULF01', 'freezer', -80.0, 500, true, 8, '1', NOW()),
    (84, gen_random_uuid(), 'Sample Processing Cabinet', 'TM-CAB01', 'cabinet', NULL, 100, true, 8, '1', NOW()),

    -- Herbarium Collection (Room 9) devices
    (85, gen_random_uuid(), 'Herbarium Cabinet A', 'HERB-CAB-A', 'cabinet', NULL, 500, true, 9, '1', NOW()),
    (86, gen_random_uuid(), 'Herbarium Cabinet B', 'HERB-CAB-B', 'cabinet', NULL, 500, true, 9, '1', NOW()),
    (87, gen_random_uuid(), 'Herbarium Cabinet C', 'HERB-CAB-C', 'cabinet', NULL, 500, true, 9, '1', NOW()),
    (88, gen_random_uuid(), 'Mounting Station', 'HERB-MOUNT', 'cabinet', NULL, 50, true, 9, '1', NOW()),

    -- Dried Sample Storage (Room 10) devices
    (89, gen_random_uuid(), 'Dried Samples Cabinet 1', 'DRY-CAB01', 'cabinet', NULL, 300, true, 10, '1', NOW()),
    (90, gen_random_uuid(), 'Dried Samples Cabinet 2', 'DRY-CAB02', 'cabinet', NULL, 300, true, 10, '1', NOW()),
    (91, gen_random_uuid(), 'Desiccator Cabinet', 'DRY-DES01', 'cabinet', NULL, 100, true, 10, '1', NOW()),
    (92, gen_random_uuid(), 'Powder Storage Cabinet', 'DRY-PWD01', 'cabinet', NULL, 200, true, 10, '1', NOW()),

    -- Cold Storage Room (Room 11) devices
    (93, gen_random_uuid(), 'Walk-in Refrigerator', 'CLD-REF01', 'refrigerator', 4.0, 1000, true, 11, '1', NOW()),
    (94, gen_random_uuid(), 'Walk-in Freezer', 'CLD-FRZ01', 'freezer', -20.0, 800, true, 11, '1', NOW()),
    (95, gen_random_uuid(), 'Cryogenic Storage', 'CLD-CRYO1', 'freezer', -150.0, 200, true, 11, '1', NOW()),

    -- Preservation Room (Room 12) devices
    (96, gen_random_uuid(), 'Ethanol Storage Cabinet', 'PRS-ETH01', 'cabinet', NULL, 200, true, 12, '1', NOW()),
    (97, gen_random_uuid(), 'Formalin Storage Cabinet', 'PRS-FORM1', 'cabinet', NULL, 150, true, 12, '1', NOW()),
    (98, gen_random_uuid(), 'Wet Specimen Refrigerator', 'PRS-REF01', 'refrigerator', 4.0, 150, true, 12, '1', NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    type = EXCLUDED.type,
    temperature_setting = EXCLUDED.temperature_setting,
    capacity_limit = EXCLUDED.capacity_limit,
    active = EXCLUDED.active,
    parent_room_id = EXCLUDED.parent_room_id,
    last_updated = NOW();
EOF
echo -e "${GREEN}✓ Devices inserted.${NC}"

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Inserting Traditional Medicine storage shelves...${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
execute_sql <<'EOF'
-- Traditional Medicine Storage Shelves
-- Using ID range 800-999 to avoid conflicts
INSERT INTO storage_shelf (id, fhir_uuid, label, code, capacity_limit, active, parent_device_id, sys_user_id, last_updated)
VALUES
    -- TradMed Refrigerator 1 (Device 80) shelves
    (800, gen_random_uuid(), 'Fresh Samples', 'FRESH', 40, true, 80, '1', NOW()),
    (801, gen_random_uuid(), 'Refrigerated Extracts', 'REF-EXT', 40, true, 80, '1', NOW()),
    (802, gen_random_uuid(), 'Pending Analysis', 'PEND-ANAL', 40, true, 80, '1', NOW()),

    -- TradMed Refrigerator 2 (Device 81) shelves
    (803, gen_random_uuid(), 'Plant Material', 'PLANT-MAT', 40, true, 81, '1', NOW()),
    (804, gen_random_uuid(), 'Fungal Samples', 'FUNGAL', 40, true, 81, '1', NOW()),
    (805, gen_random_uuid(), 'Mineral Samples', 'MINERAL', 40, true, 81, '1', NOW()),

    -- TradMed Deep Freezer (Device 82) shelves
    (806, gen_random_uuid(), 'Frozen Plant Tissue', 'FRZ-PLANT', 60, true, 82, '1', NOW()),
    (807, gen_random_uuid(), 'Frozen Extracts', 'FRZ-EXT', 60, true, 82, '1', NOW()),
    (808, gen_random_uuid(), 'Long-term Storage', 'FRZ-LTS', 60, true, 82, '1', NOW()),

    -- TradMed Ultra-Low Freezer (Device 83) shelves
    (809, gen_random_uuid(), 'Genetic Material', 'ULF-GEN', 80, true, 83, '1', NOW()),
    (810, gen_random_uuid(), 'Sensitive Compounds', 'ULF-SENS', 80, true, 83, '1', NOW()),
    (811, gen_random_uuid(), 'Archive Samples', 'ULF-ARCH', 80, true, 83, '1', NOW()),

    -- Sample Processing Cabinet (Device 84) shelves
    (812, gen_random_uuid(), 'Active Processing', 'PROC-ACT', 20, true, 84, '1', NOW()),
    (813, gen_random_uuid(), 'Supplies', 'PROC-SUP', 20, true, 84, '1', NOW()),

    -- Herbarium Cabinet A (Device 85) shelves - organized alphabetically by genus
    (814, gen_random_uuid(), 'Genus A-D', 'HERB-AD', 100, true, 85, '1', NOW()),
    (815, gen_random_uuid(), 'Genus E-H', 'HERB-EH', 100, true, 85, '1', NOW()),
    (816, gen_random_uuid(), 'Genus I-L', 'HERB-IL', 100, true, 85, '1', NOW()),

    -- Herbarium Cabinet B (Device 86) shelves
    (817, gen_random_uuid(), 'Genus M-P', 'HERB-MP', 100, true, 86, '1', NOW()),
    (818, gen_random_uuid(), 'Genus Q-T', 'HERB-QT', 100, true, 86, '1', NOW()),
    (819, gen_random_uuid(), 'Genus U-Z', 'HERB-UZ', 100, true, 86, '1', NOW()),

    -- Herbarium Cabinet C (Device 87) shelves - special collections
    (820, gen_random_uuid(), 'Endemic Species', 'HERB-END', 100, true, 87, '1', NOW()),
    (821, gen_random_uuid(), 'Medicinal Plants', 'HERB-MED', 100, true, 87, '1', NOW()),
    (822, gen_random_uuid(), 'Type Specimens', 'HERB-TYPE', 100, true, 87, '1', NOW()),

    -- Mounting Station (Device 88) shelves
    (823, gen_random_uuid(), 'To Be Mounted', 'MNT-PEND', 25, true, 88, '1', NOW()),
    (824, gen_random_uuid(), 'Recently Mounted', 'MNT-REC', 25, true, 88, '1', NOW()),

    -- Dried Samples Cabinet 1 (Device 89) shelves
    (825, gen_random_uuid(), 'Whole Plant Dried', 'DRY-WHOLE', 60, true, 89, '1', NOW()),
    (826, gen_random_uuid(), 'Dried Leaves', 'DRY-LEAF', 60, true, 89, '1', NOW()),
    (827, gen_random_uuid(), 'Dried Roots', 'DRY-ROOT', 60, true, 89, '1', NOW()),

    -- Dried Samples Cabinet 2 (Device 90) shelves
    (828, gen_random_uuid(), 'Dried Bark', 'DRY-BARK', 60, true, 90, '1', NOW()),
    (829, gen_random_uuid(), 'Dried Seeds', 'DRY-SEED', 60, true, 90, '1', NOW()),
    (830, gen_random_uuid(), 'Dried Flowers', 'DRY-FLOWER', 60, true, 90, '1', NOW()),

    -- Desiccator Cabinet (Device 91) shelves
    (831, gen_random_uuid(), 'Low Moisture Storage', 'DES-LOW', 30, true, 91, '1', NOW()),
    (832, gen_random_uuid(), 'Drying Active', 'DES-ACTIVE', 30, true, 91, '1', NOW()),

    -- Powder Storage Cabinet (Device 92) shelves
    (833, gen_random_uuid(), 'Ground Plant Material', 'PWD-PLANT', 50, true, 92, '1', NOW()),
    (834, gen_random_uuid(), 'Mineral Powders', 'PWD-MIN', 50, true, 92, '1', NOW()),
    (835, gen_random_uuid(), 'Formulation Powders', 'PWD-FORM', 50, true, 92, '1', NOW()),

    -- Walk-in Refrigerator (Device 93) shelves
    (836, gen_random_uuid(), 'Large Samples Section A', 'WALKIN-A', 150, true, 93, '1', NOW()),
    (837, gen_random_uuid(), 'Large Samples Section B', 'WALKIN-B', 150, true, 93, '1', NOW()),
    (838, gen_random_uuid(), 'Bulk Storage', 'WALKIN-BLK', 200, true, 93, '1', NOW()),

    -- Walk-in Freezer (Device 94) shelves
    (839, gen_random_uuid(), 'Frozen Bulk Section A', 'FRZWALK-A', 150, true, 94, '1', NOW()),
    (840, gen_random_uuid(), 'Frozen Bulk Section B', 'FRZWALK-B', 150, true, 94, '1', NOW()),

    -- Cryogenic Storage (Device 95) shelves
    (841, gen_random_uuid(), 'Cryo Tank 1', 'CRYO-T1', 50, true, 95, '1', NOW()),
    (842, gen_random_uuid(), 'Cryo Tank 2', 'CRYO-T2', 50, true, 95, '1', NOW()),

    -- Ethanol Storage Cabinet (Device 96) shelves
    (843, gen_random_uuid(), 'Ethanol 70%', 'ETH-70', 50, true, 96, '1', NOW()),
    (844, gen_random_uuid(), 'Ethanol 95%', 'ETH-95', 50, true, 96, '1', NOW()),
    (845, gen_random_uuid(), 'Methanol', 'METH', 30, true, 96, '1', NOW()),

    -- Formalin Storage Cabinet (Device 97) shelves
    (846, gen_random_uuid(), 'Fixed Specimens', 'FORM-FIXED', 40, true, 97, '1', NOW()),
    (847, gen_random_uuid(), 'Pending Fixation', 'FORM-PEND', 30, true, 97, '1', NOW()),

    -- Wet Specimen Refrigerator (Device 98) shelves
    (848, gen_random_uuid(), 'Preserved Plant Parts', 'WET-PLANT', 40, true, 98, '1', NOW()),
    (849, gen_random_uuid(), 'Preserved Fungi', 'WET-FUNGI', 30, true, 98, '1', NOW()),
    (850, gen_random_uuid(), 'Preserved Animal Parts', 'WET-ANIMAL', 30, true, 98, '1', NOW())
ON CONFLICT (id) DO UPDATE SET
    label = EXCLUDED.label,
    code = EXCLUDED.code,
    capacity_limit = EXCLUDED.capacity_limit,
    active = EXCLUDED.active,
    parent_device_id = EXCLUDED.parent_device_id,
    last_updated = NOW();
EOF
echo -e "${GREEN}✓ Shelves inserted.${NC}"

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Inserting Traditional Medicine storage racks...${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
execute_sql <<'EOF'
-- Traditional Medicine Storage Racks
-- Using ID range 8000-9999 to avoid conflicts
-- Note: For Traditional Medicine, racks hold containers/jars/bags, not 96-well plates
INSERT INTO storage_rack (id, fhir_uuid, label, code, active, parent_shelf_id, sys_user_id, last_updated)
VALUES
    -- Fresh Samples shelf (800) racks
    (8000, gen_random_uuid(), 'Fresh Row 1', 'FR1', true, 800, '1', NOW()),
    (8001, gen_random_uuid(), 'Fresh Row 2', 'FR2', true, 800, '1', NOW()),
    (8002, gen_random_uuid(), 'Fresh Row 3', 'FR3', true, 800, '1', NOW()),

    -- Refrigerated Extracts shelf (801) racks
    (8010, gen_random_uuid(), 'Extract Row A', 'ERA', true, 801, '1', NOW()),
    (8011, gen_random_uuid(), 'Extract Row B', 'ERB', true, 801, '1', NOW()),

    -- Pending Analysis shelf (802) racks
    (8020, gen_random_uuid(), 'Analysis Queue 1', 'AQ1', true, 802, '1', NOW()),
    (8021, gen_random_uuid(), 'Analysis Queue 2', 'AQ2', true, 802, '1', NOW()),

    -- Plant Material shelf (803) racks
    (8030, gen_random_uuid(), 'Plants Tray 1', 'PT1', true, 803, '1', NOW()),
    (8031, gen_random_uuid(), 'Plants Tray 2', 'PT2', true, 803, '1', NOW()),
    (8032, gen_random_uuid(), 'Plants Tray 3', 'PT3', true, 803, '1', NOW()),

    -- Fungal Samples shelf (804) racks
    (8040, gen_random_uuid(), 'Fungi Tray 1', 'FT1', true, 804, '1', NOW()),
    (8041, gen_random_uuid(), 'Fungi Tray 2', 'FT2', true, 804, '1', NOW()),

    -- Mineral Samples shelf (805) racks
    (8050, gen_random_uuid(), 'Minerals Tray 1', 'MT1', true, 805, '1', NOW()),
    (8051, gen_random_uuid(), 'Minerals Tray 2', 'MT2', true, 805, '1', NOW()),

    -- Frozen Plant Tissue shelf (806) racks
    (8060, gen_random_uuid(), 'Frozen Tissue Box 1', 'FTB1', true, 806, '1', NOW()),
    (8061, gen_random_uuid(), 'Frozen Tissue Box 2', 'FTB2', true, 806, '1', NOW()),
    (8062, gen_random_uuid(), 'Frozen Tissue Box 3', 'FTB3', true, 806, '1', NOW()),

    -- Frozen Extracts shelf (807) racks
    (8070, gen_random_uuid(), 'Frozen Extract Box A', 'FEA', true, 807, '1', NOW()),
    (8071, gen_random_uuid(), 'Frozen Extract Box B', 'FEB', true, 807, '1', NOW()),

    -- Long-term Storage shelf (808) racks
    (8080, gen_random_uuid(), 'LTS Box 1', 'LTS1', true, 808, '1', NOW()),
    (8081, gen_random_uuid(), 'LTS Box 2', 'LTS2', true, 808, '1', NOW()),
    (8082, gen_random_uuid(), 'LTS Box 3', 'LTS3', true, 808, '1', NOW()),

    -- Genetic Material shelf (809) racks - Ultra-Low
    (8090, gen_random_uuid(), 'DNA Box 1', 'DNA1', true, 809, '1', NOW()),
    (8091, gen_random_uuid(), 'DNA Box 2', 'DNA2', true, 809, '1', NOW()),
    (8092, gen_random_uuid(), 'RNA Box 1', 'RNA1', true, 809, '1', NOW()),

    -- Sensitive Compounds shelf (810) racks
    (8100, gen_random_uuid(), 'Compounds Box A', 'CBA', true, 810, '1', NOW()),
    (8101, gen_random_uuid(), 'Compounds Box B', 'CBB', true, 810, '1', NOW()),

    -- Archive Samples shelf (811) racks
    (8110, gen_random_uuid(), 'Archive 2024', 'A2024', true, 811, '1', NOW()),
    (8111, gen_random_uuid(), 'Archive 2023', 'A2023', true, 811, '1', NOW()),
    (8112, gen_random_uuid(), 'Archive 2022', 'A2022', true, 811, '1', NOW()),

    -- Herbarium shelves - folders/sections within each shelf
    -- Genus A-D (814) racks
    (8140, gen_random_uuid(), 'Folder A', 'FA', true, 814, '1', NOW()),
    (8141, gen_random_uuid(), 'Folder B', 'FB', true, 814, '1', NOW()),
    (8142, gen_random_uuid(), 'Folder C', 'FC', true, 814, '1', NOW()),
    (8143, gen_random_uuid(), 'Folder D', 'FD', true, 814, '1', NOW()),

    -- Genus E-H (815) racks
    (8150, gen_random_uuid(), 'Folder E', 'FE', true, 815, '1', NOW()),
    (8151, gen_random_uuid(), 'Folder F', 'FF', true, 815, '1', NOW()),
    (8152, gen_random_uuid(), 'Folder G', 'FG', true, 815, '1', NOW()),
    (8153, gen_random_uuid(), 'Folder H', 'FH', true, 815, '1', NOW()),

    -- Genus I-L (816) racks
    (8160, gen_random_uuid(), 'Folder I', 'FI', true, 816, '1', NOW()),
    (8161, gen_random_uuid(), 'Folder J-K', 'FJK', true, 816, '1', NOW()),
    (8162, gen_random_uuid(), 'Folder L', 'FL', true, 816, '1', NOW()),

    -- Genus M-P (817) racks
    (8170, gen_random_uuid(), 'Folder M', 'FM', true, 817, '1', NOW()),
    (8171, gen_random_uuid(), 'Folder N-O', 'FNO', true, 817, '1', NOW()),
    (8172, gen_random_uuid(), 'Folder P', 'FP', true, 817, '1', NOW()),

    -- Genus Q-T (818) racks
    (8180, gen_random_uuid(), 'Folder Q-R', 'FQR', true, 818, '1', NOW()),
    (8181, gen_random_uuid(), 'Folder S', 'FS', true, 818, '1', NOW()),
    (8182, gen_random_uuid(), 'Folder T', 'FT', true, 818, '1', NOW()),

    -- Genus U-Z (819) racks
    (8190, gen_random_uuid(), 'Folder U-V', 'FUV', true, 819, '1', NOW()),
    (8191, gen_random_uuid(), 'Folder W-Z', 'FWZ', true, 819, '1', NOW()),

    -- Endemic Species (820) racks
    (8200, gen_random_uuid(), 'Endemic Section 1', 'EN1', true, 820, '1', NOW()),
    (8201, gen_random_uuid(), 'Endemic Section 2', 'EN2', true, 820, '1', NOW()),
    (8202, gen_random_uuid(), 'Endemic Section 3', 'EN3', true, 820, '1', NOW()),

    -- Medicinal Plants (821) racks
    (8210, gen_random_uuid(), 'Medicinal Folder 1', 'MED1', true, 821, '1', NOW()),
    (8211, gen_random_uuid(), 'Medicinal Folder 2', 'MED2', true, 821, '1', NOW()),
    (8212, gen_random_uuid(), 'Medicinal Folder 3', 'MED3', true, 821, '1', NOW()),

    -- Type Specimens (822) racks
    (8220, gen_random_uuid(), 'Type Collection A', 'TCA', true, 822, '1', NOW()),
    (8221, gen_random_uuid(), 'Type Collection B', 'TCB', true, 822, '1', NOW()),

    -- Dried samples racks
    -- Whole Plant Dried (825) racks
    (8250, gen_random_uuid(), 'Whole Plants Row 1', 'WP1', true, 825, '1', NOW()),
    (8251, gen_random_uuid(), 'Whole Plants Row 2', 'WP2', true, 825, '1', NOW()),
    (8252, gen_random_uuid(), 'Whole Plants Row 3', 'WP3', true, 825, '1', NOW()),

    -- Dried Leaves (826) racks
    (8260, gen_random_uuid(), 'Leaves Jar Row 1', 'LJ1', true, 826, '1', NOW()),
    (8261, gen_random_uuid(), 'Leaves Jar Row 2', 'LJ2', true, 826, '1', NOW()),

    -- Dried Roots (827) racks
    (8270, gen_random_uuid(), 'Roots Container 1', 'RC1', true, 827, '1', NOW()),
    (8271, gen_random_uuid(), 'Roots Container 2', 'RC2', true, 827, '1', NOW()),

    -- Dried Bark (828) racks
    (8280, gen_random_uuid(), 'Bark Bag Row 1', 'BB1', true, 828, '1', NOW()),
    (8281, gen_random_uuid(), 'Bark Bag Row 2', 'BB2', true, 828, '1', NOW()),

    -- Dried Seeds (829) racks
    (8290, gen_random_uuid(), 'Seeds Container 1', 'SC1', true, 829, '1', NOW()),
    (8291, gen_random_uuid(), 'Seeds Container 2', 'SC2', true, 829, '1', NOW()),

    -- Dried Flowers (830) racks
    (8300, gen_random_uuid(), 'Flowers Container 1', 'FC1', true, 830, '1', NOW()),
    (8301, gen_random_uuid(), 'Flowers Container 2', 'FC2', true, 830, '1', NOW()),

    -- Ground Plant Material (833) racks
    (8330, gen_random_uuid(), 'Ground Material Row 1', 'GM1', true, 833, '1', NOW()),
    (8331, gen_random_uuid(), 'Ground Material Row 2', 'GM2', true, 833, '1', NOW()),

    -- Ethanol 70% (843) racks
    (8430, gen_random_uuid(), 'Ethanol 70 Row 1', 'E70-1', true, 843, '1', NOW()),
    (8431, gen_random_uuid(), 'Ethanol 70 Row 2', 'E70-2', true, 843, '1', NOW()),

    -- Ethanol 95% (844) racks
    (8440, gen_random_uuid(), 'Ethanol 95 Row 1', 'E95-1', true, 844, '1', NOW()),
    (8441, gen_random_uuid(), 'Ethanol 95 Row 2', 'E95-2', true, 844, '1', NOW()),

    -- Preserved Plant Parts (848) racks
    (8480, gen_random_uuid(), 'Preserved Plants 1', 'PP1', true, 848, '1', NOW()),
    (8481, gen_random_uuid(), 'Preserved Plants 2', 'PP2', true, 848, '1', NOW()),
    (8482, gen_random_uuid(), 'Preserved Plants 3', 'PP3', true, 848, '1', NOW())
ON CONFLICT (id) DO UPDATE SET
    label = EXCLUDED.label,
    code = EXCLUDED.code,
    active = EXCLUDED.active,
    parent_shelf_id = EXCLUDED.parent_shelf_id,
    last_updated = NOW();
EOF
echo -e "${GREEN}✓ Racks inserted.${NC}"

echo ""
echo -e "${YELLOW}Updating sequences to avoid ID conflicts...${NC}"
execute_sql <<'EOF'
-- Update sequences to be higher than all inserted IDs
-- Only update if current value is lower than our range
DO $$
BEGIN
    IF (SELECT last_value FROM storage_room_seq) < 100 THEN
        PERFORM setval('storage_room_seq', 100, false);
    END IF;
    IF (SELECT last_value FROM storage_device_seq) < 200 THEN
        PERFORM setval('storage_device_seq', 200, false);
    END IF;
    IF (SELECT last_value FROM storage_shelf_seq) < 1000 THEN
        PERFORM setval('storage_shelf_seq', 1000, false);
    END IF;
    IF (SELECT last_value FROM storage_rack_seq) < 10000 THEN
        PERFORM setval('storage_rack_seq', 10000, false);
    END IF;
END $$;
EOF
echo -e "${GREEN}✓ Sequences updated.${NC}"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           Data Population Complete!                          ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Print summary
echo -e "${CYAN}Summary of Traditional Medicine Storage Locations:${NC}"
echo ""
execute_sql -t <<'EOF'
SELECT '  Rooms:   ' || COUNT(*)::text FROM storage_room WHERE id >= 8 AND id < 20
UNION ALL
SELECT '  Devices: ' || COUNT(*)::text FROM storage_device WHERE id >= 80 AND id < 100
UNION ALL
SELECT '  Shelves: ' || COUNT(*)::text FROM storage_shelf WHERE id >= 800 AND id < 1000
UNION ALL
SELECT '  Racks:   ' || COUNT(*)::text FROM storage_rack WHERE id >= 8000 AND id < 10000;
EOF

echo ""
echo -e "${CYAN}Storage Types Available:${NC}"
echo "  • Refrigerated (2-8°C)      - Devices 80, 81, 93, 98"
echo "  • Frozen (-20°C)            - Devices 82, 94"
echo "  • Ultra-Low (-80°C)         - Device 83"
echo "  • Cryogenic (-150°C)        - Device 95"
echo "  • Dried/Ambient             - Devices 89, 90, 91, 92"
echo "  • Preserved (Ethanol/etc)   - Devices 96, 97, 98"
echo "  • Herbarium                 - Devices 85, 86, 87, 88"
echo ""

echo -e "${CYAN}Sample Hierarchy Query:${NC}"
echo "  docker exec -i ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \\"
echo "    \"SELECT r.name as room, d.name as device, d.temperature_setting as temp,"
echo "            s.label as shelf, rk.label as rack"
echo "     FROM storage_rack rk"
echo "     JOIN storage_shelf s ON rk.parent_shelf_id = s.id"
echo "     JOIN storage_device d ON s.parent_device_id = d.id"
echo "     JOIN storage_room r ON d.parent_room_id = r.id"
echo "     WHERE r.id >= 8 AND r.id < 20"
echo "     ORDER BY r.name, d.name, s.label, rk.label"
echo "     LIMIT 15;\""
echo ""
echo -e "${GREEN}Traditional Medicine storage locations are ready!${NC}"
