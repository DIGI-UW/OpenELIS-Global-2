package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.qaevent.service.NceActionLogService;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.springframework.beans.factory.annotation.Autowired;

public class NceActionLogServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NceActionLogService nceActionLogService;

    private List<NceActionLog> nceActionLogs;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nce-action-log.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("actionType", "corrective");
        orderProperties = new ArrayList<>();
        orderProperties.add("correctiveAction");
    }

    @Test
    public void getNceActionLogByNceId_ShouldReturnAllNceActionLogsWithAnNceIdMatchingTheParameterValue() {
        nceActionLogs = nceActionLogService.getNceActionLogByNceId("901");
        assertNotNull(nceActionLogs);
        assertEquals(6, nceActionLogs.size());
        assertEquals("8", nceActionLogs.get(3).getId());
        assertEquals("fix calibration issue", nceActionLogs.get(4).getCorrectiveAction());
    }

    @Test
    public void getAll_ShouldReturnAllNceActionLogs() {
        nceActionLogs = nceActionLogService.getAll();
        assertNotNull(nceActionLogs);
        assertEquals(10, nceActionLogs.size());
        assertEquals("3", nceActionLogs.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceActionLogs_UsingPropertyNameAndValue() {
        nceActionLogs = nceActionLogService.getAllMatching("turnAroundTime", "40");
        assertNotNull(nceActionLogs);
        assertEquals(3, nceActionLogs.size());
        assertEquals("5", nceActionLogs.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingNceActionLogs_UsingAMap() {
        nceActionLogs = nceActionLogService.getAllMatching(propertyValues);
        assertNotNull(nceActionLogs);
        assertEquals(7, nceActionLogs.size());
        assertEquals("9", nceActionLogs.get(6).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedNceActionLogs_UsingAnOrderProperty() {
        nceActionLogs = nceActionLogService.getAllOrdered("personResponsible", false);
        assertNotNull(nceActionLogs);
        assertEquals(10, nceActionLogs.size());
        assertEquals("9", nceActionLogs.get(7).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        nceActionLogs = nceActionLogService.getAllOrdered(orderProperties, true);
        assertNotNull(nceActionLogs);
        assertEquals(10, nceActionLogs.size());
        assertEquals("8", nceActionLogs.get(8).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceActionLogs_UsingPropertyNameAndValueAndAnOrderProperty() {
        nceActionLogs = nceActionLogService.getAllMatchingOrdered("ncEventId", "903", "dateCompleted", true);
        assertNotNull(nceActionLogs);
        assertEquals(2, nceActionLogs.size());
        assertEquals("3", nceActionLogs.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceActionLogs_UsingPropertyNameAndValueAndAList() {
        nceActionLogs = nceActionLogService.getAllMatchingOrdered("ncEventId", "901", orderProperties, true);
        assertNotNull(nceActionLogs);
        assertEquals(6, nceActionLogs.size());
        assertEquals("8", nceActionLogs.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceActionLogs_UsingAMapAndAnOrderProperty() {
        nceActionLogs = nceActionLogService.getAllMatchingOrdered(propertyValues, "personResponsible", true);
        assertNotNull(nceActionLogs);
        assertEquals(7, nceActionLogs.size());
        assertEquals("1", nceActionLogs.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedNceActionLogs_UsingAMapAndAList() {
        nceActionLogs = nceActionLogService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(nceActionLogs);
        assertEquals(7, nceActionLogs.size());
        assertEquals("5", nceActionLogs.get(5).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfNceActionLogs_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getPage(1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceActionLogs_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getMatchingPage("personResponsible", "mark brown", 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfNceActionLogs_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceActionLogs_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getOrderedPage("dateCompleted", true, 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfNceActionLogs_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceActionLogs_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getMatchingOrderedPage("actionType", "preventive", "dateCompleted", true,
                1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceActionLogs_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getMatchingOrderedPage("correctiveAction", "replace reagent",
                orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceActionLogs_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getMatchingOrderedPage(propertyValues, "id", false, 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfNceActionLogs_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        nceActionLogs = nceActionLogService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= nceActionLogs.size());
    }

    @Test
    public void delete_ShouldDeleteANceActionLog() {
        nceActionLogs = nceActionLogService.getAll();
        assertEquals(10, nceActionLogs.size());
        assertTrue(nceActionLogs.stream().anyMatch(nal -> "update software".equals(nal.getCorrectiveAction())));
        NceActionLog nceActionLog = nceActionLogService.get("6");
        nceActionLogService.delete(nceActionLog);
        List<NceActionLog> newNceActionLogs = nceActionLogService.getAll();
        assertFalse(newNceActionLogs.stream().anyMatch(nal -> "update software".equals(nal.getCorrectiveAction())));
        assertEquals(9, newNceActionLogs.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllNceActionLogs() {
        nceActionLogs = nceActionLogService.getAll();
        assertFalse(nceActionLogs.isEmpty());
        assertEquals(10, nceActionLogs.size());
        nceActionLogService.deleteAll(nceActionLogs);
        List<NceActionLog> updatedNceActionLogs = nceActionLogService.getAll();
        assertTrue(updatedNceActionLogs.isEmpty());
    }
}
