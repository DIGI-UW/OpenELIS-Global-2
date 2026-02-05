# Test Sections Configuration

## Overview

**Test Sections** (also called departments, labs, or units) represent the
different laboratory departments or sections in your organization.

**File:** `example-test-sections.csv`

## Format

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
```

### Column Descriptions

| Column            | Required | Description                                 | Example                                       |
| ----------------- | -------- | ------------------------------------------- | --------------------------------------------- |
| `testSectionName` | Yes      | Unique identifier name for the test section | `Bioanalytical Laboratory`                    |
| `description`     | Yes      | Human-readable description                  | `Bioanalytical Testing Laboratory Department` |
| `isActive`        | Yes      | Activate this test section? (`Y` or `N`)    | `Y`                                           |
| `sortOrder`       | Yes      | Display order in lists                      | `1`                                           |
| `isExternal`      | Yes      | Is this an external lab? (`Y` or `N`)       | `N`                                           |
| `englishName`     | Yes      | English display name                        | `Bioanalytical Laboratory`                    |
| `frenchName`      | Yes      | French display name                         | `Laboratoire Bianalytique`                    |

## Example

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
Hematology,Hematology Department,Y,1,N,Hematology,Hématologie
Biochemistry,Biochemistry Department,Y,2,N,Biochemistry,Biochimie
Bioanalytical Laboratory,Bioanalytical Testing Laboratory Department,Y,13,N,Bioanalytical Laboratory,Laboratoire Bianalytique
```

## How It Works

1. When the application starts, it reads this CSV file
2. For each row, it creates or updates a test section in the database
3. Other configurations can then reference these test sections by their
   `testSectionName`
4. Users can be assigned to test sections
5. Notebooks and tests can be filtered by test section

## Setup Steps

### Step 1: Add Test Sections to CSV

Edit `example-test-sections.csv` and add your test sections:

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
My Custom Lab,Custom Laboratory Department,Y,1,N,My Custom Lab,Mon Laboratoire Personnalisé
Research Division,Research Division Department,Y,2,N,Research Division,Division de Recherche
```

### Step 2: Restart the Application

The configuration is loaded when the application starts:

```bash
# Docker example
docker-compose restart openelis

# Or stop/start your application
```

### Step 3: Verify in Database

After restart, verify test sections were created:

```sql
SELECT id, name_english, name_french, is_active, sort_order
FROM test_section
ORDER BY sort_order;
```

## Important Notes

### Test Section Names Must Match

When you configure notebooks to use test sections in
`notebook-departments/research-lab-linkages.csv`, the department name **must
exactly match** the `testSectionName` in this file:

**test-sections/example-test-sections.csv:**

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
Bioanalytical Laboratory,Bioanalytical Testing Lab,Y,13,N,Bioanalytical Laboratory,Laboratoire Bianalytique
```

**notebook-departments/research-lab-linkages.csv:**

```csv
notebookTitle,departmentName
Bioanalytical Notebook,Bioanalytical Laboratory
```

Notice: `testSectionName` in test-sections matches `departmentName` in
notebook-departments.

### Active Status

Only test sections with `isActive = Y` will be:

- Available for user assignment
- Visible in department selection at login
- Able to have notebooks linked to them

Set to `N` to hide a test section without deleting it.

### Sort Order

Test sections are displayed to users in order of `sortOrder` (ascending). Use
numbers like 1, 2, 3, etc. to control the order departments appear in dropdowns
and selection screens.

### External vs. Internal

Set `isExternal = Y` if this is a reference lab or external facility. Use `N`
for internal departments.

## Bilingual Support

This system supports English and French names:

- **englishName**: Displayed when user's language is set to English
- **frenchName**: Displayed when user's language is set to French

Users see the appropriate language based on their language preference in
application settings.

## Modifying Test Sections

### Adding New Test Sections

1. Add a new row to `example-test-sections.csv`
2. Restart the application
3. The new test section will be created in the database

### Updating Existing Test Sections

1. Modify the row in `example-test-sections.csv`
2. Restart the application
3. The database record will be updated

### Deactivating Test Sections

1. Change `isActive` to `N` for that row
2. Restart the application
3. The test section will be hidden but not deleted

### Deleting Test Sections

Test sections cannot be deleted via configuration (to prevent data loss).

To permanently remove a test section:

1. First, unassign all users from it
2. Unlink all notebooks from it
3. Manually delete from database (if needed)

## Default Test Sections

The example file includes these standard lab sections:

- Hematology
- Biochemistry
- Serology
- Parasitology
- Urinalysis
- Microbiology
- Immunology
- Molecular Biology
- Cytology
- Histopathology
- Genomics & Bioinformatics Laboratory
- Traditional & Modern Medicine Research Lab
- Bioanalytical Laboratory
- Bioequivalence Laboratory

You can modify these or add your own.

## Troubleshooting

### Changes to CSV Don't Take Effect

**Solution:**

1. Stop the application
2. Wait a few seconds
3. Verify the CSV file has correct format
4. Restart the application
5. Allow time for startup to complete
6. Check database with SQL query above

### Can't Find Test Section When Configuring Notebooks

**Solution:**

1. Verify the test section was created: `SELECT * FROM test_section;`
2. Verify `testSectionName` in test-sections CSV matches exactly what you're
   using in notebook-departments CSV
3. Check that `isActive = Y` for that test section
4. Names are case-sensitive

### Test Section Appears in Database But Not in UI

**Checklist:**

- [ ] `isActive = Y` in database
- [ ] Browser cache cleared
- [ ] Application restarted
- [ ] User is logged out and back in

## Related Configuration

- **Linking Notebooks to Test Sections:** `../notebook-departments/README.md`
- **Assigning Users to Test Sections:** See NOTEBOOK_DEPARTMENT_DEBUG.md
- **Configuration Property:** `REQUIRE_LAB_UNIT_AT_LOGIN` (in application
  properties)

## Reference

Test sections are processed by the **TestSectionConfigurationHandler** class
which:

- Loads on application startup
- Processes each line in the CSV
- Creates or updates test sections in the database
- Manages bilingual display names

See the debugging guide: `NOTEBOOK_DEPARTMENT_DEBUG.md` for more information on
setting up the complete workflow.
