package org.openelisglobal.notebook.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.HashMap;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        // Call overloaded method with skipAutoRouting = false (default behavior - T150
        // enabled)
        return bulkUpdateStatus(pageId, sampleIds, status, userId, false);
    }

    @Override
    @Transactional
    public int bulkUpdateStatus(Integer pageId, List<Integer> sampleIds, Status status, String userId,
            boolean skipAutoRouting) {
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
                // Skip T150 logic if skipAutoRouting is true (frontend handles routing
                // explicitly)
                if (!skipAutoRouting && !nextPageLoaded) {
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
                            // Get source page sample data to copy to target page
                            NotebookPageSample sourcePageSample = getByPageIdAndSampleItemId(pageId, sampleId);
                            Map<String, Object> sourceData = null;
                            if (sourcePageSample != null && sourcePageSample.getData() != null) {
                                // Create a deep copy of the data to avoid reference issues
                                sourceData = new HashMap<>(sourcePageSample.getData());
                            }

                            // Create a PENDING record on the target page
                            NotebookPageSample targetPageNps = new NotebookPageSample();
                            targetPageNps.setNotebookPage(targetPage);
                            targetPageNps.setSampleItemId(sampleId.toString());
                            targetPageNps.setStatus(Status.PENDING);
                            // Copy data from source page to target page
                            if (sourceData != null) {
                                targetPageNps.setData(sourceData);
                                LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatus",
                                        "T150: Copying " + sourceData.size() + " data fields for sample " + sampleId
                                                + " from pageId=" + pageId + " to targetPage id=" + targetPage.getId());
                            }
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
    public int clearDestinationType(Integer pageId, List<Integer> sampleIds, String userId) {
        LogEvent.logInfo(this.getClass().getName(), "clearDestinationType",
                "Clearing destinationType for pageId=" + pageId + ", sampleCount=" + sampleIds.size());

        int updated = baseObjectDAO.clearDestinationType(pageId, sampleIds);

        LogEvent.logInfo(this.getClass().getName(), "clearDestinationType",
                "Successfully cleared destinationType for " + updated + " samples");

        return updated;
    }

    @Override
    @Transactional
    public int bulkApplyData(Integer pageId, List<Integer> sampleIds, Map<String, Object> data, String userId) {
        int totalUpdated = 0;
        LogEvent.logInfo(this.getClass().getName(), "bulkApplyData",
                "Starting bulkApplyData for pageId=" + pageId + ", sampleIds=" + sampleIds);

        // Get page reference for creating new records if needed
        NoteBookPage page = null;

        // Process in batches
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            for (Integer sampleId : batch) {
                LogEvent.logInfo(this.getClass().getName(), "bulkApplyData",
                        "Looking up sample: pageId=" + pageId + ", sampleId=" + sampleId);
                NotebookPageSample nps = getByPageIdAndSampleItemId(pageId, sampleId);

                // If sample doesn't exist on this page, create it
                if (nps == null) {
                    LogEvent.logInfo(this.getClass().getName(), "bulkApplyData",
                            "Sample not found on page, creating: pageId=" + pageId + ", sampleId=" + sampleId);

                    // Lazy load page reference
                    if (page == null) {
                        page = noteBookService.getPage(pageId);
                        if (page == null) {
                            LogEvent.logError(this.getClass().getName(), "bulkApplyData", "Page not found: " + pageId);
                            continue;
                        }
                    }

                    // Create new NotebookPageSample record
                    nps = new NotebookPageSample();
                    nps.setNotebookPage(page);
                    nps.setSampleItemId(sampleId.toString());
                    nps.setStatus(Status.IN_PROGRESS);
                    nps.setData(new HashMap<>(data));
                    insert(nps);
                    totalUpdated++;
                    LogEvent.logInfo(this.getClass().getName(), "bulkApplyData",
                            "Created new sample record with data: pageId=" + pageId + ", sampleId=" + sampleId);
                } else {
                    LogEvent.logInfo(this.getClass().getName(), "bulkApplyData",
                            "Found sample record: id=" + nps.getId() + ", sampleItemId=" + nps.getSampleItemId());
                    // Merge new data with existing data
                    Map<String, Object> existingData = nps.getData();
                    if (existingData == null) {
                        nps.setData(new HashMap<>(data));
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

        LogEvent.logInfo(this.getClass().getName(), "bulkApplyData",
                "Completed bulkApplyData. Updated " + totalUpdated + " samples");
        return totalUpdated;
    }

    @Override
    @Transactional
    public int bulkAppendToArray(Integer pageId, List<Integer> sampleIds, String arrayField,
            Map<String, Object> newEntry, String userId) {
        int totalUpdated = 0;
        LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray", "Starting bulkAppendToArray for pageId="
                + pageId + ", sampleIds=" + sampleIds + ", arrayField=" + arrayField);

        // Get page reference for creating new records if needed
        NoteBookPage page = null;

        // Process in batches
        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            for (Integer sampleId : batch) {
                LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray",
                        "Looking up sample: pageId=" + pageId + ", sampleId=" + sampleId);
                NotebookPageSample nps = getByPageIdAndSampleItemId(pageId, sampleId);

                // If sample doesn't exist on this page, create it
                if (nps == null) {
                    LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray",
                            "Sample not found on page, creating: pageId=" + pageId + ", sampleId=" + sampleId);

                    // Lazy load page reference
                    if (page == null) {
                        page = noteBookService.getPage(pageId);
                        if (page == null) {
                            LogEvent.logError(this.getClass().getName(), "bulkAppendToArray",
                                    "Page not found: " + pageId);
                            continue;
                        }
                    }

                    // Create new NotebookPageSample record with array containing first entry
                    nps = new NotebookPageSample();
                    nps.setNotebookPage(page);
                    nps.setSampleItemId(sampleId.toString());
                    nps.setStatus(Status.IN_PROGRESS);

                    // Initialize data with array field containing first entry
                    Map<String, Object> data = new HashMap<>();
                    List<Map<String, Object>> array = new java.util.ArrayList<>();
                    array.add(newEntry);
                    data.put(arrayField, array);
                    nps.setData(data);

                    insert(nps);
                    entityManager.flush();
                    totalUpdated++;
                    LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray",
                            "Created new sample record with array data: pageId=" + pageId + ", sampleId=" + sampleId);
                } else {
                    LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray",
                            "Found sample record: id=" + nps.getId() + ", sampleItemId=" + nps.getSampleItemId());

                    // Use native SQL to append to JSONB array - Hibernate JSONB type doesn't detect
                    // nested changes
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        String newEntryJson = mapper.writeValueAsString(newEntry);

                        // Use CAST instead of :: to avoid Hibernate parameter parsing issues
                        String sql = "UPDATE clinlims.notebook_page_sample " + "SET data = CASE " + "  WHEN data->'"
                                + arrayField + "' IS NULL THEN jsonb_set(data, '{" + arrayField
                                + "}', CAST(:newArray AS jsonb)) " + "  ELSE jsonb_set(data, '{" + arrayField
                                + "}', (data->'" + arrayField + "' || CAST(:newEntry AS jsonb))) " + "END, "
                                + "status = :status, " + "last_updated = CURRENT_TIMESTAMP " + "WHERE id = :npsId";

                        int updated = entityManager.createNativeQuery(sql)
                                .setParameter("newArray", "[" + newEntryJson + "]")
                                .setParameter("newEntry", newEntryJson)
                                .setParameter("status",
                                        nps.getStatus() == Status.PENDING ? "IN_PROGRESS" : nps.getStatus().name())
                                .setParameter("npsId", nps.getId()).executeUpdate();

                        LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray",
                                "Native SQL updated " + updated + " record(s) for npsId=" + nps.getId());

                        totalUpdated++;
                    } catch (Exception e) {
                        LogEvent.logError(this.getClass().getName(), "bulkAppendToArray",
                                "Error appending to array with native SQL: " + e.getMessage());
                        throw new RuntimeException("Failed to append to JSONB array", e);
                    }
                }
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "bulkAppendToArray",
                "Completed bulkAppendToArray. Updated " + totalUpdated + " samples");
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
        LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                "START: notebookId=" + notebookId + ", sampleItemId=" + sampleItemId);

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
            LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "Notebook retrieved: " + (notebook != null ? notebook.getId() : "null"));
        } catch (org.hibernate.ObjectNotFoundException e) {
            LogEvent.logError("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "Notebook ObjectNotFoundException: " + notebookId + " - " + e.getMessage());
            throw new IllegalArgumentException("Notebook not found: " + notebookId, e);
        } catch (Exception e) {
            LogEvent.logError("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "Unexpected exception getting notebook: " + e.getClass().getName() + " - " + e.getMessage());
            throw e;
        }

        if (notebook == null) {
            LogEvent.logError("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "Notebook is null for ID: " + notebookId);
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        SampleItem sampleItem = sampleItemService.get(sampleItemId.toString());
        if (sampleItem == null) {
            LogEvent.logError("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "SampleItem not found: " + sampleItemId);
            throw new IllegalArgumentException("SampleItem not found: " + sampleItemId);
        }
        LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                "SampleItem retrieved: " + sampleItemId);

        // Initialize lazy-loaded pages collection
        org.hibernate.Hibernate.initialize(notebook.getPages());

        List<NoteBookPage> pages = notebook.getPages();
        if (pages == null || pages.isEmpty()) {
            LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "Notebook has no pages, skipping: notebookId=" + notebookId);
            return;
        }

        LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                "Notebook has " + pages.size() + " pages");

        // T150: Only create NotebookPageSample for the FIRST page (Page 1 - Reception)
        // Samples will be auto-created on subsequent pages when completed on previous
        // page
        // Find the first page by order
        NoteBookPage firstPage = pages.stream().filter(p -> p.getOrder() != null)
                .min((p1, p2) -> p1.getOrder().compareTo(p2.getOrder())).orElse(pages.get(0)); // Fallback to first in
        // list if no order set

        LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                "First page ID: " + firstPage.getId() + ", order: " + firstPage.getOrder());

        // Check if page sample already exists on first page
        NotebookPageSample existing = getByPageIdAndSampleItemId(firstPage.getId(), sampleItemId);
        if (existing == null) {
            LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "Creating new NotebookPageSample for pageId=" + firstPage.getId() + ", sampleItemId="
                            + sampleItemId);
            NotebookPageSample nps = new NotebookPageSample();
            nps.setNotebookPage(firstPage);
            nps.setSampleItemId(sampleItem.getId());
            nps.setStatus(Status.PENDING);
            try {
                insert(nps);
                // Force flush to prevent Hibernate batching issues with duplicate inserts
                // This ensures subsequent queries can see the newly inserted record
                entityManager.flush();
                LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                        "Successfully inserted and flushed NotebookPageSample");
            } catch (Exception e) {
                LogEvent.logError("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                        "Error inserting NotebookPageSample: " + e.getClass().getName() + " - " + e.getMessage());
                throw e;
            }
        } else {
            LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                    "NotebookPageSample already exists, skipping");
        }

        LogEvent.logInfo("NotebookPageSampleServiceImpl", "createPageSamplesForNotebook",
                "END: notebookId=" + notebookId + ", sampleItemId=" + sampleItemId);
    }

    @Override
    @Transactional
    public void createPageSamplesForNotebookWithData(Integer notebookId, Integer sampleItemId,
            java.util.Map<String, Object> data) {
        NoteBook notebook = noteBookService.get(notebookId);
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

        // Only create NotebookPageSample for the FIRST page (Page 1 - Reception)
        NoteBookPage firstPage = pages.stream().filter(p -> p.getOrder() != null)
                .min((p1, p2) -> p1.getOrder().compareTo(p2.getOrder())).orElse(pages.get(0));

        // Check if page sample already exists on first page
        NotebookPageSample existing = getByPageIdAndSampleItemId(firstPage.getId(), sampleItemId);
        if (existing == null) {
            NotebookPageSample nps = new NotebookPageSample();
            nps.setNotebookPage(firstPage);
            nps.setSampleItemId(sampleItem.getId());
            nps.setStatus(Status.PENDING);
            nps.setData(data); // Store the manifest metadata
            insert(nps);
        } else if (data != null && !data.isEmpty()) {
            // Update existing record with data if not already set
            existing.setData(data);
            update(existing);
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

    @Override
    @Transactional
    public int bulkUpdateStatusWithPathwayRouting(Integer pageId, List<Integer> sampleIds, Status status, String userId,
            Boolean pathwayRouting, String sourcePageName, String targetPageName) {
        // If pathway routing is not enabled, delegate to standard bulkUpdateStatus
        if (pathwayRouting == null || !pathwayRouting) {
            return bulkUpdateStatus(pageId, sampleIds, status, userId);
        }

        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatusWithPathwayRouting",
                "Pathway routing enabled: sourcePageName=" + sourcePageName + ", targetPageName=" + targetPageName);

        // Delegate to the core implementation with pathway parameters
        return bulkUpdateStatusWithPathwayRoutingInternal(pageId, sampleIds, status, userId, sourcePageName,
                targetPageName);
    }

    @Override
    @Transactional
    public int bulkUpdateStatusStringWithPathwayRouting(Integer pageId, List<String> sampleIds, Status status,
            String userId, Boolean pathwayRouting, String sourcePageName, String targetPageName) {
        // If pathway routing is not enabled, delegate to standard bulkUpdateStatusString
        if (pathwayRouting == null || !pathwayRouting) {
            return bulkUpdateStatusString(pageId, sampleIds, status, userId);
        }

        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatusStringWithPathwayRouting",
                "Pathway routing enabled: sourcePageName=" + sourcePageName + ", targetPageName=" + targetPageName);

        // For now, convert to integer-based handling
        // In a future enhancement, this can be made more efficient
        List<Integer> intSampleIds = new java.util.ArrayList<>();
        for (String sampleId : sampleIds) {
            try {
                intSampleIds.add(Integer.parseInt(sampleId));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getName(), "bulkUpdateStatusStringWithPathwayRouting",
                        "Could not parse sample ID as integer: " + sampleId + ", skipping for pathway routing");
            }
        }

        return bulkUpdateStatusWithPathwayRoutingInternal(pageId, intSampleIds, status, userId, sourcePageName,
                targetPageName);
    }

    /**
     * Internal implementation of pathway-based routing. Routes samples to different
     * pages based on their pathway selection (analyticalPathwayId in JSONB data).
     *
     * @param pageId        the source page ID
     * @param sampleIds     list of sample item IDs
     * @param status        the new status
     * @param userId        the user performing the update
     * @param sourcePageName the source page name (used to find next page)
     * @param targetPageName the target page name (for path_b routing)
     * @return number of samples updated
     */
    private int bulkUpdateStatusWithPathwayRoutingInternal(Integer pageId, List<Integer> sampleIds, Status status,
            String userId, String sourcePageName, String targetPageName) {
        int totalUpdated = 0;
        SystemUser user = systemUserService.get(userId);

        // Get source page and target page by name
        NoteBookPage sourcePage = noteBookService.getPage(pageId);
        if (sourcePage == null) {
            LogEvent.logError(this.getClass().getName(), "bulkUpdateStatusWithPathwayRoutingInternal",
                    "Source page not found: " + pageId);
            return 0;
        }

        NoteBookPage nextPage = noteBookService.getNextPage(pageId);
        NoteBookPage targetPageForPathB = null;

        // Find target page by name
        if (sourcePage.getNotebook() != null) {
            org.hibernate.Hibernate.initialize(sourcePage.getNotebook().getPages());
            java.util.List<NoteBookPage> allPages = sourcePage.getNotebook().getPages();
            if (allPages != null) {
                for (NoteBookPage p : allPages) {
                    if (targetPageName != null && p.getTitle() != null
                            && p.getTitle().contains(targetPageName)) {
                        targetPageForPathB = p;
                        break;
                    }
                }
            }
        }

        if (targetPageForPathB == null) {
            LogEvent.logWarn(this.getClass().getName(), "bulkUpdateStatusWithPathwayRoutingInternal",
                    "Target page not found by name: " + targetPageName + ", will use default routing");
        }

        // Update status on current page
        int updated = baseObjectDAO.bulkUpdateStatus(pageId, sampleIds, status);
        totalUpdated += updated;

        // Update completion info
        if (status == Status.COMPLETED) {
            updateCompletionInfo(pageId, sampleIds, user);

            // Process pathway-based routing for completed samples
            for (Integer sampleId : sampleIds) {
                NoteBookPage targetPage = nextPage; // default to next page (path_a)

                // Get sample data to check pathway selection
                NotebookPageSample sourcePageSample = getByPageIdAndSampleItemId(pageId, sampleId);
                if (sourcePageSample != null && sourcePageSample.getData() != null) {
                    Map<String, Object> data = sourcePageSample.getData();
                    String selectedPathway = (String) data.get("analyticalPathwayId");

                    // If sample selected path_b and we found the target page, route to target page
                    if ("path_b".equalsIgnoreCase(selectedPathway) && targetPageForPathB != null) {
                        targetPage = targetPageForPathB;
                        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatusWithPathwayRoutingInternal",
                                "Sample " + sampleId + " with pathway=" + selectedPathway + " routed to target page: "
                                        + targetPageForPathB.getTitle());
                    } else if ("path_a".equalsIgnoreCase(selectedPathway)) {
                        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatusWithPathwayRoutingInternal",
                                "Sample " + sampleId + " with pathway=" + selectedPathway + " routed to next page: "
                                        + (nextPage != null ? nextPage.getTitle() : "null"));
                    }
                }

                // Create record on target page if it doesn't already exist
                if (targetPage != null) {
                    NotebookPageSample existingOnTargetPage = getByPageIdAndSampleItemId(targetPage.getId(),
                            sampleId);
                    if (existingOnTargetPage == null) {
                        // Get source data to copy
                        NotebookPageSample sourceSample = getByPageIdAndSampleItemId(pageId, sampleId);
                        Map<String, Object> sourceData = null;
                        if (sourceSample != null && sourceSample.getData() != null) {
                            sourceData = new HashMap<>(sourceSample.getData());
                        }

                        // Create PENDING record on target page
                        NotebookPageSample targetPageNps = new NotebookPageSample();
                        targetPageNps.setNotebookPage(targetPage);
                        targetPageNps.setSampleItemId(sampleId.toString());
                        targetPageNps.setStatus(Status.PENDING);
                        if (sourceData != null) {
                            targetPageNps.setData(sourceData);
                        }
                        insert(targetPageNps);
                        LogEvent.logInfo(this.getClass().getName(), "bulkUpdateStatusWithPathwayRoutingInternal",
                                "Created PENDING record for sample " + sampleId + " on page: "
                                        + targetPage.getTitle());
                    }
                }
            }
        }

        return totalUpdated;
    }
}
