package org.openelisglobal.analysisqaeventaction;

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
import org.openelisglobal.analysisqaeventaction.service.AnalysisQaEventActionService;
import org.openelisglobal.analysisqaeventaction.valueholder.AnalysisQaEventAction;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalysisQaEventActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisQaEventActionService analysisQaEventActionService;

    private List<AnalysisQaEventAction> analysisQaEventActions;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 1;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis-qa-event-action.xml");
        propertyValues = new HashMap<>();
        orderProperties = new ArrayList<>();
    }

    @Test
    public void getAllEventActions_ShouldReturnAllEventActions() {
        analysisQaEventActions = analysisQaEventActionService.getAll();
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("3", analysisQaEventActions.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingEventActions_UsingPropertyName() {
        analysisQaEventActions = analysisQaEventActionService.getAllMatching("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"));
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("2", analysisQaEventActions.get(0).getId());
        assertEquals("3", analysisQaEventActions.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingEventActions_UsingMap() {
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getAllMatching(propertyValues);
        assertNotNull(analysisQaEventActions);
        assertEquals("2", analysisQaEventActions.get(0).getId());
        assertEquals("3", analysisQaEventActions.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedEventActions_UsingPropertyName() {
        analysisQaEventActions = analysisQaEventActionService.getAllOrdered("createdDate", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("3", analysisQaEventActions.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedEventActions_UsingList() {
        orderProperties.add("createdDate");
        analysisQaEventActions = analysisQaEventActionService.getAllOrdered(orderProperties, true);
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedEventActions_Using() {
        analysisQaEventActions = analysisQaEventActionService.getAllMatchingOrdered("createdDate",
                Timestamp.valueOf("2025-06-23 14:15:00"), "createdDate", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedEventActions_UsingList() {
        orderProperties.add("createdDate");
        analysisQaEventActions = analysisQaEventActionService.getAllMatchingOrdered("createdDate",
                Timestamp.valueOf("2025-06-23 14:15:00"), orderProperties, true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedEventActions_UsingMap() {
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getAllMatchingOrdered(propertyValues, "lastupdated",
                true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("2", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResults_UsingPageNumber() {
        analysisQaEventActions = analysisQaEventActionService.getPage(1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingPropertyNameAndValue() {
        analysisQaEventActions = analysisQaEventActionService.getMatchingPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingMap() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getMatchingPage(propertyValues, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingOrderProperty() {
        analysisQaEventActions = analysisQaEventActionService.getOrderedPage("lastupdated", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingList() {
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventActionService.getOrderedPage(orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingPropertyNameAndValue() {
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), "lastupdated", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingList() {
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingMap() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage(propertyValues, "createdDate",
                true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingMapAndList() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage(propertyValues, orderProperties,
                true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void deleteAnalysisQaEventActions_ShouldDeleteAnalysisQaEventActionPassedAsParameter() {
        AnalysisQaEventAction analysisQaEventAction = analysisQaEventActionService.getAll().get(0);
        analysisQaEventActionService.delete(analysisQaEventAction);
        List<AnalysisQaEventAction> deletedAnalysisQaEventAction = analysisQaEventActionService.getAll();
        assertEquals(2, deletedAnalysisQaEventAction.size());
    }

    @Test
    public void deleteAllAnalysisQaEvents_ShouldDeleteAllAnalysisQaEvents() {
        analysisQaEventActionService.deleteAll(analysisQaEventActionService.getAll());
        List<AnalysisQaEventAction> delectedAnalysisQaEventAction = analysisQaEventActionService.getAll();
        assertNotNull(delectedAnalysisQaEventAction);
        assertEquals(0, delectedAnalysisQaEventAction.size());
    }
}
