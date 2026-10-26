package org.openelisglobal.dashboard.management.kpi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.springframework.stereotype.Component;

@Component
public class KpiDAOImpl implements KpiDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Long> countSamplesReceivedByPeriod(LocalDate from, LocalDate to, String interval) {
        Map<String, Long> counts = new HashMap<>();
        for (Object row : entityManager
                .createNativeQuery("SELECT date_trunc(:unit, received_date) AS bucket, count(*) AS n"
                        + " FROM clinlims.sample WHERE received_date >= :from AND received_date < :to GROUP BY 1")
                .setParameter("unit", QiReportSupport.truncUnit(interval))
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .getResultList()) {
            Object[] cols = (Object[]) row;
            counts.put(QiReportSupport.periodKey(cols[0], interval), ((Number) cols[1]).longValue());
        }
        return counts;
    }

    @Override
    public Map<String, long[]> countQcRunsByPeriod(LocalDate from, LocalDate to, String interval) {
        Map<String, long[]> counts = new HashMap<>();
        for (Object row : entityManager.createNativeQuery("SELECT date_trunc(:unit, r.run_date_time) AS bucket,"
                + " count(*) AS total, count(*) FILTER (WHERE r.qualitative_outcome IN ('FAIL', 'INVALID')"
                + " OR EXISTS (SELECT 1 FROM clinlims.qc_rule_violation v"
                + " WHERE v.triggering_result_id = r.id AND v.severity = 'REJECTION')) AS failed"
                + " FROM clinlims.qc_result r WHERE r.run_date_time >= :from AND r.run_date_time < :to GROUP BY 1")
                .setParameter("unit", QiReportSupport.truncUnit(interval))
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .getResultList()) {
            Object[] cols = (Object[]) row;
            counts.put(QiReportSupport.periodKey(cols[0], interval),
                    new long[] { ((Number) cols[1]).longValue(), ((Number) cols[2]).longValue() });
        }
        return counts;
    }
}
