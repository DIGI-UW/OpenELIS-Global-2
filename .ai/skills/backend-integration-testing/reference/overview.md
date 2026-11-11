# OpenELIS Global 2: Backend Integration Testing Guide

This document defines the mandatory patterns and infrastructure for writing and
running service-layer integration tests in OpenELIS Global 2. These tests call
real services and data-access code against a temporary PostgreSQL database.

## 1. Core Technology Stack

- **JUnit 4**: The standard test runner (NOT JUnit 5).
- **Spring Test**: `@RunWith(SpringRunner.class)` with `@ContextConfiguration`.
- **Testcontainers**: Spawns a PostgreSQL 14.4 container for database isolation.
- **DBUnit**: Loads XML flat datasets for data seeding.
- **Liquibase**: Applies schema migrations to the test container.
- **Mockito**: For mocking external dependencies (FHIR, Odoo, etc.).

## 2. Mandatory Base Classes

### `BaseWebContextSensitiveTest`

All service integration tests MUST extend this class. It provides:

- Full Spring Web context loading.
- Database connection management via Testcontainers.
- `executeDataSetWithStateManagement(String xmlPath)`: Truncates relevant tables
  and loads XML data.
- Default authentication: `ROLE_ADMIN` and `ROLE_RESULTS`.

## 3. Configuration Classes

### `BaseTestConfig`

Handles the infrastructure layer:

- Starts `PostgreSQLContainer`.
- Configures `DataSource` pointing to the container.
- Runs `SpringLiquibase` to initialize the schema.
- Sets up `LocalContainerEntityManagerFactoryBean` (JPA/Hibernate).

### `AppTestConfig`

Loads the application services and test-only mocks for external systems so the
test cannot make network calls.

## 4. External System Isolation

The following services are **MANDATORY** to mock in `AppTestConfig` to prevent
external side effects:

- **Odoo**: `OdooClient`, `OdooConnection` (Prevents ERP calls).
- **FHIR**: `FhirUtil`, `FhirConfig`, `FhirContext` (Prevents HAPI FHIR server
  calls).
- **Communication**: `JavaMailSender`, `OzekiMessageOutService` (Prevents real
  emails/SMS).
- **Security**: `TruststoreService`, `TextEncryptor` (Prevents real
  certificate/encryption logic).

### The `test` Profile

All test-specific mocks must be annotated with `@Profile("test")`. This ensures
they never accidentally leak into production code.

## 5. Writing an Integration Test

### Step 1: Create the Test Class

Naming convention: `*IntegrationTest.java` or `*Test.java` (if it extends the
base class).

Use the bundled
[MenuService example](../examples/menu-service/GeneratedMenuServiceIntegrationTest.java)
as the working reference. It compiles against the OpenELIS classes, loads the
existing `testdata/menu.xml` fixture, and checks the exact active menu rows.

### Step 2: Create the Dataset (XML)

DBUnit XML datasets are stored in `src/test/resources/testdata/` (note:
`src/test/resources/fixtures/` holds raw SQL, not DBUnit XML). Use DBUnit Flat
XML format with bare table names (do NOT prefix with `clinlims.`; the database
schema prefix is handled automatically by the loader).

```xml
<dataset>
    <organization id="100" name="Test Lab" ... />
    <system_user id="100" login_name="tester" ... />
</dataset>
```

#### Hardened Loader Invariants & Constraints:

- **Never declare protected seed tables**: Do not add `reference_tables`,
  `requester_type`, or `label_preset` rows to a fixture. The loader preserves
  the Liquibase-seeded contents of all three tables.
- **System User ID 1**: The user `system_user` with `id=1` is a protected seed
  and is automatically re-seeded after every load.
- **Use IDs >= 100**: Always use IDs >= 100 for rows in tables that the
  test/application might insert into, to prevent primary key/ID conflicts with
  seed data.
- **Sequence Resync**: When using explicit IDs in fixtures, use the
  `resyncSequence(sequence, table)` helper to sync PostgreSQL sequences and
  avoid duplicate key exceptions on subsequent inserts.

## 6. Running Tests

### Via Maven

- **All tests**: `mvn test`
- **Specific test**: `mvn test -Dtest=MyFeatureIntegrationTest`
- **Skip all tests**: `mvn clean install -DskipTests -Dmaven.test.skip=true`
  (MANDATORY flags for skipping).

## 7. Best Practices

- **Transactional State**: Use `@Transactional` at the test level to ensure
  rollbacks, or use `executeDataSetWithStateManagement` to manually reset state
  if propagation is not supported.
- **Eager Fetching**: Ensure services used in tests eagerly fetch all required
  data to avoid `LazyInitializationException` outside of service transactions.
- **Mocking**: Only mock external systems or extremely expensive components.
  Prefer real DAOs and Services for integration tests.
- **No Hardcoded IDs**: Prefer using data loaded from fixtures and avoid relying
  on database-generated IDs that might change.
- **Harden Assertions**: Avoid weak assertions (e.g. `assertNotNull`,
  `assertNull`, or check-empty). Assert aggressively on specific values, exact
  list sizes, and field states.

## 8. Debugging & Troubleshooting

To effectively maintain integration tests, the model must recognize and resolve
these common issues:

### A. Common Exceptions

- **`LazyInitializationException`**: Occurs if a test tries to access an entity
  relationship (e.g., `sample.getAnalyses()`) outside of the Service
  transaction.
  - **Fix**: Use `JOIN FETCH` in the DAO or ensure all data is eagerly loaded by
    the Service before it returns to the test.
- **`NoSuchTableException` (DBUnit)**: Occurs if the XML dataset references a
  table that does not exist in the database schema or is misspelled.
  - **Fix**: Verify table spelling against the database schema or JPA Entity. Do
    NOT use the `clinlims.` schema prefix, as the schema prefix is resolved
    automatically by the DBUnit connection loader.
- **`ConstraintViolationException`**: Often caused by stale data or foreign key
  issues.
  - **Fix**: Ensure `executeDataSetWithStateManagement` is called in the
    `@Before` block to truncate and reset tables.

### B. SQL Debugging

If you suspect the wrong data is being queried or updated:

1. Open `src/test/resources/hibernate/test-hibernate.cfg.xml`.
2. Change `<property name="show_sql">false</property>` to `true`.
3. Re-run the test to see the raw SQL in the console.

### C. Testcontainers / Docker Issues

If the database fails to start:

- **Check Docker Status**: Ensure the Docker daemon is running and has enough
  memory (at least 4GB).
- **Inspect Logs**: Look for `PostgreSQLContainer` in the Maven output. It will
  show if the image was pulled and if the port binding succeeded.

### D. Liquibase Failures

If the schema fails to apply:

- Check `src/main/resources/liquibase/base-changelog.xml` (loaded onto the test
  classpath via `BaseTestConfig`) for syntax errors or missing changesets.

## 9. Test Quality Rules

The complete, current rules are maintained in
[Test Quality Invariants](../../../../.specify/guides/testing-roadmap.md#test-quality-invariants-constitution-v6).
Generated tests must follow every applicable backend and universal rule there,
including negative and boundary cases. Do not copy a partial version of those
rules into this skill.
