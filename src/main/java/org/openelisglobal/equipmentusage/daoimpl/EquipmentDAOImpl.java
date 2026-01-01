package org.openelisglobal.equipmentusage.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.equipmentusage.dao.EquipmentDAO;
import org.openelisglobal.equipmentusage.valueholder.Equipment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EquipmentDAOImpl extends BaseDAOImpl<Equipment, Long> implements EquipmentDAO {

    public EquipmentDAOImpl() {
        super(Equipment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAll() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Equipment> cq = cb.createQuery(Equipment.class);
            Root<Equipment> root = cq.from(Equipment.class);

            cq.select(root).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all equipment", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAllActive() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Equipment> cq = cb.createQuery(Equipment.class);
            Root<Equipment> root = cq.from(Equipment.class);

            cq.select(root).where(cb.equal(root.get("isActive"), "Y")).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all active equipment", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Equipment> getBySerialNumber(String serialNumber) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Equipment> cq = cb.createQuery(Equipment.class);
            Root<Equipment> root = cq.from(Equipment.class);

            cq.select(root).where(cb.equal(root.get("serialNumber"), serialNumber));

            List<Equipment> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting equipment by serial number", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> searchByName(String name) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Equipment> cq = cb.createQuery(Equipment.class);
            Root<Equipment> root = cq.from(Equipment.class);

            cq.select(root).where(cb.and(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"),
                    cb.equal(root.get("isActive"), "Y"))).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching equipment by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getByDepartment(String department) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Equipment> cq = cb.createQuery(Equipment.class);
            Root<Equipment> root = cq.from(Equipment.class);

            cq.select(root)
                    .where(cb.and(cb.equal(root.get("department"), department), cb.equal(root.get("isActive"), "Y")))
                    .orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting equipment by department", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentForDropdown() throws LIMSRuntimeException {
        return getAllActive();
    }
}
