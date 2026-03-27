# OE↔Bridge State Sync + Dynamic Mock Networks + E2E Demos

**Branch**: `fix/013-hl7-test-connection` (continuing) **Date**: 2026-03-26

## Context

Deep audit of the OE↔bridge architecture revealed systemic fragility beyond the
original test-connection fix. Three layers of problems:

1. **Bridge state is ephemeral** — The bridge's analyzer registry is an
   in-memory `LinkedHashMap` (no DB, no file persistence). Bridge restart = all
   registrations lost. OE is the authoritative source (PostgreSQL DB) but
   there's no reconciliation. Only fires on OE startup — if the bridge restarts
   while OE is running, registrations are gone permanently.

2. **OE still has direct analyzer socket code** —
   `AnalyzerQueryServiceImpl.queryAnalyzerASTM()` (138 lines) opens direct TCP
   to analyzers with full ASTM ENQ/ACK/frame handling. Production violation of
   bridge-mandatory architecture.

3. **Mock doesn't simulate separate physical analyzers** — all analyzers share
   172.21.1.100, breaking the bridge's `findByIpAddress` registration and not
   matching how real lab networks work.

## Current State (11 commits on PR #3195, all pushed)

### Done:

- CommunicationMode enum + data model + Liquibase + frontend + validation
- Bridge-proxied test-connectivity for all transports (TCP/ASTM/HL7/FILE/SERIAL)
  via new bridge `POST /api/test-connectivity` endpoint
- `testTcpConnectOnly()` removed — OE no longer opens direct sockets to
  analyzers for test-connection (replaced with `callBridgeTestConnectivity()`)
- Bridge HTTPS in dev profile + shared keystore from certs container
- Mock MLLP listener + protocol-agnostic port dispatch
- Mock accession fix (filler_order_id = sample_id) + realistic template IDs
- BridgeRegistrationService SSL fix (trust-all for self-signed certs)
- AnalyzerDAOImpl non-throwing findByIpAddress
- 013 spec artifacts, canonical roadmap, Copilot reviews resolved
- Unified Playwright helpers + test file (not yet passing)
- CI green on all checks

### Remaining for this PR:

- **OE→bridge periodic sync** — reconciliation for bridge restarts (Step 1)
- **Dynamic mock networks** — Docker API for unique IPs per analyzer (Step 3)
- **Dead code removal** — old direct-check methods (Step 4)
- **E2E demo tests** — run and package (Step 5)

### Deferred (Jira tickets):

- **AnalyzerQueryServiceImpl direct socket** — migrate to bridge (Step 2)
- **Bridge persistence (Model B/C)** — lightweight DB for operational state

## Design: Profile as Single Source of Truth

The analyzer profile JSON is the one file that describes everything about an
analyzer type. Every system (OE, bridge, plugins, mock) reads from it. When an
admin selects a profile, OE extracts the relevant config for each system and
pushes it where needed.

### Profile Schema (structured sections)

```json
{
  // Identity — who is this analyzer?
  "profileMeta": { "id": "mindray-bc5380", "version": "1.2.0", "displayName": "..." },
  "analyzer_name": "Mindray BC-5380",
  "manufacturer": "Mindray",
  "category": "HEMATOLOGY",

  // Protocol — what message format does it speak?
  "protocol": { "name": "HL7", "version": "2.3.1" },
  "identifier_pattern": "MINDRAY.*BC.?5380|BC.?5380",
  "msh3_pattern": "MINDRAY",

  // Transport — how does the bridge connect/listen?
  "transport": ["TCP/IP"],
  "transport_config": {
    "TCP/IP": { "default_port": 5380, "framing": "MLLP" }
  },

  // Communication — who initiates, what's supported?
  "communication": {
    "mode": "ANALYZER_INITIATED",
    "supports_lis_initiated": true
  },

  // OE Config — how does OE handle results from this analyzer?
  "configDefaults": { ... },

  // Test Mappings — how do protocol fields map to OE tests?
  "default_test_mappings": [ ... ]
}
```

This already exists. The key insight: **no new schema needed**. The profile
already has transport, protocol, communication, and identity sections. What's
missing is the **sync mechanism** that extracts transport config and pushes it
to the bridge.

### How Profile Flows Through the System

