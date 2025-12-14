# Quickstart: Pharmaceuticals Laboratory Workflow

## Prerequisites

- Java 21, Maven 3.8+, Node.js 16+.
- Docker + docker compose for dev stack.
- Run builds with tests skipped during iteration:
  `mvn clean install -DskipTests -Dmaven.test.skip=true`.

## Setup

```bash
cd /Users/macbookm1/uw/OpenELIS-Global-2
mvn clean install -DskipTests -Dmaven.test.skip=true
cd frontend && npm install && cd ..
docker compose -f dev.docker-compose.yml up -d
```

## Development Pointers

- Backend: add pharma module under
  `backend/src/main/java/org/openelisglobal/pharmaceutical/` following 5-layer
  pattern.
- Database: use Liquibase changesets in
  `backend/src/main/resources/liquibase/pharmaceutical/`; include rollback.
- Frontend: add pages/components under `frontend/src/pages/pharmaceuticals/` and
  `frontend/src/components/pharmaceuticals/`; use Carbon + React Intl.
- Reuse NotebookService/Page templates (OGC-51) and Storage services; avoid new
  APIs unless required.
- Tests: JUnit4 + Mockito for services; BaseWebContextSensitiveTest for
  controllers/DAO; Jest/RTL for UI; Cypress per feature file (not full suite).
- Formatting: `mvn spotless:apply` and `cd frontend && npm run format`.

## Running

```bash
# Backend (skip tests for speed)
mvn clean install -DskipTests -Dmaven.test.skip=true
# Frontend dev server
cd frontend && npm start
```

## Key Artifacts

- Spec: `specs/283-pharmaceuticals-lab/spec.md`
- Plan: `specs/283-pharmaceuticals-lab/plan.md`
- Research: `specs/283-pharmaceuticals-lab/research.md`
- Data model: `specs/283-pharmaceuticals-lab/data-model.md`
- Contracts: `specs/283-pharmaceuticals-lab/contracts/`
