package org.openelisglobal.reports.amendment.service;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.owasp.encoder.Encode;

/**
 * The amendment report reads a result's prior value from the same history XML
 * the audit trail reads, where each value is stored through
 * {@code Encode.forXmlContent}. It decodes exactly once.
 */
public class AmendmentReportServiceImplExtractPriorValueTest {

    private static String priorValue(String value) {
        byte[] changes = ("<value>" + Encode.forXmlContent(value) + "</value>").getBytes(StandardCharsets.UTF_8);
        return AmendmentReportServiceImpl.extractPriorValue(changes);
    }

    @Test
    public void extractPriorValue_decodesEncodedCharacters() {
        assertEquals("H&E", priorValue("H&E"));
        assertEquals("<5", priorValue("<5"));
        assertEquals(">100", priorValue(">100"));
    }

    @Test
    public void extractPriorValue_decodesOnceOnly() {
        assertEquals("H&amp;E", priorValue("H&amp;E"));
    }
}
