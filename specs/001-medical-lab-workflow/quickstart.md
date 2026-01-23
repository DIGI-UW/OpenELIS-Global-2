# Quickstart Guide: Medical Laboratory Workflow

**Feature**: 001-medical-lab-workflow **Branch**: `001-medical-lab-workflow`
**Purpose**: Step-by-step developer guide for implementing this feature

## Prerequisites

Before starting implementation, ensure you have:

1. **Development Environment**

   - Java 21 LTS (OpenJDK/Temurin)
   - Maven 3.8+
   - Node.js 18+ with npm
   - PostgreSQL 14+
   - Docker (for containerized development)

2. **Tools Configured**

   - SDKMAN for Java version management
   - IDE with Lombok support (IntelliJ/VSCode)
   - Git with branch checked out

3. **Required Reading**
   - [spec.md](spec.md) - Feature specification
   - [plan.md](plan.md) - Implementation plan
   - [data-model.md](data-model.md) - Entity definitions
   - [research.md](research.md) - Technical decisions

## Quick Setup

```bash
# 1. Checkout feature branch
git checkout 001-medical-lab-workflow

# 2. Verify Java version
java -version  # Should be 21.x

# 3. Build backend (skip tests initially)
mvn clean install -DskipTests -Dmaven.test.skip=true

# 4. Setup frontend
cd frontend
npm install
cd ..

# 5. Start development database
docker-compose -f dev.docker-compose.yml up -d db

# 6. Run application
mvn spring-boot:run -f pom.xml
```

## Implementation Order

Follow the milestones defined in [plan.md](plan.md):

### Phase 1: Backend Foundation

#### Step 1.1: Create Module Structure

Create the new `medlab` module:

```
src/main/java/org/openelisglobal/medlab/
├── valueholder/
├── dao/
├── service/
├── controller/rest/
└── form/
```

#### Step 1.2: Database Schema (Liquibase)

Create changesets in order:

```
src/main/resources/liquibase/3.x.x/
├── 001-quality-check.xml
├── 002-transport-packaging.xml
├── 003-environmental-reading.xml
├── 004-processing-record.xml
├── 005-qc-result.xml
├── 006-validation-record.xml
├── 007-disposal-record.xml
├── 008-equipment-usage-log.xml
└── 009-sample-allocation.xml
```

#### Step 1.3: First Entity (QualityCheck)

Follow TDD workflow:

```bash
# 1. Write failing test first
# Create: src/test/java/org/openelisglobal/medlab/service/SampleReceptionServiceTest.java

# 2. Run test (should fail)
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn test \
  -Dtest=SampleReceptionServiceTest

# 3. Implement service to make test pass
# Create: src/main/java/org/openelisglobal/medlab/service/SampleReceptionService.java

# 4. Run test again (should pass)
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn test \
  -Dtest=SampleReceptionServiceTest

# 5. Format code
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn spotless:apply
```

### Phase 2: Frontend Implementation

#### Step 2.1: Create Component Structure

```
frontend/src/components/medlab/
├── PatientRegistration.js
├── SampleCollection.js
├── SampleReception.js
├── QCDashboard.js
│   └── LeveyJenningsChart.js
├── ResultValidation.js
└── LabDashboard.js
```

#### Step 2.2: Add i18n Keys

Update `frontend/src/languages/en.json`:

```json
{
  "medlab.reception.title": "Sample Reception",
  "medlab.reception.qc.hemolysis": "Hemolysis",
  "medlab.reception.qc.lipemia": "Lipemia",
  "medlab.reception.qc.accept": "Accept Sample",
  "medlab.reception.qc.reject": "Reject Sample",
  "medlab.qc.chart.title": "Levey-Jennings Chart",
  "medlab.validation.approve": "Approve Result",
  "medlab.dashboard.tat.title": "Turnaround Time"
}
```

#### Step 2.3: First Component (SampleReception)

```jsx
// frontend/src/components/medlab/SampleReception.js
import React, { useState, useEffect } from "react";
import { useIntl } from "react-intl";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
} from "@carbon/react";

const SampleReception = () => {
  const intl = useIntl();
  // Component implementation...
};

export default SampleReception;
```

#### Step 2.4: Cypress E2E Test

```bash
# Run individual test during development
cd frontend
npm run cy:run -- --spec "cypress/e2e/medlabSampleReception.cy.js"
```

## Key Implementation Patterns

