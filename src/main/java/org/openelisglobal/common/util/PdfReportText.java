package org.openelisglobal.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Plain PDF text from the report builders' escaped/styled text, retaining line
 * breaks.
 */
public final class PdfReportText {
    private PdfReportText() {
    }

    public static String plain(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        append(Jsoup.parseBodyFragment(value).body(), text);
        return text.toString();
    }

    private static void append(Node node, StringBuilder text) {
        if (node instanceof TextNode) {
            text.append(((TextNode) node).getWholeText());
            return;
        }
        if (node instanceof Element && ((Element) node).normalName().equals("br")) {
            text.append('\n');
            return;
        }
        for (Node child : node.childNodes()) {
            append(child, text);
        }
    }
}
