# Requirements Checklist: Analyzer-Independent QC/Results Roadmap

## Scope Integrity

- [ ] Roadmap excludes analyzer profiles, analyzer mapping GUI, bridge transport,
      ASTM/HL7, and FILE polling.
- [ ] OGC-811 is treated as the current Results Entry authority.
- [ ] OGC-517 is historical context only.
- [ ] OGC-427 is the first implementation anchor.
- [ ] OGC-817 follows Results Entry and consumes shared signals.
- [ ] OGC-899 is used for PNG/CPHL rollup and evidence.

## MVP Integrity

- [ ] MVP A covers Batch Workplan persistence, lifecycle, reagent lot, QC state,
      inline QC, NCE override, and print/preview.
- [ ] MVP B covers Results Entry worklist, edit-state, bench controls, inline
      NCE, sample usage, aliquots, and scoped history.
- [ ] MVP C covers Validation risk triage, guarded release, inline NCE reject,
      retest note, and e-sign release.

## Evidence Integrity

- [ ] Mock baseline commit is pinned.
- [ ] Mock screenshots are captured before UI implementation.
- [ ] Implementation screenshots match the same viewports.
- [ ] Mock comparison note exists.
- [ ] MP4 user-story proof exists.
- [ ] MP4 was inspected.
- [ ] Browser console logs were reviewed.
- [ ] Playwright does not use API calls as proof.

## Code-QA Integrity

- [ ] Spec-code alignment is documented.
- [ ] Tests sit at the right layer.
- [ ] No overmocking hides product gaps.
- [ ] Controllers do not own transactions.
- [ ] Services compile response data inside transactions.
- [ ] Liquibase owns schema changes.
- [ ] React Intl is used for user-facing strings.
- [ ] Non-English locale files are not edited.
- [ ] Carbon is used for UI.
- [ ] Legacy paths are removed, redirected, or tracked.

## PR Readiness

- [ ] Milestone tasks are checked off.
- [ ] Local validation commands are recorded.
- [ ] Evidence note is linked in PR body.
- [ ] Code-qa gate summary is in PR body.
- [ ] Mock comparison summary is in PR body.
- [ ] Follow-ups are explicit and not hidden scope.
