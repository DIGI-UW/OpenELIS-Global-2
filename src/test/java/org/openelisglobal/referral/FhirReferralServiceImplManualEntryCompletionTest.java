package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
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
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.referral.fhir.service.FhirReferralServiceImpl;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit coverage for
 * {@link FhirReferralServiceImpl#publishManualEntryCompletion} (OGC-799 Manual
 * Entry FHIR sync).
 *
 * <p>
 * Driven directly against the implementation with stubbed collaborators —
 * AppTestConfig swaps the real bean for a mock in Spring-context tests, so the
 * real method is never exercised from a higher-level integration test. Same
 * approach as {@link FhirReferralServiceImplPatientGuardTest}.
 */
public class FhirReferralServiceImplManualEntryCompletionTest {

    private static final String REFERRAL_ID = "R-901";
    private static final String ANALYSIS_ID = "Analysis-901";
    private static final String SAMPLE_ID = "Sample-901";
    private static final String ORG_ID = "Org-901";
    private static final UUID REFERRAL_FHIR_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANALYSIS_FHIR_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESULT_ONE_FHIR_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RESULT_TWO_FHIR_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private FhirReferralServiceImpl service;

    private OrganizationService organizationService;
    private SampleHumanService sampleHumanService;
    private FhirTransformService fhirTransformService;
    private FhirPersistanceService fhirPersistanceService;
    private ResultService resultService;
    private FhirConfig fhirConfig;

    @Before
    public void setUp() {
        service = new FhirReferralServiceImpl();
        organizationService = mock(OrganizationService.class);
        sampleHumanService = mock(SampleHumanService.class);
        fhirTransformService = mock(FhirTransformService.class);
        fhirPersistanceService = mock(FhirPersistanceService.class);
        resultService = mock(ResultService.class);
        fhirConfig = mock(FhirConfig.class);

        ReflectionTestUtils.setField(service, "organizationService", organizationService);
        ReflectionTestUtils.setField(service, "sampleHumanService", sampleHumanService);
        ReflectionTestUtils.setField(service, "fhirTransformService", fhirTransformService);
        ReflectionTestUtils.setField(service, "fhirPersistanceService", fhirPersistanceService);
        ReflectionTestUtils.setField(service, "resultService", resultService);
        ReflectionTestUtils.setField(service, "fhirConfig", fhirConfig);

        when(fhirConfig.getOeFhirSystem()).thenReturn("https://fhir.example/oe");
        when(fhirConfig.getRemoteStoreIdentifier()).thenReturn(Collections.<String>emptyList());
        // createReferralTask invokes createReferenceFor on multiple resource types;
        // return stable references so the Task builds cleanly.
        when(fhirTransformService.createReferenceFor(any(Organization.class))).thenReturn(new Reference("Org/1"));
        when(fhirTransformService.createReferenceFor(any(ServiceRequest.class))).thenReturn(new Reference("SR/1"));
        when(fhirTransformService.createReferenceFor(any(Practitioner.class))).thenReturn(new Reference("Pr/1"));
        when(fhirTransformService.createReferenceFor(any(Patient.class))).thenReturn(new Reference("Pa/1"));
        when(fhirTransformService.createReferenceFor(any(DiagnosticReport.class)))
                .thenReturn(new Reference("DiagnosticReport/" + ANALYSIS_FHIR_UUID));
    }

    @Test
    public void skipsSilently_whenReferralHasNoFhirUuid() throws Exception {
        Referral referral = buildReferral(/* fhirUuid */ null, ANALYSIS_FHIR_UUID);

        service.publishManualEntryCompletion(referral, "1");

        verify(fhirPersistanceService, never()).updateFhirResourcesInFhirStore(any());
        verify(fhirTransformService, never()).transformResultToDiagnosticReport(any());
    }

