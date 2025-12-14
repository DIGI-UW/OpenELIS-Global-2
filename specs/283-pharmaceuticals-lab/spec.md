# Feature Specification: Pharmaceuticals Laboratory Workflow

**Feature Branch**: `spec/283-pharmaceuticals-lab`  
**Created**: 2025-12-14  
**Status**: Draft  
**Input**: User description: "With inspiration from the current work of OGC-51,
create a spec document for this new laboratory for the different pages required.
Note that some of the existing backend services can be reused. Below are the
requirement for this Pharmaceuticals Laboratory in the document at the root
called Pharmaceutical.pdf."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Accession & QC for diverse sample types (Priority: P1)

As a sample intake technologist, I need to register pharmaceutical, herbal,
biological, and environmental specimens with required metadata, auto-generate
barcode/QR labels, and run lab-type-specific QC so only compliant samples
proceed.

**Why this priority**: Ensures traceability, regulatory compliance, and prevents
downstream errors; blocks the pipeline without it.

**Independent Test**: Create a new sample with mandatory metadata, print/scan
barcode, run QC checklist (pharma vs biological vs micro), and verify pass/fail
routing without touching downstream modules.

**Acceptance Scenarios**:

1. **Given** a new pharmaceutical product sample, **When** I capture metadata
   (sample name, IUPAC/chemical name, lot, grade, expiry, storage condition,
   requester, chain-of-custody) and print a barcode, **Then** the system saves
   the record with a unique ID and verifies the barcode/QR links to the LMIS
   entry.
2. **Given** a biological sample with insufficient volume, **When** I complete
   the biological QC checklist, **Then** the sample is auto-marked as QC-failed,
   a rejection reason is logged, and a replacement request notification is
   issued.

---

### User Story 2 - Notebook-based assay execution & review (Priority: P1)

As an analyst, I need notebook pages (OGC-51 style) for pharmaceutical assays
that support controls, replicates, acceptance criteria, and deviation logging so
assay runs are validated and reviewable.

**Why this priority**: Core value delivery—produces validated analytical results
while leveraging the existing notebook/page architecture.

**Independent Test**: Start an assay page from a pharma template, capture
controls and triplicates, calculate acceptance, flag OOS, and route for
multi-tier review without needing storage or distribution flows.

**Acceptance Scenarios**:

1. **Given** an HPLC potency assay template, **When** I enter control results
   and triplicate measurements, **Then** the page auto-computes %RSD, checks
   acceptance limits, and flags OOS runs.
2. **Given** an OOS flag, **When** I submit the run for review, **Then** primary
   and secondary reviewers can add comments, approve/reject, and CAPA/deviation
   records are linked to the notebook page.

---

### User Story 3 - Aliquoting, storage, and retrieval (Priority: P2)

As a sample manager, I need to aliquot parent samples, store them in a
hierarchical location (room → device → rack → box → position), and fulfill
retrieval/shipping requests with chain-of-custody to minimize freeze–thaw
cycles.

**Why this priority**: Preserves material integrity and supports downstream
testing and external sharing.

**Independent Test**: Create aliquots, assign storage positions using existing
storage services, request retrieval, and log chain-of-custody without executing
assays.

**Acceptance Scenarios**:

1. **Given** a parent pharmaceutical sample, **When** I create aliquots A01–A03
   and assign each to a storage position, **Then** parent–child lineage is
   stored and available for search and barcode scan.
2. **Given** an external shipment request, **When** a supervisor approves,
   **Then** the system records MTA/SDS attachments, generates a shipping
   checklist (IATA/GMP), and updates chain-of-custody with handler and
   timestamp.

---

### User Story 4 - Operational reporting & disposal (Priority: P3)

As a lab lead, I need dashboards for intake volumes, QC pass rates, assay
success/OOS, TAT, and disposal/archiving so I can monitor performance and
compliance.

**Why this priority**: Enables oversight and continuous improvement; required
for audits.

**Independent Test**: View a dashboard sourced from LMIS events (intake, QC,
assays, disposals) and export a compliance report without running new assays.

**Acceptance Scenarios**:

1. **Given** two weeks of intake activity, **When** I open the dashboard,
   **Then** I see counts by sample type, QC pass/fail rates, and median
   accession-to-assay TAT.
2. **Given** disposal events for expired samples, **When** I export a report,
   **Then** it lists sample IDs, disposal method (incineration/autoclave),
   sign-offs, and retention end dates.

---

### Edge Cases

- QC failure due to mislabeling, damaged container, or expired product must
  block progression and auto-notify requester.
- Insufficient volume or wrong storage condition triggers partial acceptance
  (some aliquots) with remaining volume flagged unavailable.
- Duplicate barcode/QR detection prevents ID collisions; system suggests next
  available identifier.
- Equipment failure (e.g., freezer alarm) triggers contingency workflow to move
  items to backup storage and log excursions.
- Freeze–thaw limit reached for an aliquot blocks further retrieval until
  supervisor override.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST register all sample types with mandatory metadata
  (sample name, IUPAC/chemical name, lot/batch, grade/spec, manufacture date,
  expiry/retest, storage condition, owner/requester, chain-of-custody, clinical
  trial/patient IDs when applicable).
