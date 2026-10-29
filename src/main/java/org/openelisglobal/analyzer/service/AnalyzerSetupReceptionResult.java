package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/** Receiving setup evidence is separate from routine clinical activation. */
public record AnalyzerSetupReceptionResult(String analyzerId, Analyzer.AnalyzerStatus status, boolean receiving,
        List<AnalyzerActivationBlocker> blockers) {

    public AnalyzerSetupReceptionResult {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
