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
    private static int PAGE_SIZE = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/cron-scheduler.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastRun", Timestamp.valueOf("2025-06-24 00:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("jobName");
    }

    @Test
    public void getAll_ShouldReturnAllCronSchedulers() {
        cronSchedulerList = cronSchedulerService.getAll();
        assertNotNull(cronSchedulerList);
        assertEquals(3, cronSchedulerList.size());
        assertEquals("3", cronSchedulerList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCronSchedulers_UsingPropertyNameAndValue() {
        cronSchedulerList = cronSchedulerService.getAllMatching("name", "HalfHourlyJob");
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCronSchedulers_UsingAMap() {
        cronSchedulerList = cronSchedulerService.getAllMatching(propertyValues);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("3", cronSchedulerList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedCronSchedulers_UsingAnOrderProperty() {
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
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronSchedulers_UsingPropertyNameAndValueAndAnOrderProperty() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered("name", "HalfHourlyJob", "id", false);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronSchedulers_UsingPropertyNameAndValueAndAList() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered("name", "HalfHourlyJob", orderProperties, true);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("2", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronSchedulers_UsingAMapAndAnOrderProperty() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered(propertyValues, "cronStatement", false);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("1", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCronSchedulers_UsingAMapAndAList() {
        cronSchedulerList = cronSchedulerService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(cronSchedulerList);
        assertEquals(2, cronSchedulerList.size());
        assertEquals("1", cronSchedulerList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfCronSchedulers_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getPage(1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCronSchedulers_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingPage("displayKey", "cleanup.display", 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCronSchedulers_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCronSchedulers_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getOrderedPage("id", true, 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCronSchedulers_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronSchedulers_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage("name", "HalfHourlyJob", "id", true, 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronSchedulers_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage("name", "HalfHourlyJob", orderProperties, true,
                1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronSchedulers_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage(propertyValues, "lastRun", false, 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCronSchedulers_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        cronSchedulerList = cronSchedulerService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= cronSchedulerList.size());
    }

    @Test
    public void delete_ShouldDeleteACronScheduler() {
        cronSchedulerList = cronSchedulerService.getAll();
        assertEquals(3, cronSchedulerList.size());
        CronScheduler cronScheduler = cronSchedulerService.get("2");
        cronSchedulerService.delete(cronScheduler);
        List<CronScheduler> cronSchedulers = cronSchedulerService.getAll();
        assertEquals(2, cronSchedulers.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllCronSchedulers() {
        cronSchedulerList = cronSchedulerService.getAll();
        assertEquals(3, cronSchedulerList.size());
        cronSchedulerService.deleteAll(cronSchedulerList);
        List<CronScheduler> updatedCronSchedulers = cronSchedulerService.getAll();
        assertTrue(updatedCronSchedulers.isEmpty());
    }
}
