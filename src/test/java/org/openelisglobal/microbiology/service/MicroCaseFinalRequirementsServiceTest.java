package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;

public class MicroCaseFinalRequirementsServiceTest {
    @Test
    public void disabledQuestionAndItsRequiredChildrenDoNotBlockFinalRelease() {
        var group = new Questionnaire.QuestionnaireItemComponent().setLinkId("exposure")
                .setType(Questionnaire.QuestionnaireItemType.GROUP);
        group.addEnableWhen().setQuestion("treated").setOperator(Questionnaire.QuestionnaireItemOperator.EQUAL)
                .setAnswer(new BooleanType(true));
        group.addItem().setLinkId("drug").setType(Questionnaire.QuestionnaireItemType.STRING).setRequired(true);
        var blockers = new ArrayList<String>();
        MicroCaseFinalRequirementsService.required(List.of(group), Map.of("treated", List.of(new BooleanType(false))),
                blockers);
        assertEquals(List.of(), blockers);
        MicroCaseFinalRequirementsService.required(List.of(group), Map.of("treated", List.of(new BooleanType(true))),
                blockers);
        assertEquals(List.of("QUESTIONNAIRE_FIELD_REQUIRED:drug"), blockers);
    }

    @Test
    public void numericConditionsCompareNumbersAndRequiredFalseBooleanCountsAsAnswered() {
        var item = new Questionnaire.QuestionnaireItemComponent().setLinkId("symptom")
                .setType(Questionnaire.QuestionnaireItemType.BOOLEAN).setRequired(true);
        item.addEnableWhen().setQuestion("age").setOperator(Questionnaire.QuestionnaireItemOperator.GREATER_THAN)
                .setAnswer(new IntegerType(9));
        var blockers = new ArrayList<String>();
        MicroCaseFinalRequirementsService.required(List.of(item), Map.of("age", List.of(new IntegerType(10))),
                blockers);
        assertEquals(List.of("QUESTIONNAIRE_FIELD_REQUIRED:symptom"), blockers);
        blockers.clear();
        MicroCaseFinalRequirementsService.required(List.of(item),
                Map.of("age", List.of(new IntegerType(10)), "symptom", List.of(new BooleanType(false))), blockers);
        assertEquals(List.of(), blockers);
    }

    @Test
    public void missingClinicalFieldAndProgramAreBothReported() {
        var details = mock(MicroCaseOrderDetailDAO.class);
        var questionnaires = mock(QuestionnaireStorageService.class);
        when(details.getProgramSamples("case")).thenReturn(List.of());
        var service = new MicroCaseFinalRequirementsService(details, questionnaires, "patientOrigin");
        assertEquals(List.of("CLINICAL_FIELD_REQUIRED:patientOrigin", "PROGRAM_REQUIRED"), service.blockers("case"));
        verifyZeroInteractions(questionnaires);
    }

    @Test
    public void missingQuestionIncludesProgramAndNestedQuestionText() {
        var details = mock(MicroCaseOrderDetailDAO.class);
        var questionnaires = mock(QuestionnaireStorageService.class);
        var program = new org.openelisglobal.program.valueholder.Program();
        program.setProgramName("Tuberculosis");
        program.setQuestionnaireUUID(UUID.randomUUID());
        var sample = new org.openelisglobal.program.valueholder.ProgramSample();
        sample.setProgram(program);
        var questionnaire = new Questionnaire();
        questionnaire.addItem().setLinkId("history").setType(Questionnaire.QuestionnaireItemType.GROUP).addItem()
                .setLinkId("exposure").setText("Previous treatment").setType(Questionnaire.QuestionnaireItemType.STRING)
                .setRequired(true);
        when(details.getProgramSamples("case")).thenReturn(List.of(sample));
        when(questionnaires.getQuestionnaire(program.getQuestionnaireUUID())).thenReturn(Optional.of(questionnaire));
        var service = new MicroCaseFinalRequirementsService(details, questionnaires, "");
        assertEquals(List.of("QUESTIONNAIRE_FIELD_REQUIRED:exposure"), service.blockers("case"));
        assertEquals("Tuberculosis: Previous treatment",
                service.labels("case").get("QUESTIONNAIRE_FIELD_REQUIRED:exposure"));
    }

}
