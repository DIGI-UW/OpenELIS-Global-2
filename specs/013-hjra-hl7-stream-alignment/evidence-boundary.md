# HL7 Evidence Boundary

**Feature**: `013-hjra-hl7-stream-alignment`  
**Date**: 2026-03-10

## Purpose

Freeze what is actually confirmed for the HL7 lane before continuing profile
development. This note separates tracker-backed scope, vendor-manual evidence,
current code behavior, and synthetic test scaffolding.

## Tracker-Backed Facts

Source:
[OpenELIS Global Analyzer Integration Tracker](https://uwdigi.atlassian.net/wiki/spaces/mdgoe/pages/1097531396/OpenELIS+Global+Analyzer+Integration+Tracker)

- `Mindray BC-5380` is in scope as `A2 (HL7)` under `OGC-327`.
- `Mindray BS-series (incl. BS-200)` is in scope as `A2 (HL7)` under `OGC-326`.
- `HL7 MLLP Listener Service` is shared infrastructure under `OGC-325`.
- The tracker rates both BC-5380 and BS-series as `HIGH` confidence from vendor
  documentation.
- The tracker also states that real HL7 captures are still pending for both
  Mindray families.
- The BS-series note is explicit that the adapter must not hardcode `MSH-4`
  sender allowlists and should match dynamically through analyzer registration.

## Vendor-Manual Evidence

Source:
[BS-200 Host Interface Manual (v1.2)](https://pdfcoffee.com/bs-200-host-interface-manual-v12-pdf-free.html)

- The BS-200 family manual describes HL7 `v2.3.1` over TCP/IP with `MLLP`.
- The manual separates sender identity as:
  - `MSH-3`: manufacturer
  - `MSH-4`: model
- The manual examples use one test result per `ORU^R01` message for the
  BS-200/220/120/130/180 family.
- The manual examples use simple numeric test identifiers in `OBX-3`, not the
  copied `^^^CODE^NAME` structure used by several current synthetic fixtures.

## Current Implementation Facts

Current matcher behavior is defined in:

- `plugins/analyzers/GenericHL7/src/main/java/org/openelisglobal/plugins/analyzer/generichl7/GenericHL7Analyzer.java`
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerServiceImpl.java`

Current reality:

- `GenericHL7Analyzer` extracts only `MSH-3`.
- `AnalyzerService.findByIdentifierPatternMatch(...)` evaluates one regex
  against one input string.
- The current matcher does not consider `MSH-4`.
- Profile and fixture success currently depends on synthetic messages that make
  `MSH-3` carry enough analyzer identity to match the configured regex.

## What Is Confirmed Vs Synthetic

### Confirmed

- HL7 is the correct protocol lane for the in-scope Mindray analyzers.
- `OGC-325` remains the shared prerequisite for the lane.
- BC-5380 should remain the first proving slice before BS-series expansion.

### Synthetic But Currently Useful

- Existing HL7 fixtures that force model identity into `MSH-3`.
- Existing profile regexes that assume model-specific matching can be derived
  from `MSH-3` alone.
- Multi-OBX chemistry fixtures for BS-series analyzers that are useful for
  parser tests but are not confirmed to reflect real device emission.

### Not Yet Proven

- That BS-series analyzers can be safely distinguished by `MSH-3` alone.
- That BS-300 truly shares the same effective sender identity and result shape
  as BS-200 in OpenELIS.
- That the existing BC-5380 seed profile matches real production message
  identity without adjustment.

## Current Profile Confidence

### `projects/analyzer-profiles/hl7/mindray-bc5380.json`

- Status: seed profile, lane-valid, message-identity details not fully validated
- Safe use: reference analyzer for matcher and parser regression tests
- Risk: its current `identifier_pattern` still assumes `MSH-3` contains enough
  model detail

### `projects/analyzer-profiles/hl7/mindray-bs360e.json`

- Status: seed profile only
- Safe use: structural template for chemistry profile shape
- Risk: must not be treated as evidence for BS-200 or BS-300 behavior

### `projects/analyzer-profiles/hl7/mindray-bs200.json`

- Status: active strict-013 profile used by the profile-backed mock adapter
- Risk: still synthetic relative to real-device captures; transport and matcher
  behavior are validated via harness proof but vendor capture parity remains
  open

### `projects/analyzer-profiles/hl7/mindray-bs300.json`

- Status: active strict-013 profile used by the profile-backed mock adapter
- Risk: BS-300 equivalence to BS-200 remains a validation question outside
  synthetic harness evidence until real captures are available

## Required Remediation Before Continuing Profile Expansion

1. Define the canonical sender identity contract for GenericHL7.
2. Lock that contract with tests before changing more profiles.
3. Realign seed profiles and fixtures to the tested matcher contract.
4. Keep all synthetic fixtures explicitly labeled as synthetic until real HL7
   captures are available.

## Unblock Condition

Profile development is considered honestly unblocked only when analyzer matching
is explicitly defined, tested, and no longer depends on undocumented assumptions
about `MSH-3` carrying model identity for the BS-series.
