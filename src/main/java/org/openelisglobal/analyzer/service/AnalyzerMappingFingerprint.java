package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public final class AnalyzerMappingFingerprint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private AnalyzerMappingFingerprint() {
    }

    public static String calculate(AnalyzerMappingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Mapping draft is required");
        }

        ObjectNode canonical = JSON.createObjectNode();
        ArrayNode tests = canonical.putArray("tests");
        draft.tests().stream().sorted(Comparator.comparing(AnalyzerMappingTestDraft::sourceRowKey)
                .thenComparing(AnalyzerMappingTestDraft::subIdentity)).forEach(row -> {
                    ObjectNode value = tests.addObject();
                    value.put("sourceRowKey", row.sourceRowKey());
                    value.put("subIdentity", row.subIdentity());
                    value.put("mappingState", row.mappingState().name());
                    value.put("origin", row.origin().name());
                    putNullable(value, "testId", row.testId());
                    putNullable(value, "componentId", row.componentId());
                    putNullable(value, "callComponentId", row.callComponentId());
                });

        ArrayNode results = canonical.putArray("results");
        draft.results().stream()
                .sorted(Comparator.comparing(AnalyzerMappingResultDraft::sourceRowKey)
                        .thenComparing(AnalyzerMappingResultDraft::subIdentity)
                        .thenComparing(AnalyzerMappingResultDraft::rawValue))
                .forEach(row -> {
                    ObjectNode value = results.addObject();
                    value.put("sourceRowKey", row.sourceRowKey());
                    value.put("subIdentity", row.subIdentity());
                    value.put("rawValue", row.rawValue());
                    value.put("mappingState", row.mappingState().name());
                    value.put("origin", row.origin().name());
                    putNullable(value, "testResultId", row.testResultId());
                });

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new LIMSRuntimeException("SHA-256 is unavailable", e);
        }
    }

    private static void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }
}
