# Research: External Quality Assurance (EQA) Module

**Feature**: 005-eqa-module **Date**: 2025-11-18 **Status**: Complete

## Research Summary

All technical unknowns from the plan's Technical Context have been resolved
through codebase exploration. Key decisions are documented below.

---

## R1: Sample Entity Extension Strategy

**Question**: How to add EQA-specific fields to the existing Sample entity which
uses legacy XML Hibernate mappings?

**Decision**: Create a separate `SampleEQA` entity with JPA annotations

**Rationale**:

- The existing `Sample` entity uses `Sample.hbm.xml` (legacy XML mapping)
- Constitution mandates JPA annotations for new entities (XML exempt until
  refactored)
- Creating `SampleEQA` avoids modifying legacy XML while keeping new code
  compliant
- `SampleEQA` references `Sample` via FK (`sample_id`), one-to-one relationship
- Similar pattern to `ProgramSample` which already uses JPA annotations to
  reference the XML-mapped `Sample`

**Alternatives Considered**:

- (a) Add columns to `SAMPLE` table + extend `Sample.hbm.xml` - Rejected:
  violates constitution's annotation mandate for new code
- (c) Extend `Sample` class with annotations on new fields only - Rejected:
  Hibernate cannot mix XML + annotation mapping for the same class

---

## R2: EQA Program vs Existing Program Entity

**Question**: Should EQA programs reuse the existing `Program` entity or create
a new `EQAProgram`?

**Decision**: Create new `EQAProgram` entity

**Rationale**:

- Existing `Program` entity is tightly coupled to FHIR Questionnaire workflows
  (has `questionnaireUUID`, `code`, `testSection`)
- EQA programs have different properties: name, description, active status, test
  assignments via junction table
- EQA programs are administrative configuration, not clinical program workflows
- Separate entity avoids polluting the existing Program concept
- Clear separation of concerns: `Program` = clinical workflows, `EQAProgram` =
  proficiency testing

**Alternatives Considered**:

- Extend `Program` with EQA fields - Rejected: semantic mismatch, Program is
  questionnaire-driven
- Use configuration table - Rejected: EQA programs need proper entity lifecycle
  (CRUD, relationships, audit trail)

---

## R3: Alert System Extension

**Question**: How to extend the existing polymorphic Alert system for EQA
deadline alerts and comprehensive alert dashboard?

**Decision**: Extend existing `AlertType` enum + enhance `AlertService`

**Rationale**:

- Existing `Alert` entity is already polymorphic with `alertEntityType` +
  `alertEntityId` pattern
- `AlertType` enum already has: FREEZER_TEMPERATURE, EQUIPMENT_FAILURE,
  INVENTORY_LOW, SAMPLE_TRACKING, OTHER
- Adding EQA types: `EQA_DEADLINE`, `SAMPLE_EXPIRATION`, `STAT_UPCOMING`,
  `STAT_OVERDUE`, `CRITICAL_UNACKNOWLEDGED`
- Existing `AlertService` already provides deduplication, event publishing,
  acknowledgment workflow
- Dashboard can query all alert types via existing `/rest/alerts` endpoint with
  filtering

**Approach**:

1. Add new values to `AlertType` enum (Java code)
2. Liquibase changeset to update any DB constraints if AlertType is persisted as
   string
3. Create `EQADeadlineAlertScheduler` that calls existing
   `AlertService.createAlert()` with new types
4. Extend `AlertRestController` with additional filter/query parameters for
   dashboard needs (severity counts, lab section filtering)
5. Reuse existing alert acknowledgment + resolution workflow

**Alternatives Considered**:

- Create separate EQA alert entity - Rejected: duplicates existing
  infrastructure, no polymorphic querying across alert types

---

## R4: Scheduled Alert Generation

**Question**: How should EQA deadline alerts be generated at 72h, 24h, and 4h
intervals before deadline?

**Decision**: `@Scheduled` service polling active EQA samples

