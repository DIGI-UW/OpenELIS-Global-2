package org.openelisglobal.analysisqaevent;

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
import org.openelisglobal.analysisqaevent.service.AnalysisQaEventService;
import org.openelisglobal.analysisqaevent.valueholder.AnalysisQaEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalysisQaEventServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisQaEventService analysisQaEventService;

    private List<AnalysisQaEvent> analysisQaEventList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 1;

    @Before
    public void setUp() throws Exception {

        // TODO: I noticed there is a column named "lastupdated" in the DB
        // but its nit registered as a field in the Entity
        executeDataSetWithStateManagement("testdata/analysis-qa-event.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("completedDate", Timestamp.valueOf("2025-06-23 15:30:00"));

        orderProperties = new ArrayList<>();
        orderProperties.add("completedDate");
    }

    @Test
    public void testGetAllEvents() {
        analysisQaEventList = analysisQaEventService.getAll();
        assertNotNull(analysisQaEventList);
        assertEquals(3, analysisQaEventList.size());
        assertEquals("1", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllMatching() {
        analysisQaEventList = analysisQaEventService.getAllMatching("completedDate",
                Timestamp.valueOf("2025-06-23 15:30:00"));
        assertNotNull(analysisQaEventList);
        assertEquals(2, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllMatching_UsingMap() {
        analysisQaEventList = analysisQaEventService.getAllMatching(propertyValues);
        assertNotNull(analysisQaEventList);
        assertEquals(2, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllOrdered() {
        analysisQaEventList = analysisQaEventService.getAllOrdered("completedDate", false);
        assertNotNull(analysisQaEventList);
        assertEquals(3, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllOrdered_UsingList() {
        analysisQaEventList = analysisQaEventService.getAllOrdered(orderProperties, false);
        assertNotNull(analysisQaEventList);
        assertEquals(3, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered() {
        analysisQaEventList = analysisQaEventService.getAllMatchingOrdered("completedDate",
                Timestamp.valueOf("2025-06-23 15:30:00"), "completedDate", false);
        assertNotNull(analysisQaEventList);
        assertEquals(2, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingList() {
        analysisQaEventList = analysisQaEventService.getAllMatchingOrdered("completedDate",
                Timestamp.valueOf("2025-06-23 15:30:00"), orderProperties, false);
        assertNotNull(analysisQaEventList);
        assertEquals(2, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingMap() {
        analysisQaEventList = analysisQaEventService.getAllMatchingOrdered(propertyValues, "completedDate", false);
        assertNotNull(analysisQaEventList);
        assertEquals(2, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingMapAndList() {
        analysisQaEventList = analysisQaEventService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(analysisQaEventList);
        assertEquals(2, analysisQaEventList.size());
        assertEquals("2", analysisQaEventList.get(0).getId());
    }

    @Test
    public void testGetPage() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetMatchingPage() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getMatchingPage("completedDate",
                Timestamp.valueOf("2025-06-23 15:30:00"), 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetMatchingPage_UsingMap() {
        analysisQaEventList = analysisQaEventService.getMatchingPage(propertyValues, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetOrderedPage() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getOrderedPage("completedDate", false, 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetOrderedPage_UsingList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetOrderedPage_UsingPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getMatchingOrderedPage("completedDate",
                Timestamp.valueOf("2025-06-23 15:30:00"), "completedDate", true, 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetMatchingOrderedPage_UsingList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getMatchingOrderedPage("completedDate",
                Timestamp.valueOf("2025-06-23 15:30:00"), orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetMatchingOrderedPage_UsingMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getMatchingOrderedPage(propertyValues, "completedDate", false, 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }

    @Test
    public void testGetMatchingOrderedPage_UsingMapAndList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analysisQaEventList = analysisQaEventService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventList.size());
    }
}
