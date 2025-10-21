package org.openelisglobal.program.valueholder;

import java.util.UUID;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Value holder for program sample questionnaire response data
 */
public class ProgramSampleQuestionnaireResponse {

    private Integer programSampleId;
    private Sample sample;
    private Program program;
    private QuestionnaireResponse questionnaireResponse;
    private UUID questionnaireResponseUuid;

    public ProgramSampleQuestionnaireResponse() {
    }

    public Integer getProgramSampleId() {
        return programSampleId;
    }

    public void setProgramSampleId(Integer programSampleId) {
        this.programSampleId = programSampleId;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public QuestionnaireResponse getQuestionnaireResponse() {
        return questionnaireResponse;
    }

    public void setQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        this.questionnaireResponse = questionnaireResponse;
    }

    public UUID getQuestionnaireResponseUuid() {
        return questionnaireResponseUuid;
    }

    public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
        this.questionnaireResponseUuid = questionnaireResponseUuid;
    }
}
