package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/**
 * Immutable activation candidate and the exact Bridge registration it names.
 */
public record AnalyzerActivationDocuments(ObjectNode candidate, ObjectNode registration) {

    public AnalyzerActivationDocuments {
        candidate = Objects.requireNonNull(candidate, "candidate").deepCopy();
        registration = Objects.requireNonNull(registration, "registration").deepCopy();
    }

    @Override
    public ObjectNode candidate() {
        return candidate.deepCopy();
    }

    @Override
    public ObjectNode registration() {
        return registration.deepCopy();
    }
}
