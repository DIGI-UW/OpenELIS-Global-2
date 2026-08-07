# R1 Authoritative Visual Comparison

**Compared:** 2026-08-06  
**OpenELIS Work authority:**
[`a1f720d7b3b0`](https://github.com/DIGI-UW/openelis-work/commit/a1f720d7b3b01db63387361495f4aa6589105003)  
**OpenELIS evidence head:** `3ea6d82c1`

## Evidence Method

The authoritative HTML walkthrough was run locally from its pinned revision and
captured at 1440 by 1000. The current OpenELIS branch was exercised through the
registered Playwright projects with property-gated, service-created scenarios.
Desktop captures use the same 1440 by 1000 viewport; mobile captures use the
registered Pixel 5 project. Screenshots wait for named controls and, for the
worklist, the exact seeded row. There are no sleeps, fixed primary keys, SQL
fixtures, forced actions, or test-only UI branches.

The external comparison bundle is at `/private/tmp/ogc782-r1-visual/` on the
review machine. Binary evidence is intentionally not committed.

## Verdict

The remediation is ready for product review. The captured desktop and mobile
surfaces have no blocking clipping, overlap, blank-state, or navigation defect.
They preserve the authoritative workflow outcomes while using the current
OpenELIS shell and Carbon components rather than copying the prototype chrome.

Human UAT remains authoritative for whether the integrated order-entry
composition is sufficiently clear. Automated evidence does not turn visual
adaptations into accepted product deviations.

## Drift Ledger

| Source screen               | Current behavior and visual result                                                                                                                                                                                                                                                                                                                             | Disposition                                                                                                                                                                                                                                            |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| M-03 Order entry            | Selecting a typed culture test derives and visibly locks Program = Microbiology, presents Culture Method, Patient Origin, sets, history, and notification preference, and carries them into the routed case. The fields live inside the supported Enter Order page rather than a new standalone Program Selection step.                                        | Workflow aligned; intentional shell/composition adaptation. The selected test is lower in the existing form than the derived Program panel, so human UAT must judge discoverability. Do not add a second order-entry route solely to imitate the mock. |
| M-04 Case workbench         | The case header, sibling workflow link, stage, progress rail, next step, current action, timeline, isolate, AST, critical, report, and amendment sections are present. One URL-addressed action section is mounted at a time.                                                                                                                                  | Workflow aligned; intentional interaction adaptation. Mounting all expanded prototype cards would duplicate hidden work and weaken the canonical `section` state.                                                                                      |
| M-05 AST                    | Ordered panel, Method, versioned breakpoint standard, reagent/card lots, multiple readings, matched basis, interpretation, override/revert history, analyzer provenance/QC, review, and repeat paths are implemented. The captured state emphasizes the current manual/reagent workflow rather than reproducing the mock's analyzer-complete example verbatim. | Behavior aligned; visual state differs by run source and lifecycle. Analyzer-specific acceptance is covered by functional tests and remains a separate UAT story.                                                                                      |
| M-07 Worklist               | A single Carbon worklist exposes Culture and AST grains, source-aligned summary cards, canonical filters/search/sort/page state, due action, priority, latest actor, exact case/run navigation, refresh status, resistance summary, and recent activity.                                                                                                       | Workflow aligned. The mock's `Tech` and `My Cases only` controls are omitted because the authoritative narrative defines a shared queue and the product source does not define case assignment ownership.                                              |
| M-11 Critical communication | Communication is inline in the case, supports Case/Isolate/Sample Item/Result targets, records recipient/contact/method/message/follow-up, and synchronizes open/acknowledged/closed state with Alerts.                                                                                                                                                        | Workflow aligned. The visual scan captures the empty form; functional Playwright covers the complete lifecycle. Mock references to a notifications feed and named service are implementation leakage, not UI requirements.                             |
| M-12 Reagent linkage/picker | The same Carbon picker is rendered in culture and AST, shows every eligible and blocked lot, QC state, expiry reason, FEFO recommendation, exact lot search/scan, quantity, and save-time conflict behavior.                                                                                                                                                   | Workflow aligned. Linkage administration remains owned by shared Test Catalog/Inventory. The mock's proposed schema, class, and component names are non-binding implementation leakage.                                                                |

## Source Health Findings

- The prototype walkthrough chrome, step counter, black/blue banners, fake user,
  and Prev/Next controls explain the design; they are not OpenELIS application
  requirements.
- M-11 prescribes a feed and service name, and M-12 prescribes entities,
  component signatures, and storage behavior. Those details should move to an
  engineering crosswalk. Product artifacts should retain only workflow,
  business rules, and observable outcomes.
- The M-NFR numeric timing values are not visible workflow requirements and do
  not define a reproducible environment. They remain diagnostic engineering
  baselines, not visual/product merge gates.

## Automated Result

- Full desktop/mobile microbiology accessibility suite: `15 passed`.
- Final source-comparison subset after stable-capture changes: `7 passed`.
- Final M-12 culture/AST subset: `3 passed`.
- Worklist capture initially exposed a nondeterministic first-page assumption;
  the test now scopes the canonical `q` state to its service-created accession
  and waits for the exact row.
- The known shared-shell URL-less `404` remains visible in console diagnostics
  and is not caused by these microbiology surfaces.
