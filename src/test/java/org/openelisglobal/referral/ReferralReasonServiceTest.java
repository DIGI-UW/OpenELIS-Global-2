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
import org.openelisglobal.referral.service.ReferralReasonService;
import org.openelisglobal.referral.valueholder.ReferralReason;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralReasonServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralReasonService referralReasonService;

    private List<ReferralReason> referralReasons;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-reason.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2024-07-01 09:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("description");
    }

    @Test
    public void getAllReferralReasons_ShouldReturnAllReferralReasonsInTheDB() {
        List<ReferralReason> referralReasons = referralReasonService.getAllReferralReasons();
        assertNotNull(referralReasons);
        assertEquals(5, referralReasons.size());
        assertEquals("3", referralReasons.get(2).getId());
        assertEquals("Specialist consultation", referralReasons.get(3).getDescription());
        assertEquals(Timestamp.valueOf("2024-07-01 09:00:00"), referralReasons.get(0).getLastupdated());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingReferralReasons_UsingPropertyNameAndValue() {
        referralReasons = referralReasonService.getAllMatching("nameKey", "referral.reason.consult");
        assertNotNull(referralReasons);
        assertEquals(2, referralReasons.size());
        assertEquals("5", referralReasons.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingReferralReasons_UsingAMap() {
        referralReasons = referralReasonService.getAllMatching(propertyValues);
        assertNotNull(referralReasons);
        assertEquals(2, referralReasons.size());
        assertEquals("5", referralReasons.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedReferralReasons_UsingAnOrderProperty() {
        referralReasons = referralReasonService.getAllOrdered("id", false);
        assertNotNull(referralReasons);
        assertEquals(5, referralReasons.size());
        assertEquals("3", referralReasons.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        referralReasons = referralReasonService.getAllOrdered(orderProperties, true);
        assertNotNull(referralReasons);
        assertEquals(5, referralReasons.size());
        assertEquals("2", referralReasons.get(3).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralReasons_UsingPropertyNameAndValueAndAnOrderProperty() {
        referralReasons = referralReasonService.getAllMatchingOrdered("nameKey", "referral.reason.consult",
                "lastupdated", true);
        assertNotNull(referralReasons);
        assertEquals(2, referralReasons.size());
        assertEquals("4", referralReasons.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralReasons_UsingPropertyNameAndValueAndAList() {
        referralReasons = referralReasonService.getAllMatchingOrdered("lastupdated",
                Timestamp.valueOf("2024-07-01 09:00:00"), orderProperties, true);
        assertNotNull(referralReasons);
        assertEquals(2, referralReasons.size());
        assertEquals("1", referralReasons.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralReasons_UsingAMapAndAnOrderProperty() {
        referralReasons = referralReasonService.getAllMatchingOrdered(propertyValues, "description", true);
        assertNotNull(referralReasons);
        assertEquals(2, referralReasons.size());
        assertEquals("5", referralReasons.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferralReasons_UsingAMapAndAList() {
        referralReasons = referralReasonService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(referralReasons);
        assertEquals(2, referralReasons.size());
        assertEquals("1", referralReasons.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfReferralReasons_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getPage(1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfReferralReasons_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getMatchingPage("name", "Lab Test", 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfReferralReasons_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfReferralReasons_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfReferralReasons_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralReasons_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getMatchingOrderedPage("name", "Consultation", "lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralReasons_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getMatchingOrderedPage("id", "4", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralReasons_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getMatchingOrderedPage(propertyValues, "nameKey", false, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferralReasons_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referralReasons = referralReasonService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= referralReasons.size());
    }

    @Test
    public void delete_ShouldDeleteAReferralReason() {
        referralReasons = referralReasonService.getAll();
        assertEquals(5, referralReasons.size());
        assertTrue(referralReasons.stream().anyMatch(rr -> "Lab Test".equals(rr.getName())));
        ReferralReason referralReason = referralReasonService.get("3");
        referralReasonService.delete(referralReason);
        List<ReferralReason> newReferralReasons = referralReasonService.getAll();
        assertFalse(newReferralReasons.stream().anyMatch(rr -> "Lab Test".equals(rr.getName())));
        assertEquals(4, newReferralReasons.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllReferralReasons() {
        referralReasons = referralReasonService.getAll();
        assertFalse(referralReasons.isEmpty());
        assertEquals(5, referralReasons.size());
        referralReasonService.deleteAll(referralReasons);
        List<ReferralReason> updatedReferralReasons = referralReasonService.getAll();
        assertTrue(updatedReferralReasons.isEmpty());
    }
}
