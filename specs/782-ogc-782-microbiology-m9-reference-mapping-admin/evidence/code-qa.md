# M3 Code-QA Review

Reviewed against pinned `tools/code-qa` revision
`30528d176bd128b4765242d130f38ca9fb85d7b8`. The bundled multi-agent runner is
not available in this Codex session, so the four reviews were executed
sequentially against base `0d963f3fe925c3d2bc90818e109ce4aafb030031` and the
current M3 branch.

## Spec-to-code alignment

| Finding                                                                                                             | Classification        | Resolution                                                                                                                                   |
| ------------------------------------------------------------------------------------------------------------------- | --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Opening a linkable editor reset list pagination.                                                                    | Code defect           | Fixed in `queryState.js`; `edit` now preserves page state and has a focused regression test.                                                 |
| Unsupported status values could remain in the URL while the server applied `ALL`.                                   | Code defect           | Fixed with section-aware client canonicalization and server status whitelists.                                                               |
| A current but inactive AST panel displayed a green `Current` tag.                                                   | Code defect           | Current/historical and active/inactive are now rendered as separate Carbon tags in `AstPanelPage.jsx`.                                       |
| Carbon table rows briefly mixed refreshed source data with the prior normalized row model.                          | Runtime defect        | Cell values now carry their complete render data; a component regression proves publication can refresh without dereferencing a missing row. |
| `MicroBreakpointActivationEvent` was omitted from explicit runtime/test persistence registration.                   | ORM defect            | Registered the entity in both persistence units; exact-SHA remote ORM validation now passes.                                                 |
| A broad reagent-lot `span` selector leaked secondary text color into Carbon definition tooltips on mobile.          | Accessibility defect  | Scoped the selector to the test-name label; the formerly failing Pixel 5 AST scan and the complete desktop/mobile suite now pass.            |
| Applied breakpoint imports disabled the action but did not expose `importedRows` to the user.                       | UX defect             | Added an explicit Carbon `1 imported` status tag with component and Playwright assertions.                                                   |
| The product spec limited duplicate-name rejection to active entries while persistence enforces identity uniqueness. | Product wording drift | `spec.md` now says duplicate existing entries are rejected.                                                                                  |
| Local correction protection had service coverage but no assembled user-story proof.                                 | Evidence gap          | The M3 Playwright import story now edits an imported rule, verifies `Local correction`, and verifies a matching re-import is rejected.       |

No unresolved product contradiction remains in the M3 acceptance boundary.
Breakpoint content remains synthetic; WHONET export, subscriptions, expert rules,
and operational TB remain explicitly excluded. The M-02 mock's `Export CSV`
action is absent from the M3 product contract and UAT, so it is recorded in
`mock-comparison.md` as a later product-scope ruling rather than silently claimed.

## Meaningful test coverage

| Layer            | Guard                                                                                                              | Assessment                                                                                                                                                                |
| ---------------- | ------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Service          | `MicrobiologyReferenceAdminServiceTest`, `MicroBreakpointAdminServiceTest`, and `MicroBreakpointImportServiceTest` | Real normalization, versioning, actor, and import decisions are asserted on produced values or captured entities. Mockito does not claim to prove HQL or schema behavior. |
| Architecture/ORM | `MicrobiologyArchitectureTest` and `MicrobiologyOrmValidationTest`                                                 | Guards service-only fixture construction and entity mapping. Mock-local IDs in unit tests are not persisted fixture seeds.                                                |
| Migration        | `MicrobiologyM9LiquibaseRollbackTest`                                                                              | PostgreSQL update and transaction rollback proof passed in a Docker-capable environment.                                                                                  |
| Component/query  | `MicrobiologyReferenceAdmin.test.jsx` and `queryState.test.js`                                                     | Covers rendering, direct-link rule editing, invalid URL normalization, and editor-state preservation. The new URL tests would fail against the pre-fix implementation.    |
| E2E              | `microbiology-reference-admin.spec.ts`                                                                             | Uses Carbon roles/labels and observable state; no arbitrary waits. All five foundational stories and the paced deployed walkthrough pass.                                 |

Final automated results:

- Backend: 21 focused JUnit 4 tests passed.
- Remote exact-SHA ORM: 2 tests passed against the deployed checkout.
- Frontend: 33 focused Vitest tests passed; the final import/tooltip slice passed
  12 tests after the last UI corrections.
- Production frontend build passed with only repository-baseline warnings.
- Final deployed foundational Playwright: 6/6 passed.
- Final deployed desktop/mobile accessibility: 9/9 passed.
- Final standardized video project: 2/2 passed in 58.4 seconds, including
  authentication setup. The packaged H.264 walkthrough is 53.9 seconds.

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

- Automated runtime/evidence SHA:
  `7416a1626ccd0dade98aa6b010f91b56c226e4f4`.
- Standardized presentation/test source SHA:
  `4db920d2844b201ed1e4d98b06f28a94e9932dc6`.
- Ready target app SHA: `fe7ca789f4f9026e6a679e496a06c3e860da8c12`;
  commits after the runtime evidence only reconcile tests, presentation, and
  specification artifacts.
- Review harness SHA: `72eb003155db91f08a90d5e853e7811f86d3c642`.
- Deployment: `20260804T200840Z-fe7ca789f4f9`, ready with health and smoke
  verification passed; `/` and `/Microbiology/worklist` returned HTTP 200.
- Grist revision: `c3a490ab422180d87ada093cf05a2cc727413a01bc6234c3217fb99c466e7c3c`
  with 33 steps, 32 required, and one optional TB reflection.
- External evidence:
  - [Paced MP4](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/walkthrough.mp4)
  - [Screenshot contact sheet](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/contact-sheet.png)
  - [Video-frame contact sheet](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/video-contact-sheet.png)
  - [Complete bundle](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/walkthrough.zip)
- Verified host SHA-256:
  - MP4: `149169f6edd134bd6d9f09ace4539d30b2f22f4324866e539b8194bf0d788deb`
  - Screenshot contact sheet: `a240f990e78c3b1b35811167122382c6feba0565dd498f1cf88c32d6b7e5a6e0`
  - Video-frame contact sheet: `f892bfe1648f02518b41985cbffcefe606089d6348180a3f65bc72d8497efd2c`
  - Complete bundle: `dca0d64cdbcc6e93d1bddb33ede7f6db282086cf37962fa2857854969dbfa675`

The walkthrough follows the reusable stakeholder-evidence format now documented
in `frontend/playwright/README.md`: milestone opener, story chapter cards,
compact scene labels, a completion card that distinguishes automation from
human acceptance, reviewed screenshots, and contact sheets. Functional waits
remain observable assertions; presentation pauses execute only in video mode.

The detailed `mock-comparison.md` review found no product contradiction in the
delivered M3 workflow. The implementation intentionally uses the native config-driven
OpenELIS Admin sidenav, shell, breadcrumbs, and Carbon tabs instead of treating
the mock's internal navigation as an implementation contract. The resulting
workflow preserves the mock's vocabulary, panel-versioning, breakpoint, and
import intent while remaining usable at mobile widths.

Two nonblocking evidence risks remain visible. The long-lived AMR database
retains service-created synthetic panel history, so broad searches accumulate
review records; the final evidence targets the exact newly published row without
deleting history. Playwright also observes the repository's existing unnamed
404 asset request, but no M3 API request fails. Neither is counted as human UAT.
All reviewer Pass/Fail/N/A marks remain pending in the live overlay.
