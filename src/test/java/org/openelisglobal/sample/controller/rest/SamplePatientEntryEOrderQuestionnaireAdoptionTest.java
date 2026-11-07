package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageServiceImpl;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SamplePatientEntryServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryEOrderQuestionnaireAdoptionTest {

    @Mock
    private QuestionnaireStorageService questionnaireStorageService;
    @Mock
    private ProgramService programService;

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
}
