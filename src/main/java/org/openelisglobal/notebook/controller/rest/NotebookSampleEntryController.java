package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.medlab.service.OrderSampleLinkService;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.service.NotebookSampleEntryService;
import org.openelisglobal.notebook.service.SampleRoutingService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for notebook sample entry operations. Handles searching for
 * samples and linking them to notebook instances.
 */
@RestController
@RequestMapping(value = "/rest/notebook")
public class NotebookSampleEntryController extends BaseRestController {

    @Autowired
    private NotebookSampleEntryService sampleEntryService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private SampleRoutingService sampleRoutingService;

    @Autowired
    private StorageBoxDAO storageBoxDAO;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private OrderSampleLinkService orderSampleLinkService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    /**
     * Search for samples to add to a notebook. T035: GET
     * /notebook/{id}/samples/search
     *
     * @param notebookId      the notebook ID
     * @param accessionNumber optional accession number filter
     * @param patientName     optional patient name filter
     * @param sampleType      optional sample type filter
     * @param dateFrom        optional start date filter
     * @param dateTo          optional end date filter
     * @return list of matching samples
     */
    @GetMapping(value = "/{notebookId}/samples/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> searchSamples(@PathVariable("notebookId") Integer notebookId,
            @RequestParam(required = false) String accessionNumber, @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String sampleType, @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        // Verify notebook exists
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        List<SampleItem> samples = sampleEntryService.searchSamples(accessionNumber, patientName, sampleType, dateFrom,
                dateTo);

        List<SampleDisplayBean> displayBeans = samples.stream().map(noteBookService::convertSampleToDisplayBean)
                .collect(Collectors.toList());

        return ResponseEntity.ok(displayBeans);
    }

    /**
     * Link samples to a notebook. T036: POST /notebook/{id}/samples/link
     *
     * @param notebookId  the notebook ID
     * @param request     contains sampleItemIds to link
     * @param httpRequest for getting user session
     * @return linking result with count
     */
    @PostMapping(value = "/{notebookId}/samples/link", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> linkSamples(@PathVariable("notebookId") Integer notebookId,
            @RequestBody LinkSamplesRequest request, HttpServletRequest httpRequest) {

        // Verify notebook exists
        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getSampleItemIds() == null || request.getSampleItemIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        int linkedCount = sampleEntryService.linkSamplesToNotebook(notebookId, request.getSampleItemIds());

        Map<String, Object> result = new HashMap<>();
        result.put("linkedCount", linkedCount);
        result.put("notebookId", notebookId);
        result.put("totalRequested", request.getSampleItemIds().size());

        return ResponseEntity.ok(result);
    }

    /**
     * Get samples linked to a notebook.
     *
     * @param notebookId the notebook ID
     * @return list of linked samples
     */
    @GetMapping(value = "/{notebookId}/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> getLinkedSamples(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        List<SampleItem> samples = sampleEntryService.getSamplesForNotebook(notebookId);

        List<SampleDisplayBean> displayBeans = samples.stream().map(noteBookService::convertSampleToDisplayBean)
                .collect(Collectors.toList());

        return ResponseEntity.ok(displayBeans);
    }

    /**
     * Get page progress for all pages in a notebook.
     *
     * @param notebookId the notebook ID
     * @return map of page ID to progress
     */
    @GetMapping(value = "/{notebookId}/pages/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<Integer, NotebookPageSampleService.PageProgress>> getPageProgress(
            @PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        Map<Integer, NotebookPageSampleService.PageProgress> progressMap = new HashMap<>();
        if (notebook.getPages() != null) {
            for (NoteBookPage page : notebook.getPages()) {
                NotebookPageSampleService.PageProgress progress = notebookPageSampleService
                        .getPageProgress(page.getId());
                progressMap.put(page.getId(), progress);
            }
        }

        return ResponseEntity.ok(progressMap);
    }

    /**
     * Get samples for a specific notebook page with their page-specific status. GET
     * /notebook/page/{pageId}/samples
     *
     * Includes hierarchy information (parent/child relationships) for display.
     *
     * @param pageId the notebook page ID
     * @param status optional filter by page status (PENDING, IN_PROGRESS,
     *               COMPLETED, SKIPPED)
     * @return list of samples with their page status and hierarchy info
     */
    @GetMapping(value = "/page/{pageId}/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getPageSamples(@PathVariable("pageId") Integer pageId,
            @RequestParam(required = false) String status) {
        List<org.openelisglobal.notebook.valueholder.NotebookPageSample> pageSamples;
        try {
            // If status filter provided, use filtered query
            if (status != null && !status.isBlank()) {
                org.openelisglobal.notebook.valueholder.NotebookPageSample.Status statusEnum = org.openelisglobal.notebook.valueholder.NotebookPageSample.Status
                        .valueOf(status.trim().toUpperCase());
                pageSamples = notebookPageSampleService.getByPageIdAndStatus(pageId, statusEnum);
            } else {
                pageSamples = notebookPageSampleService.getByPageId(pageId);
            }
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getName(), "getPageSamples",
                    "Invalid status filter: " + status + " - returning all samples");
            pageSamples = notebookPageSampleService.getByPageId(pageId);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getPageSamples",
                    "Error loading page samples for page " + pageId + ": " + e.getMessage());
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }

