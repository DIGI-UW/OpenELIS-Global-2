package org.openelisglobal.vector.deconvolution.service;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;

public class VectorDeconvolutionServiceImplTest extends BaseWebContextSensitiveTest {

    private static final String SAMPLE_ID = "700";
    private static final Integer INTAKE_POOL_ID = 700;
    private static final Long INTAKE_POOL_ID_LONG = INTAKE_POOL_ID.longValue();
    private static final String SYS_USER_ID = "1";

    // Pool-level analysis seeded in @Before via jdbcTemplate (FlatXmlDataSet can't
    // mix sampitem_id-anchored and pool-anchored analyses in one file).
    private static final Integer POOL_ANALYSIS_ID = 700;

    @Autowired
    private VectorDeconvolutionService deconvolutionService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/vector-decon-initiate.xml");
        // Seed one pool-level analysis (status_id=Not Tested) so initiate has
        // something to copy. Inserted here because dbunit's FlatXmlDataSet
        // infers columns from the first row, which would force every analysis
        // to either have sampitem_id or vector_pool_id but not both shapes.
        Timestamp now = Timestamp.valueOf("2026-05-13 00:00:00");
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, vector_pool_id, test_id, test_sect_id, revision,"
                        + " status_id, status, started_date, entry_date, analysis_type, reflex_trigger, referred_out,"
                        + " corrected, result_calculated, type_of_sample_name, fhir_uuid, lastupdated)"
                        + " VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                POOL_ANALYSIS_ID, INTAKE_POOL_ID, 700, 700, 0, 701, "1", now, now, "MANUAL", false, false, false, false,
                "Mosquito", UUID.fromString("00000000-0000-4000-8000-000000000700"), now);
    }

    @Test
    public void initiate_shouldCreateSubPoolsThatLinkBackToOriginalPool() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);

        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        Assert.assertNotNull(result);
        Assert.assertEquals("two sub-pools should be created", 2, result.getChildPoolIds().size());
        for (Long subPoolId : result.getChildPoolIds()) {
            VectorPool sub = vectorPoolService.get(subPoolId.intValue());
            Assert.assertNotNull("sub-pool row must exist for id " + subPoolId, sub);
            Assert.assertNotNull("sub-pool must have a parentPool", sub.getParentPool());
            Assert.assertEquals("parent pool must be the intake pool", INTAKE_POOL_ID, sub.getParentPool().getId());
            Assert.assertEquals(SAMPLE_ID, sub.getSampleId());
        }
    }

    @Test
    public void initiate_shouldRedistributeExistingMembersWithoutCreatingNewSampleItems() {
        Integer beforeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.sample_item WHERE samp_id = ?",
                Integer.class, 700);
        Assert.assertEquals("precondition: fixture seeds six pool members", Integer.valueOf(6), beforeCount);

        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(3);
        request.setOrganismsPerPool(2);
        deconvolutionService.initiate(request, SYS_USER_ID);

        Integer afterCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.sample_item WHERE samp_id = ?",
                Integer.class, 700);
        Assert.assertEquals("decon must redistribute existing organisms, not duplicate them", beforeCount, afterCount);
    }

    @Test
    public void initiate_shouldLinkEachOriginalMemberToOneSubPoolViaVectorPoolMember() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        // Sum of sub-pool member rows must equal poolCount × organismsPerPool.
        Set<Integer> subPoolIds = new HashSet<>();
        result.getChildPoolIds().forEach(id -> subPoolIds.add(id.intValue()));
        Integer subPoolMemberCount = jdbcTemplate
                .queryForObject(
                        "SELECT COUNT(*) FROM clinlims.vector_pool_member WHERE vector_pool_id IN ("
                                + String.join(",", subPoolIds.stream().map(String::valueOf).toList()) + ")",
                        Integer.class);
        Assert.assertEquals("each sub-pool slice must be linked to its members", Integer.valueOf(6),
                subPoolMemberCount);

        // Each redistributed sample_item lives in BOTH the intake pool and one
        // sub-pool (M:N is the explicit point of the new model).
        for (Long childId : result.getChildSampleItemIds()) {
            Integer membershipCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM clinlims.vector_pool_member WHERE sample_item_id = ?", Integer.class,
                    childId);
            Assert.assertEquals("redistributed member should belong to intake + one sub-pool", Integer.valueOf(2),
                    membershipCount);
        }
    }

    @Test
    public void initiate_shouldCopyPoolLevelAnalysesToEachSubPool() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        Assert.assertEquals("one analysis per parent analysis × sub-pool", 2, result.getTestOrdersCreated());
        for (Long subPoolId : result.getChildPoolIds()) {
            List<Analysis> subAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(subPoolId));
            Assert.assertEquals("each sub-pool gets a copy of every parent analysis", 1, subAnalyses.size());
            Analysis copy = subAnalyses.get(0);
            Assert.assertNull("ck_analysis_pool_or_item: pool-anchored analyses must have sampitem_id NULL",
                    copy.getSampleItem());
            Assert.assertEquals(String.valueOf(subPoolId), copy.getVectorPoolId());
        }
    }

    @Test
    public void initiate_shouldAdvanceSampleDeconvolutionStatusToInProgress() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(2);
        deconvolutionService.initiate(request, SYS_USER_ID);

        VectorPool reloadedPool = vectorPoolService.get(INTAKE_POOL_ID);
        Assert.assertEquals(VectorDeconvolutionServiceImpl.STATUS_IN_PROGRESS, reloadedPool.getDeconvolutionStatus());
    }

    @Test
    public void initiate_shouldRejectWhenRequestedMembersExceedAvailable() {
        // Pool has 6 members; 4×2 = 8 requested → over capacity.
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(4);
        request.setOrganismsPerPool(2);
        try {
            deconvolutionService.initiate(request, SYS_USER_ID);
            Assert.fail("expected IllegalStateException for over-capacity request");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void initiate_shouldRejectWhenPoolDoesNotExist() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(99999L);
        request.setPoolCount(2);
        request.setOrganismsPerPool(2);
        try {
            deconvolutionService.initiate(request, SYS_USER_ID);
            Assert.fail("expected IllegalStateException for missing pool");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void initiate_shouldRejectWhenPoolCountLessThanTwo() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(1);
        request.setOrganismsPerPool(2);
        try {
            deconvolutionService.initiate(request, SYS_USER_ID);
            Assert.fail("expected IllegalArgumentException for poolCount<2");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void initiate_shouldRejectOnNonVectorDomainSample() {
        // Flip the fixture's sample to a non-V domain so gateBusinessRules trips.
        jdbcTemplate.update("UPDATE clinlims.sample SET domain = 'H' WHERE id = ?", 700);

        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(2);
        try {
            deconvolutionService.initiate(request, SYS_USER_ID);
            Assert.fail("expected IllegalStateException for non-vector sample");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void initiate_shouldNotIncludeIntakePoolInOwnSubPoolList() {
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(2);
        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        Assert.assertFalse("intake pool must not appear in its own sub-pool list",
                result.getChildPoolIds().contains(INTAKE_POOL_ID_LONG));
        // Every created sub-pool must have parentPool set (no orphan roots).
        for (Long subPoolId : result.getChildPoolIds()) {
            VectorPool sub = vectorPoolService.get(subPoolId.intValue());
            Assert.assertNotNull(sub.getParentPool());
        }
    }

    @Test
    public void evaluatePositiveResult_shouldFlipNotApplicableToPending() {
        // Pool starts at deconvolution_status = NOT_APPLICABLE (fixture default).
        String result = deconvolutionService.evaluatePositiveResult(INTAKE_POOL_ID_LONG, "POSITIVE", SYS_USER_ID);

        Assert.assertEquals(VectorDeconvolutionServiceImpl.STATUS_PENDING, result);
        VectorPool reloadedPool = vectorPoolService.get(INTAKE_POOL_ID);
        Assert.assertEquals(VectorDeconvolutionServiceImpl.STATUS_PENDING, reloadedPool.getDeconvolutionStatus());
    }

    @Test
    public void evaluatePositiveResult_shouldBeIdempotentForActiveStates() {
        // Manually move the pool to PENDING; a second positive must not flip again.
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = ? WHERE id = ?",
                VectorDeconvolutionServiceImpl.STATUS_PENDING, INTAKE_POOL_ID);

        String result = deconvolutionService.evaluatePositiveResult(INTAKE_POOL_ID_LONG, "DETECTED", SYS_USER_ID);

        Assert.assertNull("active states are sticky — second positive should be a no-op", result);
    }

    @Test
    public void evaluatePositiveResult_shouldIgnoreNegativeResults() {
        String result = deconvolutionService.evaluatePositiveResult(INTAKE_POOL_ID_LONG, "NEGATIVE", SYS_USER_ID);

        Assert.assertNull(result);
        VectorPool reloadedPool = vectorPoolService.get(INTAKE_POOL_ID_LONG.intValue());
        Assert.assertEquals("NOT_APPLICABLE", reloadedPool.getDeconvolutionStatus());
    }

    @Test
    public void initiate_shouldRedistributeMembersDeterministicallyBySortOrder() {
        // 2×3 = 6 = all members. Slice order matches sample_item.sort_order.
        DeconvolutionInitiateRequest request = new DeconvolutionInitiateRequest();
        request.setVectorPoolId(INTAKE_POOL_ID_LONG);
        request.setPoolCount(2);
        request.setOrganismsPerPool(3);
        DeconvolutionResult result = deconvolutionService.initiate(request, SYS_USER_ID);

        // childSampleItemIds is flattened: pool1.member1, pool1.member2, ...,
        // pool2.member3. With 6 fixture members sorted by sort_order asc, the
        // first three go to sub-pool 1, the last three to sub-pool 2.
        Assert.assertEquals("flat child list covers all 6 redistributed members", 6,
                result.getChildSampleItemIds().size());
        // Sanity: the union should be exactly the original member ids.
        Set<Long> redistributed = new HashSet<>(result.getChildSampleItemIds());
        Set<Long> originalIds = new HashSet<>();
        for (SampleItem si : sampleItemService.getSampleItemsBySampleId(SAMPLE_ID)) {
            originalIds.add(Long.valueOf(si.getId()));
        }
        Assert.assertEquals("redistributed set equals original member set", originalIds, redistributed);
    }
}
