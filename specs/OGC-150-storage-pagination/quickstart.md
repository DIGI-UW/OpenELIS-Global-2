# Quick Start: Sample Storage Pagination Implementation

**Feature**: OGC-150 Sample Storage Pagination  
**Estimated Time**: 1-2 days (9 hours)  
**Prerequisites**: Feature 001-sample-storage must be fully implemented and
merged to `develop`

---

## Overview

This guide walks you through implementing server-side pagination for the Sample
Storage Dashboard. You'll modify 4 files (1 service, 1 controller, 1 component,
plus tests) following a strict Test-Driven Development (TDD) workflow.

**Key Principle**: Write tests BEFORE implementation code. Every test should
FAIL initially, then PASS after implementation.

---

## Prerequisites Check

Before starting, verify these exist:

```bash
# 1. Check Sample Storage components exist (from 001)
ls frontend/src/components/storage/StorageDashboard.jsx
ls src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java
ls src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java

# 2. Verify you're on the correct branch
git checkout develop
git pull --rebase upstream develop
git checkout -b feat/OGC-150-storage-pagination

# 3. Verify Java 21
java -version  # Must show "21.x.x"
```

---

## Implementation Phases

### Phase 1: Backend Tests (RED) - 2 hours

**Goal**: Write tests that define expected pagination behavior (tests will FAIL
initially).

#### Step 1.1: Create Service Unit Tests

**File**:
`src/test/java/org/openelisglobal/storage/service/SampleStorageServiceImplTest.java`

Add these test methods:

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceImplTest {

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    @Test
    public void testGetSampleAssignments_WithPageable_ReturnsCorrectPageSize() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 25);
        List<SampleStorageAssignment> assignments = createTestAssignments(25);
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Page size should be 25", 25, result.getContent().size());
        assertEquals("Total elements should be 100", 100, result.getTotalElements());
    }

    @Test
    public void testGetSampleAssignments_FirstPage_ReturnsFirstNItems() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 25);
        List<SampleStorageAssignment> assignments = createTestAssignments(25);
        Page<SampleStorageAssignment> page = new PageImpl<>(assignments, pageable, 100);
        when(sampleStorageAssignmentDAO.findAll(pageable)).thenReturn(page);

        // Act
        Page<SampleStorageAssignment> result = sampleStorageService.getSampleAssignments(pageable);

        // Assert
        assertEquals("Current page should be 0", 0, result.getNumber());
        assertEquals("Total pages should be 4", 4, result.getTotalPages());
    }

    // Helper method
    private List<SampleStorageAssignment> createTestAssignments(int count) {
        List<SampleStorageAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setId(String.valueOf(i));
            assignments.add(assignment);
        }
        return assignments;
    }
}
```

#### Step 1.2: Create Controller Integration Tests

**File**:
`src/test/java/org/openelisglobal/storage/controller/SampleStorageRestControllerTest.java`

Add these test methods (extending `BaseWebContextSensitiveTest`):

```java
import org.openelisglobal.test.BaseWebContextSensitiveTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SampleStorageRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetSampleItems_WithPaginationParams_ReturnsPagedResults() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items")
                .param("page", "0")
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.pageSize").value(25))
            .andExpect(jsonPath("$.totalPages").exists())
            .andExpect(jsonPath("$.totalItems").exists());
    }

    @Test
    public void testGetSampleItems_DefaultParams_Returns25Items() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageSize").value(25));
    }
}
```

#### Step 1.3: Run Tests (Verify FAIL)

```bash
mvn test -Dtest="SampleStorageServiceImplTest,SampleStorageRestControllerTest"
```

**Expected**: ALL tests should FAIL (methods don't exist yet). This is correct
TDD!

---

### Phase 2: Backend Implementation (GREEN) - 2 hours

**Goal**: Write minimal code to make tests pass.

#### Step 2.1: Add DAO Method

**File**:
`src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAO.java`

Add interface method:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, String> {
    Page<SampleStorageAssignment> findAll(Pageable pageable);
}
```

**File**:
`src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAOImpl.java`

Implement method:

```java
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

#### Step 2.2: Add Service Method

**File**:
`src/main/java/org/openelisglobal/storage/service/SampleStorageService.java`

Add interface method:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable);
```

**File**:
`src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java`

Implement method:

```java
@Override
@Transactional(readOnly = true)
public Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable) {
    return sampleStorageAssignmentDAO.findAll(pageable);
}
```

#### Step 2.3: Update Controller Endpoint

**File**:
`src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java`

