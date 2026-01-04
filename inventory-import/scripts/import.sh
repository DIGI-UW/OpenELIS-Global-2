#!/bin/bash

################################################################################
# MNTD Reagent Inventory Import Script
#
# Executes the complete inventory import process:
# 1. Validates database connection
# 2. Creates storage locations (if needed)
# 3. Imports inventory items
# 4. Imports lots and audit transactions
# 5. Verifies data integrity
#
# Usage:
#   bash import.sh --database openelis --user postgres --host localhost
#
# Exit codes:
#   0 = Success
#   1 = Validation failed
#   2 = Database error
#   3 = File not found
################################################################################

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMPORT_DIR="$(dirname "$SCRIPT_DIR")"
SQL_DIR="$IMPORT_DIR/sql"
LOGS_DIR="$IMPORT_DIR/logs"

# Defaults
DB_NAME="openelis"
DB_USER="postgres"
DB_HOST="localhost"
DB_PORT="5432"
BACKUP=true
DRY_RUN=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging
mkdir -p "$LOGS_DIR"
LOG_FILE="$LOGS_DIR/import-$(date +%Y%m%d_%H%M%S).log"

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}✗ ERROR: $1${NC}" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}⚠ $1${NC}" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${BLUE}ℹ $1${NC}" | tee -a "$LOG_FILE"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --database)
            DB_NAME="$2"
            shift 2
            ;;
        --user)
            DB_USER="$2"
            shift 2
            ;;
        --host)
            DB_HOST="$2"
            shift 2
            ;;
        --port)
            DB_PORT="$2"
            shift 2
            ;;
        --no-backup)
            BACKUP=false
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

###############################################################################
# Functions
###############################################################################

print_banner() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║   MNTD Reagent Inventory - Database Import                    ║"
    echo "║   Database: $DB_NAME @ $DB_HOST:$DB_PORT"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""
}

validate_prerequisites() {
    log "Validating prerequisites..."

    # Check psql installed
    if ! command -v psql &> /dev/null; then
        log_error "psql is not installed. Please install PostgreSQL client."
        return 1
    fi
    log_success "psql found"

    # Check Python installed
    if ! command -v python3 &> /dev/null; then
        log_error "python3 is not installed. Please install Python 3."
        return 1
    fi
    log_success "python3 found"

    # Check openpyxl
    if ! python3 -c "import openpyxl" 2>/dev/null; then
        log_error "openpyxl module not installed. Run: pip install openpyxl"
        return 1
    fi
    log_success "openpyxl module found"

    # Check SQL directory
    if [ ! -d "$SQL_DIR" ]; then
        mkdir -p "$SQL_DIR"
        log_success "Created SQL directory: $SQL_DIR"
    fi

    return 0
}

test_database_connection() {
    log "Testing database connection to $DB_NAME..."

    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
            -c "SELECT 1" > /dev/null 2>&1; then
        log_success "Database connection successful"
        return 0
    else
        log_error "Cannot connect to database. Check credentials and PostgreSQL is running."
        return 2
    fi
}

check_inventory_tables() {
    log "Checking if inventory tables exist..."

    local tables_exist=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('inventory_item', 'inventory_lot') AND table_schema = 'public'")

    if [ "$tables_exist" -eq 2 ]; then
        log_success "Inventory tables found"
        return 0
    else
        log_error "Inventory tables not found. Please check database schema."
        return 2
    fi
}

backup_database() {
    if [ "$BACKUP" = false ]; then
        log_warning "Skipping database backup (--no-backup)"
        return 0
    fi

    local backup_file="$LOGS_DIR/backup-$DB_NAME-$(date +%Y%m%d_%H%M%S).sql"

    log "Creating database backup: $backup_file"

    if pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > "$backup_file" 2>&1; then
        log_success "Backup created: $backup_file"
        log_info "Size: $(du -h "$backup_file" | cut -f1)"
        return 0
    else
        log_error "Backup failed. Aborting import."
        return 2
    fi
}

