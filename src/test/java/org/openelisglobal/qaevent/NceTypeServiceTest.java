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
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.beans.factory.annotation.Autowired;

public class NceTypeServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private NceTypeService nceTypeService;

    private List<NceType> nceTypes;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nce-type.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-07-25 10:05:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("name");
    }

    @Test
    public void getAllNceTypes_ShouldReturnAllNceTypesInTheDB() {
        List<NceType> nceTypes = nceTypeService.getAllNceTypes();
        assertNotNull(nceTypes);
        assertEquals(5, nceTypes.size());
        assertEquals("2", nceTypes.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceTypes_UsingPropertyNameAndValue() {
        nceTypes = nceTypeService.getAllMatching("categoryId", "1002");
        assertNotNull(nceTypes);
        assertEquals(2, nceTypes.size());
        assertEquals("5", nceTypes.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceTypes_UsingAMap() {
        nceTypes = nceTypeService.getAllMatching(propertyValues);
        assertNotNull(nceTypes);
        assertEquals(3, nceTypes.size());
        assertEquals("4", nceTypes.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedNceTypes_UsingAnOrderProperty() {
        nceTypes = nceTypeService.getAllOrdered("lastupdated", false);
        assertNotNull(nceTypes);
        assertEquals(5, nceTypes.size());
        assertEquals("4", nceTypes.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        nceTypes = nceTypeService.getAllOrdered(orderProperties, true);
        assertNotNull(nceTypes);
        assertEquals(5, nceTypes.size());
        assertEquals("3", nceTypes.get(3).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceTypes_UsingPropertyNameAndValueAndAnOrderProperty() {
        nceTypes = nceTypeService.getAllMatchingOrdered("categoryId", "1001", "lastupdated", true);
        assertNotNull(nceTypes);
        assertEquals(3, nceTypes.size());
        assertEquals("4", nceTypes.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceTypes_UsingPropertyNameAndValueAndAList() {
        nceTypes = nceTypeService.getAllMatchingOrdered("categoryId", "1001", orderProperties, true);
        assertNotNull(nceTypes);
        assertEquals(3, nceTypes.size());
        assertEquals("1", nceTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceTypes_UsingAMapAndAnOrderProperty() {
        nceTypes = nceTypeService.getAllMatchingOrdered(propertyValues, "id", true);
        assertNotNull(nceTypes);
        assertEquals(3, nceTypes.size());
        assertEquals("2", nceTypes.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceTypes_UsingAMapAndAList() {
        nceTypes = nceTypeService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(nceTypes);
        assertEquals(3, nceTypes.size());
        assertEquals("5", nceTypes.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfNceTypes_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getPage(1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceTypes_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getMatchingPage("categoryId", "1001", 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceTypes_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceTypes_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceTypes_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceTypes_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getMatchingOrderedPage("displayKey", "incorrect_result", "lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceTypes_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getMatchingOrderedPage("name", "broken container", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceTypes_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getMatchingOrderedPage(propertyValues, "id", false, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceTypes_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceTypes = nceTypeService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceTypes.size());
    }

    @Test
    public void delete_ShouldDeleteANceType() {
        nceTypes = nceTypeService.getAll();
        assertEquals(5, nceTypes.size());
        assertTrue(nceTypes.stream().anyMatch(nct -> "expired reagent".equals(nct.getName())));
        NceType nceType = nceTypeService.get("3");
        nceTypeService.delete(nceType);
        List<NceType> newNceTypes = nceTypeService.getAll();
        assertFalse(newNceTypes.stream().anyMatch(nct -> "expired reagent".equals(nct.getName())));
        assertEquals(4, newNceTypes.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllNceTypes() {
        nceTypes = nceTypeService.getAll();
        assertFalse(nceTypes.isEmpty());
        assertEquals(5, nceTypes.size());
        nceTypeService.deleteAll(nceTypes);
        List<NceType> updatedNceTypes = nceTypeService.getAll();
        assertTrue(updatedNceTypes.isEmpty());
    }

}
