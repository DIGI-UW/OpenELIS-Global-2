package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for NoteBookPage entity. Inherits standard CRUD operations
 * from BaseDAOImpl.
 */
@Component
@Transactional
public class NoteBookPageDAOImpl extends BaseDAOImpl<NoteBookPage, Integer> implements NoteBookPageDAO {

    public NoteBookPageDAOImpl() {
        super(NoteBookPage.class);
    }

    @Override
    public String getTableName() {
        return "notebook_page";
    }

    @Override
    public List<NoteBookPage> getByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NoteBookPage p WHERE p.notebook.id = :notebookId ORDER BY p.order";
        return session.createQuery(hql, NoteBookPage.class).setParameter("notebookId", notebookId).getResultList();
    }

    @Override
    public Integer getPageIdByNotebookIdAndOrder(Integer notebookId, Integer pageOrder) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT p.id FROM NoteBookPage p WHERE p.notebook.id = :notebookId AND p.order = :pageOrder";
        List<Integer> results = session.createQuery(hql, Integer.class).setParameter("notebookId", notebookId)
                .setParameter("pageOrder", pageOrder).setMaxResults(1).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Integer getPageIdByNotebookIdAndTitlePattern(Integer notebookId, String titlePattern) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT p.id FROM NoteBookPage p WHERE p.notebook.id = :notebookId AND p.title LIKE :titlePattern";
        List<Integer> results = session.createQuery(hql, Integer.class).setParameter("notebookId", notebookId)
                .setParameter("titlePattern", "%" + titlePattern + "%").setMaxResults(1).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
