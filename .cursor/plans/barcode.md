<!-- e85e4271-3af2-4e89-a494-d0f22ea8141a 8c01f277-ba09-4231-b5f9-fed2af9d084b -->

# Barcode Implementation Remediation Plan

## Overview

This plan addresses all gaps identified in the barcode implementation analysis,
completing Iterations 9.2-9.5 to achieve full specification compliance. The plan
follows TDD methodology: write failing tests first, then implement to make tests
pass.

## Current Status

- ✅ **Iteration 9.1 (Backend)**: Complete - all tests passing
- ⚠️ **Iteration 9.2 (Frontend Unified Input)**: Implementation complete, tests
  missing
- ⚠️ **Iteration 9.3 (Debouncing)**: Implementation complete, tests missing
- ❌ **Iteration 9.4 (Last-Modified Wins)**: Not started
- ❌ **Iteration 9.5 (Label Management)**: Not started

## Phase 1: Complete Frontend Test Coverage (Iterations 9.2-9.3)

### 1.1 Write UnifiedBarcodeInput Unit Tests (T237)

**File**:
`frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.test.jsx`

**Test Methods**:

- `testAcceptsKeyboardInput` - Verify manual typing works
- `testAcceptsRapidCharacterInput` - Simulate barcode scanner (50ms intervals)
- `testFormatBasedDetection` - Hyphens = barcode, no hyphens = type-ahead
- `testEnterKeyTriggersValidation` - Enter key calls validation API
- `testFieldBlurTriggersValidation` - Blur event calls validation API
- `testVisualFeedbackStates` - Ready/success/error states display correctly
- `testAutoClearAfterSuccess` - Input clears 2 seconds after success

**Test Pattern**: Follow existing `StorageDashboard.test.jsx` conventions:

- Use `renderWithIntl` helper
- Mock `getFromOpenElisServer` with `jest.mock`
- Use `screen.findBy*` for async, `screen.getBy*` for sync
- Use `waitFor` instead of `setTimeout`
- Mock `useBarcodeDebounce` hook

### 1.2 Write UnifiedBarcodeInput Integration Tests (T238)

**File**:
`frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.integration.test.jsx`

**Test Methods**:

- `testApiCallOnEnter` - Verify API call on Enter key
- `testApiCallOnBlur` - Verify API call on blur
- `testSuccessResponsePopulatesFields` - Success response triggers
  `onValidationResult` with correct data
- `testErrorResponseDisplaysMessage` - Error response shows error message
- `testPartialValidationPreFillsComponents` - Partial validation calls
  `onValidationResult` with `validComponents`

**Integration Points**: Test actual API calls to
`/rest/storage/barcode/validate` endpoint

### 1.3 Write BarcodeDebounceHook Unit Tests (T245)

**File**:
`frontend/src/components/storage/StorageLocationSelector/BarcodeDebounceHook.test.js`

**Test Methods**:

- `testDuplicateBarcodeWithin500msIgnored` - Same barcode within 500ms is
  ignored silently
- `testDifferentBarcodeWithin500msShowsWarning` - Different barcode within 500ms
  shows warning via `onWarning` callback
- `testBarcodeAfter500msProcessed` - Barcode after 500ms cooldown is processed
  normally
- `testCooldownTimerResets` - Cooldown timer resets after each successful scan
- `testMultipleRapidScansHandled` - Multiple rapid scans handled correctly (only
  last one processed after cooldown)

**Test Pattern**: Use `@testing-library/react-hooks` or `renderHook` from React
Testing Library

### 1.4 Run Tests to Verify They Fail (T239, T246)

```bash
cd frontend && npm test UnifiedBarcodeInput --no-coverage --watchAll=false
cd frontend && npm test BarcodeDebounceHook --no-coverage --watchAll=false
```

**Expected**: All tests should fail initially (TDD red phase)

## Phase 2: Implement "Last-Modified Wins" Logic (Iteration 9.4)

### 2.1 Write LocationSelectorModal Tests (T250)

**File**:
`frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.test.jsx`
(create if missing, update if exists)

**Test Methods**:

- `testDropdownThenInputOverwrites` - Dropdown selection then barcode scan
  overwrites dropdown values
