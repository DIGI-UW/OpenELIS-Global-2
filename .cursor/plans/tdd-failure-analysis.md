# TDD Strategy Failure Analysis: shortCode Cleanup

## Executive Summary

The TDD strategy failed to catch 189+ `shortCode` references because of **test
isolation**, **mocking strategies**, and **coverage gaps**. The failures fall
into three categories:

1. **HQL Query Compilation Errors** (most critical)
2. **Unused Code Paths** (not tested)
3. **Test-Implementation Mismatch** (tests updated, code not)

---

## Root Causes

### 1. Mocked DAOs Hide HQL Compilation Errors ⚠️ **CRITICAL**

**Problem:**

- Unit tests mock DAOs (`@Mock StorageDeviceDAO`)
- HQL queries never compile or execute
- Invalid HQL like `FROM StorageDevice d WHERE d.shortCode = :shortCode` never
  fails

**Evidence:**

```java
// ShortCodeValidationServiceTest.java
@Mock
private StorageDeviceDAO storageDeviceDAO;

@Test
public void testUniquenessWithinContext() {
    when(storageDeviceDAO.findByShortCode("FRZ01")).thenReturn(null);
    // ↑ Mock never executes HQL, so invalid property never caught
}
```

**Impact:**

- `findByShortCode()` methods with invalid HQL (`d.shortCode`) passed all unit
  tests
- Errors only surfaced in CI when integration tests executed real HQL queries
- **24 test failures** in CI vs **0 failures** locally

**Why This Happened:**

- TDD focused on **business logic** (validation, uniqueness checks)
- **Infrastructure** (HQL queries) was assumed correct
- No test validated HQL query compilation

**Roadmap Gap:** The testing roadmap (`.specify/guides/testing-roadmap.md`)
**explicitly requires** `@DataJpaTest` for DAO testing:

> **@DataJpaTest (DAO/Repository Layer)**
>
> - Use for: Testing persistence layer in isolation
> - Test HQL queries, CRUD operations, relationships
> - Use `TestEntityManager` for test data setup

**But in practice:**

- ❌ No `@DataJpaTest` tests existed for `findByShortCode()` methods
- ❌ Only mocked unit tests (`@Mock StorageDeviceDAO`) existed
- ❌ Roadmap guidance was **not followed** for DAO methods
- ❌ HQL queries were never executed, so compilation errors never caught

**Root Cause:** The roadmap says "Test HQL queries" but there was **no
enforcement mechanism**:

- No pre-commit hook to check for `@DataJpaTest` tests
- No CI check to validate DAO methods have integration tests
- No requirement that all public DAO methods must have `@DataJpaTest` coverage
- Developers followed TDD for services but **skipped DAO layer testing**

---

### 2. Test Coverage Gaps: Unused Code Paths

**Problem:**

- `findByShortCode()` methods existed but were **never called** in tests
- Tests mocked DAOs, so methods were never exercised
- Integration tests didn't cover all code paths

**Evidence:**

```java
// StorageDeviceDAOImpl.java - Method exists but never tested
public StorageDevice findByShortCode(String shortCode) {
    String hql = "FROM StorageDevice d WHERE d.shortCode = :shortCode";
    // ↑ Invalid HQL, but never executed in tests
}
```

**Impact:**

- Dead code paths with invalid HQL passed silently
- Only CI's full test suite caught these errors

**Why This Happened:**

