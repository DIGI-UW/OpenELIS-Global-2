# FHIR Location Mapping: StorageBoxPlate

**Feature**: 149-box-plate-hierarchy  
**FHIR Version**: R4  
**Resource**: Location  
**Date**: December 5, 2025

## Overview

This document defines the FHIR R4 Location resource mapping for the new
`StorageBoxPlate` entity. Box/Plate represents a physical container (box, plate,
tray) with grid-based position structure, sitting between Rack and Position in
the 6-level storage hierarchy.

**Related Mappings**:

- StorageRack → Location (updated, grid extensions removed)
- StoragePosition → Location (updated for 6-level hierarchy)

---

## StorageBoxPlate → FHIR Location Resource

### Resource Structure

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [
    {
      "system": "http://openelis-global.org/storage/box-plate",
      "value": "{room_code}-{device_code}-{shelf_code}-{rack_code}-{box_plate_code}"
    }
  ],
  "status": "active" | "inactive",
  "name": "{name}",
  "type": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
          "code": "co",
          "display": "Container"
        }
      ]
    }
  ],
  "physicalType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
        "code": "co",
        "display": "Container"
      }
    ]
  },
  "partOf": {
    "reference": "Location/{parent_rack_fhir_uuid}",
    "display": "{parent_rack_name}"
  },
  "extension": [
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows",
      "valueInteger": {rows}
    },
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns",
      "valueInteger": {columns}
    },
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint",
      "valueString": "{position_schema_hint}"  // Optional
    }
  ]
}
```

---

## Field Mappings

| FHIR Field                                            | Source                  | Type      | Required | Notes                                                          |
| ----------------------------------------------------- | ----------------------- | --------- | -------- | -------------------------------------------------------------- |
| `id`                                                  | `fhir_uuid`             | UUID      | Yes      | Unique FHIR resource identifier                                |
| `identifier[0].system`                                | Constant                | URI       | Yes      | `http://openelis-global.org/storage/box-plate`                 |
| `identifier[0].value`                                 | Computed                | String    | Yes      | Hierarchical code: `{room}-{device}-{shelf}-{rack}-{boxplate}` |
| `status`                                              | `active`                | Code      | Yes      | `"active"` if true, `"inactive"` if false                      |
| `name`                                                | `name`                  | String    | Yes      | Human-readable box/plate name                                  |
| `type[0].coding[0].code`                              | Constant                | Code      | Yes      | `"co"` (container)                                             |
| `physicalType.coding[0].code`                         | Constant                | Code      | Yes      | `"co"` (container)                                             |
| `partOf.reference`                                    | `parent_rack.fhir_uuid` | Reference | Yes      | Reference to parent Rack Location                              |
| `extension[storage-grid-rows].valueInteger`           | `rows`                  | Integer   | Yes      | Grid row count (minimum 1)                                     |
| `extension[storage-grid-columns].valueInteger`        | `columns`               | Integer   | Yes      | Grid column count (minimum 1)                                  |
| `extension[storage-position-schema-hint].valueString` | `position_schema_hint`  | String    | No       | Optional position naming hint                                  |

---

## FHIR Extensions

### Extension: storage-grid-rows

**URL**:
`http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows`  
**Type**: `valueInteger`  
**Description**: Number of rows in the Box/Plate grid (minimum 1)  
**Required**: Yes

**Example**:

```json
{
  "url": "http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows",
  "valueInteger": 8
}
```

### Extension: storage-grid-columns

**URL**:
`http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns`  
**Type**: `valueInteger`  
**Description**: Number of columns in the Box/Plate grid (minimum 1)  
**Required**: Yes

**Example**:

```json
{
  "url": "http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns",
  "valueInteger": 12
}
```

### Extension: storage-position-schema-hint

**URL**:
`http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint`  
**Type**: `valueString`  
**Description**: Optional hint for position coordinate naming scheme (e.g.,
"A1", "1-1", "row-col")  
**Required**: No

**Example**:

```json
{
  "url": "http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint",
  "valueString": "A1"
}
```

---

## Example: 96-Well Plate

**Database Entity**:

```java
StorageBoxPlate {
  id: 123
  fhir_uuid: "a7f3c2b1-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
  name: "96-Well Plate 001"
  code: "PLATE001"
  rows: 8
  columns: 12
  position_schema_hint: "A1"
  active: true
  parent_rack_id: 45 (Rack "R1" in Shelf "S1" in Device "F1" in Room "LAB")
}
```

**FHIR Location Resource**:

```json
{
  "resourceType": "Location",
  "id": "a7f3c2b1-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "identifier": [
    {
      "system": "http://openelis-global.org/storage/box-plate",
      "value": "LAB-F1-S1-R1-PLATE001"
    }
  ],
  "status": "active",
  "name": "96-Well Plate 001",
  "type": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
          "code": "co",
          "display": "Container"
        }
      ]
    }
  ],
  "physicalType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
        "code": "co",
        "display": "Container"
      }
    ]
  },
  "partOf": {
    "reference": "Location/parent-rack-fhir-uuid-here",
    "display": "Rack R1"
  },
  "extension": [
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows",
      "valueInteger": 8
    },
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns",
      "valueInteger": 12
    },
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint",
      "valueString": "A1"
    }
  ]
}
```

