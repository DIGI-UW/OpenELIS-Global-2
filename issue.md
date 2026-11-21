### Title

**[Bug] Code uniqueness validation is skipped for storage shelves and racks,
allowing duplicate barcode codes within parent containers**

---

### Affected Component / Area

Backend (Java / Spring)

---

### Description of the Bug

In the Sample Storage Management hierarchy, location codes (`code`) are used to
identify containers and construct hierarchical barcodes
(`<RoomCode>-<DeviceCode>-<ShelfCode>-<RackCode>`) for specimen tracking.
According to design specifications and service calls in
`StorageLocationServiceImpl`, shelf codes must be unique within their parent
device, and rack codes must be unique within their parent shelf.

However, in `org.openelisglobal.storage.service.CodeValidationServiceImpl.java`
(lines 119–152), the uniqueness validation logic for `shelf` and `rack` is
completely skipped with placeholder `// TODO` comments:

```java
case "shelf":
    // Shelf: unique within parent device
    // TODO: Add findByCodeAndParentDevice method to StorageShelfDAO in implementation phase
    if (StringUtils.isBlank(parentId)) {
        return CodeValidationResult.invalid("Parent device ID is required for shelf code validation");
    }
    StorageDevice parentDevice = storageDeviceDAO.get(Integer.parseInt(parentId)).orElse(null);
    if (parentDevice == null) {
        return CodeValidationResult.invalid("Parent device not found");
    }
    // Placeholder: Will be implemented when DAO method is added
    // StorageShelf existingShelf = storageShelfDAO.findByCodeAndParentDevice(normalized, parentDevice);
    // For now, skip uniqueness check for shelf (will be added in implementation)
    break;

case "rack":
    // Rack: unique within parent shelf
    // TODO: Add findByCodeAndParentShelf method to StorageRackDAO in implementation phase
    if (StringUtils.isBlank(parentId)) {
        return CodeValidationResult.invalid("Parent shelf ID is required for rack code validation");
    }
    StorageShelf parentShelf = storageShelfDAO.get(Integer.parseInt(parentId)).orElse(null);
    if (parentShelf == null) {
        return CodeValidationResult.invalid("Parent shelf not found");
    }
    // Placeholder: Will be implemented when DAO method is added
    // StorageRack existingRack = storageRackDAO.findByCodeAndParentShelf(normalized, parentShelf);
    // For now, skip uniqueness check for rack (will be added in implementation)
    break;
```

Furthermore:

1. `StorageShelfDAO` and `StorageShelfDAOImpl` do not implement
   `findByCodeAndParentDevice(String code, StorageDevice parentDevice)`.
2. `StorageRackDAO` and `StorageRackDAOImpl` do not implement
   `findByCodeAndParentShelf(String code, StorageShelf parentShelf)`.
3. Database tables `storage_shelf` and `storage_rack` only enforce unique
   constraints on `(parent_device_id, label)` and `(parent_shelf_id, label)`,
   but NOT on `code` (previous unique constraints on `short_code` were removed
   in Liquibase changeset `013-remove-short-code-columns.xml` when migrating to
   `code`).
4. Unit tests in `CodeValidationServiceTest.java` (lines 187–206) explicitly
   confirm that uniqueness checking is currently disabled with placeholder
   assertions:
   ```java
   // Shelf uniqueness check will be implemented when DAO method is added
   result = codeValidationService.validateUniqueness("SHA", "shelf", "2", "1");
   assertTrue("Code validation should pass (uniqueness check placeholder)", result.isValid());
   ```

Because `StorageLocationServiceImpl` relies entirely on
`codeValidationService.validateUniqueness(...)` (lines 327–331, 349–353,
453–458, 484–488), duplicate codes can be created or updated under the same
parent container without error.

---

### Steps to Reproduce

1. Navigate to `/StorageManagement` (or use the REST API `/rest/storage/shelf`).
2. Select a storage room (e.g., `ROOM1`) and a device within it (e.g., `FRZ01`).
3. Create a new shelf under `FRZ01` with:
   - Label: `Shelf 1`
   - Code: `SH01`
4. Create a second shelf under the **same** device `FRZ01` with:
   - Label: `Shelf 2`
   - Code: `SH01` (identical code)
