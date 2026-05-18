package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Builds ASTM LIS2-A2 order messages (H / P / O / L records) for outbound
 * dispatch from OE2 to an analyzer via the bridge.
 *
 * <p>
 * The bridge handles per-frame STX/ETX/checksum/FN framing when shipping over
 * TCP — this builder produces only the record-level content with CR (0x0D)
 * separators, suitable as the body of a POST to the bridge's existing generic
 * forwarder.
 *
 * <p>
 * Record layout per LIS2-A2 §7:
 * <ul>
 * <li><code>H|\^&amp;|||OpenELIS^Order^1.0|||||||LIS2-A2</code> — header
 * <li><code>P|1|||&lt;patientId&gt;</code> — patient
 * <li><code>O|&lt;seq&gt;|&lt;accession&gt;||^^^&lt;testCode&gt;|R</code> — one
 * per test code
 * <li><code>L|1|N</code> — terminator
 * </ul>
 *
 * <p>
 * The accession is placed in O field 3 (specimen ID); analyzers and the mock
 * alike echo it verbatim on the response, and OE2's existing accession- keyed
 * inbound result import correlates results back to the originating sample on
 * that basis.
 */
@Component
public class ASTMOrderBuilder {

    private static final String CR = "\r";
    private static final String HEADER = "H|\\^&|||OpenELIS^Order^1.0|||||||LIS2-A2";
    private static final String TERMINATOR = "L|1|N";

    public String build(String accession, String patientId, List<String> testCodes) {
        Objects.requireNonNull(accession, "accession");
        Objects.requireNonNull(testCodes, "testCodes");
        if (accession.isBlank()) {
            throw new IllegalArgumentException("accession cannot be blank");
        }
        if (testCodes.isEmpty()) {
            throw new IllegalArgumentException("at least one testCode is required");
        }

        String safePatientId = patientId == null ? "" : patientId;

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(CR);
        sb.append("P|1|||").append(safePatientId).append(CR);

        int orderSeq = 1;
        for (String testCode : testCodes) {
            if (testCode == null || testCode.isBlank()) {
                continue;
            }
            // Field 1: O · Field 2: seq · Field 3: accession · Field 4: '' ·
            // Field 5: universal test ID '^^^<code>' · Field 6: priority R
            sb.append("O|").append(orderSeq).append("|").append(accession).append("||^^^").append(testCode).append("|R")
                    .append(CR);
            orderSeq += 1;
        }

        if (orderSeq == 1) {
            throw new IllegalArgumentException("at least one non-blank testCode is required");
        }

        sb.append(TERMINATOR).append(CR);
        return sb.toString();
    }
}
