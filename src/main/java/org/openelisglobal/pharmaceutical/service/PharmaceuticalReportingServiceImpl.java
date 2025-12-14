package org.openelisglobal.pharmaceutical.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.pharmaceutical.dao.AssayRunDAO;
import org.openelisglobal.pharmaceutical.dao.DisposalRecordDAO;
import org.openelisglobal.pharmaceutical.dao.EnvironmentalExcursionEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.dao.QCCheckDAO;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional(readOnly = true)
public class PharmaceuticalReportingServiceImpl implements PharmaceuticalReportingService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Autowired
    private AssayRunDAO assayRunDAO;

    @Autowired
    private QCCheckDAO qcCheckDAO;

    @Autowired
    private DisposalRecordDAO disposalRecordDAO;

    @Autowired
    private EnvironmentalExcursionEventDAO excursionEventDAO;

    @Override
    public Map<String, Object> getIntakeMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String hql = "SELECT COUNT(s) FROM PharmaceuticalSample s WHERE s.registeredAt BETWEEN :startDate AND :endDate";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        Long totalIntake = query.uniqueResult();
        metrics.put("totalIntake", totalIntake != null ? totalIntake : 0L);

        String byLabTypeHql = "SELECT s.labType, COUNT(s) FROM PharmaceuticalSample s " +
                "WHERE s.registeredAt BETWEEN :startDate AND :endDate GROUP BY s.labType";
        Query<Object[]> byLabTypeQuery = entityManager.unwrap(Session.class).createQuery(byLabTypeHql, Object[].class);
        byLabTypeQuery.setParameter("startDate", startDate);
        byLabTypeQuery.setParameter("endDate", endDate);
        List<Object[]> byLabType = byLabTypeQuery.list();
        Map<String, Long> labTypeDistribution = new HashMap<>();
        for (Object[] row : byLabType) {
            labTypeDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byLabType", labTypeDistribution);

        return metrics;
    }

    @Override
    public Map<String, Object> getQCMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String hql = "SELECT COUNT(q) FROM QCCheck q WHERE q.checkedAt BETWEEN :startDate AND :endDate";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        Long totalQC = query.uniqueResult();
        metrics.put("totalQCChecks", totalQC != null ? totalQC : 0L);

        String passHql = "SELECT COUNT(q) FROM QCCheck q WHERE q.outcome = 'PASS' AND q.checkedAt BETWEEN :startDate AND :endDate";
        Query<Long> passQuery = entityManager.unwrap(Session.class).createQuery(passHql, Long.class);
        passQuery.setParameter("startDate", startDate);
        passQuery.setParameter("endDate", endDate);
        Long passCount = passQuery.uniqueResult();
        metrics.put("passCount", passCount != null ? passCount : 0L);

        String failHql = "SELECT COUNT(q) FROM QCCheck q WHERE q.outcome = 'FAIL' AND q.checkedAt BETWEEN :startDate AND :endDate";
        Query<Long> failQuery = entityManager.unwrap(Session.class).createQuery(failHql, Long.class);
        failQuery.setParameter("startDate", startDate);
        failQuery.setParameter("endDate", endDate);
        Long failCount = failQuery.uniqueResult();
        metrics.put("failCount", failCount != null ? failCount : 0L);

        if (totalQC != null && totalQC > 0) {
            double passRate = ((double) (passCount != null ? passCount : 0) / totalQC) * 100;
            metrics.put("passRate", Math.round(passRate * 100.0) / 100.0);
        } else {
            metrics.put("passRate", 0.0);
        }

        return metrics;
    }

    @Override
    public Map<String, Object> getAssayMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String hql = "SELECT COUNT(a) FROM AssayRun a WHERE a.startedAt BETWEEN :startDate AND :endDate";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        Long totalAssays = query.uniqueResult();
        metrics.put("totalAssays", totalAssays != null ? totalAssays : 0L);

        String byStatusHql = "SELECT a.status, COUNT(a) FROM AssayRun a " +
                "WHERE a.startedAt BETWEEN :startDate AND :endDate GROUP BY a.status";
        Query<Object[]> byStatusQuery = entityManager.unwrap(Session.class).createQuery(byStatusHql, Object[].class);
        byStatusQuery.setParameter("startDate", startDate);
        byStatusQuery.setParameter("endDate", endDate);
        List<Object[]> byStatus = byStatusQuery.list();
        Map<String, Long> statusDistribution = new HashMap<>();
        for (Object[] row : byStatus) {
            statusDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byStatus", statusDistribution);

        String byTypeHql = "SELECT a.assayType, COUNT(a) FROM AssayRun a " +
                "WHERE a.startedAt BETWEEN :startDate AND :endDate GROUP BY a.assayType";
        Query<Object[]> byTypeQuery = entityManager.unwrap(Session.class).createQuery(byTypeHql, Object[].class);
        byTypeQuery.setParameter("startDate", startDate);
        byTypeQuery.setParameter("endDate", endDate);
        List<Object[]> byType = byTypeQuery.list();
        Map<String, Long> typeDistribution = new HashMap<>();
        for (Object[] row : byType) {
            typeDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byType", typeDistribution);

        return metrics;
    }

    @Override
    public Map<String, Object> getOOSMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String totalHql = "SELECT COUNT(a) FROM AssayRun a WHERE a.startedAt BETWEEN :startDate AND :endDate";
        Query<Long> totalQuery = entityManager.unwrap(Session.class).createQuery(totalHql, Long.class);
        totalQuery.setParameter("startDate", startDate);
        totalQuery.setParameter("endDate", endDate);
        Long totalAssays = totalQuery.uniqueResult();
        metrics.put("totalAssays", totalAssays != null ? totalAssays : 0L);

        String oosHql = "SELECT COUNT(a) FROM AssayRun a WHERE a.oosFlag = true AND a.startedAt BETWEEN :startDate AND :endDate";
        Query<Long> oosQuery = entityManager.unwrap(Session.class).createQuery(oosHql, Long.class);
        oosQuery.setParameter("startDate", startDate);
        oosQuery.setParameter("endDate", endDate);
        Long oosCount = oosQuery.uniqueResult();
        metrics.put("oosCount", oosCount != null ? oosCount : 0L);

        if (totalAssays != null && totalAssays > 0) {
            double oosRate = ((double) (oosCount != null ? oosCount : 0) / totalAssays) * 100;
            metrics.put("oosRate", Math.round(oosRate * 100.0) / 100.0);
        } else {
            metrics.put("oosRate", 0.0);
        }

        String byTypeHql = "SELECT a.assayType, COUNT(a) FROM AssayRun a " +
                "WHERE a.oosFlag = true AND a.startedAt BETWEEN :startDate AND :endDate GROUP BY a.assayType";
        Query<Object[]> byTypeQuery = entityManager.unwrap(Session.class).createQuery(byTypeHql, Object[].class);
        byTypeQuery.setParameter("startDate", startDate);
        byTypeQuery.setParameter("endDate", endDate);
        List<Object[]> byType = byTypeQuery.list();
        Map<String, Long> oosByType = new HashMap<>();
        for (Object[] row : byType) {
            oosByType.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("oosByType", oosByType);

        return metrics;
    }

    @Override
    public Map<String, Object> getTATMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String avgTATHql = "SELECT AVG(TIMESTAMPDIFF(HOUR, a.startedAt, a.reviewedAt)) FROM AssayRun a " +
                "WHERE a.reviewedAt IS NOT NULL AND a.startedAt BETWEEN :startDate AND :endDate";
        Query<Double> avgQuery = entityManager.unwrap(Session.class).createQuery(avgTATHql, Double.class);
        avgQuery.setParameter("startDate", startDate);
        avgQuery.setParameter("endDate", endDate);
        Double avgTAT = avgQuery.uniqueResult();
        metrics.put("averageTATHours", avgTAT != null ? Math.round(avgTAT * 100.0) / 100.0 : 0.0);

        String withinSLAHql = "SELECT COUNT(a) FROM AssayRun a " +
                "WHERE a.reviewedAt IS NOT NULL AND TIMESTAMPDIFF(HOUR, a.startedAt, a.reviewedAt) <= 48 " +
                "AND a.startedAt BETWEEN :startDate AND :endDate";
        Query<Long> withinSLAQuery = entityManager.unwrap(Session.class).createQuery(withinSLAHql, Long.class);
        withinSLAQuery.setParameter("startDate", startDate);
        withinSLAQuery.setParameter("endDate", endDate);
        Long withinSLA = withinSLAQuery.uniqueResult();
        metrics.put("withinSLACount", withinSLA != null ? withinSLA : 0L);

        String totalCompletedHql = "SELECT COUNT(a) FROM AssayRun a " +
                "WHERE a.reviewedAt IS NOT NULL AND a.startedAt BETWEEN :startDate AND :endDate";
        Query<Long> totalCompletedQuery = entityManager.unwrap(Session.class).createQuery(totalCompletedHql, Long.class);
        totalCompletedQuery.setParameter("startDate", startDate);
        totalCompletedQuery.setParameter("endDate", endDate);
        Long totalCompleted = totalCompletedQuery.uniqueResult();
        metrics.put("totalCompleted", totalCompleted != null ? totalCompleted : 0L);

        if (totalCompleted != null && totalCompleted > 0) {
            double slaCompliance = ((double) (withinSLA != null ? withinSLA : 0) / totalCompleted) * 100;
            metrics.put("slaComplianceRate", Math.round(slaCompliance * 100.0) / 100.0);
        } else {
            metrics.put("slaComplianceRate", 0.0);
        }

        return metrics;
    }

    @Override
    public Map<String, Object> getStorageMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        String totalSamplesHql = "SELECT COUNT(s) FROM PharmaceuticalSample s WHERE s.status = 'IN_STORAGE'";
        Query<Long> totalSamplesQuery = entityManager.unwrap(Session.class).createQuery(totalSamplesHql, Long.class);
        Long totalInStorage = totalSamplesQuery.uniqueResult();
        metrics.put("totalInStorage", totalInStorage != null ? totalInStorage : 0L);

        String totalAliquotsHql = "SELECT COUNT(a) FROM Aliquot a WHERE a.status = 'AVAILABLE'";
        Query<Long> totalAliquotsQuery = entityManager.unwrap(Session.class).createQuery(totalAliquotsHql, Long.class);
        Long availableAliquots = totalAliquotsQuery.uniqueResult();
        metrics.put("availableAliquots", availableAliquots != null ? availableAliquots : 0L);

        String freezeThawExceededHql = "SELECT COUNT(a) FROM Aliquot a WHERE a.freezeThawCount >= a.freezeThawLimit";
        Query<Long> freezeThawQuery = entityManager.unwrap(Session.class).createQuery(freezeThawExceededHql, Long.class);
        Long freezeThawExceeded = freezeThawQuery.uniqueResult();
        metrics.put("freezeThawExceeded", freezeThawExceeded != null ? freezeThawExceeded : 0L);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp thirtyDaysLater = new Timestamp(now.getTime() + 30L * 24 * 60 * 60 * 1000);
        String expiringSoonHql = "SELECT COUNT(s) FROM PharmaceuticalSample s " +
                "WHERE s.expiryRetestDate BETWEEN :now AND :thirtyDays";
        Query<Long> expiringSoonQuery = entityManager.unwrap(Session.class).createQuery(expiringSoonHql, Long.class);
        expiringSoonQuery.setParameter("now", now);
        expiringSoonQuery.setParameter("thirtyDays", thirtyDaysLater);
        Long expiringSoon = expiringSoonQuery.uniqueResult();
        metrics.put("expiringSoon30Days", expiringSoon != null ? expiringSoon : 0L);

        return metrics;
    }

    @Override
    public Map<String, Object> getDisposalMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String totalHql = "SELECT COUNT(d) FROM DisposalRecord d WHERE d.requestedAt BETWEEN :startDate AND :endDate";
        Query<Long> totalQuery = entityManager.unwrap(Session.class).createQuery(totalHql, Long.class);
        totalQuery.setParameter("startDate", startDate);
        totalQuery.setParameter("endDate", endDate);
        Long totalRequests = totalQuery.uniqueResult();
        metrics.put("totalRequests", totalRequests != null ? totalRequests : 0L);

        String byStatusHql = "SELECT d.status, COUNT(d) FROM DisposalRecord d " +
                "WHERE d.requestedAt BETWEEN :startDate AND :endDate GROUP BY d.status";
        Query<Object[]> byStatusQuery = entityManager.unwrap(Session.class).createQuery(byStatusHql, Object[].class);
        byStatusQuery.setParameter("startDate", startDate);
        byStatusQuery.setParameter("endDate", endDate);
        List<Object[]> byStatus = byStatusQuery.list();
        Map<String, Long> statusDistribution = new HashMap<>();
        for (Object[] row : byStatus) {
            statusDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byStatus", statusDistribution);

        String byReasonHql = "SELECT d.reason, COUNT(d) FROM DisposalRecord d " +
                "WHERE d.requestedAt BETWEEN :startDate AND :endDate GROUP BY d.reason";
        Query<Object[]> byReasonQuery = entityManager.unwrap(Session.class).createQuery(byReasonHql, Object[].class);
        byReasonQuery.setParameter("startDate", startDate);
        byReasonQuery.setParameter("endDate", endDate);
        List<Object[]> byReason = byReasonQuery.list();
        Map<String, Long> reasonDistribution = new HashMap<>();
        for (Object[] row : byReason) {
            reasonDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byReason", reasonDistribution);

        return metrics;
    }

    @Override
    public Map<String, Object> getExcursionMetrics(Timestamp startDate, Timestamp endDate) {
        Map<String, Object> metrics = new HashMap<>();

        String totalHql = "SELECT COUNT(e) FROM EnvironmentalExcursionEvent e WHERE e.detectedAt BETWEEN :startDate AND :endDate";
        Query<Long> totalQuery = entityManager.unwrap(Session.class).createQuery(totalHql, Long.class);
        totalQuery.setParameter("startDate", startDate);
        totalQuery.setParameter("endDate", endDate);
        Long totalExcursions = totalQuery.uniqueResult();
        metrics.put("totalExcursions", totalExcursions != null ? totalExcursions : 0L);

        String byStatusHql = "SELECT e.status, COUNT(e) FROM EnvironmentalExcursionEvent e " +
                "WHERE e.detectedAt BETWEEN :startDate AND :endDate GROUP BY e.status";
        Query<Object[]> byStatusQuery = entityManager.unwrap(Session.class).createQuery(byStatusHql, Object[].class);
        byStatusQuery.setParameter("startDate", startDate);
        byStatusQuery.setParameter("endDate", endDate);
        List<Object[]> byStatus = byStatusQuery.list();
        Map<String, Long> statusDistribution = new HashMap<>();
        for (Object[] row : byStatus) {
            statusDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byStatus", statusDistribution);

        String byTypeHql = "SELECT e.alertType, COUNT(e) FROM EnvironmentalExcursionEvent e " +
                "WHERE e.detectedAt BETWEEN :startDate AND :endDate GROUP BY e.alertType";
        Query<Object[]> byTypeQuery = entityManager.unwrap(Session.class).createQuery(byTypeHql, Object[].class);
        byTypeQuery.setParameter("startDate", startDate);
        byTypeQuery.setParameter("endDate", endDate);
        List<Object[]> byType = byTypeQuery.list();
        Map<String, Long> typeDistribution = new HashMap<>();
        for (Object[] row : byType) {
            typeDistribution.put(row[0].toString(), (Long) row[1]);
        }
        metrics.put("byAlertType", typeDistribution);

        String activeHql = "SELECT COUNT(e) FROM EnvironmentalExcursionEvent e WHERE e.status = 'ACTIVE'";
        Query<Long> activeQuery = entityManager.unwrap(Session.class).createQuery(activeHql, Long.class);
        Long activeExcursions = activeQuery.uniqueResult();
        metrics.put("currentlyActive", activeExcursions != null ? activeExcursions : 0L);

        return metrics;
    }

    @Override
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp thirtyDaysAgo = new Timestamp(now.getTime() - 30L * 24 * 60 * 60 * 1000);

        summary.put("intake", getIntakeMetrics(thirtyDaysAgo, now));
        summary.put("qc", getQCMetrics(thirtyDaysAgo, now));
        summary.put("assays", getAssayMetrics(thirtyDaysAgo, now));
        summary.put("oos", getOOSMetrics(thirtyDaysAgo, now));
        summary.put("tat", getTATMetrics(thirtyDaysAgo, now));
        summary.put("storage", getStorageMetrics());
        summary.put("disposal", getDisposalMetrics(thirtyDaysAgo, now));
        summary.put("excursions", getExcursionMetrics(thirtyDaysAgo, now));

        String pendingReviewHql = "SELECT COUNT(a) FROM AssayRun a WHERE a.status = 'PENDING_REVIEW'";
        Query<Long> pendingReviewQuery = entityManager.unwrap(Session.class).createQuery(pendingReviewHql, Long.class);
        Long pendingReview = pendingReviewQuery.uniqueResult();
        summary.put("pendingReviewCount", pendingReview != null ? pendingReview : 0L);

        String pendingDisposalHql = "SELECT COUNT(d) FROM DisposalRecord d WHERE d.status = 'PENDING_APPROVAL'";
        Query<Long> pendingDisposalQuery = entityManager.unwrap(Session.class).createQuery(pendingDisposalHql, Long.class);
        Long pendingDisposal = pendingDisposalQuery.uniqueResult();
        summary.put("pendingDisposalApprovalCount", pendingDisposal != null ? pendingDisposal : 0L);

        return summary;
    }

    @Override
    public List<Map<String, Object>> getSampleStatusDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();

        String hql = "SELECT s.status, COUNT(s) FROM PharmaceuticalSample s GROUP BY s.status";
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
        List<Object[]> results = query.list();

        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", row[0].toString());
            item.put("count", row[1]);
            distribution.add(item);
        }

        return distribution;
    }

    @Override
    public List<Map<String, Object>> getAssayTypeDistribution(Timestamp startDate, Timestamp endDate) {
        List<Map<String, Object>> distribution = new ArrayList<>();

        String hql = "SELECT a.assayType, COUNT(a) FROM AssayRun a " +
                "WHERE a.startedAt BETWEEN :startDate AND :endDate GROUP BY a.assayType ORDER BY COUNT(a) DESC";
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        List<Object[]> results = query.list();

        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("assayType", row[0].toString());
            item.put("count", row[1]);
            distribution.add(item);
        }

        return distribution;
    }

    @Override
    public List<Map<String, Object>> getExcursionHistory(Timestamp startDate, Timestamp endDate) {
        List<Map<String, Object>> history = new ArrayList<>();

        String hql = "FROM EnvironmentalExcursionEvent e WHERE e.detectedAt BETWEEN :startDate AND :endDate ORDER BY e.detectedAt DESC";
        Query<EnvironmentalExcursionEvent> query = entityManager.unwrap(Session.class)
                .createQuery(hql, EnvironmentalExcursionEvent.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        List<EnvironmentalExcursionEvent> events = query.list();

        for (EnvironmentalExcursionEvent event : events) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", event.getId());
            item.put("deviceId", event.getDeviceId());
            item.put("alertType", event.getAlertType().toString());
            item.put("recordedValue", event.getRecordedValue());
            item.put("thresholdValue", event.getThresholdValue());
            item.put("status", event.getStatus().toString());
            item.put("detectedAt", event.getDetectedAt());
            item.put("resolvedAt", event.getResolvedAt());
            item.put("deviceLocation", event.getDeviceLocation());
            history.add(item);
        }

        return history;
    }

    @Override
    public List<Map<String, Object>> getDisposalHistory(Timestamp startDate, Timestamp endDate) {
        List<Map<String, Object>> history = new ArrayList<>();

        String hql = "FROM DisposalRecord d WHERE d.requestedAt BETWEEN :startDate AND :endDate ORDER BY d.requestedAt DESC";
        Query<DisposalRecord> query = entityManager.unwrap(Session.class).createQuery(hql, DisposalRecord.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        List<DisposalRecord> records = query.list();

        for (DisposalRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("sampleId", record.getSampleId());
            item.put("reason", record.getReason().toString());
            item.put("method", record.getMethod().toString());
            item.put("status", record.getStatus().toString());
            item.put("requestedAt", record.getRequestedAt());
            item.put("requestedBy", record.getRequestedBy());
            item.put("approvedAt", record.getApprovedAt());
            item.put("executedAt", record.getExecutedAt());
            history.add(item);
        }

        return history;
    }

    @Override
    public byte[] exportReportAsCSV(String reportType, Timestamp startDate, Timestamp endDate) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        switch (reportType) {
            case "excursions":
                writer.println("ID,Device ID,Alert Type,Recorded Value,Threshold Value,Status,Detected At,Resolved At,Location");
                List<Map<String, Object>> excursions = getExcursionHistory(startDate, endDate);
                for (Map<String, Object> exc : excursions) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            exc.get("id"), exc.get("deviceId"), exc.get("alertType"),
                            exc.get("recordedValue"), exc.get("thresholdValue"),
                            exc.get("status"), exc.get("detectedAt"),
                            exc.get("resolvedAt"), exc.get("deviceLocation"));
                }
                break;

            case "disposal":
                writer.println("ID,Sample ID,Reason,Method,Status,Requested At,Requested By,Approved At,Executed At");
                List<Map<String, Object>> disposals = getDisposalHistory(startDate, endDate);
                for (Map<String, Object> disp : disposals) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                            disp.get("id"), disp.get("sampleId"), disp.get("reason"),
                            disp.get("method"), disp.get("status"),
                            disp.get("requestedAt"), disp.get("requestedBy"),
                            disp.get("approvedAt"), disp.get("executedAt"));
                }
                break;

            default:
                writer.println("Report type not supported: " + reportType);
        }

        writer.flush();
        return outputStream.toByteArray();
    }

    @Override
    public byte[] exportReportAsPDF(String reportType, Timestamp startDate, Timestamp endDate) {
        return new byte[0];
    }
}
