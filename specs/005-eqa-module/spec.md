# Feature Specification: External Quality Assurance (EQA) Module

**Feature Branch**: `005-eqa-module`
**Created**: 2025-11-14
**Status**: Draft
**Input**: User description: "External Quality Assurance (EQA) module for proficiency testing"

## Clarifications

### Session 2025-11-17

- Q: Does the Figma design include the priority level field (Standard/Urgent/Critical) required by FR-003 on the Program Selection screen? → A: Priority field already exists in implementation
- Q: Does "provider sample ID" in FR-003 refer to one field or two separate fields (Shipment ID and Sample ID shown in Figma)? → A: Sample ID only - Shipment ID is for outgoing distributions, not incoming samples
- Q: Should the system capture which external organization (e.g., WHO, CAP, National Reference Lab) sent the EQA sample? → A: Yes - Add EQA Provider field to track the sending organization
- Q: Should the system capture the laboratory's participant ID assigned by the EQA provider? → A: Yes - Add as optional field for result submission traceability
- Q: How should supervisors be notified when critical alerts escalate after 4 hours? → A: Dashboard badge + in-app notification using Carbon ToastNotification
- Q: The Figma Make mockup uses Shadcn/ui + Tailwind CSS. Should it be treated as production code or UX reference only? → A: UX reference only - Use workflows/layouts as design guidance, rebuild all components using Carbon Design System from scratch
- Q: The Figma Make mockup has no statistical analysis UI implementation. Where should the statistical analysis be displayed? → A: Dedicated page - Create new "EQA Statistical Analysis" page accessible from EQA Management menu with full DataTable, charts, and export functionality
- Q: How should STAT order target times be configured for 50%/75%/100% alert thresholds (FR-042)? → A: Per-test configuration - Each test in the catalog has a configurable STAT turnaround time target (default: 4 hours), allowing lab-specific customization
- Q: What level of integration is required with the 002-shipment-support feature for EQA distributions? → A: Create-only integration - EQA distribution workflow creates draft shipments using existing shipment API, then hands off to standard shipment tracking (no EQA-specific shipment modifications)
- Q: How should EQA performance reports be generated (FR-029, UI-011)? → A: Existing report infrastructure
- Q: Should EQA result entry use a custom interface or integrate with existing Results interface? → A: Reuse existing "Results - By Order Number" interface with EQA badge/icon visible on result; EQA context (provider, program) shown when expanding result details

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Register and Process Incoming EQA Samples (Priority: P1)

A laboratory technician receives an EQA sample from an external proficiency testing program. They need to register this sample in OpenELIS Global, ensuring it's properly identified as an EQA sample (not a patient sample), assigned the correct tests, and tracked against the submission deadline.

**Why this priority**: This is the foundational capability required for any laboratory to participate in external proficiency testing programs. Without the ability to register and identify EQA samples, none of the other EQA workflows are possible. This delivers immediate value by enabling basic EQA participation.

**Independent Test**: Can be fully tested by registering an EQA sample through the sample entry workflow, verifying it appears with proper EQA indicators in work queues, processing the sample through testing, and confirming the sample maintains its EQA designation throughout the workflow.

**Acceptance Scenarios**:

1. **Given** I am on the sample entry screen, **When** I select "EQA sample" checkbox during patient information entry, **Then** all patient demographic fields become disabled with "N/A" placeholders and EQA-specific fields become available on the Program tab
2. **Given** I have selected "EQA sample", **When** I navigate to the Program tab, **Then** I must select the EQA provider organization, select an EQA program, enter the sample ID from the EQA provider, set a testing deadline, and choose a priority level (Standard/Urgent/Critical) before proceeding
3. **Given** I have registered an EQA sample, **When** I view the work queue, **Then** the sample displays an EQA badge/icon for visual identification and can be filtered separately from patient samples
4. **Given** I am processing an EQA sample, **When** I enter results, **Then** the system prevents result modification after the submission deadline has passed

---

### User Story 2 - Monitor EQA Deadlines and Alerts (Priority: P1)

A laboratory supervisor needs to monitor upcoming EQA submission deadlines across all active EQA samples to ensure the laboratory meets proficiency testing requirements. They receive automated alerts at critical intervals and can take action to prioritize samples approaching their deadlines.

