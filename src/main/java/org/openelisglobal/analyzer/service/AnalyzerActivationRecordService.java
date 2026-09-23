package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerActivationRecordService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerActivationRecord retain(Analyzer analyzer, AnalyzerSiteBindingRevision siteBindingRevision,
            AnalyzerSiteBindingConfirmation confirmation, ObjectNode runtimeAcknowledgement, String intent,
            String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerActivationRecord> findByAnalyzerId(String analyzerId);
}
