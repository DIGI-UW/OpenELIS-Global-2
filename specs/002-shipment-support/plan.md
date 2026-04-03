# Implementation Plan: Referred Sample Container Management System

**Branch**: `002-shipment-support` | **Date**: 2025-12-04 | **Spec**:
[spec.md](spec.md) **Input**: Feature specification from
`/specs/002-shipment-support/spec.md`

## Summary

Implement a comprehensive shipment tracking system for managing referred
laboratory samples sent to reference laboratories. The system enables shipping
coordinators to create boxes, track unassigned samples, generate manifests with
labels, manage electronic transmission via FHIR, and support receiving
technicians in reconciling received boxes with full quality incident tracking
integrated into the existing OpenELIS quality system.

**Technical Approach**: Web application with Spring Boot backend (5-layer
architecture) + React Carbon Design System frontend, PostgreSQL database with
Liquibase migrations, FHIR R4 SupplyDelivery resources for electronic manifest
exchange, full integration with existing OpenELIS quality module for
non-conformity tracking.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) + React 17 **Primary
Dependencies**: Spring Boot 3.x, Hibernate 6.x, Carbon Design System v1.15, HAPI
FHIR R4 (6.6.2), PostgreSQL 14+ **Storage**: PostgreSQL 14+ with Liquibase 4.8.0
for schema migrations **Testing**: JUnit 4 (4.13.1) + Mockito 2.21.0 (backend),
Jest + React Testing Library (frontend unit), Cypress 12.17.3 (E2E) **Target
Platform**: Linux server (Docker containers), web browsers
(Chrome/Firefox/Safari/Edge latest 2 versions) **Project Type**: Web (backend +
frontend separated) **Performance Goals**:

- 5 seconds max response time for all operations (page loads, searches, API
  calls, barcode scans) under normal load
- Support 1,000+ active boxes and 10,000+ samples per day without degradation
- E2E test suite execution <5 minutes **Constraints**:
- Must integrate with existing OpenELIS order entry, user management, facility
  registry, and quality systems
- Electronic manifest transmission must support FHIR R4, legacy API, and Email
  fallback
- 7-year minimum data retention for regulatory compliance
- Encryption at rest (database-level) and in transit (TLS 1.2+) **Scale/Scope**:
- 7 user stories (5 P1, 1 P2, 1 P3)
- 55 functional requirements
- 9 constitution compliance requirements
- 29 UI/UX requirements
- 6 core entities (Box, Shipment, Sample, UnassignedSample, Facility,
  NonConformity)
- Target: 20-30 E2E test cases focused on user stories

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: No country-specific code branches planned
  - Label formats, aging thresholds, temperature ranges, capacity templates,
    time periods (Lost in Transit default 14 days) all configurable via
    SystemConfiguration
  - Facility registry managed via existing OpenELIS configuration tables
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (NO
      Bootstrap/Tailwind)
  - UI-001 to UI-029 explicitly mandate Carbon components
  - Dashboard tabs, metric cards, tables, modals, forms all use Carbon patterns
  - No custom CSS frameworks planned
- [x] **FHIR/IHE Compliance**: External data integrates via FHIR R4 + IHE
      profiles
  - FR-040: System MUST support FHIR R4 SupplyDelivery resources for electronic
    manifest exchange
  - All entities with external exposure include fhir_uuid
  - Integration with FHIR server for manifest transmission (FR-023)
- [x] **Layered Architecture**: Backend follows 5-layer pattern
      (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files -
    legacy exempt until refactored)
    - Entities: ShippingBox, Shipment, BoxSample, UnassignedSample,
      ShipmentFacility extend BaseObject<String>
    - All include fhir_uuid UUID, use @Entity, @Table, @Column, @ManyToOne, etc.
    - ID generation via @GenericGenerator with sequence names
  - **Transaction management MUST be in service layer only** - NO @Transactional
    annotations on controller methods
    - ShippingBoxService, ShipmentService, UnassignedSampleService annotated
      with @Service + @Transactional
    - Controllers delegate to services, which manage transaction boundaries
  - **Services MUST compile all data within transaction** to prevent
    LazyInitializationException
    - Services return complete DTOs/maps with all hierarchical data resolved
    - Controllers MUST NOT traverse entity relationships
