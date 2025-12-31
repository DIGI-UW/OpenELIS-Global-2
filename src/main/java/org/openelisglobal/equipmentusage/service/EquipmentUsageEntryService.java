package org.openelisglobal.equipmentusage.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry.EntryStatus;

public interface EquipmentUsageEntryService extends BaseObjectService<EquipmentUsageEntry, Long> {

    /**
     * Get all usage entries for an equipment
     */
    List<EquipmentUsageEntry> getByEquipmentId(Long equipmentId);

    /**
     * Get usage entries for equipment within date range
     */
    List<EquipmentUsageEntry> getByEquipmentAndDateRange(Long equipmentId, LocalDate startDate, LocalDate endDate);

    /**
     * Get all usage entries by operator
     */
    List<EquipmentUsageEntry> getByOperatorId(Long operatorId);

    /**
     * Get usage entries by status (DRAFT, SUBMITTED, APPROVED)
     */
    List<EquipmentUsageEntry> getByStatus(EntryStatus status);

    /**
     * Get all entries pending approval
     */
    List<EquipmentUsageEntry> getPendingApproval();

    /**
     * Get all approved entries
     */
    List<EquipmentUsageEntry> getApproved();

    /**
     * Get draft entries for specific operator
     */
    List<EquipmentUsageEntry> getDraftsByOperator(Long operatorId);

    /**
     * Get usage entries within date range
     */
    List<EquipmentUsageEntry> getByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get usage entries by department
     */
    List<EquipmentUsageEntry> getByDepartment(String department);

    /**
     * Advanced search with multiple filters
     */
    List<EquipmentUsageEntry> search(Long equipmentId, Long operatorId, LocalDate startDate, LocalDate endDate,
            String department, EntryStatus status);

    /**
     * Create new usage entry (as DRAFT)
     */
    EquipmentUsageEntry createEntry(EquipmentUsageEntry entry);

    /**
     * Save entry as draft
     */
    EquipmentUsageEntry saveDraft(EquipmentUsageEntry entry);

    /**
     * Submit entry for approval (changes status to SUBMITTED)
     */
    EquipmentUsageEntry submitForApproval(Long entryId);

    /**
     * Approve entry (changes status to APPROVED) Only Lab Supervisor/Admin can
     * approve
     */
    EquipmentUsageEntry approveEntry(Long entryId, Long approverId);

    /**
     * Reject entry (reverts to DRAFT)
     */
    EquipmentUsageEntry rejectEntry(Long entryId);

    /**
     * Check if entry can be edited (only DRAFT entries can be edited)
     */
    boolean canEditEntry(Long entryId);

    /**
     * Check if entry can be approved (only SUBMITTED entries can be approved)
     */
    boolean canApproveEntry(Long entryId);
}
