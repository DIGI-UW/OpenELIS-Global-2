# JasperReports retirement

## Goal

Remove JasperReports from OpenELIS. Every live PDF report is generated in code
on OpenPDF, the same way the recent reports are built, and the 6 open Dependabot
alerts on `net.sf.jasperreports:jasperreports` close because the dependency is
gone.

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

## Acceptance criteria

1. `pom.xml` declares no `net.sf.jasperreports` or `com.itextpdf` artifact, and
   `mvn dependency:tree` shows neither them nor `com.lowagie:itext`.
2. `src/main/resources/reports/` holds no `.jasper` or `.jrxml` files.
3. Every report listed under Live reports produces a PDF from its existing menu
   entry or endpoint, with the same content as before the port.
4. The Dependabot tab has no open alert on JasperReports or iText.

## Verification for each port

- Tests read generated PDFs with `testsupport/PdfText` (PDFBox), so they do not
  depend on the library that wrote the PDF.
- Before deleting a template, render it and the new code from the same report
  data (same rows and parameters) and compare the extracted text page by page,
  then compare the two PDFs side by side. Patient result reports are clinical
  documents; their layout is reviewed, not only their text.
- Each ported report gets a test that renders it and asserts on its content, at
  the level the report's data comes from (integration test when it queries the
  database).
- Reports reachable from the UI keep a Playwright check that the PDF downloads.

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
- Cold storage: `FreezerTemperatureMonitoringReport` is drawn by
  `coldstorage/service/FreezerTemperatureReportPdf` for the daily, weekly and
  monthly logs.

### Live reports

38 templates remain. Grouped by family, with the code that loads them:

| Family                     | Templates                                                                                                                                                                                                                                                                                                                                           | Loaded by                                                                                                                                                                                 |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Management                 | `ReferredOutBySite`, `StatisticsReport`, `NonConformityByReceivedDate`, `NonConformityByGroupCategory`                                                                                                                                                                                                                                              | `ReferredOutReport`, `StatisticsReport`, `NonConformityByDate`, `NonConformityBySectionReason`                                                                                            |
| Indicators                 | `HIVSummary`, `LabAggregate`, `IPCIRealisationTest`, `ConfirmationSummary` (table component), `RetroCI_backlog` (bar chart)                                                                                                                                                                                                                         | `IndicatorHIV`, `IndicatorAllTest`, `IPCIRealisationReport`, `ConfirmationReport`, `IndicatorSectionPerformanceReport`                                                                    |
| Patient results            | `PatientReportCDI`, `PatientReportCDI_vreduit`, `PatientClinicalReport`, `TBPatientReport`, `PatientPathologyReport`, `PatientCytologyReport`, `PatientImmunoChemistryReport`, `PatientImmunoChemistryResults`, `DualInSituHybridizationReport`, `BreastCancerHormoneReceptorReport`                                                                | `PatientCILNSPClinical`, `PatientCILNSPClinical_vreduit`, `PatientClinicalReport`, `TBPatientReport`, the pathology report creators                                                       |
| Shared headers and footers | `CDIHeader`, `CILNSPHeader`, `CILNSPFooter`, `GeneralHeader`, `RetroCIHeader`                                                                                                                                                                                                                                                                       | subreports of the families above, chosen through the `headerName` parameter                                                                                                               |
| Study patient reports      | `Patient_ARV_Version1`, `Patient_ARV_Version2`, `Patient_ARV_Followup_patient_info`, `RetroCI_Patient_EID`, `RetroCI_Patient_EID_Version2`, `RetroCI_Patient_EID_info`, `RetroCI_Patient_EID_info_Version2`, `Patient_VL_Version_Nationale`, `Patient_Indeterminate_Version1`, `Patient_Indeterminate_Version2`, `Patient_Indeterminate_ByLocation` | `PatientARVReport` and its version subclasses, `PatientEIDReport`, `PatientVLReport`, `PatientIndeterminateReport`, `PatientIndeterminateByLocationReport`, `PatientSpecialRequestReport` |
| Study non-conformity       | `retroCINonConformityByLabno`, `NonConformityNotification`, `RetroCI_FollowupRequired_ByLocation`                                                                                                                                                                                                                                                   | `NonConformityByLabno`, `RetroCINonConformityNotification`, `RetroCIFollowupRequiredByLocation`                                                                                           |

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

## Plan

Each step is one or more stacked PRs. Status is updated in this file as steps
land.

| Step | Change                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Status                  |
| ---- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| 1    | Delete the dead templates and add this roadmap                                                                                                                                                                                                                                                                                                                                                                                                                              | in review               |
| 2    | Add OpenPDF; port `PdfExportSupport`, the 18 iText 5 files and `BarcodeLabelMaker` to it; remove `itextpdf`                                                                                                                                                                                                                                                                                                                                                                 | in review               |
| 3    | Delete report code nothing reaches: the `ReportsServlet` subsystem (no UI path; two of its providers load templates that are not in the repository, the third names a class that does not exist) and the freezer excursion and audit trail report creators (never constructed)                                                                                                                                                                                              | in review               |
| 4    | Port the workplans                                                                                                                                                                                                                                                                                                                                                                                                                                                          | in review               |
| 5    | Port the management reports and the shared headers and footers they use, in three PRs: the error notice every report shares, a render hook in `Report` and the validation backlog; the activity and rejection reports; the shared headers with the referred-out, non-conformity and statistics reports. `ReferredOutReport` extends `PatientReport` and `StatisticsReport` extends `IndicatorReport`; if their ports need those parents changed, they move to steps 8 and 6 | first two PRs in review |
| 6    | Port the indicators, including the bar chart and the table component                                                                                                                                                                                                                                                                                                                                                                                                        | not started             |
| 7    | Port the cold storage report                                                                                                                                                                                                                                                                                                                                                                                                                                                | in review               |
| 8    | Port the patient result reports                                                                                                                                                                                                                                                                                                                                                                                                                                             | not started             |
| 9    | Port the study patient reports                                                                                                                                                                                                                                                                                                                                                                                                                                              | not started             |
| 10   | Port the study non-conformity reports                                                                                                                                                                                                                                                                                                                                                                                                                                       | not started             |
| 11   | Remove `jasperreports`, `jasperreports-fonts`, the remaining templates, `JRHibernateDataSource` and the Jasper types in `IReportCreator` / `Report`; give `CertificateFonts` its own DejaVu source; re-check whether `jfreechart` and `barbecue` are still used                                                                                                                                                                                                             | not started             |

## Rejected alternatives

- **Upgrade to JasperReports 7.** Every template must be converted by hand in
  Jaspersoft Studio, and the result keeps a designer-only, binary-compiled
  template format.
- **Port onto iText 5.** It matches the current helper but grows an end-of-life,
  AGPL-licensed dependency inside an MPL-2.0 project.
- **Stay on 6.15 and dismiss the alerts.** Templates load only from the WAR, so
  exposure is low, but it leaves an unmaintained engine in place with no upgrade
  path.
