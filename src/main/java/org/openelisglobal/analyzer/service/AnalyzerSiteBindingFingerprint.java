package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public final class AnalyzerSiteBindingFingerprint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private AnalyzerSiteBindingFingerprint() {
    }

    public static String calculate(AnalyzerSiteBindingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Site binding draft is required");
        }

        ObjectNode canonical = JSON.createObjectNode();
        ArrayNode tests = canonical.putArray("tests");
        draft.tests().stream().sorted(Comparator.comparing(AnalyzerSiteBindingTestDraft::sourceRowKey)).forEach(row -> {
            ObjectNode value = tests.addObject();
            value.put("sourceRowKey", row.sourceRowKey());
            value.put("mappingState", row.mappingState().name());
            putNullable(value, "testId", row.testId());
        });

        ArrayNode results = canonical.putArray("results");
        draft.results().stream().sorted(Comparator.comparing(AnalyzerSiteBindingResultDraft::sourceRowKey)
                .thenComparing(AnalyzerSiteBindingResultDraft::rawValue)).forEach(row -> {
                    ObjectNode value = results.addObject();
                    value.put("sourceRowKey", row.sourceRowKey());
                    value.put("rawValue", row.rawValue());
                    value.put("mappingState", row.mappingState().name());
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
