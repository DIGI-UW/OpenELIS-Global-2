# Milestone 6 — susceptibility testing and issued reports

This slice extends the case testing workspace with per-isolate susceptibility
runs, per-drug report selection, molecular drug bindings, and immutable issued
reports. It stacks on Milestone 5 through the repository's native GitHub stack.

## Contract and ownership

- `GET /rest/microbiology/cases/{caseId}/ast` compiles the visible isolate,
  assigned-panel, run, reading, selected-drug and resistance evidence data.
- `POST .../ast/entry` accepts panel assignment, start, reading, repeat, retest,
  and invalidation actions. `POST .../ast/validation` accepts validation,
  interpretation override/revert, selected-reading, instrument acknowledgment,
  and quality-control override actions. Services verify case ownership and
  the corresponding role in the owning laboratory unit.
- A report choice is unique to an isolate and antimicrobial. Selecting a repeat
  for one drug does not replace another drug or another isolate. Stored raw
  readings remain append-only; database sequence order determines the latest
  entry for a drug within a run. The old run-level report flag is derived from
  those choices for existing export consumers.
- Published compatible panel versions are required. Identification assigns the
  configured default except for nontuberculous mycobacteria. Breakpoint standard
  versions and interpretations on recorded readings do not follow later catalog
  activation changes.
- `/rest/microbiology/admin/dst-bindings` binds a test component's coded values
  to an antimicrobial. Classification uses configured drug roles and exact
  component values. Molecular initial-testing evidence can contribute before
  culture identification. Nontuberculous mycobacteria use the separate protocol.
  Deactivating a catalog drug does not hide its recorded discordance.
- `POST .../dst/resolve` requires validation rights, a reason, validated evidence,
  and the current evidence fingerprint. New evidence makes an older resolution
  inapplicable.

## Issuing and displaying reports

`GET .../report` returns a preview while work is open and the immutable issued
snapshot after final release. `GET .../report/versions` exposes the issued
history. Partial release requires at least one validated selected result and can
be repeated. Final release also checks definitive culture outcomes, selected
ordinary-result validation, configured clinical fields, program questionnaires,
relevant referrals, quality-control holds, critical follow-up, identification
mismatch and drug discordance. Pending unselected additional work and
in-laboratory-only results are excluded.

The existing Patient Report uses the snapshot under the case's laboratory unit,
including cases whose original culture analysis has no ordinary finalized
result. A case appears once even when multiple analyses anchor it. The report
contains initial testing, culture/identification, per-isolate susceptibility
grids, additional testing and only notes explicitly marked Send with Result.
The report menu uses the OpenPDF patient-results renderer; the same structured
rows are also supported by the clinical Jasper variants. Jasper source and
compiled resources are included for those variants. Internal notes and hidden
result components are excluded.

Final release locks case mutations until an official amendment is opened.
Amendment cancellation restores the issued reading choices; already issued
versions retain their original content.

## Verification

Focused database tests cover per-drug repeat selection, isolation between
isolates, append ordering, repeated immutable partial versions, write access,
self-validation, final locking, NTM defaults, note visibility, printed-report
anchor discovery and owning-unit access. Separate tests cover schema mappings,
rollback refusal when history would be lost, molecular evidence and stale
resolutions, required fields and questionnaire conditions, and multipage PDF
output. The browser journey uses an application-owned synthetic scenario and
exercises entry, validation, repeat selection, successive partial versions,
final locking, and the Patient Report entry point, parsing the returned PDF to
assert selected readings, section order, and exclusion of internal notes.

Full local CI, GitHub checkpoints, and browser acceptance must be recorded for
the submitted commit before this milestone is declared complete.
