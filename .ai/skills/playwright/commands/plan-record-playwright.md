# Plan Record Playwright

Use this command to plan feature/PR scope, identify recording-worthy flows, and
orchestrate authoring + execution for one or multiple Playwright E2E tests.

## User Input

```text
$ARGUMENTS
```

Interpret input best-effort:

- `/plan-record-playwright` - plan from current PR/worktree context
- `/plan-record-playwright <feature-or-pr-scope>` - plan specific scope
- `/plan-record-playwright --flows <csv>` - plan and run a predefined flow set
- `/plan-record-playwright --record` - include `*-demo-video` recording stage

## Workflow

1. **Scope triage**

   - Review feature/PR scope, changed files, and acceptance criteria.
   - Identify candidate user flows and map each to a single test concern.
   - Classify each flow as: `core-app`, `harness`, `demo`, or `demo-video`.

2. **Flow plan**

   - Produce a concrete checklist for each flow:
     - route/page
     - setup data assumptions
     - user actions
     - expected assertions
     - target project
   - For batches, keep one behavior per spec/test where possible.

3. **Author tests**

   - For each planned flow, invoke `/write-playwright-test`.
   - Enforce source-first selector strategy and template usage.
   - Ensure each new spec is added to `frontend/playwright.config.ts`
     allowlists.

4. **Quality pass**

   - Run `/audit-playwright` on authored/updated specs.
   - Resolve selector and anti-pattern findings before recording.

5. **Execution and recording**

   - Run normal validation first in target project (`core-app`, `core-demo`,
     `harness`, or `harness-demo`).
   - Use `core-demo-video` / `harness-demo-video` only for recording output.
   - Keep pauses/title cards project-aware (`videoPause`, `showTitleCard`,
     `showStepCard`) so behavior is no-op outside `*-demo-video`.

6. **Artifact verification**

   - Verify `video.webm` exists for each recorded flow under
     `frontend/test-results/<test-name>/video.webm`.
   - If a run fails, use `/debug-playwright` with screenshot/trace evidence.

## Required References

- `.ai/skills/playwright/reference/plan-record-workflow.md`
- `.ai/skills/playwright/reference/write-workflow.md`
- `.ai/skills/playwright/reference/selector-policy.md`
- `.ai/skills/playwright/reference/debug-workflow.md`
