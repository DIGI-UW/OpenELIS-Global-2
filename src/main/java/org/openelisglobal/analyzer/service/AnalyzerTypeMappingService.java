package org.openelisglobal.analyzer.service;

public interface AnalyzerTypeMappingService {

    AnalyzerTypeMappingView getMapping(String profileId, int profileRevision);

    AnalyzerTypeMappingView saveMapping(String profileId, int profileRevision, AnalyzerTypeMappingUpdate update,
            String actor);

    AnalyzerSiteBindingConfirmationView confirmMapping(String profileId, int profileRevision,
            AnalyzerSiteBindingConfirmationRequest request, String actor);
}
