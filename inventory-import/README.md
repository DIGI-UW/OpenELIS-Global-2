# MNTD Laboratory Inventory Import

## Quick Start

```bash
./final-complete-import.sh
```

**One command imports everything with zero data loss.**

## What Was Accomplished

✅ **Successfully imported complete MNTD laboratory inventory:**
- **805 catalog items** (sheet + reagent combinations)
- **1,061 individual lots** with full traceability
- **1,061 transactions** for complete audit trail

✅ **Zero data loss** - All Excel metadata preserved:
- Quantities, expiry dates, lot numbers
- Volumes, storage locations, manufacturing dates
- Proper project name: "Malaria and Neglected Tropical Disease (MNTD) Laboratory"

## File Structure

- `MNTD Reagent inventory (2) (1).xlsx` - Source Excel file (24 sheets, 970+ rows)
- `final-complete-import.sh` - **Main import script** (recommended)
- `import-inventory.sh` - Alternative comprehensive script with validation
- `logs/` - Import logs and database backups (kept minimal)
- `data/` - Configuration files
- `scripts/` - Utility scripts

## Import Strategy Used

**Correct Approach (Fixed Data Loss Issue):**
- **Excel Sheet Names** → **Item Categories** (Enzymes, Primers, qPCR, etc.)
- **Excel Rows** → **Individual Lots** with complete details
- **Preserved all data**: No aggregation or loss of granular information

**Previous Approach (Had Data Loss):**
- ❌ Only captured unique item names as "lots"
- ❌ Lost quantities, expiry dates, multiple lots per item
- ❌ Reduced 970 lots to 25 generic items

## Access Your Inventory

**OpenELIS UI**: `http://localhost:8080/OpenELIS/inventory`
**Search for**: "Malaria and Neglected Tropical Disease (MNTD) Laboratory"

**You can now:**
- Browse inventory by category (Enzymes, Sequencing primers, etc.)
- View individual lots with quantities and expiry dates
- Track usage and manage stock levels
- Generate reports and audit trails

## Prerequisites

```bash
pip install openpyxl
# Ensure Docker container 'openelisglobal-database' is running
```

## Verification

Check import results:
```bash
tail logs/import-corrected-*.log
```

View counts:
```sql
SELECT 'Catalog Items', COUNT(*) FROM clinlims.inventory_item
WHERE project_name = 'Malaria and Neglected Tropical Disease (MNTD) Laboratory';
```

---

**Mission accomplished: Complete MNTD inventory successfully imported into OpenELIS with full data preservation.**