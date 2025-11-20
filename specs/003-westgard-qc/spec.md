# Feature Specification: Westgard Rules Quality Control Compliance

**Feature Branch**: `003-westgard-qc`
**Created**: 2025-11-14
**Status**: Draft
**Input**: User description: "I want to create a specification for a new feature that would implement lab analyzer compliance functionality according to issue OGC-41"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - View Analyzer Compliance Status (Priority: P1)

As a laboratory technician, when I check the compliance status of analyzers, I want to see a clear overview with visual indicators so that I can quickly assess which analyzers require attention and which are operating normally.

**Why this priority**: This is the foundational capability that provides immediate visibility into analyzer quality control status, enabling technicians to identify problems before processing patient samples.

**Independent Test**: Can be fully tested by displaying a dashboard with multiple analyzers showing color-coded status (green/yellow/red) based on pre-configured QC data, and delivers immediate value by showing compliance at a glance.

**Acceptance Scenarios**:

1. **Given** multiple analyzers are running QC tests, **When** I view the compliance dashboard, **Then** I see each analyzer displayed with its current compliance status indicated by color (green for compliant, yellow for warning, red for non-compliant)
2. **Given** an analyzer has recent QC results, **When** I view its analyzer card, **Then** I see the current compliance status, list of any triggered rules, and the date/time of the latest data point analyzed
3. **Given** I want detailed information about an analyzer, **When** I click on an analyzer card, **Then** I am taken to a detailed view showing control charts and historical data

---

### User Story 2 - Monitor QC Data with Control Charts (Priority: P1)

As a laboratory technician, when I need to understand why an analyzer is out of compliance, I want to view interactive Levey-Jennings charts with Westgard rule overlays so that I can visually identify patterns and violations in the QC data.

**Why this priority**: Control charts are essential for understanding the nature of quality control violations and determining appropriate corrective actions. This is a critical diagnostic tool.

**Independent Test**: Can be fully tested by displaying a Levey-Jennings chart for a single analyzer with plotted QC results, standard deviation lines (±1SD, ±2SD, ±3SD), and highlighted violation points.

**Acceptance Scenarios**:

1. **Given** an analyzer has historical QC results, **When** I view its control chart, **Then** I see QC values plotted over time with clearly marked mean and standard deviation limits (±1SD, ±2SD, ±3SD)
2. **Given** QC results violate Westgard rules, **When** I view the control chart, **Then** violation points are highlighted with distinct colors and increased size
3. **Given** I hover over a data point on the chart, **When** the tooltip appears, **Then** I see the result value, z-score, date/time, and any rule violations for that point
4. **Given** I need to focus on a specific time period, **When** I apply date range filters, **Then** the chart updates to show only data within that range

---

### User Story 3 - Receive Automated Alerts for Rule Violations (Priority: P1)

As a lab manager, when Westgard rules are violated, I want to receive automated alerts via email and system notifications so that I can take immediate corrective actions before processing patient samples.

**Why this priority**: Timely notification of quality control failures is critical for patient safety and regulatory compliance. Delays in addressing QC issues can compromise test accuracy.

**Independent Test**: Can be fully tested by triggering a QC rule violation (e.g., entering a result that exceeds 3 standard deviations) and verifying that email and in-system notifications are sent to configured recipients.

**Acceptance Scenarios**:

1. **Given** a rejection-level rule is violated (1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ), **When** the violation is detected, **Then** automated alerts are immediately sent via email to lab managers and technicians
2. **Given** a warning-level rule is triggered (1₂ₛ, 3₁ₛ, 7ₜ), **When** the violation is detected, **Then** system notifications appear in the user's notification feed
3. **Given** multiple violations occur on the same analyzer within 15 minutes, **When** alerts are generated, **Then** they are batched into a single notification to prevent alert fatigue
4. **Given** I receive an alert, **When** I click the notification link, **Then** I am taken directly to the detailed view of the affected analyzer and violation

---

### User Story 4 - Manage Corrective Actions (Priority: P2)

As a laboratory supervisor, when a rule violation requires corrective action, I want to log the actions taken and assign follow-up tasks so that we maintain a complete audit trail and ensure violations are properly resolved.

**Why this priority**: Regulatory compliance requires documented corrective actions for all quality control failures. This capability supports both operational needs and audit requirements.

**Independent Test**: Can be fully tested by creating a corrective action record for a known violation, assigning it to a user, tracking status changes, and verifying the violation is resolved when the action is completed.

