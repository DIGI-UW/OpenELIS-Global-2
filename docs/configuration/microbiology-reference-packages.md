# Microbiology reference configuration

Reference metadata can be released independently of OpenELIS. A deployment pins
and verifies a release, installs its ordinary CSV files, and uses the existing
configuration startup/reload and Import Catalog workflows. OpenELIS does not
read release archives, download metadata or run R.

## Configuration domains

| Folder under `configuration/backend/` | Required columns                         | Order |
| ------------------------------------- | ---------------------------------------- | ----- |
| `microbiology-whonet-organisms/`      | WHONET's `Organisms.txt`, see below      | 305   |
| `microbiology-whonet-antibiotics/`    | WHONET's `Antibiotics.txt`, see below    | 306   |
| `microbiology-whonet-breakpoints/`    | WHONET's `Breakpoints.txt`, see below    | 307   |
| `microbiology-organisms/`             | `source,code,displayName,whonetCode`     | 310   |
| `microbiology-antimicrobials/`        | `source,code,displayName,whonetCode`     | 320   |
| `microbiology-breakpoints/`           | Existing breakpoint import columns below | 330   |

Organisms also accept `gramStain`; antimicrobials accept `antibioticClass`.
`source` and `code` identify a reference concept independently of its local name
and database identifier. An existing source identity retains local values and
activity. Unambiguous pre-existing name/code matches can adopt an identity;
conflicting matches fail the catalog import. These files do not choose a site's
panels, routing, significance or clinical protocol.

## WHONET's organism list

`microbiology-whonet-organisms/` takes `Organisms.txt` exactly as
[WHONET publishes it](https://github.com/AClark-WHONET/AMRIE): tab-separated,
with WHONET's column names. The `amr-reference-eucast` and `amr-reference-clsi`
packages carry the file unchanged, so OpenELIS offers the organisms, names and
codes a WHONET user knows. The table arrives through the configuration folder
only: Import Catalog does not offer this domain.

- The whole table is kept, every row and column, including the older names a
  code has carried and codes WHONET has replaced. Loading a file replaces the
  table with that file.
- Each current entry (`TAXONOMIC_STATUS` of `C`) is an organism, under WHONET's
  name and identified by its WHONET code. "Klebsiella sp." is `kl-`;
  "Escherichia coli" (`eco`) and "Escherichia coli O157:H7" (`157`) are separate
  organisms.
- An organism the laboratory already has under a WHONET code keeps its record,
  panel and notes, and takes WHONET's current name. One with the same name and
  no code takes the code.
- If WHONET's name for a code already belongs to an organism with a different
  code, the file is not loaded and the report names both codes.

## WHONET's antibiotic list

`microbiology-whonet-antibiotics/` takes `Antibiotics.txt` the same way:
tab-separated, WHONET's columns, configuration folder only.

- The whole table is kept. WHONET lists a code once per disk potency, so
  amoxicillin/clavulanic acid (`AMC`) has a row for 20/10µg, 2/1µg and others.
- Each code is one antibiotic, identified by its WHONET code and named after the
  code's first WHONET entry (lowest `ABX_NUMBER`). Its class comes from that
  entry when the laboratory has not set one.
- An antibiotic the laboratory already has under a WHONET code keeps its record
  and notes and takes WHONET's name. One with the same name and no code takes
  the code. A name that belongs to an antibiotic with a different code fails the
  file, naming both codes.

## WHONET's breakpoint table

`microbiology-whonet-breakpoints/` takes `Breakpoints.txt` the same way:
tab-separated, WHONET's columns, configuration folder only.

- The whole table is kept as text, every row and column: each guideline, year,
  host and breakpoint type WHONET lists, including animal and ECOFF rows.
  Loading a file replaces the table with that file.
- `GUIDELINES`, `YEAR`, `TEST_METHOD`, `ORGANISM_CODE` and `BREAKPOINT_TYPE` are
  required. A row missing one fails the file, naming the line, and the table
  keeps its earlier rows.
- The human clinical breakpoints of the three latest CLSI and EUCAST years
  become breakpoint editions, loaded inactive. An edition is named after its
  year and the package version, as in `2026-2026.4`. A year whose rules an
  edition already holds gets no second edition from a later package. Changed
  rules under a version already loaded fail the file.
- A rule keeps WHONET's organism level (serovar group, organism, species group,
  genus, genus group, family, anaerobe or all organisms), site of infection and
  disk potency. A reading takes the rule at the most specific level its organism
  belongs to in WHONET's organism table, then the site WHONET prefers. A rule
  without values at a closer level means "No breakpoint" and stops a broader
  rule from applying.
- A rule whose site of infection is a urinary tract infection applies only to a
  urine specimen, and a urine specimen prefers it. A specimen is urine when its
  sample type carries WHONET's specimen code `ur`. Any other specimen never
  takes a urinary rule: when the closest level has no other rule the reading has
  "No breakpoint".
- WHONET writes `R` as the first resistant value, so an MIC at or above it and a
  zone at or below it are resistant. An MIC between dilutions is read at the
  next dilution.
- A rule for an antibiotic code that `Antibiotics.txt` does not list is left
  out and named in the log.

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

CLSI MIC interpretation rounds observations up to the conventional dilution
ladder used by AMR for R; the original measured value stays on the reading.
EUCAST MIC and disk diameters are not rounded by this rule. The run's
snapshotted standard determines the publisher for both manual and instrument
results.

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
