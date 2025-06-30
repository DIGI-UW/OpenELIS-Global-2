package org.openelisglobal.pathology;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.dbunit.DatabaseUnitException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

public class PathologySampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PathologySampleService pathologySampleService;

    @Autowired
    SystemUserService systemUserService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
    }

    @Test
    public void getAll_shouldReturnAllExistingPathologySamples() {
        Assert.assertEquals(Integer.parseInt("2"), pathologySampleService.getAll().size());
    }

    @Test
    public void getWithStatus_shouldReturnPathologySampleWhenGivenStatus() {
        List<PathologySample> pathologySamples = pathologySampleService
                .getWithStatus(List.of(PathologySample.PathologyStatus.GROSSING));

        Assert.assertNotNull(pathologySamples);
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySamples.get(0).getStatus());
        Assert.assertEquals("John", pathologySamples.get(0).getTechnician().getFirstName());
    }

    @Test
    public void searchWithStatusAndTerm_shouldSearchGivenStatusAndSearchTerm() {
        List<PathologySample> pathologySamples = pathologySampleService
                .searchWithStatusAndTerm(List.of(PathologySample.PathologyStatus.GROSSING), "");

        Assert.assertNotNull(pathologySamples);
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySamples.get(0).getStatus());
        Assert.assertEquals("John", pathologySamples.get(0).getTechnician().getFirstName());
    }

    @Test
    public void assignTechnician_shouldAssignTechnicianToPathologySample() throws SQLException, DatabaseUnitException {
        SystemUser curUser = systemUserService.getUserById("1004");
        pathologySampleService.assignTechnician(2, curUser, "");

        PathologySample pathologySample = pathologySampleService.get(1);

        Assert.assertEquals(Integer.valueOf(1), pathologySample.getId());
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySample.getStatus());
        Assert.assertEquals("John", pathologySample.getTechnician().getFirstName());
        Assert.assertEquals(Integer.parseInt("1"), pathologySample.getBlocks().size());
    }

    @Test
    public void assignPathologist_shouldAssignPathologistToPathologySample() {
        SystemUser curUser = systemUserService.getUserById("1004");
        pathologySampleService.assignPathologist(2, curUser, "");

        PathologySample pathologySample = pathologySampleService.get(1);

        Assert.assertEquals(Integer.valueOf(1), pathologySample.getId());
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySample.getStatus());
        Assert.assertEquals("John", pathologySample.getTechnician().getFirstName());
    }

    @Test
    public void getCountWithStatus_shouldCountPathologySampleWithStatus() {
        Long pathologyStatuses = pathologySampleService
                .getCountWithStatus(Collections.singletonList(PathologySample.PathologyStatus.GROSSING));

        Assert.assertEquals(Long.valueOf(2), pathologyStatuses);
    }

    @Test
    public void getCountWithStatusBetweenDates_shouldCountPathologySampleWithStatusBetweenDates() {
        Long expectedCount = 2L;
        Long countWithStatusBetweenDates = pathologySampleService.getCountWithStatusBetweenDates(
                Collections.singletonList(PathologySample.PathologyStatus.GROSSING),
                Timestamp.valueOf("2024-06-10 12:00:00.0"), Timestamp.valueOf("2024-07-10 12:00:00.0"));

        Assert.assertEquals(expectedCount, countWithStatusBetweenDates);
    }

    @Test
    public void updateWithFormValues_shouldUpdatePathologySampleWithFormValues() {
        PathologySampleForm pathologySampleForm = new PathologySampleForm();
        pathologySampleForm.setSystemUserId("2");

        PathologyBlock block1 = new PathologyBlock();
        block1.setBlockNumber(12);
        block1.setLocation("Lab 2");

        PathologyBlock block2 = new PathologyBlock();
        block2.setBlockNumber(13);
        block2.setLocation("Lab 3");

        List<PathologyBlock> pathologyBlocks = Arrays.asList(block1, block2);

        pathologySampleForm.setBlocks(pathologyBlocks);
        pathologySampleForm.setSlides(Collections.singletonList(new PathologySampleForm.PathologySlideForm()));
        pathologySampleForm.setReports(Collections.singletonList(new PathologySampleForm.PathologyReportForm()));
        pathologySampleService.updateWithFormValues(2, pathologySampleForm);

        Assert.assertEquals(Integer.parseInt("2"), pathologySampleForm.getBlocks().size());
        Assert.assertEquals("2", pathologySampleForm.getSystemUserId());
    }

}
