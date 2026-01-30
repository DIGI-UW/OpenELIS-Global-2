package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.springframework.beans.factory.annotation.Autowired;

public class NceSpecimenServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NceSpecimenService nceSpecimenService;

    private List<NceSpecimen> nceSpecimenList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nce-specimen.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("sampleItemId", "602");
        orderProperties = new ArrayList<>();
        orderProperties.add("nceId");
    }

    @Test
    public void getSpecimenByNceId_ShouldReturnAllNceSpecimensWithAnNceIdMatchingTheParameterValue() {
        nceSpecimenList = nceSpecimenService.getSpecimenByNceId("1002");
        assertNotNull(nceSpecimenList);
        assertEquals(3, nceSpecimenList.size());
        assertEquals("2", nceSpecimenList.get(0).getId());
    }

    @Test
    public void getSpecimenBySampleItemId_ShouldReturnAllNceSpecimensWithASampleIdMatchingTheParameterValue() {
        nceSpecimenList = nceSpecimenService.getSpecimenBySampleItemId("601");
        assertNotNull(nceSpecimenList);
        assertEquals(2, nceSpecimenList.size());
        assertEquals("5", nceSpecimenList.get(1).getId());
        assertEquals("4", nceSpecimenList.get(0).getId());
    }

    @Test
    public void getAll_ShouldReturnAllNceSpecimens() {
        nceSpecimenList = nceSpecimenService.getAll();
        assertNotNull(nceSpecimenList);
        assertEquals(5, nceSpecimenList.size());
        assertEquals("3", nceSpecimenList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceSpecimens_UsingPropertyNameAndValue() {
        nceSpecimenList = nceSpecimenService.getAllMatching("nceId", "1002");
        assertNotNull(nceSpecimenList);
        assertEquals(3, nceSpecimenList.size());
        assertEquals("4", nceSpecimenList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceSpecimens_UsingAMap() {
        nceSpecimenList = nceSpecimenService.getAllMatching(propertyValues);
        assertNotNull(nceSpecimenList);
        assertEquals(2, nceSpecimenList.size());
        assertEquals("2", nceSpecimenList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedNceSpecimens_UsingAnOrderProperty() {
        nceSpecimenList = nceSpecimenService.getAllOrdered("id", false);
        assertNotNull(nceSpecimenList);
        assertEquals(5, nceSpecimenList.size());
        assertEquals("3", nceSpecimenList.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        nceSpecimenList = nceSpecimenService.getAllOrdered(orderProperties, true);
        assertNotNull(nceSpecimenList);
        assertEquals(5, nceSpecimenList.size());
        assertEquals("1", nceSpecimenList.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceSpecimens_UsingPropertyNameAndValueAndAnOrderProperty() {
        nceSpecimenList = nceSpecimenService.getAllMatchingOrdered("nceId", "1002", "sampleItemId", true);
        assertNotNull(nceSpecimenList);
        assertEquals(3, nceSpecimenList.size());
        assertEquals("4", nceSpecimenList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceSpecimens_UsingPropertyNameAndValueAndAList() {
        nceSpecimenList = nceSpecimenService.getAllMatchingOrdered("nceId", "1002", orderProperties, true);
        assertNotNull(nceSpecimenList);
        assertEquals(3, nceSpecimenList.size());
        assertEquals("5", nceSpecimenList.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceSpecimens_UsingAMapAndAnOrderProperty() {
        nceSpecimenList = nceSpecimenService.getAllMatchingOrdered(propertyValues, "sampleItemId", true);
        assertNotNull(nceSpecimenList);
        assertEquals(2, nceSpecimenList.size());
        assertEquals("1", nceSpecimenList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceSpecimens_UsingAMapAndAList() {
        nceSpecimenList = nceSpecimenService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(nceSpecimenList);
        assertEquals(2, nceSpecimenList.size());
        assertEquals("1", nceSpecimenList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfNceSpecimens_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getPage(1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceSpecimens_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getMatchingPage("nceId", "1001", 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceSpecimens_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceSpecimens_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getOrderedPage("id", true, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceSpecimens_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceSpecimens_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getMatchingOrderedPage("sampleItemId", "601", "nceId", true, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceSpecimens_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getMatchingOrderedPage("sampleItemId", "603", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceSpecimens_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getMatchingOrderedPage(propertyValues, "id", false, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceSpecimens_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceSpecimenList = nceSpecimenService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceSpecimenList.size());
    }

    @Test
    public void delete_ShouldDeleteANceSpecimen() {
        nceSpecimenList = nceSpecimenService.getAll();
        assertEquals(5, nceSpecimenList.size());
        assertTrue(nceSpecimenList.stream().anyMatch(ncs -> "2".equals(ncs.getId())));
        NceSpecimen nceSpecimen = nceSpecimenService.get("2");
        nceSpecimenService.delete(nceSpecimen);
        List<NceSpecimen> newNceSpecimens = nceSpecimenService.getAll();
        assertFalse(newNceSpecimens.stream().anyMatch(ncs -> "2".equals(ncs.getId())));
        assertEquals(4, newNceSpecimens.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllNceSpecimens() {
        nceSpecimenList = nceSpecimenService.getAll();
        assertFalse(nceSpecimenList.isEmpty());
        assertEquals(5, nceSpecimenList.size());
        nceSpecimenService.deleteAll(nceSpecimenList);
        List<NceSpecimen> updatedNceSpecimens = nceSpecimenService.getAll();
        assertTrue(updatedNceSpecimens.isEmpty());
    }
}
