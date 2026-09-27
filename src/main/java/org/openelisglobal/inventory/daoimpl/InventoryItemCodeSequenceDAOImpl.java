package org.openelisglobal.inventory.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryItemCodeSequenceDAO;
import org.springframework.stereotype.Component;

@Component
public class InventoryItemCodeSequenceDAOImpl implements InventoryItemCodeSequenceDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long nextValue(String prefix) throws LIMSRuntimeException {
        try {
            // ON CONFLICT: two first-time callers must not both insert the row.
            entityManager
                    .createNativeQuery("INSERT INTO clinlims.inventory_item_code_sequence (prefix, next_value)"
                            + " VALUES (:prefix, 1) ON CONFLICT (prefix) DO NOTHING")
                    .setParameter("prefix", prefix).executeUpdate();
            // UPDATE takes the row lock, so callers for one prefix queue here.
            Number taken = (Number) entityManager
                    .createNativeQuery("UPDATE clinlims.inventory_item_code_sequence"
                            + " SET next_value = next_value + 1 WHERE prefix = :prefix RETURNING next_value - 1")
                    .setParameter("prefix", prefix).getSingleResult();
            return taken.longValue();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error taking the next inventory item code for prefix " + prefix, e);
        }
    }
}
