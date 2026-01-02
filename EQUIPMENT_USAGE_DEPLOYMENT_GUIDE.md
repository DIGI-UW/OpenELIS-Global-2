# Equipment Usage Feature - Deployment Guide

## Overview

The Equipment Usage Enhancement feature has been successfully implemented. This guide explains what was built and how to deploy it.

## What Was Built

A comprehensive equipment usage tracking system that:
- ✅ Records equipment usage **WITHOUT reducing inventory quantities**
- ✅ Captures complete form data (operator name, activities, login/logout times, approval info)
- ✅ Persists all data to the database (source of truth)
- ✅ Displays always-visible dashboard with metrics and submissions
- ✅ Provides full traceability (user, timestamp, equipment, lot)

## Architecture

### Backend Stack
- **Language:** Java 21
- **Framework:** Spring Boot 6.x
- **ORM:** Hibernate 5.6
- **Database:** PostgreSQL 14+
- **Migrations:** Liquibase 4.x

### Frontend Stack
- **Framework:** React 18+
- **UI Library:** Carbon Design System
- **Styling:** CSS + SCSS
- **State Management:** React Hooks + Context

## Implementation Components

### 1. Database Schema Changes

**File:** `src/main/resources/liquibase/3.4.x.x/042-equipment-usage-form-fields.xml`

Adds 6 new columns to `inventory_usage` table:

```sql
ALTER TABLE clinlims.inventory_usage ADD COLUMN
  operator_name VARCHAR(255),           -- Equipment operator name
  usage_activities TEXT,                -- Activities performed
  equipment_status VARCHAR(50),         -- Status (Functional/Non-functional/Maintenance)
  login_time VARCHAR(50),               -- Operator login time
  logout_time VARCHAR(50),              -- Operator logout time
  approved_by VARCHAR(255),             -- Approval authority
  approval_date VARCHAR(50);            -- Approval date
```

### 2. Backend Implementation

#### Entity: InventoryUsage
**File:** `src/main/java/org/openelisglobal/inventory/valueholder/InventoryUsage.java`

Added 6 new properties to store form fields (all nullable for backward compatibility):
```java
@Column(name = "operator_name")
private String operatorName;

@Column(name = "usage_activities", columnDefinition = "TEXT")
private String activities;

@Column(name = "equipment_status")
private String equipmentStatus;

@Column(name = "login_time")
private String loginTime;

@Column(name = "logout_time")
private String logoutTime;

@Column(name = "approved_by")
private String approvedBy;

@Column(name = "approval_date")
private String approvalDate;
```

#### DTO: EquipmentUsageEntryDTO
**File:** `src/main/java/org/openelisglobal/inventory/controller/rest/dto/EquipmentUsageEntryDTO.java`

Extended response DTO containing:
- Base inventory fields (item ID, lot ID, quantity used, etc.)
- Enriched user information (performed by user name)
- Equipment usage form fields
- Approval information

#### REST Controller: EquipmentUsageRestController
**File:** `src/main/java/org/openelisglobal/inventory/controller/rest/EquipmentUsageRestController.java`

**Endpoint 1: POST /rest/equipment/usage/submit**
```
Request Body:
{
  "itemId": 13,
  "lotId": 13,
  "quantity": 1,
  "operatorName": "John Doe",
  "date": "01/02/2026",
  "loginTime": "17:28",
  "activities": "Testing equipment",
  "equipmentStatus": "Functional",
  "logoutTime": "17:45",
  "approvedBy": "Jane Smith",
  "approvalDate": "01/02/2026"
}

Response: 201 Created
{
  EquipmentUsageEntryDTO with all submitted fields + database-generated ID
}
```

**Endpoint 2: GET /rest/equipment/usage/submissions**
```
Query Parameters:
- startDate (optional): ISO 8601 format
- endDate (optional): ISO 8601 format

Response: 200 OK
[
  List of all equipment usage submissions with all form fields from database
]
```

### 3. Frontend Implementation

#### Service: EquipmentUsageService.js
**File:** `frontend/src/components/equipmentUsage/EquipmentUsageService.js`

