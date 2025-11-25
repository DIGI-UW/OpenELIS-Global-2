# Test Data Strategy Analysis Report

**Date**: 2025-11-23  
**Scope**: Comprehensive analysis of test data management across backend integration tests, Cypress E2E tests, and CI/CD pipelines  
**Goal**: Identify inconsistencies, root causes of issues (including ON CONFLICT problems), and recommend improvements aligned with industry best practices

---

## Executive Summary

The OpenELIS Global 2 project uses **three different test data loading approaches** across test types, leading to inconsistencies and maintenance issues. The primary problem is **redundant and incomplete `ON CONFLICT` clauses** in SQL fixtures that cause data corruption when fixtures are reloaded. Additionally, there's a **fundamental mismatch** between backend (DBUnit TRUNCATE) and frontend (SQL DELETE+INSERT) approaches.

**Key Findings:**
- **CRITICAL**: 28+ `ON CONFLICT` clauses in `storage-test-data.sql` are redundant (DELETE happens first) and incomplete (missing field updates)
- **HIGH**: Two different data loading mechanisms (DBUnit TRUNCATE vs SQL DELETE+INSERT) create maintenance burden
- **MEDIUM**: Inconsistent cleanup strategies between test types
- **LOW**: Missing idempotency guarantees in some test data scripts

**Recommendation Priority:**
1. **IMMEDIATE**: Remove all `ON CONFLICT` clauses from test data that's already deleted
2. **SHORT-TERM**: Standardize on DELETE+INSERT approach (simpler, more explicit)
3. **MEDIUM-TERM**: Add idempotency verification tests
4. **LONG-TERM**: Consider test data builders for dynamic test data

---

## Current State Analysis

### 1. Test Data Loading Approaches

#### A. Backend Integration Tests (Java/JUnit 4)

**Mechanism**: DBUnit XML + `DatabaseOperation.REFRESH`

**Location**: `src/test/java/org/openelisglobal/BaseWebContextSensitiveTest.java`

**How it works:**
```java
// Line 99: TRUNCATE all tables in dataset
cleanRowsInCurrentConnection(tableNames);  // TRUNCATE TABLE ... RESTART IDENTITY CASCADE

// Line 101: INSERT data from XML
DatabaseOperation.REFRESH.execute(connection, dataset);
```

**Characteristics:**
- ✅ **Idempotent**: TRUNCATE ensures clean state before INSERT
- ✅ **Atomic**: Single transaction per test class
- ✅ **No ON CONFLICT needed**: TRUNCATE guarantees no conflicts
- ❌ **Complex**: Requires DBUnit XML format, harder to maintain
- ❌ **Limited**: Can't use dynamic SQL (e.g., `gen_random_uuid()`, `CURRENT_TIMESTAMP`)

**Files Used:**
- `src/test/resources/testdata/storage-e2e.xml` (E2E fixtures)
- `src/test/resources/testdata/status-of-sample.xml`
- `src/test/resources/testdata/result.xml`
- `src/test/resources/testdata/analysis.xml`
- 100+ other XML files

**Usage Pattern:**
```java
@Before
public void setUp() throws Exception {
    super.setUp();
    executeDataSetWithStateManagement("testdata/storage-e2e.xml");
    cleanStorageTestData();  // Clean test-created data (preserves fixtures)
}
```

#### B. Cypress E2E Tests (Frontend)

**Mechanism**: SQL Script (`storage-test-data.sql`) via shell script

**Location**: `src/test/resources/storage-test-data.sql` + `load-test-fixtures.sh`

**How it works:**
```sql
-- Step 1: DELETE existing test data (lines 44-99)
DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';
DELETE FROM patient WHERE external_id LIKE 'E2E-%';
-- ... (15+ DELETE statements)

-- Step 2: INSERT test data with ON CONFLICT (lines 117-1249)
INSERT INTO organization (id, name, ...) VALUES (1000, 'CAMES MAN', ...)
ON CONFLICT (id) DO UPDATE SET ...;  -- ❌ REDUNDANT: Already deleted above

INSERT INTO sample (id, accession_number, ...) VALUES (1000, 'E2E001', ...)
ON CONFLICT (id) DO UPDATE SET ...;  -- ❌ REDUNDANT: Already deleted above
```

