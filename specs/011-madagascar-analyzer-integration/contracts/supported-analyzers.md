# Supported Analyzers Inventory Contract

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Status:** Authoritative Source of Truth

## Purpose

This document defines the **authoritative inventory** of supported analyzers for
Feature 011 (Madagascar Analyzer Integration). All project documentation, mock
templates, plugin implementations, and test fixtures MUST align with this
contract.

## Research Findings & Corrections (2026-02-02)

Internet research revealed factual inconsistencies in the original contract. The
following analyzers require verification or have expanded capabilities:

| Analyzer             | Original Contract | Research Findings                                                                                                  | Status                |
| -------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------ | --------------------- |
| **Mindray BA-88A**   | ASTM/RS232        | Semi-automatic analyzer (2008), limited LIS capabilities. May have been confused with BS-series (BS-120, BS-360E). | ⚠️ NEEDS VERIFICATION |
| **Mindray BS-360E**  | HL7 v2.3.1 only   | Supports BOTH HL7 v2.3.1 AND ASTM E1381-95 per Mindray LIS Interface Manual                                        | ✅ EXPANDED           |
| **Abbott Architect** | HL7/TCP only      | Primary interface is RS-232 Serial per Abbott documentation. TCP/IP may require middleware.                        | ⚠️ NEEDS VERIFICATION |
| **Sysmex XN-L**      | ASTM + HL7        | Only ASTM over TCP/IP confirmed in vendor specs. HL7 claim unverified.                                             | ⚠️ HL7 REMOVED        |

**Contract correction summary** (deployment verification):

- **BA-88A**: Add warning "Verify LIS capability with deployment team"
  (semi-auto 2008, limited LIS).
- **BS-360E**: Document dual protocol support (HL7 v2.3.1 AND ASTM E1381-95).
- **Abbott Architect**: Add "Verify transport with deployment team" (RS-232
  primary per docs).
- **Sysmex XN-L**: Remove HL7 claim; mark as ASTM/TCP only (HL7 unverified).

**Action Required**: Deployment team must verify actual analyzer models and
available connectivity options. See VERIFICATION-CHECKLIST.md.

---

## Contract Requirements

Each supported analyzer MUST have:

1. **Mock Server Template**: JSON file in
   `tools/analyzer-mock-server/templates/`
2. **Plugin Implementation**: External JAR in `plugins/analyzers/{PluginName}/`
3. **Test Fixtures**: DB fixtures (DBUnit XML) for populating Analyzer Dashboard
4. **Test Path Documentation**: Supported testing modes (manual, integration,
   E2E)

---

## Supported Analyzers (13 Required, 36 Total Plugins)

**Madagascar Contract (Required):** 13 analyzers (IDs 2000, 2002-2012)

- 11 original P1 analyzers (IDs 2000, 2002-2011)
- QuantStudio 7 Flex added via PR #42 (merged; uses ID 2009, legacy QuantStudio3
  reassigned if needed)
- BC2000 promoted from P2 to P1 (ID 2012); validated via GenericHL7 (M19)

**All Supported Plugins:** 36 plugins in `plugins/analyzers/` directory (see
"Global Plugin Inventory" section below). Includes GenericASTM and GenericHL7
(plugin IDs 3033-3034).

