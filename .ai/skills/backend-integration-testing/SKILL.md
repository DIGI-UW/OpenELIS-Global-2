---
name: backend-integration-testing
description:
  Backend integration testing skill for OpenELIS Global 2. Provides guidance on
  writing and running integration tests using Testcontainers, DBUnit, and the
  5-layer architecture. Use when writing new integration tests, refactoring
  backend logic, or validating service layers.
---

# Skill: Backend Integration Testing

## Context

This skill provides authoritative guidance on writing and running backend
integration tests in OpenELIS Global 2. It enforces the use of Testcontainers,
DBUnit, and the 5-layer architecture standards.

## Trigger

- When a user asks to "write a test" for a backend service.
- When a user asks "how to run integration tests."
- When refactoring backend logic that requires regression testing.

## Behavior

1. **Choose the boundary**: For service/database integration, use
   `BaseWebContextSensitiveTest` with the relevant real internal services and
   DAOs. Unit/component tests may use mocks and a smaller context; do not force
   every backend test into the full application context.
2. **Choose transaction ownership** before setup. Ordinary rollback tests must
   explicitly add Spring `@Transactional`; the base defaults to
   `Propagation.NOT_SUPPORTED`. Commit/concurrency tests need committed setup
   and explicit cleanup of all affected data.
3. **Create owned initial data** with real services where appropriate. If a
   DBUnit dataset is needed, load it with `executeDataSetWithStateManagement`
   in `@Before`.
   Fixture loading and cleanup join an active transaction, otherwise commit
   their own operation. Truncation uses `CASCADE` beyond XML-listed tables.
4. **Inspect shared substitutions** in `AppTestConfig`. Isolate external effects
   without mocking the internal path under test; do not remove mocks globally.
5. **Verify the specific test** with `mvn test -Dtest=<NewTest>` using Java 21
   and Docker for database tests.
6. **Assert behavior**: exact changed state, values, filtering, required history,
   negative and boundary cases. Null/empty assertions are valid when absence is
   the expected outcome, not a substitute for checking the behavior under test.

## Reference

- [Overview](reference/overview.md) - Detailed infrastructure and patterns.
- [Template](templates/integration-test-template.java.template) - Boilerplate
  for new tests.
- [Example request](examples/menu-service/request.md) with its complete
  [generated Java test](examples/menu-service/GeneratedMenuServiceIntegrationTest.java),
  grounded in the OpenELIS `MenuService` and its existing DBUnit fixture.