**Characteristics:**
- ✅ **Flexible**: Can use dynamic SQL (`gen_random_uuid()`, `CURRENT_TIMESTAMP`)
- ✅ **Readable**: Standard SQL, easier to understand
- ❌ **Redundant ON CONFLICT**: DELETE happens first, so ON CONFLICT never triggers
- ❌ **Incomplete ON CONFLICT**: Some clauses don't update all fields (e.g., missing `test_id`)
- ❌ **Maintenance burden**: 28+ ON CONFLICT clauses to maintain

**Files Used:**
- `src/test/resources/storage-test-data.sql` (1,249 lines)
- `src/test/resources/load-test-fixtures.sh` (wrapper script)

**Usage Pattern:**
```javascript
before("load fixtures", () => {
  cy.loadStorageFixtures();  // Calls load-test-fixtures.sh → storage-test-data.sql
});
```

#### C. Manual Testing

**Mechanism**: Same as Cypress (SQL script via shell script)

**Location**: `src/test/resources/load-test-fixtures.sh`

**Usage:**
```bash
./src/test/resources/load-test-fixtures.sh [--reset] [--no-verify]
```

---

### 2. Data Cleanup Strategies

#### Backend Tests

**Pattern**: Selective cleanup (preserve fixtures, delete test-created data)

**Example** (`BaseStorageTest.cleanStorageTestData()`):
```java
// Preserve fixtures (E2E-* samples, IDs 10000-20000)
// Delete test-created data (TEST-* samples, IDs >= 20000)
jdbcTemplate.execute("DELETE FROM sample WHERE accession_number LIKE 'TEST-%'");
```

**Characteristics:**
- ✅ Preserves fixture data between tests
- ✅ Fast (only deletes test-created data)
- ❌ Complex logic (ID ranges, prefix matching)

#### Cypress E2E Tests

**Pattern**: Full cleanup (DELETE all test data, then INSERT fresh)

**Example** (`storage-test-data.sql` lines 44-99):
```sql
DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';
DELETE FROM patient WHERE external_id LIKE 'E2E-%';
-- ... (15+ DELETE statements)
```

**Characteristics:**
- ✅ Simple and explicit
- ✅ Guarantees clean state
- ❌ Slower (deletes everything, even if unchanged)

---

### 3. CI/CD Integration

#### Backend CI (`ci.yml`)

**Test Execution**: `mvn clean install` (runs JUnit tests)

**Test Data Loading**: 
- Liquibase loads foundation data automatically (`context="test"`)
- DBUnit XML loaded per test class via `executeDataSetWithStateManagement()`

**No explicit fixture loading step** - handled by test framework

#### Frontend CI (`frontend-qa.yml`)

**Test Execution**: `npx cypress run --browser chrome --headless`

**Test Data Loading**:
- Cypress `before()` hooks call `cy.loadStorageFixtures()`
- This calls `load-test-fixtures.sh` → `storage-test-data.sql`
- **No explicit reset step in CI** - relies on DELETE statements in SQL

**Potential Issue**: If a previous test run fails mid-execution, leftover data might cause conflicts

---

## Issues Identified

### Issue 1: Redundant ON CONFLICT Clauses (CRITICAL)

**Problem**: `storage-test-data.sql` contains 28+ `ON CONFLICT` clauses for test data that's already deleted at the top of the script.

**Root Cause**: The script deletes all E2E test data (lines 44-99), then inserts it (lines 117-1249). Since DELETE happens first, `ON CONFLICT` clauses never trigger, making them redundant.

