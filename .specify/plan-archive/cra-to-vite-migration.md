# CRA → Vite Migration Plan

## Context

GitHub issue #3312. Create React App (CRA) is officially abandoned.
`react-scripts@5.0.1` is the last release and locks us into 16 vulnerable
transitive dependencies (6 high, 1 moderate, 9 low) with no fix path. These are
all build/test-time (not production runtime), but they trigger GitHub's push
banner ("20 vulnerabilities") and fail `npm audit --audit-level=high`.

## Current State

### Build Toolchain

- **react-scripts@5.0.1** — last CRA release, no further patches
- **Webpack 5** (via react-scripts) — bundler
- **Babel** (via babel-preset-react-app) — transpiler
- **Jest 27** (via react-scripts) — test runner

### Change Surface

| Metric                            | Count                                                         |
| --------------------------------- | ------------------------------------------------------------- |
| Source files (JS/JSX/TS/TSX)      | 411                                                           |
| Jest test files                   | 75                                                            |
| Playwright E2E specs              | 23 (unaffected — test running app)                            |
| Cypress specs (deprecated)        | 49 (unaffected)                                               |
| `REACT_APP_` env vars             | **0** (huge win — no renaming needed)                         |
| `process.env` references          | 1 (`NODE_ENV` in serviceWorkerRegistration.js)                |
| `@svgr` overrides in package.json | 12 entries                                                    |
| Docker build references           | 1 (frontend/Dockerfile, `npm run build`)                      |
| CI workflow references            | `npm run build` in `frontend.yml`, `build.docker-compose.yml` |

### Test Coverage

- 75 Jest tests / 411 source files = **18% file coverage**
- All Jest tests currently use `react-scripts test` as the runner
- E2E tests (Playwright + Cypress) are build-tool agnostic

### What Gets Fixed

Removes all 16 react-scripts-locked vulnerabilities:

- 6 HIGH: serialize-javascript, css-minimizer-webpack-plugin,
  rollup-plugin-terser, workbox-build, workbox-webpack-plugin, react-scripts
  itself
- 1 MODERATE: webpack-dev-server
- 9 LOW: jest@27 chain (jest-environment-jsdom → jsdom → http-proxy-agent)

## Git Workflow

**Branch:** `refactor/cra-to-vite` **PR title:**
`refactor(frontend): migrate from Create React App to Vite (#3312)` **Closes:**
#3312

```bash
# Start clean from latest develop
git fetch origin
git checkout -b refactor/cra-to-vite origin/develop

# After Phase 1 (core migration compiles + validates):
mvn spotless:apply  # backend formatting (touches XML, MD, properties)
cd frontend && npm run format && cd ..
git add frontend/package.json frontend/package-lock.json frontend/vite.config.ts \
  frontend/index.html frontend/src/serviceWorkerRegistration.js
git commit -m "refactor(frontend): replace react-scripts with Vite (#3312)

Remove react-scripts@5.0.1, install vite + @vitejs/plugin-react + vite-plugin-svgr.
Move index.html to root, update scripts, fix process.env.NODE_ENV reference.
Resolves 16 npm audit vulnerabilities locked by abandoned CRA."
git push -u origin refactor/cra-to-vite

# Open PR immediately with: gh pr create --base develop \
#   --title "refactor(frontend): migrate from Create React App to Vite (#3312)" \
#   --body "Closes #3312 ..."
# CI starts running — continue locally

# After Phase 2 (Jest config validates):
git add frontend/jest.config.js frontend/package.json frontend/src/setupTests.js
git commit -m "refactor(frontend): standalone Jest config decoupled from CRA (#3312)"
git push

# After Phase 3 (Docker + CI validates):
git add frontend/Dockerfile .github/workflows/frontend.yml
git commit -m "refactor(frontend): update Dockerfile and CI for Vite build (#3312)"
git push

# After Phase 4 (import fixes validate):
git add frontend/src/
git commit -m "fix(frontend): resolve import paths for Vite strict ESM mode (#3312)"
git push
# Check CI results from earlier commits: gh pr checks <PR#>
```

