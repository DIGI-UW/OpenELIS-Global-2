# Changelog

Human-readable release notes for OpenELIS Global, written for three audiences:
decision-makers, implementers & admins, and lab users. This file is generated
from the OpenELIS features inventory (do not hand-edit).

From 3.3.0, versions follow Semantic Versioning (see RELEASES.md). Releases up
to and including the 3.2 lines keep their four-part numbers.

See also: the live product roadmap
(https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/640319495), the per-audience
release-notes pages for v3.3
(https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1756889121) and v3.2
(https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1412136974), and earlier
releases v3.0/v2.7/v2.6
(https://uwdigi.atlassian.net/wiki/spaces/OG/pages/645005332).

## v3.3

## 3.3.0

### For decision-makers

- Faster, safer registration at reception: fewer fields, fewer duplicate
  patients, and referrals started at the bench instead of a separate screen.
  (OGC-1266, OGC-1419, OGC-1422, OGC-1423, OGC-1424, OGC-1443) · _Order & sample
  entry_
- Orders are no longer blocked at reception for missing demographics, without
  hiding the effect on interpretation. (OGC-1362, OGC-1483, OGC-1488, OGC-1497,
  OGC-1240) · _Patient & order enhancements_
- Critical results get a recorded acknowledgement at the point of entry,
  supporting ISO 15189 critical-result handling. (OGC-1417) · _Results entry_
- QC failures now block release of the affected results, closing a quality gap.
  (OGC-1267, OGC-1361) · _Result validation_
- A complete proficiency-testing lifecycle inside OpenELIS, for both EQA
  providers and participating labs, with evidence for analyst competency.
  (OGC-608, OGC-1243, OGC-1244, OGC-1245, OGC-1246, OGC-1247, OGC-1249,
  OGC-1250, OGC-1251, OGC-1252, OGC-1255, OGC-1256, OGC-1257, OGC-1260) · _EQA /
  proficiency testing_
- Stock-outs become visible before they happen: the lab sees days of supply left
  and a suggested reorder for every item. (OGC-438) · _Inventory & supplies
  management_
- Connecting and updating instruments is safer and needs less engineering help:
  labs review the effect of a profile change before it reaches patient results.
  (OGC-1054, OGC-1409) · _Analyzer integration framework_
- Instrument connections are authenticated and only administrators can change
  them. **[security]** · _Analyzer integration framework_
- Coded results travel with standard codes, which analyzers, FHIR exchange and
  surveillance exports rely on. (OGC-1054) · _Standardized terminology_
- Facility and site lists can be loaded in bulk from national registries instead
  of typed in by hand. (OGC-1363, OGC-1420, OGC-1263, OGC-1264, OGC-1265) ·
  _Organizations management_
- Reports reach patients and clinicians faster, with a clear view of what has
  and has not been printed. (OGC-431) · _Patient report print queue_
- A clearer, easier-to-read patient report for clinicians. (OGC-932) · _Final
  PDF patient report layout_
- Removes the iText 5 dependency from PDF generation and starts retiring
  JasperReports, reducing licensing and maintenance risk.
- The base for sending reports, priority alerts and patient notices
  electronically, with a record of what was sent. (OGC-439, OGC-1506) · _Test
  notifications & SMS gateways_
- Closer conformance to the OpenELIS FHIR implementation guide, so EMR and HIE
  partners can rely on the data. (OGC-1393, OGC-1396, OGC-1398, OGC-1399,
  OGC-1400, OGC-1401) · _FHIR R4 API_
- Known library vulnerabilities are patched in this release. **[security]**
  (OGC-1377, OGC-1457) · _Strengthened data security_
- 3.3.0 has a tested upgrade path from the 3.2.3.0 production release.
  (OGC-1508, OGC-1509, OGC-1510, OGC-1511, OGC-1512, OGC-1513, OGC-1516,
  OGC-1517)

### For implementers & admins

- Order Entry v4 groups handling options in one Handling group, records when a
  test was done elsewhere, and drops low-value fields. A fully referred order
  completes on its own. (OGC-1266, OGC-1419, OGC-1422, OGC-1423, OGC-1424,
  OGC-1443) · _Order & sample entry_
- Modify Order, the Test Catalog editor and Patient Management refuse a save
  from a stale page instead of overwriting another user's change. (OGC-1366,
  OGC-1375, OGC-1376, OGC-1378, OGC-1387, OGC-1388, OGC-1389, OGC-1406,
  OGC-1407, OGC-1449, OGC-1268, OGC-1269) · _Order & sample entry_
- The separate validation search pages are retired in favour of the one-page
  search. (OGC-1418) · _Result validation_
- EQA screens use the term scheme and only offer the actions a cycle's state
  allows. The EQA System Settings tab that rewrote lab-wide alert settings is
  retired. (OGC-608, OGC-1243, OGC-1244, OGC-1245, OGC-1246, OGC-1247, OGC-1249,
  OGC-1250, OGC-1251, OGC-1252, OGC-1255, OGC-1256, OGC-1257, OGC-1260) · _EQA /
  proficiency testing_
- Item types become free-form tags with a tag directory. The item catalog can be
  imported from CSV, low-stock alerts appear in the Alerts module, lead time is
  learned from order-to-receipt history, and six reports become four period
  reports. The old dashboard and Catalog tab are retired. (OGC-438) · _Inventory
  & supplies management_