**Acceptance Scenarios**:

1. **Given** a rejection-level violation exists, **When** I create a corrective action, **Then** I can select the action type (recalibration, maintenance, repeat control, reagent change, other), provide a description, and assign it to a specific user
2. **Given** a corrective action is assigned to me, **When** I view my task list, **Then** I see all pending corrective actions with their priority and due status
3. **Given** I complete a corrective action, **When** I mark it as completed with resolution notes, **Then** the associated violation is automatically resolved and the QC result is annotated with the corrective action summary
4. **Given** a violation has an active corrective action, **When** patient samples from that analyzer are ready for release, **Then** they are held pending violation resolution

---

### User Story 5 - Configure Westgard Rules (Priority: P2)

As a laboratory manager, when setting up quality control for an analyzer, I want to configure which Westgard rules are enabled and their parameters so that I can customize the quality control strategy to match our laboratory's needs and regulatory requirements.

**Why this priority**: Different analyzers and test methods may require different quality control strategies. Configuration capability allows the system to adapt to various laboratory environments and regulatory frameworks.

**Independent Test**: Can be fully tested by configuring rule sets for a test analyzer, enabling/disabling specific rules, and verifying that rule evaluation only applies the enabled rules.

**Acceptance Scenarios**:

1. **Given** I am setting up QC for a test-analyzer combination, **When** I access the rule configuration interface, **Then** I see all 8 available Westgard rules with options to enable/disable each one
2. **Given** I want a basic QC strategy, **When** I select a preset configuration (Basic, Standard, or Comprehensive), **Then** the appropriate rules are enabled automatically
3. **Given** I enable or disable rules, **When** I save the configuration, **Then** subsequent QC results for that test-analyzer combination are evaluated using only the enabled rules
4. **Given** I disable all rejection-level rules, **When** I attempt to save, **Then** the system prevents me and displays a warning that at least one rejection rule must be enabled

---

### User Story 6 - Manage QC Control Lots (Priority: P2)

As a laboratory supervisor, when introducing a new control lot or batch, I want to set up the control lot with its expected values and statistical parameters so that the system can accurately evaluate QC results against the appropriate standards.

**Why this priority**: Proper control lot management is foundational to accurate QC evaluation. Each lot has unique statistical characteristics that must be established before use.

**Independent Test**: Can be fully tested by creating a new control lot, configuring its statistical calculation method (initial runs, rolling, or manufacturer fixed), and verifying that QC results are correctly evaluated against the lot's parameters.

**Acceptance Scenarios**:

1. **Given** I receive a new QC control lot, **When** I create the lot record, **Then** I enter the product name, lot number, level (Low/Normal/High), associated test and analyzer, and activation dates
2. **Given** I am establishing statistics for a new lot, **When** I select the calculation method, **Then** I can choose from: initial establishment (first N runs, default 20), rolling calculation (moving window), or fixed manufacturer values
3. **Given** I select manufacturer fixed values, **When** I save the lot, **Then** the lot is immediately active and ready for QC result evaluation
4. **Given** I select initial establishment method, **When** I save the lot, **Then** the lot enters "establishment" status and does not evaluate Westgard rules until sufficient results are collected
5. **Given** a control lot has reached its expiration date, **When** I deactivate it, **Then** new QC results cannot be entered for that lot and the system prompts to set up a replacement lot

---

### User Story 7 - Analyze Compliance Trends (Priority: P3)

As a laboratory manager, when reviewing quality control performance, I want to view trend graphs showing compliance over time so that I can identify recurring issues, track improvements, and prepare for regulatory audits.

**Why this priority**: Trend analysis provides strategic insights for laboratory quality improvement and helps demonstrate ongoing compliance to accreditors. While important, it can be implemented after core operational features.

**Independent Test**: Can be fully tested by generating trend reports for a date range showing violation frequency by analyzer, rule type, and severity, with filtering and export capabilities.

**Acceptance Scenarios**:

1. **Given** I want to review QC performance, **When** I access the trend analysis view, **Then** I see graphs showing compliance percentage over time with filtering options for date range, analyzer, test, and rule type
2. **Given** I identify an analyzer with recurring violations, **When** I view its trend graph, **Then** I can see which specific rules are triggering most frequently
3. **Given** I need data for an audit, **When** I export compliance reports, **Then** I can generate summary reports showing violations by analyzer, rule type, time period, corrective action completion rate, and mean time to resolution
4. **Given** I want to compare multiple analyzers, **When** I view the violation frequency distribution, **Then** I can identify which analyzers have the highest violation rates

