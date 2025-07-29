package org.openelisglobal.reports.send.sample;

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
import org.openelisglobal.reports.send.sample.valueholder.SampleTransmissionSequence;
import org.openelisglobal.reports.service.send.sample.SampleTransmissionSequenceService;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleTransmissionSequenceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTransmissionSequenceService sampleTransmissionSequenceService;

    private List<SampleTransmissionSequence> sampleTransmissionSequenceList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/hl7-transmission-sequence.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-07-15 12:05:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("id");
    }

    @Test
    public void getAll_ShouldReturnAllSampleTransmissionSequences() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAll();
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(8, sampleTransmissionSequenceList.size());
        assertEquals("10007", sampleTransmissionSequenceList.get(6).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleTransmissionSequences_UsingPropertyNameAndValue() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllMatching("id", "10002");
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(1, sampleTransmissionSequenceList.size());
        assertEquals("10002", sampleTransmissionSequenceList.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSampleTransmissionSequences_UsingAMap() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllMatching(propertyValues);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(4, sampleTransmissionSequenceList.size());
        assertEquals("10008", sampleTransmissionSequenceList.get(3).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSampleTransmissionSequences_UsingAnOrderProperty() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllOrdered("id", false);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(8, sampleTransmissionSequenceList.size());
        assertEquals("10007", sampleTransmissionSequenceList.get(6).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllOrdered(orderProperties, true);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(8, sampleTransmissionSequenceList.size());
        assertEquals("10004", sampleTransmissionSequenceList.get(4).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTransmissionSequences_UsingPropertyNameAndValueAndAnOrderProperty() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllMatchingOrdered("lastupdated",
                Timestamp.valueOf("2025-07-15 12:05:00"), "id", true);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(4, sampleTransmissionSequenceList.size());
        assertEquals("10004", sampleTransmissionSequenceList.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTransmissionSequences_UsingPropertyNameAndValueAndAList() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllMatchingOrdered("lastupdated",
                Timestamp.valueOf("2025-07-15 12:05:00"), orderProperties, true);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(4, sampleTransmissionSequenceList.size());
        assertEquals("10008", sampleTransmissionSequenceList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTransmissionSequences_UsingAMapAndAnOrderProperty() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllMatchingOrdered(propertyValues, "id",
                true);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(4, sampleTransmissionSequenceList.size());
        assertEquals("10004", sampleTransmissionSequenceList.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSampleTransmissionSequences_UsingAMapAndAList() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAllMatchingOrdered(propertyValues,
                orderProperties, false);
        assertNotNull(sampleTransmissionSequenceList);
        assertEquals(4, sampleTransmissionSequenceList.size());
        assertEquals("10008", sampleTransmissionSequenceList.get(3).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSampleTransmissionSequences_UsingAPageNumber() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getPage(1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleTransmissionSequences_UsingAPropertyNameAndValue() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getMatchingPage("lastupdated",
                Timestamp.valueOf("2025-07-15 12:10:00"), 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSampleTransmissionSequences_UsingAMap() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getMatchingPage(propertyValues, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleTransmissionSequences_UsingAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getOrderedPage("lastupdated", true, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSampleTransmissionSequences_UsingAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getOrderedPage(orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTransmissionSequences_UsingAPropertyNameAndValueAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-07-15 12:00:00"), "id", true, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTransmissionSequences_UsingAPropertyNameAndValueAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getMatchingOrderedPage("id", "1002",
                orderProperties, true, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTransmissionSequences_UsingAMapAndAnOrderProperty() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getMatchingOrderedPage(propertyValues, "id",
                false, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSampleTransmissionSequences_UsingAMapAndAList() {
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getMatchingOrderedPage(propertyValues,
                orderProperties, false, 1);
        assertTrue(PAGE_SIZE >= sampleTransmissionSequenceList.size());
    }

    @Test
    public void delete_ShouldDeleteASampleTransmissionSequence() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAll();
        assertEquals(8, sampleTransmissionSequenceList.size());
        assertTrue(sampleTransmissionSequenceList.stream().anyMatch(samp_seq -> "10006".equals(samp_seq.getId())));
        SampleTransmissionSequence sampleTransmissionSequence = sampleTransmissionSequenceService.get("10006");
        sampleTransmissionSequenceService.delete(sampleTransmissionSequence);
        List<SampleTransmissionSequence> newSampleTransmissionSequences = sampleTransmissionSequenceService.getAll();
        assertFalse(newSampleTransmissionSequences.stream().anyMatch(samp_seq -> "10006".equals(samp_seq.getId())));
        assertEquals(7, newSampleTransmissionSequences.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSampleTransmissionSequences() {
        sampleTransmissionSequenceList = sampleTransmissionSequenceService.getAll();
        assertFalse(sampleTransmissionSequenceList.isEmpty());
        assertEquals(8, sampleTransmissionSequenceList.size());
        sampleTransmissionSequenceService.deleteAll(sampleTransmissionSequenceList);
        List<SampleTransmissionSequence> updatedSampleTransmissionSequences = sampleTransmissionSequenceService
                .getAll();
        assertTrue(updatedSampleTransmissionSequences.isEmpty());
    }
}
