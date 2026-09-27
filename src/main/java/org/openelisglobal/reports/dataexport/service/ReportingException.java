package org.openelisglobal.reports.dataexport.service;

public class ReportingException extends RuntimeException {
    private final int status;

    public ReportingException(int status, String code) {
        super(code);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
