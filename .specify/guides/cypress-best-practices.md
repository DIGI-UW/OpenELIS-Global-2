# Cypress Best Practices Quick Reference

**Quick Reference Guide** for common Cypress E2E testing patterns in OpenELIS
Global 2.

**For Comprehensive Guidance**: See
[Testing Roadmap](.specify/guides/testing-roadmap.md) for detailed patterns and
examples.

**For Functional Requirements**: See
[Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices).

---

## Selector Cheat Sheet (Priority Order)

**STRICT Priority** - Use in this order:

1. **data-testid** (MOST STABLE)

   ```javascript
   cy.get('[data-testid="submit-button"]');
   ```

2. **ARIA roles** (ACCESSIBLE)

   ```javascript
   cy.get('[role="button"]');
   cy.get('[role="dialog"]');
   cy.get('[aria-label="Close"]');
   ```

3. **Semantic with context** (USE CAREFULLY)

   ```javascript
   cy.get('[data-testid="table"]').contains("tr", "Sample-001");
   ```

4. **CSS selectors** (LAST RESORT - STRONGLY DISCOURAGED)
   ```javascript
   cy.get(".button-class"); // Only if no other option
   ```

---

## Session Management

**cy.session() Pattern** (10-20x faster):

```javascript
// In cypress/support/commands.js
Cypress.Commands.add("login", (username, password) => {
  cy.session(
    [username, password],
    () => {
      cy.request({
        method: "POST",
        url: "/api/OpenELIS-Global/LoginPage",
        body: { username, password },
      }).then((response) => {
        // Adapt to OpenELIS authentication
        window.localStorage.setItem("authToken", response.body.token);
      });
    },
    {
      cacheAcrossSpecs: true,
    }
  );
});

// In test files
before(() => {
  cy.login("admin", "password"); // Runs ONCE per test file
});
```

---

## Test Data Patterns

**API Setup** (FAST):

```javascript
before(() => {
  cy.request("POST", "/rest/storage/rooms", {
    name: "Test Room",
    code: "TEST-ROOM",
  }).then((response) => {
    cy.wrap(response.body.id).as("roomId");
  });
});
```

**Fixture Pattern**:

```javascript
cy.intercept("GET", "/rest/storage/rooms", { fixture: "rooms.json" }).as(
  "getRooms"
);
cy.visit("/storage");
cy.wait("@getRooms");
```

**Cleanup**:

```javascript
after(() => {
  cy.get("@roomId").then((id) => {
    cy.request("DELETE", `/rest/storage/rooms/${id}`);
  });
});
```

**config.serverBaseUrl Usage**:

```javascript
// Use config.serverBaseUrl for API endpoints (handles dev vs production)
// In component code (not test code)
import config from "../../config.json";

const endpoint = `${config.serverBaseUrl}/rest/storage/${type}/${id}/print-label`;
```

**Why**: Handles dev vs production URL differences automatically.

---

## Carbon Component Queries

**ComboBox**:

```javascript
cy.get('[data-testid="room-combobox"]').click().type("Main Lab");
cy.get('[role="listbox"]').should("be.visible");
cy.get('[role="option"]').contains("Main Laboratory").click();
```

**DataTable**:

```javascript
cy.get('[data-testid="table"]')
  .find("tbody") // Exclude header
  .find("tr")
  .contains("Room-001")
  .find('[data-testid="action-button"]')
  .click();
```

**Modal/Dialog**:

```javascript
cy.get('[role="dialog"]').should("be.visible");
cy.get('[data-testid="modal-confirm-button"]')
  .should("be.visible")
  .should("not.be.disabled")
  .click();
```

**OverflowMenu** (Portal Rendering):

```javascript
// OverflowMenu items render in portal, outside the parent element
cy.get('[data-testid="overflow-menu-button"]').click();
// Menu items are in portal - don't use .within() for menu items
cy.get('[data-testid="menu-item"]', { timeout: 10000 })
  .should("be.visible")
  .click({ force: true });
```

**Tabs (Carbon Tabs)**:

```javascript
// Carbon Tabs hide non-selected panels with display: none
// Always wait for visible tab panel before interacting
cy.visit("/Storage/rooms");
cy.get('[data-testid="tab-rooms"]', { timeout: 10000 })
  .should("be.visible")
  .should("have.attr", "aria-selected", "true");
cy.get(".cds--tab-content:visible", { timeout: 10000 }).should("be.visible");

// Scope interactions within visible tab panel
cy.get(".cds--tab-content:visible").within(() => {
  cy.get('[data-testid="filter"]').should("be.visible").click();
});
```

