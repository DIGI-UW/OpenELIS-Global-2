package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerProfileCatalogClient {

    List<BridgeProfileCatalogEntry> list(AnalyzerProfileCatalogFilter filter);

    BridgeProfileCatalogEntry get(String profileId, Integer revision);

    List<BridgeProfileCatalogEntry> history(String profileId);

    BridgeProfileCatalogEntry fork(String profileId, AnalyzerProfileForkRequest request, String actor);

    BridgeProfileCatalogEntry deactivate(String profileId, String actor);

    BridgeProfileCatalogEntry reactivate(String profileId, String actor);
}
