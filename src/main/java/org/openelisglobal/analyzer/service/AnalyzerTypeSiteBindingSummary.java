package org.openelisglobal.analyzer.service;

import java.time.Instant;

public record AnalyzerTypeSiteBindingSummary(String bindingId, String revisionId, int revisionNumber,
        int bridgeProfileRevision, String fingerprint, String createdBy, Instant createdAt) {
}
