package org.openelisglobal.sampledomain;

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
import org.openelisglobal.sampledomain.service.SampleDomainService;
import org.openelisglobal.sampledomain.valueholder.SampleDomain;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleDomainServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleDomainService sampleDomainService;

    private List<SampleDomain> sampleDomains;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-domain.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-07-01 09:15:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("description");
    }

    @Test
    public void getAll_ShouldReturnAllSampleDomains() {
        sampleDomains = sampleDomainService.getAll();
        assertNotNull(sampleDomains);
        assertEquals(3, sampleDomains.size());
        assertEquals("3", sampleDomains.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleDomains_UsingPropertyNameAndValue() {
        sampleDomains = sampleDomainService.getAllMatching("description", "Veterinary");
        assertNotNull(sampleDomains);
        assertEquals(1, sampleDomains.size());
        assertEquals("3", sampleDomains.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleDomains_UsingAMap() {
        sampleDomains = sampleDomainService.getAllMatching(propertyValues);
        assertNotNull(sampleDomains);
        assertEquals(2, sampleDomains.size());
        assertEquals("3", sampleDomains.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleDomains_UsingAnOrderProperty() {
        sampleDomains = sampleDomainService.getAllOrdered("lastupdated", false);
        assertNotNull(sampleDomains);
        assertEquals(3, sampleDomains.size());
        assertEquals("3", sampleDomains.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        sampleDomains = sampleDomainService.getAllOrdered(orderProperties, true);
        assertNotNull(sampleDomains);
        assertEquals(3, sampleDomains.size());
        assertEquals("3", sampleDomains.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleDomains_UsingPropertyNameAndValueAndAnOrderProperty() {
        sampleDomains = sampleDomainService.getAllMatchingOrdered("description", "Pediatric", "lastupdated", true);
        assertNotNull(sampleDomains);
        assertEquals(1, sampleDomains.size());
        assertEquals("2", sampleDomains.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleDomains_UsingPropertyNameAndValueAndAList() {
        sampleDomains = sampleDomainService.getAllMatchingOrdered("description", "Adult Clinical", orderProperties,
                true);
        assertNotNull(sampleDomains);
        assertEquals(1, sampleDomains.size());
        assertEquals("1", sampleDomains.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleDomains_UsingAMapAndAnOrderProperty() {
        sampleDomains = sampleDomainService.getAllMatchingOrdered(propertyValues, "lastupdated", true);
        assertNotNull(sampleDomains);
        assertEquals(2, sampleDomains.size());
        assertEquals("2", sampleDomains.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleDomains_UsingAMapAndAList() {
        sampleDomains = sampleDomainService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(sampleDomains);
        assertEquals(2, sampleDomains.size());
        assertEquals("2", sampleDomains.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSampleDomains_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleDomains_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getMatchingPage("code", "P", 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleDomains_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleDomains_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleDomains_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleDomains_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getMatchingOrderedPage("description", "Pediatric", "lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleDomains_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getMatchingOrderedPage("description", "Pediatric", orderProperties, true,
                1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleDomains_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getMatchingOrderedPage(propertyValues, "code", false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleDomains_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleDomains = sampleDomainService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleDomains.size());
    }

    @Test
    public void delete_ShouldDeleteASampleDomain() {
        sampleDomains = sampleDomainService.getAll();
        assertEquals(3, sampleDomains.size());
        SampleDomain sampleDomain = sampleDomainService.get("2");
        sampleDomainService.delete(sampleDomain);
        List<SampleDomain> newSampleDomains = sampleDomainService.getAll();
        assertEquals(2, newSampleDomains.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSampleDomains() {
        sampleDomains = sampleDomainService.getAll();
        assertEquals(3, sampleDomains.size());
        sampleDomainService.deleteAll(sampleDomains);
        List<SampleDomain> updatedSampleDomains = sampleDomainService.getAll();
        assertTrue(updatedSampleDomains.isEmpty());
    }
}
