package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
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
                null, 1, null);
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

    @Test
    public void updateWithFormValues_projectMetadata_updatesFields() {
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Project Metadata Test");
        form.setTechnicianId(1);
        form.setType(101);
        form.setObjective("Test Objective");
        form.setPrincipalInvestigator("Dr. Jane Smith");
        form.setFundingSource("NIH Grant #12345");
        form.setBudget(new BigDecimal("50000.00"));
        form.setProjectTimeline("Jan 2025 - Dec 2025");

        noteBookService.updateWithFormValues(2, form);
        NoteBook updated = noteBookService.get(2);

        assertEquals("Dr. Jane Smith", updated.getPrincipalInvestigator());
        assertEquals("NIH Grant #12345", updated.getFundingSource());
        assertEquals(new BigDecimal("50000.00"), updated.getBudget());
        assertEquals("Jan 2025 - Dec 2025", updated.getProjectTimeline());
    }

    @Test
    public void createChildInstance_inheritsProjectMetadata() {
        // First, set project metadata on parent template (id=1)
        NoteBook parent = noteBookService.get(1);
        parent.setPrincipalInvestigator("Dr. John Doe");
        parent.setFundingSource("WHO Research Grant");
        parent.setBudget(new BigDecimal("100000.00"));
        parent.setProjectTimeline("Q1 2025 - Q4 2026");
        parent.setSysUserId("1");
        noteBookService.update(parent);

        // Create child instance
        NoteBook child = noteBookService.createChildInstance(1, "Test Child Instance", "1");

        assertNotNull(child);
        assertNotNull(child.getId());
        assertFalse(child.getIsTemplate());
        assertEquals(parent.getId(), child.getParentNotebook().getId());

        // Verify project metadata was inherited
        assertEquals("Dr. John Doe", child.getPrincipalInvestigator());
        assertEquals("WHO Research Grant", child.getFundingSource());
        assertEquals(new BigDecimal("100000.00"), child.getBudget());
        assertEquals("Q1 2025 - Q4 2026", child.getProjectTimeline());
    }

    @Test
    public void convertToDisplayBean_includesProjectMetadata() {
        // Set project metadata on notebook
        NoteBook notebook = noteBookService.get(1);
        notebook.setPrincipalInvestigator("Dr. Test PI");
        notebook.setFundingSource("Test Grant");
        notebook.setBudget(new BigDecimal("25000.50"));
        notebook.setProjectTimeline("2025");
        notebook.setSysUserId("1");
        noteBookService.update(notebook);

        // Convert to display bean
        NoteBookDisplayBean displayBean = noteBookService.convertToDisplayBean(1);

        assertNotNull(displayBean);
        assertEquals("Dr. Test PI", displayBean.getPrincipalInvestigator());
        assertEquals("Test Grant", displayBean.getFundingSource());
        assertEquals(new BigDecimal("25000.50"), displayBean.getBudget());
        assertEquals("2025", displayBean.getProjectTimeline());
    }

    @Test
    public void convertToFullDisplayBean_includesProjectMetadata() {
        // Set project metadata on notebook
        NoteBook notebook = noteBookService.get(2);
        notebook.setPrincipalInvestigator("Dr. Full Display PI");
        notebook.setFundingSource("Full Display Grant");
        notebook.setBudget(new BigDecimal("75000.00"));
        notebook.setProjectTimeline("Mar 2025 - Sep 2025");
        notebook.setSysUserId("1");
        noteBookService.update(notebook);

        // Convert to full display bean
        NoteBookFullDisplayBean fullDisplayBean = noteBookService.convertToFullDisplayBean(2);

        assertNotNull(fullDisplayBean);
        assertEquals("Dr. Full Display PI", fullDisplayBean.getPrincipalInvestigator());
        assertEquals("Full Display Grant", fullDisplayBean.getFundingSource());
        assertEquals(new BigDecimal("75000.00"), fullDisplayBean.getBudget());
        assertEquals("Mar 2025 - Sep 2025", fullDisplayBean.getProjectTimeline());
    }

    @Test
    public void projectMetadata_nullValues_handledCorrectly() {
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Null Metadata Test");
        form.setTechnicianId(1);
        form.setType(101);
        form.setObjective("Test Objective");
        // Don't set project metadata fields - they should remain null

        noteBookService.updateWithFormValues(2, form);
        NoteBook updated = noteBookService.get(2);

        // Fields should be null or empty (depending on previous state)
        // The important thing is no exception is thrown
        assertNotNull(updated);
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
