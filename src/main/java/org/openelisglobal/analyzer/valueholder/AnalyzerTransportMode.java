package org.openelisglobal.analyzer.valueholder;

import java.util.Locale;

/** Site-selected transport initialized from the pinned analyzer profile. */
public enum AnalyzerTransportMode {
    TCP, MLLP, SERIAL, FILE, HTTP;

    public static AnalyzerTransportMode fromProfileValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
        case "TCP", "TCP/IP" -> TCP;
        case "MLLP" -> MLLP;
        case "RS-232", "RS232", "SERIAL" -> SERIAL;
        case "FILE" -> FILE;
        case "HTTP", "HTTPS" -> HTTP;
        default -> null;
        };
    }
}
