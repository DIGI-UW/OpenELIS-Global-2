# Code QA: Spec-Code Alignment

> Historical checkpoint at `dbc6aba66`. The current lot-traceability review at
> `554063fc8` is recorded in `code-qa-lot-traceability-554063fc8.md` and
> supersedes the lot-specific findings below.

- Feature commit: `dbc6aba66`
- Baseline: `6aafb05a9345525e04a0749e01ba09a3e41b5c2d`
- `DIGI-UW/code-qa`: `30528d176bd128b4765242d130f38ca9fb85d7b8`
- Verdict: **aligned for completed slices; full M8 blocked on one product ruling**

## Requirement Map

| Requirement | Shipped behavior and primary guard                                                                                                 | Status                                      |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| FR-001/002  | Amendment lifecycle, immutable report versions, one-open guard, relock; `MicroAmendmentIntegrationTest` and amendment Playwright   | Aligned                                     |
| FR-003/010  | Append-only before/after identification events and history; service, integration, and isolate component tests                      | Aligned                                     |
| FR-004/005  | Distinct repeat/retest runs and explicit reportable-run selection; service, projection, component, and repeat-AST Playwright tests | Aligned                                     |
| FR-006/007  | Reagent-lot eligibility, shared Inventory usage, and retained history                                                              | **Blocked**                                 |
| FR-008      | Keyboard workflow plus 14 desktop/mobile axe scans with zero detected WCAG 2.1 AA violations                                       | Aligned except the blocked lot-picker state |
| FR-009      | Service-created 200-case/dense-case datasets, API/browser percentiles, and query plans                                             | Aligned                                     |

## Findings

| ID   | Severity | Classification             | Finding                                                                                                                                                        | Disposition                                                                                                         |
| ---- | -------- | -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| A-01 | Blocker  | Both sources ambiguous     | OGC-784 requires `REQUIRED / OPTIONAL / SUBSTITUTE`; the existing Test Catalog exposes `PRIMARY / SECONDARY`. Neither mapping preserves product intent safely. | Product ruling required before T024-T035b. No default or schema was invented.                                       |
| A-02 | Medium   | Documentation lag          | The M8 spec still said `Draft for implementation`, and the MVP V2 list did not distinguish follow-up work now implemented on this branch.                      | Corrected in the artifact-sync commit without changing #3789's historical acceptance boundary.                      |
| A-03 | High     | Code defect, resolved      | The default frontend service omitted repeat-run creation and reportable selection even though named exports existed.                                           | Fixed in `50daa1855`; service-contract and Playwright guards pass.                                                  |
| A-04 | High     | Code defect, resolved      | Read-only report preview returned HTTP 500 when multiple reviewed attempts lacked a selection.                                                                 | Fixed in `4cfa4b7ae`; preview is non-reportable and release still returns `REPORTABLE_AST_RUN_REQUIRED`.            |
| A-05 | Medium   | Evidence gap, resolved     | Initial performance evidence showed per-case worklist relationship loading.                                                                                    | Batched in `053f11ff0`; API, browser, and exact query-plan evidence pass. No unsupported index migration was added. |
| A-06 | Medium   | Test-quality gap, resolved | New component tests used low-level events and older microbiology Playwright used private Carbon selectors.                                                     | Replaced with `userEvent` and role/label/title-based interactions in `eb9f0e9a1` and `dbc6aba66`.                   |

## Grep-Clean Checks

- Microbiology Playwright: no `waitForTimeout`, forced click, or private
  `.cds--*` selector.
- Qualification fixtures: no direct SQL, fixed primary keys, DAO bypass, or
  production qualification endpoint.
- Non-English locale files are unchanged.

Full M8 acceptance must remain open until A-01 is ruled and the lot-specific
requirements, accessibility state, and Playwright journey are implemented.
