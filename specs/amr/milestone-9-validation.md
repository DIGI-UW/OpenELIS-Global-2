# AMR V2 — workflow and design audit

## Decision

**Not ready for workflow sign-off.** Core result, culture, isolate, referral,
and report actions work in the local application, but the default workspace
cannot complete required clinical information and navigation exposes different
case implementations. Resolve the high-priority findings before UAT acceptance
or workflow recording.

This is a validation report, not an implementation change or a CI sign-off.
No fixes, public issue postings, or shared UAT acceptance changes were made.
The implementation response is planned in [Milestone 9](milestone-9.md); this
report preserves the observations on the audited revision.

## Candidate, reference and method

- Audited on 14 October 2026, on `codex/amr-v2-ux-validation` at
  `9471be12ba997998bd3ddf64db69551b6e8f8aa0` (rebased M8).
- Application: `https://localhost:38220`, source development stack.
- Product reference: openelis-work commit
  `c9722f07304c3170913d565876a6dd085c388a7d`,
  [AMR V2 design](https://github.com/DIGI-UW/openelis-work/blob/c9722f07304c3170913d565876a6dd085c388a7d/designs/microbiology/amr-micro-v2-amendments.md), rendered TB and blood-culture
  previews, plus the environmental workflow reference. Product references govern
  visible behavior; implementation choices remain owned by this repository.
- Six fresh synthetic cases were provisioned through the application scenario
  service. Subsequent workflow actions were performed through the browser.
  Provisioning is test preparation, not proof that order entry/routing works.
- Login used the local development administrator. This pass does not certify
  restricted-role behavior or self-validation settings.
- Screenshots and synthetic API evidence are committed in
  [the evidence directory](evidence/m9/2026-10-14/).
  Screenshots include the application frame. Desktop DOM measurement was
  1280 × 720; the reference desktop image is 1440 × 1000, so the comparison
  is not pixel-for-pixel. Earlier narrow-panel captures were also taken. The viewport override
  was restored. This is not a complete mobile or accessibility audit.
- Trusted HTTPS worked without bypassing a certificate warning. Earlier TLS
  blockage and older-candidate CI notes are not current findings.

## Confirmed findings and remediation

### UX-01 — P1: Required clinical information cannot be completed from the default case

**Observed:** Open `UATMICRO55E40C21F0` using its ordinary case link. The page
has Program and Culture purpose, but no patient origin, clinical history,
admission date, prior antibiotics, or Program questionnaire editor. Select
**Report and release**: the final-release blocker explicitly requires
**Patient origin**. It is plain text without a link to an editor. Other blockers
also exist on this fresh case; the finding is the unreachable required field,
not a claim that patient origin is its only blocker.

**Expected:** Case information holds these fields; required-before-final fields
are marked and the checklist links to them (reference FR-03.1–03.7).

**Evidence:** [01-default-case.jpg](evidence/m9/2026-10-14/01-default-case.jpg), [02-default-release.jpg](evidence/m9/2026-10-14/02-default-release.jpg),
[16-release-blocker-detail.jpg](evidence/m9/2026-10-14/16-release-blocker-detail.jpg). `CaseViewShell.jsx:179` renders
`CaseContextEditor`, not `OrderDetailPanel`; `MicrobiologyPage.jsx:12` selects
this shell on the ordinary route.

**Remediation:** Put the full clinical context and configured Program questions
in the canonical case workspace, with links from release blockers. Prove the
journey with initially blank required fields, then save/reload and clear each
corresponding blocker. Do not rely on the reporting fixture, which prepopulates
clinical context.

### UX-02 — P1: Case entry routes expose different workflows and inconsistent state

**Observed:** The same clinical case at its ordinary URL shows **Active** and
**Diagnostic**, initial testing and notes. Adding `?view=workbench` opens a
separate accordion workbench, showing **Received**, purpose **Unspecified**, and
**Case info Complete**, despite missing patient origin. It adds Order detail,
critical communication, nonconformance and amendment sections absent from the
default shell. The ordinary final-report case `UATMICRO55A3F1531A` locks editing
but still says **Active** and offers no visible action to open an amendment.
This is not a claim that Active alone contradicts the separate release state;
the problem is that meaningful work/release state and correction access are
missing from the normal case header/workflow.

**Expected:** One coherent case workspace, work stage separate from release
state, and reasoned amendment access after final (A04, FR-17.1, FR-17.6–17.7).

**Evidence:** [01-default-case.jpg](evidence/m9/2026-10-14/01-default-case.jpg), [03-alternate-case.jpg](evidence/m9/2026-10-14/03-alternate-case.jpg),
[11-final-report.jpg](evidence/m9/2026-10-14/11-final-report.jpg). `MicrobiologyPage.jsx` explicitly switches component for
`view=workbench`, any `section`, and `grain=cultures/ast`.

**Remediation:** Choose a single case implementation and map entry URLs to its
section state. Retain all required capabilities and display the same saved
purpose, work stage and release state across direct, worklist, bench, scanner
and related-case links. Include a final → reasoned amendment → amended report
acceptance journey.

### UX-03 — P2: Culture-bench navigation discards the lab-unit filter

**Observed:** In the default worklist, search `UATMICRO9CDF3A0670` and select
lab unit `M4 9CDF3A0670`. One case is returned. Click **Culture**: search is
retained, but Lab unit becomes **All permitted lab units**, while the URL still
contains `labUnitId=723`. The subsequent **AST runs** action opens another
worklist with different controls and a default monthly reporting period.

**Evidence:** [07-filtered-worklist.jpg](evidence/m9/2026-10-14/07-filtered-worklist.jpg), [08-bench-filter-loss.jpg](evidence/m9/2026-10-14/08-bench-filter-loss.jpg).
`CultureWorklist.jsx:43` initializes selected units to an empty list;
its request adds only `labUnitIds` from that list, ignoring the incoming
`labUnitId`. This confirms the filter is not merely mislabeled. Existing access
restrictions still apply; this is not evidence of unauthorized access.

**Remediation:** Share and round-trip applicable filters across case, culture
and AST scopes. Keep a consistent lab-unit control, and explicitly handle
scope-specific filters. Test two units with overlapping search matches, scope
switching, back navigation and reload.

### UX-04 — P2: Waiting incoming results are invisible until the section is opened

**Observed:** `UATMICRO4F51A2BAA5` opens on Initial testing with no waiting badge,
count or alert. Selecting **Incoming results** reveals a result waiting for
placement. Placing it succeeds and it appears in Additional testing as value
12, Awaiting validation.

**Expected:** Pending evidence is discoverable from the case, as in the prominent
Incoming results panel in reference A09, with Needs attention support in A12.

**Evidence:** [05-incoming-hidden.jpg](evidence/m9/2026-10-14/05-incoming-hidden.jpg), [06-incoming-waiting.jpg](evidence/m9/2026-10-14/06-incoming-waiting.jpg).
`CaseTestingWorkspace.jsx` renders the panel only for the selected INCOMING
section. The selector has no waiting count.

**Remediation:** Show pending counts and an actionable alert independent of the
selected section; verify waiting, placed, declined and post-final states.

### UX-05 — P2: Sample and header context omit required bench information

**Observed:** The clinical Samples table contains Lab Number, Specimen, Status,
and Collection date only. It provides no collection time, collector, body site,
side, container, or Correct site entry point. The default header does not show
the reporting track, order priority, or a useful work-stage summary.

**Expected:** Per-sample identity, container, site/side, collected-at and collector,
with correction through the saved order workflow (FR-04.1–04.3).

**Evidence:** [01-default-case.jpg](evidence/m9/2026-10-14/01-default-case.jpg), [15-default-desktop.jpg](evidence/m9/2026-10-14/15-default-desktop.jpg), compared with
[14-reference-desktop.jpg](evidence/m9/2026-10-14/14-reference-desktop.jpg); `CaseViewShell.jsx:179–247` defines the reduced table.
The missing columns/actions are confirmed; multi-bottle data propagation has
not yet been exercised.

**Remediation:** Expose the existing sample context and correction entry point.
Validate four bottles with different sites and collection times, including
related/split cases and transfers.

### UX-06 — P2: Dates mix clocks, and the referral summary loses handoff precision

**Observed:** A result entered with its case note in one interaction displays
Performed by **10/15/2026**, while the note shows **10/14/2026 5:46 PM**.
Culture timings use the browser clock without a timezone label. The lab API
reports **Africa/Nairobi**, while the browser is on the Mac's local timezone.
For `UATMICROFBBD14160C`, Handoff Date **15/10/2026**, Time **10:30** produces a
referral summary showing **10/14/2026 5:00 PM**.

**Source trace:** `MicroIsolateWorkspaceServiceImpl.java:110–114` projects
`Referral.getSentDate()` into `sentAt`, rather than the subcontract handoff
datetime. `CaseIsolateWorkspace.jsx:366,696` formats that instant in the browser
zone. The returned `sentAt` is `1792011600000`, midnight on 15 October in the lab
zone. Thus the displayed value does not represent the entered handoff time.
The subcontract's stored handoff value was not independently read; do not infer
that the underlying handoff record was destroyed.

**Evidence:** [13-isolate-referral.jpg](evidence/m9/2026-10-14/13-isolate-referral.jpg), [clock.json](evidence/m9/2026-10-14/clock.json), [isolate.json](evidence/m9/2026-10-14/isolate.json), plus
`CaseCultureWorkspace.jsx:359` and `CaseTestingWorkspace.jsx:542,804–805`.
Reference FR-05.3 specifies the laboratory clock for culture timing.

**Remediation:** Make lab dates and timestamp zones explicit and consistent.
Use the actual handoff datetime in the referral summary. Test cross-midnight
browser/lab zones and round-trip both date and minute precision.

### UX-07 — P2: Culture actions lack the decision aids shown in the reference

**Observed:** Extend incubation accepts 12 Hours and a reason but shows no
**New incubation end** before saving. After saving, the end moves correctly
from 16 October 17:47 to 17 October 05:47; inoculated time and next check stay
unchanged. The duration remains 48 Hours and the extension is in a separate
history table, without a duration-adjacent extension tag. Growth outcome opens
with an empty Positive at field; saving supplies a time, but no computed
positivity preview is shown. The row reports time to positivity as decimal
hours (`0.01`), not days/hours/minutes. Medium selection is a long native select,
not a searchable picker with tracked/not-tracked labels.

**Expected:** Searchable medium selection, explicit tracking context, previewed
new end, visible extension indicator, and exact readable positivity timing
(FR-05.1b, FR-05.3a, FR-05.4b).

**Evidence:** [04-culture-persistence.jpg](evidence/m9/2026-10-14/04-culture-persistence.jpg); observed inoculation, extension and
positive-outcome dialogs. `CaseCultureWorkspace.jsx:292,298,542–543`.

**Remediation:** Add previews and human-readable timing, preserve the working
clock arithmetic, and use the searchable medium/lot interaction. Verify the
preview matches the saved row and that lot eligibility remains enforced.

### UX-08 — P2: Attribution and note presentation are inconsistent

**Observed:** Culture readings, extensions, outcomes and isolate identification
history display actor **1**. Case notes and test results display the person's
name. The Case notes table lacks a Type column even though entry supports
**Send with Result**. Culture and isolate actions have no Notes(count) control
matching the reference; a reading note is not a substitute for the shared
internal/external note workflow.

**Expected:** Recognizable author attribution and shared note type/history on
case, result, culture, isolate and drug rows (FR-13.1–13.1c).

**Evidence:** [04-culture-persistence.jpg](evidence/m9/2026-10-14/04-culture-persistence.jpg), [13-isolate-referral.jpg](evidence/m9/2026-10-14/13-isolate-referral.jpg),
[01-default-case.jpg](evidence/m9/2026-10-14/01-default-case.jpg). These are UI gaps; numeric attribution is still retained
in persistence and is not evidence of a missing audit record.

**Remediation:** Resolve actor IDs to display names; show note type and counts;
reuse shared note behavior across supported attachments. Verify internal notes
stay off reports and external notes appear beside the intended result.

### UX-09 — P2: Validation errors are generic and persist into unrelated dialogs

**Observed:** Start a Broth microdilution run on `UATMICRO55A3F1531A`, enter
1 and 8, and attempt to validate while both agents show No matching breakpoint.
The save is rejected, but the message only says to check permissions, required
fields, and whether the case changed. Cancel and open Override interpretation:
the previous error appears there before any new submission. Reasoned overrides
then allow successful validation.

**Evidence:** [10-ast-generic-error.jpg](evidence/m9/2026-10-14/10-ast-generic-error.jpg); `CaseAstWorkspace.jsx:98–118,150–153,518–521`.
This fixture has no matching broth rule; that configuration condition is not
itself classified as a product defect.

**Remediation:** Display actionable server validation reasons beside the affected
items and reset action-specific errors when opening a different dialog. Keep
the server guard intact.

## Reference inconsistency

The pinned blood-culture HTML preview marks Date of admission as needed before
final report, whereas FR-03.5 explicitly makes admission date optional and says
its absence must not block release. Reconcile the preview with the written rule;
do not introduce a mandatory admission date merely to match the screenshot.

## Workflow evidence

| Journey                        | Observed result                                                                                                             | Limits                                                                                                   |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| Trusted login and case access  | Passed on local trusted HTTPS                                                                                               | Administrator only; not a role matrix                                                                    |
| Multi-component initial result | Invalid numeric text blocked Save; 12 and 24 saved and survived later navigation                                            | Not every result type or calculation                                                                     |
| Case note                      | Saved with author/time and persisted                                                                                        | Internal/external report placement not exercised                                                         |
| Tested elsewhere               | External performer/date saved; External tag displayed with existing results                                                 | Person selection used; laboratory search path not completed                                              |
| Culture and subculture         | Tracked lot required; linked duration/check interval populated; parent and child created                                    | Expired-lot and future/receipt-time negative cases not exercised                                         |
| Reading and extension          | Two readings retained after reload; time edit disappeared after first read; 12-hour extension preserved original clock      | Long-running due/final-read transitions not exercised                                                    |
| Culture testing                | Gram stain shortcut attached a test to the parent row                                                                       | Gram stain result itself not entered in this pass                                                        |
| Independent outcomes           | Child No growth persisted while parent remained Incubating                                                                  | Instrument negative confirmation and late-growth amendment not exercised                                 |
| Isolate identification         | Picked from growth row; preliminary then final identification; both history rows and reason retained                        | Received-isolate mismatch flow not exercised                                                             |
| Referral                       | Created isolate specimen on demand; local referral saved as Draft                                                           | Dispatch and returned results not exercised                                                              |
| Incoming evidence              | Waiting result placed; appeared in Additional testing with value 12 awaiting validation                                     | Analyzer transport and cross-case moves not exercised                                                    |
| AST and reporting              | Two readings saved; unresolved run rejected; reasoned overrides validated; partial v1 and final v2 created; editing locked  | Synthetic manually interpreted data; breakpoint correctness, QC, repeats and DST conflicts not certified |
| Environmental workflow         | Site/point shown without patient fields; purpose/3 replicates persisted; No growth allowed final release and locked editing | Compliance threshold semantics and report visual layout not certified                                    |
| Environmental certificate      | API returned HTTP 200, application/pdf, 37,923 bytes with PDF signature                                                     | Browser download event timed out and reset tool; successful user download and PDF contents not verified  |
| Worklist                       | Search + lab-unit filter returned the intended case                                                                         | Scope-switch filter loss confirmed; bulk actions/labels not exercised                                    |

Browser console inspection after the isolate/referral journey returned no captured
warnings/errors. This does not establish that every earlier page was error-free.

## Reproduction fixtures and retained evidence

[fixtures.json](evidence/m9/2026-10-14/fixtures.json)
contains the exact scenario keys and identifiers. Cases remain in the local stack
for reproducing findings:

| Purpose                   | Accession          | Case ID                              |
| ------------------------- | ------------------ | ------------------------------------ |
| Initial results/context   | UATMICRO55E40C21F0 | ddd32277-fcdf-4b53-8ac9-b1d53a4d2293 |
| Culture tree              | UATMICRO9CDF3A0670 | 108c7113-edca-4ae7-aa97-fedf2020f4a5 |
| Isolates/referral         | UATMICROFBBD14160C | 6c6a9ab9-5951-465f-ba32-c318297129a4 |
| Incoming placement        | UATMICRO4F51A2BAA5 | ec0f30b4-e507-44c7-9e71-7466883d7833 |
| AST/reports (now final)   | UATMICRO55A3F1531A | c13bf755-1e9f-47e9-84cf-0f226c9f5bbc |
| Environmental (now final) | UATMICRO3755F558F1 | 159e2e84-7864-441d-b5e8-9eb5b4bc32e7 |

The PDF response was retained locally as `environmental-certificate-response.bin`
and is not included in this committed evidence set; its contents remain unverified.
No authentication cookies, passwords or session tokens were saved in the evidence.

## Suggested remediation order and remaining acceptance

1. Resolve UX-01 and UX-02 together: one usable case workspace with required
   context, consistent state and amendment access.
2. Correct worklist scope preservation, pending-result visibility and sample
   context (UX-03–05).
3. Correct date/attribution presentation and actionable errors; complete culture
   decision aids (UX-06–09).
4. Rerun affected journeys with normal lab roles, missing context and realistic
   multi-bottle data. Add focused regression coverage for demonstrated bugs.
5. Complete the unexercised acceptance lanes: normal order routing, related cases,
   transfer/split access, block-self-validation, questionnaires, received-isolate
   mismatch, referral dispatch/return, instrument signals, bulk bench actions,
   labels, amendments, surveillance export contents, keyboard/mobile use, and
   report/PDF visual comparison. These are coverage gaps, not automatically defects.
6. Obtain workflow acceptance before publishing the UAT checklist or recordings.
