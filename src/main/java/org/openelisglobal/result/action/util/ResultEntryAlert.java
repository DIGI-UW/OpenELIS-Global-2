package org.openelisglobal.result.action.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openelisglobal.result.valueholder.Result;

/**
 * OGC-1417: one value in a save that the person entering it has to acknowledge
 * (a critical value) or confirm (a value outside the valid range) before the
 * save goes through. Serialized into the refusal so the screen can show what is
 * owed; the {@link Result} it was built for is kept so the acknowledgement can
 * name the persisted result once the save commits.
 */
public class ResultEntryAlert {

    public static final String KIND_CRITICAL = "CRITICAL";
    public static final String KIND_INVALID = "INVALID";

    private final String kind;
    private final String value;
    private Double lowValid;
    private Double highValid;
    private String analysisId;
    private String rowId;
    private String testId;
    private String componentId;
    private String testName;
    private String accessionNumber;
    private boolean acknowledged;
    private Result result;

    public ResultEntryAlert(String kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public String getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    public Double getLowValid() {
        return lowValid;
    }

    public Double getHighValid() {
        return highValid;
    }

    public void setValidRange(Double lowValid, Double highValid) {
        this.lowValid = lowValid;
        this.highValid = highValid;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    /** The analyzer review row the value was typed into, when it came from one. */
    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    @JsonIgnore
    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    @JsonIgnore
    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    @JsonIgnore
    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