Added methods:
- `submitEquipmentUsageEntry()` - POST to /submit endpoint
- `getEquipmentUsageSubmissions()` - GET from /submissions endpoint

#### Components

**EquipmentUsageLog.jsx** - Usage entry form
- Collects all 8 form fields
- Date picker for date field
- Pre-fills operator name from current user
- Auto-populates login/logout times
- Sends complete data to /submit endpoint

**EquipmentUsageDashboard.jsx** - Metrics and submissions display
- 4 metric tiles: Total Equipment, Total Usage Records, Equipment with Usage, Recent Submission
- Usage by Equipment table
- Equipment Usage Submissions datatable (always visible)
- Fetches fresh data from database on load
- Print functionality

**EquipmentUsage.css** - Professional styling
- Responsive grid layouts (4 columns → 2 columns → 1 column)
- Metric tiles with hover effects
- DataTable with pagination support
- Form input styling
- Print-friendly styles

## Deployment Steps

### Step 1: Database Migration Setup

The migration is automatically registered in the Liquibase changelog:
- File: `src/main/resources/liquibase/3.4.x.x/base.xml`
- Includes: `042-equipment-usage-form-fields.xml`

### Step 2: Build and Deploy

```bash
# Clean build
mvn clean install -DskipTests -Dmaven.test.skip=true

# Apply code formatting
mvn spotless:apply

# Frontend formatting
cd frontend && npm run format && cd ..
```

### Step 3: Migration Execution

When the Spring Boot application starts:

1. Liquibase initializes automatically
2. Scans `base.xml` for migrations
3. Checks `databasechangelog` table for completed migrations
4. Executes any pending migrations (including 042-equipment-usage-form-fields.xml)
5. Records migration as complete

**If migration fails during initial run:**

See [MIGRATION_RECOVERY.md](MIGRATION_RECOVERY.md) for recovery instructions.

### Step 4: Application Restart

```bash
# If running in Docker
docker-compose restart oe.openelis.org

# If running locally
# Restart Tomcat/Spring Boot application
```

### Step 5: Verification

1. **Database Check:**
```sql
-- Verify columns exist
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'clinlims'
  AND table_name = 'inventory_usage'
  AND column_name IN (
    'operator_name', 'usage_activities', 'equipment_status',
    'login_time', 'logout_time', 'approved_by', 'approval_date'
  );
```

2. **Test Equipment Usage Submission:**
Navigate to: `/OpenELIS-Global/admin/equipment-usage`
- Select an equipment item
- Fill in form fields
- Submit
- Verify data appears in dashboard datatable

3. **Test Database Persistence:**
- Refresh page
- Verify data still appears (fetched from database)

## Data Flow

```
1. User selects equipment in frontend form
2. User fills all form fields
3. User clicks Submit
   ↓
4. Frontend: POST to /rest/equipment/usage/submit with all fields
   ↓
5. Backend: EquipmentUsageRestController.submitEquipmentUsageEntry()
   - Creates InventoryUsage record
   - Sets all form field properties
   - Calls usageService.save(usage) to persist to database
   - Returns enriched EquipmentUsageEntryDTO
   ↓
6. Frontend: Receives response with database-generated ID
   ↓
7. Frontend: Calls GET /rest/equipment/usage/submissions
   ↓
8. Backend: Returns all submissions from database with enriched data
   ↓
9. Frontend: Updates datatable with fresh data from database
   ↓
10. User sees submission in always-visible datatable
11. Data persists across page refreshes (database is source of truth)
```

## Key Features

### 1. Database as Source of Truth
- All form fields persisted to database when submitted
- Dashboard fetches data from database on page load
- No localStorage dependency (eliminated data loss on refresh)

### 2. Data Enrichment
- User display names resolved from SystemUser table
- Equipment names retrieved from InventoryItem table
- DTOs include both IDs and display names

### 3. Always-Visible Dashboard
- Submissions datatable never hidden
- Shows "No submissions yet" when empty
- Displays all submissions from database
- Print functionality for reporting