**Why this priority**: Deadline management is critical for EQA compliance. Missing submission deadlines can result in failed proficiency testing evaluations and regulatory non-compliance. This is P1 because it directly impacts the laboratory's ability to meet external quality requirements.

**Independent Test**: Can be fully tested by registering EQA samples with various deadlines, verifying alert generation at 72 hours, 24 hours, and 4 hours before deadline, and confirming that overdue samples are flagged as critical alerts in the alerts dashboard.

**Acceptance Scenarios**:

1. **Given** I have access to the alerts dashboard, **When** an EQA sample has a deadline within 72 hours, **Then** I see an informational alert with the sample lab number, deadline, and assigned section
2. **Given** an EQA sample deadline is within 24 hours, **When** I access the alerts dashboard, **Then** the alert severity escalates to "warning" with orange color coding
3. **Given** an EQA sample deadline is within 4 hours, **When** I view the dashboard, **Then** the alert severity becomes "critical" with red color coding and the sample appears in the "EQA Deadlines Today" summary tile
4. **Given** an EQA sample deadline has passed, **When** results have not been submitted, **Then** the sample is flagged as overdue and requires supervisor approval for late submission
5. **Given** I am a supervisor, **When** I filter alerts by "My Alerts", **Then** I see only alerts for EQA samples in my assigned lab sections
6. **Given** I acknowledge a critical alert, **When** I click "Acknowledge", **Then** I must provide a resolution comment and the system logs my user ID and timestamp

---

### User Story 3 - Create and Distribute EQA Samples to Participating Laboratories (Priority: P2)

An EQA coordinator at a reference laboratory needs to create a proficiency testing distribution by selecting participating clinic organizations, assigning test panels, generating appropriate barcodes and labels, and organizing samples into shipments for distribution.

**Why this priority**: This enables laboratories to act as EQA providers, not just participants. While important for reference laboratories running proficiency testing programs, it's P2 because most laboratories will first need the ability to receive and process EQA samples (P1 stories) before they act as distributors.

**Independent Test**: Can be fully tested by creating an EQA distribution for multiple clinic organizations, verifying that the system creates separate order/sample records for each participant, generates barcodes, and organizes samples into draft shipments ready for physical distribution.

**Acceptance Scenarios**:

1. **Given** I have EQA coordinator permissions, **When** I access the "Create EQA Distribution" workflow, **Then** I can select multiple participating clinic organizations from the existing organizations list
2. **Given** I have selected participating organizations, **When** I choose an EQA panel template, **Then** the system creates separate order/sample records for each clinic with identical test assignments
3. **Given** EQA distribution samples are created, **When** I initiate batch barcode generation, **Then** the system generates unique barcodes following existing labeling standards with one order per health facility
4. **Given** distribution samples are barcoded, **When** I organize them into shipments, **Then** the system creates draft shipments using the existing shipment/box functionality with appropriate tracking documentation
5. **Given** I am finalizing a distribution, **When** fewer than 2 participating organizations are selected, **Then** the system prevents finalization with an error message requiring minimum 2 participants

---

### User Story 4 - Collect Results and Analyze EQA Performance (Priority: P2)

An EQA coordinator receives results from participating laboratories through multiple channels (FHIR API, manual entry, CSV upload) and needs to view automatic statistical analysis (Z-scores, means, standard deviations) to generate performance reports for each participant.

**Why this priority**: This completes the EQA distribution cycle, enabling reference laboratories to provide meaningful feedback to participants. It's P2 because it depends on P1 capabilities (receiving EQA samples) and P2 distribution capabilities. Statistical analysis is essential for proficiency testing compliance.

**Independent Test**: Can be fully tested by submitting EQA results through different channels (API, manual entry, file upload), verifying automatic Z-score calculation when minimum 5 participants have submitted, and generating downloadable performance reports.

**Acceptance Scenarios**:

1. **Given** a participating laboratory has completed EQA testing, **When** they submit results via FHIR API, **Then** the results automatically appear in the EQA Management menu with submission timestamp and method
2. **Given** I am an EQA coordinator, **When** I need to enter results for a participant without electronic submission capability, **Then** I can manually enter results through a dedicated entry screen
3. **Given** I have results from multiple participants in CSV format, **When** I upload the file via the batch import interface, **Then** the system validates and imports all results with error reporting for invalid entries
4. **Given** at least 5 participants have submitted results for an EQA distribution, **When** I view the EQA Management dashboard, **Then** the system displays automatic statistical analysis including Z-scores (Z = (result - target) / SD), mean, and standard deviation
5. **Given** statistical analysis is complete, **When** I classify performance, **Then** results with |Z-score| ≤ 2.0 are marked "Acceptable", 2.0 < |Z-score| ≤ 3.0 are "Questionable", and |Z-score| > 3.0 are "Unacceptable"
6. **Given** EQA results and statistics are available, **When** I generate a performance report, **Then** the system creates a downloadable report with comparative analysis across all participants and individual participant performance breakdowns

---

### User Story 5 - Submit EQA Results to External Programs (Priority: P3)

A laboratory that participates in external EQA programs needs to submit their testing results to the EQA provider through their preferred submission method (FHIR API for OpenELIS-to-OpenELIS communication, or manual web interface for other providers).

**Why this priority**: This enables seamless result submission to external EQA providers. It's P3 because the core capability (testing EQA samples) is already covered in P1, and this story focuses on the submission mechanism rather than the testing workflow itself. Many laboratories may submit results through external provider portals independently.

**Independent Test**: Can be fully tested by completing an EQA sample test, verifying results can be submitted via FHIR API with proper authentication, and confirming submission acknowledgment is recorded in the audit trail.

**Acceptance Scenarios**:

1. **Given** I have completed testing an EQA sample, **When** the EQA program is configured for FHIR integration, **Then** I can submit results electronically to the external provider's API endpoint
2. **Given** FHIR submission is not available, **When** I need to submit results, **Then** I can access a web form to manually report results with provider-specific format
3. **Given** I submit results before the deadline, **When** the submission is successful, **Then** the system logs the submission timestamp, method, and user ID in the audit trail
4. **Given** I attempt to submit results after the deadline, **When** I click submit, **Then** the system requires supervisor approval and mandatory justification comment before allowing late submission

---

### User Story 6 - Manage Comprehensive Laboratory Alerts (Priority: P2)

A laboratory supervisor needs a centralized dashboard to monitor multiple types of time-sensitive alerts including EQA deadlines, sample expirations, STAT order status, and unacknowledged critical results, all filtered to their assigned laboratory sections.

**Why this priority**: This extends the alerts system beyond just EQA to cover all time-sensitive laboratory operations. It's P2 because while EQA deadline alerts are P1-critical, the comprehensive multi-type alert system delivers broader operational value and requires additional development for non-EQA alert types.

**Independent Test**: Can be fully tested by creating various alert-triggering conditions (expiring samples, STAT orders, EQA deadlines, critical results), verifying they all appear in the centralized dashboard with appropriate severity color coding, and confirming role-based filtering works correctly.

**Acceptance Scenarios**:

1. **Given** I access the Laboratory Alerts dashboard, **When** the page loads, **Then** I see summary tiles showing counts for "Critical Alerts", "EQA Deadlines Today", "Overdue STAT Orders", and "Samples Expiring"
2. **Given** I am viewing the alerts table, **When** multiple alert types are present, **Then** each alert displays type icon, severity color coding (Red=Critical, Orange=High, Yellow=Medium, Blue=Low), description, lab section, due date, lab number, assigned user, and action buttons
3. **Given** I want to focus on my responsibilities, **When** I check the "My Alerts" checkbox, **Then** the table filters to show only alerts for lab sections I'm assigned to
4. **Given** I need to find a specific alert, **When** I use the search bar with a lab number, alert type, or assignment name, **Then** the table filters to show matching alerts
5. **Given** a sample is expiring within 7 days, **When** I view the alerts dashboard, **Then** I see an informational (blue) alert; at 2 days it escalates to warning (orange); at 1 day it becomes critical (red)
6. **Given** a STAT order is at 50% of target time, **When** it appears in the dashboard, **Then** it shows as informational; at 75% it becomes warning; at 100% it becomes critical
7. **Given** I acknowledge an alert, **When** I click "Acknowledge" on a critical alert, **Then** I must provide a resolution comment before the alert is marked as resolved with my user ID and timestamp
8. **Given** a critical alert is unacknowledged for 4 hours, **When** the escalation timer expires, **Then** the alert automatically escalates to supervisor level with notification badge increment and Carbon ToastNotification displayed on supervisor's next login

