# Analyzer integration

How OpenELIS, the Analyzer Bridge and the analyzer mock work together to get
instrument results into the laboratory record. This spec is the authority for
analyzer design; code that disagrees with it is wrong. The
[analyzer baseline roadmap](../roadmaps/analyzer-baseline-roadmap.md) records
how the baseline was built and the decisions behind each rule. Work outside it
is listed in [roadmap.md](roadmap.md).

## Boundary implementation status — 22 October 2026

The interpret-once boundary below is the revised engineering decision requested
by the user. The coordinated stack cleanup removes duplicated interpretation checks and
incomplete qualitative normalization. The [boundary audit and implementation
plan](boundary-remediation.md) records those gaps and the removal order; this
specification is not a claim that the new boundary has shipped.

## How it works

1. **A profile describes an instrument model.** The Bridge ships one profile
   per supported instrument, written from the vendor's own LIS document: the
   codes and values the instrument sends, the codings that name each test and
   each value, units, where each part of a result sits in the message, the
   vendor's language translations, and how control results are recognised. A
   coding is `{system, code}`. A core-shipped profile cites standard systems
   (LOINC for tests; LOINC, SNOMED or CIEL for values); a distro's profile may
   also cite the distro catalog's own system, the vocabulary its tests are
   named in. A profile says nothing about a site.
2. **Each analyzer owns its mapping.** When a lab adds an analyzer, OpenELIS
   resolves the profile's codings against the local catalog by exact match
   into that analyzer's own mapping, with one rule for tests and values: a
   profile coding names the local test or answer that carries the same
   `(system, code)`. OpenELIS keeps a concept's codes as terminology rows whose
   `source` is the system. LOINC, SNOMED, CIEL and OCL have canonical URIs for
   FHIR; any other source is an identifier the catalog owner chose, and no list
   of permitted systems exists anywhere. A row that does not resolve says why
   (no match, ambiguous, or incompatible). There is no shared mapping per
   analyzer type: the Analyzer Types page only previews a profile's defaults.
   A profile may mark a test or part `"default": "EXCLUDED"`. Individual
   values use the containing test or part's `value_defaults` map, for example
   `"value_defaults": {"ERROR": "EXCLUDED"}`. A new analyzer starts with these
   rows on Do not receive, and the page counts each as
   decided, so a type is Ready when every row is mapped or excluded. For a type
   in use the page also says, for each analyzer on it, whether its mapping is
   confirmed and current or behind. In the setup wizard the operator
   enables the assays this instrument runs, as on the instrument's own host test
   code table, resolves every unresolved row among them, confirms, and activates.
   Language and number format can be overridden per analyzer on its Bridge
   connection, and a code the profile never declared is mapped like any other
   row. The code an instrument sends for a declared assay is a setting of its
   Bridge connection; OpenELIS does not hold it.
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
   review row. A reviewer may remove a held result from review instead, such
   as a test message or a result from a retired analyzer: the removal is in
   the audit trail with the row it removed, the bundle stays on the delivery
   receipt, and the removed result does not recover.
7. **A newer profile revision is adopted explicitly.** One review screen shows
   configuration changes and their effect on local mapping targets; the analyzer keeps
   receiving on its current revision until the operator confirms and applies. Apply switches OpenELIS and the Bridge
   together; if the Bridge cannot switch, neither does.
8. **The baseline profiles started fresh.** No profile from before the baseline
   is an earlier revision of a baseline profile or can be adopted into one. A
   one-time migration kept every existing analyzer's identity, history, lab
   units and Bridge connection, exported its old mapping, cleared it, and left
   it inactive until a person sets it up again on a baseline profile.
9. **OpenELIS and the Bridge trust each other by certificate.** They pair once,
   with the Bridge's single-use pairing code. Each side then pins the other's
   certificate by its SHA-256 fingerprint and accepts only that certificate,
   in both directions: no password, no certificate authority, no host name
   check. `/analyzer/fhir` admits only the paired Bridge and records its
   deliveries as the system user. OpenELIS keeps its own key pair for this,
   apart from the certificate it serves HTTPS with, and reads the pairing from
   its database wherever it checks it, so pairing again needs no restart.
   [Pairing](../../docs/analyzers/bridge-pairing.md) is the operator's guide.
