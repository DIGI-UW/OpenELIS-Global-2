package org.openelisglobal.reportqueue.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * What a queue row shows when expanded: its tests, the order block and the
 * version history.
 */
public class ReportQueueRowDetailDTO {

    public static class TestRowDTO {
        private Long analysisId;
        private String testName;
        private String result;
        private String flag;
        private String status;
        private String releasedAt;
        private String validatedBy;
        private boolean released;

        public Long getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(Long analysisId) {
            this.analysisId = analysisId;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getFlag() {
            return flag;
        }

        public void setFlag(String flag) {
            this.flag = flag;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReleasedAt() {
            return releasedAt;
        }

        public void setReleasedAt(String releasedAt) {
            this.releasedAt = releasedAt;
        }

        public String getValidatedBy() {
            return validatedBy;
        }

        public void setValidatedBy(String validatedBy) {
            this.validatedBy = validatedBy;
        }

        public boolean isReleased() {
            return released;
        }

        public void setReleased(boolean released) {
            this.released = released;
        }
    }

    public static class PrintEventDTO {
        private String printedAt;
        private String printedBy;

        public String getPrintedAt() {
            return printedAt;
        }

        public void setPrintedAt(String printedAt) {
            this.printedAt = printedAt;
        }

        public String getPrintedBy() {
            return printedBy;
        }

        public void setPrintedBy(String printedBy) {
            this.printedBy = printedBy;
        }
    }

    public static class VersionDTO {
        private Long id;
        private int version;
        private String issuedAt;
        private String issuedBy;
        private boolean amended;
        private String changeSummary;
        private int releasedCount;
        private int totalCount;
        private String sha256;
        private boolean archived;
        private boolean legacy;
        private boolean archiveFailed;
        private List<PrintEventDTO> reprints = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(String issuedAt) {
            this.issuedAt = issuedAt;
        }

        public String getIssuedBy() {
            return issuedBy;
        }

        public void setIssuedBy(String issuedBy) {
            this.issuedBy = issuedBy;
        }

        public boolean isAmended() {
            return amended;
        }

        public void setAmended(boolean amended) {
            this.amended = amended;
        }

        public String getChangeSummary() {
            return changeSummary;
        }

        public void setChangeSummary(String changeSummary) {
            this.changeSummary = changeSummary;
        }

        public int getReleasedCount() {
            return releasedCount;
        }

        public void setReleasedCount(int releasedCount) {
            this.releasedCount = releasedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }

        public boolean isArchived() {
            return archived;
        }

        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        public boolean isLegacy() {
            return legacy;
        }

        public void setLegacy(boolean legacy) {
            this.legacy = legacy;
        }

        public boolean isArchiveFailed() {
            return archiveFailed;
        }

        public void setArchiveFailed(boolean archiveFailed) {
            this.archiveFailed = archiveFailed;
        }

        public List<PrintEventDTO> getReprints() {
            return reprints;
        }

        public void setReprints(List<PrintEventDTO> reprints) {
            this.reprints = reprints;
        }
    }

    private Long sampleId;
    private String labNumber;
    private String facility;
    private String ward;
    private String requester;
    private String collected;
    private String received;
    private String installDate;
    private List<TestRowDTO> tests = new ArrayList<>();
    private List<VersionDTO> versions = new ArrayList<>();

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getCollected() {
        return collected;
    }

    public void setCollected(String collected) {
        this.collected = collected;
    }

    public String getReceived() {
        return received;
    }

    public void setReceived(String received) {
        this.received = received;
    }

    public String getInstallDate() {
        return installDate;
    }

    public void setInstallDate(String installDate) {
        this.installDate = installDate;
    }

    public List<TestRowDTO> getTests() {
        return tests;
    }

    public void setTests(List<TestRowDTO> tests) {
        this.tests = tests;
    }

    public List<VersionDTO> getVersions() {
        return versions;
    }

    public void setVersions(List<VersionDTO> versions) {
        this.versions = versions;
    }
}
