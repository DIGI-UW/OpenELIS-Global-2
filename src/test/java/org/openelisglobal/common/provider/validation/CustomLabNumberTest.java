package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;

public class CustomLabNumberTest {

    @Test
    public void testTemplateTokenEngine_DateTokens() {
        Date now = new Date();
        String yyyy = new SimpleDateFormat("yyyy").format(now);
        String mm = new SimpleDateFormat("MM").format(now);

        String template = "LAB-{YYYY}-{MM}";
        String expected = "LAB-" + yyyy + "-" + mm;

        assertEquals(expected, TemplateTokenEngine.processTokens(template, 0, "TEST"));
    }

    @Test
    public void testTemplateTokenEngine_SequenceTokens() {
        String template = "SEQ-{SEQ:6}";
        String expected = "SEQ-000123";

        assertEquals(expected, TemplateTokenEngine.processTokens(template, 123, "TEST"));
    }

    @Test
    public void testTemplateTokenEngine_AlphaNumTokens() {
        String template = "ALPHA-{ALPHANUMSEQ:3}";
        // 27 in base 27 is "10"
        assertEquals("ALPHA-010", TemplateTokenEngine.processTokens(template, 27, "TEST"));
    }

    @Test
    public void testTemplateTokenEngine_StaticPrefix() {
        Date now = new Date();
        String yyyy = new SimpleDateFormat("yyyy").format(now);

        String template = "LAB-{YYYY}-{SEQ:6}";
        String expected = "LAB-" + yyyy + "-";

        assertEquals(expected, TemplateTokenEngine.getStaticPrefix(template));
    }
}
