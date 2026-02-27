# GeneXpert ASTM E2E Readiness: Fixture Cleanup & Plugin Alignment

## Context

We've completed the mock server ASTM remediation (PR #13 submodule, PR #2942
parent). The mock now generates spec-compliant GeneXpert ASTM messages. **This
plan focuses on getting the receiving side (GenericASTM plugin + test fixtures)
ready for E2E testing with a single GeneXpert ASTM configuration.**

The current test fixtures load 12 analyzers (IDs 2000-2012), most linked to
legacy plugins. Only 4 use generic plugins. **No GeneXpert ASTM entry exists** —
ID 2002 is a FILE-based GeneXpert linked to the legacy `GeneXpertAnalyzer`.

### Critical Schema Discovery

**Validation against the codebase revealed a fundamental fixture/runtime
mismatch:**

The XML fixtures insert `identifier_pattern` and `is_generic_plugin` into an
`analyzer_configuration` table. However, the **runtime code does NOT read from
this table**:

- `identifier_pattern` is read from **`analyzer.identifier_pattern`** (added by
  Liquibase `3.4.x.x/002-modify-analyzer-table.xml:57-82`)
- `is_generic_plugin` is read from **`analyzer_type.is_generic_plugin`** (in
  `3.4.x.x/001-create-standalone-tables.xml:18-41`)
- The DAO query `findGenericAnalyzersWithPatterns()` (in
  `AnalyzerDAOImpl.java:71-76`) joins `Analyzer a → AnalyzerType at` and reads
  `a.identifierPattern` where `at.genericPlugin = true`

This means **the current fixtures will not enable runtime pattern matching** —
the `identifier_pattern` values are stored in the wrong table. The `analyzer`
rows themselves need the `identifier_pattern` column populated.

Additional corrections:

- Test mapping table is **`analyzer_test_map`** (not `analyzer_test_mapping`) —
  entity: `AnalyzerTestMapping.java:32` → `@Table(name = "analyzer_test_map")`
- Defaults JSONs are loaded **on-demand via REST API** during analyzer creation
  (`AnalyzerRestController.java:216`), NOT auto-seeded at startup
- `analyzer_configuration` may serve as config storage for protocol metadata
  (port, IP), but `identifier_pattern` and `is_generic_plugin` on it are **not
  used by the runtime pattern matching code**

---

## Phase 0: Archive Plan

Save this plan to `specs/011-madagascar-analyzer-integration/plans/` for
archival alongside existing plans.

**File to create:**

- `specs/011-madagascar-analyzer-integration/plans/genexpert-astm-e2e-readiness.md`
  — copy of this plan

---

## Phase 1: Fixture Cleanup — Generic Plugins Only

**Goal**: Strip `madagascar-analyzer-test-data.xml` to only include analyzers
that use GenericASTM or GenericHL7 plugins, and fix the schema mismatch so
`identifier_pattern` is on the `analyzer` rows where the runtime expects it.

### Analyzers to KEEP (4 existing + 1 new):

| ID       | Name                         | Plugin          | Protocol           | `identifier_pattern`        |
| -------- | ---------------------------- | --------------- | ------------------ | --------------------------- |
| 2006     | Mindray BA-88A               | GenericASTM     | ASTM LIS2-A2       | `MINDRAY.*BA-88A\|BA88A`    |
| 2007     | Mindray BC-5380              | GenericHL7      | HL7 v2.3.1         | `MINDRAY.*BC.?5380\|BC5380` |
| 2008     | Mindray BS-360E              | GenericHL7      | HL7 v2.3.1         | `MINDRAY.*BS.?360E\|BS360E` |
| 2012     | Mindray BC2000               | GenericHL7      | HL7 v2.3.1         | `MINDRAY.*BC.?2000`         |
| **2013** | **Cepheid GeneXpert (ASTM)** | **GenericASTM** | **ASTM E-1394-97** | **`GENEXPERT\|CEPHEID`**    |

### Analyzers to REMOVE (8 legacy-plugin analyzers):

2000 (Abbott Architect), 2002 (GeneXpert FILE), 2003 (Hain FluoroCycler), 2004
(Horiba Micros 60), 2005 (Horiba Pentra 60), 2009 (QuantStudio 7), 2010 (Stago
STart 4), 2011 (Sysmex XN)

### Associated configs to REMOVE:

- All `serial_port_configuration` rows (SERIAL-2004..2011)
- All `file_import_configuration` rows (FILE-2002, FILE-2003, FILE-2009)
- `analyzer_configuration` rows for removed analyzers (CONFIG-2000..2005,
  CONFIG-2009..2011)

