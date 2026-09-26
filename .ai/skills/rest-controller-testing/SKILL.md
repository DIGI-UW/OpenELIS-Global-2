---
name: rest-controller-testing
description: >
  REST controller testing skill for OpenELIS Global 2. Covers both standalone
  MockMvc controller unit tests (no database or DBUnit dataset overhead, <100ms
  execution) and full web context integration tests (BaseWebContextSensitiveTest
  with Testcontainers DB and session auth). Use when writing tests for
  @RestController classes, request validation, routing, HTTP status code
  response shaping, or security checks.
---

# Skill: REST Controller Testing

## Context

Controller tests in OpenELIS Global 2 verify HTTP behaviour end-to-end: routing,
request deserialization, validation, security, response shape, and status codes.

There are **two key patterns** for testing controllers in this repository:

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
- Requires session authentication (`MockHttpSession` with `UserSessionData`).
- Seeds data via `JdbcTemplate` or
  `executeDataSetWithStateManagement("testdata/xxx.xml")`.
- Use when testing full-stack database interactions, transactional REST
  behavior, or end-to-end security slices.

## Trigger

- User asks to "write a test" or "add tests" for a `*RestController`.
- User asks for controller tests that don't touch the database or don't use
  datasets.
- User mentions testing HTTP status codes, request validation, or JSON response
  structure on a REST endpoint.
- User asks to reproduce a regression that manifests as a wrong HTTP response or
  status (e.g. 500 instead of 409, 400 instead of 200).

## Behavior

1. **Select the Right Pattern**:

   - If the controller delegates logic to services and does not require database
     state, use **Pattern 1 (Standalone MockMvc)**.
   - If full Spring context integration, session authority, or DB verification
     is required, use **Pattern 2 (Web Context)**.

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

3. **For Standalone Controller Tests**:

   - Annotate class with `@RunWith(MockitoJUnitRunner.class)`.
   - Mock service dependencies using `@Mock`.
   - Inject dependencies into controller using `@InjectMocks` (or constructor).
   - In `@Before setUp()`, instantiate `MockMvc` using
     `MockMvcBuilders.standaloneSetup(controller).build()`.
   - Stub service responses with `when(service.method()).thenReturn(...)`.

4. **For Web Context Integration Tests**:

   - Extend `BaseWebContextSensitiveTest` (omit `@RunWith` as base class
     declares it).
   - In `@Before setUp()`, call `super.setUp()` and build a `MockHttpSession`
     carrying a `UserSessionData` with `usd.setSytemUserId(...)`.
   - Seed DB data using `JdbcTemplate` (preferred for small sets) or
     `executeDataSetWithStateManagement`. Always clean up in `@After`.

5. **Naming Conventions**:

   - `*RestControllerTest` — Standalone MockMvc unit test or stateless test.
   - `*RestControllerIntegrationTest` — Full web context test with database
     seeding/reading.
   - `*RestControllerSecurityTest` — Authorization/role security check test.

6. **Run and Verify**:
   - Run `mvn test -Dtest=<NewTest> -Dsurefire.failIfNoSpecifiedTests=false` to
     verify before submitting.

## Reference & Examples

- [Overview & Detailed Patterns](reference/overview.md) — deep dive into
  standalone setup, session setup, data strategy, and assertions.
- [Template: Standalone MockMvc (No DB)](templates/standalone-controller-test.java.template)
- [Template: Web Context Integration (With DB)](templates/rest-controller-integration-test.java.template)
- [Example: Standalone Reflex Rule Filter](examples/standalone/ReflexRuleIdFilterRestControllerTest.java)
  — pure MockMvc + Mockito standalone test with strong property assertions.
- [Example: Standalone Analyzer Connection Probe](examples/standalone/AnalyzerConnectionProbeRestControllerTest.java)
  — standalone MockMvc test asserting structured JSON responses.
- [Example: Web Context Menu Controller](examples/integration/MenuRestControllerTest.java)
  — full context test using strong MockMvc fluent jsonPath property assertions.
