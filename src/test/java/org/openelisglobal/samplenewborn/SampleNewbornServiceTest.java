package org.openelisglobal.samplenewborn;

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
import org.openelisglobal.samplenewborn.service.SampleNewbornService;
import org.openelisglobal.samplenewborn.valueholder.SampleNewborn;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleNewbornServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleNewbornService sampleNewbornService;

    private List<SampleNewborn> sampleNewbornList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-newborn.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-07-01 12:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("weight");
    }

    @Test
    public void getAll_ShouldReturnAllSampleNewborn() {
        sampleNewbornList = sampleNewbornService.getAll();
        assertNotNull(sampleNewbornList);
        assertEquals(3, sampleNewbornList.size());
        assertEquals("1002", sampleNewbornList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleNewborns_UsingPropertyNameAndValue() {
        sampleNewbornList = sampleNewbornService.getAllMatching("other", "Mother diabetic");
        assertNotNull(sampleNewbornList);
        assertEquals(2, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleNewborns_UsingAMap() {
        sampleNewbornList = sampleNewbornService.getAllMatching(propertyValues);
        assertNotNull(sampleNewbornList);
        assertEquals(1, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleNewborns_UsingAnOrderProperty() {
        sampleNewbornList = sampleNewbornService.getAllOrdered("weight", false);
        assertNotNull(sampleNewbornList);
        assertEquals(3, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        sampleNewbornList = sampleNewbornService.getAllOrdered(orderProperties, true);
        assertNotNull(sampleNewbornList);
        assertEquals(3, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleNewborns_UsingPropertyNameAndValueAndAnOrderProperty() {
        sampleNewbornList = sampleNewbornService.getAllMatchingOrdered("other", "Mother diabetic", "lastupdated", true);
        assertNotNull(sampleNewbornList);
        assertEquals(2, sampleNewbornList.size());
        assertEquals("1002", sampleNewbornList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleNewborns_UsingPropertyNameAndValueAndAList() {
        sampleNewbornList = sampleNewbornService.getAllMatchingOrdered("other", "Mother diabetic", orderProperties,
                true);
        assertNotNull(sampleNewbornList);
        assertEquals(2, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleNewborns_UsingAMapAndAnOrderProperty() {
        sampleNewbornList = sampleNewbornService.getAllMatchingOrdered(propertyValues, "weight", true);
        assertNotNull(sampleNewbornList);
        assertEquals(1, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleNewborns_UsingAMapAndAList() {
        sampleNewbornList = sampleNewbornService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(sampleNewbornList);
        assertEquals(1, sampleNewbornList.size());
        assertEquals("1003", sampleNewbornList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSampleNewborns_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleNewborns_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getMatchingPage("other", "1001", 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleNewborns_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleNewborns_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleNewborns_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleNewborns_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getMatchingOrderedPage("other", "Mother diabetic", "lastupdated", true,
                1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleNewborns_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-07-01 12:00:00"), orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleNewborns_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getMatchingOrderedPage(propertyValues, "lastupdated", false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleNewborns_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleNewbornList = sampleNewbornService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleNewbornList.size());
    }

    @Test
    public void delete_ShouldDeleteASampleNewborn() {
        sampleNewbornList = sampleNewbornService.getAll();
        assertEquals(3, sampleNewbornList.size());
        SampleNewborn sampleNewborn = sampleNewbornService.get("1002");
        sampleNewbornService.delete(sampleNewborn);
        List<SampleNewborn> newSampleNewborns = sampleNewbornService.getAll();
        assertEquals(2, newSampleNewborns.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSampleNewborns() {
        sampleNewbornList = sampleNewbornService.getAll();
        assertEquals(3, sampleNewbornList.size());
        sampleNewbornService.deleteAll(sampleNewbornList);
        List<SampleNewborn> updatedSampleNewborns = sampleNewbornService.getAll();
        assertTrue(updatedSampleNewborns.isEmpty());
    }

}
