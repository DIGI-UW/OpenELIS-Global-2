package org.openelisglobal.biorepository.controller.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for manifest validation endpoint.
 */
public class ManifestValidationResponse {

    private boolean valid;
    private int invalidCount;
    private List<RowValidationResult> rows = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public void setInvalidCount(int invalidCount) {
        this.invalidCount = invalidCount;
    }

    public List<RowValidationResult> getRows() {
        return rows;
    }

    public void setRows(List<RowValidationResult> rows) {
        this.rows = rows;
    }

    public void addRow(RowValidationResult row) {
        this.rows.add(row);
        if (!row.isValid()) {
            this.invalidCount++;
            this.valid = false;
        }
    }

    /**
     * Validation result for a single row in the manifest.
     */
    public static class RowValidationResult {
        private int rowIndex;
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();

        public RowValidationResult(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public void setRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
    }
}
