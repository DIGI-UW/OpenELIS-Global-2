# FHIR R4 Location Resource Mappings

**Date**: 2025-10-30  
**Feature**: Sample Storage Management POC  
**Branch**: 001-sample-storage  
**FHIR Version**: R4 (4.0.1)  
**Profile**: IHE mCSD (Mobile Care Services Discovery)

## Overview

This document specifies how OpenELIS sample storage entities map to FHIR R4 Location resources for external interoperability. The mapping follows IHE mCSD profile requirements for hierarchical location structures.

**Scope**: Room, Device, Shelf, Rack → FHIR Location resources  
**Out of Scope**: Individual positions (tracked in OpenELIS database only)

---

## 1. StorageRoom → FHIR Location

**Physical Type**: `ro` (room)

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{code}"
  }],
  "status": "active" | "inactive",
  "name": "{name}",
  "description": "{description}",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro",
      "display": "Room"
    }]
  },
  "managingOrganization": {
    "reference": "Organization/{openelis_organization_id}",
    "display": "OpenELIS Laboratory"
  },
  "meta": {
    "profile": ["http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location"],
    "tag": [{
      "system": "http://openelis.org/fhir/tag/storage-hierarchy",
      "code": "room"
    }]
  }
}
```

**Mapping Table**:

| OpenELIS Field | FHIR R4 Location Field | Notes |
|----------------|------------------------|-------|
| `fhir_uuid` | `Location.id` | Primary FHIR resource identifier |
| `code` | `Location.identifier[0].value` | Unique room code |
| `name` | `Location.name` | Human-readable room name |
| `description` | `Location.description` | Optional room description |
| `active` | `Location.status` | `true` → "active", `false` → "inactive" |
| - | `Location.mode` | Always "instance" (physical location) |
| - | `Location.physicalType` | Always "ro" (room) |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "MAIN"
  }],
  "status": "active",
  "name": "Main Laboratory",
  "description": "Primary laboratory storage facility",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro",
      "display": "Room"
    }]
  },
  "meta": {
    "profile": ["http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location"]
  }
}
```

---

## 2. StorageDevice → FHIR Location

**Physical Type**: `ve` (vehicle/equipment)

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{room_code}-{device_code}"
  }],
  "status": "active" | "inactive",
  "name": "{name}",
  "mode": "instance",
  "type": [{
    "coding": [{
      "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
      "code": "freezer" | "refrigerator" | "cabinet" | "other",
      "display": "Freezer" | "Refrigerator" | "Cabinet" | "Other"
    }]
  }],
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ve",
      "display": "Vehicle"
    }],
    "text": "Storage Equipment"
  },
  "partOf": {
    "reference": "Location/{parent_room_fhir_uuid}",
    "display": "{parent_room_name}"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/storage-temperature",
    "valueDecimal": -80.0
  }, {
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 500
  }],
  "meta": {
    "profile": ["http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location"],
    "tag": [{
      "system": "http://openelis.org/fhir/tag/storage-hierarchy",
      "code": "device"
    }]
  }
}
```

**Mapping Table**:

| OpenELIS Field | FHIR R4 Location Field | Notes |
|----------------|------------------------|-------|
| `fhir_uuid` | `Location.id` | Primary FHIR resource identifier |
| `room.code + code` | `Location.identifier[0].value` | Hierarchical code (e.g., "MAIN-FRZ01") |
| `name` | `Location.name` | Device name |
| `type` | `Location.type[0].coding.code` | freezer/refrigerator/cabinet/other |
| `active` | `Location.status` | `true` → "active", `false` → "inactive" |
| `parent_room.fhir_uuid` | `Location.partOf.reference` | Reference to parent Room Location |
| `temperature_setting` | `Location.extension[storage-temperature]` | Optional temperature in Celsius |
| `capacity_limit` | `Location.extension[storage-capacity]` | Optional capacity limit |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "MAIN-FRZ01"
  }],
  "status": "active",
  "name": "Freezer Unit 1",
  "mode": "instance",
  "type": [{
    "coding": [{
      "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
      "code": "freezer",
      "display": "Freezer"
    }]
  }],
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ve",
      "display": "Vehicle"
    }],
    "text": "Storage Equipment"
  },
  "partOf": {
    "reference": "Location/a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "display": "Main Laboratory"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/storage-temperature",
    "valueDecimal": -80.0
  }, {
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 500
  }]
}
```

---

## 3. StorageShelf → FHIR Location

