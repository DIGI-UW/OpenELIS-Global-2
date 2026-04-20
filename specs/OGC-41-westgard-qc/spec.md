# Feature Specification: Westgard QC Rules Dashboard

**Feature Branch**: `003-westgard-qc` **Created**: 2026-04-13 **Status**: Draft
(v1 implementation in review) **Jira**:
[OGC-41 — Westgard Rules Dashboard for Analyzers](https://uwdigi.atlassian.net/browse/OGC-41)
**Labels**: Madagascar, Indonesia

## Source Documents

- **Design requirements**:
  [westgard-rules.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)
  — FR1-FR13, NFR1-NFR6
- **Dashboard mockup (Figma)**:
  [kit-mango-37758223.figma.site](https://kit-mango-37758223.figma.site)
- **Dashboard gallery**:
  [westgard-dashboard.jsx](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-dashboard.jsx)
- **Manual QC spec**:
  [analyzer-manual-qc.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/analyzer-manual-qc.md)
  (separate feature, not in v1 scope)
- **Implementation**: PR
  [#3390](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3390), Bridge PR
  [#33](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/33)

## Executive Summary

Laboratory instruments produce quality control (QC) measurements alongside
patient samples to verify that the analytical system is performing correctly.
This feature provides automated Westgard rule evaluation, a real-time compliance
dashboard, Levey-Jennings control charts, and configurable alerting — replacing
a fully manual, paper-based QC process.

The system identifies QC samples automatically (via configurable rules evaluated
by the analyzer bridge before results reach OpenELIS), persists QC measurements
with statistical context, evaluates 8 standard Westgard rules asynchronously,
and surfaces compliance status through a color-coded dashboard with drill-down
charts and violation tracking.

---

## User Scenarios & Testing _(mandatory)_

### User Story 1 — Lab Technician Monitors Instrument Compliance (Priority: P1)

A lab technician starts their shift and opens the QC dashboard to check whether
all instruments are in compliance. They see a summary showing how many
instruments are compliant (green), have warnings (yellow), or are out of
compliance (red). They click on a yellow instrument card to see which Westgard
rule triggered the warning, review the Levey-Jennings chart to understand the
trend, and decide whether to re-run the control.

**Why this priority**: This is the primary daily workflow for QC monitoring.
Without it, technicians have no digital visibility into instrument compliance.

**Independent Test**: Navigate to the QC dashboard, verify summary tiles show
counts, verify instrument cards display status, click a card and verify the
detail page shows violations and a chart.

**Acceptance Scenarios**:

1. **Given** QC results have been ingested for 3 instruments, **When** the
   technician opens the QC dashboard, **Then** summary tiles show the correct
   count of compliant/warning/out-of-compliance instruments.
2. **Given** an instrument has a 1-2s warning violation, **When** the technician
   clicks the instrument card, **Then** the detail page shows the violation with
   the rule code, severity, z-score, and the affected result.
3. **Given** the dashboard is open, **When** 5 minutes elapse, **Then** the
   dashboard auto-refreshes with updated data.

---

### User Story 2 — QC Officer Sets Up a New Control Lot (Priority: P1)

When a new lot of QC control material arrives, the QC officer creates a control
lot in the system, selecting the test and instrument it applies to, the
manufacturer's lot number, expiry date, and the statistical calculation method
(initial runs, rolling window, or manufacturer-provided values). For
manufacturer-provided values, the mean and SD are entered immediately and the
lot activates. For initial-runs or rolling methods, the lot enters an
"establishment" phase where it collects enough data points before activating.

**Why this priority**: Without control lots, no QC results can be evaluated.
This is a prerequisite for all Westgard rule evaluation.

**Independent Test**: Navigate to the control lot setup page, fill out the form,
save, verify the lot appears in the list with the correct status (ESTABLISHMENT
for initial/rolling, ACTIVE for manufacturer-fixed).

**Acceptance Scenarios**:

1. **Given** the QC officer selects "Manufacturer Fixed" as the calculation
   method, **When** they enter mean and SD and save, **Then** the lot is created
   with ACTIVE status and statistics are immediately available.
2. **Given** the QC officer selects "Initial Runs" with N=20, **When** they save
   without entering manufacturer values, **Then** the lot is created with
   ESTABLISHMENT status and a message indicates how many runs are needed.
3. **Given** an ESTABLISHMENT lot has accumulated 20 QC results, **When** the
   officer activates the lot, **Then** statistics are calculated from the
   initial runs and the lot transitions to ACTIVE.

---

### User Story 3 — Automatic QC Detection from Analyzer Files (Priority: P1)

When the analyzer bridge processes a result file (Excel, CSV) or ASTM message,
it evaluates configurable QC identification rules (e.g., specimen ID starts with
"CNEG", or the Task column says "STANDARD") to determine which rows are QC
control samples vs patient samples. QC samples are tagged before results reach
OpenELIS. OpenELIS routes tagged results to the QC pipeline, computes z-scores,
and triggers Westgard rule evaluation.

**Why this priority**: This is the data intake path. Without automatic QC
identification, no results flow into the Westgard evaluation engine.

**Independent Test**: Drop a file with a control sample (e.g., specimen ID
"CNEG001") into a FILE analyzer's watched directory. Verify the system tags it
as QC, creates a QC result, and (if the lot is active) runs Westgard evaluation.

**Acceptance Scenarios**:

1. **Given** a QuantStudio Excel file contains rows with Task="STANDARD",
   **When** the bridge processes the file, **Then** those rows are identified as
   QC control samples.
2. **Given** a FILE analyzer has no QC rules configured, **When** a file is
   processed, **Then** all rows are treated as patient samples (no QC tagging).
3. **Given** a QC-tagged result arrives and the control lot is ACTIVE, **When**
   the result is persisted, **Then** a z-score is computed and Westgard rules
   are evaluated without blocking the result ingestion pipeline.

---

### User Story 4 — Lab Manager Reviews Levey-Jennings Chart (Priority: P2)

The lab manager opens the control chart for a specific instrument/test
combination to review QC performance over time. The Levey-Jennings chart shows
each QC result plotted chronologically, with reference lines for the mean and
plus/minus 1, 2, 3 standard deviations. Points that violated Westgard rules are
highlighted with tooltip details.

**Why this priority**: The L-J chart is the standard clinical laboratory
visualization for QC trending. It's essential for ISO 15189 compliance but can
function as a read-only view after the core pipeline (Stories 1-3) works.

**Independent Test**: Navigate to a control lot's chart page, verify the chart
renders with data points, reference lines, and violation markers.

**Acceptance Scenarios**:

1. **Given** a control lot has 30 QC results, **When** the manager opens the
   chart, **Then** all 30 points are plotted with mean and SD reference lines.
2. **Given** a result with z=3.5 triggered the 1-3s rule, **When** the chart
   renders, **Then** that point is visually highlighted as a violation.

---

### User Story 5 — Lab Supervisor Configures Westgard Rules (Priority: P2)

The lab supervisor opens the rule configuration panel and enables or disables
specific Westgard rules for each test-instrument combination. They can set
whether each rule is a "warning" (informational) or "rejection" (requires
action), and whether corrective action is required. A default multi-rule preset
is available to apply in one click.

**Why this priority**: Configuration is needed for customization but the system
ships with sensible defaults. Most labs can use the preset without manual
configuration.

**Independent Test**: Navigate to the rule config panel, verify it shows
test-instrument pairs with enabled rules, toggle a rule, verify the change
persists.

**Acceptance Scenarios**:

1. **Given** a new test-instrument pair, **When** the supervisor creates a
   control lot, **Then** all 8 Westgard rules are auto-enabled with default
   severity settings.
2. **Given** the supervisor disables a specific rule, **When** the next QC
   result is evaluated, **Then** that rule is skipped.

---

### User Story 6 — Lab Supervisor Receives Violation Alerts (Priority: P2)

When a Westgard rule is violated, the system creates an in-app alert. Rejection-
severity violations fire immediately. Warning-severity violations are batched in
15-minute windows to prevent alert fatigue. The supervisor sees alerts on the
dashboard's Alerts tab and can acknowledge them.

**Why this priority**: Alerts close the feedback loop — without them, violations
exist in the system but nobody is notified.

**Independent Test**: Trigger a rejection-severity violation, verify an alert
appears in the Alerts tab within seconds, verify the acknowledge action works.

**Acceptance Scenarios**:

1. **Given** a QC result triggers a rejection-severity rule, **When** the
   evaluation completes, **Then** an alert appears immediately for active users.
2. **Given** two warning-severity violations occur within the same 15-minute
   batch window for the same instrument/test, **When** the window closes,
   **Then** a single consolidated alert is sent (not one alert per violation).
3. **Given** the supervisor acknowledges a violation, **When** they refresh the
   dashboard, **Then** the violation shows as resolved.

---

### User Story 7 — Admin Configures Per-Analyzer QC Rules (Priority: P3)

A system administrator opens an analyzer's QC rules page to customize which
patterns identify QC samples for that specific instrument. Rules support four
types: field value equals, field value contains, specimen ID prefix matching,
and specimen ID regex patterns. Rules are seeded automatically from the
analyzer's profile when the analyzer is created, and the admin can add, edit, or
disable individual rules.

**Why this priority**: Profile-driven defaults cover most instruments. Manual
customization is for edge cases where the lab's naming convention differs from
the profile default.

**Independent Test**: Navigate to an analyzer's QC rules page, verify rules
seeded from profile appear, add a new rule, verify it persists and is picked up
by the bridge.

**Acceptance Scenarios**:

1. **Given** a QuantStudio analyzer is created from its profile, **When** the
   admin opens the QC rules page, **Then** 6 rules are pre-populated (2
   field-equals for STANDARD/NTC + 4 specimen-ID prefixes).
2. **Given** the admin adds a new specimen ID prefix rule, **When** the bridge
   next refreshes, **Then** the new rule appears in the bridge's active rules.

---

### Edge Cases

- What happens when a QC result arrives but no matching control lot exists? The
  result is stored but not routed to QC processing. An error record is created
  for review.
- What happens when a lot is still in ESTABLISHMENT phase? Results are stored
  and contribute to statistics accumulation, but Westgard rules are NOT
  evaluated (no z-score baseline available yet).
- What happens when the bridge has no QC rules for an analyzer? All samples are
  treated as patient samples. No QC identification occurs.
- What happens if rule evaluation fails (e.g., database timeout)? The QC result
  is already saved. The evaluation failure is logged but does not block the
  analyzer ingestion pipeline.
- What happens when a control lot expires? The lot transitions to EXPIRED
  status. New QC results cannot be linked to it. A new lot must be created.

## Requirements _(mandatory)_

### Functional Requirements

**QC Identification**

- **FR-001**: The system MUST evaluate configurable per-analyzer rules to
  identify QC samples before results reach the LIS. Rule evaluation happens on
  the analyzer bridge component, which pulls the active rule set from OpenELIS's
  analyzer registration API at bootstrap.
- **FR-002**: Four rule types MUST be supported: field value equals, field value
  contains, specimen ID prefix matching, and specimen ID regex patterns.
- **FR-003**: QC identification rules MUST be seeded from the analyzer's profile
  when the analyzer is created. Administrators can customize rules afterward.
- **FR-004**: Identified QC observations MUST be clearly marked in the data
  pipeline so they are routed to QC processing rather than patient result
  acceptance.

**QC Result Processing**

- **FR-005**: The system MUST route identified QC observations to the QC result
  processing pipeline automatically.
- **FR-006**: Each QC result MUST be associated with a control lot, test, and
  instrument.
- **FR-007**: A z-score MUST be computed for each QC result using the lot's
  current mean and standard deviation.
- **FR-008**: Z-score computation MUST be skipped when the lot is in its
  establishment phase (insufficient baseline data).

**Control Lot Management**

- **FR-009**: The system MUST support three statistical calculation methods:
  initial establishment (first N runs), rolling window, and manufacturer-
  provided fixed values.
- **FR-010**: Control lots MUST transition through a defined lifecycle:
  ESTABLISHMENT (collecting data) to ACTIVE (evaluating rules) to
  EXPIRED/ARCHIVED (retired).
- **FR-011**: Lots using manufacturer-provided values MUST activate immediately
  upon creation.

**Westgard Rule Evaluation**

- **FR-012**: The system MUST evaluate 8 standard Westgard rules: 1-2s
  (warning), 1-3s (rejection), 2-2s (rejection), R-4s (rejection), 3-1s
  (warning), 4-1s (rejection), 7-t (warning), 10-x (rejection).
- **FR-013**: Rule evaluation MUST run without blocking the analyzer result
  ingestion pipeline.
- **FR-014**: Each Westgard rule MUST be independently configurable
  (enable/disable, severity, requires-corrective-action flag) per
  test-instrument combination. The `requires_corrective_action` flag is
  persisted in v1 but enforcement of corrective-action workflows begins in M2
  (see v2+ roadmap).
- **FR-015**: The system MUST provide a default rule preset applied
  automatically when a control lot is created.

**Dashboard & Visualization**

- **FR-016**: The QC dashboard MUST display summary tiles showing counts of
  compliant, warning, and out-of-compliance instruments using color coding.
- **FR-017**: Each instrument MUST be represented as a clickable card showing
  compliance status, triggered rules, and last QC timestamp.
- **FR-018**: Levey-Jennings charts MUST plot QC results chronologically with
  reference lines for mean and standard deviation boundaries.
- **FR-019**: Points on charts that triggered rule violations MUST be visually
  distinguished from normal points.
- **FR-020**: The dashboard MUST auto-refresh at a regular interval.

**Alerts**

- **FR-021**: Rejection-severity violations MUST generate immediate in-app
  alerts.
- **FR-022**: Warning-severity violations MUST be batched to prevent alert
  fatigue.
- **FR-023**: Users MUST be able to acknowledge violations from the alerts view.

**Navigation**

- **FR-024**: The analyzer form, QC rules page, and instrument detail view MUST
  be full pages with breadcrumb navigation (not modal dialogs).

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react)
- **CR-002**: All UI strings MUST be internationalized via React Intl
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder, DAO,
  Service, Controller, Form)
- **CR-004**: Database schema changes MUST use Liquibase changesets; runtime
  configuration (QC rules, control lots) MUST come from the application layer,
  not from Liquibase seed data
- **CR-005**: QC data flowing between systems MUST use FHIR R4 conventions
- **CR-007**: Role-based access control with audit trail (user tracking and
  timestamps on all QC entities)
- **CR-008**: Tests MUST cover unit, integration, and E2E levels

### Key Entities

- **QC Control Lot**: A specific lot of QC control material, scoped to a
  test-instrument pair. Tracks lifecycle status (establishment, active, expired)
  and statistical calculation method. Anchor for all QC measurements.
- **QC Result**: A single QC measurement with value, computed z-score,
  timestamp, and evaluation status (pending, accepted, rejected).
- **QC Statistics**: Cached mean and standard deviation for a control lot,
  recalculated per the lot's chosen method.
- **Westgard Rule Configuration**: Per test-instrument toggle for each of the 8
  Westgard rules, with severity and corrective-action flags.
- **QC Rule Violation**: An immutable audit record created when a rule triggers,
  linking the triggering result and any related historical results.
- **QC Alert**: Notification record per user per violation, with read/unread
  tracking and batching for warnings.
- **Analyzer QC Rule**: Per-analyzer identification rule that determines which
  incoming samples are QC controls vs patient samples. Seeded from the
  analyzer's profile and customizable by administrators.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Lab technicians can assess instrument compliance status within 10
  seconds of opening the QC dashboard.
- **SC-002**: QC results from analyzer files are automatically identified and
  evaluated without manual data entry (for instruments with configured rules).
- **SC-003**: Westgard rule violations appear as alerts within 1 minute of the
  QC result being ingested.
- **SC-004**: Levey-Jennings charts render correctly with reference lines and
  violation markers for control lots with 5+ data points.
- **SC-005**: Control lot form submission completes in under 2 minutes per lot.
  MANUFACTURER_FIXED lots activate on save; INITIAL_RUNS and ROLLING lots remain
  in ESTABLISHMENT until the lab has accumulated enough real QC results (no
  fixed calendar-time target — establishment is data-driven).
- **SC-006**: All QC data is auditable — no QC result or violation can be
  deleted, only resolved or archived.
- **SC-007**: The QC evaluation pipeline does not degrade analyzer result
  ingestion performance.

## Assumptions & Constraints

- The analyzer bridge is always in the communication path between analyzers and
  OpenELIS. QC identification happens on the bridge side.
- QC rules are defined per analyzer profile and seeded at analyzer creation
  time. Database migrations handle schema only; runtime data comes from the
  application layer.
- v1 delivers in-app alerts only. Email and real-time push notifications are
  deferred to v2.
- v1 does not include corrective action workflows (recalibration, maintenance
  tracking, task assignment). Violations can be acknowledged/resolved but
  without typed corrective action categories.
- v1 does not include manual QC recording (the analyzer-manual-qc spec is a
  separate feature for instruments that run manual tests).
- v1 does not include trend analysis, reporting/export, or advanced chart
  interactions (zoom, pan, export to PDF).
- The system targets CLIA/CAP QC requirements and ISO 15189 audit trail
  standards.

## v1 Implementation Status

All v1 features are implemented across PR #3390 (OpenELIS) and Bridge PR #33.
327 backend + bridge tests cover the core evaluation engine, calculators,
services, event listener, and bridge-side rule evaluation. Frontend components
and REST controllers are implemented but lack automated test coverage (frontend
unit tests and E2E Playwright tests are v1 completion targets).

## v2+ Roadmap (deferred from design spec)

| Feature                                  | Design ref                         | Priority |
| ---------------------------------------- | ---------------------------------- | -------- |
| Corrective action workflow               | FR7 (westgard-rules.md)            | High     |
| Manual QC recording                      | analyzer-manual-qc.md              | High     |
| Email/push alerts                        | FR11.2, FR11.7 (westgard-rules.md) | Medium   |
| Trend analysis                           | FR10 (westgard-rules.md)           | Medium   |
| Reporting + PDF/CSV export               | FR12 (westgard-rules.md)           | Medium   |
| Chart zoom/pan/export                    | FR9.6, FR9.8 (westgard-rules.md)   | Low      |
| Manual rule re-evaluation / preview mode | FR5 (westgard-rules.md)            | Low      |
| Per-user notification preferences        | FR11.3 (westgard-rules.md)         | Low      |
| Granular QC roles (Results/Biologist)    | FR13.2-13.4 (westgard-rules.md)    | Low      |
| Multiple control levels per lot          | FR1.2 (westgard-rules.md)          | Medium   |