- Each analyzer has its own mapping, with each reported record on its own row. A
  newer profile revision can be adopted after reviewing what it does to each
  record, and problems are fixed inside setup and verification. Defaults resolve
  by exact standard-code match, and only observations with invalid mappings are
  held. (OGC-1054, OGC-1409) · _Analyzer integration framework_
- OpenELIS pairs with the analyzer Bridge by certificate, analyzer setup is
  limited to Global Admin, and the legacy HL7 order servlets are closed.
  **[security]** · _Analyzer integration framework_
- The legacy analyzer plugin API and its build are removed. On upgrade,
  pre-Bridge analyzer settings are exported and their storage removed; sites
  still on legacy plugins must move those analyzers to Bridge profiles.
  **[breaking]** (OGC-1412) · _Analyzer integration framework_
- Select-list answers can carry codes in any terminology system, an answer-codes
  API is available, and LOINC answer codes can be synced from every screen that
  edits them. Config import places an answer on the result component a row
  names. (OGC-1054) · _Standardized terminology_
- One admin menu manages facilities, wards, sampling sites and geographic areas,
  with codes, search, and CSV import with Replace. Organization saves report
  failures, address fields fit their columns, and organizations have a contact
  email. (OGC-1363, OGC-1420, OGC-1263, OGC-1264, OGC-1265) · _Organizations
  management_
- Lab Units can be disabled and re-enabled, and a disabled Lab Unit stays
  disabled. (OGC-189, OGC-1459) · _Unified test editor_
- Test Catalog saves report what really happened, input the catalog cannot store
  is refused with a message, test codes are trimmed and unique, an active test
  always has a sample type, and a save from an old page no longer undoes another
  admin's change. The tests loader reads Excel files and its preview changes
  nothing. (OGC-1455, OGC-1456, OGC-1458, OGC-1460, OGC-1461, OGC-1462,
  OGC-1463, OGC-1464, OGC-1465, OGC-1466, OGC-1467) · _Unified test editor_
- Each label preset has its own content-fields editor with seeded defaults, and
  the site-wide pre-printed accession setting is shown on the Label Presets
  page. Preset edits save reliably, and upgraded sites get repaired system
  presets. (OGC-1217, OGC-1218, OGC-1219, OGC-1227, OGC-1169) · _Barcode label
  management_
- All PDF output (patient, study, statistics, non-conformity, referral and
  activity reports, the freezer temperature report and printed workplans) is
  generated with OpenPDF. Unreferenced Jasper templates and unreachable report
  code are removed.
- Result Delivery adds delivery channels, an outbox and a Sent Messages view,
  starting with email. (OGC-439, OGC-1506) · _Test notifications & SMS gateways_
- The Microbiology V1 front end is retired in favour of the V2 case model.
  (OGC-1383) · _Microbiology case workbench_
- Every order Task carries a status, EMR imports name the EMR request in
  ServiceRequest.basedOn, patientless orders have a subject,
  Observation.performer names the validator, specimen units are UCUM only when
  valid, identifiers always have values and use the configured base. Updates
  through the facade save only what changed; unsupported search parameters are
  rejected, and patient search accepts address and telecom. (OGC-1393, OGC-1396,
  OGC-1398, OGC-1399, OGC-1400, OGC-1401) · _FHIR R4 API_
- CSV exports release their database connection on every path. (OGC-1360) ·
  _Custom data export_
- The legacy Test Management pages are retired; use the Test Catalog editor.
  Logout keeps the port and sort tooltips name their column. **[breaking]**
  (OGC-1151, OGC-1448) · _Modernized administration_
- Spring Security 6.5.11, Spring Data 3.5.13 and patched Spring, Jackson, FHIR
  core, Bouncy Castle, beanutils, log4j, jsoup and qs. The session id is no
  longer returned by /session, only admins can change translations, and security
  scans cover the shipped WAR. **[security]** (OGC-1377, OGC-1457) ·
  _Strengthened data security_
- Upgrade and catalog-loader defects found by the 3.2.3.0 to 3.3.0
  release-readiness run are fixed. (OGC-1508, OGC-1509, OGC-1510, OGC-1511,
  OGC-1512, OGC-1513, OGC-1516, OGC-1517)

### For lab users

- Clinical order entry is now three steps that each save as a whole, with a
  progress indicator and a clear Cancel. Before a new patient is created you see
  possible matches. On Prepare Samples you can print barcode labels and refer
  one tube or every tube out with a reason; the collector is optional.
  (OGC-1266, OGC-1419, OGC-1422, OGC-1423, OGC-1424, OGC-1443) · _Order & sample
  entry_
- When you add a test to an existing sample on Modify Order, you can search for
  it and pick panels, the same way as on a new order. (OGC-1514) · _Order &
  sample entry_
- Every sample you add is saved, whatever its position; a failed sample-step
  request keeps the order; one patient stays on the order across saves; dates
  you pick are saved on that day in every time zone; a future date of birth is
  refused; priority can go back to Routine; and a resample names its counterpart
  by lab number. (OGC-1366, OGC-1375, OGC-1376, OGC-1378, OGC-1387, OGC-1388,
  OGC-1389, OGC-1406, OGC-1407, OGC-1449, OGC-1268, OGC-1269) · _Order & sample
  entry_
