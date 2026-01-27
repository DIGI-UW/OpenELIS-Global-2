package org.openelisglobal.referral;

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
import org.openelisglobal.referral.service.ReferralTypeService;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralTypeService referralTypeService;

    private List<ReferralType> referralTypes;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-type.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2024-07-02 10:15:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("name");
    }

    @Test
    public void getReferralTypeByName_ShouldReturnAReferralTypeMatchingTheNamePassedAsParameter() {
        ReferralType referralType = referralTypeService.getReferralTypeByName("Specialist");
        assertNotNull(referralType);
        assertEquals("3", referralType.getId());
        assertEquals(Timestamp.valueOf("2024-07-03 11:30:00"), referralType.getLastupdated());
        assertEquals("referral.type.specialist", referralType.getDisplayKey());
    }

    @Test
    public void getAll_ShouldReturnAllReferralTypes() {
        referralTypes = referralTypeService.getAll();
        assertNotNull(referralTypes);
        assertEquals(6, referralTypes.size());
        assertEquals("5", referralTypes.get(4).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingReferralTypes_UsingPropertyNameAndValue() {
        referralTypes = referralTypeService.getAllMatching("name", "External");
        assertNotNull(referralTypes);
        assertEquals(3, referralTypes.size());
        assertEquals("5", referralTypes.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingReferralTypes_UsingAMap() {
        referralTypes = referralTypeService.getAllMatching(propertyValues);
        assertNotNull(referralTypes);
        assertEquals(3, referralTypes.size());
        assertEquals("5", referralTypes.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedReferralTypes_UsingAnOrderProperty() {
        referralTypes = referralTypeService.getAllOrdered("id", false);
        assertNotNull(referralTypes);
        assertEquals(6, referralTypes.size());
        assertEquals("6", referralTypes.get(5).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        referralTypes = referralTypeService.getAllOrdered(orderProperties, true);
        assertNotNull(referralTypes);
        assertEquals(6, referralTypes.size());
        assertEquals("6", referralTypes.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralTypes_UsingPropertyNameAndValueAndAnOrderProperty() {
        referralTypes = referralTypeService.getAllMatchingOrdered("description", "Referral to another facility",
                "lastupdated", true);
        assertNotNull(referralTypes);
        assertEquals(3, referralTypes.size());
        assertEquals("5", referralTypes.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralTypes_UsingPropertyNameAndValueAndAList() {
        referralTypes = referralTypeService.getAllMatchingOrdered("displayKey", "referral.type.external",
                orderProperties, true);
        assertNotNull(referralTypes);
        assertEquals(3, referralTypes.size());
        assertEquals("5", referralTypes.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralTypes_UsingAMapAndAnOrderProperty() {
        referralTypes = referralTypeService.getAllMatchingOrdered(propertyValues, "id", true);
        assertNotNull(referralTypes);
        assertEquals(3, referralTypes.size());
        assertEquals("2", referralTypes.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralTypes_UsingAMapAndAList() {
        referralTypes = referralTypeService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(referralTypes);
        assertEquals(3, referralTypes.size());
        assertEquals("2", referralTypes.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfReferralTypes_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getPage(1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfReferralTypes_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getMatchingPage("name", "External", 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfReferralTypes_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfReferralTypes_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfReferralTypes_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralTypes_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getMatchingOrderedPage("name", "Specialist", "lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralTypes_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getMatchingOrderedPage("id", "3", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralTypes_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getMatchingOrderedPage(propertyValues, "lastupdated", false, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralTypes_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralTypes = referralTypeService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= referralTypes.size());
    }

    @Test
    public void delete_ShouldDeleteAReferralType() {
        referralTypes = referralTypeService.getAll();
        assertEquals(6, referralTypes.size());
        assertTrue(referralTypes.stream().anyMatch(rt -> "Internal".equals(rt.getName())));
        ReferralType referralType = referralTypeService.get("1");
        referralTypeService.delete(referralType);
        List<ReferralType> newReferralTypes = referralTypeService.getAll();
        assertFalse(newReferralTypes.stream().anyMatch(rt -> "Internal".equals(rt.getName())));
        assertEquals(5, newReferralTypes.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllReferralTypes() {
        referralTypes = referralTypeService.getAll();
        assertFalse(referralTypes.isEmpty());
        assertEquals(6, referralTypes.size());
        referralTypeService.deleteAll(referralTypes);
        List<ReferralType> updatedReferralTypes = referralTypeService.getAll();
        assertTrue(updatedReferralTypes.isEmpty());
    }

}
