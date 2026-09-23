package org.openelisglobal.analyzerimport.service;

public record AnalyzerNormalizedResultImportSummary(String analyzerId, int resultsStaged, int resultsHeld,
        int controlResultsProcessed) {
}
