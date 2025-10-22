package org.openelisglobal.program.valueholder.generalProgram;

public class GeneralProgramDisplayItem {
    private String programId;
    private String programName;
    private String programCode;
    private String description;
    private Long orderCount;
    private String testSectionName;
    private Boolean hasQuestionnaire;

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public String getTestSectionName() {
        return testSectionName;
    }

    public void setTestSectionName(String testSectionName) {
        this.testSectionName = testSectionName;
    }

    public Boolean getHasQuestionnaire() {
        return hasQuestionnaire;
    }

    public void setHasQuestionnaire(Boolean hasQuestionnaire) {
        this.hasQuestionnaire = hasQuestionnaire;
    }
}
