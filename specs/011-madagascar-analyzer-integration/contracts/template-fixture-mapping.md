# Template-to-Fixture Mapping Contract

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Purpose:** Documents the 1:1 relationship between mock server templates and
DBUnit fixtures

## Overview

This document maps each analyzer mock server template (JSON files in
`tools/analyzer-mock-server/templates/`) to its corresponding database fixture
in `madagascar-analyzer-test-data.xml`. This ensures consistency between test
simulation and database setup.

---

## Complete Mapping Table

| Analyzer             | Mock Template File          | Analyzer ID | Serial Config ID                   | File Config ID | Protocol      | Transport               |
| -------------------- | --------------------------- | ----------- | ---------------------------------- | -------------- | ------------- | ----------------------- |
| Abbott Architect     | `abbott_architect_hl7.json` | `2000`      | -                                  | -              | HL7 v2.5.1    | TCP/IP                  |
| Cepheid GeneXpert    | `genexpert.json`            | `2002`      | -                                  | `FILE-2002`    | FILE          | Filesystem              |
| Hain FluoroCycler XT | `hain_fluorocycler.json`    | `2003`      | -                                  | `FILE-2003`    | FILE          | Filesystem              |
| Horiba ABX Micros 60 | `horiba_micros60.json`      | `2004`      | `SERIAL-2004`                      | -              | ASTM LIS2-A2  | RS232 Serial            |
| Horiba ABX Pentra 60 | `horiba_pentra60.json`      | `2005`      | `SERIAL-2005`                      | -              | ASTM LIS2-A2  | RS232 Serial            |
| Mindray BA-88A       | `mindray_ba88a.json`        | `2006`      | `SERIAL-2006`                      | -              | ASTM          | RS232 Serial            |
| Mindray BC-5380      | `mindray_bc5380.json`       | `2007`      | -                                  | -              | HL7 v2.3.1    | TCP/IP                  |
| Mindray BS-360E      | `mindray_bs360e.json`       | `2008`      | -                                  | -              | HL7 v2.3.1    | TCP/IP                  |
| QuantStudio 7 Flex   | `quantstudio7.json`         | `2009`      | -                                  | `FILE-2009`    | FILE          | Filesystem              |
| Stago STart 4        | `stago_start4.json`         | `2010`      | `SERIAL-2010`                      | -              | ASTM LIS2-A2  | RS232 Serial            |
| Sysmex XN Series     | `sysmex_xn.json`            | `2011`      | `SERIAL-2011` (optional, inactive) | -              | ASTM E1381-02 | TCP/IP (RS232 optional) |

**Note:** Analyzer ID `2001` is reserved but not assigned in this version.
BC2000 (P1-H, validated via GenericHL7 M19) has separate fixture ID 2012.

---

## Template Details

### 1. Abbott Architect (ID: 2000)

**Template:** `abbott_architect_hl7.json`

**Fixture Details:**

- **Analyzer ID:** `2000`
- **Name:** Abbott Architect
- **Type:** IMMUNOLOGY
- **Protocol:** HL7 v2.5.1 over TCP/IP
- **Serial Config:** N/A
- **File Config:** N/A

**Template Identification:**

- MSH-3 (Sending Application): `ARCHITECT` or `ABBOTT`

**Testing Notes:**

- Use HL7 ORU^R01 message format
- MLLP framing: 0x0B start, 0x1C+0x0D end
- May require AlinIQ middleware in production

---

### 2. Cepheid GeneXpert (ID: 2002)

**Template:** `genexpert.json`

**Fixture Details:**

- **Analyzer ID:** `2002`
- **Name:** Cepheid GeneXpert
- **Type:** MOLECULAR
- **Protocol:** FILE-based import (CSV)
- **Serial Config:** N/A
- **File Config:** `FILE-2002`
  - Import directory: `/data/analyzer-imports/genexpert`
  - File pattern: `*.csv`

**Testing Notes:**

- FILE mode prioritized for testing
- ASTM/HL7 variants exist but not in primary test path

---

### 3. Hain FluoroCycler XT (ID: 2003)

**Template:** `hain_fluorocycler.json`

