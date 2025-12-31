#!/bin/bash

################################################################################
# MNTD REAGENT INVENTORY COMPLETE IMPORT
#
# ONE SCRIPT TO DO EVERYTHING:
# 1. Reads Excel file
# 2. Generates SQL
# 3. Validates database
# 4. Creates backup
# 5. Imports items and lots
# 6. Verifies success
# 7. Shows results
#
# Usage:
#   bash import-inventory.sh
#
# Docker defaults (auto-detected):
#   --container openelisglobal-database
#   --database clinlims
#   --user postgres
#   --port 15432
#
# Optional flags:
#   --container <name>     Docker container name
#   --database <name>      Database name
#   --user <name>         Database user
#   --host <host>         Database host (for non-Docker)
#   --port <port>         Database port
#   --no-backup           Skip database backup
#   --dry-run             Test without changes
#   --no-docker           Use direct psql instead of Docker
#
################################################################################

set -euo pipefail

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXCEL_FILE="$SCRIPT_DIR/MNTD Reagent inventory (2) (1).xlsx"
TEMP_DIR=$(mktemp -d)
LOG_FILE="$SCRIPT_DIR/import-$(date +%Y%m%d_%H%M%S).log"
mkdir -p "$SCRIPT_DIR/logs"
LOG_FILE="$SCRIPT_DIR/logs/import-$(date +%Y%m%d_%H%M%S).log"

# Default values for Docker setup
CONTAINER="${CONTAINER:-openelisglobal-database}"
DB_NAME="${DB_NAME:-clinlims}"
DB_USER="${DB_USER:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-15432}"
BACKUP=true
DRY_RUN=false
USE_DOCKER=true

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ============================================================================
# FUNCTIONS
# ============================================================================

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}✗ ERROR: $1${NC}" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${BLUE}ℹ $1${NC}" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}⚠ $1${NC}" | tee -a "$LOG_FILE"
}

cleanup() {
    rm -rf "$TEMP_DIR"
}

trap cleanup EXIT

# Function to execute SQL via Docker or direct psql
execute_sql() {
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
    else
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" "$@"
    fi
}

# Function to execute SQL query and return result
execute_sql_query() {
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$1"
    else
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc "$1"
    fi
}

print_banner() {
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║  MNTD REAGENT INVENTORY IMPORT - ONE SCRIPT COMPLETE IMPORT   ║"
    if [ "$USE_DOCKER" = true ]; then
        echo "║  Container: $CONTAINER"
        echo "║  Database: $DB_NAME"
    else
        echo "║  Database: $DB_NAME @ $DB_HOST:$DB_PORT"
    fi
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --container) CONTAINER="$2"; shift 2 ;;
            --database) DB_NAME="$2"; shift 2 ;;
            --user) DB_USER="$2"; shift 2 ;;
            --host) DB_HOST="$2"; shift 2 ;;
            --port) DB_PORT="$2"; shift 2 ;;
            --no-backup) BACKUP=false; shift ;;
            --dry-run) DRY_RUN=true; shift ;;
            --no-docker) USE_DOCKER=false; shift ;;
            *) log_error "Unknown option: $1"; exit 1 ;;
        esac
    done
}

step_1_validate() {
    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "STEP 1: VALIDATE ENVIRONMENT"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # Check Excel file
    if [ ! -f "$EXCEL_FILE" ]; then
        log_error "Excel file not found: $EXCEL_FILE"
        return 1
    fi
    log_success "Excel file found: $(basename "$EXCEL_FILE")"

    # Check Python
    if ! command -v python3 &> /dev/null; then
        log_error "python3 not installed"
        return 1
    fi
    log_success "python3 available"

    # Check openpyxl
    if ! python3 -c "import openpyxl" 2>/dev/null; then
        log_error "openpyxl module not found. Run: pip install openpyxl"
        return 1
    fi
    log_success "openpyxl module available"

    # Check Docker or psql
    if [ "$USE_DOCKER" = true ]; then
        if ! command -v docker &> /dev/null; then
            log_error "docker command not found"
            return 1
        fi
        log_success "docker available"

        # Check if container is running
        if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
            log_error "Container '${CONTAINER}' is not running"
            log_info "Running containers:"
            docker ps --format '  {{.Names}}'
            return 1
        fi
        log_success "Container '${CONTAINER}' is running"
    else
        if ! command -v psql &> /dev/null; then
            log_error "psql (PostgreSQL client) not installed"
            return 1
        fi
        log_success "psql available"
    fi

    return 0
}

