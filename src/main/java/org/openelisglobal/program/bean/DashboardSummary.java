package org.openelisglobal.program.bean;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

public class DashboardSummary {

    private long totalEntries;
    private long inProgressEntries;
    private long completedEntries;

    private List<ViewItems> programSample;

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public long getInProgressEntries() {
        return inProgressEntries;
    }

    public void setInProgressEntries(long inProgressEntries) {
        this.inProgressEntries = inProgressEntries;
    }

    public long getCompletedEntries() {
        return completedEntries;
    }

    public void setCompletedEntries(long completedEntries) {
        this.completedEntries = completedEntries;
    }

    public List<ViewItems> getProgramSample() {
        return programSample;
    }

    public void setProgramSample(List<ViewItems> programSample) {
        this.programSample = programSample;
    }

    public static class ViewItems {
        private int programSampleId;
        private String programName;
        private String programCode;
        private Date receivedDate;
        private String accessionNumber;
        private UUID questionnaireResponseUuid;

        public int getProgramSampleId() {
            return programSampleId;
        }

        public void setProgramSampleId(int programSampleId) {
            this.programSampleId = programSampleId;
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

        public Date getReceivedDate() {
            return receivedDate;
        }

        public void setReceivedDate(Date receivedDate) {
            this.receivedDate = receivedDate;
        }

        public String getAccessionNumber() {
            return accessionNumber;
        }

        public void setAccessionNumber(String accessionNumber) {
            this.accessionNumber = accessionNumber;
        }

        public UUID getQuestionnaireResponseUuid() {
            return questionnaireResponseUuid;
        }

        public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
            this.questionnaireResponseUuid = questionnaireResponseUuid;
        }
    }
}
