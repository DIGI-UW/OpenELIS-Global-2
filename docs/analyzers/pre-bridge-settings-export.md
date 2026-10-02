# Pre-Bridge analyzer settings

Before the Analyzer Bridge, OpenELIS stored each analyzer's own connection and
import settings: an analyzer type, host and port, communication mode, an import
directory with file pattern and format, column mappings, plugin configuration,
per-analyzer test mappings and serial port settings. The Bridge owns all of that
now, through the connection and profile an analyzer is set up with.

Changeset `115-remove-pre-bridge-analyzer-storage` removes that storage. Before
dropping anything it exports, in the same transaction, every analyzer that still
holds such settings and has no Bridge connection, so nothing configured is lost.
Analyzers themselves, their results and their history are untouched.

## Reading the export

The export is one row in the configuration-import history, with one JSON
document per analyzer:

```sql
SELECT jsonb_pretty(summary::jsonb)
  FROM clinlims.configuration_import_run
 WHERE source = 'ANALYZER_LEGACY';
```

No row means the database had nothing to export. Each document carries the
analyzer's id, name and status, the old type name and protocol, host, port,
communication mode, import directory, file pattern, format and column mappings,
the plugin configuration, the test mappings (source code, test id, component id)
and any serial port settings.

## Setting the instrument up again

Set the analyzer up through the Bridge-backed pages (Admin > Analyzers),
choosing the Bridge profile that matches the old type, and enter the host, port
or import directory from the document. Confirm the test mappings in the profile
mapping step; the exported mappings list the test ids they used. The old storage
is not restored by rolling the changeset back; a pre-upgrade backup is the only
way to get the tables themselves back.

## Databases that already lost the storage

Develop builds between 8 and 24 September 2026 carried cleanup changesets, since
removed, that dropped this storage without an export. On such a database the
changeset finds nothing to do and records nothing.
