package org.openelisglobal.taglib;

import static org.junit.Assert.*;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;

public class OutputEncodingFunctionsTest {
    private String encoded(String context, String value) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder()
                .parse(Path.of("src/main/webapp/WEB-INF/taglib/output-encoding.tld").toFile());
        var functions = document.getElementsByTagName("function");
        for (int i = 0; i < functions.getLength(); i++) {
            Element function = (Element) functions.item(i);
            if (function.getElementsByTagName("name").item(0).getTextContent().equals(context)) {
                String className = function.getElementsByTagName("function-class").item(0).getTextContent();
                String signature = function.getElementsByTagName("function-signature").item(0).getTextContent();
                String method = signature.substring(signature.indexOf(' ') + 1, signature.indexOf('('));
                return (String) Class.forName(className).getMethod(method, String.class).invoke(null, value);
            }
        }
        throw new AssertionError("Missing output context " + context);
    }

    @Test
    public void htmlAndAttributeFunctionsPreserveTextWithoutCreatingElements() throws Exception {
        String payload = "O'Brien \"</textarea><img src=x onerror=alert(1)> &";
        String html = encoded("html", payload);
        String attribute = encoded("attribute", payload);
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(("<root data=\"" + attribute + "\">" + html + "</root>")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(payload, document.getDocumentElement().getAttribute("data"));
        assertEquals(payload, document.getDocumentElement().getTextContent());
        assertEquals(0, document.getElementsByTagName("img").getLength());
    }

    @Test
    public void scriptFunctionsCannotTerminateTheStringOrScriptElement() throws Exception {
        String payload = "'\";window.injected=true;//</script>\n";
        for (String context : java.util.List.of("js", "jsAttribute")) {
            String result = encoded(context, payload);
            if ("js".equals(context)) {
                assertFalse("A script-body string cannot close its HTML element", result.contains("</script>"));
            }
            for (int i = 0; i < result.length(); i++) {
                char character = result.charAt(i);
                if (character == '\\') {
                    assertTrue("Complete JavaScript escape", ++i < result.length());
                    continue;
                }
                assertFalse("No unescaped string delimiter", character == '\'' || character == '\"');
                assertFalse("No raw line break", character == '\n' || character == '\r');
            }
        }
    }
}
