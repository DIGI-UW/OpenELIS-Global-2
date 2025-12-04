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
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;

/**
 * Unit tests for SampleStorageAssignmentDAO - Verifies SampleItem ID queries
 * (SampleItem.id is String in entity and VARCHAR in database)
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageAssignmentDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<SampleStorageAssignment> query;

    @InjectMocks
    private SampleStorageAssignmentDAOImpl dao;

    private SampleStorageAssignment testAssignment;
    private SampleItem testSampleItem;

    @Before
    public void setUp() {
        testSampleItem = new SampleItem();
        testSampleItem.setId("1000"); // String ID (numeric in database)

        testAssignment = new SampleStorageAssignment();
        testAssignment.setId(1);
        testAssignment.setSampleItem(testSampleItem);
        testAssignment.setLocationId(10);
        testAssignment.setLocationType("device");
    }

    /**
     * Test: findBySampleItemId correctly uses String sampleItemId for database
     * query (SampleItem.id is String in entity and VARCHAR in database)
     */
    @Test
    public void testFindBySampleItemId_UsesStringId_ReturnsAssignment() {
        // Setup
        String sampleItemId = "1000"; // Numeric string (matches database numeric column)
        List<SampleStorageAssignment> results = new ArrayList<>();
        results.add(testAssignment);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), eq(1000))).thenReturn(query); // Integer parameter
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.list()).thenReturn(results);

        // Execute
        SampleStorageAssignment result = dao.findBySampleItemId(sampleItemId);

        // Verify
        assertNotNull(result);
        assertEquals(testAssignment.getId(), result.getId());
        assertEquals(testAssignment.getSampleItem().getId(), result.getSampleItem().getId());

        // Verify String ID is parsed to Integer for database query
        verify(query).setParameter("sampleItemId", 1000);
    }

    /**
     * Test: findBySampleItemId returns null when no assignment found
     */
    @Test
    public void testFindBySampleItemId_NoAssignmentFound_ReturnsNull() {
        // Setup
        String sampleItemId = "9999"; // Numeric string
        List<SampleStorageAssignment> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.list()).thenReturn(emptyResults);

        // Execute
        SampleStorageAssignment result = dao.findBySampleItemId(sampleItemId);

        // Verify
        assertNull(result);
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
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);
        when(query.setParameter(anyString(), anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.list()).thenThrow(new RuntimeException("Database connection error"));

        // Execute - should throw LIMSRuntimeException
        try {
            dao.findBySampleItemId(sampleItemId);
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Error finding SampleStorageAssignment"));
            throw e;
        }
    }
}
