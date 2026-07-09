# Paired PR Expectations and Handoff Process

**Purpose**: Define how bridge and main-repository teams coordinate for
`OGC-325` and downstream HL7 work.  
**Reference**: [hl7-readiness-gates.md](./hl7-readiness-gates.md),
[hl7-branch-contract.md](./hl7-branch-contract.md)

## OGC-325 Paired Delivery Model

For `feat/013-ogc-325-hl7-listener-foundation`, transport and main-repository
ingestion are **not** separable done states. The branch is considered ready only
when:

1. Bridge PR (MLLP listener, routing, ACK behavior) is reviewable
2. Main-repository PR (`/analyzer/hl7` readiness, GenericHL7 completion) is
   reviewable
3. Both PRs can be reviewed together as one readiness bundle

## Expectations

### Bridge Team

- Own MLLP listener behavior and transport
- Deliver PR that demonstrates: listener accepts traffic, ACK behavior, routing
  into `/analyzer/hl7`
- Coordinate branch naming and timing with main-repository team so both PRs are
  ready for joint review

### Main-Repository Team

- Own `/analyzer/hl7` ingestion readiness and downstream analyzer-handling
  coordination
- Deliver PR that completes one representative bridge-to-OpenELIS ingestion path
- Coordinate branch naming and timing with bridge team so both PRs are ready for
  joint review

## Handoff Process

1. **Before starting**: Both teams confirm `develop` and submodules are synced;
   required versions documented in `launch-checklists/pre-m1-readiness.md`
2. **During work**: Keep PRs linked (e.g. cross-reference in PR descriptions);
   avoid declaring one side "done" in isolation
3. **Readiness gate**: Gate 1 passes only when both PRs are reviewable together
   and evidence is collected per `launch-checklists/gate1-ogc325-evidence.md`
4. **After acceptance**: M2 (`feat/013-ogc-327-bc5380-hl7`) can open; no paired
   PR model required for M2/M3 (single-repo work)

## Evidence Expectations

- E2E proof MUST use analyzer mock with loaded profile and specific analyzer
  type
- Jira status alone is not enough; concrete reviewable evidence required
- Missing documentation must be recorded as evidence limitation, not implied
  completion