- Environmental orders can be opened and changed in Modify Order and show on the
  compliance dashboard. (OGC-1192) · _Environmental order entry_
- Reception can leave a patient's sex or age blank. When a reference range
  depends on a value that is missing, the result says so instead of silently
  picking a range. Required patient fields are announced to screen readers and
  named before save. (OGC-1362, OGC-1483, OGC-1488, OGC-1497, OGC-1240) ·
  _Patient & order enhancements_
- In Results Entry, a critical value must be acknowledged and an invalid result
  confirmed before the result is saved. (OGC-1417) · _Results entry_
- A save that gets no answer from the server is no longer shown as saved, and a
  coded answer marked Normal is no longer flagged abnormal. (OGC-1408, OGC-897)
  · _Results entry_
- Validation has a single search like Results: filter by Lab Unit, lab number or
  date range on one page. Lab numbers that contain dashes are found. (OGC-1418)
  · _Result validation_
- A result held by an open QC failure cannot be released, and Validation says
  why. Results a validator released are no longer listed as auto-validated, and
  attachments stay read-only in the review panel. (OGC-1267, OGC-1361) · _Result
  validation_
- EQA now runs as cycles. Providers prepare and dispatch panels from a workbench
  and score returns; participants receive samples, enter results without
  patients, and see scores once scored. In-house blinded samples stay blinded on
  the Results page, and each result is credited to the analyst who ran it.
  (OGC-608, OGC-1243, OGC-1244, OGC-1245, OGC-1246, OGC-1247, OGC-1249,
  OGC-1250, OGC-1251, OGC-1252, OGC-1255, OGC-1256, OGC-1257, OGC-1260) · _EQA /
  proficiency testing_
- Inventory opens on an items board that shows when each item will run out and
  what to reorder. Receive a whole delivery by scanning, receive from a row,
  count stock in count mode, filter by tags, and mark items as ordered.
  (OGC-438) · _Inventory & supplies management_
- Storage refuses recording more sample than remains, asks before marking a
  sample used up, and names the missing field when a disposal cannot be saved.
  (OGC-1374) · _Sample storage management_
- Analyzer results land on the tube and analysis they name, keep their decimal
  places on review and save, and results the Bridge set aside are visible on the
  Analyzer Types page and in setup. (OGC-1054, OGC-1409) · _Analyzer integration
  framework_
- Print Barcode shows one table of labels by name, with a stepper up to each
  label's maximum and one print control per row. Printed labels show their named
  fields with Lab Number first. (OGC-1217, OGC-1218, OGC-1219, OGC-1227,
  OGC-1169) · _Barcode label management_
- Validated patient reports queue up for printing in one place, so staff print
  what is ready instead of searching for each report. (OGC-431) · _Patient
  report print queue_
- The patient report has a redesigned, easier-to-read layout built on existing
  data. (OGC-932) · _Final PDF patient report layout_
- The routine patient results report prints on the site's configured paper size.
- Overdue STAT alerts are raised, alerts show their real dates, and
  acknowledging an alert is separate from resolving it. (OGC-1372) · _Test
  notifications & SMS gateways_
- Pathology cases open on the shared case-view layout, blocks and slides are
  tracked as identified rows, and the landing page lists cases awaiting a
  pathologist. (OGC-264) · _Immunohistochemistry (IHC) case view_
- Microbiology cases are created and routed from the bench, each with a clear
  owner. (OGC-1383) · _Microbiology case workbench_
- The Routine CSV export puts each result under its own test column. (OGC-1360)
  · _Custom data export_
- The Main menu shows only what your role can use. The legacy Results Entry page
  is gone; use the Results worklist. **[breaking]** (OGC-1151, OGC-1448) ·
  _Modernized administration_
- If the server is briefly unreachable, OpenELIS keeps trying and recovers
  instead of stopping on a System Error page. (OGC-1442) · _Modernized user
  interface_

## v3.2 (3.2.1.x and 3.2.2.0 beta stream; 3.2.3.0 production)

## 3.2.3.0

### For decision-makers

- Faster, safer result release: validators spend their time on the results that
  need judgment, and every rejection is captured as a quality event. (OGC-817,
  OGC-1027, OGC-1028, OGC-1029, OGC-1030, OGC-1226) · _Result validation_
- The modern results screen is now the standard experience for every lab on
  upgrade. (OGC-811) · _Results entry_
- Connecting a new instrument becomes a configuration task rather than a
  development project, which lowers the cost of every analyzer rollout.
  (OGC-1054) · _Analyzer integration framework_
- Brings bacteriology into OpenELIS as a first-class workflow. (OGC-782) ·
  _Microbiology case workbench_
- Antimicrobial-resistance data can now flow to national and global surveillance
  through WHONET. (OGC-782) · _AMR module & WHONET_
- Accreditation readiness in one place: the quality indicators, CAPA and
  accreditation evidence ISO 15189 assessors ask for, without spreadsheets.
  (OGC-682, OGC-688, OGC-694, OGC-695, OGC-696, OGC-698, OGC-709, OGC-710,
  OGC-712, OGC-714, OGC-686) · _Quality Assurance workspace & quality
  indicators_
- QC failures can no longer go unrecorded — each rejection becomes a tracked
  quality event. (OGC-700, OGC-701, OGC-704, OGC-705, OGC-706, OGC-728,
  OGC-1224) · _Westgard QC rules & dashboard_
