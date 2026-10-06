# Quickstart: Storage Location Management & Configuration

**Feature**: OGC-68-storage-location-crud  
**Jira**: [OGC-68](https://uwdigi.atlassian.net/browse/OGC-68)  
**Branch**: `OGC-68-storage-location-crud`

## Prerequisites

- OpenELIS Global 2 development environment running
- Java 21 installed (`java -version` shows 21.x.x)
- Node.js 16+ installed
- Docker + Docker Compose running

## Quick Reference

### Key Files

| Component      | Location                                                                                 |
| -------------- | ---------------------------------------------------------------------------------------- |
| Spec           | `specs/OGC-68-storage-location-crud/spec.md`                                             |
| Plan           | `specs/OGC-68-storage-location-crud/plan.md`                                             |
| Entity         | `src/main/java/org/openelisglobal/storage/valueholder/StorageDevice.java`                |
| Service        | `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java`       |
| Controller     | `src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java` |
| FHIR Transform | `src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java`        |
| Liquibase      | `src/main/resources/liquibase/3.3.x.x/023-storage-device-connectivity.xml`               |
| Frontend Modal | `frontend/src/components/storage/StorageLocationModal.jsx`                               |
| E2E Tests      | `frontend/cypress/e2e/storageLocationCrud.cy.js`                                         |

### API Endpoints (New)

| Method | Endpoint                     | Description   |
| ------ | ---------------------------- | ------------- |
| POST   | `/rest/storage/rooms`        | Create Room   |
| PUT    | `/rest/storage/rooms/{id}`   | Update Room   |
| DELETE | `/rest/storage/rooms/{id}`   | Delete Room   |
| POST   | `/rest/storage/devices`      | Create Device |
| PUT    | `/rest/storage/devices/{id}` | Update Device |
| DELETE | `/rest/storage/devices/{id}` | Delete Device |
| POST   | `/rest/storage/shelves`      | Create Shelf  |
| PUT    | `/rest/storage/shelves/{id}` | Update Shelf  |
| DELETE | `/rest/storage/shelves/{id}` | Delete Shelf  |
| POST   | `/rest/storage/racks`        | Create Rack   |
| PUT    | `/rest/storage/racks/{id}`   | Update Rack   |
| DELETE | `/rest/storage/racks/{id}`   | Delete Rack   |

### New StorageDevice Fields

| Field                   | Type              | Description                             |
| ----------------------- | ----------------- | --------------------------------------- |
| `ipAddress`             | String (45)       | IPv4/IPv6 address for device monitoring |
| `port`                  | Integer (1-65535) | Network port                            |
| `communicationProtocol` | String (20)       | Protocol (default: 'BACnet')            |

## Development Workflow

### 1. Setup Feature Branch

```bash
# Fetch latest develop
git fetch origin develop
git checkout develop
git pull

# Create milestone branch (backend first)
git checkout -b feat/OGC-68-storage-location-crud/m1-backend
```

### 2. Backend Development (M1)

```bash
# 1. Add Liquibase changeset
# Create: src/main/resources/liquibase/3.3.x.x/023-storage-device-connectivity.xml

# 2. Update StorageDevice entity
# Edit: src/main/java/org/openelisglobal/storage/valueholder/StorageDevice.java
# Add: ipAddress, port, communicationProtocol fields

# 3. Update FHIR transform
# Edit: src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java
# Add: new extension mappings

# 4. Add deletion validation
# Edit: src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java
# Add: canDelete() method for each entity type

# 5. Add DELETE endpoints
# Edit: src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java

# 6. Build and test
mvn clean install -DskipTests -Dmaven.test.skip=true
mvn test -Dtest=StorageLocationServiceTest
```

### 3. Frontend Development (M2)

```bash
# Switch to frontend milestone (can be parallel)
git checkout develop
git checkout -b feat/OGC-68-storage-location-crud/m2-frontend

cd frontend

# 1. Add i18n strings
# Edit: src/languages/en.json, fr.json

# 2. Create modals
# Create: src/components/storage/StorageLocationModal.jsx
# Create: src/components/storage/DeleteLocationModal.jsx

# 3. Update tabs with overflow menu actions
# Edit: src/components/storage/StorageRoomsTab.jsx
# Edit: src/components/storage/StorageDevicesTab.jsx
# Edit: src/components/storage/StorageShelvesTab.jsx
# Edit: src/components/storage/StorageRacksTab.jsx

# 4. Run tests
npm test
npm run cy:run -- --spec "cypress/e2e/storageLocationCrud.cy.js"
```

### 4. Pre-Commit Checklist

```bash
# Format code (REQUIRED before every commit)
mvn spotless:apply
cd frontend && npm run format && cd ..

# Run tests
mvn test
cd frontend && npm test && cd ..

# Verify build
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### 5. Create PR

```bash
# Push branch
git push -u origin feat/OGC-68-storage-location-crud/m1-backend

# Create PR targeting develop
# Title: "feat(OGC-68): Add storage location CRUD backend"
# Reference: specs/OGC-68-storage-location-crud/spec.md
```

## Testing

### Unit Test Example (Deletion Validation)

```java
@Test
public void testCanDeleteRoom_WithChildren_ReturnsBlocked() {
    // Arrange
    StorageRoom room = StorageRoomBuilder.create()
        .withId(1)
        .withName("Main Lab")
        .build();

    when(storageRoomDAO.get(1)).thenReturn(room);
    when(storageDeviceDAO.findByParentRoomId(1))
        .thenReturn(Arrays.asList(new StorageDevice()));

    // Act
    DeletionValidationResult result = storageLocationService.canDeleteRoom(1);

    // Assert
    assertFalse("Should be blocked", result.isAllowed());
    assertEquals("REFERENTIAL_INTEGRITY_VIOLATION", result.getErrorCode());
}
```

### E2E Test Example (Cypress)

```javascript
describe("Storage Location CRUD", () => {
  beforeEach(() => {
    cy.login("admin", "adminADMIN!");
    cy.visit("/storage");
  });

  it("should create a new Room via modal", () => {
    cy.get('[data-testid="rooms-tab"]').click();
    cy.get('[data-testid="add-room-button"]').click();

    cy.get('[data-testid="room-name-input"]').type("Test Room");
    cy.get('[data-testid="room-code-input"]').type("TEST01");
    cy.get('[data-testid="save-button"]').click();

    cy.get('[data-testid="success-notification"]').should("be.visible");
    cy.contains("Test Room").should("be.visible");
  });

  it("should block deletion of Room with children", () => {
    // Arrange: Room with Device exists
    cy.get('[data-testid="rooms-tab"]').click();
    cy.get('[data-testid="row-overflow-menu"]').first().click();
    cy.get('[data-testid="delete-action"]').click();

    // Assert: Error shown
    cy.get('[data-testid="error-notification"]').should(
      "contain",
      "Cannot delete"
    );
  });
});
```

## Common Issues

### Issue: Liquibase changeset not applied

```bash
# Force Liquibase update
docker compose -f dev.docker-compose.yml exec db psql -U clinlims -c \
  "DELETE FROM databasechangelog WHERE id LIKE 'storage-151%';"

# Rebuild and restart
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --force-recreate oe.openelis.org
```

### Issue: FHIR sync failing

Check `StorageLocationFhirTransform.java` for null handling on new fields:

```java
if (device.getIpAddress() != null) {
    location.addExtension(new Extension(EXT_DEVICE_IP_ADDRESS)
        .setValue(new StringType(device.getIpAddress())));
}
```

## Success Criteria

- [ ] All 4 entity types (Room, Device, Shelf, Rack) have working CRUD
- [ ] Device form shows IP/Port/Protocol fields
- [ ] Deletion blocked when children or assignments exist
- [ ] FHIR extensions synced for connectivity fields
- [ ] All tests passing (>80% backend, >70% frontend)
