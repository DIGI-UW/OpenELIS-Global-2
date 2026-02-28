# Quickstart: Feature 012 (GeneXpert-Focused)

## Goal

Implement and validate v1.2 profile/config features while keeping the existing
GeneXpert ASTM workflow green.

## Prerequisites

1. Java 21 active.
2. Submodules initialized.
3. Analyzer harness operational.

## 1) Start Harness Baseline

```bash
/restart-analyzer-harness --full-reset --build
```

Expected baseline:

- GeneXpert fixture analyzer `2013` exists.
- `test-connection` to fixture path succeeds from Analyzer UI.

## 2) Backend Iteration Loop

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
mvn test -Dtest=*Analyzer*Test
```

Focus areas per milestone:

- Profile library APIs and SemVer lineage policy.
- Runtime ASTM config APIs (connection role, QC rules, transforms, extraction,
  aggregation, flags).
- Pending code queue behavior.

## 3) Frontend Iteration Loop

```bash
cd frontend
npm test -- --watch=false
npm run format
```

Focus areas:

- Add/Edit analyzer profile selection and lab unit assignment.
- Mapping/simulator tab extensions.
- React Intl coverage for new strings (`en`, `fr` minimum).

## 4) GeneXpert Workflow Validation (Required Gate)

### 4.1 Fixture path (always required)

Run analyzer connection E2E:

```bash
cd frontend
npm run pw:test -- playwright/tests/analyzer-test-connection.spec.ts -g "GeneXpert test-connection succeeds via ASTM mock"
```

### 4.2 ASTM message simulation/push path

```bash
cd projects/analyzer-harness
./scripts/test-genexpert-astm.sh 1 bridge
```

Verify:

- Analyzer simulator/preview endpoint returns parsed fields and warnings.
- No patient/QC persistence from simulator-only preview.
- Pending unmapped code queue behavior respects cap and retention rules.

### 4.3 Optional real-device path (non-CI)

```bash
cd frontend
GENEXPERT_HOST=<ip> GENEXPERT_PORT=<port> npm run pw:test -- playwright/tests/analyzer-test-connection.spec.ts -g "Real GeneXpert"
```

## 5) Formatting and Final Checks

```bash
mvn spotless:apply
cd frontend && npm run format
```

Recommended before PR:

- Backend unit + integration tests for touched analyzer modules.
- Frontend Jest tests for touched analyzer components.
- Playwright GeneXpert fixture test.

## Done Criteria

1. All clarified FR/BR requirements for implemented milestone are covered by
   tests.
2. GeneXpert fixture test-connection and simulation workflow stays green.
3. No regression to existing 011 analyzer onboarding and mapping flows.
