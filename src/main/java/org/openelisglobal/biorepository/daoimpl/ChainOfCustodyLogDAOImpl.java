package org.openelisglobal.biorepository.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.ChainOfCustodyLogDAO;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for ChainOfCustodyLog entity operations.
 */
@Component
public class ChainOfCustodyLogDAOImpl extends BaseDAOImpl<ChainOfCustodyLog, Integer> implements ChainOfCustodyLogDAO {

    public ChainOfCustodyLogDAOImpl() {
        super(ChainOfCustodyLog.class);
    }

    @Override
    public List<ChainOfCustodyLog> getBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM ChainOfCustodyLog c WHERE c.sampleItem.id = :sampleItemId "
                + "ORDER BY c.actionTimestamp ASC";
        return session.createQuery(hql, ChainOfCustodyLog.class).setParameter("sampleItemId", sampleItemId)
                .getResultList();
    }

    @Override
    public List<ChainOfCustodyLog> getByTransferInRequestId(Integer transferInRequestId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM ChainOfCustodyLog c WHERE c.transferInRequest.id = :transferInRequestId "
                + "ORDER BY c.actionTimestamp ASC";
        return session.createQuery(hql, ChainOfCustodyLog.class)
                .setParameter("transferInRequestId", transferInRequestId).getResultList();
    }

    @Override
    public List<ChainOfCustodyLog> getByRetrievalRequestId(Integer retrievalRequestId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM ChainOfCustodyLog c WHERE c.retrievalRequest.id = :retrievalRequestId "
                + "ORDER BY c.actionTimestamp ASC";
        return session.createQuery(hql, ChainOfCustodyLog.class).setParameter("retrievalRequestId", retrievalRequestId)
                .getResultList();
    }

    @Override
    public List<ChainOfCustodyLog> getByAction(CustodyAction action) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM ChainOfCustodyLog c WHERE c.custodyAction = :action " + "ORDER BY c.actionTimestamp DESC";
        return session.createQuery(hql, ChainOfCustodyLog.class).setParameter("action", action).getResultList();
    }

    @Override
    public List<ChainOfCustodyLog> getRecentActions(int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM ChainOfCustodyLog c ORDER BY c.actionTimestamp DESC";
        return session.createQuery(hql, ChainOfCustodyLog.class).setMaxResults(limit).getResultList();
    }

    @Override
    public List<ChainOfCustodyLog> getFullLifecycle(Integer sampleItemId, Integer transferInRequestId,
            Integer retrievalRequestId) {
        Session session = entityManager.unwrap(Session.class);

        StringBuilder hql = new StringBuilder("FROM ChainOfCustodyLog c WHERE c.sampleItem.id = :sampleItemId");

        if (transferInRequestId != null || retrievalRequestId != null) {
            hql.append(" AND (");
            boolean needOr = false;
            if (transferInRequestId != null) {
                hql.append("c.transferInRequest.id = :transferInRequestId");
                needOr = true;
            }
            if (retrievalRequestId != null) {
                if (needOr) {
                    hql.append(" OR ");
                }
                hql.append("c.retrievalRequest.id = :retrievalRequestId");
            }
            hql.append(")");
        }

        hql.append(" ORDER BY c.actionTimestamp ASC");

        var query = session.createQuery(hql.toString(), ChainOfCustodyLog.class).setParameter("sampleItemId",
                sampleItemId);

        if (transferInRequestId != null) {
            query.setParameter("transferInRequestId", transferInRequestId);
        }
        if (retrievalRequestId != null) {
            query.setParameter("retrievalRequestId", retrievalRequestId);
        }

        return query.getResultList();
    }

    @Override
    public long countByAction(CustodyAction action) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(c) FROM ChainOfCustodyLog c WHERE c.custodyAction = :action";
        return session.createQuery(hql, Long.class).setParameter("action", action).getSingleResult();
    }

    @Override
    public List<ChainOfCustodyLog> searchCustodyLogs(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate, int page, int pageSize) {
        Session session = entityManager.unwrap(Session.class);

        StringBuilder hql = new StringBuilder(
                "FROM ChainOfCustodyLog c LEFT JOIN FETCH c.sampleItem si LEFT JOIN FETCH si.sample s "
                        + "LEFT JOIN FETCH c.toCustodian WHERE 1=1");

        if (sampleExternalId != null && !sampleExternalId.trim().isEmpty()) {
            hql.append(" AND (si.externalId LIKE :externalId OR s.accessionNumber LIKE :externalId)");
        }
        if (action != null) {
            hql.append(" AND c.custodyAction = :action");
        }
        if (custodianId != null) {
            hql.append(" AND (c.toCustodian.id = :custodianId OR c.fromCustodian.id = :custodianId)");
        }
        if (startDate != null) {
            hql.append(" AND c.actionTimestamp >= :startDate");
        }
        if (endDate != null) {
            hql.append(" AND c.actionTimestamp <= :endDate");
        }

        hql.append(" ORDER BY c.actionTimestamp DESC");

        var query = session.createQuery(hql.toString(), ChainOfCustodyLog.class);

        if (sampleExternalId != null && !sampleExternalId.trim().isEmpty()) {
            query.setParameter("externalId", "%" + sampleExternalId.trim() + "%");
        }
        if (action != null) {
            query.setParameter("action", action);
        }
        if (custodianId != null) {
            query.setParameter("custodianId", custodianId);
        }
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }

        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    @Override
    public long countCustodyLogs(String sampleExternalId, CustodyAction action, Integer custodianId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate) {
        Session session = entityManager.unwrap(Session.class);

        StringBuilder hql = new StringBuilder(
                "SELECT COUNT(c) FROM ChainOfCustodyLog c LEFT JOIN c.sampleItem si LEFT JOIN si.sample s WHERE 1=1");

        if (sampleExternalId != null && !sampleExternalId.trim().isEmpty()) {
            hql.append(" AND (si.externalId LIKE :externalId OR s.accessionNumber LIKE :externalId)");
        }
        if (action != null) {
            hql.append(" AND c.custodyAction = :action");
        }
        if (custodianId != null) {
            hql.append(" AND (c.toCustodian.id = :custodianId OR c.fromCustodian.id = :custodianId)");
        }
        if (startDate != null) {
            hql.append(" AND c.actionTimestamp >= :startDate");
        }
        if (endDate != null) {
            hql.append(" AND c.actionTimestamp <= :endDate");
        }

        var query = session.createQuery(hql.toString(), Long.class);

        if (sampleExternalId != null && !sampleExternalId.trim().isEmpty()) {
            query.setParameter("externalId", "%" + sampleExternalId.trim() + "%");
        }
        if (action != null) {
            query.setParameter("action", action);
        }
        if (custodianId != null) {
            query.setParameter("custodianId", custodianId);
        }
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }

        return query.getSingleResult();
    }
}