- One CAPA register to show assessors, instead of hunting through individual
  events. (OGC-707) · _Non-conforming events (NCE) & CAPA_
- Signature evidence for regulated sign-off is available on demand. (OGC-702,
  OGC-703) · _Electronic signatures_
- Quality control now covers manual and rapid tests, not just connected
  analyzers. (OGC-1147) · _Analyzer manual QC recording_
- Self-service data export without developer help. (OGC-479) · _Custom data
  export_
- Programme-specific order questions can be managed by lab administrators
  without code changes. (OGC-781) · _Programs & order questionnaires_
- A new site's test catalog can be loaded from a spreadsheet in one step,
  shortening go-live. (OGC-1193, OGC-1194, OGC-1228) · _Unified test editor_
- Fewer duplicate patient records created because a search missed an existing
  patient. · _Patient & order enhancements_
- Partner systems get consistent, current answers from OpenELIS's FHIR API
  without depending on a separate FHIR store. · _FHIR R4 API_
- One storage system for samples and supplies, and stock reports ready to share.
  · _Inventory & supplies management_
- A silent sensor no longer looks like a healthy fridge. · _Sample storage
  management_
- 3.2.3.0 is the first production release of OpenELIS v3.2, with a supported
  upgrade path from the 3.2.2.0 beta.

### For implementers & admins

- Server-side Clear lane backs the bulk "Release all clear" action, with
  fail-safe lanes and a stale-page guard so a validator never releases a result
  that changed underneath them. Ordinary (non-flagged) results can clear for
  bulk release, and the queue explains why a result cannot. (OGC-817, OGC-1027,
  OGC-1028, OGC-1029, OGC-1030, OGC-1226) · _Result validation_
- Unified worklist flag defaults on; server-side paging driven by the Carbon
  table; sample kind shown per row; electronic orders scoped to the user's lab
  units. (OGC-811) · _Results entry_
- Set up an analyzer in a guided flow: pick an Analyzer Type (backed by the
  analyzer bridge), map its tests and result values to your catalog, author
  reusable analyzer profiles, then activate. Result traffic is only accepted
  once mapping is safe, and connection diagnostics explain what is wrong.
  (OGC-1054) · _Analyzer integration framework_
- Microbiology order routing, reference data administration and breakpoint
  configuration; final reports lock on release. (OGC-782) · _Microbiology case
  workbench_
- Configure breakpoints and reference data; map OpenELIS microbiology data to
  WHONET and export it. (OGC-782) · _AMR module & WHONET_
- Configure indicator thresholds on the QI Configuration page; a breached
  threshold can raise a non-conformity automatically. Record accrediting bodies
  and per-test accreditation, which appears on patient reports. A seeded QA
  Officer role and qa.view.* permissions gate the menu. (OGC-682, OGC-688,
  OGC-694, OGC-695, OGC-696, OGC-698, OGC-709, OGC-710, OGC-712, OGC-714,
  OGC-686) · *Quality Assurance workspace & quality indicators\*
- Export QC evidence for inspectors as PDF or CSV, including Levey-Jennings
  charts and sigma. The QC page title is now a proper heading for screen
  readers. (OGC-700, OGC-701, OGC-704, OGC-705, OGC-706, OGC-728, OGC-1224) ·
  _Westgard QC rules & dashboard_
- Fixes the corrective-action workflow on non-conformities. (OGC-707) ·
  _Non-conforming events (NCE) & CAPA_
- Review every electronic signature in one log under Quality Assurance and
  export it as CSV or PDF. (OGC-702, OGC-703) · _Electronic signatures_
- Bench QC tab on the QC dashboard. (OGC-1147) · _Analyzer manual QC recording_
- Foundation only: reagent QC gating, inline QC entry and analyzer polling
  follow in later milestones. (OGC-427) · _Batch workplan + reagent QC_
- Reporting sources are configured per instance; saved definitions are
  version-aware; queued deliveries support retry, cancellation and retention;
  every export is audited. (OGC-479) · _Custom data export_
- Programs now sit under Test Management with a Domain
  (Clinical/Environmental/Vector), deactivate/reactivate, lab units, and a
  visual questionnaire builder with a JSON view. Programme questionnaires are
  stored in OpenELIS (FHIR store optional). Upgrade note: every existing program
  is set to Clinical, so the Environmental and Vector program pickers stay empty
  until an administrator reclassifies programs. (OGC-781) · _Programs & order
  questionnaires_
- Manage Lab Units from one list and editor, alongside Tests, Panels and Sample
  Types in Test Catalog Management. (OGC-189) · _Unified test editor_
- Carry the whole test catalog in CSV and import it from Admin, with one test
  per row carrying all its specimens. Names the loader cannot resolve are queued
  for review, and a refused import says why instead of showing an empty result.
  (OGC-1193, OGC-1194, OGC-1228) · _Unified test editor_
- Closes the open Test Catalog v2.5 items, and the catalog list waits for two
  characters before searching. (OGC-949, OGC-1118, OGC-1119, OGC-1120, OGC-1121,
  OGC-1148, OGC-1134) · _Unified test editor_
- Editor section saves keep their data, take effect, and report what actually
  happened; a duplicate test description returns a clear conflict message;
  sample types are labelled by name; saving a result select list shows the right
  notification. (OGC-1180, OGC-1234, OGC-1238) · _Unified test editor_
