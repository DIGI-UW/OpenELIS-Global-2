# OGC-782 Microbiology MVP Code-QA Review

Originally applied locally on 2026-06-27 using `DIGI-UW/code-qa`. The reviewed
tooling is now pinned in this repository at `tools/code-qa`, revision
`30528d176bd128b4765242d130f38ca9fb85d7b8`.

Remediation review refreshed on 2026-07-28 at app commit `3440cbdc3`.

## Meaningful Test Coverage

| Layer | Guard | Inversion it catches |
|---|---|---|
| Service | `MicroReportReleaseServiceTest` | Final release cannot proceed until readiness passes, and release activity history is written. |
| Service | `MicroWhonetReadinessServiceTest` | WHONET readiness reports blockers for missing isolate/AST/mapping state instead of silently passing. |
| Service | `MicroCaseReadinessServiceTest.missingIsolateBlocksFinalRelease` | A case with no isolates cannot be final-release ready. |
| Architecture/ORM | `MicrobiologyArchitectureTest`, `MicrobiologyOrmValidationTest` | New controllers stay out of transactions and mapped microbiology entities remain loadable. |
| Component | `ReportReadinessPanel.test.jsx` | The release button stays disabled while blocked and renders `FINAL_RELEASED` only after a real release call. |
| Component | `MicrobiologyCaseView.test.jsx`, `AstEntryPanel.test.jsx` | The case workbench keeps release readiness wired to AST updates and service dependencies. |
| Service/REST | `MicroIsolateServiceTest`, `MicroAstServiceTest`, `MicrobiologyRestExceptionHandlerTest` | Final cases reject mutation and expose a named HTTP 409 conflict instead of a generic 500. |
| Component | `IsolatePanel.test.jsx`, `MicrobiologyCaseView.test.jsx` | Final cases visibly become read-only and no longer offer enabled isolate mutation actions. |
| E2E | `microbiology-order-entry.spec.ts` on `core-app` | The real order wizard shows microbiology details for a culture-routed test and removes them after switching to a nonculture test. |
| E2E | `ogc-782-microbiology-mvp.spec.ts` on `core-demo` and `core-demo-video` | The assembled browser flow covers explicit setup, isolate creation and identification update, two AST readings, override/review, Result-target critical communication, final release, named mutation rejection, and visible patient-report content. |

The load-bearing guards are the browser E2E proof plus release and mutation
service tests. Reverting culture routing, report projection, final readiness, or
the final-case lock breaks a focused test at the layer that owns the behavior.

## Spec-Code Alignment

| Finding | Classification | Resolution |
|---|---|---|
| M7 originally listed a release/readiness migration. | Doc lagged implementation decision. | Updated `tasks.md`: no M7 migration is required because existing `micro_case` release state and case activity history cover the MVP. |
| M7 originally listed separate release-readiness and demo Playwright specs. | Doc lagged implementation decision. | Updated `tasks.md` and `playwright-plan.md`: the canonical `ogc-782-microbiology-mvp.spec.ts` now carries the full MVP proof. |
| WHONET export extension was phrased like an implementation task. | Engineering scope clarification. | Updated `tasks.md`: M7 exposes readiness and avoids a duplicate exporter; existing export service remains untouched. |
| Final-release readiness did not block cases with no isolate. | Real code defect found during implementation. | Fixed `MicroCaseReadinessServiceImpl` and added `missingIsolateBlocksFinalRelease`. |
| Release panel unmounted during post-release refresh in E2E. | Real code defect found by Playwright. | Kept the case workbench mounted during post-release refresh and guarded async panel updates. |
| A service-created UAT `Method` lacked localization and broke backend restart. | Real fixture-integrity defect found by deployment. | The fixture now creates or repairs localization through services; the display list uses the entity's null-safe localized-value accessor. |
| Final-case mutation surfaced as generic HTTP 500 and left edit controls active. | Real behavior/UX defect found during acceptance review. | Added a named 409 conflict and an explicit read-only final state while retaining service-layer enforcement. |
| Culture-specific order fields had component tests but no assembled browser proof. | Evidence gap. | Added a registered `core-app` Playwright test across both culture and nonculture branches. |

## Simplicity Review

Verdict: `lean`.

Kept on purpose:

- `MicroReportReleaseService` and `MicroWhonetReadinessService` are separate
  because release state mutation and surveillance-readiness evaluation have
  different ownership and test boundaries.
- WHONET readiness is shown in `ReportReadinessPanel` rather than a separate
  panel to avoid adding a new component for a single read-only status block.
- No release/readiness migration was added because the MVP can use existing
  `micro_case` state plus activity history.
- No parallel WHONET exporter or generic Alert bridge was added.
- The final lock uses one domain exception and one package-scoped REST advice;
  it does not duplicate lock checks in every controller.
- Order-entry evidence reuses the property-gated service fixture and existing
  wizard rather than introducing SQL, fixed primary keys, or a second fixture
  mechanism.

No speculative configuration, duplicate exporter, duplicate alert surface, or
unused migration was introduced in M7.

## Validation Refresh

Passed locally at `3440cbdc3`:

- 15 focused backend controller/service tests.
- 9 focused frontend component tests.
- Playwright ESLint and project discovery for `core-app` and `core-demo`.
- `npm run pw:guard`.
- Backend Spotless and frontend Prettier.

After refreshing ignored local dependencies, TypeScript 6.0.3 executes but the
repository-wide `npm run typecheck` still reports broad pre-existing strict-type
debt across legacy Playwright and application files. The new microbiology
order-entry spec is not among the reported diagnostics. That baseline belongs
to the frontend-modernization follow-up and is not hidden as an OGC-782 pass.

## Evidence Bundle

Ran:

```bash
node tools/code-qa/skills/evidence-bundle/scripts/evidence-bundle.mjs \
  --results frontend/test-results \
  --evidence frontend/e2e-evidence \
  --out specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27 \
  --title "OGC-782 Microbiology MVP Checkpoint"
```

Result: 7 screenshots, 1 MP4, and a zip bundle generated locally. Binary media
is intentionally uncommitted; see
`specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27.md`
for local artifact paths.
