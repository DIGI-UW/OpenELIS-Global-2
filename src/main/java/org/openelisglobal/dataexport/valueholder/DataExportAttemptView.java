package org.openelisglobal.dataexport.valueholder;

public record DataExportAttemptView(Long id, String status, String startTime, String endTime, Long durationMs) {
}
