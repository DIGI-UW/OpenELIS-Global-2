OpenELIS ASTM Analyzer Mapping Feature Comprehensive Specification Document

Version: 1.0 Date: November 12, 2025 Status: Draft for Review Technology: Java
Spring Framework, Carbon React Protocol: ASTM LIS2-A2 (TCP/IP)

Table of Contents • 1. Executive Summary • 2. Functional Requirements • 3.
Business Rules • 4. User Stories • 5. UI Design Guidance (Carbon Design System)
• 6. API Endpoints • 7. Validation Rules • 8. Security & Permissions • 9.
Testing Requirements • 10. Implementation Phases • 11. Success Metrics • 12.
Risks & Mitigations • 13. Future Enhancements • 14. Appendices

1. Executive Summary This specification defines the analyzer field mapping
   feature for OpenELIS, enabling dynamic configuration of data mappings between
   clinical laboratory analyzers using ASTM LIS2-A2 protocol and the OpenELIS
   data model. This feature extends the existing ASTM communication layer with a
   user-friendly configuration interface built with Carbon Design System. Key
   Objectives • Enable dynamic field mapping between analyzers and OpenELIS
   without code changes • Support multiple analyzer types with per-analyzer or
   shared configurations • Provide intuitive UI conforming to Carbon Design
   System standards • Ensure data integrity through comprehensive validation
   rules • Facilitate troubleshooting with comprehensive error logging and
   dashboard Scope This specification covers the mapping configuration interface
   only. The existing ASTM LIS2-A2 TCP/IP communication layer, order request
   handling, and result receiving mechanisms are already implemented and are not
   in scope for this feature.

2. Functional Requirements 2.1 Analyzer Management FR-1: Analyzer Registration
   The system shall allow users to register new analyzers with the following
   attributes: • Analyzer name (unique, required, 1-100 characters) • Analyzer
   type/model (required, dropdown with "Other" option) • IP address (required,
   IPv4 format validation) • Port number (required, 1-65535 range) • Protocol
   version (default: ASTM LIS2-A2) • Active status (boolean, default: true) •
   Test unit assignment (required, multi-select, minimum 1) • Created date/time
   (auto-generated) • Last modified date/time (auto-generated) • Created by user
   (auto-generated) FR-2: Analyzer List View The system shall display all
   analyzers in a searchable, filterable data table with: • Columns: Name, Type,
   IP:Port, Status, Test Units, Last Modified • Default sort: Last Modified
   (descending) • Pagination: 25, 50, 100 items per page • Inline status toggle
   (active/inactive) FR-3: Analyzer Search and Filter The system shall provide:
   • Search: Free text search across Name, Type, IP fields (debounced, 300ms) •
   Filters: • Status: All, Active, Inactive • Test Unit: Multi-select dropdown •
   Analyzer Type: Multi-select dropdown • Filter pill display with clear
   functionality • Persistent filter state during session FR-4: Analyzer Actions
   The system shall support: • Add Analyzer: Modal dialog for new analyzer
   creation • Edit Analyzer: Modal dialog for existing analyzer modification •
   Delete Analyzer: Soft delete with confirmation (only if no active mappings) •
   Test Connection: Verify TCP connectivity and ASTM handshake • Configure
   Mappings: Navigate to field mapping interface 2.2 Field Discovery FR-5:
   Analyzer Query The system shall query the analyzer to retrieve available data
   fields: • Trigger: Manual "Query Analyzer" button in mapping interface •
   Process: Send ASTM query message, parse response records • Extract: All field
   identifiers from ASTM message segments • Display: Available fields in source
   panel with field type indicators FR-6: Field Type Detection The system shall
   detect or allow specification of field types: • Numeric (with unit of
   measure) • Qualitative (text/interpretation) • Control Test (flag) • Melting
   Point (temperature) • Date/Time • Text (general) • Custom (extensible) 2.3
   Field Mapping Configuration FR-7: Mapping Interface The system shall provide
   a dual-panel mapping interface with: • Left Panel (Source): Searchable list,
   field type indicators, ASTM references • Right Panel (Target): OpenELIS
   fields categorized by entity, searchable, type-filtered FR-8: Mapping
   Creation Drag-and-drop or click-to-map with visual connections and type
   validation FR-9: Unit of Measure Mapping For numeric fields: display analyzer
   unit, select OpenELIS unit, flag mismatches FR-10: Qualitative Result Mapping
   Map analyzer values to OpenELIS codes, support many-to-one, default handling
   FR-11: Copy Mappings Copy all field mappings from another analyzer with
   search and confirmation FR-12: Custom Field Types Administrators can add new
   field types with validation rules FR-13: Dynamic Target Fields Add new
   OpenELIS target fields directly from mapping interface

