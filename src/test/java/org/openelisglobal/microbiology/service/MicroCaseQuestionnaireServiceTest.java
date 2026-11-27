package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroCaseQuestionnaireForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.program.service.*;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.sample.service.SampleService;

public class MicroCaseQuestionnaireServiceTest {
    private final MicroCaseDAO cases = mock(MicroCaseDAO.class);
    private final MicroCaseOrderDetailDAO details = mock(MicroCaseOrderDetailDAO.class);
    private final MicroCaseAuthorizationService authorization = mock(MicroCaseAuthorizationService.class);
    private final ProgramService programs = mock(ProgramService.class);
    private final ProgramSampleService associations = mock(ProgramSampleService.class);
    private final SampleService samples = mock(SampleService.class);
    private final QuestionnaireStorageService storage = mock(QuestionnaireStorageService.class);
    private final MicroCaseActivityDAO activities = mock(MicroCaseActivityDAO.class);
    private final MicroCaseQuestionnaireService service = new MicroCaseQuestionnaireService(cases, details,
            authorization, programs, associations, samples, storage, activities);
    private final Questionnaire questionnaire = new Questionnaire();
    private final MicroCase c = new MicroCase();
    private final ProgramSample association = new ProgramSample();
    private QuestionnaireResponse saved;

    @Before
    public void setup() {
        c.setId("case");
        c.setSampleId("order");
        c.setProgramId("program");
        var program = new Program();
        program.setId("program");
        program.setQuestionnaireUUID(UUID.randomUUID());
        questionnaire.setId(program.getQuestionnaireUUID().toString());
        questionnaire.addItem().setLinkId("exposure").setType(Questionnaire.QuestionnaireItemType.BOOLEAN)
                .setRequired(true);
        association.setProgram(program);
        association.setQuestionnaireResponseUuid(UUID.randomUUID());
        when(cases.get("case")).thenReturn(Optional.of(c));
        when(cases.getByOrder("order")).thenReturn(List.of(c));
        when(authorization.requireWrite(eq("case"), eq("actor"), anyString())).thenReturn(c);
        when(programs.get("program")).thenReturn(program);
        when(details.getProgramSamples("case")).thenReturn(List.of(association));
        when(storage.getQuestionnaire(program.getQuestionnaireUUID())).thenReturn(Optional.of(questionnaire));
        when(storage.getQuestionnaireResponse(association.getQuestionnaireResponseUuid()))
                .thenAnswer(i -> Optional.ofNullable(saved));
        when(storage.saveQuestionnaireResponse(any())).thenAnswer(i -> {
            saved = i.getArgument(0);
            return saved;
        });
    }

    private MicroCaseQuestionnaireForm input() {
        var form = service.get("case");
        form.responseJson = "{\"resourceType\":\"QuestionnaireResponse\",\"status\":\"completed\",\"item\":[{\"linkId\":\"exposure\",\"answer\":[{\"valueBoolean\":false}]}]}";
        return form;
    }

