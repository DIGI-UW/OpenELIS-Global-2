package org.openelisglobal.compliance.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.compliance.controller.rest.dto.DashboardSummaryDTO;
import org.openelisglobal.compliance.controller.rest.dto.DashboardTrendDTO;
import org.openelisglobal.compliance.controller.rest.dto.MonthDataPointDTO;
import org.openelisglobal.compliance.controller.rest.dto.PagedExceedanceDTO;
import org.openelisglobal.compliance.controller.rest.dto.ParameterDataPointDTO;
import org.openelisglobal.compliance.controller.rest.dto.ParameterSeriesDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteComparisonDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteParameterTrendDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteSeriesDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceDashboardQueryServiceImpl implements ComplianceDashboardQueryService {

    private static final String ENV_SITE_TYPE = "envSamplingSiteId";
    private static final int LOW_DATA_THRESHOLD = 3;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // Compliance status computed from result.value vs result_limits bounds.
    // PASS  : value within [low_normal, high_normal]
    // FAIL  : value outside [low_critical, high_critical]  (critical breach)
    // MARGINAL: value outside normal but within critical bounds
    // NULL value or non-numeric: excluded from compliance calc
    private static final String COMPLIANCE_EXPR =
        "CASE "
        + "WHEN r.value ~ '^[0-9]+(\\.[0-9]+)?$' AND rl.id IS NOT NULL "
        + "  AND r.value::numeric >= rl.low_normal AND r.value::numeric <= rl.high_normal "
        + "  THEN 'PASS' "
        + "WHEN r.value ~ '^[0-9]+(\\.[0-9]+)?$' AND rl.id IS NOT NULL "
        + "  AND (r.value::numeric < rl.low_critical OR r.value::numeric > rl.high_critical) "
        + "  THEN 'FAIL' "
        + "WHEN r.value ~ '^[0-9]+(\\.[0-9]+)?$' AND rl.id IS NOT NULL "
        + "  THEN 'MARGINAL' "
        + "ELSE NULL END";

    // PASS + MARGINAL both count as passing per spec
    private static final String IS_PASSING =
        "r.value ~ '^[0-9]+(\\.[0-9]+)?$' AND rl.id IS NOT NULL "
        + "AND r.value::numeric >= rl.low_normal AND r.value::numeric <= rl.high_critical";

    // FAIL = outside critical bounds
    private static final String IS_FAILING =
        "r.value ~ '^[0-9]+(\\.[0-9]+)?$' AND rl.id IS NOT NULL "
        + "AND (r.value::numeric < rl.low_critical OR r.value::numeric > rl.high_critical)";

    // MARGINAL = outside normal but inside critical
    private static final String IS_MARGINAL =
        "r.value ~ '^[0-9]+(\\.[0-9]+)?$' AND rl.id IS NOT NULL "
        + "AND NOT (r.value::numeric >= rl.low_normal AND r.value::numeric <= rl.high_normal) "
        + "AND NOT (r.value::numeric < rl.low_critical OR r.value::numeric > rl.high_critical)";

    @PersistenceContext
    private EntityManager em;

    // Core FROM + JOIN block shared by all queries.
    // Links result → analysis → sample_item → sample → site (via observation_history)
    // and result → result_limits (for compliance bounds).
    // Optional standard filter is applied as a WHERE clause addition.
    private String coreFrom() {
        return "FROM clinlims.result r "
             + "JOIN clinlims.analysis a ON r.analysis_id = a.id "
             + "JOIN clinlims.test t ON a.test_id = t.id "
             + "JOIN clinlims.sample_item si ON a.sampitem_id = si.id "
             + "JOIN clinlims.sample s ON si.samp_id = s.id "
             + "JOIN clinlims.observation_history oh_site "
             + "  ON oh_site.sample_id = s.id "
             + "  AND oh_site.observation_history_type_id = "
             + "    (SELECT id FROM clinlims.observation_history_type WHERE type_name = '" + ENV_SITE_TYPE + "') "
             + "JOIN clinlims.vector_sampling_site vss ON vss.id::text = oh_site.value "
             + "LEFT JOIN clinlims.result_limits rl "
             + "  ON rl.test_id = a.test_id "
             + "  AND r.is_reportable = 'Y' ";
    }

    private void applyFilters(StringBuilder sql, List<String> siteIds, String standardId) {
        if (siteIds != null && !siteIds.isEmpty()) {
            sql.append("AND vss.id::text IN (:siteIds) ");
        }
        if (standardId != null && !standardId.isBlank()) {
            sql.append("AND EXISTS ("
                + "SELECT 1 FROM clinlims.sample_compliance_standards scs "
                + "WHERE scs.sample_id = s.id AND scs.compliance_standard_id::text = :standardId) ");
        }
    }

    private void setParams(Query q, List<String> siteIds, String standardId,
            LocalDate start, LocalDate end) {
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (siteIds != null && !siteIds.isEmpty()) q.setParameter("siteIds", siteIds);
        if (standardId != null && !standardId.isBlank()) q.setParameter("standardId", standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(List<String> siteIds, String standardId,
            LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(r.id), "
            + "SUM(CASE WHEN " + IS_PASSING + " THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN " + IS_FAILING + " THEN 1 ELSE 0 END), "
            + "COUNT(DISTINCT vss.id) ");
        sql.append(coreFrom());
        sql.append("WHERE s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);

        Object[] row = (Object[]) q.getSingleResult();
        long total   = row[0] == null ? 0L : ((Number) row[0]).longValue();
        long passing = row[1] == null ? 0L : ((Number) row[1]).longValue();
        long exceed  = row[2] == null ? 0L : ((Number) row[2]).longValue();
        int  sites   = row[3] == null ? 0  : ((Number) row[3]).intValue();

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setTotalOrders((int) total);
        dto.setTotalExceedances((int) exceed);
        dto.setSitesMonitored(sites);
        dto.setComplianceRate(total == 0 ? 0.0
            : BigDecimal.valueOf(passing * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP).doubleValue());

        // Trend delta vs prior equivalent period
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate priorEnd   = start.minusDays(1);
        LocalDate priorStart = priorEnd.minusDays(days - 1);
        DashboardSummaryDTO prior = getSummaryRaw(siteIds, standardId, priorStart, priorEnd);
        DashboardSummaryDTO.TrendDTO trend = new DashboardSummaryDTO.TrendDTO();
        trend.setTotalOrders(dto.getTotalOrders() - prior.getTotalOrders());
        trend.setTotalExceedances(dto.getTotalExceedances() - prior.getTotalExceedances());
        trend.setSitesMonitored(dto.getSitesMonitored() - prior.getSitesMonitored());
        trend.setComplianceRate(BigDecimal.valueOf(
                dto.getComplianceRate() - prior.getComplianceRate())
            .setScale(1, RoundingMode.HALF_UP).doubleValue());
        dto.setTrend(trend);
        return dto;
    }

    private DashboardSummaryDTO getSummaryRaw(List<String> siteIds, String standardId,
            LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(r.id), "
            + "SUM(CASE WHEN " + IS_PASSING + " THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN " + IS_FAILING + " THEN 1 ELSE 0 END), "
            + "COUNT(DISTINCT vss.id) ");
        sql.append(coreFrom());
        sql.append("WHERE s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);

        Object[] row = (Object[]) q.getSingleResult();
        long total   = row[0] == null ? 0L : ((Number) row[0]).longValue();
        long passing = row[1] == null ? 0L : ((Number) row[1]).longValue();
        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setTotalOrders((int) total);
        dto.setTotalExceedances(row[2] == null ? 0 : ((Number) row[2]).intValue());
        dto.setSitesMonitored(row[3] == null ? 0 : ((Number) row[3]).intValue());
        dto.setComplianceRate(total == 0 ? 0.0
            : BigDecimal.valueOf(passing * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP).doubleValue());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardTrendDTO getTrend(List<String> siteIds, String standardId,
            LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder(
            "SELECT TO_CHAR(s.collection_date, 'YYYY-MM') AS month, "
            + "vss.id::text, vss.name, vss.code, "
            + "COUNT(r.id), "
            + "SUM(CASE WHEN " + IS_PASSING + " THEN 1 ELSE 0 END) ");
        sql.append(coreFrom());
        sql.append("WHERE s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);
        sql.append("GROUP BY month, vss.id, vss.name, vss.code ORDER BY month, vss.id");

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);

        List<String> months = new ArrayList<>();
        LocalDate cur = start.withDayOfMonth(1);
        while (!cur.isAfter(end)) { months.add(cur.format(MONTH_FMT)); cur = cur.plusMonths(1); }

        Map<String, SiteSeriesDTO> siteMap = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            String month  = (String) r[0];
            String sid    = (String) r[1];
            String sname  = (String) r[2];
            String scode  = (String) r[3];
            long total    = ((Number) r[4]).longValue();
            long passing  = ((Number) r[5]).longValue();
            siteMap.computeIfAbsent(sid, id -> {
                SiteSeriesDTO s = new SiteSeriesDTO();
                s.setSiteId(id); s.setSiteName(sname); s.setSiteCode(scode);
                s.setDataPoints(new ArrayList<>()); return s;
            });
            MonthDataPointDTO pt = new MonthDataPointDTO();
            pt.setMonth(month); pt.setTotalResults((int) total);
            pt.setLowData(total < LOW_DATA_THRESHOLD);
            pt.setComplianceRate(total == 0 ? 0.0
                : BigDecimal.valueOf(passing * 100.0 / total)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue());
            siteMap.get(sid).getDataPoints().add(pt);
        }

        DashboardTrendDTO dto = new DashboardTrendDTO();
        dto.setMonths(months);
        dto.setSeries(new ArrayList<>(siteMap.values()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteParameterTrendDTO getSiteParameters(String siteId, String standardId,
            LocalDate start, LocalDate end) {
        Query snq = em.createNativeQuery(
            "SELECT name FROM clinlims.vector_sampling_site WHERE id::text = :sid");
        snq.setParameter("sid", siteId);
        String siteName;
        try { siteName = (String) snq.getSingleResult(); } catch (Exception e) { siteName = siteId; }

        StringBuilder sql = new StringBuilder(
            "SELECT TO_CHAR(s.collection_date, 'YYYY-MM') AS month, "
            + "t.name AS param_name, "
            + "AVG(CASE WHEN r.value ~ '^[0-9]+(\\.[0-9]+)?$' THEN r.value::numeric END) AS avg_val, "
            + "MAX(CASE WHEN r.value ~ '^[0-9]+(\\.[0-9]+)?$' THEN r.value::numeric END) AS max_val, "
            + "SUM(CASE WHEN " + IS_FAILING + " THEN 1 ELSE 0 END) AS exceedances, "
            + "MIN(rl.high_normal) AS threshold, "
            + "MIN(rl.high_critical) AS critical_threshold ");
        sql.append(coreFrom());
        sql.append("WHERE oh_site.value = :siteId "
            + "AND s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        if (standardId != null && !standardId.isBlank()) {
            sql.append("AND EXISTS (SELECT 1 FROM clinlims.sample_compliance_standards scs "
                + "WHERE scs.sample_id = s.id AND scs.compliance_standard_id::text = :standardId) ");
        }
        sql.append("GROUP BY month, t.name ORDER BY t.name, month");

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("siteId", siteId);
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (standardId != null && !standardId.isBlank()) q.setParameter("standardId", standardId);

        Map<String, ParameterSeriesDTO> paramMap = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            String pname = (String) r[1];
            paramMap.computeIfAbsent(pname, n -> {
                ParameterSeriesDTO p = new ParameterSeriesDTO();
                p.setParameterCode(n); p.setDisplayName(n);
                if (r[5] != null) p.setThreshold(
                    BigDecimal.valueOf(((Number) r[5]).doubleValue()).setScale(3, RoundingMode.HALF_UP));
                p.setThresholdType("MAXIMUM");
                p.setDataPoints(new ArrayList<>());
                return p;
            });
            ParameterDataPointDTO pt = new ParameterDataPointDTO();
            pt.setMonth((String) r[0]);
            if (r[2] != null) pt.setAvgValue(
                BigDecimal.valueOf(((Number) r[2]).doubleValue()).setScale(3, RoundingMode.HALF_UP).doubleValue());
            if (r[3] != null) pt.setMaxValue(
                BigDecimal.valueOf(((Number) r[3]).doubleValue()).setScale(3, RoundingMode.HALF_UP).doubleValue());
            pt.setExceedanceCount(r[4] == null ? 0 : ((Number) r[4]).intValue());
            paramMap.get(pname).getDataPoints().add(pt);
        }

        SiteParameterTrendDTO dto = new SiteParameterTrendDTO();
        dto.setSiteId(siteId); dto.setSiteName(siteName);
        dto.setParameters(new ArrayList<>(paramMap.values()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteComparisonDTO> getSiteComparison(String standardId,
            LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder(
            "SELECT vss.id::text, vss.name, COUNT(r.id), "
            + "SUM(CASE WHEN " + IS_PASSING + " THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN " + IS_FAILING + " THEN 1 ELSE 0 END) ");
        sql.append(coreFrom());
        sql.append("WHERE s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, null, standardId);
        sql.append("GROUP BY vss.id, vss.name "
            + "ORDER BY SUM(CASE WHEN " + IS_PASSING + " THEN 1 ELSE 0 END) "
            + "* 100.0 / NULLIF(COUNT(r.id), 0) ASC");

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (standardId != null && !standardId.isBlank()) q.setParameter("standardId", standardId);

        List<SiteComparisonDTO> result = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            long total   = ((Number) r[2]).longValue();
            long passing = ((Number) r[3]).longValue();
            double rate  = total == 0 ? 0.0
                : BigDecimal.valueOf(passing * 100.0 / total)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            SiteComparisonDTO dto = new SiteComparisonDTO();
            dto.setSiteId((String) r[0]); dto.setSiteName((String) r[1]);
            dto.setTotalOrders((int) total);
            dto.setExceedances(r[4] == null ? 0 : ((Number) r[4]).intValue());
            dto.setComplianceRate(rate);
            dto.setColorBand(rate >= 90.0 ? SiteComparisonDTO.ColorBand.GREEN
                : rate >= 70.0 ? SiteComparisonDTO.ColorBand.YELLOW
                : SiteComparisonDTO.ColorBand.RED);
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedExceedanceDTO getExceedances(List<String> siteIds, String standardId,
            LocalDate start, LocalDate end, int page, int size, String sortBy, String sortDir) {
        String orderCol = "parameter".equals(sortBy) ? "t.name"
            : "site".equals(sortBy)      ? "vss.name"
            : "status".equals(sortBy)    ? "compliance_status"
            : "s.collection_date";
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder sql = new StringBuilder(
            "SELECT s.collection_date, s.accession_number, "
            + "vss.id::text, vss.name, t.name, r.value, "
            + COMPLIANCE_EXPR + " AS compliance_status, "
            + "rl.high_normal, rl.high_critical ");
        sql.append(coreFrom());
        sql.append("WHERE s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);
        // Exceedances only: FAIL or MARGINAL
        sql.append("AND (" + IS_FAILING + " OR " + IS_MARGINAL + ") ");
        sql.append("ORDER BY ").append(orderCol).append(" ").append(dir)
           .append(" LIMIT :lim OFFSET :off");

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);
        q.setParameter("lim", size);
        q.setParameter("off", page * size);

        // Count query
        StringBuilder cntSql = new StringBuilder("SELECT COUNT(*) FROM (SELECT r.id ");
        cntSql.append(coreFrom());
        cntSql.append("WHERE s.collection_date BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(cntSql, siteIds, standardId);
        cntSql.append("AND (" + IS_FAILING + " OR " + IS_MARGINAL + ")) subq");
        Query cq = em.createNativeQuery(cntSql.toString());
        setParams(cq, siteIds, standardId, start, end);
        int total = ((Number) cq.getSingleResult()).intValue();

        List<PagedExceedanceDTO.ExceedanceItemDTO> items = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            PagedExceedanceDTO.ExceedanceItemDTO item = new PagedExceedanceDTO.ExceedanceItemDTO();
            item.setDate(r[0] == null ? "" : r[0].toString());
            item.setLabNumber(r[1] == null ? "" : (String) r[1]);
            item.setSiteId((String) r[2]);
            item.setSiteName((String) r[3]);
            item.setParameter((String) r[4]);
            item.setResult(r[5] == null ? "" : (String) r[5]);
            // Threshold: show high_normal for MARGINAL, high_critical for FAIL
            String status = (String) r[6];
            Double highNormal   = r[7] == null ? null : ((Number) r[7]).doubleValue();
            Double highCritical = r[8] == null ? null : ((Number) r[8]).doubleValue();
            if ("FAIL".equals(status) && highCritical != null) {
                item.setThreshold("≤" + highCritical);
            } else if (highNormal != null) {
                item.setThreshold("≤" + highNormal);
            }
            item.setStatus(status == null ? "MARGINAL" : status);
            items.add(item);
        }

        PagedExceedanceDTO dto = new PagedExceedanceDTO();
        dto.setTotalCount(total); dto.setPage(page); dto.setPageSize(size); dto.setItems(items);
        return dto;
    }
}
