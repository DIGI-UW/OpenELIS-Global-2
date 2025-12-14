package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AssayRunDAOImpl extends BaseDAOImpl<AssayRun, Integer> implements AssayRunDAO {

    public AssayRunDAOImpl() {
        super(AssayRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM AssayRun WHERE sampleId = :sampleId ORDER BY startedAt DESC";
            Query<AssayRun> query = entityManager.unwrap(Session.class).createQuery(hql, AssayRun.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AssayRuns by sampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findByStatus(AssayRun.AssayStatus status) {
        try {
            String hql = "FROM AssayRun WHERE status = :status ORDER BY startedAt DESC";
            Query<AssayRun> query = entityManager.unwrap(Session.class).createQuery(hql, AssayRun.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AssayRuns by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findByOosFlag(boolean oosFlag) {
        try {
            String hql = "FROM AssayRun WHERE oosFlag = :oosFlag ORDER BY startedAt DESC";
            Query<AssayRun> query = entityManager.unwrap(Session.class).createQuery(hql, AssayRun.class);
            query.setParameter("oosFlag", oosFlag);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AssayRuns by oosFlag", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssayRun> findPendingReview() {
        try {
            String hql = "FROM AssayRun WHERE status IN ('SUBMITTED', 'PRIMARY_REVIEW', 'SECONDARY_REVIEW') "
                    + "ORDER BY startedAt ASC";
            Query<AssayRun> query = entityManager.unwrap(Session.class).createQuery(hql, AssayRun.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding pending review AssayRuns", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssayRun findByNotebookPageId(String notebookPageId) {
        try {
            String hql = "FROM AssayRun WHERE notebookPageId = :notebookPageId";
            Query<AssayRun> query = entityManager.unwrap(Session.class).createQuery(hql, AssayRun.class);
            query.setParameter("notebookPageId", notebookPageId);
            query.setMaxResults(1);
            List<AssayRun> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AssayRun by notebookPageId", e);
        }
    }
}
