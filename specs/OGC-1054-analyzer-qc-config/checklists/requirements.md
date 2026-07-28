# OGC-1054 Acceptance Checklist

## Specification

- [x] Each implementation change traces to an AC-1054 criterion.
- [x] Feature 004, 012, 014, OGC-41, roadmap, and API/route contracts agree.
- [x] Deferred work is absent from MVP completion claims.
- [x] FILE runtime ownership matches `AGENTS.md`.

## Frontend

- [x] Carbon components/tokens are used without a Carbon dependency upgrade.
- [x] Every page has one semantic `h1` and linkable breadcrumbs.
- [x] Search, filters, setup step, selected profile, and safe return path are
      bookmarkable.
- [x] Back, forward, and reload preserve saved-analyzer workflow state.
- [x] Primary actions remain visible and non-overlapping on desktop and mobile.
- [x] All new strings are in `frontend/src/languages/en.json`.
- [x] No duplicate analyzer creation, mapping, pending-value, or QC editor path
      is introduced.

## Testing

- [x] Pure route and validation logic has focused unit tests.
- [x] Component tests use real router behavior for navigation acceptance.
- [x] Backend service tests own result-option, fingerprint, readiness, audit,
      and bridge contracts.
- [x] Playwright performs the full visible user story without API-focused
      shortcuts, forced controls, response polling, or arbitrary waits.
- [x] Non-video evidence is inspected before recording the MP4.
- [x] Desktop/mobile screenshots are compared with
      `openelis-work@4c0e1a28`.

## Remote UAT

- [x] Grist contains stable required keys `AN-QC-001` through `AN-QC-008`.
- [x] The review overlay shows the expected checklist revision.
- [x] Build metadata identifies application and harness SHAs.
- [x] Every required step passes against that exact build.
- [x] Markdown/JSON report, screenshots, trace, and MP4 are retained.

## Merge

- [x] Spotless, formatting, lint, focused suites, and package suites pass.
- [x] `digi-uw/code-qa` gates pass or have explicit disposition.
- [ ] PR title/body and linked evidence describe current HEAD.
- [ ] PR is non-draft, mergeable, and all required checks/reviews pass.
