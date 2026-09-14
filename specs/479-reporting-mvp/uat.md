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

The public recovery stage adds RPT-302 (failed retry) and RPT-304 (expired re-run),
bringing that recovery-stage checklist to five stories and 14 required steps.
The subsequent navigation stage adds RPT-S06 and RPT-501–RPT-503 (six stories,
17 steps). RPT-504 is prepared for the database-backed menu editor stage; the
current deployment receipt identifies whether that increment is published. Both prepared
examples remain reusable after a run. RPT-303 is still planned: cancellation is
connected and passed a local browser walkthrough, but its repeatable public queued
fixture and narrow confirmation check are not yet available. No human acceptance
is implied by the agent's walkthrough or automated checks.

## Target and Evidence

- UAT host: `reporting.catalyst.openelis-global.org`
- Checklist instance: `reporting`
- Application route: `/reports/custom-data-export` (the older `/CustomDataExport` redirects here)
- Deployment identity: `/__review/target.json`
- Public checklist: `https://grist.openelis-global.org/uat/reporting.json`
- Fixture period: 2026-05-05 through 2026-05-05
- Repeated-result accession: `REPORTING-MVP-REPEAT`
- Turnaround fixture: `REPORTING-MVP-TURNAROUND`, collected 2026-05-06, two
  Viral Load values of 450 with result-to-validation intervals of 30 and 90 minutes
- Repeated configured test: `Viral Load`
- Independent repeated values: two results whose displayed value is `450`
- Recovery fixtures for admin: failed job `47900000-0000-4000-8000-000000000101`
  and expired job `47900000-0000-4000-8000-000000000102`; loaded by
  `reporting-recovery.sql`. Deep link with `?view=queue&job=<id>` after sign-in.
  Their two-column CSV contains Accession Number and Viral Load, with both 450
  readings. Public availability is recorded in the deployment receipt.
- Referral fixture: the `REPORTING-MVP-REPEAT` analysis has two returned 450
  readings and one pending referral sent on May 7, 2026. Use May 7 for the
  Referral period. `reporting-referrals.sql` also includes an unsent draft and
  a May 8 sent referral, both excluded from that period.
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
remain unavailable. Referrals is publicly connected at `d48cd790c492`;
Non-Conformance remains Not yet connected;
visible cards do not establish functional acceptance. No unseeded fixture is needed for this availability check.
RPT-201 is runnable now with the deployed fixture, although its Grist checklist
publication is still blocked by the authoring connection. The remaining checks
become runnable as their sources arrive; preserve their stable planned keys.

1. `RPT-201` required — Select Referrals and add Accession Number, Referral ID,
   Referral Result ID, Result ID, Referred Lab, Referred Test Name, Referral Date,
   Referral Result Value, Referral Result Date and Referral Status. Use May 7,
   2026 for both dates. Review identifies referral sent dates and does not claim
   Finalized-only results. Save a shared report, reopen it and choose fresh May 7
   dates. Download three rows for `REPORTING-MVP-REPEAT`: two distinct 450 returns
   from Synthetic Reference Lab, dated May 8 and May 9, plus one REQUESTED
   referral with blank returned fields. Returned rows retain distinct link/result
   IDs. Exclude the unsent draft and May 8 sent referral. Repeat on a phone.
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
3. `RPT-303` required — With the 50,000-result synthetic workload loaded, start a
   Sample & Testing spreadsheet with Accession Number and Viral Load for May 7, 2026. While it is Generating, return to the overview and start the same columns
   for May 5. In My Report Queue, choose Cancel on that queued May 5 job, then
   Keep queued. Reload; it remains Queued. Choose Cancel again and confirm Cancel
   export. Expect Cancelled after reload with no download, including after the
   first job finishes. The large report contains 50,000 rows, preserving repeated
   equal values. Repeat on a phone. If processing has already begun, expect a clear
   refusal rather than a false cancellation; prepare both reports in separate tabs
   before starting the large one when more setup time is needed.
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

## RPT-S06 — Navigate and Configure Reports Without Losing Work

Publication status on September 14: RPT-501 through RPT-503 remain live. RPT-504
below is prepared and its application workflow passes publicly at `22e3a66b6175`,
but the Grist authoring connection timed out. Keep the existing 17 live steps
unchanged until this addition is applied. The separate review-picker problem is
fixed in public widget `54b99f8d76ba`; live story selection across separate tabs
and refresh/reload now passes. See `execution.md` for its evidence.

**User story**: As a report user, I can find routine workflows in a clear sidebar and move between a report and its queue without losing my draft. Sign in as admin using the supplied demo login and start from Home. The report steps change your browser draft. The final administration step changes one menu icon and restores its original value; it does not edit laboratory records. Check the sidebar, reporting address, retained draft, keyboard navigation and phone layout against the linked mock. Other reports and More tools intentionally retain older destinations.

1. `RPT-501` required — Start at Home in a desktop-width browser. Inspect Main Menu, Patient & Orders, Reports and Administration in the sidebar. Open Reports, then Other reports; close Other reports again. Open More tools and locate Alerts, then close More tools. Under Reports choose Custom Data Export.
   Expect: The four sections have readable, consistent labels and simple icons. Routine workflows are easy to find. Older reports and tools remain reachable in collapsed groups. Patient Report Print Queue is visibly Not yet connected. Custom Data Export opens at /reports/custom-data-export and exactly that sidebar entry is active.
2. `RPT-502` required — From the reporting overview choose Start a new export, then Sample & Testing. Search for Accession Number and choose Add. Use the sidebar's My Report Queue entry, then browser Back, Forward and reload. Choose Custom Data Export in the sidebar, then Continue current export in the overview.
   Expect: The queue address contains view=queue and only its menu entry is active. Back and Forward restore the corresponding view. Reload keeps the queue usable. Continue current export returns to Choose columns with Accession Number still selected. Any UAT parameters already present in the reporting address remain present when changing sidebar views.
3. `RPT-503` required — With the sidebar open, use Tab to focus a reporting link and Enter to open it. Narrow the browser to a phone-sized window, open the menu button at the top left, expand Reports and choose My Report Queue. Restore desktop width, choose Admin, then Back to main menu.
   Expect: Keyboard focus is visible. At phone width, selecting the queue closes the drawer and leaves the report usable. Main and admin navigation use consistent font sizes and readable wrapped labels. At desktop width the pinned sidebar does not cover the admin page heading. Back to main menu restores the configured sections.
4. `RPT-504` required — As admin, open Admin, Menu Configuration, then Global Menu Configuration. Expand Administration, More tools, then Alerts. Note its current Icon, choose another icon and Save. Reload the page and reopen Alerts. Restore the original icon and Save. Expand the Reports section and its Reports menu; inspect the settings marked Managed by instance configuration.
   Expect: The saved Alerts icon remains selected after reload, and restoring the original value succeeds. Instance-controlled settings stay visible and read-only. The editor is usable at desktop and phone widths. Returning to the main menu retains the configured sections and existing report destinations.

## Preflight and Human Acceptance

Before handoff, automated browser preflight repeats the critical actions against
the deployed host and compares downloaded CSV rows, values, identities and
headers with the fixture oracle. It also verifies checklist JSON, overlay load,
target identity, route capture and one authenticated review submission.

Preflight success means the target is ready for UAT. Human acceptance remains
pending until a reviewer submits or downloads a report bound to the deployed
application SHA, review-tooling SHA and checklist revision.
