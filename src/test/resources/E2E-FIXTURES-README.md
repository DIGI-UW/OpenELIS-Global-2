# E2E Test Fixtures - Quick Reference

## Overview

The `storage-test-data.sql` file now contains **complete test fixtures**
including:

- Storage hierarchy (rooms, devices, shelves, racks, positions)
- Test patients (3 patients)
- Test samples (10 samples)
- Test sample items (20+ items)
- Storage assignments (15+ assignments)
- Test analyses (5 orders for E2E sample items)
- Test results (2 results for finalized analyses)

## Quick Load

### Method 1: Shell Script (Recommended)

```bash
./src/test/resources/load-e2e-fixtures.sh
```

### Method 2: Direct SQL

```bash
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < src/test/resources/storage-test-data.sql
```

### Method 3: Cypress (Automatic)

Cypress tests automatically load fixtures via `cy.loadStorageFixtures()` in the
`before()` hook.

## Test Data Available

### Patients (Searchable in UI)

- **John E2E-Smith** (External ID: `E2E-PAT-001`)
- **Jane E2E-Jones** (External ID: `E2E-PAT-002`)
- **Bob E2E-Williams** (External ID: `E2E-PAT-003`)

### Samples (Visible in Storage Dashboard)

- **E2E-001**: Assigned to `MAIN > FRZ01 > Shelf-A > Rack R1 > A1`
  - Has 2 analyses: 1 finalized (with result), 1 not started
- **E2E-002**: Assigned to `MAIN > FRZ01 > Shelf-A > Rack R1 > A2`
  - Has 1 analysis: Technical acceptance
- **E2E-003**: Assigned to `MAIN > FRZ01 > Shelf-A > Rack R1 > A4`
  - Has 1 analysis: Canceled
- **E2E-004**: **Unassigned** (for testing assignment workflow)
- **E2E-005**: Assigned to `MAIN > FRZ01 > Shelf-A > Rack R1 > A5`
  - Has 1 analysis: Finalized (with result)
- **E2E-006** through **E2E-010**: Additional samples for filter testing

## Verification

### Check Database State

```bash
# Count patients
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';"

# Count samples
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%';"

# Count analyses
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT COUNT(*) FROM analysis WHERE id BETWEEN 20000 AND 30000;"

# Count results
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT COUNT(*) FROM result WHERE id BETWEEN 30000 AND 40000;"

# View sample assignments
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT s.accession_number, p.last_name, pos.coordinate FROM sample s JOIN sample_human sh ON s.id = sh.samp_id JOIN patient pt ON sh.patient_id = pt.id JOIN person p ON pt.person_id = p.id LEFT JOIN sample_storage_assignment ssa ON s.id = ssa.sample_id LEFT JOIN storage_position pos ON ssa.storage_position_id = pos.id WHERE s.accession_number LIKE 'E2E-%';"
```

### Check UI

1. **Patient Search**: Search for "Smith", "Jones", or "Williams" - should find
   test patients
2. **Storage Dashboard**: Navigate to `/Storage` - should show 5 samples
3. **Sample Search**: Search for "E2E-001" through "E2E-005" - should find
   samples

## Clean Up

To remove all test fixtures:

```bash
# Via Cypress task (if available)
cy.cleanStorageFixtures()

# Or manually via SQL
docker exec openelisglobal-database psql -U clinlims -d clinlims << 'EOF'
DELETE FROM result WHERE analysis_id IN (SELECT id FROM analysis WHERE sampitem_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%')));
DELETE FROM analysis WHERE sampitem_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%'));
DELETE FROM sample_storage_movement WHERE sample_item_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%'));
DELETE FROM sample_storage_assignment WHERE sample_item_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%'));
DELETE FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%');
DELETE FROM sample_human WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%');
DELETE FROM sample WHERE accession_number LIKE 'E2E-%';
DELETE FROM patient_identity WHERE patient_id IN (SELECT id FROM patient WHERE external_id LIKE 'E2E-%');
DELETE FROM patient WHERE external_id LIKE 'E2E-%';
DELETE FROM person WHERE id IN (SELECT person_id FROM patient WHERE external_id LIKE 'E2E-%' UNION SELECT id FROM person WHERE last_name LIKE 'E2E-%');
DELETE FROM storage_position WHERE id BETWEEN 100 AND 10000;
DELETE FROM storage_rack WHERE id BETWEEN 30 AND 100;
DELETE FROM storage_shelf WHERE id BETWEEN 20 AND 100;
DELETE FROM storage_device WHERE id BETWEEN 10 AND 100;
DELETE FROM storage_room WHERE id BETWEEN 1 AND 100;
EOF
```

## Troubleshooting

### Fixtures Not Loading

1. Check file path: `src/test/resources/storage-test-data.sql` exists
2. Check Docker: `docker ps | grep openelisglobal-database`
3. Check permissions: SQL file is readable
4. Check logs: Look for SQL errors in output

### Samples Not Visible in UI

1. Verify samples exist:
   `SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%';`
2. Verify sample_human links:
   `SELECT COUNT(*) FROM sample_human WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%');`
3. Check storage assignments:
   `SELECT COUNT(*) FROM sample_storage_assignment WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%');`

### Patients Not Found

1. Verify patients exist:
   `SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';`
2. Check person table:
   `SELECT COUNT(*) FROM person WHERE last_name LIKE 'E2E-%';`
3. Verify patient_identity if needed

## Integration with Cypress

The fixtures are automatically loaded and cleaned up by Cypress tests:

### Automatic Lifecycle

**Loading**: Each storage test file loads fixtures in the `before()` hook:

```javascript
before("Login and load fixtures", () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();

  // Load storage test fixtures (includes patients, samples, and storage hierarchy)
  cy.loadStorageFixtures();
});
```

**Cleanup**: Each storage test file cleans up fixtures in the `after()` hook:

```javascript
after("clean up fixtures", () => {
  // Clean up test fixtures after all tests complete
  cy.cleanStorageFixtures();
});
```

### Commands

- `cy.loadStorageFixtures()` - Loads all test fixtures (storage hierarchy +
  patients + samples)
- `cy.cleanStorageFixtures()` - Removes all test fixtures

Both commands are defined in `frontend/cypress/support/load-storage-fixtures.js`
and call tasks in `cypress.config.js`.

### Cleanup Behavior (Configurable)

**By default, fixtures are cleaned up after each test file completes**. This can
be controlled via the `CYPRESS_CLEANUP_FIXTURES` environment variable:

**Default behavior (cleanup enabled):**

```bash
# Run tests with cleanup (default)
npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

**Disable cleanup (keep fixtures for manual testing):**

```bash
# Run tests WITHOUT cleanup - fixtures persist for manual testing
CYPRESS_CLEANUP_FIXTURES=false npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

**Configuration in cypress.config.js:**

```javascript
env: {
  CLEANUP_FIXTURES: process.env.CYPRESS_CLEANUP_FIXTURES !== "false", // Default: true
}
```

**Note:** Cypress automatically strips the `CYPRESS_` prefix when accessing via
`Cypress.env()`, so use `Cypress.env("CLEANUP_FIXTURES")` in tests even though
the environment variable is `CYPRESS_CLEANUP_FIXTURES`.

**When cleanup is disabled:**

- Fixtures remain in database after tests complete
- Useful for manual testing and debugging
- Fixtures persist until manually cleaned or next test run with cleanup enabled
- Each test file still loads fresh fixtures at the start

**When cleanup is enabled (default):**

- Fixtures are removed after each test file completes
- Each test file runs independently with fresh fixtures
- Tests don't interfere with each other
- Database is cleaned between test file runs
