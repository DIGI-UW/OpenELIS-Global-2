package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;

/**
 * Canonical outputs to which the lab binds its catalog. Never a parser recipe.
 */
public record AnalyzerMappingDefinition(String profileId, int revision, String revisionFingerprint, String displayName,
        String protocol, List<BridgeAnalyzerProfile.TestDefinition> testDefinitions) {

    private static final ObjectMapper JSON = new ObjectMapper();
    public static AnalyzerMappingDefinition from(BridgeAnalyzerProfile profile) {
        return new AnalyzerMappingDefinition(profile.profileId(), profile.revision(), profile.revisionFingerprint(),
                profile.displayName(), profile.protocol(),
                profile.testDefinitions().stream()
                        .map(test -> new BridgeAnalyzerProfile.TestDefinition(test.analyzerCode(), List.of(),
                                test.testNameHint(), test.codings(), test.unit(), test.resultType(),
                                test.resultValues(), test.valueCodes(), test.callComponent(),
                                test.components().stream()
                                        .map(c -> new BridgeAnalyzerProfile.ComponentDefinition(c.code(), c.label(),
                                                c.resultType(), c.unit(), c.subIdentity(), c.resultValues(),
                                                c.valueCodes(), Map.of()))
                                        .toList(),
                                Map.of()))
                        .toList());
    }

    public static AnalyzerMappingDefinition of(AnalyzerMapping mapping) {
        if (mapping.getSourceDefinitionJson() == null) {
            // Older mappings remain repairable from saved rows and received canonical
            // results.
            return new AnalyzerMappingDefinition(mapping.getProfileId(), mapping.getProfileRevision(),
                    mapping.getProfileFingerprint(),
                    mapping.getAnalyzer() == null ? mapping.getProfileId() : mapping.getAnalyzer().getName(), null,
                    List.of());
        }
        try {
            return JSON.readValue(mapping.getSourceDefinitionJson(), AnalyzerMappingDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored mapping definition is invalid", e);
        }
    }

    public String toJson() {
        try {
            return JSON.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot retain mapping definition", e);
        }
    }
}
