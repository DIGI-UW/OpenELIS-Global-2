# Test Data Management Strategy

**Date**: 2025-01-XX  
**Feature**: Sample Storage Management POC  
**Branch**: 001-sample-storage  
**Related**: [plan.md](./plan.md), [data-model.md](./data-model.md),
[.specify/guides/test-data-strategy.md](../../.specify/guides/test-data-strategy.md)

## Executive Summary

This document analyzes the current test data management situation in OpenELIS
Global 2, identifies root causes of test data debugging difficulties, and
proposes a comprehensive layered test data architecture to unify and improve
test data management across all test types (unit, integration, E2E).

**Key Findings**:

- Three separate test data systems exist with no integration
- Storage test data duplicates foundation data already in Liquibase
- No validation layer ensures consistency between test data sources
- CI doesn't explicitly load required test data, causing flaky E2E tests

**Recommended Solution**: Implement a 6-phase layered test data architecture
that consolidates foundation data in Liquibase, converts feature-specific data
to DBUnit XML, and provides unified loading mechanisms.

---

## Current State Analysis

### 1. DBUnit + XML System (Unit/Integration Tests)

**Location**: `src/test/resources/testdata/*.xml`

**How It Works**:

- Tests extend `BaseWebContextSensitiveTest`
- Each test class calls
  `executeDataSetWithStateManagement("testdata/result.xml")`
- DBUnit loads XML datasets using `DatabaseOperation.REFRESH`
- Data is isolated per test class

**Example**:

```java
public class ResultServiceTest extends BaseWebContextSensitiveTest {
    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
    }
}
```

**Files**: 100+ XML files including:

- `result.xml` - Test results data
- `analysis.xml` - Analysis test data
- `patient.xml` - Patient test data
- `nc-event.xml` - Non-conforming events
- And many more...

**Characteristics**:

- ✅ Isolated per test class
- ✅ Fast loading (DBUnit REFRESH operation)
- ✅ Well-established pattern in codebase
- ❌ No shared fixtures across test types
- ❌ XML format can be verbose

### 2. Liquibase System (Schema + Reference Data)

**Location**: `src/main/resources/liquibase/`

**How It Works**:

- `BaseTestConfig` sets `liquibase.setContexts("test")`
- Liquibase changesets with `context="test"` load automatically
- Runs once per test suite (not per test)
- Includes schema migrations + reference data

**Key Changeset**: `004-insert-test-storage-data.xml`

- Loads storage hierarchy (rooms, devices, shelves, racks, positions)
- Only runs when `context="test"` is active
- Foundation data shared across all tests

**Characteristics**:

- ✅ Automatic loading via Spring context
- ✅ Single source of truth for schema
- ✅ Foundation data shared across tests
- ❌ Not suitable for test-specific data (runs once per suite)
- ❌ Changesets are versioned (harder to modify for testing)

### 3. New Storage SQL System (E2E Test Data)

**Location**: `src/test/resources/storage-test-data.sql`

**How It Works**:

- SQL script with complete test fixtures
- Loaded via `load-test-fixtures.sh` script
- Used by Cypress E2E tests via `cy.loadStorageFixtures()`
- Used by `BaseStorageTest` for backend integration tests

**Content**:

- Storage hierarchy (duplicates Liquibase data!)
- E2E patients (John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams)
- E2E samples (E2E001-E2E005)
- E2E sample items, analyses, results
- Storage assignments

**Characteristics**:

- ✅ Comprehensive test data in one file
- ✅ Easy to inspect and modify
- ✅ Works for both E2E and integration tests
- ❌ Duplicates storage hierarchy from Liquibase
- ❌ Not integrated with existing DBUnit system
- ❌ No validation against other test data sources

### 4. CI Integration

**Location**: `.github/workflows/frontend-qa.yml`

**Current State**:

- Runs `docker compose -f build.docker-compose.yml up -d --build`
- Liquibase runs automatically on startup with `context="test"`
- **No explicit step to load `storage-test-data.sql`**
- Cypress tests call `cy.loadStorageFixtures()` but this may fail if script
  isn't available