### Critical fix: Move `identifier_pattern` to `analyzer` rows

The `analyzer` XML rows must include `identifier_pattern` directly (not just on
`analyzer_configuration`). This is where the runtime code reads it from:

```xml
<!-- ID 2006: Mindray BA-88A (GenericASTM) -->
<analyzer id="2006" name="Mindray BA-88A"
    analyzer_type="CHEMISTRY" DESCRIPTION="ASTM over RS232 Serial"
    identifier_pattern="MINDRAY.*BA-88A|BA88A"
    is_active="true" last_updated="2026-02-02 00:00:00" />

<!-- ID 2013: GeneXpert ASTM Mode (GenericASTM) - NEW -->
<analyzer id="2013" name="Cepheid GeneXpert (ASTM Mode)"
    analyzer_type="MOLECULAR" DESCRIPTION="ASTM E-1394-97 over TCP/IP"
    identifier_pattern="GENEXPERT|CEPHEID"
    is_active="true" last_updated="2026-02-02 00:00:00" />
```

Similarly for HL7 analyzers (2007, 2008, 2012) — add `identifier_pattern` to
their `<analyzer>` rows.

### Keep `analyzer_configuration` for protocol metadata only

The `analyzer_configuration` rows can stay for storing protocol version, IP,
port, etc., but **remove `identifier_pattern` and `is_generic_plugin` from
them** since the runtime doesn't read these from `analyzer_configuration`:

```xml
<analyzer_configuration id="CONFIG-2013"
    analyzer_id="2013" protocol_version="ASTM E-1394-97"
    status="ACTIVE" sys_user_id="1" last_updated="2026-02-02 00:00:00" />
```

### Fix `LINK_ANALYZER_TYPES_SQL` in `load-test-fixtures.sh`

Replace the current legacy-only links with generic plugin links. The class names
are confirmed from `PluginRegistryService.java` constants:

```sql
SET search_path TO clinlims;
-- Link GenericASTM analyzers to their AnalyzerType
-- (AnalyzerType auto-created at startup by PluginRegistryService)
UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type
  WHERE plugin_class_name =
    'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer'
  LIMIT 1
) WHERE id IN (2006, 2013) AND analyzer_type_id IS NULL;

-- Link GenericHL7 analyzers to their AnalyzerType
UPDATE analyzer SET analyzer_type_id = (
  SELECT id FROM analyzer_type
  WHERE plugin_class_name =
    'org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer'
  LIMIT 1
) WHERE id IN (2007, 2008, 2012) AND analyzer_type_id IS NULL;
```

### Add `analyzer_test_map` rows for GeneXpert

The table `analyzer_test_map` (composite PK: `analyzer_id` +
`analyzer_test_name`) maps analyzer test codes to OpenELIS test IDs. These must
be added to the XML fixture for the GeneXpert analyzer:

```xml
<!-- Test mappings for GeneXpert ASTM (ID 2013) -->
<!-- analyzer_test_name must match what the plugin extracts from R.3 component 4 -->
<analyzer_test_map analyzer_id="2013" analyzer_test_name="MTB-RIF"
    test_id="[TB DNA test ID]" last_updated="2026-02-02 00:00:00" />
<analyzer_test_map analyzer_id="2013" analyzer_test_name="RIF"
    test_id="[RIF resistance test ID]" last_updated="2026-02-02 00:00:00" />
<analyzer_test_map analyzer_id="2013" analyzer_test_name="HIV-VL"
    test_id="[HIV-1 RNA test ID]" last_updated="2026-02-02 00:00:00" />
<analyzer_test_map analyzer_id="2013" analyzer_test_name="COVID19"
    test_id="[SARS-CoV-2 test ID]" last_updated="2026-02-02 00:00:00" />
```

Note: `test_id` values must reference actual `test` rows in the database. We'll
need to look up the IDs for these tests in the running DB or create them in the
foundational fixtures if they don't exist.

### Files to modify:

- `src/test/resources/testdata/madagascar-analyzer-test-data.xml` — Remove 8
  legacy analyzers, add GeneXpert ASTM (ID 2013), move `identifier_pattern` to
  `analyzer` rows, add `analyzer_test_map` entries
- `src/test/resources/load-test-fixtures.sh` — Replace `LINK_ANALYZER_TYPES_SQL`
  with generic plugin links

---

## Phase 2: Defaults Code Alignment (F1 — CRITICAL)

