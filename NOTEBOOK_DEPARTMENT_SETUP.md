# Complete Guide: Setting Up Notebook Departments

## Quick Start (TL;DR)

1. **Enable department selection at login:**
   - Edit `volume/properties/SystemConfiguration.properties`
   - Uncomment line 66: `REQUIRE_LAB_UNIT_AT_LOGIN=false` → `REQUIRE_LAB_UNIT_AT_LOGIN=true`

2. **Verify test sections exist:**
   - Check `volume/configuration/backend/test-sections/example-test-sections.csv`
   - Should include your departments (Bioanalytical Lab, Bioequivalence Lab, etc.)

3. **Link notebooks to departments:**
   - Edit `volume/configuration/backend/notebook-departments/research-lab-linkages.csv`
   - Add rows: `notebookTitle,departmentName`

4. **Create notebooks (if needed):**
   - Login as admin → Settings → Notebooks → Create new notebook
   - Title must match CSV exactly

5. **Assign users to departments:**
   - Login as admin → User Management → Select user → Test Sections tab → Check departments

6. **Restart & test:**
   - Restart application
   - Clear browser cache
   - Login with test user
   - Should see department selection screen

---

## The Problem (Why Notebooks Don't Show)

The department selection screen appears only when:

```
✓ REQUIRE_LAB_UNIT_AT_LOGIN = true
✓ Test sections exist in database
✓ Notebooks exist in database
✓ Notebook-department associations exist
✓ User is assigned to at least one test section
```

If **any** of these are missing, users won't see the department selection screen or notebooks.

---

## Complete Setup Workflow

### Phase 1: Configuration File Setup

#### Step 1.1: Enable Department Requirement

**File:** `volume/properties/SystemConfiguration.properties` (Line 66)

**Current (WRONG):**
```properties
# REQUIRE_LAB_UNIT_AT_LOGIN=false
```

**Change to (CORRECT):**
```properties
REQUIRE_LAB_UNIT_AT_LOGIN=true
```

**Effect:** After restart, users will be required to select a department when logging in.

#### Step 1.2: Verify/Add Test Sections

**File:** `volume/configuration/backend/test-sections/example-test-sections.csv`

Ensure all required test sections are listed:

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
Hematology,Hematology Department,Y,1,N,Hematology,Hématologie
Biochemistry,Biochemistry Department,Y,2,N,Biochemistry,Biochimie
Bioanalytical Laboratory,Bioanalytical Testing Laboratory Department,Y,13,N,Bioanalytical Laboratory,Laboratoire Bianalytique
Bioequivalence Laboratory,Bioequivalence Testing Laboratory Department,Y,14,N,Bioequivalence Laboratory,Laboratoire de Bioéquivalence
```

**Important:** The `testSectionName` must match exactly what you use in `notebook-departments/research-lab-linkages.csv`.

#### Step 1.3: Configure Notebook-Department Links

**File:** `volume/configuration/backend/notebook-departments/research-lab-linkages.csv`

Example for your research labs:

```csv
notebookTitle,departmentName
Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab
Genomics & Bioinformatics Laboratory,Genomics & Bioinformatics Laboratory
Bioanalytical Laboratory,Bioanalytical Laboratory
Bioequivalence Laboratory,Bioequivalence Laboratory
```

**Column meanings:**
- `notebookTitle`: The exact title of a notebook that already exists
- `departmentName`: The exact name of a test section that already exists

---

### Phase 2: Data Creation

#### Step 2.1: Create Notebooks (if they don't exist)

**Via UI:**
1. Login as an **administrator** (must have `ROLE_GLOBAL_ADMIN`)
2. Navigate to **Settings** → **Notebooks**
3. Click **Create New Notebook**
4. Enter title exactly as specified in your CSV (e.g., "Bioanalytical Laboratory")
5. Configure other settings as needed
6. Save

**Important:** The notebook title must **exactly match** the `notebookTitle` in your CSV file (case-sensitive).

#### Step 2.2: Verify Test Sections in Database

After restarting (from Phase 1), test sections should be in the database.

**SQL to verify:**
```sql
SELECT id, name_english, name, is_active, sort_order
FROM test_section
WHERE is_active = 'Y'
ORDER BY sort_order;
```

**Expected output:** Lists of test sections including your custom ones (Bioanalytical Laboratory, etc.)

If empty:
- Verify CSV file format is correct
- Check that application restarted successfully
- Look for any errors in application logs

---

### Phase 3: User Configuration

#### Step 3.1: Create a Test User (if needed)

**Via UI:**
1. Login as admin
2. Navigate to **User Management**
3. Click **Create New User**
4. Set username (e.g., "testuser")
5. Set password
6. Save

#### Step 3.2: Assign User to Test Sections

**Via UI (Recommended):**
1. Login as admin
2. Navigate to **User Management**
3. Find the test user
4. Click to open their profile
5. Go to **Test Sections** tab
6. Check the boxes for the test sections they should access:
   - Bioanalytical Laboratory
   - Bioequivalence Laboratory
   - Or others as needed
7. Save

**Via SQL (Alternative):**
```sql
-- Step 1: Get the user ID
SELECT id FROM system_user WHERE login_id = 'testuser';
-- Example result: 123

