package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.service.NonConformityHelper;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class NonConformityHelperIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleQaEventService sampleQaEventService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/non-conformity-helper.xml");
    }

    // -------------------------------------------------------------------------
    // getNoteForSample tests
    // -------------------------------------------------------------------------

    @Test
    public void getNoteForSample_shouldReturnNoteText_whenMatchingNoteExists() {
        Sample sample = sampleService.get("401");

        String result = NonConformityHelper.getNoteForSample(sample);

        assertEquals("QA note for sample 401", result);
    }

    @Test
    public void getNoteForSample_shouldReturnNull_whenNoMatchingNoteExists() {
        Sample sample = sampleService.get("402");

        String result = NonConformityHelper.getNoteForSample(sample);

        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // getNoteForSampleQaEvent tests
    // -------------------------------------------------------------------------

    @Test
    public void getNoteForSampleQaEvent_shouldReturnNoteText_whenMatchingNoteExists() {
        SampleQaEvent sampleQaEvent = sampleQaEventService.get("1");

        String result = NonConformityHelper.getNoteForSampleQaEvent(sampleQaEvent);

        assertEquals("QA note for sampleQaEvent 1", result);
    }

    @Test
    public void getNoteForSampleQaEvent_shouldReturnNull_whenSampleQaEventIsNull() {
        String result = NonConformityHelper.getNoteForSampleQaEvent(null);

        assertNull(result);
    }

    @Test
    public void getNoteForSampleQaEvent_shouldReturnNull_whenSampleQaEventIdIsBlank() {
        SampleQaEvent sampleQaEvent = new SampleQaEvent();

        String result = NonConformityHelper.getNoteForSampleQaEvent(sampleQaEvent);

        assertNull(result);
    }

    @Test
    public void getNoteForSampleQaEvent_shouldReturnNull_whenNoMatchingNoteExists() {
        SampleQaEvent sampleQaEvent = sampleQaEventService.get("2");

        String result = NonConformityHelper.getNoteForSampleQaEvent(sampleQaEvent);

        assertNull(result);
    }
}