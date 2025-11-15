---
description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/` **Prerequisites**:
plan.md (required), spec.md (required for user stories), research.md,
data-model.md, contracts/

**Tests**: The examples below include test tasks. Tests are MANDATORY for all
user stories (per Constitution V and Testing Roadmap). Test tasks MUST appear
BEFORE implementation tasks to enforce TDD workflow.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can
be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T004 Setup database schema and migrations framework
- [ ] T005 [P] Implement authentication/authorization framework
- [ ] T006 [P] Setup API routing and middleware structure
- [ ] T007 Create base models/entities that all stories depend on
- [ ] T008 Configure error handling and logging infrastructure
- [ ] T009 Setup environment configuration management

**Checkpoint**: Foundation ready - user story implementation can now begin in
parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T010 [P] [US1] Unit test for [ServiceName] in
      src/test/java/org/openelisglobal/{module}/service/[ServiceName]Test.java
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito)
      for detailed patterns - Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
      for quick reference - **TDD Workflow**: Write test FIRST (RED), then
      implement (GREEN), then refactor - **Test Slicing**: Use
      `@RunWith(MockitoJUnitRunner.class)` for isolated unit tests (NOT
      `@SpringBootTest`) - **Mocking**: Use `@Mock` (NOT `@MockBean`) for
      isolated unit tests - **Test Data**: Use builders/factories (NOT hardcoded
      values) - **Coverage Goal**: >80% (measured via JaCoCo) - **SDD
      Checkpoint**: Must pass after Phase 2 (Services)
- [ ] T010a [P] [US1] ORM validation test in
      src/test/java/org/openelisglobal/{module}/HibernateMappingValidationTest.java -
      Reference:
      [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4)
      for detailed patterns - Build SessionFactory using
      `config.addAnnotatedClass()` - Validate all entity mappings load without
      errors - MUST execute in <5 seconds (per Constitution V.4) - MUST NOT
      require database connection - **SDD Checkpoint**: Must pass after Phase 1
      (Entities)
- [ ] T010b [P] [US1] DAO test for [DAO] in
      src/test/java/org/openelisglobal/{module}/dao/[DAO]Test.java (Template:
      `.specify/templates/testing/DataJpaTestDao.java.template`) - Reference:
      [Testing Roadmap - @DataJpaTest (DAO/Repository Layer)](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer)
      for detailed patterns - Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
      for quick reference - **Test Slicing**: Use `@DataJpaTest` for DAO testing
      (NOT `@SpringBootTest` - faster execution) - **Test Data**: Use
      `TestEntityManager` (NOT JdbcTemplate) for test data setup - **Transaction
      Management**: Automatic rollback (no manual cleanup needed) - **Test HQL
      queries**: CRUD operations, findBy\* methods, relationship queries -
      **Coverage Goal**: >80% (measured via JaCoCo)
- [ ] T011 [P] [US1] Controller test for REST endpoint in
      src/test/java/org/openelisglobal/{module}/controller/[ControllerName]Test.java
      (Template:
      `.specify/templates/testing/WebMvcTestController.java.template`) -
      Reference:
      [Testing Roadmap - @WebMvcTest (Controller Layer)](.specify/guides/testing-roadmap.md#webmvctest-controller-layer)
      for detailed patterns - Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
      for quick reference - **Test Slicing**: Use `@WebMvcTest` for controller
      testing (NOT `@SpringBootTest` - faster execution) - **Mocking**: Use
      `@MockBean` (NOT `@Mock`) for Spring context mocking - **HTTP Testing**:
      Use `MockMvc` for HTTP request/response testing - **Test Data**: Use
      builders/factories for test data - **Coverage Goal**: >80% (measured via
      JaCoCo) - **SDD Checkpoint**: Must pass after Phase 3 (Controllers)
- [ ] T011a [P] [US1] Frontend unit test for [ComponentName] in
      frontend/src/components/{feature}/[ComponentName].test.jsx (Template:
      `.specify/templates/testing/JestComponent.test.jsx.template`) - Reference:
      [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests)
      for detailed patterns - Reference:
      [Jest Best Practices](.specify/guides/jest-best-practices.md) for quick
      reference - **TDD Workflow**: Write test FIRST (RED), then implement
      (GREEN), then refactor - **Import Order**: React → Testing Library →
      userEvent → jest-dom → Intl → Router → Component → Utils → Messages -
      **Mocks BEFORE imports**: Jest hoisting requires mocks before imports -
      **userEvent PREFERRED**: Use `userEvent.click()`, `userEvent.type()` for
      user interactions (NOT `fireEvent`) - **Async Testing**: Use `waitFor`
      with `queryBy*` (NOT `getBy*`) or `findBy*` for async elements - **DON'T
      use setTimeout**: Use `waitFor` instead (has retry logic) - **Carbon
      Components**: Use `userEvent`, `waitFor` for portals, `within()` for
      scoped queries - **Test Behavior**: Test user-visible behavior, NOT
      implementation details - **Edge Cases**: Test null, empty, boundary
      values - **Coverage Goal**: >70% (measured via Jest) - **SDD Checkpoint**:
      Must pass after Phase 4 (Frontend)
- [ ] T011b [P] [US1] Cypress E2E test in frontend/cypress/e2e/[feature].cy.js
      (Template: `.specify/templates/testing/CypressE2E.cy.js.template`) -
      Reference:
      [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
      for functional requirements - Reference:
      [Testing Roadmap - Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing)
      for detailed patterns - Reference:
      [Cypress Best Practices](.specify/guides/cypress-best-practices.md) for
      quick reference - Use data-testid selectors (PREFERRED) - Use cy.session()
      for login state (10-20x faster) - Use API-based test data setup (10x
      faster than UI) - Set viewport before visit - Set up intercepts BEFORE
      actions - Use .should() for retry-ability (no arbitrary cy.wait()) - Focus
      on happy path user workflows (not implementation details) - Run
      individually during development:
      `npm run cy:run -- --spec "cypress/e2e/[feature].cy.js"` - Post-run review
      (MANDATORY): Review console logs, screenshots, test output

### Implementation for User Story 1

> **CRITICAL: Implementation tasks depend on test tasks. Tests must pass
> before** > **proceeding to next phase checkpoint.**

- [ ] T012 [P] [US1] Create [Entity1] model in src/models/[entity1].py
- [ ] T013 [P] [US1] Create [Entity2] model in src/models/[entity2].py
- [ ] T014 [US1] Implement [Service] in src/services/[service].py (depends on
      T012, T013)
- [ ] T015 [US1] Implement [endpoint/feature] in src/[location]/[file].py
- [ ] T016 [US1] Add validation and error handling
- [ ] T017 [US1] Add logging for user story 1 operations

**Checkpoint Validation**: At this point, User Story 1 should be fully
functional and testable independently. ALL tests from T010-T011b MUST pass
before proceeding to next phase.

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T018 [P] [US2] Unit test for [ServiceName] in
      src/test/java/org/openelisglobal/{module}/service/[ServiceName]Test.java
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito)
      and
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md) -
      **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` (NOT
      `@SpringBootTest`) - **Mocking**: Use `@Mock` (NOT `@MockBean`)
