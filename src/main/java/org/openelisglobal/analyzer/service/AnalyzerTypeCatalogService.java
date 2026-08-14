package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerTypeCatalogService {

    List<AnalyzerTypeCatalogSummary> list(AnalyzerProfileCatalogFilter filter);

    AnalyzerTypeCatalogSummary get(String profileId, Integer revision);

    List<BridgeProfileCatalogEntry> history(String profileId);

    AnalyzerTypeCatalogSummary fork(String profileId, AnalyzerProfileForkRequest request, String actor);

    AnalyzerTypeCatalogSummary deactivate(String profileId, String actor);

    AnalyzerTypeCatalogSummary reactivate(String profileId, String actor);
}
