package org.openelisglobal.microbiology.service;

public record MicroCultureAnalyzerEventCommand(String externalEventId, String eventType, String analyzerId,
        String sourceId, String targetCaseId, String note) {
}
