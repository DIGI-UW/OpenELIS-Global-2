package org.openelisglobal.program;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgramSampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ProgramSampleService programSampleService;

    @Autowired
    ProgramService programService;

    @Autowired
    SampleService sampleService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/program-sample.xml");
    }

    @Test
    public void verifyProgramSampleData() {
        List<ProgramSample> programSamples = programSampleService.getAll();

        assertNotNull("Program samples should not be null", programSamples);
        assertFalse("Program samples should not be empty", programSamples.isEmpty());

        for (ProgramSample programSample : programSamples) {
            assertNotNull("ProgramSample ID should not be null", programSample.getId());
            assertNotNull("Program should not be null for ProgramSample", programSample.getProgram());
            assertNotNull("Sample should not be null for ProgramSample", programSample.getSample());
        }
    }

    @Test
    public void verifyProgramData() {
        List<Program> programList = programService.getAll();

        assertNotNull("Programs should not be null", programList);
        assertFalse("Programs should not be empty", programList.isEmpty());

        for (Program program : programList) {
            assertNotNull("Program ID should not be null", program.getId());
            assertNotNull("Program name should not be null", program.getProgramName());
            assertNotNull("Test section should not be null", program.getTestSection());
            assertNotNull("Test section name should not be null", program.getTestSection().getTestSectionName());
        }
    }

    @Test
    public void verifySampleData() {
        List<Sample> samples = sampleService.getAll();

        assertNotNull("Samples should not be null", samples);
        assertFalse("Samples should not be empty", samples.isEmpty());

        for (Sample sample : samples) {
            assertNotNull("Sample ID should not be null", sample.getId());
            assertNotNull("Accession number should not be null", sample.getAccessionNumber());
            assertNotNull("Received date should not be null", sample.getReceivedDate());
        }
    }

    @Test
    public void getProgrammeSampleBySample_shouldReturnProgramSampleWhenGivenSample() {
        ProgramSample programSample = programSampleService.getProgrammeSampleBySample(1, "Blood Test Program");

        Assert.assertNotNull(programSample);
        Assert.assertEquals(Integer.valueOf("1"), programSample.getId());
    }

    @Test
    public void getAll_shouldGetAllProgramSamples() throws Exception {
        Assert.assertEquals(Integer.parseInt("3"), programSampleService.getAll().size());
    }

    @Test
    public void create_shouldCreateProgramSample() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "program_sample" });
        ProgramSample programSample = new ProgramSample();
        programSample.setProgram(programService.get("1"));
        programSample.setSample(sampleService.get("1"));
        programSampleService.save(programSample);

        Assert.assertEquals("Blood Test Program", programSample.getProgram().getProgramName());
        Assert.assertEquals("12345", programSample.getSample().getAccessionNumber());
    }

    @Test
    public void update_shouldUpdateProgramSample() {
        ProgramSample programSample = programSampleService.get(3);
        programSample.setSample(sampleService.get("2"));
        programSampleService.save(programSample);

        Assert.assertEquals("13333", programSample.getSample().getAccessionNumber());
    }

    @Test
    public void delete_shouldDeleteProgramSample() {
        ProgramSample program = programSampleService.get(3);
        programSampleService.delete(program);

        Assert.assertEquals(2, programSampleService.getAll().size());
    }

    @Test
    public void deleteAll_shouldDeleteAllProgramSamples() {
        List<ProgramSample> allPrograms = programSampleService.getAll();
        programSampleService.deleteAll(allPrograms);

        Assert.assertEquals(0, programSampleService.getAll().size());
    }
}