    @Test
    public void skipsSilently_whenAnalysisHasNoFhirUuid() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, /* analysisUuid */ null);

        service.publishManualEntryCompletion(referral, "1");

        verify(fhirPersistanceService, never()).updateFhirResourcesInFhirStore(any());
        verify(fhirTransformService, never()).transformResultToDiagnosticReport(any());
    }

    @Test
    public void pushesBundleWithTaskServiceRequestDiagnosticReportAndOneObservationPerResult() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);
        Analysis analysis = referral.getAnalysis();

        List<Result> results = Arrays.asList(buildResult(RESULT_ONE_FHIR_UUID, analysis),
                buildResult(RESULT_TWO_FHIR_UUID, analysis));
        when(resultService.getResultsByAnalysis(analysis)).thenReturn(results);
        stubDiagnosticReportFor(analysis);
        stubObservationsFor(results);
        stubServiceRequestLookup(/* present */ true);
        stubFhirOrganizationLookup();
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(null);
        when(sampleHumanService.getProviderForSample(any(Sample.class))).thenReturn(null);
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        service.publishManualEntryCompletion(referral, "42");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        Map<String, Resource> pushed = captor.getValue();

        // Exactly one of each: Task, ServiceRequest, DiagnosticReport.
        assertEquals("one Task", 1L, pushed.values().stream().filter(r -> r instanceof Task).count());
        assertEquals("one ServiceRequest", 1L,
                pushed.values().stream().filter(r -> r instanceof ServiceRequest).count());
        assertEquals("one DiagnosticReport", 1L,
                pushed.values().stream().filter(r -> r instanceof DiagnosticReport).count());
        // One Observation per Result.
        assertEquals("one Observation per Result", (long) results.size(),
                pushed.values().stream().filter(r -> r instanceof Observation).count());
        // No Practitioner — provider was null in this test.
        assertEquals("no Practitioner when provider is null", 0L,
                pushed.values().stream().filter(r -> r instanceof Practitioner).count());

        Task task = extractOne(pushed, Task.class);
        assertEquals("Task ID must be the referral's stable fhir_uuid", REFERRAL_FHIR_UUID.toString(),
                task.getIdElement().getIdPart());
        assertEquals("Task.status must be completed for manual-entry sync", TaskStatus.COMPLETED, task.getStatus());
        assertTrue("Task.output must reference the published DiagnosticReport", task.hasOutput());
        assertEquals("Task.output[0] must reference the DR by id", 1, task.getOutput().size());
        Reference outputRef = (Reference) task.getOutput().get(0).getValue();
        assertNotNull("Task.output[0].valueReference must be populated", outputRef);
        assertEquals("DiagnosticReport/" + ANALYSIS_FHIR_UUID, outputRef.getReference());

        ServiceRequest serviceRequest = extractOne(pushed, ServiceRequest.class);
        assertEquals("ServiceRequest ID must be the analysis fhir_uuid", ANALYSIS_FHIR_UUID.toString(),
                serviceRequest.getIdElement().getIdPart());
        assertEquals("ServiceRequest.status must be completed", ServiceRequestStatus.COMPLETED,
                serviceRequest.getStatus());

        DiagnosticReport diagnosticReport = extractOne(pushed, DiagnosticReport.class);
        assertEquals("DR ID must be the analysis fhir_uuid", ANALYSIS_FHIR_UUID.toString(),
                diagnosticReport.getIdElement().getIdPart());
        assertEquals("DR.status mirrors what the transform helper emits", DiagnosticReportStatus.FINAL,
                diagnosticReport.getStatus());
    }

    @Test
    public void wrapsTransformException_asFhirLocalPersistingException() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);

        FhirTransformationException cause = new FhirTransformationException("test could not be coded");
        when(fhirTransformService.transformResultToDiagnosticReport(any(Analysis.class))).thenThrow(cause);

        FhirLocalPersistingException ex = assertThrows(FhirLocalPersistingException.class,
                () -> service.publishManualEntryCompletion(referral, "1"));
        assertSame("wrapper must preserve the originating FhirTransformationException as the cause", cause,
                ex.getCause());
        verify(fhirPersistanceService, never()).updateFhirResourcesInFhirStore(any());
    }

    @Test
    public void propagatesFhirStoreOutage_throughTheCaller() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);
        Analysis analysis = referral.getAnalysis();

        when(resultService.getResultsByAnalysis(analysis)).thenReturn(Collections.emptyList());
        stubDiagnosticReportFor(analysis);
        stubServiceRequestLookup(true);
        stubFhirOrganizationLookup();
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(null);
        when(sampleHumanService.getProviderForSample(any(Sample.class))).thenReturn(null);
        FhirLocalPersistingException stubbed = new FhirLocalPersistingException("FHIR store unreachable");
        doThrow(stubbed).when(fhirPersistanceService).updateFhirResourcesInFhirStore(any());

        FhirLocalPersistingException ex = assertThrows(FhirLocalPersistingException.class,
                () -> service.publishManualEntryCompletion(referral, "1"));
        // Same-instance check guards against silent re-wrapping inside the method.
        assertSame("the persistence-layer exception must propagate unchanged (no re-wrap)", stubbed, ex);
    }

    @Test
    public void buildsServiceRequestFromScratch_whenLookupReturnsEmpty() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);
        Analysis analysis = referral.getAnalysis();

        when(resultService.getResultsByAnalysis(analysis)).thenReturn(Collections.emptyList());
        stubDiagnosticReportFor(analysis);
        stubFhirOrganizationLookup();
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(null);
        when(sampleHumanService.getProviderForSample(any(Sample.class))).thenReturn(null);

        // Empty SR lookup → fall back to transformToServiceRequest (env/vector path).
        when(fhirPersistanceService.getServiceRequestByAnalysisUuid(anyString())).thenReturn(Optional.empty());
        ServiceRequest freshSr = new ServiceRequest();
        freshSr.setId(ANALYSIS_FHIR_UUID.toString());
        when(fhirTransformService.transformToServiceRequest(ANALYSIS_ID)).thenReturn(freshSr);
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        service.publishManualEntryCompletion(referral, "1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        ServiceRequest pushedSr = extractOne(captor.getValue(), ServiceRequest.class);
        assertSame("must reuse the SR built by the transform fallback", freshSr, pushedSr);
        assertEquals("status flipped even for transform-fallback SR", ServiceRequestStatus.COMPLETED,
                pushedSr.getStatus());
    }

    @Test
    public void includesPractitioner_whenSampleHasAProvider() throws Exception {
        Referral referral = buildReferral(REFERRAL_FHIR_UUID, ANALYSIS_FHIR_UUID);
        Analysis analysis = referral.getAnalysis();

        when(resultService.getResultsByAnalysis(analysis)).thenReturn(Collections.emptyList());
        stubDiagnosticReportFor(analysis);
        stubServiceRequestLookup(true);
        stubFhirOrganizationLookup();
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(null);

        org.openelisglobal.provider.valueholder.Provider provider = new org.openelisglobal.provider.valueholder.Provider();
        provider.setId("Provider-7");
        when(sampleHumanService.getProviderForSample(any(Sample.class))).thenReturn(provider);
        Practitioner practitioner = new Practitioner();
        when(fhirTransformService.transformProviderToPractitioner(provider)).thenReturn(practitioner);
        when(fhirPersistanceService.updateFhirResourcesInFhirStore(any())).thenReturn(new Bundle());

        service.publishManualEntryCompletion(referral, "1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Resource>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fhirPersistanceService).updateFhirResourcesInFhirStore(captor.capture());
        Practitioner pushed = extractOne(captor.getValue(), Practitioner.class);
        assertSame("must push the exact Practitioner the transform service returned", practitioner, pushed);
        // The method assigns a fresh UUID id on the Practitioner before bundling —
        // guard
        // against a bug that drops the id (HAPI requires every resource to be
        // addressable).
        assertNotNull("Practitioner must be assigned an id before persistence", pushed.getIdElement().getIdPart());
        assertTrue("Practitioner id must be non-empty", pushed.getIdElement().getIdPart().length() > 0);
        verify(fhirTransformService).transformProviderToPractitioner(provider);
    }

    // ----- helpers -----

    /** Builds an in-memory Referral wired with the analysis/sample/org chain. */
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
        return referral;
    }

    private Result buildResult(UUID resultFhirUuid, Analysis analysis) {
        Result result = new Result();
        result.setId("Result-" + resultFhirUuid);
        result.setFhirUuid(resultFhirUuid);
        result.setAnalysis(analysis);
        return result;
    }

    private void stubDiagnosticReportFor(Analysis analysis) throws FhirTransformationException {
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(analysis.getFhirUuidAsString());
        diagnosticReport.setStatus(DiagnosticReportStatus.FINAL);
        when(fhirTransformService.transformResultToDiagnosticReport(analysis)).thenReturn(diagnosticReport);
    }

    private void stubObservationsFor(List<Result> results) throws FhirTransformationException {
        for (Result result : results) {
            Observation obs = new Observation();
            obs.setId(result.getFhirUuidAsString());
            when(fhirTransformService.transformResultToObservation(result)).thenReturn(obs);
        }
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
