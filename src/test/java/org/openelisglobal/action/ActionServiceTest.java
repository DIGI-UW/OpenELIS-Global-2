
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
    public void testGetAll_ReturnsAllActions() {
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
    public void testGetAllMatching_UsingPropertyNameAndValue() {
        actionList = actionService.getAllMatching(PROPERTY_NAME, PROPERTY_VALUE);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("REG", actionList.get(0).getType());
    }

    @Test
    public void testGetAllMatching_UsingMap() {
        actionList = actionService.getAllMatching(propertyValues);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("REG", actionList.get(0).getType());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void testGetAllOrdered_UsingStringAndBoolean() {
        actionList = actionService.getAllOrdered(ORDER_PROPERTY, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
        assertEquals("ACT002", actionList.get(1).getCode());
        assertEquals("ACT003", actionList.get(2).getCode());
        assertEquals("ACT004", actionList.get(3).getCode());
    }

    @Test
    public void testGetAllOrdered_UsingListAndBoolean() {
        actionList = actionService.getAllOrdered(orderProperties, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
        assertEquals("ACT002", actionList.get(1).getCode());
        assertEquals("ACT003", actionList.get(2).getCode());
        assertEquals("ACT004", actionList.get(3).getCode());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingOrderPropertyString() {
        actionList = actionService.getAllMatchingOrdered(PROPERTY_NAME, PROPERTY_VALUE, ORDER_PROPERTY, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingOrderPropertiesList() {
        actionList = actionService.getAllMatchingOrdered(PROPERTY_NAME, PROPERTY_VALUE, orderProperties, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingPropertyValuesMap() {
        actionList = actionService.getAllMatchingOrdered(propertyValues, PROPERTY_NAME, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingBothMapAndList() {
        actionList = actionService.getAllMatchingOrdered(propertyValues, orderProperties, IS_DESCENDING);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
    }

    @Test
    public void testGetPage() {
        actionList = actionService.getPage(STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetMatchingPage_UsingPROPERTY_NAMEAndValue() {
        actionList = actionService.getMatchingPage(PROPERTY_NAME, PROPERTY_VALUE, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetMatchingPage_UsingMap() {
        actionList = actionService.getMatchingPage(propertyValues, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetOrderedPage_UsingOrderPropertyString() {
        actionList = actionService.getOrderedPage(PROPERTY_NAME, IS_DESCENDING, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetOrderedPage_UsingOrderPropertiesList() {
        actionList = actionService.getOrderedPage(orderProperties, IS_DESCENDING, STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetMatchingOrderedPage_UsingPropertyNameAndValueAndOrderProperty() {
        actionList = actionService.getMatchingOrderedPage(PROPERTY_NAME, PROPERTY_VALUE, ORDER_PROPERTY, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetMatchingOrderedPage_UsingOrderPropertiesList() {
        actionList = actionService.getMatchingOrderedPage(PROPERTY_NAME, PROPERTY_VALUE, orderProperties, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetMatchingOrderedPage_UsingPropertiesValuesMap() {
        actionList = actionService.getMatchingOrderedPage(propertyValues, PROPERTY_NAME, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void testGetMatchingOrderedPage_UsingBothMapAndList() {
        actionList = actionService.getMatchingOrderedPage(propertyValues, orderProperties, IS_DESCENDING,
                STARTING_REC_NO);
        EXPECTED_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= EXPECTED_PAGES);
    }

    @Test
    public void updateAction() {
        Action action = actionService.getAll().get(0);
        action.setCode("ACT005");
        action.setType("UPDATEDREG");
        Action updatedAction = actionService.update(action);
        assertEquals("ACT005", updatedAction.getCode());
        assertEquals("UPDATEDREG", updatedAction.getType());
    }

    @Test
    public void deleteAction() {
        Action action = actionService.getAll().get(0);
        actionService.delete(action);
        List<Action> deletedAction = actionService.getAll();
        assertEquals(3, deletedAction.size());
    }

    @Test
    public void deleteAllActions() {
        actionService.deleteAll(actionService.getAll());
        List<Action> delectedActions = actionService.getAll();
        assertNotNull(delectedActions);
        assertEquals(0, delectedActions.size());
    }
}
