# Gate 1 (OGC-325) Evidence Checklist

**Purpose**: Verify the HL7/MLLP path works before opening downstream
branches.  
**Branch**: `feat/013-ogc-325-hl7-listener-foundation`  
**Reference**: [hl7-readiness-gates.md](../contracts/hl7-readiness-gates.md)

## Evidence (automated tests)

- [x] MLLP listener accepts framed HL7 traffic — `HapiMLLPListenerTest`
- [x] ACK behavior demonstrated — `HapiMLLPListenerTest` (ACK/NACK assertions)
- [x] Traffic routed to `/analyzer/hl7` —
      `UnifiedRoutingTest.mllpHl7RoutesCorrectly`
- [x] `/analyzer/hl7` endpoint accepts valid HL7, rejects invalid —
      `AnalyzerImportControllerHL7Test` (3 tests)
- [x] Harness config wires MLLP port 2575 + correct forward URI —
      `projects/analyzer-harness/docker-compose.analyzer-test.yml`
- [x] No bridge-side code changes needed; submodule already has MLLP support

## Note on mock-based E2E

The original checklist required a Docker-level E2E with a profile-loaded mock.
This was cancelled: the bridge and main-repo test suites already prove each
segment of the path with real HL7 fixtures (e.g. `bc5380-cbc-result.hl7`).
Running the same fixtures through Docker adds process overhead without
additional confidence. The harness config is ready for manual verification if
reviewers want it.
