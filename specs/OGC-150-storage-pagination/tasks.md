# Tasks: Sample Storage Pagination

**Branch**: `OGC-150-storage-pagination`  
**Date**: 2025-12-05  
**Last Updated**: 2025-12-05  
**Input**: Design documents from `/specs/OGC-150-storage-pagination/`

**Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)  
**Parent Feature**: [001-sample-storage](../001-sample-storage/tasks.md)  
**Type**: Performance Enhancement

## Implementation Status Overview

This document breaks down the implementation phases from `plan.md` into
actionable tasks following strict Test-Driven Development (TDD). Tests are
written BEFORE implementation code.

**Approach**: SINGLE PR (no separate milestones) per Constitution Principle IX -
feature requires <3 days effort.

### Phase Status Summary

| Phase   | Status        | Description                     | Tasks Complete | Tasks Remaining |
| ------- | ------------- | ------------------------------- | -------------- | --------------- |
| Phase 0 | [NOT STARTED] | Branch Setup & Prerequisites    | 0/2            | 2               |
| Phase 1 | [NOT STARTED] | Backend Tests (RED)             | 0/9            | 9               |
| Phase 2 | [NOT STARTED] | Backend Implementation (GREEN)  | 0/6            | 6               |
| Phase 3 | [NOT STARTED] | Frontend Tests (RED)            | 0/4            | 4               |
| Phase 4 | [NOT STARTED] | Frontend Implementation (GREEN) | 0/5            | 5               |
| Phase 5 | [NOT STARTED] | E2E Tests                       | 0/6            | 6               |
| Phase 6 | [NOT STARTED] | Polish & Verification           | 0/6            | 6               |

**Total Tasks**: 38

---

## User Story to Phase Mapping

| User Story                      | Priority | Phases                                        |
| ------------------------------- | -------- | --------------------------------------------- |
| US1: View Paginated Sample List | P1       | Phase 1-4 (Backend + Frontend implementation) |
| US2: Navigate Between Pages     | P1       | Phase 1-4 (Backend + Frontend implementation) |
| US3: Change Page Size           | P2       | Phase 1-4 (Backend + Frontend implementation) |
| All Stories                     | -        | Phase 5 (E2E Tests for all scenarios)         |

**Note**: All three user stories are implemented together since they share the
same backend pagination logic and frontend component. Separation would create
duplicate work.

---

## Implementation Dependencies

**Prerequisites**:

- ✅ Feature 001-sample-storage fully implemented and merged to `develop`
- ✅ `StorageDashboard.jsx` component exists
- ✅ `SampleStorageService` and `SampleStorageRestController` exist

**Sequential Phases** (cannot parallelize within single PR):

```
Phase 0 (Setup)
    ↓
Phase 1 (Backend Tests - RED)
    ↓
Phase 2 (Backend Implementation - GREEN)
    ↓
Phase 3 (Frontend Tests - RED)
    ↓
Phase 4 (Frontend Implementation - GREEN)
    ↓
Phase 5 (E2E Tests)
    ↓
Phase 6 (Polish & Verification)
```

---

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label ([US1], [US2], [US3])
- Include exact file paths in descriptions

---

## Phase 0: Branch Setup & Prerequisites [NOT STARTED]

**Purpose**: Create feature branch and verify prerequisites

**Checkpoint**: Branch created, prerequisites verified

### Tasks

- [ ] T001 Create feature branch `feat/OGC-150-storage-pagination` from
      `develop`
- [ ] T002 Verify prerequisites: Run
      `ls frontend/src/components/storage/StorageDashboard.jsx src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java`
      to confirm 001-sample-storage files exist

---

## Phase 1: Backend Tests (RED) - TDD [NOT STARTED]

**Purpose**: Write backend tests that define expected pagination behavior (tests
will FAIL initially)

**Duration**: 2 hours  
**User Stories**: US1, US2, US3 (all user stories share backend pagination
logic)

