# Spec vs. Code Alignment Analysis (OGC-149)

**Date**: 2025-12-09 **Comparison Targets**:

- **Specification**: Branch `spec/OGC-149-box-plate-hierarchy` (Files:
  `specs/149-box-plate-hierarchy/spec.md`, `data-model.md`)
- **Implementation**: `develop` branch (PR #2396, PR #2400)

---

## Executive Summary

There is a **CRITICAL ARCHITECTURAL MISMATCH** between the OGC-149 Specification
and the merged code in `develop`.

- **The Spec** defines a **6-level hierarchy**: Room → Device → Shelf → Rack →
  **Box/Plate** → **Position**. It retains the `StoragePosition` entity.
- **The Code** implements a **5-level hierarchy**: Room → Device → Shelf → Rack
  → **Box**. It **DELETED** the `StoragePosition` entity.

The code has fundamentally diverged from the specification's data model. The
code implements "Box" as the leaf node (the grid itself), whereas the spec
implements "Box/Plate" as a container that holds "Positions".

---

## Detailed Gap Analysis

### 1. Entity Structure (CRITICAL VIOLATION)

| Feature              | Specification (OGC-149)                                      | Code Implementation (PR #2396)               | Status              |
| :------------------- | :----------------------------------------------------------- | :------------------------------------------- | :------------------ |
| **Hierarchy Levels** | **6 Levels**: Room, Device, Shelf, Rack, Box/Plate, Position | **5 Levels**: Room, Device, Shelf, Rack, Box | 🔴 **MISMATCH**     |
| **Leaf Node**        | `StoragePosition` (Cell)                                     | `StorageBox` (Grid Container)                | 🔴 **MISMATCH**     |
| **Position Entity**  | **Retained**: Modified to have `parent_box_plate_id`.        | **Deleted**: `StoragePosition.java` removed. | 🔴 **MISMATCH**     |
| **Box Entity Name**  | `StorageBoxPlate`                                            | `StorageBox`                                 | ⚠️ **Naming Drift** |
| **Grid Dimensions**  | On `StorageBoxPlate`                                         | On `StorageBox`                              | ✅ **Aligned**      |
| **Rack Entity**      | Simplified (No dimensions)                                   | Simplified (No dimensions)                   | ✅ **Aligned**      |

**Impact**:

- The code cannot support the "Flexible 2-6 level hierarchy" defined in the
  spec.
- Assigning a sample to a specific "Position" (A1) in the code must now be done
  differently (likely as a property of the Assignment referencing the Box +
  Coordinate string), whereas the spec calls for a relational link to a Position
  entity.

### 2. Database Schema

| Table               | Specification                                       | Code Implementation                                          | Status              |
| :------------------ | :-------------------------------------------------- | :----------------------------------------------------------- | :------------------ |
| `storage_position`  | **Modified**: Adds `parent_box_plate_id`.           | **Dropped**: Table removed.                                  | 🔴 **MISMATCH**     |
| `storage_box_plate` | **Created**: With `rows`, `columns`.                | `storage_box` created.                                       | ⚠️ **Naming Drift** |
| `storage_rack`      | **Modified**: Drops grid cols, renames label->name. | **Modified**: Drops grid cols, renames label->name (likely). | ✅ **Aligned**      |

### 3. Sample Assignment Logic

- **Spec**: `SampleStorageAssignment` links to `location_id` (polymorphic).
  `location_type` includes `'box_plate'`.
- **Code**: `SampleStorageAssignment` links to `location_id`. `location_type`
  includes `'box'`.
- **Spec**: Assignments can be to a `StoragePosition` (via hierarchy traversal?
  Spec FR-021 says "Sample assignment MUST target Box/Plate positions").
  - _Correction_: Spec FR-021 says "Sample assignment MUST target Box/Plate
    positions". Spec FR-022 says "Position entity parent reference MUST be
    Box/Plate". This implies the Assignment points to... wait.
  - Spec Data Model says: `SampleStorageAssignment` references "StorageDevice,
    StorageShelf, or StorageRack" (and now Box/Plate?). It does _not_ reference
    StoragePosition directly in the polymorphic ID (based on previous spec
    reading, positions were text coordinates on the assignment?).
  - _Re-reading Spec Data Model_: "Uses polymorphic location... references
    StorageDevice, StorageShelf, or StorageRack... Optional position_coordinate
    (text field)".
  - **Spec Ambiguity/Conflict**: The Spec Data Model (Section 6) says Position
    is a text field. But Section 3 (StoragePosition) defines a
    `STORAGE_POSITION` entity. And FR-005 says "Box/Plate MUST have a
    one-to-many relationship with Position entities".
  - **Interpretation**: The Spec is internally conflicted or complex. It seems
    to want `StoragePosition` entities to exist (for occupancy tracking?), but
    Assignments might point to the _Parent_ + _Coordinate_?
- **Code Strategy**: By deleting `StoragePosition`, the code forces the
  "Assignment = Box ID + Coordinate Text" model. This actually **simplifies**
  the contradiction in the spec, but violates the requirement to have
  `StoragePosition` entities.

---

## Alignment Score: 40% (Fail)

The implementation achieves the "Add Box Level" goal but does so by
**destroying** the "Position Level" entity, which the spec explicitly sought to
preserve and extend.

## Recommendations

1.  **Update the Spec (Preferred)**: The Code's approach (removing the heavy
    `StoragePosition` entity table and likely relying on `StorageBox` +
    Coordinate) is likely more performant and simpler. The Spec should be
    updated to reflect the **removal** of `StoragePosition` and the 5-level
    hierarchy where "Position" is just a coordinate within a Box.
2.  **Align Naming**: Rename `StorageBox` to `StorageBoxPlate` in code OR update
    spec to `StorageBox`. (Code `StorageBox` is cleaner).
3.  **Fix Disposal (OGC-144)**: Your current task (OGC-144) needs to align with
    the **Code's** reality. You cannot rely on `StoragePosition` entities. You
    must implement disposal for samples assigned to a `StorageBox` with a
    coordinate.

## OGC-144 Action Plan (Revised)

Since `StoragePosition` is gone in `develop`:

1.  **Rebase `fix/OGC-144` on `develop`**.
2.  **Accept the deletion** of `StoragePosition`.
3.  **Update Disposal Logic**:
    - When disposing, if `location_type` is `'box'` (or `'box_plate'` if
      renamed), update the `SampleStorageAssignment`.
    - **Crucially**: The Code in PR #2400 _deletes_ the assignment on disposal.
      You want to _update_ it (set location to null) to keep metrics. You will
      need to restore the "Update" logic but make it work with `StorageBox`.