- `testInputThenDropdownOverwrites` - Barcode scan then dropdown selection
  overwrites input values
- `testVisualFeedbackShowsActiveMethod` - Visual feedback (highlight
  border/icon) shows which method is active
- `testNoErrorWhenSwitching` - No error when switching between methods
- `testBothMethodsVisibleSimultaneously` - Both dropdowns and input field
  visible at same time

**Test Pattern**: Mock `UnifiedBarcodeInput` and `LocationSearchAndCreate`
components, verify state updates

### 2.2 Run Tests to Verify They Fail (T251)

```bash
cd frontend && npm test LocationSelectorModal --no-coverage --watchAll=false
```

### 2.3 Implement Last-Modified Tracking (T252)

**File**:
`frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.jsx`

**Changes**:

- Add state:
  `const [lastModifiedMethod, setLastModifiedMethod] = useState(null);` (null |
  'dropdown' | 'barcode')
- Add state:
  `const [lastModifiedTimestamp, setLastModifiedTimestamp] = useState(null);`
- Update `handleLocationChange` (dropdown selection):

  ```javascript
  setLastModifiedMethod("dropdown");
  setLastModifiedTimestamp(Date.now());
  setSelectedLocation(location);
  ```

- Update `handleBarcodeValidationResult` (barcode scan):

  ```javascript
  setLastModifiedMethod("barcode");
  setLastModifiedTimestamp(Date.now());
  // ... existing validation result handling
  ```

- Implement overwrite logic: When one method modifies location, check
  `lastModifiedTimestamp` and overwrite if newer

### 2.4 Add Visual Feedback for Active Method (T253)

**File**:
`frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.jsx`

**Changes**:

- Add CSS class or inline style to highlight active method:
  - `UnifiedBarcodeInput`: Add
    `className={lastModifiedMethod === 'barcode' ? 'active-input-method' : ''}`
  - `LocationSearchAndCreate`: Add prop
    `isActive={lastModifiedMethod === 'dropdown'}` and apply highlight style
- Add icon indicator (optional): Show checkmark icon next to active method
- Use Carbon Design System styling (no custom CSS frameworks)

**CSS**: Add to `LocationSelectorModal.css`:

```css
.active-input-method {
  border: 2px solid var(--cds-border-interactive);
  box-shadow: 0 0 0 1px var(--cds-border-interactive);
}
```

## Phase 3: Add Dual Barcode Auto-Detection (FR-024b)

### 3.1 Update BarcodeValidationService for Sample Detection

**File**:
`src/main/java/org/openelisglobal/storage/service/BarcodeValidationServiceImpl.java`

**Changes**:

- Add method `detectBarcodeType(String barcode)` returning
  `'location' | 'sample' | 'unknown'`
- Sample barcode pattern: Match accession number format (configurable via system
  admin, default: `S-YYYY-NNNNN` or similar)
- Update `validateBarcode` to detect type and return `barcodeType` in response

**File**:
`src/main/java/org/openelisglobal/storage/service/BarcodeValidationResponse.java`

**Changes**:

- Add field: `private String barcodeType;` (getter/setter)

### 3.2 Update Frontend to Handle Sample Barcodes

**File**:
`frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.jsx`

**Changes**:

- Update `processInput` to check `barcodeType` from validation response
- If `barcodeType === 'sample'`, call `onSampleScan` callback (new prop)
- If `barcodeType === 'location'`, proceed with existing location population
  logic

**File**:
`frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.jsx`

**Changes**:

- Add `handleSampleScan` callback:

  ```javascript
  const handleSampleScan = (sampleData) => {
    // Load sample details, pre-fill sample context
    // This may trigger different UI flow (sample assignment vs location selection)
  };
  ```

- Pass `onSampleScan={handleSampleScan}` to `UnifiedBarcodeInput`

### 3.3 Add Tests for Dual Barcode Detection

**File**:
`src/test/java/org/openelisglobal/storage/service/BarcodeValidationServiceTest.java`

**New Test Methods**:

- `testDetectLocationBarcode` - Hierarchical format detected as location
- `testDetectSampleBarcode` - Accession number format detected as sample
- `testUnknownBarcodeType` - Invalid format returns unknown

