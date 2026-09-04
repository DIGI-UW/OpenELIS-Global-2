package org.openelisglobal.analyzer.service;

import java.util.Objects;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;

public record AnalyzerSiteBindingVerificationAssessment(boolean mappingsCurrent, boolean recognitionCurrent,
        AnalyzerSiteBindingConfirmation confirmation) {

    public static AnalyzerSiteBindingVerificationAssessment current(AnalyzerSiteBindingConfirmation confirmation) {
        return new AnalyzerSiteBindingVerificationAssessment(true, true, Objects.requireNonNull(confirmation));
    }

    public static AnalyzerSiteBindingVerificationAssessment unconfirmed() {
        return new AnalyzerSiteBindingVerificationAssessment(false, false, null);
    }

    public Optional<AnalyzerSiteBindingConfirmation> currentConfirmation() {
        return mappingsCurrent && recognitionCurrent ? Optional.ofNullable(confirmation) : Optional.empty();
    }
}
