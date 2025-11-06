# [PROJECT NAME] Development Guidelines

Auto-generated from all feature plans. Last updated: [DATE]

**Constitution**: See `.specify/memory/constitution.md` for non-negotiable
development principles.

## Active Technologies

[EXTRACTED FROM ALL PLAN.MD FILES]

**OpenELIS Global 3.0 Stack**:

- Backend: Java 21 + Spring Boot 3.x + Hibernate 6.x + JPA + PostgreSQL 14+
- Frontend: React 17 + **Carbon Design System v1.15** (OFFICIAL UI framework)
- FHIR: HAPI FHIR R4 v6.6.2 + IHE mCSD profile
- Testing: JUnit 5 + Mockito (backend), Jest + RTL + Cypress (frontend)
- Build: Maven 3.8+ (backend), npm (frontend), Docker Compose (deployment)

## Project Structure

```text
[ACTUAL STRUCTURE FROM PLANS]
```

**OpenELIS Backend Pattern**: `org.openelisglobal.{module}.{layer}`

- Layers: valueholder (JPA entities) → dao (data access) → service (business
  logic) → controller (REST) → form (DTOs)

## Commands

[ONLY COMMANDS FOR ACTIVE TECHNOLOGIES]

**OpenELIS Development Commands**:

```bash
# Backend formatting + build
mvn spotless:apply && mvn clean install -DskipTests -Dmaven.test.skip=true

# Frontend formatting + dev server
cd frontend && npm run format && npm start

# Run E2E tests
cd frontend && npm run cy:run

# Hot reload backend (rebuild + restart container)
mvn clean install -DskipTests && docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

## Code Style

[LANGUAGE-SPECIFIC, ONLY FOR LANGUAGES IN USE]

**OpenELIS Conventions**:

- **Java**: Use Spotless formatter (`tools/OpenELIS_java_formatter.xml`), NO
  native SQL in code
- **React**: Use Prettier, ALL UI components from `@carbon/react`, ALL strings
  via React Intl
- **FHIR**: Extend `FhirTransformService` for entity↔FHIR conversion, sync via
  `FhirPersistanceService`
- **Database**: Liquibase changesets ONLY (NO direct DDL/DML)
- **Tests**: JUnit 5 for backend, Jest + Cypress for frontend, >70% coverage
  goal

## Recent Changes

[LAST 3 FEATURES AND WHAT THEY ADDED]

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
