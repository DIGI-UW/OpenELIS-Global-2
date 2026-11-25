package org.openelisglobal.analyzer.service;

/**
 * A result the Bridge holds without having delivered it to OpenELIS. analyzerId
 * and analyzerName are null when the sender matches no OpenELIS analyzer.
 * sender is the message header's sender field (ASTM H.5, HL7 MSH-3). Only
 * dead-lettered rows are actionable (retry or dismiss).
 */
public record AnalyzerDeliveryIssue(String id, String state, String analyzerId, String analyzerName,
        String connectionId, String sourceId, String sender, String protocol, String accession, int attempts,
        String receivedAt, String failureReason, Integer lastHttpStatus, String lastError, boolean actionable) {
}
