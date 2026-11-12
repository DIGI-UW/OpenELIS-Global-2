# Data Model: OGC-788 Macro Library

This is an engineering contract, not a product-spec requirement.

## Text Macro

| Field           | Type                     | Rules                                                                           |
| --------------- | ------------------------ | ------------------------------------------------------------------------------- |
| `id`            | generated numeric string | Server generated from the OpenELIS sequence; never client supplied.             |
| `code`          | string, 2 to 15          | Stored in canonical lower-case dot-prefixed form; unique.                       |
| `expansionText` | string, max 500          | Trimmed, non-empty plain text.                                                  |
| `category`      | controlled value         | One of the eight M-08 categories; required.                                     |
| `active`        | boolean                  | Controls new runtime discovery/expansion only.                                  |
| `provenance`    | controlled value         | `LOCAL` for staff-authored phrases; `PACKAGED` reserved for a reviewed package. |
| `sourceKey`     | nullable string          | Package/source identifier; null for local phrases.                              |
| `sourceVersion` | nullable string          | Version of the reviewed source; null for local phrases.                         |
| `lastUpdatedBy` | system-user identifier   | Derived from the authenticated request.                                         |
| `lastupdated`   | timestamp                | Managed through existing persistence conventions.                               |

## Text Macro Context

A macro has one or more controlled context values through an association keyed
by macro ID and context. Each context is one Microbiology V2 case field:

- `MICROBIOLOGY_CLINICAL_HISTORY`: Clinical history in Case information
- `MICROBIOLOGY_CASE_NOTE`: case notes and notes on test results, isolates,
  drugs, and culture rows
- `MICROBIOLOGY_CULTURE_READING`: the note on a culture reading or extension
- `MICROBIOLOGY_OVERRIDE_REASON`: the AST action reason, including overrides
- `MICROBIOLOGY_AMENDMENT_REASON`: the amendment and cancellation reason

Antibiotic exposure stays the M-03 yes/no choice and has no context.

## Text Macro Category

A required single value that says what kind of phrase it is: `CLINICAL`,
`GRAM_STAIN`, `COLONY`, `CULTURE`, `ORGANISMS`, `AST`, `REPORTING`, or
`TIMELINE`. The category groups and labels phrases in the library and in the
suggestion list; the contexts decide where a phrase is offered.

The association avoids a comma-separated field and permits a future consumer
without changing the macro table. Context labels are translated in the client;
stored values are stable application identifiers.

## Invariants

1. The service trims input, ensures a leading dot, converts the code to
   lower-case, and rejects whitespace or characters outside `[a-z0-9_-]` after
   the dot. The reserved codes `.help` and `.list` are rejected.
2. The canonical code is unique at both service and database levels.
3. A macro always has at least one recognized context.
4. Runtime reads return active macros only and require an exact recognized
   context.
5. M1 phrases are local. No row claims packaged provenance without a reviewed
   source key and version.
6. Deactivation does not alter text already saved in any case field.
7. Administrative updates replace the context set atomically in the service
   transaction.
8. Controllers do not traverse entity relationships; services compile response
   DTOs while the transaction is open.

## Migration Rules

- Liquibase XML in `liquibase/textmacro/`, included at the end of
  `base-changelog.xml`, with a stable `logicalFilePath`.
- Create the definition table, context association table, unique code
  constraint, lookup indexes, and foreign key with cascading association
  cleanup.
- Provide rollback for indexes, association table, and definition table.
- Do not insert UAT phrases or clinical defaults in the migration.
