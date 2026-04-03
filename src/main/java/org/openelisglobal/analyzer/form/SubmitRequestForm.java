package org.openelisglobal.analyzer.form;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body for submit-after-preview (OGC-324). Matches API schema
 * SubmitRequest.
 */
public class SubmitRequestForm {

    /** Upload audit ID from preview response. */
    private Long previewSessionId;
    /** Row indices to exclude from import (0-based). */
    private List<Integer> excludedRows = new ArrayList<>();

    public Long getPreviewSessionId() {
        return previewSessionId;
    }

    public void setPreviewSessionId(Long previewSessionId) {
        this.previewSessionId = previewSessionId;
    }

    public List<Integer> getExcludedRows() {
        return excludedRows;
    }

    public void setExcludedRows(List<Integer> excludedRows) {
        this.excludedRows = excludedRows != null ? excludedRows : new ArrayList<>();
    }
}