10. **Setting analyzers up and working their results are separate roles.**
    Global Admin owns everything that changes how results are read: analyzer
    types, each analyzer's setup and mapping, adoption, activation, connection
    probes and pairing. The Analyser Import role (and administrators) works
    the results: the review page, held results and failed runs, import and
    delivery issues, and the analyzer list.
11. **What an instrument says about a sample is of three kinds, and the rule
    is fixed.** A value for a test the lab orders, or one of its parts, is a
    result on the ordered analysis. A value on a control material goes to the
    QC module, and saving patient results never removes it. Everything else the instrument says about a sample (an alarm,
    an internal control's validity, a run check) is informational: a sample
    flag shown on the review row and filed as a note on the analysis when the
    result is accepted, never a result, never QC, never a block on acceptance
    or release. A record the profile declares but the lab's catalog has no
    test for is held until the lab maps it or marks it Do not receive. The
    profile decides which kind each record is and its evidence page says so;
    OpenELIS never guesses.
12. **A Bridge reads one shipped set, and a revision is its id and
    number.** Core ships the baseline profiles and the templates. A Bridge's
    shipped set is the one in its image or the complete folder a distro points
    it at (`BRIDGE_PROFILE_CATALOG_SHIPPED_PATTERN`), and then the image's
    profiles are not read. A distro that keeps its own set copies the core
    profiles it uses unchanged, under core's ids, and adds its own under its
    own ids; sets are never merged and a profile is never overridden. A
    revision is `(profileId, revision)`. The Bridge computes a fingerprint of
    the file's content when it publishes or loads a revision, and reports it
    so OpenELIS can tell cheaply that a file changed; nobody writes it by
    hand, and nothing is refused because it changed. A copied file is the same
    revision as its source. Profiles a lab authors in the UI are the site's
    own, under `site.` ids, in the Bridge's revision store, and enter a shipped
    set only when a distro takes them in under an id of its own.
13. **The Bridge interprets once; OpenELIS maps and places the outcome.**
    The Bridge resolves the sender, decodes the message and decides what the
    instrument reported using the configuration captured for that receipt.
    It normalizes codes, numbers, vendor spellings, parts, patient/control
    classification, failure outcomes and informational flags into an explicit,
    versioned FHIR result contract. Raw text and configuration provenance remain
    audit evidence. Ordinary retry preserves the rendered bundle and delivery
    identity. Restart before rendering uses retained decoding context, never
    silently substitutes current configuration. Unresolved input is retained
    with a reason and requires explicit resolution rather than a guessed result.

    OpenELIS validates the delivered contract and source association, applies
    the approved local test/component/answer mappings and exclusions, checks
    compatibility with local targets, places the result and supports review,
    acceptance, operational QC, audit and duplicate prevention. The Bridge does
    not maintain a second copy of OpenELIS's approved local catalog mapping.

    OpenELIS does not compare a delivered result with today's decimal setting,
    extraction recipe, aliases, vendor translations, routing patterns or control
    recognition rules. Delivery and held recovery do not fetch current Bridge
    profiles/connections to reconstruct how an earlier result was interpreted.
    Profile IDs/revisions and fingerprints are provenance, not sufficient proof
    that two raw codes mean the same thing, nor standalone rejection gates.
    Delivered semantic identity, type and units must fit the approved local
    binding; changed meaning under a reused code cannot silently pass through.

    Viewing, editing and confirming an existing local mapping does not contact
    the Bridge or require a recognition-rule hash. Adoption retains canonical
    output declarations for local editing; it does not copy parsing recipes.
    Older mappings remain editable from their saved rows and received normalized
    results. Named reviewer, timestamp, mapping decisions and audit remain required.

    Local mapping changes still require approval and stale-editor protection.
    Setup/profile changes remain authorized, validated administrative actions,
    with their effects shown at adoption. A parsing-only change does not
    retrospectively invalidate a normalized result or an unchanged local mapping.
    Recognition evidence describes the delivered classification; it is not a
    second recognition-rule hash gate at import.

    A held result recovers by reapplying local mapping/placement to its preserved
    normalized payload. Missing historical decimal metadata alone is not a hold
    reason. Unknown or incomplete semantic results remain explicit unresolved
    work until a reviewed decision is possible. Recovery preserves receipts,
    dismissed/finalized results and the audit trail; it never invents another
    message identity to bypass duplicate protection. The mapping editor's
    concurrency fingerprint, connection probe setting fingerprint and certificate
    pins remain safeguards for their separate responsibilities.

## Who owns what

| System        | Owns                                                                                                                                                                                                                                                                                                          |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Bridge        | Analyzer profiles and their revisions, connections and secrets, protocols and transports (including watching result folders), parsing, control-result recognition, the raw message audit copy, the FHIR bundle it builds, and its own key pair and pairing record.                                            |
| OpenELIS      | The analyzer record (name, lab units, Bridge connection ID), each analyzer's approved mapping to the local catalog and delivered-result compatibility, placement and review of results, held results, quality control, activation, the bundle copy, and its own key pair and the paired Bridge's fingerprint. |
| Analyzer mock | Deterministic instrument behaviour and real ASTM, HL7 and FILE traffic, replayed from vendor-documented messages.                                                                                                                                                                                             |
| Distro        | Its complete shipped profile set, kept in its repository with its history: core's profiles copied unchanged under core's ids, its own profiles under its own ids.                                                                                                                                             |

Production code never branches on a manufacturer, model, profile ID or
analyzer code. OpenELIS has no file poller and never opens a result file.

## What must always hold

- Unknown or unmappable results are held; they are never dropped or posted.
  Only a reviewer removes one from review, and that is audited (point 6).
- Recovery never duplicates a result. Acceptance is recorded once per
  `(connection_id, message_id)` in `analyzer_delivery_receipt`, independent of
  staging, mapping changes and profile revisions; receipts are never purged.
- One bad mapping holds only its own results; unrelated results continue.
- An analyzer that received a result or was ever activated is retired, never
  deleted: it keeps its results and history and leaves the analyzer list
  unless retired analyzers are shown, and it can be restored to inactive. Only
  an analyzer with neither can be deleted, together with its mapping.
- Quality control and connection probes never gate activation or invalidate a
  verification.
- An instrument's warning about a sample is never a result and never gates a
  result: it is read by the reviewer and kept as a note.
- Parsing-configuration changes do not invalidate already normalized results.
  Unknown delivered meanings or incompatible local targets hold only the affected
  work. Administrative profile adoption and local mapping approval are explicit;
  whole-profile fingerprints are not delivery or recovery gates.

## Operational control recording

The approved local mapping identifies the test's primary measurement. Only a
control row mapped to that primary result is charted in operational QC;
auxiliary readings and calls remain available in review and on the retained
delivery. A primary log measurement is handled by its local component mapping,
not by testing a vendor record name or recalculating the instrument's value.

For the mapped test and analyzer, the reported specimen identifier may identify
one usable control lot directly. A reported control level must agree with that
lot when present. Otherwise the reported level must identify exactly one usable
lot. ACTIVE and ESTABLISHMENT lots are usable; merely being the only lot does
not establish a match. The retired Bridge lot-number extension is not a source
of lot identity.

No matching lot or missing qualitative target leaves the primary control held
with a reason and a path to configuration. Applying the analyzer's mapping
retries the retained observation. Only persisted QC measurements count as
recorded. Delivery retries remain idempotent; a new control run has a new delivery
identity and may reuse its specimen identifier. Off-scale quantities remain
visible for review; their boundary is not an exact measurement for a QC chart.
The lab's existing release-blocking setting is unchanged.

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
