package org.openelisglobal.biorepository.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class Brf02SamplePathFormatterTest {

    @Test
    public void format_buildsFullBrf02PathFromStructuredFields() {
        Map<String, Object> location = new HashMap<>();
        location.put("roomName", "Room-A");
        location.put("deviceName", "Freezer-1");
        location.put("shelfLabel", "S2");
        location.put("rackLabel", "R15");
        location.put("boxLabel", "BX078");
        location.put("positionCoordinate", "B3");

        assertEquals("Zn Room-A / FRZ Freezer-1 / SH S2 / RK R15 / Box BX078 / Pos B3",
                Brf02SamplePathFormatter.format(location));
    }

    @Test
    public void formatFromHierarchicalPath_parsesRoomToBoxHierarchy() {
        assertEquals("Zn Room-A / FRZ Freezer-1 / SH S2 / RK R15 / Box BX078 / Pos B3",
                Brf02SamplePathFormatter.formatFromHierarchicalPath(
                        "Room-A > Freezer-1 > S2 > R15 > BX078", "B3"));
    }

    @Test
    public void format_returnsNullForEmptyLocation() {
        assertNull(Brf02SamplePathFormatter.format(null));
        assertNull(Brf02SamplePathFormatter.format(Map.of()));
    }
}
