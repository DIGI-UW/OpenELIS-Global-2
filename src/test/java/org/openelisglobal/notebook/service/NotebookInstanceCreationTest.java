package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Unit tests for Notebook Instance Creation (User Story 1). Tests the creation
 * of notebook instances from templates and linking samples.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookInstanceCreationTest {

    @Mock
    private NoteBookDAO noteBookDAO;

    @Mock
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private NotebookPageSampleService notebookPageSampleService;

    @InjectMocks
    private NotebookSampleEntryServiceImpl sampleEntryService;

    private NoteBook templateNotebook;
    private NoteBook instanceNotebook;
    private List<NoteBookPage> templatePages;
    private SampleItem testSampleItem;

    @Before
    public void setUp() {
        // Create template notebook with 9 pages (immunology workflow)
        templateNotebook = new NoteBook();
        templateNotebook.setId(1);
        templateNotebook.setTitle("Immunology Workflow Template");
        templateNotebook.setIsTemplate(true);
        templateNotebook.setStatus(NoteBookStatus.FINALIZED);

        templatePages = new ArrayList<>();
        String[] pageNames = { "Sample Reception", "Initial Processing", "Aliquoting", "Child Sample Creation",
                "Plate Setup", "Analyzer Results", "Post-Analysis", "Compilation", "Archiving" };
        for (int i = 0; i < 9; i++) {
            NoteBookPage page = new NoteBookPage();
            page.setId(i + 1);
            page.setTitle(pageNames[i]);
            page.setOrder(i + 1);
            page.setNotebook(templateNotebook);
            templatePages.add(page);
        }
        templateNotebook.setPages(templatePages);

        // Create instance notebook
        instanceNotebook = new NoteBook();
        instanceNotebook.setId(100);
        instanceNotebook.setTitle("Immunology Workflow - Batch 2024-001");
        instanceNotebook.setIsTemplate(false);
        instanceNotebook.setStatus(NoteBookStatus.DRAFT);

        // Copy pages to instance
        List<NoteBookPage> instancePages = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            NoteBookPage page = new NoteBookPage();
            page.setId(100 + i + 1);
            page.setTitle(pageNames[i]);
            page.setOrder(i + 1);
            page.setNotebook(instanceNotebook);
            instancePages.add(page);
        }
        instanceNotebook.setPages(instancePages);

        // Create test sample
        testSampleItem = new SampleItem();
        testSampleItem.setId("1000");
    }

    /**
     * Test: Linking a sample to notebook creates NotebookPageSample records for all
     * pages
     */
    @Test
    public void testLinkSampleToNotebook_CreatesRecordsForAllPages() {
        // Setup
        Integer notebookId = 100;
        Integer sampleItemId = 1000;
        List<Integer> sampleIds = Arrays.asList(sampleItemId);

        when(noteBookService.get(notebookId)).thenReturn(instanceNotebook);
        when(sampleItemService.get(sampleItemId.toString())).thenReturn(testSampleItem);
        when(notebookPageSampleDAO.getByPageIdAndSampleItemId(anyInt(), eq(sampleItemId))).thenReturn(null);

        // Execute
        int linked = sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);

        // Verify - should create 9 NotebookPageSample records (one per page)
        assertEquals(1, linked);
        verify(notebookPageSampleService, times(9)).insert(any(NotebookPageSample.class));
    }

    /**
     * Test: Linking multiple samples creates records for all samples on all pages
     */
    @Test
    public void testLinkMultipleSamples_CreatesRecordsForAllSamplesAndPages() {
        // Setup
        Integer notebookId = 100;
        List<Integer> sampleIds = Arrays.asList(1000, 1001, 1002, 1003, 1004); // 5 samples

        when(noteBookService.get(notebookId)).thenReturn(instanceNotebook);
        for (Integer sampleId : sampleIds) {
            SampleItem sample = new SampleItem();
            sample.setId(sampleId.toString());
            when(sampleItemService.get(sampleId.toString())).thenReturn(sample);
        }
        when(notebookPageSampleDAO.getByPageIdAndSampleItemId(anyInt(), anyInt())).thenReturn(null);

        // Execute
        int linked = sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);

        // Verify - 5 samples * 9 pages = 45 NotebookPageSample records
        assertEquals(5, linked);
        verify(notebookPageSampleService, times(45)).insert(any(NotebookPageSample.class));
    }

    /** Test: Linking already-linked sample is skipped (no duplicates) */
    @Test
    public void testLinkAlreadyLinkedSample_SkipsExisting() {
        // Setup
        Integer notebookId = 100;
        Integer sampleItemId = 1000;
        List<Integer> sampleIds = Arrays.asList(sampleItemId);

        // Simulate sample already linked to first page
        NotebookPageSample existingRecord = new NotebookPageSample();
        existingRecord.setId(1);
        existingRecord.setStatus(Status.PENDING);

        when(noteBookService.get(notebookId)).thenReturn(instanceNotebook);
        when(sampleItemService.get(sampleItemId.toString())).thenReturn(testSampleItem);
        // First page already has the sample, others don't
        when(notebookPageSampleDAO.getByPageIdAndSampleItemId(eq(101), eq(sampleItemId))).thenReturn(existingRecord);
        when(notebookPageSampleDAO.getByPageIdAndSampleItemId(intThat(id -> id != 101), eq(sampleItemId)))
                .thenReturn(null);

        // Execute
        int linked = sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);

        // Verify - sample is linked but only creates records for pages where it
        // doesn't exist
        assertEquals(1, linked);
        verify(notebookPageSampleService, times(8)).insert(any(NotebookPageSample.class));
    }

    /** Test: New NotebookPageSample records have PENDING status */
    @Test
    public void testNewPageSampleRecords_HavePendingStatus() {
        // Setup
        Integer notebookId = 100;
        Integer sampleItemId = 1000;
        List<Integer> sampleIds = Arrays.asList(sampleItemId);

        when(noteBookService.get(notebookId)).thenReturn(instanceNotebook);
        when(sampleItemService.get(sampleItemId.toString())).thenReturn(testSampleItem);
        when(notebookPageSampleDAO.getByPageIdAndSampleItemId(anyInt(), eq(sampleItemId))).thenReturn(null);

        // Capture the inserted records
        List<NotebookPageSample> capturedRecords = new ArrayList<>();
        doAnswer(invocation -> {
            NotebookPageSample nps = invocation.getArgument(0);
            capturedRecords.add(nps);
            return 1;
        }).when(notebookPageSampleService).insert(any(NotebookPageSample.class));

        // Execute
        sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);

        // Verify all records have PENDING status
        assertEquals(9, capturedRecords.size());
        for (NotebookPageSample nps : capturedRecords) {
            assertEquals(Status.PENDING, nps.getStatus());
        }
    }

    /** Test: Linking to non-existent notebook throws exception */
    @Test(expected = IllegalArgumentException.class)
    public void testLinkToNonExistentNotebook_ThrowsException() {
        // Setup
        Integer notebookId = 999;
        List<Integer> sampleIds = Arrays.asList(1000);

        when(noteBookService.get(notebookId)).thenReturn(null);

        // Execute - should throw
        sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);
    }

    /** Test: Linking non-existent sample is skipped gracefully */
    @Test
    public void testLinkNonExistentSample_SkippedGracefully() {
        // Setup
        Integer notebookId = 100;
        List<Integer> sampleIds = Arrays.asList(1000, 9999); // 9999 doesn't exist

        when(noteBookService.get(notebookId)).thenReturn(instanceNotebook);
        when(sampleItemService.get("1000")).thenReturn(testSampleItem);
        when(sampleItemService.get("9999")).thenReturn(null);
        when(notebookPageSampleDAO.getByPageIdAndSampleItemId(anyInt(), eq(1000))).thenReturn(null);

        // Execute
        int linked = sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);

        // Verify - only 1 sample linked (the existing one)
        assertEquals(1, linked);
        verify(notebookPageSampleService, times(9)).insert(any(NotebookPageSample.class));
    }

    /** Test: Linking to notebook without pages handles gracefully */
    @Test
    public void testLinkToNotebookWithoutPages_NoRecordsCreated() {
        // Setup
        NoteBook emptyNotebook = new NoteBook();
        emptyNotebook.setId(200);
        emptyNotebook.setPages(new ArrayList<>());

        Integer notebookId = 200;
        List<Integer> sampleIds = Arrays.asList(1000);

        when(noteBookService.get(notebookId)).thenReturn(emptyNotebook);
        when(sampleItemService.get("1000")).thenReturn(testSampleItem);

        // Execute
        int linked = sampleEntryService.linkSamplesToNotebook(notebookId, sampleIds);

        // Verify - sample is linked but no page records created
        assertEquals(1, linked);
        verify(notebookPageSampleService, never()).insert(any(NotebookPageSample.class));
    }

    /** Test: Page progress shows 0/5 for newly linked samples */
    @Test
    public void testPageProgress_ShowsZeroCompletedForNewSamples() {
        // Setup - mock the progress calculation
        // PageProgress(total, pending, inProgress, completed, skipped, percentage)
        NotebookPageSampleService.PageProgress progress = new NotebookPageSampleService.PageProgress(5, 5, 0, 0, 0,
                0.0);

        when(notebookPageSampleService.getPageProgress(anyInt())).thenReturn(progress);

        // Execute
        NotebookPageSampleService.PageProgress result = notebookPageSampleService.getPageProgress(101);

        // Verify
        assertEquals(0, result.completed());
        assertEquals(5, result.pending());
        assertEquals(5, result.total());
        assertEquals(0.0, result.percentage(), 0.01);
    }
}
