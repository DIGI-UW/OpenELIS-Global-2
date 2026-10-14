package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;

public class MicroCaseFinalRequirementsServiceTest {
    private final org.openelisglobal.program.service.ProgramService programs = mock(
            org.openelisglobal.program.service.ProgramService.class);

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
        var service = new MicroCaseFinalRequirementsService(details, cases(), questionnaires, programs,
                "patientOrigin");
        assertEquals(List.of("CLINICAL_FIELD_REQUIRED:patientOrigin", "PROGRAM_REQUIRED"), service.blockers("case"));
        verifyZeroInteractions(questionnaires);
    }

    @Test
    public void casePurposeDoesNotRequireAnOrderDetailRecord() {
        var cases = cases();
        var c = cases.get("case").orElseThrow();
        c.setCulturePurpose("DIAGNOSTIC");
        var service = new MicroCaseFinalRequirementsService(mock(MicroCaseOrderDetailDAO.class), cases,
                mock(QuestionnaireStorageService.class), programs, "culturePurpose,patientOrigin");
        assertEquals(List.of("CLINICAL_FIELD_REQUIRED:patientOrigin", "PROGRAM_REQUIRED"), service.blockers("case"));
    }

    @Test
    public void missingQuestionIncludesProgramAndNestedQuestionText() {
        var details = mock(MicroCaseOrderDetailDAO.class);
        var questionnaires = mock(QuestionnaireStorageService.class);
        var program = new org.openelisglobal.program.valueholder.Program();
        program.setId("1");
        program.setShowOnMicroCase(true);
        program.setProgramName("Tuberculosis");
        when(programs.get("1")).thenReturn(program);
        program.setQuestionnaireUUID(UUID.randomUUID());
        var sample = new org.openelisglobal.program.valueholder.ProgramSample();
        sample.setProgram(program);
        var questionnaire = new Questionnaire();
        questionnaire.addItem().setLinkId("history").setType(Questionnaire.QuestionnaireItemType.GROUP).addItem()
                .setLinkId("exposure").setText("Previous treatment").setType(Questionnaire.QuestionnaireItemType.STRING)
                .setRequired(true);
        when(details.getProgramSamples("case")).thenReturn(List.of(sample));
        when(questionnaires.getQuestionnaire(program.getQuestionnaireUUID())).thenReturn(Optional.of(questionnaire));
        var service = new MicroCaseFinalRequirementsService(details, casesWithProgram(), questionnaires, programs, "");
        assertEquals(List.of("QUESTIONNAIRE_FIELD_REQUIRED:exposure"), service.blockers("case"));
        assertEquals("Tuberculosis: Previous treatment",
                service.labels("case").get("QUESTIONNAIRE_FIELD_REQUIRED:exposure"));
    }

    private org.openelisglobal.microbiology.dao.MicroCaseDAO casesWithProgram() {
        var dao = cases();
        dao.get("case").orElseThrow().setProgramId("1");
        return dao;
    }

    private org.openelisglobal.microbiology.dao.MicroCaseDAO cases() {
        var dao = mock(org.openelisglobal.microbiology.dao.MicroCaseDAO.class);
        when(dao.get("case")).thenReturn(Optional.of(new org.openelisglobal.microbiology.valueholder.MicroCase()));
        return dao;
    }

    @Test
    public void environmentalCaseRequiresProgramButNeverClinicalFieldsOrQuestionnaires() {
        var details = mock(MicroCaseOrderDetailDAO.class);
        var questionnaires = mock(QuestionnaireStorageService.class);
        var cases = cases();
        var c = cases.get("case").orElseThrow();
        c.setSiteId("7");
        var service = new MicroCaseFinalRequirementsService(details, cases, questionnaires, programs,
                "patientOrigin,clinicalHistory");
        assertEquals(List.of("PROGRAM_REQUIRED"), service.blockers("case"));
        c.setProgramId("1");
        assertTrue(service.blockers("case").isEmpty());
        assertTrue(service.labels("case").isEmpty());
        verifyZeroInteractions(details, questionnaires);
    }

}
