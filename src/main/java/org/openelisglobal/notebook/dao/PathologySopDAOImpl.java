package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.PathologySop;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for PathologySop entity. Inherits standard CRUD operations
 * from BaseDAOImpl.
 */
@Component
@Transactional
public class PathologySopDAOImpl extends BaseDAOImpl<PathologySop, Integer> implements PathologySopDAO {

    public PathologySopDAOImpl() {
        super(PathologySop.class);
    }

    @Override
    public String getTableName() {
        return "pathology_sop";
    }

    @Override
    public List<PathologySop> getByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM PathologySop s WHERE s.notebookId = :notebookId ORDER BY s.sopTitle";
        return session.createQuery(hql, PathologySop.class).setParameter("notebookId", notebookId).getResultList();
    }

    @Override
    public List<PathologySop> getAllActive() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM PathologySop s WHERE s.status = 'Active' ORDER BY s.sopTitle";
        return session.createQuery(hql, PathologySop.class).getResultList();
    }

    @Override
    public List<PathologySop> getByCategory(String category) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM PathologySop s WHERE s.sopCategory = :category ORDER BY s.sopTitle";
        return session.createQuery(hql, PathologySop.class).setParameter("category", category).getResultList();
    }
}
