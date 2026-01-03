# MNTD Laboratory Inventory Import Guide

## Overview

The OpenELIS system now includes sample data from the **Malaria and Neglected Tropical Disease (MNTD) Laboratory** inventory to replace generic demo data with real-world laboratory inventory examples.

## Quick Start

The Liquibase migration `025-replace-demo-inventory-with-mntd-data.xml` automatically:

1. **Removes** generic demo inventory items (DNA Extraction Kit, Microcentrifuge, etc.)
2. **Adds** sample MNTD Laboratory items representing real molecular biology inventory
3. **Demonstrates** proper categorization for research laboratory inventory

## Sample Data Included

### Reagents (REAGENT type):
- **MgCl2 10xPCR rxn buffer** - Sequencing primers category
- **Pfcrt-Fw** - P. falciparum chloroquine resistance primer
- **FastDigest AluI enzyme with buffer** - Restriction enzyme

### Equipment (CARTRIDGE type):
- **Real-Time PCR System** - Applied Biosystems QuantStudio
- **Gel Electrophoresis System** - Bio-Rad horizontal gel system

## Full MNTD Inventory Import (Optional)

For the complete 800+ item MNTD inventory from the Excel file:

```bash
cd inventory-import/
./final-complete-import.sh
```

This imports:
- **805 catalog items** from 24 Excel sheets
- **1,061 individual lots** with quantities and expiry dates
- **Complete audit trail** and traceability

## Inventory Categories

The MNTD data represents real molecular biology laboratory categories:

| Category | Item Type | Examples |
|----------|-----------|----------|
| Sequencing primers | REAGENT | MgCl2 buffer, Pfcrt primers |
| Enzymes | REAGENT | FastDigest AluI, restriction enzymes |
| PCR Equipment | CARTRIDGE | Real-time PCR systems |
| Electrophoresis | CARTRIDGE | Gel electrophoresis apparatus |
| nPCR | REAGENT | Nested PCR reagents |
| Digital PCR | REAGENT | Digital PCR consumables |
| Immunoassay | REAGENT | Immunological assays |

## Conditional Table Views

The inventory dashboard now shows different column layouts based on item type:

### Equipment View (CARTRIDGE filter)
- Equipment Name, Manufacturer, Model
- Serial Number, Software, AHRI Tag
- Installation Date, Current Location
- Equipment Condition, Service History

### Reagent View (REAGENT filter)
- Reagent Name, Catalogue Number
- Manufacturer, Category, Concentration
- Storage Temperature, Location Details
- Date Received, Project Information

## Search Functionality

The system now supports searching across:
- **Item names** (e.g., "Pfcrt", "PCR", "enzyme")
- **Project names** (e.g., "MNTD", "Malaria")
- **Lot numbers** and **categories**

## Project Association

All MNTD items are properly associated with:
**Project Name**: "Malaria and Neglected Tropical Disease (MNTD) Laboratory"

This enables project-based inventory filtering and reporting.

---

**Result**: OpenELIS inventory now demonstrates real laboratory inventory management with authentic research laboratory data instead of generic placeholders.