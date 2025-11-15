# Backend Testing Best Practices Quick Reference

**Quick Reference Guide** for common backend Java/Spring Boot testing patterns
in OpenELIS Global 2.

**For Comprehensive Guidance**: See
[Testing Roadmap](.specify/guides/testing-roadmap.md) for detailed patterns and
examples.

**For TDD Workflow & SDD Checkpoints**: See
[Testing Roadmap - TDD Workflow Integration](.specify/guides/testing-roadmap.md#tdd-workflow-integration).

---

## Test Slicing Decision Tree

**CRITICAL**: Use focused test slices when possible for faster execution.

1. **Testing REST controller HTTP layer only?** → Use `@WebMvcTest` ✅
2. **Testing DAO/repository persistence layer only?** → Use `@DataJpaTest` ✅
3. **Testing complete workflow with full application context?** → Use
   `@SpringBootTest` ✅
4. **Legacy integration tests with Testcontainers/DBUnit?** → Use
   `BaseWebContextSensitiveTest` ⚠️

**When to Use Each**:

| Test Type          | Annotation                    | Use Case               | Speed  | Context        |
| ------------------ | ----------------------------- | ---------------------- | ------ | -------------- |
| Controller         | `@WebMvcTest`                 | HTTP layer only        | Fast   | Web layer only |
| DAO                | `@DataJpaTest`                | Persistence layer only | Fast   | JPA layer only |
| Integration        | `@SpringBootTest`             | Full workflow          | Medium | Full context   |
| Legacy Integration | `BaseWebContextSensitiveTest` | Testcontainers/DBUnit  | Slow   | Full context   |

---

## Annotation Cheat Sheet

### @WebMvcTest

**Use for**: REST controller HTTP layer testing.

```java
@RunWith(SpringRunner.class)
@WebMvcTest(StorageLocationRestController.class)
public class StorageLocationRestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean  // ✅ Use @MockBean (NOT @Mock)
    private StorageLocationService storageLocationService;
}
```

**Key Points**:

- Use `@MockBean` for Spring context mocking
- Fast execution (no full application context)
- Focus on HTTP layer only

### @DataJpaTest

**Use for**: DAO/repository persistence layer testing.

```java
@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StorageLocationDAOTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StorageLocationDAO storageLocationDAO;
}
```

**Key Points**:

- Use `TestEntityManager` for test data (NOT JdbcTemplate)
- Automatic transaction rollback
- Fast execution (no full application context)

### @SpringBootTest

**Use for**: Full integration testing.

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional  // ✅ PREFERRED: Automatic rollback
public class StorageLocationServiceIntegrationTest {
    @Autowired
    private StorageLocationService storageLocationService;
}
```

**Key Points**:

- Use `@Transactional` for automatic rollback (preferred)
- Full Spring context loaded
- Test complete workflows

### @MockBean vs @Mock

**@MockBean**: Use in Spring context tests (`@WebMvcTest`, `@SpringBootTest`)

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

### @Transactional

**PREFERRED**: Use for automatic rollback in `@SpringBootTest` and
`@DataJpaTest`.

```java
@SpringBootTest
@Transactional  // ✅ Automatic rollback
public class StorageLocationServiceIntegrationTest {
    // No manual cleanup needed
}
```

**Key Points**:

- Automatic rollback after each test
- No manual cleanup needed
- Use for `@SpringBootTest` and `@DataJpaTest`

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

### @Transactional (PREFERRED)

**DO**: Use `@Transactional` for automatic rollback.

```java
@SpringBootTest
@Transactional  // ✅ Automatic rollback
public class StorageLocationServiceIntegrationTest {
    // No manual cleanup needed
}
```

### Manual Cleanup (When @Transactional Doesn't Work)

**Use when**: Using `BaseWebContextSensitiveTest` (legacy pattern).

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
- [ ] ❌ Using `@SpringBootTest` when `@WebMvcTest` or `@DataJpaTest` would work
- [ ] ❌ Manual cleanup when `@Transactional` would work
- [ ] ❌ Hardcoded test data instead of builders/factories
- [ ] ❌ Using `JdbcTemplate` in `@DataJpaTest` (use `TestEntityManager`)
- [ ] ❌ Testing implementation details instead of behavior
- [ ] ❌ Inconsistent test naming (use
      `test{MethodName}_{Scenario}_{ExpectedResult}`)
- [ ] ❌ Missing transaction management (use `@Transactional` when possible)
- [ ] ❌ Not using builders/factories for test data

---

## Quick Decision Trees

### Which Test Annotation to Use?

1. **Testing HTTP layer only?** → `@WebMvcTest` ✅
2. **Testing persistence layer only?** → `@DataJpaTest` ✅
3. **Testing full workflow?** → `@SpringBootTest` ✅
4. **Legacy Testcontainers/DBUnit?** → `BaseWebContextSensitiveTest` ⚠️

### Which Mock Annotation to Use?

1. **Spring context test?** (`@WebMvcTest`, `@SpringBootTest`) → `@MockBean` ✅
2. **Isolated unit test?** (`@RunWith(MockitoJUnitRunner.class)`) → `@Mock` ✅

### Which Transaction Management to Use?

1. **Using `@SpringBootTest` or `@DataJpaTest`?** → `@Transactional` ✅
2. **Using `BaseWebContextSensitiveTest`?** → Manual cleanup in `@After` ⚠️

---

**For Detailed Examples**: See
[Testing Roadmap - Backend Testing](.specify/guides/testing-roadmap.md#backend-testing).
