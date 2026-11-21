package org.openelisglobal.questionnaire.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;

@RunWith(MockitoJUnitRunner.class)
public class QuestionnaireStorageServiceLookupTest {

    @Mock
    private QuestionnaireService questionnaireService;
    @Mock
    private QuestionnaireResponseService questionnaireResponseService;
    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private FhirPersistanceService fhirPersistanceService;

    @InjectMocks
    private QuestionnaireStorageServiceImpl storageService;

    private final IParser parser = FhirContext.forR4Cached().newJsonParser();

    @Test
    public void getQuestionnaireResponseForServiceRequest_returnsEmptyWhenServiceRequestIdIsBlank() {
        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("", "pat-1");
        assertFalse(result.isPresent());

        result = storageService.getQuestionnaireResponseForServiceRequest(null, null);
        assertFalse(result.isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getQuestionnaireResponseForServiceRequest_fhirStoreConfigured_returnsMatchingQrAndBackfills() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("http://fhir.test/fhir");

        IGenericClient client = mock(IGenericClient.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        IQuery query = mock(IQuery.class);

        when(fhirUtil.getLocalFhirClient()).thenReturn(client);
        when(client.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(QuestionnaireResponse.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);

        Bundle bundle = new Bundle();
        QuestionnaireResponse fhirQr = new QuestionnaireResponse();
        fhirQr.setId("qr-100");
        fhirQr.setStatus(QuestionnaireResponseStatus.COMPLETED);
        fhirQr.addBasedOn(new Reference("ServiceRequest/sr-100"));
        bundle.addEntry().setResource(fhirQr);

        when(query.execute()).thenReturn(bundle);
        when(questionnaireResponseService.getMatch("fhirUuid", QuestionnaireStorageServiceImpl.toStorageUuid("qr-100")))
                .thenReturn(Optional.empty());

        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("sr-100",
                "pat-1");

        assertTrue(result.isPresent());
        assertEquals("qr-100", result.get().getIdElement().getIdPart());
        verify(questionnaireResponseService)
                .insert(any(org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getQuestionnaireResponseForServiceRequest_fhirStoreSearchFails_fallsBackToLocalDatabase() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("http://fhir.test/fhir");

        IGenericClient client = mock(IGenericClient.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        when(fhirUtil.getLocalFhirClient()).thenReturn(client);
        when(client.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(QuestionnaireResponse.class))
                .thenThrow(new RuntimeException("FHIR store connection down"));

        QuestionnaireResponse fhirQr = new QuestionnaireResponse();
        fhirQr.setId("qr-local-1");
        fhirQr.setStatus(QuestionnaireResponseStatus.COMPLETED);
        fhirQr.addBasedOn(new Reference("ServiceRequest/sr-200"));

        org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse entity = new org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse();
        entity.setFhirUuid(UUID.randomUUID());
        entity.setResourceJson(parser.encodeResourceToString(fhirQr));

        when(questionnaireResponseService.getAllMatching("subjectReference", "Patient/pat-200"))
                .thenReturn(Collections.singletonList(entity));

        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("sr-200",
                "pat-200");

        assertTrue(result.isPresent());
        assertEquals("qr-local-1", result.get().getIdElement().getIdPart());
    }

    @Test
    public void getQuestionnaireResponseForServiceRequest_localSearch_matchesExactServiceRequestIdNotSuffix() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(null);

        // QR with ServiceRequest/prefix-sr-123
        QuestionnaireResponse qr2 = new QuestionnaireResponse();
        qr2.setId("qr-2");
        qr2.addBasedOn(new Reference("ServiceRequest/prefix-sr-123"));

        org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse entity2 = new org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse();
        entity2.setFhirUuid(UUID.randomUUID());
        entity2.setResourceJson(parser.encodeResourceToString(qr2));

        when(questionnaireResponseService.getAllMatching("subjectReference", "Patient/pat-1"))
                .thenReturn(Collections.singletonList(entity2));

        // Searching for sr-123 should NOT match prefix-sr-123 even though it ends with sr-123
        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("sr-123",
                "pat-1");
        assertFalse("Suffix match must not erroneously match different ServiceRequest IDs", result.isPresent());
    }

    @Test
    public void getQuestionnaireResponseForServiceRequest_localSearch_returnsEmptyWhenPatientSearchMissesWithoutLoadingAll() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(null);

        when(questionnaireResponseService.getAllMatching("subjectReference", "Patient/pat-999"))
                .thenReturn(Collections.emptyList());
        when(questionnaireResponseService.getAllMatching("subjectReference", null))
                .thenReturn(Collections.emptyList());

        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("sr-500",
                "pat-999");
        assertFalse(result.isPresent());
        verify(questionnaireResponseService, never()).getAll();
    }

    @Test
    public void getQuestionnaireResponseForServiceRequest_patientlessOrder_findsMatchingResponseLocally() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(null);

        QuestionnaireResponse fhirQr = new QuestionnaireResponse();
        fhirQr.setId("qr-patientless-1");
        fhirQr.setStatus(QuestionnaireResponseStatus.COMPLETED);
        fhirQr.addBasedOn(new Reference("ServiceRequest/sr-env-100"));

        org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse entity = new org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse();
        entity.setFhirUuid(UUID.randomUUID());
        entity.setResourceJson(parser.encodeResourceToString(fhirQr));

        when(questionnaireResponseService.getAllMatching("subjectReference", null))
                .thenReturn(Collections.singletonList(entity));

        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("sr-env-100",
                null);

        assertTrue("Patientless local lookup must find matching QuestionnaireResponse by ServiceRequest",
                result.isPresent());
        assertEquals("qr-patientless-1", result.get().getIdElement().getIdPart());
        verify(questionnaireResponseService, never()).getAllMatching(eq("subjectReference"), org.mockito.ArgumentMatchers.startsWith("Patient/"));
    }

    @Test
    public void getQuestionnaireResponseForServiceRequest_patientSearchMisses_fallsBackToPatientlessSubjectLocally() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(null);

        QuestionnaireResponse fhirQr = new QuestionnaireResponse();
        fhirQr.setId("qr-patientless-2");
        fhirQr.setStatus(QuestionnaireResponseStatus.COMPLETED);
        fhirQr.addBasedOn(new Reference("ServiceRequest/sr-env-200"));

        org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse entity = new org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse();
        entity.setFhirUuid(UUID.randomUUID());
        entity.setResourceJson(parser.encodeResourceToString(fhirQr));

        when(questionnaireResponseService.getAllMatching("subjectReference", "Patient/pat-missing"))
                .thenReturn(Collections.emptyList());
        when(questionnaireResponseService.getAllMatching("subjectReference", null))
                .thenReturn(Collections.singletonList(entity));

        Optional<QuestionnaireResponse> result = storageService.getQuestionnaireResponseForServiceRequest("sr-env-200",
                "pat-missing");

        assertTrue("Should fall back to patientless subjectReference when patient lookup misses", result.isPresent());
        assertEquals("qr-patientless-2", result.get().getIdElement().getIdPart());
    }
}
