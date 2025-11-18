# Feature Specification: Sample Metadata Tags

**Feature Branch**: `006-sample-metadata-tags`
**Created**: 2025-11-18
**Status**: Draft
**Input**: User description: "Add configurable tags to clinical lab samples to add metadata to samples (e.g., fasting, pregnant, timed) with typeahead search UI and inheritance to child samples"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Apply Metadata Tags to Sample (Priority: P1)

A lab technician receives a blood sample from a patient who has been fasting for 12 hours. The technician needs to record this important context on the sample record so that pathologists interpreting test results later understand the patient's state at time of collection.

**Why this priority**: Core functionality that delivers immediate value. Without the ability to add tags to samples, the feature provides no value. This story represents the minimal viable product.

**Independent Test**: Can be fully tested by adding a sample through the existing workflow, selecting tags from the typeahead search, and verifying those tags are saved and displayed with the sample. Delivers immediate value by allowing critical metadata capture.

**Acceptance Scenarios**:

1. **Given** a lab technician is adding a new sample, **When** they access the metadata tags field and type "fast", **Then** the typeahead displays matching tags including "fasting" from the data dictionary
2. **Given** a lab technician has selected "fasting" and "timed" tags, **When** they save the sample, **Then** both tags are persisted and visible on the sample record
3. **Given** a lab technician is editing an existing sample, **When** they add new tags or remove existing tags, **Then** the changes are saved and reflected immediately
4. **Given** a sample has metadata tags applied, **When** a user views the sample details, **Then** all applied tags are clearly displayed
5. **Given** a sample has finalized results, **When** a user attempts to remove tags, **Then** the system prevents removal and displays an appropriate message

---

### User Story 2 - Create New Metadata Tags (Priority: P2)

A lab supervisor identifies that a new metadata tag "on medication" is needed to track samples from patients currently on specific treatments. Rather than waiting for IT support, the supervisor can quickly create this new tag through the same interface used for applying tags.

**Why this priority**: Enables self-service configuration and flexibility. While not required for the initial MVP, this empowers users to adapt the system to evolving needs without developer intervention.

**Independent Test**: Can be fully tested by accessing the tag creation interface from within the sample workflow, creating a new tag, and immediately using it on a sample. Delivers value by eliminating dependency on technical staff for common configuration changes.

**Acceptance Scenarios**:

1. **Given** a lab technician is using the typeahead search for tags, **When** they type a tag name that doesn't exist and select "Add new tag", **Then** a new tag is created in the data dictionary and immediately available for use
2. **Given** a new tag is being created, **When** the user provides a tag name, **Then** the system validates uniqueness and saves it to the data dictionary
3. **Given** a newly created tag, **When** the user applies it to a sample, **Then** it behaves identically to pre-existing tags
4. **Given** a user attempts to create a tag with a name that already exists (case-insensitive match), **When** they submit the new tag, **Then** the system displays an error message and suggests using the existing tag

---

### User Story 3 - View Tags on Results Screen (Priority: P1)

A pathologist is reviewing test results for a glucose test. The results screen displays that the sample was tagged as "fasting", providing critical context for result interpretation. The pathologist can immediately understand that the elevated glucose reading is concerning because it occurred while fasting.

**Why this priority**: Completes the value chain of the feature. Capturing metadata is only useful if it's visible at the point of decision-making. This story is equally critical as P1 because it represents the consumption side of the metadata.

**Independent Test**: Can be fully tested by creating a sample with tags, running tests on that sample, and viewing the results screen to verify tags are displayed. Delivers value by providing context that directly impacts clinical decision-making.

**Acceptance Scenarios**:

1. **Given** a test result is displayed for a sample with metadata tags, **When** a pathologist views the results screen, **Then** all sample metadata tags are clearly visible alongside the test results
2. **Given** multiple samples are displayed in a results list, **When** each sample has different tags, **Then** each sample's tags are displayed distinctly and accurately
3. **Given** a sample has no metadata tags, **When** viewing results, **Then** the tag display area is not shown or shows an empty state

---

### User Story 4 - Child Samples Inherit Parent Tags (Priority: P2)

A lab creates aliquots (child samples) from a primary blood sample that was collected while the patient was fasting. All aliquots automatically inherit the "fasting" tag, ensuring this critical context follows the sample through all downstream processing and testing.

**Why this priority**: Reduces data entry burden and prevents errors from forgotten tags. While important for workflow efficiency, the system can function without automatic inheritance if users manually apply tags to child samples.

**Independent Test**: Can be fully tested by creating a parent sample with tags, creating child samples from it, and verifying the tags are automatically applied to children. Delivers value by ensuring metadata consistency across sample hierarchies.

**Acceptance Scenarios**:

1. **Given** a parent sample has metadata tags "fasting" and "pregnant", **When** a child sample (aliquot) is created from it, **Then** the child sample automatically has both "fasting" and "pregnant" tags applied
2. **Given** a child sample has inherited tags from a parent, **When** additional tags are added to the child, **Then** the child displays both inherited and manually added tags
3. **Given** tags on a parent sample are modified after child samples exist, **When** viewing the child samples, **Then** child sample tags remain unchanged (snapshot model - tags are frozen at time of child creation)

---


## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST provide a typeahead search interface for metadata tags within the sample creation/editing workflow
- **FR-002**: System MUST retrieve tag options from the existing data dictionary system
- **FR-003**: System MUST allow users to quickly add a new tag when a desired tag is not found in search results
- **FR-004**: Newly created tags MUST be immediately available for selection without page refresh
- **FR-005**: System MUST persist selected metadata tags with the sample record
- **FR-006**: System MUST display sample metadata tags on the results screen when showing test results for that sample
- **FR-007**: Child samples MUST automatically inherit all metadata tags from their parent sample at the time of creation
- **FR-008**: Users MUST be able to add additional tags to child samples beyond inherited tags
- **FR-009**: Users MUST be able to remove tags from samples (both inherited and manually added) only if results have not been finalized
- **FR-009a**: System MUST prevent tag removal from samples once results are finalized (immutable state)
- **FR-010**: System MUST support multiple tags per sample with no enforced maximum limit (minimum support for 10 tags per sample)
- **FR-011**: Tag search MUST be case-insensitive and support partial matching
- **FR-012**: System MUST prevent duplicate tags from being applied to the same sample
- **FR-013**: System MUST display inherited tags distinctly from manually added tags when viewing a child sample
- **FR-014**: System MUST support soft deletion of tags - deleted tags remain visible on existing samples with an "archived" or "deleted" indicator but cannot be applied to new samples
- **FR-015**: Typeahead search MUST NOT include deleted/archived tags in search results
- **FR-016**: Typeahead search results MUST be limited to the first 10-15 matching results with scroll capability or "show more" option
- **FR-017**: System MUST prevent creation of duplicate tag names (case-insensitive) and display an error message suggesting the existing tag when duplication is attempted

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - include only relevant principles for this feature:_

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO custom CSS frameworks (applies to typeahead search component and tag display)
- **CR-002**: All UI strings MUST be internationalized via React Intl (no hardcoded text for labels, placeholders, buttons)
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** for sample metadata tag entities
- **CR-004**: Database changes MUST use Liquibase changesets for any new tables/columns to support tag associations
- **CR-007**: Security: RBAC for tag creation permissions, audit trail (sys_user_id + lastupdated) for tag creation and sample tag modifications, input validation on tag names
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage goal)

### Key Entities _(include if feature involves data)_

- **Sample Metadata Tag**: Represents a reusable tag stored in the data dictionary (e.g., "fasting", "pregnant", "timed"). Contains tag name and standard data dictionary attributes.
- **Sample Tag Association**: Links a sample to one or more metadata tags. Contains reference to sample, reference to tag, timestamp of application, user who applied it, and whether it was inherited from parent.
- **Sample**: Existing entity that will have a collection of associated metadata tags and a reference to parent sample (if applicable).

## Clarifications

### Session 2025-11-18

- Q: What happens when a tag is deleted from the data dictionary but is still referenced by existing samples? → A: Tags on existing samples remain visible but marked as "archived/deleted" (soft delete pattern - tag shown with indicator, cannot be applied to new samples)
- Q: Can tags be removed from samples after results have been finalized? → A: Tags cannot be removed after results are finalized (immutable after finalization)
- Q: What happens if the typeahead search returns too many matching results to display comfortably? → A: Display first 10-15 results with scroll or "show more" (standard UI pattern with pagination)
- Q: What happens when a user attempts to create a new tag with a name that already exists? → A: Show error message and suggest existing tag (user-friendly with guidance)
- Q: How many tags can be applied to a single sample (is there a limit)? → A: No hard limit enforced

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Lab technicians can apply metadata tags to a sample in under 10 seconds using the typeahead interface
- **SC-002**: Users can create a new metadata tag and apply it to a sample in under 30 seconds without leaving the sample workflow
- **SC-003**: 100% of child samples automatically inherit parent sample tags at time of creation
- **SC-004**: All metadata tags are visible on the results screen within 1 second of the screen loading
- **SC-005**: 95% of tag searches return relevant results with fewer than 5 characters typed
- **SC-006**: Tag search results appear within 500 milliseconds of typing

## Assumptions

- The data dictionary system has existing infrastructure for storing and managing configurable values that can be leveraged for tag storage
- Users have appropriate permissions to create new data dictionary entries (this may be role-restricted)
- The existing sample workflow has extension points where the tag interface can be integrated
- The results screen has space to display metadata tags without requiring significant UI redesign
- Tag inheritance should occur at child sample creation time (not retroactively)
- Tags are display-only metadata and do not trigger automated business logic or calculations
- Standard data retention policies apply to tags (they persist with the sample record)
- Tag names should be validated for reasonable length (assuming max 50 characters)
- Case-insensitive uniqueness is enforced for tag names (cannot have both "Fasting" and "fasting")

## Dependencies

- Existing data dictionary system and APIs
- Existing sample creation/editing workflow and UI components
- Existing results screen and display infrastructure
- Existing parent-child sample relationship data model
- Carbon Design System typeahead component availability

## Out of Scope

- Bulk tag application to multiple samples simultaneously
- Tag categories or hierarchical organization (all tags are flat list)
- Tag-based filtering or searching of samples
- Predefined tag suggestions based on test type or sample type
- Tag analytics or reporting (e.g., "show all fasting samples")
- Tag versioning or historical tracking beyond standard audit fields
- Import/export of tag definitions
- Tag synonyms or aliases
- Automatic tag suggestion based on previous samples from the same patient
