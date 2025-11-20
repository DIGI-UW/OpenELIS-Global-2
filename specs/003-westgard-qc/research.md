# Research: Westgard Rules Quality Control Compliance

**Feature**: 003-westgard-qc  
**Date**: 2025-11-20  
**Status**: Complete  
**Cross-Reference**: Feature 004 (ASTM Analyzer Field Mapping) - QC result capture integration

This document consolidates technical research and decisions for implementing the Westgard Rules Quality Control compliance feature.

## 1. Westgard Rule Algorithms

### Decision: Implement All 8 Standard Westgard Rules with Reference Datasets

**Rationale**: The 8 standard Westgard rules (1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ, 7ₜ) are well-established in clinical laboratory quality control and required for CLIA/CAP compliance. Implementation must follow authoritative references to ensure accuracy.

**Implementation Approach**:

- **Rule Definitions** (from `.dev-docs/OGC-41/westgard_rules_implementation.md`):
  - **1₂ₛ (Warning)**: Single control exceeds ±2 standard deviations
  - **1₃ₛ (Rejection)**: Single control exceeds ±3 standard deviations
  - **2₂ₛ (Rejection)**: Two consecutive controls exceed same ±2SD limit
  - **R₄ₛ (Rejection)**: Range between two consecutive controls exceeds 4SD
  - **4₁ₛ (Rejection)**: Four consecutive controls exceed same ±1SD limit
  - **10ₓ (Rejection)**: Ten consecutive controls on same side of mean
  - **3₁ₛ (Warning)**: Three consecutive controls exceed same ±1SD limit
  - **7ₜ (Warning)**: Seven consecutive controls showing consistent upward or downward trend

- **Z-Score Calculation**: `z = (value - mean) / standardDeviation`
  - Mean and SD stored in `QCStatistics` entity
  - Z-score calculated at result entry time
  - Cached in `QCResult.z_score` column for performance

- **Sequential Rule Evaluation**: Rules requiring historical data (2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ, 7ₜ) must retrieve previous results ordered chronologically
  - Use HQL query: `SELECT r FROM QCResult r WHERE r.controlLot.id = :lotId AND r.runDateTime < :currentDateTime ORDER BY r.runDateTime DESC`
  - Limit results to maximum needed (10 for 10ₓ rule)
  - Skip rule evaluation if insufficient data available

- **Architecture Pattern**: Strategy pattern for rule evaluators
  - Each rule implemented as separate class implementing `WestgardRuleEvaluator` interface
  - `WestgardRuleEvaluationService` coordinates evaluation using all enabled rules
  - Service retrieves enabled rules from `WestgardRuleConfig` per test-analyzer combination

**Testing Strategy**:
- Use reference datasets from Westgard QC literature for validation
- Unit test each rule evaluator with known-good/known-bad datasets
- Test edge cases: insufficient data, boundary values, consecutive violations

**Alternatives Considered**:
- **Option A**: Implement subset of rules (e.g., only 1₃ₛ and 2₂ₛ)
  - **Rejected**: Regulatory requirements (CLIA/CAP) expect comprehensive multi-rule QC
- **Option B**: Single monolithic evaluator class
  - **Rejected**: Poor testability, violates Single Responsibility Principle

**References**:
- `.dev-docs/OGC-41/westgard_rules_implementation.md` (authoritative reference)
- Westgard, J.O. "Basic QC Practices" - Industry standard reference
- CLSI C24-A3: Statistical Quality Control for Quantitative Measurement Procedures

## 2. Statistical Calculation Methods

### Decision: Support Three Calculation Methods with Strategy Pattern

**Rationale**: Different laboratories use different approaches to establish QC statistics depending on control lot characteristics, regulatory requirements, and available historical data.

**Implementation Approach**:

