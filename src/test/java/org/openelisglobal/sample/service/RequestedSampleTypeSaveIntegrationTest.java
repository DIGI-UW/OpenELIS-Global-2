package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

/**
 * The requested stage saves the order and the specimens it requests together,
 * so the two can never disagree and repeating the save cannot duplicate them.
 */
@Transactional
public class RequestedSampleTypeSaveIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItemService;

    @Autowired
    private org.openelisglobal.test.service.TestService testService;

    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseDAO cases;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO ownership;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO analysisLinks;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO specimenLinks;

    @Autowired
    private org.openelisglobal.microbiology.service.MicroCaseService caseDetails;

    @Autowired
    private org.openelisglobal.microbiology.dao.MicroWorklistContextDAO worklistContext;

    @Autowired
    private org.openelisglobal.result.service.ResultService results;
    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analyses;
    @Autowired
    private org.openelisglobal.microbiology.service.MicroCaseAnalysisService caseAnalyses;
    @Autowired
    private org.openelisglobal.common.services.IStatusService statuses;

    private String userId;
    private Patient patient;
    private TypeOfSample sampleType;
    private TypeOfSample secondSampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("REQSPEC");
        sampleType = fixtures.getOrCreateActiveSampleType();
        secondSampleType = fixtures.createTypeOfSample();
    }

    @Test
    public void lastRequestedMicroTestRemovalRequiresConfirmationAndRollsBack() {
        var test = bottleTest();
        var request = requested("1");
        request.setRequestedTests(test.getId());
        request.setCultureSetNumber(1);
        Sample order = newSample();
        persist(order, List.of(request));
        var owner = cases.getByOrder(order.getId()).get(0);
        var failure = assertThrows(org.openelisglobal.microbiology.service.MicroCaseCancellationRequiredException.class,
                () -> persist(order, List.of()));
        assertEquals(owner.getId(), failure.getCases().get(0).caseId());
        assertTrue(!failure.getCases().get(0).hasResults());
        TestTransaction.flagForCommit();
        assertThrows(UnexpectedRollbackException.class, TestTransaction::end);
        TestTransaction.start();
        assertTrue(cases.get(owner.getId()).isEmpty());
    }

    @Test
    public void confirmedLastTestRemovalClosesCaseAndRetainsOwnership() {
        var test = bottleTest();
        var request = requested("1");
        request.setRequestedTests(test.getId());
        request.setCultureSetNumber(1);
        Sample order = newSample();
        persist(order, List.of(request));
        var owner = cases.getByOrder(order.getId()).get(0);
        var saved = sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0);
        var form = new SamplePatientEntryForm();
        form.setRequestedSampleTypes(List.of());
        form.setMicroCaseCancellationIds(List.of(owner.getId()));
        persist(order, form);
        assertEquals("CANCELLED", cases.get(owner.getId()).orElseThrow().getStage());
        assertNotNull(cases.get(owner.getId()).orElseThrow().getClosedAt());
        assertEquals(SampleTypeRequest.Status.CANCELLED, sampleTypeRequestService.get(saved.getId()).getStatus());
        assertNull(ownership.getByRequestAndTest(saved.getId(), test.getId()));
        assertEquals(userId, ownership.getByCaseId(owner.getId()).get(0).getCancelledBy());
    }

    @Test
    public void confirmedCancellationRetainsAnEarlierResultAndItsLink() {
        var test = bottleTest();
        var request = requested("1");
        request.setRequestedTests(test.getId());
        request.setCultureSetNumber(1);
        Sample order = newSample();
        persist(order, List.of(request));
        var owner = cases.getByOrder(order.getId()).get(0);
        var item = new org.openelisglobal.sampleitem.valueholder.SampleItem();
        item.setSample(order);
        item.setTypeOfSample(sampleType);
        item.setSortOrder("1");
        item.setStatusId(fixtures.ensureSampleEnteredStatus());
        item.setSysUserId(userId);
        sampleItemService.insert(item);
        var analysis = fixtures.createAnalysis(item, test);
        caseAnalyses.linkAnalysis(owner, analysis, userId);
        analysis.setStatusId(
                statuses.getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Canceled));
        analysis.setSysUserId(userId);
        analyses.update(analysis);
        var result = new org.openelisglobal.result.valueholder.Result();
        result.setAnalysis(analysis);
        result.setResultType("N");
        result.setValue("4.2");
        result.setIsReportable("N");
        result.setSysUserId(userId);
        String resultId = results.insert(result);
        var saved = sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0);
        sampleTypeRequestService.cancelRequest(saved.getId(), List.of(owner.getId()), "Duplicate request corrected");
        assertEquals("CANCELLED", cases.get(owner.getId()).orElseThrow().getStage());
        assertEquals("4.2", results.get(resultId).getValue());
        assertEquals(owner.getId(), analysisLinks.getByAnalysis(analysis.getId()).getCaseId());
        assertEquals("Duplicate request corrected",
                ownership.getByCaseId(owner.getId()).get(0).getCancellationReason());
    }

    @Test
    public void initialOrderOpensOneSetCaseAndOutOfOrderCollectionRetainsOwnership() {
        var test = bottleTest();
        var first = requested("1");
        first.setRequestedTests(test.getId());
        first.setCultureSetNumber(1);
        var second = requested("1");
        second.setRequestedTests(test.getId());
        second.setCultureSetNumber(2);
        Sample order = newSample();
        persist(order, List.of(first, second));
        var requests = sampleTypeRequestService.getRequestsBySampleId(order.getId());
        assertEquals(1, cases.getByOrder(order.getId()).size());
        var owner = cases.getByOrder(order.getId()).get(0);
        assertTrue(sampleItemService.getSampleItemsBySampleId(order.getId()).isEmpty());
        var pendingDetail = caseDetails.getCaseDetail(owner.getId());
        assertEquals(order.getAccessionNumber(), pendingDetail.accessionNumber);
        assertEquals(patient.getId(), pendingDetail.patientId);
        assertEquals(2, pendingDetail.requestedSpecimens.size());
        var discovery = worklistContext.getRequestedContexts(List.of(owner.getId()));
        assertEquals(1, discovery.size());
        assertEquals(order.getAccessionNumber(), discovery.get(0).accessionNumber());
        assertTrue(worklistContext.getRequestedContexts(List.of("unrelated-case")).isEmpty());
        assertEquals(2, pendingDetail.setWarnings.stream().filter(w -> "SINGLE_BOTTLE".equals(w.code())).count());
        assertTrue(pendingDetail.specimens.isEmpty());
        assertEquals(Integer.valueOf(2), pendingDetail.orderDetail.numberOfSets);
        assertEquals(owner.getId(), ownership.getByRequestAndTest(requests.get(0).getId(), test.getId()).getCaseId());
        assertEquals(owner.getId(), ownership.getByRequestAndTest(requests.get(1).getId(), test.getId()).getCaseId());
        persist(order, List.of(new SampleTypeRequestDTO(requests.get(1)), new SampleTypeRequestDTO(requests.get(0))));
        assertEquals(1, cases.getByOrder(order.getId()).size());
        persist(order, new SamplePatientEntryForm(),
                bottleXml(test.getId(), "sampleTypeRequestId='" + requests.get(1).getId() + "'"));
        var item = sampleTypeRequestService.get(requests.get(1).getId()).getSampleItem();
        assertEquals(Integer.valueOf(2), item.getCultureSetNumber());
        assertEquals(1, cases.getByOrder(order.getId()).size());
        assertNotNull(specimenLinks.getByCaseAndSampleItem(owner.getId(), item.getId()));
        assertEquals(1, analysisLinks.getByCaseId(owner.getId()).size());
        assertTrue(analysisLinks.getByCaseId(owner.getId()).get(0).isCollectedInSets());
        persist(order, new SamplePatientEntryForm(), bottleXml(test.getId(), "sampleItemId='" + item.getId() + "'"));
        assertEquals(1, analysisLinks.getByCaseId(owner.getId()).size());
        assertEquals(1, specimenLinks.getByCaseId(owner.getId()).size());
        var partialDetail = caseDetails.getCaseDetail(owner.getId());
        assertEquals(1, partialDetail.requestedSpecimens.size());
        assertEquals(1, partialDetail.specimens.size());
        assertEquals(requests.get(0).getId(), partialDetail.requestedSpecimens.get(0).requestId);
        assertEquals(Integer.valueOf(2), partialDetail.orderDetail.numberOfSets);
        assertEquals(1, worklistContext.getRequestedContexts(List.of(owner.getId())).size());
        persist(order, new SamplePatientEntryForm(),
                bottleXml(test.getId(), "sampleTypeRequestId='" + requests.get(0).getId() + "'"));
        assertTrue(worklistContext.getRequestedContexts(List.of(owner.getId())).isEmpty());
    }

    @Test
    public void setRequestsShareCaseAcrossTypesAndCollectionKeepsOrderedRole() {
        var test = bottleTest();
        var first = requested(sampleType, "1");
        first.setRequestedTests(test.getId());
        first.setCultureSetNumber(1);
        var second = requested(secondSampleType, "1");
        second.setRequestedTests(test.getId());
        second.setCultureSetNumber(2);
        Sample order = newSample();
        persist(order, List.of(first, second));
        var requests = sampleTypeRequestService.getRequestsBySampleId(order.getId());
        var owner = ownership.getByRequestAndTest(requests.get(0).getId(), test.getId()).getCaseId();
        assertEquals(owner, ownership.getByRequestAndTest(requests.get(1).getId(), test.getId()).getCaseId());
        test.setCollectedInSets(false);
        test.setMicrobiologyCaseRole("DIRECT");
        test.setSysUserId(userId);
        testService.update(test);
        persist(order, new SamplePatientEntryForm(),
                bottleXml(test.getId(), "sampleTypeRequestId='" + requests.get(0).getId() + "'"));
        var link = analysisLinks.getByCaseId(owner).get(0);
        assertEquals("CULTURE", link.getCaseRole());
        assertTrue(link.isCollectedInSets());
        assertEquals(1, cases.getByOrder(order.getId()).size());
    }

    @Test
    public void collectionCannotAttachToFinalReleasedRequestedCase() {
        var test = bottleTest();
        var request = requested("1");
        request.setRequestedTests(test.getId());
        request.setCultureSetNumber(1);
        Sample order = newSample();
        persist(order, List.of(request));
        var owner = cases.getByOrder(order.getId()).get(0);
        owner.setFinalReleaseState("FINAL_RELEASED");
        owner.setSysUserId(userId);
        cases.update(owner);
        var saved = sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0);
        assertThrows(IllegalStateException.class, () -> persist(order, new SamplePatientEntryForm(),
                bottleXml(test.getId(), "sampleTypeRequestId='" + saved.getId() + "'")));
    }

    @Test
    public void directCollectionPreservesRequestedCaseAndIsIdempotent() {
        var test = bottleTest();
        var request = requested("1");
        request.setRequestedTests(test.getId());
        request.setCultureSetNumber(3);
        Sample order = newSample();
        persist(order, List.of(request));
        var saved = sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0);
        var owner = ownership.getByRequestAndTest(saved.getId(), test.getId()).getCaseId();
        var item = new org.openelisglobal.sampleitem.valueholder.SampleItem();
        item.setSample(order);
        item.setTypeOfSample(sampleType);
        item.setSortOrder("1");
        item.setStatusId(fixtures.ensureSampleEnteredStatus());
        item.setSysUserId(userId);
        sampleItemService.insert(item);
        var analysis = fixtures.createAnalysis(item, test);
        sampleTypeRequestService.fulfillRequest(saved.getId(), item.getId());
        sampleTypeRequestService.fulfillRequest(saved.getId(), item.getId());
        assertEquals(Integer.valueOf(3), sampleItemService.get(item.getId()).getCultureSetNumber());
        assertEquals(owner, analysisLinks.getByAnalysis(analysis.getId()).getCaseId());
        assertEquals(1, specimenLinks.getByCaseId(owner).size());
        assertEquals(1, analysisLinks.getByCaseId(owner).size());
        assertEquals(1, cases.getByOrder(order.getId()).size());
    }

    @Test
    public void directCollectionCannotUseOneBottleForTwoRequests() {
        var test = bottleTest();
        var first = requested("1");
        first.setRequestedTests(test.getId());
        first.setCultureSetNumber(1);
        var second = requested("1");
        second.setRequestedTests(test.getId());
        second.setCultureSetNumber(2);
        Sample order = newSample();
        persist(order, List.of(first, second));
        var requests = sampleTypeRequestService.getRequestsBySampleId(order.getId());
        persist(order, new SamplePatientEntryForm(),
                bottleXml(test.getId(), "sampleTypeRequestId='" + requests.get(0).getId() + "'"));
        var itemId = sampleTypeRequestService.get(requests.get(0).getId()).getSampleItem().getId();
        assertThrows(IllegalStateException.class,
                () -> sampleTypeRequestService.fulfillRequest(requests.get(1).getId(), itemId));
    }

    @Test
    public void ordinaryRequestedTestDoesNotOpenCaseAndUnitsRemainSeparate() {
        var unit = fixtures.createLabUnit();
        var other = fixtures.createLabUnit();
        var direct = fixtures
                .createCatalogMicroTest(org.openelisglobal.microbiology.valueholder.MicroCaseTestRole.DIRECT, unit);
        var another = fixtures
                .createCatalogMicroTest(org.openelisglobal.microbiology.valueholder.MicroCaseTestRole.DIRECT, other);
        var ordinary = fixtures.createCatalogTest();
        var request = requested("1");
        request.setRequestedTests(ordinary.getId());
        Sample order = newSample();
        persist(order, List.of(request));
        assertEquals(0, cases.getByOrder(order.getId()).size());
        request.setRequestedTests(direct.getId() + "," + another.getId() + "," + ordinary.getId());
        persist(order, List.of(request));
        assertEquals(2, cases.getByOrder(order.getId()).size());
        var saved = sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0);
        assertNull(ownership.getByRequestAndTest(saved.getId(), ordinary.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bottleRequestCannotBeSavedWithoutAnExplicitSet() {
        var test = fixtures.createCatalogTest();
        test.setCollectedInSets(true);
        test.setSysUserId(userId);
        testService.update(test);
        var request = requested("1");
        request.setRequestedTests(test.getId());
        persist(newSample(), List.of(request));
    }

    @Test
    public void ordinaryRequestDoesNotNeedASetAndBottleAcceptsExplicitSet() {
        var test = fixtures.createCatalogTest();
        var ordinary = requested("1");
        ordinary.setRequestedTests(test.getId());
        Sample ordinaryOrder = newSample();
        persist(ordinaryOrder, List.of(ordinary));
        assertEquals(null,
                sampleTypeRequestService.getRequestsBySampleId(ordinaryOrder.getId()).get(0).getCultureSetNumber());
        test.setCollectedInSets(true);
        test.setSysUserId(userId);
        testService.update(test);
        var bottle = requested("1");
        bottle.setRequestedTests(test.getId());
        bottle.setCultureSetNumber(2);
        Sample bottleOrder = newSample();
        persist(bottleOrder, List.of(bottle));
        assertEquals(Integer.valueOf(2),
                sampleTypeRequestService.getRequestsBySampleId(bottleOrder.getId()).get(0).getCultureSetNumber());
    }

    @Test
    public void bottleDetailsSurviveRequestReloadCollectionAndExplicitCorrection() {
        var requested = requested("1");
        requested.setCultureSetNumber(2);
        requested.setContainer("Aerobic");
        requested.setBodySite("Left arm");
        requested.setCollectionDate("2026-10-07");
        requested.setCollectionTime("07:25");
        Sample order = newSample();
        persist(order, List.of(requested));
        var request = sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0);
        var reloaded = new SampleTypeRequestDTO(request);
        assertEquals("Aerobic", reloaded.getContainer());
        assertEquals("Left arm", reloaded.getBodySite());
        assertEquals("2026-10-07", reloaded.getCollectionDate());
        assertEquals("07:25", reloaded.getCollectionTime());
        persist(order, new SamplePatientEntryForm(), bottleXml("", "sampleTypeRequestId='" + request.getId() + "'"));
        var item = sampleItemService.getSampleItemsBySampleId(order.getId()).get(0);
        assertEquals("Aerobic", item.getContainer());
        assertEquals("Left arm", item.getSourceOther());
        assertEquals(Timestamp.valueOf("2026-10-07 07:25:00"), item.getCollectionDate());
        persist(order, new SamplePatientEntryForm(),
                bottleXml("", "sampleItemId='" + item.getId() + "' bodySite='Right arm' container='Anaerobic'"));
        item = sampleItemService.get(item.getId());
        assertEquals("Right arm", item.getSourceOther());
        assertEquals("Anaerobic", item.getContainer());
        assertEquals(Timestamp.valueOf("2026-10-07 07:25:00"), item.getCollectionDate());
    }

    @Test
    public void collectionWithoutBottleSetRollsBackTheWholeOrder() {
        var test = bottleTest();
        Sample order = newSample();
        String accession = order.getAccessionNumber();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> persist(order, new SamplePatientEntryForm(), bottleXml(test.getId(), "")));
        assertEquals("A set number is required for each culture bottle", failure.getMessage());
        TestTransaction.flagForCommit();
        assertThrows(UnexpectedRollbackException.class, TestTransaction::end);
        TestTransaction.start();
        assertNull(sampleService.getSampleByAccessionNumber(accession));
    }

    @Test
    public void collectionRestoresRequestedSetBeforeValidationAndKeepsItOnEdit() {
        var test = bottleTest();
        var request = requested("1");
        request.setRequestedTests(test.getId());
        request.setCultureSetNumber(3);
        Sample order = newSample();
        persist(order, List.of(request));
        String requestId = String.valueOf(sampleTypeRequestService.getRequestsBySampleId(order.getId()).get(0).getId());
        persist(order, new SamplePatientEntryForm(),
                bottleXml(test.getId(), "sampleTypeRequestId='" + requestId + "'"));
        var item = sampleItemService.getSampleItemsBySampleId(order.getId()).get(0);
        assertEquals(Integer.valueOf(3), item.getCultureSetNumber());
        persist(order, new SamplePatientEntryForm(), bottleXml("", "sampleItemId='" + item.getId() + "'"));
        assertEquals(Integer.valueOf(3), sampleItemService.get(item.getId()).getCultureSetNumber());
        assertEquals(1, sampleItemService.getSampleItemsBySampleId(order.getId()).size());
    }

    @Test
    public void collectedBottleWithNoSetCannotBypassValidationByOmittingTests() {
        var item = fixtures.createSampleWithSampleItem("NOSET");
        fixtures.createAnalysis(item, bottleTest());
        String xml = "<samples><sample typeId='" + item.getTypeOfSampleId() + "' sampleItemId='" + item.getId()
                + "' tests='' panels='' testSectionMap='' testSampleTypeMap='' /></samples>";
        assertThrows(IllegalArgumentException.class,
                () -> persist(item.getSample(), new SamplePatientEntryForm(), xml));
    }

    private org.openelisglobal.test.valueholder.Test bottleTest() {
        var test = fixtures.createCatalogMicroTest(
                org.openelisglobal.microbiology.valueholder.MicroCaseTestRole.CULTURE, fixtures.createLabUnit());
        test.setCollectedInSets(true);
        test.setSysUserId(userId);
        return testService.update(test);
    }

    private String bottleXml(String testId, String attributes) {
        return "<samples><sample typeId='" + sampleType.getId() + "' tests='" + testId
                + "' panels='' testSectionMap='' testSampleTypeMap='' " + attributes + "/></samples>";
    }

    @Test
    public void bottleSetsKeepIdentityWhenSameTypeRequestsAreReordered() {
        Sample order = newSample();
        var first = requested("1");
        first.setCultureSetNumber(1);
        var second = requested("1");
        second.setCultureSetNumber(2);
        persist(order, List.of(first, second));
        var original = sampleTypeRequestService.getRequestsBySampleId(order.getId());
        var firstReloaded = new SampleTypeRequestDTO(original.get(0));
        var secondReloaded = new SampleTypeRequestDTO(original.get(1));
        assertEquals(Integer.valueOf(1), firstReloaded.getCultureSetNumber());
        assertEquals(Integer.valueOf(2), secondReloaded.getCultureSetNumber());
        persist(order, List.of(secondReloaded, firstReloaded));
        assertEquals(Integer.valueOf(1), sampleTypeRequestService.get(original.get(0).getId()).getCultureSetNumber());
        assertEquals(Integer.valueOf(2), sampleTypeRequestService.get(original.get(1).getId()).getCultureSetNumber());
        assertEquals(Integer.valueOf(0), sampleTypeRequestService.get(original.get(1).getId()).getSortOrder());
    }

    @Test
    public void collectingSecondBottleFirstUsesItsRequestAndSetNumber() {
        Sample order = newSample();
        var first = requested("1");
        first.setCultureSetNumber(1);
        var second = requested("1");
        second.setCultureSetNumber(2);
        persist(order, List.of(first, second));
        var requests = sampleTypeRequestService.getRequestsBySampleId(order.getId());
        persist(order, new SamplePatientEntryForm(),
                "<samples><sample typeId='" + sampleType.getId() + "' sampleTypeRequestId='" + requests.get(1).getId()
                        + "' tests='' panels='' testSectionMap='' testSampleTypeMap='' /></samples>");
        var collected = sampleTypeRequestService.get(requests.get(1).getId());
        assertEquals(SampleTypeRequest.Status.COLLECTED, collected.getStatus());
        assertEquals(Integer.valueOf(2),
                sampleItemService.get(collected.getSampleItem().getId()).getCultureSetNumber());
        assertEquals(SampleTypeRequest.Status.REQUESTED,
                sampleTypeRequestService.get(requests.get(0).getId()).getStatus());
    }

    @Test
    public void sampleXmlKeepsExplicitSetNumberOnSaveAndEdit() {
        Sample order = newSample();
        String attributes = " typeId='" + sampleType.getId()
                + "' tests='' panels='' testSectionMap='' testSampleTypeMap='' ";
        persist(order, new SamplePatientEntryForm(),
                "<samples><sample" + attributes + "cultureSetNumber='2'/></samples>");
        var item = sampleItemService.getSampleItemsBySampleId(order.getId()).get(0);
        assertEquals(Integer.valueOf(2), item.getCultureSetNumber());
        persist(order, new SamplePatientEntryForm(), "<samples><sample" + attributes + "sampleItemId='" + item.getId()
                + "' cultureSetNumber='3'/></samples>");
        assertEquals(Integer.valueOf(3), sampleItemService.get(item.getId()).getCultureSetNumber());
    }

    @Test
    public void requestedSpecimensArePersistedWithTheOrder() {
        Sample sample = newSample();

        persist(sample, List.of(requested("2.5")));

        assertNotNull(sample.getId());
        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        assertEquals(1, requests.size());
        assertEquals(sampleType.getId(), requests.getFirst().getTypeOfSample().getId());
        assertEquals(Double.valueOf("2.5"), requests.getFirst().getRequestedQuantity());
        assertEquals(SampleTypeRequest.Status.REQUESTED, requests.getFirst().getStatus());
    }

    @Test
    public void savingTheRequestedStageAgainDoesNotDuplicateSpecimens() {
        Sample sample = newSample();
        persist(sample, List.of(requested("2.5")));

        persist(sample, List.of(requested("4")));

        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        assertEquals(1, requests.size());
        assertEquals(Double.valueOf("4"), requests.getFirst().getRequestedQuantity());
    }

    @Test
    public void removingASpecimenCancelsThatSpecimenAndKeepsTheOther() {
        Sample sample = newSample();
        persist(sample, List.of(requested(sampleType, "2.5"), requested(secondSampleType, "1")));
        assertEquals(2, sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).size());

        persist(sample, List.of(requested(secondSampleType, "1")));

        List<SampleTypeRequest> pending = sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId());
        assertEquals(1, pending.size());
        assertEquals("the specimen still on the order is the one that stays pending", secondSampleType.getId(),
                pending.getFirst().getTypeOfSample().getId());
        assertEquals(Double.valueOf("1"), pending.getFirst().getRequestedQuantity());

        List<SampleTypeRequest> cancelled = sampleTypeRequestService.getRequestsBySampleId(sample.getId()).stream()
                .filter(request -> request.getStatus() == SampleTypeRequest.Status.CANCELLED).toList();
        assertEquals(1, cancelled.size());
        assertEquals("the specimen taken off the order is the one that is cancelled", sampleType.getId(),
                cancelled.getFirst().getTypeOfSample().getId());
    }

    @Test
    public void aSaveThatDoesNotMentionSpecimensLeavesThemUntouched() {
        Sample sample = newSample();
        persist(sample, List.of(requested("2.5")));

        persistWithoutSpecimenField(sample);

        List<SampleTypeRequest> pending = sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId());
        assertEquals(1, pending.size());
        assertEquals(SampleTypeRequest.Status.REQUESTED, pending.getFirst().getStatus());
    }

    @Test
    public void aSpecimenAlreadyCollectedIsNotRequestedAgainByALaterEntrySave() {
        Sample sample = newSample();
        persist(sample, List.of(requested("2.5")));
        SampleTypeRequest request = sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).getFirst();
        var collected = fixtures.createSampleWithSampleItem("COLLECTED");
        collected.setSample(sample);
        collected.setTypeOfSample(sampleType);
        collected.setSysUserId(userId);
        sampleItemService.update(collected);
        sampleTypeRequestService.fulfillRequest(request.getId(), collected.getId());

        persist(sample, List.of(requested("2.5")));

        List<SampleTypeRequest> all = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        assertEquals("a collected specimen must not gain a second, pending request", 1, all.size());
        assertEquals(SampleTypeRequest.Status.COLLECTED, all.getFirst().getStatus());
    }

    @Test
    public void removingEverySpecimenLeavesNoPendingRequest() {
        Sample sample = newSample();
        persist(sample, List.of(requested("2.5")));

        persist(sample, List.of());

        assertTrue("a specimen removed from the order must not stay pending",
                sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).isEmpty());
    }

    /**
     * A requested specimen the server cannot resolve rejects the whole save, so the
     * order never reaches a state where it exists without its specimens. Committing
     * afterwards is refused and the order is gone with it.
     */
    @Test
    public void anUnresolvableRequestedSpecimenLeavesNoOrderBehind() {
        Sample sample = newSample();
        String accessionNumber = sample.getAccessionNumber();

        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> persist(sample, List.of(requested(sampleType, "2.5"), unresolvableSpecimen())));
        assertEquals("Unknown requested sample type: -1", rejected.getMessage());

        TestTransaction.flagForCommit();
        assertThrows("the rejected save must not be allowed to commit", UnexpectedRollbackException.class,
                TestTransaction::end);

        TestTransaction.start();
        assertNull("a rejected save must leave no order behind",
                sampleService.getSampleByAccessionNumber(accessionNumber));
    }

    private SampleTypeRequestDTO unresolvableSpecimen() {
        SampleTypeRequestDTO unknown = new SampleTypeRequestDTO();
        unknown.setTypeOfSampleId("-1");
        return unknown;
    }

    private SampleTypeRequestDTO requested(String quantity) {
        return requested(sampleType, quantity);
    }

    private SampleTypeRequestDTO requested(TypeOfSample typeOfSample, String quantity) {
        SampleTypeRequestDTO requested = new SampleTypeRequestDTO();
        requested.setTypeOfSampleId(typeOfSample.getId());
        requested.setRequestedQuantity(Double.valueOf(quantity));
        return requested;
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("RSP" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    private void persistWithoutSpecimenField(Sample sample) {
        persist(sample, new SamplePatientEntryForm());
    }

    private void persist(Sample sample, List<SampleTypeRequestDTO> requestedSampleTypes) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setRequestedSampleTypes(requestedSampleTypes);
        persist(sample, form);
    }

    private void persist(Sample sample, SamplePatientEntryForm form) {
        persist(sample, form, "<samples></samples>");
    }

    private void persist(Sample sample, SamplePatientEntryForm form, String xml) {
        SampleAddService sampleAddService = new SampleAddService(xml, userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }
}
