package org.openelisglobal.audittrail.controller.rest;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.owasp.encoder.Encode;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * History rows store each prior value as {@code <field>} +
 * {@code Encode.forXmlContent(value)} + {@code </field>}. The system audit
 * events reader must return each value decoded exactly once.
 */
public class SystemAuditEventRestControllerParseChangesTest {

    private static String stored(String field, String value) {
        return "<" + field + ">" + Encode.forXmlContent(value) + "</" + field + ">";
    }

    private Map<String, String> parse(String changes) {
        History history = new History();
        history.setActivity("U");
        history.setChanges(changes.getBytes(StandardCharsets.UTF_8));
        return ReflectionTestUtils.invokeMethod(new SystemAuditEventRestController(), "parseChanges", history);
    }

    @Test
    public void parseChanges_decodesAmpersand() {
        assertEquals("H&E", parse(stored("name", "H&E")).get("name"));
    }

    @Test
    public void parseChanges_decodesLessThan() {
        assertEquals("<5", parse(stored("value", "<5")).get("value"));
    }

    @Test
    public void parseChanges_decodesGreaterThan() {
        assertEquals(">100", parse(stored("value", ">100")).get("value"));
    }

    @Test
    public void parseChanges_decodesOnlyOnce() {
        String changes = stored("value", "&amp;");
        assertEquals("<value>&amp;amp;</value>", changes);
        assertEquals("&amp;", parse(changes).get("value"));
    }

    @Test
    public void parseChanges_plainValuesUnchangedAcrossFields() {
        Map<String, String> changes = parse(stored("firstName", "Tom & Jerry") + stored("lastName", "Smith"));
        assertEquals(2, changes.size());
        assertEquals("Tom & Jerry", changes.get("firstName"));
        assertEquals("Smith", changes.get("lastName"));
    }
}
