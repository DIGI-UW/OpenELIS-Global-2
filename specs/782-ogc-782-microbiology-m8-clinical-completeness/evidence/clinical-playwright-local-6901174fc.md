# Microbiology clinical Playwright qualification

- Application fix commits: `50daa1855`, `4cfa4b7ae`
- Semantic interaction commit: `6901174fc`
- Stack: isolated `cc8c-ogc782` Docker Compose project
- Base URL: `https://127.0.0.1:48444`

## Verified journeys

`npx playwright test playwright/tests/foundational/core/microbiology-amendment.spec.ts --project=core-app --reporter=line`

- Result: setup and amendment journey passed, 2/2.
- Proves original and amended Patient History results, reasoned isolate
  re-identification, immutable report versions, and restored final-case lock.

`npx playwright test playwright/tests/foundational/core/microbiology-repeat-ast.spec.ts --project=core-app --reporter=line`

- Result: setup and repeat-AST journey passed, 2/2.
- Proves immutable original/retest attempts, required reason, explicit
  reportable-run selection, release gating, and selected S/I/R propagation to
  Patient History.

`npx playwright test playwright/tests/accessibility/core/microbiology-keyboard.spec.ts --project=core-accessibility --reporter=line`

- Result: setup and keyboard-only journey passed, 2/2.
- Proves worklist filtering/navigation, isolate and AST entry, and amendment
  release without pointer-only interaction.

All case controls are selected through accessible roles and names within the
case workbench. The journeys synchronize on URL state, enabled/checked state,
and rendered application status. They contain no `waitForTimeout`, sleeps,
forced clicks, or private Carbon class selectors.
