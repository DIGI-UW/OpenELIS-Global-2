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
| ORM and real-database integration | Passed | Mapping bootstrap plus exact decrement and culture/AST provenance through the service-created UAT scenario |
| Liquibase | 1/1 passed | Full update, rollback of four M8 changesets, absence check, and reapply |
| Carbon component | Passed | Eligibility and role rendering, ordinary radio interaction, typed payload, and retained usage history |
| Foundational Playwright | 2/2 passed | Authenticated desktop/mobile culture and AST lot journey with visible persisted provenance |
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
- T025 still needs a real-database stale-lot rejection test. Current rejection
  guards are service tests around the row-locked selection path.
- T026 still needs a forced downstream-failure integration test proving the
  lot decrement and usage link roll back together. The successful real-database
  path is covered.
- AST cards use Inventory `CARTRIDGE`; current Test Catalog authoring may expose
  only `REAGENT`. This is an administration-surface compatibility decision, not
  a reason to duplicate Inventory data.
- PR #3840 changes `InventoryItem` identifier typing and may require a rebase
  adjustment if it lands before M2.
