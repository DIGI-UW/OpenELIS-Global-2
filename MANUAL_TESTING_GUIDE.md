# Manual Testing Guide: OGC-73 & OGC-75 Fixes

## Environment Setup

✅ **Database Reset Complete**

- Database volumes cleared and recreated
- Liquibase migrations executed
- SampleDisposed status (ID 24) verified in database

✅ **Test Fixtures Loaded**

- **Storage Hierarchy**: 3 Rooms, 5 Devices, 6 Shelves, 6 Racks, 106 Positions
- **E2E Test Data**: 3 Patients, 10 Samples, 20 Sample Items, 14 Storage
  Assignments
- **Test Patients**: John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams
- **Test Samples**: DEV01000000000000001 through DEV01000000000000010
- **Sample Items with Assignments**: 14 items assigned to various locations
- **Unassigned Sample Items**: Available for testing assignment workflow

✅ **Application Status**

- Containers running: `docker compose -f dev.docker-compose.yml ps`
- Backend WAR rebuilt and deployed
- Frontend hot-reload enabled

## Access Points

| Service   | URL                                    | Credentials         |
| --------- | -------------------------------------- | ------------------- |
| React UI  | https://localhost/                     | admin / adminADMIN! |
| Legacy UI | https://localhost/api/OpenELIS-Global/ | admin / adminADMIN! |

**Note:** If browser shows security warning, click "Advanced" → "Proceed to
https://localhost"

---

## OGC-73: Sample Disposal API Fix

### Test Scenario 1: Dispose Sample Item (Success)

1. **Navigate to Storage Management:**

   - Go to https://localhost/
   - Login: `admin` / `adminADMIN!`
   - Navigate to Storage section

2. **Select a Test Sample Item:**

   - Use one of the pre-loaded sample items with assignments:
     - **Sample Item ID 10001** (E2E-001-TUBE-1) - Assigned to Main Freezer
       Shelf-A Rack 1, position A1
     - **Sample Item ID 10002** (E2E-001-ALIQUOT-1) - Assigned to Main
       Refrigerator Shelf-1 Rack 1, position X1
     - **Sample Item ID 10011** (E2E-002-TUBE-1) - Assigned to Main Freezer
       Shelf-A Rack 1, position A2
   - Or search for samples by accession number: DEV01000000000000001 through
     DEV01000000000000010

3. **Dispose the Sample:**

   - Use the disposal API endpoint or UI
   - POST to `/rest/storage/sample-items/dispose`
   - Request body:
     ```json
     {
       "sampleItemId": "<sample-item-id>",
       "reason": "expired",
       "method": "autoclave",
       "notes": "Manual test disposal"
     }
     ```

4. **Verify Success:**
   - ✅ Response: HTTP 200
   - ✅ Response contains: `"status": "disposed"`
   - ✅ Database: `sample_item.status_id = '24'` (SampleDisposed)
   - ✅ Storage assignment cleared (if existed)

### Test Scenario 2: Dispose Already Disposed Sample (Error)

1. **Dispose a sample** (follow Test Scenario 1)

2. **Try to dispose again:**

   - Use same sample item ID
   - Same request body

3. **Verify Error:**
   - ✅ Response: HTTP 400
   - ✅ Error message contains: "already disposed"

### Test Scenario 3: Dispose Sample with Missing Fields (Validation)

1. **Try disposal without reason:**

   ```json
   {
     "sampleItemId": "<sample-item-id>",
     "method": "autoclave"
   }
   ```

2. **Verify Error:**

   - ✅ Response: HTTP 400
   - ✅ Error message contains: "reason"

3. **Try disposal without method:**

   ```json
   {
     "sampleItemId": "<sample-item-id>",
     "reason": "expired"
   }
   ```

4. **Verify Error:**
   - ✅ Response: HTTP 400
   - ✅ Error message contains: "method"

### Database Verification

```sql
-- Check SampleDisposed status exists
SELECT id, name, status_type FROM status_of_sample
WHERE name = 'SampleDisposed' AND status_type = 'SAMPLE';
-- Expected: id=24, name='SampleDisposed'

-- Check disposed sample item
SELECT id, samp_id, status_id FROM sample_item
WHERE status_id = '24';
-- Should show disposed sample items

-- Check storage assignment cleared
SELECT * FROM sample_storage_assignment
WHERE sample_item_id IN (
  SELECT id FROM sample_item WHERE status_id = '24'
);
-- Should return 0 rows (assignments cleared on disposal)
```