## Phase 4: Verify Error Message Format (FR-024g)

### 4.1 Update Backend Error Messages

**File**:
`src/main/java/org/openelisglobal/storage/service/BarcodeValidationServiceImpl.java`

**Changes**:

- Update error messages to include raw barcode string and parsed components
- Format:
  `"Scanned code: MAIN-FRZ01-SHA-RKR1 (Room: MAIN, Device: FRZ01, Shelf: SHA, Rack: RKR1). Rack 'RKR1' not found in Shelf 'SHA'"`
- If parsing fails, show only raw string:
  `"Scanned code: INVALID-CODE. Invalid barcode format."`

**File**:
`src/main/java/org/openelisglobal/storage/service/BarcodeValidationResponse.java`

**Changes**:

- Ensure `errorMessage` field contains full formatted message
- Add helper method
  `formatErrorMessage(String rawBarcode, ParsedBarcode parsed, String specificError)`

### 4.2 Verify Frontend Displays Full Error Message

**File**:
`frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.jsx`

**Changes**:

- Verify `barcodeErrorMessage` displays full formatted message from backend
- No truncation or reformatting needed (backend provides complete message)

### 4.3 Add Test for Error Message Format

**File**:
`src/test/java/org/openelisglobal/storage/service/BarcodeValidationServiceTest.java`

**New Test Method**:

- `testErrorMessageFormatIncludesRawAndParsed` - Verify error message format
  matches FR-024g specification

## Phase 5: Implement Label Management (Iteration 9.5)

### 5.1 Backend: Short Code Validation Service (T254, T260)

**File**:
`src/test/java/org/openelisglobal/storage/service/ShortCodeValidationServiceTest.java`
(write tests first)

**Test Methods**:

- `testShortCodeFormat` - Max 10 chars, alphanumeric, hyphen/underscore allowed
- `testAutoUppercaseConversion` - Input auto-converted to uppercase
- `testMustStartWithLetterOrNumber` - Reject codes starting with
  hyphen/underscore
- `testUniquenessWithinContext` - Validate uniqueness within device/shelf/rack
  scope
- `testWarningWhenChangingShortCode` - Warning generated when short code changes

**File**:
`src/main/java/org/openelisglobal/storage/service/ShortCodeValidationService.java`
(implement after tests)

**Methods**:

- `validateFormat(String shortCode)` - Returns validation result with error
  message
- `validateUniqueness(String shortCode, String context, String locationId)` -
  Check uniqueness
- `checkShortCodeChangeWarning(String oldCode, String newCode, String locationId)` -
  Generate warning message listing affected locations

### 5.2 Backend: Label Generation Service (T261, T262)

**File**:
`src/main/java/org/openelisglobal/storage/service/LabelManagementService.java`

**Methods**:

- `generateLabel(StorageDevice device, String shortCode)` - Generate PDF label
- `generateLabel(StorageShelf shelf, String shortCode)` - Generate PDF label
- `generateLabel(StorageRack rack, String shortCode)` - Generate PDF label
- `trackPrintHistory(String locationId, String locationType, String userId)` -
  Record print audit trail

**File**:
`src/main/java/org/openelisglobal/storage/barcode/labeltype/StorageLocationLabel.java`

**Implementation**:

- Extend `org.openelisglobal.barcode.labeltype.Label`
- Use hierarchical path or short code for barcode value
- Read dimensions from
  `ConfigurationProperties.STORAGE_LOCATION_BARCODE_HEIGHT/WIDTH`
- Display location name, code, hierarchical path on label
- Use `Barcode128` for Code 128 barcode generation (existing iTextPDF library)

**Integration**: Follow pattern from `research.md` Section 9, reuse
`BarcodeLabelMaker` infrastructure

### 5.3 Backend: REST Controller (T263)

**File**:
`src/main/java/org/openelisglobal/storage/controller/LabelManagementRestController.java`

**Endpoints**:

- `PUT /rest/storage/{type}/{id}/short-code` - Update short code (body:
  `{ "shortCode": "FRZ01" }`)
- `POST /rest/storage/{type}/{id}/print-label` - Generate and return PDF label
  (query param: `?shortCode=FRZ01`)