-- Step 2: Get the test section IDs
SELECT id, name_english FROM test_section
WHERE name_english IN ('Bioanalytical Laboratory', 'Bioequivalence Laboratory')
AND is_active = 'Y';
-- Example results:
-- id=45, name_english=Bioanalytical Laboratory
-- id=46, name_english=Bioequivalence Laboratory

-- Step 3: Create the associations
INSERT INTO user_test_section (user_id, test_section_id) VALUES (123, 45);
INSERT INTO user_test_section (user_id, test_section_id) VALUES (123, 46);
```

#### Step 3.3: Verify User Assignments

**SQL to check:**
```sql
SELECT
    u.login_id,
    ts.name_english,
    uts.user_id
FROM system_user u
LEFT JOIN user_test_section uts ON u.id = uts.user_id
LEFT JOIN test_section ts ON uts.test_section_id = ts.id
WHERE u.login_id = 'testuser'
ORDER BY ts.name_english;
```

**Expected output:** User is listed with their assigned test sections.

---

### Phase 4: Application Restart & Testing

#### Step 4.1: Restart Application

```bash
# Docker example
docker-compose restart openelis

# Or stop/start your application based on your setup
```

**Wait for:**
- Application to fully start
- Configuration to be loaded
- Database connections to be established

#### Step 4.2: Clear Browser Cache

Clear your browser cache to ensure you're not seeing old versions:

- **Chrome/Edge:** Ctrl+Shift+Delete (or Cmd+Shift+Delete on Mac)
- **Firefox:** Ctrl+Shift+Delete (or Cmd+Shift+Delete on Mac)
- **Safari:** Develop → Clear Caches

Or use private/incognito window for testing.

#### Step 4.3: Test the Flow

1. Navigate to the application login page
2. Login with the test user credentials
3. **Expected behavior:**
   - You should see the department selection screen (LandingPage)
   - A search box and department list should appear
   - You should be able to select a department
   - After selection, you should be able to access the application

4. **Navigate to Notebooks:**
   - Go to Sample Order or similar feature that uses notebooks
   - You should only see notebooks linked to your selected department

#### Step 4.4: Verify Database Associations

**SQL to check the complete setup:**
```sql
-- 1. Check notebook-department links
SELECT
    n.id as notebook_id,
    n.title as notebook_title,
    ts.id as test_section_id,
    ts.name_english as test_section_name
FROM notebook n
LEFT JOIN notebook_departments nd ON n.id = nd.notebook_id
LEFT JOIN test_section ts ON nd.department_id = ts.id
WHERE n.title LIKE '%Bioanalytical%' OR n.title LIKE '%Bioequivalence%';

-- 2. Check user test sections
SELECT
    u.login_id,
    STRING_AGG(ts.name_english, ', ') as test_sections
