package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageServiceImpl;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryEOrderQuestionnaireAdoptionTest {

    @Mock
    private QuestionnaireStorageService questionnaireStorageService;
    @Mock
    private ProgramService programService;
    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private ProgramSampleService programSampleService;

    @InjectMocks
    private SamplePatientEntryRestController controller;

    private SamplePatientEntryForm form;
    private SampleOrderItem orderItem;

    @Before
    public void setUp() {
        form = new SamplePatientEntryForm();
        orderItem = new SampleOrderItem();
        form.setSampleOrderItems(orderItem);
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

        controller.resolveAndPrefillQuestionnaireResponse(form, task, eOrder);

        assertNotNull(orderItem.getAdditionalQuestions());
        assertEquals("qr-uuid-555", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
        assertEquals("prog-1", orderItem.getProgramId());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_skipsWhenAlreadyPresent() {
        QuestionnaireResponse existingQr = new QuestionnaireResponse();
        existingQr.setId("existing-qr");
        orderItem.setAdditionalQuestions(existingQr);

        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-100"));

        controller.resolveAndPrefillQuestionnaireResponse(form, task, new ElectronicOrder());

        assertEquals("existing-qr", orderItem.getAdditionalQuestions().getIdElement().getIdPart());
    }

    @Test
    public void resolveAndPrefillQuestionnaireResponse_handlesMissingQrGracefully() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/sr-unknown"));

        when(questionnaireStorageService.getQuestionnaireResponseForServiceRequest("sr-unknown", null))
                .thenReturn(Optional.empty());

        controller.resolveAndPrefillQuestionnaireResponse(form, task, new ElectronicOrder());

        assertNull(orderItem.getAdditionalQuestions());
        assertNull(orderItem.getProgramId());
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
