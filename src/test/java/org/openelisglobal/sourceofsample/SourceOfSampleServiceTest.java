package org.openelisglobal.sourceofsample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.sourceofsample.service.SourceOfSampleService;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class SourceOfSampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SourceOfSampleService sourceOfSampleService;

    private List<SourceOfSample> SourceOfSampleList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/source-of-sample.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-07-15 10:31:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("description");
    }

    @Test
    public void getAll_ShouldReturnAllSourceOfSamples() {
        SourceOfSampleList = sourceOfSampleService.getAll();
        assertNotNull(SourceOfSampleList);
        assertEquals(5, SourceOfSampleList.size());
        assertEquals("3", SourceOfSampleList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSourceOfSamples_UsingPropertyNameAndValue() {
        SourceOfSampleList = sourceOfSampleService.getAllMatching("domain", "H");
        assertNotNull(SourceOfSampleList);
        assertEquals(2, SourceOfSampleList.size());
        assertEquals("4", SourceOfSampleList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSourceOfSamples_UsingAMap() {
        SourceOfSampleList = sourceOfSampleService.getAllMatching(propertyValues);
        assertNotNull(SourceOfSampleList);
        assertEquals(2, SourceOfSampleList.size());
        assertEquals("5", SourceOfSampleList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSourceOfSamples_UsingAnOrderProperty() {
        SourceOfSampleList = sourceOfSampleService.getAllOrdered("id", false);
        assertNotNull(SourceOfSampleList);
        assertEquals(5, SourceOfSampleList.size());
        assertEquals("5", SourceOfSampleList.get(4).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        SourceOfSampleList = sourceOfSampleService.getAllOrdered(orderProperties, true);
        assertNotNull(SourceOfSampleList);
        assertEquals(5, SourceOfSampleList.size());
        assertEquals("2", SourceOfSampleList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSourceOfSamples_UsingPropertyNameAndValueAndAnOrderProperty() {
        SourceOfSampleList = sourceOfSampleService.getAllMatchingOrdered("domain", "H", "lastupdated", true);
        assertNotNull(SourceOfSampleList);
        assertEquals(2, SourceOfSampleList.size());
        assertEquals("3", SourceOfSampleList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSourceOfSamples_UsingPropertyNameAndValueAndAList() {
        SourceOfSampleList = sourceOfSampleService.getAllMatchingOrdered("description", "Saliva", orderProperties,
                true);
        assertNotNull(SourceOfSampleList);
        assertEquals(1, SourceOfSampleList.size());
        assertEquals("3", SourceOfSampleList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSourceOfSamples_UsingAMapAndAnOrderProperty() {
        SourceOfSampleList = sourceOfSampleService.getAllMatchingOrdered(propertyValues, "id", true);
        assertNotNull(SourceOfSampleList);
        assertEquals(2, SourceOfSampleList.size());
        assertEquals("5", SourceOfSampleList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSourceOfSamples_UsingAMapAndAList() {
        SourceOfSampleList = sourceOfSampleService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(SourceOfSampleList);
        assertEquals(2, SourceOfSampleList.size());
        assertEquals("5", SourceOfSampleList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSourceOfSamples_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getPage(1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSourceOfSamples_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getMatchingPage("description", "Urine", 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSourceOfSamples_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSourceOfSamples_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSourceOfSamples_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSourceOfSamples_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getMatchingOrderedPage("description", "Stool", "lastupdated", true,
                1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSourceOfSamples_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getMatchingOrderedPage("description", "Saliva", orderProperties,
                true, 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSourceOfSamples_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getMatchingOrderedPage(propertyValues, "domain", false, 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSourceOfSamples_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        SourceOfSampleList = sourceOfSampleService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= SourceOfSampleList.size());
    }

    @Test
    public void insert_ShouldSaveAnewSourceOfSampleInTheDB() {
        SourceOfSampleList = sourceOfSampleService.getAll();
        sourceOfSampleService.deleteAll(SourceOfSampleList);
        List<SourceOfSample> updatedSourceOfSamples = sourceOfSampleService.getAll();
        assertTrue(updatedSourceOfSamples.isEmpty());
        SourceOfSample sourceOfSample = new SourceOfSample();
        sourceOfSample.setDomain("P");
        sourceOfSample.setDescription("Endoscopy test");
        sourceOfSample.setLastupdated(Timestamp.valueOf("2025-07-15 10:33:00"));
        sourceOfSampleService.insert(sourceOfSample);
        List<SourceOfSample> newSourceOfSamples = sourceOfSampleService.getAll();
        assertFalse(newSourceOfSamples.isEmpty());
        assertEquals(1, newSourceOfSamples.size());
        assertEquals("Endoscopy test", newSourceOfSamples.get(0).getDescription());
    }

    @Test
    public void delete_ShouldDeleteASourceOfSample() {
        SourceOfSampleList = sourceOfSampleService.getAll();
        assertEquals(5, SourceOfSampleList.size());
        SourceOfSample patientSourceOfSample = sourceOfSampleService.get("2");
        sourceOfSampleService.delete(patientSourceOfSample);
        List<SourceOfSample> newSourceOfSamples = sourceOfSampleService.getAll();
        assertEquals(4, newSourceOfSamples.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSourceOfSamples() {
        SourceOfSampleList = sourceOfSampleService.getAll();
        assertEquals(5, SourceOfSampleList.size());
        sourceOfSampleService.deleteAll(SourceOfSampleList);
        List<SourceOfSample> updatedSourceOfSamples = sourceOfSampleService.getAll();
        assertTrue(updatedSourceOfSamples.isEmpty());
    }
}