### 1. Layered Architecture (5 Layers)

```java
// Valueholder (Entity)
@Entity
@Table(name = "quality_check")
public class QualityCheck extends BaseObject<Integer> {
    // Fields, getters, setters
}

// DAO Interface
public interface QualityCheckDAO extends BaseDAO<QualityCheck, Integer> {
    List<QualityCheck> findBySampleItemId(Integer sampleItemId);
}

// DAO Implementation
@Component
public class QualityCheckDAOImpl extends BaseDAOImpl<QualityCheck, Integer>
    implements QualityCheckDAO {
    // Implementation
}

// Service Interface
public interface SampleReceptionService {
    QualityCheck performQualityCheck(QualityCheckForm form);
}

// Service Implementation
@Service
@Transactional  // Transactions ONLY in service layer
public class SampleReceptionServiceImpl implements SampleReceptionService {
    // All data compiled within transaction
}

// Controller
@RestController
@RequestMapping("/rest/medlab/reception")
public class SampleReceptionController {
    // NO @Transactional here
}
```

### 2. Quality Check Implementation

```java
// Service calculates QC criteria by sample type
public QualityCheck performQualityCheck(QualityCheckForm form) {
    QualityCheck qc = new QualityCheck();
    qc.setSampleItemId(form.getSampleItemId());

    // Get sample type criteria
    QCCriteria criteria = getQCCriteria(form.getSampleType());

    // Validate delay
    qc.setDelayPassed(form.getDelayMinutes() <= criteria.getMaxDelayMinutes());

    // Validate volume
    qc.setVolumePassed(form.getVolumeMl() >= criteria.getMinVolumeMl());

    // Determine overall status
    boolean passed = qc.isDelayPassed() && qc.isVolumePassed()
        && !form.isContamination() && !form.isLeakage();
    qc.setOverallStatus(passed ? "ACCEPTED" : "REJECTED");

    return qualityCheckDAO.save(qc);
}
```

### 3. Westgard Rules for QC

```java
// QualityControlService evaluates Westgard rules
public WestgardEvaluation evaluateWestgardRules(Integer testId, String qcLevel) {
    List<QCResult> results = qcResultDAO.findRecentResults(testId, qcLevel, 20);
    WestgardEvaluation eval = new WestgardEvaluation();

    // 1:3s Rule - Single control >3SD from mean (REJECT)
    if (Math.abs(results.get(0).getZScore()) > 3.0) {
        eval.addViolation("1:3s", "REJECT");
    }

    // 1:2s Rule - Single control >2SD from mean (WARNING)
    if (Math.abs(results.get(0).getZScore()) > 2.0) {
        eval.addViolation("1:2s", "WARNING");
    }

    // 2:2s Rule - Two consecutive >2SD same direction (REJECT)
    if (results.size() >= 2) {
        double z1 = results.get(0).getZScore();
        double z2 = results.get(1).getZScore();
        if (Math.abs(z1) > 2.0 && Math.abs(z2) > 2.0 && (z1 * z2 > 0)) {
            eval.addViolation("2:2s", "REJECT");
        }
    }

    // Additional rules: R:4s, 4:1s, 10x
    // ...

    return eval;
}
```

### 4. Levey-Jennings Chart Data

```java
// Service generates chart data
public LeveyJenningsData getLeveyJenningsData(Integer testId, String qcLevel, int days) {
    QCLot lot = qcLotDAO.findActiveByTestAndLevel(testId, qcLevel);
    List<QCResult> results = qcResultDAO.findByTestAndLevelSince(testId, qcLevel, days);

    LeveyJenningsData data = new LeveyJenningsData();
    data.setTargetValue(lot.getTargetValue());
    data.setSdValue(lot.getSdValue());
    data.setSd1High(lot.getTargetValue() + lot.getSdValue());
    data.setSd1Low(lot.getTargetValue() - lot.getSdValue());
    data.setSd2High(lot.getTargetValue() + 2 * lot.getSdValue());
    data.setSd2Low(lot.getTargetValue() - 2 * lot.getSdValue());
    data.setSd3High(lot.getTargetValue() + 3 * lot.getSdValue());
    data.setSd3Low(lot.getTargetValue() - 3 * lot.getSdValue());

    // Add data points with z-scores
    for (QCResult result : results) {
        data.addDataPoint(result.getResultDate(), result.getResultValue(),
            calculateZScore(result, lot));
    }

    return data;
}
```

