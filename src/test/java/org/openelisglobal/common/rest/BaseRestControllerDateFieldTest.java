package org.openelisglobal.common.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.sql.Date;
import java.util.Map;
import org.junit.Test;

public class BaseRestControllerDateFieldTest {

    private final BaseRestController controller = new BaseRestController();

    @Test
    public void readsValidIsoDate() {
        assertEquals(Date.valueOf("2028-02-29"), controller.dateField(Map.of("d", "2028-02-29"), "d"));
    }

    @Test
    public void blankIsNull() {
        assertNull(controller.dateField(Map.of("d", " "), "d"));
    }

    @Test
    public void rejectsFebruary30() {
        assertRejected("2026-02-30");
    }

    @Test
    public void rejectsFebruary29InNonLeapYear() {
        assertRejected("2027-02-29");
    }

    @Test
    public void rejectsApril31() {
        assertRejected("2026-04-31");
    }

    private void assertRejected(String value) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> controller.dateField(Map.of("expirationDate", value), "expirationDate"));
        assertEquals("expirationDate must be an ISO date (yyyy-MM-dd)", e.getMessage());
    }
}
