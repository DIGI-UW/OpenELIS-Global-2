package org.openelisglobal.program;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgramServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ProgramService programService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/program.xml");
    }

    @Test
    public void verifyTestData() {
        List<Program> programList = programService.getAll();
        assertNotNull("Program list should not be null", programList);
        assertFalse("Program list should not be empty", programList.isEmpty());

        programList.forEach(program -> {
            assertNotNull("Program ID should not be null", program.getId());
            assertNotNull("Program name should not be null", program.getProgramName());
            assertNotNull("Test section name should not be null", program.getTestSection());
            assertNotNull("Test section should have a name", program.getTestSection().getTestSectionName());
        });
    }

    @Test
    public void getAll_shouldGetAllPrograms() throws Exception {
        Assert.assertEquals(Integer.parseInt("5"), programService.getAll().size());
    }

    @Test
    public void create_shouldCreateProgram() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "program" });
        Program program = new Program();
        program.setProgramName("Viral Loading");
        program.setCode("PROG007");
        program.setManuallyChanged(false);

        programService.save(program);
        Assert.assertEquals("Viral Loading", program.getProgramName());
        Assert.assertEquals("PROG007", program.getCode());
        Assert.assertEquals(false, program.getManuallyChanged());
    }

    @Test
    public void update_shouldUpdateProgram() {
        Program program = programService.get("4");
        program.setProgramName("Microbiology Programme");
        programService.save(program);

        Assert.assertEquals("Microbiology Programme", program.getProgramName());
    }

    @Test
    public void delete_shouldDeleteProgram() {
        Program program = programService.get("5");
        programService.delete(program);

        Assert.assertEquals(Integer.parseInt("4"), programService.getAll().size());
    }

    @Test
    public void deleteAll_shouldDeleteAllPrograms() {
        List<Program> allPrograms = programService.getAll();
        programService.deleteAll(allPrograms);

        Assert.assertEquals(0, programService.getAll().size());
    }
}
