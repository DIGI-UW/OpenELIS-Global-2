/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.analyzerresults.bean;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for analyzer result details including previous results, QC data, reagent
 * lots, and run info. Contains validation annotations to enforce data integrity
 * (MAJ-006 fix).
 */
public class AnalyzerResultDetailsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PreviousResult> previousResults = new ArrayList<>();
    private List<QCResult> qcData = new ArrayList<>();
    private List<ReagentLot> reagentLots = new ArrayList<>();
    private RunInfo runInfo;
    private DeltaCheck deltaCheck;

    public AnalyzerResultDetailsDTO() {
    }

    /**
     * Represents a previous result for delta check comparison. Contains validation
     * for date format, value, and status.
     */
    public static class PreviousResult implements Serializable {
        private static final long serialVersionUID = 1L;

        @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$|^$", message = "Date must be in MM/dd/yyyy format")
        private String date;

        @Size(max = 50, message = "Value must not exceed 50 characters")
        private String value;

        @Pattern(regexp = "^(normal|abnormal|critical)?$", message = "Status must be normal, abnormal, or critical")
        private String status; // normal, abnormal, critical

        public PreviousResult() {
        }

        public PreviousResult(String date, String value, String status) {
            this.date = date;
            this.value = value;
            this.status = status;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Represents a QC result with validation for required fields.
     */
    public static class QCResult implements Serializable {
        private static final long serialVersionUID = 1L;

        @Pattern(regexp = "^(High|Normal|Low|Control)?$", message = "Level must be High, Normal, Low, or Control")
        private String level; // High, Normal, Low

        @Size(max = 50, message = "Expected value must not exceed 50 characters")
        private String expected;

        @Size(max = 50, message = "Actual value must not exceed 50 characters")
        private String actual;

        @Pattern(regexp = "^(pass|fail)?$", message = "Status must be pass or fail")
        private String status; // pass, fail

        @Size(max = 20, message = "CV must not exceed 20 characters")
        private String cv; // coefficient of variation

        @Size(max = 20, message = "SD must not exceed 20 characters")
        private String sd; // standard deviation

        public QCResult() {
        }

        public QCResult(String level, String expected, String actual, String status, String cv) {
            this.level = level;
            this.expected = expected;
            this.actual = actual;
            this.status = status;
            this.cv = cv;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getExpected() {
            return expected;
        }

        public void setExpected(String expected) {
            this.expected = expected;
        }

        public String getActual() {
            return actual;
        }

        public void setActual(String actual) {
            this.actual = actual;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCv() {
            return cv;
        }

        public void setCv(String cv) {
            this.cv = cv;
        }

        public String getSd() {
            return sd;
        }

        public void setSd(String sd) {
            this.sd = sd;
        }
    }

    /**
     * Represents a reagent lot with FIFO tracking and expiration status.
     */
    public static class ReagentLot implements Serializable {
        private static final long serialVersionUID = 1L;

        @Size(max = 50, message = "ID must not exceed 50 characters")
        private String id;

        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @Size(max = 50, message = "Lot must not exceed 50 characters")
        private String lot;

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", message = "Expires date must be in yyyy-MM-dd format")
        private String expires;

        @Pattern(regexp = "^(ok|expiring-soon|expired)?$", message = "Status must be ok, expiring-soon, or expired")
        private String status; // ok, expiring-soon, expired

        private Integer fifoPosition; // 1 = first in line

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", message = "Opened date must be in yyyy-MM-dd format")
        private String openedDate;

        public ReagentLot() {
        }

        public ReagentLot(String name, String lot, String expires, String status) {
            this.name = name;
            this.lot = lot;
            this.expires = expires;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLot() {
            return lot;
        }

        public void setLot(String lot) {
            this.lot = lot;
        }

        public String getExpires() {
            return expires;
        }

        public void setExpires(String expires) {
            this.expires = expires;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getFifoPosition() {
            return fifoPosition;
        }

        public void setFifoPosition(Integer fifoPosition) {
            this.fifoPosition = fifoPosition;
        }

        public String getOpenedDate() {
            return openedDate;
        }

        public void setOpenedDate(String openedDate) {
            this.openedDate = openedDate;
        }
    }

    /**
     * Represents run information including analyzer, operator, and QC status.
     */
    public static class RunInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @Size(max = 50, message = "Run ID must not exceed 50 characters")
        private String runId;

        private String runDate;

        @Size(max = 100, message = "Analyzer name must not exceed 100 characters")
        private String analyzer;

        @Size(max = 100, message = "Operator name must not exceed 100 characters")
        private String operator;

        @Pattern(regexp = "^(pass|fail|pending|none)?$", message = "QC status must be pass, fail, pending, or none")
        private String qcStatus; // pass, fail, pending

        private Integer sampleCount;
        private Integer qcCount;

        public RunInfo() {
        }

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public String getRunDate() {
            return runDate;
        }

        public void setRunDate(String runDate) {
            this.runDate = runDate;
        }

        public String getAnalyzer() {
            return analyzer;
        }

        public void setAnalyzer(String analyzer) {
            this.analyzer = analyzer;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getQcStatus() {
            return qcStatus;
        }

        public void setQcStatus(String qcStatus) {
            this.qcStatus = qcStatus;
        }

        public Integer getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(Integer sampleCount) {
            this.sampleCount = sampleCount;
        }

        public Integer getQcCount() {
            return qcCount;
        }

        public void setQcCount(Integer qcCount) {
            this.qcCount = qcCount;
        }
    }

    /**
     * Represents delta check information comparing current vs previous results.
     */
    public static class DeltaCheck implements Serializable {
        private static final long serialVersionUID = 1L;

        @Size(max = 50, message = "Previous value must not exceed 50 characters")
        private String previous;

        @Size(max = 50, message = "Current value must not exceed 50 characters")
        private String current;

        @Pattern(regexp = "^[+-]?\\d+\\.?\\d*%?$|^$", message = "Change must be a percentage format (e.g., +15.2%)")
        private String change; // e.g., "+15.2%"

        @Size(max = 20, message = "Threshold must not exceed 20 characters")
        private String threshold; // e.g., "±20%"

        private boolean exceeded;

        public DeltaCheck() {
        }

        public DeltaCheck(String previous, String current, String change, String threshold, boolean exceeded) {
            this.previous = previous;
            this.current = current;
            this.change = change;
            this.threshold = threshold;
            this.exceeded = exceeded;
        }

        public String getPrevious() {
            return previous;
        }

        public void setPrevious(String previous) {
            this.previous = previous;
        }

        public String getCurrent() {
            return current;
        }

        public void setCurrent(String current) {
            this.current = current;
        }

        public String getChange() {
            return change;
        }

        public void setChange(String change) {
            this.change = change;
        }

        public String getThreshold() {
            return threshold;
        }

        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }

        public boolean isExceeded() {
            return exceeded;
        }

        public void setExceeded(boolean exceeded) {
            this.exceeded = exceeded;
        }
    }

    public List<PreviousResult> getPreviousResults() {
        return previousResults;
    }

    public void setPreviousResults(List<PreviousResult> previousResults) {
        this.previousResults = previousResults;
    }

    public List<QCResult> getQcData() {
        return qcData;
    }

    public void setQcData(List<QCResult> qcData) {
        this.qcData = qcData;
    }

    public List<ReagentLot> getReagentLots() {
        return reagentLots;
    }

    public void setReagentLots(List<ReagentLot> reagentLots) {
        this.reagentLots = reagentLots;
    }

    public RunInfo getRunInfo() {
        return runInfo;
    }

    public void setRunInfo(RunInfo runInfo) {
        this.runInfo = runInfo;
    }

    public DeltaCheck getDeltaCheck() {
        return deltaCheck;
    }

    public void setDeltaCheck(DeltaCheck deltaCheck) {
        this.deltaCheck = deltaCheck;
    }
}