**Fixture Details:**

- **Analyzer ID:** `2003`
- **Name:** Hain FluoroCycler XT
- **Type:** MOLECULAR
- **Protocol:** FILE-based import (CSV)
- **Serial Config:** N/A
- **File Config:** `FILE-2003`
  - Import directory: `/data/analyzer-imports/fluorocycler`
  - File pattern: `*.csv`

---

### 4. Horiba ABX Micros 60 (ID: 2004)

**Template:** `horiba_micros60.json`

**Fixture Details:**

- **Analyzer ID:** `2004`
- **Name:** Horiba ABX Micros 60
- **Type:** HEMATOLOGY
- **Protocol:** ASTM LIS2-A2 over RS232 Serial
- **Serial Config:** `SERIAL-2004`
  - Port: `/dev/ttyUSB0`
  - Baud rate: 9600
  - Data bits: 8, Parity: NONE, Stop bits: 1
  - Flow control: NONE
- **File Config:** N/A

**Template Fields:** 16 (3-part differential)

**Testing Notes:**

- Requires ASTM-HTTP Bridge with RS232 support
- Requires USB-to-serial adapter

---

### 5. Horiba ABX Pentra 60 (ID: 2005)

**Template:** `horiba_pentra60.json`

**Fixture Details:**

- **Analyzer ID:** `2005`
- **Name:** Horiba ABX Pentra 60 C+
- **Type:** HEMATOLOGY
- **Protocol:** ASTM LIS2-A2 over RS232 Serial
- **Serial Config:** `SERIAL-2005`
  - Port: `/dev/ttyUSB1`
  - Baud rate: 9600
  - Data bits: 8, Parity: NONE, Stop bits: 1
  - Flow control: NONE
- **File Config:** N/A

**Template Fields:** 20 (5-part differential)

**Testing Notes:**

- Requires ASTM-HTTP Bridge with RS232 support
- Requires USB-to-serial adapter

---

### 6. Mindray BA-88A (ID: 2006)

**Template:** `mindray_ba88a.json`

**Fixture Details:**

- **Analyzer ID:** `2006`
- **Name:** Mindray BA-88A
- **Type:** CHEMISTRY
- **Protocol:** ASTM over RS232 Serial
- **Serial Config:** `SERIAL-2006`
  - Port: `/dev/ttyUSB2`
  - Baud rate: 9600
  - Data bits: 8, Parity: NONE, Stop bits: 1
  - Flow control: NONE
- **File Config:** N/A

**Testing Notes:**

- Uses existing Mindray plugin
- Requires ASTM-HTTP Bridge with RS232 support

---

### 7. Mindray BC-5380 (ID: 2007)

**Template:** `mindray_bc5380.json`

**Fixture Details:**

- **Analyzer ID:** `2007`
- **Name:** Mindray BC-5380
- **Type:** HEMATOLOGY
- **Protocol:** HL7 v2.3.1 over TCP/IP
- **Serial Config:** N/A
- **File Config:** N/A

**Template Identification:**

- MSH-3 (Sending Application): `MINDRAY`

**Testing Notes:**

- MLLP framing: 0x0B start, 0x1C+0x0D end
- Uses existing Mindray plugin
- M5 validation milestone complete

---

### 8. Mindray BS-360E (ID: 2008)

**Template:** `mindray_bs360e.json`

**Fixture Details:**

- **Analyzer ID:** `2008`
- **Name:** Mindray BS-360E
- **Type:** CHEMISTRY
- **Protocol:** HL7 v2.3.1 over TCP/IP
- **Serial Config:** N/A
- **File Config:** N/A

**Template Identification:**

- MSH-3 (Sending Application): `MINDRAY`

**Testing Notes:**

- MLLP framing: 0x0B start, 0x1C+0x0D end
- Uses existing Mindray plugin
- M5 validation milestone complete

---

### 9. QuantStudio 7 Flex (ID: 2009)

**Template:** `quantstudio7.json`

**Fixture Details:**

