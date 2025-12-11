# Research: Box Storage Hierarchy Enhancement

**Feature**: 149-box-plate-hierarchy  
**Parent Feature**: 001-sample-storage  
**Date**: December 5, 2025  
**Status**: Draft

## Executive Summary

This document provides background research and impact analysis for implementing
the Box Storage Hierarchy Enhancement (OGC-149). It analyzes the current Feature
001 implementation, identifies affected files, and documents the barcode parsing
strategy.

**Key Findings**:

- Feature 001 has ~40 backend files that reference StorageRack
- Rack entity currently has grid fields (rows, columns, positionSchemaHint) that
  need removal
- Barcode validation service already exists and can be extended for 5-level
  hierarchy with optional position coordinate
- FHIR transform service follows consistent pattern that can be applied to Box
- Position hierarchy is already flexible (2-4 levels), extending to 5 levels
  (Box) with virtual positions is straightforward

---

## 1. Feature 001 Current Implementation Analysis

### 1.1 StorageRack Entity (Current State)

**File**:
`src/main/java/org/openelisglobal/storage/valueholder/StorageRack.java`

**Current Fields**:

```java
@Column(name = "LABEL", length = 100, nullable = false)
private String label;  // ← WILL BE RENAMED TO 'name'

@Column(name = "ROWS", nullable = false)
private Integer rows;  // ← WILL BE REMOVED (moved to Box)

@Column(name = "COLUMNS", nullable = false)
private Integer columns;  // ← WILL BE REMOVED (moved to Box)

@Column(name = "POSITION_SCHEMA_HINT", length = 50)
private String positionSchemaHint;  // ← WILL BE REMOVED (moved to Box)
```

**Current Computed Method**:

```java
public Integer getCapacity() {
    if (rows == null || columns == null || rows == 0 || columns == 0) {
        return 0;
    }
    return rows * columns;  // ← WILL BE REMOVED (capacity moves to Box)
}
```

**FHIR Sync Hooks**:

```java
@PostPersist
protected void onPostPersist() {
    syncToFhir(true);  // ← Already exists, no changes needed
}

@PostUpdate
protected void onPostUpdate() {
    syncToFhir(false);  // ← Already exists, no changes needed
}
```

**Impact**: StorageRack is already using JPA annotations and FHIR sync. Changes
are straightforward removals + rename.

---

### 1.2 Virtual Position (Develop Implementation)

- `StoragePosition` entity is **removed** (no table).
- Position is stored as `position_coordinate` (text) on
  `SampleStorageAssignment`.
- `location_type` enum includes `'box'`; occupancy is derived from assignments
  to a Box ID.
- Supports free-text positions at any hierarchy level (rack/shelf/box) without
  pre-generating empty slots.

---

### 1.3 FHIR Transform Service (Current Pattern)

**File**:
`src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java`

**Rack Transform Pattern**:

```java
public Location transformRackToLocation(StorageRack rack) {
    Location location = new Location();
    location.setId(rack.getFhirUuidAsString());
    location.setName(rack.getName());
    location.setStatus(rack.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);

    // Hierarchical identifier
    String hierarchicalCode = buildRackCode(rack);  // Room-Device-Shelf-Rack
    location.addIdentifier()
        .setSystem("http://openelis-global.org/storage/rack")
        .setValue(hierarchicalCode);

    // Physical type
    CodeableConcept physicalType = new CodeableConcept();
    physicalType.addCoding()
        .setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type")
        .setCode("co");  // Container
    location.setPhysicalType(physicalType);

    // Parent reference
    location.setPartOf(new Reference("Location/" + rack.getParentShelf().getFhirUuidAsString()));

    return location;
}
```

**Box Transform Pattern**:

```java
public Location transformBoxToLocation(StorageBox box) {
    Location location = new Location();
    location.setId(box.getFhirUuidAsString());
    location.setName(box.getName());
    location.setStatus(box.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);

    // Hierarchical identifier: Room-Device-Shelf-Rack-Box
    String hierarchicalCode = buildBoxCode(box);
    location.addIdentifier()
        .setSystem("http://openelis-global.org/storage/box")
        .setValue(hierarchicalCode);

    // Physical type
    CodeableConcept physicalType = new CodeableConcept();
    physicalType.addCoding()
        .setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type")
        .setCode("co");  // Container
    location.setPhysicalType(physicalType);

    // Parent reference to Rack
    location.setPartOf(new Reference("Location/" + box.getParentRack().getFhirUuidAsString()));

    // Grid dimensions extensions (FR-025)
    location.addExtension(new Extension("http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows",
                                        new IntegerType(box.getRows())));
    location.addExtension(new Extension("http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns",
                                        new IntegerType(box.getColumns())));

    if (box.getPositionSchemaHint() != null) {
        location.addExtension(new Extension("http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint",
                                            new StringType(box.getPositionSchemaHint())));
    }

    return location;
}
```

