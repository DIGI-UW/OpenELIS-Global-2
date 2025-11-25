# Root Cause Analysis: Why These Cypress Tests Keep Failing

**Date**: 2025-01-XX  
**Context**: 8 failing Cypress E2E tests in CI run 19549404810  
**Problem**: Tests have been failing repeatedly despite multiple fix attempts  
**Reference**: [Testing Roadmap](.specify/guides/testing-roadmap.md)

---

## Executive Summary

These tests violate **every major principle** in the Testing Roadmap. They are
fundamentally misdesigned and cannot be reliably fixed with patches. They need
**complete redesign** following Testing Roadmap patterns.

**Key Finding**: The tests are failing not because of bugs in the application,
but because the tests themselves are **architecturally flawed** and **violate
Cypress best practices**.

---

## Critical Violations of Testing Roadmap

### 1. Arbitrary Waits (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[Cypress E2E Testing - DOM Query Effectiveness](.specify/guides/testing-roadmap.md#dom-query-effectiveness)

**What the Roadmap Says**:

> **DON'T**: Use `cy.wait(5000)` instead of `.should()` assertions (profy.dev
> anti-pattern)

**What These Tests Do**:

- `patientEntry.cy.js`: **15+ instances** of `cy.wait(1000)`, `cy.wait(200)`,
  `cy.wait(500)`
- `modifyOrder.cy.js`: **10+ instances** of arbitrary waits
- `dashboard.cy.js`: Multiple `cy.wait(1000)` calls
- `orderEntity.cy.js`: `cy.wait(1000)`, `cy.wait(300)`, `cy.wait(200)`

**Why This Is Fatal**:

- Arbitrary waits have **no retry logic** - if element takes 1100ms to appear,
  test fails
- Tests become **brittle** - any performance change breaks them
- **No feedback** on what's actually happening - just "wait and hope"

**Example from `patientEntry.cy.js`**:

```javascript
it("Search patient By Date Of Birth", function () {
  cy.wait(1000); // ❌ VIOLATION: Arbitrary wait
  cy.fixture("Patient").then((patient) => {
    patientPage.searchPatientByDateOfBirth(patient.DOB);
    patientPage.clickSearchPatientButton();
    // ...
  });
  cy.wait(200).reload(); // ❌ VIOLATION: Another arbitrary wait
});
```

**Correct Pattern** (from Testing Roadmap):

```javascript
it("Search patient By Date Of Birth", function () {
  cy.fixture("Patient").then((patient) => {
    patientPage.searchPatientByDateOfBirth(patient.DOB);
    patientPage.clickSearchPatientButton();
    // ✅ CORRECT: Use .should() for retry-ability
    cy.get("table tbody tr", { timeout: 10000 }).should(
      "have.length.greaterThan",
      0
    );
  });
});
```

---

### 2. No Session Management (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[Session Management (cy.session())](.specify/guides/testing-roadmap.md#session-management-cysession)

**What the Roadmap Says**:

> **CRITICAL**: Use `cy.session()` to preserve login state across tests (10-20x
> faster - Cypress official pattern).

**What These Tests Do**:

- `patientEntry.cy.js`: `before("login", () => { loginPage.visit(); })` - **NO
  cy.session()**
- `orderEntity.cy.js`: Same pattern - login every time
- `dashboard.cy.js`: Same pattern
- `workplan.cy.js`: Same pattern
- `result.cy.js`: Same pattern
- `modifyOrder.cy.js`: Same pattern

**Why This Is Fatal**:

- **10-20x slower** than necessary (per Testing Roadmap)
- Login runs **before every test** instead of once per file
- **Race conditions** - if login takes longer, tests fail
- **No session caching** - redundant authentication

**Example from `patientEntry.cy.js`**:

```javascript
before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit(); // ❌ VIOLATION: No cy.session()
});
```

**Correct Pattern** (from Testing Roadmap):

```javascript
before("login", () => {
  cy.session("test-session", () => {
    const loginPage = new LoginPage();
    loginPage.visit();
    loginPage.goToHomePage();
  });
  // Session cached - login only runs once per test file
});
```

**Note**: Only `storage-setup.js` uses `cy.session()` correctly. All other test
files violate this.

---

### 3. CSS Selectors Instead of data-testid (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[Selector Strategy (MANDATORY Priority)](.specify/guides/testing-roadmap.md#selector-strategy-mandatory-priority)

**What the Roadmap Says**:

