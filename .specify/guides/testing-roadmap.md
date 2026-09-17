# OpenELIS Testing Roadmap

**Audience**: AI Agents and Human Developers  
**Purpose**: Comprehensive guide to testing practices, patterns, and workflows
for OpenELIS Global 2  
**Last Updated**: 2025-01-XX  
**Constitution Reference**: Principle V (Test-Driven Development)

---

## Table of Contents

1. [Overview](#overview)
2. [Test-Driven Development (TDD) Workflow](#test-driven-development-tdd-workflow)
3. [Test Pyramid and Coverage Goals](#test-pyramid-and-coverage-goals)
4. [Backend Testing](#backend-testing)
5. [Frontend Testing](#frontend-testing)
   - [Vitest + React Testing Library](#vitest--react-testing-library-unit-and-component-tests)
   - [Cypress E2E Testing](#cypress-e2e-testing)
   - [Playwright E2E Testing](#playwright-e2e-testing)
6. [Test Data Management](#test-data-management)
7. [SDD Integration](#sdd-integration)
8. [Quick Reference](#quick-reference)

---

## Overview

This roadmap provides detailed guidance on testing practices for OpenELIS
Global 2. All testing MUST follow the patterns and procedures documented here.
The constitution (Principle V) mandates adherence to this roadmap.

**Repository Note (OpenELIS Global 2)**: This codebase uses **traditional Spring
Framework (Spring MVC)**. Use `BaseWebContextSensitiveTest` for Spring-context
controller/DAO/integration tests.

### Key Principles

- **TDD First**: Write tests before implementation for complex logic
- **Test Pyramid**: 75% unit, 15% integration, 5% ORM validation, 5% E2E
- **Coverage Goals**: >80% backend (JaCoCo), >70% frontend (Vitest)
- **Clean State**: Tests must be isolated and use builders/factories for data
- **Checkpoint Validation**: Tests must pass at each SDD phase checkpoint

### CI Enforcement And Migration Guardrails

- **Playwright-first E2E direction**: New end-to-end coverage should be authored
  in Playwright unless there is a clear blocker.
- **Cypress is legacy/deprecating**: Existing Cypress coverage is maintained for
  risk control, but avoid expanding long-term Cypress scope.
- **Stable required gates only**: Protect `develop` using stable gate checks,
  not shard-level checks.
- **Ruleset-managed CI checks**: CI required status checks are managed by
  repository rulesets; classic branch protection should retain non-CI controls
  (reviews, conversation resolution, etc.).
- **Rename safety rule**: Any workflow/job rename that changes required check
  names must be paired with explicit ruleset/branch-protection update steps in
  the same change window.

### Test Type Contracts (What each test MUST prove)

This project uses “E2E” to mean **real browser + real backend + real database**.
If the backend is stubbed, the test is **not** end-to-end (it is a
mocked-backend UI test).

- **Unit tests (mocked collaborators allowed)**:
  - Prove **business logic** (branching, validation, transformations).
  - Should fail if you delete/short-circuit the core logic.
  - **Anti-pattern**: “returns what the mock returned” without verifying any
    logic.

- **Controller HTTP tests (service mocked)**:
  - Prove **request/response mapping**: routing, validation, status codes, JSON
    shape.
  - Do **not** prove persistence (service is mocked by design).

- **Backend integration tests (real service + DAO + DB)**:
  - Prove **persistence and transactions**: read-after-write, constraints,
    rollback behavior.
  - Must include at least one “real-effect assertion” (database state changed as
    expected).

- **Cypress E2E tests (`frontend/cypress/e2e/`)**:
  - Prove the full chain: **UI action → real HTTP → backend logic → DB update →
    UI reflects result**.
  - Must include at least one “real-effect assertion” after mutations (see
    Cypress section).

### Mocking and Stubbing Rules (project-wide)

- **Default rule**: do not stub the part you are trying to validate.
- **Acceptable mocking**:
  - Unit tests: mock external collaborators (DAOs, FHIR services, etc.) to
    isolate business logic.
  - Controller tests: mock the service layer to isolate HTTP mapping/validation.
- **Not acceptable**:
  - Calling a test “integration” or “E2E” while stubbing the system under test.
  - Stubbing success responses for the mutation you are validating in a Cypress
    E2E test.

## Test Quality Invariants (Constitution V.6)

Every test MUST satisfy the **Inversion Test**: if the function under test is
replaced with a hardcoded return value, the test MUST fail. Tests that pass
regardless of implementation are scaffolding, not tests.

### Backend (Java/JUnit/Mockito)

- **J1. No assert-on-mock-return.** `when(x).thenReturn(Y)` then
  `assertEquals(Y, result)` with no intervening logic tests Mockito, not code.
  Assertions must verify a transformation of the mock's output.
- **J2. Verify mock args, not just calls.** `verify(service).save(any())` tests
  nothing. Use `argThat(h -> h.getName().equals("X"))`.
- **J3. Auth before business logic.** Every controller test suite must include a
  test verifying unauthenticated requests receive 401 BEFORE any service method
  is called. Use `verifyZeroInteractions(service)`.
- **J4. Filter pass-through.** If a service method accepts filter parameters, at
  least one test must verify the filter changes the result set. Passing null for
  all filters is not sufficient.
- **J5. Negative tests required.** Every service/controller test class must
  include at least: 1 null/empty input test, 1 invalid input test, 1 boundary
  test.
- **J6. No catch-and-continue in @Transactional.** Catching exceptions inside a
  transactional method corrupts the JPA session. Validate first, then persist.
- **J7. HQL/SQL tests.** If a service builds a query with named params, at least
  one test must verify the query produces different results for different
  parameter values.

### Frontend (Vitest/React Testing Library)

- **F1. No render-only tests.** Every test must simulate a user interaction or
  verify data flow (API URL/headers/params, callback updates state).
- **F2. Verify API request shape.** Assert the URL contains
  `config.serverBaseUrl`, query parameters, and CSRF headers.
- **F3. No raw fetch() in components.** Use project utilities
  (`getFromOpenElisServer`, `postToOpenElisServer*`, `putToOpenElisServer`,
  `deleteFromOpenElisServer`). If a utility doesn't exist, add one to Utils.js.
- **F4. waitFor, not wait.** Import `waitFor` from `@testing-library/dom`. The
  `wait` export is deprecated.
- **F5. i18n assertions.** Tests for components with user-visible text must
  verify `intl.formatMessage` renders correctly.

### E2E (Playwright)

- **E1. Every test.step must have an assertion.** Steps without `expect()`
  provide zero regression protection.
- **E2. No `isVisible({timeout})`.** Use `expect(el).toBeVisible({timeout})`.
- **E3. No `.catch(() => false)` on locator methods.** Handle conditional
  elements with explicit guards.
- **E4. API-first data setup.** Test data via API in beforeAll/beforeEach, not
  UI interactions.

### Universal

- **U1. Inversion Test.** Replace the function under test with
  `return hardcodedValue`. If the test still passes, it's broken.
- **U2. One bug, one test.** Every bug found in review MUST get a regression
  test.
- **U3. No `any()` without justification.** Mockito `any()` in verify/when calls
  must have a comment explaining why.

### LLM-Generated Anti-Patterns

LLMs consistently produce these test anti-patterns. All are prohibited:

1. **Mock-everything** — stub query results, assert the stub returned what you
   told it to
2. **`any()` matcher everywhere** — `verify(mock).save(any())` tests nothing
3. **Assert-on-mock-return** — `when(x).thenReturn(Y)` then
   `assertEquals(Y, result)` is a tautology
4. **Happy-path only** — zero negative/edge/error cases
5. **Render-only frontend tests** — render component, check it exists, no
   interactions
6. **Raw fetch bypasses mocks** — utility is mocked but component uses direct
   `fetch()`

### Test Review Checklist

Before approving any test:

1. Does each test fail if the core logic is deleted? (Inversion Test)
2. Are mock return values transformed before assertion?
3. Do verify() calls use specific matchers, not any()?
4. Is auth tested before happy-path?
5. Are filter parameters tested with non-null values?
6. Is there at least one negative/error test per method?
7. Do frontend tests verify API URLs include serverBaseUrl?
8. Do frontend tests verify CSRF token in headers?
9. Are there render-only tests with no interactions?
10. Do E2E test steps all have assertions?
11. Is raw fetch() used instead of project utilities?
12. Are deprecated APIs used (wait vs waitFor)?
13. Could any test pass with a hardcoded return value?
14. Are edge cases tested (null, empty, boundary, negative)?
15. Do tests cover the same categories as known bugs? (status names, precision,
    auth ordering, filter pass-through)

### For AI Agents

This roadmap provides explicit rules, patterns, and code examples. Follow the
"DO/DON'T" sections precisely. Reference the templates in
`.specify/templates/testing/` when generating test code.

### For Human Developers

This roadmap explains the "why" behind testing decisions, provides
troubleshooting tips, and includes common pitfalls. Use this as a reference when
writing or reviewing tests.

---

## Test-Driven Development (TDD) Workflow

### Red-Green-Refactor Cycle

**MANDATORY for complex features. ENCOURAGED for all features.**

1. **Red**: Write failing test first (defines expected behavior)
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Improve code quality while keeping tests green

### When to Use TDD

**✅ Use TDD for:**

- Complex business logic (validation rules, calculations, state machines)
- API endpoint design (contract-first development)
- Service layer methods with multiple branches
- Critical user workflows

**⚠️ Consider TDD for:**

- Simple CRUD operations (may write tests after implementation)
- UI components (may write tests after component structure is clear)
- Exploratory/spike work (document learnings, then add tests)

### TDD Workflow Example

```java
// 1. RED: Write failing test
@Test
public void testCalculateStorageCapacity_WithChildLocations_ReturnsSum() {
    StorageDevice device = StorageDeviceBuilder.create()
        .withId("DEV-001")
        .build();

    StorageShelf shelf1 = StorageShelfBuilder.create()
        .withCapacity(50)
        .withParentDevice(device)
        .build();
    StorageShelf shelf2 = StorageShelfBuilder.create()
        .withCapacity(75)
        .withParentDevice(device)
        .build();

    int capacity = storageService.calculateCapacity(device.getId());

    assertEquals("Capacity should sum child locations", 125, capacity);
}

// 2. GREEN: Write minimal implementation
public int calculateCapacity(String deviceId) {
    // Minimal code to make test pass
    return 125; // Hardcoded for now
}

// 3. REFACTOR: Improve implementation
public int calculateCapacity(String deviceId) {
    StorageDevice device = deviceDAO.get(deviceId);
    return device.getShelves().stream()
        .mapToInt(StorageShelf::getCapacity)
        .sum();
}
```

---

## Test Pyramid and Coverage Goals

### Test Pyramid Structure

```
        ┌─────────────┐
        │   E2E (5%)   │  Cypress - User workflows
        │   Slow       │
        └──────┬───────┘
       ┌───────▼────────┐
       │ Integration    │  Spring Test - Full stack
       │   (15%)        │
       │   Medium       │
       └───────┬────────┘
    ┌──────────▼───────────┐
    │  ORM Validation (5%) │  Hibernate SessionFactory build
    │    Fast              │
    └──────────┬───────────┘
 ┌─────────────▼──────────────┐
 │    Unit Tests (75%)         │  JUnit 4 + Mockito - Business logic
 │    Very Fast                │
 └─────────────────────────────┘
```

### Coverage Goals

- **Backend**: >80% code coverage (measured via JaCoCo)
- **Frontend**: >70% code coverage (runner: Vitest; coverage provider must be configured)
- **Critical Paths**: 100% coverage (authentication, authorization, data
  validation)

### Test Execution Time Targets

- **Unit Tests**: <1 second per test
- **ORM Validation Tests**: <5 seconds total (per Constitution V.4)
- **Integration Tests**: <10 seconds per test
- **E2E Tests**: <30 seconds per test, <5 minutes for full suite (per
  Constitution V.5)

---

## Backend Testing

**Reference**: Use the existing repository test bases and templates in
`.specify/templates/testing/`. OpenELIS Global 2 is **traditional Spring MVC**
and does not use Spring Boot test slices.

This section provides comprehensive technical guidance for implementing backend
Java tests in OpenELIS Global 2.

**Repository reality (MANDATORY):** OpenELIS Global 2 uses **Traditional Spring
MVC** (not Spring Boot). Do not introduce Spring Boot testing annotations/slices
like `@WebMvcTest`, `@DataJpaTest`, or `@SpringBootTest` into new tests unless
the repository is explicitly migrated to Spring Boot.

**Database fixture ownership:**

- Create only the records needed by the scenario. Use real application services
  for the save, confirm, adopt, import, or review behavior under test.
- DBUnit Flat XML datasets in `src/test/resources/testdata/` remain supported for
  initial state. Load them through
  `BaseWebContextSensitiveTest.executeDataSetWithStateManagement(...)` so fixture
  writes participate in the test's transaction. Do not use fixture SQL to perform
  the business transition being asserted.
- The helper uses cascading truncation. It can affect dependent tables absent
  from the XML. A dataset is not isolated merely because it lists few tables.
- Choose transaction ownership explicitly as described below. Never repair a
  missing seed or replace an internal service just to hide another test's damage.

For quick reference, see
[Backend Testing Best Practices Guide](backend-testing-best-practices.md).

### TDD Workflow Integration

**MANDATORY**: Backend unit tests MUST follow Test-Driven Development (TDD)
workflow for complex logic.

**Red-Green-Refactor Cycle**:

1. **Red**: Write failing test first (defines expected behavior)
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Improve code quality while keeping tests green

**Test-First Development Process**:

- Write test BEFORE implementation
- Test defines the contract/interface
- Implementation satisfies the test
- Enables confident refactoring

**SDD Checkpoint Requirements**:

- **After Phase 1 (Entities)**: ORM validation tests MUST pass
- **After Phase 2 (Services)**: Unit tests MUST pass
- **After Phase 3 (Controllers)**: Integration tests MUST pass
- **Coverage Goal**: >80% (measured via JaCoCo)
- **All user stories**: Must have corresponding tests

### Test Organization

**File Naming**:

- Service tests: `{ServiceName}Test.java` (e.g.,
  `StorageLocationServiceTest.java`)
- Controller tests: `{ControllerName}Test.java` (e.g.,
  `StorageLocationRestControllerTest.java`)
- DAO tests: `{DAO}Test.java` (e.g., `StorageLocationDAOTest.java`)
- Integration tests: `{ServiceName}IntegrationTest.java` (e.g.,
  `StorageLocationServiceIntegrationTest.java`)

**Package Structure**:

- Mirror main package structure: `src/test/java/org/openelisglobal/{module}/`
- Service tests: `src/test/java/org/openelisglobal/{module}/service/`
- Controller tests: `src/test/java/org/openelisglobal/{module}/controller/`
- DAO tests: `src/test/java/org/openelisglobal/{module}/dao/`

**Test Naming Convention**:

- Format: `test{MethodName}_{Scenario}_{ExpectedResult}`
- Example: `testGetLocationById_WithValidId_ReturnsLocation`
- Descriptive: Clearly states what is being tested

**Test Grouping**:

- Group by layer (service, controller, dao)
- Group by feature/user story within each layer
- Use `@Category` annotations if needed for test suites

### Test Slicing Strategy Decision Tree (OpenELIS Global 2)

Use the smallest setup that can establish the claimed behavior. The test's
location or use of MockMvc does not, by itself, determine its level.

**Choose the level by what the test proves:**

| Level       | Intended proof                                                                         | Setup                                                                                                         |
| ----------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Unit        | One rule or transformation                                                             | JUnit 4; mock direct collaborators where needed; no Spring or database                                        |
| Component   | HTTP binding, response shape, or a UI interaction within one component                 | Standalone MockMvc or React Testing Library; control the component boundary                                   |
| Integration | Actual service wiring, persistence, history, security filters, or a transport contract | Real relevant internal services; PostgreSQL when persistence is claimed; external transport may be controlled |
| End-to-end  | An assembled user workflow across the browser, Bridge, and OpenELIS                    | Running components with identified revisions and inspected persisted outcomes                                 |

Permissions, history, concurrency, migrations, and protocol contracts describe
what is tested; they are not additional test levels. Human acceptance is separate.
The existing fast ORM mapping validation remains required.

#### Controller component tests

Use `MockMvcBuilders.standaloneSetup(...)` with an explicitly constructed
controller and Mockito collaborators for request/response behavior. This does not
prove deployed authorization, service wiring, or database changes. Use an
integration test with the relevant real filters and services for those claims.
Do not add the full database context to an HTTP-only check.

#### Database integration tests

Use the existing traditional Spring configuration, commonly through
`BaseWebContextSensitiveTest`, with real DAOs and relevant internal services.
Flush and clear before reloading persisted objects. A query test should assert
exact membership, filtering, and ordering, including meaningful negative cases;
an upper bound on collection size also accepts an empty, broken query.

**CRUD Testing Pattern**:

```java
@Test
public void testInsert_WithValidData_PersistsToDatabase() {
    // Arrange
    StorageRoom room = StorageRoomBuilder.create()
        .withName("Test Room")
        .withCode("TEST-ROOM")
        .build();

    // Act
    String id = storageLocationDAO.insert(room);
    entityManager.flush();
    entityManager.clear();

    // Assert
    StorageRoom retrieved = entityManager.find(StorageRoom.class, id);
    assertNotNull("Room should be persisted", retrieved);
    assertEquals("Name should match", "Test Room", retrieved.getName());
}
```

#### Full Integration Tests (Repository Pattern)

Use `BaseWebContextSensitiveTest` when the behavior needs the existing application
context and real database. `BaseTestConfig` supplies PostgreSQL and Liquibase;
`AppTestConfig` contains substitutions that must be inspected before claiming an
internal path is real. Default MockMvc setup does not load production security
filters.

Create owned initial records or load the necessary DBUnit fixture. Exercise the
actual services for business transitions, then inspect persisted results and
history. Follow [transaction ownership](#transaction-management): a rollback test
and a committed/concurrency test have different setup and cleanup requirements.
Never delete all rows above a guessed ID or sharing a common prefix.

### ORM Validation Tests (Constitution V.4)

**MANDATORY**: All Hibernate/JPA projects MUST include ORM validation tests.

```java
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class HibernateMappingValidationTest {

    @Test
    public void testHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(StorageRoom.class);
        config.addAnnotatedClass(StorageDevice.class);
        config.addAnnotatedClass(StorageShelf.class);
        config.addAnnotatedClass(StorageRack.class);
        config.setProperty("hibernate.dialect",
            "org.hibernate.dialect.PostgreSQLDialect");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("All Hibernate mappings should load without errors", sf);
        sf.close();
    }
}
```

**Requirements**:

- MUST execute in <5 seconds
- MUST NOT require database connection
- MUST validate all entity mappings load without errors
- MUST verify no JavaBean getter/setter conflicts

### Transaction Management

**CRITICAL**: Proper transaction management ensures test isolation and prevents
database pollution.

#### Tests that can roll back

For synchronous query or persistence checks, declare `@Transactional` on the
concrete test class. Fixture loading and `cleanRowsInCurrentConnection(...)` use
the same Spring-managed connection. Neither helper commits or closes the active
test transaction. Spring rolls it back after the test, including cascading
fixture replacement. The shared fixture helper refreshes its managed status and
observation-history caches after loading and after rollback.

```java
@Transactional
public class ExamplePersistenceTest extends BaseWebContextSensitiveTest {
    // Arrange initial records, call real services, flush/clear, and assert reloads.
}
```

This proves database writes within a transaction. It does not prove commit-time
events or behavior across independent transactions.

#### Tests that require committed state

`BaseWebContextSensitiveTest` retains `Propagation.NOT_SUPPORTED` for legacy
callers; it is not a recommendation for every new test. Without an active test
transaction, each fixture load or cleanup call owns an atomic transaction. A
successful call commits; a failed cleanup rolls back all of that call's changes.
It does not automatically restore data after the test.

Use explicit committed setup for concurrent workers, commit/rollback boundaries,
after-commit actions, and independent request transactions. Record the exact IDs
created by the test and remove only those records in teardown, including on
failure. Do not use global truncation, guessed ID ranges, or a shared seed repair
as ownership. Check outcomes from a fresh transaction after the application
transaction has ended.

Do not wrap such tests in `@Transactional` merely to make cleanup easier: an outer
test transaction can hide a missing application commit or change rollback and
locking behavior. `@Rollback(false)` is not needed to verify a database write;
flush and reload suffice unless commit itself is part of the requirement.

#### Shared contexts and caches

Do not swap dependencies on application singletons or retain injected services in
static fields across Spring contexts. Control external systems at their transport
boundary. When fixture loading bypasses a normal cache invalidation path, assign
refresh and restoration to the fixture owner; remove repeated per-test repairs.
Verify affected classes together and in a different execution order. A focused
pass does not establish that every caller of a shared helper remains correct.

### Test Data Management

**CRITICAL**: Consistent test data management ensures test reliability and
maintainability.

#### Builders/Factories (PREFERRED)

**PREFERRED**: Use builder pattern for test data creation.

**Benefits**:

- Reusable test data
- Clear, readable test setup
- Easy to create variations
- No hardcoded values

**Pattern**:

```java
// Builder class
public class StorageRoomBuilder {
    private StorageRoom room = new StorageRoom();

    public static StorageRoomBuilder create() {
        return new StorageRoomBuilder();
    }

    public StorageRoomBuilder withId(String id) {
        room.setId(id);
        return this;
    }

    public StorageRoomBuilder withName(String name) {
        room.setName(name);
        return this;
    }

    public StorageRoom build() {
        return room;
    }
}

// Usage in tests
@Test
public void testGetLocation_ReturnsLocation() {
    // Arrange: Use builder for test data
    StorageRoom room = StorageRoomBuilder.create()
        .withId("ROOM-001")
        .withName("Main Laboratory")
        .withCode("MAIN")
        .withActive(true)
        .build();

    when(storageLocationDAO.get("ROOM-001")).thenReturn(room);

    // Act & Assert
    StorageRoom result = storageLocationService.getLocationById("ROOM-001");
    assertEquals("Name should match", "Main Laboratory", result.getName());
}
```

**Key Points**:

- Use builder pattern for all test entities
- Builders should be in test package (not main)
- Use `create()` static method for fluent API
- Use `build()` to create entity instance

#### DBUnit (Backend Integration Tests)

**Use when**: Complex test data requiring multiple related entities in **backend
integration tests**.

**When to Use**:

- Existing tests using `BaseWebContextSensitiveTest`
- Complex test data with many relationships
- Reusable test datasets for backend integration tests

**Pattern**:

```java
public class StorageLocationRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Load complex test data from XML dataset
        executeDataSetWithStateManagement("testdata/storage-location-controller.xml");
    }
}
```

**Key Points**:

- Use for complex test data (multiple related entities)
- XML datasets in `src/test/resources/testdata/*.xml`
- Load via `executeDataSetWithStateManagement()`
- Fixture replacement rolls back only when an explicit test transaction owns
  it. Committed fixtures need teardown covering their actual effects, including
  cascading changes outside the XML.

#### Existing CI SQL fixture infrastructure

Existing browser CI jobs may load generated SQL through
`src/test/resources/load-test-fixtures.sh`. This is not a universal local setup
interface and does not establish cleanup for backend tests. For local browser or
manual work, use `scripts/dev-stack` and the registered project's application
scenario setup. See the [test-data strategy](test-data-strategy.md).

#### JdbcTemplate (Direct Database Operations)

**Use when**: Direct database operations needed (rare).

**When to Use**:

- Setting up test data that doesn't fit builder pattern
- Verifying database state directly
- Complex cleanup operations

**Pattern**:

```java
@Before
public void setUp() throws Exception {
    super.setUp();
    jdbcTemplate = new JdbcTemplate(dataSource);

    // Direct database setup
    jdbcTemplate.update(
        "INSERT INTO storage_room (id, name, code, active) VALUES (?, ?, ?, ?)",
        "ROOM-001", "Main Lab", "MAIN", true
    );
}
```

**Key Points**:

- Use sparingly (prefer builders)
- Use for complex setup that doesn't fit builder pattern
- Use for direct database verification

#### Testcontainers (Database Integration Tests)

**Use when**: You need a real database for integration tests.

**Repository note**: Testcontainers is already configured in `BaseTestConfig`
for the `test` Spring profile. Tests get a real PostgreSQL database by extending
`BaseWebContextSensitiveTest`.

**Key Points**:

- Configured in `BaseTestConfig`
- Uses PostgreSQL container + Liquibase migrations
- Choose owned records or existing DBUnit datasets for initial state; follow the
  explicit transaction and cleanup rules above.

### MockMvc Patterns

**CRITICAL**: Proper MockMvc usage ensures comprehensive HTTP layer testing.

#### Request Building Patterns

**GET Request**:

```java
mockMvc.perform(get("/rest/storage/rooms/ROOM-001")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk());
```

**POST Request**:

```java
StorageRoomForm form = new StorageRoomForm();
form.setName("Test Room");
form.setCode("TEST-ROOM");
String requestBody = objectMapper.writeValueAsString(form);

mockMvc.perform(post("/rest/storage/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
    .andExpect(status().isCreated());
```

**PUT Request**:

```java
StorageRoomForm form = new StorageRoomForm();
form.setName("Updated Room");
String requestBody = objectMapper.writeValueAsString(form);

mockMvc.perform(put("/rest/storage/rooms/ROOM-001")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
    .andExpect(status().isOk());
```

**DELETE Request**:

```java
mockMvc.perform(delete("/rest/storage/rooms/ROOM-001")
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isNoContent());
```

#### Response Assertion Patterns (JSONPath)

**Single Field**:

```java
mockMvc.perform(get("/rest/storage/rooms/ROOM-001"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value("ROOM-001"))
    .andExpect(jsonPath("$.name").value("Main Laboratory"));
```

**Array Elements**:

```java
mockMvc.perform(get("/rest/storage/rooms"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$").isArray())
    .andExpect(jsonPath("$[0].id").value("ROOM-001"))
    .andExpect(jsonPath("$[0].name").value("Main Laboratory"));
```

**Nested Objects**:

```java
mockMvc.perform(get("/rest/storage/devices/DEV-001"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value("DEV-001"))
    .andExpect(jsonPath("$.parentRoom.id").value("ROOM-001"))
    .andExpect(jsonPath("$.parentRoom.name").value("Main Laboratory"));
```

**Array Size**:

```java
mockMvc.perform(get("/rest/storage/rooms"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$").isArray())
    .andExpect(jsonPath("$.length()").value(2));
```

#### Error Response Testing

**400 Bad Request**:

```java
mockMvc.perform(post("/rest/storage/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"invalid\": \"json\"}"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.error").exists());
```

**404 Not Found**:

```java
when(storageLocationService.getLocationById("INVALID-ID"))
    .thenReturn(null);

mockMvc.perform(get("/rest/storage/rooms/INVALID-ID"))
    .andExpect(status().isNotFound());
```

**409 Conflict**:

```java
when(storageLocationService.insert(any(StorageRoom.class)))
    .thenThrow(new LIMSRuntimeException("Duplicate code"));

mockMvc.perform(post("/rest/storage/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
    .andExpect(status().isConflict());
```

**500 Internal Server Error**:

```java
when(storageLocationService.getLocationById("ERROR-ID"))
    .thenThrow(new RuntimeException("Database error"));

mockMvc.perform(get("/rest/storage/rooms/ERROR-ID"))
    .andExpect(status().isInternalServerError());
```

#### Authentication/Authorization Testing

**With Authentication**:

```java
mockMvc.perform(get("/rest/storage/rooms")
        .header("Authorization", "Bearer token")
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk());
```

**Without Authentication**:

```java
mockMvc.perform(get("/rest/storage/rooms")
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isUnauthorized());
```

### Mocks and Spring contexts

Use `@Mock` and `@InjectMocks` with JUnit 4's Mockito runner for unit tests and
standalone controller component tests. Keep real collaborators when their behavior
is part of the assertion.

In integration tests, inject the relevant real services, DAOs, message parsers,
and history recorder. If a remote system must be controlled, substitute its HTTP
client or other explicit transport boundary through the test configuration. Do
not replace internal mapping, confirmation, or history services while claiming
their lifecycle works. Do not mutate dependencies on cached singleton beans.

Starting a Spring context is not, by itself, a reason to mock a bean. Do not add
Spring Boot `@MockBean` or test-slice annotations to this traditional Spring setup.

### Unit Tests (JUnit 4 + Mockito)

**Use for**: Testing business logic in isolation.

**Benefits**:

- Fast execution (no Spring context, no database)
- Focused on business logic
- Easy to mock dependencies
- Test edge cases and error scenarios

**Pattern**:

```java
@RunWith(MockitoJUnitRunner.class)
public class StorageLocationServiceTest {

    @Mock
    private StorageLocationDAO storageLocationDAO;

    @Mock
    private FhirPersistanceService fhirService;

    @InjectMocks
    private StorageLocationServiceImpl storageLocationService;

    @Test
    public void testCalculateCapacity_WithNoChildren_ReturnsZero() {
        // Arrange
        StorageDevice device = StorageDeviceBuilder.create()
            .withId("DEV-001")
            .build();
        when(storageLocationDAO.get("DEV-001")).thenReturn(device);
        when(storageLocationDAO.findChildrenByParentId("DEV-001"))
            .thenReturn(Collections.emptyList());

        // Act
        int capacity = storageLocationService.calculateCapacity("DEV-001");

        // Assert
        assertEquals("Capacity should be zero with no children", 0, capacity);
    }
}
```

**Key Points**:

- Use `@RunWith(MockitoJUnitRunner.class)` for JUnit 4
- Use `@Mock` for dependencies, `@InjectMocks` for class under test
- Use builders/factories for test data (see
  [Test Data Management](#test-data-management))
- Test business logic only (mock DAOs, other services)
- Test edge cases (null, empty, boundary values)
- Test error scenarios (exceptions, validation failures)

**Exception Testing**:

```java
@Test(expected = LIMSRuntimeException.class)
public void testGetLocationById_WithInvalidId_ThrowsException() {
    // Arrange
    when(storageLocationDAO.get("INVALID-ID")).thenReturn(null);

    // Act
    storageLocationService.getLocationById("INVALID-ID");

    // Assert: Exception expected (handled by @Test(expected))
}
```

**Verification Testing**:

```java
@Test
public void testCreateLocation_CallsFhirService() {
    // Arrange
    StorageRoom room = StorageRoomBuilder.create()
        .withName("Test Room")
        .build();
    when(storageLocationDAO.insert(any(StorageRoom.class))).thenReturn("ROOM-001");

    // Act
    storageLocationService.insert(room);

    // Assert: Verify FHIR service was called
    verify(fhirService, times(1)).createUpdateFhirResource(any(Specimen.class));
}
```

---

## Frontend Testing

### Vitest + React Testing Library (Unit and Component Tests)

The current runner is Vitest (`frontend/package.json`), configured in
`frontend/vite.config.ts` with globals, jsdom, and `frontend/src/setupTests.js`.
The setup file installs jest-dom matchers; their package name does not imply a
Jest runner. React component tests with mocked API utilities are component
coverage, even when an existing filename includes `integration`.

See [Vitest Best Practices Guide](vitest-best-practices.md) for patterns.

#### TDD Workflow Integration

**MANDATORY**: Frontend unit tests MUST follow Test-Driven Development (TDD)
workflow for complex logic.

**Red-Green-Refactor Cycle**:

1. **Red**: Write failing test first (defines expected behavior)
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Improve code quality while keeping tests green

**Test-First Development Process**:

- Write test BEFORE implementation
- Test defines the contract/interface
- Implementation satisfies the test
- Enables confident refactoring

**SDD Checkpoint Requirements**:

- **After Phase 4 (Frontend)**: All unit tests MUST pass
- **Coverage Goal**: >70% (runner: Vitest; coverage provider must be configured)
- **All user stories**: Must have corresponding unit tests

#### Test Organization

**File Naming**:

- `ComponentName.test.jsx` (co-located with component)
- OR `__tests__/ComponentName.test.jsx` (separate directory)
- One test file per component

**Test Naming Convention**:

- Format: `test{Scenario}_{ExpectedResult}`
- Example: `testSubmitForm_WithValidData_ShowsSuccessMessage`
- Descriptive: Clearly states what is being tested

**Test Grouping**:

- Use `describe()` blocks for related tests
- Group by feature, user story, or component section
- Example: `describe("ComponentName Form Validation", () => { ... })`

#### Standard Import Order (MANDATORY)

Keep imports grouped for readability. Vitest hoists `vi.mock` independently of
its visual position; use `vi.hoisted` for values a mock factory needs:

```javascript
// 1. React
import React from "react";

// 2. Testing Library (all utilities in one import)
import { render, screen, fireEvent, within, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 4. jest-dom matchers are already installed by src/setupTests.js

// 5. IntlProvider (if component uses i18n)
import { IntlProvider } from "react-intl";

// 6. Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import ComponentName from "./ComponentName";

// 8. Utilities (import functions, not just for mocking)
import { getFromOpenElisServer } from "../utils/Utils";

// 9. Messages/translations
import messages from "../../../languages/en.json";
```

#### Mock Structure

Use Vitest mocks. Hoisted factories cannot safely close over ordinary
module-level `const` values initialized later:

```javascript
vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
}));

const mockHistory = vi.hoisted(() => ({
  replace: vi.fn(),
  push: vi.fn(),
}));

vi.mock("react-router-dom", async (importOriginal) => ({
  ...(await importOriginal()),
  useHistory: () => mockHistory,
  useLocation: () => ({ pathname: "/path" }),
}));
```

#### Helper Functions

**Standard Render Helper**:

```javascript
// Standard render helper with IntlProvider
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};
```

**API Mock Setup Helper**:

```javascript
// Helper function to setup API mocks
const setupApiMocks = (overrides = {}) => {
  const defaults = {
    rooms: [],
    devices: [],
    samples: [],
  };
  const data = { ...defaults, ...overrides };

  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/storage/rooms")) {
      callback(data.rooms);
    } else if (url.includes("/rest/storage/devices")) {
      callback(data.devices);
    }
    // Add more URL patterns as needed
  });
};
```

#### Query Methods

**Decision Tree** (per React Testing Library best practices):

1. **getBy\*** - Use for required elements (throws if not found)
   - Use when: Element must exist for test to proceed
   - Example: `screen.getByText("Submit")`

2. **queryBy\*** - Use for absence checks (returns null if not found)
   - Use when: Checking if element does NOT exist
   - Use inside `waitFor` (doesn't throw during retries)
   - Example: `screen.queryByText("Error")` with `.not.toBeInTheDocument()`

3. **findBy\*** - Use for async element queries (waits and retries)
   - Use when: Element appears after async operation
   - Automatically waits and retries
   - Example: `await screen.findByText("Loaded Data")`

**DO**:

- Use `screen.getBy*` for required elements (throws if not found)
- Use `screen.queryBy*` for absence checks (returns null if not found)
- Use `screen.findBy*` for async element queries (waits and retries)
- Use `queryBy*` inside `waitFor` (doesn't throw during retries)

**DON'T**:

- Return false from `waitFor` instead of asserting (only thrown errors retry)
- Use `setTimeout` for async operations (use `waitFor` instead - no retry logic)

```javascript
// ✅ CORRECT: Use waitFor with queryBy* for async operations
test("testAsyncOperation", async () => {
  renderWithIntl(<ComponentName />);

  await waitFor(() => {
    const element = screen.queryByText("Loaded Data");
    expect(element).toBeInTheDocument();
  });
});

// ✅ CORRECT: Use findBy* for async elements
test("testAsyncElement", async () => {
  renderWithIntl(<ComponentName />);

  const element = await screen.findByText("Loaded Data");
  expect(element).toBeInTheDocument();
});

// ❌ WRONG: Using setTimeout (no retry logic)
setTimeout(() => {
  expect(screen.getByText("Loaded Data")).toBeInTheDocument();
}, 1000); // FAILS - no retry logic, brittle timing

// ✅ CORRECT: waitFor retries thrown queries/assertions
await waitFor(() => {
  expect(screen.getByText("Loaded Data")).toBeInTheDocument(); // Retried until found or timeout
});
```

#### userEvent vs fireEvent

**PREFERRED: userEvent** (React Testing Library
recommendations):

- **userEvent**: Simulates real user interactions (clicks, typing, keyboard
  events)
- More realistic: Triggers all events a real user would trigger
- Better for: User interactions (clicks, typing, keyboard navigation)
- Example: `await userEvent.click(button)`,
  `await userEvent.type(input, "text")`

**FALLBACK: fireEvent** (use only when userEvent doesn't work):

- **fireEvent**: Directly fires DOM events
- Less realistic: Only fires the specific event
- Use when: userEvent doesn't work (rare edge cases)
- Example: `fireEvent.click(button)`,
  `fireEvent.change(input, { target: { value: "text" } })`

**Decision Tree**:

1. **User interaction** (click, type, keyboard) → Use `userEvent` ✅
2. **userEvent doesn't work** → Use `fireEvent` ⚠️
3. **Programmatic event** (not user-initiated) → Use `fireEvent` ⚠️

```javascript
// ✅ CORRECT: Use userEvent for user interactions
test("testUserInteraction", async () => {
  renderWithIntl(<ComponentName />);

  const button = screen.getByRole("button", { name: /submit/i });
  await userEvent.click(button); // More realistic - triggers all events

  const input = screen.getByLabelText(/name/i);
  await userEvent.type(input, "Test Name", { delay: 0 }); // Simulates typing
});

// ⚠️ ACCEPTABLE: Use fireEvent when userEvent doesn't work
test("testFireEventFallback", () => {
  renderWithIntl(<ComponentName />);

  const input = screen.getByLabelText(/name/i);
  fireEvent.change(input, { target: { value: "Test Name" } }); // Direct event
});
```

#### Async Testing Patterns

**CRITICAL**: Proper async testing prevents flaky tests and timing issues.

**DO - Use waitFor**:

```javascript
// ✅ CORRECT: waitFor with queryBy* (doesn't throw during retries)
test("testAsyncOperation", async () => {
  renderWithIntl(<ComponentName />);

  await waitFor(
    () => {
      const element = screen.queryByText("Loaded Data");
      expect(element).toBeInTheDocument();
    },
    { timeout: 5000 },
  );
});
```

**DO - Use findBy\***:

```javascript
// ✅ CORRECT: findBy* automatically waits and retries
test("testAsyncElement", async () => {
  renderWithIntl(<ComponentName />);

  const element = await screen.findByText("Loaded Data", {}, { timeout: 5000 });
  expect(element).toBeInTheDocument();
});
```

**DO - Use act() for state updates**:

```javascript
// ✅ CORRECT: act() for state updates in Carbon components
test("testStateUpdate", async () => {
  renderWithIntl(<ComponentName />);

  const input = screen.getByLabelText(/name/i);
  await act(async () => {
    await userEvent.type(input, "Test");
  });

  expect(input.value).toBe("Test");
});
```

**DON'T - Use setTimeout**:

```javascript
// ❌ WRONG: setTimeout has no retry logic, brittle timing
test("testAsyncOperation", async () => {
  renderWithIntl(<ComponentName />);

  await new Promise((resolve) => setTimeout(resolve, 1000)); // Brittle!
  expect(screen.getByText("Loaded Data")).toBeInTheDocument();
});
```

**DON'T - Use getBy\* in waitFor**:

```javascript
// ❌ WRONG: getBy* throws immediately, breaks waitFor retry logic
await waitFor(() => {
  expect(screen.getByText("Loaded Data")).toBeInTheDocument(); // Throws if not found
});
```

#### Carbon Component Testing

**Why Carbon Needs Special Handling**: Carbon components use React portals,
async rendering, and complex event handling.

**TextInput**:

```javascript
test("testTextInput", async () => {
  renderWithIntl(<ComponentName />);

  const input = screen.getByLabelText(/name/i);
  await userEvent.type(input, "Test Name", { delay: 0 });

  expect(input.value).toBe("Test Name");
});
```

**ComboBox** (existing component patterns):

```javascript
test("testComboBox", async () => {
  renderWithIntl(<ComponentName />);

  const input = screen.getByRole("combobox", { name: /room/i });

  // Type to open dropdown
  await userEvent.type(input, "Main Laboratory", { delay: 0 });

  // Wait for dropdown to open (Carbon renders in portal)
  await waitFor(
    () => {
      const menu = document.querySelector('[role="listbox"]');
      expect(menu && menu.children.length > 0).toBeTruthy();
    },
    { timeout: 2000 },
  );

  // Select option
  const option = await screen.findByRole("option", {
    name: /main laboratory/i,
  });
  await userEvent.click(option);
});
```

**OverflowMenu** (portal pattern):

```javascript
test("testOverflowMenu", async () => {
  renderWithIntl(<ComponentName />);

  // Find menu button
  const menuButton = screen.getByTestId("overflow-menu-button");
  await userEvent.click(menuButton);

  // Wait for menu items to render in portal
  await waitFor(
    () => {
      const menu = screen.queryByRole("menu");
      expect(menu).toBeInTheDocument();
    },
    { timeout: 5000 },
  );

  // Select menu item
  const menuItem = await screen.findByRole("menuitem", {
    name: /delete/i,
  });
  await userEvent.click(menuItem);
});
```

**DataTable**:

```javascript
test("testDataTable", async () => {
  renderWithIntl(<ComponentName />);

  // Wait for table to render
  const table = await screen.findByRole("table");

  // Find row by text (use within for scoped queries)
  const row = within(table).getByText("Row Data");
  expect(row).toBeInTheDocument();

  // Interact with row action
  const actionButton = within(row).getByRole("button", { name: /edit/i });
  await userEvent.click(actionButton);
});
```

**Modal/Dialog** (portal pattern):

```javascript
test("testModal", async () => {
  renderWithIntl(<ComponentName />);

  // Open modal
  const openButton = screen.getByRole("button", { name: /open modal/i });
  await userEvent.click(openButton);

  // Wait for modal to appear (Carbon uses portals)
  const modal = await screen.findByRole("dialog");
  expect(modal).toBeInTheDocument();

  // Interact with modal
  const confirmButton = within(modal).getByRole("button", { name: /confirm/i });
  await userEvent.click(confirmButton);
});
```

#### Test Data Management

**Mock Data Builders/Factories** (per Medium article - use generic cases):

```javascript
// ✅ CORRECT: Builder pattern for test data
const createMockRoom = (overrides = {}) => ({
  id: "1",
  name: "Main Laboratory",
  code: "MAIN",
  active: true,
  ...overrides,
});

// Use in tests
const mockRoom = createMockRoom({ name: "Test Room" });
```

**Reusable Mock Setup**:

```javascript
// Helper function for consistent mock setup
const setupApiMocks = (overrides = {}) => {
  const defaults = {
    rooms: [createMockRoom()],
    devices: [],
    samples: [],
  };
  const data = { ...defaults, ...overrides };

  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/storage/rooms")) {
      callback(data.rooms);
    }
    // Add more patterns
  });
};
```

**Edge Case Data** (per Medium article - test null, empty, boundary):

```javascript
// Test edge cases
const edgeCaseData = {
  nullValue: null,
  emptyString: "",
  emptyArray: [],
  boundaryValue: 100, // Max/min values
};
```

#### What to Test (behavioral coverage)

**DO - Test User-Visible Behavior**:

- ✅ What user sees (text, buttons, forms)
- ✅ User interactions (clicks, typing, navigation)
- ✅ Inputs and outputs (form submission, API calls)
- ✅ Edge cases (null, empty, boundary values)
- ✅ Error states (validation errors, API errors)

**DON'T - Test Implementation Details**:

- ❌ Internal component state (unless user-visible)
- ❌ Function call counts
- ❌ Prop values (unless they affect user-visible behavior)
- ❌ Redux store state (unless user-visible)
- ❌ Implementation-specific details

```javascript
// ✅ CORRECT: Test user-visible behavior
test("testFormSubmission", async () => {
  renderWithIntl(<ComponentName />);

  const input = screen.getByLabelText(/name/i);
  await userEvent.type(input, "Test Name");

  const submitButton = screen.getByRole("button", { name: /submit/i });
  await userEvent.click(submitButton);

  // Assert user-visible outcome
  expect(await screen.findByText("Success")).toBeInTheDocument();
});

// ❌ WRONG: Test implementation details
test("testInternalState", () => {
  const component = renderWithIntl(<ComponentName />);
  // Don't test internal state
  expect(component.state.isLoading).toBe(false); // Implementation detail
});
```

**Edge Case Testing** (per Medium article):

```javascript
// Test null values
test("testNullValue", () => {
  renderWithIntl(<ComponentName value={null} />);
  expect(screen.getByText("N/A")).toBeInTheDocument();
});

// Test empty values
test("testEmptyArray", () => {
  renderWithIntl(<ComponentName items={[]} />);
  expect(screen.getByText("No items")).toBeInTheDocument();
});

// Test boundary values
test("testBoundaryValue", () => {
  renderWithIntl(<ComponentName maxLength={100} />);
  const input = screen.getByLabelText(/name/i);
  await userEvent.type(input, "a".repeat(100));
  expect(input.value.length).toBe(100);
});
```

### Cypress E2E Testing

This section is for maintaining existing Cypress tests. Use Playwright for new
end-to-end coverage; do not copy legacy fixture setup into local feature setup.

> **Lifecycle status:** Cypress E2E is now a **legacy/deprecation track** in
> this repository. Keep existing coverage healthy; prioritize new E2E work in
> Playwright unless a Cypress-only gap is justified.

**Reference**:
[Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
for functional requirements.

This section provides comprehensive technical guidance for implementing Cypress
E2E tests. For quick reference, see
[Cypress Best Practices Guide](.specify/guides/cypress-best-practices.md).

#### Selector Strategy (MANDATORY Priority)

**STRICT Priority Order** (per Cypress official recommendations + profy.dev):

1. **data-testid attributes** (MOST STABLE - PREFERRED)
   - Format: `data-testid="{component}-{action}"` (e.g.,
     `data-testid="storage-location-selector"`)
   - Why: Survives CSS changes, refactoring, styling updates, i18n changes
   - Example: `cy.get('[data-testid="submit-button"]')`
   - **MANDATORY**: All interactive elements MUST have data-testid during
     development
   - **Migration Note**: For existing tests, gradually migrate to data-testid.
     For new tests, data-testid is mandatory.

2. **ARIA roles and labels** (ACCESSIBLE - SECOND CHOICE)
   - Use `cy.get('[role="button"]')` or `cy.get('[role="option"]')`
   - Use `cy.get('[aria-label="..."]')` for labeled elements
   - Why: Accessibility-first, semantic meaning, Carbon components use ARIA
   - Example: `cy.get('[role="dialog"]')` for Carbon modals

3. **Semantic selectors with context** (TEXT CONTENT - USE CAREFULLY)
   - Pattern from profy.dev: `cy.get("main").find("li").contains("Issues")`
   - Always scope to parent container to avoid ambiguity
   - Why: User-visible, but can break with i18n - always scope to container
   - Example: `cy.get('[data-testid="table"]').contains('tr', 'Sample-001')`

4. **CSS selectors** (LAST RESORT - STRONGLY DISCOURAGED)
   - Only when no other option exists
   - Document why CSS selector was necessary
   - Avoid deep chains: `cy.get('.container > div > div > button')` (profy.dev
     anti-pattern)

```javascript
// ✅ CORRECT: data-testid (PREFERRED)
cy.get('[data-testid="storage-location-selector"]').click();

// ✅ CORRECT: ARIA role (SECOND CHOICE)
cy.get('[role="button"]').contains("Save Location").click();

// ⚠️ ACCEPTABLE: Semantic selector with context
cy.get('[data-testid="table"]').contains("tr", "Sample-001").click();

// ❌ AVOID: CSS selector (LAST RESORT)
cy.get(".storage-selector-button").click();
```

#### Session Management (cy.session())

**CRITICAL**: Use `cy.session()` to preserve login state across tests (10-20x
faster - Cypress official pattern).

**Pattern**:

```javascript
// In cypress/support/commands.js
Cypress.Commands.add("login", (username, password) => {
  cy.session(
    [username, password],
    () => {
      // Login via API (FAST - not UI)
      cy.request({
        method: "POST",
        url: "/api/OpenELIS-Global/LoginPage",
        body: { username, password },
      }).then((response) => {
        // Store session cookies/tokens
        // Adapt to OpenELIS authentication implementation
        // May use cookies, tokens, or session storage
        window.localStorage.setItem("authToken", response.body.token);
      });
    },
    {
      cacheAcrossSpecs: true, // Share session across test files
    },
  );
});

// In test files - login runs ONCE, cached for all tests
before(() => {
  cy.login("admin", "password"); // Only runs once per test file
});
```

**Benefits**:

- Login runs ONCE per test file, not per test
- Session cached and reused automatically
- 10-20x faster test execution
- No redundant authentication

**testIsolation Clarification**: With cy.session(), typically keep
`testIsolation: true` (cy.session handles caching). Only set
`testIsolation: false` if you need shared state beyond session caching.

#### Test Data Management (API-First)

**DO**: Use `cy.request()` for fast test data setup (profy.dev recommendation).

```javascript
before(() => {
  // Fast API-based setup (NOT UI interactions)
  cy.request("POST", "/rest/storage/rooms", {
    name: "Test Room",
    code: "TEST-ROOM",
  }).then((response) => {
    cy.wrap(response.body.id).as("roomId");
  });
});
```

**DON'T**: Use slow UI interactions for setup (profy.dev anti-pattern).

```javascript
// ❌ WRONG: Slow, brittle, unnecessary (10+ seconds)
beforeEach(() => {
  cy.visit("/storage");
  cy.get('[data-testid="add-room-button"]').click();
  cy.get('[data-testid="room-name-input"]').type("Test Room");
  cy.get('[data-testid="save-button"]').click();
  // ... 10+ seconds of UI interactions
});
```

**Fixture Pattern** (profy.dev):

```javascript
// Use fixtures for consistent test data
cy.intercept("GET", "/rest/storage/rooms", { fixture: "rooms.json" }).as(
  "getRooms",
);
cy.visit("/storage");
cy.wait("@getRooms");
```

**Important clarification**:

- Fixture-backed intercepts are acceptable for **read-only page bootstrapping**
  when you are not validating backend behavior.
- If you stub responses that your user story is meant to validate (especially
  mutation success), the test is **not** an E2E test and must not live in
  `frontend/cypress/e2e/`.
- For real E2E environments, prefer the unified fixture loader (see
  `src/test/resources/FIXTURE_LOADER_README.md`).

**Smart Fixture Management**: Check if fixtures exist before loading, skip
loading if fixtures already present, use environment variables for control
(`CYPRESS_SKIP_FIXTURES`, `CYPRESS_FORCE_FIXTURES`).

#### DOM Query Effectiveness

**Effective Patterns** (from profy.dev article):

1. **Scoped queries** (profy.dev pattern: `cy.get("main").find("li")`):

   ```javascript
   // Start with container, then find children
   cy.get('[data-testid="storage-dashboard"]')
     .find('[data-testid="location-card"]')
     .first()
     .click();
   ```

2. **Table row filtering** (profy.dev debugging example - use `tbody`):

   ```javascript
   // From profy.dev: Avoid selecting header row
   cy.get("main")
     .find("tbody") // Exclude thead
     .find("tr")
     .each(($el, index) => {
       // Process data rows only
     });
   ```

3. **Text-based queries with context** (profy.dev pattern):

   ```javascript
   // Pattern from profy.dev: cy.get("main").find("tr").contains(...)
   cy.get('[data-testid="issues-table"]')
     .find("tbody") // Filter out header row
     .find("tr")
     .contains("Sample-001") // Find row containing text
     .find('[data-testid="view-button"]')
     .click();
   ```

4. **Viewport management** (profy.dev: set viewport before visit):

   ```javascript
   beforeEach(() => {
     cy.viewport(1025, 900); // Desktop viewport
     cy.visit("/dashboard");
   });
   ```

5. **Chaining with .should()** (Cypress retry-ability):
   ```javascript
   cy.get('[data-testid="submit-button"]')
     .should("be.visible")
     .should("not.be.disabled")
     .click();
   ```

**Anti-Patterns to Avoid** (from profy.dev and Cypress docs):

- ❌ Deep CSS selector chains: `cy.get('.container > div > div > button')`
  (profy.dev)
- ❌ Using `:nth-child()` selectors (brittle)
- ❌ Querying by text without context (ambiguous - profy.dev)
- ❌ Using `cy.wait(5000)` instead of `.should()` assertions (profy.dev)
- ❌ Selecting table header rows when you want data rows (profy.dev debugging
  example)
- ❌ Not setting viewport (mobile vs desktop differences - profy.dev)

#### cy.intercept() Patterns

**Official Cypress Pattern** with aliases:

```javascript
// Set up intercept BEFORE action that triggers it
cy.intercept("POST", "/rest/storage/rooms").as("createRoom");
cy.get('[data-testid="save-button"]').click();
cy.wait("@createRoom").its("response.statusCode").should("eq", 201);
```

**Timing**: Intercepts MUST be set up before actions that trigger them.

**Fixture Usage**:

```javascript
cy.intercept("GET", "/rest/storage/rooms", { fixture: "rooms.json" }).as(
  "getRooms",
);
cy.visit("/storage");
cy.wait("@getRooms");
```

#### Cypress Network Policy (Spy-first; no stubbing the operation under test)

**Rule for `frontend/cypress/e2e/`**:

- `cy.intercept()` is **spy-first**: use it to alias and assert
  requests/responses.
- **DO NOT** stub the primary mutation endpoint under test
  (`PUT|POST|PATCH|DELETE`) with a fabricated success response.
- If you must stub backend responses to validate UI-only behavior, that belongs
  in a **mocked-backend UI** test suite (not `frontend/cypress/e2e/`) and must
  be labeled accordingly.

**Spy-only mutation example** (valid E2E):

```javascript
cy.intercept("PUT", "**/rest/storage/rooms/**").as("updateRoom");
cy.get('[data-testid="edit-location-save-button"]').click();
cy.wait("@updateRoom").its("response.statusCode").should("eq", 200);
```

#### “Real-effect assertions” (required for E2E mutations)

After a successful mutation, prove it was real by doing **at least one**:

- Reload/revisit and verify the updated state is still present.
- `cy.request()` a read endpoint and verify the updated field
  (read-after-write).

Example:

```javascript
cy.request("GET", `/rest/storage/rooms/${roomId}`)
  .its("body")
  .its("name")
  .should("eq", newName);
```

#### Test Simplification (Happy Path Focus)

**MANDATORY**: Tests MUST focus on user workflows, not implementation details
(profy.dev philosophy).

**Good Test Structure** (from profy.dev):

```javascript
it("should assign sample to storage location", () => {
  // Arrange: Test data already set up in before() hook (API-based)
  cy.visit("/storage/assignment");

  // Act: User workflow (what user does)
  cy.get('[data-testid="sample-input"]').type("SAMPLE-001");
  cy.get('[data-testid="location-selector"]').click();
  cy.get('[data-testid="room-option"]').contains("Main Lab").click();
  cy.get('[data-testid="assign-button"]').click();

  // Assert: User-visible outcome
  cy.get('[data-testid="success-notification"]')
    .should("be.visible")
    .should("contain.text", "Sample assigned");
});
```

**What NOT to Test** (profy.dev guidance):

- ❌ Internal component state
- ❌ Function call counts
- ❌ Prop values
- ❌ Redux store state (unless user-visible)
- ❌ Multiple ways to do the same thing (test one happy path)

#### Carbon Design System Specific Patterns

**Why Carbon Needs Special Handling**: Carbon components use React portals,
rendering outside normal DOM hierarchy, requiring explicit waits for portal
elements.

**ComboBox Selection** (Carbon + profy.dev patterns):

```javascript
// Carbon ComboBox requires explicit selection (not auto-select)
cy.get('[data-testid="room-combobox"]')
  .should("be.visible")
  .click()
  .type("Main Lab");
// Wait for dropdown to open (Carbon renders in portal)
cy.get('[role="listbox"]').should("be.visible");
// Explicitly select option (Carbon doesn't auto-select)
cy.get('[role="option"]').contains("Main Laboratory").click();
```

**DataTable Interactions** (profy.dev table pattern + Carbon):

```javascript
// Use tbody to exclude header (profy.dev pattern)
cy.get('[data-testid="storage-table"]')
  .find("tbody") // Exclude thead
  .find("tr")
  .contains("Room-001") // Find row by text
  .find('[data-testid="action-button"]')
  .click();
```

**Modal/Dialog** (Carbon portal pattern):

```javascript
// Wait for modal to be visible (Carbon uses portals - renders outside normal DOM)
cy.get('[role="dialog"]').should("be.visible");
// Modal content is in portal, use data-testid for buttons
cy.get('[data-testid="modal-confirm-button"]')
  .should("be.visible")
  .should("not.be.disabled")
  .click();
```

**OverflowMenu** (Carbon portal pattern):

```javascript
// Carbon OverflowMenu renders items in portal
cy.get('[data-testid="overflow-menu-button"]').click();
// Wait for menu items to render in portal
cy.get('[role="menu"]').should("be.visible");
cy.get('[role="menuitem"]').contains("Delete").click();
```

#### Debugging Techniques

**Chrome DevTools Integration** (from profy.dev):

1. Open DevTools in Cypress UI (right-click → Inspect)
2. Use Sources tab to open test files
3. Add breakpoints to pause execution
4. Inspect variables in right-side menu
5. Hover over elements to highlight in UI
6. Use console.log() for debugging

**Common Debugging Scenarios** (profy.dev examples):

- **Table header row issue**: Use `tbody` to filter out headers
- **Viewport issues**: Set viewport before visit
- **Timing issues**: Use `.should()` instead of `cy.wait()`

**Post-Run Review**: See
[Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
for mandatory post-run review requirements (console logs, screenshots, test
output).

#### Migration Strategy

**Priority Order** for migrating existing tests (incremental approach - don't
break existing tests):

1. **Convert login to cy.session()** - Biggest performance gain (10-20x faster)
2. **Convert UI-based setup to API-based** - 10x faster test data setup
3. **Replace CSS selectors with data-testid** - More stable, survives
   refactoring
4. **Add viewport management** - Prevents mobile/desktop differences
5. **Fix intercept timing** - Set up intercepts before actions
6. **Add element readiness checks** - Use `.should()` for retry-ability
7. **Replace arbitrary waits with .should()** - Leverage Cypress retry-ability

**Migration Checklist**:

- [ ] Login uses cy.session() pattern
- [ ] Test data setup uses API (cy.request() or fixtures)
- [ ] All selectors use data-testid (or ARIA roles if data-testid not available)
- [ ] Viewport set before visit
- [ ] Intercepts set up before actions
- [ ] Element readiness checked with .should()
- [ ] No arbitrary cy.wait() calls
- [ ] Tests focus on user workflows, not implementation details

#### Test Execution Workflow (Constitution V.5)

**During Development**:

- Run individual test files:
  `npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"`
- Maximum 5-10 test cases per execution
- Review console logs and screenshots after each run

**CI/CD Only**:

- Full suite: `npm run cy:run`
- Run only in pipeline or pre-merge validation

#### Configuration Requirements

```javascript
// cypress.config.js
module.exports = defineConfig({
  video: false, // MUST be disabled by default (Constitution V.5)
  screenshotOnRunFailure: true, // MUST be enabled (Constitution V.5)
  defaultCommandTimeout: 10000, // Appropriate for Carbon components
  e2e: {
    setupNodeEvents(on, config) {
      // Browser console logging enabled by default (Cypress captures automatically)
      return config;
    },
    baseUrl: "https://localhost",
    testIsolation: true, // Default: true (cy.session() handles caching)
  },
  viewportWidth: 1025, // Desktop default
  viewportHeight: 900,
});
```

### Playwright E2E Testing

**Reference**:
[Playwright Best Practices Guide](.specify/guides/playwright-best-practices.md)
for comprehensive patterns and examples.

**Command-first workflow**:

- `/plan-record-playwright` for planning feature/PR flow coverage and
  project-aware recording stages
- `/write-playwright-test` for first-pass authoring and project registration
- `/debug-playwright` for evidence-first debugging
- `/audit-playwright` for selector and anti-pattern audits

Playwright is the primary E2E framework for new test development. It offers
modern async/await patterns, auto-waiting, and better debugging tools.

#### When to Use Playwright vs Cypress

| Scenario                 | Recommended    | Reason                         |
| ------------------------ | -------------- | ------------------------------ |
| New E2E tests            | **Playwright** | Modern API, better debugging   |
| Existing Cypress tests   | Cypress        | Maintain while deprecating     |
| Complex multi-tab/window | **Playwright** | Native support                 |
| Visual regression        | **Playwright** | Built-in screenshot comparison |
| API testing alongside UI | **Playwright** | First-class request API        |

#### Quick Start

Start the isolated local stack from the repository root, then run one registered
spec. The wrapper discovers the worktree URL and handles authentication:

```bash
scripts/dev-stack up
scripts/dev-stack playwright --project=core-app playwright/tests/foundational/core/microbiology-whonet-export.spec.ts
```

For direct npm scripts, export `eval "$(scripts/dev-stack env)"` at the repository
root before changing to `frontend/`. Install browsers with `npm run pw:install`
and select one file and its registered project. See
[`frontend/playwright/README.md`](../../frontend/playwright/README.md) for
foundational/demo setup policies and the current authentication configuration.

#### Key Patterns

**1. Setup Project for Authentication**

Authenticate once, reuse session for all tests:

```typescript
// playwright/tests/auth.setup.ts
import { test as setup, expect } from "@playwright/test";

setup("authenticate", async ({ page }) => {
  await page.goto("/");
  await page.getByLabel("Username").fill("admin");
  await page.getByLabel("Password").fill("adminADMIN!");
  await page.getByRole("button", { name: "Login" }).click();
  await expect(page.locator('[data-testid="menu-button"]')).toBeVisible();
  await page.context().storageState({ path: "playwright/.auth/user.json" });
});
```

**2. Page Object Model**

Encapsulate interactions in reusable classes:

```typescript
// playwright/fixtures/sidenav.ts
export class Sidenav {
  constructor(private page: Page) {}

  async expectExpanded() {
    await expect(this.page.locator(".cds--side-nav")).toHaveClass(
      /cds--side-nav--expanded/,
    );
  }

  async toggle() {
    await this.page.locator('[data-testid="menu-button"]').click();
  }
}
```

**3. Use Auto-Retrying Assertions (NO waitForTimeout)**

```typescript
// ❌ BAD
await page.waitForTimeout(2000);

// ✅ GOOD
await expect(element).toBeVisible(); // Auto-retries
```

**4. Semantic Selectors for Carbon Components**

```typescript
// Prefer role-based selectors
page.getByRole("button", { name: "Submit" });
page.getByLabel("Username");

// Use exact: true for substring conflicts
page.getByRole("button", { name: "Storage", exact: true });

// Carbon structural elements (acceptable)
page.locator(".cds--side-nav");
```

**Project registration check**:

```bash
python .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/sidenav.spec.ts
```

#### Configuration (`playwright.config.ts`)

```typescript
export default defineConfig({
  testDir: "./playwright/tests",
  timeout: 30_000,
  expect: { timeout: 5_000 },
  use: {
    baseURL: process.env.BASE_URL || "https://localhost",
    ignoreHTTPSErrors: true,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "setup", testMatch: /.*\.setup\.ts/ },
    {
      name: "chromium",
      use: { storageState: "playwright/.auth/user.json" },
      dependencies: ["setup"],
    },
  ],
});
```

#### File Structure

```
frontend/playwright/
├── .auth/user.json          # Cached auth (gitignored)
├── fixtures/
│   └── sidenav.ts           # Page Objects
└── tests/
    ├── auth.setup.ts        # Auth setup
    └── sidenav.spec.ts      # Test specs
```

---

## Test Data Management

### Builders/Factories Pattern

**MANDATORY**: Use builders/factories, NOT hardcoded values.

#### Backend Example

```java
public class StorageLocationBuilder {
    private String id;
    private String name;
    private String code;
    private Boolean active = true;

    public static StorageLocationBuilder create() {
        return new StorageLocationBuilder();
    }

    public StorageLocationBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public StorageLocationBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public StorageLocationBuilder withCode(String code) {
        this.code = code;
        return this;
    }

    public StorageLocationBuilder withActive(Boolean active) {
        this.active = active;
        return this;
    }

    public StorageRoom build() {
        StorageRoom room = new StorageRoom();
        if (id != null) room.setId(id);
        room.setName(name != null ? name : "Test Room " + UUID.randomUUID());
        room.setCode(code != null ? code : "TEST-" + System.currentTimeMillis());
        room.setActive(active);
        return room;
    }
}
```

**Usage**:

```java
StorageRoom room = StorageRoomBuilder.create()
    .withName("Main Laboratory")
    .withCode("MAIN-LAB")
    .withActive(true)
    .build();
```

#### Frontend Example

```javascript
export const createMockStorageLocation = (overrides = {}) => {
  return {
    id: "LOC-001",
    name: "Test Location",
    code: "TEST-LOC",
    active: true,
    ...overrides,
  };
};
```

### Test Data Cleanup

Backend cleanup follows [transaction ownership](#transaction-management), not the
choice of fixture format. DBUnit does not remove the need to clean up committed
state. Inspect shared cache effects as well as stored rows.

Browser tests must follow their registered project's setup policy and own the
records they create. Preserve returned IDs and clean up only those records where
teardown is required. Do not delete every record with a common `TEST-` prefix;
other tests can own records with the same prefix. Prefer existing project fixture
and scenario lifecycle code over inventing another cleanup interface.

---

## SDD Integration

### Testing in Spec-Driven Development Workflow

Testing is integrated into every phase of the SDD workflow:

```
specify → clarify → plan → tasks → implement
   ↓         ↓        ↓       ↓         ↓
  (no      (no     (test   (test    (TDD
  tests)   tests)  strategy) tasks)  cycle)
```

### Plan Phase: Testing Strategy

**MANDATORY**: Every `plan.md` MUST include a "Testing Strategy" section.

```markdown
## Testing Strategy

**Reference**: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)

**Coverage Goals**:

- Backend: >80% (JaCoCo)
- Frontend: >70% (Vitest)

**Test Types**:

- Unit tests: Individual business rules
- Component tests: HTTP mapping and isolated UI behavior
- Integration tests: Real internal services, persistence, and relevant filters
- E2E tests: Critical assembled user workflows
- ORM validation: Retain the required fast framework-mapping check

**Test Data Management**:

- Backend: Owned initial data; explicit rollback or committed transaction ownership
- Frontend: Registered Playwright project setup and application scenarios

**Checkpoint Validations**:

- After Phase 1 (Entities): ORM validation tests must pass
- After Phase 2 (Services): Unit tests must pass
- After Phase 3 (Controllers): Integration tests must pass
- After Phase 4 (Frontend): E2E tests must pass
```

### Tasks Phase: Test Task Generation

**MANDATORY**: Test tasks MUST appear before implementation tasks (TDD
enforcement).

```markdown
## Phase 3: User Story 1 - Location Management

### Tests for User Story 1 (MANDATORY)

- [ ] T010 [P] [US1] Unit test for StorageLocationService in
      src/test/java/org/openelisglobal/storage/service/StorageLocationServiceTest.java
      (Template: .specify/templates/testing/JUnit4ServiceTest.java.template)
- [ ] T010a [P] [US1] ORM validation test in
      src/test/java/org/openelisglobal/storage/HibernateMappingValidationTest.java
      (Template: .specify/templates/testing/DataJpaTestDao.java.template)
- [ ] T011 [P] [US1] Integration test for REST endpoint in
      src/test/java/org/openelisglobal/storage/controller/StorageLocationControllerIntegrationTest.java
      (Template: .specify/templates/testing/WebMvcTestController.java.template)
- [ ] T011b [P] [US1] Playwright E2E test in
      frontend/playwright/tests/foundational/core/storage-assignment.spec.ts
      (register in frontend/playwright.config.ts)

### Implementation for User Story 1

- [ ] T012 [US1] Create StorageRoom entity (depends on T010a passing)
- [ ] T013 [US1] Create StorageRoomService (depends on T010 passing)
```

### Implement Phase: TDD Enforcement

**MANDATORY**: Execute test tasks before implementation tasks.

1. Write failing test (Red)
2. Run test to confirm failure
3. Write minimal implementation (Green)
4. Run test to confirm pass
5. Refactor implementation (Refactor)
6. Run test to confirm still passing

**Checkpoint Validation**: Tests must pass before proceeding to next phase.

---

## Quick Reference

### Backend Test Commands

```bash
# One unit or integration class (both use Surefire)
mvn test -Dtest=YourTestClass

# Broader backend suite
mvn test

# ORM validation tests
mvn test -Dtest='*ValidationTest'

# Specific test class
mvn test -Dtest=StorageLocationServiceTest

# Coverage report
mvn verify
# Report: target/site/jacoco/index.html
```

### Frontend Test Commands

```bash
cd frontend

# Unit tests (Vitest)
npm test                    # Run all
npm exec -- vitest --watch  # Watch mode (npm test uses vitest run)
# Coverage requires a configured Vitest provider; none is currently declared
# in package.json, so do not assume npm test -- --coverage works out of the box.

# Cypress E2E legacy (development: individual files)
npm run cy:run -- --spec "cypress/e2e/feature.cy.js"

# Cypress E2E legacy (CI only: full suite)
npm run cy:run

# Playwright E2E (primary for new tests)
npm run pw:install          # First time: install browsers
npm run pw:test -- --project=core-app playwright/tests/foundational/core/{feature}.spec.ts
npm run pw:test:ui          # Interactive UI debugger
npm run pw:test:headed      # See browser window
npm run pw:test -- --project=core-app file.spec.ts  # Select its registered project
```

### Test Template Locations

- Backend Service: `.specify/templates/testing/JUnit4ServiceTest.java.template`
- Backend Controller:
  `.specify/templates/testing/WebMvcTestController.java.template`
- Backend DAO: `.specify/templates/testing/DataJpaTestDao.java.template`
- Frontend Component:
  `.specify/templates/testing/VitestComponent.test.jsx.template`
- Frontend E2E (Cypress): `.specify/templates/testing/CypressE2E.cy.js.template`
- Frontend E2E (Playwright):
  [Playwright Best Practices](.specify/guides/playwright-best-practices.md)

### Common Anti-Patterns

**Backend**:

- ❌ Introducing Spring Boot test slices (`@WebMvcTest`, `@SpringBootTest`) in
  this repo
- ❌ Hardcoded test data (use builders/factories)
- ❌ Leaving transaction/data ownership implicit: choose rollback isolation or
  committed setup with explicit cleanup for the behavior under test
- ❌ Skipping ORM validation tests (catches mapping errors early)

**Frontend**:

- ❌ Using CSS selectors in Cypress/Playwright (use data-testid or ARIA roles)
- ❌ Bypassing the registered project setup policy; demo scenarios may require
  UI-only setup, while foundational tests can use supported APIs
- ❌ Using `setTimeout` in Vitest tests (use `waitFor`)
- ❌ Using `waitForTimeout()` in Playwright (use auto-retrying assertions)
- ❌ Running full E2E suite during development (run individual files)

---

## References

- **Constitution**: `.specify/memory/constitution.md` (Principle V)
- **Research**: `specs/001-sample-storage/research.md` (Sections 10, 12)
- **Templates**: `.specify/templates/testing/`
- **AGENTS.md**: Testing section for workflow integration

---

**Last Updated**: 2025-01-XX  
**Maintained By**: OpenELIS Global Core Team  
**Questions?**: Post in GitHub Discussions or weekly developer sync
