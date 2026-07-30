package org.openelisglobal.qaevent.criticalcallback.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackDetailResponse;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackEvent;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackSummaryResponse;
import org.openelisglobal.qaevent.criticalcallback.dao.CriticalCallbackDAO;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain (non-audited) service: a callback row is write-once and is itself the
 * record, so it skips AuditableBaseObjectServiceImpl and the reference_tables
 * registration that would require a numeric id.
 *
 * <p>
 * The compliance compute (OGC-714 read side) lives here rather than on a
 * parallel report service: the metric window and clock both anchor on
 * {@code analysis.released_date} (the C.4 outline §3.1/§3.3 — a callback made
 * before release counts as compliant via the negative delta), criticality is
 * recomputed from result_limits critical bounds with the same outside-band rule
 * the write side validates, and the numerator is EXISTS(CONFIRMED within SLA) —
 * immune to repeat attempt rows.
 */
@Service
public class CriticalCallbackServiceImpl extends BaseObjectServiceImpl<CriticalCallback, String>
        implements CriticalCallbackService {

    /**
     * Callback target window, the pass line for the compliance numerator and the
     * "over target" failure reason. TJC NPSG.02.03.01 makes the lab define this
     * timeframe rather than fixing 60 minutes. ponytail: property, not a qi_config
     * column — no per-test-section callback policy to model yet.
     */
    @Value("${org.openelisglobal.qi.callback.sla.minutes:60}")
    private int slaMinutes;

    /**
     * Released critical results in the window. Same predicate as the write side's
     * isCritical: numeric result, value stripped of comparators, at or beyond a
     * configured (finite) critical bound. Per-bound finiteness guards are
     * load-bearing — unconfigured bounds persist as ±Infinity, and an all-Infinity
     * low bound would otherwise match every result.
     */
    // ponytail: any-variant limit match (ignores gender/age discrimination — exact
    // for the common one-default-limit-per-test config); add SQL demographic
    // resolution if labs configure gender/age-specific criticals
    private static final String CRITICAL_FROM = " FROM analysis a JOIN result r ON r.analysis_id = a.id"
            + " JOIN result_limits rl ON rl.test_id = a.test_id"
            + " CROSS JOIN LATERAL (SELECT BTRIM(regexp_replace(r.value, '[<>]', '', 'g')) AS val) v";

    private static final String CRITICAL_WHERE = " WHERE a.released_date >= :fromTs AND a.released_date < :toTs"
            + " AND r.result_type = 'N'" + " AND v.val ~ '^-?[0-9]+(\\.[0-9]+)?$'"
            + " AND ((rl.low_critical > CAST('-Infinity' AS float8) AND rl.low_critical < CAST('Infinity' AS float8)"
            + " AND CAST(v.val AS float8) <= rl.low_critical)"
            + " OR (rl.high_critical > CAST('-Infinity' AS float8) AND rl.high_critical < CAST('Infinity' AS float8)"
            + " AND CAST(v.val AS float8) >= rl.high_critical))";

    private static final String CONFIRMED_IN_SLA = "EXISTS (SELECT 1 FROM critical_callback cc"
            + " WHERE cc.analysis_id = a.id AND cc.status = 'CONFIRMED'"
            + " AND cc.logged_at <= a.released_date + (:slaMinutes * interval '1 minute'))";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    protected CriticalCallbackDAO baseObjectDAO;

    @Autowired
    private QiConfigService qiConfigService;

    CriticalCallbackServiceImpl() {
        super(CriticalCallback.class);
    }

    @Override
    protected CriticalCallbackDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriticalCallback> getByAnalysisId(String analysisId) {
        return baseObjectDAO.getByAnalysisId(analysisId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getLoggedResultIds(Collection<String> resultIds) {
        return baseObjectDAO.getLoggedResultIds(resultIds);
    }

    @Override
    @Transactional(readOnly = true)
    public CallbackSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        ResolvedConfig config = qiConfigService.resolve(QiIndicator.CALLBACK.name(), null);
        CallbackSummaryResponse response = new CallbackSummaryResponse();
        response.setEnabled(config.isEnabled());
        response.setTarget(config.getTarget() == null ? null : config.getTarget().doubleValue());
        response.setSlaMinutes(slaMinutes);
        if (!config.isEnabled()) {
            // opt-in indicator: labs that never enable it pay zero query cost
            return response;
        }

        Session session = entityManager.unwrap(Session.class);
        Object[] counts = (Object[]) session
                .createNativeQuery(
                        "SELECT COUNT(DISTINCT a.id) AS critical_count," + " COUNT(DISTINCT a.id) FILTER (WHERE "
                                + CONFIRMED_IN_SLA + ") AS confirmed_count" + CRITICAL_FROM + CRITICAL_WHERE)
                .setParameter("fromTs", startOf(fromDate)).setParameter("toTs", endOf(toDate))
                .setParameter("slaMinutes", slaMinutes).uniqueResult();

        long critical = ((Number) counts[0]).longValue();
        long confirmed = ((Number) counts[1]).longValue();
        response.setCriticalCount(critical);
        response.setConfirmedCount(confirmed);
        if (critical > 0) {
            response.setCompliancePercent(
                    BigDecimal.valueOf(confirmed * 100.0 / critical).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CallbackDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
        CallbackDetailResponse response = new CallbackDetailResponse();
        response.setPage(page);
        response.setPageSize(pageSize);
        if (!qiConfigService.resolve(QiIndicator.CALLBACK.name(), null).isEnabled()) {
            response.setItems(List.of());
            return response;
        }

        Session session = entityManager.unwrap(Session.class);
        // ponytail: fetch-all + in-memory paging, same as the Amendment report;
        // criticals are rare and the window is capped at 366 days. DISTINCT ON
        // collapses result_limits variants; the lateral picks the latest attempt.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = session.createNativeQuery("SELECT DISTINCT ON (a.id) CAST(a.id AS varchar),"
                + " s.accession_number, t.name, COALESCE(cc.result_value, r.value),"
                + " rl.low_critical, rl.high_critical, a.released_date,"
                + " cc.recipient_name, cc.status, cc.logged_at, TRIM(CONCAT(su.first_name, ' ', su.last_name))"
                + CRITICAL_FROM + " JOIN sample_item si ON si.id = a.sampitem_id JOIN sample s ON s.id = si.samp_id"
                + " JOIN test t ON t.id = a.test_id"
                + " LEFT JOIN LATERAL (SELECT * FROM critical_callback c2 WHERE c2.analysis_id = a.id"
                + " ORDER BY c2.logged_at DESC LIMIT 1) cc ON true"
                + " LEFT JOIN system_user su ON su.id = cc.logged_by" + CRITICAL_WHERE + " ORDER BY a.id")
                .setParameter("fromTs", startOf(fromDate)).setParameter("toTs", endOf(toDate)).list();

        List<CallbackEvent> all = new ArrayList<>();
        for (Object[] row : rows) {
            CallbackEvent event = new CallbackEvent();
            event.setAnalysisId((String) row[0]);
            event.setLabNumber((String) row[1]);
            event.setTestName((String) row[2]);
            event.setResultValue((String) row[3]);
            event.setCriticalRange(criticalRange((Double) row[4], (Double) row[5]));
            event.setReleasedAt((Timestamp) row[6]);
            event.setRecipientName((String) row[7]);
            event.setStatus((String) row[8]);
            event.setLoggedAt((Timestamp) row[9]);
            event.setLoggedBy(blankToNull((String) row[10]));
            if (event.getLoggedAt() != null && event.getReleasedAt() != null) {
                event.setMinutesToCallback(
                        (event.getLoggedAt().getTime() - event.getReleasedAt().getTime()) / (60 * 1000));
            }
            all.add(event);
        }
        // gaps first (never-logged criticals are the actionable list), then newest
        all.sort((a, b) -> {
            boolean aLogged = a.getStatus() != null;
            boolean bLogged = b.getStatus() != null;
            if (aLogged != bLogged) {
                return aLogged ? 1 : -1;
            }
            return b.getReleasedAt().compareTo(a.getReleasedAt());
        });

        response.setAckDistribution(ackDistribution(all));
        response.setFailureCounts(failureCounts(all));

        int fromIndex = Math.min(page * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        response.setItems(all.subList(fromIndex, toIndex));
        response.setTotalCount(all.size());
        return response;
    }

    /**
     * Design §callback-detail histogram: CONFIRMED results bucketed by minutes from
     * release (a pre-release call lands in "0-5"), everything else "noAck". The
     * bucket edges are absolute latency, deliberately independent of
     * {@link #slaMinutes} — the histogram describes how fast calls happened, the
     * SLA decides which of them passed (see {@link #failureCounts}).
     */
    private static Map<String, Long> ackDistribution(List<CallbackEvent> events) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (String key : List.of("0-5", "5-15", "15-30", "30-60", "over60", "noAck")) {
            buckets.put(key, 0L);
        }
        for (CallbackEvent event : events) {
            buckets.merge(ackBucket(event), 1L, Long::sum);
        }
        return buckets;
    }

    private static String ackBucket(CallbackEvent event) {
        if (!"CONFIRMED".equals(event.getStatus()) || event.getMinutesToCallback() == null) {
            return "noAck";
        }
        long minutes = event.getMinutesToCallback();
        if (minutes < 5) {
            return "0-5";
        }
        if (minutes < 15) {
            return "5-15";
        }
        if (minutes < 30) {
            return "15-30";
        }
        if (minutes <= 60) {
            return "30-60";
        }
        return "over60";
    }

    /**
     * Non-compliant results by reason; compliant CONFIRMED-in-SLA rows are omitted.
     */
    private Map<String, Long> failureCounts(List<CallbackEvent> events) {
        Map<String, Long> reasons = new LinkedHashMap<>();
        for (String key : List.of("overTarget", "unableToReach", "noReadback", "noCallback")) {
            reasons.put(key, 0L);
        }
        for (CallbackEvent event : events) {
            String status = event.getStatus();
            if (status == null) {
                reasons.merge("noCallback", 1L, Long::sum);
            } else if ("UNABLE_TO_REACH".equals(status)) {
                reasons.merge("unableToReach", 1L, Long::sum);
            } else if ("REACHED_NO_READBACK".equals(status)) {
                reasons.merge("noReadback", 1L, Long::sum);
            } else if (event.getMinutesToCallback() != null && event.getMinutesToCallback() > slaMinutes) {
                reasons.merge("overTarget", 1L, Long::sum);
            }
        }
        return reasons;
    }

    private static Timestamp startOf(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private static Timestamp endOf(LocalDate date) {
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }

    /** "≤ low / ≥ high" from the configured (finite) critical bounds. */
    private static String criticalRange(Double low, Double high) {
        StringBuilder range = new StringBuilder();
        if (low != null && Double.isFinite(low)) {
            range.append("≤ ").append(trimTrailingZeros(low));
        }
        if (high != null && Double.isFinite(high)) {
            if (range.length() > 0) {
                range.append(" / ");
            }
            range.append("≥ ").append(trimTrailingZeros(high));
        }
        return range.length() == 0 ? null : range.toString();
    }

    private static String trimTrailingZeros(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