**Physical Type**: `co` (container)

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{room_code}-{device_code}-{shelf_label}"
  }],
  "status": "active" | "inactive",
  "name": "{label}",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "co",
      "display": "Container"
    }],
    "text": "Storage Shelf"
  },
  "partOf": {
    "reference": "Location/{parent_device_fhir_uuid}",
    "display": "{parent_device_name}"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 100
  }],
  "meta": {
    "profile": ["http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location"],
    "tag": [{
      "system": "http://openelis.org/fhir/tag/storage-hierarchy",
      "code": "shelf"
    }]
  }
}
```

**Mapping Table**:

| OpenELIS Field | FHIR R4 Location Field | Notes |
|----------------|------------------------|-------|
| `fhir_uuid` | `Location.id` | Primary FHIR resource identifier |
| `room.code + device.code + label` | `Location.identifier[0].value` | Hierarchical code (e.g., "MAIN-FRZ01-SHA") |
| `label` | `Location.name` | Shelf label |
| `active` | `Location.status` | `true` → "active", `false` → "inactive" |
| `parent_device.fhir_uuid` | `Location.partOf.reference` | Reference to parent Device Location |
| `capacity_limit` | `Location.extension[storage-capacity]` | Optional capacity limit |

---

## 4. StorageRack → FHIR Location

**Physical Type**: `co` (container)

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{room_code}-{device_code}-{shelf_label}-{rack_label}"
  }],
  "status": "active" | "inactive",
  "name": "{label}",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "co",
      "display": "Container"
    }],
    "text": "Storage Rack"
  },
  "partOf": {
    "reference": "Location/{parent_shelf_fhir_uuid}",
    "display": "{parent_shelf_label}"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/rack-grid-dimensions",
    "valueString": "{rows} × {columns}"
  }, {
    "url": "http://openelis.org/fhir/extension/rack-position-schema-hint",
    "valueString": "{position_schema_hint}"
  }, {
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 81
  }],
  "meta": {
    "profile": ["http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location"],
    "tag": [{
      "system": "http://openelis.org/fhir/tag/storage-hierarchy",
      "code": "rack"
    }]
  }
}
```

**Mapping Table**:

| OpenELIS Field | FHIR R4 Location Field | Notes |
|----------------|------------------------|-------|
| `fhir_uuid` | `Location.id` | Primary FHIR resource identifier |
| `room.code + device.code + shelf.label + label` | `Location.identifier[0].value` | Full hierarchical code (e.g., "MAIN-FRZ01-SHA-RKR1") |
| `label` | `Location.name` | Rack label |
| `active` | `Location.status` | `true` → "active", `false` → "inactive" |
| `parent_shelf.fhir_uuid` | `Location.partOf.reference` | Reference to parent Shelf Location |
| `rows * columns` | `Location.extension[storage-capacity]` | Calculated capacity |
| `rows` and `columns` | `Location.extension[rack-grid-dimensions]` | Grid dimensions as string (e.g., "9 × 9") |
| `position_schema_hint` | `Location.extension[rack-position-schema-hint]` | Optional hint for position naming |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "d4e5f6a7-b8c9-0123-defg-h23456789012",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "MAIN-FRZ01-SHA-RKR1"
  }],
  "status": "active",
  "name": "Rack R1",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "co",
      "display": "Container"
    }],
    "text": "Storage Rack"
  },
  "partOf": {
    "reference": "Location/c3d4e5f6-a7b8-9012-cdef-g12345678901",
    "display": "Shelf-A"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/rack-grid-dimensions",
    "valueString": "9 × 9"
  }, {
    "url": "http://openelis.org/fhir/extension/rack-position-schema-hint",
    "valueString": "A1"
  }, {
    "url": "http://openelis.org/fhir/extension/storage-capacity",
    "valueInteger": 81
  }]
}
```

---

## 5. StoragePosition → FHIR Location

**Physical Type**: `co` (container)

**Rationale**: Positions mapped to FHIR Location resources for complete storage hierarchy in FHIR server. Enables full interoperability and external query capabilities.

```json
{
  "resourceType": "Location",
  "id": "{fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "{room_code}-{device_code}-{shelf_label}-{rack_label}-{coordinate}"
  }],
  "status": "active" | "inactive",
  "name": "{coordinate}",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "co",
      "display": "Container"
    }],
    "text": "Storage Position"
  },
  "partOf": {
    "reference": "Location/{parent_rack_fhir_uuid}",
    "display": "{parent_rack_label}"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/position-occupancy",
    "valueBoolean": true
  }, {
    "url": "http://openelis.org/fhir/extension/position-grid-row",
    "valueInteger": 1
  }, {
    "url": "http://openelis.org/fhir/extension/position-grid-column",
    "valueInteger": 5
  }],
  "meta": {
    "profile": ["http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location"],
    "tag": [{
      "system": "http://openelis.org/fhir/tag/storage-hierarchy",
      "code": "position"
    }]
  }
}
```

**Mapping Table**:

| OpenELIS Field | FHIR R4 Location Field | Notes |
|----------------|------------------------|-------|
| `fhir_uuid` | `Location.id` | Primary FHIR resource identifier |
| Full hierarchical code | `Location.identifier[0].value` | Complete path code (e.g., "MAIN-FRZ01-SHA-RKR1-A5") |
| `coordinate` | `Location.name` | Position coordinate (free-text) |
| `active` | `Location.status` | Always "active" (positions don't have active/inactive state separately) |
| `parent_rack.fhir_uuid` | `Location.partOf.reference` | Reference to parent Rack Location |
| `occupied` | `Location.extension[position-occupancy]` | Boolean occupancy status |
| `row_index` | `Location.extension[position-grid-row]` | Optional row number for grid visualization |
| `column_index` | `Location.extension[position-grid-column]` | Optional column number for grid visualization |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "e5f6a7b8-c9d0-1234-efgh-i34567890124",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "MAIN-FRZ01-SHA-RKR1-A5"
  }],
  "status": "active",
  "name": "A5",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "co",
      "display": "Container"
    }],
    "text": "Storage Position"
  },
  "partOf": {
    "reference": "Location/d4e5f6a7-b8c9-0123-defg-h23456789012",
    "display": "Rack R1"
  },
  "extension": [{
    "url": "http://openelis.org/fhir/extension/position-occupancy",
    "valueBoolean": true
  }, {
    "url": "http://openelis.org/fhir/extension/position-grid-row",
    "valueInteger": 1
  }, {
    "url": "http://openelis.org/fhir/extension/position-grid-column",
    "valueInteger": 5
  }]
}
```

