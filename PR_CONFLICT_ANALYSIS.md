# Storage Functionality PR Conflict Analysis

## Executive Summary

This analysis examines potential conflicts between the OGC-144 feature branch
(`fix/OGC-144-disposed-counter-update`) and two recently merged PRs:

- **PR #2396** (OGC-142): StorageBox entity introduction and storage hierarchy
  refactoring
- **PR #2400**: SampleStorageAssignment refactoring to use direct sampleItemId

**CRITICAL CONFLICTS IDENTIFIED**: 4 major conflicts requiring resolution before
merge.

---

## PR #2396 (OGC-142) Summary

**Merged**: December 7, 2025  
**Changes**: +2,155 / -5,559 lines across 58 files

### Key Changes:

1. **StorageBox Entity**: Introduced `StorageBox` to replace `StoragePosition`

   - `StorageBox` represents gridded containers (96-well plates, sample boxes)
     within racks
   - Has `rows`, `columns`, `positionSchemaHint` (moved from `StorageRack`)
   - Parent relationship: `StorageBox` → `StorageRack` (required)

2. **StorageRack Simplification**:

   - Removed `rows`, `columns`, `positionSchemaHint` fields
   - Changed `code` → `shortCode` (nullable, max 10 chars)
   - Racks are now simple containers for boxes

3. **Location Type Enum**: Added `'box'` as valid locationType

   - Updated CHECK constraints:
     `location_type IN ('device', 'shelf', 'rack', 'box')`

4. **Database Schema**:

   - Removed `storage_position` table
   - Added `storage_box` table
   - Updated all Liquibase changelogs

5. **Service/DAO Updates**:
   - `StoragePositionDAO` → `StorageBoxDAO`
   - `getPositionsByRack()` → `getBoxesByRack()`
   - All occupancy counting methods updated to use boxes

---

## PR #2400 Summary

**Merged**: December 7, 2025  
**Changes**: +323 / -210 lines across 15 files

### Key Changes:

1. **SampleStorageAssignment Entity Refactoring**:

   - **CRITICAL**: Changed from `@ManyToOne` relationship to direct
     `sampleItemId` (Integer) column
   - `sampleItem` field is now `@Transient` (must be loaded manually)
   - Added `getSampleItemId()` and `setSampleItemId(Integer)` methods
   - Reason: Avoids cross-mapping issues between JPA annotations and HBM XML
     mapping

2. **SampleStorageMovement Entity**: Same refactoring (direct `sampleItemId`)

3. **resolveSampleItem() Method**:

   - **REMOVED**: Internal ID lookup (Step 1)
   - **ONLY ACCEPTS**: Accession numbers or external IDs
   - Updated error message: "Please check the accession number or external
     reference number"

4. **HQL Query Updates**:

   - Changed from `ssa.sampleItem.id = :sampleItemId` to
     `ssa.sampleItemId = :sampleItemId`
   - Updated `findBySampleItemId()` to parse String to Integer

