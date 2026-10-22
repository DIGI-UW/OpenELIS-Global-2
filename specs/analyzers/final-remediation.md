# Analyzer stack final remediation

Baseline synchronized on 21 October 2026. This checklist separates saved review
work from the branch synchronization needed to finish it. It does not certify a
release or authorize deployment.

## Current direction — interpret once

The 22 October audit supersedes the decimal-metadata/reprocessing proposal.
Follow [the boundary remediation plan](boundary-remediation.md): remove OE's duplicate parsing checks and preserve Bridge normalization across
confirmation, delivery, recovery, adoption and readiness. Retain local mapping
approval, compatibility, placement, QC, receipts and audit. The historical
progress below records the previous baseline, including gates now removed.
The replacement is implemented; its focused and full-stack validation must be
reported separately from that baseline. See the numbered cleanup inventory.

Baseline `fde241e1a6` passed all 16 full local CI groups (9,203 backend tests,
zero failures/errors, nine skipped). The audit refresh found only the expected
upstream-pin validation failing on GitHub. New implementation must receive its
own validation; the baseline does not prove the planned boundary.

Focused cleanup validation: Bridge `mvn test` passed 1,221 tests with four skipped;
OE mapping/import/contract/adoption suites passed 264 tests; mapping editor, error-message and
result-review frontend suites passed 129 tests. Formatting and whitespace checks
passed. These are focused results, not full-stack acceptance; the candidate must
pass complete local CI and current-head GitHub checkpoints before merge readiness.
Review follow-ups preserve delivered clinical answer codings for local target
validation, reject removed local components, and use the existing current mapping
assessment for type readiness. The audit-only status shortcut was deleted.

## Candidate and stack order

OpenELIS stack: #4770 → #4781 → #4784 → #4786 → #4787 → #4788 → #4789 →
#4790 → #4791 → #4792 → #4793 → #4794 → #4795 → #4797 → #4798.
Bridge stack: #101 → #102 → #104 → #105 → #106 → #107 → #108 → #109 →
#110 → #111 → #112 → #113.

OpenELIS #4798 records these candidate commits:

- Bridge #113: `e5a9e33ffd8490e9190873bee7a641cf028bc7dd`.
- Analyzer mock #62: `88b51f7c5ed50e953b8e80be5934162bcb138fd9`.

The Bridge pin includes exclusion defaults, full sender matching/display, and
private truststore configuration. Compose uses `BRIDGE_TRUSTSTORE_*` rather
than placing its password in the JVM's startup-echoed `JDK_JAVA_OPTIONS`.
Custom store passwords are now shared by generation, the Spring store/key
properties, and the Tomcat, FHIR and Bridge consumers.

The trusted submodule check requires upstream default-branch commits. Candidate
pins deliberately remain merge-blocked until the dependencies merge. After a
squash merge, replace each candidate pin with the resulting upstream commit and
rerun the trusted check. Do not weaken that check to accept this candidate.

Standalone OpenELIS #4768 and Bridge #88 remain independent changes. Mock #62 is
an independent host-query dependency included by the pin. Older mock #56 is
separate control-fixture work, has conflicts, and is not part of this candidate.

## Remediation progress — 21 October, latest working candidate

Bridge `474f8f2` fixes both outstanding Bridge findings. Full Bridge validation:
1,231 tests, four skipped; ASTM library tests passed. Both review threads were
replied to and resolved after pushing.

The current OpenELIS changes implement certificate upgrade/password consistency,
removed-reading protection, undeclared-code recovery, unavailable-profile handling,
component-order canonicalization, activation-history protection, failed-restore
compensation, frontend response fields, reporting menu text, and LOINC alias
normalization. Focused backend validation: 115 tests passed. Seven real-certificate
install/upgrade tests passed, including custom passwords and repeated startup.
Mindray browser validation distinguishes routine automated CBCs from the
manual-count fixture; exclusions are explicit lab decisions, not shipped defaults.
The corrected manual-count scenario passed analyzer shard 1 (40 tests);
analyzer shard 2 passed all 20 tests after narrowing its microbiology action
assertion to the relevant event row. These focused runs do not establish full CI. The repository-wide TypeScript check reports missing Node
ambient types across existing Playwright files; changed browser tests pass ESLint.

