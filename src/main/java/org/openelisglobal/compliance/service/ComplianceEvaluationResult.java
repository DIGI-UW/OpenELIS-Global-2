package org.openelisglobal.compliance.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;

public class ComplianceEvaluationResult {

    private ComplianceStatus overallStatus;
    private List<ParameterResult> parameterResults = new ArrayList<>();

    public ComplianceStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(ComplianceStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<ParameterResult> getParameterResults() {
        return parameterResults;
    }

    public void setParameterResults(List<ParameterResult> parameterResults) {
        this.parameterResults = parameterResults;
    }

    public static class ParameterResult {
        private String parameterCode;
        private String displayName;
        private String resultValue;
        private String thresholdDisplay;
        private String units;
        private ComplianceStatus status;

        public String getParameterCode() {
            return parameterCode;
        }

        public void setParameterCode(String parameterCode) {
            this.parameterCode = parameterCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getResultValue() {
            return resultValue;
        }

        public void setResultValue(String resultValue) {
            this.resultValue = resultValue;
        }

        public String getThresholdDisplay() {
            return thresholdDisplay;
        }

        public void setThresholdDisplay(String thresholdDisplay) {
            this.thresholdDisplay = thresholdDisplay;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        public ComplianceStatus getStatus() {
            return status;
        }

        public void setStatus(ComplianceStatus status) {
            this.status = status;
        }
    }
}
