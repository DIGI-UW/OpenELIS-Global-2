# Universal Analyzer Bridge — Updated Implementation Plan v2.0

**Updated**: 2026-02-06 | **Original**: universal-analyzer-bridge.md v1.0
**Status**:

- **M1-M6**: ✅ complete (8 PRs merged)
- **M7**: ⚠️ PR open, **Copilot remediation complete** (tests passing) —
  awaiting final review/merge:
  [openelis-analyzer-bridge#11](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/11)
- **M7.1 (plan)**: ✅ documented in parent repo PR:
  [OpenELIS-Global-2#2730](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2730)
  (implementation still pending)
- **M8**: ❌ not started (prompt ready; metrics/health/E2E still missing)
- **M9 + R-OPENELIS**: ❌ pending

---

## Immediate Next Steps (Current)

### Step 1: Merge M7 after final review

- **Bridge repo**: M7 is ready for merge once PR #11 review is satisfied.
- **Validation**: `mvn test` and `mvn verify` both pass on the PR branch.

### Step 2: Merge the plan update PR (this doc + M7.1 plan)

- **Parent repo**: merge
  [OpenELIS-Global-2#2730](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2730)
  so the M7.1 security milestone is captured on `demo/madagascar`.

### Step 3: Start M8 implementation (after M7 merge)

- **Bridge repo branch**: create `feat/universal-bridge-integration` from
  `develop` _after_ M7 merges.
- **Implementation prompt**: archived (M8 complete, see bridge repo
  `feat/universal-bridge-integration`).

---

## Decisions Confirmed

| Decision          | Choice                       | Rationale                                                                              |
| ----------------- | ---------------------------- | -------------------------------------------------------------------------------------- |
| ASTM scope in M7  | **Unify all 5 listeners**    | ASTMBridgeAdapter is ~50 lines, no library changes, zero risk to bidirectional queries |
| Version tag       | **v3.0.0**                   | Major bump signals breaking handler architecture change                                |
| R-OPENELIS timing | **Parallel with M7**         | Separate OpenELIS branch, saves calendar time                                          |
| Plan location     | **Default Claude plan file** | Archive decision deferred to after completion                                          |

---

## Context

The original Universal Analyzer Bridge plan defined 9 milestones across two
repos to extend the ASTM-HTTP bridge into a multi-protocol bridge handling ASTM,
HL7, Serial, File, and HTTP input. After completing M1-M6 (all transport
listeners + OpenELIS endpoints), deep exploration revealed:

1. **Routing fragmentation** — Serial, File, and ASTM handlers bypass the
   central `HttpForwardingRouter`, each with their own HTTP forwarding logic
2. **Dead code paths** — HTTP `/input` endpoint creates a `MessageEnvelope` but
   silently drops it (TODO comment)
3. **Unwired config** — Retry/backoff properties exist in YAML but aren't
   connected to any code
4. **Security gaps** — OpenELIS endpoints return raw exception messages and
   accept bridge headers without validation
5. **Test gaps** — HL7 reader has only 4 tests; no controller integration tests
   exist

This updated plan restructures the remaining work into actionable milestones
with remediation tracks that can run in parallel.

---

## Section 1: Milestone Status

| ID       | Milestone                | Repo         | Status           | PR      | Notes                                                                                                                                   |
| -------- | ------------------------ | ------------ | ---------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| M1       | Foundation               | Bridge       | ✅ MERGED        | #5      | Protocol/Transport enums, MessageEnvelope, ProtocolDetector                                                                             |
| M2a      | MLLP Listener            | Bridge       | ✅ MERGED        | #7      | HapiMLLPListener, HapiReceivingApplication                                                                                              |
| M2b      | HL7 Endpoint             | OpenELIS     | ✅ MERGED        | #2718   | `/analyzer/hl7`, HL7AnalyzerReader 3-tier ID                                                                                            |
| M3       | Serial Listener          | Bridge       | ✅ MERGED        | #9      | SerialPortListener, SerialFrameBuffer                                                                                                   |
| M4a      | File Watcher             | Bridge       | ✅ MERGED        | #8      | FileWatcher, CSVParser, FileMessageHandler                                                                                              |
| M4b      | CSV Endpoint             | OpenELIS     | ✅ MERGED        | #2714   | CSVAnalyzerReader, AnalyzerReaderFactory                                                                                                |
| M5       | HTTP Input               | Bridge       | ✅ MERGED        | #6      | AnalyzerInputController `/input`                                                                                                        |
| M6       | ASTM Enhancements        | Bridge       | ✅ MERGED        | #4      | X-Source-Analyzer-IP propagation                                                                                                        |
| **M7**   | **Message Normalizer**   | **Bridge**   | **⚠️ IN REVIEW** | **#11** | Copilot remediation complete; `mvn test` + `mvn verify` passing; awaiting merge                                                         |
| **M7.1** | **Bridge Auth/Security** | **Bridge**   | **❌ PLANNED**   | —       | Plan documented in [OpenELIS-Global-2#2730](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2730); implementation depends on M7 merge |
| **M8**   | **Integration & Deploy** | **Bridge**   | **❌ 0%**        | —       | Docker compose scaffolding exists; metrics/health/E2E tests not implemented yet                                                         |
| **M9**   | **Spec Documentation**   | **OpenELIS** | **❌ 0%**        | —       | No branch created                                                                                                                       |

### Routing State

**Before M7** (resolved by PR #11):

```
MLLP:    HapiReceivingApplication → MessageRouter → HttpForwardingRouter  ✅ CORRECT
Serial:  SerialMessageHandler → own forwardMessage() with HttpClient      ❌ BYPASS
File:    FileMessageHandler → own forwardToOpenELIS() with RestTemplate   ❌ BYPASS
ASTM:    ASTMReceiveThread → ASTMHandlerService → legacy handler chain    ❌ LEGACY
HTTP:    AnalyzerInputController → creates envelope → DOES NOTHING        ❌ BROKEN
```

**After M7** (PR #11, verified by `UnifiedRoutingTest`):

```
MLLP/Serial/File/ASTM/HTTP → MessageNormalizer → HttpForwardingRouter → OpenELIS
```

---

## Section 2: Repository Workflow

### Repos & Remaining Work

| Repository                         | Purpose                            | Remaining        |
| ---------------------------------- | ---------------------------------- | ---------------- |
| `DIGI-UW/openelis-analyzer-bridge` | Protocol bridge (Java Spring Boot) | M7, M8, R-BRIDGE |
| `I-TECH-UW/OpenELIS-Global-2`      | Main OpenELIS application          | M9, R-OPENELIS   |

### Branch Naming

| Work Item  | Bridge Branch                       | OpenELIS Branch                        | Notes                  |
| ---------- | ----------------------------------- | -------------------------------------- | ---------------------- |
| M7         | `feat/universal-bridge-normalizer`  | N/A                                    | Unify all routing      |
| M8         | `feat/universal-bridge-integration` | N/A                                    | Tests, metrics, health |
| M9         | N/A                                 | `docs/universal-bridge-spec-alignment` | Spec updates           |
| R-BRIDGE   | (fold into M7 PR)                   | N/A                                    | Bridge fixes           |
| R-OPENELIS | N/A                                 | `fix/011-analyzer-endpoint-hardening`  | OpenELIS hardening     |

### PR Dependency Chain (Remaining Only)

```
BRIDGE REPO (DIGI-UW/openelis-analyzer-bridge):

          Current: develop @ 5a9b12b (M1-M6 merged)
                              |
                              v
                ┌─────────────────────────┐
                | PR: M7+R-BRIDGE         |
                | feat/..-normalizer      |
                | [Unified routing +      |
                |  config consolidation]  |
                └────────────┬────────────┘
                             |  MERGE
                             v
                ┌─────────────────────────┐
                | PR: M8 → develop        |
                | feat/..-integration     |
                | [E2E tests, metrics,    |
                |  health, Docker, docs]  |
                └────────────┬────────────┘
                             |  MERGE & TAG v3.0.0
                             v

OPENELIS REPO (I-TECH-UW/OpenELIS-Global-2):

  ┌──────────────────────┐       ┌──────────────────────────┐
  | PR: R-OPENELIS       |       | PR: M9                   |
  | fix/011-analyzer-    |       | docs/universal-bridge-   |
  | endpoint-hardening   |       | spec-alignment           |
  | [ANY TIME - parallel]|       | [AFTER M8 tag v3.0.0]    |
  └──────────────────────┘       └──────────────────────────┘
```

---

## Section 3: Remaining Milestones

### M7: Message Normalizer — Unify All Routing (4 days)

**Repo**: `DIGI-UW/openelis-analyzer-bridge` | **Branch**:
`feat/universal-bridge-normalizer` **Depends on**: M1-M6 (done)

**Target**: All 5 listeners route through
`MessageNormalizer → HttpForwardingRouter`.

```
All listeners → MessageEnvelope → MessageNormalizer → HttpForwardingRouter → OpenELIS
                                       |
                                       ├── AnalyzerIdentifier (multi-strategy)
                                       ├── Retry/backoff (from config)
                                       └── Audit logging
```

| Task | Type   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | File(s)                                                                           |
| ---- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| T01  | CREATE | `MessageNormalizer` service: accepts `MessageEnvelope`, runs `AnalyzerIdentifier`, delegates to `MessageRouter`, logs audit trail                                                                                                                                                                                                                                                                                                                                                                                                                                | `normalizer/MessageNormalizer.java`                                               |
| T02  | CREATE | `AnalyzerIdentifier` with strategy chain: (1) envelope.analyzerId, (2) IP config lookup, (3) header-based (MSH-3/4, ASTM H-record), (4) serial-port config, (5) file-path pattern                                                                                                                                                                                                                                                                                                                                                                                | `normalizer/AnalyzerIdentifier.java`                                              |
| T03  | CREATE | `AnalyzerRegistryConfig` — `@ConfigurationProperties("bridge.analyzers")` for IP/serial/file-path → analyzer-ID mappings                                                                                                                                                                                                                                                                                                                                                                                                                                         | `config/AnalyzerRegistryConfig.java`                                              |
| T04  | WIRE   | Add retry/backoff to `HttpForwardingRouter` using existing `bridge.openelis.retry.*` config. Remove `@ConditionalOnProperty("bridge.file")` from `OpenELISConfig` so retry config is always available                                                                                                                                                                                                                                                                                                                                                            | `routing/HttpForwardingRouter.java`, `config/OpenELISConfig.java`                 |
| T05  | WIRE   | Refactor `SerialMessageHandler`: remove own `forwardMessage()` + `HttpClient`; inject and delegate to `MessageNormalizer`                                                                                                                                                                                                                                                                                                                                                                                                                                        | `serial/SerialMessageHandler.java`                                                |
| T06  | WIRE   | Refactor `FileMessageHandler`: remove own `forwardToOpenELIS()` + `RestTemplate`; inject and delegate to `MessageNormalizer`                                                                                                                                                                                                                                                                                                                                                                                                                                     | `file/FileMessageHandler.java`                                                    |
| T07  | WIRE   | Wire `AnalyzerInputController`: after creating envelope, call `messageNormalizer.process(envelope)`. Remove TODO                                                                                                                                                                                                                                                                                                                                                                                                                                                 | `controller/AnalyzerInputController.java`                                         |
| T08  | CREATE | `ASTMBridgeAdapter`: implements `ASTMHandler` interface (from `astm-http-lib`). Replaces `DefaultForwardingASTMToHTTPHandler` in the `@Bean` factory at `AstmHttpBridgeApplication.astmHandlerService()`. Converts `ASTMMessage` + `sourceIp` → `MessageEnvelope` with `Protocol.ASTM`, `Transport.TCP`, then delegates to `MessageNormalizer`. ~50 lines. **No library code changes** — only the bean registration in main app changes. Bidirectional queries (HTTP→ASTM via `DefaultForwardingHTTPToASTMHandler`) are a separate handler and remain untouched. | `normalizer/ASTMBridgeAdapter.java`, `AstmHttpBridgeApplication.java` (bean swap) |
| T09  | CONFIG | Add `bridge.analyzers` section to `configuration.yml` with example IP/serial/file-path mappings                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | `configuration.yml`                                                               |
| T10  | TEST   | Unit tests for `MessageNormalizer`, `AnalyzerIdentifier`, `ASTMBridgeAdapter`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | `test/normalizer/`                                                                |
| T11  | TEST   | Integration test: verify all 5 listener types route through `MessageNormalizer` to `HttpForwardingRouter`                                                                                                                                                                                                                                                                                                                                                                                                                                                        | `test/integration/UnifiedRoutingTest.java`                                        |

#### T08 Detail: ASTM Adapter Architecture

The ASTM path uses a handler chain from the `astm-http-lib` library module:

```
TCP Socket → ASTMServlet → ASTMReceiveThread       ← UNTOUCHED (library code)
                              ├── extractSourceIp()
                              └── communicator.receiveProtocol()
                                       ↓
                              ASTMHandlerService.handle(msg, ip) ← UNTOUCHED (library code)
                                       ↓
                              ASTMBridgeAdapter (NEW — main app, replaces DefaultForwarding...)
                                 ├── Build MessageEnvelope(Protocol.ASTM, Transport.TCP, sourceIp, rawMsg)
                                 └── messageNormalizer.process(envelope)
                                       ↓
                              HttpForwardingRouter → POST /analyzer/astm + all X-* headers + retry
```

**Why this is safe**: The adapter implements `ASTMHandler` (a library interface)
and is registered via the existing `@Bean` factory. No library code is modified.
The bidirectional query path (`DefaultForwardingHTTPToASTMHandler`) is a
completely separate handler going the opposite direction (HTTP→ASTM) and remains
untouched. Line contention handling happens in `Communicator` _before_ handler
dispatch and is unaffected.

**Also resolves these R-BRIDGE items** (folded into M7):

- RB-01: Serial password String exposure (code deleted by T05)
- RB-02: HTTP `/input` silent message drop (fixed by T07)
- RB-03: Three different HTTP clients (consolidated by T04-T06)
- RB-04: Retry config `@ConditionalOnProperty` scoping (fixed by T04)

**Standalone R-BRIDGE items** (add to this PR):

- RB-05: Standardize config prefixes — document in README that `org.itech.ahb.*`
  is legacy, `bridge.*` is preferred (deferred full migration)
- RB-06: Consolidate dual OpenELIS base URIs in `configuration.yml`

---

### M7.1: Bridge Authentication & Security (2 days)

**Repo**: `DIGI-UW/openelis-analyzer-bridge` | **Branch**:
`feat/bridge-authentication` **Depends on**: M7 merged (PR #11)

**Context**: Copilot review of PR #11 identified a critical security gap — the
`/input` HTTP endpoint in `AnalyzerInputController` has no authentication
requirements. Any client with network access to the bridge can POST arbitrary
ASTM/HL7/CSV payloads, which are forwarded to OpenELIS under the bridge's
credentials.

**Security Risk**:

- **Attack scenario**: Attacker on hospital network POSTs fake lab results
  (e.g., positive HIV test)
- **Impact**: Unauthorized data injection, tampering with patient records
- **Scope**: HTTP endpoints that accept analyzer data (`/input`)

**Requirement**: Implement authentication pattern consistent with OpenELIS REST
API security model.

| Task | Type      | Description                                                                                                                                  | File(s)                                               |
| ---- | --------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| T7a  | RESEARCH  | Document OpenELIS REST API authentication patterns (Spring Security config, bearer tokens, basic auth, role-based authorization)             | Research findings → this section                      |
| T7b  | DESIGN    | Select authentication approach: (A) IP whitelist, (B) Bearer token/API key, (C) Mutual TLS, (D) Spring Security integration with OE patterns | `docs/security.md`                                    |
| T7c  | CONFIG    | Add authentication configuration properties to `bridge.security.*` namespace                                                                 | `config/SecurityConfig.java`, `configuration.yml`     |
| T7d  | IMPLEMENT | Add Spring Security filter chain for `/input` and analyzer data endpoints                                                                    | `config/SecurityConfig.java`                          |
| T7e  | IMPLEMENT | Add authentication to `AnalyzerInputController`                                                                                              | `controller/AnalyzerInputController.java`             |
| T7f  | TEST      | Unit tests for security filter chain, unauthorized request rejection                                                                         | `test/security/SecurityConfigTest.java`               |
| T7g  | TEST      | Integration test: verify authenticated requests succeed, unauthenticated requests return 401/403                                             | `test/integration/AuthenticationIntegrationTest.java` |
| T7h  | DOC       | Update README with authentication setup instructions, credential configuration                                                               | `README.md`                                           |

**OpenELIS Auth Pattern Research** (T7a findings):

**Current OE REST API Authentication:**

- **Pattern**: Spring Security with form-based session authentication
- **Security Config**: `SecurityConfig.java` uses `@EnableWebSecurity` with
  multiple filter chains
- **REST Endpoints**: `/rest/**` paths are protected by
  `ModuleAuthenticationInterceptor` (requires authenticated session)
- **HTTP Basic Auth**: Available via `BasicAuthFilter` but conditional
  (`org.itech.login.basic=true`)
- **RBAC**: Module-based permissions via
  `ModuleAuthenticationInterceptor.preHandle()` - checks user roles/modules

**Bridge `/input` Endpoint Requirements:**

- **Challenge**: Machine-to-machine communication (not browser sessions)
- **Current State**: No authentication (security gap identified by Copilot)
- **Recommended Approach**: HTTP Basic Authentication (aligns with OE's existing
  `BasicAuthFilter` pattern)
  - Simple credential management (username/password in bridge config)
  - Compatible with OE's existing HTTP Basic auth infrastructure
  - No session management needed (stateless)
  - Standard protocol (widely supported by analyzers/HTTP clients)

**Alternative Considered**: API key/bearer token - more complex, requires custom
filter (OE doesn't have this pattern)

**References**:

- PR #11 Copilot Comment #2775830476
- M7 Implementation: PR #11 `feat/universal-bridge-normalizer`

---

### M8: Integration, Testing & Deployment (4 days)

**Repo**: `DIGI-UW/openelis-analyzer-bridge` | **Branch**:
`feat/universal-bridge-integration` **Depends on**: M7 merged

| Task | Type   | Description                                                                                                                       | File(s)                                       |
| ---- | ------ | --------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| T12  | CREATE | Add `micrometer-registry-prometheus` dependency                                                                                   | `pom.xml`                                     |
| T13  | CREATE | `MetricsService`: counters per protocol/transport, forwarding latency timer, active listener gauge. Wire into `MessageNormalizer` | `config/MetricsService.java`                  |
| T14  | CREATE | `MLLPHealthIndicator`: checks `HapiMLLPListener.isRunning()`                                                                      | `health/MLLPHealthIndicator.java`             |
| T15  | CREATE | `SerialHealthIndicator`: reports port connection statuses                                                                         | `health/SerialHealthIndicator.java`           |
| T16  | CREATE | `FileWatcherHealthIndicator`: reports watched directory status                                                                    | `health/FileWatcherHealthIndicator.java`      |
| T17  | UPDATE | Docker Compose: expose MLLP port 2575, add file watcher volume, serial device mount placeholder                                   | `docker-compose.yml`                          |
| T18  | UPDATE | Docker Compose test: add HL7 mock sender, CSV file drop container, verify X-\* header logging                                     | `docker-compose.test.yml`                     |
| T19  | TEST   | E2E: MLLP HL7 → bridge → HTTP capture verifies `/analyzer/hl7` + headers                                                          | `test/integration/MLLPEndToEndTest.java`      |
| T20  | TEST   | E2E: File drop CSV → bridge → HTTP capture verifies `/analyzer/csv` + headers                                                     | `test/integration/FileEndToEndTest.java`      |
| T21  | TEST   | E2E: HTTP `/input` POST → bridge → HTTP capture verifies routing                                                                  | `test/integration/HTTPInputEndToEndTest.java` |
| T22  | TEST   | E2E: ASTM TCP → bridge → HTTP capture verifies normalizer path                                                                    | `test/integration/ASTMEndToEndTest.java`      |
| T23  | UPDATE | README: add Universal Bridge config reference (`bridge.*`), all transport sections, architecture diagram update                   | `README.md`                                   |
| T24  | UPDATE | `configuration.yml`: add MLLP section, analyzer registry examples, Prometheus actuator exposure                                   | `configuration.yml`                           |
| T25  | TAG    | Tag release `v3.0.0` after all tests pass                                                                                         | `git tag`                                     |

---

### M9: Spec Documentation (2 days)

**Repo**: `I-TECH-UW/OpenELIS-Global-2` | **Branch**:
`docs/universal-bridge-spec-alignment` **Depends on**: M8 tagged v3.0.0

| Task | Type   | Description                                                                                                                                                                                                 | Target File                                        |
| ---- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| T26  | UPDATE | Update submodule reference to bridge v3.0.0                                                                                                                                                                 | `tools/openelis-analyzer-bridge`                   |
| T27  | UPDATE | Add "Protocol vs Transport Architecture" section with transport matrix table                                                                                                                                | `specs/011-.../research.md`                        |
| T28  | UPDATE | Document MLLP architecture decision and bridge requirement for HL7                                                                                                                                          | `specs/011-.../research.md`                        |
| T29  | UPDATE | Add "Universal Bridge Integration" cross-reference section                                                                                                                                                  | `specs/004-.../research.md`                        |
| T30  | UPDATE | Cross-reference Universal Bridge from spec 004 plan                                                                                                                                                         | `specs/004-.../plan.md`                            |
| T31  | UPDATE | Update this plan document with final completion status                                                                                                                                                      | `specs/011-.../plans/universal-analyzer-bridge.md` |
| T32  | UPDATE | Document plugin architecture and business logic boundaries: 4-layer separation (bridge → reader → plugin → mapping wrapper), HL7 identification inconsistency, and guidelines for future plugin development | `specs/011-.../research.md`                        |

---

## Section 4: R-OPENELIS Remediation (3 days, parallel)

**Repo**: `I-TECH-UW/OpenELIS-Global-2` | **Branch**:
`fix/011-analyzer-endpoint-hardening` **Depends on**: Nothing — can start
immediately, parallel with M7/M8

### Priority 1: Security

| Task  | Description                                                                                  | File                                    | Severity |
| ----- | -------------------------------------------------------------------------------------------- | --------------------------------------- | -------- |
| RO-01 | Sanitize error responses on `/analyzer/hl7` — don't expose raw exception messages            | `AnalyzerImportController.java:210-224` | HIGH     |
| RO-02 | Sanitize error responses on `/analyzer/csv` (lines 139, 150)                                 | `AnalyzerImportController.java`         | HIGH     |
| RO-03 | Sanitize error responses on `/analyzer/astm` (line 113)                                      | `AnalyzerImportController.java`         | HIGH     |
| RO-04 | Validate bridge headers (X-Source-Analyzer-IP, X-Analyzer-Id) come from trusted bridge IP(s) | `AnalyzerImportController.java`         | MEDIUM   |

### Priority 2: Correctness

| Task  | Description                                                                                                                 | File                                   | Severity |
| ----- | --------------------------------------------------------------------------------------------------------------------------- | -------------------------------------- | -------- |
| RO-05 | Add request size limits on analyzer endpoints (Spring `server.tomcat.max-http-form-post-size` or `@RequestBody` annotation) | `application.properties` or controller | MEDIUM   |
| RO-06 | Make CSV plugin iteration deterministic — add ordering or conflict detection                                                | `CSVAnalyzerReader.java:153`           | LOW      |

### Priority 3: Test Coverage

| Task  | Description                                                                                                                       | File                                      | Severity |
| ----- | --------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------- | -------- |
| RO-07 | Expand HL7AnalyzerReaderTest: add 3-tier identification tests (bridge ID, MSH, source IP), bridge header tests, fallback behavior | `HL7AnalyzerReaderTest.java`              | MEDIUM   |
| RO-08 | Add AnalyzerImportController integration tests for all three endpoints                                                            | `AnalyzerImportControllerTest.java` (NEW) | MEDIUM   |

---

## Section 5: Parallelization Strategy

### Dependency Graph

```
              Current: M1-M6 merged across both repos
                                |
          ┌─────────────────────┼────────────────────┐
          |                     |                     |
          v                     |                     v
┌───────────────────┐           |          ┌───────────────────┐
| M7 + R-BRIDGE     |           |          | R-OPENELIS        |
| (Bridge repo)     |           |          | (OpenELIS repo)   |
| [4 days]          |           |          | [3 days]          |
| PARALLEL TRACK A  |           |          | PARALLEL TRACK B  |
└────────┬──────────┘           |          └───────────────────┘
         |                      |               (independent)
         ├──────────────────┐   |
         v                  v   |
┌───────────────────┐  ┌────────────────────┐
| M8: Integration   |  | M7.1: Auth/Sec     |
| (Bridge repo)     |  | (Bridge repo)      |
| [4 days]          |  | [2 days, PARALLEL] |
└────────┬──────────┘  └────────────────────┘
         |  TAG v3.0.0
         v
┌───────────────────┐
| M9: Documentation |
| (OpenELIS repo)   |
| [2 days]          |
└───────────────────┘
```

### Timeline

| Scenario                   | Calendar Days | Notes                                                                                  |
| -------------------------- | ------------- | -------------------------------------------------------------------------------------- |
| Sequential                 | 15 days       | M7(4) → M7.1(2) → M8(4) → M9(2) + R-OPENELIS(3)                                        |
| **Parallel (recommended)** | **10 days**   | Track A: M7(4) → M8(4) + M7.1(2) parallel → M9(2). Track B: R-OPENELIS(3) during M7+M8 |

### Work Order

| Day  | Track A (Bridge)                                                             | Track B (OpenELIS)                                           | Track C (Bridge, parallel)                 |
| ---- | ---------------------------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------ |
| 1-4  | **M7**: Create normalizer, wire all listeners, add retry, tests              | **R-OPENELIS**: Sanitize errors, validate headers, add tests | —                                          |
| 5-8  | **M8**: Prometheus metrics, health indicators, E2E tests, Docker, tag v3.0.0 | (complete)                                                   | **M7.1**: Auth/security (parallel with M8) |
| 9-10 | —                                                                            | **M9**: Update submodule, spec docs                          | —                                          |

---

## Section 6: Business Logic Separation (Architecture Note)

The Universal Bridge architecture maintains clear layer boundaries:

```
BRIDGE (transport-only)          OPENELIS (business logic)
========================         ==============================
                                 AnalyzerImportController
TCP/MLLP/Serial/File/HTTP  →      ├── HL7AnalyzerReader (protocol parsing)
  ↓                                ├── CSVAnalyzerReader (format parsing)
MessageNormalizer                  ├── ASTMAnalyzerReader (protocol parsing)
  ↓                                │
HttpForwardingRouter               ├── PluginAnalyzerService (plugin registry)
  ↓                                │     └── plugin.isTargetAnalyzer(lines)
POST /analyzer/{hl7|csv|astm}      │
  + X-Analyzer-Id (hint)           ├── AnalyzerLineInserter (per-plugin parsing)
  + X-Source-Analyzer-IP (hint)    │     └── Extract test codes, results, mappings
  + raw message body               │
                                   └── MappingAwareAnalyzerLineInserter (optional)
                                         └── Field transformation overlay
```

**Key principle**: Bridge knows NOTHING about analyzer-specific parsing. It
provides transport hints (headers) and raw protocol data. OpenELIS plugins own
all business logic.

**Known inconsistency** (not blocking, document in M9): HL7AnalyzerReader does
its own 3-tier analyzer identification (bridge ID → MSH-3 → source IP) before
plugin delegation, while CSV/ASTM readers delegate identification entirely to
plugins via `isTargetAnalyzer()`.

---

## Section 7: Verification & Success Criteria

### Success Criteria (Carried from Original Plan)

| #   | Criterion                                                         | Verified By                        | Status                      |
| --- | ----------------------------------------------------------------- | ---------------------------------- | --------------------------- |
| 1   | Single bridge handles: ASTM/TCP, HL7/MLLP, Serial, File, HTTP     | M8 E2E tests (T19-T22)             | PENDING                     |
| 2   | ALL 5 listeners route through MessageNormalizer (no bypasses)     | M7 integration test (T11)          | DONE (PR #11; `mvn verify`) |
| 3   | All Madagascar analyzers work through bridge                      | M8 E2E + plugin tests              | PENDING                     |
| 4   | <100ms p95 latency for message forwarding (bridge overhead)       | M8 metrics + load test             | PENDING                     |
| 5   | Auto-reconnection on serial disconnect                            | M3 (done) — verified in deployment | COMPLETE                    |
| 6   | Prometheus metrics for all listeners                              | M8 MetricsService (T13)            | PENDING                     |
| 7   | Zero protocol code in OpenELIS (HTTP-only at boundary)            | Architecture review                | COMPLETE                    |
| 8   | Spec documentation updated with protocol/transport clarifications | M9 (T27-T31)                       | PENDING                     |
| 9   | No raw exception messages returned to clients                     | R-OPENELIS (RO-01-03)              | PENDING                     |
| 10  | Retry/backoff on all forwarding paths                             | M7 (T04)                           | DONE (PR #11; `mvn verify`) |

### Verification Steps

### How to Verify M7

```bash
# In bridge repo
cd tools/openelis-analyzer-bridge/
mvn clean test  # All unit + integration tests pass

# Verify unified routing (T11 test):
# - Mock OpenELIS HTTP server captures requests
# - Send MLLP message → verify /analyzer/hl7 received with X-* headers
# - Send serial message → verify /analyzer/astm received (no bypass)
# - Drop CSV file → verify /analyzer/csv received (no bypass)
# - POST to /input → verify message routed (no longer dropped)
# - Send ASTM TCP → verify routed through MessageNormalizer
```

### How to Verify M8

```bash
# Run E2E with Docker
docker compose -f docker-compose.test.yml up --build
# Check Prometheus metrics
curl http://localhost:8443/actuator/prometheus | grep bridge_
# Check health
curl http://localhost:8443/actuator/health | jq '.components'
# Verify all listeners report UP
```

### How to Verify R-OPENELIS

```bash
# Run existing + new tests
cd /Users/pmanko/code/OpenELIS-Global-2
mvn test -pl . -Dtest="HL7AnalyzerReaderTest,CSVAnalyzerReaderTest,AnalyzerImportControllerTest"

# Manual: POST malformed HL7 → verify generic error (not stack trace)
# Manual: POST with spoofed X-Source-Analyzer-IP from non-bridge IP → verify rejected/logged
```

---

## Section 8: Critical Files

### Bridge Repo (to modify)

| File                                      | M7 Action                                                   |
| ----------------------------------------- | ----------------------------------------------------------- |
| `routing/HttpForwardingRouter.java`       | Wire retry/backoff from config                              |
| `serial/SerialMessageHandler.java`        | Replace own HTTP forwarding → delegate to MessageNormalizer |
| `file/FileMessageHandler.java`            | Replace own HTTP forwarding → delegate to MessageNormalizer |
| `controller/AnalyzerInputController.java` | Wire envelope to MessageNormalizer (remove TODO)            |
| `config/OpenELISConfig.java`              | Remove `@ConditionalOnProperty("bridge.file")`              |
| `configuration.yml`                       | Add `bridge.analyzers` section, consolidate URIs            |

### Bridge Repo (to create)

| File                                 | Purpose                                                   |
| ------------------------------------ | --------------------------------------------------------- |
| `normalizer/MessageNormalizer.java`  | Central orchestration service                             |
| `normalizer/AnalyzerIdentifier.java` | Multi-strategy analyzer ID resolution                     |
| `normalizer/ASTMBridgeAdapter.java`  | Adapter from legacy ASTM handlers to MessageEnvelope flow |
| `config/AnalyzerRegistryConfig.java` | `@ConfigurationProperties` for analyzer mappings          |

### OpenELIS Repo (to modify)

| File                                                        | R-OPENELIS Action                        |
| ----------------------------------------------------------- | ---------------------------------------- |
| `analyzerimport/action/AnalyzerImportController.java`       | Sanitize errors, validate bridge headers |
| `analyzerimport/analyzerreaders/CSVAnalyzerReader.java`     | Deterministic plugin ordering            |
| `analyzerimport/analyzerreaders/HL7AnalyzerReaderTest.java` | Expand test coverage                     |

---

## Task Summary

| Work Item     | Tasks             | Days | Parallel?                     | Repo     |
| ------------- | ----------------- | ---- | ----------------------------- | -------- |
| M7 + R-BRIDGE | T01-T11, RB-05-06 | 4    | Track A                       | Bridge   |
| R-OPENELIS    | RO-01-08          | 3    | Track B (parallel with M7/M8) | OpenELIS |
| M7.1          | T7a-T7h           | 2    | Track C (parallel with M8)    | Bridge   |
| M8            | T12-T25           | 4    | After M7, parallel with M7.1  | Bridge   |
| M9            | T26-T32           | 2    | After M8                      | OpenELIS |

**Remaining (post-M7 remediation):** implement M7.1 auth, complete M8, complete
R-OPENELIS, then M9 docs + submodule update.

---

## Appendix: Reference to Original Plan

The following sections from the original plan (v1.0) remain valid and are NOT
duplicated here. Refer to `universal-analyzer-bridge.md` (v1.0, superseded by
this document) for:

| Section                                                    | Original Lines | Notes                                        |
| ---------------------------------------------------------- | -------------- | -------------------------------------------- |
| Architecture Diagram (5 listeners → normalizer → OpenELIS) | 117-158        | Still the target architecture                |
| Hybrid Architecture (Bridge ↔ OpenELIS message flow)       | 162-225        | HTTP forward format examples unchanged       |
| Protocol vs Transport Matrix                               | 230-245        | Reference for M9 documentation               |
| Configuration Schema (full YAML)                           | 636-710        | Add `bridge.analyzers` in M7; rest unchanged |
| Dependencies (pom.xml)                                     | 714-742        | Already added in M1                          |
| Git Workflow / Worktree Setup                              | 746-788        | Use for M7/M8 branch work                    |
| PR Strategy per Repository                                 | 790-828        | Updated chain in Section 2 above             |
| Submodule Update Workflow                                  | 830-857        | Use after M8 tag                             |
| Quick Start Commands                                       | 892-909        | Test commands still valid                    |

**In case of conflict**: This updated plan (v2.0) takes precedence over v1.0.
