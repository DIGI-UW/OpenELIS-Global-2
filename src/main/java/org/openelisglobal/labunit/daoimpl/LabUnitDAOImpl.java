package org.openelisglobal.labunit.daoimpl;

import jakarta.persistence.Query;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.labunit.dao.LabUnitDAO;
import org.openelisglobal.labunit.valueholder.LabUnit;
import org.openelisglobal.labunit.valueholder.LabUnitAssignment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clean DAO implementation for Lab Unit operations. Uses JPA directly with
 * proper EntityManager.
 */
@Component
public class LabUnitDAOImpl extends BaseDAOImpl<LabUnit, String> implements LabUnitDAO {

    public LabUnitDAOImpl() {
        super(LabUnit.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnit> getAllLabUnits() {
        try {
            Query query = entityManager.createQuery("SELECT lu FROM LabUnit lu ORDER BY lu.sortOrder", LabUnit.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving all lab units", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnit> getActiveLabUnits() {
        try {
            Query query = entityManager.createQuery(
                    "SELECT lu FROM LabUnit lu WHERE lu.isActive = 'Y' ORDER BY lu.sortOrder", LabUnit.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving active lab units", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnit> getInactiveLabUnits() {
        try {
            Query query = entityManager.createQuery(
                    "SELECT lu FROM LabUnit lu WHERE lu.isActive = 'N' ORDER BY lu.sortOrder", LabUnit.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving inactive lab units", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LabUnit getLabUnitById(String id) {
        try {
            LabUnit labUnit = entityManager.unwrap(org.hibernate.Session.class).get(LabUnit.class, id);
            return labUnit;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving lab unit by id: " + id, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LabUnit getLabUnitByName(String name) {
        try {
            Query query = entityManager.createQuery("SELECT lu FROM LabUnit lu WHERE lu.name = :name", LabUnit.class);
            query.setParameter("name", name);
            try {
                return (LabUnit) query.getSingleResult();
            } catch (Exception e) {
                return null; // Return null if no result found
            }
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving lab unit by name: " + name, e);
        }
    }

    // Assignment operations
    @Override
    @Transactional(readOnly = true)
    public List<LabUnitAssignment> getAssignmentsForLabUnit(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                    "SELECT lua FROM LabUnitAssignment lua WHERE lua.labUnitId = :labUnitId", LabUnitAssignment.class);
            query.setParameter("labUnitId", labUnitId);
            return query.getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving assignments for lab unit: " + labUnitId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAssignments(String labUnitId) {
        try {
            Query query = entityManager
                    .createQuery("SELECT COUNT(lua) FROM LabUnitAssignment lua WHERE lua.labUnitId = :labUnitId");
            query.setParameter("labUnitId", labUnitId);
            Long count = (Long) query.getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking assignments for lab unit: " + labUnitId, e);
        }
    }

    // Count operations
    @Override
    @Transactional(readOnly = true)
    public int getTotalLabUnitCount() {
        try {
            Query query = entityManager.createQuery("SELECT COUNT(lu) FROM LabUnit lu");
            return ((Number) query.getSingleResult()).intValue();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting total lab unit count", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getActiveLabUnitCount() {
        try {
            Query query = entityManager.createQuery("SELECT COUNT(lu) FROM LabUnit lu WHERE lu.isActive = 'Y'");
            return ((Number) query.getSingleResult()).intValue();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting active lab unit count", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getInactiveLabUnitCount() {
        try {
            Query query = entityManager.createQuery("SELECT COUNT(lu) FROM LabUnit lu WHERE lu.isActive = 'N'");
            return ((Number) query.getSingleResult()).intValue();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inactive lab unit count", e);
        }
    }

    // Search and filtering
    @Override
    @Transactional(readOnly = true)
    public List<LabUnit> getLabUnitsByNameFilter(String filter) {
        try {
            Query query;
            if (filter != null && !filter.trim().isEmpty()) {
                query = entityManager.createQuery(
                        "SELECT lu FROM LabUnit lu WHERE lu.name LIKE :filter ORDER BY lu.sortOrder", LabUnit.class);
                query.setParameter("filter", "%" + filter + "%");
            } else {
                query = entityManager.createQuery("SELECT lu FROM LabUnit lu ORDER BY lu.sortOrder", LabUnit.class);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving lab units with filter: " + filter, e);
        }
    }

    // Ordering operations
    @Override
    @Transactional
    public void updateSortOrderForMultiple(List<LabUnit> labUnits) {
        try {
            for (int i = 0; i < labUnits.size(); i++) {
                LabUnit labUnit = labUnits.get(i);
                labUnit.setSortOrder(i + 1);
                entityManager.merge(labUnit);
            }
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error updating sort order for lab units", e);
        }
    }

    // Validation operations
    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicateName(String name, String excludeId) {
        try {
            jakarta.persistence.criteria.CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            jakarta.persistence.criteria.CriteriaQuery<Long> query = cb.createQuery(Long.class);
            jakarta.persistence.criteria.Root<LabUnit> root = query.from(LabUnit.class);

            if (excludeId != null && !excludeId.trim().isEmpty()) {
                query.select(cb.count(root));
                query.where(cb.and(cb.equal(root.get("name"), name), cb.notEqual(root.get("id"), excludeId)));
            } else {
                query.select(cb.count(root));
                query.where(cb.equal(root.get("name"), name));
            }

            Long count = entityManager.createQuery(query).getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking duplicate lab unit name: " + name, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicateCode(String code, String excludeId) {
        try {
            if (code == null || code.trim().isEmpty()) {
                return false; // Empty code is considered unique
            }

            jakarta.persistence.criteria.CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            jakarta.persistence.criteria.CriteriaQuery<Long> query = cb.createQuery(Long.class);
            jakarta.persistence.criteria.Root<LabUnit> root = query.from(LabUnit.class);

            if (excludeId != null && !excludeId.trim().isEmpty()) {
                query.select(cb.count(root));
                query.where(cb.and(cb.equal(root.get("code"), code), cb.notEqual(root.get("id"), excludeId)));
            } else {
                query.select(cb.count(root));
                query.where(cb.equal(root.get("code"), code));
            }

            Long count = entityManager.createQuery(query).getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking duplicate lab unit code: " + code, e);
        }
    }
}