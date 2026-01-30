package org.openelisglobal.tb.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of TB Reporting Service. Uses JdbcTemplate for efficient JSONB
 * queries on notebook_page_sample.data and standard queries on dedicated TB
 * tables.
 */
@Service
@Transactional(readOnly = true)
public class TbReportingServiceImpl implements TbReportingService {

    private static final int PAGE_REGISTRATION = 1;
    private static final int PAGE_QC = 2;
    private static final int PAGE_TEST_EXECUTION = 5;
    private static final int PAGE_DISPOSAL = 7;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TbReportingServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Map<String, Object> getSampleIntakeMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // Total received count
        String totalSql = """
                SELECT COUNT(*) FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                """;
        Long totalReceived = jdbcTemplate.queryForObject(totalSql, Long.class, notebookId, PAGE_REGISTRATION,
                Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("totalReceived", totalReceived != null ? totalReceived : 0L);

        // By specimen type
        String bySpecimenTypeSql = """
                SELECT nps.data->>'specimenType' as specimen_type, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'specimenType' IS NOT NULL
                GROUP BY nps.data->>'specimenType'
                """;
        Map<String, Long> bySpecimenType = new HashMap<>();
        jdbcTemplate.query(bySpecimenTypeSql, rs -> {
            bySpecimenType.put(rs.getString("specimen_type"), rs.getLong("count"));
        }, notebookId, PAGE_REGISTRATION, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("bySpecimenType", bySpecimenType);

        // By referring facility
        String byFacilitySql = """
                SELECT nps.data->>'referringFacility' as facility, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'referringFacility' IS NOT NULL
                GROUP BY nps.data->>'referringFacility'
                """;
        Map<String, Long> byReferringFacility = new HashMap<>();
        jdbcTemplate.query(byFacilitySql, rs -> {
            byReferringFacility.put(rs.getString("facility"), rs.getLong("count"));
        }, notebookId, PAGE_REGISTRATION, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("byReferringFacility", byReferringFacility);

        // By treatment history
        String byTreatmentSql = """
                SELECT nps.data->>'treatmentHistory' as treatment, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'treatmentHistory' IS NOT NULL
                GROUP BY nps.data->>'treatmentHistory'
                """;
        Map<String, Long> byTreatmentHistory = new HashMap<>();
        jdbcTemplate.query(byTreatmentSql, rs -> {
            byTreatmentHistory.put(rs.getString("treatment"), rs.getLong("count"));
        }, notebookId, PAGE_REGISTRATION, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("byTreatmentHistory", byTreatmentHistory);

        return metrics;
    }

    @Override
    public Map<String, Object> getQcMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // QC result counts
        String qcResultSql = """
                SELECT nps.data->>'qcResult' as qc_result, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND (nps.status = 'COMPLETED' OR nps.status = 'REJECTED')
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'qcResult' IS NOT NULL
                GROUP BY nps.data->>'qcResult'
                """;

        long totalChecked = 0;
        long passCount = 0;
        long passToStorageCount = 0;
        long failDiscardCount = 0;
        long failProceedCount = 0;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(qcResultSql, notebookId, PAGE_QC,
                Date.valueOf(startDate), Date.valueOf(endDate));

        for (Map<String, Object> row : results) {
            String qcResult = (String) row.get("qc_result");
            long count = ((Number) row.get("count")).longValue();
            totalChecked += count;

            switch (qcResult) {
            case "PASS" -> passCount = count;
            case "PASS_TO_STORAGE" -> passToStorageCount = count;
            case "FAIL_DISCARD" -> failDiscardCount = count;
            case "FAIL_PROCEED" -> failProceedCount = count;
            }
        }

        metrics.put("totalChecked", totalChecked);
        metrics.put("passCount", passCount + passToStorageCount);
        metrics.put("passRate", calculateRate(passCount + passToStorageCount, totalChecked));
        metrics.put("failDiscardCount", failDiscardCount);
        metrics.put("failProceedCount", failProceedCount);

        // By rejection reason
        String byRejectionSql = """
                SELECT nps.data->>'rejectionReason' as reason, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND (nps.status = 'COMPLETED' OR nps.status = 'REJECTED')
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'rejectionReason' IS NOT NULL
                GROUP BY nps.data->>'rejectionReason'
                """;
        Map<String, Long> byRejectionReason = new HashMap<>();
        jdbcTemplate.query(byRejectionSql, rs -> {
            byRejectionReason.put(rs.getString("reason"), rs.getLong("count"));
        }, notebookId, PAGE_QC, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("byRejectionReason", byRejectionReason);

        return metrics;
    }

    @Override
    public Map<String, Object> getCultureMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // Culture result counts from dedicated table, filtered by notebook via sample
        // linkage
        // Join through notebook_page_sample to filter by notebook
        String cultureSql = """
                SELECT tcr.culture_result, COUNT(DISTINCT tcr.id) as count,
                       AVG(tcr.positive_week) as avg_positive_week
                FROM clinlims.tb_culture_reading tcr
                JOIN clinlims.notebook_page_sample nps ON tcr.sample_item_id::varchar = nps.sample_item_id
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ?
                AND tcr.culture_result IS NOT NULL
                AND tcr.final_result_date BETWEEN ? AND ?
                GROUP BY tcr.culture_result
                """;

        long totalWithResults = 0;
        long positiveCount = 0;
        long negativeCount = 0;
        long contaminatedCount = 0;
        Double avgPositiveWeek = null;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(cultureSql, notebookId, Date.valueOf(startDate),
                Date.valueOf(endDate));

        for (Map<String, Object> row : results) {
            String cultureResult = (String) row.get("culture_result");
            long count = ((Number) row.get("count")).longValue();
            totalWithResults += count;

            switch (cultureResult) {
            case "POSITIVE" -> {
                positiveCount = count;
                Object avgWeek = row.get("avg_positive_week");
                if (avgWeek != null) {
                    avgPositiveWeek = ((Number) avgWeek).doubleValue();
                }
            }
            case "NEGATIVE" -> negativeCount = count;
            case "CONTAMINATED" -> contaminatedCount = count;
            }
        }

        metrics.put("totalWithResults", totalWithResults);
        metrics.put("positiveCount", positiveCount);
        metrics.put("positivityRate", calculateRate(positiveCount, totalWithResults));
        metrics.put("negativeCount", negativeCount);
        metrics.put("contaminatedCount", contaminatedCount);
        metrics.put("contaminationRate", calculateRate(contaminatedCount, totalWithResults));
        metrics.put("avgPositiveWeek", avgPositiveWeek != null ? round(avgPositiveWeek, 1) : null);

        return metrics;
    }

    @Override
    public Map<String, Object> getSmearMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // Smear results from JSONB
        String smearSql = """
                SELECT nps.data->>'afbResult' as afb_result, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'afbResult' IS NOT NULL
                GROUP BY nps.data->>'afbResult'
                """;

        long totalSmears = 0;
        long positiveCount = 0;
        Map<String, Long> byGrading = new HashMap<>();

        List<Map<String, Object>> results = jdbcTemplate.queryForList(smearSql, notebookId, PAGE_TEST_EXECUTION,
                Date.valueOf(startDate), Date.valueOf(endDate));

        for (Map<String, Object> row : results) {
            String afbResult = (String) row.get("afb_result");
            long count = ((Number) row.get("count")).longValue();
            totalSmears += count;
            byGrading.put(afbResult, count);

            // Count positive results (anything except NEGATIVE)
            if (!"NEGATIVE".equals(afbResult)) {
                positiveCount += count;
            }
        }

        metrics.put("totalSmears", totalSmears);
        metrics.put("positiveCount", positiveCount);
        metrics.put("positivityRate", calculateRate(positiveCount, totalSmears));
        metrics.put("byGrading", byGrading);

        // By method
        String byMethodSql = """
                SELECT nps.data->>'smearMethod' as method, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'smearMethod' IS NOT NULL
                GROUP BY nps.data->>'smearMethod'
                """;
        Map<String, Long> byMethod = new HashMap<>();
        jdbcTemplate.query(byMethodSql, rs -> {
            byMethod.put(rs.getString("method"), rs.getLong("count"));
        }, notebookId, PAGE_TEST_EXECUTION, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("byMethod", byMethod);

        return metrics;
    }

    @Override
    public Map<String, Object> getGeneXpertMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // GeneXpert results from JSONB
        String geneXpertSql = """
                SELECT nps.data->>'geneXpertResult' as result, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'geneXpertResult' IS NOT NULL
                GROUP BY nps.data->>'geneXpertResult'
                """;

        long totalTests = 0;
        long mtbDetectedCount = 0;
        long rifSensitiveCount = 0;
        long rifResistantCount = 0;
        long mtbNotDetectedCount = 0;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(geneXpertSql, notebookId, PAGE_TEST_EXECUTION,
                Date.valueOf(startDate), Date.valueOf(endDate));

        for (Map<String, Object> row : results) {
            String result = (String) row.get("result");
            long count = ((Number) row.get("count")).longValue();
            totalTests += count;

            switch (result) {
            case "MTB_NOT_DETECTED" -> mtbNotDetectedCount = count;
            case "MTB_DETECTED_RIF_SENSITIVE" -> {
                rifSensitiveCount = count;
                mtbDetectedCount += count;
            }
            case "MTB_DETECTED_RIF_RESISTANT" -> {
                rifResistantCount = count;
                mtbDetectedCount += count;
            }
            }
        }

        metrics.put("totalTests", totalTests);
        metrics.put("mtbDetectedCount", mtbDetectedCount);
        metrics.put("mtbDetectionRate", calculateRate(mtbDetectedCount, totalTests));
        metrics.put("mtbNotDetectedCount", mtbNotDetectedCount);
        metrics.put("rifSensitiveCount", rifSensitiveCount);
        metrics.put("rifResistantCount", rifResistantCount);
        metrics.put("rifResistanceRate", calculateRate(rifResistantCount, mtbDetectedCount));

        return metrics;
    }

    @Override
    public Map<String, Object> getDstMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // DST classification from JSONB
        String dstSql = """
                SELECT nps.data->>'dstClassification' as classification, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'dstClassification' IS NOT NULL
                GROUP BY nps.data->>'dstClassification'
                """;

        long totalDst = 0;
        long fullySensitiveCount = 0;
        long mdrCount = 0;
        long xdrCount = 0;
        Map<String, Long> byClassification = new HashMap<>();

        List<Map<String, Object>> results = jdbcTemplate.queryForList(dstSql, notebookId, PAGE_TEST_EXECUTION,
                Date.valueOf(startDate), Date.valueOf(endDate));

        for (Map<String, Object> row : results) {
            String classification = (String) row.get("classification");
            long count = ((Number) row.get("count")).longValue();
            totalDst += count;
            byClassification.put(classification, count);

            switch (classification) {
            case "FULLY_SENSITIVE" -> fullySensitiveCount = count;
            case "MDR" -> mdrCount = count;
            case "XDR" -> xdrCount = count;
            }
        }

        metrics.put("totalDst", totalDst);
        metrics.put("fullySensitiveCount", fullySensitiveCount);
        metrics.put("mdrCount", mdrCount);
        metrics.put("mdrRate", calculateRate(mdrCount, totalDst));
        metrics.put("xdrCount", xdrCount);
        metrics.put("xdrRate", calculateRate(xdrCount, totalDst));
        metrics.put("byClassification", byClassification);

        return metrics;
    }

