---
name: Analyzer Architecture Remediation
overview:
  Remediate Feature 011 by correcting contract inconsistencies, implementing
  Generic-First architecture (GenericASTM/GenericHL7 with default configs),
  adding BC2000, expanding test fixtures to all 35 plugins, and deferring
  GenericFile for future research.
todos:
  - id: p0-contract
    content:
      "Phase 0: Correct contract inconsistencies based on internet research
      findings"
    status: pending
  - id: p1-ba88a
    content:
      "Phase 1.1: Configure BA-88A via GenericASTM with default config (verify
      actual protocol)"
    status: pending
  - id: p1-qs7
    content:
      "Phase 1.2: Review/merge PR #42 (QuantStudio7Flex dedicated plugin)"
    status: pending
  - id: p2-bc2000
    content:
      "Phase 2: Add BC2000 to required list with fixture ID 2012 (use
      GenericHL7)"
    status: pending
  - id: p3-global
    content:
      "Phase 3: Create global-analyzer-inventory.xml for all 35 plugins (IDs
      3000-3034)"
    status: pending
  - id: p4-hl7
    content:
      "Phase 4: Implement GenericHL7 plugin (M19) with default config loading
      option"
    status: pending
  - id: p5-defaults
    content:
      "Phase 5: Create default config templates for common analyzers (ASTM +
      HL7)"
    status: pending
  - id: p6-spec
    content:
      "Phase 6.1: Update spec.md with corrected protocols and Generic-First
      architecture"
    status: pending
  - id: p6-plan
    content:
      "Phase 6.2: Update plan.md with M19-M20 milestones (GenericHL7 + defaults)"
    status: pending
  - id: p6-tasks
    content: "Phase 6.3: Update tasks.md with T200-T229 task blocks"
    status: pending
  - id: p6-contract
    content:
      "Phase 6.4: Update supported-analyzers.md with research corrections"
    status: pending
  - id: todo-1770071685206-531wjt39y
    content: "Phase 7: Commit, Push, create associated plugins PR"
    status: pending
isProject: false
---

# Analyzer Architecture Remediation Plan

## Context

Current state analysis reveals:

- **Mindray BA-88A (GAP)**: Contract requires ASTM/RS232, but Mindray plugin
  only supports HL7
- **QuantStudio 7 Flex (GAP)**: QuantStudio3 plugin does not recognize QS7 files
  (different identification pattern, column names)
- **BC2000**: Listed as P2/deferred but plugin exists - needs activation
- **Test Fixtures**: Only 12 Madagascar analyzers have fixtures; 23 additional
  plugins have no test coverage
- **Architecture Inconsistency**: GenericASTM exists (database-driven), but no
  GenericHL7 equivalent

---

## Internet Research Findings: Contract Inconsistencies

Research conducted 2026-02-02 revealed factual errors in the contract:

| Analyzer             | Contract Says   | Research Shows                                                          | Action                          |
| -------------------- | --------------- | ----------------------------------------------------------------------- | ------------------------------- |
| **Mindray BA-88A**   | ASTM/RS232      | Semi-auto analyzer (2008), limited LIS - may be confused with BS-series | **VERIFY with deployment team** |
| **Mindray BS-360E**  | HL7/TCP only    | Supports BOTH ASTM E1381-95 AND HL7 v2.3.1                              | **EXPAND capabilities**         |
| **Abbott Architect** | HL7/TCP         | RS-232 Serial is primary interface per Abbott docs                      | **VERIFY transport**            |
| **Sysmex XN-L**      | ASTM + HL7      | Only ASTM over TCP/IP confirmed                                         | **REMOVE unverified HL7**       |
| **Stago STart 4**    | ASTM/HL7 RS232  | Older semi-auto, protocol details unclear                               | **VERIFY**                      |
| **GeneXpert**        | FILE, ASTM, HL7 | All three confirmed per Cepheid LIS spec                                | **ACCURATE**                    |
| **Horiba Pentra 60** | ASTM/RS232      | Confirmed ASTM E-1381/E-1394                                            | **ACCURATE**                    |

**Key Finding**: BA-88A is a 2008-era semi-automatic analyzer with basic
connectivity. The contract may have confused it with BS-series (BS-120, BS-360E)
which have full bidirectional LIS support.

---

## Architecture Decision: Generic-First Approach

