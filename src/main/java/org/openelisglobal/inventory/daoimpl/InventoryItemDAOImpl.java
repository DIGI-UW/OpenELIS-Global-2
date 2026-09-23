package org.openelisglobal.inventory.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryItemDAOImpl extends BaseDAOImpl<InventoryItem, Long> implements InventoryItemDAO {

    public InventoryItemDAOImpl() {
        super(InventoryItem.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTags() throws LIMSRuntimeException {
        try {
            // The collection table has no entity of its own, so this reads it
            // directly. Whatever is in use is the whole suggestion list: there
            // is no curated directory to draw one from.
            @SuppressWarnings("unchecked")
            List<String> results = entityManager
                    .createNativeQuery("SELECT DISTINCT tag FROM clinlims.inventory_item_tag ORDER BY tag")
                    .getResultList();
            return results;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory item tags", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTagsMatching(Collection<String> canonicalKeys) throws LIMSRuntimeException {
        if (canonicalKeys == null || canonicalKeys.isEmpty()) {
            return List.of();
        }
        try {
            // The key is built in SQL the same way tagKey() builds it in Java:
            // trimmed, inner whitespace collapsed, lower case. Anything looser
            // would return a spelling the service would then not recognise.
            @SuppressWarnings("unchecked")
            List<String> results = entityManager
                    .createNativeQuery("SELECT DISTINCT tag FROM clinlims.inventory_item_tag"
                            + " WHERE regexp_replace(lower(trim(tag)), '\\s+', ' ', 'g') IN (:keys)")
                    .setParameter("keys", canonicalKeys).getResultList();
            return results;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting matching inventory item tags", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllActive() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.equal(root.get("isActive"), "Y")).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all active inventory items", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByCategory(String category) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.and(cb.equal(root.get("category"), category), cb.equal(root.get("isActive"), "Y")))
                    .orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory items by category", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> searchByName(String name) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.and(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"),
                    cb.equal(root.get("isActive"), "Y"))).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching inventory items by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByCode(String code) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.equal(root.get("code"), code));

            List<InventoryItem> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory item by code", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByFhirUuid(String fhirUuid) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.equal(root.get("fhirUuid"), java.util.UUID.fromString(fhirUuid)));

            List<InventoryItem> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory item by FHIR UUID", e);
        }
    }

}
