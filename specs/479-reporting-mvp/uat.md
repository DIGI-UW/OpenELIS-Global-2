# Reporting MVP UAT Contract

This file defines the implementation-facing UAT contract. The central Grist
document in `DIGI-UW/openelis-review-tooling` is the live checklist source of
truth after deployment. Do not serve this Markdown as a second checklist.

UAT follows each development stage and helps discover issues as work proceeds.
Automated end-to-end checks and human review exercise the same workflows,
fixtures and expected results. Publish currently executable story steps with
the stage scope and known gaps, then extend them as capabilities arrive. Do not
withhold the public target until the complete MVP passes.

The deployment receipt in `execution.md` identifies the currently public stage.
The frontend repair restores the canonical overview and three-stage builder.
Grist instructions were updated with that deployment: start at the overview,
choose Sample & Testing, explicitly Add fields, then continue through Set Filters
and Review & Submit. A new export starts with no selected fields. Existing shared
reports retain their saved selections. The remaining steps below retain the full
planned acceptance contract; pending functionality is not represented as working.

## Target and Evidence

- UAT host: `reporting.catalyst.openelis-global.org`
- Checklist instance: `reporting`
- Application route: `/CustomDataExport`
- Deployment identity: `/__review/target.json`
- Public checklist: `https://grist.openelis-global.org/uat/reporting.json`
- Fixture period: 2026-05-05 through 2026-05-05
- Repeated-result accession: `REPORTING-MVP-REPEAT`
- Turnaround fixture: `REPORTING-MVP-TURNAROUND`, collected 2026-05-06, two
  Viral Load values of 450 with result-to-validation intervals of 30 and 90 minutes
- Repeated configured test: `Viral Load`
- Independent repeated values: two results whose displayed value is `450`
- Planned referral fixture (not yet seeded): `REPORTING-MVP-REFERRAL`
- Planned non-conformance fixture (not yet seeded): `REPORTING-MVP-NCE`

Fixtures are public synthetic data. Seeding is idempotent for one deployment
and must refuse to overwrite a real record that reuses a stable identifier.
Each state-changing story has its own prepared data or reset boundary.

## RPT-S01 — Routine Sample and Testing Export

**User story**: As a report user, I can build and download a useful routine
Sample & Testing report in either layout without losing repeated results.

1. `RPT-001` required — Sign in as the assigned report user and navigate through
   Reports to Custom Data Export. Expect the overview with new-export and saved-report
   cards. Choose Start a new export, then Sample & Testing. Expect Spreadsheet
   by default, collapsed field groups, Add/Added controls and no selected fields.
2. `RPT-002` required — Use Find a field to Add Accession Number, Specimen ID and Viral Load, clearing
   search after each addition. Reorder Specimen ID before Accession Number using
   its arrow or grip. Expect the preview to follow that order. Choose Next: Set
   Filters, enter 2026-05-05 for both dates, then Next: Review & Submit. Expect
   the review to retain the exact period and ordered fields.
3. `RPT-003` required — Generate and download the spreadsheet. Inspect the CSV,
   not only the READY badge. Expect two data rows for
   `REPORTING-MVP-REPEAT`, with two independent Viral Load values of `450`; no
   result is collapsed or replaced by a latest-only value.
4. `RPT-004` required — In review, choose Edit report, switch to Detailed list — results in rows,
   and explicitly Add Accession Number, Result Value and Result ID. Continue
   through filters with the same period, review, generate and download.
   Expect the same two results once each, with distinct Result IDs and the value
   `450` for both.
5. `RPT-005` required — Compare the builder directly with the linked canonical
   mock at desktop and phone widths. Search for a field, clear the search, switch
   Available/Selected panes and reorder selected fields with the arrows or grip.
   Open My Report Queue, continue the draft, use browser Back/Forward and reload.
   Expect the mock's layout and controls, restored group folds, retained field
   order and dates, no horizontal page overflow and usable keyboard focus.

## RPT-S02 — Reuse a Shared Report

**User story**: As a second report user, I can reuse an instance-shared report
definition with fresh dates and my own access.

1. `RPT-101` required — As report user A, start a fresh spreadsheet, Add Specimen ID, Accession Number and Viral Load,
   enter May 5, 2026 for both dates, and proceed to review. Select Save these
   report settings for later, enter a unique `Routine Viral Load UAT` name and
   choose Save report settings. Use a new name per walkthrough; keep existing
   definitions unless their deletion is explicitly part of that walkthrough. Expect it in the shared report list with
   the selected layout, ordered columns and non-date filters.