> **STRICT Priority Order**:
>
> 1. **data-testid attributes** (MOST STABLE - PREFERRED)
> 2. **ARIA roles and labels** (ACCESSIBLE - SECOND CHOICE)
> 3. **Semantic selectors with context** (TEXT CONTENT - USE CAREFULLY)
> 4. **CSS selectors** (LAST RESORT - STRONGLY DISCOURAGED)

**What These Tests Do**:

- `patientEntry.cy.js`: `#labNumber`, `#display_labNumber`, `#local_search`,
  `.cds--data-table` - **ALL CSS selectors**
- `orderEntity.cy.js`: `button:contains("Print Barcode")` - **Text-based
  selector without context**
- `workplan.cy.js`: `select#select-1`, `button:contains(/Print Workplan?/i)` -
  **CSS + text selectors**
- `result.cy.js`: `.cds--data-table tbody`, `table tbody tr` - **CSS selectors**
- `modifyOrder.cy.js`: `table input[type="checkbox"][name="add"]` - **Deep CSS
  selector chain**

**Why This Is Fatal**:

- **Brittle** - breaks when CSS classes change (Carbon Design System updates)
- **No semantic meaning** - doesn't express intent
- **Hard to maintain** - refactoring breaks tests
- **Not accessible** - doesn't test what users actually interact with

**Example from `patientEntry.cy.js`**:

```javascript
cy.get("#display_labNumber, #labNumber", { timeout: 10000 }) // ❌ VIOLATION: CSS selector
  .first()
  .should("be.visible")
  .clear()
  .type(patient.labNo);
```

**Correct Pattern** (from Testing Roadmap):

```javascript
cy.get('[data-testid="lab-number-input"]') // ✅ CORRECT: data-testid
  .should("be.visible")
  .clear()
  .type(patient.labNo);
```

**Note**: `barcodeWorkflow.cy.js` correctly uses `data-testid` - this is why
it's more stable.

---

### 4. Arbitrary Waits vs UI State Waits

