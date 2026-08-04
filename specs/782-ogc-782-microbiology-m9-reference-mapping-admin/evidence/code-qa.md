# M3 Code-QA Review

Reviewed against pinned `tools/code-qa` revision
`30528d176bd128b4765242d130f38ca9fb85d7b8`. The bundled multi-agent runner is
not available in this Codex session, so the four reviews were executed
sequentially against base `0d963f3fe925c3d2bc90818e109ce4aafb030031` and the
current M3 branch.

## Spec-to-code alignment

| Finding                                                                                                             | Classification        | Resolution                                                                                                                             |
| ------------------------------------------------------------------------------------------------------------------- | --------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| Opening a linkable editor reset list pagination.                                                                    | Code defect           | Fixed in `queryState.js`; `edit` now preserves page state and has a focused regression test.                                           |
| Unsupported status values could remain in the URL while the server applied `ALL`.                                   | Code defect           | Fixed with section-aware client canonicalization and server status whitelists.                                                         |
| A current but inactive AST panel displayed a green `Current` tag.                                                   | Code defect           | Current/historical and active/inactive are now rendered as separate Carbon tags in `AstPanelPage.jsx`.                                 |
| The product spec limited duplicate-name rejection to active entries while persistence enforces identity uniqueness. | Product wording drift | `spec.md` now says duplicate existing entries are rejected.                                                                            |
| Local correction protection had service coverage but no assembled user-story proof.                                 | Evidence gap          | The M3 Playwright import story now edits an imported rule, verifies `Local correction`, and verifies a matching re-import is rejected. |

No unresolved product contradiction remains in the M3 acceptance boundary.
Breakpoint content remains synthetic; WHONET export, subscriptions, expert rules,
and operational TB remain explicitly excluded.

## Meaningful test coverage

| Layer            | Guard                                                                                                              | Assessment                                                                                                                                                                |
| ---------------- | ------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Service          | `MicrobiologyReferenceAdminServiceTest`, `MicroBreakpointAdminServiceTest`, and `MicroBreakpointImportServiceTest` | Real normalization, versioning, actor, and import decisions are asserted on produced values or captured entities. Mockito does not claim to prove HQL or schema behavior. |
| Architecture/ORM | `MicrobiologyArchitectureTest` and `MicrobiologyOrmValidationTest`                                                 | Guards service-only fixture construction and entity mapping. Mock-local IDs in unit tests are not persisted fixture seeds.                                                |
| Migration        | `MicrobiologyM9LiquibaseRollbackTest`                                                                              | Correct level and authored against PostgreSQL, but not yet executed because local Docker is unavailable. This remains blocking task T007A.                                |
| Component/query  | `MicrobiologyReferenceAdmin.test.jsx` and `queryState.test.js`                                                     | Covers rendering, direct-link rule editing, invalid URL normalization, and editor-state preservation. The new URL tests would fail against the pre-fix implementation.    |
| E2E              | `microbiology-reference-admin.spec.ts`                                                                             | Uses Carbon roles/labels and observable state; no arbitrary waits. Live execution remains pending exact-SHA deployment.                                                   |

Local predeployment results:

- Backend: 16 focused JUnit tests passed.
- Frontend: 11 focused Vitest tests passed.
- Production frontend build passed with only repository-baseline warnings.
- Playwright registration passed: setup plus five M3 stories.

## Simplicity review

Verdict: **lean for the required full-stack workflow**.

- Shared vocabulary behavior is concentrated in `ReferenceDataPage.jsx` and
  `definitions.js`; AST panel versioning and breakpoint lifecycle remain separate
  because their interactions are materially different.
- Query-state behavior is one small shared module instead of route-specific copies.
- No remote subscription, export, proprietary breakpoint distribution, expert-rule
  engine, or TB behavior was added.
- The migration exists only for new persistence state. Routes, UI, tests, and UAT
  fixtures add no migrations.
- UAT data is created through the property-gated scenario service. The architecture
  test rejects SQL/native-query fixture access.

## Evidence readiness

The committed test and UAT contracts are ready, but media evidence must come from
the deployed exact SHA. Before acceptance, the remaining evidence gate is:

1. execute PostgreSQL migration update and rollback proof;
2. deploy the final pushed SHA and verify app/schema metadata;
3. run focused Playwright and accessibility projects against AMR;
4. inspect stable desktop/mobile screenshots against M-01/M-02 workflow intent;
5. create the external MP4/screenshot bundle and link it from the PR without
   committing binaries.

This report is a predeployment gate, not a claim that live or human UAT has passed.
