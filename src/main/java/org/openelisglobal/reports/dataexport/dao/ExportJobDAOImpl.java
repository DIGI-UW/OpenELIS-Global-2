package org.openelisglobal.reports.dataexport.dao;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;
import org.openelisglobal.reports.dataexport.valueholder.ExportJobState;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ExportJobDAOImpl extends BaseDAOImpl<ExportJob, String> implements ExportJobDAO {
    public ExportJobDAOImpl() {
        super(ExportJob.class);
    }

    @Override
    public void lockOwner(String owner) {
        entityManager.find(SystemUser.class, owner, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public ExportJob submission(String owner, String requestId) {
        return entityManager
                .createQuery("from ExportJob j where j.ownerId = :owner and j.clientRequestId = :request",
                        ExportJob.class)
                .setParameter("owner", owner).setParameter("request", requestId).getResultList().stream().findFirst()
                .orElse(null);
    }

    @Override
    public long activeCount(String owner) {
        return entityManager
                .createQuery("select count(j) from ExportJob j where j.ownerId = :owner and j.state in :states",
                        Long.class)
                .setParameter("owner", owner)
                .setParameter("states", List.of(ExportJobState.QUEUED, ExportJobState.GENERATING)).getSingleResult();
    }

    @Override
    public List<ExportJob> page(String owner, int offset, int limit) {
        return entityManager
                .createQuery("from ExportJob j where j.ownerId = :owner order by j.submittedAt desc, j.id desc",
                        ExportJob.class)
                .setParameter("owner", owner).setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    @Override
    public ExportJob owned(String owner, String id, boolean lock) {
        var query = entityManager
                .createQuery("from ExportJob j where j.ownerId = :owner and j.id = :id", ExportJob.class)
                .setParameter("owner", owner).setParameter("id", id);
        if (lock)
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultList().stream().findFirst().orElse(null);
    }

    @Override
    public ExportJob nextQueued() {
        return entityManager
                .createQuery("from ExportJob j where j.state = :state order by j.submittedAt, j.id", ExportJob.class)
                .setParameter("state", ExportJobState.QUEUED).setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream().findFirst().orElse(null);
    }

    @Override
    public void persistJob(ExportJob job) {
        entityManager.persist(job);
    }

    @Override
    public ExportJob locked(String id) {
        return entityManager.find(ExportJob.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public List<ExportJob> dueForRecovery(java.time.Instant now) {
        return entityManager
                .createQuery(
                        "from ExportJob j where "
                                + "(j.state = :generating and (j.leaseUntil is null or j.leaseUntil <= :now)) "
                                + "or (j.state = :ready and j.expiresAt <= :now) order by j.submittedAt, j.id",
                        ExportJob.class)
                .setParameter("generating", ExportJobState.GENERATING).setParameter("ready", ExportJobState.READY)
                .setParameter("now", now).setMaxResults(100).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    @Override
    public List<ExportJob> pendingCleanup() {
        return entityManager
                .createQuery("from ExportJob j where j.state in :states "
                        + "and j.outputCleanedAt is null order by j.submittedAt, j.id", ExportJob.class)
                .setParameter("states",
                        List.of(ExportJobState.FAILED, ExportJobState.CANCELLED, ExportJobState.EXPIRED))
                .setMaxResults(100).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    @Override
    public void flushJobs() {
        entityManager.flush();
    }
}
