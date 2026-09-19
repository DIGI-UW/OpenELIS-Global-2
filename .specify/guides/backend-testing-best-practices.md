# Backend Testing Best Practices Quick Reference

Use this guide to choose a test's scope and data ownership. Detailed rules and
examples belong in the [testing roadmap](testing-roadmap.md#backend-testing).
OpenELIS uses Java 21, JUnit 4, and traditional Spring MVC. Do not introduce Spring
Boot test slices.

## Choose the test level

| Level       | What it proves                                                                     | Typical setup                                                                 |
| ----------- | ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Unit        | One business rule or transformation                                                | JUnit 4 and Mockito; no Spring or database                                    |
| Component   | A controller's HTTP behavior or a UI component's interaction                       | Standalone MockMvc or React Testing Library; controlled boundary              |
| Integration | Actual service wiring, database behavior, history, filters, or transport contracts | Real relevant internal services; PostgreSQL for persistence claims            |
| End-to-end  | An assembled workflow across the browser and running components                    | Identified component revisions, real transport, and inspected stored outcomes |

Permissions, concurrency, history, and migration checks describe the behavior
under test; they do not create additional levels. Human acceptance is separate.
Retain the required fast ORM mapping validation without a database.

## Controller component tests

Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` collaborators, `@InjectMocks`,
and `MockMvcBuilders.standaloneSetup(controller)`. Register real advice or
converters when the endpoint behavior requires them. Assert actual status,
response values, and invalid-input behavior.

A standalone controller check does not prove deployed permissions, persistence,
or application wiring. Cover those requirements with integration or end-to-end
tests. Do not start a database merely to check JSON shape.

The [controller template](../templates/testing/WebMvcTestController.java.template)
retains its historical filename but uses standalone MockMvc, not a Boot slice.

## Database integration tests

Use real DAO/service beans from the existing traditional Spring test
configuration. `BaseWebContextSensitiveTest` is the existing database-backed base;
it is not required for unit or component tests.

- Create only the data the scenario needs, with clear ownership.
- Call real application services for the business transition being asserted.
- Flush and clear the persistence context before asserting a reload.
- For queries, create matching and nonmatching rows and assert exact membership
  and ordering. A non-null list or maximum-size check also accepts an empty query.
- Assert persisted history where the behavior requires it. Do not install a
  no-op recorder or repair a service dependency in each test.
- Control remote dependencies at the transport boundary. Identify which external
  behavior the test does not establish.

The [DAO template](../templates/testing/DataJpaTestDao.java.template) uses the
existing Spring base and a real `EntityManager`; its filename is historical.

## Test data management

Use builders when they reduce repeated setup; direct construction is also
appropriate for small, clear fixtures. Neither technique establishes isolation
without explicit transaction ownership.

Existing DBUnit XML datasets live in `src/test/resources/testdata/`. Load them via
`executeDataSetWithStateManagement("testdata/example.xml")`. This helper uses
cascading truncation: dependent tables not named in the XML can be affected.
Do not use fixture SQL to stand in for mapping save, confirmation, adoption, or
another application transition under test.

## Transaction management

For synchronous query and persistence checks, put `@Transactional` on the concrete
class. The fixture loader and cleanup helper share that transaction, and Spring
rolls it back after the test. Flush/reload proves a database write without needing
`@Rollback(false)`.

For concurrent workers, commit-time actions, rollback boundaries, or independent
requests, use explicitly committed setup and read outcomes in a fresh transaction.
Record the exact created IDs and clean up only those records, including after
failure. An outer test transaction can hide the behavior these tests need to prove.

The base class retains `Propagation.NOT_SUPPORTED` for legacy callers. Without a
test transaction, each fixture load or cleanup call commits its own atomic
transaction. It does not restore those records after the test. Do not infer safe
cleanup from DBUnit usage, guessed ID ranges, or a `TEST-` prefix alone.

See the [transaction rules](testing-roadmap.md#transaction-management) for the
shared helper's cache lifecycle and the distinction between rollback and commit.

## Iteration and validation

1. Reproduce a meaningful failure or demonstrate that an assertion accepts broken
   behavior.
2. Correct the cause and remove the superseded workaround.
3. Run the focused tests and inspect stored outcomes.
4. If shared setup changed, run all affected callers together and in a different
   class order. Verify the executed class list rather than relying on a wildcard.
5. Run the applicable build and CI checks on the final committed revision.

Name tests for their behavior and mirror the feature package under
`src/test/java/org/openelisglobal/`. Record test level and evidence limits in the
feature catalogue. Keep coverage and milestone requirements from the constitution
and testing roadmap; a passing test count is not a substitute for acceptance.
