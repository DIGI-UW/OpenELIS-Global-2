package org.openelisglobal.qaevent.daoimpl;

import java.util.List;
import jakarta.persistence.TypedQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.qaevent.dao.NceHistoryDAO;
import org.openelisglobal.qaevent.valueholder.NceHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceHistoryDAOImpl extends BaseDAOImpl<NceHistory, Integer> implements NceHistoryDAO {

    public NceHistoryDAOImpl() {
        super(NceHistory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceHistory> findByNceId(Integer nceId) {
        String jpql = "FROM NceHistory h WHERE h.nceId = :nceId ORDER BY h.timestamp DESC";
        TypedQuery<NceHistory> query = entityManager.createQuery(jpql, NceHistory.class);
        query.setParameter("nceId", nceId);
        return query.getResultList();
    }
}
