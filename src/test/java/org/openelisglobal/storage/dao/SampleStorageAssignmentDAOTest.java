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
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;

/**
 * Unit tests for SampleStorageAssignmentDAO - Verifies String-to-numeric
 * conversion for sample ID queries (Sample.id is String in entity but numeric
 * in database)
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
    private Sample testSample;

    @Before
    public void setUp() {
        testSample = new Sample();
        testSample.setId("1000"); // String ID

        testAssignment = new SampleStorageAssignment();
        testAssignment.setId(1);
        testAssignment.setSample(testSample);
        testAssignment.setLocationId(10);
        testAssignment.setLocationType("device");
    }

    /**
     * Test: findBySampleId correctly converts String sampleId to Integer for
     * database query This verifies the fix for the type mismatch issue (String
     * parameter vs numeric column)
     */
    @Test
    public void testFindBySampleId_ConvertsStringToInteger_ReturnsAssignment() {
        // Setup
        String sampleId = "1000";
        List<SampleStorageAssignment> results = new ArrayList<>();
        results.add(testAssignment);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);
        when(query.setParameter(eq("sampleId"), eq(1000))).thenReturn(query); // Verify Integer is used
        when(query.list()).thenReturn(results);

        // Execute
        SampleStorageAssignment result = dao.findBySampleId(sampleId);

        // Verify
        assertNotNull(result);
        assertEquals(testAssignment.getId(), result.getId());
        assertEquals(testAssignment.getSample().getId(), result.getSample().getId());

        // Verify Integer.parseInt was used (not String directly)
        verify(query).setParameter("sampleId", 1000); // Integer, not String
    }

    /**
     * Test: findBySampleId returns null when no assignment found
     */
    @Test
    public void testFindBySampleId_NoAssignmentFound_ReturnsNull() {
        // Setup
        String sampleId = "9999";
        List<SampleStorageAssignment> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);
        when(query.setParameter(eq("sampleId"), eq(9999))).thenReturn(query);
        when(query.list()).thenReturn(emptyResults);

        // Execute
        SampleStorageAssignment result = dao.findBySampleId(sampleId);

        // Verify
        assertNull(result);
        verify(query).setParameter("sampleId", 9999); // Integer conversion
    }

    /**
     * Test: findBySampleId throws exception for invalid (non-numeric) sample ID
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testFindBySampleId_InvalidFormat_ThrowsException() {
        // Setup
        String invalidSampleId = "not-a-number";

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);

        // Execute - should throw NumberFormatException which is caught and re-thrown as
        // LIMSRuntimeException
        try {
            dao.findBySampleId(invalidSampleId);
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid sample ID format"));
            throw e;
        }
    }

    /**
     * Test: findBySampleId handles database errors gracefully
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testFindBySampleId_DatabaseError_ThrowsException() {
        // Setup
        String sampleId = "1000";

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageAssignment.class))).thenReturn(query);
        when(query.setParameter(anyString(), anyInt())).thenReturn(query);
        when(query.list()).thenThrow(new RuntimeException("Database connection error"));

        // Execute - should throw LIMSRuntimeException
        try {
            dao.findBySampleId(sampleId);
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Error finding SampleStorageAssignment by sample ID"));
            throw e;
        }
    }
}
