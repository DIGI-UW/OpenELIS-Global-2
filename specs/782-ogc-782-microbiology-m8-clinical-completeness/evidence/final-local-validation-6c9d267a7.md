# M8 final local validation

- Commit under test: `6c9d267a7`
- Stack: isolated `cc8c-ogc782` Docker Compose project
- Base URL: `https://127.0.0.1:48444`
- Result: **implemented slices pass; full M8 remains blocked on lot policy**

## Automated results

| Level | Result |
| --- | --- |
| Focused JUnit, ORM, Testcontainers, API qualification | 85/85 passed |
| Liquibase full update, M8 rollback, and reapply | 1/1 passed within the backend set |
| Carbon component tests | 27/27 passed |
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
| Worklist initial render | 2000 ms | 210.3 ms | PASS |
| Dense case initial render | 1000 ms | 206.5 ms | PASS |
| Worklist page interaction | 300 ms | 71.3 ms | PASS |

## Flagged validation issue

`npm run typecheck` remains red across the repository after the TypeScript 6
modernization. The same broad failure reproduces on the dedicated
`codex/typescript-6-typecheck` branch at `49714eb84`; examples include legacy
test globals, untyped JSX imports, existing service callback contracts, and
Vite configuration typing.

All errors in M8-owned Microbiology and qualification files were resolved in
`6c9d267a7`: Playwright 1.58 reduced-motion configuration now uses
`contextOptions`, animation readiness uses `Animation.pending`, live UAT JSON
has explicit types, and `MicrobiologyRoutes.js` has a declaration contract.
The project-wide baseline remains separate work and prevents marking T049
complete.

## Scope boundary

Lot selection and usage remain unimplemented and untested because the product
artifacts conflict on `PRIMARY / SECONDARY` versus
`REQUIRED / OPTIONAL / SUBSTITUTE` semantics. No default was inferred.
