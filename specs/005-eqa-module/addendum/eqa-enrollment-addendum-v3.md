# OpenELIS Global EQA Module — Addendum
## EQA Navigation Restructure, Self-Enrollment, Program Enrollment & Provider Management

**Parent Document:** OpenELIS Global External Quality Assurance (EQA) Module FRS v1.0  
**Addendum Version:** 3.0  
**Date:** February 13, 2026  

---

## 1. Overview

This addendum restructures the EQA module navigation and extends the FRS with new capabilities. The EQA functionality is organized into two parent navigation items with sidebar sub-items reflecting the two primary EQA workflows, plus a standalone Alerts module:

1. **EQA Tests** (top-level sidebar parent) — The laboratory's participation view.
   - **Orders** — A listing of all EQA test orders the lab has entered, with an "Enter New EQA Test" button.
   - **My Programs** — Self-enrollment in EQA programs with optional lab unit and test/panel mapping. These enrollments populate the EQA Program dropdown in Order Entry.

2. **EQA Management** (top-level sidebar parent) — The provider/coordinator view for managing EQA programs as a distributing laboratory.
   - **Programs** — Create and manage EQA programs with provider attribution.
   - **Participants** — Enroll other organizations in programs the local lab distributes.
   - **Distributions** — EQA distribution creation and tracking (per parent FRS).
   - **Results & Analysis** — Results collection and statistical analysis (per parent FRS).

3. **Alerts** (top-level sidebar item) — A standalone alerts dashboard covering EQA deadlines, STAT orders, critical results, QC failures, and sample expirations.

### Navigation Structure

```
Sidebar (top-level items):
├── Home
├── Order
│   ├── Order Entry
│   ├── Order Search
│   ├── Batch Order Entry
│   └── Electronic Orders
├── Results
│   ├── Results Entry
│   ├── Validation
│   └── Result Search
├── Microbiology
│   ├── Dashboard
│   ├── Pending Cultures
│   └── AST Worklist
├── Patient
├── Sample
├── EQA Tests                  ← Parent
│   ├── Orders                 ← EQA order listing (participation)
│   └── My Programs            ← Self-enrollment with test/panel mapping
├── EQA Management             ← Parent
│   ├── Programs               ← Program CRUD (provider role)
│   ├── Participants           ← Organization enrollment
│   ├── Distributions          ← Sample distribution tracking
│   └── Results & Analysis     ← Statistical analysis
├── Inventory
├── Quality Control
├── Alerts                     ← Standalone top-level
├── Reports
│   ├── Routine Reports
│   ├── Study Reports
│   ├── WHONET Export
│   └── Antibiogram
└── Administration
    ├── Test Management
    ├── AMR Configuration
    ├── Site Information
    ├── User Management
    └── Dictionaries
```

---

## 2. Business Rules

### BR-012: EQA Provider Attribution
**Business Rule:** EQA Providers are stored as a text attribute on the EQA Program record rather than as a separate entity.
- The `provider` field on the EQA Program record is a free-text field with typeahead suggestions from previously entered provider names.
- Provider names must be non-empty when a program is created.

### BR-013: EQA Program–Organization Enrollment (Provider Side)
**Business Rule:** Participant enrollment links an Organization record to an EQA Program the local lab distributes.
- Any organization in the Organizations table may be enrolled as a participant in one or more EQA programs.
- An organization cannot be enrolled in the same program more than once concurrently (unique constraint on `organization_id` + `eqa_program_id` where status = Active).
- Enrollment records carry a status lifecycle: Active → Suspended → Withdrawn (or Active → Withdrawn).
- Only users with the EQA Coordinator or Administrator role may manage enrollments (per BR-010).

### BR-014: Organization Eligibility
**Business Rule:** All active organizations in the Organizations table are eligible for EQA enrollment.
- The participant list displays organizations from the existing `organization` table.
- No new organization entity is created; the EQA module references existing Organization IDs.