        org.openelisglobal.sampleitem.service.SampleItemService sampleItemService = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.sampleitem.service.SampleItemService.class);

        // Get the notebook ID from the page for routing lookups
        Integer notebookId = null;
        org.openelisglobal.notebook.valueholder.NoteBookPage page = noteBookService.getPage(pageId);
        if (page != null && page.getNotebook() != null) {
            notebookId = page.getNotebook().getId();
        }

        // Track which sample IDs are already in the list
        java.util.Set<String> includedSampleIds = new java.util.HashSet<>();
        List<Map<String, Object>> sampleMaps = new java.util.ArrayList<>();

        LogEvent.logInfo(this.getClass().getName(), "getPageSamples",
                "Loading samples for pageId=" + pageId + ", found " + pageSamples.size() + " NotebookPageSample records");

        // First pass: build sample maps from page samples
        for (org.openelisglobal.notebook.valueholder.NotebookPageSample nps : pageSamples) {
            String sampleItemId = nps.getSampleItemId();

            LogEvent.logInfo(this.getClass().getName(), "getPageSamples",
                    "Processing NPS: id=" + nps.getId() + ", sampleItemId=" + sampleItemId
                            + ", data=" + (nps.getData() != null ? nps.getData().toString() : "null"));

            // Check if this is a composite/virtual sample ID (e.g., "123_cassette_0",
            // "123_block_0")
            // These IDs contain underscores and don't correspond to actual SampleItem
            // entities
            // We must check BEFORE calling sampleItemService.get() because that call will
            // throw NumberFormatException when Hibernate tries to convert the ID to integer
            if (sampleItemId != null && sampleItemId.contains("_")) {
                // Handle virtual/composite sample IDs directly without database lookup
                Map<String, Object> virtualSampleMap = buildVirtualSampleMap(sampleItemId, nps);
                sampleMaps.add(virtualSampleMap);
                includedSampleIds.add(sampleItemId);
                continue;
            }

            org.openelisglobal.sampleitem.valueholder.SampleItem sampleItem = sampleItemService.get(sampleItemId);
            if (sampleItem != null) {
                Map<String, Object> sampleMap = buildSampleMap(sampleItem, nps);
                sampleMaps.add(sampleMap);
                includedSampleIds.add(sampleItem.getId());
                LogEvent.logInfo(this.getClass().getName(), "getPageSamples",
                        "Added sample: sampleItem.id=" + sampleItem.getId() + ", data in map=" + sampleMap.get("data"));
            } else {
                // Handle cases where sampleItemId is numeric but no SampleItem exists
                // This shouldn't normally happen but handle gracefully
                LogEvent.logWarn(this.getClass().getName(), "getPageSamples",
                        "SampleItem not found for sampleItemId=" + nps.getSampleItemId() + ", creating virtual sample");
                Map<String, Object> virtualSampleMap = buildVirtualSampleMap(sampleItemId, nps);
                sampleMaps.add(virtualSampleMap);
                includedSampleIds.add(sampleItemId);
            }
        }

        // Second pass: for each parent sample on the page, also include its children
        // ONLY if the children have their own NotebookPageSample record for this page
        // This prevents children from appearing on pages they haven't been explicitly
        // added to
        // IMPORTANT: Skip virtual/composite samples (e.g., "123_cassette_0") as they
        // don't have actual SampleItem entities and can't have children
        List<Map<String, Object>> childMaps = new java.util.ArrayList<>();
        for (Map<String, Object> sampleMap : sampleMaps) {
            // Skip virtual samples - they don't have real SampleItem entities
            if (Boolean.TRUE.equals(sampleMap.get("isVirtual"))) {
                continue;
            }

            Boolean isAliquot = (Boolean) sampleMap.get("isAliquot");
            if (isAliquot == null || !isAliquot) {
                // This is a parent sample - get its children
                String sampleId = String.valueOf(sampleMap.get("id"));

                // Skip if sampleId is not a valid numeric ID (composite IDs contain
                // underscores)
                if (sampleId == null || sampleId.contains("_")) {
                    continue;
                }

                try {
                    int sampleIdInt = Integer.parseInt(sampleId);
                    List<SampleItem> children = sampleEntryService.getChildSamples(sampleIdInt);
                    for (SampleItem child : children) {
                        if (!includedSampleIds.contains(child.getId())) {
                            // Look up the child's actual NotebookPageSample record for this page
                            org.openelisglobal.notebook.valueholder.NotebookPageSample childNps = notebookPageSampleService
                                    .getBySampleItemIdAndPageId(child.getId(), pageId);

                            // CRITICAL FIX: Only include child if it has a NotebookPageSample record on
                            // THIS page
                            // This prevents children from automatically appearing when only their parent is
                            // on the page
                            if (childNps != null) {
                                Map<String, Object> childMap = buildSampleMap(child, childNps);
                                childMaps.add(childMap);
                                includedSampleIds.add(child.getId());
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric sample IDs (composite IDs like "123_cassette_0")
                } catch (Exception e) {
                    // Skip child samples if query fails (type mismatch in legacy data)
                    LogEvent.logWarn(this.getClass().getName(), "getPageSamples",
                            "Could not load child samples for " + sampleId + ": " + e.getMessage());
                }
            }
        }
        sampleMaps.addAll(childMaps);

        // Repeat for newly added children (recursive - one level deep for children of
        // children). Only real SampleItem children can have their own children.
        List<Map<String, Object>> grandchildMaps = new java.util.ArrayList<>();
        for (Map<String, Object> childMap : childMaps) {
            String childId = String.valueOf(childMap.get("id"));
            // Skip non-numeric IDs
            if (childId == null || childId.contains("_")) {
                continue;
            }
            try {
                int childIdInt = Integer.parseInt(childId);
                List<SampleItem> grandchildren = sampleEntryService.getChildSamples(childIdInt);
                for (SampleItem grandchild : grandchildren) {
                    if (!includedSampleIds.contains(grandchild.getId())) {
                        org.openelisglobal.notebook.valueholder.NotebookPageSample grandchildNps = notebookPageSampleService
                                .getBySampleItemIdAndPageId(grandchild.getId(), pageId);
                        Map<String, Object> grandchildMap = buildSampleMap(grandchild, grandchildNps);
                        grandchildMaps.add(grandchildMap);
                        includedSampleIds.add(grandchild.getId());
                    }
                }
            } catch (NumberFormatException e) {
                // Skip non-numeric sample IDs
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "getPageSamples",
                        "Could not load grandchild samples for " + childId + ": " + e.getMessage());
            }
        }
        sampleMaps.addAll(grandchildMaps);

        // Repeat for grandchildren to get great-grandchildren (supports 4-level
        // hierarchy: Specimen → Cassette → Block → Slide)
        List<Map<String, Object>> greatGrandchildMaps = new java.util.ArrayList<>();
        for (Map<String, Object> grandchildMap : grandchildMaps) {
            String grandchildId = String.valueOf(grandchildMap.get("id"));
            // Skip non-numeric IDs
            if (grandchildId == null || grandchildId.contains("_")) {
                continue;
            }
            try {
                int grandchildIdInt = Integer.parseInt(grandchildId);
                List<SampleItem> greatGrandchildren = sampleEntryService.getChildSamples(grandchildIdInt);
                for (SampleItem greatGrandchild : greatGrandchildren) {
                    if (!includedSampleIds.contains(greatGrandchild.getId())) {
                        org.openelisglobal.notebook.valueholder.NotebookPageSample greatGrandchildNps = notebookPageSampleService
                                .getBySampleItemIdAndPageId(greatGrandchild.getId(), pageId);
                        Map<String, Object> greatGrandchildMap = buildSampleMap(greatGrandchild, greatGrandchildNps);
                        greatGrandchildMaps.add(greatGrandchildMap);
                        includedSampleIds.add(greatGrandchild.getId());
                    }
                }
            } catch (NumberFormatException e) {
                // Skip non-numeric sample IDs
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "getPageSamples",
                        "Could not load great-grandchild samples for " + grandchildId + ": " + e.getMessage());
            }
        }
        sampleMaps.addAll(greatGrandchildMaps);

        // Third pass: count children for each parent
        Map<String, Integer> childCountMap = new HashMap<>();
        for (Map<String, Object> sampleMap : sampleMaps) {
            Object parentIdObj = sampleMap.get("parentSampleItemId");
            if (parentIdObj != null) {
                String parentId = String.valueOf(parentIdObj);
                childCountMap.merge(parentId, 1, Integer::sum);
            }
        }

        // Fourth pass: add child counts to parent samples
        for (Map<String, Object> sampleMap : sampleMaps) {
            String sampleId = String.valueOf(sampleMap.get("id"));
            Integer childCount = childCountMap.getOrDefault(sampleId, 0);
            sampleMap.put("childAliquotCount", childCount);
            sampleMap.put("hasChildren", childCount > 0);
        }

        // Fifth pass: add routing information for each sample
        if (notebookId != null) {
            for (Map<String, Object> sampleMap : sampleMaps) {
                String sampleId = String.valueOf(sampleMap.get("id"));
                try {
                    org.openelisglobal.notebook.valueholder.SampleRouting routing = sampleRoutingService
                            .getByNotebookIdAndSampleItemId(notebookId, Integer.parseInt(sampleId));
                    if (routing != null) {
                        sampleMap.put("destinationType",
                                routing.getDestinationType() != null ? routing.getDestinationType().name() : null);
                        sampleMap.put("wellCoordinate", routing.getWellCoordinate());
                        sampleMap.put("routingStatus", "ROUTED");
                    } else {
                        sampleMap.put("destinationType", null);
                        sampleMap.put("wellCoordinate", null);
                        sampleMap.put("routingStatus", "UNROUTED");
                    }
                } catch (Exception e) {
                    // If routing lookup fails, mark as unrouted
                    sampleMap.put("destinationType", null);
                    sampleMap.put("wellCoordinate", null);
                    sampleMap.put("routingStatus", "UNROUTED");
                }
            }
        }

        // Sort to show parents before their children (by external ID, then nesting
        // level)
        sampleMaps.sort((a, b) -> {
            // First compare by parent or self
            String aParent = (String) a.get("parentExternalId");
            String bParent = (String) b.get("parentExternalId");
            String aExt = (String) a.get("externalId");
            String bExt = (String) b.get("externalId");

            // Group by parent external ID (or own ID if no parent)
            String aGroup = aParent != null ? aParent : aExt;
            String bGroup = bParent != null ? bParent : bExt;

            int groupCompare = (aGroup != null && bGroup != null) ? aGroup.compareTo(bGroup) : 0;
            if (groupCompare != 0) {
                return groupCompare;
            }

            // Within same group, parents first (nesting level 0 before 1)
            int aLevel = (Integer) a.getOrDefault("nestingLevel", 0);
            int bLevel = (Integer) b.getOrDefault("nestingLevel", 0);
            return Integer.compare(aLevel, bLevel);
        });

        return ResponseEntity.ok(sampleMaps);
    }

    /**
     * Build a sample map from a SampleItem entity.
     */
    private Map<String, Object> buildSampleMap(SampleItem sampleItem,
            org.openelisglobal.notebook.valueholder.NotebookPageSample nps) {
        Map<String, Object> sampleMap = new HashMap<>();
        sampleMap.put("id", sampleItem.getId());
        sampleMap.put("sampleItemId", sampleItem.getId()); // Duplicate for routing lookup
        sampleMap.put("externalId", sampleItem.getExternalId());
        sampleMap.put("sortOrder", sampleItem.getSortOrder());
        sampleMap.put("sampleId", sampleItem.getSample() != null ? sampleItem.getSample().getId() : null);

        if (nps != null) {
            String status = nps.getStatus() != null ? nps.getStatus().name() : "PENDING";
            sampleMap.put("pageStatus", status);
            sampleMap.put("status", status); // For SampleGrid status column display
            sampleMap.put("pageSampleId", nps.getId());
            Map<String, Object> npsData = nps.getData();
            sampleMap.put("data", npsData);
            // Extract key fields from data object to top level for easier frontend access
            if (npsData != null) {
                if (npsData.containsKey("sampleCategory")) {
                    sampleMap.put("sampleCategory", npsData.get("sampleCategory"));
                }
                if (npsData.containsKey("receivedDateTime")) {
                    sampleMap.put("receivedDateTime", npsData.get("receivedDateTime"));
                }
                if (npsData.containsKey("sourceFacility")) {
                    sampleMap.put("sourceFacility", npsData.get("sourceFacility"));
                }
                if (npsData.containsKey("specimenSite")) {
                    sampleMap.put("specimenSite", npsData.get("specimenSite"));
                }
                if (npsData.containsKey("receivingStaff")) {
                    sampleMap.put("receivingStaff", npsData.get("receivingStaff"));
                }
            }
        } else {
            sampleMap.put("pageStatus", "PENDING");
            sampleMap.put("status", "PENDING"); // For SampleGrid status column display
            sampleMap.put("pageSampleId", null);
            sampleMap.put("data", null);
        }

        if (sampleItem.getTypeOfSample() != null) {
            sampleMap.put("sampleType", sampleItem.getTypeOfSample().getDescription());
            sampleMap.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
        }
        if (sampleItem.getSample() != null) {
            sampleMap.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
        }
        // Add collection date
        if (sampleItem.getCollectionDate() != null) {
            sampleMap.put("collectionDate", org.openelisglobal.common.util.DateUtil
                    .convertTimestampToStringDate(sampleItem.getCollectionDate()));
        } else {
            sampleMap.put("collectionDate", null);
        }

        // Hierarchy information - calculate nesting level by following parent chain
        SampleItem parentSample = sampleItem.getParentSampleItem();
        if (parentSample != null) {
            sampleMap.put("isAliquot", true);
            // Calculate actual nesting level by walking up the parent chain
            int nestingLevel = 1;
            SampleItem ancestor = parentSample;
            while (ancestor.getParentSampleItem() != null) {
                nestingLevel++;
                ancestor = ancestor.getParentSampleItem();
            }
            sampleMap.put("nestingLevel", nestingLevel);
            sampleMap.put("parentSampleItemId", parentSample.getId());
            sampleMap.put("parentExternalId", parentSample.getExternalId());
        } else {
            sampleMap.put("isAliquot", false);
            sampleMap.put("nestingLevel", 0);
            sampleMap.put("parentSampleItemId", null);
            sampleMap.put("parentExternalId", null);
        }

        // Patient information (for MedLab workflow)
        if (sampleItem.getSample() != null) {
            try {
                Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());
                if (patient != null) {
                    String patientName = patientService.getLastFirstName(patient);
                    sampleMap.put("patientName", patientName);
                    sampleMap.put("patientId", patient.getId());
                } else {
                    sampleMap.put("patientName", "Participant");
                    sampleMap.put("patientId", null);
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "buildSampleMap",
                        "Could not load patient for sample " + sampleItem.getId() + ": " + e.getMessage());
                sampleMap.put("patientName", "Participant");
                sampleMap.put("patientId", null);
            }
        } else {
            sampleMap.put("patientName", "Participant");
            sampleMap.put("patientId", null);
        }

        // Linked order information (for MedLab workflow)
        try {
            List<OrderSampleLink> orderLinks = orderSampleLinkService
                    .getLinksBySampleItemId(Integer.parseInt(sampleItem.getId()));
            if (orderLinks != null && !orderLinks.isEmpty()) {
                OrderSampleLink firstLink = orderLinks.get(0);
                ElectronicOrder order = electronicOrderService.get(String.valueOf(firstLink.getElectronicOrderId()));
                if (order != null) {
                    sampleMap.put("linkedOrderLabNo", order.getExternalId());
                    sampleMap.put("linkedOrderId", order.getId());
                } else {
                    sampleMap.put("linkedOrderLabNo", null);
                    sampleMap.put("linkedOrderId", null);
                }
            } else {
                sampleMap.put("linkedOrderLabNo", null);
                sampleMap.put("linkedOrderId", null);
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "buildSampleMap",
                    "Could not load order for sample " + sampleItem.getId() + ": " + e.getMessage());
            sampleMap.put("linkedOrderLabNo", null);
            sampleMap.put("linkedOrderId", null);
        }

        return sampleMap;
    }

    /**
     * Build a sample map for virtual/composite sample IDs that don't have actual
     * SampleItem entities. These are created by pathology workflow expansion (e.g.,
     * "123_cassette_0", "123_block_0", "123_slide_0").
     *
     * @param sampleItemId the composite sample ID string
     * @param nps          the NotebookPageSample record containing the stored data
     * @return a map with sample data suitable for frontend display
     */
    private Map<String, Object> buildVirtualSampleMap(String sampleItemId,
            org.openelisglobal.notebook.valueholder.NotebookPageSample nps) {
        Map<String, Object> sampleMap = new HashMap<>();
        sampleMap.put("id", sampleItemId);
        sampleMap.put("sampleItemId", sampleItemId);

        // Extract parent sample ID and type from composite ID (e.g., "123_cassette_0"
        // -> parentId=123, type=cassette)
        String parentSampleId = null;
        String virtualType = null;
        Integer childIndex = null;
        if (sampleItemId != null && sampleItemId.contains("_")) {
            String[] parts = sampleItemId.split("_");
            if (parts.length >= 3) {
                parentSampleId = parts[0];
                virtualType = parts[1]; // cassette, block, or slide
                try {
                    childIndex = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    childIndex = 0;
                }
            }
        }
        sampleMap.put("parentSampleId", parentSampleId);
        sampleMap.put("virtualType", virtualType);
        sampleMap.put("childIndex", childIndex);

        // Set status from page sample
        if (nps != null) {
            sampleMap.put("pageStatus", nps.getStatus() != null ? nps.getStatus().name() : "PENDING");
            sampleMap.put("status", nps.getStatus() != null ? nps.getStatus().name() : "PENDING");
            sampleMap.put("pageSampleId", nps.getId());
            Map<String, Object> npsData = nps.getData();
            sampleMap.put("data", npsData);

            // Extract key fields from data to top level for easier frontend access
            if (npsData != null) {
                // Block-specific fields
                if (npsData.containsKey("blockCount")) {
                    sampleMap.put("blockCount", npsData.get("blockCount"));
                }
                if (npsData.containsKey("numberOfBlocks")) {
                    sampleMap.put("numberOfBlocks", npsData.get("numberOfBlocks"));
                }
                if (npsData.containsKey("blocksCreated")) {
                    sampleMap.put("blocksCreated", npsData.get("blocksCreated"));
                }
                if (npsData.containsKey("embeddingQuality")) {
                    sampleMap.put("embeddingQuality", npsData.get("embeddingQuality"));
                }
                // Slide-specific fields
                if (npsData.containsKey("slideCount")) {
                    sampleMap.put("slideCount", npsData.get("slideCount"));
                }
                if (npsData.containsKey("numberOfSlides")) {
                    sampleMap.put("numberOfSlides", npsData.get("numberOfSlides"));
                }
                if (npsData.containsKey("slidesCreated")) {
                    sampleMap.put("slidesCreated", npsData.get("slidesCreated"));
                }
                if (npsData.containsKey("sectionQuality")) {
                    sampleMap.put("sectionQuality", npsData.get("sectionQuality"));
                }
                if (npsData.containsKey("sectionThickness")) {
                    sampleMap.put("sectionThickness", npsData.get("sectionThickness"));
                }
                // QC fields
                if (npsData.containsKey("qcStatus")) {
                    sampleMap.put("qcStatus", npsData.get("qcStatus"));
                }
                // Technician info
                if (npsData.containsKey("technicianName")) {
                    sampleMap.put("technicianName", npsData.get("technicianName"));
                }
                // Dates
                if (npsData.containsKey("blockDate")) {
                    sampleMap.put("blockDate", npsData.get("blockDate"));
                }
                if (npsData.containsKey("slideDate")) {
                    sampleMap.put("slideDate", npsData.get("slideDate"));
                }
            }
        } else {
            sampleMap.put("pageStatus", "PENDING");
            sampleMap.put("status", "PENDING");
            sampleMap.put("pageSampleId", null);
            sampleMap.put("data", null);
        }

        // Virtual samples don't have direct SampleItem entity references
        sampleMap.put("isVirtual", true);
        sampleMap.put("isAliquot", false);
        sampleMap.put("nestingLevel", 0);
        sampleMap.put("parentSampleItemId", parentSampleId);
        sampleMap.put("parentExternalId", null);
        sampleMap.put("externalId", null);
        sampleMap.put("sortOrder", childIndex != null ? childIndex : 0);

        return sampleMap;
    }

    // ================== ASSAY RUN MANAGEMENT ENDPOINTS ==================

    /**
     * Get assay runs for a page. GET /notebook/page/{pageId}/assay-runs
     *
     * Assay runs are stored in the page's data field under the key "assayRuns".
     *
     * @param pageId the page ID
     * @return list of assay run configurations
     */
    @GetMapping(value = "/page/{pageId}/assay-runs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAssayRuns(@PathVariable("pageId") Integer pageId) {
        NoteBookPage page = noteBookService.getPage(pageId);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> pageData = page.getData();
        if (pageData == null || !pageData.containsKey("assayRuns")) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assayRuns = (List<Map<String, Object>>) pageData.get("assayRuns");
        return ResponseEntity.ok(assayRuns != null ? assayRuns : new java.util.ArrayList<>());
    }

    /**
     * Save/update an assay run for a page. POST /notebook/page/{pageId}/assay-runs
     *
     * Creates or updates an assay run configuration.
     *
     * @param pageId      the page ID
     * @param assayRun    the assay run configuration
     * @param httpRequest for getting user session
     * @return success or error response
     */
    @PostMapping(value = "/page/{pageId}/assay-runs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAssayRun(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> assayRun, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        NoteBookPage page = noteBookService.getPage(pageId);
        if (page == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Page not found");
            return ResponseEntity.notFound().build();
        }

        String assayRunId = (String) assayRun.get("assayRunId");
        if (assayRunId == null || assayRunId.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "assayRunId is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Get or initialize page data
            Map<String, Object> pageData = page.getData();
            if (pageData == null) {
                pageData = new HashMap<>();
            }

            // Get or initialize assay runs list
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assayRuns = (List<Map<String, Object>>) pageData.get("assayRuns");
            if (assayRuns == null) {
                assayRuns = new java.util.ArrayList<>();
            }

            // Check if this is an update or new
            boolean found = false;
            for (int i = 0; i < assayRuns.size(); i++) {
                Map<String, Object> existing = assayRuns.get(i);
                if (assayRunId.equals(existing.get("assayRunId"))) {
                    // Update existing
                    assayRuns.set(i, assayRun);
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Add new assay run
                assayRuns.add(assayRun);
            }

            // Save back to page
            pageData.put("assayRuns", assayRuns);
            page.setData(pageData);
            page.setSysUserId(sysUserId);
            noteBookPageService.update(page);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("assayRunId", assayRunId);
            result.put("message", found ? "Assay run updated" : "Assay run created");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "saveAssayRun",
                    "Error saving assay run for page " + pageId + ": " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to save assay run: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get a specific assay run by ID. GET
     * /notebook/page/{pageId}/assay-runs/{assayRunId}
     *
     * @param pageId     the page ID
     * @param assayRunId the assay run ID
     * @return the assay run configuration
     */
    @GetMapping(value = "/page/{pageId}/assay-runs/{assayRunId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAssayRun(@PathVariable("pageId") Integer pageId,
            @PathVariable("assayRunId") String assayRunId) {

        NoteBookPage page = noteBookService.getPage(pageId);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> pageData = page.getData();
        if (pageData == null || !pageData.containsKey("assayRuns")) {
            return ResponseEntity.notFound().build();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assayRuns = (List<Map<String, Object>>) pageData.get("assayRuns");
        if (assayRuns == null) {
            return ResponseEntity.notFound().build();
        }

        for (Map<String, Object> run : assayRuns) {
            if (assayRunId.equals(run.get("assayRunId"))) {
                return ResponseEntity.ok(run);
            }
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Delete samples from a notebook page. POST
     * /notebook/page/{pageId}/samples/delete
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds to delete
     * @param httpRequest for getting user session
     * @return deletion result with count
     */
    @PostMapping(value = "/page/{pageId}/samples/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSamplesFromPage(@PathVariable("pageId") Integer pageId,
            @RequestBody DeleteSamplesRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        // Verify page exists
        NoteBookPage page = noteBookService.getPage(pageId);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }

        int deletedCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Integer sampleId : request.getSampleIds()) {
            try {
                // Remove the NotebookPageSample record (link between page and sample)
                org.openelisglobal.notebook.valueholder.NotebookPageSample nps = notebookPageSampleService
                        .getByPageIdAndSampleItemId(pageId, sampleId);
                if (nps != null) {
                    notebookPageSampleService.delete(nps);
                    deletedCount++;
                } else {
                    errors.add("Sample " + sampleId + " not found on page");
                }
            } catch (Exception e) {
                errors.add("Error deleting sample " + sampleId + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("pageId", pageId);
        result.put("totalRequested", request.getSampleIds().size());
        result.put("success", errors.isEmpty());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Create a notebook instance from a template.
     *
     * @param templateId  the template ID
     * @param request     contains title for the new instance
     * @param httpRequest for getting user session
     * @return the created notebook
     */
    @PostMapping(value = "/templates/{templateId}/instance", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createInstanceFromTemplate(
            @PathVariable("templateId") Integer templateId, @RequestBody CreateInstanceRequest request,
            HttpServletRequest httpRequest) {

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Title is required");
            return ResponseEntity.badRequest().body(error);
        }

        String sysUserId = getSysUserId(httpRequest);

        try {
            NoteBook instance = noteBookService.createInstanceFromTemplate(templateId, request.getTitle(), sysUserId);

            Map<String, Object> result = new HashMap<>();
            result.put("id", instance.getId());
            result.put("title", instance.getTitle());
            result.put("status", instance.getStatus());
            result.put("pageCount", instance.getPages() != null ? instance.getPages().size() : 0);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Create child samples from parent samples. T078: POST
     * /notebook/{id}/samples/create-children
     *
     * @param notebookId  the notebook ID
     * @param request     contains parentSampleIds, childCountPerParent,
     *                    externalIdPrefix
     * @param httpRequest for getting user session
     * @return created child samples
     */
    @PostMapping(value = "/{notebookId}/samples/create-children", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createChildSamples(@PathVariable("notebookId") Integer notebookId,
            @RequestBody CreateChildSamplesRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getParentSampleIds() == null || request.getParentSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No parent sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        int childCount = request.getChildCountPerParent() > 0 ? request.getChildCountPerParent() : 1;
        String prefix = request.getExternalIdPrefix() != null ? request.getExternalIdPrefix() : "CHILD";
        Integer pageId = request.getPageId(); // Optional: page where child samples should appear

        // Convert AliquotData to Map for storing in JSONB data field
        Map<String, Object> aliquotDataMap = null;
        if (request.getAliquotData() != null) {
            aliquotDataMap = new HashMap<>();
            AliquotData ad = request.getAliquotData();
            if (ad.getAliquotType() != null) {
                aliquotDataMap.put("aliquotType", ad.getAliquotType());
            }
            if (ad.getVolume() != null) {
                aliquotDataMap.put("volume", ad.getVolume());
            }
            if (ad.getInitialVolume() != null) {
                aliquotDataMap.put("initialVolume", ad.getInitialVolume());
            }
            if (ad.getVolumeUnit() != null) {
                aliquotDataMap.put("volumeUnit", ad.getVolumeUnit());
            }
            if (ad.getDbsSpots() != null) {
                aliquotDataMap.put("dbsSpots", ad.getDbsSpots());
            }
            if (ad.getNotes() != null) {
                aliquotDataMap.put("notes", ad.getNotes());
            }
        }

        try {
            List<SampleItem> children = sampleEntryService.createChildSamplesForPage(notebookId, pageId,
                    request.getParentSampleIds(), childCount, prefix, sysUserId, aliquotDataMap);

            List<SampleDisplayBean> displayBeans = children.stream().map(noteBookService::convertSampleToDisplayBean)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("createdCount", children.size());
            result.put("notebookId", notebookId);
            result.put("children", displayBeans);
            result.put("success", true);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get child samples for a parent sample.
     *
     * @param parentSampleId the parent sample ID
     * @return list of child samples
     */
    @GetMapping(value = "/samples/{parentSampleId}/children", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> getChildSamples(
            @PathVariable("parentSampleId") Integer parentSampleId) {
        List<SampleItem> children = sampleEntryService.getChildSamples(parentSampleId);

        List<SampleDisplayBean> displayBeans = children.stream().map(noteBookService::convertSampleToDisplayBean)
                .collect(Collectors.toList());

        return ResponseEntity.ok(displayBeans);
    }

    /**
     * Get parent sample for a child sample.
     *
     * @param childSampleId the child sample ID
     * @return the parent sample or 404
     */
    @GetMapping(value = "/samples/{childSampleId}/parent", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<SampleDisplayBean> getParentSample(@PathVariable("childSampleId") Integer childSampleId) {
        SampleItem parent = sampleEntryService.getParentSample(childSampleId);
        if (parent == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(noteBookService.convertSampleToDisplayBean(parent));
    }

    /**
     * Route samples to a destination. T084: POST /notebook/{id}/samples/route
     *
     * @param notebookId  the notebook ID
     * @param request     routing request with sampleIds, destinationType, etc.
     * @param httpRequest for getting user session
     * @return routing result
     */
    @PostMapping(value = "/{notebookId}/samples/route", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> routeSamples(@PathVariable("notebookId") Integer notebookId,
            @RequestBody RouteSamplesRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getDestinationType() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Destination type is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            int routedCount = 0;
            DestinationType destType = DestinationType.valueOf(request.getDestinationType().toUpperCase());

            switch (destType) {
            case INTERNAL_ANALYSIS:
                // Internal Analysis uses assay plates (temporary, NOT connected to storage
                // hierarchy)
                AssayPlateInfo assayPlate = request.getAssayPlate();
                if (assayPlate == null && request.getBoxId() == null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Assay plate or box ID required for internal analysis routing");
                    return ResponseEntity.badRequest().body(error);
                }

                if (assayPlate != null) {
                    // Use assay plate routing (temporary plates, NOT in storage hierarchy)
                    Map<Integer, String> wellAssignments = request.getWellAssignments();
                    if (wellAssignments == null || wellAssignments.isEmpty()) {
                        // Auto-generate well assignments for assay plate
                        wellAssignments = new HashMap<>();
                        int columns = assayPlate.getColumns();
                        int index = 0;
                        for (Integer sampleId : request.getSampleIds()) {
                            String wellCoord = sampleRoutingService.generateWellCoordinate(index, columns);
                            wellAssignments.put(sampleId, wellCoord);
                            index++;
                        }
                    }
                    routedCount = sampleRoutingService.bulkRouteToAssayPlate(notebookId, request.getSampleIds(),
                            assayPlate.getName(), wellAssignments, sysUserId);
                } else {
                    // Legacy: use storage box for internal analysis (if boxId provided)
                    Map<Integer, String> wellAssignments = request.getWellAssignments();
                    if (wellAssignments == null || wellAssignments.isEmpty()) {
                        wellAssignments = sampleRoutingService.autoAssignWells(notebookId, request.getSampleIds(),
                                request.getBoxId(), 12);
                    }
                    routedCount = sampleRoutingService.bulkRouteToInternalAnalysis(notebookId, request.getSampleIds(),
                            request.getBoxId(), wellAssignments, sysUserId);
                }
                break;

            case EXTERNAL_LAB:
                for (Integer sampleId : request.getSampleIds()) {
                    java.time.LocalDate shipmentDate = request.getShipmentDate() != null
                            ? java.time.LocalDate.parse(request.getShipmentDate())
                            : null;
                    sampleRoutingService.routeToExternalLab(notebookId, sampleId, request.getExternalLabName(),
                            shipmentDate, sysUserId);
                    routedCount++;
                }
                break;

            case STORAGE:
                // Storage can optionally use box/well assignment (like INTERNAL_ANALYSIS)
                // or just be marked as "for storage" without immediate assignment
                if (request.getBoxId() != null) {
                    // Auto-assign wells if box provided
                    Map<Integer, String> storageWellAssignments = request.getWellAssignments();
                    if (storageWellAssignments == null || storageWellAssignments.isEmpty()) {
                        storageWellAssignments = sampleRoutingService.autoAssignWells(notebookId,
                                request.getSampleIds(), request.getBoxId(), 12);
                    }
                    routedCount = sampleRoutingService.bulkRouteToStorage(notebookId, request.getSampleIds(),
                            request.getBoxId(), storageWellAssignments, sysUserId);
                } else {
                    // No box specified - just mark as routed to storage (pending assignment)
                    for (Integer sampleId : request.getSampleIds()) {
                        sampleRoutingService.routeToStorage(notebookId, sampleId, request.getStorageAssignmentId(),
                                sysUserId);
                        routedCount++;
                    }
                }
                break;

            default:
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unknown destination type: " + request.getDestinationType());
                return ResponseEntity.badRequest().body(error);
            }

            // Update page status to COMPLETED if pageId is provided
            if (request.getPageId() != null && routedCount > 0) {
                notebookPageSampleService.bulkUpdateStatus(request.getPageId(), request.getSampleIds(),
                        org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED, sysUserId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("routedCount", routedCount);
            result.put("notebookId", notebookId);
            result.put("destinationType", destType.name());
            result.put("success", true);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get routing summary for a notebook. T085: GET /notebook/{id}/samples/routing
     *
     * @param notebookId the notebook ID
     * @return routing summary with counts by destination type
     */
    @GetMapping(value = "/{notebookId}/samples/routing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoutingSummary(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        SampleRoutingService.RoutingSummary summary = sampleRoutingService.getRoutingSummary(notebookId);

        Map<String, Object> result = new HashMap<>();
        result.put("internalAnalysis", summary.internalAnalysis());
        result.put("externalLab", summary.externalLab());
        result.put("storage", summary.storage());
        result.put("unrouted", summary.unrouted());
        result.put("total", summary.total());
        result.put("notebookId", notebookId);

        return ResponseEntity.ok(result);
    }

    /**
     * Get all routing records for a notebook.
     *
     * @param notebookId      the notebook ID
     * @param destinationType optional filter by destination type
     * @return list of routing records
     */
    @GetMapping(value = "/{notebookId}/routing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getRoutingRecords(@PathVariable("notebookId") Integer notebookId,
            @RequestParam(required = false) String destinationType) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        List<SampleRouting> routings;
        if (destinationType != null && !destinationType.isBlank()) {
            DestinationType destType = DestinationType.valueOf(destinationType.toUpperCase());
            routings = sampleRoutingService.getByNotebookIdAndDestinationType(notebookId, destType);
        } else {
            routings = sampleRoutingService.getByNotebookId(notebookId);
        }

        List<Map<String, Object>> result = routings.stream().map(this::convertRoutingToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get box layout showing occupied wells. T086: GET
     * /notebook/{id}/box/{boxId}/layout
     *
     * Returns both notebook-specific routing AND global SampleStorageAssignment
     * records to show a complete picture of occupied wells.
     *
     * @param notebookId    the notebook ID
     * @param boxId         the box ID
     * @param includeGlobal if true, include global SampleStorageAssignment records
     *                      (default: true)
     * @return map of well coordinates to routing info
     */
    @GetMapping(value = "/{notebookId}/box/{boxId}/layout", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBoxLayout(@PathVariable("notebookId") Integer notebookId,
            @PathVariable("boxId") Integer boxId,
            @RequestParam(required = false, defaultValue = "true") Boolean includeGlobal) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, SampleRouting> layout = sampleRoutingService.getBoxLayout(notebookId, boxId);

        Map<String, Object> result = new HashMap<>();
        result.put("notebookId", notebookId);
        result.put("boxId", boxId);

        // Convert routing records to displayable format
        Map<String, Map<String, Object>> wells = new HashMap<>();
        for (Map.Entry<String, SampleRouting> entry : layout.entrySet()) {
            Map<String, Object> wellInfo = convertRoutingToMap(entry.getValue());
            wellInfo.put("source", "notebook"); // Mark as notebook-specific
            wells.put(entry.getKey(), wellInfo);
        }

        // Also include global SampleStorageAssignment records if requested
        // This ensures we show ALL occupied positions, not just those in SampleRouting
        if (Boolean.TRUE.equals(includeGlobal)) {
            try {
                org.openelisglobal.storage.dao.SampleStorageAssignmentDAO assignmentDAO = org.openelisglobal.spring.util.SpringContext
                        .getBean(org.openelisglobal.storage.dao.SampleStorageAssignmentDAO.class);
                Map<String, Map<String, String>> globalOccupied = assignmentDAO
                        .getOccupiedCoordinatesWithSampleInfo(boxId);

                for (Map.Entry<String, Map<String, String>> entry : globalOccupied.entrySet()) {
                    String coord = entry.getKey();
                    if (!wells.containsKey(coord)) {
                        // Add global assignment that's not in notebook routing
                        Map<String, Object> wellInfo = new HashMap<>();
                        wellInfo.put("sampleItemId", entry.getValue().get("sampleItemId"));
                        wellInfo.put("externalId", entry.getValue().get("externalId"));
                        wellInfo.put("source", "global"); // Mark as global storage
                        wells.put(coord, wellInfo);
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "getBoxLayout",
                        "Could not load global storage assignments: " + e.getMessage());
            }

            // Also include archived pathology samples from NotebookPageSample JSONB data
            // This handles composite sample IDs (e.g., "4_cassette_0_block_0_slide_0")
            // that cannot be stored in SampleStorageAssignment (which requires Integer IDs)
            try {
                Map<String, Map<String, String>> archivedOccupied = notebookPageSampleDAO
                        .getOccupiedWellsByBoxId(boxId);

                for (Map.Entry<String, Map<String, String>> entry : archivedOccupied.entrySet()) {
                    String coord = entry.getKey();
                    if (!wells.containsKey(coord)) {
                        // Add archived pathology sample that's not in other sources
                        Map<String, Object> wellInfo = new HashMap<>();
                        wellInfo.put("sampleItemId", entry.getValue().get("sampleItemId"));
                        wellInfo.put("externalId", entry.getValue().get("externalId"));
                        wellInfo.put("source", "archived"); // Mark as archived pathology
                        wells.put(coord, wellInfo);
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "getBoxLayout",
                        "Could not load archived pathology assignments: " + e.getMessage());
            }
        }

        result.put("wells", wells);
        result.put("occupiedCount", wells.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Check well availability in a box.
     *
     * @param notebookId     the notebook ID
     * @param boxId          the box ID
     * @param wellCoordinate the well coordinate to check
     * @return availability status
     */
    @GetMapping(value = "/{notebookId}/box/{boxId}/well/{wellCoordinate}/available", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkWellAvailability(@PathVariable("notebookId") Integer notebookId,
            @PathVariable("boxId") Integer boxId, @PathVariable("wellCoordinate") String wellCoordinate) {

        boolean available = sampleRoutingService.isWellAvailable(notebookId, boxId, wellCoordinate);

        Map<String, Object> result = new HashMap<>();
        result.put("wellCoordinate", wellCoordinate);
        result.put("boxId", boxId);
        result.put("available", available);

        return ResponseEntity.ok(result);
    }

    /**
     * Assign samples to storage with conditions and retention period. T112: POST
     * /notebook/{id}/samples/assign-storage
     *
     * US6: Store processed samples under defined conditions with tracked location
     * and retention period using existing SampleStorageService.
     *
     * @param notebookId  the notebook ID
     * @param request     storage assignment request
     * @param httpRequest for getting user session
     * @return assignment result
     */
    @PostMapping(value = "/{notebookId}/samples/assign-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignSamplesToStorage(@PathVariable("notebookId") Integer notebookId,
            @RequestBody AssignStorageRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Support both sampleIds (integers) and sampleIdsString (composite strings)
        List<String> effectiveSampleIds = request.getEffectiveSampleIds();
        if (effectiveSampleIds.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        // Support both boxId (for well-based storage) and locationId (for hierarchical
        // storage)
        if (request.getBoxId() == null && request.getLocationId() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Storage box ID or location ID is required");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getCondition() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Storage condition is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            org.openelisglobal.notebook.valueholder.StorageCondition condition = org.openelisglobal.notebook.valueholder.StorageCondition
                    .valueOf(request.getCondition().toUpperCase());

            int retentionYears = request.getRetentionYears() > 0 ? request.getRetentionYears() : 5;

            // Check if we're dealing with composite sample IDs (e.g.,
            // "4_cassette_0_block_0")
            // Composite IDs cannot be used with SampleRouting or SampleStorageAssignment
            // services
            boolean hasCompositeSampleIds = effectiveSampleIds.stream().anyMatch(id -> id.contains("_"));

            int assignedCount = 0;
            Map<Integer, String> wellAssignments = null;

            // Only use routing/storage services for non-composite (integer) sample IDs
            if (!hasCompositeSampleIds && request.getSampleIds() != null && !request.getSampleIds().isEmpty()) {
                // If reassign flag is set, delete existing routing records first
                if (request.isReassign()) {
                    for (Integer sampleId : request.getSampleIds()) {
                        SampleRouting existingRouting = sampleRoutingService.getByNotebookIdAndSampleItemId(notebookId,
                                sampleId);
                        if (existingRouting != null) {
                            LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                                    "Deleting existing routing for sample " + sampleId + " (reassignment)");
                            sampleRoutingService.delete(existingRouting);
                        }
                    }
                }

                if (request.getBoxId() != null) {
                    // Use box-based storage with well assignments
                    wellAssignments = request.getWellAssignmentsAsIntegerMap();
                    if (wellAssignments == null || wellAssignments.isEmpty()) {
                        // Auto-assign wells if not provided
                        wellAssignments = sampleRoutingService.autoAssignWells(notebookId, request.getSampleIds(),
                                request.getBoxId(), 12);
                    }
                    assignedCount = sampleRoutingService.bulkRouteToStorage(notebookId, request.getSampleIds(),
                            request.getBoxId(), wellAssignments, sysUserId);
                } else if (request.getLocationId() != null) {
                    // Use hierarchical location-based storage
                    assignedCount = sampleRoutingService.bulkRouteToStorage(notebookId, request.getSampleIds(),
                            request.getLocationId(),
                            request.getLocationType() != null ? request.getLocationType() : "rack", condition,
                            retentionYears, sysUserId);
                }

                // Also create SampleStorageAssignment records for global Storage Management
                // integration
                java.time.LocalDate expiryDate = sampleRoutingService.calculateExpiryDate(retentionYears);
                int storageAssignmentCount = 0;
                for (Integer sampleId : request.getSampleIds()) {
                    try {
                        String notes = String.format("Notebook: %s | Condition: %s | Retention: %d years | Expiry: %s",
                                notebook.getTitle() != null ? notebook.getTitle() : String.valueOf(notebookId),
                                condition.name(), retentionYears, expiryDate.toString());

                        String wellCoord = wellAssignments != null ? wellAssignments.get(sampleId) : null;
                        String locationIdForAssign = request.getBoxId() != null ? request.getBoxId().toString()
                                : request.getLocationId();
                        String locationTypeForAssign = request.getBoxId() != null ? "box"
                                : (request.getLocationType() != null ? request.getLocationType() : "rack");

                        LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                                "Creating SampleStorageAssignment for sample " + sampleId + " at "
                                        + locationTypeForAssign + " " + locationIdForAssign + " (well: " + wellCoord
                                        + ")");

                        // Check if sample already has a storage assignment BEFORE trying to assign
                        // This avoids the transaction rollback issue that occurs when
                        // assignSampleItemWithLocation throws an exception
                        // NOTE: Check for sampleItemId key presence (not location) since an assignment
                        // record may exist with an empty location path (e.g., after disposal or
                        // partial assignment)
                        Map<String, Object> existingLocation = sampleStorageService
                                .getSampleItemLocation(sampleId.toString());
                        boolean hasExistingAssignment = existingLocation != null && !existingLocation.isEmpty()
                                && existingLocation.containsKey("sampleItemId");

                        if (request.isReassign() || hasExistingAssignment) {
                            // Use move for reassignment or if sample already has an assignment
                            sampleStorageService.moveSampleItemWithLocation(sampleId.toString(), locationIdForAssign,
                                    locationTypeForAssign, wellCoord,
                                    hasExistingAssignment ? "Notebook assignment update" : "Notebook reassignment",
                                    notes);
                        } else {
                            // Create new assignment
                            sampleStorageService.assignSampleItemWithLocation(sampleId.toString(), locationIdForAssign,
                                    locationTypeForAssign, wellCoord, notes);
                        }
                        storageAssignmentCount++;
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getName(), "assignSamplesToStorage",
                                "Error creating SampleStorageAssignment for sample " + sampleId + ": "
                                        + e.getMessage());
                    }
                }
                LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage", "Created "
                        + storageAssignmentCount + " SampleStorageAssignment records for notebook " + notebookId);
            } else if (hasCompositeSampleIds) {
                // For composite sample IDs (pathology workflow), we skip routing/storage
                // services
                // and just update the NotebookPageSample records with storage info directly
                LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                        "Using composite sample IDs - skipping routing/storage services, updating page samples directly");
                assignedCount = effectiveSampleIds.size();
            }

            // Calculate expiry date for page sample updates
            java.time.LocalDate expiryDate = sampleRoutingService.calculateExpiryDate(retentionYears);

            // Determine which page to update based on request.pageId
            // If pageId is provided (from Routing page), update that page's samples as
            // COMPLETED
            // Otherwise, find the Storage page and update those samples
            Integer targetPageId = request.getPageId();
            NoteBookPage targetPage = null;

            if (targetPageId != null) {
                // Request came from a specific page (e.g., Routing page)
                targetPage = noteBookService.getPage(targetPageId);
            }

            if (targetPage == null) {
                // Fallback to Storage page (Page 7)
                targetPage = findStoragePageForNotebook(notebook);
            }

            if (targetPage != null) {
                // Update NotebookPageSample records with storage info
                // Use effectiveSampleIds (strings) to support composite IDs (e.g.,
                // "4_cassette_0")
                for (String sampleIdStr : effectiveSampleIds) {
                    try {
                        // Use string-based lookup for composite sample IDs
                        org.openelisglobal.notebook.valueholder.NotebookPageSample nps = notebookPageSampleService
                                .getBySampleItemIdAndPageId(sampleIdStr, targetPage.getId());

                        // Auto-create if doesn't exist - this is needed to store storage data
                        if (nps == null) {
                            notebookPageSampleService.createPageSampleForPageString(targetPage.getId(), sampleIdStr,
                                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS);
                            nps = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr, targetPage.getId());
                        }

                        if (nps != null) {
                            // Samples routed to storage should NOT be marked as COMPLETED
                            // They stay on the routing page as IN_PROGRESS (storage assigned but not moving
                            // forward)
                            // Only mark as IN_PROGRESS if currently PENDING
                            if (nps.getStatus() == org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.PENDING) {
                                nps.setStatus(
                                        org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS);
                            }

                            // Store storage info in data field
                            // Store storage info in data field (status update handled separately below)
                            Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                    : new HashMap<>();
                            data.put("storageCondition", condition.name());
                            data.put("retentionYears", retentionYears);
                            data.put("retentionExpiry", expiryDate.toString());

                            // Save dateStored if provided
                            if (request.getDateStored() != null && !request.getDateStored().isEmpty()) {
                                data.put("dateStored", request.getDateStored());
                            } else {
                                // Default to today's date if not provided
                                data.put("dateStored", java.time.LocalDate.now().toString());
                            }

                            // Add box/well info if available
                            if (request.getBoxId() != null) {
                                data.put("boxId", request.getBoxId());
                                // Get well coordinate using string key (supports composite sample IDs)
                                String wellCoord = request.getWellAssignments() != null
                                        ? request.getWellAssignments().get(sampleIdStr)
                                        : null;
                                if (wellCoord != null) {
                                    data.put("wellCoordinate", wellCoord);
                                    data.put("storageWell", wellCoord); // Frontend expects this field name
                                }
                                // Build storage location string and save box info
                                org.openelisglobal.storage.valueholder.StorageBox box = storageBoxDAO
                                        .get(request.getBoxId()).orElse(null);
                                if (box != null) {
                                    String boxLabel = box.getLabel();
                                    data.put("storageBox", boxLabel); // Frontend expects this field name
                                    if (wellCoord != null) {
                                        data.put("storageLocation", boxLabel + " - " + wellCoord);
                                    } else {
                                        data.put("storageLocation", boxLabel);
                                    }
                                    // Build storage path from box hierarchy if available
                                    StringBuilder pathBuilder = new StringBuilder();
                                    if (box.getParentRack() != null) {
                                        if (box.getParentRack().getParentShelf() != null) {
                                            if (box.getParentRack().getParentShelf().getParentDevice() != null) {
                                                pathBuilder.append(box.getParentRack().getParentShelf()
                                                        .getParentDevice().getName()).append(" > ");
                                            }
                                            pathBuilder.append(box.getParentRack().getParentShelf().getLabel())
                                                    .append(" > ");
                                        }
                                        pathBuilder.append(box.getParentRack().getLabel()).append(" > ");
                                    }
                                    pathBuilder.append(boxLabel);
                                    if (wellCoord != null) {
                                        pathBuilder.append(" - ").append(wellCoord);
                                    }
                                    data.put("storagePath", pathBuilder.toString());
                                } else {
                                    data.put("storageBox", "Box " + request.getBoxId());
                                    if (wellCoord != null) {
                                        data.put("storageLocation", "Box " + request.getBoxId() + " - " + wellCoord);
                                    } else {
                                        data.put("storageLocation", "Box " + request.getBoxId());
                                    }
                                }
                                LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                                        "Set storageLocation for sample " + sampleIdStr + ": "
                                                + data.get("storageLocation"));
                            } else if (request.getLocationId() != null) {
                                // Shelf-level storage (no box/wells)
                                String locationType = request.getLocationType() != null ? request.getLocationType()
                                        : "shelf";
                                data.put("locationId", request.getLocationId());
                                data.put("locationType", locationType);

                                // Build storage path from the storage service
                                try {
                                    org.openelisglobal.storage.service.SampleStorageService sampleStorageService = org.openelisglobal.spring.util.SpringContext
                                            .getBean(org.openelisglobal.storage.service.SampleStorageService.class);

                                    // Get the storage assignment to retrieve hierarchical path
                                    java.util.Map<String, Object> location = sampleStorageService
                                            .getSampleItemLocation(sampleIdStr);
                                    if (location != null && !location.isEmpty()) {
                                        String hierarchicalPath = (String) location.get("hierarchicalPath");
                                        if (hierarchicalPath != null && !hierarchicalPath.trim().isEmpty()) {
                                            data.put("storagePath", hierarchicalPath);
                                            data.put("storageLocation", hierarchicalPath);
                                        } else {
                                            data.put("storageLocation", "Shelf Storage");
                                        }
                                    } else {
                                        data.put("storageLocation", "Shelf Storage");
                                    }
                                    LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                                            "Set storageLocation for sample " + sampleIdStr + " (location-based): "
                                                    + data.get("storageLocation"));
                                } catch (Exception e) {
                                    LogEvent.logWarn(this.getClass().getName(), "assignSamplesToStorage",
                                            "Error retrieving storage path for sample " + sampleIdStr + ": "
                                                    + e.getMessage());
                                    data.put("storageLocation", "Shelf Storage");
                                }
                            }

                            nps.setData(data);
                            LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage",
                                    "NPS ID=" + nps.getId() + " data set with storageLocation="
                                            + data.get("storageLocation") + ", status=" + nps.getStatus());

                            notebookPageSampleService.update(nps);
                            LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage", "Updated NPS ID="
                                    + nps.getId() + " for pageId=" + targetPage.getId() + " sampleId=" + sampleIdStr);
                        }
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getName(), "assignSamplesToStorage",
                                "Error updating NotebookPageSample for sample " + sampleIdStr + ": " + e.getMessage());
                    }
                }
                // Note: We do NOT auto-mark samples as COMPLETED here.
                // Storage assignment sets samples to IN_PROGRESS (lines 1263-1266).
                // The user must explicitly click "Mark Complete" to mark samples COMPLETED,
                // which uses the /bulk/page/{pageId}/samples/status endpoint.
            }

            Map<String, Object> result = new HashMap<>();
            result.put("assignedCount", assignedCount);
            result.put("notebookId", notebookId);
            result.put("condition", condition.name());
            result.put("retentionYears", retentionYears);
            result.put("expiryDate", expiryDate.toString());
            result.put("success", true);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Convert SampleRouting to a displayable map.
     */
    private Map<String, Object> convertRoutingToMap(SampleRouting routing) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", routing.getId());
        map.put("sampleItemId", routing.getSampleItemId());
        map.put("destinationType", routing.getDestinationType() != null ? routing.getDestinationType().name() : null);
        map.put("wellCoordinate", routing.getWellCoordinate());
        map.put("externalLabName", routing.getExternalLabName());
        map.put("shipmentDate", routing.getShipmentDate() != null ? routing.getShipmentDate().toString() : null);
        map.put("routedAt", routing.getRoutedAt() != null ? routing.getRoutedAt().toString() : null);

        // Box is eagerly fetched via LEFT JOIN FETCH in DAO queries
        try {
            if (routing.getBox() != null) {
                map.put("boxId", routing.getBox().getId());
                map.put("boxName", routing.getBox().getLabel());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Box not loaded - skip box info
            LogEvent.logWarn(this.getClass().getName(), "convertRoutingToMap",
                    "Box not loaded for routing " + routing.getId());
        }

        return map;
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }

    /**
     * Find the Storage page for a notebook. The page is identified by title
     * containing "storage" or "inventory", or by common page orders: - Page 5 for
     * Pharma workflow (Storage & Inventory Management) - Page 7 for MNTD workflow
     * (Post-Analysis Storage)
     *
     * @param notebook the notebook
     * @return the storage page, or null if not found
     */
    private NoteBookPage findStoragePageForNotebook(NoteBook notebook) {
        if (notebook == null) {
            return null;
        }

        try {
            // Use service method to fetch pages directly (avoids
            // LazyInitializationException)
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());
            if (pages == null || pages.isEmpty()) {
                return null;
            }

            // First try to find by title containing "storage" or "inventory"
            for (NoteBookPage page : pages) {
                String title = page.getTitle();
                if (title != null) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("storage") || lowerTitle.contains("inventory")) {
                        return page;
                    }
                }
            }

            // Fallback: find page with order = 5 (Pharma Storage) or 7 (MNTD Storage)
            for (NoteBookPage page : pages) {
                if (page.getOrder() != null && (page.getOrder() == 5 || page.getOrder() == 7)) {
                    return page;
                }
            }

            return null;
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "findStoragePageForNotebook",
                    "Error finding storage page: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get report history for a specific notebook page. GET
     * /notebook/page/{pageId}/reports
     *
     * Returns an empty list for now - report history storage not yet implemented.
     *
     * @param pageId the notebook page ID
     * @return list of report history entries (currently empty)
     */
    @GetMapping(value = "/page/{pageId}/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPageReportHistory(@PathVariable("pageId") Integer pageId) {
        // TODO: Implement report history storage
        // For now, return empty list to prevent frontend errors
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    /**
     * Get storage logbook entries for a page. GET
     * /notebook/page/{pageId}/storage-logbook
     *
     * Returns storage assignment history for samples on this page.
     *
     * @param pageId the notebook page ID
     * @return list of storage logbook entries
     */
    @GetMapping(value = "/page/{pageId}/storage-logbook", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getStorageLogbook(@PathVariable("pageId") Integer pageId) {
        List<Map<String, Object>> logbook = new java.util.ArrayList<>();

        try {
            // Get all samples for this page and extract storage history from their data
            List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(pageId);

            for (NotebookPageSample sample : pageSamples) {
                if (sample.getData() != null) {
                    Map<String, Object> data = sample.getData();

                    // Check if sample has storage assignment
                    if (data.containsKey("storageLocation") || data.containsKey("boxId")
                            || data.containsKey("wellCoordinate") || data.containsKey("storagePath")) {

                        Map<String, Object> entry = new HashMap<>();

                        // Look up SampleItem via service using the stored ID
                        String sampleItemId = sample.getSampleItemId();
                        SampleItem sampleItem = null;
                        if (sampleItemId != null && !sampleItemId.isEmpty()) {
                            sampleItem = sampleItemService.getData(sampleItemId);
                        }

                        entry.put("sampleId", sampleItem != null ? sampleItem.getId() : null);

                        // Get accession number
                        if (sampleItem != null && sampleItem.getSample() != null) {
                            entry.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
                        }

                        entry.put("storageLocation", data.get("storageLocation"));
                        entry.put("storagePath", data.get("storagePath"));
                        entry.put("storageCondition", data.get("storageCondition"));
                        entry.put("boxId", data.get("boxId"));
                        entry.put("wellCoordinate", data.get("wellCoordinate"));
                        entry.put("dateStored", data.get("dateStored"));
                        entry.put("retentionExpiry", data.get("retentionExpiry"));
                        entry.put("dateRetrieved", data.get("dateRetrieved"));
                        entry.put("retrievedBy", data.get("retrievedBy"));
                        entry.put("status", sample.getStatus() != null ? sample.getStatus().name() : "PENDING");

                        logbook.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list on error to prevent frontend errors
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }

        return ResponseEntity.ok(logbook);
    }

    // ================== IMMUNOLOGY POST-ANALYSIS ENDPOINTS ==================

    /**
     * Update volume and status for samples. POST
     * /notebook/{id}/samples/update-volume
     *
     * Used in post-analysis handling to track remaining volume and sample status
     * (analyzed, partially used, exhausted).
     *
     * @param notebookId  the notebook ID
     * @param request     volume update request
     * @param httpRequest for getting user session
     * @return update result
     */
    @PostMapping(value = "/{notebookId}/samples/update-volume", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSampleVolume(@PathVariable("notebookId") Integer notebookId,
            @RequestBody UpdateVolumeRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            int updatedCount = 0;

            for (Integer sampleId : request.getSampleIds()) {
                // Find the NotebookPageSample for this sample (look in Post-Analysis page)
                NoteBookPage postAnalysisPage = findPostAnalysisPageForNotebook(notebook);
                if (postAnalysisPage == null) {
                    // Try Storage page as fallback
                    postAnalysisPage = findStoragePageForNotebook(notebook);
                }

                if (postAnalysisPage != null) {
                    org.openelisglobal.notebook.valueholder.NotebookPageSample nps = notebookPageSampleService
                            .getByPageIdAndSampleItemId(postAnalysisPage.getId(), sampleId);

                    if (nps != null) {
                        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                : new HashMap<>();

                        // Update volume and status
                        if (request.getSampleStatus() != null) {
                            data.put("sampleStatus", request.getSampleStatus());
                        }
                        if (request.getRemainingVolume() != null) {
                            data.put("remainingVolume", request.getRemainingVolume());
                        }
                        if (request.getVolumeUnit() != null) {
                            data.put("volumeUnit", request.getVolumeUnit());
                        }
                        if (request.getVolumeNotes() != null) {
                            data.put("volumeNotes", request.getVolumeNotes());
                        }

                        nps.setData(data);
                        nps.setSysUserId(sysUserId);
                        notebookPageSampleService.update(nps);
                        updatedCount++;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updatedCount", updatedCount);
            result.put("notebookId", notebookId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "updateSampleVolume",
                    "Error updating sample volume: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update sample volume: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Add quality flag to samples. POST /notebook/{id}/samples/quality-flag
     *
     * Used in post-analysis handling and result compilation to flag samples with
     * quality issues.
     *
     * @param notebookId  the notebook ID
     * @param request     quality flag request
     * @param httpRequest for getting user session
     * @return flag result
     */
    @PostMapping(value = "/{notebookId}/samples/quality-flag", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addQualityFlag(@PathVariable("notebookId") Integer notebookId,
            @RequestBody QualityFlagRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getFlagType() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Flag type is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            int updatedCount = 0;

            for (Integer sampleId : request.getSampleIds()) {
                // Find the NotebookPageSample for this sample
                NoteBookPage targetPage = findPostAnalysisPageForNotebook(notebook);
                if (targetPage == null) {
                    targetPage = findStoragePageForNotebook(notebook);
                }
                if (targetPage == null) {
                    targetPage = findResultCompilationPageForNotebook(notebook);
                }

                if (targetPage != null) {
                    org.openelisglobal.notebook.valueholder.NotebookPageSample nps = notebookPageSampleService
                            .getByPageIdAndSampleItemId(targetPage.getId(), sampleId);

                    if (nps != null) {
                        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                : new HashMap<>();

                        // Get or create quality flags list
                        @SuppressWarnings("unchecked")
                        List<String> qualityFlags = (List<String>) data.get("qualityFlags");
                        if (qualityFlags == null) {
                            qualityFlags = new java.util.ArrayList<>();
                        }

                        // Add the new flag if not already present
                        if (!qualityFlags.contains(request.getFlagType())) {
                            qualityFlags.add(request.getFlagType());
                        }
                        data.put("qualityFlags", qualityFlags);

                        // Store flag metadata
                        if (request.getCategory() != null) {
                            data.put("flagCategory", request.getCategory());
                        }
                        if (request.getReason() != null) {
                            data.put("flagReason", request.getReason());
                        }
                        if (request.getNotes() != null) {
                            data.put("qualityNotes", request.getNotes());
                        }
                        if (request.isRequiresRepeatTesting()) {
                            data.put("requiresRepeatTesting", true);
                        }
                        if (request.isRequiresInvestigation()) {
                            data.put("requiresInvestigation", true);
                        }

                        nps.setData(data);
                        nps.setSysUserId(sysUserId);
                        notebookPageSampleService.update(nps);
                        updatedCount++;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updatedCount", updatedCount);
            result.put("notebookId", notebookId);
            result.put("flagType", request.getFlagType());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "addQualityFlag",
                    "Error adding quality flag: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to add quality flag: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get QC summary for a notebook. GET /notebook/{id}/qc-summary
     *
     * Returns summary of QC controls (passed/failed) for immunology workflows.
     *
     * @param notebookId the notebook ID
     * @return QC summary
     */
    @GetMapping(value = "/{notebookId}/qc-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQCSummary(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook;
        try {
            notebook = noteBookService.get(notebookId);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Count QC controls from assay runs stored in page data
            int controlsPassed = 0;
            int controlsFailed = 0;
            int totalControls = 0;

            // Look for assay runs in the Analysis page (typically page 6)
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebookId);
            for (NoteBookPage page : pages) {
                Map<String, Object> pageData = page.getData();
                if (pageData != null && pageData.containsKey("assayRuns")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> assayRuns = (List<Map<String, Object>>) pageData.get("assayRuns");
                    if (assayRuns != null) {
                        for (Map<String, Object> run : assayRuns) {
                            // Count controls from each run
                            Object passedObj = run.get("controlsPassed");
                            Object failedObj = run.get("controlsFailed");
                            Object totalObj = run.get("totalControls");

                            if (passedObj instanceof Number) {
                                controlsPassed += ((Number) passedObj).intValue();
                            }
                            if (failedObj instanceof Number) {
                                controlsFailed += ((Number) failedObj).intValue();
                            }
                            if (totalObj instanceof Number) {
                                totalControls += ((Number) totalObj).intValue();
                            }
                        }
                    }
                }

                // Also check for QC data in page's qcSummary field
                if (pageData != null && pageData.containsKey("qcSummary")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> qcSummary = (Map<String, Object>) pageData.get("qcSummary");
                    if (qcSummary != null) {
                        Object passedObj = qcSummary.get("controlsPassed");
                        Object failedObj = qcSummary.get("controlsFailed");
                        Object totalObj = qcSummary.get("totalControls");

                        if (passedObj instanceof Number) {
                            controlsPassed += ((Number) passedObj).intValue();
                        }
                        if (failedObj instanceof Number) {
                            controlsFailed += ((Number) failedObj).intValue();
                        }
                        if (totalObj instanceof Number) {
                            totalControls += ((Number) totalObj).intValue();
                        }
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("controlsPassed", controlsPassed);
            result.put("controlsFailed", controlsFailed);
            result.put("totalControls", totalControls);
            result.put("notebookId", notebookId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getQCSummary", "Error getting QC summary: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get QC summary: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Find the Post-Analysis page for a notebook (typically page 7 for immunology).
     */
    private NoteBookPage findPostAnalysisPageForNotebook(NoteBook notebook) {
        if (notebook == null) {
            return null;
        }

        try {
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());
            if (pages == null || pages.isEmpty()) {
                return null;
            }

            // First try to find by title containing "post-analysis" or "post analysis"
            for (NoteBookPage page : pages) {
                String title = page.getTitle();
                if (title != null) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("post-analysis") || lowerTitle.contains("post analysis")) {
                        return page;
                    }
                }
            }

            // Fallback: find page with order = 7 (Immunology Post-Analysis)
            for (NoteBookPage page : pages) {
                if (page.getOrder() != null && page.getOrder() == 7) {
                    return page;
                }
            }

            return null;
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "findPostAnalysisPageForNotebook",
                    "Error finding post-analysis page: " + e.getMessage());
            return null;
        }
    }

    /**
     * Find the Result Compilation page for a notebook (typically page 8 for
     * immunology).
     */
    private NoteBookPage findResultCompilationPageForNotebook(NoteBook notebook) {
        if (notebook == null) {
            return null;
        }

        try {
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());
            if (pages == null || pages.isEmpty()) {
                return null;
            }

            // First try to find by title containing "result" or "compilation"
            for (NoteBookPage page : pages) {
                String title = page.getTitle();
                if (title != null) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("result") || lowerTitle.contains("compilation")) {
                        return page;
                    }
                }
            }

            // Fallback: find page with order = 8 (Immunology Result Compilation)
            for (NoteBookPage page : pages) {
                if (page.getOrder() != null && page.getOrder() == 8) {
                    return page;
                }
            }

            return null;
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "findResultCompilationPageForNotebook",
                    "Error finding result compilation page: " + e.getMessage());
            return null;
        }
    }

    /**
     * Request body for linking samples.
     */
    public static class LinkSamplesRequest {
        private List<Integer> sampleItemIds;

        public List<Integer> getSampleItemIds() {
            return sampleItemIds;
        }

        public void setSampleItemIds(List<Integer> sampleItemIds) {
            this.sampleItemIds = sampleItemIds;
        }
    }

    /**
     * Request body for creating an instance from template.
     */
    public static class CreateInstanceRequest {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * Request body for creating child samples.
     */
    public static class CreateChildSamplesRequest {
        private List<Integer> parentSampleIds;
        private int childCountPerParent;
        private String externalIdPrefix;
        private Integer pageId; // Optional: page ID where child samples should appear
        private AliquotData aliquotData; // Optional: aliquot-specific data
        private String childSampleType; // Optional: type of child sample (slide, aliquot, cell_block, etc.)
        private String processingType; // "histopathology" or "cytopathology"
        private PathologyProcessingData processingData; // Pathology-specific processing data

        public List<Integer> getParentSampleIds() {
            return parentSampleIds;
        }

        public void setParentSampleIds(List<Integer> parentSampleIds) {
            this.parentSampleIds = parentSampleIds;
        }

        public int getChildCountPerParent() {
            return childCountPerParent;
        }

        public void setChildCountPerParent(int childCountPerParent) {
            this.childCountPerParent = childCountPerParent;
        }

        public String getExternalIdPrefix() {
            return externalIdPrefix;
        }

        public void setExternalIdPrefix(String externalIdPrefix) {
            this.externalIdPrefix = externalIdPrefix;
        }

        public Integer getPageId() {
            return pageId;
        }

        public void setPageId(Integer pageId) {
            this.pageId = pageId;
        }

        public AliquotData getAliquotData() {
            return aliquotData;
        }

        public void setAliquotData(AliquotData aliquotData) {
            this.aliquotData = aliquotData;
        }

        public String getChildSampleType() {
            return childSampleType;
        }

        public void setChildSampleType(String childSampleType) {
            this.childSampleType = childSampleType;
        }

        public String getProcessingType() {
            return processingType;
        }

        public void setProcessingType(String processingType) {
            this.processingType = processingType;
        }

        public PathologyProcessingData getProcessingData() {
            return processingData;
        }

        public void setProcessingData(PathologyProcessingData processingData) {
            this.processingData = processingData;
        }
    }

    /**
     * Pathology-specific processing data for grossing/aliquoting.
     */
    public static class PathologyProcessingData {
        private String processingType; // "histopathology" or "cytopathology"

        // Histopathology - Gross Description
        private String grossExamBy;
        private String grossExamDate;
        private String specimenSize;
        private String specimenColor;
        private String specimenConsistency;
        private String lesionsIdentified;
        private String grossDescriptionNarrative;

        // Sectioning
        private boolean sectioningDone;
        private int numberOfSections;
        private String sectionOrientation;
        private boolean representativeSections;

        // Embedding
        private boolean embeddingDone;
        private String embeddingMedium;

        // Microtomy
        private boolean microtomyDone;
        private int microtomyThickness;

        // Cytopathology - Aliquot Purposes
        private boolean aliquotForLBC;
        private boolean aliquotForCellBlock;
        private boolean aliquotForMolecularTesting;
        private boolean aliquotForBiobanking;

        // Aliquot Details
        private String lbcVolume;
        private String cellBlockVolume;
        private String molecularTestingVolume;
        private String biobankingVolume;
        private String preservativeUsed;

        // Common Fields
        private String processingDate;
        private String staffInitials;
        private String processingNotes;

        // Getters and setters
        public String getProcessingType() {
            return processingType;
        }

        public void setProcessingType(String processingType) {
            this.processingType = processingType;
        }

        public String getGrossExamBy() {
            return grossExamBy;
        }

        public void setGrossExamBy(String grossExamBy) {
            this.grossExamBy = grossExamBy;
        }

        public String getGrossExamDate() {
            return grossExamDate;
        }

        public void setGrossExamDate(String grossExamDate) {
            this.grossExamDate = grossExamDate;
        }

        public String getSpecimenSize() {
            return specimenSize;
        }

        public void setSpecimenSize(String specimenSize) {
            this.specimenSize = specimenSize;
        }

        public String getSpecimenColor() {
            return specimenColor;
        }

        public void setSpecimenColor(String specimenColor) {
            this.specimenColor = specimenColor;
        }

        public String getSpecimenConsistency() {
            return specimenConsistency;
        }

        public void setSpecimenConsistency(String specimenConsistency) {
            this.specimenConsistency = specimenConsistency;
        }

        public String getLesionsIdentified() {
            return lesionsIdentified;
        }

        public void setLesionsIdentified(String lesionsIdentified) {
            this.lesionsIdentified = lesionsIdentified;
        }

        public String getGrossDescriptionNarrative() {
            return grossDescriptionNarrative;
        }

        public void setGrossDescriptionNarrative(String grossDescriptionNarrative) {
            this.grossDescriptionNarrative = grossDescriptionNarrative;
        }

        public boolean isSectioningDone() {
            return sectioningDone;
        }

        public void setSectioningDone(boolean sectioningDone) {
            this.sectioningDone = sectioningDone;
        }

        public int getNumberOfSections() {
            return numberOfSections;
        }

        public void setNumberOfSections(int numberOfSections) {
            this.numberOfSections = numberOfSections;
        }

        public String getSectionOrientation() {
            return sectionOrientation;
        }

        public void setSectionOrientation(String sectionOrientation) {
            this.sectionOrientation = sectionOrientation;
        }

        public boolean isRepresentativeSections() {
            return representativeSections;
        }

        public void setRepresentativeSections(boolean representativeSections) {
            this.representativeSections = representativeSections;
        }

        public boolean isEmbeddingDone() {
            return embeddingDone;
        }

        public void setEmbeddingDone(boolean embeddingDone) {
            this.embeddingDone = embeddingDone;
        }

        public String getEmbeddingMedium() {
            return embeddingMedium;
        }

        public void setEmbeddingMedium(String embeddingMedium) {
            this.embeddingMedium = embeddingMedium;
        }

        public boolean isMicrotomyDone() {
            return microtomyDone;
        }

        public void setMicrotomyDone(boolean microtomyDone) {
            this.microtomyDone = microtomyDone;
        }

        public int getMicrotomyThickness() {
            return microtomyThickness;
        }

        public void setMicrotomyThickness(int microtomyThickness) {
            this.microtomyThickness = microtomyThickness;
        }

        public boolean isAliquotForLBC() {
            return aliquotForLBC;
        }

        public void setAliquotForLBC(boolean aliquotForLBC) {
            this.aliquotForLBC = aliquotForLBC;
        }

        public boolean isAliquotForCellBlock() {
            return aliquotForCellBlock;
        }

        public void setAliquotForCellBlock(boolean aliquotForCellBlock) {
            this.aliquotForCellBlock = aliquotForCellBlock;
        }

        public boolean isAliquotForMolecularTesting() {
            return aliquotForMolecularTesting;
        }

        public void setAliquotForMolecularTesting(boolean aliquotForMolecularTesting) {
            this.aliquotForMolecularTesting = aliquotForMolecularTesting;
        }

        public boolean isAliquotForBiobanking() {
            return aliquotForBiobanking;
        }

        public void setAliquotForBiobanking(boolean aliquotForBiobanking) {
            this.aliquotForBiobanking = aliquotForBiobanking;
        }

        public String getLbcVolume() {
            return lbcVolume;
        }

        public void setLbcVolume(String lbcVolume) {
            this.lbcVolume = lbcVolume;
        }

        public String getCellBlockVolume() {
            return cellBlockVolume;
        }

        public void setCellBlockVolume(String cellBlockVolume) {
            this.cellBlockVolume = cellBlockVolume;
        }

        public String getMolecularTestingVolume() {
            return molecularTestingVolume;
        }

        public void setMolecularTestingVolume(String molecularTestingVolume) {
            this.molecularTestingVolume = molecularTestingVolume;
        }

        public String getBiobankingVolume() {
            return biobankingVolume;
        }

        public void setBiobankingVolume(String biobankingVolume) {
            this.biobankingVolume = biobankingVolume;
        }

        public String getPreservativeUsed() {
            return preservativeUsed;
        }

        public void setPreservativeUsed(String preservativeUsed) {
            this.preservativeUsed = preservativeUsed;
        }

        public String getProcessingDate() {
            return processingDate;
        }

        public void setProcessingDate(String processingDate) {
            this.processingDate = processingDate;
        }

        public String getStaffInitials() {
            return staffInitials;
        }

        public void setStaffInitials(String staffInitials) {
            this.staffInitials = staffInitials;
        }

        public String getProcessingNotes() {
            return processingNotes;
        }

        public void setProcessingNotes(String processingNotes) {
            this.processingNotes = processingNotes;
        }
    }

    /**
     * Aliquot-specific data for child sample creation.
     */
    public static class AliquotData {
        private String aliquotType; // TUBE, PLATE_96, CRYOBOX_9x9, CRYOBOX_10x10
        private Double volume;
        private Double initialVolume; // Alias for volume from frontend
        private String volumeUnit; // uL, mL
        private Integer dbsSpots; // For DBS samples
        private String notes;

        public String getAliquotType() {
            return aliquotType;
        }

        public void setAliquotType(String aliquotType) {
            this.aliquotType = aliquotType;
        }

        public Double getVolume() {
            return volume;
        }

        public void setVolume(Double volume) {
            this.volume = volume;
        }

        public Double getInitialVolume() {
            return initialVolume;
        }

        public void setInitialVolume(Double initialVolume) {
            this.initialVolume = initialVolume;
        }

        public String getVolumeUnit() {
            return volumeUnit;
        }

        public void setVolumeUnit(String volumeUnit) {
            this.volumeUnit = volumeUnit;
        }

        public Integer getDbsSpots() {
            return dbsSpots;
        }

        public void setDbsSpots(Integer dbsSpots) {
            this.dbsSpots = dbsSpots;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Request body for storage assignment.
     */
    public static class AssignStorageRequest {
        private List<Integer> sampleIds;
        private List<String> sampleIdsString; // For composite sample IDs (e.g., "4_cassette_0_block_0")
        private Integer boxId;
        private Map<String, String> wellAssignments;
        private String locationId;
        private String locationType;
        private String positionCoordinate;
        private String condition;
        private int retentionYears;
        private boolean reassign; // Flag to allow reassignment of already-assigned samples
        private Integer pageId; // Notebook page ID for routing context
        private boolean postAnalysisStorage; // Flag for post-analysis storage (immunology workflow)

        // Additional storage fields
        private String storagePurpose;
        private String retrievalDate;
        private String storageNotes;
        private String dateStored;

        // Bacteriology post-analysis storage fields
        private String aliquotType;
        private String storageMedia;
        private String containerType;
        private String storageMethod;
        private String storageCondition;
        private String organismIdentified;
        private String storageBox;
        private String storageWell;
        private Double remainingVolume;
        private String volumeUnit;
        private String sampleStatus;
        private List<String> qualityFlags;
        private String qualityNotes;
        private Boolean requiresInvestigation;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public List<String> getSampleIdsString() {
            return sampleIdsString;
        }

        public void setSampleIdsString(List<String> sampleIdsString) {
            this.sampleIdsString = sampleIdsString;
        }

        /**
         * Get effective sample IDs as strings. Prefers sampleIdsString for composite
         * IDs, falls back to converting sampleIds to strings.
         */
        public List<String> getEffectiveSampleIds() {
            if (sampleIdsString != null && !sampleIdsString.isEmpty()) {
                return sampleIdsString;
            }
            if (sampleIds != null) {
                return sampleIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList());
            }
            return new java.util.ArrayList<>();
        }

        public Integer getBoxId() {
            return boxId;
        }

        public void setBoxId(Integer boxId) {
            this.boxId = boxId;
        }

        public Map<String, String> getWellAssignments() {
            return wellAssignments;
        }

        public void setWellAssignments(Map<String, String> wellAssignments) {
            this.wellAssignments = wellAssignments;
        }

        /**
         * Convert string-keyed well assignments to integer-keyed for service layer.
         */
        public Map<Integer, String> getWellAssignmentsAsIntegerMap() {
            if (wellAssignments == null) {
                return null;
            }
            Map<Integer, String> result = new HashMap<>();
            for (Map.Entry<String, String> entry : wellAssignments.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
            return result;
        }

        public String getLocationId() {
            return locationId;
        }

        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public String getLocationType() {
            return locationType;
        }

        public void setLocationType(String locationType) {
            this.locationType = locationType;
        }

        public String getPositionCoordinate() {
            return positionCoordinate;
        }

        public void setPositionCoordinate(String positionCoordinate) {
            this.positionCoordinate = positionCoordinate;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public int getRetentionYears() {
            return retentionYears;
        }

        public void setRetentionYears(int retentionYears) {
            this.retentionYears = retentionYears;
        }

        public boolean isReassign() {
            return reassign;
        }

        public void setReassign(boolean reassign) {
            this.reassign = reassign;
        }

        public Integer getPageId() {
            return pageId;
        }

        public void setPageId(Integer pageId) {
            this.pageId = pageId;
        }

        public boolean isPostAnalysisStorage() {
            return postAnalysisStorage;
        }

        public void setPostAnalysisStorage(boolean postAnalysisStorage) {
            this.postAnalysisStorage = postAnalysisStorage;
        }

        // Additional storage field getters/setters
        public String getStoragePurpose() {
            return storagePurpose;
        }

        public void setStoragePurpose(String storagePurpose) {
            this.storagePurpose = storagePurpose;
        }

        public String getRetrievalDate() {
            return retrievalDate;
        }

        public void setRetrievalDate(String retrievalDate) {
            this.retrievalDate = retrievalDate;
        }

        public String getStorageNotes() {
            return storageNotes;
        }

        public void setStorageNotes(String storageNotes) {
            this.storageNotes = storageNotes;
        }

        // Bacteriology post-analysis storage getters/setters
        public String getAliquotType() {
            return aliquotType;
        }

        public void setAliquotType(String aliquotType) {
            this.aliquotType = aliquotType;
        }

        public String getStorageMedia() {
            return storageMedia;
        }

        public void setStorageMedia(String storageMedia) {
            this.storageMedia = storageMedia;
        }

        public String getContainerType() {
            return containerType;
        }

        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }

        public String getStorageMethod() {
            return storageMethod;
        }

        public void setStorageMethod(String storageMethod) {
            this.storageMethod = storageMethod;
        }

        public String getStorageCondition() {
            return storageCondition;
        }

        public void setStorageCondition(String storageCondition) {
            this.storageCondition = storageCondition;
        }

        public String getOrganismIdentified() {
            return organismIdentified;
        }

        public void setOrganismIdentified(String organismIdentified) {
            this.organismIdentified = organismIdentified;
        }

        public String getStorageBox() {
            return storageBox;
        }

        public void setStorageBox(String storageBox) {
            this.storageBox = storageBox;
        }

        public String getStorageWell() {
            return storageWell;
        }

        public void setStorageWell(String storageWell) {
            this.storageWell = storageWell;
        }

        public Double getRemainingVolume() {
            return remainingVolume;
        }

        public void setRemainingVolume(Double remainingVolume) {
            this.remainingVolume = remainingVolume;
        }

        public String getVolumeUnit() {
            return volumeUnit;
        }

        public void setVolumeUnit(String volumeUnit) {
            this.volumeUnit = volumeUnit;
        }

        public String getSampleStatus() {
            return sampleStatus;
        }

        public void setSampleStatus(String sampleStatus) {
            this.sampleStatus = sampleStatus;
        }

        public List<String> getQualityFlags() {
            return qualityFlags;
        }

        public void setQualityFlags(List<String> qualityFlags) {
            this.qualityFlags = qualityFlags;
        }

        public String getQualityNotes() {
            return qualityNotes;
        }

        public void setQualityNotes(String qualityNotes) {
            this.qualityNotes = qualityNotes;
        }

        public Boolean getRequiresInvestigation() {
            return requiresInvestigation;
        }

        public void setRequiresInvestigation(Boolean requiresInvestigation) {
            this.requiresInvestigation = requiresInvestigation;
        }

        public String getDateStored() {
            return dateStored;
        }

        public void setDateStored(String dateStored) {
            this.dateStored = dateStored;
        }
    }

    /**
     * Assay plate info for Internal Analysis routing. Assay plates are temporary
     * plates for running assays - NOT connected to the hierarchical storage system.
     */
    public static class AssayPlateInfo {
        private String id;
        private String name;
        private int rows;
        private int columns;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getColumns() {
            return columns;
        }

        public void setColumns(int columns) {
            this.columns = columns;
        }
    }

    /**
     * Request body for routing samples.
     */
    public static class RouteSamplesRequest {
        private List<Integer> sampleIds;
        private String destinationType;
        private Integer boxId;
        private Map<Integer, String> wellAssignments;
        private String externalLabName;
        private String shipmentDate;
        private Integer storageAssignmentId;
        private AssayPlateInfo assayPlate; // For Internal Analysis - temporary assay plates

        // Additional external lab fields
        private String externalLabContact;
        private String chainOfCustodyNotes;
        private String packagingRequirements;
        private String shipmentStatus;
        private String trackingNumber;

        // Additional storage fields
        private String storagePurpose;
        private String storageTemperature;
        private String retrievalDate;
        private String storageNotes;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getDestinationType() {
            return destinationType;
        }

        public void setDestinationType(String destinationType) {
            this.destinationType = destinationType;
        }

        public Integer getBoxId() {
            return boxId;
        }

        public void setBoxId(Integer boxId) {
            this.boxId = boxId;
        }

        public Map<Integer, String> getWellAssignments() {
            return wellAssignments;
        }

        public void setWellAssignments(Map<Integer, String> wellAssignments) {
            this.wellAssignments = wellAssignments;
        }

        public String getExternalLabName() {
            return externalLabName;
        }

        public void setExternalLabName(String externalLabName) {
            this.externalLabName = externalLabName;
        }

        public String getShipmentDate() {
            return shipmentDate;
        }

        public void setShipmentDate(String shipmentDate) {
            this.shipmentDate = shipmentDate;
        }

        public Integer getStorageAssignmentId() {
            return storageAssignmentId;
        }

        public void setStorageAssignmentId(Integer storageAssignmentId) {
            this.storageAssignmentId = storageAssignmentId;
        }

        public AssayPlateInfo getAssayPlate() {
            return assayPlate;
        }

        public void setAssayPlate(AssayPlateInfo assayPlate) {
            this.assayPlate = assayPlate;
        }

        private Integer pageId;

        public Integer getPageId() {
            return pageId;
        }

        public void setPageId(Integer pageId) {
            this.pageId = pageId;
        }

        // External lab additional getters/setters
        public String getExternalLabContact() {
            return externalLabContact;
        }

        public void setExternalLabContact(String externalLabContact) {
            this.externalLabContact = externalLabContact;
        }

        public String getChainOfCustodyNotes() {
            return chainOfCustodyNotes;
        }

        public void setChainOfCustodyNotes(String chainOfCustodyNotes) {
            this.chainOfCustodyNotes = chainOfCustodyNotes;
        }

        public String getPackagingRequirements() {
            return packagingRequirements;
        }

        public void setPackagingRequirements(String packagingRequirements) {
            this.packagingRequirements = packagingRequirements;
        }

        public String getShipmentStatus() {
            return shipmentStatus;
        }

        public void setShipmentStatus(String shipmentStatus) {
            this.shipmentStatus = shipmentStatus;
        }

        public String getTrackingNumber() {
            return trackingNumber;
        }

        public void setTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
        }

        // Storage additional getters/setters
        public String getStoragePurpose() {
            return storagePurpose;
        }

        public void setStoragePurpose(String storagePurpose) {
            this.storagePurpose = storagePurpose;
        }

        public String getStorageTemperature() {
            return storageTemperature;
        }

        public void setStorageTemperature(String storageTemperature) {
            this.storageTemperature = storageTemperature;
        }

        public String getRetrievalDate() {
            return retrievalDate;
        }

        public void setRetrievalDate(String retrievalDate) {
            this.retrievalDate = retrievalDate;
        }

        public String getStorageNotes() {
            return storageNotes;
        }

        public void setStorageNotes(String storageNotes) {
            this.storageNotes = storageNotes;
        }
    }

    /**
     * Request body for updating sample volume.
     */
    public static class UpdateVolumeRequest {
        private List<Integer> sampleIds;
        private String sampleStatus; // ANALYZED, PARTIALLY_USED, EXHAUSTED
        private Double remainingVolume;
        private String volumeUnit; // µL, mL
        private String volumeNotes;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getSampleStatus() {
            return sampleStatus;
        }

        public void setSampleStatus(String sampleStatus) {
            this.sampleStatus = sampleStatus;
        }

        public Double getRemainingVolume() {
            return remainingVolume;
        }

        public void setRemainingVolume(Double remainingVolume) {
            this.remainingVolume = remainingVolume;
        }

        public String getVolumeUnit() {
            return volumeUnit;
        }

        public void setVolumeUnit(String volumeUnit) {
            this.volumeUnit = volumeUnit;
        }

        public String getVolumeNotes() {
            return volumeNotes;
        }

        public void setVolumeNotes(String volumeNotes) {
            this.volumeNotes = volumeNotes;
        }
    }

    /**
     * Request body for adding quality flags.
     */
    public static class QualityFlagRequest {
        private List<Integer> sampleIds;
        private String flagType; // INSUFFICIENT_VOLUME, QUALITY_ISSUE, UNEXPECTED_RESULTS, etc.
        private String category; // SAMPLE, INSTRUMENT, RESULT
        private String reason;
        private String notes; // Additional notes about the flag
        private boolean requiresRepeatTesting;
        private boolean requiresInvestigation;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getFlagType() {
            return flagType;
        }

        public void setFlagType(String flagType) {
            this.flagType = flagType;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public boolean isRequiresRepeatTesting() {
            return requiresRepeatTesting;
        }

        public void setRequiresRepeatTesting(boolean requiresRepeatTesting) {
            this.requiresRepeatTesting = requiresRepeatTesting;
        }

        public boolean isRequiresInvestigation() {
            return requiresInvestigation;
        }

        public void setRequiresInvestigation(boolean requiresInvestigation) {
            this.requiresInvestigation = requiresInvestigation;
        }
    }

    /**
     * Request body for deleting samples from a page.
     */
    public static class DeleteSamplesRequest {
        private List<Integer> sampleIds;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }
    }
}
