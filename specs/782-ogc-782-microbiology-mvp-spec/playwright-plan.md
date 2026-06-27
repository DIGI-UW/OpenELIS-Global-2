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
