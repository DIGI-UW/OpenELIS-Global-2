package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import org.openelisglobal.analyzer.valueholder.AnalyzerField;

/**
 * DAO tests for AnalyzerFieldDAO
 * 
 * References: - Testing Roadmap: .specify/guides/testing-roadmap.md
 * 
 * 
 * Note: Using Mockito pattern (matching existing codebase) since @DataJpaTest
 * dependencies not available. Tests HQL query logic and DAO methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerFieldDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<AnalyzerField> query;

    @InjectMocks
    private AnalyzerFieldDAOImpl analyzerFieldDAO;

    private Analyzer testAnalyzer;
    private AnalyzerField testField1;
    private AnalyzerField testField2;

    @Before
    public void setUp() {
        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup test fields
        testField1 = new AnalyzerField();
        testField1.setId("FIELD-001");
        testField1.setAnalyzer(testAnalyzer);
        testField1.setFieldName("GLUCOSE");
        testField1.setFieldType(AnalyzerField.FieldType.NUMERIC);
        testField1.setUnit("mg/dL");
        testField1.setIsActive(true);

        testField2 = new AnalyzerField();
        testField2.setId("FIELD-002");
        testField2.setAnalyzer(testAnalyzer);
        testField2.setFieldName("CHOLESTEROL");
        testField2.setFieldType(AnalyzerField.FieldType.NUMERIC);
        testField2.setUnit("mg/dL");
        testField2.setIsActive(true);
    }

    /**
     * Test: Find by analyzer ID with valid ID returns fields
     */
    @Test
    public void testFindByAnalyzerId_WithValidId_ReturnsFields() {
        // Arrange: Mock HQL query (single query with JOIN to legacy Analyzer)
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerField.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq(1))).thenReturn(query);
        
        List<AnalyzerField> expectedResults = new ArrayList<>();
        expectedResults.add(testField1);
        expectedResults.add(testField2);
        when(query.list()).thenReturn(expectedResults);

        // Act
        List<AnalyzerField> results = analyzerFieldDAO.findByAnalyzerId("1");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 2 fields", 2, results.size());
        assertEquals("First field name should match", "GLUCOSE", results.get(0).getFieldName());
        assertEquals("Second field name should match", "CHOLESTEROL", results.get(1).getFieldName());
    }

    /**
     * Test: Find by analyzer ID with invalid ID returns empty list
     */
    @Test
    public void testFindByAnalyzerId_WithInvalidId_ReturnsEmptyList() {
        // Arrange: Mock HQL query to return empty list
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerField.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq(999))).thenReturn(query);
        when(query.list()).thenReturn(new ArrayList<>());

        // Act: When no fields are found, method should return empty list
        List<AnalyzerField> results = analyzerFieldDAO.findByAnalyzerId("999");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return empty list", 0, results.size());
    }

    /**
     * Test: Insert with valid data persists to database
     * 
     * Note: Testing DAO insert method. Actual persistence verified via service
     * tests.
     */
    @Test
    public void testInsert_WithValidData_PersistsToDatabase() {
        // Arrange: Create new field
        AnalyzerField newField = new AnalyzerField();
        newField.setAnalyzer(testAnalyzer);
        newField.setFieldName("HEMOGLOBIN");
        newField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        newField.setUnit("g/dL");
        newField.setIsActive(true);

        // Mock EntityManager persist - DAO generates UUID before calling persist
        doAnswer(invocation -> {
            // DAO already sets ID before calling persist, so just verify it's set
            return null;
        }).when(entityManager).persist(newField);

        // Act: Insert field (method returns ID)
        // Note: DAO.insert() generates UUID if ID is null (line 49-50 in
        // AnalyzerFieldDAOImpl)
        String id = analyzerFieldDAO.insert(newField);

        // Assert: ID should be generated by DAO
        assertNotNull("ID should not be null", id);
        assertNotNull("Field should have ID set", newField.getId());
        assertEquals("Field ID should match returned ID", id, newField.getId());
        // Verify it's a valid UUID format (36 chars with dashes)
        assertTrue("ID should be a valid UUID format", id.length() == 36 && id.contains("-"));
    }

    /**
     * Test: Get by ID returns field
     */
    @Test
    public void testGet_WithValidId_ReturnsField() {
        // Arrange: BaseDAOImpl.get() uses entityManager.find(), not session.get()
        when(entityManager.find(AnalyzerField.class, "FIELD-001")).thenReturn(testField1);

        // Act
        Optional<AnalyzerField> result = analyzerFieldDAO.get("FIELD-001");

        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Result should be present", result.orElse(null));
        assertEquals("ID should match", "FIELD-001", result.get().getId());
        assertEquals("Field name should match", "GLUCOSE", result.get().getFieldName());
    }

    /**
     * Test: Get by invalid ID returns empty Optional
     */
    @Test
    public void testGet_WithInvalidId_ReturnsEmpty() {
        // Arrange: BaseDAOImpl.get() uses entityManager.find(), not session.get()
        when(entityManager.find(AnalyzerField.class, "INVALID-ID")).thenReturn(null);

        // Act
        Optional<AnalyzerField> result = analyzerFieldDAO.get("INVALID-ID");

        // Assert
        assertNotNull("Result should not be null", result);
        assertNull("Result should be empty", result.orElse(null));
    }
}
