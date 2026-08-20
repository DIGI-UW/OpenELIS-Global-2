# Data Model: OGC-788 Macro Library

This is an engineering contract, not a product-spec requirement.

## Text Macro

| Field           | Type                     | Rules                                                               |
| --------------- | ------------------------ | ------------------------------------------------------------------- |
| `id`            | generated numeric string | Server generated from the OpenELIS sequence; never client supplied. |
| `code`          | string, max 64           | Stored in canonical lower-case dot-prefixed form; unique.           |
| `expansionText` | string, max 4000         | Trimmed, non-empty plain text.                                      |
| `active`        | boolean                  | Controls new runtime discovery/expansion only.                      |
| `provenance`    | controlled value         | `LOCAL` in M1; `PACKAGED` reserved for reviewed M2 content.         |
| `sourceKey`     | nullable string          | Package/source identifier; null for local phrases.                  |
| `sourceVersion` | nullable string          | Version of the reviewed source; null for local phrases.             |
| `lastUpdatedBy` | system-user identifier   | Derived from the authenticated request.                             |
| `lastupdated`   | timestamp                | Managed through existing persistence conventions.                   |

## Text Macro Context

A macro has one or more controlled context values through an association keyed
by macro ID and context. M1 supports:

- `MICROBIOLOGY_CULTURE_ACTIVITY`
- `MICROBIOLOGY_CLINICAL_HISTORY`
- `MICROBIOLOGY_ANTIBIOTIC_EXPOSURE`

The association avoids a comma-separated field and permits a future consumer
without changing the macro table. Context labels are translated in the client;
stored values are stable application identifiers.

## Invariants

1. The service trims input, ensures a leading dot, converts the code to
   lower-case, and rejects whitespace or characters outside `[a-z0-9_-]` after
   the dot.
2. The canonical code is unique at both service and database levels.
3. A macro always has at least one recognized context.
4. Runtime reads return active macros only and require an exact recognized
   context.
5. M1 phrases are local. No row claims packaged provenance without a reviewed
   source key and version.
6. Deactivation does not alter existing activity notes or order narratives.
7. Administrative updates replace the context set atomically in the service
   transaction.
8. Controllers do not traverse entity relationships; services compile response
   DTOs while the transaction is open.

## Migration Rules

- Liquibase XML in the repository's current versioned folder.
- Create the definition table, context association table, unique code
  constraint, lookup indexes, and foreign key with cascading association
  cleanup.
- Provide rollback for indexes, association table, and definition table.
- Do not insert UAT phrases or clinical defaults in the migration.
