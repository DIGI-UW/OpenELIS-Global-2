package org.openelisglobal.configuration.service;

public record ConfigurationReloadFileResult(String domain, String fileName, Status status, String skippedReason,
        String errorMessage) {

    public enum Status {
        PROCESSED, SKIPPED, ERROR
    }

    public static ConfigurationReloadFileResult processed(String domain, String fileName) {
        return new ConfigurationReloadFileResult(domain, fileName, Status.PROCESSED, null, null);
    }

    public static ConfigurationReloadFileResult skipped(String domain, String fileName, String reason) {
        return new ConfigurationReloadFileResult(domain, fileName, Status.SKIPPED, reason, null);
    }

    public static ConfigurationReloadFileResult error(String domain, String fileName, String errorMessage) {
        return new ConfigurationReloadFileResult(domain, fileName, Status.ERROR, null, errorMessage);
    }
}
