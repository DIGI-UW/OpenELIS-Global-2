# Feature Specification: Sample Management Menu

**Feature Branch**: `001-sample-management` **Created**: 2025-11-20 **Status**:
Draft **Input**: User description: "Add a new menu for sample management with
the following features:

1. Search for sample items by accession number (similar to current
   implementation)
2. Add multiple tests to a given sample item
3. Aliquot sample items to create new sample items:
   - New sample items get external ID with .{aliquot_number} suffix
   - Parent-child relationship tracking
   - Track original quantity and remaining quantity columns
   - Reduce parent sample quantity when aliquoting
   - Warning when attempting to aliquot with 0 remaining quantity ('all volume
     dispensed')
4. Add tests to aliquots in bulk
5. Order the same type of tests on multiple sample items at once

Technical requirements:

- Follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form)
- Use Carbon Design System for UI components
- Use React Intl for all user-facing strings
- FHIR R4 compliance for external-facing entities
- Liquibase for any schema changes
- @Transactional in services only
- TDD workflow (Red-Green-Refactor)
- Individual E2E tests for each feature"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Search Sample Items by Accession Number (Priority: P1)

Laboratory technicians need to quickly locate sample items using accession
numbers to verify sample status, view associated tests, and perform sample
management operations.

**Why this priority**: This is the foundation for all other sample management
features. Without the ability to search and retrieve samples, no other
operations can be performed. This delivers immediate value by providing a
dedicated interface for sample lookup.

**Independent Test**: Can be fully tested by entering an accession number in the
search interface and verifying that the correct sample item(s) are displayed
with their current status and metadata. Delivers value by enabling users to
locate samples without navigating through complex menus.

**Acceptance Scenarios**:

1. **Given** a sample exists with accession number "2025-001234", **When** the
   user enters "2025-001234" in the search field and clicks search, **Then** the
   sample item is displayed with its details (sample type, collection date,
   current status, quantity)
2. **Given** no sample exists with accession number "2025-999999", **When** the
   user searches for "2025-999999", **Then** a message displays "No samples
   found with this accession number"
3. **Given** multiple sample items exist for accession number "2025-001234"
   (parent and aliquots), **When** the user searches for "2025-001234", **Then**
   all related sample items are displayed showing their parent-child
   relationships
4. **Given** a user enters a partial accession number "2025-00", **When** the
   search is performed, **Then** all samples with matching prefix are displayed
   or autocomplete suggestions are shown

---

### User Story 2 - Add Multiple Tests to Sample Items (Priority: P1)

Laboratory staff need to add multiple test orders to a single sample item or
multiple sample items at once to efficiently process sample testing workflows
without repetitive data entry.

**Why this priority**: This is a core operational requirement that directly
impacts laboratory efficiency. Technicians commonly need to order multiple tests
on samples, and doing so one-by-one is time-consuming and error-prone. This can
be tested and deployed independently of aliquoting features.

**Independent Test**: Can be tested by loading a sample item, selecting multiple
tests from available test catalog, and verifying that all selected tests are
associated with the sample item. Delivers value by reducing test ordering time
from minutes to seconds.

**Acceptance Scenarios**:

1. **Given** a sample item is loaded in the interface, **When** the user selects
   multiple tests (e.g., "CBC", "Chemistry Panel", "HIV Test") from the test
   catalog and clicks "Add Tests", **Then** all selected tests are added to the
   sample item and displayed in the tests list
2. **Given** multiple sample items are selected (e.g., 3 blood samples),
   **When** the user selects a test (e.g., "Malaria") and clicks "Add Test to
   All", **Then** the same test is added to all selected sample items
3. **Given** a sample item already has a test ordered, **When** the user
   attempts to add the same test again, **Then** a warning displays "This test
   is already ordered for this sample" and prevents duplicate addition
4. **Given** a sample item type is "Urine", **When** the user views the test
   catalog, **Then** only tests compatible with urine samples are available for
   selection

