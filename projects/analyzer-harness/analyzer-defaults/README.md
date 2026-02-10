# Analyzer Default Configuration Templates

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Purpose:** Loadable default configurations for GenericASTM and GenericHL7
plugins

---

## Overview

This directory contains pre-configured JSON templates for common analyzers that
use the **Generic-First Architecture**. These templates enable quick analyzer
setup without writing Java code.

**Generic-First Approach**: New analyzer integrations should use GenericASTM or
GenericHL7 plugins with loadable default configurations. Vendor-specific plugins
remain for backward compatibility.

---

## Directory Structure

```
analyzer-defaults/
├── astm/           # ASTM protocol default configs
│   ├── mindray-ba88a.json
│   ├── horiba-pentra60.json
│   ├── horiba-micros60.json
│   ├── stago-start4.json
│   ├── sysmex-xn.json
│   └── genexpert-astm.json
├── hl7/            # HL7 protocol default configs
│   ├── mindray-bc2000.json
│   ├── mindray-bc5380.json
│   ├── mindray-bs360e.json
│   ├── abbott-architect.json
│   └── genexpert-hl7.json
└── README.md
```

---

## JSON Schema

Each default config JSON file follows this schema:

```json
{
  "schema_version": "1.0",
  "analyzer_name": "Mindray BC-5380",
  "manufacturer": "Mindray",
  "category": "HEMATOLOGY",
  "protocol": "HL7",
  "protocol_version": "2.3.1",
  "identifier_pattern": "MINDRAY.*BC.?5380",
  "transport": "TCP/IP",
  "default_port": 5380,
  "test_mappings": [
    {
      "analyzer_code": "WBC",
      "test_name": "White Blood Cells",
      "loinc": "6690-2",
      "unit": "10^3/uL"
    }
  ],
  "notes": "Requires MLLP framing (0x0B start, 0x1C 0x0D end)"
}
```

### Required Fields

| Field                | Type   | Description                                 |
| -------------------- | ------ | ------------------------------------------- |
| `schema_version`     | String | Config schema version (currently "1.0")     |
| `analyzer_name`      | String | Full analyzer name                          |
| `manufacturer`       | String | Manufacturer name                           |
| `category`           | String | HEMATOLOGY, CHEMISTRY, MOLECULAR, etc.      |
| `protocol`           | String | ASTM or HL7                                 |
| `protocol_version`   | String | Protocol version (e.g., "LIS2-A2", "2.3.1") |
| `identifier_pattern` | String | Regex pattern for analyzer identification   |
| `transport`          | String | RS-232 Serial or TCP/IP                     |
| `test_mappings`      | Array  | Default test code mappings                  |

### Optional Fields

| Field                   | Type    | Description                            | Protocol |
| ----------------------- | ------- | -------------------------------------- | -------- |
| `default_port`          | Number  | Default TCP port                       | HL7      |
| `default_baud_rate`     | Number  | Default serial baud rate               | ASTM     |
| `msh3_pattern`          | String  | MSH-3 sending application pattern      | HL7      |
| `notes`                 | String  | Additional configuration notes         | Both     |
| `verification_required` | Boolean | Requires field verification before use | Both     |

---

## Usage

### Dashboard Integration (Future - M20)

1. Navigate to **Admin > Analyzer Management > Add Analyzer**
2. Select protocol: **GenericASTM** or **GenericHL7**
3. Click **Load Default Config** dropdown
4. Select analyzer from list (e.g., "Mindray BC2000")
5. Dashboard auto-populates:
   - Identifier pattern
   - Protocol version
   - Default test mappings
6. Customize as needed before saving

### REST API (Future - M20)

```bash
# List available defaults
GET /rest/analyzer/defaults
# Returns: ["astm/mindray-ba88a", "hl7/mindray-bc2000", ...]

# Load specific default
GET /rest/analyzer/defaults/hl7/mindray-bc2000
# Returns: JSON config object
```

---

## ASTM Default Configs

### Mindray BA-88A (Chemistry)

**File:** `astm/mindray-ba88a.json`

**Status:** ⚠️ **CRITICAL VERIFICATION REQUIRED**

BA-88A is a 2008-era semi-automatic analyzer with limited LIS capabilities
documented publicly. Original contract may have confused this model with
BS-series (BS-120, BS-360E) which have full LIS support.

**Action Required:** Deployment team must confirm actual model deployed and
available LIS protocols before configuring.

**Test Mappings:** GLU, CREA, ALT, AST, ALB

---

## HL7 Default Configs

### Mindray BC2000 (Hematology)

**File:** `hl7/mindray-bc2000.json`

**Protocol:** HL7 v2.3.1 over TCP/IP

**Transport:** TCP/IP with MLLP framing (0x0B start, 0x1C+0x0D end)

**MSH-3:** MINDRAY

**Test Mappings:** WBC, RBC, HGB, HCT, PLT

**Note:** Protocol identical to BC-5380. Existing Mindray plugin also works as
legacy fallback.

---

## Related Documentation

- [GenericASTM Plugin README](../../../plugins/analyzers/GenericASTM/README.md)
- [GenericHL7 Architecture](../../../plugins/analyzers/GenericHL7/ARCHITECTURE.md)
  (M19 - implementation pending)
- [Feature 011 Spec](../../../specs/011-madagascar-analyzer-integration/spec.md)
- [Supported Analyzers Contract](../../../specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md)

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Repository:** `DIGI-UW/OpenELIS-Global-2`
