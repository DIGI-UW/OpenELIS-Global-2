# Playwright Plan: OGC-782 Microbiology MVP

## M4 Case Workbench

- Flow: `microbiology-case-workbench`
- Route: `/Microbiology/cases/:caseId`
- Setup: provision one bacteriology case and its reference prerequisites
  through the property-gated application scenario service.
- User actions:
  - open the case workbench,
  - record setup activity with next stage `SETUP_RECORDED`,
  - create isolate `ISO-1` from Gram stain and colony morphology,
  - confirm AST remains unavailable,
  - identify the organism with method, confidence, and significance.
- Expected outcomes:
  - case header renders sample item, workflow, and current stage,
  - visible stage changes to `SETUP_RECORDED`,
  - timeline shows the setup note,
  - isolate list first shows pending identification and then `Escherichia coli`,
  - AST becomes available only after identification,
  - timeline shows the `ISOLATE_CREATED` activity after case refresh.
- Project: `core-app`
- Evidence command:
  `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-case-workbench.spec.ts --project=core-app`

## M5 Manual AST

- Flow: `ogc-782-microbiology-mvp`
- Route: `/Microbiology/cases/:caseId`
- Setup: provision one bacteriology case, AST panel, antibiotic, breakpoint
  standard, and one MIC breakpoint rule through the property-gated application
  scenario service.
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

## M6 Worklist + Critical Communication

- Flow: `microbiology-worklist-critical`
- Routes: `/Microbiology/cases/:caseId`, `/Microbiology/worklist`
- Setup: provision one bacteriology case with a sibling TB workflow on the same
  sample item and AST reference prerequisites through the property-gated
  application scenario service.
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
- Engineering note: the clinical communication remains authoritative while its
  lifecycle is projected into the existing Alert workflow.

## M7 Release + Surveillance Readiness

- Flow: `ogc-782-microbiology-mvp`
- Route: `/Microbiology/cases/:caseId`
- Setup: reuse the M5/M6 seeded bacteriology case, AST panel, antibiotic, CLSI
  2026 standard, and MIC breakpoint rule. M7 adds no new schema fixture because
  release uses existing `micro_case` release state and case activity history.
- User actions:
  - open the case workbench,
  - record setup activity,
  - create a clinically significant isolate,
  - start a manual AST run,
  - record and override a MIC reading,
  - review the AST run,
  - confirm final release readiness,
  - release the final report.
- Expected outcomes:
  - final release remains blocked until an isolate exists and AST is reviewed,
  - the report readiness panel refreshes after AST review,
  - WHONET readiness is displayed separately from final release readiness,
  - final release writes `FINAL_RELEASED` and renders that state on the case
    workbench.
- Projects: `core-demo`, `core-demo-video`
- Evidence commands:
  `python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts`
  `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo`
  `cd frontend && npm run pw:test -- playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts --project=core-demo-video`

## Navigation And Stable URLs

- Flow: `microbiology-worklist-critical`
- Routes:
  - `/Microbiology/worklist`
  - `/Microbiology/cases/:caseId`
- Automated actions:
  - open the configured main navigation and choose Microbiology worklist,
  - verify the navigation remains usable beside the worklist,
  - set workflow and sort filters and assert canonical query composition,
  - open a seeded case and assert worklist context is retained,
  - select the Isolates case section and assert refresh-stable section state,
  - return to the worklist and assert prior filters are restored.
- Project: `core-app`
- Evidence command:
  `cd frontend && BASE_URL=https://localhost:48443 DB_CONTAINER=ogc-782-microbiology-db npm run pw:test -- playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts --project=core-app`

## R1 Case Nonconformance And Lost Specimen

- Flow: `microbiology-case-workbench`
- Route: `/Microbiology/cases/:caseId`
- Setup: provision two independent cases through the property-gated
  application scenario service. One remains available for a flag-only
  nonconformance; the other is disposable because Mark Lost rejects its
  physical specimen and open work.
- User actions:
  - open Report NCE from the case header and verify canonical action state,
  - choose configured category, reporting unit, and severity; enter a
    description; retain Flag only; and submit,
  - open a separate case, choose Mark Lost, verify configured defaults, and
    submit.
- Expected outcomes:
  - specimen identity and actor are derived rather than entered by the user,
  - reference choices come from active configuration rather than fixed IDs,
  - missing lost-specimen configuration is a named blocker,
  - the browser observes the saved timeline and terminal case state.
- Project: `core-app`
- Evidence command:
  `cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-case-workbench.spec.ts --project=core-app`
