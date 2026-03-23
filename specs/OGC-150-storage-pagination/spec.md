# Feature Specification: Sample Storage Pagination

**Feature Branch**: `OGC-150-storage-pagination`  
**Created**: 2025-12-05  
**Status**: In Progress  
**Input**: User description: "Add pagination to Sample Storage page for large
datasets"  
**Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)  
**Parent Feature**: [001-sample-storage](../001-sample-storage/spec.md)  
**Type**: Performance Enhancement

## Overview

Add server-side pagination to the Sample Storage Dashboard to support large
datasets (100,000+ samples) without performance degradation. This enhancement
addresses performance issues when the Sample Storage page loads all sample
storage assignments at once.

**Prerequisite**: Feature 001-sample-storage MUST be fully implemented and
merged to `develop` branch before this enhancement can begin.

## Problem Statement

Currently, the Sample Storage page (`/Storage/samples`) loads all sample storage
assignments at once, causing:

- **Slow page loads**: 10-20 seconds with 100,000+ samples
- **Browser memory issues**: High memory consumption affecting browser
  performance
- **Poor user experience**: Users wait for full dataset to load before seeing
  any results
- **Scalability concerns**: Performance degrades linearly with dataset size

Lab technicians need to access sample storage information quickly, but current
performance makes the system unusable for laboratories with large sample
volumes.

## User Scenarios & Testing

### User Story 1 - View Paginated Sample List (Priority: P1)

Lab technicians need to view sample storage assignments in manageable chunks
without waiting for the entire dataset to load.

**Why this priority**: This is the core functionality that addresses the
performance issue. Without pagination, the page is unusable for labs with large
sample volumes. This must work before any other enhancements.

**Independent Test**: Can be fully tested by loading the Sample Storage page
with a database containing 100,000+ samples and verifying that only the first 25
items load immediately (page load time <2 seconds).

**Acceptance Scenarios**:

1. **Given** the Sample Storage page with 100,000+ samples, **When** a lab
   technician navigates to `/Storage/samples`, **Then** only the first 25 sample
   storage assignments are displayed and the page loads in under 2 seconds
2. **Given** the Sample Storage page is loaded, **When** the lab technician
   views the pagination controls, **Then** they see the total number of pages,
   current page number, and navigation buttons (previous, next, page numbers)
3. **Given** the Sample Storage page displays 25 items, **When** the lab
   technician looks at the page, **Then** they see a page size selector showing
   "25 items per page" with options to change to 50 or 100 items

---

### User Story 2 - Navigate Between Pages (Priority: P1)

Lab technicians need to browse through different pages of sample storage
assignments to find specific samples.

**Why this priority**: Core navigation functionality. Without this, pagination
is useless. This is part of the MVP and must work for the feature to be viable.

**Independent Test**: Can be fully tested by clicking "Next" button and
verifying that the next 25 items load without a full page refresh, and the page
number increments.

**Acceptance Scenarios**:

1. **Given** the Sample Storage page showing items 1-25, **When** the lab
   technician clicks the "Next" button, **Then** items 26-50 are displayed and
   the current page indicator updates to "Page 2"
2. **Given** the Sample Storage page showing page 2, **When** the lab technician
   clicks the "Previous" button, **Then** items 1-25 are displayed and the
   current page indicator updates to "Page 1"
3. **Given** the Sample Storage page showing page 2, **When** the lab technician
   clicks on page number "5", **Then** items 101-125 are displayed and the
   current page indicator updates to "Page 5"
4. **Given** the Sample Storage page showing page 5, **When** the lab technician
   clicks "First" or page number "1", **Then** items 1-25 are displayed and the
   current page indicator updates to "Page 1"

---

### User Story 3 - Change Page Size (Priority: P2)

Lab technicians need to adjust how many items are displayed per page based on
their workflow preferences.

**Why this priority**: Enhances user experience but not critical for basic
functionality. The default 25 items per page works for most use cases. This can
be added after core pagination works.

**Independent Test**: Can be fully tested by selecting "50 items per page" from
the dropdown and verifying that 50 items are displayed and pagination controls
update accordingly.

**Acceptance Scenarios**:

1. **Given** the Sample Storage page displaying 25 items, **When** the lab
   technician selects "50" from the items per page dropdown, **Then** the page
   reloads showing 50 items and pagination controls update (e.g., total pages
   decreases from 4000 to 2000)
2. **Given** the Sample Storage page on page 3 with 25 items per page, **When**
   the lab technician changes to 100 items per page, **Then** the page resets to
   page 1 showing 100 items (prevents showing an invalid page number)
3. **Given** the Sample Storage page with 100 items per page selected, **When**
   the lab technician navigates to a different tab (e.g., "Rooms") and returns
   to "Samples" tab, **Then** the page size preference of 100 items per page is
   preserved

---

### Edge Cases

- **Empty dataset**: When no samples exist, display "No samples found" message
  instead of empty pagination controls
- **Single page of data**: When total samples ≤ page size (e.g., 20 samples with
  25 per page), hide pagination controls or show "Page 1 of 1" without
  navigation buttons
- **Last page partial data**: When on the last page with fewer items than page
  size (e.g., page 100 shows 13 items when page size is 25), display correctly
  without errors
- **Invalid page number**: When URL contains invalid page number (e.g., page
  9999 when only 100 pages exist), redirect to last valid page or show error
  message
- **Page state during filtering**: When user applies a filter that reduces total
  items (e.g., search reduces 10,000 items to 50), reset to page 1 to avoid
  showing empty results
- **Concurrent data changes**: When new samples are added while user is viewing
  page 5, ensure pagination remains stable (total pages may change but current
  page data remains consistent)

## Requirements

### Functional Requirements

