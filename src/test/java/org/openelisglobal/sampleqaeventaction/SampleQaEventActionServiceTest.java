package org.openelisglobal.sampleqaeventaction;

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
import org.openelisglobal.sampleqaeventaction.service.SampleQaEventActionService;
import org.openelisglobal.sampleqaeventaction.valueholder.SampleQaEventAction;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleQaEventActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleQaEventActionService sampleQaEventActionService;

    private List<SampleQaEventAction> sampleQaEventActions;
    private List<String> orderProperties;
    private Map<String, Object> propertyValues;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-qa-event-action.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-25 11:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("createdDate");
    }

    @Test
    public void getAll_ShouldReturnAllSampleQaEventActions() {
        sampleQaEventActions = sampleQaEventActionService.getAll();
        assertNotNull(sampleQaEventActions);
        assertEquals(3, sampleQaEventActions.size());
        assertEquals("2", sampleQaEventActions.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleQaEventActions_UsingPropertyNameAndValue() {
        sampleQaEventActions = sampleQaEventActionService.getAllMatching("lastupdated",
                Timestamp.valueOf("2025-06-25 11:00:00"));
        assertNotNull(sampleQaEventActions);
        assertEquals(2, sampleQaEventActions.size());
        assertEquals("3", sampleQaEventActions.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleQaEventActions_UsingAMap() {

        sampleQaEventActions = sampleQaEventActionService.getAllMatching(propertyValues);
        assertNotNull(sampleQaEventActions);
        assertEquals(2, sampleQaEventActions.size());
        assertEquals("3", sampleQaEventActions.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleQaEventActions_UsingPropertyOrderProperty() {
        sampleQaEventActions = sampleQaEventActionService.getAllOrdered("lastupdated", true);
        assertNotNull(sampleQaEventActions);
        assertEquals(3, sampleQaEventActions.size());
        assertEquals("3", sampleQaEventActions.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleQaEventActions_UsingAnOrderProperty() {
        List<SampleQaEventAction> sampleQaEventActions = sampleQaEventActionService.getAllOrdered("lastupdated", false);
        assertNotNull(sampleQaEventActions);
        assertEquals(3, sampleQaEventActions.size());
        assertEquals("3", sampleQaEventActions.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleQaEventActions_UsingAList() {
        sampleQaEventActions = sampleQaEventActionService.getAllOrdered(orderProperties, true);
        assertNotNull(sampleQaEventActions);
        assertEquals(3, sampleQaEventActions.size());
        assertEquals("1", sampleQaEventActions.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEventActions_UsingAPropertyNameAndValueAndAnOrderProperty() {
        sampleQaEventActions = sampleQaEventActionService.getAllMatchingOrdered("lastupdated",
                Timestamp.valueOf("2025-06-25 10:15:00"), "createdDate", true);
        assertNotNull(sampleQaEventActions);
        assertEquals(1, sampleQaEventActions.size());
        assertEquals("2", sampleQaEventActions.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEventActions_UsingAPropertyNameAndValueAndList() {
        sampleQaEventActions = sampleQaEventActionService.getAllMatchingOrdered("lastupdated",
                Timestamp.valueOf("2025-06-25 10:15:00"), orderProperties, true);
        assertNotNull(sampleQaEventActions);
        assertEquals(1, sampleQaEventActions.size());
        assertEquals("2", sampleQaEventActions.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEventActions_UsingAMapAndAnOrderProperty() {

        sampleQaEventActions = sampleQaEventActionService.getAllMatchingOrdered(propertyValues, "lastupdated", true);
        assertNotNull(sampleQaEventActions);
        assertEquals(2, sampleQaEventActions.size());
        assertEquals("1", sampleQaEventActions.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleQaEventActions_UsingAMapAndList() {

        sampleQaEventActions = sampleQaEventActionService.getAllMatchingOrdered(propertyValues, orderProperties, true);
        assertNotNull(sampleQaEventActions);
        assertEquals(2, sampleQaEventActions.size());
        assertEquals("3", sampleQaEventActions.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResults_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAMatchingPageOfResults_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getMatchingPage("lastupdated",
                Timestamp.valueOf("2025-06-25 10:15:00"), 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAMatchingPageOfResults_UsingAMap() {

        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfResults_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getOrdered_ShouldReturnAnOrderedPageOfResults_UsingAList() {

        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getOrderedPage(orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfResults_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-25 10:15:00"), "lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfResults_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-25 10:15:00"), orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfResults_UsingAMapAndAnOrderProperty() {

        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getMatchingOrderedPage(propertyValues, "lastupdated", true,
                1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfResults_UsingAMapAndAList() {

        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleQaEventActions = sampleQaEventActionService.getMatchingOrderedPage(propertyValues, orderProperties, true,
                1);
        assertTrue(NUMBER_OF_PAGES >= sampleQaEventActions.size());
    }

    @Test
    public void deleteSampleQaEventAction_ShouldDeleteSampleQaEventActionPassedAsParameter() {
        SampleQaEventAction sampleQaEventAction = sampleQaEventActionService.getAll().get(0);
        sampleQaEventActionService.delete(sampleQaEventAction);
        List<SampleQaEventAction> deletedSampleQaEventAction = sampleQaEventActionService.getAll();
        assertEquals(2, deletedSampleQaEventAction.size());
    }

    @Test
    public void deleteAllSampleQaEventActions_ShouldDeleteAllSampleQaEventActions() {
        sampleQaEventActionService.deleteAll(sampleQaEventActionService.getAll());
        List<SampleQaEventAction> delectedSampleQaEventActions = sampleQaEventActionService.getAll();
        assertNotNull(delectedSampleQaEventActions);
        assertEquals(0, delectedSampleQaEventActions.size());
    }

}
