package org.openelisglobal.analyzerimport.service;

/**
 * What OpenELIS did with one delivery, and the reference the sender keeps as
 * proof.
 *
 * <p>
 * {@code receiptId} identifies the durable record of this acceptance. It is the
 * same value whether the delivery was accepted now or accepted earlier and sent
 * again, which is what lets the Analyzer Bridge confirm a delivery against
 * OpenELIS rather than against its own logs.
 */
public record AnalyzerNormalizedResultImportSummary(String receiptId, String analyzerId, int resultsStaged,
        int resultsHeld, int controlResultsProcessed) {
}
