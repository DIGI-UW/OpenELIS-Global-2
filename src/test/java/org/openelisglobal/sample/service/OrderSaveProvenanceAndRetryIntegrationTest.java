package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The order save records what the user picked and survives being repeated: a
 * test is attached to a panel only when that panel was chosen on the same
 * sample, a collected sample fulfils the specimen that was requested for it, a
 * retried save updates the samples the first attempt created, and a sample type
 * whose name is longer than 40 characters can be ordered.
 */
@Transactional
public class OrderSaveProvenanceAndRetryIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleRequesterService sampleRequesterService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    @Autowired
    private org.openelisglobal.test.service.TestSectionService testSections;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseDAO microCases;
    @Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseRequestDAO microRequests;

    private String userId;
    private Patient patient;
    private TypeOfSample sampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("PROV");
        sampleType = fixtures.createTypeOfSample();
    }

    @Test
    public void aTestIsAttachedOnlyToAPanelChosenOnTheSameSample() {
        org.openelisglobal.test.valueholder.Test test = catalogTest();
        Panel panel = panelContaining(test);
        SampleAddService sampleAddService = new SampleAddService(
                samplesXml(sampleXml(test, "", null), sampleXml(test, panel.getId(), null)), userId, newSample(), "");

        List<SampleTestCollection> collections = sampleAddService.createSampleTestCollection();

        assertNull("a test picked on its own is not attached to a panel chosen on another sample",
                sampleAddService.getPanelForTest(collections.get(0), test));
        assertEquals("a test picked through a panel keeps that panel", panel.getId(),
                sampleAddService.getPanelForTest(collections.get(1), test).getId());
    }

    @Test
    public void collectingARequestedSpecimenMarksItCollectedAndLinksTheSample() {
        Sample sample = newSample();
        persist(sample, "<samples></samples>", List.of(requested(), requested()));

        persist(sample, samplesXml(sampleXml(null, "", UUID.randomUUID().toString())), null);

        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
        assertEquals(1, items.size());
        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        List<SampleTypeRequest> collected = requests.stream()
                .filter(request -> request.getStatus() == SampleTypeRequest.Status.COLLECTED).toList();
        assertEquals("one sample fulfils exactly one of the two requests", 1, collected.size());
        assertEquals(items.getFirst().getId(), collected.getFirst().getSampleItem().getId());
        assertEquals("the second requested specimen is still awaited", 1,
                sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).size());
    }

    @Test
    public void explicitRequestIdentityCollectsTheChosenDuplicateAndCarriesItsRecordedDetails() {
        Sample sample = newSample();
        SampleTypeRequestDTO first = requested();
        first.setCultureSetNumber(1);
        first.setBodySite("Left arm");
        SampleTypeRequestDTO second = requested();
        second.setCultureSetNumber(2);
        second.setBodySite("Right arm");
        second.setContainer("Configured bottle");
        second.setCollectionDate("2026-10-07");
        second.setCollectionTime("14:25");
        persist(sample, "<samples></samples>", List.of(first, second));
        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        SampleTypeRequest chosen = requests.stream().filter(row -> Integer.valueOf(2).equals(row.getCultureSetNumber()))
                .findFirst().orElseThrow();
        String xml = sampleXml(null, "", UUID.randomUUID().toString()).replace("<sample ",
                "<sample sampleTypeRequestId='" + chosen.getId() + "' ");
        persist(sample, samplesXml(xml), null);
        persist(sample, samplesXml(xml), null);
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
        assertEquals(1, items.size());
        assertEquals(Integer.valueOf(2), items.getFirst().getCultureSetNumber());
        assertEquals("Right arm", items.getFirst().getBodySite());
        assertEquals("Configured bottle", items.getFirst().getContainer());
        assertEquals(Timestamp.valueOf("2026-10-07 14:25:00"), items.getFirst().getCollectionDate());
        assertEquals(items.getFirst().getId(), sampleTypeRequestService.get(chosen.getId()).getSampleItem().getId());
        assertEquals(1, sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).size());
    }

    @Test
    public void collectingAPendingCaseTestKeepsOneCaseAndCreatesNoResultAnalysis() {
        Sample sample = newSample();
        org.openelisglobal.test.valueholder.Test test = entityManager
                .find(org.openelisglobal.test.valueholder.Test.class, catalogTest().getId());
        test.setTestSection(testSections.getAllActiveTestSections().getFirst());
        test.setOpensMicrobiologyCase(true);
        test.setMicrobiologyCaseRole("CASE");
        entityManager.flush();
        SampleTypeRequestDTO requested = requested();
        requested.setRequestedTests(test.getId());
        persist(sample, "<samples></samples>", List.of(requested));
        SampleTypeRequest request = sampleTypeRequestService.getRequestsBySampleId(sample.getId()).getFirst();
        String caseId = microRequests.getActiveByRequestAndTest(request.getId(), test.getId()).getCaseId();
        String xml = sampleXml(test, "", UUID.randomUUID().toString()).replace("<sample ",
                "<sample sampleTypeRequestId='" + request.getId() + "' ");
        persist(sample, samplesXml(xml), null);
        persist(sample, samplesXml(xml), null);
        assertEquals(1, microCases.getByOrder(sample.getId()).size());
        assertEquals(1, sampleTypeRequestService.getRequestsBySampleId(sample.getId()).size());
        SampleItem collected = sampleItemService.getSampleItemsBySampleId(sample.getId()).getFirst();
        assertTrue(analysisService.getAnalysesBySampleItem(collected).isEmpty());
        assertEquals(caseId, microRequests.getActiveByRequestAndTest(request.getId(), test.getId()).getCaseId());
        assertEquals(collected.getId(),
                microRequests.getActiveByRequestAndTest(request.getId(), test.getId()).getSampleItemId());
        assertNull(collected.getCollectionDate());
        assertNull(collected.getReceivedDate());
    }

    @Test
    public void reorderingSameTypeRequestsKeepsTheirIdentitiesAndUpdatesTheChosenBottle() {
        Sample sample = newSample();
        SampleTypeRequestDTO first = requested();
        first.setCultureSetNumber(1);
        SampleTypeRequestDTO second = requested();
        second.setCultureSetNumber(2);
        persist(sample, "<samples></samples>", List.of(first, second));
        List<SampleTypeRequest> requests = sampleTypeRequestService.getRequestsBySampleId(sample.getId());
        SampleTypeRequestDTO storedFirst = new SampleTypeRequestDTO(requests.get(0));
        SampleTypeRequestDTO storedSecond = new SampleTypeRequestDTO(requests.get(1));
        storedSecond.setBodySite("Right arm");
        persist(sample, "<samples></samples>", List.of(storedSecond, storedFirst));
        assertEquals(Integer.valueOf(2),
                sampleTypeRequestService.get(Integer.valueOf(storedSecond.getId())).getCultureSetNumber());
        assertEquals("Right arm", sampleTypeRequestService.get(Integer.valueOf(storedSecond.getId())).getBodySite());
        assertNull(sampleTypeRequestService.get(Integer.valueOf(storedFirst.getId())).getBodySite());
        assertEquals(2, sampleTypeRequestService.getRequestsBySampleId(sample.getId()).size());
    }

    @Test
    public void aRetriedCollectSaveUpdatesTheSampleTheFirstAttemptCreated() {
        Sample sample = newSample();
        persist(sample, "<samples></samples>", List.of(requested(), requested()));
        String collectXml = samplesXml(sampleXml(null, "", UUID.randomUUID().toString()));

        persist(sample, collectXml, null);
        persist(sample, collectXml, null);

        assertEquals("the retry must not create a second sample", 1,
                sampleItemService.getSampleItemsBySampleId(sample.getId()).size());
        assertEquals("the retry must not fulfil the second request", 1,
                sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).size());
    }

    @Test
    public void twoSamplesWithTheirOwnKeysAreBothCreated() {
        Sample sample = newSample();
        persist(sample, "<samples></samples>", List.of(requested(), requested()));

        persist(sample, samplesXml(sampleXml(null, "", UUID.randomUUID().toString()),
                sampleXml(null, "", UUID.randomUUID().toString())), null);

        assertEquals(2, sampleItemService.getSampleItemsBySampleId(sample.getId()).size());
        assertEquals(0, sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).size());
    }

    @Test
    public void aVectorPoolFannedOutOnSaveFulfilsItsRequestWithOneOfTheOrganisms() {
        Sample sample = newSample();
        sample.setDomain("V");
        persist(sample, "<samples></samples>", List.of(requested()));

        persist(sample, samplesXml(sampleXml(null, "", UUID.randomUUID().toString()).replace("rejected='false'",
                "rejected='false' quantity='3'")), null);

        List<SampleItem> organisms = sampleItemService.getSampleItemsBySampleId(sample.getId());
        assertEquals("the pool of three is saved as three organisms", 3, organisms.size());
        SampleTypeRequest request = sampleTypeRequestService.getRequestsBySampleId(sample.getId()).getFirst();
        assertEquals(SampleTypeRequest.Status.COLLECTED, request.getStatus());
        assertTrue("the request points at a sample that still exists",
                organisms.stream().anyMatch(organism -> organism.getId().equals(request.getSampleItem().getId())));
    }

    @Test
    public void savingTheOrderAgainKeepsOneRowForItsSiteAndAChangedSiteReplacesIt() {
        Sample sample = newSample();
        Organization firstSite = organization("Kila Health Centre");
        Organization secondSite = organization("Moyo District Hospital");

        persistWithSite(sample, firstSite);
        persistWithSite(sample, firstSite);
        assertEquals(List.of(Long.valueOf(firstSite.getId())), organizationRequesterIds(sample));

        persistWithSite(sample, secondSite);
        assertEquals(List.of(Long.valueOf(secondSite.getId())), organizationRequesterIds(sample));
    }

    @Test
    public void aSampleTypeNamedLongerThanFortyCharactersCanBeOrdered() {
        String longName = "Nasopharyngeal Swab from Disease-Bearing Animal";
        Localization localization = localizationService.get(sampleType.getLocalization().getId());
        localization.setEnglish(longName);
        localization.setFrench(longName);
        localizationService.update(localization);
        org.openelisglobal.test.valueholder.Test test = catalogTest();
        Sample sample = newSample();
        persist(sample, "<samples></samples>", List.of());

        persist(sample, samplesXml(sampleXml(test, "", UUID.randomUUID().toString())), null);

        SampleItem item = sampleItemService.getSampleItemsBySampleId(sample.getId()).getFirst();
        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(item);
        assertEquals(1, analyses.size());
        assertNotNull(analyses.getFirst().getId());
        assertEquals(longName, analyses.getFirst().getSampleTypeName());
    }

    private Organization organization(String name) {
        Organization organization = new Organization();
        organization.setOrganizationName(name + " " + UUID.randomUUID().toString().substring(0, 6));
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("N");
        organization.setSysUserId(userId);
        organization.setId(organizationService.insert(organization));
        return organization;
    }

    private List<Long> organizationRequesterIds(Sample sample) {
        long organizationType = TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID;
        return sampleRequesterService.getRequestersForSampleId(sample.getId()).stream()
                .filter(requester -> requester.getRequesterTypeId() == organizationType)
                .map(SampleRequester::getRequesterId).toList();
    }

    private void persistWithSite(Sample order, Organization site) {
        SampleRequester siteRequester = new SampleRequester();
        siteRequester.setRequesterId(site.getId());
        siteRequester.setRequesterTypeId(TableIdService.getInstance().ORGANIZATION_REQUESTER_TYPE_ID);
        siteRequester.setSysUserId(userId);
        persist(order, "<samples></samples>", List.of(), siteRequester);
    }

    private org.openelisglobal.test.valueholder.Test catalogTest() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName("ProvenanceIT " + suffix);
        test.setDescription("ProvenanceIT " + suffix);
        test.setIsActive("Y");
        test.setGuid(UUID.randomUUID().toString());
        test.setDomain("CLINICAL");
        test.setOrderable(true);
        test.setSysUserId(userId);
        testService.insert(test);
        return test;
    }

    private Panel panelContaining(org.openelisglobal.test.valueholder.Test test) {
        String name = "Provenance panel " + UUID.randomUUID().toString().substring(0, 8);
        Localization localization = new Localization();
        localization.setDescription("Order save provenance test panel");
        localization.setEnglish(name);
        localization.setFrench(name);
        localization.setSysUserId(userId);
        localization.setId(localizationService.insert(localization));

        Panel panel = new Panel();
        panel.setPanelName(name);
        panel.setLocalization(localization);
        panel.setDescription("Order save provenance test panel");
        panel.setIsActive("Y");
        panel.setSysUserId(userId);
        panel.setId(panelService.insert(panel));

        PanelItem item = new PanelItem();
        item.setPanel(panel);
        item.setTest(test);
        item.setSortOrder("1");
        item.setSysUserId(userId);
        panelItemService.insert(item);
        return panel;
    }

    private String samplesXml(String... samples) {
        return "<samples>" + String.join("", samples) + "</samples>";
    }

    private String sampleXml(org.openelisglobal.test.valueholder.Test test, String panelIds, String clientKey) {
        return "<sample sampleID='1' typeId='" + sampleType.getId() + "' sampleItemId='' clientKey='"
                + (clientKey == null ? "" : clientKey) + "' tests='" + (test == null ? "" : test.getId()) + "' panels='"
                + panelIds + "' testSectionMap='' testSampleTypeMap='' rejected='false'/>";
    }

    private SampleTypeRequestDTO requested() {
        SampleTypeRequestDTO requested = new SampleTypeRequestDTO();
        requested.setTypeOfSampleId(sampleType.getId());
        return requested;
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("PRV" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    /**
     * Each save starts from the order as stored, the way the endpoint loads it by
     * id on every request, so a repeated save does not carry a stale version.
     */
    private void persist(Sample order, String sampleXml, List<SampleTypeRequestDTO> requestedSampleTypes) {
        persist(order, sampleXml, requestedSampleTypes, null);
    }

    private void persist(Sample order, String sampleXml, List<SampleTypeRequestDTO> requestedSampleTypes,
            SampleRequester requesterSite) {
        Sample sample = order.getId() == null ? order : sampleService.get(order.getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setRequestedSampleTypes(requestedSampleTypes);
        SampleAddService sampleAddService = new SampleAddService(sampleXml, userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());
        updateData.setRequesterSite(requesterSite);

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }
}
