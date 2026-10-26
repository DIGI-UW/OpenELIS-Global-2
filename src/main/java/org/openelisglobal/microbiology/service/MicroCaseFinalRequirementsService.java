package org.openelisglobal.microbiology.service;

import java.util.*;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroCaseFinalRequirementsService {
    private final MicroCaseOrderDetailDAO details;
    private final org.openelisglobal.microbiology.dao.MicroCaseDAO cases;
    private final QuestionnaireStorageService questionnaires;
    private final org.openelisglobal.program.service.ProgramService programs;
    private final List<String> requiredFields;

    public MicroCaseFinalRequirementsService(MicroCaseOrderDetailDAO details,
            org.openelisglobal.microbiology.dao.MicroCaseDAO cases, QuestionnaireStorageService questionnaires,
            org.openelisglobal.program.service.ProgramService programs,
            @org.springframework.beans.factory.annotation.Value("${org.openelisglobal.microbiology.requiredBeforeFinalFields:patientOrigin}") String fields) {
        this.details = details;
        this.cases = cases;
        this.questionnaires = questionnaires;
        this.programs = programs;
        requiredFields = Arrays.stream(fields.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (!Set.of("clinicalHistory", "patientOrigin", "admissionDate", "antibioticExposure", "culturePurpose",
                "numberOfSets").containsAll(requiredFields))
            throw new IllegalArgumentException("Unknown microbiology required-before-final clinical field");
    }

    public List<String> requiredFields() {
        return requiredFields;
    }

    public List<String> blockers(String caseId) {
        var blockers = new ArrayList<String>();
        var microCase = cases.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        if (microCase.getSiteId() != null) {
            if (microCase.getProgramId() == null)
                blockers.add("PROGRAM_REQUIRED");
            return blockers;
        }
        var detail = details.getByCaseId(caseId);
        for (String field : requiredFields) {
            Object value = switch (field) {
            case "clinicalHistory" -> detail == null ? null : detail.getClinicalHistory();
            case "patientOrigin" -> detail == null ? null : detail.getPatientOrigin();
            case "admissionDate" -> detail == null ? null : detail.getAdmissionDate();
            case "antibioticExposure" -> detail == null ? null : detail.getAntibioticExposure();
            case "culturePurpose" -> microCase.getCulturePurpose();
            case "numberOfSets" -> detail == null ? null : detail.getNumberOfSets();
            default -> null;
            };
            if (value == null || value instanceof String text && text.isBlank())
                blockers.add("CLINICAL_FIELD_REQUIRED:" + field);
        }
        if (microCase.getProgramId() == null) {
            blockers.add("PROGRAM_REQUIRED");
            return blockers;
        }
        var program = programs.get(microCase.getProgramId());
        if (program == null || !program.isShowOnMicroCase()) {
            blockers.add("PROGRAM_REQUIRED");
            return blockers;
        }
        if (program.getQuestionnaireUUID() != null) {
            var q = questionnaires.getQuestionnaire(program.getQuestionnaireUUID()).orElse(null);
            var association = details.getProgramSamples(caseId).stream()
                    .filter(ps -> program.getId().equals(ps.getProgram().getId())).findFirst().orElse(null);
            var response = association == null || association.getQuestionnaireResponseUuid() == null ? null
                    : questionnaires.getQuestionnaireResponse(association.getQuestionnaireResponseUuid()).orElse(null);
            if (q == null) {
                blockers.add("QUESTIONNAIRE_UNAVAILABLE");
                return blockers;
            }
            var answers = new HashMap<String, List<org.hl7.fhir.r4.model.Type>>();
            if (response != null && !Set.of(QuestionnaireResponse.QuestionnaireResponseStatus.ENTEREDINERROR,
                    QuestionnaireResponse.QuestionnaireResponseStatus.STOPPED).contains(response.getStatus()))
                collect(response.getItem(), answers);
            required(q.getItem(), answers, blockers);
        }
        return blockers;
    }

    public Map<String, String> labels(String caseId) {
        var labels = new LinkedHashMap<String, String>();
        var c = cases.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        if (c.getSiteId() != null || c.getProgramId() == null)
            return labels;
        var program = programs.get(c.getProgramId());
        if (program != null && program.getQuestionnaireUUID() != null)
            questionnaires.getQuestionnaire(program.getQuestionnaireUUID())
                    .ifPresent(q -> labels(q.getItem(), program.getProgramName(), labels));
        return labels;
    }

    private static void labels(List<Questionnaire.QuestionnaireItemComponent> items, String program,
            Map<String, String> labels) {
        for (var item : items) {
            if (item.hasText())
                labels.put("QUESTIONNAIRE_FIELD_REQUIRED:" + item.getLinkId(), program + ": " + item.getText());
            labels(item.getItem(), program, labels);
        }
    }

    private static void collect(List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items,
            Map<String, List<org.hl7.fhir.r4.model.Type>> answers) {
        for (var item : items) {
            for (var answer : item.getAnswer()) {
                if (answer.hasValue())
                    answers.computeIfAbsent(item.getLinkId(), x -> new ArrayList<>()).add(answer.getValue());
                collect(answer.getItem(), answers);
            }
            collect(item.getItem(), answers);
        }
    }

    static void required(List<Questionnaire.QuestionnaireItemComponent> items,
            Map<String, List<org.hl7.fhir.r4.model.Type>> answers, List<String> blockers) {
        for (var item : items) {
            if (!enabled(item, answers))
                continue;
            if (item.getRequired() && item.getType() != Questionnaire.QuestionnaireItemType.GROUP
                    && item.getType() != Questionnaire.QuestionnaireItemType.DISPLAY
                    && answers.getOrDefault(item.getLinkId(), List.of()).stream().noneMatch(v -> !v.isEmpty()))
                blockers.add("QUESTIONNAIRE_FIELD_REQUIRED:" + item.getLinkId());
            required(item.getItem(), answers, blockers);
        }
    }

    private static boolean enabled(Questionnaire.QuestionnaireItemComponent item,
            Map<String, List<org.hl7.fhir.r4.model.Type>> answers) {
        if (item.getEnableWhen().isEmpty())
            return true;
        var predicates = item.getEnableWhen().stream().map(condition -> {
            var values = answers.getOrDefault(condition.getQuestion(), List.of());
            if (condition.getOperator() == Questionnaire.QuestionnaireItemOperator.EXISTS)
                return !values.isEmpty() == condition.getAnswerBooleanType().booleanValue();
            return values.stream().anyMatch(value -> {
                boolean equal = value.equalsDeep(condition.getAnswer());
                if (condition.getOperator() == Questionnaire.QuestionnaireItemOperator.EQUAL)
                    return equal;
                if (condition.getOperator() == Questionnaire.QuestionnaireItemOperator.NOT_EQUAL)
                    return !equal;
                // Numeric/date ordered comparisons follow the FHIR primitive value rather than
                // a display label.
                String left = value.primitiveValue(), right = condition.getAnswer().primitiveValue();
                if (left == null || right == null)
                    throw new IllegalStateException("QUESTIONNAIRE_CONDITION_UNSUPPORTED");
                int comparison;
                try {
                    comparison = new java.math.BigDecimal(left).compareTo(new java.math.BigDecimal(right));
                } catch (NumberFormatException notNumeric) {
                    comparison = left.compareTo(right);
                }
                return switch (condition.getOperator()) {
                case GREATER_THAN -> comparison > 0;
                case LESS_THAN -> comparison < 0;
                case GREATER_OR_EQUAL -> comparison >= 0;
                case LESS_OR_EQUAL -> comparison <= 0;
                default -> throw new IllegalStateException("QUESTIONNAIRE_CONDITION_UNSUPPORTED");
                };
            });
        }).toList();
        return item.getEnableBehavior() == Questionnaire.EnableWhenBehavior.ANY ? predicates.contains(true)
                : !predicates.contains(false);
    }
}
