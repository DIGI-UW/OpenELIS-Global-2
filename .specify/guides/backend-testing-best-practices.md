# Backend Testing Best Practices Quick Reference

**Quick Reference Guide** for common backend Java testing patterns in OpenELIS
Global 2.

**For Comprehensive Guidance**: See
[Testing Roadmap](.specify/guides/testing-roadmap.md) for detailed patterns and
examples.

**For TDD Workflow & SDD Checkpoints**: See
[Testing Roadmap - TDD Workflow Integration](.specify/guides/testing-roadmap.md#tdd-workflow-integration).

---

## Test Slicing Decision Tree

**CRITICAL**: Use focused test slices when possible for faster execution.

**Repository reality**: OpenELIS Global 2 uses **Traditional Spring MVC** (not
Spring Boot). Spring Boot test slices (`@WebMvcTest`, `@DataJpaTest`,
`@SpringBootTest`) are **not** the standard for this repo. Use
`BaseWebContextSensitiveTest` for Spring-context tests.

1. **Testing REST controller HTTP layer only?** → Use
   `BaseWebContextSensitiveTest` + MockMvc ✅
2. **Testing DAO/persistence layer only?** → Use `BaseWebContextSensitiveTest` +
   real DAO beans ✅
3. **Testing complete workflow (service → DAO → DB)?** → Use
   `BaseWebContextSensitiveTest` ✅

**When to Use Each**:

| Test Type   | Base Class/Pattern            | Use Case                         | Speed  | Context      |
| ----------- | ----------------------------- | -------------------------------- | ------ | ------------ |
| Controller  | `BaseWebContextSensitiveTest` | HTTP mapping/validation          | Medium | Full context |
| DAO         | `BaseWebContextSensitiveTest` | HQL queries, CRUD, relationships | Medium | Full context |
| Integration | `BaseWebContextSensitiveTest` | Full workflow (service→DAO→DB)   | Medium | Full context |

---

## Annotation Cheat Sheet

### Controller Tests (HTTP layer)

**Use for**: REST controller request/response mapping (with real Spring
context).

```java
public class StorageLocationRestControllerTest extends BaseWebContextSensitiveTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean // ✅ Mock service for HTTP-only tests
  private StorageLocationService storageLocationService;
}
```

**Key Points**:

- Use `@MockBean` for Spring context mocking
- Focus on routing/validation/status codes/JSON shape
- This test does NOT prove persistence (service is mocked)

### DAO Tests (Persistence layer)

**Use for**: Real HQL query behavior and persistence layer correctness.

```java
public class StorageLocationDAOTest extends BaseWebContextSensitiveTest {
  @Autowired
  private StorageLocationDAO storageLocationDAO;
}
```

**Key Points**:

- Use DBUnit datasets for complex setup:
  `executeDataSetWithStateManagement("testdata/<file>.xml")`
- Use `EntityManager`/`JdbcTemplate` only when necessary and clean up properly

### Integration Tests (Full workflow)

**Use for**: Service → DAO → DB workflows with real persistence.

```java
public class StorageLocationServiceIntegrationTest extends BaseWebContextSensitiveTest {
  @Autowired
  private StorageLocationService storageLocationService;
}
```

**Key Points**:

- Must include at least one “real-effect assertion” (read-after-write / DB state
  change)
- Prefer DBUnit-managed datasets for setup/cleanup; otherwise do targeted
  cleanup

### @MockBean vs @Mock

**@MockBean**: Use in Spring context tests (extends
`BaseWebContextSensitiveTest`)

```java
@MockBean  // ✅ Spring context test
private StorageLocationService storageLocationService;
```

**@Mock**: Use in isolated unit tests (`@RunWith(MockitoJUnitRunner.class)`)

```java
@Mock  // ✅ Isolated unit test
private StorageLocationDAO storageLocationDAO;

@InjectMocks
private StorageLocationServiceImpl storageLocationService;
```

**Decision Tree**:

1. Spring context test? → Use `@MockBean` ✅
2. Isolated unit test? → Use `@Mock` ✅

---

## MockMvc Quick Patterns

### Request Building

**GET**:

```java
mockMvc.perform(get("/rest/storage/rooms/ROOM-001")
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk());
```

**POST**:

```java
String requestBody = objectMapper.writeValueAsString(form);
mockMvc.perform(post("/rest/storage/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
    .andExpect(status().isCreated());
```

**PUT**:

```java
String requestBody = objectMapper.writeValueAsString(form);
mockMvc.perform(put("/rest/storage/rooms/ROOM-001")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
    .andExpect(status().isOk());
```

**DELETE**:

```java
mockMvc.perform(delete("/rest/storage/rooms/ROOM-001")
        .contentType(MediaType.APPLICATION_JSON))
    .andExpect(status().isNoContent());
```

### Response Assertions (JSONPath)

**Single Field**:

```java
.andExpect(jsonPath("$.id").value("ROOM-001"))
.andExpect(jsonPath("$.name").value("Main Laboratory"));
```

**Array Elements**:

```java
.andExpect(jsonPath("$").isArray())
.andExpect(jsonPath("$[0].id").value("ROOM-001"));
```

**Nested Objects**:

```java
.andExpect(jsonPath("$.parentRoom.id").value("ROOM-001"));
```

**Array Size**:

```java
.andExpect(jsonPath("$.length()").value(2));
```

### Error Responses

**400 Bad Request**:

```java
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.error").exists());
```

**404 Not Found**:

```java
.andExpect(status().isNotFound());
```

**409 Conflict**:

```java
.andExpect(status().isConflict());
```

**500 Internal Server Error**:

```java
.andExpect(status().isInternalServerError());
```

---

## Test Data Management

### Builders/Factories (PREFERRED)

**DO**: Use builder pattern for test data.

```java
StorageRoom room = StorageRoomBuilder.create()
    .withId("ROOM-001")
    .withName("Main Laboratory")
    .withCode("MAIN")
    .withActive(true)
    .build();
```

**DON'T**: Use hardcoded values or direct entity construction.

```java
// ❌ BAD
StorageRoom room = new StorageRoom();
room.setId("ROOM-001");
room.setName("Main Laboratory");
```

### DBUnit (Legacy Pattern)

**Use when**: Complex test data requiring multiple related entities.

```java
@Before
public void setUp() throws Exception {
    super.setUp();
    executeDataSetWithStateManagement("test-data/storage-hierarchy.xml");
}
```

### JdbcTemplate (Direct Database Operations)

**Use when**: Direct database operations needed (rare).

```java
jdbcTemplate.update(
    "INSERT INTO storage_room (id, name, code, active) VALUES (?, ?, ?, ?)",
    "ROOM-001", "Main Lab", "MAIN", true
);
```

---

## Transaction Management

### Manual Cleanup (BaseWebContextSensitiveTest)

**Use when**: You create rows outside DBUnit-managed datasets.

```java
@Before
public void setUp() throws Exception {
    super.setUp();
    cleanStorageTestData(); // Clean before test
}

@After
public void tearDown() throws Exception {
    cleanStorageTestData(); // Clean after test
}
```

---

## Test Organization

### File Naming

- Service tests: `{ServiceName}Test.java`
- Controller tests: `{ControllerName}Test.java`
- DAO tests: `{DAO}Test.java`
- Integration tests: `{ServiceName}IntegrationTest.java`

### Test Naming Convention

**Format**: `test{MethodName}_{Scenario}_{ExpectedResult}`

**Example**: `testGetLocationById_WithValidId_ReturnsLocation`

### Package Structure

- Mirror main package structure: `src/test/java/org/openelisglobal/{module}/`
- Service tests: `src/test/java/org/openelisglobal/{module}/service/`
- Controller tests: `src/test/java/org/openelisglobal/{module}/controller/`
- DAO tests: `src/test/java/org/openelisglobal/{module}/dao/`

---

## TDD Workflow Quick Reference

**Red-Green-Refactor Cycle**:

1. **Red**: Write failing test first
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Improve code quality while keeping tests green

**Test-First Development**:

- Write test BEFORE implementation
- Test defines the contract/interface
- Implementation satisfies the test

**SDD Checkpoint Requirements**:

- **After Phase 1 (Entities)**: ORM validation tests MUST pass
- **After Phase 2 (Services)**: Unit tests MUST pass
- **After Phase 3 (Controllers)**: Integration tests MUST pass
- **Coverage Goal**: >80% (measured via JaCoCo)

---

## Anti-Patterns Checklist

- [ ] ❌ Using `@Mock` in Spring context tests (use `@MockBean`)
- [ ] ❌ Using `@MockBean` in isolated unit tests (use `@Mock`)
- [ ] ❌ Introducing Spring Boot test slices (`@WebMvcTest`, `@DataJpaTest`,
      `@SpringBootTest`) in this repo
- [ ] ❌ Hardcoded test data instead of builders/factories
- [ ] ❌ Testing implementation details instead of behavior
- [ ] ❌ Inconsistent test naming (use
      `test{MethodName}_{Scenario}_{ExpectedResult}`)
- [ ] ❌ Not using builders/factories for test data

---

## Quick Decision Trees

### Which Test Base to Use?

1. **Need Spring context?** → `BaseWebContextSensitiveTest` ✅
2. **Isolated unit logic only?** → `@RunWith(MockitoJUnitRunner.class)` ✅

### Which Mock Annotation to Use?

1. **Spring context test?** (`BaseWebContextSensitiveTest`) → `@MockBean` ✅
2. **Isolated unit test?** (`@RunWith(MockitoJUnitRunner.class)`) → `@Mock` ✅

---

**For Detailed Examples**: See
[Testing Roadmap - Backend Testing](.specify/guides/testing-roadmap.md#backend-testing).