---

### User Story 3 - Aliquot Sample Items with Volume Tracking (Priority: P2)

Laboratory technicians need to create child sample items (aliquots) from a
parent sample by dividing its volume, while maintaining accurate tracking of
original and remaining quantities to prevent over-dispensing.

**Why this priority**: Aliquoting is essential for laboratories that need to
split samples for different tests or storage. While important, it can be
implemented after basic search and test ordering are in place. It's
independently testable and provides clear value in sample inventory management.

**Independent Test**: Can be tested by selecting a parent sample with quantity
10mL, creating an aliquot of 3mL, and verifying that: (a) parent remaining
quantity updates to 7mL, (b) new aliquot is created with external ID
"originalID.1", (c) parent-child relationship is recorded. Delivers value by
automating volume calculations and preventing dispensing errors.

**Acceptance Scenarios**:

1. **Given** a sample item with external ID "SAMPLE001" has original quantity
   10mL and remaining quantity 10mL, **When** the user creates an aliquot of
   3mL, **Then** a new sample item is created with external ID "SAMPLE001.1",
   quantity 3mL, and the parent's remaining quantity is reduced to 7mL
2. **Given** a sample item has remaining quantity 2mL, **When** the user
   attempts to create an aliquot of 5mL, **Then** an error displays "Cannot
   aliquot: requested volume (5mL) exceeds remaining volume (2mL)"
3. **Given** a sample item has remaining quantity 0mL, **When** the user
   attempts to create an aliquot, **Then** a warning displays "All volume
   dispensed: no remaining volume available for aliquoting"
4. **Given** a parent sample "SAMPLE001" already has aliquots ".1" and ".2",
   **When** a new aliquot is created, **Then** it is numbered ".3" following the
   sequence
5. **Given** an aliquot "SAMPLE001.1" is created from parent "SAMPLE001",
   **When** viewing either sample, **Then** the parent-child relationship is
   visible (parent shows list of children, child shows parent reference)
6. **Given** a sample with remaining quantity equals original quantity (no
   aliquots yet), **When** the first aliquot is created, **Then** both "original
   quantity" and "remaining quantity" fields are tracked going forward
7. **Given** an aliquot "SAMPLE001.1" with 3mL remaining quantity, **When** the
   user creates an aliquot of 1mL from this aliquot, **Then** a new nested
   aliquot "SAMPLE001.1.1" is created with 1mL, and "SAMPLE001.1" remaining
   quantity is reduced to 2mL
8. **Given** a multi-level aliquot hierarchy exists (SAMPLE001 → SAMPLE001.1 →
   SAMPLE001.1.1), **When** viewing any sample in the hierarchy, **Then** the
   full lineage path is displayed showing all ancestors and descendants

---

### User Story 4 - Add Tests to Aliquots in Bulk (Priority: P3)

Laboratory staff need to add tests to multiple aliquot sample items
simultaneously to efficiently manage workflows where aliquots are designated for
different test types.

**Why this priority**: This is an efficiency enhancement that builds on
aliquoting (P2) and test ordering (P1). While valuable, it can be implemented
after the core aliquoting feature is stable. It's independently testable by
creating aliquots and batch-adding tests.

**Independent Test**: Can be tested by creating 3 aliquots, selecting all of
them, choosing a test, and verifying that the test is added to all 3 aliquots in
a single operation. Delivers value by reducing test ordering time for aliquots
from N operations to 1.

**Acceptance Scenarios**:

1. **Given** three aliquots exist (SAMPLE001.1, SAMPLE001.2, SAMPLE001.3),
   **When** the user selects all three and adds test "PCR", **Then** the PCR
   test is ordered for all three aliquots
2. **Given** five aliquots are selected, **When** the user adds multiple tests
   ("Culture", "Sensitivity") in bulk, **Then** both tests are added to all five
   aliquots