- [x] **Test Coverage**: Unit + ORM validation + integration + E2E tests planned
      (>70% coverage goal per Constitution V.4 and V.5)
  - Unit tests: Business logic in services (TDD workflow)
  - ORM validation tests: Hibernate mapping validation without database (<5s
    execution)
  - Integration tests: REST API endpoints with test database
  - E2E tests MUST follow Cypress best practices (Constitution V.5):
    - Run tests individually during development (not full suite)
    - Maximum 5-10 test cases per execution during development
    - Video recording disabled by default, screenshots enabled for failures
    - Browser console logging enabled and reviewed after each run
    - Post-run review of console logs and screenshots required
    - Target: Individual test <30 seconds, full suite <5 minutes
- [x] **Schema Management**: Database changes via Liquibase changesets only
  - All tables (shipping_box, shipment, box_sample, unassigned_sample,
    non_conformity_attachment, etc.) created via Liquibase
  - Changeset IDs: shipment-{sequence}-{description}
  - Rollback scripts provided for structural changes
- [x] **Internationalization**: All UI strings use React Intl (no hardcoded
      text)
  - All UI requirements (UI-001 to UI-029) strings externalized
  - Message keys: shipment.{module}.{key} pattern
  - Minimum: en + fr translations required
- [x] **Security & Compliance**: RBAC, audit trail, input validation included
  - FR-038: Role-based access control (Shipping role, Administrator role)
  - FR-037: Audit trail for all actions (box creation, sample
    additions/removals, state changes, sending, receiving)
  - FR-046: Patient name visibility permission-based (HIPAA compliance)
  - FR-055: 7-year minimum data retention
  - CR-007: Encryption at rest (database-level) and in transit (TLS 1.2+)
  - Input validation via Hibernate Validator + Formik on frontend

**Complexity Justification Required If**:

- Adding custom CSS framework alongside Carbon - NOT APPLICABLE
- Using native SQL instead of JPA/Hibernate - NOT APPLICABLE
- Hardcoding country-specific logic instead of configuration - NOT APPLICABLE
- Bypassing FHIR for external integration - NOT APPLICABLE
- Skipping test implementation - NOT APPLICABLE

**No constitution violations - all principles satisfied.**

## Project Structure

### Documentation (this feature)

```text
specs/002-shipment-support/
├── spec.md              # Feature specification (/speckit.specify command output)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── openapi.yaml     # REST API contract (OpenAPI 3.0)
│   └── fhir-profiles/   # FHIR SupplyDelivery profile examples
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Web application structure (backend + frontend)

backend/
└── src/
    ├── main/
    │   ├── java/org/openelisglobal/
    │   │   ├── shipment/              # NEW MODULE
    │   │   │   ├── valueholder/       # Entities (ShippingBox, Shipment, BoxSample, etc.)
    │   │   │   ├── dao/               # DAOs (ShippingBoxDAO, ShipmentDAO, etc.)
    │   │   │   ├── service/           # Services (ShippingBoxService, ShipmentService, etc.)
    │   │   │   ├── controller/        # REST Controllers (ShipmentRestController)
    │   │   │   └── form/              # DTOs/Forms (ShippingBoxForm, UnassignedSampleForm, etc.)
    │   │   ├── referral/              # Existing module - integration point for unassigned samples
    │   │   ├── nonconformity/         # Existing quality module - integration point for FR-027
    │   │   ├── facility/              # Existing facility registry - integration point for destinations
    │   │   └── fhir/                  # Existing FHIR infrastructure - SupplyDelivery transforms
    │   └── resources/
    │       ├── liquibase/
    │       │   └── shipment/          # NEW: Schema migrations (shipment-001-*, shipment-002-*, etc.)
    │       └── hibernate/             # Legacy XML mappings (NOT used for new entities)
    └── test/
        └── java/org/openelisglobal/shipment/
            ├── HibernateMappingValidationTest.java  # ORM validation tests
            ├── service/                             # Service unit tests
            ├── dao/                                 # DAO integration tests
            └── controller/                          # REST API integration tests

frontend/
└── src/
    ├── components/
    │   └── shipment/                  # NEW MODULE
    │       ├── ShipmentDashboard.jsx  # Main dashboard (tabs: Shipments, Unassigned Tests)
    │       ├── CreateBoxModal.jsx     # Box creation modal (UI-010, UI-011)
    │       ├── BoxManifestTable.jsx   # Sample manifest table (UI-005, UI-006)
    │       ├── UnassignedTestsTable.jsx # Unassigned samples table (UI-003, UI-004, UI-005)
    │       ├── ReceivingWorkflow.jsx  # Receiving workflow with progress indicator (UI-015)
    │       ├── NonConformityModal.jsx # Non-conformity recording (FR-027)
    │       └── __tests__/             # Jest unit tests
    ├── languages/
    │   ├── en.json                    # NEW: Shipment i18n keys
    │   └── fr.json                    # NEW: French translations
    └── cypress/
        └── e2e/
            ├── shipmentDashboard.cy.js      # User Story 6 (Dashboard)
            ├── boxCreation.cy.js            # User Story 1 (Create boxes)
            ├── unassignedTracking.cy.js     # User Story 2 (Unassigned tests)
            ├── manifestGeneration.cy.js     # User Story 3 (Labels/manifests)
            ├── boxSending.cy.js             # User Story 4 (Send boxes)
            └── boxReceiving.cy.js           # User Story 5 (Receive/reconcile)
```

