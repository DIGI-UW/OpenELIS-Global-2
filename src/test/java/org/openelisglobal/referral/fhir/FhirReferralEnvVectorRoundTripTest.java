package org.openelisglobal.referral.fhir;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
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

// Locks the env/vector null-patient contract: Task built with no `for` stays that way through JSON round-trip.
public class FhirReferralEnvVectorRoundTripTest {

    private FhirReferralServiceImpl service;

    private OrganizationService organizationService;
    private SampleService sampleService;
    private SampleHumanService sampleHumanService;
    private AnalysisService analysisService;
    private FhirTransformService fhirTransformService;
    private FhirPersistanceService fhirPersistanceService;
    private FhirConfig fhirConfig;

    private final IParser jsonParser = FhirContext.forR4().newJsonParser();

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
        when(fhirTransformService.createReferenceFor(any(Patient.class)))
                .thenThrow(new AssertionError("createReferenceFor(Patient) must not be called when patient is null"));

        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        when(fhirTransformService.transformToSpecimen(any(SampleItem.class))).thenAnswer(invocation -> {
            SampleItem item = invocation.getArgument(0);
            Specimen specimen = new Specimen();
            specimen.setId(item.getFhirUuidAsString());
            return specimen;
        });
    }

    @Test
    public void envDomain_taskHasNoForReference_andSurvivesJsonRoundTrip() throws Exception {
        assertNoForSurvivesRoundTrip("E");
    }

    @Test
    public void vectorDomain_taskHasNoForReference_andSurvivesJsonRoundTrip() throws Exception {
        assertNoForSurvivesRoundTrip("V");
    }

    private void assertNoForSurvivesRoundTrip(String domain) throws Exception {
        Task task = dispatchAndExtractTask(domain);
        assertFalse(domain + " Task must have no `for` set on the send side", task.hasFor());

        String json = jsonParser.encodeResourceToString(task);
        assertFalse("serialized " + domain + " Task JSON must not contain a `for` field", json.contains("\"for\""));

        Task roundTripped = (Task) jsonParser.parseResource(json);
        assertFalse(domain + " Task must still have no `for` after JSON round-trip", roundTripped.hasFor());
        assertTrue("Task.owner must survive round-trip", roundTripped.hasOwner());
        assertTrue("Task.focus must survive round-trip", roundTripped.hasFocus());
        assertFalse("Task.basedOn must survive round-trip", roundTripped.getBasedOn().isEmpty());
    }

    // ---------------- helpers ----------------

    private Task dispatchAndExtractTask(String domain) throws Exception {
        Fixture f = new Fixture(domain);
        f.analysisFromService.setSampleItem(newSampleItem("item-uuid-" + domain));
        ServiceRequest fresh = new ServiceRequest();
        fresh.setId("fresh-sr-" + domain);
        when(fhirTransformService.transformToServiceRequest(f.analysisFromService.getId())).thenReturn(fresh);

        service.referAnalysisesToOrganization(f.referral);

        Map<String, Resource> persisted = capturePersistedResources();
        Task task = (Task) persisted.values().stream().filter(r -> r instanceof Task).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no Task in persisted bundle for domain " + domain + "; got " + persisted.keySet()));
        assertNotNull(task);
        return task;
    }

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

    // Mirrors the env/vector dispatch scaffold from
    // FhirReferralServiceImplDispatchBundleTest.Fixture.
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

            when(organizationService.get(orgId)).thenReturn(domainOrg);
            Organization fhirOrg = new Organization();
            fhirOrg.setId("Org/fhir-" + orgId);
            when(fhirPersistanceService.getFhirOrganizationByName(domainOrg.getOrganizationName()))
                    .thenReturn(Optional.of(fhirOrg));
            when(sampleService.get(sample.getId())).thenReturn(sample);
            when(sampleHumanService.getProviderForSample(sample)).thenReturn(null);
            when(sampleHumanService.getPatientForSample(sample)).thenReturn(null);

            analysisFromService = new Analysis();
            analysisFromService.setId(analysisId);
            analysisFromService.setFhirUuid(referralAnalysis.getFhirUuid());
            when(analysisService.get(analysisId)).thenReturn(analysisFromService);

            when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.empty());
            when(fhirTransformService.transformToServiceRequest(anyString())).thenReturn(null);
        }
    }

}