    @Override
    public Map<String, Object> getTurnaroundTimeMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // Culture TAT from dedicated table, filtered by notebook via sample linkage
        // Note: In PostgreSQL, date - date returns an integer (days), not an interval
        String cultureTatSql = """
                SELECT
                    AVG(tcr.final_result_date - tcr.inoculation_date) as avg_tat,
                    MIN(tcr.final_result_date - tcr.inoculation_date) as min_tat,
                    MAX(tcr.final_result_date - tcr.inoculation_date) as max_tat
                FROM clinlims.tb_culture_reading tcr
                JOIN clinlims.notebook_page_sample nps ON tcr.sample_item_id::varchar = nps.sample_item_id
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ?
                AND tcr.culture_result IS NOT NULL
                AND tcr.inoculation_date IS NOT NULL
                AND tcr.final_result_date IS NOT NULL
                AND tcr.final_result_date BETWEEN ? AND ?
                """;

        Map<String, Object> tatResult = jdbcTemplate.queryForMap(cultureTatSql, notebookId, Date.valueOf(startDate),
                Date.valueOf(endDate));

        Double avgTat = tatResult.get("avg_tat") != null ? ((Number) tatResult.get("avg_tat")).doubleValue() : null;
        Double minTat = tatResult.get("min_tat") != null ? ((Number) tatResult.get("min_tat")).doubleValue() : null;
        Double maxTat = tatResult.get("max_tat") != null ? ((Number) tatResult.get("max_tat")).doubleValue() : null;

