<!-- 25b33d5c-b991-41f0-bba0-1d4bdcf33f42 09151b30-d6a4-4610-a141-a03c84a7a5fb -->

# Frontend E2E Test Failures Remediation Plan

## Problem Analysis

### Primary Issue: Liquibase Test Context Not Active in CI

**Root Cause**: In CI, Liquibase runs with default context instead of
`context="test"`, so the storage hierarchy foundation data (MAIN, SEC, INACTIVE
rooms) is never loaded.

**Evidence**:

- `load-test-fixtures.sh` fails with:
  `ERROR: Storage hierarchy not found. Expected 3 test rooms (MAIN, SEC, INACTIVE) from Liquibase. Found: 0`
- This causes cascading failures in all storage-related E2E tests
- Affects: `storageAssignment.cy.js`, `storageDashboard.cy.js`,
  `storageDisposal.cy.js`, `storageFilters.cy.js`, `storageLocationCRUD.cy.js`,
  `storageMovement.cy.js`, `storageSearch.cy.js`, `storageViewStorage.cy.js`,
  `barcodeWorkflow.cy.js`, `nonConform.cy.js`, `result.cy.js`, `dashboard.cy.js`

### Secondary Issues

1. **Patient Data Mismatches**:

   - Tests expect `'E2E-Smith'` but find `'EE-Smith'` (UI truncation - some
     tests handle this, others don't)
   - Birth date mismatch: Expected `'01/15/1990'` but found `'01/03/1991'`
   - Affects: `patientEntry.cy.js`, `orderEntity.cy.js`, `modifyOrder.cy.js`

2. **UI Element Timeouts**:

   - Various tests timeout waiting for elements that may not render due to
     missing test data
   - Affects: `orderEntity.cy.js`, `dashboard.cy.js`

## Solution Strategy

### Phase 1: Fix Liquibase Test Context in CI (CRITICAL)

**Goal**: Ensure Liquibase runs with `context="test"` in CI environment

**Approach**: Configure Liquibase context via environment variable or
application properties

**Files to Modify**:

- `.github/workflows/frontend-qa.yml`: Add environment variable for Liquibase
  context
- `build.docker-compose.yml`: Pass Liquibase context to application container
- `src/main/java/org/openelisglobal/liquibase/LiquibaseConfig.java`: Verify it
  reads from environment/properties

**Implementation**:

1. Add `SPRING_LIQUIBASE_CONTEXTS=test` environment variable to
   `oe.openelis.org` service in `build.docker-compose.yml`
2. Verify `LiquibaseConfig.java` reads from `spring.liquibase.contexts` property
   (already does via `@Value`)
3. Ensure the property is set in `common.properties` or via environment variable

### Phase 2: Fix Patient Data Mismatches

**Goal**: Ensure test data matches test expectations

**Approach**:

1. Verify patient data in `storage-e2e.xml` matches test expectations
2. Update tests to handle UI truncation consistently (or fix UI to not truncate)
3. Verify birth dates match between test data and test expectations

**Files to Check/Modify**:

- `src/test/resources/testdata/storage-e2e.xml`: Verify patient data
  (last_name="E2E-Smith", birth_date="1990-01-15")
- `frontend/cypress/e2e/patientEntry.cy.js`: Ensure truncation handling is
  consistent
- `frontend/cypress/e2e/orderEntity.cy.js`: Add truncation handling if missing
- `frontend/cypress/e2e/modifyOrder.cy.js`: Add truncation handling if missing

### Phase 3: Verify Test Data Loading

**Goal**: Ensure `load-test-fixtures.sh` works correctly after Liquibase fix

**Approach**:

1. After Phase 1, verify storage hierarchy exists before `load-test-fixtures.sh`
   runs
2. Ensure script can successfully load E2E test data
3. Add better error messages if dependencies are still missing

**Files to Check**:

- `src/test/resources/load-test-fixtures.sh`: Verify dependency checks work
  correctly
- `src/test/resources/storage-test-data.sql`: Ensure it only loads E2E data (not
  storage hierarchy)

## Implementation Details

### Phase 1: Liquibase Context Configuration

**Option A: Environment Variable (Recommended)**

- Add to `build.docker-compose.yml` `oe.openelis.org` service:
  ```yaml
  environment:
  ```
- SPRING_LIQUIBASE_CONTEXTS=test

````

- `LiquibaseConfig.java` already reads from `spring.liquibase.contexts` property
- Spring Boot automatically maps `SPRING_LIQUIBASE_CONTEXTS` to `spring.liquibase.contexts`

**Option B: Application Properties**

- Add to `volume/properties/common.properties`:
```properties
spring.liquibase.contexts=test
````

- This file is already mounted as a secret in `build.docker-compose.yml`

**Recommendation**: Use Option A (environment variable) for CI, as it's more
explicit and doesn't require modifying shared properties file.

### Phase 2: Patient Data Verification

1. **Check `storage-e2e.xml` patient data**:

   - Verify `last_name="E2E-Smith"` (not "EE-Smith")
   - Verify `birth_date="1990-01-15 00:00:00"` (not "1991-01-03")
   - Verify `entered_birth_date="01/15/1990"` matches

2. **Update tests to handle truncation**:

   - Use existing `cy.typeWithRetry()` command from `commands.js` which handles
     truncation
   - Or add explicit truncation handling in tests that don't use the command

3. **Verify test expectations match data**:

   - Check all test files that reference "E2E-Smith" or "01/15/1990"
   - Ensure they match the data in `storage-e2e.xml`

### Phase 3: Test Data Loading Verification

1. **Verify Liquibase runs before Cypress tests**:

   - Check that `oe.openelis.org` container healthcheck passes before Cypress
     runs
   - Liquibase should run automatically on application startup

2. **Add wait/retry logic in `load-test-fixtures.sh`**:

   - If storage hierarchy check fails, wait and retry (Liquibase may still be
     running)
   - Add timeout and clear error messages

3. **Verify script execution order**:

   - Ensure `load-test-fixtures.sh` runs after application is fully started
   - Consider adding explicit wait for Liquibase completion

## Testing Strategy

### Local Reproduction of CI Issues

**Goal**: Enable developers to reproduce CI failures locally without relying on
CI

**Method 1: Reproduce Liquibase Context Issue**

```bash
# Start containers WITHOUT test context (simulates CI failure)
docker compose -f build.docker-compose.yml up -d

# Manually remove test context from environment (if needed)
# Or use a separate docker-compose override file

# Verify storage hierarchy is missing
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT code FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');"
# Should return 0 rows

# Try to load fixtures (should fail)
./src/test/resources/load-test-fixtures.sh
# Should fail with: "ERROR: Storage hierarchy not found"

# Run Cypress test (should fail)
cd frontend && npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
```

**Method 2: Create Test Script for Local CI Simulation**

- Create `scripts/simulate-ci-failure.sh` that:

  1. Starts containers without test context
  2. Verifies storage hierarchy is missing
  3. Attempts to load fixtures
  4. Runs a sample Cypress test
  5. Reports which failures match CI

**Method 3: Document Exact CI Environment**

- Document exact docker-compose configuration used in CI
- Provide instructions to run same configuration locally
- Include environment variable overrides needed

### Local Verification (After Fixes)

1. **Verify Liquibase Context Fix**:

   - Run `docker compose -f build.docker-compose.yml up -d` (with test context)
   - Verify storage rooms exist:
     `docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT code FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');"`
   - Should return 3 rows

2. **Verify Test Data Loading**:

   - Run `load-test-fixtures.sh` and verify it succeeds
   - Verify E2E patients exist:
     `docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT external_id FROM patient WHERE external_id LIKE 'E2E-%';"`

3. **Verify Patient Name Fix**:

   - Run patient search test:
     `npm run cy:run -- --spec "cypress/e2e/patientEntry.cy.js"`
   - Verify no truncation errors

4. **Verify Robust Test Patterns**:

   - Run tests and verify no arbitrary timeouts
   - Check that tests use proper element visibility/readiness checks

### CI Verification

- Push changes and verify CI workflow passes
- Check that storage-related tests pass
- Check that patient search tests pass
- Verify no truncation-related failures

## Risk Assessment

**Low Risk**:

- Adding environment variable to docker-compose (non-breaking)
- Verifying patient data matches expectations

**Medium Risk**:

- Changing Liquibase context may affect other tests if they depend on default
  context data
- Mitigation: Verify all tests pass after change

**High Risk**:

- None identified

## Success Criteria

1. ✅ All storage-related E2E tests pass in CI
2. ✅ `load-test-fixtures.sh` succeeds in CI
3. ✅ Patient search tests pass (no truncation/data mismatch errors)
4. ✅ No regression in other E2E tests

### To-dos

- [ ] Investigate the exact DBUnit exception by adding detailed error logging or
      running with -X flag to see full stack trace
- [ ] Add explicit voided='false' and rejected='false' attributes to all
      sample_item entries in storage-e2e.xml
- [ ] Verify parent sample_items (without sampitem_id) are listed before
      children in storage-e2e.xml
- [ ] Enhance error logging in
      BaseWebContextSensitiveTest.executeDataSetWithStateManagement() to capture
      exact DBUnit exception details
- [ ] Run full test suite locally to reproduce CI error and verify fix
- [ ] Push changes and verify CI build passes
