package org.openelisglobal.microbiology;

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
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The order-entry invariant: microbiology details belong only to orders that
 * qualify for the microbiology workflow. A client may submit the details object
 * on any order, so the server decides eligibility itself rather than trusting
 * the payload.
 */
@Transactional
public class MicrobiologyOrderEligibilityIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseOrderDetailService orderDetailService;

    private String userId;
    private org.openelisglobal.test.valueholder.Test cultureTest;
    @Autowired
    private ProgramService programService;

    private org.openelisglobal.test.valueholder.Test routineTest;
    private Patient patient;
    private TypeOfSample sampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        String methodId = fixtures.createMethodId();
        fixtures.createReferenceData(methodId);
        cultureTest = fixtures.createCatalogCultureTest(methodId, MicroWorkflowType.BACTERIOLOGY);
        routineTest = fixtures.createCatalogTest();
        patient = fixtures.createPatient("MICROELIG");
        sampleType = fixtures.getOrCreateActiveSampleType();
    }

    @Test
    public void routineOrderKeepsNoMicrobiologyDetailEvenWhenTheClientSubmitsIt() {
        Sample sample = newSample();
        SamplePatientUpdateData update = orderUpdate(sample, routineTest);

        persist(update, orderDetail());

        SampleItem savedItem = update.getSampleItemsTests().getFirst().item;
        List<MicroCase> cases = caseService.getSiblingCases(savedItem.getId());

        assertNotNull(sample.getId());
        assertTrue("a routine order must not create a microbiology case", cases.isEmpty());
        assertNull("a routine order must not retain submitted microbiology details",
                orderDetailService.getOrderDraft(sample.getId()));
    }

    @Test
    public void cultureOrderStillRetainsItsSubmittedMicrobiologyDetail() {
        Sample sample = newSample();
        SamplePatientUpdateData update = orderUpdate(sample, cultureTest);
        MicroCaseOrderDetailRequestForm submitted = orderDetail();

        persist(update, submitted);

        SampleItem savedItem = update.getSampleItemsTests().getFirst().item;
        assertEquals(1, caseService.getSiblingCases(savedItem.getId()).size());

        MicroCaseOrderDetailRequestForm retained = orderDetailService.getOrderDraft(sample.getId());
        assertNotNull("a culture order must retain the details entered at order entry", retained);
        assertEquals(submitted.clinicalHistory, retained.clinicalHistory);
    }

    @Test
    public void aSubmittedTestClaimingCultureWorkflowDoesNotMakeTheOrderMicrobiology() {
        Sample sample = newSample();
        SamplePatientUpdateData update = orderUpdate(sample, routineTest);
        // The client sends a routine test but marks it as culture work.
        update.getSampleItemsTests().forEach(collection -> collection.tests
                .forEach(test -> test.setCultureWorkflowType(MicroWorkflowType.BACTERIOLOGY.name())));

        persist(update, orderDetail());

        assertNull("eligibility must come from the catalog, not the submitted test",
                orderDetailService.getOrderDraft(sample.getId()));
    }

    @Test
    public void anExplicitMicrobiologyProgramQualifiesTheOrderTheRequestedStageActuallySends() {
        Sample sample = newSample();
        // The requested stage sends no sample XML at all, so the program is the
        // only thing that can qualify the order at that point.
        SamplePatientUpdateData update = orderUpdateWithoutTests(sample);
        update.setProgramSample(microbiologyProgramSample());
        MicroCaseOrderDetailRequestForm submitted = orderDetail();

        persist(update, submitted);

        MicroCaseOrderDetailRequestForm retained = orderDetailService.getOrderDraft(sample.getId());
        assertNotNull("an explicitly selected Microbiology program must qualify the order", retained);
        assertEquals(submitted.clinicalHistory, retained.clinicalHistory);
    }

    @Test
    public void anOrderThatStopsQualifyingDiscardsTheDetailItCaptured() {
        Sample sample = newSample();
        persist(orderUpdate(sample, cultureTest), orderDetail());
        assertNotNull(orderDetailService.getOrderDraft(sample.getId()));

        persist(orderUpdate(sample, routineTest), null);

        assertNull("details must not outlive the order's eligibility",
                orderDetailService.getOrderDraft(sample.getId()));
    }

    /**
     * A detail row is owned by exactly one of the case or the sample, so a discard
     * keyed on the sample cannot reach a case's row. This pins that a save which no
     * longer qualifies still leaves the case's details in place.
     */
    @Test
    public void detailsAlreadyBelongingToACaseSurviveASaveThatNoLongerQualifies() {
        Sample sample = newSample();
        SamplePatientUpdateData cultureUpdate = orderUpdate(sample, cultureTest);
        persist(cultureUpdate, orderDetail());
        String caseId = caseService.getSiblingCases(cultureUpdate.getSampleItemsTests().getFirst().item.getId())
                .getFirst().getId();
        orderDetailService.saveOrderDetail(caseId, orderDetail(), userId);

        persist(orderUpdate(sample, routineTest), null);

        assertNotNull("details recorded against a case belong to the case, not the order entry",
                orderDetailService.getOrderDetail(caseId));
    }

    @Test
    public void requestedStageQualifiesFromRequestedSampleTypesWhenNoProgramIsSelected() {
        Sample sample = newSample();
        SamplePatientUpdateData update = orderUpdateWithoutTests(sample);
        org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO requested = new org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO();
        requested.setTypeOfSampleId(sampleType.getId());
        requested.setRequestedTests(cultureTest.getId());

        persist(update, orderDetail(), java.util.List.of(requested));

        assertNotNull("requested culture tests must qualify the order before the program loads",
                orderDetailService.getOrderDraft(sample.getId()));
    }

    private ProgramSample microbiologyProgramSample() {
        Program program = new Program();
        program.setCode("MICROBIOLOGY");
        program.setProgramName("Microbiology " + UUID.randomUUID().toString().substring(0, 6));
        program.setManuallyChanged(false);
        program.setSysUserId(userId);
        program.setId(programService.insert(program));

        ProgramSample programSample = new ProgramSample();
        programSample.setProgram(program);
        programSample.setSysUserId(userId);
        return programSample;
    }

    private SamplePatientUpdateData orderUpdateWithoutTests(Sample sample) {
        SampleAddService sampleAddService = new SampleAddService("", userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());
        return updateData;
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("MEL" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    private SamplePatientUpdateData orderUpdate(Sample sample, org.openelisglobal.test.valueholder.Test test) {
        String sampleXml = "<samples><sample sampleID='" + sampleType.getId() + "' tests='" + test.getId()
                + "' testSectionMap='' testSampleTypeMap='' panels='' date='' time='' initialConditionIds=''/></samples>";
        SampleAddService sampleAddService = new SampleAddService(sampleXml, userId, sample, "");

        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());
        return updateData;
    }

    private void persist(SamplePatientUpdateData updateData, MicroCaseOrderDetailRequestForm orderDetail) {
        persist(updateData, orderDetail, null);
    }

    private void persist(SamplePatientUpdateData updateData, MicroCaseOrderDetailRequestForm orderDetail,
            java.util.List<org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO> requestedSampleTypes) {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setPatientProperties(patientInfo);
        form.setMicrobiologyOrderDetail(orderDetail);
        form.setRequestedSampleTypes(requestedSampleTypes);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }

    private MicroCaseOrderDetailRequestForm orderDetail() {
        MicroCaseOrderDetailRequestForm detail = new MicroCaseOrderDetailRequestForm();
        detail.culturePurpose = "CLINICAL_DIAGNOSTIC";
        detail.patientOrigin = "INPATIENT";
        detail.numberOfSets = 2;
        detail.clinicalHistory = "Persistent fever after antibiotics";
        detail.antibioticExposure = true;
        return detail;
    }
}
