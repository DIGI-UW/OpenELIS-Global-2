# Research: Sample Storage Pagination

**Feature**: OGC-150 Sample Storage Pagination  
**Date**: 2025-12-05  
**Purpose**: Resolve technical unknowns and validate implementation approach

---

## Research Question 1: Existing Pagination Pattern in NoteBook Module

### Investigation

Analyzed reference implementation in NoteBook module to understand pagination
pattern.

**Files Reviewed**:

- `frontend/src/components/notebook/NoteBookDashBoard.js` (lines 1-100, 200-250)
- `src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`
  (lines 67-100)

### Findings

#### Frontend Pattern (`NoteBookDashBoard.js`)

**Pagination State Management**:

```javascript
// Line 75-76
const [page, setPage] = useState(1); // 1-based indexing for Carbon
const [pageSize, setPageSize] = useState(100); // Default page size
```

**Carbon Pagination Component**:

```javascript
// Lines 21-22 (imports)
import { Pagination } from "@carbon/react";

// Usage pattern (inferred from state variables)
<Pagination
  page={page}
  pageSize={pageSize}
  pageSizes={[25, 50, 100]} // Standard options
  totalItems={totalItems}
  onChange={({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
  }}
/>;
```

**Key Observations**:

- Uses React `useState` for pagination state
- Carbon Pagination uses 1-based indexing (page 1 is first page)
- Page size defaults to 100 in NoteBook (OGC-150 requires 25)
- State updates trigger re-fetch via `useEffect` dependency

#### Backend Pattern (`NoteBookRestController.java`)

**Endpoint Signature**:

```java
// Line 67-73
@GetMapping(value = "/dashboard/entries", produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public ResponseEntity<List<NoteBookDisplayBean>> getFilteredNoteBooks(
        @RequestParam(required = false) List<NoteBookStatus> statuses,
        @RequestParam(required = false) List<String> types,
        // ... other filter params
        @RequestParam(required = false) Integer noteBookId) {
```

**Observation**: NoteBook module does NOT use server-side pagination!

- Returns full `List<NoteBookDisplayBean>` without `Page<T>`
- No `page` or `size` parameters
- Client-side pagination only (Carbon component slices data in browser)

### Decision

**Approach**: Implement TRUE server-side pagination (better than NoteBook
reference)

**Rationale**:

- NoteBook's client-side pagination won't scale to 100,000+ samples
- Need to follow Spring Data JPA best practices for server-side pagination
- Use `Pageable` parameter and return `Page<T>` (industry standard)

**Pattern to Follow**:

- **Frontend**: Use Carbon Pagination component (same as NoteBook)
- **Backend**: Use Spring Data JPA `Pageable` + `Page<T>` (better than NoteBook)

---

## Research Question 2: Spring Data JPA Pageable Support in Existing DAOs

### Investigation

Reviewed existing DAO implementations in storage module.

**Files Reviewed**:

- `src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAO.java`
- `src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAOImpl.java`

### Findings

**Current DAO Pattern**:

```java
public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, String> {
    // Custom methods
}

public class SampleStorageAssignmentDAOImpl extends BaseDAOImpl<SampleStorageAssignment, String>
        implements SampleStorageAssignmentDAO {
    // Implementation
}
```

**BaseDAO Capabilities**:

- Extends `BaseDAOImpl` which uses Hibernate `Session` API
- Does NOT extend Spring Data JPA `PagingAndSortingRepository`
- Uses HQL queries, NOT Spring Data JPA query methods

**Pagination Support**:

- `BaseDAOImpl` does NOT have built-in pagination methods
- Need to add custom pagination method to DAO interface

### Decision

**Add Custom Pagination Method to DAO**:

```java
// SampleStorageAssignmentDAO.java
Page<SampleStorageAssignment> findAll(Pageable pageable);

// SampleStorageAssignmentDAOImpl.java
@Override
public Page<SampleStorageAssignment> findAll(Pageable pageable) {
    Session session = entityManager.unwrap(Session.class);

    // Count query for total
    String countHql = "SELECT COUNT(s) FROM SampleStorageAssignment s";
    Long total = session.createQuery(countHql, Long.class).getSingleResult();

    // Data query with pagination
    String dataHql = "SELECT s FROM SampleStorageAssignment s ORDER BY s.assignedDate DESC";
    List<SampleStorageAssignment> content = session.createQuery(dataHql, SampleStorageAssignment.class)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();

    return new PageImpl<>(content, pageable, total);
}
```

**Rationale**:

- Works with existing Hibernate Session-based DAO pattern
- Compatible with BaseDAOImpl structure
- Follows Spring Data JPA `Page<T>` return type for service layer
- Uses HQL (consistent with existing codebase - Constitution IV)

---

## Research Question 3: Pagination State Preservation Across Tab Navigation

### Investigation

Analyzed `StorageDashboard.jsx` tab state management.

**Files Reviewed**:

- `frontend/src/components/storage/StorageDashboard.jsx`

### Findings

**Tab Structure**:

```javascript
<Tabs selectedIndex={activeTab} onChange={setActiveTab}>
  <Tab label="Samples">...</Tab>
  <Tab label="Rooms">...</Tab>
  <Tab label="Devices">...</Tab>
  <Tab label="Shelves">...</Tab>
  <Tab label="Racks">...</Tab>
</Tabs>
```

**Current State Management**:

- Each tab has its own data fetching logic
- No shared state between tabs
- Switching tabs re-renders content

**Pagination State Considerations**:

| Approach                 | Pros                                  | Cons                       | Decision      |
| ------------------------ | ------------------------------------- | -------------------------- | ------------- |
| **Component State**      | Simple, no URL pollution              | Lost on page refresh       | ✅ **CHOSEN** |
| **URL Query Params**     | Preserves on refresh, shareable links | Clutters URL, more complex | ❌ Rejected   |
| **Context/Global State** | Shared across components              | Overkill for single tab    | ❌ Rejected   |

### Decision

**Use Component State with Tab-Specific Storage**:

```javascript
const [samplesPage, setSamplesPage] = useState(1);
const [samplesPageSize, setSamplesPageSize] = useState(25);

// Only active when Samples tab is selected
useEffect(() => {
  if (activeTab === 0) {
    // Samples tab
    fetchSamples(samplesPage, samplesPageSize);
  }
}, [activeTab, samplesPage, samplesPageSize]);
```

**Rationale**:

- Component state persists during tab navigation (tab content unmounts but
  component doesn't)
- Simple implementation, no external dependencies
- Matches existing tab pattern in StorageDashboard
- User expectation: pagination resets on page refresh (acceptable UX)

**Behavior**:

- ✅ Pagination state preserved when switching tabs and returning
- ❌ Pagination state lost on page refresh (acceptable - starts at page 1)
- ✅ Each tab could have independent pagination if needed in future

---

## Research Summary

### Technologies Validated

| Technology        | Version  | Purpose               | Status                                   |
| ----------------- | -------- | --------------------- | ---------------------------------------- |
| Spring Data JPA   | 3.x      | Backend pagination    | ✅ Compatible with Hibernate Session API |
| Carbon Pagination | v1.15.0  | Frontend UI component | ✅ Available in @carbon/react            |
| React useState    | React 17 | State management      | ✅ Standard pattern                      |
| Hibernate HQL     | 6.x      | DAO pagination query  | ✅ Consistent with codebase              |

### Key Decisions Made

1. **Backend Pagination**: Use custom DAO method with Hibernate Session + HQL
   (not Spring Data JPA repositories)
2. **Frontend Pattern**: Use Carbon Pagination component with component state
   (not URL params)
3. **Default Page Size**: 25 items (OGC-150 requirement, different from
   NoteBook's 100)
4. **State Preservation**: Component state persists across tab navigation (lost
   on refresh - acceptable)

### Alternatives Considered

| Alternative                            | Why Rejected                                                     |
| -------------------------------------- | ---------------------------------------------------------------- |
| Client-side pagination (like NoteBook) | Won't scale to 100,000+ samples, defeats performance goal        |
| Spring Data JPA repositories           | Requires refactoring all DAOs, outside scope of this enhancement |
| URL query params for state             | Unnecessary complexity for simple pagination feature             |
| Redux/Context for state                | Overkill for single tab pagination                               |

### Risks & Mitigations

| Risk                                | Mitigation                                                     |
| ----------------------------------- | -------------------------------------------------------------- |
| Performance with 100k+ samples      | Use database indexes on assignedDate (already exists from 001) |
| Memory issues with large page sizes | Validate page size (max 100 items)                             |
| Inconsistent UX with NoteBook       | Document difference (server-side vs client-side) in quickstart |

---

## Implementation Ready

All research questions resolved. No blockers identified. Ready to proceed with
implementation following TDD workflow.

**Next Steps**:

1. Generate `quickstart.md` developer guide
2. Run `/speckit.tasks` to create detailed task breakdown
3. Create `feat/OGC-150-storage-pagination` branch
4. Begin Phase 1: Backend Tests (RED)