**Testing Roadmap Section**:
[cy.intercept() Patterns](.specify/guides/testing-roadmap.md#cyintercept-patterns)

**What the Roadmap Says**:

> **IMPORTANT**: Intercepts are NOT required for basic E2E tests. Wait for UI
> state instead.

**What These Tests Do**:

- `patientEntry.cy.js` (DOB search): Waits for table to appear ✅ (correct
  approach)
- `orderEntity.cy.js`: Waits for UI elements ✅ (correct approach)
- Some tests use `cy.wait(1000)` ❌ (arbitrary wait - wrong)

**Why Wait for UI State**:

- **Tests what users see** - If UI is correct, test passes
- **Cypress retry-ability** - `.should()` automatically retries until condition
  is met
- **Simpler** - No URL pattern matching dependencies
- **More reliable** - No timeouts from intercepts that never fire
- **API issues caught elsewhere** - Integration/backend tests verify API
  behavior

**Example from `patientEntry.cy.js` (DOB search)**:

```javascript
it("Search patient By Date Of Birth", function () {
  cy.wait(1000); // ❌ VIOLATION: Arbitrary wait
  cy.fixture("Patient").then((patient) => {
    patientPage.searchPatientByDateOfBirth(patient.DOB);
    patientPage.clickSearchPatientButton();
    // ❌ VIOLATION: Arbitrary wait instead of waiting for UI
    cy.wait(2000);
    patientPage.validatePatientSearchTablebyRespectiveField(patient.DOB, "DOB");
  });
});
```

**Correct Pattern** (wait for UI state):

```javascript
it("Search patient By Date Of Birth", function () {
  cy.fixture("Patient").then((patient) => {
    patientPage.searchPatientByDateOfBirth(patient.DOB);
    patientPage.clickSearchPatientButton();
    // ✅ CORRECT: Wait for UI to show results (Cypress retries automatically)
    cy.get("table tbody tr").should("be.visible").should("have.length.greaterThan", 0);
    cy.get("table tbody").should("contain.text", "TEST-Smith");
      expect(interception.response.body.patientSearchResults).to.be.an("array");
    });
    // Then validate table
    patientPage.validatePatientSearchTablebyRespectiveField(patient.DOB, "DOB");
  });
});
```

---

### 5. No Viewport Management (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[DOM Query Effectiveness](.specify/guides/testing-roadmap.md#dom-query-effectiveness)

**What the Roadmap Says**:

> **Viewport management** (profy.dev: set viewport before visit):
>
> ```javascript
> beforeEach(() => {
>   cy.viewport(1025, 900); // Desktop viewport
>   cy.visit("/dashboard");
> });
> ```

**What These Tests Do**:

- **NONE** of the failing tests set viewport before `cy.visit()`
- Tests rely on default viewport (may vary by environment)
- Mobile/desktop differences cause failures

**Why This Is Fatal**:

- **Inconsistent** - different viewports show different UI (mobile vs desktop)
- **Hidden elements** - some elements only visible at certain viewport sizes
- **Layout differences** - Carbon components render differently on mobile

**Example**: All failing tests lack viewport management.

**Correct Pattern** (from Testing Roadmap):

```javascript
beforeEach(() => {
  cy.viewport(1025, 900); // Desktop viewport
  cy.visit("/PatientManagement");
});
```

---

### 6. Fragile Table Validation (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[DOM Query Effectiveness - Table row filtering](.specify/guides/testing-roadmap.md#dom-query-effectiveness)

**What the Roadmap Says**:

> **Table row filtering** (profy.dev debugging example - use `tbody`):
>
> ```javascript
> cy.get("main")
>   .find("tbody") // Exclude thead
>   .find("tr")
>   .each(($el, index) => {
>     // Process data rows only
>   });
> ```

**What These Tests Do**:

- `patientEntry.cy.js`: Uses `.cds--data-table > tbody` but **doesn't wait for
  rows** before validation
- `result.cy.js`: Uses `.cds--data-table tbody` but **doesn't verify table
  loaded** before checking rows
- `workplan.cy.js`: Uses `tbody tr` but **no wait for API** - just hopes table
  appears
- `modifyOrder.cy.js`: Uses `table input[type="checkbox"]` - **deep CSS
  selector**, no readiness check

**Why This Is Fatal**:

- **Race conditions** - validation runs before table renders
- **Header row confusion** - may select header instead of data rows
- **No empty state handling** - fails if API returns empty array
- **Brittle selectors** - breaks when table structure changes

**Example from `result.cy.js`**:

```javascript
it("should accept the sample, refer the sample, and save the result", function () {
  // ❌ VIOLATION: No wait for API, no verification table loaded
  cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
  result.expandSampleDetails();
  // ...
});
```

**Correct Pattern** (from Testing Roadmap):

```javascript
it("should accept the sample, refer the sample, and save the result", function () {
  // ✅ CORRECT: Wait for API, then verify table
  cy.intercept("GET", "**/rest/results**").as("getResults");
  cy.wait("@getResults");
  cy.get("table tbody tr", { timeout: 10000 }).should(
    "have.length.greaterThan",
    0
  );
  result.expandSampleDetails();
  // ...
});
```

---

### 7. Reloads Between Tests (CODE SMELL)

**What These Tests Do**:

- `patientEntry.cy.js`: `cy.wait(200).reload()` after **every test**
- `modifyOrder.cy.js`: `cy.wait(200).reload()` after multiple tests
- `dashboard.cy.js`: Implicit reloads via navigation

**Why This Is Fatal**:

- **Slow** - unnecessary page reloads
- **Hides test isolation issues** - if tests need reloads, they're not isolated
- **Race conditions** - reload may not complete before next test
- **No session preservation** - may lose authentication state

**Example from `patientEntry.cy.js`**:

```javascript
it("Search Patient By FirstName only", function () {
  // ... test logic ...
  cy.wait(200).reload(); // ❌ VIOLATION: Unnecessary reload
});
```

**Correct Pattern**: Tests should be isolated - no reloads needed. If reloads
are needed, tests are not properly isolated.

---

### 8. Complex, Fragile Validation Logic (CRITICAL VIOLATION)

**What These Tests Do**:

- `PatientEntryPage.js`: `validatePatientSearchTablebyRespectiveField()` -
  **200+ lines** of complex validation
- DOB validation: Checks for "TEST-Smith" in results instead of exact DOB match
  (workaround, not fix)
- Table validation: Uses `nth-child()` selectors, text matching, complex
  conditionals

**Why This Is Fatal**:

- **Overly complex** - hard to understand, maintain, debug
- **Brittle** - breaks when table structure changes
- **Workarounds instead of fixes** - DOB validation checks name instead of DOB
  (hides real issue)
- **No clear intent** - what are we actually testing?

**Example from `PatientEntryPage.js`**:

```javascript
validatePatientSearchTablebyRespectiveField(expectedFieldValue, searchBy) {
  if (searchBy === "DOB") {
    // ❌ VIOLATION: Complex, fragile validation
    this.getPatientSearchResultsTable()
      .find("tr")
      .should("have.length.greaterThan", 0)
      .then(($rows) => {
        let foundPatient = false;
        cy.wrap($rows)
          .each(($row) => {
            cy.wrap($row)
              .find("td")
              .eq(1)
              .invoke("text")
              .then((lastName) => {
                if (lastName.trim().includes("TEST-Smith")) {
                  foundPatient = true;
                }
              });
          })
          .then(() => {
            expect(foundPatient, `Expected to find patient TEST-Smith...`).to.be.true;
          });
      });
  }
  // ... more complex validation ...
}
```

**Correct Pattern** (from Testing Roadmap):

```javascript
// ✅ CORRECT: Simple, clear validation
validatePatientSearchResults(expectedPatient) {
  cy.intercept("GET", "**/rest/patient-search-results*").as("getPatientSearch");
  cy.wait("@getPatientSearch").then((interception) => {
    const results = interception.response.body.patientSearchResults;
    const found = results.some(p => p.id === expectedPatient.id);
    expect(found).to.be.true;
  });
  // Then validate UI shows the patient
  cy.get('[data-testid="patient-search-results"]')
    .contains(expectedPatient.lastName)
    .should("be.visible");
}
```

---

### 9. No Test Data Isolation (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[Test Data Management (API-First)](.specify/guides/testing-roadmap.md#test-data-management-api-first)

**What the Roadmap Says**:

> **DO**: Use `cy.request()` for fast test data setup (profy.dev
> recommendation). **DON'T**: Use slow UI interactions for setup (profy.dev
> anti-pattern).

