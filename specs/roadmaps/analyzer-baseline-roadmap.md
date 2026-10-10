# Analyzer baseline roadmap

Execution plan for the analyzer rework in OpenELIS-Global-2, the Analyzer
Bridge, and the analyzer mock. Written for an implementing agent. This file
and its step files are the one authoritative document for the remediation:
where anything else disagrees, this file wins.
[specs/analyzers/spec.md](../analyzers/spec.md) is a short overview of the
target, checked against the landed code in step 9;
[specs/analyzers/roadmap.md](../analyzers/roadmap.md) holds analyzer work
outside this remediation. Every rule and step is decided; this file does not
argue for them. Each step is
self-contained: its Facts section holds everything needed to build it. Links
under Background are optional reading.

Code baseline: OE2 `develop` c55eb17a1e, Bridge 3.2.6 (OE2 pin 5583a7274e),
analyzer-mock 0.1.3 (OE2 pin 8c64750). Written 2026-10-05.

## Contents

- Steps (one file each, under `analyzer-baseline/`)
- Rules (apply to every step)
- Repo working agreements
- Packaging

## Rules

A change that breaks a rule is wrong even if its step's Done-when passes.

1. A profile describes the instrument model and nothing about a site: codes,
   declared values, result types, units, components, control-recognition
   rules, connection defaults. Standard codes only: test LOINC; for each
   categorical value, the standard codes the vendor or LIVD give it, each as
   `{system, code}` (LOINC, SNOMED, CIEL). No OE2 test IDs, answer labels,
   specimen names or lab units. No `result_value_hints`, no
   `specimen_type_hint`.
2. One profile contract, one shipped set. Core ships the full default profile
   set under one documented contract with templates. A distro points the
   Bridge at its own complete folder (`BRIDGE_PROFILE_CATALOG_SHIPPED_PATTERN`)
   or uses core. No merging.
3. Mappings belong to the analyzer. Each analyzer has its own mapping:
   defaults resolved at setup by exact match against the catalog at that
   moment, plus the operator's overrides. Each row is flagged `DEFAULT` or
   `OVERRIDE`. A row targets a test or a component of a test. There is no
   shared type mapping. The Analyzer Types page shows a read-only preview of
   defaults and has no save or confirm action. This supersedes the shared
   profile with fork in the OGC-1054 FRS (FR-A2, FR-H in
   `openelis-work/designs/analyzer-integration/analyzer-profile-mapping.md`),
   which is updated to match. The vendor makes codes and language a setting of
   each instrument: "Test codes can be user-defined" (Cepheid 303-0251 §1),
   and an assay host test code belongs to one assay definition (302-7279 §2).
   It keeps the FRS gap analysis principle that the portable profile is
   catalog-independent and binding resolves when it is applied (§5).
4. Defaults are exact matches only. Profile LOINC to a local test carrying
   that LOINC; a value to a local answer sharing one of its `{system, code}`
   codings (answers carry LOINC, SNOMED, CIEL and OCL codes from step 2c). A
   value with no coding, or none an answer shares, stays `NO_MATCH`. A
   candidate that cannot hold the result
   (categorical test with no answers, or numeric test without a numeric
   result type) is dropped before uniqueness is checked. Two usable
   candidates stay unresolved. Every unresolved row records a reason:
   `NO_MATCH`, `AMBIGUOUS`, or `INCOMPATIBLE`.
5. Catalog changes never move an existing analyzer. A new default is seen at
   the next setup or adoption, never silently.
6. Fresh baseline, not an upgrade. No pre-baseline profile, core or distro, is
   an earlier version of a baseline profile, and none is adoptable. One
   migration changeset keeps each existing analyzer's identity, history, lab
   units and Bridge connection values, exports its old mapping to
   `configuration_import_run`, clears it, and leaves the analyzer inactive
   until a human verifies it.
7. Adoption of a newer revision is explicit and recorded. Same profile ID,
   newer revision only. One review screen buckets rows `UNCHANGED`, `CHANGED`,
   `NEEDS_MAPPING`, `BLOCKED`. Unchanged rows (default and override) pre-fill.
   A changed row keeps the operator's override beside the new default; the
   operator decides. An override equal to the new default stays an override.
   Renamed, split or merged codes do not carry; they resolve fresh. Adoption
   blocks only when an override targets an inactive local test or a removed
   code has held results. The existing confirm records actor and time. An
   active analyzer keeps receiving on the old revision until Apply, which
   switches OE2 and the Bridge together; if the Bridge cannot be reached,
   neither switches. A result from another revision of the same profile is
   never refused: it maps when the analyzer's revision reads its record the
   same way (same parts, result type and unit), otherwise it is held with
   the revision it arrived under for a person. Held results recover on
   Apply by the same rule and keep the revision they arrived under.