**TextInput (Enter Key Handling)**:

```javascript
// Carbon TextInput may pass Enter key as event.key, event.keyCode, or event.code
// Cypress .type("{enter}") handles all variants automatically
cy.get('[data-testid="barcode-input"]')
  .focus()
  .clear()
  .type("MAIN-FRZ01")
  .type("{enter}"); // Handles all Enter key variants

// Note: Component code should check all three for compatibility:
// const isEnterKey = event.key === "Enter" || event.keyCode === 13 || event.code === "Enter";
```

---

## cy.intercept() Quick Reference

**Basic Pattern**:

```javascript
cy.intercept("POST", "/rest/storage/rooms").as("createRoom");
cy.get('[data-testid="save-button"]').click();
cy.wait("@createRoom").its("response.statusCode").should("eq", 201);
```

**With Fixture**:

```javascript
cy.intercept("GET", "/rest/rooms", { fixture: "rooms.json" }).as("getRooms");
```

**Timing**: Set up BEFORE action that triggers it.

**PDF/Blob Response Handling**:

```javascript
// Intercept POST endpoints that return PDFs
cy.intercept("POST", "**/rest/storage/**/print-label").as("printLabel");

// After action triggers request
cy.wait("@printLabel").then((interception) => {
  // Check content-type to distinguish PDF from JSON errors
  const contentType = interception.response.headers["content-type"];
  if (contentType && contentType.includes("application/pdf")) {
    // PDF response - success
    expect(interception.response.statusCode).to.equal(200);
  } else {
    // Error response - parse JSON
    const errorData = interception.response.body;
    console.log("Error:", errorData.error || errorData.message);
  }
});
```

---

## Database Transaction Timing

**When cy.wait() is Acceptable**:

```javascript
// After database updates, wait for transaction to commit before subsequent operations
cy.wait("@updateDevice");
cy.wait(500); // Allow database transaction to commit

// Then proceed with operation that depends on updated data
cy.get('[data-testid="print-label-menu-item"]').click();
```

**Use Case**: When test updates data and immediately needs to use updated data
(e.g., device update followed by label printing).

**Rationale**: Database transactions need time to commit before subsequent
reads.

---

## CSRF Token Handling

**Pattern for Authenticated Requests**:

```javascript
// In component code (not test code)
const endpoint = `${config.serverBaseUrl}/rest/storage/${type}/${id}/print-label`;

const response = await fetch(endpoint, {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-CSRF-Token": localStorage.getItem("CSRF"),
  },
  credentials: "include", // Required for cookie-based auth
});
```

**When to Use**: POST/PUT/DELETE requests that modify data.

**Why**: OpenELIS requires CSRF token for authenticated requests.

**config.serverBaseUrl Usage**:

```javascript
// Use config.serverBaseUrl for API endpoints (handles dev vs production)
import config from "../../config.json";

const endpoint = `${config.serverBaseUrl}/rest/storage/...`;
```

---

## Debugging Quick Guide

**Logging to Files** (Recommended for Debugging):

```bash
# Run test with logging to /tmp file for easier debugging
ELECTRON_ENABLE_LOGGING=1 npm run cy:single -- "cypress/e2e/storageSearch.cy.js" 2>&1 | tee /tmp/storage-search-debug.log

# Then review the log
tail -50 /tmp/storage-search-debug.log
grep -A 5 "CypressError\|AssertionError" /tmp/storage-search-debug.log
```

**Screenshots at Key Points**:

```javascript
// Take screenshots at key points to help debug failures
cy.screenshot("01-dashboard-loaded");
cy.get('[data-testid="button"]').click();
cy.screenshot("02-after-click");
cy.get('[data-testid="modal"]').should("be.visible");
cy.screenshot("03-modal-opened");
```

**Why**: Screenshots help identify UI state at failure points, especially when
tests fail in CI/CD.

**Chrome DevTools**:

1. Right-click in Cypress UI → Inspect
2. Sources tab → Open test file
3. Add breakpoint
4. Inspect variables

**Common Issues**:

- Table header row → Use `tbody`
- Viewport issues → Set viewport before visit
- Timing issues → Use `.should()` not `cy.wait()`

**Error Message Assertion Patterns**:

```javascript
// Error messages may be in title attribute (not text content)
// Check visual feedback component state
cy.get('[data-testid="unified-barcode-input"]')
  .find('[data-state="error"]')
  .should("be.visible");

// If error message is in title attribute
cy.get('[data-state="error"]')
  .should("have.attr", "title")
  .and("include", "Invalid barcode format");
```

**Console.log Debugging Patterns**:

```javascript
// Extensive logging for debugging API calls
cy.wait("@printLabel").then((interception) => {
  console.log("Request URL:", interception.request.url);
  console.log("Request method:", interception.request.method);
  console.log("Request headers:", JSON.stringify(interception.request.headers));
  console.log("Response status:", interception.response?.statusCode);
  console.log(
    "Response headers:",
    JSON.stringify(interception.response?.headers)
  );

  if (interception.response?.statusCode !== 200) {
    console.log("Response body:", JSON.stringify(interception.response?.body));
    // For 400 errors, extract error message
    if (interception.response?.statusCode === 400) {
      const body = interception.response?.body;
      if (body && body.error) {
        console.log("Error message:", body.error);
      }
    }
  }
});
```

**Use Case**: Debugging 400/404 errors in E2E tests when intercepts don't
provide enough detail.

---

## Performance Optimization

- **cy.session()**: 10-20x faster login
- **API setup**: 10x faster than UI setup
- **Fixture caching**: Skip if already loaded
- **Individual test runs**: 5-10x faster feedback

---

## Test Simplification Principles

**Focus on User Workflows**:

```javascript
// ✅ GOOD: Test user-visible behavior
it("Should filter samples by location", function () {
  cy.get('[data-testid="location-filter"]').click();
  cy.get('[role="option"]').first().click();
  cy.get('[role="table"] tbody tr').should("have.length.at.least", 1);
});

// ❌ BAD: Testing implementation details
it("Should call API with correct parameters", function () {
  cy.wait("@getSamples").then((interception) => {
    expect(interception.request.url).to.include("location=");
    expect(interception.response.body[0].status).to.equal("active");
  });
});
```

**Remove Redundancy**:

- Don't test the same workflow multiple times (e.g., filtering by
  active/inactive separately)
- One test per user workflow is enough
- Test happy paths, not every edge case

**Simplify Complex Tests**:

```javascript
// ✅ GOOD: Simple, focused test
it("Should open location management modal", function () {
  cy.get('[data-testid="sample-row"]')
    .first()
    .within(() => {
      cy.get('[data-testid="overflow-menu"]').click();
    });
  cy.get('[data-testid="manage-location-menu-item"]').click();
  cy.get('[data-testid="location-management-modal"]').should("be.visible");
});

// ❌ BAD: Overengineered with unnecessary steps
it("Should open modal, select location, verify API, check response, validate table update...", function () {
  // 50+ lines of implementation detail testing
});
```

## Visibility and Scoping Patterns

**Filter Visible Elements**:

```javascript
// Carbon Tabs and modals may have hidden elements with display: none
// Always filter for visible elements
cy.get('[role="table"]').filter(":visible").should("be.visible");
```

**Scope Interactions with cy.within()**:

```javascript
// Scope interactions to specific containers (modals, tab panels)
cy.get('[data-testid="location-management-modal"]')
  .should("be.visible")
  .within(() => {
    cy.get('[data-testid="location-filter"]').click();
    cy.get('[role="option"]').first().click();
  });
```

**Force Click for Covered Elements**:

```javascript
// Use force: true when element is covered by another element
cy.get('[data-testid="expand-button"]').should("exist").click({ force: true });
```

## Anti-Patterns Checklist

**DO NOT**:

- ❌ Use CSS selectors (use data-testid)
- ❌ Use `cy.wait(5000)` or `cy.wait(1000)` (use `.should()` with retry-ability)
- ❌ Set up intercepts after actions
- ❌ Query by text without context
- ❌ Use UI interactions for test data setup
- ❌ Start new sessions unnecessarily
- ❌ Test implementation details (API URLs, response bodies, status codes)
- ❌ Skip element readiness checks
- ❌ Run full suite during development
- ❌ Test redundant workflows (e.g., active + inactive filters separately)
- ❌ Overengineer tests with unnecessary complexity

---

## Quick Commands

**Run Individual Test** (Development):

```bash
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
```

**Run with Logging to File** (Debugging):

```bash
# Electron with console logging to /tmp file
ELECTRON_ENABLE_LOGGING=1 npm run cy:single -- "cypress/e2e/storageSearch.cy.js" 2>&1 | tee /tmp/storage-search-debug.log

# Review log file
tail -50 /tmp/storage-search-debug.log
grep -A 5 "CypressError\|AssertionError" /tmp/storage-search-debug.log
```

**Run Full Suite** (CI/CD Only):

```bash
npm run cy:run
```

---

**Last Updated**: 2025-01-XX  
**Reference**: [Testing Roadmap](.specify/guides/testing-roadmap.md) for
comprehensive guidance
