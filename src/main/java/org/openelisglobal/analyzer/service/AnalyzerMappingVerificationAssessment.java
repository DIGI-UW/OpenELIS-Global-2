package org.openelisglobal.analyzer.service;

import java.util.Objects;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;

public record AnalyzerMappingVerificationAssessment(boolean mappingsCurrent, boolean recognitionCurrent,
        AnalyzerMappingConfirmation confirmation) {

    public static AnalyzerMappingVerificationAssessment current(AnalyzerMappingConfirmation confirmation) {
        return new AnalyzerMappingVerificationAssessment(true, true, Objects.requireNonNull(confirmation));
    }

    public static AnalyzerMappingVerificationAssessment unconfirmed() {
        return new AnalyzerMappingVerificationAssessment(false, false, null);
    }

    public Optional<AnalyzerMappingConfirmation> currentConfirmation() {
        return mappingsCurrent && recognitionCurrent ? Optional.ofNullable(confirmation) : Optional.empty();
    }
}