---

### User Story 7 - Configure EQA Programs (Priority: P3)

A laboratory administrator needs to create and manage EQA programs, defining program names, descriptions, and assigning appropriate test panels that can be used during EQA sample registration and distribution creation.

**Why this priority**: This is administrative configuration that supports the operational workflows in P1 and P2 stories. It's P3 because the system can function with a minimal set of pre-configured programs, and this configuration is infrequent compared to daily operational workflows.

**Independent Test**: Can be fully tested by creating a new EQA program through the admin interface, assigning tests and panels to it, activating the program, and verifying it appears as a selectable option during EQA sample registration.

**Acceptance Scenarios**:

1. **Given** I have administrator permissions, **When** I access the EQA Program Management screen, **Then** I can view a list of all existing EQA programs with their status (active/inactive)
2. **Given** I want to create a new EQA program, **When** I click "Create Program", **Then** I see a modal form to enter program name, description, and activation status
3. **Given** I have created an EQA program, **When** I assign tests and panels to it, **Then** I can select from the existing test and panel catalog using the normal assignment interface
4. **Given** I have configured an active EQA program, **When** a technician registers an EQA sample, **Then** the program appears in the EQA program selection dropdown on the Program tab
5. **Given** I deactivate an EQA program, **When** I save the change, **Then** the program no longer appears in selection dropdowns but existing samples referencing it remain intact

---

### Edge Cases

- **What happens when an EQA sample has the same accession number as a patient sample?** The system must enforce unique accession number constraints across all sample types; EQA samples follow the same barcode generation rules and cannot duplicate existing accession numbers.

- **How does the system handle late result submissions after the EQA deadline has passed?** Results submitted after the deadline are flagged as late submissions, require supervisor approval with mandatory justification comments, and may be excluded from statistical analysis or marked separately in performance reports depending on EQA program rules.

- **What happens when fewer than 5 participants submit results for statistical analysis?** The system displays a warning that "Minimum 5 participants required for valid statistical analysis" and shows descriptive statistics (mean, SD) but marks Z-scores as "Not calculable - insufficient participants".

- **How does the system handle duplicate result submissions from the same participant?** Duplicate submissions overwrite previous results with a complete audit trail showing the original submission timestamp, original result value, update timestamp, updated result value, and the user who made the change.

- **What happens when an EQA sample is received but the EQA program is not configured in the system?** The sample registration workflow allows technicians to register the sample with a temporary "Unconfigured Program" placeholder, generates an alert for administrators to configure the missing program, and allows retroactive program assignment once configured.

- **How does the system handle EQA distributions when a participating organization is deactivated or removed?** Existing distribution records remain intact for audit purposes, but the organization no longer appears in new distribution creation workflows; pending shipments to deactivated organizations generate alerts for EQA coordinators to resolve.

- **What happens when EQA result values fall outside biologically plausible ranges?** The system validates numeric results against test-specific ranges during submission and displays a warning if values exceed expected limits, requiring explicit confirmation from the user before accepting the result.

- **How does the system handle timezone differences for EQA deadlines in multi-site deployments?** All deadlines are stored in UTC in the database and displayed to users in their configured local timezone; alert generation uses UTC-based calculations to ensure consistent deadline enforcement across sites.

- **What happens when a user attempts to modify an EQA sample's test assignments after distribution has been created?** The system prevents test assignment changes once the sample is part of a finalized distribution (status = "Shipped" or "Completed"); samples in "Draft" or "Prepared" status allow modifications with audit trail logging.

- **How does the alerts dashboard handle high-volume scenarios with hundreds of active alerts?** The system uses pagination (25/50/100/200 items per page), implements server-side filtering and sorting, and provides real-time auto-refresh every 60 seconds with notification count badges; alerts older than 30 days are automatically archived to maintain performance.

## Requirements _(mandatory)_

### Functional Requirements

