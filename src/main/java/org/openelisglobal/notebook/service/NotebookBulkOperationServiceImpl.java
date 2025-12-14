package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of bulk operations for notebook page samples. Processes
 * operations in batches of 50 to prevent timeout.
 *
 * Per FR-033: System MUST process bulk operations in batches of 50.
 */
@Service
public class NotebookBulkOperationServiceImpl implements NotebookBulkOperationService {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Override
    @Transactional
    public int bulkApplyValues(Integer pageId, List<Integer> sampleIds, Map<String, Object> data, String userId) {
        if (sampleIds == null || sampleIds.isEmpty() || data == null || data.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;

        // Process in batches of BATCH_SIZE (50)
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            for (Integer sampleId : batch) {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps != null) {
                    // Merge new data with existing data
                    Map<String, Object> existingData = nps.getData();
                    if (existingData == null) {
                        existingData = new HashMap<>();
                    }
                    existingData.putAll(data);
                    nps.setData(existingData);

                    // Update timestamp (BaseObject uses setLastupdated)
                    nps.setLastupdated(new Timestamp(System.currentTimeMillis()));

                    // If status is PENDING and data is being applied, transition to IN_PROGRESS
                    if (nps.getStatus() == Status.PENDING) {
                        nps.setStatus(Status.IN_PROGRESS);
                    }

                    notebookPageSampleService.update(nps);
                    updatedCount++;
                }
            }
        }

        return updatedCount;
    }

    @Override
    @Transactional
    public int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return 0;
        }

        // Delegate to NotebookPageSampleService which handles batch processing
        return notebookPageSampleService.bulkUpdateStatus(pageId, sampleIds, status, userId);
    }

    @Override
    public NotebookPageSampleService.PageProgress getPageProgress(Integer pageId) {
        return notebookPageSampleService.getPageProgress(pageId);
    }

    @Override
    public List<NotebookPageSample> getSamplesPaginated(Integer pageId, Status status, int page, int size) {
        return notebookPageSampleService.getByPageIdPaginated(pageId, status, page, size);
    }

    @Override
    public long getSamplesCount(Integer pageId, Status status) {
        return notebookPageSampleService.getCountByPageId(pageId, status);
    }

    @Override
    @Transactional
    public boolean markPageComplete(Integer pageId, String userId, boolean requireComplete) {
        // Get progress to check if all samples are done
        NotebookPageSampleService.PageProgress progress = notebookPageSampleService.getPageProgress(pageId);

        if (requireComplete) {
            // All samples must be COMPLETED or SKIPPED (no PENDING or IN_PROGRESS)
            if (progress.pending() > 0 || progress.inProgress() > 0) {
                return false;
            }
        }

        // Get the page and mark it as completed
        NoteBookPage page = noteBookPageService.get(pageId);
        if (page != null) {
            page.setCompleted(true);
            page.setLastupdated(new Timestamp(System.currentTimeMillis()));
            noteBookPageService.update(page);
            return true;
        }

        return false;
    }
}
