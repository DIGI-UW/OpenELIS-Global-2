# Configuration Documentation

This directory contains detailed documentation for OpenELIS configuration files.

Concepts, catalogs, and defaults belong in these configuration files and their
application loaders. Liquibase handles schema changes and conversions of
existing historical records; do not add concept seeds to migrations. Imports
must be repeatable and preserve referenced identifiers and local customizations.
Verify fresh installation, reload, and upgrade behavior when moving a catalog.

## Available Configurations

### [Roles Configuration](roles-configuration.md)

Configure user roles with hierarchical relationships, permissions, and display
settings.

- **Location:** `configuration/backend/roles/`
- **Format:** CSV
- **Features:** Hierarchical roles, internationalization, role grouping

### [Dictionaries Configuration](dictionaries-configuration.md)

Configure dictionary entries for dropdowns and data validation.

- **Location:** `configuration/backend/dictionaries/`
- **Format:** CSV
- **Features:** Categories, LOINC codes, abbreviations, sorting

### [Questionnaires Configuration](questionnaires-configuration.md)

Configure FHIR Questionnaire resources for data collection forms.

- **Location:** `configuration/backend/questionnaires/`
- **Format:** JSON (FHIR R4)
- **Features:** FHIR-compliant questionnaires, automatic UUID assignment,
  checksum tracking

### [Address Hierarchy Configuration](address-hierarchy-configuration.md)

Configure hierarchical geographic/administrative divisions (e.g., Province →
District → Sub-District → Village).

- **Location:** `configuration/backend/address-hierarchy/`
- **Format:** CSV
- **Features:** Configurable hierarchy levels, parent-child relationships,
  backward compatible with Health Region/District

### [Microbiology Reference Configuration](microbiology-reference-packages.md)

- **Locations:** `configuration/backend/microbiology-organisms/`,
  `microbiology-antimicrobials/`, and `microbiology-breakpoints/`
- **Format:** ordinary configuration CSVs, optionally installed from an
  independently published, deployment-pinned metadata package
- **Features:** shared catalog preview/apply/reload, source identities, local
  customization preservation, immutable inactive editions and exact boundaries

## How Configuration Loading Works

Configuration files are loaded automatically during application startup:

1. **Files are discovered** from:
   - Classpath: `src/main/resources/configuration/{domain}/*.csv`
   - Filesystem: `/var/lib/openelis-global/configuration/backend/{domain}/*.csv`
     (mapped from `./configuration/backend/{domain}/` in Docker)

2. **Checksums are tracked** to prevent reprocessing unchanged files

3. **Create or update** domain entries based on unique identifiers

4. **Examples are ignored** - Files in `examples/` subdirectories are not loaded

## Disabling Auto-loading

To disable automatic configuration loading, add to your properties file:

```properties
org.openelisglobal.configuration.autocreate=false
```

## Adding New Configuration Domains

To add a new configuration domain:

1. Create handler implementing `DomainConfigurationHandler`
2. Annotate with `@Component`
3. Implement `getDomainName()`, `getFileExtension()`, and
   `processConfiguration()`
4. Optionally override `getFileMatcher()` to return an Ant-style glob pattern
   (e.g., `*-levels.csv`) if the handler should only match a subset of files.
   The default is `*.{extension}`. When multiple handlers share a domain,
   handlers with lower `getLoadOrder()` claim matching files first; later
   handlers with broader patterns automatically skip already-claimed files.
5. Add documentation following the pattern in this directory

## See Also

- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Contributing guidelines
- [README.md](../../README.md) - Project overview

### Microbiology environmental and surveillance configuration

Environmental observation types are packaged in
`src/main/resources/configuration/observation-history-types/`. The existing
observation-history-types loader imports them; installations can supply an
instance-specific CSV through the configuration directory.

Reporting track concepts use the existing dictionary loader and
`configuration/microbiology-reporting-tracks/default.csv`. This catalog has its
own configuration domain so an unrelated dictionary catalog cannot hide it.
`configuration/program-reporting-tracks/*.csv` assigns those configured tracks
to existing Programs, after dictionary and questionnaire loading. Its columns
are `programCode,reportingTrack`; the track must be an active entry in the
`Reporting track` category. Unmatched Program codes are left alone. Imports
follow the normal configuration checksum and reload behavior. Program
administrators can also select the track in Program management.

These catalogs are configuration, not Liquibase seed data. The M8 migration only
changes storage and converts existing legacy case data.

Culture atmospheres, reading codes, quantities, and extension reasons are loaded
from `configuration/microbiology-cultures/default.csv` through the dictionary
importer. The former M4 concept seed changesets have been removed. Existing
entries retain their identifiers when the configuration is imported, and
site-specific entries are retained. Installations can override this catalog in
the `microbiology-cultures` configuration directory.

Container label presets now load from
`configuration/label-presets/microbiology.csv`, and the self-validation default
loads from `configuration/microbiology-settings/default.csv` through the
site-information importer. Both catalogs use `createOnly=true` to retain
existing administrator choices, including enabled self-validation protection. A
site configuration can set `createOnly=false` to update existing values. Their
former M3 and M7 seed changesets have been removed; upgrading does not delete
existing settings or labels.

The earlier patient-origin catalog also loads through configuration, from
`configuration/microbiology-patient-origins/default.csv`. Matching codes retain
their identifiers and existing WHONET mappings. The original migration checksum
remains explicitly accepted so existing installations upgrade without clearing
Liquibase checksums or recreating those concepts.
