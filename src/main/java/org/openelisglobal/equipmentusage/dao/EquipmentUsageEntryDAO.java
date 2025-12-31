package org.openelisglobal.equipmentusage.dao;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry.EntryStatus;

public interface EquipmentUsageEntryDAO extends BaseDAO<EquipmentUsageEntry, Long> {

    /**
     * Get usage entries by equipment ID
     */
    List<EquipmentUsageEntry> getByEquipmentId(Long equipmentId) throws LIMSRuntimeException;

    /**
     * Get usage entries by equipment ID within date range
     */
    List<EquipmentUsageEntry> getByEquipmentAndDateRange(Long equipmentId, LocalDate startDate, LocalDate endDate)
            throws LIMSRuntimeException;

    /**
     * Get usage entries by operator ID
     */
    List<EquipmentUsageEntry> getByOperatorId(Long operatorId) throws LIMSRuntimeException;

    /**
     * Get usage entries by status
     */
    List<EquipmentUsageEntry> getByStatus(EntryStatus status) throws LIMSRuntimeException;

    /**
     * Get usage entries pending approval
     */
    List<EquipmentUsageEntry> getPendingApproval() throws LIMSRuntimeException;

    /**
     * Get approved usage entries
     */
    List<EquipmentUsageEntry> getApproved() throws LIMSRuntimeException;

    /**
     * Get draft entries for a specific operator
     */
    List<EquipmentUsageEntry> getDraftsByOperator(Long operatorId) throws LIMSRuntimeException;

    /**
     * Get usage entries by date range
     */
    List<EquipmentUsageEntry> getByDateRange(LocalDate startDate, LocalDate endDate) throws LIMSRuntimeException;

    /**
     * Get usage entries by department
     */
    List<EquipmentUsageEntry> getByDepartment(String department) throws LIMSRuntimeException;

    /**
     * Search usage entries (complex query support)
     */
    List<EquipmentUsageEntry> search(Long equipmentId, Long operatorId, LocalDate startDate, LocalDate endDate,
            String department, EntryStatus status) throws LIMSRuntimeException;
}
