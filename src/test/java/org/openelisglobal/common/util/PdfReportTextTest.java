package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A result is stored as typed, not escaped, so a "<" in it is text: "<LOD" read
 * as an unknown tag printed an empty result cell.
 */
public class PdfReportTextTest {

    @Test
    public void aLessThanThatOpensNoFormattingTagIsText() {
        assertEquals("<LOD", PdfReportText.plain("<LOD"));
        assertEquals("Neg <Detected>", PdfReportText.plain("Neg <Detected>"));
        assertEquals("a<b", PdfReportText.plain("a<b"));
        assertEquals("<20", PdfReportText.plain("<20"));
    }

    @Test
    public void formattingAndEscapedTextStillRead() {
        assertEquals("Positive\nsee note", PdfReportText.plain("Positive<br/>see note"));
        assertEquals("bold text", PdfReportText.plain("<b>bold</b> text"));
        assertEquals("x < y", PdfReportText.plain("x &lt; y"));
        assertEquals("10^3/µl", PdfReportText.plain("10^3/&micro;l"));
    }
}
