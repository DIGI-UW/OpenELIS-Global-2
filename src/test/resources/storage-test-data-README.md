# Storage Test Data Fixtures

This directory contains test data fixtures for integration testing the Storage
Management POC feature.

## Overview

The test data provides a complete storage hierarchy for testing User Story P1
(Basic Assignment):

```
Main Laboratory (MAIN)
├── Freezer Unit 1 (FRZ01) -80°C
│   ├── Shelf-A
│   │   ├── Rack R1 (8x12 grid = 96 positions)
│   │   │   ├── A1, A2, A3 (occupied), A4, A5, A6, A7, A8...
│   │   └── Rack R2 (10x10 grid = 100 positions, 80% occupied)
│   │       └── 1-1, 1-2, 1-3... (80 occupied, 20 empty)
│   └── Shelf-B
│       └── Rack R3 (no grid, flexible positions)
│           ├── RED-01 (duplicate allowed)
│           ├── RED-02
│           └── RED-01 (duplicate)
└── Refrigerator Unit 1 (REF01) +4°C
    └── Shelf-1
        └── Rack R1 (8x12 grid)

Secondary Laboratory (SEC)
└── Cabinet Unit 1 (CAB01)
    └── Shelf-1

Inactive Room (INACTIVE) - for testing inactive validation
└── Inactive Freezer
```

## Loading Test Data

### Method 1: Liquibase (Automatic - Recommended)

Test data loads automatically when running with `test` context:

```bash
# Run application with test context
mvn spring-boot:run -Dspring.profiles.active=test

# Or specify context in properties:
# liquibase.contexts=test
```

**Note**: The changeset `004-insert-test-storage-data.xml` has `context="test"`,
so it only runs when test context is active.

### Method 2: Manual SQL Script

For manual loading or re-loading test data:

```bash
# Connect to database
psql -U clinlims -d clinlims

# Run SQL script
\i src/test/resources/storage-test-data.sql

# Or from command line:
psql -U clinlims -d clinlims -f src/test/resources/storage-test-data.sql
```

### Method 3: Docker Environment

If using dev.docker-compose.yml:

```bash
# Copy SQL to running container
docker cp src/test/resources/storage-test-data.sql database.openelis.org:/tmp/

# Execute in container
docker exec -it database.openelis.org psql -U clinlims -d clinlims -f /tmp/storage-test-data.sql
```

## Test Data Details

### Rooms (3)

- **Main Laboratory (MAIN)**: Active, primary test location
- **Secondary Laboratory (SEC)**: Active, alternative location
- **Inactive Room (INACTIVE)**: Inactive for testing validation

### Devices (4)

- **Freezer Unit 1 (FRZ01)**: -80°C, 500 capacity
- **Refrigerator Unit 1 (REF01)**: +4°C, 300 capacity
- **Cabinet Unit 1 (CAB01)**: Room temperature
- **Inactive Freezer**: For testing inactive device validation

### Racks (4)

- **Rack R1** (Shelf-A): 8x12 grid (96 positions) - Low occupancy
- **Rack R2** (Shelf-A): 10x10 grid (100 positions) - **80% occupied** for
  capacity warning testing
- **Rack R3** (Shelf-B): No grid (flexible positions) - For testing duplicate
  coordinates
- **Rack R1** (Shelf-1): 8x12 grid - Alternative location

### Positions (100+)

- **8 positions** in Rack R1 (A1-A8)
- **100 positions** in Rack R2 (1-1 through 10-10) - 80 occupied, 20 empty
- **3 positions** in Rack R3 (RED-01, RED-02, RED-01 duplicate)
- **1 position** in inactive location (X1)

## Test Scenarios Enabled

### ✅ Basic Assignment (T042-T050)

- **Valid assignment**: Use position `A5` in Rack R1 (MAIN > FRZ01 > Shelf-A >
  Rack R1 > A5)
- **Occupied position**: Use position `A3` in Rack R1 (should fail with 400)
- **Inactive location**: Use position `X1` in Inactive Room (should fail
  with 400)