- `GET /rest/storage/{type}/{id}/print-history` - Get print history (returns
  list of print records)

**Request/Response**: Use DTOs for request/response bodies, follow existing REST
API patterns

### 5.4 Backend: Database Schema (T264)

**File**:
`src/main/resources/liquibase/storage/004-create-print-history-table.xml`

**Table**: `storage_location_print_history`

**Columns**:

- `id` (UUID, primary key)
- `location_type` (VARCHAR: 'device' | 'shelf' | 'rack')
- `location_id` (VARCHAR, foreign key to respective table)
- `short_code` (VARCHAR(10), nullable)
- `printed_by` (VARCHAR, user ID)
- `printed_date` (TIMESTAMP)
- `print_count` (INTEGER, default 1)

**Liquibase Changeset**: Include rollback script

### 5.5 Backend: Configuration Properties (T265, T266)

**File**:
`src/main/java/org/openelisglobal/common/util/ConfigurationProperties.java`

**Changes**:

- Add to `Property` enum: `STORAGE_LOCATION_BARCODE_HEIGHT`,
  `STORAGE_LOCATION_BARCODE_WIDTH`
- Default values: Height=50mm, Width=100mm (or match existing label dimensions)

**File**:
`src/main/java/org/openelisglobal/barcode/form/BarcodeConfigurationForm.java`

**Changes**:

- Add fields: `heightStorageLocationLabels`, `widthStorageLocationLabels`
- Add to form UI (system administration page)

### 5.6 Frontend: Label Management Modal (T267-T270)

**File**:
`frontend/src/components/storage/LocationManagement/LabelManagementModal.jsx`

**Structure**:

- Modal title: "Label Management" (React Intl key: `label.management.title`)
- Short Code input field (use `ShortCodeInput` component)
- Print Label button (use `PrintLabelButton` component)
- Print History display (use `PrintHistoryDisplay` component)
- Warning dialog for short code changes (Carbon `Modal` with confirmation)

**File**:
`frontend/src/components/storage/LocationManagement/ShortCodeInput.jsx`

**Features**:

- Max 10 characters
- Auto-uppercase on input
- Validation: alphanumeric, hyphen, underscore only
- Must start with letter or number
- Show validation errors inline

**File**:
`frontend/src/components/storage/LocationManagement/PrintLabelButton.jsx`

**Features**:

- Calls `POST /rest/storage/{type}/{id}/print-label?shortCode={code}`
- Opens PDF in new tab (same pattern as existing label printing)
- Shows loading state during PDF generation

**File**:
`frontend/src/components/storage/LocationManagement/PrintHistoryDisplay.jsx`

**Features**:

- Calls `GET /rest/storage/{type}/{id}/print-history`
- Displays: "Last printed: [date] [time] by [user]"
- Optional "View History" link (expandable list of all print records)

### 5.7 Frontend: Integration with Overflow Menu (T271)

**File**:
`frontend/src/components/storage/LocationManagement/LocationActionsOverflowMenu.jsx`

**Changes**:

- Add "Label Management" menu item for Devices, Shelves, and Racks
- On click, open `LabelManagementModal` with location context
- Hide menu item for Rooms (not applicable per spec)

### 5.8 Frontend: Internationalization (T272, T244, T249)

**Files**: `frontend/src/languages/en.json`, `fr.json`, `sw.json`

**Add Message Keys**:

- `barcode.ready` - "Ready to scan"
- `barcode.success` - "Barcode scanned successfully"
- `barcode.error` - "Invalid barcode"
- `barcode.scanOrType` - "Scan barcode or type location code"
- `barcode.invalidFormat` - "Invalid barcode format"
- `barcode.debounce.warning` - "Please wait before next scan"
- `label.management.title` - "Label Management"
- `label.shortCode` - "Short Code"
- `label.print` - "Print Label"
- `label.printHistory` - "Print History"
- `label.shortCodeWarning` - "Changing short code will invalidate existing
  labels"
- `label.lastPrinted` - "Last printed: {date} {time} by {user}"

### 5.9 Backend Integration Tests (T255)

**File**:
`src/test/java/org/openelisglobal/storage/controller/LabelManagementRestControllerTest.java`

**Test Methods**:

