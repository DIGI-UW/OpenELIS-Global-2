package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * OGC-738a — the storage audit-trail viewer used to show raw numeric user ids
 * ("Moved By: 42"). This locks the service-layer responsibility of resolving
 * each movement row's user id into a display name with batched lookups so a
 * list of N movements with K distinct users does K lookups, not N.
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageServiceMovementsTest {

    @Mock
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private SampleStorageServiceImpl sampleStorageService;

    private SampleStorageMovement movementByUser42;
    private SampleStorageMovement secondMovementByUser42;
    private SampleStorageMovement movementByUser99;
    private SystemUser user42;
    private SystemUser user99;

    @Before
    public void setUp() {
        user42 = new SystemUser();
        user42.setId("42");
        user42.setFirstName("Bob");
        user42.setLastName("Tester");

        user99 = new SystemUser();
        user99.setId("99");
        user99.setFirstName("Casey");
        user99.setLastName("");

        movementByUser42 = newMovement(1, 42);
        secondMovementByUser42 = newMovement(2, 42);
        movementByUser99 = newMovement(3, 99);
    }

    private SampleStorageMovement newMovement(int id, int movedByUserId) {
        SampleStorageMovement m = new SampleStorageMovement();
        m.setId(id);
        m.setMovedByUserId(movedByUserId);
        m.setMovementDate(new Timestamp(System.currentTimeMillis()));
        return m;
    }

    @Test
    public void resolvesMovedByUserNameOnEachRow() {
        when(sampleStorageMovementDAO.findBySampleItemId("123"))
                .thenReturn(Arrays.asList(movementByUser42, movementByUser99));
        when(systemUserService.get("42")).thenReturn(user42);
        when(systemUserService.get("99")).thenReturn(user99);

        List<Map<String, Object>> rows = sampleStorageService.getSampleItemMovementsWithUserNames("123");

        assertEquals(2, rows.size());
        assertEquals("Bob Tester", rows.get(0).get("movedByUserName"));
        assertEquals("Casey", rows.get(1).get("movedByUserName"));
        // Raw id preserved as a fallback for older rows in the FE.
        assertEquals(42, rows.get(0).get("movedByUserId"));
    }

    @Test
    public void cachesUserLookupAcrossRows() {
        when(sampleStorageMovementDAO.findBySampleItemId("123"))
                .thenReturn(Arrays.asList(movementByUser42, secondMovementByUser42));
        when(systemUserService.get("42")).thenReturn(user42);

        sampleStorageService.getSampleItemMovementsWithUserNames("123");

        // 2 rows, both by user 42 — should hit systemUserService exactly once.
        verify(systemUserService, times(1)).get("42");
    }

    @Test
    public void missingUserFallsBackToNullName_ButRowStillReturned() {
        when(sampleStorageMovementDAO.findBySampleItemId("123"))
                .thenReturn(Arrays.asList(movementByUser42));
        when(systemUserService.get("42")).thenReturn(null);

        List<Map<String, Object>> rows = sampleStorageService.getSampleItemMovementsWithUserNames("123");

        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("movedByUserName"));
        assertNotNull(rows.get(0).get("movedByUserId"));
    }
}
