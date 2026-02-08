---
name: M19-M20 Immediate Execution
overview:
  Sync SpecKit docs first (spec.md, tasks.md, plan.md), then implement M19
  GenericHL7 and M20 defaults loading as two milestone-sized PRs, using
  /speckit.implement with TDD workflow.
todos:
  - id: doc-sync-spec
    content:
      "Update spec.md: Remove 'BC2000 is P2' references, align with 'GenericHL7
      planned (M19)' + 'BC2000 in scope for M19 validation'"
    status: completed
  - id: doc-sync-tasks
    content:
      "Update tasks.md: M20 tasks reference AnalyzerForm.jsx (not
      AnalyzerConfigForm.jsx), add /speckit.implement + TDD workflow note"
    status: completed
  - id: doc-sync-plan
    content:
      "Update plan.md: M20 section states defaults from filesystem
      /data/analyzer-defaults (configurable via ANALYZER_DEFAULTS_DIR)"
    status: completed
  - id: m19-scan
    content:
      Confirm current HL7 dispatch + GenericASTM matching reuse; identify exact
      packages/classes to reuse for HL7 parsing
    status: completed
  - id: m19-tests-red
    content:
      Write JUnit4 tests for GenericHL7Analyzer.isTargetAnalyzer() and
      GenericHL7LineInserter OBX parsing (Red)
    status: completed
  - id: m19-impl-green
    content:
      Implement GenericHL7Analyzer + GenericHL7LineInserter in plugins submodule
      (Green), mirroring GenericASTM patterns
    status: completed
  - id: m19-integration
    content:
      Add/adjust fixtures for CONFIG-2012 and verify HL7 ingest path can select
      GenericHL7; add integration test
    status: completed
  - id: m20-backend-defaults-api
    content:
      Add filesystem-backed defaults endpoints to AnalyzerRestController with
      path sanitization
    status: completed
  - id: m20-frontend-dropdown
    content:
      Add 'Load Default Config' dropdown to AnalyzerForm.jsx (create mode only)
      + i18n strings
    status: completed
  - id: m20-tests
    content:
      Add backend tests for defaults API + Jest test + Cypress E2E for
      load-default→populate→save
    status: completed
isProject: false
---

# M19-M20 Immediate Execution Plan (with Doc-Sync Gate)

## Phase 0: Doc-Sync Gate (MANDATORY FIRST STEP)

Before any implementation, fix documentation mismatches to ensure
`/speckit.implement` references correct files and scope.

### 0.1 Update spec.md

- **File**: `specs/011-madagascar-analyzer-integration/spec.md`
- **Changes**:
  - Remove remaining "BC2000 is P2" references
  - Align language with "GenericHL7 planned (M19)" + "BC2000 in scope for M19
    validation"
  - Ensure analyzer counts reflect current scope (13 required, 36 plugins)

### 0.2 Update tasks.md

- **File**: `specs/011-madagascar-analyzer-integration/tasks.md`
- **Changes**:
  - M20 tasks: Reference `AnalyzerForm.jsx` (NOT `AnalyzerConfigForm.jsx`)
  - Correct path:
    `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx`
  - Add explicit note: "Execute via `/speckit.implement` + TDD
    (Red→Green→Refactor)"

### 0.3 Update plan.md

- **File**: `specs/011-madagascar-analyzer-integration/plan.md`
- **Changes**:
  - M20 section: Explicitly state defaults served from filesystem
    (`/data/analyzer-defaults`, configurable via `ANALYZER_DEFAULTS_DIR`)
  - Tie M20 endpoints to that filesystem source

---

## Implementation Workflow (MANDATORY)

- **Use `/speckit.implement**` to execute work once doc-sync is complete
- **Follow TDD (Red → Green → Refactor)**:
  - Write failing test first
  - Implement minimal change to make it pass
  - Refactor with tests staying green
  - Do NOT bulk implement then test

---

## Phase 1: M19 - GenericHL7 Plugin (PR 1)

### Architecture (mirroring GenericASTM)

```mermaid
flowchart TD
  Hl7Post[AnalyzerImportController:/analyzer/hl7] --> Hl7Reader[HL7AnalyzerReader]
  Hl7Reader --> MshExtract[HL7MessageServiceImpl.extractMshInfo]
  MshExtract --> Msh3[MSH-3]
  Hl7Reader --> PluginLoop[Iterate AnalyzerImporterPlugin]
  PluginLoop --> GenericHl7IsTarget[GenericHL7Analyzer.isTargetAnalyzer]
  GenericHl7IsTarget --> DbMatch[AnalyzerConfigurationService.findByIdentifierPatternMatch]
  DbMatch -->|match| Inserter[GenericHL7LineInserter]
  Inserter --> Cache[AnalyzerTestNameCache lookup]
  Cache --> Persist[AnalyzerLineInserter.persistImport]
```

### Key Files to Create/Modify

- `plugins/analyzers/GenericHL7/src/main/java/.../GenericHL7Analyzer.java`
- `plugins/analyzers/GenericHL7/src/main/java/.../GenericHL7LineInserter.java`
- `plugins/analyzers/GenericHL7/pom.xml`
- Fixtures: `src/test/resources/testdata/madagascar-analyzer-test-data.xml`
  (BC2000 config)

### Tests (TDD)

- Unit: `isTargetAnalyzer()` MSH-3 regex matching
- Unit: OBX parsing to AnalyzerResults
- Integration: POST HL7 to `/analyzer/hl7` and verify persistence

---

## Phase 2: M20 - Defaults API + UI (PR 2)

### Backend

- **File**:
  `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`
- **Endpoints**:
  - `GET /rest/analyzer/defaults` - list available templates
  - `GET /rest/analyzer/defaults/{protocol}/{name}` - return JSON content
- **Source**: Filesystem `/data/analyzer-defaults` (configurable via
  `ANALYZER_DEFAULTS_DIR`)

### Frontend

- **File**: `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx`
- **Changes**: Add "Load Default Config" dropdown (create mode only)
- **Service**: `frontend/src/services/analyzerService.js` - add
  `getDefaultConfigs()`, `getDefaultConfig()`
- **i18n**: Add keys to `en.json` and `fr.json`

### Tests

- Backend: Defaults listing + path traversal prevention
- Frontend: Jest test for dropdown + form population
- E2E: Cypress test for load-default→populate→save flow

---

## Consistency Gate (End of Each Milestone)

After finishing each PR:

- Verify `tasks.md` checkboxes updated
- Cross-doc alignment check (spec/plan/tasks match implementation)
- Endpoints/files match what's documented

---

## Execution Order

1. **Doc-Sync Gate** (Phase 0) - fix docs first
2. **PR 1**: M19 GenericHL7 (tasks T200-T209)
3. **PR 2**: M20 Defaults API + UI (tasks T210-T219)

## Definition of Done

- **Doc-Sync**: All three docs (spec/plan/tasks) aligned with current scope and
  correct file paths
- **M19**: GenericHL7 plugin exists + tests green; BC2000 handled via MSH-3
  matching
- **M20**: Defaults endpoints work from filesystem; AnalyzerForm loads defaults;
  i18n complete
