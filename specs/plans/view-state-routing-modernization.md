# View-State & Routing Modernization Plan

## Context

OpenELIS-Global-2 puts **entity identity** in the URL (path params) but keeps
**view state** — filters, pagination, sort, active tab, search terms, selected
row — in component memory. The result: filtered/searched pages are not
shareable, not bookmarkable, do not survive a reload, and the browser Back
button does not restore prior view state. There is no shared convention for
URL-addressable view state, so the codebase has drifted into two divergent
one-off implementations and ~100 pages that do nothing.

This plan is the **view-state adoption** effort: define one convention + a small
hook toolkit, then apply it in value-ordered tiers so the codebase converges on
a pattern instead of accumulating per-page filter-persistence hacks.

### Relationship to the React 18 / Router v6 modernization

`specs/plans/react-18-frontend-modernization.md` **Phase 2** upgrades
react-router v5 → v6 (the routing _infrastructure_ — `useNavigate`, `<Routes>`,
and crucially `useSearchParams`). That is a separate, independent track.

- **This plan does not require v6.** The query-string layer is delivered on v5
  today via `useLocation` + `useHistory` + `URLSearchParams`, wrapped in the
  `useUrlFilters` hook (already shipped in PR #3732).
- **This plan does not block v6**, and v6 does not block it. The hooks
  encapsulate the router API, so a later v6 migration can re-back them with
  `useSearchParams` **without changing a single consumer**. That stable-API
  boundary is the whole point of the toolkit.

## Current State (surface audit, 2026-07-04)

Router: **react-router-dom `^5.2.0`**, `BrowserRouter`, ~88 flat routes plus a
nested order-workflow subtree. v6 APIs (`useNavigate`, `useSearchParams`) are
NOT available on v5.

### What is already "proper" — do not redo

- **Identity is in the path.** ~20 routes use path params for entity ids:
  `/PatientManagement/:patientId`, `/analyzers/:id`, `/Storage/.../:id`,
  `/PathologyCaseView/:pathologySampleId`, `/SampleShipment/box/:boxId`, etc.
  Read via `useParams()`. This half of proper routing is largely done.
- `location.state` misuse is minimal (one spot: Storage sample handoff in
  `storage/pages/ManageLocationPage.jsx`, with an `:id` fallback).

### The gap — view state is almost never in the URL

Of 130+ files that touch the URL, **only 2 do a full filter round-trip**:

| File                                                                    | Mechanism                                         | Note                                                    |
| ----------------------------------------------------------------------- | ------------------------------------------------- | ------------------------------------------------------- |
| `components/reports/vectorSurveillance/VectorSurveillanceDashboard.jsx` | `useUrlFilters`                                   | The reference implementation (PR #3732)                 |
| `components/analyzers/AnalyzersList.jsx`                                | hand-rolled `window.location` + `history.replace` | Divergent one-off — should migrate onto `useUrlFilters` |

### View-state surface (the change target)

| Category                         | ~Files | Representative pages                                                                                                                      |
| -------------------------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Search / filter query            | 40+    | `resultPage/SearchResultForm`, `validation/SearchForm`, `sampleManagement/SampleSearch`, `eOrder/EOrderSearch`, `modifyOrder/SearchOrder` |
| Tabs (in-memory `selectedIndex`) | 35+    | `reports/tat/TATReport`, `inventory/InventoryManagement`, `admin/complianceStandards/ComplianceStandardsAdmin`, notification-config tabs  |
| Pagination (copy-pasted scheme)  | 30+    | `admin/OrganizationManagement`, `admin/userManagement/UserManagement`, `reports/tat/TATDetailListTab`, `alerts/AlertsDashboard`           |
| Master-detail selection          | 25+    | `order/steps/sections/PatientSearchSection`, `alerts/AlertsDashboard`                                                                     |
| Sort                             | 12+    | `reports/tat/TATDetailListTab`                                                                                                            |

Rough triage: **~21 critical** (high-traffic search/results + key report/admin
lists + alerts), **~45 high** (tabbed pages, filtered admin lists), **~73
lower** (form/ephemeral state we deliberately leave in memory).

### Two cross-cutting anti-patterns (fix with primitives, not per-page)

1. **Duplicated pagination scheme.** 30+ pages copy the same tangle:
   `page` / `pageSize` / `paging` / `nextPage` / `previousPage` /
   `currentApiPage` / `totalApiPages`. A shared convention retires the copy.
2. **Tabs never linked to routes.** 35+ pages hold `selectedIndex` in state, so
   a tab is not deep-linkable. A `useUrlTab` hook fixes the class.

## The Convention (target: proper routing)

1. **Identity → path params.** Keep the existing norm (`/entity/:id`).
2. **View state → query string**, via the shared hooks: filters, pagination,
   sort, active tab, search terms.
3. **URL is the source of truth.** Components hydrate from the URL on mount and
   write back on a state change:
   - **Explicit-trigger pages** (Apply / Search / Generate): write on the
     deliberate action via `history.push` (a back-navigable, shareable entry);
     auto-run once on load when the link already carried filters.
   - **Auto-fetch pages** (query on every filter change): write via
     `history.replace` (no history spam); the existing fetch effect fires from
     the hydrated state.
4. **Retire direct `window.location.search` reads** in favor of `useLocation` /
   the hooks (consistency; one code path to migrate at v6).
5. **Object-shaped filters** (e.g. a selected patient) are flattened by the
   consumer to the scalar id that belongs in the URL; the display object is
   re-derived on hydration.

## The Toolkit (primitives)

| Primitive                             | Status                | Purpose                                                                                                                 |
| ------------------------------------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `useUrlFilters(defaults)`             | ✅ shipped (PR #3732) | Round-trips scalar + array (comma-joined id/enum) filters through the query string                                      |
| `useUrlFilterAutoRun(shouldRun, run)` | ✅ shipped (PR #3732) | Runs a report once on load when the link carried filters; mount-snapshot + ref guard prevent the double-fetch footgun   |
| `setUrlFilters(next, { replace })`    | ➕ add                | `replace` option for auto-fetch/list pages (avoid history spam)                                                         |
| `useUrlTab(key, tabs)`                | ➕ add                | Deep-linkable Carbon `<Tabs>` (`?tab=detail`)                                                                           |
| pagination convention                 | ➕ add                | `page` / `pageSize` as URL scalars on top of `useUrlFilters`; retire the copy-pasted cursor tangle where the API allows |

All primitives live in `frontend/src/components/common/` and are unit-tested
with `renderHook` + `MemoryRouter` (the pattern established in
`useUrlFilters.test.jsx`).

## Risk Assessment

| Risk                                                                                                | Likelihood | Impact | Mitigation                                                                                                               |
| --------------------------------------------------------------------------------------------------- | ---------- | ------ | ------------------------------------------------------------------------------------------------------------------------ |
| Uncontrolled Carbon inputs (Dropdown/DatePicker with only `onChange`) can't display hydrated values | Certain    | Medium | Bind `value`/`selectedItem`; the input-control fix is part of that page's conversion, done deliberately (not a shortcut) |
| History spam on auto-fetch pages                                                                    | Medium     | Low    | `replace` option                                                                                                         |
| Cursor-style pagination (`nextPage`/`currentApiPage`) is harder than `page`/`pageSize`              | Medium     | Medium | Convert per-page; some backend query shapes may need a page-index param — flag, don't force                              |
| Scope sprawl (100+ files)                                                                           | High       | Medium | Strict tiering; ship value-first; never big-bang; explicit NOT-doing list                                                |
| v6 migration reorders under the hooks                                                               | Low        | Low    | Hooks encapsulate the router API; consumer API stays stable                                                              |

## Strategy

Phase 0 (convention + toolkit + one reference migration) is foundational and
blocks the rest. Phases 1–3 are independent and value-ordered — each page
family ships as its own PR. Phase 4 is an optional post-v6 simplification. Every
converted page follows the same **per-page conversion recipe** (below), which is
what makes the rollout deterministic rather than bespoke.

### Per-page conversion recipe (deterministic)

1. Replace the filter/pagination/tab `useState` **initializers** with values
   hydrated from `useUrlFilters(defaults)` (+ `useUrlTab` for tabs).
2. Ensure every filter input is **controlled** (`value` / `selectedItem` bound)
   so hydrated values display; fix uncontrolled Carbon inputs here.
3. On the trigger, call `setUrlFilters(...)` — `push` for explicit-trigger
   pages, `{ replace: true }` for auto-fetch pages.
4. Explicit-trigger pages: add `useUrlFilterAutoRun(hasParams, run)` so a shared
   link runs the query once on load.
5. Add a focused test (`render` / `renderHook` + `MemoryRouter`): hydration from
   a URL, write-on-trigger, and no double-fetch / correct auto-run.

## Phases

### Phase 0 — Convention + toolkit + reference migration

**Goal:** finish the primitives and set the standard so every later phase is
mechanical.

**Steps:**

1. Add the `{ replace }` option to `setUrlFilters` in `useUrlFilters.js` (+ test).
2. Add `useUrlTab(key, tabs)` in `common/` (+ test).
3. Document the pagination convention (`page`/`pageSize` scalars) with an example.
4. Write the convention as a short doc (`frontend/src/components/common/README`
   or an AGENTS.md section) pointing at the reference implementation.
5. **Migrate `AnalyzersList.jsx`** off its hand-rolled `window.location` filter
   round-trip onto `useUrlFilters` — collapses the two divergent impls into one.

**Validation:** `AnalyzersList` behaves identically (share/reload); toolkit unit
tests green; convention doc merged.

**Estimated effort:** 1–2 days **Risk:** Low.

### Phase 1 — High-traffic search / results pages

**Goal:** make the most-visited pages shareable/reload-safe. They share the
cursor-pagination scheme, so convert them together with the pagination work.

**Files:** `resultPage/SearchResultForm.jsx` (+ `ResultSearch`),
`validation/SearchForm.jsx`, `sampleManagement/SampleSearch.jsx`,
`eOrder/EOrderSearch.jsx`, `modifyOrder/SearchOrder.jsx`.

**Validation:** per-page recipe test; manual share/reload of a search.

**Estimated effort:** 3–5 days **Risk:** Medium (cursor pagination + object
filters like selected patient).

### Phase 2 — Reports & dashboards

**Goal:** shareable filtered reports (this replaces the earlier ad-hoc "roll
`useUrlFilters` out to the reports" idea, now framed as one tier of a convention).

**Files:** `reports/tat/TATReport.jsx` (+ `TATDetailListTab` pagination/sort +
tabs via `useUrlTab`), `compliance/EnvironmentalDashboard.jsx` (auto-fetch →
`replace`), `reports/auditTrailReport/SystemAuditEvents.jsx` (pagination +
uncontrolled-input fix), `alerts/AlertsDashboard.jsx`,
`eqa/EQAManagementDashboard.jsx` + `EQAResultsPage.jsx`,
`admin/complianceStandards/ComplianceStandardsAdmin.jsx`.

**Note:** `reports/common/ReportByDate.jsx` opens a server-rendered window
rather than an in-page report — client URL-state adds little; convert only its
in-page form selections if at all.

**Estimated effort:** 4–6 days **Risk:** Medium (heterogeneous trigger models;
uncontrolled inputs in `SystemAuditEvents`).

### Phase 3 — Tabbed admin/config + filtered lists

**Goal:** deep-linkable tabs (`useUrlTab`) across the 35+ tabbed pages, and
pagination/filter URL state on the filtered admin lists.

**Files (representative):** `admin/OrganizationManagement`,
`admin/userManagement/UserManagement`, `admin/testManagement/ViewTestCatalog`,
`inventory/InventoryManagement` (+ `InventoryCatalog`/`InventoryList`),
notification-config tabs.

**Estimated effort:** 5–8 days (breadth) **Risk:** Low-Medium (repetitive once
`useUrlTab` + pagination convention exist).

### Phase 4 — Post-v6 simplification (depends on modernization Phase 2)

**Goal:** after react-router v6 lands, re-back the hooks with `useSearchParams`
and remove the remaining direct `window.location.search` reads. Consumer API is
unchanged, so this is internal to the toolkit.

**Estimated effort:** 1 day **Risk:** Low.

## Sequencing & Dependencies

- **Phase 0 blocks** 1–3 (primitives + convention must exist first).
- **Phases 1–3 are independent** and value-ordered — do the highest-traffic
  family first; each is its own PR.
- **Phase 4 depends on** `react-18-frontend-modernization.md` Phase 2 (v6). It
  is optional and does not gate 0–3.
- View-state adoption and the v6 migration are **mutually non-blocking** (the
  hook boundary decouples them).

## PR Strategy

One PR per phase slice / page family; each is independently shippable and
tested. No big-bang PR. Phase 0 ships as one small foundational PR; each Phase
1–3 family is a separate stacked or independent PR onto `develop`.

## Total Estimated Effort

~14–22 days across Phases 0–3 (Phase 4 +1 day, gated on v6). Value is
front-loaded: Phase 0 + Phase 1 (≈5–7 days) deliver the shared standard plus the
highest-traffic shareable pages.

## What We're NOT Doing (Scope Boundaries)

- **Not migrating to react-router v6 here** — that is the modernization plan
  (Phase 2). This plan runs on v5 and is v6-ready by design.
- **Not persisting ephemeral UI state** — modal open/close, input focus, loading
  flags stay in memory.
- **Not changing backend query APIs**, except where cursor-style pagination
  genuinely needs a page-index param (flagged per-page, not assumed).
- **Not converting all ~100 files** — strictly tiered, value-first; the ~73
  lower-priority form/ephemeral pages are explicitly out of near-term scope.
- **Not touching identity routing** — path params are already correct.

## References

- `specs/plans/react-18-frontend-modernization.md` — Phase 2 (react-router
  v5→v6) is the enabler for Phase 4 here.
- PR #3732 — `useUrlFilters` + `useUrlFilterAutoRun` + the vector dashboard: the
  reference implementation and tests.
- `frontend/src/components/common/useUrlFilters.js` — the toolkit home.
- `specs/016-unified-app-navigation/` — related but distinct (sidebar menu, not
  view-state).
