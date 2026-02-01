# Notebook Department Configuration - Debugging Guide

## Problem Statement

Notebooks created via configuration are not appearing in the department selection screen at login, and users don't see them when checking notebooks.

## Root Causes Checklist

Use this checklist to identify which step is failing:

### 1. **REQUIRE_LAB_UNIT_AT_LOGIN Configuration**

The department selection screen **ONLY appears** if `REQUIRE_LAB_UNIT_AT_LOGIN` is set to `true`.

**Where to check:**
- Backend configuration file (likely in `application.properties` or similar)
- Run this query to verify:
```sql
SELECT * FROM system_configuration WHERE config_key = 'REQUIRE_LAB_UNIT_AT_LOGIN';
```

**Expected result:** Value should be `true`

**If not set or false:**
- Update configuration: `REQUIRE_LAB_UNIT_AT_LOGIN=true`
- Restart the application
- Clear browser cache before logging in again

---

### 2. **Test Sections (Departments) Must Exist**

Notebooks are linked to Test Sections, which represent departments.

**Where to check:**
```sql
SELECT id, name, name_english FROM test_section WHERE is_active = 'Y' ORDER BY sort_order;
```

**Expected results:** You should see entries like:
- Bioanalytical Laboratory
- Bioequivalence Laboratory
- Genomics & Bioinformatics Laboratory
- Traditional & Modern Medicine Research Lab

**If missing:**
- The configuration file `/volume/configuration/backend/test-sections/example-test-sections.csv` has not been loaded
- The application may need to be restarted after adding configuration files
- Verify the CSV file exists and has correct formatting (no trailing newlines, proper headers)

**Note:** These are created from configuration file:
- Path: `volume/configuration/backend/test-sections/example-test-sections.csv`
- Format: `testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName`

---

### 3. **Notebooks Must Exist in the Database**

Notebooks can be created either:
- Via UI (Admin > Notebooks)
- Via configuration files (future feature)

**Where to check:**
```sql
SELECT id, title FROM notebook WHERE is_active = 'Y' ORDER BY id;
```

**If no notebooks exist:**
- Create them manually via the UI:
  1. Login as admin
  2. Go to Settings → Notebooks
  3. Create a notebook with title (e.g., "Traditional & Modern Medicine Research Lab")
  4. Save it

---

### 4. **Notebook-Department Associations Must Be Created**

Once notebooks and test sections exist, they must be linked together.

**Where to check:**
```sql
SELECT
    n.id as notebook_id,
    n.title as notebook_title,
    ts.id as test_section_id,
    ts.name_english as test_section_name
FROM notebook n
LEFT JOIN notebook_departments nd ON n.id = nd.notebook_id
LEFT JOIN test_section ts ON nd.department_id = ts.id
ORDER BY n.title;
```

**If associations are missing:**

**Option A: Via Configuration (Recommended)**
1. Edit `volume/configuration/backend/notebook-departments/research-lab-linkages.csv`
2. Format: `notebookTitle,departmentName`
3. Example:
   ```csv
   notebookTitle,departmentName
   Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab
   Bioanalytical Laboratory,Bioanalytical Laboratory
   ```
4. Restart application
5. Verify with SQL query above

**Option B: Via UI/API**
1. Login as admin
2. Navigate to each notebook
3. Assign departments/test sections in notebook settings
4. Save

---

### 5. **Users Must Be Assigned to Test Sections**

Users can only see notebooks for test sections they are assigned to.

**Where to check:**
```sql
SELECT
    u.id,
    u.login_id,
    u.last_name,
    ts.name_english
FROM system_user u
LEFT JOIN user_test_section uts ON u.id = uts.user_id
LEFT JOIN test_section ts ON uts.test_section_id = ts.id
WHERE u.is_active = 'Y'
ORDER BY u.login_id;
```

**If user has no test sections assigned:**

**Option A: Via UI (Recommended)**
1. Login as admin
2. Go to Users management
3. Find the test user
4. In "Test Sections" tab, check the boxes for:
   - Bioanalytical Laboratory
   - Bioequivalence Laboratory
   - Or other departments as needed
5. Save

**Option B: Via SQL (Direct insertion - use with caution)**
```sql
-- First, get the user ID and test section ID
SELECT id FROM system_user WHERE login_id = 'your_test_user';
SELECT id FROM test_section WHERE name = 'Bioanalytical Laboratory';

-- Then insert the association
INSERT INTO user_test_section (user_id, test_section_id) VALUES (USER_ID, TEST_SECTION_ID);
```

