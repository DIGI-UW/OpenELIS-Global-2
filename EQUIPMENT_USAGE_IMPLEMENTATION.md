# Equipment Usage Log - Implementation Guide

## Overview

The Equipment Usage Log module is a standalone feature that tracks permanent
laboratory equipment usage in OpenELIS. It operates independently from the
inventory management system and is specifically designed to support MNTD
laboratory compliance requirements (Format 5.3-003).

## Features

✅ **Equipment Master Data Management** - Create, edit, and manage equipment
records ✅ **Usage Entry Tracking** - Log equipment usage with operator details
and activities ✅ **Approval Workflow** - Draft → Submitted → Approved workflow
with role-based access ✅ **Advanced Search** - Filter by equipment, operator,
date range, department, and status ✅ **Audit Trail** - All changes tracked with
user and timestamp ✅ **Export/Reports** - PDF and Excel export (framework in
place) ✅ **MNTD Compliance** - Captures all fields from Equipment Usage Format
5.3-003

## Architecture

### Backend (5-Layer Pattern)

#### 1. **Value Holders** (Database Entities)

```
org.openelisglobal.equipmentusage.valueholder/
├── Equipment.java
├── EquipmentUsageEntry.java
└── EquipmentUsageAudit.java
```

#### 2. **DAO Layer** (Data Access)

```
org.openelisglobal.equipmentusage.dao/
├── EquipmentDAO.java
└── EquipmentUsageEntryDAO.java

org.openelisglobal.equipmentusage.daoimpl/
├── EquipmentDAOImpl.java
└── EquipmentUsageEntryDAOImpl.java
```

#### 3. **Service Layer** (Business Logic)

```
org.openelisglobal.equipmentusage.service/
├── EquipmentService.java
├── EquipmentServiceImpl.java
├── EquipmentUsageEntryService.java
└── EquipmentUsageEntryServiceImpl.java
```

#### 4. **REST Controllers** (API Endpoints)

```
org.openelisglobal.equipmentusage.controller.rest/
├── EquipmentRestController.java
└── EquipmentUsageRestController.java
```

### Frontend (React Components)

```
frontend/src/components/equipmentUsage/
├── EquipmentUsageManagement.jsx       (Main container with tabs)
├── EquipmentUsageLog.jsx              (Create/edit entries)
├── EquipmentUsageHistory.jsx          (View all entries with filters)
├── EquipmentUsageApproval.jsx         (Supervisor approval workflow)
├── EquipmentUsageReports.jsx          (Export/reports)
├── EquipmentUsageService.js           (API client)
├── EquipmentUsage.css                 (Styling)
└── modals/
    └── EquipmentUsageModal.jsx        (Equipment usage entry form)
```

## Database Schema

### Tables Created (via Liquibase Migration: 041-equipment-usage-log.xml)

#### **equipment**

Stores equipment master data

- `id` - Primary key
- `name` - Equipment name
- `serial_number` - Unique identifier
- `department` - Laboratory department
- `manufacturer`, `model_number` - Equipment details
- `last_calibration_date`, `next_calibration_due` - Maintenance tracking
- `is_active` - Soft delete flag

#### **equipment_usage_entry**

Stores individual usage logs

- `id` - Primary key
- `equipment_id` (FK) - Links to equipment
- `operator_name`, `operator_id` (FK) - Who used the equipment
- `login_time`, `logout_time` - Usage duration
- `activities_done` - Description of activities
- `equipment_status` - ENUM (FUNCTIONAL, UNDER_MAINTENANCE, FAULTY,
  CALIBRATION_REQUIRED)
- `department` - Usage location
- `entry_status` - ENUM (DRAFT, SUBMITTED, APPROVED)
- `approved_by`, `approval_date`, `approval_signature` - Approval tracking
- Audit fields: `created_by`, `created_date`, `modified_by`, `modified_date`,
  `version`

#### **equipment_usage_audit**

Audit trail for all changes

- `id` - Primary key
- `usage_entry_id` (FK) - Links to usage entry
- `change_type` - Type of change
- `changed_field` - Which field changed
- `old_value`, `new_value` - Before/after values
- `changed_by`, `changed_date` - Who made the change and when

