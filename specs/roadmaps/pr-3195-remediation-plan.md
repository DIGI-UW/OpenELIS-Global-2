# Audit Remediation + Spec Accuracy + Unified Bridge Interface

**Date**: 2026-03-27 **PRs**: Mock #25, Bridge #25, Main #3195

---

## Inventory: What's Done

### Submodule code fixes — DONE (committed)

**Mock server** — committed + pushed, CI green:

- M1-M6, M10: code fixes
- M3-M4: MLLP robustness (persistent connections, pipelined frames)
- M7: 8 MLLP listener tests, M11: 17 /analyzers API tests (81 total passing)

**Bridge** — committed, push pending:

- B2-B3, B6-B7, B10, B17: bug + correctness fixes
- B8, B11-B13, B19: nitpick fixes
- B9: 10 connectivity tests, B15: 10 syncAll tests (all passing)

### Main repo edits — IN PROGRESS (uncommitted)

**Already applied to working tree:**

- [x] C2: fixture path fix in `push-analyzer-result.ts` (../fixtures)
- [x] C3: `copyFileSync` instead of binary-corrupting append
- [x] C4: FILE analyzers uncommented in `analyzer-demo-flow.spec.ts` with
      correct paths
- [x] C3b: host mount paths (REPO_ROOT-based)
- [x] C3c: `fileSampleId` for FILE protocol (no null assertion)
- [x] C10: step numbering fix (`!== "FILE"` not `=== "ASTM"`)
- [x] C5: deleted `hl7-mindray-results.spec.ts`, removed from playwright config
- [x] C8: Javadoc updated to match implementation
- [x] C9: empty-string communicationMode guard
- [x] C1: `getAcceptedIssuers()` null→empty array (both files)
- [x] C6: ASTM_PORT_TEMPLATES quoted
- [x] C7: docker.sock TEST/HARNESS ONLY comment
- [x] `fileSampleId` field added to `AnalyzerTestConfig` type

**NOT yet done:**

- [ ] Commit + format main repo changes
- [ ] Push bridge submodule
- [ ] Push main repo
- [ ] Reply & resolve 15 threads on PR #3195
- [x] Reply & resolve threads on mock #25 (user resolved)
- [x] Reply & resolve threads on bridge #25 (user resolved)

---

## The Architecture Problem: Two Input Paths

Currently OE has **two completely different pipelines** for receiving analyzer
results:

### Path 1: ASTM/HL7 (bridge-mediated, structured)

```
Analyzer → Bridge /input → protocol detect → normalize →
  POST /analyzer/{protocol} (raw message) → OE AnalyzerReader → DB
```

- Bridge receives raw protocol stream
- Bridge forwards raw message text to OE
- OE parses with protocol-specific `AnalyzerReader` (ASTMAnalyzerReader,
  HL7AnalyzerReader)
- Headers carry analyzer ID, source IP

### Path 2: FILE (bridge-mediated, binary blob)

```
File on disk → Bridge FileWatcher → multipart POST /rest/analyzers/{id}/import →
  OE writes temp file → FileImportService.processFile() → AnalyzerReader → DB
```

- Bridge detects file, reads bytes, POSTs as multipart to OE
- OE receives binary blob, writes to temp staging
- OE parses with format-specific reader (ExcelAnalyzerReader, CSVAnalyzerReader)
- OE manages FileImportConfiguration (column mappings, format, archive dirs)

### Why this is a problem

- OE has format-specific knowledge it shouldn't need (xlsx parsing, CSV column
  mappings)
- Two separate OE controllers, services, and configuration systems
- FILE path has its own config entity (FileImportConfiguration) with column
  mappings, archive dirs, etc. — all concerns that belong in the bridge
- Can't add a new file format without touching OE
- Testing requires different paths for different protocols

### Target: Single unified FHIR R4 interface

```
Any Analyzer → Bridge (handles ALL protocol/format specifics) →
  POST /fhir (FHIR R4 transaction Bundle) → OE inserts → DB
```

