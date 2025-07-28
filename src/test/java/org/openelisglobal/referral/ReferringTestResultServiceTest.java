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
import org.openelisglobal.referral.service.ReferringTestResultService;
import org.openelisglobal.referral.valueholder.ReferringTestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferringTestResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferringTestResultService referringTestResultService;

    private List<ReferringTestResult> referringTestResultList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referring-test-result.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2024-07-03 10:30:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("testName");
    }

    @Test
    public void getReferringTestResultsForSampleItem_ShouldReturnAllReferralTestResultsWithASpecificSampleItem() {
        referringTestResultList = referringTestResultService.getReferringTestResultsForSampleItem("503");
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("3", referringTestResultList.get(0).getId());
        assertEquals("4", referringTestResultList.get(1).getId());
    }

    @Test
    public void getAll_ShouldReturnAllReferringTestResults() {
        referringTestResultList = referringTestResultService.getAll();
        assertNotNull(referringTestResultList);
        assertEquals(5, referringTestResultList.size());
        assertEquals("4", referringTestResultList.get(3).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingReferringTestResults_UsingPropertyNameAndValue() {
        referringTestResultList = referringTestResultService.getAllMatching("testName", "Malaria");
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("4", referringTestResultList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingReferringTestResults_UsingAMap() {
        referringTestResultList = referringTestResultService.getAllMatching(propertyValues);
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("4", referringTestResultList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedReferringTestResults_UsingAnOrderProperty() {
        referringTestResultList = referringTestResultService.getAllOrdered("id", false);
        assertNotNull(referringTestResultList);
        assertEquals(5, referringTestResultList.size());
        assertEquals("5", referringTestResultList.get(4).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        referringTestResultList = referringTestResultService.getAllOrdered(orderProperties, true);
        assertNotNull(referringTestResultList);
        assertEquals(5, referringTestResultList.size());
        assertEquals("2", referringTestResultList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferringTestResults_UsingPropertyNameAndValueAndAnOrderProperty() {
        referringTestResultList = referringTestResultService.getAllMatchingOrdered("resultValue", "Negative",
                "lastupdated", true);
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("4", referringTestResultList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferringTestResults_UsingPropertyNameAndValueAndAList() {
        referringTestResultList = referringTestResultService.getAllMatchingOrdered("sampleItemId", "503",
                orderProperties, true);
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("5", referringTestResultList.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferringTestResults_UsingAMapAndAnOrderProperty() {
        referringTestResultList = referringTestResultService.getAllMatchingOrdered(propertyValues, "id", true);
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("5", referringTestResultList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedReferringTestResults_UsingAMapAndAList() {
        referringTestResultList = referringTestResultService.getAllMatchingOrdered(propertyValues, orderProperties,
                false);
        assertNotNull(referringTestResultList);
        assertEquals(3, referringTestResultList.size());
        assertEquals("5", referringTestResultList.get(2).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfReferringTestResults_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getPage(1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfReferringTestResults_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getMatchingPage("resultValue", "Negative", 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfReferringTestResults_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfReferringTestResults_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfReferringTestResults_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferringTestResults_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getMatchingOrderedPage("testName", "Typhoid",
                "lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferringTestResults_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getMatchingOrderedPage("testName", "Malaria",
                orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferringTestResults_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getMatchingOrderedPage(propertyValues, "sampleItemId",
                false, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfReferringTestResults_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        referringTestResultList = referringTestResultService.getMatchingOrderedPage(propertyValues, orderProperties,
                false, 1);
        assertTrue(PAGE_SIZE >= referringTestResultList.size());
    }

    @Test
    public void delete_ShouldDeleteAReferringTestResult() {
        referringTestResultList = referringTestResultService.getAll();
        assertEquals(5, referringTestResultList.size());
        assertTrue(referringTestResultList.stream().anyMatch(rtr -> "WBC Count".equals(rtr.getTestName())));
        ReferringTestResult referringTestResult = referringTestResultService.get("2");
        referringTestResultService.delete(referringTestResult);
        List<ReferringTestResult> newReferringTestResults = referringTestResultService.getAll();
        assertFalse(newReferringTestResults.stream().anyMatch(rtr -> "WBC Count".equals(rtr.getTestName())));
        assertEquals(4, newReferringTestResults.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllReferringTestResults() {
        referringTestResultList = referringTestResultService.getAll();
        assertFalse(referringTestResultList.isEmpty());
        assertEquals(5, referringTestResultList.size());
        referringTestResultService.deleteAll(referringTestResultList);
        List<ReferringTestResult> updatedReferringTestResults = referringTestResultService.getAll();
        assertTrue(updatedReferringTestResults.isEmpty());
    }
}
