# M3 Task Outline: OGC-326 BS-Series HL7

**Branch**: `feat/013-ogc-326-bs-series-hl7`  
**Reference**: [hl7-branch-contract.md](../contracts/hl7-branch-contract.md),
[hl7-readiness-gates.md](../contracts/hl7-readiness-gates.md)

## Purpose

Reference outline for `/speckit.implement` when the M3 branch is opened.
Combined BS-series delivery for BS-200 and BS-300.

## Prerequisites

- Gate 2 passed: BC-5380 proving path accepted

## Task Categories

### Branch Creation

- Create `feat/013-ogc-326-bs-series-hl7` from `develop` after M2 acceptance

### BS-200 Validation

- BS-200 target validated (BS-200 has stronger evidence than BS-300)

### Early BS-300 Validation

- Validate whether BS-300 can safely share BS-200 path
- Document outcome before equivalence is treated as settled
- If "not yet" or "not cleanly," record explicitly

### Mock Configured with BS-Series HL7 Profile for E2E

- Configure analyzer mock to load BS-series-compatible HL7 profile
- Mock specific analyzer type for end-to-end testing
- Note: `mindray-bs360e.json` covers BS-360E (different model); may be
  structural template only for BS-200/BS-300 profiles

### Gate 3 Evidence Collection

- Branch scope explicitly committed to both BS-200 and BS-300
- Early BS-300 validation outcome documented and accepted
- Any scope change from BS-300 differences made explicit

### PR

- PR targets `develop` after M2 acceptance

## Done Rule

- BS-200 validation complete
- BS-300 early validation outcome documented and accepted
- Any scope change triggered by BS-300 differences is explicit
- E2E runs use mock configured with BS-series HL7 profile
