---
name: Comprehensive Remediation Plan
overview: |
  Addresses /speckit.analyze findings, completes outstanding items from
  architecture-remediation and m19-m20 plans, and validates M19/M20 
  implementation against spec/plan/tasks.
created: 2026-02-03
todos:
  - id: fix-i1-priority
    content: "Fix I1 (HIGH): Update plan.md BC2000 priority from P2 to P1-H"
    status: pending
  - id: update-task-status
    content:
      "Fix C1-C4 (MEDIUM): Update task completion status for M6, M7, M8, M14"
    status: pending
  - id: reconcile-task-count
    content: "Fix I2 (MEDIUM): Reconcile task count summary in tasks.md"
    status: pending
  - id: verify-m19-m20
    content: "Verify M19/M20 implementation completeness vs spec requirements"
    status: pending
  - id: contract-corrections
    content:
      "Complete Phase 0 from architecture-remediation: contract corrections"
    status: pending
  - id: ba88a-default
    content:
      "Complete Phase 1.1 from architecture-remediation: BA-88A GenericASTM
      default"
    status: pending
  - id: qs7-pr42
    content:
      "Complete Phase 1.2 from architecture-remediation: Review/merge QS7 PR #42"
    status: pending
  - id: global-fixtures
    content:
      "Complete Phase 3 from architecture-remediation: Global fixtures testing"
    status: pending
  - id: build-test-validate
    content: "Build project, run tests, debug failures (requested by user)"
    status: pending
isProject: false
---

# Comprehensive Remediation Plan (2026-02-03)

## Executive Summary

This plan consolidates:

1. **Analysis findings** from `/speckit.analyze` (14 issues: 1 HIGH, 6 MEDIUM, 7
   LOW)
2. **Outstanding work** from `architecture-remediation-2026-02-02.plan.md`
3. **Validation gaps** from `m19-m20-immediate-execution.plan.md`
4. **Build/test verification** requested by user

**Status of M19/M20**: ✅ Implementation complete, ⚠️ validation pending

---

## Phase 0: Critical Documentation Fixes (30 min)

### 0.1 Fix I1 (HIGH Priority): BC2000 Priority Inconsistency

**Issue**: `spec.md` line 539 says BC2000 is "P1-H (GenericHL7 Validation)" but
`plan.md` line 166 shows "P2"

**Files affected**:

- `specs/011-madagascar-analyzer-integration/plan.md`

**Change required**:

```markdown
# In plan.md Priority-Ordered Analyzer List (line ~166-167)

- CHANGE: | **P2** | Mindray BC2000 | HL7 | ✅ Mindray | B |
- TO: | **P1-H** | Mindray BC2000 | HL7 | ✅ GenericHL7 (M19) | B |
```

**Verification**: Search for all "BC2000" references in spec/plan/tasks and
ensure consistent priority tagging

---

### 0.2 Fix C1-C4 (MEDIUM): Update Task Completion Status

**Issue**: Tasks for M6, M7, M8, M14 show unchecked but PRs may be merged

**Investigation needed**:

```bash
# Check which milestone PRs are actually merged
gh pr list --state merged --search "m6 OR m7 OR m8 OR m14" --limit 10
```

**Expected actions**:

- M6 (Mindray Serial): If PR merged, mark T134-T142 as `[x]`
- M7 (GeneXpert Multi): If PR merged, mark T143-T154 as `[x]`
- M8 (QuantStudio): If PR merged, mark T155-T164 as `[x]`
- M14 (P2 Validation): If PR merged, mark T215-T224 as `[x]`

**Files affected**:

- `specs/011-madagascar-analyzer-integration/tasks.md`

---

### 0.3 Fix I2 (MEDIUM): Reconcile Task Count

**Issue**: Header claims "~340 tasks" but summary table shows "~304"

**Action**: Count actual tasks (T001-T314 original + T200-T239 remediation)

**Expected outcome**: Update line 1549-1551 in `tasks.md` to reflect accurate
count