### BR-015: Enter New EQA Test Context Passing
**Business Rule:** When the "Enter New EQA Test" action is triggered:
- The Order Entry workflow shall open with the "This is an EQA Order" checkbox pre-checked on Step 1 (Patient Info).
- If the user triggers the action from EQA Management → Programs for a specific program, the EQA Program dropdown on Step 2 shall be pre-selected.
- If the user triggers from the EQA Tests → Orders page (no program context), the dropdown remains unselected.
- If the enrolled program has test/panel mappings, those shall be pre-populated on Step 3 (Add Sample) but can be overridden.
- The user may override any pre-populated values during order entry.

### BR-016: EQA Tests Order Filtering
**Business Rule:** The EQA Tests → Orders page displays orders where `is_eqa_sample = true`.
- The list shows all EQA orders associated with the local laboratory.
- Orders are sorted by deadline (nearest first) by default.
- Completed orders remain visible with a "Completed" status.

### BR-017: Alerts Module Independence
**Business Rule:** The Alerts dashboard operates as a standalone module, not scoped to EQA.
- Alerts aggregates notifications from all laboratory subsystems: EQA deadlines, STAT orders, critical results, sample expirations, QC failures, and any future alert types.
- The Alerts sidebar icon shall display an unread/critical count badge when critical alerts exist.

### BR-018: Self-Enrollment in EQA Programs
**Business Rule:** The local laboratory may enroll itself in external EQA programs via EQA Tests → My Programs.
- Self-enrollment creates a local record of the EQA program with program name, provider, and optional test/panel mapping.
- Programs enrolled via My Programs populate the EQA Program dropdown in Order Entry.
- Each enrollment may optionally specify:
  - One or more **lab units** (test sections) that participate in the program.
  - One or more **tests or panels** the lab will perform for that program.
- When specified, these mappings pre-populate the Order Entry form fields (lab unit selector, test/panel selector) but can be overridden by the user at order time.
- If no tests/panels are mapped, Order Entry presents the standard test selection interface.
- Self-enrollment is separate from the provider-side enrollment managed in EQA Management → Participants.

### BR-019: Test/Panel Mapping Scope
**Business Rule:** Test and panel mappings on self-enrollment are drawn from the existing Test Catalog.
- Lab units are drawn from the existing Test Sections / Lab Units table.
- Tests are drawn from the active tests in the Test Catalog.
- Panels are drawn from the active panels in the Panel Catalog.
- Mappings are optional; an enrollment with no test/panel mapping is valid.
- Multiple tests and panels may be mapped to a single enrollment.

---

## 3. Functional Requirements

### 3.1 EQA Tests → Orders (FR-010)

The Orders sub-page is the laboratory's view of its own EQA test orders.

#### FR-010.1: Orders List View
- Display all EQA orders in a DataTable with columns: Lab Number, EQA Program, Provider, Status, Deadline, Priority, Date Entered, Actions.
- Status values: Pending, In Progress, Completed, Overdue.
- Support sorting by any column; default sort by deadline ascending.
- Overdue orders shall display a warning icon and red highlight on the deadline column.

#### FR-010.2: Orders Search and Filtering
- Search bar supporting Lab Number and Program Name text search.
- Filter dropdowns: Status, Program (populated from My Programs enrollments), Priority, Date Range (From/To).

#### FR-010.3: Enter New EQA Test Button
- A primary action button labeled "Enter New EQA Test" in the page header, right-aligned.
- Clicking navigates to Order Entry with `isEQA=true` (per BR-015).

#### FR-010.4: Summary Tiles
- Top row: Pending, In Progress, Overdue (red indicator), Completed This Month.

#### FR-010.5: Row Actions
- Overflow menu per row: View Order, Enter Results (disabled if Completed), View Results (enabled only if Completed).

### 3.2 EQA Tests → My Programs (FR-013)

The My Programs sub-page manages the local lab's self-enrollment in external EQA programs.

#### FR-013.1: My Programs List View
- Display enrolled programs in a DataTable with columns: Program Name, Provider, Lab Units (tag list), Tests/Panels (tag list), Status, Actions.
- Status values: Active, Inactive.
- Search bar for program name or provider.

