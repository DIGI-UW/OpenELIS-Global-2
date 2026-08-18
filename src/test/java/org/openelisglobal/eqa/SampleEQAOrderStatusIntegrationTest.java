package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * D-LIVE-2 (T-02): EQA order status must follow the order's analyses through
 * the result pipeline instead of sitting on PENDING until the deadline passes.
 * Runs against the real schema: fixture analyses are moved through real
 * {@code status_of_sample} ids resolved via {@link IStatusService}.
 */
public class SampleEQAOrderStatusIntegrationTest extends BaseWebContextSensitiveTest {

    /** Fixture sample carrying analyses 1 and 2. */
    private static final Long SAMPLE_WITH_TWO_ANALYSES = 1L;
    /** Fixture sample carrying analysis 3 only. */
    private static final Long SAMPLE_WITH_ONE_ANALYSIS = 2L;
    /** Fixture sample with no analyses at all. */
    private static final Long SAMPLE_WITHOUT_ANALYSES = 3L;

    @Autowired
    private SampleEQAService sampleEQAService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Before
    public void initData() throws Exception {
        ensureStatusRows();
        executeDataSetWithStateManagement("testdata/eqa-order-status.xml");
    }

    /**
     * Other fixtures (e.g. testdata/analysis.xml) truncate status_of_sample and a
     * later cache refresh rebuilds StatusService's maps from those dummy rows, so
     * in a full-suite run every AnalysisStatus lookup can return "-1" by the time
     * this class runs. Restore the canonical ANALYSIS rows by the exact names
     * StatusService maps (plus sentinel id 1 for the dataset's sample_item rows),
     * then refresh the cache so the ids this class writes and compares are real
     * again — under any suite order.
     */
    private void ensureStatusRows() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " VALUES (1, 1, 'ANALYSIS', 'Fixture sentinel', 'restored by SampleEQAOrderStatusIntegrationTest')"
                + " ON CONFLICT (id) DO NOTHING");
        String[][] canonical = { { "9604", "Not Tested" }, { "9615", "Technical Acceptance" },
                { "9616", "Technical Rejected" }, { "9606", "Finalized" }, { "9614", "Test Canceled" } };
        for (String[] row : canonical) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                    + " SELECT ?::numeric, 1, 'ANALYSIS', ?, 'restored by SampleEQAOrderStatusIntegrationTest'"
                    + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                    + "   WHERE name = ? AND status_type = 'ANALYSIS')", row[0], row[1], row[1]);
        }
        statusService.refreshCache();
    }

    private SampleEQA eqaOrder(Long sampleId, Timestamp deadline) {
        SampleEQA eqa = new SampleEQA();
        eqa.setSampleId(sampleId);
        eqa.setIsEqaSample(true);
        eqa.setEqaDeadline(deadline);
        eqa.setSysUserId(TEST_SYS_USER_ID);
        sampleEQAService.insert(eqa);
        return eqa;
    }

    /** Sets the sample's analyses (ordered by id) to the given real statuses. */
    private void setAnalysisStatuses(Long sampleId, AnalysisStatus... statuses) {
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(String.valueOf(sampleId));
        analyses.sort(Comparator.comparing(a -> Long.valueOf(a.getId())));
        assertEquals("fixture analysis count for sample " + sampleId, statuses.length, analyses.size());
        for (int i = 0; i < statuses.length; i++) {
            Analysis analysis = analyses.get(i);
            analysis.setStatusId(statusService.getStatusID(statuses[i]));
            analysis.setSysUserId(TEST_SYS_USER_ID);
            analysisService.update(analysis);
        }
    }

    private static Timestamp inDays(int days) {
        return Timestamp.valueOf(LocalDate.now().plusDays(days).atStartOfDay());
    }

    @Test
    public void deriveOrderStatus_allAnalysesNotStarted_isPending() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.NotStarted, AnalysisStatus.NotStarted);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(7));
        assertEquals("PENDING", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_anyAnalysisEntered_isInProgress() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.TechnicalAcceptance, AnalysisStatus.NotStarted);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(7));
        assertEquals("IN_PROGRESS", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_allAnalysesFinalized_isCompleted() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.Finalized, AnalysisStatus.Finalized);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(7));
        assertEquals("COMPLETED", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_partiallyFinalized_isInProgressNotCompleted() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.Finalized, AnalysisStatus.NotStarted);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(7));
        assertEquals("IN_PROGRESS", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_pastDeadlineNotCompleted_isOverdue() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.TechnicalAcceptance, AnalysisStatus.NotStarted);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(-3));
        assertEquals("OVERDUE", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_pastDeadlineAllFinalized_staysCompleted() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.Finalized, AnalysisStatus.Finalized);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(-3));
        assertEquals("COMPLETED", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_cancelledAnalysisExcludedFromCompletion() {
        setAnalysisStatuses(SAMPLE_WITH_TWO_ANALYSES, AnalysisStatus.Finalized, AnalysisStatus.Canceled);
        SampleEQA order = eqaOrder(SAMPLE_WITH_TWO_ANALYSES, inDays(7));
        assertEquals("COMPLETED", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_onlyCancelledAnalyses_isPending() {
        setAnalysisStatuses(SAMPLE_WITH_ONE_ANALYSIS, AnalysisStatus.Canceled);
        SampleEQA order = eqaOrder(SAMPLE_WITH_ONE_ANALYSIS, inDays(7));
        assertEquals("PENDING", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_rejectedResult_countsAsInProgress() {
        setAnalysisStatuses(SAMPLE_WITH_ONE_ANALYSIS, AnalysisStatus.TechnicalRejected);
        SampleEQA order = eqaOrder(SAMPLE_WITH_ONE_ANALYSIS, inDays(7));
        assertEquals("IN_PROGRESS", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_noAnalyses_isPending() {
        SampleEQA order = eqaOrder(SAMPLE_WITHOUT_ANALYSES, inDays(7));
        assertEquals("PENDING", sampleEQAService.deriveOrderStatus(order));
    }

    @Test
    public void deriveOrderStatus_noAnalysesPastDeadline_isOverdue() {
        SampleEQA order = eqaOrder(SAMPLE_WITHOUT_ANALYSES, inDays(-3));
        assertEquals("OVERDUE", sampleEQAService.deriveOrderStatus(order));
    }
}
