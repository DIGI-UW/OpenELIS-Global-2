# ASTM Generic Plugin Remediation & Gap 4 — Milestone Report

**Date:** 2026-02-03  
**Feature:** 011-madagascar-analyzer-integration  
**Scope:** ASTM flows audit, GenericASTM hardening, reader semantics alignment,
and Gap 4 (prefer_generic_plugin).

---

## 1. Executive Summary

This milestone covers two bodies of work conducted in a single conversation
thread:

1. **Phases 1–3 (ASTM Generic Plugin Audit):** Audit of ASTM entry points,
   strengthening of the GenericASTM integration test, server-side validation and
   documentation for `identifier_pattern`, alignment of ASTM/Serial reader
   semantics with HL7 (parse-only `readStream()`, plugin match at process/insert
   time), and controller HTTP 400 for “no plugin matched.”
2. **Gap 4 (Plugin order / prefer generic):** Introduction of
   `prefer_generic_plugin` in `analyzer_configuration` so admins can force
   GenericASTM/GenericHL7 to be tried before legacy plugins when both could
   match, without removing the legacy plugin JAR.

All work was implemented test-first or with tests updated, formatted with
Spotless/Prettier, and committed and pushed to
`feat/011-analyzer-dashboard-fixtures`.

---

## 2. Conversation Thread and Work Accomplished

### 2.1 Phase 1 — Audit, Tests, and Explicit Behavior

**Objectives:**

- Confirm ASTM ingestion is plugin-only (no non-plugin fallback).
- Document per-analyzer selection (generic vs legacy).
- Fix and strengthen `GenericASTMIntegrationTest`.
- Add unit tests for `findByIdentifierPatternMatch`.

**Accomplished:**

- **Audit:** All ASTM entry points (HTTP `/analyzer/astm`, Serial, error
  reprocessing) were audited. Finding: ASTM ingestion is **plugin-only**; there
  is no built-in ASTM inserter fallback in the main repo. Documented in
  `specs/011-madagascar-analyzer-integration/plans/astm-flows-audit-report.md`.
- **GenericASTMIntegrationTest:**
  - Corrected semantics: `readStream()` succeeds (parse-only); “no plugin
    matched” is asserted at `processData()` / `insertAnalyzerData()`.
  - Assertions now verify persisted `analyzer_results` by `analyzer_id` and
    `test_id` (not only `test_name`).
  - Fixtures refactored: analyzer, analyzer_configuration, and analyzer_test_map
    inserted via JDBC in `loadFixtures()` to avoid DBUnit column mismatch (e.g.
    `fhir_uuid` in `madagascar-analyzer-test-data.xml`).
  - O-segment parsing in test messages aligned with `GenericASTMLineInserter`
    accession-number expectations.
- **Unit tests:** `AnalyzerConfigurationServiceTest` was extended with tests for
  `findByIdentifierPatternMatch`: null/empty identifier, no candidates,
  substring match, no match, invalid regex (skip with warn), and priority when
  multiple configs match.

**Deliverables:**

- `specs/011-madagascar-analyzer-integration/plans/astm-flows-audit-report.md`
- Updated
  `src/test/java/org/openelisglobal/analyzer/GenericASTMIntegrationTest.java`
- Updated
  `src/test/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationServiceTest.java`

---

### 2.2 Phase 2 — Determinism and Configuration UX Safety

**Objectives:**

- Validate `identifier_pattern` when `is_generic_plugin=true`.
- Document what `identifier_pattern` matches (ASTM H-segment field 4, HL7 MSH-3)
  and give examples.

**Accomplished:**

- **Server-side validation:** In `AnalyzerConfigurationServiceImpl`, `insert()`
  and `update()` call `validateIdentifierPatternForGenericPlugin()`. If
  `is_generic_plugin` is true, `identifier_pattern` must be non-empty and a
  valid Java regex; otherwise a `LIMSRuntimeException` is thrown.