#### FR-013.2: Enroll in Program Button
- A primary action button labeled "Enroll in Program" in the toolbar, right-aligned.
- Opens an enrollment form (modal or inline).

#### FR-013.3: Enrollment Form Fields
- **Program Name** (text, required) — Name of the external EQA program.
- **Provider** (text with typeahead from existing provider values, required) — The organization running the program.
- **Description** (textarea, optional) — Notes about the program.
- **Lab Unit(s)** (multi-select from Lab Units / Test Sections table, optional) — Which lab sections participate.
- **Tests** (multi-select from Test Catalog, optional) — Individual tests this lab will run for the program.
- **Panels** (multi-select from Panel Catalog, optional) — Panels this lab will run for the program.
- **Active** (toggle, default: active).

#### FR-013.4: Edit and Deactivate
- Edit enrollment via row action → re-opens the enrollment form pre-populated.
- Deactivate removes the program from Order Entry dropdowns but preserves historical records.
- Reactivate restores the program to Order Entry dropdowns.

#### FR-013.5: Order Entry Integration
- Active programs from My Programs populate the EQA Program dropdown in Order Entry Step 2.
- If a program has mapped lab units, the lab unit selector in Order Entry is pre-filtered.
- If a program has mapped tests/panels, those are pre-selected in the test selection step but can be added to or removed by the user.

### 3.3 EQA Management (FR-011)

The EQA Management section is the provider/coordinator view. Each tab is a sidebar sub-item.

#### FR-011.1: Programs Sub-Item
- Display all EQA Programs in a DataTable with columns: Program Name, Provider, Enrolled Participants (count), Status, Actions.
- Search/filter by name, provider, status.
- "Add Program" button opens creation modal.
- Program form fields: Name (required), Provider (typeahead, required), Description (optional), Active toggle.
- Programs created here are for distribution management; they are distinct from My Programs self-enrollments.

#### FR-011.2: Participants Sub-Item
- Program selector dropdown at top.
- Display enrolled organizations in a DataTable: Organization Name, Code, District, Enrollment Date, Status, Actions.
- "Enroll Participant" button opens modal with searchable multi-select of Organizations table.
- Local lab marked with "(This Lab)" tag.
- Status management: Suspend, Withdraw (with reason), Reactivate.

#### FR-011.3: Distributions Sub-Item
- Per existing FR-004, FR-005 from parent FRS.

#### FR-011.4: Results & Analysis Sub-Item
- Per existing FR-006, FR-007 from parent FRS.

### 3.4 Alerts Dashboard (FR-012)

#### FR-012.1: Alerts Navigation
- Standalone top-level sidebar item with AlertCircle icon and critical count badge.

#### FR-012.2: Alerts Content
- Per existing FR-009 and FR-010 from the parent FRS — no functional changes, only navigation placement.
- Shows all alert types: EQA Deadlines, STAT Orders, Critical Results, Sample Expiry, QC Failures.

---

## 4. User Stories and Acceptance Criteria

### US-010: View EQA Test Orders
**As a** lab technician, **I want to** see all EQA test orders assigned to my laboratory **so that** I can track what needs to be completed and by when.

**Acceptance Criteria:**
- Navigate to EQA Tests → Orders in the sidebar
- DataTable shows all EQA orders with status, deadline, priority
- Sorted by deadline (nearest first) by default
- Overdue orders visually flagged
- Filter by status, program, priority, date range

### US-011: Enter New EQA Test
**As a** lab technician, **I want to** quickly enter a new EQA test order **so that** I can register incoming proficiency testing samples.

**Acceptance Criteria:**
- Click "Enter New EQA Test" on the Orders page
- Order Entry opens with "This is an EQA Order" pre-checked
- EQA Program dropdown populated from My Programs
- If enrolled program has test/panel mappings, those are pre-selected but overridable

### US-012: Enroll in External EQA Program
**As a** lab manager, **I want to** enroll my laboratory in an external EQA program and specify which tests we participate in **so that** the program appears in Order Entry with correct pre-populated tests.

