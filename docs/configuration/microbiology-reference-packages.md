# Microbiology reference configuration

Reference metadata can be released independently of OpenELIS. A deployment pins
and verifies a release, installs its ordinary CSV files, and uses the existing
configuration startup/reload and Import Catalog workflows. OpenELIS does not
read release archives, download metadata or run R.

## Configuration domains

| Folder under `configuration/backend/` | Required columns                         | Order |
| ------------------------------------- | ---------------------------------------- | ----- |
| `microbiology-organisms/`             | `source,code,displayName,whonetCode`     | 310   |
| `microbiology-antimicrobials/`        | `source,code,displayName,whonetCode`     | 320   |
| `microbiology-breakpoints/`           | Existing breakpoint import columns below | 330   |

Organisms also accept `gramStain`; antimicrobials accept `antibioticClass`.
`source` and `code` identify a reference concept independently of its local name
and database identifier. An existing source identity retains local values and
activity. Unambiguous pre-existing name/code matches can adopt an identity;
conflicting matches fail the catalog import. These files do not choose a site's
panels, routing, significance or clinical protocol.

Breakpoint files use the existing administrator CSV importer:

```text
publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units
```

Optional columns are `resistant_inclusive`, `organism_source`, `organism_code`,
`antibiotic_source`, `antibiotic_code` and `notes`. Source codes resolve
concepts without installation-specific IDs. `resistant_inclusive=false`
preserves strict resistance boundaries; omission retains the existing inclusive
convention. Imports validate supported measurements and exact storage precision.

## Import, preview and reload

The existing configuration loader discovers these files, orders dependencies,
records file checksums and exposes failures in its reload result. Import Catalog
supports preview and apply, with row summaries. Preview does not create concepts
or editions. Preview files separately after their dependencies exist; the shared
preview mechanism does not simulate writes across files. Combined apply orders
concepts before breakpoint files.

A failed file rolls back its writes and is not marked successfully loaded.
Previously successful concept files may remain when a later breakpoint file
fails. Each breakpoint file is atomic: it cannot leave half an edition loaded.
Provide the complete edition in one file; do not split it across files. New
editions load inactive. An authorized administrator explicitly activates one. An
application's HTTP health alone does not demonstrate a successful import.

Configuration-managed editions reject changed content under the same identity
and cannot be edited through the rule editor or an administrator CSV upload.
Identity checks use the normalized rows of each edition, independently of file
ordering, other editions in the same CSV, ZIP bytes or unrelated release
evidence. A correction must use a new edition version. Reloads preserve
activation and existing IDs. Retain historical editions referenced by results.

An explicit rule with empty thresholds is valid: it yields “No breakpoint” and
prevents matching from falling through to a broader numeric rule. Intermediate
intervals must not overlap susceptible or resistant ranges. Imported notes are
part of the rule content and edition identity.

## Independently published metadata

The AMR exporter supplies normal configuration folders plus provenance,
attribution, coverage and source-generated comparison cases. Package validation
belongs to release/deployment preparation; comparison cases run in receiving
application compatibility tests. They are not application startup inputs.

The deployment owns acquisition and pinning. PNG's initializer uses the generic
`prepare-configuration-packages.py` adapter: HTTPS URL, package identity/version
and reviewed SHA-256 in deployment configuration; verified offline cache; safe
installation into existing configuration folders without overwriting site files.
The adapter accepts metadata from any publisher using the documented layout. No
AMR dataset, R runtime or source-repository dependency is bundled into OE2.

The first AMR candidate covers selected EUCAST 2025/2026 MIC contexts. It does
not provide complete PNG testing coverage. Review its exclusions and compatible
application version before activating any imported edition. Schema changes use
Liquibase; reference concepts and yearly data use configuration files only.
