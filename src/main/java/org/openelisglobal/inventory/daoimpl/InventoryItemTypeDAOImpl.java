package org.openelisglobal.inventory.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryItemTypeDAO;
import org.openelisglobal.inventory.valueholder.InventoryItemType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryItemTypeDAOImpl extends BaseDAOImpl<InventoryItemType, Long> implements InventoryItemTypeDAO {

    public InventoryItemTypeDAOImpl() {
        super(InventoryItemType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemType> getAllOrderedBySortOrder() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItemType> cq = cb.createQuery(InventoryItemType.class);
            Root<InventoryItemType> root = cq.from(InventoryItemType.class);
            cq.select(root).orderBy(cb.asc(root.get("sortOrder")), cb.asc(root.get("code")));
            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory item types", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemType> getAllActiveOrderedBySortOrder() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItemType> cq = cb.createQuery(InventoryItemType.class);
            Root<InventoryItemType> root = cq.from(InventoryItemType.class);
            cq.select(root).where(cb.isTrue(root.get("isActive"))).orderBy(cb.asc(root.get("sortOrder")),
                    cb.asc(root.get("code")));
            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting active inventory item types", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemType getByCode(String code) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItemType> cq = cb.createQuery(InventoryItemType.class);
            Root<InventoryItemType> root = cq.from(InventoryItemType.class);
            cq.select(root).where(cb.equal(root.get("code"), code));
            List<InventoryItemType> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory item type by code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) throws LIMSRuntimeException {
        return getByCode(code) != null;
    }
}