**Impact**:
- **Maintenance burden**: 28+ clauses to maintain
- **False sense of idempotency**: Developers might think ON CONFLICT handles conflicts, but DELETE already does
- **Incomplete updates**: Some ON CONFLICT clauses don't update all fields (e.g., analysis 20001, 20004, 20005 missing `test_id` updates)
- **Data corruption**: If DELETE fails or is incomplete, ON CONFLICT might update only some fields, leaving stale data

**Evidence**:
```sql
-- Line 90: DELETE all E2E samples
DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';

-- Line 284: INSERT with ON CONFLICT (redundant - already deleted)
INSERT INTO sample (id, accession_number, ...) VALUES (1000, 'E2E001', ...)
ON CONFLICT (id) DO UPDATE SET ...;  -- ❌ Never triggers
```

**Affected Inserts**:
- All `sample` inserts (10+ occurrences)
- All `sample_item` inserts (20+ occurrences)
- All `analysis` inserts (7+ occurrences) - **3 have incomplete ON CONFLICT** (20001, 20004, 20005)
- All `result` inserts (2+ occurrences)
- All `patient` inserts (3 occurrences)
- All `person` inserts (3 occurrences for test patients)
- All `patient_identity` inserts (6 occurrences)

**Valid ON CONFLICT Clauses** (should be kept):
- `organization` (ID 1000) - NOT deleted (reference data)
- `person` (ID 2000 - Optimus) - NOT deleted (reference data)
- `provider` (ID 2000) - NOT deleted (reference data)
- `organization_organization_type` - NOT deleted (reference data)

### Issue 2: Incomplete ON CONFLICT Updates (HIGH)

**Problem**: Some `ON CONFLICT` clauses don't update all fields, leading to stale data.

**Example** (Analysis 20001 - lines 870-874):
```sql
ON CONFLICT (id) DO UPDATE SET
  test_id = EXCLUDED.test_id,        -- ✅ Updates test_id
  test_sect_id = EXCLUDED.test_sect_id,  -- ✅ Updates test_sect_id
  status_id = EXCLUDED.status_id,    -- ✅ Updates status_id
  lastupdated = CURRENT_TIMESTAMP;   -- ✅ Updates timestamp
  -- ❌ MISSING: sampitem_id, analysis_type, entry_date, started_date, completed_date, is_reportable
```

**Impact**: If an analysis already exists (e.g., from a failed test run), only some fields are updated, leaving stale data in other fields.

**Fixed in this PR**: Analysis 20001, 20004, 20005 now update `test_id` and `test_sect_id` (but still redundant since DELETE happens first).

### Issue 3: Two Different Loading Mechanisms (HIGH)

**Problem**: Backend uses DBUnit (TRUNCATE + INSERT), frontend uses SQL (DELETE + INSERT).

**Impact**:
- **Maintenance burden**: Two formats to maintain (`storage-e2e.xml` vs `storage-test-data.sql`)
- **Inconsistency risk**: Data might differ between formats
- **Developer confusion**: Which format is authoritative?
- **Documentation drift**: Guides reference both approaches

**Current State**:
- **Backend**: `storage-e2e.xml` (DBUnit XML) - 508 lines
- **Frontend**: `storage-test-data.sql` (SQL) - 1,249 lines
- **Overlap**: Both contain same E2E test data (patients, samples, analyses, results)

**Evidence of Duplication**:
- `storage-e2e.xml` line 88-100: Patient 1000 (John TEST-Smith)
- `storage-test-data.sql` line 182-183: Same patient 1000
- Same data, different formats, must be kept in sync manually

### Issue 4: Missing Idempotency Guarantees (MEDIUM)

**Problem**: No verification that test data loading is truly idempotent.

**Impact**:
- If script is run multiple times, data might accumulate or become inconsistent
- No way to verify that fixtures are in expected state after loading

**Current State**:
- `load-test-fixtures.sh` has verification queries, but they only check counts, not data integrity
- No verification that `ON CONFLICT` clauses work correctly (if they were needed)

