package org.openelisglobal.analyzer.service;

public record AnalyzerTypeMappingProgress(int total, int bound, int unresolved, int ignored, int missing, int extra) {
}
