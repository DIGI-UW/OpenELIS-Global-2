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
import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerServiceImpl status transition validation and
 * identifier pattern matching (methods migrated from
 * AnalyzerConfigurationService).
 *
 *
 * Tests the unified status transition validation: - Manual transitions
 * (INACTIVE, SETUP, VALIDATION) - Automatic transitions (ACTIVE, ERROR_PENDING,
 * OFFLINE) - Invalid transitions are rejected
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerServiceStatusTest {

    @Mock
    private AnalyzerDAO baseObjectDAO;

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    @Mock
    private AnalyzerTestMappingService analyzerMappingService;

    @InjectMocks
    private AnalyzerServiceImpl analyzerServiceImpl;

    private Analyzer testAnalyzer;

    @Before
    public void setUp() {
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
    }

    // === validateStatusTransition Tests ===

    @Test
    public void testValidateStatusTransition_SameStatus_ReturnsTrue() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.SETUP));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_ToInactive_AlwaysAllowed() {
        // From any status to INACTIVE should be allowed
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.INACTIVE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.INACTIVE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.INACTIVE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ERROR_PENDING, AnalyzerStatus.INACTIVE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.OFFLINE, AnalyzerStatus.INACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromInactive_ToSetup_Allowed() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.INACTIVE, AnalyzerStatus.SETUP));
    }

    @Test
    public void testValidateStatusTransition_FromInactive_ToActive_NotAllowed() {
        // Cannot go directly from INACTIVE to ACTIVE
        assertFalse(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.INACTIVE, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromSetup_ToValidation_Allowed() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.VALIDATION));
    }

    @Test
    public void testValidateStatusTransition_FromSetup_ToActive_NotAllowed() {
        // Cannot skip VALIDATION stage
        assertFalse(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromValidation_ToActive_Allowed() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromValidation_ToSetup_Allowed() {
        // Rollback to SETUP is allowed
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.SETUP));
    }

    @Test
    public void testValidateStatusTransition_FromActive_ToErrorPending_Allowed() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.ERROR_PENDING));
    }

    @Test
    public void testValidateStatusTransition_FromActive_ToOffline_Allowed() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.OFFLINE));
    }

    @Test
    public void testValidateStatusTransition_FromErrorPending_ToActive_Allowed() {
        // All errors acknowledged
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ERROR_PENDING, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_FromOffline_ToActive_Allowed() {
        // Connection restored
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.OFFLINE, AnalyzerStatus.ACTIVE));
    }

    @Test
    public void testValidateStatusTransition_NullStatus_ReturnsFalse() {
        assertFalse(analyzerServiceImpl.validateStatusTransition(null, AnalyzerStatus.SETUP));
        assertFalse(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, null));
        assertFalse(analyzerServiceImpl.validateStatusTransition(null, null));
    }

    @Test
    public void testValidateStatusTransition_ToDeleted_OnlyFromInactive() {
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.INACTIVE, AnalyzerStatus.DELETED));
        assertFalse(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.DELETED));
        assertFalse(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.DELETED));
    }

    // === canTransitionTo Tests ===

    @Test
    public void testCanTransitionTo_ValidTransition_ReturnsTrue() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(baseObjectDAO.get("1")).thenReturn(Optional.of(testAnalyzer));

        assertTrue(analyzerServiceImpl.canTransitionTo("1", AnalyzerStatus.VALIDATION));
    }

    @Test
    public void testCanTransitionTo_InvalidTransition_ReturnsFalse() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(baseObjectDAO.get("1")).thenReturn(Optional.of(testAnalyzer));

        assertFalse(analyzerServiceImpl.canTransitionTo("1", AnalyzerStatus.ACTIVE));
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testCanTransitionTo_AnalyzerNotFound_ThrowsException() {
        when(baseObjectDAO.get("999")).thenReturn(Optional.empty());

        analyzerServiceImpl.canTransitionTo("999", AnalyzerStatus.VALIDATION);
    }

    // === setStatusManually Tests ===

    @Test
    public void testSetStatusManually_ToInactive_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(baseObjectDAO.get("1")).thenReturn(Optional.of(testAnalyzer));
        when(baseObjectDAO.update(any(Analyzer.class))).thenReturn(testAnalyzer);

        Analyzer result = analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.INACTIVE, "testUser");

        assertEquals(AnalyzerStatus.INACTIVE, result.getStatus());
        verify(baseObjectDAO).update(any(Analyzer.class));
    }

    @Test
    public void testSetStatusManually_ToSetup_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.INACTIVE);
        when(baseObjectDAO.get("1")).thenReturn(Optional.of(testAnalyzer));
        when(baseObjectDAO.update(any(Analyzer.class))).thenReturn(testAnalyzer);

        Analyzer result = analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.SETUP, "testUser");

        assertEquals(AnalyzerStatus.SETUP, result.getStatus());
    }

    @Test
    public void testSetStatusManually_ToValidation_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(baseObjectDAO.get("1")).thenReturn(Optional.of(testAnalyzer));
        when(baseObjectDAO.update(any(Analyzer.class))).thenReturn(testAnalyzer);

        Analyzer result = analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.VALIDATION, "testUser");

        assertEquals(AnalyzerStatus.VALIDATION, result.getStatus());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_ToActive_ThrowsException() {
        // ACTIVE cannot be set manually - exception thrown before mock is used
        analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.ACTIVE, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_ToErrorPending_ThrowsException() {
        // ERROR_PENDING cannot be set manually - exception thrown before mock is used
        analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.ERROR_PENDING, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_ToOffline_ThrowsException() {
        // OFFLINE cannot be set manually - exception thrown before mock is used
        analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.OFFLINE, "testUser");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testSetStatusManually_AnalyzerNotFound_ThrowsException() {
        when(baseObjectDAO.get("999")).thenReturn(Optional.empty());

        analyzerServiceImpl.setStatusManually("999", AnalyzerStatus.INACTIVE, "testUser");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testSetStatusManually_InvalidTransition_ThrowsException() {
        // Cannot go from INACTIVE directly to VALIDATION
        testAnalyzer.setStatus(AnalyzerStatus.INACTIVE);
        when(baseObjectDAO.get("1")).thenReturn(Optional.of(testAnalyzer));

        analyzerServiceImpl.setStatusManually("1", AnalyzerStatus.VALIDATION, "testUser");
    }

    @Test
    public void testValidateStatusTransition_CompleteWorkflow_AllTransitionsValid() {
        // Test the complete happy-path workflow:
        // SETUP -> VALIDATION -> ACTIVE -> ERROR_PENDING -> ACTIVE -> OFFLINE -> ACTIVE
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.SETUP, AnalyzerStatus.VALIDATION));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.VALIDATION, AnalyzerStatus.ACTIVE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.ERROR_PENDING));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ERROR_PENDING, AnalyzerStatus.ACTIVE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.ACTIVE, AnalyzerStatus.OFFLINE));
        assertTrue(analyzerServiceImpl.validateStatusTransition(AnalyzerStatus.OFFLINE, AnalyzerStatus.ACTIVE));
    }

    // === findByIdentifierPatternMatch Tests (GenericASTM/GenericHL7 plugin
    // selection) ===

    @Test
    public void testFindByIdentifierPatternMatch_NullIdentifier_ReturnsEmpty() {
        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch(null));
    }

    @Test
    public void testFindByIdentifierPatternMatch_EmptyIdentifier_ReturnsEmpty() {
        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch(""));
        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch("   "));
    }

    @Test
    public void testFindByIdentifierPatternMatch_NoCandidates_ReturnsEmpty() {
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(Collections.emptyList());

        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0"));
    }

    @Test
    public void testFindByIdentifierPatternMatch_SubstringMatch_ReturnsAnalyzer() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("2006");
        analyzer.setName("Mindray BA-88A");
        analyzer.setIdentifierPattern("MINDRAY.*BA-88A|BA88A");
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(Collections.singletonList(analyzer));

        Optional<Analyzer> result = analyzerServiceImpl.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0");

        assertTrue("Pattern uses .find() so substring should match", result.isPresent());
        assertEquals("2006", result.get().getId());
    }

    @Test
    public void testFindByIdentifierPatternMatch_NoMatch_ReturnsEmpty() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("2006");
        analyzer.setIdentifierPattern("MINDRAY.*BA-88A");
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(Collections.singletonList(analyzer));

        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch("UNKNOWN^MODEL^1.0"));
    }

    @Test
    public void testFindByIdentifierPatternMatch_InvalidRegex_SkipsAnalyzerAndReturnsEmpty() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("BAD");
        analyzer.setIdentifierPattern("[invalid(regex");
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(Collections.singletonList(analyzer));

        Optional<Analyzer> result = analyzerServiceImpl.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0");

        assertFalse("Invalid regex should be skipped, no throw", result.isPresent());
    }

    @Test
    public void testFindByIdentifierPatternMatch_FirstMatchWins() {
        Analyzer analyzer1 = new Analyzer();
        analyzer1.setId("FIRST");
        analyzer1.setName("First Match");
        analyzer1.setIdentifierPattern("MINDRAY");

        Analyzer analyzer2 = new Analyzer();
        analyzer2.setId("SECOND");
        analyzer2.setName("Second Match");
        analyzer2.setIdentifierPattern("BA-88A");

        List<Analyzer> list = new ArrayList<>();
        list.add(analyzer1);
        list.add(analyzer2);
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(list);

        Optional<Analyzer> result = analyzerServiceImpl.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0");

        assertTrue(result.isPresent());
        assertEquals("FIRST", result.get().getId());
    }

    @Test
    public void testFindByIdentifierPatternMatch_AnalyzerWithNullPattern_Skipped() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("NULL-PATTERN");
        analyzer.setIdentifierPattern(null);
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(Collections.singletonList(analyzer));

        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch("MINDRAY^BA-88A^1.0"));
    }
}