Connection-code overrides are now part of the confirmed interpretation. Changing
or removing them requires reconfirmation even when the profile fingerprint is
unchanged; unrelated host changes do not. The focused override/reading/import/
activation suite passes 101 tests. Spring TLS property resolution passes its test,
and the deployment-script group passes 64 tests (including seven certificate tests).
The trusted upstream-pin gate remains unchanged. No merge or deployment is authorized
by this progress record.

## Implemented review fixes

The reviewed findings now have implementation changes in the stack tips.
GitHub resolution is tracked separately from implementation and full CI.

| Finding                     | Correction and focused evidence                                                                                                                                                                               |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Shared certificates         | Both historical subjects and previously exported shared keys are replaced; seven real-certificate tests pass.                                                                                                 |
| Store passwords             | Generation and every consumer, including the HTTP client's private-key password, use the configured values; defaults remain supported.                                                                        |
| Removed readings            | Removed values/spellings differ; recovery rejects absent confirmed values.                                                                                                                                    |
| Undeclared-code recovery    | Explicitly confirmed undeclared codes recover when parsing agrees. GitHub run 39724746731 passed the mapping recovery scenario; explicit exclusion recovery also passed the corrected 40-test analyzer shard. |
| Connection overrides        | Changing/removing overrides requires reconfirmation even with an unchanged profile; unrelated host edits remain current.                                                                                      |
| Unreadable profile          | Delivery waits for a baseline; reconfirmation fails without changing the saved confirmation.                                                                                                                  |
| Component order             | Tuples are sorted by code/sub-identity before comparison.                                                                                                                                                     |
| API response fields         | Both reading-change fields are present in the frontend response type.                                                                                                                                         |
| Migrated activation history | A last activation date prevents permanent deletion.                                                                                                                                                           |
| Failed-restore retry        | Persistence failure preserves already-active runtime intent.                                                                                                                                                  |
| Reporting navigation        | The optional qualification assertion matches the current description; its separate reporting-profile browser qualification passed on `89e02a629c`.                                                            |
| LOINC URI storage/import    | Known URIs normalize before validation; import copies preserve canonical aliases deterministically without mutating managed rows. Eight tests pass.                                                           |
| ASTM sender delimiter       | Bridge honors the header's declared delimiter for HTTP and serial traffic.                                                                                                                                    |
| Capacity refusal metric     | Bridge counts an admitted peer refused at the listener's session limit.                                                                                                                                       |

Bridge's two threads are resolved. OpenELIS replies cite the pushed remediation
commit and distinguish focused evidence from combined CI. The original drafts
remain backed up at `refs/codex/analyzer-remediation-drafts-20261021`; the
candidate now includes their fixes.

## Subsequent review corrections

- Local browser validation found that saving an excluded mapped test clears its
  target correctly on the server, but Carbon emits a selection-change callback
  while the disabled picker updates. The callback wrongly restored UNRESOLVED in
  the browser and disabled confirmation. Disabled excluded pickers now ignore
  that callback. The regression failed before the correction; all 32 mapping-editor
  tests pass afterward. One pre-existing effect-state lint error remains unchanged.

- Connection decimal-separator changes and removal are part of confirmation and
  arrival readings. A profile fingerprint match cannot bypass them; results parsed
  under the old setting cannot recover under the new one. The combined confirmation,
  interpretation and importer suite passes 90 tests.
- Undeclared held codes accept an explicitly confirmed exclusion as well as an
  explicit mapping, so applying that exclusion can remove the held row.
