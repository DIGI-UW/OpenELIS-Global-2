package org.openelisglobal.microbiology.service;

import ca.uhn.fhir.context.FhirContext;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroCaseQuestionnaireForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroCaseQuestionnaireService {
    private final MicroCaseDAO cases;
    private final MicroCaseOrderDetailDAO details;
    private final MicroCaseAuthorizationService authorization;
    private final ProgramService programs;
    private final ProgramSampleService associations;
    private final SampleService samples;
    private final QuestionnaireStorageService storage;
    private final MicroCaseActivityDAO activities;

    public MicroCaseQuestionnaireService(MicroCaseDAO cases, MicroCaseOrderDetailDAO details,
            MicroCaseAuthorizationService authorization, ProgramService programs, ProgramSampleService associations,
            SampleService samples, QuestionnaireStorageService storage, MicroCaseActivityDAO activities) {
        this.cases = cases;
        this.details = details;
        this.authorization = authorization;
        this.programs = programs;
        this.associations = associations;
        this.samples = samples;
        this.storage = storage;
        this.activities = activities;
    }

    public MicroCaseQuestionnaireForm get(String caseId) {
        var c = cases.get(caseId).orElseThrow();
        var form = new MicroCaseQuestionnaireForm();
        form.programId = c.getProgramId();
        if (form.programId == null || c.getSiteId() != null)
            return form;
        var program = programs.get(form.programId);
        if (program == null || program.getQuestionnaireUUID() == null)
            return form;
        var questionnaire = storage.getQuestionnaire(program.getQuestionnaireUUID())
                .orElseThrow(() -> new IllegalStateException("QUESTIONNAIRE_UNAVAILABLE"));
        var parser = FhirContext.forR4Cached().newJsonParser();
        form.questionnaireJson = parser.encodeResourceToString(questionnaire);
        var association = association(caseId, form.programId);
        var response = association == null || association.getQuestionnaireResponseUuid() == null ? null
                : storage.getQuestionnaireResponse(association.getQuestionnaireResponseUuid()).orElse(null);
        if (response != null)
            form.responseJson = parser.encodeResourceToString(response);
        form.version = fingerprint(form.questionnaireJson + "\n" + Objects.toString(form.responseJson, ""));
        return form;
    }

    @Transactional
    public MicroCaseQuestionnaireForm save(String caseId, MicroCaseQuestionnaireForm input, String actor) {
        if (input == null || input.responseJson == null || input.responseJson.isBlank())
            throw new IllegalArgumentException("QUESTIONNAIRE_RESPONSE_REQUIRED");
        var beforeLock = cases.get(caseId).orElseThrow();
        cases.lockOrder(beforeLock.getSampleId());
        var c = authorization.requireWrite(caseId, actor, Constants.ROLE_RESULTS);
        var current = get(caseId);
        if (current.questionnaireJson == null || !Objects.equals(current.programId, input.programId)
                || !Objects.equals(current.version, input.version))
            throw new IllegalArgumentException("MICROBIOLOGY_QUESTIONNAIRE_CHANGED");
        // Program answers belong to the order; all cases sharing them must be writable.
        for (var sibling : cases.getByOrder(c.getSampleId()))
            if (Objects.equals(sibling.getProgramId(), c.getProgramId()))
                authorization.requireWrite(sibling.getId(), actor, Constants.ROLE_RESULTS);
        var parser = FhirContext.forR4Cached().newJsonParser();
        var questionnaire = parser.parseResource(Questionnaire.class, current.questionnaireJson);
        QuestionnaireResponse submitted;
        try {
            submitted = parser.parseResource(QuestionnaireResponse.class, input.responseJson);
        } catch (ca.uhn.fhir.parser.DataFormatException e) {
            throw new IllegalArgumentException("QUESTIONNAIRE_INVALID_RESPONSE", e);
        }
        validateItems(questionnaire.getItem(), submitted.getItem());
        var association = association(caseId, c.getProgramId());
        if (association == null) {
            association = new ProgramSample();
            association.setSample(samples.get(c.getSampleId()));
            association.setProgram(programs.get(c.getProgramId()));
        }
        if (association.getQuestionnaireResponseUuid() == null)
            association.setQuestionnaireResponseUuid(UUID.randomUUID());
        var response = current.responseJson == null ? new QuestionnaireResponse()
                : parser.parseResource(QuestionnaireResponse.class, current.responseJson);
        response.setId(association.getQuestionnaireResponseUuid().toString());
        response.setQuestionnaire("Questionnaire/" + questionnaire.getIdElement().getIdPart());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setAuthored(new Date());
        // The authenticated local user is recorded in the activity; no Practitioner
        // resource is implied.
        response.setItem(submitted.getItem());
        association.setSysUserId(actor);
        associations.save(association);
        storage.saveQuestionnaireResponse(response);
        var event = new MicroCaseActivity();
        event.setCaseId(caseId);
        event.setActivityType("QUESTIONNAIRE_UPDATED");
        event.setPerformedBy(actor);
        event.setOccurredAt(MicroCaseServiceImpl.now());
        event.setSysUserId(actor);
        activities.insert(event);
        return get(caseId);
    }

    private ProgramSample association(String caseId, String programId) {
        return details.getProgramSamples(caseId).stream().filter(ps -> programId.equals(ps.getProgram().getId()))
                .findFirst().orElse(null);
    }

    static void validateItems(List<Questionnaire.QuestionnaireItemComponent> questions,
            List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items) {
        Set<String> seen = new HashSet<>();
        for (var item : items) {
            var question = questions.stream().filter(q -> q.getLinkId().equals(item.getLinkId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("QUESTIONNAIRE_UNKNOWN_QUESTION"));
            if (!seen.add(item.getLinkId()) || !question.getRepeats() && item.getAnswer().size() > 1)
                throw new IllegalArgumentException("QUESTIONNAIRE_DUPLICATE_ANSWER");
            for (var answer : item.getAnswer()) {
                if (!answer.hasValue())
                    throw new IllegalArgumentException("QUESTIONNAIRE_INVALID_ANSWER_TYPE");
                String type = answer.getValue().fhirType();
                String expected = question.getType().toCode();
                boolean valid = switch (expected) {
                case "text", "url" -> type.equals(expected.equals("text") ? "string" : "uri");
                case "choice", "open-choice" -> type.equals("Coding") || type.equals("string");
                default -> type.equalsIgnoreCase(expected);
                };
                if (!valid)
                    throw new IllegalArgumentException("QUESTIONNAIRE_INVALID_ANSWER_TYPE");
                if (expected.equals("choice") && question.hasAnswerOption() && question.getAnswerOption().stream()
                        .noneMatch(o -> o.getValue().equalsDeep(answer.getValue())))
                    throw new IllegalArgumentException("QUESTIONNAIRE_INVALID_CHOICE");
                validateItems(question.getItem(), answer.getItem());
            }
            validateItems(question.getItem(), item.getItem());
        }
    }

    private String fingerprint(String value) {
        return org.springframework.util.DigestUtils
                .md5DigestAsHex((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
}
