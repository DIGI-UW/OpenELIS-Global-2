package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;

/**
 * DAO tests for AnalyzerConfigurationDAO
 * 
 * Task Reference: T033 (DAO tests)
 * Test Coverage Goal: >80%
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerConfigurationDAOTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private org.hibernate.query.NativeQuery<AnalyzerConfiguration> nativeQuery;

    @InjectMocks
    private AnalyzerConfigurationDAOImpl analyzerConfigurationDAO;

    private Analyzer testAnalyzer;
    private AnalyzerConfiguration testConfig;

    @Before
    public void setUp() {
        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        // Setup test configuration
        testConfig = new AnalyzerConfiguration();
        testConfig.setId("CONFIG-001");
        testConfig.setAnalyzer(testAnalyzer);
        testConfig.setIpAddress("192.168.1.100");
        testConfig.setPort(5000);
        testConfig.setProtocolVersion("ASTM LIS2-A2");
    }

    /**
     * Test: Find by analyzer ID returns configuration
     * Task Reference: T033
     * Note: Uses HQL with JOIN to legacy Analyzer entity
     */
    @Test
    public void testFindByAnalyzerId_WithValidId_ReturnsConfiguration() {
        // Arrange: Mock HQL query (actual implementation uses HQL)
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        @SuppressWarnings("unchecked")
        org.hibernate.query.Query<AnalyzerConfiguration> query = 
            org.mockito.Mockito.mock(org.hibernate.query.Query.class);
        when(session.createQuery(anyString(), eq(AnalyzerConfiguration.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq(1))).thenReturn(query); // Integer, not String
        when(query.uniqueResult()).thenReturn(testConfig);

        // Act
        Optional<AnalyzerConfiguration> result = analyzerConfigurationDAO.findByAnalyzerId("1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be present", result.isPresent());
        assertEquals("Configuration ID should match", "CONFIG-001", result.get().getId());
        assertEquals("IP address should match", "192.168.1.100", result.get().getIpAddress());
    }

    /**
     * Test: Find by analyzer ID with no configuration returns empty
     * Task Reference: T033
     */
    @Test
    public void testFindByAnalyzerId_WithNoConfiguration_ReturnsEmpty() {
        // Arrange: Mock HQL query to return null (no configuration found)
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        @SuppressWarnings("unchecked")
        org.hibernate.query.Query<AnalyzerConfiguration> query = 
            org.mockito.Mockito.mock(org.hibernate.query.Query.class);
        when(session.createQuery(anyString(), eq(AnalyzerConfiguration.class))).thenReturn(query);
        when(query.setParameter(eq("analyzerId"), eq(999))).thenReturn(query); // Integer, not String
        when(query.uniqueResult()).thenReturn(null);

        // Act
        Optional<AnalyzerConfiguration> result = analyzerConfigurationDAO.findByAnalyzerId("999");

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    /**
     * Test: Find by analyzer ID handles exceptions gracefully
     * Task Reference: T033
     */
    @Test
    public void testFindByAnalyzerId_WithException_ReturnsEmpty() {
        // Arrange: Simulate NumberFormatException (invalid analyzer ID format)
        // This tests the exception handling path without needing to mock query execution
        // Act
        Optional<AnalyzerConfiguration> result = analyzerConfigurationDAO.findByAnalyzerId("INVALID-ID-FORMAT");

        // Assert: Should return empty instead of throwing exception
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty on error", result.isEmpty());
    }
}

