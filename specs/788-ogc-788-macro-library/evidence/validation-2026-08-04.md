# OGC-788 M1 Automated Validation - 2026-08-04

## Results

| Check                                                                    | Result                                                                                                        |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| Focused JUnit 4, controller, security, ORM, Liquibase, and fixture tests | 26 passed                                                                                                     |
| Text-macro integration scale                                             | 500 service-created phrases; fifth 100-row page returned deterministically in a 0.735-second three-test suite |
| Frontend Vitest/RTL                                                      | 14 passed across 5 files                                                                                      |
| Foundational Playwright                                                  | 2 passed including authentication setup                                                                       |
| Desktop/mobile accessibility                                             | 4 passed and 1 intentional mobile keyboard skip; no WCAG 2.1 AA findings                                      |
| Standard demo video                                                      | 2 passed including authentication setup; 46.52-second H.264/yuv420p 16:9 MP4                                  |
| Production Vite build                                                    | Passed                                                                                                        |

The Liquibase test updates, rolls back, and reapplies the new model on
Testcontainers PostgreSQL. The standalone Hibernate ORM test completes in
1.134 seconds without starting a database. The property-gated UAT scenario and
the 500-phrase qualification use services, generated IDs, and transactions; no
SQL fixture, fixed persisted ID, or DAO bypass is used.

## Reusable Test Strategy

- Pure token parsing and caret replacement are tested in
  `macroTextEngine.test.js` without rendering.
- Controlled Carbon field behavior, keyboard selection, pointer selection,
  announcements, focus restoration, and context changes are tested in
  `MacroTextArea.test.jsx`.
- Canonical URL state is tested in `queryState.test.js`; editor and DataTable
  behavior are covered by focused component tests.
- Service and controller tests own normalization, authorization, actor
  attribution, malformed requests, paging, and context filtering.
- ORM and Liquibase tests cover mapping and migration contracts separately.
- `playwright/helpers/text-macro.ts` supplies role/label-driven interactions to
  both foundational and demo journeys. Functional readiness uses response or
  visible-state assertions; no arbitrary waits are used.
- `captureEvidenceScreenshot` centralizes deterministic screenshot capture for
  demo and accessibility projects.

## Negative Proof

The active-state integration fixture was intentionally inverted. The focused
integration test failed because the inactive generated-ID phrase was excluded;
restoring the production behavior made the same test pass. This demonstrates
that the test is sensitive to the behavior it claims to protect.

## Known Baseline Noise

Repository-wide `npm run typecheck` remains red in pre-existing calculated-value,
results-viewer, and older Playwright sources. No OGC-788 file appears in those
errors. The focused frontend tests and production Vite build pass. Existing
Vite warnings for CSS-module syntax, direct `eval`, and chunk size are outside
this slice.

The local self-signed stack reports service-worker certificate errors and an
unnamed asset 404. Text-macro API requests pass, and the noise does not affect
the journey or accessibility scans.
