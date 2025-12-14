package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntry.EntryStatus;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
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

        try {
            // Verify template exists
            NoteBook template = noteBookService.get(notebookId);
            if (template == null) {
                return ResponseEntity.notFound().build();
            }

            NotebookEntry entry = notebookEntryService.createEntry(notebookId, title, sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", entry.getId());
            response.put("notebookId", notebookId);
            response.put("title", entry.getEffectiveTitle());
            response.put("status", entry.getStatus().name());

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
    public ResponseEntity<Map<String, Object>> getEntry(@PathVariable("entryId") Integer entryId) {
        NotebookEntry entry = notebookEntryService.getWithRelationships(entryId);
        if (entry == null) {
            return ResponseEntity.notFound().build();
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
            @PathVariable("notebookId") Integer notebookId) {

        List<NotebookEntry> entries = notebookEntryService.findByNotebookId(notebookId);
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
    public ResponseEntity<List<Map<String, Object>>> getEntriesByStatus(@PathVariable("status") EntryStatus status) {

        List<NotebookEntry> entries = notebookEntryService.findByStatus(status);
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

        notebookEntryService.updateStatus(entryId, status, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", entryId);
        response.put("status", status.name());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Add a sample to an entry.
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
}
