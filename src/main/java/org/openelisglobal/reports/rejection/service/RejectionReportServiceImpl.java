package org.openelisglobal.reports.rejection.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.hibernate.Session;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rejection Rate compute (OGC-710, closing C.3 gap #4). A "rejection" is a
 * REJECTION_REASON ('R') note on an analysis — the durable marker the
 * results-entry and validation flows write when a tech/biologist rejects a
 * test, with the note text snapshotting the REJECTION_REASONS dictionary value
 * (LogbookResultsController / ResultUtil). Rate = rejected analyses / analyses
 * started in the window: rejection removes an analysis from the released
 * population, so releases can't be the denominator the way they are for the
 * amendment rate; started_date is the stable per-analysis intake stamp.
 *
 * <p>
 * Structure deliberately mirrors AmendmentReportServiceImpl — same window
 * predicates, bucketing, and rate rounding — so the two QI computes stay
 * reviewable side by side.
 */
@Service
@Transactional(readOnly = true)
public class RejectionReportServiceImpl implements RejectionReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Override
    public RejectionSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);

        Number rejected = (Number) session
                .createNativeQuery("SELECT COUNT(DISTINCT n.reference_id) FROM note n"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).uniqueResult();

        Number total = (Number) session
                .createNativeQuery("SELECT COUNT(*) FROM analysis a WHERE a.started_date >= :fromTs"
                        + " AND a.started_date < :toTs")
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).uniqueResult();

        RejectionSummaryResponse response = new RejectionSummaryResponse();
        response.setRejectedCount(rejected.longValue());
        response.setTotalCount(total.longValue());
        response.setRatePercent(ratePercent(rejected.longValue(), total.longValue()));
        return response;
    }

    @Override
    public RejectionDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);

        // ponytail: fetch-all + in-memory paging, same as the amendment report;
        // rejections are rare and the window is capped at 366 days.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = session
                .createNativeQuery("SELECT CAST(a.id AS varchar), n.lastupdated,"
                        + " s.accession_number, t.name, n.text, TRIM(CONCAT(su.first_name, ' ', su.last_name))"
                        + " FROM note n JOIN analysis a ON a.id = n.reference_id"
                        + " JOIN sample_item si ON si.id = a.sampitem_id JOIN sample s ON s.id = si.samp_id"
                        + " JOIN test t ON t.id = a.test_id LEFT JOIN system_user su ON su.id = n.sys_user_id"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs ORDER BY n.lastupdated DESC")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).list();

        List<RejectionDetailResponse.RejectionEvent> all = new ArrayList<>();
        for (Object[] row : rows) {
            RejectionDetailResponse.RejectionEvent event = new RejectionDetailResponse.RejectionEvent();
            event.setAnalysisId((String) row[0]);
            event.setRejectedAt((Timestamp) row[1]);
            event.setLabNumber((String) row[2]);
            event.setTestName((String) row[3]);
            event.setReason((String) row[4]);
            event.setRejectedBy((String) row[5]);
            all.add(event);
        }

        int fromIndex = Math.min(page * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());

        RejectionDetailResponse response = new RejectionDetailResponse();
        response.setItems(all.subList(fromIndex, toIndex));
        response.setTotalCount(all.size());
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public RejectionTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, String interval) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);
        String unit = truncUnit(interval);

        @SuppressWarnings("unchecked")
        List<Object[]> rejectedBuckets = session
                .createNativeQuery("SELECT date_trunc(:unit, n.lastupdated), COUNT(DISTINCT n.reference_id)"
                        + " FROM note n WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs GROUP BY 1")
                .setParameter("unit", unit).setParameter("analysisRef", analysisRefTableId())
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        @SuppressWarnings("unchecked")
        List<Object[]> totalBuckets = session
                .createNativeQuery("SELECT date_trunc(:unit, a.started_date), COUNT(*) FROM analysis a"
                        + " WHERE a.started_date >= :fromTs AND a.started_date < :toTs GROUP BY 1")
                .setParameter("unit", unit).setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        // union of period keys — a rejection can land in a bucket with no starts
        Map<String, long[]> byPeriod = new TreeMap<>();
        for (Object[] row : rejectedBuckets) {
            byPeriod.computeIfAbsent(periodKey(row[0], interval), k -> new long[2])[0] = ((Number) row[1]).longValue();
        }
        for (Object[] row : totalBuckets) {
            byPeriod.computeIfAbsent(periodKey(row[0], interval), k -> new long[2])[1] = ((Number) row[1]).longValue();
        }

        List<RejectionTrendResponse.TrendPoint> points = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : byPeriod.entrySet()) {
            RejectionTrendResponse.TrendPoint point = new RejectionTrendResponse.TrendPoint();
            point.setPeriod(entry.getKey());
            point.setRejectedCount(entry.getValue()[0]);
            point.setTotalCount(entry.getValue()[1]);
            point.setRatePercent(ratePercent(entry.getValue()[0], entry.getValue()[1]));
            points.add(point);
        }

        RejectionTrendResponse response = new RejectionTrendResponse();
        response.setPoints(points);
        return response;
    }

    @Override
    public RejectionBreakdownResponse getBreakdown(LocalDate fromDate, LocalDate toDate) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);

        RejectionBreakdownResponse response = new RejectionBreakdownResponse();

        // Reason Pareto: counts note-events (not distinct analyses) because one
        // analysis rejected for two reasons genuinely contributes two reasons.
        @SuppressWarnings("unchecked")
        List<Object[]> reasonRows = session
                .createNativeQuery("SELECT n.text, COUNT(*) FROM note n"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs GROUP BY n.text"
                        + " ORDER BY COUNT(*) DESC, n.text")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).list();

        long reasonTotal = 0;
        for (Object[] row : reasonRows) {
            reasonTotal += ((Number) row[1]).longValue();
        }
        double cumulative = 0;
        for (Object[] row : reasonRows) {
            RejectionBreakdownResponse.ReasonRow reasonRow = new RejectionBreakdownResponse.ReasonRow();
            reasonRow.setReason((String) row[0]);
            reasonRow.setCount(((Number) row[1]).longValue());
            Double percent = ratePercent(reasonRow.getCount(), reasonTotal);
            reasonRow.setPercentOfRejections(percent);
            if (percent != null) {
                cumulative = Math.min(100d, round2(cumulative + percent));
                reasonRow.setCumulativePercent(cumulative);
            }
            response.getReasons().add(reasonRow);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rejectedByTest = session
                .createNativeQuery("SELECT CAST(t.id AS varchar), t.name, COUNT(DISTINCT n.reference_id)"
                        + " FROM note n JOIN analysis a ON a.id = n.reference_id JOIN test t ON t.id = a.test_id"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs GROUP BY t.id, t.name")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).list();

        @SuppressWarnings("unchecked")
        List<Object[]> totalByTest = session
                .createNativeQuery(
                        "SELECT CAST(t.id AS varchar), COUNT(*) FROM analysis a JOIN test t ON t.id = a.test_id"
                                + " WHERE a.started_date >= :fromTs AND a.started_date < :toTs GROUP BY t.id")
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        Map<String, Long> totalCounts = new HashMap<>();
        for (Object[] row : totalByTest) {
            totalCounts.put((String) row[0], ((Number) row[1]).longValue());
        }

        // only rejected tests appear — a full every-test list is noise here
        List<RejectionBreakdownResponse.TestRow> testRows = new ArrayList<>();
        for (Object[] row : rejectedByTest) {
            RejectionBreakdownResponse.TestRow testRow = new RejectionBreakdownResponse.TestRow();
            testRow.setTestName((String) row[1]);
            testRow.setRejectedCount(((Number) row[2]).longValue());
            testRow.setTotalCount(totalCounts.getOrDefault((String) row[0], 0L));
            testRow.setRatePercent(ratePercent(testRow.getRejectedCount(), testRow.getTotalCount()));
            testRows.add(testRow);
        }
        testRows.sort((a, b) -> a.getRejectedCount() != b.getRejectedCount()
                ? Long.compare(b.getRejectedCount(), a.getRejectedCount())
                : a.getTestName().compareTo(b.getTestName()));
        response.setTests(testRows);

        return response;
    }

    private static String truncUnit(String interval) {
        if (interval == null) {
            return "day";
        }
        return switch (interval.toUpperCase()) {
        case "WEEKLY" -> "week";
        case "MONTHLY" -> "month";
        default -> "day"; // DAILY + unknown, matching the TAT/amendment reports
        };
    }

    private static String periodKey(Object truncatedBucket, String interval) {
        return getPeriodKey(((Timestamp) truncatedBucket).toLocalDateTime().toLocalDate(), interval);
    }

    private static String getPeriodKey(LocalDate date, String interval) {
        if (interval == null)
            interval = "DAILY";
        return switch (interval.toUpperCase()) {
        case "WEEKLY" -> date.getYear() + "-W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        case "MONTHLY" -> date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        default -> date.toString(); // DAILY
        };
    }

    private static Double ratePercent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return round2(part * 100.0 / whole);
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Long analysisRefTableId() {
        return Long.valueOf(referenceTablesService.getReferenceTableByName("ANALYSIS").getId());
    }
}
