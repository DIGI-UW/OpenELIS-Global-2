package org.openelisglobal.reports.amendment.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.Session;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.amendment.bean.AmendmentBreakdownResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentEvent;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentTrendResponse;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Amendment Rate compute (OGC-698). An "amendment" is a result corrected after
 * its patient report went out. The durable marker is the corrected-result
 * EXTERNAL note the save/validation flows write exactly once per analysis
 * (LogbookResultsController / AccessionValidationRestController,
 * "note.corrected.result"). History rows can't anchor the count on their own:
 * re-validation re-stamps analysis.released_date past the amending update, so a
 * timestamp-vs-released_date predicate silently drops amendments once they are
 * re-released. History supplies the prior value for the detail view.
 */
@Service
@Transactional(readOnly = true)
public class AmendmentReportServiceImpl implements AmendmentReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Override
    public AmendmentSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        Timestamp fromTs = QiReportSupport.startOf(fromDate);
        Timestamp toTs = QiReportSupport.endOf(toDate);
        Session session = entityManager.unwrap(Session.class);

        Number amended = (Number) session
                .createNativeQuery("SELECT COUNT(DISTINCT n.reference_id) FROM note n"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'E' AND n.text = :correctedText"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("correctedText", correctedNoteText())
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).uniqueResult();

        Number released = (Number) session
                .createNativeQuery("SELECT COUNT(*) FROM analysis a WHERE a.released_date >= :fromTs"
                        + " AND a.released_date < :toTs")
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).uniqueResult();

        AmendmentSummaryResponse response = new AmendmentSummaryResponse();
        response.setAmendedCount(amended.longValue());
        response.setReleasedCount(released.longValue());
        response.setRatePercent(QiReportSupport.ratePercent(amended.longValue(), released.longValue()));
        return response;
    }

    @Override
    public AmendmentDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
        Timestamp fromTs = QiReportSupport.startOf(fromDate);
        Timestamp toTs = QiReportSupport.endOf(toDate);
        Session session = entityManager.unwrap(Session.class);

        // Loads the whole window (amendments are rare, max 366 days) and pages in
        // memory.
        @SuppressWarnings("unchecked")
        List<Object[]> noteRows = session
                .createNativeQuery("SELECT CAST(a.id AS varchar), s.accession_number, t.name,"
                        + " n.lastupdated, a.released_date" + " FROM note n JOIN analysis a ON a.id = n.reference_id"
                        + " JOIN sample_item si ON si.id = a.sampitem_id JOIN sample s ON s.id = si.samp_id"
                        + " JOIN test t ON t.id = a.test_id"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'E' AND n.text = :correctedText"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs ORDER BY n.lastupdated DESC")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("correctedText", correctedNoteText())
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        Map<String, Object[]> amendmentByAnalysis = fetchNewestResultAmendments(session, noteRows);

        List<AmendmentEvent> all = new ArrayList<>();
        for (Object[] row : noteRows) {
            String analysisId = (String) row[0];
            AmendmentEvent event = new AmendmentEvent();
            event.setAnalysisId(analysisId);
            event.setLabNumber((String) row[1]);
            event.setTestName((String) row[2]);
            event.setReleasedAt((Timestamp) row[4]);

            Object[] amendment = amendmentByAnalysis.get(analysisId);
            if (amendment != null) {
                event.setCurrentValue((String) amendment[1]);
                event.setAmendedAt((Timestamp) amendment[2]);
                event.setPriorValue(extractPriorValue((byte[]) amendment[3]));
                event.setAmendedBy((String) amendment[4]);
            } else {
                // Note exists but no value-change history row (e.g. amended via a
                // path that only touched other fields) — fall back to note time.
                event.setAmendedAt((Timestamp) row[3]);
            }

            if (event.getAmendedAt() != null && event.getReleasedAt() != null
                    && event.getAmendedAt().after(event.getReleasedAt())) {
                event.setMinutesToAmend(
                        (event.getAmendedAt().getTime() - event.getReleasedAt().getTime()) / (60 * 1000));
            }
            all.add(event);
        }

        AmendmentDetailResponse response = new AmendmentDetailResponse();
        response.setItems(QiReportSupport.page(all, page, pageSize));
        response.setTotalCount(all.size());
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public AmendmentTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, String interval) {
        Timestamp fromTs = QiReportSupport.startOf(fromDate);
        Timestamp toTs = QiReportSupport.endOf(toDate);
        Session session = entityManager.unwrap(Session.class);
        String unit = QiReportSupport.truncUnit(interval);

        @SuppressWarnings("unchecked")
        List<Object[]> amendedBuckets = session
                .createNativeQuery("SELECT date_trunc(:unit, n.lastupdated), COUNT(DISTINCT n.reference_id)"
                        + " FROM note n"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'E' AND n.text = :correctedText"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs GROUP BY 1")
                .setParameter("unit", unit).setParameter("analysisRef", analysisRefTableId())
                .setParameter("correctedText", correctedNoteText()).setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs).list();

        @SuppressWarnings("unchecked")
        List<Object[]> releasedBuckets = session
                .createNativeQuery("SELECT date_trunc(:unit, a.released_date), COUNT(*) FROM analysis a"
                        + " WHERE a.released_date >= :fromTs AND a.released_date < :toTs GROUP BY 1")
                .setParameter("unit", unit).setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        // union of period keys — an amendment can land in a bucket with no releases
        Map<String, long[]> byPeriod = new TreeMap<>();
        for (Object[] row : amendedBuckets) {
            byPeriod.computeIfAbsent(QiReportSupport.periodKey(row[0], interval),
                    k -> new long[2])[0] = ((Number) row[1]).longValue();
        }
        for (Object[] row : releasedBuckets) {
            byPeriod.computeIfAbsent(QiReportSupport.periodKey(row[0], interval),
                    k -> new long[2])[1] = ((Number) row[1]).longValue();
        }

        List<AmendmentTrendResponse.TrendPoint> points = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : byPeriod.entrySet()) {
            AmendmentTrendResponse.TrendPoint point = new AmendmentTrendResponse.TrendPoint();
            point.setPeriod(entry.getKey());
            point.setAmendedCount(entry.getValue()[0]);
            point.setReleasedCount(entry.getValue()[1]);
            point.setRatePercent(QiReportSupport.ratePercent(entry.getValue()[0], entry.getValue()[1]));
            points.add(point);
        }

        AmendmentTrendResponse response = new AmendmentTrendResponse();
        response.setPoints(points);
        return response;
    }

    @Override
    public AmendmentBreakdownResponse getBreakdown(LocalDate fromDate, LocalDate toDate) {
        Timestamp fromTs = QiReportSupport.startOf(fromDate);
        Timestamp toTs = QiReportSupport.endOf(toDate);
        Session session = entityManager.unwrap(Session.class);

        @SuppressWarnings("unchecked")
        List<Object[]> amendedByTest = session
                .createNativeQuery("SELECT CAST(t.id AS varchar), t.name, COUNT(DISTINCT n.reference_id)"
                        + " FROM note n JOIN analysis a ON a.id = n.reference_id JOIN test t ON t.id = a.test_id"
                        + " WHERE n.reference_table = :analysisRef AND n.note_type = 'E' AND n.text = :correctedText"
                        + " AND n.lastupdated >= :fromTs AND n.lastupdated < :toTs GROUP BY t.id, t.name")
                .setParameter("analysisRef", analysisRefTableId()).setParameter("correctedText", correctedNoteText())
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        @SuppressWarnings("unchecked")
        List<Object[]> releasedByTest = session
                .createNativeQuery(
                        "SELECT CAST(t.id AS varchar), COUNT(*)" + " FROM analysis a JOIN test t ON t.id = a.test_id"
                                + " WHERE a.released_date >= :fromTs AND a.released_date < :toTs GROUP BY t.id")
                .setParameter("fromTs", fromTs).setParameter("toTs", toTs).list();

        Map<String, Long> releasedCounts = new HashMap<>();
        for (Object[] row : releasedByTest) {
            releasedCounts.put((String) row[0], ((Number) row[1]).longValue());
        }

        // only amended tests appear — a full every-test list is noise here
        List<AmendmentBreakdownResponse.BreakdownRow> rows = new ArrayList<>();
        for (Object[] row : amendedByTest) {
            AmendmentBreakdownResponse.BreakdownRow breakdownRow = new AmendmentBreakdownResponse.BreakdownRow();
            breakdownRow.setTestName((String) row[1]);
            breakdownRow.setAmendedCount(((Number) row[2]).longValue());
            breakdownRow.setReleasedCount(releasedCounts.getOrDefault((String) row[0], 0L));
            breakdownRow.setRatePercent(
                    QiReportSupport.ratePercent(breakdownRow.getAmendedCount(), breakdownRow.getReleasedCount()));
            rows.add(breakdownRow);
        }
        rows.sort((a, b) -> a.getAmendedCount() != b.getAmendedCount()
                ? Long.compare(b.getAmendedCount(), a.getAmendedCount())
                : a.getTestName().compareTo(b.getTestName()));

        AmendmentBreakdownResponse response = new AmendmentBreakdownResponse();
        response.setRows(rows);
        return response;
    }

    /**
     * Newest value-changing 'U' history row per amended analysis, batched in one
     * query: [analysisId, currentValue, amendedAt, changesBlob, amenderName].
     */
    private Map<String, Object[]> fetchNewestResultAmendments(Session session, List<Object[]> noteRows) {
        Set<String> analysisIds = new HashSet<>();
        for (Object[] row : noteRows) {
            analysisIds.add((String) row[0]);
        }
        Map<String, Object[]> byAnalysis = new HashMap<>();
        if (analysisIds.isEmpty()) {
            return byAnalysis;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = session.createNativeQuery("SELECT CAST(r.analysis_id AS varchar), r.value, h.timestamp,"
                + " h.changes, TRIM(CONCAT(su.first_name, ' ', su.last_name))"
                + " FROM result r JOIN history h ON h.reference_id = r.id"
                + " LEFT JOIN system_user su ON su.id = h.sys_user_id"
                + " WHERE r.analysis_id IN (:analysisIds) AND h.reference_table = :resultRef AND h.activity = 'U'"
                + " AND h.changes IS NOT NULL ORDER BY h.timestamp DESC")
                .setParameterList("analysisIds", toLongs(analysisIds)).setParameter("resultRef", resultRefTableId())
                .list();

        for (Object[] row : rows) {
            String analysisId = (String) row[0];
            byte[] changes = (byte[]) row[3];
            // newest row wins; skip rows whose blob has no value change
            if (!byAnalysis.containsKey(analysisId) && extractPriorValue(changes) != null) {
                byAnalysis.put(analysisId, new Object[] { analysisId, row[1], row[2], changes, row[4] });
            }
        }
        return byAnalysis;
    }

    private List<Long> toLongs(Set<String> ids) {
        return ids.stream().map(Long::valueOf).toList();
    }

    private Long analysisRefTableId() {
        return QiReportSupport.refTableId(referenceTablesService, "ANALYSIS");
    }

    private Long resultRefTableId() {
        return QiReportSupport.refTableId(referenceTablesService, "RESULT");
    }

    // Caveat: corrected notes are stored in whatever server locale was active
    // when they were written. If a deployment switches locale, notes from before
    // the switch stop matching and the count drops.
    private String correctedNoteText() {
        return MessageUtil.getMessage("note.corrected.result");
    }

    /** History blobs hold pre-update values as newline-delimited XML tags. */
    static String extractPriorValue(byte[] changes) {
        if (changes == null) {
            return null;
        }
        String xml = new String(changes, StandardCharsets.UTF_8);
        int start = xml.indexOf("<value>");
        if (start < 0) {
            return null;
        }
        int end = xml.indexOf("</value>", start);
        if (end < 0) {
            return null;
        }
        return StringEscapeUtils.unescapeXml(xml.substring(start + "<value>".length(), end));
    }
}
