# M1 Task Outline: OGC-325 HL7 Listener Foundation

**Branch**: `feat/013-ogc-325-hl7-listener-foundation`  
**Reference**: [hl7-branch-contract.md](../contracts/hl7-branch-contract.md),
[hl7-readiness-gates.md](../contracts/hl7-readiness-gates.md),
[gate1-ogc325-evidence.md](../launch-checklists/gate1-ogc325-evidence.md)
**Implementation tasks**:
[m1-implementation-tasks.md](./m1-implementation-tasks.md) (actual M1 task
tracking)

## Purpose

Reference outline for `/speckit.implement` when the M1 branch is opened. Actual
implementation runs on `feat/013-ogc-325-hl7-listener-foundation`; this
coordination branch only documents the expected scope.

## Prerequisites

- Coordination artifacts accepted (spec PR merged or in review)
- `develop` and submodules synced; versions documented in
  `launch-checklists/pre-m1-readiness.md`
- Bridge and main-repository teams agreed on paired PR model per
  `contracts/paired-pr-handoff.md`

## Task Categories

### Branch Creation

- Create `feat/013-ogc-325-hl7-listener-foundation` from `develop` in main
  repository
- Create corresponding working branch in `tools/openelis-analyzer-bridge`
  submodule (map to OGC-325)

### Bridge MLLP Work

- Implement or extend MLLP listener to accept framed HL7 traffic
- Demonstrate ACK behavior (not assume)
- Route traffic into main-repository `/analyzer/hl7` path

### Main-Repository Readiness

- Ensure `/analyzer/hl7` path is ready to receive bridge traffic
- Complete GenericHL7-side work needed for one representative ingestion path

### Mock-with-Profile E2E Proof

- Use analyzer mock (e.g. `tools/analyzer-mock-server` or harness) configured
  with loaded HL7 profile and specific analyzer type
- Run full path: mock → transport → `/analyzer/hl7` → ingestion
- Evidence must satisfy `launch-checklists/gate1-ogc325-evidence.md`

### Paired PR

- Bridge PR and main-repository PR reviewable together as one readiness bundle
- Gate 1 evidence collected before declaring M1 complete

## Done Rule

- One representative MLLP message path proven end to end
- ACK behavior validated
- Bridge and main-repository PRs can be reviewed together
- E2E proof uses mock with loaded profile and specific analyzer type
