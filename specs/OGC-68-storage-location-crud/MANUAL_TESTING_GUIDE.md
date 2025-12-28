# Manual Testing Guide - M2 Frontend Remediation

**Date**: 2025-12-16  
**Branch**: `feat/001-ogc-68-sample-storage-m2-frontend`  
**Purpose**: Manual verification of all CRITICAL and HIGH issues fixed

## Prerequisites

1. **Application URL**: https://localhost/ (or
   http://localhost:8080/OpenELIS-Global/)
2. **Login Credentials**:
   - Username: `admin`
   - Password: `adminADMIN!`
3. **Browser**: Chrome/Firefox with developer tools enabled

## Testing Checklist

### ✅ C1: Code Update Blocked by Backend

**Test**: Verify code field is editable for all location types

1. Navigate to **Storage Dashboard** → **Rooms** tab
2. Click **Add Room** button
3. Fill in Name and Code fields
4. Save successfully
5. Click **Edit** (overflow menu) on the created room
6. **Verify**: Code field is editable (not disabled/read-only)
7. Change code value and save
8. **Verify**: Code update persists in table

**Repeat for**: Device, Shelf, Rack tabs

**Expected**: Code field editable and updates successfully

---

### ✅ C2: Active Toggle Non-Functional (Room)

**Test**: Verify active toggle persists changes

1. Navigate to **Storage Dashboard** → **Rooms** tab
2. Click **Edit** on any room
3. Toggle **Active** switch (ON → OFF or OFF → ON)
4. Click **Save**
5. **Verify**: Modal closes
6. **Verify**: Table shows updated active status
7. Refresh page
8. **Verify**: Active status persists after refresh

**Expected**: Active toggle changes persist correctly

---

### ✅ C3: Box/Plate CRUD Missing in UI

**Test**: Verify Box CRUD functionality integrated into Boxes tab

#### Test 3.1: Add Box Button

1. Navigate to **Storage Dashboard** → **Boxes** tab
2. **Verify**: "Add Box/Plate" button is **disabled** (no rack selected)
3. Select a **Rack** from the dropdown
4. **Verify**: "Add Box/Plate" button becomes **enabled**
5. Click **Add Box/Plate** button
6. Fill in form:
   - Label: `Test Box 1`
   - Code: `TB001`
   - Type: `Box`
   - Rows: `8`
   - Columns: `12`
   - Position Schema: `Letter-Number (A1)`
   - Active: `ON`
7. Click **Save**
8. **Verify**: Modal closes, notification appears
9. **Verify**: Box appears in box selector dropdown

#### Test 3.2: Edit Box

1. In **Boxes** tab, select a rack
2. Select a box from **Box** dropdown
3. **Verify**: Overflow menu (⋮) appears next to box selector
4. Click overflow menu → **Edit**
5. Change Label to `Updated Box Label`
6. Change Code to `TB002`
7. Click **Save**
8. **Verify**: Changes persist in box selector

#### Test 3.3: Delete Box

1. Select a box that has **no samples** stored
2. Click overflow menu → **Delete**
3. **Verify**: Delete modal opens
4. **Verify**: "Delete" button is enabled (canDelete: true)
5. Click **Delete**
6. **Verify**: Box removed from dropdown

#### Test 3.4: Delete Box with Constraints

