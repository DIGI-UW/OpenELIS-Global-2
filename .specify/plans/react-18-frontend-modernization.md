# React 18 Frontend Modernization Plan

> **⚠️ Superseded for execution by** >
> [`pr-3448-react-18-unified-plan.md`](./pr-3448-react-18-unified-plan.md)
> (2026-04-18). This document remains as the strategic/architectural reference,
> but several assumptions are outdated:
>
> - **Vitest:** #3349 migrated Jest → Vitest (this doc said Vitest was
>   "optional"). #3448 must adopt Vitest.
> - **Carbon v11:** The Vite PR did NOT upgrade Carbon; the bump is actually in
>   #3448 itself.
> - **Phasing:** The 5-PR phased approach described here was collapsed into a
>   single squashed #3448. Execution now follows the unified plan.
>
> For any work landing #3448, use the unified plan. Refer back here only for
> historical context on the original strategy.

## Context

OpenELIS-Global-2 runs React **17.0.2** (released Oct 2020) with React Router
**v5** and several deprecated patterns. React 17 is no longer maintained — React
18 (Mar 2022) and React 19 (Dec 2024) have shipped. The longer we wait, the
harder the migration and the further behind our dependency ecosystem falls.

### Prerequisite

**PR #3349 / Issue #3312 — CRA → Vite migration** must merge first. It replaces
the abandoned Create React App build toolchain with Vite 8, upgrades Carbon to
v1.104, and brings Jest to v29. The Vite migration deliberately does NOT change
the React version — that is this plan's scope.

## Current State (as of 2026-04-07)

### Core Dependencies

| Package                      | Current  | Target    | Breaking? |
| ---------------------------- | -------- | --------- | --------- |
| react                        | ^17.0.2  | ^18.3.1   | Yes       |
| react-dom                    | ^17.0.2  | ^18.3.1   | Yes       |
| react-router-dom             | ^5.2.0   | ^6.28     | Yes       |
| @testing-library/react       | ^9.5.0   | ^16.1     | Yes       |
| @testing-library/react-hooks | ^8.0.1   | (removed) | Yes       |
| react-intl                   | ^5.20.12 | ^7.1      | Yes       |
| react-i18next                | 11.x     | 15.x      | Yes       |
| eslint-plugin-react-hooks    | ^4.6.0   | ^5.1      | Minor     |
| @carbon/react                | ^1.104.1 | (keep)    | No        |
| @carbon/charts-react         | ^1.27.3  | (keep)    | No        |

### Migration Surface Audit

| Pattern                        | Files | Occurrences | Migration Action                         |
| ------------------------------ | ----- | ----------- | ---------------------------------------- |
| `ReactDOM.render`              | 1     | 1           | → `createRoot`                           |
| `injectIntl` (deprecated HOC)  | 93    | 183         | → `useIntl()` hook                       |
| `useHistory` (Router v5)       | 29    | 51          | → `useNavigate()` (Router v6)            |
| `<Switch>` (Router v5)         | 1     | 1           | → `<Routes>` (Router v6)                 |
| `<Route>` definitions          | 3     | 35          | → v6 `<Route element={}>` syntax         |
| `<Redirect>`                   | 1     | 1           | → `<Navigate>` (Router v6)               |
| `useIntl` / `FormattedMessage` | 250   | 2181        | Already modern — no change needed        |
| Class components               | 1     | 1           | RouteErrorBoundary — convert to function |
| `findDOMNode`                  | 0     | 0           | None needed                              |
| String refs                    | 0     | 0           | None needed                              |

### What Carbon v1.104 Already Gives Us