3. Business Rules BR-1: Analyzer Uniqueness • Analyzer name must be unique
   across the system • IP:Port combination generates warning if duplicate BR-2:
   Active Status • Only active analyzers receive orders • Inactive analyzers
   retain configuration • Status changes logged in audit trail BR-3: Test Unit
   Assignment • Minimum one test unit required • Test units control visibility
   in filtered views BR-4: Mapping Validation • Required mappings: Sample ID,
   Test Code, Result Value • Missing required mappings prevent activation BR-5:
   Field Type Compatibility • Error: text to numeric (blocked) • Warning:
   numeric to text (allowed with confirmation) • Error: missing unit for numeric
   fields (blocked) BR-6: Mapping Changes • Active analyzer changes require
   confirmation • Changes apply to new results only • Complete audit log
   maintained BR-7: Delete Protection • Analyzers with recent results cannot be
   hard deleted • Soft delete only (90-day window) BR-8: Connection Testing •
   Validates TCP handshake only • Timeout: 30 seconds • Displays latency and
   timestamp BR-9: Query Limitations • Query timeout: 5 minutes • Maximum 500
   fields per query • Rate limit: 1 query per minute per analyzer BR-10: Copy
   Mappings Validation • Source must have active mappings • Overwrites existing
   mappings with confirmation • Type incompatibilities generate warnings

4. User Stories Epic 1: Analyzer Setup US-1.1: Register New Analyzer As a
   laboratory administrator I want to register a new analyzer in the system So
   that I can receive results from that instrument Acceptance Criteria: • Given
   I'm on the Analyzers page • When I click "Add Analyzer" • Then I see a modal
   with connection and configuration fields • And I can save the analyzer after
   validation US-1.2: Test Analyzer Connection As a laboratory administrator I
   want to test connectivity to an analyzer So that I can verify the
   configuration before going live Acceptance Criteria: • Given I have entered
   analyzer connection details • When I click "Test Connection" • Then the
   system attempts TCP connection • And displays success/failure with latency
   US-1.3: Manage Analyzer Status As a laboratory supervisor I want to activate
   or deactivate analyzers So that I can control which instruments receive
   orders Acceptance Criteria: • Given I'm viewing the analyzer list • When I
   toggle the status switch • Then the analyzer status changes immediately • And
   change is logged in audit trail Epic 2: Field Mapping US-2.1: Query Analyzer
   Fields As a laboratory administrator I want to query an analyzer for its
   available data fields So that I know what data can be mapped Acceptance
   Criteria: • When I click "Query Analyzer" • Then the system sends ASTM query
   • And displays all available fields US-2.2: Map Analyzer Fields As a
   laboratory administrator I want to map analyzer fields to OpenELIS data
   fields So that results are correctly stored Acceptance Criteria: • When I
   drag a source field to target • Then a visual connection is created • And
   mapping is validated for compatibility US-2.3: Map Numeric Results As a
   laboratory administrator I want to specify units of measure for numeric
   results So that values are correctly interpreted Acceptance Criteria: • When
   I create numeric mapping • Then I see unit dropdown • And unit mapping is
   saved US-2.4: Map Qualitative Results As a laboratory administrator I want to
   map qualitative interpretations So that text results are standardized
   Acceptance Criteria: • When I create qualitative mapping • Then I can map
   each value to OpenELIS code • And specify default for unmapped values US-2.5:
   Copy Mappings As a laboratory administrator I want to copy field mappings
   from similar analyzer So that I don't reconfigure identical instruments
   Acceptance Criteria: • When I click "Copy from existing analyzer" • Then I
   see searchable list of analyzers • And can confirm to copy all mappings Epic
   3: Analyzer Management User stories for searching, filtering, viewing
   details, and editing analyzer configurations. Epic 4: System Administration
   User stories for adding custom field types and viewing the error dashboard.