- GitHub run 39724746731 passed undeclared-code mapping recovery. Its remaining
  Mindray failure exposed an incorrect test assumption: `analysis-result` contains
  the routine automated CBC; `manual-count` adds the technician's differential.
  The corrected scenario uses `manual-count`, explicitly excludes the declared
  manual codes, then clears the two undeclared codes through the mapping screen.
  The corrected scenario passed in the 40-test analyzer shard 1.
- Queued deliveries fetch the actual delivered profile revision, and held-result
  dismissal preserves unsaved edits on other eligible result rows.

- Value exclusions are documented as entries in `value_defaults`; only tests and
  parts use the `default` field directly.
- A delivery with an identified source waits if the currently served profile is a
  different edit of that revision. Its readings and legacy baseline cannot be
  reconstructed from another edit. This does not restore the removed comparison
  against the old confirmation's whole-file fingerprint.
- Protocol settings are included in the saved interpretation, so changing file
  format requires reconfirmation and cannot recover a result under the old format.

## Legacy fingerprint gate removed

- [x] Remove the whole-file fingerprint fallback from legacy delivery and
      verification. Older confirmations without readings begin tracking the served
      interpretation on their first readable delivery, irrespective of the old
      fingerprint. This is a forward baseline, not a reconstruction of history.
      Read-only status, assessment and served-profile checks do not write readings.
- Later changes to stored interpretation still hold the affected rows or type.
  The replacement regressions cover changed-fingerprint legacy delivery,
  verification, and a subsequent unit change. Missing-baseline/outage and
  reconfirmation regressions now also pass.
- Validation: both new regressions failed before the fix; all 104 focused
  confirmation, activation, interpretation, catalog and import tests pass after
  it. Formatting and whitespace checks pass. Full CI remains separate.

## Baseline corrections and evidence

- Synchronize every OE2 layer with its parent and develop `965d6d1055` without
  rewriting published history. Resolve the TLS/encryption branch conflict.
