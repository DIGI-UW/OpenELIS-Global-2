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
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;

/**
 * DAO tests for QualitativeResultMappingDAO
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class QualitativeResultMappingDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private NativeQuery<QualitativeResultMapping> nativeQuery;

    @InjectMocks
    private QualitativeResultMappingDAOImpl qualitativeResultMappingDAO;

    private Analyzer testAnalyzer;
    private AnalyzerField testAnalyzerField;
    private QualitativeResultMapping mapping1;
    private QualitativeResultMapping mapping2;

    @Before
    public void setUp() {
        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup test analyzer field
        testAnalyzerField = new AnalyzerField();
        testAnalyzerField.setId("FIELD-001");
        testAnalyzerField.setAnalyzer(testAnalyzer);
        testAnalyzerField.setFieldName("HIV_TEST");

        // Setup test mappings
        mapping1 = new QualitativeResultMapping();
        mapping1.setId("MAPPING-001");
        mapping1.setAnalyzerField(testAnalyzerField);
        mapping1.setAnalyzerValue("POSITIVE");
        mapping1.setOpenelisCode("OPENELIS-POS");

        mapping2 = new QualitativeResultMapping();
        mapping2.setId("MAPPING-002");
        mapping2.setAnalyzerField(testAnalyzerField);
        mapping2.setAnalyzerValue("NEGATIVE");
        mapping2.setOpenelisCode("OPENELIS-NEG");
    }

    /**
     * Test: Find by analyzer field ID returns mappings
     */
    @Test
    public void testFindByAnalyzerFieldId_ReturnsMappings() {
        // Arrange
        List<QualitativeResultMapping> expectedMappings = new ArrayList<>();
        expectedMappings.add(mapping1);
        expectedMappings.add(mapping2);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.addEntity(eq(QualitativeResultMapping.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("analyzerFieldId"), eq("FIELD-001"))).thenReturn(nativeQuery);
        when(nativeQuery.list()).thenReturn(expectedMappings);

        // Act
        List<QualitativeResultMapping> actualMappings = qualitativeResultMappingDAO.findByAnalyzerFieldId("FIELD-001");

        // Assert
        assertNotNull("Mapping list should not be null", actualMappings);
        assertEquals("Should return 2 mappings", 2, actualMappings.size());
        assertEquals("First mapping ID should match", "MAPPING-001", actualMappings.get(0).getId());
        assertEquals("First mapping analyzer value should match", "POSITIVE", actualMappings.get(0).getAnalyzerValue());
    }

    /**
     * Test: Find by analyzer field ID with no mappings returns empty list
     */
    @Test
    public void testFindByAnalyzerFieldId_NoMappings_ReturnsEmptyList() {
        // Arrange
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.addEntity(eq(QualitativeResultMapping.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("analyzerFieldId"), eq("FIELD-999"))).thenReturn(nativeQuery);
        when(nativeQuery.list()).thenReturn(new ArrayList<>());

        // Act
        List<QualitativeResultMapping> actualMappings = qualitativeResultMappingDAO.findByAnalyzerFieldId("FIELD-999");

        // Assert
        assertNotNull("Mapping list should not be null", actualMappings);
        assertTrue("Should return empty list", actualMappings.isEmpty());
    }
}