- Bridge owns ALL parsing: ASTM, HL7, CSV, Excel, XML
- Bridge delivers a **FHIR R4 transaction Bundle** with DiagnosticReport +
  Observations:

```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "fullUrl": "urn:uuid:report-1",
      "resource": {
        "resourceType": "DiagnosticReport",
        "status": "preliminary",
        "category": [
          {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/v2-0074",
                "code": "LAB"
              }
            ]
          }
        ],
        "code": {
          "coding": [
            {
              "system": "http://loinc.org",
              "code": "58410-2",
              "display": "CBC panel"
            }
          ]
        },
        "subject": { "identifier": { "value": "HARN-QS7-2026-00001" } },
        "specimen": [{ "reference": "urn:uuid:specimen-1" }],
        "result": [
          { "reference": "urn:uuid:obs-wbc" },
          { "reference": "urn:uuid:obs-rbc" }
        ]
      },
      "request": { "method": "POST", "url": "DiagnosticReport" }
    },
    {
      "fullUrl": "urn:uuid:specimen-1",
      "resource": {
        "resourceType": "Specimen",
        "identifier": [{ "value": "HARN-QS7-2026-00001" }],
        "type": { "text": "Blood" }
      },
      "request": { "method": "POST", "url": "Specimen" }
    },
    {
      "fullUrl": "urn:uuid:obs-wbc",
      "resource": {
        "resourceType": "Observation",
        "status": "preliminary",
        "category": [
          {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                "code": "laboratory"
              }
            ]
          }
        ],
        "code": {
          "coding": [{ "code": "WBC", "display": "White Blood Cells" }]
        },
        "valueQuantity": { "value": 7.5, "unit": "10*3/uL" },
        "specimen": { "reference": "urn:uuid:specimen-1" }
      },
      "request": { "method": "POST", "url": "Observation" }
    }
  ]
}
```

**FHIR R4 resource mapping:**

- **DiagnosticReport** — groups results per analyzer run; `status: preliminary`
  (pending review)
- **Observation** — one per test result; `valueQuantity` for numeric,
  `valueString` for text, `valueCodeableConcept` for coded (pos/neg)
- **Specimen** — carries the accession/sample ID as `identifier`
- **Bundle** type `transaction` — atomic insert of all resources

**Why FHIR, not custom JSON:**

- OE already has HAPI FHIR infrastructure (`FhirTransformService`,
  `ObservationProvider`, etc.)
- Constitution mandates FHIR R4 for external-facing entities
- Enables interoperability — bridge could forward to other LIS systems
- Standard resource validation via HAPI FHIR parser

**OE has ONE endpoint, ONE processing path:**

- `POST /fhir` accepts the transaction Bundle
- Maps DiagnosticReport → AnalyzerResults staging table
- Maps each Observation → individual result row
- Maps Specimen.identifier → accession number
- Reuses existing FHIR processing pipeline

---

## Phase 3A: Immediate — Commit current fixes + reply to PR comments

### Remaining steps for this session:

1. Verify all working tree edits compile (backend + TS)
2. Run `mvn spotless:apply` + `cd frontend && npm run format`
3. Commit main repo changes
4. Push bridge submodule
5. Push main repo
6. Reply & resolve all PR comment threads (3 PRs)

### Commit scope:

- `push-analyzer-result.ts` — fixture path + copyFileSync
- `analyzer-demo-flow.spec.ts` — FILE analyzers restored, step numbering,
  fileSampleId
- `analyzer-test-config.ts` — fileSampleId field
- `hl7-mindray-results.spec.ts` — DELETED
- `playwright.config.ts` — removed stale entry
- `AnalyzerRestController.java` — Javadoc + empty-string guard
- `BridgeRegistrationService.java` — null→empty array
- `AnalyzerQueryServiceImpl.java` — null→empty array
- `docker-compose.analyzer-test.yml` — quoting + comment

