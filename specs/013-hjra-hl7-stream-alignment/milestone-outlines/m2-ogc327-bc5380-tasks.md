# M2 Task Outline: OGC-327 BC-5380 HL7

**Branch**: `feat/013-ogc-327-bc5380-hl7`  
**Reference**: [hl7-branch-contract.md](../contracts/hl7-branch-contract.md),
[hl7-readiness-gates.md](../contracts/hl7-readiness-gates.md)

## Purpose

Reference outline for `/speckit.implement` when the M2 branch is opened. First
analyzer validation target after M1 listener foundation.

## Prerequisites

- Gate 1 (OGC-325) passed: end-to-end bridge + ACK + `/analyzer/hl7` ingestion
  proof accepted

## Task Categories

### Branch Creation

- Create `feat/013-ogc-327-bc5380-hl7` from `develop` after M1 acceptance

### BC-5380 Profile Validation

- BC-5380 remains in HL7 lane (no ASTM contamination)
- BC-5380 validation uses proven listener path, not a bypass
- Use profile seed from `projects/analyzer-profiles/hl7/mindray-bc5380.json`

### Mock Configured with BC-5380 HL7 Profile for E2E

- Configure analyzer mock to load BC-5380 HL7 profile
- Mock specific analyzer type so implementation is tested end-to-end with known
  message format

### Gate 2 Evidence Collection

- Reviewers agree BC-5380 is first proving slice before BS-series expansion
- No unresolved protocol-lane ambiguity from stale repo references
- Test-connection for BC-5380 returns genuine TCP result (verifying
  fix/013-hl7-test-connection behavior is present on the current consolidation
  branch or on `develop`).

### PR

- PR targets `develop` after M1 acceptance

## Done Rule

- BC-5380 validation evidence accepted
- No unresolved ambiguity about BC-5380 protocol lane or its use as first
  proving target
- E2E runs use mock configured with BC-5380 HL7 profile
