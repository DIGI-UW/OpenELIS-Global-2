# Quickstart Guide: Westgard Rules Quality Control Compliance

**Feature**: 003-westgard-qc  
**Date**: 2025-11-20  
**Audience**: Developers  
**Status**: Ready for Implementation

This guide provides step-by-step instructions for setting up, developing,
testing, and troubleshooting the Westgard QC feature.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Database Schema Setup](#database-schema-setup)
4. [Backend Development](#backend-development)
5. [Frontend Development](#frontend-development)
6. [Testing](#testing)
7. [Common Issues & Troubleshooting](#common-issues--troubleshooting)
8. [Development Workflow](#development-workflow)

## Prerequisites

### Required Software

- **Java 21 LTS** (MANDATORY - build will fail with Java 8/11/17)
- **Maven 3.8+** (build system)
- **Node.js 16+** (frontend development)
- **Docker + Docker Compose** (container orchestration)
- **PostgreSQL 14+** (runs in Docker)
- **Git with submodules**

### Verify Java Version

```bash
# Check Java version (MUST show 21.x.x)
java -version

# Use SDKMAN for automatic version switching (recommended)
sdk env  # Automatically switches to Java 21 based on .sdkmanrc
```

### IDE Setup

- **IntelliJ IDEA** (recommended) or **Eclipse**
- **Spotless** code formatter plugin
- **Lombok** plugin (if used)
- **React Developer Tools** browser extension

## Local Development Setup

### 1. Clone Repository

```bash
# Clone with submodules
git clone https://github.com/DIGI-UW/OpenELIS-Global-2.git
cd OpenELIS-Global-2
git submodule update --init --recursive

# Checkout feature branch
git checkout 003-westgard-qc
```

### 2. Build DataExport Submodule

```bash
cd dataexport
mvn clean install -DskipTests -Dmaven.test.skip=true
cd ..
```

### 3. Build OpenELIS Backend

```bash
# Build WAR file (skip tests for fast iteration)
mvn clean install -DskipTests -Dmaven.test.skip=true
```

**Why both flags?**

- `-DskipTests`: Skips Surefire unit test execution
- `-Dmaven.test.skip=true`: Skips test compilation AND execution (including
  Failsafe integration tests)

### 4. Start Development Environment

```bash
# Start PostgreSQL + HAPI FHIR containers
docker compose -f dev.docker-compose.yml up -d

# View logs
docker compose -f dev.docker-compose.yml logs -f oe.openelis.org
```

**Access Points**:

- React UI: https://localhost/
- Legacy UI: https://localhost/api/OpenELIS-Global/
- FHIR Server: https://fhir.openelis.org:8443/fhir/
- PostgreSQL: localhost:5432 (credentials in `dev.docker-compose.yml`)

## Database Schema Setup

### 1. Run Liquibase Migrations

Liquibase migrations run automatically on application startup. Verify migrations
applied:

```bash
# Connect to PostgreSQL container
docker exec -it openelisglobal-database-1 psql -U clinlims -d clinlims

# Check Liquibase changelog
SELECT id, filename FROM databasechangelog WHERE filename LIKE '%qc%' ORDER BY orderexecuted DESC LIMIT 10;

# Verify QC tables created
\dt qc_*
```

Expected tables:

- `qc_control_lot`
- `qc_result`
- `qc_statistics`
- `westgard_rule_config`
- `qc_rule_violation`
- `qc_corrective_action`
- `qc_alert`

### 2. Rollback Schema (if needed)

```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Or rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=qc-baseline
```

### 3. Insert Test Data (Optional)

```bash
# Insert sample control lots and results
psql -U clinlims -d clinlims -f specs/003-westgard-qc/test-data/sample-control-lots.sql
```

## Backend Development

### Project Structure

```
src/main/java/org/openelisglobal/qc/
├── valueholder/          # JPA Entities (Layer 1)
│   ├── QCControlLot.java
│   ├── QCResult.java
│   └── ...
├── dao/                  # Data Access Objects (Layer 2)
│   ├── QCControlLotDAO.java
│   ├── QCControlLotDAOImpl.java
│   └── ...
├── service/              # Business Logic (Layer 3)
│   ├── QCResultService.java
│   ├── QCResultServiceImpl.java
│   ├── evaluator/        # Strategy pattern for Westgard rules
│   │   ├── WestgardRuleEvaluator.java (interface)
│   │   ├── Rule1_3sEvaluator.java
│   │   └── ...
│   └── calculator/       # Strategy pattern for statistics
│       ├── StatisticsCalculator.java (interface)
│       ├── InitialRunsCalculator.java
│       └── ...
├── controller/           # REST Endpoints (Layer 4)
│   ├── QCRestController.java
│   └── ...
└── form/                 # DTOs (Layer 5)
    ├── QCResultForm.java
    └── ...
```

### Key Development Patterns

#### 1. Entity Creation (JPA + Annotations)

```java
@Entity
@Table(name = "qc_control_lot")
public class QCControlLot extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)  // Always LAZY for FK relationships
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @Column(name = "lot_number", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String lotNumber;

    // Getters and setters
}
```

#### 2. DAO Implementation

```java
@Component
@Transactional
public class QCResultDAOImpl extends BaseDAOImpl<QCResult, String> implements QCResultDAO {

    QCResultDAOImpl() {
        super(QCResult.class);
    }

    @Override
    public List<QCResult> findHistoricalResults(String controlLotId, Timestamp before, int limit) {
        String hql = "SELECT r FROM QCResult r " +
                     "JOIN FETCH r.controlLot " +  // Prevent N+1 queries
                     "WHERE r.controlLot.id = :lotId " +
                     "AND r.runDateTime < :before " +
                     "ORDER BY r.runDateTime DESC";

        return entityManager.createQuery(hql, QCResult.class)
            .setParameter("lotId", controlLotId)
            .setParameter("before", before)
            .setMaxResults(limit)
            .getResultList();
    }
}
```

#### 3. Service Implementation (Business Logic + Transactions)

```java
@Service
@Transactional  // Transactions START here (NOT in controllers)
public class QCResultServiceImpl implements QCResultService {

    @Autowired
    private QCResultDAO qcResultDAO;

    @Autowired
    private QCStatisticsService statisticsService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public QCResult createQCResult(String analyzerId, String testId, String controlLotId,
                                     ControlLevel controlLevel, BigDecimal resultValue,
                                     String unit, Timestamp timestamp) {
        // 1. Validate control lot exists and is active
        QCControlLot lot = qcControlLotDAO.get(controlLotId);
        if (lot == null || !LotStatus.ACTIVE.equals(lot.getStatus())) {
            throw new IllegalArgumentException("Control lot not active: " + controlLotId);
        }

        // 2. Retrieve latest statistics
        QCStatistics stats = statisticsService.getOrCalculateStatistics(lot);

        // 3. Calculate z-score
        BigDecimal zScore = calculateZScore(resultValue, stats.getMean(), stats.getStandardDeviation());

        // 4. Create and persist result
        QCResult result = new QCResult();
        result.setControlLot(lot);
        result.setResultValue(resultValue);
        result.setZScore(zScore);
        result.setRunDateTime(timestamp);
        result.setStatus(ResultStatus.PENDING);

        String id = qcResultDAO.insert(result);

        // 5. Publish event for async rule evaluation
        eventPublisher.publishEvent(new QCResultCreatedEvent(id));

        return qcResultDAO.get(id);
    }

    private BigDecimal calculateZScore(BigDecimal value, BigDecimal mean, BigDecimal sd) {
        return value.subtract(mean).divide(sd, 4, RoundingMode.HALF_UP);
    }
}
```

#### 4. Controller Implementation (HTTP Layer)

```java
@RestController
@RequestMapping("/rest/qc")
public class QCRestController extends BaseRestController {

    @Autowired
    private QCResultService qcResultService;

    @PostMapping("/results")
    public ResponseEntity<?> createQCResult(@Valid @RequestBody QCResultForm form) {
        try {
            QCResult result = qcResultService.createQCResult(
                form.getAnalyzerId(),
                form.getTestId(),
                form.getControlLotId(),
                form.getControlLevel(),
                form.getResultValue(),
                form.getUnit(),
                form.getTimestamp()
            );

            return ResponseEntity.ok(Map.of(
                "resultId", result.getId(),
                "zScore", result.getZScore(),
                "status", result.getStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

**Critical Rules**:

- ❌ NO `@Transactional` in controllers
- ✅ Services compile ALL data within transaction using JOIN FETCH
- ❌ Controllers MUST NOT traverse entity relationships (e.g.,
  `result.getControlLot().getAnalyzer()`)

### Hot Reload After Code Changes

```bash
# Rebuild WAR
mvn clean install -DskipTests -Dmaven.test.skip=true

# Restart container
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

## Frontend Development

### Project Structure

```
frontend/src/components/qc/
├── dashboard/
│   ├── QCDashboard.jsx               # Main dashboard
│   ├── ComplianceStatusTile.jsx      # Green/Yellow/Red indicator
│   └── ...
├── charts/
│   ├── LeveyJenningsChart.jsx        # Carbon LineChart wrapper
│   └── ...
├── violations/
│   ├── ViolationList.jsx             # Carbon DataTable
│   └── ...
└── correctiveActions/
    ├── CorrectiveActionForm.jsx
    └── ...
```

### Key Development Patterns

#### 1. Carbon Design System Components

```jsx
import { Tile, Tag, Button, DataTable } from "@carbon/react";
import { CheckmarkFilled, WarningAlt, ErrorFilled } from "@carbon/icons-react";
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";

function ComplianceStatusTile({ analyzer }) {
  const getStatusIcon = (status) => {
    switch (status) {
      case "COMPLIANT":
        return <CheckmarkFilled size={24} className="status-icon-green" />;
      case "WARNING":
        return <WarningAlt size={24} className="status-icon-yellow" />;
      case "REJECTED":
        return <ErrorFilled size={24} className="status-icon-red" />;
    }
  };

  return (
    <Tile className="compliance-tile">
      {getStatusIcon(analyzer.status)}
      <h4>{analyzer.name}</h4>
      <Tag
        type={
          analyzer.status === "COMPLIANT"
            ? "green"
            : analyzer.status === "WARNING"
            ? "yellow"
            : "red"
        }
      >
        {analyzer.status}
      </Tag>
    </Tile>
  );
}
```

#### 2. Internationalization (React Intl)

```jsx
import { useIntl } from "react-intl";

function QCDashboard() {
  const intl = useIntl();

  return (
    <div>
      <h1>{intl.formatMessage({ id: "qc.dashboard.title" })}</h1>
      <Button>{intl.formatMessage({ id: "qc.button.refresh" })}</Button>
    </div>
  );
}
```

**Message file** (`frontend/src/languages/en.json`):

```json
{
  "qc.dashboard.title": "Quality Control Dashboard",
  "qc.button.refresh": "Refresh",
  "qc.violation.severity.warning": "Warning",
  "qc.violation.severity.rejection": "Rejection"
}
```

#### 3. Data Fetching with SWR

```jsx
import useSWR from "swr";

const fetcher = (url) => fetch(url).then((res) => res.json());

function QCDashboard() {
  const { data, error, isLoading, mutate } = useSWR(
    "/rest/qc/dashboard",
    fetcher,
    {
      refreshInterval: 300000, // Auto-refresh every 5 minutes
    }
  );

  if (isLoading) return <Loading />;
  if (error) return <ErrorMessage message={error.message} />;

  return (
    <div>
      {data.analyzers.map((analyzer) => (
        <ComplianceStatusTile key={analyzer.id} analyzer={analyzer} />
      ))}
    </div>
  );
}
```

#### 4. Levey-Jennings Chart Configuration

```jsx
import { LineChart } from "@carbon/charts-react";

function LeveyJenningsChart({ results, mean, sd }) {
  const chartData = results.map((r, index) => ({
    group: r.violated ? "Violation" : "QC Result",
    runNumber: index + 1,
    value: r.value,
    zScore: r.zScore,
    date: r.date,
    violated: r.violated,
    violations: r.violations,
  }));

  const chartOptions = {
    axes: {
      bottom: {
        title: "Run Number",
        mapsTo: "runNumber",
        scaleType: "linear",
      },
      left: {
        title: "Result Value",
        mapsTo: "value",
        scaleType: "linear",
      },
    },
    grid: {
      y: {
        enabled: true,
        values: [
          mean,
          mean + sd,
          mean + 2 * sd,
          mean + 3 * sd,
          mean - sd,
          mean - 2 * sd,
          mean - 3 * sd,
        ],
      },
    },
    points: {
      radius: (dataPoint) => (dataPoint.violated ? 6 : 4),
      filled: true,
    },
    color: {
      scale: {
        "QC Result": "#0f62fe", // Carbon $blue-60
        Violation: "#da1e28", // Carbon $red-60
      },
    },
    tooltip: {
      customHTML: ([dataPoint]) => `
        <div class="bx--tooltip__label">
          <strong>Value:</strong> ${dataPoint.value}<br/>
          <strong>Z-score:</strong> ${dataPoint.zScore}<br/>
          <strong>Date:</strong> ${dataPoint.date}<br/>
          ${
            dataPoint.violations
              ? `<strong>Violations:</strong> ${dataPoint.violations.join(
                  ", "
                )}`
              : ""
          }
        </div>
      `,
    },
  };

  return <LineChart data={chartData} options={chartOptions} />;
}
```

### Frontend Development Server

```bash
cd frontend

# Install dependencies
npm install

# Start development server with hot reload
npm start

# Access at http://localhost:3000
```

## Testing

### Backend Unit Tests (JUnit 4 + Mockito)

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=Rule1_3sEvaluatorTest

# Run tests with coverage report (JaCoCo)
mvn clean test jacoco:report
# View report: target/site/jacoco/index.html
```

**Example Unit Test**:

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class Rule1_3sEvaluatorTest {

    @Mock
    private QCStatisticsService statisticsService;

    @InjectMocks
    private Rule1_3sEvaluator evaluator;

    @Test
    public void testEvaluate_ViolationDetected_WhenZScoreExceeds3SD() {
        // Arrange
        QCStatistics stats = createStatistics(100.0, 5.0);  // mean=100, sd=5
        QCResult current = createResult(116.0, 3.2);        // value=116, z=3.2

        // Act
        RuleEvaluationResult result = evaluator.evaluate(current, emptyList(), stats);

        // Assert
        assertTrue("1₃ₛ rule should be violated", result.isViolated());
        assertEquals("1_3s", result.getRuleCode());
    }
}
```

### Frontend Unit Tests (Jest + React Testing Library)

```bash
cd frontend

# Run all tests
npm test

# Run specific test file
npm test -- QCDashboard.test.jsx

# Run tests with coverage report
npm test -- --coverage
# View report: frontend/coverage/lcov-report/index.html
```

**Example Frontend Test**:

```javascript
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import QCDashboard from "./QCDashboard";
import messages from "../../languages/en.json";

test("displays compliance status tiles for analyzers", () => {
  const mockData = {
    analyzers: [
      { id: "A1", name: "Hematology Analyzer", status: "COMPLIANT" },
      { id: "A2", name: "Chemistry Analyzer", status: "REJECTED" },
    ],
  };

  render(
    <IntlProvider locale="en" messages={messages}>
      <QCDashboard data={mockData} />
    </IntlProvider>
  );

  expect(screen.getByText("Hematology Analyzer")).toBeInTheDocument();
  expect(screen.getByText("Chemistry Analyzer")).toBeInTheDocument();
  expect(screen.getByText("COMPLIANT")).toBeInTheDocument();
  expect(screen.getByText("REJECTED")).toBeInTheDocument();
});
```

### E2E Tests (Cypress)

```bash
cd frontend

# Run individual test file (DEVELOPMENT - fast feedback)
npm run cy:run -- --spec "cypress/e2e/qc/qcDashboard.cy.js"

# Run full E2E suite (CI/CD ONLY - slow)
npm run cy:run
```

**Example Cypress Test**:

```javascript
describe("User Story 1: View Analyzer Compliance Status", () => {
  beforeEach(() => {
    cy.login("admin", "password"); // Use cy.session() for caching
    cy.visit("/analyzers/qc");
  });

  it("should display color-coded compliance status for analyzers", () => {
    // Setup: Create test data via API
    cy.request("POST", "/rest/qc/test-data/setup", {
      analyzers: [
        { id: "A1", name: "Hematology", status: "COMPLIANT" },
        { id: "A2", name: "Chemistry", status: "REJECTED" },
      ],
    });

    // Navigate to dashboard
    cy.visit("/analyzers/qc");

    // Assert: Compliance tiles visible with correct status
    cy.get('[data-testid="compliance-tile-A1"]').should("be.visible");
    cy.get('[data-testid="compliance-tile-A1"]').should("contain", "COMPLIANT");
    cy.get('[data-testid="compliance-tile-A1"]')
      .find(".status-icon-green")
      .should("exist");

    cy.get('[data-testid="compliance-tile-A2"]').should("contain", "REJECTED");
    cy.get('[data-testid="compliance-tile-A2"]')
      .find(".status-icon-red")
      .should("exist");

    // Cleanup
    cy.request("POST", "/rest/qc/test-data/cleanup");
  });
});
```

**Cypress Best Practices**:

- ✅ Run individual test files during development (max 5-10 test cases)
- ✅ Use `data-testid` selectors (most stable)
- ✅ Use `cy.intercept()` for reliable API waiting
- ✅ Use `cy.session()` for login caching (10-20x faster)
- ✅ Review browser console logs after each run
- ❌ NO arbitrary time delays (`cy.wait(5000)`)

### Code Formatting (MANDATORY before commit)

```bash
# Backend formatting
mvn spotless:apply

# Frontend formatting
cd frontend && npm run format && cd ..

# Verify formatting (fails if not formatted)
mvn spotless:check
cd frontend && npm run format:check && cd ..
```

## Common Issues & Troubleshooting

### Issue 1: Build Fails with Java Version Error

**Symptom**:

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
(default-compile) on project OpenELIS-Global: Fatal error compiling: invalid target release: 21
```

**Solution**:

```bash
# Verify Java version
java -version  # Must show "21.x.x"

# Use SDKMAN for automatic switching
sdk env

# Or manually set JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
```

### Issue 2: Tests Run Despite -DskipTests Flag

**Symptom**: Failsafe integration tests still execute during build

**Solution**: Use BOTH flags:

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### Issue 3: LazyInitializationException in Controllers

**Symptom**:

```
org.hibernate.LazyInitializationException: could not initialize proxy - no Session
```

**Cause**: Controller accessing entity relationships after transaction closed

**Solution**: Service must compile all data within transaction using JOIN FETCH:

```java
// ✅ CORRECT - Service layer
@Transactional
public Map<String, Object> getDashboardData(String analyzerId) {
    String hql = "SELECT a FROM Analyzer a " +
                 "LEFT JOIN FETCH a.qcControlLots lots " +
                 "WHERE a.id = :analyzerId";

    Analyzer analyzer = query.setParameter("analyzerId", analyzerId).getSingleResult();

    // Compile all data within transaction
    Map<String, Object> result = new HashMap<>();
    result.put("analyzerName", analyzer.getName());
    result.put("lotCount", analyzer.getQcControlLots().size());
    return result;  // Return complete data structure
}
```

### Issue 4: Cypress Test Flakiness

**Symptom**: Tests pass/fail randomly

**Solution**:

```javascript
// ❌ BAD - Arbitrary wait
cy.wait(5000);
cy.get('[data-testid="button"]').click();

// ✅ GOOD - Use retry-ability
cy.get('[data-testid="button"]').should("be.visible").click();

// ✅ GOOD - Wait for API response
cy.intercept("POST", "/rest/qc/results").as("createResult");
cy.get('[data-testid="submit"]').click();
cy.wait("@createResult").its("response.statusCode").should("eq", 200);
```

### Issue 5: Carbon Chart Not Rendering

**Symptom**: Blank area where chart should appear

**Solution**:

```javascript
// Ensure Carbon Charts CSS imported
import "@carbon/charts/styles.css";

// Verify data structure matches Carbon Charts format
const chartData = [
  { group: "QC Result", runNumber: 1, value: 100 }, // ✅ Correct
  // NOT { x: 1, y: 100 }  // ❌ Wrong
];
```

### Issue 6: Docker Container Fails to Start

**Symptom**:

```
Error response from daemon: Conflict. The container name "/openelis-webapp" is already in use
```

**Solution**:

```bash
# Stop and remove existing containers
docker compose -f dev.docker-compose.yml down

# Remove volumes (WARNING: destroys database data)
docker compose -f dev.docker-compose.yml down -v

# Restart
docker compose -f dev.docker-compose.yml up -d
```

## Development Workflow

### TDD Workflow (Red-Green-Refactor)

1. **Red**: Write failing test first

   ```bash
   # Create test class
   touch src/test/java/org/openelisglobal/qc/service/Rule1_3sEvaluatorTest.java

   # Write test that fails
   mvn test -Dtest=Rule1_3sEvaluatorTest  # Expected: FAIL
   ```

2. **Green**: Write minimal code to pass test

   ```bash
   # Implement evaluator
   touch src/main/java/org/openelisglobal/qc/service/evaluator/Rule1_3sEvaluator.java

   # Run test again
   mvn test -Dtest=Rule1_3sEvaluatorTest  # Expected: PASS
   ```

3. **Refactor**: Improve code quality while keeping tests green
   ```bash
   # Refactor implementation
   # Run test to ensure still passing
   mvn test -Dtest=Rule1_3sEvaluatorTest  # Expected: PASS
   ```

### Pre-Commit Checklist

- [ ] Format code:
      `mvn spotless:apply && cd frontend && npm run format && cd ..`
- [ ] Build passes: `mvn clean install -DskipTests -Dmaven.test.skip=true`
- [ ] Unit tests pass: `mvn test`
- [ ] Frontend tests pass: `cd frontend && npm test && cd ..`
- [ ] E2E test passes (individual file):
      `npm run cy:run -- --spec "cypress/e2e/qc/qcDashboard.cy.js"`
- [ ] No hardcoded strings (React Intl used)
- [ ] Liquibase changesets for schema changes
- [ ] Constitution compliance verified

### Creating Pull Request

```bash
# Push feature branch
git push origin 003-westgard-qc

# Create PR targeting 'develop' branch (NOT main)
# PR title: "003-westgard-qc: Implement Westgard Rules QC Compliance"
# Attach UI screenshots for UI changes
```

## Additional Resources

- **Constitution**: `.specify/memory/constitution.md`
- **Feature Spec**: `specs/003-westgard-qc/spec.md`
- **Implementation Plan**: `specs/003-westgard-qc/plan.md`
- **Data Model**: `specs/003-westgard-qc/data-model.md`
- **Research**: `specs/003-westgard-qc/research.md`
- **API Contracts**: `specs/003-westgard-qc/contracts/`
- **Testing Roadmap**: `.specify/guides/testing-roadmap.md`
- **Carbon Design System**: https://carbondesignsystem.com/
- **OpenELIS Carbon Guide**:
  https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838

## Getting Help

- **GitHub Discussions**:
  https://github.com/DIGI-UW/OpenELIS-Global-2/discussions
- **Weekly Developer Sync**: Thursdays 2PM UTC (check calendar)
- **Slack**: #openelis-dev channel
- **Email**: dev@openelis-global.org
