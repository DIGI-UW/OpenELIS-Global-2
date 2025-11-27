<!-- 7a91ce40-ef53-4416-a10f-a96613c85a2d ec6b0670-171b-4952-be70-680bfa3e5688 -->

# Unified Test Data Strategy

## Problem Analysis

### Current Issues

1. **Dual Fixture Loading Paths:**

- E2E/Cypress: `cy.loadStorageFixtures()` → Cypress task →
  `load-test-fixtures.sh` → `storage-test-data.sql`
- Manual/Backend: Direct execution of `load-test-fixtures.sh` →
  `storage-test-data.sql`
- Backend tests don't use unified fixtures - they create their own data via
  builders or rely on Liquibase fixtures

2. **No Database Reset:**

- SQL script uses `ON CONFLICT` which means corrupted data persists
- No mechanism to reset database state before loading fixtures
- Liquibase test data (`004-insert-test-storage-data.xml`) may conflict with SQL
  fixtures

3. **Missing Verification:**

- No verification that samples/sample_items exist after loading
- No validation that required dependencies exist (`type_of_sample`,
  `status_of_sample`)
- Fixture existence check only checks rooms, not samples/patients

4. **Inconsistent Test Data:**

- Backend tests create their own data (IDs >= 1000) but don't use unified
  fixtures
- E2E tests expect fixtures but may not have them loaded
- Manual testing requires manual fixture loading

## Solution Architecture

### 1. Unified Fixture Loader Script

**File:** `src/test/resources/load-test-fixtures.sh`

**Enhancements:**

- Add `--reset` flag to reset database before loading (optional, defaults to
  false for safety)
- Add `--verify` flag to verify all fixtures loaded correctly (default: true)
- Add dependency checks (verify `type_of_sample`, `status_of_sample` exist)
- Add comprehensive verification queries (samples, sample_items, patients,
  storage hierarchy)
- Support both Docker and direct psql connections (existing functionality)

**Verification Queries:**

- Storage hierarchy: rooms, devices, shelves, racks, positions
- E2E test data: patients (E2E-PAT-_), samples (E2E-_), sample_items,
  assignments, analyses, results
- Dependencies: `type_of_sample`, `status_of_sample` tables exist and have
  required values

### 2. Database Reset Script

**File:** `src/test/resources/reset-test-database.sh`

**Purpose:** Reset database to clean state before loading fixtures

**Functionality:**

- Option 1: Truncate test tables (preserves schema, faster)
- Option 2: Drop and recreate database (cleanest, slower)
- Support both Docker and direct psql connections
- Preserve production data (only reset test data ranges)

**Test Data Ranges (to reset):**

- Storage: IDs 1-999 (fixture range), 1000+ (test-created)
- Samples: E2E- _and TEST-_ accession numbers
- Patients: E2E-PAT-\* external IDs
- Sample items: IDs 10000-20000 (fixture range), 20000+ (test-created)

### 3. Enhanced SQL Fixture Script

**File:** `src/test/resources/storage-test-data.sql`

**Enhancements:**

- Add dependency validation (check `type_of_sample`, `status_of_sample` exist
  before use)
- Add error handling for missing dependencies
- Add verification queries at end of script
- Improve cleanup logic to handle foreign key constraints properly
- Add comments documenting fixture data ranges

**Dependency Handling:**

- Check `type_of_sample` table has at least 3 rows (Serum, Urine, Blood)
- Check `status_of_sample` table has required statuses (Entered, Not Tested,
  Finalized, etc.)
- Fail fast with clear error messages if dependencies missing

### 4. Backend Test Integration

**Pattern:** Create `@BeforeClass` method in base test class that loads fixtures

**File:** `src/test/java/org/openelisglobal/storage/BaseStorageTest.java` (new)

**Functionality:**

- Load fixtures once per test class (not per test method)
- Use `@BeforeClass` to call fixture loader script
- Verify fixtures loaded before tests run
- Clean up test-created data in `@After` (preserve fixtures)

**Migration Strategy:**

- Create base class for storage tests
- Migrate existing tests to extend base class
- Tests continue to create their own data (IDs >= 1000) but also have fixtures
  available

### 5. Cypress Task Enhancement

**File:** `frontend/cypress.config.js`

**Enhancements:**

- Add `resetDatabase` task (calls reset script)
- Enhance `loadStorageTestData` task to support `--reset` flag
- Enhance `checkStorageFixturesExist` to verify samples/patients, not just rooms
- Add `verifyFixtures` task for comprehensive verification

**New Tasks:**

- `resetDatabase()` - Reset database before loading fixtures
- `verifyFixtures()` - Verify all fixtures loaded correctly (samples, patients,
  storage)

