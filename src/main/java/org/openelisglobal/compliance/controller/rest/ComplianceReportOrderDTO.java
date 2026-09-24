package org.openelisglobal.compliance.controller.rest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.openelisglobal.compliance.service.ComplianceEvaluationResult.ParameterResult;

public class ComplianceReportOrderDTO {

    private Long sampleId;
    private String labNumber;
    private String siteCode;
    private String siteName;
    private String standardName;
    private String regulationNumber;
    private String collectionDate;
    private int testCount;
    private String complianceStatus;
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx");
    private String lastGenerated;

    private String gpsCoordinates;
    private String collectionMethod;

    private String waterTemp;
    private String ambientTemp;
    private String weather;
    private String preservation;

    private boolean hasBeenReleased;
    private List<ParameterResult> parameterResults;
    private SignatureDTO analystSignature;
    private SignatureDTO managerSignature;

    public static class SignatureDTO {
        private String signerName;
        private String signerRole;
        private String signedAt;

        public String getSignerName() {
            return signerName;
        }

        public void setSignerName(String signerName) {
            this.signerName = signerName;
        }

        public String getSignerRole() {
            return signerRole;
        }

        public void setSignerRole(String signerRole) {
            this.signerRole = signerRole;
        }

        public String getSignedAt() {
            return signedAt;
        }

        public void setSignedAt(String signedAt) {
            this.signedAt = signedAt;
        }
    }

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

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public String getRegulationNumber() {
        return regulationNumber;
    }

    public void setRegulationNumber(String regulationNumber) {
        this.regulationNumber = regulationNumber;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public String getLastGenerated() {
        return lastGenerated;
    }

    public void setLastGenerated(OffsetDateTime lastGenerated) {
        this.lastGenerated = lastGenerated != null ? lastGenerated.format(DISPLAY_FMT) : null;
    }

    public String getGpsCoordinates() {
        return gpsCoordinates;
    }

    public void setGpsCoordinates(String gpsCoordinates) {
        this.gpsCoordinates = gpsCoordinates;
    }

    public String getCollectionMethod() {
        return collectionMethod;
    }

    public void setCollectionMethod(String collectionMethod) {
        this.collectionMethod = collectionMethod;
    }

    public String getWaterTemp() {
        return waterTemp;
    }

    public void setWaterTemp(String waterTemp) {
        this.waterTemp = waterTemp;
    }

    public String getAmbientTemp() {
        return ambientTemp;
    }

    public void setAmbientTemp(String ambientTemp) {
        this.ambientTemp = ambientTemp;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(String preservation) {
        this.preservation = preservation;
    }

    public List<ParameterResult> getParameterResults() {
        return parameterResults;
    }

    public void setParameterResults(List<ParameterResult> parameterResults) {
        this.parameterResults = parameterResults;
    }

    public boolean isHasBeenReleased() {
        return hasBeenReleased;
    }

    public void setHasBeenReleased(boolean hasBeenReleased) {
        this.hasBeenReleased = hasBeenReleased;
    }

    public SignatureDTO getAnalystSignature() {
        return analystSignature;
    }

    public void setAnalystSignature(SignatureDTO analystSignature) {
        this.analystSignature = analystSignature;
    }

    public SignatureDTO getManagerSignature() {
        return managerSignature;
    }

    public void setManagerSignature(SignatureDTO managerSignature) {
        this.managerSignature = managerSignature;
    }
}
