# Plan: Bridge-OE Analyzer Registration + Demo Videos

## Context

Demo video work exposed that OE and the bridge have no registration protocol.
When an analyzer is created in OE's dashboard, the bridge doesn't learn about
it. This causes:

- No deterministic device identification (bridge can't tag traffic with OE
  analyzer ID)
- Routing falls to regex pattern matching (ambiguous when multiple analyzers
  share a pattern)
- Test mappings can't update because stale type-level entries block
  `autoCreateTestMappings()`
- E2E tests can't run idempotently

**Scope for this PR:** Bridge registration for results-in flow only. Not full
analyzer lifecycle stages — just enough for: create analyzer → register with
bridge → results arrive tagged with correct OE analyzer ID → show in staging
with correct test names → accept → AccessionResults.

## Architecture

### Current Flow (broken for multi-device)

```
Device → Bridge (no device context) → OE
  OE tries 4 strategies to identify which analyzer:
    Strategy 0: IP+port (works but bridge doesn't always have OE analyzer ID)
    Strategy 1: H-segment name (per-MODEL, not per-device)
    Strategy 2: IP only (non-deterministic if multiple analyzers share subnet)
    Strategy 3: identifier_pattern regex (first-match-wins, ambiguous)
```

### Target Flow (this PR)

```
1. Admin creates analyzer in OE dashboard (IP, port, transport type, profile)
2. OE calls bridge: POST /api/analyzers/register
   Body: { oeAnalyzerId: "123", transport: "TCP", ip: "192.168.1.10", port: 9600 }
   Or:   { oeAnalyzerId: "456", transport: "FILE", watchDir: "/data/imports/qs5/", filePattern: "*.xls" }
3. Bridge stores mapping: source → oeAnalyzerId
4. When device connects / file arrives:
   Bridge tags message: X-Analyzer-Id: "123"
   Forwards to OE
5. OE receives X-Analyzer-Id header → direct DB lookup by ID
   No pattern matching needed. Deterministic.
```

### What Bridge Needs Per Analyzer (minimum)

| Transport      | Bridge needs                        | From OE                                                       |
| -------------- | ----------------------------------- | ------------------------------------------------------------- |
| TCP (ASTM/HL7) | Expected source IP (+optional port) | `analyzer.ip_address`, `analyzer.port`                        |
| FILE           | Watch directory, file pattern       | `file_import_configuration.import_directory`, `.file_pattern` |
| Serial         | Serial port path                    | `serial_port_configuration.port_name`                         |

Plus for all: `analyzer.id` (the OE analyzer ID to stamp on forwarded messages).

---

## Implementation

### Part 1: Bridge — Analyzer Registration API

**Bridge repo:** `tools/openelis-analyzer-bridge`

Add a REST endpoint the bridge exposes for OE to call:

**POST /api/analyzers/register**

```json
{
  "oeAnalyzerId": "123",
  "transport": "TCP", // TCP | FILE | SERIAL
  "config": {
    "expectedIp": "192.168.1.10",
    "expectedPort": 9600
  }
}
```

Or for FILE:

```json
{
  "oeAnalyzerId": "456",
  "transport": "FILE",
  "config": {
    "watchDirectory": "/data/imports/qs5/",
    "filePattern": "*.xls"
  }
}
```

Response: `{ "registered": true, "oeAnalyzerId": "123" }`

**DELETE /api/analyzers/{oeAnalyzerId}** — unregister

**GET /api/analyzers** — list registered analyzers (for dashboard display)

The bridge stores these in an in-memory map (or simple persistence). When a TCP
connection arrives from a registered IP, or a file appears in a registered
directory, the bridge stamps the forwarded message with
`X-Analyzer-Id: {oeAnalyzerId}`.

### Part 2: OE — Call Bridge on Analyzer Create/Update

**File:**
`src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`

After saving an analyzer, if the analyzer has transport config (IP+port or file
import config), call the bridge registration API:

```java
// After analyzer save succeeds
if (analyzer.getIpAddress() != null) {
    bridgeRegistrationService.register(analyzer.getId(), "TCP",
        analyzer.getIpAddress(), analyzer.getPort());
}
// Or if file import config exists
Optional<FileImportConfiguration> fileConfig = fileImportService.getByAnalyzerId(analyzerId);
if (fileConfig.isPresent()) {
    bridgeRegistrationService.register(analyzer.getId(), "FILE",
        fileConfig.get().getImportDirectory(), fileConfig.get().getFilePattern());
}
```