- **FR-002**: System MUST generate and print human-readable + machine-readable
  labels (barcode/QR) linked to the LMIS record; scans must resolve the correct
  sample/aliquot.
  - **Label Format:** Configurable via `common.properties`; defaults: 1D barcode
    = Code 128, 2D = QR Code (Version 2, Error Correction Level M). Label
    content includes sample ID, accession number, and collection date.
- **FR-003**: System MUST execute lab-type-specific QC checklists (pharma,
  biological, microbiological/environmental) with pass/fail outcomes, rejection
  reasons, and requester notification.
- **FR-004**: System MUST capture initial processing steps (e.g., weighing,
  grinding, dissolution, centrifugation, streaking/filtration) including
  operator, timestamp, and resulting preparation ID for downstream assays.
- **FR-005**: System MUST provide notebook pages (reuse OGC-51 Notebook/Page
  architecture) for assay execution supporting controls, replicates,
  calculations (%RSD/CV), acceptance criteria, and OOS flagging.
- **FR-006**: System MUST log deviations/OOS with reasons and link CAPA actions
  to the corresponding assay notebook page.
- **FR-007**: System MUST support multi-tier review/approval (primary,
  secondary, investigator sign-off) before results are finalized.
- **FR-008**: System MUST manage aliquots with parent–child lineage, including
  label generation, aliquot ID sequencing, and usage history.
- **FR-009**: System MUST assign and search storage using hierarchical locations
  (room → device → rack → box → position) reusing existing Storage/Notebook
  backend services where applicable.
- **FR-010**: System MUST process retrieval/shipping requests with approval
  workflow, chain-of-custody logging (date/time/handler/condition), and attach
  MTA/SDS for external shipments (IATA/GMP compliant).
- **FR-011**: System MUST track environmental monitoring events and storage
  excursions, recording contingency moves to backup locations.
- **FR-012**: System MUST support disposal/archiving rules (expiry, exhaustion,
  failed QC, safety) with required sign-offs and retention tracking.
- **FR-013**: System MUST provide dashboards/reports for intake volumes, QC pass
  rates, assay success/OOS rates, TAT, storage utilization, and disposal
  summaries with export capability.
- **FR-014**: System SHOULD reuse existing backend services from OGC-51
  (NotebookService/Page templates) and Storage services for location management
  to minimize new surface area.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks.
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text).
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form); Valueholders use JPA annotations.
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct
  DDL/DML).
- **CR-005**: External data integration MUST use FHIR R4 + IHE profiles when
  applicable.
- **CR-006**: Configuration-driven variation for lab/country-specific rules (NO
  code branching).
- **CR-007**: Security: RBAC, audit trail (user + timestamp), input validation,
  and chain-of-custody logging.
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal).

### Key Entities _(include if feature involves data)_

- **PharmaceuticalSample**: Captures sample metadata (ID, name, IUPAC,
  lot/batch, grade, manufacture/expiry, storage condition, requester,
  chain-of-custody).
- **Aliquot**: Child of PharmaceuticalSample with aliquot ID, volume/weight,
  label data, freeze–thaw count, and storage assignment.
- **QCCheck**: Lab-type-specific checklist result with pass/fail, reasons, and
  reviewer.
- **ProcessingStep**: Recorded preparation step (e.g., dissolution,
  centrifugation) with operator, timestamp, and output reference.
- **AssayRun (Notebook Page)**: Assay template instance holding controls,
  replicates, calculations, acceptance criteria, OOS flags, and links to
  deviations/CAPA.
- **DeviationCAPA**: Records OOS/deviation, root cause, corrective/preventive
  actions, and status.
- **StorageLocation**: Hierarchical location object
  (room/device/rack/box/position) reused from storage services.
- **ChainOfCustodyEvent**: Logs retrieval/shipping/transfer with handler,
  condition, and approvals.
- **DisposalRecord**: Disposal or archiving decision with method, sign-offs, and
  retention references.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 95% of new samples registered with complete metadata and
  barcode/QR generated in under 2 minutes per sample.
  - **Measurement:** Wall-clock time from clicking "New Sample" to successful
    label print confirmation (measured via E2E test or manual stopwatch).
- **SC-002**: 100% of QC failures capture a rejection reason and trigger
  requester notification within 5 minutes.
  - **Measurement:** Time from QC status change to notification timestamp in
    audit log.
- **SC-003**: 100% of assay runs with OOS status are linked to a deviation/CAPA
  record before final approval.
- **SC-004**: 100% parent–child traceability for aliquots with searchable
  lineage and storage location.
- **SC-005**: Dashboards display intake/QC/assay/disposal metrics within 15
  minutes of event creation and support CSV/PDF export.
  - **Measurement:** Dashboard uses polling refresh (configurable interval,
    default 5 minutes) or manual refresh button; data staleness shown via "Last
    updated" timestamp.
- **SC-006**: Retrieval/shipping actions always produce a chain-of-custody entry
  with handler and timestamp (no missing events in audit).
