package org.openelisglobal.virology.daoimpl;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.virology.dao.VirusCultureBatchDAO;
import org.openelisglobal.virology.valueholder.VirusCultureBatch;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for VirusCultureBatch entity
 */
@Component
@Transactional
public class VirusCultureBatchDAOImpl extends BaseDAOImpl<VirusCultureBatch, Integer> implements VirusCultureBatchDAO {

    public VirusCultureBatchDAOImpl() {
        super(VirusCultureBatch.class);
    }

    @Override
    @Transactional(readOnly = true)
    public VirusCultureBatch findByBatchId(String batchId) {
        if (batchId == null) {
            return null;
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.batchId = :batchId";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("batchId", batchId);
        query.setMaxResults(1);

        List<VirusCultureBatch> results = query.getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findByNotebookPageSample(NotebookPageSample notebookPageSample) {
        if (notebookPageSample == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.notebookPageSample = :notebookPageSample ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("notebookPageSample", notebookPageSample);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findByNotebookPageSampleId(Integer notebookPageSampleId) {
        if (notebookPageSampleId == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.notebookPageSample.id = :notebookPageSampleId ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("notebookPageSampleId", notebookPageSampleId);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findByStatus(VirusCultureBatch.BatchStatus status) {
        if (status == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.status = :status ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("status", status.name());

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findActiveBatches() {
        String hql = """
            FROM VirusCultureBatch vcb
            WHERE vcb.status NOT IN (:completedStatuses)
            ORDER BY vcb.createdDate DESC
            """;
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("completedStatuses", List.of(
                VirusCultureBatch.BatchStatus.WORKFLOW_COMPLETE,
                VirusCultureBatch.BatchStatus.FAILED,
                VirusCultureBatch.BatchStatus.CANCELLED
        ));

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findByVirusStrain(String virusStrain) {
        if (virusStrain == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.virusStrain = :virusStrain ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("virusStrain", virusStrain);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findByCellLine(String cellLine) {
        if (cellLine == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.cellLineUsed = :cellLine ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("cellLine", cellLine);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findByCreatedBy(Integer userId) {
        if (userId == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.createdBy.id = :userId ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("userId", userId);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(VirusCultureBatch.BatchStatus status) {
        if (status == null) {
            return 0;
        }

        String hql = "SELECT COUNT(vcb) FROM VirusCultureBatch vcb WHERE vcb.status = :status";
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery(hql, Long.class);
        query.setParameter("status", status.name());

        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findBatchesRequiringAttention() {
        String hql = """
            FROM VirusCultureBatch vcb
            WHERE vcb.status IN (:attentionStatuses)
            OR EXISTS (
                SELECT 1 FROM VirusCultureWorkflowStatus vws
                WHERE vws.cultureBatch = vcb
                AND vws.status = 'FAILED'
            )
            ORDER BY vcb.createdDate DESC
            """;
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("attentionStatuses", List.of(
                VirusCultureBatch.BatchStatus.FAILED
        ));

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureBatch> findRecentBatches(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
        Timestamp cutoffDate = new Timestamp(cutoffTime);

        String hql = "FROM VirusCultureBatch vcb WHERE vcb.createdDate >= :cutoffDate ORDER BY vcb.createdDate DESC";
        Query<VirusCultureBatch> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureBatch.class);
        query.setParameter("cutoffDate", cutoffDate);

        return query.getResultList();
    }
}