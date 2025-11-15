# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See
`.specify/templates/commands/plan.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from
research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS
CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [single/web/mobile - determines source structure]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps
or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory,
offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS
CLARIFICATION]

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../.specify/memory/constitution.md):

- [ ] **Configuration-Driven**: No country-specific code branches planned
- [ ] **Carbon Design System**: UI uses @carbon/react exclusively (NO
      Bootstrap/Tailwind)
- [ ] **FHIR/IHE Compliance**: External data integrates via FHIR R4 + IHE
      profiles
- [ ] **Layered Architecture**: Backend follows 5-layer pattern
      (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files -
    legacy exempt until refactored)
  - **Transaction management MUST be in service layer only** - NO
    `@Transactional` annotations on controller methods
- [ ] **Test Coverage**: Unit + ORM validation (if applicable) + integration +
      E2E tests planned (>80% backend, >70% frontend coverage goal per
      Constitution V)
  - E2E tests MUST follow Cypress best practices (Constitution V.5):
    - Run tests individually during development (not full suite)
    - Browser console logging enabled and reviewed after each run
    - Video recording disabled by default
    - Post-run review of console logs and screenshots required
    - Use data-testid selectors (PREFERRED)
    - Use cy.session() for login state (10-20x faster)
    - Use API-based test data setup (10x faster than UI)
    - See
      [Testing Roadmap](.specify/guides/testing-roadmap.md#cypress-e2e-testing)
      for comprehensive Cypress guidance
- [ ] **Schema Management**: Database changes via Liquibase changesets only
- [ ] **Internationalization**: All UI strings use React Intl (no hardcoded
      text)
- [ ] **Security & Compliance**: RBAC, audit trail, input validation included

**Complexity Justification Required If**:

- Adding custom CSS framework alongside Carbon
- Using native SQL instead of JPA/Hibernate
- Hardcoding country-specific logic instead of configuration
- Bypassing FHIR for external integration
- Skipping test implementation

## Testing Strategy

**Reference**: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)

**MANDATORY**: Every plan MUST include a complete testing strategy that
references the Testing Roadmap and documents test coverage goals, test types,
data management, and checkpoint validations.

### Coverage Goals

- **Backend**: >80% code coverage (measured via JaCoCo)
- **Frontend**: >70% code coverage (measured via Jest)
- **Critical Paths**: 100% coverage (authentication, authorization, data
  validation)

### Test Types

Document which test types will be used for this feature:

- [ ] **Unit Tests**: Service layer business logic (JUnit 4 + Mockito)
  - Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`
  - **Reference**:
    [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Coverage Goal**: >80% (measured via JaCoCo)
  - **SDD Checkpoint**: After Phase 2 (Services), all unit tests MUST pass
  - **TDD Workflow**: Red-Green-Refactor cycle for complex logic
  - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` for isolated unit
    tests (NOT `@SpringBootTest`)
  - **Mocking**: Use `@Mock` (NOT `@MockBean`) for isolated unit tests
- [ ] **DAO Tests**: Persistence layer testing (@DataJpaTest)
  - Template: `.specify/templates/testing/DataJpaTestDao.java.template`
  - **Reference**:
    [Testing Roadmap - @DataJpaTest (DAO/Repository Layer)](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Test Slicing**: Use `@DataJpaTest` for DAO testing (NOT
    `@SpringBootTest` - faster execution)
  - **Test Data**: Use `TestEntityManager` (NOT JdbcTemplate) for test data
    setup
  - **Transaction Management**: Automatic rollback (no manual cleanup needed)
- [ ] **Controller Tests**: REST API endpoints (@WebMvcTest)
  - Template: `.specify/templates/testing/WebMvcTestController.java.template`
  - **Reference**:
    [Testing Roadmap - @WebMvcTest (Controller Layer)](.specify/guides/testing-roadmap.md#webmvctest-controller-layer)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Test Slicing**: Use `@WebMvcTest` for controller testing (NOT
    `@SpringBootTest` - faster execution)
  - **Mocking**: Use `@MockBean` (NOT `@Mock`) for Spring context mocking
  - **HTTP Testing**: Use `MockMvc` for HTTP request/response testing
- [ ] **ORM Validation Tests**: Entity mapping validation (Constitution V.4)
  - **Reference**:
    [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4)
    for detailed patterns
  - **SDD Checkpoint**: After Phase 1 (Entities), ORM validation tests MUST pass
  - **Requirements**: MUST execute in <5 seconds, MUST NOT require database
    connection
- [ ] **Integration Tests**: Full workflow testing (@SpringBootTest)
  - **Reference**:
    [Testing Roadmap - @SpringBootTest (Full Integration)](.specify/guides/testing-roadmap.md#springboottest-full-integration)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Test Slicing**: Use `@SpringBootTest` only when full application context
    is required
  - **Transaction Management**: Use `@Transactional` for automatic rollback
    (preferred)
  - **SDD Checkpoint**: After Phase 3 (Controllers), integration tests MUST pass
- [ ] **Frontend Unit Tests**: React component logic (Jest + React Testing
      Library)
  - Template: `.specify/templates/testing/JestComponent.test.jsx.template`
  - **Reference**:
    [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests)
    for detailed patterns
  - **Reference**: [Jest Best Practices](.specify/guides/jest-best-practices.md)
    for quick reference
  - **Coverage Goal**: >70% (measured via Jest)
  - **SDD Checkpoint**: After Phase 4 (Frontend), all unit tests MUST pass
  - **TDD Workflow**: Red-Green-Refactor cycle for complex logic
- [ ] **E2E Tests**: Critical user workflows (Cypress)
  - Template: `.specify/templates/testing/CypressE2E.cy.js.template`
  - **Reference**:
    [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
    for functional requirements
  - **Reference**:
    [Testing Roadmap - Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing)
    for detailed patterns
  - **Reference**:
    [Cypress Best Practices](.specify/guides/cypress-best-practices.md) for
    quick reference

### Test Data Management

Document how test data will be created and cleaned up:

- **Backend**:

  - **Unit Tests (JUnit 4 + Mockito)**:
    - [ ] Use builders/factories for test data (NOT hardcoded values)
    - [ ] Use mock data builders/factories for reusable test data
    - [ ] Test edge cases (null, empty, boundary values)
  - **DAO Tests (@DataJpaTest)**:
    - [ ] Use `TestEntityManager` for test data setup (NOT JdbcTemplate)
    - [ ] Use builders/factories for test entities
    - [ ] Automatic transaction rollback (no manual cleanup needed)
  - **Controller Tests (@WebMvcTest)**:
    - [ ] Use builders/factories for test data
    - [ ] Mock service layer (use `@MockBean`)
  - **Integration Tests (@SpringBootTest)**:
    - [ ] Use builders/factories for test data
    - [ ] Use `@Transactional` for automatic rollback (preferred)
    - [ ] Use `@Sql` scripts for complex data setup (if needed)
  - **Legacy Integration Tests (BaseWebContextSensitiveTest)**:
    - [ ] Use DBUnit for complex test data (via
          `executeDataSetWithStateManagement()`)
    - [ ] Manual cleanup in `@After` methods (when `@Transactional` doesn't
          work)

- **Frontend**:
  - **E2E Tests (Cypress)**:
    - [ ] Use API-based setup via `cy.request()` (NOT slow UI interactions) -
          10x faster
    - [ ] Use fixtures with `cy.intercept()` for consistent test data
    - [ ] Use `cy.session()` for login state (10-20x faster than per-test login)
    - [ ] Use custom Cypress commands for reusable setup/cleanup
  - **Unit Tests (Jest)**:
    - [ ] Use mock data builders/factories (per Medium article - use generic
          cases)
    - [ ] Use `setupApiMocks()` helper for consistent API mocking
    - [ ] Test edge cases (null, empty, boundary values)
    - [ ] Use `renderWithIntl()` helper for consistent component rendering

### Checkpoint Validations

Document which tests must pass at each SDD phase checkpoint:

- [ ] **After Phase 1 (Entities)**: ORM validation tests must pass
- [ ] **After Phase 2 (Services)**: Backend unit tests must pass
- [ ] **After Phase 3 (Controllers)**: Integration tests must pass
- [ ] **After Phase 4 (Frontend)**: Frontend unit tests (Jest) AND E2E tests
      (Cypress) must pass

### TDD Workflow

- [ ] **TDD Mandatory**: Red-Green-Refactor cycle for complex logic
- [ ] **Test Tasks First**: Test tasks MUST appear before implementation tasks
      in tasks.md
- [ ] **Checkpoint Enforcement**: Tests must pass before proceeding to next
      phase

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation                  | Why Needed         | Simpler Alternative Rejected Because |
| -------------------------- | ------------------ | ------------------------------------ |
| [e.g., 4th project]        | [current need]     | [why 3 projects insufficient]        |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient]  |