2. `RPT-102` required — Sign out and sign in as report user B. Open
   Export overview → Shared reports, search for the exact saved name, and
   choose Use report on that card. Expect the definition to open without an invitation
   and require a fresh reporting period before generation.
3. `RPT-103` required — Enter 2026-05-05 for both dates, generate and download.
   Expect the same configured header and the results permitted to user B;
   report user A's generated job/file does not appear in user B's queue.
4. `RPT-104` required — Save a copy, then update it after confirmation. Expect
   the original and copy to remain distinct and a stale concurrent update to be
   refused rather than silently replacing another user's changes.

## RPT-S03 — Configured Referral and Non-Conformance Reports

**User story**: As a report user, I can run the other mock-derived report types
through the same reporting experience.

Current-stage availability check: `RPT-200` required — Open Report type in
the builder. Expect Referrals and Non-Conformance alongside Sample & Testing.
Choose Start a new export and inspect both type cards. Mark Fail while they
remain unavailable. The restored design displays both as Not yet connected;
visible cards do not establish functional acceptance. No unseeded fixture is needed for this availability check.
The three execution checks below become runnable when their sources and
fixtures arrive; preserve their stable planned keys.

1. `RPT-201` required — Select Referrals, use the fixture period and generate a
   report including the referral identity, accession, destination, event date
   and status. Expect exactly one `REPORTING-MVP-REFERRAL` occurrence with the
   configured date meaning and no duplicated rows.
2. `RPT-202` required — Select Non-Conformance, use the fixture period and
   generate a report including the event identity, accession, reason, event
   date and status. Expect exactly one `REPORTING-MVP-NCE` occurrence linked to
   its sample and no duplicated rows.
3. `RPT-203` required — Return to Sample & Testing and inspect the builder and
   queue. Expect the same page, selection/review behavior and delivery controls;
   the report types do not open separate report-specific applications.

## RPT-S04 — Queue and Recovery

**User story**: As a report user, I can return to completed work and recover
from ordinary queued or failed jobs without reconstructing the report.

1. `RPT-301` required — Start an independent new spreadsheet with Accession Number, Specimen ID and
   Viral Load and May 5, 2026 for both dates. Generate and download it, then open
   My Report Queue and open the completed job. Expect the period, row count and
   READY state, and expect re-download to return the same stored bytes.
2. `RPT-302` required — Open the prepared failed job and choose Retry. Expect a
   new linked job with the same frozen definition, columns and scope while the
   original remains FAILED.
3. `RPT-303` required — Cancel the prepared queued job after confirmation.
   Expect CANCELLED and no generated download. If it has already begun, expect a
   clear refusal rather than a false cancellation.
4. `RPT-304` required — Open the prepared expired job and restore its choices.
   Expect download to remain unavailable and generation to require a fresh date
   range.

## RPT-S05 — Turnaround Beside Each Repeated Test

**User story**: As a report user, I can distinguish each repeated test's
turnaround in the same exported report.

1. `RPT-401` required — Sign in and open Reports → Custom Data Export → Start a new export → Sample & Testing.
   Keep Spreadsheet. Add Accession Number, Viral Load and Viral Load — Resulted
   to Validated (min), in that order. Continue to filters and set both dates to
   May 6, 2026, then continue to review. Generate and
   download the CSV. Expect two rows for `REPORTING-MVP-TURNAROUND`, each with
   Viral Load 450 and its own interval: 30 and 90 minutes. The fixture is
   read-only and reusable; generation does not consume it.
2. `RPT-402` required — Edit the report, switch to Detailed list — results in
   rows, Add Accession Number, Result Value and Resulted to Validated (min). Continue
   through filters with the same dates, then review. Generate
   and download the new file. Expect the same two result/interval pairs.

## Preflight and Human Acceptance

Before handoff, automated browser preflight repeats the critical actions against
the deployed host and compares downloaded CSV rows, values, identities and
headers with the fixture oracle. It also verifies checklist JSON, overlay load,
target identity, route capture and one authenticated review submission.

Preflight success means the target is ready for UAT. Human acceptance remains
pending until a reviewer submits or downloads a report bound to the deployed
application SHA, review-tooling SHA and checklist revision.