**Structure Decision**: Web application structure selected based on existing
OpenELIS architecture. Backend follows modular package structure
(`org.openelisglobal.{module}`), frontend follows component-based structure
(`src/components/{feature}`). New shipment module integrates with existing
modules (referral, nonconformity, facility, fhir) via service-layer
dependencies.

## Complexity Tracking

> **No violations identified - this section intentionally left empty**

All constitution requirements satisfied without exceptions. Configuration-driven
variation implemented for all customization points. Carbon Design System used
exclusively. FHIR R4 SupplyDelivery resources for external integration. 5-layer
architecture with JPA/Hibernate annotations, service-layer transactions, and
data compilation within transactions. Full test coverage planned (unit + ORM
validation + integration + E2E). Liquibase for all schema changes. React Intl
for all UI strings. RBAC, audit trail, encryption, and data retention
requirements included.

---

## Phase 0: Research & Technical Discovery

### Research Goals

Phase 0 resolves technical unknowns and establishes implementation patterns
before detailed design.

#### R1: FHIR SupplyDelivery Profile Research

- **Question**: How do we model box shipments as FHIR R4 SupplyDelivery
  resources?
- **Scope**: Map ShippingBox + Shipment entities to SupplyDelivery resource
  structure
- **Deliverable**: FHIR resource examples with field mappings documented in
  research.md

#### R2: OpenELIS Quality System Integration Patterns

- **Question**: How do we create non-conformities in existing OpenELIS quality
  module from shipment receiving workflow?
- **Scope**: Identify existing NonConformity entity structure, service methods,
  required fields
- **Deliverable**: Integration pattern documented with service method signatures
  and data flow

#### R3: Barcode Scanning Implementation Patterns

- **Question**: How do USB keyboard wedge scanners integrate with React form
  inputs?
- **Scope**: Event handling, validation, visual feedback patterns for barcode
  scan fields
- **Deliverable**: React component pattern with Carbon input integration and
  "Simulate Scan" button

#### R4: Label Generation and Printing Options

- **Question**: What library/approach for generating printable labels with
  barcodes?
- **Scope**: Evaluate PDF generation libraries (iText, Apache PDFBox), barcode
  generation (Barcode4J, ZXing)
- **Deliverable**: Recommended stack with code example for label template

#### R5: Electronic Manifest Transmission Strategies

- **Question**: How do we implement retry logic with exponential backoff for
  FHIR/API/Email transmission?
- **Scope**: Spring Retry patterns, async processing, transmission status
  tracking
- **Deliverable**: Service pattern with retry configuration and status
  persistence

#### R6: Performance Optimization for Large Datasets

- **Question**: How do we efficiently query 1,000+ boxes and 10,000+ samples
  with filtering/pagination?
- **Scope**: JPA pagination, query optimization, index strategy
- **Deliverable**: DAO method examples with performance testing criteria

#### R7: Hibernate Lazy Loading Prevention Patterns

- **Question**: How do we ensure services compile all hierarchical data within
  transactions?
- **Scope**: JOIN FETCH strategies, DTO mapping patterns, N+1 query prevention
- **Deliverable**: Service method examples with HQL queries and DTO compilation

#### R8: Data Retention and Archival Strategy

- **Question**: How do we implement 7-year retention with archival for older
  records?
- **Scope**: Database partitioning, archival table patterns, scheduled cleanup
  jobs
- **Deliverable**: Liquibase schema design with archival strategy documented

