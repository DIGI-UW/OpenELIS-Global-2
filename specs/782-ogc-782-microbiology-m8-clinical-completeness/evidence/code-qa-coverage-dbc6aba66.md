# Code QA: Meaningful Test Coverage

- Feature commit: `dbc6aba66`
- `DIGI-UW/code-qa`: `30528d176bd128b4765242d130f38ca9fb85d7b8`
- Verdict: **meaningful coverage for completed slices; lot slice unimplemented**

## Level-Correct Guards

| Level                     | Guard                                                                                                | Result                                 | What it catches                                                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------------------------- | -------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Integration               | `MicroAmendmentIntegrationTest`, `MicroAstIntegrationTest`, qualification integration, ORM bootstrap | Included in 84/84 focused backend pass | Real Liquibase mappings, report-version persistence, history relationships, rollback, and AST attempt storage   |
| Unit/controller           | Amendment, AST, projection, release, mutation, worklist, actor/error tests                           | Included in 84/84 focused backend pass | Lifecycle branches, named blockers, authenticated actor mapping, and response contracts                         |
| Component                 | Amendment, AST, isolate, worklist Carbon tests                                                       | 27/27 focused pass                     | Rendered state, enabled/disabled controls, incremental typing, native selection, focus, and service integration |
| E2E                       | Amendment, repeat AST, keyboard, workbench, critical/worklist journeys                               | Green on isolated `cc8c-ogc782` stack  | Assembled clinical outcomes, patient-result propagation, canonical URLs, Carbon interaction, and final relock   |
| Accessibility/performance | 14 axe scans; API/browser/query-plan qualification                                                   | Pass                                   | Detectable WCAG 2.1 AA defects and measured workload budgets                                                    |

## Migration Reversibility

`MicrobiologyM8LiquibaseRollbackTest` starts a disposable PostgreSQL 14.4
database, applies the repository's full root changelog, and verifies the M8
schema. It then loads a test-only changelog containing the canonical `060` and
`061` paths, rolls back exactly those two changesets, verifies that all M8
tables and columns are absent, reapplies them, and verifies that the schema is
restored.

```text
mvn -Dtest=MicrobiologyM8LiquibaseRollbackTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The first harness attempt intentionally exposed a Liquibase path-identity
problem when the entire `3.5.x.x/base.xml` was loaded from a different root.
The final guard scopes validation to the two canonical M8 changeset paths and
does not alter or clear historical checksums.

## Focused JaCoCo

The clean focused run executed 84 backend tests and generated JaCoCo at
`target/site/jacoco/`. Instruction coverage for the changed clinical service
classes is:

| Class                                   | Coverage |
| --------------------------------------- | -------: |
| `MicroAstServiceImpl`                   |    86.9% |
| `MicroCaseAmendmentServiceImpl`         |    96.2% |
| `MicroCaseReadinessServiceImpl`         |    96.8% |
| `MicroIdentificationHistoryServiceImpl` |    94.6% |
| `MicroIsolateServiceImpl`               |    88.9% |
| `MicroReportProjectionServiceImpl`      |    87.3% |
| `MicroReportReleaseServiceImpl`         |    76.8% |
| `MicroReportVersionServiceImpl`         |    79.3% |
| `MicroWorklistServiceImpl`              |    91.6% |

Every listed changed service exceeds the repository's 70% new-code target.
The new amendment controller is 94.2% instruction-covered. Existing controller
classes contain unrelated legacy endpoints, so their whole-class aggregate is
not presented as changed-line coverage.

## Inversion Proof

The load-bearing integration guard was run once with
`reportVersionService.recordAmendedFinal(...)` temporarily removed from
`MicroCaseAmendmentServiceImpl.completeAmendment`.

- Expected red result: `MicroAmendmentIntegrationTest` failed at line 95 with
  `expected:<2> but was:<1>`.
- Production behavior was restored immediately.
- Green confirmation: the same integration class passed 2/2.

This proves the integration test detects loss of amended report history against
the real persistence path; it is not an assert-on-mock-return test.

## Honest Gaps

- FR-006/FR-007 and SC-003 have no tests because lot policy is unresolved and
  no lot implementation has been written.
- Frontend percentage coverage is not claimed because this repo has no enabled
  Vitest coverage provider. Component and E2E outcomes are reported directly.
- The repository-wide TypeScript 6 check remains red on pre-existing project
  debt, including on the dedicated modernization branch. M8-owned TypeScript
  errors are resolved and documented in `final-local-validation-6c9d267a7.md`.
