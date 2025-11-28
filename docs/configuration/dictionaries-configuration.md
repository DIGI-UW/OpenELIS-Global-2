# Dictionaries Configuration

This directory contains dictionary configuration files in CSV format. Dictionary
entries are loaded automatically during application initialization.

**Note:** Example files are located in the `examples/` subdirectory and are NOT
automatically loaded. Copy them to this directory to use them.

## File Format

Each CSV file should contain a header row followed by dictionary entries with
the following columns:

### Required Columns

- **category**: The name of the dictionary category (will be created if it
  doesn't exist)
- **dictEntry**: The display name of the dictionary entry

### Optional Columns

- **localAbbreviation**: Short code/abbreviation for the entry
- **isActive**: Whether the entry is active ("Y" or "N", defaults to "Y")
- **sortOrder**: Numeric sort order for display (integer)
- **loincCode**: LOINC code if applicable

## Example

```csv
category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode
Sample Types,Whole Blood,WB,Y,1,26881-3
Sample Types,Serum,SER,Y,2,26882-1
Sample Types,Plasma,PLS,Y,3,26883-9
Sample Types,Urine,UR,Y,4,26884-7
Sample Types,Saliva,SAL,Y,5,
```

## CSV Format Notes

- First row must be the header with column names
- Columns can be in any order
- Empty values are allowed for optional fields
- If a value contains commas, wrap it in double quotes: `"Value, with comma"`
- Case-insensitive column names (e.g., "category" or "Category" both work)
- Empty lines are ignored
- Multiple dictionary categories can be in the same file

## How It Works

1. Configuration files are loaded from:

   - Classpath: `src/main/resources/configuration/dictionaries/*.csv`
   - Filesystem:
     `/var/lib/openelis-global/configuration/backend/dictionaries/*.csv` (mapped
     from `./configuration/backend/dictionaries/` in Docker)

2. Files are only reprocessed when their content changes (tracked by checksum)

3. If a dictionary entry already exists with the same name in the same category,
   it will be updated

4. Dictionary categories are automatically created if they don't exist

5. Each row in the CSV creates or updates one dictionary entry

## Examples

### Simple Dictionary with Minimal Fields

```csv
category,dictEntry
Test Results,Positive
Test Results,Negative
Test Results,Inconclusive
```

### Dictionary with All Fields

```csv
category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode
Blood Types,A Positive,A+,Y,1,883-9
Blood Types,A Negative,A-,Y,2,884-7
Blood Types,B Positive,B+,Y,3,885-4
Blood Types,B Negative,B-,Y,4,886-2
Blood Types,O Positive,O+,Y,5,887-0
Blood Types,O Negative,O-,Y,6,888-8
Blood Types,AB Positive,AB+,Y,7,889-6
Blood Types,AB Negative,AB-,Y,8,890-4
```

### Multiple Categories in One File

```csv
category,dictEntry,localAbbreviation,isActive,sortOrder
Sample Types,Blood,BLD,Y,1
Sample Types,Urine,URN,Y,2
Test Status,Pending,PEND,Y,1
Test Status,Complete,COMP,Y,2
Test Status,Cancelled,CANC,Y,3
```

## Notes

- Dictionary entries must have unique names within their category
- If an entry exists in a different category, it will be skipped
- Updates to existing entries will preserve their database relationships
- Set `org.openelisglobal.configuration.autocreate=false` in properties to
  disable automatic loading
- Invalid rows will be logged but won't stop the processing of other rows
