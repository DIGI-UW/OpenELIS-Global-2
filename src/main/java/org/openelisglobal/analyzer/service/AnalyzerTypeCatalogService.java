package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface AnalyzerTypeCatalogService {

    List<AnalyzerTypeCatalogSummary> list(AnalyzerProfileCatalogFilter filter);

    AnalyzerTypeCatalogSummary get(String profileId, Integer revision);

    List<BridgeProfileCatalogEntry> history(String profileId);

    JsonNode exportProfile(String profileId, Integer revision);

    AnalyzerTypeCatalogSummary fork(String profileId, AnalyzerProfileForkRequest request, String actor);

    AnalyzerTypeCatalogSummary deactivate(String profileId, String actor);

    AnalyzerTypeCatalogSummary reactivate(String profileId, String actor);
}
