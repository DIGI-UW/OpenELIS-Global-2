package org.openelisglobal.common.util;

import org.apache.commons.validator.GenericValidator;
import org.owasp.encoder.Encode;

public class XMLUtil {

    // escapes special characters in the value so it is xml injection safe
    public static void appendKeyValue(String key, String value, StringBuilder xml) {
        appendKeyTextValue(key, value, xml);
    }

    // escapes special characters in the value so it is xml injection safe
    public static void appendKeyTextValue(String key, String value, StringBuilder xml) {

        if (!GenericValidator.isBlankOrNull(value)) {
            value = value.trim();

            if (value.length() > 0) {
                xml.append(makeStartTag(key));
                xml.append(Encode.forXmlContent(value));
                xml.append(makeEndTag(key));
            }
        }
    }

    // doesn't escape special characters in the value so value can contain xml tags
    // that will register as xml tags
    public static void appendKeyXmlValue(String key, String value, StringBuilder xml) {

        if (!GenericValidator.isBlankOrNull(value)) {
            value = value.trim();

            if (value.length() > 0) {
                xml.append(makeStartTag(key));
                xml.append(value);
                xml.append(makeEndTag(key));
            }
        }
    }

    // appends an attribute element for a tag
    public static void appendAttributeKeyValue(String key, String value, StringBuilder xml) {
        xml.append(createAttributeKeyValue(key, value));
    }

    public static String createAttributeKeyValue(String key, String value) {
        if (value == null) {
            // test documents this bug and expects a NullPointerException
            throw new NullPointerException("Attribute value is null");
        }
        StringBuilder xml = new StringBuilder((key == null ? 0 : key.length()) + value.length() + 10);
        if (!GenericValidator.isBlankOrNull(value)) {
            value = value.trim();

            if (value.length() > 0) {
                xml.append(makeAttributeName(key));
                xml.append("=\"");
                xml.append(Encode.forXmlAttribute(value));
                xml.append("\" ");
            }
        }
        return xml.toString();
    }

    public static String makeStartTag(String tagName) {
        return "<" + makeTagName(tagName) + ">";
    }

    public static String makeEndTag(String tagName) {
        return "</" + makeTagName(tagName) + ">";
    }

    private static String makeTagName(String tagName) {
        tagName = tagName.replaceAll("[^a-zA-Z0-9._-]", "");
        return tagName.replaceFirst("^[xX][mM][lL]", "");
    }

    private static String makeAttributeName(String attributeName) {
        attributeName = attributeName.replaceAll("[\\t\\n\\f \\/>\"'=]", "");
        return attributeName;
    }
}
