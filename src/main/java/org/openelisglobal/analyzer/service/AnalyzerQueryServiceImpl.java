package org.openelisglobal.analyzer.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerQueryServiceImpl implements AnalyzerQueryService {

    // In-memory job store (sufficient for skeleton/testing)
    private final Map<String, Map<String, Object>> jobStore = new ConcurrentHashMap<>();

    @Override
    public String startQuery(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            throw new LIMSRuntimeException("Analyzer ID required");
        }
        String jobId = UUID.randomUUID().toString();
        Map<String, Object> status = new HashMap<>();
        status.put("analyzerId", analyzerId);
        status.put("jobId", jobId);
        status.put("createdAt", Instant.now().toString());
        status.put("state", "completed"); // skeleton: immediately completed
        status.put("progress", 100);
        status.put("logs", Collections.singletonList("[00:00:00.000] Query scheduled and completed (skeleton)"));
        status.put("fields", Collections.emptyList());
        jobStore.put(jobKey(analyzerId, jobId), status);
        return jobId;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(String analyzerId, String jobId) {
        Map<String, Object> status = jobStore.get(jobKey(analyzerId, jobId));
        if (status == null) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("analyzerId", analyzerId);
            notFound.put("jobId", jobId);
            notFound.put("state", "not_found");
            notFound.put("progress", 0);
            return notFound;
        }
        return status;
    }

    @Override
    public void cancel(String analyzerId, String jobId) {
        Map<String, Object> status = jobStore.get(jobKey(analyzerId, jobId));
        if (status != null && !"completed".equals(status.get("state"))) {
            status.put("state", "cancelled");
        }
    }

    private String jobKey(String analyzerId, String jobId) {
        return analyzerId + "::" + jobId;
    }
}
