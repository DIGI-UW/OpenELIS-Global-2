# JasperReports retirement

## Goal

Remove JasperReports from OpenELIS. Every live server PDF report is generated in
code on OpenPDF, the same way the recent reports are built. Remove the
superseded rendering paths, templates and dependencies, keep every supported
report working, and clear the JasperReports/iText dependency alerts through
removal.

## Why

- JasperReports 6.15.0 has two deserialization advisories (CVE-2025-10492,
  CVE-2026-6009). Every 6.x release is in range.
- JasperReports 7 cannot load 6.x templates. Its only converter is the
  Jaspersoft Studio desktop application; the one batch converter
  (`tknie/jaspergo`) compiled none of the 47 live templates.
- The application already builds its recent PDFs in code through
  `common/util/PdfExportSupport` (EQA, compliance certificates, audit trail,
  e-signature, QC export, inventory, shipment manifests). That code used iText
  5, which is end-of-life and AGPL-licensed and conflicts with this project's
  MPL-2.0 license; step 2 moved it to OpenPDF.
- OpenPDF 3.x is maintained and dual-licensed LGPL-2.1 / MPL-2.0. Its classes
  keep the iText names (`Document`, `PdfPTable`, `PdfWriter`,
  `PdfPageEventHelper`, `Barcode128`) under the `org.openpdf.text` package.

## Audit baseline and current implementation

