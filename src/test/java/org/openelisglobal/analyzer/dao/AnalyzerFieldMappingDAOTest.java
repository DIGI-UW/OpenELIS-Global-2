package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;

/**
 * DAO tests for AnalyzerFieldMappingDAO
 * 
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AnalyzerFieldMappingDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query<AnalyzerFieldMapping> query;

    @InjectMocks
    private AnalyzerFieldMappingDAOImpl analyzerFieldMappingDAO;

    private Analyzer testAnalyzer;
    private AnalyzerField testField;
    private AnalyzerFieldMapping testMapping1;
    private AnalyzerFieldMapping testMapping2;

    @Before
    public void setUp() {
        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup test field
        testField = new AnalyzerField();
        testField.setId("FIELD-001");
        testField.setAnalyzer(testAnalyzer);
        testField.setFieldName("GLUCOSE");
        testField.setFieldType(AnalyzerField.FieldType.NUMERIC);
        testField.setUnit("mg/dL");

        // Setup test mappings
        testMapping1 = new AnalyzerFieldMapping();
        testMapping1.setId("MAPPING-001");
        testMapping1.setAnalyzerField(testField);
        testMapping1.setOpenelisFieldId("TEST-001");
        testMapping1.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.TEST);
        testMapping1.setMappingType(AnalyzerFieldMapping.MappingType.TEST_LEVEL);
        testMapping1.setIsRequired(false);
        testMapping1.setIsActive(true);

        testMapping2 = new AnalyzerFieldMapping();
        testMapping2.setId("MAPPING-002");
        testMapping2.setAnalyzerField(testField);
        testMapping2.setOpenelisFieldId("RESULT-001");
        testMapping2.setOpenelisFieldType(AnalyzerFieldMapping.OpenELISFieldType.RESULT);
        testMapping2.setMappingType(AnalyzerFieldMapping.MappingType.RESULT_LEVEL);
        testMapping2.setIsRequired(true);
        testMapping2.setIsActive(false); // Draft
    }

    /**
     * Test: Find by analyzer field ID returns mappings
     */
    @Test
    public void testFindByAnalyzerFieldId_ReturnsMappings() {
        // Arrange
        List<AnalyzerFieldMapping> expectedResults = new ArrayList<>();
        expectedResults.add(testMapping1);
        expectedResults.add(testMapping2);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerFieldMapping.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerFieldId"), eq("FIELD-001"))).thenReturn(query);
        when(query.list()).thenReturn(expectedResults);

        // Act
        List<AnalyzerFieldMapping> results = analyzerFieldMappingDAO.findByAnalyzerFieldId("FIELD-001");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 2 mappings", 2, results.size());
        assertEquals("First mapping ID should match", "MAPPING-001", results.get(0).getId());
        assertEquals("Second mapping ID should match", "MAPPING-002", results.get(1).getId());
    }

    /**
     * Test: Find active mappings by analyzer ID returns only active
     */
    @Test
    public void testFindActiveMappingsByAnalyzerId_ReturnsOnlyActive() {
        // Arrange: Only active mappings should be returned
        List<AnalyzerFieldMapping> allMappings = new ArrayList<>();
        allMappings.add(testMapping1); // Active
        allMappings.add(testMapping2); // Inactive (draft)

        List<AnalyzerFieldMapping> activeMappings = new ArrayList<>();
        activeMappings.add(testMapping1); // Only active

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerFieldMapping.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq("1"))).thenReturn(query);
        when(query.list()).thenReturn(activeMappings);

        // Act
        List<AnalyzerFieldMapping> results = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("1");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 1 active mapping", 1, results.size());
        assertEquals("Mapping should be active", true, results.get(0).getIsActive());
        assertEquals("Mapping ID should match", "MAPPING-001", results.get(0).getId());
    }

    /**
     * Test: Find active mappings with no active mappings returns empty list
     */
    @Test
    public void testFindActiveMappingsByAnalyzerId_WithNoActiveMappings_ReturnsEmptyList() {
        // Arrange: No active mappings
        List<AnalyzerFieldMapping> emptyResults = new ArrayList<>();

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString(), eq(AnalyzerFieldMapping.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq("999"))).thenReturn(query);
        when(query.list()).thenReturn(emptyResults);

        // Act
        List<AnalyzerFieldMapping> results = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("999");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return empty list", 0, results.size());
    }
}
