package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;

public interface AnalyzerSiteBindingConfirmationService {

    AnalyzerSiteBindingConfirmationView confirm(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint,
            AnalyzerSiteBindingConfirmationRequest request, String actor);

    AnalyzerSiteBindingConfirmationView getStatus(AnalyzerSiteBindingSnapshot candidate, String recognitionFingerprint);

    Optional<AnalyzerSiteBindingConfirmation> findCurrent(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint);
}
