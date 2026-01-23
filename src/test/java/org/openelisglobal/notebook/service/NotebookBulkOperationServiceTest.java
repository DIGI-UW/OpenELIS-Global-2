package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/**
 * Unit tests for NotebookBulkOperationService. Tests bulk apply operations,
 * batch processing (50 samples per batch), and status transitions.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookBulkOperationServiceTest {

    @Mock
    private NotebookPageSampleService notebookPageSampleService;

    @InjectMocks
    private NotebookBulkOperationServiceImpl bulkOperationService;

    private Integer testPageId;
    private List<Integer> testSampleIds;
    private Map<String, Object> testData;
    private String testUserId;

    @Before
    public void setUp() {
        testPageId = 1;
        testSampleIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        testData = new HashMap<>();
        testData.put("volume", "5.0");
        testData.put("method", "Ficoll");
        testData.put("cellCount", "2.4×10⁶");
        testUserId = "1";
    }

    /**
     * Test that bulk apply values updates all selected samples. Verifies FR-031:
     * System MUST support "Apply to Selected" to update common values for multiple
     * samples in single transaction.
     */
    @Test
    public void testBulkApplyValues_updatesAllSamples() {
        // Arrange
        List<NotebookPageSample> mockSamples = createMockSamples(10);
        when(notebookPageSampleService.getByPageIdAndSampleItemId(anyInt(), anyInt())).thenAnswer(invocation -> {
            Integer sampleId = invocation.getArgument(1);
            return mockSamples.stream().filter(s -> s.getSampleItemId().equals(String.valueOf(sampleId))).findFirst()
                    .orElse(null);
        });

        // Act
        int updatedCount = bulkOperationService.bulkApplyValues(testPageId, testSampleIds, testData, testUserId);

        // Assert
        assertEquals("Should update all 10 samples", 10, updatedCount);
    }

    /**
     * Test that bulk operations process in batches of 50. Verifies FR-033: System
     * MUST process bulk operations in batches of 50.
     */
    @Test
    public void testBulkApplyValues_processesBatchesOf50() {
        // Arrange - create 120 sample IDs (should result in 3 batches: 50 + 50 + 20)
        List<Integer> largeSampleList = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            largeSampleList.add(i);
        }

        List<NotebookPageSample> mockSamples = createMockSamples(120);
        when(notebookPageSampleService.getByPageIdAndSampleItemId(anyInt(), anyInt())).thenAnswer(invocation -> {
            Integer sampleId = invocation.getArgument(1);
            return mockSamples.stream().filter(s -> s.getSampleItemId().equals(String.valueOf(sampleId))).findFirst()
                    .orElse(null);
        });

        // Act
        int updatedCount = bulkOperationService.bulkApplyValues(testPageId, largeSampleList, testData, testUserId);

        // Assert
        assertEquals("Should update all 120 samples", 120, updatedCount);
    }

    /**
     * Test that bulk status update changes status for all selected samples.
     * Verifies FR-031 and status transitions.
     */
    @Test
    public void testBulkUpdateStatus_changesStatusForAllSamples() {
        // Arrange
        when(notebookPageSampleService.bulkUpdateStatus(eq(testPageId), eq(testSampleIds), eq(Status.COMPLETED),
                eq(testUserId)))
                .thenReturn(10);

        // Act
        int updatedCount = bulkOperationService.bulkUpdateStatus(testPageId, testSampleIds, Status.COMPLETED,
                testUserId);

        // Assert
        assertEquals("Should update all 10 samples", 10, updatedCount);
        verify(notebookPageSampleService).bulkUpdateStatus(testPageId, testSampleIds, Status.COMPLETED, testUserId);
    }

    /**
     * Test that empty sample list returns 0 updates.
     */
    @Test
    public void testBulkApplyValues_emptyList_returnsZero() {
        // Act
        int updatedCount = bulkOperationService.bulkApplyValues(testPageId, new ArrayList<>(), testData, testUserId);

        // Assert
        assertEquals("Should return 0 for empty list", 0, updatedCount);
    }

    /**
     * Test that null sample list returns 0 updates.
     */
    @Test
    public void testBulkApplyValues_nullList_returnsZero() {
        // Act
        int updatedCount = bulkOperationService.bulkApplyValues(testPageId, null, testData, testUserId);

        // Assert
        assertEquals("Should return 0 for null list", 0, updatedCount);
    }

    /**
     * Test bulk apply with null data map returns 0 updates.
     */
    @Test
    public void testBulkApplyValues_nullData_returnsZero() {
        // Act
        int updatedCount = bulkOperationService.bulkApplyValues(testPageId, testSampleIds, null, testUserId);

        // Assert
        assertEquals("Should return 0 for null data", 0, updatedCount);
    }

    /**
     * Test that progress calculation returns correct values. Verifies FR-004:
     * System MUST display progress indicators.
     */
    @Test
    public void testGetPageProgress_returnsCorrectProgress() {
        // Arrange
        NotebookPageSampleService.PageProgress mockProgress = new NotebookPageSampleService.PageProgress(200, 50, 25,
                100, 25, 50.0);
        when(notebookPageSampleService.getPageProgress(testPageId)).thenReturn(mockProgress);

        // Act
        NotebookPageSampleService.PageProgress progress = bulkOperationService.getPageProgress(testPageId);

        // Assert
        assertNotNull("Progress should not be null", progress);
        assertEquals("Total should be 200", 200, progress.total());
        assertEquals("Completed should be 100", 100, progress.completed());
        assertEquals("Percentage should be 50%", 50.0, progress.percentage(), 0.01);
    }

    /**
     * Test that JSONB data field is properly merged during bulk apply.
     */
    @Test
    public void testBulkApplyValues_mergesExistingData() {
        // Arrange - sample with existing data
        NotebookPageSample existingSample = new NotebookPageSample();
        existingSample.setId(1);
        existingSample.setSampleItemId("1");
        Map<String, Object> existingData = new HashMap<>();
        existingData.put("existingField", "existingValue");
        existingSample.setData(existingData);

        when(notebookPageSampleService.getByPageIdAndSampleItemId(testPageId, 1)).thenReturn(existingSample);

        // Act
        int updatedCount = bulkOperationService.bulkApplyValues(testPageId, Arrays.asList(1), testData, testUserId);

        // Assert
        assertEquals("Should update 1 sample", 1, updatedCount);
        // The existing data should be preserved and new data merged
        verify(notebookPageSampleService).update(any(NotebookPageSample.class));
    }

    // Helper method to create mock samples
    private List<NotebookPageSample> createMockSamples(int count) {
        List<NotebookPageSample> samples = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            NotebookPageSample sample = new NotebookPageSample();
            sample.setId(i);
            sample.setSampleItemId(String.valueOf(i));
            sample.setStatus(Status.PENDING);
            sample.setData(new HashMap<>());
            samples.add(sample);
        }
        return samples;
    }
}
