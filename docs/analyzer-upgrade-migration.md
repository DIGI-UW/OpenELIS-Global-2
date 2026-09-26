# Existing analyzer upgrade

## Scope agreed for PR #4433

Transfer retained settings to Bridge after application startup, using the
existing profile, shared mapping and connection services. The expected case is
one analyzer per old profile, or several with identical saved test mappings.
Keep original analyzer IDs, lab units, results, history and source settings. Do
not add a migration framework or manufacture verification or activation.

This complements #4431: that change retained the old configuration and removed
premature cleanup from the startup changelog. It did not transfer configuration.
No additional Liquibase changes are needed for this transfer.

## Eligibility and transfer

A record is eligible when it has **no Bridge connection reference** and still
has retained configuration: an old analyzer type/profile reference, host or file
directory, plugin settings, test mappings, or active serial settings. Creation
dates are not used. Normal new setup leaves that old storage empty; an already
transferred analyzer is skipped because its Bridge reference exists.

1. Use its existing pinned profile reference where present. Otherwise match the
   old type name exactly to a unique active Bridge profile family (display name
   is case insensitive), choosing its latest active revision. An administrator
   can supply an explicit profile if the name does not match unambiguously.
2. Read only values declared by that profile's connection fields from retained
   plugin configuration. Fill missing host, directory and file pattern from the
   retained columns. Translate old communication direction into the existing
   Bridge connection fields. Profile defaults supply omitted values. Old shared
   listener ports are not copied into outbound endpoint settings.
3. Reuse the current shared-mapping services. Preserve saved test targets;
   automatic defaults fill gaps. A saved mapping that conflicts with a binding
   already used by another analyzer remains pending. Categorical answers are not
   invented, and answer defaults are cleared if their test target changes.
4. Commit the selected profile and mappings on the original analyzer, in Setup
   and inactive. Then call the existing Bridge connection client outside the
   database transaction, passing that same analyzer ID as `clientAnalyzerId`.
5. Save the returned connection ID on the original record. If the process stops
   between Bridge creation and reference storage, retry uses the selected
   profile and retained values. Bridge reconciles equivalent creation requests
   by `clientAnalyzerId`; it does not need a new analyzer record.

A successful transfer means the configuration and connection reference were
saved. The operator still reviews/confirms mappings and control recognition,
checks the connection and activates through the existing setup workflow. Results
continue to belong to the original analyzer record.

## Startup, pending records and retry

`AnalyzerUpgradeStartup` uses the existing daemon scheduler for one attempt, 30
seconds after scheduler initialization. Failures are caught outside startup;
Bridge being unavailable cannot block database upgrades or application startup.
The production startup trigger is disabled under the test profile.

Each attempt uses the existing configuration-import history with source
`ANALYZER_UPGRADE`. It stores per-analyzer `MIGRATED` or `PENDING` outcomes and
pending reasons. Untransferred records remain listable in Analyzers, with a
**Retry configuration transfer** action. Completed records are not retried.

Authenticated users with analyzer-import or administrator access can also use:

- `GET /rest/analyzer/upgrade`: current pending analyzers and latest reasons.
- `POST /rest/analyzer/upgrade` with `{}`: rerun the same transfer service.
- Supply explicit profile selection for an unmatched existing analyzer:

```json
{
  "123": { "profileId": "profile-id-from-bridge", "revision": 1 }
}
```

The selection does not move an analyzer whose profile is already pinned.
Unresolved profile identity, invalid configuration, conflicting shared mappings,
component-specific mappings, active serial settings, or file parsing overrides
that differ from the profile remain pending. This scoped release does not guess
how to convert those configurations. Retain them for explicit review. Bridge
validates the submitted connection values through its existing API.

## Focused acceptance

Per the agreed release scope, automated testing concentrates on:

- A populated ASTM/HL7/FILE upgrade using real PostgreSQL and OE2 services:
  original IDs and lab units, retained settings/test mappings, saved Bridge
  references, no automatic confirmation or activation, and repeat execution.
- One interrupted-reference-save scenario: show the pending reason, resume with
  the same profile/configuration and analyzer ID, and retain one connection.
- Normalized result ingestion through the real import service after explicit
  mapping confirmation, reaching the original analyzer and mapped test.
- The visible retry action, plus existing analyzer service and ORM checks.

Only the remote Bridge API boundary is substituted in the new integration
fixture. A larger failure matrix, deployed browser/instrument acceptance and
actual network/filesystem permissions are separate release checks. These tests
do not claim physical-instrument qualification.

Local validation for this addition: 22 backend tests (including the two
populated upgrade/recovery scenarios) and 29 analyzer-list UI tests passed.
Spotless, changed-file Prettier and whitespace checks passed. Repository-wide
TypeScript checking remains failing; comparison against the prior PR head found
the same 1,983 diagnostics and no new diagnostics. GitHub CI and deployed
acceptance are not covered by those local results.

## Later cleanup

The read-only `AnalyzerUpgrade*` entities are migration inputs, not runtime
configuration owners. Bridge owns runtime configuration and analyzer-facing
behavior; OpenELIS owns clinical bindings and the connection reference.

Remove these readers and retained storage together in a later release, after
populated-site transfer and delivery are verified. Use new changeset IDs. Do not
restore the removed cleanup changesets: their old guards did not prove transfer.
Data already deleted by an earlier release requires backups; this service cannot
reconstruct missing settings, mappings or history.
