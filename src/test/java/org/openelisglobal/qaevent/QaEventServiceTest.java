package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.qaevent.service.QaEventService;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class QaEventServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QaEventService qaEventService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qa-event.xml");
    }

    @Test
    public void getAll_shouldReturnAllQaEvents() throws Exception {
        List<QaEvent> events = qaEventService.getAll();
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("1", events.get(0).getId());
        assertEquals("2", events.get(1).getId());
    }

    @Test
    public void getData_shouldReturnQaEvent() throws Exception {
        QaEvent qaEvent = qaEventService.get("1");
        qaEventService.getData(qaEvent);
        assertNotNull(qaEvent);
        assertEquals("1", qaEvent.getId());
        assertEquals("QA Event 1", qaEvent.getQaEventName());
    }

    @Test
    public void getQaEventByName_shouldReturnQaEvent() throws Exception {
        QaEvent qaEvent = new QaEvent();
        qaEvent.setQaEventName("QA Event 1");
        qaEvent = qaEventService.getQaEventByName(qaEvent);
        assertNotNull(qaEvent);
        assertEquals("1", qaEvent.getId());
        assertEquals("QA Event 1", qaEvent.getQaEventName());
    }

    @Test
    public void getAllQaEvents_shouldReturnAllQaEvents() throws Exception {
        List<QaEvent> events = qaEventService.getAllQaEvents();
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("1", events.get(0).getId());
        assertEquals("2", events.get(1).getId());
    }

    @Test
    public void getTotalQaEventCount_shouldReturnTotalQaEventCount() throws Exception {
        Integer count = qaEventService.getTotalQaEventCount();
        assertNotNull(count);
        assertEquals(2, count.intValue());
    }

    @Test
    public void getPageOfQaEvents_shouldReturnPageOfQaEvents() throws Exception {
        List<QaEvent> events = qaEventService.getPageOfQaEvents(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getAllMachingQaEvents_shouldReturnAllMatchingQaEvents() throws Exception {
        List<QaEvent> events = qaEventService.getAllMatching("isBillable", "Y");
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("1", events.get(0).getId());
    }

    @Test
    public void getAllMatching_givenMapping_shouldReturnAllMatchingQaEvents() throws Exception {
        Map<String, Object> mapping = Map.of("isBillable", "Y");
        List<QaEvent> events = qaEventService.getAllMatching(mapping);
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("1", events.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_givenMapping_shouldReturnAllMatchingOrderedQaEvents() throws Exception {
        Map<String, Object> mapping = Map.of("isBillable", "Y");
        List<QaEvent> events = qaEventService.getAllMatchingOrdered(mapping, "id", true);
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("1", events.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrdered() {
        List<QaEvent> events = qaEventService.getAllMatchingOrdered("isBillable", "Y", "id", true);
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("1", events.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenLIst_shouldReturnAllMatchingOrderedQaEvents() {
        List<String> list = List.of("id");
        List<QaEvent> events = qaEventService.getAllMatchingOrdered("isBillable", "Y", list, true);
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("1", events.get(0).getId());

    }

    @Test
    public void getAllMatchingOrderedGivenLIstAndMapping_shouldReturnAllMatchingOrderedQaEvents() {
        Map<String, Object> mapping = Map.of("isBillable", "Y");
        List<String> list = List.of("id");
        List<QaEvent> events = qaEventService.getAllMatchingOrdered(mapping, list, true);
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("1", events.get(0).getId());
    }

    @Test
    public void getOrdered_shouldReturnOrdered() {
        List<QaEvent> events = qaEventService.getAllOrdered("id", false);
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("1", events.get(0).getId());
        assertEquals("2", events.get(1).getId());
    }

    @Test
    public void getOrderedGivenLIst_shouldReturnOrdered() {
        List<String> list = List.of("id");
        List<QaEvent> events = qaEventService.getAllOrdered(list, false);
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("1", events.get(0).getId());
        assertEquals("2", events.get(1).getId());
    }

    @Test
    public void getAllMatchingPage_shouldReturnAllMatchingPage() {
        List<QaEvent> events = qaEventService.getMatchingPage("isBillable", "Y", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getAllMatchingPageGivenMapping_shouldReturnAllMatchingPage() {
        Map<String, Object> mapping = Map.of("isBillable", "Y");
        List<QaEvent> events = qaEventService.getMatchingPage(mapping, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getAllMatchingPageOrdered_shouldReturnAllMatchingPageOrdered() {
        List<QaEvent> events = qaEventService.getMatchingOrderedPage("isBillable", "Y", "id", true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getAllOrderedPage_shouldReturnAllOrderedPage() {
        List<QaEvent> events = qaEventService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getAllOrderedPageGivenLIst_shouldReturnAllOrderedPage() {
        List<String> list = List.of("id");
        List<QaEvent> events = qaEventService.getOrderedPage(list, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getPage_shouldReturnPageOfQaEvents() {
        List<QaEvent> events = qaEventService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= events.size());
    }

    @Test
    public void getNext_shouldReturnNextQaEvent() throws Exception {
        QaEvent qaEvent = qaEventService.getNext("1");
        assertNotNull(qaEvent);
        assertEquals("2", qaEvent.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousQaEvent() throws Exception {
        QaEvent qaEvent = qaEventService.getPrevious("2");
        assertNotNull(qaEvent);
        assertEquals("1", qaEvent.getId());
    }

    @Test
    public void getTotalQaEventCount_shouldMatchListSize() {
        List<QaEvent> list = qaEventService.getAll();
        int count = qaEventService.getTotalQaEventCount();
        assertEquals(list.size(), count);
    }

    @Test
    public void getCountMatching() {
        List<QaEvent> events = qaEventService.getAllMatching("isBillable", "Y");
        int count = qaEventService.getCountMatching("isBillable", "Y");
        assertEquals(events.size(), count);
    }

    @Test
    public void getCountMatchingGivenMapping() {
        Map<String, Object> mapping = Map.of("isBillable", "Y");
        List<QaEvent> events = qaEventService.getAllMatching(mapping);
        int count = qaEventService.getCountMatching(mapping);
        assertEquals(events.size(), count);
    }

    @Test
    public void getCountLike_shouldReturnCountLike() {
        List<QaEvent> events = qaEventService.getAllMatching("isBillable", "Y");
        int count = qaEventService.getCountLike("isBillable", "Y");
        assertEquals(events.size(), count);
    }

    @Test
    public void getCountLikeGivenMapping_shouldReturnCountLike() {
        List<QaEvent> events = qaEventService.getAllMatching("isBillable", "Y");
        Map<String, String> mapping = Map.of("isBillable", "Y");
        int count = qaEventService.getCountLike(mapping);
        assertEquals(events.size(), count);
    }

    @Test
    public void getCount_shouldReturnTotalQaEvent() {
        List<QaEvent> events = qaEventService.getAll();
        int count = qaEventService.getCount();
        assertEquals(events.size(), count);
    }

}
