package org.openelisglobal.reports.dataexport.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;

public interface ExportJobDAO extends BaseDAO<ExportJob, String> {
    void lockOwner(String owner);

    ExportJob submission(String owner, String requestId);

    long activeCount(String owner);

    List<ExportJob> page(String owner, int offset, int limit);

    ExportJob owned(String owner, String id, boolean lock);

    ExportJob nextQueued();

    ExportJob locked(String id);

    List<ExportJob> dueForRecovery(java.time.Instant now);

    List<ExportJob> pendingCleanup();

    void persistJob(ExportJob job);

    void flushJobs();
}