**What These Tests Do**:

- `patientEntry.cy.js`: "Add New Patient" test **creates data** that conflicts
  with "Search Patient" tests
- Tests depend on **shared mutable database state**
- Fixture loading happens in `before()` but **no verification** it worked
- Tests **pollute each other's data** - "Add New Patient" creates patients that
  affect search results

**Why This Is Fatal**:

- **Non-deterministic** - test results depend on execution order
- **Flaky** - fails if previous test didn't clean up
- **Hard to debug** - "why did search return wrong patient?" → "because previous
  test created one"
- **No isolation** - tests can't run in parallel

**Example from `patientEntry.cy.js`**:

```javascript
describe("Add New Patient", function () {
  // ❌ VIOLATION: Creates patient that may conflict with search tests
  it("Enter patient Information and save", function () {
    patientPage.enterPatientInfo(/* ... */);
    patientPage.clickSavePatientButton();
    // No cleanup - patient persists for next tests
  });
});

describe("Search Patient", function () {
  // ❌ VIOLATION: May find patient from "Add New Patient" test
  it("Search Patient By FirstName only", function () {
    patientPage.searchPatientByFirstNameOnly(patient.firstName);
    // May return multiple patients (fixture + test-created)
  });
});
```

**Correct Pattern** (from Testing Roadmap):

```javascript
describe("Add New Patient", function () {
  it("Enter patient Information and save", function () {
    // ✅ CORRECT: Use unique test data
    const uniquePatient = {
      firstName: `TestCreate-${Date.now()}`,
      lastName: `TestPatient-${Date.now()}`,
      // ...
    };
    patientPage.enterPatientInfo(/* uniquePatient */);
    patientPage.clickSavePatientButton();
    // Cleanup after test
    cy.request("DELETE", `/rest/patients/${createdPatientId}`);
  });
});
```

---

### 10. Missing Element Readiness Checks (CRITICAL VIOLATION)