```
1. Admin selects profile "hl7/mindray-bc5380" in OE UI
2. OE creates Analyzer entity with profile defaults:
   - name, category, protocol, IP, port, communication mode
   - identifier pattern, test mappings (from profile)
3. OE extracts bridge registration payload FROM the profile:
   - sourceId: analyzer IP
   - protocol: HL7
   - identifier_pattern: from profile
   - transport_config: from profile
4. OE pushes registration to bridge via sync API
5. Bridge stores in registry (in-memory for MVP, persistent later)
6. Bridge uses registry to identify + route incoming messages
7. Plugin uses test_mappings from profile to parse results
```

The profile is read by OE at step 2. OE stores the extracted values in the
Analyzer entity (DB). Steps 3-5 sync from the Analyzer entity (not re-reading
the profile). Step 6 is bridge runtime. Step 7 is plugin runtime. No system
besides OE reads the profile directly — OE mediates everything through the
Analyzer entity as the runtime representation.

### Model A MVP (this PR)

**OE is authoritative. Bridge is stateless. Periodic sync.**

- Profile defines all config (already done)
- OE creates analyzer from profile (already done)
- OE syncs analyzer transport config to bridge periodically
- Bridge stores in-memory, loses on restart, next sync recovers
- Test-connection routes through bridge (already done)

### Path to Model C (future)

- Bridge gets persistence (H2/SQLite) — sync writes to DB
- Bridge reports operational state back to OE (connection status, latency)
- OE displays bridge-reported state in admin UI
- No profile schema changes needed — just transport layer improvements

## Completion Status (updated 2026-03-27)

### Step 1: OE↔Bridge State Sync — DONE ✅

