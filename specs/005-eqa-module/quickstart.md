# Quickstart: External Quality Assurance (EQA) Module

**Date**: 2025-11-18 **Feature**: External Quality Assurance (EQA) Module
**Branch**: `005-eqa-module` **Scope**: P1 (EQA Sample Entry, Alerts), P2
(Distribution, Results, IC/QC), P3 (FHIR Submission, Config)

## Prerequisites

**CRITICAL: Java Version**

- **Java 21 LTS** (OpenJDK/Temurin) - **MANDATORY** per constitution
- **NOT compatible** with Java 8, 11, or 17 - build will fail
- Verify: `java -version` should show `openjdk version "21.x.x"`
- For SDKMAN users: `.sdkmanrc` file in project root auto-switches to Java 21

**Other Prerequisites**

- OpenELIS Global 3.0 development environment running (see
  [dev_setup.md](../../../docs/dev_setup.md))
- PostgreSQL 14+ database accessible
- Maven 3.8+
- Node.js 16+, npm
- Docker + Docker Compose
- HAPI FHIR R4 server running at `https://fhir.openelis.org:8443/fhir/`

**Setup Java 21** (if needed):

```bash
# With SDKMAN (recommended)
sdk install java 21.0.5-tem
cd /path/to/OpenELIS-Global-2
sdk env  # Activates Java 21 from .sdkmanrc

# Or download from: https://adoptium.net/temurin/releases/?version=21
```

## CRITICAL: Test-First Development

**This feature follows strict Test-Driven Development (TDD)**. You MUST write
tests BEFORE implementation code.

### TDD Workflow (Red-Green-Refactor)

