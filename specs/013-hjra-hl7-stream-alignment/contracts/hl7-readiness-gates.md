# HL7 Readiness Gates

## Purpose

This contract records the minimum evidence required before the HL7 lane can move
from planning into each downstream implementation branch.

## Gate 1: `OGC-325` Listener Foundation

**Applies to**: `feat/013-ogc-325-hl7-listener-foundation`

**Required evidence**:

- Bridge listener accepts framed HL7 traffic over MLLP
- Expected acknowledgment behavior is demonstrated
- Traffic is routed into the main-repository `/analyzer/hl7` path
- One representative bridge-to-OpenELIS ingestion path completes successfully
- Bridge and main-repository changes are reviewable as a paired readiness bundle
- E2E proof MUST use the analyzer mock (e.g. `tools/analyzer-mock-server` or
  harness); the mock MUST load a profile and MUST mock a specific analyzer type
  so the path is testable and reproducible
- Mock MUST accept inbound MLLP connections on the analyzer's configured port
  and respond with proper HL7 ACK (matching MSH-10). This simulates real
  analyzer behavior where the instrument listens for LIS-initiated messages.

**Gate fails if**:

- The proof stops at bridge startup or configuration
- ACK behavior is still assumed rather than demonstrated
- The representative ingestion path does not reach `/analyzer/hl7`
- One side of the paired bundle is ready but the other is not

## Gate 2: BC-5380 First Validation Target

**Applies to**: `feat/013-ogc-327-bc5380-hl7`

**Required evidence**:

- Gate 1 already passed
- BC-5380 remains in the HL7 lane with no ASTM contamination
- BC-5380 validation uses the proven listener path rather than a bypass
- Reviewers agree BC-5380 is the first proving slice before BS-series expansion
- E2E validation uses the analyzer mock configured with a BC-5380 HL7 profile
  (mock loads profile, mocks that analyzer type) so the implementation is tested
  end-to-end with a known message format
- Test-connection for BC-5380 returns genuine TCP connectivity result (not
  hardcoded success). CommunicationMode = ANALYZER_INITIATED.
- Test-connection verifies both bridge health (HTTPS /actuator/health) and TCP
  reachability to the mock analyzer's MLLP port.

**Gate fails if**:

- BC-5380 proof depends on unproven listener behavior
- The proving path widens to BS-series scope before BC-5380 is accepted
- Stale repo references create protocol-lane ambiguity

## Gate 3: BS-Series Combined Branch

**Applies to**: `feat/013-ogc-326-bs-series-hl7`

**Required evidence**:

- Gate 2 already passed
- Branch scope is explicitly committed to both BS-200 and BS-300
- Early branch work validates whether BS-300 can safely share the BS-200 path
- The outcome of BS-300 early validation is documented before equivalence is
  treated as settled
- E2E validation uses the analyzer mock configured with a BS-series HL7 profile
  (mock loads profile, mocks that analyzer type) so the implementation is tested
  end-to-end with a known message format

**Gate fails if**:

- BS-300 is silently treated as equivalent without evidence
- The branch is narrowed to BS-200 only without a scope decision
- Early BS-300 validation is deferred until after the branch is treated as
  functionally complete

## Gate 4: Optional GeneXpert HL7 Elevation

**Applies to**: optional `OGC-336` scope review

**Required evidence**:

- Core HL7 lane milestones remain intact
- Reviewers explicitly elevate `OGC-336` from optional to active scope
- Added work does not destabilize the primary proving path

**Gate fails if**:

- Optional scope is added implicitly
- The core proving path is delayed without an explicit tradeoff decision

## Evidence Recording Rules

- Missing documentation must be recorded as an evidence limitation, not as
  implied completion.
- Analyzer profile JSON files are seed inputs, not proof of implementation
  completeness.
- Jira status alone is not enough to pass a gate without concrete reviewable
  evidence.
- E2E evidence MUST use the analyzer mock with a loaded profile and a specific
  analyzer type; ad-hoc or one-off message payloads alone do not satisfy the
  mock-based E2E requirement.
- For strict `013` readiness, evidence MUST include BC-5380, BS-200, and BS-300
  through the bridge-mediated MLLP path (no direct `/analyzer/hl7` bypass).