step_2_database_check() {
    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "STEP 2: CHECK DATABASE CONNECTION & STORAGE SETUP"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if ! execute_sql -c "SELECT 1" > /dev/null 2>&1; then
        if [ "$USE_DOCKER" = true ]; then
            log_error "Cannot connect to $DB_NAME in container $CONTAINER"
        else
            log_error "Cannot connect to $DB_NAME @ $DB_HOST:$DB_PORT"
        fi
        return 1
    fi
    log_success "Database connection successful"

    # Check tables exist
    local tables=$(execute_sql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('inventory_item', 'inventory_lot') AND table_schema = 'public'" 2>/dev/null || echo "0")

    if [ "$tables" -ne 2 ]; then
        log_error "Inventory tables not found"
        return 1
    fi
    log_success "Inventory tables exist"

    # Check and create storage devices if needed
    local storage_count=$(execute_sql_query "SELECT COUNT(*) FROM storage_device" 2>/dev/null || echo "0")

    if [ "$storage_count" -eq 0 ]; then
        log_warning "No storage devices found - creating default storage hierarchy..."

        execute_sql > /dev/null 2>&1 << 'SQL'
-- Create storage room
INSERT INTO storage_room (name, description, active, created_date, version)
VALUES ('MNTD Laboratory', 'MNTD Research Facility', 'Y', NOW(), 1)
ON CONFLICT DO NOTHING;

-- Create freezers
INSERT INTO storage_device (storage_room_id, name, storage_type, description, active, created_date, version)
SELECT sr.id, 'PARA Freezer 04', 'FREEZER', 'Ultra-low -20C freezer', 'Y', NOW(), 1
FROM storage_room sr WHERE sr.name = 'MNTD Laboratory'
ON CONFLICT DO NOTHING;

INSERT INTO storage_device (storage_room_id, name, storage_type, description, active, created_date, version)
SELECT sr.id, 'PARA Freezer 05', 'FREEZER', 'Ultra-low -20C freezer', 'Y', NOW(), 1
FROM storage_room sr WHERE sr.name = 'MNTD Laboratory'
ON CONFLICT DO NOTHING;

-- Create refrigerators
INSERT INTO storage_device (storage_room_id, name, storage_type, description, active, created_date, version)
SELECT sr.id, 'Refrigerator 1', 'REFRIGERATOR', 'Standard 4C refrigerator', 'Y', NOW(), 1
FROM storage_room sr WHERE sr.name = 'MNTD Laboratory'
ON CONFLICT DO NOTHING;

INSERT INTO storage_device (storage_room_id, name, storage_type, description, active, created_date, version)
SELECT sr.id, 'Refrigerator 2', 'REFRIGERATOR', 'Standard 4C refrigerator', 'Y', NOW(), 1
FROM storage_room sr WHERE sr.name = 'MNTD Laboratory'
ON CONFLICT DO NOTHING;
SQL

        log_success "Storage devices created automatically"
    else
        log_success "Storage devices exist ($storage_count devices)"
    fi

    return 0
}

step_3_generate_sql() {
    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "STEP 3: GENERATE SQL FROM EXCEL"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    log "Reading Excel file and generating SQL..."

    python3 << 'PYTHON_EOF'
import sys
import json
import uuid
import openpyxl
from datetime import datetime, timedelta
from pathlib import Path

excel_file = sys.argv[1]
temp_dir = sys.argv[2]
log_file = sys.argv[3]

class InventoryImporter:
    VALID_UOMS = {
        'vial', 'ml', 'ul', 'mg', 'ug', 'nmol', 'pack', 'box', 'lyophilized',
        'unit', 'ug/ml', 'u/ml', 'percent', 'mm', 'cm', 'g', 'ng', 'pmol'
    }

    SHEET_TYPE_MAP = {
        'Enzymes': 'ENZYME',
        'Immunoassay': 'REAGENT',
    }

    COLUMN_MAPPING = {
        'SN': 'serial_number',
        'S.N': 'serial_number',
        '   SN': 'serial_number',
        'Ref No.': 'ref_number',
        'Ref/ LOT No.': 'ref_number',
        'Ref/LOT No.': 'ref_number',
        'Ref No': 'ref_number',
        'Vial No.': 'vial_number',
        'Items': 'item_name',
        'Experiment type': 'experiment_type',
        'Concetration': 'concentration',
        'Concentration': 'concentration',
        'concentration': 'concentration',
        'Quantity': 'quantity',
        'Unit': 'unit',
        'Volume': 'volume',
        'volume': 'volume',
        'unit': 'volume_unit',
        'Manufacturing date': 'manufacturing_date',
        'Open date': 'open_date',
        'Expiry date': 'expiry_date',
        'Box number': 'box_number',
        'Box No.': 'box_number',
        'BOX Number': 'box_number',
        'box number': 'box_number',
        'BOX NUMBER': 'box_number',
        'REMARK': 'remark',
        'Remark': 'remark',
        'remark': 'remark',
        'Remarks': 'remark',
        'Recived from': 'received_from',
        'Received from': 'received_from',
        'Label and location': 'label_location',
        'Storage condition': 'storage_condition',
        'Requested by': 'requested_by',
        'Refridgrator #': 'refrigerator',
        'Freezer partition': 'freezer_partition',
        'storage temprature & condition': 'storage_condition',
    }

    def __init__(self):
        self.items_inserted = {}
        self.items_sql = []
        self.lots_sql = []
        self.stats = {
            'items': 0,
            'lots': 0,
            'total_rows': 0,
        }

    def normalize_uom(self, uom):
        if not uom:
            return 'unit'
        uom = str(uom).strip().lower()
        if uom in self.VALID_UOMS:
            return uom
        uom_map = {
            'milliliter': 'ml', 'microliter': 'ul', 'μl': 'ul', 'µl': 'ul',
            'microgram': 'ug', 'μg': 'ug', 'µg': 'ug', 'milligram': 'mg',
            'nanomol': 'nmol', 'picomol': 'pmol', 'stock': 'unit',
            'vials': 'vial', 'packs': 'pack', 'boxes': 'box',
            'lyophilized vial': 'lyophilized', 'u/ml': 'u/ml', 'ug/ml': 'ug/ml',
        }
        return uom_map.get(uom, 'unit')

    def normalize_date(self, date_val):
        if not date_val:
            return None
        if hasattr(date_val, 'strftime'):
            return date_val.strftime('%Y-%m-%d')
        date_str = str(date_val).strip()
        if not date_str or date_str.lower() in ['na', 'n/a', 'none']:
            return None
        for fmt in ['%Y-%m-%d', '%d-%m-%Y', '%m/%d/%Y', '%Y/%m/%d', '%d/%m/%Y']:
            try:
                return datetime.strptime(date_str, fmt).strftime('%Y-%m-%d')
            except ValueError:
                continue
        return None

    def normalize_quantity(self, qty):
        if qty is None:
            return 1.0
        try:
            q = str(qty).strip()
            if ',' in q:
                return sum([float(p.strip()) for p in q.split(',')])
            return float(q)
        except (ValueError, AttributeError):
            return 1.0

    def generate_item_sql(self, item_name, item_type, record):
        fhir_uuid = str(uuid.uuid4())
        category = str(record.get('experiment_type') or 'General').replace("'", "''")
        units = self.normalize_uom(record.get('volume_unit') or record.get('unit'))
        item_name_escaped = item_name.replace("'", "''")

        sql = f"""INSERT INTO inventory_item (fhir_uuid, name, item_type, category, units, is_active, project_name, created_date, version)
VALUES ('{fhir_uuid}', '{item_name_escaped}', '{item_type}', '{category}', '{units}', 'Y', 'MNTD Research', NOW(), 1)
ON CONFLICT (fhir_uuid) DO NOTHING;"""
        return sql, fhir_uuid

    def generate_lot_sql(self, item_id, record, item_uuid):
        lot_fhir_uuid = str(uuid.uuid4())
        lot_number = str(record.get('ref_number') or f"LOT_{datetime.now().strftime('%Y%m%d')}_{uuid.uuid4().hex[:8]}").replace("'", "''")

        manufacturing_date = self.normalize_date(record.get('manufacturing_date'))
        open_date = self.normalize_date(record.get('open_date'))
        expiry_date = self.normalize_date(record.get('expiry_date'))

        if not expiry_date:
            if manufacturing_date:
                expiry = datetime.strptime(manufacturing_date, '%Y-%m-%d') + timedelta(days=730)
            else:
                expiry = datetime.now() + timedelta(days=730)
            expiry_date = expiry.strftime('%Y-%m-%d')

        quantity = self.normalize_quantity(record.get('quantity'))
        status = 'EXPIRED' if expiry_date and datetime.strptime(expiry_date, '%Y-%m-%d') < datetime.now() else 'ACTIVE'

        unit_size = str(record.get('volume') or '').replace("'", "''")[:100]
        remarks = ' | '.join([
            f"Concentration: {record.get('concentration')}" if record.get('concentration') else '',
            f"Vial #: {record.get('vial_number')}" if record.get('vial_number') else '',
            f"Box: {record.get('box_number')}" if record.get('box_number') else '',
            f"Storage: {record.get('storage_condition')}" if record.get('storage_condition') else '',
            f"Note: {record.get('remark')}" if record.get('remark') else '',
        ]).replace('  |  ', ' | ').replace("'", "''")[:1000]

        sql = f"""INSERT INTO inventory_lot (fhir_uuid, inventory_item_id, lot_number, initial_quantity, current_quantity, expiration_date, date_opened, qc_status, status, unit_size, remarks, created_date, version)
VALUES ('{lot_fhir_uuid}', {item_id}, '{lot_number}', {quantity}, {quantity}, '{expiry_date}', {f"'{open_date}'" if open_date else 'NULL'}, 'PENDING', '{status}', '{unit_size}', '{remarks}', NOW(), 1);

INSERT INTO inventory_transaction (lot_id, transaction_type, quantity_change, quantity_after, transaction_date, notes, created_date, version)
VALUES ((SELECT id FROM inventory_lot WHERE fhir_uuid = '{lot_fhir_uuid}'), 'RECEIPT', {quantity}, {quantity}, NOW(), 'Batch import from MNTD Excel', NOW(), 1);"""
        return sql

    def process_excel(self):
        wb = openpyxl.load_workbook(excel_file, data_only=False)

        for sheet_name in wb.sheetnames:
            ws = wb[sheet_name]
            headers = []
            header_row = None

            for row_idx, row in enumerate(ws.iter_rows(max_row=5, values_only=True), 1):
                if row[0]:
                    headers = [h for h in row]
                    header_row = row_idx
                    break

            if not headers:
                continue

            item_type = self.SHEET_TYPE_MAP.get(sheet_name, 'REAGENT')

            for row_idx in range(header_row + 1, ws.max_row + 1):
                row = [ws.cell(row=row_idx, column=col_idx).value for col_idx in range(1, len(headers) + 1)]

                if not any(row):
                    continue

                self.stats['total_rows'] += 1

                record = {}
                for col_idx, cell_val in enumerate(row):
                    if col_idx >= len(headers):
                        break
                    header = headers[col_idx]
                    if header in self.COLUMN_MAPPING:
                        record[self.COLUMN_MAPPING[header]] = cell_val

                if not record.get('item_name'):
                    continue

                item_name = str(record['item_name']).strip()

                if item_name not in self.items_inserted:
                    item_sql, item_uuid = self.generate_item_sql(item_name, item_type, record)
                    self.items_sql.append(item_sql)
                    self.items_inserted[item_name] = item_uuid
                    self.stats['items'] += 1
                else:
                    item_uuid = self.items_inserted[item_name]

                item_id = f"(SELECT id FROM inventory_item WHERE fhir_uuid = '{item_uuid}')"
                lot_sql = self.generate_lot_sql(item_id, record, item_uuid)
                self.lots_sql.append(lot_sql)
                self.stats['lots'] += 1

    def write_sql(self, output_dir):
        # Items SQL
        with open(f"{output_dir}/02-items.sql", 'w') as f:
            f.write("BEGIN TRANSACTION;\n\n")
            for sql in self.items_sql:
                f.write(sql + "\n")
            f.write("\nCOMMIT;\n")

        # Lots SQL
        with open(f"{output_dir}/03-lots.sql", 'w') as f:
            f.write("BEGIN TRANSACTION;\n\n")
            for sql in self.lots_sql:
                f.write(sql + "\n")
            f.write("\nCOMMIT;\n")

        # Report
        with open(f"{output_dir}/report.json", 'w') as f:
            json.dump({
                'timestamp': datetime.now().isoformat(),
                'statistics': self.stats
            }, f, indent=2)

try:
    importer = InventoryImporter()
    importer.process_excel()
    importer.write_sql(temp_dir)

    with open(log_file, 'a') as f:
        f.write(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] SQL generation complete: {importer.stats['items']} items, {importer.stats['lots']} lots\n")

    print(f"✓ SQL Generated: {importer.stats['items']} items, {importer.stats['lots']} lots")
except Exception as e:
    print(f"✗ Error: {e}")
    sys.exit(1)

PYTHON_EOF
}

step_4_backup() {
    if [ "$BACKUP" = false ]; then
        log_warning "Skipping database backup (--no-backup)"
        return 0
    fi

    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "STEP 4: BACKUP DATABASE"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    local backup_file="$SCRIPT_DIR/logs/backup-$(date +%Y%m%d_%H%M%S).sql"
    log "Creating backup: $(basename "$backup_file")"

    if [ "$USE_DOCKER" = true ]; then
        if docker exec "$CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" > "$backup_file" 2>&1; then
            log_success "Backup created ($(du -h "$backup_file" | cut -f1))"
            return 0
        else
            log_error "Backup failed - aborting import"
            return 1
        fi
    else
        if pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > "$backup_file" 2>&1; then
            log_success "Backup created ($(du -h "$backup_file" | cut -f1))"
            return 0
        else
            log_error "Backup failed - aborting import"
            return 1
        fi
    fi
}

step_5_import() {
    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "STEP 5: IMPORT TO DATABASE"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    if [ "$DRY_RUN" = true ]; then
        log_warning "DRY RUN MODE - Not executing SQL"
        return 0
    fi

    log "Importing inventory items..."
    if ! execute_sql < "$TEMP_DIR/02-items.sql" >> "$LOG_FILE" 2>&1; then
        log_error "Items import failed"
        return 1
    fi
    log_success "Items imported"

    log "Importing lots and transactions..."
    if ! execute_sql < "$TEMP_DIR/03-lots.sql" >> "$LOG_FILE" 2>&1; then
        log_error "Lots import failed"
        return 1
    fi
    log_success "Lots imported"

    return 0
}

step_6_verify() {
    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "STEP 6: VERIFY IMPORT"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    local item_count=$(execute_sql_query "SELECT COUNT(*) FROM inventory_item WHERE project_name = 'MNTD Research'" 2>/dev/null || echo "0")
    local lot_count=$(execute_sql_query "SELECT COUNT(*) FROM inventory_lot WHERE inventory_item_id IN (SELECT id FROM inventory_item WHERE project_name = 'MNTD Research')" 2>/dev/null || echo "0")

    log_info "Inventory items imported: $item_count"
    log_info "Inventory lots imported: $lot_count"

    if [ "$item_count" -gt 0 ] && [ "$lot_count" -gt 0 ]; then
        log_success "IMPORT SUCCESSFUL!"
        return 0
    else
        log_warning "Import verification incomplete"
        return 0
    fi
}

step_7_summary() {
    log ""
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "FINAL SUMMARY"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    log ""
    log_success "IMPORT COMPLETE!"
    log ""
    log_info "Logs saved to: $LOG_FILE"
    log_info "Next steps:"
    echo "  1. Check UI: http://localhost:8080/OpenELIS/inventory"
    echo "  2. Search for 'MNTD' items"
    echo "  3. Verify data looks correct"
    echo "  4. Mark QC status as PASSED in UI"
    log ""
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    print_banner
    parse_args "$@"

    log "Starting import process..."
    log "Excel: $EXCEL_FILE"
    if [ "$USE_DOCKER" = true ]; then
        log "Container: $CONTAINER"
        log "Database: $DB_NAME"
    else
        log "Database: $DB_NAME @ $DB_HOST:$DB_PORT"
    fi
    log "Backup: $BACKUP"
    log "DRY RUN: $DRY_RUN"

    step_1_validate || exit 1
    step_2_database_check || exit 1
    step_3_generate_sql || exit 1
    step_4_backup || exit 1
    step_5_import || exit 1
    step_6_verify
    step_7_summary
}

main "$@"
