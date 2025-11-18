# Figma Make Mockup Analysis - EQA Module

**Date**: 2025-11-17
**Figma Make File**: https://www.figma.com/make/k5cOys9xJEbanitKIlEC61/EQA-Sample-Management-System
**Spec File**: [spec.md](spec.md)

## Executive Summary

The Figma Make mockup provides a **comprehensive React/TypeScript prototype** using Shadcn/ui components (NOT Carbon Design System). This analysis compares the mockup against the OpenELIS Global 3.0 EQA specification to identify alignment gaps and implementation guidance.

**Critical Finding**: The mockup uses **Shadcn/ui + Tailwind CSS**, which **violates CR-001** requiring Carbon Design System (@carbon/react). The mockup serves as a **UX/workflow reference** but components must be rebuilt using Carbon.

---

## 1. Architecture Comparison

### Mockup Technology Stack
```typescript
// From App.tsx and components
- Framework: React + TypeScript
- UI Library: Shadcn/ui (Radix UI primitives)
- Styling: Tailwind CSS
- Icons: Lucide React
- State: React Hooks (useState)
- Date Handling: date-fns
```

### OpenELIS Global 3.0 Required Stack (CR-001, CR-002, CR-003)
```
- Framework: React + TypeScript ✅
- UI Library: Carbon Design System (@carbon/react) ❌ (Mockup uses Shadcn)
- Styling: Carbon styles (NO Tailwind) ❌
- Icons: Carbon icons ❌ (Mockup uses Lucide)
- i18n: React Intl ❌ (Mockup has hardcoded strings)
- Backend: Spring Boot + JPA/Hibernate (not in mockup)
- Database: PostgreSQL + Liquibase (not in mockup)
```

**Verdict**: Mockup is **front-end only** and requires **complete component migration** to Carbon Design System.

---

## 2. User Workflows - Detailed Comparison

### 2.1 EQA Sample Entry (User Story 1 - P1)

#### Mockup Implementation (`EQASampleEntry.tsx`)
**4-Tab Workflow**:
1. **Tab 1: Sample Info** → Checkbox "This is an EQA sample", accession number, sample type, received date, priority
2. **Tab 2: EQA Details** → Provider, program, shipment ID, sample ID, deadline, participant ID
3. **Tab 3: Tests** → Lab section, test checkboxes
4. **Tab 4: Review** → Summary cards with all details