### Issue 5: Inconsistent Cleanup Between Test Types (MEDIUM)

**Problem**: Backend tests preserve fixtures between tests, Cypress tests delete everything.

**Backend Pattern** (`BaseStorageTest`):
```java
// Preserve fixtures (E2E-*), delete test-created (TEST-*)
cleanStorageTestData();  // Only deletes TEST-* samples
```

**Cypress Pattern** (`storage-test-data.sql`):
```sql
-- Delete ALL test data (E2E-* and TEST-*)
DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';
```

**Impact**:
- Backend tests can reuse fixtures across multiple test methods (faster)
- Cypress tests reload fixtures for each test file (slower but more isolated)
- Inconsistent behavior makes debugging harder

---

## Best Practices Research

### Industry Best Practices for Test Data Management

Based on research and industry standards:

#### 1. **Idempotent Fixtures** ✅ (Partially Implemented)

**Best Practice**: Test data loading should be idempotent - running it multiple times should produce the same result.

**Current State**: 
- ✅ Backend (DBUnit): Idempotent (TRUNCATE ensures clean state)
- ⚠️ Frontend (SQL): Attempts idempotency via DELETE+ON CONFLICT, but ON CONFLICT is redundant

**Recommendation**: Remove ON CONFLICT, rely on DELETE for idempotency.

#### 2. **DELETE vs TRUNCATE** ✅ (Both Valid, But Choose One)

**Best Practice**: 
- **TRUNCATE**: Faster, resets sequences, but requires table-level access
- **DELETE**: More flexible (can use WHERE clauses), slower, doesn't reset sequences

**Current State**:
- Backend: TRUNCATE (via DBUnit)
- Frontend: DELETE (via SQL)

**Recommendation**: Standardize on DELETE for consistency and flexibility.

#### 3. **ON CONFLICT vs DELETE+INSERT** ❌ (Current Approach is Wrong)

**Best Practice**: 
- **If using DELETE first**: ON CONFLICT is redundant and should be removed
- **If using ON CONFLICT for idempotency**: Don't DELETE first (defeats the purpose)

**Current State**: 
- ❌ **WRONG**: DELETE first, then INSERT with ON CONFLICT (redundant)
- ✅ **CORRECT**: DELETE first, then INSERT without ON CONFLICT (simpler, explicit)

**Recommendation**: Remove all ON CONFLICT clauses from test data that's already deleted.

#### 4. **Test Data Builders** ⚠️ (Not Currently Used)

**Best Practice**: Use builders/factories for dynamic test data created in tests.

**Current State**: 
- ✅ Partially implemented (`BaseStorageTest` has cleanup for test-created data)
- ❌ No standardized builder pattern

**Recommendation**: Keep current approach (test-created data cleaned up), but document builder pattern for future use.

#### 5. **Fixture Verification** ⚠️ (Partially Implemented)

**Best Practice**: Verify fixtures after loading to ensure data integrity.

**Current State**:
- ✅ `load-test-fixtures.sh` has verification queries
- ⚠️ Only checks counts, not data integrity (e.g., foreign key relationships)
- ❌ No verification in backend tests (commented out due to transaction issues)

**Recommendation**: Enhance verification to check data integrity, not just counts.

---

## Alignment Assessment

### How Aligned is Our Approach?

| Aspect | Current State | Best Practice | Alignment | Priority |
|--------|--------------|---------------|-----------|----------|
| **Idempotency** | DELETE+ON CONFLICT (redundant) | DELETE+INSERT (explicit) | ⚠️ Partial | CRITICAL |
| **Consistency** | Two formats (DBUnit + SQL) | Single format | ❌ Low | HIGH |
| **Maintainability** | 28+ redundant ON CONFLICT clauses | No redundant code | ❌ Low | CRITICAL |
| **Verification** | Count checks only | Data integrity checks | ⚠️ Partial | MEDIUM |
| **Cleanup Strategy** | Inconsistent (preserve vs delete) | Consistent pattern | ⚠️ Partial | MEDIUM |
| **Documentation** | Comprehensive guides | Well-documented | ✅ Good | LOW |

