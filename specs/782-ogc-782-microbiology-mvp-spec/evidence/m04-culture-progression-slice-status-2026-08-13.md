# M-04 Culture Progression Slice Status

**Pinned functional source:**
[`DIGI-UW/openelis-work@bf51582766ea`](https://github.com/DIGI-UW/openelis-work/blob/bf51582766ea/designs/microbiology/m-04-case-workbench-core.md)
(`m-04-case-workbench-core.md` v2.0 and
`m-04-case-workbench-prototype.html`)

**Slice boundary:** This checkpoint covers the positive-culture path from a
received case through primary inoculation, subculture, a manual observation,
and a positive signal. Isolate identification, AST, release, late growth, and
the no-growth final-report lifecycle remain separate outcomes.

**Status vocabulary:** `Specified`, `Implemented`, `Automated`, `Deployed`, and
`Human reviewed` are independent claims. A routed panel or component test is
not evidence that a reviewer can complete the story from the worklist.

## Observable Behavior Inventory

| ID | Observable behavior from the source and mock | Specified | Implemented | Automated | Deployed | Human reviewed | Current evidence or next action |
|---|---|---|---|---|---|---|---|
| M04-CP-01 | From the Microbiology worklist, a reviewer can find the deployment-scoped accession and open its Bacteriology case while retaining worklist query state. | Yes | Yes | Yes | No | No | The focused `core-app` journey starts at the accession-filtered worklist, opens the exact row, and asserts the retained query plus focused Setup section. |
| M04-CP-02 | A received case opens on Inoculation/Setup as the current step, never on Timeline or blindly at the top. | Yes | Yes | Yes | No | No | Current-step component coverage and the focused local browser journey pass against a fresh service-created case. |
| M04-CP-03 | The Inoculation section offers inline `Start inoculation`; saving a primary bottle/plate records its identifier, media, incubation, atmosphere, and eligible reagent lot. | Yes | Yes | Yes | No | No | Service/component coverage plus the focused browser journey select `UAT-MICRO-MEDIA-FEFO` through a Carbon keyboard interaction and verify recorded lot usage. |
| M04-CP-04 | Saving the first inoculation atomically advances `Received` to `Incubating` and creates an automatic Timeline event with the recorded values, actor, and time. | Yes | Yes | Yes | No | No | Service tests cover stage/event/actor/lot atomically; Timeline now renders actor and semantic time; component and browser coverage pass. |
| M04-CP-05 | Once primary media exists, `Add subculture` is available inline and requires a parent; the resulting row visibly preserves parent lineage. | Yes | Yes | Yes | R2 | No | Service, component, and browser coverage exists. |
| M04-CP-06 | Saving a subculture creates an automatic Timeline event; users do not manually recreate inoculation events in Timeline. | Yes | Yes | Yes | R2 | No | Timeline exposes only `Add note`; typed inoculation and subculture events are projected as Auto. |
| M04-CP-07 | `Add note` records a separate manual observation and labels it Manual rather than Auto. | Yes | Yes | Yes | R2 | No | Component and browser coverage exists. |
| M04-CP-08 | While a case is Incubating, the Inoculation section visibly offers `Mark positive` and `Mark no growth`. | Yes | Yes | Yes | No | No | The stage-gated actions reuse the existing routed confirmation; focused Carbon and browser coverage pass. |
| M04-CP-09 | The Incubating next-step banner explains the positive/no-growth choice and offers `Mark positive` as the primary next action. | Yes | Yes | Yes | No | No | The banner uses source-aligned guidance and a URL-backed Mark positive action; focused case-view and browser coverage pass. |
| M04-CP-10 | Selecting `Mark positive` opens an inline confirmation with canonical `section=setup&action=mark-positive` URL state; cancel or completion clears only the action. | Yes | Yes | Yes | R2 | No | Existing route and component tests prove the panel mechanics, but not a case-page entry point. |
| M04-CP-11 | Confirming positive advances `Incubating` to `Positive signal`, creates an audited automatic event, and keeps positive signal distinct from confirmed growth. | Yes | Yes | Yes | No | No | The service stage-distinction test and exact local user path pass. Deployed proof remains T314. |
| M04-CP-12 | At Positive signal, guidance points to subculture/Gram stain and does not claim that growth is already confirmed. | Yes | Yes | Yes | No | No | The exact local journey asserts the post-confirmation stage and guidance. |
| M04-CP-13 | All entry actions are inline Carbon interactions, keyboard reachable, focus-managed, and do not rely on arbitrary waits or forced browser actions. | Yes | Yes | Yes | No | No | The final 2/2 browser run uses Carbon roles/labels, focus + Space for the lot radio, and readiness assertions; no forced action or arbitrary wait is present. |

## Local Validation

- Backend: 20/20 focused JUnit 4 service/controller tests pass on Java 21.
- Frontend: 38/38 focused Carbon interaction and route tests pass.
- Browser: Playwright setup plus the focused `core-app` user journey pass 2/2
  in 7.7 seconds against the local R2 stack.
- Browser path: filtered worklist -> received case -> FEFO-backed primary
  inoculation -> subculture lineage -> automatic Timeline and manual note ->
  Mark positive confirmation -> Positive signal guidance.
- Remote deployment and human review remain separate open statuses.

## Drift And Interpretation

1. **Missing case-page action entry points:** the source and mock place culture
   actions in Inoculation and the next-step banner. The repository currently
   places them only in the worklist overflow menu. This is the blocking defect
   for this slice.
2. **Timeline wording:** AC-M04-04 says there is no inoculation entry "in the
   Timeline" while the same criterion and mock require an automatic Timeline
   event. The consistent observable interpretation is that Timeline has no
   manual inoculation-entry action; saved inoculations still appear as Auto
   history.
3. **No-growth lifecycle:** the source transitions directly to
   `NO_GROWTH_FINAL` and produces a final negative report. The repository uses
   `NO_GROWTH_READY` and a separate report-release step. This is a real product
   behavior divergence. It is recorded for a dedicated no-growth outcome slice
   and is not silently changed during the positive-culture slice.
4. **Route spelling:** the source uses a conceptual lower-case singular route;
   the application uses its established `/Microbiology/cases/:caseId` route.
   Deep linking and state preservation are observable requirements; the literal
   route spelling is implementation guidance and does not require churn.

## Focused Technical Gate

This slice is technically ready only when all of the following are true:

- focused backend tests pass for atomic primary/subculture persistence,
  authenticated audit actor, and positive-signal stage distinction;
- component tests prove stage-gated Inoculation actions, the next-step action,
  canonical URL state, keyboard/focus behavior, and Timeline labels;
- registered `core-app` Playwright starts at the worklist, uses a service-created
  fresh case, selects the eligible FEFO lot, and completes the exact positive
  progression without arbitrary waits;
- the exact commit is deployed and the same story passes against
  `amr.openelis-global.org`;
- Grist `AMR-S02` names the complete navigation path, exact disposable fixture,
  shared-record rule, controls, inputs, and one observable expected outcome per
  step;
- `Human reviewed` remains `No` until Piotr records the marks in the overlay.