### 6. Documentation Updates

**Files to Update:**

- `.specify/guides/testing-roadmap.md` - Add unified test data strategy section
- `src/test/resources/E2E-FIXTURES-README.md` - Update with new reset/verify
  workflow
- `README.md` - Add test data setup instructions

**New Documentation:**

- `src/test/resources/TEST-DATA-STRATEGY.md` - Comprehensive guide to test data
  management

## Implementation Plan

### Phase 1: Core Infrastructure

1. **Create reset script** (`src/test/resources/reset-test-database.sh`)

- Support Docker and direct psql
- Reset test data ranges only
- Add safety checks (confirm before reset)

2. **Enhance fixture loader** (`src/test/resources/load-test-fixtures.sh`)

- Add `--reset` flag (calls reset script)
- Add `--verify` flag (runs verification queries)
- Add dependency checks
- Add comprehensive verification

3. **Enhance SQL fixture script** (`src/test/resources/storage-test-data.sql`)

- Add dependency validation
- Add error handling
- Add verification queries
- Improve cleanup logic

### Phase 2: Backend Integration

4. **Create base test class**
   (`src/test/java/org/openelisglobal/storage/BaseStorageTest.java`)

- Load fixtures in `@BeforeClass`
- Verify fixtures loaded
- Provide cleanup helpers

5. **Migrate existing tests** (gradual migration)

- Start with integration tests
- Update to extend base class
- Verify tests still pass

### Phase 3: Cypress Integration

6. **Enhance Cypress tasks** (`frontend/cypress.config.js`)

- Add `resetDatabase` task
- Enhance `loadStorageTestData` with reset support
- Enhance `checkStorageFixturesExist` verification
- Add `verifyFixtures` task

7. **Update Cypress commands** (`frontend/cypress/support/storage-setup.js`)

- Add reset option to `setupStorageTests`
- Enhance fixture existence check
- Add verification step

### Phase 4: Documentation

8. **Update documentation**

- Update testing-roadmap.md with unified strategy
- Create TEST-DATA-STRATEGY.md guide
- Update E2E-FIXTURES-README.md
- Update README.md with setup instructions

## File Changes Summary

### New Files

- `src/test/resources/reset-test-database.sh` - Database reset script
- `src/test/resources/TEST-DATA-STRATEGY.md` - Test data strategy guide
- `src/test/java/org/openelisglobal/storage/BaseStorageTest.java` - Base test
  class

### Modified Files

- `src/test/resources/load-test-fixtures.sh` - Add reset/verify flags,
  dependency checks
- `src/test/resources/storage-test-data.sql` - Add dependency validation,
  verification queries
- `frontend/cypress.config.js` - Add reset/verify tasks
- `frontend/cypress/support/storage-setup.js` - Enhance fixture management
- `.specify/guides/testing-roadmap.md` - Add unified test data strategy section
- `src/test/resources/E2E-FIXTURES-README.md` - Update workflow
- `README.md` - Add test data setup instructions

## Verification Strategy

### After Loading Fixtures, Verify:

1. **Storage Hierarchy:**

- 3 rooms (MAIN, SEC, INACTIVE)
- 5 devices (MAIN-FRZ01, MAIN-REF01, SEC-CAB01, SEC-FRZ01, INACTIVE-FRZ)
- 6 shelves
- 6 racks
- 99 positions

2. **E2E Test Data:**

- 3 patients (E2E-PAT-001, E2E-PAT-002, E2E-PAT-003)
- 10 samples (E2E-001 through E2E-010)
- 20+ sample_items
- 15+ storage assignments
- 5 analyses
- 2 results

3. **Dependencies:**

- `type_of_sample` table has at least 3 rows
- `status_of_sample` table has required statuses (Entered, Not Tested,
  Finalized, etc.)

## Safety Considerations

- Reset script requires explicit `--reset` flag (no accidental resets)
- Reset only affects test data ranges (preserves production data)
- Verification runs automatically after loading (fail fast if issues)
- Clear error messages for missing dependencies
- Documentation explains test data ranges and cleanup strategies

### To-dos

- [ ] Add console.log statements to UnifiedBarcodeInput handlers (handleChange,
- [ ] Test barcode input in browser DevTools - type, press Enter, check console
- [ ] Fix event handling in UnifiedBarcodeInput - ensure onKeyDown works with
- [ ] Verify useBarcodeDebounce hook is not blocking first call - check cooldown
- [ ] Verify barcodeValidationState updates correctly in LocationManagementModal
- [ ] Run all unit tests for UnifiedBarcodeInput and LocationManagementModal to
- [ ] Run E2E tests in barcodeWorkflow.cy.js to verify end-to-end flow works
