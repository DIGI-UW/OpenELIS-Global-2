# Quickstart: Microbiology MVP Workflow

## Purpose

Use this guide to continue the MVP implementation without re-reading every
source artifact from scratch.

## Preconditions

1. Use Java 21.
2. Use the owning branch from the official product stack in `tasks.md`; do not
   add another remediation layer.
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

## Work In The Owning Product PR

Use the exact eight-PR order in `tasks.md`. Put a change in the lowest retained
PR that owns the behavior, then restack the branches above it once. Keep generic
development-stack and CI-readiness changes in the independent tooling PR. Keep
review-overlay and Grist-authoring changes in `openelis-review-tooling`.

Before closing a superseded remediation PR, compare its changes with the
retained owner and link the closure to that PR. Reconstructing history must not
change stable microbiology URLs, public contracts, or clinical behavior unless
the product specification changes explicitly.

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
