# Alignment Report: Feature 012 vs 011 + Plugin/Harness Reality

**Date**: 2026-02-27  
**Scope**: Compare `specs/012-generic-astm-plugin-profiles/plan.md` and
`tasks.md` against 011 defaults semantics, plugin architecture, harness
infrastructure, and GeneXpert bidirectional MVP requirements.

---

## 1. Profiles == Analyzer Default Config Files

### Current State (011)

- **Canonical configs** live in `projects/analyzer-defaults/{astm,hl7}/*.json`
  (6 ASTM + 5 HL7).
- **Defaults API** already exists:
  - `GET /rest/analyzer/defaults` — lists all defaults from filesystem.
  - `GET /rest/analyzer/defaults/{protocol}/{name}` — loads a specific config.
- **Frontend handler** (`AnalyzerForm.jsx` `handleDefaultConfigSelect()`) sets
  only plugin-level fields (`identifierPattern`, `analyzerType`,
  `protocolVersion`, `pluginTypeId`); it does NOT set instance-level fields
  (name, IP, port).
- **Directory resolution**: `ANALYZER_DEFAULTS_DIR` env var, defaults to
  `/data/analyzer-defaults`.

### 012 Divergence

The 012 plan/tasks treat profiles as a wholly new artifact system with new DB
tables, import/export APIs, and a profile library concept. This is correct for
the _site library_ and _community import_ use cases, but it under-acknowledges
that **built-in profiles are sourced from the same filesystem configs** that 011
already uses.

### Required Edits

| Artifact        | Change                                                                                                                                                                                                                                                                                                            |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `plan.md`       | Add explicit statement: built-in profiles are sourced from `projects/analyzer-defaults/` (to be renamed `projects/analyzer-profiles/`), and profile IDs map 1:1 to filenames.                                                                                                                                     |
| `tasks.md` T024 | Currently references 7 profiles but `projects/analyzer-defaults/astm/` only has 6 (genexpert, horiba-micros60, horiba-pentra60, mindray-ba88a, stago-start4, sysmex-xn). Missing: cobas-6800-roche, m2000-abbott, panther-hologic, celldyn-ruby-abbott. Either create the missing JSON files or adjust the count. |
| `plan.md`       | The defaults API (`GET /rest/analyzer/defaults`) must remain operational (backward compat) even after profiles rename.                                                                                                                                                                                            |

---

## 2. Bidirectional GeneXpert ASTM — Missing from 012

### Current State (011 + Plugin Architecture)

The bidirectional flow is already **architecturally supported** in the codebase:

- `ASTMAnalyzerReader.processData()` branches on
  `plugin.isAnalyzerResult(lines)`:
  - `true` → `insertAnalyzerData()` (results push ingest).
  - `false` → `buildResponseForQuery()` → `responder.buildResponse(lines)`
    (query response).
- `AnalyzerImportController.doPost()` returns `reader.getResponse()` if
  `reader.hasResponse()`.
- Bridge routing via `testConnectionViaBridge()` sends HTTP POST to bridge with
  `forwardAddress`/`forwardPort` query params → bridge opens TCP to analyzer.

However, **no Generic ASTM plugin currently implements Q-segment query
handling**. The default `isAnalyzerResult()` returns `true` for all messages,
and no `AnalyzerResponder` exists for the Generic ASTM stack.

### 012 Divergence

Current milestones focus on profile library + ASTM config overlays + simulator
preview. None of M1–M4 define or test the bidirectional order/query flows:

- **Results PUSH** (Analyzer → OE): already works, tested by
  `test-genexpert-astm.sh`.
- **Orders PULL** (Analyzer queries OE for orders): no implementation, no tests.
- **Orders PUSH** (OE sends orders to Analyzer): no implementation, no tests.
- **Results PULL** (OE queries Analyzer for results): no implementation, no
  tests.

### Required Edits

| Artifact     | Change                                                                                                                                                                                 |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `plan.md`    | Add new milestone M5 (or redefine M4): **Bidirectional GeneXpert ASTM gate** covering all 4 pathways.                                                                                  |
| `plan.md`    | Change "MVP First (M1 Only)" to include bidirectional gate as MVP requirement.                                                                                                         |
| `tasks.md`   | Add tasks for: Q-segment responder, order message builder, results query service, mock server Q-segment support, harness scripts for all 4 pathways, real-device validation checklist. |
| `contracts/` | Add bidirectional endpoints: `POST /analyzers/{id}/send-order`, `POST /analyzers/{id}/query-results`.                                                                                  |

