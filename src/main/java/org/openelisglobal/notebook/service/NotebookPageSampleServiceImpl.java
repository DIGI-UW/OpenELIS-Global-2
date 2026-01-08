package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NotebookPageSample operations. Provides
 * per-sample-per-page tracking for immunology workflow.
 */
@Service
public class NotebookPageSampleServiceImpl extends AuditableBaseObjectServiceImpl<NotebookPageSample, Integer>
        implements NotebookPageSampleService {

    private static final int BATCH_SIZE = 50;

    @Autowired
    private NotebookPageSampleDAO baseObjectDAO;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SampleRoutingService sampleRoutingService;

    public NotebookPageSampleServiceImpl() {
        super(NotebookPageSample.class);
    }

    @Override
    protected BaseDAO<NotebookPageSample, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookPageSample> getByPageId(Integer pageId) {
        return baseObjectDAO.getByPageId(pageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookPageSample> getByPageIdAndStatus(Integer pageId, Status status) {
        return baseObjectDAO.getByPageIdAndStatus(pageId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookPageSample getByPageIdAndSampleItemId(Integer pageId, Integer sampleItemId) {
        return baseObjectDAO.getByPageIdAndSampleItemId(pageId, sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageProgress getPageProgress(Integer pageId) {
        Map<Status, Long> counts = baseObjectDAO.getStatusCountsByPageId(pageId);
        return NotebookPageSampleService.createPageProgress(counts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookPageSample> getBySampleItemId(Integer sampleItemId) {
        return baseObjectDAO.getBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional
    public int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId) {
        int totalUpdated = 0;
        SystemUser user = systemUserService.get(userId);

        // Get the page reference for creating new records
        NoteBookPage page = null;
        // Get next page reference for auto-creating records when COMPLETED (T150)
        NoteBookPage nextPage = null;
        boolean nextPageLoaded = false;

        // Process in batches to avoid memory issues with large sample lists
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            int updated = baseObjectDAO.bulkUpdateStatus(pageId, batch, status);
            totalUpdated += updated;

            // For samples that don't have NotebookPageSample records yet (e.g., dynamically
            // loaded aliquots), create them with the target status
            for (Integer sampleId : batch) {
                NotebookPageSample existing = getByPageIdAndSampleItemId(pageId, sampleId);
                if (existing == null) {
                    // Lazy load page reference only when needed
                    if (page == null) {
                        page = noteBookService.getPage(pageId);
                        if (page == null) {
                            throw new IllegalArgumentException("Page not found: " + pageId);
                        }
                    }

                    // Create a new NotebookPageSample record
                    NotebookPageSample nps = new NotebookPageSample();
                    nps.setNotebookPage(page);
                    nps.setSampleItemId(sampleId.toString());
                    nps.setStatus(status);
                    if (status == Status.COMPLETED && user != null) {
                        nps.setCompletedBy(user);
                        nps.setCompletedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                    }
                    insert(nps);
                    totalUpdated++;
                }
            }

            // If marking as COMPLETED, update completed_by and completed_at for existing
            // records
            if (status == Status.COMPLETED) {
                updateCompletionInfo(pageId, batch, user);

                // T150: Auto-create NotebookPageSample records on the appropriate page
                // For routing pages, samples routed to EXTERNAL_LAB or STORAGE go to archiving
                // page
                // Samples routed to INTERNAL_ANALYSIS go to the next page (Analysis)
                if (!nextPageLoaded) {
                    LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                            "T150: Loading next page for pageId=" + pageId);
                    NoteBookPage currentPageDebug = noteBookService.getPage(pageId);
                    if (currentPageDebug != null) {
                        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                "T150: Current page details - id=" + currentPageDebug.getId() + " title='"
                                        + currentPageDebug.getTitle() + "' order=" + currentPageDebug.getOrder());
                    }

                    nextPage = noteBookService.getNextPage(pageId);
                    nextPageLoaded = true;
                    LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                            "T150: Looking for next page from pageId=" + pageId + ", nextPage="
                                    + (nextPage != null
                                            ? "id=" + nextPage.getId() + " title='" + nextPage.getTitle() + "' order="
                                                    + nextPage.getOrder()
                                            : "null"));
                }

                // Check if this is a routing page or storage page
                boolean isRoutingPage = noteBookService.isRoutingPage(pageId);
                boolean isStoragePage = noteBookService.isStoragePage(pageId);
                LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus", "T150: pageId=" + pageId
                        + " isRoutingPage=" + isRoutingPage + " isStoragePage=" + isStoragePage);
                NoteBookPage archivingPage = null;
                Integer notebookId = null;

                // For both routing pages and storage pages, we need the archiving page
                // Always ensure page is loaded for routing/storage pages
                if (isRoutingPage || isStoragePage) {
                    if (page == null) {
                        page = noteBookService.getPage(pageId);
                    }
                    if (page != null) {
                        // Initialize lazy-loaded notebook reference
                        org.hibernate.Hibernate.initialize(page.getNotebook());
                        if (page.getNotebook() != null) {
                            notebookId = page.getNotebook().getId();

                            // Log notebook pages for debugging
                            org.hibernate.Hibernate.initialize(page.getNotebook().getPages());
                            java.util.List<NoteBookPage> allPages = page.getNotebook().getPages();
                            LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus", "Notebook " + notebookId
                                    + " has " + (allPages != null ? allPages.size() : 0) + " pages");
                            if (allPages != null) {
                                for (NoteBookPage p : allPages) {
                                    LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus", "  Page: id="
                                            + p.getId() + " order=" + p.getOrder() + " title='" + p.getTitle() + "'");
                                }
                            }

                            archivingPage = noteBookService.getLastPage(notebookId);
                            LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                    (isStoragePage ? "Storage" : "Routing") + " page detected: pageId=" + pageId
                                            + ", notebookId=" + notebookId + ", archivingPageId="
                                            + (archivingPage != null ? archivingPage.getId() : "null")
                                            + ", archivingPageTitle="
                                            + (archivingPage != null ? archivingPage.getTitle() : "null"));
                        } else {
                            LogEvent.logWarn(this.getClass().getName(), "bulkUpdateStatus",
                                    "Page notebook reference is null for pageId=" + pageId);
                        }
                    } else {
                        LogEvent.logWarn(this.getClass().getName(), "bulkUpdateStatus",
                                "Could not load page for pageId=" + pageId);
                    }
                }

                for (Integer sampleId : batch) {
                    NoteBookPage targetPage = nextPage; // default to next page

                    LogEvent.logDebug(this.getClass().getName(), "bulkUpdateStatus",
                            "T150: Processing sample " + sampleId + ": isRoutingPage=" + isRoutingPage
                                    + ", isStoragePage=" + isStoragePage + ", targetPage="
                                    + (targetPage != null ? targetPage.getId() : "null"));

                    // For storage pages, always skip to archiving page (Disposal & Archiving)
                    // Storage samples skip the Reporting page
                    if (isStoragePage && archivingPage != null) {
                        targetPage = archivingPage;
                        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                "Sample " + sampleId + " on storage page, skipping to archiving page");
                    }
                    // For routing pages, check where the sample is routed
                    else if (isRoutingPage && notebookId != null) {
                        SampleRouting routing = sampleRoutingService.getByNotebookIdAndSampleItemId(notebookId,
                                sampleId);
                        if (routing != null) {
                            SampleRouting.DestinationType destination = routing.getDestinationType();
                            if (destination == SampleRouting.DestinationType.EXTERNAL_LAB
                                    || destination == SampleRouting.DestinationType.STORAGE) {
                                // Send to archiving page instead of next page
                                targetPage = archivingPage;
                                LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus", "Sample " + sampleId
                                        + " routed to " + destination + ", skipping to archiving page");
                            }
                            // INTERNAL_ANALYSIS samples go to next page (Prep) - targetPage remains
                            // nextPage
                        } else {
                            // On routing page but no routing record - don't create on any next page
                            // Sample must be routed first before advancing
                            targetPage = null;
                            LogEvent.logDebug(this.getClass().getName(), "bulkUpdateStatus", "Sample " + sampleId
                                    + " on routing page but not yet routed - skipping next page creation");
                        }
                    }

                    if (targetPage != null) {
                        // Check if record already exists on target page
                        NotebookPageSample existingOnTargetPage = getByPageIdAndSampleItemId(targetPage.getId(),
                                sampleId);
                        if (existingOnTargetPage == null) {
                            // Create a PENDING record on the target page
                            NotebookPageSample targetPageNps = new NotebookPageSample();
                            targetPageNps.setNotebookPage(targetPage);
                            targetPageNps.setSampleItemId(sampleId.toString());
                            targetPageNps.setStatus(Status.PENDING);
                            insert(targetPageNps);
                            LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                    "T150: Created PENDING record for sample " + sampleId + " on targetPage id="
                                            + targetPage.getId() + " title='" + targetPage.getTitle() + "'");
                        } else {
                            LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                    "T150: Sample " + sampleId + " already exists on targetPage id="
                                            + targetPage.getId() + " with status=" + existingOnTargetPage.getStatus());
                        }
                    } else {
                        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                "T150: targetPage is null for sample " + sampleId + ", not creating record");
                    }
                }
            }
        }

        return totalUpdated;
    }

    private void updateCompletionInfo(Integer pageId, List<Integer> sampleIds, SystemUser user) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Integer sampleId : sampleIds) {
            NotebookPageSample nps = getByPageIdAndSampleItemId(pageId, sampleId);
            if (nps != null) {
                nps.setCompletedBy(user);
                nps.setCompletedAt(now);
                update(nps);
            }
        }
    }

    private void updateCompletionInfoString(Integer pageId, List<String> sampleIds, SystemUser user) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (String sampleId : sampleIds) {
            NotebookPageSample nps = getBySampleItemIdAndPageId(sampleId, pageId);
            if (nps != null) {
                nps.setCompletedBy(user);
                nps.setCompletedAt(now);
                update(nps);
            }
        }
    }

    @Override
    @Transactional
    public int bulkUpdateStatusString(Integer pageId, List<String> sampleIds, Status status, String userId) {
        int totalUpdated = 0;
        SystemUser user = systemUserService.get(userId);

        // Get the page reference for creating new records
        NoteBookPage page = null;

        // Process in batches to avoid memory issues with large sample lists
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<String> batch = sampleIds.subList(i, endIndex);

            // Update existing records using String-based DAO method
            int updated = baseObjectDAO.bulkUpdateStatusString(pageId, batch, status);
            totalUpdated += updated;

            // For samples that don't have NotebookPageSample records yet, create them
            for (String sampleId : batch) {
                NotebookPageSample existing = getBySampleItemIdAndPageId(sampleId, pageId);
                if (existing == null) {
                    // Lazy load page reference only when needed
                    if (page == null) {
                        page = noteBookService.getPage(pageId);
                        if (page == null) {
                            throw new IllegalArgumentException("Page not found: " + pageId);
                        }
                    }

                    // Create a new NotebookPageSample record with string ID
                    NotebookPageSample nps = new NotebookPageSample();
                    nps.setNotebookPage(page);
                    nps.setSampleItemId(sampleId);
                    nps.setStatus(status);
                    if (status == Status.COMPLETED) {
                        nps.setCompletedBy(user);
                        nps.setCompletedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                    }
                    insert(nps);
                    totalUpdated++;
                }
            }

            // If marking as COMPLETED, update completed_by and completed_at for existing
            // records
            if (status == Status.COMPLETED) {
                updateCompletionInfoString(pageId, batch, user);
            }
        }

        return totalUpdated;
    }

    @Override
    @Transactional
    public int bulkApplyData(Integer pageId, List<Integer> sampleIds, Map<String, Object> data, String userId) {
        int totalUpdated = 0;

        // Process in batches
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            for (Integer sampleId : batch) {
                NotebookPageSample nps = getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps != null) {
                    // Merge new data with existing data
                    Map<String, Object> existingData = nps.getData();
                    if (existingData == null) {
                        nps.setData(data);
                    } else {
                        existingData.putAll(data);
                        nps.setData(existingData);
                    }

                    // Update status to IN_PROGRESS if currently PENDING
                    if (nps.getStatus() == Status.PENDING) {
                        nps.setStatus(Status.IN_PROGRESS);
                    }

                    update(nps);
                    totalUpdated++;
                }
            }
        }

        return totalUpdated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookPageSample> getByPageIdPaginated(Integer pageId, Status status, int page, int size) {
        int offset = page * size;
        return baseObjectDAO.getByPageIdPaginated(pageId, status, offset, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCountByPageId(Integer pageId, Status status) {
        return baseObjectDAO.getCountByPageId(pageId, status);
    }

    @Override
    @Transactional
    public void createPageSamplesForNotebook(Integer notebookId, Integer sampleItemId) {
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId, e);
        }
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        SampleItem sampleItem = sampleItemService.get(sampleItemId.toString());
        if (sampleItem == null) {
            throw new IllegalArgumentException("SampleItem not found: " + sampleItemId);
        }

        // Initialize lazy-loaded pages collection
        org.hibernate.Hibernate.initialize(notebook.getPages());

        List<NoteBookPage> pages = notebook.getPages();
        if (pages == null || pages.isEmpty()) {
            return;
        }

        // T150: Only create NotebookPageSample for the FIRST page (Page 1 - Reception)
        // Samples will be auto-created on subsequent pages when completed on previous
        // page
        // Find the first page by order
        NoteBookPage firstPage = pages.stream().filter(p -> p.getOrder() != null)
                .min((p1, p2) -> p1.getOrder().compareTo(p2.getOrder())).orElse(pages.get(0)); // Fallback to first in
                                                                                               // list if no order set

        // Check if page sample already exists on first page
        NotebookPageSample existing = getByPageIdAndSampleItemId(firstPage.getId(), sampleItemId);
        if (existing == null) {
            NotebookPageSample nps = new NotebookPageSample();
            nps.setNotebookPage(firstPage);
            nps.setSampleItemId(sampleItem.getId());
            nps.setStatus(Status.PENDING);
            insert(nps);
        }
    }

    @Override
    @Transactional
    public void createPageSampleForPage(Integer pageId, Integer sampleItemId, Status status) {
        if (pageId == null || sampleItemId == null) {
            return;
        }

        // Check if record already exists
        NotebookPageSample existing = getByPageIdAndSampleItemId(pageId, sampleItemId);
        if (existing != null) {
            return; // Already exists
        }

        NoteBookPage page = noteBookService.getPage(pageId);
        if (page == null) {
            throw new IllegalArgumentException("Page not found: " + pageId);
        }

        NotebookPageSample nps = new NotebookPageSample();
        nps.setNotebookPage(page);
        nps.setSampleItemId(sampleItemId.toString());
        nps.setStatus(status != null ? status : Status.PENDING);
        insert(nps);
    }

    @Override
    @Transactional
    public void createPageSampleForPageString(Integer pageId, String sampleItemId, Status status) {
        createPageSampleForPageString(pageId, sampleItemId, status, null);
    }

    @Override
    @Transactional
    public void createPageSampleForPageString(Integer pageId, String sampleItemId, Status status,
            java.util.Map<String, Object> data) {
        if (pageId == null || sampleItemId == null || sampleItemId.isEmpty()) {
            return;
        }

        // Check if record already exists using string-based lookup
        NotebookPageSample existing = getBySampleItemIdAndPageId(sampleItemId, pageId);
        if (existing != null) {
            return; // Already exists
        }

        NoteBookPage page = noteBookService.getPage(pageId);
        if (page == null) {
            throw new IllegalArgumentException("Page not found: " + pageId);
        }

        NotebookPageSample nps = new NotebookPageSample();
        nps.setNotebookPage(page);
        nps.setSampleItemId(sampleItemId);
        nps.setStatus(status != null ? status : Status.PENDING);
        // Set initial data from source page (preserves test assignments, metadata,
        // etc.)
        if (data != null && !data.isEmpty()) {
            nps.setData(data);
        }
        insert(nps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookPageSample> getByNotebookId(Integer notebookId) {
        return baseObjectDAO.getByNotebookId(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookPageSample getBySampleItemIdAndPageId(String sampleItemId, Integer pageId) {
        return baseObjectDAO.getBySampleItemIdAndPageId(sampleItemId, pageId);
    }
}
