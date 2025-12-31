package org.openelisglobal.equipmentusage.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.equipmentusage.dao.EquipmentUsageEntryDAO;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry;
import org.openelisglobal.equipmentusage.valueholder.EquipmentUsageEntry.EntryStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EquipmentUsageEntryDAOImpl extends BaseDAOImpl<EquipmentUsageEntry, Long>
        implements EquipmentUsageEntryDAO {

    public EquipmentUsageEntryDAOImpl() {
        super(EquipmentUsageEntry.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByEquipmentId(Long equipmentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            cq.select(root).where(cb.equal(root.get("equipment").get("id"), equipmentId))
                    .orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage entries by equipment ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByEquipmentAndDateRange(Long equipmentId, LocalDate startDate,
            LocalDate endDate) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

            cq.select(root)
                    .where(cb.and(cb.equal(root.get("equipment").get("id"), equipmentId),
                            cb.greaterThanOrEqualTo(root.get("loginTime"), startDateTime),
                            cb.lessThanOrEqualTo(root.get("loginTime"), endDateTime)))
                    .orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage entries by equipment and date range", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByOperatorId(Long operatorId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            cq.select(root).where(cb.equal(root.get("operator").get("id"), operatorId))
                    .orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage entries by operator ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByStatus(EntryStatus status) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            cq.select(root).where(cb.equal(root.get("entryStatus"), status)).orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage entries by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getPendingApproval() throws LIMSRuntimeException {
        return getByStatus(EntryStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getApproved() throws LIMSRuntimeException {
        return getByStatus(EntryStatus.APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getDraftsByOperator(Long operatorId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            cq.select(root)
                    .where(cb.and(cb.equal(root.get("operator").get("id"), operatorId),
                            cb.equal(root.get("entryStatus"), EntryStatus.DRAFT)))
                    .orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting draft entries by operator", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByDateRange(LocalDate startDate, LocalDate endDate)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

            cq.select(root)
                    .where(cb.and(cb.greaterThanOrEqualTo(root.get("loginTime"), startDateTime),
                            cb.lessThanOrEqualTo(root.get("loginTime"), endDateTime)))
                    .orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage entries by date range", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> getByDepartment(String department) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            cq.select(root).where(cb.equal(root.get("department"), department)).orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage entries by department", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageEntry> search(Long equipmentId, Long operatorId, LocalDate startDate, LocalDate endDate,
            String department, EntryStatus status) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<EquipmentUsageEntry> cq = cb.createQuery(EquipmentUsageEntry.class);
            Root<EquipmentUsageEntry> root = cq.from(EquipmentUsageEntry.class);

            List<Predicate> predicates = new ArrayList<>();

            if (equipmentId != null) {
                predicates.add(cb.equal(root.get("equipment").get("id"), equipmentId));
            }
            if (operatorId != null) {
                predicates.add(cb.equal(root.get("operator").get("id"), operatorId));
            }
            if (startDate != null && endDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                predicates.add(cb.greaterThanOrEqualTo(root.get("loginTime"), startDateTime));
                predicates.add(cb.lessThanOrEqualTo(root.get("loginTime"), endDateTime));
            }
            if (department != null && !department.isEmpty()) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("entryStatus"), status));
            }

            cq.select(root).where(cb.and(predicates.toArray(new Predicate[0]))).orderBy(cb.desc(root.get("loginTime")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching usage entries", e);
        }
    }
}