---

## 3. Mock Server Q-Segment Gap

### Current State

`tools/analyzer-mock-server/server.py`:

- `Q|` records are processed as **QC records** (`_process_qc()`), not ASTM query
  segments.
- Query detection (`_is_field_query()`) uses a heuristic: "H record present, no
  P or O records." This detects connection-test pings but does NOT detect ASTM
  Q-segment queries.
- The mock can push ASTM messages (results-push) but cannot respond to Q-segment
  queries with P/O/R segments (needed for orders-pull and results-pull testing).

### 011 Documentation

The 011 architecture report
(`specs/011-madagascar-analyzer-integration/research/analyzer-plugin-architecture-report.md`)
explicitly documents Q-segment query format:

```
Q|1|SampleID^PatientID||ALL||||||||
```

And response modes: P + O segments for order queries, P + O + R for result
queries.

### Required Edits

| Artifact   | Change                                                                                                                         |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `tasks.md` | Add tasks for mock server Q-segment support: distinguish `Q\|` as ASTM query (not QC), respond with P/O/R based on query type. |
| `tasks.md` | Add harness scripts: `test-genexpert-orders-pull.sh`, `test-genexpert-orders-push.sh`, `test-genexpert-results-pull.sh`.       |

---

## 4. Profile Count Mismatch

### Current Files in `projects/analyzer-defaults/astm/`

1. `genexpert-astm.json`
2. `horiba-micros60.json`
3. `horiba-pentra60.json`
4. `mindray-ba88a.json`
5. `stago-start4.json`
6. `sysmex-xn.json`

### Spec Claims 7 Built-in ASTM Profiles

The spec (FR-023) lists:

1. genexpert-cepheid-astm
2. cobas-6800-roche-astm
3. m2000-abbott-astm
4. panther-hologic-astm
5. celldyn-ruby-abbott-astm
6. sysmex-xn-astm
7. mindray-bc-astm

### Gap

Four profiles from the spec don't exist as defaults files. Two existing defaults
(horiba-micros60, horiba-pentra60, stago-start4) are not in the spec's list.

### Resolution

Profile creation for the 4 missing analyzers is a separate task. For MVP, T024
should reference only the profiles that actually exist as source files. The
existing Horiba and Stago defaults should also be included as built-in profiles
(they are valid analyzer configs even if the spec's initial list didn't
enumerate them).

---

## 5. Real Device Validation — Missing

### Current State

- M4 description mentions "real GeneXpert optional gate" but provides no
  checklist, no expected evidence, and no commands for the 4 bidirectional
  pathways.
- Existing Playwright test `analyzer-test-connection.spec.ts` verifies bridge
  connectivity to a real/mock GeneXpert but does not test data exchange.

### Required Edits

| Artifact   | Change                                                                                                                                                           |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| New file   | Create `specs/012-generic-astm-plugin-profiles/checklists/real-device-validation.md` with per-pathway gates, preconditions, commands, and evidence requirements. |
| `tasks.md` | Add real-device validation tasks referencing the checklist.                                                                                                      |

---

## 6. API Contract Gaps

### Current Contract Coverage

`contracts/analyzer-profiles-api.yaml` covers:

- Profile library CRUD
- Profile apply/export
- ASTM config CRUD
- QC rules CRUD
- Test mapping config CRUD
- Pending codes
- Preview mapping
- Lab units

### Missing

- `POST /analyzers/{id}/send-order` — send ASTM order message via bridge.
- `POST /analyzers/{id}/query-results` — send ASTM Q-segment query via bridge
  and ingest response.

---

## Summary of Required Changes

1. **plan.md**: Add bidirectional milestone, update MVP definition, document
   profiles-as-defaults alignment.
2. **tasks.md**: Add ~15 new tasks for bidirectional flows, mock Q-segment,
   harness scripts, real-device gates.
3. **contracts/**: Add 2 bidirectional endpoints.
4. **New checklist**: Real-device validation for all 4 pathways.
5. **Rename strategy**: Document `projects/analyzer-defaults/` →
   `projects/analyzer-profiles/` migration.