5. Observe that both shelves are saved successfully without any uniqueness
   validation error.
6. Generate barcode labels for both shelves: both print `ROOM1-FRZ01-SH01`,
   producing duplicate barcode identifiers for physically distinct storage
   locations.

---

### URL

`https://localhost/StorageManagement`, `/rest/storage/shelf`,
`/rest/storage/rack`

---

### Expected Behavior

`CodeValidationServiceImpl.validateUniqueness()` should query the database for
existing shelves/racks sharing the same code under the same parent container:

- If another shelf under the same parent device already has that code (and has a
  different ID), it must return an invalid result:
  `Code '<CODE>' already exists for another shelf in this device`.
- If another rack under the same parent shelf already has that code (and has a
  different ID), it must return an invalid result:
  `Code '<CODE>' already exists for another rack in this shelf`.
- `StorageLocationServiceImpl` should reject creation/update with a validation
  error, preventing barcode collision.

---

### Severity Rating

Level 2: Minor / medium (difficult or confusing, but workaround exists)

---

### Environment Info

- Branch: `develop`
- Java: OpenJDK 21 LTS
- Database: PostgreSQL 14+ / Liquibase
- Component: `org.openelisglobal.storage.service.CodeValidationServiceImpl`

---

### Checklist

- [x] I have verified this bug exists on the latest develop branch.
- [x] I have searched for existing issues to avoid duplicates.

---

### Proposed Solution & Code Pointers

1. **DAO Layer:**

   - In `StorageShelfDAO.java` and `StorageShelfDAOImpl.java`, add:
     ```java
     StorageShelf findByCodeAndParentDevice(String code, StorageDevice parentDevice);
     ```
     Querying:
     ```hql
     FROM StorageShelf s WHERE s.code = :code AND s.parentDevice = :parentDevice
     ```
   - In `StorageRackDAO.java` and `StorageRackDAOImpl.java`, add:
     ```java
     StorageRack findByCodeAndParentShelf(String code, StorageShelf parentShelf);
     ```
     Querying:
     ```hql
     FROM StorageRack r WHERE r.code = :code AND r.parentShelf = :parentShelf
     ```

2. **Service Layer:**

   - In `CodeValidationServiceImpl.java` (lines 119–152), implement the checks
     in cases `"shelf"` and `"rack"` matching the pattern used for `"device"`:

     ```java
     case "shelf":
         if (StringUtils.isBlank(parentId)) {
             return CodeValidationResult.invalid("Parent device ID is required for shelf code validation");
         }
         StorageDevice parentDevice = storageDeviceDAO.get(Integer.parseInt(parentId)).orElse(null);
         if (parentDevice == null) {
             return CodeValidationResult.invalid("Parent device not found");
         }
         StorageShelf existingShelf = storageShelfDAO.findByCodeAndParentDevice(normalized, parentDevice);
         if (existingShelf != null && !String.valueOf(existingShelf.getId()).equals(locationId)) {
             return CodeValidationResult.invalid(
                     String.format("Code '%s' already exists for another shelf in this device", normalized));
         }
         break;

     case "rack":
         if (StringUtils.isBlank(parentId)) {
             return CodeValidationResult.invalid("Parent shelf ID is required for rack code validation");
         }
         StorageShelf parentShelf = storageShelfDAO.get(Integer.parseInt(parentId)).orElse(null);
         if (parentShelf == null) {
             return CodeValidationResult.invalid("Parent shelf not found");
         }
         StorageRack existingRack = storageRackDAO.findByCodeAndParentShelf(normalized, parentShelf);
         if (existingRack != null && !String.valueOf(existingRack.getId()).equals(locationId)) {
             return CodeValidationResult.invalid(
                     String.format("Code '%s' already exists for another rack in this shelf", normalized));
         }
         break;
     ```

3. **Database Schema (Liquibase):**

   - Add a Liquibase changeset to enforce uniqueness at the database level:
     - `uk_shelf_parent_code` on `storage_shelf(parent_device_id, code)`
     - `uk_rack_parent_code` on `storage_rack(parent_shelf_id, code)`

4. **Testing:**
   - Update `CodeValidationServiceTest.java` (lines 187–206) to test positive
     and negative cases for shelf and rack code uniqueness.
