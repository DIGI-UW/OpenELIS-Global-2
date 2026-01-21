package org.openelisglobal.virology.daoimpl;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.virology.dao.VirusCultureWorkflowStatusDAO;
import org.openelisglobal.virology.valueholder.VirusCultureBatch;
import org.openelisglobal.virology.valueholder.VirusCultureWorkflowStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for VirusCultureWorkflowStatus entity
 */
@Component
@Transactional
public class VirusCultureWorkflowStatusDAOImpl extends BaseDAOImpl<VirusCultureWorkflowStatus, Integer> implements VirusCultureWorkflowStatusDAO {

    public VirusCultureWorkflowStatusDAOImpl() {
        super(VirusCultureWorkflowStatus.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureWorkflowStatus> findByCultureBatch(VirusCultureBatch cultureBatch) {
        if (cultureBatch == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.cultureBatch = :cultureBatch ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("cultureBatch", cultureBatch);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureWorkflowStatus> findByCultureBatchId(Integer cultureBatchId) {
        if (cultureBatchId == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.cultureBatch.id = :cultureBatchId ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("cultureBatchId", cultureBatchId);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public VirusCultureWorkflowStatus findByCultureBatchAndStepName(VirusCultureBatch cultureBatch, String stepName) {
        if (cultureBatch == null || stepName == null) {
            return null;
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.cultureBatch = :cultureBatch AND vws.stepName = :stepName";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("cultureBatch", cultureBatch);
        query.setParameter("stepName", stepName);
        query.setMaxResults(1);

        List<VirusCultureWorkflowStatus> results = query.getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureWorkflowStatus> findByStatus(VirusCultureWorkflowStatus.StepStatus status) {
        if (status == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.status = :status ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("status", status.name());

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public VirusCultureWorkflowStatus findCurrentStep(VirusCultureBatch cultureBatch) {
        if (cultureBatch == null) {
            return null;
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.cultureBatch = :cultureBatch AND vws.status = :status ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("cultureBatch", cultureBatch);
        query.setParameter("status", VirusCultureWorkflowStatus.StepStatus.IN_PROGRESS);
        query.setMaxResults(1);

        List<VirusCultureWorkflowStatus> results = query.getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public VirusCultureWorkflowStatus findNextPendingStep(VirusCultureBatch cultureBatch) {
        if (cultureBatch == null) {
            return null;
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.cultureBatch = :cultureBatch AND vws.status = :status ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("cultureBatch", cultureBatch);
        query.setParameter("status", VirusCultureWorkflowStatus.StepStatus.PENDING);
        query.setMaxResults(1);

        List<VirusCultureWorkflowStatus> results = query.getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureWorkflowStatus> findByAssignedTo(Integer userId) {
        if (userId == null) {
            return List.of();
        }

        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.assignedTo.id = :userId ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("userId", userId);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureWorkflowStatus> findInProgress() {
        String hql = "FROM VirusCultureWorkflowStatus vws WHERE vws.status = :status ORDER BY vws.stepOrder ASC";
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("status", VirusCultureWorkflowStatus.StepStatus.IN_PROGRESS);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VirusCultureWorkflowStatus> findFailedQualityChecks() {
        String hql = """
            FROM VirusCultureWorkflowStatus vws
            WHERE vws.status = :failedStatus
            OR vws.qualityCheckResult = :failedQc
            ORDER BY vws.stepOrder ASC
            """;
        Query<VirusCultureWorkflowStatus> query = entityManager.unwrap(Session.class)
                .createQuery(hql, VirusCultureWorkflowStatus.class);
        query.setParameter("failedStatus", VirusCultureWorkflowStatus.StepStatus.FAILED);
        query.setParameter("failedQc", VirusCultureWorkflowStatus.QualityCheckResult.FAILED);

        return query.getResultList();
    }
}