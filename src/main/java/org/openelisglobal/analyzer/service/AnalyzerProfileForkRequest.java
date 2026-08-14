package org.openelisglobal.analyzer.service;

public record AnalyzerProfileForkRequest(int sourceRevision, String profileId, String displayName) {
}
