# FHIR R4 Location Resource Mappings

**Date**: 2025-10-30  
**Feature**: Sample Storage Management POC  
**Branch**: 001-sample-storage  
**FHIR Version**: R4 (4.0.1)  
**Profile**: IHE mCSD (Mobile Care Services Discovery)

## Overview

This document specifies how OpenELIS sample storage entities map to FHIR R4
Location resources for external interoperability. The mapping follows IHE mCSD
profile requirements for hierarchical location structures.

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

| OpenELIS Field | FHIR R4 Location Field         | Notes                                   |
| -------------- | ------------------------------ | --------------------------------------- |
| `fhir_uuid`    | `Location.id`                  | Primary FHIR resource identifier        |
| `code`         | `Location.identifier[0].value` | Unique room code                        |
| `name`         | `Location.name`                | Human-readable room name                |
| `description`  | `Location.description`         | Optional room description               |
| `active`       | `Location.status`              | `true` → "active", `false` → "inactive" |
| -              | `Location.mode`                | Always "instance" (physical location)   |
| -              | `Location.physicalType`        | Always "ro" (room)                      |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identifier": [
    {
      "system": "http://openelis.org/storage-location-code",
      "value": "MAIN"
    }
  ],
  "status": "active",
  "name": "Main Laboratory",
  "description": "Primary laboratory storage facility",
  "mode": "instance",
  "physicalType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
        "code": "ro",
        "display": "Room"
      }
    ]
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

| OpenELIS Field          | FHIR R4 Location Field                    | Notes                                   |
| ----------------------- | ----------------------------------------- | --------------------------------------- |
| `fhir_uuid`             | `Location.id`                             | Primary FHIR resource identifier        |
| `room.code + code`      | `Location.identifier[0].value`            | Hierarchical code (e.g., "MAIN-FRZ01")  |
| `name`                  | `Location.name`                           | Device name                             |
| `type`                  | `Location.type[0].coding.code`            | freezer/refrigerator/cabinet/other      |
| `active`                | `Location.status`                         | `true` → "active", `false` → "inactive" |
| `parent_room.fhir_uuid` | `Location.partOf.reference`               | Reference to parent Room Location       |
| `temperature_setting`   | `Location.extension[storage-temperature]` | Optional temperature in Celsius         |
| `capacity_limit`        | `Location.extension[storage-capacity]`    | Optional capacity limit                 |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "identifier": [
    {
      "system": "http://openelis.org/storage-location-code",
      "value": "MAIN-FRZ01"
    }
  ],
  "status": "active",
  "name": "Freezer Unit 1",
  "mode": "instance",
  "type": [
    {
      "coding": [
        {
          "system": "http://openelis.org/fhir/CodeSystem/storage-device-type",
          "code": "freezer",
          "display": "Freezer"
        }
      ]
    }
  ],
  "physicalType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
        "code": "ve",
        "display": "Vehicle"
      }
    ],
    "text": "Storage Equipment"
  },
  "partOf": {
    "reference": "Location/a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "display": "Main Laboratory"
  },
  "extension": [
    {
      "url": "http://openelis.org/fhir/extension/storage-temperature",
      "valueDecimal": -80.0
    },
    {
      "url": "http://openelis.org/fhir/extension/storage-capacity",
      "valueInteger": 500
    }
  ]
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

| OpenELIS Field                    | FHIR R4 Location Field                 | Notes                                      |
| --------------------------------- | -------------------------------------- | ------------------------------------------ |
| `fhir_uuid`                       | `Location.id`                          | Primary FHIR resource identifier           |
| `room.code + device.code + label` | `Location.identifier[0].value`         | Hierarchical code (e.g., "MAIN-FRZ01-SHA") |
| `label`                           | `Location.name`                        | Shelf label                                |
| `active`                          | `Location.status`                      | `true` → "active", `false` → "inactive"    |
| `parent_device.fhir_uuid`         | `Location.partOf.reference`            | Reference to parent Device Location        |
| `capacity_limit`                  | `Location.extension[storage-capacity]` | Optional capacity limit                    |

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

