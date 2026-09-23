package org.openelisglobal.inventory.daoimpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryTagDAO;
import org.openelisglobal.inventory.valueholder.InventoryTag;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryTagDAOImpl extends BaseDAOImpl<InventoryTag, Long> implements InventoryTagDAO {

    public InventoryTagDAOImpl() {
        super(InventoryTag.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTag> getAllTags() throws LIMSRuntimeException {
        try {
            return entityManager.unwrap(Session.class)
                    .createQuery("FROM InventoryTag t ORDER BY t.name", InventoryTag.class).list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory tag directory", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryTag getByName(String name) throws LIMSRuntimeException {
        try {
            Query<InventoryTag> query = entityManager.unwrap(Session.class)
                    .createQuery("FROM InventoryTag t WHERE lower(t.name) = :name", InventoryTag.class);
            query.setParameter("name", name == null ? null : name.trim().toLowerCase());
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory tag by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countItemsPerTag() throws LIMSRuntimeException {
        try {
            // One grouped read over the collection table. The alternative — counting per
            // tag — turns a directory of twenty tags into twenty queries.
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager
                    .createNativeQuery("SELECT t.tag, COUNT(*) FROM clinlims.inventory_item_tag t"
                            + " JOIN clinlims.inventory_item i ON i.id = t.item_id"
                            + " WHERE i.is_active = 'Y' GROUP BY t.tag")
                    .getResultList();
            Map<String, Long> counts = new HashMap<>();
            for (Object[] row : rows) {
                counts.put((String) row[0], ((Number) row[1]).longValue());
            }
            return counts;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting items per inventory tag", e);
        }
    }
}
