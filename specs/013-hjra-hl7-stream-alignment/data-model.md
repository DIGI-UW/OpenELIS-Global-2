# Data Model: HJRA HL7 Stream Coordination

## Overview

This feature does not introduce runtime database entities. The "data model" for
the planning branch is the set of coordination objects that downstream
implementation branches must honor.

## Coordination Concepts

- **HL7 Stream Coordination Bundle**: The set of Jira-backed HL7 work items and
  planning decisions that define what belongs in the `013` stream and in what
  order those items become implementation-ready.
- **Bridge Listener Foundation**: The shared transport prerequisite that covers
  HL7 over MLLP listener readiness and its paired handoff into OpenELIS-side
  ingestion.
- **Analyzer Validation Target**: A named analyzer-specific proving slice used
  to validate the shared foundation before the stream expands to broader profile
  work.
- **Evidence Gate**: A documented readiness checkpoint that determines whether
  the stream can move from coordination to issue-specific implementation.
- **Implementation Branch Recommendation**: A branch name and scope statement
  that tells reviewers which future slice should start next and what it is
  allowed to include.

## Relationships

- The **HL7 Stream Coordination Bundle** contains the **Bridge Listener
  Foundation**, three **Implementation Branch Recommendations**, and the
  optional `OGC-336` branch decision.
- The **Bridge Listener Foundation** is the first required **Evidence Gate** for
  the entire bundle.
- The **Analyzer Validation Target** for BC-5380 depends on the foundation gate.
- The **Analyzer Validation Targets** for BS-200 and BS-300 share one
  **Implementation Branch Recommendation** but have different confidence levels.
- Every **Implementation Branch Recommendation** references one or more
  **Evidence Gates** that determine when it can move from `frozen` to
  `eligible`.

## State Transitions

### Coordination Bundle

`draft` → `clarified` → `planned` → `implementation-ready`

- `draft` to `clarified`: ambiguity questions resolved
- `clarified` to `planned`: plan, research, contracts, and quickstart complete
- `planned` to `implementation-ready`: downstream tasks created and first gate
  owners aligned

### Evidence Gate

`open` → `in-review` → `passed` or `failed`

- `open` to `in-review`: evidence collection begins
- `in-review` to `passed`: required proofs accepted
- `in-review` to `failed`: proof disproves the assumption or reveals blocking
  issues

### Branch Recommendation

`proposed` → `frozen` → `eligible` → `active` → `done`

- `proposed` to `frozen`: accepted in the coordination artifacts
- `frozen` to `eligible`: all prerequisite gates passed
- `eligible` to `active`: branch is created and implementation begins
- `active` to `done`: verification criteria met and PR merged
