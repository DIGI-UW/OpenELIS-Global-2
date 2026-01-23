# Test Data XML Files

This directory contains DBUnit XML datasets for test fixtures used in unit and
integration tests.

## Overview

Test data is organized into a **layered architecture**:

1. **Layer 1 (Foundation)**: Liquibase - Schema migrations + baseline reference
   data needed for the application context to start in tests
2. **Layer 2 (Feature-Specific)**: DBUnit XML - E2E test data (patients,
   samples, analyses, results)
3. **Layer 3 (Test-Specific)**: Test builders - Dynamic data created in tests
   (cleaned after each test)

## File Organization

### Storage E2E Test Data

- **`storage-e2e.xml`**: E2E test fixtures for storage management feature
  - E2E patients (John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams) - IDs
    1000-1002
  - E2E samples (E2E001-E2E010, E2E) - IDs 1000-1009
  - E2E sample items - IDs 10001-10093
  - E2E sample_human links - IDs 1000-1009
  - E2E storage assignments - IDs 1000-1013
  - E2E storage movements - IDs 1000-1013
  - E2E analyses - IDs 20001-20010
  - E2E results - ID 30001-30002

### Other Test Data Files

- **`result.xml`**: Test results data
- **`analysis.xml`**: Analysis test data
- **`patient.xml`**: Patient test data
- **`status-of-sample.xml`**: Status of sample test data
- And many more...

## ID Ranges for Fixtures vs Test-Created Data

### Fixture Data (Preserved during cleanup)

- **Storage**: IDs 1-999 (from Liquibase foundation data)
- **Assignments**: IDs 5000-5013 (DBUnit E2E fixtures)
- **Movements**: IDs 5000-5013 (DBUnit E2E fixtures)
- **Samples**: E2E-\* accession numbers (DBUnit fixtures)
- **Patients**: E2E-PAT-\* external IDs (DBUnit fixtures)
- **Sample items**: IDs 10000-20000 (DBUnit fixtures)
- **Analyses**: IDs 20000-30000 (DBUnit fixtures)
- **Results**: IDs 30000-40000 (DBUnit fixtures)

### Test-Created Data (Cleaned up after tests)

- **Storage**: IDs >= 1000, codes/names starting with TEST-
- **Samples**: TEST-\* accession numbers (if created by tests)
- **Sample items**: IDs >= 20000 (test-created, not DBUnit fixtures)

## How to Add New Test Data

### 1. Determine the Layer

- **Foundation data** (schema + long-lived baseline reference data): Add to
  Liquibase changeset
- **Feature-specific data** (E2E fixtures): Add to appropriate DBUnit XML file
- **Test-specific data**: Create in test using builders/factories

### 2. For DBUnit XML Files

1. **Choose the right file**: Use existing file if it matches your feature, or
   create new one
2. **Follow the format**: See `storage-e2e.xml` or `result.xml` for examples
3. **Use appropriate IDs**: Follow ID range conventions (see above)
4. **Handle dependencies**: Some fields reference other tables (e.g.,
   `typeosamp_id`, `status_id`)
   - Use placeholder values that should exist in test database
   - Document placeholder requirements in XML comments

### 3. DBUnit XML Format Reference

```xml
<?xml version='1.0' encoding='UTF-8'?>
<dataset>
    <!-- Entity name matches table name (lowercase, underscores) -->
    <table_name id="1" column1="value1" column2="value2"
        nullable_column="" lastupdated="2025-01-01 12:00:00" />

    <!-- Foreign key references use ID values -->
    <related_table id="1" foreign_key_id="1" other_column="value" />
</dataset>
```

**Key Points**:

- Table names match database table names (lowercase, underscores)
- Column names match database column names exactly
- Empty strings (`""`) represent NULL values
- Timestamps use format: `YYYY-MM-DD HH:MM:SS`
- IDs must be unique within each table

### 4. Loading Test Data in Tests

**For tests extending `BaseStorageTest`**:

- E2E data is automatically loaded via
  `executeDataSetWithStateManagement("testdata/storage-e2e.xml")`
