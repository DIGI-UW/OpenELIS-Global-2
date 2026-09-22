package org.openelisglobal.program.controller;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.program.valueholder.Program;

public class EditProgramForm {

    private Program program;

    private Questionnaire additionalOrderEntryQuestions;

    // Legacy single lab-unit FK; kept populated for readers that have not
    // migrated to the many-to-many labUnitIds set.
    private String testSectionId;
    private String testSectionName;

    // Programs V2 additive fields (server-side transport for the admin UI).
    // Domain and active carry directly from Program; labUnitIds is the
    // canonical many-to-many surface, replacing testSectionId over time.
    private String domain;
    private Boolean active;
    private List<String> labUnitIds = new ArrayList<>();

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
        this.labUnitIds = labUnitIds == null ? new ArrayList<>() : labUnitIds;
    }
}