8. Placement of a result is never silent and never blocked. A result is
   pre-ticked for one-click save only when its instrument ID resolves without
   inference: a tube ID (`SampleItem.externalId`, shape `<accession>-<n>`) to
   one current analysis for the mapped test on that tube, or an accession to
   exactly one tube with one current analysis; and, when the instrument
   reported a patient, that identity matches OE2's patient on the order.
   Everything else (retest, same test on two tubes, unordered test, unknown
   accession, unrecognised ID, patient mismatch) is a visible state on the
   review row with full context, resolvable on the page; overriding the
   proposal records a justification. Results before registration remain
   allowed. Match only the current analysis revision; never take the first
   match.
9. The FHIR bundle is the boundary. Everything the Bridge parsed goes into the
   bundle, in a FHIR slot where one exists: each part of a result (rule 17),
   instrument-reported patient (identifier and name, marked
   instrument-reported), `Quantity.comparator` from the instrument's `<` or
   `>` flag, the instrument's other flags as sent, assay name and version,
   notes and error detail as `Observation.note`, operator and
   instrument/module/cartridge identity and reagent lot, and the instrument's
   specimen descriptor as sent. OE2 never reads ASTM,
   HL7 or CSV. The review row shows the instrument's flag, assay name and
   version and operator as sent, labelled instrument-reported; OE2 does not
   interpret the flag.
   Both systems keep their audit copy indefinitely: the Bridge keeps every raw
   message and rendered bundle (no default purge; retention configurable);
   OE2 keeps the bundle per delivery for the life of the result and shows it
   read-only from the review row. Instrument patient data is shown and
   compared, never used to create or change a patient.
10. Everything the analyzer sends has a home. Main result on the primary; log,
    analyte calls, Ct, end point and internal controls on components of the
    same test; qualitative control cartridges in the QC module; error detail
    as a note on the held run failure. Invalid is a coded non-clinical answer;
    ERROR and NO RESULT are held run failures, never patient results. No
    separate internal-control concept.
11. A quantitative result with a qualitative call is one test (HIV viral load
    is the worked case). The number goes on the primary, with a leading
    comparator when the result is off scale (`<40`, `>10000000` copies/mL);
    the call (Detected, Not detected, Invalid) goes on a call component of
    the same test. Not detected fills only the call, never `0`. This is what
    manual results entry (one row per component) and viral-load reporting
    (`Result.getVLValueAsNumber`) already read. Cepheid sends an off-scale
    result as the call with an R.7 `<` or `>` flag and the limit in the R.6
    range, with no number in R.4 (303-0251 §2.1.1); the Bridge maps that to a
    FHIR quantity with the comparator and the limit (rule 17).
12. (On hold until a vendor LIS document or verified capture states them; step 5.)
    MTB/RIF is three results: MTB detection (Detected, Not detected, Trace
    detected), bacillary level, rifampicin resistance on LOINC 89372-7
    (Detected, Not detected, Indeterminate). 46244-0 is retired.
13. Instrument codes are a per-analyzer override. The profile ships the
    vendor's suggested codes; setup lets the operator change what this
    instrument uses; the Bridge uses the override for result translation and
    outbound orders. Setup lists the profile's assays the way the instrument's
    host test code table does: the lab enables the ones this instrument runs
    and sets each code, and only enabled assays are mapped. A result for an
    assay that is not enabled is held, never dropped. A code the profile does not declare (a cartridge or test
    the default profile does not cover) reaches OE2 and is held as an unknown
    test; the operator maps it like any other override, from the held row or
    the editor, as a row of that analyzer's mapping. What the default profile
    covers never limits what a lab can run.
14. The harness runs on its own copy of OE2's generic default dictionary
    (`volume/configuration/backend` shape), loaded through the CSV path, with
    answers and components for every outcome each shipped instrument sends.
    Legacy 2.3.x rows that share LOINC 94500-6 get their correct LOINC through
    the harness CSVs. The main generic dictionary is out of scope.
15. Mock traffic is manufacturer-shaped, replayed from vendor-documented
    example messages (GeneXpert: Cepheid 301-2002 Rev E §6.3.4.1.9 to
    6.3.4.1.11, 303-0251 §2.1, 302-7279 §4), never generated from our
    profile.