generate_sql_from_excel() {
    log "Generating SQL from Excel file..."

    local excel_file="$IMPORT_DIR/MNTD Reagent inventory (2) (1).xlsx"

    if [ ! -f "$excel_file" ]; then
        log_error "Excel file not found: $excel_file"
        return 3
    fi

    log_info "Excel file: $excel_file"

    if python3 "$SCRIPT_DIR/excel_to_sql.py" \
            --input "$excel_file" \
            --output "$SQL_DIR" >> "$LOG_FILE" 2>&1; then
        log_success "SQL generation complete"
        return 0
    else
        log_error "SQL generation failed. Check logs."
        return 1
    fi
}

execute_sql_file() {
    local sql_file="$1"
    local step_name="$2"

    if [ ! -f "$sql_file" ]; then
        log_error "SQL file not found: $sql_file"
        return 3
    fi

    log "Executing $step_name: $(basename "$sql_file")"

    if [ "$DRY_RUN" = true ]; then
        log_warning "DRY RUN - Not executing SQL"
        log_info "File: $sql_file"
        return 0
    fi

    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
            -f "$sql_file" >> "$LOG_FILE" 2>&1; then
        log_success "$step_name completed"
        return 0
    else
        log_error "$step_name failed. Check logs: $LOG_FILE"
        return 2
    fi
}

create_storage_locations() {
    log "Creating default storage locations..."

    local sql_file="$SQL_DIR/01-create-storage-locations.sql"

    if [ -f "$sql_file" ]; then
        execute_sql_file "$sql_file" "Storage locations setup"
        return $?
    else
        log_warning "Storage locations SQL file not found. Skipping."
        return 0
    fi
}

import_items() {
    log "Phase 1: Importing inventory items..."

    local sql_file="$SQL_DIR/02-insert-inventory-items.sql"

    if [ ! -f "$sql_file" ]; then
        log_error "Items SQL file not found. Run excel_to_sql.py first."
        return 3
    fi

    execute_sql_file "$sql_file" "Inventory items import"
    return $?
}

import_lots() {
    log "Phase 2: Importing inventory lots and transactions..."

    local sql_file="$SQL_DIR/03-insert-inventory-lots.sql"

    if [ ! -f "$sql_file" ]; then
        log_error "Lots SQL file not found. Run excel_to_sql.py first."
        return 3
    fi

    execute_sql_file "$sql_file" "Inventory lots import"
    return $?
}

verify_import() {
    log "Verifying import..."

    local verify_file="$SQL_DIR/04-verify-import.sql"

    if [ -f "$verify_file" ]; then
        execute_sql_file "$verify_file" "Import verification"
    else
        log_warning "Verification SQL file not found. Skipping."
    fi

    # Quick stats
    local item_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -tAc "SELECT COUNT(*) FROM inventory_item WHERE project_name = 'MNTD Research'")

    local lot_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -tAc "SELECT COUNT(*) FROM inventory_lot WHERE project_name = 'MNTD Research'")

    log_info "Inventory items: $item_count"
    log_info "Inventory lots: $lot_count"

    return 0
}

###############################################################################
# Main Execution
###############################################################################

main() {
    print_banner

    # Validation phase
    if ! validate_prerequisites; then
        exit 1
    fi

    if ! test_database_connection; then
        exit 2
    fi

    if ! check_inventory_tables; then
        exit 2
    fi

    # Backup phase
    if ! backup_database; then
        exit 2
    fi

    # SQL generation phase
    if ! generate_sql_from_excel; then
        exit 1
    fi

    # Import phases
    if ! create_storage_locations; then
        exit 2
    fi

    if ! import_items; then
        exit 2
    fi

    if ! import_lots; then
        exit 2
    fi

    # Verification
    if ! verify_import; then
        log_warning "Verification had issues but import may have succeeded"
    fi

    # Success
    echo ""
    log_success "IMPORT COMPLETE"
    echo ""
    log_info "Logs saved to: $LOG_FILE"
    log_info "Next steps:"
    echo "  1. Review logs: tail -f $LOG_FILE"
    echo "  2. Verify in UI: http://localhost:8080/OpenELIS/inventory"
    echo "  3. Search for 'MNTD' items in Catalog"
    echo ""

    return 0
}

# Run main with error handling
if main; then
    exit 0
else
    log_error "Import failed"
    exit 1
fi