- Each panel is filed under its own domain, with a warning when its specimens
  fall outside that domain; Environmental and Vector panels can now be created
  and edited. (OGC-1209, OGC-1232) · _Panel management_
- Maintain the list of provider titles on a new admin page. (OGC-1223) ·
  _Modernized administration_
- Navigation configuration consolidated to support sections, icons and report
  routes. · _Modernized administration_
- Modify User shows the correct lab unit and refuses "All Lab Units" alongside
  lab-unit-scoped grants. (OGC-1231) · _Modernized administration_
- Practitioner, DiagnosticReport, Device and storage Location resources are
  served from the OpenELIS database; every declared search parameter works and
  pages correctly; Observations carry their performer; selected samples can be
  replayed through the native transforms. · _FHIR R4 API_
- Item codes are generated from the item name and backfilled on upgrade; lot
  numbers are generated when left blank; stock visibility and barcode fixes. ·
  _Inventory & supplies management_
- Cold storage monitoring alerts when a sensor stops reporting, and monitoring
  is gated by role. · _Sample storage management_
- Bench stages replace the legacy pathology status values. (OGC-1195, OGC-264) ·
  _Immunohistochemistry (IHC) case view_
- Translation bundles load dynamically and the language selector matches your
  configured locale codes. · _Deployment-configurable translations_
- Database and FHIR runtime images are rebuilt on a supported base image.
  **[security]** · _Strengthened data security_
- 3.2.2.0 databases upgrade cleanly: fixes for the storage migration, QC lots,
  date of birth, ranges, alerts and calculations; analyzer upgrades no longer
  run premature cleanup and existing analyzer connections are migrated.
- Fixes mapping state, activation races, undelivered results and replayed
  deliveries; startup no longer fails when the DBS sample type is missing.
  (OGC-1054) · _Analyzer integration framework_

### For lab users

- Validation now opens on a triage queue that flags what to check before
  release, with filters and separate lanes for results that need attention. Open
  a result to see its history and notes, act on each row, release everything
  that is clear in one guarded step, and reject a result straight into a
  non-conformity or send it back for retest. (OGC-817, OGC-1027, OGC-1028,
  OGC-1029, OGC-1030, OGC-1226) · _Result validation_
- The unified Results worklist is now the default. Search results by patient,
  see out-of-range flags as you type, and enter exponents such as 10⁶ directly
  in numeric results. (OGC-811) · _Results entry_
- Work microbiology cases in a dedicated workbench: record isolates and manual
  antibiotic susceptibility results, follow worklists, communicate critical
  findings, review no-growth cultures, and release preliminary and final
  reports. (OGC-782) · _Microbiology case workbench_
- Amend and re-identify isolates, record repeat AST details and the reagent or
  card lot used. (OGC-782) · _AMR module & WHONET_
- A new Quality Assurance menu brings QC, EQA, non-conformities and the audit
  trail together, opening on a QA Overview of what needs attention this week.
  Quality Indicators show turnaround time, amendment rate, rejection rate and
  critical-callback compliance, and critical results now prompt for a callback
  record. (OGC-682, OGC-688, OGC-694, OGC-695, OGC-696, OGC-698, OGC-709,
  OGC-710, OGC-712, OGC-714, OGC-686) · _Quality Assurance workspace & quality
  indicators_
- Control charts show sigma metrics, the QC dashboard shows a banner for active
  rule violations, and a Westgard rejection opens a non-conformity for you.
  (OGC-700, OGC-701, OGC-704, OGC-705, OGC-706, OGC-728, OGC-1224) · _Westgard
  QC rules & dashboard_
- See every corrective and preventive action across all non-conformities in one
  register; the NCE dashboard now reflects CAPAs. (OGC-707) · _Non-conforming
  events (NCE) & CAPA_
- Record manual quantitative and rapid diagnostic test controls at the bench. A
  failed control holds the affected results from validation. (OGC-1147) ·
  _Analyzer manual QC recording_
- Group pending tests into batches on the Workplan page and move each batch
  through draft, active, completed and archived. (OGC-427) · _Batch workplan +
  reagent QC_
- Build a report from the fields you need, save it and share the definition with
  colleagues, and collect large exports from a queue when they are ready. Sample
  results export without losing repeats; Referrals and Non-Conformance export
  through the same engine. (OGC-479) · _Custom data export_
- Order entry offers only active programs that belong to the order's domain.
  (OGC-781) · _Programs & order questionnaires_
- Edit Order no longer reassigns a specimen's accession number, and removing a
  sample item cancels all its tests. Panels only add tests configured for the
  chosen sample type, and order entry keeps working if a program is removed.
  (OGC-1189, OGC-1191, OGC-1201, OGC-1221, OGC-1222) · _Order & sample entry_
- Environmental orders, which have no patient, now appear on the dashboard and
  no longer cause errors elsewhere. (OGC-1192) · _Environmental order entry_
- Patient search finds people even when the name is misspelled, and returns
  results faster. · _Patient & order enhancements_
- Give a provider a title chosen from a list. (OGC-1223) · _Modernized
  administration_
- The main menu is grouped into sections with icons, and menu groups stay open
  when a page redirects. · _Modernized administration_
