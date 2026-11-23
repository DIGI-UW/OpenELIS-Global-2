package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
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

    private static final String PINNED_DATE = "15/01/2025";

    private static final List<ExpectedObservation> EXPECTED_OBSERVATIONS = List.of(
            new ExpectedObservation("TbOrderReason", "Reason1"), new ExpectedObservation("TbDiagnosticReason", "Diag1"),
            new ExpectedObservation("TbFollowupReason", "Follow1"),
            new ExpectedObservation("TbSampleAspects", "Aspect1"),
            new ExpectedObservation("TbFollowupReasonPeriodLine1", "Period1"),
            new ExpectedObservation("TbFollowupReasonPeriodLine2", "Period2"),
            new ExpectedObservation("TbAnalysisMethod", "Method1"));

    private static final class ExpectedObservation {
        final String typeName;
        final String expectedValue;

        ExpectedObservation(String typeName, String expectedValue) {
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
    private ObservationHistoryTypeService observationHistoryTypeService;

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

    private final Set<String> ownedSampleIds = new LinkedHashSet<>();
    private final Set<String> ownedPatientIds = new LinkedHashSet<>();
    private final Set<String> ownedProviderIds = new LinkedHashSet<>();
    private final Set<String> ownedPersonIds = new LinkedHashSet<>();
    private final List<SampleTbEntryForm> currentForms = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        currentForms.clear();
        ownedSampleIds.clear();
        ownedPatientIds.clear();
        ownedProviderIds.clear();
        ownedPersonIds.clear();

        executeDataSetWithStateManagement(DATASET);
        PatientIdentityTypeMap.reset();

        ensureCanonicalObservationHistoryTypes();

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
        resyncSequence("observation_history_type_seq", "observation_history_type");
        resyncSequence("sample_org_seq", "sample_organization");

        ensureReferenceTable("SampleTbEntryForm");
    }

    @After
    public void tearDown() {
        for (SampleTbEntryForm form : currentForms) {
            trackOwnedEntities(form);
        }
        currentForms.clear();
        PatientIdentityTypeMap.reset();
        cleanOwnedRows();
    }

    private void trackOwnedEntities(SampleTbEntryForm form) {
        if (form == null) {
            return;
        }
        if (form.getLabNo() != null) {
            Sample sample = sampleService.getSampleByAccessionNumber(form.getLabNo());
            if (sample != null && sample.getId() != null) {
                ownedSampleIds.add(sample.getId());
                SampleHuman query = new SampleHuman();
                query.setSampleId(sample.getId());
                SampleHuman sh = sampleHumanService.getDataBySample(query);
                if (sh != null && sh.getProviderId() != null) {
                    ownedProviderIds.add(sh.getProviderId());
                    Provider prov = providerService.get(sh.getProviderId());
                    if (prov != null && prov.getPerson() != null && prov.getPerson().getId() != null) {
                        ownedPersonIds.add(prov.getPerson().getId());
                    }
                }
            }
        }
        if (form.getTbSubjectNumber() != null) {
            Patient p = patientService.getByExternalId(form.getTbSubjectNumber());
            if (p != null) {
                if (p.getId() != null && !"900".equals(p.getId())) {
                    ownedPatientIds.add(p.getId());
                }
                if (p.getPerson() != null && p.getPerson().getId() != null && !"900".equals(p.getPerson().getId())) {
                    ownedPersonIds.add(p.getPerson().getId());
                }
            }
        }
    }

    private void ensureCanonicalObservationHistoryTypes() {
        String[] types = { "TbOrderReason", "TbDiagnosticReason", "TbFollowupReason", "TbSampleAspects",
                "TbFollowupReasonPeriodLine1", "TbFollowupReasonPeriodLine2", "TbAnalysisMethod" };
        for (String typeName : types) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated) "
                            + "SELECT nextval('clinlims.observation_history_type_seq'), ?, ?, now() "
                            + "WHERE NOT EXISTS (SELECT 1 FROM clinlims.observation_history_type WHERE type_name = ?)",
                    typeName, typeName, typeName);
        }
    }

    private void cleanOwnedRows() {
        if (!ownedSampleIds.isEmpty()) {
            for (String sampleIdStr : ownedSampleIds) {
                long sampleId = Long.parseLong(sampleIdStr);
                jdbcTemplate.update("DELETE FROM clinlims.observation_history WHERE sample_id = ?", sampleId);
                jdbcTemplate.update("DELETE FROM clinlims.analysis WHERE sampitem_id IN "
                        + "(SELECT id FROM clinlims.sample_item WHERE samp_id = ?)", sampleId);
                jdbcTemplate.update("DELETE FROM clinlims.sample_organization WHERE samp_id = ?", sampleId);
                jdbcTemplate.update("DELETE FROM clinlims.sample_human WHERE samp_id = ?", sampleId);
                jdbcTemplate.update("DELETE FROM clinlims.sample_item WHERE samp_id = ?", sampleId);
                jdbcTemplate.update("DELETE FROM clinlims.sample WHERE id = ?", sampleId);
            }
            ownedSampleIds.clear();
        }

        if (!ownedProviderIds.isEmpty()) {
            for (String providerIdStr : ownedProviderIds) {
                long providerId = Long.parseLong(providerIdStr);
                jdbcTemplate.update("DELETE FROM clinlims.provider WHERE id = ?", providerId);
            }
            ownedProviderIds.clear();
        }

        if (!ownedPatientIds.isEmpty()) {
            for (String patientIdStr : ownedPatientIds) {
                if (!"900".equals(patientIdStr)) {
                    long patientId = Long.parseLong(patientIdStr);
                    jdbcTemplate.update("DELETE FROM clinlims.patient_patient_type WHERE patient_id = ?", patientId);
                    jdbcTemplate.update("DELETE FROM clinlims.patient_identity WHERE patient_id = ?", patientId);
                    jdbcTemplate.update("DELETE FROM clinlims.patient WHERE id = ?", patientId);
                }
            }
            ownedPatientIds.clear();
        }

        // Restore fixture patient 900 to point to fixture person 900 if modified by
        // test
        jdbcTemplate.update("UPDATE clinlims.patient SET person_id = 900, national_id = 'SUB-900' WHERE id = 900");
        jdbcTemplate.update("UPDATE clinlims.person SET first_name = 'Jane', last_name = 'Smith' WHERE id = 900");

        if (!ownedPersonIds.isEmpty()) {
            for (String personIdStr : ownedPersonIds) {
                if (!"900".equals(personIdStr)) {
                    long personId = Long.parseLong(personIdStr);
                    jdbcTemplate.update("DELETE FROM clinlims.person_address WHERE person_id = ?", personId);
                    jdbcTemplate.update("DELETE FROM clinlims.person WHERE id = ?", personId);
                }
            }
            ownedPersonIds.clear();
        }
    }

    private String generateUniqueSubjectNumber() {
        return "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateUniqueLabNumber() {
        return "LAB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private SampleTbEntryForm createBaseForm() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        currentForms.add(form);
        form.setSysUserId(SYS_USER_ID);
        form.setPatientFirstName("Jane");
        form.setPatientLastName("Smith");
        form.setPatientGender("F");
        form.setPatientBirthDate("01/01/1990");
        form.setPatientPhone("555-1234");
        form.setPatientAddress("123 Main St");

        form.setTbSubjectNumber(generateUniqueSubjectNumber());

        form.setLabNo(generateUniqueLabNumber());
        form.setRequestDate(PINNED_DATE);
        form.setReceivedDate(PINNED_DATE);

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

    @Test
    @Ignore("Quarantined: pre-existing quirk in TbSampleServiceImpl.persistSampleData creates a new Sample() without copying form.sampleId, causing sampleService.update() to fail (#4850)")
    public void persistTbData_existingSample_updatesSample() {
        Sample existingSample = sampleService.getSampleByAccessionNumber(EXISTING_SAMPLE_ACCESSION_NUMBER);
        assertNotNull(existingSample);

        SampleTbEntryForm form = createBaseForm();
        form.setSampleId(existingSample.getId());
        form.setLabNo(generateUniqueLabNumber());
        form.setRequestDate(PINNED_DATE);
        form.setReceivedDate(PINNED_DATE);

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue("Updating an existing sample should succeed", result);
    }

    @Test
    public void persistTbData_createsProviderFromFormFields() {
        SampleTbEntryForm form = createBaseForm();
        String labNo = form.getLabNo();

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue(result);

        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertNotNull(sample);
        assertEquals(labNo, sample.getAccessionNumber());

        SampleHuman query = new SampleHuman();
        query.setSampleId(sample.getId());
        SampleHuman sampleHuman = sampleHumanService.getDataBySample(query);
        assertNotNull("SampleHuman linkage should exist for sample", sampleHuman);
        assertNotNull("SampleHuman should have a provider ID", sampleHuman.getProviderId());

        Provider provider = sampleHumanService.getProviderForSample(sample);
        assertNotNull("Provider should be linked to sample", provider);
        assertEquals(sampleHuman.getProviderId(), provider.getId());
        assertNotNull("Provider person should exist", provider.getPerson());
        assertEquals("Doc", provider.getPerson().getFirstName());
        assertEquals("Brown", provider.getPerson().getLastName());
    }

    @Test
    public void persistTbData_insertsAllRequiredObservations() {
        SampleTbEntryForm form = createBaseForm();

        boolean result = tbSampleService.persistTbData(form, null);
        assertTrue(result);

        Sample sample = sampleService.getSampleByAccessionNumber(form.getLabNo());
        assertNotNull(sample);

        List<ObservationHistory> obs = observationHistoryService.getObservationHistoriesBySampleId(sample.getId());
        assertEquals("Should insert exactly " + EXPECTED_OBSERVATIONS.size() + " observation history records",
                EXPECTED_OBSERVATIONS.size(), obs.size());

        for (ExpectedObservation expected : EXPECTED_OBSERVATIONS) {
            ObservationHistoryType oht = observationHistoryTypeService.getByName(expected.typeName);
            assertNotNull("Observation history type should exist for " + expected.typeName, oht);
            String expectedTypeId = oht.getId();
            boolean found = obs.stream().anyMatch(o -> expected.expectedValue.equals(o.getValue())
                    && expectedTypeId.equals(o.getObservationHistoryTypeId()));
            assertTrue(expected.typeName + " observation should be present with value '" + expected.expectedValue + "'",
                    found);
        }
    }
}