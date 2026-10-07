package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
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
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reception saves do not own case information or derive cases from a Program.
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

    /**
     * The orders here are saved through the real services, which take the analysis
     * status from the JVM-wide status cache. A sibling class that reloads
     * {@code status_of_sample} leaves that cache stale, and a stale lookup answers
     * {@code -1}, which the analysis insert then fails on. Provision the workflow
     * statuses (and refresh the cache) first, as the other microbiology tests do.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
        userId = fixtures.defaultUserId();
        String methodId = fixtures.createMethodId();
        fixtures.createReferenceData(methodId);
        cultureTest = fixtures.createCatalogCultureTest(methodId, fixtures.createLabUnit());
        routineTest = fixtures.createCatalogTest();
        patient = fixtures.createPatient("MICROELIG");
        sampleType = fixtures.getOrCreateActiveSampleType();
    }

    @Test
    public void routineOrderDoesNotOpenCaseWhenMicrobiologyProgramSelected() {
        Sample sample = newSample();
        SamplePatientUpdateData update = orderUpdate(sample, routineTest);
        update.setProgramSample(microbiologyProgramSample());
        persist(update);
        assertTrue(caseService.getSiblingCases(update.getSampleItemsTests().getFirst().item.getId()).isEmpty());
    }

    @Test
    public void cultureOrderDoesNotCreateReceptionDetail() {
        SamplePatientUpdateData update = orderUpdate(newSample(), cultureTest);
        persist(update);
        List<MicroCase> cases = caseService.getSiblingCases(update.getSampleItemsTests().getFirst().item.getId());
        assertEquals(1, cases.size());
        assertNull(orderDetailService.getOrderDetail(cases.getFirst().getId()));
    }

    @Test
    public void laterOrderSaveDoesNotOverwriteInformationRecordedOnTheCase() {
        Sample sample = newSample();
        SamplePatientUpdateData update = orderUpdate(sample, cultureTest);
        persist(update);
        String caseId = caseService.getSiblingCases(update.getSampleItemsTests().getFirst().item.getId()).getFirst()
                .getId();
        orderDetailService.saveOrderDetail(caseId, orderDetail(), userId);
        persist(orderUpdate(sample, routineTest));
        assertEquals("Persistent fever after antibiotics",
                orderDetailService.getOrderDetail(caseId).getClinicalHistory());
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

    private void persist(SamplePatientUpdateData updateData) {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setPatientProperties(patientInfo);

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
