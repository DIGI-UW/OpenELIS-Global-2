# Address Hierarchy Configuration

This guide explains how to configure a custom address hierarchy in OpenELIS
using CSV files. This approach allows you to define hierarchical geographic
levels (e.g., Country → Department → Commune → Section → Locality) without code
changes.

## Overview

The address hierarchy uses a simple, maintainable CSV format where **each row
represents a complete path** through the hierarchy. The header row defines the
level names, and each data row specifies locations from the top level down.

## Quick Start

1. Create your hierarchy CSV in
   `configuration/backend/address-hierarchy/my-hierarchy.csv`
2. Restart OpenELIS - the hierarchy is loaded automatically
3. Levels are created from the header row
4. Duplicate locations are automatically deduplicated

## CSV Format

### Basic Format

The header row defines hierarchy level names. Each data row is a complete path:

```csv
Country,Department,Commune,Section,Locality
Haiti,Ouest,Kenscoff,Sourcailles,Duplan
Haiti,Ouest,Kenscoff,Sourcailles,Brouette
Haiti,Ouest,Kenscoff,Belle Fontaine,Catno
Haiti,Nord,Cap-Haitien,Bande du Nord,Vertières
```

### With Optional Codes

Use `Name%Code` format to specify both display name and unique code:

```csv
Country,Department,Commune,Section,Locality
Haiti,Ouest,Kenscoff,Sourcailles%115-03,Duplan
Haiti,Ouest,Kenscoff,Sourcailles%115-03,Brouette
Haiti,Ouest,Kenscoff,Belle Fontaine%115-04,Catno
```

If no code is provided, one is auto-generated from the name.

### Key Features

| Feature              | Description                                          |
| -------------------- | ---------------------------------------------------- |
| Header as levels     | Column names become OrganizationType names           |
| Complete paths       | Each row is a full hierarchy path (easy to maintain) |
| Auto-deduplication   | Same location in multiple rows is created once       |
| Optional codes       | Use `Name%Code` format for explicit codes            |
| Parent relationships | Inferred from column position (left = parent)        |

## Examples

### Haiti Example

```csv
Country,Department,Commune,Section,Locality
Haiti,Ouest,Kenscoff,Sourcailles%115-03,Duplan
Haiti,Ouest,Kenscoff,Sourcailles%115-03,Brouette
Haiti,Ouest,Kenscoff,Sourcailles%115-03,Zorazo
Haiti,Ouest,Kenscoff,Sourcailles%115-03,Fond Ferrier
Haiti,Ouest,Kenscoff,Belle Fontaine%115-04,Catno
Haiti,Ouest,Petion-Ville,Bellevue%116-01,Morne Calvaire
Haiti,Ouest,Petion-Ville,Bellevue%116-01,Thomassin
Haiti,Nord,Cap-Haitien,Bande du Nord%201-01,Vertières
```

### Indonesia Example

```csv
Provinsi,Kabupaten/Kota,Kecamatan,Kelurahan/Desa
DKI Jakarta,Kota Jakarta Selatan,Tebet%317101,Tebet Barat%3171010001
DKI Jakarta,Kota Jakarta Selatan,Tebet%317101,Tebet Timur%3171010002
DKI Jakarta,Kota Jakarta Selatan,Setiabudi%317102,Setiabudi%3171020001
DKI Jakarta,Kota Jakarta Selatan,Setiabudi%317102,Karet%3171020002
Jawa Barat,Kota Bandung,Coblong,Dago
Jawa Barat,Kota Bandung,Coblong,Lebak Siliwangi
```

## Optional: Pre-define Levels

You can optionally pre-define hierarchy levels with a separate `*-levels.csv`
file for more control over level configuration:

```csv
level,typeName,displayKey,sortOrder,defaultValue
1,Country,address.level.country,1,Haiti
2,Department,address.level.department,2,Ouest
3,Commune,address.level.commune,3,
4,Section,address.level.section,4,
5,Locality,address.level.locality,5,
```

### Level Configuration Columns

| Column       | Required | Description                                                   |
| ------------ | -------- | ------------------------------------------------------------- |
| level        | Yes      | Hierarchy level number (1 = top level)                        |
| typeName     | Yes      | OrganizationType name to create                               |
| displayKey   | No       | i18n key for display label                                    |
| sortOrder    | No       | Display order                                                 |
| defaultValue | No       | Default value name to pre-select for new patients (see below) |