### Research Output

**File**: `research.md` (generated in Phase 0)

Structure:

```markdown
# Research Findings: Shipment Support

## R1: FHIR SupplyDelivery Profile

**Decision**: [chosen approach] **Rationale**: [why chosen] **Alternatives
considered**: [what else evaluated]

## R2: Quality System Integration

[... pattern for each research goal]
```

---

## Phase 1: Design & Contracts

### Prerequisites

- `research.md` complete with all NEEDS CLARIFICATION resolved

### Deliverables

#### D1: Data Model (`data-model.md`)

Extract entities from spec.md Key Entities section and enrich with:

- **Valueholder classes**: ShippingBox, Shipment, BoxSample, UnassignedSample,
  ShipmentFacility, NonConformityAttachment
- **Fields with JPA annotations**: @Column constraints, @ManyToOne
  relationships, @Enumerated for state enums
- **State machine definitions**: Box states (Draft → Ready to Send → Sent → In
  Transit → Partially Received → Received → Reconciled), alternative paths
  (Cancelled, Lost in Transit)
- **Validation rules**: @NotNull, @Size, @Pattern constraints derived from
  functional requirements
- **Indexes**: Performance-critical fields (box_id, shipment_id,
  sample_accession_number, destination_facility_id, sent_date, received_date)

#### D2: API Contracts (`contracts/openapi.yaml`)

Generate REST API contract from functional requirements:

**Endpoints derived from user stories:**

- `POST /rest/shipment/boxes` - Create box (US1, FR-001, FR-002)
- `GET /rest/shipment/boxes/{id}` - Get box details
- `PUT /rest/shipment/boxes/{id}/samples` - Add sample to box (US1, FR-003,
  FR-004)
- `DELETE /rest/shipment/boxes/{id}/samples/{sampleId}` - Remove sample (FR-009)
- `GET /rest/shipment/unassigned` - List unassigned samples (US2, FR-010)
- `PUT /rest/shipment/boxes/{id}/ready` - Mark box as Ready to Send (US3,
  FR-017)
- `POST /rest/shipment/boxes/{id}/send` - Send box (US4, FR-021, FR-022, FR-023)
- `POST /rest/shipment/boxes/{id}/receive` - Initiate receiving (US5, FR-024)
- `PUT /rest/shipment/boxes/{id}/receive/samples/{sampleId}` - Mark sample
  received (FR-025)
- `POST /rest/shipment/boxes/{id}/receive/nonconformities` - Record
  non-conformity (FR-027)
- `POST /rest/shipment/boxes/{id}/receive/complete` - Complete receiving
  (FR-030)
- `GET /rest/shipment/dashboard/metrics` - Get dashboard metrics (US6, FR-031)
- `GET /rest/shipment/boxes` - List/search/filter boxes (FR-032)
- `POST /rest/shipment/reports` - Generate report (FR-033)

**FHIR Profiles** (`contracts/fhir-profiles/`):

- SupplyDelivery resource examples for electronic manifest transmission

#### D3: Quickstart Guide (`quickstart.md`)

Step-by-step implementation example for User Story 1 (Create and Manage Sample
Boxes):

1. Create database tables (Liquibase)
2. Create ShippingBox valueholder
3. Create ShippingBoxDAO interface + implementation
4. Create ShippingBoxService (TDD: write test first)
5. Create ShippingBoxRestController
6. Create ShippingBoxForm DTO
7. Create CreateBoxModal React component
8. Create internationalization keys (en.json, fr.json)
9. Write E2E test (boxCreation.cy.js)

#### D4: Agent Context Update

Run `.specify/scripts/bash/update-agent-context.sh claude` to update
CLAUDE_CONTEXT.md with shipment module architecture and patterns.

---

## Phase 2: Task Breakdown

**OUT OF SCOPE** for `/speckit.plan` command.

Phase 2 executed separately via `/speckit.tasks` command, which generates
`tasks.md` with:

- Dependency-ordered task list
- TDD workflow for each task (Red → Green → Refactor)
- Acceptance criteria per task
- Estimated complexity (Small/Medium/Large)

---

## Implementation Notes

### Critical Path Items

1. **Quality System Integration** (R2, FR-027): Must identify exact
   NonConformity entity structure and service methods in existing codebase
   before implementing receiving workflow. Dependency: Explore
   `org.openelisglobal.nonconformity` package.

