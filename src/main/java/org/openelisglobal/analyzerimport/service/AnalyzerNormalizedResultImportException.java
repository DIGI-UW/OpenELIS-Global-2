package org.openelisglobal.analyzerimport.service;

public class AnalyzerNormalizedResultImportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorKey;

    public AnalyzerNormalizedResultImportException(String errorKey, String message) {
        super(message);
        this.errorKey = errorKey;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
