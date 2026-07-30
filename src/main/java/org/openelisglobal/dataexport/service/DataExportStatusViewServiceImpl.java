package org.openelisglobal.dataexport.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.itech.fhir.dataexport.api.service.DataExportService;
import org.itech.fhir.dataexport.core.dao.DataExportAttemptDAO;
import org.itech.fhir.dataexport.core.model.DataExportAttempt;
import org.itech.fhir.dataexport.core.model.DataExportAttempt.DataExportStatus;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.openelisglobal.dataexport.valueholder.DataExportAttemptView;
import org.openelisglobal.dataexport.valueholder.DataExportStatusView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataExportStatusViewServiceImpl implements DataExportStatusViewService {

    @Autowired
    private DataExportTaskService dataExportTaskService;

    @Autowired
    private DataExportAttemptDAO dataExportAttemptDAO;

    @Autowired
    private DataExportService dataExportService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<DataExportStatusView> getAllStatuses() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        List<DataExportStatusView> results = new ArrayList<>();

        for (DataExportTask task : dataExportTaskService.getDAO().findAll()) {
            Instant lastSuccess = dataExportTaskService.getLatestSuccessInstantForDataExportTask(task);
            Instant lastAttempt = dataExportTaskService.getLatestInstantForDataExportTask(task);

            List<DataExportAttempt> latestList = dataExportAttemptDAO
                    .findLatestDataExportAttemptsByDataExportTask(PageRequest.of(0, 1), task.getId());
            String lastStatus = latestList.isEmpty() ? null : latestList.get(0).getDataExportStatus().name();

            long failed = countSince(task.getId(), DataExportStatus.FAILED, since24h)
                    + countSince(task.getId(), DataExportStatus.INCOMPLETE, since24h);
            long total = countAllSince(task.getId(), since24h);

            results.add(new DataExportStatusView(task.getId(), task.getEndpoint(), task.getMaxDataExportInterval(),
                    Instant.EPOCH.equals(lastSuccess) ? null : lastSuccess.toString(),
                    Instant.EPOCH.equals(lastAttempt) ? null : lastAttempt.toString(), lastStatus, failed, total));
        }
        return results;
    }

    private long countSince(Long taskId, DataExportStatus status, Instant since) {
        return entityManager
                .createQuery("SELECT COUNT(d) FROM DataExportAttempt d" + " WHERE d.dataExportTask.id = :taskId"
                        + " AND d.dataExportStatus = :status" + " AND d.startTime >= :since", Long.class)
                .setParameter("taskId", taskId).setParameter("status", status).setParameter("since", since)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataExportAttemptView> getAttemptsForTask(Long taskId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<DataExportAttempt> attempts = dataExportAttemptDAO
                .findLatestDataExportAttemptsByDataExportTask(PageRequest.of(0, safeLimit), taskId);
        List<DataExportAttemptView> views = new ArrayList<>(attempts.size());
        for (DataExportAttempt a : attempts) {
            Instant start = a.getStartTime();
            Instant end = a.getEndTime();
            Long durationMs = (start != null && end != null) ? (end.toEpochMilli() - start.toEpochMilli()) : null;
            views.add(new DataExportAttemptView(a.getId(),
                    a.getDataExportStatus() == null ? null : a.getDataExportStatus().name(),
                    start == null ? null : start.toString(), end == null ? null : end.toString(), durationMs));
        }
        return views;
    }

    @Override
    @Transactional
    public boolean triggerExport(Long taskId) {
        Optional<DataExportTask> task = dataExportTaskService.getDAO().findById(taskId);
        if (task.isEmpty()) {
            return false;
        }
        dataExportService.exportNewDataFromLocalToRemote(task.get());
        return true;
    }

    private long countAllSince(Long taskId, Instant since) {
        return entityManager
                .createQuery("SELECT COUNT(d) FROM DataExportAttempt d" + " WHERE d.dataExportTask.id = :taskId"
                        + " AND d.startTime >= :since", Long.class)
                .setParameter("taskId", taskId).setParameter("since", since).getSingleResult();
    }
}
