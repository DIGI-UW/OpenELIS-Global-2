# Notebook Department Configuration

This directory contains CSV files for linking notebook templates to test sections (departments). This configuration allows specific test sections to see and access notebook templates in dropdown menus.

## File Format

CSV files in this directory should follow this format:

```csv
notebookTitle,departmentName
Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab
Genomics & Bioinformatics Laboratory,Genomics & Bioinformatics Laboratory
Bioanalytical Laboratory,Bioanalytical Laboratory
Bioequivalence Laboratory,Bioequivalence Laboratory
```

## Column Requirements

- **notebookTitle**: The exact title of the notebook template (must exist in database with `is_template = true`)
- **departmentName**: The exact name of the test section/department (must exist in `test_section` table)

## Processing Rules

1. **Required Fields**: Both notebookTitle and departmentName are required
2. **Exact Match**: Names must match exactly with database records
3. **Templates Only**: Only notebook templates (`is_template = true`) can be linked
4. **Duplicate Prevention**: Existing associations are automatically skipped
5. **Comments**: Lines starting with `#` are ignored
6. **Empty Lines**: Blank lines are ignored

## Load Order

This configuration has a load order of **220**, meaning it processes after:
- Test sections (load order: 100)
- Notebook templates (load order: varies)

## Error Handling

The handler will log warnings for:
- Missing notebook templates
- Missing test sections
- Empty required fields
- Duplicate associations

## Configuration Files

Place CSV files in this directory. The system will automatically:
1. Detect and process all `.csv` files
2. Track file checksums to prevent reprocessing unchanged files
3. Log processing results

## Example Usage

To link a new notebook template to a department:

1. Add a line to an existing CSV file or create a new one:
   ```csv
   notebookTitle,departmentName
   My New Research Template,Research Department
   ```

2. Restart the application or wait for configuration reload

3. Verify the association in the application logs

## Troubleshooting

- **"Notebook template not found"**: Ensure the notebook exists and has `is_template = true`
- **"Test section not found"**: Check the exact name in the `test_section` table
- **"Already exists"**: The association already exists and was skipped (normal behavior)
- **File checksum unchanged**: File content hasn't changed, so it's skipped (add a comment to force reprocessing)