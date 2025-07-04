package org.openelisglobal.analyzerimport;

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
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerTestMappingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    private List<AnalyzerTestMapping> analyzerTestMappings;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyzer-test-mapping.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2023-01-01 13:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("lastupdated");
    }

    @Test
    public void getAllForAnalyzer_ReturnsAllAnalyzerTestMappingsWithASpecificAnalyzerId() {
        analyzerTestMappings = analyzerTestMappingService.getAllForAnalyzer("2");
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("102", analyzerTestMappings.get(1).getTestId());
    }

    @Test
    public void getAll_ReturnsAllAnalyzerTestMappings() {
        analyzerTestMappings = analyzerTestMappingService.getAll();
        assertNotNull(analyzerTestMappings);
        assertEquals(3, analyzerTestMappings.size());
        assertEquals("Potassium Test", analyzerTestMappings.get(2).getAnalyzerTestName());
    }

    @Test
    public void getAllMatching_ReturnsAllMatchingAnalyzerTestMappings_UsingPropertyNameAndValue() {
        analyzerTestMappings = analyzerTestMappingService.getAllMatching("testId", "102");
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("1", analyzerTestMappings.get(0).getAnalyzerId());
    }

    @Test
    public void getAllMatching_ReturnsAllMatchingAnalyzerTestMappings_UsingAMap() {
        analyzerTestMappings = analyzerTestMappingService.getAllMatching(propertyValues);
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("Glucose Test", analyzerTestMappings.get(0).getAnalyzerTestName());
    }

    @Test
    public void getAllOrdered_ReturnsAllOrderedAnalyzerTestMappings_UsingPropertyName() {
        analyzerTestMappings = analyzerTestMappingService.getAllOrdered("lastupdated", true);
        assertNotNull(analyzerTestMappings);
        assertEquals(3, analyzerTestMappings.size());
        assertEquals("Glucose Test", analyzerTestMappings.get(0).getAnalyzerTestName());
    }

    @Test
    public void getAllOrdered_ReturnsAllOrderedAnalyzerTestMappings_UsingAList() {
        analyzerTestMappings = analyzerTestMappingService.getAllOrdered(orderProperties, false);
        assertNotNull(analyzerTestMappings);
        assertEquals(3, analyzerTestMappings.size());
        assertEquals("Glucose Test", analyzerTestMappings.get(1).getAnalyzerTestName());
    }

    @Test
    public void getAllMatchingOrdered_ReturnsAllMatchingOrderedAnalyzerTestMappings_UsingPropertyNameValueAndOrderProperty() {
        analyzerTestMappings = analyzerTestMappingService.getAllMatchingOrdered("testId", "102", "testId", true);
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("Hemoglobin Test", analyzerTestMappings.get(0).getAnalyzerTestName());
    }

    @Test
    public void getAllMatchingOrdered_ReturnsAllMatchingOrderedAnalyzerTestMappings_UsingPropertyNameValueAndAList() {
        analyzerTestMappings = analyzerTestMappingService.getAllMatchingOrdered("testId", "102", orderProperties, true);
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("Potassium Test", analyzerTestMappings.get(0).getAnalyzerTestName());
    }

    @Test
    public void getAllMatchingOrdered_ReturnsAllMatchingOrderedAnalyzerTestMappings_UsingAMapAndOrderProperty() {
        analyzerTestMappings = analyzerTestMappingService.getAllMatchingOrdered(propertyValues, "testId", true);
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("Potassium Test", analyzerTestMappings.get(0).getAnalyzerTestName());
    }

    @Test
    public void getAllMatchingOrdered_ReturnsAllMatchingOrderedAnalyzerTestMappings_UsingAMapAndList() {
        analyzerTestMappings = analyzerTestMappingService.getAllMatchingOrdered(propertyValues, orderProperties, true);
        assertNotNull(analyzerTestMappings);
        assertEquals(2, analyzerTestMappings.size());
        assertEquals("Glucose Test", analyzerTestMappings.get(0).getAnalyzerTestName());
    }

    @Test
    public void getPage_ReturnsAPageOfResults_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getMatchingPage_ReturnsAMatchingPageOfResults_UsingPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getMatchingPage("testId", "102", 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getMatchingPage_ReturnsAMatchingPageOfResults_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getOrderedPage_ReturnsAOrderedPageOfResults_UsingOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getOrderedPage("testId", true, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getOrderedPage_ReturnsAnOrderedPageOfResults_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getOrderedPage(orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getMatchingOrderedPage_ReturnsAMatchingOrderedPageOfResults_UsingPropertyNameAndValueAndOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getMatchingOrderedPage("testId", "102", "testId", true, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getMatchingOrderedPage_ReturnsAMatchingOrderedPageOfResults_UsingPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getMatchingOrderedPage("testId", "102", orderProperties, true,
                1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getMatchingOrderedPage_ReturnsAMatchingOrderedPageOfResults_UsingAMapAndOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getMatchingOrderedPage(propertyValues, "testId", true, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void getMatchingOrderedPage_ReturnsAMatchingOrderedPageOfResults_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerTestMappings = analyzerTestMappingService.getMatchingOrderedPage(propertyValues, orderProperties, true,
                1);
        assertTrue(NUMBER_OF_PAGES >= analyzerTestMappings.size());
    }

    @Test
    public void deleteAnalyzerTestMapping_DeletesAnalyzerTestMappingPassedAsParameter() {
        AnalyzerTestMapping analyzerTestMapping = analyzerTestMappingService.getAll().get(0);
        analyzerTestMappingService.delete(analyzerTestMapping);
        List<AnalyzerTestMapping> deletedAnalyzerTestMapping = analyzerTestMappingService.getAll();
        assertEquals(2, deletedAnalyzerTestMapping.size());
    }

    @Test
    public void deleteAllAnalyzerTestMapping_DeletesAllAnalyzerTestMappings() {
        analyzerTestMappingService.deleteAll(analyzerTestMappingService.getAll());
        List<AnalyzerTestMapping> delectedAnalyzerTestMapping = analyzerTestMappingService.getAll();
        assertNotNull(delectedAnalyzerTestMapping);
        assertEquals(0, delectedAnalyzerTestMapping.size());
    }

}
