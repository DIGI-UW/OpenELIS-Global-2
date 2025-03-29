package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
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

    public void getAnalyzersFromDatabase() {
        List<Analyzer> analyzerList = analyzerService.getAll();
        assertEquals(3, analyzerList.size());
        analyzerList.forEach(analyzer -> {
            System.out.print(analyzer.getName() + " ");
        });
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
        // Create a new analyzer
        Analyzer newAnalyzer = createTestAnalyzer("Test Analyzer", "TEST-001", "TEST");

        // Create new test mappings
        List<AnalyzerTestMapping> newMappings = new ArrayList<>();
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTestName("New Test");
        mapping.setTestId("101");
        newMappings.add(mapping);

        // Persist data
        analyzerService.persistData(newAnalyzer, newMappings, new ArrayList<>());

        // Verify analyzer was inserted
        assertNotNull(newAnalyzer.getId());
        Analyzer savedAnalyzer = analyzerService.getAnalyzerByName("Test Analyzer");
        assertNotNull(savedAnalyzer);
        assertEquals("Test Analyzer", savedAnalyzer.getName());

        // Verify mapping was created
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
        // Get existing analyzer
        Analyzer existingAnalyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(existingAnalyzer);

        // Modify analyzer
        String originalLocation = existingAnalyzer.getLocation();
        existingAnalyzer.setLocation("Updated Location");

        // Create new test mappings
        List<AnalyzerTestMapping> newMappings = new ArrayList<>();
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTestName("Updated Test");
        mapping.setTestId("103");
        newMappings.add(mapping);

        // Get existing mappings
        List<AnalyzerTestMapping> existingMappings = new ArrayList<>();

        // Persist data
        analyzerService.persistData(existingAnalyzer, newMappings, existingMappings);

        // Verify analyzer was updated
        Analyzer updatedAnalyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(updatedAnalyzer);
        assertEquals("Updated Location", updatedAnalyzer.getLocation());

        // Verify new mapping was added
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
        // Get existing analyzer
        Analyzer existingAnalyzer = analyzerService.getAnalyzerByName("Cobas 6800");
        assertNotNull(existingAnalyzer);

        // Create mapping that duplicates an existing one
        List<AnalyzerTestMapping> newMappings = new ArrayList<>();
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerTestName("Glucose Test");
        mapping.setTestId("101");
        mapping.setAnalyzerId(existingAnalyzer.getId());
        newMappings.add(mapping);

        // Get existing mappings
        List<AnalyzerTestMapping> existingMappings = new ArrayList<>();
        existingMappings.add(mapping);

        // Get initial count of mappings
        int initialCount = analyzerTestMappingService.getAll().size();

        // Persist data
        analyzerService.persistData(existingAnalyzer, newMappings, existingMappings);

        // Verify no new mappings were added (count should be the same)
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