### 5. Carbon Charts for Levey-Jennings

```jsx
// frontend/src/components/medlab/QCDashboard/LeveyJenningsChart.js
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";

const LeveyJenningsChart = ({ data }) => {
  const chartData = [
    // QC values
    ...data.dataPoints.map((p) => ({
      group: "QC Values",
      date: p.date,
      value: p.value,
    })),
    // Reference lines
    { group: "Target", date: data.dataPoints[0].date, value: data.targetValue },
    { group: "+2SD", date: data.dataPoints[0].date, value: data.sd2High },
    { group: "-2SD", date: data.dataPoints[0].date, value: data.sd2Low },
  ];

  const options = {
    title: "Levey-Jennings Chart",
    axes: {
      bottom: { title: "Date", mapsTo: "date", scaleType: "time" },
      left: { title: "Value", mapsTo: "value" },
    },
    curve: "curveMonotoneX",
    points: { enabled: true, radius: 4 },
  };

  return <LineChart data={chartData} options={options} />;
};
```

## Testing Requirements

### Backend Unit Tests

```bash
# Run all medlab tests
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn test \
  -Dtest="org.openelisglobal.medlab.**"

# Run specific test class
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn test \
  -Dtest=SampleReceptionServiceTest
```

### Frontend E2E Tests

```bash
# Individual test (development)
cd frontend
npm run cy:run -- --spec "cypress/e2e/medlabSampleReception.cy.js"

# Full suite (CI only)
npm run cy:run
```

## Pre-Commit Checklist

Before EVERY commit:

```bash
# 1. Format backend
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn spotless:apply

# 2. Format frontend
cd frontend && npm run format && cd ..

# 3. Run tests
JAVA_HOME=/Users/macbookm1/.sdkman/candidates/java/21.0.2-open mvn test

# 4. Verify no hardcoded strings (React Intl)
grep -r "\"[A-Z][a-z]" frontend/src/components/medlab/ | grep -v "intl"
```

## API Contract Reference

All API contracts are in `contracts/`:

| Contract                                                                 | Endpoints | Purpose                        |
| ------------------------------------------------------------------------ | --------- | ------------------------------ |
| [patient-registration.yaml](contracts/patient-registration.yaml)         | 6         | Patient/participant management |
| [sample-collection.yaml](contracts/sample-collection.yaml)               | 4         | Sample collection workflow     |
| [sample-reception-qc.yaml](contracts/sample-reception-qc.yaml)           | 6         | Reception QC checks            |
| [transport-packaging.yaml](contracts/transport-packaging.yaml)           | 4         | IATA PI650 compliance          |
| [sample-allocation.yaml](contracts/sample-allocation.yaml)               | 4         | Department routing             |
| [storage-management.yaml](contracts/storage-management.yaml)             | 6         | Hierarchical storage           |
| [environmental-monitoring.yaml](contracts/environmental-monitoring.yaml) | 6         | Temperature logging            |
| [sample-processing.yaml](contracts/sample-processing.yaml)               | 4         | Processing/aliquoting          |
| [testing-integration.yaml](contracts/testing-integration.yaml)           | 4         | Worklist/analyzers             |
| [quality-control.yaml](contracts/quality-control.yaml)                   | 6         | Westgard/Levey-Jennings        |
| [result-entry-validation.yaml](contracts/result-entry-validation.yaml)   | 6         | Result approval                |
| [reporting-dashboard.yaml](contracts/reporting-dashboard.yaml)           | 6         | KPIs/reports                   |
| [disposal-archiving.yaml](contracts/disposal-archiving.yaml)             | 4         | Disposal/biobank               |

## Common Issues & Solutions

### Issue: LazyInitializationException

**Solution**: Ensure services compile all data within the `@Transactional`
boundary. Don't pass lazy entities to controllers.

### Issue: Test skipping not working

**Solution**: Use BOTH flags:

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### Issue: Carbon Charts not rendering

**Solution**: Ensure CSS is imported:

```jsx
import "@carbon/charts/styles.css";
```

### Issue: i18n key not found

**Solution**: Add key to both `en.json` and `fr.json`.

## Next Steps

After completing this quickstart:

1. Run `/speckit.tasks` to generate detailed task breakdown
2. Implement milestones in order (M1 → M10)
3. Create PR for each milestone
4. Run `/speckit.analyze` for consistency validation