**Overall Alignment**: **⚠️ 40%** - Significant improvements needed

---

## Recommendations

### Recommendation 1: Remove Redundant ON CONFLICT Clauses (IMMEDIATE - CRITICAL)

**Action**: Remove all `ON CONFLICT` clauses from test data inserts that are already deleted.

**Rationale**:
- DELETE happens first (lines 44-99), so ON CONFLICT never triggers
- Reduces maintenance burden (28+ clauses to maintain)
- Eliminates risk of incomplete updates
- Makes code intent clearer (explicit DELETE, then INSERT)

**Implementation**:
1. Remove ON CONFLICT from:
   - All `sample` inserts
   - All `sample_item` inserts
   - All `analysis` inserts
   - All `result` inserts
   - All `patient` inserts (test patients only)
   - All `person` inserts (test persons only)
   - All `patient_identity` inserts

2. **Keep** ON CONFLICT for reference data that's NOT deleted:
   - `organization` (ID 1000 - CAMES MAN)
   - `person` (ID 2000 - Optimus)
   - `provider` (ID 2000)
   - `organization_organization_type`

**Files to Modify**:
- `src/test/resources/storage-test-data.sql` (remove ~25 ON CONFLICT clauses)

**Estimated Impact**: 
- Reduces file size by ~200 lines
- Eliminates source of data corruption bugs
- Makes script faster (no conflict resolution overhead)

### Recommendation 2: Standardize on SQL DELETE+INSERT (SHORT-TERM - HIGH)

**Action**: Migrate backend tests from DBUnit XML to SQL script approach.

**Rationale**:
- Single format to maintain (SQL is more readable)
- Consistent approach across all test types
- SQL supports dynamic values (`gen_random_uuid()`, `CURRENT_TIMESTAMP`)
- Easier to debug (can run SQL directly)

**Alternative** (if DBUnit is preferred):
- Migrate frontend to DBUnit XML
- **NOT RECOMMENDED**: DBUnit XML is harder to maintain, doesn't support dynamic SQL

**Implementation**:
1. Convert `storage-e2e.xml` to SQL inserts (or generate from SQL)
2. Update `BaseStorageTest` to use SQL script instead of DBUnit
3. Remove `storage-e2e.xml` (or keep as generated artifact)

**Estimated Impact**:
- Reduces maintenance burden (one format instead of two)
- Improves consistency between test types
- **Breaking change**: Requires updating all backend tests

**Risk**: Medium - Requires careful migration to avoid breaking existing tests

### Recommendation 3: Enhance Fixture Verification (MEDIUM-TERM - MEDIUM)

**Action**: Add data integrity checks to verification queries.

**Current Verification** (counts only):
```sql
SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E%';
SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%';
```

**Enhanced Verification** (data integrity):
```sql
-- Verify foreign key relationships
SELECT COUNT(*) FROM sample s
LEFT JOIN sample_item si ON s.id = si.samp_id
WHERE s.accession_number LIKE 'E2E%' AND si.id IS NULL;  -- Should be 0

-- Verify required fields are not NULL
SELECT COUNT(*) FROM analysis WHERE test_id IS NULL;  -- Should be 0

-- Verify data consistency
SELECT COUNT(*) FROM analysis a
LEFT JOIN test t ON a.test_id = t.id
WHERE a.test_id IS NOT NULL AND t.id IS NULL;  -- Should be 0
```

**Implementation**:
1. Add integrity checks to `load-test-fixtures.sh` verification function
2. Add integrity checks to `BaseStorageTest.validateTestData()` (currently commented out)
3. Fail fast if integrity checks fail

