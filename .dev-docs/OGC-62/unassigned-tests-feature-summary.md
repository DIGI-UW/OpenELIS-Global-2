# Unassigned Tests Feature - Summary of Additions

**Document Version:** 1.1  
**Date:** November 7, 2025  
**Feature:** Unassigned Referred Tests Tracking

---

## Overview

The Unassigned Tests feature adds a critical accountability mechanism to ensure all samples marked for referral are tracked and accounted for. This prevents samples from being forgotten or lost between the time they're marked for referral and when they're actually shipped.

---

## What Was Added to the Specification

### 1. New User Stories (5 stories)

**US-021A: View Unassigned Referred Tests**
- Dashboard tab showing all referred samples not yet in a shipment
- Searchable and filterable table
- Visual highlighting based on aging (yellow for 7-30 days, red for >30 days)

**US-021B: Add Unassigned Sample to Box**
- Quick action to add sample directly to existing or new box
- Removes sample from unassigned list

**US-021C: Mark Unassigned Sample as Lost**
- Workflow to permanently mark sample as lost
- Requires reason and confirmation
- Cannot be undone (except by administrator)

**US-021D: Cancel Referral for Unassigned Sample**
- Remove referral flag and return sample to normal workflow
- Requires reason
- Sample removed from all shipping tracking

**US-021E: Bulk Actions on Unassigned Samples**
- Multi-select capability
- Bulk add to box
- Bulk export

---

### 2. New Business Rules (6 rules)

**BR-028: Unassigned Test Definition**
- Clear definition of what makes a sample "unassigned"
- Criteria for appearing in unassigned list

**BR-029: Unassigned Test Aging**
- Aging thresholds: 0-7 days (normal), 7-30 days (warning), >30 days (alert)
- Visual indicators based on aging
- Configurable thresholds

**BR-030: Mark as Lost Workflow**
- Permanent action (requires admin to reverse)
- Lost samples retain referral flag but are tracked separately
- Included in Lost Samples report

**BR-031: Cancel Referral Workflow**
- Removes referral tracking completely
- Returns sample to normal OpenELIS workflow
- Requires documented reason

**BR-032: Unassigned Test Notifications**
- Optional alerting for samples unassigned >7 days
- Weekly summary reports
- Configurable notification settings

**BR-033: Bulk Operations**
- All-or-nothing transaction for bulk actions
- Validation of all selected samples
- Single audit log entry for bulk operation

---

### 3. New Functional Requirements (8 requirements)

**FR-028A: Display Unassigned Tests List**
- Query logic for finding unassigned samples
- Table display with all required columns
- Real-time refresh when samples added to boxes

**FR-028B: Search and Filter Unassigned Tests**
- Global search across all columns
- Multiple filter dimensions (date, destination, test type, aging)
- Combined filter logic

**FR-028C: Add Unassigned Sample to Box**
- Modal workflow for box selection
- Option to create new box
- Automatic removal from unassigned list

**FR-028D: Mark Unassigned Sample as Lost**
- Confirmation dialog with warning
- Update sample flags and timestamps
- Audit trail logging

**FR-028E: Cancel Referral for Unassigned Sample**
- Referral flag removal
- OpenELIS integration to update sample status
- Reason capture and logging

**FR-028F: Bulk Selection and Actions**
- Multi-select with checkboxes
- Batch actions toolbar
- Transaction handling for bulk operations

**FR-028G: Export Unassigned Tests**
- Multiple export formats (CSV, Excel, PDF)
- Include filter criteria in export
- Audit logging of exports

**FR-028H: Unassigned Tests Aging Alerts**
- Daily background check for aged samples
- Email digest option
- Dashboard alert banner

---

### 4. New UI Components

**Dashboard with Tab Navigation**
```
[Shipments] [Unassigned Tests] ← New Tab
```

**Unassigned Tests Table**
- Columns: Accession #, Patient, Test Type, Collection Date, Referral Date, Destination, Referral Reason, Days Unassigned
- Color-coded rows based on aging
- Overflow menu with actions: Add to Box, Mark as Lost, Cancel Referral

**Alert Banner**
- Shows count of unassigned samples
- Highlights samples >7 days unassigned
- Dismissible

**Three New Modals:**
1. **Add to Box Modal** - Select existing box or create new
2. **Mark as Lost Modal** - Warning dialog with reason selection
3. **Cancel Referral Modal** - Confirmation with reason selection
4. **Bulk Add Modal** - Handles multiple samples, validates destinations match

