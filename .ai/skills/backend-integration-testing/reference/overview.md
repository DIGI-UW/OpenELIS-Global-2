# OpenELIS Global 2: Backend Integration Testing Guide

Service integration tests exercise real relevant services and data access
against PostgreSQL. The test name or a full Spring context alone does not
establish that coverage.

## Infrastructure and boundary

- Java 21, JUnit 4, Mockito, and traditional Spring Test; no Spring Boot test
  slices, `@MockBean`, or `TestEntityManager`.
- `BaseWebContextSensitiveTest` loads `BaseTestConfig` and `AppTestConfig` with
  the `test` profile. It provides MockMvc and a default authenticated principal.
- `BaseTestConfig` starts PostgreSQL 14.4 through Testcontainers, applies
  Liquibase, and configures JPA/Hibernate against the shared test data source.
- Unit tests can use Mockito without Spring. Controller component tests can use
  standalone MockMvc or focused Spring configuration. Use the shared full
  context when the integration behavior requires it.
- Permissions, history, and concurrency are dimensions of these tests, not
  separate test layers. Human acceptance is separate from automated results.

## Substitutions and external systems

Inspect `AppTestConfig` before claiming an integration path is real. It contains
both external-boundary replacements and internal substitutions. Keep the
relevant internal services, domain logic, DAOs, and database real; use a focused
configuration when a shared replacement would hide the behavior being tested. Do
not remove every mock indiscriminately.

Isolate external network effects. For example, the shared `FhirContext` is real
so it parses real messages; its HTTP transport is mocked. Mocking the parser
would remove that coverage. Test-only beans belong under test sources and the
`test` profile. The shared context excludes production security configuration;
its default principal does not prove production authorization behavior.

## Fixture and transaction ownership

DBUnit Flat XML datasets live in `src/test/resources/testdata/`. Use bare table
names and load with `executeDataSetWithStateManagement` in `@Before`.

The base class defaults to `Propagation.NOT_SUPPORTED`. For ordinary service and
persistence tests, explicitly add Spring `@Transactional` to join fixture setup
and application writes in one rollback transaction. The bundled template uses
this pattern. It is not appropriate for every integration test:

- For concurrent workers, independent connections, commit-time behavior, or
  production transaction boundaries, use committed setup and explicit cleanup.
- Fixture loading and `cleanRowsInCurrentConnection` join an active Spring
  transaction; without one, each operation owns an atomic committed operation.
- Truncation uses `RESTART IDENTITY CASCADE` and can clear dependent tables not
  named in the XML. Identify affected data before committed setup/cleanup and
  avoid concurrent destructive loads against shared tables.
- Do not add fixture rows for the protected seed tables listed in
  `PROTECTED_SEED_TABLES` (`reference_tables`, `requester_type`, `label_preset`,
  `observation_history_type`). Filtering their names is not a general guarantee
  against indirect cascade effects.
- Use the helper's cache refresh lifecycle. Explicit fixture IDs must agree with
  the real seed and foreign keys; an ID range alone does not prevent collisions.
  The loader resynchronizes only its explicit `FIXTURE_SEQUENCE_MAPPINGS`.
- For audit-emitting operations, provide the required user identity. After
  fixtures replace `system_user`, ensure the principal matches the fixture user;
  do not assume admin ID 1 is universally restored inside a rollback
  transaction.

See
[Backend Testing Best Practices](../../../../.specify/guides/backend-testing-best-practices.md)
for rollback and committed patterns. The bundled MenuService example illustrates
fixture assertions; existing test names do not define the target taxonomy.

## Assertions and execution

Assert real outcomes: exact returned values and filtering, persisted fields,
required history, rejected inputs, and unchanged state after rejection. Null or
empty assertions are appropriate when absence is the expected behavior; they
must not replace stronger assertions about the behavior being claimed. Flush and
clear before persisted-state rereads when needed.

With Java 21 and Docker available, run one class:

```bash
mvn test -Dtest=MyFeatureIntegrationTest
```

Surefire runs both unit and integration classes. The root POM has no separate
`integration` profile or Failsafe configuration. `mvn test` runs the broader
backend suite. To skip compilation and execution for a development build, use
`mvn clean install -DskipTests -Dmaven.test.skip=true`.

On fixture failure, inspect the constraint, affected tables, and transaction
ownership rather than adding another destructive reset. On lazy loading errors,
check the service's returned-data contract; do not blanket-enable eager loading
or a test transaction to hide a production failure.

The complete mandatory assertion rules remain in
[Test Quality Invariants](../../../../.specify/guides/testing-roadmap.md#test-quality-invariants-constitution-v6).
