# Analyzer stack final remediation

Updated 22 October 2026. The coordinated cleanup is implemented and pushed.
Focused validation passes; full local CI and the required GitHub checkpoints
must pass on the final published commit. No merge or deployment is authorized.

## Implemented boundary

The Bridge interprets the instrument message once and preserves that decision
through interruption and retry. OpenELIS consumes the normalized result and
handles its own laboratory responsibilities. The [cleanup inventory](cleanup-inventory.md)
records ten work items; the [source-backed audit](boundary-remediation.md)
records the eighteen original check areas and why each changes or stays.

- Deleted `AnalyzerRecordReading`, `AnalyzerArrivalReading` and
  `AnalyzerConfirmationReading`, their import/recovery/approval gates and
  parser-change UI. Historical snapshot columns remain audit data only.
- Bridge retains effective decoding context with each durable raw receipt.
  Restart uses that context; ordinary delivery retry preserves its rendered
  bundle and identity. Older or unresolved raw receipts require explicit Retry.
- Contract 1.1 carries canonical qualitative answers and their codings, explicit
  call/flag roles, typed values, units, failure outcomes and patient/control
  classification. Existing 1.0 quantities remain supported. Missing meaning is
  not guessed.
- OpenELIS checks the delivered contract, authenticated source association,
  approved local mappings/exclusions and actual local test/component/answer
  compatibility. It owns placement, QC, review, audit and duplicate receipts.
- Held recovery reapplies local mappings and placement to the stored normalized
  payload. It does not fetch Bridge profiles/connections or compare historical
  decimal settings, parsing recipes or recognition hashes.
- Older uncoded results use existing append-only local mapping history to
  establish unchanged test/component targets. Changed or unknown targets stay
  held. Coded results validate delivered terminology against local targets.
  This adds no parser snapshots, Bridge lookups or persistence.
- Type readiness uses the same current local mapping assessment as activation.
  An approval audit record alone does not establish that local targets remain
  usable. Stale-editor protection and local mapping approval remain.
- Mapping defaults no longer generate separate rows for translated vendor
  spellings. Conflicting existing local answer choices stay visible for a
  local decision.

The [versioned contract](../../tools/openelis-analyzer-bridge/contracts/analyzer/v1/README.md#normalized-result-version-11-interpret-once)
documents the coordinated OE2-first upgrade and legacy receipt behavior.
No blanket historical reprocessing or second interpretation path is introduced.

## Stack and dependency order

OpenELIS: #4770 → #4781 → #4784 → #4786 → #4787 → #4788 → #4789 →
#4790 → #4791 → #4792 → #4793 → #4794 → #4795 → #4797 → #4798.

Bridge: #101 → #102 → #104 → #105 → #106 → #107 → #108 → #109 →
#110 → #111 → #112 → #113.

Both stacks were rebased and published with `gh stack`; each parent is an
ancestor of its child. OpenELIS #4798 records these candidate dependencies:

- Bridge #113: `e5a9e33ffd8490e9190873bee7a641cf028bc7dd`.
- Analyzer mock #62: `88b51f7c5ed50e953b8e80be5934162bcb138fd9`.

Standalone OpenELIS #4768 and Bridge #88 remain independent changes. Mock #62
is the independent host-query dependency included by the pin. Older mock #56
is separate control-fixture work with conflicts and is outside this candidate.

The trusted submodule check requires upstream default-branch commits. The
candidate pins remain blocked until the user merges their upstream dependencies.
After squash merges, pin the actual resulting commits and recheck. Do not weaken
or bypass the trusted check. Required independent review remains separate.

## Validation and review evidence

Focused cleanup validation:

- Bridge: 1,221 tests, zero failures/errors, four skipped.
- OpenELIS mapping/import/contract/adoption: 268 tests, zero failures/errors,
  across the focused run and corrected import-fixture rerun. This includes
  database-backed adoption/recovery and local target continuity.
- Affected frontend suites: 129 tests across fourteen files.
- Formatting, whitespace and documentation link targets passed.
- Updated analyzer browser suites collect successfully; execution is part of
  complete CI, not established by test discovery.

Five follow-up review findings were fixed, replied to and resolved: queued
number-setting comparisons, historical arrival fallback, clinical answer coding,
stale type readiness and older uncoded target reassignment. The latest refresh
found no open review threads across the thirty in-scope PRs. Refresh again after
CI because later reviews may introduce new findings.

Full local CI and GitHub checks are running on the published candidate. Earlier
baseline `fde241e1a6` passed all sixteen local groups; that does not certify the
cleanup. Final evidence must identify the tested commit and include all groups,
the aggregate result, reports and cleanup outcome. PR summaries track the latest
candidate rather than recording a self-referential commit hash here.

## Retained earlier stack corrections

The stack also contains the preceding fixes for terminology/LOINC aliases,
held-result dismissal, analyzer lifecycle/audit, failed-restore compensation,
reporting navigation, private install keys and consistent keystore passwords.
Bridge retains its sender-delimiter, listener-capacity, connection and delivery
corrections. These remain covered by complete candidate validation.

Earlier proposals to add arrival/confirmation parsing snapshots, require
reconfirmation after decimal/override changes, or reconstruct missing historical
readings are superseded and removed. Their historical implementation notes remain
in Git history; they are not acceptance requirements for this cleanup.

## Remaining acceptance

- [x] Audit and remove duplicate interpretation machinery and its UI callers.
- [x] Implement retained receipt interpretation and normalized contract 1.1.
- [x] Replace obsolete tests and pass focused backend, frontend and Bridge suites.
- [x] Rebase and push both stacks with `gh stack`; synchronize the dependency pin.
- [x] Reply to and resolve the reviewed findings after publishing their fixes.
- [ ] Pass all sixteen full local CI groups on the final published commit,
      including analyzer recovery/exclusions/sender identity, and inspect reports.
- [ ] Pass the three required GitHub checkpoints on that commit; verify every
      stack layer and refresh conflicts and review threads.
- [ ] User merges upstream dependencies, then replace candidate pins with actual
      merged commits and pass the trusted submodule check.
- [ ] Obtain required independent review before any OpenELIS merge.

Offline installation, deployed PNG migration and general historical reprocessing
remain outside this cleanup. No production migration or merge has occurred.
