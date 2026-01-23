# Optional Remediation Checklist

**Purpose**: Quality improvements for spec/plan/tasks documentation **Created**:
2025-11-09 **Based on**: /speckit.analyze analysis report **Priority**:
OPTIONAL - These can be addressed post-POC or as time allows

---

## High Priority (Recommended Before Implementation)

### CHK004 - Add Quantitative Barcode Acceptance Criteria

- **Issue**: Barcode workflows extensively specified (30+ clarifications in
  spec.md Session 2025-11-06) but lack measurable acceptance criteria for key
  behaviors
- **Impact**: Implementers may make different assumptions about
  performance/behavior without clear quantitative requirements
- **Files**:
  - specs/001-sample-storage/spec.md (lines 205-270, Barcode Workflows
    clarification session)
- **Actions**:
  1. Add quantitative AC to barcode requirements:
     - "Barcode scan populates location fields within 200ms" (performance)
     - "Debouncing prevents duplicate scans within 500ms window" (behavior)
     - "Visual feedback appears within 100ms of scan" (UX responsiveness)
     - "Error message displays for <5 seconds before auto-dismiss" (UX timing)
     - "Failed scan recovery allows rescan after error clears" (recovery flow)
  2. Add barcode test task with quantitative validation:
     - Task: "Write barcode performance test validating 200ms population time"
     - Task: "Write debouncing test validating 500ms duplicate prevention"
     - Task: "Write visual feedback test validating 100ms response time"
- **Priority**: HIGH (prevents ambiguity during implementation)
- **Status**: ⏸️ OPTIONAL

---

### CHK005 - Add Navigation Menu Integration Tasks

- **Issue**: FR-009a through FR-009d specify storage link in main side
  navigation menu (position, roles, navigation target) but no tasks implement
  this integration
- **Impact**: Navigation feature might be forgotten or implemented incorrectly
  without explicit task
- **Files**:
  - specs/001-sample-storage/spec.md (lines 872-880, FR-009a-d)
  - specs/001-sample-storage/tasks.md (missing task)
- **Actions**:
  1. Add task to Phase 1 or Phase 5 (US1) of tasks.md:
     ```markdown
     - [ ] T0XX Update main navigation menu component to add Storage link below
           Patients menu item - Add storage icon to navigation - Position
           directly below "Patients" menu item per FR-009b - Restrict access to
           Technician, Lab Manager, Admin roles per FR-009c - Navigate to
           Storage Dashboard on click per FR-009d - Test: Verify link appears
           for authorized roles, verify navigation works
     ```
  2. Verify navigation requirement coverage in test tasks
- **Priority**: HIGH (required feature, currently no implementation task)
- **Status**: ⏸️ OPTIONAL

---

### CHK006 - Consolidate Validation Requirements

- **Issue**: Validation logic duplicated across user stories - "Position
  occupied" error in US1 acceptance #2 AND US2B acceptance #2, "Inactive
  location" error in US1 #3 and US2B #3
- **Impact**: Duplication creates maintenance burden, potential for
  inconsistency
- **Files**:
  - specs/001-sample-storage/spec.md (lines 314-380 US1 acceptance, lines
    508-528 US2B acceptance)
- **Actions**:

  1. Create dedicated "Validation Requirements" section in spec.md (after FR
     requirements):

     ```markdown
     ### Validation Requirements

     **VLD-001: Position Occupancy Validation**

     - System MUST prevent assignment to occupied position
     - Error message: "Position {coordinate} in {rack} is already occupied by
       sample {sampleId}"
     - Applies to: US1 (assignment), US2B (movement)

     **VLD-002: Active Location Validation**

     - System MUST prevent assignment to inactive/decommissioned locations
     - Error message: "Cannot assign to inactive location. Please select an
       active location."
     - Applies to: US1 (assignment), US2B (movement)

     **VLD-003: Minimum Hierarchy Validation**

     - System MUST require at least 2 levels (Room + Device) for valid
       assignment
     - Error message: "A valid location requires at least Room and Device to be
       selected"
     - Applies to: US1 (assignment), US2B (movement)
     ```

  2. Update user story acceptance scenarios to reference validation
     requirements:
     - US1 Scenario #2: "Given position A5 is occupied, When assigning sample,
       Then system displays VLD-001 error"
     - US1 Scenario #3: "Given location is inactive, When assigning sample, Then
       system displays VLD-002 error"
  3. Remove duplication from US2B acceptance scenarios

- **Priority**: HIGH (reduces duplication, improves maintainability)
- **Status**: ⏸️ OPTIONAL

---

## Medium Priority (Quality Improvements)

### CHK007 - Create Barcode Format Support Matrix

