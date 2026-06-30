# Frontend Modernization Roadmap

**Last Updated**: 2026-04-20 **Status**: Vite migration shipped (M1.a merged);
React 18 PR in review (M1.c); remaining milestones scoped and staged. **Epic**:
[#3449 — Frontend Modernization (Vite + React 18 + Carbon v11)](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3449)

---

## Context

OpenELIS Global 2's frontend is migrating from a legacy Create React App + React
17 + Carbon v1.15 stack to a modern Vite + React 18 + Carbon v11 toolchain. This
roadmap coordinates the interdependent upgrades, documents the sequencing
constraints, and clarifies which artifacts (SpecKit spec vs `specs/plans/`
plan doc vs GitHub issue) are appropriate for each work item. Several downstream
features (notably spec 016 unified navigation M2–M4) are blocked on this
modernization foundation landing.

---

## Epic #3449 milestone state (verified 2026-04-20)

| ID     | Description                                             | Tracker                                                           | Status               |
| ------ | ------------------------------------------------------- | ----------------------------------------------------------------- | -------------------- |
| M1.a   | Vite migration                                          | PR #3349 (branch `refactor/cra-to-vite`)                          | ✅ Merged 2026-04-20 |
| M1.a.1 | ESLint 9 flat config + PW guard migration               | PR #3458 (branch `fix/pw-retry-count-antipattern`)                | ✅ Merged 2026-04-20 |
| M1.a.2 | `frontend/src/` ESLint burndown (~1098 err + 1160 warn) | Issue #3459, PR #3460 (branch `cleanup/frontend-src-eslint-debt`) | ⏳ Draft PR          |
| M1.b   | Rebase #3448 onto post-Vite develop                     | —                                                                 | ⏳ Needed now        |
| M1.c   | Merge #3448 (React 18 upgrade)                          | PR #3448 (branch `feat/016-react-18-upgrade`)                     | ⏳ In review         |
| M1.d   | Decide fate of parallel PR #3366 (Carbon v11)           | PR #3366                                                          | ⏳ Stakeholder call  |
| M1.e   | Post-merge React 18 stabilization                       | —                                                                 | ⏳ Follows M1.c      |
| M1.f   | npm → pnpm migration                                    | Issue #3452                                                       | ⏳ Open              |
| M1.g   | Full TypeScript migration (big-bang)                    | Issue #2959                                                       | ⏳ Open              |

Then spec 016 nav work (M2–M4) unblocks.

---

## Artifact decision — where does work live?

This repo uses **three artifact tiers**. The rule: feature-shaped → SpecKit;
engineering-shaped → plan doc; trivial → issue-only.

| Tier                    | Use for                                                    | Location                                        |
| ----------------------- | ---------------------------------------------------------- | ----------------------------------------------- |
| **SpecKit spec**        | Features with FRs, user stories, user-facing acceptance    | `specs/NNN-{name}/spec.md + plan.md + tasks.md` |
| **Standalone plan doc** | Cross-cutting engineering: upgrades, refactors, migrations | `specs/plans/{name}.md`                         |
| **Issue-only**          | Small chores, single-PR fixes                              | GitHub issue body                               |

### Existing plan docs

Active plans live under `specs/plans/`; shipped/historical plans are moved to
`.specify/plan-archive/`.

- `.specify/plan-archive/cra-to-vite-migration.md` — Vite migration (shipped, archived)
- `specs/plans/react-18-frontend-modernization.md` — React 18 plan (PR #3448)
- `specs/plans/authoritative-harness-path.md`
- `specs/plans/e2e-ci-privilege-matrix.md`
- `specs/plans/e2e-ci-required-vs-convenience.md`
- `specs/plans/e2e-ci-trust-boundary-hardening.md`
- `specs/plans/playwright-quality-remediation.md`

Epic #3449 references `src-lint-cleanup.md` — that file does not yet exist and
will be created as part of M1.a.2 (#3460).

### Work-item → artifact mapping (this wave)

| Work item            | Tier                 | Artifact                                          | Tracker              | Branch                                        |
| -------------------- | -------------------- | ------------------------------------------------- | -------------------- | --------------------------------------------- |
| Vite (done)          | Plan doc             | `.specify/plan-archive/cra-to-vite-migration.md`  | #3312 CLOSED         | `refactor/cra-to-vite`                        |
| React 18 (in review) | Plan doc             | `specs/plans/react-18-frontend-modernization.md`  | PR #3448             | `feat/016-react-18-upgrade`                   |
| ESLint flat config   | Issue-only           | —                                                 | #3458 MERGED         | `fix/pw-retry-count-antipattern`              |
| src/ ESLint burndown | Plan doc             | `specs/plans/src-lint-cleanup.md` (to be created) | #3459, PR #3460      | `cleanup/frontend-src-eslint-debt`            |
| pnpm migration       | **NEW plan doc**     | `specs/plans/npm-to-pnpm-migration.md`            | #3452                | `chore/pnpm-frontend-package-manager`         |
| **TS migration**     | **NEW SpecKit spec** | `specs/017-typescript-migration/`                 | #2959                | `feat/017-typescript-migration-m{N}-{desc}`   |
| Unified nav (016)    | SpecKit spec         | `specs/016-unified-app-navigation/`               | #3346 spec PR merged | `feat/016-unified-app-navigation-m{N}-{desc}` |

### Rationale for the split

- **TS migration = spec 017**: codebase-wide refactor with real architectural
  decisions (strict mode config, OpenAPI type generation, bulk LLM strategy,
  SWR/Carbon/intl type impact). FR + user-story + tasks structure pays off for
  500+ file changes.
- **pnpm = plan doc**: tool swap, no user-facing behavior. Issue #3452's body is
  already 80% of a plan doc.
- **No new spec for Vite/React 18/ESLint**: in-flight or done. Retrofitting
  SpecKit is wasted effort.
- **016 stays nav-focused**: FR0 (modernization prerequisite) deleted;
  prerequisites updated to point at epic #3449 closure.

---

## Branch prefix conventions (observed + Constitution IX)

Constitution IX (`.specify/memory/constitution.md:1226-1234`) explicitly lists:
`spec/`, `feat/`, `fix/`, `hotfix/`. The repo in practice also uses `refactor/`,
`chore/`, `cleanup/`, `docs/` (verified via recent merged PRs #3349
`refactor/cra-to-vite`, #3461 `hotfix/useeffect-timer-leaks`, #3460
`cleanup/frontend-src-eslint-debt`).

Treat IX as authoritative for spec milestone branches; treat repo practice as
the norm for engineering branches. A constitution amendment to formally include
`refactor/`, `chore/`, `cleanup/` is folded into Stage 0 below.

### Prefixes used in this roadmap

| Prefix                          | Use                                 | Example                                             |
| ------------------------------- | ----------------------------------- | --------------------------------------------------- |
| `feat/{NNN}-{name}-m{N}-{desc}` | SpecKit spec milestones (strict IX) | `feat/016-unified-app-navigation-m2-backend-config` |
| `chore/{desc}`                  | Tool swaps                          | `chore/pnpm-frontend-package-manager`               |
| `cleanup/{desc}`                | Debt burndown                       | `cleanup/frontend-src-eslint-debt`                  |
| `fix/{desc}`                    | Targeted fixes                      | `fix/m1e-timer-leak-hunt`                           |
| `docs/{desc}`                   | Documentation-only changes          | `docs/frontend-modernization-roadmap`               |

---

## Staged roadmap

### Stage 0 — Constitution & doc hygiene (prerequisite)

**Goal:** Remove version pins and implementation details from the constitution.
Move them to `AGENTS.md` where they belong. Verified against SpecKit's own
framing: constitution is "architectural DNA" / principles only; implementation
details explicitly excluded.

Actions:

1. Edit `.specify/memory/constitution.md`: strip § "Technical Stack Constraints"
   down to principle statements (no versions). Retain principle-level rules.
2. Move version pins (React X, Carbon Y, pnpm Z, Cypress→Playwright transition,
   etc.) to `AGENTS.md` — § "Technical Stack (Current)".
3. Formalize `refactor/`/`chore/`/`cleanup/`/`docs/` in Constitution IX
   branch-naming table.
4. Add sync-impact-report entry + version bump.
5. Branch: `refactor/constitution-remove-version-pins`. Single PR.

**Checkpoint S0:**

- [ ] Constitution has no version pins, no specific tool commands, no Dockerfile
      details
- [ ] AGENTS.md has all moved content, clearly dated
- [ ] Constitution version bumped per § "Amendment Process"
- [ ] Sync-impact-report entry at top of constitution

---

### Stage 1 — Land #3448 (React 18)

**Goal:** Get React 18 on develop.

Actions:

1. `git fetch origin && git rebase origin/develop` on branch
   `feat/016-react-18-upgrade`. Conflicts expected in `frontend/package.json`,
   `vite.config.ts`, `frontend/src/index.jsx`, `frontend/src/App.jsx`,
   `frontend/src/setupTests.js`. Drop hunks already on develop.
2. Runtime canary: `cd frontend && npm run build`, serve `dist/` via outer
   proxy, smoke login + sidebar + one data-fetching page. Watch for React
   #130/#310.
3. `npm run pw:test` smoke bucket locally (never raw `npx playwright test`).
4. `mvn spotless:apply && cd frontend && npm run format`.
5. Diff `frontend/src/languages/en.json` vs develop to verify no keys dropped
   during rebase.
6. Close #3366 with a link to #3448 — its Carbon v11 bump is already baked into
   the Vite baseline.

**Checkpoint S1 (all must pass to merge #3448):**

- [ ] Rebase clean, no unresolved conflicts
- [ ] `npm run build` produces `dist/` without errors
- [ ] Runtime canary: no React #130/#310 in browser console
- [ ] `npm run pw:test` smoke green locally
- [ ] `npm run pw:guard` passes
- [ ] `rg "injectIntl" frontend/src` returns 0
- [ ] `rg "useHistory" frontend/src` returns 0
- [ ] `rg "react-router-dom.*Switch\|react-router-dom.*Redirect" frontend/src`
      returns 0
- [ ] `frontend/package.json` has no `resolutions` field
- [ ] `carbon-components` (v10) absent from `frontend/package.json`
- [ ] `en.json` diff: keys added, none removed
- [ ] `mvn spotless:check && cd frontend && npm run check-format` clean

**Doc sync (S1) — ship in the same PR or paired PR:**

- [ ] `AGENTS.md` § Technical Stack: React 17 → 18.3.1, Router 5 → 6.28,
      react-intl 5 → 7, `@testing-library/react` 9 → 16
- [ ] `CLAUDE.md`: remove references to `injectIntl`/`useHistory`/ `<Switch>`
      patterns if any remain
- [ ] `frontend/README.md`: update dev commands if changed
- [ ] Constitution: **NO version changes** — after Stage 0 it has no versions to
      sync

### Stage 2 — Parallel hardening

**Goal:** ESLint debt burndown (can run alongside Stage 1).

Actions:

7. PR #3460 (`cleanup/frontend-src-eslint-debt`). Author
   `specs/plans/src-lint-cleanup.md` if still missing.

**Checkpoint S2:**

- [ ] `npx eslint playwright/ src/ --max-warnings=0` exits 0 on fresh clone
      (note: the packaged `npm run lint:all` script masks failures with
      `|| exit 0`; un-masking it is part of this milestone's cleanup)
- [ ] `src-lint-cleanup.md` exists in `specs/plans/`

**Doc sync (S2):**

- [ ] `AGENTS.md`: add ESLint 9 flat config + `lint:all` script
- [ ] `frontend/README.md`: document lint commands

### Stage 3 — M1.e stabilization

**Goal:** Close out React 18 fallout on develop.

Actions:

8. Grep develop for `setInterval`/`setTimeout` in components without cleanup —
   the #3461 ErrorDashboard pattern suggests latent strict-mode-exposed timer
   leaks.
9. Triage residual `act()` warnings surfaced during Stage 1 test runs.
10. Open targeted `fix/` PRs for any runtime React 18 regression.

**Checkpoint S3:**

- [ ] Develop CI green for 3 consecutive runs
- [ ] No open `act()`-related test suite failures
- [ ] No `fix/m1e-*` PRs older than 1 week still open

### Stage 4 — pnpm migration

**Goal:** Lockfile swap. Per issue #3452: MUST NOT merge before #3448 + M1.e.

Actions:

11. Create `specs/plans/npm-to-pnpm-migration.md` (seed from #3452 issue
    body).
12. Branch `chore/pnpm-frontend-package-manager` from develop.
13. Delete `frontend/package-lock.json`, introduce `frontend/pnpm-lock.yaml`.
    Resolve phantom dependencies.
14. Update `frontend/Dockerfile`, `frontend/Dockerfile.prod`,
    `.github/workflows/*.yml` to `pnpm install` + pnpm caching.
15. Update `AGENTS.md` + `CLAUDE.md` tooling instructions.

**Checkpoint S4:**

- [ ] `pnpm build` completes in CI
- [ ] `pnpm test` green
- [ ] Playwright E2E green with pnpm Dockerfiles
- [ ] No `shamefully-hoist=true` in `.npmrc`

**Doc sync (S4):**

- [ ] `AGENTS.md` § Technical Stack: npm → pnpm
- [ ] `CLAUDE.md`: all `npm run X` → `pnpm X` where applicable
- [ ] `README.md`: frontend setup commands
- [ ] `frontend/README.md`: install + dev commands

### Stage 5 — TypeScript migration (NEW spec 017)

**Goal:** Codebase-wide TS conversion, strict mode.

**Important:** Use the canonical SpecKit workflow rather than hand-writing spec
files. Per SpecKit docs:
`/speckit.specify → /speckit.clarify → /speckit.plan → /speckit.tasks → /speckit.analyze → /speckit.implement`.
The spec is the source of truth; plan.md and tasks.md are derived.

Actions:

16. **Spec branch:** create `spec/017-typescript-migration` from develop.
17. **Run `/speckit.specify`** with prompt covering codebase-wide TS migration,
    strict mode, LLM-bulk-driven per issue #2959. Output:
    `specs/017-typescript-migration/spec.md`.
18. Review/edit spec.md to match repo conventions.
19. **Run `/speckit.clarify`** for the three open questions:
    - OpenAPI type generation: auto via `openapi-typescript` or hand-written?
    - Test file migration timing: alongside source or separate M?
    - Strict mode scope: `strict: true` all at once vs staged?
20. **Run `/speckit.plan`** to generate `plan.md`. Extend with Constitution IX
    milestone plan.
21. **Run `/speckit.tasks`** to generate `tasks.md`.
22. **Run `/speckit.analyze`** before implementation.
23. Execute milestones:
    - **017-M1** `feat/017-typescript-migration-m1-config`: `tsconfig.json`,
      Vite type-check integration, CI gate
    - **017-M2** `feat/017-typescript-migration-m2-bulk-convert`: LLM-driven
      bulk `.jsx` → `.tsx`
    - **017-M3** `feat/017-typescript-migration-m3-tsc-fix`: iterate until
      `tsc --noEmit` passes
    - **017-M4** `feat/017-typescript-migration-m4-strict-enforce`: tighten
      config, flip CI gate to blocking

**Checkpoint S5:**

- [ ] `tsc --noEmit` exits 0 on develop
- [ ] `pnpm test` green in TS
- [ ] No `.jsx` remains in `frontend/src/`
- [ ] Vite build still <15s

**Doc sync (S5):**

- [ ] `AGENTS.md` § Technical Stack: TypeScript adoption, strict mode,
      `tsc --noEmit` CI gate
- [ ] `CLAUDE.md`: type-check command reference
- [ ] `frontend/README.md`: TS setup

### Stage 6 — Close epic #3449

**Goal:** Verify all epic M1 acceptance criteria and close.

**Checkpoint S6 (from epic #3449 body):**

- [ ] `develop` on `react@^18.3.1` + `react-dom@^18.3.1`
- [ ] No `injectIntl` usages (grep clean)
- [ ] `react-router-dom@^6` everywhere; no `withRouter` or v5 API
- [ ] `@carbon/react@^1.104.0+`; `carbon-components` v10 removed
- [ ] Frontend unit tests: 0 failing suites
- [ ] Playwright E2E green on develop
- [ ] `npm run lint` and `npx eslint playwright/ src/ --max-warnings=0` both
      exit 0 (the packaged `npm run lint:all` script still masks failures with
      `|| exit 0`; check ESLint directly until the script is un-masked)
- [ ] `npm run pw:guard` passes
- [ ] `pnpm-lock.yaml` present, `package-lock.json` absent
- [ ] `tsc --noEmit` passing
- [ ] 016 M2 branch can rebase onto develop without conflicts

### Stage 7 — Spec 016 nav work (M2–M4)

**Goal:** Actual unified navigation. Born TypeScript-native, built on
post-modernization develop.

**Spec refinement strategy** (per SpecKit community guidance — spec is source of
truth; regenerate downstream where possible, manual-edit where regeneration
would lose repo-specific structure):

Actions:

21. **Run `/speckit.clarify`** on 016 to resolve scalar vs array `requiredRole`
    (analysis finding I1).
22. **Manual edit** `specs/016-unified-app-navigation/spec.md` (regeneration
    would lose custom Clarifications section):
    - Delete FR0 entirely
    - Update Dependencies: "Carbon v1.15+" → "Carbon v11 latest"
    - Add Prerequisite: "Epic #3449 closed; spec 017 shipped"
    - Apply I1 clarification to `data-model.md`, Liquibase snippet,
      `contracts/menu-api.yaml` in lockstep
    - Fix I4 (`MenuValueholder` → `Menu`)
    - Fix D1 (handler naming)
    - Fix I6 (Carbon dep version)
    - Add FR for `/rest/menu/{elementId}/reset` (U3)
23. **Manual edit** `specs/016-unified-app-navigation/tasks.md`:
    - Delete T001–T010 (M1 superseded by epic #3449)
    - Add tasks: U1 (icon-registry validation), A1 (perf measurement), M1 (i18n
      keys)
    - Replace dead script refs with Playwright scripts
    - Renumber
24. **Run `/speckit.analyze`** on 016 after edits.
25. Execute:
    - **016-M2** `feat/016-unified-app-navigation-m2-backend-config`:
      Liquibase + `Menu` entity + `MenuConfigurationHandler` + `MenuController`
      role filter + `/reset` endpoint
    - **016-M3** `feat/016-unified-app-navigation-m3-global-sidebar`:
      `GlobalSidebar.tsx` + `MenuIconRegistry.ts` + SWR + base `menus.json`
    - **016-M4** `feat/016-unified-app-navigation-m4-migration`: Replace
      hardcoded SideNavs, admin UI with `config_source`,
      `e2e/playwright/tests/navigation.spec.ts`

**Checkpoint S7 (from spec 016 success criteria):**

- [ ] All navigation uses `GlobalSidebar`
- [ ] Menu loading p95 < 200ms (via Playwright perf trace or Lighthouse CI)
- [ ] `/rest/menu` returns role-filtered tree
- [ ] `/rest/menu/{elementId}/reset` reverts admin overrides
- [ ] Icon registry rejects unknown `iconName`
- [ ] WCAG 2.1 AA pass (axe-core or Playwright a11y)
- [ ] New `sidenav.label.*` keys added to `en.json` only

**Doc sync (S7):**

- [ ] `AGENTS.md`: GlobalSidebar + `MenuConfigurationHandler` as navigation
      pattern
- [ ] `CLAUDE.md`: any hardcoded-SideNav references updated
- [ ] Update this roadmap to mark epic #3449 closed + 016 M4 complete

---

## Spec 017 TypeScript migration — FR outline

Draft for `specs/017-typescript-migration/spec.md` when Stage 5 begins:

**FR1** — No `.jsx` remains in `frontend/src/` (excepting documented
exceptions). All source files are `.ts` / `.tsx`.

**FR2** — `tsconfig.json` has `"strict": true`. Scope decision (all-at-once vs
staged) deferred to `/speckit.clarify`.

**FR3** — CI type-check gate: `tsc --noEmit` must pass to merge to develop.

**FR4** — Test files migrated: `.test.jsx` → `.test.tsx` with appropriate
test-library type imports.

**FR5** — API response types derived from OpenAPI contracts where available.
Strategy (`openapi-typescript` codegen vs hand-written) deferred to
`/speckit.clarify`.

**Non-functional:**

- No bundle-size regression vs pre-M1 baseline
- No Vitest/Jest suite regression
- Vite build time remains <15s

**User story (dev-facing):** "As a developer, I get type errors at compile time
instead of runtime. As a reviewer, I can trust that prop types are enforced by
the compiler."

**Acceptance tests:** (1) `tsc --noEmit` exits 0 on fresh develop; (2) PR with a
type error is blocked by CI; (3) `find frontend/src -name '*.jsx'` returns 0.

---

## Risks & open questions

1. **Carbon v10 cleanup verification** — verify `carbon-components` actually
   absent from `frontend/package.json` post-rebase. Fold removal into #3448 if
   still present.
2. **Transifex key preservation** — CI has a "Translation source-of-truth check"
   that blocks non-en locale edits. Confirm green post-rebase.
3. **TS-first for nav (Stage 5 before Stage 7)** — prevents the "write nav in
   JSX then convert" churn.
4. **Branch naming for #3448** — `feat/016-react-18-upgrade` doesn't match
   Constitution IX (missing `-m{N}-{desc}` segment). Too late to rename
   mid-review; document exemption in PR body.
5. **Testing Roadmap coverage for nav M3** — spec claims >70% Jest coverage.
   Post-React-18 strict-mode fallout may make new test writing slower than plan
   estimate.

---

## Summary of artifact changes

### To create

- **`specs/plans/npm-to-pnpm-migration.md`** (Stage 4).
- **`specs/plans/src-lint-cleanup.md`** (Stage 2) — referenced by epic #3449
  but not yet existing.
- **`specs/017-typescript-migration/spec.md + plan.md + tasks.md`** (Stage 5) —
  generated via canonical SpecKit commands, not hand-authored.

### To edit

- **`.specify/memory/constitution.md`** (Stage 0) — strip technical details;
  formalize branch prefixes.
- **`AGENTS.md`** (Stage 0 + per-milestone sync) — receive all version-pin
  content moved from constitution; update per stage.
- **`specs/016-unified-app-navigation/spec.md`** (Stage 7) — FR0 removal +
  I1/I4/D1/I6/U3 fixes + prerequisite update.
- **`specs/016-unified-app-navigation/tasks.md`** (Stage 7) — M1 deletion +
  missing-coverage adds + script-ref rewrites.

### Not creating

- No new specs for Vite, React 18, pnpm, or ESLint work. Plan docs + epic
  tracking are sufficient.

---

## SpecKit compliance

This roadmap was cross-checked against the
[official GitHub SpecKit docs](https://github.com/github/spec-kit) on
2026-04-20:

| Claim                                     | SpecKit stance                                       | Action                                                              |
| ----------------------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------- |
| Stage 5 uses canonical commands           | ✅ Canonical                                         | Kept                                                                |
| Stage 7 manual-edits 016                  | ⚠️ Deviation — spec is source of truth               | Accepted: regeneration would lose Constitution IX milestone tagging |
| Stage 0 strips constitution version pins  | ✅ Matches "architectural DNA / principles" framing  | Kept                                                                |
| Constitution IX milestone-per-PR          | 🟦 Repo extension — SpecKit silent                   | Kept, documented                                                    |
| `specs/plans/` engineering plans          | 🟦 Repo extension — SpecKit feature-focused          | Kept, documented                                                    |
| `specs/roadmaps/`                         | 🟦 Repo extension — SpecKit has no portfolio pattern | Kept — this file                                                    |
| Feature 017 numbering                     | ✅ Matches SpecKit algorithm                         | Verified                                                            |
| Plan.md extended with milestone/risk/deps | ⚠️ Official template simpler                         | Accepted — Constitution IX requires milestones                      |
| `/speckit.analyze` mid-workflow           | ✅ Canonical                                         | Applied                                                             |
| `/speckit.clarify` for spec refinement    | ✅ Canonical                                         | Applied in Stages 5, 7                                              |

Sources:

- [github/spec-kit](https://github.com/github/spec-kit)
- [spec-driven.md](https://github.com/github/spec-kit/blob/main/spec-driven.md)
- [plan-template.md](https://github.com/github/spec-kit/blob/main/templates/plan-template.md)
- [Discussion #775 — refining spec after plan/tasks](https://github.com/github/spec-kit/discussions/775)

---

## Reference

- **Epic**: [#3449](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3449)
- **React 18 PR**:
  [#3448](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3448)
- **Vite PR (merged)**:
  [#3349](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3349)
- **ESLint PRs**:
  [#3458 merged](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3458),
  [#3460 draft](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3460)
- **pnpm issue**:
  [#3452](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3452)
- **TS migration issue**:
  [#2959](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/2959)
- **Spec 016**:
  [`specs/016-unified-app-navigation/`](../016-unified-app-navigation/)
- **React 18 plan**:
  [`specs/plans/react-18-frontend-modernization.md`](../plans/react-18-frontend-modernization.md)
- **Vite plan (archived)**:
  [`.specify/plan-archive/cra-to-vite-migration.md`](../../.specify/plan-archive/cra-to-vite-migration.md)
- **Constitution**:
  [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)
