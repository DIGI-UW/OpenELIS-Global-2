# Understanding Lab Unit, Department, Test Section, and Their Differences

## Quick Answer

| Concept | What It Is | Example | Purpose |
|---------|-----------|---------|---------|
| **Test Section** | Database entity with ID | "Bioanalytical Laboratory" (ID: 45) | Organizational unit for sample routing |
| **Department** | Set of TestSections | Notebooks linked to TestSections | Notebook access control |
| **Lab Unit** | String identifier | "Bioanalytical" | Role-based access control (RBAC) |

---

## Detailed Explanation

### 1. **TestSection** (Called "Department" in some contexts)

**What it is:**
- A concrete database entity representing an organizational unit/lab
- Located in `test_section` table with ID, name, description, etc.
- Configured via CSV: `volume/configuration/backend/test-sections/example-test-sections.csv`

**Example:**
```
testSectionName: "Bioanalytical Laboratory"
Database ID: 45
Description: "Bioanalytical Testing Laboratory Department"
Is Active: Y
Sort Order: 13
```

**Used for:**
- ✅ Organizing samples by department
- ✅ Linking notebooks to departments
- ✅ Assigning users to departments
- ✅ Sample type filtering by department
- ✅ Department selection at login

**Database Table:**
```sql
test_section
  - ID (PK): 45
  - name: "Bioanalytical Laboratory"
  - description: "Bioanalytical Testing Laboratory Department"
  - is_active: 'Y'
  - sort_order: 13
  - parent_test_section: NULL (no hierarchy)
```

---

### 2. **Lab Unit** (RBAC Concept)

**What it is:**
- A **STRING IDENTIFIER** used for role-based access control
- NOT a database entity, just a value in the `lab_unit_role_map` table
- Maps users to roles within a specific lab/unit

**Types of Lab Units:**

#### a) Special Lab Unit: "AllLabUnits"
- Built-in value
- Grants access to all departments
- Used for global role permissions
- Example: A global admin role might be assigned to "AllLabUnits"

#### b) Specific Lab Unit: Named Lab Units
- Created for specific departments (e.g., "Bioanalytical", "Bioequivalence")
- Maps to test sections by association
- Has specific roles assigned to it

**Examples Created in tobermvd:**
```
Lab Unit: "Bioanalytical"
  - Roles: Bioanalytical Sample Receiver, Bioanalytical Chemical Analyst, etc.

Lab Unit: "Bioequivalence"
  - Roles: Bioequivalence Sample Receiver, Bioequivalence Chemical Analyst, etc.

Lab Unit: "AllLabUnits"
  - Roles: All system roles (for global access)
```

**Used for:**
- ✅ Defining which roles are available in each lab/unit
- ✅ Restricting user access to specific lab departments
- ✅ Enforcing role-based permissions

**Database Tables:**
```sql
lab_unit_role_map
  - lab_unit_role_map_id: 1
  - lab_unit: "Bioanalytical"  -- STRING VALUE

lab_roles
  - lab_unit_role_map_id: 1
  - role: 74  -- ID of "Bioanalytical Chemical Analyst" role
```

---

### 3. **Department** (In Notebook Context)

**What it is:**
- A **relationship** between notebooks and test sections
- NOT a new entity, just a collection of TestSection objects
- Configured via CSV: `volume/configuration/backend/notebook-departments/research-lab-linkages.csv`

**Example:**
```csv
notebookTitle,departmentName
Bioanalytical Laboratory,Bioanalytical Laboratory
```

This means:
- Notebook with title "Bioanalytical Laboratory" is associated with
- TestSection with name "Bioanalytical Laboratory"

**Used for:**
- ✅ Filtering notebooks shown to users
- ✅ Ensuring users only see notebooks for their assigned departments

**Database Table:**
```sql
notebook_departments
  - notebook_id: 12
  - test_section_id: 45  -- The TestSection ID
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        SYSTEM USER                          │
│                     (system_user table)                     │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ├─── 1. USER ROLE ASSIGNMENT
                 │    (user_lab_unit_roles + lab_unit_roles)
                 │
                 └──────────────────────────────┐
                                                │
                    ┌───────────────────────────▼─────────────┐
                    │      Lab Unit Role Map                  │
                    │  (Lab Unit RBAC Framework)              │
                    ├─────────────────────────────────────────┤
                    │ Lab Unit: "AllLabUnits"                 │
                    │  └─ Roles: Global Admin, etc.           │
                    │                                         │
                    │ Lab Unit: "Bioanalytical"               │
                    │  └─ Roles: BioAnal Technician, etc.     │
                    │                                         │
                    │ Lab Unit: "Bioequivalence"              │
                    │  └─ Roles: BioEq Technician, etc.       │
                    └───────────────────────────┬─────────────┘
                                                │
                ┌───────────────────────────────┴──────────────┐
                │                                              │
                │    2. USER TEST SECTION ASSIGNMENT           │
                │       (user_test_section table)             │
                │                                              │
                └────────────────┬─────────────────────────────┘
                                 │
           ┌─────────────────────┴────────────────────┐
           │                                          │
    ┌──────▼──────────────┐              ┌───────────▼────────┐
    │  TestSection        │              │   TestSection      │
    │ (test_section tbl)  │              │  (test_section tbl)│
    ├─────────────────────┤              ├────────────────────┤
    │ ID: 45              │              │ ID: 46             │
    │ Name: Bioanalytical │              │ Name: Bioequivalence
    │ Active: Y           │              │ Active: Y          │
    │ Sort: 13            │              │ Sort: 14           │
    └──────┬──────────────┘              └────────┬───────────┘
           │                                      │
           │ 3. NOTEBOOK-DEPARTMENT ASSOC        │
           │  (notebook_departments tbl)         │
           │                                      │
    ┌──────▼──────────────────────────────────────▼──────────┐
    │         NoteBook Templates                            │
    │     (notebook table with is_template=true)           │
    ├──────────────────────────────────────────────────────┤
    │ Title: "Bioanalytical Laboratory"                    │
    │  └─ departments: {TestSection 45}                    │
    │                                                       │
    │ Title: "Bioequivalence Laboratory"                   │
    │  └─ departments: {TestSection 46}                    │
    │                                                       │
    │ Title: "Shared Lab Notebook"                         │
    │  └─ departments: {TestSection 45, 46}  (in both!)   │
    └──────────────────────────────────────────────────────┘
```

