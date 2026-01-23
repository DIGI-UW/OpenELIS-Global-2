package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NotebookEntryTemperatureLog;
import org.springframework.stereotype.Repository;

/**
 * DAO implementation for NotebookEntryTemperatureLog operations.
 */
@Repository
public class NotebookEntryTemperatureLogDAOImpl extends BaseDAOImpl<NotebookEntryTemperatureLog, Integer>
        implements NotebookEntryTemperatureLogDAO {

    public NotebookEntryTemperatureLogDAOImpl() {
        super(NotebookEntryTemperatureLog.class);
    }

    @Override
    public List<NotebookEntryTemperatureLog> findByEntryId(Integer entryId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookEntryTemperatureLog t WHERE t.notebookEntry.id = :entryId "
                + "ORDER BY t.checkedDateTime DESC NULLS LAST, t.loggedAt DESC";
        Query<NotebookEntryTemperatureLog> query = session.createQuery(hql, NotebookEntryTemperatureLog.class);
        query.setParameter("entryId", entryId);
        return query.list();
    }

    @Override
    public List<NotebookEntryTemperatureLog> findByEntryIdAndFreezerId(Integer entryId, String freezerId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookEntryTemperatureLog t WHERE t.notebookEntry.id = :entryId "
                + "AND t.freezerId = :freezerId ORDER BY t.checkedDateTime DESC NULLS LAST";
        Query<NotebookEntryTemperatureLog> query = session.createQuery(hql, NotebookEntryTemperatureLog.class);
        query.setParameter("entryId", entryId);
        query.setParameter("freezerId", freezerId);
        return query.list();
    }

    @Override
    public Long countByEntryId(Integer entryId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(t) FROM NotebookEntryTemperatureLog t WHERE t.notebookEntry.id = :entryId";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("entryId", entryId);
        return query.uniqueResult();
    }
}
