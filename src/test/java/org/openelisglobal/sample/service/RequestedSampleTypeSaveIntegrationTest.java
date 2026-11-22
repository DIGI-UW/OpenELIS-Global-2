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
        sampleTypeRequestService.fulfillRequest(request.getId(),
                fixtures.createSampleWithSampleItem("COLLECTED").getId());

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
        SampleAddService sampleAddService = new SampleAddService("<samples></samples>", userId, sample, "");
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
