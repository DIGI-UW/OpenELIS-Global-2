---
name: rest-controller-testing
description: >
  REST controller testing skill for OpenELIS Global 2. Covers standalone MockMvc
  controller unit tests (<100ms execution), web context integration tests
  (BaseWebContextSensitiveTest with Testcontainers DB and session auth), and
  security slice tests (SecuritySliceMockMvcTest with Spring Security filters).
  Use when writing tests for @RestController classes, request validation,
  routing, HTTP status code response shaping, or role-based security checks.
---

# Skill: REST Controller Testing

## Context

Controller tests in OpenELIS Global 2 verify HTTP behaviour end-to-end: routing,
request deserialization, validation, security, response shape, and status codes.

There are **three key patterns** for testing controllers in this repository:

### Pattern 1: Standalone Controller Unit Tests (No Database / No Datasets) — _Fastest & Preferred when DB state is not required_

- Uses `@RunWith(MockitoJUnitRunner.class)`, `@Mock` services, and
  `@InjectMocks`.
- Sets up MockMvc with `MockMvcBuilders.standaloneSetup(controller).build()`.
- Requires **no DBUnit, no SQL datasets, and no Spring Web Context loading**.
- Runs in **<100ms**.
- Use when the controller delegates database operations to service interfaces,
  transforms payloads, validates request parameters, or formats HTTP responses.

### Pattern 2: Web Context Controller Integration Tests (With Database / Datasets)

- Extends `BaseWebContextSensitiveTest` (which injects `super.mockMvc` wired to
  full Spring context + Testcontainers DB).
- **Authentication & Security Filter Boundary**:
  - `BaseWebContextSensitiveTest` is annotated with `@WithDaemonUser` (initially
    supplying a daemon token), but `setDefaultTestAuthentication()` subsequently
    installs `admin` with `ROLE_ADMIN` and `ROLE_RESULTS` in `@Before` into
    `SecurityContextHolder`.
  - Its built-in `super.mockMvc` is initialized without
    `.apply(springSecurity())`, so the Spring Security filter chain is **not**
    registered.
  - Adding `UserSessionData` via
    `session.setAttribute(IActionConstants.USER_SESSION_DATA, usd)` supplies
    **legacy OpenELIS controller session data** (e.g. `systemUserId`); it does
    **not** select the authenticated Spring principal and **cannot** prove
    anonymous or role-restricted security enforcement.
- Seeds data via `JdbcTemplate` or
  `executeDataSetWithStateManagement("testdata/xxx.xml")`.
- Use when testing full-stack database interactions, transactional REST
  behavior, or DB persistence round-trips.

### Pattern 3: Security Slice Tests (Security Filters & Role Enforcement)

- Extends `SecuritySliceMockMvcTest` (which calls
  `MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build()`).
- Tests of the security filters **must actually install them**.
- Verifies Spring Security filter chain and method-level `@PreAuthorize`
  authorization across all three canonical states:
  1. **Anonymous / Unauthenticated**: Unauthenticated requests return
     `401 Unauthorized`.
  2. **Denied Role**: Authenticated users lacking the required role return
     `403 Forbidden`.
  3. **Permitted Role**: Users with the required role clear the auth gate
     (`200 OK`, or expected downstream status like `404 Not Found`).
- Use when testing endpoint access controls, role restrictions (e.g. `ADMIN`,
  `RESULTS`, `VALIDATION`), or `@PreAuthorize` rules.

## Trigger

- User asks to "write a test" or "add tests" for a `*RestController`.
- User asks for controller tests that don't touch the database or don't use
  datasets.
- User asks to test authorization, security filters, or role permissions on a
  REST controller.
- User mentions testing HTTP status codes, request validation, or JSON response
  structure on a REST endpoint.
- User asks to reproduce a regression that manifests as a wrong HTTP response or
  status (e.g. 500 instead of 409, 400 instead of 200, 403 instead of 401).

## Behavior

1. **Select the Right Pattern**:

   - If testing business logic delegation, request parsing, parameter filtering,
     or response status codes without DB, use **Pattern 1 (Standalone
     MockMvc)**.
   - If testing real database persistence, entity queries, or transactions, use
     **Pattern 2 (Web Context Integration)**.
   - If testing security filters, anonymous rejection, or role-based access
     control, use **Pattern 3 (Security Slice)**.

2. **Strict Assertion Policy — Strong Assertions ONLY**:

   - **PROHIBITED (Weak Assertions)**: Do NOT use `assertNotNull`, `assertNull`,
     `assertFalse(list.isEmpty())`, `assertTrue(list.size() > 0)`, or generic
     `.andExpect(jsonPath("$.field").exists())` as primary assertions.
   - **MANDATORY (Strong Assertions)**: Assert exact expected values for every
     field, status code, array length, and property.
   - Use MockMvc `jsonPath("$.property").value(expectedValue)` to assert exact
     strings, booleans, numbers, and enum codes.
   - Use `jsonPath("$.length()").value(expectedSize)` to assert exact collection
     sizes.

