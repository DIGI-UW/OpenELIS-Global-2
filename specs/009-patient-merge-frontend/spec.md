# Feature Specification: Patient Merge Frontend

**Feature Branch**: `009-patient-merge-frontend` **Created**: 2025-12-05
**Status**: Draft **Input**: Frontend implementation for patient merge UI
including patient selection, merge workflow, confirmation dialogs, real-time
notifications using Carbon Design System and React Intl

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Menu Access for Global Administrators (Priority: P1)

As a Global Administrator, I need to see and access the "Merge Patient" menu
option so that I can initiate patient merge operations when duplicates are
detected.

**Why this priority**: Foundation for all frontend functionality - users must be
able to navigate to the feature.

**Independent Test**: Can be fully tested by logging in as Global Administrator
and verifying menu item appears in Patient menu, and logging in as non-admin to
verify it's hidden.

**Acceptance Scenarios**:

1. **Given** user has Global Administrator permission, **When** navigating to
   Main Menu → Patient, **Then** "Merge Patient" menu item is visible
2. **Given** user does not have Global Administrator permission, **When**
   navigating to Main Menu → Patient, **Then** "Merge Patient" menu item is not
   visible
3. **Given** Global Administrator clicks "Merge Patient", **When** menu item is
   selected, **Then** browser navigates to `/patient/merge` route

---

### User Story 2 - Patient Selection Interface (Priority: P1)

As a Global Administrator, I need to select two patients to merge using the
existing patient search modal so that I can identify which records should be
consolidated.

**Why this priority**: Core functionality - cannot proceed with merge without
selecting patients first.

**Independent Test**: Can be fully tested by navigating to merge page, clicking
patient selection buttons, searching for patients, and verifying selected
patients are displayed correctly with all demographics.

**Acceptance Scenarios**:

1. **Given** on patient merge page, **When** clicking "Select Patient" button
   for first slot, **Then** existing patient search modal opens
2. **Given** patient search modal is open, **When** selecting a patient from
   search results, **Then** modal closes and patient card displays with
   demographics (ID, name, DOB, gender, national ID, phone, address, data
   counts)
3. **Given** first patient is already selected, **When** selecting second
   patient, **Then** cannot select the same patient (validation prevents
   duplicate selection)
4. **Given** patient card is displayed, **When** clicking Remove X button,
   **Then** patient selection is cleared and card returns to empty state
5. **Given** two different patients are selected, **When** both cards are
   populated, **Then** "Next Step" button becomes enabled

---

### User Story 3 - Primary Patient Selection with Data Review (Priority: P1)

As a Global Administrator, I need to choose which patient becomes the primary
record while reviewing both patients' demographics and clinical data so that I
can make an informed decision about which record to keep.

**Why this priority**: Critical decision point that determines which
demographics are preserved.

**Independent Test**: Can be fully tested by selecting two patients and
verifying primary selection interface displays complete patient information with
expandable accordions and proper radio button selection.

**Acceptance Scenarios**:

1. **Given** two patients are selected, **When** clicking "Next Step" from
   selection page, **Then** navigate to primary selection page showing both
   patients with radio buttons
2. **Given** on primary selection page, **When** viewing patient options,
   **Then** each patient displays demographics accordion, clinical summary
   accordion, and identifiers accordion (all initially collapsed)
3. **Given** patient accordion is collapsed, **When** clicking accordion header,
   **Then** accordion expands showing detailed information (Demographics:
   name/DOB/gender/IDs/contact; Clinical: order counts/result counts/sample
   counts/date range; Identifiers: all patient IDs and external IDs)
4. **Given** no primary patient selected, **When** attempting to click "Next
   Step", **Then** button is disabled and validation message appears
5. **Given** primary patient is selected via radio button, **When** clicking
   "Next Step", **Then** navigate to confirmation page

---

### User Story 4 - Review and Confirmation Dialog (Priority: P1)

As a Global Administrator, I need to review a detailed merge summary and
explicitly confirm the operation before execution so that I understand the
impact and can prevent accidental merges.

**Why this priority**: Critical safety mechanism to prevent data corruption from
accidental merges.

**Independent Test**: Can be fully tested by proceeding through merge workflow
to confirmation page and verifying all summary data is displayed correctly with
proper warnings and validation.

**Acceptance Scenarios**:

1. **Given** primary patient selected, **When** reaching confirmation page,
   **Then** display critical warning banner stating action cannot be undone
