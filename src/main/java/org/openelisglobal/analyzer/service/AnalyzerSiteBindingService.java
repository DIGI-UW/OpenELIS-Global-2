package org.openelisglobal.analyzer.service;

public interface AnalyzerSiteBindingService {

    AnalyzerSiteBindingSnapshot findOrCreate(AnalyzerSiteBindingDraft draft, String actor);

    AnalyzerSiteBindingSnapshot create(AnalyzerSiteBindingDraft draft, String actor);

    AnalyzerSiteBindingSnapshot revise(String bindingId, AnalyzerSiteBindingDraft draft, String actor);
}