- TDD focused on **happy paths** (code that's actively used)
- **Legacy/unused methods** weren't tested
- No "test all public methods" requirement

**Roadmap Gap:** The testing roadmap requires `@DataJpaTest` for DAO methods but
doesn't enforce:

- ❌ Coverage of all public DAO methods
- ❌ Testing of unused/legacy methods
- ❌ Validation that HQL queries compile correctly
- ❌ Requirement that each DAO method has a corresponding `@DataJpaTest`

---

### 3. Test-Implementation Mismatch

**Problem:**

- Tests were updated to use `code` field
- Implementation still referenced `shortCode`
- Tests passed because they mocked the behavior, not the implementation

**Evidence:**

```java
// LabelManagementRestControllerTest.java
@Test
public void testPutShortCodeEndpoint_ValidCode_Returns200() {
    ShortCodeUpdateForm form = new ShortCodeUpdateForm();
    form.setCode("FRZ01"); // ← Test uses 'code'
    // But controller still references ShortCodeValidationService
    // which uses findByShortCode() with invalid HQL
}
```

**Impact:**

- Tests passed locally (mocked behavior)
- CI failed (real HQL execution)

**Why This Happened:**

- Refactoring was **incremental** (tests first, then implementation)
- Tests were updated before implementation cleanup
- No "test must fail before fix" checkpoint

---

### 4. Integration Test Gaps

**Problem:**

- Integration tests (`@SpringBootTest`) don't cover all code paths
- Some methods only execute in specific scenarios
- CI runs full suite, catching edge cases

**Evidence:**

```java
// LabelManagementRestController.printLabel()
// Only executes when:
// 1. Location exists
// 2. Code validation passes
// 3. PDF generation succeeds
// If any step fails, findByShortCode() never called
```

**Impact:**

- Local test runs missed error paths
- CI's comprehensive suite caught failures

**Why This Happened:**

- Integration tests focused on **happy paths**
- **Error paths** and **edge cases** not fully covered
- No requirement to test all code paths

---

### 5. No HQL Query Validation Test

**Problem:**

- No test validates HQL queries compile correctly
- HQL errors only surface at runtime
- Unit tests never execute HQL

**Missing Test Pattern:**

```java
// Should exist but doesn't:
@Test
public void testAllDAOQueriesCompileSuccessfully() {
    // Attempt to compile each HQL query
    deviceDAO.findByCode("TEST"); // Would catch invalid property
    deviceDAO.findByShortCode("TEST"); // Would fail here
}
```

**Impact:**

- HQL compilation errors hidden until CI
- No early detection mechanism

**Why This Happened:**

- TDD focused on **business logic**, not **infrastructure**
- HQL validation assumed to be handled by Hibernate
- No "validate all queries" requirement

**Roadmap Gap:** The testing roadmap says:

> **@DataJpaTest**: Test HQL queries, CRUD operations, relationships

But it doesn't specify:

- ❌ **How** to validate HQL compilation (just execution)
- ❌ Requirement for a **compilation validation test** (not just execution)
- ❌ Pattern for testing **all** DAO methods (not just used ones)
- ❌ Early detection mechanism for HQL errors (before runtime)

---

## Test Execution Strategy Issues

### Local vs CI Differences

| Aspect             | Local                          | CI                        |
| ------------------ | ------------------------------ | ------------------------- |
| **Test Execution** | Individual test files          | Full suite                |
| **Database State** | Persistent (may have old data) | Fresh Testcontainers      |
| **HQL Execution**  | Mocked (never executes)        | Real (executes and fails) |
| **Coverage**       | Partial (selected tests)       | Complete (all tests)      |

**Impact:**

- Local: Tests pass (mocked, partial coverage)
- CI: Tests fail (real HQL, full coverage)

---

## Specific Failure Points

### 1. DAO Methods (24 failures)

- `StorageDeviceDAO.findByShortCode()` - Invalid HQL `d.shortCode`
- `StorageShelfDAO.findByShortCode()` - Invalid HQL `s.shortCode`
- `StorageRackDAO.findByShortCode()` - Invalid HQL `r.shortCode`

**Why Not Caught:**

- Methods mocked in unit tests
- Never called in integration tests
- No HQL validation test

### 2. Service Layer (21 errors)

- `ShortCodeValidationService` - Entire service uses `shortCode`
- `LabelManagementService.validateShortCodeExists()` - References removed
  methods

**Why Not Caught:**

- Service tests mock DAOs
- Never execute real HQL queries
- Tests updated but implementation not

### 3. Controller Layer

- `LabelManagementRestController.updateShortCode()` - Uses removed service
- `LabelManagementRestController.printLabel()` - References removed methods

**Why Not Caught:**

- Controller tests mock services
- Never execute real code paths
- Tests pass with mocked behavior

---

## Recommendations

### 1. Add HQL Query Validation Test ✅ **CRITICAL**

**Create:** `StorageDAOQueryValidationTest.java`

```java
@SpringBootTest
public class StorageDAOQueryValidationTest {
    @Autowired private StorageDeviceDAO deviceDAO;

    @Test
    public void testAllDAOQueriesCompileSuccessfully() {
        // Exercise all DAO methods to catch HQL errors
        deviceDAO.findByCode("TEST");
        // Would fail if findByShortCode() exists with invalid HQL
    }
}
```

**Benefit:**

- Catches HQL compilation errors early
- Validates all DAO methods compile
- Runs in <5 seconds (no database needed for compilation check)

**Update Testing Roadmap:** Add explicit requirement:

> **MANDATORY**: All DAO methods MUST have `@DataJpaTest` tests that execute the
> HQL query. Additionally, create a compilation validation test that exercises
> all DAO methods to catch HQL property reference errors before runtime.

### 2. Require Integration Test Coverage ✅ **ALIGN WITH ROADMAP**

**Rule:** All public DAO methods must have `@DataJpaTest` tests that execute
real HQL queries.

**Roadmap Alignment:** The testing roadmap already specifies this:

> **@DataJpaTest**: Test HQL queries, CRUD operations, relationships

**Enforcement Needed:**

- Add pre-commit hook to check for `@DataJpaTest` coverage
- Add CI check to validate all DAO methods have tests
- Require `@DataJpaTest` test for each public DAO method

**Benefit:**

- Catches HQL errors in local test runs
- Validates query correctness
- Ensures methods are actually used
- **Follows existing roadmap guidance** (just needs enforcement)

### 3. Test Execution Strategy

**Rule:** Before pushing, run:

1. Unit tests (mocked)
2. Integration tests (real HQL)
3. HQL validation test (compilation check)
4. Full CI suite locally (`./scripts/run-ci-checks.sh`)

**Benefit:**

- Catches errors before CI
- Validates all code paths
- Reduces CI failures

### 4. Refactoring Checkpoint

**Rule:** When refactoring (e.g., removing `shortCode`):

1. **Phase 1:** Update tests to use new API
2. **Phase 2:** Verify tests FAIL (old implementation)
3. **Phase 3:** Update implementation
4. **Phase 4:** Verify tests PASS
5. **Phase 5:** Remove old code

**Benefit:**

- Ensures tests catch implementation gaps
- Validates refactoring completeness
- Prevents test-implementation mismatch

### 5. Code Coverage Requirements

**Rule:** All public methods must have:

- Unit test (mocked dependencies)
- Integration test (real dependencies)
- HQL validation (if DAO method)

**Benefit:**

- Ensures all code paths tested
- Catches infrastructure errors
- Validates query correctness

---

## Lessons Learned

### What Worked ✅

- TDD caught business logic errors
- Unit tests validated service behavior
- Integration tests caught some infrastructure issues

### What Failed ❌

- Mocked DAOs hid HQL compilation errors
- Unused code paths not tested
- Test-implementation mismatch not caught
- No HQL query validation

### Key Insight 💡

**TDD is excellent for business logic, but infrastructure (HQL, SQL, etc.)
requires explicit validation tests that execute real queries.**

---

## Action Items

1. ✅ **Create HQL Query Validation Test** (Phase 13 in plan)
2. ✅ **Add to CI check script** (already done)
3. ⏳ **Require integration tests for all DAO methods**
4. ⏳ **Add refactoring checkpoint process**
5. ⏳ **Update TDD guidelines** to include infrastructure validation

---

## Conclusion

The TDD strategy failed because:

1. **Mocking hid HQL errors** (most critical)
2. **Coverage gaps** in unused code paths
3. **Test-implementation mismatch** during refactoring
4. **No infrastructure validation** (HQL queries)
5. **Roadmap guidance not followed** (DAO testing requirements existed but
   weren't enforced)

**Root Cause:** The testing roadmap **already specified** `@DataJpaTest` for DAO
testing, but:

- ❌ No enforcement mechanism existed
- ❌ Developers skipped DAO layer testing
- ❌ No validation that roadmap guidance was followed
- ❌ HQL compilation errors hidden by mocking

**Solution:**

1. **Enforce existing roadmap guidance**: Require `@DataJpaTest` for all DAO
   methods
2. **Add HQL compilation validation**: Create test that exercises all DAO
   methods
3. **Add pre-commit checks**: Validate DAO test coverage before commit
4. **Update roadmap**: Add explicit requirement for HQL compilation validation

This bridges the gap between **what the roadmap says** (test DAOs with
`@DataJpaTest`) and **what actually happened** (mocked unit tests only).
