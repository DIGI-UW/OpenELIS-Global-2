package org.openelisglobal.common.services.historyservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import sun.misc.Unsafe;

/**
 * Locks the display-attribute extraction in PatientHistoryService against
 * canned {@code changes} XML in the format produced by
 * {@code AuditTrailServiceImpl.getChanges}. Each declared attribute must appear
 * in the extracted change map when its tag is present and be absent otherwise.
 *
 * <p>
 * The class's constructor calls {@code SpringContext.getBean} to wire
 * dependencies, so tests instantiate via {@code Unsafe.allocateInstance} to
 * skip the constructor and exercise {@code getObservableChanges} directly.
 */
public class PatientHistoryServiceTest {

    private PatientHistoryService allocate() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe u = (Unsafe) f.get(null);
        return (PatientHistoryService) u.allocateInstance(PatientHistoryService.class);
    }

    private Map<String, String> extract(String changesXml) throws Exception {
        PatientHistoryService svc = allocate();
        Map<String, String> changeMap = new HashMap<>();
        Method m = PatientHistoryService.class.getDeclaredMethod("getObservableChanges", History.class, Map.class,
                String.class);
        m.setAccessible(true);
        // History argument is unused by getObservableChanges so null is fine.
        m.invoke(svc, null, changeMap, changesXml);
        return changeMap;
    }

    @Test
    public void getObservableChanges_extractsAllDeclaredAttributes() throws Exception {
        String xml = "<birthDateForDisplay>1990-01-01</birthDateForDisplay>" + "<gender>M</gender>"
                + "<nationalId>NID-OLD</nationalId>" + "<externalId>EXT-OLD</externalId>"
                + "<firstName>Alice</firstName>" + "<lastName>Smith</lastName>" + "<email>alice@example.com</email>"
                + "<primaryPhone>+261 37 11 111 11</primaryPhone>" + "<gpsLatitude>-18.879190</gpsLatitude>"
                + "<gpsLongitude>47.507905</gpsLongitude>";

        Map<String, String> changes = extract(xml);

        assertEquals("1990-01-01", changes.get("birthDateForDisplay"));
        assertEquals("M", changes.get("gender"));
        assertEquals("NID-OLD", changes.get("nationalId"));
        assertEquals("EXT-OLD", changes.get("externalId"));
        assertEquals("Alice", changes.get("firstName"));
        assertEquals("Smith", changes.get("lastName"));
        assertEquals("alice@example.com", changes.get("email"));
        assertEquals("+261 37 11 111 11", changes.get("primaryPhone"));
        assertEquals("-18.879190", changes.get("gpsLatitude"));
        assertEquals("47.507905", changes.get("gpsLongitude"));
    }

    @Test
    public void getObservableChanges_partialXmlOnlyExtractsPresentAttributes() throws Exception {
        // Only phone + GPS changed — the rest of the form was untouched.
        String xml = "<primaryPhone>+261 37 22 222 22</primaryPhone>" + "<gpsLatitude>-18.5</gpsLatitude>"
                + "<gpsLongitude>47.5</gpsLongitude>";

        Map<String, String> changes = extract(xml);

        assertEquals("+261 37 22 222 22", changes.get("primaryPhone"));
        assertEquals("-18.5", changes.get("gpsLatitude"));
        assertEquals("47.5", changes.get("gpsLongitude"));
        assertNull("firstName must not appear when not in XML", changes.get("firstName"));
        assertNull("email must not appear when not in XML", changes.get("email"));
        assertEquals("Only 3 changed attributes should be extracted", 3, changes.size());
    }

    @Test
    public void getObservableChanges_emailUpdateIsTracked() throws Exception {
        // Regression: email is one of the attrs the patient audit trail must surface.
        String xml = "<email>old@example.com</email>";

        Map<String, String> changes = extract(xml);

        assertEquals("old@example.com", changes.get("email"));
        assertEquals(1, changes.size());
    }

    @Test
    public void getObservableChanges_emptyXmlProducesEmptyMap() throws Exception {
        Map<String, String> changes = extract("");
        assertTrue("Empty changes XML must produce empty changeMap; got: " + changes, changes.isEmpty());
    }

    @Test
    public void getObservableChanges_ignoresUnknownTags() throws Exception {
        // Tags for attributes we don't track (e.g. internal cache columns) must
        // not pollute the changeMap.
        String xml = "<primaryPhone>+261 37 99 999 99</primaryPhone>" + "<__internalFlag>x</__internalFlag>"
                + "<lastupdatedCache>abc</lastupdatedCache>";

        Map<String, String> changes = extract(xml);

        assertEquals("+261 37 99 999 99", changes.get("primaryPhone"));
        assertNull(changes.get("__internalFlag"));
        assertNull(changes.get("lastupdatedCache"));
        assertEquals(1, changes.size());
    }
}
