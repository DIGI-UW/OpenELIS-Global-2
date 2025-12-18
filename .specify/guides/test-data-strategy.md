# Test Data Strategy Guide

**Audience**: AI Agents and Human Developers  
**Purpose**: Define the canonical test data/fixture approach across unit,
integration, and E2E tests in OpenELIS Global 2.

## Canonical sources of truth

- **Unified fixture loader (canonical entry point)**:
  - Script: `src/test/resources/load-test-fixtures.sh`
  - Overview: `src/test/resources/FIXTURE_LOADER_README.md`
- **DBUnit datasets** (for DB-backed tests and E2E baseline data):
  - Location: `src/test/resources/testdata/*.xml`
  - Loader: `org.openelisglobal.testutils.DbUnitFixtureLoader` (invoked by
    `load-test-fixtures.sh`)

## Principles

- **Prefer real data paths**: If you are validating persistence/integration
  behavior, use real backend + DB fixtures rather than stubbing network
  responses.
- **Separate “fixtures” from “test-created” data**:
  - Fixtures: stable baseline rows required for many tests.
  - Test-created: rows created during a test run; must be cleaned up (or created
    with safe prefixes/ID ranges).
- **Use the same baseline across test types**:
  - Manual testing, Cypress E2E, and backend integration tests should share the
    same fixture loader where possible.

## When to use what

- **Unit tests**:

  - Use builders/factories (in-memory objects).
  - Mock external dependencies as needed.

- **Backend integration tests**:

  - Prefer DBUnit datasets for setup and deterministic cleanup via
    `executeDataSetWithStateManagement("testdata/<file>.xml")`.
  - If inserting rows outside DBUnit, perform targeted cleanup in `@After`.

- **Cypress E2E tests**:
  - Prefer the unified fixture loader via `cy.loadStorageFixtures()` (see
    `e2e-fixtures-readme.md`).
  - Prefer API-based setup (`cy.request()`) for per-test setup that must be
    unique.

## Related docs

- `.specify/guides/testing-roadmap.md` (authoritative testing guide)
- `.specify/guides/e2e-fixtures-readme.md` (Cypress fixture usage quick
  reference)
