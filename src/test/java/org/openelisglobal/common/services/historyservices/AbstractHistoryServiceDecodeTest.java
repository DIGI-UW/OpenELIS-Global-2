package org.openelisglobal.common.services.historyservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.owasp.encoder.Encode;
import sun.misc.Unsafe;

/**
 * History rows store each prior value as {@code <attribute>} +
 * {@code Encode.forXmlContent(value)} + {@code </attribute>}. The shared reader
 * must hand back the value as the user typed it, decoded exactly once, so the
 * audit trail never prints {@code H&amp;E} or {@code &lt;5} and an unchanged
 * value never shows up as a change.
 *
 * <p>
 * The service constructors call {@code SpringContext.getBean}, so instances are
 * made with {@code Unsafe.allocateInstance}, as in
 * {@code PatientHistoryServiceTest}.
 */
public class AbstractHistoryServiceDecodeTest {

    /**
     * Skips the user lookup and message bundle that {@code getCoreTrail} and the
     * attribute label need, which are not under test here.
     */
    static class PlainTrailPatientHistoryService extends PatientHistoryService {
        // never run: instances come from Unsafe.allocateInstance
        PlainTrailPatientHistoryService() {
            super(null);
        }

        @Override
        protected AuditTrailItem getCoreTrail(History history) {
            return new AuditTrailItem();
        }

        @Override
        protected boolean showAttribute() {
            return true;
        }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe u = (Unsafe) f.get(null);
        return type.cast(u.allocateInstance(type));
    }

    private static String stored(String attribute, String value) {
        return "<" + attribute + ">" + Encode.forXmlContent(value) + "</" + attribute + ">";
    }

    private String extract(String changes, String attribute) throws Exception {
        return allocate(PatientHistoryService.class).extractSimple(changes, attribute);
    }

    @Test
    public void extractSimple_decodesAmpersand() throws Exception {
        assertEquals("H&E", extract(stored("value", "H&E"), "value"));
    }

    @Test
    public void extractSimple_decodesLessThan() throws Exception {
        assertEquals("<5", extract(stored("value", "<5"), "value"));
    }

    @Test
    public void extractSimple_decodesGreaterThan() throws Exception {
        assertEquals(">100", extract(stored("value", ">100"), "value"));
    }

    @Test
    public void extractSimple_keepsDoubleQuote() throws Exception {
        assertEquals("say \"hi\"", extract(stored("value", "say \"hi\""), "value"));
        assertEquals("say \"hi\"", extract("<value>say &quot;hi&quot;</value>", "value"));
    }

    @Test
    public void extractSimple_keepsSingleQuote() throws Exception {
        assertEquals("O'Brien", extract(stored("lastName", "O'Brien"), "lastName"));
        assertEquals("O'Brien", extract("<lastName>O&apos;Brien</lastName>", "lastName"));
    }

    @Test
    public void extractSimple_decodesOnlyOnce() throws Exception {
        String changes = stored("value", "&amp;");
        assertEquals("<value>&amp;amp;</value>", changes);
        assertEquals("&amp;", extract(changes, "value"));
    }

    @Test
    public void extractSimple_plainValueUnchanged() throws Exception {
        String changes = stored("lastName", "Smith");
        assertEquals("Smith", extract(changes, "lastName"));
        assertNull(extract(changes, "firstName"));
    }

    @Test
    public void addItemsForKeys_unchangedValueWithAmpersandMakesNoRow() throws Exception {
        PlainTrailPatientHistoryService svc = allocate(PlainTrailPatientHistoryService.class);
        Map<String, String> changeMap = new HashMap<>();
        svc.getObservableChanges(new History(), changeMap, stored("firstName", "Tom & Jerry"));
        svc.newValueMap = new HashMap<>();
        svc.newValueMap.put("firstName", "Tom & Jerry");

        List<AuditTrailItem> items = new ArrayList<>();
        svc.addItemsForKeys(items, new History(), changeMap);

        assertTrue("an unchanged value must not make a row; got " + items.size(), items.isEmpty());
    }

    @Test
    public void addItemsForKeys_changedValuePrintsDecodedOld() throws Exception {
        PlainTrailPatientHistoryService svc = allocate(PlainTrailPatientHistoryService.class);
        Map<String, String> changeMap = new HashMap<>();
        svc.getObservableChanges(new History(), changeMap, stored("firstName", "Tom & Jerry"));
        svc.newValueMap = new HashMap<>();
        svc.newValueMap.put("firstName", "Smith");

        List<AuditTrailItem> items = new ArrayList<>();
        svc.addItemsForKeys(items, new History(), changeMap);

        assertEquals(1, items.size());
        assertEquals("Tom & Jerry", items.get(0).getOldValue());
        assertEquals("Smith", items.get(0).getNewValue());
    }
}
