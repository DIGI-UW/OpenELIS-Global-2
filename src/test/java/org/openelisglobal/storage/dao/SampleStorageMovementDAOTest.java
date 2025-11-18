package org.openelisglobal.storage.dao;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;

/**
 * Unit tests for SampleStorageMovementDAO - Verifies String-to-numeric
 * conversion for sample ID queries (Sample.id is String in entity but numeric
 * in database)
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageMovementDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<SampleStorageMovement> query;

    @InjectMocks
    private SampleStorageMovementDAOImpl dao;

    private SampleStorageMovement testMovement1;
    private SampleStorageMovement testMovement2;
    private SampleItem testSampleItem;

    @Before
    public void setUp() {
        testSampleItem = new SampleItem();
        testSampleItem.setId("1000"); // String ID (numeric in database)

        testMovement1 = new SampleStorageMovement();
        testMovement1.setId(1);
        testMovement1.setSampleItem(testSampleItem);
        testMovement1.setReason("Test movement 1");

        testMovement2 = new SampleStorageMovement();
        testMovement2.setId(2);
        testMovement2.setSampleItem(testSampleItem);
        testMovement2.setReason("Test movement 2");
    }

    /**
     * Test: findBySampleItemId correctly uses String sampleItemId for database
     * query (SampleItem.id is String in entity and VARCHAR in database)
     */
    @Test
    public void testFindBySampleItemId_UsesStringId_ReturnsMovements() {
        // Setup
        String sampleItemId = "1000"; // Numeric string (matches database numeric column)
        List<SampleStorageMovement> results = new ArrayList<>();
        results.add(testMovement2); // Most recent first (ORDER BY movementDate DESC)
        results.add(testMovement1);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageMovement.class))).thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), eq(1000))).thenReturn(query); // Integer parameter
        when(query.list()).thenReturn(results);

        // Execute
        List<SampleStorageMovement> result = dao.findBySampleItemId(sampleItemId);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testMovement2.getId(), result.get(0).getId()); // Most recent first

        // Verify String ID is parsed to Integer for database query
        verify(query).setParameter("sampleItemId", 1000);
    }

    /**
     * Test: findBySampleItemId returns empty list when no movements found
     */
    @Test
    public void testFindBySampleItemId_NoMovementsFound_ReturnsEmptyList() {
        // Setup
        String sampleItemId = "9999"; // Numeric string
        List<SampleStorageMovement> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageMovement.class))).thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), anyInt())).thenReturn(query);
        when(query.list()).thenReturn(emptyResults);

        // Execute
        List<SampleStorageMovement> result = dao.findBySampleItemId(sampleItemId);

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(query).setParameter(eq("sampleItemId"), anyInt());
    }

    /**
     * Test: findBySampleItemId handles database errors gracefully
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testFindBySampleItemId_DatabaseError_ThrowsException() {
        // Setup
        String sampleItemId = "1000"; // Numeric string

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageMovement.class))).thenReturn(query);
        when(query.setParameter(anyString(), anyInt())).thenReturn(query);
        when(query.list()).thenThrow(new RuntimeException("Database connection error"));

        // Execute - should throw LIMSRuntimeException
        try {
            dao.findBySampleItemId(sampleItemId);
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Error finding SampleStorageMovements"));
            throw e;
        }
    }
}
