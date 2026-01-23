
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
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;

/**
 * Unit tests for SampleStorageAssignmentDAOImpl
 *
 * Focus:
 * - Correct handling of sampleItemId parsing
 * - Correct query execution
 * - Proper wrapping of database exceptions in LIMSRuntimeException
 *
 * This test intentionally avoids SampleItem entity usage because
 * findBySampleItemId() queries directly on the numeric sampleItemId column.
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageAssignmentDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    @SuppressWarnings("rawtypes")
    private Query query;


    @InjectMocks
    private SampleStorageAssignmentDAOImpl dao;

    private SampleStorageAssignment testAssignment;

    @Before
    public void setUp() {
        testAssignment = new SampleStorageAssignment();
        testAssignment.setId(1);
        testAssignment.setLocationId(10);
        testAssignment.setLocationType("device");
    }

    /**
     * Test: Valid numeric String sampleItemId should be parsed
     * and used correctly in the database query.
     */
    @Test
    public void testFindBySampleItemId_ValidNumericString_ReturnsAssignment() {
        String sampleItemId = "1000";

        List<SampleStorageAssignment> results = new ArrayList<>();
        results.add(testAssignment);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class)))
                .thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), eq(1000)))
                .thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.list()).thenReturn(results);

        SampleStorageAssignment result = dao.findBySampleItemId(sampleItemId);

        assertNotNull(result);
        assertEquals(testAssignment.getId(), result.getId());
        verify(query).setParameter("sampleItemId", 1000);
    }

    /**
     * Test: When no assignment is found for a valid sampleItemId,
     * method should return null.
     */
    @Test
    public void testFindBySampleItemId_NoResults_ReturnsNull() {
        String sampleItemId = "9999";

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class)))
                .thenReturn(query);
        when(query.setParameter(eq("sampleItemId"), anyInt()))
                .thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.list()).thenReturn(new ArrayList<>());

        SampleStorageAssignment result = dao.findBySampleItemId(sampleItemId);

        assertNull(result);
    }

    /**
     * Test: Passing null sampleItemId should return null
     * without executing any database query.
     */
    @Test
    public void testFindBySampleItemId_NullInput_ReturnsNull() {
        SampleStorageAssignment result = dao.findBySampleItemId(null);
        assertNull(result);
        verifyZeroInteractions(entityManager);

    }

    /**
     * Test: Passing empty sampleItemId should return null
     * without executing any database query.
     */
    @Test
    public void testFindBySampleItemId_EmptyInput_ReturnsNull() {
        SampleStorageAssignment result = dao.findBySampleItemId("   ");
        assertNull(result);
        verifyZeroInteractions(entityManager);

    }

    /**
     * Test: Invalid (non-numeric) sampleItemId should throw LIMSRuntimeException.
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testFindBySampleItemId_InvalidFormat_ThrowsException() {
        dao.findBySampleItemId("ABC123");
    }

    /**
     * Test: Database exception should be wrapped in LIMSRuntimeException.
     * This satisfies issue #2604 acceptance criteria.
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testFindBySampleItemId_DatabaseError_ThrowsLIMSRuntimeException() {
        String sampleItemId = "1000";

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class)))
                .thenReturn(query);
        when(query.setParameter(anyString(), anyInt()))
                .thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.list())
                .thenThrow(new RuntimeException("Database connection error"));

        try {
            dao.findBySampleItemId(sampleItemId);
        } catch (LIMSRuntimeException e) {
            assertTrue(
                    e.getMessage().contains("Error finding SampleStorageAssignment")
            );
            throw e;
        }
    }
}
