package org.openelisglobal.sampleqaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleQaEventServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleQaEventService qaEventService;

    private List<SampleQaEvent> sampleQaEvents;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-qa-event.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2024-06-25 09:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("completedDate");

    }

    @Test
    public void getData_ShouldReturnDataForASpecificSampleQaEvent() {
        SampleQaEvent sampleQaEvent = qaEventService.get("3");
        qaEventService.getData(sampleQaEvent);
        assertNotNull(sampleQaEvent);
        assertEquals(Timestamp.valueOf("2025-06-25 12:00:00"), sampleQaEvent.getEnteredDate());
    }

    @Test
    public void getData_ShouldReturnDataUsingASampleQaEventID() {
        SampleQaEvent sampleQaEvent = qaEventService.getData("2");
        assertNotNull(sampleQaEvent);
        assertEquals(Timestamp.valueOf("2025-06-25 10:15:00"), sampleQaEvent.getLastupdated());
    }

    @Test
    public void getAllUncompletedEvents_ShouldReturnAllUncompletedSampleQaEvents() {
        List<SampleQaEvent> sampleQaEvents = qaEventService.getAllUncompleatedEvents();
        assertNotNull(sampleQaEvents);
        assertEquals(0, sampleQaEvents.size());
    }

    @Test
    public void getSampleQaEventsBySample_ShouldReturnAllSampleQaEvents_UsingASample() {
        Sample sample = new Sample();
        sample.setId("402");
        List<SampleQaEvent> sampleQaEvents = qaEventService.getSampleQaEventsBySample(sample);
        assertNotNull(sampleQaEvents);
        assertEquals(2, sampleQaEvents.size());
        assertEquals(Timestamp.valueOf("2025-06-25 10:45:00"), sampleQaEvents.get(0).getEnteredDate());
    }

    @Test
    public void getSampleQaEventsByUpdatedDate_ShouldReturnAllSampleQaEventsThatLieInTheParameterUpdateDates()
            throws ParseException {
        List<SampleQaEvent> sampleQaEvents = qaEventService.getSampleQaEventsByUpdatedDate(Date.valueOf("2025-05-25"),
                Date.valueOf("2025-07-25"));
        assertNotNull(sampleQaEvents);
        assertEquals(1, sampleQaEvents.size());
        assertEquals("2", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getSampleQaEventBySampleAndQaEvent_ShouldReturnASampleQaEvent_UsingASampleAndAQaEvent() {
        SampleQaEvent sampleQaEvent = qaEventService.get("2");
        SampleQaEvent returnedSampleQaEvent = qaEventService.getSampleQaEventBySampleAndQaEvent(sampleQaEvent);
        assertNotNull(returnedSampleQaEvent);
        assertEquals("2", returnedSampleQaEvent.getId());
        assertEquals(Timestamp.valueOf("2025-06-25 10:45:00"), returnedSampleQaEvent.getEnteredDate());
    }

    @Test
    public void getAll_ShouldReturnAllSampleQaEvents() {
        sampleQaEvents = qaEventService.getAll();
        assertNotNull(sampleQaEvents);
        assertEquals(3, sampleQaEvents.size());
        assertEquals("3", sampleQaEvents.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleQaEvents_UsingPropertyNameAndValue() {
        sampleQaEvents = qaEventService.getAllMatching("enteredDate", Timestamp.valueOf("2025-06-25 12:00:00"));
        assertNotNull(sampleQaEvents);
        assertEquals(1, sampleQaEvents.size());
        assertEquals("3", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleQaEvents_UsingAMap() {
        sampleQaEvents = qaEventService.getAllMatching(propertyValues);
        assertNotNull(sampleQaEvents);
        assertEquals(2, sampleQaEvents.size());
        assertEquals("3", sampleQaEvents.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleQaEvents_UsingAnOrderProperty() {
        sampleQaEvents = qaEventService.getAllOrdered("enteredDate", false);
        assertNotNull(sampleQaEvents);
        assertEquals(3, sampleQaEvents.size());
        assertEquals("3", sampleQaEvents.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        sampleQaEvents = qaEventService.getAllOrdered(orderProperties, true);
        assertNotNull(sampleQaEvents);
        assertEquals(3, sampleQaEvents.size());
        assertEquals("3", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEvents_UsingPropertyNameAndValueAndAnOrderProperty() {
        sampleQaEvents = qaEventService.getAllMatchingOrdered("completedDate", Date.valueOf("2025-03-01"),
                "lastupdated", true);
        assertNotNull(sampleQaEvents);
        assertEquals(1, sampleQaEvents.size());
        assertEquals("1", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEvents_UsingPropertyNameAndValueAndAList() {
        sampleQaEvents = qaEventService.getAllMatchingOrdered("id", "2", orderProperties, true);
        assertNotNull(sampleQaEvents);
        assertEquals(1, sampleQaEvents.size());
        assertEquals("2", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEvents_UsingAMapAndAnOrderProperty() {
        sampleQaEvents = qaEventService.getAllMatchingOrdered(propertyValues, "enteredDate", true);
        assertNotNull(sampleQaEvents);
        assertEquals(2, sampleQaEvents.size());
        assertEquals("3", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEvents_UsingAMapAndAList() {
        sampleQaEvents = qaEventService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(sampleQaEvents);
        assertEquals(2, sampleQaEvents.size());
        assertEquals("1", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSampleQaEvents_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleQaEvents_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getMatchingPage("completedDate", Date.valueOf("2025-03-20"), 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleQaEvents_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleQaEvents_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleQaEvents_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleQaEvents_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getMatchingOrderedPage("lastupdated", Timestamp.valueOf("2025-06-25 10:15:00"),
                "lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleQaEvents_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getMatchingOrderedPage("enteredDate", Timestamp.valueOf("2025-06-25 09:30:00"),
                orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleQaEvents_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getMatchingOrderedPage(propertyValues, "lastupdated", false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleQaEvents_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEvents = qaEventService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEvents.size());
    }

    @Test
    public void delete_ShouldDeleteASampleQaEvent() {
        sampleQaEvents = qaEventService.getAll();
        assertEquals(3, sampleQaEvents.size());
        SampleQaEvent patientSampleQaEvent = qaEventService.get("2");
        qaEventService.delete(patientSampleQaEvent);
        List<SampleQaEvent> newSampleQaEvents = qaEventService.getAll();
        assertEquals(2, newSampleQaEvents.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSampleQaEvents() {
        sampleQaEvents = qaEventService.getAll();
        assertEquals(3, sampleQaEvents.size());
        qaEventService.deleteAll(sampleQaEvents);
        List<SampleQaEvent> updatedSampleQaEvents = qaEventService.getAll();
        assertTrue(updatedSampleQaEvents.isEmpty());
    }
}