**Problem**: Mock template codes (`MTB-RIF`, `HIV-VL`) don't match defaults
codes (`MTB`, `HIV`). The plugin extracts `MTB-RIF` from R.3 component 4
(`GenericASTMLineInserter.java:290-309`), but the defaults JSON has
`analyzer_code: "MTB"`.

Since defaults JSONs are loaded on-demand via the REST API
(`AnalyzerRestController.java:216`) when creating an analyzer through the
Dashboard, and our test fixtures bypass the Dashboard by inserting directly into
`analyzer_test_map`, **the defaults JSON alignment is important for production
use but doesn't directly affect E2E test fixtures**. However, they must still
match so the Dashboard flow works correctly.

**Fix**: Update `genexpert-astm.json` defaults to match the mock template's more
specific codes (per Cepheid spec):

| Current (defaults) | New (matches mock + Cepheid spec) |
| ------------------ | --------------------------------- |
| `MTB`              | `MTB-RIF`                         |
| `HIV`              | `HIV-VL`                          |
| `RIF`              | `RIF` (already aligned)           |
| `COVID19`          | `COVID19` (already aligned)       |

Also fix protocol version: `"LIS2-A2"` → `"E-1394-97"` (per Cepheid spec Rev E,
H.13 field).

### Files to modify:

- `projects/analyzer-defaults/astm/genexpert-astm.json` — Update `analyzer_code`
  values and `protocol.version`

---

## Phase 3: Plugin Fixes (GenericASTMLineInserter)

### 3a: QC Detection (F3 — HIGH)

**Problem**: `GenericASTMLineInserter.java:237` hardcodes
`analyzerResult.setIsControl(false)`. The O-record is stored as a raw string
(`orderRecord`, line 112-124) but O.12 (Action Code) is never parsed. Mock
correctly sends `O.12 = "Q"` for QC samples.

**Fix**:

- Add `private static final int O_ACTION_CODE_FIELD = 11;` (0-based index for
  ASTM field O.12)
- In `addResultFromLine()`, split `orderRecord` on `|` and check field 11
- When value is `"Q"`, call `analyzerResult.setIsControl(true)`

### 3b: Value Cleanup (F4 — HIGH)

**Problem**: GeneXpert qualitative R.4 = `NEGATIVE^` (trailing `^` per Cepheid
spec — value in component 1, empty component 2). Plugin stores raw
`resultFields[R_VALUE_FIELD]` at line 234, including the trailing `^`.
Downstream value matching against `"NEGATIVE"` fails.

Similarly, complementary results have R.4 = `^3.10` (leading `^` — value in
component 2). Plugin stores `"^3.10"` instead of `"3.10"`.

**Fix**: In `addResultFromLine()`, clean the R.4 value:

```java
String resultValue = resultFields[R_VALUE_FIELD];
if (resultValue != null) {
    // Strip trailing component delimiters (e.g., "NEGATIVE^" → "NEGATIVE")
    resultValue = resultValue.replaceAll("\\^+$", "");
    // Strip leading component delimiter (e.g., "^3.10" → "3.10")
    if (resultValue.startsWith("^")) {
        resultValue = resultValue.substring(1);
    }
}
```

### 3c: Add Tests

Add integration tests in `GenericASTMIntegrationTest.java`:

- Test QC detection: O.12="Q" → `isControl=true`
- Test qualitative cleanup: `NEGATIVE^` → `NEGATIVE`
- Test complementary cleanup: `^3.10` → `3.10`
- Test normal value passthrough: `25.5` → `25.5` (no change)

### Files to modify:

- `plugins/analyzers/GenericASTM/src/main/java/org/openelisglobal/plugins/analyzer/genericastm/GenericASTMLineInserter.java`
- `plugins/analyzers/GenericASTM/src/test/java/.../GenericASTMIntegrationTest.java`

---

## Phase 4: E2E Validation

1. `/restart-analyzer-harness --full-reset` (loads cleaned fixtures)
2. Verify only 5 analyzers loaded:
   ```sql
   SELECT id, name, identifier_pattern, analyzer_type_id
   FROM clinlims.analyzer WHERE id >= 2006;
   ```
3. Verify `analyzer_type_id` is NOT NULL for all 5 rows
4. Verify `identifier_pattern` is set on the `analyzer` rows (not just
   `analyzer_configuration`)
5. Verify `analyzer_test_map` has entries for analyzer 2013:
   ```sql
   SELECT * FROM clinlims.analyzer_test_map WHERE analyzer_id = 2013;
   ```
6. Push GeneXpert ASTM message through bridge → verify results in import queue
7. Verify qualitative values are clean (`NEGATIVE` not `NEGATIVE^`)
8. Verify QC results marked as controls (if Phase 3a implemented)

