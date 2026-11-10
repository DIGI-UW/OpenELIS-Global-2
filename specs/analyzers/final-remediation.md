# Analyzer stack final remediation

Updated 22 October 2026. Follow-up cleanup is in progress after the boundary
re-audit disproved the previous completion claim. The cleanup inventory is the
current work ledger. Final full local/GitHub checks and publication remain to be
verified. Do not merge anything.

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
- Canonical source identities remain stable within a profile across parser revisions.
  Current approved local corrections apply without comparing old mapping targets.
  A different source profile remains held as unresolved local work.
- Type readiness uses the same current local mapping assessment as activation.
  An approval audit record alone does not establish that local targets remain
  usable. Stale-editor protection and local mapping approval remain.
- Existing mapping view, edit and confirmation use saved canonical declarations
  and normalized held results without a live Bridge lookup or recognition hash.
  Adoption/default previews still consult the Bridge. Historical hashes remain
  readable; reviewer, timestamp, exact local decisions and audit remain required.
- Mapping defaults no longer generate separate rows for translated vendor
  spellings. Raw text cannot override the canonical answer; obsolete spelling rows
  can be removed through the editor.

The [versioned contract](../../tools/openelis-analyzer-bridge/contracts/analyzer/v1/README.md#normalized-result-version-11-interpret-once)
documents the coordinated OE2-first upgrade and legacy receipt behavior.
No blanket historical reprocessing or second interpretation path is introduced.

## Stack and dependency order

OpenELIS: #4770 → #4781 → #4784 → #4786 → #4787 → #4788 → #4789 →
#4790 → #4791 → #4792 → #4793 → #4794 → #4795 → #4797 → #4798.

Bridge: #101 → #102 → #104 → #105 → #106 → #107 → #108 → #109 →
#110 → #111 → #112 → #113.

The OpenELIS stack was synchronized with `gh stack` on 23 October 2026.
The user merged the Bridge stack, Bridge #88 and #114, and mock #62 and #56.
OpenELIS #4798 now pins the released upstream commits:

- [Analyzer Bridge 3.5.0](https://github.com/DIGI-UW/openelis-analyzer-bridge/releases/tag/3.5.0):
  `82682a27be04ae387af1b349bc7d669f914d94cc`.
- [Analyzer Mock Server 0.4.0](https://github.com/DIGI-UW/analyzer-mock-server/releases/tag/v0.4.0):
  `9e828e7427d5c11edb3ca1e4c2a113fe2d313bd7`.

The release trees exactly match the tested candidates. Bridge validation passed
1,225 tests with no failures/errors and four skips; mock validation passed 283
tests and 13 subtests with five skips. All seven combined native-traffic acceptance
groups passed. The combined Bridge merge required one constructor repair after
removing obsolete control fields; #114 includes it and the version bump.

OpenELIS #4768 is the final layer of the stack, after #4798, as of 24 October
2026. It adds stale-pairing recovery to the combined cleanup and released pins.
The upstream merge prerequisite is satisfied; the trusted submodule check and
full OpenELIS validation must pass on this combined head. Required independent
review remains separate. No OpenELIS PR has been merged by this work.

## Validation and review evidence

Focused follow-up validation:

- Bridge: complete suite passed before the additional HTTP regression; the HTTP
  subset then passed. Current reports contain 1,224 tests, four skipped.
- OpenELIS: 103 focused import/contract/result tests passed. The manual boundary
  checks deliberately restored raw-answer lookup, role guessing, equal-value
  suppression and a live Bridge lookup during local mapping repair in isolated
  copies; all four mutations failed their regression
  tests while the unchanged baselines passed.
- Local mapping: database migration and all eleven persistence tests passed,
  including confirmation without a recognition hash and retained canonical
  declarations. All 65 focused unit/ORM checks also pass.
- Mapping editor: all 30 component tests pass, including confirmation without
  Bridge recognition metadata.
- Full local CI and GitHub results must still be checked on the final published
  commit. Earlier test totals do not establish acceptance of these follow-up edits.

Review fixes cover queued number-setting comparisons, historical arrival
fallback, clinical answer coding, stale type readiness and older uncoded local
target continuity. The later answer-target and certificate-rotation findings
also have fixes and regression coverage. GitHub replies/resolutions must cite
their published commits; the PR summaries carry the latest thread counts.
Refresh after CI because later reviews may introduce new findings.

Follow-up CI and review fixes preserve retirement through the FHIR Device API,
select the database encryption key by a format marker, reset configuration-menu
state on navigation, and make accession previews non-mutating. Certificate
startup fails on mismatched store passwords without modifying existing key
material. Focused regressions passed; full-candidate validation remains required.

Full local browser validation also exposed a billing-menu test that filled its
URL before the initial settings request completed. The test now waits for loaded
settings; browser execution of that correction remains an acceptance check.

Full local CI and GitHub checks are running on the published candidate. Earlier
baseline `fde241e1a6` passed all sixteen local groups; that does not certify the
cleanup. Final evidence must identify the tested commit and include all groups,
the aggregate result, reports and cleanup outcome. PR summaries track the latest
candidate rather than recording a self-referential commit hash here.

## Retained earlier stack corrections

The stack also contains the preceding fixes for terminology/LOINC aliases,
held-result dismissal, analyzer lifecycle/audit, failed-restore compensation,
reporting navigation, private install keys and consistent keystore passwords.
A preserved site keystore now updates install certificate/key files and the
internal trust alias after rotation, retaining unrelated trust entries and
separate proxy certificates.
Bridge retains its sender-delimiter, listener-capacity, connection and delivery
corrections. These remain covered by complete candidate validation.

Earlier proposals to add arrival/confirmation parsing snapshots, require
reconfirmation after decimal/override changes, or reconstruct missing historical
readings are superseded and removed. Their historical implementation notes remain
in Git history; they are not acceptance requirements for this cleanup.

## Review follow-up — 23 October 2026

The remaining review fixes preserve the interpret-once boundary:

- Control runs bypass the patient-result duplicate lock. Delivery receipts still
  prevent replay; independent numeric and qualitative control runs both enter QC.
- The mapping editor retains a saved call-target requirement when the operator
  changes its local test, even without older source declarations. No Bridge lookup
  or new persistence model is introduced.
- The webapp entrypoint grants its application user write access to the public
  truststore only. A container probe reproduces the denied upload before repair,
  calls the real upload service after repair, and checks private-store permissions.
- Menu configuration updates pass a saved original state to the existing audit
  service. Unchanged configuration creates no additional history.
- Normal operation uses one installation encryption key and performs no rekey scan.
  The previous startup listener, service, DAO and obsolete tests are removed.
  Existing databases use the separate, explicit [one-time upgrade](../../docs/upgrades/installation-encryption.md)
  while application writers are stopped. Liquibase tracks completion; primary-key
  batches are resumable. Fresh installations never need this upgrade.
- The Linux installer retains its existing parsed `common.properties` key,
  including Java escape semantics; it does not mount the raw configuration key
  as an override. Compose uses its generated installation key. The application
  validates its effective key once, with no shared defaults. Installer rendering,
  escaped-key decryption and file-based Compose startup are tested.
- Shared schema preparation widens the two limited ciphertext columns before the
  explicit upgrade and on fresh installation. Maximum-length existing values can
  be upgraded and written again without exceeding the old limits.
- Contract 1.0 call-role tags are excluded from clinical answer identity matching.
  Actual delivered clinical codings remain intact. Coded-answer import and held
  result recovery use the same retained payload; no profile lookup or parsing
  decision is added to OpenELIS.
- Analyzer deletion uses Hibernate entity queries within the existing service
  transaction, retaining history restrictions and its deletion audit event.

Focused validation passed 57 backend tests and 31 mapping-editor tests. The
standalone upgrade tests use PostgreSQL to verify interruption after 250 rows,
wrong-key rejection, recovery of 501 secrets and one-time completion. Full local
and GitHub validation must be refreshed for the committed candidate before
claiming these review fixes ready.

## Remaining acceptance

- [x] Audit and remove duplicate interpretation machinery and its UI callers.
- [x] Implement retained receipt interpretation and normalized contract 1.1.
- [x] Replace obsolete tests and pass focused backend, frontend and Bridge suites.
- [x] Rebase and push both stacks with `gh stack`; synchronize the dependency pin.
- [ ] Refresh replies/resolutions after the final fixes are published; verify
      no open review threads remain.
- [ ] Pass all sixteen full local CI groups on the final published commit,
      including analyzer recovery/exclusions/sender identity, and inspect reports.
- [ ] Pass the three required GitHub checkpoints on that commit; verify every
      stack layer and refresh conflicts and review threads.
- [x] User merged upstream dependencies; Bridge 3.5.0 and mock 0.4.0 are released,
      pinned to their merged commits, and passed the trusted submodule check.
- [ ] Obtain required independent review before any OpenELIS merge.

Offline installation, deployed PNG migration and general historical reprocessing
remain outside this cleanup. No production migration or OpenELIS merge has occurred.
