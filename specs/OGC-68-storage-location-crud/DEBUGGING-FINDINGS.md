# Cypress Integration Test Debugging Findings

## Test Execution Summary

**Duration**: 31 seconds (down from 45+ seconds after removing arbitrary
timeouts)  
**Tests**: 4/4 passing  
**Screenshots**: 14 screenshots captured at key points

## Key Findings

### ✅ B1: Fixed - Follow-up fetch uses authenticated request

- **Status**: FIXED
- **Evidence**: Using `getFromOpenElisServerV2()` instead of raw `fetch()`
- **Verification**: PUT requests succeed (200 status)

### ✅ B2/B5: Fixed - Parent location display

- **Status**: FIXED
- **Evidence**: Test passes - parent room name displays correctly
- **Verification**: `should display parent room name in device edit modal` test
  passes

### ⚠️ B3: Partially Fixed - Active toggle persistence

- **Status**: BACKEND WORKS, UI VISIBILITY ISSUE
- **Evidence from logs**:
  ```
  Toggle aria-checked: true
  Toggle visible: false, parent visibility: hidden
  BUG DETECTED: Toggle is not visible or parent is hidden!
  ```
- **Root Cause**:
  - Backend IS saving correctly (`aria-checked="true"` in HTML)
  - Toggle parent has `visibility: hidden` CSS property
  - This is a UI rendering issue, not a data persistence issue
- **PUT Request Evidence**:
  ```json
  {
    "requestBody": { "active": false },
    "responseBody": { "active": false }
  }
  ```
  Then after toggle ON:
  ```json
  {
    "requestBody": { "active": true }, // ✅ Correctly sent
    "responseBody": { "active": true } // ✅ Backend saved it
  }
  ```
- **Next Steps**: Investigate why Carbon Toggle parent has `visibility: hidden`

### ✅ B4: Fixed - Error message extraction

- **Status**: FIXED
- **Evidence**: Test passes - error messages are extracted from backend
  responses
- **Verification**:
  `should display specific error message for invalid temperature` test passes

### 🔍 Additional Issue Found: Table Refresh

- **Status**: NEEDS INVESTIGATION
- **Evidence**:
  - PUT succeeds (200)
  - Table refresh API call happens (200)
  - But GET verification request returns HTML instead of JSON
- **Root Cause**: `cy.request()` URL needs `/api/OpenELIS-Global/` prefix
- **Fix Applied**: Updated GET request URL to use full API path

## Screenshots Captured

1. `01-initial-page-load.png` - Initial dashboard state
2. `02-rooms-tab-opened.png` - Rooms tab opened
3. `03-edit-modal-opened.png` - Edit modal opened
4. `04-name-updated.png` - Name field updated
5. `05-after-save-clicked.png` - After save button clicked
6. `06-table-shows-updated-name.png` - Table showing updated name
7. `07-devices-tab-opened.png` - Devices tab opened
8. `08-device-edit-modal-opened.png` - Device edit modal opened
9. `09-toggle-not-visible.png` - **KEY**: Toggle not visible (parent hidden)
10. `11-toggle-verified-off.png` - Toggle verified as OFF
11. `12-toggle-clicked-on.png` - Toggle clicked to ON
12. `13-modal-reopened-to-verify.png` - Modal reopened to verify state
13. `14-final-toggle-state.png` - **KEY**: Final toggle state
    (aria-checked="true" but not visible)

## Logging Output

### Frontend Console Errors

- React state update warnings (non-critical, memory leak warnings)
- No JavaScript errors blocking functionality

### Backend Logs

- PUT requests returning 200 (success)
- No validation errors for active toggle
- Backend correctly processing `active: true` and `active: false`

### API Request/Response Logging

- All PUT requests logged with request/response bodies
- Table refresh GET requests logged
- Error responses logged with status codes and bodies

## Recommendations

1. **B3 Toggle Visibility**: Investigate why Carbon Toggle parent has
   `visibility: hidden`

   - Check if `isLoading` state is affecting toggle visibility
   - Check Carbon component CSS/styling
   - May need to force toggle visibility or fix parent container

2. **Table Refresh**: Verify table actually updates in UI (screenshot shows it
   does)

3. **Error Messages**: Already working - backend returns error details, frontend
   displays them

## Test Performance

- **Before**: 45+ seconds with arbitrary 10s timeouts
- **After**: 31 seconds using Cypress retry-ability
- **Improvement**: 31% faster by removing unnecessary timeouts