- Move removal of the retired menu-overlay test expectation to the introducing
  menu layer (#4794), so #4795 and #4797 inherit it. Existing seeded-menu tests
  retain coverage of the replacement behavior.
- Fix formatter retrieval: Spotless 3.10.4 provisions the explicitly pinned
  Eclipse 4.26 formatter through Maven Central. The previous download endpoint
  returned an HTML temporary-shutdown page. Java formatting version stays 4.26.
  [Spotless release history](https://github.com/diffplug/spotless/blob/main/plugin-maven/CHANGES.md).
- Passed locally before propagation: complete formatter check; 29 focused menu,
  menu-seeding, and report-queue tests. These checks are not full CI acceptance.
- Combined contract checks exposed the mock's obsolete requirement for stamped
  fingerprints in authored profiles. Resolve by exact ID/revision, preserve an
  optional supplied stamp, and leave fingerprint derivation to the Bridge.
- Local frontend tests exposed a daylight-saving-sensitive inventory fixture:
  the expiry assertion counts elapsed 24-hour periods, while its fixture added
  calendar days. Make the fixture use elapsed periods; production is unchanged.
- The corrected mock passed 281 tests and 13 subtests (5 skipped), Python
  compilation, and all seven Bridge cross-process traffic/recovery scenarios.
  The corrected inventory test passed all 81 tests in both Los Angeles and UTC.
- The dictionary integration test now supplies Bridge API-shaped fingerprint
  metadata when reading authored profile files. The candidate Mindray profile
  also requires 17 manual-differential entries in the harness dictionary; those
  fixtures now bind without weakening the all-records assertion. All eight
  focused dictionary and API-profile parsing tests passed.
- Full local CI and GitHub checks must be inspected on the pushed candidate.
  Check every stack layer; a green tip does not establish a green stack.

## Combined validation

The previous full local run at `29547aa1b1` passed fifteen of sixteen groups.
Analyzer shard 1 failed undeclared-code recovery and two obsolete Mindray
expectations. This candidate fixes recovery and updates both scenarios to assert
thirteen CBC records, nineteen visible shipped exclusions, and accepted results.
GitHub also had a truncated Cypress binary download in a core Playwright job;
that was a dependency-install failure before the test ran.

The full run started on `380075460d` was stopped to include the additional
HTTP-client private-key password correction. A new committed candidate must pass
all sixteen local groups and the three required GitHub checkpoints. The trusted
upstream-pin check remains blocked until the upstream dependency merges.

The queued-revision follow-up preserves the delivery profile reading before that
profile is removed; a changed or unavailable source does not fabricate history.
Its focused confirmation/reading/import suite passed 85 tests. Both published and
source-build Compose configurations rendered consistent custom store passwords.

Both stacks were rebased with `gh stack rebase` onto the current upstream
branches. All 27 branch trees match their pre-rebase backups, and every stack
parent is an ancestor of its child. Duplicate historical patches were removed
from the encryption layer without changing its final tree. The OpenELIS tip
records the rewritten Bridge tip. `gh stack push` publishes these updates with
explicit per-branch force-with-lease checks.

A missing or unavailable individual Bridge connection now marks that analyzer
unconfirmed in the type catalog instead of failing the whole request. The
38-test confirmation suite passes, including both missing and failed reads.

## Database integration correction — 22 October

Full local validation on `89e02a629c` found a detached analyzer proxy when a
mapping loaded by ID reached confirmation. The DAO now fetches the mapping's
analyzer within its transaction, as the latest-mapping query already did. The
adoption integration test exercises that detached service boundary directly.
The adoption/import integration fixtures now supply readable Bridge connection
settings, and the test catalog includes the profile named by the QC fixture.
Production handling of unavailable Bridge settings is unchanged.

All 104 focused tests pass: 10 adoption integration, 31 normalized import
integration, 42 confirmation unit and 21 mapping unit tests. The first focused
attempt exposed an incorrect cleanup reflection overload and missing QC test
profile; both are corrected. This is focused proof, not full CI acceptance.

## Delivery-contract proposal superseded

The queued-settings and pre-confirmation paths have been removed. Their replacement
is the broader [interpret-once boundary](boundary-remediation.md), not a new
OpenELIS decimal-history gate. Bridge now supplies explicit normalized meaning,
including qualitative calls, and preserves the receipt-time context across a
restart before rendering. OpenELIS recovers from the retained normalized
payload using its own local decisions. No blanket hold or general raw-message
reprocessor is required for missing decimal metadata. Existing incomplete
qualitative payloads need bounded compatibility handling and reviewed local
resolution, preserving receipts and finalized/dismissed results.

## Remaining acceptance

- [x] Audit duplicated interpretation checks and record the revised boundary.
- [ ] Implement and validate the six steps in the boundary remediation plan.
- [ ] Finish GitHub replies/resolutions and refresh for new findings.
- [ ] Pass combined analyzer scenarios, including undeclared recovery, exclusions
      and sender identity, and retain logs/screenshots.
- [x] Run the optional reporting navigation qualification. The reporting instance
      menu passed on `89e02a629c` with the normal admin session (one test).
      The unrelated participant setup was excluded after failing against an
      obsolete container name; this is focused qualification, not full CI.
- [x] Confirm native mock host-query transport acceptance: two queries on one
      connection received addressed no-orders replies from the real Bridge. All
      seven isolated transport groups passed. The separate pending-orders feature
      remains outside this work.
- [ ] Pass full local CI and every required GitHub checkpoint on the final commit.
- [ ] Merge upstream dependencies after approval, replace candidate pins with the
      actual merged commits, and pass the trusted submodule check.
- [ ] Obtain the required independent review before merging OpenELIS.

Fresh/upgrade certificate-script tests pass, including custom trust entries,
passwords, and stable identity. A deployed PNG migration and offline installer
are outside this stack acceptance. No merge or production migration has occurred.