The Vite migration (PR #3349) upgraded Carbon from v1.15 to v1.104. Carbon
v1.104 **already supports React 18** — it uses hooks internally and has no
`findDOMNode` or legacy lifecycle calls. This means the Carbon components will
work seamlessly after the React 18 upgrade with no additional changes.

## Risk Assessment

| Risk                                        | Likelihood | Impact | Mitigation                                     |
| ------------------------------------------- | ---------- | ------ | ---------------------------------------------- |
| `injectIntl` removal breaks 93 files        | Certain    | High   | Codemod + phased rollout                       |
| React Router v6 route restructuring         | Certain    | High   | Isolated to 3 routing files                    |
| Strict Mode double-render exposes bugs      | Medium     | Medium | Enable incrementally, fix side effects         |
| `@testing-library/react` API breaks tests   | Certain    | Medium | Well-documented migration path                 |
| Third-party libs incompatible with React 18 | Low        | Medium | react-confirm-alert, react-data-table assessed |
| Concurrent rendering breaks assumptions     | Low        | Low    | React 18 is opt-in concurrent by default       |

## Strategy: Two Independent Tracks

The two biggest migrations — **react-intl** (93 files) and **React Router** (3
core files + 29 `useHistory` consumers) — are **independent of each other** and
independent of the React 18 core upgrade. This enables parallel work and
smaller, reviewable PRs.

## Phases

### Phase 0: React 18 Core (Smallest Possible Change)

**Goal:** Upgrade react/react-dom to 18, change the entry point, verify
everything works.

**Why first:** React 18 is backward-compatible with React 17 patterns. The
`createRoot` API is the only required change. Everything else (Strict Mode,
concurrent features, Suspense) is opt-in. This means we can upgrade the core
without touching any component code.

**Changes:**

1. `frontend/package.json`:
   - `react`: `^17.0.2` → `^18.3.1`
   - `react-dom`: `^17.0.2` → `^18.3.1`
   - `@types/react`: add `^18.3` (for TS files)
   - `@types/react-dom`: add `^18.3`
2. `frontend/src/index.jsx`:

   ```jsx
   // Before (React 17)
   import ReactDOM from "react-dom";
   ReactDOM.render(<App />, document.getElementById("root"));

   // After (React 18)
   import { createRoot } from "react-dom/client";
   const root = createRoot(document.getElementById("root"));
   root.render(<App />);
   ```

3. `frontend/src/setupTests.js`: update any React 17-specific test setup
4. Remove `@testing-library/react-hooks` (merged into `@testing-library/react`
   in v13+)
5. Upgrade `@testing-library/react`: `^9.5.0` → `^16.1`

**Validation:**

- [ ] `npm run build` succeeds
- [ ] `npm test` — all 83 test suites pass (some may need `act()` wrapping)
- [ ] App loads in browser
- [ ] CI green (all checkpoints)

**Estimated effort:** 1-2 days **Risk:** Low — React 18 is explicitly
backward-compatible with 17 patterns.

### Phase 1: Remove `injectIntl` (93 files)

**Goal:** Replace the deprecated `injectIntl` HOC with the `useIntl()` hook
across all 93 files.

**Why:** `injectIntl` wraps components in a HOC that injects the `intl` object
as a prop. The modern `useIntl()` hook does the same thing without the wrapper.
This is a mechanical transformation that can be largely automated.

**Pattern:**

```jsx
// Before
import { injectIntl } from "react-intl";
const MyComponent = ({ intl, ...props }) => {
  const label = intl.formatMessage({ id: "my.key" });
  return <div>{label}</div>;
};
export default injectIntl(MyComponent);

// After
import { useIntl } from "react-intl";
const MyComponent = (props) => {
  const intl = useIntl();
  const label = intl.formatMessage({ id: "my.key" });
  return <div>{label}</div>;
};
export default MyComponent;
```

**Approach:**

1. Write a codemod (jscodeshift or manual script) to automate the transform
2. Run across all 93 files
3. Manual review for edge cases (components that forward `intl` as props, HOC
   composition chains like `injectIntl(withRouter(Component))`)
4. Run tests after each batch

**Can be split into PRs by directory:**

- `components/admin/` (largest group)
- `components/reports/`
- `components/addOrder/` + `components/modifyOrder/`
- Everything else

**Estimated effort:** 2-3 days (mostly mechanical) **Risk:** Low — 250 files
already use `useIntl`, so the pattern is established.

### Phase 2: React Router v5 → v6 (3 core + 29 consumer files)

**Goal:** Upgrade from React Router v5 to v6.

**Why:** React Router v5 is in maintenance mode. v6 has a completely different
API but better nested routing, data loading, and TypeScript support.

**Core routing changes (3 files):**

| File                                  | Change                                                                                                   |
| ------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `App.jsx`                             | `<Switch>` → `<Routes>`, `<Route component={X}>` → `<Route element={<X/>}>`, `<Redirect>` → `<Navigate>` |
| `components/security/SecureRoute.jsx` | Rewrite auth guard for v6 API                                                                            |
| `components/admin/Admin.jsx`          | 32 `<Route>` definitions → v6 syntax                                                                     |

**Consumer changes (29 files):**

| Pattern         | Count    | Migration                                                                                  |
| --------------- | -------- | ------------------------------------------------------------------------------------------ |
| `useHistory()`  | 29 files | → `useNavigate()` — `history.push(x)` → `navigate(x)`, `history.goBack()` → `navigate(-1)` |
| `useLocation()` | (keep)   | API unchanged in v6                                                                        |
| `useParams()`   | (keep)   | API unchanged in v6                                                                        |

**Approach:**

1. Upgrade `react-router-dom`: `^5.2.0` → `^6.28`
2. Rewrite `App.jsx` routing structure (this is the hardest part)
3. Rewrite `SecureRoute.jsx` auth guard
4. Rewrite `Admin.jsx` route definitions
5. Find-and-replace `useHistory` → `useNavigate` across 29 files
6. Update any `withRouter` usage (currently 0 — clean)

**Estimated effort:** 2-3 days **Risk:** Medium — routing is load-bearing.
Thorough E2E testing required.

### Phase 3: react-intl v5 → v7

**Goal:** Upgrade react-intl to the latest major version.

**Why:** react-intl v5 works with React 18, but v6/v7 have performance
improvements and better TypeScript support. This is lower priority than Phases
0-2.

**Breaking changes in v6+:**

- `IntlProvider` changes (minimal — we already use it correctly)
- Some formatter API changes
- Default `textComponent` changed from `<span>` to `<></>` (React.Fragment)

**Estimated effort:** 1 day **Risk:** Low — mostly internal improvements.

### Phase 4: Testing Library Modernization

**Goal:** Upgrade `@testing-library/react` from v9 to v16.

**Note:** Phase 0 already upgrades to v16 as a necessity (v9 doesn't support
React 18's `createRoot`). This phase handles any remaining test fixes that
weren't caught in Phase 0.

**Key changes:**

- `@testing-library/react-hooks` is removed (hooks testing is built into
  `@testing-library/react` v13+)
- `renderHook` imported from `@testing-library/react` instead of separate
  package
- Queries return `null` instead of throwing for missing elements
- `act()` wrapping is stricter

**Estimated effort:** 1-2 days (may overlap with Phase 0) **Risk:** Medium —
test changes are tedious but not architecturally risky.

### Phase 5: Enable Strict Mode (Optional, Recommended)

**Goal:** Enable `<React.StrictMode>` in development to catch side-effect bugs.

**Note:** Strict Mode is already in `index.jsx` wrapping `<App />`. React 18
Strict Mode is stricter than React 17's — it double-invokes effects in
development to catch cleanup bugs. This may surface existing bugs.

**Approach:**

1. Enable in dev, monitor console for warnings
2. Fix double-render issues (usually missing effect cleanup)
3. This is ongoing cleanup, not a blocking phase

**Estimated effort:** Ongoing (1-2 days initial pass) **Risk:** Low — dev-only,
no production impact.

## Sequencing and Dependencies

```
PR #3349 (Vite 8)  ──┐
                      ├──> Phase 0 (React 18 core)
                      │         │
                      │         ├──> Phase 1 (injectIntl removal)  [independent]
                      │         ├──> Phase 2 (Router v5 → v6)      [independent]
                      │         └──> Phase 4 (Testing lib)         [with Phase 0]
                      │
                      │    Phase 1 + Phase 2 ──> Phase 3 (react-intl v7)
                      │
                      └──> Phase 5 (Strict Mode) [anytime after Phase 0]
```

**Phases 1 and 2 are independent and can run in parallel on separate branches.**

## PR Strategy

Each phase should be its own PR for reviewability:

| PR  | Phase | Title                                                      | Est. Files Changed   |
| --- | ----- | ---------------------------------------------------------- | -------------------- |
| 1   | 0     | `refactor(frontend): upgrade React 17 → 18`                | ~5 core + test fixes |
| 2   | 1     | `refactor(frontend): replace injectIntl with useIntl hook` | ~93                  |
| 3   | 2     | `refactor(frontend): migrate React Router v5 → v6`         | ~32                  |
| 4   | 3     | `chore(frontend): upgrade react-intl v5 → v7`              | ~5                   |
| 5   | 4     | `fix(test): modernize testing-library usage`               | ~20                  |

## Total Estimated Effort

| Phase     | Effort         |
| --------- | -------------- |
| 0         | 1-2 days       |
| 1         | 2-3 days       |
| 2         | 2-3 days       |
| 3         | 1 day          |
| 4         | 1-2 days       |
| 5         | ongoing        |
| **Total** | **~8-12 days** |

With Phases 1 and 2 running in parallel: **~5-8 days wall clock**.

## What We're NOT Doing (Scope Boundaries)

- **React 19 upgrade** — React 18 first, assess 19 later. React 19 has more
  breaking changes (compiler, new hooks) that warrant separate planning.
- **TypeScript migration** — Tracked separately. The React 18 upgrade is
  JS-compatible.
- **Vitest migration** — Jest 29 works well with Vite. Vitest migration is
  optional and separate.
- **State management overhaul** — No Redux/Zustand introduction. Current
  Context-based state works fine.
- **React Server Components** — Not applicable to our SPA architecture.

## References

- [React 18 Upgrade Guide](https://react.dev/blog/2022/03/08/react-18-upgrade-guide)
- [React Router v5→v6 Migration](https://reactrouter.com/upgrading/v5)
- [react-intl v5→v6 Migration](https://formatjs.io/docs/react-intl/upgrade-guide-5x-to-6x)
- [Testing Library React Migration](https://testing-library.com/docs/react-testing-library/migrate-v13)
- Prerequisite: `.specify/plans/cra-to-vite-migration.md`
- GitHub Issue: #3312 (Vite migration), TBD (React 18 upgrade)