| OpenELIS Field                                  | FHIR R4 Location Field                          | Notes                                                |
| ----------------------------------------------- | ----------------------------------------------- | ---------------------------------------------------- |
| `fhir_uuid`                                     | `Location.id`                                   | Primary FHIR resource identifier                     |
| `room.code + device.code + shelf.label + label` | `Location.identifier[0].value`                  | Full hierarchical code (e.g., "MAIN-FRZ01-SHA-RKR1") |
| `label`                                         | `Location.name`                                 | Rack label                                           |
| `active`                                        | `Location.status`                               | `true` → "active", `false` → "inactive"              |
| `parent_shelf.fhir_uuid`                        | `Location.partOf.reference`                     | Reference to parent Shelf Location                   |
| `rows * columns`                                | `Location.extension[storage-capacity]`          | Calculated capacity                                  |
| `rows` and `columns`                            | `Location.extension[rack-grid-dimensions]`      | Grid dimensions as string (e.g., "9 × 9")            |
| `position_schema_hint`                          | `Location.extension[rack-position-schema-hint]` | Optional hint for position naming                    |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "d4e5f6a7-b8c9-0123-defg-h23456789012",
  "identifier": [
    {
      "system": "http://openelis.org/storage-location-code",
      "value": "MAIN-FRZ01-SHA-RKR1"
    }
  ],
  "status": "active",
  "name": "Rack R1",
  "mode": "instance",
  "physicalType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
        "code": "co",
        "display": "Container"
      }
    ],
    "text": "Storage Rack"
  },
  "partOf": {
    "reference": "Location/c3d4e5f6-a7b8-9012-cdef-g12345678901",
    "display": "Shelf-A"
  },
  "extension": [
    {
      "url": "http://openelis.org/fhir/extension/rack-grid-dimensions",
      "valueString": "9 × 9"
    },
    {
      "url": "http://openelis.org/fhir/extension/rack-position-schema-hint",
      "valueString": "A1"
    },
    {
      "url": "http://openelis.org/fhir/extension/storage-capacity",
      "valueInteger": 81
    }
  ]
}
```

---

## 5. StoragePosition → FHIR Location

**Physical Type**: `co` (container)

**Rationale**: Positions mapped to FHIR Location resources for complete storage
hierarchy in FHIR server. Enables full interoperability and external query
capabilities.

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

| OpenELIS Field          | FHIR R4 Location Field                     | Notes                                                                   |
| ----------------------- | ------------------------------------------ | ----------------------------------------------------------------------- |
| `fhir_uuid`             | `Location.id`                              | Primary FHIR resource identifier                                        |
| Full hierarchical code  | `Location.identifier[0].value`             | Complete path code (e.g., "MAIN-FRZ01-SHA-RKR1-A5")                     |
| `coordinate`            | `Location.name`                            | Position coordinate (free-text)                                         |
| `active`                | `Location.status`                          | Always "active" (positions don't have active/inactive state separately) |
| `parent_rack.fhir_uuid` | `Location.partOf.reference`                | Reference to parent Rack Location                                       |
| `occupied`              | `Location.extension[position-occupancy]`   | Boolean occupancy status                                                |
| `row_index`             | `Location.extension[position-grid-row]`    | Optional row number for grid visualization                              |
| `column_index`          | `Location.extension[position-grid-column]` | Optional column number for grid visualization                           |

**Example**:

```json
{
  "resourceType": "Location",
  "id": "e5f6a7b8-c9d0-1234-efgh-i34567890124",
  "identifier": [
    {
      "system": "http://openelis.org/storage-location-code",
      "value": "MAIN-FRZ01-SHA-RKR1-A5"
    }
  ],
  "status": "active",
  "name": "A5",
  "mode": "instance",
  "physicalType": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
        "code": "co",
        "display": "Container"
      }
    ],
    "text": "Storage Position"
  },
  "partOf": {
    "reference": "Location/d4e5f6a7-b8c9-0123-defg-h23456789012",
    "display": "Rack R1"
  },
  "extension": [
    {
      "url": "http://openelis.org/fhir/extension/position-occupancy",
      "valueBoolean": true
    },
    {
      "url": "http://openelis.org/fhir/extension/position-grid-row",
      "valueInteger": 1
    },
    {
      "url": "http://openelis.org/fhir/extension/position-grid-column",
      "valueInteger": 5
    }
  ]
}
```

---

## 6. SampleItem-to-Location Link via Specimen Resource

**OpenELIS Entity**: `SampleStorageAssignment` (references `SampleItem`, not
`Sample`)  
**FHIR Resource**: `Specimen` (represents physical specimen, aligns with
SampleItem)

**⚠️ CRITICAL**: Storage tracking operates at the **SampleItem level** (physical
specimens), not Sample level (orders). Each SampleItem maps to a FHIR Specimen
resource, and the storage location is recorded in `Specimen.container`.

**Note**: In OpenELIS, a Sample (order) may have multiple SampleItems (physical
specimens). Each SampleItem can be stored independently, even if they belong to
the same Sample. The FHIR Specimen resource represents the physical specimen
(SampleItem), and the parent Sample accession number is included in the Specimen
identifier for traceability.

```json
{
  "resourceType": "Specimen",
  "id": "{sample_item_fhir_uuid}",
  "identifier": [
    {
      "system": "http://openelis.org/sample-item-id",
      "value": "{sample_item_id}"
    },
    {
      "system": "http://openelis.org/sample-item-external-id",
      "value": "{sample_item_external_id}"
    },
    {
      "system": "http://openelis.org/accession-number",
      "value": "{parent_sample_accession_number}",
      "display": "Parent Sample Accession"
    }
  ],
  "status": "available",
  "type": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "{type_of_sample_code}",
        "display": "{type_of_sample_name}"
      }
    ]
  },
  "container": [
    {
      "identifier": {
        "value": "{hierarchical_location_path}"
      },
      "extension": [
        {
          "url": "http://openelis.org/fhir/extension/storage-position-location",
          "valueReference": {
            "reference": "Location/{location_fhir_uuid}",
            "display": "{hierarchical_path_to_location}"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-location-type",
          "valueCode": "{location_type}",
          "display": "device" | "shelf" | "rack"
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-position-coordinate",
          "valueString": "{position_coordinate}"
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-assigned-by",
          "valueReference": {
            "reference": "Practitioner/{user_fhir_uuid}",
            "display": "{user_name}"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-assigned-date",
          "valueDateTime": "{assigned_date}"
        }
      ]
    }
  ]
}
```

**Mapping Table**:

| OpenELIS Field                   | FHIR Specimen Field                                         | Notes                                                                                    |
| -------------------------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `sample_item.fhir_uuid`          | `Specimen.id`                                               | SampleItem FHIR identifier (physical specimen)                                           |
| `sample_item.id`                 | `Specimen.identifier[0].value`                              | SampleItem ID (system: `http://openelis.org/sample-item-id`)                             |
| `sample_item.external_id`        | `Specimen.identifier[1].value`                              | SampleItem External ID (system: `http://openelis.org/sample-item-external-id`, optional) |
| `sample.accession_number`        | `Specimen.identifier[2].value`                              | Parent Sample accession number (system: `http://openelis.org/accession-number`)          |
| `sample_item.type_of_sample`     | `Specimen.type`                                             | Type of sample (e.g., Blood, Serum)                                                      |
| Full hierarchical path           | `Specimen.container.identifier.value`                       | "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"                     |
| `location.fhir_uuid`             | `Specimen.container.extension[storage-position-location]`   | Reference to Location resource (device, shelf, or rack)                                  |
| `assignment.location_type`       | `Specimen.container.extension[storage-location-type]`       | Location type: "device", "shelf", or "rack"                                              |
| `assignment.position_coordinate` | `Specimen.container.extension[storage-position-coordinate]` | Optional text-based position coordinate                                                  |
| `assigned_by_user.fhir_uuid`     | `Specimen.container.extension[storage-assigned-by]`         | User who assigned                                                                        |
| `assigned_date`                  | `Specimen.container.extension[storage-assigned-date]`       | Assignment timestamp                                                                     |

**Example**:

```json
{
  "resourceType": "Specimen",
  "id": "e5f6a7b8-c9d0-1234-efgh-i34567890123",
  "identifier": [
    {
      "system": "http://openelis.org/sample-item-id",
      "value": "10001"
    },
    {
      "system": "http://openelis.org/sample-item-external-id",
      "value": "SI-2025-001-TUBE-1"
    },
    {
      "system": "http://openelis.org/accession-number",
      "value": "S-2025-001",
      "display": "Parent Sample Accession"
    }
  ],
  "status": "available",
  "type": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "119364003",
        "display": "Serum specimen"
      }
    ]
  },
  "container": [
    {
      "identifier": {
        "value": "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
      },
      "extension": [
        {
          "url": "http://openelis.org/fhir/extension/storage-position-location",
          "valueReference": {
            "reference": "Location/e5f6a7b8-c9d0-1234-efgh-i34567890124",
            "display": "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-location-type",
          "valueCode": "rack",
          "display": "rack"
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-position-coordinate",
          "valueString": "A5"
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-assigned-by",
          "valueReference": {
            "reference": "Practitioner/user-123",
            "display": "John Doe"
          }
        },
        {
          "url": "http://openelis.org/fhir/extension/storage-assigned-date",
          "valueDateTime": "2025-01-15T14:32:00Z"
        }
      ]
    }
  ]
}
```

**Key Points**:

1. **SampleItem-Level Tracking**: Each `SampleStorageAssignment` references a
   `SampleItem` (physical specimen), not a `Sample` (order). This allows
   multiple SampleItems from the same Sample to be stored in different
   locations.

2. **Specimen Resource**: The FHIR Specimen resource represents the physical
   specimen (SampleItem). The `Specimen.id` is the SampleItem's `fhir_uuid`.

3. **Parent Sample Context**: The parent Sample accession number is included in
   `Specimen.identifier` for traceability, but storage location is tracked per
   SampleItem.

4. **Container Extension**: The `Specimen.container` extension includes:
   - Location reference (device, shelf, or rack FHIR UUID)
   - Location type ("device", "shelf", or "rack")
   - Optional position coordinate (text-based)
   - Assignment metadata (user, date)

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

**Service**: `StorageLocationFhirTransform` (implements `FhirTransformService`
interface)

**FHIR Server**: https://fhir.openelis.org:8443/fhir/

### Immediate Sync (All Entities)

**Trigger**: On entity insert/update in OpenELIS database (via JPA lifecycle
hooks)

**Process**:

1. Entity created/updated → `@PostPersist` / `@PostUpdate` hook triggered
2. `StorageLocationFhirTransform.transformToFhirLocation(entity)` called
3. FHIR Location resource created/updated
4. `FhirPersistanceService.save(location)` → sync to FHIR server immediately

**Applies To**:

- ✅ Room, Device, Shelf, Rack - Immediate sync on create/update
- ✅ Position - Immediate sync on create, sync on occupancy change (occupied ↔
  empty)

**Rationale**: Uses existing OpenELIS FHIR sync pattern (same as Patient,
Specimen, etc.). Simpler implementation, real-time FHIR availability, no batch
queue infrastructure needed.

### Specimen Container Sync

**Trigger**: On SampleItem assignment or movement (SampleItem-level operations)

**Process**:

1. SampleItem assignment/movement complete → Update or create Specimen resource
   for SampleItem
2. Set `Specimen.id` to SampleItem's `fhir_uuid` (create if doesn't exist)
3. Set `Specimen.identifier` to include:
   - SampleItem ID (system: `http://openelis.org/sample-item-id`)
   - SampleItem External ID (if available, system:
     `http://openelis.org/sample-item-external-id`)
   - Parent Sample accession number (system:
     `http://openelis.org/accession-number`)
