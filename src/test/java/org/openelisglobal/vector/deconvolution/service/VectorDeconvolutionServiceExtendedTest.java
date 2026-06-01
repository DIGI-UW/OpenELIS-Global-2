package org.openelisglobal.vector.deconvolution.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionOutcome;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionPreview;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;

public class VectorDeconvolutionServiceExtendedTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE_ID = 700L;
    private static final Integer INTAKE_POOL_ID = 700;
    private static final String SYS_USER_ID = "1";
    private static final Integer POOL_ANALYSIS_ID = 800;

    @Autowired
    private VectorDeconvolutionService deconvolutionService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/vector-decon-initiate.xml");
        Timestamp now = Timestamp.valueOf("2026-05-18 00:00:00");
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis" + " (id, sampitem_id, vector_pool_id, test_id, test_sect_id, revision,"
                        + " status_id, status, started_date, entry_date, analysis_type, reflex_trigger,"
                        + " referred_out, corrected, result_calculated, type_of_sample_name, fhir_uuid,"
                        + " lastupdated)" + " VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                POOL_ANALYSIS_ID, INTAKE_POOL_ID, 700, 700, 0, 701, "1", now, now, "MANUAL", false, false, false, false,
                "Mosquito", UUID.fromString("00000000-0000-4000-8000-000000000800"), now);
    }

    @Test
    public void previewReflexes_nullPoolId_returnsEmptyPreview() {
        DeconvolutionPreview preview = deconvolutionService.previewReflexes(null);

        assertNotNull(preview);
        assertTrue(preview.getCopiedTests().isEmpty());
        assertTrue(preview.getReflexTests().isEmpty());
    }

    @Test
    public void previewReflexes_unknownPoolId_returnsEmptyPreview() {
        DeconvolutionPreview preview = deconvolutionService.previewReflexes(99999L);

        assertNotNull(preview);
        assertTrue(preview.getCopiedTests().isEmpty());
        assertTrue(preview.getReflexTests().isEmpty());
    }

    @Test
    public void previewReflexes_poolWithSeededAnalysis_includesTestNameInCopiedList() {
        DeconvolutionPreview preview = deconvolutionService.previewReflexes(INTAKE_POOL_ID.longValue());

        assertNotNull(preview);
        assertEquals("seeded analysis test name must appear in copied list", 1, preview.getCopiedTests().size());
        assertTrue("test name must appear in copied entry",
                preview.getCopiedTests().stream().anyMatch(e -> "Mosquito".equals(e.getTestName())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void forceComplete_nullSampleId_throwsIllegalArgument() {
        deconvolutionService.forceComplete(null, SYS_USER_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forceComplete_missingSampleId_throwsIllegalArgument() {
        deconvolutionService.forceComplete(999999L, SYS_USER_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forceComplete_notApplicableStatus_throwsIllegalArgument() {
        // Fixture leaves deconvolution_status as NULL → gating rejects it.
        deconvolutionService.forceComplete(SAMPLE_ID, SYS_USER_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forceComplete_nonVectorDomainSample_throwsIllegalArgument() {
        jdbcTemplate.update("UPDATE clinlims.sample SET domain = 'H' WHERE id = ?", SAMPLE_ID);
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'PENDING' WHERE id = ?",
                INTAKE_POOL_ID);
        deconvolutionService.forceComplete(SAMPLE_ID, SYS_USER_ID);
    }

    @Test
    public void forceComplete_sampleWithPendingStatus_advancesToComplete() {
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'PENDING' WHERE id = ?",
                INTAKE_POOL_ID);

        deconvolutionService.forceComplete(INTAKE_POOL_ID.longValue(), SYS_USER_ID);

        VectorPool pool = vectorPoolService.get(INTAKE_POOL_ID);
        assertEquals(VectorDeconvolutionServiceImpl.STATUS_COMPLETE, pool.getDeconvolutionStatus());
    }

    @Test
    public void forceComplete_sampleWithInProgressStatus_advancesToComplete() {
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'IN_PROGRESS' WHERE id = ?",
                INTAKE_POOL_ID);

        deconvolutionService.forceComplete(INTAKE_POOL_ID.longValue(), SYS_USER_ID);

        VectorPool pool = vectorPoolService.get(INTAKE_POOL_ID);
        assertEquals(VectorDeconvolutionServiceImpl.STATUS_COMPLETE, pool.getDeconvolutionStatus());
    }

    @Test
    public void evaluateChildResultsForCompletion_nullPoolId_returnsNull() {
        DeconvolutionOutcome outcome = deconvolutionService.evaluateChildResultsForCompletion(null, SYS_USER_ID);
        assertNull(outcome);
    }

    @Test
    public void evaluateChildResultsForCompletion_unknownPoolId_returnsNull() {
        DeconvolutionOutcome outcome = deconvolutionService.evaluateChildResultsForCompletion(99999L, SYS_USER_ID);
        assertNull(outcome);
    }

    @Test
    public void evaluateChildResultsForCompletion_intakeNotInProgress_returnsNull() {
        // Fixture leaves sample.deconvolution_status as NOT_APPLICABLE → gate returns
        // null.
        DeconvolutionOutcome outcome = deconvolutionService
                .evaluateChildResultsForCompletion(INTAKE_POOL_ID.longValue(), SYS_USER_ID);
        assertNull(outcome);
    }

    @Test
    public void evaluateChildResultsForCompletion_unfinalizedLeafAnalyses_returnsNull() {
        // After initiate, each sub-pool gets a copied analysis at status=Not Tested.
        DeconvolutionResult initiated = initiateTwoSubPools();

        // Sub-pool analyses are at Not Tested (status_id=701) — not all finalized.
        DeconvolutionOutcome outcome = deconvolutionService
                .evaluateChildResultsForCompletion(initiated.getChildPoolIds().get(0), SYS_USER_ID);

        assertNull("non-finalized leaves must block completion", outcome);
    }

    @Test
    public void evaluateChildResultsForCompletion_allFinalizedLeaves_completesAndComputesOutcomePct() {
        DeconvolutionResult initiated = initiateTwoSubPools();
        List<Long> subPoolIds = initiated.getChildPoolIds();

        // Tech confirms both sub-pools — sets deconvolution_status = COMPLETE.
        for (Long subPoolId : subPoolIds) {
            jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = ? WHERE id = ?",
                    VectorDeconvolutionServiceImpl.STATUS_COMPLETE, subPoolId);
        }

        DeconvolutionOutcome outcome = deconvolutionService.evaluateChildResultsForCompletion(subPoolIds.get(0),
                SYS_USER_ID);

        assertNotNull("all-confirmed leaves must trigger intake pool completion", outcome);
        assertEquals(2, outcome.getTotalChildCount());
        assertEquals(2, outcome.getConfirmedCount());
        assertEquals(100.0, outcome.getOutcomePct(), 0.001);

        VectorPool intakePool = vectorPoolService.get(INTAKE_POOL_ID);
        assertEquals(VectorDeconvolutionServiceImpl.STATUS_COMPLETE, intakePool.getDeconvolutionStatus());
    }

    private DeconvolutionResult initiateTwoSubPools() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID.longValue());
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        return deconvolutionService.initiate(request, SYS_USER_ID);
    }
}
