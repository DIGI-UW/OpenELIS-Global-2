package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
import org.openelisglobal.analyzer.valueholder.UnitMapping;

/**
 * DAO tests for UnitMappingDAO
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class UnitMappingDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private NativeQuery<UnitMapping> nativeQuery;

    @InjectMocks
    private UnitMappingDAOImpl unitMappingDAO;

    private Analyzer testAnalyzer;
    private AnalyzerField testAnalyzerField;
    private UnitMapping mapping1;
    private UnitMapping mapping2;

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
        testAnalyzerField.setFieldName("GLUCOSE");

        // Setup test mappings
        mapping1 = new UnitMapping();
        mapping1.setId("MAPPING-001");
        mapping1.setAnalyzerField(testAnalyzerField);
        mapping1.setAnalyzerUnit("mg/dL");
        mapping1.setOpenelisUnit("OPENELIS-MGDL");
        mapping1.setConversionFactor(new BigDecimal("1.0"));

        mapping2 = new UnitMapping();
        mapping2.setId("MAPPING-002");
        mapping2.setAnalyzerField(testAnalyzerField);
        mapping2.setAnalyzerUnit("mmol/L");
        mapping2.setOpenelisUnit("OPENELIS-MMOLL");
        mapping2.setConversionFactor(new BigDecimal("0.0555"));
    }

    /**
     * Test: Find by analyzer field ID returns mappings
     */
    @Test
    public void testFindByAnalyzerFieldId_ReturnsMappings() {
        // Arrange
        List<UnitMapping> expectedMappings = new ArrayList<>();
        expectedMappings.add(mapping1);
        expectedMappings.add(mapping2);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.addEntity(eq(UnitMapping.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("analyzerFieldId"), eq("FIELD-001"))).thenReturn(nativeQuery);
        when(nativeQuery.list()).thenReturn(expectedMappings);

        // Act
        List<UnitMapping> actualMappings = unitMappingDAO.findByAnalyzerFieldId("FIELD-001");

        // Assert
        assertNotNull("Mapping list should not be null", actualMappings);
        assertEquals("Should return 2 mappings", 2, actualMappings.size());
        assertEquals("First mapping ID should match", "MAPPING-001", actualMappings.get(0).getId());
        assertEquals("First mapping analyzer unit should match", "mg/dL", actualMappings.get(0).getAnalyzerUnit());
    }

    /**
     * Test: Find by analyzer field ID with no mappings returns empty list
     */
    @Test
    public void testFindByAnalyzerFieldId_NoMappings_ReturnsEmptyList() {
        // Arrange
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.addEntity(eq(UnitMapping.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("analyzerFieldId"), eq("FIELD-999"))).thenReturn(nativeQuery);
        when(nativeQuery.list()).thenReturn(new ArrayList<>());

        // Act
        List<UnitMapping> actualMappings = unitMappingDAO.findByAnalyzerFieldId("FIELD-999");

        // Assert
        assertNotNull("Mapping list should not be null", actualMappings);
        assertTrue("Should return empty list", actualMappings.isEmpty());
    }
}
