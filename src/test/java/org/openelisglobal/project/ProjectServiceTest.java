package org.openelisglobal.project;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    ProjectService pService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/project.xml");
    }

    @Test
    public void insertDuplicateProjectShouldThrowException() throws Exception {
        Project project = new Project();
        project.setProjectName("Integration Test Project");
        project.setLocalAbbreviation("1001");
        project.setIsActive("Y");
        project.setSysUserId("1");

        try {
            pService.insert(project);
            Assert.fail("Expected exception due to duplicate project");
        } catch (LIMSRuntimeException e) {
            Assert.assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("duplicate"));
        }
    }

    @Test
    public void getProjectByIdShouldReturnCorrectProject() throws Exception {
        Project project = pService.getProjectById("101");
        Assert.assertEquals("Integration Test Project", project.getProjectName());
    }

    @Test
    public void duplicateProjectInsertShouldThrowDuplicateException() throws Exception {
        Project project = new Project();
        project.setProjectName("Integration Test Project");
        project.setLocalAbbreviation("1001");
        project.setIsActive("Y");
        project.setSysUserId("1");

        try {
            pService.insert(project);
            Assert.fail("Expected exception due to duplicate project");
        } catch (LIMSRuntimeException e) {
            Assert.assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("duplicate"));
        }
    }

    @Test
    public void getAllProjectsShouldReturnAll() throws Exception {
        List<Project> projects = pService.getAllProjects();
        Assert.assertEquals(1, projects.size());
        Assert.assertEquals("Integration Test Project", projects.get(0).getProjectName());
    }

    @Test
    public void getProjectsByFilterShouldReturnFilteredProjects() throws Exception {
        List<Project> projects = pService.getProjects("Integration", true);
        Assert.assertEquals(1, projects.size());
        Assert.assertEquals("Integration Test Project", projects.get(0).getProjectName());
    }

    @Test
    public void getTotalProjectCountShouldReturnCount() throws Exception {
        int count = pService.getTotalProjectCount();
        Assert.assertEquals(1, count);
    }

    @Test
    public void getProjectByNameShouldReturnCorrectProject() throws Exception {
        Project project = new Project();
        project.setProjectName("Integration Test Project");
        project.setIsActive("Y");

        Project result = pService.getProjectByName(project, true, true);
        Assert.assertNotNull("Project should be found by name", result);
        Assert.assertEquals("Integration Test Project", result.getProjectName());
    }

    @Test
    public void getTotalProjectCountShouldReturnCorrectCount() throws Exception {
        int count = pService.getTotalProjectCount();
        Assert.assertEquals("Expected project count to be 1", 1, count);
    }

    @Test
    public void getProjectsByFilterShouldReturnFilteredResults() throws Exception {
        List<Project> projects = pService.getProjects("Integration", true);

        Assert.assertNotNull("Projects should not be null", projects);
        Assert.assertEquals("Expected 1 project", 1, projects.size());
        Assert.assertEquals("Integration Test Project", projects.get(0).getProjectName());
    }

}
