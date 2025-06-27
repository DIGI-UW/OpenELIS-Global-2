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
    private AnalysisQaEventActionService analysisQaEventService;

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
    public void testGetAllEventActions() {
        analysisQaEventActions = analysisQaEventService.getAll();
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("3", analysisQaEventActions.get(2).getId());
    }

    @Test
    public void testGetAllMatching() {
        analysisQaEventActions = analysisQaEventService.getAllMatching("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"));
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("2", analysisQaEventActions.get(0).getId());
        assertEquals("3", analysisQaEventActions.get(1).getId());
    }

    @Test
    public void testGetAllMatching_UsingMap() {
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventService.getAllMatching(propertyValues);
        assertNotNull(analysisQaEventActions);
        assertEquals("2", analysisQaEventActions.get(0).getId());
        assertEquals("3", analysisQaEventActions.get(1).getId());
    }

    @Test
    public void testGetAllOrdered() {
        analysisQaEventActions = analysisQaEventService.getAllOrdered("createdDate", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("3", analysisQaEventActions.get(2).getId());
    }

    @Test
    public void testGetAllOrdered_UsingList() {
        orderProperties.add("createdDate");
        analysisQaEventActions = analysisQaEventService.getAllOrdered(orderProperties, true);
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered() {
        analysisQaEventActions = analysisQaEventService.getAllMatchingOrdered("createdDate",
                Timestamp.valueOf("2025-06-23 14:15:00"), "createdDate", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingList() {
        orderProperties.add("createdDate");
        analysisQaEventActions = analysisQaEventService.getAllMatchingOrdered("createdDate",
                Timestamp.valueOf("2025-06-23 14:15:00"), orderProperties, true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingMap() {
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventService.getAllMatchingOrdered(propertyValues, "lastupdated", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("2", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetPage() {
        analysisQaEventActions = analysisQaEventService.getPage(1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingPage_UsingPropertyNameAndValue() {
        analysisQaEventActions = analysisQaEventService.getMatchingPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingPage_UsingMap() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventService.getMatchingPage(propertyValues, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetOrderedPage() {
        analysisQaEventActions = analysisQaEventService.getOrderedPage("lastupdated", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetOrderedPage_UsingList() {
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventService.getOrderedPage(orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage() {
        analysisQaEventActions = analysisQaEventService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), "lastupdated", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage_UsingList() {
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testgetMatchingOrderedPage_UsingMap() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventService.getMatchingOrderedPage(propertyValues, "createdDate", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage_UsingMapAndList() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventService.getMatchingOrderedPage(propertyValues, orderProperties, true,
                1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }
}
