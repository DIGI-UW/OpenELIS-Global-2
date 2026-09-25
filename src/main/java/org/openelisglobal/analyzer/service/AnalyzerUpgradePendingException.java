package org.openelisglobal.analyzer.service;

/** A migration issue that can be shown to an operator by its message key. */
public class AnalyzerUpgradePendingException extends RuntimeException {
    private final String reasonKey;

    public AnalyzerUpgradePendingException(String reasonKey, String detail) {
        super(detail);
        this.reasonKey = reasonKey;
    }

    public AnalyzerUpgradePendingException(String reasonKey, String detail, Throwable cause) {
        super(detail, cause);
        this.reasonKey = reasonKey;
    }

    public String reasonKey() {
        return reasonKey;
    }
}