**Impact**: FHIR transform pattern is consistent and well-established. Box
follows same structure with updated extension URIs.

---

### 1.4 Barcode Validation Service (Current Pattern)

**File**:
`src/main/java/org/openelisglobal/storage/service/BarcodeValidationServiceImpl.java`

**Current Parsing Logic** (Feature 001):

```java
public BarcodeValidationResult parseBarcode(String barcode) {
    String[] segments = barcode.split("-");

    // Validate Room (segment 0)
    StorageRoom room = roomDAO.findByCode(segments[0]);
    if (room == null) {
        return BarcodeValidationResult.error("Room not found: " + segments[0]);
    }
    result.setRoom(room);

    // Validate Device (segment 1)
    if (segments.length > 1) {
        StorageDevice device = deviceDAO.findByCodeAndParent(segments[1], room.getId());
        if (device == null) {
            return result.withWarning("Device not found: " + segments[1]);
        }
        result.setDevice(device);
    }

    // ... similar for Shelf (segment 2), Rack (segment 3)

    return result;
}
```

**Enhanced Parsing Logic** (OGC-149 - Generic Left-to-Right, FR-029):

```java
public BarcodeValidationResult parseBarcode(String barcode) {
    String[] segments = barcode.split("-");
    BarcodeValidationResult result = new BarcodeValidationResult();

    // Segment 0: Room (required)
    if (segments.length < 1) {
        return result.error("Barcode must have at least 1 segment (Room)");
    }
    StorageRoom room = roomDAO.findByCode(segments[0]);
    if (room == null) {
        return result.error("Room '" + segments[0] + "' not found");
    }
    result.setRoom(room);

    // Segment 1: Device (stop if invalid)
    if (segments.length < 2) {
        return result.success();  // Valid 1-segment barcode
    }
    StorageDevice device = deviceDAO.findByCodeAndParent(segments[1], room.getId());
    if (device == null) {
        return result.autofill(room)
                     .warning("Device '" + segments[1] + "' not found in Room '" + room.getCode() + "'");
    }
    result.setDevice(device);

    // Segment 2: Shelf (stop if invalid)
    if (segments.length < 3) {
        return result.success();  // Valid 2-segment barcode
    }
    StorageShelf shelf = shelfDAO.findByCodeAndParent(segments[2], device.getId());
    if (shelf == null) {
        return result.autofill(room, device)
                     .warning("Shelf '" + segments[2] + "' not found in Device '" + device.getCode() + "'");
    }
    result.setShelf(shelf);

    // Segment 3: Rack (stop if invalid)
    if (segments.length < 4) {
        return result.success();  // Valid 3-segment barcode
    }
    StorageRack rack = rackDAO.findByCodeAndParent(segments[3], shelf.getId());
    if (rack == null) {
        return result.autofill(room, device, shelf)
                     .warning("Rack '" + segments[3] + "' not found in Shelf '" + shelf.getCode() + "'");
    }
    result.setRack(rack);

// Segment 4: Box (stop if invalid) - NEW
    if (segments.length < 5) {
        return result.success();  // Valid 4-segment barcode
    }
StorageBox box = boxDAO.findByCodeAndParent(segments[4], rack.getId());
if (box == null) {
        return result.autofill(room, device, shelf, rack)
                 .warning("Box '" + segments[4] + "' not found in Rack '" + rack.getCode() + "'");
    }
result.setBox(box);

// Segment 5: Position coordinate (optional text, not validated against DB) - NEW
    if (segments.length >= 6) {
        result.setPositionCoordinate(segments[5]);
    }

    return result.success();
}
```

**Key Design Decisions**:

1. **Generic left-to-right parsing** - no special "legacy" detection (FR-032)
2. **Stop at first invalid segment** - autofill valid levels, warn for unmatched
   (FR-029, FR-030)
3. **Contextual warning messages** - include parent context (FR-031)
4. **No segment count assumptions** - works for 1-6 segments

**Example Behaviors**:

| Barcode                | Valid Segments | Autofilled Levels                        | Warning                                    |
| ---------------------- | -------------- | ---------------------------------------- | ------------------------------------------ |
| `LAB-F1-S1-R1-BOX1-A5` | All 6          | Room, Device, Shelf, Rack, Box, Position | None                                       |
| `LAB-F1-S1-R1-INVALID` | 4 of 5         | Room, Device, Shelf, Rack                | "Box 'INVALID' not found in Rack 'R1'"     |
| `LAB-F1-S1-R1`         | All 4          | Room, Device, Shelf, Rack                | None (valid 4-level)                       |
| `LAB-INVALID`          | 1 of 2         | Room                                     | "Device 'INVALID' not found in Room 'LAB'" |

---

## 2. Impact Analysis

### 2.1 Backend Files Affected

**Entities (Valueholders)**:

- `StorageRack` — remove grid fields, rename `label` → `name`.
- `StorageBox` — new entity (grid dimensions, barcode, parent_rack_id).
- `SampleStorageAssignment` — add location_type = 'box', store
  `position_coordinate` text.
- `SampleStorageMovement` — extend location_type enums to include 'box'.

**DAOs**:

- `StorageRackDAO/Impl` — queries without grid fields.
- `StorageBoxDAO/Impl` — new DAO for boxes and occupancy.
- `SampleStorageAssignmentDAO/Impl` — box lookups, occupied coordinates.

**Services**:

- `StorageLocationService/Impl` — Box CRUD, rack path building.
- `StorageDashboardService/Impl` — box metrics.
- `BarcodeValidationServiceImpl` — 5-level barcode (Room→Device→Shelf→Rack→Box +
  optional coordinate).
- `SampleStorageService/Impl` — assignments/movements for box, virtual
  positions, disposal support.
- `LabelManagementServiceImpl` — barcode generation with rack shortCode + box
  code.

**Controllers**:

- `StorageLocationRestController` — Box endpoints (create/update/get), rack
  payload uses shortCode.
- `SampleStorageRestController` — assignment APIs accept box + position
  coordinate.
- `LabelManagementRestController` — barcode generation uses rack shortCode.

**Forms**:

- `StorageRackForm` — simplified (no rows/columns/position_schema_hint, uses
  shortCode).
- `StorageBoxForm` — new (rows, columns, position_schema_hint, shortCode,
  parent_rack_id).

**FHIR**:

- `StorageLocationFhirTransform` — rack without grid extensions; box transform
  with grid + schema-hint extensions.

**Tests**:

- Rack/controller/service tests updated for shortCode and box path.
- Assignment/disposal tests updated to use box + position_coordinate.

---

### 2.2 Frontend Files Affected

**Storage Dashboard / Selector**:

- `frontend/src/components/storage/StorageDashboard/StorageDashboard.jsx` —
  Boxes tab, rack→box dropdowns, grid preview and assignment form.
- `frontend/src/components/storage/StorageDashboard/StorageLocationsMetricCard.jsx`
  — include box counts.
- `frontend/src/components/storage/StorageLocationHierarchy.jsx` / tables —
  render Box level.
- `frontend/src/components/storage/StorageDashboard/StorageAssignmentModal.jsx`
  — select Box + position text coordinate.
- `frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.jsx`
  — parse 5-level barcode + optional coordinate.

**Location Management**:

- `frontend/src/components/storage/LocationManagement/EditLocationModal.jsx` —
  rack fields simplified (no grid).
- Box create/edit modal for dimensions and schema hint.

**Internationalization**:

- `frontend/src/languages/en.json`, `frontend/src/languages/fr.json` — box tab,
  grid labels, position strings.

---

### 2.3 Database Migration Files

**Liquibase Changesets**:

- Existing storage changelog (`src/main/resources/liquibase/3.3.x.x/`) drops
  rack grid columns, creates `storage_box`, and extends location_type
  checks/indexes/seeds for `'box'`.

**Persistence Configuration**:

- No new `persistence.xml` entries required (Box entity discovered via package
  scan).

---

## 3. Barcode Parsing Strategy Research

### 3.1 Requirements Analysis (FR-029 through FR-032)

**FR-029**: Generic left-to-right hierarchical validation

- Split by delimiter (hyphen)
- Validate each segment against database
- Stop at first unmatched segment
- Autofill all preceding valid levels
- Display contextual warning

**FR-030**: Autofill all validated hierarchy levels

- Populate UI dropdowns with validated entities
- Stop at first invalid segment
- Allow user to correct invalid segment or continue

**FR-031**: Contextual warning messages

- Include segment value, parent context, and reason
- Example: "Device 'INVALID' not found in Room 'LAB'"