    @Test
    public void updatesExistingAssociationAndClearsRequiredQuestionWithoutTreatingFalseAsMissing() {
        var responseId = association.getQuestionnaireResponseUuid();
        var before = input();
        var result = service.save("case", before, "actor");
        assertEquals(responseId.toString(), saved.getIdElement().getIdPart());
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED, saved.getStatus());
        assertFalse(saved.getItemFirstRep().getAnswerFirstRep().getValueBooleanType().booleanValue());
        assertNotEquals(before.version, result.version);
        verify(associations).save(association);
        verify(cases).lockOrder("order");
        var answers = Map.<String, List<Type>>of("exposure", List.of(new BooleanType(false)));
        var blockers = new ArrayList<String>();
        MicroCaseFinalRequirementsService.required(questionnaire.getItem(), answers, blockers);
        assertTrue(blockers.isEmpty());
        verify(activities).insert(any());
    }

    @Test
    public void editingCompleteAnswersDoesNotDowngradeCompletedResponse() {
        saved = new QuestionnaireResponse().setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        service.save("case", input(), "actor");
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED, saved.getStatus());
    }

    @Test
    public void incompleteAnswersRemainInProgressRegardlessOfSubmittedStatus() {
        var form = input();
        form.responseJson = "{\"resourceType\":\"QuestionnaireResponse\",\"status\":\"completed\",\"item\":[]}";
        service.save("case", form, "actor");
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS, saved.getStatus());
        service.save("case", input(), "actor");
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED, saved.getStatus());
    }

    @Test
    public void disabledRequiredQuestionDoesNotKeepResponseInProgress() {
        questionnaire.addItem().setLinkId("exposureDetails").setType(Questionnaire.QuestionnaireItemType.STRING)
                .setRequired(true).addEnableWhen().setQuestion("exposure")
                .setOperator(Questionnaire.QuestionnaireItemOperator.EQUAL).setAnswer(new BooleanType(true));
        service.save("case", input(), "actor");
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED, saved.getStatus());
    }

    @Test
    public void rejectsStaleFormAfterAnotherSave() {
        var stale = input();
        service.save("case", stale, "actor");
        try {
            service.save("case", stale, "actor");
            fail("stale save accepted");
        } catch (IllegalArgumentException e) {
            assertEquals("MICROBIOLOGY_QUESTIONNAIRE_CHANGED", e.getMessage());
        }
        verify(storage, times(1)).saveQuestionnaireResponse(any());
    }

    @Test
    public void rejectsFormWhenQuestionnaireChanges() {
        var stale = input();
        questionnaire.addItem().setLinkId("new").setType(Questionnaire.QuestionnaireItemType.STRING);
        try {
            service.save("case", stale, "actor");
            fail("stale questionnaire accepted");
        } catch (IllegalArgumentException e) {
            assertEquals("MICROBIOLOGY_QUESTIONNAIRE_CHANGED", e.getMessage());
        }
        verify(storage, never()).saveQuestionnaireResponse(any());
    }

    @Test
    public void recordsSharedQuestionnaireChangesOnEveryAffectedCaseOnly() {
        var sibling = new MicroCase();
        sibling.setId("sibling");
        sibling.setProgramId("program");
        var other = new MicroCase();
        other.setId("other");
        other.setProgramId("different-program");
        when(cases.getByOrder("order")).thenReturn(List.of(c, sibling, other));
        service.save("case", input(), "actor");
        var events = org.mockito.ArgumentCaptor
                .forClass(org.openelisglobal.microbiology.valueholder.MicroCaseActivity.class);
        verify(activities, times(2)).insert(events.capture());
        assertEquals(Set.of("case", "sibling"),
                events.getAllValues().stream().map(e -> e.getCaseId()).collect(java.util.stream.Collectors.toSet()));
        events.getAllValues().forEach(e -> {
            assertEquals("QUESTIONNAIRE_UPDATED", e.getActivityType());
            assertEquals("actor", e.getPerformedBy());
            assertNotNull(e.getOccurredAt());
        });
        verify(authorization).requireWrite(eq("sibling"), eq("actor"), anyString());
        verify(authorization, never()).requireWrite(eq("other"), anyString(), anyString());
        verify(storage).saveQuestionnaireResponse(any());
    }

    @Test
    public void doesNotChangeOrderAnswersWhenSiblingCaseIsReadOnly() {
        var sibling = new MicroCase();
        sibling.setId("sibling");
        sibling.setProgramId("program");
        when(cases.getByOrder("order")).thenReturn(List.of(c, sibling));
        when(authorization.requireWrite(eq("sibling"), eq("actor"), anyString()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("forbidden"));
        try {
            service.save("case", input(), "actor");
            fail("shared answers changed without rights");
        } catch (org.springframework.security.access.AccessDeniedException expected) {
        }
        verify(storage, never()).saveQuestionnaireResponse(any());
    }

    @Test
    public void malformedResponseIsRejectedBeforePersistence() {
        var form = input();
        form.responseJson = "invalid json";
        try {
            service.save("case", form, "actor");
            fail("malformed response accepted");
        } catch (IllegalArgumentException e) {
            assertEquals("QUESTIONNAIRE_INVALID_RESPONSE", e.getMessage());
        }
        verify(storage, never()).saveQuestionnaireResponse(any());
    }

    @Test
    public void rejectsUnknownQuestionsWrongTypesAndDuplicateAnswers() {
        for (String items : List.of("[{\"linkId\":\"unknown\"}]",
                "[{\"linkId\":\"exposure\",\"answer\":[{\"valueString\":\"false\"}]}]",
                "[{\"linkId\":\"exposure\",\"answer\":[{\"valueBoolean\":true},{\"valueBoolean\":false}]}]")) {
            var form = input();
            form.responseJson = "{\"resourceType\":\"QuestionnaireResponse\",\"item\":" + items + "}";
            try {
                service.save("case", form, "actor");
                fail("invalid answers accepted");
            } catch (IllegalArgumentException expected) {
            }
        }
        verify(storage, never()).saveQuestionnaireResponse(any());
    }
}
