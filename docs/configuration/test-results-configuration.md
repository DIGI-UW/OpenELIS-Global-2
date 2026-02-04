# Test Results Configuration

This directory contains test result configuration files in CSV format. Test
results (possible values/options for tests) are loaded automatically during
application initialization.

**Note:** Example files are located in the `examples/` subdirectory and are NOT
automatically loaded. Copy them to this directory to use them.

## File Format

Each CSV file should contain a header row followed by test result entries with
the following columns:

### Required Columns

- **testName**: The name of the test (must match an existing test by localized
  name, description, or name)
- **resultType**: The type of result (see Result Types below)

### Optional Columns

- **resultValue**: The result value (required for Dictionary types)
- **dictionaryCategory**: The dictionary category name for Dictionary types
  (recommended to avoid ambiguity when multiple categories have entries with the
  same name)
- **sortOrder**: Numeric sort order for display (auto-assigned if not provided)
- **isQuantifiable**: Whether result can be further quantified ("Y" or "N",
  defaults to "N")
- **isActive**: Whether the result option is active ("Y" or "N", defaults to
  "Y")
- **isNormal**: Whether this is considered a normal result ("Y" or "N", defaults
  to "N")
- **significantDigits**: Number of significant digits for numeric types
- **flags**: Optional flags (e.g., "H" for high, "L" for low)

## Result Types

| Code | Type                  | Description                                      |
| ---- | --------------------- | ------------------------------------------------ |
| D    | Dictionary            | Coded value from a dictionary entry              |
| N    | Numeric               | Numeric result with optional significant digits  |
| A    | Alpha                 | Alphanumeric text                                |
| R    | Remark                | Free-form text/remarks                           |
| T    | Titer                 | Titer ranges (e.g., 1:10, 1:20, 1:40)            |
| M    | Multiselect           | Multiple dictionary values can be selected       |
| C    | Cascading Multiselect | Dependent multiselect from dictionary categories |

## Examples

### Coded/Dictionary Test Results (e.g., HIV Rapid Test)

For tests with predefined result options like "Positive/Negative":

```csv
testName,resultType,resultValue,dictionaryCategory,sortOrder,isQuantifiable,isActive,isNormal,significantDigits,flags
HIV Rapid Test,D,Positive,Test Results,1,N,Y,N,,
HIV Rapid Test,D,Negative,Test Results,2,N,Y,Y,,
HIV Rapid Test,D,Inconclusive,Test Results,3,N,Y,N,,
```

**Important:** For Dictionary (D) result types:

- The `resultValue` must match an existing dictionary entry
- Use `dictionaryCategory` to specify which category to look in (recommended)
- Without `dictionaryCategory`, the system finds the first matching entry
- Create the dictionary entries first using the
  [dictionaries configuration](./dictionaries-configuration.md).

### Numeric Test Results (e.g., Hemoglobin)

For tests with numeric results:

```csv
testName,resultType,resultValue,sortOrder,isQuantifiable,isActive,isNormal,significantDigits,flags
Hemoglobin,N,,1,Y,Y,N,1,
Glucose,N,,1,Y,Y,N,0,
Creatinine,N,,1,Y,Y,N,2,
```

### Mixed Configuration Example

```csv
testName,resultType,resultValue,sortOrder,isQuantifiable,isActive,isNormal,significantDigits,flags
HIV Rapid Test,D,Positive,1,N,Y,N,,
HIV Rapid Test,D,Negative,2,N,Y,Y,,
Hepatitis B Surface Antigen,D,Positive,1,N,Y,N,,
Hepatitis B Surface Antigen,D,Negative,2,N,Y,Y,,
Hemoglobin,N,,1,Y,Y,N,1,
Glucose,N,,1,Y,Y,N,0,
White Blood Cell Count,N,,1,Y,Y,N,2,
```

## CSV Format Notes

- First row must be the header with column names
- Columns can be in any order
- Empty values are allowed for optional fields
- If a value contains commas, wrap it in double quotes: `"Value, with comma"`
- Case-insensitive column names (e.g., "testName" or "TESTNAME" both work)
- Empty lines are ignored
- Multiple tests can be configured in the same file

## How It Works

1. Configuration files are loaded from:

   - Classpath: `src/main/resources/configuration/test-results/*.csv`
   - Filesystem:
     `/var/lib/openelis-global/configuration/backend/test-results/*.csv` (mapped
     from `./configuration/backend/test-results/` in Docker)

2. Files are only reprocessed when their content changes (tracked by checksum)

3. If a test result already exists with the same test, type, and value, it will
   be updated

4. Tests must exist before configuring their results (load order: tests=200,
   dictionaries=300, test-results=310)

5. For Dictionary types, dictionary entries must exist before configuring test
   results

## Workflow for Configuring Coded Tests

1. **Create dictionary entries** (if they don't exist):

   ```csv
   # dictionaries/test-result-options.csv
   category,dictEntry,localAbbreviation,isActive,sortOrder
   Test Results,Positive,POS,Y,1
   Test Results,Negative,NEG,Y,2
   Test Results,Inconclusive,INCON,Y,3
   ```

2. **Create the test** (if it doesn't exist):

   ```csv
   # tests/my-tests.csv
   testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,englishName
   HIV Rapid Test,Serology,Whole Blood|Serum|Plasma,68961-2,Y,Y,20,HIV Rapid Test
   ```

3. **Configure test results**:
   ```csv
   # test-results/my-test-results.csv
   testName,resultType,resultValue,sortOrder,isQuantifiable,isActive,isNormal
   HIV Rapid Test,D,Positive,1,N,Y,N
   HIV Rapid Test,D,Negative,2,N,Y,Y
   HIV Rapid Test,D,Inconclusive,3,N,Y,N
   ```

## Notes

- Dictionary entries are referenced by their `dictEntry` name (display name)
- When using Dictionary type, the system stores the dictionary ID internally
- Invalid rows will be logged but won't stop the processing of other rows
- Test names can be matched by localized name, description, or test name
- Sort order is auto-assigned based on existing results if not specified
