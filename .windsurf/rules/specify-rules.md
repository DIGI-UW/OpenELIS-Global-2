# OpenELIS-Global-2 Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-06

**Constitution**: See `.specify/memory/constitution.md` for non-negotiable
development principles.

## Active Technologies

- Java 21, React 18 (upgrading from 17.0.2) + Spring Framework 6.2.2,
  @carbon/react latest v11 (upgrading from 1.15.0), @carbon/icons-react latest
  v11, Hibernate 5.6.15, HAPI FHIR R4 6.6.2 (spec/016-unified-app-navigation)

**OpenELIS Global Stack**:

- Backend: Java 21 + Spring Framework 6.2.x (Traditional Spring MVC) +
  Hibernate + JPA + PostgreSQL 14+
- Frontend: React 17 + **Carbon Design System v1.15** (OFFICIAL UI framework)
- FHIR: HAPI FHIR R4 v6.6.2 + IHE mCSD profile
- Testing: JUnit 4 + Mockito (backend), Jest + React Testing Library (frontend
  unit), Cypress (frontend E2E)
- Build: Maven 3.8+ (backend), npm (frontend), Docker Compose (deployment)

## Project Structure

```text
src/
tests/
```

**OpenELIS Backend Pattern**: `org.openelisglobal.{module}.{layer}`

- Layers: valueholder (JPA entities) → dao (data access) → service (business
  logic) → controller (REST) → form (DTOs)

## Commands

# Add commands for Java 21, React 18 (upgrading from 17.0.2)

**OpenELIS Development Commands**:

```bash
# Backend formatting + build
mvn spotless:apply && mvn clean install -DskipTests -Dmaven.test.skip=true

# Frontend formatting + dev server
cd frontend && npm run format && npm start

# Run E2E tests
cd frontend && npm run cy:run

# Hot reload backend (rebuild + restart container)
mvn clean install -DskipTests -Dmaven.test.skip=true && docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

## Code Style

Java 21, React 18 (upgrading from 17.0.2): Follow standard conventions

**OpenELIS Conventions**:

- **Java**: Use Spotless formatter (`tools/OpenELIS_java_formatter.xml`), NO
  native SQL in code
- **React**: Use Prettier, ALL UI components from `@carbon/react`, ALL strings
  via React Intl
- **FHIR**: Extend `FhirTransformService` for entity↔FHIR conversion, sync via
  `FhirPersistanceService`
- **Database**: Liquibase changesets ONLY (NO direct DDL/DML)
- **Tests**: JUnit 4 for backend, Jest + Cypress for frontend, >80% backend
  coverage goal, >70% frontend coverage goal

## Recent Changes

- spec/016-unified-app-navigation: Added Java 21, React 18 (upgrading from
  17.0.2) + Spring Framework 6.2.2, @carbon/react latest v11 (upgrading from
  1.15.0), @carbon/icons-react latest v11, Hibernate 5.6.15, HAPI FHIR R4 6.6.2

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
