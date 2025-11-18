# Unified Test Data Strategy

**Last Updated**: 2025-01-XX  
**Purpose**: Comprehensive guide to test data management for OpenELIS Global 2

## Overview

This document describes the unified test data strategy for OpenELIS Global 2, covering E2E testing (Cypress), backend integration testing, and manual testing. All test data loading follows consistent patterns and uses the same fixture files.

## Architecture

### Unified Fixture Loading

All test types use the same fixture loading mechanism:

1. **E2E/Cypress**: `cy.loadStorageFixtures()` → Cypress task → `load-test-fixtures.sh` → `storage-test-data.sql`
2. **Backend Integration**: `BaseStorageTest` → `load-test-fixtures.sh` → `storage-test-data.sql`
3. **Manual Testing**: Direct execution of `load-test-fixtures.sh` → `storage-test-data.sql`

### Fixture Data Ranges

**Fixture Data (Preserved during cleanup):**
- Storage: IDs 1-999 (fixtures)
- Samples: E2E-* and TEST-* accession numbers
- Patients: E2E-PAT-* external IDs
- Sample items: IDs 10000-20000 (fixtures)
- Analyses: IDs 20000-30000 (fixtures)
- Results: IDs 30000-40000 (fixtures)

**Test-Created Data (Cleaned up after tests):**
- Storage: IDs >= 1000, codes/names starting with TEST-
- Samples: TEST-* accession numbers (if created by tests)
- Sample items: IDs >= 20000 (test-created)

## Scripts

### `load-test-fixtures.sh`

Unified fixture loader script that supports both Docker and direct psql connections.

**Usage:**
```bash
# Basic usage (loads fixtures, verifies automatically)
./src/test/resources/load-test-fixtures.sh

# Reset database before loading
./src/test/resources/load-test-fixtures.sh --reset

# Load without verification (faster)
./src/test/resources/load-test-fixtures.sh --no-verify

# Reset and load without verification
./src/test/resources/load-test-fixtures.sh --reset --no-verify
```

**Features:**
- Dependency checks (verifies `type_of_sample`, `status_of_sample` exist)
- Comprehensive verification (storage hierarchy, E2E test data)
- Docker and direct psql support
- Clear error messages for missing dependencies

### `reset-test-database.sh`

Resets test data ranges only (preserves production data).

**Usage:**
```bash
# Interactive (prompts for confirmation)
./src/test/resources/reset-test-database.sh

# Non-interactive (use with --force)
./src/test/resources/reset-test-database.sh --force
```

**Safety:**
- Only resets test data ranges (IDs 1-999 for fixtures, 1000+ for test-created)
- Preserves production data
- Requires explicit `--force` flag for non-interactive use

### `storage-test-data.sql`

SQL fixture script that loads all test data.

**Features:**
- Dependency validation (checks required tables exist)
- Error handling for missing dependencies
- Verification queries at end of script
- Comprehensive test data (storage hierarchy, patients, samples, sample items, assignments, analyses, results)

## Backend Integration

### BaseStorageTest

Base test class for storage-related tests that provides unified fixture loading.

**Usage:**
```java
public class MyStorageTest extends BaseStorageTest {
    @Before
    public void setUp() throws Exception {
        super.setUp(); // Loads fixtures automatically
        // Your test setup
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown(); // Cleans up test-created data
        // Your test cleanup
    }
}
```

**Features:**
- Loads fixtures once per test run (static flag)
- Verifies fixtures exist before loading
- Cleans up test-created data (preserves fixtures)
- Provides `cleanStorageTestData()` helper method

**Migration:**
- Existing tests can gradually migrate to extend `BaseStorageTest`
- Tests continue to create their own data (IDs >= 1000) but also have fixtures available

## Cypress E2E Integration

### Commands

**`cy.loadStorageFixtures(options)`**
- Loads test fixtures
- Options: `{ reset: true }` to reset before loading, `{ noVerify: true }` to skip verification

**`cy.checkStorageFixturesExist()`**
- Checks if fixtures exist (rooms, samples, patients)
- Returns boolean

**`cy.verifyStorageFixtures()`**
- Comprehensive verification of all fixtures
- Returns boolean (true if all fixtures present)

**`cy.cleanStorageFixtures()`**
- Cleans up test fixtures

### Environment Variables

**`CYPRESS_SKIP_FIXTURES=true`**
- Skip fixture loading entirely (assumes fixtures exist)
- Fastest option for iteration

**`CYPRESS_FORCE_FIXTURES=true`**
- Force reload fixtures even if they exist
- Use when fixtures may be corrupted

**`CYPRESS_RESET_DATABASE=true`**
- Reset database before loading fixtures
- Use with `FORCE_FIXTURES` for clean state

**`CYPRESS_VERIFY_FIXTURES=true`**
- Verify fixtures even when skipping load
- Useful for debugging fixture issues

**`CYPRESS_CLEANUP_FIXTURES=true`**
- Clean up fixtures after tests
- Default: false (preserves fixtures for next run)

