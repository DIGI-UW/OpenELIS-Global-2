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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AnalyzerResultDetailsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PreviousResult> previousResults = new ArrayList<>();
    private List<QCResult> qcData = new ArrayList<>();
    private List<ReagentLot> reagentLots = new ArrayList<>();
    private RunInfo runInfo;
    private DeltaCheck deltaCheck;

    public AnalyzerResultDetailsDTO() {
    }

    public static class PreviousResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private String value;
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

    public static class QCResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String level; // High, Normal, Low
        private String expected;
        private String actual;
        private String status; // pass, fail
        private String cv; // coefficient of variation
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

    public static class ReagentLot implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private String lot;
        private String expires;
        private String status; // ok, expiring-soon, expired
        private Integer fifoPosition; // 1 = first in line
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

    public static class RunInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String runId;
        private String runDate;
        private String analyzer;
        private String operator;
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

    public static class DeltaCheck implements Serializable {
        private static final long serialVersionUID = 1L;
        private String previous;
        private String current;
        private String change; // e.g., "+15.2%"
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
