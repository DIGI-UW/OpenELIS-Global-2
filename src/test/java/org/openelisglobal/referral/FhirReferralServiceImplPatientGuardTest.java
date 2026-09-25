package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.referral.fhir.service.FhirReferralServiceImpl;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Guards around the OGC-356 null-patient handling in
 * {@link FhirReferralServiceImpl}.
 *
 * <p>
 * Environmental ("E") and vector ("V") samples have no linked patient — a
 * referral on them used to NPE inside the {@code @Transactional} call to
 * {@code referAnalysisesToOrganization}, rolling back the parent save (the
 * referral, its subcontract row, and the status_history seed). The fix
 * domain-gates the null-patient path: env/vector flows through; clinical with a
 * missing patient still fails loudly so a broken sample_human link isn't
 * silently masked.
 *
 * <p>
 * This is a focused unit test rather than an integration test because the
 * project's {@code AppTestConfig} replaces {@code FhirReferralService} with a
 * Mockito mock for all Spring-context tests; the real implementation is never
 * exercised from a higher-level service. So we drive the implementation
 * directly with stubbed collaborators.
 */
public class FhirReferralServiceImplPatientGuardTest {

    private FhirReferralServiceImpl service;

    private OrganizationService organizationService;
    private SampleService sampleService;
    private SampleHumanService sampleHumanService;
    private AnalysisService analysisService;
    private FhirTransformService fhirTransformService;
    private FhirPersistanceService fhirPersistanceService;
    private FhirConfig fhirConfig;

    @Before
    public void setUp() throws Exception {
        service = new FhirReferralServiceImpl();
        organizationService = mock(OrganizationService.class);
        sampleService = mock(SampleService.class);
        sampleHumanService = mock(SampleHumanService.class);
        analysisService = mock(AnalysisService.class);
        fhirTransformService = mock(FhirTransformService.class);
        fhirPersistanceService = mock(FhirPersistanceService.class);
        fhirConfig = mock(FhirConfig.class);

        ReflectionTestUtils.setField(service, "organizationService", organizationService);
        ReflectionTestUtils.setField(service, "sampleService", sampleService);
        ReflectionTestUtils.setField(service, "sampleHumanService", sampleHumanService);
        ReflectionTestUtils.setField(service, "analysisService", analysisService);
        ReflectionTestUtils.setField(service, "fhirTransformService", fhirTransformService);
        ReflectionTestUtils.setField(service, "fhirPersistanceService", fhirPersistanceService);
        ReflectionTestUtils.setField(service, "fhirConfig", fhirConfig);

        when(fhirConfig.getOeFhirSystem()).thenReturn("https://fhir.example/oe");
        when(fhirConfig.getRemoteStoreIdentifier()).thenReturn(Collections.<String>emptyList());
        // createReferenceFor is called against several resource types; return a
        // stable Reference for whichever overload is invoked.
        when(fhirTransformService.createReferenceFor(any(Organization.class))).thenReturn(new Reference("Org/1"));
        when(fhirTransformService.createReferenceFor(any(ServiceRequest.class))).thenReturn(new Reference("SR/1"));
        when(fhirTransformService.createReferenceFor(any(Practitioner.class))).thenReturn(new Reference("Pr/1"));
        when(fhirTransformService.createReferenceFor(any(Patient.class))).thenReturn(new Reference("Pa/1"));
    }

    @Test
    public void createReferralTask_omitsForReference_whenPatientIsNull() {
        Organization referralOrg = new Organization();
        ServiceRequest sr = new ServiceRequest();
        Practitioner requester = new Practitioner();
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-ENV-001");

        Task task = service.createReferralTask(referralOrg, null, sr, Optional.of(requester), sample);

        assertNotNull(task);
        assertFalse("task.for must be unset when patient is null (env/vector samples)", task.hasFor());
        // The rest of the Task should still be populated so the referral is a
        // complete FHIR record.
        assertTrue("task.owner must be set to the referral organization", task.hasOwner());
        assertTrue("task.basedOn must reference the service request", task.hasBasedOn());
        assertTrue("task.focus must reference the service request", task.hasFocus());
        verify(fhirTransformService, never()).createReferenceFor(any(Patient.class));
    }

