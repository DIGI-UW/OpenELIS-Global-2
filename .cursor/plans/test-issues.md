<!-- e85e4271-3af2-4e89-a494-d0f22ea8141a 06bdf2cc-4032-46a6-b963-b2a4a13e7694 -->

# Fix Test Failures for Removed Occupied Field

## Problem Summary

The `occupied` column was removed from `storage_position` table in Liquibase
changeset `007-remove-occupied-column.xml`. Occupancy is now calculated
dynamically from `SampleStorageAssignment` records. However, 6 test files still
reference `setOccupied()` and `getOccupied()` methods that no longer exist,
causing compilation failures.

## Affected Files

1. `src/test/java/org/openelisglobal/storage/valueholder/StoragePositionTest.java` -
   7 failures
2. `src/test/java/org/openelisglobal/storage/service/SampleStorageServiceImplTest.java` -
   1 failure
3. `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceHierarchyTest.java` -
   4 failures
4. `src/test/java/org/openelisglobal/storage/valueholder/StorageEntityTest.java` -
   1 failure
5. `src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java` -
   3 failures
6. `src/test/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransformTest.java` -
   1 failure

## Implementation Plan

### Phase 1: Remove Unnecessary setOccupied() Calls

**Files to fix:**

- `StoragePositionTest.java` - Remove 7 instances of
  `position.setOccupied(false)` (lines 62, 81, 102, 123, 147, 165, 187)
- `SampleStorageServiceImplTest.java` - Remove 1 instance (line 80)
- `StorageLocationServiceHierarchyTest.java` - Remove 4 instances (lines 93,
  114, 137, 161)
- `StorageLocationFhirTransformTest.java` - Remove 1 instance (line 69)

**Action:** These calls are in test setup and are not needed since occupancy is
calculated dynamically. Simply delete the lines.

### Phase 2: Fix StorageEntityTest.java

**File:**
`src/test/java/org/openelisglobal/storage/valueholder/StorageEntityTest.java`

**Current test (line 105-111):**

```java
@Test
public void testStoragePosition_OccupancyFlag_DefaultsFalse() {
    StoragePosition position = new StoragePosition();
    assertTrue(position.getOccupied() == null || !position.getOccupied());
}
```

**Fix:** Remove this test entirely since `getOccupied()` no longer exists. The
test was checking default values for a field that no longer exists.

### Phase 3: Fix StorageLocationRestControllerTest.java

**File:**
`src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java`

**Issues:**

1. Line 591: `positionForm.setOccupied(false)` - Remove this call
   (StoragePositionForm doesn't have this field)
2. Line 598: `jsonPath("$.occupied").value(false)` - Keep this assertion (REST
   controller calculates occupancy dynamically)
3. Lines 650, 659: `setOccupied(true/false)` - Remove these calls
4. Line 664: Filter by occupied status - This test may need adjustment if it
   relies on setting occupied

**Action:**

- Remove all `positionForm.setOccupied()` calls
- Keep JSON response assertions for `occupied` field (it's calculated by the
  controller)
- If test `testGetPositions_FilterByRackAndOccupancy_ReturnsFiltered` relies on
  setting occupied, update it to create actual `SampleStorageAssignment` records
  instead

### Phase 4: Verify REST Controller Behavior

**File:**
`src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java`

**Verification:** Confirm that:

- Line 933: Controller calculates `occupied` dynamically using
  `sampleStorageAssignmentDAO.isPositionOccupied(position)`
- This means JSON responses will still include `occupied` field (calculated, not
  stored)

**Action:** No changes needed to controller - it already calculates occupancy
correctly.

### Phase 5: Run Tests to Verify Fixes

**Command:**
`mvn test -Dtest="StoragePositionTest,SampleStorageServiceImplTest,StorageLocationServiceHierarchyTest,StorageEntityTest,StorageLocationRestControllerTest,StorageLocationFhirTransformTest"`

**Expected:** All tests should compile and pass.

## Implementation Details

### Pattern for Simple Removals

For files with only `setOccupied()` calls in setup:

- Locate the line: `position.setOccupied(false);` or
  `position.setOccupied(true);`
- Delete the entire line
- No other changes needed

### Pattern for StorageEntityTest

- Delete the entire test method
  `testStoragePosition_OccupancyFlag_DefaultsFalse()`
- No replacement needed - this test is no longer applicable

### Pattern for StorageLocationRestControllerTest

- Remove `positionForm.setOccupied(...)` calls
- Keep JSON assertions for `occupied` field
- For `testGetPositions_FilterByRackAndOccupancy_ReturnsFiltered`, if it needs
  occupied positions, create `SampleStorageAssignment` records instead of
  setting a field

## Verification Checklist

- [ ] All `setOccupied()` calls removed from test setup
- [ ] `getOccupied()` assertions removed
- [ ] `testStoragePosition_OccupancyFlag_DefaultsFalse` test removed
- [ ] REST controller tests still assert `occupied` in JSON responses
      (calculated value)
- [ ] All affected test files compile successfully
- [ ] All affected tests pass

## Notes

- These failures are **pre-existing** and not related to our Label Management
  implementation
- The fixes are straightforward: remove obsolete method calls
- Occupancy calculation logic is already correct in the controller and DAO
  layers
- No changes needed to production code - only test code needs updating

### To-dos

- [ ] Remove all 7 setOccupied(false) calls from StoragePositionTest.java (lines
      62, 81, 102, 123, 147, 165, 187)
- [ ] Remove setOccupied(false) call from SampleStorageServiceImplTest.java
      (line 80)
- [ ] Remove all 4 setOccupied(false) calls from
      StorageLocationServiceHierarchyTest.java (lines 93, 114, 137, 161)
- [ ] Remove testStoragePosition_OccupancyFlag_DefaultsFalse test method from
      StorageEntityTest.java (lines 105-111)
- [ ] Remove setOccupied() calls from StorageLocationRestControllerTest.java and
      verify occupied assertions work with calculated values (lines 591,
      650, 659)
- [ ] Remove setOccupied(true) call from StorageLocationFhirTransformTest.java
      (line 69)
- [ ] Run mvn compile to verify all test files compile successfully
- [ ] Run affected test classes to verify they pass: StoragePositionTest,
      SampleStorageServiceImplTest, StorageLocationServiceHierarchyTest,
      StorageEntityTest, StorageLocationRestControllerTest,
      StorageLocationFhirTransformTest
