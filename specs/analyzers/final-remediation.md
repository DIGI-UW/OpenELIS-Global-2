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

- Bridge #113: `572cda3676ff7fdb3403a72a5082c6b332086cec`.
- Analyzer mock #62: `c84afe0f837eb01877c66777f69cb32b92702e24`.

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

## Five open review-fix groups

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
- Full local CI and GitHub checks must be inspected on the pushed candidate.
  Check every stack layer; a green tip does not establish a green stack.

## Final acceptance

- [ ] Implement and validate the five groups above, then reply to and resolve
  their six GitHub threads. Historical bot summaries are not current status.
- [ ] Exercise exclusions and sender identity through the combined candidate.
- [ ] Add host-query harness acceptance using native mock traffic; keep the
  separate pending-orders feature outside this transport acceptance.
- [ ] Verify fresh installation and upgrade behavior with owned keys, custom
  trust entries, and configured passwords.
- [ ] Pass the full local CI runner and all required GitHub checkpoints after
  final changes; retain logs and reports. Replace candidate pins after upstream
  merges and pass the trusted submodule check.
- [ ] Obtain independent review. No merge or production migration has occurred.
