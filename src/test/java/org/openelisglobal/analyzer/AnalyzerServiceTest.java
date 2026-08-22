package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerService analyzerService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyzer.xml");
    }

    @Test
    public void getAnalyzersFromDatabase_shouldReturnExpectedResults() {
        List<Analyzer> analyzerList = analyzerService.getAll();

        assertNotNull("Analyzer list should not be null", analyzerList);
        assertFalse("Analyzer list should not be empty", analyzerList.isEmpty());
        assertEquals("Expected 3 analyzers in the database", 3, analyzerList.size());

        for (Analyzer analyzer : analyzerList) {
            assertNotNull("Analyzer name should not be null", analyzer.getName());
            assertFalse("Analyzer name should not be empty", analyzer.getName().trim().isEmpty());
        }
    }

    @Test
    public void getAnalyzerByName_shouldReturnAnalyzerByName() {
        Analyzer analyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(analyzer);
        assertEquals("Cobas 6800", analyzer.getName());
        assertEquals("COBAS6800-001", analyzer.getMachineId());
        assertEquals("MOLECULAR", analyzer.getType());
        assertEquals("Main Laboratory - Room 101", analyzer.getLocation());
        assertTrue(analyzer.isActive());
    }

    @Test
    public void getAnalyzerByName_shouldReturnNullForNonExistentName() {
        Analyzer analyzer = analyzerService.getAnalyzerByName("Non-existent Analyzer");
        assertEquals(null, analyzer);
    }

}