---

## Dependencies _(mandatory)_

**Feature 004 (ASTM Analyzer Field Mapping)**: Feature 003 depends on feature 004 for automatic QC result capture from analyzers. The dependency relationship is as follows:

1. **QC Result Capture**: Feature 004 handles ALL ASTM message processing, including parsing Q-segments (Quality Control result segments) from analyzer messages. See 004:FR-021 for detailed QC result processing requirements.

2. **Field Mapping Configuration**: Feature 004 provides the QC field mapping interface where administrators configure mappings for: Control Level (Low/Normal/High), Instrument ID, Lot Number, Result Value, Timestamp. See 004:FR-019 for QC field type definition.

3. **Error Handling**: Feature 004's Error Dashboard (004:FR-011) handles unmapped QC messages. When QC field mappings are incomplete, messages are queued in 004's error dashboard. Once mappings are configured, queued messages are automatically reprocessed.

4. **Integration Contract**: Feature 004 persists QC results by calling 003's QCResultService.createQCResult() method (per 004:plan.md QC Result Processing Integration section). This is a direct service-to-service call following the 5-layer architecture pattern.

**Implementation Order**: Feature 004 MUST be implemented before 003 can receive automatic QC results from analyzers. However, 003's manual entry functionality (FR-009) can be implemented independently for testing purposes.

**Navigation Hierarchy**: Feature 003's QC pages are nested under feature 004's /analyzers parent menu per 004:FR-020. Routes: /analyzers/qc (main QC dashboard), /analyzers/qc/alerts (QC Alerts & Violations), /analyzers/qc/corrective-actions (Corrective Actions). This navigation hierarchy is MANAGED by 004; 003 implements only the page components.

**Terminology**: This specification uses "analyzer" to refer to laboratory analyzer equipment (e.g., hematology analyzers, chemistry analyzers). The term "analyzer" is synonymous with "instrument" in legacy OpenELIS terminology. Feature 003 links QC results to the Analyzer entity (managed by feature 004), NOT the legacy Instrument entity.

---

### Edge Cases

- What happens when insufficient historical data exists to evaluate sequential rules (e.g., 10ₓ rule requires 10 previous results but only 5 are available)?
  - System should skip rules that cannot be evaluated and log an informational message
  - Dashboard should indicate when rules are not yet active due to insufficient data

- How does the system handle control lot changes mid-analysis?
  - Only results from the current active lot should be used for sequential rule evaluation
  - Switching lots resets the historical sequence for sequential rules

- What happens when multiple control levels (Low/Normal/High) are used for the same test?
  - Each control level is evaluated independently with its own statistics and rule configuration
  - Violations are tracked separately per level

- How does the system handle QC results entered out of chronological order?
  - System should accept results with any valid timestamp
  - Rule evaluation should consider chronological order based on timestamp, not entry order
  - Re-evaluation may be needed if out-of-order results affect sequential rules

- What happens when statistics are recalculated (e.g., when switching from initial to rolling calculation)?
  - System should provide option to re-evaluate all historical results with new statistics
  - Previous violations remain in audit trail but may be marked as "recalculated"

- How does the system handle analyzer downtime or maintenance periods?
  - Gaps in QC data should not trigger sequential rules that span the gap
  - System should allow marking analyzers as "under maintenance" to suppress alerts

- What happens when an alert recipient is unavailable (on leave, email bounces)?
  - System should have a backup notification chain
  - Undeliverable email alerts should be logged and escalated to administrators

- How does the system handle simultaneous violations of multiple rules?
  - All violated rules should be recorded in a single violation event
  - Notification should list all triggered rules with the most severe rule highlighted
  - Corrective action addresses all violations collectively

## Requirements _(mandatory)_

### Functional Requirements

#### QC Control Lot Management

- **FR-001**: System MUST support creation of QC control lots with product name, lot number, manufacturer information, and activation/deactivation dates
- **FR-002**: System MUST support multiple control levels (Low, Normal, High) per lot, each with independent statistical tracking
- **FR-003**: System MUST associate each control lot with a specific test-analyzer combination
- **FR-004**: System MUST support three statistical calculation methods: initial establishment (first N runs, configurable, default 20), rolling calculation (moving window), and fixed manufacturer values
- **FR-005**: System MUST prevent use of control lots before sufficient data is collected for statistical establishment (when using initial runs method)
- **FR-006**: System MUST require new statistical calculations when control lot changes
- **FR-007**: System MUST track who created each control lot and when it was created

