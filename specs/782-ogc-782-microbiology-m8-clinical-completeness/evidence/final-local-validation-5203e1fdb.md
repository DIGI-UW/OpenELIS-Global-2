# M8 final local validation

- Commit under test: `5203e1fdb`
- Stack: isolated `cc8c-ogc782` Docker Compose project
- Base URL: `https://127.0.0.1:48444`
- Result: **implemented slices pass; full M8 remains blocked on lot policy**

## Automated results

| Level | Result |
| --- | --- |
| Focused JUnit, ORM, Testcontainers, fixture, API qualification | 99/99 passed |
| Liquibase full update, M8 rollback, and reapply | 1/1 passed within the backend set |
| Changed Carbon component tests | 55/55 passed |
| Core clinical and Carbon Playwright journeys | 6/6 passed |
| Desktop/mobile accessibility and keyboard Playwright | 8/8 passed; 14 axe scans reported zero WCAG 2.1 AA violations |
| Browser performance Playwright | 2/2 passed |
| Spotless | passed |
| Prettier | passed |
| Playwright ESLint | passed |
| Vite production build | passed with existing repository warnings |
| `git diff --check` | passed |

The Playwright journeys synchronize on URL, response, enabled/checked, focus,
and rendered app state. The Microbiology tests contain no `waitForTimeout`,
forced clicks, or private Carbon class selectors. Component tests use
`user-event`, including `selectOptions` for Carbon native selects and ordinary
click/type interactions for controls.

## Current browser performance

The disposable workload contained 200 worklist cases and one dense case with 5
isolates and 80 AST readings. Two warm-ups preceded ten measured iterations.

| Operation | Budget | p95 | Result |
| --- | ---: | ---: | --- |
| Worklist initial render | 2000 ms | 230.8 ms | PASS |
| Dense case initial render | 1000 ms | 217.1 ms | PASS |
| Worklist page interaction | 300 ms | 73.0 ms | PASS |

The M7 base was synchronized before this run. The merge exposed a test-order
dependency in workflow status setup; all required statuses are now established
through `StatusOfSampleService` at each integration/qualification fixture
entry point. The focused suite proves the scenarios remain green without SQL,
fixed primary keys, DAO bypass, or reliance on another test running first.

## Flagged validation issue

`npm run typecheck` remains red across the repository after the TypeScript 6
modernization. The same broad failure reproduces on the dedicated
`codex/typescript-6-typecheck` branch at `49714eb84`; examples include legacy
test globals, untyped JSX imports, existing service callback contracts, and
Vite configuration typing.

All errors in M8-owned Microbiology and qualification files were resolved by
`5203e1fdb`: Playwright 1.58 reduced-motion configuration uses
`contextOptions`, animation readiness uses `Animation.pending`, live UAT JSON
has explicit types, and `MicrobiologyRoutes.js` has a declaration contract.
The project-wide baseline remains separate work and prevents marking T049
complete.

## Scope boundary

Lot selection and usage remain unimplemented and untested because the product
artifacts conflict on `PRIMARY / SECONDARY` versus
`REQUIRED / OPTIONAL / SUBSTITUTE` semantics. No default was inferred.
