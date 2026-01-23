package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;
import org.springframework.stereotype.Repository;

/**
 * DAO implementation for NotebookEntryRoomEnvironmentLog operations.
 */
@Repository
public class NotebookEntryRoomEnvironmentLogDAOImpl extends BaseDAOImpl<NotebookEntryRoomEnvironmentLog, Integer>
        implements NotebookEntryRoomEnvironmentLogDAO {

    public NotebookEntryRoomEnvironmentLogDAOImpl() {
        super(NotebookEntryRoomEnvironmentLog.class);
    }

    @Override
    public List<NotebookEntryRoomEnvironmentLog> findByEntryId(Integer entryId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookEntryRoomEnvironmentLog r WHERE r.notebookEntry.id = :entryId "
                + "ORDER BY r.checkedDateTime DESC NULLS LAST, r.loggedAt DESC";
        Query<NotebookEntryRoomEnvironmentLog> query = session.createQuery(hql, NotebookEntryRoomEnvironmentLog.class);
        query.setParameter("entryId", entryId);
        return query.list();
    }

    @Override
    public List<NotebookEntryRoomEnvironmentLog> findByEntryIdAndRoomId(Integer entryId, String roomId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookEntryRoomEnvironmentLog r WHERE r.notebookEntry.id = :entryId "
                + "AND r.roomId = :roomId ORDER BY r.checkedDateTime DESC NULLS LAST";
        Query<NotebookEntryRoomEnvironmentLog> query = session.createQuery(hql, NotebookEntryRoomEnvironmentLog.class);
        query.setParameter("entryId", entryId);
        query.setParameter("roomId", roomId);
        return query.list();
    }

    @Override
    public Long countByEntryId(Integer entryId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(r) FROM NotebookEntryRoomEnvironmentLog r WHERE r.notebookEntry.id = :entryId";
        Query<Long> query = session.createQuery(hql, Long.class);
        query.setParameter("entryId", entryId);
        return query.uniqueResult();
    }
}