- **Analyzer ID:** `2009`
- **Name:** Thermo Fisher QuantStudio 7 Flex
- **Type:** MOLECULAR
- **Protocol:** FILE-based import (CSV)
- **Serial Config:** N/A
- **File Config:** `FILE-2009`
  - Import directory: `/data/analyzer-imports/quantstudio`
  - File pattern: `*.csv`

**Testing Notes:**

- Adapted from QuantStudio3 plugin

---

### 10. Stago STart 4 (ID: 2010)

**Template:** `stago_start4.json`

**Fixture Details:**

- **Analyzer ID:** `2010`
- **Name:** Stago STart 4
- **Type:** COAGULATION
- **Protocol:** ASTM LIS2-A2 over RS232 Serial
- **Serial Config:** `SERIAL-2010`
  - Port: `/dev/ttyUSB3`
  - Baud rate: 9600
  - Data bits: 8, Parity: NONE, Stop bits: 1
  - Flow control: NONE
- **File Config:** N/A

**Template Fields:** 5 (coagulation panel)

**Testing Notes:**

- Supports both ASTM and HL7 per vendor spec
- RS232/ASTM mode prioritized
- M11 milestone complete

---

### 11. Sysmex XN Series (ID: 2011)

**Template:** `sysmex_xn.json`

**Fixture Details:**

- **Analyzer ID:** `2011`
- **Name:** Sysmex XN Series
- **Type:** HEMATOLOGY
- **Protocol:** ASTM E1381-02 over TCP/IP (or RS232)
- **Serial Config:** `SERIAL-2011` (optional, inactive by default)
  - Port: `/dev/ttyUSB4`
  - Baud rate: 19200
  - Data bits: 8, Parity: NONE, Stop bits: 1
  - Flow control: NONE
- **File Config:** N/A

**Testing Notes:**

- Supports both TCP/IP and RS232 per vendor spec
- Uses existing SysmexXN-L plugin
- P2 priority

---

## Usage Examples

### Loading Fixtures

```bash
# Load Feature 011 fixtures only
./src/test/resources/load-analyzer-test-data.sh --dataset-011

# Load both Feature 004 and 011 fixtures
./src/test/resources/load-analyzer-test-data.sh --all
```

### Triggering Mock Server

```bash
# HL7 analyzer (Abbott Architect)
python server.py --hl7 --push https://localhost:8443 --hl7-template abbott_architect_hl7

# RS232 analyzer (Horiba Pentra 60) via virtual serial
python server.py --serial-port /dev/pts/X --serial-analyzer horiba_pentra60

# FILE analyzer (GeneXpert)
python server.py --generate-files /tmp/import --generate-files-analyzer genexpert
```

### Cypress Test Usage

```javascript
// Load Madagascar fixtures before test
before(() => {
  cy.loadMadagascarAnalyzerFixtures();
});

// Verify analyzer appears in dashboard
cy.get('[data-testid="analyzer-2007"]').should("contain", "Mindray BC-5380");
```

---

## Verification Queries

### Check Loaded Analyzers

```sql
SELECT id, name, analyzer_type, description
FROM analyzer
WHERE id BETWEEN 2000 AND 2011
ORDER BY id;
```

### Check Serial Configurations

```sql
SELECT id, analyzer_id, port_name, baud_rate, active
FROM serial_port_configuration
WHERE id LIKE 'SERIAL-20%'
ORDER BY id;
```

### Check File Configurations

```sql
SELECT id, analyzer_id, import_directory, file_pattern, active
FROM file_import_configuration
WHERE id LIKE 'FILE-20%'
ORDER BY id;
```

---

## Related Documentation

- **Supported Analyzers Contract:** `supported-analyzers.md`
- **Mock Server Templates:** `tools/analyzer-mock-server/templates/`
- **Template Schema:** `tools/analyzer-mock-server/templates/schema.json`
- **Template README:** `tools/analyzer-mock-server/templates/README.md`

---

**Version History:**

| Version | Date       | Changes                                           |
| ------- | ---------- | ------------------------------------------------- |
| 1.0.0   | 2026-02-02 | Initial template-fixture mapping for 12 analyzers |

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Source Repository:** `DIGI-UW/OpenELIS-Global-2`
