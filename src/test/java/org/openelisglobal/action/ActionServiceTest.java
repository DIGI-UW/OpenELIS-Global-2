package org.openelisglobal.action;

import static org.junit.Assert.*;

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
    private String propertyName;
    private Object propertyValue;
    private String orderProperty;
    private int numberOfExpectedPages;
    private boolean isDescending;
    private int startingRecNo;
    private List<String> orderProperties;
    private Map<String, Object> propertyValues;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/action.xml");

        propertyName = "code";
        propertyValue = "ACT001";
        orderProperty = "code";
        isDescending = false;
        startingRecNo = 1;

        orderProperties = new ArrayList<>();
        orderProperties.add("code");

        propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

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

        actionList = actionService.getAllMatching(propertyName, propertyValue);

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

        actionList = actionService.getAllOrdered(orderProperty, isDescending);
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
        assertEquals("ACT002", actionList.get(1).getCode());
        assertEquals("ACT003", actionList.get(2).getCode());
        assertEquals("ACT004", actionList.get(3).getCode());

    }

    @Test
    public void testGetAllOrdered_UsingListAndBoolean() {

        actionList = actionService.getAllOrdered(orderProperties, isDescending);
        assertNotNull(actionList);
        assertEquals(4, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());
        assertEquals("ACT002", actionList.get(1).getCode());
        assertEquals("ACT003", actionList.get(2).getCode());
        assertEquals("ACT004", actionList.get(3).getCode());
    }

    @Test
    public void testGetAllMatchingOrdered_UsingOrderPropertyString() {

        actionList = actionService.getAllMatchingOrdered(propertyName, propertyValue, orderProperty, isDescending);

        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());

    }

    @Test
    public void testGetAllMatchingOrdered_UsingOrderPropertiesList() {

        actionList = actionService.getAllMatchingOrdered(propertyName, propertyValue, orderProperties, isDescending);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());

    }

    @Test
    public void testGetAllMatchingOrdered_UsingPropertyValuesMap() {

        actionList = actionService.getAllMatchingOrdered(propertyValues, propertyName, isDescending);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());

    }

    @Test
    public void testGetAllMatchingOrdered_UsingBothMapAndList() {

        actionList = actionService.getAllMatchingOrdered(propertyValues, orderProperties, isDescending);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        assertEquals("ACT001", actionList.get(0).getCode());

    }

    @Test
    public void testGetPage() {
        actionList = actionService.getPage(startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetMatchingPage_UsingPropertyNameAndValue() {
        actionList = actionService.getMatchingPage(propertyName, propertyValue, startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);
    }

    @Test
    public void testGetMatchingPage_UsingMap() {
        actionList = actionService.getMatchingPage(propertyValues, startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetOrderedPage_UsingOrderPropertyString() {
        actionList = actionService.getOrderedPage(propertyName, isDescending, startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetOrderedPage_UsingOrderPropertiesList() {

        actionList = actionService.getOrderedPage(orderProperties, isDescending, startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetMatchingOrderedPage_UsingPropertyNameAndValueAndOrderProperty() {
        actionList = actionService.getMatchingOrderedPage(propertyName, propertyValue, orderProperty, isDescending,
                startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetMatchingOrderedPage_UsingOrderPropertiesList() {

        actionList = actionService.getMatchingOrderedPage(propertyName, propertyValue, orderProperties, isDescending,
                startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetMatchingOrderedPage_UsingPropertiesValuesMap() {
        actionList = actionService.getMatchingOrderedPage(propertyValues, propertyName, isDescending, startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

    }

    @Test
    public void testGetMatchingOrderedPage_UsingBothMapAndList() {

        actionList = actionService.getMatchingOrderedPage(propertyValues, orderProperties, isDescending, startingRecNo);

        numberOfExpectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(actionList.size() <= numberOfExpectedPages);

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
        List<Action> actions = actionService.getAll();
        actionService.deleteAll(actions);

        List<Action> delectedActions = actionService.getAll();
        assertNotNull(delectedActions);
        assertEquals(0, delectedActions.size());
    }

}
