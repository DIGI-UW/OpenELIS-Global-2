# CI Test Status Report

**Generated:** 2025-11-12  
**Branch:** 001-sample-storage

## Executive Summary

| Check               | Status     | Details                                           |
| ------------------- | ---------- | ------------------------------------------------- |
| Backend Formatting  | ✅ PASS    | All Java files pass Spotless check                |
| Backend Tests       | ❌ FAIL    | 2 failures, 59 errors out of 2252 tests           |
| Frontend Formatting | ✅ PASS    | All files pass Prettier check                     |
| Frontend Jest Tests | ❌ FAIL    | 17 failures, 183 passed out of 200 tests          |
| Cypress E2E Tests   | ⏸️ NOT RUN | Requires Docker containers (not executed locally) |

---

## Backend Test Results

### Overall Statistics

- **Total Tests:** 2,252
- **Passed:** 2,191
- **Failed:** 2
- **Errors:** 59
- **Skipped:** 1
- **Success Rate:** 97.3%

### Test Failures (2)

1. **ResultServiceTest.getReportingTestName_shouldReturnReportingTestName**

   - **Error:** Expected `[GPT/ALAT]` but was `[]`
   - **Location:** `org.openelisglobal.result.service.ResultServiceTest:266`

2. **ResultServiceTest.getTestDescription_shouldReturnTestDescription**
   - **Error:** Expected `[GPT/ALAT(Serum)]` but was `[]`
   - **Location:** `org.openelisglobal.result.service.ResultServiceTest:201`

### Test Errors (59) - Categories

#### 1. SQL Grammar Errors (Most Common - ~50 errors)

**Issue:** Bad SQL grammar in test setup methods, primarily:

- `INSERT INTO storage_position` statements with incorrect syntax
- `INSERT INTO storage_device` statements with incorrect syntax
- `INSERT INTO sample_item` statements with incorrect syntax

**Affected Tests:**

- `BarcodeValidationRestControllerTest` (multiple setup errors)
- `LabelManagementRestControllerTest` (multiple setup errors)
- `StorageLocationRestControllerTest`
- `StorageDashboardRestControllerTest`
- `StorageSearchRestControllerTest` (multiple setup errors)
- `PositionHierarchyMigrationTest`

**Root Cause:** Test setup methods are using raw SQL INSERT statements that
don't match the actual database schema or are missing required columns.

#### 2. EmptyResultDataAccess Errors (~7 errors)

**Issue:** Database queries expecting 1 result but getting 0

**Affected Tests:**

- `StorageDashboardRestControllerTest.setUp:49->createTestStorageHierarchyWithSamples:428`
- `StorageSearchRestControllerTest.setUp:58->createTestStorageHierarchyWithSamples:489`
  (multiple)

**Root Cause:** Test data setup is not properly creating required entities or
foreign key relationships are missing.

#### 3. NullPointerException Errors (2 errors)

**Issue:** Null pointer when accessing `StorageDevice.getParentRoom()`

**Affected Tests:**

- `StorageLocationFhirTransformTest.testHierarchicalCodeGeneration_BuildsCorrectPath:190`
- `StorageLocationFhirTransformTest.testTransformStoragePositionToFhirLocation_ValidPosition_ReturnsLocationWithOccupancyExtension:151`

**Root Cause:** Test setup is not properly initializing `StorageDevice` objects
with parent relationships.

#### 4. UnnecessaryStubbing Warning (1)

**Issue:** Mockito unnecessary stubbing detected

**Affected Test:**

- `BarcodeValidationServiceTest.testDetectLocationBarcode` (lines 402-403)

**Note:** This is a warning, not a failure, but should be cleaned up.

---

## Frontend Test Results

### Overall Statistics

- **Total Tests:** 200
- **Passed:** 183
- **Failed:** 17
- **Success Rate:** 91.5%

### Test Failures (17)

#### Primary Issue: Module Resolution Errors

**Error:** `Cannot find module '../utils/Utils'`

**Affected Test Files:**

- `src/components/storage/__tests__/TEST_TEMPLATE.jsx` (and potentially others)

**Root Cause:** Test template file references a utility module that doesn't
exist or has been moved/renamed.

**Additional Failures:**

- Multiple test suites failed to run due to module resolution issues
- 7 test suites failed, 16 passed, 23 total

---

## Previously Fixed Tests ✅

The following tests were **successfully fixed** in this session:

1. ✅ **HibernateMappingValidationTest** - Added missing XML mappings
2. ✅
   **LabelManagementRestControllerTest.testPutShortCodeEndpoint_LocationNotFound_Returns404** -
   Fixed Optional handling
3. ✅ **BarcodeValidationServiceTest.testUnknownBarcodeType** - Fixed barcode
   type detection logic
4. ✅ **BarcodeValidationServiceTest.testErrorMessageFormatWhenParsingFails** -
   Fixed error message formatting
5. ✅ **StorageDashboardServiceImplTest.testGetRacks_IncludesRoomColumn** -
   Fixed test expectation (roomId → parentRoomId)

---

## Recommendations

### High Priority

1. **Fix SQL Grammar Errors in Test Setup**

   - Review and fix all `INSERT INTO` statements in test setup methods
   - Ensure SQL matches actual database schema
   - Consider using JPA/Hibernate for test data creation instead of raw SQL

2. **Fix Module Resolution in Frontend Tests**

   - Update `TEST_TEMPLATE.jsx` to use correct import paths
   - Verify all utility modules exist and are properly exported

3. **Fix ResultServiceTest Failures**
   - Investigate why `getReportingTestName` and `getTestDescription` return
     empty strings
   - Check if test data setup is missing required fields

### Medium Priority

4. **Fix EmptyResultDataAccess Errors**

   - Ensure test data setup creates all required entities with proper
     relationships
   - Verify foreign key constraints are satisfied

5. **Fix NullPointerException in FHIR Transform Tests**
   - Ensure `StorageDevice` objects are properly initialized with parent
     relationships
   - Review test setup methods for `StorageLocationFhirTransformTest`

### Low Priority

6. **Clean Up Unnecessary Stubbing**
   - Remove unnecessary Mockito stubbings in `BarcodeValidationServiceTest`

---

## Next Steps

1. **Immediate:** Fix the 2 ResultServiceTest failures and SQL grammar errors
2. **Short-term:** Resolve frontend module resolution issues
3. **Medium-term:** Fix remaining test setup issues (EmptyResultDataAccess,
   NullPointerException)
4. **Long-term:** Consider refactoring test setup to use JPA/Hibernate instead
   of raw SQL

---

## Test Execution Commands

```bash
# Backend formatting check
mvn spotless:check

# Backend tests
mvn clean install -Dspotless.check.skip=true

# Frontend formatting check
cd frontend && npx prettier ./ --check

# Frontend Jest tests
cd frontend && CI=true npm test -- --watchAll=false --coverage=false

# Cypress E2E tests (requires Docker)
cd frontend && docker compose -f build.docker-compose.yml up -d --build
cd frontend && npx cypress run --browser chrome --headless
```
