package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;

public interface AnalyzerActivationCandidateService {

    AnalyzerActivationCandidate retain(Analyzer analyzer, AnalyzerSiteBindingRevision siteBindingRevision,
            AnalyzerSiteBindingConfirmation confirmation, AnalyzerActivationDocuments documents, String actor);

    List<AnalyzerActivationCandidate> findByAnalyzerId(String analyzerId);
}