---

### 6. **Application Cache Must Be Cleared**

Configuration changes take effect on:
- Application restart
- Configuration reload (if implemented)

**What to do:**
1. Stop the application
2. Restart the application
3. Clear browser cache (Ctrl+Shift+Delete or Cmd+Shift+Delete)
4. Log out and log back in

---

## Complete Setup Workflow

Follow these steps in order to set up notebooks for a new test user:

### Step 1: Ensure Configuration is Loaded
```bash
# Verify test-sections exist in CSV
cat volume/configuration/backend/test-sections/example-test-sections.csv

# Verify notebook-departments CSV exists
cat volume/configuration/backend/notebook-departments/research-lab-linkages.csv

# Restart application if files were added
docker-compose restart openelis  # or your restart command
```

### Step 2: Verify Test Sections in Database
```sql
SELECT id, name FROM test_section WHERE is_active = 'Y' LIMIT 5;
```

### Step 3: Create Notebooks (if needed)
- Login as admin
- Navigate to Settings → Notebooks
- Create notebooks for each test section

### Step 4: Link Notebooks to Test Sections
- Edit `volume/configuration/backend/notebook-departments/research-lab-linkages.csv`
- Or use the UI to manually assign

### Step 5: Create/Configure Test User
```sql
-- Verify user exists
SELECT id FROM system_user WHERE login_id = 'testuser';

-- Add user to test sections
INSERT INTO user_test_section (user_id, test_section_id)
SELECT u.id, ts.id
FROM system_user u, test_section ts
WHERE u.login_id = 'testuser'
AND ts.name = 'Bioanalytical Laboratory';
```

### Step 6: Restart & Test
1. Restart application
2. Clear browser cache
3. Login as the test user
4. Should see department selection screen
5. Select a department
6. Should see notebooks for that department

---

## Query to Check Complete Setup Status

Run this query to see the complete status of notebooks, test sections, users, and their associations:

```sql
-- Complete Notebook-Department-User Status
SELECT
    'TEST SECTIONS' as category,
    COUNT(*) as count
FROM test_section ts
WHERE ts.is_active = 'Y'

UNION ALL

SELECT
    'NOTEBOOKS',
    COUNT(*)
FROM notebook n
WHERE n.is_active = 'Y'

UNION ALL

SELECT
    'NOTEBOOK-DEPT LINKS',
    COUNT(*)
FROM notebook_departments

UNION ALL

SELECT
    'USERS WITH TEST SECTIONS',
    COUNT(DISTINCT u.id)
FROM system_user u
JOIN user_test_section uts ON u.id = uts.user_id
WHERE u.is_active = 'Y';
```

---

## Common Issues and Solutions

### Issue: Department selection screen doesn't appear after login
**Solution:**
1. Check `REQUIRE_LAB_UNIT_AT_LOGIN=true` is set
2. Restart application
3. Clear browser cache
4. Verify user has at least one test section assigned

### Issue: User logs in, sees department selection, but it's empty
**Solution:**
1. Check: `SELECT id FROM test_section WHERE is_active = 'Y';`
2. If empty, verify configuration files are loaded
3. Check: `SELECT id FROM user_test_section WHERE user_id = ?;`
4. If empty, assign user to test sections (see Step 5 in workflow)

### Issue: User selects department, but sees no notebooks
**Solution:**
1. Check notebooks exist: `SELECT * FROM notebook WHERE is_active = 'Y';`
2. Check associations exist: `SELECT * FROM notebook_departments;`
3. Verify associations match selected test section ID
4. If using configuration, edit `research-lab-linkages.csv` and restart

### Issue: Configuration files added but changes don't appear
**Solution:**
1. Application needs to be restarted to load new configuration
2. After adding/modifying CSV files:
   - Stop application
   - Restart application
   - Wait for startup to complete
   - Clear browser cache
   - Test again

---

## Files Referenced

- Backend configuration: `volume/configuration/backend/test-sections/example-test-sections.csv`
- Notebook-department mapping: `volume/configuration/backend/notebook-departments/research-lab-linkages.csv`
- Configuration property: `application.properties` (look for `REQUIRE_LAB_UNIT_AT_LOGIN`)

## Key Code Locations

- Department selection UI: `frontend/src/components/home/LandingPage.tsx`
- Notebook access control: `src/main/java/org/openelisglobal/notebook/service/NotebookSecurityServiceImpl.java`
- Configuration handler: `src/main/java/org/openelisglobal/notebook/service/NotebookDepartmentConfigurationHandler.java`
