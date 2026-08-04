# M8 lot traceability local validation

- Commits under test: `307a9b3de`, `554063fc8`
- Stack: isolated `cc8c-ogc782` Docker Compose project
- Base URL: `https://localhost:48444`
- Scope: policy-neutral culture-media and AST-card lot traceability
- Result: **implemented scope passes; requiredness policy remains open**

## Proven behavior

- Test Catalog `PRIMARY / SECONDARY` values are displayed as role metadata and
  never converted into mandatory/optional/substitute policy.
- The first eligible lot is recommended by FEFO. Expired, failed-QC, inactive,
  and insufficient lots have named blockers.
- Saving locks and revalidates the exact selected lot, consumes it through the
  shared Inventory services, and rejects partial consumption.
- Culture setup and AST setup retain links to the shared Inventory usage record;
  case history displays the lot, quantity, context, status, and time.
- The property-gated UAT scenario creates Test Catalog links, Inventory items,
  and named lots through services only, with no SQL, fixed IDs, or DAO bypass.

## Automated results

| Level | Result | Guard |
| --- | --- | --- |
| Service/controller | Passed | FEFO, eligibility reasons, exact-lot revalidation, role preservation, actor mapping, and response contracts |
| ORM and real-database integration | Passed | Mapping bootstrap, exact decrement and culture/AST provenance, stale-lot rejection, and atomic rollback after a forced provenance failure |
| Liquibase | 1/1 passed | Full update, rollback of four M8 changesets, absence check, and reapply |
| Carbon component | Passed | Eligibility and role rendering, ordinary radio interaction, typed payload, and retained cross-stage usage history |
| Foundational Playwright | 2/2 passed | Authenticated culture and AST lot journey with both usages visible in the final AST review state |
| Accessibility | 4/4 passed | Desktop/mobile lot-picker scans with zero detected violations |
| Keyboard | 2/2 passed | Carbon radio selection through focus and `Space`, with no forced clicks or arbitrary waits |

The Liquibase guard initially failed because the new `BaseObject` mapping
expected `micro_inventory_usage_link.last_updated` while changeset `062` did
not provide it. Corrective changeset `063` was then added, and the same full
update/rollback/reapply test passed. This is the load-bearing inversion proof
for schema/runtime compatibility.

## Visual review

The desktop and mobile Playwright screenshots were inspected at full page. The
case header, setup and AST sections, FEFO and blocked states, catalog role tags,
and exact usage history were visible without clipping or overlap.

The generated PNGs remain outside git under the Playwright report. Binary
evidence is not committed.

## Open issues

- Product must define required/optional/substitute policy independently of the
  existing `PRIMARY / SECONDARY` catalog role. Full US4 requiredness acceptance
  remains open.
- AST cards use Inventory `CARTRIDGE`; current Test Catalog authoring may expose
  only `REAGENT`. This is an administration-surface compatibility decision, not
  a reason to duplicate Inventory data.
- PR #3840 changes `InventoryItem` identifier typing and may require a rebase
  adjustment if it lands before M2.

## Post-checkpoint guards

- `87177aee2` adds real-PostgreSQL stale-selection and forced downstream-failure
  tests. The latter flushes an invalid bench provenance link after consumption
  is staged, then proves quantity, Inventory transaction, Inventory usage, and
  Microbiology linkage all rolled back.
- `8be8cd201` fixes a screenshot-discovered UI defect where the AST history
  filtered out the earlier culture usage. The component guard was observed red
  before the fix; the tightened Playwright journey now requires both rows.
