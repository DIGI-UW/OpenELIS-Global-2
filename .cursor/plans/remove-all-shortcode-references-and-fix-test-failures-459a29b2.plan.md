<!-- 459a29b2-0f44-4b13-8160-532ab0b4b747 44e1d3a2-cdb1-4eef-9d9f-5192b0d03aa1 -->

# Run E2E Tests with Electron Logging and Update Testing Roadmap

## Overview

Run all Cypress E2E tests using Electron browser with console logging enabled,
capture output to log file, analyze results, and update testing-roadmap.md to
document this as the recommended debugging setup.

## Current State Analysis

**Package.json Scripts:**

- `cy:run`: Uses Chrome (`cypress run --headless --browser chrome`)
- `test:e2e:full`: Uses Chrome (`cypress run --headless --browser chrome`)
- `test:e2e:single`: Uses Electron
  (`cypress run --browser electron --config video=false,defaultCommandTimeout=10000 --spec`)
- `cy:quick`: Uses Electron
  (`cypress run --browser electron --config video=false,defaultCommandTimeout=10000`)
- `cy:single`: Uses Electron
  (`cypress run --browser electron --config video=false,defaultCommandTimeout=10000 --spec`)

**CI Configuration:**

- `.github/workflows/frontend-qa.yml` line 83: Uses
  `npx cypress run --browser chrome --headless`

**Issue:**

- No npm script exists for running full E2E suite with Electron + console
  logging
- Testing roadmap doesn't document Electron + ELECTRON_ENABLE_LOGGING=1 as
  recommended debugging setup

## Test Results Analysis

**Tests Run So Far:**

- ✅ login.cy.js: 8 passing, 0 failing
- ✅ home.cy.js: 14 passing, 0 failing
- ✅ organizationManagement.cy.js: 6 passing, 0 failing
- ✅ providerManagement.cy.js: 9 passing, 0 failing
- ✅ patientEntry.cy.js: 15 passing, 0 failing
- ✅ barcode.cy.js: 11 passing, 0 failing
- ✅ batchTestReassignmentandCancelation.cy.js: 5 passing, 0 failing
- ⚠️ dashboard.cy.js: 3 passing, 1 pending (known issue: PathologySample
  creation)
- ❌ orderEntity.cy.js: 1 passing, 7 failing
- ❌ calculatedValueTestsManagement.cy.js: 1 passing, 6 failing

**Storage Tests Status:**

- ❌ **ALL storage E2E tests are EXCLUDED** via
  `excludeSpecPattern: ["**/storage*.cy.js"]` in `cypress.config.js` line 310
- 14 storage test files exist but are not being run:
- storageAssignment.cy.js
- storageDashboard.cy.js
- storageDashboardMetrics.cy.js
- storageDisposal.cy.js
- storageFilters.cy.js
- storageLocationCRUD.cy.js
- storageLocationExpandableRows.cy.js
- storageMovement.cy.js
- storageMovementAddLocationDebug.cy.js
- storageMovementFormDebug.cy.js
- storageSamplesTable.cy.js
- storageSearch.cy.js
- storageViewStorage.cy.js
- Comment in config says: "Storage E2E tests (001-sample-storage) are currently
  disabled"

**Console Issues Observed:**

1. **Frequent TypeErrors**:
   `TypeError: Cannot read properties of undefined (reading 'org.openelisglobal.help.manual.url')`

- **Root Cause**: Missing system configuration for help menu URL
  (`org.openelisglobal.help.manual.url`)
- **Impact**: Non-critical - help menu feature doesn't work but doesn't break
  tests
- **Fix Needed**: Add configuration value to system_configuration table or
  handle missing config gracefully

2. **Frequent Fetch Errors**: `TypeError: Failed to fetch` for subscription
   status

- **Root Cause**: Subscription API endpoints not available in test environment
- **Impact**: Non-critical - expected in test environment
- **Fix Needed**: Mock subscription API or handle gracefully when unavailable

3. **React Warning**: Memory leak in HelpMenu component (state update on
   unmounted component)

- **Root Cause**: useEffect cleanup missing in HelpMenu component
- **Impact**: Non-critical - doesn't break tests but indicates memory leak
- **Fix Needed**: Add cleanup function to useEffect in HelpMenu component

**App Status:**

- ✅ Application is reachable and functional at `https://localhost`
- ✅ Authentication working
- ✅ Most core workflows passing
- ✅ Production URL available: `https://storage.openelis-global.org/` (with
  valid certs)

## Implementation Plan

### Step 1: Update Testing Roadmap with Electron Logging Setup

- Add section to `.specify/guides/testing-roadmap.md` after "Test Execution
  Workflow (Constitution V.5)" (around line 2288)
- Document:
- **Recommended Debugging Setup**: Electron browser with
  `ELECTRON_ENABLE_LOGGING=1`
- **Command**: `ELECTRON_ENABLE_LOGGING=1 npm run cy:quick` (for full suite) or
  `ELECTRON_ENABLE_LOGGING=1 npm run cy:single -- "cypress/e2e/{spec}.cy.js"`
  (for single spec)
