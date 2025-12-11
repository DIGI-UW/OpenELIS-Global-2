# Research: Storage Location Management & Configuration

**Feature**: 151-storage-location-crud  
**Date**: 2025-12-11  
**Status**: Complete

## 1. Existing CRUD Patterns in OpenELIS

### Decision

Follow the existing Sample Storage dashboard patterns from Feature 001.

### Findings

**Backend Pattern** (from `StorageLocationRestController.java`):

- Controllers extend `BaseRestController`
- Use `@RestController` + `@RequestMapping("/rest/storage")`
- Return `ResponseEntity<?>` with JSON body
- Service layer handles business logic and transactions

**Frontend Pattern** (from `StorageRoomsTab.jsx`, `StorageDevicesTab.jsx`):

- Carbon `DataTable` with `OverflowMenu` for row actions
- Modals for Add/Edit operations
- `getFromOpenElisServer`/`postToOpenElisServer` for API calls
- React Intl for all strings

### Alternatives Considered

- Using a generic CRUD controller: Rejected - OpenELIS pattern is
  entity-specific controllers
- GraphQL mutations: Rejected - OpenELIS uses REST

---

## 2. Deletion Validation Strategy

### Decision

Manual validation in service layer before delete (NOT Hibernate cascade).

### Rationale

1. **Better User Feedback**: Can provide specific error messages ("Cannot
   delete: 3 devices exist")
2. **Audit Trail**: Log attempted deletions with reason
3. **Referential Integrity**: Database constraints as backup, but UI prevents
   invalid attempts

### Implementation Pattern

```java
// StorageLocationServiceImpl.java
public DeletionValidationResult canDeleteRoom(String roomId) {
    StorageRoom room = storageRoomDAO.get(roomId);

    // Check for child devices
    List<StorageDevice> devices = storageDeviceDAO.findByParentRoomId(roomId);
    if (!devices.isEmpty()) {
        return DeletionValidationResult.blocked(
            "REFERENTIAL_INTEGRITY_VIOLATION",
            String.format("Cannot delete Room '%s': contains %d device(s)",
                room.getName(), devices.size())
        );
    }

    // Check for direct sample assignments (edge case)
    List<SampleStorageAssignment> assignments =
        sampleStorageAssignmentDAO.findByLocationType("room", roomId);
    if (!assignments.isEmpty()) {
        return DeletionValidationResult.blocked(
            "ACTIVE_ASSIGNMENTS",
            String.format("Cannot delete Room '%s': %d sample(s) assigned",
                room.getName(), assignments.size())
        );
    }

    return DeletionValidationResult.allowed();
}
```

### Alternatives Considered

- Hibernate `CascadeType.REMOVE`: Rejected - too dangerous, could delete samples
- Database `ON DELETE RESTRICT`: Used as backup, but UI validation preferred

---

## 3. IP Address Validation

### Decision

Use standard IPv4/IPv6 regex pattern with optional field.

### IPv4/IPv6 Regex Pattern

```javascript
// Frontend validation (Formik/Yup)
const IP_ADDRESS_PATTERN = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/;

// Backend validation (Hibernate Validator)
@Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "Invalid IP address format")
private String ipAddress;
```

### Field Constraints

- **ip_address**: VARCHAR(45) - Accommodates IPv6 max length (e.g.,
  `2001:0db8:85a3:0000:0000:8a2e:0370:7334`)
- **port**: INTEGER (1-65535) - Standard port range
- **communication_protocol**: VARCHAR(20) - Default 'BACnet'

---

## 4. Communication Protocols

### Decision

Start with BACnet only, design for extensibility.

### Protocol Options

| Protocol  | Use Case                                      | Status      |
| --------- | --------------------------------------------- | ----------- |
| BACnet    | Building automation, HVAC, freezer monitoring | **Default** |
| SNMP      | Network device monitoring                     | Future      |
| Modbus    | Industrial equipment                          | Future      |
| HTTP/REST | Modern IoT devices                            | Future      |

### Implementation

- Store as String (not enum) to allow future additions without schema changes
- UI shows dropdown with "BACnet" as default
- Validate against allowed list in service layer

---

## 5. Existing Storage Entity Structure

### StorageDevice Current Fields (from codebase)

```java
// From StorageDevice.java
- id: Integer (PK, sequence generated)
- fhirUuid: UUID (unique, non-null)
- name: String (255, non-null)
- code: String (10, non-null)
- type: String (20, non-null) - freezer/refrigerator/cabinet/other
- temperatureSetting: BigDecimal (5,2)
- capacityLimit: Integer
- active: Boolean (non-null)
- parentRoom: StorageRoom (FK, non-null)
- sysUserId: Integer (non-null)
```

### New Fields to Add

```java
// Connectivity configuration (OGC-68)
- ipAddress: String (45, nullable)
- port: Integer (nullable, 1-65535)
- communicationProtocol: String (20, nullable, default 'BACnet')
```

---

## 6. FHIR Extension Design

### Decision

Add connectivity extensions to FHIR Location resource for StorageDevice.

### Extension URLs (consistent with existing pattern)

```java
// From StorageLocationFhirTransform.java - existing pattern
private static final String EXT_STORAGE_TEMPERATURE = "http://openelis.org/fhir/extension/storage-temperature";
private static final String EXT_STORAGE_CAPACITY = "http://openelis.org/fhir/extension/storage-capacity";

// New extensions for OGC-68
private static final String EXT_DEVICE_IP_ADDRESS = "http://openelis.org/fhir/extension/device-ip-address";
private static final String EXT_DEVICE_PORT = "http://openelis.org/fhir/extension/device-port";
private static final String EXT_DEVICE_PROTOCOL = "http://openelis.org/fhir/extension/device-communication-protocol";
```

### FHIR Location Example (with connectivity)

```json
{
  "resourceType": "Location",
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "status": "active",
  "name": "Freezer Unit 1",
  "physicalType": {
    "coding": [{ "code": "ve", "display": "Vehicle" }],
    "text": "Storage Equipment"
  },
  "extension": [
    {
      "url": "http://openelis.org/fhir/extension/storage-temperature",
      "valueDecimal": -80.0
    },
    {
      "url": "http://openelis.org/fhir/extension/device-ip-address",
      "valueString": "192.168.1.100"
    },
    {
      "url": "http://openelis.org/fhir/extension/device-port",
      "valueInteger": 47808
    },
    {
      "url": "http://openelis.org/fhir/extension/device-communication-protocol",
      "valueString": "BACnet"
    }
  ]
}
```

---

## 7. UI Component Design

### Decision

Single shared modal with dynamic fields per entity type.

### Modal Architecture

```jsx
// StorageLocationModal.jsx
<StorageLocationModal
  isOpen={true}
  mode="add" | "edit"
  entityType="room" | "device" | "shelf" | "rack"
  initialData={existingLocation}
  onSave={handleSave}
  onClose={handleClose}
/>
```

### Fields by Entity Type

| Field             | Room | Device   | Shelf      | Rack      |
| ----------------- | ---- | -------- | ---------- | --------- |
| Name              | ✓    | ✓        | ✓          | ✓         |
| Code              | ✓    | ✓        | ✓          | ✓         |
| Description       | ✓    | -        | -          | -         |
| Type              | -    | ✓        | -          | -         |
| Temperature       | -    | ✓        | -          | -         |
| Capacity          | -    | ✓        | ✓          | ✓         |
| IP Address        | -    | ✓        | -          | -         |
| Port              | -    | ✓        | -          | -         |
| Protocol          | -    | ✓        | -          | -         |
| Parent (dropdown) | -    | ✓ (Room) | ✓ (Device) | ✓ (Shelf) |
| Status            | ✓    | ✓        | ✓          | ✓         |

---

## Summary

All research questions resolved. Ready for Phase 1 design and implementation.
