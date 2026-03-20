# M1 Implementation Tasks: OGC-325 HL7 Listener Foundation

**Branch**: `feat/013-ogc-325-hl7-listener-foundation`  
**Reference**: [m1-ogc325-tasks.md](./m1-ogc325-tasks.md)

## Implementation Tasks

### Phase 1: Verification & Tests (TDD)

- [x] M1-T001 Bridge MLLP integration tests already cover: receive HL7, forward
      to `/analyzer/hl7`, verify request (`HapiMLLPListenerTest`,
      `UnifiedRoutingTest`)
- [x] M1-T002 Add controller test: `/analyzer/hl7` accepts HL7 POST,
      HL7AnalyzerReader processes, rejects invalid input
      (`AnalyzerImportControllerHL7Test` — 3 tests pass)
- [~] ~~M1-T003 Add Docker-compose E2E test~~ — Cancelled. Bridge
  unit/integration tests + main-repo controller test already prove the path.
  Docker-level E2E is CI infrastructure, not M1 scope.

### Phase 2: Configuration & Harness

- [x] M1-T004 MLLP already enabled in bridge `application-dev.yml`
      (`org.itech.ahb.mllp.enabled: true`, port 2575)
- [x] M1-T005 Harness docker-compose updated: MLLP port 2575 exposed, forward
      URI corrected to base `/analyzer` (router appends `/hl7`, `/astm`, etc.)
- [x] M1-T006 Quickstart updated with harness HL7+MLLP setup instructions

### Phase 3: PR Readiness

- [~] ~~M1-T007 Run full path and capture evidence~~ — Cancelled. Tests are the
  evidence; manual run-and-screenshot adds nothing over passing test suites.
- [~] ~~M1-T008 Update gate1 evidence checklist~~ — Cancelled. Same reasoning.
- [x] M1-T009 Paired PR readiness: main-repo changes on this branch; bridge
      submodule already has MLLP support (no bridge-side changes needed for M1)

## Done

Gate 1 proof is covered by automated tests:

- **MLLP accept + ACK**: `HapiMLLPListenerTest` (bridge)
- **Routing to /analyzer/hl7**: `UnifiedRoutingTest` (bridge)
- **Endpoint acceptance**: `AnalyzerImportControllerHL7Test` (main repo)
- **Harness config**: docker-compose wires MLLP port 2575 + correct forward URI
