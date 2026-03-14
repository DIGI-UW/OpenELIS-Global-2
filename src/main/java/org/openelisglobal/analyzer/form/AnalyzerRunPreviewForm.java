package org.openelisglobal.analyzer.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response for upload preview (OGC-324). Matches API schema AnalyzerRunPreview.
 */
public class AnalyzerRunPreviewForm {

    private Integer totalRecords;
    private Integer validRecords;
    private Integer warningRecords;
    private Integer errorRecords;
    private List<PreviewRecordForm> records = new ArrayList<>();
    private Map<String, Object> customPreviewData;
    /** Upload audit ID for submit step. */
    private Long uploadId;
    /** True if file hash matched a previous upload (duplicate warning). */
    private Boolean duplicateWarning;

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getValidRecords() {
        return validRecords;
    }

    public void setValidRecords(Integer validRecords) {
        this.validRecords = validRecords;
    }

    public Integer getWarningRecords() {
        return warningRecords;
    }

    public void setWarningRecords(Integer warningRecords) {
        this.warningRecords = warningRecords;
    }

    public Integer getErrorRecords() {
        return errorRecords;
    }

    public void setErrorRecords(Integer errorRecords) {
        this.errorRecords = errorRecords;
    }

    public List<PreviewRecordForm> getRecords() {
        return records;
    }

    public void setRecords(List<PreviewRecordForm> records) {
        this.records = records != null ? records : new ArrayList<>();
    }

    public Map<String, Object> getCustomPreviewData() {
        return customPreviewData;
    }

    public void setCustomPreviewData(Map<String, Object> customPreviewData) {
        this.customPreviewData = customPreviewData;
    }

    public Long getUploadId() {
        return uploadId;
    }

    public void setUploadId(Long uploadId) {
        this.uploadId = uploadId;
    }

    public Boolean getDuplicateWarning() {
        return duplicateWarning;
    }

    public void setDuplicateWarning(Boolean duplicateWarning) {
        this.duplicateWarning = duplicateWarning;
    }
}