- **FR-001**: System MUST load only the requested page of sample storage
  assignments from the database (server-side pagination), not the full dataset
- **FR-002**: System MUST display exactly 25 sample storage assignments per page
  by default
- **FR-003**: System MUST provide pagination controls matching the OpenELIS
  Global application pattern (consistent with NoteBook and other paginated
  pages)
- **FR-004**: Users MUST be able to select items per page from options: 25, 50,
  or 100
- **FR-005**: System MUST preserve the current page number when users navigate
  to other tabs and return to the Samples tab
- **FR-006**: System MUST preserve the selected page size when users navigate to
  other tabs and return to the Samples tab
- **FR-007**: System MUST display total number of pages based on total items and
  current page size
- **FR-008**: System MUST display current page number
- **FR-009**: System MUST provide "Next" and "Previous" navigation buttons
- **FR-010**: System MUST disable "Previous" button when on first page
- **FR-011**: System MUST disable "Next" button when on last page
- **FR-012**: System MUST provide direct page number navigation (clickable page
  numbers)
- **FR-013**: System MUST reset to page 1 when page size is changed
- **FR-014**: System MUST handle edge cases gracefully (empty data, single page,
  invalid page numbers)

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - applicable principles for this
feature:_

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) -
  specifically the `<Pagination>` component
- **CR-002**: Backend MUST follow 5-layer architecture (Service→Controller) -
  pagination logic in service layer using Spring Data JPA `Pageable`
- **CR-003**: Pagination MUST NOT use @Transactional in controllers -
  transactions belong in service layer only
- **CR-004**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal)
- **CR-005**: All UI strings MUST be internationalized via React Intl (Carbon
  Pagination component provides default labels, any custom messages must use
  React Intl)
- **CR-006**: Security: Pagination parameters MUST be validated to prevent
  injection attacks (e.g., negative page numbers, excessive page sizes)

### Non-Functional Requirements

- **NFR-001**: Page load time MUST be under 2 seconds even with 100,000+ samples
  in the database
- **NFR-002**: Pagination controls MUST be consistent with other paginated pages
  in OpenELIS Global (NoteBook, Search Results, etc.)
- **NFR-003**: Pagination MUST work correctly with existing Sample Storage
  Dashboard features (tabs, filters, search)
- **NFR-004**: Solution MUST NOT require database schema changes
- **NFR-005**: Solution MUST be backward compatible with existing Sample Storage
  functionality

### Key Entities

_This feature modifies existing entities from 001-sample-storage:_

- **SampleStorageAssignment**: Represents the assignment of a sample item to a
  storage location. Pagination retrieves a subset of these assignments based on
  page number and page size.

_No new entities are introduced by this feature._

## Success Criteria

### Measurable Outcomes

- **SC-001**: Sample Storage page loads in under 2 seconds even with 100,000+
  samples in the database (currently 10-20 seconds) — full performance
  validation will occur in follow-up performance testing after this increment
- **SC-002**: Lab technicians can view sample storage assignments without
  browser performance degradation (memory usage remains stable regardless of
  total sample count)
- **SC-003**: Lab technicians can navigate to any page within 1 second (next,
  previous, or direct page number) — timing metrics to be gathered during
  dedicated performance testing
- **SC-004**: 100% of lab technicians can successfully find specific samples
  using pagination navigation on first attempt (measured via user testing or
  support ticket reduction)
- **SC-005**: Pagination controls match the user experience of other paginated
  pages in OpenELIS Global (NoteBook module serves as reference implementation)

## Out of Scope

The following are explicitly NOT included in this feature:

- **Pagination for other tabs**: Location tabs (Rooms, Devices, Shelves, Racks)
  do not require pagination in this enhancement (can be addressed in future work
  if needed)
- **Advanced filtering with pagination**: Complex filters combined with
  pagination (basic search already works in 001, pagination works independently)
- **Infinite scroll alternative**: This feature implements traditional
  pagination controls, not infinite scroll
- **Customizable default page size**: Default is fixed at 25 items per
  requirement FR-002 (user preference persistence could be added later)
- **Search results pagination**: Search functionality from 001-sample-storage
  already implemented; this focuses on the main Samples tab list

## Assumptions

- **Existing infrastructure**: Feature 001-sample-storage is fully implemented
  with all required backend services and frontend components
- **Database performance**: Database queries with `LIMIT` and `OFFSET` clauses
  perform adequately for expected dataset sizes (100,000+ samples)
- **User behavior**: Lab technicians typically view recent samples and do not
  frequently jump to arbitrary high page numbers
- **Data volume growth**: Sample storage assignments grow linearly over time,
  but pagination prevents performance issues regardless of growth rate
- **Reference pattern**: NoteBook module's pagination implementation serves as
  the reference pattern for consistency

## Dependencies

**CRITICAL PREREQUISITE**:

- Feature 001-sample-storage MUST be fully implemented and merged to `develop`
  branch
- The following components MUST exist:
  - `StorageDashboard.jsx` component with Samples tab
  - `SampleStorageService` interface and implementation
  - `SampleStorageRestController` with sample items endpoint
  - Sample storage database tables and entities

**Reference Implementation**:

- NoteBook module (`NoteBookDashBoard.js`, `NoteBookRestController.java`) serves
  as the reference pattern for pagination implementation

## References

- **Parent Feature**: [001-sample-storage](../001-sample-storage/spec.md)
- **Jira Issue**: [OGC-150](https://uwdigi.atlassian.net/browse/OGC-150)
- **Constitution**: [Constitution v1.8.0](../../.specify/memory/constitution.md)
- **Reference Implementation**:
  `frontend/src/components/notebook/NoteBookDashBoard.js` (pagination pattern)
- **Carbon Design System**:
  [Pagination Component](https://carbondesignsystem.com/components/pagination/)
