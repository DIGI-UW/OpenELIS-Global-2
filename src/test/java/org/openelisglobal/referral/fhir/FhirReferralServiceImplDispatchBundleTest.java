package org.openelisglobal.referral.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
import org.hl7.fhir.r4.model.Specimen;
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
import org.openelisglobal.referral.fhir.service.FhirReferralServiceImpl;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers the dispatch-bundle contract added in Phase 1: Specimen is always
 * attached when the analysis has a SampleItem, and ServiceRequest is reused
 * from the FHIR store for clinical paths but freshly built (with the
 * samp_domain category) for env/vector paths where no prior SR exists.
 *
 * <p>
 * Pure-Mockito unit test: the project's {@code AppTestConfig} replaces
 * {@code FhirReferralService} with a Mockito mock for all Spring-context tests,
 * so the real implementation is exercised directly here with stubbed
 * collaborators.
 */
public class FhirReferralServiceImplDispatchBundleTest {

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
        when(fhirTransformService.createReferenceFor(any(Organization.class))).thenReturn(new Reference("Org/1"));
        when(fhirTransformService.createReferenceFor(any(ServiceRequest.class))).thenReturn(new Reference("SR/1"));
        when(fhirTransformService.createReferenceFor(any(Practitioner.class))).thenReturn(new Reference("Pr/1"));
        when(fhirTransformService.createReferenceFor(any(Patient.class))).thenReturn(new Reference("Pa/1"));

        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        // Default: any SampleItem maps to a Specimen identified by its FHIR uuid.
        // Individual tests can override to assert identity on a stub instance.
        when(fhirTransformService.transformToSpecimen(any(SampleItem.class))).thenAnswer(invocation -> {
            SampleItem item = invocation.getArgument(0);
            Specimen specimen = new Specimen();
            specimen.setId(item.getFhirUuidAsString());
            return specimen;
        });
    }

    @Test
    public void dispatch_addsSpecimenToBundle_whenAnalysisHasSampleItem() throws Exception {
        Fixture f = new Fixture("E");
        SampleItem item = newSampleItem("item-uuid-1");
        f.analysisFromService.setSampleItem(item);

        Specimen stubSpecimen = new Specimen();
        stubSpecimen.setId(item.getFhirUuidAsString());
        when(fhirTransformService.transformToSpecimen(item)).thenReturn(stubSpecimen);

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> captured = capturePersistedResources();
        Resource specimenInBundle = captured.get(item.getFhirUuidAsString());
        assertNotNull("bundle must contain a Specimen keyed by SampleItem.fhirUuid", specimenInBundle);
        assertTrue("entry must be a Specimen", specimenInBundle instanceof Specimen);
        assertSame("bundle Specimen must be the one returned by transformToSpecimen", stubSpecimen, specimenInBundle);
        verify(fhirTransformService).transformToSpecimen(item);
    }

    @Test
    public void dispatch_skipsSpecimen_whenAnalysisHasNoSampleItem() throws Exception {
        Fixture f = new Fixture("V");
        // pool-pre-fanout: analysis exists but isn't yet tied to a SampleItem
        f.analysisFromService.setSampleItem(null);

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> captured = capturePersistedResources();
        boolean hasSpecimen = captured.values().stream().anyMatch(r -> r instanceof Specimen);
        assertFalse("no Specimen should be in the bundle when analysis has no SampleItem", hasSpecimen);
        verify(fhirTransformService, never()).transformToSpecimen(any(SampleItem.class));
    }

    @Test
    public void dispatch_clinicalPath_reusesExistingServiceRequest_unchanged() throws Exception {
        Fixture f = new Fixture("H");
        f.analysisFromService.setSampleItem(newSampleItem("item-uuid-clin"));

        ServiceRequest existing = new ServiceRequest();
        existing.setId("existing-sr-id");
        when(fhirPersistanceService.getServiceRequestByAnalysisUuid(f.analysisFromService.getFhirUuidAsString()))
                .thenReturn(Optional.of(existing));
        // clinical path requires a patient
        org.openelisglobal.patient.valueholder.Patient localPatient = new org.openelisglobal.patient.valueholder.Patient();
        UUID patientUuid = UUID.randomUUID();
        localPatient.setFhirUuid(patientUuid);
        when(sampleHumanService.getPatientForSample(f.sample)).thenReturn(localPatient);
        Patient fhirPatient = new Patient();
        fhirPatient.setId("Patient/clin-1");
        when(fhirPersistanceService.getPatientByUuid(patientUuid.toString())).thenReturn(Optional.of(fhirPatient));

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> captured = capturePersistedResources();
        Resource sr = captured.get("existing-sr-id");
        assertNotNull("bundle must contain the existing SR keyed by its id", sr);
        assertSame("clinical path must reuse the SR from the store, not build a fresh one", existing, sr);
        verify(fhirTransformService, never()).transformToServiceRequest(anyString());
    }

    @Test
    public void dispatch_envVectorPath_buildsFreshServiceRequestWithDomainCategory() throws Exception {
        Fixture f = new Fixture("E");
        f.analysisFromService.setSampleItem(newSampleItem("item-uuid-env"));

        ServiceRequest fresh = new ServiceRequest();
        fresh.setId("fresh-sr-id");
        when(fhirPersistanceService.getServiceRequestByAnalysisUuid(f.analysisFromService.getFhirUuidAsString()))
                .thenReturn(Optional.empty());
        when(fhirTransformService.transformToServiceRequest(f.analysisFromService.getId())).thenReturn(fresh);

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> captured = capturePersistedResources();
        Resource sr = captured.get("fresh-sr-id");
        assertNotNull("bundle must contain the freshly-built SR keyed by its id", sr);
        assertSame("env/vector path must use the SR returned by transformToServiceRequest", fresh, sr);
        verify(fhirTransformService).transformToServiceRequest(f.analysisFromService.getId());
    }

    @Test
    public void dispatch_envVectorPath_fallsBackToBareSR_whenTransformReturnsNull() throws Exception {
        Fixture f = new Fixture("V");
        f.analysisFromService.setSampleItem(null); // pool-pre-fanout: no SampleItem either

        when(fhirPersistanceService.getServiceRequestByAnalysisUuid(f.analysisFromService.getFhirUuidAsString()))
                .thenReturn(Optional.empty());
        when(fhirTransformService.transformToServiceRequest(f.analysisFromService.getId())).thenReturn(null);

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> captured = capturePersistedResources();
        String analysisFhirId = f.analysisFromService.getFhirUuidAsString();
        Resource sr = captured.get(analysisFhirId);
        assertNotNull("bundle must contain a bare SR keyed by analysis.fhirUuid", sr);
        assertTrue("entry must be a ServiceRequest", sr instanceof ServiceRequest);
        ServiceRequest bare = (ServiceRequest) sr;
        assertEquals("bare SR's id must equal analysis.fhirUuid", analysisFhirId, bare.getIdElement().getIdPart());
        assertFalse("bare SR must have no category populated", bare.hasCategory());
        assertFalse("bare SR must have no specimen reference", bare.hasSpecimen());
        assertNull("bare SR must have no status populated", bare.getStatus());
        verify(fhirTransformService).transformToServiceRequest(f.analysisFromService.getId());
    }

    // ----- helpers -----

    @SuppressWarnings("unchecked")
    private Map<String, Resource> capturePersistedResources() throws Exception {
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        return captor.getValue();
    }

    private static SampleItem newSampleItem(String fhirUuidSeed) {
        SampleItem item = new SampleItem();
        item.setId("Item-" + fhirUuidSeed);
        item.setFhirUuid(UUID.nameUUIDFromBytes(fhirUuidSeed.getBytes()));
        return item;
    }

    /**
     * Shared scaffold for the standard dispatch path: a referral pointing at an
     * analysis on a sample of the given domain. The analysis returned by
     * {@code analysisService.get(...)} starts with no SampleItem — tests set one
     * (or leave null) before calling the service.
     */
    private class Fixture {
        final Sample sample;
        final Analysis analysisFromService;
        final Referral referral;

        Fixture(String domain) {
            String orgId = "Org-" + domain;
            String sampleId = "Sample-" + domain;
            String analysisId = "Analysis-" + domain;

            sample = new Sample();
            sample.setId(sampleId);
            sample.setAccessionNumber("ACC-" + sampleId);
            sample.setDomain(domain);
            sample.setFhirUuid(UUID.randomUUID());

            SampleItem referralItem = new SampleItem();
            referralItem.setId("Item-referral-" + sampleId);
            referralItem.setSample(sample);

            Analysis referralAnalysis = new Analysis();
            referralAnalysis.setId(analysisId);
            referralAnalysis.setSampleItem(referralItem);
            referralAnalysis.setFhirUuid(UUID.randomUUID());

            org.openelisglobal.organization.valueholder.Organization domainOrg = new org.openelisglobal.organization.valueholder.Organization();
            domainOrg.setId(orgId);
            domainOrg.setOrganizationName("Test Referral Lab " + orgId);

            referral = new Referral();
            referral.setId("R-" + domain);
            referral.setOrganization(domainOrg);
            referral.setAnalysis(referralAnalysis);
            referral.setFhirUuid(UUID.randomUUID());

            // Stubs that mirror the lookups inside referAnalysisesToOrganization.
            when(organizationService.get(orgId)).thenReturn(domainOrg);
            Organization fhirOrg = new Organization();
            fhirOrg.setId("Org/fhir-" + orgId);
            when(fhirPersistanceService.getFhirOrganizationByName(domainOrg.getOrganizationName()))
                    .thenReturn(Optional.of(fhirOrg));
            when(sampleService.get(sample.getId())).thenReturn(sample);
            when(sampleHumanService.getProviderForSample(sample)).thenReturn(null);
            // env/vector default: no patient; clinical tests override below
            when(sampleHumanService.getPatientForSample(sample)).thenReturn(null);

            // The analysis fetched inside the service — distinct instance from
            // the one on the referral so tests can attach a SampleItem here
            // without polluting referral state.
            analysisFromService = new Analysis();
            analysisFromService.setId(analysisId);
            analysisFromService.setFhirUuid(referralAnalysis.getFhirUuid());
            when(analysisService.get(analysisId)).thenReturn(analysisFromService);

            // Default: no SR in the store. Tests can override.
            when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.empty());
            // Default: transform returns null (so the env/vector tests can opt
            // in to a non-null fresh SR per test).
            when(fhirTransformService.transformToServiceRequest(anyString())).thenReturn(null);
        }
    }

    @Test
    public void dispatch_assertsTaskIsAlwaysPersisted() throws Exception {
        // Sanity check that the broader bundle structure (Task) survives the
        // changes — if a future refactor accidentally drops the Task, the
        // other tests above might still pass because they look at SR/Specimen
        // only.
        Fixture f = new Fixture("E");
        f.analysisFromService.setSampleItem(null);

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> captured = capturePersistedResources();
        boolean hasTask = captured.values().stream().anyMatch(r -> r instanceof Task);
        assertTrue("Task must always be in the bundle", hasTask);
    }
}
