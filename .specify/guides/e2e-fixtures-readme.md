# E2E Fixtures Quick Reference (Cypress)

**Audience**: Cypress test authors (humans + AI agents)  
**Purpose**: Make the “real backend + real DB” E2E fixture workflow easy and
consistent.

## Canonical fixture loader

- **Unified loader**: `./src/test/resources/load-test-fixtures.sh`
- **Overview**: `src/test/resources/FIXTURE_LOADER_README.md`

This loader is the preferred way to ensure baseline E2E data exists (patients,
samples, storage hierarchy, etc.).

## Cypress usage

### Preferred: use the Cypress helper

Most storage-related Cypress specs should call:

- `cy.loadStorageFixtures()`

This is wired to the unified loader and is designed to be “smart” about skipping
reloads for faster iteration.

### Environment variables

- `CYPRESS_SKIP_FIXTURES=true` – skip loading (assumes fixtures already exist)
- `CYPRESS_FORCE_FIXTURES=true` – force reload even if fixtures exist
- `CYPRESS_CLEANUP_FIXTURES=true` – cleanup after tests (default false)

## Key rule: fixtures are not a substitute for real E2E

In `frontend/cypress/e2e/`, avoid stubbing backend success responses for the
workflow you are validating. Use fixtures/API setup to create baseline data,
then let the real backend handle the requests.