---

## Phase 3B: Unified bridge interface (follow-up, separate PR)

This is the structural refactor to give OE a single standard interface for all
analyzer results regardless of protocol or transport.

### Step 1: Bridge parses FILE formats

Move Excel/CSV parsing from OE into bridge. The bridge already has access to the
file content — instead of forwarding raw bytes, it should:

1. Read the file (xlsx/xls/csv)
2. Apply the column mapping configuration (currently in OE's
   FileImportConfiguration)
3. Extract structured results: `[{sampleId, testCode, result, unit, timestamp}]`
4. POST structured JSON to OE (same format as ASTM/HL7 results)

**Bridge changes:**

- Add Apache POI or similar for xlsx/xls parsing (bridge is already Java/Spring
  Boot)
- Move `FileImportConfiguration` column mapping logic to bridge
- `FileMessageHandler.processFile()` → parse file → POST JSON results

**OE changes needed: none for this step** — bridge still calls an OE endpoint,
just a different one with structured data instead of raw bytes.

### Step 2: Unify the OE receiving endpoint

Leverage OE's existing FHIR infrastructure to accept the transaction Bundle:

**Existing OE FHIR infrastructure to reuse:**

- `FhirTransformService` / `FhirTransformServiceImpl` — already maps FHIR↔OE
  entities
- `ObservationProvider` — HAPI FHIR resource provider for Observations
- HAPI FHIR server endpoint (likely at `/fhir`)

**OE changes:**

- Add handler in FHIR transaction processing for `DiagnosticReport` +
  `Observation` Bundles from the bridge (source identified by `Meta.source` or a
  custom extension)
- Map `DiagnosticReport` → `AnalyzerResults` staging entry
- Map each `Observation` → individual result row (`testCode` from `code.coding`,
  value from `valueQuantity`/`valueString`/`valueCodeableConcept`)