**New service:** `BridgeRegistrationService` — makes HTTP calls to the bridge's
`/api/analyzers/register` endpoint. Uses the existing `ANALYZER_BRIDGE_URL` env
var for the bridge base URL.

### Part 3: OE — Use X-Analyzer-Id for Direct Lookup

**File:**
`src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java`

When `X-Analyzer-Id` header is present, skip the 4-strategy identification and
do a direct DB lookup:

```java
String analyzerId = request.getHeader("X-Analyzer-Id");
if (analyzerId != null) {
    // Direct lookup — deterministic, no ambiguity
    reader.setContextAnalyzerId(analyzerId);
}
```

This makes the existing 4-strategy chain a **fallback** for when the bridge
doesn't have a registration (backward compatibility).

### Part 4: `autoCreateTestMappings` — Update Stale Mappings

**File:**
`src/main/java/org/openelisglobal/analyzer/service/AnalyzerServiceImpl.java`

When a mapping for `(type_id, test_name)` already exists but the `test_id`
differs (e.g., stale mapping from old test catalog), update it instead of
skipping.

### Part 5: E2E Test Fixes

**All test files:**

- Clean stale E2E analyzers before creating new ones (API DELETE)
- Fill correct simulator IP (`172.21.1.100`) and port (`9600`) in form
- Add test-connection step (verify green)
- Fix `LabNo` selector for react-data-table (use `page.getByText()` not `tr`
  selector)
- Update hardcoded ID 2013 references in other tests

### Part 6: Generated SQL Cleanup

Already done — `analyzer-e2e.generated.sql` stripped of GeneXpert.

---

## Files

### Bridge (tools/openelis-analyzer-bridge)

| File                                       | Change                                            |
| ------------------------------------------ | ------------------------------------------------- |
| New: `AnalyzerRegistrationController.java` | POST/DELETE/GET /api/analyzers endpoints          |
| New: `AnalyzerRegistrationService.java`    | In-memory registry, IP→analyzerID mapping         |
| `HttpForwardingRouter.java`                | Stamp X-Analyzer-Id from registry when forwarding |
| `MessageEnvelope.java`                     | Add oeAnalyzerId field                            |

### OE (src/main/java)

| File                                  | Change                                        |
| ------------------------------------- | --------------------------------------------- |
| New: `BridgeRegistrationService.java` | HTTP client to call bridge registration API   |
| `AnalyzerRestController.java`         | Call bridge registration after analyzer save  |
| `AnalyzerImportController.java`       | Use X-Analyzer-Id for direct lookup           |
| `AnalyzerServiceImpl.java`            | autoCreateTestMappings: update stale mappings |

### E2E Tests (frontend/playwright)

| File                               | Change                                                     |
| ---------------------------------- | ---------------------------------------------------------- |
| `astm-genexpert-results.spec.ts`   | Stale cleanup, fill IP/port, test-connection, fix selector |
| `file-import-results.spec.ts`      | Test-connection step                                       |
| `analyzer-test-connection.spec.ts` | Fix hardcoded ID 2013                                      |
| `analyzer-simulator.spec.ts`       | Fix hardcoded ID 2013                                      |
| `analyzer-plugin-config.spec.ts`   | Fix hardcoded ID 2013                                      |

### Fixtures

| File                         | Change          |
| ---------------------------- | --------------- |
| `analyzer-e2e.generated.sql` | Already cleaned |
| `analyzer-harness-e2e.sql`   | Already cleaned |

---

## Execution Order

1. Bridge: add registration API (POST/DELETE/GET)
2. Bridge: stamp X-Analyzer-Id on forwarded messages from registry
3. OE: add BridgeRegistrationService
4. OE: call bridge registration on analyzer create (REST controller)
5. OE: use X-Analyzer-Id header for direct lookup
6. OE: fix autoCreateTestMappings to update stale mappings
7. E2E: fix all test issues (cleanup, IP, selectors, hardcoded IDs)
8. `/restart-analyzer-harness --reset`
9. Run `--project=harness` — ALL green
10. Run `--project=harness` AGAIN — still ALL green
11. Record `--project=demo-video` — 4 videos
12. Commit + push

## Verification

- [ ] Bridge registration API works (curl test)
- [ ] Creating analyzer in OE dashboard registers it with bridge
- [ ] ASTM simulator push → bridge tags with X-Analyzer-Id → OE routes correctly
- [ ] File import → bridge tags with X-Analyzer-Id → OE routes correctly
- [ ] Multiple E2E runs don't cause stale data conflicts
- [ ] ALL harness tests green
- [ ] 4 demo videos show: create → test-connection (green) → results with real
      test names → AccessionResults
