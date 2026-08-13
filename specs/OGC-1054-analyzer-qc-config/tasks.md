# OGC-1054 Analyzer QC/Configuration Foundation Tasks

**Status:** Historical branch task record; not the full feature backlog
**Authoritative backlog:** F0 through R2 in
[the feature roadmap](../roadmaps/ogc-1054-analyzer-feature-roadmap.md)

## Completed Branch Work

- [x] Add route/query helpers and URL-backed analyzer/profile list state.
- [x] Add shared Carbon page header, breadcrumbs, setup progress, and tables.
- [x] Add bookmarkable Instrument, Verify, Connect, and Review routes.
- [x] Redirect `/analyzers/new` to the branch's inline setup entry.
- [x] Apply a selected shipped bootstrap profile exactly once at create time.
- [x] Add catalog-bound Result Option selection and legacy-unbound handling.
- [x] Add mapping/QC fingerprints, actor/time, audit, stale state, and blockers.
- [x] Integrate operational analyzer QC rules/control lots and Bridge resync.
- [x] Keep Bridge collections deterministic and FILE runtime Bridge-owned.
- [x] Run branch-focused JUnit, RTL, formatting, build, and Playwright gates.
- [x] Record historical July foundation UAT and MP4 evidence.

## Foundation Disposition (F0)

- [ ] Rename PR #3792 from MVP acceptance to foundation.
- [ ] Rebase onto current `develop` and review the range-diff.
- [ ] Re-run all affected tests against current code.
- [ ] Confirm each retained change conforms to the fixed Bridge ownership model.
- [ ] Remove, migrate, or priority-track every touched legacy/duplicate path.
- [ ] Merge as a reviewable foundation or extract compatible commits and
      supersede #3792.

Do not add M1-M4 to this branch. Their dependency-ordered tasks and acceptance
criteria are maintained in the authoritative roadmap and future milestone
SpecKit sets.