#### QC Result Capture

- **FR-008**: System MUST provide QCResultService.createQCResult() method that accepts QC result data from feature 004's ASTM message processing. This method MUST: (1) Accept parameters: analyzer ID, test ID, control lot ID, control level, result value, unit, timestamp, (2) Calculate z-score based on control lot statistics, (3) Persist QCResult entity with all required fields, (4) Publish QCResultCreatedEvent to trigger rule evaluation, (5) Return created QCResult entity. **Integration Pattern**: Feature 004 calls this service method after parsing Q-segments and applying QC field mappings (per 004:FR-021). This is the ONLY entry point for QC results from analyzers.
- **FR-009**: System MUST support manual QC result entry as fallback method when automatic capture is unavailable. Manual entry MUST use the same QCResultService.createQCResult() method as automatic capture, ensuring consistent z-score calculation and rule evaluation triggering.
- **FR-010**: System MUST timestamp all QC results with run date and time
- **FR-011**: System MUST link QC results to the user who performed or approved the test
- **FR-012**: System MUST store result value and unit of measure
- **FR-013**: System MUST support manual result entry as fallback when interface is unavailable
- **FR-014**: System MUST validate that QC results are within reasonable range for the test method

#### Westgard Rule Configuration

- **FR-015**: System MUST allow configuration of rules per test-analyzer combination
- **FR-016**: System MUST support enabling and disabling individual rules independently
- **FR-017**: System MUST support all 8 standard Westgard rules:
  - 1₂ₛ: Single control exceeds ±2SD (warning)
  - 1₃ₛ: Single control exceeds ±3SD (rejection)
  - 2₂ₛ: Two consecutive controls exceed same ±2SD (rejection)
  - R₄ₛ: Range between two consecutive controls exceeds 4SD (rejection)
  - 4₁ₛ: Four consecutive controls exceed same ±1SD (rejection)
  - 10ₓ: Ten consecutive controls on same side of mean (rejection)
  - 3₁ₛ: Three consecutive controls exceed same ±1SD (warning)
  - 7ₜ: Seven consecutive controls showing consistent trend (warning)
- **FR-018**: System MUST classify each rule as WARNING or REJECTION severity
- **FR-019**: System MUST allow configuration of whether corrective action is required per rule
- **FR-020**: System MUST provide preset multi-rule configurations (Basic: 1₃ₛ only; Standard: 1₃ₛ/2₂ₛ/R₄ₛ/4₁ₛ; Comprehensive: all rules)
- **FR-021**: System MUST prevent configurations where no rejection-level rules are enabled

#### Automatic Rule Evaluation

- **FR-022**: System MUST automatically evaluate all enabled rules when a QC result is entered
- **FR-023**: System MUST calculate z-scores for each result using formula: (value - mean) / standard deviation
- **FR-024**: System MUST retrieve sufficient historical results needed for sequential rules (up to 10 previous results for 10ₓ rule)
- **FR-025**: System MUST create violation records when rules are triggered, linking all related QC results
- **FR-026**: System MUST apply non-conformity flags to results that cause rejection-level violations
- **FR-027**: System MUST continue processing even when some rules cannot be evaluated due to insufficient data
- **FR-028**: System MUST complete rule evaluation within 5 seconds of result entry

#### Manual Rule Evaluation

- **FR-029**: System MUST provide on-demand rule evaluation for historical data ranges
- **FR-030**: System MUST allow re-evaluation after statistics recalculation or rule configuration changes
- **FR-031**: System MUST allow evaluation of specific rule subsets
- **FR-032**: System MUST support preview mode that shows evaluation results without creating violation records

#### Violation Management

- **FR-033**: System MUST record all rule violations with timestamp, affected results, and triggering rule
- **FR-034**: System MUST track violation resolution status (unresolved, acknowledged, corrective action pending, resolved)
- **FR-035**: System MUST require corrective action for rejection-level violations before resolution
- **FR-036**: System MUST allow warning-level violations to be acknowledged without corrective action
- **FR-037**: System MUST prevent deletion of violations (audit requirement - violations can only be marked as resolved)
- **FR-038**: System MUST link violations to corrective actions when actions are created

#### Corrective Action Workflow