---

## 6. Sample-to-Location Link via Specimen Resource

**OpenELIS Entity**: `SampleStorageAssignment`  
**FHIR Resource**: `Specimen` (existing resource type for samples)

```json
{
  "resourceType": "Specimen",
  "id": "{sample_fhir_uuid}",
  "identifier": [{
    "system": "http://openelis.org/accession-number",
    "value": "{accession_number}"
  }],
  "status": "available",
  "container": [{
    "identifier": {
      "value": "{hierarchical_location_path}"
    },
    "extension": [{
      "url": "http://openelis.org/fhir/extension/storage-position-location",
      "valueReference": {
        "reference": "Location/{position_fhir_uuid}",
        "display": "{hierarchical_path_to_position}"
      }
    }, {
      "url": "http://openelis.org/fhir/extension/storage-assigned-by",
      "valueReference": {
        "reference": "Practitioner/{user_fhir_uuid}",
        "display": "{user_name}"
      }
    }, {
      "url": "http://openelis.org/fhir/extension/storage-assigned-date",
      "valueDateTime": "{assigned_date}"
    }]
  }]
}
```

**Mapping Table**:

| OpenELIS Field | FHIR Specimen Field | Notes |
|----------------|---------------------|-------|
| `sample.fhir_uuid` | `Specimen.id` | Sample FHIR identifier |
| `sample.accession_number` | `Specimen.identifier[0].value` | Accession number |
| Full hierarchical path | `Specimen.container.identifier.value` | "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5" |
| `storage_position.fhir_uuid` | `Specimen.container.extension[storage-position-location]` | Reference to Position Location resource |
| `assigned_by_user.fhir_uuid` | `Specimen.container.extension[storage-assigned-by]` | User who assigned |
| `assigned_date` | `Specimen.container.extension[storage-assigned-date]` | Assignment timestamp |

**Example**:

```json
{
  "resourceType": "Specimen",
  "id": "e5f6a7b8-c9d0-1234-efgh-i34567890123",
  "identifier": [{
    "system": "http://openelis.org/accession-number",
    "value": "S-2025-001"
  }],
  "status": "available",
  "type": {
    "coding": [{
      "system": "http://snomed.info/sct",
      "code": "119364003",
      "display": "Serum specimen"
    }]
  },
  "container": [{
    "identifier": {
      "value": "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
    },
    "extension": [{
      "url": "http://openelis.org/fhir/extension/storage-position-location",
      "valueReference": {
        "reference": "Location/e5f6a7b8-c9d0-1234-efgh-i34567890124",
        "display": "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
      }
    }, {
      "url": "http://openelis.org/fhir/extension/storage-assigned-date",
      "valueDateTime": "2025-01-15T14:32:00Z"
    }]
  }]
}
```

---

## 7. IHE mCSD Compliance

**Profile**: http://ihe.net/fhir/StructureDefinition/IHE.mCSD.Location

**Required Elements**:
- `Location.id` - FHIR resource identifier (fhir_uuid)
- `Location.status` - active/inactive status
- `Location.name` - Human-readable name
- `Location.mode` - Always "instance" (physical location)
- `Location.partOf` - Parent location reference (for Device, Shelf, Rack)

**mCSD Queries Supported**:

1. **Get all rooms**:
   ```
   GET /fhir/Location?physicalType=ro
   ```

2. **Get devices in a room**:
   ```
   GET /fhir/Location?partOf=Location/{room_fhir_uuid}
   ```

3. **Get positions in a rack**:
   ```
   GET /fhir/Location?partOf=Location/{rack_fhir_uuid}&_tag=http://openelis.org/fhir/tag/storage-hierarchy|position
   ```

4. **Get full hierarchy for a location**:
   ```
   GET /fhir/Location/{location_id}?_include=Location:partOf&_revinclude=Location:partOf
   ```

5. **Search by hierarchical code**:
   ```
   GET /fhir/Location?identifier=http://openelis.org/storage-location-code|MAIN-FRZ01-SHA-RKR1-A5
   ```

6. **Get available (unoccupied) positions in a rack**:
   ```
   GET /fhir/Location?partOf=Location/{rack_fhir_uuid}&extension=http://openelis.org/fhir/extension/position-occupancy|false
   ```

---

## 8. Sync Strategy

**Service**: `StorageLocationFhirTransform` (implements `FhirTransformService` interface)

**FHIR Server**: https://fhir.openelis.org:8443/fhir/

### Immediate Sync (Room, Device, Shelf, Rack)

**Trigger**: On entity insert/update in OpenELIS database (via JPA lifecycle hooks)

**Process**:
1. Entity created/updated → `@PostPersist` / `@PostUpdate` hook triggered
2. `StorageLocationFhirTransform.transformToFhir(entity)` called
3. FHIR Location resource created/updated
4. `FhirPersistanceService.save(location)` → sync to FHIR server immediately

**Rationale**: Low volume entities (typically <1000 total), immediate sync provides real-time FHIR availability

### Batch Sync (Position)

**Trigger**: On sample assignment to position (not on position creation)

**Process**:
1. Sample assigned to position → `SampleStorageService.assignSample()` called
2. Service queues position for FHIR sync: `FhirBatchService.queuePositionSync(positionId)`
3. Background job runs periodically (every 5 minutes OR 100 positions queued)
4. Batch transforms queued positions → FHIR Location resources
5. Batch POST to FHIR server (Bundle transaction)

**Rationale**: High volume entities (potentially 10,000+ positions), most never occupied. Only sync positions when first used to avoid unnecessary FHIR server load.

**Position Sync Triggers**:
- ✅ First assignment to position → Queue for sync
- ✅ Position occupancy change (occupied ↔ empty) → Update existing FHIR Location
- ❌ Empty position creation → NO sync (wait for assignment)

**Batch Sync Configuration** (suggested):
```properties
# common.properties
org.openelisglobal.storage.fhir.batch.interval=300000  # 5 minutes in ms
org.openelisglobal.storage.fhir.batch.size=100          # Max positions per batch
```

### Specimen Container Sync

**Trigger**: On sample assignment or movement

**Process**:
1. Assignment/movement complete → Update existing Specimen resource
2. Set `Specimen.container.extension[storage-position-location]` to position FHIR UUID
3. Set `Specimen.container.identifier.value` to hierarchical path string
4. `FhirPersistanceService.update(specimen)` → sync to FHIR server

**Note**: Specimen sync is immediate (existing OpenELIS pattern for sample updates)

---

## Summary

| OpenELIS Entity | FHIR Resource | Physical Type | Synced to FHIR Server |
|-----------------|---------------|---------------|----------------------|
| StorageRoom | Location | ro (room) | ✅ Yes |
| StorageDevice | Location | ve (vehicle/equipment) | ✅ Yes |
| StorageShelf | Location | co (container) | ✅ Yes |
| StorageRack | Location | co (container) | ✅ Yes |
| StoragePosition | Location | co (container) | ✅ Yes (with occupancy extension) |
| SampleStorageAssignment | Specimen.container extension | N/A | ✅ Yes (via Specimen update) |

**Extension URLs**:
- `http://openelis.org/fhir/extension/storage-temperature` - Device temperature setting
- `http://openelis.org/fhir/extension/storage-capacity` - Location capacity limit
- `http://openelis.org/fhir/extension/rack-grid-dimensions` - Rack grid dimensions
- `http://openelis.org/fhir/extension/rack-position-schema-hint` - Position naming hint
- `http://openelis.org/fhir/extension/position-occupancy` - Position occupancy status (boolean)
- `http://openelis.org/fhir/extension/position-grid-row` - Position row index (integer)
- `http://openelis.org/fhir/extension/position-grid-column` - Position column index (integer)
- `http://openelis.org/fhir/extension/storage-position-location` - Specimen position Location reference
- `http://openelis.org/fhir/extension/storage-assigned-by` - User who assigned sample
- `http://openelis.org/fhir/extension/storage-assigned-date` - Assignment timestamp