## API Endpoints

### Equipment Management

```
GET    /rest/equipment                      # Get all active equipment
GET    /rest/equipment/dropdown             # Get equipment for dropdowns
GET    /rest/equipment/{id}                 # Get equipment by ID
GET    /rest/equipment/serial/{serialNumber}  # Get by serial number
GET    /rest/equipment/search?q={query}    # Search equipment
GET    /rest/equipment/department/{dept}   # Get by department
POST   /rest/equipment                      # Create equipment
PUT    /rest/equipment/{id}                 # Update equipment
PUT    /rest/equipment/{id}/activate       # Activate equipment
PUT    /rest/equipment/{id}/deactivate     # Deactivate equipment
```

### Equipment Usage Entries

```
GET    /rest/equipment-usage                             # Get all entries
GET    /rest/equipment-usage/{id}                        # Get entry by ID
GET    /rest/equipment-usage/equipment/{equipmentId}     # Get by equipment
GET    /rest/equipment-usage/equipment/{id}/range        # Date range filter
GET    /rest/equipment-usage/operator/{operatorId}       # Get by operator
GET    /rest/equipment-usage/pending-approval            # Get pending approval
GET    /rest/equipment-usage/approved                    # Get approved entries
GET    /rest/equipment-usage/department/{dept}           # Get by department
GET    /rest/equipment-usage/search?...                  # Advanced search
POST   /rest/equipment-usage                             # Create entry (DRAFT)
PUT    /rest/equipment-usage/{id}/draft                  # Save draft
PUT    /rest/equipment-usage/{id}/submit                 # Submit for approval
PUT    /rest/equipment-usage/{id}/approve?approverId=... # Approve
PUT    /rest/equipment-usage/{id}/reject                 # Reject
GET    /rest/equipment-usage/{id}/can-edit               # Check edit permission
GET    /rest/equipment-usage/{id}/can-approve            # Check approve permission
```

## Navigation Integration

The Equipment Usage Log is added to the main navigation menu via database
entries:

**Menu Structure:**

```
Equipment Usage (Parent)
└── Equipment Usage Log (Child)
    └── URL: /equipment-usage
```

**Liquibase entries** (via 041-equipment-usage-log.xml):

- `menu_equipment_usage` - Parent menu
- `menu_equipment_usage_log` - Child menu with route

## Role-Based Access Control

### Access Levels

| Role                 | Permissions                                                                                 |
| -------------------- | ------------------------------------------------------------------------------------------- |
| **Lab Technologist** | ✓ Create entries<br>✓ View own drafts<br>✗ Approve<br>✓ View history (read-only)            |
| **Lab Supervisor**   | ✓ Create entries<br>✓ View all entries<br>✓ Approve/Reject<br>✓ View audit trails           |
| **Admin**            | ✓ Full access<br>✓ Manage equipment master<br>✓ Manage permissions<br>✓ View all audit logs |

### Route Protection (frontend/src/App.js)

```javascript
<SecureRoute
  path="/equipment-usage"
  exact
  component={() => <EquipmentUsageManagement />}
  role={[Roles.RESULTS, Roles.GLOBAL_ADMIN]}
/>
```

## Data Flow

### Creating an Entry

1. User navigates to Equipment Usage → Usage Log tab
2. Clicks "New Entry"
3. Modal opens with form fields:
   - Equipment (dropdown, auto-fills serial number & department)
   - Operator Name (auto-populated, editable)
   - Login/Logout Time (date & time pickers)
   - Activities (multiline text)
   - Equipment Status (dropdown)
4. User saves as DRAFT
5. Entry stored in `equipment_usage_entry` with status=DRAFT
6. Audit record created in `equipment_usage_audit`

### Submitting for Approval

1. User selects DRAFT entry
2. Clicks "Submit for Approval"
3. Status changes to SUBMITTED
4. Entry becomes read-only for technologist
5. Entry appears in Supervisor's "Approval" tab

### Approval Workflow

1. Supervisor navigates to Approval tab
2. Reviews pending entries
3. Approves or Rejects
4. If approved:
   - Status → APPROVED
   - Sets `approved_by`, `approval_date`, `approval_signature`
   - Entry becomes permanently read-only
   - Moved to "History" tab with APPROVED filter