- **Issue**: Barcode format specifications inconsistent - Code 128 specified as
  primary (L208) but also mentions "auto-detect format" and "format pattern
  matching" (L253)
- **Impact**: Unclear which barcode formats are supported in POC vs post-POC
- **Files**:
  - specs/001-sample-storage/spec.md (lines 207-208, 253)
- **Actions**:

  1. Add Barcode Format Support Matrix to spec clarification section:

     ```markdown
     ### Barcode Format Support Matrix

     **POC Scope**:

     - ✅ Code 128 (primary format for storage location labels)
     - ✅ Hyphen-delimited hierarchical path format: ROOM-DEVICE-SHELF-RACK

     **Post-POC Scope**:

     - ⏸️ QR Code support (2D barcodes)
     - ⏸️ Data Matrix support (2D barcodes)
     - ⏸️ EAN-13 support (1D product codes)
     - ⏸️ Code 39 support (1D barcodes)

     **Auto-Detection** (Post-POC):

     - System will auto-detect barcode format type (1D vs 2D)
     - POC assumes all barcodes are Code 128 format
     ```

  2. Update plan.md barcode section to reference matrix

- **Priority**: MEDIUM (clarifies scope, prevents feature creep)
- **Status**: ⏸️ OPTIONAL

---

### CHK008 - Add Terminology Glossary

- **Issue**: "Position" has dual meaning after Phase 4 architecture change -
  hierarchy level entity vs text field for location within rack
- **Impact**: May confuse implementers reading spec, plan, and tasks
- **Files**:
  - specs/001-sample-storage/spec.md (throughout)
  - specs/001-sample-storage/plan.md (throughout)
  - specs/001-sample-storage/tasks.md (Phase 4 context)
- **Actions**:

  1. Add Terminology Glossary section to spec.md (after Executive Summary):

     ```markdown
     ## Terminology Glossary

     **Storage Hierarchy Levels**:

     - **Room**: Top-level physical location (e.g., "Main Laboratory", "Storage
       Room 2")
     - **Device**: Storage equipment (e.g., "Freezer Unit 1", "Refrigerator 2",
       "Cabinet A")
     - **Shelf**: Shelving unit within device (e.g., "Shelf-A", "Shelf-1")
     - **Rack**: Container on shelf (e.g., "Rack R1", "Box B3")
     - **Position** (entity - deprecated): Legacy hierarchy level entity
       representing exact location within rack
     - **Position Coordinate** (text field): Free-text field for location within
       rack/shelf/device (e.g., "A5", "1-1", "RED-12")

     **Assignment Architecture**:

     - **Polymorphic Location**: Simplified assignment using locationId +
       locationType (device/shelf/rack)
     - **Minimum 2 Levels**: All assignments require at least Room + Device
     - **Optional Levels**: Shelf, Rack, and Position Coordinate are optional

     **Note**: After Phase 4 architecture change (flexible assignment), samples
     are assigned to devices/shelves/racks using polymorphic references, with
     optional positionCoordinate text field. The StoragePosition entity exists
     for hierarchy structure but is not required for sample assignment.
     ```

  2. Reference glossary in plan.md and tasks.md where terminology is used

- **Priority**: MEDIUM (improves clarity, prevents confusion)
- **Status**: ⏸️ OPTIONAL

---

### CHK009 - Clarify Occupancy Color-Coding Application

- **Issue**: Occupancy display uses "Color coding (optional): Green (<70%),
  Yellow (70-90%), Red (>90%)" (spec.md L666) but clarification 2025-11-20
  specifies color-coding for location type badges (blue/teal/purple/orange). Two
  different color schemes unclear where each applies.
- **Impact**: UI implementation may apply wrong color scheme to wrong elements
- **Files**:
  - specs/001-sample-storage/spec.md (line 666, lines 89-102 clarification
    session)
- **Actions**:

  1. Update spec.md L666 to distinguish two color-coding use cases:

     ```markdown
     **Occupancy Display (Dashboard Tables)**:

     - Fraction (occupied/total): "287/500"
     - Percentage: "57%"
     - Visual progress bar: Shows 57% filled
     - **Occupancy Color-Coding** (OPTIONAL - accessibility consideration):
       - Green progress bar fill (<70% occupancy)
       - Yellow progress bar fill (70-90% occupancy)
       - Red progress bar fill (>90% occupancy)
       - Note: Color-coding is optional to ensure accessibility. Percentage text
         always displayed.

     **Location Type Badges (Metrics Card & Tabs)**:

     - **Location Type Color-Coding** (MANDATORY per clarification 2025-11-20):
       - Rooms: blue-70 (Carbon token)
       - Devices: teal-70 (Carbon token)
       - Shelves: purple-70 (Carbon token)
       - Racks: orange-70 (Carbon token)
     - Applied to: Metric card text AND tab labels/backgrounds
     - Purpose: Visual distinction of location types, colorblind-friendly
     ```

  2. Add FR requirement distinguishing the two color schemes:
     - FR-XXX: Occupancy color-coding is optional for accessibility
     - FR-YYY: Location type color-coding is mandatory per design system

