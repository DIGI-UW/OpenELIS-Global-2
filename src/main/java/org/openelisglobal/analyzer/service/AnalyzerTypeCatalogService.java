package org.openelisglobal.analyzer.service;

public interface AnalyzerTypeCatalogService {

    AnalyzerTypeCatalogView getCatalog();

    AnalyzerTypeCatalogView.TypeSummary getType(String profileId, int revision);
}
