# OGC-1054 Analyzer Contract Migration

This contract governs migration from current OpenELIS analyzer stores and raw
ingress to the accepted Bridge-profile/OpenELIS-site-binding architecture. It is
executable through the E0 characterization tests and the versioned Bridge v1
consumer contracts. It does not authorize M1 lifecycle production code.

## Current-State Inputs

| Input | Current role | Target handling |
| --- | --- | --- |
| `defaultConfigId` | Transient create hint for a bundled OpenELIS profile | Read during migration only; replace with an explicit Bridge profile ID/revision pin |
| `analyzer_plugin_config.config` | Mixed copied profile and analyzer settings | Extract for preflight; retain only analyzer-instance overrides, never profile authority |
| `analyzer_test_map` | Analyzer-specific source-code to Test rows | Preserve every independent source row and local binding in the revision-scoped site binding |
| qualitative result mappings | Analyzer value to free-text OpenELIS code | Bind explicitly to one active Result Option owned by the selected Test or block |
| Analyzer rows | Installed analyzer identity and settings | Preserve identity; attach an explicit profile revision and site-binding candidate |
| `AnalyzerQcRule` | Superseded per-analyzer control classifier | Inventory and migrate to pinned Bridge profile recognition, then remove |
| raw analyzer routes | Mixed active and deprecated OpenELIS readers | Retire after normalized Bridge traffic is proven and rollback gates pass |
| local Test/Result Option catalog | Site-owned clinical concepts | Preserve in OpenELIS and expose explicit zero/one/many/inactive/missing states |

## No-loss invariants

1. Preserve every analyzer identity, lab-unit relationship, instance setting,
   source test row, source result row, local binding, explicit exclusion, and
   relevant audit fact. Migration must not collapse aliases or copy one source
   row's decision to another.
2. Select the target profile identity/revision explicitly. Name, protocol,
   mapping similarity, LOINC, or copied JSON may inform a visible suggestion but
   never establish the pin.
3. Compare `AnalyzerQcRule` behavior only after explicit profile selection.
   Canonical equivalence is equality of the complete sorted active rule semantics,
   including rule type, target field where applicable, and operand. Subset,
   superset, ordering coincidence, or fingerprint text alone is not equivalence.
4. Exact recognition behavior may be discarded only after the profile pin,
   site-binding verification, Bridge desired-state acknowledgment, migration
   export, and audit outcome all succeed.
5. Valid divergent recognition becomes a newly published site profile identity
   and immutable revision through the Bridge lifecycle before pinning. It is not
   stored as an analyzer override.
6. Invalid or untransformable recognition is a visible blocking anomaly. No
   default, hidden fallback, silent drop, per-analyzer classifier, or parallel
   runtime is allowed.
7. Inventory active and inactive `AnalyzerQcRule` rows. Only active rows define
   effective legacy behavior; inactive rows remain preserved in the export and
   audit until migration acceptance.
8. Resolve qualitative values only to active Result Options owned by the mapped
   Test. Free text, inactive options, options from another Test, missing LOINC,
   and ambiguous candidates remain explicit.
9. Operational QC data and behavior are not transformed by this migration.
   `QCControlLot`, `QCResult`, statistics, Westgard evaluation, violations, and
   alerts remain OpenELIS-owned and never gate migration, verification, or
   activation.

## Preflight outcomes

Preflight is read-only, rerunnable, and deterministic for unchanged source data.
It produces one disposition for every analyzer and one outcome for every source
row. A rerun cannot publish profiles, modify bindings, delete rules, or activate
runtime paths.

- `READY_EXACT_PROFILE_PIN`: the explicitly selected profile is valid, every
  required local row has one valid binding or explicit exclusion, and legacy
  recognition is canonically identical.
- `READY_AFTER_SITE_PROFILE_PUBLISH`: current recognition is schema-valid but
  divergent; migration names the complete candidate behavior that BR-M1 must
  publish as a new site profile identity/revision.
