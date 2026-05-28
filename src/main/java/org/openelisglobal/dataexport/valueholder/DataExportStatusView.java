package org.openelisglobal.dataexport.valueholder;

public record DataExportStatusView(Long id, String endpoint, Integer maxIntervalMinutes, String lastSuccess,
        String lastAttempt, String lastStatus, long failedLast24h, long totalLast24h) {
}