2. **Given** on confirmation page, **When** viewing merge summary, **Then**
   display primary patient ID/name, merging patient ID/name, data counts
   (orders, results, samples, documents, audit entries), all identifiers from
   both patients, and detected conflicts (phone, email, address differences)
3. **Given** viewing merge summary, **When** conflicting information exists,
   **Then** display separate "Conflicting Information Detected" section
   highlighting differences with note that primary patient data will be used
4. **Given** on confirmation page, **When** "Reason for Merge" text area is
   empty, **Then** "Confirm Merge" button is disabled
5. **Given** reason is provided but confirmation checkbox unchecked, **When**
   attempting to submit, **Then** "Confirm Merge" button remains disabled
6. **Given** reason provided and checkbox checked, **When** clicking "Confirm
   Merge", **Then** call backend merge API, display loading state, and show
   success or error result

---

### User Story 5 - Success State and Navigation (Priority: P2)

As a Global Administrator, I need clear confirmation when a merge completes
successfully and options to view the merged patient or start another merge so
that I can verify the result or continue working.

**Why this priority**: Important for user confidence and workflow continuation,
but depends on core merge completing first.

**Independent Test**: Can be fully tested by completing a merge successfully and
verifying success message displays with correct patient IDs and action buttons
work properly.

**Acceptance Scenarios**:

1. **Given** merge API call succeeds, **When** response is received, **Then**
   display success notification with checkmark, primary patient ID/name, merged
   patient ID (now inactive), and confirmation message about audit trail
2. **Given** success notification is displayed, **When** clicking "View Patient"
   button, **Then** navigate to primary patient's detail page
3. **Given** success notification is displayed, **When** clicking "Merge Another
   Patient" button, **Then** navigate back to patient selection page (step 1)
   with all fields cleared
4. **Given** success notification is displayed, **When** page is shown, **Then**
   automatically focus "View Patient" button for keyboard accessibility

---

### User Story 6 - Error Handling and User Feedback (Priority: P2)

As a Global Administrator, I need clear, actionable error messages when
something goes wrong so that I can understand what happened and how to resolve
it.

**Why this priority**: Essential for usability and troubleshooting, but depends
on core functionality being built first.

**Independent Test**: Can be fully tested by simulating various error conditions
(API failures, validation errors, permission errors) and verifying appropriate
Carbon InlineNotification components are displayed with correct messaging.

**Acceptance Scenarios**:

1. **Given** API call fails with permission error, **When** error is returned,
   **Then** display error notification with message: "You do not have permission
   to merge patients. Global Administrator permission is required."
2. **Given** API call fails with validation error (same patient, already merged,
   etc.), **When** error is returned, **Then** display error notification with
   specific validation message from backend
3. **Given** API call fails with network/server error, **When** error is
   returned, **Then** display error notification with generic message: "An error
   occurred while merging patients. Please contact support."
4. **Given** merge involves 500+ results (performance warning), **When**
   validation API returns warning, **Then** display warning notification on
   confirmation page stating operation may take up to 30 seconds
5. **Given** error notification is displayed, **When** user corrects the issue,
   **Then** error notification is cleared before next action

---

### User Story 7 - Real-Time Notification for Open Patient Records (Priority: P3)

As any system user, I need to be notified immediately when a patient record I'm
viewing has been merged so that I see current, accurate information and don't
work with stale data.

**Why this priority**: Important for data integrity across active sessions, but
depends on core merge and backend notification system being implemented first.

**Independent Test**: Can be fully tested by opening a patient record in one
browser session, merging that patient in another session as Global Admin, and
verifying notification appears and reload button works correctly.

**Acceptance Scenarios**:

1. **Given** user is viewing patient record page, **When** that patient is
   merged by another user, **Then** warning notification appears at top of page
   stating "Patient record has been merged" and page needs reload
2. **Given** merge notification is displayed, **When** user clicks "Reload Page"
   button, **Then** page reloads and navigates to primary patient record if
   viewing merged patient, or reloads current page if viewing primary patient
3. **Given** user has patient dashboard open, **When** patient is merged,
   **Then** notification also appears on dashboard pages (results entry, order
   entry, etc.)
4. **Given** WebSocket connection is unavailable, **When** merge occurs,
   **Then** polling mechanism detects change within 10 seconds and displays
   notification
5. **Given** multiple tabs open with same patient, **When** merge notification
   received, **Then** notification appears in all tabs simultaneously

---

