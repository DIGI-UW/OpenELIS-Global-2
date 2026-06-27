# OGC-782 Microbiology MVP Code-QA Review

Applied locally on 2026-06-27 using `DIGI-UW/code-qa` from
`https://github.com/DIGI-UW/code-qa` cloned at `/tmp/code-qa`.

## Meaningful Test Coverage

| Layer | Guard | Inversion it catches |
|---|---|---|
| Service | `MicroReportReleaseServiceTest` | Final release cannot proceed until readiness passes, and release activity history is written. |
| Service | `MicroWhonetReadinessServiceTest` | WHONET readiness reports blockers for missing isolate/AST/mapping state instead of silently passing. |
| Service | `MicroCaseReadinessServiceTest.missingIsolateBlocksFinalRelease` | A case with no isolates cannot be final-release ready. |
| Architecture/ORM | `MicrobiologyArchitectureTest`, `MicrobiologyOrmValidationTest` | New controllers stay out of transactions and mapped microbiology entities remain loadable. |
| Component | `ReportReadinessPanel.test.jsx` | The release button stays disabled while blocked and renders `FINAL_RELEASED` only after a real release call. |
| Component | `MicrobiologyCaseView.test.jsx`, `AstEntryPanel.test.jsx` | The case workbench keeps release readiness wired to AST updates and service dependencies. |
| E2E | `ogc-782-microbiology-mvp.spec.ts` on `core-demo` and `core-demo-video` | The assembled browser flow reaches final report release only after setup, isolate creation, AST reading, override, and review. |

The load-bearing guard is the browser E2E proof plus release service unit tests:
reverting the release-readiness UI/service wiring or final readiness gates would
break either the `FINAL_RELEASED` browser assertion or the service blocker tests.

## Spec-Code Alignment

| Finding | Classification | Resolution |
|---|---|---|
| M7 originally listed a release/readiness migration. | Doc lagged implementation decision. | Updated `tasks.md`: no M7 migration is required because existing `micro_case` release state and case activity history cover the MVP. |
| M7 originally listed separate release-readiness and demo Playwright specs. | Doc lagged implementation decision. | Updated `tasks.md` and `playwright-plan.md`: the canonical `ogc-782-microbiology-mvp.spec.ts` now carries the full MVP proof. |
| WHONET export extension was phrased like an implementation task. | Engineering scope clarification. | Updated `tasks.md`: M7 exposes readiness and avoids a duplicate exporter; existing export service remains untouched. |
| Final-release readiness did not block cases with no isolate. | Real code defect found during implementation. | Fixed `MicroCaseReadinessServiceImpl` and added `missingIsolateBlocksFinalRelease`. |
| Release panel unmounted during post-release refresh in E2E. | Real code defect found by Playwright. | Kept the case workbench mounted during post-release refresh and guarded async panel updates. |

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

No speculative configuration, duplicate exporter, duplicate alert surface, or
unused migration was introduced in M7.

## Evidence Bundle

Ran:

```bash
node /tmp/code-qa/skills/evidence-bundle/scripts/evidence-bundle.mjs \
  --results frontend/test-results \
  --evidence frontend/e2e-evidence \
  --out specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27 \
  --title "OGC-782 Microbiology MVP Checkpoint"
```

Result: 7 screenshots, 1 MP4, and a zip bundle generated locally. Binary media
is intentionally uncommitted; see
`specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27.md`
for local artifact paths.