**Acceptance Criteria:**
- Navigate to EQA Tests → My Programs
- Click "Enroll in Program"
- Enter program name, provider (typeahead), optional description
- Optionally select lab units, tests, and panels from existing catalog
- Program appears in Order Entry EQA Program dropdown
- When selected in Order Entry, mapped tests/panels are pre-selected

### US-013: Manage EQA Programs as Provider
**As an** EQA coordinator, **I want to** create and manage EQA programs my lab distributes **so that** I can enroll participants and track distributions.

**Acceptance Criteria:**
- Navigate to EQA Management → Programs
- Create program with name, provider (typeahead), description, active toggle
- Program appears in EQA Management context

### US-014: Enroll Facilities in EQA Program
**As an** EQA coordinator, **I want to** enroll organizations from the Organizations table into a program **so that** I can manage participation.

**Acceptance Criteria:**
- Navigate to EQA Management → Participants, select a program
- "Enroll Participant" opens searchable multi-select of Organizations table
- Already-enrolled organizations marked/disabled
- Local lab marked with "(This Lab)" tag
- Multi-select and confirm enrollment

### US-015: Manage Participant Enrollment Status
**As an** EQA coordinator, **I want to** suspend or withdraw a facility **so that** I can manage participation over time.

**Acceptance Criteria:**
- Suspend/Withdraw/Reactivate via row overflow menu
- Withdrawal shows confirmation with optional reason
- Status changes recorded in audit trail

### US-016: Access Alerts
**As a** laboratory supervisor, **I want to** access alerts from the sidebar **so that** I can monitor deadlines and critical notifications.

**Acceptance Criteria:**
- "Alerts" is a standalone sidebar item with badge count
- Shows all alert types (EQA, STAT, critical results, QC, expiry)
- Filter by type, acknowledge individual alerts

---

## 5. User Interface Specifications

### UI-008: EQA Tests → Orders

**Page Header:**
- Breadcrumb: Home / EQA Tests / Orders
- Title: "EQA Orders"
- Subtitle: "Proficiency testing orders for this laboratory"
- Primary button: "Enter New EQA Test" (Plus icon, right-aligned)

**Summary Tiles:** Pending (blue), In Progress (teal), Overdue (red), Completed This Month (green)

**DataTable Columns:** Lab Number (monospace, clickable), EQA Program, Provider, Status (Badge), Deadline (red + icon if overdue), Priority (Badge), Date Entered, Actions (overflow)

### UI-009: EQA Tests → My Programs

**Page Header:**
- Breadcrumb: Home / EQA Tests / My Programs
- Title: "My EQA Programs"
- Subtitle: "Programs this laboratory participates in"
- Primary button: "Enroll in Program" (Plus icon, right-aligned)

**DataTable Columns:**
- Program Name (with description subtitle)
- Provider
- Lab Units (tag list, e.g., "Chemistry", "Hematology")
- Tests/Panels (tag list, e.g., "Glucose", "CBC Panel"; shows count if >3 with "+N more" overflow)
- Status (Active/Inactive badge)
- Actions (Edit, Deactivate/Reactivate, Remove)

**Enrollment Modal (540px width):**
- Program Name (text input, required)
- Provider (text input with typeahead, required)
- Description (textarea, optional)
- Divider: "Test Mapping (Optional)"
- Lab Unit(s) (multi-select dropdown from Lab Units table)
- Tests (multi-select with search from Test Catalog)
- Panels (multi-select with search from Panel Catalog)
- Active toggle
- Footer: Cancel, Save

### UI-010: EQA Management → Programs

**Page Header:**
- Breadcrumb: Home / EQA Management / Programs
- Title: "EQA Programs"
- Subtitle: "Manage programs distributed by this laboratory"
- Primary button: "Add Program" (Plus icon)

**Content:** Same as prior version — DataTable with CRUD, provider typeahead modal.

### UI-011: EQA Management → Participants

**Content:** Same as prior version — Program selector, enrollment DataTable, enroll modal, withdraw modal.

