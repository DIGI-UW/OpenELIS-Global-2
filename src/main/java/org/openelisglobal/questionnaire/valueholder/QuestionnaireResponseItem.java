package org.openelisglobal.questionnaire.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class QuestionnaireResponseItem extends BaseObject<String> {

    private String id;

    private QuestionnaireResponse questionnaireResponse;

    private String linkId;

    private String answer;

    public QuestionnaireResponseItem() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public QuestionnaireResponse getQuestionnaireResponse() {
        return questionnaireResponse;
    }

    public void setQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        this.questionnaireResponse = questionnaireResponse;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}