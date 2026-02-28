package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerLabUnitDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;

/**
 * Unit tests for AnalyzerLabUnitService.
 *
 * <p>
 * Test cases: assign/replace lab units, list by analyzer.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerLabUnitServiceTest {

    @Mock
    private AnalyzerLabUnitDAO analyzerLabUnitDAO;

    @InjectMocks
    private AnalyzerLabUnitServiceImpl analyzerLabUnitService;

    @Test
    public void testGetLabUnitsByAnalyzerId_ReturnsList() {
        AnalyzerLabUnit u1 = new AnalyzerLabUnit();
        u1.setLabUnitId("unit-1");
        when(analyzerLabUnitDAO.findByAnalyzerId(2013)).thenReturn(Arrays.asList(u1));

        List<AnalyzerLabUnit> result = analyzerLabUnitService.getLabUnitsByAnalyzerId("2013");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("unit-1", result.get(0).getLabUnitId());
        verify(analyzerLabUnitDAO).findByAnalyzerId(2013);
    }

    @Test
    public void testGetLabUnitsByAnalyzerId_EmptyList_ReturnsEmpty() {
        when(analyzerLabUnitDAO.findByAnalyzerId(2013)).thenReturn(Collections.emptyList());

        List<AnalyzerLabUnit> result = analyzerLabUnitService.getLabUnitsByAnalyzerId("2013");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testReplaceLabUnitsForAnalyzer_CallsDao() {
        List<String> labUnitIds = Arrays.asList("unit-1", "unit-2");

        analyzerLabUnitService.replaceLabUnitsForAnalyzer("2013", labUnitIds);

        verify(analyzerLabUnitDAO).replaceForAnalyzer(2013, labUnitIds);
    }

    @Test
    public void testReplaceLabUnitsForAnalyzer_EmptyList_ReplacesWithEmpty() {
        analyzerLabUnitService.replaceLabUnitsForAnalyzer("2013", Collections.emptyList());

        verify(analyzerLabUnitDAO).replaceForAnalyzer(2013, Collections.emptyList());
    }
}