**Key principles:**

- `git fetch origin` before branching — always start from latest develop
- Commit after each phase — don't batch into one massive commit
- `mvn spotless:apply` + `npm run format` before every commit
- Open PR after Phase 1 so CI runs in parallel with local Phase 2-4 work
- Use `gh pr checks` to monitor CI while working locally
- Each commit should be independently reviewable
- Stage specific files, not `git add -A` (avoid accidental inclusions)
- If CI fails on an earlier commit, fix forward in the next commit
- PR body should include `Closes #3312` to auto-close the tracking issue on
  merge

## Migration Plan

### Phase 1: Core Migration (~4 hours)

**Validate before moving to Phase 2:**

- [ ] `npm run build` succeeds (Vite production build)
- [ ] `npm run start` serves the app (Vite dev server)
- [ ] App loads in browser, login page renders
- [ ] Commit + push + open PR

**1.1 Install Vite + plugins**

```bash
npm install --save-dev vite @vitejs/plugin-react vite-plugin-svgr
npm uninstall react-scripts
```

**1.2 Create `vite.config.ts`**

- React plugin with JSX runtime
- SVGR plugin (replaces 12 `@svgr` overrides)
- Proxy config for `/api` → backend (replaces CRA proxy in package.json)
- `resolve.extensions` to match CRA's lenient import resolution
- `build.outDir: 'build'` to maintain Docker compatibility (or update
  Dockerfile)

**1.3 Move `public/index.html` → `index.html` (root)**

- Add `<script type="module" src="/src/index.js"></script>`
- Replace `%PUBLIC_URL%` with `/` (Vite serves from root)

**1.4 Update `package.json` scripts**

```json
"start": "vite",
"build": "vite build",
"preview": "vite preview"
```

**1.5 Fix `process.env.NODE_ENV`**

- `serviceWorkerRegistration.js`: replace with `import.meta.env.MODE`

**1.6 Remove CRA residuals**

- Delete `@svgr` overrides (Vite plugin handles it)
- Remove `babel-preset-react-app` dependency
- Remove `eslint-config-react-app` (or keep if ESLint config depends on it)

### Phase 2: Jest Configuration (~4 hours)

**Validate before moving to Phase 3:**

- [ ] `npm test` — all 75 Jest tests pass
- [ ] No new warnings or import errors
- [ ] Commit + push (CI runs Phase 1 build in parallel)

Two options:

**Option A (recommended): Keep Jest, add standalone config**

- Install `@testing-library/jest-dom`, `babel-jest`, `@babel/preset-env`,
  `@babel/preset-react`
- Create `jest.config.js` with `testEnvironment: 'jsdom'`, module name mapper
  for CSS/SVG/image imports
- Update `package.json`: `"test": "jest"`
- Existing 75 tests should work with minimal changes

**Option B: Migrate to Vitest**

- More aligned with Vite ecosystem
- `import { describe, it, expect } from 'vitest'` — different import pattern
- Would require touching all 75 test files
- Better long-term but higher effort

### Phase 3: Docker + CI (~2 hours)

**Validate before moving to Phase 4:**

- [ ] `docker build` frontend image succeeds locally
- [ ] Docker container serves the app correctly
- [ ] Commit + push (check CI results from Phase 1-2 pushes)
- [ ] If earlier CI failed, diagnose and fix before proceeding

**3.1 Update Dockerfile**

- If using `build.outDir: 'build'` in vite.config → no change needed
- Otherwise update `COPY --from=build /app/build` →
  `COPY --from=build /app/dist`

**3.2 Update CI workflows**

- `frontend.yml`: `npm run build` stays the same
- `build.docker-compose.yml`: verify frontend image build still works
- `e2e-playwright.yml`: verify `npm ci` + `npm run build` in shared build

**3.3 Update `.env.example`**

- No REACT*APP* vars to rename (confirmed zero usage)
- Vite uses `VITE_` prefix — document for future additions

### Phase 4: Import Resolution Fixes (~4 hours)

**Validate before moving to Phase 5:**

