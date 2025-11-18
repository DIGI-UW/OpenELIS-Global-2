package org.openelisglobal.analyzer.service;

import java.util.Map;

public interface AnalyzerQueryService {
    /**
     * Start an asynchronous query job for an analyzer. Returns a jobId immediately.
     */
    String startQuery(String analyzerId);

    /**
     * Get current status for a job.
     */
    Map<String, Object> getStatus(String analyzerId, String jobId);

    /**
     * Cancel a running job if possible.
     */
    void cancel(String analyzerId, String jobId);
}
