# Implementation Plan: M8 Clinical Completeness and Qualification

**Branch**: `feat/782-ogc-782-microbiology-m8-clinical-completeness`  
**Stack base**: PR #3789 head `6aafb05a9345525e04a0749e01ba09a3e41b5c2d`  
**Target after MVP merge**: `develop`

## Technical Context

- Java 21, Spring MVC 6.2, Hibernate, PostgreSQL, Liquibase
- JUnit 4, Mockito, Spring integration tests, Testcontainers
- React 17, Carbon, React Intl, Vitest, Playwright
- Existing Microbiology five-layer package structure
- Existing standard Analysis/Result report path
- Existing Test Catalog reagent links and Inventory item/lot/usage services

## Constitution Check

- Configuration-driven behavior: PASS. No lab/country branching.
- Carbon first: PASS. New UI uses installed Carbon primitives/tokens.
- Layered architecture: PASS. Transactions and clinical validation stay in
  services; controllers map requests only.
- TDD: REQUIRED. Each slice begins with service/controller/component/E2E tests.
- Schema management: PASS. Only data-model changes receive Liquibase changesets
  with rollback.
- Internationalization: PASS. New rendered strings go to `en.json` only.
- Security/audit: REQUIRED. Authenticated actor is server-derived and every
  clinical correction is attributable.
- Spec-driven iteration: PASS. M8 is separate from #3789 and divided into
  independently verifiable slices.
- Legacy removal: PASS. Reuse standard report and Inventory paths; no parallel
  result or reagent subsystem.

## Architecture

### Slice A - Amendment and re-identification history

Add three append-oriented concepts:

1. An amendment lifecycle for a final case: open, released, or cancelled.
2. A report release version containing the exact released microbiology content,
   release type, actor/time, source Analysis/Result identifiers, and corrected
   prior version.
3. An isolate-identification event containing before/after values, reason,
   actor/time, and optional amendment association.

Report versions retain their standard Analysis/Result sources through
normalized append-only source links, not delimited identifiers. AST runs
created during an amendment carry the lifecycle association needed to cancel
draft runs without changing a previously reviewed run.

Services enforce one open amendment, permission-independent business guards,
server-derived actors, and final locking outside an active amendment. Opening
an amendment captures a baseline report version when a legacy final case does
not already have one. Amended release creates a new Analysis revision and
Result through existing services, appends the report version, and locks again.

Schema change: one Liquibase file with rollback for the amendment, report
version/source, identification-event records, and the narrow AST amendment
association. No route/UI/test migration.

### Slice B - Repeat AST and reagent/card-lot traceability

Extend an AST run beyond its Slice A lifecycle association with attempt type,
source run, reason, run-level method, and a reportable-selection flag. A
repeat/retest creates a new run; old readings are never copied as mutable rows
or overwritten.

Create a shared lot-query/validation service over existing Test Catalog and
Inventory services. Add narrow linkage records from a culture setup or AST run
to the shared Inventory usage row. The save transaction re-reads and validates
the lot before recording usage to close the stale-selection race.

Schema change: one Liquibase file with rollback for AST metadata and usage
linkage only. Existing reagent definitions, lots, and usage remain untouched.

### Slice C - Accessibility and performance qualification

Add `@axe-core/playwright` to frontend development dependencies and register a
focused Microbiology accessibility project/test set. Pair automated checks with
a keyboard-only workflow because axe cannot prove interaction completeness.

Add an explicitly enabled qualification builder under test/support ownership
that creates the 200-case and dense-case datasets through application services.
The builder stays in test source and is not packaged into the WAR. Add
API/browser measurements with warm-up, fixed iteration counts, p50/p95/max
calculation, and JSON/Markdown evidence generation. Browser qualification uses
the existing property-gated UAT scenario endpoint on a disposable stack;
qualification data is never available from an ordinary deployment.

Qualification runs that commit their fixtures are supported only on a
disposable stack/database and require an explicit environment guard. Teardown
of that database is the cleanup mechanism. Shared demo or clinical databases
must not be cleaned by deleting audited patient/sample rows, and the browser
runner refuses to execute without the disposable-stack acknowledgement.

No schema migration is expected for Slice C. Performance indexes may be added
only if query-plan and measurement evidence demonstrates a data-model need.

## API and URL Behavior

- Case detail remains canonical at `/Microbiology/cases/:caseId`.
- Focused case state uses the existing `section` query parameter.
- Amendment selection/history uses stable query state under the case URL; no
  separate duplicate case page is introduced.
- New write endpoints are subordinate case/isolate/AST resources and never
  accept an audit actor from the client.
- Error responses use named, stable blocker codes for UI and Playwright
  assertions.

## Test Strategy

### Backend

- JUnit 4/Mockito first for lifecycle guards, report revision orchestration,
  identification history, repeat-run rules, lot eligibility, and stale-lot
  rejection.
- Controller tests prove authenticated actor derivation and 400/403/404/409
  behavior.
- Testcontainers integration tests prove persistence, unique-open-amendment,
  append-only history, Analysis/Result revision preservation, Inventory usage,
  and rollback-safe transactions.
- ORM validation covers every new entity.
- Liquibase update and rollback are run against an empty database and the
  current feature schema.

### Frontend

- Vitest/Testing Library covers Carbon form semantics, focus restoration,
  reason validation, repeat/run selection, lot eligibility messages, and
  history rendering.
- Playwright covers amendment, repeat AST, reagent traceability, keyboard-only
  workflow, and axe checks using registered projects.
- Selectors use roles/labels/test contracts, never presentation classes.

### Qualification

- Fixture construction is outside measured intervals.
- Five warm-up iterations precede at least twenty measured iterations per API
  operation; browser render measurements use at least ten iterations.
- A failure is any p95 above the stated threshold. Evidence includes all raw
  samples so percentile calculations can be reproduced.

## Delivery

M8 remains stacked on #3789 until the MVP spec and milestone PRs merge. It must
not be retargeted directly to the unmerged spec branch as a way to bypass the
MVP acceptance gate. The first review checkpoint is Slice A with focused tests;
Slices B and C may be split into follow-up PRs if review size exceeds a
coherent validation milestone.
