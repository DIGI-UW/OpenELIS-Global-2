package org.openelisglobal.cytology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

public class CytologySampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CytologySampleService cytologySampleService;

    @Autowired
    private SystemUserService systemUserService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/cytology.xml");
    }

    @Test
    public void getAll_shouldReturnAllCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAll();
        assertNotNull(cytologySamples);
        assertEquals(3, cytologySamples.size());
        assertEquals(1, cytologySamples.get(0).getId().intValue());
        assertEquals(2, cytologySamples.get(1).getId().intValue());
        assertEquals(3, cytologySamples.get(2).getId().intValue());

    }

    @Test
    public void getAllMatching_shouldReturnMatchingCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatching("status", "COMPLETED");
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatching_givenMapping_shouldReturnMatchingCytologySamples() {
        Map<String, Object> mapping = Map.of("status", "COMPLETED");
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatching(mapping);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatchingPage_shouldReturnMatchingCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingPage("status", "COMPLETED", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());

    }

    @Test
    public void getAllMatchingPage_givenMapping_shouldReturnMatchingCytologySamples() {
        Map<String, Object> mapping = Map.of("status", "COMPLETED");
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingPage(mapping, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllOrdered_shouldReturnOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAllOrdered("id", false);
        assertNotNull(cytologySamples);
        assertEquals(3, cytologySamples.size());
        assertEquals(1, cytologySamples.get(0).getId().intValue());
        assertEquals(2, cytologySamples.get(1).getId().intValue());
        assertEquals(3, cytologySamples.get(2).getId().intValue());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getAllOrdered(orderBy, false);
        assertNotNull(cytologySamples);
        assertEquals(3, cytologySamples.size());
        assertEquals(1, cytologySamples.get(0).getId().intValue());
        assertEquals(2, cytologySamples.get(1).getId().intValue());
        assertEquals(3, cytologySamples.get(2).getId().intValue());
    }

    @Test
    public void getAllOrderedPage_shouldReturnOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllOrderedPageGivenList_shouldReturnOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatchingOrdered("status", "COMPLETED", "id",
                false);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnMatchingOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatchingOrdered("status", "COMPLETED",
                orderBy, false);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnMatchingOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingOrderedPage("status", "COMPLETED", "id",
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllMatchingOrderedPageGivenList_shouldReturnMatchingOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingOrderedPage("status", "COMPLETED",
                orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getWithStatus_shouldReturnCytologySamplesWithStatus() {
        List<CytologyStatus> status = List.of(CytologyStatus.COMPLETED);
        List<CytologySample> cytologySamples = cytologySampleService.getWithStatus(status);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getPage_shouldReturnPageOfCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void get_shouldReturnCytologySampleById() {
        CytologySample cytologySample = cytologySampleService.get(1);
        assertNotNull(cytologySample);
        assertEquals(1, cytologySample.getId().intValue());
    }

    @Test
    public void getCount_shouldReturnCountOfCytologySamples() {
        int count = cytologySampleService.getCount();
        assertEquals(3, count);
    }

    @Test
    public void deleteAll_shouldDeleteAllCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAll();
        assertEquals(3, cytologySamples.size());
        cytologySampleService.deleteAll(cytologySamples);
        int count = cytologySampleService.getCount();
        assertEquals(0, count);
    }

    @Test
    public void delete_shouldDeleteCytologySample() {
        CytologySample cytologySample = cytologySampleService.get(1);
        assertNotNull(cytologySample);
        cytologySampleService.delete(cytologySample);
        int count = cytologySampleService.getCount();
        assertEquals(2, count);
    }

    @Test
    public void getNextId_shouldReturnNextCytologySampleId() {
        CytologySample nextId = cytologySampleService.getNext("1");
        assertEquals(2, nextId.getId().intValue());
    }

    @Test
    public void getPrevious_id_shouldReturnPreviousCytologySampleId() {
        CytologySample previousId = cytologySampleService.getPrevious("3");
        assertEquals(1, previousId.getId().intValue());
    }

    @Test
    public void getCountLike_shouldReturnCountOfCytologySamplesLike() {
        int count = cytologySampleService.getCountLike("status", "COMPLETED");
        assertEquals(1, count);
    }

    @Test
    public void getCountLikeGivenMapping_shouldReturnCountOfCytologySamplesLike() {
        Map<String, String> mapping = Map.of("status", "COMPLETED");
        int count = cytologySampleService.getCountLike(mapping);
        assertEquals(1, count);
    }

    @Test
    public void getCountMatching_shouldReturnCountOfCytologySamplesMatching() {
        int count = cytologySampleService.getCountMatching("status", "COMPLETED");
        assertEquals(1, count);
    }

    @Test
    public void getCountMatchingGivenMapping_shouldReturnCountOfCytologySamplesMatching() {
        Map<String, Object> mapping = Map.of("status", "COMPLETED");
        int count = cytologySampleService.getCountMatching(mapping);
        assertEquals(1, count);
    }

    @Test
    public void assignTechnician_shouldAssignTechnicianToCytologySample() {
        SystemUser systemUser = systemUserService.get("1");
        cytologySampleService.assignTechnician(2, systemUser);
        CytologySample cytologySample = cytologySampleService.get(2);
        assertEquals("1", cytologySample.getTechnician().getId());
    }

    @Test
    public void assignCycoPathologist_shouldAssignCycoPathologistToCytologySample() {
        SystemUser systemUser = systemUserService.get("1");
        cytologySampleService.assignCytoPathologist(2, systemUser);
        CytologySample cytologySample = cytologySampleService.get(2);
        assertEquals("1", cytologySample.getCytoPathologist().getId());
    }

    @Test
    public void getCountWithStatusesBetweenDates_shouldReturnCountOfCytologySamplesByStatusesBetweenDates() {
        Timestamp from = Timestamp.valueOf("2024-01-01 09:00:00");
        Timestamp to = Timestamp.valueOf("2025-04-10 12:00:00");
        List<CytologyStatus> statuses = List.of(CytologyStatus.COMPLETED);
        Long count = cytologySampleService.getCountWithStatusBetweenDates(statuses, from, to);
        assertEquals(1, count.longValue());
    }

}
