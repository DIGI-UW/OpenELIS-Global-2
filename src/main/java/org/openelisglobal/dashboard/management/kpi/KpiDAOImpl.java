package org.openelisglobal.dashboard.management.kpi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
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

    @Override
    public Map<String, TatFigures> tatByPeriod(LocalDate from, LocalDate to, String interval, BigDecimal targetHours) {
        Map<String, TatFigures> figures = new HashMap<>();
        for (Object row : tatQuery("SELECT bucket, percentile_disc(0.5) WITHIN GROUP (ORDER BY hours) AS median,"
                + " count(hours) AS validated, count(*) FILTER (WHERE hours <= :target) AS on_time FROM (SELECT date_trunc(:unit, received) AS bucket, hours"
                + " FROM tat) b GROUP BY GROUPING SETS ((bucket), ())", from, to, targetHours)
                .setParameter("unit", QiReportSupport.truncUnit(interval)).getResultList()) {
            Object[] cols = (Object[]) row;
            figures.put(cols[0] == null ? ALL_PERIODS : QiReportSupport.periodKey(cols[0], interval), new TatFigures(
                    (BigDecimal) cols[1], ((Number) cols[2]).longValue(), ((Number) cols[3]).longValue()));
        }
        return figures;
    }

    @Override
    public Map<String, long[]> countOnTimeBySection(LocalDate from, LocalDate to, BigDecimal targetHours) {
        Map<String, long[]> counts = new HashMap<>();
        for (Object row : tatQuery(
                "SELECT section, count(hours) AS validated, count(*) FILTER (WHERE hours <= :target) AS on_time FROM tat"
                        + " GROUP BY section HAVING count(hours) > 0",
                from, to, targetHours).getResultList()) {
            Object[] cols = (Object[]) row;
            counts.put(cols[0] == null ? null : String.valueOf(((Number) cols[0]).longValue()),
                    new long[] { ((Number) cols[1]).longValue(), ((Number) cols[2]).longValue() });
        }
        return counts;
    }

    // The TAT report's rows and receipt-to-validation hours: whole milliseconds,
    // two decimals, none when negative.
    private Query tatQuery(String select, LocalDate from, LocalDate to, BigDecimal targetHours) {
        return entityManager
                .createNativeQuery("WITH tat AS (SELECT s.received_date AS received,"
                        + " COALESCE(a.test_sect_id, t.test_section_id) AS section,"
                        + " CASE WHEN a.released_date >= s.received_date THEN round(trunc(CAST(extract(epoch FROM"
                        + " a.released_date - s.received_date) AS numeric) * 1000) / 3600000, 2) END AS hours"
                        + " FROM clinlims.analysis a JOIN clinlims.sample_item si ON si.id = a.sampitem_id"
                        + " JOIN clinlims.sample s ON s.id = si.samp_id LEFT JOIN clinlims.test t ON t.id = a.test_id"
                        + " WHERE s.received_date >= :from AND s.received_date < :to"
                        + " AND a.status_id NOT IN (SELECT st.id FROM clinlims.status_of_sample st"
                        + " WHERE st.status_type = 'ANALYSIS' AND st.name = 'Test Canceled')"
                        + " AND (si.rejected IS NULL OR NOT si.rejected)) " + select)
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .setParameter("target", new TypedParameterValue(StandardBasicTypes.BIG_DECIMAL, targetHours));
    }
}