- [ ] T019 [P] [US2] Controller test for REST endpoint in
      src/test/java/org/openelisglobal/{module}/controller/[ControllerName]Test.java
      (Template:
      `.specify/templates/testing/WebMvcTestController.java.template`) -
      Reference:
      [Testing Roadmap - @WebMvcTest](.specify/guides/testing-roadmap.md#webmvctest-controller-layer)
      and
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md) -
      **Test Slicing**: Use `@WebMvcTest` (NOT `@SpringBootTest`) - **Mocking**:
      Use `@MockBean` (NOT `@Mock`)

### Implementation for User Story 2

- [ ] T020 [P] [US2] Create [Entity] model in src/models/[entity].py
- [ ] T021 [US2] Implement [Service] in src/services/[service].py
- [ ] T022 [US2] Implement [endpoint/feature] in src/[location]/[file].py
- [ ] T023 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work
independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T024 [P] [US3] Contract test for [endpoint] in
      tests/contract/test\_[name].py
- [ ] T025 [P] [US3] Integration test for [user journey] in
      tests/integration/test\_[name].py

### Implementation for User Story 3

- [ ] T026 [P] [US3] Create [Entity] model in src/models/[entity].py
- [ ] T027 [US3] Implement [Service] in src/services/[service].py
- [ ] T028 [US3] Implement [endpoint/feature] in src/[location]/[file].py

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX [P] Additional unit tests (if requested) in tests/unit/
- [ ] TXXX Security hardening
- [ ] TXXX Run quickstart.md validation

---

## Phase N+1: Constitution Compliance Verification (OpenELIS Global 3.0)

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

- [ ] TXXX **Configuration-Driven**: Verify no country-specific code branches
      introduced
- [ ] TXXX **Carbon Design System**: Audit UI - confirm @carbon/react used
      exclusively (NO Bootstrap/Tailwind)
- [ ] TXXX **FHIR/IHE Compliance**: Validate FHIR resources against R4 profiles
      (if applicable)
- [ ] TXXX **Layered Architecture**: Verify 5-layer pattern followed
      (Valueholder→DAO→Service→Controller→Form)
- [ ] TXXX **Test Coverage**: Run coverage report - confirm >70% for new code
      (JaCoCo/Jest)
- [ ] TXXX **Schema Management**: Verify ALL database changes use Liquibase
      changesets (NO direct SQL)
- [ ] TXXX **Internationalization**: Audit UI strings - confirm React Intl used
      for ALL text (no hardcoded strings)
- [ ] TXXX **Security & Compliance**: Verify RBAC, audit trail (sys_user_id +
      lastupdated), input validation

**Verification Commands**:

```bash
# Backend: Code formatting (MUST run before each commit) + build + tests
mvn spotless:apply && mvn spotless:check && mvn clean install

# Frontend: Formatting (MUST run before each commit) + E2E tests
cd frontend && npm run format
# Run E2E tests individually (per Constitution V.5):
npm run cy:run -- --spec "cypress/e2e/[feature].cy.js"
# Full suite only in CI/CD: npm run cy:run

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user
  stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No
  dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate
  with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate
  with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if
  team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if tests requested):
Task: "Contract test for [endpoint] in tests/contract/test_[name].py"
Task: "Integration test for [user journey] in tests/integration/test_[name].py"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] model in src/models/[entity1].py"
Task: "Create [Entity2] model in src/models/[entity2].py"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break
  independence