**FR-032**: No special legacy handling

- All barcodes use same parsing logic
- No segment count assumptions
- Works for 1-6 segments

### 3.2 Parsing Algorithm

```
INPUT: barcode string (e.g., "LAB-F1-S1-R1-BOX1")

1. Split by delimiter '-'
   segments = ["LAB", "F1", "S1", "R1", "BOX1"]

2. Initialize result object
   result = { valid: [], warnings: [] }

3. For each segment (left to right):
   a. Validate segment against expected entity type
      - Segment 0: Room
      - Segment 1: Device (within parent Room)
      - Segment 2: Shelf (within parent Device)
      - Segment 3: Rack (within parent Shelf)
      - Segment 4: Box (within parent Rack)
      - Segment 5: Position coordinate (text, not validated)

   b. If validation succeeds:
      - Add entity to result.valid
      - Continue to next segment

   c. If validation fails:
      - Add warning to result.warnings
      - Stop processing (don't validate remaining segments)
      - Return result with partial autofill

4. Return result with all validated entities
```

### 3.3 Database Lookup Sequence

Each segment requires a database lookup:

```sql
-- Segment 0: Room
SELECT * FROM storage_room WHERE code = 'LAB' AND active = true;

-- Segment 1: Device (requires parent Room ID)
SELECT * FROM storage_device
WHERE code = 'F1'
  AND parent_room_id = :roomId
  AND active = true;

-- Segment 2: Shelf (requires parent Device ID)
SELECT * FROM storage_shelf
WHERE code = 'S1'
  AND parent_device_id = :deviceId
  AND active = true;

-- Segment 3: Rack (requires parent Shelf ID)
SELECT * FROM storage_rack
WHERE code = 'R1'
  AND parent_shelf_id = :shelfId
  AND active = true;

-- Segment 4: Box (requires parent Rack ID) - NEW
SELECT * FROM storage_box
WHERE code = 'BOX1'
  AND parent_rack_id = :rackId
  AND active = true;

-- Segment 5: Position coordinate (text, no lookup)
-- Just store as string for position_coordinate field
```

**Performance**: 5 database queries max (1 per hierarchy level). With proper
indexing on `(parent_id, code)`, each lookup is O(1).

### 3.4 Example Scenarios with Expected Outcomes

**Scenario 1: Valid 6-level barcode**

Input: `LAB-F1-S1-R1-BOX1-A5`

| Segment | Value | Validation                 | Result  |
| ------- | ----- | -------------------------- | ------- |
| 0       | LAB   | Room found                 | ✓ Valid |
| 1       | F1    | Device found in LAB        | ✓ Valid |
| 2       | S1    | Shelf found in F1          | ✓ Valid |
| 3       | R1    | Rack found in S1           | ✓ Valid |
| 4       | BOX1  | Box found in R1            | ✓ Valid |
| 5       | A5    | Position coordinate (text) | ✓ Valid |

**Output**:

- Autofill: Room=LAB, Device=F1, Shelf=S1, Rack=R1, Box=BOX1, Position=A5
- Warning: None

---

**Scenario 2: Invalid device (stop at segment 1)**

Input: `LAB-INVALID-S1-R1`

| Segment | Value   | Validation              | Result           |
| ------- | ------- | ----------------------- | ---------------- |
| 0       | LAB     | Room found              | ✓ Valid          |
| 1       | INVALID | Device NOT found in LAB | ✗ Invalid - STOP |
| 2       | S1      | (Not processed)         | -                |
| 3       | R1      | (Not processed)         | -                |

**Output**:

- Autofill: Room=LAB
- Warning: "Device 'INVALID' not found in Room 'LAB'"

---

**Scenario 3: Valid 4-level barcode (legacy Rack-level)**

Input: `LAB-F1-S1-R1`

| Segment | Value | Validation          | Result  |
| ------- | ----- | ------------------- | ------- |
| 0       | LAB   | Room found          | ✓ Valid |
| 1       | F1    | Device found in LAB | ✓ Valid |
| 2       | S1    | Shelf found in F1   | ✓ Valid |
| 3       | R1    | Rack found in S1    | ✓ Valid |

**Output**:

- Autofill: Room=LAB, Device=F1, Shelf=S1, Rack=R1
- Warning: None
- Note: This is a valid barcode (Rack-level assignment), no special "legacy"
  handling needed

---

**Scenario 4: Invalid Box (stop at segment 4)**

Input: `LAB-F1-S1-R1-INVALID-A5`

