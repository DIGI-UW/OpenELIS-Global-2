# Changelog

Human-readable release notes for OpenELIS Global, written for three audiences:
decision-makers, implementers & admins, and lab users. This file is generated
from the OpenELIS features inventory (do not hand-edit).

See also: the live product roadmap
(https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/640319495), the per-audience
release-notes page
(https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1412136974), and earlier
releases v3.0/v2.7/v2.6
(https://uwdigi.atlassian.net/wiki/spaces/OG/pages/645005332).

## v3.2 (beta stream — 3.2.1.x, pilot / UAT)

## 3.2.1.10

### For decision-makers

- Full two-way instrument integration reduces manual order entry at the bench.
  (OGC-773) — _Outbound order dispatch_

### For implementers & admins

- New live log screen and logging configuration, including under SSO login. —
  _System monitoring & logs_
- Bidirectional ASTM/HL7 order dispatch via the analyzer bridge. (OGC-773) —
  _Outbound order dispatch_

### For lab users

- Orders can now be sent to instruments automatically, not just received.
  (OGC-773) — _Outbound order dispatch_

## 3.2.1.9

### For decision-makers

- Operational visibility into data exchange reliability. — _Automatic result
  sharing (FHIR)_

### For implementers & admins

- See and act on FHIR data-export retry health from admin. — _Automatic result
  sharing (FHIR)_
- Filter and manage test notifications more easily. — _Test notifications & SMS
  gateways_

## 3.2.1.8

### For lab users

- Patient identifiers now show in the modify-order header. — _Patient & order
  enhancements_
- Entered date/time now persists on result entry. — _Results entry_

## 3.2.1.7

### For decision-makers

- Supports consent-tracking compliance for patient testing. (OGC-557, OGC-558) —
  _Informed consent capture_
- Stronger quality control supports accreditation readiness. (OGC-41) —
  _Westgard QC rules & dashboard_
- Modernized, faster UI foundation. — _Modernized user interface_

### For implementers & admins

- Manual consent recording fields configurable on patient orders. (OGC-557,
  OGC-558) — _Informed consent capture_
- FHIR-based QC pipeline with automated rule evaluation. (OGC-41) — _Westgard QC
  rules & dashboard_
- Create file-based analyzers from profiles with two-way bridge sync. —
  _Analyzer file import_
- Breaking change to query translation — review analyzer query configuration on
  upgrade. **[breaking]** (OGC-346) — _Analyzer integration framework_

### For lab users

- Record patient informed consent against an order. (OGC-557, OGC-558) —
  _Informed consent capture_
- QC results are evaluated automatically against Westgard rules. (OGC-41) —
  _Westgard QC rules & dashboard_
- Cleaner, faster storage screens. — _Sample storage management_

## 3.2.1.6

### For lab users

- Attach patient ID documents to records. — _Patient & order enhancements_

## 3.2.1.5

### For decision-makers

- Chain-of-custody for sample referral networks. (OGC-62) — _Sample shipment &
  referral_
- Audit trail supports accreditation and data integrity. — _System-level audit
  trail_
- 21 CFR Part 11-aligned electronic signatures. — _Electronic signatures_
- Structured CAPA supports quality management. — _Non-conforming events (NCE) &
  CAPA_
- Security hardening across the REST API. **[security]** (OGC-150) —
  _Strengthened data security_
- TAT monitoring surfaces lab performance. (OGC-306, OGC-307) — _Turnaround time
  report & dashboard_
- Expanded FHIR R4 surface for interoperability. — _FHIR R4 API_
- Supports multilingual deployments. (OGC-349) — _Standardized terminology_
- Broader instrument coverage. (OGC-344, OGC-417, OGC-418) — _Analyzer file
  import_

### For implementers & admins

- System-wide audit trail of configuration and data changes. — _System-level
  audit trail_
- Project-wide CSRF and REST @PreAuthorize hardening — re-test custom
  integrations. **[security]** (OGC-150) — _Strengthened data security_
- Metadata can be translated into any language. (OGC-349) — _Standardized
  terminology_
- Added flat-file import for additional instruments. (OGC-344, OGC-417, OGC-418)
  — _Analyzer file import_

### For lab users

- Create electronic shipment manifests for referred samples. (OGC-62) — _Sample
  shipment & referral_
- Sign results and reports electronically. — _Electronic signatures_
- Track non-conforming events with history and assignment. — _Non-conforming
  events (NCE) & CAPA_
- Monitor turnaround times; manage the lab calendar. (OGC-306, OGC-307) —
  _Turnaround time report & dashboard_
- Order entry and sample collection are now separate steps. (OGC-356) — _Order &
  sample entry_

## 3.2.1.4

### For decision-makers

- Connect instruments without custom code. (OGC-325, OGC-492) — _Analyzer
  integration framework_
- Validated TB/HIV/COVID instrument support. (OGC-335) — _Validated instrument
  library_
- Patient/provider SMS notifications in more regions. — _Test notifications &
  SMS gateways_
- Brings EQA into the LIS for accreditation. — _EQA / proficiency testing_
- Foundation for flexible, self-service reporting. — _Custom data export_

### For implementers & admins

- Generic, bridge-mandatory analyzer framework with per-analyzer mappings.
  (OGC-325, OGC-492) — _Analyzer integration framework_
- Cepheid GeneXpert ASTM adapter. (OGC-335) — _Validated instrument library_
- Configure Twilio or Africa's Talking for SMS. — _Test notifications & SMS
  gateways_

### For lab users

- Control how many barcode labels print. (OGC-284) — _Barcode label management_
- Begin managing proficiency-testing events in OpenELIS. — _EQA / proficiency
  testing_
