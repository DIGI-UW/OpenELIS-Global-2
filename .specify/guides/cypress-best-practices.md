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

**OverflowMenu**:

```javascript
cy.get('[data-testid="overflow-menu-button"]').click();
cy.get('[role="menu"]').should("be.visible");
cy.get('[role="menuitem"]').contains("Delete").click();
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

---

## Debugging Quick Guide

**Chrome DevTools**:

1. Right-click in Cypress UI → Inspect
2. Sources tab → Open test file
3. Add breakpoint
4. Inspect variables

**Common Issues**:

- Table header row → Use `tbody`
- Viewport issues → Set viewport before visit
- Timing issues → Use `.should()` not `cy.wait()`

---

## Performance Optimization

- **cy.session()**: 10-20x faster login
- **API setup**: 10x faster than UI setup
- **Fixture caching**: Skip if already loaded
- **Individual test runs**: 5-10x faster feedback

---

## Anti-Patterns Checklist

**DO NOT**:

- ❌ Use CSS selectors (use data-testid)
- ❌ Use `cy.wait(5000)` (use `.should()`)
- ❌ Set up intercepts after actions
- ❌ Query by text without context
- ❌ Use UI interactions for test data setup
- ❌ Start new sessions unnecessarily
- ❌ Test implementation details
- ❌ Skip element readiness checks
- ❌ Run full suite during development

---

## Quick Commands

**Run Individual Test** (Development):

```bash
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
```

**Run Full Suite** (CI/CD Only):

```bash
npm run cy:run
```

---

**Last Updated**: 2025-01-XX  
**Reference**: [Testing Roadmap](.specify/guides/testing-roadmap.md) for
comprehensive guidance
