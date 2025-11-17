# Storage Management Cypress E2E Tests

## Overview

This directory contains Cypress end-to-end tests for the Sample Storage Management feature, covering all three user stories (P1, P2A, P2B).

## Test Files

### 1. `storageAssignment.cy.js` (P1 - Basic Assignment)

Tests the three assignment modes:

- **Cascading Dropdowns**: Hierarchical selection (Room → Device → Shelf → Rack → Position)
- **Type-Ahead Autocomplete**: Search-based location selection
- **Barcode Scan**: Barcode scanner input simulation
- **Inline Location Creation**: Creating new locations from the selector
- **Capacity Warnings**: Displaying capacity alerts at 80%, 90%, 100%

### 2. `storageSearch.cy.js` (P2A - Search/Retrieval)

Tests sample search and filtering:

- **Search by Sample ID**: Finding samples by accession number
- **Filter by Room**: Filtering samples by storage room
- **Filter by Multiple Criteria**: Combining filters (room + device)
- **Clear Filters**: Resetting filters to show all samples

### 3. `storageMovement.cy.js` (P2B - Movement)

Tests sample movement workflows:

- **Single Sample Move**: Moving one sample between locations
- **Occupied Position Prevention**: Error handling for occupied targets
- **Bulk Move**: Moving multiple samples with auto-assigned positions
- **Manual Position Editing**: Overriding auto-assigned positions
- **Previous Position Freed**: Verifying positions are freed after moves

## Test Execution Requirements

### Prerequisites

1. **Xvfb** (for headless execution on Linux):

   ```bash
   sudo apt-get install -y xvfb
   ```

2. **Docker Environment** (alternative):
   Tests are designed to run in the Docker environment where all dependencies are available.

3. **Application Running**:
   - Backend API must be running and accessible at `https://localhost`
   - Test database must be populated with storage fixture data
   - Frontend must be running and accessible

### Running Tests

#### All Storage Tests

```bash
cd frontend
npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

#### Individual Test Files

```bash
# P1 - Assignment
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

# P2A - Search
npm run cy:run -- --spec "cypress/e2e/storageSearch.cy.js"

# P2B - Movement
npm run cy:run -- --spec "cypress/e2e/storageMovement.cy.js"
```

#### Interactive Mode (for debugging)

```bash
npm run cy:open
# Then select the test file from the Cypress UI
```

### Test Data Requirements

Tests expect the following fixture data to be available:

- **Rooms**: "MAIN" (Main Laboratory)
- **Devices**: "FRZ01" (Freezer Unit 1)
- **Shelves**: "SHA" (Shelf-A)
- **Racks**: "RKR1", "RKR2"
- **Positions**: "A5", "B3", "B4", "C1", "C8"
- **Samples**: Sample ID "101" (or accessible sample IDs)

Load test data using:

```bash
./src/test/resources/load-storage-test-data.sh
```

Or via Liquibase changeset:

```xml
<changeSet context="test" ...>
  <!-- Test data from 004-insert-test-storage-data.xml -->
</changeSet>
```

## Test Structure

### Page Objects

- `StorageAssignmentPage.js`: Reusable methods for storage location selection
- `LoginPage.js`: Authentication and navigation

### Test Patterns

- Uses `before()` hook for login setup
- Waits for API calls to complete (`cy.wait()`)
- Uses `data-testid` attributes for reliable element selection
- Verifies success notifications and error messages
- Tests both success and failure scenarios

## Known Limitations

1. **Xvfb Dependency**: Headless execution requires Xvfb on Linux systems
2. **Autocomplete/Barcode Modes**: Some tests may require component mode switching implementation
3. **Dashboard Routes**: Search/movement tests assume storage dashboard routes exist (`/StorageDashboard`)
4. **Component Integration**: Tests assume StorageLocationSelector is integrated in OrderEntry workflow

## Future Improvements

- [ ] Add test fixtures for storage hierarchy data
- [ ] Implement mode switching in component for autocomplete/barcode tests
- [ ] Add storage dashboard routes if not already implemented
- [ ] Add API mocking for faster test execution
- [ ] Add visual regression testing for storage components

## Related Documentation

- Feature Specification: `specs/001-sample-storage/spec.md`
- Implementation Plan: `specs/001-sample-storage/plan.md`
- Quickstart Guide: `specs/001-sample-storage/quickstart.md`
