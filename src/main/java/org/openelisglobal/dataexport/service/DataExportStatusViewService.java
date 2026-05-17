package org.openelisglobal.dataexport.service;

import java.util.List;
import org.openelisglobal.dataexport.valueholder.DataExportAttemptView;
import org.openelisglobal.dataexport.valueholder.DataExportStatusView;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
public interface DataExportStatusViewService {

    List<DataExportStatusView> getAllStatuses();

    List<DataExportAttemptView> getAttemptsForTask(Long taskId, int limit);

    /**
     * Trigger an immediate export for the given task. The underlying export
     * is @Async, so this returns as soon as the work is queued; the new attempt row
     * appears in `data_export_attempt` once the worker thread inserts it.
     *
     * @return true if the task exists and was triggered, false if not found.
     */
    boolean triggerExport(Long taskId);
}
