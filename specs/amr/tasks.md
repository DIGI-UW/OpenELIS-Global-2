# AMR roadmap

One execution roadmap. Functionality and design stay in the linked sources;
this file owns implementation order and acceptance, not a copy of the FRS.

## Contents

- [Iteration rules](#iteration-rules)
- [00 — source and cutover gate](#00--source-and-cutover-gate)
- [01 — migration rehearsal](#01--migration-rehearsal)
- [02 — case routing and access](#02--case-routing-and-access)
- [03 — case information](#03--case-information)
- [04 — case results and notes](#04--case-results-and-notes)
- [05 — culture media](#05--culture-media)
- [06 — independent culture readings](#06--independent-culture-readings)
- [09 — isolates and referral](#09--isolates-and-referral)
- [10 — AST reporting](#10--ast-reporting)
- [11 — DST rules](#11--dst-rules)
- [08 — incoming results](#08--incoming-results)
- [12 — releases and reports](#12--releases-and-reports)
- [13 — critical callbacks](#13--critical-callbacks)
- [07 — bench work and labels](#07--bench-work-and-labels)
- [14 — patient history](#14--patient-history)
- [15 — environmental and export populations](#15--environmental-and-export-populations)
- [16 — complete cutover acceptance](#16--complete-cutover-acceptance)

## Implementation sequence

V00 → V01 → V02 → V03 → V04 → V05 → V06 → V09 → V10 → V11 → V08 →
V12 → V13 → V07 → V14 → V15 → V16.

Identifiers are retained. Isolates and susceptibility targets exist before
incoming placement, labels and bulk work are accepted. Jira children are tracking
only: V15 belongs to OGC-1382; all other slices belong to OGC-1383. Shared epics
remain with their owners and are linked, not copied into new specifications.

## Iteration rules

- Status: `[*]` active, `[ ]` not started, `[x]` exact automated gate passed,
  `[✓]` stated acceptance reviewed. No parallel status ledger.
- Work one iteration at a time. Freeze interfaces, source decisions and shared
  owner dependencies before implementation; start with a failing focused test.
- Commit each coherent tested behavior; open the draft PR at the first passing
  implementation commit and push subsequent increments. Do not wait for the
  entire slice before making its work reviewable. Manage dependent PRs through
  `gh stack`; base each on its immediate predecessor and keep the stack order
  consistent with this roadmap. No merge or deployment is implied.
- Each PR declares its source requirements, file/behavior boundary, removals,
  exclusions and exact checks before implementation. A newly discovered dependency
  changes that boundary explicitly before more code is added.
- At each commit and goal continuation, reconcile the current branch, PR, source
  revision, remaining acceptance and next bounded change. After two unsuccessful
  repair attempts on the same gate, diagnose the approach before expanding work.
- Advance once the focused functional gate and applicable video/mock comparison
  pass and the increment is committed/published in `gh stack`. Full CI and human
  review are not prerequisites for starting the next implementation slice.
- Run local CI parity and GitHub CI in parallel on committed code checkpoints;
  record pending checks and failures on the owning PR. Finish implementation,
  then resolve CI issues in V16 stabilization before final acceptance/merge.
  A failing functional dependency still blocks dependent work.
- Each slice deletes superseded consumers/expectations, not just adds V2 code.
- **Implemented when:** run the linked exact focused gate; inspect evidence; fix and repeat that
  gate until green; review the stated outcome against the direct mock reference.
  Every user-facing change includes the [recorded mock comparison](plan.md#video-and-mock-comparison).
  An HTTP success, skipped test or missing dependency never closes a step.
- Preserve behavior through code/test evidence, not source “built” labels. Keep
  every post-final change behind the amendment and authorization boundary.
- Pinned requirements and mock sources define the comparison baseline.
  Reconcile any changed source explicitly before expanding the iteration.

## 00 — source and cutover gate

**Tracking:** [OGC-1425](https://uwdigi.atlassian.net/browse/OGC-1425).

**Prerequisites:** None; execute the approved documentation cleanup.

**Removals:** Superseded engineering/design guidance and registrations.

**Status:** `[x]` — documentation and tracking gate verified on 2026-10-06.
Sources: [grouping][routing], [preservation][preserved], [mock][mock].

- [x] Align the three engineering documents and paired design sources to draft
      10.4 with separate transfers and deferred joining.
- [x] Remove the mapped superseded guidance without replacement archives.
- [x] Verify gallery source/generated output, rendered mocks, all 112 criterion
      owners, Jira replacement links/dependencies and archived Confluence history.
- [x] Pass [V00](plan.md#v00--documentation-and-sources).

**Evidence:** [paired engineering review](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4605)
and [design review](https://github.com/DIGI-UW/openelis-work/pull/354). Gallery tests
276/276 and build passed; source/generated retirement scan had zero hits.
828 links/anchors checked: zero current errors, 17 pre-existing unrelated missing
targets. Chrome rendered review covered both V2 mocks and shared reception, catalog,
inventory and report specifications. All 112 criteria have one primary owner.
Jira readback verified 31 superseded closures, 29 aligned shared/future issues,
17 roadmap tasks, 35 dependency links and the unchanged 1383-blocks-1382 direction.
Retained owners/statuses and independent catalog work were preserved. Confluence
1315209256 and its obsolete graph/phase-diagram copies are archived; walkthrough
versions 1–6 remain available. Breakpoint research and proposed personas were
preserved as sibling pages. Prettier and whitespace checks passed.

**Acceptance:** one functional baseline, one engineering roadmap and tracking
that points to them. Documentation completion is not application acceptance.

## 01 — migration rehearsal

**Tracking:** [OGC-1426](https://uwdigi.atlassian.net/browse/OGC-1426).

**Prerequisites:** V00 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Obsolete final-schema storage after required clinical/audit transformation; never applied migration history.

**Status:** `[x]` local rehearsal passed; repository review pending. Sources:
[migration intent][catalog], [mock][mock].

Implementation investigation started from `8c8d31aba30def3f6232475238fa19754523b9a8`
on `feat/1383-ogc-1383-microbiology-v2-v01-migration`, stacked above the
documentation baseline. The candidate is not registered in the active application
changelog; runtime rollout and deployment are outside this rehearsal.

The current `MicroCase` stores one `sampleItemId` and `workflowType`, but no
working lab unit or case Program. `MicroOrderRoutingServiceImpl` can create
`UNASSIGNED` cases from Program selection alone; its case-analysis links do not
establish a unique working lab unit. Current access checks inspect every analysis
on the specimen. For the AC-V2-42 boundary for cases with missing or conflicting
ownership and for mappings from workflow to Program that are not unique, the
implementer confirmed explicit per-case mappings and an abort before changing
clinical data when any mapping is unresolved. Existing case identities and
separate membership are preserved.

- [x] Write the disposable fresh/upgrade rehearsal, preserving clinical records,
      IDs, reports and audit; prove collision failure and rollback/reapply.
- [x] Prove the target schema has no legacy runtime/configuration authority.
- [x] Pass [V01](plan.md#v01--migration); do not roll out runtime in this slice.

Local gate: `AmrCutoverMigrationTest` ran 10 tests with no failures, errors or
skips on disposable PostgreSQL 14.4 databases. Coverage includes fresh install,
clinical upgrade, incomplete/duplicate/invalid mappings, audit-ID collision,
1,004 existing cases, rollback/reapply, and refusal to erase later measurements
or edited migration Timeline entries. Each database first applies the complete
current application changelog. Historical Liquibase rows, clinical IDs, original
and amended reports, identification history, repeat measurements, overrides,
panel versions, lot quantities and audit remain unchanged. Diff review found and
fixed the Timeline rollback guard; its focused regression failed before the fix.
Prettier, Spotless and whitespace checks pass. No browser gate applies to this
slice because it changes no user-facing runtime behavior. CI, deployment and
clinical acceptance are separate evidence.

Tested code revision:
[`38cfcff62396a867d93a94e8090b9a4a24d22bac`](https://github.com/DIGI-UW/OpenELIS-Global-2/commit/38cfcff62396a867d93a94e8090b9a4a24d22bac).
The exact V01 command passed again on that committed revision on 2026-10-06:
10 tests, 0 failures, 0 errors, 0 skipped (124.1 seconds). This evidence-only
roadmap update changes no tested code. [OGC-1426](https://uwdigi.atlassian.net/browse/OGC-1426)
holds the review link and matching evidence; V02/V12 dependent verification is
still required for full AC-V2-42 acceptance.

**Acceptance:** fresh install and upgrade yield the same V2 relationships;
historical meaning survives without a production legacy reader.

## 02 — case routing and access

**Tracking:** [OGC-1427](https://uwdigi.atlassian.net/browse/OGC-1427).

**Prerequisites:** V01 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Workflow classification, protocol routing, Program guards, obsolete routing expectations and reception callers whose backend support is removed.

**Status:** `[*]` recovery in progress; no V02 acceptance. Sources: [catalog][catalog], [routing][routing], [mock][mock].

- [ ] Replace workflow/protocol routing with V2 membership/grouping and catalog
      eligibility; apply the rehearsed cutover and remove obsolete consumers.
- [ ] Share rules between preview/save/edit; enforce case-unit rights everywhere.
- [ ] Pass [V02](plan.md#v02--routing).

**Acceptance:** one blood-culture set yields one case, direct-only micro work
opens a case, CBC opens none, resave is idempotent, Program is unlocked and a
forbidden write changes nothing.

### V02 recovery and review boundaries

The original V02 worktree is preserved for extraction, not treated as an
accepted implementation. Its 204 changed/new/deleted source paths were captured
on 2026-10-07 with a binary patch, new-file archive and SHA-256 manifest under
`.devin/artifacts/amr-v02-recovery-20261007/`. The parent revision is
`0f1f5a8c80da1141db0f923af8fdef2f627a8fec`. Existing test evidence remains in that
worktree; it does not prove the extracted PRs until they are tested again.

V02 keeps its identifier, source requirements and criterion ownership. The
following sub-PRs make its dependencies reviewable without advancing V03 early.
Tests ship with each behavior; the final row is not permission to defer tests.

| Sub-PR                                                 | Prerequisite and change boundary                                                                                                                                                                               | Verification before advancing                                                                                                                                                                                                                      |
| ------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| V02a — migration safety                                | Green committed parent; reject ambiguous observation history, duplicate analysis ownership and cross-case culture parents in the unregistered candidate.                                                       | Reproduce each failure, then run the complete candidate migration rehearsal; verify clinical/history preservation and unchanged active changelog.                                                                                                  |
| V02b — membership rehearsal                            | V02a; extract and audit membership, specimen/isolate/culture provenance constraints and rollback. Keep candidate migrations unregistered.                                                                      | Fresh/upgrade/rollback/reapply; ambiguous ownership and later-data rollback rejection; applied history unchanged.                                                                                                                                  |
| V02c — coherent runtime cutover                        | V02b; canonical membership/routing, catalog switch, shared preview/save/edit, case-unit read/write permissions and every affected old caller. Remove workflow/protocol and reception draft authority together. | Persisted ordinary/direct/culture/mixed orders, resave/cancel/edit, case read/write permissions; frontend/backend contract checks; fresh and upgraded startup; scoped retired-caller scan. Register the candidate only with this coherent runtime. |
| V02d — transfers                                       | V02c; extract separate-case transfers, both-unit permissions, refreshed write ownership and final/amendment restrictions.                                                                                      | Persisted and browser transfers, denial without mutation, concurrent write after transfer, unchanged IDs/history and separate destination cases.                                                                                                   |
| V02e — no-result splits                                | V02d; implement case-scoped eligibility, atomic membership/pending-culture moves and reason on both histories.                                                                                                 | Persisted/browser allowed and denied splits, failure rollback, unrelated ordinary results, preserved IDs/provenance and no case joining.                                                                                                           |
| V02 completion gate (not a separate implementation PR) | V02c–e checks already pass; verify the complete V02 requirements and reconcile the existing coverage matrix/evidence.                                                                                          | Exact V02 focused gate, affected shared-order/security regressions, persisted browser journeys and recorded mock comparison. Full CI is tracked for final stabilization.                                                                           |

V02c is delivered in two ordered PRs after the caller audit:

- **V02c1 — reception retirement:** remove the reception microbiology tile,
  forced Program selection, special readiness gate and draft submission/reload
  path together, including backend draft methods and routing's draft/Program
  fallback. Preserve ordinary order saves, requested samples, questionnaires and
  existing case information. Do not activate the candidate migrations. Verify
  shared order/Program tests, backend routing and case-context regressions, and
  record a persisted reception journey against the shared reception requirement.
- **V02c2 — runtime cutover:** complete the canonical catalog/routing/permissions
  model and remaining workflow/protocol caller removal, then register the tested
  migrations. All V02c runtime gates in the table still apply; V02c1 alone does
  not establish V02c completion or permit V02d to start.

If the V02c dependency audit reveals a smaller independently coherent change,
record that boundary here before extracting it. Do not publish an active cutover
with broken callers to achieve an arbitrary file-count target. Program/export
behavior remains owned by V03/V15; only a demonstrated cutover dependency belongs
in V02. Culture provenance protection does not establish V05 media or V06
independent-reading acceptance. Authorization fixes in existing writers stay
where needed for safe case ownership; they are not deferred merely because the
writer's full feature belongs to a later slice.

V02a is published as [draft PR #4614](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4614)
in stack #4610 after #4612. Its four unsafe-input regressions failed before the
fix; the full migration rehearsal now passes 15 tests with no failures, errors
or skips, including recognized observation preservation through upgrade and
rollback. The active application changelog is unchanged. Full local/GitHub CI
and repository review remain pending; this is not V02 acceptance. The PR records
the exact review revision and evidence. The bounded recovery round finished at V02a.

V02b is now [draft PR #4617](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4617),
stacked above V02a in stack #4610. Its first committed increment (`53789ad725`)
passed two PostgreSQL rehearsals: fresh membership schema and clinical preservation
through upgrade and full rollback. The complete focused suite subsequently passed
29 tests with no failures, errors or skips, covering fresh/upgrade/rollback/reapply,
clinical and applied-history preservation, ambiguous ownership, case-member
provenance constraints and refusal to erase later clinical work. The cross-case
observation regression reproduced the defect before the constraint fix. The
candidate remains unregistered; the PR records the tested source and the additional
isolate-specific fixture verification. This establishes the migration prerequisite,
not runtime or V02 acceptance. V02c is next. Full CI and human review do not block focused implementation. Do not resume edits on the preserved
204-file worktree. Extract only the declared boundary into its owning worktree;
carry necessary fixes and tests together. A sub-PR passing its own checks does not
mark V02 accepted. Finish V02c–e and the completion gate before V03.

V02c1 is published as [draft PR #4619](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4619)
above V02b. Runtime revision `64e8b5f848` passed 37 focused backend and 43
frontend tests. Browser revision `a6ff6de125` passed both persisted reception
journeys: a culture order saves/reloads without a forced Program or reception
details, while an ordinary test with manually selected Microbiology Program
creates no case. Keyboard Program selection and Program-section widths at
1440×1000 and 393×851 are covered. The same journeys are registered in the local
`core-demo-video` project for paced evidence; their normal CI registration stays
in `core-app`.

The scoped mock comparison uses design revision
`516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b`, V2 FR-02.2 and the shared
`designs/sample-collection/clinical-order-entry-v4.html` Request details section.
The rendered implementation keeps Program optional and independent, uses the
ordinary questionnaire, and removes the reception microbiology tile. A remaining
shared-layout difference is explicit: the implementation has a separate Program
section and Clinical Information section, while the shared mock combines Program
and provisional diagnosis under Request details. V16 must reconcile that shared
layout; this comparison does not claim whole-page visual parity or acceptance of
case-information behavior (V03). The existing workflow-based worklist remains
for replacement in V02c2/V07.

Local recordings and screenshots are preserved under
`.devin/artifacts/v02c1-reception/`. PR video attachment publication is pending:
the Chrome extension rejected file upload because file-URL access is disabled.
Do not claim review-accessible video delivery until the attachment is published.
Full-CI follow-ups include the participant authentication setup's fixed database
container name and the project-registration validator reporting `setup` for a
spec that the runner correctly lists under `core-app`. Neither is passing
application evidence. V02c2 remains the next implementation boundary; V02d and
V03 remain gated by the complete V02 checks above.

V02c2 is in progress on `feat/1383-ogc-1427-v02c2-runtime`, based on
V02c1 revision `12f9cef582`. Its first increment closes a reproduced sequence
hazard: a membership history-ID or target-column collision previously failed
only after the preceding structural cutover committed. The candidate now checks
membership targets before retiring the old schema. The combined-sequence
regressions assert unchanged clinical records, applied migration history and
old runtime storage on rejection, and successful upgrade/rollback/reapply.
This is a prerequisite within V02c2, not a new slice or runtime acceptance.
Keep both candidate migrations unregistered until the catalog, routing,
permissions and affected-caller gate is complete. Video attachment publication
for V02c1 remains outstanding and is not satisfied by this migration work.

The recovery audit in [the engineering plan](plan.md#recovery-audit) records
confirmed blockers and unresolved checks. Each extracted PR must state which
findings it resolves, its exact tested revision, and what remains. Existing
uncommitted test counts are historical supporting evidence, not passing evidence
for a recovery PR. V03 starts after V02 functional completion; later iteration order and
all 112 criterion owners remain unchanged.

## 03 — case information

**Tracking:** [OGC-1428](https://uwdigi.atlassian.net/browse/OGC-1428).

**Prerequisites:** V02 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Duplicate questionnaire/context authority. Retirement of reception micro draft callers moves to V02c alongside backend retirement.

**Status:** `[ ]`. Sources: [context][context], [mock][mock].

- [ ] Confirm V02c removed the reception micro tile/draft pipeline; implement context editing on the case.
- [ ] Reuse Program/questionnaires and shared order response; distinguish
      patient-wide context from specimen-specific fields and audit the changes.
- [ ] Keep admission optional, enforce the draft required-before-final fields and
      preserve unknown infection origin; no invented export mapping.
- [ ] Pass [V03](plan.md#v03--context).

**Acceptance:** a shared answer reloads consistently in reception and related
cases; changing one specimen's purpose does not alter its sibling's purpose.
Admission stays optional without fabricating infection origin.

## 04 — case results and notes

**Tracking:** [OGC-1429](https://uwdigi.atlassian.net/browse/OGC-1429).

**Prerequisites:** V03 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Competing case-owned result editors and obsolete whole-case result assumptions.

**Status:** `[ ]`. Sources: [initial testing][initial], [notes][notes], [mock][mock].

- [ ] Reuse chooser, result controls/components, flags, validation and notes;
      remove competing editing paths for case-owned tests.
- [ ] Prove Tested elsewhere, internal-only handling, actor history and shared
      self-validation policy without a second result/validation framework.
- [ ] Prove a graded Gram through existing catalog components and audit every
      technician-added test; apply the draft Results rights for report choices.
- [ ] Pass [V04](plan.md#v04--results-and-notes).

**Acceptance:** a direct Gram/Xpert is entered without an invented isolate,
reloads with real status/actor, and cannot be edited on two worklists.
Its component values remain correctly bound; report choices follow the draft role.

## 05 — culture media

**Tracking:** [OGC-1430](https://uwdigi.atlassian.net/browse/OGC-1430).

**Prerequisites:** V04 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Culture protocol defaults and culture stock-consumption paths.

**Status:** `[ ]`. Sources: [media and links][culture], [mock][mock].

- [ ] Integrate Inventory medium/lot selection and catalog media links; delete
      protocol defaults and culture-side stock consumption.
- [ ] Keep reagent/control policies separate; enforce tracked-medium lot rules.
- [ ] Prove configured test/sample-type media, duration and check defaults pre-fill
      the bench rows and remain freely editable, with no restored protocol lane.
- [ ] Pass [V05](plan.md#v05--media).

**Acceptance:** medium/lot reload correctly; unusable lots are blocked; tracked
and untracked rows behave as designed; culture setup leaves stock unchanged.

## 06 — independent culture readings

**Tracking:** [OGC-1431](https://uwdigi.atlassian.net/browse/OGC-1431).

**Prerequisites:** V05 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Case-wide culture clocks/outcomes and setup-derived timing.

**Status:** `[ ]`. Sources: [culture rows][culture], [mock][mock].

- [ ] Add row-local timing/readings/outcomes, extensions and subculture lineage;
      target analyzer signals by container and derive case progress.
- [ ] Prove reasoned time edits, late growth and confirmed instrument negatives.
- [ ] Pass [V06](plan.md#v06--cultures).

**Acceptance:** one positive bottle leaves its sibling incubating; no-growth
recording does not publish final; due times survive reload and a pinned boundary.

## 09 — isolates and referral

**Tracking:** [OGC-1432](https://uwdigi.atlassian.net/browse/OGC-1432).

**Prerequisites:** V06 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Workflow-specific isolate creation and duplicate accession/referral paths.

**Status:** `[ ]`. Sources: [lineage][isolates], [referral][referral],
[received isolates][routing], [mock][mock].

- [ ] Integrate source-linked isolate sample items, received-isolate provenance
      and shared referrals/returns without double accessioning.
- [ ] Keep sender/local identification distinct and explicitly place extra returns.
- [ ] Pass [V09](plan.md#v09--isolate-and-referral).

**Acceptance:** an isolate can be labelled/referred; a received isolate skips
primary inoculation and preserves the producer of each identification/result.

## 10 — AST reporting

**Tracking:** [OGC-1433](https://uwdigi.atlassian.net/browse/OGC-1433).

**Prerequisites:** V09 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Workflow panel defaults and whole-run reporting selection where agents overlap.

**Status:** `[ ]`. Sources: [panels/interpretation][ast], [mock][mock].

- [ ] Replace workflow-based panels with the agreed interpretation model;
      integrate supplementary panels and explicit per-agent report selection.
- [ ] Preserve versions, raw measurements, QC, provenance and reasoned attempts,
      overrides/reverts; remove retired defaults and expectations.
- [ ] Pass [V10](plan.md#v10--ast).

**Acceptance:** overlapping panels report one selected reading per agent while
both attempts and their original measurements remain attributable.

## 11 — DST rules

**Tracking:** [OGC-1434](https://uwdigi.atlassian.net/browse/OGC-1434).

**Prerequisites:** V10 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** TB-profile execution assumptions; no hard-coded instrument behavior.

**Status:** `[ ]`. Sources: [classification/reconciliation][dst], [mock][mock].

- [ ] Add WHO critical-concentration interpretation and derived classification
      with clinically reviewed expected values; no vendor/profile-ID special cases.
- [ ] Prove discordance resolution and the NTM off-ramp using instrument evidence.
- [ ] Pass [V11](plan.md#v11--dst).

**Acceptance:** unresolved discordance blocks validation; NTM receives neither
the MTB cascade nor its resistance classification.

## 08 — incoming results

**Tracking:** [OGC-1435](https://uwdigi.atlassian.net/browse/OGC-1435).

**Prerequisites:** V11 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Existing-run-only placement assumptions and duplicate result copies.

**Status:** `[ ]`. Sources: [placement][incoming], [mock][mock].

- [ ] Add case-level holding/explicit placement; preserve existing-row ingestion
      and shared unmatched-order reconciliation.
- [ ] Prove duplicate-send handling, failed-placement retention, reasoned moves
      and post-final amendment gating with one result identity.
- [ ] Apply the draft placement-review rights; keep specimen/result ownership
      stable, update intended isolate links atomically, and retain producer,
      actor/time and original/target audit through moves and delivery.
- [ ] Pass [V08](plan.md#v08--incoming).

**Acceptance:** an unexpected result waits until placed, reports once, and cannot
change a finalized case without an amendment.
Placement cannot silently transfer ownership to the wrong specimen or isolate.

## 12 — releases and reports

**Tracking:** [OGC-1436](https://uwdigi.atlassian.net/browse/OGC-1436).

**Prerequisites:** V08 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Printed REMARK/mapping dependence after electronic continuity proof; linear release/work-state assumptions.

**Status:** `[ ]`. Sources: [choices][reports], [release rules][release],
[patient report][patient-report], [mock][mock].

- [ ] Separate work/release state; integrate shared versions/signatures, report
      groups, internal-only controls and attached notes.
- [ ] Remove printed REMARK/mapping dependence only after proving electronic
      delivery remains correct; no silent loss of delivered content.
- [ ] Prove distinct clinical-release and export-ready wording; check configured
      display names versus generated identifiers without stripping legitimate labels.
- [ ] Pass [V12](plan.md#v12--reports).

**Acceptance:** repeat partial releases permit continued culture work; final
prints selected validated results; an amendment preserves the issued original
and patient results show the current amended version.

## 13 — critical callbacks

**Tracking:** [OGC-1437](https://uwdigi.atlassian.net/browse/OGC-1437).

**Prerequisites:** V12 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Duplicate call entry and disconnected callback reporting.

**Status:** `[ ]`. Sources: [calls][callbacks], [mock][mock].

- [ ] Integrate case calls with shared callbacks/alerts/clearance, including
      rule-driven findings, outcomes and repeated attempts; no duplicate entry.
- [ ] Verify shared report placement and the draft callback clock from validation.
- [ ] Pass [V13](plan.md#v13--callbacks).

**Acceptance:** one attempt appears once in the case/log/report, with synchronized
follow-up and acknowledgement.

## 07 — bench work and labels

**Tracking:** [OGC-1438](https://uwdigi.atlassian.net/browse/OGC-1438).

**Prerequisites:** V13 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Old workflow filters, offline save/replay expectations and batch/run assumptions for bench sheets.

**Status:** `[ ]`. Sources: [Worklist][bench], [labels][labels], [mock][mock].

- [ ] Add scoped due/bulk actions with named skips, offline write blocking and
      stable URL/focus; extend Workplan print records/Open sheet and label presets.
- [ ] Add no culture batch/run model or AMR-only printing framework.
- [ ] Pass [V07](plan.md#v07--bench).

**Acceptance:** a sheet reopens its saved order with current row state; bulk work
affects only eligible authorized rows; a label locates its case row.

## 14 — patient history

**Tracking:** [OGC-1439](https://uwdigi.atlassian.net/browse/OGC-1439).

**Prerequisites:** V07 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Competing history/matching queries and automatic repeat suppression.

**Status:** `[ ]`. Sources: [history/repeats][history], [mock][mock].

- [ ] Reuse patient-results access and first-isolate matching for history and
      reasoned reference to previous susceptibility; never suppress automatically.
- [ ] Prove the clinical window, unit access and TB follow-up without changing
      surveillance deduplication semantics.
- [ ] Pass [V14](plan.md#v14--history).

**Acceptance:** an earlier permitted case is inspectable, the reference is
reviewable/audited, and the technician can still order a new panel.

## 15 — environmental and export populations

**Tracking:** [OGC-1440](https://uwdigi.atlassian.net/browse/OGC-1440).

**Prerequisites:** V14 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Workflow-type export selection and patient assumptions for site subjects.

**Status:** `[ ]`. Sources: [population rules][populations],
[environmental design][environment], [mock][mock].

- [ ] Apply case mechanics to a site subject with order/sample-type/lab-unit/site grouping, domain fields
      and certificate output; no fake patient.
- [ ] Apply track/purpose/provenance selection and preserve cohort/no-growth
      meaning for downstream reporting; no national aggregator.
- [ ] Prove the export mappings supported by the source, including missing
      values; no guessed patient-origin classification.
- [ ] Pass [V15](plan.md#v15--populations).

**Acceptance:** environmental/EQA/external data enter only permitted populations;
referred-in local work preserves original specimen identity; exclusions explain why.

## 16 — complete cutover acceptance

**Tracking:** [OGC-1441](https://uwdigi.atlassian.net/browse/OGC-1441).

**Prerequisites:** V15 accepted at its gate; shared owners linked before changing their behavior.

**Removals:** Any remaining superseded runtime, storage, seed, configuration and test consumer.

**Status:** `[ ]`. Sources: [preservation][preserved],
[functional acceptance][acceptance], [mock][mock].

- [ ] Prove retired runtime/storage/config/test consumers are gone; retain only
      immutable migration history and the one-way disposable upgrade fixture.
- [ ] Review source-AC coverage, security, desktop/mobile and keyboard evidence;
      every required outcome is implemented or explicitly outside scope.
- [ ] State excluded micro QC and isolate-storage capabilities explicitly in CPHL
      acceptance material; do not imply that AST QC handling fills those gaps.
- [ ] After V01–V15 implementation, resolve the recorded CI issues in bounded
      commits/PRs within `gh stack`; rerun the affected checks and full local CI.
- [ ] Pass all three GitHub checkpoints on the final stack revision, inspect
      skipped checks and retain exact evidence. Pending/failing CI blocks final
      acceptance and merge, not earlier implementation progression.
- [ ] Run the [final gate](plan.md#final-integration-gate); obtain migration/
      deployment approval and human acceptance on that revision.

**Acceptance:** V2-only AMR runtime/persistence, preserved clinical/audit meaning,
no competing legacy specification and no unexplained acceptance gap.

## Acceptance coverage

All **112 existing AC-V2 criteria** have exactly one primary owner below. Each
row links the functional criterion; dependent gates are also required before
acceptance. V00 verifies this mapping, not application behavior. All listed
application tests remain implementation deliverables until run on the slice.

| Criterion           | Primary owner | Dependent verification                                         |
| ------------------- | ------------- | -------------------------------------------------------------- |
| [AC-V2-01][ac-1]    | V02           | V01 migration; V07 worklist                                    |
| [AC-V2-02][ac-2]    | V04           | V02 routing; V11 reflex; V12 direct-only release               |
| [AC-V2-03][ac-3]    | V02           | V03 context                                                    |
| [AC-V2-04][ac-4]    | V02           | V07 destination worklist; V12 final lock                       |
| [AC-V2-05][ac-5]    | V02           | V16 consumer scan                                              |
| [AC-V2-06][ac-6]    | V03           | V02 reception save                                             |
| [AC-V2-07][ac-7]    | V03           | V12 final checklist                                            |
| [AC-V2-08][ac-8]    | V04           | V02 catalog eligibility                                        |
| [AC-V2-09][ac-9]    | V04           | V12 provenance; V15 external populations                       |
| [AC-V2-10][ac-10]   | V06           | V05 media                                                      |
| [AC-V2-11][ac-11]   | V07           | V06 timing; V16 offline                                        |
| [AC-V2-12][ac-12]   | V06           | V07 attention                                                  |
| [AC-V2-13][ac-13]   | V06           | V07 extended due date                                          |
| [AC-V2-14][ac-14]   | V15           | V04 flag; V12 patient delivery                                 |
| [AC-V2-15][ac-15]   | V09           | V12 partial release                                            |
| [AC-V2-16][ac-16]   | V08           | V09 isolates; V10 susceptibility                               |
| [AC-V2-17][ac-17]   | V08           | V10 review; V12 locking                                        |
| [AC-V2-18][ac-18]   | V08           | V09 ownership; V12 amendment                                   |
| [AC-V2-19][ac-19]   | V10           | V12 selected output                                            |
| [AC-V2-20][ac-20]   | V12           | V06 continued work                                             |
| [AC-V2-21][ac-21]   | V10           | V11 WHO interpretation                                         |
| [AC-V2-22][ac-22]   | V16           | All UI slices; V16 locale audit                                |
| [AC-V2-23][ac-23]   | V06           | V07 read log                                                   |
| [AC-V2-24][ac-24]   | V04           | V12 component print                                            |
| [AC-V2-25][ac-25]   | V08           | V09 identification history                                     |
| [AC-V2-26][ac-26]   | V09           | V08 return                                                     |
| [AC-V2-27][ac-27]   | V11           | V10 versioned results; V12 print                               |
| [AC-V2-28][ac-28]   | V04           | V12 placement                                                  |
| [AC-V2-29][ac-29]   | V12           | V06 continued work                                             |
| [AC-V2-30][ac-30]   | V04           | V09 isolate sample                                             |
| [AC-V2-31][ac-31]   | V04           | V12 patient delivery; V15 independent eligibility              |
| [AC-V2-32][ac-32]   | V04           | V12 amendment                                                  |
| [AC-V2-33][ac-33]   | V04           | V12 report choices                                             |
| [AC-V2-34][ac-34]   | V04           | V11 discordance                                                |
| [AC-V2-35][ac-35]   | V08           | V02 shared order integrity                                     |
| [AC-V2-36][ac-36]   | V08           | V11 molecular results                                          |
| [AC-V2-37][ac-37]   | V04           | V08 deduplication                                              |
| [AC-V2-38][ac-38]   | V04           | V09 organism; V10 panels                                       |
| [AC-V2-39][ac-39]   | V04           | V09 provenance                                                 |
| [AC-V2-40][ac-40]   | V04           | V12 flags; V13 critical call                                   |
| [AC-V2-41][ac-41]   | V16           | V04/V05/V07/V09/V10/V11/V12/V13 preservation gates             |
| [AC-V2-42][ac-42]   | V01           | V02 collisions and separate membership; V12 history            |
| [AC-V2-43][ac-43]   | V09           | V10 panel defaults; V11 NTM                                    |
| [AC-V2-44][ac-44]   | V15           | V12 final release                                              |
| [AC-V2-45][ac-45]   | V12           | V08 holding; every mutation slice                              |
| [AC-V2-46][ac-46]   | V12           | V08 late placement; V09 isolate                                |
| [AC-V2-47][ac-47]   | V11           | V10 QC/expert review; V12 final gate                           |
| [AC-V2-48][ac-48]   | V13           | V12 report; shared callbacks                                   |
| [AC-V2-49][ac-49]   | V02           | Every write slice; V07 worklist and direct links               |
| [AC-V2-50][ac-50]   | V15           | V03 purpose; future consumers excluded from delivery claim     |
| [AC-V2-51][ac-51]   | V04           | Shared reagent eligibility                                     |
| [AC-V2-52][ac-52]   | V12           | Shared patient results                                         |
| [AC-V2-53][ac-53]   | V12           | V15 environmental certificate                                  |
| [AC-V2-54][ac-54]   | V06           | V02 explicit sets; V12 independent release                     |
| [AC-V2-55][ac-55]   | V06           | V09 lineage                                                    |
| [AC-V2-56][ac-56]   | V10           | V09 sources; V12 one reading per agent                         |
| [AC-V2-57][ac-57]   | V09           | V04 result; V07 label                                          |
| [AC-V2-58][ac-58]   | V15           | V02 grouping; V03 per-case purpose                             |
| [AC-V2-59][ac-59]   | V04           | Shared Runs/QC; V12 validation                                 |
| [AC-V2-60][ac-60]   | V12           | V02 rights; V04 validation                                     |
| [AC-V2-61][ac-61]   | V04           | V12 complete component rendering                               |
| [AC-V2-62][ac-62]   | V09           | Shared shipment; V08 returns                                   |
| [AC-V2-63][ac-63]   | V02           | V01 identities; V12 no-result boundary                         |
| [AC-V2-64][ac-64]   | V05           | V07 bulk/undo stock invariant                                  |
| [AC-V2-65][ac-65]   | V05           | Shared Inventory visibility                                    |
| [AC-V2-66][ac-66]   | V05           | V06 row defaults                                               |
| [AC-V2-67][ac-67]   | V04           | V12 notes print                                                |
| [AC-V2-68][ac-68]   | V04           | V12 withheld notes                                             |
| [AC-V2-69][ac-69]   | V05           | V07 bulk defaults and unchanged stock                          |
| [AC-V2-70][ac-70]   | V04           | V06 bottle source; V12 partial; V13 critical                   |
| [AC-V2-71][ac-71]   | V06           | V09 source lineage                                             |
| [AC-V2-72][ac-72]   | V06           | V07 explicit confirmation                                      |
| [AC-V2-73][ac-73]   | V07           | V09 isolate; V10 panel targets                                 |
| [AC-V2-74][ac-74]   | V14           | V09 identification; V10 cancelled default; V12 reference print |
| [AC-V2-75][ac-75]   | V09           | V08 placement; V12 direct release                              |
| [AC-V2-76][ac-76]   | V07           | V05 lot eligibility; V06 rows                                  |
| [AC-V2-77][ac-77]   | V04           | V06 source; V12 withheld notes                                 |
| [AC-V2-78][ac-78]   | V07           | V02 sample grouping; V05 defaults                              |
| [AC-V2-79][ac-79]   | V07           | V06 reading/audit                                              |
| [AC-V2-80][ac-80]   | V07           | V06 eligibility; V12 no automatic release                      |
| [AC-V2-81][ac-81]   | V07           | V16 deleted consumers                                          |
| [AC-V2-82][ac-82]   | V06           | V04 catalog results; V09 isolates                              |
| [AC-V2-83][ac-83]   | V06           | V07 labels; V09 parentage                                      |
| [AC-V2-84][ac-84]   | V04           | V12 amendment                                                  |
| [AC-V2-85][ac-85]   | V07           | Shared Workplan; V16 privacy/keyboard                          |
| [AC-V2-86][ac-86]   | V07           | V05 links                                                      |
| [AC-V2-87][ac-87]   | V07           | V06 concurrent readings; V02 rights                            |
| [AC-V2-88][ac-88]   | V02           | V03 Program; V07 transfer visibility                           |
| [AC-V2-89][ac-89]   | V06           | V07 bulk timing                                                |
| [AC-V2-90][ac-90]   | V03           | V02 sample ownership; shared reception                         |
| [AC-V2-91][ac-91]   | V02           | V12 independent releases                                       |
| [AC-V2-92][ac-92]   | V03           | V15 track selection                                            |
| [AC-V2-93][ac-93]   | V06           | V07 future due time                                            |
| [AC-V2-94][ac-94]   | V07           | V06 reason/audit                                               |
| [AC-V2-95][ac-95]   | V06           | V08 analyzer provenance                                        |
| [AC-V2-96][ac-96]   | V12           | V10 selection; shared patient report                           |
| [AC-V2-97][ac-97]   | V04           | V06 positive signal; V12 validation                            |
| [AC-V2-98][ac-98]   | V10           | V09 isolate target                                             |
| [AC-V2-99][ac-99]   | V03           | Shared questionnaires; V12 required fields                     |
| [AC-V2-100][ac-100] | V04           | V12 blank component suppression                                |
| [AC-V2-101][ac-101] | V06           | V07 printed codes                                              |
| [AC-V2-102][ac-102] | V04           | V06 idempotent culture signal                                  |
| [AC-V2-103][ac-103] | V02           | V02 atomic preview/save; shared order integrity                |
| [AC-V2-104][ac-104] | V02           | V06 bottle rows; shared reception                              |
| [AC-V2-105][ac-105] | V04           | V12 release; shared validation                                 |
| [AC-V2-106][ac-106] | V09           | V12 required original specimen; V15 referred-in                |
| [AC-V2-107][ac-107] | V15           | V09 producer identity                                          |
| [AC-V2-108][ac-108] | V04           | V12 provenance                                                 |
| [AC-V2-109][ac-109] | V05           | V07 bulk lots                                                  |
| [AC-V2-110][ac-110] | V05           | V07 bulk/undo; shared Inventory                                |
| [AC-V2-111][ac-111] | V03           | Shared reception and configured FHIR mirror                    |
| [AC-V2-112][ac-112] | V07           | V06 controlled clock; OGC-1411 laboratory month boundary       |

Supplemental gates: V02 proves idempotent/all-or-nothing shared order saves and
separate transfers; V05 proves no culture stock writes; V08 proves failed placement
retention and producer/actor audit; V10 proves measurements and historical versions;
V12 proves readiness matches available release actions (OGC-1384/1385 history,
OGC-1386 still open); V15 proves collection-month membership independently of
deduplication chronology, missing mappings and preview/generation parity. V07
retains OGC-1411 month-boundary regression. V16 checks keyboard/mobile behavior,
the existing 200-row Worklist <2 s, case (5 isolates × 16 agents × 30 events) <1 s,
filter <300 ms and export performance budgets in the nonfunctional source.

The twelve `Amr*Test` classes in plan.md are absent at this baseline. Those and
the planned `amr-*.spec.ts` browser tests are deliverables, never passing evidence.

[ac-1]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-01
[ac-2]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-02
[ac-3]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-03
[ac-4]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-04
[ac-5]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-05
[ac-6]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-06
[ac-7]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-07
[ac-8]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-08
[ac-9]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-09
[ac-10]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-10
[ac-11]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-11
[ac-12]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-12
[ac-13]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-13
[ac-14]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-14
[ac-15]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-15
[ac-16]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-16
[ac-17]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-17
[ac-18]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-18
[ac-19]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-19
[ac-20]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-20
[ac-21]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-21
[ac-22]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-22
[ac-23]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-23
[ac-24]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-24
[ac-25]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-25
[ac-26]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-26
[ac-27]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-27
[ac-28]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-28
[ac-29]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-29
[ac-30]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-30
[ac-31]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-31
[ac-32]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-32
[ac-33]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-33
[ac-34]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-34
[ac-35]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-35
[ac-36]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-36
[ac-37]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-37
[ac-38]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-38
[ac-39]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-39
[ac-40]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-40
[ac-41]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-41
[ac-42]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-42
[ac-43]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-43
[ac-44]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-44
[ac-45]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-45
[ac-46]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-46
[ac-47]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-47
[ac-48]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-48
[ac-49]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-49
[ac-50]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-50
[ac-51]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-51
[ac-52]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-52
[ac-53]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-53
[ac-54]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-54
[ac-55]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-55
[ac-56]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-56
[ac-57]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-57
[ac-58]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-58
[ac-59]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-59
[ac-60]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-60
[ac-61]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-61
[ac-62]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-62
[ac-63]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-63
[ac-64]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-64
[ac-65]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-65
[ac-66]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-66
[ac-67]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-67
[ac-68]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-68
[ac-69]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-69
[ac-70]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-70
[ac-71]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-71
[ac-72]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-72
[ac-73]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-73
[ac-74]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-74
[ac-75]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-75
[ac-76]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-76
[ac-77]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-77
[ac-78]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-78
[ac-79]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-79
[ac-80]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-80
[ac-81]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-81
[ac-82]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-82
[ac-83]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-83
[ac-84]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-84
[ac-85]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-85
[ac-86]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-86
[ac-87]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-87
[ac-88]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-88
[ac-89]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-89
[ac-90]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-90
[ac-91]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-91
[ac-92]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-92
[ac-93]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-93
[ac-94]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-94
[ac-95]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-95
[ac-96]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-96
[ac-97]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-97
[ac-98]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-98
[ac-99]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-99
[ac-100]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-100
[ac-101]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-101
[ac-102]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-102
[ac-103]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-103
[ac-104]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-104
[ac-105]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-105
[ac-106]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-106
[ac-107]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-107
[ac-108]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-108
[ac-109]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-109
[ac-110]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-110
[ac-111]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-111
[ac-112]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#ac-v2-112
[mock]: https://digi-uw.github.io/openelis-work/#/microbiology/microbiology-v2-amendments
[catalog]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-01.1
[routing]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-02.4
[context]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-03.1
[culture]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-05.1
[initial]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-06.1
[ast]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-07.1
[referral]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-08.0
[incoming]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-09.1
[isolates]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-10.1
[reports]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-11.1
[bench]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-12.1
[notes]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-13.1
[dst]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-14.1
[preserved]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#a-16-preservation-boundary
[release]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-17.1
[callbacks]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-18.1
[populations]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-19.1
[labels]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-20.1
[history]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#fr-21.1
[acceptance]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/amr-micro-v2-amendments.md#acceptance-criteria
[environment]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/microbiology/m-18-environmental-microbiology.md
[patient-report]: https://github.com/DIGI-UW/openelis-work/blob/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b/designs/reports/patient-report-redesign.md