### UI-012: Alerts

**Sidebar Item:** AlertCircle icon, "Alerts" label, red badge with critical unacknowledged count.

**Content:** Summary tiles (Critical, Warnings, Total Active), filter dropdown, alert cards with severity borders and acknowledge buttons.

---

## 6. Data Model Requirements

### DM-002 Extension: EQA Program Entity

Add `provider` field:

| Field | Type | Description |
|-------|------|-------------|
| `provider` | varchar(255), NOT NULL | EQA Provider name |

### DM-007: EQA Program Enrollment — Provider Side (New)

`eqa_program_enrollment` — tracks organizations enrolled by the local lab as a provider:

| Field | Type | Description |
|-------|------|-------------|
| `id` | PK | Primary key |
| `eqa_program_id` | FK → eqa_program.id, NOT NULL | Program being distributed |
| `organization_id` | FK → organization.id, NOT NULL | Participating organization |
| `enrollment_date` | datetime, NOT NULL | Enrollment timestamp |
| `status` | enum('Active','Suspended','Withdrawn'), default 'Active' | Lifecycle status |
| `status_changed_date` | datetime | Last status change |
| `status_changed_by` | FK → system_user.id | Who changed status |
| `withdrawal_reason` | text, nullable | Reason if withdrawn |
| `created_by` | FK → system_user.id | Creator |

**Constraints:** Unique on (`eqa_program_id`, `organization_id`) WHERE `status` = 'Active'

### DM-008: EQA Self-Enrollment — Participation Side (New)

`eqa_lab_program_enrollment` — tracks the local lab's own enrollment in external programs:

| Field | Type | Description |
|-------|------|-------------|
| `id` | PK | Primary key |
| `program_name` | varchar(255), NOT NULL | External program name |
| `provider` | varchar(255), NOT NULL | Provider organization name |
| `description` | text, nullable | Notes |
| `is_active` | boolean, default true | Active status |
| `created_date` | datetime, NOT NULL | Record creation |
| `created_by` | FK → system_user.id | Creator |
| `last_modified` | datetime | Last edit |

### DM-009: Self-Enrollment Lab Unit Mapping (New)

`eqa_lab_enrollment_lab_unit` — maps self-enrollment to lab units:

| Field | Type | Description |
|-------|------|-------------|
| `id` | PK | Primary key |
| `enrollment_id` | FK → eqa_lab_program_enrollment.id, NOT NULL | Parent enrollment |
| `test_section_id` | FK → test_section.id, NOT NULL | Lab unit / test section |

### DM-010: Self-Enrollment Test/Panel Mapping (New)

`eqa_lab_enrollment_test_map` — maps self-enrollment to tests and panels:

| Field | Type | Description |
|-------|------|-------------|
| `id` | PK | Primary key |
| `enrollment_id` | FK → eqa_lab_program_enrollment.id, NOT NULL | Parent enrollment |
| `test_id` | FK → test.id, nullable | Mapped test (null if panel) |
| `panel_id` | FK → panel.id, nullable | Mapped panel (null if test) |

**Constraint:** CHECK (`test_id` IS NOT NULL OR `panel_id` IS NOT NULL)

---

## 7. API Endpoints

### EQA Tests — Orders
```
GET    /api/eqa/orders
       → EQA orders for local lab. Supports: ?status, ?programId, ?priority, ?from, ?to, ?search
GET    /api/eqa/orders/summary
       → Counts: pending, inProgress, overdue, completedThisMonth
```

### EQA Tests — My Programs (Self-Enrollment)
```
GET    /api/eqa/my-programs
       → All self-enrollment records for local lab
GET    /api/eqa/my-programs/{id}
POST   /api/eqa/my-programs
       → Body: { programName, provider, description, isActive, labUnitIds[], testIds[], panelIds[] }
PUT    /api/eqa/my-programs/{id}
       → Same body as POST
DELETE /api/eqa/my-programs/{id}
       → Soft delete (sets is_active=false)
GET    /api/eqa/providers
       → Distinct provider names for typeahead (union of my-programs + managed programs)
```