**Checkpoint**: 9 failing backend tests created

### Backend Unit Tests (JUnit 4 + Mockito)

- [ ] T003 [US1] Create test class
      `src/test/java/org/openelisglobal/storage/service/SampleStorageServiceImplTest.java`
      with imports (JUnit 4, Mockito, Spring Data Page/Pageable)
- [ ] T004 [US1] Write test
      `testGetSampleAssignments_WithPageable_ReturnsCorrectPageSize()` in
      `SampleStorageServiceImplTest.java` - verify page size matches request
- [ ] T005 [US1] Write test
      `testGetSampleAssignments_WithPageable_ReturnsTotalElements()` in
      `SampleStorageServiceImplTest.java` - verify total count correct
- [ ] T006 [US2] Write test
      `testGetSampleAssignments_FirstPage_ReturnsFirstNItems()` in
      `SampleStorageServiceImplTest.java` - verify first page data correct
- [ ] T007 [US2] Write test
      `testGetSampleAssignments_LastPage_ReturnsRemainingItems()` in
      `SampleStorageServiceImplTest.java` - verify last page handles partial
      data
- [ ] T008 [US1] Write test
      `testGetSampleAssignments_InvalidPageNumber_HandlesGracefully()` in
      `SampleStorageServiceImplTest.java` - verify error handling for negative
      page numbers

### Backend Integration Tests (BaseWebContextSensitiveTest)

- [ ] T009 [US1] Create test class
      `src/test/java/org/openelisglobal/storage/controller/SampleStorageRestControllerTest.java`
      extending `BaseWebContextSensitiveTest` with MockMvc
- [ ] T010 [US1] Write test
      `testGetSampleItems_WithPaginationParams_ReturnsPagedResults()` in
      `SampleStorageRestControllerTest.java` - verify endpoint accepts page/size
      params and returns pagination metadata
- [ ] T011 [US1] Write test `testGetSampleItems_DefaultParams_Returns25Items()`
      in `SampleStorageRestControllerTest.java` - verify default page size is 25
- [ ] T012 [US3] Write test
      `testGetSampleItems_CustomPageSize_ReturnsSpecifiedSize()` in
      `SampleStorageRestControllerTest.java` - verify page size 50 and 100 work

### Verification

- [ ] T013 Run backend tests and verify ALL FAIL:
      `mvn test -Dtest="SampleStorageServiceImplTest,SampleStorageRestControllerTest"` -
      Expected: 9 failing tests (correct TDD - no implementation yet)

---

## Phase 2: Backend Implementation (GREEN) - Make Tests Pass [NOT STARTED]

**Purpose**: Write minimal backend code to make all tests pass

**Duration**: 2 hours  
**User Stories**: US1, US2, US3

**Checkpoint**: 9 passing backend tests

### DAO Layer

- [ ] T014 [US1] Add method signature
      `Page<SampleStorageAssignment> findAll(Pageable pageable);` to
      `src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAO.java`
- [ ] T015 [US1] Implement `findAll(Pageable pageable)` method in
      `src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAOImpl.java`
      using Hibernate Session + HQL with count query and data query
      (setFirstResult, setMaxResults, return PageImpl)

### Service Layer

- [ ] T016 [US1] Add method signature
      `Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable);`
      to
      `src/main/java/org/openelisglobal/storage/service/SampleStorageService.java`
- [ ] T017 [US1] Implement `getSampleAssignments(Pageable pageable)` method with
      `@Transactional(readOnly = true)` in
      `src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java` -
      delegate to DAO.findAll(pageable)

### Controller Layer

- [ ] T018 [US1] Modify GET `/sample-items` endpoint in
      `src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java` -
      add `@RequestParam(defaultValue = "0") int page` and
      `@RequestParam(defaultValue = "25") int size` parameters