**Estimated Impact**:
- Catches data corruption early
- Provides clear error messages when fixtures are invalid
- Helps debug ON CONFLICT issues (if they existed)

### Recommendation 4: Document Test Data Lifecycle (MEDIUM-TERM - LOW)

**Action**: Create clear documentation of test data lifecycle for each test type.

**Current Documentation**:
- ✅ Comprehensive guides exist (`.specify/guides/test-data-strategy.md`)
- ⚠️ Missing: Clear lifecycle diagrams
- ⚠️ Missing: Decision tree for when to use each approach

**Recommended Documentation**:
1. **Lifecycle Diagrams**: Show data flow for each test type
2. **Decision Tree**: When to use fixtures vs test-created data
3. **Troubleshooting Guide**: Common issues and solutions

### Recommendation 5: Add Idempotency Tests (LONG-TERM - LOW)

**Action**: Add automated tests that verify fixture loading is idempotent.

**Implementation**:
```java
@Test
public void testFixtureLoadingIsIdempotent() {
    // Load fixtures twice
    executeDataSetWithStateManagement("testdata/storage-e2e.xml");
    executeDataSetWithStateManagement("testdata/storage-e2e.xml");
    
    // Verify data is identical (not duplicated)
    Integer sampleCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%'", 
        Integer.class);
    assertEquals("Should have same count after second load", 10, sampleCount.intValue());
}
```

**Estimated Impact**:
- Catches regressions in fixture loading
- Ensures ON CONFLICT removal doesn't break idempotency
- Provides confidence in fixture reliability

---

## Implementation Plan

### Phase 1: Fix ON CONFLICT Issues (IMMEDIATE - 1-2 hours)

**Tasks**:
1. ✅ Remove ON CONFLICT from test persons (1000-1002)
2. ✅ Remove ON CONFLICT from test patients (1000-1002)
3. ✅ Remove ON CONFLICT from patient_identity (1000-1005)
4. ⏳ Remove ON CONFLICT from all sample inserts (10+ occurrences)
5. ⏳ Remove ON CONFLICT from all sample_item inserts (20+ occurrences)
6. ⏳ Remove ON CONFLICT from all analysis inserts (7+ occurrences)
7. ⏳ Remove ON CONFLICT from all result inserts (2+ occurrences)
8. ✅ Keep ON CONFLICT for reference data (organization, person 2000, provider 2000)

**Verification**:
- Run `load-test-fixtures.sh` multiple times - should produce identical results
- Run Cypress tests - should pass
- Check database state after multiple loads - should be consistent

### Phase 2: Standardize on SQL Approach (SHORT-TERM - 1-2 days)

**Tasks**:
1. Evaluate: Keep DBUnit for backend or migrate to SQL?
2. If migrate: Convert `storage-e2e.xml` to SQL (or generate from SQL)
3. Update `BaseStorageTest` to use SQL script
4. Update documentation

**Decision Point**: 
- **Option A**: Migrate backend to SQL (recommended - simpler, more maintainable)
- **Option B**: Keep DBUnit, but generate XML from SQL (more complex)

### Phase 3: Enhance Verification (MEDIUM-TERM - 1 day)

**Tasks**:
1. Add data integrity checks to `load-test-fixtures.sh`
2. Re-enable `BaseStorageTest.validateTestData()` (fix transaction issues)
3. Add integrity checks to Cypress verification

### Phase 4: Add Idempotency Tests (LONG-TERM - 1 day)

**Tasks**:
1. Add idempotency test to `BaseStorageTest`
2. Add idempotency verification to `load-test-fixtures.sh`
3. Document idempotency guarantees

---

## Metrics & Success Criteria

### Before Fixes

- **ON CONFLICT Clauses**: 28+ (25 redundant, 3 incomplete)
- **Data Formats**: 2 (DBUnit XML + SQL)
- **Maintenance Burden**: High (must keep two formats in sync)
- **Idempotency Guarantees**: Partial (redundant ON CONFLICT suggests uncertainty)

