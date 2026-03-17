package org.openelisglobal.analyzer.form;

import java.util.ArrayList;
import java.util.List;

/**
 * One row in the file import preview (OGC-324). Matches API schema
 * PreviewRecord.
 */
public class PreviewRecordForm {

    private Integer rowNumber;
    private String sampleId;
    private String testCode;
    private String result;
    private String status; // VALID, WARNING, ERROR
    private List<ValidationMessageForm> validationMessages = new ArrayList<>();

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ValidationMessageForm> getValidationMessages() {
        return validationMessages;
    }

    public void setValidationMessages(List<ValidationMessageForm> validationMessages) {
        this.validationMessages = validationMessages != null ? validationMessages : new ArrayList<>();
    }

    public static class ValidationMessageForm {
        private String code;
        private String message;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