- [ ] `npm run build` still succeeds after import fixes
- [ ] `npm test` still passes
- [ ] `npm run format && npm run check-format` clean
- [ ] Commit + push

CRA (Webpack) allows:

- Extensionless imports: `import Foo from './Foo'` (resolves .js/.jsx/.ts/.tsx)
- Absolute imports via `jsconfig.json` / `tsconfig.json` `baseUrl`

Vite is stricter. May need:

- `resolve.extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json']` in
  vite.config
- Verify no bare `import './styles'` that rely on Webpack's CSS loader magic

### Phase 5: Validation (~4 hours)

**Local (run after each phase):**

1. `npm run build` — verify production build succeeds
2. `npm run start` — verify dev server works with HMR
3. `npm test` — run all 75 Jest tests
4. `npm run format && npm run check-format` — formatting clean

**CI (parallel — runs after each push):** 5. Monitor via `gh pr checks <PR#>`
after each push 6. Backend + Frontend checkpoints should pass after Phase 1 7.
E2E checkpoint validates the full Docker-built app after Phase 3 8. If CI fails,
diagnose from `gh run view <id> --log-failed` and fix forward

**Final validation:** 9. All CI checkpoints green (01-Backend, 02-Frontend,
03-E2E) 10. `npm audit --audit-level=high` shows 0 high/critical 11. Compare
bundle size: CRA vs Vite output (log in PR description) 12. Run E2E locally
against Vite-built app to confirm no regressions

## Files to Modify

| File                                        | Change                                                                  |
| ------------------------------------------- | ----------------------------------------------------------------------- |
| `frontend/package.json`                     | Replace react-scripts, add vite, update scripts, remove @svgr overrides |
| `frontend/package-lock.json`                | Regenerated                                                             |
| `frontend/vite.config.ts`                   | **New** — Vite configuration                                            |
| `frontend/index.html`                       | **Moved** from `public/` to root, add script tag                        |
| `frontend/public/index.html`                | **Deleted**                                                             |
| `frontend/jest.config.js`                   | **New** — standalone Jest config (if Option A)                          |
| `frontend/src/serviceWorkerRegistration.js` | `process.env.NODE_ENV` → `import.meta.env.MODE`                         |
| `frontend/src/setupTests.js`                | May need import adjustments                                             |
| `frontend/Dockerfile`                       | Verify/update build output path                                         |
| `.github/workflows/frontend.yml`            | Verify build commands still work                                        |

## Risk Assessment

| Risk                     | Likelihood | Mitigation                                 |
| ------------------------ | ---------- | ------------------------------------------ |
| Import resolution breaks | Medium     | `resolve.extensions` in vite.config        |
| CSS import differences   | Low        | Vite handles CSS natively                  |
| SVG import breaks        | Medium     | vite-plugin-svgr replaces @svgr overrides  |
| Jest tests break         | Low        | Standalone config + same jsdom environment |
| Docker build breaks      | Low        | Keep `outDir: 'build'` for compatibility   |
| HMR regression           | Very Low   | Vite HMR is more reliable than CRA's       |

## Estimated Effort

| Phase              | Hours                    |
| ------------------ | ------------------------ |
| Core migration     | 4                        |
| Jest configuration | 4                        |
| Docker + CI        | 2                        |
| Import resolution  | 4                        |
| Validation         | 4                        |
| **Total**          | **~18 hours (2-3 days)** |

## Verification

1. `npm run build` produces working production bundle
2. `npm test` — all 75 Jest tests pass
3. `npm run start` — dev server with HMR works
4. CI: Backend + Frontend + E2E checkpoints all green
5. `npm audit --audit-level=high` shows 0 high/critical (down from 6)
6. Bundle size comparison logged in PR description

## References

- [GitHub Issue #3312](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3312)
- [React blog: Sunsetting CRA](https://react.dev/blog/2025/02/14/sunsetting-create-react-app)
- [Vite Migration Guide](https://dev.to/solitrix02/goodbye-cra-hello-vite-a-developers-2026-survival-guide-for-migration-2a9f)