### ✅ Capacity Warnings (T043)

- **80% warning**: Assign to Rack R2 (80/100 occupied)
- **Normal capacity**: Assign to Rack R1 (<10% occupied)

### ✅ Duplicate Coordinates (T029)

- **Flexible storage**: Both `RED-01` positions in Rack R3 are valid (duplicates
  allowed)

### ✅ Cascading Dropdowns (T064 Cypress)

- Full hierarchy available for selection testing
- Multiple rooms/devices/shelves for dropdown population

## Verification Queries

```sql
-- View all storage locations
SELECT r.name AS room, d.name AS device, s.label AS shelf, k.label AS rack,
       COUNT(p.id) AS total_positions,
       SUM(CASE WHEN p.occupied THEN 1 ELSE 0 END) AS occupied
FROM storage_room r
LEFT JOIN storage_device d ON d.parent_room_id = r.id
LEFT JOIN storage_shelf s ON s.parent_device_id = d.id
LEFT JOIN storage_rack k ON k.parent_shelf_id = s.id
LEFT JOIN storage_position p ON p.parent_rack_id = k.id
GROUP BY r.name, d.name, s.label, k.label
ORDER BY r.name, d.name, s.label, k.label;

-- Check capacity for Rack R2 (should show 80%)
SELECT
    k.label,
    k.rows * k.columns AS total_capacity,
    COUNT(p.id) AS position_count,
    SUM(CASE WHEN p.occupied THEN 1 ELSE 0 END) AS occupied,
    ROUND(100.0 * SUM(CASE WHEN p.occupied THEN 1 ELSE 0 END) / (k.rows * k.columns), 2) AS occupancy_percentage
FROM storage_rack k
LEFT JOIN storage_position p ON p.parent_rack_id = k.id
WHERE k.id = '31'
GROUP BY k.id, k.label, k.rows, k.columns;

-- List available (unoccupied) positions
SELECT
    r.code || ' > ' || d.code || ' > ' || s.label || ' > ' || k.label || ' > ' || p.coordinate AS hierarchical_path,
    p.id AS position_id
FROM storage_position p
JOIN storage_rack k ON k.id = p.parent_rack_id
JOIN storage_shelf s ON s.id = k.parent_shelf_id
JOIN storage_device d ON d.id = s.parent_device_id
JOIN storage_room r ON r.id = d.parent_room_id
WHERE p.occupied = false AND r.active = true AND d.active = true
ORDER BY hierarchical_path
LIMIT 20;
```

## Clean Up

To remove all test data:

```sql
DELETE FROM result WHERE analysis_id IN (SELECT id FROM analysis WHERE sampitem_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%')));
DELETE FROM analysis WHERE sampitem_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'));
DELETE FROM sample_storage_movement WHERE sample_item_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'));
DELETE FROM sample_storage_assignment WHERE sample_item_id IN (SELECT id FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%'));
DELETE FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%');
DELETE FROM sample_human WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%');
DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';
DELETE FROM storage_position WHERE id BETWEEN 100 AND 10000;
DELETE FROM storage_rack WHERE id BETWEEN 30 AND 100;
DELETE FROM storage_shelf WHERE id BETWEEN 20 AND 100;
DELETE FROM storage_device WHERE id BETWEEN 10 AND 100;
DELETE FROM storage_room WHERE id BETWEEN 1 AND 100;
```

## Integration Test Usage

### Running Integration Tests

```bash
# Run all storage controller tests with test context
mvn test -Dtest="StorageLocationRestControllerTest" -Dspring.profiles.active=test -DskipTests=false -Dmaven.test.skip=true

# Run sample assignment tests
mvn test -Dtest="SampleStorageRestControllerTest" -Dspring.profiles.active=test -DskipTests=false -Dmaven.test.skip=true
```

### Expected Test Behavior

With these fixtures loaded:

1. **testCreateRoom_ValidInput_Returns201**: ✅ Creates new room (ID > 1000)
2. **testCreateDevice_DuplicateCode_Returns400**: ✅ Fails when creating device
   with code "FRZ01" in room "1"
3. **testAssignSample_ValidInput_Returns201**: ✅ Assigns to position "104" (A5)
4. **testAssignSample_OccupiedPosition_Returns400**: ✅ Fails when assigning to
   position "102" (A3 - occupied)
5. **testAssignSample_InactiveLocation_Returns400**: ✅ Fails when assigning to
   position "120" (in inactive room)

## Cypress E2E Test Usage

For Cypress tests (`storageAssignment.cy.js`, `storageDashboard.cy.js`, etc.),
test fixtures are automatically loaded via the `before()` hook using
`cy.loadStorageFixtures()`. This loads both storage hierarchy and E2E test data
(patients, samples, assignments).

### Manual Loading (if needed)

```bash
# Load complete test fixtures (storage hierarchy + E2E test data)
psql -U clinlims -d clinlims -f src/test/resources/storage-test-data.sql

# Or use the shell script
./src/test/resources/load-storage-test-data.sh

# Run Cypress tests
cd frontend
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
```

### Test Data Available for E2E Tests

**Test Patients:**

- **John E2E-Smith** (External ID: E2E-PAT-001)
- **Jane E2E-Jones** (External ID: E2E-PAT-002)
- **Bob E2E-Williams** (External ID: E2E-PAT-003)

**Test Samples:**

- **E2E-001**: Assigned to MAIN > FRZ01 > Shelf-A > Rack R1 > A1
  - SampleItem 10001 has 2 analyses: 1 finalized (with result "Positive"), 1 not
    started
- **E2E-002**: Assigned to MAIN > FRZ01 > Shelf-A > Rack R1 > A2
  - SampleItem 10011 has 1 analysis: Technical acceptance
- **E2E-003**: Assigned to MAIN > FRZ01 > Shelf-A > Rack R1 > A4
  - SampleItem 10021 has 1 analysis: Canceled
- **E2E-004**: **Unassigned** (for testing assignment workflow)
- **E2E-005**: Assigned to MAIN > FRZ01 > Shelf-A > Rack R1 > A5
  - SampleItem 10041 has 1 analysis: Finalized (with result "125.5")

**Test Analyses (Orders):**

- **Analysis 20001**: SampleItem 10001 (E2E-001) - Finalized status, has result
- **Analysis 20002**: SampleItem 10001 (E2E-001) - Not started status, no result
- **Analysis 20003**: SampleItem 10011 (E2E-002) - Technical acceptance status
- **Analysis 20004**: SampleItem 10021 (E2E-003) - Canceled status
- **Analysis 20005**: SampleItem 10041 (E2E-005) - Finalized status, has result

**Test Results:**

- **Result 30001**: Analysis 20001 - Dictionary type result "Positive"
- **Result 30002**: Analysis 20005 - Numeric type result "125.5"

These samples appear in the Storage Dashboard and can be searched/filtered by
the tests. The analyses and results enable testing of order entry, result entry,
and storage integration workflows.

## Troubleshooting

### Test data not loading

```bash
# Check Liquibase changelog
psql -U clinlims -d clinlims -c "SELECT * FROM databasechangelog WHERE id LIKE 'storage-test%';"

# Verify test context is active
# Check application.properties or command line args for: liquibase.contexts=test
```

### Duplicate key violations

```bash
# Reset sequences
psql -U clinlims -d clinlims << EOF
SELECT setval('storage_room_seq', 1000, false);
SELECT setval('storage_device_seq', 1000, false);
SELECT setval('storage_shelf_seq', 1000, false);
SELECT setval('storage_rack_seq', 1000, false);
SELECT setval('storage_position_seq', 10000, false);
EOF
```

---

**Created**: 2025-10-31  
**Feature**: Sample Storage Management POC (001-sample-storage)  
**Purpose**: Enable integration and E2E testing with realistic storage hierarchy