- **FR-039**: System MUST provide predefined corrective action types: Recalibration, Maintenance, Repeat Control, Reagent Change, Other (with description)
- **FR-040**: System MUST allow assignment of corrective actions to specific users
- **FR-041**: System MUST track corrective action status (Pending, In Progress, Completed)
- **FR-042**: System MUST require resolution notes when corrective action is marked as completed
- **FR-043**: System MUST add external notes to flagged QC results with corrective action summary upon completion
- **FR-044**: System MUST automatically resolve linked violations when corrective action is completed
- **FR-045**: System MUST prevent release of patient sample results from affected analyzer/test until violation is resolved

#### Dashboard & Visualization

- **FR-046**: System MUST display overview dashboard showing compliance status for all analyzers
- **FR-047**: System MUST use color-coded indicators: Green (all rules compliant), Yellow (warning-level violations present), Red (rejection-level violations present)
- **FR-048**: System MUST display analyzer cards showing: current compliance status, list of triggered rules with severity, date/time of last QC result, count of unresolved violations
- **FR-049**: System MUST make analyzer cards clickable to access detailed views
- **FR-050**: Dashboard MUST auto-refresh at configurable intervals (default: 5 minutes)
- **FR-051**: Dashboard MUST display timestamp of last data update

#### Levey-Jennings Charts

- **FR-052**: System MUST display interactive Levey-Jennings charts for each control lot showing QC results plotted chronologically
- **FR-053**: Charts MUST overlay reference lines for mean (solid), ±1SD (dashed), ±2SD (dashed), and ±3SD (solid)
- **FR-054**: Charts MUST highlight points violating rules using distinct colors and increased point size
- **FR-055**: Charts MUST display tooltips on hover showing result value, z-score, date/time, and rule violations
- **FR-056**: Charts MUST support date range filtering
- **FR-057**: Charts MUST support zoom and pan functionality
- **FR-058**: Charts MUST display multiple control levels in separate sections or tabs
- **FR-059**: Charts MUST allow export to standard image formats and printing

#### Trend Analysis

- **FR-060**: System MUST provide compliance trend graphs showing performance over time
- **FR-061**: Trend graphs MUST support filtering by date range, analyzer, test, control level, rule type, and severity
- **FR-062**: System MUST calculate and display compliance percentage over time
- **FR-063**: System MUST display violation frequency distribution by rule type
- **FR-064**: System MUST identify analyzers with recurring violations based on configurable thresholds

#### Alert & Notification System

- **FR-065**: System MUST generate real-time alerts when rules are violated
- **FR-066**: System MUST support two notification channels: email notifications and in-system notifications
- **FR-067**: System MUST allow users to configure notification preferences per rule severity
- **FR-068**: System MUST maintain real-time alert feed on dashboard showing recent violations
- **FR-069**: System MUST track notification read status
- **FR-070**: System MUST send alerts to: technician who ran the QC, assigned QC supervisor/manager, and users subscribed to specific analyzers
- **FR-071**: Email alerts MUST include: analyzer and test identification, rule violated, QC result value and z-score, link to detailed view, corrective action status
- **FR-072**: System MUST batch notifications (maximum one per analyzer per 15 minutes) to prevent alert fatigue
- **FR-073**: System MUST provide immediate (unbatched) alerts for 1₃ₛ violations regardless of batching settings

#### Reporting

- **FR-074**: System MUST provide violation history log with filtering and sorting capabilities
- **FR-075**: System MUST generate summary reports showing: violations by analyzer, violations by rule type, violations by time period, corrective action completion rate, mean time to resolution
- **FR-076**: System MUST export reports to standard formats (PDF and spreadsheet-compatible formats)
- **FR-077**: System MUST maintain complete audit trail of all violations, corrective actions, and status changes

#### Access Control

- **FR-078**: System MUST restrict QC features to users with "Results", "Biologist", or "Global Admin" roles
- **FR-079**: "Results" role MUST have permissions to: view QC results and charts, create corrective actions, view violations
- **FR-080**: "Biologist" role MUST have permissions to: all "Results" permissions, configure rules, manage control lots, resolve violations
- **FR-081**: "Global Admin" role MUST have full access including system configuration and user management

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form)
  - Valueholders MUST use JPA/Hibernate annotations (NO XML mapping files)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