3. **Given** an aliquot already has a specific test ordered, **When** that test
   is added in bulk to multiple aliquots including this one, **Then** the
   duplicate is skipped for that aliquot with a notification "Test already
   ordered for SAMPLE001.1"
4. **Given** aliquots with different sample types are selected (e.g., blood and
   urine), **When** a blood-specific test is added in bulk, **Then** only
   compatible aliquots receive the test with warnings for incompatible ones

---

### Edge Cases

- What happens when a user tries to aliquot from an aliquot (creating a
  grandchild sample)? **System MUST support nested aliquoting with unlimited
  levels. External IDs follow pattern SAMPLE001.1.1.1 for multi-level hierarchy.
  Each level tracks its own original/remaining quantity independently.**
- How does the system handle concurrent aliquoting operations on the same parent
  sample by different users? **System MUST use database-level locking or
  optimistic concurrency control to prevent race conditions when updating parent
  remaining quantity.**
- What happens when a sample item is deleted after aliquots have been created
  from it? **System MUST either: (a) prevent deletion if children exist, or (b)
  cascade delete all descendants with user confirmation. Parent reference in
  orphaned aliquots would be invalid.**
- How should the system handle decimal precision for quantities (e.g., 0.5mL vs
  0.333333mL)? **System MUST support decimal quantities with precision up to 3
  decimal places (e.g., 0.333mL). UI should handle rounding appropriately for
  display.**
- What happens when a test is removed from a sample after aliquots have been
  created with that test? **Test removal from parent does NOT automatically
  remove tests from aliquots. Each sample item maintains independent test
  orders.**
- How should the system behave when searching for an aliquot by its full
  external ID (e.g., "SAMPLE001.2") vs. just the base ID ("SAMPLE001")?
  **Searching for base ID returns all related samples in hierarchy. Searching
  for full external ID with suffix returns exact match and its descendants.**
- What happens to aliquot numbering if an intermediate aliquot is voided/deleted
  (e.g., if .2 is deleted, does the next aliquot become .3 or .2)? **Numbering
  continues sequentially without reusing deleted numbers. If .2 is deleted, next
  aliquot is .3 to maintain audit trail and prevent ID confusion.**

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST provide a dedicated sample management menu accessible
  from the main navigation
- **FR-002**: System MUST allow users to search for sample items by entering a
  complete or partial accession number
- **FR-003**: System MUST display search results showing sample item details
  including: external ID, sample type, collection date, original quantity,
  remaining quantity, current status, and associated tests
- **FR-004**: System MUST allow users to select one or more tests from a catalog
  and add them to a sample item in a single operation
- **FR-005**: System MUST allow users to select multiple sample items and add
  the same test(s) to all of them simultaneously
- **FR-006**: System MUST prevent duplicate test orders on the same sample item
  and display an appropriate warning message
- **FR-007**: System MUST filter available tests based on sample type
  compatibility (e.g., only show urine-compatible tests for urine samples)
- **FR-008**: System MUST allow users to create an aliquot by specifying a
  quantity to transfer from a parent sample item
- **FR-009**: System MUST automatically generate external IDs for aliquots by
  appending ".{number}" to the parent's external ID, where {number} is
  sequential (1, 2, 3, etc.)
- **FR-010**: System MUST track two quantity fields for each sample item:
  "original quantity" (initial volume) and "remaining quantity" (current
  available volume)
- **FR-011**: System MUST reduce the parent sample's remaining quantity by the
  aliquot quantity when an aliquot is created
- **FR-012**: System MUST prevent aliquot creation when the requested quantity
  exceeds the parent's remaining quantity
- **FR-013**: System MUST display a warning "All volume dispensed" when
  attempting to aliquot a sample with zero remaining quantity
- **FR-014**: System MUST maintain a parent-child relationship between original
  samples and their aliquots