### User Story 8 - Internationalization Support (Priority: P2)

As a system user in any language locale, I need all UI text to appear in my
selected language so that I can use the merge feature in my preferred language.

**Why this priority**: Required for global deployment per constitution, but can
be implemented after English UI is working.

**Independent Test**: Can be fully tested by switching application locale and
verifying all strings (labels, buttons, error messages, warnings, notifications)
are translated correctly using React Intl message IDs.

**Acceptance Scenarios**:

1. **Given** application locale is English, **When** viewing merge page,
   **Then** all UI strings display in English using React Intl FormattedMessage
   components
2. **Given** application locale is French, **When** viewing merge page, **Then**
   all UI strings display in French translations
3. **Given** error message is received from backend, **When** displaying to
   user, **Then** error message key is used to lookup translated text via React
   Intl
4. **Given** dynamic content includes patient names/IDs, **When** rendering
   messages, **Then** use React Intl formatMessage with placeholder substitution
   (e.g., "Merging {patientName}")
5. **Given** all strings are translated, **When** new locale is added, **Then**
   only message catalog file needs updating (no code changes required)

---

### Edge Cases

- What happens when user navigates away mid-merge workflow? (Confirm dialog
  warns about losing progress)
- How does UI handle when backend API is slow or times out? (Loading spinner
  with timeout warning after 10 seconds)
- What happens if user refreshes page during patient selection? (State is lost,
  must start over - consider session storage for future enhancement)
- How does UI adapt for screen readers and keyboard navigation? (Carbon
  components provide WCAG 2.1 AA compliance by default, ensure proper focus
  management)
- What happens if patient data is too long for card display? (Use Carbon Tile
  with scrollable content or truncation with ellipsis)
- How does notification system handle when multiple patients are merged
  simultaneously? (Queue notifications, display one at a time with count
  indicator)
- What happens if same patient is merged multiple times in different browsers?
  (Backend validation prevents, frontend shows validation error on second
  attempt)
