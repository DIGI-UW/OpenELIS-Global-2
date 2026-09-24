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
migration closes them as history, or discard them. The `qa-075` changeset has no
change of its own; its precondition writes a warning to the startup log when
such rows exist, so a missed cleanup is visible at upgrade time. Check with:

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

### Rolling the whole V2 set back

The V2 set is a contiguous run of changesets. Two things about removing it are
not obvious from the changelog.

**Two housekeeping changesets sit above everything, always.**
`2.8.x.x/panel_item_fix.xml::1` and `2.3.x.x/minor_fixes.xml::2` are
`runAlways`, so they re-execute on every application start and are therefore the
two most recently applied changesets on any running installation. Neither had an
inverse: one deletes orphaned panel items, the other stamps null timestamps. So
every rollback attempted on this product, of any feature, stopped on
`No inverse to DeleteDataChange created` before it reached whatever was being
removed, and had to be unblocked by hand from a stack trace.

Both now declare an empty `<rollback />`, which is the truthful inverse as well
as the convenient one: restoring orphaned panel items would restore broken
references, and the next start would delete them again.

**Which route you take depends on how the database got the feature.** There are
two, and only one of them works on any given database.

_A database provisioned after `qa/040-eqa-v2-start-tag.xml` shipped_ carries the
tag `eqa-v2-start`, written ahead of the first V2 changeset rather than in file
order. Say where to stop: `liquibase rollbackToTag eqa-v2-start`.

_A database that already carried the V2 set when that changeset arrived_ has no
such tag, and `rollbackToTag eqa-v2-start` fails naming the missing tag. That
failure is the intended outcome, not a fault to work around. A tag marks the
last changeset applied when it runs, so on such a database the changeset would
tag the wrong end, above the whole V2 set instead of below it, and
`rollbackToTag` would report success and remove nothing. Its precondition skips
it there, and `qa/041` clears the tag from databases that recorded one before
the precondition was corrected. Use the counted route.

**Counting, where you have to.** The V2 set is 57 changesets, but the count you
pass is not 57: everything between the set and the head of the changelog goes
with it. That is always the two `runAlways` rows, plus `qa/041`, plus whatever
else has been applied since. Two participant databases measured on the same day
needed 60 and 61, differing only in whether a `qa-079` row was already on file.

So **read the number off `databasechangelog` rather than trusting one**:

```sql
SELECT COUNT(*) FROM clinlims.databasechangelog
 WHERE orderexecuted >= (SELECT orderexecuted FROM clinlims.databasechangelog
                          WHERE id = 'eqa-cycle-001-create-eqa-cycle');
```

`eqa-cycle-001-create-eqa-cycle` creates `eqa_cycle` and is the first changeset
of the set. The count is a property of one database's history, not of V2.

**Widened CHECK constraints delete their own rows.** Four V2 changesets widened
a CHECK precisely so the feature could write a new value, and their rollbacks
restore the narrow definition. That cannot succeed while rows carrying the new
value are on file, which on any installation that used EQA is all of them. Each
of those rollbacks now deletes the rows it would otherwise fail on, in the same
block:

| Changeset | Constraint                                     | Rows removed on rollback                |
| --------- | ---------------------------------------------- | --------------------------------------- |
| `qa/024`  | `eqa_cycle_state_transition_trigger_event_chk` | `trigger_event = 'PANEL_RECEIPT'`       |
| `qa/028`  | `eqa_cycle_state_transition_trigger_event_chk` | `trigger_event = 'FIRST_SHIPMENT_SENT'` |
| `qa/022`  | `eqa_cycle_state_transition_trigger_event_chk` | `trigger_event = 'V1_BACKFILL'`         |
| `qa/030`  | `chk_alert_type` on `clinlims.alert`           | `alert_type = 'EQA_SUBMISSION_FAILED'`  |

The first three are audit rows on a table the rollback drops a few changesets
later, so deleting them costs nothing that was not already going. The fourth is
different: `clinlims.alert` survives the rollback, so those alerts are genuinely
deleted. They name a feature that is being removed, and the constraint they
violate belongs to `3.5.x.x/070-critical-result-alert-type.xml` rather than to
EQA, so leaving them would block a core constraint from being restored.

**The end state**, measured on a `pg_dump` copy of a participant database that
had used the feature: eleven V2 tables gone, the eight V1 EQA tables standing,
`shipping_box.eqa_cycle_id` removed, the one `EQA_SUBMISSION_FAILED` alert
deleted with the other six untouched, `chk_alert_type` back to the definition
`3.5.x.x/070-critical-result-alert-type.xml` owns, and 61 rows gone from
`databasechangelog`: the 57, the two `runAlways` rows, `qa/041`, and the
`qa-079` row that copy already carried. Liquibase reported "Rollback has been
successful" with no manual step at any point.

Measured on the same copy, the tag route does what it should on a database that
has no tag: `rollback eqa-v2-start` stops with _"Could not find tag
'eqa-v2-start' in the database"_, exits non-zero, changes nothing, and leaves
all nineteen EQA tables standing. An operator who runs it learns at once that it
is the wrong route for their database, which is the point of recording no tag
there.

Measured on a database provisioned from the baseline dump with this changelog,
the tag route is the whole story: the tag lands at the head of the set,
immediately below `eqa-cycle-001-create-eqa-cycle`, and `rollback eqa-v2-start`
takes the changelog from 954 rows to 895 with the eleven V2 tables gone, the
eight V1 tables standing, and nothing left above the tag point.

## Why the legacy scores stay where they are

V1 scores predate cycles, panels, and the V2 scoring fields. Copying them into
`eqa_participant_result` would produce half-empty rows whose mapping is
guesswork. The V1 table stays queryable and V2 history starts clean.
