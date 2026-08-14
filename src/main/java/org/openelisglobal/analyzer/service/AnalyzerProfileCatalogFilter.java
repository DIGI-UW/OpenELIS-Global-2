package org.openelisglobal.analyzer.service;

public record AnalyzerProfileCatalogFilter(String query, String source, String status, String protocol) {

    public static AnalyzerProfileCatalogFilter empty() {
        return new AnalyzerProfileCatalogFilter(null, null, null, null);
    }
}