#### EQA Sample Reception

- **FR-001**: System MUST allow users to designate a sample as "EQA sample" during registration through a checkbox on the patient information tab
- **FR-002**: System MUST automatically disable patient demographic fields and populate them with "N/A" placeholders when "EQA sample" is selected
- **FR-003**: System MUST provide EQA-specific fields on the Program tab including EQA provider organization (mandatory), EQA program selection (mandatory), sample ID (identifier from EQA provider), participant ID (optional - laboratory's ID in the EQA program), testing deadline, and priority level (Standard/Urgent/Critical)
- **FR-004**: System MUST prevent EQA sample registration from completing without selecting an EQA program
- **FR-005**: System MUST display EQA badge/icon on all samples marked as EQA in work queues, sample listings, and result entry screens; in Results interface, EQA context (provider organization, program name) must be accessible when expanding result details
- **FR-006**: System MUST support filtering work queues to show/hide EQA samples independently from patient samples
- **FR-007**: System MUST prevent modification of EQA sample results after the submission deadline has passed without supervisor approval

#### EQA Deadline and Priority Management

- **FR-008**: System MUST generate informational alerts 72 hours before EQA sample deadlines
- **FR-009**: System MUST generate warning alerts 24 hours before EQA sample deadlines with escalated severity
- **FR-010**: System MUST generate critical alerts 4 hours before EQA sample deadlines with highest severity
- **FR-011**: System MUST flag overdue EQA samples (past deadline without result submission) as critical alerts requiring supervisor attention
- **FR-012**: System MUST apply priority handling for EQA samples: Critical priority processed within 4 hours bypassing normal queue, Urgent priority processed within 24 hours with elevated queue position, Standard priority follows normal workflow
- **FR-013**: System MUST display priority level indicators in all work queues and sample displays

#### EQA Distribution

- **FR-014**: System MUST provide an "Create EQA Distribution" workflow accessible to users with EQA coordinator role
- **FR-015**: System MUST allow selection of multiple participating clinic organizations from existing organizations list during distribution creation
- **FR-016**: System MUST create separate order/sample records for each selected participating organization
- **FR-017**: System MUST enforce identical test panel assignments for all samples within a single EQA distribution
- **FR-018**: System MUST prevent distribution finalization with fewer than 2 participating organizations
- **FR-019**: System MUST generate unique barcodes for each distribution sample following existing labeling standards
- **FR-020**: System MUST integrate distribution samples with existing draft shipment/box functionality for physical distribution preparation by creating shipment records via existing shipment API without modifying shipment entities or tracking logic
- **FR-021**: System MUST track master sample to distribution sample relationships using standard aliquoting processes

#### EQA Results Collection and Analysis

- **FR-022**: System MUST accept EQA result submissions via FHIR API integration for OpenELIS-to-OpenELIS communication
- **FR-023**: System MUST provide manual result entry for non-electronic participant submissions by integrating with existing "Results - By Order Number" interface, displaying EQA badge/icon on the result entry, with EQA context (provider, program) accessible in expanded result details
- **FR-024**: System MUST support CSV and Excel file upload for batch result import with validation and error reporting
- **FR-025**: System MUST calculate Z-scores using formula: Z = (participant result - target value) / standard deviation
- **FR-026**: System MUST classify performance as Acceptable (|Z-score| ≤ 2.0), Questionable (2.0 < |Z-score| ≤ 3.0), or Unacceptable (|Z-score| > 3.0)
- **FR-027**: System MUST require minimum 5 participants for valid statistical analysis and display warning when insufficient data
- **FR-028**: System MUST display statistical analysis (Z-scores, means, standard deviations) in the EQA Management menu for each distribution
- **FR-029**: System MUST generate downloadable performance reports with comparative analysis across participants and individual performance breakdowns using existing OpenELIS report infrastructure
- **FR-030**: System MUST log submission method (FHIR/Manual/File Upload), submission timestamp, and submitting user ID for all EQA results

#### EQA Result Submission

- **FR-031**: System MUST validate numeric results against test-specific biologically plausible ranges during submission
- **FR-032**: System MUST prevent result submission after deadline without supervisor approval and justification comment
- **FR-033**: System MUST support duplicate result submissions that overwrite previous values with complete audit trail
- **FR-034**: System MUST record submission acknowledgment in audit trail with timestamp, method, and user identification

#### Comprehensive Alerts Dashboard

- **FR-035**: System MUST provide centralized alerts dashboard with summary tiles showing counts for "Critical Alerts", "EQA Deadlines Today", "Overdue STAT Orders", and "Samples Expiring"
- **FR-036**: System MUST display alerts in a data table with columns: Alert Type (icon + text), Severity (color-coded), Description, Lab Section, Due Date/Time, Lab Number, Assigned To, Actions
- **FR-037**: System MUST support "My Alerts" checkbox filter to show only alerts for user's assigned lab sections
- **FR-038**: System MUST support search functionality by lab number, alert type, or assignment name
- **FR-039**: System MUST support filtering by Alert Type (EQA Deadlines, STAT Orders, Critical Results, Sample Expiration), Severity (Critical, High, Medium, Low), and Lab Section
- **FR-040**: System MUST apply severity-based color coding: Red=Critical, Orange=High, Yellow=Medium, Blue=Low
- **FR-041**: System MUST generate sample expiration alerts at 7 days (info/blue), 2 days (warning/orange), and 1 day (critical/red) before expiration
- **FR-042**: System MUST generate STAT order alerts at 50% of target time (info/blue), 75% (warning/orange), and 100% (critical/red). Target times are configurable per-test in the test catalog (default: 4 hours STAT turnaround)
- **FR-043**: System MUST require resolution comments for critical alert acknowledgment
- **FR-044**: System MUST log acknowledgment with user ID and timestamp in audit trail
- **FR-045**: System MUST escalate unacknowledged critical alerts to supervisor level after 4 hours by incrementing notification badge on alerts menu icon and displaying Carbon ToastNotification when supervisor next accesses the system
- **FR-046**: System MUST auto-refresh alerts dashboard every 60 seconds with notification count badge updates
- **FR-047**: System MUST support pagination with selectable items per page (25, 50, 100, 200)

#### EQA Program Management

- **FR-048**: System MUST provide EQA Program Management screen accessible to administrators
- **FR-049**: System MUST allow creation of EQA programs with name, description, and active/inactive status
- **FR-050**: System MUST support assignment of tests and panels to EQA programs using existing test catalog
- **FR-051**: System MUST make active EQA programs available in selection dropdowns during EQA sample registration
- **FR-052**: System MUST preserve existing sample references when an EQA program is deactivated
- **FR-053**: System MUST maintain EQA programs independently from organization entities

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO custom CSS frameworks. The Figma Make mockup (Shadcn/ui + Tailwind) serves as UX reference only for workflows and layouts; all components must be rebuilt using Carbon from scratch. EQA interfaces must use: Carbon Checkbox for EQA sample selection, Carbon FormGroup for disabled patient demographics, Carbon Select for EQA program and priority selection, Carbon DatePicker for deadlines, Carbon Tag for EQA badges, Carbon DataTable for work queues and alerts, Carbon Modal for result entry, Carbon FileUploader for batch imports, Carbon Tile for alert summary cards, Carbon ToastNotification for urgent alerts
- **CR-002**: All UI strings MUST be internationalized via React Intl (no hardcoded text) - includes all EQA-specific labels, alert messages, button text, form field labels, and error messages
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form):
  - Valueholders: `EQAProgram`, `EQADistribution`, `EQAResult`, `Alert`, `EQAProgramTest` entities with JPA/Hibernate annotations
  - DAOs: `EQAProgramDAO`, `EQADistributionDAO`, `EQAResultDAO`, `AlertDAO`, `EQAProgramTestDAO`
  - Services: `EQAProgramService`, `EQADistributionService`, `EQAResultService`, `AlertService` with @Transactional annotations
  - Controllers: `EQAProgramController`, `EQADistributionController`, `EQAResultController`, `AlertController` (NO @Transactional in controllers)
  - Forms: `EQASampleEntryForm`, `EQADistributionForm`, `EQAResultForm` for data binding
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML) - includes new tables for EQAProgram, EQADistribution, EQAResult, Alert, EQAProgramTest, and Order table extensions for EQA fields
- **CR-005**: External data integration MUST use FHIR R4 for EQA result submissions between OpenELIS instances
- **CR-006**: Configuration-driven variation for country-specific EQA requirements (NO code branching) - use configuration tables for EQA program definitions, alert thresholds, and statistical calculation parameters
- **CR-007**: Security MUST implement RBAC for EQA coordinator role, audit trail with sys_user_id + lastupdated for all EQA entities, input validation for result values and statistical calculations
- **CR-008**: Tests MUST be included with >70% coverage goal:
  - Unit tests: Services for statistical calculations, alert generation logic, validation rules
  - Integration tests: EQA sample registration workflow, distribution creation, result submission via API
  - E2E tests (Cypress): Complete EQA sample journey from registration to result submission, EQA distribution workflow, alerts dashboard interaction