16. Test discipline: red first; transactional data through REST, catalog
    through config import; no SQL fixtures, no mapping-repair scripts; E2E
    asserts every declared row, not one per test; readback independent of the
    mapping chosen.
17. Result parts are generic. A result can carry a qualitative part, a
    quantity part, an off-scale or abnormal flag, analyte and complementary
    values (an analyte call, Ct, EndPt, Delta Ct, LOG), notes and errors, and
    provenance (assay name and version, operator, instrument). A profile
    declares where each part sits for its instrument, extending the
    `extraction`, `transformation` and `abnormalFlagMapping` blocks of the
    ASTM mapping addendum v1.2 (FR-22.1, with `GREATER_LESS_FLAG` as the
    comparator precedent). For GeneXpert ASTM that is R.3 components 5 to 8,
    R.4 components 1 and 2, R.6, R.7, R.11 to R.14 and C records (Cepheid
    301-2002 Rev E §6.3.4.1.6); for HL7, OBX-4 (analyte and complementary
    name), OBX-5 components 1 and 2, OBX-8 and NTE (303-0251 §2.1.2). The
    Bridge emits one Observation per reported record, as the instrument sends
    them. A record is identified by its code and its documented
    sub-identity: the analyte and complementary names, written as the
    vendor's own HL7 sub-ID (`HIV-1&Ct`, `&LOG`, empty for the main result;
    Rev E §6.3.4.1.6.1 "Parsing a result record", 303-0251 §2.1.2), carried in
    the standard v2-to-FHIR extension
    `http://hl7.org/fhir/StructureDefinition/observation-v2-subid`
    (`original-sub-identifier`). The number goes in `valueQuantity`, an R.7
    `<` or `>` as `Quantity.comparator` with the R.6 limit as the value when
    the instrument sends no number. A record carrying both a call and a
    number puts the call in `Observation.interpretation` (HL7
    ObservationInterpretation, e.g. `DET` Detected, `ND` Not detected,
    alongside the instrument's text); a record with no number carries its
    call as the value. The instrument's flags (R.7 `N`, `A`, `H`, `L`) are
    interpretations too, so OE2 reads the call only from an interpretation
    coded `POS`, `NEG`, `DET`, `ND` or `IND`. OE2 maps each (code, sub-identity) to a (test,
    component), as OGC-1129 specifies
    (`openelis-work/designs/results-validation/analyzer-multicomponent-ingestion.md`);
    a record with both a call and a number has a second target for the
    call. A profile declares a test's records as its `components[]`, each
    `{code, label, result_type, unit, values, value_codes, sub_identity}`,
    where `sub_identity` is the record the component receives (absent for
    the component that takes the main record's call), and the test names
    that call component in `call_component`. Defaults bind each record to the
    local component of the mapped test whose `code` equals the profile
    component's `code`, OGC-1129's stable code. No instrument-specific code
    in the Bridge or OE2.
18. Language and number format are profile facts. Each declared value lists
    the vendor's translations, all bound to one answer code, so an instrument
    binds out of the box whatever language it runs (Cepheid's Observation
    Language Translations Table, 303-0251 §3 and 302-7279 §7: English,
    French, German, Spanish, Italian, Portuguese, Russian, Ukrainian,
    Japanese). A test or component writes them as `translations{value ->
[text]}` beside `values` (decided 6 Oct); defaults create an answer row
    for each translation, bound to its value's answer, and the editor shows
    it under its value. The per-analyzer override covers anything off the
    table. The
    profile declares the vendor's number format (decimal separator), as the
    OGC-1054 FRS FR-A1 puts result formatting in the profile; an analyzer
    whose instrument runs another locale overrides it, like any other default
    (rule 3). The Bridge turns the instrument's number into a FHIR decimal and
    keeps the raw text.
19. Sources, in order of authority: the vendor's documents, then the
    integration specs and FRSs in `DIGI-UW/openelis-work`, then the code. The
    vendor documents are in `openelis-work/assets/vendor-manuals/` (Cepheid
    301-2002 Rev E, the LIS Interface Protocol Specification) and on the
    vendor's portal (Cepheid 303-0251 Rev A, HIV-1 VL XC; 302-7279 Rev A,
    Xpress CoV-2/Flu/RSV plus). Each declared test cites its document and
    section. Real captures are checks of a site's configuration, never a
    source for a profile.
20. A bad item degrades, never stops. A profile, draft, connection or
    listener that cannot load or start is set aside with its reason, shown in
    health and in the views OE2 reads; everything else keeps running.
    Integrity checks (pins, fingerprints, validation) stay; their failure
    isolates the item. A result is never read against a profile that could
    not be resolved: it is held, visibly. Clinical validity comes from
    transparency and management, never from a component refusing to run.
    (Decided 6 Oct: "issues need to degrade the system gracefully!")
21. Readers at the OE2 and Bridge boundary ignore fields they do not know.
    A field added within a schema version is optional and breaks nobody; a
    breaking change bumps `schemaVersion`, which the reader refuses with a
    clear message. What a reader depends on it checks explicitly. Fields one
    side must never receive (local ownership, operational QC) are refused by
    name, not by refusing everything unknown. (Agreed 6 Oct.)

Deferred and not in this roadmap: moving Analyzer Types under Admin; pairing
the Bridge to its OE2 instance instead of password authentication (6 Oct: "I
would love to have a non-password-based authentication that pairs the bridge
to the OE2 instance instead, but that might be a follow up PR"); opening the
OE2 submodule bump automatically when the Bridge or mock default branch moves,
and the image-tag bump in `docker-compose.analyzers.yml` from the Bridge and
mock release workflows (7 Oct: "why are we manually pinning anything??";
decided "Only remove cross-checks now").

## Repo working agreements

- Each repository tests only itself; tests that need the Bridge and the mock
  together run in OE2 (`.github/workflows/analyzer-components.yml`) against
  the submodule commits OE2 records, and no repository's CI checks another out
  at a commit (7 Oct: "bridge should test bridge concerns, mock should test
  it's own concerns no? and then wherever we need both we do it in the Oe2
  umbrella where submodules literally are there to pin the right version").

`AGENTS.md` and `CLAUDE.md` govern. The items below are the ones every step
uses; they are restated so a step can be run without re-reading those files.

- Work in a worktree under `.worktrees/<short-name>`; run
  `scripts/setup-workspace.sh` in it once.
- Format before every commit, cold:
  `rm -rf target/spotless-* && mvn spotless:apply` and
  `cd frontend && npm run format && cd ..`.
- Skip tests only with both flags: `-DskipTests -Dmaven.test.skip=true`.
- Backend single test: `mvn -Dtest=<Class> test` (Testcontainers; needs
  Docker, JDK 21).
- Harness E2E: `cd frontend && npm run pw:test -- --project=harness-foundational`
  (never raw `npx playwright test`). Parity with CI:
  `projects/analyzer-harness/ci-parity-test.sh`.
- Read CI only with `gh pr checks <PR>`; all three checkpoints
  (`01 Checkpoint - Backend`, `02 Checkpoint - Frontend`,
  `03 Checkpoint - E2E`) must be present and passing.
- Liquibase: new changesets only, numbered after the current highest in
  `src/main/resources/liquibase/3.5.x.x/` (119 at baseline); never edit an
  applied changeset.
- i18n: new keys in `frontend/src/languages/en.json` only.
- Commit early; open the PR early; no `Co-Authored-By` trailer; PR body
  explains the change, not the test runs.

## Packaging

- OE2: one stack on `develop`, bottom to top: step 0, 1, 1b, 2 (two PRs:
  the rename, then the per-analyzer model), 2b, 2c, 3, 4, 7.
  Build, Test and E2E run on the top PR and report to the stack. The stack
  lands as a unit: steps 1 to 4 change behaviour that the harness E2E only
  satisfies once steps 6 and 7 deliver the profile and dictionary, so no PR
  below the top is independently shippable.
- Bridge: step 6 as PRs in `DIGI-UW/openelis-analyzer-bridge`, GeneXpert
  first, then one profile per PR.
- Mock: step 8 as a PR in `DIGI-UW/analyzer-mock-server`.
- Order from 6 Oct, one reviewable stacked PR each: Bridge request
  tolerance and the boundary checks (T6.21 to T6.23) with the startup work
  in #4618; the analyzer page shows the Bridge's blockers and catalog
  issues (T6.17); baseline profiles under new IDs (T6.18); the harness
  dictionary (T7.3, T7.4); the fresh-setup and vendor-outcome E2E with the
  step 2b fixes they found (T7.1, T7.2, T2b.7b, T2b.7c). Then, decided 7 Oct
  ("Delete first, then rewrite (Recommended)"): the old revisions and the
  mock's generative GeneXpert templates deleted (T6.19, T8.4) with the seed
  setting up the harness analyzers as an operator would (T7.4b); the
  remaining specs rewritten straight to the end state (T7.2b, T7.6); then the
  evidence package (T7.8), which is the finish line.
- Last: step 9 as its own PR on `develop`, after everything above has
  landed.
- The top OE2 PR (step 7) bumps the pins (`tools/openelis-analyzer-bridge`
  and `tools/analyzer-mock-server` submodules, and image tags) to the Bridge
  and mock releases and carries the baseline E2E, green. Order: the pins move
  to the Bridge and mock PR heads as soon as they exist (CI builds both from
  the submodules); a maintainer reviews and cuts the Bridge release, then the
  mock release; only then do the image tags move, which is what turns the
  `deployment-contract` check green. Bridge and mock edits are made inside
  the submodule checkouts of the OE2 worktree, and each task's tick and pin
  bump ride in the OE2 commit that lands it.
- Distro follow-on, out of scope here: each distro removes profiles core now
  carries, unsets the shipped-pattern override, rebuilds any remaining
  instrument as a fresh baseline profile, runs the migration.

## Steps

Each step is one file. It is self-contained: its Facts section holds everything needed to build it. Open only the step you are working on.

| Step | File                                                                                         | Goal                                                                                                                                                                                                                                       |
| ---- | -------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 0    | [One analyzer spec folder and this roadmap](analyzer-baseline/00-docs.md)                    | one OGC-agnostic folder holds a short overview of the target and the analyzer work outside this remediation; this roadmap is the plan; nothing unfinished from the earlier roadmap is lost.                                                |
| 1    | [One exact-match resolver](analyzer-baseline/01-resolver.md)                                 | setup and the mapping editor resolve defaults with one rule, by exact standard-code match, and say why when a row stays unresolved.                                                                                                        |
| 1b   | [Placement of analyzer results](analyzer-baseline/01b-placement.md)                          | an analyzer result lands on the exact tube and current analysis when that is unambiguous, and otherwise shows the reviewer what it will do and lets them decide on the page. The bundle is kept and viewable.                              |
| 2    | [Mappings owned by each analyzer](analyzer-baseline/02-analyzer-mappings.md)                 | each analyzer owns its mapping (defaults plus overrides, rows targeting a test or a component), the shared type mapping is gone, every record type the analyzer sends has a home, and existing analyzers are migrated to a fresh baseline. |
| 2b   | [Each result part lands in its own place](analyzer-baseline/02b-result-parts.md)             | OE2 maps each record of an analyzer result (by code and sub-identity) and each record's number and call to its own (test, component), and the review row shows what the instrument reported.                                               |
| 2c   | [Answers carry standard codes in any system](analyzer-baseline/02c-answer-terminology.md)    | an answer keeps LOINC, SNOMED, CIEL and OCL codes as a test does; config import, OCL import, FHIR output and the editors use them; analyzer defaults match on any shared code.                                                             |
| 3    | [Adopt a newer profile revision](analyzer-baseline/03-adoption.md)                           | an analyzer can move from revision N to N+1 of its profile through one review screen, keeping its identity, with every row's fate visible.                                                                                                 |
| 4    | [Remediation inside setup and verification](analyzer-baseline/04-setup-remediation.md)       | the setup wizard shows every unresolved row with its reason, lets the operator fix it in place, and never shows raw server text.                                                                                                           |
| 5    | [Verify vendor vocabulary](analyzer-baseline/05-vendor-vocabulary.md)                        | every code, value and record type a baseline profile declares is cited from the vendor's own LIS or host-interface document.                                                                                                               |
| 6    | [Profile contract, templates, and the shipped set](analyzer-baseline/06-profile-contract.md) | the Bridge enforces one profile contract, ships templates and a guide, ships GeneXpert as the first baseline profile, and carries every parsed fact into the bundle.                                                                       |
| 7    | [Harness copy of the generic dictionary](analyzer-baseline/07-harness-dictionary.md)         | the harness runs on a copy of OE2's generic dictionary that lets every shipped analyzer bind out of the box; the Bridge and mock pins are bumped; the baseline E2E is green.                                                               |
| 8    | [Manufacturer-shaped mock traffic](analyzer-baseline/08-mock-traffic.md)                     | the mock replays vendor-documented messages for every outcome, and a contract test keeps it aligned with the pinned baseline profile.                                                                                                      |
| 9    | [Validate and sync the spec](analyzer-baseline/09-spec-sync.md)                              | `specs/analyzers/spec.md` describes the analyzer setup that landed, so it can be read without this roadmap.                                                                                                                                |
