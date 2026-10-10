package org.openelisglobal.analyzer.service;

public interface AnalyzerMappingEditorService {

    AnalyzerMappingView getMapping(String profileId, int profileRevision);

    AnalyzerMappingView saveMapping(String profileId, int profileRevision, AnalyzerMappingUpdate update, String actor);

    AnalyzerMappingConfirmationView confirmMapping(String profileId, int profileRevision,
            AnalyzerMappingConfirmationRequest request, String actor);
}
