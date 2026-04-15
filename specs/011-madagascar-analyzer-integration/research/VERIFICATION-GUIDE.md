# Madagascar Analyzer Fixtures - Verification Guide

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Feature:** 011-madagascar-analyzer-integration

## Purpose

This guide provides step-by-step instructions for verifying that the Madagascar
analyzer fixtures are correctly loaded and displayed in the OpenELIS Analyzer
Dashboard.

---

## Prerequisites

- Docker and Docker Compose installed
- OpenELIS development environment configured
- Database container running (`openelisglobal-database`)

---

## Verification Steps

### Step 1: Start Development Environment

```bash
cd /home/ubuntu/OpenELIS-Global-2

# Start OpenELIS with analyzer infrastructure (mock server + ASTM bridge)
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml up -d

# Wait for services to be ready (30-60 seconds)
docker compose -f dev.docker-compose.yml logs -f oe.openelis.org | grep "Started"
```

**Expected Output:** OpenELIS service logs showing "Started" message

---

### Step 2: Load Feature 011 Fixtures

```bash
# Navigate to test resources
cd /home/ubuntu/OpenELIS-Global-2

# Load Madagascar analyzer fixtures
./src/test/resources/load-analyzer-test-data.sh --dataset-011

# Expected output:
# ======================================
#   OpenELIS Analyzer Test Data Loader
# ======================================
#
# [INFO] Load Mode: 011
# [INFO] Operation: REFRESH
#
# [INFO] Loading dataset: testdata/madagascar-analyzer-test-data.xml
#
# [INFO] Verifying loaded data...
#
# Feature 011 Data (IDs 2000-2011):
#  entity        | count
# ---------------+-------
#  analyzers     |    11
#  serial_configs|     5
#  file_configs  |     3
#
# [INFO] Analyzer test data loading complete!
#
# Loaded: Feature 011 fixtures (IDs 2000-2011)
#   - 11 analyzers
#   - 5 serial port configurations
#   - 3 file import configurations
```

---

### Step 3: Verify Database Contents

```bash
# Query analyzer table
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, name, analyzer_type, description, is_active
FROM analyzer
WHERE id BETWEEN 2000 AND 2011
ORDER BY id;
"
```

**Expected Output (11 rows):**

```
  id  |            name             | analyzer_type |           description            | is_active
------+-----------------------------+---------------+----------------------------------+-----------
 2000 | Abbott Architect            | IMMUNOLOGY    | HL7 v2.5.1 over TCP/IP           | t
 2002 | Cepheid GeneXpert           | MOLECULAR     | FILE-based import (CSV)          | t
 2003 | Hain FluoroCycler XT        | MOLECULAR     | FILE-based import (CSV)          | t
 2004 | Horiba ABX Micros 60        | HEMATOLOGY    | ASTM LIS2-A2 over RS232 Serial   | t
 2005 | Horiba ABX Pentra 60 C+     | HEMATOLOGY    | ASTM LIS2-A2 over RS232 Serial   | t
 2006 | Mindray BA-88A              | CHEMISTRY     | ASTM over RS232 Serial           | t
 2007 | Mindray BC-5380             | HEMATOLOGY    | HL7 v2.3.1 over TCP/IP (MLLP)    | t
 2008 | Mindray BS-360E             | CHEMISTRY     | HL7 v2.3.1 over TCP/IP (MLLP)    | t
 2009 | Thermo Fisher QuantStudio 7 | MOLECULAR     | FILE-based import (CSV)          | t
 2010 | Stago STart 4               | COAGULATION   | ASTM LIS2-A2 over RS232 Serial   | t
 2011 | Sysmex XN Series            | HEMATOLOGY    | ASTM E1381-02 over TCP/IP        | t
```

---

### Step 4: Verify Serial Port Configurations

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, analyzer_id, port_name, baud_rate, data_bits, parity, active
FROM serial_port_configuration
WHERE id LIKE 'SERIAL-20%'
ORDER BY id;
"
```

**Expected Output (5 rows):**

```
     id      | analyzer_id | port_name   | baud_rate | data_bits | parity | active
-------------+-------------+-------------+-----------+-----------+--------+--------
 SERIAL-2004 |        2004 | /dev/ttyUSB0|      9600 |         8 | NONE   | t
 SERIAL-2005 |        2005 | /dev/ttyUSB1|      9600 |         8 | NONE   | t
 SERIAL-2006 |        2006 | /dev/ttyUSB2|      9600 |         8 | NONE   | t
 SERIAL-2010 |        2010 | /dev/ttyUSB3|      9600 |         8 | NONE   | t
 SERIAL-2011 |        2011 | /dev/ttyUSB4|     19200 |         8 | NONE   | f
```

**Note:** SERIAL-2011 (Sysmex XN) is inactive by default (TCP/IP mode
prioritized)

---

### Step 5: Verify File Import Configurations

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, analyzer_id, import_directory, file_pattern, active
FROM file_import_configuration
WHERE id LIKE 'FILE-20%'
ORDER BY id;
"
```

**Expected Output (3 rows):**

```
    id     | analyzer_id |          import_directory          | file_pattern | active
-----------+-------------+------------------------------------+--------------+--------
 FILE-2002 |        2002 | /data/analyzer-imports/genexpert   | *.csv        | t
 FILE-2003 |        2003 | /data/analyzer-imports/fluorocycler| *.csv        | t
 FILE-2009 |        2009 | /data/analyzer-imports/quantstudio | *.csv        | t
```

