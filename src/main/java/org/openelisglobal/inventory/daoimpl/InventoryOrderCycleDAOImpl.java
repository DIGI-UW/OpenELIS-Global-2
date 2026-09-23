package org.openelisglobal.inventory.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryOrderCycleDAO;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryOrderCycleDAOImpl extends BaseDAOImpl<InventoryOrderCycle, Long>
        implements InventoryOrderCycleDAO {

    public InventoryOrderCycleDAOImpl() {
        super(InventoryOrderCycle.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryOrderCycle> getReceivedSince(Timestamp cutoff) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryOrderCycle c WHERE c.receivedAt >= :cutoff ORDER BY c.receivedAt DESC";
            Query<InventoryOrderCycle> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryOrderCycle.class);
            query.setParameter("cutoff", cutoff);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory order cycles", e);
        }
    }
}
