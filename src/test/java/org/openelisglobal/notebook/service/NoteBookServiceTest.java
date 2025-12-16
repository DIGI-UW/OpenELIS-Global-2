package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class NoteBookServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NoteBookService noteBookService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Load fixture datasets
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/dictionary.xml");
        executeDataSetWithStateManagement("testdata/notebook-test-data.xml");
    }

    // ========== Template Entry Retrieval ==========
    @Test
    public void getNoteBookEntries_validTemplateId_returnsEntries() {
        Integer templateId = 1;
        List<NoteBook> entries = noteBookService.getNoteBookEntries(templateId);
        assertNotNull(entries);
        for (NoteBook entry : entries) {
            assertFalse(entry.getIsTemplate());
            assertNotNull(entry.getId());
        }
    }

    @Test
    public void getNoteBookEntries_nonTemplateId_returnsEmptyList() {
        List<NoteBook> entries = noteBookService.getNoteBookEntries(2);
        assertNotNull(entries);
        assertTrue(entries.isEmpty());
    }

    // ========== Active Notebooks ==========
    @Test
    public void getAllActiveNotebooks_validCall_returnsOnlyActive() {
        List<NoteBook> activeNotebooks = noteBookService.getAllActiveNotebooks();
        assertNotNull(activeNotebooks);
        for (NoteBook notebook : activeNotebooks) {
            assertNotEquals(NoteBookStatus.ARCHIVED, notebook.getStatus());
            assertNotNull(notebook.getId());
        }
    }

    // ========== Count Operations ==========
    @Test
    public void getTotalCount_validCall_returnsNonTemplateCount() {
        Long totalCount = noteBookService.getTotalCount();
        assertNotNull(totalCount);
        assertTrue(totalCount >= 5);
    }

    @Test
    public void getCountWithStatus_singleStatus_returnsCorrectCount() {
        Long count = noteBookService.getCountWithStatus(List.of(NoteBookStatus.DRAFT));
        assertNotNull(count);
        assertTrue(count >= 1);
    }

    @Test
    public void getCountWithStatus_multipleStatuses_returnsCorrectCount() {
        Long count = noteBookService.getCountWithStatus(List.of(NoteBookStatus.DRAFT, NoteBookStatus.SUBMITTED,
                NoteBookStatus.FINALIZED, NoteBookStatus.LOCKED));
        assertNotNull(count);
        assertTrue(count >= 4);
    }

    @Test
    public void getCountWithStatusBetweenDates_validRange_returnsCorrectCount() {
        Timestamp from = Timestamp.valueOf("2025-01-01 00:00:00");
        Timestamp to = Timestamp.valueOf("2025-01-10 23:59:59");
        Long count = noteBookService
                .getCountWithStatusBetweenDates(List.of(NoteBookStatus.DRAFT, NoteBookStatus.SUBMITTED), from, to);
        assertNotNull(count);
        assertTrue(count >= 2);
    }

    // ========== Filtering ==========
    @Test
    public void filterNoteBooks_singleStatus_returnsFiltered() {
        List<NoteBook> filtered = noteBookService.filterNoteBooks(List.of(NoteBookStatus.DRAFT), null, null, null,
                null);
        assertNotNull(filtered);
        assertTrue(filtered.size() >= 1);
        for (NoteBook notebook : filtered) {
            assertEquals(NoteBookStatus.DRAFT, notebook.getStatus());
        }
    }

    @Test
    public void filterNoteBooks_multipleStatuses_returnsFiltered() {
        List<NoteBook> filtered = noteBookService
                .filterNoteBooks(List.of(NoteBookStatus.DRAFT, NoteBookStatus.SUBMITTED), null, null, null, null);
        assertNotNull(filtered);
        assertTrue(filtered.size() >= 2);
        for (NoteBook notebook : filtered) {
            assertTrue(
                    notebook.getStatus() == NoteBookStatus.DRAFT || notebook.getStatus() == NoteBookStatus.SUBMITTED);
        }
    }

    @Test
    public void filterNoteBookEntries_validTemplateId_returnsFilteredEntries() {
        List<NoteBook> filtered = noteBookService.filterNoteBookEntries(List.of(NoteBookStatus.DRAFT), null, null, null,
                null, 1);
        assertNotNull(filtered);
        for (NoteBook entry : filtered) {
            assertEquals(NoteBookStatus.DRAFT, entry.getStatus());
        }
    }

    // ========== Status Updates ==========
    @Test
    public void updateWithStatus_validStatus_updatesNotebook() {
        NoteBook notebook = noteBookService.get(2);
        assertNotNull(notebook);
        assertEquals(NoteBookStatus.DRAFT, notebook.getStatus());

        noteBookService.updateWithStatus(2, NoteBookStatus.SUBMITTED, "1");

        NoteBook updated = noteBookService.get(2);
        assertNotNull(updated);
        assertEquals(NoteBookStatus.SUBMITTED, updated.getStatus());
    }

    @Test
    public void updateWithFormValues_validForm_updatesNotebook() {
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Updated Notebook Title");
        form.setTechnicianId(1);
        form.setType(101);
        form.setObjective("Updated Objective");
        form.setProtocol("Updated Protocol");

        noteBookService.updateWithFormValues(2, form);
        NoteBook updated = noteBookService.get(2);
        assertEquals("Updated Notebook Title", updated.getTitle());
        assertEquals("Updated Objective", updated.getObjective());
    }

    // ========== DisplayBean Conversion ==========
    @Test
    public void convertToDisplayBean_validId_returnsInitializedBean() {
        NoteBookDisplayBean displayBean = noteBookService.convertToDisplayBean(1);
        assertNotNull(displayBean);
        assertEquals(Integer.valueOf(1), displayBean.getId());
        assertNotNull(displayBean.getTitle());
        assertNotNull(displayBean.getStatus());
        assertTrue(displayBean.getIsTemplate());
    }

    @Test
    public void convertToFullDisplayBean_validId_returnsFullDisplay() {
        NoteBookFullDisplayBean fullDisplayBean = noteBookService.convertToFullDisplayBean(2);
        assertNotNull(fullDisplayBean);
        assertEquals(Integer.valueOf(2), fullDisplayBean.getId());
        assertNotNull(fullDisplayBean.getTitle());
        assertNotNull(fullDisplayBean.getContent());
        assertNotNull(fullDisplayBean.getPages());
        assertNotNull(fullDisplayBean.getComments());
    }

    // ========== Sample Search ==========
    @Test
    public void searchSampleItems_validAccession_returnsResults() {
        var results = noteBookService.searchSampleItems("TEST-001");
        assertNotNull(results);
    }

    @Test
    public void searchSampleItems_missingAccession_returnsEmptyList() {
        var results = noteBookService.searchSampleItems("NON-EXISTENT-999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
