# REST Controller Testing — Patterns & Reference

OpenELIS Global 2 supports three distinct approaches for testing REST
controllers:

---

## Strict Assertion Policy: Strong Assertions vs. Weak Assertions

**Weak assertions provide false confidence.** A test that merely checks
`assertNotNull(response)` or `assertFalse(list.isEmpty())` passes even when the
controller returns corrupted payload values, wrong field mappings, or invalid
state.

### Anti-Pattern (Weak Assertions — DO NOT USE)

```java
// WEAK: Does not verify payload contents, properties, or array bounds
assertNotNull("Response should not be null", response);
assertNotNull("Menu object should not be null", menuWrapper.getMenu());
assertFalse("List should not be empty", items.isEmpty());
.andExpect(jsonPath("$.data").exists());
```

### Best Practice (Strong Assertions — MANDATORY)

```java
// STRONG: Verifies exact HTTP status, precise array length, and exact scalar property values
mockMvc.perform(get("/rest/menu/testElement1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.menu.elementId").value("testElement1"))
        .andExpect(jsonPath("$.menu.presentationOrder").value(1))
        .andExpect(jsonPath("$.menu.openInNewWindow").value(false))
        .andExpect(jsonPath("$.menu.isActive").value(true))
        .andExpect(jsonPath("$.menu.hideInOldUI").value(false))
        .andExpect(jsonPath("$.childMenus.length()").value(0));
```

---

## Pattern 1: Standalone Controller Unit Tests (No Database / No Datasets)

Use this pattern when testing controllers that delegate business logic to
services, validate request payloads, perform parameter filtering, or format HTTP
responses.

### Key Benefits

- **No Spring Context Startup**: Runs unit-test fast (<100ms per test file).
- **No Database / DBUnit Datasets**: Does not require PostgreSQL containers,
  schema creation, or XML datasets.
- **Isolated Testing**: Tests only HTTP routing, payload binding, parameter
  validation, and controller response mapping.

### Standard Setup

```java
package org.openelisglobal.myfeature.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.myfeature.service.MyFeatureService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class MyFeatureRestControllerTest {

    @Mock
    private MyFeatureService myFeatureService;

    @InjectMocks
    private MyFeatureRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getItems_returnsFilteredList() throws Exception {
        when(myFeatureService.getItems("active")).thenReturn(List.of(new Item(1, "Item A")));

        mockMvc.perform(get("/rest/myfeature?status=active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Item A"));
    }
}
```

---

## Pattern 2: Web Context Integration Tests (With Database / Datasets)

Use this pattern when testing end-to-end HTTP endpoints that interact with
database transactions, entity queries, and data persistence round-trips.

### Infrastructure & Inheritance

Inheriting from `BaseWebContextSensitiveTest` provides:

- A real **Testcontainers PostgreSQL** database instance.
- A pre-configured `super.mockMvc` bound to the full DispatcherServlet context.
- DBUnit helper methods like `executeDataSetWithStateManagement(path)`.

### Authentication vs. Legacy Session Data Distinction

It is critical to distinguish between the **authenticated Spring Security
principal** and **legacy OpenELIS session attributes**:

1. **`BaseWebContextSensitiveTest` Security Context**:
   - `BaseWebContextSensitiveTest` is annotated with `@WithDaemonUser`, which
     initially supplies a `DaemonAuthenticationToken("1")` with `ROLE_SYSTEM`.
     However, `BaseWebContextSensitiveTest.setDefaultTestAuthentication()`
     subsequently installs an `admin` principal with `ROLE_ADMIN` and
     `ROLE_RESULTS` in `@Before` directly into Spring's `SecurityContextHolder`.
   - Its `MockMvc` is built via
     `MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build()`,
     which **does not register the security filter chain** (it does not call
     `.apply(springSecurity())`).
   - Furthermore, `AppTestConfig` excludes `SecurityConfig`, so
     `@EnableMethodSecurity` is inactive and method-level security
     (`@PreAuthorize`) is inert.
   - Consequently, `BaseWebContextSensitiveTest` **cannot** be used to prove
     anonymous (401) or role-denied (403) authorization enforcement.
2. **`UserSessionData` in `MockHttpSession`**:
   - Setting `session.setAttribute(IActionConstants.USER_SESSION_DATA, usd)`
     supplies legacy controller-level user state (such as `systemUserId`, user
     language, and lab context) consumed by `BaseRestController` or
     `ControllerUtils.getSysUserId()`.
   - It **does not** authenticate or select the Spring principal, and it does
     not invoke or exercise Spring Security authorization.

### Standard Web Context Setup

```java
@Before
@Override
public void setUp() throws Exception {
    super.setUp();
    // Supplies legacy session data to controller helpers; does NOT configure Spring Security filters
    UserSessionData usd = new UserSessionData();
    usd.setSytemUserId(1);
    session = new MockHttpSession();
    session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
}
```

Then pass `.session(session)` to `mockMvc.perform(...)` when the controller
reads legacy session state.

### Data Strategy for Web Context Tests

1. **JdbcTemplate (Recommended for small custom fixtures)**: Use SQL statements
   with IDs in the 99000+ range. Always clean up in `@After`.