### After Fixes (Target)

- **ON CONFLICT Clauses**: 4 (only for reference data that's not deleted)
- **Data Formats**: 1 (SQL only, or DBUnit generated from SQL)
- **Maintenance Burden**: Low (single format, no redundant code)
- **Idempotency Guarantees**: Full (explicit DELETE+INSERT pattern)

### Success Criteria

1. ✅ All ON CONFLICT clauses removed from test data that's already deleted
2. ✅ Fixture loading is idempotent (run multiple times = same result)
3. ✅ No data corruption when fixtures are reloaded
4. ✅ Single source of truth for test data (one format, not two)
5. ✅ Clear documentation of test data lifecycle

---

## Conclusion

The current test data strategy has **good intentions** (idempotency, unified loading) but **poor execution** (redundant ON CONFLICT clauses, two formats). The primary issue is **redundant ON CONFLICT clauses** that create maintenance burden and potential for data corruption.

**Immediate Action Required**: Remove all redundant ON CONFLICT clauses from `storage-test-data.sql`. This is a **low-risk, high-reward** change that:
- Eliminates source of bugs (incomplete updates)
- Reduces maintenance burden (28+ clauses → 4 clauses)
- Makes code intent clearer (explicit DELETE, then INSERT)
- Improves performance (no conflict resolution overhead)

**Long-term Vision**: Standardize on SQL DELETE+INSERT approach across all test types for consistency and maintainability.

---

## Appendix: Detailed Findings

### ON CONFLICT Clause Analysis

| Table | Inserts | ON CONFLICT Present | Redundant? | Incomplete? | Action |
|-------|---------|---------------------|------------|------------|--------|
| `organization` | 1 | ✅ Yes | ❌ No (not deleted) | ✅ Complete | **KEEP** |
| `person` (2000) | 1 | ✅ Yes | ❌ No (not deleted) | ✅ Complete | **KEEP** |
| `provider` | 1 | ✅ Yes | ❌ No (not deleted) | ✅ Complete | **KEEP** |
| `person` (1000-1002) | 3 | ✅ Yes | ✅ Yes (deleted) | ✅ Complete | **REMOVE** |
| `patient` | 3 | ✅ Yes | ✅ Yes (deleted) | ✅ Complete | **REMOVE** |
| `patient_identity` | 6 | ✅ Yes | ✅ Yes (deleted) | ✅ Complete | **REMOVE** |
| `sample` | 10+ | ✅ Yes | ✅ Yes (deleted) | ✅ Complete | **REMOVE** |
| `sample_item` | 20+ | ✅ Yes | ✅ Yes (deleted) | ✅ Complete | **REMOVE** |
| `analysis` | 7 | ✅ Yes | ✅ Yes (deleted) | ⚠️ 3 incomplete | **REMOVE** (fixed in PR) |
| `result` | 2+ | ✅ Yes | ✅ Yes (deleted) | ✅ Complete | **REMOVE** |

**Total Redundant Clauses**: 25+  
**Total Valid Clauses**: 4 (reference data only)

### Data Format Comparison

| Aspect | DBUnit XML | SQL Script |
|--------|------------|------------|
| **Readability** | ⚠️ Verbose | ✅ Standard SQL |
| **Dynamic Values** | ❌ No (`gen_random_uuid()`, `CURRENT_TIMESTAMP`) | ✅ Yes |
| **Maintenance** | ⚠️ Complex (XML structure) | ✅ Simple (SQL) |
| **Tooling** | ⚠️ Requires DBUnit library | ✅ Standard psql |
| **Debugging** | ⚠️ Hard to run directly | ✅ Can run in psql |
| **Idempotency** | ✅ TRUNCATE ensures it | ⚠️ Requires DELETE first |

**Recommendation**: SQL Script (more flexible, easier to maintain)

---

**Report Generated**: 2025-11-23  
**Next Review**: After Phase 1 implementation



