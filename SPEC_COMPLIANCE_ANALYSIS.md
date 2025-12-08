# Storage Specification Compliance Analysis

**Date**: 2025-12-09  
**Analysis Scope**: PR #2396, PR #2400, and current `develop` branch changes vs.
Storage Specification  
**Specification**: `specs/001-sample-storage/spec.md` and
`specs/001-sample-storage/data-model.md`

---

## Executive Summary

This analysis compares the storage functionality changes in `develop` (from PRs
#2396 and #2400) against the original storage specification to identify
**specification violations** and **deviations**.

**CRITICAL FINDINGS**:

- ❌ **MAJOR SPEC VIOLATION**: StorageBox entity structure violates FR-009
  (StoragePosition requirements)
- ❌ **MAJOR SPEC VIOLATION**: StorageRack simplification violates FR-008 and
  FR-017
- ❌ **SPEC VIOLATION**: Location type 'box' violates CHECK constraint in spec
- ⚠️ **SPEC DEVIATION**: SampleStorageAssignment refactoring (technical change,
  may be acceptable)
- ✅ **SPEC COMPLIANT**: Disposal workflow changes (PR #2400) align with FR-051
  through FR-056

---

## 1. StoragePosition vs StorageBox (CRITICAL SPEC VIOLATION)

### Specification Requirements (FR-009, data-model.md Section 5)

**Specification States**:

- **FR-009**: Position entity MUST include:
  - Free-text coordinate (optional, only required for 5-level positions)
  - Optional row/column integers for grid visualization
  - Occupancy state (empty/occupied)
  - **parent_device_id** (required - minimum 2 levels: Room + Device)
  - **parent_shelf_id** (optional - for 3+ level positions)
  - **parent_rack_id** (optional - for 4+ level positions)

**Data Model States** (data-model.md lines 294-346):

- StoragePosition MUST have `parent_device_id` (NOT NULL, required)
- StoragePosition can have `parent_shelf_id` (NULL, optional)
- StoragePosition can have `parent_rack_id` (NULL, optional)
- Position can be at: device level (2 levels), shelf level (3 levels), rack
  level (4 levels), or position level (5 levels)
- Minimum requirement: Room + Device (2 levels)

### PR #2396 Implementation

**StorageBox Entity** (from PR #2396):

- ❌ **NO `parent_device_id` field** - Violates minimum 2-level requirement
- ❌ **NO `parent_shelf_id` field** - Cannot support 3-level positions
- ✅ Has `parent_rack_id` (required, NOT NULL)
- ✅ Has `rows`, `columns`, `positionSchemaHint` (moved from StorageRack)
- ✅ Has `label`, `type`, `shortCode`, `active`

**Impact**:

- **CRITICAL**: StorageBox CANNOT represent positions at device level (2 levels)
  or shelf level (3 levels)
- **CRITICAL**: StorageBox CANNOT represent positions at rack level (4 levels)
  without a box
- **VIOLATION**: Spec requires positions to support 2-5 level hierarchy, but
  StorageBox only supports 5-level (Room → Device → Shelf → Rack → Box)
- **VIOLATION**: Spec allows assignment at device/shelf/rack level without
  position, but StorageBox requires a rack parent

**Specification Compliance**: ❌ **NON-COMPLIANT**

---

## 2. StorageRack Simplification (CRITICAL SPEC VIOLATION)

### Specification Requirements (FR-008, FR-017, data-model.md Section 4)

**Specification States**:

- **FR-008**: Rack entity MUST include:

  - Label/ID
  - **Dimensions (rows and columns as positive integers)**
  - **Optional position schema hint**
  - Active/inactive status
  - Parent shelf reference

- **FR-017**: System MUST calculate rack capacity as **rows × columns** (or 0 if
  no grid configured). Rack capacity is ALWAYS calculated (never uses static
  `capacity_limit` field - racks do not have this field)

**Data Model States** (data-model.md lines 223-291):

- StorageRack table MUST have:
  - `rows` (INT, NOT NULL, DEFAULT 0) - Grid rows (0 = no grid)
  - `columns` (INT, NOT NULL, DEFAULT 0) - Grid columns (0 = no grid)
  - `position_schema_hint` (VARCHAR(50), NULL) - Optional hint for position
    naming
- Capacity = `rows` × `columns` (computed, not stored)
- If rows=0 OR columns=0, capacity=0 (no grid, rack-level assignment only)

### PR #2396 Implementation

**StorageRack Changes** (from PR #2396):

- ❌ **REMOVED `rows` field** - Violates FR-008
- ❌ **REMOVED `columns` field** - Violates FR-008
- ❌ **REMOVED `position_schema_hint` field** - Violates FR-008
- ✅ Changed `code` → `shortCode` (nullable, max 10 chars)
- ✅ Racks are now "simple containers" for boxes

**Impact**:

- **CRITICAL**: Cannot calculate rack capacity per FR-017 (requires rows ×
  columns)
- **CRITICAL**: Cannot support rack-level assignment without boxes (spec allows
  this)
- **VIOLATION**: Spec requires racks to have dimensions for capacity calculation
- **VIOLATION**: Spec allows racks with rows=0/columns=0 for rack-level
  assignment, but PR removes this capability

**Specification Compliance**: ❌ **NON-COMPLIANT**

---

## 3. Location Type 'box' (SPEC VIOLATION)

### Specification Requirements (data-model.md Section 6)

**Specification States** (data-model.md lines 416-480):

- `SampleStorageAssignment` table CHECK constraint:
  - `CHECK (location_type IN ('device', 'shelf', 'rack'))`
  - Valid location types: **'device', 'shelf', or 'rack'** only
  - Position is just text coordinate, NOT an entity

**Specification States** (spec.md FR-033a):

- System MUST require that a valid location has at least 2 levels: Room and
  Device MUST be selected
- Shelf, Rack, and Position levels are optional
- Position coordinate is a **text field** (`position_coordinate`), not a
  separate entity reference

### PR #2396 Implementation

**Location Type Changes** (from PR #2396):

- ✅ Added `'box'` to CHECK constraint:
  `location_type IN ('device', 'shelf', 'rack', 'box')`
- ✅ Updated all validation logic to accept `'box'` as valid location type
- ✅ StorageBox is now a first-class location entity (not just a text
  coordinate)

**Impact**:

- **VIOLATION**: Spec explicitly states location_type can only be 'device',
  'shelf', or 'rack'
- **VIOLATION**: Spec treats position as a text coordinate field, not an entity
- **DEVIATION**: PR #2396 introduces StorageBox as an entity, making it a 5th
  location type

**Specification Compliance**: ❌ **NON-COMPLIANT**

---

## 4. SampleStorageAssignment Entity Refactoring (SPEC DEVIATION - TECHNICAL)

### Specification Requirements (data-model.md Section 6)

**Specification States** (data-model.md lines 416-480):

- `SampleStorageAssignment` has:
  - `sample_item_id` (VARCHAR(36), NOT NULL, UNIQUE, FK to `sample_item(id)`)
  - Relationship: Many-to-One with `SampleItem` (one SampleItem, one current
    assignment)

### PR #2400 Implementation

**Entity Changes** (from PR #2400):

- Changed from `@ManyToOne` relationship to direct `sampleItemId` (Integer)
  column
- `sampleItem` field is now `@Transient` (must be loaded manually)
- Reason: Avoids cross-mapping issues between JPA annotations and HBM XML
  mapping

**Impact**:

- **DEVIATION**: Technical implementation change (not a functional requirement
  violation)
- **ACCEPTABLE**: Does not change functional behavior, only internal
  implementation
- **NOTE**: May require code updates to manually load SampleItem when needed

**Specification Compliance**: ⚠️ **TECHNICAL DEVIATION (ACCEPTABLE)**

---

## 5. Disposal Workflow (SPEC COMPLIANT)

### Specification Requirements (FR-051 through FR-056)

**Specification States** (spec.md lines 1704-1763):

- **FR-052**: System MUST set disposed sample status to "Disposed"
  (irreversible)
- **FR-053**: System MUST clear disposed sample's current location (position
  becomes available)
- **FR-054**: System MUST create immutable audit record with all disposal
  details
- **FR-055**: System MUST prevent future assignment/movement of disposed samples
- **FR-056**: Disposed samples MUST remain viewable for audit purposes but
  non-editable

### PR #2400 Implementation

**Disposal Changes** (from PR #2400):

- ✅ Sets sample status to "Disposed" (status_id = "24")
- ✅ Clears location (deletes assignment or sets location fields to NULL)
- ✅ Creates audit record in `SampleStorageMovement`
- ✅ Prevents future assignment (status check)
- ✅ Disposed samples remain in system for audit

**Impact**:

- **COMPLIANT**: Implementation aligns with specification requirements
- **NOTE**: PR #2400 deletes assignment, but OGC-144 branch updates assignment
  (clears location). Both approaches satisfy FR-053.

**Specification Compliance**: ✅ **COMPLIANT**

---

## 6. Hierarchical Path Building (SPEC DEVIATION)

### Specification Requirements (FR-033a, FR-040c)

**Specification States**:

- Hierarchical path format: `Room > Device > Shelf > Rack > Position`
- Position is a text coordinate, not an entity
- Path can be at device level (2 levels), shelf level (3 levels), rack level (4
  levels), or position level (5 levels)

### PR #2396 Implementation

**Path Building Changes** (from PR #2396):

- Updated to handle StorageBox as a location entity
- Path format: `Room > Device > Shelf > Rack > Box > Coordinate`
- Box is now part of the hierarchy (6 levels possible)

**Impact**:

- **DEVIATION**: Spec allows 2-5 level paths, but PR #2396 enables 6-level paths
  (Room → Device → Shelf → Rack → Box → Coordinate)
- **DEVIATION**: Spec treats position as text coordinate, but PR treats box as
  entity + coordinate

**Specification Compliance**: ⚠️ **DEVIATION (FUNCTIONAL IMPACT)**

---

## Summary Table

| Requirement                           | Spec Requirement                                                                   | PR #2396                                | PR #2400                     | Compliance       |
| ------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------- | ---------------------------- | ---------------- |
| **FR-008**: Rack dimensions           | rows, columns, position_schema_hint                                                | ❌ Removed                              | N/A                          | ❌ **VIOLATION** |
| **FR-009**: Position entity structure | parent_device_id (required), parent_shelf_id (optional), parent_rack_id (optional) | ❌ StorageBox only has parent_rack_id   | N/A                          | ❌ **VIOLATION** |
| **FR-017**: Rack capacity calculation | rows × columns                                                                     | ❌ Cannot calculate (no rows/columns)   | N/A                          | ❌ **VIOLATION** |
| **Location type enum**                | 'device', 'shelf', 'rack' only                                                     | ❌ Added 'box'                          | N/A                          | ❌ **VIOLATION** |
| **FR-033a**: 2-5 level hierarchy      | Device (2), Shelf (3), Rack (4), Position (5)                                      | ❌ Only supports 5-level (requires box) | N/A                          | ❌ **VIOLATION** |
| **FR-051-056**: Disposal workflow     | Clear location, create audit, prevent reassignment                                 | N/A                                     | ✅ Compliant                 | ✅ **COMPLIANT** |
| **SampleStorageAssignment**           | @ManyToOne with SampleItem                                                         | N/A                                     | ⚠️ Direct column (technical) | ⚠️ **DEVIATION** |

---

## Recommendations

### 1. **IMMEDIATE ACTION REQUIRED**: Specification Update or Code Reversion

**Option A: Update Specification** (Recommended if StorageBox design is
intentional):

- Update `specs/001-sample-storage/spec.md` to reflect StorageBox entity
- Update `specs/001-sample-storage/data-model.md` to document StorageBox
  structure
- Update FR-008, FR-009, FR-017 to match new design
- Update location type enum to include 'box'
- Document rationale for design change (why StorageBox is better than
  StoragePosition)

**Option B: Revert PR #2396 Changes** (If spec must be followed exactly):

- Restore StoragePosition entity with parent_device_id, parent_shelf_id,
  parent_rack_id
- Restore StorageRack rows, columns, position_schema_hint fields
- Remove 'box' from location type enum
- Restore 2-5 level hierarchy support

### 2. **DECISION REQUIRED**: StorageBox Design Rationale

**Questions for Product/Architecture Team**:

1. Why was StorageBox introduced instead of keeping StoragePosition?
2. Is the 6-level hierarchy (Room → Device → Shelf → Rack → Box → Coordinate)
   intentional?
3. Should we support device-level and shelf-level assignments without boxes?
4. How should rack capacity be calculated without rows/columns on racks?

### 3. **TECHNICAL FIX**: SampleStorageAssignment Refactoring

**Acceptable Deviation** (PR #2400):

- Technical change to avoid cross-mapping issues
- Does not violate functional requirements
- **Action**: Update all code using `assignment.getSampleItem()` to manually
  load SampleItem

---

## Testing Implications

### Missing Test Coverage

1. **Device-level assignment** (2 levels): Cannot test with StorageBox (requires
   parent_rack_id)
2. **Shelf-level assignment** (3 levels): Cannot test with StorageBox (requires
   parent_rack_id)
3. **Rack-level assignment** (4 levels): Cannot test with StorageBox (requires
   parent_rack_id)
4. **Rack capacity calculation**: Cannot test (no rows/columns on racks)

### Required Test Updates

1. Update E2E tests to use StorageBox instead of StoragePosition
2. Remove tests for device/shelf/rack-level assignments (not supported by
   StorageBox)
3. Update capacity calculation tests to use box capacities instead of rack
   dimensions
4. Add tests for 6-level hierarchy (Room → Device → Shelf → Rack → Box →
   Coordinate)

---

## References

- **Specification**: `specs/001-sample-storage/spec.md` (2299 lines)
- **Data Model**: `specs/001-sample-storage/data-model.md` (687 lines)
- **PR #2396**: https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2396
- **PR #2400**: https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2400
- **Constitution**: `.specify/memory/constitution.md` (v1.8.0)

---

## Conclusion

**CRITICAL FINDINGS**: PR #2396 introduces **major specification violations**
that fundamentally change the storage hierarchy model. The StorageBox entity
does not support the 2-5 level hierarchy required by the specification, and the
StorageRack simplification removes required fields for capacity calculation.

**RECOMMENDATION**: Either update the specification to match the new design (if
intentional) or revert PR #2396 changes to restore specification compliance. The
StorageBox design appears to be a significant architectural change that requires
specification approval before proceeding.

**PR #2400** changes are acceptable technical deviations that do not violate
functional requirements.
