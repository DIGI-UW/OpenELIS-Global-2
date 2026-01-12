package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.AnalyzerResultImport;
import org.springframework.stereotype.Component;

/** DAO implementation for AnalyzerResultImport entity operations. */
@Component
public class AnalyzerResultImportDAOImpl extends BaseDAOImpl<AnalyzerResultImport, Integer>
        implements AnalyzerResultImportDAO {

    public AnalyzerResultImportDAOImpl() {
        super(AnalyzerResultImport.class);
    }

    @Override
    public List<AnalyzerResultImport> getByNotebookPageId(Integer notebookPageId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM AnalyzerResultImport ari " + "WHERE ari.notebookPage.id = :notebookPageId "
                + "ORDER BY ari.importDate DESC";
        return session.createQuery(hql, AnalyzerResultImport.class).setParameter("notebookPageId", notebookPageId)
                .getResultList();
    }

    @Override
    public AnalyzerResultImport getLatestByNotebookPageId(Integer notebookPageId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM AnalyzerResultImport ari " + "WHERE ari.notebookPage.id = :notebookPageId "
                + "ORDER BY ari.importDate DESC";
        List<AnalyzerResultImport> results = session.createQuery(hql, AnalyzerResultImport.class)
                .setParameter("notebookPageId", notebookPageId).setMaxResults(1).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<AnalyzerResultImport> getByAnalyzerId(Integer analyzerId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM AnalyzerResultImport ari " + "WHERE ari.analyzer.id = :analyzerId "
                + "ORDER BY ari.importDate DESC";
        return session.createQuery(hql, AnalyzerResultImport.class).setParameter("analyzerId", analyzerId)
                .getResultList();
    }

    @Override
    public List<AnalyzerResultImport> getByImportedBy(String userId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM AnalyzerResultImport ari " + "WHERE ari.importedBy.id = :userId "
                + "ORDER BY ari.importDate DESC";
        return session.createQuery(hql, AnalyzerResultImport.class).setParameter("userId", userId).getResultList();
    }

    @Override
    public long getTotalSuccessfulRows(Integer notebookPageId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COALESCE(SUM(ari.successfulRows), 0) FROM AnalyzerResultImport ari "
                + "WHERE ari.notebookPage.id = :notebookPageId";
        return session.createQuery(hql, Long.class).setParameter("notebookPageId", notebookPageId).getSingleResult();
    }

    @Override
    public boolean hasFailedImports(Integer notebookPageId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(ari) FROM AnalyzerResultImport ari " + "WHERE ari.notebookPage.id = :notebookPageId "
                + "AND ari.failedRows > 0";
        Long count = session.createQuery(hql, Long.class).setParameter("notebookPageId", notebookPageId)
                .getSingleResult();
        return count > 0;
    }
}