- Each inventory item has a readable code. Store lots in the same storage
  locations samples use, find a lot by its barcode, and export inventory reports
  as CSV, PDF or Excel. · _Inventory & supplies management_
- Storage Management shows inventory lots next to samples, disposing of a lot
  frees its box position, and six storage menu items are folded into one page.
  Multi-word location names can now be searched. · _Sample storage management_
- Anatomic-pathology case screens share one layout, and cases move through named
  bench stages. (OGC-1195, OGC-264) · _Immunohistochemistry (IHC) case view_
- A refer-out from Results Entry reaches the reference lab, a shipment box keeps
  every sample staged for it, the Reference Lab Results page shows outstanding
  referrals accurately, and critical-result alerts name the patient and range.
  (OGC-1188, OGC-803, OGC-804) · _Sample shipment & referral_
- Screens update in place after you save instead of reloading the whole page. ·
  _Modernized user interface_
- Titer results have an entry control, a multi-select result is filed on the
  component it was entered on, and batch test reassignment works. (OGC-1185,
  OGC-1186, OGC-1187) · _Results entry_
- Switching validation submenus loads the right page, and the result value stays
  on one line beside its flag. · _Result validation_
- Referred tests are no longer counted as the laboratory's own work in reports,
  an incomplete report link says what is missing, and home-dashboard paging
  works at 100 rows per page. (OGC-1053)

## 3.2.2.0

### For decision-makers

- A single modern results surface across clinical, environmental, and vector
  labs. (OGC-811, OGC-1020) · _Results entry_
- Brings result review, quality events, and specimen handling into one auditable
  screen, cutting clicks and the risk of overwriting validated values. (OGC-811)
  · _Results entry_
- Turns the test catalog into a self-checking system that flags incomplete or
  misconfigured tests before they reach production. (OGC-1142) · _Unified test
  editor_
- Removes a long-standing data-model limitation: in one national deployment, a
  third of all tests already span multiple specimen types. This cuts duplicate
  catalog entries and configuration drift. (OGC-1145) · _Unified test editor_
- Extends clinical decision support to the component and specimen level,
  matching how modern assays actually report. · _Unified test editor_
- Consolidates specimen-type configuration — previously scattered across the app
  — into one interface, reducing setup errors. (OGC-296) · _Sample type
  management_
- Modernizes panel setup and enforces domain integrity, so ordering and e-order
  intake resolve panels consistently. (OGC-224) · _Panel management_
- Lets a country or site localize and adjust the interface language on their own
  timetable, without an engineering release. · _Deployment-configurable
  translations_
- Personal data is no longer written to application logs, reducing the risk of
  exposing patient information through log files. **[security]** · _Strengthened
  data security_

### For implementers & admins

- The canonical /Results route consolidates the legacy result-entry routes
  (behind a flag); polymorphic result cell (numeric/dictionary/multi-select);
  edit-state machine (saved rows read-only until Edit); Lab Unit drives
  cross-domain rendering (Clinical/Water Quality/Vector); save payload scoped to
  the edited analysis with an optimistic version check. (OGC-811, OGC-1020) ·
  _Results entry_
- Expanded panel adds the work-zone/reference-zone split, inline NCE form
  (Cancel/Reject/Retest), separate refer-out action, this-analysis history
  (revisions/status/notes/audit), critical-value acknowledgment, and aliquot
  management (LABNO.X-Y incl. vector pool composition). Matches the
  design-gallery mockup. (OGC-811) · _Results entry_
- Per-result save scoping hardened (OGC-1179 — the unified worklist no longer
  writes a result nobody edited); worklist 500s resolved (OGC-1170); reflex/calc
  summary and dashboard patient lookup null-safety fixed; cached Test Result
  display list refreshes after a catalog sample-results save. (OGC-1179,
  OGC-1170) · _Results entry_
- Completes the unified test editor: full per-sample label preset picker with
  Order Entry preview, Terminology display name + LOINC surfacing, all four
  shared combined-editor sections with a per-field differs view, inline
  new-select-option creation, a REST-enforced activation gate, a completeness
  checklist with a no-silent-save rule, and a catalog-health 'only tests with
  issues' view. (OGC-1142) · _Unified test editor_
- A test can now be associated with many sample types at once (multi-select with
  chips), sharing configuration by default and overriding ranges/LOINC per
  specimen where they differ — replacing the old clone-and-variant workaround.
  Specimen-aware resolution fixes the first-match bug behind duplicate test
  records. (OGC-1145) · _Unified test editor_
- Fixes the editor's Localization section (writeable, defaults to the session
  locale), duplicated/unstyled empty states, and non-numeric id 500s; scopes
  free-text result options to the Test Result category so they stop breaking
  dictionary-id consumers; restores catalog coverage + LOINC and FHIR specimen
  terminology. Regression-swept across Test Catalogue, Sample Types, and
  Results. (OGC-1153) · _Unified test editor_
- Panels and sample types can now carry localized display names and LOINC
  mappings, matching the per-test terminology model. · _Unified test editor_
- Alert, calculated, and reflex rules can now be scoped to a specific result
  component and sample type, and the rule builders decide the result type at the
  component level — so multi-component and multi-specimen tests get the right
  logic. · _Unified test editor_
- A dedicated Sample Type Management screen in the Test Catalog shell:
  create/edit/activate specimen types with domain
  (Clinical/Environmental/Vector), localized terminology, drag-and-drop display
  order, default storage & disposal, bidirectional test links, and WHONET
  specimen-code mapping for AMR exports. (OGC-296) · _Sample type management_
