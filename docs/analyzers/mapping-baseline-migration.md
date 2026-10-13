# Moving analyzers to their own mappings

An analyzer's mapping used to be shared: every analyzer on the same profile
revision pointed at one set of test and answer choices, so editing it for one
analyzer changed it for the others. Each analyzer now owns its mapping
(`analyzer_mapping`, with its rows and confirmation), and the shared tables are
gone.

Changeset `124-analyzer-mapping-baseline-migration` does the move in one
transaction. Before it drops anything it writes the shared mappings to the
configuration-import history, then leaves every analyzer that was pinned to one
with its identity, name, lab units, Bridge connection and activation history,
and marks it inactive with no mapping. Nothing is activated or mapped for you.

## Reading the export

```sql
SELECT jsonb_pretty(summary::jsonb)
  FROM clinlims.configuration_import_run
 WHERE source = 'ANALYZER_MAPPING';
```

No row means no analyzer was pinned to a shared mapping. The document lists the
affected analyzers (`analyzers`, with the `site_binding_revision_id` each was
pinned to) and every row of the removed tables, keyed by table name:
`analyzer_profile_binding`, `analyzer_site_binding`,
`analyzer_site_binding_revision`, `analyzer_site_binding_test`,
`analyzer_site_binding_result` and `analyzer_site_binding_confirmation`. Two
analyzers that shared a mapping name the same revision.

## Verifying an analyzer again

In Admin > Analyzers, choose Edit setup on the analyzer's row, pick its analyzer
type and save. It keeps its Bridge connection and stays inactive. Then review
the mapping it starts with (the defaults for that type), correct what needs it,
confirm it and apply it, then activate the analyzer. Use the export to see which
test and answer choices were used before. The tables are not restored by rolling
the changeset back; a pre-upgrade backup is the only way to get them back.
