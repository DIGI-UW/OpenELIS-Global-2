# Patient Merge Frontend - Quick Start Guide

**Backend Status:** ✅ Complete (44/44 tests passing)
**Branch:** `feat/008-m3-rest-controller`
**API Base:** `/rest/patient/merge`

---

## TL;DR - What You Need to Know

### The Flow
```
Search Patients → Compare Side-by-Side → Select Primary → Validate → Confirm → Execute
```

### The API Calls
```typescript
1. GET  /rest/patient/merge/details/{id}     // Get patient info
2. POST /rest/patient/merge/validate         // Check if merge is safe
3. POST /rest/patient/merge/execute          // Do the merge
```

### The Security
- All endpoints require **ROLE_GLOBAL_ADMIN**
- Returns 403 if user lacks permission
- Returns 401 if session invalid

---

## Quick API Reference

### 1. Get Patient Details
```typescript
GET /rest/patient/merge/details/123

Response 200 OK:
{
  "patientId": "123",
  "firstName": "John",
  "lastName": "Doe",
  "dataSummary": {
    "totalSamples": 15,
    "totalOrders": 8
  },
  "identifiers": [...],
  "conflictingFields": []
}
```

### 2. Validate Merge
```typescript
POST /rest/patient/merge/validate

Request:
{
  "patient1Id": "123",
  "patient2Id": "456",
  "primaryPatientId": "123",  // Which one to keep
  "reason": "Duplicate record",
  "confirmed": false
}

Response 200 OK:
{
  "valid": true,
  "errors": [],
  "warnings": ["Patient 456 has 5 samples"],
  "dataSummary": { ... }
}
```

### 3. Execute Merge
```typescript
POST /rest/patient/merge/execute

Request: (same as validate, but confirmed: true)
{
  "patient1Id": "123",
  "patient2Id": "456",
  "primaryPatientId": "123",
  "reason": "Duplicate record",
  "confirmed": true  // Must be true!
}

Response 200 OK:
{
  "success": true,
  "auditId": "789",
  "recordsReassigned": {
    "samples": 15,
    "total": 26
  }
}
```

---

## Component Structure Suggestion

```
PatientMerge/
├── PatientMergeContainer.tsx         (Main orchestrator)
├── PatientSearchStep.tsx             (Search & select 2 patients)
├── PatientComparisonView.tsx         (Side-by-side comparison)
│   ├── PatientCard.tsx               (Individual patient display)
│   └── DataSummaryCard.tsx           (Show counts)
├── ValidationDialog.tsx              (Show warnings/errors)
├── ConfirmationModal.tsx             (Final confirmation)
└── ResultDisplay.tsx                 (Success/failure message)
```

---

## State Management Example

```typescript
interface PatientMergeState {
  // Step 1: Search
  searchResults: Patient[];

  // Step 2: Selection
  selectedPatients: {
    patient1: PatientDetails | null;
    patient2: PatientDetails | null;
  };

  // Step 3: Comparison
  primaryPatientId: string | null;

  // Step 4: Validation
  validationResult: ValidationResult | null;

  // Step 5: Reason
  mergeReason: string;

  // Step 6: Execution
  executionResult: ExecutionResult | null;

  // UI State
  currentStep: number;
  isLoading: boolean;
  error: string | null;
}
```

---

## Carbon Design Components to Use

### Required Components
- `DataTable` - Patient comparison grid
- `Modal` - Confirmation dialog
- `Button` - Actions (Primary/Ghost variants)
- `TextInput` - Merge reason input
- `InlineNotification` - Warnings/Errors
- `ProgressIndicator` - Multi-step workflow
- `Tag` - Conflicting field indicators
- `SkeletonText` - Loading states

### Layout Components
- `Grid`, `Row`, `Column` - Layout structure
- `Tile` - Patient cards
- `Stack` - Vertical spacing

---

## Error Handling Pattern

```typescript
const handleMergeError = (response: Response) => {
  switch (response.status) {
    case 401:
      router.push('/login');
      break;

    case 403:
      showNotification(
        'Insufficient Permissions',
        'Only Global Administrators can merge patients',
        'error'
      );
      break;

    case 404:
      showNotification(
        'Patient Not Found',
        'One or both patients could not be found',
        'error'
      );
      break;

    case 400:
      response.json().then(data => {
        showNotification('Validation Error', data.message, 'error');
      });
      break;

    default:
      showNotification(
        'Unexpected Error',
        'Please try again or contact support',
        'error'
      );
  }
};
```

---

## Internationalization Keys

```typescript
// Add to your i18n messages file
{
  "patient.merge.title": "Merge Patients",
  "patient.merge.subtitle": "Combine duplicate patient records",
  "patient.merge.selectTwo": "Select two patients to merge",
  "patient.merge.selectPrimary": "Select which patient record to keep",
  "patient.merge.patient1": "Patient 1",
  "patient.merge.patient2": "Patient 2",
  "patient.merge.keepThis": "Keep This Record",
  "patient.merge.reason": "Reason for Merge",
  "patient.merge.reasonPlaceholder": "Enter reason (e.g., duplicate record)",
  "patient.merge.validate": "Validate Merge",
  "patient.merge.confirmMerge": "Confirm Merge",
  "patient.merge.cancel": "Cancel",
  "patient.merge.warning": "Warning",
  "patient.merge.success": "Patients merged successfully",
  "patient.merge.recordsReassigned": "{count} records reassigned",

  // Data summary
  "patient.merge.summary.samples": "Samples",
  "patient.merge.summary.orders": "Orders",
  "patient.merge.summary.contacts": "Contacts",
  "patient.merge.summary.identifiers": "Identifiers",

  // Errors
  "patient.merge.error.noPermission": "Only Global Administrators can merge patients",
  "patient.merge.error.patientNotFound": "Patient not found",
  "patient.merge.error.validationFailed": "Merge validation failed",
  "patient.merge.error.executionFailed": "Merge execution failed"
}
```

