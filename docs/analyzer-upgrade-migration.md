# Existing analyzer upgrade

**Draft: not ready to merge or deploy.** This patch preserves the inputs needed
for migration. The transfer itself remains to be implemented.

## Current problem

Cleanup requires existing analyzers to have Bridge connection and local catalog
references, but no upgrade routine populates them. Startup therefore halts.
Earlier cleanup can already have removed settings needed for that transfer.

## This patch

Remove premature cleanup from 086 and remove the standalone cleanup changesets
088, 093, 094, 097 and 098 from the startup changelog and source tree. Keep
additive migrations enabled. Git history and prior release tags retain the
published definitions; no ignored migrations or runtime archive is needed.
Existing DATABASECHANGELOG records are left alone. A new, separate changeset
retains the old registration-status conversion and obsolete menu removal,
without deleting analyzer configuration or error data.

This retains old profile references, recognition rules, connection settings,
FILE/serial configuration, mappings and error records as migration input only;
Bridge remains the runtime owner.

Already-deleted data is not restored. Inspect applied changesets and backups
before upgrading an affected installation. Retaining data alone does not repair
analyzer listing, which currently expects the new profile bindings.

## Complete before merge

- [ ] Add a one-time OE2 transfer using the existing Bridge connection API and
      unchanged analyzer IDs. Reconcile matching connections, save returned IDs,
      and migrate profile references and local clinical mappings without
      guessing.
- [ ] Keep pending records visible and retain their source data when Bridge is
      unavailable or configuration cannot be resolved. Resume interrupted work
      without duplicate connections or false activation/verification.
- [ ] Test a populated upgrade, repeated execution, interruption after Bridge
      creation, and ASTM/HL7/FILE delivery to the existing analyzer records.
- [ ] Verify fresh, partially upgraded and already migrated databases, including
      preservation of results, mappings and outstanding raw/error messages.

Cleanup must use new changeset IDs in a later release after verified transfer.
Do not resurrect the removed changesets: their old guards do not prove the data
reached Bridge. Account for earlier destructive migrations and already-applied
changesets; reverting an image cannot restore deleted data.

## Validation

Existing populated PostgreSQL upgrade fixtures require successful completion
with source settings and mappings preserved, rather than a cleanup failure or
deleted tables. They also rerun the upgrade to check repeatability. Full
application startup and migration-to-Bridge acceptance remain separate gates.
