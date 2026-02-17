package org.openelisglobal.analyzer.valueholder;

import java.util.HashMap;
import java.util.Map;

/**
 * Canonical set of analyzer message-format protocols.
 *
 * <p>
 * This enum represents the <em>message format</em> an analyzer speaks (how to
 * parse/construct message content), NOT the transport mechanism (TCP, serial,
 * file). Transport is derived from configuration entities
 * ({@code FileImportConfiguration}, {@code SerialPortConfiguration}, or
 * {@code Analyzer.ipAddress}/{@code port}).
 *
 * <p>
 * Stored in the database via {@code @Enumerated(EnumType.STRING)}.
 */
public enum ProtocolVersion {

    /** ASTM / CLSI LIS2-A2 pipe-delimited record format. */
    ASTM_LIS2_A2("ASTM LIS2-A2"),

    /** HL7 v2.3.1 segment-based messaging. */
    HL7_V2_3_1("HL7 v2.3.1"),

    /** HL7 v2.5 segment-based messaging. */
    HL7_V2_5("HL7 v2.5");

    private final String label;

    /** Lookup map: upper-cased label/alias → enum constant. */
    private static final Map<String, ProtocolVersion> LOOKUP = new HashMap<>();

    static {
        for (ProtocolVersion pv : values()) {
            LOOKUP.put(pv.name(), pv);
            LOOKUP.put(pv.label.toUpperCase(), pv);
        }
        // Legacy aliases
        LOOKUP.put("LIS2-A2", ASTM_LIS2_A2);
        LOOKUP.put("ASTM", ASTM_LIS2_A2);
        LOOKUP.put("HL7", HL7_V2_3_1);
        LOOKUP.put("HL7 V2.3.1", HL7_V2_3_1);
        LOOKUP.put("HL7 V2.5", HL7_V2_5);
    }

    ProtocolVersion(String label) {
        this.label = label;
    }

    /** Human-readable display label (e.g. "ASTM LIS2-A2"). */
    public String getLabel() {
        return label;
    }

    /**
     * Resolve a protocol version from an enum name, label, or legacy alias.
     *
     * @param value the string to resolve (case-insensitive)
     * @return the matching {@code ProtocolVersion}, or {@code null} if unrecognized
     */
    public static ProtocolVersion fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LOOKUP.get(value.trim().toUpperCase());
    }

    /** True if this is an ASTM-family message format. */
    public boolean isAstm() {
        return this == ASTM_LIS2_A2;
    }

    /** True if this is an HL7 v2.x message format. */
    public boolean isHl7() {
        return this == HL7_V2_3_1 || this == HL7_V2_5;
    }
}
