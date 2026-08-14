package org.openelisglobal.analyzer.service;

public interface AnalyzerSiteBindingService {

    AnalyzerSiteBindingSnapshot create(AnalyzerSiteBindingDraft draft, String actor);

    AnalyzerSiteBindingSnapshot revise(String bindingId, AnalyzerSiteBindingDraft draft, String actor);
}
