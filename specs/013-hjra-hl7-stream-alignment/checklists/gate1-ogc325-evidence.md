# Gate 1 (OGC-325) Evidence Checklist

**Purpose**: Collect evidence required before the HL7 lane can move from
coordination into the first implementation branch.  
**Branch**: `feat/013-ogc-325-hl7-listener-foundation`  
**Reference**: [hl7-readiness-gates.md](../contracts/hl7-readiness-gates.md)

## Required Evidence

- [ ] MLLP listener accepts framed HL7 traffic
- [ ] ACK behavior is demonstrated (not assumed)
- [ ] Traffic is routed into main-repository `/analyzer/hl7` path
- [ ] One representative bridge-to-OpenELIS ingestion path completes
      successfully
- [ ] Paired PR readiness: bridge and main-repository changes are reviewable as
      one readiness bundle
- [ ] E2E proof uses analyzer mock with **loaded profile** and **specific
      analyzer type** (e.g. BC-5380 for first proving path)

## Gate Fails If

- Proof stops at bridge startup or configuration only
- ACK behavior is assumed rather than demonstrated
- Representative ingestion path does not reach `/analyzer/hl7`
- One side of the paired bundle is ready but the other is not

## E2E Mock Requirement

Per `contracts/hl7-readiness-gates.md`: The mock MUST load a profile and MUST
mock a specific analyzer type so the path is testable and reproducible. Ad-hoc
or one-off message payloads alone do not satisfy the mock-based E2E requirement.
