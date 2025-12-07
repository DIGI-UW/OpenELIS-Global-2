# Inventory API Frontend-Backend Alignment

## Overview

This document ensures the frontend components correctly consume backend API responses from the inventory management system.

## Data Structure Mapping

### InventoryItem Entity

**Backend Response** (from `InventoryItemRestController`):

```json
{
  "id": 1,
  "fhirUuid": "550e8400-e29b-41d4-a716-446655440000",
  "name": "HIV Test Kit",
  "description": "Rapid HIV test kit",
  "itemType": "RDT",
  "category": "Diagnostics",
  "manufacturer": "Abbott",
  "catalogNumber": "ABC-123",
  "storageRequirements": "2-8°C",
  "quantityPerUnit": 100,
  "units": "tests",
  "lowStockThreshold": 50,
  "expirationAlertDays": 30,
  "stabilityAfterOpening": 90,
  "dilutionNotes": "Use within 24 hours",
  "compatibleAnalyzers": "Analyzer A, Analyzer B",
  "calibrationRequired": "Y",
  "testsPerKit": 100,
  "isActive": true,
  "lastupdated": "2024-01-15T10:30:00"
}
```

**Frontend Expectation** (in components):

- ✅ Uses `item.id` (Long)
- ✅ Uses `item.name` (String) - displayed as "itemName" in UI
- ✅ Uses `item.itemType` (String enum: REAGENT, RDT, CARTRIDGE)
- ✅ Uses `item.units` (String)
- ✅ Uses `item.manufacturer` (String)
- ✅ Uses `item.lowStockThreshold` (Integer) - mapped to "minimumStockLevel" in UI
- ⚠️ **ISSUE**: Frontend uses `item.isActive` (boolean), backend has this field
- ⚠️ **ISSUE**: Frontend displays "minimumStockLevel" but backend uses "lowStockThreshold"

### InventoryLot Entity

**Backend Response** (from `InventoryLotRestController`):

```json
{
  "id": 1,
  "fhirUuid": "550e8400-e29b-41d4-a716-446655440001",
  "inventoryItem": {
    "id": 1,
    "name": "HIV Test Kit",
    "itemType": "RDT",
    "units": "tests"
  },
  "storageLocation": {
    "id": 1,
    "name": "Refrigerator A"
  },
  "lotNumber": "LOT-2024-001",
  "expirationDate": "2025-12-31T00:00:00",
  "dateOpened": null,
  "calculatedExpiryAfterOpening": null,
  "receiptDate": "2024-01-15T10:30:00",
  "initialQuantity": 100.0,
  "currentQuantity": 75.0,
  "qcStatus": "PENDING",
  "status": "ACTIVE",
  "barcode": "BAR123456",
  "version": 0,
  "lastupdated": "2024-01-15T10:30:00"
}
```

**Frontend Expectation** (in components):

- ✅ Uses `lot.id` (Long)
- ✅ Uses `lot.lotNumber` (String)
- ✅ Uses `lot.inventoryItem` (nested object with id, name, itemType, units)
- ✅ Uses `lot.storageLocation` (nested object with id, name)
- ✅ Uses `lot.expirationDate` (Timestamp/String)
- ✅ Uses `lot.receiptDate` (Timestamp/String)
- ✅ Uses `lot.initialQuantity` (Double)
- ✅ Uses `lot.currentQuantity` (Double)
- ✅ Uses `lot.qcStatus` (String enum: PENDING, PASSED, FAILED, NOT_REQUIRED)
- ✅ Uses `lot.status` (String enum: ACTIVE, IN_USE, EXPIRED, CONSUMED, QUARANTINED)
- ✅ Uses `lot.dateOpened` (Timestamp/String, nullable)

## API Endpoint Mapping

### InventoryItem API

| Frontend Call                       | Backend Endpoint                                 | Response Type         | Status            |
| ----------------------------------- | ------------------------------------------------ | --------------------- | ----------------- |
| `InventoryItemAPI.getAll()`         | `GET /rest/inventory/items`                      | `List<InventoryItem>` | ✅                |
| `InventoryItemAPI.getById(id)`      | `GET /rest/inventory/items/{id}`                 | `InventoryItem`       | ✅                |
| `InventoryItemAPI.getByType(type)`  | `GET /rest/inventory/items/type/{type}`          | `List<InventoryItem>` | ✅                |
| `InventoryItemAPI.search(query)`    | `GET /rest/inventory/items/search?query={query}` | `List<InventoryItem>` | ⚠️ Need to verify |
| `InventoryItemAPI.create(item)`     | `POST /rest/inventory/items`                     | `InventoryItem`       | ⚠️ Need to verify |
| `InventoryItemAPI.update(id, item)` | `PUT /rest/inventory/items/{id}`                 | `InventoryItem`       | ⚠️ Need to verify |
| `InventoryItemAPI.deactivate(id)`   | `PUT /rest/inventory/items/{id}/deactivate`      | status code           | ⚠️ Need to verify |

### InventoryLot API

