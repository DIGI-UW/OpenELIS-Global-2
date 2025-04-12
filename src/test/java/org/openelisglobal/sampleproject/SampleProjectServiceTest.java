package org.openelisglobal.sampleproject;

import static org.junit.Assert.*;

import jakarta.persistence.PersistenceException;
import java.sql.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleProjectServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleProjectService sampleProjectService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/sampleproject.xml");
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnCorrectSampleProject() {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId("1");

        assertNotNull(sampleProject);
        assertEquals("1", sampleProject.getSample().getId());
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnSingleSampleProject() {
        SampleProject result = sampleProjectService.getSampleProjectBySampleId("1");
        assertNotNull(result);
        assertNotNull(result.getSample());
        assertEquals("1", result.getSample().getId().toString());
        assertNotNull(result.getProject());
        assertEquals("1", result.getProject().getId().toString());
    }

    @Test
    public void getData_shouldPopulateSampleProject() {
        SampleProject emptyProject = new SampleProject();
        emptyProject.setId("1");

        sampleProjectService.getData(emptyProject);

        assertNotNull(emptyProject.getProject());
        assertEquals("1", emptyProject.getProject().getId().toString());
    }

    @Test
    public void getData_shouldHandleNonExistentId() {
        SampleProject nonExistent = new SampleProject();
        nonExistent.setId("999");

        sampleProjectService.getData(nonExistent);

        assertNull(nonExistent.getId());
    }

    @Test
    public void getSampleProjectBySampleId_shouldThrowForNullInput() {

        try {
            sampleProjectService.getSampleProjectBySampleId(null);
            fail("Expected exception not thrown");
        } catch (Exception e) {

            assertTrue(e instanceof PersistenceException || e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnNullForNonExistentSample() {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId("999");
        assertNull(sampleProject);
    }

    @Test
    public void updateSampleProject_shouldModifyExistingSampleProject() {
        SampleProject existingProject = sampleProjectService.getSampleProjectBySampleId("1");
        existingProject.setIsPermanent("N");
        sampleProjectService.save(existingProject);

        SampleProject updatedProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertEquals("N", updatedProject.getIsPermanent());
    }

    @Test
    public void getByOrganizationProjectAndReceivedOnRange_shouldReturnEmptyListForInvalidRange() {
        Date lowDate = Date.valueOf("2024-01-01");
        Date highDate = Date.valueOf("2024-02-01");
        List<SampleProject> projects = sampleProjectService.getByOrganizationProjectAndReceivedOnRange("1",
                "Test Project", lowDate, highDate);
        assertNotNull(projects);
        assertTrue(projects.isEmpty());
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnNullIfNotFound() {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId("999"); // Assuming "999" does not
                                                                                              // exist.
        assertNull(sampleProject);
    }

    @Test
    public void getData_shouldSetIdToNullIfNoDataFound() {
        SampleProject sampleProject = new SampleProject();
        sampleProject.setId("999");

        sampleProjectService.getData(sampleProject);
        assertNull(sampleProject.getId());
    }

    @Test
    public void updateSampleProject_shouldHandleNullValuesGracefully() {
        SampleProject existingProject = sampleProjectService.getSampleProjectBySampleId("1");
        existingProject.setIsPermanent(null);
        sampleProjectService.save(existingProject);

        SampleProject updatedProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertNull(updatedProject.getIsPermanent());
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnEmptyForEmptyDatabase() {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId("1");

        assertNotNull(sampleProject);
        assertNull(sampleProject.getSampleId());
        assertNull(sampleProject.getProjectId());
    }

    @Test
    public void getAllSampleProjects_shouldReturnNonEmptyList() {
        List<SampleProject> projects = sampleProjectService.getAll();
        assertNotNull(projects);
        assertFalse(projects.isEmpty());
    }

    @Test
    public void deleteSampleProject_shouldRemoveProject() {
        SampleProject existingProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertNotNull(existingProject);

        sampleProjectService.delete(existingProject);

        SampleProject deletedProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertNull(deletedProject);
    }

}
