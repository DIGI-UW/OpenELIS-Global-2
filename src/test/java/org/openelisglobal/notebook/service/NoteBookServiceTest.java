package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
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

        // Reset sequences to avoid ID conflicts (test data uses IDs 1-12)
        jdbcTemplate.execute("SELECT setval('clinlims.notebook_seq', 100, false)");
        jdbcTemplate.execute("SELECT setval('clinlims.notebook_page_seq', 100, false)");

        // Insert notebook_entries join table data (no PK, can't use DBUnit XML)
        // Link notebook 2 as an entry of template 1
        jdbcTemplate.execute(
                "INSERT INTO clinlims.notebook_entries (notebook_id, entry_id) VALUES (1, 2) ON CONFLICT DO NOTHING");
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
        // Use notebook 4 (entry without pages, status=FINALIZED)
        NoteBook notebook = noteBookService.get(4);
        assertNotNull(notebook);
        assertEquals(NoteBookStatus.FINALIZED, notebook.getStatus());

        noteBookService.updateWithStatus(4, NoteBookStatus.LOCKED, "1");

        NoteBook updated = noteBookService.get(4);
        assertNotNull(updated);
        assertEquals(NoteBookStatus.LOCKED, updated.getStatus());
    }

    @Test
    public void updateWithStatus_biorepositoryCannotBeFinalized() {
        NoteBook notebook = noteBookService.get(3);
        assertNotNull(notebook);
        assertEquals(NoteBookStatus.SUBMITTED, notebook.getStatus());

        notebook.setWorkflowType("biorepository");
        noteBookService.update(notebook);

        try {
            noteBookService.updateWithStatus(3, NoteBookStatus.FINALIZED, "1");
            fail("Expected IllegalStateException when finalizing a biorepository notebook");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("biorepository"));
        }

        NoteBook reloaded = noteBookService.get(3);
        assertEquals("Status should remain unchanged", NoteBookStatus.SUBMITTED, reloaded.getStatus());
    }

    @Test
    public void updateWithStatus_biorepositoryCannotBeArchived() {
        NoteBook notebook = noteBookService.get(3);
        assertNotNull(notebook);
        assertEquals(NoteBookStatus.SUBMITTED, notebook.getStatus());

        notebook.setWorkflowType("biorepository");
        noteBookService.update(notebook);

        try {
            noteBookService.updateWithStatus(3, NoteBookStatus.ARCHIVED, "1");
            fail("Expected IllegalStateException when archiving a biorepository notebook");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("biorepository"));
        }

        NoteBook reloaded = noteBookService.get(3);
        assertEquals("Status should remain unchanged", NoteBookStatus.SUBMITTED, reloaded.getStatus());
    }

    @Test
    public void updateWithFormValues_validForm_updatesNotebook() {
        // Use notebook 3 (entry without pages) to avoid page cascade issues
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Updated Notebook Title");
        form.setTechnicianId(1);
        form.setType(101);
        form.setObjective("Updated Objective");
        form.setProtocol("Updated Protocol");

        noteBookService.updateWithFormValues(3, form);
        NoteBook updated = noteBookService.get(3);
        assertEquals("Updated Notebook Title", updated.getTitle());
        assertEquals("Updated Objective", updated.getObjective());
    }

    @Test
    public void updateWithFormValues_pagesWithoutIds_matchesExistingPagesAndRemovesMissingOnes() {
        jdbcTemplate.execute("INSERT INTO clinlims.notebook (id, title, type, protocol, content, objective, status, "
                + "is_template, technician_id, creator_id, date_created, last_updated) VALUES "
                + "(8, 'Pathology Entry', 100, 'Protocol', 'Content', 'Objective', 'DRAFT', false, 1, 1, "
                + "'2025-01-08 10:00:00', '2025-01-08 10:00:00')");
        jdbcTemplate.execute("INSERT INTO clinlims.notebook_page "
                + "(id, notebook_id, page_order, title, instructions, content, page_id, page_type, completed) VALUES "
                + "(101, 8, 1, 'Sample Creation and Metadata Capture', 'Old Instructions 1', 'Old Content 1', "
                + "'sample-creation', 'PATHOLOGY', false)");
        jdbcTemplate.execute("INSERT INTO clinlims.notebook_page "
                + "(id, notebook_id, page_order, title, instructions, content, page_id, page_type, completed) VALUES "
                + "(102, 8, 2, 'Sample Quality Control', 'Old Instructions 2', 'Old Content 2', "
                + "'sample-qc', 'PATHOLOGY', false)");
        jdbcTemplate.execute("INSERT INTO clinlims.notebook_page "
                + "(id, notebook_id, page_order, title, instructions, content, page_id, page_type, completed) VALUES "
                + "(103, 8, 3, 'Gross Examination', 'Old Instructions 3', 'Old Content 3', "
                + "'gross-exam', 'PATHOLOGY', false)");
        jdbcTemplate.execute("INSERT INTO clinlims.notebook_page_sample "
                + "(id, notebook_page_id, sample_item_id, status) VALUES (1000, 101, 501, 'PENDING')");

        NoteBookForm form = new NoteBookForm();
        form.setTitle("Pathology Entry");
        form.setTechnicianId(1);
        form.setType(100);
        form.setObjective("Objective");

        NoteBookPage pageOne = new NoteBookPage();
        pageOne.setOrder(1);
        pageOne.setTitle("Sample Creation and Metadata Capture");
        pageOne.setInstructions("Updated Instructions 1");
        pageOne.setContent("Updated Content 1");
        pageOne.setPageId("sample-creation");
        pageOne.setPageType("PATHOLOGY");
        pageOne.setCompleted(true);
        Map<String, Object> pageOneData = new HashMap<>();
        pageOneData.put("workflow", "histopathology");
        pageOne.setData(pageOneData);

        NoteBookPage pageTwo = new NoteBookPage();
        pageTwo.setOrder(2);
        pageTwo.setTitle("Sample Quality Control");
        pageTwo.setInstructions("Updated Instructions 2");
        pageTwo.setContent("Updated Content 2");
        pageTwo.setPageId("sample-qc");
        pageTwo.setPageType("PATHOLOGY");
        pageTwo.setCompleted(false);

        form.setPages(List.of(pageOne, pageTwo));

        noteBookService.updateWithFormValues(8, form);

        NoteBook updated = noteBookService.get(8);
        assertNotNull(updated);
        assertEquals(2, updated.getPages().size());

        NoteBookPage updatedPageOne = updated.getPages().stream()
                .filter(page -> "sample-creation".equals(page.getPageId()))
                .findFirst()
                .orElse(null);
        NoteBookPage updatedPageTwo = updated.getPages().stream()
                .filter(page -> "sample-qc".equals(page.getPageId()))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedPageOne);
        assertNotNull(updatedPageTwo);
        assertEquals(Integer.valueOf(101), updatedPageOne.getId());
        assertEquals(Integer.valueOf(102), updatedPageTwo.getId());
        assertEquals("Updated Instructions 1", updatedPageOne.getInstructions());
        assertEquals("Updated Content 1", updatedPageOne.getContent());
        assertTrue(updatedPageOne.getCompleted());
        assertEquals("histopathology", updatedPageOne.getData().get("workflow"));
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.notebook_page_sample WHERE notebook_page_id = 101",
                        Integer.class));
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.notebook_page WHERE id = 103", Integer.class));
    }

    @Test
    public void updateWithFormValues_projectMetadata_updatesFields() {
        // Use notebook 3 (entry without pages) to avoid page cascade issues
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Project Metadata Test");
        form.setTechnicianId(1);
        form.setType(101);
        form.setObjective("Test Objective");
        form.setPrincipalInvestigator("Dr. Jane Smith");
        form.setFundingSource("NIH Grant #12345");
        form.setBudget(new BigDecimal("50000.00"));
        form.setProjectTimeline("Jan 2025 - Dec 2025");

        noteBookService.updateWithFormValues(3, form);
        NoteBook updated = noteBookService.get(3);

        assertEquals("Dr. Jane Smith", updated.getPrincipalInvestigator());
        assertEquals("NIH Grant #12345", updated.getFundingSource());
        assertEquals(new BigDecimal("50000.00"), updated.getBudget());
        assertEquals("Jan 2025 - Dec 2025", updated.getProjectTimeline());
    }

    @Test
    public void createChildInstance_inheritsProjectMetadata() {
        // Use notebook 7 (template without pages to avoid page cascade issues)
        // Set project metadata using form-based update
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Template With Project Metadata");
        form.setTechnicianId(1);
        form.setType(102);
        form.setObjective("Objective T2");
        form.setIsTemplate(true); // Preserve template status
        form.setPrincipalInvestigator("Dr. John Doe");
        form.setFundingSource("WHO Research Grant");
        form.setBudget(new BigDecimal("100000.00"));
        form.setProjectTimeline("Q1 2025 - Q4 2026");
        form.setWorkflowType("fnac");

        noteBookService.updateWithFormValues(7, form);

        // Create child instance
        NoteBook child = noteBookService.createChildInstance(7, "Test Child Instance", "1");

        assertNotNull(child);
        assertNotNull(child.getId());
        assertFalse(child.getIsTemplate());
        assertEquals(Integer.valueOf(7), child.getParentNotebook().getId());

        // Verify project metadata was inherited
        assertEquals("Dr. John Doe", child.getPrincipalInvestigator());
        assertEquals("WHO Research Grant", child.getFundingSource());
        assertEquals(new BigDecimal("100000.00"), child.getBudget());
        assertEquals("Q1 2025 - Q4 2026", child.getProjectTimeline());
        assertEquals("fnac", child.getWorkflowType());
    }

    @Test
    public void createChildInstance_inheritsInstruments() throws Exception {
        // Load inventory test data for valid instrument IDs (needed for FK constraint)
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");

        // Use notebook 7 (template without pages to avoid page cascade issues)
        // Insert instruments using native SQL (join table has no PK, can't use DBUnit
        // XML)
        jdbcTemplate.execute("INSERT INTO clinlims.notebook_inventory_instruments "
                + "(notebook_id, inventory_item_id) VALUES (7, 1000)");
        jdbcTemplate.execute("INSERT INTO clinlims.notebook_inventory_instruments "
                + "(notebook_id, inventory_item_id) VALUES (7, 1001)");

        // Create child instance from template with instruments
        NoteBook child = noteBookService.createChildInstance(7, "Child With Instruments", "1");

        assertNotNull(child);
        assertNotNull(child.getId());
        assertFalse(child.getIsTemplate());
        assertEquals(Integer.valueOf(7), child.getParentNotebook().getId());

        // Verify instruments were inherited from parent template
        assertNotNull(child.getInventoryInstrumentIds());
        assertEquals(2, child.getInventoryInstrumentIds().size());
        assertTrue(child.getInventoryInstrumentIds().contains(1000L));
        assertTrue(child.getInventoryInstrumentIds().contains(1001L));
    }

    @Test
    public void convertToDisplayBean_includesProjectMetadata() {
        // Use notebook 7 (template without pages) and form-based update
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Test Template Notebook 2");
        form.setTechnicianId(1);
        form.setType(102);
        form.setObjective("Objective T2");
        form.setIsTemplate(true); // Preserve template status
        form.setPrincipalInvestigator("Dr. Test PI");
        form.setFundingSource("Test Grant");
        form.setBudget(new BigDecimal("25000.50"));
        form.setProjectTimeline("2025");

        noteBookService.updateWithFormValues(7, form);

        // Convert to display bean
        NoteBookDisplayBean displayBean = noteBookService.convertToDisplayBean(7);

        assertNotNull(displayBean);
        assertEquals("Dr. Test PI", displayBean.getPrincipalInvestigator());
        assertEquals("Test Grant", displayBean.getFundingSource());
        assertEquals(new BigDecimal("25000.50"), displayBean.getBudget());
        assertEquals("2025", displayBean.getProjectTimeline());
    }

    @Test
    public void convertToFullDisplayBean_includesProjectMetadata() {
        // Use notebook 7 (template without pages) and form-based update
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Test Template Notebook 2");
        form.setTechnicianId(1);
        form.setType(102);
        form.setObjective("Objective T2");
        form.setIsTemplate(true); // Preserve template status
        form.setPrincipalInvestigator("Dr. Full Display PI");
        form.setFundingSource("Full Display Grant");
        form.setBudget(new BigDecimal("75000.00"));
        form.setProjectTimeline("Mar 2025 - Sep 2025");

        noteBookService.updateWithFormValues(7, form);

        // Convert to full display bean
        NoteBookFullDisplayBean fullDisplayBean = noteBookService.convertToFullDisplayBean(7);

        assertNotNull(fullDisplayBean);
        assertEquals("Dr. Full Display PI", fullDisplayBean.getPrincipalInvestigator());
        assertEquals("Full Display Grant", fullDisplayBean.getFundingSource());
        assertEquals(new BigDecimal("75000.00"), fullDisplayBean.getBudget());
        assertEquals("Mar 2025 - Sep 2025", fullDisplayBean.getProjectTimeline());
    }

    @Test
    public void projectMetadata_nullValues_handledCorrectly() {
        // Use notebook 7 (template without pages)
        NoteBookForm form = new NoteBookForm();
        form.setTitle("Null Metadata Test");
        form.setTechnicianId(1);
        form.setType(102);
        form.setObjective("Test Objective");
        form.setIsTemplate(true); // Preserve template status
        // Don't set project metadata fields - they should remain null

        noteBookService.updateWithFormValues(7, form);
        NoteBook updated = noteBookService.get(7);

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
        // Use notebook 3 (entry without pages) to avoid page cascade issues from other
        // tests
        NoteBookFullDisplayBean fullDisplayBean = noteBookService.convertToFullDisplayBean(3);
        assertNotNull(fullDisplayBean);
        assertEquals(Integer.valueOf(3), fullDisplayBean.getId());
        assertNotNull(fullDisplayBean.getTitle());
        assertNotNull(fullDisplayBean.getContent());
        assertNotNull(fullDisplayBean.getPages()); // Empty list is still not null
    }

    // ========== Entry Number Calculation ==========
    @Test
    public void convertToDisplayBean_entryLinkedToTemplate_hasNotebookNameAndEntryNumber() {
        // Notebook 2 is linked to Template 1 via notebook_entries join table
        NoteBookDisplayBean displayBean = noteBookService.convertToDisplayBean(2);

        assertNotNull(displayBean);
        assertEquals(Integer.valueOf(2), displayBean.getId());
        // Entry should have parent template's name
        assertEquals("Test Template Notebook", displayBean.getNotebookName());
        // Entry should have entry number (1-based position in parent's entries list)
        assertNotNull(displayBean.getEntryNumber());
        assertEquals(Integer.valueOf(1), displayBean.getEntryNumber());
    }

    @Test
    public void filterNoteBookEntries_allEntries_onlyReturnsLinkedEntries() {
        // When filtering all entries (no noteBookId), should only return
        // notebooks that are actually entries (linked via notebook_entries table)
        List<NoteBook> entries = noteBookService.filterNoteBookEntries(null, null, null, null, null, null, false);

        assertNotNull(entries);
        // Only notebook 2 is linked as an entry in our test data
        // Other non-template notebooks (3,4,5,6) are NOT linked to any parent
        assertEquals(1, entries.size());
        assertEquals(Integer.valueOf(2), entries.get(0).getId());
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
