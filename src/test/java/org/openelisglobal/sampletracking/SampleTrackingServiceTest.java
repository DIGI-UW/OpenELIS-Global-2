package org.openelisglobal.sampletracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.sampletracking.service.SampleTrackingService;
import org.openelisglobal.sampletracking.valueholder.SampleTracking;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleTrackingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTrackingService sampleTrackingService;

    private List<SampleTracking> sampleTrackingList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-tracking.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("sosDesc", "Scheduled");
        orderProperties = new ArrayList<>();
        orderProperties.add("patientId");
    }

    @Test
    public void getAll_ShouldReturnAllSampleTrackings() {
        sampleTrackingList = sampleTrackingService.getAll();
        assertNotNull(sampleTrackingList);
        assertEquals(6, sampleTrackingList.size());
        assertEquals("9006", sampleTrackingList.get(5).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleTrackings_UsingPropertyNameAndValue() {
        sampleTrackingList = sampleTrackingService.getAllMatching("sosDesc", "Emergency");
        assertNotNull(sampleTrackingList);
        assertEquals(2, sampleTrackingList.size());
        assertEquals("9004", sampleTrackingList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleTrackings_UsingAMap() {
        sampleTrackingList = sampleTrackingService.getAllMatching(propertyValues);
        assertNotNull(sampleTrackingList);
        assertEquals(1, sampleTrackingList.size());
        assertEquals("9002", sampleTrackingList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleTrackings_UsingAnOrderProperty() {
        sampleTrackingList = sampleTrackingService.getAllOrdered("patientId", false);
        assertNotNull(sampleTrackingList);
        assertEquals(6, sampleTrackingList.size());
        assertEquals("9004", sampleTrackingList.get(3).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        sampleTrackingList = sampleTrackingService.getAllOrdered(orderProperties, true);
        assertNotNull(sampleTrackingList);
        assertEquals(6, sampleTrackingList.size());
        assertEquals("9003", sampleTrackingList.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTrackings_UsingPropertyNameAndValueAndAnOrderProperty() {
        sampleTrackingList = sampleTrackingService.getAllMatchingOrdered("sosDesc", "Emergency", "collDate", true);
        assertNotNull(sampleTrackingList);
        assertEquals(2, sampleTrackingList.size());
        assertEquals("9004", sampleTrackingList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTrackings_UsingPropertyNameAndValueAndAList() {
        sampleTrackingList = sampleTrackingService.getAllMatchingOrdered("sosDesc", "Emergency", orderProperties, true);
        assertNotNull(sampleTrackingList);
        assertEquals(2, sampleTrackingList.size());
        assertEquals("9004", sampleTrackingList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTrackings_UsingAMapAndAnOrderProperty() {
        sampleTrackingList = sampleTrackingService.getAllMatchingOrdered(propertyValues, "patientId", true);
        assertNotNull(sampleTrackingList);
        assertEquals(1, sampleTrackingList.size());
        assertEquals("9002", sampleTrackingList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTrackings_UsingAMapAndAList() {
        sampleTrackingList = sampleTrackingService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(sampleTrackingList);
        assertEquals(1, sampleTrackingList.size());
        assertEquals("9002", sampleTrackingList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSampleTrackings_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getPage(1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleTrackings_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getMatchingPage("patientId", "PAT-001", 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleTrackings_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleTrackings_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getOrderedPage("collDate", true, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleTrackings_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTrackings_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getMatchingOrderedPage("sosDesc", "Emergency", "collDate", true, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTrackings_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getMatchingOrderedPage("sosDesc", "Emergency", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTrackings_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getMatchingOrderedPage(propertyValues, "patientId", false, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTrackings_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTrackingList = sampleTrackingService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= sampleTrackingList.size());
    }
}
