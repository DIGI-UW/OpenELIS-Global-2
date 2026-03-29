package org.openelisglobal.odoo;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.odoo.dao.OdooSyncQueueDAO;
import org.openelisglobal.odoo.service.OdooSyncQueueService;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue.SyncStatus;

@RunWith(MockitoJUnitRunner.class)
public class OdooSyncQueueServiceTest {

    @Mock
    private OdooSyncQueueDAO odooSyncQueueDAO;

    @InjectMocks
    private OdooSyncQueueService odooSyncQueueService;

    private static final String ACCESSION_NUMBER = "LARC12345";

    @Before
    public void setUp() {
        when(odooSyncQueueDAO.insert(any(OdooSyncQueue.class))).thenReturn(
            null
        );
        when(odooSyncQueueDAO.update(any(OdooSyncQueue.class))).thenAnswer(
            inv -> inv.getArgument(0)
        );
    }

    // ─── enqueue ──────────────────────────────────────────────────────────────

    @Test
    public void enqueue_createsItemWithPendingStatus() {
        OdooSyncQueue result = odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        assertEquals(SyncStatus.PENDING, result.getStatus());
        assertEquals(ACCESSION_NUMBER, result.getAccessionNumber());
        assertEquals(0, result.getRetryCount());
        assertEquals(3, result.getMaxRetries());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getNextRetryTime());
    }

    @Test
    public void enqueue_callsDAOInsert() {
        odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        ArgumentCaptor<OdooSyncQueue> captor = ArgumentCaptor.forClass(OdooSyncQueue.class);
        verify(odooSyncQueueDAO, times(1)).insert(captor.capture());
        assertEquals(ACCESSION_NUMBER, captor.getValue().getAccessionNumber());
    }

    @Test
    public void enqueue_nextRetryTimeIsNowOrPast() {
        Timestamp before = Timestamp.from(Instant.now().minusSeconds(1));
        OdooSyncQueue result = odooSyncQueueService.enqueue(ACCESSION_NUMBER);
        Timestamp after = Timestamp.from(Instant.now().plusSeconds(1));

        assertTrue(result.getNextRetryTime().after(before));
        assertTrue(result.getNextRetryTime().before(after));
    }

    // ─── markInProgress ───────────────────────────────────────────────────────

    @Test
    public void markInProgress_setsStatusToInProgress() {
        OdooSyncQueue item = buildPendingItem();

        odooSyncQueueService.markInProgress(item);

        assertEquals(SyncStatus.IN_PROGRESS, item.getStatus());
        verify(odooSyncQueueDAO, times(1)).update(item);
    }

    // ─── markCompleted ────────────────────────────────────────────────────────

    @Test
    public void markCompleted_setsStatusAndCompletedAt() {
        OdooSyncQueue item = buildPendingItem();

        odooSyncQueueService.markCompleted(item);

        assertEquals(SyncStatus.COMPLETED, item.getStatus());
        assertNotNull(item.getCompletedAt());
        assertNull(item.getLastError());
        verify(odooSyncQueueDAO, times(1)).update(item);
    }

    @Test
    public void markCompleted_clearsLastError() {
        OdooSyncQueue item = buildPendingItem();
        item.setLastError("some previous error");

        odooSyncQueueService.markCompleted(item);

        assertNull(item.getLastError());
    }

    // ─── markFailed — retry scheduling ────────────────────────────────────────

    @Test
    public void markFailed_firstFailure_schedulesRetryAfter1Min() {
        OdooSyncQueue item = buildPendingItem(); // retryCount=0
        Timestamp before = Timestamp.from(Instant.now().plusSeconds(59));

        odooSyncQueueService.markFailed(item, "connection refused");

        assertEquals(SyncStatus.PENDING, item.getStatus());
        assertEquals(1, item.getRetryCount());
        assertEquals("connection refused", item.getLastError());
        // nextRetryTime should be ~1 min in future
        assertTrue(item.getNextRetryTime().after(before));
        verify(odooSyncQueueDAO, times(1)).update(item);
    }

    @Test
    public void markFailed_secondFailure_schedulesRetryAfter5Min() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(1); // already failed once
        Timestamp before = Timestamp.from(Instant.now().plusSeconds(4 * 60 + 59));

        odooSyncQueueService.markFailed(item, "timeout");

        assertEquals(SyncStatus.PENDING, item.getStatus());
        assertEquals(2, item.getRetryCount());
        assertTrue(item.getNextRetryTime().after(before));
    }

    @Test
    public void markFailed_thirdFailure_schedulesRetryAfter15Min() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(2); // failed twice before — this is the 3rd failure, index 2 → 15min delay
        item.setMaxRetries(5); // raise max so it doesn't hit FAILED
        Timestamp before = Timestamp.from(Instant.now().plusSeconds(14 * 60 + 59));

        odooSyncQueueService.markFailed(item, "server error");

        assertEquals(SyncStatus.PENDING, item.getStatus());
        assertEquals(3, item.getRetryCount());
        assertTrue(item.getNextRetryTime().after(before));
    }

    // ─── markFailed — max retries exceeded ────────────────────────────────────

    @Test
    public void markFailed_exceedsMaxRetries_setsStatusToFailed() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(3); // already at max (maxRetries=3)

        odooSyncQueueService.markFailed(item, "final failure");

        assertEquals(SyncStatus.FAILED, item.getStatus());
        assertEquals(4, item.getRetryCount());
        assertEquals("final failure", item.getLastError());
        verify(odooSyncQueueDAO, times(1)).update(item);
    }

    @Test
    public void markFailed_exceedsMaxRetries_doesNotSetNextRetryTime() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(3);
        item.setNextRetryTime(null);

        odooSyncQueueService.markFailed(item, "error");

        // nextRetryTime should remain null — no further retry expected
        assertNull(item.getNextRetryTime());
    }

    // ─── getItemsReadyForRetry ─────────────────────────────────────────────────

    @Test
    public void getItemsReadyForRetry_delegatesToDAO() {
        OdooSyncQueue item1 = buildPendingItem();
        OdooSyncQueue item2 = buildPendingItem();
        when(odooSyncQueueDAO.getItemsReadyForRetry()).thenReturn(Arrays.asList(item1, item2));

        List<OdooSyncQueue> result = odooSyncQueueService.getItemsReadyForRetry();

        assertEquals(2, result.size());
        verify(odooSyncQueueDAO, times(1)).getItemsReadyForRetry();
    }

    @Test
    public void getItemsReadyForRetry_returnsEmptyWhenNoneReady() {
        when(odooSyncQueueDAO.getItemsReadyForRetry()).thenReturn(List.of());

        List<OdooSyncQueue> result =
            odooSyncQueueService.getItemsReadyForRetry();

        assertTrue(result.isEmpty());
    }

    // ─── getFailedItems ────────────────────────────────────────────────────────

    @Test
    public void getFailedItems_returnsOnlyFailedStatus() {
        OdooSyncQueue failedItem = buildPendingItem();
        failedItem.setStatus(SyncStatus.FAILED);
        when(odooSyncQueueDAO.getByStatus(SyncStatus.FAILED)).thenReturn(List.of(failedItem));

        List<OdooSyncQueue> result = odooSyncQueueService.getFailedItems();

        assertEquals(1, result.size());
        assertEquals(SyncStatus.FAILED, result.get(0).getStatus());
        verify(odooSyncQueueDAO, times(1)).getByStatus(SyncStatus.FAILED);
    }

    // ─── hasExceededMaxRetries ─────────────────────────────────────────────────

    @Test
    public void hasExceededMaxRetries_falseWhenRetryCountLessThanMax() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(2);
        item.setMaxRetries(3);

        assertFalse(item.hasExceededMaxRetries());
    }

    @Test
    public void hasExceededMaxRetries_falseWhenRetryCountEqualsMax() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(3);
        item.setMaxRetries(3);

        assertFalse(item.hasExceededMaxRetries());
    }

    @Test
    public void hasExceededMaxRetries_trueWhenRetryCountExceedsMax() {
        OdooSyncQueue item = buildPendingItem();
        item.setRetryCount(5);
        item.setMaxRetries(3);

        assertTrue(item.hasExceededMaxRetries());
    }

    // ─── enqueue idempotency ──────────────────────────────────────────────────

    @Test
    public void enqueue_whenPendingItemExists_returnsExistingItemWithoutInserting() {
        OdooSyncQueue existing = buildPendingItem();
        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(existing);

        OdooSyncQueue result = odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        assertSame(existing, result);
        verify(odooSyncQueueDAO, never()).insert(any());
    }

    @Test
    public void enqueue_whenInProgressItemExists_returnsExistingItemWithoutInserting() {
        OdooSyncQueue existing = buildPendingItem();
        existing.setStatus(SyncStatus.IN_PROGRESS);
        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(existing);

        OdooSyncQueue result = odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        assertSame(existing, result);
        verify(odooSyncQueueDAO, never()).insert(any());
    }

    @Test
    public void enqueue_whenNoActiveItemExists_insertsNewItem() {
        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(null);

        odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        verify(odooSyncQueueDAO, times(1)).insert(any(OdooSyncQueue.class));
    }

    @Test
    public void enqueue_whenPreviousItemIsFailed_insertsNewItem() {
        // FAILED items do not block a fresh enqueue
        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(null);

        odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        verify(odooSyncQueueDAO, times(1)).insert(any(OdooSyncQueue.class));
    }

    @Test
    public void enqueue_whenPreviousItemIsCompleted_insertsNewItem() {
        // COMPLETED items do not block a fresh enqueue
        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(null);

        odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        verify(odooSyncQueueDAO, times(1)).insert(any(OdooSyncQueue.class));
    }

    // ─── enqueue concurrent race (unique-constraint violation) ────────────────

    @Test
    public void enqueue_whenInsertRacesAndConstraintViolationOccurs_returnsWinnerRow() {
        // First call to getActiveItemByAccessionNumber returns null (no item yet),
        // simulating the gap between the check and the insert on a concurrent path.
        // The insert then throws because a concurrent enqueue already committed.
        OdooSyncQueue winner = buildPendingItem();
        ConstraintViolationException cve = new ConstraintViolationException(
                "duplicate key value violates unique constraint \"idx_osq_unique_active_accession\"",
                new SQLException("unique constraint violation"), "idx_osq_unique_active_accession");
        LIMSRuntimeException wrappedCve = new LIMSRuntimeException("Error in OdooSyncQueueDAOImpl insert", cve);

        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(null) // first call:
                                                                                                 // pre-insert check
                                                                                                 // sees nothing
                .thenReturn(winner); // second call: post-collision re-fetch finds winner
        doThrow(wrappedCve).when(odooSyncQueueDAO).insert(any(OdooSyncQueue.class));

        OdooSyncQueue result = odooSyncQueueService.enqueue(ACCESSION_NUMBER);

        assertSame(winner, result);
        verify(odooSyncQueueDAO, times(1)).insert(any(OdooSyncQueue.class));
        verify(odooSyncQueueDAO, times(2)).getActiveItemByAccessionNumber(ACCESSION_NUMBER);
    }

    @Test
    public void enqueue_whenConstraintViolationOccursButNoWinnerFound_rethrows() {
        // Extremely unlikely edge case: constraint fires but the winning row has
        // already transitioned out of PENDING/IN_PROGRESS before our re-fetch.
        // The service must not swallow the exception in this case.
        ConstraintViolationException cve = new ConstraintViolationException(
                "duplicate key value violates unique constraint \"idx_osq_unique_active_accession\"",
                new SQLException("unique constraint violation"), "idx_osq_unique_active_accession");
        LIMSRuntimeException wrappedCve = new LIMSRuntimeException("Error in OdooSyncQueueDAOImpl insert", cve);

        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(null);
        doThrow(wrappedCve).when(odooSyncQueueDAO).insert(any(OdooSyncQueue.class));

        try {
            odooSyncQueueService.enqueue(ACCESSION_NUMBER);
            fail("Expected LIMSRuntimeException to be rethrown");
        } catch (LIMSRuntimeException e) {
            assertSame(wrappedCve, e);
        }
    }

    @Test
    public void enqueue_whenNonConstraintLIMSExceptionThrown_rethrowsUnmodified() {
        // A genuine insert failure (e.g. connectivity, mapping error) must not be
        // silently swallowed by the constraint-violation recovery path.
        LIMSRuntimeException otherError = new LIMSRuntimeException("Error in OdooSyncQueueDAOImpl insert",
                new RuntimeException("disk full"));

        when(odooSyncQueueDAO.getActiveItemByAccessionNumber(ACCESSION_NUMBER)).thenReturn(null);
        doThrow(otherError).when(odooSyncQueueDAO).insert(any(OdooSyncQueue.class));

        try {
            odooSyncQueueService.enqueue(ACCESSION_NUMBER);
            fail("Expected LIMSRuntimeException to be rethrown");
        } catch (LIMSRuntimeException e) {
            assertSame(otherError, e);
            // re-fetch must NOT have been attempted — this is not a race, it's a real error
            verify(odooSyncQueueDAO, times(1)).getActiveItemByAccessionNumber(ACCESSION_NUMBER);
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private OdooSyncQueue buildPendingItem() {
        OdooSyncQueue item = new OdooSyncQueue();
        item.setAccessionNumber(ACCESSION_NUMBER);
        item.setStatus(SyncStatus.PENDING);
        item.setRetryCount(0);
        item.setMaxRetries(3);
        item.setCreatedAt(Timestamp.from(Instant.now()));
        item.setNextRetryTime(Timestamp.from(Instant.now()));
        return item;
    }
}
