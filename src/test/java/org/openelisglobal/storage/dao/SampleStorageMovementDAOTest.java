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
    private Sample testSample;

    @Before
    public void setUp() {
        testSample = new Sample();
        testSample.setId("1000"); // String ID

        testMovement1 = new SampleStorageMovement();
        testMovement1.setId(1);
        testMovement1.setSample(testSample);
        testMovement1.setReason("Test movement 1");

        testMovement2 = new SampleStorageMovement();
        testMovement2.setId(2);
        testMovement2.setSample(testSample);
        testMovement2.setReason("Test movement 2");
    }

    /**
     * Test: findBySampleId correctly converts String sampleId to Integer for
     * database query This verifies the fix for the type mismatch issue (String
     * parameter vs numeric column)
     */
    @Test
    public void testFindBySampleId_ConvertsStringToInteger_ReturnsMovements() {
        // Setup
        String sampleId = "1000";
        List<SampleStorageMovement> results = new ArrayList<>();
        results.add(testMovement2); // Most recent first (ORDER BY movementDate DESC)
        results.add(testMovement1);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageMovement.class))).thenReturn(query);
        when(query.setParameter(eq("sampleId"), eq(1000))).thenReturn(query); // Verify Integer is used
        when(query.list()).thenReturn(results);

        // Execute
        List<SampleStorageMovement> result = dao.findBySampleId(sampleId);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testMovement2.getId(), result.get(0).getId()); // Most recent first

        // Verify Integer.parseInt was used (not String directly)
        verify(query).setParameter("sampleId", 1000); // Integer, not String
    }

    /**
     * Test: findBySampleId returns empty list when no movements found
     */
    @Test
    public void testFindBySampleId_NoMovementsFound_ReturnsEmptyList() {
        // Setup
        String sampleId = "9999";
        List<SampleStorageMovement> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(SampleStorageMovement.class))).thenReturn(query);
        when(query.setParameter(eq("sampleId"), eq(9999))).thenReturn(query);
        when(query.list()).thenReturn(emptyResults);

        // Execute
        List<SampleStorageMovement> result = dao.findBySampleId(sampleId);

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
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
        when(session.createQuery(anyString(), eq(SampleStorageMovement.class))).thenReturn(query);

        // Execute - should throw NumberFormatException which is caught and re-thrown as
        // LIMSRuntimeException
        try {
            dao.findBySampleId(invalidSampleId);
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid sample ID format"));
            throw e;
        }
    }
}
