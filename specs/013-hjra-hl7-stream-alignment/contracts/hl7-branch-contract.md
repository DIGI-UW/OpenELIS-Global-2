# HL7 Branch Contract

## Purpose

This contract defines the scope boundary and promotion rules for the future
implementation branches launched from the `013` coordination lane.

## Branches In Scope

### `feat/013-ogc-325-hl7-listener-foundation`

**Scope**:

- Bridge-submodule MLLP listener work required by `OGC-325`
- Main-repository readiness needed to route bridge traffic into `/analyzer/hl7`
- GenericHL7-side completion needed to prove one representative ingestion path

**Out of scope**:

- Analyzer-specific validation for BC-5380 or BS-series messages beyond the
  representative readiness proof
- Unrelated analyzer protocol work

**Promotion rule**:

- The branch is not eligible to start until the coordination artifacts,
  paired-PR model, and readiness gate are accepted.

**Done rule**:

- One representative MLLP message path is proven end to end
- ACK behavior is validated
- Bridge and main-repository PRs can be reviewed together as one readiness
  bundle
- E2E proof uses the analyzer mock with a loaded profile and a specific analyzer
  type (see readiness gates)

### `feat/013-ogc-327-bc5380-hl7`

**Scope**:

- First analyzer-specific proving path after the listener foundation is accepted
- BC-5380 validation using current HL7 scope and profile seeds

**Out of scope**:

- BS-series expansion
- Any assumption that BC-5380 proves BS-series equivalence
- Bidirectional HL7 messaging (ORM^O01 worklist download, QBP queries) —
  explicitly deferred to post-MVP. CommunicationMode field prepares the data
  model. Bridge outbound MLLP/ASTM client not yet implemented.

**Promotion rule**:

- `feat/013-ogc-325-hl7-listener-foundation` must already satisfy its readiness
  gate

**Done rule**:

- BC-5380 validation evidence is accepted
- No unresolved ambiguity remains about BC-5380 protocol lane or its use as the
  first proving target
- E2E runs use the mock configured with a BC-5380 HL7 profile so the
  implementation is tested end-to-end with that analyzer type

### `feat/013-ogc-326-bs-series-hl7`

**Scope**:

- Combined BS-series branch for BS-200 and BS-300
- Early validation to confirm whether BS-300 can safely share the BS-200 path

**Out of scope**:

- Quietly treating BS-300 equivalence as already proven
- Splitting BS-300 into a separate branch without an explicit scope change

**Promotion rule**:

- BC-5380 proving path must already be accepted

**Done rule**:

- BS-200 validation is complete
- BS-300 early validation outcome is documented and accepted
- Any scope change triggered by BS-300 differences is made explicit
- E2E runs use the mock configured with a BS-series HL7 profile so the
  implementation is tested end-to-end with that analyzer type

## Optional Branch

### `feat/013-ogc-336-genexpert-hl7`

This branch is optional and does not block the primary proving path. It may only
be promoted if later review explicitly elevates `OGC-336` into the active HL7
lane.

## Cross-Repo Ownership Rules

- The bridge submodule owns MLLP listener behavior.
- The main repository owns `/analyzer/hl7` ingestion readiness and downstream
  analyzer-handling coordination.
- The first practical implementation bundle spans both repositories and must be
  reviewed that way.
- No branch may redefine BC-5380 or BS-series work into the ASTM lane based on
  stale examples.
