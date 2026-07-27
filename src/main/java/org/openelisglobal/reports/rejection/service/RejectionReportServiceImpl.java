package org.openelisglobal.reports.rejection.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionHeatmapResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rejection Rate compute (OGC-710, closing C.3 gap #4). A "rejection" is a
 * REJECTION_REASON ('R') note on an analysis — the durable marker the
 * results-entry flow writes when a tech rejects a test, with the note text
 * snapshotting the REJECTION_REASONS dictionary value (LogbookResultsController
 * / ResultUtil).
 *
 * <p>
 * Every rate is cohort-consistent: the window selects analyses by
 * {@code started_date}, and the numerator is the subset of those analyses that
 * carry an 'R' note (whenever the note was written). Numerator ⊆ denominator,
 * so a rate can never exceed 100% — bucketing the numerator by note date
 * instead used to render a 200% daily point whenever an analysis started in one
 * bucket was rejected in another. Consequence worth knowing: a detail row may
 * show a "rejected at" timestamp outside the selected window; the window is
 * about when the work arrived, not when it was rejected.
 *
 * <p>
 * Structure deliberately mirrors AmendmentReportServiceImpl — same rate
 * rounding and period keys — so the two QI computes stay reviewable side by
 * side.
 */
@Service
@Transactional(readOnly = true)
public class RejectionReportServiceImpl implements RejectionReportService {

    /**
     * Started analyses LEFT JOINed to their rejection notes; the CASE inside
     * COUNT(DISTINCT ...) counts an analysis once no matter how many 'R' notes it
     * carries (one rejection can record two reasons).
     */
    private static final String COHORT_FROM = " FROM analysis a LEFT JOIN note n ON n.reference_id = a.id"
            + " AND n.reference_table = :analysisRef AND n.note_type = 'R'"
            + " WHERE a.started_date >= :fromTs AND a.started_date < :toTs";

    private static final String REJECTED_COUNT = "COUNT(DISTINCT CASE WHEN n.id IS NOT NULL THEN a.id END)";
    // explicit aliases — Hibernate's native-query auto-discovery rejects two
    // columns that both auto-alias to "count"
    private static final String COUNT_COLUMNS = "COUNT(DISTINCT a.id) AS started_count, " + REJECTED_COUNT
            + " AS rejected_count";

    /**
     * Requesting-organization ("ordering location") of the analysis's sample.
     * ponytail: a sample with two organization requesters fans out — one detail row
     * per requester (inflating that list's totalCount) and one heatmap cell per
     * location. Not observed in practice; dedup to a single requester if it ever
     * is.
     */
    private static final String REQUESTER_ORG_JOIN = " LEFT JOIN sample_requester sr ON sr.sample_id = s.id"
            + " AND sr.requester_type_id = (SELECT rt.id FROM requester_type rt WHERE rt.requester_type ="
            + " 'organization') LEFT JOIN organization o ON o.id = sr.requester_id";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Override
    public RejectionSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);

        Object[] row = (Object[]) session.createNativeQuery("SELECT " + COUNT_COLUMNS + COHORT_FROM)
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).uniqueResult();

        long total = ((Number) row[0]).longValue();
        long rejected = ((Number) row[1]).longValue();

        RejectionSummaryResponse response = new RejectionSummaryResponse();
        response.setRejectedCount(rejected);
        response.setTotalCount(total);
        response.setRatePercent(ratePercent(rejected, total));
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
                        + " s.accession_number, t.name AS test_name, n.text,"
                        + " TRIM(CONCAT(su.first_name, ' ', su.last_name))," + " o.name AS org_name, ne.nce_number"
                        + " FROM note n JOIN analysis a ON a.id = n.reference_id"
                        + " JOIN sample_item si ON si.id = a.sampitem_id JOIN sample s ON s.id = si.samp_id"
                        + " JOIN test t ON t.id = a.test_id LEFT JOIN system_user su ON su.id = n.sys_user_id"
                        + REQUESTER_ORG_JOIN + " LEFT JOIN nc_event ne ON ne.trigger_source_type = 'TEST_REJECTION'"
                        + " AND ne.trigger_source_id = CAST(a.id AS varchar)"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND a.started_date >= :fromTs AND a.started_date < :toTs ORDER BY n.lastupdated DESC")
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
            event.setLocation((String) row[6]);
            event.setNceNumber((String) row[7]);
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
        List<Object[]> buckets = session
                .createNativeQuery("SELECT date_trunc(:unit, a.started_date) AS bucket, " + COUNT_COLUMNS + COHORT_FROM
                        + " GROUP BY 1 ORDER BY 1")
                .setParameter("unit", unit).setParameter("analysisRef", analysisRefTableId())
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        List<RejectionTrendResponse.TrendPoint> points = new ArrayList<>();
        for (Object[] row : buckets) {
            long total = ((Number) row[1]).longValue();
            long rejected = ((Number) row[2]).longValue();
            RejectionTrendResponse.TrendPoint point = new RejectionTrendResponse.TrendPoint();
            point.setPeriod(periodKey(row[0], interval));
            point.setRejectedCount(rejected);
            point.setTotalCount(total);
            point.setRatePercent(ratePercent(rejected, total));
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
        // analysis rejected for two reasons genuinely contributes two reasons;
        // the window still selects by the analysis's start.
        @SuppressWarnings("unchecked")
        List<Object[]> reasonRows = session
                .createNativeQuery("SELECT n.text, COUNT(*) FROM note n JOIN analysis a ON a.id = n.reference_id"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " AND a.started_date >= :fromTs AND a.started_date < :toTs GROUP BY n.text"
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

        // only rejected tests appear — a full every-test list is noise here
        @SuppressWarnings("unchecked")
        List<Object[]> testRows = session
                .createNativeQuery("SELECT CAST(t.id AS varchar), t.name, " + COUNT_COLUMNS
                        + " FROM analysis a JOIN test t ON t.id = a.test_id LEFT JOIN note n ON n.reference_id = a.id"
                        + " AND n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " WHERE a.started_date >= :fromTs AND a.started_date < :toTs GROUP BY t.id, t.name"
                        + " HAVING " + REJECTED_COUNT + " > 0")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).list();

        List<RejectionBreakdownResponse.TestRow> tests = new ArrayList<>();
        for (Object[] row : testRows) {
            RejectionBreakdownResponse.TestRow testRow = new RejectionBreakdownResponse.TestRow();
            testRow.setTestName((String) row[1]);
            testRow.setTotalCount(((Number) row[2]).longValue());
            testRow.setRejectedCount(((Number) row[3]).longValue());
            testRow.setRatePercent(ratePercent(testRow.getRejectedCount(), testRow.getTotalCount()));
            tests.add(testRow);
        }
        tests.sort((a, b) -> a.getRejectedCount() != b.getRejectedCount()
                ? Long.compare(b.getRejectedCount(), a.getRejectedCount())
                : a.getTestName().compareTo(b.getTestName()));
        response.setTests(tests);

        return response;
    }

    @Override
    public RejectionHeatmapResponse getHeatmap(LocalDate fromDate, LocalDate toDate) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);

        // Section falls back to the test's home section — dev/legacy analyses
        // often carry no analysis-level test_sect_id, and an INNER JOIN there
        // silently dropped them from the grid (rejected ⊆ started still holds
        // per cell either way).
        @SuppressWarnings("unchecked")
        List<Object[]> rows = session
                .createNativeQuery("SELECT o.name AS org_name, ts.name AS section_name, " + COUNT_COLUMNS
                        + " FROM analysis a JOIN sample_item si ON si.id = a.sampitem_id"
                        + " JOIN sample s ON s.id = si.samp_id JOIN test t ON t.id = a.test_id"
                        + " LEFT JOIN test_section ts ON ts.id = COALESCE(a.test_sect_id, t.test_section_id)"
                        + REQUESTER_ORG_JOIN + " LEFT JOIN note n ON n.reference_id = a.id"
                        + " AND n.reference_table = :analysisRef AND n.note_type = 'R'"
                        + " WHERE a.started_date >= :fromTs AND a.started_date < :toTs"
                        + " GROUP BY o.name, ts.name ORDER BY ts.name, o.name")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).list();

        RejectionHeatmapResponse response = new RejectionHeatmapResponse();
        for (Object[] row : rows) {
            RejectionHeatmapResponse.Cell cell = new RejectionHeatmapResponse.Cell();
            cell.setLocation((String) row[0]);
            cell.setSection((String) row[1]);
            cell.setTotalCount(((Number) row[2]).longValue());
            cell.setRejectedCount(((Number) row[3]).longValue());
            cell.setRatePercent(ratePercent(cell.getRejectedCount(), cell.getTotalCount()));
            response.getCells().add(cell);
        }
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
        LocalDate date = ((Timestamp) truncatedBucket).toLocalDateTime().toLocalDate();
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
