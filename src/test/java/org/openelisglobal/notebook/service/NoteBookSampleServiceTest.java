package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.springframework.beans.factory.annotation.Autowired;

public class NoteBookSampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NoteBookSampleService noteBookSampleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/dictionary.xml");
        executeDataSetWithStateManagement("testdata/notebook-sample-test-data.xml");
    }

    @Test
    public void getAll_validCall_returnsNonNullList() {
        List<NoteBookSample> samples = noteBookSampleService.getAll();
        assertNotNull(samples);
    }

    @Test
    public void getNotebookSamplesBySampleItemId_validId_returnsSamples() {
        List<NoteBookSample> samples = noteBookSampleService
                .getNotebookSamplesBySampleItemId(1);
        assertNotNull(samples);
        assertTrue(samples.size() >= 1);
        for (NoteBookSample s : samples) {
            assertNotNull(s.getSampleItem());
            assertNotNull(s.getSampleItem().getId());
        }
    }

    @Test
    public void getNotebookSamplesBySampleItemId_invalidId_returnsEmpty() {
        List<NoteBookSample> samples = noteBookSampleService
                .getNotebookSamplesBySampleItemId(99999);
        assertNotNull(samples);
        assertTrue(samples.isEmpty());
    }

    @Test
    public void getNotebookSamplesByNoteBookId_validId_returnsDisplayBeans() {
        List<SampleDisplayBean> beans = noteBookSampleService
                .getNotebookSamplesByNoteBookId(2);
        assertNotNull(beans);
        assertTrue(beans.size() >= 1);
    }

    @Test
    public void getNotebookSamplesByNoteBookId_invalidId_returnsEmpty() {
        List<SampleDisplayBean> beans = noteBookSampleService
                .getNotebookSamplesByNoteBookId(99999);
        assertNotNull(beans);
        assertTrue(beans.isEmpty());
    }

    @Test
    public void get_validId_returnsNoteBookSample() {
        NoteBookSample sample = noteBookSampleService.get(1);
        assertNotNull(sample);
        assertEquals(Integer.valueOf(1), sample.getId());
        assertNotNull(sample.getNotebook());
        assertNotNull(sample.getSampleItem());
    }
}