- **Priority**: MEDIUM (prevents UI implementation errors)
- **Status**: ⏸️ OPTIONAL

---

### CHK010 - Clarify Print History Scope

- **Issue**: "Print History" feature mentioned in spec.md (L262-264) but unclear
  if it's POC scope or post-POC
- **Impact**: Plan.md doesn't list print history implementation tasks, creating
  scope ambiguity
- **Files**:
  - specs/001-sample-storage/spec.md (lines 262-264)
  - specs/001-sample-storage/plan.md (no print history tasks)
- **Actions**:

  1. If print history is POST-POC, add to "Deferred to Post-POC" section:

     ```markdown
     **Deferred to Post-POC**:

     - ⏸️ **Print History Tracking**: Record basic print audit trail (who
       printed, when, for which location entity). Store in audit/history table.
       Display in Label Management modal. Useful for compliance and
       troubleshooting label issues. (Per clarification Session 2025-11-06, Q:
       "Should the system track print history?")
     ```

  2. If print history is POC, add tasks to Phase 10 (Barcode workflows):
     - Task: Create print_history table (Liquibase changeset)
     - Task: Record print events in service layer
     - Task: Display print history in Label Management modal

- **Priority**: MEDIUM (clarifies scope, prevents feature creep)
- **Status**: ⏸️ OPTIONAL

---

## Low Priority (Nice to Have)

### CHK011 - Consolidate Edge Cases Section

- **Issue**: Several edge cases in "Edge Cases" section (spec.md L722-780)
  already covered in acceptance scenarios, creating duplication
- **Impact**: Minor - duplication creates maintenance burden but doesn't affect
  implementation
- **Files**:
  - specs/001-sample-storage/spec.md (lines 722-780 Edge Cases, scattered in
    acceptance scenarios)
- **Actions**:
  1. Keep edge cases section for complex scenarios only:
     - Barcode disambiguation (unique complex scenario)
     - Capacity threshold warnings (multi-tier behavior)
     - Bulk move with insufficient capacity (complex validation)
     - Search performance with 100k+ samples (scale consideration)
  2. Remove edge cases already in acceptance scenarios:
     - Concurrent access conflict → Already in US1/US2B acceptance
     - Disposed sample movement attempt → Already in US2B acceptance
     - Inactive location validation → Already in US1/US2B acceptance
  3. Reference acceptance scenarios for standard validation edge cases
- **Priority**: LOW (cosmetic improvement)
- **Status**: ⏸️ OPTIONAL

---

### CHK012 - Rename E2E Test Scenarios Section

- **Issue**: Section title "E2E Test Scenarios" (spec.md L781-831) suggests
  these are Cypress tests, but Constitution V.5 mandates individual test
  execution. Unclear if these are Jest unit tests or Cypress E2E tests.
- **Impact**: Minor - title may confuse readers about test type
- **Files**:
  - specs/001-sample-storage/spec.md (line 781)
- **Actions**:

  1. Rename section to "End-to-End Test Requirements"
  2. Add clarification note:

     ```markdown
     ## End-to-End Test Requirements

     **Note**: These are test objectives, not literal Cypress test files.
     Implemented as individual Cypress E2E test files per Constitution V.5
     (individual execution during development, full suite in CI/CD only).
     ```

- **Priority**: LOW (improves clarity marginally)
- **Status**: ⏸️ OPTIONAL

---

## Summary

**Total Optional Items**: 9 **Priority Breakdown**:

- HIGH: 3 items (CHK004-006) - Recommended before implementation
- MEDIUM: 5 items (CHK007-011) - Quality improvements
- LOW: 1 item (CHK012) - Cosmetic

**Recommendation**:

- Address HIGH priority items (CHK004-006) before `/speckit.implement` if time
  allows
- MEDIUM/LOW items can be addressed post-POC during documentation cleanup phase

**Completion Tracking**:

- [ ] CHK004 - Barcode acceptance criteria
- [ ] CHK005 - Navigation menu tasks
- [ ] CHK006 - Validation requirements consolidation
- [ ] CHK007 - Barcode format matrix
- [ ] CHK008 - Terminology glossary
- [ ] CHK009 - Occupancy color-coding clarification
- [ ] CHK010 - Print history scope
- [ ] CHK011 - Edge cases consolidation
- [ ] CHK012 - E2E section rename
