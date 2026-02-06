package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for sample entry operations in notebooks. Handles
 * searching for samples and linking them to notebook instances.
 */
@Service
public class NotebookSampleEntryServiceImpl implements NotebookSampleEntryService {

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> searchSamples(String accessionNumber, String patientName, String sampleType,
            String dateFrom, String dateTo) {
        // Delegate to SampleItemService for sample searching
        // This is a simplified implementation - the actual search logic
        // may need to be more sophisticated based on the LIMS requirements
        List<SampleItem> results = new ArrayList<>();

        if (accessionNumber != null && !accessionNumber.isBlank()) {
            // Search by accession number
            SampleItem item = sampleItemService.get(accessionNumber);
            if (item != null) {
                results.add(item);
            }
        }
        // Additional search criteria can be added as needed

        return results;
    }

    @Override
    @Transactional
    public int linkSamplesToNotebook(Integer notebookId, List<Integer> sampleItemIds) {
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId, e);
        }
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        int linkedCount = 0;
        List<NoteBookPage> pages = notebook.getPages();

        for (Integer sampleItemId : sampleItemIds) {
            SampleItem sampleItem = sampleItemService.get(sampleItemId.toString());
            if (sampleItem == null) {
                // Skip non-existent samples gracefully
                continue;
            }

            // Create NotebookPageSample record ONLY on the first page
            // Samples should only appear on subsequent pages after completing the previous
            // page
            // (auto-progression is handled by bulkUpdateStatus when status changes to
            // COMPLETED)
            if (pages != null && !pages.isEmpty()) {
                // Sort pages by order to find the first page
                List<NoteBookPage> sortedPages = new ArrayList<>(pages);
                sortedPages.sort((p1, p2) -> {
                    Integer o1 = p1.getOrder() != null ? p1.getOrder() : Integer.MAX_VALUE;
                    Integer o2 = p2.getOrder() != null ? p2.getOrder() : Integer.MAX_VALUE;
                    return o1.compareTo(o2);
                });
                NoteBookPage firstPage = sortedPages.get(0);

                // Check if record already exists on first page
                NotebookPageSample existing = notebookPageSampleDAO.getByPageIdAndSampleItemId(firstPage.getId(),
                        sampleItemId);
                if (existing == null) {
                    NotebookPageSample nps = new NotebookPageSample();
                    nps.setNotebookPage(firstPage);
                    nps.setSampleItemId(sampleItem.getId());
                    nps.setStatus(Status.PENDING);
                    notebookPageSampleService.insert(nps);
                }
            }
            linkedCount++;
        }

        return linkedCount;
    }

    @Override
    @Transactional
    public boolean unlinkSampleFromNotebook(Integer notebookId, Integer sampleItemId) {
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return false;
        }
        if (notebook == null) {
            return false;
        }

        List<NoteBookPage> pages = notebook.getPages();
        if (pages == null || pages.isEmpty()) {
            return false;
        }

        boolean unlinked = false;
        for (NoteBookPage page : pages) {
            NotebookPageSample nps = notebookPageSampleDAO.getByPageIdAndSampleItemId(page.getId(), sampleItemId);
            if (nps != null) {
                notebookPageSampleService.delete(nps);
                unlinked = true;
            }
        }

        return unlinked;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getSamplesForNotebook(Integer notebookId) {
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return new ArrayList<>();
        }
        if (notebook == null || notebook.getPages() == null || notebook.getPages().isEmpty()) {
            return new ArrayList<>();
        }

        // Get samples from the first page (all pages have the same samples)
        NoteBookPage firstPage = notebook.getPages().get(0);
        List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(firstPage.getId());

        // Load SampleItems by their IDs
        List<SampleItem> sampleItems = new ArrayList<>();
        for (NotebookPageSample nps : pageSamples) {
            SampleItem sampleItem = sampleItemService.get(nps.getSampleItemId());
            if (sampleItem != null) {
                sampleItems.add(sampleItem);
            }
        }
        return sampleItems;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSampleLinked(Integer notebookId, Integer sampleItemId) {
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return false;
        }
        if (notebook == null || notebook.getPages() == null || notebook.getPages().isEmpty()) {
            return false;
        }

        // Check if sample exists on any page
        NoteBookPage firstPage = notebook.getPages().get(0);
        NotebookPageSample existing = notebookPageSampleDAO.getByPageIdAndSampleItemId(firstPage.getId(), sampleItemId);
        return existing != null;
    }

    @Override
    @Transactional
    public List<SampleItem> createChildSamples(Integer notebookId, List<Integer> parentSampleIds,
            int childCountPerParent, String externalIdPrefix, String sysUserId) {
        // Default: link to Page 1 (Reception)
        return createChildSamplesForPage(notebookId, null, parentSampleIds, childCountPerParent, externalIdPrefix,
                sysUserId);
    }

    @Override
    @Transactional
    public List<SampleItem> createChildSamplesForPage(Integer notebookId, Integer pageId, List<Integer> parentSampleIds,
            int childCountPerParent, String externalIdPrefix, String sysUserId) {
        if (childCountPerParent <= 0) {
            throw new IllegalArgumentException("Child count per parent must be positive");
        }

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId, e);
        }
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        // Initialize lazy-loaded pages and sort by order
        org.hibernate.Hibernate.initialize(notebook.getPages());
        List<NoteBookPage> sortedPages = new ArrayList<>(notebook.getPages());
        sortedPages.sort((p1, p2) -> {
            Integer o1 = p1.getOrder() != null ? p1.getOrder() : Integer.MAX_VALUE;
            Integer o2 = p2.getOrder() != null ? p2.getOrder() : Integer.MAX_VALUE;
            return o1.compareTo(o2);
        });

        // Find the current page's order to determine which pages are "previous"
        Integer currentPageOrder = null;
        if (pageId != null) {
            NoteBookPage currentPage = noteBookService.getPage(pageId);
            if (currentPage != null) {
                currentPageOrder = currentPage.getOrder();
            }
        }

        List<SampleItem> createdChildren = new ArrayList<>();
        int childSequence = 1;

        for (Integer parentId : parentSampleIds) {
            SampleItem parentSample = sampleItemService.get(parentId.toString());
            if (parentSample == null) {
                throw new IllegalArgumentException("Parent sample not found: " + parentId);
            }

            // Fix detached entity issue: Reload Sample in current transaction to ensure
            // it's managed with proper lastupdated/version field. When parentSample is
            // loaded,
            // its Sample reference may be detached with lastupdated=null, causing Hibernate
            // to treat it as NEW and attempt INSERT when child references parent.
            if (parentSample.getSample() != null && parentSample.getSample().getId() != null) {
                Sample managedSample = sampleService.get(parentSample.getSample().getId());
                parentSample.setSample(managedSample);
            }

            // Create child samples for this parent
            for (int i = 0; i < childCountPerParent; i++) {
                SampleItem child = createChildSample(parentSample, externalIdPrefix, childSequence++, sysUserId);
                createdChildren.add(child);
            }
        }

        // Link children to all pages in the notebook, inheriting status from parent
        if (!createdChildren.isEmpty()) {
            for (SampleItem child : createdChildren) {
                Integer childId = Integer.parseInt(child.getId());
                SampleItem parent = child.getParentSampleItem();
                Integer parentId = parent != null ? Integer.parseInt(parent.getId()) : null;

                // Determine completed order: If child is created on a specific page, all
                // previous
                // pages should be marked as COMPLETED. This is because the parent must have
                // completed
                // all previous pages to reach the current page (implicit workflow progress).
                int completedUpToOrder;
                if (currentPageOrder != null && currentPageOrder > 0) {
                    // Child created on page X means pages 1 to X-1 are already completed
                    completedUpToOrder = currentPageOrder - 1;
                    LogEvent.logInfo(this.getClass().getName(), "createChildSamplesForPage",
                            "Child " + childId + " created on page order " + currentPageOrder
                                    + ", marking pages up to order " + completedUpToOrder + " as COMPLETED");
                } else {
                    // Fallback: check parent's actual COMPLETED status in this notebook
                    List<NotebookPageSample> parentPageSamples = new ArrayList<>();
                    if (parentId != null) {
                        List<NotebookPageSample> allParentRecords = notebookPageSampleService
                                .getBySampleItemId(parentId);
                        for (NotebookPageSample pps : allParentRecords) {
                            if (pps.getNotebookPage() != null && pps.getNotebookPage().getNotebook() != null
                                    && notebookId.equals(pps.getNotebookPage().getNotebook().getId())) {
                                parentPageSamples.add(pps);
                            }
                        }
                    }

                    completedUpToOrder = -1;
                    for (NotebookPageSample pps : parentPageSamples) {
                        if (pps.getStatus() == Status.COMPLETED && pps.getNotebookPage() != null
                                && pps.getNotebookPage().getOrder() != null) {
                            completedUpToOrder = Math.max(completedUpToOrder, pps.getNotebookPage().getOrder());
                        }
                    }
                    LogEvent.logInfo(this.getClass().getName(), "createChildSamplesForPage", "Parent " + parentId
                            + " completed up to page order: " + completedUpToOrder + " (fallback mode)");
                }

                // Create NotebookPageSample records ONLY for pages up to and including
                // the current page (where child is being created). Child will progress to
                // subsequent pages when marked complete on current page.
                for (NoteBookPage page : sortedPages) {
                    Integer pageOrder = page.getOrder() != null ? page.getOrder() : Integer.MAX_VALUE;

                    // Only create records for pages up to the current page
                    if (currentPageOrder != null && pageOrder > currentPageOrder) {
                        continue; // Skip future pages
                    }

                    createChildPageSampleInheritingFromParent(page, childId, parentId, completedUpToOrder,
                            new ArrayList<>());
                }
            }
        }

        return createdChildren;
    }

    @Override
    @Transactional
    public List<SampleItem> createChildSamplesForPage(Integer notebookId, Integer pageId, List<Integer> parentSampleIds,
            int childCountPerParent, String externalIdPrefix, String sysUserId, Map<String, Object> aliquotData) {
        // Create the children using the base method
        List<SampleItem> children = createChildSamplesForPage(notebookId, pageId, parentSampleIds, childCountPerParent,
                externalIdPrefix, sysUserId);

        // If aliquot data is provided, store it in each child's page sample record
        if (aliquotData != null && !aliquotData.isEmpty() && !children.isEmpty()) {
            // Find the page where children were created (or use first page if not
            // specified)
            Integer targetPageId = pageId;
            if (targetPageId == null) {
                NoteBook notebook = null;
                try {
                    notebook = noteBookService.get(notebookId);
                } catch (org.hibernate.ObjectNotFoundException e) {
                    // Notebook not found, skip aliquot data assignment
                }
                if (notebook != null && notebook.getPages() != null && !notebook.getPages().isEmpty()) {
                    // Get the first page by order
                    List<NoteBookPage> pages = new ArrayList<>(notebook.getPages());
                    pages.sort((p1, p2) -> {
                        Integer o1 = p1.getOrder() != null ? p1.getOrder() : Integer.MAX_VALUE;
                        Integer o2 = p2.getOrder() != null ? p2.getOrder() : Integer.MAX_VALUE;
                        return o1.compareTo(o2);
                    });
                    targetPageId = pages.get(0).getId();
                }
            }

            // Update the NotebookPageSample records with aliquot data
            if (targetPageId != null) {
                for (SampleItem child : children) {
                    Integer childId = Integer.parseInt(child.getId());
                    NotebookPageSample nps = notebookPageSampleDAO.getByPageIdAndSampleItemId(targetPageId, childId);
                    if (nps != null) {
                        // Merge aliquot data with any existing data
                        Map<String, Object> existingData = nps.getData();
                        if (existingData == null) {
                            existingData = new HashMap<>();
                        }
                        existingData.putAll(aliquotData);
                        nps.setData(existingData);
                        notebookPageSampleService.update(nps);

                        LogEvent.logInfo(this.getClass().getName(), "createChildSamplesForPage",
                                "Stored aliquot data for child sample " + childId + " on page " + targetPageId + ": "
                                        + aliquotData);
                    }
                }
            }
        }

        return children;
    }

    /**
     * Create a child NotebookPageSample record, inheriting COMPLETED status based
     * on how far parent has progressed in the workflow.
     */
    private void createChildPageSampleInheritingFromParent(NoteBookPage page, Integer childSampleId, Integer parentId,
            int parentCompletedUpToOrder, List<NotebookPageSample> parentPageSamples) {
        // Check if record already exists
        NotebookPageSample existing = notebookPageSampleDAO.getByPageIdAndSampleItemId(page.getId(), childSampleId);
        if (existing != null) {
            return; // Already exists
        }

        Integer pageOrder = page.getOrder() != null ? page.getOrder() : Integer.MAX_VALUE;

        NotebookPageSample nps = new NotebookPageSample();
        nps.setNotebookPage(page);
        nps.setSampleItemId(childSampleId.toString());

        // If the page order is <= the order where parent has completed,
        // then child should also be COMPLETED on this page
        if (pageOrder <= parentCompletedUpToOrder) {
            // Find parent's page sample for this page to copy data from
            NotebookPageSample parentPageSample = null;
            for (NotebookPageSample pps : parentPageSamples) {
                if (pps.getNotebookPage() != null && pps.getNotebookPage().getId().equals(page.getId())) {
                    parentPageSample = pps;
                    break;
                }
            }

            nps.setStatus(Status.COMPLETED);
            nps.setCompletedAt(new Timestamp(System.currentTimeMillis()));

            // Copy parent's data if available
            if (parentPageSample != null) {
                nps.setCompletedBy(parentPageSample.getCompletedBy());
                if (parentPageSample.getData() != null) {
                    Map<String, Object> childData = new HashMap<>(parentPageSample.getData());
                    childData.put("inheritedFromParent", parentId);
                    childData.put("inheritedValidation", true);
                    nps.setData(childData);
                }
            } else {
                // Parent doesn't have a record on this page but we know parent completed
                // past this point, so mark child as completed
                Map<String, Object> childData = new HashMap<>();
                childData.put("inheritedFromParent", parentId);
                childData.put("inheritedValidation", true);
                nps.setData(childData);
            }

            LogEvent.logInfo(this.getClass().getName(), "createChildPageSampleInheritingFromParent",
                    "Child " + childSampleId + " set to COMPLETED on page " + page.getId() + " (order " + pageOrder
                            + ") because parent completed up to order " + parentCompletedUpToOrder);
        } else {
            // Page is beyond where parent has completed - child starts as PENDING
            nps.setStatus(Status.PENDING);
        }

        notebookPageSampleService.insert(nps);
    }

    /**
     * Creates a single child sample from a parent.
     */
    private SampleItem createChildSample(SampleItem parent, String externalIdPrefix, int sequence, String sysUserId) {
        SampleItem child = new SampleItem();

        // Create a NEW Sample object with a unique accession number for this child
        // This ensures children don't share the same accession number as their parent
        Sample parentSample = parent.getSample();
        Sample childSample = new Sample();

        // Copy relevant properties from parent's Sample
        childSample.setEnteredDate(parentSample.getEnteredDate()); // Required NOT NULL field
        childSample.setCollectionDate(parentSample.getCollectionDate());
        childSample.setReceivedDate(parentSample.getReceivedDate());
        childSample.setReceivedTimestamp(parentSample.getReceivedTimestamp());
        childSample.setStatusId(parentSample.getStatusId());
        childSample.setDomain(parentSample.getDomain());
        childSample.setReferringId(parentSample.getReferringId());
        childSample.setClinicalOrderId(parentSample.getClinicalOrderId());
        childSample.setPriority(parentSample.getPriority());
        childSample.setSysUserId(sysUserId);

        // Generate unique accession number and insert the new Sample
        sampleService.generateAccessionNumberAndInsert(childSample);

        // Set the new Sample on the child SampleItem
        child.setSample(childSample);
        child.setTypeOfSample(parent.getTypeOfSample());
        child.setSourceOfSample(parent.getSourceOfSample());
        child.setCollector(parent.getCollector());
        child.setCollectionDate(parent.getCollectionDate());
        child.setStatusId(parent.getStatusId());
        child.setSortOrder(parent.getSortOrder()); // Required NOT NULL field
        child.setSysUserId(sysUserId); // Required for audit trail

        // Set parent reference
        child.setParentSampleItem(parent);

        // Generate external ID: prefix-year-sequence (e.g., IMM-C-2024-0001)
        String year = String.valueOf(java.time.Year.now().getValue());
        String formattedSequence = String.format("%04d", sequence);
        String prefix = externalIdPrefix != null ? externalIdPrefix : "CHILD";
        child.setExternalId(prefix + "-" + year + "-" + formattedSequence);

        // Insert the child sample
        String childId = sampleItemService.insert(child);
        child.setId(childId);

        return child;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getChildSamples(Integer parentSampleId) {
        SampleItem parent = sampleItemService.get(parentSampleId.toString());
        if (parent == null) {
            return new ArrayList<>();
        }

        // Get all samples that have this sample as their parent
        return sampleItemService.getChildSamples(parent);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleItem getParentSample(Integer childSampleId) {
        SampleItem child = sampleItemService.get(childSampleId.toString());
        if (child == null) {
            return null;
        }
        return child.getParentSampleItem();
    }
}
