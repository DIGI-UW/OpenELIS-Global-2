package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class NcEventServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NCEventService ncEventService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nc-event.xml");

    }

    @Test
    public void getAll_shouldReturnAllEvents() {
        List<NcEvent> ncEvents = ncEventService.getAll();
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());

    }

    @Test
    public void getAllMatching() {
        List<NcEvent> ncEvents = ncEventService.getAllMatching("name", "Sample Mislabeling");
        assertEquals(1, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());

    }

    @Test
    public void getAllMatchingGivenMap() {
        Map<String, Object> map = Map.of("name", "Sample Mislabeling");
        List<NcEvent> ncEvents = ncEventService.getAllMatching(map);
        assertEquals(1, ncEvents.size());
    }

    @Test
    public void getAllOrdered() {
        List<NcEvent> ncEvents = ncEventService.getAllOrdered("id", false);
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());
    }

    @Test
    public void getAllOrderedGivenList() {
        List<String> list = List.of("id");
        List<NcEvent> ncEvents = ncEventService.getAllOrdered(list, false);
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());
    }

    @Test
    public void getAllOrderedMatching() {
        List<String> list = List.of("id");
        List<NcEvent> ncEvents = ncEventService.getAllMatchingOrdered("name", "Sample Mislabeling", list, false);
        assertEquals(1, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
    }

    @Test
    public void getAllOrderedMatchingGivenMap() {
        Map<String, Object> map = Map.of("name", "Sample Mislabeling");
        List<NcEvent> ncEvents = ncEventService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
    }

    @Test
    public void getAllOrderedMatchingGivenList() {
        List<String> list = List.of("id");
        Map<String, Object> map = Map.of("name", "Sample Mislabeling");
        List<NcEvent> ncEvents = ncEventService.getAllMatchingOrdered(map, list, false);
        assertEquals(1, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
    }

    @Test
    public void getPaged() {
        List<NcEvent> ncEvents = ncEventService.getPage(1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);

    }

    @Test
    public void getMatchingPaged() {
        List<NcEvent> ncEvents = ncEventService.getMatchingPage("name", "Sample Mislabeling", 1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);
    }

    @Test
    public void getMatchingPagedGivenMap() {
        Map<String, Object> map = Map.of("name", "Sample Mislabeling");
        List<NcEvent> ncEvents = ncEventService.getMatchingPage(map, 1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);
    }

    @Test
    public void getPagedOrdered() {
        List<String> list = List.of("id");
        List<NcEvent> ncEvents = ncEventService.getOrderedPage(list, false, 1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);
    }

    @Test
    public void getPagedOrderedMatching() {
        List<String> list = List.of("id");
        List<NcEvent> ncEvents = ncEventService.getMatchingOrderedPage("name", "Sample Mislabeling", list, false, 1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);
    }

    @Test
    public void getPagedOrderedMatchingGivenMap() {
        Map<String, Object> map = Map.of("name", "Sample Mislabeling");
        List<String> list = List.of("id");
        List<NcEvent> ncEvents = ncEventService.getMatchingOrderedPage(map, list, false, 1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);
    }

    @Test
    public void getOrderedMatchingPage() {
        Map<String, Object> map = Map.of("name", "Sample Mislabeling");
        List<NcEvent> ncEvents = ncEventService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPage = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= expectedPage);
    }

    @Test
    public void getCount() {
        int count = ncEventService.getCount();
        assertEquals(3, count);
    }

    @Test
    public void getCountLike() {
        int count = ncEventService.getCountLike("name", "Sample Mislabeling");
        assertEquals(1, count);
    }

    @Test
    public void deleteAll() {
        List<NcEvent> ncEvents = ncEventService.getAll();
        ncEventService.deleteAll(ncEvents);
        List<NcEvent> ncEvent = ncEventService.getAll();
        assertEquals(0, ncEvent.size());
    }

    @Test
    public void delete() {
        NcEvent ncEvent = ncEventService.get("1");
        ncEventService.delete(ncEvent);
        List<NcEvent> ncEvents = ncEventService.getAll();
        assertEquals(2, ncEvents.size());
    }

}