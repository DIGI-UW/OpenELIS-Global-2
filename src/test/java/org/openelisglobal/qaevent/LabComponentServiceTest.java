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
import org.openelisglobal.qaevent.service.LabComponentService;
import org.openelisglobal.qaevent.valueholder.LabComponent;
import org.springframework.beans.factory.annotation.Autowired;

public class LabComponentServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private LabComponentService labComponentService;

    private List<LabComponent> labComponents;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/lap-component.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastmodified", Timestamp.valueOf("2024-01-13 13:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("name");
    }

    @Test
    public void getAll_ShouldReturnAllLabComponents() {
        labComponents = labComponentService.getAll();
        assertNotNull(labComponents);
        assertEquals(10, labComponents.size());
        assertEquals("3", labComponents.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingLabComponents_UsingPropertyNameAndValue() {
        labComponents = labComponentService.getAllMatching("name", "centrifuge");
        assertNotNull(labComponents);
        assertEquals(2, labComponents.size());
        assertEquals("6", labComponents.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingLabComponents_UsingAMap() {
        labComponents = labComponentService.getAllMatching(propertyValues);
        assertNotNull(labComponents);
        assertEquals(4, labComponents.size());
        assertEquals("9", labComponents.get(3).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedLabComponents_UsingAnOrderProperty() {
        labComponents = labComponentService.getAllOrdered("id", false);
        assertNotNull(labComponents);
        assertEquals(10, labComponents.size());
        assertEquals("10", labComponents.get(9).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        labComponents = labComponentService.getAllOrdered(orderProperties, true);
        assertNotNull(labComponents);
        assertEquals(10, labComponents.size());
        assertEquals("6", labComponents.get(6).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedLabComponents_UsingPropertyNameAndValueAndAnOrderProperty() {
        labComponents = labComponentService.getAllMatchingOrdered("name", "incubator", "lastmodified", true);
        assertNotNull(labComponents);
        assertEquals(2, labComponents.size());
        assertEquals("10", labComponents.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedLabComponents_UsingPropertyNameAndValueAndAList() {
        labComponents = labComponentService.getAllMatchingOrdered("lastmodified",
                Timestamp.valueOf("2024-01-17 17:35:00"), orderProperties, true);
        assertNotNull(labComponents);
        assertEquals(2, labComponents.size());
        assertEquals("1", labComponents.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedLabComponents_UsingAMapAndAnOrderProperty() {
        labComponents = labComponentService.getAllMatchingOrdered(propertyValues, "lastmodified", true);
        assertNotNull(labComponents);
        assertEquals(4, labComponents.size());
        assertEquals("4", labComponents.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedLabComponents_UsingAMapAndAList() {
        labComponents = labComponentService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(labComponents);
        assertEquals(4, labComponents.size());
        assertEquals("2", labComponents.get(2).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfLabComponents_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getPage(1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfLabComponents_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getMatchingPage("lastmodified", Timestamp.valueOf("2024-01-12 11:15:00"),
                1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfLabComponents_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfLabComponents_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getOrderedPage("lastmodified", true, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfLabComponents_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfLabComponents_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getMatchingOrderedPage("id", "10", "lastmodified", true, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfLabComponents_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getMatchingOrderedPage("name", "freezer", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfLabComponents_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getMatchingOrderedPage(propertyValues, "id", false, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfLabComponents_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        labComponents = labComponentService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= labComponents.size());
    }

    @Test
    public void delete_ShouldDeleteALabComponent() {
        labComponents = labComponentService.getAll();
        assertEquals(10, labComponents.size());
        assertTrue(labComponents.stream().anyMatch(lc -> "ph meter".equals(lc.getName())));
        LabComponent labComponent = labComponentService.get("2");
        labComponentService.delete(labComponent);
        List<LabComponent> newLabComponents = labComponentService.getAll();
        assertFalse(newLabComponents.stream().anyMatch(lc -> "ph meter".equals(lc.getNameKey())));
        assertEquals(9, newLabComponents.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllLabComponents() {
        labComponents = labComponentService.getAll();
        assertFalse(labComponents.isEmpty());
        assertEquals(10, labComponents.size());
        labComponentService.deleteAll(labComponents);
        List<LabComponent> updatedLabComponents = labComponentService.getAll();
        assertTrue(updatedLabComponents.isEmpty());
    }

}
