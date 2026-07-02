package org.openelisglobal.batchworkplan.form;

import java.util.List;

public class BatchWorkplanRequest {

    private String name;
    private String testSectionId;
    private String notes;
    private List<String> analysisIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getAnalysisIds() {
        return analysisIds;
    }

    public void setAnalysisIds(List<String> analysisIds) {
        this.analysisIds = analysisIds;
    }
}
