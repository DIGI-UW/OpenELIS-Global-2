package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.service.NotebookPageSampleService.PageProgress;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Unit tests for NotebookPageSampleService - Verifies workflow tracking logic
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookPageSampleServiceTest {

    @Mock
    private NotebookPageSampleDAO baseObjectDAO;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private NotebookPageSampleServiceImpl service;

    private NotebookPageSample testPageSample;
    private NoteBook testNotebook;
    private NoteBookPage testPage;
    private SampleItem testSampleItem;
    private SystemUser testUser;

    @Before
    public void setUp() {
        testNotebook = new NoteBook();
        testNotebook.setId(1);
        testNotebook.setTitle("Test Immunology Workflow");

        testPage = new NoteBookPage();
        testPage.setId(1);
        testPage.setTitle("Sample Reception");
        testPage.setNotebook(testNotebook);

        testNotebook.setPages(Arrays.asList(testPage));

        testSampleItem = new SampleItem();
        testSampleItem.setId("1000");

        testUser = new SystemUser();
        testUser.setId("1");
        testUser.setLoginName("testuser");

        testPageSample = new NotebookPageSample();
        testPageSample.setId(1);
        testPageSample.setNotebookPage(testPage);
        testPageSample.setSampleItemId(testSampleItem.getId());
        testPageSample.setStatus(Status.PENDING);
    }

    /** Test: getPageProgress calculates correct completion percentage */
    @Test
    public void testGetPageProgress_CalculatesCorrectly() {
        // Setup
        Integer pageId = 1;
        Map<Status, Long> statusCounts = new HashMap<>();
        statusCounts.put(Status.PENDING, 100L);
        statusCounts.put(Status.IN_PROGRESS, 50L);
        statusCounts.put(Status.COMPLETED, 50L);
        statusCounts.put(Status.SKIPPED, 0L);

        when(baseObjectDAO.getStatusCountsByPageId(pageId)).thenReturn(statusCounts);

        // Execute
        PageProgress progress = service.getPageProgress(pageId);

        // Verify
        assertNotNull(progress);
        assertEquals(200, progress.total());
        assertEquals(50, progress.completed());
        assertEquals(100, progress.pending());
        assertEquals(50, progress.inProgress());
        assertEquals(25.0, progress.percentage(), 0.01);
    }

    /** Test: getPageProgress handles empty page (no samples) */
    @Test
    public void testGetPageProgress_EmptyPage_ReturnsZeros() {
        // Setup
        Integer pageId = 1;
        Map<Status, Long> statusCounts = new HashMap<>();
        statusCounts.put(Status.PENDING, 0L);
        statusCounts.put(Status.IN_PROGRESS, 0L);
        statusCounts.put(Status.COMPLETED, 0L);
        statusCounts.put(Status.SKIPPED, 0L);

        when(baseObjectDAO.getStatusCountsByPageId(pageId)).thenReturn(statusCounts);

        // Execute
        PageProgress progress = service.getPageProgress(pageId);

        // Verify
        assertNotNull(progress);
        assertEquals(0, progress.total());
        assertEquals(0.0, progress.percentage(), 0.01);
    }

    /**
     * Test: bulkUpdateStatus processes samples in batches. When NotebookPageSample
     * records don't exist, the service creates them dynamically. Total updated =
     * DAO updates + newly inserted records.
     */
    @Test
    public void testBulkUpdateStatus_ProcessesInBatches() {
        // Setup
        Integer pageId = 1;
        List<Integer> sampleIds = new ArrayList<>();
        for (int i = 0; i < 75; i++) { // More than one batch (BATCH_SIZE = 50)
            sampleIds.add(i);
        }
        String userId = "1";

        when(systemUserService.get(userId)).thenReturn(testUser);
        // First batch updates 50 existing records, second batch updates 25 existing
        // records
        when(baseObjectDAO.bulkUpdateStatus(eq(pageId), anyList(), eq(Status.IN_PROGRESS))).thenReturn(50) // First
                // batch
                .thenReturn(25); // Second batch
        // Mock getPage to handle cases where records need to be created
        when(noteBookService.getPage(pageId)).thenReturn(testPage);
        // Mock that no existing records are found, so new ones are created
        // (returns null means record doesn't exist for that sample)
        when(baseObjectDAO.getByPageIdAndSampleItemId(eq(pageId), anyInt())).thenReturn(null);

        // Execute
        int updated = service.bulkUpdateStatus(pageId, sampleIds, Status.IN_PROGRESS, userId);

        // Verify:
        // - 75 from bulkUpdateStatus (50 + 25)
        // - 75 from insert (new records created for each sample since they don't
        // exist)
        assertEquals(150, updated);
        verify(baseObjectDAO, times(2)).bulkUpdateStatus(eq(pageId), anyList(), eq(Status.IN_PROGRESS));
    }

    /** Test: bulkApplyData merges data with existing */
    @Test
    public void testBulkApplyData_MergesWithExisting() {
        // Setup
        Integer pageId = 1;
        Integer sampleItemId = 1000;
        List<Integer> sampleIds = Arrays.asList(sampleItemId);
        String userId = "1";

        Map<String, Object> existingData = new HashMap<>();
        existingData.put("volume", "5.0");

        Map<String, Object> newData = new HashMap<>();
        newData.put("method", "Ficoll");

        testPageSample.setData(existingData);

        when(baseObjectDAO.getByPageIdAndSampleItemId(pageId, sampleItemId)).thenReturn(testPageSample);

        // Execute
        int updated = service.bulkApplyData(pageId, sampleIds, newData, userId);

        // Verify
        assertEquals(1, updated);
        Map<String, Object> resultData = testPageSample.getData();
        assertEquals("5.0", resultData.get("volume")); // Preserved
        assertEquals("Ficoll", resultData.get("method")); // Added
    }

    /** Test: bulkApplyData transitions PENDING to IN_PROGRESS */
    @Test
    public void testBulkApplyData_TransitionsPendingToInProgress() {
        // Setup
        Integer pageId = 1;
        Integer sampleItemId = 1000;
        List<Integer> sampleIds = Arrays.asList(sampleItemId);
        String userId = "1";

        Map<String, Object> newData = new HashMap<>();
        newData.put("method", "Ficoll");

        testPageSample.setStatus(Status.PENDING);
        testPageSample.setData(null);

        when(baseObjectDAO.getByPageIdAndSampleItemId(pageId, sampleItemId)).thenReturn(testPageSample);

        // Execute
        service.bulkApplyData(pageId, sampleIds, newData, userId);

        // Verify
        assertEquals(Status.IN_PROGRESS, testPageSample.getStatus());
    }

    /** Test: bulkApplyData does not change IN_PROGRESS or COMPLETED status */
    @Test
    public void testBulkApplyData_PreservesNonPendingStatus() {
        // Setup
        Integer pageId = 1;
        Integer sampleItemId = 1000;
        List<Integer> sampleIds = Arrays.asList(sampleItemId);
        String userId = "1";

        Map<String, Object> newData = new HashMap<>();
        newData.put("method", "Ficoll");

        testPageSample.setStatus(Status.COMPLETED);

        when(baseObjectDAO.getByPageIdAndSampleItemId(pageId, sampleItemId)).thenReturn(testPageSample);

        // Execute
        service.bulkApplyData(pageId, sampleIds, newData, userId);

        // Verify - status should remain COMPLETED
        assertEquals(Status.COMPLETED, testPageSample.getStatus());
    }

    /**
     * Test: createPageSamplesForNotebook creates sample ONLY on the first page
     * (T150 implementation - lazy creation, samples auto-created on next page when
     * completed)
     */
    @Test
    public void testCreatePageSamplesForNotebook_CreatesOnlyForFirstPage() {
        // Setup
        Integer notebookId = 1;
        Integer sampleItemId = 1000;

        NoteBookPage page2 = new NoteBookPage();
        page2.setId(2);
        page2.setTitle("Initial Processing");
        page2.setOrder(2);

        testPage.setOrder(1); // First page

        testNotebook.setPages(Arrays.asList(testPage, page2));

        when(noteBookService.get(notebookId)).thenReturn(testNotebook);
        when(sampleItemService.get(sampleItemId.toString())).thenReturn(testSampleItem);
        when(baseObjectDAO.getByPageIdAndSampleItemId(anyInt(), eq(sampleItemId))).thenReturn(null);

        // Execute
        service.createPageSamplesForNotebook(notebookId, sampleItemId);

        // Verify - T150: insert called ONLY for first page (lazy creation)
        verify(baseObjectDAO, times(1)).insert(any(NotebookPageSample.class));
    }

    /** Test: createPageSamplesForNotebook skips existing samples */
    @Test
    public void testCreatePageSamplesForNotebook_SkipsExisting() {
        // Setup
        Integer notebookId = 1;
        Integer sampleItemId = 1000;

        when(noteBookService.get(notebookId)).thenReturn(testNotebook);
        when(sampleItemService.get(sampleItemId.toString())).thenReturn(testSampleItem);
        when(baseObjectDAO.getByPageIdAndSampleItemId(testPage.getId(), sampleItemId)).thenReturn(testPageSample);

        // Execute
        service.createPageSamplesForNotebook(notebookId, sampleItemId);

        // Verify - insert NOT called because sample already exists
        verify(baseObjectDAO, never()).insert(any(NotebookPageSample.class));
    }

    /** Test: createPageSamplesForNotebook throws for invalid notebook */
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePageSamplesForNotebook_InvalidNotebook_Throws() {
        // Setup
        Integer notebookId = 999;
        Integer sampleItemId = 1000;

        when(noteBookService.get(notebookId)).thenReturn(null);

        // Execute - should throw
        service.createPageSamplesForNotebook(notebookId, sampleItemId);
    }

    /** Test: createPageSamplesForNotebook throws for invalid sample */
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePageSamplesForNotebook_InvalidSample_Throws() {
        // Setup
        Integer notebookId = 1;
        Integer sampleItemId = 999;

        when(noteBookService.get(notebookId)).thenReturn(testNotebook);
        when(sampleItemService.get(sampleItemId.toString())).thenReturn(null);

        // Execute - should throw
        service.createPageSamplesForNotebook(notebookId, sampleItemId);
    }
}