5. **Metrics Calculation** (SampleStorageRestController):

   - **CRITICAL**: Changed to manually load `SampleItem` for each assignment
   - Uses `sampleItemDAO.get()` to load SampleItem by ID
   - Reason: `assignment.getSampleItem()` no longer works (it's @Transient)

6. **Native SQL for Occupied Coordinates**:
   - `getOccupiedCoordinatesWithSampleInfo()` uses native SQL instead of HQL
   - Reason: Can't use HQL JOIN with @Transient field

---

## OGC-144 Branch Summary

**Current Branch**: `fix/OGC-144-disposed-counter-update`  
**Changes**: ~500 lines across 13 files

### Key Changes:

1. **Disposal Logic** (SampleStorageServiceImpl):

   - **DIFFERENT FROM PR #2400**: Keeps assignment but clears location fields
   - Sets `locationId = null`, `locationType = null`,
     `positionCoordinate = null`
   - Updates assignment instead of deleting it
   - Reason: FR-056, FR-057 - disposed samples must remain in assignment table
     for metrics

2. **Metrics Calculation** (SampleStorageRestController):

   - Counts disposed samples from assignments
   - Uses `assignment.getSampleItem()` to check status
   - **CONFLICT**: Won't work with PR #2400 changes (sampleItem is @Transient)

3. **Liquibase Changeset**:

   - Added `016-allow-null-location-for-disposed.xml`
   - Makes `location_id` and `location_type` nullable for disposed samples

4. **Frontend Updates**:
   - Added `refreshMetrics()` call after disposal
   - Optimistic update pattern (FR-057b, FR-057c)

---

## Conflict Analysis

### 🔴 CRITICAL CONFLICT #1: SampleStorageAssignment Entity Structure

**File**:
`src/main/java/org/openelisglobal/storage/valueholder/SampleStorageAssignment.java`

**PR #2400 Changes**:

```java
// Direct column mapping (no @ManyToOne)
@Column(name = "SAMPLE_ITEM_ID", nullable = false, unique = true)
private Integer sampleItemId;

@Transient
private SampleItem sampleItem;
```

**OGC-144 Branch**:

```java
// Still uses @ManyToOne relationship
@ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
@JoinColumn(name = "SAMPLE_ITEM_ID", nullable = false, unique = true)
private SampleItem sampleItem;
```

**Impact**:

- OGC-144 branch code will NOT compile with PR #2400 changes
- All code using `assignment.getSampleItem()` will fail (NullPointerException)
- `findBySampleItemId()` signature changed (now accepts String, parses to
  Integer)

**Resolution Required**:

1. Update `SampleStorageAssignment.java` to match PR #2400 structure
2. Update all code using `assignment.getSampleItem()` to load SampleItem
   manually
3. Update `findBySampleItemId()` calls to use String parameter

---

### 🔴 CRITICAL CONFLICT #2: disposeSampleItem() Implementation

**File**:
`src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java`

**PR #2400 Changes**:

```java
// Uses resolveSampleItem() (only accepts accession/external ID)
SampleItem sampleItem = resolveSampleItem(sampleItemId);

// Direct status check (status_id = 24)
if (sampleItem.getStatusId() != null && "24".equals(sampleItem.getStatusId())) {
    throw new LIMSRuntimeException("SampleItem is already disposed");
}

// Deletes assignment
sampleStorageAssignmentDAO.delete(existingAssignment);
```

**OGC-144 Branch**:

```java
// Direct ID lookup (won't work with PR #2400)
SampleItem sampleItem = sampleItemDAO.get(sampleItemId)
    .orElseThrow(() -> new LIMSRuntimeException("SampleItem not found: " + sampleItemId));

// Uses statusService.matches() (won't work with PR #2400)
if (sampleItem.getStatusId() != null
        && statusService.matches(sampleItem.getStatusId(), SampleStatus.Disposed)) {
    throw new LIMSRuntimeException("SampleItem is already disposed");
}

// Updates assignment (clears location, keeps assignment)
existingAssignment.setLocationId(null);
existingAssignment.setLocationType(null);
existingAssignment.setPositionCoordinate(null);
sampleStorageAssignmentDAO.update(existingAssignment);
```

**Impact**:

- OGC-144 disposal logic is incompatible with PR #2400 `resolveSampleItem()`
  changes
- OGC-144 uses different disposal strategy (update vs delete)
- Status checking approach differs

**Resolution Required**:

1. Update `disposeSampleItem()` to use `resolveSampleItem()` (accepts
   accession/external ID only)
2. Update status check to use direct comparison (`"24".equals(statusId)`)
3. **DECISION NEEDED**: Keep OGC-144 strategy (update assignment) or use PR
   #2400 strategy (delete assignment)?
   - OGC-144 approach supports metrics (FR-056, FR-057)
   - PR #2400 approach is simpler but loses metrics data

---

### 🔴 CRITICAL CONFLICT #3: Metrics Calculation

**File**:
`src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java`

**PR #2400 Changes**:

```java
// Manually loads SampleItem for each assignment
for (SampleStorageAssignment assignment : allAssignments) {
    if (assignment.getSampleItemId() != null) {
        String sampleItemIdStr = assignment.getSampleItemId().toString();
        Optional<SampleItem> sampleItemOpt = sampleItemDAO.get(sampleItemIdStr);
        if (sampleItemOpt.isPresent()) {
            SampleItem sampleItem = sampleItemOpt.get();
            if (sampleItem.getStatusId() == null
                    || !statusService.matches(sampleItem.getStatusId(), SampleStatus.Disposed)) {
                active++;
            } else {
                disposed++;
            }
        }
    }
}
```

**OGC-144 Branch**:

```java
// Uses assignment.getSampleItem() (won't work with PR #2400)
long active = allAssignments.stream()
    .filter(a -> a.getSampleItem() != null && (a.getSampleItem().getStatusId() == null
            || !statusService.matches(a.getSampleItem().getStatusId(), SampleStatus.Disposed)))
    .count();
long disposed = allAssignments.stream()
    .filter(a -> a.getSampleItem() != null
            && statusService.matches(a.getSampleItem().getStatusId(), SampleStatus.Disposed))
    .count();
```

**Impact**:

- OGC-144 metrics calculation will fail (NullPointerException on
  `getSampleItem()`)
- PR #2400 approach is correct but less efficient (N+1 query problem)

**Resolution Required**:

1. Update metrics calculation to match PR #2400 approach (manual SampleItem
   loading)
