package org.openelisglobal.analyzer.service;

import java.util.List;

/** Safe authoring command for one Bridge-owned profile draft. */
public record AnalyzerControlRecognitionUpdate(String mode, boolean affirmedNoControlResults,
        List<Condition> conditions) {

    public AnalyzerControlRecognitionUpdate {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public record Condition(String key, String kind, String sourceKey, String value, String controlLevel,
            String controlType) {
    }
}
