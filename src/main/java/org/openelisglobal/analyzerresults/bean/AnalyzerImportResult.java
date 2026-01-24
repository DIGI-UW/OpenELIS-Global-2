package org.openelisglobal.analyzerresults.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result object for analyzer import operations.
 * Provides structured response for import success/failure.
 */
public class AnalyzerImportResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private int importedCount;
    private int rejectedCount;
    private String nonConformityId;
    private QcStatus qcStatus;
    private List<String> errors;
    private List<String> warnings;

    public enum QcStatus {
        PASSED, FAILED, NOT_EVALUATED
    }

    public AnalyzerImportResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.qcStatus = QcStatus.NOT_EVALUATED;
    }

    public static AnalyzerImportResult success(int count) {
        AnalyzerImportResult result = new AnalyzerImportResult();
        result.setSuccess(true);
        result.setImportedCount(count);
        result.setMessage("Successfully imported " + count + " results");
        result.setQcStatus(QcStatus.PASSED);
        return result;
    }

    public static AnalyzerImportResult qcFailure(String ncId, String message) {
        AnalyzerImportResult result = new AnalyzerImportResult();
        result.setSuccess(false);
        result.setNonConformityId(ncId);
        result.setMessage(message);
        result.setQcStatus(QcStatus.FAILED);
        return result;
    }

    public static AnalyzerImportResult validationFailure(List<String> errors) {
        AnalyzerImportResult result = new AnalyzerImportResult();
        result.setSuccess(false);
        result.setErrors(errors);
        result.setMessage("Validation failed: " + errors.size() + " errors");
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public String getNonConformityId() {
        return nonConformityId;
    }

    public void setNonConformityId(String nonConformityId) {
        this.nonConformityId = nonConformityId;
    }

    public QcStatus getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(QcStatus qcStatus) {
        this.qcStatus = qcStatus;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
