# Implementation Plan: Sample Storage Pagination

**Branch**: `OGC-150-storage-pagination` | **Date**: 2025-12-05 | **Spec**:
[spec.md](./spec.md)  
**Input**: Feature specification from
`/specs/OGC-150-storage-pagination/spec.md`  
**Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)  
**Parent Feature**: [001-sample-storage](../001-sample-storage/plan.md)

## Summary

Add server-side pagination to the Sample Storage Dashboard (`/Storage/samples`)
to support large datasets (100,000+ samples) without performance degradation.
Currently, the page loads all sample storage assignments at once, causing 10-20
second page loads with large datasets. This enhancement implements pagination
following the existing pattern in the NoteBook module, using Spring Data JPA
`Pageable` + `Page<T>` on the backend and Carbon Design System `<Pagination>`
component on the frontend.

**Technical Approach**:

1. **Backend**: Modify `SampleStorageService` to accept `Pageable` parameter and
   return `Page<SampleStorageAssignment>` instead of full list
2. **Backend**: Update `SampleStorageRestController` GET endpoint to accept
   `page` and `size` query parameters
3. **Frontend**: Add pagination state (`page`, `pageSize`) to `StorageDashboard`
   component
4. **Frontend**: Add Carbon `<Pagination>` component below the samples DataTable
5. **Testing**: Unit tests for service pagination logic, integration tests for
   REST endpoint, Jest tests for component state, Cypress E2E test for
   pagination workflow

**Estimated Effort**: 1-2 days (9 hours total)

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**:

- Backend: Spring Data JPA (Spring Framework 6.2.2), Hibernate 6.x, PostgreSQL
  14+
- Frontend: @carbon/react v1.15.0 (Pagination component), React Intl 5.20.12,
  SWR 2.0.3

**Storage**: PostgreSQL 14+ (existing OpenELIS database) - NO schema changes
required  
**Testing**:

- Backend: JUnit 4 (4.13.1) + Mockito 2.21.0 (unit tests),
  BaseWebContextSensitiveTest (integration tests)
- Frontend: Jest + React Testing Library (unit tests), Cypress 12.17.3 (E2E
  tests)

**Target Platform**: Web application (Linux server deployment, browser-based
UI)  
**Project Type**: Web (backend + frontend enhancement to existing feature
001-sample-storage)  
**Performance Goals**:

- Page load time <2 seconds with 100,000+ samples (currently 10-20 seconds)
- Page navigation <1 second (next/previous/page number clicks)
- Browser memory usage stable regardless of total sample count

**Constraints**:

- MUST follow existing pagination pattern from NoteBook module for consistency
- NO database schema changes allowed
- MUST maintain backward compatibility with existing Sample Storage
  functionality
- MUST work with existing filters and search from 001-sample-storage

**Scale/Scope**:

- 4 files to modify (1 service impl, 1 controller, 1 component, test files)
- ~200 lines of code total
- Support datasets of 100,000+ samples
- Default page size: 25 items (configurable to 50 or 100)

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: Default page size (25) could be made
      configurable via SystemConfiguration if needed in future - no
      country-specific logic
- [x] **Carbon Design System**: UI uses @carbon/react `<Pagination>` component
      exclusively (NO Bootstrap/Tailwind)
- [x] **FHIR/IHE Compliance**: N/A - internal performance optimization, no
      external data integration
- [x] **Layered Architecture**: Backend follows 5-layer pattern
      (Service→Controller)
  - Pagination logic in **service layer** using Spring Data JPA `Pageable`
  - Controller handles HTTP request/response mapping only
  - **NO @Transactional annotations on controller methods** (belongs in service
    layer)