Modify GET endpoint:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@GetMapping("/sample-items")
public ResponseEntity<Map<String, Object>> getSampleItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size) {

    // Validate page size
    if (!Arrays.asList(25, 50, 100).contains(size)) {
        return ResponseEntity.badRequest().body(
            Map.of("error", "Invalid page size. Allowed values: 25, 50, 100")
        );
    }

    // Validate page number
    if (page < 0) {
        return ResponseEntity.badRequest().body(
            Map.of("error", "Page number must be >= 0")
        );
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

#### Step 2.4: Run Tests (Verify PASS)

```bash
mvn test -Dtest="SampleStorageServiceImplTest,SampleStorageRestControllerTest"
```

**Expected**: ALL tests should PASS now. Backend pagination complete!

---

### Phase 3: Frontend Tests (RED) - 1 hour

**Goal**: Write tests for pagination component state (tests will FAIL
initially).

**File**: `frontend/src/components/storage/StorageDashboard.test.jsx`

Add these test cases:

```javascript
import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import StorageDashboard from "./StorageDashboard";
import messages from "../../languages/en.json";

// Mock API
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const { getFromOpenElisServer } = require("../utils/Utils");

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

describe("StorageDashboard Pagination", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockClear();
  });

  test("testPaginationComponent_Renders_WithDefaultPageSize", async () => {
    // Arrange
    getFromOpenElisServer.mockResolvedValue({
      items: Array(25).fill({}),
      currentPage: 0,
      totalPages: 4,
      totalItems: 100,
      pageSize: 25,
    });

    // Act
    renderWithIntl(<StorageDashboard />);

    // Assert
    await waitFor(() => {
      const pagination = screen.queryByRole("navigation", {
        name: /pagination/i,
      });
      expect(pagination).toBeInTheDocument();
    });
  });

  test("testPageChange_TriggersAPICall_WithCorrectParams", async () => {
    // Arrange
    getFromOpenElisServer.mockResolvedValue({
      items: Array(25).fill({}),
      currentPage: 0,
      totalPages: 4,
      totalItems: 100,
      pageSize: 25,
    });

    renderWithIntl(<StorageDashboard />);

    // Wait for initial render
    await waitFor(() => {
      expect(
        screen.queryByRole("navigation", { name: /pagination/i })
      ).toBeInTheDocument();
    });

    // Act: Click next page
    const nextButton = screen.getByLabelText(/next page/i);
    await userEvent.click(nextButton);

    // Assert
    await waitFor(() => {
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("page=1")
      );
    });
  });
});
```

Run tests (verify FAIL):

```bash
cd frontend && npm test -- StorageDashboard.test.jsx
```

---

### Phase 4: Frontend Implementation (GREEN) - 2 hours

**Goal**: Add pagination component and state management.

**File**: `frontend/src/components/storage/StorageDashboard.jsx`

#### Step 4.1: Add Imports

```javascript
import { Pagination } from "@carbon/react";
```

#### Step 4.2: Add State Variables

```javascript
// Inside component, after existing useState declarations
const [page, setPage] = useState(1); // Carbon uses 1-based indexing
const [pageSize, setPageSize] = useState(25); // Default from OGC-150
const [totalItems, setTotalItems] = useState(0);
```

#### Step 4.3: Update Data Fetching

```javascript
const fetchSamples = async () => {
  try {
    setLoading(true);
    const response = await getFromOpenElisServer(
      `/rest/storage/sample-items?page=${page - 1}&size=${pageSize}` // Convert to 0-based
    );
    setSamples(response.items || []);
    setTotalItems(response.totalItems || 0);
    setLoading(false);
  } catch (error) {
    console.error("Error fetching samples:", error);
    setLoading(false);
  }
};

// Trigger re-fetch when page or pageSize changes
useEffect(() => {
  if (activeTab === 0) {
    // Only fetch when Samples tab is active
    fetchSamples();
  }
}, [activeTab, page, pageSize]);
```

#### Step 4.4: Add Pagination Component

Add after the DataTable component in Samples tab:

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

#### Step 4.5: Run Tests (Verify PASS)

```bash
cd frontend && npm test -- StorageDashboard.test.jsx
```

**Expected**: All tests should PASS now. Frontend pagination complete!

---

### Phase 5: E2E Tests - 1 hour

**File**: `frontend/cypress/e2e/storagePagination.cy.js`

```javascript
describe("Sample Storage Pagination", () => {
  before(() => {
    cy.login("admin", "password");
    cy.loadStorageFixtures(); // Load test data from 001-sample-storage
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/Storage/samples");
  });

  it("should display first page with 25 items by default", () => {
    // Wait for page load
    cy.get('[data-testid="samples-table"]', { timeout: 10000 }).should(
      "be.visible"
    );

    // Verify 25 items displayed (or fewer if less than 25 total)
    cy.get('[data-testid="samples-table"] tbody tr').should(
      "have.length.at.most",
      25
    );

    // Verify pagination controls visible
    cy.get('nav[aria-label*="pagination"]', { timeout: 5000 }).should(
      "be.visible"
    );
  });

  it("should navigate to next page when clicking Next button", () => {
    // Arrange: Set up API intercept
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");

    // Act: Click Next button
    cy.get('button[aria-label*="next page"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Assert: Verify API called with page=1
    cy.wait("@getSamples").its("request.url").should("include", "page=1");
  });

  it("should change page size to 50 items", () => {
    // Arrange: Set up API intercept
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");

    // Act: Change page size
    cy.get('select[aria-label*="items per page"]')
      .should("be.visible")
      .select("50");

    // Assert: Verify API called with size=50
    cy.wait("@getSamples").its("request.url").should("include", "size=50");
  });
});
```

Run test individually (Constitution V.5):

```bash
npm run cy:run -- --spec "cypress/e2e/storagePagination.cy.js"
```

**Post-run checklist** (MANDATORY per Constitution V.5):

1. Review browser console logs in Cypress UI
2. Review screenshots if any failures
3. Verify test output shows all assertions passed

---

### Phase 6: Polish & Verification - 1 hour

#### Step 6.1: Format Code

```bash
# Backend
mvn spotless:apply

# Frontend
cd frontend && npm run format
```

#### Step 6.2: Run Full Test Suite

```bash
# Backend tests
mvn test

# Frontend tests
cd frontend && npm test

# E2E test (individual file)
npm run cy:run -- --spec "cypress/e2e/storagePagination.cy.js"
```

#### Step 6.3: Build Verification

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

#### Step 6.4: Manual Testing

1. Start development environment:

```bash
docker compose -f dev.docker-compose.yml up -d
```

2. Navigate to `https://localhost/Storage/samples`
3. Verify:
   - Page loads with 25 items in <2 seconds
   - Pagination controls visible
   - Next/Previous buttons work
   - Page size selector works (25, 50, 100)
   - Page state preserved when switching tabs

---

## Troubleshooting

### Common Issues

**Issue**: Tests fail with "Method not found" error  
**Solution**: Ensure method signatures match exactly (check imports)

**Issue**: Frontend API calls fail with 404  
**Solution**: Verify backend server is running and endpoint path is correct
(`/rest/storage/sample-items`)

**Issue**: Pagination component doesn't render  
**Solution**: Check Carbon React version is v1.15.0+ (`npm list @carbon/react`)

**Issue**: Tests fail with "LazyInitializationException"  
**Solution**: Ensure @Transactional annotation is on service method (NOT
controller)

---

## Success Checklist

Before creating PR, verify:

- [ ] All backend unit tests pass
- [ ] All backend integration tests pass
- [ ] All frontend unit tests pass
- [ ] E2E test passes (run individually)
- [ ] Browser console logs reviewed (no errors)
- [ ] Code formatted (`mvn spotless:apply` + `npm run format`)
- [ ] Manual testing confirms <2 second page loads with large dataset
- [ ] Screenshots attached to PR showing pagination working

---

## PR Template

```markdown
## Description

Implements server-side pagination for Sample Storage Dashboard per OGC-150.

## Changes

- Added pagination to `SampleStorageService` using Spring Data JPA `Pageable`
- Updated `SampleStorageRestController` GET endpoint with page/size params
- Added Carbon Pagination component to `StorageDashboard.jsx`
- Includes unit, integration, and E2E tests

## Testing

- ✅ Backend unit tests: 5/5 passing
- ✅ Backend integration tests: 4/4 passing
- ✅ Frontend unit tests: 4/4 passing
- ✅ E2E test: 5/5 passing
- ✅ Manual testing: Page loads in <2 seconds with 100k+ samples

## Screenshots

[Attach screenshots showing pagination controls and performance]

## Related

- Issue: OGC-150
- Parent Feature: 001-sample-storage
```

---

## Time Tracking

| Phase                   | Estimated   | Actual | Notes |
| ----------------------- | ----------- | ------ | ----- |
| Backend Tests           | 2 hours     |        |       |
| Backend Implementation  | 2 hours     |        |       |
| Frontend Tests          | 1 hour      |        |       |
| Frontend Implementation | 2 hours     |        |       |
| E2E Tests               | 1 hour      |        |       |
| Polish                  | 1 hour      |        |       |
| **TOTAL**               | **9 hours** |        |       |

---

## References

- **Specification**: [spec.md](./spec.md)
- **Implementation Plan**: [plan.md](./plan.md)
- **Research**: [research.md](./research.md)
- **Jira Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)
- **Testing Roadmap**:
  [testing-roadmap.md](../../.specify/guides/testing-roadmap.md)
