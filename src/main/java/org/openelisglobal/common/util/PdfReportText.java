package org.openelisglobal.common.util;

import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Plain PDF text from the report builders' escaped/styled text, retaining line
 * breaks. Values such as results are stored as typed, so a "<" that does not
 * open a formatting tag ("<LOD", "Neg <Detected>") is kept as text rather than
 * read as markup and dropped.
 */
public final class PdfReportText {
    private static final Pattern STRAY_LESS_THAN = Pattern
            .compile("<(?!/?(?:br|b|i|u|p|em|strong|sup|sub|span|font|div)\\b[^<>]*>)", Pattern.CASE_INSENSITIVE);

    private PdfReportText() {
    }

    public static String plain(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        append(Jsoup.parseBodyFragment(STRAY_LESS_THAN.matcher(value).replaceAll("&lt;")).body(), text);
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