**Problem**: CI doesn't guarantee test data is loaded, causing flaky E2E tests.

---

## Problem Identification

### Root Cause 1: Data Duplication

**Storage hierarchy exists in TWO places**:

1. Liquibase: `004-insert-test-storage-data.xml` (rooms, devices, shelves,
   racks, positions)
2. SQL: `storage-test-data.sql` (same storage hierarchy + E2E data)

**Impact**:

- Risk of inconsistency (one updated, other not)
- Confusion about which is the "source of truth"
- Maintenance burden (update in two places)

### Root Cause 2: Lack of Integration

**Three separate systems with no bridge**:

- DBUnit XML: Used by unit/integration tests
- Liquibase: Used for foundation data
- SQL Script: Used by E2E tests

**Impact**:

- Storage E2E data (patients, samples) not accessible to DBUnit tests
- No way to share test data across test types
- Tests can't leverage existing test data

### Root Cause 3: Inconsistent Loading

**Different loading mechanisms**:

- DBUnit: Per-test-class via `executeDataSetWithStateManagement()`
- Liquibase: Once per suite via Spring context
- SQL: Manual script execution or Cypress task

**Impact**:

- CI doesn't guarantee all test data is loaded
- E2E tests fail if fixtures aren't loaded
- No validation that required data exists

### Root Cause 4: No Test Data Isolation

**Tests may interfere with each other**:

- DBUnit tests clean their own data but may leave artifacts
- SQL script uses `ON CONFLICT` which may not clean properly
- No clear boundaries between fixture data and test-created data

**Impact**:

- Tests may pass/fail based on execution order
- Hard to debug data-related failures
- No clear cleanup strategy

---

## Recommended Solution: Layered Test Data Architecture

Based on industry best practices for test data management, implement a **layered
architecture** that separates concerns and provides clear boundaries:

```
┌─────────────────────────────────────────────────────────┐
│ Layer 1: Foundation Data (Liquibase)                    │
│ - Schema migrations                                      │
│ - Reference data (statuses, types, roles)               │
│ - Storage hierarchy (rooms, devices, shelves, racks)    │
│ - Loaded automatically via Liquibase context="test"    │
│ - Shared across ALL test types                          │
│ - Single source of truth                                 │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│ Layer 2: Feature-Specific Test Data (DBUnit XML)        │
│ - Storage E2E data (patients, samples, analyses)         │
│ - Result test data (result.xml)                        │
│ - Analysis test data (analysis.xml)                     │
│ - Loaded per-test-class via executeDataSetWithState... │
│ - Isolated per test type                                │
│ - Can be shared across test classes                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│ Layer 3: Test-Specific Data (Test Builders)            │
│ - Dynamic data created in tests                         │
│ - Cleaned up after each test                            │
│ - No persistence between tests                          │
│ - Uses builders/factories for consistency               │
└─────────────────────────────────────────────────────────┘
```

### Benefits of Layered Architecture

1. **Single Source of Truth**: Foundation data in Liquibase only
2. **Integration**: Feature-specific data uses same DBUnit system as other tests
3. **Isolation**: Each test type loads only what it needs
4. **Maintainability**: Clear separation of concerns
5. **CI Reliability**: Explicit test data loading in CI
6. **Backward Compatibility**: Existing XML tests continue to work

---

## Implementation Plan

### Phase 1: Consolidate Foundation Data (Liquibase)

**Goal**: Single source of truth for storage hierarchy

**Actions**:

1. Keep storage hierarchy in Liquibase (`004-insert-test-storage-data.xml`)
2. Remove storage hierarchy from `storage-test-data.sql`
3. Update `storage-test-data.sql` to only include E2E test data (patients,
   samples, analyses, results)
4. Add dependency check in `storage-test-data.sql` to verify storage hierarchy
   exists

**Files to Modify**:

- `src/main/resources/liquibase/3.3.x.x/004-insert-test-storage-data.xml` (keep
  as-is)
- `src/test/resources/storage-test-data.sql` (remove storage hierarchy, keep
  only E2E data)

**Validation**:

- Verify storage hierarchy loads from Liquibase only
- Verify `storage-test-data.sql` doesn't create storage entities
- Verify dependency check works correctly

**Risk**: Low - No test changes required, only data file updates

---

### Phase 2: Convert Storage E2E Data to DBUnit XML

**Goal**: Integrate storage E2E data with existing DBUnit system

**Actions**:

1. Create `src/test/resources/testdata/storage-e2e.xml` with:
   - E2E patients (John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams)
   - E2E samples (E2E001-E2E005)
   - E2E sample items, analyses, results
   - Storage assignments
2. Update `BaseStorageTest` to load `storage-e2e.xml` via
   `executeDataSetWithStateManagement()`
3. Keep `storage-test-data.sql` as a convenience script for manual/Cypress
   loading
4. Update `load-test-fixtures.sh` to:
   - Check if storage hierarchy exists (from Liquibase)
   - Load only E2E data from SQL (or convert to call DBUnit)

**Files to Create**:

- `src/test/resources/testdata/storage-e2e.xml`

**Files to Modify**:

- `src/test/java/org/openelisglobal/storage/BaseStorageTest.java`
- `src/test/resources/storage-test-data.sql` (simplify to E2E data only)
- `src/test/resources/load-test-fixtures.sh` (update to check Liquibase data)

**Validation**:

- Verify `BaseStorageTest` loads E2E data correctly
- Verify existing storage tests still pass
- Verify Cypress tests can still load data via script

**Risk**: Medium - Requires test updates, but maintains backward compatibility

---

### Phase 3: Unified Test Data Loader

**Goal**: Single entry point for all test data loading

**Actions**:

1. Create `TestDataLoader` utility class:
   ```java
   public class TestDataLoader {
       // Load foundation data (Liquibase - already done)
       // Load feature-specific data (DBUnit XML)
       // Load E2E data (DBUnit XML or SQL)
   }
   ```
2. Update `BaseStorageTest` to use `TestDataLoader`
3. Update Cypress `cy.loadStorageFixtures()` to use `TestDataLoader` via Cypress
   task

**Files to Create**:

- `src/test/java/org/openelisglobal/test/util/TestDataLoader.java`

**Files to Modify**:

- `src/test/java/org/openelisglobal/storage/BaseStorageTest.java`
- `frontend/cypress/support/load-storage-fixtures.js` (update Cypress task)

**Validation**:

- Verify `TestDataLoader` loads all required data
- Verify tests can use `TestDataLoader` instead of direct DBUnit calls
- Verify Cypress integration works

**Risk**: Low - Incremental improvement, doesn't break existing functionality

---

### Phase 4: Test Data Isolation and Cleanup

**Goal**: Ensure tests don't interfere with each other

**Actions**:

1. Update `BaseStorageTest.cleanStorageTestData()` to:
   - Preserve Liquibase foundation data (storage hierarchy)
   - Preserve DBUnit fixture data (E2E patients, samples)
   - Clean only test-created data (IDs >= 1000, TEST-\* prefixes)
2. Add test data validation:
   - Verify foundation data exists before tests
   - Verify feature-specific data loaded correctly
   - Fail fast with clear error messages

**Files to Modify**:

- `src/test/java/org/openelisglobal/storage/BaseStorageTest.java`

**Validation**:

- Verify cleanup preserves fixtures
- Verify cleanup removes test-created data
- Verify validation catches missing data early

**Risk**: Low - Improves existing cleanup logic

---

### Phase 5: CI Integration

**Goal**: Ensure CI loads all required test data

**Actions**:

1. Update CI workflow to:
   - Liquibase runs automatically (already done)
   - Explicitly load E2E test data before Cypress tests
   - Add verification step to confirm data loaded