    @Test
    public void createReferralTask_setsForReference_whenPatientPresent() {
        Organization referralOrg = new Organization();
        ServiceRequest sr = new ServiceRequest();
        Practitioner requester = new Practitioner();
        Patient patient = new Patient();
        patient.setId("Patient/clin-1");
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-CLIN-001");

        Task task = service.createReferralTask(referralOrg, patient, sr, Optional.of(requester), sample);

        assertNotNull(task);
        assertTrue("task.for must be set when a patient is provided", task.hasFor());
        assertEquals("Pa/1", task.getFor().getReference());
        verify(fhirTransformService).createReferenceFor(patient);
    }

    @Test
    public void referAnalysisesToOrganization_envSampleWithNullPatient_persistsTaskWithoutPatientRef()
            throws Exception {
        Referral referral = stubReferralWithIds("R1", "Org-1", "Sample-1", "Analysis-1", "fhir-referral-uuid");
        Sample envSample = stubSample("Sample-1", "E");
        stubCollaboratorsFor(envSample, "Org-1", "Analysis-1", /* provider */ null);
        // Critical setup: env sample has no patient row.
        when(sampleHumanService.getPatientForSample(envSample)).thenReturn(null);

        Bundle resultBundle = new Bundle();
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(resultBundle);

        Bundle bundle = service.referAnalysisesToOrganization(referral);

        assertNotNull("env-sample referral must complete without exception", bundle);
        // The actual Task pushed to FHIR must have no `for` reference. Capture
        // the map to inspect the Task that the service built.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        Task persistedTask = (Task) captor.getValue().values().stream().filter(r -> r instanceof Task).findFirst()
                .orElseThrow();
        assertFalse("Task.for must be unset for env sample", persistedTask.hasFor());
        // sample_human lookup attempted exactly once
        verify(sampleHumanService).getPatientForSample(envSample);
        // FHIR patient lookup must NOT be attempted when local patient is null
        verify(fhirPersistanceService, never()).getPatientByUuid(anyString());
    }

    @Test
    public void referAnalysisesToOrganization_vectorSampleWithNullPatient_persistsTaskWithoutPatientRef()
            throws Exception {
        Referral referral = stubReferralWithIds("R2", "Org-2", "Sample-V1", "Analysis-V1", "fhir-vec-uuid");
        Sample vecSample = stubSample("Sample-V1", "V");
        stubCollaboratorsFor(vecSample, "Org-2", "Analysis-V1", /* provider */ null);
        when(sampleHumanService.getPatientForSample(vecSample)).thenReturn(null);
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        Bundle bundle = service.referAnalysisesToOrganization(referral);

        assertNotNull(bundle);
        verify(fhirPersistanceService, never()).getPatientByUuid(anyString());
    }

