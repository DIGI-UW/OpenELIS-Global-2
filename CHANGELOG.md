# Changelog

Human-readable release notes for OpenELIS Global, written for three audiences: decision-makers, implementers & admins, and lab users. This file is generated from the OpenELIS features inventory (do not hand-edit).

See also: the live product roadmap (https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/640319495), the per-audience release-notes page (https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1412136974), and earlier releases v3.0/v2.7/v2.6 (https://uwdigi.atlassian.net/wiki/spaces/OG/pages/645005332).

## v3.2 (beta stream — 3.2.1.x, pilot / UAT)


## 3.2.1.10
### For decision-makers
- Full two-way instrument integration reduces manual order entry at the bench. (OGC-773) — *Outbound order dispatch*

### For implementers & admins
- New live log screen and logging configuration, including under SSO login. — *System monitoring & logs*
- Bidirectional ASTM/HL7 order dispatch via the analyzer bridge. (OGC-773) — *Outbound order dispatch*

### For lab users
- Orders can now be sent to instruments automatically, not just received. (OGC-773) — *Outbound order dispatch*


## 3.2.1.9
### For decision-makers
- Operational visibility into data exchange reliability. — *Automatic result sharing (FHIR)*

### For implementers & admins
- See and act on FHIR data-export retry health from admin. — *Automatic result sharing (FHIR)*
- Filter and manage test notifications more easily. — *Test notifications & SMS gateways*


## 3.2.1.8
### For lab users
- Patient identifiers now show in the modify-order header. — *Patient & order enhancements*
- Entered date/time now persists on result entry. — *Results entry*


## 3.2.1.7
### For decision-makers
- Supports consent-tracking compliance for patient testing. (OGC-557, OGC-558) — *Informed consent capture*
- Stronger quality control supports accreditation readiness. (OGC-41) — *Westgard QC rules & dashboard*
- Modernized, faster UI foundation. — *Modernized user interface*

### For implementers & admins
- Manual consent recording fields configurable on patient orders. (OGC-557, OGC-558) — *Informed consent capture*
- FHIR-based QC pipeline with automated rule evaluation. (OGC-41) — *Westgard QC rules & dashboard*
- Create file-based analyzers from profiles with two-way bridge sync. — *Analyzer file import*
- Breaking change to query translation — review analyzer query configuration on upgrade. **[breaking]** (OGC-346) — *Analyzer integration framework*

### For lab users
- Record patient informed consent against an order. (OGC-557, OGC-558) — *Informed consent capture*
- QC results are evaluated automatically against Westgard rules. (OGC-41) — *Westgard QC rules & dashboard*
- Cleaner, faster storage screens. — *Sample storage management*


## 3.2.1.6
### For lab users
- Attach patient ID documents to records. — *Patient & order enhancements*


## 3.2.1.5
### For decision-makers
- Chain-of-custody for sample referral networks. (OGC-62) — *Sample shipment & referral*
- Audit trail supports accreditation and data integrity. — *System-level audit trail*
- 21 CFR Part 11-aligned electronic signatures. — *Electronic signatures*
- Structured CAPA supports quality management. — *Non-conforming events (NCE) & CAPA*
- Security hardening across the REST API. **[security]** (OGC-150) — *Strengthened data security*
- TAT monitoring surfaces lab performance. (OGC-306, OGC-307) — *Turnaround time report & dashboard*
- Expanded FHIR R4 surface for interoperability. — *FHIR R4 API*
- Supports multilingual deployments. (OGC-349) — *Standardized terminology*
- Broader instrument coverage. (OGC-344, OGC-417, OGC-418) — *Analyzer file import*

### For implementers & admins
- System-wide audit trail of configuration and data changes. — *System-level audit trail*
- Project-wide CSRF and REST @PreAuthorize hardening — re-test custom integrations. **[security]** (OGC-150) — *Strengthened data security*
- Metadata can be translated into any language. (OGC-349) — *Standardized terminology*
- Added flat-file import for additional instruments. (OGC-344, OGC-417, OGC-418) — *Analyzer file import*

### For lab users
- Create electronic shipment manifests for referred samples. (OGC-62) — *Sample shipment & referral*
- Sign results and reports electronically. — *Electronic signatures*
- Track non-conforming events with history and assignment. — *Non-conforming events (NCE) & CAPA*
- Monitor turnaround times; manage the lab calendar. (OGC-306, OGC-307) — *Turnaround time report & dashboard*
- Order entry and sample collection are now separate steps. (OGC-356) — *Order & sample entry*


## 3.2.1.4
### For decision-makers
- Connect instruments without custom code. (OGC-325, OGC-492) — *Analyzer integration framework*
- Validated TB/HIV/COVID instrument support. (OGC-335) — *Validated instrument library*
- Patient/provider SMS notifications in more regions. — *Test notifications & SMS gateways*
- Brings EQA into the LIS for accreditation. — *EQA / proficiency testing*
- Foundation for flexible, self-service reporting. — *Custom data export*

### For implementers & admins
- Generic, bridge-mandatory analyzer framework with per-analyzer mappings. (OGC-325, OGC-492) — *Analyzer integration framework*
- Cepheid GeneXpert ASTM adapter. (OGC-335) — *Validated instrument library*
- Configure Twilio or Africa's Talking for SMS. — *Test notifications & SMS gateways*

### For lab users
- Control how many barcode labels print. (OGC-284) — *Barcode label management*
- Begin managing proficiency-testing events in OpenELIS. — *EQA / proficiency testing*

