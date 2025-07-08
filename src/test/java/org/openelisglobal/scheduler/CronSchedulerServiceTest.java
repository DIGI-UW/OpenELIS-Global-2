package org.openelisglobal.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "facilitylist.schedule.fixedRate=864000000" })

public class CronSchedulerServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CronSchedulerService cronSchedulerService;

    private List<CronScheduler> cronSchedulerList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/cron-scheduler.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastRun", Timestamp.valueOf("2025-06-24 00:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("jobName");
    }

    @Test
    public void getAll_ShouldReturnAllCronStatements() {
        cronSchedulerList = cronSchedulerService.getAll();
        assertNotNull(cronSchedulerList);
        assertEquals(3, cronSchedulerList.size());
        assertEquals("3", cronSchedulerList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCronStatements_UsingPropertyNameAndValue() {
        cronSchedulerList = cronSchedulerService.getAllMatching("name", "HalfHourlyJob");
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCronStatements_UsingAMap() {
        cronSchedulerList = cronSchedulerService.getAllMatching(propertyValues);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("3", cronSchedulerList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedCronStatements_UsingAnOrderProperty() {
        cronSchedulerList = cronSchedulerService.getAllOrdered("cronStatement", false);
        assertNotNull(cronSchedulerList);
        assertEquals(3, cronSchedulerList.size());
        assertEquals("3", cronSchedulerList.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        cronSchedulerList = cronSchedulerService.getAllOrdered(orderProperties, true);
        assertNotNull(cronSchedulerList);
        assertEquals(3, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronStatements_UsingPropertyNameAndValueAndAnOrderProperty() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered("name", "HalfHourlyJob", "id", false);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronStatements_UsingPropertyNameAndValueAndAList() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered("name", "HalfHourlyJob", orderProperties, true);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronStatements_UsingAMapAndAnOrderProperty() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered(propertyValues, "cronStatement", false);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("1", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronStatements_UsingAMapAndAList() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("1", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfCronStatements_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCronStatements_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingPage("displayKey", "cleanup.display", 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCronStatements_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCronStatements_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getOrderedPage("id", true, 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCronStatements_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronStatements_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage("name", "HalfHourlyJob", "id", true, 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronStatements_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage("name", "HalfHourlyJob", orderProperties, true,
                1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronStatements_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage(propertyValues, "lastRun", false, 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronStatements_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= cronSchedulerList.size());
    }

    @Test
    public void delete_ShouldDeleteACronStatement() {
        cronSchedulerList = cronSchedulerService.getAll();
        assertEquals(3, cronSchedulerList.size());
        CronScheduler cronScheduler = cronSchedulerService.get("2");
        cronSchedulerService.delete(cronScheduler);
        List<CronScheduler> cronSchedulers = cronSchedulerService.getAll();
        assertEquals(2, cronSchedulers.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllCronStatements() {
        cronSchedulerList = cronSchedulerService.getAll();
        assertEquals(3, cronSchedulerList.size());
        cronSchedulerService.deleteAll(cronSchedulerList);
        List<CronScheduler> updatedCronSchedulers = cronSchedulerService.getAll();
        assertTrue(updatedCronSchedulers.isEmpty());
    }
}