- Liquibase runs for the test profile via `BaseTestConfig`, but DBUnit datasets
  are the authoritative source of truth for table contents used by a test (the
  loader truncates and refreshes tables included in the dataset)

**For other tests**:

```java
@Before
public void setUp() throws Exception {
    super.setUp();
    executeDataSetWithStateManagement("testdata/your-file.xml");
}
```

## Dependencies

### Foundation Data (Liquibase)

Storage E2E test data depends on:

- Schema + baseline reference data (Liquibase)
- Any additional reference rows required by a test should be included in the
  DBUnit dataset that uses them (so the test is self-contained and repeatable)

### Placeholder Values

Some fields in `storage-e2e.xml` use placeholder values that must exist in test
database:

- `typeosamp_id`: References `type_of_sample` (typically IDs 1-3 for Serum,
  Urine, Blood)
- `status_id`: References `status_of_sample` (Entered status, typically ID 1)
- `test_id`: References `test` table (first active test)
- `test_sect_id`: References `test_section` (first active test section, can be
  NULL)
- `test_result_id`: References `test_result` (first dictionary type result, can
  be NULL)

These are resolved at runtime or should exist in the test database.

## Validation

Test data is validated automatically:

- **Foundation data**: Verified by `BaseStorageTest.validateTestData()`
- **E2E fixture data**: Verified by `BaseStorageTest.validateTestData()`
- **Dependencies**: Checked by `load-test-fixtures.sh` before loading

## Cleanup

Test data cleanup is handled automatically:

- **Fixture data**: Preserved during cleanup (IDs in fixture ranges)
- **Test-created data**: Cleaned up after each test (TEST-\* prefixes, IDs
  outside fixture ranges)

See `BaseStorageTest.cleanStorageTestData()` for cleanup logic.

## Migration from SQL to DBUnit XML

Storage E2E test data was migrated from SQL (`storage-test-data.sql`) to DBUnit
XML (`storage-e2e.xml`) as part of the test data strategy remediation:

- **Before**: SQL script loaded both storage hierarchy and E2E data
- **Current repository default for backend tests**: DBUnit XML datasets provide
  the DB-backed fixtures a test needs (including any reference rows required for
  repeatability). Liquibase remains responsible for schema migrations.
- **Benefits**:
  - Single source of truth for foundation data (Liquibase)
  - Integration with existing DBUnit system
  - Better test isolation
  - Consistent loading across test types

## Generated SQL Files (E2E Tests Only)

For E2E tests (Cypress) that don't have access to Java/DBUnit, we generate SQL
on-demand from the authoritative DBUnit XML:

- **Authoritative Source**: `storage-e2e.xml` (DBUnit XML)
- **Generated Output**: `storage-e2e.generated.sql` (never committed to git)
- **Converter**: `xml-to-sql.py` (Python script)
- **How**: `load-test-fixtures.sh` generates SQL on-the-fly before loading

**IMPORTANT**: The `*.generated.sql` files are **NEVER committed** to git (see
`.gitignore`). They are generated on-demand during test runs to avoid
source-of-truth fragmentation.

**Workflow**:

1. Backend tests → Load `storage-e2e.xml` via DBUnit (Java)
2. E2E tests → Load `storage-e2e.xml` via generated SQL (Python + psql)
3. Manual testing → Load `storage-e2e.xml` via generated SQL (Python + psql)

**Why generated SQL?**

- E2E CI (`frontend-qa.yml`) doesn't have Maven/Java dependencies
- Keeps E2E tests fast (no Maven compilation)
- Maintains single source of truth (XML is authoritative)
- Prevents accidental divergence (SQL regenerated every run)

## Related Documentation

- [Test Data Strategy Guide](../../../../.specify/guides/test-data-strategy.md) -
  Comprehensive test data management guide
- [Testing Roadmap](../../../../.specify/guides/testing-roadmap.md) - Testing
  patterns and best practices
- [Test Data Strategy Remediation Plan](../../../../test-data-strategy-remediation.plan.md) -
  Detailed remediation plan
