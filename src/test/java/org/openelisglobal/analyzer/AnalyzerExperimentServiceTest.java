package org.openelisglobal.analyzer;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerExperimentService;
import org.openelisglobal.analyzer.valueholder.AnalyzerExperiment;
import org.openelisglobal.common.exception.LIMSException;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerExperimentServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerExperimentService analyzerExperimentService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyzer-experiment.xml");
    }

    @Test
    public void getAnalyzerExperimentFromDataBase_shouldReturnExpectedResults() {

        List<AnalyzerExperiment> experimentList = analyzerExperimentService.getAll();

        assertNotNull("Experiment list should not be null", experimentList);
        assertFalse("Experiment list should not be empty", experimentList.isEmpty());
        assertEquals("Expected 3 experiments in the database", 3, experimentList.size());

        for (AnalyzerExperiment experiment : experimentList) {
            assertNotNull("Experiment name should not be null", experiment.getName());
            assertFalse("Experiment name should not be empty", experiment.getName().trim().isEmpty());
        }
    }

    @Test
    public void getWellValuesForId_shouldReturnWellValues() throws IOException {

        Map<String, String> wellValues = analyzerExperimentService.getWellValuesForId(1);
        assertNotNull(wellValues);
    }

    @Test
    public void saveMapAsCSVFile_shouldSaveAndReturnId() throws LIMSException, Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> wellValues = new HashMap<>();
        wellValues.put("A1", "Sample1");
        wellValues.put("B2", "Sample2");
        wellValues.put("C3", "Sample3");
        assertEquals(0, analyzerExperimentService.getAll().size());
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("TestFile.csv", wellValues);
        
        assertNotNull("Saved experiment should have a valid ID", id);
        
        AnalyzerExperiment savedExperiment = analyzerExperimentService.get(id);
        assertEquals("TestFile.csv", savedExperiment.getName());
        assertEquals(1, analyzerExperimentService.getAll().size());
        
        Map<String, String> retrievedWellValues = analyzerExperimentService.getWellValuesForId(id);
        assertNotNull("Retrieved well values should not be null", retrievedWellValues);
        assertEquals("A1", retrievedWellValues.get("A1"));
        assertEquals("Sample1", retrievedWellValues.get("A1"));
        assertEquals("B2", retrievedWellValues.get("B2"));
        assertEquals("Sample2", retrievedWellValues.get("B2"));
        assertEquals("C3", retrievedWellValues.get("C3"));
        assertEquals("Sample3", retrievedWellValues.get("C3"));
        assertEquals(3, retrievedWellValues.size());
        
        analyzerExperimentService.delete(savedExperiment);
    }
    
    @Test
    public void getAnalyzerExperimentById_shouldReturnCorrectExperiment() {
        AnalyzerExperiment experiment = analyzerExperimentService.get(1);

        assertNotNull(experiment);
        assertEquals("Blood Chemistry Analysis", experiment.getName());
    }

    @Test
    public void update_shouldUpdateAnalyzerExperiment() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });

        AnalyzerExperiment newExperiment = new AnalyzerExperiment();
        newExperiment.setName("PCR Test Experiment");
        newExperiment.setFile("well,Sample Name\nD1,NewSample1\n".getBytes());
        Integer inserted = analyzerExperimentService.insert(newExperiment);

        List<AnalyzerExperiment> experiments = analyzerExperimentService.getAll();
        AnalyzerExperiment experiment = experiments.get(0);
        assertNotNull(experiment);

        Integer id = experiment.getId();
        String originalName = experiment.getName();
        experiment.setName("Updated Blood Count Test");

        analyzerExperimentService.update(experiment);
        AnalyzerExperiment updatedExperiment = analyzerExperimentService.get(id);

        assertNotNull(updatedExperiment);
        assertEquals("Updated Blood Count Test", updatedExperiment.getName());

        analyzerExperimentService.delete(updatedExperiment);
    }

    @Test
    public void delete_shouldDeleteAnalyzerExperiment() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        AnalyzerExperiment newExperiment = new AnalyzerExperiment();
        newExperiment.setName("PCR Test Experiment");
        newExperiment.setFile("well,Sample Name\nD1,NewSample1\n".getBytes(StandardCharsets.UTF_8));
        Integer inserted = analyzerExperimentService.insert(newExperiment);
        assertEquals(1, analyzerExperimentService.getAll().size());

        List<AnalyzerExperiment> experiments = analyzerExperimentService.getAll();
        AnalyzerExperiment experiment = experiments.get(0);
        assertNotNull(experiment);
        Integer id = experiment.getId();

        AnalyzerExperiment deleteExperiment = analyzerExperimentService.get(id);

        assertNotNull(deleteExperiment);
        analyzerExperimentService.delete(deleteExperiment);
        assertEquals(0, analyzerExperimentService.getAll().size());
    }

    @Test
    public void saveAndRetrieveUTF8BasicData_shouldPreserveEncoding() throws LIMSException, IOException {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> basicData = new HashMap<>();
        basicData.put("A1", "StandardData");
        basicData.put("B2", "MoreData");
        basicData.put("C3", "FinalData");
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("BasicUTF8.csv", basicData);
        assertNotNull("Basic UTF-8 test should save successfully", id);
        
        Map<String, String> retrievedData = analyzerExperimentService.getWellValuesForId(id);
        assertEquals("Basic UTF-8 data should preserve values", basicData, retrievedData);
        
        analyzerExperimentService.delete(analyzerExperimentService.get(id));
    }

    @Test
    public void saveAndRetrieveUTF8SpecialCharacters_shouldPreserveEncoding() throws LIMSException, IOException {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> specialData = new HashMap<>();
        specialData.put("A1", "Café");
        specialData.put("B2", "Niño");
        specialData.put("C3", "Москва");
        specialData.put("D4", "東京");
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("SpecialChars.csv", specialData);
        assertNotNull("Special characters test should save successfully", id);
        
        Map<String, String> retrievedData = analyzerExperimentService.getWellValuesForId(id);
        assertEquals("Café", retrievedData.get("A1"));
        assertEquals("Niño", retrievedData.get("B2"));
        assertEquals("Москва", retrievedData.get("C3"));
        assertEquals("東京", retrievedData.get("D4"));
        assertEquals(4, retrievedData.size());
        
        analyzerExperimentService.delete(analyzerExperimentService.get(id));
    }

    @Test
    public void saveAndRetrieveEmptyMap_shouldHandleGracefully() throws LIMSException, IOException {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> emptyData = new HashMap<>();
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("Empty.csv", emptyData);
        assertNotNull("Empty map should save successfully", id);
        
        Map<String, String> retrievedData = analyzerExperimentService.getWellValuesForId(id);
        assertEquals("Empty map should retrieve empty map", 0, retrievedData.size());
        
        analyzerExperimentService.delete(analyzerExperimentService.get(id));
    }

    @Test
    public void saveAndRetrieveSingleEntry_shouldWorkCorrectly() throws LIMSException, IOException {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> singleEntry = new HashMap<>();
        singleEntry.put("A1", "OnlyOneEntry");
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("SingleEntry.csv", singleEntry);
        assertNotNull("Single entry should save successfully", id);
        
        Map<String, String> retrievedData = analyzerExperimentService.getWellValuesForId(id);
        assertEquals("OnlyOneEntry", retrievedData.get("A1"));
        assertEquals(1, retrievedData.size());
        
        analyzerExperimentService.delete(analyzerExperimentService.get(id));
    }

    @Test
    public void saveAndRetrieveLargeDataset_shouldHandleCorrectly() throws LIMSException, IOException {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> largeData = new HashMap<>();
        for (int row = 1; row <= 96; row++) {
            String wellId = String.format("%s%d", (char)('A' + (row - 1) / 12), ((row - 1) % 12) + 1);
            largeData.put(wellId, "SampleData_" + row);
        }
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("LargeDataset.csv", largeData);
        assertNotNull("Large dataset should save successfully", id);
        
        Map<String, String> retrievedData = analyzerExperimentService.getWellValuesForId(id);
        assertEquals("Large dataset should preserve all entries", largeData.size(), retrievedData.size());
        assertEquals("SampleData_50", retrievedData.get("A5"));
        assertEquals("SampleData_96", retrievedData.get("H8"));
        
        analyzerExperimentService.delete(analyzerExperimentService.get(id));
    }

    @Test
    public void saveAndRetrieveCommaDelimitedData_shouldHandleCorrectly() throws LIMSException, IOException {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        
        Map<String, String> commaData = new HashMap<>();
        commaData.put("A1", "Value,With,Commas");
        commaData.put("B2", "Another\"Quoted\"Value");
        commaData.put("C3", "Line\nBreaks");
        
        Integer id = analyzerExperimentService.saveMapAsCSVFile("ComplexData.csv", commaData);
        assertNotNull("Complex data should save successfully", id);
        
        Map<String, String> retrievedData = analyzerExperimentService.getWellValuesForId(id);
        assertEquals("Value,With,Commas", retrievedData.get("A1"));
        assertEquals("Another\"Quoted\"Value", retrievedData.get("B2"));
        assertEquals("Line\nBreaks", retrievedData.get("C3"));
        
        analyzerExperimentService.delete(analyzerExperimentService.get(id));
    }
}
