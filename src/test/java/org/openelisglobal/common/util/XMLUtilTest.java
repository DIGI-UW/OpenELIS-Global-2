package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class XMLUtilTest {

    @Test
    public void testAppendKeyTextValue_HappyPath() {
        StringBuilder xml = new StringBuilder();
        XMLUtil.appendKeyTextValue("name", "John Doe", xml);

        assertEquals("Should create simple tag", "<name>John Doe</name>", xml.toString());
    }

    @Test
    public void testAppendKeyTextValue_Escaping() {
        StringBuilder xml = new StringBuilder();
        String dangerousInput = "John & Jane <Script>";
        XMLUtil.appendKeyTextValue("data", dangerousInput, xml);

        String result = xml.toString();

        assertTrue("Should contain start tag", result.startsWith("<data>"));
        assertTrue("Should contain end tag", result.endsWith("</data>"));
        assertTrue("Should escape ampersand", result.contains("John &amp; Jane"));
    }

    @Test
    public void testAppendKeyTextValue_NullOrBlank() {
        StringBuilder xml = new StringBuilder();
        XMLUtil.appendKeyTextValue("key", null, xml);
        assertEquals("Should append nothing for null", "", xml.toString());

        XMLUtil.appendKeyTextValue("key", "", xml);
        assertEquals("Should append nothing for empty", "", xml.toString());

        XMLUtil.appendKeyTextValue("key", "   ", xml);
        assertEquals("Should append nothing for blank", "", xml.toString());
    }

    @Test
    public void testAppendKeyTextValue_Trimming() {
        StringBuilder xml = new StringBuilder();
        XMLUtil.appendKeyTextValue("key", "  value  ", xml);
        assertEquals("Should trim input", "<key>value</key>", xml.toString());
    }

    @Test
    public void testAppendKeyXmlValue_HappyPath() {
        StringBuilder xml = new StringBuilder();
        String rawXml = "<child>content</child>";
        XMLUtil.appendKeyXmlValue("parent", rawXml, xml);

        assertEquals("Should preserve inner XML", "<parent><child>content</child></parent>", xml.toString());
    }

    @Test
    public void testAppendKeyXmlValue_NullOrBlank() {
        StringBuilder xml = new StringBuilder();
        XMLUtil.appendKeyXmlValue("key", null, xml);
        assertEquals("", xml.toString());
    }

    @Test
    public void testCreateAttributeKeyValue_HappyPath() {
        String result = XMLUtil.createAttributeKeyValue("id", "123");
        assertEquals("id=\"123\" ", result);
    }

    @Test
    public void testCreateAttributeKeyValue_EscapingValue() {
        String result = XMLUtil.createAttributeKeyValue("title", "Book \"One\"");
        assertTrue("Should start with key", result.startsWith("title=\""));
        assertTrue(result.contains("&quot;") || result.contains("&#34;"));
    }

    @Test
    public void testCreateAttributeKeyValue_SanitizingKey() {
        String result = XMLUtil.createAttributeKeyValue("my attr", "val");
        assertEquals("Should strip space from key", "myattr=\"val\" ", result);
    }

    @Test
    public void testCreateAttributeKeyValue_Blank() {
        assertEquals("", XMLUtil.createAttributeKeyValue("key", "   "));
        assertEquals("", XMLUtil.createAttributeKeyValue("key", ""));
    }

    @Test
    public void testCreateAttributeKeyValue_Null() {
        assertEquals("", XMLUtil.createAttributeKeyValue("key", null));
    }

    @Test
    public void testMakeStartTag_Sanitization() {
        assertEquals("Should remove xml prefix", "<Data>", XMLUtil.makeStartTag("xmlData"));
        assertEquals("Should remove xml prefix", "<Data>", XMLUtil.makeStartTag("XMLData"));
        assertEquals("Should remove invalid chars", "<tagname>", XMLUtil.makeStartTag("tag name!"));
    }

    @Test
    public void testMakeEndTag() {
        assertEquals("</test>", XMLUtil.makeEndTag("test"));
        assertEquals("</Data>", XMLUtil.makeEndTag("xmlData"));
    }

    @Test
    public void testConstructor() {
        assertNotNull(new XMLUtil());
    }

    @Test
    public void testAppendKeyValue_Wrapper() {
        StringBuilder xml = new StringBuilder();
        XMLUtil.appendKeyValue("wrapper", "content", xml);

        assertEquals("<wrapper>content</wrapper>", xml.toString());
    }

    @Test
    public void testAppendAttributeKeyValue_Wrapper() {
        StringBuilder xml = new StringBuilder();
        xml.append("<tag "); // simulate being inside a tag
        XMLUtil.appendAttributeKeyValue("id", "999", xml);
        xml.append("/>");
        assertEquals("<tag id=\"999\" />", xml.toString());
    }
}