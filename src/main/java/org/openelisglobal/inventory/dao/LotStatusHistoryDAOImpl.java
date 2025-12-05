package org.openelisglobal.inventory.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.LotStatusHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class LotStatusHistoryDAOImpl extends BaseDAOImpl<LotStatusHistory, String> implements LotStatusHistoryDAO {

    public LotStatusHistoryDAOImpl() {
        super(LotStatusHistory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotStatusHistory> findByLotId(String lotId) {
        try {
            String hql = "FROM LotStatusHistory h WHERE h.lot.id = :lotId ORDER BY h.changedDate DESC";
            Query<LotStatusHistory> query = entityManager.unwrap(Session.class).createQuery(hql,
                    LotStatusHistory.class);
            query.setParameter("lotId", lotId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding status history by lot ID", e);
        }
    }
}
