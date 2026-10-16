# REST Controller Testing — Patterns & Reference

OpenELIS Global 2 supports two primary approaches for testing REST controllers:

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
- **Isolated Testing**: Tests only the HTTP mapping, parameter handling, and
  controller logic.

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
database transactions, Spring Security filters, or complex framework component
scanning.

### Infrastructure & Inheritance

Inheriting from `BaseWebContextSensitiveTest` provides:

- A real **Testcontainers PostgreSQL** database instance.
- A pre-configured `super.mockMvc` bound to the full DispatcherServlet context.
- DBUnit helper methods like `executeDataSetWithStateManagement(path)`.

### Session Authentication

Every secured endpoint in `BaseWebContextSensitiveTest` requires session
authentication in `setUp()`:

```java
@Before
@Override
public void setUp() throws Exception {
    super.setUp();
    UserSessionData usd = new UserSessionData();
    usd.setSytemUserId(1);
    session = new MockHttpSession();
    session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
}
```

Then pass `.session(session)` to `mockMvc.perform(...)`.

### Data Strategy for Web Context Tests

1. **JdbcTemplate (Recommended for small custom fixtures)**: Use SQL statements
   with IDs in the 99000+ range. Always clean up in `@After`.
2. **DBUnit XML Datasets**: Use
   `executeDataSetWithStateManagement("testdata/my_file.xml")` when sharing
   existing XML dataset definitions.

---

## Common Strong Assertion Patterns

```java
// Status assertions
.andExpect(status().isOk())
.andExpect(status().isBadRequest())
.andExpect(status().isNotFound())
.andExpect(status().isConflict())
.andExpect(status().isServiceUnavailable())

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

| Suffix                           | Scope                                | Execution Speed           |
| -------------------------------- | ------------------------------------ | ------------------------- |
| `*RestControllerTest`            | Standalone MockMvc / Stateless logic | Instant (<100ms)          |
| `*RestControllerIntegrationTest` | Web Context + Testcontainers DB      | Full integration (~5-15s) |
| `*RestControllerSecurityTest`    | Security filter & role enforcement   | Full integration (~5-15s) |

### Command to Run Tests

```bash
mvn test -Dtest=ReflexRuleIdFilterRestControllerTest -Dsurefire.failIfNoSpecifiedTests=false
```
