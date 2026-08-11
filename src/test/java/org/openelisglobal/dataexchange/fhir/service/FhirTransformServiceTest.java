package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class FhirTransformServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OrganizationService organizationService;

    private FhirPersistanceService mockFhirPersistanceService;
    private Object originalFhirPersistanceService;
    private Object fhirTransformServiceTarget;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");

        mockFhirPersistanceService = mock(FhirPersistanceService.class);
        fhirTransformServiceTarget = AopTestUtils.getTargetObject(fhirTransformService);
        originalFhirPersistanceService = ReflectionTestUtils.getField(fhirTransformServiceTarget,
                "fhirPersistanceService");
        ReflectionTestUtils.setField(fhirTransformServiceTarget, "fhirPersistanceService", mockFhirPersistanceService);
    }

    @After
    public void tearDown() throws Exception {
        if (fhirTransformServiceTarget != null) {
            ReflectionTestUtils.setField(fhirTransformServiceTarget, "fhirPersistanceService",
                    originalFhirPersistanceService);
        }
        cleanRowsInCurrentConnection(new String[] { "clinlims.analysis", "clinlims.analyte", "clinlims.dictionary",
                "clinlims.dictionary_category", "clinlims.label", "clinlims.localization",
                "clinlims.localization_value", "clinlims.method", "clinlims.observation_history",
                "clinlims.observation_history_type", "clinlims.organization", "clinlims.panel", "clinlims.panel_item",
                "clinlims.patient", "clinlims.person", "clinlims.provider", "clinlims.report", "clinlims.result",
                "clinlims.result_signature", "clinlims.sample", "clinlims.sample_human", "clinlims.sample_item",
                "clinlims.sampletype_test", "clinlims.scriptlet", "clinlims.status_of_sample",
                "clinlims.supported_locale", "clinlims.system_user", "clinlims.test", "clinlims.test_analyte",
                "clinlims.test_formats", "clinlims.test_result", "clinlims.test_section", "clinlims.test_trailer",
                "clinlims.type_of_sample", "clinlims.unit_of_measure" });
    }

    @Test
    public void transformToFhirPatient_shouldReturnPopulatedFhirPatient_whenValidPatientIdProvided() throws Exception {
        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("1");
        assertEquals("John", fhirPatient.getNameFirstRep().getGivenAsSingleString());
        assertEquals("Doe", fhirPatient.getNameFirstRep().getFamily());
        assertEquals("550e8400-e29b-41d4-a716-446655440001", fhirPatient.getId());
        assertEquals(AdministrativeGender.UNKNOWN, fhirPatient.getGender());
    }

    @Test
    public void createOePatientManagementInfo_shouldMapAllFields_whenFhirPatientProvided() {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        fhirPatient.setId("12345");
        HumanName name = fhirPatient.addName();
        name.addGiven("Alice").setFamily("Smith");
        fhirPatient.setGender(AdministrativeGender.FEMALE);
        fhirPatient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType("1995-05-15"));

        PatientManagementInfo info = fhirTransformService.createOePatientManagementInfo(fhirPatient);
        assertEquals("Alice", info.getFirstName());
        assertEquals("Smith", info.getLastName());
        assertEquals("F", info.getGender());
        assertEquals("15/05/1995", info.getBirthDateForDisplay());
    }

    @Test
    public void transformToOpenElisPatientSearchResults_shouldMapAllFields_whenFhirPatientProvided() {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        HumanName name = fhirPatient.addName();
        name.addGiven("Bob").setFamily("Jones");
        fhirPatient.setGender(AdministrativeGender.MALE);
        fhirPatient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType("1990-01-01"));

        PatientSearchResults results = fhirTransformService.transformToOpenElisPatientSearchResults(fhirPatient);
        assertEquals("Bob", results.getFirstName());
        assertEquals("Jones", results.getLastName());
        assertEquals("M", results.getGender());
        assertEquals("01/01/1990", results.getBirthdate());
    }

    @Test
    public void transformToFhirOrganization_shouldReturnPopulatedFhirOrganization_whenValidOrganizationProvided()
            throws Exception {
        Organization org = organizationService.get("3");

        org.hl7.fhir.r4.model.Organization fhirOrg = fhirTransformService.transformToFhirOrganization(org);
        assertEquals("Global Health Org", fhirOrg.getName());
        assertEquals("New York", fhirOrg.getAddressFirstRep().getCity());
        assertEquals("123 Health St", fhirOrg.getAddressFirstRep().getLine().get(0).getValue());
        assertEquals(true, fhirOrg.getActive());
    }

    @Test
    public void transformToOrganization_shouldReturnPopulatedOrganization_whenValidFhirOrganizationProvided()
            throws Exception {
        org.hl7.fhir.r4.model.Organization fhirOrg = new org.hl7.fhir.r4.model.Organization();
        fhirOrg.setId(UUID.randomUUID().toString());
        fhirOrg.setName("National Lab");
        Address address = fhirOrg.addAddress();
        address.setCity("Entebbe");
        address.addLine("Plot 4");

        Organization org = fhirTransformService.transformToOrganization(fhirOrg);
        assertEquals("National Lab", org.getOrganizationName());
        assertEquals("Entebbe", org.getCity());
        assertEquals("Plot 4", org.getStreetAddress());
    }

    @Test
    public void getIdFromLocation_shouldExtractId_whenLocationStringProvided() {
        assertEquals("123", fhirTransformService.getIdFromLocation("Patient/123"));
        assertEquals("456", fhirTransformService.getIdFromLocation("Patient/456/_history/1"));
    }

    @Test
    public void createReferenceFor_shouldBuildReference_whenResourceProvided() {
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("pat-789");
        Reference ref = fhirTransformService.createReferenceFor(patient);
        assertEquals("Patient/pat-789", ref.getReference());
    }

    @Test
    public void createReferenceFor_shouldBuildReference_whenResourceTypeAndIdProvided() {
        Reference ref = fhirTransformService.createReferenceFor(ResourceType.Observation, "obs-001");
        assertEquals("Observation/obs-001", ref.getReference());
    }

    @Test
    public void createIdentifier_shouldBuildIdentifier_whenSystemAndValueProvided() {
        Identifier identifier = fhirTransformService.createIdentifier("http://sys.org", "val-999");
        assertEquals("http://sys.org", identifier.getSystem());
        assertEquals("val-999", identifier.getValue());
    }

    @Test
    public void setTempIdIfMissing_shouldAssignGeneratedId_whenResourceIdIsMissing() {
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        boolean result = fhirTransformService.setTempIdIfMissing(patient, new CountingTempIdGenerator());
        assertTrue(result);
        assertEquals("1", patient.getId());
    }

    @Test
    public void transformProviderToPractitioner_shouldReturnPopulatedPractitioner_whenValidProviderProvided() {
        Provider provider = new Provider();
        provider.setFhirUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655441004"));
        Person person = new Person();
        person.setFirstName("Dr. John");
        person.setLastName("Watson");
        person.setPrimaryPhone("0770000000");
        provider.setPerson(person);
        provider.setNpi("NPI-555");
        provider.setActive(true);

        Practitioner practitioner = fhirTransformService.transformProviderToPractitioner(provider);
        assertEquals("Watson", practitioner.getNameFirstRep().getFamily());
        assertEquals("Dr. John", practitioner.getNameFirstRep().getGivenAsSingleString());
        assertEquals("0770000000", practitioner.getTelecomFirstRep().getValue());
    }

    @Test
    public void transformToProvider_shouldReturnPopulatedProvider_whenValidPractitionerProvided() {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(UUID.randomUUID().toString());
        HumanName name = practitioner.addName();
        name.addGiven("Sherlock").setFamily("Holmes");
        ContactPoint phone = practitioner.addTelecom();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setUse(ContactPoint.ContactPointUse.WORK);
        phone.setValue("0771111111");

        Provider provider = fhirTransformService.transformToProvider(practitioner);
        assertEquals("Sherlock", provider.getPerson().getFirstName());
        assertEquals("Holmes", provider.getPerson().getLastName());
        assertEquals("0771111111", provider.getPerson().getWorkPhone());
    }

    @Test
    public void addHumanNameToPerson_shouldSetFirstAndLastName_whenNameProvided() {
        HumanName name = new HumanName();
        name.addGiven("Harry").setFamily("Potter");
        Person person = new Person();
        fhirTransformService.addHumanNameToPerson(name, person);
        assertEquals("Harry", person.getFirstName());
        assertEquals("Potter", person.getLastName());
    }

    @Test
    public void addTelecomToPerson_shouldSetWorkPhone_whenWorkPhoneContactProvided() {
        ContactPoint phone = new ContactPoint();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setUse(ContactPoint.ContactPointUse.WORK);
        phone.setValue("123456");
        Person person = new Person();
        fhirTransformService.addTelecomToPerson(Collections.singletonList(phone), person);
        assertEquals("123456", person.getWorkPhone());
    }

    @Test
    public void transformResultToObservation_shouldReturnPopulatedObservation_whenValidResultProvided()
            throws Exception {
        Analysis dbAnalysis = analysisService.get("1");
        Result result = new Result();
        result.setFhirUuid(UUID.fromString("68438220-5cef-44c4-9e6f-9f88e6b93270"));
        result.setValue("5.6");
        result.setResultType("A");
        result.setAnalysis(dbAnalysis);

        Observation observation = fhirTransformService.transformResultToObservation(result);
        assertEquals("Observation", observation.getResourceType().name());
        assertEquals("68438220-5cef-44c4-9e6f-9f88e6b93270", observation.getId());
        assertEquals("5.6", observation.getValueStringType().getValue());
    }

    @Test
    public void createResultFromObservation_shouldReturnTestResultItem_whenObservationProvided() {
        Observation observation = new Observation();
        observation.setValue(new StringType("Negative"));
        observation.setStatus(Observation.ObservationStatus.FINAL);

        observation.getSpecimen().setReference("Specimen/68438220-5cef-44c4-9e6f-9f88e6b93270");
        observation.addBasedOn().setReference("ServiceRequest/f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01");
        observation.getSubject().setReference("Patient/550e8400-e29b-41d4-a716-446655440001");

        TestResultItem item = fhirTransformService.createResultFromObservation(observation);
        assertEquals("Negative", item.getResultValue());
    }

    @Test
    public void transformResultToDiagnosticReport_shouldReturnPopulatedDiagnosticReport_whenValidAnalysisProvided()
            throws Exception {
        Analysis analysis = analysisService.get("2");

        DiagnosticReport report = fhirTransformService.transformResultToDiagnosticReport(analysis);
        assertEquals("DiagnosticReport", report.getResourceType().name());
        assertEquals("f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02", report.getId());
        assertEquals(DiagnosticReport.DiagnosticReportStatus.FINAL, report.getStatus());
    }

    @Test
    public void transformToServiceRequest_shouldReturnServiceRequest_whenValidIdProvided() {
        ServiceRequest request = fhirTransformService.transformToServiceRequest("1");
        assertEquals("ServiceRequest", request.getResourceType().name());
    }

    @Test
    public void transformToSpecimen_shouldReturnPopulatedSpecimen_whenSampleItemProvided() {
        SampleItem sampleItem = sampleItemService.get("601");
        Specimen specimen = fhirTransformService.transformToSpecimen(sampleItem);
        assertEquals("Specimen", specimen.getResourceType().name());
        assertEquals("68438220-5cef-44c4-9e6f-9f88e6b93270", specimen.getId());
    }

    @Test
    public void transformToSpecimen_shouldReturnSpecimen_whenSampleItemIdProvided() {
        Specimen specimen = fhirTransformService.transformToSpecimen("601");
        assertEquals("Specimen", specimen.getResourceType().name());
    }

    @Test
    public void createSampleItemFromSpecimen_shouldReturnSampleItemWithFhirUuid_whenValidSpecimenProvided() {
        Specimen specimen = new Specimen();
        specimen.setId("68438220-5cef-44c4-9e6f-9f88e6b93270");
        specimen.setReceivedTimeElement(new DateTimeType("2026-07-28T12:00:00Z"));

        SampleItem item = fhirTransformService.createSampleItemFromSpecimen(specimen, "1");
        assertEquals("68438220-5cef-44c4-9e6f-9f88e6b93270", item.getFhirUuidAsString());
    }

    @Test
    public void buildSampleOrderItemFromServiceRequest_shouldSetRequesterSampleId_whenServiceRequestHasSpecimen()
            throws Exception {
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID().toString());
        request.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        Reference subject = new Reference("Patient/550e8400-e29b-41d4-a716-446655440001");
        request.setSubject(subject);
        Reference specimenRef = new Reference("Specimen/68438220-5cef-44c4-9e6f-9f88e6b93270");
        request.addSpecimen(specimenRef);

        SampleOrderItem orderItem = fhirTransformService.buildSampleOrderItemFromServiceRequest(request, "1");
        assertEquals("68438220-5cef-44c4-9e6f-9f88e6b93270", orderItem.getRequesterSampleID());
    }

    @Test
    public void resolveTestsFromServiceRequest_shouldReturnEmptyList_whenServiceRequestHasNoCoding() {
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID().toString());

        List<org.openelisglobal.test.valueholder.Test> tests = fhirTransformService
                .resolveTestsFromServiceRequest(request);
        assertEquals(0, tests.size());
    }

    @Test
    public void transformPersistPatient_shouldPersistFhirResources_whenPatientInfoProvided() throws Exception {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("1");

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        fhirTransformService.transformPersistPatient(patientInfo, true);
        Mockito.verify(mockFhirPersistanceService, Mockito.timeout(2000).times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());

        FhirOperations captured = opsCaptor.getValue();
        int totalResources = captured.createResources.size() + captured.updateResources.size();
        assertTrue("Expected some resources to be prepared for persistence", totalResources > 0);

        org.hl7.fhir.r4.model.Patient fhirPatient = (org.hl7.fhir.r4.model.Patient) captured.createResources.values()
                .stream().filter(r -> r instanceof org.hl7.fhir.r4.model.Patient).findFirst()
                .orElse((org.hl7.fhir.r4.model.Patient) captured.updateResources.values().stream()
                        .filter(r -> r instanceof org.hl7.fhir.r4.model.Patient).findFirst().orElse(null));

        assertTrue("Expected patient to be prepared for persistence", fhirPatient != null);
        assertEquals("550e8400-e29b-41d4-a716-446655440001", fhirPatient.getId());
        assertEquals("Doe", fhirPatient.getName().get(0).getFamily());
        assertEquals("John", fhirPatient.getName().get(0).getGivenAsSingleString());
    }

    @Test
    public void transformPersistOrganization_shouldPersistFhirResources_whenOrganizationProvided() throws Exception {
        Organization org = organizationService.get("3");

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        fhirTransformService.transformPersistOrganization(org);
        Mockito.verify(mockFhirPersistanceService, Mockito.timeout(2000).times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());

        FhirOperations captured = opsCaptor.getValue();
        assertTrue("Expected some resources to be prepared for persistence",
                captured.createResources.size() + captured.updateResources.size() > 0);

        org.hl7.fhir.r4.model.Organization fhirOrg = (org.hl7.fhir.r4.model.Organization) captured.createResources
                .values().stream().filter(r -> r instanceof org.hl7.fhir.r4.model.Organization).findFirst()
                .orElse((org.hl7.fhir.r4.model.Organization) captured.updateResources.values().stream()
                        .filter(r -> r instanceof org.hl7.fhir.r4.model.Organization).findFirst().orElse(null));

        assertTrue("Expected an Organization resource to be prepared", fhirOrg != null);
        assertEquals("68438220-5cef-44c4-9e6f-9f88e6b93287", fhirOrg.getId());
        assertEquals("Global Health Org", fhirOrg.getName());
        assertEquals("123 Health St", fhirOrg.getAddress().get(0).getLine().get(0).getValue());
    }

    @Test
    public void transformPersistOrderEntryFhirObjects_shouldPersistFhirResources_whenSampleAndPatientInfoProvided()
            throws Exception {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        Sample sample = sampleService.get("1");
        updateData.setSample(sample);
        updateData.setSampleItemsTests(new ArrayList<>());
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("1");

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        fhirTransformService.transformPersistOrderEntryFhirObjects(updateData, patientInfo, false, new ArrayList<>());
        Mockito.verify(mockFhirPersistanceService, Mockito.timeout(2000).times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());

        FhirOperations captured = opsCaptor.getValue();
        assertTrue("Expected resources to be prepared",
                captured.createResources.size() + captured.updateResources.size() > 0);

        org.hl7.fhir.r4.model.Patient fhirPatient = (org.hl7.fhir.r4.model.Patient) captured.createResources.values()
                .stream().filter(r -> r instanceof org.hl7.fhir.r4.model.Patient).findFirst()
                .orElse((org.hl7.fhir.r4.model.Patient) captured.updateResources.values().stream()
                        .filter(r -> r instanceof org.hl7.fhir.r4.model.Patient).findFirst().orElse(null));

        assertTrue("Expected patient in order entry persistence", fhirPatient != null);
        assertEquals("550e8400-e29b-41d4-a716-446655440001", fhirPatient.getId());
        assertEquals("Doe", fhirPatient.getName().get(0).getFamily());
    }

    @Test
    public void transformPersistResultsEntryFhirObjects_shouldPersistFhirResources_whenActionDataSetProvided()
            throws Exception {
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet("DEV01260000000000001");

        Result result = new Result();
        result.setId("4");
        Analysis analysis = new Analysis();
        analysis.setId("2");
        result.setAnalysis(analysis);
        org.openelisglobal.result.action.util.ResultSet rs = new org.openelisglobal.result.action.util.ResultSet(result,
                null, null, null, null, null, false);
        actionDataSet.getModifiedResults().add(rs);

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        fhirTransformService.transformPersistResultsEntryFhirObjects(actionDataSet);
        Mockito.verify(mockFhirPersistanceService, Mockito.timeout(2000).times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());

        FhirOperations captured = opsCaptor.getValue();
        assertTrue("Expected resources to be prepared",
                captured.createResources.size() + captured.updateResources.size() > 0);

        org.hl7.fhir.r4.model.Observation observation = (org.hl7.fhir.r4.model.Observation) captured.createResources
                .values().stream().filter(r -> r instanceof org.hl7.fhir.r4.model.Observation).findFirst()
                .orElse((org.hl7.fhir.r4.model.Observation) captured.updateResources.values().stream()
                        .filter(r -> r instanceof org.hl7.fhir.r4.model.Observation).findFirst().orElse(null));

        assertTrue("Expected Observation in results entry persistence", observation != null);
        assertEquals(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL, observation.getStatus());
    }

    @Test
    public void transformPersistResultValidationFhirObjects_shouldPersistFhirResources_whenUpdateListsProvided()
            throws Exception {
        List<Result> deletableList = new ArrayList<>();
        List<Analysis> analysisUpdateList = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();
        List<AnalysisItem> resultItemList = new ArrayList<>();
        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        ArrayList<Note> noteUpdateList = new ArrayList<>();

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        fhirTransformService.transformPersistResultValidationFhirObjects(deletableList, analysisUpdateList,
                resultUpdateList, resultItemList, sampleUpdateList, noteUpdateList);
        Mockito.verify(mockFhirPersistanceService, Mockito.timeout(2000).times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());
    }

    @Test
    public void transformPersistObjectsUnderSamples_shouldReturnPersistedBundle_whenSampleIdsProvided()
            throws Exception {
        List<String> sampleIds = Collections.singletonList("1");

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        Future<Bundle> future = fhirTransformService.transformPersistObjectsUnderSamples(sampleIds);
        assertEquals(bundle, future.get());
        Mockito.verify(mockFhirPersistanceService, Mockito.times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());
        FhirOperations captured = opsCaptor.getValue();
        int totalResources = captured.createResources.size() + captured.updateResources.size();
        assertTrue("Expected some resources to be prepared for persistence", totalResources > 0);
        boolean hasSpecimen = captured.createResources.values().stream().anyMatch(r -> r instanceof Specimen)
                || captured.updateResources.values().stream().anyMatch(r -> r instanceof Specimen);
        assertTrue("Expected at least one Specimen to be prepared for sample persistence", hasSpecimen);
        boolean hasExpectedSpecimenId = captured.createResources.values().stream().filter(r -> r instanceof Specimen)
                .map(r -> r.getId()).anyMatch("68438220-5cef-44c4-9e6f-9f88e6b93270"::equals)
                || captured.updateResources.values().stream().filter(r -> r instanceof Specimen).map(r -> r.getId())
                        .anyMatch("68438220-5cef-44c4-9e6f-9f88e6b93270"::equals);
        assertTrue("Expected specimen with the fixture ID to be prepared for persistence", hasExpectedSpecimenId);
        boolean hasExpectedPatientId = captured.createResources.values().stream()
                .filter(r -> r instanceof org.hl7.fhir.r4.model.Patient).map(r -> r.getId())
                .anyMatch("550e8400-e29b-41d4-a716-446655440001"::equals)
                || captured.updateResources.values().stream().filter(r -> r instanceof org.hl7.fhir.r4.model.Patient)
                        .map(r -> r.getId()).anyMatch("550e8400-e29b-41d4-a716-446655440001"::equals);
        assertTrue("Expected patient with the fixture ID to be prepared for persistence", hasExpectedPatientId);
    }

    @Test
    public void transformPersistPatients_shouldReturnPersistedBundle_whenPatientIdsProvided() throws Exception {
        List<String> patientIds = Collections.singletonList("1");

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        Future<Bundle> future = fhirTransformService.transformPersistPatients(patientIds);
        assertEquals(bundle, future.get());
        Mockito.verify(mockFhirPersistanceService, Mockito.times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());
        FhirOperations captured = opsCaptor.getValue();
        int totalResources = captured.createResources.size() + captured.updateResources.size();
        assertTrue("Expected some resources to be prepared for persistence", totalResources > 0);
        boolean hasPatient = captured.createResources.values().stream()
                .anyMatch(r -> r instanceof org.hl7.fhir.r4.model.Patient)
                || captured.updateResources.values().stream().anyMatch(r -> r instanceof org.hl7.fhir.r4.model.Patient);
        assertTrue("Expected at least one Patient to be prepared for patient persistence", hasPatient);
        boolean hasExpectedPatientId = captured.createResources.values().stream()
                .filter(r -> r instanceof org.hl7.fhir.r4.model.Patient).map(r -> r.getId())
                .anyMatch("550e8400-e29b-41d4-a716-446655440001"::equals)
                || captured.updateResources.values().stream().filter(r -> r instanceof org.hl7.fhir.r4.model.Patient)
                        .map(r -> r.getId()).anyMatch("550e8400-e29b-41d4-a716-446655440001"::equals);
        assertTrue("Expected patient with the fixture ID to be prepared for persistence", hasExpectedPatientId);
    }

    @Test
    public void transformAnalysisByIds_shouldPersistFhirResources_whenAnalysisIdsProvided() throws Exception {
        List<String> analysisIds = Collections.singletonList("2");

        Bundle bundle = new Bundle();
        when(mockFhirPersistanceService.createUpdateFhirResourcesInFhirStore(any(FhirOperations.class)))
                .thenReturn(bundle);

        ArgumentCaptor<FhirOperations> opsCaptor = ArgumentCaptor.forClass(FhirOperations.class);
        fhirTransformService.transformAnalysisByIds(analysisIds);
        Mockito.verify(mockFhirPersistanceService, Mockito.times(1))
                .createUpdateFhirResourcesInFhirStore(opsCaptor.capture());

        FhirOperations captured = opsCaptor.getValue();
        assertTrue("Expected resources to be prepared",
                captured.createResources.size() + captured.updateResources.size() > 0);

        boolean hasDiagnosticReport = captured.createResources.values().stream()
                .anyMatch(r -> r instanceof org.hl7.fhir.r4.model.DiagnosticReport)
                || captured.updateResources.values().stream()
                        .anyMatch(r -> r instanceof org.hl7.fhir.r4.model.DiagnosticReport);
        assertTrue("Expected DiagnosticReport for analysis", hasDiagnosticReport);
    }

    @Test
    public void buildSampleEditItemsListFromServiceRequest_shouldMarkItemAsAdd_whenServiceRequestHasCoding()
            throws Exception {
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID().toString());
        request.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        Reference subject = new Reference("Patient/550e8400-e29b-41d4-a716-446655440001");
        request.setSubject(subject);
        Reference specimenRef = new Reference("Specimen/68438220-5cef-44c4-9e6f-9f88e6b93270");
        request.addSpecimen(specimenRef);

        org.hl7.fhir.r4.model.CodeableConcept code = new org.hl7.fhir.r4.model.CodeableConcept();
        org.hl7.fhir.r4.model.Coding coding = code.addCoding();
        coding.setSystem("http://loinc.org");
        coding.setCode("123456");
        request.setCode(code);

        List<SampleEditItem> items = fhirTransformService.buildSampleEditItemsListFromServiceRequest(request, "1");
        assertFalse(items.isEmpty());
        assertTrue(items.get(0).isAdd());
    }
}
