# Existing analyzer upgrade

**Draft: not ready to merge or deploy.** This patch preserves the inputs needed
for migration. The transfer itself remains to be implemented.

## Current problem

Cleanup requires existing analyzers to have Bridge connection and local catalog
references, but no upgrade routine populates them. Startup therefore halts.
Earlier cleanup can already have removed settings needed for that transfer.

## This patch

Defer destructive cleanup in 086, 088, 093, 094, 097 and 098 using Liquibase's
`ignore` flag. Keep additive migrations enabled and preserve the published
changeset identities and checksums. Ignored cleanup is not marked as executed.
This retains old profile references, recognition rules, connection settings,
FILE/serial configuration, mappings and error records as migration input only;
Bridge remains the runtime owner.

Already-deleted data is not restored. Inspect applied changesets and backups
before upgrading an affected installation. Retaining data alone does not repair
analyzer listing, which currently expects the new profile bindings.

## Complete before merge

- [ ] Add a one-time OE2 transfer using the existing Bridge connection API and
      unchanged analyzer IDs. Reconcile matching connections, save returned IDs,
      and migrate profile references and local clinical mappings without guessing.
- [ ] Keep pending records visible and retain their source data when Bridge is
      unavailable or configuration cannot be resolved. Resume interrupted work
      without duplicate connections or false activation/verification.
- [ ] Test a populated upgrade, repeated execution, interruption after Bridge
      creation, and ASTM/HL7/FILE delivery to the existing analyzer records.
- [ ] Verify fresh, partially upgraded and already migrated databases, including
      preservation of results, mappings and outstanding raw/error messages.

Cleanup can follow in a separate PR after verified transfer. Do not simply
re-enable the ignored changesets: their existing guards do not prove the data
reached Bridge. Account for earlier destructive migrations and already-applied
changesets; reverting an image cannot restore deleted data.

## Validation

The focused changelog test uses Liquibase 4.8's actual ignore filter, verifies
all seven published cleanup checksums, and confirms additive migrations remain
scheduled. Its scheduling assertion fails on unmodified develop. These checks
do not establish full application startup or database upgrade acceptance.