---

## Validation Rules

### What the Backend Validates
✅ Both patients exist
✅ Neither patient is already merged
✅ Primary patient ID matches patient1Id or patient2Id
✅ No circular merge references
✅ User has Global Admin role

### What Frontend Should Validate
✅ Two different patients selected
✅ One patient selected as primary
✅ Merge reason is not empty
✅ User confirms the destructive operation

---

## Important Data Notes

### What Gets Merged
✅ **Clinical Data** (reassigned to primary):
- Samples
- Orders
- Contacts
- Relations

✅ **Demographics** (gap-filling only):
- Address, city, state, zip
- Phones (primary, work, cell, fax)
- Email

### What Does NOT Get Merged
❌ **Names** (firstName, lastName, middleName)
- Primary patient's name is kept
- Names are core identifiers

❌ **Identifiers** (national ID, etc.)
- Each patient keeps their own identifiers
- System doesn't support multiple IDs of same type

### Data Summary Fields (Currently Return 0)
⚠️ These are placeholders for future enhancement:
- `activeOrders` (needs status filtering)
- `totalResults` (needs result table joins)
- `totalDocuments` (needs document model)

---

## Testing Your Frontend

### Mock API Server (for development)
```typescript
// Create mock responses for testing
const mockPatientDetails = {
  patientId: "123",
  firstName: "John",
  lastName: "Doe",
  // ... rest of fields
};

const mockValidationResult = {
  valid: true,
  errors: [],
  warnings: ["Patient has 5 samples"]
};

const mockExecutionResult = {
  success: true,
  auditId: "789",
  recordsReassigned: { total: 26 }
};
```

### E2E Test Scenario (Cypress)
```typescript
describe('Patient Merge', () => {
  it('should merge two patients successfully', () => {
    // 1. Login as admin
    cy.login('admin', 'password');

    // 2. Navigate to patient merge
    cy.visit('/patient/merge');

    // 3. Search and select patients
    cy.get('[data-testid="patient-search"]').type('John');
    cy.get('[data-testid="patient-123"]').click();
    cy.get('[data-testid="patient-search"]').type('Jane');
    cy.get('[data-testid="patient-456"]').click();

    // 4. Select primary patient
    cy.get('[data-testid="keep-patient-123"]').click();

    // 5. Validate
    cy.get('[data-testid="validate-merge"]').click();
    cy.contains('Patient has 5 samples');

    // 6. Enter reason and confirm
    cy.get('[data-testid="merge-reason"]').type('Duplicate record');
    cy.get('[data-testid="confirm-merge"]').click();

    // 7. Verify success
    cy.contains('Patients merged successfully');
  });
});
```

---

## Performance Considerations

### What's Fast ⚡
- GET /details/{id} - Simple query, very fast
- POST /validate - No DB writes, fast

### What's Slower 🐢
- POST /execute - Bulk updates, FHIR sync
  - Expected: 1-3 seconds for typical merge
  - Show loading indicator during execution

### Best Practices
1. Debounce patient search (500ms)
2. Cache patient details after fetching
3. Show skeleton loaders during API calls
4. Disable submit button during execution
5. Use optimistic UI updates where possible

---

## Accessibility Checklist

✅ Keyboard navigation (Tab, Enter, Escape)
✅ ARIA labels on all interactive elements
✅ Focus management (modal traps, return focus)
✅ Screen reader announcements for state changes
✅ Color contrast (WCAG 2.1 AA minimum)
✅ Clear error messages
✅ Confirmation dialogs for destructive actions

---

## Common Gotchas & Solutions

### Gotcha 1: Why patient1Id, patient2Id, AND primaryPatientId?
**Answer:** Frontend-friendly design. You display both, user picks primary. Backend validates primary matches one of the two.

### Gotcha 2: Why does confirmed field exist in the API?
**Answer:** Defense-in-depth. Prevents accidental execution if frontend validation fails. Backend enforces it.

### Gotcha 3: What happens if FHIR update fails?
**Answer:** Merge still succeeds! FHIR is optional. Logged as warning, not error.

### Gotcha 4: Can I merge a patient that's already been merged?
**Answer:** No. Validation will fail with "Patient already merged" error.

### Gotcha 5: Why aren't names merged?
**Answer:** Design decision. Names are core identifiers. User explicitly selected which patient to keep.

---

## Getting Help

### Backend API Issues
- Check: `specs/008-patient-merge-backend/spec.md`
- Check: `specs/008-patient-merge-backend/BACKEND_COMPLETE.md`
- Run tests: `mvn test -Dtest="*PatientMerge*Test"`

### Frontend Questions
- Refer to OpenELIS frontend patterns in `frontend/src/components/`
- Check Carbon Design System docs: https://carbondesignsystem.com/
- React Intl docs: https://formatjs.io/docs/react-intl/

### Example Code
- Look at test files for API usage examples
- Check existing OpenELIS forms for Carbon component patterns
- Patient search: `frontend/src/components/patient/PatientSearch.js`

---

## Ready to Start? 🚀

1. **Read:** [BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md) for full API docs
2. **Plan:** Sketch your component structure
3. **Build:** Start with PatientSearchStep.tsx
4. **Test:** Use mock data first, then integrate
5. **Polish:** Internationalization, accessibility, error handling

**The backend is ready and waiting! Good luck!** 💪

---

*Backend: 44/44 tests ✅*
*API: 3 endpoints ready ✅*
*Documentation: Complete ✅*
*Your turn! 🎨*