- **CR-005**: Services MUST compile all data within transaction boundaries to prevent LazyInitializationException
- **CR-006**: @Transactional annotation MUST be used in services ONLY (NOT in controllers)
- **CR-007**: Configuration-driven variation for institution-specific requirements (NO code branching)
- **CR-008**: Security: RBAC enforcement, audit trail (sys_user_id + lastupdated), input validation on all data entry
- **CR-009**: Tests MUST be included (unit + integration + E2E, target >70% coverage)
- **CR-010**: Individual E2E tests MUST be runnable independently during development (NOT full suite)

### Key Entities

- **QC Control Lot**: Represents a specific batch of quality control material with defined characteristics. Attributes include: product name, lot number, control level (Low/Normal/High), associated test and analyzer, manufacturer mean and standard deviation (if applicable), activation dates, establishment method, and current status.

- **QC Result**: Represents a single quality control measurement. Attributes include: reference to control lot, test and analyzer, measured value, unit of measure, run timestamp, technician who performed the test, status (pending/accepted/rejected), non-conformity flag, and external notes.

- **QC Statistics**: Represents calculated statistical parameters for a control lot. Attributes include: reference to control lot, calculation date, mean, standard deviation, number of values used in calculation, calculation method, and validity period.

- **Westgard Rule Configuration**: Represents the active rule set for a test-analyzer combination. Attributes include: test and analyzer references, rule code, enabled status, severity level, whether corrective action is required, and configuration timestamps.

- **QC Rule Violation**: Represents a detected violation of one or more Westgard rules. Attributes include: triggering QC result, rule code(s) violated, violation timestamp, severity, related QC results involved in the violation, resolution status, linked corrective action, and resolution details.

- **QC Corrective Action**: Represents an action taken to address a violation. Attributes include: reference to violation, action type, description, assigned user, creating user, creation and completion timestamps, status, and resolution notes.

- **QC Alert**: Represents a notification sent about a violation. Attributes include: reference to violation, alert type (email/system), recipient, sent timestamp, read status, and read timestamp.

- **Analyzer**: Entity representing laboratory analyzer equipment, managed by feature 004 (ASTM Analyzer Field Mapping). QCResult entities MUST link to Analyzer.id via foreign key qc_result.analyzer_id. Note: This is the Analyzer entity from feature 004, NOT the legacy Instrument entity. QCControlLot also links to Analyzer to associate control materials with specific analyzers.

- **Test**: Existing entity representing laboratory test types. Must be linked to QC control lots and rule configurations.

- **User**: Existing entity representing system users. Must be linked to QC results (performing technician), corrective actions (assigned user), and alerts (recipients).

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory technicians can identify non-compliant analyzers within 10 seconds of viewing the dashboard
- **SC-002**: All QC results received from feature 004's ASTM processing are evaluated within 5 seconds of QCResultService.createQCResult() invocation. Note: ASTM message capture and field mapping are handled by feature 004 (see 004:FR-021); 003's responsibility begins when QCResult entity is persisted.
- **SC-003**: Lab managers receive automated alerts for rejection-level violations within 30 seconds of detection
- **SC-004**: 95% of Westgard rule evaluations complete without errors when sufficient historical data is available
- **SC-005**: Users can configure a complete rule set for a test-analyzer combination in under 5 minutes
- **SC-006**: System maintains 99.9% uptime for QC result capture and rule evaluation
- **SC-007**: Levey-Jennings charts render within 3 seconds for up to 100 data points
- **SC-008**: Dashboard supports at least 50 analyzers without performance degradation
- **SC-009**: System correctly identifies all 8 Westgard rule types with less than 1% false positive/negative rate when tested against validated reference datasets
- **SC-010**: 100% of corrective actions are traceable to their originating violations in audit trails
- **SC-011**: Users can export compliance reports for regulatory audits in under 30 seconds
- **SC-012**: System prevents release of patient results from analyzers with unresolved rejection-level violations in 100% of cases
- **SC-013**: Alert batching reduces notification volume by at least 50% while maintaining all critical alerts as immediate
- **SC-014**: System handles minimum 1,000 QC results per day with all automated evaluations completing successfully
- **SC-015**: QC data is retained and accessible for minimum 2 years for regulatory compliance

### Qualitative Outcomes

- **SC-016**: Laboratory meets CLIA and CAP quality control documentation requirements as verified by successful audits
- **SC-017**: Laboratory staff report increased confidence in analyzer quality control compared to manual chart review
- **SC-018**: Reduction in patient sample reruns due to earlier detection of analyzer quality issues
- **SC-019**: Time to identify and resolve quality control issues decreases compared to baseline manual processes