**Principle**: Use GenericASTM and GenericHL7 plugins with **loadable default
configurations** as the PRIMARY integration method. Vendor-specific plugins
remain for backward compatibility but new analyzers should use generic plugins.

**Benefits**:

- No Java code changes to add new analyzers
- Dashboard-configurable test mappings
- Default configs provide quick-start templates
- Consistent architecture across protocols

**GenericFile: DEFERRED** - Requires more research on file format abstraction
and whether it should replace individual FILE plugins. Will be addressed in
future milestone.

---

## Phase 0: Contract Corrections

Update contract documents with research findings:

### 0.1 supported-analyzers.md Corrections

| Analyzer         | Current    | Corrected              | Notes                              |
| ---------------- | ---------- | ---------------------- | ---------------------------------- |
| BA-88A           | ASTM/RS232 | **NEEDS VERIFICATION** | Semi-auto (2008), may be doc error |
| BS-360E          | HL7 only   | HL7 + ASTM             | Per Mindray LIS Interface Manual   |
| Abbott Architect | HL7/TCP    | HL7 + **RS232 option** | Verify deployed model              |
| Sysmex XN-L      | ASTM + HL7 | **ASTM/TCP only**      | Remove unverified HL7 claim        |

### 0.2 Add Verification Tasks

Create verification checklist for deployment team to confirm:

- Actual analyzer models deployed (BA-88A vs BS-series?)
- Connectivity options available on each unit
- Physical interfaces present (RS232 ports, Ethernet)

**Files to update**:

- [supported-analyzers.md](specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md)
- Add `VERIFICATION-CHECKLIST.md` for deployment team

---

## Phase 1: Validate and Fix Partial Supports

### 1.1 Mindray BA-88A - Use GenericASTM with Default Config

**Problem**: Contract says ASTM/RS232, Mindray plugin only supports HL7, and
research shows BA-88A may have limited LIS.

**Resolution**: Use GenericASTM plugin with loadable default configuration

**Approach**:

1. Create default config template: `analyzer-defaults/mindray-ba88a-astm.json`
2. Configure via Dashboard with "Load Default" option
3. GenericASTM handles all ASTM H-segment pattern matching
4. If BA-88A actually uses HL7 (or is actually a BS-series), use GenericHL7
   instead

**Default Config Template**:

```json
{
  "analyzer_name": "Mindray BA-88A",
  "identifier_pattern": "MINDRAY.*BA-88A|BA88A",
  "protocol": "ASTM",
  "category": "CHEMISTRY",
  "default_test_mappings": [
    { "analyzer_code": "GLU", "test_name": "Glucose", "loinc": "2345-7" },
    { "analyzer_code": "CREA", "test_name": "Creatinine", "loinc": "2160-0" }
  ]
}
```

**Files to update**:

- [madagascar-analyzer-test-data.xml](src/test/resources/testdata/madagascar-analyzer-test-data.xml):
  Add `identifier_pattern`, `is_generic_plugin=true`
- Create `analyzer-defaults/` directory with JSON templates

### 1.2 QuantStudio 7 Flex - Review/Merge PR #42

**Problem**:
[QuantStudio3 plugin](plugins/analyzers/QuantStudio3/src/main/java/oe/plugin/analyzer/QuantStudio3AnalyzerImplementation.java)
identification pattern only matches `QuantStudio 3 System`.

**Solution**: PR #42 in `DIGI-UW/openelisglobal-plugins` creates a dedicated
`QuantStudio7Flex` plugin

