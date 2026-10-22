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
Custom store passwords remain supported by the consumer; generator consistency
is an open fix below.

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

## Original review-fix groups

Do not resolve these threads until the fix is validated and pushed.

- [ ] **Historical shared certificates:** recognize both historical image
      subjects, including DIGI-UW as well as I-TECH-UW. Preserve genuine site keys,
      custom trust entries, and identity across repeated starts. An upgrade test
      draft exists; production fix is still required.
      [OE2 review](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4798#discussion_r4333386585).
- [ ] **Store password consistency:** reconcile certificate generation,
      application properties, and every consumer. Test defaults and custom values;
      do not silently drop custom-password support.
      [OE2 review](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4798#discussion_r4333365575).
- [ ] **Clinical-reading comparison and recovery:** removed values/spellings
      must invalidate comparison and recovery; additions remain allowed. Missing
      confirmed rows must not be treated as confirmed. Draft code/tests exist.
      [Comparison review](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4798#discussion_r4333708099),
      [recovery review](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4798#discussion_r4333708114).
- [ ] **ASTM sender extraction:** honor the delimiter declared in the message
      header. Draft code/tests exist.
      [Bridge review](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/113#discussion_r4333438833).
- [ ] **Session refusal metrics:** count listener capacity refusals. Draft
      callback, wiring, and tests exist.
      [Bridge review](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/113#discussion_r4333438847).

The local OE2 and Bridge repositories retain the drafts at
`refs/codex/analyzer-remediation-drafts-20261021`; the baseline does not apply
these drafts. The review inventory and recovery patch archive also retain them.

## Additional findings from the refreshed review

Ten more threads arrived after the original inventory. These remain open for
final remediation; source inspection is not a passing regression test.

- [ ] [Canonical LOINC URI storage (#4770)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4770#discussion_r4342426345):
      align canonical URI input with the storage alias used by legacy LOINC columns.
- [ ] [Connection code overrides (#4781)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4781#discussion_r4342459025):
      trace connection overrides through confirmation and delivery before deciding
      the correction. Bridge applies overrides, but OE2 readings are profile-based.
- [ ] [Missing baseline reading (#4786)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4786#discussion_r4342432773):
      the profile-fetch failure path currently returns unchanged when no reading was
      recorded; reproduce changed-fingerprint delivery through that path.
- [ ] [Unavailable profile on reconfirmation (#4786)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4786#discussion_r4342432779):
      an existing confirmation can be returned as current without being updated.
- [ ] [Component order (#4786)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4786#discussion_r4342432789):
      canonicalize semantically unordered component readings after verifying parser
      semantics; current row readings preserve declaration order.
- [ ] [Frontend response type (#4787)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4787#discussion_r4342433047):
      declare the response's reading-change fields in the TypeScript API contract.
- [ ] [Migrated activation history (#4791)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4791#discussion_r4342387827):
      preserve previously activated analyzers identified by `lastActivatedDate`, even
      without a durable activation-record row or received results.

- [ ] [Reporting navigation assertion (#4794)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4794#discussion_r4342586091):
      update the optional `REPORTING_INSTANCE_NAV=true` qualification assertion for
      the revised menu description; the default core lane does not exercise it.
- [ ] [Failed-restore compensation (#4797)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4797#discussion_r4342577027):
      preserve prior active intent if the Bridge retry succeeds but saving the
      OpenELIS activation state fails. Reproduce before changing compensation.
- [ ] [Undeclared-code recovery (#4798)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4798#discussion_r4342887378):
      an undeclared code has an empty arrival-reading row set and no profile-derived
      confirmed record. The current null check prevents recovery after explicit
      mapping and confirmation. The combined GeneXpert browser scenario reproduces
      this; keep rejection of genuinely missing confirmed readings covered.

## Legacy fingerprint gate removed

- [x] Remove the whole-file fingerprint fallback from legacy delivery and
      verification. Older confirmations without readings begin tracking the served
      interpretation on their first readable delivery, irrespective of the old
      fingerprint. This is a forward baseline, not a reconstruction of history.
      Read-only status, assessment and served-profile checks do not write readings.
- Later changes to stored interpretation still hold the affected rows or type.
  The replacement regressions cover changed-fingerprint legacy delivery,
  verification, and a subsequent unit change. Missing-baseline/outage and
  reconfirmation findings above remain separate work.
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

## Combined validation snapshot

Executable candidate `2a4fad086f` passes the GitHub backend and frontend
checkpoints, Bridge/mock contracts, Bridge acceptance, all four core Playwright
shards, both Cypress groups, and analyzer shard 2. The
[analyzer shard 1 run](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/39691617412/job/119539089516)
fails three scenarios: undeclared-code recovery above, and two Mindray scenarios
that still expect 32 staged records although default exclusions leave 13.

- [ ] Update the two Mindray scenarios to verify the shipped exclusion defaults
      and operator decisions. Do not remove the exclusion behavior to satisfy the
      obsolete expectations.
- [ ] Pass the undeclared-code recovery scenario after its production fix.

Full local validation is not green: backend, frontend and contract lanes passed;
core browser lanes had empty-table/disabled-control failures that did not recur
in GitHub; analyzer lanes failed before tests on Docker Hub metadata requests
returning Bad Gateway. Cypress stalled at its first spec for over 70 minutes and
was stopped with the runner's owned cleanup; its independent group did not run.
Retain and diagnose these local failures without claiming they are proven
analyzer regressions. The trusted upstream-pin gate remains expected to fail
until the candidate dependencies merge. No release acceptance is claimed.

## Final acceptance

- [ ] Implement and validate the original five groups and the additional ten
      findings above, then reply to and resolve
      their sixteen GitHub threads. Historical bot summaries are not current status.
- [ ] Exercise exclusions and sender identity through the combined candidate.
- [ ] Add host-query harness acceptance using native mock traffic; keep the
      separate pending-orders feature outside this transport acceptance.
- [ ] Verify fresh installation and upgrade behavior with owned keys, custom
      trust entries, and configured passwords.
- [ ] Pass the full local CI runner and all required GitHub checkpoints after
      final changes; retain logs and reports. Replace candidate pins after upstream
      merges and pass the trusted submodule check.
- [ ] Obtain independent review. No merge or production migration has occurred.