- [ ] T019 [US1] Implement pagination logic in
      `SampleStorageRestController.java` GET endpoint - validate page size
      (25/50/100 only), validate page >= 0, create PageRequest with Sort by
      assignedDate DESC, call service, build response Map with
      items/currentPage/totalPages/totalItems/pageSize

### Verification

- [ ] T020 Run backend tests and verify ALL PASS:
      `mvn test -Dtest="SampleStorageServiceImplTest,SampleStorageRestControllerTest"` -
      Expected: 9 passing tests (backend pagination complete)

---

## Phase 3: Frontend Tests (RED) - TDD [NOT STARTED]

**Purpose**: Write frontend tests for pagination component state (tests will
FAIL initially)

**Duration**: 1 hour  
**User Stories**: US1, US2, US3

**Checkpoint**: 4 failing frontend tests created

### Jest Unit Tests (React Testing Library)

- [ ] T021 [US1] Create test file
      `frontend/src/components/storage/StorageDashboard.test.jsx` if not exists,
      add imports (React, testing-library, userEvent, jest-dom, IntlProvider,
      BrowserRouter, StorageDashboard, messages)
- [ ] T022 [US1] Mock `getFromOpenElisServer` in `StorageDashboard.test.jsx` -
      add `jest.mock('../utils/Utils')` with mockResolvedValue for pagination
      response
- [ ] T023 [US1] Write test
      `testPaginationComponent_Renders_WithDefaultPageSize()` in
      `StorageDashboard.test.jsx` - verify Pagination component renders with
      default 25 items
- [ ] T024 [US2] Write test `testPageChange_TriggersAPICall_WithCorrectParams()`
      in `StorageDashboard.test.jsx` - verify clicking Next button calls API
      with page=1
- [ ] T025 [US3] Write test `testPageSizeChange_ResetsToPageOne()` in
      `StorageDashboard.test.jsx` - verify changing page size to 50 resets to
      page 1
- [ ] T026 [US1] Write test `testPaginationState_PreservedOnTabSwitch()` in
      `StorageDashboard.test.jsx` - verify page state preserved when switching
      tabs

### Verification

- [ ] T027 Run frontend tests and verify ALL FAIL:
      `cd frontend && npm test -- StorageDashboard.test.jsx` - Expected: 4
      failing tests (correct TDD - no implementation yet)

---

## Phase 4: Frontend Implementation (GREEN) - Make Tests Pass [NOT STARTED]

**Purpose**: Add pagination component and state management to frontend

**Duration**: 2 hours  
**User Stories**: US1, US2, US3

**Checkpoint**: 4 passing frontend tests

### Component State

- [ ] T028 [US1] Add Pagination import in
      `frontend/src/components/storage/StorageDashboard.jsx` - add
      `import { Pagination } from '@carbon/react';`
- [ ] T029 [US1] Add pagination state variables in `StorageDashboard.jsx` after
      existing useState declarations - add
      `const [page, setPage] = useState(1);` (1-based for Carbon),
      `const [pageSize, setPageSize] = useState(25);`,
      `const [totalItems, setTotalItems] = useState(0);`
- [ ] T030 [US1] Update `fetchSamples` function in `StorageDashboard.jsx` to
      include page/size params - modify API call to
      `/rest/storage/sample-items?page=${page - 1}&size=${pageSize}` (convert to
      0-based), extract response.items and response.totalItems
- [ ] T031 [US1] Add useEffect dependency for pagination in
      `StorageDashboard.jsx` - add `page` and `pageSize` to dependency array of
      fetchSamples useEffect

### UI Component

- [ ] T032 [US1] Add Pagination component in `StorageDashboard.jsx` after
      samples DataTable - add
      `<Pagination page={page} pageSize={pageSize} pageSizes={[25, 50, 100]} totalItems={totalItems} onChange={({ page, pageSize }) => { setPage(page); setPageSize(pageSize); }} />`

### Verification

- [ ] T033 Run frontend tests and verify ALL PASS:
      `cd frontend && npm test -- StorageDashboard.test.jsx` - Expected: 4
      passing tests (frontend pagination complete)

