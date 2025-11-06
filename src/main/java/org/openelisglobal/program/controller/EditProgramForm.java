package org.openelisglobal.program.controller;

import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.program.valueholder.Program;

public class EditProgramForm {

    private Program program;

    private Questionnaire additionalOrderEntryQuestions;

    private String testSectionId;
    private String testSectionName;

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Questionnaire getAdditionalOrderEntryQuestions() {
        return additionalOrderEntryQuestions;
    }

    public void setAdditionalOrderEntryQuestions(Questionnaire additionalOrderEntryQuestions) {
        this.additionalOrderEntryQuestions = additionalOrderEntryQuestions;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getTestSectionName() {
        return testSectionName;
    }

    public void setTestSectionName(String testSectionName) {
        this.testSectionName = testSectionName;
    }
}