- Panels move into the Test Catalog Management shell (peer of Tests / Sample
  Types / Lab Units) with a list + editor (Basic Info / Tests / Terminology).
  Each panel carries a single required domain with a domain-guarded membership
  picker; sample types are derived read-only from member tests; activation
  requires at least one test; full multi-source terminology mapping with LOINC
  kept for FHIR-intake routing. (OGC-224) · _Panel management_
- A deployment can supply or override its own UI translation strings without
  rebuilding the frontend. · _Deployment-configurable translations_
- Background and system-initiated actions now run as a dedicated System/Daemon
  user, and user identity is resolved through one centralised UserContext — so
  automated writes are attributable in the audit trail. · _Strengthened data
  security_
- Validation page: the row link now reads 'Modify Order' (was 'View Patient')
  and the result-table column header reads 'Validate' (was 'Save'). Sample Entry
  no longer crashes when the departments list fails to load. · _Result
  validation_
- Cold Storage timestamps now handle seconds vs. milliseconds correctly, so
  stored times display and compare accurately. · _Sample storage management_
- The Cytopathologist role name is now consistent across the Cytology module. ·
  _Cytology case view_
- Fixes report and analyzer breadcrumbs, a charted-report 500
  (jfreechart/JasperReports version mismatch), Report Non-Conformity gating, and
  Patient History for multi-component tests.

### For lab users

- One /Results worklist replaces the old separate result screens — search,
  filter by Lab Unit, date, and status, and enter results inline. Each result
  saves on its own, so two techs working the same worklist no longer overwrite
  each other. (OGC-811, OGC-1020) · _Results entry_
- Open a result to a focused work zone: enter the value with method and
  analyzer, see this analysis's own history, flags and critical-value handling,
  record non-conforming events, refer a test out, or aliquot the sample — all
  inline, without leaving the worklist. (OGC-811) · _Results entry_
- Saving in the worklist no longer changes a result you didn't touch, and the
  pages that were throwing errors now load. (OGC-1179, OGC-1170) · _Results
  entry_
- The main menu now adapts to your screen: pinned and always visible on a
  desktop, or an on-demand drawer on smaller screens. A pin control lets you
  switch between the two. · _Modernized user interface_
- Requester details on Add Order are no longer required by default, so you can
  register an order without them when they aren't available. · _Order & sample
  entry_
- When your session expires you now see a spinner instead of a 'Not
  Authenticated' flash, and changing your password reports real success or
  failure with validation errors. · _Modernized user interface_

## 3.2.1.11

### For decision-makers

- Consolidates test configuration into one maintainable editor, reducing admin
  training time and configuration errors. (OGC-949) · _Unified test editor_
- Rule-driven alerts and reflex logic are now authored directly in the test
  catalog, standardizing clinical decision support across the lab. (OGC-949) ·
  _Unified test editor_
- Foundation for molecular and panel-style testing where one test yields several
  reportable components. (OGC-1131) · _Unified test editor_
- Labs can adapt barcode printing to local hardware without code changes.
  (OGC-285) · _Barcode label management_
- Improves screen-reader accessibility and non-English usability. · _Order &
  sample entry_

### For implementers & admins

- Unified test editor replaces the fragmented legacy screens: editor shell, list
  view, Basic Info, and all v1 sections (M0–M12), with sections always visible
  in the catalog side nav (disabled until a test is open). (OGC-949) · _Unified
  test editor_
- Configure barcode labels, result alerts, reflex/calculated tests, and storage
  per test; alerts now dispatch asynchronously and support multi-terminology
  FHIR codings. (OGC-949) · _Unified test editor_
- Completion and correction handling added to the test catalog editor.
  (OGC-1112) · _Unified test editor_
- Associate tests with treatment regimens directly in the catalog. · _Unified
  test editor_
- Define per-test result components with an is_primary flag, show-on-report
  control, multi-component terminology, and analyzer ingestion; molecular seed
  data included. (OGC-1131 epic still in progress — initial delivery.)
  (OGC-1131) · _Unified test editor_
- Sample & Results and reference-range editing reconciled to the FRS, including
  range nesting. (OGC-749, OGC-1117) · _Reference range editor_
- Newly created tests are now immediately orderable, and lab unit / sample type
  can be edited when modifying an existing test. (OGC-1116) · _Unified test
  editor_
- Create and manage configurable barcode label presets (sizes/layouts) instead
  of the four fixed system presets. (OGC-285) · _Barcode label management_
- Validation now reuses the existing localization system so labels translate
  consistently. (OGC-767) · _Result validation_

### For lab users

- A single modern Test Catalog editor — open a test and move through every
  configuration section in one place. (OGC-949) · _Unified test editor_
- Tests can now report multiple result components (e.g. molecular panels) in a
  single entry. (OGC-1131) · _Unified test editor_
- Print from label presets that match your printer and label stock. (OGC-285) ·
  _Barcode label management_
- Jump straight to a patient's record from the validation screen. (OGC-767) ·
  _Result validation_
- The validation action button now reads "Validate," matching what it does. ·
  _Result validation_
- Date pickers now display in your locale and the Quick Address Search field is
  properly labeled. · _Order & sample entry_
