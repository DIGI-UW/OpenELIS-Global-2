# Implementation Plan: Analytical Laboratory Workflow

**Branch**: `281-analytical-lab-workflow` | **Date**: 2025-12-14 | **Spec**:
`specs/281-analytical-lab-workflow/spec.md` **Input**: Feature specification
from `/specs/281-analytical-lab-workflow/spec.md`

## Summary

Deliver analytical lab workflow across bioanalytical and pharmaceutical units
with multi-source intake, role-based test assignment, result capture (manual +
instrument CSV import), reporting, and retention storage. Reuse notebook/page
patterns from OGC-51 while adding new analytical entities (assignments, results,
dissolution, reports, retention) and integrating with SampleStorageService for
post-test handling. Ensure Carbon-based UI, React Intl strings, 5-layer Spring
architecture, Liquibase migrations, and tests (JUnit 4, Jest/Cypress where
applicable).

## Technical Context

**Language/Version**: Java 21 (Spring 6.2.2) backend; React 17 frontend  
**Primary Dependencies**: Spring MVC + Hibernate/JPA; Liquibase; SWR/React
Router; Carbon Design System; React Intl; CSV import utilities from OGC-51 bulk
framework  
**Storage**: PostgreSQL 14 via JPA entities + Liquibase changesets;
SampleStorageService integration for retention assignments  
**Testing**: JUnit 4 + Mockito + BaseWebContextSensitiveTest; Jest + React
Testing Library; Cypress (feature-specific files only during dev)  
**Target Platform**: Tomcat 10 WAR deployment for backend; web SPA for
frontend  
**Project Type**: Web (backend + frontend)  
**Performance Goals**: Batch ops ≤30s for 50 samples; HPLC CSV import 100
samples <3m; report generation 100 samples <60s  
**Constraints**: Carbon-only UI; Intl for all strings; @Transactional only in
services; eager data compilation to avoid lazy load issues; no direct SQL
(Liquibase)  
**Scale/Scope**: Typical batches 50–100 samples; concurrent work by ≥5 analysts;
multi-page notebook flow (reception → assignment → analysis → reporting →
retention)

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- Carbon Design System required for all UI; no other UI frameworks.
- React Intl for every user-facing string (en + fr minimum for new keys).
- Strict 5-layer architecture (Valueholder → DAO → Service → Controller → Form).
- @Transactional only in services; controllers must not traverse lazy relations.
- All schema changes via Liquibase XML with rollback.
- Tests required: JUnit 4 unit/integration, Jest/RTL for UI logic, Cypress for
  workflows touched.
- Configuration-driven variation; avoid country/unit-specific forks.
- Security: RBAC for analytical operations; audit trail for
  assignments/results/storage.

## Project Structure

### Documentation (this feature)

```text
specs/281-analytical-lab-workflow/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
```

### Source Code (repository root)

```text
# Backend (Spring MVC, WAR)
src/main/java/org/openelisglobal/analytical/
├── valueholder/          # AnalyticalTestAssignment, AnalyticalResult, DissolutionResult, AnalyticalReport, RetentionSample
├── dao/                  # DAO interfaces + impls
├── service/              # @Service transactional layer
├── controller/           # REST controllers (no @Transactional)
└── form/                 # DTOs/forms

src/main/resources/liquibase/analytical/   # Liquibase changesets + rollbacks
src/main/resources/                        # Messages, validation

src/test/java/org/openelisglobal/analytical/
├── service/      # JUnit 4 + Mockito unit tests
├── controller/   # BaseWebContextSensitiveTest integration tests
└── dao/          # BaseWebContextSensitiveTest DAO tests

# Frontend (React 17 + Carbon)
frontend/src/components/analytical/        # Reusable widgets (grids, forms)
frontend/src/pages/analytical/             # Notebook pages (reception, assignment, analysis, reporting, retention)
frontend/src/services/analytical/          # API hooks (SWR) + helpers
frontend/src/languages/                    # New intl keys (en.json, fr.json)
frontend/src/__tests__/ or components/.../__tests__/  # Jest + RTL tests
frontend/cypress/e2e/analytical.cy.js      # Targeted E2E spec (per page/batch)
```

**Structure Decision**: Web app with backend + frontend modules following
existing OpenELIS layout; new analytical domain under
`org.openelisglobal.analytical` and `frontend/src/*/analytical`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed                             | Simpler Alternative Rejected Because |
| --------- | -------------------------------------- | ------------------------------------ |
| None      | All gates satisfied under constitution | N/A                                  |
