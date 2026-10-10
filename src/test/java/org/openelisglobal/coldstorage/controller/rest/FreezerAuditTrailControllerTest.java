package org.openelisglobal.coldstorage.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionType;

public class FreezerAuditTrailControllerTest {

    private static Map<String, Object> eventAt(String id, String performedAt) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("performedAt", performedAt);
        return event;
    }

    private static List<String> idsInOrder(List<Map<String, Object>> events) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> event : events) {
            ids.add((String) event.get("id"));
        }
        return ids;
    }

    @Test
    public void testMostRecentFirst_WithMixedOffsets_OrdersByInstantNotByText() {
        // 13:30+01:00 is the later instant but sorts first as text.
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(eventAt("earlier", "2026-10-25T14:00:00+02:00"));
        events.add(eventAt("later", "2026-10-25T13:30:00+01:00"));

        events.sort(FreezerAuditTrailController.MOST_RECENT_FIRST);

        assertEquals("newest instant must come first", List.of("later", "earlier"), idsInOrder(events));
    }

    @Test
    public void testMostRecentFirst_WithOneOffset_StillOrdersNewestFirst() {
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(eventAt("older", "2026-09-08T09:00:00+03:00"));
        events.add(eventAt("newest", "2026-09-08T11:00:00+03:00"));
        events.add(eventAt("middle", "2026-09-08T10:00:00+03:00"));

        events.sort(FreezerAuditTrailController.MOST_RECENT_FIRST);

        assertEquals(List.of("newest", "middle", "older"), idsInOrder(events));
    }

    @Test
    public void testBuildCorrectiveActionDetails_RendersCompletedAtForReading() {
        OffsetDateTime completedAt = OffsetDateTime.parse("2026-09-08T13:24:11.411912+03:00");
        CorrectiveAction action = new CorrectiveAction();
        action.setActionType(CorrectiveActionType.CALIBRATION);
        action.setStatus(CorrectiveActionStatus.COMPLETED);
        action.setCompletedAt(completedAt);

        String details = new FreezerAuditTrailController().buildCorrectiveActionDetails(action);

        String expected = completedAt.atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        assertTrue("details should carry the readable timestamp but were: " + details,
                details.contains("Completed: " + expected + "\n"));
        assertFalse("details must not expose the raw ISO value: " + details, details.contains(completedAt.toString()));
    }
}
