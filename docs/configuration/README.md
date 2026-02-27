# Configuration Documentation

This directory contains detailed documentation for OpenELIS configuration files.

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
