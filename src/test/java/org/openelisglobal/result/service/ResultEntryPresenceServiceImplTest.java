package org.openelisglobal.result.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

/**
 * OGC-1020 (FR-O3) — presence is advisory, session-bound, in-memory, and
 * self-expiring: an abandoned panel can never leave a ghost indicator or block
 * a colleague.
 */
public class ResultEntryPresenceServiceImplTest {

    @Test
    public void presence_isVisibleToOtherSessions_butNeverToOwn() {
        ResultEntryPresenceServiceImpl service = new ResultEntryPresenceServiceImpl();

        service.heartbeat("session-A", "Doe,Jane", "17");

        Map<String, String> seenByB = service.getPresence(Arrays.asList("17", "18"), "session-B");
        assertEquals("Doe,Jane", seenByB.get("17"));
        assertEquals(1, seenByB.size());

        Map<String, String> seenByA = service.getPresence(Arrays.asList("17", "18"), "session-A");
        assertTrue("a session never sees its own claim", seenByA.isEmpty());
    }

    @Test
    public void blankAnalysisHeartbeat_clearsTheSessionsClaim() {
        ResultEntryPresenceServiceImpl service = new ResultEntryPresenceServiceImpl();

        service.heartbeat("session-A", "Doe,Jane", "17");
        service.heartbeat("session-A", "Doe,Jane", null);

        assertTrue(service.getPresence(Arrays.asList("17"), "session-B").isEmpty());
    }

    @Test
    public void movingToAnotherRow_replacesTheClaim_oneClaimPerSession() {
        ResultEntryPresenceServiceImpl service = new ResultEntryPresenceServiceImpl();

        service.heartbeat("session-A", "Doe,Jane", "17");
        service.heartbeat("session-A", "Doe,Jane", "18");

        Map<String, String> seen = service.getPresence(Arrays.asList("17", "18"), "session-B");
        assertEquals(1, seen.size());
        assertEquals("Doe,Jane", seen.get("18"));
    }

    @Test
    public void clearSession_dropsTheClaimImmediately() {
        ResultEntryPresenceServiceImpl service = new ResultEntryPresenceServiceImpl();

        service.heartbeat("session-A", "Doe,Jane", "17");
        service.clearSession("session-A");

        assertTrue(service.getPresence(Arrays.asList("17"), "session-B").isEmpty());
    }

    @Test
    public void staleClaim_expiresWithoutAnyHeartbeat() throws InterruptedException {
        ResultEntryPresenceServiceImpl service = new ResultEntryPresenceServiceImpl(50L);

        service.heartbeat("session-A", "Doe,Jane", "17");
        Thread.sleep(80L);

        assertTrue("a dead session's claim evicts itself after the TTL",
                service.getPresence(Arrays.asList("17"), "session-B").isEmpty());
    }
}
