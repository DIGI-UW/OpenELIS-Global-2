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

    // Two LATERAL subqueries per (result, sample) pair, mirroring
    // ComplianceEvaluationServiceImpl which checks all thresholds for a test:
    //
    // primary_ct: the single primary pass/fail rule (RANGE/MIN/MAX/EXACT).
    // RANGE preferred over MAX over MIN over EXACT — matches ThresholdType
    // priority.
    //
    // borderline_ct: an optional BORDERLINE advisory zone for the same test.
    // When a value fails the primary rule but falls within the BORDERLINE
    // zone it is MARGINAL rather than FAIL (same as isBorderline() in the service).
    private static final String THRESHOLD_JOIN = "LEFT JOIN LATERAL (" + "  SELECT ct.threshold_type AS ct_type, "
            + "         ct.min_value      AS ct_min, " + "         ct.max_value      AS ct_max, "
            + "         ct.target_value   AS ct_target " + "  FROM clinlims.compliance_threshold ct "
            + "  JOIN clinlims.parameter_group pg ON pg.id = ct.group_id "
            + "  JOIN clinlims.sample_compliance_standards scs2 " + "       ON scs2.sample_id = s.id "
            + "       AND scs2.compliance_standard_id = pg.standard_id " + "  WHERE (ct.test_id = a.test_id "
            + "         OR (ct.test_id IS NULL " + "             AND LOWER(ct.parameter_code) = LOWER(t.name))) "
            + "    AND ct.threshold_type NOT IN ('BORDERLINE','DESCRIPTIVE','SELECT_MAP') "
            + "    AND ct.is_active = TRUE AND ct.archived = FALSE " + "  ORDER BY " + "    CASE ct.threshold_type "
            + "      WHEN 'RANGE' THEN 1 WHEN 'MAXIMUM' THEN 2 "
            + "      WHEN 'MINIMUM' THEN 3 WHEN 'EXACT' THEN 4 END " + "  LIMIT 1" + ") primary_ct ON TRUE "
            + "LEFT JOIN LATERAL (" + "  SELECT ct.min_value AS bl_min, ct.max_value AS bl_max "
            + "  FROM clinlims.compliance_threshold ct " + "  JOIN clinlims.parameter_group pg ON pg.id = ct.group_id "
            + "  JOIN clinlims.sample_compliance_standards scs2 " + "       ON scs2.sample_id = s.id "
            + "       AND scs2.compliance_standard_id = pg.standard_id " + "  WHERE (ct.test_id = a.test_id "
            + "         OR (ct.test_id IS NULL " + "             AND LOWER(ct.parameter_code) = LOWER(t.name))) "
            + "    AND ct.threshold_type = 'BORDERLINE' " + "    AND ct.is_active = TRUE AND ct.archived = FALSE "
            + "  LIMIT 1" + ") borderline_ct ON TRUE ";

    private static final String IS_NUMERIC = "r.value ~ '^-?[0-9]+(\\.[0-9]+)?$'";

    // Passes primary threshold (RANGE/MIN/MAX/EXACT)
    private static final String PRIMARY_PASSES = "("
            + "  (primary_ct.ct_type = 'RANGE'   AND CAST(r.value AS numeric) BETWEEN primary_ct.ct_min AND primary_ct.ct_max) OR "
            + "  (primary_ct.ct_type = 'MINIMUM' AND CAST(r.value AS numeric) >= primary_ct.ct_min) OR "
            + "  (primary_ct.ct_type = 'MAXIMUM' AND CAST(r.value AS numeric) <= primary_ct.ct_max) OR "
            + "  (primary_ct.ct_type = 'EXACT'   AND CAST(r.value AS numeric) = primary_ct.ct_target)" + ")";

    // Has an applicable primary threshold
    private static final String HAS_PRIMARY = "primary_ct.ct_type IS NOT NULL";

    // Within the BORDERLINE advisory zone (value outside primary but inside
    // borderline bounds)
    private static final String IN_BORDERLINE = "borderline_ct.bl_min IS NOT NULL "
            + "AND CAST(r.value AS numeric) BETWEEN borderline_ct.bl_min AND borderline_ct.bl_max";

    private static final String IS_PASSING = IS_NUMERIC + " AND " + HAS_PRIMARY + " AND " + PRIMARY_PASSES;

    // FAIL: outside primary AND outside borderline advisory zone (or no borderline
    // defined)
    private static final String IS_FAILING = IS_NUMERIC + " AND " + HAS_PRIMARY + " AND NOT " + PRIMARY_PASSES
            + " AND NOT (" + IN_BORDERLINE + ")";

    // MARGINAL: outside primary but within the BORDERLINE advisory zone
    private static final String IS_MARGINAL = IS_NUMERIC + " AND " + HAS_PRIMARY + " AND NOT " + PRIMARY_PASSES
            + " AND (" + IN_BORDERLINE + ")";

    private static final String COMPLIANCE_EXPR = "CASE " + "WHEN " + IS_FAILING + " THEN 'FAIL' " + "WHEN "
            + IS_MARGINAL + " THEN 'MARGINAL' " + "WHEN " + IS_PASSING + " THEN 'PASS' " + "ELSE NULL END";

    @PersistenceContext
    private EntityManager em;

    // Core FROM + JOIN block shared by all queries.
    // result → analysis → test → sample_item → sample → site (via obs_history)
    // THRESHOLD_JOIN resolves compliance_threshold for each (result, sample) pair.
    private String coreFrom() {
        return "FROM clinlims.result r " + "JOIN clinlims.analysis a ON r.analysis_id = a.id AND r.is_reportable = 'Y' "
                + "JOIN clinlims.test t ON a.test_id = t.id " + "JOIN clinlims.sample_item si ON a.sampitem_id = si.id "
                + "JOIN clinlims.sample s ON si.samp_id = s.id " + "JOIN clinlims.observation_history oh_site "
                + "  ON oh_site.sample_id = s.id " + "  AND oh_site.observation_history_type_id = "
                + "    (SELECT id FROM clinlims.observation_history_type WHERE type_name = '" + ENV_SITE_TYPE + "') "
                + "JOIN clinlims.vector_sampling_site vss ON CAST(vss.id AS text) = oh_site.value " + THRESHOLD_JOIN;
    }

    private void applyFilters(StringBuilder sql, List<String> siteIds, String standardId) {
        if (siteIds != null && !siteIds.isEmpty()) {
            sql.append("AND CAST(vss.id AS text) IN (:siteIds) ");
        }
        if (standardId != null && !standardId.isBlank()) {
            sql.append("AND EXISTS (" + "SELECT 1 FROM clinlims.sample_compliance_standards scs "
                    + "WHERE scs.sample_id = s.id AND CAST(scs.compliance_standard_id AS text) = :standardId) ");
        }
    }

    private void setParams(Query q, List<String> siteIds, String standardId, LocalDate start, LocalDate end) {
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (siteIds != null && !siteIds.isEmpty())
            q.setParameter("siteIds", siteIds);
        if (standardId != null && !standardId.isBlank())
            q.setParameter("standardId", standardId);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(List<String> siteIds, String standardId, LocalDate start, LocalDate end) {
        DashboardSummaryDTO dto = getSummaryRaw(siteIds, standardId, start, end);

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate priorEnd = start.minusDays(1);
        LocalDate priorStart = priorEnd.minusDays(days - 1);
        DashboardSummaryDTO prior = getSummaryRaw(siteIds, standardId, priorStart, priorEnd);

        DashboardSummaryDTO.TrendDTO trend = new DashboardSummaryDTO.TrendDTO();
        trend.setTotalOrders(dto.getTotalOrders() - prior.getTotalOrders());
        trend.setTotalExceedances(dto.getTotalExceedances() - prior.getTotalExceedances());
        trend.setSitesMonitored(dto.getSitesMonitored() - prior.getSitesMonitored());
        trend.setComplianceRate(BigDecimal.valueOf(dto.getComplianceRate() - prior.getComplianceRate())
                .setScale(1, RoundingMode.HALF_UP).doubleValue());
        dto.setTrend(trend);
        return dto;
    }

    private DashboardSummaryDTO getSummaryRaw(List<String> siteIds, String standardId, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(r.id) AS total_cnt, " + "SUM(CASE WHEN " + IS_PASSING
                + " THEN 1 ELSE 0 END) AS passing_cnt, " + "SUM(CASE WHEN (" + IS_FAILING + ") OR (" + IS_MARGINAL
                + ") THEN 1 ELSE 0 END) AS exceedance_cnt, " + "COUNT(DISTINCT vss.id) AS site_cnt ");
        sql.append(coreFrom());
        sql.append("WHERE CAST(si.collection_date AS date) BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);

        Object[] row = (Object[]) q.getSingleResult();
        long total = row[0] == null ? 0L : ((Number) row[0]).longValue();
        long passing = row[1] == null ? 0L : ((Number) row[1]).longValue();

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setTotalOrders((int) total);
        dto.setTotalExceedances(row[2] == null ? 0 : ((Number) row[2]).intValue());
        dto.setSitesMonitored(row[3] == null ? 0 : ((Number) row[3]).intValue());
        dto.setComplianceRate(total == 0 ? 0.0
                : BigDecimal.valueOf(passing * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardTrendDTO getTrend(List<String> siteIds, String standardId, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder(
                "SELECT TO_CHAR(CAST(si.collection_date AS date), 'YYYY-MM-DD') AS coll_date, "
                        + "CAST(vss.id AS text) AS site_id, vss.name AS site_name, vss.code AS site_code, "
                        + "COUNT(r.id) AS total_cnt, " + "SUM(CASE WHEN " + IS_PASSING
                        + " THEN 1 ELSE 0 END) AS passing_cnt ");
        sql.append(coreFrom());
        sql.append("WHERE CAST(si.collection_date AS date) BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);
        sql.append("GROUP BY CAST(si.collection_date AS date), vss.id, vss.name, vss.code "
                + "ORDER BY CAST(si.collection_date AS date), vss.id");

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);

        Map<String, SiteSeriesDTO> siteMap = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            String date = (String) r[0];
            String sid = (String) r[1];
            String sname = (String) r[2];
            String scode = (String) r[3];
            long total = ((Number) r[4]).longValue();
            long passing = ((Number) r[5]).longValue();
            siteMap.computeIfAbsent(sid, id -> {
                SiteSeriesDTO s = new SiteSeriesDTO();
                s.setSiteId(id);
                s.setSiteName(sname);
                s.setSiteCode(scode);
                s.setDataPoints(new ArrayList<>());
                return s;
            });
            MonthDataPointDTO pt = new MonthDataPointDTO();
            pt.setMonth(date);
            pt.setTotalResults((int) total);
            pt.setLowData(total < LOW_DATA_THRESHOLD);
            pt.setComplianceRate(total == 0 ? 0.0
                    : BigDecimal.valueOf(passing * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue());
            siteMap.get(sid).getDataPoints().add(pt);
        }

        DashboardTrendDTO dto = new DashboardTrendDTO();
        dto.setSeries(new ArrayList<>(siteMap.values()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteParameterTrendDTO getSiteParameters(String siteId, String standardId, LocalDate start, LocalDate end) {
        Query snq = em
                .createNativeQuery("SELECT name FROM clinlims.vector_sampling_site WHERE CAST(id AS text) = :sid");
        snq.setParameter("sid", siteId);
        String siteName;
        try {
            siteName = (String) snq.getSingleResult();
        } catch (Exception e) {
            siteName = siteId;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT TO_CHAR(CAST(si.collection_date AS date), 'YYYY-MM-DD') AS coll_date, "
                        + "t.name AS param_name, " + "AVG(CASE WHEN " + IS_NUMERIC
                        + " THEN CAST(r.value AS numeric) END) AS avg_val, " + "MAX(CASE WHEN " + IS_NUMERIC
                        + " THEN CAST(r.value AS numeric) END) AS max_val, " + "SUM(CASE WHEN " + IS_FAILING
                        + " THEN 1 ELSE 0 END) AS exceedances, " + "MIN(primary_ct.ct_max) AS threshold_max, "
                        + "MIN(primary_ct.ct_min) AS threshold_min, " + "MIN(primary_ct.ct_type) AS threshold_type ");
        sql.append(coreFrom());
        sql.append("WHERE oh_site.value = :siteId "
                + "AND CAST(si.collection_date AS date) BETWEEN :start AND :end AND s.domain = 'E' ");
        if (standardId != null && !standardId.isBlank()) {
            sql.append("AND EXISTS (SELECT 1 FROM clinlims.sample_compliance_standards scs "
                    + "WHERE scs.sample_id = s.id AND CAST(scs.compliance_standard_id AS text) = :standardId) ");
        }
        sql.append("GROUP BY CAST(si.collection_date AS date), t.name "
                + "ORDER BY t.name, CAST(si.collection_date AS date)");

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("siteId", siteId);
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (standardId != null && !standardId.isBlank())
            q.setParameter("standardId", standardId);

        Map<String, ParameterSeriesDTO> paramMap = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            String pname = (String) r[1];
            paramMap.computeIfAbsent(pname, n -> {
                ParameterSeriesDTO p = new ParameterSeriesDTO();
                p.setParameterCode(n);
                p.setDisplayName(n);
                String ttype = r[7] != null ? (String) r[7] : "MAXIMUM";
                // Show the relevant bound as the threshold reference line
                Number bound = "MINIMUM".equals(ttype) ? (Number) r[6] : (Number) r[5];
                if (bound != null)
                    p.setThreshold(BigDecimal.valueOf(bound.doubleValue()).setScale(3, RoundingMode.HALF_UP));
                p.setThresholdType(ttype);
                p.setDataPoints(new ArrayList<>());
                return p;
            });
            ParameterDataPointDTO pt = new ParameterDataPointDTO();
            pt.setMonth((String) r[0]);
            if (r[2] != null)
                pt.setAvgValue(BigDecimal.valueOf(((Number) r[2]).doubleValue()).setScale(3, RoundingMode.HALF_UP)
                        .doubleValue());
            if (r[3] != null)
                pt.setMaxValue(BigDecimal.valueOf(((Number) r[3]).doubleValue()).setScale(3, RoundingMode.HALF_UP)
                        .doubleValue());
            pt.setExceedanceCount(r[4] == null ? 0 : ((Number) r[4]).intValue());
            paramMap.get(pname).getDataPoints().add(pt);
        }

        SiteParameterTrendDTO dto = new SiteParameterTrendDTO();
        dto.setSiteId(siteId);
        dto.setSiteName(siteName);
        dto.setParameters(new ArrayList<>(paramMap.values()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteComparisonDTO> getSiteComparison(String standardId, LocalDate start, LocalDate end) {
        StringBuilder inner = new StringBuilder(
                "SELECT CAST(vss.id AS text) AS site_id, vss.name AS site_name, COUNT(r.id) AS total_cnt, "
                        + "SUM(CASE WHEN " + IS_PASSING + " THEN 1 ELSE 0 END) AS passing_cnt, " + "SUM(CASE WHEN ("
                        + IS_FAILING + ") OR (" + IS_MARGINAL + ") THEN 1 ELSE 0 END) AS failing_cnt ");
        inner.append(coreFrom());
        inner.append("WHERE CAST(si.collection_date AS date) BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(inner, null, standardId);
        inner.append("GROUP BY vss.id, vss.name");

        StringBuilder sql = new StringBuilder("SELECT site_id, site_name, total_cnt, passing_cnt, failing_cnt FROM (")
                .append(inner).append(") subq ORDER BY passing_cnt * 100.0 / NULLIF(total_cnt, 0) ASC");

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("start", start);
        q.setParameter("end", end);
        if (standardId != null && !standardId.isBlank())
            q.setParameter("standardId", standardId);

        List<SiteComparisonDTO> result = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) q.getResultList();
        for (Object[] r : rows) {
            long total = ((Number) r[2]).longValue();
            long passing = ((Number) r[3]).longValue();
            double rate = total == 0 ? 0.0
                    : BigDecimal.valueOf(passing * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
            SiteComparisonDTO dto = new SiteComparisonDTO();
            dto.setSiteId((String) r[0]);
            dto.setSiteName((String) r[1]);
            dto.setTotalOrders((int) total);
            dto.setExceedances(r[4] == null ? 0 : ((Number) r[4]).intValue());
            dto.setComplianceRate(rate);
            dto.setColorBand(rate >= 90.0 ? SiteComparisonDTO.ColorBand.GREEN
                    : rate >= 70.0 ? SiteComparisonDTO.ColorBand.YELLOW : SiteComparisonDTO.ColorBand.RED);
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedExceedanceDTO getExceedances(List<String> siteIds, String standardId, LocalDate start, LocalDate end,
            int page, int size, String sortBy, String sortDir) {
        String orderCol = "parameter".equals(sortBy) ? "param_name"
                : "site".equals(sortBy) ? "site_name"
                        : "status".equals(sortBy) ? "compliance_status" : "collection_date";
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        // Threshold display: for RANGE show "min–max", for MIN show "≥min", for MAX
        // show "≤max"
        String thresholdExpr = "CASE primary_ct.ct_type "
                + "WHEN 'RANGE'   THEN CAST(primary_ct.ct_min AS text) || ' to ' || CAST(primary_ct.ct_max AS text) "
                + "WHEN 'MINIMUM' THEN '>= ' || CAST(primary_ct.ct_min AS text) "
                + "WHEN 'MAXIMUM' THEN '<= ' || CAST(primary_ct.ct_max AS text) "
                + "WHEN 'EXACT'   THEN '= ' || CAST(primary_ct.ct_target AS text) " + "ELSE NULL END";

        StringBuilder sql = new StringBuilder("SELECT si.collection_date, s.accession_number, "
                + "CAST(vss.id AS text) AS site_id, vss.name AS site_name, "
                + "t.name AS param_name, r.value AS result_val, " + COMPLIANCE_EXPR + " AS compliance_status, "
                + thresholdExpr + " AS threshold_display ");
        sql.append(coreFrom());
        sql.append("WHERE CAST(si.collection_date AS date) BETWEEN :start AND :end AND s.domain = 'E' ");
        applyFilters(sql, siteIds, standardId);
        sql.append("AND (" + IS_FAILING + " OR " + IS_MARGINAL + ") ");
        sql.append("ORDER BY ").append(orderCol).append(" ").append(dir).append(" LIMIT :lim OFFSET :off");

        Query q = em.createNativeQuery(sql.toString());
        setParams(q, siteIds, standardId, start, end);
        q.setParameter("lim", size);
        q.setParameter("off", page * size);

        StringBuilder cntSql = new StringBuilder("SELECT COUNT(*) FROM (SELECT r.id ");
        cntSql.append(coreFrom());
        cntSql.append("WHERE CAST(si.collection_date AS date) BETWEEN :start AND :end AND s.domain = 'E' ");
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
            item.setStatus(r[6] == null ? "MARGINAL" : (String) r[6]);
            item.setThreshold(r[7] == null ? "" : (String) r[7]);
            items.add(item);
        }

        PagedExceedanceDTO dto = new PagedExceedanceDTO();
        dto.setTotalCount(total);
        dto.setPage(page);
        dto.setPageSize(size);
        dto.setItems(items);
        return dto;
    }
}