### Examples

```bash
# Fast iteration (skip loading, skip cleanup)
npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"

# Force reload fixtures
CYPRESS_FORCE_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"

# Reset database and reload fixtures
CYPRESS_FORCE_FIXTURES=true CYPRESS_RESET_DATABASE=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"

# Verify fixtures without loading
CYPRESS_SKIP_FIXTURES=true CYPRESS_VERIFY_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"

# Clean up after tests
CYPRESS_CLEANUP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

## Manual Testing

### Loading Fixtures

```bash
# Basic usage
./src/test/resources/load-test-fixtures.sh

# Reset and load
./src/test/resources/load-test-fixtures.sh --reset
```

### Verifying Fixtures

The loader script automatically verifies fixtures after loading. You can also verify manually:

```bash
# Check if fixtures exist
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
  SELECT 
    (SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')) as rooms,
    (SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%') as samples,
    (SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%') as patients;
"
```

### Test Data Available

**Storage Hierarchy:**
- 3 rooms (MAIN, SEC, INACTIVE)
- 5 devices (MAIN-FRZ01, MAIN-REF01, SEC-CAB01, SEC-FRZ01, INACTIVE-FRZ)
- 6 shelves
- 6 racks
- 99+ positions

**E2E Test Data:**
- 3 patients (John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams)
- 10 samples (E2E-001 through E2E-010)
- 20+ sample items
- 15+ storage assignments
- 5 analyses
- 2 results

## Verification Strategy

### After Loading Fixtures

The loader script automatically verifies:

1. **Storage Hierarchy:**
   - 3 rooms (MAIN, SEC, INACTIVE)
   - 5 devices
   - 6 shelves
   - 6 racks
   - 99+ positions

2. **E2E Test Data:**
   - 3 patients (E2E-PAT-001, E2E-PAT-002, E2E-PAT-003)
   - 10 samples (E2E-001 through E2E-010)
   - 20+ sample items
   - 15+ storage assignments
   - 5 analyses
   - 2 results

3. **Dependencies:**
   - `type_of_sample` table has at least 3 rows
   - `status_of_sample` table has required statuses (Entered, Not Tested, Finalized, etc.)

### Manual Verification

```bash
# Check storage hierarchy
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
  SELECT 'Rooms' AS type, COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')
  UNION ALL
  SELECT 'Devices', COUNT(*) FROM storage_device WHERE id BETWEEN 10 AND 20
  UNION ALL
  SELECT 'Shelves', COUNT(*) FROM storage_shelf WHERE id BETWEEN 20 AND 30
  UNION ALL
  SELECT 'Racks', COUNT(*) FROM storage_rack WHERE id BETWEEN 30 AND 40;
"

# Check E2E test data
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
  SELECT 'Patients' AS type, COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%'
  UNION ALL
  SELECT 'Samples', COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'
  UNION ALL
  SELECT 'Sample Items', COUNT(*) FROM sample_item WHERE id BETWEEN 10000 AND 20000;
"
```

## Troubleshooting

### Fixtures Not Loading

1. **Check dependencies:**
   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
     SELECT COUNT(*) FROM type_of_sample;
     SELECT COUNT(*) FROM status_of_sample WHERE name IN ('Entered', 'Not Tested', 'Finalized');
   "
   ```

2. **Check script permissions:**
   ```bash
   chmod +x src/test/resources/load-test-fixtures.sh
   chmod +x src/test/resources/reset-test-database.sh
   ```

3. **Check Docker container:**
   ```bash
   docker ps | grep openelisglobal-database
   ```

### Samples Not Visible in UI

1. **Verify samples exist:**
   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
     SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%';
   "
   ```

2. **Verify sample_human links:**
   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
     SELECT COUNT(*) FROM sample_human WHERE samp_id IN 
     (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%');
   "
   ```

3. **Check storage assignments:**
   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
     SELECT COUNT(*) FROM sample_storage_assignment WHERE sample_id IN 
     (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%');
   "
   ```

### Patients Not Found

1. **Verify patients exist:**
   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
     SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';
   "
   ```

2. **Check person table:**
   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
     SELECT COUNT(*) FROM person WHERE last_name LIKE 'E2E-%';
   "
   ```

## Best Practices

1. **Use unified scripts**: Always use `load-test-fixtures.sh` rather than executing SQL directly
2. **Reset when needed**: Use `--reset` flag when fixtures may be corrupted
3. **Verify after loading**: Verification runs automatically, but can be disabled with `--no-verify`
4. **Preserve fixtures**: Cleanup only removes test-created data, not fixtures
5. **Check dependencies**: Ensure database is properly initialized before loading fixtures

## References

- [Testing Roadmap](testing-roadmap.md) - Comprehensive testing guide
- [E2E Fixtures Quick Reference](e2e-fixtures-readme.md) - E2E-specific fixture documentation
- [Cypress Best Practices](cypress-best-practices.md) - Cypress testing patterns
- [AGENTS.md](../../AGENTS.md) - Project overview and testing strategy section