| Segment | Value   | Validation          | Result           |
| ------- | ------- | ------------------- | ---------------- |
| 0       | LAB     | Room found          | ✓ Valid          |
| 1       | F1      | Device found in LAB | ✓ Valid          |
| 2       | S1      | Shelf found in F1   | ✓ Valid          |
| 3       | R1      | Rack found in S1    | ✓ Valid          |
| 4       | INVALID | Box NOT found in R1 | ✗ Invalid - STOP |
| 5       | A5      | (Not processed)     | -                |

**Output**:

- Autofill: Room=LAB, Device=F1, Shelf=S1, Rack=R1
- Warning: "Box 'INVALID' not found in Rack 'R1'"

---

### 3.5 UI Interaction Flow

```
User scans barcode: LAB-F1-S1-R1-INVALID

1. Barcode input field captures scan
2. Debounce (500ms) - wait for complete barcode
3. Call barcode validation API
4. API returns:
   {
     "valid": true,
     "autofill": {
       "room": { "id": 1, "code": "LAB", "name": "Main Lab" },
       "device": { "id": 5, "code": "F1", "name": "Freezer 1" },
       "shelf": { "id": 12, "code": "S1", "name": "Shelf 1" },
       "rack": { "id": 45, "code": "R1", "name": "Rack R1" }
     },
     "warnings": [
       "Box 'INVALID' not found in Rack 'R1'"
     ]
   }
5. UI populates dropdowns:
   - Room dropdown: "LAB - Main Lab" (selected)
   - Device dropdown: "F1 - Freezer 1" (selected)
   - Shelf dropdown: "S1 - Shelf 1" (selected)
   - Rack dropdown: "R1 - Rack R1" (selected)
   - Box/Plate dropdown: (empty, with warning icon)
6. Warning toast/banner appears:
   "⚠️ Box/Plate 'INVALID' not found in Rack 'R1'"
7. User can:
   - Manually select correct Box/Plate from dropdown
   - Create new Box/Plate with code 'INVALID'
   - Continue assignment at Rack level (flexible hierarchy)
```

---

## 4. Technical Constraints and Considerations

### 4.1 Backward Compatibility

**Existing Rack Data**:

- Feature 001 not in production → destructive migration acceptable
- Existing test data will be dropped and recreated
- No production data migration concerns

**Existing Barcodes**:

- 4-level barcodes (Room-Device-Shelf-Rack) remain valid
- No special handling needed (generic parser handles 1-6 segments)
- UI autofills up to 4 levels, user can add Box manually

### 4.2 Performance Considerations

**Barcode Validation**:

- Max 5 database queries per barcode scan (1 per hierarchy level)
- Indexed lookups on `(parent_id, code)` → O(1) performance
- Total validation time < 50ms for 5-level barcode

**FHIR Sync**:

- Box creation triggers FHIR Location resource creation
- Follows same pattern as Rack (already proven performant)
- Async sync possible if needed (not required for POC)

### 4.3 Testing Strategy

**Unit Tests**:

- StorageBox entity (getters, setters, capacity calculation)
- Box DAO (CRUD, findByCodeAndParent)
- Box service (validation, code generation)
- Barcode parser (all scenarios documented in 3.4)

**Integration Tests**:

- Box REST endpoints (POST, GET, PUT, DELETE)
- Rack simplification (verify grid fields removed)
- Box assignment hierarchy (5-level support with position_coordinate)
- FHIR transform (rack container; box grid extensions)

**E2E Tests**:

- Create Box via dashboard
- Assign sample to Box position (with coordinate)
- Scan barcode with Box level
- Dashboard displays Boxes tab with data

---

## 5. Dependencies and Prerequisites

### 5.1 Feature 001 Completion Status

**Required from Feature 001**:

- ✅ StorageRack entity implemented
- ✅ FHIR transform service implemented
- ✅ Barcode validation service implemented
- ✅ Storage dashboard implemented
- ✅ Location selector widget implemented

**All prerequisites met** - OGC-149 can proceed

### 5.2 External Dependencies

**None** - All required infrastructure exists in Feature 001

---

## Summary

**Research Findings**:

- Feature 001 provides solid foundation for OGC-149
- Rack entity changes are straightforward (removals + rename)
- Position hierarchy extension is natural (already flexible 2-5 levels)
- FHIR transform pattern is consistent and proven
- Barcode parser can be extended with generic left-to-right logic
- No backward compatibility concerns (Feature 001 not in production)

**Recommendation**: Proceed with implementation as planned. All technical risks
mitigated.