---

## Real-World Example

### Setup:

**1. Create TestSections (Departments):**
```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
Bioanalytical Laboratory,Testing Lab,Y,13,N,Bioanalytical Laboratory,Laboratoire Bianalytique
Bioequivalence Laboratory,Testing Lab,Y,14,N,Bioequivalence Laboratory,Laboratoire Bioéquivalence
```

**2. Create Lab Unit RBAC (Liquibase changesets):**
- Create "Bioanalytical" lab unit
- Create "Bioequivalence" lab unit
- Assign specific roles to each

**3. Create Notebooks:**
```csv
notebookTitle,departmentName
Bioanalytical Laboratory,Bioanalytical Laboratory
Bioequivalence Laboratory,Bioequivalence Laboratory
```

**4. Create Users and Assign:**
- User: "john_lab"
  - Test Sections: Bioanalytical Laboratory (ID: 45)
  - Lab Unit Roles: "Bioanalytical" + "Sample Receiver" role

- User: "jane_lab"
  - Test Sections: Bioequivalence Laboratory (ID: 46)
  - Lab Unit Roles: "Bioequivalence" + "Technician" role

### Result:

When "john_lab" logs in:
1. ✅ Sees "Bioanalytical Laboratory" in department selection
2. ✅ Selects it
3. ✅ Sees only "Bioanalytical Laboratory" notebook
4. ✅ Can access it with "Sample Receiver" role permissions

When "jane_lab" logs in:
1. ✅ Sees "Bioequivalence Laboratory" in department selection
2. ✅ Selects it
3. ✅ Sees only "Bioequivalence Laboratory" notebook
4. ✅ Can access it with "Technician" role permissions

---

## Are The Lab Unit Changesets Necessary?

### YES - For RBAC to Work Properly

**The lab unit changesets (072, 073, 075, 076) are necessary because:**

1. **They establish the RBAC framework** - Define which roles exist for each lab
   - Without them, users wouldn't have role assignments to specific labs
   - System wouldn't know which roles are appropriate for "Bioanalytical" lab

2. **They enable role-based access control** - Restrict what users can do in each lab
   - A "Bioanalytical Technician" role only makes sense in the "Bioanalytical" lab unit
   - System enforces that this role can only be assigned to users in that lab unit

3. **They support global role assignment** (AllLabUnits)
   - Some roles (like global admin) are assigned to "AllLabUnits"
   - This allows certain users to work across all departments

### But There IS Potential Overlap

**Potential Concern:**
- TestSection already represents the department
- Lab Unit is another concept representing the same logical unit
- Could be seen as duplication

**Why Both Exist:**
- **TestSection** = Organizational/operational structure (sample routing, storage)
- **Lab Unit** = Access control structure (role permissions, user restrictions)
- They serve different purposes but happen to have the same names

---

## Current Status in tobermvd

### What's in Your Branch:

| Changeset | Purpose | Necessary? | Notes |
|-----------|---------|-----------|-------|
| 072-create-bioanalytical-lab-unit.xml | Create "Bioanalytical" lab unit + roles | ✅ YES | Defines RBAC for Bioanalytical lab |
| 073-add-bioanalytical-roles-to-alllabunits.xml | Add roles to global access | ✅ YES | Allows global role access |
| 074-bioequivalence-system-roles.xml | Create bioequivalence roles | ✅ YES | Prerequisite for lab unit creation |
| 075-create-bioequivalence-lab-unit.xml | Create "Bioequivalence" lab unit + roles | ✅ YES | Defines RBAC for Bioequivalence lab |
| 076-add-bioequivalence-roles-to-alllabunits.xml | Add roles to global access | ✅ YES | Allows global role access |

---

## Summary

```
┌─────────────────────────────────────────────────────────────┐
│ USE TEST SECTION FOR:                                       │
│ - Organizing samples by lab/department                      │
│ - Assigning users to departments                            │
│ - Filtering sample types per department                     │
│ - Linking notebooks to departments (via CSV)                │
│ - Department selection at login                             │
│ - Configurable via CSV ✅                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ USE LAB UNIT FOR:                                           │
│ - Role-based access control (RBAC)                          │
│ - Restricting user permissions by lab                       │
│ - Managing which roles are available in each lab            │
│ - Supporting global role access (AllLabUnits)               │
│ - Must be created via Liquibase (schema enforcement)        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ DEPARTMENT (in Notebook context):                           │
│ - Just a collection of TestSections                         │
│ - Configured via CSV (notebook-departments)                 │
│ - No separate entity, just relationships                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Recommendation

**Keep all lab unit changesets (072, 073, 074, 075, 076)** - They are essential for:
- RBAC enforcement
- Role-based notebook access
- Department-specific permissions
- User role assignment at the lab unit level

The only potential optimization would be to make lab unit creation configurable (like test sections), but that's a larger refactor and beyond the scope of the current cleanup.
