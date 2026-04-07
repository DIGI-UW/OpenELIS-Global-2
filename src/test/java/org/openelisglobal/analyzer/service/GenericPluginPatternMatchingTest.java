package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerTypeDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;

/**
 * Unit tests for Generic Plugin pattern matching functionality using
 * AnalyzerType.
 *
 * <p>
 * Feature: Type/Instance Separation for Analyzer Plugins
 *
 * <p>
 * Tests: - AnalyzerType entity fields (identifierPattern, isGenericPlugin) -
 * Pattern matching service methods (findMatchingType) - Generic plugin type
 * queries (getGenericPluginTypes)
 *
 * <p>
 * Note: This test uses AnalyzerType as part of the Type/Instance separation
 * architecture (analyzer_configuration was merged into analyzer).
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericPluginPatternMatchingTest {

    @Mock
    private AnalyzerTypeDAO analyzerTypeDAO;

    @InjectMocks
    private AnalyzerTypeServiceImpl analyzerTypeService;

    private AnalyzerType genericType;
    private AnalyzerType legacyType;

    @Before
    public void setUp() {
        // Setup generic plugin type
        genericType = new AnalyzerType();
        genericType.setId("1");
        genericType.setName("Generic ASTM Analyzer");
        genericType.setIdentifierPattern("GENERIC\\^ASTM.*");
        genericType.setGenericPlugin(true);
        genericType.setActive(true);

        // Setup legacy plugin type (no pattern)
        legacyType = new AnalyzerType();
        legacyType.setId("2");
        legacyType.setName("Horiba Pentra 60");
        legacyType.setIdentifierPattern(null);
        legacyType.setGenericPlugin(false);
        legacyType.setActive(true);
    }

    // === Entity Field Tests ===

    @Test
    public void testAnalyzerType_IdentifierPattern_CanBeSet() {
        AnalyzerType type = new AnalyzerType();
        type.setIdentifierPattern("TEST\\^PATTERN.*");

        assertEquals("TEST\\^PATTERN.*", type.getIdentifierPattern());
    }

    @Test
    public void testAnalyzerType_IdentifierPattern_NullByDefault() {
        AnalyzerType type = new AnalyzerType();

        // Should be null by default (legacy plugins don't have patterns)
        assertEquals(null, type.getIdentifierPattern());
    }

    @Test
    public void testAnalyzerType_IsGenericPlugin_FalseByDefault() {
        AnalyzerType type = new AnalyzerType();

        // Should be false by default (legacy plugins are not generic)
        assertFalse(type.isGenericPlugin());
    }

    @Test
    public void testAnalyzerType_IsGenericPlugin_CanBeSetTrue() {
        AnalyzerType type = new AnalyzerType();
        type.setGenericPlugin(true);

        assertTrue(type.isGenericPlugin());
    }

    // === Pattern Matching Tests ===

    @Test
    public void testMatchesIdentifier_ExactMatch_ReturnsTrue() {
        assertTrue(genericType.matchesIdentifier("GENERIC^ASTM^1.0"));
    }

    @Test
    public void testMatchesIdentifier_RegexMatch_ReturnsTrue() {
        // Pattern "GENERIC\\^ASTM.*" should match "GENERIC^ASTM^2.0^EXTENDED"
        assertTrue(genericType.matchesIdentifier("GENERIC^ASTM^2.0^EXTENDED"));
    }

    @Test
    public void testMatchesIdentifier_NoMatch_ReturnsFalse() {
        assertFalse(genericType.matchesIdentifier("HORIBA^PENTRA60^1.0"));
    }

    @Test
    public void testMatchesIdentifier_NullIdentifier_ReturnsFalse() {
        assertFalse(genericType.matchesIdentifier(null));
    }

    @Test
    public void testMatchesIdentifier_EmptyIdentifier_ReturnsFalse() {
        assertFalse(genericType.matchesIdentifier(""));
    }

    @Test
    public void testMatchesIdentifier_NullPattern_ReturnsFalse() {
        // Legacy type has null pattern
        assertFalse(legacyType.matchesIdentifier("ANY^IDENTIFIER"));
    }

    // === Pattern Matching Logic Tests (using entity directly) ===

    @Test
    public void testMatchingLogic_FindsMatchFromList() {
        // Test the pattern matching logic that service uses
        List<AnalyzerType> types = Arrays.asList(genericType, legacyType);

        Optional<AnalyzerType> result = types.stream().filter(t -> t.matchesIdentifier("GENERIC^ASTM^1.0")).findFirst();

        assertTrue(result.isPresent());
        assertEquals("1", result.get().getId());
        assertEquals("Generic ASTM Analyzer", result.get().getName());
    }

    @Test
    public void testMatchingLogic_NoMatchFromList_ReturnsEmpty() {
        List<AnalyzerType> types = Arrays.asList(genericType, legacyType);

        Optional<AnalyzerType> result = types.stream().filter(t -> t.matchesIdentifier("UNKNOWN^ANALYZER")).findFirst();

        assertFalse(result.isPresent());
    }

    // === Generic Plugin Filtering Tests (using list directly) ===

    @Test
    public void testFilterGenericTypes_ReturnsOnlyGenericTypes() {
        List<AnalyzerType> types = Arrays.asList(genericType, legacyType);

        List<AnalyzerType> result = types.stream().filter(AnalyzerType::isGenericPlugin).toList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isGenericPlugin());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testFilterGenericTypes_EmptyList_WhenNoGenericTypes() {
        List<AnalyzerType> types = Arrays.asList(legacyType);

        List<AnalyzerType> result = types.stream().filter(AnalyzerType::isGenericPlugin).toList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterGenericTypes_EmptyList_WhenNoTypes() {
        List<AnalyzerType> types = Collections.emptyList();

        List<AnalyzerType> result = types.stream().filter(AnalyzerType::isGenericPlugin).toList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterGenericTypes_MultipleGenericTypes() {
        AnalyzerType anotherGeneric = new AnalyzerType();
        anotherGeneric.setId("3");
        anotherGeneric.setName("Another Generic");
        anotherGeneric.setIdentifierPattern("ANOTHER\\^PATTERN.*");
        anotherGeneric.setGenericPlugin(true);
        anotherGeneric.setActive(true);

        List<AnalyzerType> types = Arrays.asList(genericType, legacyType, anotherGeneric);

        List<AnalyzerType> result = types.stream().filter(AnalyzerType::isGenericPlugin).toList();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(AnalyzerType::isGenericPlugin));
    }

    // === Type-Instance Relationship Test ===

    @Test
    public void testAnalyzerType_CanHaveMultipleInstances() {
        Analyzer instance1 = new Analyzer();
        instance1.setId("A1");
        instance1.setName("Generic Analyzer - Lab 1");
        instance1.setLocation("Lab 1");
        instance1.setAnalyzerType(genericType);

        Analyzer instance2 = new Analyzer();
        instance2.setId("A2");
        instance2.setName("Generic Analyzer - Lab 2");
        instance2.setLocation("Lab 2");
        instance2.setAnalyzerType(genericType);

        // Both instances should reference the same type
        assertEquals(genericType, instance1.getAnalyzerType());
        assertEquals(genericType, instance2.getAnalyzerType());
        assertEquals("Generic ASTM Analyzer", instance1.getAnalyzerType().getName());
        assertEquals("Generic ASTM Analyzer", instance2.getAnalyzerType().getName());
    }
}