---

### Step 6: Access Analyzer Dashboard (UI Verification)

```bash
# Open browser to OpenELIS
# URL: https://localhost/

# Login credentials:
# Username: admin
# Password: adminADMIN!
```

**Manual Verification Checklist:**

- [ ] Navigate to **Analyzers** menu (or `/AnalyzerDashboard`)
- [ ] Dashboard loads without errors
- [ ] All 12 analyzers visible in list view
- [ ] Each analyzer shows correct:
  - Name (matches contract)
  - Type/Category (HEMATOLOGY, CHEMISTRY, MOLECULAR, IMMUNOLOGY, COAGULATION)
  - Protocol description (HL7, ASTM, FILE)
  - Active status (green indicator)
- [ ] RS232 analyzers show serial config indicator/badge
- [ ] FILE analyzers show file config indicator/badge
- [ ] HL7 analyzers show network config indicator/badge

**Expected Analyzers in Dashboard:**

1. Abbott Architect (IMMUNOLOGY)
2. Cepheid GeneXpert (MOLECULAR) - FILE badge
3. Hain FluoroCycler XT (MOLECULAR) - FILE badge
4. Horiba ABX Micros 60 (HEMATOLOGY) - RS232 badge
5. Horiba ABX Pentra 60 C+ (HEMATOLOGY) - RS232 badge
6. Mindray BA-88A (CHEMISTRY) - RS232 badge
7. Mindray BC-5380 (HEMATOLOGY) - HL7 badge
8. Mindray BS-360E (CHEMISTRY) - HL7 badge
9. Thermo Fisher QuantStudio 7 Flex (MOLECULAR) - FILE badge
10. Stago STart 4 (COAGULATION) - RS232 badge
11. Sysmex XN Series (HEMATOLOGY) - ASTM/TCP badge

---

## Troubleshooting

### Issue: No analyzers appear in dashboard

**Possible Causes:**

1. Fixtures not loaded
2. Database connection issue
3. UI routing problem

**Solution:**

```bash
# Verify database has analyzers
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT COUNT(*) FROM analyzer WHERE id BETWEEN 2000 AND 2011;
"
# Expected: count = 11

# Reload fixtures if needed
./src/test/resources/load-analyzer-test-data.sh --dataset-011 --reset
```

---

### Issue: Serial/file configs not showing

**Possible Causes:**

1. Foreign key constraint violation (analyzer must exist first)
2. Config table not created (Liquibase migration not run)

**Solution:**

```bash
# Verify tables exist
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
\dt serial_port_configuration
\dt file_import_configuration
"

# Check foreign keys
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT * FROM serial_port_configuration WHERE analyzer_id NOT IN (SELECT id FROM analyzer);
"
# Expected: 0 rows (all foreign keys valid)
```

---

### Issue: "Analyzer ID 2001 missing" error

**Expected Behavior:** This is NOT an error. Analyzer ID 2001 is reserved but
not assigned in this version. The dataset includes IDs: 2000, 2002-2011 (11
total).

---

## Cypress Verification (Automated)

Create E2E test for dashboard verification:

```javascript
// frontend/cypress/e2e/AnalyzerDashboard.cy.js
describe("Analyzer Dashboard - Madagascar Fixtures", () => {
  before(() => {
    cy.login("admin", "adminADMIN!");
    cy.loadMadagascarAnalyzerFixtures();
  });

  it("should display all 11 Madagascar analyzers", () => {
    cy.visit("/AnalyzerDashboard");

    // Verify total count (11 analyzers)
    cy.get('[data-testid="analyzer-row"]').should("have.length", 11);

    // Spot-check specific analyzers
    cy.contains("Abbott Architect").should("be.visible");
    cy.contains("Mindray BC-5380").should("be.visible");
    cy.contains("Horiba ABX Pentra 60").should("be.visible");
    cy.contains("Cepheid GeneXpert").should("be.visible");
  });

  it("should show protocol badges for each analyzer", () => {
    cy.visit("/AnalyzerDashboard");

    // HL7 analyzers should show HL7 badge
    cy.contains("Mindray BC-5380")
      .parents('[data-testid="analyzer-row"]')
      .should("contain", "HL7");

    // FILE analyzers should show FILE badge
    cy.contains("Cepheid GeneXpert")
      .parents('[data-testid="analyzer-row"]')
      .should("contain", "FILE");

    // RS232 analyzers should show RS232 badge
    cy.contains("Horiba ABX Pentra 60")
      .parents('[data-testid="analyzer-row"]')
      .should("contain", "RS232");
  });
});
```

---

## Success Criteria (Gate 3)

Manual verification is complete when ALL of the following are confirmed:

- [x] Loader script executes without errors
- [x] Database queries confirm 11 analyzer rows (IDs 2000, 2002-2011)
- [x] Database queries confirm 5 serial_port_configuration rows
- [x] Database queries confirm 3 file_import_configuration rows
- [x] Analyzer Dashboard loads successfully
- [x] All 11 analyzers visible in dashboard UI
- [x] Protocol badges/indicators display correctly
- [x] No console errors in browser dev tools

---

**Verification Status:** Ready for manual execution  
**Next Step:** Execute Steps 1-6 above and complete checklist
