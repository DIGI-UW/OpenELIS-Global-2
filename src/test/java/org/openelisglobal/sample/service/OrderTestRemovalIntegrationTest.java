package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.List;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1613: Enter Order lists a collected specimen's tests on its requested
 * row. A test taken off there is cancelled (results kept, reason noted), a test
 * added there is ordered, and a specimen the save lists without tests keeps
 * them.
 */
@Transactional
public class OrderTestRemovalIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleService sampleService;

    private String userId;
    private Patient patient;
    private TypeOfSample sampleType;
    private String cancelledStatusId;
    private SampleItem item;
    private Sample sample;
    private Test kept;
    private Test removed;
    private Analysis keptAnalysis;
    private Analysis removedAnalysis;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
        cancelledStatusId = fixtures.ensureAnalysisCanceledStatus();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("OTR");
        sampleType = fixtures.getOrCreateActiveSampleType();
        item = fixtures.createSampleWithSampleItem("OTR");
        sample = item.getSample();
        kept = orderableTest();
        removed = orderableTest();
        keptAnalysis = fixtures.createAnalysis(item, kept);
        removedAnalysis = fixtures.createAnalysis(item, removed);

        save(requested(null, ids(kept, removed)), null);
        SampleTypeRequest request = sampleTypeRequestService.getPendingRequestsBySampleId(sample.getId()).getFirst();
        sampleTypeRequestService.fulfillRequest(request.getId(), item.getId());
    }

    @org.junit.Test
    public void aTestTakenOffACollectedSpecimenIsCancelledWithItsReason() {
        save(requested(item.getId(), ids(kept)), "Duplicate order");

        assertEquals(cancelledStatusId, analysisService.get(removedAnalysis.getId()).getStatusId());
        assertNotEquals("the test still listed stays ordered", cancelledStatusId,
                analysisService.get(keptAnalysis.getId()).getStatusId());
        List<Note> notes = noteService.getNotes(analysisService.get(removedAnalysis.getId()));
        assertTrue("the reason is kept on the cancelled test",
                notes.stream().anyMatch(note -> "Duplicate order".equals(note.getText())));
    }

    @org.junit.Test
    public void aTestAddedToACollectedSpecimenIsOrderedOnIt() {
        Test added = orderableTest();

        save(requested(item.getId(), ids(kept, removed, added)), null);

        List<Analysis> forAdded = analysisService.getAnalysesBySampleItem(item).stream()
                .filter(analysis -> added.getId().equals(analysis.getTest().getId())).toList();
        assertEquals(1, forAdded.size());
        assertNotEquals(cancelledStatusId, forAdded.getFirst().getStatusId());
        assertEquals("the requested row follows the specimen's tests", ids(kept, removed, added),
                sampleTypeRequestService.getRequestsBySampleId(sample.getId()).getFirst().getRequestedTests());
    }

    @org.junit.Test
    public void aCollectedSpecimenListedWithoutTestsKeepsThem() {
        save(requested(item.getId(), ""), null);

        assertNotEquals(cancelledStatusId, analysisService.get(keptAnalysis.getId()).getStatusId());
        assertNotEquals(cancelledStatusId, analysisService.get(removedAnalysis.getId()).getStatusId());
    }

    @org.junit.Test
    public void aTestPutBackAfterItWasCancelledIsOrderedAgain() {
        save(requested(item.getId(), ids(kept)), "Duplicate order");

        save(requested(item.getId(), ids(kept, removed)), null);

        List<Analysis> forRemoved = analysisService.getAnalysesBySampleItem(item).stream()
                .filter(analysis -> removed.getId().equals(analysis.getTest().getId())).toList();
        assertEquals(2, forRemoved.size());
        assertEquals("one cancelled row and one ordered row", 1,
                forRemoved.stream().filter(analysis -> !cancelledStatusId.equals(analysis.getStatusId())).count());
    }

    @org.junit.Test
    public void anAnalysisDerivedFromAnotherIsNotTakenOffByAnOrderSave() {
        Analysis derived = fixtures.createAnalysis(item, orderableTest());
        derived.setParentAnalysis(keptAnalysis);
        analysisService.update(derived);

        save(requested(item.getId(), ids(kept, removed)), null);

        assertNotEquals(cancelledStatusId, analysisService.get(derived.getId()).getStatusId());
    }

    @org.junit.Test
    public void theManifestWinsOverAStaleRequestedRowForTheSameSpecimen() {
        java.util.Map<String, java.util.Set<String>> requested = OrderTestEdits.requestedTestsBySampleItem(
                List.of(manifest(item.getId(), kept)), List.of(requested(item.getId(), ids(kept, removed))),
                panelId -> List.of());

        assertEquals(java.util.Map.of(item.getId(), java.util.Set.of(kept.getId())), requested);
    }

    @org.junit.Test
    public void aManifestSpecimenWithoutTestsIsLeftOut() {
        assertTrue(OrderTestEdits
                .requestedTestsBySampleItem(List.of(manifest(item.getId())), List.of(), panelId -> List.of())
                .isEmpty());
    }

    private SampleAddService.SampleTestCollection manifest(String sampleItemId, Test... tests) {
        SampleAddService service = new SampleAddService("<samples></samples>", userId, sample, "");
        SampleAddService.SampleTestCollection collection = service.new SampleTestCollection(item, List.of(tests), null,
                null, null, null, null);
        collection.existingSampleItemId = sampleItemId;
        return collection;
    }

    private Test orderableTest() {
        Test test = fixtures.createCatalogTest();
        test.setTestSection(fixtures.createLabUnit());
        testService.update(test);
        return test;
    }

    private static String ids(Test... tests) {
        return String.join(",", java.util.Arrays.stream(tests).map(Test::getId).toList());
    }

    private SampleTypeRequestDTO requested(String sampleItemId, String testIds) {
        SampleTypeRequestDTO requested = new SampleTypeRequestDTO();
        requested.setTypeOfSampleId(sampleType.getId());
        requested.setSampleItemId(sampleItemId);
        requested.setRequestedTests(testIds);
        requested.setRequestedPanels("");
        return requested;
    }

    private void save(SampleTypeRequestDTO requested, String cancelReason) {
        sample = sampleService.get(sample.getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setRequestedSampleTypes(List.of(requested));
        form.setCancelReason(cancelReason);
        SampleAddService sampleAddService = new SampleAddService("<samples></samples>", userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());
        updateData.setCancelReason(cancelReason);
        sample.setEnteredDate(new Date(System.currentTimeMillis()));

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);

        samplePatientEntryService.persistData(updateData, SpringContext.getBean(PatientManagementUpdate.class),
                patientInfo, form, new MockHttpServletRequest());
    }
}
