package org.openelisglobal.analyzer;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

        assertNotNull(id);

        AnalyzerExperiment savedExperiment = analyzerExperimentService.get(id);
        assertEquals("TestFile.csv", savedExperiment.getName());
        assertEquals(1, analyzerExperimentService.getAll().size());

        analyzerExperimentService.delete(savedExperiment);
    }

    @Test
    public void insert_shouldInsertAnalyzerExperiment() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analyzer_experiment" });
        AnalyzerExperiment newExperiment = new AnalyzerExperiment();
        newExperiment.setName("PCR Test Experiment");
        newExperiment.setFile("well,Sample Name\nD1,NewSample1\n".getBytes());
        assertEquals(0, analyzerExperimentService.getAll().size());

        Integer inserted = analyzerExperimentService.insert(newExperiment);
        AnalyzerExperiment insertedExperiment = analyzerExperimentService.get(inserted);

        assertNotNull(insertedExperiment);
        assertEquals("PCR Test Experiment", insertedExperiment.getName());
        assertEquals(1, analyzerExperimentService.getAll().size());

        analyzerExperimentService.delete(insertedExperiment);
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
        newExperiment.setFile("well,Sample Name\nD1,NewSample1\n".getBytes());
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
}
