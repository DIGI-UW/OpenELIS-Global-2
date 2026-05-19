package org.openelisglobal.questionnaire.valueholder;

import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

public class QuestionnaireResponse extends BaseObject<String> {

    private String id;

    private UUID fhirUuid;

    private Questionnaire questionnaire;

    private String subjectId;

    private Set<QuestionnaireResponseItem> responseItems;

    public QuestionnaireResponse() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public Set<QuestionnaireResponseItem> getResponseItems() {
        return responseItems;
    }

    public void setResponseItems(Set<QuestionnaireResponseItem> responseItems) {
        this.responseItems = responseItems;
    }

}