---

## OGC-75: Shelf Deletion URL Pluralization Fix

### Test Scenario 1: Delete Shelf (Success)

1. **Navigate to Storage Location Management:**

   - Go to https://localhost/
   - Navigate to Storage → Locations

2. **Select a Test Shelf:**

   - Use one of the pre-loaded shelves:
     - **Main Freezer Shelf-A** (has racks with sample assignments - test
       constraint checking)
     - **Main Freezer Shelf-B** (has racks - test constraint checking)
     - **Main Refrigerator Shelf-2** (no racks - can be deleted if no
       assignments)
   - Query shelves:
     `SELECT id, label, parent_device_id FROM storage_shelf ORDER BY id;`

3. **Delete the Shelf:**

   - Click delete button on shelf
   - Confirm deletion

4. **Verify Success:**
   - ✅ API call uses correct URL: `/rest/storage/shelves/{id}` (NOT
     `/rest/storage/shelfs/{id}`)
   - ✅ Shelf deleted successfully
   - ✅ No 404 errors in browser console

### Test Scenario 2: Delete Shelf with Constraints (Error)

1. **Select a shelf with child racks:**

   - Find a shelf that has racks assigned
   - Try to delete it

2. **Verify Error Handling:**
   - ✅ API call uses correct URL: `/rest/storage/shelves/{id}/can-delete`
   - ✅ Error message displayed: "Cannot delete shelf with child racks"
   - ✅ Shelf NOT deleted

### Browser Console Verification

1. **Open Browser DevTools** (F12)
2. **Navigate to Network tab**
3. **Try to delete a shelf**
4. **Check API calls:**
   - ✅ URL contains `/rest/storage/shelves/` (plural "shelves")
   - ✅ NO `/rest/storage/shelfs/` (incorrect "shelfs")

### Test All Location Types

Verify pluralization works for all types:

- ✅ Rooms: `/rest/storage/rooms/{id}`
- ✅ Devices: `/rest/storage/devices/{id}`
- ✅ Shelves: `/rest/storage/shelves/{id}` (irregular plural - this was the bug)
- ✅ Racks: `/rest/storage/racks/{id}`

---

## Quick Verification Commands

### Check Application Status

```bash
docker compose -f dev.docker-compose.yml ps
```

### Check Database Status

```bash
# Check SampleDisposed status
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT id, name FROM status_of_sample WHERE name = 'SampleDisposed';"

# Check test fixtures loaded
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT COUNT(*) as rooms FROM storage_room; SELECT COUNT(*) as shelves FROM storage_shelf; SELECT COUNT(*) as sample_items FROM sample_item WHERE id >= 10000;"
```

### View Application Logs

```bash
docker compose -f dev.docker-compose.yml logs -f oe.openelis.org
```

### Rebuild After Code Changes

```bash
# Backend changes
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# Frontend changes (hot-reloads automatically)
cd frontend && npm run format
```

---

## Expected Results Summary

### OGC-73 (Sample Disposal)

- ✅ SampleDisposed status (ID 24) exists in database
- ✅ Disposal API sets status_id to '24' (numeric string)
- ✅ Disposal API clears storage assignments
- ✅ Disposal API creates movement audit record
- ✅ Validation errors for missing fields
- ✅ Error when trying to dispose already-disposed sample

### OGC-75 (Shelf Deletion)

- ✅ Delete shelf uses `/rest/storage/shelves/{id}` (correct plural)
- ✅ All location types use correct pluralization
- ✅ No 404 errors in browser console
- ✅ Constraint checking works correctly

---

## Troubleshooting

### Application Not Starting

```bash
# Check logs
docker compose -f dev.docker-compose.yml logs oe.openelis.org

# Restart containers
docker compose -f dev.docker-compose.yml restart
```

### Database Connection Issues

```bash
# Check database is healthy
docker compose -f dev.docker-compose.yml ps db.openelis.org

# Check database logs
docker compose -f dev.docker-compose.yml logs db.openelis.org
```

### Liquibase Migration Issues

```bash
# Check migration status
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT * FROM databasechangelog WHERE id = 'add-sample-disposed-status';"
```

---

## Next Steps After Manual Testing

Once manual testing passes:

1. ✅ All test scenarios verified
2. ✅ No console errors
3. ✅ Database state correct
4. ✅ Ready to continue with additional features or fixes
