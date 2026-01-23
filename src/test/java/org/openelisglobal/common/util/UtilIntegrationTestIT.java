package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilIntegrationTestIT {
    @Test
    public void integerAndXml_integrationProducesWellFormedEncodedXml() {
        int original = 12345;
        String base27 = IntegerUtil.toStringBase27(original);
        int parsed = IntegerUtil.parseIntBase27(base27);
        assertEquals("hi", original, parsed);


        StringBuilder xml = new StringBuilder();
        xml.append("<person ");
        xml.append(XMLUtil.createAttributeKeyValue("id", base27));
        xml.append(">");

        XMLUtil.appendKeyTextValue("name", "<John & Jane>", xml);
        XMLUtil.appendKeyXmlValue("bio", "<p>Test</p>", xml);

        xml.append(XMLUtil.makeEndTag("person"));

        String out = xml.toString();

        assertTrue("Output should start with person start tag", out.startsWith("<person "));
        assertTrue("Output should end with person end tag", out.endsWith("</person>"));
        assertTrue("Output should contain id attribute", out.contains("id=\""));

        assertTrue("Text content should be encoded", out.contains("&lt;John") || out.contains("&amp;"));

        assertTrue("XML content should be preserved", out.contains("<p>Test</p>"));
    }
}