4. Set `Specimen.container.extension[storage-position-location]` to location
   FHIR UUID (device, shelf, or rack)
5. Set `Specimen.container.extension[storage-location-type]` to location type
   ("device", "shelf", or "rack")
6. Set `Specimen.container.extension[storage-position-coordinate]` to position
   coordinate (if provided)
7. Set `Specimen.container.identifier.value` to hierarchical path string
8. Set `Specimen.container.extension[storage-assigned-by]` and
   `[storage-assigned-date]` to assignment metadata
9. `FhirPersistanceService.createOrUpdate(specimen)` → sync to FHIR server

**Note**: Specimen sync is immediate (existing OpenELIS pattern). Each
SampleItem has its own Specimen resource, allowing independent storage tracking
even when multiple SampleItems belong to the same Sample.

---

## Summary

| OpenELIS Entity                            | FHIR Resource                | Physical Type          | Synced to FHIR Server                              |
| ------------------------------------------ | ---------------------------- | ---------------------- | -------------------------------------------------- |
| StorageRoom                                | Location                     | ro (room)              | ✅ Yes                                             |
| StorageDevice                              | Location                     | ve (vehicle/equipment) | ✅ Yes                                             |
| StorageShelf                               | Location                     | co (container)         | ✅ Yes                                             |
| StorageRack                                | Location                     | co (container)         | ✅ Yes                                             |
| StoragePosition                            | Location                     | co (container)         | ✅ Yes (with occupancy extension)                  |
| SampleStorageAssignment (SampleItem-level) | Specimen.container extension | N/A                    | ✅ Yes (via Specimen create/update per SampleItem) |

**Extension URLs**:

- `http://openelis.org/fhir/extension/storage-temperature` - Device temperature
  setting
- `http://openelis.org/fhir/extension/storage-capacity` - Location capacity
  limit
- `http://openelis.org/fhir/extension/rack-grid-dimensions` - Rack grid
  dimensions
- `http://openelis.org/fhir/extension/rack-position-schema-hint` - Position
  naming hint
- `http://openelis.org/fhir/extension/position-occupancy` - Position occupancy
  status (boolean)
- `http://openelis.org/fhir/extension/position-grid-row` - Position row index
  (integer)
- `http://openelis.org/fhir/extension/position-grid-column` - Position column
  index (integer)
- `http://openelis.org/fhir/extension/storage-position-location` - Specimen
  storage Location reference (device, shelf, or rack)
- `http://openelis.org/fhir/extension/storage-location-type` - Location type
  ("device", "shelf", or "rack")
- `http://openelis.org/fhir/extension/storage-position-coordinate` - Optional
  text-based position coordinate
- `http://openelis.org/fhir/extension/storage-assigned-by` - User who assigned
  SampleItem to storage location
- `http://openelis.org/fhir/extension/storage-assigned-date` - Assignment
  timestamp