- **FR-015**: System MUST display parent-child relationships in the sample
  details view (parents show list of child aliquots, aliquots show parent
  reference)
- **FR-016**: System MUST allow users to select multiple aliquots and add tests
  to all of them in a single bulk operation
- **FR-017**: System MUST handle bulk test additions by skipping duplicates for
  individual aliquots and notifying the user of any skipped items
- **FR-018**: System MUST persist all sample management operations (searches,
  test additions, aliquoting) with proper audit trail information
- **FR-019**: System MUST support nested aliquoting with unlimited hierarchy
  levels (e.g., SAMPLE001 → SAMPLE001.1 → SAMPLE001.1.1 → SAMPLE001.1.1.1)
- **FR-020**: System MUST generate external IDs for nested aliquots by appending
  ".{number}" at each level (e.g., parent.1.2 means second aliquot of first
  aliquot)
- **FR-021**: System MUST allow aliquoting from any sample item regardless of
  whether it is an original sample or an aliquot itself
- **FR-022**: System MUST track the full lineage path for nested aliquots to
  enable recursive queries and display of complete family trees
- **FR-023**: System MUST use database-level locking or optimistic concurrency
  control to prevent race conditions during concurrent aliquoting operations
- **FR-024**: System MUST support decimal quantities with precision up to 3
  decimal places (e.g., 0.333mL)
- **FR-025**: System MUST continue aliquot numbering sequentially without
  reusing numbers from deleted/voided aliquots to maintain audit trail integrity

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - include only relevant
principles for this feature:_

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files -
    legacy exempt until refactored)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
- **CR-005**: External data integration MUST use FHIR R4 + IHE profiles for
  sample items and aliquot relationships
- **CR-006**: @Transactional annotations MUST only be used in service layer (NOT
  in controllers)
- **CR-007**: Services MUST compile all data within transaction boundaries to
  prevent LazyInitializationException
- **CR-008**: Security: RBAC for sample management operations, audit trail
  (sys_user_id + lastupdated), input validation for quantities and IDs
- **CR-009**: Tests MUST be included (unit + integration + E2E per feature, >70%
  coverage goal)
- **CR-010**: Individual E2E tests MUST be runnable independently during
  development (NOT full suite)

### Key Entities _(include if feature involves data)_

- **Sample Item**: Represents a physical sample or aliquot. Key attributes
  include external ID (with aliquot suffix if applicable), quantity (original
  and remaining), sample type, collection date, status, and parent reference
  (for aliquots). Relationships include parent-child linkage for aliquots and
  association with tests.
- **Sample Item Aliquot Relationship**: Represents the parent-child relationship
  between an original sample item and its aliquots. Tracks the parent sample
  item ID, child sample item ID, sequence number (for numbering like .1, .2,
  .3), and the quantity transferred.
- **Test Order**: Represents a test that has been ordered for a sample item.
  Links a specific test definition to a sample item, tracks order status, and
  prevents duplicate test orders.
- **Sample Management Search Result**: A view entity combining sample item data
  with related information (associated tests, parent/child relationships,
  current quantities) for display in search results.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory technicians can locate a sample by accession number in
  under 10 seconds from menu access to results display
- **SC-002**: Users can add 5 tests to a single sample item in under 30 seconds
  (compared to 2-3 minutes with individual test entry)
- **SC-003**: Users can create an aliquot and update parent quantity in a single
  transaction with zero calculation errors
- **SC-004**: System prevents 100% of over-dispensing attempts (aliquoting more
  volume than remaining quantity)
- **SC-005**: Users can add the same test to 10 aliquots in under 15 seconds
  using bulk operations (compared to 3-5 minutes individually)
- **SC-006**: 95% of sample management operations complete successfully without
  user errors or system errors
- **SC-007**: All parent-child sample relationships remain accurate and
  queryable across all sample management operations
- **SC-008**: Search results return within 2 seconds for databases with up to
  100,000 sample items