- **Three Calculation Methods**:
  1. **Initial Establishment (First N Runs)**: Calculate mean and SD from first N results (default N=20, configurable)
     - Control lot status: `ESTABLISHMENT` until N results collected
     - Rules NOT evaluated during establishment phase
     - Formula: Standard sample mean and sample SD (Bessel's correction: n-1)
  2. **Rolling Calculation (Moving Window)**: Continuously recalculate statistics using most recent N results
     - Statistics updated with each new result
     - Older results excluded from calculation after window size exceeded
     - Provides dynamic adaptation to reagent/calibrator changes
  3. **Manufacturer Fixed Values**: Use mean and SD provided by control lot manufacturer
     - Lot immediately active (no establishment phase)
     - Statistics never recalculated
     - Used when manufacturer provides peer-verified values

- **Architecture Pattern**: Strategy pattern for statistics calculators
  - Interface: `StatisticsCalculator` with method `QCStatistics calculate(QCControlLot lot, List<QCResult> results)`
  - Implementations: `InitialRunsCalculator`, `RollingCalculator`, `ManufacturerFixedCalculator`
  - Service layer selects calculator based on `QCControlLot.calculationMethod` field

- **Caching Strategy**: Statistics stored in `qc_statistics` table
  - Recalculation triggered only when:
    - New result added to lot
    - Calculation method changed
    - Lot transitions from establishment to active
  - Cache invalidation by checking `qc_statistics.calculated_date` vs `qc_result.run_date_time`

- **Formula Details**:
  ```java
  // Sample mean
  mean = sum(values) / count(values)
  
  // Sample standard deviation (Bessel's correction)
  SD = sqrt(sum((value - mean)^2) / (n - 1))
  
  // Z-score
  z = (value - mean) / SD
  ```

**Validation Rules**:
- Minimum 20 results required for initial establishment (configurable via `SystemConfiguration`)
- Rolling window size: 20-100 results (configurable)
- Manufacturer fixed values: Mean and SD must be positive, SD must be reasonable (e.g., <50% of mean for numeric results)

**Alternatives Considered**:
- **Option A**: Single fixed calculation method (initial runs only)
  - **Rejected**: Inflexible, doesn't meet diverse laboratory needs
- **Option B**: Automatic method selection based on lot characteristics
  - **Rejected**: Removes administrator control, may cause unexpected behavior

**References**:
- CLSI C24-A3: Statistical Quality Control for Quantitative Measurement Procedures
- Specification FR-004 (statistical calculation method requirements)

## 3. QC Result Capture Integration with Feature 004

### Decision: Service-to-Service Integration via QCResultService.createQCResult() Method

**Rationale**: Feature 004 (ASTM Analyzer Field Mapping) handles ALL ASTM message parsing including Q-segments (QC results). Feature 003 provides a service method that 004 calls after parsing QC fields.

**Implementation Approach**:

- **Integration Contract** (from 004:plan.md QC Result Processing Integration section):
  ```java
  public interface QCResultService {
      /**
       * Creates QC result from ASTM analyzer data (called by Feature 004)
       * 
       * @param analyzerId Analyzer ID (from AnalyzerField mapping)
       * @param testId Test ID (mapped from ASTM test code)
       * @param controlLotId Control lot ID (mapped from ASTM lot number)
       * @param controlLevel Control level enum (LOW/NORMAL/HIGH from ASTM control level field)
       * @param resultValue Measured value (from ASTM result field)
       * @param unit Unit of measure (from ASTM unit field or UnitMapping)
       * @param timestamp Run date/time (from ASTM timestamp field)
       * @return Created QCResult entity
       * @throws IllegalArgumentException if control lot not found or inactive
       */
      QCResult createQCResult(String analyzerId, String testId, String controlLotId,
                              ControlLevel controlLevel, BigDecimal resultValue,
                              String unit, Timestamp timestamp);
  }
  ```

- **Method Responsibilities**:
  1. Validate control lot exists and is active
  2. Retrieve latest `QCStatistics` for control lot
  3. Calculate z-score: `(resultValue - statistics.mean) / statistics.sd`
  4. Persist `QCResult` entity with all fields populated
  5. Publish `QCResultCreatedEvent` to trigger asynchronous rule evaluation
  6. Return created `QCResult` entity

- **Rule Evaluation Triggering**: Event-driven architecture
  - `QCResultCreatedEvent` published after result persisted (transaction boundary respected)
  - `QCResultCreatedEventListener` handles event asynchronously using `@Async`
  - Event listener retrieves enabled rules, evaluates all rules, creates violation records

- **Transaction Boundaries**:
  - Feature 004's ASTM parser calls `QCResultService.createQCResult()` within 004's transaction
  - Result persistence completes before 004's transaction commits
  - Rule evaluation runs in separate async transaction (prevents blocking ASTM interface)

- **Error Handling**:
  - If control lot not found: Feature 004 creates `AnalyzerError` (type: MAPPING, severity: ERROR)
  - If control lot inactive/expired: Feature 004 creates `AnalyzerError` (type: VALIDATION, severity: WARNING)
  - If statistics not yet established: Result saved but rules skipped (logged as informational)

**Manual Entry Fallback**:
- Manual QC result entry form calls same `QCResultService.createQCResult()` method
- Ensures consistent z-score calculation and rule evaluation regardless of entry method
- Form validation ensures all required fields provided before service call

**Alternatives Considered**:
- **Option A**: Feature 003 directly parses ASTM Q-segments
  - **Rejected**: Duplicates ASTM parsing logic from 004, violates DRY principle
- **Option B**: Feature 004 directly persists QC results without service call
  - **Rejected**: Bypasses business logic (z-score calculation, rule triggering), creates tight coupling

**References**:
- Feature 004 spec.md FR-021 (QC result processing)
- Feature 004 plan.md "QC Result Processing Integration" section
- Feature 003 spec.md FR-008 (QC result capture service contract)
- Feature 003 spec.md FR-009 (manual entry fallback)

## 4. Carbon Design System Components for QC Visualization

### Decision: @carbon/charts-react LineChart for Levey-Jennings Charts

**Rationale**: OpenELIS uses Carbon Design System exclusively (Constitution II). Carbon Charts provides production-ready, accessible charting components that align with Carbon design tokens.

**Implementation Approach**:

- **Levey-Jennings Chart Component**:
  ```tsx
  import { LineChart } from '@carbon/charts-react';
  import '@carbon/charts/styles.css';
  
  const chartOptions = {
    axes: {
      bottom: {
        title: 'Run Number',
        mapsTo: 'runNumber',
        scaleType: 'linear',
      },
      left: {
        title: 'Result Value',
        mapsTo: 'value',
        scaleType: 'linear',
      },
    },
    grid: {
      y: {
        enabled: true,
        numberOfTicks: 15,  // Mean + 6 SD lines (±1SD, ±2SD, ±3SD)
      },
    },
    points: {
      radius: (dataPoint) => (dataPoint.violated ? 6 : 4),  // Larger for violations
      filled: true,
    },
    color: {
      scale: {
        'QC Result': '#0f62fe',   // $blue-60 (normal points)
        'Violation': '#da1e28',   // $red-60 (violation points)
      },
    },
    tooltip: {
      customHTML: ([dataPoint]) => `
        <div class="bx--tooltip__label">
          <strong>Value:</strong> ${dataPoint.value}<br/>
          <strong>Z-score:</strong> ${dataPoint.zScore}<br/>
          <strong>Date:</strong> ${dataPoint.date}<br/>
          ${dataPoint.violations ? `<strong>Violations:</strong> ${dataPoint.violations.join(', ')}` : ''}
        </div>
      `,
    },
  };
  ```

- **Data Structure for Chart**:
  ```javascript
  const chartData = [
    {
      group: 'QC Result',
      runNumber: 1,
      value: 100.5,
      zScore: 0.5,
      date: '2025-01-15 08:00',
      violated: false,
    },
    {
      group: 'Violation',
      runNumber: 2,
      value: 115.2,
      zScore: 3.2,
      date: '2025-01-15 10:00',
      violated: true,
      violations: ['1₃ₛ'],
    },
  ];
  ```

- **Reference Lines (Mean, ±1SD, ±2SD, ±3SD)**:
  - Carbon Charts doesn't natively support horizontal reference lines
  - **Solution**: Use `grid.y.values` array to force grid lines at specific Y values (mean, mean±1SD, mean±2SD, mean±3SD)
  - Add custom CSS to style mean line as solid, SD lines as dashed:
    ```scss
    // Use Carbon design tokens for colors
    .bx--cc--grid-line--y {
      stroke: $ui-03;  // Carbon token for subtle grid lines
      stroke-width: 1;
      stroke-dasharray: 4, 4;  // Dashed for SD lines
      
      &.mean-line {
        stroke: $text-01;  // Darker for mean line
        stroke-width: 2;
        stroke-dasharray: none;  // Solid for mean
      }
    }
    ```

- **Interactive Features**:
  - Date range filtering: Use Carbon `DatePicker` component, filter data array before passing to chart
  - Zoom/pan: Carbon Charts supports built-in zoom via mouse wheel and drag
  - Point highlighting: Hover shows tooltip with z-score and violation details
  - Export: Carbon `OverflowMenu` with "Export as PNG" and "Print Chart" options

**Dashboard Components**:
- **Compliance Status Tiles**: Carbon `Tile` component with color-coded indicators
  - Green ($green-60): All rules compliant
  - Yellow ($yellow-30): Warning-level violations
  - Red ($red-60): Rejection-level violations
- **Violation List**: Carbon `DataTable` with sorting, filtering, pagination
- **Corrective Action Form**: Carbon `Form` components (TextInput, Dropdown, TextArea, Button)

**Alternatives Considered**:
- **Option A**: Use D3.js directly for custom charting
  - **Rejected**: Violates Carbon Design System First principle, requires custom accessibility implementation
- **Option B**: Use third-party charting library (Chart.js, Recharts)
  - **Rejected**: Violates Constitution II (Carbon exclusive), inconsistent styling

**References**:
- @carbon/charts-react documentation: https://charts.carbondesignsystem.com/react/
- OpenELIS Carbon Guide: https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838
- Feature 003 spec.md FR-052 to FR-059 (Levey-Jennings chart requirements)

## 5. Alert and Notification System

### Decision: Leverage Existing EmailNotification Infrastructure with Batching

**Rationale**: OpenELIS already has email notification infrastructure (`org.openelisglobal.notification.service.EmailNotificationService`). Extending this service for QC alerts avoids reinventing the wheel.

**Implementation Approach**:

- **Email Notification Service Integration**:
  - `QCAlertService` calls `EmailNotificationService.sendEmail()` with QC alert template
  - Email template: `qc_alert_email.ftl` (FreeMarker template)
  - Template variables: analyzer name, test name, rule violated, z-score, QC result value, timestamp, link to detailed view

- **Alert Batching Logic** (per FR-072, FR-073):
  ```java
  @Service
  public class QCAlertServiceImpl implements QCAlertService {
      
      @Transactional
      public void sendAlertForViolation(QCRuleViolation violation) {
          // Exception: 1₃ₛ rule always sends immediate alert
          if ("1_3s".equals(violation.getRuleCode())) {
              sendImmediateEmail(violation);
              return;
          }
          
          // Batching: Check for recent alerts on same analyzer
          LocalDateTime now = LocalDateTime.now();
          QCAlert recentAlert = qcAlertDAO.findMostRecentByAnalyzerId(
              violation.getAnalyzerId()
          );
          
          if (recentAlert != null) {
              long minutesSinceLastAlert = ChronoUnit.MINUTES.between(
                  recentAlert.getSentDateTime(), now
              );
              
              if (minutesSinceLastAlert < 15) {
                  // Within batching window - skip email, log only
                  log.info("Alert batched for analyzer {} (last alert {}min ago)",
                      violation.getAnalyzerId(), minutesSinceLastAlert);
                  return;
              }
          }
          
          // Outside batching window - send email
          sendBatchedEmail(violation);
      }
  }
  ```

- **In-System Notifications**: Carbon `InlineNotification` component
  - Real-time alert feed on QC dashboard (polls `/rest/qc/alerts/recent` every 30 seconds)
  - Notification includes: severity (error/warning), analyzer name, rule violated, timestamp
  - Click notification → navigate to violation detail view

- **Notification Recipients**:
  - Technician who performed QC test (retrieved from `QCResult.sys_user_id`)
  - QC supervisor/manager (retrieved from user role: `BIOLOGIST`)
  - Users subscribed to specific analyzer (stored in `AnalyzerSubscription` table - future enhancement)

- **Alert Persistence**: `QCAlert` entity stores all sent alerts
  - Fields: violation ID, alert type (email/system), recipient user ID, sent timestamp, read status
  - Read status updated when user views violation detail page
  - Alert history queryable for audit reports

**Alternatives Considered**:
- **Option A**: Use third-party notification service (Twilio, SendGrid)
  - **Rejected**: Introduces external dependency, adds cost, requires internet connectivity
- **Option B**: Real-time WebSocket notifications
  - **Rejected**: Adds complexity, OpenELIS doesn't currently use WebSockets, polling sufficient for QC alerts (not high-frequency)

**References**:
- `src/main/java/org/openelisglobal/notification/service/EmailNotificationService.java`
- Feature 003 spec.md FR-065 to FR-073 (alert and notification requirements)

## 6. Corrective Action Workflow

### Decision: Adapt Non-Conforming Event (NcEvent) Pattern for QC Corrective Actions

**Rationale**: OpenELIS already implements corrective action workflows for non-conforming events (`org.openelisglobal.nce.*` package). The QC corrective action workflow has similar requirements: issue detection, assignment, resolution, audit trail.

**Implementation Approach**:

- **State Machine Pattern**:
  - States: `PENDING` → `ASSIGNED` → `IN_PROGRESS` → `COMPLETED` → `RESOLVED`
  - Transitions validated in service layer to prevent invalid state changes
  - Example: Cannot mark `COMPLETED` without resolution notes, cannot mark `RESOLVED` without all linked corrective actions completed

- **Predefined Action Types** (per FR-039):
  - `RECALIBRATION`: Recalibrate analyzer
  - `MAINTENANCE`: Perform preventive/corrective maintenance
  - `REPEAT_CONTROL`: Repeat QC control run
  - `REAGENT_CHANGE`: Replace reagents/consumables
  - `OTHER`: Custom action with required description

- **Workflow Integration**:
  ```java
  @Service
  public class QCCorrectiveActionServiceImpl {
      
      @Transactional
      public void completeCorrectiveAction(String actionId, String resolutionNotes) {
          QCCorrectiveAction action = correctiveActionDAO.get(actionId);
          
          // Validate state transition
          if (!CorrectiveActionStatus.IN_PROGRESS.equals(action.getStatus())) {
              throw new IllegalStateException("Can only complete in-progress actions");
          }
          
          // Update action status
          action.setStatus(CorrectiveActionStatus.COMPLETED);
          action.setResolutionNotes(resolutionNotes);
          action.setCompletedDateTime(new Timestamp(System.currentTimeMillis()));
          correctiveActionDAO.update(action);
          
          // Add external note to QC result
          QCResult result = action.getViolation().getTriggeringResult();
          String note = String.format("Corrective Action: %s. Resolution: %s",
              action.getActionType(), resolutionNotes);
          result.setExternalNote(note);
          qcResultDAO.update(result);
          
          // Auto-resolve linked violation
          QCRuleViolation violation = action.getViolation();
          violation.setResolutionStatus(ViolationResolutionStatus.RESOLVED);
          violation.setResolvedDateTime(new Timestamp(System.currentTimeMillis()));
          qcRuleViolationDAO.update(violation);
      }
  }
  ```

- **Patient Result Release Blocking** (per FR-045):
  - When rejection-level violation detected, create `AnalyzerBlock` record linking analyzer + test
  - Result validation service checks for active blocks before allowing result release
  - Block removed when all linked corrective actions completed and violation resolved

- **Audit Trail**:
  - All corrective action state changes logged with user ID and timestamp (via `BaseObject<String>`)
  - Violation resolution links back to corrective action for traceability
  - QC result external notes provide human-readable summary for review

**Alternatives Considered**:
- **Option A**: Create entirely new corrective action system
  - **Rejected**: Duplicates existing NcEvent infrastructure, increases maintenance burden
- **Option B**: Reuse NcEvent entity directly for QC corrective actions
  - **Rejected**: QC and NCE have different business contexts, would create confusion

**References**:
- `src/main/java/org/openelisglobal/nce/` (Non-Conforming Event package)
- Feature 003 spec.md FR-039 to FR-045 (corrective action workflow requirements)

## 7. Navigation Integration with Feature 004 Analyzer Menu

### Decision: QC Pages Nested Under /analyzers Parent Route

**Rationale**: Feature 003's QC functionality is tightly coupled to analyzer management (Feature 004). Navigation hierarchy should reflect this relationship, with QC pages accessible from analyzer menu.

**Implementation Approach**:

- **Navigation Hierarchy** (from 004:research.md Section 6):
  ```
  Analyzers (parent, managed by 004)
  ├── Analyzers Dashboard (/analyzers) [004]
  ├── Error Dashboard (/analyzers/errors) [004]
  └── Quality Control (group)
      ├── QC Dashboard (/analyzers/qc) [003]
      ├── QC Alerts & Violations (/analyzers/qc/alerts) [003]
      └── Corrective Actions (/analyzers/qc/corrective-actions) [003]
  ```

- **Backend-Driven Menu**: Feature 004 manages menu structure via `/rest/menu` API
  - Feature 003 adds new menu entries to database for QC routes
  - Menu items filtered by role: QC routes only visible to users with `BIOLOGIST` or `GLOBAL_ADMIN` roles
  - Active navigation highlighting: `location.pathname.startsWith('/analyzers/qc')` highlights QC menu items

- **Route Configuration**:
  ```javascript
  // frontend/src/App.js
  <Route path="/analyzers/qc" element={<QCDashboard />} />
  <Route path="/analyzers/qc/alerts" element={<QCViolationList />} />
  <Route path="/analyzers/qc/corrective-actions" element={<QCCorrectiveActionList />} />
  <Route path="/analyzers/qc/charts/:analyzerId" element={<ControlChartDetail />} />
  ```

- **State Preservation**:
  - URL query parameters for filters, pagination: `?analyzerId=A1&status=UNRESOLVED&page=2`
  - sessionStorage for scroll position, form drafts
  - No Carbon Tabs/TabList components - navigation via left-hand sub-menu only (unified pattern per 004:FR-020)

**Alternatives Considered**:
- **Option A**: QC pages as top-level routes (/qc, /qc/alerts)
  - **Rejected**: Breaks logical grouping with analyzer management, confuses users
- **Option B**: Separate QC menu section independent of analyzers
  - **Rejected**: QC is analyzer-specific functionality, should be nested under analyzer context

**References**:
- Feature 004 research.md Section 6 (Navigation Integration)
- Feature 004 spec.md FR-020 (unified tab-navigation pattern)
- Feature 003 spec.md Dependencies section (navigation hierarchy requirement)

## 8. Testing Strategy

### Decision: Comprehensive TDD Workflow with >80% Backend, >70% Frontend Coverage

**Rationale**: Westgard rule evaluation is complex, safety-critical logic that requires rigorous testing. TDD ensures correctness from the start and enables confident refactoring.

**Implementation Approach**:

- **Unit Tests (JUnit 4 + Mockito)**: Focus on rule evaluators
  - Each of 8 rule evaluators has dedicated test class with 5-10 test cases
  - Test cases use reference datasets from Westgard QC literature (known-good/known-bad)
  - Example test for Rule1_3sEvaluator:
    ```java
    @Test
    public void testRule1_3s_ViolationDetected_WhenZScoreExceeds3SD() {
        // Arrange
        QCStatistics stats = createStatistics(mean=100.0, sd=5.0);
        QCResult current = createResult(value=116.0, zScore=3.2);  // Exceeds +3SD
        
        // Act
        RuleEvaluationResult result = rule1_3sEvaluator.evaluate(current, emptyList(), stats);
        
        // Assert
        assertTrue("1₃ₛ rule should be violated", result.isViolated());
        assertEquals("1_3s", result.getRuleCode());
    }
    ```

- **DAO Tests (@DataJpaTest)**: Persistence layer validation
  - Test historical result retrieval for sequential rules (10ₓ requires 10 previous results)
  - Test violation queries with filtering (by severity, date range, resolution status)
  - Test statistics calculation queries (date-based aggregation)

- **Controller Tests (@WebMvcTest)**: REST API endpoint testing
  - Test GET `/rest/qc/dashboard/{analyzerId}` returns compiled dashboard data
  - Test POST `/rest/qc/results` triggers auto-evaluation
  - Test POST `/rest/qc/violations/{id}/resolve` updates violation status

- **ORM Validation Tests** (Constitution V.4):
  ```java
  @Test
  public void testQCHibernateMappingsLoadSuccessfully() {
      Configuration config = new Configuration();
      config.addAnnotatedClass(QCControlLot.class);
      config.addAnnotatedClass(QCResult.class);
      config.addAnnotatedClass(QCStatistics.class);
      config.addAnnotatedClass(WestgardRuleConfig.class);
      config.addAnnotatedClass(QCRuleViolation.class);
      config.addAnnotatedClass(QCCorrectiveAction.class);
      config.addAnnotatedClass(QCAlert.class);
      
      SessionFactory sf = config.buildSessionFactory();
      assertNotNull("All QC mappings should load without errors", sf);
      sf.close();
  }
  ```

- **Frontend Tests (Jest + React Testing Library)**:
  - Test QC dashboard component with compliance tiles (green/yellow/red indicators)
  - Test Levey-Jennings chart renders with correct data points and SD lines
  - Test violation list filtering and sorting
  - Test corrective action form validation

- **E2E Tests (Cypress)**: User workflow validation
  - Individual test execution during development (max 5-10 test cases per run)
  - Use `data-testid` selectors for stability
  - Use `cy.intercept()` for reliable API waiting
  - Example test:
    ```javascript
    it('should display violation alert when 1₃ₛ rule triggered', () => {
      // Setup: Create QC result via API that violates 1₃ₛ
      cy.request('POST', '/rest/qc/results', {
        analyzerId: 'A1',
        value: 116.0,  // Exceeds +3SD
      });
      
      // Navigate to dashboard
      cy.visit('/analyzers/qc');
      
      // Assert: Violation alert visible
      cy.get('[data-testid="violation-alert"]').should('be.visible');
      cy.get('[data-testid="violation-alert"]').should('contain', '1₃ₛ');
    });
    ```

**Coverage Goals**:
- Backend: >80% (measured via JaCoCo)
- Frontend: >70% (measured via Jest)
- Critical paths: 100% (rule evaluators, statistics calculation, violation detection)

**Alternatives Considered**:
- **Option A**: Implementation-first workflow (tests after code)
  - **Rejected**: Higher risk of bugs, harder to refactor, doesn't enforce testability
- **Option B**: Lower coverage targets (50% backend, 50% frontend)
  - **Rejected**: Insufficient for safety-critical QC logic, regulatory compliance concerns

**References**:
- `.specify/guides/testing-roadmap.md` (testing strategy guidance)
- Constitution Section V (Test-Driven Development requirements)
- Feature 003 plan.md Testing Strategy section

## Summary of Technical Decisions

| Decision Area | Chosen Approach | Rationale |
|---|---|---|
| Westgard Rules | All 8 rules with Strategy pattern | CLIA/CAP compliance, independent testability |
| Statistics Calculation | Three methods (initial/rolling/manufacturer) with Strategy pattern | Flexible for diverse laboratory needs |
| QC Result Capture | Service-to-service integration via QCResultService.createQCResult() | Decoupled from ASTM parsing (004), reusable for manual entry |
| Charting | @carbon/charts-react LineChart | Carbon Design System compliance, accessibility |
| Alert Batching | Service-layer batching with 1₃ₛ exception | Prevents alert fatigue, prioritizes critical alerts |
| Corrective Actions | Adapted NcEvent pattern | Leverages existing infrastructure, proven workflow |
| Navigation | QC pages nested under /analyzers route (004) | Logical grouping, unified navigation pattern |
| Testing | TDD with >80% backend, >70% frontend coverage | Safety-critical logic requires rigor |

## Open Questions (Resolved)

All research questions have been resolved. No outstanding technical unknowns.

## Next Steps

1. Generate data model (`data-model.md`) based on entity decisions above
2. Create API contracts (`contracts/`) for REST endpoints
3. Write quickstart guide (`quickstart.md`) for developers
4. Execute tasks.md implementation using TDD workflow

