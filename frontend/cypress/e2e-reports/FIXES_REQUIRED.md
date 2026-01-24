# E2E Test Failure Analysis & Solutions

**Generated:** $(date)  
**Test Run:** Chunk 3-7 (Tests 3-7 of 51)  
**Failures:** 2 of 48 tests

---

## Failure #1: patientEntry.cy.js - "Search patient By Date Of Birth"

### Error
```
CypressError: Timed out retrying after 3050ms: `cy.click()` failed because this element:
`<button id="local_search" data-cy="searchPatientButton">Search</button>`
is being covered by another element:
`<a id="menu_administration_nav" href="/MasterListsPage" class="cds--side-nav__link">...</a>`
```

### Root Cause
The search button is covered by the sidebar navigation menu. This is a UI layout issue where the Carbon Design System sidebar overlaps the button.

### Location
- **Test File:** `cypress/e2e/patientEntry.cy.js:130`
- **Page Object:** `cypress/pages/PatientEntryPage.js:86`
- **Method:** `clickSearchPatientButton()`

### Current Code
```javascript
// cypress/pages/PatientEntryPage.js:86
clickSearchPatientButton() {
  cy.getElement("#local_search").should("be.visible").click();
}
```

### Solution
**Option 1 (Recommended):** Scroll element into view before clicking
```javascript
clickSearchPatientButton() {
  cy.getElement("#local_search")
    .should("be.visible")
    .scrollIntoView()
    .click();
}
```

**Option 2:** Use force click (less ideal, but works)
```javascript
clickSearchPatientButton() {
  cy.getElement("#local_search")
    .should("be.visible")
    .click({ force: true });
}
```

**Option 3:** Close sidebar before clicking (if sidebar can be toggled)
```javascript
clickSearchPatientButton() {
  // Close sidebar if open
  cy.get("body").then(($body) => {
    if ($body.find(".cds--side-nav--expanded").length > 0) {
      cy.get("[data-cy='side-nav-toggle']").click();
    }
  });
  cy.getElement("#local_search").should("be.visible").click();
}
```

### Recommendation
Use **Option 1** (scrollIntoView) as it's the most robust and doesn't bypass Cypress's actionability checks.

---

## Failure #2: orderEntity.cy.js - "Select sample type"

### Error
```
AssertionError: Timed out retrying after 3000ms: Expected to find element: `#referralReasonId_0_1`, but never found it.
```

### Root Cause
The referral reason dropdown (`#referralReasonId_0_1`) doesn't exist in the DOM when the test tries to select it. This suggests:
1. The "Refer test to a reference lab" checkbox wasn't properly clicked/activated
2. The form didn't render the referral fields after clicking
3. The element ID changed or the form structure changed

### Location
- **Test File:** `cypress/e2e/orderEntity.cy.js:57`
- **Page Object:** `cypress/pages/OrderEntityPage.js:55-56`
- **Method:** `selectReferralReason()`

### Current Code
```javascript
// cypress/pages/OrderEntityPage.js:47-56
referTest() {
  cy.contains("span", "Refer test to a reference lab").click();
}

selectReferralReason() {
  cy.get("#referralReasonId_0_1").select("Test not performed");
}
```

### Solution
**Option 1 (Recommended):** Wait for element to appear after clicking refer checkbox
```javascript
referTest() {
  cy.contains("span", "Refer test to a reference lab").click();
  // Wait for referral fields to render
  cy.get("#referralReasonId_0_1", { timeout: 10000 })
    .should("be.visible");
}

selectReferralReason() {
  cy.get("#referralReasonId_0_1")
    .should("be.visible")
    .select("Test not performed");
}
```

**Option 2:** Check if element exists before selecting
```javascript
selectReferralReason() {
  cy.get("body").then(($body) => {
    if ($body.find("#referralReasonId_0_1").length > 0) {
      cy.get("#referralReasonId_0_1")
        .should("be.visible")
        .select("Test not performed");
    } else {
      cy.log("Referral reason field not found - skipping");
    }
  });
}
```

**Option 3:** Use more robust selector with data-testid
```javascript
selectReferralReason() {
  // If element has data-testid attribute
  cy.get("[data-testid='referral-reason-select']")
    .should("be.visible")
    .select("Test not performed");
}
```

### Investigation Needed
1. Check if the checkbox click actually triggers the form to show referral fields
2. Verify the element ID hasn't changed in the React component
3. Check browser console for JavaScript errors that might prevent rendering
4. Verify test data fixtures include referral reason options

### Recommendation
Use **Option 1** with explicit wait and visibility check. Also add logging to understand the form state.

---

## Test Execution Summary

### Chunk 3-7 Results
- **Total Tests:** 48
- **Passed:** 40
- **Failed:** 2
- **Skipped:** 6
- **Success Rate:** 83.3%

### Passing Tests
✅ AdminE2E/providerManagement.cy.js (9/9)  
✅ AdminE2E/barcode.cy.js (11/11)  
✅ AdminE2E/batchTestReassignmentandCancelation.cy.js (5/5)  
✅ patientEntry.cy.js (12/13) - 1 failure  
✅ orderEntity.cy.js (3/8) - 1 failure, 4 skipped

---

## Next Steps

1. **Fix patientEntry.cy.js:**
   - Update `PatientEntryPage.js:86` to use `scrollIntoView()` before clicking

2. **Fix orderEntity.cy.js:**
   - Update `OrderEntityPage.js:47-56` to wait for referral fields after clicking checkbox
   - Add visibility checks before selecting dropdown

3. **Re-run failing tests:**
   ```bash
   cd frontend
   ./run-e2e-chunks.sh 5 3  # Re-run chunk 3-7
   ```

4. **Continue with remaining chunks:**
   ```bash
   ./run-e2e-chunks.sh 5 8   # Next chunk (tests 8-12)
   ```

5. **Monitor for patterns:**
   - If more sidebar coverage issues appear, consider a global fix
   - If more missing element issues appear, check form rendering timing

---

## CI Compatibility

These fixes maintain CI compatibility:
- ✅ Uses same command: `npx cypress run --browser chrome --headless`
- ✅ Screenshots enabled for failure analysis
- ✅ No changes to test structure or data
- ✅ Uses Cypress best practices (retry-ability, visibility checks)
