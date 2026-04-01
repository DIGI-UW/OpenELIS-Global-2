# Plan: Fix GeneXpert ASTM Analyzer Configuration — Bug Fix + Config/Form Separation + E2E Tests

## Context

**Branch**: `fix/011-analyzer-config-form` (bugfix off current
`fix/plugin-init-order`)

When creating or editing a Generic ASTM analyzer (e.g., GeneXpert) through the
UI, two bugs and one design gap block ASTM testing:

1. **Bug**: `NumberFormatException: For input string: "generic-astm"` — frontend
   sends hardcoded string IDs to backend that expects numeric DB IDs
2. **Bug**: Default config loading doesn't populate `identifierPattern`
   (critical for generic plugin device matching)
3. **Design gap**: Config template fields (plugin metadata) and instance fields
   (IP/port/name) are conflated in the form UX. The "Load Default Config"
   dropdown is hidden in edit mode.
4. **Missing**: No E2E tests for the create/edit analyzer form workflow
5. **Missing**: Default configs contain `default_test_mappings` (the
   `/rest/analyzer/defaults/{protocol}/{name}` endpoint returns the full JSON
   including these) but these never get persisted to the DB

After this fix, a user should be able to: select Generic ASTM → load GeneXpert
config → see plugin-level fields auto-populated → provide instance-specific
IP/port → save → have test mappings auto-created.

---

## 1. Backend: Graceful resolution of non-numeric `pluginTypeId`

