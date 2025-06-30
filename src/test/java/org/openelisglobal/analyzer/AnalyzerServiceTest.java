package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

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

    @Test
    public void persistData_shouldInsertNewAnalyzerAndMappings() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer_test_map", "analyzer" });
        Analyzer newAnalyzer = createTestAnalyzer("Test Analyzer", "TEST-001", "TEST");

        List<AnalyzerTestMapping> newMappings = new ArrayList<>();
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTestName("New Test");
        mapping.setTestId("101");
        newMappings.add(mapping);

        analyzerService.persistData(newAnalyzer, newMappings, new ArrayList<>());

        assertNotNull(newAnalyzer.getId());
        Analyzer savedAnalyzer = analyzerService.getAnalyzerByName("Test Analyzer");
        assertNotNull(savedAnalyzer);
        assertEquals("Test Analyzer", savedAnalyzer.getName());
        List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAll();
        boolean found = false;
        for (AnalyzerTestMapping m : mappings) {
            if (m.getAnalyzerId().equals(newAnalyzer.getId()) && m.getAnalyzerTestName().equals("New Test")
                    && m.getTestId().equals("101")) {
                found = true;
                break;
            }
        }
        assertTrue("Expected mapping not found", found);
    }

    @Test
    public void persistData_shouldUpdateExistingAnalyzerAndAddNewMappings() {
        Analyzer existingAnalyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(existingAnalyzer);

        String originalLocation = existingAnalyzer.getLocation();
        existingAnalyzer.setLocation("Updated Location");

        List<AnalyzerTestMapping> newMappings = new ArrayList<>();
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTestName("Updated Test");
        mapping.setTestId("103");
        newMappings.add(mapping);

        List<AnalyzerTestMapping> existingMappings = new ArrayList<>();

        analyzerService.persistData(existingAnalyzer, newMappings, existingMappings);

        Analyzer updatedAnalyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(updatedAnalyzer);
        assertEquals("Updated Location", updatedAnalyzer.getLocation());

        List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAll();
        boolean found = false;
        for (AnalyzerTestMapping m : mappings) {
            if (m.getAnalyzerId().equals(existingAnalyzer.getId()) && m.getAnalyzerTestName().equals("Updated Test")
                    && m.getTestId().equals("103")) {
                found = true;
                break;
            }
        }
        assertTrue("Expected mapping not found", found);
    }

    @Test
    public void persistData_shouldNotDuplicateExistingMappings() {
        Analyzer existingAnalyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(existingAnalyzer);

        List<AnalyzerTestMapping> newMappings = new ArrayList<>();
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTestName("Glucose Test");
        mapping.setTestId("101");
        mapping.setAnalyzerId(existingAnalyzer.getId());
        newMappings.add(mapping);

        List<AnalyzerTestMapping> existingMappings = new ArrayList<>();
        existingMappings.add(mapping);

        int initialCount = analyzerTestMappingService.getAll().size();

        analyzerService.persistData(existingAnalyzer, newMappings, existingMappings);

        int newCount = analyzerTestMappingService.getAll().size();
        assertEquals(initialCount, newCount);
    }

    private Analyzer createTestAnalyzer(String name, String machineId, String analyzerType) {
        Analyzer analyzer = new Analyzer();
        analyzer.setName(name);
        analyzer.setMachineId(machineId);
        analyzer.setType(analyzerType);
        analyzer.setDescription("Test description");
        analyzer.setLocation("Test location");
        analyzer.setActive(true);
        analyzer.setHasSetupPage(true);
        analyzer.setSysUserId("1");
        return analyzer;
    }
}