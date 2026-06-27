# Playwright Plan: OGC-782 Microbiology MVP

## M4 Case Workbench

- Flow: `microbiology-case-workbench`
- Route: `/MicrobiologyCaseView/:caseId`
- Setup: seed one bacteriology `micro_case` with a `sample_item` and initial
  `CASE_CREATED` activity through test-only Postgres setup.
- User actions:
  - open the case workbench,
  - record setup activity with next stage `SETUP_RECORDED`,
  - create isolate `ISO-1` with preliminary organism text.
- Expected outcomes:
  - case header renders sample item, workflow, and current stage,
  - visible stage changes to `SETUP_RECORDED`,
  - timeline shows the setup note,
  - isolate list shows `ISO-1: Escherichia coli`,
  - timeline shows the `ISOLATE_CREATED` activity after case refresh.
- Project: `core-app`
- Evidence command:
  `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-case-workbench.spec.ts --project=core-app`
- Evidence result: passed locally on 2026-06-27 against the worktree dev stack
  after rebuilding `target/OpenELIS-Global.war`, recreating the OpenELIS dev
  containers, and confirming Liquibase had applied the microbiology M1/M2
  tables.

## M5 Manual AST

- Flow: `ogc-782-microbiology-mvp`
- Route: `/MicrobiologyCaseView/:caseId`
- Setup: seed one bacteriology `micro_case` with a `sample_item`, AST panel,
  antibiotic, CLSI 2026 standard, and one MIC breakpoint rule through
  test-only Postgres setup.
- User actions:
  - open the case workbench,
  - record setup activity,
  - create a clinically significant isolate,
  - confirm final release is blocked before AST review,
  - start a manual AST run,
  - record a MIC reading,
  - confirm automatic susceptible interpretation,
  - override interpretation with a reason,
  - review the AST run.
- Expected outcomes:
  - setup and isolate activity appears in the case view,
  - AST run transitions from in progress to reviewed,
  - raw interpretation and overridden interpretation are visible,
  - readiness changes from blocked to final-release ready after AST review.
- Projects: `core-demo`, `core-demo-video`
- Evidence commands:
  `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo`
  `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo-video`
- Evidence result: passed locally on 2026-06-27 against the worktree dev stack
  after rebuilding and recreating the OpenELIS/frontend/proxy containers.
- Screenshot evidence:
  - `frontend/e2e-evidence/ogc-782-case-opened.png`
  - `frontend/e2e-evidence/ogc-782-setup-recorded.png`
  - `frontend/e2e-evidence/ogc-782-isolate-created.png`
  - `frontend/e2e-evidence/ogc-782-ast-reading.png`
  - `frontend/e2e-evidence/ogc-782-ast-overridden.png`
  - `frontend/e2e-evidence/ogc-782-ast-reviewed-ready.png`
- Video evidence:
  `frontend/test-results/demo-core-ogc-782-microbio-3f6cc-ual-AST-override-and-review-core-demo-video/video.webm`

## M6 Worklist + Critical Communication

- Flow: `microbiology-worklist-critical`
- Routes: `/MicrobiologyCaseView/:caseId`, `/MicrobiologyWorklist`
- Setup: seed one bacteriology case with a sibling TB workflow on the same
  sample item and AST reference prerequisites through test-only Postgres setup.
- User actions:
  - open the bacteriology case,
  - log a critical communication with a free-text recipient and follow-up flag,
  - open the shared microbiology worklist,
  - confirm the seeded case is high priority and shows sibling workflow context,
  - open the case from the worklist,
  - acknowledge the critical communication.
- Expected outcomes:
  - logged communication appears as `OPEN` on the case,
  - worklist row shows `HIGH`, `Critical communication`, and
    `MYCOBACTERIOLOGY_TB`,
  - acknowledgement changes the communication status to `ACKNOWLEDGED`.
- Project: `core-app`
- Evidence commands:
  `python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts`
  `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts --project=core-app`
- Evidence result: passed locally on 2026-06-27 against the worktree dev stack
  after rebuilding and recreating the OpenELIS/frontend/proxy containers.
- Engineering note: generic `Alert` currently requires numeric entity ids while
  microbiology cases use UUID strings, so M6 surfaces critical communication in
  the microbiology worklist and does not force a generic alert row.