1. **RED**: Write a failing test

   - Write test for the behavior you want
   - Run test → it should FAIL (code doesn't exist yet)
   - Verify test fails for the right reason

2. **GREEN**: Make the test pass

   - Write minimal implementation code
   - Run test → it should PASS
   - Don't write extra code beyond what's needed

3. **REFACTOR**: Improve code quality

   - Clean up implementation
   - Remove duplication
   - Improve naming, structure
   - Run tests → all should still PASS

4. **Repeat** for next feature/behavior

### Development Order for This Feature

**Step 1: Write ORM Validation Tests** (framework config → test)

```bash
# Create test file (per Constitution v1.8.1, Section V.4)
src/test/java/org/openelisglobal/eqa/EQAHibernateMappingValidationTest.java

# IMPORTANT: Use JUnit 4 (NOT JUnit 5)
import org.junit.Test;  # Correct
import org.junit.Assert.*;  # Correct
# NOT: import org.junit.jupiter.api.Test;  Wrong

# Write ORM validation test for all 5 new entities
@Test
public void testAllEQAEntityMappingsLoadSuccessfully() {
    // Validate EQAProgram, EQAProgramTest, EQADistribution,
    // EQAResult, SampleEQA all load without errors
    // Executes in <5 seconds, no database required
}

# Run test → Should validate JPA annotation configuration
mvn test -Dtest="EQAHibernateMappingValidationTest"
```

**Step 2: Write Backend Unit Tests** (spec → test)

```bash
# Create test file for Z-score statistics
src/test/java/org/openelisglobal/eqa/service/EQAStatisticsServiceTest.java

# Write test methods based on data-model.md validation rules
@RunWith(MockitoJUnitRunner.class)
public class EQAStatisticsServiceTest {

    @Test
    public void testCalculateZScore_ValidDataset_ReturnsCorrectScore() {
        // Given: Known dataset of 10 participant results
        BigDecimal result = new BigDecimal("105.0");
        BigDecimal target = new BigDecimal("100.0");
        BigDecimal sd = new BigDecimal("2.5");

        // When: Calculate Z-score
        BigDecimal zScore = statisticsService.calculateZScore(result, target, sd);

        // Then: Z = (105 - 100) / 2.5 = 2.0
        assertEquals(new BigDecimal("2.00000"), zScore);
    }

    @Test
    public void testClassifyPerformance_Acceptable() {
        // |Z| ≤ 2.0 = Acceptable
        assertEquals(EQAPerformanceStatus.ACCEPTABLE,
            statisticsService.classifyPerformance(new BigDecimal("1.5")));
    }

    @Test
    public void testClassifyPerformance_Questionable() {
        // 2.0 < |Z| ≤ 3.0 = Questionable
        assertEquals(EQAPerformanceStatus.QUESTIONABLE,
            statisticsService.classifyPerformance(new BigDecimal("2.5")));
    }

    @Test
    public void testClassifyPerformance_Unacceptable() {
        // |Z| > 3.0 = Unacceptable
        assertEquals(EQAPerformanceStatus.UNACCEPTABLE,
            statisticsService.classifyPerformance(new BigDecimal("3.5")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateStatistics_LessThan5Participants_ThrowsException() {
        // Minimum 5 participants required
        List<BigDecimal> results = Arrays.asList(
            new BigDecimal("100"), new BigDecimal("101"),
            new BigDecimal("99"), new BigDecimal("102"));
        statisticsService.calculateStatistics(results);
    }
}

# Run test → FAILS (service doesn't exist yet)
mvn test -Dtest="EQAStatisticsServiceTest"
```

**Step 3: Write Backend Integration Tests** (spec → test)

```bash
# Create test file
src/test/java/org/openelisglobal/eqa/controller/EQAProgramRestControllerTest.java

# Write test methods based on eqa-api.yaml contract
@Test
public void testCreateProgram_ValidInput_Returns201() throws Exception {
    mockMvc.perform(post("/rest/eqa/programs")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"WHO Malaria EQA\",\"description\":\"Annual malaria proficiency testing\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("WHO Malaria EQA"))
        .andExpect(jsonPath("$.isActive").value(true));
}

@Test
public void testCreateDistribution_ValidInput_Returns201() throws Exception {
    mockMvc.perform(post("/rest/eqa/distributions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"eqaProgramId\":\"1\",\"distributionName\":\"Malaria-2025-Q1\",\"deadline\":\"2025-06-30T23:59:59\",\"participantOrganizationIds\":[\"10\",\"11\",\"12\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"));
}

# Run test → FAILS (controller doesn't exist yet)
mvn test -Dtest="EQAProgramRestControllerTest"
```

**Step 4: Write Frontend Unit Tests** (spec → test)

```bash
# Create test file
frontend/src/components/eqa/__tests__/EQASampleEntry.test.jsx

# Write test methods for EQA sample entry behavior
test('should disable demographics when EQA checkbox selected', () => {
    render(<EQASampleEntry />);
    const eqaCheckbox = screen.getByTestId('eqa-sample-checkbox');
    fireEvent.click(eqaCheckbox);
    expect(screen.getByTestId('patient-first-name')).toBeDisabled();
    expect(screen.getByTestId('patient-last-name')).toBeDisabled();
});

test('should show EQA fields on Program tab when EQA selected', () => {
    render(<EQASampleEntry isEQA={true} />);
    expect(screen.getByTestId('eqa-program-select')).toBeInTheDocument();
    expect(screen.getByTestId('eqa-provider-org-select')).toBeInTheDocument();
    expect(screen.getByTestId('eqa-deadline-picker')).toBeInTheDocument();
    expect(screen.getByTestId('eqa-priority-select')).toBeInTheDocument();
});

# Run test → FAILS (component doesn't exist yet)
cd frontend && npm test -- EQASampleEntry.test.jsx
```

**Step 5: Implement Code to Pass Tests**

```bash
# Only NOW write implementation code
# Follow milestone order: M1 → M2 → M3 → M4 → M5 → M6 → M7 → M8

# After each implementation, run tests
mvn test  # Backend
cd frontend && npm test  # Frontend

# All tests should pass before moving to next component
```

**Step 6: Write E2E Tests** (after implementation)

```bash
# Create Cypress test
frontend/cypress/e2e/eqaSampleEntry.cy.js

# Write test based on US1 user story
describe("EQA Sample Registration (US1)", () => {
    it("Should register EQA sample with all required fields", () => {
        cy.session('admin', () => { cy.login('admin', 'adminADMIN!'); });
        cy.visit('/sample-entry');
        cy.get('[data-testid="eqa-sample-checkbox"]').click();
        // Verify demographics disabled
        cy.get('[data-testid="patient-first-name"]').should('be.disabled');
        // Fill EQA fields
        cy.get('[data-testid="eqa-program-select"]').select('WHO Malaria EQA');
        cy.get('[data-testid="eqa-provider-org-select"]').select('WHO');
        cy.get('[data-testid="eqa-provider-sample-id"]').type('WHO-2025-001');
        cy.get('[data-testid="eqa-deadline-picker"]').type('2025-06-30');
        cy.get('[data-testid="eqa-priority-select"]').select('STANDARD');
        // Save
        cy.get('[data-testid="save-button"]').click();
        cy.get('.cds--toast-notification--success').should('be.visible');
    });
});

# Run E2E test individually (Constitution V.5)
cd frontend && npm run cy:run -- --spec "cypress/e2e/eqaSampleEntry.cy.js"
```

**REMEMBER**: Never write implementation code without a failing test first!

## Quick Navigation

- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Testing User Scenarios](#testing-user-scenarios)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)

---

## Backend Setup

### 1. Database Migration

Liquibase changesets automatically run on application startup. Verify migration
success:

```bash
# Connect to PostgreSQL
psql -U clinlims -d clinlims

# Check EQA tables created
\dt clinlims.eqa_*
\dt clinlims.sample_eqa

# Expected tables:
# eqa_program
# eqa_program_test
# eqa_distribution
# eqa_result
# sample_eqa

# Verify sequences created
\ds *eqa*

# Verify AlertType extension (stored as VARCHAR, no schema change needed)
SELECT DISTINCT alert_type FROM clinlims.alert;

# Exit psql
\q
```

**Rollback** (if needed):

```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=eqa-001
```

### 2. Build Backend

From repository root:

```bash
# Clean build (skip tests for faster iteration)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Build with unit tests
mvn clean install

# Build only main module (after initial full build)
mvn clean install -pl :openelisglobal -am -DskipTests -Dmaven.test.skip=true
```

**Expected Output**:

```
[INFO] BUILD SUCCESS
[INFO] Total time: ~2-3 min
```

### 3. Run Backend Tests

```bash
# Run all EQA unit tests
mvn test -Dtest="org.openelisglobal.eqa.**"

# Run specific test class
mvn test -Dtest="org.openelisglobal.eqa.service.EQAStatisticsServiceTest"

# Run ORM validation tests (must complete in <5s)
mvn test -Dtest="org.openelisglobal.eqa.EQAHibernateMappingValidationTest"

# Run integration tests (requires database)
mvn verify -Dtest="org.openelisglobal.eqa.controller.**"

# Check test coverage (JaCoCo)
mvn jacoco:report
# Open: target/site/jacoco/index.html
```

**Coverage Goals**: >80% backend, 100% for Z-score calculations and alert timing

### 4. Start Backend Dev Server

```bash
# From repository root
docker compose -f dev.docker-compose.yml up -d

# Watch logs
docker logs -f oe.openelis.org

# Verify backend started successfully
# Look for: "Started OpenELISApplication in X seconds"
```

**Access Points**:

- **Backend API**: https://localhost/api/OpenELIS-Global/rest/eqa/programs
- **Legacy UI**: https://localhost/api/OpenELIS-Global/
- **React UI**: https://localhost/

### 5. Hot Reload Backend Changes

After making Java code changes:

```bash
# Rebuild WAR
mvn clean install -DskipTests -Dmaven.test.skip=true

# Recreate backend container only
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# Watch logs for startup confirmation
docker logs -f oe.openelis.org
```

---

## Frontend Setup

### 1. Install Dependencies

```bash
cd frontend

# Install dependencies (first time only)
npm install

# @carbon/charts-react already in dependencies (for Levey-Jennings charts)
# Cypress already installed (existing OpenELIS E2E framework)
```

### 2. Add Internationalization Keys

Edit translation files to add EQA-specific message keys:

**frontend/src/languages/en.json** (additions):

```json
{
  "eqa.sample.checkbox": "EQA Sample",
  "eqa.sample.badge": "EQA",
  "eqa.program.label": "EQA Program",
  "eqa.provider.label": "EQA Provider Organization",
  "eqa.provider.sampleId": "Provider Sample ID",
  "eqa.participant.id": "Participant ID",
  "eqa.deadline.label": "Testing Deadline",
  "eqa.priority.label": "Priority",
  "eqa.priority.standard": "Standard",
  "eqa.priority.urgent": "Urgent",
  "eqa.priority.critical": "Critical",
  "eqa.distribution.create": "Create EQA Distribution",
  "eqa.distribution.name": "Distribution Name",
  "eqa.distribution.participants": "Participating Organizations",
  "eqa.distribution.deadline": "Submission Deadline",
  "eqa.distribution.status.draft": "Draft",
  "eqa.distribution.status.prepared": "Prepared",
  "eqa.distribution.status.shipped": "Shipped",
  "eqa.distribution.status.completed": "Completed",
  "eqa.results.manual.entry": "Manual Result Entry",
  "eqa.results.batch.import": "Batch Import",
  "eqa.results.statistics": "Statistical Analysis",
  "eqa.results.zscore": "Z-Score",
  "eqa.results.acceptable": "Acceptable",
  "eqa.results.questionable": "Questionable",
  "eqa.results.unacceptable": "Unacceptable",
  "eqa.results.report.generate": "Generate Report",
  "eqa.results.minimum.participants": "Minimum 5 participants required for analysis",
  "alerts.dashboard.title": "Alerts Dashboard",
  "alerts.summary.critical": "Critical Alerts",
  "alerts.summary.eqa.deadlines": "EQA Deadlines Today",
  "alerts.summary.overdue.stat": "Overdue STAT Orders",
  "alerts.summary.expiring.samples": "Expiring Samples",
  "alerts.acknowledge.button": "Acknowledge",
  "alerts.acknowledge.comment.required": "Resolution comment is required",
  "alerts.filter.myAlerts": "My Alerts",
  "alerts.filter.type": "Alert Type",
  "alerts.filter.severity": "Severity",
  "qc.leveyJennings.title": "Levey-Jennings Chart",
  "qc.westgard.rules": "Westgard Rules",
  "qc.signature.sign": "Sign Report",
  "qc.frequency.config": "QC Frequency Configuration"
}
```

**frontend/src/languages/fr.json** (additions):

```json
{
  "eqa.sample.checkbox": "Echantillon EQA",
  "eqa.sample.badge": "EQA",
  "eqa.program.label": "Programme EQA",
  "eqa.provider.label": "Organisation fournisseur EQA",
  "eqa.provider.sampleId": "ID echantillon du fournisseur",
  "eqa.participant.id": "ID du participant",
  "eqa.deadline.label": "Date limite de test",
  "eqa.priority.label": "Priorite",
  "eqa.priority.standard": "Standard",
  "eqa.priority.urgent": "Urgent",
  "eqa.priority.critical": "Critique",
  "eqa.distribution.create": "Creer une distribution EQA",
  "eqa.distribution.name": "Nom de la distribution",
  "eqa.distribution.participants": "Organisations participantes",
  "eqa.distribution.deadline": "Date limite de soumission",
  "eqa.distribution.status.draft": "Brouillon",
  "eqa.distribution.status.prepared": "Prepare",
  "eqa.distribution.status.shipped": "Expedie",
  "eqa.distribution.status.completed": "Termine",
  "eqa.results.manual.entry": "Saisie manuelle des resultats",
  "eqa.results.batch.import": "Importation par lot",
  "eqa.results.statistics": "Analyse statistique",
  "eqa.results.zscore": "Score Z",
  "eqa.results.acceptable": "Acceptable",
  "eqa.results.questionable": "Discutable",
  "eqa.results.unacceptable": "Inacceptable",
  "eqa.results.report.generate": "Generer le rapport",
  "eqa.results.minimum.participants": "Minimum 5 participants requis pour l'analyse",
  "alerts.dashboard.title": "Tableau de bord des alertes",
  "alerts.summary.critical": "Alertes critiques",
  "alerts.summary.eqa.deadlines": "Echeances EQA aujourd'hui",
  "alerts.summary.overdue.stat": "Commandes STAT en retard",
  "alerts.summary.expiring.samples": "Echantillons expirants",
  "alerts.acknowledge.button": "Accuser reception",
  "alerts.acknowledge.comment.required": "Le commentaire de resolution est requis",
  "alerts.filter.myAlerts": "Mes alertes",
  "alerts.filter.type": "Type d'alerte",
  "alerts.filter.severity": "Severite",
  "qc.leveyJennings.title": "Graphique de Levey-Jennings",
  "qc.westgard.rules": "Regles de Westgard",
  "qc.signature.sign": "Signer le rapport",
  "qc.frequency.config": "Configuration de frequence CQ"
}
```

### 3. Run Frontend Dev Server

```bash
# From frontend/ directory
cd frontend
npm start

# Frontend starts with hot reload at https://localhost/
# Changes to .jsx files auto-reload in browser
```

### 4. Run Frontend Tests

```bash
# Run all tests
npm test

# Run EQA component tests only
npm test -- components/eqa

# Run alert dashboard tests
npm test -- components/alerts

# Run QC component tests
npm test -- components/qc

# Run tests in watch mode
npm test -- --watch

# Run E2E tests individually (Constitution V.5)
npm run cy:run -- --spec "cypress/e2e/eqaSampleEntry.cy.js"
npm run cy:run -- --spec "cypress/e2e/alertsDashboard.cy.js"
npm run cy:run -- --spec "cypress/e2e/eqaDistribution.cy.js"
npm run cy:run -- --spec "cypress/e2e/eqaResults.cy.js"

# Run E2E tests with Cypress UI
npx cypress open
```

### 5. Lint and Format

```bash
# Backend formatting (MANDATORY before every commit)
mvn spotless:apply

# Frontend formatting (MANDATORY before every commit)
cd frontend && npm run format && cd ..
```

---

## Testing User Scenarios

### US1: Register and Process Incoming EQA Sample

**Workflow**: Register EQA sample through modified sample entry

1. **Navigate**: https://localhost/sample-entry

2. **Select EQA Sample**:

   - Check "EQA Sample" checkbox on Patient Information tab
   - Verify all demographic fields become disabled with "N/A" placeholders

3. **Fill EQA Fields** (Program tab):

   - Select EQA Provider Organization (e.g., "WHO")
   - Select EQA Program (e.g., "WHO Malaria EQA")
   - Enter Provider Sample ID (e.g., "WHO-2025-001")
   - Optionally enter Participant ID
   - Set Testing Deadline (e.g., "2025-06-30")
   - Select Priority: "Standard"

4. **Complete Sample Entry**:

   - Proceed through Sample tab (select sample type, tests)
   - Complete Order tab
   - Click "Save"

5. **Verify in Work Queue**:
   - Navigate to logbook/work queue
   - Verify EQA badge (Carbon Tag) displayed on sample
   - Use filter to show "EQA Only" samples

**API Verification**:

```bash
# Check SampleEQA record created
curl -k https://localhost/api/OpenELIS-Global/rest/eqa/samples?programId=1

# Expected: JSON array with EQA sample metadata
```

---

### US2: Monitor EQA Deadlines and Alerts

**Workflow**: View and manage alerts dashboard

1. **Navigate**: https://localhost/alerts-dashboard

2. **View Summary Tiles**:

   - Critical Alerts count (red)
   - EQA Deadlines Today count
   - Overdue STAT Orders count
   - Expiring Samples count

3. **View Alert Table**:

   - Alerts sorted by severity (critical first)
   - Color coding: Red (critical), Orange (warning), Blue (info)
   - Columns: Lab #, Type, Message, Severity, Created, Lab Section

4. **Filter Alerts**:

   - Click "My Alerts" to filter by your lab section
   - Filter by type (EQA Deadline, Sample Expiration, etc.)
   - Search by lab number

5. **Acknowledge Alert**:
   - Click "Acknowledge" on a critical alert
   - Enter required resolution comment
   - System logs user ID and timestamp

**API Verification**:

```bash
# Get all alerts
curl -k https://localhost/api/OpenELIS-Global/rest/alerts?status=OPEN

# Get alert summary counts
curl -k https://localhost/api/OpenELIS-Global/rest/alerts/summary

# Acknowledge an alert
curl -k -X PUT https://localhost/api/OpenELIS-Global/rest/alerts/{alertId}/acknowledge \
  -H "Content-Type: application/json" \
  -d '{"comment":"Sample being processed urgently"}'
```

---

### US3: Create and Distribute EQA Samples

**Workflow**: Create distribution for participating laboratories

1. **Navigate**: https://localhost/eqa/distributions/create

2. **Step 1 - Program & Details**:

   - Select EQA Program
   - Enter Distribution Name (e.g., "Malaria-2025-Q1")
   - Set Distribution Date and Deadline

3. **Step 2 - Participants**:

   - Multi-select participating organizations (minimum 2)
   - Review list of selected participants

4. **Step 3 - Confirmation**:

   - Review distribution summary
   - Click "Create Distribution"

5. **Generate Barcodes**:

   - System creates one order per participating organization
   - Click "Generate Barcodes" for batch label generation

6. **Ship Distribution**:
   - Organize samples into shipment boxes
   - Advance status: DRAFT → PREPARED → SHIPPED

**API Verification**:

```bash
# Create distribution
curl -k -X POST https://localhost/api/OpenELIS-Global/rest/eqa/distributions \
  -H "Content-Type: application/json" \
  -d '{"eqaProgramId":"1","distributionName":"Malaria-2025-Q1","deadline":"2025-06-30T23:59:59","participantOrganizationIds":["10","11","12"]}'

# Get distribution details
curl -k https://localhost/api/OpenELIS-Global/rest/eqa/distributions/{id}
```

---

### US4: Collect Results and Analyze Performance

**Workflow**: Enter results and view statistical analysis

1. **Manual Result Entry**:

   - Navigate to EQA distribution detail
   - Click "Enter Results"
   - Select participant organization
   - Enter result value for each test

2. **Batch Import** (CSV):

   - Click "Batch Import"
   - Upload CSV file with columns: participant_org_id, test_id, result_value
   - Review validation results
   - Confirm import

3. **View Statistics** (requires 5+ participant results):

   - Navigate to distribution statistics
   - View: Mean, Standard Deviation, Target Value
   - View per-participant: Z-score, Performance Classification
   - Performance thresholds:
     - |Z| <= 2.0: Acceptable (green)
     - 2.0 < |Z| <= 3.0: Questionable (yellow)
     - |Z| > 3.0: Unacceptable (red)

4. **Generate PDF Report**:
   - Click "Generate Report"
   - Report includes all participants, Z-scores, comparative analysis
   - Must generate in <5 seconds for 50 participants

**API Verification**:

```bash
# Submit result manually
curl -k -X POST https://localhost/api/OpenELIS-Global/rest/eqa/results \
  -H "Content-Type: application/json" \
  -d '{"eqaDistributionId":"1","participantOrganizationId":"10","testId":"5","resultValue":"105.0","submissionMethod":"MANUAL"}'

# Get statistics for distribution
curl -k https://localhost/api/OpenELIS-Global/rest/eqa/distributions/{id}/statistics

# Generate PDF report
curl -k https://localhost/api/OpenELIS-Global/rest/eqa/distributions/{id}/report \
  -o report.pdf
```

---

## API Reference

All EQA endpoints are documented in the OpenAPI specification:
[contracts/eqa-api.yaml](contracts/eqa-api.yaml)

### Key Endpoints

| Method   | Path                                      | Description              | Milestone |
| -------- | ----------------------------------------- | ------------------------ | --------- |
| GET/POST | `/rest/eqa/programs`                      | List/create EQA programs | M7        |
| GET/PUT  | `/rest/eqa/programs/{id}`                 | Get/update program       | M7        |
| GET      | `/rest/eqa/samples`                       | Query EQA samples        | M1        |
| POST     | `/rest/eqa/distributions`                 | Create distribution      | M5        |
| GET      | `/rest/eqa/distributions/{id}`            | Get distribution details | M5        |
| PUT      | `/rest/eqa/distributions/{id}/status`     | Advance status           | M5        |
| GET      | `/rest/eqa/distributions/{id}/statistics` | Get stats                | M6        |
| POST     | `/rest/eqa/results`                       | Submit result            | M6        |
| POST     | `/rest/eqa/results/batch`                 | Batch import             | M6        |
| GET      | `/rest/alerts`                            | List alerts (extended)   | M3        |
| GET      | `/rest/alerts/summary`                    | Alert summary counts     | M3        |
| PUT      | `/rest/alerts/{id}/acknowledge`           | Acknowledge alert        | M3        |
| POST     | `/rest/eqa/samples/{id}/submit`           | FHIR submission          | M7        |
| GET/PUT  | `/rest/qc/instruments/{id}/frequency`     | QC frequency             | M8        |
| GET      | `/rest/qc/westgard-rules`                 | List Westgard rules      | M8        |
| GET/POST | `/rest/qc/controls/{id}/results`          | QC results               | M8        |
| POST     | `/rest/qc/reports/{id}/sign`              | E-signature              | M8        |

---

## Troubleshooting

### Backend Issues

**Liquibase migration fails**:

```bash
# Check Liquibase status
mvn liquibase:status

# View changelog history
psql -U clinlims -d clinlims -c "SELECT * FROM databasechangelog WHERE id LIKE 'eqa%';"

# Manual rollback (if needed)
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Clear locks (if hung)
mvn liquibase:clearCheckSums
```

**JPA entity mapping errors**:

```bash
# Run ORM validation test to diagnose
mvn test -Dtest="EQAHibernateMappingValidationTest" -X

# Common issues:
# - Missing @Column annotation
# - Wrong column name in @JoinColumn
# - Missing sequence generator registration
# - Schema name missing ("clinlims")
```

**Alert scheduler not firing**:

```bash
# Check scheduler configuration
docker logs oe.openelis.org | grep "EQADeadlineAlertScheduler"

# Verify @Scheduled annotation picked up
docker logs oe.openelis.org | grep "Scheduling"

# Check for existing alerts
psql -U clinlims -d clinlims -c "SELECT * FROM clinlims.alert WHERE alert_type LIKE 'EQA%';"
```

**FHIR sync fails**:

```bash
# Verify FHIR server running
docker ps | grep fhir

# Check FHIR server logs
docker logs fhir.openelis.org

# Verify FHIR server accessible
curl -k https://fhir.openelis.org:8443/fhir/metadata

# Test FHIR submission
curl -k -X POST https://fhir.openelis.org:8443/fhir/DiagnosticReport \
  -H "Content-Type: application/fhir+json" \
  -d @test-diagnostic-report.json
```

### Frontend Issues

**EQA fields not appearing on sample entry**:

```bash
# Check console for errors
# Open browser DevTools: F12 → Console tab

# Verify React Intl message keys loaded
# In browser console:
# intl.messages['eqa.sample.checkbox']

# Verify EQA checkbox data-testid present
# In Elements tab, search for: data-testid="eqa-sample-checkbox"

# Clear browser cache and reload
# Ctrl+Shift+R (Chrome/Firefox)
```

**Carbon chart not rendering (Levey-Jennings)**:

```bash
# Verify @carbon/charts-react installed
cd frontend && npm ls @carbon/charts-react

# Check for missing CSS imports
# Ensure @carbon/charts-react/dist/styles.css is imported

# Check browser console for chart errors
```

**API requests failing (CORS, 401, etc.)**:

```bash
# Check session cookie present
# Browser DevTools → Application tab → Cookies
# Look for JSESSIONID cookie

# Re-login if session expired
# Navigate to: https://localhost/login

# Check API endpoint URL
# Verify: https://localhost/api/OpenELIS-Global/rest/eqa/... (NOT http://)
```

### Test Issues

**Unit tests failing**:

```bash
# Run tests with verbose output
mvn test -Dtest="EQAStatisticsServiceTest" -X

# Common issues:
# - Using JUnit 5 imports instead of JUnit 4
# - Wrong Mockito version (project uses 2.21.0)
# - Missing @RunWith(MockitoJUnitRunner.class)
```

**E2E tests failing (Cypress)**:

```bash
# Run with headed browser (see what's happening)
npx cypress run --headed --spec "cypress/e2e/eqaSampleEntry.cy.js"

# Debug in Cypress UI
npx cypress open

# Check video recordings
ls cypress/videos/

# View screenshots of failures
ls cypress/screenshots/
```

---

## Development Tips

### Quick Iteration Cycle

**Backend changes**:

1. Edit Java file
2. `mvn clean install -DskipTests -Dmaven.test.skip=true`
3. `docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org`
4. Test in browser

**Frontend changes**:

1. Edit .jsx file
2. Webpack auto-reloads in browser
3. Test immediately (no rebuild needed)

### Useful Queries

```sql
-- View all EQA programs
SELECT id, name, description, is_active FROM clinlims.eqa_program;

-- View EQA samples with program info
SELECT se.id, s.accession_number, ep.name AS program,
       se.eqa_provider_sample_id, se.eqa_deadline, se.eqa_priority
FROM clinlims.sample_eqa se
JOIN clinlims.sample s ON s.id = se.sample_id
LEFT JOIN clinlims.eqa_program ep ON ep.id = se.eqa_program_id
ORDER BY se.eqa_deadline;

-- View distribution with participant count
SELECT ed.id, ed.distribution_name, ed.status, ed.deadline,
       COUNT(DISTINCT er.participant_organization_id) AS result_count
FROM clinlims.eqa_distribution ed
LEFT JOIN clinlims.eqa_result er ON er.eqa_distribution_id = ed.id
GROUP BY ed.id, ed.distribution_name, ed.status, ed.deadline;

-- View EQA results with Z-scores
SELECT er.id, o.organization_name AS participant,
       er.result_value, er.target_value, er.z_score,
       er.performance_status, er.submission_method
FROM clinlims.eqa_result er
JOIN clinlims.organization o ON o.id = er.participant_organization_id
WHERE er.eqa_distribution_id = {distribution_id}
ORDER BY er.z_score DESC;

-- View active alerts by type
SELECT alert_type, severity, COUNT(*) AS count
FROM clinlims.alert
WHERE status = 'OPEN'
GROUP BY alert_type, severity
ORDER BY severity, alert_type;

-- View overdue EQA samples
SELECT s.accession_number, se.eqa_deadline, ep.name AS program,
       se.eqa_priority, se.eqa_provider_sample_id
FROM clinlims.sample_eqa se
JOIN clinlims.sample s ON s.id = se.sample_id
JOIN clinlims.eqa_program ep ON ep.id = se.eqa_program_id
WHERE se.eqa_deadline < NOW() AND se.is_eqa_sample = true
ORDER BY se.eqa_deadline;
```

### Carbon Design System Resources

- **Components**: https://react.carbondesignsystem.com/
- **Charts** (for LJ charts): https://charts.carbondesignsystem.com/
- **Icons**: https://www.carbondesignsystem.com/guidelines/icons/library/
- **Design Tokens**:
  https://www.carbondesignsystem.com/guidelines/color/overview/

---

## Milestone Order

Follow the milestone dependency graph when implementing:

```
M1 (Backend Entities) ──→ M3 (Alerts Backend) ──→ M4 (Alerts Frontend)
     │                                                    ↑
     │                    M2 (EQA Sample Frontend) ───────┘
     │
     └──→ M5 (Distribution) ──→ M6 (Results & Analysis) ──→ M7 (FHIR & Config)
                                       │
                                       └──→ M8 (IC/QC Features)
```

**Parallel work**: M1 and M2 can be developed simultaneously by different
developers. M3 and M5 can also be parallelized after M1 completes.

---

## Next Steps

After completing quickstart:

1. **Review Design Artifacts**: [plan.md](plan.md),
   [data-model.md](data-model.md),
   [contracts/eqa-api.yaml](contracts/eqa-api.yaml)
2. **Generate Tasks**: Run `/speckit.tasks` to create detailed implementation
   tasks
3. **Start M1**: Create Liquibase changesets and JPA entities
4. **Run Full Test Suite**: `mvn clean install` (backend) + `npm test`
   (frontend)
5. **Check Coverage**: Review JaCoCo report for >80% backend coverage
6. **FHIR Validation**: Verify entity `fhir_uuid` fields sync to FHIR server

---

**Support**: For issues, see [Troubleshooting](#troubleshooting) or OpenELIS
documentation at https://docs.openelis-global.org/