2. Consider optimizing with batch loading or JOIN FETCH if performance is
   critical
3. Update to use direct status comparison if disposing uses status_id = "24"

---

### 🟡 MEDIUM CONFLICT #4: Missing 'box' Location Type Support

**File**:
`src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java`

**PR #2396 Changes**:

- Added `'box'` as valid locationType in CHECK constraints
- Updated all location type validation to include `'box'`
- Added `StorageBox` entity and DAO

**OGC-144 Branch**:

- Disposal logic only handles `'device'`, `'shelf'`, `'rack'` cases
- Missing `case "box":` in `disposeSampleItem()` location entity loading

**Impact**:

- Disposing samples assigned to boxes will fail (location entity not found)
- Hierarchical path building will fail for box locations

**Resolution Required**:

1. Add `case "box":` to disposal location entity loading
2. Update hierarchical path building to handle StorageBox
3. Test disposal with box-assigned samples

---

### 🟡 MEDIUM CONFLICT #5: StorageBox vs StoragePosition References

**Files**: Multiple test files and service implementations

**PR #2396 Changes**:

- Removed all `StoragePosition` references
- Replaced with `StorageBox`
- Updated all DAO methods

**OGC-144 Branch**:

- May still reference `StoragePosition` in comments or test data
- No direct code conflicts identified, but needs verification

**Resolution Required**:

1. Search for any remaining `StoragePosition` references
2. Update test data if it references positions
3. Verify no imports or type references remain

---

## File-by-File Conflict Summary

| File                                 | PR #2396               | PR #2400                     | OGC-144                     | Conflict Severity |
| ------------------------------------ | ---------------------- | ---------------------------- | --------------------------- | ----------------- |
| `SampleStorageAssignment.java`       | No changes             | ✅ Major refactor            | ❌ Uses old structure       | 🔴 CRITICAL       |
| `SampleStorageServiceImpl.java`      | ✅ Added box support   | ✅ Changed resolveSampleItem | ❌ Different disposal logic | 🔴 CRITICAL       |
| `SampleStorageRestController.java`   | No changes             | ✅ Changed metrics calc      | ❌ Uses old metrics calc    | 🔴 CRITICAL       |
| `StorageLocationRestController.java` | ✅ Added box endpoints | No changes                   | ✅ Minor changes            | 🟢 LOW            |
| `SampleStorageMovement.java`         | No changes             | ✅ Major refactor            | No changes                  | 🟡 MEDIUM         |
| Test files                           | ✅ Updated for boxes   | ✅ Updated for sampleItemId  | ✅ Updated for disposal     | 🟡 MEDIUM         |

