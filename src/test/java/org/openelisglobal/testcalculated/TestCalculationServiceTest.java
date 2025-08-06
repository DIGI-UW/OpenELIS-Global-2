package org.openelisglobal.testcalculated;

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
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.beans.factory.annotation.Autowired;

public class TestCalculationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestCalculationService calculationService;

    private List<Calculation> testCalculations;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/test-calculation.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("testId", "4001");
        orderProperties = new ArrayList<>();
        orderProperties.add("name");
    }

    @Test
    public void getAll_ShouldReturnAllTestCalculations() {
        testCalculations = calculationService.getAll();
        assertNotNull(testCalculations);
        assertEquals(5, testCalculations.size());
        assertEquals(Integer.valueOf("103"), testCalculations.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingTestCalculations_UsingPropertyNameAndValue() {
        testCalculations = calculationService.getAllMatching("result", "4.8");
        assertNotNull(testCalculations);
        assertEquals(1, testCalculations.size());
        assertEquals(Integer.valueOf("104"), testCalculations.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingTestCalculations_UsingAMap() {
        testCalculations = calculationService.getAllMatching(propertyValues);
        assertNotNull(testCalculations);
        assertEquals(3, testCalculations.size());
        assertEquals(Integer.valueOf("102"), testCalculations.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedTestCalculations_UsingAnOrderProperty() {
        testCalculations = calculationService.getAllOrdered("result", false);
        assertNotNull(testCalculations);
        assertEquals(5, testCalculations.size());
        assertEquals(Integer.valueOf("104"), testCalculations.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        testCalculations = calculationService.getAllOrdered(orderProperties, true);
        assertNotNull(testCalculations);
        assertEquals(5, testCalculations.size());
        assertEquals(Integer.valueOf("104"), testCalculations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedTestCalculations_UsingPropertyNameAndValueAndAnOrderProperty() {
        testCalculations = calculationService.getAllMatchingOrdered("sampleId", "1002", "lastupdated", true);
        assertNotNull(testCalculations);
        assertEquals(1, testCalculations.size());
        assertEquals(Integer.valueOf("102"), testCalculations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedTestCalculations_UsingPropertyNameAndValueAndAList() {
        testCalculations = calculationService.getAllMatchingOrdered("sampleId", "1001", orderProperties, true);
        assertNotNull(testCalculations);
        assertEquals(3, testCalculations.size());
        assertEquals(Integer.valueOf("104"), testCalculations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedTestCalculations_UsingAMapAndAnOrderProperty() {
        testCalculations = calculationService.getAllMatchingOrdered(propertyValues, "result", true);
        assertNotNull(testCalculations);
        assertEquals(3, testCalculations.size());
        assertEquals(Integer.valueOf("105"), testCalculations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedTestCalculations_UsingAMapAndAList() {
        testCalculations = calculationService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(testCalculations);
        assertEquals(3, testCalculations.size());
        assertEquals(Integer.valueOf("105"), testCalculations.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfTestCalculations_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getPage(1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfTestCalculations_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getMatchingPage("result", "180", 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfTestCalculations_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfTestCalculations_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfTestCalculations_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfTestCalculations_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getMatchingOrderedPage("testId", "1002", "lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfTestCalculations_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getMatchingOrderedPage("sampleId", "1002", orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfTestCalculations_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getMatchingOrderedPage(propertyValues, "id", false, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfTestCalculations_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        testCalculations = calculationService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= testCalculations.size());
    }
}
