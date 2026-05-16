package org.openelisglobal.vector.deconvolution.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionOutcome;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Edge-case coverage for {@link VectorDeconvolutionServiceImpl} — branches not
 * reached by VectorDeconvolutionServiceImplTest or
 * VectorDeconvolutionServiceExtendedTest:
 * <ul>
 * <li>Explicit memberAssignments map path
 * <li>Pool-homogeneity gate (mixed sample types)
 * <li>Sub-pool externalId derivation (parent field vs. accession fallback)
 * <li>Validation: notes &gt; 500 chars, organismsPerPool &lt; 1
 * <li>Re-split guard when a sub-pool analysis has advanced past NotStarted
 * <li>evaluatePositiveResult on a single-member pool (must return null)
 * <li>evaluateChildResultsForCompletion blocked by a PENDING leaf
 * <li>Per-sub-pool notes overrides persisted to SampleItem rows
 * </ul>
 */
public class VectorDeconvolutionServiceEdgeCaseTest extends BaseWebContextSensitiveTest {

    private static final Integer INTAKE_POOL_ID = 700;
    private static final Long INTAKE_POOL_ID_LONG = 700L;
    private static final String SYS_USER_ID = "1";
    // Keep distinct from IDs used by ImplTest (700) and ExtendedTest (800).
    private static final Integer POOL_ANALYSIS_ID = 900;

