package org.openelisglobal.reports.amendment.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentEvent;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
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
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
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
        if (released.longValue() > 0) {
            response.setRatePercent(BigDecimal.valueOf(amended.longValue() * 100.0 / released.longValue())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return response;
    }

    @Override
    public AmendmentDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
        Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(toDate.plusDays(1).atStartOfDay());
        Session session = entityManager.unwrap(Session.class);

        // ponytail: fetch-all + in-memory paging, same as the TAT report;
        // amendments are rare and the window is capped at 366 days.
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

        int fromIndex = Math.min(page * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());

        AmendmentDetailResponse response = new AmendmentDetailResponse();
        response.setItems(all.subList(fromIndex, toIndex));
        response.setTotalCount(all.size());
        response.setPage(page);
        response.setPageSize(pageSize);
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
        List<Long> longs = new ArrayList<>();
        for (String id : ids) {
            longs.add(Long.valueOf(id));
        }
        return longs;
    }

    private Long analysisRefTableId() {
        return Long.valueOf(referenceTablesService.getReferenceTableByName("ANALYSIS").getId());
    }

    private Long resultRefTableId() {
        return Long.valueOf(referenceTablesService.getReferenceTableByName("RESULT").getId());
    }

    // ponytail: the corrected note stores the message resolved in the server
    // locale at write time; a deployment that switches locale undercounts
    // pre-switch notes. Revisit if multi-locale history matters.
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
        return xml.substring(start + "<value>".length(), end).replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
    }
}