**Key Features**:
- ✅ EQA checkbox on first tab (disables patient fields conceptually)
- ✅ Priority field: STAT/URGENT/ROUTINE (matches spec)
- ✅ EQA-specific fields: provider, program, sample ID, deadline, participant ID
- ✅ Visual EQA badge throughout workflow
- ✅ Tab progression validation (can't proceed without required fields)
- ✅ Success confirmation message

#### Spec Requirements (FR-001 to FR-007)
| Requirement | Mockup Status | Notes |
|-------------|---------------|-------|
| FR-001: EQA checkbox on patient tab | ✅ Implemented | Tab 1 has checkbox |
| FR-002: Auto-disable patient demographics | ⚠️ Not shown | Mockup doesn't show patient fields to disable |
| FR-003: EQA fields (provider org, program, sample ID, participant ID, deadline, priority) | ✅ All present | Tab 2 has all fields |
| FR-004: Prevent registration without EQA program | ✅ Validation | Can't proceed to Tab 3 without required fields |
| FR-005: EQA badge in work queues | ✅ Badge shown | Blue badge with flask icon |
| FR-006: Filter work queues for EQA samples | ✅ Implemented | WorkQueue.tsx has "EQA Samples" filter |
| FR-007: Prevent result modification after deadline | ❌ Not shown | Not in mockup |

**Gap**: Patient demographics tab not shown (mockup starts at "Sample Info" tab). Spec requires **4-tab structure** with Patient/Program/Sample/Order, but mockup has Sample Info/EQA Details/Tests/Review.

**Recommendation**:
- Rename mockup tabs to match existing OpenELIS structure:
  - Tab 1: **Patient** (with EQA checkbox disabling demographics)
  - Tab 2: **Program** (EQA fields)
  - Tab 3: **Sample** (sample type, received date, tests)
  - Tab 4: **Order** (review and submit)

---

### 2.2 Alerts Dashboard (User Story 2 - P1, User Story 6 - P2)

#### Mockup Implementation (`AlertsDashboard.tsx`)

**Dashboard Layout**:
```typescript
// Summary Tiles (4 cards)
- EQA Deadlines: Count + critical count
- STAT Orders: Pending count
- Critical Results: Review count
- Sample Expirations: Expiring count

// Alerts Table
- Columns: Type, Alert, Accession #, Severity, Time, Status, Actions
- Filters: Tabs for All/Deadlines/STAT/Critical/Expirations
- EQA Badge: Visual indicator for EQA samples
- Action Buttons: View, Acknowledge (for critical), Dismiss
```

**Alert Data Model**:
```typescript
interface Alert {
  type: "deadline" | "stat" | "critical" | "expiration"
  severity: "high" | "medium" | "low"
  title: string
  message: string
  accessionNumber: string
  timestamp: string
  daysRemaining?: number
  isEQA?: boolean
  eqaProvider?: string
  acknowledged?: boolean
  acknowledgedBy?: string
  acknowledgedAt?: string
  resolutionText?: string
}
```

#### Spec Requirements (FR-008 to FR-047)

| Requirement | Mockup Status | Notes |
|-------------|---------------|-------|
| FR-008 to FR-015: Alert timing (72h/24h/4h) | ✅ Data model supports | Mock data shows days remaining |
| FR-016 to FR-023: STAT order processing | ✅ Implemented | STAT alerts with time tracking |
| FR-024 to FR-026: Priority levels | ⚠️ Different model | Spec: Standard/Urgent/Critical; Mockup: STAT/URGENT/ROUTINE |
| FR-027 to FR-035: Alerts dashboard | ✅ Comprehensive | Summary tiles + table + filters |
| FR-036: Alert table columns | ✅ All present | Type, alert, accession, severity, time, status, actions |
| FR-037: "My Alerts" filter | ❌ Missing | Not implemented in mockup |
| FR-038: Search by lab number/type/assignment | ❌ Missing | No search bar |
| FR-039: Filter by type/severity/lab section | ⚠️ Partial | Type filters exist, severity/section missing |
| FR-040: Color coding (Red/Orange/Yellow/Blue) | ✅ Implemented | `destructive`, `orange`, `secondary` variants |
| FR-041: Sample expiration alert escalation | ✅ Data model | 7d→2d→1d progression supported |
| FR-042: STAT order alert escalation | ✅ Data model | 50%→75%→100% progression supported |
| FR-043: Resolution comments for critical | ✅ Modal dialog | `<Textarea>` for resolution text |
| FR-044: Audit trail (user ID + timestamp) | ✅ Captured | `acknowledgedBy`, `acknowledgedAt` fields |
| FR-045: Escalation after 4h to supervisor | ❌ Not shown | Backend logic, not in UI mockup |
| FR-046: Auto-refresh every 60s | ❌ Not implemented | No polling logic |
| FR-047: Pagination (25/50/100/200) | ❌ Missing | No pagination controls |

**Gaps**:
1. Missing "My Alerts" checkbox filter (FR-037)
2. No search bar (FR-038)
3. Missing lab section and severity dropdown filters (FR-039 partial)
4. No pagination (FR-047)
5. No auto-refresh mechanism (FR-046)

**Recommendation**: Add Carbon DataTable with:
- Search component
- Filter dropdowns (severity, lab section)
- "My Alerts" toggle
- Pagination component
- `useEffect` with 60s interval for auto-refresh

---

### 2.3 EQA Distribution (User Story 4 - P2)

#### Mockup Implementation (`EQADistribution.tsx`)

**3-Step Creation Workflow** (in modal dialog):
```typescript
// Step 1: Shipment Details
- Program dropdown
- Round/Cycle text
- Shipment ID (auto-generated)
- Result Deadline datepicker

// Step 2: Participants
- Table with checkboxes
- Columns: Lab Name, Code, Country, Status (Enrolled/Not Enrolled)
- Selected count badge

// Step 3: Samples
- Number of samples input
- Sample type dropdown
- Expected results text
```

**Shipment Tracking Table**:
```typescript
interface Shipment {
  shipmentId: string
  program: string
  round: string
  status: "draft" | "shipped" | "received" | "completed"
  sampleCount: number
  participantCount: number
  shippedDate?: string
  deadline: string
  responsesReceived: number
}
```

#### Spec Requirements (FR-027 to FR-035)

| Requirement | Mockup Status | Notes |
|-------------|---------------|-------|
| FR-027: EQA Distribution Management screen | ✅ Dedicated page | Full dashboard with shipments table |
| FR-028: Create new distribution | ✅ Modal workflow | 3-tab creation dialog |
| FR-029: Program, round, deadline selection | ✅ All fields | Step 1 has program/round/deadline |
| FR-030: Select participating organizations | ✅ Checkbox table | Step 2 participant selection |
| FR-031: Define sample count and expected results | ✅ Inputs | Step 3 has sample count + expected results |
| FR-032: Generate sample barcodes | ❌ Not shown | No barcode generation UI |
| FR-033: Update distribution status | ⚠️ Implicit | Status badges shown but no edit UI |
| FR-034: Track participant responses | ✅ Table column | "Responses" column shows 38/45 format |
| FR-035: Generate shipping labels | ❌ Not shown | No label generation |

**Gaps**:
1. No barcode generation workflow (FR-032)
2. No shipping label generation (FR-035)
3. No integration with 002-shipment-support feature shown

**Recommendation**:
- Add "Generate Barcodes" button after Step 3 (calls existing barcode service)
- Add "Print Labels" button in shipments table (calls existing label service)
- Link to shipment tracking from 002-shipment-support

---

### 2.4 EQA Results Management (User Story 5 - P2)

#### Mockup Implementation (`EQAManagement.tsx`)

**Results Entry Dialog** (3 tabs):
```typescript
// Tab 1: Manual Entry
- Input fields for each test
- Comments textarea

// Tab 2: File Upload
- Drag-and-drop zone
- CSV/Excel file support
- File format requirements listed

// Tab 3: FHIR API
- API key input
- JSON payload textarea
- FHIR endpoint shown: POST /fhir/DiagnosticReport
```

**Sample Tracking**:
```typescript
interface EQASample {
  accessionNumber: string
  provider: string
  program: string
  sampleId: string
  status: "pending" | "in-progress" | "completed" | "submitted"
  tests: string[]
  resultsEntered: number
  totalTests: number
  daysRemaining: number
}
```

#### Spec Requirements (FR-036 to FR-044)

| Requirement | Mockup Status | Notes |
|-------------|---------------|-------|
| FR-036: Received Sample Management screen | ✅ Dedicated page | Table with all received EQA samples |
| FR-037: View sample details | ✅ Table columns | Provider, program, sample ID, deadline, progress |
| FR-038: Enter results (manual) | ✅ Tab 1 | Input per test + comments |
| FR-039: Upload results (CSV/Excel) | ✅ Tab 2 | File upload with validation |
| FR-040: Submit results via FHIR | ✅ Tab 3 | API key + JSON payload |
| FR-041: Validate results before submission | ⚠️ Not shown | No validation UI |
| FR-042: Submit to provider organization | ⚠️ Implicit | "Submit" button shown but no confirmation |
| FR-043: Track submission status | ✅ Status column | pending/in-progress/completed/submitted |
| FR-044: Receive and display feedback | ❌ Not shown | No feedback display |

**Performance Summary Card**:
- Participation Rate: 100%
- On-Time Submission: 95.8%
- Average Z-Score: 0.85

**Gaps**:
1. No validation error display (FR-041)
2. No submission confirmation workflow (FR-042)
3. No provider feedback display (FR-044)
4. No statistical analysis shown after submission (Z-scores mentioned in README but not in component)

---

### 2.5 Statistical Analysis (User Story 3 - P2)

#### Mockup: LIMITED Implementation

**From README.md** (documentation, not UI):
```markdown
## Statistics and Analysis

The system automatically calculates:
- **Mean Value**: Average of all participant results
- **Standard Deviation**: Measure of result variability
- **Z-Scores**: Performance metric for each participant
- **Performance Categories**:
  - Acceptable: |Z| ≤ 2
  - Questionable: 2 < |Z| ≤ 3
  - Unacceptable: |Z| > 3
```

**Actual UI**: Only shows "Average Z-Score: 0.85" in performance card. **NO** detailed statistical analysis UI.

#### Spec Requirements (FR-045 to FR-053)

| Requirement | Mockup Status | Notes |
|-------------|---------------|-------|
| FR-045: Statistical Analysis screen | ❌ Missing | No dedicated analysis view |
| FR-046: Select distribution for analysis | ❌ Missing | No distribution selector |
| FR-047: Calculate mean, SD, Z-scores | ⚠️ Documented | Logic in README but no UI |
| FR-048: Display participant results table | ❌ Missing | No results comparison table |
| FR-049: Color-code performance | ❌ Missing | No color coding |
| FR-050: Generate PDF report | ⚠️ Button only | "View Report" button but no generation |
| FR-051: Include charts (histograms, scatter plots) | ❌ Missing | No data visualization |
| FR-052: Export data (CSV, Excel) | ❌ Missing | No export functionality |
| FR-053: Email reports to participants | ❌ Missing | No email feature |

**MAJOR GAP**: Statistical analysis is **entirely missing** from the UI mockup. Only documented in README.

**Recommendation**: Create new page `EQAStatisticalAnalysis.tsx` with:
- Carbon DataTable for participant results
- Z-score calculation display
- Performance categorization with color coding (Carbon Tag component)
- Carbon charts (histogram, scatter plot) using `@carbon/charts-react`
- PDF export using existing report infrastructure
- CSV/Excel export buttons

---

### 2.6 EQA Program Configuration (User Story 7 - P3)

#### Mockup Implementation (`EQAProgramManager.tsx`)

**UI Not Provided**: Mockup file lists component but content is empty in navigation. **Assumed to exist** based on README.

#### Spec Requirements (FR-048 to FR-051)

| Requirement | Mockup Status | Notes |
|-------------|---------------|-------|
| FR-048: EQA Program Management screen | ⚠️ Placeholder | Component exists but not implemented |
| FR-049: Create programs (name, description, status) | ❌ Not shown | No form UI |
| FR-050: Assign tests/panels to programs | ❌ Not shown | No test assignment UI |
| FR-051: Deactivate programs | ❌ Not shown | No status toggle |

**Recommendation**: Low priority (P3). Implement after P1/P2 workflows are complete.

---

## 3. UI/UX Component Mapping

### Mockup → Carbon Design System Translation

| Mockup Component (Shadcn/ui) | Carbon Equivalent | Notes |
|------------------------------|-------------------|-------|
| `<Card>` | `<Tile>` | Carbon has ClickableTile, SelectableTile, ExpandableTile |
| `<Button>` | `<Button>` | Direct mapping, Carbon has same primary/secondary/ghost variants |
| `<Input>` | `<TextInput>` | Carbon uses TextInput component |
| `<Select>` | `<Dropdown>` or `<ComboBox>` | Carbon Dropdown for simple, ComboBox for searchable |
| `<Checkbox>` | `<Checkbox>` | Direct mapping |
| `<Textarea>` | `<TextArea>` | Direct mapping |
| `<Tabs>` | `<Tabs>` | Direct mapping, Carbon has ContentSwitcher for simple cases |
| `<Table>` | `<DataTable>` | Carbon DataTable is **much more powerful** (sorting, filtering, pagination, batch actions) |
| `<Dialog>` / `<Modal>` | `<Modal>` | Direct mapping, Carbon has ComposedModal for complex flows |
| `<Badge>` | `<Tag>` | Carbon uses Tag component with filter/read-only types |
| `<Popover>` | `<Tooltip>` or `<OverflowMenu>` | Depends on use case |
| `<Calendar>` / `<DatePicker>` | `<DatePicker>` | Carbon has DatePicker and DatePickerInput |
| `<Progress>` | `<ProgressBar>` or `<ProgressIndicator>` | ProgressBar for linear, ProgressIndicator for steps |
| `<Alert>` | `<InlineNotification>` or `<ToastNotification>` | InlineNotification for static, ToastNotification for temporary |
| Lucide Icons | Carbon Icons (`@carbon/icons-react`) | Must use Carbon icon library |
| Tailwind classes | Carbon classes/SCSS | Use Carbon design tokens and grid |

### Critical Differences

1. **DataTable**: Carbon DataTable is **FAR more feature-rich** than Shadcn Table. Provides:
   - Built-in sorting
   - Pagination
   - Batch selection
   - Row expansion
   - Custom toolbars
   - **Use DataTable for all EQA tables** (alerts, distributions, samples)

2. **Forms**: Carbon uses `<Form>` wrapper with FormGroup, FieldSet, and validation patterns. Mockup uses raw inputs.

3. **Layout**: Carbon uses 16-column grid system. Mockup uses Tailwind Flexbox/Grid. **Must convert** to Carbon Grid.

4. **Theme**: Carbon has light/dark themes with design tokens. Mockup hardcodes colors. **Use Carbon tokens** for colors/spacing.

---

## 4. Data Model Alignment

### Mockup Entities vs Spec Entities

| Spec Entity | Mockup Equivalent | Differences |
|-------------|-------------------|-------------|
| Order (extended) | `EQASample` interface | ✅ Fields match: provider, program, sample ID, participant ID, deadline, priority |
| EQAProgram | Not shown | ❌ Missing in mockup |
| EQADistribution | `Shipment` interface | ✅ Close match: shipment ID, program, round, deadline, participants |
| EQAParticipant | `Participant` interface | ✅ Match: lab name, code, country, enrolled status |
| EQAResult | Implied in forms | ⚠️ No explicit interface, only form inputs |
| DistributionParticipant | `selectedParticipants` array | ⚠️ Only IDs, not full relationship |
| Alert (Spec: implied) | `Alert` interface | ✅ Comprehensive: type, severity, EQA flag, acknowledgment fields |

### Missing from Mockup
1. **EQAProgram** entity (User Story 7)
2. **EQAResult** entity with Z-score calculations
3. **Statistical analysis** data structures

---

## 5. Integration Points

### Mockup → OpenELIS Global Integration Needs

| Mockup Feature | OpenELIS Integration | Implementation |
|----------------|----------------------|----------------|
| Sample Entry | Existing 4-tab sample entry | **Extend** existing form with EQA checkbox + Program tab fields |
| Work Queue | Existing work queue | **Add** EQA filter + badge to existing queue |
| Barcode Generation | Existing barcode service | **Call** from distribution creation |
| Shipping Labels | Existing label service | **Call** from distribution management |
| FHIR API | Existing FHIR infrastructure | **Extend** DiagnosticReport resource for EQA results |
| Organizations | Existing organizations DB | **Link** EQA provider organizations |
| User Roles | Existing RBAC | **Add** "EQA Coordinator" role |
| Alerts System | **NEW** | Build extensible alert framework (EQA + STAT + Critical + Expiration) |
| Statistical Calculation | **NEW** | Implement Z-score service layer |
| Report Generation | Existing report infrastructure | **Extend** with EQA templates |

---

## 6. Constitution Compliance Checklist

| Principle | Mockup Status | Required Changes |
|-----------|---------------|------------------|
| **CR-001**: Carbon Design System | ❌ FAIL | **Rebuild ALL components** with @carbon/react |
| **CR-002**: React Intl i18n | ❌ FAIL | **Replace hardcoded strings** with `<FormattedMessage>` |
| **CR-003**: 5-layer architecture | N/A (frontend only) | Backend: Implement Order→OrderDAO→OrderService→OrderController→OrderForm |
| **CR-004**: Liquibase migrations | N/A | Create changesets for Order extensions + new tables |
| **CR-005**: FHIR R4 compliance | ⚠️ Partial | FHIR endpoint shown but not implemented |
| **CR-006**: Configuration-driven | ⚠️ Partial | Programs shown as configurable, but thresholds hardcoded |
| **CR-007**: Security (RBAC, audit) | ⚠️ Partial | Audit fields in Alert model, but no RBAC shown |
| **CR-008**: Test coverage >70% | N/A | No tests in mockup |

**Constitution Verdict**: **MAJOR REFACTOR REQUIRED** to align with OpenELIS Global 3.0 standards.

---

## 7. Implementation Recommendations

### Phase 1: Core Infrastructure (Pre-P1)
```
1. Database schema (Liquibase changesets)
   - Extend Order table with 7 new EQA fields
   - Create EQAProgram, EQADistribution, EQAParticipant, EQAResult, Alert tables

2. Backend entities (JPA/Hibernate)
   - Order valueholder extensions
   - 5 new valueholders (EQAProgram, EQADistribution, etc.)

3. Alert framework (extensible for non-EQA alerts)
   - AlertService with deadline calculation
   - AlertDAO with status tracking
   - Scheduled job for alert generation (cron)
```

### Phase 2: P1 User Stories
```
1. Sample Entry (User Story 1)
   - Extend existing PatientEntryForm.tsx with:
     - EQA checkbox on Patient tab (Carbon Checkbox)
     - New Program tab with 7 EQA fields (Carbon Dropdown, DatePicker, TextInput)
     - EQA validation in OrderValidator.java
   - Extend WorkQueueService to include EQA filter
   - Add EQA badge to WorkQueue.tsx (Carbon Tag)

2. Alerts Dashboard (User Story 2)
   - NEW AlertsDashboard.tsx page (Carbon DataTable + Tiles)
   - AlertService.java with deadline calculations
   - AlertController.java with REST endpoints
   - Scheduled job: AlertGenerationJob.java (cron every 1 hour)
   - CRITICAL: Use Carbon ToastNotification for escalations (FR-045)
```

### Phase 3: P2 User Stories
```
1. Distribution (User Story 4)
   - NEW EQADistribution.tsx page (Carbon Modal + DataTable)
   - EQADistributionService.java with barcode/label integration
   - Link to 002-shipment-support for shipment tracking

2. Results Management (User Story 5)
   - NEW EQAManagement.tsx page (Carbon DataTable + Modal)
   - EQAResultService.java with FHIR integration
   - File upload validation (Apache POI for Excel, OpenCSV for CSV)

3. Statistical Analysis (User Story 3)
   - NEW EQAStatisticalAnalysis.tsx page
   - StatisticalAnalysisService.java with Z-score calculation
   - Charts using @carbon/charts-react (histogram + scatter)
   - PDF generation using existing iText infrastructure

4. General Alerts (User Story 6)
   - Extend AlertsDashboard with STAT, Critical, Expiration alerts
   - STAT alert service (50%/75%/100% thresholds)
   - Sample expiration service (7d/2d/1d thresholds)
```

### Phase 4: P3 User Stories
```
1. Program Configuration (User Story 7)
   - NEW EQAProgramManager.tsx page
   - EQAProgramService.java CRUD operations
   - Admin-only access (RBAC check)
```

---

## 8. Key Differences Summary

| Aspect | Figma Make Mockup | OpenELIS Global 3.0 Spec | Action Required |
|--------|-------------------|--------------------------|-----------------|
| **UI Framework** | Shadcn/ui + Tailwind | Carbon Design System | Complete rebuild |
| **Internationalization** | Hardcoded English | React Intl | Replace all strings |
| **Tab Structure** | Sample Info→EQA Details→Tests→Review | Patient→Program→Sample→Order | Rename tabs |
| **Priority Values** | STAT/URGENT/ROUTINE | Standard/Urgent/Critical | Align terminology |
| **Alerts Filters** | Type tabs only | Type + Severity + Lab Section + "My Alerts" | Add missing filters |
| **Pagination** | None | Required (FR-047) | Add Carbon pagination |
| **Statistical Analysis** | Missing UI | Required (FR-045 to FR-053) | Build new page |
| **Search** | None | Required (FR-038) | Add Carbon Search |
| **Icons** | Lucide | Carbon Icons | Replace all icons |
| **Backend** | None (frontend mockup) | 5-layer architecture | Implement full stack |
| **Database** | None | PostgreSQL + Liquibase | Create schema |
| **FHIR** | Documented only | Implemented (CR-005) | Build endpoints |

---

## 9. Risk Assessment

### High Risk
1. **Component Migration**: Shadcn → Carbon is **1:1 replacement** for simple components, but DataTable migration is complex
2. **Statistical Analysis**: Entirely missing from mockup, requires new implementation
3. **Performance**: Mock data vs real-time alerts with 200+ active items (FR-034 performance requirement)

### Medium Risk
1. **FHIR Integration**: Mockup shows UI but no implementation
2. **Alert Escalation**: Backend scheduled job complexity
3. **File Upload Validation**: CSV/Excel parsing with error handling

### Low Risk
1. **Sample Entry**: Extending existing form is straightforward
2. **Distribution**: Similar to existing shipment workflow
3. **Program Configuration**: Standard CRUD operations

---

## 10. Next Steps

### Immediate Actions (Before `/speckit.plan`)

1. ✅ **Confirm mockup is UX reference only** (not production code)
2. ⚠️ **Clarify priority terminology**: Spec uses Standard/Urgent/Critical, mockup uses STAT/URGENT/ROUTINE → **Use spec values**
3. ⚠️ **Clarify tab structure**: Mockup has 4 tabs, spec requires existing 4-tab structure → **Extend existing tabs**
4. ❌ **Add statistical analysis UI to spec**: Mockup doesn't show it, but it's required by User Story 3 → **Confirm UI design**

### After Clarifications

1. Run `/speckit.plan` to generate implementation plan
2. Plan database schema (Liquibase changesets)
3. Design API endpoints (REST + FHIR)
4. Create Carbon component mapping guide
5. Set up test data fixtures

---

## Appendix: Mockup File Structure

```
EQA-Sample-Management-System/
├── App.tsx                           # Main navigation + hamburger menu
├── components/
│   ├── sample-entry/
│   │   └── EQASampleEntry.tsx        # 4-tab sample registration (P1)
│   ├── work-queue/
│   │   └── WorkQueue.tsx             # Sample queue with EQA filter (P1)
│   ├── alerts/
│   │   └── AlertsDashboard.tsx       # Comprehensive alerts (P1+P2)
│   ├── management/
│   │   └── EQAManagement.tsx         # Received samples + results entry (P2)
│   ├── distribution/
│   │   └── EQADistribution.tsx       # Create/track distributions (P2)
│   ├── admin/
│   │   └── EQAProgramManager.tsx     # Program configuration (P3) - NOT IMPLEMENTED
│   ├── test-request/
│   │   └── TestRequest.tsx           # Test request form (not EQA-specific)
│   └── ui/                           # Shadcn/ui components (57 components)
│       ├── accordion.tsx
│       ├── alert.tsx
│       ├── badge.tsx
│       ├── button.tsx
│       ├── calendar.tsx
│       ├── card.tsx
│       ├── checkbox.tsx
│       ├── dialog.tsx
│       ├── input.tsx
│       ├── select.tsx
│       ├── table.tsx
│       ├── tabs.tsx
│       ├── textarea.tsx
│       └── ... (44 more components)
├── styles/
│   └── globals.css                   # Tailwind CSS config
├── guidelines/
│   └── Guidelines.md                 # Empty template
└── README.md                         # Feature documentation
```

**Total Components**: 6 EQA-specific + 57 Shadcn/ui base components + 1 non-EQA component

---

**Analysis Complete**: Figma Make mockup provides excellent UX reference for workflows but requires complete rebuild using Carbon Design System to comply with OpenELIS Global 3.0 constitution.