### History & Reporting

1. Users filter entries by equipment, date, status, department
2. View detailed history in History tab
3. Export to PDF/Excel (framework ready)
4. Print for regulatory compliance

## Internationalization (i18n)

Added message keys in `src/main/resources/languages/message_en.properties`:

```properties
sidenav.label.equipment.usage = Equipment Usage
sidenav.label.equipment.usage.log = Equipment Usage Log
equipment.usage.title = Equipment Usage Log
equipment.usage.tab.log = Usage Log
equipment.usage.tab.history = History
equipment.usage.tab.approval = Approval
equipment.usage.tab.reports = Reports
equipment.usage.button.new = New Entry
...
```

## Migration Instructions

### 1. Database Migration

Liquibase will automatically apply migration `041-equipment-usage-log.xml` on
startup:

- Creates `equipment` table
- Creates `equipment_usage_entry` table
- Creates `equipment_usage_audit` table
- Adds menu entries to navigation
- Creates sequences for ID generation

### 2. Backend Deployment

- Java classes compiled during Maven build
- REST controllers registered via `@RestController` annotations
- Services auto-wired via Spring dependency injection

### 3. Frontend Deployment

- React components bundled during npm build
- Route automatically added to App.js
- CSS styling applied via imported stylesheet

### 4. Configuration

No additional configuration needed. The module is automatically:

- Integrated with Spring Security via SecureRoute
- Connected to the menu system via Liquibase
- Wired to the REST API endpoints

## Compliance Features

### MNTD Format 5.3-003 Alignment

✓ Equipment identification (name + serial number) ✓ Operator accountability
(name + timestamp) ✓ Login/logout times with activities ✓ Equipment status
tracking ✓ Approval signature (digital: username + timestamp) ✓ Audit trail (all
changes tracked) ✓ Permanent record (approved entries read-only)

### Audit & Regulatory Requirements

- All modifications tracked in `equipment_usage_audit` table
- User attribution for every change
- Timestamp precision (to millisecond)
- Immutable approved entries
- Export capability for inspections

## Future Enhancements

- [ ] Complete PDF/Excel export implementation
- [ ] Equipment maintenance history visualization
- [ ] Equipment calibration alerts
- [ ] Bulk entry import via CSV
- [ ] Dashboard with equipment usage metrics
- [ ] Email notifications for pending approvals
- [ ] Signature capture integration
- [ ] Mobile app support

## Troubleshooting

### Issue: Equipment dropdown is empty

- **Cause**: No equipment created in master data
- **Solution**: Add equipment via API endpoint or admin interface

### Issue: Cannot see Equipment Usage in navigation

- **Cause**: Liquibase migration not applied
- **Solution**: Check logs for migration errors; manually add menu entry if
  needed

### Issue: Approval endpoint returns 403 Forbidden

- **Cause**: User doesn't have RESULTS or GLOBAL_ADMIN role
- **Solution**: Add appropriate role to user via admin interface

## Development Notes

- Base classes: All extend `BaseObject<Long>` for consistency
- Audit trail: Automatic via `AuditableBaseObjectServiceImpl`
- Transaction management: `@Transactional` annotations on service layer
- Error handling: Wrapped in LIMSRuntimeException with logging
- API responses: Standard HTTP status codes with JSON payloads

## Testing Checklist

- [ ] Create equipment master data
- [ ] Create equipment usage entry as draft
- [ ] Edit draft entry
- [ ] Submit entry for approval
- [ ] Approve entry as supervisor
- [ ] Reject entry (revert to draft)
- [ ] Search/filter entries
- [ ] Verify audit trail captures changes
- [ ] Test role-based access restrictions
- [ ] Verify menu navigation appears correctly

## Support

For issues or questions, refer to:

- Project Constitution: `.specify/memory/constitution.md`
- Agent Onboarding: `AGENTS.md`
- Architecture Documentation: This file

---

**Implementation Date**: 2025-12-31 **Status**: Complete - Ready for Testing
**Version**: 1.0.0
