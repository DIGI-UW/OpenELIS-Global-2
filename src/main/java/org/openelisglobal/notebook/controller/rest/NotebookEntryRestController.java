package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntry.EntryStatus;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for NotebookEntry operations. Handles notebook entry
 * (instance) CRUD operations, separate from notebook templates.
 */
@RestController
@RequestMapping(value = "/rest/notebook-entry")
public class NotebookEntryRestController extends BaseRestController {

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NotebookSecurityService notebookSecurityService;

    @Autowired
    private TestSectionService testSectionService;

    /**
     * Create a new notebook entry from a template.
     *
     * @param notebookId the template notebook ID
     * @param title      optional custom title for the entry
     * @param request    HTTP request for user session
     * @return the created entry ID
     */
    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createEntry(@RequestParam("notebookId") Integer notebookId,
            @RequestParam(value = "title", required = false) String title, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        String loginLabUnit = getLoginLabUnit(request);

        try {
            // Check if template exists first
            NoteBook template = noteBookService.get(notebookId);
            if (template == null) {
                return ResponseEntity.notFound().build();
            }

            // Check if user can view the template
            if (!notebookSecurityService.canViewTemplate(notebookId, sysUserId, loginLabUnit)) {
                Map<String, Object> error = new HashMap<>();
                if (loginLabUnit == null || loginLabUnit.isEmpty()) {
                    error.put("error",
                            "Access denied. No department selected. Please log in with a specific lab unit or ensure you have 'AllLabUnits' access.");
                } else {
                    error.put("error",
                            "Access denied. This template is not assigned to your department: " + loginLabUnit);
                }
                error.put("loginLabUnit", loginLabUnit != null ? loginLabUnit : "none");
                return ResponseEntity.status(403).body(error);
            }

            // Check if user can create entries for this template
            if (!notebookSecurityService.canCreateEntry(notebookId, sysUserId, loginLabUnit)) {
                // User can view but not create - must be role restriction
                // Provide detailed info to help debug
                Set<String> allowedRoles = noteBookService.getNoteBookAllowedRoles(notebookId);
                Map<String, Object> error = new HashMap<>();
                if (allowedRoles != null && !allowedRoles.isEmpty()) {
                    error.put("error",
                            "Access denied. You need one of the required roles to create entries for this template.");
                    error.put("requiredRoles", allowedRoles);
                } else {
                    error.put("error", "Access denied. Unable to verify permissions for entry creation.");
                }
                error.put("loginLabUnit", loginLabUnit);
                return ResponseEntity.status(403).body(error);
            }

            // Get organization from loginLabUnit
            Organization organization = notebookSecurityService.getOrganizationForLoginLabUnit(loginLabUnit);

            NotebookEntry entry = notebookEntryService.createEntry(notebookId, title, organization, sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", entry.getId());
            response.put("notebookId", notebookId);
            response.put("title", entry.getEffectiveTitle());
            response.put("status", entry.getStatus().name());
            if (organization != null) {
                response.put("organizationId", organization.getId());
                response.put("organizationName", organization.getOrganizationName());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create notebook entry: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get a notebook entry by ID.
     *
     * @param entryId the entry ID
     * @return the entry details
     */
    @GetMapping(value = "/{entryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getEntry(@PathVariable("entryId") Integer entryId, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        NotebookEntry entry = notebookEntryService.getWithRelationships(entryId);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if user can view this entry
        if (!notebookSecurityService.canViewEntry(entry, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to this entry"));
        }

        return ResponseEntity.ok(convertToMap(entry));
    }

    /**
     * Get all entries for a notebook template.
     *
     * @param notebookId the template notebook ID
     * @return list of entries
     */
    @GetMapping(value = "/by-notebook/{notebookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEntriesByNotebook(
            @PathVariable("notebookId") Integer notebookId, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // Filter entries by user's accessible organizations
        List<NotebookEntry> entries = notebookEntryService.findByNotebookId(notebookId).stream()
                .filter(entry -> notebookSecurityService.canViewEntry(entry, sysUserId, loginLabUnit))
                .collect(Collectors.toList());
        List<Map<String, Object>> result = entries.stream().map(this::convertToMap).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get entries by status.
     *
     * @param status the entry status
     * @return list of entries with that status
     */
    @GetMapping(value = "/by-status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEntriesByStatus(@PathVariable("status") EntryStatus status,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // Filter entries by user's accessible organizations
        List<NotebookEntry> entries = notebookEntryService.findByStatus(status).stream()
                .filter(entry -> notebookSecurityService.canViewEntry(entry, sysUserId, loginLabUnit))
                .collect(Collectors.toList());
        List<Map<String, Object>> result = entries.stream().map(this::convertToMap).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Update entry status.
     *
     * @param entryId the entry ID
     * @param status  the new status
     * @param request HTTP request for user session
     * @return success response
     */
    @PutMapping(value = "/{entryId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable("entryId") Integer entryId,
            @RequestParam("status") EntryStatus status, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can edit this entry
        if (!notebookSecurityService.canEditEntry(entryId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. You need the required role to edit this entry."));
        }

        notebookEntryService.updateStatus(entryId, status, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", entryId);
        response.put("status", status.name());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Add a new missed sample to an entry. Creates a new sample item and adds it to
     * the entry.
     *
     * @param entryId the entry ID
     * @param body    request body with sample details
     * @param request HTTP request for user session
     * @return success response with new sample details
     */
    @PostMapping(value = "/{entryId}/samples/add", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addMissedSample(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        String externalId = (String) body.get("externalId");
        String sampleType = (String) body.get("sampleType");
        Integer pageId = body.get("pageId") != null ? Integer.parseInt(body.get("pageId").toString()) : null;
        String parentSampleId = (String) body.get("parentSampleId");
        String notes = (String) body.get("notes");
        String source = (String) body.get("source");

        if (externalId == null || externalId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample ID (externalId) is required"));
        }

        if (sampleType == null || sampleType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample type is required"));
        }

        try {
            // Get or create sample type
            org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService = org.openelisglobal.spring.util.SpringContext
                    .getBean(org.openelisglobal.typeofsample.service.TypeOfSampleService.class);
            org.openelisglobal.typeofsample.valueholder.TypeOfSample typeObj = null;

            // Try to get sample type by ID first, then by name if that fails
            try {
                // Check if sampleType is a numeric ID
                Integer.parseInt(sampleType);
                typeObj = typeOfSampleService.get(sampleType);
            } catch (NumberFormatException e) {
                // sampleType is a name/description, look it up by localized name
                typeObj = typeOfSampleService.getTypeOfSampleByLocalizedName(sampleType, java.util.Locale.ENGLISH);
            }

            if (typeObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample type not found: " + sampleType));
            }

            // Get the entry to access its samples
            NotebookEntry entry = notebookEntryService.getWithRelationships(entryId);
            if (entry == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Entry not found: " + entryId));
            }

            // Create a new SampleItem
            org.openelisglobal.sampleitem.service.SampleItemService sampleItemService = org.openelisglobal.spring.util.SpringContext
                    .getBean(org.openelisglobal.sampleitem.service.SampleItemService.class);
            org.openelisglobal.sample.service.SampleService sampleService = org.openelisglobal.spring.util.SpringContext
                    .getBean(org.openelisglobal.sample.service.SampleService.class);

            // Find a sample from the entry to get the parent sample (for the accession
            // number)
            org.openelisglobal.sample.valueholder.Sample parentSample = null;
            List<SampleItem> existingSamples = notebookEntryService.getSamples(entryId);
            if (!existingSamples.isEmpty()) {
                parentSample = existingSamples.get(0).getSample();
            }

            if (parentSample == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot add sample: No existing samples in entry to link to"));
            }

            // Create new sample item
            SampleItem newSampleItem = new SampleItem();
            newSampleItem.setSample(parentSample);
            newSampleItem.setExternalId(externalId.trim());
            newSampleItem.setTypeOfSample(typeObj);
            newSampleItem.setSortOrder(String.valueOf(existingSamples.size() + 1));
            newSampleItem.setSysUserId(sysUserId);

            // Save the sample item
            String newSampleItemId = sampleItemService.insert(newSampleItem);
            newSampleItem = sampleItemService.get(newSampleItemId);

            // Add sample to entry
            notebookEntryService.addSample(entryId, newSampleItem, sysUserId);

            // If pageId is provided, add sample to that specific page
            if (pageId != null) {
                org.openelisglobal.notebook.service.NotebookPageSampleService pageSampleService = org.openelisglobal.spring.util.SpringContext
                        .getBean(org.openelisglobal.notebook.service.NotebookPageSampleService.class);
                org.openelisglobal.notebook.service.NoteBookPageService pageService = org.openelisglobal.spring.util.SpringContext
                        .getBean(org.openelisglobal.notebook.service.NoteBookPageService.class);

                // Get the notebook page
                org.openelisglobal.notebook.valueholder.NoteBookPage page = pageService.get(pageId);
                if (page != null) {
                    org.openelisglobal.notebook.valueholder.NotebookPageSample pageSample = new org.openelisglobal.notebook.valueholder.NotebookPageSample();
                    pageSample.setNotebookPage(page);
                    pageSample.setSampleItemId(newSampleItem.getId());
                    pageSample.setStatus(org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.PENDING);
                    pageSample.setSysUserId(sysUserId);

                    // Store metadata in data JSON
                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("source", source != null ? source : "MISSED_FROM_FIELD");
                    sampleData.put("notes", notes);
                    if (parentSampleId != null && !parentSampleId.isEmpty()) {
                        sampleData.put("parentSampleId", parentSampleId);
                    }
                    pageSample.setData(sampleData);

                    pageSampleService.save(pageSample);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("sampleId", newSampleItem.getId());
            response.put("externalId", externalId);
            response.put("sampleType", sampleType);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to add sample: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Add an existing sample to an entry.
     *
     * @param entryId  the entry ID
     * @param sampleId the sample item ID
     * @param request  HTTP request for user session
     * @return success response
     */
    @PostMapping(value = "/{entryId}/samples/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSample(@PathVariable("entryId") Integer entryId,
            @PathVariable("sampleId") Integer sampleId, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can edit this entry
        if (!notebookSecurityService.canEditEntry(entryId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. You need the required role to edit this entry."));
        }

        // Get the sample item
        org.openelisglobal.sampleitem.service.SampleItemService sampleItemService = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.sampleitem.service.SampleItemService.class);
        SampleItem sample = sampleItemService.get(sampleId.toString());
        if (sample == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample not found: " + sampleId));
        }

        notebookEntryService.addSample(entryId, sample, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("entryId", entryId);
        response.put("sampleId", sampleId);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a sample from an entry.
     *
     * @param entryId  the entry ID
     * @param sampleId the sample item ID
     * @param request  HTTP request for user session
     * @return success response
     */
    @DeleteMapping(value = "/{entryId}/samples/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeSample(@PathVariable("entryId") Integer entryId,
            @PathVariable("sampleId") Integer sampleId, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can edit this entry
        if (!notebookSecurityService.canEditEntry(entryId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. You need the required role to edit this entry."));
        }

        notebookEntryService.removeSample(entryId, sampleId, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("entryId", entryId);
        response.put("sampleId", sampleId);
        response.put("removed", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Get samples for an entry.
     *
     * @param entryId the entry ID
     * @return list of samples
     */
    @GetMapping(value = "/{entryId}/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamples(@PathVariable("entryId") Integer entryId) {
        List<SampleItem> samples = notebookEntryService.getSamples(entryId);

        List<Map<String, Object>> result = samples.stream().map(sample -> {
            Map<String, Object> sampleMap = new HashMap<>();
            sampleMap.put("id", sample.getId());
            sampleMap.put("externalId", sample.getExternalId());
            sampleMap.put("sortOrder", sample.getSortOrder());
            if (sample.getTypeOfSample() != null) {
                sampleMap.put("sampleType", sample.getTypeOfSample().getDescription());
                sampleMap.put("sampleTypeId", sample.getTypeOfSample().getId());
            }
            if (sample.getSample() != null) {
                sampleMap.put("accessionNumber", sample.getSample().getAccessionNumber());
            }
            return sampleMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Add a comment to an entry.
     *
     * @param entryId the entry ID
     * @param body    request body with comment text
     * @param request HTTP request for user session
     * @return success response
     */
    @PostMapping(value = "/{entryId}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, String> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can edit this entry (adding comment is an edit action)
        if (!notebookSecurityService.canEditEntry(entryId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. You need the required role to add comments."));
        }

        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment text is required"));
        }

        notebookEntryService.addComment(entryId, text, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("entryId", entryId);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Get count of entries for a notebook.
     *
     * @param notebookId the template notebook ID
     * @return count of entries
     */
    @GetMapping(value = "/count/by-notebook/{notebookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> countByNotebook(@PathVariable("notebookId") Integer notebookId) {
        Long count = notebookEntryService.countByNotebookId(notebookId);

        Map<String, Object> response = new HashMap<>();
        response.put("notebookId", notebookId);
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Get count of entries by status.
     *
     * @param status the entry status
     * @return count of entries with that status
     */
    @GetMapping(value = "/count/by-status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> countByStatus(@PathVariable("status") EntryStatus status) {
        Long count = notebookEntryService.countByStatus(status);

        Map<String, Object> response = new HashMap<>();
        response.put("status", status.name());
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Sync pages from template to an entry's notebook instance. Adds any pages that
     * exist in the template but are missing from the instance. This is useful when
     * new pages are added to a template after instances were created.
     *
     * @param entryId the entry ID
     * @param request HTTP request for user session
     * @return result with number of pages added
     */
    @PostMapping(value = "/{entryId}/sync-pages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncPagesFromTemplate(@PathVariable("entryId") Integer entryId,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            // Get the entry to find the associated notebook instance
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null) {
                return ResponseEntity.notFound().build();
            }

            // Get the notebook instance ID from the entry
            NoteBook instanceNotebook = entry.getNotebook();
            if (instanceNotebook == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Entry has no associated notebook instance"));
            }

            // Sync pages from template
            int addedCount = noteBookService.syncPagesFromTemplate(instanceNotebook.getId(), sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("notebookInstanceId", instanceNotebook.getId());
            response.put("pagesAdded", addedCount);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to sync pages: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private Map<String, Object> convertToMap(NotebookEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.getId());
        map.put("title", entry.getEffectiveTitle());
        map.put("customTitle", entry.getTitle());
        map.put("status", entry.getStatus() != null ? entry.getStatus().name() : null);
        map.put("dateCreated", entry.getDateCreated());
        map.put("dateCompleted", entry.getDateCompleted());

        if (entry.getNotebook() != null) {
            Map<String, Object> notebookMap = new HashMap<>();
            notebookMap.put("id", entry.getNotebook().getId());
            notebookMap.put("title", entry.getNotebook().getTitle());
            notebookMap.put("type", entry.getNotebook().getType());
            map.put("notebook", notebookMap);
        }

        if (entry.getTechnician() != null) {
            Map<String, Object> techMap = new HashMap<>();
            techMap.put("id", entry.getTechnician().getId());
            techMap.put("name", entry.getTechnician().getNameForDisplay());
            map.put("technician", techMap);
        }

        if (entry.getCreator() != null) {
            Map<String, Object> creatorMap = new HashMap<>();
            creatorMap.put("id", entry.getCreator().getId());
            creatorMap.put("name", entry.getCreator().getNameForDisplay());
            map.put("creator", creatorMap);
        }

        // Add organization info for access control display
        if (entry.getOrganization() != null) {
            map.put("organizationId", entry.getOrganization().getId());
            map.put("organizationName", entry.getOrganization().getOrganizationName());
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
     * Get the login lab unit name for the current user from session.
     *
     * @param request the HTTP request
     * @return the login lab unit name, or null if not set
     */
    private String getLoginLabUnit(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        int loginLabUnitId = usd.getLoginLabUnit();
        if (loginLabUnitId == 0) {
            return null;
        }
        TestSection testSection = testSectionService.getTestSectionById(String.valueOf(loginLabUnitId));
        if (testSection != null) {
            return testSection.getLocalizedName();
        }
        return null;
    }
}