- **Documentation:** `docs/analyzer.md` was updated with a section on generic
  plugin configuration: what string is matched for ASTM vs HL7, regex semantics
  (`.find()`), and example patterns.

**Deliverables:**

- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationServiceImpl.java`
  (validation)
- `docs/analyzer.md` (identifier_pattern and generic plugin behavior)

---

### 2.3 Phase 3 — Align ASTM Reader Semantics with HL7

**Objectives:**

- Make ASTM/Serial readers behave like HL7: `readStream()` only parses lines;
  plugin matching in `processData()` / `insertAnalyzerData()`.
- Return HTTP 400 (with error body) when no plugin matches, instead of 500.

**Accomplished:**

- **ASTMAnalyzerReader / SerialAnalyzerReader:**
  - `readStream()` only reads and parses lines; it no longer performs plugin
    matching and returns true whenever lines are read.
  - Plugin resolution (`setInserterResponder()`) is done in `processData()` and
    `insertAnalyzerData()` via `ensureInserterResponder()`.
  - Error messages updated so “no plugin matched” is reported at process/insert
    time.
- **AnalyzerImportController:** When ASTM processing fails due to no matching
  plugin, the controller returns **HTTP 400 Bad Request** with the reader’s
  error message in the body instead of 500.

**Deliverables:**

- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/SerialAnalyzerReader.java`
- `src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java`

---

### 2.4 Gap 4 — Plugin Order / Prefer Generic Over Legacy

**Problem (from plan):** If both a legacy plugin and GenericASTM (or GenericHL7)
could match the same analyzer identifier, the legacy plugin wins because it
appears earlier in the plugin list. Admins could not force the generic plugin
without removing the legacy JAR.

**Solution:** Add an explicit “prefer generic” setting and use it when choosing
plugin order.

**Accomplished:**

- **Schema:** New column `prefer_generic_plugin BOOLEAN DEFAULT FALSE` on
  `analyzer_configuration` (Liquibase changeset
  `025-add-prefer-generic-plugin-to-analyzer-configuration.xml`).
- **Entity & API:** `AnalyzerConfiguration` has `preferGenericPlugin`;
  `AnalyzerForm` and REST create/update/response include `identifierPattern`,
  `genericPlugin`, and `preferGenericPlugin` for full round-trip.
- **Plugin interface:** `AnalyzerImporterPlugin` gained default
  `isGenericPlugin()` (false). GenericASTM and GenericHL7 override it to return
  true.
- **Ordering:** `PluginAnalyzerService.getAnalyzerPluginsWithGenericFirst()`
  returns the same plugin list with generic plugins first.
- **Readers:** ASTM, Serial, and HL7 readers:
  - Derive message identifier (ASTM H-segment field 4 or HL7 MSH-3).
  - Call `findByIdentifierPatternMatch(identifier)`.
  - If a matching config has `preferGenericPlugin == true`, they use
    `getAnalyzerPluginsWithGenericFirst()`; otherwise default plugin order.
- **Docs:** `docs/analyzer.md` updated with a “Prefer generic plugin
  (`prefer_generic_plugin`)” subsection.
- **Test:**
  `AnalyzerConfigurationServiceTest#testFindByIdentifierPatternMatch_SubstringMatch_ReturnsConfig`
  sets and asserts `preferGenericPlugin` on the returned config.
- **Submodule:** In `plugins`, GenericASTMAnalyzer and GenericHL7Analyzer
  implement `isGenericPlugin()` (committed on `develop`).

**Deliverables:**

