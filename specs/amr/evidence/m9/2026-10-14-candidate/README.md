# M9 fixed-candidate evidence

Captured 14 October 2026 America/New_York (15 October UTC), from the local source
stack at `https://localhost:38220`. All patient/site identifiers are synthetic.
Candidate: the implementation commit containing this directory, on
`codex/amr-v2-ux-validation`, above M8 `9471be12ba997998bd3ddf64db69551b6e8f8aa0`.

Reference: openelis-work `c9722f07304c3170913d565876a6dd085c388a7d`.
The [baseline evidence](../2026-10-14/README.md) remains separate.

| File                                                                    | Meaning                                                                                                                                  |
| ----------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `m9-viewport-1440.png`, `m9-viewport-390.png`                           | Actual 1440 × 1000 and 390 × 1000 viewports of the same persisted clinical case after opening an amendment.                              |
| `m9-canonical-1440.png`, `m9-canonical-390.png`                         | Full-page captures at those viewport widths; clinical context, questionnaire, sample context, amendment and history share one workspace. |
| `m9-report-notes.pdf`, `patient-report-render.png`                      | Released patient report and its rendered page. The external culture note appears beside M9-PLATE; the internal note is absent.           |
| `environmental-certificate.pdf`, `environmental-certificate-render.png` | Released environmental certificate and rendered page, with site identity and no-growth culture outcome.                                  |
| `typecheck-comparison.json`                                             | Repository-wide TypeScript comparison with the unchanged preparation revision; both fail and are not acceptance evidence.                |
| `backend-results.json`                                                  | Sanitized counts from 22 focused local Surefire reports (186 tests, zero failures/errors/skips). This is not a full CI summary.          |

Screenshots/PDFs come from `microbiology-ux-context.spec.ts` and
`microbiology-environmental.spec.ts`, in the successful six-test candidate-F batch.
The same batch covers incoming placement/count refresh, bench inoculation/scan,
narrow bench layout and current WHONET population selection. Backend tests use
real PostgreSQL where specified by the integration suite. Component tests and
browser evidence remain distinct.

Local diagnostic artifacts live under `.worktrees/amr-ux-validation/` and are not
committed: `m9-ui-final.log`, `m9-persistence-regression.log`,
`m9-journey-regression.log`, `m9-boundaries.log`, `m9-last-services.log`, and
`m9-candidate-{a,b,c,d,e,f,g}.log`. Earlier failing batches are retained for diagnosis;
only rerun successes support the [candidate report](../../../milestone-9-candidate-validation.md).
Browser console review found requests aborted by navigation, rather than failed
server mutations; the culture-purpose save test now waits for its save response
before reload. No authentication storage or raw server logs are included here.