- **Log Output**: Console logs captured to terminal and can be redirected to
  file: `2>&1 | tee /tmp/cypress-e2e-full.log`
- **When to Use**:
- Electron + logging: Local debugging, investigating console errors
- Chrome: CI/CD pipelines (as configured in `.github/workflows/frontend-qa.yml`)
- **Benefits**:
- Captures browser console logs (INFO:CONSOLE messages)
- Helps identify React warnings, API errors, and JavaScript exceptions
- Useful for debugging test failures and application issues

### Step 2: Document Known Issues (Non-Blocking)

- Add note about non-critical console errors:
- Help menu URL configuration missing (doesn't affect functionality)
- Subscription status API failures (expected in test environment)
- React memory leak warning in HelpMenu (should be fixed but doesn't break
  tests)

### Step 3: Create Fix Plan for Failing Tests

- **orderEntity.cy.js** (7 failing tests):
- Need to investigate specific failures
- Check if related to test data setup or API changes

- **calculatedValueTestsManagement.cy.js** (6 failing tests):
- Need to investigate specific failures
- Check if related to calculation logic or test data

### Step 4: Optional - Add npm Script for Electron Logging

- Consider adding to `package.json`:
- `"cy:debug": "ELECTRON_ENABLE_LOGGING=1 cypress run --browser electron --config video=false,defaultCommandTimeout=10000"`
- Makes it easier to run with logging without remembering environment variable

## Files to Modify

1. `.specify/guides/testing-roadmap.md`

- Add section after "Test Execution Workflow (Constitution V.5)" (around
  line 2288)
- Document Electron + ELECTRON_ENABLE_LOGGING=1 setup
- Update "Configuration Requirements" section if needed

## Expected Outcomes

1. All E2E tests run with Electron and console logging captured
2. Test results analyzed and documented
3. Testing roadmap updated with Electron debugging setup recommendation
4. Fix plan created for any identified issues

### To-dos

- [ ] Write backend unit tests (CodeGenerationServiceTest,
      CodeValidationServiceTest)
- [ ] Run tests to verify they fail (RED phase) - T291, T292 (Tests pass - basic
      implementation complete)
- [ ] Update frontend components (EditLocationModal, PrintLabelButton,
      LocationActionsOverflowMenu)
- [ ] Update valueholders (code VARCHAR(10), remove short_code) - T297, T298
- [ ] Create Liquibase changesets (code column update, remove short_code) -
      T299, T300
- [ ] Implement CodeGenerationService and CodeValidationService (basic
      implementation done)
- [ ] Update StorageLocationServiceImpl for auto-generation and validation -
      T303
- [ ] Backend compiles successfully - all shortCode references replaced with
      code
- [ ] Update EditLocationModal - make code editable, remove shortCode
- [ ] Update test files - replace shortCode with code, ensure valid codes ≤10
      chars
- [ ] Update barcode workflow E2E tests - replace shortCode with code
- [ ] Fix Liquibase migration: Add code column as nullable first, then populate,
      then add NOT NULL constraint
- [ ] Fix test failures: Update test helpers to generate codes ≤10 characters
- [ ] Add CI check scripts to run CI workflow locally before pushing
- [ ] Phase 1: Remove DAO shortCode Methods - Remove findByShortCode() from
      StorageDeviceDAO, StorageShelfDAO, StorageRackDAO and their
      implementations
- [ ] Phase 2: Remove ShortCodeValidationService - Delete service files and
      update LabelManagementRestController
- [ ] Phase 3: Remove ShortCodeUpdateForm - Delete form class
- [ ] Phase 4: Update LabelManagementService - Remove validateShortCodeExists
      and trackPrintHistory methods, or update to use code
- [ ] Phase 5: Update StorageLocationLabel - Remove shortCode from constructor
      and usage
- [ ] Phase 6: Fix Test Failures - Created DAO tests using
      BaseWebContextSensitiveTest, updated documentation
- [ ] Fixed documentation to clarify Spring Framework 6.2.2 (not Spring Boot)
      across AGENTS.md, testing-roadmap.md
- [ ] Fixed remaining shortCode references: renamed validateShortCodeExists to
      validateCodeExists, updated SQL to use location_code, created migration to
      rename column
- [ ] Fixed remaining 3 test errors: StorageDashboardRestControllerTest (code
      too long), LabelManagementServiceIntegrationTest (test expectation),
      StorageLocationServiceImplTest (unnecessary stubbing)
- [ ] Fix dashboard.cy.js test failures - Updated selectors following testing
      roadmap: data-cy (priority 1), ARIA roles (priority 2), semantic selectors
      (priority 3), proper waits with .should() instead of cy.wait()
- [ ] Fix nonConform.cy.js test failures - Already using data-testid, added
      proper waits and visibility checks
- [ ] Fix result.cy.js test failures - Added waits for table data before
      checking button states
- [ ] Fixed SSL certificate generation issue - Manually copied certs from
      certgen image to volumes
