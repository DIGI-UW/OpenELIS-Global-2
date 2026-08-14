package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerProfileCatalogClient {

    List<BridgeProfileCatalogEntry> list(AnalyzerProfileCatalogFilter filter);
}
