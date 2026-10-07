package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroCaseIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseStateService stateService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private org.openelisglobal.microbiology.service.MicroOrderRoutingService routingService;
    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItemService;
    @Autowired
    private org.openelisglobal.test.service.TestService testService;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO orderDetailDAO;

    private String sampleItemId;
    private String methodId;
    private ReferenceData referenceData;
    private org.openelisglobal.test.valueholder.Test cultureTest;
    private org.openelisglobal.test.valueholder.Test otherUnitTest;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("OGC782M2").getId();
        referenceData = fixtures.createReferenceData(methodId);
        cultureTest = fixtures.createCatalogCultureTest(methodId, fixtures.createLabUnit());
        otherUnitTest = fixtures.createCatalogCultureTest(methodId, fixtures.createLabUnit());
    }

    @Test
    public void caseIdentityUsesOrderSampleTypeAndLabUnitWithSeparateRelatedCases() {
        MicroCase first = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), cultureTest,
                fixtures.defaultUserId());
        MicroCase duplicate = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), cultureTest,
                fixtures.defaultUserId());
        MicroCase sibling = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), otherUnitTest,
                fixtures.defaultUserId());

        assertEquals(first.getId(), duplicate.getId());
        assertEquals(java.util.List.of(sampleItemId), caseService.getSpecimenIds(sibling.getId()));
        assertEquals(2, caseService.getSiblingCases(sampleItemId).size());
    }

    @Test
    public void compiledCaseDetailIncludesTimelineAndIsolatesWithoutControllerTraversal() {
        MicroCase microCase = caseService.createOrGetCase(fixtures.getSampleItem(sampleItemId), cultureTest,
                fixtures.defaultUserId());
        stateService.advanceStage(microCase.getId(), MicroCaseStage.SETUP_RECORDED, fixtures.defaultUserId(),
                "setup complete");
        var isolate = isolateService.createIsolate(microCase.getId(), sampleItemId, "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                fixtures.defaultUserId());
        isolateService.updateIdentification(isolate.getId(), referenceData.organism().getId(),
                referenceData.organism().getDisplayName(), MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                MicroIsolateIdentificationStatus.CONFIRMED, "MALDI_TOF", new java.math.BigDecimal("99.5"),
                fixtures.defaultUserId());

        MicroCaseDetailForm detail = caseService.getCaseDetail(microCase.getId());

        assertEquals(microCase.getId(), detail.id);
        assertEquals(1, detail.isolates.size());
        assertEquals("ISO-1", detail.isolates.get(0).isolateLabel);
        assertTrue(detail.activities.size() >= 3);
    }

    @Test
    public void setCountComesFromDistinctBottleAssignmentsNotCapturedOrderCount() {
        cultureTest.setCollectedInSets(true);
        testService.update(cultureTest);
        var first = fixtures.getSampleItem(sampleItemId);
        first.setCultureSetNumber(1);
        first.setContainer("Aerobic");
        sampleItemService.update(first);
        var firstAnalysis = fixtures.createAnalysis(first, cultureTest);
        var owner = routingService
                .routeAnalysesForSampleItem(first, java.util.List.of(firstAnalysis), fixtures.defaultUserId()).get(0);
        var second = fixtures.createSampleWithSampleItem("SETSECOND");
        second.setSample(first.getSample());
        second.setTypeOfSample(first.getTypeOfSample());
        second.setCultureSetNumber(3);
        second.setSysUserId(fixtures.defaultUserId());
        sampleItemService.update(second);
        var secondAnalysis = fixtures.createAnalysis(second, cultureTest);
        assertEquals(owner.getId(),
                routingService
                        .routeAnalysesForSampleItem(second, java.util.List.of(secondAnalysis), fixtures.defaultUserId())
                        .get(0).getId());
        var historical = new org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail();
        historical.setCaseId(owner.getId());
        historical.setNumberOfSets(99);
        historical.setSysUserId(fixtures.defaultUserId());
        orderDetailDAO.insert(historical);
        var detail = caseService.getCaseDetail(owner.getId());
        assertEquals(Integer.valueOf(2), detail.orderDetail.numberOfSets);
        assertEquals(2, detail.specimens.size());
        assertTrue(detail.specimens.stream().allMatch(specimen -> specimen.collectedInSets));
        assertTrue(detail.specimens.stream().anyMatch(specimen -> "Aerobic".equals(specimen.containerType)));
        second = sampleItemService.get(second.getId());
        second.setCultureSetNumber(1);
        sampleItemService.update(second);
        assertEquals(Integer.valueOf(1), caseService.getCaseDetail(owner.getId()).orderDetail.numberOfSets);
        assertEquals(Integer.valueOf(99), orderDetailDAO.getByCaseId(owner.getId()).getNumberOfSets());
        var otherAnalysis = fixtures.createAnalysis(first, otherUnitTest);
        var otherCase = routingService
                .routeAnalysesForSampleItem(first, java.util.List.of(otherAnalysis), fixtures.defaultUserId()).get(0);
        assertTrue(caseService.getCaseDetail(otherCase.getId()).specimens.stream()
                .noneMatch(specimen -> specimen.collectedInSets));
    }

}
