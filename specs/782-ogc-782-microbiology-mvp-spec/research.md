# Research: Microbiology MVP Workflow

## Evidence Sources

- Feature spec: `specs/782-ogc-782-microbiology-mvp-spec/spec.md`
- Engineering crosswalk:
  `specs/roadmaps/analyzer-microbiology-engineering-crosswalk.md`
- Confluence workflow narrative:
  `https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256`
- Confluence dependency map:
  `https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1370554369`
- Public M-* design bundle:
  `https://github.com/DIGI-UW/openelis-work/tree/main/designs/microbiology`
- Repo verification:
  - `src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`
  - `src/main/java/org/openelisglobal/test/valueholder/Test.java`
  - `src/main/resources/liquibase/3.5.x.x/040-test-domain-amr-whonet.xml`
  - `src/main/java/org/openelisglobal/testresultcomponent/valueholder/TestResultComponent.java`
  - `src/main/java/org/openelisglobal/reports/service/WHONetReportServiceImpl.java`
  - `src/main/java/org/openelisglobal/alert/valueholder/AlertType.java`
  - `src/main/java/org/openelisglobal/alert/controller/rest/AlertRestController.java`

## Decisions

### Decision: Use `SampleItem + workflow_type` as the microbiology case identity

**Rationale**: The Confluence narrative explicitly states that a specimen can
have separate bacteriology and TB workflows without duplicate accessioning. Repo
state confirms `SampleItem` is the existing specimen-level valueholder with
sample, type/source, collection, rejection, and FHIR UUID fields. Anchoring
cases at `SampleItem` avoids a false 1:1 `Sample` assumption and supports
sibling workflows.

**Alternatives considered**:

- `Sample` only: rejected because one accession can include multiple physical
  specimen items and workflow-specific workups.
- `Analysis` only: rejected because microbiology case work spans multi-day
  activities, isolates, and AST work that are not one analysis row.
- New specimen entity: rejected for MVP because it duplicates `SampleItem`.

### Decision: Persist test routing separately from case workflow state

**Rationale**: Product language says ordered tests start the workflow. Repo state
already has Test Catalog AMR groundwork (`antimicrobialResistance`,
`test_amr_config`, and WHONET code tables). Planning should add a culture
workflow attribute to culture-capable tests and copy the resolved workflow onto
the case at creation time. This keeps test configuration as the routing source
while preserving case history even if catalog config changes later.

**Alternatives considered**:

- Clerk-selected program routing: rejected because it depends on user memory.
- Derive from specimen type: rejected because the same specimen type can need
  routine bacteriology and TB.
- Store only on case: rejected because order creation needs a catalog-level
  routing source.

### Decision: Reuse Method/TestMethod for the default culture setup identity

**Rationale**: Repo state includes `method`, `test_method`, Method services, and
Test Catalog methods UI. Using the existing Method association for the default
culture setup keeps culture setup aligned with current test configuration. Add
microbiology-specific setup metadata only where Method is too generic, such as
media, incubation, atmosphere, and bottle defaults.

**Alternatives considered**:

- Fully separate culture protocol master unrelated to Method: rejected for MVP
  because it duplicates existing catalog behavior.
- Encode media defaults directly on Test: rejected because methods/protocols
  need reuse across tests and deployments.

### Decision: Use microbiology AST workflow tables as system of record, then
bridge finalized/reportable results to existing result/reporting paths

**Rationale**: Existing `test_result_component.allow_multiple_readings` helps,
but AST needs run-level state, organism/isolate context, repeats, QC/review,
override audit, and no-breakpoint handling. Those are workflow concepts, not
simple result components. The plan therefore uses microbiology AST run/reading
records for workflow truth and validates in M5 how finalized/reportable results
project into existing result/reporting infrastructure.

**Alternatives considered**:

- Store all AST directly in existing `result`: rejected because it risks forcing
  a multi-isolate, multi-run workflow into a chemistry-shaped table.
- Store only in new AST tables and bypass existing reporting/result validation:
  rejected because reports, WHONET, audit, and validation should reuse existing
  OpenELIS paths where feasible.

### Decision: Add a clinical critical communication log and surface operational
alerts through the existing Alert infrastructure

**Rationale**: Repo state includes a generic `alert` table, `AlertType`, REST
controller, and Alerts dashboard. The product requirement is not a second alert
dashboard; it is a clinically auditable call/read-back communication. The plan
therefore creates a clinical log for recipient/message/time/method/ack/follow-up
and links/surfaces it through generic Alerts for operational visibility.

**Alternatives considered**:

- Alert row only: rejected because generic alerts do not carry the full clinical
  communication record.
- Separate microbiology alert dashboard: rejected because the requirement says
  reuse existing operational alert workflow.
- Reuse `OTHER` alert type silently: rejected because microbiology critical
  alerts should be filterable and validated explicitly.

### Decision: Extend existing WHONET services for readiness/export

**Rationale**: Repo state includes `WHONetReportServiceImpl`, WHONET report
actions, the Reports -> WHONET route, `whonet_antibiotic_codes`, and
`test_amr_config`. The plan extends these paths for finalized microbiology data
instead of building a parallel exporter.

**Alternatives considered**:

- New exporter module: rejected because it duplicates existing report/export
  infrastructure.
- Full WHONET export in MVP-1A: deferred because the product spec only requires
  readiness and flags full export automation as a later slice.

### Decision: Use Playwright-first E2E planning for new microbiology flows

**Rationale**: The current Testing Roadmap says new E2E coverage should be
authored in Playwright unless blocked, while Cypress remains legacy/deprecating.
The plan keeps Cypress from expanding and routes E2E authoring through
`/plan-record-playwright`.

**Alternatives considered**:

- Add new Cypress E2E tests: rejected unless a specific blocker prevents
  Playwright.
- Rely only on component tests: rejected because order-to-report is a critical
  cross-layer workflow.

## Deferred Engineering Decisions

- Exact names of every table, DTO, and React component belong to milestone task
  design, not product specs.
- Analyzer-ingested AST, BacT/Alert events, VITEK/Phoenix profiles, and expert
  rules are later slices.
- TB/mycobacteriology is represented as a sibling workflow concept now, but its
  full profile is a later cycle.
- FHIR/GLASS external surveillance is deferred until a later feature; MVP keeps
  WHONET readiness/export in existing report paths.
