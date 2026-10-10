# Analyzer integration

How OpenELIS, the Analyzer Bridge and the analyzer mock work together to get
instrument results into the laboratory record. This is a short overview of the
target setup. The
[analyzer baseline roadmap](../roadmaps/analyzer-baseline-roadmap.md) is the
authoritative plan while that setup is being built; its last step checks this
file against the landed code. Work outside the remediation is listed in
[roadmap.md](roadmap.md).

## How it works

1. **A profile describes an instrument model.** The Bridge ships one profile
   per supported instrument, written from the vendor's own LIS document: the
   codes and values the instrument sends, their standard codes (LOINC for
   tests; LOINC, SNOMED or CIEL for values), units, where each part of a
   result sits in the message, the vendor's language translations, and how
   control results are recognised. A profile says nothing about a site.
2. **Each analyzer owns its mapping.** When a lab adds an analyzer, OpenELIS
   resolves the profile's standard codes against the local catalog by exact
   match into that analyzer's own mapping. In the setup wizard the operator
   resolves or acknowledges every unresolved row, confirms, and activates. Instrument
   codes, language and number format can be overridden per analyzer, and a
   code the profile never declared is mapped the same way.
3. **Results arrive as FHIR bundles.** The Bridge parses the instrument's
   message and puts everything it understood into the bundle: each part of a
   result (number with comparator, qualitative call, log, analyte values,
   flags, notes), the instrument-reported patient and specimen, assay, operator
   and instrument identity. OpenELIS never reads ASTM, HL7 or CSV.
4. **Every part lands in its own place.** A quantitative result with a call is
   one test: the number on the primary, the call and the complementary values
   on components. Qualitative control cartridges go to the QC module; a failed
   run is held with the instrument's error detail, never posted as a result.
5. **Placement is never silent and never blocked.** A result is pre-ticked only
   when its tube ID or accession resolves to one current analysis with no
   inference and the instrument's patient matches. Everything else is a visible
   state on the review row that the reviewer resolves on the page.
6. **Nothing is lost.** A result that cannot be mapped or placed is held with
   its reason and recovers, without duplication, once the mapping is fixed.
   Both systems keep their audit copy: the Bridge every raw message and
   bundle, OpenELIS the bundle for the life of the result, viewable from the
   review row.
7. **A newer profile revision is adopted explicitly.** One review screen shows
   every row's fate; the analyzer keeps receiving on its current revision until
   the operator confirms and re-activates.

## Who owns what

| System        | Owns                                                                                                                                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Bridge        | Analyzer profiles (immutable revisions), connections and secrets, protocols and transports, parsing, control-result recognition, the raw message audit copy, and the FHIR bundle it builds.             |
| OpenELIS      | The analyzer record (name, lab units, Bridge connection ID), each analyzer's mapping to the local catalog, placement and review of results, held results, quality control, activation, the bundle copy. |
| Analyzer mock | Deterministic instrument behaviour and real ASTM, HL7 and FILE traffic, replayed from vendor-documented messages.                                                                                       |

Production code never branches on a manufacturer, model, profile ID or
analyzer code.

## What must always hold

- Unknown or unmappable results are held; they are never dropped or posted.
- Recovery never duplicates a result. Acceptance is recorded once per
  `(connection_id, message_id)` in `analyzer_delivery_receipt`, independent of
  staging, mapping changes and profile revisions; receipts are never purged.
- One bad mapping holds only its own results; unrelated results continue.
- Quality control and connection probes never gate activation or invalidate a
  verification.

## Where the detail is

- Product design, in
  [DIGI-UW/openelis-work](https://github.com/DIGI-UW/openelis-work):
  [Analyzer Types & Mapping FRS](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/analyzer-profile-mapping.md)
  and its
  [gap analysis](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/analyzer-profile-mapping-gap-analysis.md),
  the
  [ASTM](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/astm-analyzer-mapping-addendum.md)
  and
  [HL7](https://github.com/DIGI-UW/openelis-work/blob/main/designs/analyzer-integration/hl7-analyzer-mapping-addendum.md)
  mapping addenda, the
  [multi-component ingestion FRS](https://github.com/DIGI-UW/openelis-work/blob/main/designs/results-validation/analyzer-multicomponent-ingestion.md),
  [analyzer manual QC](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/analyzer-manual-qc.md),
  the per-instrument integration specs in
  [designs/analyzer-integration](https://github.com/DIGI-UW/openelis-work/tree/main/designs/analyzer-integration),
  and the vendor manuals in
  [assets/vendor-manuals](https://github.com/DIGI-UW/openelis-work/tree/main/assets/vendor-manuals).
- The Bridge, in
  [DIGI-UW/openelis-analyzer-bridge](https://github.com/DIGI-UW/openelis-analyzer-bridge):
  the profile contract (`contracts/analyzer/v1/analyzer-profile.schema.json`),
  the shipped profiles (`src/main/resources/analyzer-profiles/`), the profile
  authoring guide and each profile's evidence note (`docs/`).
- The mock, in
  [DIGI-UW/analyzer-mock-server](https://github.com/DIGI-UW/analyzer-mock-server).
- OpenELIS operator documentation in [docs/analyzers](../../docs/analyzers/).
- The analyzer harness in [projects/analyzer-harness](../../projects/analyzer-harness/).