**Note:** BC2000 is active with fixture ID 2012. QuantStudio 7 Flex is a
separate plugin (PR #42 merged in openelisglobal-plugins).

---

### 1. Abbott Architect (IMMUNOLOGY)

| Property           | Value                                                                |
| ------------------ | -------------------------------------------------------------------- |
| **Canonical Name** | Abbott Architect                                                     |
| **Model**          | Architect                                                            |
| **Manufacturer**   | Abbott                                                               |
| **Category**       | IMMUNOLOGY                                                           |
| **Priority**       | P1 (High)                                                            |
| **Protocol**       | HL7 v2.5.1                                                           |
| **Transport**      | **RS-232 Serial (primary)** or TCP/IP via middleware                 |
| **Template**       | `abbott_architect_hl7.json`                                          |
| **Plugin**         | `plugins/analyzers/AbbottArchitect/` (✅ Exists - M12)               |
| **Fixture ID**     | 2000                                                                 |
| **Serial Config**  | Available (verify with deployment team - baud rate, port assignment) |
| **File Config**    | N/A                                                                  |
| **Test Path**      | Manual, Integration, E2E                                             |

**Notes**: MSH-3 sending application = `ARCHITECT` or `ABBOTT`. Per Abbott
documentation, RS-232 is the primary interface. TCP/IP may require Abbott AlinIQ
middleware in production. **⚠️ VERIFY**: Confirm deployed model and available
transport options.

---

### 2. GeneXpert (MOLECULAR)

| Property           | Value                                                                                          |
| ------------------ | ---------------------------------------------------------------------------------------------- |
| **Canonical Name** | Cepheid GeneXpert                                                                              |
| **Model**          | GeneXpert                                                                                      |
| **Manufacturer**   | Cepheid                                                                                        |
| **Category**       | MOLECULAR                                                                                      |
| **Priority**       | P1 (High)                                                                                      |
| **Protocol**       | FILE (primary), ASTM, HL7                                                                      |
| **Transport**      | Filesystem, TCP/IP                                                                             |
| **Template**       | `genexpert.json`                                                                               |
| **Plugin**         | `plugins/analyzers/GeneXpertFile/`, `GeneXpert/` (ASTM), `GeneXpertHL7/` (✅ 3 variants exist) |
| **Fixture ID**     | 2002                                                                                           |
| **Serial Config**  | N/A                                                                                            |
| **File Config**    | `FILE-2002` (import directory: `/data/analyzer-imports/genexpert`, pattern: `*.csv`)           |
| **Test Path**      | Manual, Integration, E2E (FILE mode)                                                           |

**Notes**: Multi-protocol capable but FILE mode used for pragmatic testing.
ASTM/HL7 modes deferred to post-deployment.

---

### 3. Hain FluoroCycler XT (MOLECULAR)

| Property           | Value                                                                                   |
| ------------------ | --------------------------------------------------------------------------------------- |
| **Canonical Name** | Hain FluoroCycler XT                                                                    |
| **Model**          | FluoroCycler XT                                                                         |
| **Manufacturer**   | Hain Lifescience                                                                        |
| **Category**       | MOLECULAR                                                                               |
| **Priority**       | P1 (High)                                                                               |
| **Protocol**       | FILE                                                                                    |
| **Transport**      | Filesystem                                                                              |
| **Template**       | `hain_fluorocycler.json`                                                                |
| **Plugin**         | `plugins/analyzers/FluoroCyclerXT/` (✅ Exists - M13)                                   |
| **Fixture ID**     | 2003                                                                                    |
| **Serial Config**  | N/A                                                                                     |
| **File Config**    | `FILE-2003` (import directory: `/data/analyzer-imports/fluorocycler`, pattern: `*.csv`) |
| **Test Path**      | Manual, Integration, E2E                                                                |

**Notes**: CSV-based result export.

---

### 4. Horiba Micros 60 (HEMATOLOGY)

| Property           | Value                                                                                        |
| ------------------ | -------------------------------------------------------------------------------------------- |
| **Canonical Name** | Horiba ABX Micros 60                                                                         |
| **Model**          | Micros 60                                                                                    |
| **Manufacturer**   | Horiba ABX                                                                                   |
| **Category**       | HEMATOLOGY                                                                                   |
| **Priority**       | P1 (High)                                                                                    |
| **Protocol**       | ASTM LIS2-A2                                                                                 |
| **Transport**      | RS232 Serial                                                                                 |
| **Template**       | `horiba_micros60.json`                                                                       |
| **Plugin**         | `plugins/analyzers/HoribaMicros60/` (✅ Exists - M10)                                        |
| **Fixture ID**     | 2004                                                                                         |
| **Serial Config**  | `SERIAL-2004` (port: `/dev/ttyUSB0`, baud: 9600, data: 8, parity: NONE, stop: 1, flow: NONE) |
| **File Config**    | N/A                                                                                          |
| **Test Path**      | Manual, Integration (requires ASTM-HTTP Bridge with RS232 support)                           |

**Notes**: Requires USB-to-serial adapter + ASTM-HTTP Bridge for RS232→TCP
conversion. 16 fields (3-part differential).

---

### 5. Horiba Pentra 60 (HEMATOLOGY)

| Property           | Value                                                                                        |
| ------------------ | -------------------------------------------------------------------------------------------- |
| **Canonical Name** | Horiba ABX Pentra 60 C+                                                                      |
| **Model**          | Pentra 60 C+                                                                                 |
| **Manufacturer**   | Horiba ABX                                                                                   |
| **Category**       | HEMATOLOGY                                                                                   |
| **Priority**       | P1 (High)                                                                                    |
| **Protocol**       | ASTM LIS2-A2                                                                                 |
| **Transport**      | RS232 Serial                                                                                 |
| **Template**       | `horiba_pentra60.json`                                                                       |
| **Plugin**         | `plugins/analyzers/HoribaPentra60/` (✅ Exists - M9)                                         |
| **Fixture ID**     | 2005                                                                                         |
| **Serial Config**  | `SERIAL-2005` (port: `/dev/ttyUSB1`, baud: 9600, data: 8, parity: NONE, stop: 1, flow: NONE) |
| **File Config**    | N/A                                                                                          |
| **Test Path**      | Manual, Integration (requires ASTM-HTTP Bridge with RS232 support)                           |

**Notes**: Requires USB-to-serial adapter + ASTM-HTTP Bridge for RS232→TCP
conversion. 20 fields (5-part differential).

---

### 6. Mindray BA-88A (CHEMISTRY)

| Property           | Value                                                                                        |
| ------------------ | -------------------------------------------------------------------------------------------- |
| **Canonical Name** | Mindray BA-88A                                                                               |
| **Model**          | BA-88A                                                                                       |
| **Manufacturer**   | Mindray                                                                                      |
| **Category**       | CHEMISTRY                                                                                    |
| **Priority**       | P1 (High)                                                                                    |
| **Protocol**       | ASTM (assumed, needs verification)                                                           |
| **Transport**      | RS232 Serial (assumed, needs verification)                                                   |
| **Template**       | `mindray_ba88a.json`                                                                         |
| **Plugin**         | GenericASTM with default config (Mindray plugin is HL7-only)                                 |
| **Fixture ID**     | 2006                                                                                         |
| **Serial Config**  | `SERIAL-2006` (port: `/dev/ttyUSB2`, baud: 9600, data: 8, parity: NONE, stop: 1, flow: NONE) |
| **File Config**    | N/A                                                                                          |
| **Test Path**      | Manual, Integration (requires ASTM-HTTP Bridge with RS232 support)                           |

**Notes**: ⚠️ **CRITICAL VERIFICATION REQUIRED**: BA-88A is a 2008-era
semi-automatic analyzer with limited LIS capabilities documented publicly.
Original contract may have confused this model with BS-series (BS-120, BS-360E)
which have full bidirectional LIS support. **Action**: Deployment team must
confirm actual model deployed and available LIS protocols. If analyzer has
limited/no LIS, manual entry may be required. Plugin uses GenericASTM with
default configuration (existing Mindray plugin only supports HL7).

---

### 7. Mindray BC-5380 (HEMATOLOGY)

| Property           | Value                                                   |
| ------------------ | ------------------------------------------------------- |
| **Canonical Name** | Mindray BC-5380                                         |
| **Model**          | BC-5380                                                 |
| **Manufacturer**   | Mindray                                                 |
| **Category**       | HEMATOLOGY                                              |
| **Priority**       | P1 (High)                                               |
| **Protocol**       | HL7 v2.3.1                                              |
| **Transport**      | TCP/IP (MLLP framing: 0x0B start, 0x1C+0x0D end)        |
| **Template**       | `mindray_bc5380.json`                                   |
| **Plugin**         | `plugins/analyzers/Mindray/` (✅ Exists - M5 validated) |
| **Fixture ID**     | 2007                                                    |
| **Serial Config**  | N/A                                                     |
| **File Config**    | N/A                                                     |
| **Test Path**      | Manual, Integration, E2E                                |

**Notes**: HL7 ORU^R01 results. MSH-3 = `MINDRAY`.

---

### 8. Mindray BS-360E (CHEMISTRY)

| Property           | Value                                                   |
| ------------------ | ------------------------------------------------------- |
| **Canonical Name** | Mindray BS-360E                                         |
| **Model**          | BS-360E                                                 |
| **Manufacturer**   | Mindray                                                 |
| **Category**       | CHEMISTRY                                               |
| **Priority**       | P1 (High)                                               |
| **Protocol**       | **HL7 v2.3.1 OR ASTM E1381-95** (dual protocol support) |
| **Transport**      | TCP/IP (MLLP framing for HL7)                           |
| **Template**       | `mindray_bs360e.json`                                   |
| **Plugin**         | `plugins/analyzers/Mindray/` (✅ Exists - M5 validated) |
| **Fixture ID**     | 2008                                                    |
| **Serial Config**  | N/A (HL7 mode used)                                     |
| **File Config**    | N/A                                                     |
| **Test Path**      | Manual, Integration, E2E                                |

**Notes**: HL7 ORU^R01 results. MSH-3 = `MINDRAY`. ✅ **RESEARCH CORRECTION**:
Per Mindray LIS Interface Manual, BS-360E supports BOTH HL7 v2.3.1 AND ASTM
E1381-95. Current fixture uses HL7 mode. ASTM mode available if needed.

---

### 9. QuantStudio 7 Flex (MOLECULAR)

| Property           | Value                                                                                  |
| ------------------ | -------------------------------------------------------------------------------------- |
| **Canonical Name** | Thermo Fisher QuantStudio 7 Flex                                                       |
| **Model**          | QuantStudio 7 Flex                                                                     |
| **Manufacturer**   | Thermo Fisher Scientific                                                               |
| **Category**       | MOLECULAR                                                                              |
| **Priority**       | P1 (High)                                                                              |
| **Protocol**       | FILE                                                                                   |
| **Transport**      | Filesystem                                                                             |
| **Template**       | `quantstudio7.json`                                                                    |
| **Plugin**         | `plugins/analyzers/QuantStudio7Flex/` (✅ **New plugin via PR #42**)                   |
| **Fixture ID**     | 2009                                                                                   |
| **Serial Config**  | N/A                                                                                    |
| **File Config**    | `FILE-2009` (import directory: `/data/analyzer-imports/quantstudio`, pattern: `*.csv`) |
| **Test Path**      | Manual, Integration, E2E                                                               |

**Notes**: CSV export from PC software. ✅ **NEW PLUGIN (PR #42)**: Dedicated
`QuantStudio7Flex` plugin created in `openelisglobal-plugins` repo. Has
different column structure than QS3 (`Well Position`, `Target`, `Amp Status`
columns). Plugin includes unit tests and follows full plugin architecture
pattern (Analyzer, Implementation, Menu, Permission classes).

---

### 10. Stago STart 4 (COAGULATION)

| Property           | Value                                                                                        |
| ------------------ | -------------------------------------------------------------------------------------------- |
| **Canonical Name** | Stago STart 4                                                                                |
| **Model**          | STart 4                                                                                      |
| **Manufacturer**   | Stago                                                                                        |
| **Category**       | COAGULATION                                                                                  |
| **Priority**       | P1-M (Moderate)                                                                              |
| **Protocol**       | ASTM LIS2-A2                                                                                 |
| **Transport**      | RS232 Serial                                                                                 |
| **Template**       | `stago_start4.json`                                                                          |
| **Plugin**         | `plugins/analyzers/StagoSTart4/` (✅ Exists - M11)                                           |
| **Fixture ID**     | 2010                                                                                         |
| **Serial Config**  | `SERIAL-2010` (port: `/dev/ttyUSB3`, baud: 9600, data: 8, parity: NONE, stop: 1, flow: NONE) |
| **File Config**    | N/A                                                                                          |
| **Test Path**      | Manual, Integration (requires ASTM-HTTP Bridge with RS232 support)                           |

**Notes**: Supports both ASTM and HL7 (per vendor spec), but RS232/ASTM mode
prioritized. 5 coagulation fields.

---

### 11. Sysmex XN Series (HEMATOLOGY)

| Property           | Value                                                                                                                   |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| **Canonical Name** | Sysmex XN Series                                                                                                        |
| **Model**          | XN-L, XN-1000                                                                                                           |
| **Manufacturer**   | Sysmex                                                                                                                  |
| **Category**       | HEMATOLOGY                                                                                                              |
| **Priority**       | P2 (Not on Romain's list)                                                                                               |
| **Protocol**       | **ASTM E1381-02** (HL7 unverified, removed)                                                                             |
| **Transport**      | RS232 Serial or TCP/IP                                                                                                  |
| **Template**       | `sysmex_xn.json`                                                                                                        |
| **Plugin**         | `plugins/analyzers/SysmexXN-L/`, `SysmexXN1000/` (✅ Exists)                                                            |
| **Fixture ID**     | 2011                                                                                                                    |
| **Serial Config**  | `SERIAL-2011` (optional, for RS232 mode: port: `/dev/ttyUSB4`, baud: 19200, data: 8, parity: NONE, stop: 1, flow: NONE) |
| **File Config**    | N/A                                                                                                                     |
| **Test Path**      | Manual, Integration                                                                                                     |

**Notes**: Per vendor spec, supports both RS232 and TCP/IP. TCP/IP mode uses
ASTM E1394-97 presentation layer + E1381-02 data link layer. ⚠️ **RESEARCH
CORRECTION**: Only ASTM support confirmed in public documentation. Original HL7
claim could not be verified and has been removed.

---

### 12. Mindray BC2000 (HEMATOLOGY)

| Property           | Value                                                             |
| ------------------ | ----------------------------------------------------------------- |
| **Canonical Name** | Mindray BC2000                                                    |
| **Model**          | BC2000                                                            |
| **Manufacturer**   | Mindray                                                           |
| **Category**       | HEMATOLOGY                                                        |
| **Priority**       | **P1 (Promoted from P2)**                                         |
| **Protocol**       | HL7 v2.3.1                                                        |
| **Transport**      | TCP/IP (MLLP framing)                                             |
| **Template**       | Shares `mindray_bc5380.json` (similar protocol)                   |
| **Plugin**         | GenericHL7 with default config (Mindray plugin works as fallback) |
| **Fixture ID**     | **2012** (assigned 2026-02-02)                                    |
| **Serial Config**  | N/A                                                               |
| **File Config**    | N/A                                                               |
| **Test Path**      | Manual, Integration                                               |

**Notes**: Promoted to P1 priority. Uses GenericHL7 plugin with loadable default
configuration. Existing Mindray plugin also works as legacy fallback. MSH-3 =
`MINDRAY`. Protocol identical to BC-5380.

---

## Protocol Summary

| Protocol         | Analyzers (Count)                                                                          | Transport              | Framing/Acknowledgment                                          |
| ---------------- | ------------------------------------------------------------------------------------------ | ---------------------- | --------------------------------------------------------------- |
| **HL7 v2.x**     | Abbott Architect, Mindray BC-5380/BS-360E, GeneXpert (variant), BC2000 (5)                 | TCP/IP                 | MLLP (0x0B start, 0x1C+0x0D end), ACK messages                  |
| **ASTM LIS2-A2** | Horiba Micros60/Pentra60, Mindray BA-88A, Stago STart4, GeneXpert (variant), Sysmex XN (7) | RS232 Serial or TCP/IP | ENQ/ACK/NAK establishment, STX+FN+payload+ETB/ETX+checksum+CRLF |
| **FILE**         | GeneXpert (variant), Hain FluoroCycler XT, QuantStudio 7 (3)                               | Filesystem             | N/A (directory watcher, CSV/TXT parsing)                        |

**Notes**: Some analyzers support multiple protocols (GeneXpert, Stago STart4,
Sysmex XN). Primary protocol listed.

---

## Gaps & Limitations

### 1. Missing Fixture IDs

- _(None)_ — BC2000 has fixture ID 2012 (promoted to P1, GenericHL7 validation).

### 2. Protocol Assumptions

- **GeneXpert**: FILE mode used for testing. ASTM/HL7 modes are vendor-supported
  but not prioritized for deadline validation.
- **Stago STart4**: ASTM/RS232 mode prioritized. HL7 mode exists per vendor spec
  but not validated.
- **Sysmex XN**: Both RS232 and TCP/IP supported per vendor spec. Fixture
  assumes TCP/IP mode.

### 3. Serial Port Allocation

- RS232 analyzers assume distinct USB-to-serial adapters (`/dev/ttyUSB0` through
  `/dev/ttyUSB4`).
- Lab deployment may require different port assignments based on actual
  hardware.

### 4. Abbott Middleware Requirement

- Abbott Architect may require vendor-provided AlinIQ middleware in production.
  Fixture assumes direct HL7 connection for testing.

---

## Global Plugin Inventory (All 36 Supported Analyzers)

Beyond the 13 analyzers required for Madagascar (Feature 011), OpenELIS supports
23 additional analyzer plugins for a total of 36. All plugins are located in
`plugins/analyzers/` directory.

**Global Test Fixture IDs**: 3000-3035 (see
`src/test/resources/testdata/global-analyzer-inventory.xml`)

### Plugin Categories

| Category       | Plugin Count | ID Range  | Protocols               |
| -------------- | ------------ | --------- | ----------------------- |
| Hematology     | 12           | 3000-3011 | ASTM, HL7               |
| Molecular      | 10           | 3012-3021 | FILE, ASTM, HL7         |
| Chemistry      | 5            | 3022-3026 | ASTM, HL7               |
| Flow Cytometry | 4            | 3027-3030 | FILE, vendor-specific   |
| Immunology     | 1            | 3031      | HL7                     |
| Coagulation    | 1            | 3032      | ASTM, HL7               |
| Generic        | 2            | 3033-3034 | GenericASTM, GenericHL7 |
| Template       | 1            | 3035      | Template/example        |

**Note**: See `plugins/analyzers/INVENTORY.md` for complete list with plugin
names, protocols, and status.

### Generic-First Architecture

Starting with Feature 011, new analyzer integrations should use **GenericASTM**
or **GenericHL7** plugins with loadable default configurations. This approach
eliminates the need for Java code changes when adding new analyzers.

**Benefits**:

- Dashboard-configurable test mappings
- Default config templates for quick setup
- No plugin compilation required
- Consistent architecture across protocols

**GenericFile Plugin**: Deferred pending research on file format abstraction
strategy.

---

## Version History

| Version | Date       | Changes                                                                                                                                                                                                           |
| ------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.0.0   | 2026-02-02 | Initial authoritative inventory. 12 analyzers documented.                                                                                                                                                         |
| 1.1.0   | 2026-02-02 | Research corrections applied. BC2000 promoted to P1 (fixture ID 2012). QuantStudio 7 Flex plugin added (PR #42). Global plugin inventory section added (36 total plugins). Generic-First architecture documented. |

---

**Maintained By**: OpenELIS Global Feature 011 Team  
**Source Repository**: `DIGI-UW/OpenELIS-Global-2`  
**Related Documentation**:

- `specs/011-madagascar-analyzer-integration/spec.md`
- `specs/011-madagascar-analyzer-integration/VERIFICATION-CHECKLIST.md` (NEW)
- `tools/analyzer-mock-server/templates/README.md`
- `plugins/analyzers/*/README.md`
- `plugins/analyzers/INVENTORY.md` (Global plugin list)
- `projects/analyzer-defaults/README.md` (Default config templates)