**Calculated Capacity**: 8 × 12 = 96 positions

---

## Updated StorageRack FHIR Mapping

**Changes**: Remove grid extensions (moved to Box/Plate)

**Before (Feature 001)**:

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "name": "{label}",
  "extension": [
    {
      "url": "http://openelis-global.org/fhir/StructureDefinition/grid-dimensions",
      "valueString": "{rows} × {columns}"
    }
  ]
}
```

**After (OGC-149)**:

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "name": "{name}"
  // No grid extensions - Rack is now a simple container
}
```

---

## Hierarchy Representation in FHIR

**6-Level Hierarchy**:

```
Location (Room) - physicalType: "ro"
└─ Location (Device) - physicalType: "ve", partOf: Room
   └─ Location (Shelf) - physicalType: "co", partOf: Device
      └─ Location (Rack) - physicalType: "co", partOf: Shelf
         └─ Location (Box/Plate) - physicalType: "co", partOf: Rack, extensions: [rows, columns]
            └─ Location (Position) - physicalType: "co", partOf: Box/Plate
```

**FHIR Query Example** (retrieve all Box/Plates in a specific Rack):

```
GET /fhir/Location?partof=Location/{rack_fhir_uuid}&type=co
```

---

## Validation Rules

1. **Grid Dimensions**: `rows` and `columns` MUST be >= 1
2. **Parent Reference**: `partOf` MUST reference a valid Rack Location resource
3. **Identifier**: Hierarchical code MUST be unique across all Box/Plates
4. **Extensions**: All three extensions (rows, columns, schema hint) MUST use
   the specified URIs
5. **PhysicalType**: MUST be "co" (container)

---

## IHE mCSD Compliance

This mapping aligns with **IHE Mobile Care Services Discovery (mCSD)** profile
for Location resources:

- **Hierarchical Structure**: Location.partOf creates the hierarchy
- **Identifier**: Unique identifier for external system reference
- **Status**: Active/inactive status tracked
- **PhysicalType**: Uses standard code "co" for container

**mCSD Query Support**:

```
GET /fhir/Location?identifier=LAB-F1-S1-R1-PLATE001
GET /fhir/Location?name:contains=96-Well
GET /fhir/Location?partof:Location.name=Rack%20R1
```

---

## Implementation Notes

### Java Transform (StorageLocationFhirTransform.java)

```java
public Location transformBoxPlateToLocation(StorageBoxPlate boxPlate) {
    Location location = new Location();

    // Basic fields
    location.setId(boxPlate.getFhirUuidAsString());
    location.setName(boxPlate.getName());
    location.setStatus(boxPlate.getActive() ? LocationStatus.ACTIVE : LocationStatus.INACTIVE);

    // Identifier (hierarchical code)
    String hierarchicalCode = buildBoxPlateCode(boxPlate);
    location.addIdentifier()
        .setSystem("http://openelis-global.org/storage/box-plate")
        .setValue(hierarchicalCode);

    // Physical type
    CodeableConcept physicalType = new CodeableConcept();
    physicalType.addCoding()
        .setSystem("http://terminology.hl7.org/CodeSystem/location-physical-type")
        .setCode("co")
        .setDisplay("Container");
    location.setPhysicalType(physicalType);
    location.addType(physicalType);

    // Parent reference
    location.setPartOf(new Reference(
        "Location/" + boxPlate.getParentRack().getFhirUuidAsString()
    ).setDisplay(boxPlate.getParentRack().getName()));

    // Extensions (FR-025)
    location.addExtension()
        .setUrl("http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows")
        .setValue(new IntegerType(boxPlate.getRows()));

    location.addExtension()
        .setUrl("http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns")
        .setValue(new IntegerType(boxPlate.getColumns()));

    if (boxPlate.getPositionSchemaHint() != null) {
        location.addExtension()
            .setUrl("http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint")
            .setValue(new StringType(boxPlate.getPositionSchemaHint()));
    }

    return location;
}

private String buildBoxPlateCode(StorageBoxPlate boxPlate) {
    StorageRack rack = boxPlate.getParentRack();
    StorageShelf shelf = rack.getParentShelf();
    StorageDevice device = shelf.getParentDevice();
    StorageRoom room = device.getParentRoom();

    return String.format("%s-%s-%s-%s-%s",
        room.getCode(),
        device.getCode(),
        shelf.getCode(),
        rack.getCode(),
        boxPlate.getCode()
    );
}
```

---

## Summary

- **Resource Type**: Location (standard FHIR R4)
- **Physical Type**: "co" (container) - aligns with Rack, Shelf
- **Hierarchy Level**: 5 (Room → Device → Shelf → Rack → **Box/Plate**)
- **Key Extensions**: storage-grid-rows, storage-grid-columns,
  storage-position-schema-hint
- **Parent**: Rack Location resource
- **Children**: Position Location resources
- **IHE Compliance**: Fully compliant with mCSD profile