**Testing Roadmap Section**:
[DOM Query Effectiveness - Chaining with .should()](.specify/guides/testing-roadmap.md#dom-query-effectiveness)

**What the Roadmap Says**:

> **Chaining with .should()** (Cypress retry-ability):
>
> ```javascript
> cy.get('[data-testid="submit-button"]')
>   .should("be.visible")
>   .should("not.be.disabled")
>   .click();
> ```

**What These Tests Do**:

- `orderEntity.cy.js`: Clicks submit button **without checking** if it's enabled
- `workplan.cy.js`: Clicks dropdown **without checking** if it's ready
- `modifyOrder.cy.js`: Clicks checkboxes **without checking** if table loaded
- `result.cy.js`: Expands sample details **without checking** if table has rows

**Why This Is Fatal**:

- **Race conditions** - clicks before element is ready
- **No retry logic** - fails immediately if element not ready
- **Flaky** - works sometimes, fails other times

**Example from `orderEntity.cy.js`**:

```javascript
it("should click submit order button", function () {
  orderEntityPage.clickSubmitOrderButton(); // ❌ VIOLATION: No readiness check
  cy.get('[data-testid="success-message"]', { timeout: 15000 }).should(
    "be.visible"
  );
});
```

**Correct Pattern** (from Testing Roadmap):

```javascript
it("should click submit order button", function () {
  // ✅ CORRECT: Check readiness before action
  cy.get('[data-testid="submit-order-button"]')
    .should("be.visible")
    .should("not.be.disabled")
    .click();
  cy.get('[data-testid="success-message"]', { timeout: 15000 }).should(
    "be.visible"
  );
});
```

---

## Summary: Why These Tests Are So Problematic

### Root Cause #1: Architectural Violations

These tests violate **every major architectural principle** in the Testing
Roadmap:

1. ❌ **No session management** - login runs every time (10-20x slower)
2. ❌ **Arbitrary waits everywhere** - no retry logic, brittle timing
3. ❌ **CSS selectors** - brittle, breaks on refactoring
4. ❌ **No API intercepts** - no visibility into what's happening
5. ❌ **No viewport management** - inconsistent UI rendering
6. ❌ **No element readiness checks** - race conditions
7. ❌ **No test data isolation** - tests pollute each other
8. ❌ **Complex validation logic** - hard to maintain, brittle

### Root Cause #2: Missing Fundamentals

These tests are missing **basic Cypress best practices**:

- No `cy.session()` for login caching
- No `cy.intercept()` for API visibility
- No `.should()` for retry-ability
- No `data-testid` attributes
- No viewport management
- No proper test isolation

### Root Cause #3: Workarounds Instead of Fixes

The tests use **workarounds** that hide problems instead of fixing them:

- DOB validation checks name instead of DOB (hides date format issue)
- Reloads between tests (hides test isolation issues)
- Arbitrary waits (hides timing issues)
- Complex validation logic (hides selector issues)

---

## Why Patches Don't Work

**Every fix attempt** has been a **patch** that addresses symptoms, not root
causes:

1. **Fix DOB mismatch** → Update database → **Doesn't fix** test design
2. **Fix lab number search** → Add intercept → **Doesn't fix** other missing
   intercepts
3. **Fix table validation** → Check for rows → **Doesn't fix** missing API waits
4. **Fix Print Barcode button** → Add timeout → **Doesn't fix** missing
   readiness checks

**The tests are fundamentally broken** - patches can't fix architectural
violations.

---

## What Needs to Happen

### Option 1: Complete Redesign (RECOMMENDED)

**Redesign all 8 failing tests** following Testing Roadmap patterns:

1. **Add `cy.session()`** for login caching
2. **Replace all `cy.wait()`** with `.should()` assertions
3. **Add `data-testid` attributes** to all interactive elements
4. **Add API intercepts** for all async operations
5. **Add viewport management** before all `cy.visit()`
6. **Simplify validation logic** - test API responses, then UI
7. **Isolate test data** - use unique data, clean up after tests
8. **Add element readiness checks** before all interactions

**Effort**: ~2-3 days per test file (8 files = 16-24 days)

### Option 2: Incremental Migration (ALTERNATIVE)

**Migrate one test file at a time** following Testing Roadmap migration
strategy:

1. Start with `patientEntry.cy.js` (most violations)
2. Apply all Testing Roadmap patterns
3. Verify it's stable
4. Move to next file

**Effort**: Same as Option 1, but spread over time

### Option 3: Delete and Rewrite (FASTEST)

**Delete failing tests** and rewrite from scratch using Testing Roadmap
templates:

1. Use `.specify/templates/testing/CypressE2E.cy.js.template`
2. Follow Testing Roadmap patterns from the start
3. Write tests that actually test user workflows

**Effort**: ~1 day per test file (8 files = 8 days)

---

## Conclusion

These tests are failing because they **violate every major principle** in the
Testing Roadmap. They cannot be reliably fixed with patches - they need
**complete redesign** following Testing Roadmap patterns.

**The tests are not testing the application - they're testing whether arbitrary
waits are long enough and whether CSS selectors still work.**

**Recommendation**: Choose Option 3 (Delete and Rewrite) for fastest path to
stable tests, or Option 1 (Complete Redesign) for comprehensive fix.

---

## References

- [Testing Roadmap](.specify/guides/testing-roadmap.md) - Comprehensive testing
  guide
- [Cypress Best Practices Guide](.specify/guides/cypress-best-practices.md) -
  Quick reference
- [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices) -
  E2E testing requirements
- [Cypress E2E Template](.specify/templates/testing/CypressE2E.cy.js.template) -
  Standard test template