### EQA Management — Programs
```
GET    /api/eqa/programs
GET    /api/eqa/programs/{id}
POST   /api/eqa/programs
PUT    /api/eqa/programs/{id}
```

### EQA Management — Enrollments
```
GET    /api/eqa/programs/{programId}/enrollments
POST   /api/eqa/programs/{programId}/enrollments
       → Body: { organizationIds: [id1, id2, ...] }
PUT    /api/eqa/programs/{programId}/enrollments/{enrollmentId}
       → Body: { status, reason }
GET    /api/eqa/eligible-organizations?programId={id}&search={term}
```

---

## 8. Localization Tags

| Tag | Default (English) |
|-----|-------------------|
| `eqa.tests.orders.title` | EQA Orders |
| `eqa.tests.orders.subtitle` | Proficiency testing orders for this laboratory |
| `eqa.tests.enterNew` | Enter New EQA Test |
| `eqa.tests.searchPlaceholder` | Search by lab number or program... |
| `eqa.tests.labNumber` | Lab Number |
| `eqa.tests.program` | EQA Program |
| `eqa.tests.provider` | Provider |
| `eqa.tests.status` | Status |
| `eqa.tests.deadline` | Deadline |
| `eqa.tests.priority` | Priority |
| `eqa.tests.dateEntered` | Date Entered |
| `eqa.tests.status.pending` | Pending |
| `eqa.tests.status.inProgress` | In Progress |
| `eqa.tests.status.completed` | Completed |
| `eqa.tests.status.overdue` | Overdue |
| `eqa.tests.summary.pending` | Pending |
| `eqa.tests.summary.inProgress` | In Progress |
| `eqa.tests.summary.overdue` | Overdue |
| `eqa.tests.summary.completedThisMonth` | Completed This Month |
| `eqa.tests.action.viewOrder` | View Order |
| `eqa.tests.action.enterResults` | Enter Results |
| `eqa.tests.action.viewResults` | View Results |
| `eqa.myPrograms.title` | My EQA Programs |
| `eqa.myPrograms.subtitle` | Programs this laboratory participates in |
| `eqa.myPrograms.enrollButton` | Enroll in Program |
| `eqa.myPrograms.programName` | Program Name |
| `eqa.myPrograms.provider` | Provider |
| `eqa.myPrograms.description` | Description |
| `eqa.myPrograms.labUnits` | Lab Unit(s) |
| `eqa.myPrograms.tests` | Tests |
| `eqa.myPrograms.panels` | Panels |
| `eqa.myPrograms.testMapping` | Test Mapping (Optional) |
| `eqa.myPrograms.testMappingHint` | Select tests and panels to pre-populate Order Entry when this program is selected |
| `eqa.myPrograms.status` | Status |
| `eqa.myPrograms.status.active` | Active |
| `eqa.myPrograms.status.inactive` | Inactive |
| `eqa.myPrograms.action.edit` | Edit |
| `eqa.myPrograms.action.deactivate` | Deactivate |
| `eqa.myPrograms.action.reactivate` | Reactivate |
| `eqa.management.programs.title` | EQA Programs |
| `eqa.management.programs.subtitle` | Manage programs distributed by this laboratory |
| `eqa.management.participants.title` | Participants |
| `eqa.management.participants.subtitle` | Manage organization enrollment in EQA programs |
| `eqa.management.distributions.title` | Distributions |
| `eqa.management.results.title` | Results & Analysis |
| `eqa.program.addProgram` | Add Program |
| `eqa.program.editTitle` | Edit EQA Program |
| `eqa.program.createTitle` | Create EQA Program |
| `eqa.program.name` | Program Name |
| `eqa.program.provider` | Provider |
| `eqa.program.description` | Description |
| `eqa.program.participantCount` | Enrolled Participants |
| `eqa.enrollment.enrollParticipant` | Enroll Participant |
| `eqa.enrollment.organizationName` | Organization Name |
| `eqa.enrollment.organizationCode` | Code |
| `eqa.enrollment.district` | District |
| `eqa.enrollment.enrollmentDate` | Enrollment Date |
| `eqa.enrollment.status` | Status |
| `eqa.enrollment.suspend` | Suspend |
| `eqa.enrollment.withdraw` | Withdraw |
| `eqa.enrollment.reactivate` | Reactivate |
| `eqa.enrollment.withdrawReason` | Reason for Withdrawal |
| `eqa.enrollment.confirmWithdraw` | Are you sure you want to withdraw this organization? |
| `eqa.enrollment.enrollSelected` | Enroll Selected ({count}) |
| `eqa.enrollment.alreadyEnrolled` | Already Enrolled |
| `eqa.enrollment.thisLab` | This Lab |
| `eqa.enrollment.selectProgram` | Select EQA Program |
| `alerts.title` | Alerts |
| `alerts.subtitle` | Laboratory-wide alerts and notifications |
| `button.save` | Save |
| `button.cancel` | Cancel |
| `column.actions` | Actions |

