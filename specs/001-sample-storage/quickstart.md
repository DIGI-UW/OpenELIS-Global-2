# Quickstart: Sample Storage Management POC

**Date**: 2025-10-30  
**Feature**: Sample Storage Management  
**Branch**: 001-sample-storage  
**POC Scope**: P1 (Assignment), P2A (Search), P2B (Movement)

## Prerequisites

- ✅ OpenELIS Global 3.0 development environment running (see [dev_setup.md](../../../docs/dev_setup.md))
- ✅ PostgreSQL 14+ database accessible
- ✅ Java 21 (OpenJDK), Maven 3.8+
- ✅ Node.js 16+, npm
- ✅ Docker + Docker Compose
- ✅ HAPI FHIR R4 server running at `https://fhir.openelis.org:8443/fhir/`

## Quick Navigation

- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [FHIR Validation](#fhir-validation)
- [Testing User Scenarios](#testing-user-scenarios)
- [Troubleshooting](#troubleshooting)

---

## Backend Setup

### 1. Database Migration

Liquibase changesets automatically run on application startup. Verify migration success:

```bash
# Connect to PostgreSQL
psql -U clinlims -d clinlims

# Check storage tables created
\dt storage_*

# Expected tables:
# storage_room
# storage_device
# storage_shelf
# storage_rack
# storage_position
# sample_storage_assignment
# sample_storage_movement

# Verify sequences created
\ds *storage*

# Exit psql
\q
```

**Rollback** (if needed):

```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=storage-001
```

### 2. Build Backend

From repository root:

```bash
# Clean build (skip tests for faster iteration)
mvn clean install -DskipTests

# Build with unit tests
mvn clean install

# Build only storage module (after initial full build)
mvn clean install -pl :openelisglobal -am -DskipTests
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 2:15 min
```

### 3. Run Backend Tests

```bash
# Run all storage unit tests
mvn test -Dtest="org.openelisglobal.storage.**"

# Run specific test class
mvn test -Dtest="org.openelisglobal.storage.service.SampleStorageServiceImplTest"

# Run integration tests (requires database)
mvn verify -Dtest="org.openelisglobal.storage.controller.**"

# Check test coverage (JaCoCo)
mvn jacoco:report
# Open: target/site/jacoco/index.html
```

**Coverage Goal**: >70% for new storage code

### 4. Start Backend Dev Server

```bash
# From repository root
docker-compose -f dev.docker-compose.yml up -d

# Watch logs
docker logs -f oe.openelis.org

# Verify backend started successfully
# Look for: "Started OpenELISApplication in X seconds"
```

**Access Points**:
- **Backend API**: https://localhost/rest/storage/rooms
- **Legacy UI**: https://localhost/api/OpenELIS-Global/
- **React UI**: https://localhost/

### 5. Hot Reload Backend Changes

After making Java code changes:

```bash
# Rebuild WAR
mvn clean install -DskipTests

# Recreate backend container only
docker-compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# Watch logs for startup confirmation
docker logs -f oe.openelis.org
```

---

## Frontend Setup

### 1. Install Dependencies

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Install Playwright (for E2E tests)
npm install -D @playwright/test
npx playwright install --with-deps chromium
```

### 2. Add Internationalization Keys

Edit translation files to add storage-specific message keys:

**frontend/src/languages/en.json**:
```json
{
  "storage.location.label": "Storage Location",
  "storage.room.label": "Room",
  "storage.device.label": "Device",
  "storage.shelf.label": "Shelf",
  "storage.rack.label": "Rack",
  "storage.position.label": "Position",
  "storage.hierarchical.path": "Location Path",
  "storage.assign.button": "Assign Location",
  "storage.move.button": "Move Sample",
  "storage.barcode.scan.placeholder": "Scan barcode or enter manually",
  "storage.error.position.occupied": "Position is already occupied",
  "storage.error.location.inactive": "Cannot assign to inactive location",
  "storage.success.assigned": "Sample assigned to storage location successfully",
  "storage.success.moved": "Sample moved to new location successfully"
}
```

**frontend/src/languages/fr.json** (French):
```json
{
  "storage.location.label": "Emplacement de stockage",
  "storage.room.label": "Salle",
  "storage.device.label": "Dispositif",
  "storage.shelf.label": "Étagère",
  "storage.rack.label": "Rack",
  "storage.position.label": "Position",
  "storage.hierarchical.path": "Chemin de l'emplacement",
  "storage.assign.button": "Attribuer l'emplacement",
  "storage.move.button": "Déplacer l'échantillon",
  "storage.barcode.scan.placeholder": "Scanner le code-barres ou saisir manuellement",
  "storage.error.position.occupied": "La position est déjà occupée",
  "storage.error.location.inactive": "Impossible d'attribuer à un emplacement inactif",
  "storage.success.assigned": "Échantillon attribué à l'emplacement de stockage avec succès",
  "storage.success.moved": "Échantillon déplacé vers un nouvel emplacement avec succès"
}
```

**frontend/src/languages/sw.json** (Swahili):
```json
{
  "storage.location.label": "Mahali pa Uhifadhi",
  "storage.room.label": "Chumba",
  "storage.device.label": "Kifaa",
  "storage.shelf.label": "Rafu",
  "storage.rack.label": "Rafu ya Sampuli",
  "storage.position.label": "Nafasi",
  "storage.hierarchical.path": "Njia ya Mahali",
  "storage.assign.button": "Weka Mahali",
  "storage.move.button": "Hamisha Sampuli",
  "storage.barcode.scan.placeholder": "Changanua barcode au weka mwenyewe",
  "storage.error.position.occupied": "Nafasi tayari imeshughulikiwa",
  "storage.error.location.inactive": "Huwezi kuweka mahali palipo zima",
  "storage.success.assigned": "Sampuli imewekwa mahali pa uhifadhi kwa mafanikio",
  "storage.success.moved": "Sampuli imehamishwa mahali mapya kwa mafanikio"
}
```

### 3. Run Frontend Dev Server

```bash
# From frontend/ directory
npm start

# Frontend starts with hot reload at https://localhost/
# Changes to .jsx files auto-reload in browser
```

**Expected Output**:
```
webpack compiled successfully
```

### 4. Run Frontend Tests

```bash
# Run all tests
npm test

# Run storage component tests only
npm test -- components/storage

# Run tests in watch mode
npm test -- --watch

# Run E2E tests (Playwright)
npx playwright test tests/e2e/storage

# Run E2E tests with UI mode
npx playwright test --ui

# Generate E2E test report
npx playwright show-report
```

### 5. Lint and Format

```bash
# Run ESLint
npm run lint

# Fix linting issues automatically
npm run lint:fix

# Format code with Prettier
npm run format

# Check formatting without changes
npm run format:check
```

---

## FHIR Validation

### 1. Access FHIR Server

FHIR server runs in Docker container alongside OpenELIS:

```bash
# Verify FHIR server running
curl -k https://fhir.openelis.org:8443/fhir/metadata

# Expected: CapabilityStatement resource (FHIR server capabilities)
```

### 2. Query Storage Locations

```bash
# Get all rooms
curl -k https://fhir.openelis.org:8443/fhir/Location?physicalType=ro

# Get devices in a specific room (replace {room_fhir_uuid})
curl -k https://fhir.openelis.org:8443/fhir/Location?partOf=Location/{room_fhir_uuid}

# Get location by hierarchical code
curl -k https://fhir.openelis.org:8443/fhir/Location?identifier=http://openelis.org/storage-location-code|MAIN-FRZ01

# Get full hierarchy for a location (includes parent references)
curl -k https://fhir.openelis.org:8443/fhir/Location/{location_id}?_include=Location:partOf
```

### 3. Create Test Location via FHIR

Create `test-room.json`:

```json
{
  "resourceType": "Location",
  "identifier": [{
    "system": "http://openelis.org/storage-location-code",
    "value": "TEST-ROOM"
  }],
  "status": "active",
  "name": "Test Room",
  "mode": "instance",
  "physicalType": {
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/location-physical-type",
      "code": "ro",
      "display": "Room"
    }]
  }
}
```

```bash
# POST to FHIR server
curl -k -X POST https://fhir.openelis.org:8443/fhir/Location \
  -H "Content-Type: application/fhir+json" \
  -d @test-room.json

# Expected: 201 Created with Location resource ID
```

### 4. Validate FHIR Resources

```bash
# Validate Location resource against FHIR R4 spec
curl -k -X POST https://fhir.openelis.org:8443/fhir/Location/\$validate \
  -H "Content-Type: application/fhir+json" \
  -d @test-room.json

# Expected: OperationOutcome with validation results
```

---

## Testing User Scenarios

### P1: Basic Storage Assignment

**Workflow**: Assign sample to storage location via cascading dropdowns

1. **Setup**: Create storage hierarchy in database or via API
   ```bash
   # Using curl to create locations
   curl -k -X POST https://localhost/rest/storage/rooms \
     -H "Content-Type: application/json" \
     -d '{"name":"Main Laboratory","code":"MAIN","active":true}'
   
   curl -k -X POST https://localhost/rest/storage/devices \
     -H "Content-Type: application/json" \
     -d '{"name":"Freezer Unit 1","code":"FRZ01","type":"freezer","parentRoomId":"{room_id}","active":true}'
   
   # Continue for shelf, rack, position...
   ```

2. **Navigate**: https://localhost/sample-entry

3. **Complete Sample Entry**:
   - Enter accession number (e.g., "S-2025-001")
   - Fill patient information
   - Select tests

4. **Assign Storage Location**:
   - Scroll to Storage Location Selector widget
   - Mode: Cascading Dropdowns
   - Select: Room → Device → Shelf → Rack
   - Enter Position: "A5"
   - Verify hierarchical path displays: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"

5. **Save**: Click "Save" button

6. **Verify**:
   ```bash
   # Query sample location
   curl -k https://localhost/rest/storage/samples/search?sampleId={sample_id}
   
   # Expected: JSON with full location hierarchy
   ```

**Expected Result**: Sample assigned to storage location, hierarchical path displayed, assignment timestamp recorded.

---

### P2A: Sample Search and Retrieval

**Workflow**: Search for sample and retrieve storage location

1. **Navigate**: https://localhost/logbook

2. **Search Sample**:
   - Enter sample ID: "S-2025-001"
   - Click Search

3. **Expand Results**:
   - Click on sample row to expand details
   - Scroll to Storage Location section

4. **Verify Location Displayed**:
   - Hierarchical path: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
   - Assigned by: User name
   - Assigned date: Timestamp

**Alternative - API Search**:

```bash
# Search by sample ID
curl -k https://localhost/rest/storage/samples/search?sampleId=S-2025-001

# Filter by location
curl -k "https://localhost/rest/storage/samples?roomId={room_id}&deviceId={device_id}"

# Filter by date range
curl -k "https://localhost/rest/storage/samples?fromDate=2025-01-01&toDate=2025-01-31"
```

**Expected Result**: Sample location retrieved in <2 seconds, hierarchical path displayed correctly.

---

### P2B: Sample Movement

**Workflow**: Move sample from Freezer to Refrigerator

1. **Navigate**: https://localhost/logbook

2. **Find Sample**: Search for "S-2025-001"

3. **Initiate Move**:
   - Click Actions menu (⋮) on sample row
   - Select "Move"

4. **Move Dialog**:
   - Current location displayed: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5"
   - Select target location using cascading dropdowns:
     - Room: Main Laboratory
     - Device: Refrigerator 2
     - Shelf: Shelf-1
     - Rack: Rack R3
     - Position: C8
   - Enter reason: "Temporary storage for testing"

5. **Confirm Move**: Click "Move" button

6. **Verify**:
   - Success notification appears
   - Sample location updated in UI
   - Audit trail records movement

**API Verification**:

```bash
# Verify sample moved
curl -k https://localhost/rest/storage/samples/search?sampleId=S-2025-001

# Check audit log (if endpoint available)
curl -k https://localhost/rest/storage/samples/S-2025-001/movements
```

**Expected Result**: Sample moved to new location, previous position freed (occupied=false), audit trail created with user/timestamp/reason.

---

### P2B: Bulk Movement

**Workflow**: Move 5 samples to same rack

1. **Setup**: Create multiple samples with assigned locations

2. **Navigate**: https://localhost/logbook

3. **Select Samples**:
   - Use checkboxes to select 5 samples
   - Click Actions → "Bulk Move"

4. **Bulk Move Dialog**:
   - Select target rack: "Main Laboratory > Refrigerator 2 > Shelf-1 > Rack R3"
   - System auto-assigns positions: A1, A2, A3, A4, A5 (preview shown)
   - User can edit positions if needed
   - Enter reason: "Batch transfer for viral load testing"

5. **Confirm**: Click "Confirm Move"

6. **Verify**:
   - All 5 samples moved
   - Individual audit records created
   - Previous positions freed

**Expected Result**: Bulk move completes, each sample receives individual audit record, dashboard updates immediately.

---

## Troubleshooting

### Backend Issues

**Liquibase migration fails**:

```bash
# Check Liquibase status
mvn liquibase:status

# View changelog history
psql -U clinlims -d clinlims -c "SELECT * FROM databasechangelog WHERE id LIKE 'storage%';"

# Manual rollback (if needed)
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Clear locks (if hung)
mvn liquibase:clearCheckSums
```

**FHIR sync fails**:

```bash
# Verify FHIR server running
docker ps | grep fhir

# Check FHIR server logs
docker logs fhir.openelis.org

# Verify FHIR server accessible
curl -k https://fhir.openelis.org:8443/fhir/metadata

# Check OpenELIS FHIR configuration
grep "openelisglobal.fhir" common.properties
```

**Database connection errors**:

```bash
# Check PostgreSQL running
docker ps | grep postgres

# Test connection
psql -U clinlims -h localhost -d clinlims

# Restart database container
docker restart database.openelis.org
```

### Frontend Issues

**Frontend widget not appearing**:

```bash
# Check console for errors
# Open browser DevTools: F12 → Console tab

# Verify React Intl message keys loaded
# In browser console:
# intl.messages['storage.location.label']

# Clear browser cache and reload
# Ctrl+Shift+R (Chrome/Firefox)
```

**Barcode scanner not detected**:

```bash
# Verify scanner emitting keyboard events
# Open text editor, scan barcode
# Expected: Characters appear rapidly, followed by Enter

# Check scanner mode (USB HID keyboard mode)
# Consult scanner manual for configuration

# Test with manual entry as fallback
# Type barcode into TextInput field manually
```

**API requests failing (CORS, 401, etc.)**:

```bash
# Check session cookie present
# Browser DevTools → Application tab → Cookies
# Look for JSESSIONID cookie

# Re-login if session expired
# Navigate to: https://localhost/login

# Check API endpoint URL
# Verify: https://localhost/rest/storage/... (NOT http://)
```

### FHIR Issues

**Location resources not syncing**:

```bash
# Check FhirPersistanceService logs
docker logs oe.openelis.org | grep FhirPersistance

# Verify fhir_uuid generated for entities
psql -U clinlims -d clinlims -c "SELECT id, fhir_uuid FROM storage_room;"

# Manual sync trigger (if needed)
# POST to /rest/storage/rooms/{id}/sync-fhir
```

**FHIR validation errors**:

```bash
# Get FHIR resource
curl -k https://fhir.openelis.org:8443/fhir/Location/{id}

# Validate against R4 spec
curl -k -X POST https://fhir.openelis.org:8443/fhir/Location/\$validate \
  -H "Content-Type: application/fhir+json" \
  -d @location.json

# Check OperationOutcome for errors
```

### Test Issues

**Unit tests failing**:

```bash
# Run tests with verbose output
mvn test -Dtest="StorageRoomServiceImplTest" -X

# Check test database configuration
cat src/test/resources/application-test.properties

# Clean test artifacts
mvn clean test
```

**E2E tests failing (Playwright)**:

```bash
# Run with headed browser (see what's happening)
npx playwright test --headed

# Debug specific test
npx playwright test assignment.spec.ts --debug

# Update snapshots (if screenshot comparison fails)
npx playwright test --update-snapshots

# Check Playwright report
npx playwright show-report
```

---

## Development Tips

### Quick Iteration Cycle

**Backend changes**:
1. Edit Java file
2. `mvn clean install -DskipTests -pl :openelisglobal -am`
3. `docker-compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org`
4. Test in browser

**Frontend changes**:
1. Edit .jsx file
2. Webpack auto-reloads in browser
3. Test immediately (no rebuild needed)

### Useful Queries

```sql
-- View all storage locations
SELECT r.name AS room, d.name AS device, s.label AS shelf, k.label AS rack, p.coordinate AS position
FROM storage_room r
LEFT JOIN storage_device d ON d.parent_room_id = r.id
LEFT JOIN storage_shelf s ON s.parent_device_id = d.id
LEFT JOIN storage_rack k ON k.parent_shelf_id = s.id
LEFT JOIN storage_position p ON p.parent_rack_id = k.id
ORDER BY r.name, d.name, s.label, k.label, p.coordinate;

-- View sample assignments
SELECT sa.id, s.accession_number, p.coordinate, k.label, sh.label, d.name, r.name, sa.assigned_date
FROM sample_storage_assignment sa
JOIN sample s ON s.id = sa.sample_id
JOIN storage_position p ON p.id = sa.storage_position_id
JOIN storage_rack k ON k.id = p.parent_rack_id
JOIN storage_shelf sh ON sh.id = k.parent_shelf_id
JOIN storage_device d ON d.id = sh.parent_device_id
JOIN storage_room r ON r.id = d.parent_room_id
ORDER BY sa.assigned_date DESC;

-- View movement audit trail
SELECT sm.id, s.accession_number, sm.movement_date, sm.reason, 
       p_prev.coordinate AS prev_position, p_new.coordinate AS new_position
FROM sample_storage_movement sm
JOIN sample s ON s.id = sm.sample_id
LEFT JOIN storage_position p_prev ON p_prev.id = sm.previous_position_id
LEFT JOIN storage_position p_new ON p_new.id = sm.new_position_id
ORDER BY sm.movement_date DESC;
```

### Carbon Design System Resources

- **Components**: https://react.carbondesignsystem.com/
- **Icons**: https://www.carbondesignsystem.com/guidelines/icons/library/
- **Design Tokens**: https://www.carbondesignsystem.com/guidelines/color/overview/

---

## Next Steps

After completing quickstart:

1. **Review Code**: Examine generated code in `src/main/java/org/openelisglobal/storage/` and `frontend/src/components/storage/`
2. **Run Full Test Suite**: `mvn clean install` (backend) + `npm test` (frontend) + `npx playwright test` (E2E)
3. **Check Coverage**: Review JaCoCo report for >70% coverage
4. **FHIR Validation**: Verify all Location resources sync correctly to FHIR server
5. **User Testing**: Run through all user scenarios (P1, P2A, P2B)
6. **Documentation**: Review [plan.md](./plan.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

---

**Support**: For issues, see [Troubleshooting](#troubleshooting) or OpenELIS documentation at https://docs.openelis-global.org/