**Visual Indicators:**
- Normal (0-7 days): White background
- Warning (7-30 days): Yellow/amber tint (#FFF3CD)
- Alert (>30 days): Red/pink tint (#FFE6E6)

---

### 5. Data Model Extensions

**Sample Table Additions:**
```sql
ALTER TABLE sample ADD COLUMN referral_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE sample ADD COLUMN referral_date TIMESTAMP NULL;
ALTER TABLE sample ADD COLUMN referral_destination_facility_id BIGINT NULL;
ALTER TABLE sample ADD COLUMN referral_reason TEXT NULL;
ALTER TABLE sample ADD COLUMN referral_cancelled_date TIMESTAMP NULL;
ALTER TABLE sample ADD COLUMN referral_cancelled_by BIGINT NULL;
ALTER TABLE sample ADD COLUMN referral_cancellation_reason TEXT NULL;
ALTER TABLE sample ADD COLUMN lost_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE sample ADD COLUMN lost_date TIMESTAMP NULL;
ALTER TABLE sample ADD COLUMN lost_by BIGINT NULL;
ALTER TABLE sample ADD COLUMN lost_reason TEXT NULL;
```

**New Tables:**
1. **lost_sample_log** - Tracks all samples marked as lost with full audit trail
2. **referral_cancellation_log** - Tracks all referral cancellations with reasons
3. **unassigned_alert_config** - Admin configuration for aging alerts
4. **unassigned_samples_view** - Database view for efficient querying

---

## Key Features Summary

### Accountability & Tracking
✅ All referred samples are visible until assigned to a box  
✅ Aging indicators prevent samples from being forgotten  
✅ Multiple views: unassigned, lost, cancelled  
✅ Full audit trail for all actions  

### Operational Efficiency
✅ Quick add to box from unassigned list  
✅ Bulk operations for high-volume workflows  
✅ Search and filter to find specific samples quickly  
✅ Export capability for reporting  

### Quality & Safety
✅ Formal "Mark as Lost" workflow with required documentation  
✅ Referral cancellation workflow for changed requirements  
✅ Visual alerts for aging samples (7 day and 30 day thresholds)  
✅ Optional email notifications for aged samples  

### User Experience
✅ Tab-based navigation for easy access  
✅ Color-coded rows for instant visual assessment  
✅ Inline actions from overflow menu  
✅ Confirmation dialogs for critical actions  
✅ Bulk selection with batch actions toolbar  

---

## Integration Points

### With OpenELIS Order Entry System
- Reads referral flag and metadata from sample records
- Updates referral flag when adding to boxes or cancelling referrals
- Syncs lost status with sample management

### With Box Management
- Real-time removal from unassigned list when sample added to box
- Validates destination matching when adding to box
- Links to box creation workflow

### With Reporting
- Unassigned samples report
- Lost samples report
- Referral cancellation report
- Aging analysis report

---

## Permissions

| Action | Shipping Role | Admin Role |
|--------|--------------|-----------|
| View unassigned tests | ✓ | ✓ |
| Add unassigned sample to box | ✓ | ✓ |
| Mark sample as lost | ✓ | ✓ |
| **Reverse lost status** | ✗ | **✓** (admin only) |
| Cancel referral | ✓ | ✓ |
| Export unassigned tests | ✓ | ✓ |
| Configure aging thresholds | ✗ | ✓ |
| Configure alerts | ✗ | ✓ |

---

## Configuration Options (Admin)

Administrators can configure:
1. **Aging Thresholds**
   - Warning threshold (default: 7 days)
   - Alert threshold (default: 30 days)

2. **Email Notifications**
   - Enable/disable notifications
   - Recipient email addresses
   - Notification frequency (daily or weekly)

3. **Lost Sample Reason Categories**
   - Predefined reasons can be customized
   - Required vs. optional reasons

4. **Referral Cancellation Reason Categories**
   - Predefined reasons can be customized
   - Required vs. optional reasons

---

## Implementation Priority

Given the accountability focus of this feature, the recommended implementation order is:

### Phase 1 (Core - High Priority)
1. Database schema changes to sample table
2. Unassigned tests view/query logic
3. Dashboard tab with basic table display
4. Add to box workflow
5. Basic search and filter

### Phase 2 (Critical Actions - High Priority)
6. Mark as lost workflow with confirmation
7. Cancel referral workflow
8. Visual aging indicators (color coding)
9. Action audit logging

### Phase 3 (Efficiency - Medium Priority)
10. Bulk selection and actions
11. Export functionality
12. Advanced filters
13. Lost sample and cancellation logs

### Phase 4 (Alerts - Lower Priority)
14. Aging alert configuration
15. Email notifications
16. Alert banner on dashboard

---

## Testing Considerations

### Key Test Scenarios
1. **Sample appears in unassigned list** when referral flag set
2. **Sample disappears from unassigned list** when added to box
3. **Aging calculation correct** (current date - referral date)
4. **Color coding updates** based on aging thresholds
5. **Mark as lost** removes from unassigned, can't be undone
6. **Cancel referral** removes from all tracking, returns to normal workflow
7. **Bulk add to box** validates all samples have same destination
8. **Search and filters** work correctly across all columns
9. **Export** includes filtered results only
10. **Admin can reverse lost status** but regular users cannot

### Edge Cases
- Sample marked for referral but immediately added to box
- Sample in box, box cancelled, sample becomes unassigned again
- Bulk operation with samples for different destinations
- Lost sample later found (admin reversal workflow)
- Referral cancelled but sample needs to be referred again later
- User tries to add lost sample to box (should be prevented)

---

## Benefits to Users

### For Shipping Coordinators
- No more manually tracking referred samples in spreadsheets
- Visual indicators show which samples need immediate attention
- Quick actions reduce time to assign samples to boxes
- Bulk operations for high-volume periods

### For Laboratory Management
- Complete accountability for all referred samples
- Metrics on aging and turnaround time
- Identification of bottlenecks in referral process
- Audit trail for compliance and quality management

### For Quality Assurance
- Documented workflow for lost samples
- Reason capture for all referral cancellations
- Aging reports for process improvement
- Full traceability of sample lifecycle

---

## Metrics & KPIs

The unassigned tests feature enables tracking of:
- Average days from referral to assignment
- Number of samples >7 days unassigned
- Number of samples >30 days unassigned
- Lost sample rate
- Referral cancellation rate
- Reasons for cancellations (for process improvement)
- Destination-specific turnaround times

---

## Future Enhancements (Out of Scope for v1.1)

Potential future additions:
- Predictive alerts based on historical patterns
- Automated box suggestions based on destination and sample volume
- Integration with courier scheduling systems
- Mobile app for quick unassigned sample lookup
- Dashboard widgets showing trending metrics
- SMS notifications for critical aging thresholds
- Barcode scanning directly from unassigned list (one-scan add to box)

---

**End of Summary**