- `testPutShortCodeEndpoint` - Verify short code update
- `testPostPrintLabelEndpoint` - Verify PDF generation
- `testPrintHistoryTracking` - Verify print history recorded
- `testPdfGenerationWithSystemAdminSettings` - Verify label dimensions from
  config

### 5.10 Frontend Unit Tests (T256)

**File**:
`frontend/src/components/storage/LocationManagement/LabelManagementModal.test.jsx`

**Test Methods**:

- `testShortCodeInputValidation` - Format validation works
- `testAutoUppercaseOnInput` - Auto-uppercase conversion
- `testWarningDialogBeforeChange` - Warning dialog displays
- `testPrintLabelOpensPdf` - Print button opens PDF
- `testPrintHistoryDisplay` - Print history loads and displays

## Phase 6: E2E Tests (Iteration 9.6)

### 6.1 Write Cypress E2E Tests (T273)

**File**: `frontend/cypress/e2e/barcodeWorkflow.cy.js`

**Test Cases** (per Constitution V.5 - run individually):

- `testScan4LevelBarcodePopulatesFields` - Scan "MAIN-FRZ01-SHA-RKR1", verify
  fields populate
- `testScan2LevelBarcodeMinimum` - Scan "MAIN-FRZ01", verify Room+Device
  populate
- `testScanInvalidBarcodeShowsError` - Scan invalid code, verify error message
- `testDebouncingPreventsDuplicateScans` - Rapid duplicate scans ignored
- `testLastModifiedWinsLogic` - Dropdown then scan overwrites, scan then
  dropdown overwrites
- `testLabelManagementModalOpens` - Overflow menu → Label Management opens modal
- `testShortCodeChangeShowsWarning` - Changing short code shows warning dialog
- `testPrintLabelGeneratesPdf` - Print button generates PDF in new tab
- `testPrintHistoryDisplays` - Print history shows last printed info

**Configuration**:

- `cypress.config.js`: `video: false`, `screenshotOnRunFailure: true`
- Use intercepts before actions
- Review console logs after each test run

### 6.2 Run E2E Tests (T274)

```bash
cd frontend && npm run cy:run -- --spec "cypress/e2e/barcodeWorkflow.cy.js"
```

**Expected**: Tests fail initially, then pass after implementation

## Phase 7: Verification and Cleanup

### 7.1 Verify All Tests Pass

- Backend unit tests: `mvn test -Dtest="*Barcode*,*Label*"`
- Frontend unit tests:
  `cd frontend && npm test -- --testPathPattern="Barcode|Label"`
- Integration tests: `mvn test -Dtest="*RestControllerTest"`
- E2E tests: Run individually per Constitution V.5

### 7.2 Code Formatting

```bash
mvn spotless:apply
cd frontend && npm run format
```

### 7.3 Internationalization Audit

```bash
grep -r '"[A-Z]' frontend/src/components/storage/StorageLocationSelector/
grep -r '"[A-Z]' frontend/src/components/storage/LocationManagement/
```

**Expected**: No hardcoded English strings (all via React Intl)

### 7.4 Constitution Compliance Check

- ✅ Layered Architecture: Services → Controllers (no DAO calls from
  controllers)
- ✅ Carbon Design System: All UI components use `@carbon/react`
- ✅ Internationalization: All strings externalized
- ✅ Test Coverage: >70% for new code
- ✅ TDD Workflow: Tests written before implementation

## Dependencies

- **Phase 1** can run independently (test writing)
- **Phase 2** depends on Phase 1 completion (tests inform implementation)
- **Phase 3** can run in parallel with Phase 2
- **Phase 4** can run in parallel with Phase 2-3
- **Phase 5** depends on backend tests (T254) but frontend can start after T260
- **Phase 6** depends on all previous phases

## Success Criteria

- All frontend unit tests pass (T237-T238, T245-T246, T250-T251, T256)
- All backend unit tests pass (T254, T255)
- "Last-modified wins" logic works with visual feedback
- Dual barcode detection distinguishes sample vs location barcodes
- Error messages match FR-024g format specification
- Label Management modal functional (short code, print, history)
- All E2E tests pass (T273)
- Code formatted and linted
- No hardcoded strings (React Intl complete)
- Constitution compliance verified
