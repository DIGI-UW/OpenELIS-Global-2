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

1. **Always inherit** from `BaseWebContextSensitiveTest`.
2. **Always seed data** using `executeDataSetWithStateManagement` in the
   `@Before` block.
3. **Always use real services/DAOs** where possible; mock only external systems
   (Odoo, FHIR, Mail).
4. **Follow the verified example**: Use
   `examples/menu-service/GeneratedMenuServiceIntegrationTest.java` as the
   blueprint.
5. **Verify with test target**: Run `mvn test -Dtest=<NewTest>` to verify the
   newly added or modified test specifically.
6. **Harden assertions**: Avoid weak assertions like `assertNotNull` or
   `assertNull`. Assert aggressively on exact values, specific sizes, and field
   states to guarantee correct business logic outcomes.

## Reference

- [Overview](reference/overview.md) - Detailed infrastructure and patterns.
- [Template](templates/integration-test-template.java.template) - Boilerplate
  for new tests.
- [Example request](examples/menu-service/request.md) with its complete
  [generated Java test](examples/menu-service/GeneratedMenuServiceIntegrationTest.java),
  grounded in the OpenELIS `MenuService` and its existing DBUnit fixture.
