# Reporting MVP UAT Contract

This file maps the original `openelis-work` user stories and approved design
to the agreed reporting MVP and its UAT coverage. The central Grist
document in `DIGI-UW/openelis-review-tooling` is the live checklist source of
truth after deployment. Do not serve this Markdown as a second checklist.

UAT follows each development stage and helps discover issues as work proceeds.
Automated end-to-end checks and human review exercise the same workflows,
fixtures and expected results at different levels of detail. Publish currently executable story steps with
the stage scope and known gaps, then extend them as capabilities arrive. Do not
withhold the public target until the complete MVP passes.

## Iteration validation and review responsibilities

Follow the [cross-project ownership contract](https://github.com/DIGI-UW/openelis-review-tooling/blob/codex/grist-backend-authoring/docs/validation-ownership.md).
The original [reporting stories and specification](https://github.com/DIGI-UW/openelis-work/blob/5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f/designs/reports/custom-data-export.md)
and companion mock govern user intent. Grist presents that intent and records
review feedback. Implementation-specific automated E2E and video proof belong
with the application code. Eventual synchronization with `OpenELIS-QA` preserves
story/criterion identity, scope deltas and evidence; it is not implemented yet.

The primary boundary is automated verification versus human evaluation. Timing
is a separate choice: automated checks run before merge and after deployment;
human UAT primarily reviews deployed increments, including useful PR previews
before merge. Do not wait for the whole MVP or a merge to publish a usable stage.

| Check                                                           | When it runs                                                                                                                     | What it establishes                                                                                                                                          |
| --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Focused unit, component and database tests                      | Each affected code iteration; relevant CI before merge                                                                           | Mapping, validation, state transitions, concurrency and persistence behave correctly.                                                                        |
| Existing Playwright workflows                                   | Affected workflows before merge; the assembled stack before calling the checkpoint merge-ready                                   | The actual application completes the workflow and produces the expected CSV.                                                                                 |
| Implementation video proof                                      | Refresh for changed complete workflows before the review/merge checkpoint, using the existing recording workflow                 | Reviewers can inspect the actual implementation on the identified tested build; assertions still run.                                                        |
| Deployed smoke check                                            | After every usable deployment, whether a preview or merged build                                                                 | The identified deployed version can sign in, navigate, generate and download; its configuration works. Reuse a small selection of the same Playwright tests. |
| Grist human UAT                                                 | Feedback on deployed increments, especially the integrated post-merge version; request a recheck when affected feedback is fixed | The workflow is useful and understandable, the design matches the mock, and reviewers can record unexpected behavior.                                        |
| Workload, restart, migration and worker-isolation qualification | When related code/configuration changes and at the applicable release checkpoint                                                 | Operational behavior holds under the qualified conditions. These are not mandatory human exercises on every iteration.                                       |

The original stories and canonical mock, with explicit user-approved MVP deltas,
define expected behavior. The implementation specification scopes the increment. Executable
assertions live in the application repository. Grist owns the live human
walkthrough and feedback. Connect them through the existing RPT story/step keys
and PR/evidence links; do not create another checklist store, a generated browser
test system or a new synchronization service.

| Walkthrough keys | Original story / expectation                                                    | Approved scope notes                                                                                                    |
| ---------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| RPT-S01, RPT-S05 | OGC-479; choose fields, filter, review and export (FR-1/2/3), turnaround domain | Both layouts, every repeated result and turnaround beside each test are explicit MVP decisions.                         |
| RPT-S02          | OGC-483; saved report settings (FR-7)                                           | Definitions are shared for this MVP; do not restore the source document's personal-only restriction.                    |
| RPT-S03          | OGC-479; report families and date anchors (FR-1-008, FR-2-001)                  | Referrals and Non-Conformance are publicly connected; the approved date fallback is visibly labeled. |
| RPT-S04          | OGC-481; queue, downloads and recovery (FR-6)                                   | Common queued delivery preserves the approved builder and queue experience.                                             |
| RPT-S06          | Explicitly approved navigation/configuration follow-up                          | Original navigation story mapping remains pending; these RPT keys are walkthrough IDs, not invented upstream story IDs. |

Retain one concise human journey per story. Exact row identities, equal-value
multiplicity, every header/cell, stale-write conflicts and worker races belong
in automated assertions. A reviewer should still download a representative
report and assess whether it is understandable and useful. They need not count
50,000 rows or repeat all automated edge cases. The detailed expectations below
remain the acceptance reference; condense live Grist instructions around the
human journey without deleting criteria or changing stable key meanings.

| Shared workflow                            | Automated evidence                                                                                  | Human review focus                                                                  |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| RPT-S01 routine export; RPT-S05 turnaround | Both layouts, repeated identities/values, per-result intervals and ordered headers                  | Find the right fields, understand filters, and use the downloaded report.           |
| RPT-S02 shared reports                     | Save/use/copy/update/delete, fresh dates, separate users and stale-edit behavior                    | Reuse a routine report with little setup; understand editing versus copying.        |
| RPT-S03 other sources                      | Referral period/returned/pending rows and Non-Conformance occurrences/date basis through the same builder | Understand source choices and whether the available workflow fits the task.         |
| RPT-S04 queue/recovery                     | Frozen retries, expiry, cancellation and file identity; heavy workload separately                   | Find completed work and understand progress, failure and recovery actions.          |
| RPT-S06 navigation/configuration           | URL state, retained drafts, responsive navigation and persistence of menu settings                  | Discoverability, consistent typography, keyboard/phone usability and mock fidelity. |

For each iteration, identify changed behavior and its RPT keys, run the affected
lower-level tests and existing browser workflows, then fix failures and repeat
only affected checks. Capture or refresh implementation video proof for the
changed complete workflow before the review/merge checkpoint, retaining its
assertions and exact test/build identity. After deployment, capture the target identity, run the
deployed smoke selection plus the changed workflow, and verify that the identity
did not change during the run. Record application/test revisions, configuration,
checklist revision, selected checks, skips, results, output and known gaps.
Update Grist when the review journey or expected behavior changes, not merely
because a commit changed. Automated runs never fill in human Pass answers.

Keep PR validation, deployed validation and human feedback as separate states.
A successful smoke run is not full regression or human acceptance. A skipped
test is not a pass; a missing prerequisite for a required check remains open.
Non-Conformance is now in the usable public stage; full human acceptance remains
open. Stop and reassess after two attempts without verified progress
or new evidence; pause only work dependent on an unresolved behavior decision.

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
examples remain reusable after a run. RPT-303 now has a repeatable public
50,000-result fixture and passing desktop/phone cancellation checks. Its live
checklist instruction remains unpublished because authoring access is unavailable. No human acceptance
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

Current-stage availability check: `RPT-200` required — Start a new export and
inspect the three report-type cards. Referrals is publicly connected.
Non-Conformance is publicly qualified at `3de726b8d3`; visible cards alone do
not establish human acceptance. The public Grist checklist is published;
its Non-Conformance instruction still needs the fixture update below when deployed.

1. `RPT-201` required — Select Referrals and add Accession Number, Referral ID,
   Referral Result ID, Result ID, Referred Lab, Referred Test Name, Referral Date,
   Referral Result Value, Referral Result Date and Referral Status. Use May 7,
   2026 for both dates. Review identifies referral sent dates and does not claim
   Finalized-only results. Save a shared report, reopen it and choose fresh May 7
   dates. Download three rows for `REPORTING-MVP-REPEAT`: two distinct 450 returns
   from Synthetic Reference Lab, dated May 8 and May 9, plus one REQUESTED
   referral with blank returned fields. Returned rows retain distinct link/result
   IDs. Exclude the unsent draft and May 8 sent referral. Repeat on a phone.
2. `RPT-202` required — Select Non-Conformance and add Accession Number,
   Occurrence ID, Rejection Reason, Rejection Date, Date Basis, Event Date,
   Recorded Date, Record Source and Status. Use May 10, 2026 for both dates.
   The filters and review explain event-date precedence and recorded-date
   fallback. Download four distinct `REPORTING-MVP-NCE` occurrences, all with
   reason `RPT-MVP-HAEMOLYSIS`: one Event date row (May 10 event, May 12 recorded),
   two Recorded date rows (no event date, May 10 recorded), and one Recorded
   rejection date row from the legacy source. Native rows have OPEN status;
   unavailable legacy status is blank. The May 9 event and wholly undated event
   must not appear. Repeat at phone width. These synthetic occurrences are
   separate records; equal values are not grounds for deduplication.
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
