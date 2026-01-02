# Equipment Usage Migration Recovery

## Issue

The initial Liquibase migration (042-equipment-usage-form-fields.xml) failed because it attempted to add a duplicate `usage_date` column that already exists in the `inventory_usage` table.

**Error:**
```
ERROR: column "usage_date" of relation "inventory_usage" already exists
```

## Resolution

The migration file has been corrected to remove the duplicate `usage_date` column. Now you need to:

### Step 1: Remove the Failed Migration from Liquibase Changelog

Connect to your PostgreSQL database and execute:

```sql
-- Remove the failed migration entry so it can be re-run
DELETE FROM clinlims.databasechangelog
WHERE id = 'add-equipment-usage-form-fields'
  AND author = 'claude'
  AND filename = 'liquibase/3.4.x.x/042-equipment-usage-form-fields.xml';
```

### Step 2: Restart the Application

The corrected migration will now run automatically when the application restarts.

### Step 3: Verify Migration Success

After restart, verify the columns were added:

```sql
-- Check that all 6 new columns exist
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'clinlims'
  AND table_name = 'inventory_usage'
  AND column_name IN (
    'operator_name',
    'usage_activities',
    'equipment_status',
    'login_time',
    'logout_time',
    'approved_by',
    'approval_date'
  );
```

Expected output: 7 columns listed

## What Changed

### Migration File: [042-equipment-usage-form-fields.xml](src/main/resources/liquibase/3.4.x.x/042-equipment-usage-form-fields.xml)

**Removed:** Duplicate `usage_date` column (VARCHAR type)
- The `usage_date` column already exists in the table as a `Timestamp` type
- The entity uses this existing column to store the usage timestamp

**Added:** 6 new columns to store equipment usage form fields:
1. `operator_name` (VARCHAR(255)) - Equipment operator name
2. `usage_activities` (TEXT) - Activities performed during usage
3. `equipment_status` (VARCHAR(50)) - Status (Functional/Non-functional/Maintenance)
4. `login_time` (VARCHAR(50)) - Operator login time
5. `logout_time` (VARCHAR(50)) - Operator logout time
6. `approved_by` (VARCHAR(255)) - Approval authority
7. `approval_date` (VARCHAR(50)) - Approval date

## Files Modified

- [src/main/resources/liquibase/3.4.x.x/042-equipment-usage-form-fields.xml](src/main/resources/liquibase/3.4.x.x/042-equipment-usage-form-fields.xml) - Removed duplicate column definition

## Next Steps

1. Execute the cleanup SQL against your PostgreSQL database
2. Restart the OpenELIS-Global application
3. Test the equipment usage submission endpoint - it should now work without the database error

## Testing

After migration completes, test the endpoint:

```bash
curl -X POST https://localhost/api/OpenELIS-Global/rest/equipment/usage/submit \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": 13,
    "lotId": 13,
    "quantity": 1,
    "operatorName": "John Doe",
    "date": "01/02/2026",
    "loginTime": "17:28",
    "activities": "Testing equipment usage",
    "equipmentStatus": "Functional",
    "logoutTime": "17:45",
    "approvedBy": "Jane Smith",
    "approvalDate": "01/02/2026"
  }'
```

Expected response: HTTP 201 CREATED with enriched EquipmentUsageEntryDTO containing all submitted fields.