        metrics.put("cultureTatDays", avgTat != null ? round(avgTat, 1) : null);
        metrics.put("minCultureTatDays", minTat != null ? round(minTat, 1) : null);
        metrics.put("maxCultureTatDays", maxTat != null ? round(maxTat, 1) : null);

        return metrics;
    }

    @Override
    public Map<String, Object> getDisposalMetrics(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        // Disposal status counts
        String disposalStatusSql = """
                SELECT nps.data->>'disposalStatus' as disposal_status, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'disposalStatus' IS NOT NULL
                GROUP BY nps.data->>'disposalStatus'
                """;

        long totalProcessed = 0;
        long disposedCount = 0;
        long archivedCount = 0;

        List<Map<String, Object>> statusResults = jdbcTemplate.queryForList(disposalStatusSql, notebookId,
                PAGE_DISPOSAL, Date.valueOf(startDate), Date.valueOf(endDate));

        for (Map<String, Object> row : statusResults) {
            String status = (String) row.get("disposal_status");
            long count = ((Number) row.get("count")).longValue();
            totalProcessed += count;

            switch (status) {
            case "DISPOSED" -> disposedCount = count;
            case "ARCHIVED" -> archivedCount = count;
            }
        }

        metrics.put("totalProcessed", totalProcessed);
        metrics.put("disposedCount", disposedCount);
        metrics.put("archivedCount", archivedCount);

        // By disposal reason
        String byReasonSql = """
                SELECT nps.data->>'disposalReason' as reason, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'disposalReason' IS NOT NULL
                GROUP BY nps.data->>'disposalReason'
                """;
        Map<String, Long> byDisposalReason = new HashMap<>();
        jdbcTemplate.query(byReasonSql, rs -> {
            byDisposalReason.put(rs.getString("reason"), rs.getLong("count"));
        }, notebookId, PAGE_DISPOSAL, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("byDisposalReason", byDisposalReason);

        // By disposal method
        String byMethodSql = """
                SELECT nps.data->>'disposalMethod' as method, COUNT(*) as count
                FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'disposalMethod' IS NOT NULL
                GROUP BY nps.data->>'disposalMethod'
                """;
        Map<String, Long> byDisposalMethod = new HashMap<>();
        jdbcTemplate.query(byMethodSql, rs -> {
            byDisposalMethod.put(rs.getString("method"), rs.getLong("count"));
        }, notebookId, PAGE_DISPOSAL, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("byDisposalMethod", byDisposalMethod);

        // Count biorepository transfers (samples with biorepositoryTransferId)
        String biorepositoryCountSql = """
                SELECT COUNT(*) FROM clinlims.notebook_page_sample nps
                JOIN clinlims.notebook_page np ON nps.notebook_page_id = np.id
                WHERE np.notebook_id = ? AND np.page_order = ?
                AND nps.status = 'COMPLETED'
                AND DATE(nps.completed_at) BETWEEN ? AND ?
                AND nps.data->>'biorepositoryTransferId' IS NOT NULL
                """;
        Long biorepositoryTransferCount = jdbcTemplate.queryForObject(biorepositoryCountSql, Long.class, notebookId,
                PAGE_DISPOSAL, Date.valueOf(startDate), Date.valueOf(endDate));
        metrics.put("biorepositoryTransferCount", biorepositoryTransferCount != null ? biorepositoryTransferCount : 0L);

        return metrics;
    }

    @Override
    public Map<String, Object> getDashboardSummary(Integer notebookId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();

        // Aggregate key metrics from all methods
        Map<String, Object> intake = getSampleIntakeMetrics(notebookId, startDate, endDate);
        Map<String, Object> qc = getQcMetrics(notebookId, startDate, endDate);
        Map<String, Object> culture = getCultureMetrics(notebookId, startDate, endDate);
        Map<String, Object> smear = getSmearMetrics(notebookId, startDate, endDate);
        Map<String, Object> geneXpert = getGeneXpertMetrics(notebookId, startDate, endDate);
        Map<String, Object> dst = getDstMetrics(notebookId, startDate, endDate);
        Map<String, Object> tat = getTurnaroundTimeMetrics(notebookId, startDate, endDate);
        Map<String, Object> disposal = getDisposalMetrics(notebookId, startDate, endDate);

        summary.put("sampleIntake", intake);
        summary.put("qc", qc);
        summary.put("culture", culture);
        summary.put("smear", smear);
        summary.put("geneXpert", geneXpert);
        summary.put("dst", dst);
        summary.put("turnaroundTime", tat);
        summary.put("disposal", disposal);

        // Key summary numbers
        summary.put("totalSamplesReceived", intake.get("totalReceived"));
        summary.put("qcPassRate", qc.get("passRate"));
        summary.put("culturePositivityRate", culture.get("positivityRate"));
        summary.put("cultureContaminationRate", culture.get("contaminationRate"));
        summary.put("smearPositivityRate", smear.get("positivityRate"));
        summary.put("mtbDetectionRate", geneXpert.get("mtbDetectionRate"));
        summary.put("mdrRate", dst.get("mdrRate"));
        summary.put("avgCultureTatDays", tat.get("cultureTatDays"));
        summary.put("totalDisposed", disposal.get("disposedCount"));
        summary.put("totalArchived", disposal.get("archivedCount"));

        return summary;
    }

    /**
     * Calculate percentage rate with null safety.
     */
    private Double calculateRate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return round((double) numerator / denominator * 100, 1);
    }

    /**
     * Round a double to specified decimal places.
     */
    private Double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