2. Add test data validation script:
   ```bash
   ./scripts/validate-test-data.sh
   ```

**Files to Modify**:

- `.github/workflows/frontend-qa.yml`
- `build.docker-compose.yml` (if needed)

**Files to Create**:

- `scripts/validate-test-data.sh`

**Validation**:

- Verify CI loads all required test data
- Verify validation script catches missing data
- Verify Cypress tests pass consistently in CI

**Risk**: Low - Improves CI reliability

---

### Phase 6: Documentation and Validation

**Goal**: Clear documentation and automated validation

**Actions**:

1. Update test data documentation:
   - `src/test/resources/testdata/README.md` (describe each XML file)
   - `.specify/guides/test-data-strategy.md` (update with new architecture)
2. Add test data validation:
   - Script to verify XML files are valid DBUnit format
   - Script to verify SQL files don't conflict with XML files
   - Script to verify fixture data matches test expectations

**Files to Create**:

- `src/test/resources/testdata/README.md`
- `scripts/validate-test-data-consistency.sh`

**Files to Modify**:

- `.specify/guides/test-data-strategy.md`

**Validation**:

- Verify documentation is accurate and complete
- Verify validation scripts catch inconsistencies
- Verify developers can understand test data structure

**Risk**: Low - Documentation and validation improvements

---

## Migration Strategy

### Risk Assessment

| Phase     | Risk Level | Impact                   | Mitigation                      |
| --------- | ---------- | ------------------------ | ------------------------------- |
| Phase 1   | Low        | No test changes          | Validate before proceeding      |
| Phase 2   | Medium     | Requires test updates    | Maintain backward compatibility |
| Phase 3-6 | Low        | Incremental improvements | Validate each phase             |

### Phased Approach

1. **Phase 1 (Foundation Consolidation)**: Low risk, no test changes
2. **Phase 2 (DBUnit Conversion)**: Medium risk, requires test updates
3. **Phase 3-6 (Enhancement)**: Low risk, incremental improvements

### Validation at Each Phase

- Run all existing tests to ensure no regressions
- Verify test data loads correctly
- Verify cleanup works as expected
- Document any breaking changes

---

## Benefits

### Immediate Benefits

1. **Single Source of Truth**: Storage hierarchy in Liquibase only
2. **Integration**: Storage E2E data uses same DBUnit system as other tests
3. **Isolation**: Each test type loads only what it needs
4. **Maintainability**: Clear separation of concerns

### Long-Term Benefits

1. **CI Reliability**: Explicit test data loading in CI
2. **Backward Compatibility**: Existing XML tests continue to work
3. **Developer Experience**: Clear documentation and validation
4. **Scalability**: Easy to add new test data types

---

## Next Steps

### Recommended Starting Point

1. **Start with Phase 1** (Consolidate Foundation Data):

   - Low risk, no test changes required
   - Immediate benefit (removes duplication)
   - Validates approach before larger changes

2. **Then Phase 2** (Convert to DBUnit XML):

   - Medium risk, but maintains backward compatibility
   - Integrates with existing system
   - Enables future improvements

3. **Phases 3-6** (Enhancement):
   - Can be done incrementally
   - Each phase adds value independently
   - Low risk of breaking existing functionality

### Success Criteria

- All existing tests pass
- No duplication of test data
- CI consistently loads all required test data
- Clear documentation for developers
- Validation catches data inconsistencies early

---

## Related Documentation

- [Test Data Strategy Guide](../../.specify/guides/test-data-strategy.md) -
  Comprehensive guide
- [E2E Fixtures Quick Reference](../../.specify/guides/e2e-fixtures-readme.md) -
  E2E-specific reference
- [Testing Roadmap](../../.specify/guides/testing-roadmap.md) - Comprehensive
  testing guide
- [Plan](./plan.md) - Implementation plan for storage feature
- [Data Model](./data-model.md) - Entity relationship documentation

---

**Last Updated**: 2025-01-XX  
**Status**: Proposed - Awaiting implementation approval