---

## 9. Acceptance Criteria Summary

| ID | Requirement | Criteria |
|----|-------------|----------|
| AC-010.1 | EQA Tests → Orders page | Sidebar sub-item navigates to EQA order listing |
| AC-010.2 | Orders list | DataTable with status, deadline, priority, sorted by deadline |
| AC-010.3 | Summary tiles | Four tiles with correct counts |
| AC-010.4 | Enter New EQA Test | Button navigates to Order Entry with EQA pre-checked |
| AC-010.5 | Filters | Search and filter by status, program, priority, date range |
| AC-013.1 | My Programs page | Sidebar sub-item navigates to self-enrollment listing |
| AC-013.2 | Enroll in Program | Modal with program name, provider typeahead, optional test/panel mapping |
| AC-013.3 | Lab unit mapping | Multi-select from existing Lab Units table |
| AC-013.4 | Test/panel mapping | Multi-select from Test Catalog and Panel Catalog |
| AC-013.5 | Order Entry integration | Enrolled programs appear in dropdown; mapped tests pre-populate |
| AC-013.6 | Override | User can override pre-populated tests/panels at order time |
| AC-011.1 | EQA Management → Programs | Sidebar sub-item, DataTable with CRUD, provider typeahead |
| AC-011.2 | EQA Management → Participants | Program selector, enrollment DataTable, multi-select enroll modal |
| AC-011.3 | Participant status | Suspend/Withdraw/Reactivate with audit trail |
| AC-012.1 | Alerts standalone | Top-level sidebar item with badge |
| AC-012.2 | Alerts content | All alert types, filter, acknowledge |

---

## 10. Implementation Notes

### Navigation Changes
- EQA Tests and EQA Management are parent items with sidebar sub-items (same pattern as Order, Results, Microbiology, Reports, Administration).
- Alerts is a standalone item placed after Quality Control, before Reports.
- Alerts sidebar icon renders a dynamic badge from `/api/alerts/summary`.

### Self-Enrollment vs. Provider Enrollment
- **My Programs** (self-enrollment) creates `eqa_lab_program_enrollment` records — the lab enrolling itself in external programs.
- **Participants** (provider enrollment) creates `eqa_program_enrollment` records — the lab enrolling other organizations in programs it manages.
- These are distinct tables with distinct UIs. The Order Entry dropdown is populated from `eqa_lab_program_enrollment` (active records).

### Test/Panel Mapping
- Mappings are stored in `eqa_lab_enrollment_test_map` as a join table with nullable `test_id` / `panel_id`.
- When Order Entry loads with a selected EQA program, it queries the mapping table to pre-populate the test/panel selection.
- Pre-population is a convenience; users can always modify selections.

### Phase Alignment
- **EQA Tests (Orders + My Programs)** — Phase 1: Core EQA Reception
- **EQA Management (Programs, Participants)** — Phase 1 (setup) + Phase 3 (Distributions)
- **Results & Analysis** — Phase 4
- **Alerts** — Phase 2: Alerts and Deadline Management

---

© OpenELIS Global Community