---

### 0.4 Fix I3 (LOW): Clarify Analyzer Count

**Issue**: Spec says "13 required" but table shows 12 rows

**Clarification needed**: Is Sysmex XN (#12) considered "required" or
"P2/optional"?

**Resolution**:

- If BC2000 is the 13th: Update spec line 11 to say "12 required + BC2000 for
  GenericHL7 validation"
- If Sysmex XN is required: Keep "13 required" and ensure it's clear in table

---

## Phase 1: Complete Architecture-Remediation Outstanding Items

### 1.1 Contract Corrections (from arch-remediation Phase 0)

**Status**: Partially complete (research done, VERIFICATION-CHECKLIST.md exists)

**Remaining work**:

- [ ] Update `contracts/supported-analyzers.md` with corrected protocols:
  - BS-360E: Add "Supports BOTH HL7 AND ASTM E1381-95"
  - Sysmex XN-L: Note "ASTM/TCP only (HL7 unverified)"
  - Abbott Architect: Add "RS-232 primary per Abbott docs, verify deployed
    model"
  - BA-88A: Add "⚠️ Semi-auto (2008), verify LIS capability with deployment
    team"

**Files to modify**:

- `specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md`

**Cross-reference**: Architecture-remediation Phase 0 (lines 123-146)

---

### 1.2 BA-88A Default Config (from arch-remediation Phase 1.1)

**Status**: Not started

**Action**: Create GenericASTM default config for BA-88A

**Files to create**:

- `analyzer-defaults/astm/mindray-ba88a.json`

**Content**:

```json
{
  "schema_version": "1.0",
  "analyzer_name": "Mindray BA-88A",
  "manufacturer": "Mindray",
  "category": "CHEMISTRY",
  "protocol": "ASTM",
  "protocol_version": "E1381-95",
  "identifier_pattern": "MINDRAY.*BA.?88A|BA88A",
  "transport": "RS232",
  "default_test_mappings": [
    {
      "analyzer_code": "GLU",
      "test_name": "Glucose",
      "loinc": "2345-7",
      "unit": "mg/dL"
    },
    {
      "analyzer_code": "CREA",
      "test_name": "Creatinine",
      "loinc": "2160-0",
      "unit": "mg/dL"
    },
    {
      "analyzer_code": "ALT",
      "test_name": "ALT",
      "loinc": "1742-6",
      "unit": "U/L"
    },
    {
      "analyzer_code": "AST",
      "test_name": "AST",
      "loinc": "1920-8",
      "unit": "U/L"
    }
  ],
  "notes": "Semi-automatic analyzer (2008). Verify LIS capability with deployment team."
}
```

**Verification**: If BA-88A is confirmed as HL7 (not ASTM), create
`analyzer-defaults/hl7/mindray-ba88a.json` instead

**Cross-reference**: Architecture-remediation Phase 1.1 (lines 153-190)

---

### 1.3 QuantStudio7Flex PR #42 (from arch-remediation Phase 1.2)

**Status**: Unknown - needs investigation

**Action**:

```bash
# Check if PR #42 exists in openelisglobal-plugins repo
gh pr view 42 --repo DIGI-UW/openelisglobal-plugins
```

**If PR exists and open**: Review for quality, merge **If PR merged**: Verify
plugin in plugins/analyzers/QuantStudio7Flex/ **If PR doesn't exist**: Check if
QuantStudio7Flex plugin already in repo

**Follow-up**:

- Update `plugins/analyzers/INVENTORY.md` to list QuantStudio7Flex as plugin #36
- Update analyzer count from 35 to 36 in spec/plan/tasks

**Cross-reference**: Architecture-remediation Phase 1.2 (lines 192-235)

---

### 1.4 Global Fixtures Testing (from arch-remediation Phase 3)

**Status**: Fixtures created (per tasks.md T220-T221 marked complete), E2E tests
pending

**Outstanding tasks from tasks.md**:

- [ ] T222 [M21] Test loading global fixtures:
      `./load-analyzer-test-data.sh --all-plugins`
- [ ] T223 [M21] Verify all 36 analyzers inserted into `analyzer` table
- [ ] T224 [M21] [TEST] Create Cypress test
      `analyzerDashboardGlobalInventory.cy.js`
- [ ] T225 [M21] [TEST] Test Dashboard displays all 36 analyzers with correct
      protocol badges
- [ ] T226 [M21] [TEST] Test protocol filtering (show only HL7, ASTM, FILE)
- [ ] T227 [M21] [TEST] Test category filtering (HEMATOLOGY, CHEMISTRY,
      MOLECULAR, etc.)
- [ ] T228 [M21] [TEST] Test GenericASTM and GenericHL7 badges show
      "Configurable"
- [ ] T229 [M21] Verify performance: Dashboard loads in <2 seconds with 36
      analyzers

**Action**: Execute M21 tasks T222-T229

**Cross-reference**: Architecture-remediation Phase 3 (lines 288-327), tasks.md
lines 1636-1665

---

## Phase 2: Validate M19/M20 Implementation (1 hour)

### 2.1 M19 Verification Checklist

**Expected artifacts** (from m19-m20 plan):

- [x] `plugins/analyzers/GenericHL7/src/main/java/.../GenericHL7Analyzer.java`
- [x] `plugins/analyzers/GenericHL7/src/main/java/.../GenericHL7LineInserter.java`
- [x] `plugins/analyzers/GenericHL7/pom.xml`
- [x] Unit tests for isTargetAnalyzer() and OBX parsing
- [x] Integration test: GenericHL7IntegrationTest.java
- [ ] Liquibase changeset for HL7 schema additions ⚠️ **MISSING**

**Missing from M19**:

- **Liquibase changeset** `019-generic-hl7-schema.xml` (from tasks.md T202)
  - Add `hl7_version`, `msh3_pattern` to `analyzer_configuration`
  - Add `hl7_segment`, `hl7_field_index`, `hl7_component` to `analyzer_field`

**Action**: Create missing Liquibase changeset or verify if GenericHL7 works
without schema changes

---

### 2.2 M20 Verification Checklist

**Expected artifacts** (from m19-m20 plan):

- [x] `GET /rest/analyzer/defaults` endpoint in AnalyzerRestController
- [x] `GET /rest/analyzer/defaults/{protocol}/{name}` endpoint
- [x] Load Default Config dropdown in AnalyzerForm.jsx
- [x] `analyzerService.getDefaultConfigs()` and `getDefaultConfig()`
- [x] i18n keys in en.json and fr.json
- [x] Backend tests: AnalyzerDefaultsRestControllerTest.java
- [x] Frontend tests: AnalyzerForm.defaultConfigs.test.jsx
- [x] E2E test: analyzerDefaultConfigs.cy.js
- [ ] All 11 default config templates ⚠️ **INCOMPLETE**

**Missing default configs** (per arch-remediation Phase 5):

ASTM templates:

- [x] `analyzer-defaults/astm/stago-start4.json` (exists)
- [x] `analyzer-defaults/astm/sysmex-xn.json` (exists)
- [x] `analyzer-defaults/astm/genexpert-astm.json` (exists)
- [x] `analyzer-defaults/astm/horiba-pentra60.json` (exists)
- [x] `analyzer-defaults/astm/horiba-micros60.json` (exists)
- [ ] `analyzer-defaults/astm/mindray-ba88a.json` (Phase 1.2 above)

HL7 templates:

- [x] `analyzer-defaults/hl7/abbott-architect.json` (exists)
- [x] `analyzer-defaults/hl7/genexpert-hl7.json` (exists)
- [x] `analyzer-defaults/hl7/mindray-bc2000.json` (exists)
- [x] `analyzer-defaults/hl7/mindray-bc5380.json` (exists)
- [x] `analyzer-defaults/hl7/mindray-bs360e.json` (exists)

**Action**: Verify all 11 templates exist (only BA-88A missing if needed)

---

### 2.3 BC2000 Fixture Verification

**Expected** (from arch-remediation Phase 2):

- Fixture ID 2012 added to madagascar-analyzer-test-data.xml
- CONFIG-2012 with `is_generic_plugin=true` and MSH-3 pattern

**Action**: Read `madagascar-analyzer-test-data.xml` and verify BC2000 entry
exists

---

## Phase 3: Build and Test Validation (User Request)

### 3.1 Build Verification

**User request**: "build, run tests, and debug any issues"

**Current state**: Build failed with compilation errors (already fixed)

- ✅ Fixed: FileImportConfigurationService → FileImportService
- ✅ Fixed: SerialPortConfigurationService → SerialPortService
- ✅ Fixed: getResponseBody() → getResponse()

**Actions**:

1. Run full build: `mvn clean install -DskipTests -Dmaven.test.skip=true`
2. If successful, run unit tests: `mvn test`
3. If unit tests pass, run M19/M20 specific tests:
   - `mvn test -Dtest="*GenericHL7*"`
   - `mvn test -Dtest="*AnalyzerDefaults*"`
4. Run frontend tests:
   `cd frontend && npm test -- --testPathPattern="AnalyzerForm"`
5. Run E2E test: `npm run cy:spec "cypress/e2e/analyzerDefaultConfigs.cy.js"`

**Debug strategy** (if failures):

- Generate 3-5 hypotheses about failure causes
- Instrument code with debug logs
- Ask user to reproduce with log capture
- Analyze logs, fix with runtime evidence
- Verify fix with before/after log comparison

---

### 3.2 Integration Test Coverage

**From spec.md requirements not explicitly tested**:

| Requirement                 | Coverage Status | Action                               |
| --------------------------- | --------------- | ------------------------------------ |
| FR-012 (Error records)      | ⚠️ Implicit     | Add explicit error dashboard test    |
| FR-013 (Batch reprocessing) | ❌ Missing      | Add to M18 or defer                  |
| FR-014 (Connection status)  | ⚠️ Partial      | Verify testConnection endpoints work |
| CR-005 (FHIR sync)          | ❌ Missing      | Add fhir_uuid verification or defer  |
| CR-007 (Audit trail)        | ⚠️ Partial      | Verify sys_user_id populated         |

**Action**: Create verification test checklist for M18 or add explicit tasks

---

## Phase 4: Complete Default Config Templates

### 4.1 Missing BA-88A Template (if needed)

**Status**: See Phase 1.2 above

**Action**: Create only if BA-88A is confirmed as ASTM (not HL7)

---

### 4.2 Verify All Templates Load Correctly

**Test**: Call `GET /rest/analyzer/defaults` and verify 11 templates returned

**Expected response**:

```json
{
  "astm": [
    {
      "name": "mindray-ba88a",
      "displayName": "Mindray BA-88A",
      "category": "CHEMISTRY"
    },
    {
      "name": "horiba-pentra60",
      "displayName": "Horiba Pentra 60",
      "category": "CHEMISTRY"
    },
    {
      "name": "horiba-micros60",
      "displayName": "Horiba Micros 60",
      "category": "HEMATOLOGY"
    },
    {
      "name": "stago-start4",
      "displayName": "Stago STart 4",
      "category": "COAGULATION"
    },
    {
      "name": "sysmex-xn",
      "displayName": "Sysmex XN",
      "category": "HEMATOLOGY"
    },
    {
      "name": "genexpert-astm",
      "displayName": "GeneXpert ASTM",
      "category": "MOLECULAR"
    }
  ],
  "hl7": [
    {
      "name": "mindray-bc2000",
      "displayName": "Mindray BC2000",
      "category": "HEMATOLOGY"
    },
    {
      "name": "mindray-bc5380",
      "displayName": "Mindray BC-5380",
      "category": "HEMATOLOGY"
    },
    {
      "name": "mindray-bs360e",
      "displayName": "Mindray BS-360E",
      "category": "CHEMISTRY"
    },
    {
      "name": "abbott-architect",
      "displayName": "Abbott Architect",
      "category": "CHEMISTRY"
    },
    {
      "name": "genexpert-hl7",
      "displayName": "GeneXpert HL7",
      "category": "MOLECULAR"
    }
  ]
}
```

**Action**: Run manual API test or add automated test

---

## Phase 5: Outstanding Documentation Updates

### 5.1 Update contracts/supported-analyzers.md

**From arch-remediation Phase 6.4** (line 529-540):

Add research corrections:

- BA-88A: "⚠️ Semi-auto (2008), verify LIS capability"
- BS-360E: "Supports BOTH HL7 v2.3.1 AND ASTM E1381-95"
- Abbott Architect: "RS-232 primary per Abbott docs, verify transport"
- Sysmex XN-L: "ASTM/TCP only (HL7 claim unverified)"

Add verification checklist section for deployment team

**File**:
`specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md`

---

### 5.2 Update INVENTORY.md (if QS7 PR #42 merged)

**Action**: Add QuantStudio7Flex as plugin #36

**File**: `plugins/analyzers/INVENTORY.md`

---

### 5.3 Add Missing Technical Details

**From analysis findings**:

**A1 (MEDIUM)**: Add RS232 bridge deployment diagram to spec.md or plan.md

**U1 (MEDIUM)**: Add order export status state machine to FR-016

**U2 (LOW)**: Add Abbott middleware verification task to M12 section

**Files affected**:

- `specs/011-madagascar-analyzer-integration/spec.md`
- `specs/011-madagascar-analyzer-integration/plan.md`

---

## Phase 6: Test Execution and Validation

### 6.1 Build and Unit Tests

**Priority 1**: Verify build succeeds

```bash
# From workspace root
mvn clean install -DskipTests -Dmaven.test.skip=true
```

**Priority 2**: Run unit tests

```bash
# All tests
mvn test

# M19/M20 specific
mvn test -Dtest="*GenericHL7*,*AnalyzerDefaults*"
```

**Expected outcomes**:

- GenericHL7Analyzer unit tests pass (MSH-3 pattern matching)
- GenericHL7LineInserter unit tests pass (OBX parsing)
- AnalyzerDefaultsRestControllerTest passes (API endpoints, security)

---

### 6.2 Integration Tests

**Priority 3**: Run integration tests

```bash
# GenericHL7 integration test
mvn test -Dtest="GenericHL7IntegrationTest"
```

**Expected outcome**: BC2000 HL7 message processed via GenericHL7 plugin

---

### 6.3 Frontend Tests

**Priority 4**: Run frontend unit tests

```bash
cd frontend
npm test -- --testPathPattern="AnalyzerForm"
```

**Expected outcome**:

- AnalyzerForm.defaultConfigs.test.jsx passes
- Dropdown population works
- Form field mapping works

---

### 6.4 E2E Tests

**Priority 5**: Run E2E tests

```bash
npm run cy:spec "cypress/e2e/analyzerDefaultConfigs.cy.js"
```

**Expected outcome**: Load default → populate form → save → verify DB

---

### 6.5 M21 Global Fixtures Testing

**Priority 6**: Execute M21 tasks (T222-T229)

```bash
# Load global fixtures
./src/test/resources/load-analyzer-test-data.sh --all-plugins

# Verify count
psql -h localhost -U clinlims -d clinlims -c \
  "SELECT COUNT(*) FROM clinlims.analyzer WHERE id BETWEEN 3000 AND 3035;"
# Expected: 36

# Run Cypress test
npm run cy:spec "cypress/e2e/analyzerDashboardGlobalInventory.cy.js"
```

**Expected outcome**: All 36 analyzers display with correct badges

---

## Phase 7: Debug Workflow (If Tests Fail)

### 7.1 Debug Protocol (from system reminder)

**If any test fails**, follow systematic debug workflow:

1. **Generate hypotheses** (3-5 specific theories about failure cause)
2. **Instrument code** with debug logs:
   - Log path: `/home/ubuntu/OpenELIS-Global-2/.cursor/debug.log`
   - Server endpoint:
     `http://localhost:7242/ingest/2e2e884b-0892-4585-96d6-06a1c9fc22b3`
3. **Clear log file** before reproduction: Delete `.cursor/debug.log`
4. **Ask user to reproduce** with specific steps
5. **Analyze logs** with cited line numbers
6. **Fix with runtime evidence** (not guesses)
7. **Verify fix** with before/after log comparison
8. **Remove instrumentation** only after user confirms success

### 7.2 Common Failure Scenarios

**Scenario 1: GenericHL7 plugin not loading**

- Hypothesis: Plugin JAR not built or not in classpath
- Instrument: PluginLoader, GenericHL7Analyzer.connect()
- Verify: Check plugins submodule commit, rebuild plugin JAR

**Scenario 2: Defaults API returns empty list**

- Hypothesis: ANALYZER_DEFAULTS_DIR not set or path wrong
- Instrument: AnalyzerRestController.getDefaultConfigs()
- Verify: Check environment variable, filesystem permissions

**Scenario 3: Form doesn't populate from default**

- Hypothesis: JSON parsing error or field mapping mismatch
- Instrument: AnalyzerForm.handleDefaultConfigSelect()
- Verify: Console logs in browser, JSON structure vs form fields

**Scenario 4: Integration test fails (GenericHL7)**

- Hypothesis: BC2000 fixture missing or pattern doesn't match
- Instrument: GenericHL7Analyzer.isTargetAnalyzer(), database query
- Verify: Check madagascar-analyzer-test-data.xml for CONFIG-2012

---

## Phase 8: Final Documentation Reconciliation

### 8.1 Update Task Checkboxes

**After validation**, mark complete:

- [x] T200-T209 (M19 tasks) - if all tests pass
- [x] T210-T219 (M20 tasks) - if all tests pass
- [ ] T222-T229 (M21 tasks) - execute in Phase 6.5

**File**: `specs/011-madagascar-analyzer-integration/tasks.md`

---

### 8.2 Update Milestone Status in plan.md

**Milestones to update**:

- M19: Mark "✅ COMPLETE" if tests pass
- M20: Mark "✅ COMPLETE" if tests pass
- M21: Mark "⚠️ IN PROGRESS" or "✅ COMPLETE" based on Phase 6.5

**File**: `specs/011-madagascar-analyzer-integration/plan.md`

---

### 8.3 Cross-Doc Consistency Check

**Verify alignment**:

- [ ] spec.md analyzer count matches plan.md and tasks.md
- [ ] BC2000 priority consistent across all docs (P1-H)
- [ ] M19/M20 deliverables match actual implementation
- [ ] Default config count (11 total) matches templates in analyzer-defaults/

---

## Execution Summary

| Phase                      | Priority     | Time Est   | Dependencies     | Status  |
| -------------------------- | ------------ | ---------- | ---------------- | ------- |
| 0.1 (BC2000 priority fix)  | HIGH         | 5 min      | -                | Pending |
| 0.2 (Task status update)   | MEDIUM       | 15 min     | PR investigation | Pending |
| 0.3 (Task count)           | MEDIUM       | 5 min      | -                | Pending |
| 0.4 (Analyzer count)       | LOW          | 5 min      | -                | Pending |
| 1.1 (Contract corrections) | MEDIUM       | 20 min     | -                | Pending |
| 1.2 (BA-88A default)       | LOW          | 15 min     | -                | Pending |
| 1.3 (QS7 PR #42)           | MEDIUM       | 30 min     | GitHub access    | Pending |
| 1.4 (Global fixtures)      | MEDIUM       | 45 min     | Phase 6.5        | Pending |
| 2.1 (M19 verification)     | HIGH         | 20 min     | Build success    | Pending |
| 2.2 (M20 verification)     | HIGH         | 15 min     | Build success    | Pending |
| 2.3 (BC2000 fixture)       | MEDIUM       | 10 min     | -                | Pending |
| **3 (Build/Test)**         | **CRITICAL** | **1 hour** | Fixes above      | Pending |
| 4 (Default templates)      | LOW          | 15 min     | BA-88A decision  | Pending |
| 5 (Doc updates)            | LOW          | 30 min     | All above        | Pending |

**Total estimated time**: 4-5 hours

---

## Recommended Execution Order

### Option A: Validation-First (Recommended)

1. **Immediate**: Phase 3 (Build/Test) - Validate what's implemented
2. **If tests pass**: Phase 0 (Doc fixes), Phase 8 (Reconciliation)
3. **If tests fail**: Phase 7 (Debug with instrumentation)
4. **Then**: Phase 1 (Outstanding arch-remediation items)
5. **Finally**: Phase 5 (Final doc updates)

**Rationale**: Validate working code before adding more complexity

---

### Option B: Fix-First

1. **Immediate**: Phase 0 (Critical doc fixes)
2. **Then**: Phase 1 (Complete arch-remediation)
3. **Then**: Phase 3 (Build/Test with all fixes)
4. **Debug**: Phase 7 (If needed)
5. **Finally**: Phase 8 (Reconciliation)

**Rationale**: Fix known issues before validation

---

## Definition of Done

- [ ] All HIGH/MEDIUM issues from analysis resolved
- [ ] Build succeeds (`mvn clean install`)
- [ ] All M19/M20 tests pass (unit + integration + E2E)
- [ ] M21 global fixtures load successfully
- [ ] All 11 default configs accessible via API
- [ ] spec.md, plan.md, tasks.md aligned (BC2000 priority, task status, counts)
- [ ] contracts/supported-analyzers.md updated with research findings
- [ ] QuantStudio7Flex status clarified (PR #42 merged or pending)

---

## Risk Assessment

| Risk                                    | Likelihood | Impact | Mitigation                                        |
| --------------------------------------- | ---------- | ------ | ------------------------------------------------- |
| Build fails due to missing dependencies | Medium     | High   | Check plugins submodule, rebuild GenericHL7 JAR   |
| Tests fail due to missing fixtures      | Medium     | High   | Verify madagascar-analyzer-test-data.xml complete |
| Defaults API returns empty              | Low        | Medium | Check ANALYZER_DEFAULTS_DIR, verify filesystem    |
| GenericHL7 doesn't match BC2000         | Low        | High   | Debug MSH-3 pattern matching with logs            |
| Global fixtures exceed test DB capacity | Low        | Low    | Load separately, not with madagascar fixtures     |

---

## Next Steps (User Decision)

**Recommend**: Start with **Option A (Validation-First)**

1. Run Phase 3.1-3.4 (Build and unit tests) to validate M19/M20 implementation
2. If tests pass: Quick doc fixes (Phase 0) then move to arch-remediation items
3. If tests fail: Debug workflow (Phase 7) before continuing

**Alternative**: If you prefer doc consistency first, start with **Option B
(Fix-First)**

**Question for user**: Which option do you prefer, or should I proceed with
Option A (validation-first)?

---

## Cross-References

- **Analysis report**: Output from `/speckit.analyze` (14 findings)
- **Architecture remediation**:
  `plans/architecture-remediation-2026-02-02.plan.md` (Phases 0-7)
- **M19-M20 execution**: `plans/m19-m20-immediate-execution.plan.md` (all todos
  complete)
- **Constitution**: `.specify/memory/constitution.md` (v1.9.0, 9 principles)
- **Tasks**: `tasks.md` (T001-T314 + T200-T239 remediation)
