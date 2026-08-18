# Panel LOINC → terminology mappings

Changeset: `src/main/resources/liquibase/3.5.x.x/084-panel-loinc-terminology-backfill.xml`
(id `OGC-panel-loinc-terminology-backfill`)

## Why it exists

A panel's LOINC lives in two places.

- `panel.loinc` — the original column. FHIR intake still routes electronic
  orders by it (`getPanelByLoincCode`), and the panel lists display it.
- `panel_terminology_mapping` — the newer store, which the **Panel Editor**
  reads and writes, and which can hold several systems (LOINC, SNOMED, CIEL,
  OCL) with a relationship for each.

The Panel Editor is new, so on any real deployment the only LOINC a panel has is
the legacy one and the mapping store is empty for it. The editor showed such a
panel no terminology at all, and saving there wrote its empty set back over the
column — quietly clearing a code the lab was routing orders by.

This migration copies each legacy code into the store as **LOINC / SAME_AS**,
once. It is a one-way catch-up: there is no migration in the other direction,
because keeping the two in step afterwards belongs in the service layer (below).

## How to run it

Nothing to do by hand. It is registered in
`src/main/resources/liquibase/3.5.x.x/base.xml` and runs on the next application
start, like every other changeset.

To run it alone against a database — for a rehearsal, or to confirm what it would
touch:

```bash
mvn liquibase:update \
  -Dliquibase.changeLogFile=src/main/resources/liquibase/base-changelog.xml \
  -Dliquibase.url=jdbc:postgresql://localhost:5432/clinlims \
  -Dliquibase.username=clinlims
```

To see the panels it will affect before running anything:

```sql
SELECT p.id, p.name, trim(p.loinc) AS legacy_loinc
  FROM clinlims.panel p
 WHERE p.loinc IS NOT NULL
   AND length(trim(p.loinc)) > 0
   AND NOT EXISTS (SELECT 1 FROM clinlims.panel_terminology_mapping m
                    WHERE m.panel_id = p.id
                      AND m.source = 'LOINC'
                      AND m.code = trim(p.loinc));
```

## Why it is safe

- **Re-runnable.** The `NOT EXISTS` guard keys on the same
  `(panel_id, source, code)` the table's unique constraint uses, so a second run
  inserts nothing. Liquibase records it anyway; the guard matters if it is applied
  by hand.
- **Additive.** It only inserts. No panel row is modified, and no existing mapping
  is updated or deleted — a panel that somehow already carries a mapping is simply
  skipped.
- **Skipped where it cannot apply.** A `tableExists` precondition marks it run on
  a database without `panel_terminology_mapping`.
- **Reversible.** The rollback removes only LOINC / SAME_AS mappings whose code
  still matches their panel's legacy column — that is, the rows this changeset
  could have created. A mapping added by hand in the editor survives a rollback.

## Staying in step afterwards

The migration is a one-off catch-up. Both directions are kept in step from then
on:

| Edited in                        | What happens                                                                                                                                                 |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Panel Editor → Terminology       | `saveMappingsForPanel` writes the first active LOINC / SAME_AS code out to `panel.loinc`, and clears the column when no such mapping remains.                |
| Legacy panel pages → LOINC field | `syncLegacyLoinc` upserts the matching LOINC mapping as SAME_AS, retires any active LOINC mapping carrying a different code, and leaves other systems alone. |

Two details worth knowing:

- `panel.loinc` is `varchar(10)`. A longer code can live in the mapping store but
  cannot be denormalized onto the column, so it stays in the store only.
- `syncLegacyLoinc` does not overwrite a relationship someone chose in the
  editor. The legacy column says _which_ code the panel has, not what it means.

## Tests

`src/test/java/org/openelisglobal/panelterminology/PanelLoincSyncAndBackfillTest.java`
runs the changeset's statement verbatim and covers the backfill, including
re-running it, alongside both sync directions.