FROM system_user u
LEFT JOIN user_test_section uts ON u.id = uts.user_id
LEFT JOIN test_section ts ON uts.test_section_id = ts.id
WHERE u.login_id = 'testuser'
GROUP BY u.login_id;
```

---

## Troubleshooting

### Department Selection Screen Doesn't Appear

**Checklist:**
- [ ] `REQUIRE_LAB_UNIT_AT_LOGIN=true` in SystemConfiguration.properties
- [ ] Application has been restarted since changing the setting
- [ ] User is not a GLOBAL_ADMIN (admins bypass the landing page)
- [ ] User has at least one test section assigned

**Verify:**
```sql
-- Check configuration
SELECT config_value FROM system_configuration
WHERE config_key = 'REQUIRE_LAB_UNIT_AT_LOGIN';

-- Check user's test sections
SELECT COUNT(*) FROM user_test_section WHERE user_id = 123;
```

### Department List is Empty on Landing Page

**Checklist:**
- [ ] User has test sections assigned in the database
- [ ] Test sections are active (`is_active = 'Y'`)
- [ ] User session has been refreshed (log out and log back in)

**Verify:**
```sql
SELECT u.login_id, COUNT(uts.test_section_id) as section_count
FROM system_user u
LEFT JOIN user_test_section uts ON u.id = uts.user_id
WHERE u.login_id = 'testuser'
GROUP BY u.login_id;
```

### Notebooks Don't Appear After Selecting Department

**Checklist:**
- [ ] Notebooks exist in the database
- [ ] Notebooks are linked to the selected department via `notebook_departments` table
- [ ] Notebook titles in CSV match actual notebook titles in database (case-sensitive)
- [ ] Department names in CSV match actual test section names (case-sensitive)

**Verify:**
```sql
-- Check if associations were created
SELECT n.title, ts.name_english
FROM notebook n
JOIN notebook_departments nd ON n.id = nd.notebook_id
JOIN test_section ts ON nd.department_id = ts.id
WHERE ts.id = 45;  -- Use the actual test section ID of your selected department
```

### Configuration Changes Don't Take Effect

**Solution:**
1. Verify CSV file format is correct (no extra blank lines, proper headers)
2. Stop the application completely
3. Wait 5-10 seconds
4. Restart the application
5. Wait for startup to complete (check logs)
6. Clear browser cache
7. Test again

---

## Reference Architecture

```
User Login
    ↓
REQUIRE_LAB_UNIT_AT_LOGIN check
    ↓ (if true)
Landing Page (LandingPage.tsx)
    ↓
Fetch user's test sections (/rest/user-test-sections/ALL)
    ↓
User selects department
    ↓
POST /rest/setUserLoginLabUnit/{labUnitId}
    ↓
UserSessionData.loginLabUnit = selected_id
    ↓
User can now access application
    ↓
Notebook endpoints filter by loginLabUnit
    ↓
Only notebooks linked to that department are shown
```

## Configuration Files

| File | Purpose | When to Edit |
|------|---------|--------------|
| `volume/properties/SystemConfiguration.properties` | Enable/disable department requirement | Once during setup |
| `volume/configuration/backend/test-sections/example-test-sections.csv` | Define departments/labs | When adding new departments |
| `volume/configuration/backend/notebook-departments/research-lab-linkages.csv` | Link notebooks to departments | When adding new notebooks |

## Related Documentation

- **Debugging Guide:** `NOTEBOOK_DEPARTMENT_DEBUG.md`
- **Notebook-Departments Config:** `volume/configuration/backend/notebook-departments/README.md`
- **Test-Sections Config:** `volume/configuration/backend/test-sections/README.md`

## Key Points

1. **Enable the feature first:** Set `REQUIRE_LAB_UNIT_AT_LOGIN=true`
2. **Then create data:** Notebooks and test sections must exist before linking
3. **Then link them:** Use CSV or UI to connect notebooks to departments
4. **Then assign users:** Users must be assigned to test sections to see them
5. **Then restart:** Application restart loads configuration changes
6. **Then test:** Verify each step before moving to the next

## Support

For issues or questions:
1. Check `NOTEBOOK_DEPARTMENT_DEBUG.md` for your specific problem
2. Use the SQL queries provided to verify your setup
3. Check application logs for configuration loading errors
4. Verify all files are in the correct locations and have correct formatting
