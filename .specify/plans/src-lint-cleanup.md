# Plan: Clean up `frontend/src/` ESLint debt

Tracks [#3459](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3459). Part
of epic [#3449](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3449)
(Frontend Modernization). Depends on
[#3458](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3458) landing first
(that PR fixes the broken ESLint config + migrates to ESLint 9 flat config;
until then no lint runs and this cleanup can't start).

## Starting state (once #3458 is on develop)

- `npm run lint` — Playwright scope only, blocking, 0 issues.
- `npm run lint:all` — Playwright + src/ scope, advisory (`|| exit 0` so it
  always exits 0; surfaces violations in output).
- Current baseline on `src/` (captured during #3458 work): **~1098 errors +
  ~1160 warnings**.

## Rule-class triage plan

Work through these in order. Each phase should be its own commit (or small group
of commits) on this branch. The goal is that `npm run lint:all` trends down
toward 0; when it reaches 0, delete the `|| exit 0` fallback and fold `lint:all`
into `lint`.

### Phase 1 — config-level wins (no per-file changes)

- [ ] **Vitest globals**: add `vi`, `vitest` (and any other Vitest-specific
      identifiers) to the `**/*.test.{js,jsx,ts,tsx}` override in
      `eslint.config.js`. Should eliminate every `'vi' is not defined` error in
      one line.
- [ ] **`react/prop-types`**: decide at the config level — keep `"warn"`
      (current), bump to `"error"`, or disable. React 17 + TS migration
      conventions argue for disabling; decide explicitly and document in config
      comment.
- [ ] **`react-hooks/exhaustive-deps`**: currently disabled. Re-enable at
      `"warn"` and measure impact before deciding `"error"`.
- [ ] **`@typescript-eslint/no-explicit-any`**: likely volume-driver on TS
      files. Triage: keep as `"warn"`, scope per-directory (e.g., strict in new
      code, lenient in legacy), or disable. Document reasoning.
- [ ] **`no-useless-assignment`**: new in ESLint 9; these are usually real bugs
      (dead initializers). Fix per-occurrence, don't disable.

### Phase 2 — mechanical / auto-fixable

- [ ] `npx eslint --fix src/` — run and review the diff. Prettier +
      `@typescript-eslint/no-unused-vars` (with `argsIgnorePattern: "^_"`
      already configured) are the big wins here.
- [ ] Unused imports cleanup — likely dozens of occurrences; post-autofix
      review.

### Phase 3 — per-file remaining errors

Whatever's left after Phases 1 + 2. Expected categories:

- Genuine `no-undef` cases that aren't covered by globals config.
- TypeScript `any` usages that need real types.
- Complex `react/prop-types` violations where PropTypes add real value.
- Any rule violation that requires judgment about whether the existing code is
  correct or a subtle bug.

### Phase 4 — tighten the lint gate

- [ ] Remove `|| exit 0` from `npm run lint:all` script.
- [ ] Flip CI's `Run ESLint on src/ (advisory)` step from
      `continue-on-error: true` to blocking.
- [ ] Consider folding `lint` + `lint:all` into a single `lint` script once both
      cover the same scope at the same strictness.

## Out of scope

- Upgrading ESLint further (already on 9.x).
- Migrating rules that require TypeScript type-info
  (`@typescript-eslint/no-floating-promises` etc.) — needs
  `parserOptions.project` config, larger investment. Track separately if we want
  that coverage.
- Enabling stricter TypeScript compiler options (`strict`, `noImplicitAny`,
  etc.) — different axis from ESLint.

## Verification

- `cd frontend && npm run lint:all` — goal: exits 0 with 0 errors + 0 warnings
  on fresh clone.
- CI's `Run ESLint on src/` step can be flipped to hard-blocking without false
  failures.
- No regression in existing functionality — run the full Playwright + Vitest
  suites before each phase's commit.
