package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;

public class TbSampleServiceTest extends BaseWebContextSensitiveTest {

    private static final String DATASET = "testdata/tb-sample-service.xml";
    private static final String SYS_USER_ID = TEST_SYS_USER_ID;

    private static final String SPECIMEN_NATURE_ID = "100";
    private static final String TEST_ID = "200";
    private static final String REFERRING_SITE_CODE = "100";

    private static final String EXISTING_PATIENT_EXTERNAL_ID = "SUB-900";
    private static final String EXISTING_SAMPLE_ACCESSION_NUMBER = "LAB-900";

    private static final List<ExpectedObservation> EXPECTED_OBSERVATIONS = List.of(
            new ExpectedObservation("100", "TbOrderReason", "Reason1"),
            new ExpectedObservation("101", "TbDiagnosticReason", "Diag1"),
            new ExpectedObservation("102", "TbFollowupReason", "Follow1"),
            new ExpectedObservation("103", "TbSampleAspects", "Aspect1"),
            new ExpectedObservation("104", "TbFollowupReasonPeriodLine1", "Period1"),
            new ExpectedObservation("105", "TbFollowupReasonPeriodLine2", "Period2"),
            new ExpectedObservation("106", "TbAnalysisMethod", "Method1"));

    private static final class ExpectedObservation {
        final String typeId;
        final String typeName;
        final String expectedValue;

        ExpectedObservation(String typeId, String typeName, String expectedValue) {
            this.typeId = typeId;
            this.typeName = typeName;
            this.expectedValue = expectedValue;
        }
    }

    @Autowired
    private TbSampleService tbSampleService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private PatientIdentityService patientIdentityService;

    @Autowired
    private PersonService personService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleOrganizationService sampleOrganizationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement(DATASET);
        resyncSequence("person_seq", "person");
        resyncSequence("patient_seq", "patient");
        resyncSequence("patient_identity_seq", "patient_identity");
        resyncSequence("sample_seq", "sample");
        resyncSequence("sample_item_seq", "sample_item");
        resyncSequence("sample_human_seq", "sample_human");
        resyncSequence("analysis_seq", "analysis");
        resyncSequence("provider_seq", "provider");
        resyncSequence("observation_history_seq", "observation_history");
        resyncSequence("patient_patient_type_seq", "patient_patient_type");

        ensureReferenceTable("SampleTbEntryForm");
    }

    private String generateUniqueSubjectNumber() {
        return "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateUniqueLabNumber() {
        return "LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private SampleTbEntryForm createBaseForm() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSysUserId(SYS_USER_ID);
        form.setPatientFirstName("Jane");
        form.setPatientLastName("Smith");
        form.setPatientGender("F");
        form.setPatientBirthDate("01/01/1990");
        form.setPatientPhone("555-1234");
        form.setPatientAddress("123 Main St");

        form.setTbSubjectNumber(generateUniqueSubjectNumber());

        form.setLabNo(generateUniqueLabNumber());
        form.setRequestDate(today());
        form.setReceivedDate(today());

        form.setTbSpecimenNature(SPECIMEN_NATURE_ID);

        List<String> tests = new ArrayList<>();
        tests.add(TEST_ID);
        form.setNewSelectedTests(tests);

        form.setReferringSiteCode(REFERRING_SITE_CODE);

        form.setProviderFirstName("Doc");
        form.setProviderLastName("Brown");

        form.setTbOrderReason("Reason1");
        form.setTbDiagnosticReason("Diag1");
        form.setTbFollowupReason("Follow1");
        form.setTbAspect("Aspect1");
        form.setTbFollowupPeriodLine1("Period1");
        form.setTbFollowupPeriodLine2("Period2");
        form.setSelectedTbMethod("Method1");

        return form;
    }

    @Test
    public void persistTbData_newPatient_createsPatientAndSampleHierarchy() {
        SampleTbEntryForm form = createBaseForm();
        String subjectNumber = form.getTbSubjectNumber();
        String labNo = form.getLabNo();

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue("Service should return true on success", result);

        Patient patient = patientService.getByExternalId(subjectNumber);
        assertEquals(subjectNumber, patient.getExternalId());
        assertEquals("Jane", patient.getPerson().getFirstName());
        assertEquals("Smith", patient.getPerson().getLastName());

        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertEquals(labNo, sample.getAccessionNumber());

        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
        assertEquals(1, items.size());

        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        assertEquals(1, analyses.size());
        assertEquals(TEST_ID, analyses.get(0).getTest().getId());
    }

    @Test
    public void persistTbData_existingPatient_updatesPatientAndCreatesSampleHierarchy() {
        Patient existingPatient = patientService.getByExternalId(EXISTING_PATIENT_EXTERNAL_ID);
        String expectedInternalId = existingPatient.getId();

        SampleTbEntryForm form = createBaseForm();
        String labNo = form.getLabNo();
        form.setTbSubjectNumber(EXISTING_PATIENT_EXTERNAL_ID);
        form.setPatientFirstName("UpdatedFirst");
        form.setPatientAddress("New Address");

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue(result);

        Patient patient = patientService.getByExternalId(EXISTING_PATIENT_EXTERNAL_ID);
        assertEquals(expectedInternalId, patient.getId());
        assertEquals("UpdatedFirst", patient.getPerson().getFirstName());

        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertEquals(labNo, sample.getAccessionNumber());
    }

    @Test(expected = IllegalArgumentException.class)
    public void persistTbData_existingSample_updateSample_throwsWhenIdNotSet() {
        Sample existingSample = sampleService.getSampleByAccessionNumber(EXISTING_SAMPLE_ACCESSION_NUMBER);

        SampleTbEntryForm form = createBaseForm();
        form.setSampleId(existingSample.getId());
        form.setLabNo(generateUniqueLabNumber());
        form.setRequestDate(today());
        form.setReceivedDate(today());

        tbSampleService.persistTbData(form, null);
    }

    @Test
    public void persistTbData_createsProviderFromFormFields() {
        SampleTbEntryForm form = createBaseForm();
        String labNo = form.getLabNo();

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue(result);

        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertEquals(labNo, sample.getAccessionNumber());
    }

    @Test
    public void persistTbData_insertsAllRequiredObservations() {
        SampleTbEntryForm form = createBaseForm();

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue(result);

        Sample sample = sampleService.getSampleByAccessionNumber(form.getLabNo());

        List<ObservationHistory> obs = observationHistoryService.getObservationHistoriesBySampleId(sample.getId());
        assertEquals("Should insert exactly " + EXPECTED_OBSERVATIONS.size() + " observation history records",
                EXPECTED_OBSERVATIONS.size(), obs.size());

        for (ExpectedObservation expected : EXPECTED_OBSERVATIONS) {
            boolean found = obs.stream().anyMatch(o -> expected.expectedValue.equals(o.getValue())
                    && expected.typeId.equals(o.getObservationHistoryTypeId()));
            assertTrue(expected.typeName + " observation should be present with value '" + expected.expectedValue + "'",
                    found);
        }
    }
}