- `src/main/resources/liquibase/3.4.x.x/025-add-prefer-generic-plugin-to-analyzer-configuration.xml`
- `src/main/resources/liquibase/3.4.x.x/base.xml` (include of 025)
- `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerConfiguration.java`
- `src/main/java/org/openelisglobal/analyzer/form/AnalyzerForm.java`
- `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`
- `src/main/java/org/openelisglobal/plugin/AnalyzerImporterPlugin.java`
- `src/main/java/org/openelisglobal/common/services/PluginAnalyzerService.java`
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/SerialAnalyzerReader.java`
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReader.java`
- `docs/analyzer.md` (prefer_generic_plugin section)
- `src/test/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationServiceTest.java`
- `plugins/analyzers/GenericASTM/.../GenericASTMAnalyzer.java`
- `plugins/analyzers/GenericHL7/.../GenericHL7Analyzer.java`

---

## 3. Commits and Branch

- **Branch:** `feat/011-analyzer-dashboard-fixtures`
- **First commit (Phases 1–3):**  
  `011: Generic ASTM plugin audit, validation, and reader semantics`  
  (Phase 1: audit, GenericASTM test, findByIdentifierPatternMatch tests; Phase
  2: validation + docs; Phase 3: reader semantics + controller 400.)
- **Second commit (Gap 4):**  
  `011 (Gap 4): prefer_generic_plugin to force GenericASTM/GenericHL7 over legacy`  
  (Schema, entity, form, REST, interface, PluginAnalyzerService, readers, docs,
  test; submodule `plugins` updated to include GenericASTM/GenericHL7
  `isGenericPlugin()`.)
- **Push:** Both commits pushed to
  `origin/feat/011-analyzer-dashboard-fixtures`.

**Note:** The `plugins` submodule commit was made on `develop` inside the
submodule. Pushing that submodule’s `develop` to the plugins remote is separate
if needed.

---

## 4. Testing and Quality

- **Tests run:** `AnalyzerConfigurationServiceTest` (including
  `testFindByIdentifierPatternMatch_SubstringMatch_ReturnsConfig` with
  `preferGenericPlugin`), `GenericASTMIntegrationTest`, and
  `GenericHL7IntegrationTest` were run; relevant tests passed.
- **Formatting:** Backend formatted with `mvn spotless:apply`; pre-commit hooks
  re-staged formatted files.
- **Constitution:** Layered architecture, Carbon/React Intl, Liquibase for
  schema, no `@Transactional` in controllers, and service-layer validation were
  respected.

---

## 5. References

- **Audit report:**
  `specs/011-madagascar-analyzer-integration/plans/astm-flows-audit-report.md`
- **Archived plan (earlier):**
  `specs/011-madagascar-analyzer-integration/plans/astm-genericplugin-audit-2026-02-03.plan.md`
- **Cursor plan (this thread):**
  `specs/011-madagascar-analyzer-integration/plans/astm_genericplugin_audit_54cd1795.plan.md`
- **Docs:** `docs/analyzer.md` (identifier_pattern, prefer_generic_plugin)
- **AGENTS.md / constitution:** `.specify/memory/constitution.md`, `AGENTS.md`

---

## 6. Summary Table

| Item                                                       | Status                      |
| ---------------------------------------------------------- | --------------------------- |
| ASTM entry-point audit                                     | Done; documented            |
| GenericASTM integration test                               | Fixed and strengthened      |
| findByIdentifierPatternMatch unit tests                    | Added                       |
| identifier_pattern validation (is_generic_plugin=true)     | Added                       |
| identifier_pattern documentation                           | Updated in docs/analyzer.md |
| ASTM/Serial reader semantics (parse-only readStream)       | Aligned with HL7            |
| Controller 400 for “no plugin matched”                     | Implemented                 |
| prefer_generic_plugin column and entity                    | Added                       |
| REST/Form for preferGenericPlugin (and generic config)     | Added                       |
| isGenericPlugin() and getAnalyzerPluginsWithGenericFirst() | Implemented                 |
| ASTM/Serial/HL7 readers use prefer order when set          | Implemented                 |
| prefer_generic_plugin documentation                        | Added in docs/analyzer.md   |
| Commits pushed to feat/011-analyzer-dashboard-fixtures     | Done                        |