    @Test
    public void referAnalysisesToOrganization_clinicalSampleWithNullPatient_failsLoudly() throws Exception {
        Referral referral = stubReferralWithIds("R3", "Org-3", "Sample-H1", "Analysis-H1", "fhir-clin-uuid");
        Sample clinicalSample = stubSample("Sample-H1", "H");
        stubCollaboratorsFor(clinicalSample, "Org-3", "Analysis-H1", /* provider */ null);
        // Missing sample_human link on a clinical sample is a real bug; the
        // service should refuse rather than silently mask it.
        when(sampleHumanService.getPatientForSample(clinicalSample)).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.referAnalysisesToOrganization(referral));
        assertTrue("error message should identify the offending sample id", ex.getMessage().contains("Sample-H1"));
        assertTrue("error message should explain the cause",
                ex.getMessage().toLowerCase().contains("no linked patient"));
        // Nothing was persisted — the parent transaction would roll back as expected.
        verify(fhirPersistanceService, never()).updateFhirResourcesInFhirStore(any());
    }

    @Test
    public void referAnalysisesToOrganization_clinicalSampleWithPatient_resolvesFhirPatientByUuid() throws Exception {
        Referral referral = stubReferralWithIds("R4", "Org-4", "Sample-H2", "Analysis-H2", "fhir-clin2-uuid");
        Sample clinicalSample = stubSample("Sample-H2", "H");
        stubCollaboratorsFor(clinicalSample, "Org-4", "Analysis-H2", /* provider */ null);

        org.openelisglobal.patient.valueholder.Patient localPatient = new org.openelisglobal.patient.valueholder.Patient();
        UUID patientUuid = UUID.randomUUID();
        localPatient.setFhirUuid(patientUuid);
        when(sampleHumanService.getPatientForSample(clinicalSample)).thenReturn(localPatient);

        Patient fhirPatient = new Patient();
        fhirPatient.setId("Patient/clin");
        when(fhirPersistanceService.getPatientByUuid(patientUuid.toString())).thenReturn(Optional.of(fhirPatient));
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        service.referAnalysisesToOrganization(referral);

        verify(fhirPersistanceService).getPatientByUuid(patientUuid.toString());
        verify(fhirTransformService).createReferenceFor(fhirPatient);
    }

    // ----- helpers -----

    private Referral stubReferralWithIds(String referralId, String orgId, String sampleId, String analysisId,
            String fhirUuid) {
        org.openelisglobal.organization.valueholder.Organization domainOrg = new org.openelisglobal.organization.valueholder.Organization();
        domainOrg.setId(orgId);
        domainOrg.setOrganizationName("Test Referral Lab " + orgId);

        Sample sample = stubSample(sampleId, /* placeholder, overridden by stubSample call later */ "H");

        SampleItem item = new SampleItem();
        item.setId("Item-" + sampleId);
        item.setSample(sample);

        Analysis analysis = new Analysis();
        analysis.setId(analysisId);
        analysis.setSampleItem(item);
        analysis.setFhirUuid(UUID.randomUUID());

        Referral referral = new Referral();
        referral.setId(referralId);
        referral.setOrganization(domainOrg);
        referral.setAnalysis(analysis);
        referral.setFhirUuid(UUID.fromString(stableUuid(fhirUuid)));

        return referral;
    }

    private Sample stubSample(String sampleId, String domain) {
        Sample s = new Sample();
        s.setId(sampleId);
        s.setAccessionNumber("ACC-" + sampleId);
        s.setDomain(domain);
        s.setFhirUuid(UUID.randomUUID());
        return s;
    }

    /**
     * Wire up the chain of lookups that {@code referAnalysisesToOrganization} does
     * before it reaches the patient guard.
     */
    private void stubCollaboratorsFor(Sample sample, String orgId, String analysisId, Provider provider) {
        org.openelisglobal.organization.valueholder.Organization domainOrg = new org.openelisglobal.organization.valueholder.Organization();
        domainOrg.setId(orgId);
        domainOrg.setOrganizationName("Test Referral Lab " + orgId);

        when(organizationService.get(orgId)).thenReturn(domainOrg);

        // Make getFhirOrganization() return a non-null FHIR Organization so
        // the method proceeds past the early return.
        Organization fhirOrg = new Organization();
        fhirOrg.setId("Org/fhir-" + orgId);
        when(fhirPersistanceService.getFhirOrganizationByName(domainOrg.getOrganizationName()))
                .thenReturn(Optional.of(fhirOrg));

        when(sampleService.get(sample.getId())).thenReturn(sample);
        when(sampleHumanService.getProviderForSample(sample)).thenReturn(provider);

        Analysis analysis = new Analysis();
        analysis.setId(analysisId);
        analysis.setFhirUuid(UUID.randomUUID());
        when(analysisService.get(analysisId)).thenReturn(analysis);
        when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.empty());
    }

    private static String stableUuid(String seed) {
        // Deterministic UUID derived from the seed so test failures point to
        // a recognizable string.
        return UUID.nameUUIDFromBytes(seed.getBytes()).toString();
    }
}
