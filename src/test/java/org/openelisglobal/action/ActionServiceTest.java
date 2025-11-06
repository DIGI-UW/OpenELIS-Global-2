
package org.openelisglobal.action;

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
import org.openelisglobal.action.service.ActionService;
import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class ActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ActionService actionService;

    private List<Action> actionList;
    private static final String PROPERTY_NAME = "code";
    private static final Object PROPERTY_VALUE = "ACT001";
    private static final String ORDER_PROPERTY = "code";
    private int EXPECTED_PAGES = 0;
    private static final boolean IS_DESCENDING = false;
    private static final int STARTING_REC_NO = 1;
    private List<String> orderProperties;
    private Map<String, Object> propertyValues;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/action.xml");
        orderProperties = new ArrayList<>();
        orderProperties.add(PROPERTY_NAME);
        propertyValues = new HashMap<>();
        propertyValues.put(PROPERTY_NAME, PROPERTY_VALUE);
    }

    @Test
    public void getAll_ShouldReturnAllActions() {
        actionList = actionService.getAll();
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("1", actionList.get(0).getId());
        assertEquals("Initial patient registration", actionList.get(0).getDescription());
        assertEquals("REG", actionList.get(0).getType());
        assertEquals("2", actionList.get(1).getId());
        assertEquals("3", actionList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingActions_UsingPropertyNameAndValue() {
        actionList = actionService.getAllMatching(PROPERTY_NAME, PROPERTY_VALUE);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("REG", actionList.get(0).getType());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingActions_UsingMap() {
        actionList = actionService.getAllMatching(propertyValues);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("REG", actionList.get(0).getType());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedActions_UsingStringAndBoolean() {
        actionList = actionService.getAllOrdered(ORDER_PROPERTY, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
        assertEquals("ACT002", actionList.get(1).getCode());
        assertEquals("ACT003", actionList.get(2).getCode());
        assertEquals("ACT004", actionList.get(3).getCode());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedActions_UsingListAndBoolean() {
        actionList = actionService.getAllOrdered(orderProperties, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
        assertEquals("ACT002", actionList.get(1).getCode());
        assertEquals("ACT003", actionList.get(2).getCode());
        assertEquals("ACT004", actionList.get(3).getCode());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedActions_UsingOrderPropertyString() {
        actionList = actionService.getAllMatchingOrdered(PROPERTY_NAME, PROPERTY_VALUE, ORDER_PROPERTY, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedActions_UsingOrderPropertiesList() {
        actionList = actionService.getAllMatchingOrdered(PROPERTY_NAME, PROPERTY_VALUE, orderProperties, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedActions_UsingPropertyValuesMap() {
        actionList = actionService.getAllMatchingOrdered(propertyValues, PROPERTY_NAME, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedActions_UsingMapAndList() {
        actionList = actionService.getAllMatchingOrdered(propertyValues, orderProperties, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResults_UsingPageNumber() {
        actionList = actionService.getPage(STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingPropertyNameAndValue() {
        actionList = actionService.getMatchingPage(PROPERTY_NAME, PROPERTY_VALUE, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingMap() {
        actionList = actionService.getMatchingPage(propertyValues, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingOrderPropertyString() {
        actionList = actionService.getOrderedPage(PROPERTY_NAME, IS_DESCENDING, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingOrderPropertiesList() {
        actionList = actionService.getOrderedPage(orderProperties, IS_DESCENDING, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingPropertyNameAndValueAndOrderProperty() {
        actionList = actionService.getMatchingOrderedPage(PROPERTY_NAME, PROPERTY_VALUE, ORDER_PROPERTY, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingOrderPropertiesList() {
        actionList = actionService.getMatchingOrderedPage(PROPERTY_NAME, PROPERTY_VALUE, orderProperties, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingPropertiesValuesMap() {
        actionList = actionService.getMatchingOrderedPage(propertyValues, PROPERTY_NAME, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingBothMapAndList() {
        actionList = actionService.getMatchingOrderedPage(propertyValues, orderProperties, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void updateAction_ShouldReturnTheUpdatedAction() {
        Action action = actionService.getAll().get(0);
        action.setCode("ACT005");
        action.setType("UPDATEDREG");
        Action updatedAction = actionService.update(action);
        assertEquals("ACT005", updatedAction.getCode());
        assertEquals("UPDATEDREG", updatedAction.getType());
    }

    @Test
    public void deleteAction_ShouldDeleteActionPassedAsParameter() {
        Action action = actionService.getAll().get(0);
        actionService.delete(action);
        List<Action> deletedAction = actionService.getAll();
        assertEquals(3, deletedAction.size());
    }

    @Test
    public void deleteAllActions_ShouldDeleteAllActions() {
        actionService.deleteAll(actionService.getAll());
        List<Action> delectedActions = actionService.getAll();
        assertNotNull(delectedActions);
        assertEquals(0, delectedActions.size());
    }
}
