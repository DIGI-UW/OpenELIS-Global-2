package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.parser.IParser;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageServiceImpl;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SamplePatientEntryServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryEOrderQuestionnaireAdoptionTest {

    @Mock
    private QuestionnaireStorageService questionnaireStorageService;
    @Mock
    private ProgramService programService;
    @Mock
    private FhirUtil fhirUtil;

    @InjectMocks
    private SamplePatientEntryServiceImpl samplePatientService;

    private SampleOrderItem orderItem;

    @Before
    public void setUp() {
        orderItem = new SampleOrderItem();
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_adoptsImportedQrAndSetsProgramId() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));
        task.setFor(new Reference("Patient/pat-200"));

        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId("ext-order-1");

        UUID qUuid = UUID.randomUUID();
        QuestionnaireResponse importedQr = new QuestionnaireResponse();
        importedQr.setId("qr-uuid-555");
        importedQr.setQuestionnaire("Questionnaire/" + qUuid);

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-100", "pat-200"))
                .thenReturn(Optional.of(importedQr));

        Program program = new Program();
        program.setId("prog-1");
        program.setProgramName("HIV Program");
        program.setQuestionnaireUUID(qUuid);

        when(programService.getAllMatching("questionnaireUUID", qUuid)).thenReturn(Collections.singletonList(program));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, eOrder);

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-uuid-555", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
        assertEquals("prog-1", orderItem.getProgramId());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_skipsWhenAlreadyPresentWithMeaningfulId() {
        QuestionnaireResponse existingQr = new QuestionnaireResponse();
        existingQr.setId("existing-qr");
        orderItem.setAdditionalQuestions(existingQr);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, new ElectronicOrder());

        assertEquals("existing-qr", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_replacesPlaceholderWithoutMeaningfulId() {
        QuestionnaireResponse placeholderQr = new QuestionnaireResponse();
        placeholderQr.setId(""); // Empty placeholder from UI
        orderItem.setAdditionalQuestions(placeholderQr);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));
        task.setFor(new Reference("Patient/pat-200"));

        QuestionnaireResponse importedQr = new QuestionnaireResponse();
        importedQr.setId("qr-imported-777");

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-100", "pat-200"))
                .thenReturn(Optional.of(importedQr));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, new ElectronicOrder());

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-imported-777", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_replacesPlaceholderWithoutId() {
        QuestionnaireResponse placeholderQr = new QuestionnaireResponse();
        orderItem.setAdditionalQuestions(placeholderQr);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));
        task.setFor(new Reference("Patient/pat-200"));

        QuestionnaireResponse importedQr = new QuestionnaireResponse();
        importedQr.setId("qr-imported-888");

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-100", "pat-200"))
                .thenReturn(Optional.of(importedQr));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, new ElectronicOrder());

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-imported-888", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_handlesMissingQrGracefully() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-unknown"));

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-unknown", null))
                .thenReturn(Optional.empty());

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, new ElectronicOrder());

        assertNull(orderItem.getAdditionalQuestions());
        assertNull(orderItem.getProgramId());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_fallsBackToElectronicOrderPatientFhirUuid() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));

        UUID patientUuid = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setFhirUuid(patientUuid);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setPatient(patient);

        QuestionnaireResponse importedQr = new QuestionnaireResponse();
        importedQr.setId("qr-from-eorder-pat");

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-100", patientUuid.toString()))
                .thenReturn(Optional.of(importedQr));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, eOrder);

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-from-eorder-pat", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void hasMeaningfulId_evaluatesCorrectly() {
        assertFalse(SamplePatientEntryServiceImpl.hasMeaningfulId(null));
        assertFalse(SamplePatientEntryServiceImpl.hasMeaningfulId(new QuestionnaireResponse()));

        QuestionnaireResponse emptyIdQr = new QuestionnaireResponse();
        emptyIdQr.setId("");
        assertFalse(SamplePatientEntryServiceImpl.hasMeaningfulId(emptyIdQr));

        QuestionnaireResponse blankIdQr = new QuestionnaireResponse();
        blankIdQr.setId("   ");
        assertFalse(SamplePatientEntryServiceImpl.hasMeaningfulId(blankIdQr));

        QuestionnaireResponse validQr = new QuestionnaireResponse();
        validQr.setId("qr-valid");
        assertTrue(SamplePatientEntryServiceImpl.hasMeaningfulId(validQr));
    }

    @Test
    public void toStorageUuidAndQuestionnaireUuidOf_extractsIdsCorrectly() {
        UUID expected = UUID.randomUUID();
        assertEquals(expected, QuestionnaireStorageServiceImpl.toStorageUuid(expected.toString()));

        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setQuestionnaire("Questionnaire/" + expected);
        assertEquals(expected, QuestionnaireStorageServiceImpl.questionnaireUuidOf(qr));
    }

    @Test
    public void resolveAdoptedQuestionnaireResponseUuid_returnsNullWhenNonEOrder() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getElectronicOrder()).thenReturn(null);

        assertNull(samplePatientService.resolveAdoptedQuestionnaireResponseUuid(updateData));
        assertNull(samplePatientService.resolveAdoptedQuestionnaireResponseUuid(null));
    }

    @Test
    public void resolveAdoptedQuestionnaireResponseUuid_adoptsServerResolvedQrForElectronicOrder() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId("eo-1");
        eOrder.setData("{\"resourceType\":\"Task\"}");
        when(updateData.getElectronicOrder()).thenReturn(eOrder);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-999"));
        task.setFor(new Reference("Patient/pat-111"));

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), anyString())).thenReturn(task);

        UUID expectedUuid = UUID.randomUUID();
        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId(expectedUuid.toString());

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-999", "pat-111"))
                .thenReturn(Optional.of(qr));

        UUID resolved = samplePatientService.resolveAdoptedQuestionnaireResponseUuid(updateData);
        assertEquals(expectedUuid, resolved);
    }

    @Test
    public void resolveAdoptedQuestionnaireResponseUuid_returnsNullWhenEOrderHasNoMatchingQr() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId("eo-2");
        eOrder.setData("{\"resourceType\":\"Task\"}");
        when(updateData.getElectronicOrder()).thenReturn(eOrder);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-none"));

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), anyString())).thenReturn(task);

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest(eq("sr-none"), any()))
                .thenReturn(Optional.empty());

        assertNull(samplePatientService.resolveAdoptedQuestionnaireResponseUuid(updateData));
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_prefersElectronicOrderPatientOverRemoteTaskForReference() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));
        task.setFor(new Reference("Patient/remote-pat-999"));

        UUID patientUuid = UUID.randomUUID();
        Patient localPatient = new Patient();
        localPatient.setFhirUuid(patientUuid);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setPatient(localPatient);

        QuestionnaireResponse importedQr = new QuestionnaireResponse();
        importedQr.setId("qr-local-pat");

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-100", patientUuid.toString()))
                .thenReturn(Optional.of(importedQr));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, eOrder);

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-local-pat", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_matchesProgramWhenResponseIdAlreadyExists() {
        UUID qUuid = UUID.randomUUID();
        QuestionnaireResponse existingQr = new QuestionnaireResponse();
        existingQr.setId("existing-meaningful-id");
        existingQr.setQuestionnaire("Questionnaire/" + qUuid);
        orderItem.setAdditionalQuestions(existingQr);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));

        Program program = new Program();
        program.setId("prog-match-1");
        program.setQuestionnaireUUID(qUuid);

        when(programService.getAllMatching("questionnaireUUID", qUuid)).thenReturn(Collections.singletonList(program));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, new ElectronicOrder());

        // Keeps existing response and does not replace it
        assertEquals("existing-meaningful-id", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
        // Resolves programId even though additionalQuestions already had an ID
        assertEquals("prog-match-1", orderItem.getProgramId());
    }

    @Test
    public void resolveAdoptedQuestionnaireResponseUuid_prefersElectronicOrderPatientOverRemoteTaskForReference() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId("eo-pref");
        eOrder.setData("{\"resourceType\":\"Task\"}");
        UUID localPatientUuid = UUID.randomUUID();
        Patient localPatient = new Patient();
        localPatient.setFhirUuid(localPatientUuid);
        eOrder.setPatient(localPatient);
        when(updateData.getElectronicOrder()).thenReturn(eOrder);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-pref-1"));
        task.setFor(new Reference("Patient/remote-task-pat"));

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), anyString())).thenReturn(task);

        UUID expectedUuid = UUID.randomUUID();
        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId(expectedUuid.toString());

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-pref-1", localPatientUuid.toString()))
                .thenReturn(Optional.of(qr));

        UUID resolved = samplePatientService.resolveAdoptedQuestionnaireResponseUuid(updateData);
        assertEquals(expectedUuid, resolved);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void resolveAdoptedQuestionnaireResponseUuid_throwsLIMSRuntimeExceptionWhenResolutionFailsDueToException() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId("eo-err");
        eOrder.setData("{\"resourceType\":\"Task\"}");
        when(updateData.getElectronicOrder()).thenReturn(eOrder);

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), anyString()))
                .thenThrow(new RuntimeException("Simulated parser or database connectivity failure"));

        samplePatientService.resolveAdoptedQuestionnaireResponseUuid(updateData);
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_retriesTaskForWhenEOrderPatientDoesNotMatch() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-retry-1"));
        task.setFor(new Reference("Patient/remote-task-pat-456"));

        UUID localPatientUuid = UUID.randomUUID();
        Patient localPatient = new Patient();
        localPatient.setFhirUuid(localPatientUuid);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setPatient(localPatient);

        QuestionnaireResponse importedQr = new QuestionnaireResponse();
        importedQr.setId("qr-from-remote-pat");

        // Primary lookup by eOrder patient returns empty
        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-retry-1",
                localPatientUuid.toString())).thenReturn(Optional.empty());
        // Fallback retry with task.for remote patient finds the response
        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-retry-1",
                "remote-task-pat-456")).thenReturn(Optional.of(importedQr));

        samplePatientService.resolveAndPrefillQuestionnaireResponse(orderItem, task, eOrder);

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-from-remote-pat", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void resolveAdoptedQuestionnaireResponseUuid_retriesTaskForWhenEOrderPatientDoesNotMatch() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId("eo-retry");
        eOrder.setData("{\"resourceType\":\"Task\"}");
        UUID localPatientUuid = UUID.randomUUID();
        Patient localPatient = new Patient();
        localPatient.setFhirUuid(localPatientUuid);
        eOrder.setPatient(localPatient);
        when(updateData.getElectronicOrder()).thenReturn(eOrder);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-retry-2"));
        task.setFor(new Reference("Patient/remote-task-pat-789"));

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), anyString())).thenReturn(task);

        UUID expectedUuid = UUID.randomUUID();
        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId(expectedUuid.toString());

        // eOrder patient lookup fails
        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-retry-2",
                localPatientUuid.toString())).thenReturn(Optional.empty());
        // task.for patient lookup succeeds
        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-retry-2",
                "remote-task-pat-789")).thenReturn(Optional.of(qr));

        UUID resolved = samplePatientService.resolveAdoptedQuestionnaireResponseUuid(updateData);
        assertEquals(expectedUuid, resolved);
    }

    @Test
    public void resolveProgramQuestionnaireResponseUuid_reusesExistingMeaningfulIdWithoutPerformingLookup() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        UUID existingId = UUID.randomUUID();
        QuestionnaireResponse existingQr = new QuestionnaireResponse();
        existingQr.setId(existingId.toString());
        when(updateData.getProgramQuestionnaireResponse()).thenReturn(existingQr);

        UUID result = samplePatientService.resolveProgramQuestionnaireResponseUuid(updateData);

        assertEquals(existingId, result);
        // ElectronicOrder should never even be inspected when QR already has a meaningful ID
        org.mockito.Mockito.verify(updateData, org.mockito.Mockito.never()).getElectronicOrder();
    }

    @Test
    public void resolveProgramQuestionnaireResponseUuid_fallsBackToAdoptionWhenIdNotMeaningful() {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        QuestionnaireResponse placeholderQr = new QuestionnaireResponse();
        placeholderQr.setId(""); // Empty placeholder from UI
        when(updateData.getProgramQuestionnaireResponse()).thenReturn(placeholderQr);

        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId("eo-placeholder");
        eOrder.setData("{\"resourceType\":\"Task\"}");
        when(updateData.getElectronicOrder()).thenReturn(eOrder);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-ad-1"));

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), anyString())).thenReturn(task);

        UUID adoptedUuid = UUID.randomUUID();
        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId(adoptedUuid.toString());

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-ad-1", null))
                .thenReturn(Optional.of(qr));

        UUID result = samplePatientService.resolveProgramQuestionnaireResponseUuid(updateData);

        assertEquals(adoptedUuid, result);
    }
}
