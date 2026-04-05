package org.openelisglobal.analyzer.valueholder;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes who initiates communication between the LIS and an analyzer.
 *
 * <p>
 * This is orthogonal to {@link ProtocolVersion} (message format) and transport
 * mechanism (TCP, serial, file). It determines how test-connection results are
 * interpreted and what communication capabilities the analyzer supports.
 *
 * <p>
 * MVP implements {@link #ANALYZER_INITIATED} only (one-way result transport).
 * {@link #LIS_INITIATED} and {@link #BOTH} are planned post-MVP capabilities
 * documented in vendor specs (OGC-327 ORM^O01, OGC-326 QRY^Q02, OGC-336 QBP
 * queries). The field is exposed in the UI so sites can declare their
 * configuration intent even before OE fully supports outbound communication.
 *
 * <p>
 * Stored in the database via {@code @Enumerated(EnumType.STRING)}.
 */
public enum CommunicationMode {

    /**
     * Analyzer initiates all communication. OE/bridge listens. Includes in-session
     * responses (ASTM Q-segment, HL7 ACK, QBP→RSP). All current analyzers use this
     * mode.
     */
    ANALYZER_INITIATED("Analyzer → LIS"),

    /**
     * OE/bridge initiates outbound connections to the analyzer. Future: ORM^O01
     * worklist download (OGC-327), QRY^Q02 order download (OGC-326). Requires
     * bridge outbound MLLP/ASTM client support.
     */
    LIS_INITIATED("LIS → Analyzer"),

    /**
     * Both directions active. Analyzer can push results AND OE can initiate
     * queries/orders. Requires bridge outbound support.
     */
    BOTH("Bidirectional");

    private final String label;

    private static final Map<String, CommunicationMode> LOOKUP = new HashMap<>();

    static {
        for (CommunicationMode cm : values()) {
            LOOKUP.put(cm.name(), cm);
            LOOKUP.put(cm.label.toUpperCase(), cm);
        }
        // Convenience aliases
        LOOKUP.put("PUSH", ANALYZER_INITIATED);
        LOOKUP.put("QUERY", LIS_INITIATED);
        LOOKUP.put("BIDIRECTIONAL", BOTH);
    }

    CommunicationMode(String label) {
        this.label = label;
    }

    /** Human-readable display label. */
    public String getLabel() {
        return label;
    }

    /**
     * Resolve from an enum name, label, or alias (case-insensitive).
     *
     * @return the matching mode, or {@code null} if unrecognized
     */
    public static CommunicationMode fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LOOKUP.get(value.trim().toUpperCase());
    }
}