5. UI Design Guidance (Carbon Design System) All user interfaces must conform to
   IBM Carbon Design System standards. This ensures consistency, accessibility,
   and professional appearance. 5.1 Analyzers List Page Layout Structure: • Page
   header with "Analyzers" title and "Add Analyzer" primary action button •
   Search bar with filter dropdowns (Status, Test Unit, Analyzer Type) • Active
   filter pills below search bar • Data table with sortable columns and inline
   actions • Pagination controls at bottom Carbon Components: • <Header> - Page
   title and primary actions • <Search> - Text search (300ms debounce) •
   <MultiSelect> - Filter dropdowns • <Tag> - Filter pills (dismissible) •
   <DataTable> - Analyzer listing • <Toggle> - Status control • <OverflowMenu> -
   Row actions • <Pagination> - Navigation controls 5.2 Add/Edit Analyzer Modal
   Modal Configuration: • Type: <ComposedModal> medium size (640px) • Scrollable
   content with fixed action buttons • Form fields: <TextInput>, <Dropdown>,
   <NumberInput>, <MultiSelect> • Connection test button (ghost) in fieldset •
   Action buttons: Cancel (secondary), Save (primary) 5.3 Field Mapping
   Interface Two-Column Layout: Equal width panels (50/50) using CSS Grid. Stack
   vertically on mobile (<1024px). Source Panel (Left): • <Accordion> for field
   categories • Checkbox selection with field type <Tag> • ASTM segment
   reference (light gray) • Status bar: "X mapped • Y available" Target Panel
   (Right): • <Accordion> for entity categories • Checkboxes (checked when
   mapped) • Inline <Dropdown> for unit selection • Visual connection lines
   between mapped fields Field Type Colors: Type Color Token Numeric Blue
   $blue-60 Qualitative Purple $purple-60 Control Green $green-60 Melting Point
   Teal $teal-60 Date/Time Cyan $cyan-60 Text Gray $gray-60

5.4 Carbon Design Principles Color System: Use Carbon color tokens only. Never
hard-code hex values. Typography: • Page titles:
$heading-04 (32px) • Section
headers: $heading-03 (20px) • Body text: $body-01 (14px) • Helper text:
$label-01
(12px) Spacing: Use Carbon spacing tokens:
$spacing-05 (16px) standard,
$spacing-07 (32px) sections Accessibility (WCAG 2.1
AA): • Keyboard navigation for all interactive elements • Screen reader labels
for icon buttons • Color contrast ratio ≥ 4.5:1 • Focus indicators on all
controls

6. API Endpoints All API endpoints follow RESTful conventions and return JSON
   responses. Authentication required using OpenELIS security framework.
   Analyzer Management GET /api/analyzers Retrieve paginated list with filters •
   Query: search, status, testUnit[], page, pageSize • Authorization: LAB_USER

POST /api/analyzers Create new analyzer • Body: name, analyzerType, ipAddress,
port, testUnitIds • Authorization: LAB_ADMIN

PUT /api/analyzers/{id} Update existing analyzer • Body: Same as POST, all
fields optional • Authorization: LAB_ADMIN

DELETE /api/analyzers/{id} Soft delete analyzer • Validates no recent results •
Authorization: LAB_ADMIN

POST /api/analyzers/{id}/test-connection Test TCP connection • Returns: success,
latencyMs, timestamp • Authorization: LAB_ADMIN

Field Discovery & Mapping POST /api/analyzers/{id}/query-fields Query analyzer
for available fields • 5-minute timeout, returns array of field definitions •
Authorization: LAB_ADMIN

GET /api/analyzers/{id}/mappings Retrieve all mappings • Returns array of
mapping objects • Authorization: LAB_USER

POST /api/analyzers/{id}/mappings Create mappings (batch) • Body: Array of
mapping definitions • Authorization: LAB_ADMIN

POST /api/analyzers/{id}/mappings/copy Copy from another analyzer • Body:
sourceAnalyzerId • Authorization: LAB_ADMIN

POST /api/analyzers/{id}/mappings/{mid}/qualitative-values Map qualitative
values • Body: Array of value mappings • Authorization: LAB_ADMIN

