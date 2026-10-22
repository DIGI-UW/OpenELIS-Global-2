# Analyzer stack final remediation

Baseline synchronized on 21 October 2026. This checklist separates saved review
work from the branch synchronization needed to finish it. It does not certify a
release or authorize deployment.

## Candidate and stack order

OpenELIS stack: #4770 → #4781 → #4784 → #4786 → #4787 → #4788 → #4789 →
#4790 → #4791 → #4792 → #4793 → #4794 → #4795 → #4797 → #4798.
Bridge stack: #101 → #102 → #104 → #105 → #106 → #107 → #108 → #109 →
#110 → #111 → #112 → #113.

OpenELIS #4798 records these candidate commits:

- Bridge #113: `474f8f204408c5af2c80a64c1543efa9925dff96`.
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
The browser expectations now follow shipped Mindray exclusions; combined browser
validation is pending. The repository-wide TypeScript check reports missing Node
ambient types across existing Playwright files; changed browser tests pass ESLint.

Connection-code overrides are now part of the confirmed interpretation. Changing
or removing them requires reconfirmation even when the profile fingerprint is
unchanged; unrelated host changes do not. The focused override/reading/import/
activation suite passes 101 tests. Spring TLS property resolution passes its test,
and the deployment-script group passes 64 tests (including seven certificate tests).
The trusted upstream-pin gate remains unchanged. No merge or deployment is authorized
by this progress record.

## Implemented review fixes

All nineteen known findings now have implementation changes in the stack tips.
GitHub resolution is tracked separately from implementation and full CI.

| Finding                     | Correction and focused evidence                                                                                                                     |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Shared certificates         | Both historical subjects and previously exported shared keys are replaced; seven real-certificate tests pass.                                       |
| Store passwords             | Generation and every consumer, including the HTTP client's private-key password, use the configured values; defaults remain supported.              |
| Removed readings            | Removed values/spellings differ; recovery rejects absent confirmed values.                                                                          |
| Undeclared-code recovery    | Explicitly confirmed undeclared codes recover when parsing agrees; declared records with missing readings remain held. Browser proof is pending.    |
| Connection overrides        | Changing/removing overrides requires reconfirmation even with an unchanged profile; unrelated host edits remain current.                            |
| Unreadable profile          | Delivery waits for a baseline; reconfirmation fails without changing the saved confirmation.                                                        |
| Component order             | Tuples are sorted by code/sub-identity before comparison.                                                                                           |
| API response fields         | Both reading-change fields are present in the frontend response type.                                                                               |
| Migrated activation history | A last activation date prevents permanent deletion.                                                                                                 |
| Failed-restore retry        | Persistence failure preserves already-active runtime intent.                                                                                        |
| Reporting navigation        | The optional qualification assertion matches the current description; its optional browser run is pending.                                          |
| LOINC URI storage/import    | Known URIs normalize before validation; import copies preserve canonical aliases deterministically without mutating managed rows. Eight tests pass. |
| ASTM sender delimiter       | Bridge honors the header's declared delimiter for HTTP and serial traffic.                                                                          |
| Capacity refusal metric     | Bridge counts an admitted peer refused at the listener's session limit.                                                                             |

Bridge's two threads are resolved. OpenELIS replies cite the pushed remediation
commit and distinguish focused evidence from combined CI. The original drafts
remain backed up at `refs/codex/analyzer-remediation-drafts-20261021`; the
candidate now includes their fixes.

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

## Remaining acceptance

- [ ] Finish GitHub replies/resolutions and refresh for new findings.
- [ ] Pass combined analyzer scenarios, including undeclared recovery, exclusions
      and sender identity, and retain logs/screenshots.
- [ ] Run the optional reporting navigation qualification.
- [ ] Confirm native mock host-query transport acceptance; the separate
      pending-orders feature remains outside this work.
- [ ] Pass full local CI and every required GitHub checkpoint on the final commit.
- [ ] Merge upstream dependencies after approval, replace candidate pins with the
      actual merged commits, and pass the trusted submodule check.
- [ ] Obtain the required independent review before merging OpenELIS.

Fresh/upgrade certificate-script tests pass, including custom trust entries,
passwords, and stable identity. A deployed PNG migration and offline installer
are outside this stack acceptance. No merge or production migration has occurred.