**PR #42 Details**
([https://github.com/DIGI-UW/openelisglobal-plugins/pull/42](https://github.com/DIGI-UW/openelisglobal-plugins/pull/42)):

- **New plugin**: `analyzers/QuantStudio7Flex/` (9 files, 901 lines)
- **Files added**:
  - `QuantStudio7FlexAnalyzer.java` - Main plugin with `isTargetAnalyzer()` for
    QS7 detection
  - `QuantStudio7FlexAnalyzerImplementation.java` - Line inserter with
    QS7-specific columns
  - `QuantStudio7FlexMenu.java` - Menu registration
  - `QuantStudio7FlexPermission.java` - Permission setup
  - `QuantStudio7Flex.xml` - Plugin configuration
  - Unit tests for both classes

**Why separate plugin (not modify QS3)**:

1. QS7 Flex has different column structure: `Well Position`, `Target`,
   `Amp Status`
2. Less risk of breaking existing QS3 support
3. Follows existing plugin architecture pattern
4. Unit tests included

**Note**: This is a FILE-based analyzer. GenericFile is deferred, so using a
specific plugin is appropriate.

**Action Required**:

1. Review PR #42 for code quality and constitution compliance
2. Verify unit tests pass
3. Merge PR
4. Update `plugins/analyzers/INVENTORY.md` to list QuantStudio7Flex (plugin #36)
5. Add fixture entry for QuantStudio7Flex in test data

**Files to update after merge**:

- [INVENTORY.md](plugins/analyzers/INVENTORY.md): Add QuantStudio7Flex entry
- [madagascar-analyzer-test-data.xml](src/test/resources/testdata/madagascar-analyzer-test-data.xml):
  Add fixture ID 2008 for QS7 Flex

---

## Phase 2: Add BC2000 to Required List (via GenericHL7)

### 2.1 Approach: GenericHL7 with Default Config

**Current state**: BC2000 listed as P2/deferred with no fixture ID

**Resolution**: Use GenericHL7 plugin (Phase 4) with loadable default config

**Changes**:

- Assign fixture ID **2012** to BC2000
- Configure via GenericHL7 with `identifier_pattern` matching MSH-3 `MINDRAY`
- Create default config: `analyzer-defaults/mindray-bc2000-hl7.json`
- Note: Existing Mindray plugin also works as fallback (legacy path)

**Default Config Template**:

```json
{
  "analyzer_name": "Mindray BC2000",
  "identifier_pattern": "MINDRAY.*BC.?2000",
  "protocol": "HL7",
  "hl7_version": "2.3.1",
  "category": "HEMATOLOGY",
  "msh3_pattern": "MINDRAY",
  "default_test_mappings": [
    {
      "obx_identifier": "WBC",
      "test_name": "White Blood Cells",
      "loinc": "6690-2"
    },
    {
      "obx_identifier": "RBC",
      "test_name": "Red Blood Cells",
      "loinc": "789-8"
    }
  ]
}
```

**Files to update**:

- [madagascar-analyzer-test-data.xml](src/test/resources/testdata/madagascar-analyzer-test-data.xml):
  Add analyzer 2012, CONFIG-2012 with `is_generic_plugin=true`
- [supported-analyzers.md](specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md):
  Update BC2000 to P1, fixture ID 2012
- Create `analyzer-defaults/mindray-bc2000-hl7.json`

---

## Phase 3: Expand Test Fixtures to All 36 Plugins

### 3.1 Create Global Analyzer Test Data File

Create new fixture file for all 36 plugins (IDs 3000-3035, includes
QuantStudio7Flex from PR #42):

**File**: `src/test/resources/testdata/global-analyzer-inventory.xml`

**ID Allocation**:

| ID Range  | Category       | Count |
| --------- | -------------- | ----- |
| 3000-3011 | Hematology     | 12    |
| 3012-3021 | Molecular      | 10    |
| 3022-3026 | Chemistry      | 5     |
| 3027-3030 | Flow Cytometry | 4     |
| 3031      | Immunology     | 1     |
| 3032      | Coagulation    | 1     |
| 3033-3034 | Generic        | 2     |
| 3035      | Template       | 1     |

**Note**: ID 3013 = QuantStudio7Flex (from PR #42)

### 3.2 Update Fixture Loader

**File**:
[load-analyzer-test-data.sh](src/test/resources/load-analyzer-test-data.sh)

Add new flag: `--all-plugins` to load global inventory

### 3.3 Dashboard Verification

Create Cypress test to verify all 36 analyzers display in Dashboard:

- Protocol badges (HL7, ASTM, FILE)
- Category badges
- Active/inactive status

---

## Phase 4: Add GenericHL7 Plugin with Default Config Loading (M19)

### 4.1 Architecture: Generic-First with Defaults

```
┌─────────────────────────────────────────────────────────────┐
│              GenericHL7 with Default Config Loading          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Dashboard: "Add Analyzer"                                   │
│         │                                                    │
│         ├──► "Load Default Config" dropdown                 │
│         │    - Mindray BC2000                                │
│         │    - Mindray BC-5380                               │
│         │    - Abbott Architect                              │
│         │    - (any HL7 analyzer)                            │
│         │                                                    │
│         ▼                                                    │
│  Populates: identifier_pattern, test_mappings, etc.         │
│         │                                                    │
│         ▼                                                    │
│  User can customize before saving                            │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  HL7 ORU^R01 Message arrives                                │
│         │                                                    │
│         ▼                                                    │
│  HL7AnalyzerReader (Core Adapter)                           │
│         │                                                    │
│         ├──► Legacy Plugins iterate first (backward compat) │
│         │                                                    │
│         └──► GenericHL7Plugin.isTargetAnalyzer()            │
│              - Query analyzer_configuration                  │
│              - WHERE identifier_pattern MATCHES MSH-3       │
│              - AND is_generic_plugin = true                 │
│              │                                               │
│              ▼                                               │
│         GenericHL7LineInserter                              │
│         - Load field mappings from DB                        │
│         - Parse OBX segments                                 │
│         - Insert results                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Implementation Files

**New plugin**: `plugins/analyzers/GenericHL7/`

- `GenericHL7Analyzer.java`: Main plugin with MSH-3 pattern matching
- `GenericHL7LineInserter.java`: DB-driven OBX field extraction
- `pom.xml`, `plugin.xml`, `README.md`

**Schema updates** (Liquibase):

- Add to `analyzer_configuration`: `hl7_version`, `msh3_pattern`
- Add to `analyzer_field`: `hl7_segment`, `hl7_field_index`, `hl7_component`

**Default configs directory**: `analyzer-defaults/hl7/`

- `mindray-bc2000.json`
- `mindray-bc5380.json`
- `mindray-bs360e.json`
- `abbott-architect.json`
- `genexpert-hl7.json`

### 4.3 Dashboard UI Updates

Add to AnalyzerConfigForm.jsx:

- "Load Default" dropdown with available templates
- On select, populate form fields from JSON
- User can edit before saving

---

## Phase 5: Default Config Templates for GenericASTM + GenericHL7 (M20)

### 5.1 Create Default Config Templates

**Directory structure**:

```
analyzer-defaults/
├── astm/
│   ├── mindray-ba88a.json
│   ├── horiba-pentra60.json
│   ├── horiba-micros60.json
│   ├── stago-start4.json
│   ├── sysmex-xn.json
│   └── genexpert-astm.json
├── hl7/
│   ├── mindray-bc2000.json
│   ├── mindray-bc5380.json
│   ├── mindray-bs360e.json
│   ├── abbott-architect.json
│   └── genexpert-hl7.json
└── README.md
```

### 5.2 Default Config Schema

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

### 5.3 REST API for Loading Defaults

Add endpoint: `GET /rest/analyzer/defaults`

- Returns list of available default configs
- Frontend populates dropdown

Add endpoint: `GET /rest/analyzer/defaults/{protocol}/{name}`

- Returns specific default config JSON
- Frontend populates form

---

## GenericFile Plugin - DEFERRED

**Status**: Deferred to future milestone

**Reason**: Requires more research to determine:

1. How to abstract diverse file formats (CSV, TSV, fixed-width, multi-line
   headers)
2. Whether GenericFile should eventually replace individual FILE plugins
3. Column mapping strategy (by name vs by position)
4. File identification patterns (filename vs content signature)

**Current approach**: Fix existing FILE plugins (like QuantStudio3) as needed.
Individual FILE plugins remain the standard until GenericFile research is
complete.

---

## Phase 6: Update Specification Documents

### 6.1 spec.md Updates

- Add BC2000 to P1 required list (move from P2)
- Document Generic-First architecture (GenericASTM + GenericHL7 with default
  configs)
- Note GenericFile as deferred
- Update analyzer count from "12" to "13 required + 23 supported" (includes QS7
  Flex from PR #42)
- Add architecture diagram showing Generic plugin pattern
- Add research findings section with protocol corrections
- Reference PR #42 for QuantStudio 7 Flex implementation

### 6.2 plan.md Updates

Add milestones:

- **M19**: GenericHL7 Plugin Implementation (3 days)
- **M20**: Default Config Templates + Dashboard Integration (2 days)
- **M21**: Global Analyzer Inventory Fixtures (2 days)

Update workstream diagram:

```
Workstream F: Architecture Standardization
├── M19: GenericHL7 (3 days)
├── M20: Default Configs (2 days)
└── M21: Global Fixtures (2 days)

Note: GenericFile DEFERRED - requires research
```

### 6.3 tasks.md Updates

Add task blocks:

- T200-T209: GenericHL7 implementation tasks
- T210-T219: Default config templates + API tasks
- T220-T229: Global fixture tasks
- T230-T239: Contract corrections and verification tasks

### 6.4 contracts/supported-analyzers.md Updates

- **BA-88A**: Add verification note, document GenericASTM usage
- **BS-360E**: Add ASTM capability (both HL7 and ASTM supported)
- **Abbott Architect**: Add RS-232 transport option, verification note
- **Sysmex XN-L**: Remove unverified HL7 claim
- **BC2000**: Assign fixture ID 2012, move to P1
- **QuantStudio 7 Flex**: Document PR #42 implementation, assign fixture ID 2008
- Add section: "Protocol Verification Checklist" for deployment team
- Add section: "All 36 Supported Plugins" with global inventory (includes QS7
  Flex)

---

## File Change Summary

### New Files (12)

- `plugins/analyzers/GenericHL7/` (4 files: pom.xml, plugin.xml, Analyzer.java,
  LineInserter.java)
- `analyzer-defaults/astm/` (6 JSON files for ASTM analyzers)
- `analyzer-defaults/hl7/` (5 JSON files for HL7 analyzers)
- `analyzer-defaults/README.md`
- `src/test/resources/testdata/global-analyzer-inventory.xml`
- `src/main/resources/liquibase/3.5.x.x/019-generic-hl7-schema.xml`
- `specs/011-madagascar-analyzer-integration/VERIFICATION-CHECKLIST.md`

### From PR #42 (openelisglobal-plugins repo)

- `analyzers/QuantStudio7Flex/` - Full plugin (9 files, already in PR)

### Modified Files (12)

- `src/test/resources/testdata/madagascar-analyzer-test-data.xml` (add BC2000,
  fix BA-88A, add QS7 Flex)
- `src/test/resources/load-analyzer-test-data.sh` (add --all-plugins flag)
- `src/main/java/org/openelisglobal/analyzer/controller/rest/AnalyzerRestController.java`
  (add defaults API)
- `frontend/src/components/admin/AnalyzerManagement/AnalyzerConfigForm.jsx` (add
  Load Default dropdown)
- `specs/011-madagascar-analyzer-integration/spec.md`
- `specs/011-madagascar-analyzer-integration/plan.md`
- `specs/011-madagascar-analyzer-integration/tasks.md`
- `specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md`
- `plugins/analyzers/INVENTORY.md` (clarify Generic-First architecture, add QS7
  Flex)
- `plugins/README.md` (update architecture section)
- `frontend/cypress/e2e/analyzerDashboardGeneric.cy.js` (expand to test all 36)
- `openelisglobal-plugins/pom.xml` (already in PR #42 - adds QS7Flex module)

---

## Dependency Order

```
Phase 0 (Contract Corrections) ──► Phase 6 (Docs)
                                         │
Phase 1.1 (BA-88A) ──┐                   │
                     ├──► Phase 3 ───────┤
Phase 1.2 (QS7) ─────┤    (Fixtures)     │
                     │                   │
Phase 2 (BC2000) ────┘                   │
                                         │
Phase 4 (GenericHL7) ──► Phase 5 ────────┘
                         (Defaults)
```

**Parallel Execution**:

- Phases 0-2 can run in parallel with Phase 4
- Phase 5 depends on Phase 4 (GenericHL7 needed for HL7 defaults)
- Phase 6 consolidates all changes

---

## Risk Mitigation

### BA-88A Protocol Uncertainty

**Risk**: BA-88A may not support ASTM/RS232 as documented **Mitigation**:

1. Add verification task for deployment team
2. GenericASTM config is easy to change to GenericHL7 if needed
3. Fallback: manual entry if analyzer has no LIS

### Abbott Architect Transport

**Risk**: Contract says TCP, research shows RS-232 **Mitigation**:

1. Verify with deployment team which model is deployed
2. If RS-232, use ASTM-HTTP Bridge (already in harness)
3. GenericHL7 works with either transport (bridge handles serial-to-TCP)

### Legacy Plugin Compatibility

**Risk**: Generic plugins may conflict with legacy plugins **Mitigation**:

1. Legacy plugins iterate FIRST (preserves existing behavior)
2. Generic plugins only match if `is_generic_plugin=true` in config
3. Clear documentation on when to use generic vs legacy
