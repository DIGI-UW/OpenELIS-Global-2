package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Composed lab-facing view; Bridge connection values are never persisted here.
 */
public record AnalyzerInstanceView(AnalyzerInstanceState state, ObjectNode connection, String connectionErrorKey) {

    public AnalyzerInstanceView {
        connection = connection == null ? null : connection.deepCopy();
    }

    public boolean connected() {
        return state != null && state.bridgeConnectionId() != null && connection != null && connectionErrorKey == null;
    }
}