1. Select a box that has **samples stored** (if available)
2. Click overflow menu → **Delete**
3. **Verify**: Delete modal shows error message
4. **Verify**: "Delete" button is **disabled**
5. **Verify**: Error message explains constraint (e.g., "Cannot delete box:
   contains 3 stored samples")

#### Test 3.5: Grid Assignment Workflow Intact

1. In **Boxes** tab, select a rack
2. Select a box
3. **Verify**: Grid view appears (position grid for sample assignment)
4. **Verify**: Grid assignment workflow still works (select position → assign
   sample)

**Expected**: All Box CRUD operations work without breaking grid assignment

---

### ✅ H1: Active Toggle Frozen (Device/Shelf/Rack)

**Test**: Verify active toggle works for Device, Shelf, Rack

1. Navigate to **Storage Dashboard** → **Devices** tab
2. Click **Edit** on any device
3. Toggle **Active** switch
4. Click **Save**
5. **Verify**: Active status updates in table

**Repeat for**: Shelves tab, Racks tab

**Expected**: Active toggle works for all location types

---

### ✅ H2: Rack Create/Edit Failing

**Test**: Verify rack creation and editing works

#### Test 2.1: Create Rack

1. Navigate to **Storage Dashboard** → **Racks** tab
2. Click **Add Rack** button
3. Fill in form:
   - Label: `Test Rack 1`
   - Code: `RK001`
   - Parent Shelf: (select from dropdown)
   - Rows: `8`
   - Columns: `12`
   - Position Schema: `Letter-Number (A1)`
   - Active: `ON`
4. Click **Save**
5. **Verify**: Rack appears in table

#### Test 2.2: Edit Rack

1. Click **Edit** on created rack
2. Change Label to `Updated Rack`
3. Change Code to `RK002`
4. Click **Save**
5. **Verify**: Changes persist in table

**Expected**: Rack create and edit work correctly

---

### ✅ H3: Form Field Order Inconsistent

**Test**: Verify consistent field order across all location types

1. Navigate to **Storage Dashboard** → **Rooms** tab
2. Click **Add Room**
3. **Verify**: Field order is: Name, Code, Description, Active
4. Close modal

5. Navigate to **Devices** tab
6. Click **Add Device**
7. **Verify**: Field order is: Name, Code, Parent Room (read-only), Type,
   Temperature, Capacity, Active
8. Close modal

9. Navigate to **Shelves** tab
10. Click **Add Shelf**
11. **Verify**: Field order is: Label, Code, Parent Device (read-only),
    Capacity, Active
12. Close modal

13. Navigate to **Racks** tab
14. Click **Add Rack**
15. **Verify**: Field order is: Label, Code, Parent Shelf (read-only), Rows,
    Columns, Position Schema, Active

**Expected**: Consistent field order (Name/Code first, Parent read-only, Active
last)

---

### ✅ H4: Device Temperature Validation Not Field-Specific

**Test**: Verify temperature field shows inline validation error

1. Navigate to **Storage Dashboard** → **Devices** tab
2. Click **Add Device** or **Edit** existing device
3. In **Temperature Setting** field, enter non-numeric value: `abc`
4. **Verify**: Red error message appears below field: "Please enter a valid
   number"
5. **Verify**: Field shows invalid state (red border)
6. Clear field or enter valid number: `-80`
7. **Verify**: Error message disappears
8. **Verify**: Field shows valid state

**Expected**: Field-specific validation error appears inline

---

### ✅ H5: Header Shows "Urine" Instead of "OpenELIS"

**Test**: Verify header shows correct site name

1. Navigate to application root: https://localhost/
2. **Verify**: Header shows "OpenELIS Global" (or configured site name)
3. **Note**: If still shows "Urine", this is a database configuration issue:
   - Access **Admin** → **Site Information**
   - Update "Banner Text" field
   - Or run SQL:
     `UPDATE site_information SET value = 'OpenELIS Global' WHERE name = 'banner text';`

**Expected**: Header shows correct site name

---

## Verification Checklist (CHK059-CHK068)

After completing all tests above, verify:

- [ ] CHK059: All location types (Room, Device, Shelf, Rack, Box) can be created
- [ ] CHK060: All location types can be edited (name, code, active,
      type-specific fields)
- [ ] CHK061: All location types can be deleted (with constraint checks)
- [ ] CHK062: Active toggle works for all location types
- [ ] CHK063: Code field updates correctly for all location types
- [ ] CHK064: Form field order is consistent across all location types
- [ ] CHK065: Validation errors show inline on specific fields
- [ ] CHK066: Header shows correct site name
- [ ] CHK067: All existing E2E tests pass (run:
      `npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"`)
- [ ] CHK068: New E2E tests for fixed issues pass

---

## Monitoring Container Status

```bash
# Check container status
docker ps --filter "name=openelisglobal-webapp"

# View logs (real-time)
docker logs -f openelisglobal-webapp

# View last 50 lines
docker logs --tail 50 openelisglobal-webapp

# Restart container if needed
docker compose -f dev.docker-compose.yml restart oe.openelis.org
```

---

## Troubleshooting

### Application Not Starting

1. Check logs: `docker logs openelisglobal-webapp`
2. Verify database is running:
   `docker ps --filter "name=openelisglobal-database"`
3. Check WAR file exists: `ls -lh target/OpenELIS-Global.war`

### Changes Not Reflecting

1. Rebuild WAR: `mvn clean install -DskipTests -Dmaven.test.skip=true`
2. Force recreate container:
   `docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org`

### Frontend Not Updating

1. Check frontend container:
   `docker ps --filter "name=openelisglobal-front-end"`
2. Restart frontend:
   `docker compose -f dev.docker-compose.yml restart frontend.openelis.org`

---

## Test Results Template

```
Date: ___________
Tester: ___________

C1 - Code Update: [ ] PASS [ ] FAIL - Notes: ___________
C2 - Active Toggle (Room): [ ] PASS [ ] FAIL - Notes: ___________
C3 - Box CRUD: [ ] PASS [ ] FAIL - Notes: ___________
H1 - Active Toggle (Other): [ ] PASS [ ] FAIL - Notes: ___________
H2 - Rack Create/Edit: [ ] PASS [ ] FAIL - Notes: ___________
H3 - Form Field Order: [ ] PASS [ ] FAIL - Notes: ___________
H4 - Temperature Validation: [ ] PASS [ ] FAIL - Notes: ___________
H5 - Header Text: [ ] PASS [ ] FAIL - Notes: ___________

Overall: [ ] PASS [ ] FAIL
```
