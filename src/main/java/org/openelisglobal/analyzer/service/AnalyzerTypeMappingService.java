package org.openelisglobal.analyzer.service;

public interface AnalyzerTypeMappingService {

    AnalyzerTypeMappingView getMapping(String profileId, int profileRevision);
}
