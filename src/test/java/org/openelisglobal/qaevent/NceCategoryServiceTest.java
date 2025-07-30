package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.beans.factory.annotation.Autowired;

public class NceCategoryServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private NceCategoryService nceCategoryService;

    private List<NceCategory> nceCategoryList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nce-category.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-07-25 10:15:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("name");
    }

    @Test
    public void getAllNceCategories_ShouldReturnAllNceCategoriesInTheDB() {
        nceCategoryList = nceCategoryService.getAllNceCategories();
        assertNotNull(nceCategoryList);
        assertEquals(5, nceCategoryList.size());
        assertEquals("1004", nceCategoryList.get(3).getId());
        assertEquals("nce.improper_storage_conditions", nceCategoryList.get(1).getDisplayKey());
        assertEquals("f", nceCategoryList.get(3).getActive());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceCategories_UsingPropertyNameAndValue() {
        nceCategoryList = nceCategoryService.getAllMatching("lastupdated", Timestamp.valueOf("2025-07-25 10:30:00"));
        assertNotNull(nceCategoryList);
        assertEquals(2, nceCategoryList.size());
        assertEquals("1003", nceCategoryList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceCategories_UsingAMap() {
        nceCategoryList = nceCategoryService.getAllMatching(propertyValues);
        assertNotNull(nceCategoryList);
        assertEquals(3, nceCategoryList.size());
        assertEquals("1004", nceCategoryList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedNceCategories_UsingAnOrderProperty() {
        nceCategoryList = nceCategoryService.getAllOrdered("id", false);
        assertNotNull(nceCategoryList);
        assertEquals(5, nceCategoryList.size());
        assertEquals("1005", nceCategoryList.get(4).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        nceCategoryList = nceCategoryService.getAllOrdered(orderProperties, true);
        assertNotNull(nceCategoryList);
        assertEquals(5, nceCategoryList.size());
        assertEquals("1002", nceCategoryList.get(3).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceCategories_UsingPropertyNameAndValueAndAnOrderProperty() {
        nceCategoryList = nceCategoryService.getAllMatchingOrdered("name", "Delayed Sample Transport", "lastupdated",
                true);
        assertNotNull(nceCategoryList);
        assertEquals(1, nceCategoryList.size());
        assertEquals("1003", nceCategoryList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceCategories_UsingPropertyNameAndValueAndAList() {
        nceCategoryList = nceCategoryService.getAllMatchingOrdered("displayKey", "nce.delayed_sample_transport",
                orderProperties, true);
        assertNotNull(nceCategoryList);
        assertEquals(1, nceCategoryList.size());
        assertEquals("1003", nceCategoryList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceCategories_UsingAMapAndAnOrderProperty() {
        nceCategoryList = nceCategoryService.getAllMatchingOrdered(propertyValues, "id", true);
        assertNotNull(nceCategoryList);
        assertEquals(3, nceCategoryList.size());
        assertEquals("1002", nceCategoryList.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceCategories_UsingAMapAndAList() {
        nceCategoryList = nceCategoryService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(nceCategoryList);
        assertEquals(3, nceCategoryList.size());
        assertEquals("1005", nceCategoryList.get(2).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfNceCategories_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getPage(1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceCategories_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getMatchingPage("displayKey", "nce.incomplete_patient_info", 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceCategories_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceCategories_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceCategories_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceCategories_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getMatchingOrderedPage("name", "Instrument Calibration Error",
                "lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceCategories_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-07-25 10:15:00"), orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceCategories_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getMatchingOrderedPage(propertyValues, "id", false, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceCategories_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceCategoryList = nceCategoryService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceCategoryList.size());
    }

    @Test
    public void delete_ShouldDeleteANceCategory() {
        nceCategoryList = nceCategoryService.getAll();
        assertEquals(5, nceCategoryList.size());
        assertTrue(nceCategoryList.stream().anyMatch(nca -> "Instrument Calibration Error".equals(nca.getName())));
        NceCategory nceCategory = nceCategoryService.get("1005");
        nceCategoryService.delete(nceCategory);
        List<NceCategory> newNceCategories = nceCategoryService.getAll();
        assertFalse(newNceCategories.stream().anyMatch(nca -> "Instrument Calibration Error".equals(nca.getName())));
        assertEquals(4, newNceCategories.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllNceCategories() {
        nceCategoryList = nceCategoryService.getAll();
        assertFalse(nceCategoryList.isEmpty());
        assertEquals(5, nceCategoryList.size());
        nceCategoryService.deleteAll(nceCategoryList);
        List<NceCategory> updatedNceCategories = nceCategoryService.getAll();
        assertTrue(updatedNceCategories.isEmpty());
    }
}