### 4. Complete Traceability
- Who used the equipment (performed by user name)
- When it was used (usage date timestamp)
- What activities were performed
- Equipment status at time of use
- Approval information

### 5. Backward Compatibility
- All new columns nullable
- Existing records without form fields remain valid
- No breaking changes to existing functionality

## Files Modified/Created

### Backend
- ✅ `src/main/resources/liquibase/3.4.x.x/042-equipment-usage-form-fields.xml` - NEW migration
- ✅ `src/main/resources/liquibase/3.4.x.x/base.xml` - MODIFIED to register migration
- ✅ `src/main/java/org/openelisglobal/inventory/valueholder/InventoryUsage.java` - MODIFIED entity
- ✅ `src/main/java/org/openelisglobal/inventory/controller/rest/EquipmentUsageRestController.java` - MODIFIED controller
- ✅ `src/main/java/org/openelisglobal/inventory/controller/rest/dto/EquipmentUsageEntryDTO.java` - NEW DTO

### Frontend
- ✅ `frontend/src/components/equipmentUsage/EquipmentUsageService.js` - MODIFIED service
- ✅ `frontend/src/components/equipmentUsage/EquipmentUsageLog.jsx` - MODIFIED form
- ✅ `frontend/src/components/equipmentUsage/EquipmentUsageDashboard.jsx` - MODIFIED dashboard
- ✅ `frontend/src/components/equipmentUsage/EquipmentUsage.css` - MODIFIED styling

### Documentation
- ✅ `MIGRATION_RECOVERY.md` - Migration troubleshooting guide
- ✅ `EQUIPMENT_USAGE_DEPLOYMENT_GUIDE.md` - This file

## Troubleshooting

### Issue: Database Migration Fails

**Error:** `ERROR: column "usage_activities" does not exist`

**Cause:** Liquibase migration hasn't run yet or failed

**Solution:** See [MIGRATION_RECOVERY.md](MIGRATION_RECOVERY.md)

### Issue: Form Submission Returns 500 Error

**Possible Causes:**
1. Database columns don't exist (migration hasn't run)
2. User not authenticated in session
3. Inventory item or lot doesn't exist

**Solution:** Check server logs and verify migration completed successfully

### Issue: Dashboard Shows No Submissions

**Possible Causes:**
1. No submissions have been made yet
2. GET /submissions endpoint not working
3. CORS issue if frontend on different domain

**Solution:** Check network tab in browser dev tools

## Performance Considerations

### Database Indexing
Liquibase migration automatically creates indexes on:
- `operator_name` - For filtering by operator
- `equipment_status` - For filtering by status

### Query Optimization
- Use date range parameters when fetching submissions
- Consider pagination for large result sets
- Profile database queries in production

## Security Considerations

### Authentication
- All endpoints check session for `UserSessionData`
- Returns 401 Unauthorized if user not authenticated

### Data Validation
- Form fields validated on both frontend and backend
- Inventory item and lot existence verified
- Quantity must be positive

### Audit Trail
- All changes tracked automatically via existing audit system
- User ID recorded for every submission
- Timestamp captured on creation

## Testing Checklist

- [ ] Database migration runs on application startup
- [ ] All 6 new columns created in inventory_usage table
- [ ] Equipment Usage form loads without errors
- [ ] Form can be filled with all 8 fields
- [ ] Form submission sends data to /submit endpoint
- [ ] Dashboard displays submitted data
- [ ] Data persists across page refresh
- [ ] Always-visible datatable shows submissions
- [ ] Print functionality works
- [ ] Metrics tiles display correct values

## Support

For questions or issues, refer to:
- Backend: [EquipmentUsageRestController.java](src/main/java/org/openelisglobal/inventory/controller/rest/EquipmentUsageRestController.java)
- Frontend: [EquipmentUsageDashboard.jsx](frontend/src/components/equipmentUsage/EquipmentUsageDashboard.jsx)
- Database: [042-equipment-usage-form-fields.xml](src/main/resources/liquibase/3.4.x.x/042-equipment-usage-form-fields.xml)
- Migration Help: [MIGRATION_RECOVERY.md](MIGRATION_RECOVERY.md)
