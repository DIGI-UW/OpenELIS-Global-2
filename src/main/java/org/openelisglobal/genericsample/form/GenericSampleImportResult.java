package org.openelisglobal.genericsample.form;

import java.util.ArrayList;
import java.util.List;

public class GenericSampleImportResult {

    private boolean valid;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int totalSamplesToCreate;
    private List<ImportRowError> errors;
    private List<String> warnings;
    private List<ImportRow> previewRows;

    public GenericSampleImportResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.previewRows = new ArrayList<>();
        this.valid = true;
    }

    public static class ImportRowError {
        private int rowNumber;
        private String field;
        private String message;

        public ImportRowError(int rowNumber, String field, String message) {
            this.rowNumber = rowNumber;
            this.field = field;
            this.message = message;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ImportRow {
        private int rowNumber;
        private GenericSampleOrderForm.DefaultFields defaultFields;
        private int sampleQuantity;

        public ImportRow(int rowNumber, GenericSampleOrderForm.DefaultFields defaultFields, int sampleQuantity) {
            this.rowNumber = rowNumber;
            this.defaultFields = defaultFields;
            this.sampleQuantity = sampleQuantity;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public GenericSampleOrderForm.DefaultFields getDefaultFields() {
            return defaultFields;
        }

        public void setDefaultFields(GenericSampleOrderForm.DefaultFields defaultFields) {
            this.defaultFields = defaultFields;
        }

        public int getSampleQuantity() {
            return sampleQuantity;
        }

        public void setSampleQuantity(int sampleQuantity) {
            this.sampleQuantity = sampleQuantity;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(int invalidRows) {
        this.invalidRows = invalidRows;
    }

    public int getTotalSamplesToCreate() {
        return totalSamplesToCreate;
    }

    public void setTotalSamplesToCreate(int totalSamplesToCreate) {
        this.totalSamplesToCreate = totalSamplesToCreate;
    }

    public List<ImportRowError> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportRowError> errors) {
        this.errors = errors;
    }

    public void addError(int rowNumber, String field, String message) {
        this.errors.add(new ImportRowError(rowNumber, field, message));
        this.valid = false;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public List<ImportRow> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<ImportRow> previewRows) {
        this.previewRows = previewRows;
    }

    public void addPreviewRow(ImportRow row) {
        this.previewRows.add(row);
    }
}
