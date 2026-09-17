# Test Data Strategy Guide

Choose setup and cleanup according to the behavior and boundary under test.
The shared sources are the existing test code, `BaseWebContextSensitiveTest`,
`BaseTestConfig`, and the registered browser projects.

## Unit and component tests

Use in-memory objects or existing builders. Mock collaborators outside the
behavior being tested. A component test with mocked backend responses does not
prove persistence or an end-to-end workflow.

## Backend database integration tests

Create only the initial state the test needs, using owned records and real
services where appropriate. Existing DBUnit XML under
`src/test/resources/testdata/` may be loaded through
`executeDataSetWithStateManagement("testdata/<file>.xml")`. Exercise the real
internal services and DAOs for the behavior under test; inspect `AppTestConfig`
for substitutions that would weaken that claim.

- The shared base defaults to `Propagation.NOT_SUPPORTED`. Add explicit Spring
  `@Transactional` for tests whose setup and assertions can share a rollback
  transaction.
- Fixture loading and `cleanRowsInCurrentConnection` join an active Spring
  transaction. Without one, each operation owns an atomic committed operation.
- Truncation uses `RESTART IDENTITY CASCADE`. It can affect dependent tables not
  listed in the XML; a fixture file is not a complete cleanup boundary.
- Commit-time behavior, concurrency, and independent connections require
  committed setup. Name the data and affected tables the test owns, clean up
  after failures as well as success, and do not run destructive fixture loaders
  concurrently against shared tables.
- Follow the helper's protected seed and cache lifecycle. Consult the current
  `PROTECTED_SEED_TABLES` and explicit sequence mappings rather than assuming
  every table/sequence is restored automatically.
- Flush and clear the persistence context before database rereads where needed
  to distinguish persisted state from cached entities. Check exact changed
  fields, required history, and relevant negative outcomes.

A fixture reset before a test is not evidence that the test cleans up afterward.

## Browser and manual testing

Start the local worktree environment with `scripts/dev-stack up`. Feature setup
uses property-gated application scenario services and supported application
APIs. Export `scripts/dev-stack env` before invoking frontend Playwright scripts,
or use `scripts/dev-stack playwright` with one registered project/spec.

Existing CI jobs still call `src/test/resources/load-test-fixtures.sh`; this is
CI infrastructure, not a universal local feature-setup command. Do not copy its
SQL reset path into new tests or local setup instructions.

Use Playwright for new end-to-end tests and maintain existing Cypress tests as
needed. Keep the real backend and database for end-to-end claims. Choose setup
that follows the registered project's rules: demo scenarios have stricter
UI-only constraints than foundational tests. See
[`frontend/playwright/README.md`](../../frontend/playwright/README.md).

## Related guidance

- [Testing Roadmap](testing-roadmap.md)
- [Backend Testing Best Practices](backend-testing-best-practices.md)
- [Playwright Best Practices](playwright-best-practices.md)
