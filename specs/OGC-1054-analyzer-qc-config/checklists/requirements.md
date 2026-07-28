# OGC-1054 Acceptance Checklist

## Specification

- [ ] Each implementation change traces to an AC-1054 criterion.
- [ ] Feature 004, 012, 014, OGC-41, roadmap, and API/route contracts agree.
- [ ] Deferred work is absent from MVP completion claims.
- [ ] FILE runtime ownership matches `AGENTS.md`.

## Frontend

- [ ] Carbon components/tokens are used without a Carbon dependency upgrade.
- [ ] Every page has one semantic `h1` and linkable breadcrumbs.
- [ ] Search, filters, setup step, selected profile, and safe return path are
  bookmarkable.
- [ ] Back, forward, and reload preserve saved-analyzer workflow state.
- [ ] Primary actions remain visible and non-overlapping on desktop and mobile.
- [ ] All new strings are in `frontend/src/languages/en.json`.
- [ ] No duplicate analyzer creation, mapping, pending-value, or QC editor path
  is introduced.

## Testing

- [ ] Pure route and validation logic has focused unit tests.
- [ ] Component tests use real router behavior for navigation acceptance.
- [ ] Backend service tests own result-option, fingerprint, readiness, audit,
  and bridge contracts.
- [ ] Playwright performs the full visible user story without API-focused
  shortcuts, forced controls, response polling, or arbitrary waits.
- [ ] Non-video evidence is inspected before recording the MP4.
- [ ] Desktop/mobile screenshots are compared with
  `openelis-work@4c0e1a28`.

## Remote UAT

- [ ] Grist contains stable required keys `AN-QC-001` through `AN-QC-008`.
- [ ] The review overlay shows the expected checklist revision.
- [ ] Build metadata identifies application and harness SHAs.
- [ ] Every required step passes against that exact build.
- [ ] Markdown/JSON report, screenshots, trace, and MP4 are retained.

## Merge

- [ ] Spotless, formatting, lint, focused suites, and package suites pass.
- [ ] `digi-uw/code-qa` gates pass or have explicit disposition.
- [ ] PR title/body and linked evidence describe current HEAD.
- [ ] PR is non-draft, mergeable, and all required checks/reviews pass.
