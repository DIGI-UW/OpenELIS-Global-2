package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

public class NotebookPageSampleServiceImplTest {

    @Test
    public void shouldTreatAsLinearChildCreation_returnsTrueForChildSampleCreationPage() {
        NoteBookPage page = new NoteBookPage();
        page.setPageType("CHILD_SAMPLE_CREATION");

        assertTrue(NotebookPageSampleServiceImpl.shouldTreatAsLinearChildCreation(page));
    }

    @Test
    public void shouldTreatAsLinearChildCreation_returnsFalseForOtherPageTypes() {
        NoteBookPage page = new NoteBookPage();
        page.setPageType("BRANCHING");

        assertFalse(NotebookPageSampleServiceImpl.shouldTreatAsLinearChildCreation(page));
        assertFalse(NotebookPageSampleServiceImpl.shouldTreatAsLinearChildCreation(null));
    }
}