3. **For Standalone Controller Tests (Pattern 1)**:

   - Annotate class with `@RunWith(MockitoJUnitRunner.class)`.
   - Mock service dependencies using `@Mock`.
   - Inject dependencies into controller using `@InjectMocks` (or constructor).
   - In `@Before setUp()`, instantiate `MockMvc` using
     `MockMvcBuilders.standaloneSetup(controller).build()`.
   - Stub service responses with `when(service.method()).thenReturn(...)`.

4. **For Web Context Integration Tests (Pattern 2)**:

   - Extend `BaseWebContextSensitiveTest` (omit `@RunWith` as base class
     declares it).
   - Recognize that `super.mockMvc` bypasses security filters and has an admin
     token pre-installed.
   - In `@Before setUp()`, call `super.setUp()` and build a `MockHttpSession`
     carrying a `UserSessionData` with `usd.setSytemUserId(...)` to satisfy
     legacy controller session lookups.
   - Seed DB data using `JdbcTemplate` (preferred for small sets) or
     `executeDataSetWithStateManagement`. Always clean up in `@After`.

5. **For Security Slice Tests (Pattern 3)**:

   - Extend `SecuritySliceMockMvcTest` with `@WebAppConfiguration` and a focused
     `@ContextConfiguration(classes = { ...TestConfig.class })`.
   - In `TestConfig`, enable `@EnableWebMvc`, `@EnableWebSecurity`, and
     `@EnableMethodSecurity(prePostEnabled = true)`, and define a
     `SecurityFilterChain` bean.
   - Test all three security cases using
     `SecurityMockMvcRequestPostProcessors.user(...)`:
     - **Anonymous**: `mockMvc.perform(get("/rest/..."))` $\to$
       `.andExpect(status().isUnauthorized())`
     - **Denied Role**:
       `mockMvc.perform(get("/rest/...").with(user("user").roles("RESULTS")))`
       $\to$ `.andExpect(status().isForbidden())`
     - **Permitted Role**:
       `mockMvc.perform(get("/rest/...").with(user("admin").roles("ADMIN")))`
       $\to$ `.andExpect(status().isOk())`
   - If the controller additionally queries `UserSessionData`, supply it via
     `.sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData)` alongside
     `.with(user(...))`.

6. **Naming Conventions**:

   - `*RestControllerTest` — Standalone MockMvc unit test.
   - `*RestControllerIntegrationTest` — Full web context test with database
     seeding/reading.
   - `*RestControllerSecurityTest` — Security filter and role enforcement slice
     test.

7. **Run and Verify**:
   - Run `mvn test -Dtest=<NewTest> -Dsurefire.failIfNoSpecifiedTests=false` to
     verify before submitting.

## Reference & Examples

- [Overview & Detailed Patterns](reference/overview.md) — deep dive into
  standalone setup, session vs. security authentication, security slices, and
  assertions.
- [Template: Standalone MockMvc (No DB)](templates/standalone-controller-test.java.template)
- [Template: Web Context Integration (With DB)](templates/rest-controller-integration-test.java.template)
- [Template: Security Slice (With Security Filters)](templates/rest-controller-security-test.java.template)
- [Example: Standalone Reflex Rule Filter](examples/standalone/ReflexRuleIdFilterRestControllerTest.java)
  — pure MockMvc + Mockito standalone test with strong property assertions.
- [Example: Standalone Analyzer Connection Probe](examples/standalone/AnalyzerConnectionProbeRestControllerTest.java)
  — standalone MockMvc test asserting structured JSON responses.
- [Example: Web Context Menu Controller](examples/integration/MenuRestControllerTest.java)
  — full context test using strong MockMvc fluent jsonPath property assertions.
- [Codebase Security Example: SampleTypeManagementRestControllerSecurityTest](file:///home/coder/Documents/OpenELIS-Global-2/src/test/java/org/openelisglobal/common/management/controller/rest/SampleTypeManagementRestControllerSecurityTest.java)
  — tested slice test asserting 401 unauthenticated, 403 denied role, and admin
  access.
- [Codebase Security Example: AlertRestControllerSecurityTest](file:///home/coder/Documents/OpenELIS-Global-2/src/test/java/org/openelisglobal/alert/controller/rest/AlertRestControllerSecurityTest.java)
  — tested slice test showing role boundaries and session data interaction.