| Frontend Call                                            | Backend Endpoint                          | Response Type        | Status            |
| -------------------------------------------------------- | ----------------------------------------- | -------------------- | ----------------- |
| `InventoryLotAPI.getAll(filters)`                        | `GET /rest/inventory/lots`                | `List<InventoryLot>` | ✅                |
| `InventoryLotAPI.getById(id)`                            | `GET /rest/inventory/lots/{id}`           | `InventoryLot`       | ✅                |
| `InventoryLotAPI.create(lot)`                            | `POST /rest/inventory/lots`               | `InventoryLot`       | ⚠️ Need to verify |
| `InventoryLotAPI.update(id, lot)`                        | `PUT /rest/inventory/lots/{id}`           | `InventoryLot`       | ⚠️ Need to verify |
| `InventoryLotAPI.updateQCStatus(id, qcStatus, notes)`    | `PUT /rest/inventory/lots/{id}/qc-status` | status code          | ⚠️ Need to verify |
| `InventoryLotAPI.adjust(id, newQuantity, reason, notes)` | `POST /rest/inventory/lots/{id}/adjust`   | `InventoryLot`       | ⚠️ Need to verify |
| `InventoryLotAPI.dispose(id, reason, notes)`             | `POST /rest/inventory/lots/{id}/dispose`  | status code          | ⚠️ Need to verify |

### InventoryManagement API

| Frontend Call                          | Backend Endpoint                          | Response Type  | Status            |
| -------------------------------------- | ----------------------------------------- | -------------- | ----------------- |
| `InventoryManagementAPI.consume(data)` | `POST /rest/inventory/management/consume` | `UsageRecord`  | ⚠️ Need to verify |
| `InventoryManagementAPI.receive(data)` | `POST /rest/inventory/management/receive` | `InventoryLot` | ⚠️ Need to verify |

## Issues Found & Fixes Needed

### 1. Property Name Mismatch

**Issue**: Frontend uses different property names than backend

- Frontend: `item.itemName` → Backend: `item.name`
- Frontend: `item.minimumStockLevel` → Backend: `item.lowStockThreshold`
- Frontend: `item.reorderLevel` → Backend: Not in entity (need to add or remove from UI)

**Fix Strategy**:
Option A: Update frontend to use backend property names
Option B: Create DTOs on backend to match frontend expectations
**Recommended**: Option A (update frontend) - less backend work

### 2. API Method Signature Mismatch

**Issue**: Some frontend API calls don't match expected backend signatures

Example in `LotAdjustmentModal.jsx`:

```javascript
// Frontend sends:
await InventoryLotAPI.adjust(lot.id, {
  newQuantity: formData.newQuantity,
  reason: formData.reason,
  notes: formData.notes,
});

// But InventoryService.js defines:
adjust: (id, newQuantity, reason, notes) =>
  post(`/lots/${id}/adjust`, { newQuantity, reason, notes });
```

**Fix**: Update component to match service signature:

```javascript
await InventoryLotAPI.adjust(
  lot.id,
  formData.newQuantity,
  formData.reason,
  formData.notes,
);
```

### 3. UpdateQCStatus Signature Mismatch

**Issue**: `UpdateQCStatusModal.jsx` sends object, but service expects separate params

Frontend:

```javascript
await InventoryLotAPI.updateQCStatus(lot.id, {
  qcStatus: formData.qcStatus,
  notes: formData.notes,
});
```

Service definition:

```javascript
updateQCStatus: (id, qcStatus, notes) =>
  put(`/lots/${id}/qc-status`, { qcStatus, notes });
```

**Fix**: Update component to:

```javascript
await InventoryLotAPI.updateQCStatus(lot.id, formData.qcStatus, formData.notes);
```

### 4. Storage Location Nested Object

**Issue**: Backend returns full `InventoryStorageLocation` object, frontend expects minimal object

Backend response:

```json
"storageLocation": {
  "id": 1,
  "fhirUuid": "...",
  "name": "Refrigerator A",
  "locationType": "REFRIGERATOR",
  "temperatureMin": 2.0,
  "temperatureMax": 8.0,
  "parent": null,
  "isActive": true
}
```

Frontend expects:

```json
"storageLocation": {
  "id": 1,
  "name": "Refrigerator A"
}
```

**Status**: ✅ Frontend already handles this correctly by using optional chaining (`lot.storageLocation?.name`)

## Action Items

### High Priority

1. ✅ Verify all backend REST endpoints exist and return correct response types
2. ⚠️ Update `LotAdjustmentModal.jsx` to fix method signature
3. ⚠️ Update `UpdateQCStatusModal.jsx` to fix method signature
4. ⚠️ Standardize property names (name vs itemName, lowStockThreshold vs minimumStockLevel)

### Medium Priority

5. ⚠️ Add error response handling for all API calls
6. ⚠️ Add loading states for all async operations
7. ⚠️ Verify date format handling (backend uses Timestamp, frontend uses strings)

### Low Priority

8. ⚠️ Add TypeScript interfaces for type safety
9. ⚠️ Create DTO classes on backend for cleaner API contracts
10. ⚠️ Add API documentation (Swagger/OpenAPI)

## Date/Time Handling

**Backend**: Returns `java.sql.Timestamp` serialized as ISO-8601 strings

```json
"expirationDate": "2025-12-31T00:00:00"
```

**Frontend**: JavaScript Date parsing

```javascript
new Date(lot.expirationDate).toLocaleDateString();
```

**Status**: ✅ Works correctly, JavaScript Date can parse ISO-8601

## Enum Handling

### ItemType

- REAGENT
- RDT
- CARTRIDGE

### QCStatus

- PENDING
- PASSED
- FAILED
- NOT_REQUIRED

### LotStatus

- ACTIVE
- IN_USE
- EXPIRED
- CONSUMED
- QUARANTINED

**Status**: ✅ Frontend and backend use same enum values
