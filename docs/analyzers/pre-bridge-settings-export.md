# Pre-Bridge analyzer settings

Before the Analyzer Bridge, OpenELIS stored each analyzer's own connection and
import settings: an analyzer type, host and port, communication mode, an import
directory with file pattern and format, column mappings, plugin configuration,
per-analyzer test mappings and serial port settings. The Bridge owns all of that
now, through the connection and profile an analyzer is set up with.

Changeset `115-remove-pre-bridge-analyzer-storage` removes that storage. Before
dropping anything it exports, in the same transaction, everything it is about to
drop: every analyzer's legacy values and every row of every removed table. The
analyzers themselves, their results and their activation history are untouched.

## Reading the export

The export is one row in the configuration-import history:

```sql
SELECT jsonb_pretty(summary::jsonb)
  FROM clinlims.configuration_import_run
 WHERE source = 'ANALYZER_LEGACY';
```

No row means the database had nothing to export. The document has two kinds of
entries, all keyed by the original column names:

- `analyzers`: one entry per analyzer whose legacy columns held a value other
  than the column default, with its `id`, `name`, `bridge_connection_id` and
  those values (type id, host, port, communication mode, import directory, file
  pattern and format, column mappings, and the older descriptive columns).
- One entry per removed table that had rows, named after the table (for example
  `analyzer_type`, `analyzer_test_map`, `analyzer_plugin_config`,
  `serial_port_configuration`, `file_import_configuration`,
  `analyzer_file_upload`), holding every row of that table.

## Setting the instrument up again

Set the analyzer up through the Bridge-backed pages (Admin > Analyzers),
choosing the Bridge profile that matches the old type, and enter the host, port
or import directory from the document. Confirm the test mappings in the profile
mapping step; the exported mappings list the test ids they used. The old storage
is not restored by rolling the changeset back; a pre-upgrade backup is the only
way to get the tables themselves back.

## Databases that already lost the storage

Develop builds between 8 and 24 September 2026 carried cleanup changesets, since
removed, that dropped this storage without an export. The changeset works with
whatever is left: it exports and drops the objects that still exist, and where
none is left it records nothing.
