# OGC-782 Worklist UX Follow-up

> **Superseded scope note (2026-08-06):** This checkpoint predates the pinned
> OpenELIS Work authority audit. M-07 explicitly requires one shared page with
> Culture and AST grains plus the clinical context columns below. Those items
> are requirements, not product questions or optional future scope. Current
> implementation status is tracked in
> `openelis-work-authoritative-alignment-2026-08-05.md`.

Reviewed on 2026-07-28 against the current local OGC-782 stack and the
[M-07 worklist prototype](https://digi-uw.github.io/openelis-work/designs/microbiology/m-07-worklists-prototype.html).

## Outcome

The MVP worklist now presents the shared laboratory queue as a usable,
responsive OpenELIS page:

- configured Microbiology navigation is locked alongside the queue on desktop;
- compact viewports start with the navigation drawer closed;
- the worklist has linkable breadcrumbs, a bookmarkable filter/sort state,
  work-state counters, filters, and direct case actions;
- the clinical table remains contained on mobile and is horizontally scrollable
  without widening the page.

These changes use the existing OpenELIS Carbon shell and components. The M-07
prototype is a workflow and interaction reference, not an implementation
contract.

## Evidence

Local, uncommitted screenshots were captured only after their rendered state
was stable:

| View            | Evidence                                                                 | Verified state                                                                                                   |
| --------------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| Desktop, 1440px | `/private/tmp/ogc-782-worklist-evidence/worklist-desktop-final.png`      | Navigation begins at 256px; all seven queue counters align in one row; table fits the content region.            |
| Mobile, 390px   | `/private/tmp/ogc-782-worklist-evidence/worklist-mobile-final.png`       | Navigation begins closed; document width is 390px; the 1056px table is contained in a 358px horizontal scroller. |
| M-07 reference  | `/private/tmp/ogc-782-worklist-evidence/m07-worklist-mock-reference.png` | Used for the comparison below; not copied into the repository.                                                   |

Validation completed against the local stack at `https://localhost:48443`:

```text
cd frontend && npm test -- --run \
  src/components/microbiology/__tests__/MicrobiologyWorklist.test.jsx \
  src/components/microbiology/MicrobiologyRoutes.test.js \
  src/components/layout/Layout.test.jsx
# 3 files, 24 tests passed

cd frontend && BASE_URL=https://localhost:48443 npm run pw:test -- \
  --project=core-app \
  playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts
# 3 tests passed
```

The Playwright journey proves configured navigation, compact navigation state,
contained mobile table behavior, canonical query strings, critical
communication visibility, and breadcrumb return to the filtered worklist.

## M-07 Comparison And Scope Calls

| M-07 element                                                                | Classification               | MVP decision                                                                                                                                                                                            |
| --------------------------------------------------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Shared queue, counters, filtering, urgency, due action, and case navigation | Core workflow                | Implemented with the standard OpenELIS shell, Carbon DataTable, tags, filters, pagination, breadcrumbs, and stable URLs.                                                                                |
| Entire-row navigation                                                       | Interaction-shape difference | The MVP exposes the sample-item link and an explicit case-action icon. This preserves clear keyboard and table semantics while satisfying the workflow outcome.                                         |
| Cultures / AST-runs switch                                                  | Authoritative requirement    | Superseded: implemented in R1 as one shared page with Culture-case and AST-run grains.                                                                                                                  |
| Patient, specimen, laboratory number, and last-activity columns             | Authoritative requirement    | Superseded: implemented in `b7b025f98` as bounded read-time projections from existing authoritative records; no clinical context is fabricated or copied into microbiology tables.                      |
| Resistance strip and folded recent activity                                 | Product gap, V2              | Useful operational views, but outside the agreed MVP workflow. They need independently stated user outcomes and acceptance criteria.                                                                    |
| Fixed 30-second auto-refresh                                                | Implementation leakage       | The mock expresses freshness intent but must not mandate a timer, polling mechanism, or transport. A future product requirement should state when users need to know that queue information is current. |

## Issues Found

| Issue                                                                                                                       | Status                            | Resolution or follow-up                                                                                                                                         |
| --------------------------------------------------------------------------------------------------------------------------- | --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Worklist spacing collapsed because this Carbon build does not expose the CSS custom properties used by the page stylesheet. | Resolved                          | Local token fallbacks keep the page aligned with Carbon values without a framework upgrade.                                                                     |
| The table widened the complete mobile page instead of using its own scroll region.                                          | Resolved                          | The Carbon table container and a dedicated inner table surface now have shrink/overflow boundaries; Playwright asserts both conditions.                         |
| The default locked Microbiology sidenav covered compact worklist screens.                                                   | Resolved                          | New compact viewport default is closed; saved user preferences remain respected.                                                                                |
| The local reverse-proxy development stack emits Vite HMR WebSocket errors through `https://localhost:48443`.                | Accepted local-environment caveat | The page and all focused Playwright assertions work; resolve this in the deployment/developer-experience spike rather than adding feature-specific workarounds. |

## Guardrail

Any later M-07 follow-up must add product behavior to `spec.md` first. API,
schema, polling, component, and ownership choices belong in engineering planning
artifacts, not in the M-07-derived product requirement.
