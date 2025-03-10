package org.openelisglobal.program;

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
    public void verifyTestData() {
        List<ProgramSample> programSamples = programSampleService.getAll();
        System.out.println("program samples we have in db: " + programSamples.size());
        programSamples.forEach(program -> System.out
                .println(program.getId() + " - " + program.getProgram() + " - " + program.getSample()));

        List<Program> programList = programService.getAll();
        System.out.println("programs we have in db: " + programList.size());
        programList.forEach(program -> System.out.println(program.getId() + " - " + program.getProgramName() + " - "
                + program.getTestSection().getTestSectionName()));

        List<Sample> samples = sampleService.getAll();
        System.out.println("samples we have in db: " + samples.size());
        samples.forEach(program -> System.out
                .println(program.getId() + " - " + program.getAccessionNumber() + " - " + program.getReceivedDate()));
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
