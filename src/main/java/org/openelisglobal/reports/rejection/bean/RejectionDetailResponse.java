package org.openelisglobal.reports.rejection.bean;

import java.sql.Timestamp;
import org.openelisglobal.reports.qi.PagedResponse;

public class RejectionDetailResponse extends PagedResponse<RejectionDetailResponse.RejectionEvent> {

    public static class RejectionEvent {

        private String analysisId;
        private Timestamp rejectedAt;
        private String labNumber;
        private String testName;
        private String reason;
        private String rejectedBy;
        private String location;
        private String nceNumber;

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public Timestamp getRejectedAt() {
            return rejectedAt;
        }

        public void setRejectedAt(Timestamp rejectedAt) {
            this.rejectedAt = rejectedAt;
        }

        public String getLabNumber() {
            return labNumber;
        }

        public void setLabNumber(String labNumber) {
            this.labNumber = labNumber;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getRejectedBy() {
            return rejectedBy;
        }

        public void setRejectedBy(String rejectedBy) {
            this.rejectedBy = rejectedBy;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getNceNumber() {
            return nceNumber;
        }

        public void setNceNumber(String nceNumber) {
            this.nceNumber = nceNumber;
        }
    }
}
