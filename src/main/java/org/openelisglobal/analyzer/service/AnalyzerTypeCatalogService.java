package org.openelisglobal.analyzer.service;

import java.util.List;

public interface AnalyzerTypeCatalogService {

    List<AnalyzerTypeCatalogSummary> list(AnalyzerProfileCatalogFilter filter);
}
