# Manual Testing Script: Rack Update Operations

**Purpose**: Test the 3 failing rack update scenarios manually to verify the bug
and confirm the fix.

**Prerequisites**:

- Application running at `https://localhost/Storage`
- Test fixtures loaded (racks with IDs 30, 31, 32 exist)
- Logged in as admin user

---

## Test 1: Edit Rack Label Only

**Expected**: Update rack label without changing parent shelf

**Steps**:

1. Navigate to Storage Dashboard: `https://localhost/Storage`
2. Click **"Racks"** tab
3. Wait for racks table to load (should see at least one rack, e.g., "Rack R1")
4. Find the first rack row (e.g., "Rack R1")
5. Click the **overflow menu** (three dots) in the Actions column for that rack
6. Click **"Edit"** from the dropdown menu
7. **Wait for modal to open** - "Edit Rack" modal should appear
8. **Verify form is populated** - Rack label field should show current value
   (e.g., "Rack R1")
9. **Change the label**:
   - Clear the "Rack" field
   - Type: `Updated Rack [timestamp]` (e.g., "Updated Rack 1234567890")
10. **DO NOT change**:

- Location Code
- Parent Shelf
- Active toggle

11. Click **"Save Changes"** button
12. **Expected Result**:
    - ✅ **PASS**: Modal closes, table refreshes, rack shows new label
    - ❌ **FAIL**: Red error banner appears: "Error Parent shelf ID is required
      for rack code validation"

**Current Status**: ❌ FAILING (400 error)

---

## Test 2: Edit Rack Label and Code

**Expected**: Update both rack label and code

**Steps**:

1. Navigate to Storage Dashboard: `https://localhost/Storage`
2. Click **"Racks"** tab
3. Wait for racks table to load
4. Find the first rack row
5. Click the **overflow menu** (three dots) in the Actions column
6. Click **"Edit"** from the dropdown menu
7. **Wait for modal to open** - "Edit Rack" modal should appear
8. **Change the label**:
   - Clear the "Rack" field
   - Type: `Updated Rack [timestamp]` (e.g., "Updated Rack 1234567891")
9. **Change the code**:
   - Clear the "Location Code" field
   - Type: `RK[timestamp]` (e.g., "RK123456")
10. **Acknowledge code change warning**:
    - Scroll down if needed to see the warning checkbox
    - Check the box: "I understand and want to proceed with the code change"
11. **DO NOT change**:

- Parent Shelf
- Active toggle

12. Click **"Save Changes"** button
13. **Expected Result**:
    - ✅ **PASS**: Modal closes, table refreshes, rack shows new label and code
    - ❌ **FAIL**: Red error banner appears: "Error Parent shelf ID is required
      for rack code validation"

**Current Status**: ❌ FAILING (400 error)

---

## Test 3: Toggle Rack Active Status

**Expected**: Toggle the Active switch and save

**Steps**:

1. Navigate to Storage Dashboard: `https://localhost/Storage`
2. Click **"Racks"** tab
3. Wait for racks table to load
4. Find the first rack row
5. **Note the current Active status** (On/Off) in the table
6. Click the **overflow menu** (three dots) in the Actions column
7. Click **"Edit"** from the dropdown menu
8. **Wait for modal to open** - "Edit Rack" modal should appear
9. **Wait for form to populate** - Active toggle should be visible
10. **Toggle the Active switch**:
    - Click the "Active" toggle (should flip from On to Off, or Off to On)
11. **DO NOT change**:
    - Rack label
    - Location Code
    - Parent Shelf
12. Click **"Save Changes"** button
13. **Expected Result**:
    - ✅ **PASS**: Modal closes, table refreshes, Active status in table matches
      new toggle state
    - ❌ **FAIL**: Red error banner appears: "Error Parent shelf ID is required
      for rack code validation"

**Current Status**: ❌ FAILING (400 error)

---

## Common Observations

**All three tests fail with the same error**:

- Error message: "Error Parent shelf ID is required for rack code validation"
- HTTP Status: 400 Bad Request
- Error appears in red banner at top of modal
- Modal does NOT close after error

**Root Cause**: Backend bug in `StorageLocationRestController.updateRack()` -
when `parentShelfId` is unchanged, the controller doesn't set
`rackToUpdate.setParentShelf()`, leaving it null, which causes validation to
fail.

---

## After Fix Verification

After applying the backend fix, all three tests should:

1. Save successfully (200/201 response)
2. Modal closes automatically
3. Table refreshes showing updated values
4. No error messages displayed
