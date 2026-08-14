# M-03 Order Workflow Slice Status

**Pinned functional source:**
[`DIGI-UW/openelis-work@bf51582766ea`](https://github.com/DIGI-UW/openelis-work/blob/bf51582766ea/designs/microbiology/m-03-order-entry-micro-hook.md)
(`m-03-order-entry-micro-hook.md` v2.2 and `m-03-order-entry-step1.html`)

**Status vocabulary:** `Specified`, `Implemented`, `Automated`, `Deployed`, and
`Human reviewed` are independent claims. A checked implementation is not a
claim that the behavior is deployed or accepted.

## Observable Behavior Inventory

| ID     | Observable behavior from the source and mock                                                                                             | Specified | Implemented | Automated                                          | Deployed            | Human reviewed | Current evidence or next action                                                                                                                                                                                                                   |
| ------ | ---------------------------------------------------------------------------------------------------------------------------------------- | --------- | ----------- | -------------------------------------------------- | ------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| M03-01 | Selecting a typed culture test visibly derives and locks Program = Microbiology.                                                         | Yes       | Yes         | Yes                                                | `8da391a5041a`       | No             | R1 `b563bf293` gives pending and collected order selections the same complete test projection; the exact deployed R2 journey proves Program restoration.                                                                                           |
| M03-02 | Manual Microbiology remains an available fallback only when no typed culture workflow applies.                                           | Yes       | Yes         | Yes                                                | R2                  | No             | Existing component coverage.                                                                                                                                                                                                                      |
| M03-03 | The details tile expands inline in Step 1 with an information banner; there is no new wizard step.                                       | Yes       | Yes         | Yes                                                | R2                  | No             | Existing component and Playwright coverage.                                                                                                                                                                                                       |
| M03-04 | Culture Protocol is read-only and shows the selected test's default Method.                                                              | Yes       | Yes         | Yes                                                | `8da391a5041a`       | No             | The saved Method ID and complete selected-test metadata now round-trip through the supported pending-order path; the deployed reload assertion passes.                                                                                             |
| M03-05 | The protocol display includes available media/incubation context and explains that the bench owns changes.                               | Yes       | Partial     | Partial                                            | `8da391a5041a`       | No             | The Method name and bench-ownership helper render. Missing optional setup prose remains the accepted, non-blocking degradation recorded in `openelis-work-r2-delta-2026-08-13.md`; the UI must not invent configuration.                              |
| M03-06 | Missing default Method renders `Not set - the bench will select a protocol` and never blocks Step 1.                                     | Yes       | R2          | Yes                                                | R2                  | No             | R2 service/component coverage.                                                                                                                                                                                                                    |
| M03-07 | Exactly five editable fields appear: Patient Origin, Date of Admission, Number of Sets, Clinical History, and Antibiotic Exposure.       | Yes       | R2          | Yes                                                | R2                  | No             | R2 component and Playwright coverage.                                                                                                                                                                                                             |
| M03-08 | Patient Origin uses the active deployment vocabulary and may default from a configured requesting organization.                          | Yes       | Yes         | Yes                                                | R2                  | No             | Service, component, and Playwright coverage.                                                                                                                                                                                                      |
| M03-09 | Date of Admission is always visible, optional, and disabled for Outpatient only.                                                         | Yes       | R2          | Yes                                                | R2                  | No             | R2 component and Playwright coverage.                                                                                                                                                                                                             |
| M03-10 | Future admission dates and admission dates after collection show correctable inline errors.                                              | Yes       | R2          | Yes                                                | R2                  | No             | R2 component and Playwright coverage.                                                                                                                                                                                                             |
| M03-11 | Date of Admission round-trips through the configured locale without changing the calendar date.                                          | Yes       | R2          | Yes                                                | R2                  | No             | R2 focused date and browser coverage.                                                                                                                                                                                                             |
| M03-12 | Number of Sets accepts 1-10 and defaults appropriately for the selected culture work.                                                    | Yes       | Yes         | Yes                                                | R2                  | No             | Service/component/browser coverage.                                                                                                                                                                                                               |
| M03-13 | Clinical History accepts multiline text up to the ruled bound; `clinical` macros appear when the separate Macro capability is available. | Yes       | Partial     | Partial                                            | Partial             | No             | Text/bound behavior exists. Macro consumption remains a separate dependency and must not be represented as complete here.                                                                                                                         |
| M03-14 | Antibiotic Exposure is optional, defaults false, and persists as a boolean.                                                              | Yes       | Yes         | Yes                                                | R2                  | No             | Service/component/browser coverage.                                                                                                                                                                                                               |
| M03-15 | No Critical Notification control appears in Step 1.                                                                                      | Yes       | R2          | Yes                                                | R2                  | No             | R2 component and Playwright coverage.                                                                                                                                                                                                             |
| M03-16 | Priority remains in the existing later order step, not inside the microbiology tile.                                                     | Yes       | Yes         | Indirect                                           | R2                  | No             | Add a direct negative assertion when the focused Playwright file is updated.                                                                                                                                                                      |
| M03-17 | Removing the final culture trigger with entered details asks before discarding; cancellation preserves the details in-session.           | Yes       | Yes         | Yes                                                | R2                  | No             | Component and Playwright coverage.                                                                                                                                                                                                                |
| M03-18 | Saving and reopening the order preserves Program, protocol, and all entered microbiology details.                                        | Yes       | Yes         | Yes                                                | `8da391a5041a`       | No             | Shared pending-order projection tests pass 2/2 in Java and 2/2 in Vitest. Deployment `20260814T041514Z-8da391a5041a` passed the exact authenticated reload-and-route journey without arbitrary waits.                                                |
| M03-19 | Completing collection creates one RECEIVED case per `(SampleItem, workflow type)` and repeated routing is idempotent.                    | Yes       | Yes         | Yes                                                | `8da391a5041a`       | No             | Service-created fixture, routing integration, and deployed Playwright coverage pass; no SQL, fixed primary keys, or DAO bypass is used.                                                                                                             |
| M03-20 | An AMR/WHONET flag alone does not trigger a case.                                                                                        | Yes       | Yes         | Yes                                                | R2                  | No             | Routing service coverage.                                                                                                                                                                                                                         |
| M03-21 | Mixed micro and non-micro tests share the specimen while the case contains only the micro work.                                          | Yes       | Yes         | Partial                                            | R2                  | No             | Service coverage; retain as a regression story rather than an R2 delta blocker.                                                                                                                                                                   |
| M03-22 | Bacteriology and TB tests on one SampleItem produce distinct sibling cases without second accessioning.                                  | Yes       | Yes         | Yes                                                | R2                  | No             | Service and Playwright coverage.                                                                                                                                                                                                                  |
| M03-23 | Existing cases retain a Method that is later deactivated and visibly identify it as inactive.                                            | Yes       | Partial     | Partial                                            | R2                  | No             | Historical reference retention exists; explicit inactive labeling remains a non-blocking hardening item and is not part of the core M-03 UAT story.                                                                                                |
| M03-24 | All controls are keyboard reachable and the inline section remains usable at desktop and mobile widths.                                  | Yes       | Yes         | Partial                                            | R2                  | No             | Existing axe/responsive evidence remains applicable; the final repair changes response metadata rather than controls or layout.                                                                                                                   |

## Genuine Source Ambiguity

The detailed mixed-workflow rule permits Bacteriology and TB sibling cases on
one SampleItem, while `AC-M03-17` still says that two differing culture
protocols on one Sample are rejected with a second-sample notice. The narrow,
non-conflicting interpretation is:

- different workflow types: sibling cases on the same SampleItem;
- two protocols inside one workflow case: one protocol per case, so the second
  same-workflow protocol cannot silently overwrite the first.

The reload defect is resolved. Confirm the interpretation above before
implementing any future same-workflow, multi-protocol UI.

## Exact Deployed Validation

- R1 contract commit: `b563bf2935507cbc5d5b6593ad5db6f8f16f19b0`
- R2 deployment commit: `8da391a5041a4bbf47693251173350fb65dc55d7`
- AMR deployment: `20260814T041514Z-8da391a5041a`
- Target verification: health and smoke passed for the exact R2 commit.
- Focused Java contracts: 2 passed (pending-request and collected-order test
  projections).
- Focused frontend converter contracts: 2 passed.
- Deployed Playwright: setup plus the M-03 reload, routing, and protocol-change
  journey passed 2/2 in 28.6 seconds without arbitrary waits.
- Live UAT: checklist `a91f518f034a...`; M-03 story `AMR-S01` v4.0,
  revision `384c45086925`, has five required steps. Human marks remain pending.

The full pre-existing `SampleTypeRequestRestControllerTest` class also exposes
three unrelated create-request fixture failures because its isolated audit-user
setup reaches an uninitialized `SpringContext.factory`. The two changed
projection contracts pass; the inherited fixture defect is recorded rather
than misreported as an M-03 behavior failure.

## Current Slice Gate

The core M-03 technical gate is passed for the exact deployed R2 commit. M03-05
is an accepted configuration-dependent degradation, and M03-23 remains
non-blocking hardening. Human review is still a separate `No` status and may
run in parallel with the next dependent slice; it is not silently counted as
automated acceptance.
