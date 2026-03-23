package org.openelisglobal.county;

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
import org.openelisglobal.county.service.CountyService;
import org.openelisglobal.county.valueholder.County;
import org.springframework.beans.factory.annotation.Autowired;

public class CountyServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CountyService countyService;

    private List<County> countyList;
    private static int NUMBER_OF_PAGES = 0;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/region.xml");
        executeDataSetWithStateManagement("testdata/county.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"));
        orderProperties = new ArrayList<>();
        orderProperties.add("county");
    }

    @Test
    public void getAll_ShouldReturnAllCounties() {
        countyList = countyService.getAll();
        assertNotNull(countyList);
        assertEquals(2, countyList.size());
        assertEquals("2002", countyList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCounties_UsingPropertyNameAndValue() {
        countyList = countyService.getAllMatching("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"));
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCounties_UsingAMap() {
        countyList = countyService.getAllMatching(propertyValues);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedCounties_UsingAnOrderProperty() {
        countyList = countyService.getAllOrdered("county", false);
        assertNotNull(countyList);
        assertEquals(2, countyList.size());
        assertEquals("2001", countyList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        countyList = countyService.getAllOrdered(orderProperties, true);
        assertNotNull(countyList);
        assertEquals(2, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingPropertyNameAndValueAndAnOrderProperty() {
        countyList = countyService.getAllMatchingOrdered("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"),
                "lastupdated", true);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingPropertyNameAndValueAndAList() {
        countyList = countyService.getAllMatchingOrdered("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"),
                orderProperties, true);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingAMapAndAnOrderProperty() {
        countyList = countyService.getAllMatchingOrdered(propertyValues, "county", true);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingAMapAndAList() {
        countyList = countyService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfCounties_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCounties_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingPage("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"), 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCounties_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCounties_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCounties_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"),
                "lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00.000"),
                orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage(propertyValues, "county", false, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void delete_ShouldDeleteACounty() {
        countyList = countyService.getAll();
        assertEquals(2, countyList.size());
        County county = countyService.get("2001");
        countyService.delete(county);
        List<County> newCountyList = countyService.getAll();
        assertEquals(1, newCountyList.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllCounties() {
        countyList = countyService.getAll();
        assertEquals(2, countyList.size());
        countyService.deleteAll(countyList);
        List<County> updateCountyList = countyService.getAll();
        assertTrue(updateCountyList.isEmpty());
    }
}