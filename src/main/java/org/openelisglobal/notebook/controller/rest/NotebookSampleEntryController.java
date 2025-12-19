package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.service.NotebookSampleEntryService;
import org.openelisglobal.notebook.service.SampleRoutingService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        NoteBook notebook = noteBookService.get(notebookId);
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
        NoteBook notebook = noteBookService.get(notebookId);
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

        NoteBook notebook = noteBookService.get(notebookId);
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

        NoteBook notebook = noteBookService.get(notebookId);
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

        // First pass: build sample maps from page samples
        for (org.openelisglobal.notebook.valueholder.NotebookPageSample nps : pageSamples) {
            org.openelisglobal.sampleitem.valueholder.SampleItem sampleItem = sampleItemService
                    .get(nps.getSampleItemId());
            if (sampleItem != null) {
                Map<String, Object> sampleMap = buildSampleMap(sampleItem, nps);
                sampleMaps.add(sampleMap);
                includedSampleIds.add(sampleItem.getId());
            }
        }

        // Second pass: for each parent sample on the page, also include its children
        // This ensures aliquots are displayed even if not explicitly linked to page
        List<Map<String, Object>> childMaps = new java.util.ArrayList<>();
        for (Map<String, Object> sampleMap : sampleMaps) {
            Boolean isAliquot = (Boolean) sampleMap.get("isAliquot");
            if (isAliquot == null || !isAliquot) {
                // This is a parent sample - get its children
                String sampleId = String.valueOf(sampleMap.get("id"));
                SampleItem parentSample = sampleItemService.get(sampleId);
                if (parentSample != null) {
                    try {
                        List<SampleItem> children = sampleEntryService.getChildSamples(Integer.parseInt(sampleId));
                        for (SampleItem child : children) {
                            if (!includedSampleIds.contains(child.getId())) {
                                // Look up the child's actual NotebookPageSample record for this page
                                // to get the correct status (may be COMPLETED if inherited from parent)
                                org.openelisglobal.notebook.valueholder.NotebookPageSample childNps = notebookPageSampleService
                                        .getByPageIdAndSampleItemId(pageId, Integer.parseInt(child.getId()));
                                Map<String, Object> childMap = buildSampleMap(child, childNps);
                                childMaps.add(childMap);
                                includedSampleIds.add(child.getId());
                            }
                        }
                    } catch (Exception e) {
                        // Skip child samples if query fails (type mismatch in legacy data)
                        LogEvent.logWarn(this.getClass().getName(), "getPageSamples",
                                "Could not load child samples for " + sampleId + ": " + e.getMessage());
                    }
                }
            }
        }
        sampleMaps.addAll(childMaps);

        // Third pass: count children for each parent
        Map<String, Integer> childCountMap = new HashMap<>();
        for (Map<String, Object> sampleMap : sampleMaps) {
            String parentId = (String) sampleMap.get("parentSampleItemId");
            if (parentId != null) {
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

        if (nps != null) {
            sampleMap.put("pageStatus", nps.getStatus() != null ? nps.getStatus().name() : "PENDING");
            sampleMap.put("pageSampleId", nps.getId());
            sampleMap.put("data", nps.getData());
        } else {
            sampleMap.put("pageStatus", "PENDING");
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

        // Hierarchy information
        SampleItem parentSample = sampleItem.getParentSampleItem();
        if (parentSample != null) {
            sampleMap.put("isAliquot", true);
            sampleMap.put("nestingLevel", 1);
            sampleMap.put("parentSampleItemId", parentSample.getId());
            sampleMap.put("parentExternalId", parentSample.getExternalId());
        } else {
            sampleMap.put("isAliquot", false);
            sampleMap.put("nestingLevel", 0);
            sampleMap.put("parentSampleItemId", null);
            sampleMap.put("parentExternalId", null);
        }

        return sampleMap;
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

        NoteBook notebook = noteBookService.get(notebookId);
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

        try {
            List<SampleItem> children = sampleEntryService.createChildSamplesForPage(notebookId, pageId,
                    request.getParentSampleIds(), childCount, prefix, sysUserId);

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
    public ResponseEntity<Map<String, Object>> routeSamples(@PathVariable("notebookId") Integer notebookId,
            @RequestBody RouteSamplesRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook = noteBookService.get(notebookId);
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

        NoteBook notebook = noteBookService.get(notebookId);
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

        NoteBook notebook = noteBookService.get(notebookId);
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

        NoteBook notebook = noteBookService.get(notebookId);
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

        NoteBook notebook = noteBookService.get(notebookId);
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

            int assignedCount;

            Map<Integer, String> wellAssignments = null;
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
            } else {
                // Use hierarchical location-based storage
                assignedCount = sampleRoutingService.bulkRouteToStorage(notebookId, request.getSampleIds(),
                        request.getLocationId(), request.getLocationType() != null ? request.getLocationType() : "rack",
                        condition, retentionYears, sysUserId);
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
                            "Creating SampleStorageAssignment for sample " + sampleId + " at " + locationTypeForAssign
                                    + " " + locationIdForAssign + " (well: " + wellCoord + ")");

                    // Check if sample already has a storage assignment BEFORE trying to assign
                    // This avoids the transaction rollback issue that occurs when
                    // assignSampleItemWithLocation throws an exception
                    Map<String, Object> existingLocation = sampleStorageService
                            .getSampleItemLocation(sampleId.toString());
                    boolean hasExistingAssignment = existingLocation != null && !existingLocation.isEmpty()
                            && existingLocation.get("location") != null
                            && !existingLocation.get("location").toString().isEmpty();

                    if (request.isReassign() || hasExistingAssignment) {
                        // Use move for reassignment or if sample already has an assignment
                        sampleStorageService.moveSampleItemWithLocation(sampleId.toString(), locationIdForAssign,
                                locationTypeForAssign, wellCoord,
                                hasExistingAssignment ? "Notebook assignment update" : "Notebook reassignment", notes);
                    } else {
                        // Create new assignment
                        sampleStorageService.assignSampleItemWithLocation(sampleId.toString(), locationIdForAssign,
                                locationTypeForAssign, wellCoord, notes);
                    }
                    storageAssignmentCount++;
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getName(), "assignSamplesToStorage",
                            "Error creating SampleStorageAssignment for sample " + sampleId + ": " + e.getMessage());
                }
            }
            LogEvent.logInfo(this.getClass().getName(), "assignSamplesToStorage", "Created " + storageAssignmentCount
                    + " SampleStorageAssignment records for notebook " + notebookId);

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
                // Update NotebookPageSample records with storage info and status
                for (Integer sampleId : request.getSampleIds()) {
                    try {
                        org.openelisglobal.notebook.valueholder.NotebookPageSample nps = notebookPageSampleService
                                .getByPageIdAndSampleItemId(targetPage.getId(), sampleId);

                        // Auto-create if doesn't exist (only for Storage page, not Routing page)
                        if (nps == null && request.getPageId() == null) {
                            notebookPageSampleService.createPageSampleForPage(targetPage.getId(), sampleId,
                                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS);
                            nps = notebookPageSampleService.getByPageIdAndSampleItemId(targetPage.getId(), sampleId);
                        }

                        if (nps != null) {
                            // If request came from Routing page, mark as COMPLETED (storage assigned)
                            // Otherwise mark as IN_PROGRESS (for Storage page workflow)
                            if (request.getPageId() != null) {
                                nps.setStatus(
                                        org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED);
                                nps.setCompletedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                            } else if (nps
                                    .getStatus() == org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.PENDING) {
                                nps.setStatus(
                                        org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS);
                            }

                            // Store storage info in data field
                            Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                    : new HashMap<>();
                            data.put("storageCondition", condition.name());
                            data.put("retentionYears", retentionYears);
                            data.put("retentionExpiry", expiryDate.toString());

                            // Add box/well info if available
                            if (request.getBoxId() != null) {
                                data.put("boxId", request.getBoxId());
                                String wellCoord = wellAssignments != null ? wellAssignments.get(sampleId) : null;
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
                            }

                            nps.setData(data);
                            notebookPageSampleService.update(nps);
                        }
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getName(), "assignSamplesToStorage",
                                "Error updating NotebookPageSample for sample " + sampleId + ": " + e.getMessage());
                    }
                }
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
        private Integer boxId;
        private Map<String, String> wellAssignments;
        private String locationId;
        private String locationType;
        private String positionCoordinate;
        private String condition;
        private int retentionYears;
        private boolean reassign; // Flag to allow reassignment of already-assigned samples
        private Integer pageId; // Notebook page ID for routing context

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
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
    }
}
