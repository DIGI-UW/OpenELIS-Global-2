package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;

/**
 * DAO tests for AnalyzerErrorDAO
 * 
 * 
 * Note: Using Mockito pattern (matching existing codebase) since @DataJpaTest
 * dependencies not available. Tests HQL query logic and DAO methods.
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerErrorDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<AnalyzerError> query;

    @InjectMocks
    private AnalyzerErrorDAOImpl analyzerErrorDAO;

    private Analyzer testAnalyzer;
    private AnalyzerError testError1;
    private AnalyzerError testError2;

    @Before
    public void setUp() {
        // Setup test analyzer
        // Note: Analyzer uses String IDs in Java (e.g., "1"), but INTEGER in database
        // Reference: ID_TYPE_ANALYSIS.md - Legacy Analyzer uses
        // LIMSStringNumberUserType
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1"); // String ID (matches Java type)
        testAnalyzer.setName("Test Analyzer");

        // Setup test error 1 (UNACKNOWLEDGED, MAPPING, ERROR)
        testError1 = new AnalyzerError();
        testError1.setId("ERROR-001");
        testError1.setAnalyzer(testAnalyzer);
        testError1.setErrorType(AnalyzerError.ErrorType.MAPPING);
        testError1.setSeverity(AnalyzerError.Severity.ERROR);
        testError1.setErrorMessage("No mapping found for test code: GLUCOSE");
        testError1.setRawMessage("H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");
        testError1.setStatus(AnalyzerError.ErrorStatus.UNACKNOWLEDGED);

        // Setup test error 2 (ACKNOWLEDGED, VALIDATION, WARNING)
        testError2 = new AnalyzerError();
        testError2.setId("ERROR-002");
        testError2.setAnalyzer(testAnalyzer);
        testError2.setErrorType(AnalyzerError.ErrorType.VALIDATION);
        testError2.setSeverity(AnalyzerError.Severity.WARNING);
        testError2.setErrorMessage("Unit mismatch: mg/dL vs mmol/L");
        testError2.setRawMessage("H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^CHOLESTEROL|200|mg/dL|N");
        testError2.setStatus(AnalyzerError.ErrorStatus.ACKNOWLEDGED);
        testError2.setAcknowledgedBy("USER-001");
    }

    /**
     * Test: Find errors by status
     * 
     * Verifies that findByStatus() returns only errors with the specified status.
     */
    @Test
    public void testFindByStatus_ReturnsUnacknowledgedErrors() {
        // Arrange: Mock HQL query
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerError.class))).thenReturn(query);
        when(query.setParameter(eq("status"), eq("UNACKNOWLEDGED"))).thenReturn(query);

        List<AnalyzerError> expectedResults = new ArrayList<>();
        expectedResults.add(testError1);
        when(query.list()).thenReturn(expectedResults);

        // Act
        List<AnalyzerError> results = analyzerErrorDAO.findByStatus("UNACKNOWLEDGED");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return one unacknowledged error", 1, results.size());
        assertEquals("Status should be UNACKNOWLEDGED", AnalyzerError.ErrorStatus.UNACKNOWLEDGED,
                results.get(0).getStatus());
    }

    /**
     * Test: Find errors by analyzer ID
     * 
     * Verifies that findByAnalyzerId() returns all errors for the specified
     * analyzer.
     */
    @Test
    public void testFindByAnalyzerId_ReturnsErrorsForAnalyzer() {
        // Arrange: Mock HQL query
        // Note: AnalyzerErrorDAO converts String "1" to Integer 1 for HQL parameter
        // Reference: ID_TYPE_ANALYSIS.md - Legacy Analyzer uses LIMSStringNumberUserType
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerError.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq(1))).thenReturn(query);  // Integer, not String

        List<AnalyzerError> expectedResults = new ArrayList<>();
        expectedResults.add(testError1);
        expectedResults.add(testError2);
        when(query.list()).thenReturn(expectedResults);

        // Act: Pass String "1" (matches Java entity type)
        List<AnalyzerError> results = analyzerErrorDAO.findByAnalyzerId("1");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return two errors", 2, results.size());
        // Verify all returned errors belong to the test analyzer
        for (AnalyzerError error : results) {
            assertNotNull("Analyzer should not be null", error.getAnalyzer());
            assertEquals("Analyzer ID should match", "1", error.getAnalyzer().getId());
        }
    }

    /**
     * Test: Find errors by error type
     * 
     * Verifies that findByErrorType() returns only errors with the specified type.
     */
    @Test
    public void testFindByErrorType_ReturnsMatchingErrors() {
        // Arrange: Mock HQL query
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerError.class))).thenReturn(query);
        when(query.setParameter(eq("errorType"), eq("MAPPING"))).thenReturn(query);

        List<AnalyzerError> expectedResults = new ArrayList<>();
        expectedResults.add(testError1);
        when(query.list()).thenReturn(expectedResults);

        // Act
        List<AnalyzerError> results = analyzerErrorDAO.findByErrorType("MAPPING");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return one MAPPING error", 1, results.size());
        assertEquals("Error type should be MAPPING", AnalyzerError.ErrorType.MAPPING, results.get(0).getErrorType());
    }

    /**
     * Test: Find errors by severity
     * 
     * Verifies that findBySeverity() returns only errors with the specified
     * severity.
     */
    @Test
    public void testFindBySeverity_ReturnsMatchingErrors() {
        // Arrange: Mock HQL query
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerError.class))).thenReturn(query);
        when(query.setParameter(eq("severity"), eq("ERROR"))).thenReturn(query);

        List<AnalyzerError> expectedResults = new ArrayList<>();
        expectedResults.add(testError1);
        when(query.list()).thenReturn(expectedResults);

        // Act
        List<AnalyzerError> results = analyzerErrorDAO.findBySeverity("ERROR");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return one ERROR severity error", 1, results.size());
        assertEquals("Severity should be ERROR", AnalyzerError.Severity.ERROR, results.get(0).getSeverity());
    }

    /**
     * Test: Get error by ID
     * 
     * Verifies that get() retrieves error by ID correctly.
     */
    @Test
    public void testGet_WithValidId_ReturnsError() {
        // Arrange: Mock EntityManager.get() for BaseDAO.get()
        when(entityManager.find(AnalyzerError.class, "ERROR-001")).thenReturn(testError1);

        // Act
        Optional<AnalyzerError> result = analyzerErrorDAO.get("ERROR-001");

        // Assert
        assertTrue("Result should be present", result.isPresent());
        AnalyzerError error = result.get();
        assertEquals("ID should match", testError1.getId(), error.getId());
        assertEquals("Error message should match", "No mapping found for test code: GLUCOSE",
                error.getErrorMessage());
    }

    /**
     * Test: Get error by invalid ID
     * 
     * Verifies that get() returns empty Optional for invalid ID.
     */
    @Test
    public void testGet_WithInvalidId_ReturnsEmpty() {
        // Arrange: Mock EntityManager.get() to return null
        when(entityManager.find(AnalyzerError.class, "INVALID-ID")).thenReturn(null);

        // Act
        Optional<AnalyzerError> result = analyzerErrorDAO.get("INVALID-ID");

        // Assert
        assertTrue("Result should be empty", result.isEmpty());
    }
}
