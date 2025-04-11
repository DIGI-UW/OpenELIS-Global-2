package org.openelisglobal.cytology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.springframework.beans.factory.annotation.Autowired;

public class CytologySampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CytologySampleService cytologySampleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/cytology.xml");
    }

    @Test
    public void getAll_shouldReturnAllCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAll();
        assertNotNull(cytologySamples);
        assertEquals(3, cytologySamples.size());
        assertEquals(1, cytologySamples.get(0).getId().intValue());
        assertEquals(2, cytologySamples.get(1).getId().intValue());
        assertEquals(3, cytologySamples.get(2).getId().intValue());

    }

    @Test
    public void getAllMatching_shouldReturnMatchingCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatching("status", "COMPLETED");
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatching_givenMapping_shouldReturnMatchingCytologySamples() {
        Map<String, Object> mapping = Map.of("status", "COMPLETED");
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatching(mapping);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatchingPage_shouldReturnMatchingCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingPage("status", "COMPLETED", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());

    }

    @Test
    public void getAllMatchingPage_givenMapping_shouldReturnMatchingCytologySamples() {
        Map<String, Object> mapping = Map.of("status", "COMPLETED");
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingPage(mapping, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllOrdered_shouldReturnOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAllOrdered("id", false);
        assertNotNull(cytologySamples);
        assertEquals(3, cytologySamples.size());
        assertEquals(1, cytologySamples.get(0).getId().intValue());
        assertEquals(2, cytologySamples.get(1).getId().intValue());
        assertEquals(3, cytologySamples.get(2).getId().intValue());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getAllOrdered(orderBy, false);
        assertNotNull(cytologySamples);
        assertEquals(3, cytologySamples.size());
        assertEquals(1, cytologySamples.get(0).getId().intValue());
        assertEquals(2, cytologySamples.get(1).getId().intValue());
        assertEquals(3, cytologySamples.get(2).getId().intValue());
    }

    @Test
    public void getAllOrderedPage_shouldReturnOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllOrderedPageGivenList_shouldReturnOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatchingOrdered("status", "COMPLETED", "id",
                false);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnMatchingOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getAllMatchingOrdered("status", "COMPLETED",
                orderBy, false);
        assertNotNull(cytologySamples);
        assertEquals(1, cytologySamples.size());
        assertEquals(2, cytologySamples.get(0).getId().intValue());
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnMatchingOrderedCytologySamples() {
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingOrderedPage("status", "COMPLETED", "id",
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

    @Test
    public void getAllMatchingOrderedPageGivenList_shouldReturnMatchingOrderedCytologySamples() {
        List<String> orderBy = List.of("id");
        List<CytologySample> cytologySamples = cytologySampleService.getMatchingOrderedPage("status", "COMPLETED",
                orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= cytologySamples.size());
    }

}
