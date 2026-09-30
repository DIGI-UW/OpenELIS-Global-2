package org.openelisglobal.analyzer.service;

import java.util.Map;

public record AnalyzerActivationBlocker(String code, Map<String, Object> args) {

    public AnalyzerActivationBlocker {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Activation blocker code is required");
        }
        args = args == null ? Map.of() : Map.copyOf(args);
    }

    public AnalyzerActivationBlocker(String code) {
        this(code, Map.of());
    }
}