**File**:
[AnalyzerRestController.java:191-196](src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java#L191-L196)
and
[line 402-406](src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java#L402-L406)

Add a private `resolvePluginType(String pluginTypeId)` method:

- If numeric → `analyzerTypeService.get(pluginTypeId)` (current behavior)
- If non-numeric → map aliases (`"generic-astm"` → name `"Generic ASTM"`) then
  `analyzerTypeService.getAnalyzerTypeByName(lookupName)` (method already exists
  at
  [AnalyzerTypeServiceImpl.java:47](src/main/java/org/openelisglobal/analyzer/service/AnalyzerTypeServiceImpl.java#L47))
- If unresolvable → log warning, return `null` (graceful degradation, not 500)

Replace both `analyzerTypeService.get(form.getPluginTypeId())` calls (create at
line 192, update at line 403) with `resolvePluginType(form.getPluginTypeId())`.

**Test** (TDD — RED first): Add to
[AnalyzerRestControllerTest.java](src/test/java/org/openelisglobal/analyzer/controller/AnalyzerRestControllerTest.java):

- `testCreateAnalyzer_WithNonNumericPluginTypeId_ReturnsCreated` — POST with
  `pluginTypeId: "generic-astm"`, expect 201 not 500
- `testUpdateAnalyzer_WithNonNumericPluginTypeId_ReturnsOk` — PUT with
  `pluginTypeId: "generic-astm"`, expect 200 not 500

---

## 2. Frontend: Separate config-level vs instance-level fields in the form

**File**:
[AnalyzerForm.jsx](frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx)

### 2a. Remove hardcoded fallback plugin types (root cause of NumberFormatException)

| Lines   | Change                                                                                                                                                  |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 58-71   | **Delete** `FALLBACK_PLUGIN_TYPES` constant entirely                                                                                                    |
| 164-166 | Replace `setPluginTypes(FALLBACK_PLUGIN_TYPES)` with `setPluginTypes([])` — if API returns empty, plugin system hasn't initialized; show empty dropdown |

### 2b. Enable default config loading in edit mode

| Lines | Change                                                                                         |
| ----- | ---------------------------------------------------------------------------------------------- |
| 178   | `if (!isEditMode && open)` → `if (open)` — fetch default configs in both modes                 |
| 449   | `{!isEditMode && isGenericPlugin && (` → `{isGenericPlugin && (` — show dropdown in both modes |

### 2c. Fix `handleDefaultConfigSelect` to properly separate concerns

**Current** (lines 233-243): Sets `name`, `analyzerType`, `protocolVersion`,
`port` — mixes config-level and instance-level.

**New behavior**: Config loading should set **plugin/protocol-level** fields
only:

- `identifierPattern` ← `configData.identifier_pattern` (CRITICAL — currently
  missing)
- `analyzerType` (category) ← `configData.category` (this is type metadata)
- `protocolVersion` ← derived from protocol (this is type metadata)
- `pluginTypeId` ← auto-resolve from `configData.protocol.name` to matching
  Generic ASTM/HL7 type ID (currently NOT set — user must manually pick plugin
  type before seeing config dropdown, which is backwards)

Config loading should **NOT** set instance-level fields:

- `name` — instance-specific (user provides, e.g., "Lab Room 3 GeneXpert")
- `port` — instance-specific (depends on physical machine config)
- `ipAddress` — instance-specific (never in config template)

### 2d. Reorder form fields for seamless UX

Current order: Name → Category → Plugin Type → [Default Config] → [Identifier
Pattern] → Protocol → IP/Port → Status

**Proposed order** (group by concern):

**Section 1 — Instance Identity** (user provides):

1. Name (required)
2. Status

**Section 2 — Plugin Configuration** (from template or manual): 3. Plugin Type
dropdown (Generic ASTM / Generic HL7 / specific plugins) 4. Load Default Config
dropdown (visible when generic plugin selected) 5. Identifier Pattern
(auto-filled from config, editable) 6. Category / Analyzer Type (auto-filled
from config, editable) 7. Protocol Version (auto-filled from config, editable)

**Section 3 — Connection** (instance-specific): 8. IP Address 9. Port 10. Test
Connection button

This groups fields by the user's mental model: "What is this machine?" → "What
software/protocol does it use?" → "Where is it on the network?"

---

## 3. Backend: Auto-create test mappings from default config on save

**Files**:

- [AnalyzerRestController.java](src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java)
- [AnalyzerForm.java](src/main/java/org/openelisglobal/analyzer/form/AnalyzerForm.java)
  — currently has fields: `id`, `name`, `analyzerType`, `ipAddress`, `port`,
  `protocolVersion`, `testUnitIds`, `status`, `identifierPattern`,
  `pluginTypeId` (NO `defaultConfigId` yet)

### Backend changes:

**Step 3a**: Add `defaultConfigId` field (String, optional) to
`AnalyzerForm.java`. This is a transient hint — not persisted to DB, just tells
the controller which config template the user selected.

**Step 3b**: In the create endpoint (line ~207, after
`analyzerService.insert(analyzer)`), if `form.getDefaultConfigId()` is non-null:

1. Load the config JSON from filesystem — extract the file-reading logic from
   the existing `getDefaultConfig` endpoint (lines 895-958) into a reusable
   private method
2. Extract `default_test_mappings` array from the parsed JSON
3. For each mapping entry (`analyzer_code`, `loinc`):
   - Look up OpenELIS test by LOINC using
     `testService.getActiveTestsByLoinc(loinc)` (method exists at
     [TestService.java:46](src/main/java/org/openelisglobal/test/service/TestService.java#L46))
   - If test found, create an `AnalyzerTestMapping` record (entity at
     [AnalyzerTestMapping.java](src/main/java/org/openelisglobal/analyzerimport/valueholder/AnalyzerTestMapping.java))
     with composite PK (analyzerId + analyzerTestName=analyzer_code) and
     `testId`
   - If no test found for LOINC, log warning and skip (don't fail the save)
4. Use `analyzerTestMappingService.insert()` for each mapping

**Frontend change**: In `handleSubmit`
([AnalyzerForm.jsx:308](frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx#L308)),
include `defaultConfigId: selectedDefault?.id || null` in `submitData`.

---

## 4. Playwright E2E tests

### New file: `frontend/playwright/fixtures/analyzer-form.ts`

Page Object Model for the analyzer form modal. Uses existing `data-testid`
attributes:

- `analyzer-form` (modal), `analyzer-form-name-input`,
  `analyzer-form-type-dropdown`
- `analyzer-form-plugin-type-dropdown`, `analyzer-form-default-config-dropdown`
- `analyzer-form-identifier-pattern-input`, `analyzer-form-ip-input`,
  `analyzer-form-port-input`
- `analyzer-form-save-button`, `analyzer-form-cancel-button`,
  `analyzer-form-notification`

Methods: `expectOpen()`, `fillName(name)`, `selectPluginType(text)`,
`selectDefaultConfig(text)`, `fillIpAddress(ip)`, `fillPort(port)`, `save()`,
`expectSuccessNotification()`, `expectFieldValue(field, value)`

### New file: `frontend/playwright/tests/analyzer-form.spec.ts`

| Test                                            | Covers                                                                                                                                                                           |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Creates GeneXpert ASTM with default config      | Select Generic ASTM → load genexpert-astm config → verify identifierPattern="GENEXPERT\|CEPHEID", category=MOLECULAR auto-filled → fill name + IP + port → save → verify in list |
| Edits existing GeneXpert                        | Open edit → verify fields populated → change port → save → reopen → verify port updated                                                                                          |
| Loads default config in edit mode               | Open edit on existing → verify config dropdown visible → load config → verify identifierPattern updated                                                                          |
| Form validation rejects missing required fields | Open add → save empty → verify error messages on name and category                                                                                                               |

Test isolation: `TEST-GeneXpert-{timestamp}` names, cleanup via delete API in
`afterAll`.

**Auth**: Playwright auth setup
([auth.setup.ts](frontend/playwright/tests/auth.setup.ts)) uses `TEST_USER` and
`TEST_PASS` env vars (not hardcoded). Tests must set these or have them in CI.
For harness: `TEST_USER=admin TEST_PASS=adminADMIN!`.

Follow existing patterns from
[analyzer-list.ts](frontend/playwright/fixtures/analyzer-list.ts) and
[analyzer-list.spec.ts](frontend/playwright/tests/analyzer-list.spec.ts).

---

## 5. Update 011 spec artifacts

Update these files to document the bugfix and design clarification:

- **[spec.md](specs/011-madagascar-analyzer-integration/spec.md)**: Add
  clarification session entry documenting the config vs instance field
  separation decision
- **[plan.md](specs/011-madagascar-analyzer-integration/plan.md)**: Add bugfix
  milestone entry (M-BF1: Analyzer Config Form Fix)
- **[tasks.md](specs/011-madagascar-analyzer-integration/tasks.md)**: Add task
  entries for the fix, referencing this branch

---

## Implementation Order (TDD)

1. **Archive plan**: Save this plan to
   `.specify/plan-archive/fix-011-analyzer-config-form.md` before starting any
   code changes
2. Create branch `fix/011-analyzer-config-form` from current
   `fix/plugin-init-order`
3. **RED**: Write backend test for non-numeric `pluginTypeId` → verify it fails
   (NumberFormatException)
4. **GREEN**: Add `resolvePluginType()` to controller → verify test passes
5. **RED**: Write Playwright E2E test for create GeneXpert → verify it fails
   (config doesn't populate identifierPattern, form field order wrong)
6. **GREEN**: Apply frontend fixes (remove fallbacks, fix
   handleDefaultConfigSelect, reorder fields, enable edit-mode config)
7. Add `defaultConfigId` to AnalyzerForm.java + auto-create test mappings logic
8. **GREEN**: Verify all Playwright tests pass
9. Update spec/plan/tasks docs
10. Format: `mvn spotless:apply` + `cd frontend && npm run format`

## Verification

1. `mvn test -pl . -Dtest=AnalyzerRestControllerTest` — backend tests pass
   (including new non-numeric pluginTypeId tests)
2. `cd frontend && npx playwright test analyzer-form` — E2E tests pass against
   running harness
3. Manual workflow against harness:
   - Open https://localhost/analyzers → Add Analyzer
   - Fill name "Lab GeneXpert" → select Generic ASTM
   - Load "Cepheid GeneXpert (ASTM Mode)" config
   - Verify: identifierPattern="GENEXPERT|CEPHEID", category=MOLECULAR auto-set,
     name/port NOT overwritten
   - Fill IP=35.82.47.81, port=1200 → save
   - Edit the analyzer → verify config dropdown visible → change port → save
   - Verify test mappings (MTB, RIF, COVID19, HIV) created in DB

## Critical Files

| File                                                                                                                    | Action                                                                 |
| ----------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| [AnalyzerRestController.java](src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java)         | Add `resolvePluginType()`, add test mapping auto-creation              |
| [AnalyzerForm.java](src/main/java/org/openelisglobal/analyzer/form/AnalyzerForm.java)                                   | Add `defaultConfigId` field                                            |
| [AnalyzerForm.jsx](frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx)                                     | Remove fallbacks, fix config loading, reorder fields, enable edit mode |
| [AnalyzerRestControllerTest.java](src/test/java/org/openelisglobal/analyzer/controller/AnalyzerRestControllerTest.java) | Add non-numeric pluginTypeId tests                                     |
| `frontend/playwright/fixtures/analyzer-form.ts`                                                                         | **New** — Page Object Model                                            |
| `frontend/playwright/tests/analyzer-form.spec.ts`                                                                       | **New** — E2E tests                                                    |
| [spec.md](specs/011-madagascar-analyzer-integration/spec.md)                                                            | Add clarification session                                              |
| [plan.md](specs/011-madagascar-analyzer-integration/plan.md)                                                            | Add bugfix milestone                                                   |
| [tasks.md](specs/011-madagascar-analyzer-integration/tasks.md)                                                          | Add fix tasks                                                          |
| [genexpert-astm.json](projects/analyzer-defaults/astm/genexpert-astm.json)                                              | Read-only reference                                                    |
| [AnalyzerTypeServiceImpl.java](src/main/java/org/openelisglobal/analyzer/service/AnalyzerTypeServiceImpl.java)          | Read-only — reuse `getAnalyzerTypeByName()` at line 47                 |
| [TestService.java](src/main/java/org/openelisglobal/test/service/TestService.java)                                      | Read-only — reuse `getActiveTestsByLoinc()` at line 46                 |
| `.specify/plan-archive/fix-011-analyzer-config-form.md`                                                                 | **New** — Archive copy of this plan                                    |