2. **FHIR SupplyDelivery Transform** (R1, FR-040): FhirTransformService must
   support bidirectional ShippingBox ↔ SupplyDelivery conversion. Dependency:
   Understand existing FHIR transform patterns in
   `org.openelisglobal.fhir.FhirTransformServiceImpl`.

3. **Lazy Loading Prevention** (R7, Constitution IV): Services MUST use JOIN
   FETCH to eagerly load all box relationships (samples, destination facility,
   shipment, tracking info) within transaction. Controllers receive complete
   DTOs/maps, NO entity relationship traversal allowed.

4. **Performance Constraints** (FR-054, CR-009): 5-second max response time
   enforced. Dashboard metrics queries must use database indexes on date/state
   columns. Pagination required for box lists (100 items per page default).

5. **E2E Test Organization** (Constitution V.5): Individual test execution
   during development, full suite only in CI/CD. Maximum 20-30 test cases total
   across 6 test files. Video recording disabled, screenshots enabled, console
   logging mandatory review.

### Risk Mitigation

- **Risk**: LazyInitializationException when controllers access box
  relationships

  - **Mitigation**: Services compile complete DTOs with JOIN FETCH; ORM
    validation tests catch mapping errors; code review checklist verifies no
    relationship traversal in controllers

- **Risk**: Electronic manifest transmission failures impact workflow

  - **Mitigation**: Async processing with retry logic (3 attempts, exponential
    backoff); manual retry UI option; transmission status tracked in database

- **Risk**: Barcode scanner hardware unavailability during development/testing

  - **Mitigation**: "Simulate Scan" functionality (FR-048) allows keyboard
    input; E2E tests use simulated scans

- **Risk**: Performance degradation with 10,000+ samples
  - **Mitigation**: Database indexes on critical columns; pagination for all
    list queries; performance testing with realistic data volumes

### Acceptance Gates

Before declaring implementation complete:

- [ ] All 55 functional requirements implemented and tested
- [ ] Constitution Check passes (all 8 principles verified)
- [ ] Unit tests: >70% code coverage (JaCoCo report)
- [ ] ORM validation tests: SessionFactory builds without errors (<5s)
- [ ] Integration tests: All REST endpoints pass with test database
- [ ] E2E tests: All user stories validated (<5 minutes total execution)
- [ ] Performance tests: 5-second max response time verified under load
- [ ] Security review: Encryption, RBAC, audit trail, input validation confirmed
- [ ] Code formatting: `mvn spotless:apply` + `npm run format` executed
- [ ] Liquibase migrations: Tested on empty database and production-like data
- [ ] Internationalization: All UI strings in en.json + fr.json, NO hardcoded
      text
- [ ] FHIR validation: SupplyDelivery resources pass R4 profile validation
- [ ] Manual testing: Full workflow tested by QA (box creation → sending →
      receiving → reconciliation)

---

## Next Steps

### Immediate Actions

1. **Review Plan Approval**: Stakeholders review and approve this implementation
   plan
2. **Execute Phase 0**: Generate research.md with technical discovery findings
3. **Execute Phase 1**: Generate data-model.md, contracts/, and quickstart.md
4. **Generate Tasks**: Run `/speckit.tasks` to create dependency-ordered task
   breakdown
5. **Begin Implementation**: Follow TDD workflow from tasks.md

### Success Metrics

Track progress using these metrics:

- **Planning**: Constitution Check ✅ (8/8 principles satisfied)
- **Research**: 8 research goals documented with decisions
- **Design**: Data model + API contracts + quickstart guide complete
- **Implementation**: Track via tasks.md completion percentage
- **Testing**: >70% code coverage + all E2E tests passing (<5 min execution)
- **Performance**: All operations <5 seconds response time verified
- **Security**: Encryption, RBAC, audit trail, data retention verified

### Critical Success Factors

- **Quality System Integration**: Must explore existing
  `org.openelisglobal.qaevent` package early to understand NonConformity
  integration patterns
- **FHIR Compliance**: Must validate SupplyDelivery resource transformations
  against R4 spec
- **Lazy Loading Prevention**: All services must compile DTOs within
  transactions - code reviews must verify NO relationship traversal in
  controllers
- **E2E Test Discipline**: Run tests individually during development, review
  console logs and screenshots after EVERY run
- **Pre-Commit Formatting**: MUST run `mvn spotless:apply` + `npm run format`
  before EVERY commit
