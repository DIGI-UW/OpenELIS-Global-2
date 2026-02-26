package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class QAServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleService sampleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qaservice-test.xml");
    }

    // -------------------------------------------------------------------------
    // isOrderNonConforming tests
    // -------------------------------------------------------------------------

    @Test
    public void isOrderNonConforming_shouldReturnTrue_whenSampleHasQaEvent() {
        Sample sample = sampleService.get("401");

        boolean result = QAService.isOrderNonConforming(sample);

        assertTrue("Sample with QA event should be non-conforming", result);
    }

    @Test
    public void isOrderNonConforming_shouldReturnFalse_whenSampleHasNoQaEvent() {
        Sample sample = sampleService.get("402");

        boolean result = QAService.isOrderNonConforming(sample);

        assertFalse("Sample without QA event should not be non-conforming", result);
    }

    @Test
    public void isOrderNonConforming_shouldReturnFalse_whenSampleIsNull() {
        boolean result = QAService.isOrderNonConforming(null);

        assertFalse("Null sample should not be non-conforming", result);
    }

    @Test
    public void isOrderNonConforming_shouldReturnTrue_whenSampleHasDeprecatedNonConformingStatus() {
        Sample sample = sampleService.get("403");

        boolean result = QAService.isOrderNonConforming(sample);

        assertTrue("Sample with NonConforming deprecated status should be non-conforming", result);
    }

    // -------------------------------------------------------------------------
    // getNonConformingSampleItems tests
    // -------------------------------------------------------------------------

    @Test
    public void getNonConformingSampleItems_shouldReturnSampleItems_whenSampleHasQaEvents() {
        Sample sample = sampleService.get("401");

        List<SampleItem> result = QAService.getNonConformingSampleItems(sample);

        assertNotNull("Result should not be null", result);
        assertFalse("Result should not be empty", result.isEmpty());
        assertEquals("601", result.get(0).getId());
    }

    @Test
    public void getNonConformingSampleItems_shouldReturnEmptyList_whenSampleHasNoQaEvents() {
        Sample sample = sampleService.get("402");

        List<SampleItem> result = QAService.getNonConformingSampleItems(sample);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty", result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // QAService instance tests
    // -------------------------------------------------------------------------

    @Test
    public void getEventId_shouldReturnSampleQaEventId() {
        SampleQaEvent sampleQaEvent = new SampleQaEvent();
        sampleQaEvent.setId("1");

        QAService qaService = new QAService(sampleQaEvent);

        assertEquals("1", qaService.getEventId());
    }

    @Test
    public void getUpdatedObservations_shouldReturnEmptyList_whenNoObservationsSet() {
        SampleQaEvent sampleQaEvent = new SampleQaEvent();
        QAService qaService = new QAService(sampleQaEvent);

        List<?> observations = qaService.getUpdatedObservations();

        assertNotNull("Observations list should not be null", observations);
        assertTrue("Observations list should be empty", observations.isEmpty());
    }

    @Test
    public void getSampleQaEvent_shouldReturnSetEvent() {
        SampleQaEvent sampleQaEvent = new SampleQaEvent();
        sampleQaEvent.setId("99");

        QAService qaService = new QAService(sampleQaEvent);

        assertNotNull("SampleQaEvent should not be null", qaService.getSampleQaEvent());
        assertEquals("99", qaService.getSampleQaEvent().getId());
    }
}
