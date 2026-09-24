package org.openelisglobal.program.controller;

import java.util.List;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.program.valueholder.Program;

public class EditProgramForm {

    private Program program;

    private Questionnaire additionalOrderEntryQuestions;

    /**
     * Legacy single lab-unit FK, honoured when {@link #labUnitIds} is absent so
     * older clients keep working.
     */
    private String testSectionId;
    private String testSectionName;

    /**
     * Programs V2 additive fields. Each is optional on input: a client that omits
     * one keeps the persisted value. {@code labUnitIds} stays {@code null} when
     * absent so the controller can tell "not sent" from "clear all".
     */
    private String domain;
    private Boolean active;
    private List<String> labUnitIds;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<String> getLabUnitIds() {
        return labUnitIds;
    }

    public void setLabUnitIds(List<String> labUnitIds) {
        this.labUnitIds = labUnitIds;
    }
}
