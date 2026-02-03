# Notebook Departments Configuration

## Overview

This configuration directory allows you to define which **notebooks** (data collection forms) are available in which **test sections** (departments/labs).

**File:** `research-lab-linkages.csv`

## Format

```csv
notebookTitle,departmentName
```

- **notebookTitle**: The exact title of the notebook as it appears in the system
- **departmentName**: The exact name of the test section/department as it appears in the system

## Example

```csv
notebookTitle,departmentName
Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab
Genomics & Bioinformatics Laboratory,Genomics & Bioinformatics Laboratory
Bioanalytical Laboratory,Bioanalytical Laboratory
Bioequivalence Laboratory,Bioequivalence Laboratory
```

## How It Works

1. When the application starts, it reads this CSV file
2. For each line, it finds the notebook with matching title
3. It finds the test section with matching name
4. It creates an association between them
5. Users assigned to that test section can now see and access the notebook

## Prerequisites

Before you can link a notebook to a test section, **both must already exist** in the system:

### 1. Test Sections Must Exist

Test sections are configured in:
- **File:** `../test-sections/example-test-sections.csv`
- **Or:** Created manually via Admin UI → Settings → Test Sections

**Available test sections in this example:**
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

### 2. Notebooks Must Exist

Notebooks can be created:
- Via Admin UI → Settings → Notebooks
- Or via configuration files (custom implementation required)

You must create the notebook **before** you can link it to a test section in this CSV.

## Setup Steps

### Step 1: Create Test Sections (if needed)

If test sections don't already exist, add them to `../test-sections/example-test-sections.csv`:

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
Bioanalytical Laboratory,Bioanalytical Testing Lab,Y,1,N,Bioanalytical Laboratory,Laboratoire Bianalytique
```

### Step 2: Create Notebooks

1. Login as an administrator
2. Navigate to **Settings** → **Notebooks**
3. Click **Create New Notebook**
4. Enter the notebook title (e.g., "Bioanalytical Laboratory")
5. Save the notebook

**Important:** The notebook title must **exactly match** the `notebookTitle` in your CSV

### Step 3: Configure Notebook-Department Links

Edit this file (`research-lab-linkages.csv`):

```csv
notebookTitle,departmentName
Bioanalytical Laboratory,Bioanalytical Laboratory
```

### Step 4: Restart the Application

The configuration is loaded when the application starts:

```bash
# Docker example
docker-compose restart openelis

# Or stop/start your application
```

### Step 5: Assign Users to Test Sections

Users only see notebooks for the test sections they are assigned to.

**Via Admin UI:**
1. Navigate to **User Management**
2. Click on a user
3. Go to the **Test Sections** tab
4. Check the test sections to assign
5. Save

**Via SQL:**
```sql
INSERT INTO user_test_section (user_id, test_section_id)
SELECT u.id, ts.id
FROM system_user u, test_section ts
WHERE u.login_id = 'username'
AND ts.name = 'Bioanalytical Laboratory';
```

### Step 6: Enable Department Selection at Login

For users to be required to select a department when logging in, ensure this configuration is set:

```
REQUIRE_LAB_UNIT_AT_LOGIN=true
```

This is typically set in `application.properties` or environment variables.

## Troubleshooting

### Notebooks Don't Appear in Department Selection

**Checklist:**
- [ ] Notebook title in CSV **exactly matches** notebook title in system (case-sensitive)
- [ ] Test section name in CSV **exactly matches** test section name in system
- [ ] Application has been restarted after changing this CSV
- [ ] User is assigned to the test section
- [ ] `REQUIRE_LAB_UNIT_AT_LOGIN=true` is configured

**Verify in database:**
```sql
-- Check if associations were created
SELECT n.title, ts.name_english
FROM notebook n
LEFT JOIN notebook_departments nd ON n.id = nd.notebook_id
LEFT JOIN test_section ts ON nd.department_id = ts.id
WHERE n.title LIKE '%Bioanalytical%';
```

### User Sees Empty Department List

The user's assigned test sections are not showing up in the department selector.

**Checklist:**
- [ ] User is assigned to at least one test section in the database
- [ ] That test section is active (`is_active = 'Y'`)

**Verify in database:**
```sql
SELECT u.login_id, ts.name_english
FROM system_user u
LEFT JOIN user_test_section uts ON u.id = uts.user_id
LEFT JOIN test_section ts ON uts.test_section_id = ts.id
WHERE u.login_id = 'username';
```

### Configuration Changes Not Taking Effect

**Solution:**
1. Stop the application
2. Verify the CSV file has correct format (no extra blank lines, proper headers)
3. Restart the application
4. Clear browser cache
5. Log out and log back in

## Multiple Departments for One Notebook

A single notebook can be linked to multiple test sections:

```csv
notebookTitle,departmentName
Bioanalytical Laboratory,Bioanalytical Laboratory
Bioanalytical Laboratory,Bioequivalence Laboratory
```

This makes the notebook available in both departments.

## Matching Rules

**Important:** The matching is done by comparing against the **name** column of notebooks and test sections.

- For **test sections**: Match against the configured `name` (from configuration)
- For **notebooks**: Match against the notebook's `title` field

The matching is **case-sensitive**. Ensure names match exactly.

## Related Configuration

- **Test Sections:** `../test-sections/example-test-sections.csv`
- **Tests Assignment to Test Sections:** `../tests/` (if configured)
- **Configuration Property:** `REQUIRE_LAB_UNIT_AT_LOGIN` (in application properties)

## Reference

This configuration is processed by the **NotebookDepartmentConfigurationHandler** class which:
- Loads on application startup
- Processes each line in the CSV
- Creates associations between notebooks and test sections
- Updates the `notebook_departments` table in the database

See the debugging guide: `NOTEBOOK_DEPARTMENT_DEBUG.md` for SQL queries to verify your setup.