- `BLOCKED_INVALID_SOURCE`: copied JSON, recognition, or another source value is
  invalid or untransformable.
- `BLOCKED_CATALOG_BINDING`: a required local Test or Result Option has zero,
  multiple, inactive-only, missing-identity, or wrong-owner candidates.
- `BLOCKED_PROFILE_SELECTION`: no explicit retained Bridge profile revision has
  been selected.

The report lists every blocking anomaly, affected analyzer and source-row
identity, current value, expected action, and whether correction belongs in
Bridge profile management or OpenELIS local binding. A unique active catalog
candidate may be suggested but is never silently committed.

## One-writer cutovers

| Capability | Before cutover | Cutover gate | After cutover |
| --- | --- | --- | --- |
| Portable profile publication | Existing shipped/bootstrap sources | BR-M1 immutable catalog and lifecycle contracts pass | Bridge is sole portable-profile writer |
| Local site binding | Legacy analyzer mapping stores | OE-M1 no-loss migration and audit pass | OpenELIS revision-scoped site binding is sole writer |
| Control recognition | OE rules plus legacy Bridge fallbacks | BR-M2 normalized fixture tests prove pinned-profile-only classification | Bridge pinned profile is sole recognition runtime |
| Legacy classifier writes | OpenELIS rule editor/service | OE-M2 migration outcomes accepted | No `AnalyzerQcRule` writer, UI, activation gate, or registration payload remains |
| Normalized analyzer traffic | Mixed OpenELIS raw readers and Bridge paths | BR-M4 mock/Bridge/OE contracts pass for ASTM, HL7, and FILE | Bridge normalized FHIR is sole analyzer-result writer |
| Legacy schema/readers | Migration-only readers | OE-M4 export, anomaly clearance, and rollback rehearsal pass | `AnalyzerQcRule` schema and raw OpenELIS analyzer readers are removed |

The phase transition is `READ_LEGACY_WRITE_LEGACY` to
`READ_LEGACY_WRITE_TARGET_ONLY_AFTER_PREFLIGHT` to
`READ_TARGET_WRITE_TARGET`. No phase writes both legacy and target stores.

## Rollback

Before the first target write, capture and verify a coordinated OpenELIS database
backup, Bridge profile-catalog backup, migration export, and target contract
version. A rollback before target writes leaves legacy behavior unchanged.

If a cutover fails before any accepted target write, restore both the
pre-cutover OpenELIS database and Bridge profile catalog. If accepted target
writes have occurred, stop new writes and either restore the coordinated pair or
roll forward from the preserved source/export. Never reverse-transform target
writes into legacy rows, enable dual write, or re-enable a hidden classifier.

Rollback validation must prove the restored analyzer identities, mappings,
profile pins, runtime writer, and traffic route agree as one state. Restoring
only one repository's state is not a valid rollback.

## Runtime removal

1. OE-M2 removes the `AnalyzerQcRule` editor, routes, controller, service/DAO
   runtime callers, activation condition, registration state, and all new writes.
   A read-only E0 migration reader may remain until final schema removal.
2. BR-M2 removes OE-pushed classifier consumption and all hard-coded control
   detection. `RULES` and affirmed `NONE` use only the pinned profile revision.
3. BR-M4 makes normalized FHIR the analyzer-result transport for ASTM, HL7, and
   FILE and proves FILE watching/transport remains Bridge-only.
4. OE-M4 removes `/importAnalyzer`, `/analyzer/astm`, `/analyzer/hl7`,
   `/analyzer/runAction`, the final migration adapter, and the
   `analyzer_qc_rule` schema after preflight acceptance and rollback rehearsal.
5. Repository guards at M4 fail if an OE FILE poller, `QcRun`, per-analyzer
   mapping editor, duplicate pending queue, dual writer, copied-profile
   authority, `AnalyzerQcRule` runtime/schema/UI path, or classifier fallback
   remains.

Operational QC remains available before, during, and after these removals as a
separate analyzer-linked result-review concern.