Error Dashboard GET /api/analyzer-errors Retrieve errors with filters • Query:
analyzerId, errorType, startDate, endDate, page • Authorization: LAB_SUPERVISOR

POST /api/analyzer-errors/{id}/acknowledge Acknowledge error • Returns 204 No
Content • Authorization: LAB_SUPERVISOR

POST /api/analyzer-errors/acknowledge-all Acknowledge multiple • Body: Optional
filters • Authorization: LAB_SUPERVISOR

7. Validation Rules Analyzer Validation Field Rule Error Message Name 1-100
   chars, unique Name required and must be unique Type Required Type is required
   IP Address Valid IPv4 Valid IP required (e.g., 192.168.1.10) Port 1-65535
   Port must be 1-65535 Test Units Min 1 At least one test unit required

Field Mapping Validation Field Rule Message Source Required, unique Source field
required and unique Target Required Target field required Unit Required for
numeric Unit of measure required Required Maps Sample ID, Test Code, Result
Missing required mappings

8. Security & Permissions Role-Based Access Control Role Permissions RESULTS
   View analyzers, mappings, errors SYSTEM_ADMIN All permissions + Create field
   types

Audit Trail All administrative actions logged with: action type, entity, user,
timestamp, IP, before/after values.

9. Testing Requirements 10. Testing Requirements Unit Tests • All service
   methods • Validation logic • Type compatibility • Coverage: 80% Integration
   Tests • API endpoints • Database transactions • ASTM integration • Coverage:
   70% UI Tests • Form validation • Search/filter • Mapping interface • Modal
   interactions UAT • Complete workflows • Real analyzer testing • Laboratory
   staff validation Performance • 1000 analyzers load test • Query timeout
   handling • Concurrent users

10. Future Enhancements (Out of Scope) Automated Field Matching ML-based mapping
    suggestions from historical data Bi-directional Sync Real-time status
    updates from analyzers Advanced Transformations Complex transformation rules
    with formulas Multi-Protocol Support HL7, FHIR in addition to ASTM Analyzer
    Groups Manage identical analyzers with shared mappings Visual Data Flow
    Graphical representation of data flow Import/Export Backup and share
    configurations as JSON/XML Scheduled Queries Automatic periodic capability
    queries Analytics Dashboard Utilization and performance metrics

11. Appendices Appendix A: ASTM LIS2-A2 Field Reference Common ASTM segments and
    fields for mapping reference. Patient (P) Segment: • P|1: Sequence Number •
    P|3: Patient ID • P|6: Patient Name • P|8: Date of Birth • P|9: Patient Sex
    Order (O) Segment: • O|1: Sequence Number • O|2: Sample ID / Accession
    Number • O|4: Universal Test ID • O|6: Ordered Date/Time • O|15: Sample Type
    Result (R) Segment: • R|1: Sequence Number • R|2: Universal Test ID • R|3:
    Data/Measurement Value • R|4: Units • R|6: Result Abnormal Flag • R|7:
    Result Status Comment (C) Segment: • C|1: Sequence Number • C|2: Comment
    Source • C|3: Comment Text Appendix B: Carbon Design System Resources •
    Component Library: https://carbondesignsystem.com/components/overview/ •
    Icons: https://carbondesignsystem.com/guidelines/icons/library/ • Design
    Patterns: https://carbondesignsystem.com/patterns/overview/ • React Package:
    @carbon/react npm package Appendix C: Glossary • ASTM: American Society for
    Testing and Materials • LIS2-A2: Laboratory Instrument Standard 2, version
    A2 • Analyzer: Clinical laboratory instrument for automated testing • Field
    Mapping: Association between analyzer and OpenELIS fields • Test Unit:
    Organizational laboratory unit (e.g., Hematology) • Qualitative Result:
    Non-numeric result (e.g., Positive/Negative) • Unit of Measure: Standard
    measurement unit (e.g., mg/dL) • Carbon Design System: IBM open-source
    design system • Soft Delete: Marking record deleted without physical removal
    Appendix D: Common Units of Measure Concentration: • mg/dL • g/dL • mmol/L •
    μmol/L • ng/mL • IU/L Cell Counts: • 10^3/μL • 10^6/μL • 10^9/L • cells/μL •
    % Temperature: • °C • °F Time: • seconds • minutes • hours Volume: • mL • μL
    • L
