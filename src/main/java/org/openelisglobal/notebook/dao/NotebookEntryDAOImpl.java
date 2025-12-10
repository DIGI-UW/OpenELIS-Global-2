package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntry.EntryStatus;
import org.springframework.stereotype.Repository;

/**
 * DAO implementation for NotebookEntry.
 */
@Repository
public class NotebookEntryDAOImpl extends BaseDAOImpl<NotebookEntry, Integer> implements NotebookEntryDAO {

    public NotebookEntryDAOImpl() {
        super(NotebookEntry.class);
    }

    @Override
    public List<NotebookEntry> findByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        Query<NotebookEntry> query = session.createQuery(
                "FROM NotebookEntry e WHERE e.notebook.id = :notebookId ORDER BY e.dateCreated DESC",
                NotebookEntry.class);
        query.setParameter("notebookId", notebookId);
        return query.list();
    }

    @Override
    public List<NotebookEntry> findByStatus(EntryStatus status) {
        Session session = entityManager.unwrap(Session.class);
        Query<NotebookEntry> query = session.createQuery(
                "FROM NotebookEntry e WHERE e.status = :status ORDER BY e.dateCreated DESC", NotebookEntry.class);
        query.setParameter("status", status);
        return query.list();
    }

    @Override
    public List<NotebookEntry> findByTechnicianId(Integer technicianId) {
        Session session = entityManager.unwrap(Session.class);
        Query<NotebookEntry> query = session.createQuery(
                "FROM NotebookEntry e WHERE e.technician.id = :technicianId ORDER BY e.dateCreated DESC",
                NotebookEntry.class);
        query.setParameter("technicianId", technicianId);
        return query.list();
    }

    @Override
    public List<NotebookEntry> findByNotebookIdAndStatus(Integer notebookId, EntryStatus status) {
        Session session = entityManager.unwrap(Session.class);
        Query<NotebookEntry> query = session.createQuery(
                "FROM NotebookEntry e WHERE e.notebook.id = :notebookId AND e.status = :status ORDER BY e.dateCreated DESC",
                NotebookEntry.class);
        query.setParameter("notebookId", notebookId);
        query.setParameter("status", status);
        return query.list();
    }

    @Override
    public Long countByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        Query<Long> query = session
                .createQuery("SELECT COUNT(e) FROM NotebookEntry e WHERE e.notebook.id = :notebookId", Long.class);
        query.setParameter("notebookId", notebookId);
        return query.uniqueResult();
    }

    @Override
    public Long countByStatus(EntryStatus status) {
        Session session = entityManager.unwrap(Session.class);
        Query<Long> query = session.createQuery("SELECT COUNT(e) FROM NotebookEntry e WHERE e.status = :status",
                Long.class);
        query.setParameter("status", status);
        return query.uniqueResult();
    }
}