- Map `Specimen.identifier` → accession number
- Status flow: `preliminary` → staged for review; `final` → auto-accepted
- Deprecate (don't remove yet) the old three endpoints:
  - `POST /analyzer/hl7`
  - `POST /analyzer/astm`
  - `POST /rest/analyzers/{id}/import`

### Step 3: Bridge normalizes ASTM/HL7 to FHIR too

Instead of forwarding raw ASTM/HL7 text, bridge parses them and sends the same
FHIR R4 transaction Bundle:

**Bridge changes:**

- ASTM: parse records → build DiagnosticReport + Observation Bundle
- HL7: parse OBR/OBX segments → build DiagnosticReport + Observation Bundle
  (bridge already has HAPI HL7 v2 parser — extend to produce FHIR output)
- POST to OE `/fhir` with same Bundle format as FILE results

### Step 4: Remove OE format-specific code

Once bridge handles all parsing:

- Remove `ASTMAnalyzerReader`, `HL7AnalyzerReader`, `ExcelAnalyzerReader` from
  OE
- Remove `FileImportConfiguration` entity from OE
- Remove `FileImportService`, `AnalyzerUploadRestController`
- Remove old `/analyzer/hl7`, `/analyzer/astm`, `/rest/analyzers/{id}/import`
  endpoints
- OE becomes format-agnostic — it only receives structured results

### Migration path

- Steps 1-2 can ship together (bridge parses files, OE has new endpoint)
- Step 3 is incremental (bridge parses ASTM/HL7 too)
- Step 4 is cleanup (remove old OE code once bridge handles everything)
- Old endpoints stay alive during transition (backward compat)

### Impact on E2E tests

Once bridge parses files and sends FHIR Bundles, the E2E test for FILE analyzers
becomes:

1. POST fixture to mock's `/simulate/file/{template}` with `target_dir`
2. Mock writes to import directory
3. Bridge detects → parses xlsx → builds FHIR Bundle → POSTs to OE `/fhir`
4. Test verifies results in UI using sampleId from mock response

This eliminates:

- Static fixture files checked into repo
- `fileSampleId` hardcoding
- The fixture path resolution problem entirely
- OE's format-specific parsing code (ExcelAnalyzerReader, CSVAnalyzerReader)

**Note:** Phase 3B is a separate PR from the current work. Current PR (#3195)
ships with the existing dual-path architecture. Phase 3B unifies it.

---

## Phase 3A-2: Audit Findings Fix (this session, same commit or follow-up)

### Fix 1: JSON injection in String.format [MEDIUM/SECURITY]

Two places build JSON via `String.format` with unescaped user-controlled values:

**`AnalyzerRestController.java:~786-789`** — `callBridgeTestConnectivity()`

```java
String.format("{\"transport\":\"TCP\",\"host\":\"%s\",...}", host, port, protocol)
```

**`AnalyzerQueryServiceImpl.java:~260`** — `queryViaBridge()`

```java
String.format("{\"host\":\"%s\",\"port\":%d,...}", ipAddress, port, timeoutMs)
```

**Fix**: Replace both with `objectMapper.writeValueAsString(Map.of(...))`. The
hand-rolled `escapeJson()` helper (only escapes `\` and `"`) should be removed —
Jackson handles all edge cases.

### Fix 2: Swallowed exception [LOW]

**`BridgeRegistrationService.java:39`** — SSL context creation failure is
silent.

**Fix**: Add `log.warn("SSL context init failed, using default client", e)`

### Fix 3: TLS consolidation [HIGH — track, don't fix on this PR]

3 identical ~15-line trust-all TLS blocks in:

- `AnalyzerRestController.java:808-818`
- `AnalyzerQueryServiceImpl.java:273-284`
- `BridgeRegistrationService.java:28-38`

**Track for Phase 3B**: Extract to `BridgeSslUtil.createTrustAllContext()`. Add
`analyzer.bridge.tls.verify` property. Don't fix on this PR — it's a separate
concern and the pattern is pre-existing.

### Fix 4: Hardcoded English diagnostic messages [MEDIUM — policy decision]

8 instances in `AnalyzerRestController.java` + 2 in `TestConnectionModal.jsx`.
These are test-connection diagnostic messages consumed by the admin UI.

**Decision needed**: i18n keys or document as API-level diagnostic exception.
For now: document the exception in spec. These messages are operational
diagnostics, not end-user-facing strings. i18n can follow in a dedicated pass.

---

## Phase 3C: Spec + Roadmap Accuracy Updates

### Context

Deep audit revealed all 3 specs are out of sync with actual implementation. 012
is 60% accurate, 013 is 80%, 014 is 40%. Roadmap claims "WORKING" for analyzers
that only have profiles (no parser binding). Specs must reflect current reality,
not aspirational state.

### Spec 012 Updates

**File**: `specs/012-generic-astm-plugin-profiles/spec.md`

1. **Remove FILE profiles from ASTM scope** (lines ~234-240)

   - Change "16 files (6 ASTM + 5 HL7 + 5 FILE)" to "11 files (6 ASTM + 5 HL7)"
   - FILE profiles belong in 014 scope exclusively

2. **Add cross-cutting bridge requirements**

   - Document bridge state sync (`PUT /api/analyzers/sync`)
   - Document test-connection parity (currently only in 013)

3. **Clarify M1 exit criteria**
   - Note T018-T021 as incomplete with estimated effort
   - Note 0 integration tests for M1 features

**File**: `specs/012-generic-astm-plugin-profiles/tasks.md` 4. Update task
completion markers to match code reality

### Spec 013 Updates

**File**: `specs/013-hjra-hl7-stream-alignment/spec.md`

1. **Clarify branch mandate** — FR-016 says "no implementation on spec branch"
   but CommunicationMode, test-connectivity, Liquibase ARE implementation.
   Either relabel the branch or acknowledge the cross-cutting fix exception.

2. **Document bridge /api/query endpoint** — exists, used by OE, not in spec

3. **Document that AnalyzerQueryServiceImpl direct socket is GONE**
   - `queryAnalyzerASTM()` removed — fully migrated to bridge
   - Update any references to "Step 2: deferred migration"

**File**: `specs/013-hjra-hl7-stream-alignment/plan.md` 4. Update cross-cutting
fix section to reflect completion

### Spec 014 Updates

**File**: `specs/014-hjra-file-stream-alignment/spec.md`

1. **Correct architecture description**

   - Current: Bridge detects file → POSTs raw binary to OE → OE parses xlsx
   - Spec may imply bridge handles parsing — clarify OE still owns format
     parsing
   - Add note about Phase 3B direction (bridge parses, sends FHIR)

2. **Flag GenericFile plugin as NOT YET IMPLEMENTED**

   - Profiles exist but plugin doesn't
   - Spec should clearly mark M3 as the plugin milestone

3. **Add missing tasks.md** — currently doesn't exist

   - Derive from plan.md milestone breakdown (M1A-M4)

4. **Document FILE profiles belonging to 014** (moved from 012)

5. **Flag `fileFormat` schema as missing** — M1A blocker

### Roadmap Updates

**File**: `specs/roadmaps/madagascar-analyzer-roadmap.md`

1. **Fix "WORKING" status for FILE analyzers**

   - QuantStudio/FluoroCycler: "Profile created, OE parses xlsx directly,
     GenericFile plugin not yet built. E2E verified via existing
     ExcelAnalyzerReader."
   - Distinguish "working via legacy parser" vs "working via GenericFile plugin"

2. **Update remaining work**

   - PR #3195 items mostly done
   - Add: "Spec accuracy remediation" as immediate work
   - Add: "Phase 3B: Unified FHIR bridge interface" to Post-MVP

3. **Update "Current State" section**

   - AnalyzerQueryServiceImpl direct socket: REMOVED (not deferred)
   - FILE demo tests: RESTORED (not commented out)
   - Add PR comment remediation commit

4. **Add communication block gap** to remaining work
   - Only 5 of 12 ASTM/HL7 profiles have `communication` section
   - 0 FILE profiles have it (acceptable — FILE is always push)

**File**: `specs/roadmaps/oe-bridge-sync-dynamic-mock-plan.md`

5. **Update "Remaining" section**
   - Step 1 (sync): DONE (PUT /api/analyzers/sync + bootstrap)
   - Step 2 (query migration): DONE (not deferred — queryViaBridge implemented)
   - Step 3 (dynamic networks): DONE (mock /analyzers API)
   - Step 4 (dead code): DONE
   - Step 5 (E2E demos): IN PROGRESS (FILE tests restored)

---

## Execution Order (this session, all on PR #3195)

All changes go on the same branch/PR. Commit as needed, push once at the end.

| Step | Items                                                                   | Effort  |
| ---- | ----------------------------------------------------------------------- | ------- |
| 1    | Fix JSON injection (objectMapper)                                       | Small   |
| 2    | Fix swallowed exception (log.warn)                                      | Trivial |
| 3    | Update roadmaps (current state, FHIR direction)                         | Medium  |
| 4    | Update spec 013 (branch mandate, query migration done)                  | Small   |
| 5    | Update spec 012 (FILE scope out, bridge sync, task accuracy)            | Small   |
| 6    | Update spec 014 (architecture reality, GenericFile gap, FHIR direction) | Medium  |
| 7    | Commit all (audit fixes + spec updates)                                 |         |
| 8    | Reply & resolve PR #3195 comment threads                                |         |
| 9    | Push                                                                    |         |

## Verification

1. `mvn compile` — backend compiles after JSON injection fix
2. `mvn spotless:apply` — formatting clean
3. Specs and roadmaps internally consistent
4. No false claims about implementation state
5. Phase 3B direction documented but not committed to timeline
6. CI green after push
