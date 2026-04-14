package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.service.NceHistoryService;
import org.openelisglobal.qaevent.valueholder.NceHistory;
import org.springframework.beans.factory.annotation.Autowired;

public class NceHistoryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NceHistoryService nceHistoryService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nce-history.xml");
    }

    @Test
    public void findByNceId_shouldReturnHistoryEntriesForGivenNce() {
        List<NceHistory> history = nceHistoryService.findByNceId(2001);
        assertNotNull(history);
        assertEquals(3, history.size());
        // Should be ordered by timestamp DESC
        assertEquals("ACKNOWLEDGED", history.get(0).getActivity());
        assertEquals("NOTE_ADDED", history.get(1).getActivity());
        assertEquals("CREATED", history.get(2).getActivity());
    }

    @Test
    public void findByNceId_shouldReturnSingleEntryForNceWithOneHistory() {
        List<NceHistory> history = nceHistoryService.findByNceId(2002);
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("CREATED", history.get(0).getActivity());
    }

    @Test
    public void findByNceId_shouldReturnEmptyListForNonExistentNce() {
        List<NceHistory> history = nceHistoryService.findByNceId(9999);
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    public void logActivity_shouldCreateHistoryEntryWithTimestamp() {
        NceHistory history = nceHistoryService.logActivity(2001, "NOTE_ADDED", "Test note from unit test", null, null,
                1);
        assertNotNull(history);
        assertNotNull(history.getId());
        assertNotNull(history.getTimestamp());
        assertEquals(Integer.valueOf(2001), history.getNceId());
        assertEquals("NOTE_ADDED", history.getActivity());
        assertEquals("Test note from unit test", history.getDescription());
        assertEquals(Integer.valueOf(1), history.getUserId());
    }

    @Test
    public void logActivity_shouldHandleNullUserId() {
        NceHistory history = nceHistoryService.logActivity(2001, "ACKNOWLEDGED", "System acknowledgment", null, null,
                null);
        assertNotNull(history);
        assertNotNull(history.getId());
        assertEquals("ACKNOWLEDGED", history.getActivity());
        // sysUserId is auto-stamped from SecurityContext by AuditContextAdvice
        assertNotNull(history.getSysUserId());
    }

    @Test
    public void logActivity_shouldPersistAndBeRetrievable() {
        nceHistoryService.logActivity(2002, "NOTE_ADDED", "Persisted note", null, null, 1);

        List<NceHistory> history = nceHistoryService.findByNceId(2002);
        assertNotNull(history);
        // Original CREATED + new NOTE_ADDED
        assertEquals(2, history.size());
        // DESC order: newest first
        assertEquals("NOTE_ADDED", history.get(0).getActivity());
        assertEquals("Persisted note", history.get(0).getDescription());
    }

    @Test
    public void logActivity_shouldStoreOldAndNewValues() {
        NceHistory history = nceHistoryService.logActivity(2001, "STATUS_CHANGED", "Status updated", "Pending",
                "Under Investigation", 1);
        assertNotNull(history);
        assertEquals("Pending", history.getOldValue());
        assertEquals("Under Investigation", history.getNewValue());
    }
}
