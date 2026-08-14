package org.openelisglobal.analyzer.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

public final class AnalyzerSiteBindingFingerprint {

    private static final String FORMAT_VERSION = "analyzer-site-binding-v1";

    private AnalyzerSiteBindingFingerprint() {
    }

    public static String calculate(AnalyzerSiteBindingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("site binding draft is required");
        }
        MessageDigest digest = sha256();
        append(digest, FORMAT_VERSION);
        append(digest, draft.bridgeProfileId());
        append(digest, String.valueOf(draft.bridgeProfileRevision()));
        draft.tests().stream().sorted(Comparator.comparing(AnalyzerSiteBindingTestDraft::sourceRowKey,
                Comparator.nullsFirst(String::compareTo))).forEach(row -> append(digest, row));
        return "sha256:" + toHex(digest.digest());
    }

    private static void append(MessageDigest digest, AnalyzerSiteBindingTestDraft row) {
        append(digest, row.sourceRowKey());
        append(digest, row.rawAnalyzerCode());
        append(digest, String.valueOf(row.aliases().size()));
        row.aliases().forEach(alias -> append(digest, alias));
        append(digest, row.displayName());
        append(digest, row.resultType());
        append(digest, row.normalizedSystem());
        append(digest, row.normalizedCode());
        append(digest, row.mappingState() == null ? null : row.mappingState().name());
        append(digest, row.testId());
        append(digest, row.componentId());
    }

    private static void append(MessageDigest digest, String value) {
        if (value == null) {
            digest.update("-1:".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((bytes.length + ":").getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            hex.append(Character.forDigit(value & 0x0f, 16));
        }
        return hex.toString();
    }
}