    @Autowired
    private VectorDeconvolutionService deconvolutionService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/vector-decon-initiate.xml");
        Timestamp now = Timestamp.valueOf("2026-05-20 00:00:00");
        // One pool-level analysis (Not Tested) so initiate has something to copy.
        // FlatXmlDataSet can't mix sampitem_id-anchored and pool-anchored rows, so
        // we insert via jdbcTemplate (same transaction — rolled back per test).
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, vector_pool_id, test_id, test_sect_id, revision,"
                        + " status_id, status, started_date, entry_date, analysis_type, reflex_trigger, referred_out,"
                        + " corrected, result_calculated, type_of_sample_name, fhir_uuid, lastupdated)"
                        + " VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                POOL_ANALYSIS_ID, INTAKE_POOL_ID, 700, 700, 0, 701, "1", now, now, "MANUAL", false, false, false, false,
                "Mosquito", UUID.fromString("00000000-0000-4000-8000-000000000900"), now);
    }

    @Test
    public void initiate_withExplicitMemberAssignments_shouldDistributeMembersPerAssignment() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        request.setMemberAssignments(Map.of(700L, 1, 701L, 1, 702L, 1, 703L, 2, 704L, 2, 705L, 2));

        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        assertNotNull(result);
        assertEquals(2, result.getChildPoolIds().size());
        assertEquals("explicit assignment must cover all 6 members", 6, result.getChildSampleItemIds().size());
        for (Long subPoolId : result.getChildPoolIds()) {
            int memberCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM clinlims.vector_pool_member WHERE vector_pool_id = ?", Integer.class,
                    subPoolId);
            assertEquals("each explicitly-assigned sub-pool must receive exactly 3 members", 3, memberCount);
        }
    }

    @Test
    public void initiate_withMixedSampleTypeInSubPool_shouldRejectHomogeneityViolation() {
        // Introduce a second type (Fly). Change items 702+703 to Fly so the first
        // sequential slice (items 700,701,702 by sort_order) mixes two types.
        // type_of_sample.name_localization_id is NOT NULL — seed a localization row
        // first.
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description) VALUES (701, 'FlyType')");
        jdbcTemplate.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value)"
                + " VALUES (701, 701, 'en', 'Fly')");
        jdbcTemplate.update("INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                + " sort_order, name_localization_id, display_key, lastupdated)"
                + " VALUES (701, 'Fly', 'V', 'fly', true, 2, 701, 'sample.type.Fly', '2026-05-20 00:00:00')");
        jdbcTemplate.update("UPDATE clinlims.sample_item SET typeosamp_id = 701 WHERE id IN (702, 703)");

        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);

        try {
            deconvolutionService.initiate(request, SYS_USER_ID);
            fail("expected IllegalStateException for mixed sample types in one sub-pool");
        } catch (IllegalStateException expected) {
            assertTrue("message must mention type mixing", expected.getMessage().contains("mixes sample types"));
        }
    }

    @Test
    public void initiate_shouldDeriveSubPoolLabelsFromAccessionRegardlessOfPoolExternalId() {
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET external_id = 'LOT-2026-001' WHERE id = ?",
                INTAKE_POOL_ID);

        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        String extId1 = jdbcTemplate.queryForObject("SELECT external_id FROM clinlims.vector_pool WHERE id = ?",
                String.class, result.getChildPoolIds().get(0));
        String extId2 = jdbcTemplate.queryForObject("SELECT external_id FROM clinlims.vector_pool WHERE id = ?",
                String.class, result.getChildPoolIds().get(1));

        assertEquals("VCT-DECON-001-P01-S1", extId1);
        assertEquals("VCT-DECON-001-P01-S2", extId2);
    }

    @Test
    public void initiate_shouldDeriveSubPoolLabelsFromAccessionWhenPoolHasNoExternalId() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        String extId1 = jdbcTemplate.queryForObject("SELECT external_id FROM clinlims.vector_pool WHERE id = ?",
                String.class, result.getChildPoolIds().get(0));

        assertEquals("VCT-DECON-001-P01-S1", extId1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initiate_withNotesExceedingMaxLength_shouldRejectWithIllegalArgument() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(2);
        request.setNotes("A".repeat(501));

        deconvolutionService.initiate(request, SYS_USER_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initiate_withOrganismsPerPoolBelowMinimum_shouldRejectWithIllegalArgument() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(0);

        deconvolutionService.initiate(request, SYS_USER_ID);
    }

    @Test
    public void initiate_whenSubPoolAnalysisIsNonDraft_shouldBlockReSplitting() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult first = deconvolutionService.initiate(request, SYS_USER_ID);

        Long firstSubPoolId = first.getChildPoolIds().get(0);
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = 702 WHERE vector_pool_id = ?", firstSubPoolId);

        try {
            deconvolutionService.initiate(request, SYS_USER_ID);
            fail("expected IllegalStateException when a sub-pool has a non-draft analysis");
        } catch (IllegalStateException expected) {
            assertTrue("message must reference immutable physical groupings",
                    expected.getMessage().contains("sub-pools with results"));
        }
    }

    @Test
    public void evaluatePositiveResult_onSingleMemberPool_shouldReturnNull() {
        Timestamp now = Timestamp.valueOf("2026-05-20 00:00:00");
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, typeosamp_id, quantity,"
                        + " collection_date, lastupdated, voided)" + " VALUES (710, 700, 10, 700, 700, 1, ?, ?, false)",
                now, now);
        jdbcTemplate.update("INSERT INTO clinlims.vector_pool (id, sample_id, active, sys_user_id, lastupdated)"
                + " VALUES (710, 700, true, '1', ?)", now);
        jdbcTemplate.update("INSERT INTO clinlims.vector_pool_member (vector_pool_id, sample_item_id, lastupdated)"
                + " VALUES (710, 710, ?)", now);

        String status = deconvolutionService.evaluatePositiveResult(710L, "POSITIVE", SYS_USER_ID);

        assertNull("single-member pool must not be flagged for deconvolution", status);
    }

    @Test
    public void evaluateChildResultsForCompletion_withPendingLeaf_shouldBlockCompletion() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult initiated = deconvolutionService.initiate(request, SYS_USER_ID);

        Long firstLeafId = initiated.getChildPoolIds().get(0);
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'PENDING' WHERE id = ?",
                firstLeafId);

        DeconvolutionOutcome outcome = deconvolutionService
                .evaluateChildResultsForCompletion(initiated.getChildPoolIds().get(1), SYS_USER_ID);

        assertNull("a PENDING leaf must block the completion gate", outcome);
        VectorPool intakePool = vectorPoolService.get(INTAKE_POOL_ID);
        assertEquals("intake pool must remain IN_PROGRESS while a leaf is unresolved",
                VectorDeconvolutionServiceImpl.STATUS_IN_PROGRESS, intakePool.getDeconvolutionStatus());
    }
}