- How does UI handle when patient has no contact information? (Display "Not
  recorded" for empty fields using React Intl)

## Requirements _(mandatory)_

### Functional Requirements

**Navigation & Menu**

- **FR-001**: Main Menu "Patient" submenu MUST display "Merge Patient" menu item
  only when current user has Global Administrator permission
- **FR-002**: "Merge Patient" menu item MUST navigate to `/patient/merge` route
  when clicked
- **FR-003**: Menu item visibility check MUST occur client-side using user
  permissions from session/context

**Patient Selection (Step 1)**

- **FR-004**: Page MUST display two patient selection slots with "Select
  Patient" buttons using Carbon Button component
- **FR-005**: "Select Patient" button MUST open existing patient search modal
  component (reuse existing implementation)
- **FR-006**: When patient is selected from modal, MUST display patient card
  using Carbon Tile component with StructuredList showing: Patient ID, Name,
  DOB, Gender, National ID, Phone, Address, Active Orders count, Total Results
  count, Total Samples count
- **FR-007**: Patient card MUST include Remove button (X icon) to clear
  selection
- **FR-008**: System MUST prevent same patient from being selected in both slots
  (client-side validation)
- **FR-009**: "Next Step" button MUST be disabled until two different patients
  are selected
- **FR-010**: System MUST call `GET /api/patient/merge-details/{patientId}` for
  each selected patient to retrieve full demographics and data summary

**Primary Patient Selection (Step 2)**

- **FR-011**: Page MUST display two patient options with Carbon RadioButtonGroup
  for primary selection
- **FR-012**: Each patient option MUST display three Carbon Accordion sections:
  Demographics, Clinical Summary, Identifiers (all initially collapsed)
- **FR-013**: Demographics accordion MUST show: Name, DOB, Gender, National ID,
  Passport (if present), Phone, Email, Full Address
- **FR-014**: Clinical Summary accordion MUST show: Total Orders, Total Results,
  Total Samples, Active Orders, Date Range (earliest to most recent order)
- **FR-015**: Identifiers accordion MUST show: All Patient IDs, National IDs,
  External IDs in bulleted list
- **FR-016**: Warning banner (Carbon InlineNotification type="warning") MUST
  display explaining primary patient will receive all future data
- **FR-017**: "Next Step" button MUST be disabled until one patient is selected
  via radio button
- **FR-018**: "Back" button MUST return to patient selection page without losing
  selections

**Review & Confirmation (Step 3)**

- **FR-019**: Page MUST display critical warning banner (Carbon
  InlineNotification type="error") stating action cannot be undone
- **FR-020**: Page MUST call `POST /api/patient/merge/validate` with both
  patient IDs and primary selection to retrieve validation results and data
  summary
- **FR-021**: Merge summary section MUST display: Primary patient ID/name,
  Merging patient ID/name, Data counts (orders with active count, results,
  samples, documents, audit entries), All identifiers from both patients, Note
  that audit entries retain original patient reference
- **FR-022**: If conflicting demographics detected, MUST display "Conflicting
  Information Detected" section listing differences in phone/email/address with
  note that primary patient values will be preserved
- **FR-023**: "Reason for Merge" MUST be required Carbon TextArea with
  validation
- **FR-024**: Confirmation checkbox MUST be required with text "I understand
  this action cannot be undone and have verified the correct patients are being
  merged"
- **FR-025**: "Confirm Merge" button MUST be disabled until reason is provided
  AND checkbox is checked
- **FR-026**: When "Confirm Merge" clicked, MUST call
  `POST /api/patient/merge/execute` with patient1_id, patient2_id,
  primary_patient_id, reason, confirmation=true
- **FR-027**: During API call, MUST display loading state (Carbon Loading
  component) and disable all buttons
- **FR-028**: If validation warnings exist (e.g., 500+ results), MUST display
  warning notification about potential 30-second operation time

**Success State (Step 4)**

- **FR-029**: On successful merge, MUST display success notification (Carbon
  InlineNotification type="success") with checkmark icon
- **FR-030**: Success message MUST include: Primary patient ID and name, Merged
  patient ID with "(now inactive)" label, Confirmation that data is consolidated
  and logged
- **FR-031**: "View Patient" button MUST navigate to primary patient's detail
  page
- **FR-032**: "Merge Another Patient" button MUST navigate back to patient
  selection page (step 1) with all state cleared
- **FR-033**: Success page MUST automatically focus "View Patient" button for
  keyboard accessibility

**Error Handling**

- **FR-034**: All API errors MUST display using Carbon InlineNotification
  type="error"
- **FR-035**: Permission errors (403) MUST display: "You do not have permission
  to merge patients. Global Administrator permission is required."
- **FR-036**: Validation errors (400) MUST display specific error message from
  backend (e.g., "Cannot merge patient with itself", "Patient already merged",
  "Circular reference detected")
- **FR-037**: Not Found errors (404) MUST display: "One or both patients could
  not be found."
- **FR-038**: Server errors (500) MUST display: "An error occurred while merging
  patients. Please contact support."
- **FR-039**: Network timeout (no response after 30 seconds) MUST display:
  "Request timed out. Please check your connection and try again."
- **FR-040**: Error notifications MUST be dismissible and clear when user takes
  corrective action

**Real-Time Notifications**

- **FR-041**: System MUST establish WebSocket connection to `/ws/notifications`
  endpoint on application load
- **FR-042**: When "PATIENT_MERGED" event received, MUST check if current page
  displays either patient ID
- **FR-043**: If current page displays merged patient, MUST display warning
  notification (Carbon InlineNotification type="warning") at top of page:
  "Patient record has been merged. The page needs to be reloaded to display
  current information."
- **FR-044**: Notification MUST include "Reload Page" button that refreshes page
  and navigates to primary patient if viewing merged patient
- **FR-045**: If WebSocket unavailable, MUST implement polling mechanism
  checking for patient updates every 10 seconds
- **FR-046**: Notification MUST appear on all patient-related pages: patient
  dashboard, results entry, order entry, sample entry
- **FR-047**: If multiple tabs open with same patient, notification MUST appear
  in all tabs (use BroadcastChannel API or localStorage events)

**Internationalization**

- **FR-048**: All UI strings (labels, buttons, messages, warnings, errors) MUST
  use React Intl FormattedMessage or formatMessage
- **FR-049**: All strings MUST have message IDs defined in
  `frontend/src/lang/en-US.json` (and other locale files)
- **FR-050**: Dynamic content (patient names, IDs, counts) MUST use React Intl
  message placeholders with variable substitution
- **FR-051**: Error messages from backend MUST be mapped to message IDs for
  translation lookup
- **FR-052**: Date and number formatting MUST use React Intl FormattedDate and
  FormattedNumber

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - includes only
frontend-relevant principles:_

- **CR-001**: ALL UI components MUST use Carbon Design System (@carbon/react) -
  NO custom CSS frameworks, NO Bootstrap, NO Tailwind
- **CR-002**: Required Carbon components for this feature: Button, Tile,
  StructuredList, RadioButtonGroup, Accordion, InlineNotification, TextArea,
  Checkbox, Loading, Modal (reuse existing patient search)
- **CR-003**: ALL UI strings MUST be internationalized via React Intl - NO
  hardcoded text anywhere in JSX
- **CR-004**: Tests MUST include: React Testing Library unit tests (component
  behavior, validation, conditional rendering), Cypress E2E tests (complete
  merge workflow, error scenarios), >70% code coverage goal
- **CR-005**: Accessibility MUST be WCAG 2.1 AA compliant: proper ARIA labels,
  keyboard navigation support, focus management, screen reader compatibility
  (Carbon provides this by default)
- **CR-006**: Configuration-driven variation: Use feature flags or environment
  config for WebSocket vs polling notification strategy (NO code branching by
  country)
- **CR-007**: All API calls MUST include CSRF token from session/context
- **CR-008**: Client-side validation MUST match backend validation rules to
  provide immediate feedback

### Key UI Components

- **PatientMergeContainer**: Main page container component managing merge
  workflow state and routing between steps
- **PatientSelectionStep**: Step 1 component with two patient search slots and
  selection cards
- **PrimarySelectionStep**: Step 2 component with radio button selection and
  expandable patient details accordions
- **ConfirmationStep**: Step 3 component with merge summary, conflict detection,
  reason input, confirmation checkbox
- **SuccessStep**: Step 4 component with success message and navigation options
- **PatientMergeCard**: Reusable component displaying selected patient
  demographics in Carbon Tile with StructuredList
- **PatientDetailAccordion**: Reusable component showing patient demographics,
  clinical summary, and identifiers in expandable accordions
- **MergeNotificationBanner**: Reusable component that listens for patient merge
  events and displays warning notification

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Global Administrators can complete entire merge workflow
  (selection to confirmation) in under 3 minutes for straightforward merges
- **SC-002**: System successfully prevents 100% of invalid merge attempts (same
  patient, missing permission, already merged) with clear error messages before
  reaching backend
- **SC-003**: Real-time notifications appear within 2 seconds of merge
  completion for users viewing affected patient records
- **SC-004**: All UI components pass WCAG 2.1 AA accessibility validation
  (Carbon components provide this by default)
- **SC-005**: 100% of UI strings are internationalized with React Intl (zero
  hardcoded text in JSX)
- **SC-006**: Cypress E2E tests cover complete merge workflow with 90%+ critical
  path coverage
- **SC-007**: Page remains responsive (interactions respond within 200ms) even
  with 500+ results data summary
- **SC-008**: Error recovery is clear: 90% of users can resolve validation
  errors without support based on error messages
- **SC-009**: Confirmation step clearly displays all merge impacts: 100% of
  admins understand what data will be consolidated before confirming
- **SC-010**: Navigation between workflow steps maintains state: users can go
  back and forward without losing selections

## Dependencies

- Backend API endpoints: `GET /api/patient/merge-details/{id}`,
  `POST /api/patient/merge/validate`, `POST /api/patient/merge/execute`
- Existing patient search modal component (must be reusable)
- WebSocket notification infrastructure (or fallback to polling mechanism)
- Global Administrator permission system and user context/session
- Carbon Design System (@carbon/react) version compatible with project
- React Intl internationalization infrastructure and locale files
- CSRF token management system

## Assumptions

- Backend API endpoints follow specification and return proper HTTP status codes
  and error messages
- Existing patient search modal is componentized and can be reused with callback
  for patient selection
- User permission information is available in React context or session and
  includes Global Administrator role check
- Carbon Design System is already configured in the project with proper theme
- React Intl is already set up with locale files structure (`lang/en-US.json`,
  etc.)
- WebSocket or polling infrastructure already exists in application for
  real-time features
- CSRF token is available from session/context and automatically included in API
  calls via axios/fetch interceptor
- Patient detail page route exists and accepts patient ID as parameter for
  navigation

## Out of Scope (Backend Responsibility)

- Database schema changes and Liquibase migrations
- Patient merge service layer and transaction management
- FHIR resource updates and synchronization
- Audit trail creation and logging
- Data validation and conflict detection logic
- Permission enforcement at API level
- Merge execution and rollback handling
- Data consolidation across related tables