2. **DBUnit XML Datasets**: Use
   `executeDataSetWithStateManagement("testdata/my_file.xml")` when sharing
   existing XML dataset definitions.

---

## Pattern 3: Security Slice Tests (Spring Security Filters & Role Enforcement)

When testing endpoint security, role gates, or `@PreAuthorize` authorization,
**tests of the security filters must actually install them**.

### Why Security Slice Tests?

- `BaseWebContextSensitiveTest` bypasses security filters and pre-authenticates
  as an administrator.
- Security slice tests configure Spring Security with `@EnableWebSecurity`,
  `@EnableMethodSecurity(prePostEnabled = true)`, and register the filter chain
  on MockMvc using `apply(springSecurity())`.
- Base class: Extend `org.openelisglobal.security.SecuritySliceMockMvcTest`,
  which automatically builds `mockMvc`:
  ```java
  mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  ```

### The Three Required Security Cases

Every secured REST endpoint should test the three canonical states:

#### 1. Anonymous (Unauthenticated) Request $\to$ `401 Unauthorized`

Tests that unauthenticated requests cannot access protected resources.

```java
@Test
public void getSampleTypes_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/rest/sample-types")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
}
```

#### 2. Denied-Role Request $\to$ `403 Forbidden`

Tests that an authenticated user without the required role (e.g. `RESULTS` or
`REPORTS` user accessing an admin-only endpoint) is blocked.

```java
@Test
public void getSampleTypes_nonAdminRole_returns403() throws Exception {
    mockMvc.perform(get("/rest/sample-types")
            .with(user("worker").roles("RESULTS"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
}
```

#### 3. Permitted-Role Request $\to$ Clears Security Gate (`200 OK`, `204 No Content`, etc.)

Tests that an authenticated user with the proper role (e.g. `ADMIN`) clears the
security filter and reaches the controller logic.

```java
@Test
public void getSampleTypes_adminRole_clearsAuthGate() throws Exception {
    mockMvc.perform(get("/rest/sample-types")
            .with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
}
```

#### Supplying `UserSessionData` with Spring Security

If the controller logic under test also inspects legacy session data (such as
`ControllerUtils.getSysUserId()`), supply both the Spring Security user and the
mock session attribute:

```java
UserSessionData sessionData = new UserSessionData();
sessionData.setSytemUserId(1);

mockMvc.perform(put("/rest/sample-types/1")
        .with(user("admin").roles("ADMIN"))
        .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBodyJson))
        .andExpect(status().isOk());
```

Here, `.with(user(...))` sets the Spring Security principal for `@PreAuthorize`
authorization, while `.sessionAttr(...)` provides legacy session values to
controller method handlers.

### Tested Codebase Examples

- [SampleTypeManagementRestControllerSecurityTest.java](file:///home/coder/Documents/OpenELIS-Global-2/src/test/java/org/openelisglobal/common/management/controller/rest/SampleTypeManagementRestControllerSecurityTest.java)
  — comprehensive role tests for `ADMIN` mutations, unauthenticated 401,
  non-admin 403, and input validation.
- [AlertRestControllerSecurityTest.java](file:///home/coder/Documents/OpenELIS-Global-2/src/test/java/org/openelisglobal/alert/controller/rest/AlertRestControllerSecurityTest.java)
  — multi-role endpoint access testing (`RESULTS`, `VALIDATION`, `RECEPTION`,
  `ADMIN`) and session data interaction.
- [SecuritySliceMockMvcTest.java](file:///home/coder/Documents/OpenELIS-Global-2/src/test/java/org/openelisglobal/security/SecuritySliceMockMvcTest.java)
  — base class for security slices that applies `springSecurity()`.

---

## Common Strong Assertion Patterns

```java
// Status assertions
.andExpect(status().isOk())
.andExpect(status().isUnauthorized())
.andExpect(status().isForbidden())
.andExpect(status().isBadRequest())
.andExpect(status().isNotFound())
.andExpect(status().isConflict())
.andExpect(status().isUnprocessableEntity())

// Strong JSON property assertions — always use exact scalar values
.andExpect(jsonPath("$.status").value("SUCCESS"))
.andExpect(jsonPath("$.length()").value(2))
.andExpect(jsonPath("$[0].id").value(101))
.andExpect(jsonPath("$.messageKey").value("error.notConfigured"))
.andExpect(jsonPath("$.items.length()").value(3))
.andExpect(jsonPath("$.errorDetails").doesNotExist())
```

---

## Naming & Execution

| Suffix                           | Scope                                | Filter / DB Setup                             | Execution Speed           |
| -------------------------------- | ------------------------------------ | --------------------------------------------- | ------------------------- |
| `*RestControllerTest`            | Standalone MockMvc / Stateless logic | No Spring Context / No DB                     | Instant (<100ms)          |
| `*RestControllerIntegrationTest` | Web Context + Testcontainers DB      | Full Spring Context + DB (No Security Filter) | Full integration (~5-15s) |
| `*RestControllerSecurityTest`    | Security filter & role enforcement   | Security filter chain via `springSecurity()`  | Fast slice (~1-3s)        |

### Command to Run Tests

```bash
mvn test -Dtest=ReflexRuleIdFilterRestControllerTest -Dsurefire.failIfNoSpecifiedTests=false
```
