# V1 distribution absorption

**Status:** accepted 2026-09-02 · **Ticket:** OGC-608 · **Changeset:**
`liquibase/qa/022`

## Decision

EQA V2 replaces the V1 order list and distribution pages with My Cycles and
Provider Cycles. To keep finished V1 work visible on the new pages:

1. Every `eqa_distribution` row in status `COMPLETED` with no cycle link gains
   one synthetic `eqa_cycle` in status `CLOSED`. The cycle copies the
   distribution's name and dates, takes the next cycle number on its scheme, and
   carries one `eqa_cycle_state_transition` row with trigger event
   `V1_BACKFILL`. The distribution's `cycle_id` points at the new cycle.
2. Distributions in `DRAFT`, `PREPARED`, or `SHIPPED` are **not** migrated and
   stay unlinked.
3. Legacy `eqa_result` rows are **not** copied into `eqa_participant_result`.
   They stay readable through the linked distribution.
4. The V1 URLs redirect for one release: `/qa/eqa/orders` and `/EQAOrders` to My
   Cycles, `/qa/eqa/distribution*` and `/EQADistribution*` to the provider
   scheme list. The V1 pages and their menu rows are removed.

This amends FR-V2.1-03, which said legacy distribution rows remain unlinked.
That rule now applies only to active distributions.

## Why active distributions are not migrated

A live V2 cycle is an aggregate: a participant roster, a panel with samples,
rounds with deadlines, and shipment boxes. V1 recorded none of these. The V1
wizard collected participants but never persisted them, and a V1 distribution
has a single target value rather than panel samples. There is nothing to
convert, and a cycle created without that aggregate cannot pass the provider
workbench's ready-to-ship gate or reach the deadline scheduler. Mapping an
active distribution to any non-terminal cycle state would produce a cycle that
looks live but cannot be operated.

Completed distributions have no such problem: `CLOSED` is terminal, and the
cycle exists only so the history is visible.

## Operator note before upgrading

Active V1 distributions become unreachable once the V1 pages are removed. Before
deploying a build that carries `qa/022`, either complete them in V1 so the
migration closes them as history, or discard them. Check with:

```sql
SELECT id, distribution_name, status
FROM clinlims.eqa_distribution
WHERE cycle_id IS NULL AND status <> 'COMPLETED';
```

## Rollback

`qa-073`'s rollback removes the synthetic cycles, their audit rows, and the
`cycle_id` links. It refuses to run if any synthetic cycle has activity recorded
after the migration: a transition other than the backfill row, or a round,
panel, roster entry, receipt, result, follow-up, or shipping box attached to it.
Resolve those cycles by hand before rolling back.

## Why the legacy scores stay where they are

V1 scores predate cycles, panels, and the V2 scoring fields. Copying them into
`eqa_participant_result` would produce half-empty rows whose mapping is
guesswork. The V1 table stays queryable and V2 history starts clean.