**Bridge side** (submodule, PR #25):

- `PUT /api/analyzers/sync` — full-state idempotent registry replacement
- `AnalyzerRegistryBootstrap` — pulls from OE on bridge startup
- Thread-safe `syncAll()` with atomic reference swap
- Accurate added/updated/removed counts via `.equals()` comparison
- Stale FILE watcher cleanup on sync

**OE side** (PR #3195):

- `BridgeRegistrationService.syncAll()` method
- `AnalyzerBridgeStartupRegistrar` re-registers on OE startup

**Still pending**: `@Scheduled` periodic re-sync (currently only fires on
startup).

### Step 2: Direct Socket Code Migration — DONE ✅

`AnalyzerQueryServiceImpl.queryAnalyzerASTM()` has been **fully removed**.
Replaced by `queryViaBridge()` which POSTs to bridge's `POST /api/query`. The
bridge performs the ASTM ENQ/ACK/frame exchange — OE never opens direct sockets
to analyzers.

Bridge endpoint: `AnalyzerQueryController.java` at `POST /api/query`.

### Step 3: Dynamic Docker Networks for Mock Analyzers

**Problem**: All mock analyzers share 172.21.1.100.

**Approach**: Mock server uses Docker API (Python `docker` SDK) to dynamically
create per-analyzer networks at runtime.

Validated on running harness:

- `docker network create` + `docker network connect --ip` ✓
- Mock (binds 0.0.0.0) listens on new IPs immediately ✓
- Bridge reachable via dynamic IPs ✓
- MLLP handshake works ✓

**Mock API**: `POST /analyzers`, `DELETE /analyzers/{name}`, `GET /analyzers`

**Requires**: Docker socket mount, `docker` Python package, env vars for
container names.

### Step 4: Dead Code Removal — DONE ✅

Removed methods that bypass bridge:

- `testFileConfiguration()` removed (replaced by `testFileViaBridge()`)
- `testSerialConfiguration()` removed (replaced by `testSerialViaBridge()`)

### Step 5: E2E Demo Tests — IN PROGRESS

**Unified `analyzer-demo-flow.spec.ts` now covers 7 analyzers:**

- Mindray BC-5380 (HL7/MLLP) — full flow with dynamic sample_id ✅
- Mindray BS-200 (HL7/MLLP) — same ✅
- Mindray BS-300 (HL7/MLLP) — same ✅
- GeneXpert ASTM — same ✅
- QuantStudio 7 (FILE/xlsx) — file drop with known fixture IDs ✅ (restored)
- QuantStudio 5 (FILE/xls) — same ✅ (restored)
- FluoroCycler XT (FILE/xlsx) — same ✅ (restored)

FILE analyzers use xlsx fixtures from `frontend/playwright/fixtures/` copied to
host-mounted import directories. Fixture sample IDs (HARN-\*) are known
constants from the fixture files.

**Stale test removed**: `hl7-mindray-results.spec.ts` (hardcoded FILLER012,
superseded by unified demo flow).

**Report packaging**:

1. Run:
   `npx playwright test analyzer-demo-flow --project=harness-demo-video --workers=1`
2. All tests pass with video recordings
3. `npx playwright show-report` generates self-contained HTML at
   `playwright-report/`
4. Package:
   `cd frontend && zip -r ../analyzer-demo-report.zip playwright-report/`
5. The HTML report embeds video inline — open `index.html` in any browser
6. Attach zip to PR or host for stakeholder review

**CI**: Push all changes, verify Build+Test, Static, Image, E2E all green.

## Files to Modify

| File                                                                                | Change                                                       |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| **Bridge (submodule)**                                                              |                                                              |
| `tools/openelis-analyzer-bridge/.../controller/AnalyzerRegistrationController.java` | Add `PUT /api/analyzers/sync` full-state endpoint            |
| **OE (main repo)**                                                                  |                                                              |
| `src/.../service/AnalyzerBridgeStartupRegistrar.java`                               | Add `@Scheduled` periodic sync (replace one-shot startup)    |
| `src/.../service/BridgeRegistrationService.java`                                    | Add `syncAll(List<AnalyzerRegistration>)` method             |
| `src/.../service/AnalyzerQueryServiceImpl.java`                                     | Add TODO comment + Jira ticket (deferred — bridge migration) |
| `src/.../controller/AnalyzerRestController.java`                                    | Remove dead testFileConfiguration/testSerialConfiguration    |
| **Mock (submodule)**                                                                |                                                              |
| `tools/analyzer-mock-server/server.py`                                              | `/analyzers` API for dynamic network management              |
| `tools/analyzer-mock-server/requirements.txt`                                       | Add `docker` package                                         |
| **Harness**                                                                         |                                                              |
| `projects/analyzer-harness/docker-compose.analyzer-test.yml`                        | Docker socket mount, env vars                                |
| `projects/analyzer-harness/seed-analyzers.sh`                                       | Dynamic IP provisioning via mock API                         |

## PR + Submodule Strategy

Three repos involved — parent (OpenELIS-Global-2), bridge submodule, mock
submodule. Each needs its own PR, merged in dependency order.

**PRs needed:**

| Repo                             | PR        | Content                                              | Depends On                 |
| -------------------------------- | --------- | ---------------------------------------------------- | -------------------------- |
| `tools/openelis-analyzer-bridge` | Bridge PR | test-connectivity endpoint, sync endpoint, HTTPS dev | —                          |
| `tools/analyzer-mock-server`     | Mock PR   | MLLP listener, accession fix, dynamic networks API   | —                          |
| `OpenELIS-Global-2` (parent)     | PR #3195  | Everything else + updated submodule pointers         | Bridge PR + Mock PR merged |

**Merge order:**

1. Bridge PR → merge to bridge `main` (or develop)
2. Mock PR → merge to mock `main` (or develop)
3. Update parent repo submodule pointers to merged commits
4. Parent PR #3195 → merge to `develop`

**Current submodule state:**

- Bridge: detached HEAD at local commits (9c31a48 HTTPS, 156616b
  test-connectivity) — needs PR on bridge repo
- Mock: detached HEAD at local commits (933f07b MLLP, faf68c5 accession fix) —
  needs PR on mock repo

**After merging:**

- Parent's `tools/openelis-analyzer-bridge` pointer → merged bridge commit
- Parent's `tools/analyzer-mock-server` pointer → merged mock commit
- CI runs on parent PR with the merged submodule pointers

## Verification (Definition of Done)

1. Bridge restart → next OE sync pushes full state → "Synced N analyzers" in
   logs
2. OE restart → startup sync pushes full state → bridge has all analyzers
3. Each mock analyzer gets unique IP via Docker network API
4. test-connection returns success for all TCP analyzers via bridge
5. **All 6 analyzer demo flows pass** (BC-5380, BS-200, BS-300, GeneXpert, QS7,
   FC-XT)
6. **Playwright HTML report with embedded videos** — shareable as standalone
   webpage
7. **CI green** — Build+Test, Static, Image, E2E all pass
8. Jira ticket created for AnalyzerQueryServiceImpl direct socket migration