The audit on 2026-10-07 used the combined stack through
[#4552](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4552), commit
`2a206a381b04be5c0a77caa5acd7483c6804b735`. The baseline is the code at the top
of the stack, including all 20 underlying PRs. The implementation table below
records what exists there; it does not use PR workflow state as a completion
measure.

- Steps 1–7, the routine patient report in step 8, and steps 9–10 are
  implemented. The review findings below still require remediation.
- The audit found 24 unresolved review threads across 14 PRs: three
  high-priority findings and 21 medium-priority findings. All remain applicable
  at this baseline.
- The backend checkpoint fails at test compilation:
  `OrderLabelReprintDecreaseQtyTest` still imports
  `com.itextpdf.text.pdf.PdfReader`. Frontend and E2E checkpoints pass. See the
  [failed backend job](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/37679292586/job/112991901487).
  These are observations for the baseline commit, not proof for later revisions.
- Nine patient-report templates and four shared templates remain, each with a
  `.jasper` and `.jrxml` file. Steps 8b, 8c and 11 remain to be implemented.
- **R1 implementation and formal comment closure are complete. Next milestone:
  R2, clinical and TB reports.** The following milestones finish conversion,
  remove the legacy engine, and verify all supported reporting paths.

The current combined stack tip is
[#4649](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4649),
`codex/reporting-r1-remediation`, directly above #4552. The initial
implementation commit is `43d9d43d035dd269f056302dfa767be1d0f8dd23`, followed by
the multiple-referral correction in `5f61d646a1689a23184af9e6279a3eb0356fad8e`
described below. R1 addresses all 24 baseline findings and moves the remaining
label-reprint test from iText to PDFBox. Visual review also identified and fixed
orphaned routine non-conformity order details/comments; ordinary orders now stay
together, and long orders repeat their identifying details.

Local reporting validation passed **98 tests in 29 suites**, with zero failures,
errors or skips. This includes the existing report preparation/merging tests,
real database fixtures, every workplan variant, page-level identity and
order/notes association checks, configured logos, independent signature and
page-number settings, both antiretroviral layouts and viral-load reprints.
Representative generated pages were inspected on A4 and Letter, including
continuation pages, signatures, legends and printed recollection boxes. PDFs can
be retained by adding `-Dreporting.pdf.output=target/reporting-r1-pdfs` to the
reporting test invocation. The new database fixtures roll back after each test.

All 24 audited GitHub review threads received a linked implementation and
regression-test reply and were formally resolved on 2026-10-07. A fresh audit of
the 20 original PRs found zero unresolved threads. A new
[#4649 finding](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4649#discussion_r4212928941)
is also fixed: all referrals remain in their local-test block, with local
identity repeated when it spans pages. The regression reproduced the original
failure and now covers three referrals per ordinary test and 100 for a long
test; representative pages were inspected. This follow-up also received its fix
and test reply and was formally resolved. A fresh audit of all 21 PRs found
**zero unresolved threads**, covering 25 resolved findings in total.

Current candidate-validation results and the tested commit are recorded in
[#4649](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4649) and its checks.
R1 is validated by the 98 focused reporting tests, rendered-page inspection,
formatting and whitespace checks. Full local CI is not required for these review
fixes; the broad runs were intentionally stopped and are not completion
evidence. GitHub's backend, frontend and E2E checkpoints remain merge
requirements; use #4649's checks for their current state. **R2 is the next
conversion milestone; R2–R5 remain unimplemented.**

At each milestone, update this baseline, the remaining inventory, review-thread
outcomes and validation evidence from the new combined stack tip. Record the
commit used for validation; do not carry old passing results forward as current.

## Acceptance criteria

1. `pom.xml` declares no `net.sf.jasperreports` or `com.itextpdf` artifact, and
   `mvn dependency:tree` shows neither them nor `com.lowagie:itext`. Verify the
   packaged application also contains none of these libraries.
2. No `.jasper` or `.jrxml` files remain in `src/main/resources/reports/`, and
   no production or test code, configuration, menu entry or build tooling
   depends on the retired engine, templates or superseded rendering paths.
3. Every supported report produces a usable PDF from its existing entry point,
   with correct patient/order identification, results, units, ranges, notes,
   completion/reprint state, grouping and configured presentation. CSV and other
   supported exports continue working after their shared Jasper plumbing is
   removed.
4. Schema-valid example data exercises each supported report and relevant
   configuration, including missing values, multiple orders/sites and page
   boundaries. Tests verify content and page ownership; rendered pages verify
   readability, alignment, signatures, checkboxes and absence of clipping.
5. All audited review threads have an evidence-backed outcome, and backend,
   frontend and E2E checkpoints pass on the final candidate. Retain the
   aggregate result and reports from the repository's full local CI package as
   well.
6. The Dependabot tab has no open JasperReports/iText alert after the cleaned
   dependency set reaches the scanned branch. Record that verification
   separately from candidate-code validation.

## Verification for each port and remediation

- **Example data is the validation basis.** Real study orders are unavailable
  and are not a prerequisite. Use representative examples conforming to the same
  schema and production data contracts. Where a report queries the database,
  exercise its data preparation with isolated database fixtures or application
  scenarios as appropriate, not only a hand-filled rendering bean.
- Tests read generated PDFs with `testsupport/PdfText` (PDFBox), so they do not
  depend on the library that wrote the PDF.
- Before deleting a template, render the old and new report with the same
  example rows and parameters when the old renderer can complete. Compare
  extracted text page by page and inspect the PDFs side by side. For an old
  report that fails or loops, record the failure and assert the intended output
  directly.
- **The old output is evidence, not the acceptance oracle.** Preserve valid
  clinical meaning and usable workflows; correct known legacy defects instead of
  reproducing them. Record intentional behavior/layout changes with their
  rationale and regression examples. A review comment that requests an old
  defect back needs an explained disposition, not automatic restoration.
- Each ported report gets content assertions at the level its data comes from. A
  report extending `RetroCIReport`, whose static block needs the study's
  observation types and analytes, retains a separately testable renderer; pair
  that coverage with data-contract/preparation coverage where behavior changes.
- Exercise A4 and Letter, relevant site settings, original/reissued and
  complete/partial reports, multiple patients/orders/sites, long text and enough
  rows to force continuation pages. Each separated page must retain the report
  and patient/order context needed to interpret its contents.
- Patient-result layout review uses representative before/after page images and
  the intended clinical content. Record concrete findings and their resolution;
  approval labels alone do not establish correctness.
- Reports reachable from the UI keep a Playwright PDF-download check. Check the
  PDF content where needed to establish that the intended report was produced,
  rather than accepting an error-notice PDF as success.
- Use the supported `scripts/dev-stack` scenarios for interactive validation and
  `scripts/run-ci-checks.sh` for the final retirement candidate in R5. Review
  remediations in R1 use focused reporting tests and rendered-page checks; full
  local CI is not required for those fixes. Tests own and scope their example
  data; do not rely on records left by another test.

## Inventory

### Removed

16 templates that nothing references (no Java string, no subreport expression,
no menu entry), plus 3 sources that had no compiled template: `CIIPCIFooter`,
`MauritiusProtocolSheet`, `PatientReportCDI_INSP`, `PatientReportCDI_cedres`,
`PatientReportCDI_vreduit_cedres`, `Patient_ARV_Version1_old`,
`Patient_VL_Version_Nationale_opp`, `RetroCIHeader-cedres`,
`RetroCIHeader-insp`, `RetroCIHeader1`, `RetroCIHeader_old`, `Workplan_BioChem`,
`Workplan_Default`, `Workplan_Serology`, `Patient_ARV_Version1 (2)`,
`RetroCI_Patient_EID_info - Copy`, and the sources
`Patient_ARV_Followup_patient_info_old`, `Patient_ARV_Version11`, `patient`. The
reports `.gitignore` listed only tracked files and was removed with them.

### Ported

- Workplans: `WorkplanByTest`, `WorkplanByTestSection`, `WorkplanResultsByTest`
  and `WorkplanResultsByTestSection` are built by
  `workplan/reports/WorkplanPdf`. `ElisaWorkplan` and `ElisaWorkplanReport` were
  removed; only a commented-out line referred to them.
- Error notice: `NoticeOfReportError` is drawn by `ReportErrorPdf` for every
  report that cannot be produced, and for a collection report with nothing to
  print. `Report.runReport` renders it before any template.
- Validation backlog: `ValidationBacklog` is drawn by `ValidationBacklogReport`.
- Activity and rejection reports: `ActivityReport` and `RejectionReport` are
  drawn by `ActivityReport` and `RejectionReport` through `ManagementReportPdf`,
  for every by-test, by-panel and by-section variant.
- Non-conformity: `NonConformityByReceivedDate` and
  `NonConformityByGroupCategory` are drawn by `NonConformityReportPdf` for the
  routine (Haiti) and study (RetroCI) variants.
- Shared header: `ReportHeaderPdf` draws the header of the code-built reports
  from site information (logos, site name, additional site information, lab
  manager). It replaces `GeneralHeader`, `CILNSPHeader` and `RetroCIHeader` for
  the reports ported so far.
- Paper size: each template fixed its own page (Letter, A4 or a custom width).
  Every code-built PDF, ported or built in code before this work, prints on the
  site's paper size through `PdfExportSupport.pageSize()`: A4 by default, or
  Letter, set under Printed Reports Configuration. Landscape reports turn it,
  and the statistics report uses the next size up (A3 or Tabloid). Label sheets
  keep their own size, `ManifestPDFServiceImpl` (marked for removal) stays on
  A4, and so do the PDFs built in the browser (compliance, vector surveillance,
  shipment). Decided 2026-10-04. The setting arrives with the routine patient
  report PR; the PRs below it print on A4.
- Referred out: `ReferredOutBySite` is drawn by `ReferredOutReport`, with the
  shared header.
- Indicators: `HIVSummary` and `LabAggregate` are drawn by `IndicatorHIV` and
  `IndicatorAllTest` (and their LNSP, clinical and CDI variants) under the
  shared header, through `IndicatorReport.startPdf`. `ConfirmationSummary` (and
  its table components) is drawn by `ConfirmationReport`, and `StatisticsReport`
  by `StatisticsReport` on the next paper size up, landscape.
  `IPCIRealisationTest` is drawn by `IPCIRealisationReport` (its French labels
  kept) and `RetroCI_backlog` by `IndicatorSectionPerformanceReport`, whose bar
  chart JFreeChart draws into the PDF.
- Patient results: `PatientReportCDI_vreduit`, the report the patient results
  screen requests, is drawn by `PatientCILNSPClinical_vreduit` through
  `PatientResultsPdf`, with the accreditation logos and notes line (OGC-686) in
  the shared header.
- Cold storage: `FreezerTemperatureMonitoringReport` is drawn by
  `coldstorage/service/FreezerTemperatureReportPdf` for the daily, weekly and
  monthly logs.
- Study antiretroviral results: `Patient_ARV_Version1` and
  `Patient_ARV_Version2` are drawn by `StudyArvResultsPdf` for
  `PatientARVReport` and its six report classes, with the study's analyzers,
  methods and reference values kept as printed.
- Study early infant diagnosis results: `RetroCI_Patient_EID` and
  `RetroCI_Patient_EID_Version2`, with their `_info` subreports, are drawn by
  `StudyEidResultsPdf` for `PatientEIDReport`'s two report classes.
- Study viral load results: `Patient_VL_Version_Nationale` is drawn by
  `StudyVlResultsPdf` for `PatientVLReport`, through the patient block in
  `StudyPatientBlockPdf`, which now also prints pregnancy, breastfeeding and the
  validation date.
- Study report signatures: the study reports no longer draw the fixed signature
  images (`ALLSign.jpg`, `EIDSign.jpg`, `VLSign.jpg`); like the fixed CIRBA
  header, a signature naming one physician is site identity. Decided 2026-10-04.
- Study indeterminate results: `Patient_Indeterminate_Version1`,
  `Patient_Indeterminate_Version2` and `Patient_Indeterminate_ByLocation` are
  drawn by `StudyIndeterminateResultsPdf` for `PatientIndeterminateReport` and
  its three report classes. With them goes `Patient_ARV_Followup_patient_info`,
  the patient block `StudyPatientBlockPdf` replaced, and `CILNSPHeader`, which
  nothing loaded once the non-conformity checklist was ported.
- Study non-conformity: `NonConformityNotification`,
  `RetroCI_FollowupRequired_ByLocation` and `retroCINonConformityByLabno` are
  drawn by `StudyNonConformityPdf` for `RetroCINonConformityNotification`,
  `RetroCIFollowupRequiredByLocation` and `RetroCINonConformityByLabno`, their
  labels kept in French. The checklist by lab number draws the study patient
  block through `StudyPatientBlockPdf`, which the study patient reports of step
  9 reuse in place of the `Patient_ARV_Followup_patient_info` subreport.

### Live reports

At the baseline above, 13 templates remain (13 compiled files and 13 sources).
These are outstanding conversions or removals, not a decision to retain Jasper.
Grouped by family, with the code that loads them:

| Family                     | Templates                                                                                                                                                                                                                                                | Loaded by                                                                                                                               |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Patient results            | `PatientReportCDI`, `PatientClinicalReport`, `TBPatientReport`, `PatientPathologyReport`, `PatientCytologyReport`, `PatientImmunoChemistryReport`, `PatientImmunoChemistryResults`, `DualInSituHybridizationReport`, `BreastCancerHormoneReceptorReport` | `PatientCILNSPClinical`, `PatientClinicalReport`, `TBPatientReport`, the pathology report creators                                      |
| Shared headers and footers | `CDIHeader`, `CILNSPFooter`, `GeneralHeader`, `RetroCIHeader`                                                                                                                                                                                            | subreports of the families above, chosen through the `headerName` parameter; `CILNSPFooter` is passed by name, but no template draws it |

### Code outside PDF reports that touches these libraries

- Since step 2, `PdfExportSupport` and the EQA, compliance, audit trail,
  e-signature, inventory, QC, shipment and barcode label writers run on OpenPDF.
  `com.lowagie` iText 2.1.7 stays on the classpath only as a dependency of
  `jasperreports`.
- The CSV export report creators import Jasper types through the shared
  `IReportCreator` / `Report` classes, and
  `reports/valueholder/common/JRHibernateDataSource` imports Jasper. These go
  when the last template is ported.
- `compliance/controller/rest/CertificateFonts` loads DejaVu Sans from the
  `jasperreports-fonts` jar, so the certificate needs another source for that
  font before the jar is removed.
- The templates render with the standard Helvetica font, the same font the
  code-generated PDFs use, so porting a report does not change which characters
  it can print.

## Original steps: implementation at the stack tip

Keep the original step numbers so existing PRs remain traceable. "Implemented"
means present in the combined source, not free of review findings or fully
validated. The remaining milestone sequence follows this table.

| Step | Scope                                                                                          | Implementation at the baseline                                          | PRs                        |
| ---- | ---------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | -------------------------- |
| 1    | Remove unused templates and establish the roadmap                                              | Implemented                                                             | #4531                      |
| 2    | Move existing PDF writers and barcode labels from iText 5 to OpenPDF                           | Implemented; remaining test import tracked in R1                        | #4532                      |
| 3    | Remove unreachable report code                                                                 | Implemented; final sweep in R4                                          | #4534                      |
| 4    | Workplans                                                                                      | Implemented; one review thread in R1                                    | #4535                      |
| 5    | Error notice, backlog, activity, rejection, shared header, non-conformity and referral reports | Implemented; seven review threads in R1                                 | #4537, #4538, #4540, #4541 |
| 6    | HIV/lab aggregate, confirmation, statistics, realisation and section-performance indicators    | Implemented; five review threads in R1                                  | #4542–#4545                |
| 7    | Cold storage report                                                                            | Implemented; one review thread in R1                                    | #4539                      |
| 8a   | Routine patient results, accreditation header and site paper-size setting                      | Implemented; three review threads and content/layout verification in R1 | #4546                      |
| 8b   | Other clinical and TB patient reports                                                          | Outstanding; R2                                                         | —                          |
| 8c   | Pathology-family patient reports                                                               | Outstanding; R3                                                         | —                          |
| 9    | Study antiretroviral, infant-diagnosis, viral-load and indeterminate results                   | Implemented; four review threads in R1                                  | #4549–#4552                |
| 10   | Study non-conformity notification, follow-up list and checklist                                | Implemented; three review threads in R1                                 | #4547–#4548                |
| 11   | Remove the remaining Jasper engine, supporting code, templates and dependencies                | Outstanding; R4                                                         | —                          |

## Remaining milestones

Execute R1 → R2 → R3 → R4 → R5 on the combined stack. Each implementation
milestone is a reviewable PR with its own checks. If a milestone exceeds three
days, split it into numbered submilestones with explicit completion criteria;
retain this order and do not defer R1 findings into the later report
conversions.

### R1 — Review remediation and regression coverage (implementation and comments complete)

The build blocker is fixed by moving `OrderLabelReprintDecreaseQtyTest` to the
existing PDFBox test support. The three high-priority clinical findings, shared
pagination/text issues and report-specific corrections are implemented with
regression coverage in R1. These 14 rows account for all 24 review threads at
the baseline; each linked thread is now formally resolved with its fix and test
evidence. The additional multiple-referral finding on #4649 is fixed and covered
by `ReferredOutReportTest.everyReferralRetainsItsLocalTestContextAcrossPages`.
That follow-up is published and formally resolved. Candidate-validation results
and the tested commit are tracked on #4649, as described above.

| PR    | Threads | Remediation and regression examples                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ----- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| #4535 | 1       | [Repeat workplan identity/date](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4535#discussion_r4176685611) on every page; exercise all workplan variants with enough rows to overflow.                                                                                                                                                                                                                                                                                       |
| #4538 | 3       | [Interpret supported note/result formatting](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4538#discussion_r4176687396), [restore configured logos](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4538#discussion_r4176687400), and [repeat report identity/scope](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4538#discussion_r4176687404); cover entities, line breaks, configured images and multi-page activity/rejection output.                              |
| #4539 | 1       | [Restore the omitted descriptive statement](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4539#discussion_r4177231982), or document an intentional wording correction; verify the full intended text for daily, weekly and monthly output.                                                                                                                                                                                                                                   |
| #4540 | 2       | [Preserve footer identity/page information](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4540#discussion_r4176686492) and [reserve supervisor-signature space](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4540#discussion_r4176686494); test the page-number/signature settings and nearly full pages.                                                                                                                                                               |
| #4541 | 2       | [Keep the local-test row with its first referral](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4541#discussion_r4176689159) and [repeat report/destination identity](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4541#discussion_r4176689161); force a boundary immediately before referral details.                                                                                                                                                                  |
| #4542 | 1       | [Use non-overflowing sort comparisons](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4542#discussion_r4177232799); test panel, ordered-test and unspecified-order buckets together.                                                                                                                                                                                                                                                                                          |
| #4543 | 2       | [Separate requesting sites](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4543#discussion_r4177232421) and [keep order identity with results](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4543#discussion_r4177232424); cover multiple sites and both short and overflowing order blocks.                                                                                                                                                                              |
| #4544 | 2       | [Repeat statistics filters](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4544#discussion_r4177233324) and [print the report date on every page](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4544#discussion_r4177233329); inspect each page, not just whole-document text.                                                                                                                                                                                            |
| #4546 | 3       | **High priority:** [show each order's completion state correctly](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4546#discussion_r4211429984). Also [interpret notes formatting](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4546#discussion_r4211429999) and [repeat section/column labels](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4546#discussion_r4211430009); test a patient with complete and partial orders, corrected notes and a multi-page section. |
| #4547 | 2       | [Repeat follow-up report identity](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4547#discussion_r4211421372) and [keep notes with the identifying order](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4547#discussion_r4211421380); cover long notes and page boundaries.                                                                                                                                                                                              |
| #4548 | 1       | [Restore usable recollection choices](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4548#discussion_r4211409754); inspect individual printed boxes on the tube and infant-diagnosis forms and verify the intended selection.                                                                                                                                                                                                                                                 |
| #4549 | 1       | **High priority:** [restore the per-order duplicate marker](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4549#discussion_r4211427354) in both antiretroviral layouts, including attached pages; test original and reissued examples.                                                                                                                                                                                                                                        |
| #4551 | 2       | **High priority:** [restore the viral-load duplicate marker](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4551#discussion_r4211501450). Also [apply the intended pregnancy/breastfeeding visibility](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4551#discussion_r4211501475); cover the female condition, other/missing values and stale observations.                                                                                                               |
| #4552 | 1       | [Repeat site/study and serology identity](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4552#discussion_r4211403652) on by-location continuation pages; exercise several pages and service/doctor/date groups.                                                                                                                                                                                                                                                               |

Completion evidence:

- Each thread has a linked fix and meaningful regression evidence, or a
  documented explanation of why an intentional correction supersedes the
  requested old behavior. Record the outcome before resolving the thread.
  Re-read current threads so newly raised findings are included.
- Shared header/footer and text handling is reused where suitable. Composed page
  events retain report identity, dates, legends, page numbers and signatures
  without overwriting one another or overlapping content.
- Representative PDFs have been inspected on A4 and Letter, including page
  boundaries and clinical status. Existing valid content is retained and known
  legacy defects are explicitly corrected.
- Focused reporting tests, rendered-page inspection, formatting and whitespace
  checks pass for the remediated candidate. Full local CI is not an R1
  completion gate. Backend, frontend and E2E checkpoints remain required before
  merging; record their current results on #4649.

### R2 — Complete clinical and TB reports (original step 8b)

Convert `PatientReportCDI`, `PatientClinicalReport` and `TBPatientReport` using
the corrected shared rendering support. Trace their current callers, menu/report
selection and settings before changing them. Preserve correct clinical content;
record and test fixes for existing defects. Delete each superseded template and
its obsolete selection/configuration path in the same change.

Completion evidence: every retained variant renders through its production data
preparation and entry point using schema-valid examples; content and multi-page
layout checks cover relevant site settings, dates, results, notes, status and
patient/order identity. No caller still selects any of the three old templates.

### R3 — Complete pathology reports (original step 8c)

Convert `PatientPathologyReport`, `PatientCytologyReport`,
`PatientImmunoChemistryReport`, `PatientImmunoChemistryResults`,
`DualInSituHybridizationReport` and `BreastCancerHormoneReceptorReport`. Cover
narrative text, structured results, images and sign-off wherever the existing
report contract uses them, with long and incomplete examples as well as ordinary
ones. Reuse the corrected common patient-report behavior.

Remove the converted templates and `PatientReport`'s obsolete Jasper plumbing;
remove `CDIHeader` and `CILNSPFooter` once their last consumers are gone.
Completion evidence: all six supported report paths work through their existing
entry points, their outputs pass content and rendered-page checks, and no active
caller depends on the deleted implementations. Refresh the remaining inventory.

### R4 — Remove the legacy engine and remaining dead paths (original step 11)

- Remove `jasperreports`, `jasperreports-fonts`, transitive old iText and all
  remaining templates, including `GeneralHeader` and `RetroCIHeader` after
  verifying there are no consumers.
- Remove `JRHibernateDataSource`, Jasper types from `IReportCreator` / `Report`,
  obsolete template dispatch, parameters, loaders and fallback paths. Preserve
  working CSV exports, collection merging and error-notice behavior through the
  current interfaces.
- Give `CertificateFonts` an independent, licensed DejaVu source and verify that
  certificate threshold symbols still render correctly.
- Check real consumers before removing `jfreechart` or `barbecue`. The current
  section-performance chart uses JFreeChart; a dependency still required by a
  supported feature is not dead merely because Jasper also used it.
- Sweep production/test imports, configuration and menu selectors, static
  assets, build/package tooling and documentation for retired library/template
  references. Remove superseded implementations; migrate callers before deleting
  any remaining obsolete path, including the already-marked-for-removal
  `ManifestPDFServiceImpl`. Historical migration documentation may name removed
  components for traceability.

Completion evidence: zero Jasper/iText dependencies in the resolved tree and
packaged application; zero report templates or active legacy-engine references;
all retained exports, labels, certificates, report merging and error paths still
work. Do not retain compatibility scaffolding for deleted report engines.

### R5 — Verify and close the complete reporting target

Build a final inventory from active report creators, configuration, menus and
endpoints. It must cover the ported and newly converted report families plus the
existing OpenPDF writers affected by the library and paper-size changes: EQA,
compliance, audit trail, e-signature, inventory, QC, shipment and barcode
labels. For each supported path record its entry point, example scenario,
relevant configuration, automated check and inspected output. Reconcile the
inventory against code so no reachable report is silently omitted.

Verify normal, empty/error and boundary cases where applicable, especially mixed
completion/reprint states, multiple patients/sites, long results/notes, absent
optional values, report grouping and separated-page identification. Confirm site
logos, accreditation information, signature controls and paper sizes; labels
retain their stock dimensions. Browser-generated PDFs remain supported and their
existing behavior is checked where shared settings or interfaces affect them.

Close only when every acceptance criterion above has evidence on the final
candidate, all actionable review findings are addressed, and the remaining
legacy inventory is empty. Record dependency-alert clearance after the scanned
branch updates. Update this roadmap with final commits and validation results;
do not substitute an old test run or a PR status for working report behavior.

## Rejected alternatives

- **Port the site-specific headers as they are.** `CILNSPHeader` hardcodes the
  Côte d'Ivoire ministry block and `RetroCIHeader` hardcodes the CIRBA Abidjan
  laboratory, its phone, email and responsible doctor. Decided 2026-10-03: every
  code-built report uses one header from site information instead; a site that
  relied on that text sets its name and logos in site information.

- **Upgrade to JasperReports 7.** Every template must be converted by hand in
  Jaspersoft Studio, and the result keeps a designer-only, binary-compiled
  template format.
- **Port onto iText 5.** It matches the current helper but grows an end-of-life,
  AGPL-licensed dependency inside an MPL-2.0 project.
- **Stay on 6.15 and dismiss the alerts.** Templates load only from the WAR, so
  exposure is low, but it leaves an unmaintained engine in place with no upgrade
  path.
