package org.openelisglobal.siteinformation;

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
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;

public class SiteInformationDomainServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    private List<SiteInformationDomain> siteInformationDomains;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/site-info-domain.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("description", "Clinical data domain");
        orderProperties = new ArrayList<>();
        orderProperties.add("name");
    }

    @Test
    public void getByName_ShouldReturnASiteInformationDomainsThatMatchesTheNameParameter() {
        SiteInformationDomain siteInformationDomain = siteInformationDomainService.getByName("Clinical");
        assertNotNull(siteInformationDomain);
        assertEquals("2", siteInformationDomain.getId());
        assertEquals("Clinical data domain", siteInformationDomain.getDescription());
    }

    @Test
    public void getAll_ShouldReturnAllSiteInformationDomains() {
        siteInformationDomains = siteInformationDomainService.getAll();
        assertNotNull(siteInformationDomains);
        assertEquals(5, siteInformationDomains.size());
        assertEquals("4", siteInformationDomains.get(3).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSiteInformationDomains_UsingPropertyNameAndValue() {
        siteInformationDomains = siteInformationDomainService.getAllMatching("name", "General");
        assertNotNull(siteInformationDomains);
        assertEquals(2, siteInformationDomains.size());
        assertEquals("5", siteInformationDomains.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSiteInformationDomains_UsingAMap() {
        siteInformationDomains = siteInformationDomainService.getAllMatching(propertyValues);
        assertNotNull(siteInformationDomains);
        assertEquals(2, siteInformationDomains.size());
        assertEquals("4", siteInformationDomains.get(1).getId());
        assertEquals("2", siteInformationDomains.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSiteInformationDomains_UsingAnOrderProperty() {
        siteInformationDomains = siteInformationDomainService.getAllOrdered("description", false);
        assertNotNull(siteInformationDomains);
        assertEquals(5, siteInformationDomains.size());
        assertEquals("3", siteInformationDomains.get(4).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        siteInformationDomains = siteInformationDomainService.getAllOrdered(orderProperties, true);
        assertNotNull(siteInformationDomains);
        assertEquals(5, siteInformationDomains.size());
        assertEquals("2", siteInformationDomains.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSiteInformationDomains_UsingPropertyNameAndValueAndAnOrderProperty() {
        siteInformationDomains = siteInformationDomainService.getAllMatchingOrdered("name", "General", "id", true);
        assertNotNull(siteInformationDomains);
        assertEquals(2, siteInformationDomains.size());
        assertEquals("1", siteInformationDomains.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSiteInformationDomains_UsingPropertyNameAndValueAndAList() {
        siteInformationDomains = siteInformationDomainService.getAllMatchingOrdered("name", "Lab", orderProperties,
                true);
        assertNotNull(siteInformationDomains);
        assertEquals(2, siteInformationDomains.size());
        assertEquals("3", siteInformationDomains.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSiteInformationDomains_UsingAMapAndAnOrderProperty() {
        siteInformationDomains = siteInformationDomainService.getAllMatchingOrdered(propertyValues, "description",
                true);
        assertNotNull(siteInformationDomains);
        assertEquals(2, siteInformationDomains.size());
        assertEquals("2", siteInformationDomains.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSiteInformationDomains_UsingAMapAndAList() {
        siteInformationDomains = siteInformationDomainService.getAllMatchingOrdered(propertyValues, orderProperties,
                false);
        assertNotNull(siteInformationDomains);
        assertEquals(2, siteInformationDomains.size());
        assertEquals("2", siteInformationDomains.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSiteInformationDomains_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSiteInformationDomains_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getMatchingPage("name", "Clinical", 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSiteInformationDomains_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSiteInformationDomains_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getOrderedPage("name", true, 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSiteInformationDomains_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSiteInformationDomains_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getMatchingOrderedPage("name", "Lab", "description", true,
                1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSiteInformationDomains_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getMatchingOrderedPage("description",
                "Laboratory-related domain", orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSiteInformationDomains_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getMatchingOrderedPage(propertyValues, "description",
                false, 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSiteInformationDomains_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        siteInformationDomains = siteInformationDomainService.getMatchingOrderedPage(propertyValues, orderProperties,
                false, 1);
        assertTrue(NUMBER_OF_PAGES >= siteInformationDomains.size());
    }

    @Test
    public void delete_ShouldDeleteASiteInformationDomain() {
        siteInformationDomains = siteInformationDomainService.getAll();
        assertEquals(5, siteInformationDomains.size());
        SiteInformationDomain siteInformationDomain = siteInformationDomainService.get("2");
        siteInformationDomainService.delete(siteInformationDomain);
        List<SiteInformationDomain> newSiteInformationDomains = siteInformationDomainService.getAll();
        assertEquals(4, newSiteInformationDomains.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSiteInformationDomains() {
        siteInformationDomains = siteInformationDomainService.getAll();
        assertEquals(5, siteInformationDomains.size());
        siteInformationDomainService.deleteAll(siteInformationDomains);
        List<SiteInformationDomain> updatedSiteInformationDomains = siteInformationDomainService.getAll();
        assertTrue(updatedSiteInformationDomains.isEmpty());
    }
}