---

## Recommended Resolution Strategy

### Phase 1: Entity Structure Alignment (CRITICAL)

1. **Update SampleStorageAssignment.java**:

   ```java
   // Replace @ManyToOne with direct column mapping
   @Column(name = "SAMPLE_ITEM_ID", nullable = false, unique = true)
   private Integer sampleItemId;

   @Transient
   private SampleItem sampleItem;
   ```

2. **Update SampleStorageMovement.java** (same pattern)

3. **Update all code using `assignment.getSampleItem()`**:
   - Load SampleItem manually:
     `sampleItemDAO.get(assignment.getSampleItemId().toString())`
   - Or use `assignment.getSampleItemId()` directly for ID-based operations

### Phase 2: Disposal Logic Integration (CRITICAL)

1. **Update disposeSampleItem()**:

   - Use `resolveSampleItem()` instead of direct `sampleItemDAO.get()`
   - Update status check: `"24".equals(sampleItem.getStatusId())`
   - **KEEP OGC-144 strategy**: Update assignment (clear location) instead of
     deleting
   - Add `case "box":` for StorageBox location loading

2. **Update findBySampleItemId() calls**:
   - Ensure String parameter is passed (method signature changed in PR #2400)

### Phase 3: Metrics Calculation Fix (CRITICAL)

1. **Update getSampleItems() metrics calculation**:
   - Replace stream filter with manual SampleItem loading (PR #2400 approach)
   - Consider batch loading for performance
   - Handle disposed samples correctly (status_id = "24")

### Phase 4: Box Support (MEDIUM)

1. **Add box case to disposal logic**:

   ```java
   case "box":
       locationEntity = storageLocationService.get(previousLocationId, StorageBox.class);
       break;
   ```

2. **Update hierarchical path building** to handle StorageBox

### Phase 5: Test Updates (MEDIUM)

1. **Update all test files**:
   - Use external IDs or accession numbers (not internal IDs) for
     `resolveSampleItem()`
   - Update test data to use StorageBox instead of StoragePosition
   - Update mock setup for new entity structure

---

## Testing Checklist

After resolving conflicts, verify:

- [ ] Disposal works with accession numbers
- [ ] Disposal works with external IDs
- [ ] Disposal works for samples assigned to boxes
- [ ] Metrics calculation shows correct active/disposed counts
- [ ] Disposed counter increments immediately after disposal (OGC-144
      requirement)
- [ ] All E2E tests pass
- [ ] All unit tests pass
- [ ] No NullPointerException in metrics calculation
- [ ] Hierarchical paths build correctly for all location types including boxes

---

## Next Steps

1. **Rebase OGC-144 branch onto latest develop**:

   ```bash
   git checkout fix/OGC-144-disposed-counter-update
   git fetch origin
   git rebase origin/develop
   ```

2. **Resolve conflicts** following the resolution strategy above

3. **Run full test suite**:

   ```bash
   mvn test
   npm run cy:run -- --spec "cypress/e2e/storageDisposal.cy.js"
   ```

4. **Verify metrics calculation** works correctly with new entity structure

5. **Test disposal with box-assigned samples** (new scenario from PR #2396)

---

## Questions for Decision

1. **Disposal Strategy**: Should we keep OGC-144 approach (update assignment,
   clear location) or use PR #2400 approach (delete assignment)?

   - **Recommendation**: Keep OGC-144 approach (supports metrics requirements)

2. **Status Checking**: Should we use direct comparison
   (`"24".equals(statusId)`) or keep `statusService.matches()`?

   - **Recommendation**: Use direct comparison (simpler, matches PR #2400)

3. **Metrics Performance**: Should we optimize the N+1 query problem in metrics
   calculation?
   - **Recommendation**: Yes, consider batch loading or JOIN FETCH if
     performance is critical

---

## References

- PR #2396: https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2396
- PR #2400: https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2400
- OGC-144 Branch: `fix/OGC-144-disposed-counter-update`
- Constitution: `.specify/memory/constitution.md` (v1.8.0)
