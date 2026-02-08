# ASTM Entry Points and Plugin Selection Audit Report

**Date:** 2026-02-03  
**Plan:** astm-genericplugin-audit-2026-02-03 (completed, archived)
**Conclusion:** ASTM ingestion is plugin-only; no non-plugin fallback exists in
main repo.

---

## 1. ASTM Entry Points Audited

| Entry Point                    | Class / Location                                                                                                                                                        | Reader Used                                                           | Plugin Selection                                                                                                                             |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| **HTTP** `POST /analyzer/astm` | [AnalyzerImportController.java](../../../src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java) `doPost()` lines 89–121                  | `ASTMAnalyzerReader` via `AnalyzerReaderFactory.getReaderFor("astm")` | `setInserterResponder()` during `readStream()` — iterates `PluginAnalyzerService.getAnalyzerPlugins()`, first `isTargetAnalyzer(lines)` wins |
| **Serial** (RS232)             | [SerialAnalyzerReader.java](../../../src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/SerialAnalyzerReader.java) `readFromSerialPort()` → `readStream()` | `SerialAnalyzerReader` (extends same flow)                            | Same: `setInserterResponder()` during `readStream()`, plugin loop only                                                                       |
| **Error reprocessing**         | [AnalyzerReprocessingServiceImpl.java](../../../src/main/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingServiceImpl.java) `reprocessMessage()`            | `ASTMAnalyzerReader` via `AnalyzerReaderFactory.getReaderFor("astm")` | Same plugin loop via `readStream()`                                                                                                          |

**Reader factory:**
[AnalyzerReaderFactory.java](../../../src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/AnalyzerReaderFactory.java)
— `getReaderFor("astm")` returns **only** `new ASTMAnalyzerReader()`. The
file-upload path (`getReaderFor(file.getOriginalFilename())`) is used for
`/importAnalyzer`; when the client posts to `/analyzer/astm`, the controller
passes the literal `"astm"`, so ASTM content never goes through
`AnalyzerLineReader`.

---

## 2. Confirmation: No ASTM Non-Plugin Fallback

- **ASTMAnalyzerReader:** After reading lines, calls `setInserterResponder()`.
  If no plugin matches, `inserter` remains null and `readStream()` returns false
  with error `"Unable to understand which analyzer sent the message"`. There is
  no fallback to a built-in ASTM inserter.
- **SerialAnalyzerReader:** Same logic: `setInserterResponder()` only; no
  fallback.
- **AnalyzerLineReader** (file upload): Contains hardcoded fallbacks (e.g.
  `CobasReader`, `EvolisReader`, `SysmexReader`) for **flat file** content. This
  reader is only used when the request goes to `/importAnalyzer` with a filename
  that is not `"astm"`, `"hl7"`, or `.xls`. ASTM HTTP and reprocessing never use
  `AnalyzerLineReader`.

**Conclusion:** There is **no ASTM-specific non-plugin fallback** in the main
repository. ASTM ingestion is fully plugin-dispatched.

**Phase 3 (2026-02-03):** ASTM readers aligned with HL7: `readStream()` only
parses lines; plugin matching happens in `processData()` /
`insertAnalyzerData()`. Unknown analyzer returns HTTP 400 with error body.

---

## 3. Per-Analyzer Selection Model (Documentation)

### Generic path (dashboard-configured)

- **Condition:** `analyzer_configuration.is_generic_plugin = true` and
  `identifier_pattern` is non-null.
- **DAO:**
  [AnalyzerConfigurationDAOImpl.findGenericPluginConfigsWithPatterns()](../../../src/main/java/org/openelisglobal/analyzer/dao/AnalyzerConfigurationDAOImpl.java)
  — HQL: `genericPlugin = true AND identifierPattern IS NOT NULL`.
- **Matching:**
  [AnalyzerConfigurationServiceImpl.findByIdentifierPatternMatch(String)](../../../src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationServiceImpl.java)
  compiles each config’s `identifier_pattern` as a Java regex and uses
  `matcher(identifier).find()` (substring match). First matching config is
  returned.
- **ASTM identifier:** Extracted from ASTM H-segment **H-5** (1-based field 5;
  `fields[4]` after `line.split("|")`), i.e. manufacturer^model^version (e.g.
  `H|^~\&|...|MINDRAY^BA-88A^1.0|...`). See
  `GenericASTMAnalyzer.parseAnalyzerIdentifier()`.
- **Result:** GenericASTM plugin’s `isTargetAnalyzer(lines)` returns true; it
  provides the inserter for that analyzer.

### Specific (legacy) plugin path

- **Condition:** Either no `analyzer_configuration` row for that analyzer, or
  `is_generic_plugin = false` (and the analyzer is matched by a dedicated
  plugin's hardcoded logic).
- **Behavior:** A legacy plugin (e.g. HoribaPentra60) implements
  `isTargetAnalyzer(lines)` with fixed checks (e.g. "PENTRA60" in H-segment).
  Plugins are evaluated in registration order; first match wins.
- **Result:** That plugin’s inserter is used; GenericASTM is not consulted for
  that message if the legacy plugin matches first.

### Conflict / priority

- If both a legacy plugin and GenericASTM could match the same message, the
  **legacy plugin wins** because it appears earlier in `getAnalyzerPlugins()`.
  To use GenericASTM for that analyzer, the legacy plugin JAR must be removed or
  the registration order changed.

---

## 4. References

- Plan: astm-genericplugin-audit-2026-02-03 (completed, archived)
- ASTM reader:
  [ASTMAnalyzerReader.java](../../../src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java)
  — `setInserterResponder()` (lines 106–120)
- Serial reader:
  [SerialAnalyzerReader.java](../../../src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/SerialAnalyzerReader.java)
  — `setInserterResponder()` (lines 188–211)
- Pattern matching:
  [AnalyzerConfigurationServiceImpl.findByIdentifierPatternMatch](../../../src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationServiceImpl.java)
  (lines 97–128)
- Generic configs query:
  [AnalyzerConfigurationDAOImpl.findGenericPluginConfigsWithPatterns](../../../src/main/java/org/openelisglobal/analyzer/dao/AnalyzerConfigurationDAOImpl.java)
  (lines 102–108)
