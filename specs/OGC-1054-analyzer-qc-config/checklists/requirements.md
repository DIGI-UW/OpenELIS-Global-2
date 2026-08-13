# OGC-1054 Analyzer QC/Configuration Foundation Checklist

**Scope:** PR #3792 foundation only
**Full MVP acceptance:**
[MVP-001 through MVP-024](../../roadmaps/ogc-1054-analyzer-feature-roadmap.md#deterministic-mvp-acceptance-criteria)

## Source and Scope

- [x] `openelis-work` is used only for functional/visual comparison.
- [x] No technical directive is inferred from a product mock or product brief.
- [x] FILE and analyzer runtime ownership matches `AGENTS.md`.
- [x] July evidence is labeled historical foundation evidence.
- [x] Missing full-feature behavior is explicit.

## Foundation Frontend

- [x] Touched pages use Carbon components/tokens and localized copy.
- [x] Touched setup pages have one semantic `h1` and linked breadcrumbs.
- [x] Branch list/filter/setup state is URL-addressable.
- [x] Back, forward, and reload preserve saved-analyzer branch state.
- [x] Primary branch actions remain reachable at tested desktop/mobile widths.
- [x] `/analyzers/new` does not create a duplicate setup path.

## Foundation Testing

- [x] Pure route and validation logic has focused unit tests.
- [x] Component navigation uses real router behavior.
- [x] Backend tests own result-option, fingerprint, readiness, audit, and Bridge
      payload behavior implemented by the branch.
- [x] Foundation Playwright uses visible UI rather than API-focused shortcuts.
- [x] Non-video evidence was inspected before the historical MP4.

## Still Required Before Foundation Merge

- [ ] Rebase onto current `develop` and resolve conflicts.
- [ ] Re-run affected tests and CI on the rebased head.
- [ ] Complete review and legacy-path disposition.
- [ ] Update PR title/body to foundation scope.
- [ ] Reach non-draft, mergeable, reviewed, green status.

Passing this checklist does not satisfy the full MVP checklist.