- Enter a multi-line provisional clinical diagnosis when adding an order. ·
  _Order & sample entry_
- Home-dashboard turnaround times now show as whole hours. · _Turnaround time
  report & dashboard_

## 3.2.1.10

### For decision-makers

- Full two-way instrument integration reduces manual order entry at the bench.
  (OGC-773) · _Outbound order dispatch_

### For implementers & admins

- New live log screen and logging configuration, including under SSO login. ·
  _System monitoring & logs_
- Bidirectional ASTM/HL7 order dispatch via the analyzer bridge. (OGC-773) ·
  _Outbound order dispatch_

### For lab users

- Orders can now be sent to instruments automatically, not just received.
  (OGC-773) · _Outbound order dispatch_

## 3.2.1.9

### For decision-makers

- Operational visibility into data exchange reliability. · _Automatic result
  sharing (FHIR)_

### For implementers & admins

- See and act on FHIR data-export retry health from admin. · _Automatic result
  sharing (FHIR)_
- Filter and manage test notifications more easily. · _Test notifications & SMS
  gateways_

## 3.2.1.8

### For lab users

- Patient identifiers now show in the modify-order header. · _Patient & order
  enhancements_
- Entered date/time now persists on result entry. · _Results entry_

## 3.2.1.7

### For decision-makers

- Supports consent-tracking compliance for patient testing. (OGC-557, OGC-558) ·
  _Informed consent capture_
- Stronger quality control supports accreditation readiness. (OGC-41) ·
  _Westgard QC rules & dashboard_
- Modernized, faster UI foundation. · _Modernized user interface_

### For implementers & admins

- Manual consent recording fields configurable on patient orders. (OGC-557,
  OGC-558) · _Informed consent capture_
- FHIR-based QC pipeline with automated rule evaluation. (OGC-41) · _Westgard QC
  rules & dashboard_
- Create file-based analyzers from profiles with two-way bridge sync. ·
  _Analyzer file import_
- Breaking change to query translation — review analyzer query configuration on
  upgrade. **[breaking]** (OGC-346) · _Analyzer integration framework_

### For lab users

- Record patient informed consent against an order. (OGC-557, OGC-558) ·
  _Informed consent capture_
- QC results are evaluated automatically against Westgard rules. (OGC-41) ·
  _Westgard QC rules & dashboard_
- Cleaner, faster storage screens. · _Sample storage management_

## 3.2.1.6

### For lab users

- Attach patient ID documents to records. · _Patient & order enhancements_

## 3.2.1.5

### For decision-makers

- Chain-of-custody for sample referral networks. (OGC-62) · _Sample shipment &
  referral_
- Audit trail supports accreditation and data integrity. · _System-level audit
  trail_
- 21 CFR Part 11-aligned electronic signatures. · _Electronic signatures_
- Structured CAPA supports quality management. · _Non-conforming events (NCE) &
  CAPA_
- Security hardening across the REST API. **[security]** (OGC-150) ·
  _Strengthened data security_
- TAT monitoring surfaces lab performance. (OGC-306, OGC-307) · _Turnaround time
  report & dashboard_
- Expanded FHIR R4 surface for interoperability. · _FHIR R4 API_
- Supports multilingual deployments. (OGC-349) · _Standardized terminology_
- Broader instrument coverage. (OGC-344, OGC-417, OGC-418) · _Analyzer file
  import_

### For implementers & admins

- System-wide audit trail of configuration and data changes. · _System-level
  audit trail_
- Project-wide CSRF and REST @PreAuthorize hardening — re-test custom
  integrations. **[security]** (OGC-150) · _Strengthened data security_
- Metadata can be translated into any language. (OGC-349) · _Standardized
  terminology_
- Added flat-file import for additional instruments. (OGC-344, OGC-417, OGC-418)
  · _Analyzer file import_

### For lab users

- Create electronic shipment manifests for referred samples. (OGC-62) · _Sample
  shipment & referral_
- Sign results and reports electronically. · _Electronic signatures_
- Track non-conforming events with history and assignment. · _Non-conforming
  events (NCE) & CAPA_
- Monitor turnaround times; manage the lab calendar. (OGC-306, OGC-307) ·
  _Turnaround time report & dashboard_
- Order entry and sample collection are now separate steps. (OGC-356) · _Order &
  sample entry_

## 3.2.1.4

### For decision-makers

- Connect instruments without custom code. (OGC-325, OGC-492) · _Analyzer
  integration framework_
- Validated TB/HIV/COVID instrument support. (OGC-335) · _Validated instrument
  library_
- Patient/provider SMS notifications in more regions. · _Test notifications &
  SMS gateways_
- Brings EQA into the LIS for accreditation. · _EQA / proficiency testing_
- Foundation for flexible, self-service reporting. · _Custom data export_

### For implementers & admins

- Generic, bridge-mandatory analyzer framework with per-analyzer mappings.
  (OGC-325, OGC-492) · _Analyzer integration framework_
- Cepheid GeneXpert ASTM adapter. (OGC-335) · _Validated instrument library_
- Configure Twilio or Africa's Talking for SMS. · _Test notifications & SMS
  gateways_

### For lab users

- Control how many barcode labels print. (OGC-284) · _Barcode label management_
- Begin managing proficiency-testing events in OpenELIS. · _EQA / proficiency
  testing_