### UI/UX Requirements

- **UI-001**: EQA sample checkbox on patient information tab must be clearly labeled and positioned prominently
- **UI-002**: Disabled patient demographic fields with "N/A" placeholders must have visual indication (grayed out styling)
- **UI-003**: EQA badge/icon must use consistent Carbon Design System Tag component with distinctive color (suggest blue)
- **UI-004**: Work queue EQA filter must be accessible via Carbon TableToolbar with clear toggle state
- **UI-005**: EQA program selection dropdown must display program name and description
- **UI-006**: Priority level selection must use visual indicators: Critical (red), Urgent (orange), Standard (blue)
- **UI-007**: Create EQA Distribution workflow must use Carbon ProgressIndicator for multi-step process visualization
- **UI-008**: Alerts dashboard summary tiles must follow existing OpenELIS pathology dashboard design pattern
- **UI-009**: Alert severity color coding must be consistent throughout application: Red=Critical, Orange=High, Yellow=Medium, Blue=Low
- **UI-010**: Statistical analysis display must use Carbon StructuredList with clear labels for Z-score, mean, standard deviation, and performance classification
- **UI-011**: Performance reports must be downloadable in PDF format with laboratory branding
- **UI-012**: Alert acknowledgment action must use Carbon Modal with required text input for resolution comments
- **UI-013**: Statistical analysis must be displayed on a dedicated "EQA Statistical Analysis" page accessible from EQA Management menu, using Carbon DataTable for participant results comparison, @carbon/charts-react for histogram and scatter plot visualizations, and Carbon StructuredList for summary statistics

