package org.openelisglobal.equipmentusage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.equipmentusage.dao.EquipmentUsageEntryDAO;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry.EntryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentUsageEntryServiceImpl extends AuditableBaseObjectServiceImpl<EquipmentUsageEntry, Long>
        implements EquipmentUsageEntryService {

    @Autowired
    private EquipmentUsageEntryDAO equipmentUsageEntryDAO;

    public EquipmentUsageEntryServiceImpl() {
        super(EquipmentUsageEntry.class);
        this.auditTrailLog = true;
    }

    @Override
    protected EquipmentUsageEntryDAO getBaseObjectDAO() {
        return equipmentUsageEntryDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByEquipmentId(Long equipmentId) {
        return equipmentUsageEntryDAO.getByEquipmentId(equipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByEquipmentAndDateRange(Long equipmentId, LocalDate startDate,
            LocalDate endDate) {
        return equipmentUsageEntryDAO.getByEquipmentAndDateRange(equipmentId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByOperatorId(Long operatorId) {
        return equipmentUsageEntryDAO.getByOperatorId(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByStatus(EntryStatus status) {
        return equipmentUsageEntryDAO.getByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getPendingApproval() {
        return equipmentUsageEntryDAO.getPendingApproval();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getApproved() {
        return equipmentUsageEntryDAO.getApproved();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getDraftsByOperator(Long operatorId) {
        return equipmentUsageEntryDAO.getDraftsByOperator(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return equipmentUsageEntryDAO.getByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByDepartment(String department) {
        return equipmentUsageEntryDAO.getByDepartment(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> search(Long equipmentId, Long operatorId, LocalDate startDate, LocalDate endDate,
            String department, EntryStatus status) {
        return equipmentUsageEntryDAO.search(equipmentId, operatorId, startDate, endDate, department, status);
    }

    @Override
    @Transactional
    public EquipmentUsageEntry createEntry(EquipmentUsageEntry entry) {
        entry.setEntryStatus(EntryStatus.DRAFT);
        entry.setCreatedDate(LocalDateTime.now());
        entry.setModifiedDate(LocalDateTime.now());
        entry.setLastupdatedFields(); // Set the BaseObject lastupdated field
        Long id = (Long) super.insert(entry);
        return get(id);
    }

    @Override
    @Transactional
    public EquipmentUsageEntry saveDraft(EquipmentUsageEntry entry) {
        if (entry.getId() == null) {
            entry.setEntryStatus(EntryStatus.DRAFT);
            entry.setCreatedDate(LocalDateTime.now());
        }
        entry.setModifiedDate(LocalDateTime.now());
        entry.setLastupdatedFields(); // Set the BaseObject lastupdated field
        return update(entry);
    }

    @Override
    @Transactional
    public EquipmentUsageEntry submitForApproval(Long entryId) {
        EquipmentUsageEntry entry = get(entryId);
        if (entry.getEntryStatus() == EntryStatus.DRAFT) {
            entry.setEntryStatus(EntryStatus.SUBMITTED);
            entry.setModifiedDate(LocalDateTime.now());
            entry.setLastupdatedFields(); // Set the BaseObject lastupdated field
            return update(entry);
        }
        return entry;
    }

    @Override
    @Transactional
    public EquipmentUsageEntry approveEntry(Long entryId, Long approverId) {
        EquipmentUsageEntry entry = get(entryId);
        if (entry.getEntryStatus() == EntryStatus.SUBMITTED) {
            entry.setEntryStatus(EntryStatus.APPROVED);
            entry.setApprovalDate(LocalDateTime.now());
            // Set approval signature as username + timestamp (digital signature)
            entry.setApprovalSignature("Approved at " + LocalDateTime.now());
            entry.setModifiedDate(LocalDateTime.now());
            return update(entry);
        }
        return entry;
    }

    @Override
    @Transactional
    public EquipmentUsageEntry rejectEntry(Long entryId) {
        EquipmentUsageEntry entry = get(entryId);
        if (entry.getEntryStatus() == EntryStatus.SUBMITTED) {
            entry.setEntryStatus(EntryStatus.DRAFT);
            entry.setApprovedBy(null);
            entry.setApprovalDate(null);
            entry.setApprovalSignature(null);
            entry.setModifiedDate(LocalDateTime.now());
            return update(entry);
        }
        return entry;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditEntry(Long entryId) {
        EquipmentUsageEntry entry = get(entryId);
        return entry.getEntryStatus() == EntryStatus.DRAFT;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canApproveEntry(Long entryId) {
        EquipmentUsageEntry entry = get(entryId);
        return entry.getEntryStatus() == EntryStatus.SUBMITTED;
    }
}