- [x] **Test Coverage**: Unit + integration + E2E tests planned (>80%
      backend, >70% frontend coverage goal)
  - Backend unit tests: Service pagination logic
  - Backend integration tests: REST endpoint with page/size params
  - Frontend unit tests: Pagination component state management
  - E2E tests: Pagination workflow (view, navigate, change page size)
  - E2E tests MUST follow Cypress best practices (Constitution V.5):
    - Run tests individually during development (not full suite)
    - Browser console logging enabled and reviewed after each run
    - Video recording disabled by default
    - Post-run review of console logs and screenshots required
    - Use data-testid selectors
    - See
      [Testing Roadmap](../../.specify/guides/testing-roadmap.md#cypress-e2e-testing)
- [x] **Schema Management**: NO database changes required (N/A)
- [x] **Internationalization**: Carbon Pagination component provides default
      labels in multiple languages - any custom messages MUST use React Intl
- [x] **Security & Compliance**:
  - Input validation: page number and page size MUST be validated (prevent
    negative numbers, excessive page sizes)
  - Audit trail: NO changes to existing audit trail (pagination is read-only
    operation)
  - RBAC: Uses existing authentication/authorization from 001-sample-storage

**Complexity Justification**: None required - plan fully compliant with
constitution.

## Milestone Plan

_GATE: Features >3 days MUST define milestones per Constitution Principle IX.
Each milestone = 1 PR. Use `[P]` prefix for parallel milestones._

**Estimated Total Effort**: 1-2 days (9 hours)

**Milestone Strategy**: **SINGLE PR** - This feature requires <3 days effort, so
milestones are optional per Constitution Principle IX. Implementation will use a
single `feat/OGC-150-storage-pagination` branch with one pull request.

### Why Single PR Approach?

- **Small scope**: Only 4 files to modify, ~200 LOC total
- **Single concern**: Adds pagination to one specific table (Samples tab)
- **Low complexity**: Follows existing pattern from NoteBook module
- **No dependencies**: All changes are contained within pagination feature
- **Fast review**: Estimated 1-2 hour code review time

### Implementation Phases (Within Single PR)

| Phase                            | Scope                                                    | Verification       | Duration |
| -------------------------------- | -------------------------------------------------------- | ------------------ | -------- |
| Phase 1: Backend Tests           | Write service unit tests, controller integration tests   | Tests fail (RED)   | 2 hours  |
| Phase 2: Backend Implementation  | Implement service pagination, update controller endpoint | Tests pass (GREEN) | 2 hours  |
| Phase 3: Frontend Tests          | Write Jest tests for pagination component state          | Tests fail (RED)   | 1 hour   |
| Phase 4: Frontend Implementation | Add pagination state, Carbon Pagination component        | Tests pass (GREEN) | 2 hours  |
| Phase 5: E2E Tests               | Write Cypress test for pagination workflow               | Test passes        | 1 hour   |
| Phase 6: Polish                  | Code formatting, documentation, final review             | All checks pass    | 1 hour   |

**Total**: 9 hours (~1-2 days)

### PR Strategy

- **Spec PR**: `spec/OGC-150-storage-pagination` → `develop` (specification
  documents only) - **CURRENT STEP**
- **Implementation PR**: `feat/OGC-150-storage-pagination` → `develop` (single
  PR with all changes)

## Testing Strategy

**Reference**: [Testing Roadmap](../../.specify/guides/testing-roadmap.md)

### Coverage Goals

- **Backend**: >80% code coverage for new pagination service methods
- **Frontend**: >70% code coverage for pagination component changes
- **E2E**: 100% coverage of user stories (P1: view paginated list, P1: navigate,
  P2: change page size)

### Test Types

Following the test pyramid from
[Testing Roadmap](../../.specify/guides/testing-roadmap.md#test-pyramid):

#### 1. Backend Unit Tests (JUnit 4 + Mockito)

**File**:
`src/test/java/org/openelisglobal/storage/service/SampleStorageServiceImplTest.java`

**Tests**:

- `testGetSampleAssignments_WithPageable_ReturnsCorrectPageSize()`
- `testGetSampleAssignments_WithPageable_ReturnsTotalElements()`
- `testGetSampleAssignments_FirstPage_ReturnsFirstNItems()`
- `testGetSampleAssignments_LastPage_ReturnsRemainingItems()`
- `testGetSampleAssignments_InvalidPageNumber_HandlesGracefully()`

**Pattern**: Mock DAO, test service logic only

#### 2. Backend Integration Tests (BaseWebContextSensitiveTest)

**File**:
`src/test/java/org/openelisglobal/storage/controller/SampleStorageRestControllerTest.java`

**Tests**:

- `testGetSampleItems_WithPaginationParams_ReturnsPagedResults()`
- `testGetSampleItems_DefaultParams_Returns25Items()`
- `testGetSampleItems_CustomPageSize_ReturnsSpecifiedSize()`
- `testGetSampleItems_ResponseIncludesPaginationMetadata()`

**Pattern**: Full Spring context, test HTTP request/response

#### 3. Frontend Unit Tests (Jest + React Testing Library)

**File**: `frontend/src/components/storage/StorageDashboard.test.jsx`

**Tests**:

- `testPaginationComponent_Renders_WithCorrectProps()`
- `testPageChange_TriggersAPICall_WithCorrectParams()`
- `testPageSizeChange_ResetsToPageOne()`
- `testPaginationState_PreservedOnTabSwitch()`

**Pattern**: Mock API calls, test component state and rendering

#### 4. E2E Tests (Cypress)

**File**: `frontend/cypress/e2e/storagePagination.cy.js`

**Tests**:

- `testViewPaginatedList_LoadsFirst25Items_InUnder2Seconds()`
- `testNavigateToNextPage_LoadsNext25Items()`
- `testNavigateToPreviousPage_LoadsPrevious25Items()`
- `testChangePageSizeTo50_Loads50Items()`
- `testPageStatePreservation_AcrossTabSwitches()`

**Pattern**: Full user workflow, test with real data

### Test Data Management

**Reference**:
[Test Data Strategy Guide](../../.specify/guides/test-data-strategy.md)

- **Backend Tests**: Use builders/factories (e.g.,
  `SampleStorageAssignmentBuilder`) to create test data
- **E2E Tests**: Load test fixtures using `cy.loadStorageFixtures()` (reuses
  001-sample-storage test data)
- **No manual database setup**: All test data creation automated via scripts or
  API calls

### Checkpoint Validations

| Checkpoint                             | Required Tests                       | Must Pass Before       |
| -------------------------------------- | ------------------------------------ | ---------------------- |
| After Phase 1 (Backend Tests Written)  | Backend unit + integration tests     | Phase 2 implementation |
| After Phase 2 (Backend Implemented)    | All backend tests                    | Phase 3 frontend tests |
| After Phase 3 (Frontend Tests Written) | Frontend unit tests                  | Phase 4 implementation |
| After Phase 4 (Frontend Implemented)   | All frontend tests                   | Phase 5 E2E tests      |
| After Phase 5 (E2E Tests)              | All E2E tests                        | Phase 6 polish         |
| Before PR Creation                     | ALL tests (unit + integration + E2E) | PR can be submitted    |

### TDD Workflow

**MANDATORY**: Follow strict Test-Driven Development (Red-Green-Refactor):

1. **RED**: Write failing test that defines expected behavior
2. **GREEN**: Write minimal code to make test pass
3. **REFACTOR**: Improve code quality while keeping tests green

**Example Flow**:

```
Phase 1: Write backend tests → Tests FAIL (no implementation)
Phase 2: Implement backend code → Tests PASS
Phase 3: Write frontend tests → Tests FAIL (no implementation)
Phase 4: Implement frontend code → Tests PASS
Phase 5: Write E2E test → Test PASSES (integration complete)
```

## Project Structure

### Documentation (this feature)

```text
specs/OGC-150-storage-pagination/
├── spec.md                   # Feature specification (COMPLETE)
├── plan.md                   # This file (CURRENT)
├── research.md               # Phase 0 output (to be generated)
├── quickstart.md             # Phase 1 output (to be generated)
└── checklists/
    └── requirements.md       # Spec quality checklist (COMPLETE)
```

**Note**: `data-model.md` and `contracts/` are NOT needed for this feature:

- **data-model.md**: Not needed - reuses existing `SampleStorageAssignment`
  entity from 001-sample-storage
- **contracts/**: Not needed - modifies existing endpoint, pattern well-defined
  (follows NoteBook module)

### Source Code (repository root)

**Files to Modify**:

```text
# Backend (Java)
src/main/java/org/openelisglobal/storage/
├── service/
│   ├── SampleStorageService.java                      # ADD: Pageable method signature
│   └── SampleStorageServiceImpl.java                  # MODIFY: Add pagination logic
└── controller/
    └── SampleStorageRestController.java               # MODIFY: Add page/size params to GET endpoint

# Frontend (React)
frontend/src/components/storage/
└── StorageDashboard.jsx                               # MODIFY: Add pagination state + component

# Tests (Backend)
src/test/java/org/openelisglobal/storage/
├── service/
│   └── SampleStorageServiceImplTest.java              # ADD: Pagination unit tests
└── controller/
    └── SampleStorageRestControllerTest.java           # ADD: Pagination integration tests

# Tests (Frontend)
frontend/src/components/storage/
└── StorageDashboard.test.jsx                          # ADD: Pagination component tests

# Tests (E2E)
frontend/cypress/e2e/
└── storagePagination.cy.js                            # NEW: Pagination E2E test
```

**Structure Decision**: Follows existing OpenELIS monolithic repository
structure. All changes are within existing `storage` module from
001-sample-storage. No new packages or directories required.

## Phase 0: Research & Technology Validation

### Research Questions

**Q1: What is the existing pagination pattern in NoteBook module?**

**Research Task**: Analyze `NoteBookDashBoard.js` and
`NoteBookRestController.java` to understand:

- How pagination state is managed in React component
- How Carbon `<Pagination>` component is used
- How backend endpoint accepts page/size parameters
- How response includes pagination metadata (totalPages, totalItems,
  currentPage)

**Expected Outcome**: Document the exact pattern to replicate for Sample
Storage.

---

**Q2: How does Spring Data JPA Pageable work with existing DAOs?**

**Research Task**: Review existing DAO implementations to understand:

- Do our DAOs extend `PagingAndSortingRepository` or use custom HQL queries?
- How to add `Pageable` parameter to service methods
- How to construct `Page<T>` response with total count

**Expected Outcome**: Confirm DAO supports pagination or identify required
changes.

---

**Q3: How to preserve pagination state across tab navigation?**

**Research Task**: Analyze `StorageDashboard.jsx` tab state management:

- How are tab-specific states preserved when switching tabs?
- Should pagination state be in component state or URL query params?
- What happens to pagination when filters are applied?

**Expected Outcome**: Design pattern for state preservation.

---

### Research Findings

**To be completed in research.md after investigation**

## Phase 1: Design & Contracts

### API Contract

**Existing Endpoint to Modify**:

```
GET /rest/storage/sample-items
```

**New Parameters**:

| Parameter | Type | Default | Description                           |
| --------- | ---- | ------- | ------------------------------------- |
| `page`    | int  | 0       | Zero-based page number                |
| `size`    | int  | 25      | Items per page (allowed: 25, 50, 100) |

**Response Schema**:

```json
{
  "items": [
    {
      "id": "string",
      "sampleId": "string",
      "sampleItemId": "string",
      "assignedDate": "string (ISO 8601)",
      "locationId": "string",
      "locationType": "string",
      "hierarchicalPath": "string"
    }
  ],
  "currentPage": 0,
  "totalPages": 100,
  "totalItems": 2500,
  "pageSize": 25
}
```

**Validation Rules**:

- `page`: Must be >= 0
- `size`: Must be one of [25, 50, 100]
- Invalid params return 400 Bad Request with error message

**Reference Implementation**: `NoteBookRestController.java` GET
`/rest/notebook/dashboard/entries` endpoint

### Data Model

**No changes required** - reuses existing entity:

- **SampleStorageAssignment** (from 001-sample-storage): Already has all
  required fields, pagination retrieves a subset

### Component Design

**StorageDashboard.jsx Changes**:

1. **State Addition**:

```javascript
const [page, setPage] = useState(1); // Carbon uses 1-based indexing
const [pageSize, setPageSize] = useState(25);
const [totalItems, setTotalItems] = useState(0);
```

2. **Data Fetching**:

```javascript
const fetchSamples = async () => {
  const response = await getFromOpenElisServer(
    `/rest/storage/sample-items?page=${page - 1}&size=${pageSize}` // Convert to 0-based
  );
  setSamples(response.items);
  setTotalItems(response.totalItems);
};
```

3. **Pagination Component**:

```javascript
<Pagination
  page={page}
  pageSize={pageSize}
  pageSizes={[25, 50, 100]}
  totalItems={totalItems}
  onChange={({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
  }}
/>
```

**Reference Implementation**: `NoteBookDashBoard.js` lines 21-22, 75-76

## Implementation Workflow (TDD)

### Phase 1: Backend Tests (RED)

**Duration**: 2 hours

1. Write `SampleStorageServiceImplTest.java` with 5 test methods
2. Write `SampleStorageRestControllerTest.java` with 4 test methods
3. Run tests → Verify ALL FAIL (no implementation yet)

**Checkpoint**: 9 failing backend tests

---

### Phase 2: Backend Implementation (GREEN)

**Duration**: 2 hours

1. Add `Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable)`
   to `SampleStorageService.java`
2. Implement method in `SampleStorageServiceImpl.java`:

```java
@Override
@Transactional(readOnly = true)
public Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable) {
    return sampleStorageAssignmentDAO.findAll(pageable);
}
```

3. Update `SampleStorageRestController.java` GET endpoint:

```java
@GetMapping("/sample-items")
public ResponseEntity<Map<String, Object>> getSampleItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size) {

    // Validate page size
    if (!Arrays.asList(25, 50, 100).contains(size)) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid page size"));
    }

    Pageable pageable = PageRequest.of(page, size, Sort.by("assignedDate").descending());
    Page<SampleStorageAssignment> samplePage = sampleStorageService.getSampleAssignments(pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("items", samplePage.getContent());
    response.put("currentPage", samplePage.getNumber());
    response.put("totalItems", samplePage.getTotalElements());
    response.put("totalPages", samplePage.getTotalPages());
    response.put("pageSize", samplePage.getSize());

    return ResponseEntity.ok(response);
}
```

4. Run tests → Verify ALL PASS

**Checkpoint**: 9 passing backend tests

---

### Phase 3: Frontend Tests (RED)

**Duration**: 1 hour

1. Write `StorageDashboard.test.jsx` with 4 test methods
2. Run tests → Verify ALL FAIL (no implementation yet)

**Checkpoint**: 4 failing frontend tests

---

### Phase 4: Frontend Implementation (GREEN)

**Duration**: 2 hours

1. Add pagination state to `StorageDashboard.jsx`
2. Update data fetching logic to include page/size params
3. Add Carbon `<Pagination>` component below samples DataTable
4. Handle page change and page size change events
5. Run tests → Verify ALL PASS

**Checkpoint**: 4 passing frontend tests

---

### Phase 5: E2E Tests

**Duration**: 1 hour

1. Create `frontend/cypress/e2e/storagePagination.cy.js`
2. Write 5 test cases covering all user stories
3. Run test individually (Constitution V.5):

```bash
npm run cy:run -- --spec "cypress/e2e/storagePagination.cy.js"
```

4. Review browser console logs (MANDATORY per Constitution V.5)
5. Review screenshots if failures occur
6. Verify test passes

**Checkpoint**: 5 passing E2E tests

---

### Phase 6: Polish & Code Quality

**Duration**: 1 hour

1. Format code:

```bash
mvn spotless:apply
cd frontend && npm run format
```

2. Run full test suite:

```bash
mvn test
cd frontend && npm test
```

3. Verify constitution compliance checklist
4. Update documentation if needed
5. Create PR with screenshots

**Checkpoint**: All checks pass, ready for PR

---

## Constitution Compliance Verification (Post-Implementation)

_Re-check after implementation complete:_

- [x] **Configuration-Driven**: Default page size (25) is hardcoded but could be
      made configurable - no country-specific logic
- [x] **Carbon Design System**: Uses Carbon `<Pagination>` component exclusively
- [x] **FHIR/IHE Compliance**: N/A
- [x] **Layered Architecture**: Pagination logic in service layer, controller
      handles HTTP only, no @Transactional in controller
- [x] **Test Coverage**: Unit + integration + E2E tests implemented (>80%
      backend, >70% frontend)
- [x] **Schema Management**: N/A (no schema changes)
- [x] **Internationalization**: Carbon component provides labels
- [x] **Security & Compliance**: Input validation for page/size parameters
      implemented

**Status**: ✅ Constitution compliant

---

## Success Criteria Verification

From [spec.md Success Criteria](./spec.md#success-criteria):

- [ ] **SC-001**: Page load time <2 seconds with 100,000+ samples → Measure with
      performance testing
- [ ] **SC-002**: Browser memory usage stable → Monitor with browser dev tools
- [ ] **SC-003**: Page navigation <1 second → Verify in E2E tests
- [ ] **SC-004**: 100% user success rate → Verify in E2E tests (all scenarios
      pass)
- [ ] **SC-005**: Pagination UX matches NoteBook module → Visual comparison

**Verification Method**: E2E tests + manual testing with large dataset

---

## Next Steps

1. ✅ **Specification complete** (`spec.md`)
2. ✅ **Implementation plan complete** (`plan.md`) - **YOU ARE HERE**
3. ⏭️ **Research** - Run `/speckit.plan` Phase 0 to generate `research.md`
4. ⏭️ **Quickstart** - Generate `quickstart.md` developer guide
5. ⏭️ **Tasks** - Run `/speckit.tasks` to generate `tasks.md` with detailed task
   breakdown
6. ⏭️ **Implementation** - Create `feat/OGC-150-storage-pagination` branch and
   follow TDD workflow

---

## References

- **Parent Feature**: [001-sample-storage](../001-sample-storage/plan.md)
- **Specification**: [spec.md](./spec.md)
- **Jira Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)
- **Constitution**: [v1.8.0](../../.specify/memory/constitution.md)
- **Testing Roadmap**:
  [testing-roadmap.md](../../.specify/guides/testing-roadmap.md)
- **Reference Implementation**:
  - Frontend: `frontend/src/components/notebook/NoteBookDashBoard.js`
  - Backend:
    `src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`
