package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration.AnalyzerStatus;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerConfigurationService status transition validation
 * 
 * Task Reference: T151c, T153a Test Coverage Goal: >80%
 * 
 * Tests the unified status transition validation: - Manual transitions
 * (INACTIVE, SETUP, VALIDATION) - Automatic transitions (ACTIVE, ERROR_PENDING,
 * OFFLINE) - Invalid transitions are rejected
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerConfigurationServiceTest {

    @Mock
    private AnalyzerConfigurationDAO analyzerConfigurationDAO;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    @InjectMocks
    private AnalyzerConfigurationServiceImpl configurationService;

    private Analyzer testAnalyzer;
    private AnalyzerConfiguration testConfig;

    @Before
    public void setUp() {
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        testConfig = new AnalyzerConfiguration();
        testConfig.setId("CONFIG-001");
        testConfig.setAnalyzer(testAnalyzer);
        testConfig.setStatus(AnalyzerStatus.SETUP);
    }

    // === validateStatusTransition Tests ===

    @Test
    public void testValidateStatusTransition_SameStatus_ReturnsTrue() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.SETUP));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_ToInactive_AlwaysAllowed() {
        // From any status to INACTIVE should be allowed
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.INACTIVE));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.INACTIVE));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.INACTIVE));
        assertTrue(
                configurationService.validateStatusTransition(AnalyzerStatus.ERROR_PENDING, AnalyzerStatus.INACTIVE));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.OFFLINE, AnalyzerStatus.INACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromInactive_ToSetup_Allowed() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.INACTIVE, AnalyzerStatus.SETUP));
    }

    @Test
    public void testValidateStatusTransition_FromInactive_ToActive_NotAllowed() {
        // Cannot go directly from INACTIVE to ACTIVE
        assertFalse(configurationService.validateStatusTransition(AnalyzerStatus.INACTIVE, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromSetup_ToValidation_Allowed() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.VALIDATION));
    }

    @Test
    public void testValidateStatusTransition_FromSetup_ToActive_NotAllowed() {
        // Cannot skip VALIDATION stage
        assertFalse(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromValidation_ToActive_Allowed() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromValidation_ToSetup_Allowed() {
        // Rollback to SETUP is allowed
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.SETUP));
    }

    @Test
    public void testValidateStatusTransition_FromActive_ToErrorPending_Allowed() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.ERROR_PENDING));
    }

    @Test
    public void testValidateStatusTransition_FromActive_ToOffline_Allowed() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.OFFLINE));
    }

    @Test
    public void testValidateStatusTransition_FromErrorPending_ToActive_Allowed() {
        // All errors acknowledged
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ERROR_PENDING, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromOffline_ToActive_Allowed() {
        // Connection restored
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.OFFLINE, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_NullStatus_ReturnsFalse() {
        assertFalse(configurationService.validateStatusTransition(null, AnalyzerStatus.SETUP));
        assertFalse(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, null));
        assertFalse(configurationService.validateStatusTransition(null, null));
    }

    @Test
    public void testValidateStatusTransition_ToDeleted_OnlyFromInactive() {
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.INACTIVE, AnalyzerStatus.DELETED));
        assertFalse(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.DELETED));
        assertFalse(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.DELETED));
    }

    // === canTransitionTo Tests ===

    @Test
    public void testCanTransitionTo_ValidTransition_ReturnsTrue() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(analyzerConfigurationDAO.findByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        assertTrue(configurationService.canTransitionTo("1", AnalyzerStatus.VALIDATION));
    }

    @Test
    public void testCanTransitionTo_InvalidTransition_ReturnsFalse() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(analyzerConfigurationDAO.findByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        assertFalse(configurationService.canTransitionTo("1", AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testCanTransitionTo_AnalyzerNotFound_ReturnsFalse() {
        when(analyzerConfigurationDAO.findByAnalyzerId("999")).thenReturn(Optional.empty());

        assertFalse(configurationService.canTransitionTo("999", AnalyzerStatus.VALIDATION));
    }

    // === setStatusManually Tests ===

    @Test
    public void testSetStatusManually_ToInactive_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerConfigurationDAO.findByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = configurationService.setStatusManually("1", AnalyzerStatus.INACTIVE, "testUser");

        assertEquals(AnalyzerStatus.INACTIVE, result.getStatus());
        verify(analyzerConfigurationDAO).update(any(AnalyzerConfiguration.class));
    }

    @Test
    public void testSetStatusManually_ToSetup_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.INACTIVE);
        when(analyzerConfigurationDAO.findByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = configurationService.setStatusManually("1", AnalyzerStatus.SETUP, "testUser");

        assertEquals(AnalyzerStatus.SETUP, result.getStatus());
    }

    @Test
    public void testSetStatusManually_ToValidation_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(analyzerConfigurationDAO.findByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = configurationService.setStatusManually("1", AnalyzerStatus.VALIDATION,
                "testUser");

        assertEquals(AnalyzerStatus.VALIDATION, result.getStatus());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_ToActive_ThrowsException() {
        // ACTIVE cannot be set manually - exception thrown before mock is used
        configurationService.setStatusManually("1", AnalyzerStatus.ACTIVE, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_ToErrorPending_ThrowsException() {
        // ERROR_PENDING cannot be set manually - exception thrown before mock is used
        configurationService.setStatusManually("1", AnalyzerStatus.ERROR_PENDING, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_ToOffline_ThrowsException() {
        // OFFLINE cannot be set manually - exception thrown before mock is used
        configurationService.setStatusManually("1", AnalyzerStatus.OFFLINE, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_AnalyzerNotFound_ThrowsException() {
        when(analyzerConfigurationDAO.findByAnalyzerId("999")).thenReturn(Optional.empty());

        configurationService.setStatusManually("999", AnalyzerStatus.INACTIVE, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_InvalidTransition_ThrowsException() {
        // Cannot go from INACTIVE directly to VALIDATION
        testConfig.setStatus(AnalyzerStatus.INACTIVE);
        when(analyzerConfigurationDAO.findByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        configurationService.setStatusManually("1", AnalyzerStatus.VALIDATION, "testUser");
    }

    @Test
    public void testValidateStatusTransition_CompleteWorkflow_AllTransitionsValid() {
        // Test the complete happy-path workflow:
        // SETUP -> VALIDATION -> ACTIVE -> ERROR_PENDING -> ACTIVE -> OFFLINE -> ACTIVE
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.VALIDATION));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.ACTIVE));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.ERROR_PENDING));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ERROR_PENDING, AnalyzerStatus.ACTIVE));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.OFFLINE));
        assertTrue(configurationService.validateStatusTransition(AnalyzerStatus.OFFLINE, AnalyzerStatus.ACTIVE));
    }

    // === findByIdentifierPatternMatch Tests (GenericASTM/GenericHL7 plugin
    // selection) ===

    @Test
    public void testFindByIdentifierPatternMatch_NullIdentifier_ReturnsEmpty() {
        assertEquals(Optional.empty(), configurationService.findByIdentifierPatternMatch(null));
    }

    @Test
    public void testFindByIdentifierPatternMatch_EmptyIdentifier_ReturnsEmpty() {
        assertEquals(Optional.empty(), configurationService.findByIdentifierPatternMatch(""));
        assertEquals(Optional.empty(), configurationService.findByIdentifierPatternMatch("   "));
    }

    @Test
    public void testFindByIdentifierPatternMatch_NoCandidates_ReturnsEmpty() {
        when(analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns()).thenReturn(Collections.emptyList());

        assertEquals(Optional.empty(), configurationService.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0"));
    }

    @Test
    public void testFindByIdentifierPatternMatch_SubstringMatch_ReturnsConfig() {
        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setId("CONFIG-2006");
        config.setIdentifierPattern("MINDRAY.*BA-88A|BA88A");
        config.setGenericPlugin(true);
        config.setPreferGenericPlugin(true);
        config.setAnalyzer(testAnalyzer);
        when(analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns())
                .thenReturn(Collections.singletonList(config));

        Optional<AnalyzerConfiguration> result = configurationService
                .findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0");

        assertTrue("Pattern uses .find() so substring should match", result.isPresent());
        assertEquals("CONFIG-2006", result.get().getId());
        assertTrue("prefer_generic_plugin is returned for plugin-order override", result.get().isPreferGenericPlugin());
    }

    @Test
    public void testFindByIdentifierPatternMatch_NoMatch_ReturnsEmpty() {
        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setId("CONFIG-2006");
        config.setIdentifierPattern("MINDRAY.*BA-88A");
        config.setGenericPlugin(true);
        when(analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns())
                .thenReturn(Collections.singletonList(config));

        assertEquals(Optional.empty(), configurationService.findByIdentifierPatternMatch("UNKNOWN^MODEL^1.0"));
    }

    @Test
    public void testFindByIdentifierPatternMatch_InvalidRegex_SkipsConfigAndReturnsEmpty() {
        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setId("CONFIG-BAD");
        config.setIdentifierPattern("[invalid(regex");
        config.setGenericPlugin(true);
        when(analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns())
                .thenReturn(Collections.singletonList(config));

        Optional<AnalyzerConfiguration> result = configurationService
                .findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0");

        assertFalse("Invalid regex should be skipped, no throw", result.isPresent());
    }

    @Test
    public void testFindByIdentifierPatternMatch_FirstMatchWins() {
        AnalyzerConfiguration config1 = new AnalyzerConfiguration();
        config1.setId("CONFIG-FIRST");
        config1.setIdentifierPattern("MINDRAY");
        config1.setGenericPlugin(true);
        config1.setAnalyzer(testAnalyzer);
        AnalyzerConfiguration config2 = new AnalyzerConfiguration();
        config2.setId("CONFIG-SECOND");
        config2.setIdentifierPattern("BA-88A");
        config2.setGenericPlugin(true);
        List<AnalyzerConfiguration> list = new ArrayList<>();
        list.add(config1);
        list.add(config2);
        when(analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns()).thenReturn(list);

        Optional<AnalyzerConfiguration> result = configurationService
                .findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0");

        assertTrue(result.isPresent());
        assertEquals("CONFIG-FIRST", result.get().getId());
    }

    @Test
    public void testFindByIdentifierPatternMatch_ConfigWithNullPattern_Skipped() {
        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setId("CONFIG-NULL-PATTERN");
        config.setIdentifierPattern(null);
        config.setGenericPlugin(true);
        when(analyzerConfigurationDAO.findGenericPluginConfigsWithPatterns())
                .thenReturn(Collections.singletonList(config));

        assertEquals(Optional.empty(), configurationService.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0"));
    }
}
