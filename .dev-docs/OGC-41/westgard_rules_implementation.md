# Westgard Rules Implementation - Requirements & Approach
## OpenELIS Global Lab Management System

**Document Version:** 1.0  
**Date:** November 13, 2025  
**Author:** Implementation Planning Team

---

## Table of Contents

1. [Functional Requirements](#functional-requirements)
2. [Non-Functional Requirements](#non-functional-requirements)
3. [Implementation Approach](#implementation-approach)
4. [Risk Mitigation Strategies](#risk-mitigation-strategies)
5. [Success Criteria](#success-criteria)
6. [Recommended Implementation Order](#recommended-implementation-order)

---

## Functional Requirements

### FR1: QC Control Lot Management

- **FR1.1**: System shall support creation of QC control lots with manufacturer information
- **FR1.2**: System shall support multiple control levels (Low, Normal, High) per lot
- **FR1.3**: System shall track lot activation/deactivation dates
- **FR1.4**: System shall associate control lots with specific test-instrument combinations
- **FR1.5**: System shall support three statistical calculation methods:
  - Initial establishment (first N runs, configurable, default 20)
  - Rolling calculation (moving window)
  - Fixed manufacturer values
- **FR1.6**: System shall prevent use of lots before sufficient data for statistical establishment
- **FR1.7**: System shall require new statistical calculations when control lot changes

### FR2: QC Result Capture

- **FR2.1**: System shall receive QC results from ASTM analyzer interface
- **FR2.2**: System shall map ASTM data to appropriate control lot/level/test/instrument
- **FR2.3**: System shall timestamp all QC results with run date/time
- **FR2.4**: System shall link results to the technician who performed the test
- **FR2.5**: System shall store result value and unit of measure
- **FR2.6**: System shall support manual result entry as fallback

### FR3: Westgard Rule Configuration

- **FR3.1**: System shall allow configuration of rules per test-instrument combination
- **FR3.2**: System shall support enabling/disabling individual rules
- **FR3.3**: System shall support the following Westgard rules:
  - **1₂ₛ**: Single control exceeds ±2SD (warning)
  - **1₃ₛ**: Single control exceeds ±3SD (rejection)
  - **2₂ₛ**: Two consecutive controls exceed same ±2SD (rejection)
  - **R₄ₛ**: Range between two controls exceeds 4SD (rejection)
  - **4₁ₛ**: Four consecutive controls exceed same ±1SD (rejection)
  - **10ₓ**: Ten consecutive controls on same side of mean (rejection)
  - **3₁ₛ**: Three consecutive controls exceed same ±1SD (warning)
  - **7ₜ**: Seven consecutive controls showing trend (warning)
- **FR3.4**: System shall classify rules as WARNING or REJECTION severity
- **FR3.5**: System shall allow configuration of whether corrective action is required per rule
- **FR3.6**: System shall provide default multi-rule configuration (1₃ₛ/2₂ₛ/R₄ₛ/4₁ₛ)

### FR4: Automatic Rule Evaluation

- **FR4.1**: System shall automatically evaluate enabled rules when QC result is entered
- **FR4.2**: System shall calculate z-scores for each result (z = (value - mean) / SD)
- **FR4.3**: System shall retrieve historical results needed for sequential rules
- **FR4.4**: System shall create violation records when rules are triggered
- **FR4.5**: System shall link all related QC results to the violation
- **FR4.6**: System shall apply non-conformity flag to results causing rejection
- **FR4.7**: System shall continue processing even if some rules cannot be evaluated (insufficient data)

### FR5: Manual Rule Evaluation

- **FR5.1**: System shall provide on-demand rule evaluation for historical data ranges
- **FR5.2**: System shall allow re-evaluation after statistics recalculation
- **FR5.3**: System shall allow evaluation of specific rule subsets
- **FR5.4**: System shall display evaluation results without creating violations (preview mode)

### FR6: Violation Management

- **FR6.1**: System shall record all rule violations with timestamp
- **FR6.2**: System shall track violation resolution status
- **FR6.3**: System shall require corrective action for rejection-level violations
- **FR6.4**: System shall allow warning-level violations to be acknowledged without corrective action
- **FR6.5**: System shall prevent violation deletion (audit requirement)
- **FR6.6**: System shall link violations to corrective actions

### FR7: Corrective Action Workflow

- **FR7.1**: System shall provide corrective action types:
  - Recalibration
  - Maintenance
  - Repeat Control
  - Reagent Change
  - Other (with description)
- **FR7.2**: System shall allow assignment of corrective actions to users
- **FR7.3**: System shall track corrective action status (Pending, In Progress, Completed)
- **FR7.4**: System shall require resolution notes upon completion
- **FR7.5**: System shall add external note to flagged QC results with corrective action summary
- **FR7.6**: System shall automatically resolve violations when corrective action is completed
- **FR7.7**: System shall prevent result release for associated patient samples until violation resolved

### FR8: Dashboard & Visualization

- **FR8.1**: System shall display overview dashboard with compliance status
- **FR8.2**: System shall use color-coded indicators:
  - Green: All rules compliant
  - Yellow: Warning-level violations present
  - Red: Rejection-level violations present
- **FR8.3**: System shall display instrument cards showing:
  - Current compliance status
  - List of triggered rules with severity
  - Date/time of last QC result analyzed
  - Count of unresolved violations
- **FR8.4**: System shall make instrument cards clickable for detailed view
- **FR8.5**: Dashboard shall auto-refresh on configurable interval (default: 5 minutes)

### FR9: Levey-Jennings Charts

- **FR9.1**: System shall display interactive Levey-Jennings (L-J) charts per control lot
- **FR9.2**: Charts shall plot QC results chronologically
- **FR9.3**: Charts shall overlay the following lines:
  - Mean (solid green)
  - ±1SD (dashed yellow)
  - ±2SD (dashed orange)
  - ±3SD (solid red)
- **FR9.4**: Charts shall highlight points violating rules:
  - Different colors per rule type
  - Increased point size for violated points
  - Tooltip showing rule code and violation details
- **FR9.5**: Charts shall support date range filtering
- **FR9.6**: Charts shall support zoom and pan functionality
- **FR9.7**: Charts shall display multiple control levels in separate subplots or tabs
- **FR9.8**: Charts shall allow printing and export (PDF, PNG)

### FR10: Trend Analysis

- **FR10.1**: System shall provide compliance trend graphs over time
- **FR10.2**: Trend graphs shall support filtering by:
  - Date range
  - Instrument
  - Test
  - Control level
  - Rule type
  - Severity
- **FR10.3**: System shall calculate and display compliance percentage over time
- **FR10.4**: System shall show violation frequency distribution by rule type
- **FR10.5**: System shall identify instruments with recurring violations

### FR11: Alert & Notification System

- **FR11.1**: System shall generate real-time alerts when rules are violated
- **FR11.2**: System shall support two notification channels:
  - Email notifications
  - In-system notifications
- **FR11.3**: System shall allow users to configure notification preferences per rule severity
- **FR11.4**: System shall maintain real-time alert feed on dashboard
- **FR11.5**: System shall track notification read status
- **FR11.6**: System shall send alerts to:
  - Technician who ran the QC
  - Assigned QC supervisor/manager
  - Users subscribed to specific instruments
- **FR11.7**: Email alerts shall include:
  - Instrument and test identification
  - Rule violated
  - QC result value and z-score
  - Link to detailed view
  - Corrective action status
- **FR11.8**: System shall batch notifications (max one per instrument per 15 minutes) to prevent alert fatigue

### FR12: Reporting

- **FR12.1**: System shall provide violation history log with filtering/sorting
- **FR12.2**: System shall generate summary reports showing:
  - Violations by instrument
  - Violations by rule type
  - Violations by time period
  - Corrective action completion rate
  - Mean time to resolution
- **FR12.3**: System shall export reports to PDF and CSV
- **FR12.4**: System shall maintain audit trail of all violations and actions

### FR13: Access Control

- **FR13.1**: System shall restrict access to "Results", "Biologist", and "Global Admin" roles
- **FR13.2**: "Results" role shall have:
  - View QC results and charts
  - Create corrective actions
  - View violations
- **FR13.3**: "Biologist" role shall have:
  - All "Results" permissions
  - Configure rules
  - Manage control lots
  - Resolve violations
- **FR13.4**: "Global Admin" role shall have full access including system configuration

---

## Non-Functional Requirements

### NFR1: Performance

- **NFR1.1**: Rule evaluation shall complete within 2 seconds per result
- **NFR1.2**: Dashboard shall load within 3 seconds
- **NFR1.3**: L-J charts shall render within 2 seconds for up to 100 data points
- **NFR1.4**: System shall support concurrent evaluation of 50 QC results

### NFR2: Data Integrity

- **NFR2.1**: All statistical calculations shall be auditable
- **NFR2.2**: System shall prevent manual modification of calculated statistics
- **NFR2.3**: System shall maintain complete history of all violations
- **NFR2.4**: System shall log all rule configuration changes

### NFR3: Scalability

- **NFR3.1**: System shall support minimum 100 instruments
- **NFR3.2**: System shall handle minimum 1000 QC results per day
- **NFR3.3**: System shall retain QC data for minimum 2 years

### NFR4: Usability

- **NFR4.1**: Interface shall follow Carbon Design System guidelines
- **NFR4.2**: All data entry forms shall provide inline validation
- **NFR4.3**: Charts shall be accessible (WCAG 2.1 AA compliant)
- **NFR4.4**: System shall provide contextual help for Westgard rules

### NFR5: Integration

- **NFR5.1**: System shall integrate with existing ASTM interface
- **NFR5.2**: System shall not modify existing ASTM data flow
- **NFR5.3**: System shall handle ASTM interface unavailability gracefully

### NFR6: Regulatory Compliance

- **NFR6.1**: System shall comply with CLIA QC requirements
- **NFR6.2**: System shall support CAP inspection requirements
- **NFR6.3**: System shall maintain 21 CFR Part 11 compliant audit trails

---

## Implementation Approach

### Phase 1: Foundation (Weeks 1-3)

#### 1.1 Data Model Implementation

**Approach**: Database-first design

**Tasks**:
- Create all tables with proper foreign key relationships
- Implement indexes on frequently queried fields:
  - `qc_result.run_date_time`
  - `qc_result.qc_control_lot_id`
  - `qc_rule_violation.resolved`
  - `qc_result.instrument_id, test_id`
- Add database constraints to ensure data integrity
- Create views for common queries (active violations, current statistics)

**Key Decision**: Use PostgreSQL JSON columns for `related_qc_result_ids` to avoid complex join tables while maintaining query flexibility

**Database Tables**:

```sql
-- QC Control Lot Management
qc_control_lot:
  - id
  - control_product_name
  - lot_number
  - level (LOW, NORMAL, HIGH)
  - test_id (FK to test)
  - instrument_id (FK to instrument)
  - manufacturer_mean (optional)
  - manufacturer_sd (optional)
  - active_from_date
  - active_to_date
  - status (ESTABLISHMENT, ACTIVE, INACTIVE)
  - establishment_method (INITIAL_RUNS, ROLLING, FIXED_MANUFACTURER)
  - establishment_n (default 20 for initial runs)
  - created_by
  - created_date

-- QC Results
qc_result:
  - id
  - qc_control_lot_id (FK)
  - test_id (FK)
  - instrument_id (FK)
  - result_value
  - unit_of_measure
  - run_date_time
  - technician_id (FK)
  - status (PENDING, ACCEPTED, REJECTED)
  - non_conformity_flag (boolean)
  - external_note
  - created_date

-- Calculated Statistics (cached for performance)
qc_statistics:
  - id
  - qc_control_lot_id (FK)
  - calculation_date
  - mean
  - standard_deviation
  - n_count (number of values used)
  - calculation_method
  - valid_from_date
  - valid_to_date

-- Westgard Rule Configuration
westgard_rule_config:
  - id
  - test_id (FK)
  - instrument_id (FK)
  - rule_code (1_2S, 1_3S, 2_2S, R_4S, 4_1S, 10_X, 3_1S, 7_T)
  - enabled (boolean)
  - severity (WARNING, REJECTION)
  - requires_corrective_action (boolean)
  - created_by
  - created_date
  - modified_date

-- Rule Violations
qc_rule_violation:
  - id
  - qc_result_id (FK) - the triggering result
  - rule_code
  - violation_date_time
  - severity
  - related_qc_result_ids (JSON array of other results involved)
  - resolved (boolean)
  - resolved_date_time
  - corrective_action_id (FK)
  - resolved_by (FK to user)

-- Corrective Actions
qc_corrective_action:
  - id
  - qc_rule_violation_id (FK)
  - action_type (RECALIBRATION, MAINTENANCE, REPEAT_CONTROL, REAGENT_CHANGE, OTHER)
  - description
  - assigned_to (FK to user)
  - created_by (FK to user)
  - created_date
  - completed_date
  - status (PENDING, IN_PROGRESS, COMPLETED)
  - resolution_notes

-- Notification/Alert Log
qc_alert:
  - id
  - qc_rule_violation_id (FK)
  - alert_type (EMAIL, SYSTEM_NOTIFICATION)
  - recipient_user_id (FK)
  - sent_date_time
  - read (boolean)
  - read_date_time
```

#### 1.2 Statistical Engine

**Approach**: Service-oriented with strategy pattern

```
StatisticsCalculationStrategy (interface)
├── InitialRunsStrategy (first N runs)
├── RollingWindowStrategy (moving average)
└── ManufacturerFixedStrategy (use provided values)
```

**Implementation Steps**:
1. Create `QCStatisticsService` with pluggable strategies
2. Implement each calculation strategy independently
3. Add validation to ensure minimum data points (n=20 for initial, n=2 for rolling)
4. Cache calculated statistics in `qc_statistics` table
5. Implement automatic recalculation trigger when new result added
6. Add "establishment mode" flag to prevent rule evaluation until statistics are stable

**Key Decision**: Cache statistics rather than calculating on-the-fly to improve performance. Recalculate asynchronously when new results arrive.

**Service Layer Architecture**:

```java
// Core Services

1. QCControlLotService
   - createControlLot()
   - updateControlLot()
   - getActiveControlLot(testId, instrumentId, level)
   - deactivateControlLot()

2. QCResultService
   - recordQCResult(resultData) // from ASTM interface
   - getQCResultsByLot(lotId, dateRange)
   - updateResultStatus()
   - addNonConformityNote()

3. QCStatisticsService
   - calculateStatistics(lotId, method)
   - recalculateStatistics(lotId) // when new data added
   - getStatisticsForLot(lotId)
   Methods:
   - calculateInitialRunStatistics() // first N runs
   - calculateRollingStatistics(windowSize) // moving window
   - useManufacturerStatistics() // fixed values

4. WestgardRuleEngine
   - evaluateRules(qcResultId)
   - evaluateSpecificRule(ruleCode, qcResultId)
   - getRuleConfiguration(testId, instrumentId)
   Methods for each rule:
   - evaluate_1_2S()
   - evaluate_1_3S()
   - evaluate_2_2S()
   - evaluate_R_4S()
   - evaluate_4_1S()
   - evaluate_10_X()
   - evaluate_3_1S()
   - evaluate_7_T()

5. WestgardRuleConfigService
   - configureRulesForTestInstrument()
   - enableRule() / disableRule()
   - getRuleConfiguration()

6. QCViolationService
   - createViolation()
   - resolveViolation()
   - getUnresolvedViolations()
   - getViolationHistory()

7. QCCorrectiveActionService
   - createCorrectiveAction()
   - assignCorrectiveAction()
   - completeCorrectiveAction()
   - getActiveCorrectiveActions()

8. QCAlertService
   - sendAlerts(violationId)
   - getAlertsForUser()
   - markAlertAsRead()
```

---

### Phase 2: Rule Engine Core (Weeks 4-6)

#### 2.1 Rule Evaluation Architecture

**Approach**: Chain of Responsibility pattern with individual rule evaluators

```
WestgardRuleEngine
├── Rule_1_2S_Evaluator
├── Rule_1_3S_Evaluator
├── Rule_2_2S_Evaluator
├── Rule_R_4S_Evaluator
├── Rule_4_1S_Evaluator
├── Rule_10_X_Evaluator
├── Rule_3_1S_Evaluator
└── Rule_7_T_Evaluator
```

**Implementation Strategy**:

Each evaluator implements `WestgardRuleEvaluator` interface:

```java
interface WestgardRuleEvaluator {
    RuleEvaluationResult evaluate(QCResult currentResult, 
                                   List<QCResult> historicalResults,
                                   QCStatistics statistics);
    String getRuleCode();
    int getRequiredHistoricalCount();
}
```

**Main engine orchestrates**:
1. Fetch active rule configuration for test/instrument
2. Retrieve historical results based on maximum lookback needed
3. Call each enabled evaluator
4. Collect and persist violations
5. Return aggregated results

**Key Decisions**:
- **Sequential Rules Priority**: Evaluate simple rules first (1₃ₛ) before complex sequential rules (10ₓ) for early exit
- **Historical Data Fetch**: Single query to get last 10 results (maximum needed for 10ₓ rule) rather than multiple queries per rule
- **Stateless Evaluators**: Each evaluator is stateless and thread-safe for concurrent execution

#### 2.2 Individual Rule Logic

**Simple Point Rules** (1₂ₛ, 1₃ₛ):
```
1. Calculate z-score: (value - mean) / SD
2. Compare absolute z-score to threshold
3. If violated, return violation with single result ID
```

**Two-Point Sequential Rules** (2₂ₛ, R₄ₛ):
```
1. Get previous result for same control lot/level
2. For 2₂ₛ: Check if both z-scores exceed 2.0 on same side
3. For R₄ₛ: Calculate range and convert to SD units
4. If violated, return violation with both result IDs
```

**Multi-Point Sequential Rules** (4₁ₛ, 10ₓ):
```
1. Fetch last N results (4 for 4₁ₛ, 10 for 10ₓ)
2. Check if count is sufficient
3. For 4₁ₛ: Verify all 4 exceed 1SD on same side
4. For 10ₓ: Verify all 10 on same side of mean
5. If violated, return violation with all N result IDs
```

**Trend Rules** (7ₜ):
```
1. Fetch last 7 results
2. Calculate differences between consecutive points
3. Check if all differences have same sign (all increasing or all decreasing)
4. If violated, return violation with all 7 result IDs
```

**Edge Cases to Handle**:
- Insufficient historical data: Skip rule, log info message
- Missing statistics: Skip all rules, log warning
- Control lot changed mid-sequence: Only use results from current lot
- Multiple control levels: Evaluate rules independently per level

#### 2.3 Automatic vs Manual Evaluation

**Automatic Evaluation**:
- **Trigger**: `@Async` listener on QC result creation
- **Process**:
  1. Validate result has valid control lot with statistics
  2. Call `WestgardRuleEngine.evaluateAllActiveRules()`
  3. Create violation records
  4. Apply non-conformity flags
  5. Trigger alert service
- **Timeout**: 30 seconds max
- **Failure handling**: Log error, notify admin, don't block result entry

**Manual Evaluation**:
- **Use case**: Recalculating after statistics update or configuration change
- **Endpoint**: `POST /api/qc/evaluate-range`
- **Process**:
  1. Fetch all results in date range
  2. Clear existing violations for range (optional)
  3. Evaluate each result sequentially
  4. Return summary without sending alerts
- **Preview mode**: Evaluate without persisting violations

---

### Phase 3: User Interface (Weeks 7-10)

#### 3.1 Dashboard Implementation

**Approach**: Real-time update with WebSocket for alerts, polling for statistics

**Component Hierarchy**:
```
<QCWestgardModule>
  ├── <QCDashboard> (main view)
  │   ├── <ComplianceOverview> (summary tiles)
  │   └── <InstrumentGrid>
  │       └── <InstrumentCard> × N
  ├── <InstrumentDetailView> (modal/page)
  │   ├── <InstrumentHeader>
  │   ├── <ViolationSummary>
  │   ├── <LeveyJenningsChart>
  │   └── <RecentResults>
  └── <ConfigurationPanel>
      ├── <ControlLotManager>
      └── <RuleConfigurationTable>
```

**Component Structure**:
```
/components/qc-westgard/
  ├── QCDashboard/
  │   ├── QCDashboard.jsx (main overview)
  │   ├── ComplianceStatusCard.jsx
  │   └── InstrumentComplianceCard.jsx
  ├── QCControlCharts/
  │   ├── LeveyJenningsChart.jsx
  │   ├── WestgardOverlay.jsx
  │   └── ChartLegend.jsx
  ├── QCConfiguration/
  │   ├── ControlLotSetup.jsx
  │   ├── RuleConfiguration.jsx
  │   └── StatisticsMethodSelector.jsx
  ├── QCViolations/
  │   ├── ViolationsList.jsx
  │   ├── ViolationDetailModal.jsx
  │   └── CorrectiveActionForm.jsx
  ├── QCAlerts/
  │   ├── AlertFeed.jsx
  │   └── AlertNotification.jsx
  └── QCReports/
      ├── ComplianceTrendChart.jsx
      └── ViolationHistoryTable.jsx
```

**State Management**:
- Use React Context for global QC state (active violations, instrument status)
- Local component state for chart interactions
- API polling every 5 minutes for dashboard refresh
- WebSocket connection for real-time violation alerts

**Key Features**:
- **Optimistic Updates**: Show result immediately, mark as pending until rule evaluation completes
- **Skeleton Loading**: Show placeholders while loading data
- **Error Boundaries**: Graceful degradation if chart fails to render

#### 3.2 Levey-Jennings Chart Implementation

**Approach**: Use Carbon Charts (based on D3.js) with custom overlays

**Technical Approach**:

1. **Data Preparation**:
   ```javascript
   // Transform QC results into chart dataset
   const chartData = {
     results: qcResults.map(r => ({x: r.runNumber, y: r.value, ...})),
     mean: statistics.mean,
     limits: {
       plus3SD: mean + (3 * sd),
       plus2SD: mean + (2 * sd),
       // ...etc
     }
   };
   ```

2. **Chart Layers**:
   - Base scatter plot for QC results
   - Horizontal reference lines for SD limits
   - Shaded regions for control zones (optional)
   - Annotations for violated points

3. **Interaction**:
   - Hover tooltips showing result details and z-score
   - Click to open result detail modal
   - Brush selection for zoom
   - Legend toggle to show/hide violated points

4. **Violation Highlighting**:
   ```javascript
   const getPointStyle = (result) => {
     const violation = violations.find(v => v.qcResultIds.includes(result.id));
     if (!violation) return defaultStyle;
     
     return {
       radius: 6,
       fillColor: violation.severity === 'REJECTION' ? 'red' : 'yellow',
       strokeColor: 'black',
       strokeWidth: 2
     };
   };
   ```

**Performance Optimization**:
- Limit chart to 100 most recent points by default
- Implement virtual scrolling for larger datasets
- Debounce zoom/pan operations
- Use Canvas rendering for >200 points instead of SVG

#### 3.3 Configuration Interface

**Approach**: Form-based with validation

**Control Lot Setup Flow**:
1. Select test and instrument
2. Enter lot details (number, level, dates)
3. Choose statistical method:
   - Initial runs: Specify N (default 20)
   - Rolling: Specify window size
   - Manufacturer: Enter mean and SD
4. Activate lot (if sufficient data or using manufacturer values)

**Rule Configuration Flow**:
1. Select test and instrument combination
2. Display table of all 8 Westgard rules
3. For each rule:
   - Toggle enabled/disabled
   - Display severity (read-only, based on rule definition)
   - Checkbox for "requires corrective action"
4. Provide preset configurations:
   - Basic (1₃ₛ only)
   - Standard (1₃ₛ/2₂ₛ/R₄ₛ/4₁ₛ)
   - Comprehensive (all rules)
5. Save configuration

**Validation**:
- At least one rule must be enabled
- Cannot disable 1₃ₛ if no other rejection rules are enabled
- Warn if no warning rules are enabled

---

### Phase 4: Violation Management (Weeks 11-13)

#### 4.1 Violation Workflow

**State Machine**:
```
[New Violation Created]
    ↓
[Unresolved] ──(acknowledge)──→ [Acknowledged] (if WARNING)
    ↓
    (require CA)
    ↓
[CA Pending] ──(assign)──→ [CA Assigned]
    ↓
    (complete)
    ↓
[CA Completed] ──(resolve)──→ [Resolved]
```

**Implementation**:
- Use database triggers or application events to enforce state transitions
- Prevent patient result release if related QC has unresolved rejection violations
- Link patient results to QC results via batch/run timestamp

#### 4.2 Corrective Action Module

**Components**:
- `<ViolationsList>`: Filterable table of all violations
- `<ViolationDetailModal>`: Shows rule details, affected results, L-J chart context
- `<CorrectiveActionForm>`: Create/edit corrective action
- `<CorrectiveActionTaskList>`: Assigned tasks for current user

**Corrective Action Types**:

Each type has specific form fields:
- **Recalibration**: Calibrator lot, before/after results
- **Maintenance**: Service performed, parts replaced
- **Repeat Control**: New control lot used, repeat results
- **Reagent Change**: Old/new reagent lot numbers
- **Other**: Free text description

**Notification Flow**:
1. Violation created → Notify technician + QC supervisor
2. CA assigned → Notify assigned user
3. CA completed → Notify QC supervisor for review
4. Violation resolved → Update external note on QC result

---

### Phase 5: Alerting & Notifications (Weeks 14-15)

#### 5.1 Alert Service Architecture

**Approach**: Async event-driven with configurable routing

```
ViolationEvent (published)
    ↓
AlertRouter (subscribes)
    ├──> EmailAlertService
    │      └─> Spring Mail / SMTP
    └──> SystemNotificationService
           └─> WebSocket broadcast
```

**Alert Batching Strategy**:
- Collect violations for same instrument within 15-minute window
- Send single digest email rather than multiple
- Exception: Always send immediate alert for 1₃ₛ violations

**User Preferences**:
```
User Alert Preferences:
- Email for: [REJECTION only / WARNING + REJECTION / None]
- System notifications for: [All / REJECTION only / None]
- Subscribed instruments: [List of instrument IDs]
- Quiet hours: [Start time - End time]
```

#### 5.2 Real-Time Alert Feed

**Implementation**:
- WebSocket endpoint: `/ws/qc-alerts`
- Push new violations to connected clients
- Client maintains alert queue (last 50 alerts)
- Mark-as-read functionality updates server state

**Alert Feed UI**:
- Persistent notification panel (Carbon Notification component)
- Toast notifications for new alerts
- Click to navigate to violation detail
- Filter by severity and instrument

---

### Phase 6: Reporting & Analytics (Weeks 16-17)

#### 6.1 Trend Analysis

**Metrics to Track**:
- Violation rate per instrument (violations / total runs)
- Rule distribution (which rules trigger most often)
- Time to resolution (average, by instrument)
- Repeat violations (same rule within 24 hours)

**Visualization**:
- Line chart: Violation count over time
- Bar chart: Violations by instrument
- Pie chart: Violations by rule type
- Heat map: Violations by day-of-week and hour

**Implementation**:
- Pre-aggregate data in nightly batch job
- Store in `qc_analytics_summary` table
- Refresh on-demand for custom date ranges

#### 6.2 Compliance Reporting

**Standard Reports**:
1. **Daily QC Summary**: All instruments run that day with pass/fail status
2. **Monthly Compliance Report**: Violation statistics, trending issues
3. **Instrument Performance Report**: Single instrument detail over time period
4. **Corrective Action Report**: All CAs with completion status

**Export Formats**:
- PDF: Use iText or Apache PDFBox
- CSV: Simple table export for Excel import
- Include charts as embedded images in PDF

---

### Phase 7: Integration & Testing (Weeks 18-20)

#### 7.1 ASTM Interface Integration

**Approach**: Listener pattern with mapping service

**Integration Points**:
1. ASTM message received
2. `QCResultMappingService` extracts:
   - Instrument ID (from ASTM sender)
   - Test code (map to internal test ID)
   - QC result value
   - Control lot/level identifier
3. `QCResultService.recordQCResult()` creates result
4. Automatic rule evaluation triggered

**Error Handling**:
- Unknown instrument: Log error, queue for manual review
- Unknown test: Attempt fuzzy match, fallback to manual
- Unknown control lot: Check if new lot needs setup
- Invalid result value: Reject ASTM message, notify tech

#### 7.2 Testing Strategy

**Unit Tests** (80% coverage target):
- Each rule evaluator with comprehensive test cases:
  - Rule triggered correctly
  - Rule not triggered for near-miss values
  - Insufficient data handled gracefully
  - Edge cases (exact threshold values)
- Statistical calculations verified against known datasets
- Validation logic for all input forms

**Integration Tests**:
- End-to-end flows:
  - QC result entry → rule evaluation → violation creation → alert sent
  - Corrective action workflow → violation resolution
  - Statistics calculation → rule re-evaluation
- ASTM interface integration with mock analyzer

**Performance Tests**:
- Load test: 50 concurrent QC result submissions
- Stress test: 1000 results in rapid succession
- Dashboard rendering with 100 instruments
- Chart rendering with 200 data points

**User Acceptance Testing**:
- Lab technician workflows
- QC supervisor workflows
- Configuration scenarios
- Report generation and export

---

## Risk Mitigation Strategies

### Risk 1: Complex Sequential Rule Logic

**Mitigation**:
- Implement simplest rules first (1₂ₛ, 1₃ₛ)
- Create comprehensive test suite with known datasets
- Validate against published Westgard examples
- Peer review rule implementation code

### Risk 2: Performance with Large Datasets

**Mitigation**:
- Cache statistics calculations
- Index all foreign keys and date fields
- Implement pagination on all list views
- Use async processing for rule evaluation
- Monitor query performance, optimize slow queries

### Risk 3: User Confusion about Rules

**Mitigation**:
- Provide in-app help documentation
- Include visual examples of each rule
- Offer training mode with sample data
- Create preset configurations for common scenarios

### Risk 4: ASTM Integration Issues

**Mitigation**:
- Build robust mapping service with manual fallback
- Implement comprehensive error logging
- Create admin interface for mapping management
- Test with multiple analyzer types before full rollout

### Risk 5: Alert Fatigue

**Mitigation**:
- Implement alert batching
- Allow user preference configuration
- Differentiate severity levels clearly
- Provide "snooze" functionality for known issues

---

## Success Criteria

1. **Functional Completeness**: All 8 Westgard rules correctly identify violations with <1% false positive/negative rate
2. **Performance**: Rule evaluation completes in <2 seconds for 95% of results
3. **Usability**: Users can configure rules without training in <10 minutes
4. **Adoption**: 80% of labs using system within 3 months of release
5. **Compliance**: Zero CAP audit findings related to QC documentation
6. **Reliability**: 99.5% uptime for rule evaluation service

---

## Recommended Implementation Order

### MVP (Weeks 1-10)
Core functionality to demonstrate value and gather user feedback:
- Database schema and data model
- Statistical calculation engine
- Core rule engine (all 8 rules)
- Basic dashboard with compliance indicators
- Levey-Jennings charts
- Manual corrective action entry
- Basic violation tracking

**Deliverables**:
- Functional rule evaluation system
- Visual QC compliance dashboard
- Interactive L-J charts
- Basic violation workflow

### Phase 2 (Weeks 11-15)
Enhanced workflow and automation:
- Automated workflow for violation resolution
- Alert and notification system
- Real-time alert feed
- Advanced violation management
- Corrective action assignment and tracking

**Deliverables**:
- Automated alerting system
- Complete violation management workflow
- Email and in-system notifications
- Task assignment capabilities

### Phase 3 (Weeks 16-20)
Advanced features and polish:
- Analytics and trend reporting
- Compliance reports with export
- ASTM interface integration
- Comprehensive testing
- Documentation and training materials

**Deliverables**:
- Full reporting suite
- Seamless ASTM integration
- Production-ready system
- User documentation

---

## API Endpoints Reference

### QC Control Lots
```
POST   /api/qc/control-lots                    - Create control lot
PUT    /api/qc/control-lots/{id}               - Update control lot
GET    /api/qc/control-lots/{id}               - Get control lot details
GET    /api/qc/control-lots                    - List control lots (with filters)
DELETE /api/qc/control-lots/{id}               - Deactivate control lot
```

### QC Results
```
POST   /api/qc/results                         - Create QC result (from ASTM or manual)
GET    /api/qc/results/{id}                    - Get result details
GET    /api/qc/results                         - List results (with filters)
PUT    /api/qc/results/{id}/status             - Update result status
POST   /api/qc/results/{id}/note               - Add external note
```

### Statistics
```
GET    /api/qc/statistics/{lotId}              - Get current statistics for lot
POST   /api/qc/statistics/{lotId}/recalculate  - Trigger statistics recalculation
GET    /api/qc/statistics/{lotId}/history      - Get statistics history
```

### Rule Configuration
```
GET    /api/qc/rules/config                    - Get rule configuration (by test/instrument)
PUT    /api/qc/rules/config                    - Update rule configuration
POST   /api/qc/rules/config/preset             - Apply preset configuration
GET    /api/qc/rules/definitions               - Get all rule definitions
```

### Rule Evaluation
```
POST   /api/qc/evaluate                        - Manual evaluation (preview or persist)
POST   /api/qc/evaluate/range                  - Evaluate date range
GET    /api/qc/evaluate/{resultId}             - Get evaluation results for result
```

### Violations
```
GET    /api/qc/violations                      - List violations (with filters)
GET    /api/qc/violations/{id}                 - Get violation details
PUT    /api/qc/violations/{id}/resolve         - Resolve violation
GET    /api/qc/violations/unresolved           - Get unresolved violations
GET    /api/qc/violations/dashboard            - Get dashboard summary
```

### Corrective Actions
```
POST   /api/qc/corrective-actions              - Create corrective action
PUT    /api/qc/corrective-actions/{id}         - Update corrective action
GET    /api/qc/corrective-actions/{id}         - Get corrective action details
GET    /api/qc/corrective-actions              - List corrective actions
PUT    /api/qc/corrective-actions/{id}/assign  - Assign to user
PUT    /api/qc/corrective-actions/{id}/complete - Mark as completed
```

### Alerts & Notifications
```
GET    /api/qc/alerts                          - Get alerts for current user
PUT    /api/qc/alerts/{id}/read                - Mark alert as read
PUT    /api/qc/alerts/read-all                 - Mark all alerts as read
GET    /api/qc/alerts/preferences              - Get user notification preferences
PUT    /api/qc/alerts/preferences              - Update notification preferences
```

### Charts & Visualization
```
GET    /api/qc/charts/levey-jennings           - Get L-J chart data
GET    /api/qc/charts/trend                    - Get compliance trend data
GET    /api/qc/charts/violations-by-rule       - Get violation distribution
```

### Reports
```
GET    /api/qc/reports/daily-summary           - Daily QC summary
GET    /api/qc/reports/monthly-compliance      - Monthly compliance report
GET    /api/qc/reports/instrument-performance  - Instrument performance report
POST   /api/qc/reports/export                  - Export report (PDF/CSV)
```

### WebSocket
```
WS     /ws/qc-alerts                           - Real-time alert feed
```

---

## Appendix A: Westgard Rules Reference

### Rule 1₂ₛ (Warning)
**Description**: Single control exceeds ±2 standard deviations  
**Interpretation**: Possible random error or warning of systematic error  
**Action**: Monitor closely, consider repeat  
**Severity**: Warning

### Rule 1₃ₛ (Rejection)
**Description**: Single control exceeds ±3 standard deviations  
**Interpretation**: Random error or major systematic error  
**Action**: Reject run, investigate and correct before reporting patient results  
**Severity**: Rejection

### Rule 2₂ₛ (Rejection)
**Description**: Two consecutive controls exceed the same ±2 standard deviations  
**Interpretation**: Systematic error  
**Action**: Reject run, check for calibration or reagent issues  
**Severity**: Rejection

### Rule R₄ₛ (Rejection)
**Description**: Range between two consecutive controls exceeds 4 standard deviations  
**Interpretation**: Random error (increased imprecision)  
**Action**: Reject run, check for technical issues affecting precision  
**Severity**: Rejection

### Rule 4₁ₛ (Rejection)
**Description**: Four consecutive controls exceed the same ±1 standard deviation  
**Interpretation**: Systematic error or shift  
**Action**: Reject run, investigate trending issue  
**Severity**: Rejection

### Rule 10ₓ (Rejection)
**Description**: Ten consecutive controls on the same side of the mean  
**Interpretation**: Systematic error or calibration shift  
**Action**: Reject run, recalibrate instrument  
**Severity**: Rejection

### Rule 3₁ₛ (Warning)
**Description**: Three consecutive controls exceed the same ±1 standard deviation  
**Interpretation**: Possible systematic error developing  
**Action**: Warning only, monitor trend  
**Severity**: Warning

### Rule 7ₜ (Warning)
**Description**: Seven consecutive controls showing consistent trend (all increasing or all decreasing)  
**Interpretation**: Drift or trending systematic error  
**Action**: Warning only, investigate potential drift  
**Severity**: Warning

---

## Appendix B: Glossary

**ASTM**: American Society for Testing and Materials - defines communication protocols for laboratory instruments

**CAP**: College of American Pathologists - accreditation organization for clinical laboratories

**CLIA**: Clinical Laboratory Improvement Amendments - federal regulatory standards for laboratory testing

**Control Lot**: A specific batch of quality control material with defined characteristics

**L-J Chart**: Levey-Jennings Chart - a quality control chart showing test results over time with statistical limits

**QC**: Quality Control - procedures used to monitor test system performance

**SD**: Standard Deviation - a measure of variability in QC results

**Westgard Rules**: Statistical quality control rules used to evaluate the acceptability of an analytical run

**Z-Score**: Number of standard deviations a value is from the mean: (value - mean) / SD

---

**Document Control**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-13 | Implementation Planning Team | Initial requirements and approach document |

**Approval**

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Product Owner | | | |
| Technical Lead | | | |
| QA Manager | | | |

---

*End of Document*
