# Quickstart: Microbiology MVP Workflow

## Purpose

Use this guide to continue the MVP implementation without re-reading every
source artifact from scratch.

## Preconditions

1. Use Java 21.
2. Use the existing PR #3789 delivery branch; M1-M7 are validation blocks in
   that PR, not separate implementation branches.
3. Read these files first:
   - `specs/782-ogc-782-microbiology-mvp-spec/spec.md`
   - `specs/782-ogc-782-microbiology-mvp-spec/plan.md`
   - `specs/782-ogc-782-microbiology-mvp-spec/research.md`
   - `specs/782-ogc-782-microbiology-mvp-spec/data-model.md`
   - `specs/roadmaps/analyzer-microbiology-engineering-crosswalk.md`
   - `specs/roadmaps/microbiology-spec-health-cleanup-list.md`

## Code QA Skills

The repository pins `DIGI-UW/code-qa` at `tools/code-qa`. Initialize that
existing submodule; do not add a second copy or change `.gitmodules`:

```bash
git submodule update --init tools/code-qa
```

Make the local skill installer scan `tools/code-qa/skills`, or otherwise make
these skills available to the implementing agent:

- `meaningful-test-coverage`
- `spec-code-alignment`
- `simplicity-review`
- `evidence-bundle`

The final MVP acceptance gate in `tasks.md` requires these workflows before
marking the implementation complete.

## Work In The Delivery PR

Use PR #3789 on
`feat/782-ogc-782-microbiology-mvp-m7-release-surveillance-readiness`, based on
`spec/782-ogc-782-microbiology-mvp-spec`. Complete and validate M1-M7 in order
inside that PR. Post-MVP remediation uses separate coherent PRs stacked in
sequence above #3789.

M1 should implement only the catalog/reference foundation:

- Culture workflow routing configuration for tests.
- Minimal organism and antibiotic reference data.
- Minimal breakpoint standard/rule support.
- Culture setup metadata tied to existing Method/TestMethod behavior.
- Migration rollback and ORM validation.

Do not implement the case workbench, AST entry UI, worklist, or WHONET readiness
in M1.

## Repo Patterns To Follow

- Backend module path:
  `src/main/java/org/openelisglobal/microbiology/`
- Traditional layers:
  `valueholder`, `dao`, `daoimpl`, `service`, `controller`, `form`
- Tests:
  `src/test/java/org/openelisglobal/microbiology/`
- Liquibase:
  `src/main/resources/liquibase/3.5.x.x/`
- Frontend:
  `frontend/src/components/microbiology/`
- User-facing strings:
  `frontend/src/languages/en.json`

## Validation Commands

Use focused checks during implementation:

```bash
java -version
mvn -q -DskipTests -Dmaven.test.skip=true install
mvn -q -Dtest='*Microbiology*Test' test
```

For frontend slices:

```bash
cd frontend
npm test -- --runInBand Microbiology
npm run format
```

Before E2E authoring, run the project Playwright planning workflow:

```text
/plan-record-playwright
```

## Required Validation By Milestone

- M1: Liquibase update/rollback, ORM validation, reference lookup tests.
- M2: Case identity uniqueness and sibling workflow integration tests.
- M3: Order routing integration tests for non-micro, bacteriology, and sibling
  bacteriology/TB orders.
- M4: REST and React case workbench tests.
- M5: Manual AST interpretation, override, repeat/retest, and review tests.
- M6: Worklist sorting/filtering, critical communication, and alert surfacing
  tests.
- M7: Release readiness and WHONET readiness tests.

## Guardrails

- Do not add schema/API/service details to Casey-facing tickets as product
  requirements.
- Do not create a second alerts dashboard.
- Do not create a parallel WHONET exporter unless the existing service path is
  proven unfit and documented.
- Do not make TB/mycobacteriology usable in MVP unless an explicit TB slice is
  opened.
- Do not place transactions in controllers.
- Do not add non-English locale files; Transifex owns translations.