**Rationale**:

- Existing `ModbusPollingService` uses `@Scheduled(fixedDelayString=...)`
  pattern successfully
- `SchedulerConfig` already supports both Quartz and Spring `@Scheduled`
- Alert deduplication (30-min window) in `AlertService` prevents duplicate
  alerts from overlapping scheduler runs

**Implementation**:

```java
@Service
public class EQADeadlineAlertScheduler {

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkEQADeadlines() {
        // Query SampleEQA where deadline within thresholds
        // For each, determine severity (72h=INFO, 24h=WARNING, 4h=CRITICAL)
        // Call alertService.createAlert() - dedup handles repeat
    }
}
```

**Alert Escalation** (4-hour unacknowledged):

- Same scheduler checks alert creation time vs current time
- If alert age > 4 hours and status = OPEN, escalate severity
- Increment notification badge, trigger ToastNotification on supervisor login

**Alternatives Considered**:

- Database triggers for deadline monitoring - Rejected: constitution prohibits
  native SQL/DDL outside Liquibase, hard to test
- Event-driven (on sample creation) - Rejected: doesn't handle ongoing
  monitoring as deadlines approach

---

## R5: Statistical Calculation Engine

**Question**: How to implement Z-score calculations and performance
classification?

**Decision**: Dedicated `EQAStatisticsService` with pure calculation methods

**Rationale**:

- Z-score formula: `Z = (result - target) / SD`
- Standard deviation calculated from all participant results
- Performance thresholds: |Z| ≤ 2.0 = Acceptable, 2.0-3.0 = Questionable,
  > 3.0 = Unacceptable
- These are standard WHO/CAP proficiency testing formulas
- Pure calculation methods are easily unit-testable

**Implementation Notes**:

- Use `BigDecimal` for precision (not double)
- Minimum 5 participants check before calculation
- Mean = sum(results) / count(results)
- SD = sqrt(sum((xi - mean)^2) / (n-1)) - sample standard deviation
- Handle edge case: SD = 0 (all participants same result)

**Alternatives Considered**:

- External statistics library (Apache Commons Math) - May use if needed for
  advanced calculations, but Z-score is simple enough for direct implementation

---

## R6: PDF Report Generation

**Question**: Which library for PDF performance report generation?

**Decision**: Use OpenPDF (fork of iText) or existing report infrastructure

**Rationale**:

- OpenELIS already has report generation capabilities
- Need to investigate existing report infrastructure (JasperReports or similar)
- Reports must include: participant results, Z-scores, performance
  classification, comparative analysis
- SC-006 requires <5s generation for 50 participants

**Research Needed During M6**:

- Check existing report generation patterns in codebase
- Evaluate if JasperReports is already a dependency
- If not, OpenPDF is lightweight and Apache-licensed

---

## R7: FHIR Result Submission Format

**Question**: Which FHIR resources for EQA result submission between OpenELIS
instances?

**Decision**: Use DiagnosticReport + Observation bundle

**Rationale**:

- Existing `FhirTransformService` already handles DiagnosticReport and
  Observation resources
- EQA results map naturally to:
  - `DiagnosticReport` = distribution result submission
  - `Observation` = individual test result values
  - `ServiceRequest` = EQA sample request
- Reuse `FhirPersistanceService.createFhirResourceInFhirStore()` and
  `makeTransactionBundleForCreate()` for batch submission

**Alternatives Considered**:

- Custom API (non-FHIR) - Rejected: violates Constitution Principle III

---

## R8: SampleEQA vs Order Extension

**Question**: The spec mentions extending "Order" entity, but OpenELIS uses
"Sample" as the primary entity. Which to extend?

**Decision**: Create `SampleEQA` entity linked to `Sample` via FK

**Rationale**:

- OpenELIS uses `Sample` as the primary entity for sample tracking
- There is no separate "Order" entity - orders are represented by `Sample` +
  related entities (`SampleItem`, `ProgramSample`, etc.)