### Key Entities

- **Order (Extended)**: Existing entity extended with EQA-specific fields:
  - `is_eqa_sample` (boolean) - Flag to identify EQA samples
  - `eqa_provider_organization_id` (foreign key to Organization) - External organization that sent the EQA sample (e.g., WHO, CAP, National Reference Lab)
  - `eqa_program_id` (foreign key to EQAProgram) - Associated proficiency testing program
  - `eqa_provider_sample_id` (string) - Sample identifier from external EQA provider
  - `eqa_participant_id` (string, nullable) - Optional laboratory participant ID assigned by the EQA provider (e.g., "LAB-237")
  - `eqa_deadline` (datetime) - Testing and submission deadline
  - `eqa_priority` (enum: STANDARD, URGENT, CRITICAL) - Processing priority level
  - `eqa_distribution_id` (foreign key to EQADistribution) - Reference for distributed samples

- **EQAProgram**: Represents proficiency testing programs:
  - `id` (primary key)
  - `name` (string) - Program name (e.g., "WHO Malaria Microscopy EQA")
  - `description` (text) - Program details and objectives
  - `is_active` (boolean) - Program status
  - `created_date`, `modified_date` (timestamps)
  - Relationships: Many-to-many with Test via EQAProgramTest

- **EQADistribution**: Represents a batch distribution of EQA samples to participating laboratories:
  - `id` (primary key)
  - `eqa_program_id` (foreign key) - Associated proficiency testing program
  - `distribution_name` (string) - Unique distribution identifier (e.g., "Malaria-2025-Q1")
  - `distribution_date` (datetime) - When distribution was created
  - `status` (enum: DRAFT, PREPARED, SHIPPED, COMPLETED) - Distribution lifecycle state
  - `created_by` (foreign key to User) - EQA coordinator who created distribution
  - Relationships: One-to-many with Order (distributed samples)

