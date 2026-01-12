# MedLab Manifest Validation Test Data

This folder contains test data for demonstrating the MedLab manifest import
validation workflow.

## Files

| File                                            | Description                                    |
| ----------------------------------------------- | ---------------------------------------------- |
| `medlab_manifest_validation.csv`                | CSV manifest with various validation scenarios |
| `../scripts/populate-medlab-validation-data.sh` | Script to create test patients and orders      |

## Setup

### Step 1: Run the Population Script

Before importing the CSV, run the script to create test patients and orders:

```bash
# From the repository root
./scripts/populate-medlab-validation-data.sh

# Or with options:
./scripts/populate-medlab-validation-data.sh --container openelisglobal-database -d clinlims -U clinlims

# To clean existing test data first:
./scripts/populate-medlab-validation-data.sh --clean
```

This creates:

- **4 Patients**: John Doe, Jane Smith, Bob Wilson, Alice Brown
- **3 Electronic Orders**: Two valid orders, one for testing patient-order
  mismatch

### Step 2: Import the CSV

Use the MedLab Manifest Import feature in the Medical Laboratory workflow to
import `medlab_manifest_validation.csv`.

## Test Scenarios in CSV

### Valid Rows (7 rows - will be imported)

| Row | Sample ID | Scenario                                                              |
| --- | --------- | --------------------------------------------------------------------- |
| 2   | VALID-001 | Valid with existing patient (PAT-VALID-001) and order (ORD-VALID-001) |
| 3   | VALID-002 | Valid with existing patient (PAT-VALID-002) and order (ORD-VALID-002) |
| 4   | VALID-003 | Anonymous sample (no patient, no order)                               |
| 5   | VALID-004 | Valid with patient only (no order)                                    |
| 6   | VALID-005 | Valid with order only (no patient)                                    |
| 11  | VALID-006 | Patient found by national ID (NID-AB-22222 → Alice Brown)             |
| 22  | VALID-007 | Valid row at end of file                                              |

### Warning Rows (4 rows - will be imported with warnings)

| Row | Sample ID | Warning Type                                                       |
| --- | --------- | ------------------------------------------------------------------ |
| 7   | WARN-001  | Patient not found (PAT-NONEXISTENT-999)                            |
| 8   | WARN-002  | Order not found (ORD-NONEXISTENT-999)                              |
| 9   | WARN-003  | Both patient and order not found                                   |
| 10  | WARN-004  | Patient-order mismatch (order belongs to Bob Wilson, not John Doe) |

### Error Rows (11 rows - will NOT be imported)

| Row | Sample ID | Error Type                                               |
| --- | --------- | -------------------------------------------------------- |
| 12  | ERR-001   | Invalid sample type ("InvalidSampleType")                |
| 13  | ERR-002   | Invalid container type ("InvalidContainer")              |
| 14  | ERR-003   | Missing sample type                                      |
| 15  | ERR-004   | Missing sample ID (in CSV but empty)                     |
| 16  | (empty)   | Missing sample ID (blank field)                          |
| 17  | ERR-005   | Missing quantity                                         |
| 18  | ERR-006   | Missing collection date                                  |
| 19  | ERR-007   | Missing collection time                                  |
| 20  | ERR-008   | Missing collection source                                |
| 21  | ERR-009   | Missing collector                                        |
| 22  | ERR-010   | Multiple errors (invalid sample type AND container type) |

## Expected Results

After import:

- **Total Rows**: 22
- **Valid Rows**: 11 (7 clean + 4 with warnings)
- **Invalid Rows**: 11 (blocked due to errors)

### Preview Screen Should Show:

1. **Summary Tags**:

   - Total Rows: 22
   - Valid Rows: 11
   - Invalid Rows: 11
   - Warnings: 4

2. **Invalid Rows Table** (red background):

   - 11 rows with error messages explaining why each failed

3. **Valid Rows Table**:
   - 7 rows with no warnings (white background)
   - 4 rows with warnings (yellow background)

### After Import:

- **Samples Created**: 11
- **Skipped Rows**: 11

## Test Data Details

### Patients Created

| External ID   | National ID  | Name        | Purpose                   |
| ------------- | ------------ | ----------- | ------------------------- |
| PAT-VALID-001 | NID-JD-12345 | John Doe    | Valid patient lookup      |
| PAT-VALID-002 | NID-JS-67890 | Jane Smith  | Valid patient lookup      |
| PAT-VALID-003 | NID-BW-11111 | Bob Wilson  | Mismatch testing          |
| PAT-VALID-004 | NID-AB-22222 | Alice Brown | National ID fallback test |

### Orders Created

| External ID      | Patient    | Purpose                        |
| ---------------- | ---------- | ------------------------------ |
| ORD-VALID-001    | John Doe   | Valid order lookup             |
| ORD-VALID-002    | Jane Smith | Valid order lookup             |
| ORD-MISMATCH-001 | Bob Wilson | Patient-order mismatch warning |

## Cleanup

To remove test data, run the script with `--clean`:

```bash
./scripts/populate-medlab-validation-data.sh --clean
```

Or manually:

```sql
DELETE FROM electronic_order WHERE external_id LIKE 'ORD-VALID-%' OR external_id LIKE 'ORD-MISMATCH-%';
DELETE FROM patient WHERE external_id LIKE 'PAT-VALID-%';
DELETE FROM person WHERE first_name IN ('John', 'Jane', 'Bob', 'Alice')
  AND last_name IN ('Doe', 'Smith', 'Wilson', 'Brown');
```
