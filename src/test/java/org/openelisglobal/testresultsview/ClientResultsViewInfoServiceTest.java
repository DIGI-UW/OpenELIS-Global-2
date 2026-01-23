package org.openelisglobal.testresultsview;

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
import org.openelisglobal.testresultsview.service.ClientResultsViewInfoService;
import org.openelisglobal.testresultsview.valueholder.ClientResultsViewBean;
import org.springframework.beans.factory.annotation.Autowired;

public class ClientResultsViewInfoServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ClientResultsViewInfoService clientResultsViewInfoService;

    private List<ClientResultsViewBean> clientResultsViewInfoList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/client-results-view.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("result", 1001);
        orderProperties = new ArrayList<>();
        orderProperties.add("password");
    }

    @Test
    public void getAll_ShouldReturnAllClientResultsViewBeans() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAll();
        assertNotNull(clientResultsViewInfoList);
        assertEquals(8, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7005"), clientResultsViewInfoList.get(4).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingClientResultsViewBeans_UsingPropertyNameAndValue() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatching("password",
                "encrypted-password-string");
        assertNotNull(clientResultsViewInfoList);
        assertEquals(4, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7007"), clientResultsViewInfoList.get(3).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingClientResultsViewBeans_UsingAMap() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatching(propertyValues);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(3, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7004"), clientResultsViewInfoList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedClientResultsViewBeans_UsingAnOrderProperty() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllOrdered("id", false);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(8, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7006"), clientResultsViewInfoList.get(5).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllOrdered(orderProperties, true);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(8, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(6).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingPropertyNameAndValueAndAnOrderProperty() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered("password", "encryptedpassword",
                "result", false);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(1, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingPropertyNameAndValueAndAList() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered("password", "encryptedpassword",
                orderProperties, true);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(1, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingAMapAndAnOrderProperty() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered(propertyValues, "result", true);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(3, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7004"), clientResultsViewInfoList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingAMapAndAList() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered(propertyValues, orderProperties,
                false);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(3, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7007"), clientResultsViewInfoList.get(2).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResultsGivenPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getPage(3);
        assertEquals(Integer.valueOf("7005"), clientResultsViewInfoList.get(2).getId());
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
        assertEquals(6, clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResultsFilteredByAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingPage("password",
                "encrypted-password-string", 1);
        assertEquals(Integer.valueOf("7007"), clientResultsViewInfoList.get(3).getId());
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
        assertEquals(4, clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResultsFilteredByAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingPage(propertyValues, 1);
        assertEquals(Integer.valueOf("7004"), clientResultsViewInfoList.get(1).getId());
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
        assertEquals(3, clientResultsViewInfoList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResultsFilteredByAnOrderPropertyInDescendingOrder() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getOrderedPage("id", true, 1);
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResultsGivenAListAndOrderedInAscendingOrder() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage("password",
                "encrypted-password-string", "id", true, 1);
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage("id", "7002", orderProperties,
                true, 1);
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage(propertyValues, "password",
                false, 1);
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage(propertyValues, orderProperties,
                false, 1);
        assertTrue(PAGE_SIZE >= clientResultsViewInfoList.size());
    }
}