- **EQAResult**: Stores participant results and statistical analysis:
  - `id` (primary key)
  - `eqa_distribution_id` (foreign key) - Associated distribution
  - `participant_organization_id` (foreign key to Organization) - Submitting laboratory
  - `test_id` (foreign key) - Test performed
  - `result_value` (decimal) - Participant submitted value
  - `target_value` (decimal) - Expected correct value
  - `z_score` (decimal) - Calculated Z-score for performance assessment
  - `submission_method` (enum: FHIR, MANUAL, FILE_UPLOAD) - How result was received
  - `submission_date` (datetime) - When result was submitted
  - `performance_status` (enum: ACCEPTABLE, QUESTIONABLE, UNACCEPTABLE) - Based on Z-score thresholds
  - Relationships: References EQADistribution, Organization, Test

- **Alert**: Centralized alert management for all time-sensitive laboratory activities:
  - `id` (primary key)
  - `alert_type` (enum: EQA_DEADLINE, SAMPLE_EXPIRATION, STAT_UPCOMING, STAT_OVERDUE, CRITICAL_UNACKNOWLEDGED) - Alert category
  - `reference_id` (string) - ID of related entity (order ID, sample ID, result ID)
  - `reference_type` (enum: ORDER, SAMPLE, RESULT, TEST) - Type of referenced entity
  - `alert_message` (text) - Human-readable alert description
  - `severity_level` (enum: LOW, MEDIUM, HIGH, CRITICAL) - Alert severity
  - `lab_section_id` (foreign key to Section) - Associated laboratory section
  - `due_date` (datetime) - When alert becomes critical
  - `acknowledged` (boolean) - Whether alert has been addressed
  - `acknowledged_by` (foreign key to User) - User who acknowledged
  - `acknowledged_date` (datetime) - When acknowledged
  - `resolution_comment` (text) - Mandatory for critical alerts
  - `created_date` (timestamp)
  - Relationships: References Section, User (acknowledged_by)

- **EQAProgramTest**: Many-to-many junction table for EQA program test assignments:
  - `id` (primary key)
  - `eqa_program_id` (foreign key) - Program reference
  - `test_id` (foreign key) - Test reference
  - `is_active` (boolean) - Assignment status
  - Relationships: References EQAProgram and Test

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory technicians can register an EQA sample from start to finish (including program selection, deadline setting, and test assignment) in under 3 minutes using the modified sample entry workflow
- **SC-002**: EQA samples are visually identifiable in work queues within 1 second of page load using Carbon Design System Tag components
- **SC-003**: Alert generation for EQA deadlines occurs automatically at exactly 72 hours, 24 hours, and 4 hours before deadline with 99.9% accuracy across all timezones
- **SC-004**: EQA coordinators can create a distribution for 20 participating laboratories with complete barcode generation and shipment organization in under 10 minutes
- **SC-005**: Statistical analysis (Z-scores, means, standard deviations) calculates and displays within 2 seconds of the 5th participant result submission
- **SC-006**: Performance reports generate and download in PDF format within 5 seconds for distributions with up to 50 participants
- **SC-007**: Alerts dashboard loads and displays up to 200 active alerts with filtering and pagination in under 2 seconds
- **SC-008**: System handles concurrent result submissions from 50 participating laboratories without data corruption or performance degradation
- **SC-009**: 95% of EQA sample registrations complete successfully on first attempt without validation errors
- **SC-010**: Supervisor acknowledgment of critical alerts completes with mandatory resolution comments in under 30 seconds
- **SC-011**: FHIR API result submissions from external OpenELIS instances are received and processed within 5 seconds with automatic acknowledgment
- **SC-012**: Alert escalation to supervisor level occurs automatically and reliably 4 hours after critical alert generation if unacknowledged
- **SC-013**: System maintains >70% unit test coverage, >60% integration test coverage, and E2E tests for all critical user journeys
- **SC-014**: Zero patient identifiable information appears in EQA sample records (all demographic fields show "N/A" when EQA sample is selected)
- **SC-015**: Audit trail captures 100% of EQA-related actions including sample registration, result submission, alert acknowledgment, and statistical analysis generation with user ID and timestamp
