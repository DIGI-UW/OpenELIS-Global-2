package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/**
 * Unit tests for analyzer identifier matching.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerServiceStatusTest {

    @Mock
    private AnalyzerDAO baseObjectDAO;

    @InjectMocks
    private AnalyzerServiceImpl analyzerServiceImpl;

    // === findByIdentifierPatternMatch Tests (GenericASTM/GenericHL7 plugin
    // selection) ===

    @Test
    public void testFindByIdentifierPatternMatch_NullIdentifier_ReturnsEmpty() {
        assertEquals(Optional.empty(), analyzerServiceImpl.findByIdentifierPatternMatch((String) null));
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
    public void testFindByIdentifierPatternMatch_ListIdentifiers_PrefersMoreSpecificPattern() {
        Analyzer genericAnalyzer = new Analyzer();
        genericAnalyzer.setId("GENERIC");
        genericAnalyzer.setName("Generic Mindray");
        genericAnalyzer.setIdentifierPattern("MINDRAY");

        Analyzer specificAnalyzer = new Analyzer();
        specificAnalyzer.setId("SPECIFIC");
        specificAnalyzer.setName("Mindray BS-200");
        specificAnalyzer.setIdentifierPattern("MINDRAY.*BS.?200");

        List<Analyzer> list = new ArrayList<>();
        list.add(genericAnalyzer);
        list.add(specificAnalyzer);
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(list);

        Optional<Analyzer> result = analyzerServiceImpl
                .findByIdentifierPatternMatch(List.of("Mindray BS-200", "BS-200", "Mindray"));

        assertTrue(result.isPresent());
        assertEquals("SPECIFIC", result.get().getId());
    }

    @Test
    public void testFindByIdentifierPatternMatch_ListIdentifiers_UsesUppercaseFallback() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("UPPER");
        analyzer.setName("Mindray BS-200");
        analyzer.setIdentifierPattern("MINDRAY.*BS.?200");
        when(baseObjectDAO.findGenericAnalyzersWithPatterns()).thenReturn(Collections.singletonList(analyzer));

        Optional<Analyzer> result = analyzerServiceImpl
                .findByIdentifierPatternMatch(List.of("Mindray BS-200", "BS-200", "Mindray"));

        assertTrue(result.isPresent());
        assertEquals("UPPER", result.get().getId());
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
