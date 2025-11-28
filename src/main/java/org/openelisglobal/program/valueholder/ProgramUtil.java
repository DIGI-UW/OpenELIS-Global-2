package org.openelisglobal.program.valueholder;

import java.util.Date;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class ProgramUtil {
    private ProgramSample programSample;
    private Questionnaire programQuestionnaire;
    private QuestionnaireResponse programQuestionnaireResponse;
    private String status;
    private Date lastUpdated;

    public ProgramSample getProgramSample() {
        return programSample;
    }

    public void setProgramSample(ProgramSample programSample) {
        this.programSample = programSample;
        // Auto-set status based on questionnaire response
        if (programSample != null) {
            this.status = programSample.getQuestionnaireResponseUuid() != null ? "COMPLETED" : "PENDING";
            this.lastUpdated = programSample.getLastupdated();
        }
    }

    public Questionnaire getProgramQuestionnaire() {
        return programQuestionnaire;
    }

    public void setProgramQuestionnaire(Questionnaire programQuestionnaire) {
        this.programQuestionnaire = programQuestionnaire;
    }

    public QuestionnaireResponse getProgramQuestionnaireResponse() {
        return programQuestionnaireResponse;
    }

    public void setProgramQuestionnaireResponse(QuestionnaireResponse programQuestionnaireResponse) {
        this.programQuestionnaireResponse = programQuestionnaireResponse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}