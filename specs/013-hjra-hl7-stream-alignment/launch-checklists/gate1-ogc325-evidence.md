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
- [x] Strict 013 mock E2E path validated through bridge MLLP with profile-backed
      templates: `projects/analyzer-harness/scripts/test-hl7-profiles.sh`
      (BC-5380, BS-200, BS-300)
- [x] Strict fixture guard enforces reproducible analyzer/link state before E2E
      run: `projects/analyzer-harness/scripts/verify-strict-013-fixtures.sh`

## Mock E2E Evidence Command

Run from `projects/analyzer-harness`:

```bash
bash scripts/test-hl7-profiles.sh 1 http://localhost:8085 mllp://openelis-analyzer-bridge:2575
```

Expected result:

- script passes for BC-5380, BS-200, and BS-300
- each message receives bridge AA ACK
- OpenELIS ingestion is verifiable in `AnalyzerResults` and `analyzer_results`
