# Code QA: M8 lot traceability

- Feature commit: `554063fc8`
- Baseline deployment commit: `cd833663c`
- `DIGI-UW/code-qa`: `30528d176bd128b4765242d130f38ca9fb85d7b8`
- Verdict: **aligned and lean for policy-neutral traceability; two integration
  guards and one product ruling remain open**

## Alignment

| Finding | Classification | Disposition |
| --- | --- | --- |
| Existing docs said all lot work was blocked | Documentation lag | Spec, research, task ledger, MVP follow-up status, UAT, and evidence now distinguish delivered traceability from unresolved requiredness. |
| OGC-784 uses required/optional/substitute while the repo stores primary/secondary | Genuine product ambiguity | No mapping is inferred. Roles remain visible metadata and selection remains optional. |
| M-12 used FIFO wording despite expiry-aware behavior | Documentation defect | Current acceptance language uses FEFO. |
| AST cards use Inventory `CARTRIDGE`, while Test Catalog authoring may expose only `REAGENT` | Engineering/product-admin decision | Flagged for review; no parallel catalog or Inventory model was added. |

## Meaningful coverage

The load-bearing schema guard is
`MicrobiologyM8LiquibaseRollbackTest`. It failed before corrective changeset
`063` because the ORM required `last_updated`; after the fix, full update,
rollback, absence checks, and reapply pass against PostgreSQL.

Layer-correct guards also cover:

- Mockito service tests for exact locked-lot revalidation, named eligibility
  failures, no partial consumption, FEFO, and role preservation.
- A real-database UAT-scenario integration test for exact quantity decrement and
  retained culture/AST provenance.
- Vitest and Testing Library for rendered Carbon state and ordinary user input.
- Playwright for the assembled authenticated culture and AST workflow.

The test set is not inflated to claim missing behavior. T025's stale-lot race is
not yet represented at the real-database level, and T026's rollback-on-forced-
failure path remains absent.

## Simplicity

Verdict: **lean**.

- Reuses Test Catalog links, `InventoryLot`, `InventoryUsage`, and Inventory
  consumption instead of creating microbiology-specific stock records.
- Adds one narrow provenance link because shared usage cannot otherwise name the
  culture setup or AST run.
- Reuses one Carbon picker and one history component in both bench contexts.
- Keeps corrective changeset `063` separate so databases that already applied
  `062` receive the missing version column safely.
- Adds no requiredness schema, policy engine, cache, package upgrade, or runtime
  fixture endpoint.

## Evidence hygiene

Committed evidence is text-only. Screenshots and video remain external and
reproducible. The live Grist checklist includes `AMR-S09` and steps `AMR-29`
through `AMR-31`; they remain pre-UAT until the exact lot-enabled application
SHA is deployed and the human reviewer records rulings.