### Default Values

The `defaultValue` column allows you to set default address values that are
automatically selected when creating a new patient. This is useful for:

- Sites that primarily serve patients from a specific region
- Reducing data entry time for the most common locations
- Ensuring consistent data entry for single-location deployments

**Example with defaults:**

```csv
level,typeName,displayKey,sortOrder,defaultValue
1,Provinsi,address.level.provinsi,1,DKI Jakarta
2,Kabupaten/Kota,address.level.kabupaten,2,Kota Jakarta Selatan
3,Kecamatan,address.level.kecamatan,3,
4,Kelurahan/Desa,address.level.kelurahan,4,
```

In this example:

- When creating a new patient, "DKI Jakarta" is pre-selected for Provinsi
- "Kota Jakarta Selatan" is pre-selected for Kabupaten/Kota
- The lower levels (Kecamatan, Kelurahan) have no default and must be selected

**Notes:**

- The `defaultValue` must match an organization name exactly at that level
- Defaults cascade: if level 1 has a default, level 2's dropdown is populated
  with its children
- Users can always change the default selection
- Defaults only apply to new patients, not when editing existing patients

If no levels file exists, levels are created automatically from the values CSV
header.

## REST API

The address hierarchy provides REST endpoints for navigation:

### Get All Levels

```
GET /rest/address-hierarchy/levels
```

Returns configured hierarchy levels with optional default values:

```json
[
  {
    "level": 1,
    "typeId": "1",
    "typeName": "Country",
    "defaultValue": "Haiti",
    "defaultId": "123"
  },
  {
    "level": 2,
    "typeId": "2",
    "typeName": "Department",
    "defaultValue": "Ouest",
    "defaultId": "456"
  },
  {
    "level": 3,
    "typeId": "3",
    "typeName": "Commune",
    "defaultValue": null,
    "defaultId": null
  }
]
```

The `defaultValue` is the display name and `defaultId` is the organization ID
that can be used to pre-select values in the UI.

### Get Values at a Level

```
GET /rest/address-hierarchy/level/{levelNumber}
```

Returns all locations at a specific level.

### Get Children

```
GET /rest/address-hierarchy/children?parentId={id}
GET /rest/address-hierarchy/children?parentCode={code}
```

Returns child locations of a parent.

### Get Hierarchy Path

```
GET /rest/address-hierarchy/path/{organizationId}
```

Returns the full path from top level to the specified location.

### Search Locations

```
GET /rest/address-hierarchy/search?query={searchTerm}
```

Searches for locations by name across all levels.

## Backward Compatibility

The system maintains backward compatibility with existing "Health Region" and
"Health District" organization types:

- If no hierarchy CSV is configured, the system uses legacy types
- Existing REST endpoint `/rest/health-districts-for-region` continues to work
- Patient forms with region/district dropdowns work with both configurations

## Configuration Directory

Configuration files are loaded from:

- **Docker/Production:**
  `/var/lib/openelis-global/configuration/backend/address-hierarchy/`
- **Development:** `./volume/configuration/backend/address-hierarchy/`

## File Naming Conventions

- Files ending with `-levels.csv` define hierarchy levels (optional)
- All other `.csv` files are treated as hierarchy value files
- Multiple value files can be used (e.g., by region or country)
- Files are processed in alphabetical order
- Checksums prevent re-processing unchanged files

## Best Practices

1. **One row per leaf**: Each row should go down to the most specific level
2. **Use codes for official IDs**: `SectionName%OFFICIAL-CODE` format
3. **Maintain alphabetical order**: Makes the CSV easier to read and edit
4. **Use multiple files**: Split large hierarchies by region for easier
   maintenance

## Troubleshooting

### Locations not appearing

- Check that all hierarchy levels are in the header row
- Verify the CSV has no empty rows at the end
- Check logs for processing errors

### Duplicate locations

- Duplicates are automatically handled - same name at same level with same
  parent = one entry
- Use explicit codes if you have legitimately different locations with the same
  name

### Labels not updating

- Clear browser cache after configuration changes
- Restart application for label changes to take effect

## See Also

- [Adding Facilities](../addingfacilities.md) - FHIR-based organization setup
- [Configuration Overview](README.md) - General configuration documentation