- The spec's "Order" concept maps to OpenELIS's `Sample` entity
- `SampleEQA` stores all EQA-specific metadata:
  - `sample_id` (FK to Sample)
  - `eqa_program_id` (FK to EQAProgram)
  - `eqa_provider_organization_id` (FK to Organization)
  - `eqa_provider_sample_id` (string)
  - `eqa_participant_id` (string, nullable)
  - `eqa_deadline` (timestamp)
  - `eqa_priority` (enum)
  - `eqa_distribution_id` (FK to EQADistribution, nullable)
  - `is_eqa_sample` (boolean)

---

## R9: Frontend State Management for EQA

**Question**: How to manage EQA-specific state in the sample entry workflow?

**Decision**: Extend existing `orderFormValues` state object

**Rationale**:

- Sample entry uses `SampleOrderFormValues` object managed via React state
- EQA fields added to `sampleOrderItems` sub-object
- Submitted via existing `POST /rest/SamplePatientEntry` endpoint
- Backend controller extracts EQA fields and creates `SampleEQA` record

**New State Shape**:

```javascript
sampleOrderItems: {
  ...existingFields,
  isEQASample: false,          // EQA checkbox state
  eqaProgramId: "",            // Selected EQA program
  eqaProviderOrganizationId: "", // EQA provider org
  eqaProviderSampleId: "",     // Provider's sample ID
  eqaParticipantId: "",        // Lab's participant ID
  eqaDeadline: "",             // Testing deadline
  eqaPriority: "STANDARD"     // Priority level
}
```

---

## R10: Westgard Rules Implementation

**Question**: How to implement configurable Westgard rules for QC evaluation?

**Decision**: Rule engine with Strategy pattern

**Rationale**:

- Six core rules: 1-2s, 1-3s, 2-2s, R-4s, 4-1s, 10-x
- Each rule has different evaluation logic and data requirements
- Rules must be enable/disable per test type (configurable)
- Strategy pattern allows clean separation of rule logic

**Implementation**:

- `WestgardRule` interface with `evaluate(List<QCResult>)` method
- Concrete implementations: `Rule12s`, `Rule13s`, `Rule22s`, `RuleR4s`,
  `Rule41s`, `Rule10x`
- `WestgardRuleEngine` orchestrates all enabled rules
- Configuration stored in `qc_rule_config` table per test type

---

## R11: Levey-Jennings Chart Technology

**Question**: How to render Levey-Jennings charts in the frontend?

**Decision**: Use `@carbon/charts-react` (already in dependencies)

**Rationale**:

- `@carbon/charts-react v1.5.2` is already a project dependency
- Supports line charts with control limits
- LJ chart = line chart with:
  - X-axis: measurement sequence/date
  - Y-axis: control value
  - Horizontal lines at mean, +/-2SD, +/-3SD
  - Data points with hover tooltips (for QC comments)
- Carbon charts integrate seamlessly with Carbon Design System styling

**Alternatives Considered**:

- D3.js directly - Rejected: more complex, Carbon charts already available
- Chart.js - Rejected: not in Carbon ecosystem

---

## R12: Electronic Signature Implementation

**Question**: How to implement electronic signature for QC report review without
password re-entry?

**Decision**: Session-based signature with audit metadata

**Rationale**:

- User is already authenticated via Spring Security session
- "Signing" means recording: user ID, timestamp, IP address, optional comment
- No additional authentication step (spec says "without password re-entry")
- Stored in `qc_signature` table with FK to QC report

**Implementation**:

- `QCSignature` entity: userId, timestamp, ipAddress, comment, reportId
- Frontend: Carbon Modal with optional comment TextArea + "Sign" button
- Backend: Extract user from session, IP from request, record timestamp
- Display on QC reports: reviewer name, signature time, comments

---

## Unresolved Items

None. All NEEDS CLARIFICATION items from Technical Context have been resolved.