---

## Specification Analysis Report (from cross-reference validation)

| ID      | Category          | Severity     | Location(s)                            | Summary                                                                                      | Status             |
| ------- | ----------------- | ------------ | -------------------------------------- | -------------------------------------------------------------------------------------------- | ------------------ |
| F1      | Inconsistency     | **CRITICAL** | Mock template vs. defaults JSON        | Test code mismatch: mock sends `MTB-RIF` but defaults map `MTB`                              | Fix in Phase 2     |
| F2      | Inconsistency     | INFO         | Mock R.3 vs. plugin extraction         | Plugin returns `components[3]` — works for GeneXpert                                         | No fix needed      |
| F3      | Gap               | **HIGH**     | `GenericASTMLineInserter.java:237`     | QC always `isControl(false)`, O.12 ignored                                                   | Fix in Phase 3a    |
| F4      | Inconsistency     | **HIGH**     | Plugin value extraction                | Trailing `^` in qualitative R.4 stored raw                                                   | Fix in Phase 3b    |
| F5      | Inconsistency     | **MEDIUM**   | Plugin value extraction                | Leading `^` in complementary R.4 stored raw                                                  | Fix in Phase 3b    |
| F6      | Inconsistency     | **MEDIUM**   | `genexpert-astm.json` protocol version | `"LIS2-A2"` should be `"E-1394-97"`                                                          | Fix in Phase 2     |
| F7      | Gap               | **MEDIUM**   | Plugin timestamp field                 | R_TIMESTAMP_FIELD=9 doesn't match GeneXpert layout                                           | Accept for now     |
| F8      | Validation        | INFO         | H.5 identifier matching                | `GENEXPERT` matches via `Pattern.find()`                                                     | Confirmed working  |
| F9      | Validation        | INFO         | Multi-result O→R mapping               | Plugin tracks single orderRecord                                                             | Confirmed working  |
| F10     | Validation        | INFO         | O.3 specimen ID                        | Within 25-char spec limit                                                                    | Confirmed working  |
| **F11** | **Inconsistency** | **CRITICAL** | **Fixtures vs. runtime schema**        | **`identifier_pattern` on `analyzer_configuration` but runtime reads from `analyzer` table** | **Fix in Phase 1** |
| **F12** | **Inconsistency** | **HIGH**     | **`LINK_ANALYZER_TYPES_SQL`**          | **Links only legacy plugins (2002, 2009-2011), ignores generic plugins (2006-2008, 2012)**   | **Fix in Phase 1** |
| **F13** | **Gap**           | **HIGH**     | **`analyzer_test_map`**                | **No test mapping rows exist for any fixture analyzer**                                      | **Fix in Phase 1** |

---

## Files Reference

| File                                                                | Action                                                                                         | Phase |
| ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | ----- |
| `specs/011-.../plans/genexpert-astm-e2e-readiness.md`               | **Create** — Archive this plan                                                                 | P0    |
| `src/test/resources/testdata/madagascar-analyzer-test-data.xml`     | **Modify** — Strip to 5 analyzers, fix `identifier_pattern` placement, add `analyzer_test_map` | P1    |
| `src/test/resources/load-test-fixtures.sh`                          | **Modify** — Replace `LINK_ANALYZER_TYPES_SQL`                                                 | P1    |
| `projects/analyzer-defaults/astm/genexpert-astm.json`               | **Modify** — Fix test codes + protocol version                                                 | P2    |
| `plugins/analyzers/GenericASTM/.../GenericASTMLineInserter.java`    | **Modify** — QC detection + value cleanup                                                      | P3    |
| `plugins/analyzers/GenericASTM/.../GenericASTMIntegrationTest.java` | **Modify** — Add tests                                                                         | P3    |

## Verification

1. **Regenerate SQL**:
   `python3 src/test/resources/testdata/xml-to-sql.py src/test/resources/testdata/madagascar-analyzer-test-data.xml src/test/resources/testdata/analyzer-e2e.generated.sql --on-conflict-do-nothing`
   — verify only 5 analyzers, `identifier_pattern` on `analyzer` rows
2. **Mock tests**:
   `cd tools/analyzer-mock-server && python -m pytest test_protocols.py -v` —
   all pass
3. **Plugin unit tests**: `cd plugins && mvn test` — GenericASTM tests pass
   (including new QC + value tests)
4. **E2E harness**: `/restart-analyzer-harness --full-reset`, push GeneXpert
   message, verify mapped results appear
5. **DB verification**: Query `analyzer`, `analyzer_type`, `analyzer_test_map`
   to confirm linkage