---

## Phase 5: E2E Tests (Cypress) [NOT STARTED]

**Purpose**: Validate complete pagination workflow end-to-end

**Duration**: 1 hour  
**User Stories**: US1, US2, US3

**Checkpoint**: 5 passing E2E tests, browser console logs reviewed

### Cypress E2E Tests

- [ ] T034 [US1] Create E2E test file
      `frontend/cypress/e2e/storagePagination.cy.js` with login before hook and
      loadStorageFixtures
- [ ] T035 [US1] Write test `should display first page with 25 items by default`
      in `storagePagination.cy.js` - verify page loads, 25 items displayed,
      pagination controls visible
- [ ] T036 [US2] Write test
      `should navigate to next page when clicking Next button` in
      `storagePagination.cy.js` - set up intercept for API, click Next, verify
      API called with page=1
- [ ] T037 [US2] Write test
      `should navigate to previous page when clicking Previous button` in
      `storagePagination.cy.js` - navigate to page 2, click Previous, verify API
      called with page=0
- [ ] T038 [US3] Write test `should change page size to 50 items` in
      `storagePagination.cy.js` - set up intercept, select 50 from dropdown,
      verify API called with size=50
- [ ] T039 [US1] Write test
      `should preserve pagination state when switching tabs` in
      `storagePagination.cy.js` - navigate to page 2, switch to Rooms tab,
      return to Samples tab, verify still on page 2

### Verification (MANDATORY per Constitution V.5)

- [ ] T040 Run E2E test individually:
      `npm run cy:run -- --spec "cypress/e2e/storagePagination.cy.js"` - verify
      all 5 tests pass
- [ ] T041 Review browser console logs in Cypress UI (MANDATORY) - check for
      JavaScript errors, API failures, unexpected warnings
- [ ] T042 Review failure screenshots if any - verify no unexpected UI states
- [ ] T043 Verify test output shows all assertions passed - confirm 5/5 tests
      passing

---

## Phase 6: Polish & Verification [NOT STARTED]

**Purpose**: Final code quality checks, formatting, full test suite,
constitution compliance

**Duration**: 1 hour

**Checkpoint**: All checks pass, ready for PR

### Code Quality

- [ ] T044 Format backend code: `mvn spotless:apply` - verify no formatting
      errors
- [ ] T045 Format frontend code: `cd frontend && npm run format` - verify
      Prettier completes successfully

### Full Test Suite

- [ ] T046 Run full backend test suite: `mvn test` - verify all existing tests
      still pass (no regressions)
- [ ] T047 Run full frontend test suite: `cd frontend && npm test` - verify all
      tests pass
- [ ] T048 Build verification:
      `mvn clean install -DskipTests -Dmaven.test.skip=true` - verify build
      succeeds

### Constitution Compliance Verification

- [ ] T049 Verify Constitution compliance checklist from `plan.md` - confirm all
      8 principles followed (Layered Architecture, Carbon Design System, Test
      Coverage, No @Transactional in controller, Input validation, etc.)
- [ ] T050 Manual testing with large dataset: Start dev environment
      `docker compose -f dev.docker-compose.yml up -d`, navigate to
      `https://localhost/Storage/samples`, verify page loads in <2 seconds with
      100k+ samples, verify pagination controls work (Next, Previous, page
      numbers, page size selector), verify page state preserved when switching
      tabs
- [ ] T051 Take screenshots for PR: Capture pagination controls, page
      navigation, page size selector, performance metrics
- [ ] T052 Create PR with title "feat: Add server-side pagination to Sample
      Storage page (OGC-150)", description including changes summary, testing
      results, screenshots, references to OGC-150 and 001-sample-storage

---

## Implementation Strategy

### TDD Workflow (MANDATORY)

Every phase follows strict Test-Driven Development:

1. **RED**: Write failing test that defines expected behavior
2. **GREEN**: Write minimal code to make test pass
3. **REFACTOR**: Improve code quality while keeping tests green

**Example Flow**:

```
Phase 1: Write backend tests → Tests FAIL (no implementation)
Phase 2: Implement backend → Tests PASS
Phase 3: Write frontend tests → Tests FAIL (no implementation)
Phase 4: Implement frontend → Tests PASS
Phase 5: Write E2E tests → Tests PASS (integration complete)
```

### MVP Scope

**Minimum Viable Product**: Complete all 6 phases (no partial delivery possible)

**Rationale**: This feature is a single cohesive change. Backend pagination
without frontend component is useless, and vice versa. All three user stories
share the same implementation.

---

## Progress Tracking

### Time Tracking

| Phase                            | Estimated               | Actual | Notes |
| -------------------------------- | ----------------------- | ------ | ----- |
| Phase 0: Setup                   | 15 min                  |        |       |
| Phase 1: Backend Tests           | 2 hours                 |        |       |
| Phase 2: Backend Implementation  | 2 hours                 |        |       |
| Phase 3: Frontend Tests          | 1 hour                  |        |       |
| Phase 4: Frontend Implementation | 2 hours                 |        |       |
| Phase 5: E2E Tests               | 1 hour                  |        |       |
| Phase 6: Polish                  | 1 hour                  |        |       |
| **TOTAL**                        | **~9 hours (1-2 days)** |        |       |

### Coverage Metrics

| Metric                    | Target  | Actual | Notes                           |
| ------------------------- | ------- | ------ | ------------------------------- |
| Backend Unit Tests        | 5 tests |        | SampleStorageServiceImplTest    |
| Backend Integration Tests | 4 tests |        | SampleStorageRestControllerTest |
| Frontend Unit Tests       | 4 tests |        | StorageDashboard.test.jsx       |
| E2E Tests                 | 5 tests |        | storagePagination.cy.js         |
| Backend Coverage          | >80%    |        | JaCoCo report                   |
| Frontend Coverage         | >70%    |        | Jest coverage                   |

---

## Success Criteria (from spec.md)

From [spec.md Success Criteria](./spec.md#success-criteria):

- [ ] **SC-001**: Page load time <2 seconds with 100,000+ samples (measure with
      performance testing)
- [ ] **SC-002**: Browser memory usage stable (monitor with browser dev tools)
- [ ] **SC-003**: Page navigation <1 second (verify in E2E tests)
- [ ] **SC-004**: 100% user success rate (verify in E2E tests - all scenarios
      pass)
- [ ] **SC-005**: Pagination UX matches NoteBook module (visual comparison)

---

## Troubleshooting

### Common Issues

**Issue**: Tests fail with "Method not found" error  
**Solution**: Ensure method signatures match exactly - check imports for Page,
Pageable, PageRequest

**Issue**: Frontend API calls fail with 404  
**Solution**: Verify backend server running and endpoint path correct
(`/rest/storage/sample-items`)

**Issue**: Pagination component doesn't render  
**Solution**: Check Carbon React version is v1.15.0+: `npm list @carbon/react`

**Issue**: Tests fail with LazyInitializationException  
**Solution**: Ensure @Transactional annotation is on SERVICE method (NOT
controller)

**Issue**: E2E tests fail with timeout  
**Solution**: Use `cy.wait('@interceptAlias')` with proper intercept setup
BEFORE action

---

## References

- **Specification**: [spec.md](./spec.md)
- **Implementation Plan**: [plan.md](./plan.md)
- **Research**: [research.md](./research.md)
- **Developer Guide**: [quickstart.md](./quickstart.md)
- **Jira Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)
- **Parent Feature**: [001-sample-storage](../001-sample-storage/tasks.md)
- **Constitution**: [v1.8.0](../../.specify/memory/constitution.md)
- **Testing Roadmap**:
  [testing-roadmap.md](../../.specify/guides/testing-roadmap.md)
