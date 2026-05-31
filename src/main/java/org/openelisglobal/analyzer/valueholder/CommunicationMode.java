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
 * All three modes are now functional: {@link #ANALYZER_INITIATED} (analyzer
 * pushes results, OE/bridge listens), {@link #LIS_INITIATED} (OE dispatches
 * ORM^O01 orders via the bridge — see
 * {@link org.openelisglobal.analyzer.service.AnalyzerOrderDispatchService}),
 * and {@link #BOTH} (concurrent push + dispatch).
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
     * OE/bridge initiates outbound connections to the analyzer. Implemented for
     * ORM^O01 via
     * {@link org.openelisglobal.analyzer.service.AnalyzerOrderDispatchService},
     * which POSTs a LOINC-coded order to the bridge's {@code /api/orders}; the
     * bridge translates LOINC → analyzer code and builds the wire message (ASTM via
     * its forwarder, HL7 via its outbound MLLP client). QRY^Q02 order download
     * (OGC-326) remains future work.
     */
    LIS_INITIATED("LIS → Analyzer"),

    /**
     * Both directions active. Analyzer can push results AND OE can initiate
     * queries/orders.
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
