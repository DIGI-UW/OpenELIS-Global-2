# Why Tests Don't Catch Issues Locally

## Summary

After multiple CI failures that weren't caught locally, here's why local tests
miss issues that CI catches:

## Root Causes

### 1. **Test Isolation Issues** ⚠️ **MOST COMMON**

**Problem:**

- Tests reuse data from previous runs or fixtures
- Database state persists between test runs
- Tests pass because data already exists or is in a different state

**Example:**

```java
// Test expects code "TEST-DUP01" to be unique
// But if previous test run created it, validation fails unexpectedly
String testCode = "TEST-DUP01"; // ❌ Not unique across runs
```

**Why CI Catches It:**

- CI uses **fresh Testcontainers** database every run
- No persistent state between runs
- Tests fail if they don't properly isolate data

**Fix:**

```java
// Use timestamp-based unique codes
long timestamp = System.currentTimeMillis() % 100;
String testCode = "DUP" + String.format("%02d", timestamp); // ✅ Unique
```

### 2. **Incomplete Test Execution**

**Problem:**

- Developers run **individual test classes** or **specific tests**
- Don't run **full test suite** before pushing
- Missing tests that would catch the issue

**Example:**

```bash
# Developer runs:
mvn test -Dtest="StorageLocationServiceTest"  # ✅ Passes

# But doesn't run:
mvn test -Dtest="StorageLocationRestControllerTest"  # ❌ Would fail
```

**Why CI Catches It:**

- CI runs **ALL tests** (`mvn test`)
- Catches failures in tests developer didn't run
- Full integration test suite exposes issues

**Fix:**

- Use `./scripts/run-ci-checks.sh` before pushing
- Run full test suite locally: `mvn test`
- Use Testcontainers to simulate CI environment

### 3. **Database State Differences**

**Problem:**

- Local database has **fixture data** or **old test data**
- Tests pass because fixtures provide expected state
- CI database is **empty** or has **different fixtures**

**Example:**

```java
// Test expects room with code "MAIN-ROOM" to exist
// Local: Fixtures loaded, room exists ✅
// CI: Fresh database, room doesn't exist ❌
```

**Why CI Catches It:**

- CI uses **fresh database** with only Liquibase migrations
- No fixture data unless explicitly loaded
- Tests fail if they assume fixture data exists

**Fix:**

- Load fixtures explicitly in test setup: `load-test-fixtures.sh`
- Don't assume fixture data exists
- Use `@Before` to create required test data

### 4. **Code Format Mismatches**

**Problem:**

- Tests use **hardcoded codes** that don't match actual format
- Local tests pass because database has matching data
- CI fails because codes don't match expected format

**Example:**

```java
// Test uses full timestamp: "TESTROOM1734480000000" (16 chars)
// But code column is VARCHAR(10), so validation fails
String barcode = String.format("TESTROOM%d-TESTDEV%d", timestamp, timestamp);
// ❌ Code too long, but test passes locally if data exists
```

**Why CI Catches It:**

- CI validates **all constraints** (length, format, uniqueness)
- Fresh database enforces constraints strictly
- Tests fail if codes don't match schema

**Fix:**

```java
// Use modulo to keep codes ≤10 chars
String roomCode = "TESTROOM" + (timestamp % 100); // ✅ 9 chars max
String deviceCode = "TESTDEV" + (timestamp % 100); // ✅ 8 chars max
```

### 5. **Mocked DAOs Hide HQL Errors**

**Problem:**

- Unit tests mock DAOs (`@Mock StorageDeviceDAO`)
- HQL queries **never compile or execute**
- Invalid HQL like `d.shortCode` never fails

**Example:**

```java
@Mock
private StorageDeviceDAO storageDeviceDAO;

@Test
public void testFindByCode() {
    when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(null);
    // ↑ Mock never executes HQL, so invalid property never caught
}
```

**Why CI Catches It:**

- Integration tests use **real DAOs** with **real HQL**
- HQL queries compile and execute against database
- Errors surface when queries reference non-existent properties

**Fix:**

- Use `BaseWebContextSensitiveTest` for DAO tests (not mocks)
- Execute real HQL queries in tests
- Validate HQL compilation in DAO tests

## Solutions

### Immediate Fixes

1. **Use Unique Test Data**

   ```java
   // ❌ BAD: Hardcoded, not unique
   String code = "TEST-CODE";

   // ✅ GOOD: Timestamp-based, unique
   long timestamp = System.currentTimeMillis() % 100;
   String code = "TEST" + String.format("%02d", timestamp);
   ```

2. **Run Full Test Suite Before Pushing**

   ```bash
   # Use CI check script
   ./scripts/run-ci-checks.sh

   # Or run full test suite
   mvn test
   ```

3. **Use Testcontainers Locally**

   ```bash
   # Simulate CI environment
   docker compose -f dev.docker-compose.yml down -v
   docker compose -f dev.docker-compose.yml up -d
   mvn test
   ```

4. **Load Fixtures Explicitly**
   ```bash
   # Don't assume fixtures exist
   ./src/test/resources/load-test-fixtures.sh
   ```

### Long-Term Solutions

1. **Pre-commit Hooks**

   - Run full test suite before commit
   - Validate code formatting
   - Check for hardcoded test data

2. **CI Simulation Scripts**

   - `./scripts/run-ci-checks.sh` - Run backend CI checks
   - `./scripts/run-frontend-ci-checks.sh` - Run frontend CI checks
   - Use Testcontainers to match CI environment

3. **Test Data Factories**

   - Centralized test data generation
   - Ensures unique codes, proper formats
   - Reduces hardcoded test data

4. **DAO Test Coverage**
   - Require `BaseWebContextSensitiveTest` for all DAO methods
   - Execute real HQL queries in tests
   - Validate HQL compilation

## Key Takeaways

1. **Test Isolation**: Always use unique test data (timestamp-based)
2. **Full Test Suite**: Run all tests before pushing, not just individual
   classes
3. **CI Simulation**: Use `./scripts/run-ci-checks.sh` to simulate CI locally
4. **Database State**: Don't assume fixture data exists, load it explicitly
5. **Real DAOs**: Use `BaseWebContextSensitiveTest` for DAO tests, not mocks

## Prevention Checklist

Before pushing, verify:

- [ ] Ran full test suite: `mvn test`
- [ ] Used unique test data (timestamp-based codes)
- [ ] Ran CI check script: `./scripts/run-ci-checks.sh`
- [ ] Database is clean or fixtures loaded explicitly
- [ ] DAO tests use real HQL queries (not mocks)
- [ ] Code formatting applied: `mvn spotless:apply`
