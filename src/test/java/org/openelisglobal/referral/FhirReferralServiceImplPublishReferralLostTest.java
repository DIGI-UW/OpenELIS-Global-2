package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.referral.fhir.service.FhirReferralServiceImpl;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit coverage for {@link FhirReferralServiceImpl#publishReferralLost} — the
 * FHIR cancellation push triggered when a referral is marked lost in transit.
 *
 * <p>
 * Driven directly against the implementation with stubbed collaborators —
 * AppTestConfig swaps the real bean for a mock in Spring-context tests, so the
 * real method is never exercised from a higher-level integration test. Same
 * approach as {@link FhirReferralServiceImplManualEntryCompletionTest}.
 */
public class FhirReferralServiceImplPublishReferralLostTest {

    private static final String REFERRAL_ID = "R-701";
    private static final String ANALYSIS_ID = "Analysis-701";
    private static final String SAMPLE_ID = "Sample-701";
    private static final String ORG_ID = "Org-701";
    private static final UUID REFERRAL_FHIR_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ANALYSIS_FHIR_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String REASON = "Courier confirmed loss in transit";

    private FhirReferralServiceImpl service;

    private SampleHumanService sampleHumanService;
    private FhirTransformService fhirTransformService;
    private FhirPersistanceService fhirPersistanceService;
    private FhirConfig fhirConfig;
    private ReferralService referralService;

    @Before
    public void setUp() {
        service = new FhirReferralServiceImpl();
        sampleHumanService = mock(SampleHumanService.class);
        fhirTransformService = mock(FhirTransformService.class);
        fhirPersistanceService = mock(FhirPersistanceService.class);
        fhirConfig = mock(FhirConfig.class);
        referralService = mock(ReferralService.class);

        ReflectionTestUtils.setField(service, "sampleHumanService", sampleHumanService);
        ReflectionTestUtils.setField(service, "fhirTransformService", fhirTransformService);
        ReflectionTestUtils.setField(service, "fhirPersistanceService", fhirPersistanceService);
        ReflectionTestUtils.setField(service, "fhirConfig", fhirConfig);
        ReflectionTestUtils.setField(service, "referralService", referralService);

        when(fhirConfig.getOeFhirSystem()).thenReturn("https://fhir.example/oe");
        // createReferralTask invokes createReferenceFor on the resources it links —
        // return stable references so the Task builds cleanly.
        when(fhirTransformService.createReferenceFor(any(Organization.class))).thenReturn(new Reference("Org/1"));
        when(fhirTransformService.createReferenceFor(any(ServiceRequest.class))).thenReturn(new Reference("SR/1"));
    }

    @Test
    public void skipsSilently_whenReferralHasNoFhirUuid() throws Exception {
        Referral referral = buildReferral(/* referralFhirUuid */ null, ANALYSIS_FHIR_UUID);

        service.publishReferralLost(referral, REASON, "1");

        verify(fhirPersistanceService, never()).updateFhirResourcesInFhirStore(any());
        verify(fhirPersistanceService, never()).getServiceRequestByAnalysisUuid(anyString());
    }

    @Test
    public void skipsSilently_whenAnalysisHasNoFhirUuid() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, /* analysisFhirUuid */ null);

        service.publishReferralLost(referral, REASON, "1");

        verify(fhirPersistanceService, never()).updateFhirResourcesInFhirStore(any());
        verify(fhirPersistanceService, never()).getServiceRequestByAnalysisUuid(anyString());
    }

    @Test
    public void pushesTaskCancelledAndServiceRequestRevoked_withReasonInNote() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);
        stubServiceRequestLookup(/* present */ true);
        stubFhirOrganizationLookup();
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(null);
        when(sampleHumanService.getProviderForSample(any(Sample.class))).thenReturn(null);
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        service.publishReferralLost(referral, REASON, "42");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        Map<String, Resource> pushed = captor.getValue();

        assertEquals("one Task", 1L, pushed.values().stream().filter(r -> r instanceof Task).count());
        assertEquals("one ServiceRequest", 1L,
                pushed.values().stream().filter(r -> r instanceof ServiceRequest).count());

        Task task = extractOne(pushed, Task.class);
        assertEquals("Task ID must be the referral's stable fhir_uuid", REFERRAL_FHIR_UUID.toString(),
                task.getIdElement().getIdPart());
        assertEquals("Task.status must be cancelled", TaskStatus.CANCELLED, task.getStatus());
        assertNotNull("Task.statusReason must be populated", task.getStatusReason());
        assertEquals("Task.statusReason.text must signal the lost-in-transit cause", "lost in transit",
                task.getStatusReason().getText());
        assertTrue("Task must carry at least one note with the user-supplied reason", task.hasNote());
        assertEquals("Task.note[0].text must be the user-supplied reason verbatim", REASON,
                task.getNoteFirstRep().getText());

        ServiceRequest serviceRequest = extractOne(pushed, ServiceRequest.class);
        assertEquals("ServiceRequest ID must be the analysis fhir_uuid", ANALYSIS_FHIR_UUID.toString(),
                serviceRequest.getIdElement().getIdPart());
        assertEquals("ServiceRequest.status must be revoked so the remote knows the request is withdrawn",
                ServiceRequestStatus.REVOKED, serviceRequest.getStatus());
    }

    @Test
    public void buildsBareServiceRequest_whenStoreHasNoPriorCopy() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);
        // Empty SR lookup — publishReferralLost should fall back to a freshly-built
        // bare SR keyed by analysis.fhir_uuid, NOT call transformToServiceRequest
        // (that path is reserved for forward-flow methods).
        when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.empty());
        stubFhirOrganizationLookup();
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(null);
        when(sampleHumanService.getProviderForSample(any(Sample.class))).thenReturn(null);
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        service.publishReferralLost(referral, REASON, "1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        ServiceRequest pushedSr = extractOne(captor.getValue(), ServiceRequest.class);
        assertEquals("fallback SR must be keyed by the analysis fhir_uuid", ANALYSIS_FHIR_UUID.toString(),
                pushedSr.getIdElement().getIdPart());
        assertEquals("fallback SR must still be flipped to revoked", ServiceRequestStatus.REVOKED,
                pushedSr.getStatus());
    }

    // ----- helpers -----

    private Referral buildReferral(UUID referralFhirUuid, UUID analysisFhirUuid) {
        org.openelisglobal.organization.valueholder.Organization org = new org.openelisglobal.organization.valueholder.Organization();
        org.setId(ORG_ID);
        org.setOrganizationName("Reference Lab " + ORG_ID);

        Sample sample = new Sample();
        sample.setId(SAMPLE_ID);
        sample.setAccessionNumber("ACC-" + SAMPLE_ID);
        sample.setDomain("H");
        sample.setFhirUuid(UUID.randomUUID());

        SampleItem item = new SampleItem();
        item.setId("Item-" + SAMPLE_ID);
        item.setSample(sample);
        item.setFhirUuid(UUID.randomUUID());

        Analysis analysis = new Analysis();
        analysis.setId(ANALYSIS_ID);
        analysis.setSampleItem(item);
        if (analysisFhirUuid != null) {
            analysis.setFhirUuid(analysisFhirUuid);
        }

        Referral referral = new Referral();
        referral.setId(REFERRAL_ID);
        referral.setOrganization(org);
        referral.setAnalysis(analysis);
        if (referralFhirUuid != null) {
            referral.setFhirUuid(referralFhirUuid);
        }
        // publishReferralLost re-fetches by id (REQUIRES_NEW re-attach for lazy
        // safety); echo this hand-built graph back so the unit test still drives the
        // real method body.
        when(referralService.getReferralById(REFERRAL_ID)).thenReturn(referral);
        return referral;
    }

    private void stubServiceRequestLookup(boolean present) {
        if (present) {
            ServiceRequest sr = new ServiceRequest();
            sr.setId(ANALYSIS_FHIR_UUID.toString());
            sr.setStatus(ServiceRequestStatus.ACTIVE);
            when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.of(sr));
        } else {
            when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.empty());
        }
    }

    private void stubFhirOrganizationLookup() {
        Organization fhirOrg = new Organization();
        fhirOrg.setId("Org/" + ORG_ID);
        when(fhirPersistanceService.getFhirOrganizationByName(anyString())).thenReturn(Optional.of(fhirOrg));
    }

    private <T extends Resource> T extractOne(Map<String, Resource> map, Class<T> type) {
        return map.values().stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(
                () -> new AssertionError("expected exactly one " + type.getSimpleName() + " in pushed bundle"));
    }
}
