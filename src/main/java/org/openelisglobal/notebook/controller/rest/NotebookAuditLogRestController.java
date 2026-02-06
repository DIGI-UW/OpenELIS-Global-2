package org.openelisglobal.notebook.controller.rest;

import java.io.StringReader;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookAuditService;
import org.openelisglobal.notebook.valueholder.NotebookAuditLog;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * REST Controller for Notebook Audit Log operations.
 *
 * <p>
 * Provides endpoints to query audit logs with filtering and pagination.
 */
@RestController
@RequestMapping("/rest/notebook/audit-logs")
public class NotebookAuditLogRestController extends BaseRestController {

    @Autowired
    private NotebookAuditService notebookAuditService;

    @Autowired
    private SystemUserService systemUserService;

    /**
     * Get audit logs for a specific notebook template.
     */
    @GetMapping(value = "/notebook/{notebookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getNotebookAuditTrail(@PathVariable String notebookId) {
        try {
            List<NotebookAuditLog> logs = notebookAuditService.getAuditLogsForEntity(notebookId, "NOTEBOOK");
            List<Map<String, Object>> result = logs.stream().map(this::transformAuditLogToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit logs for a specific notebook entry.
     */
    @GetMapping(value = "/entry/{entryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getEntryAuditTrail(@PathVariable String entryId) {
        try {
            List<NotebookAuditLog> logs = notebookAuditService.getAuditLogsForEntity(entryId, "NOTEBOOK_ENTRY");
            List<Map<String, Object>> result = logs.stream().map(this::transformAuditLogToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit logs for notebook page sample status.
     */
    @GetMapping(value = "/page-sample/{pageSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getPageSampleAuditTrail(@PathVariable String pageSampleId) {
        try {
            List<NotebookAuditLog> logs = notebookAuditService.getAuditLogsForEntity(pageSampleId,
                    "NOTEBOOK_PAGE_SAMPLE");
            List<Map<String, Object>> result = logs.stream().map(this::transformAuditLogToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search audit logs with filtering and pagination.
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchAuditLogs(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userId, @RequestParam(required = false) String activity,
            @RequestParam(required = false) String statusNew, @RequestParam(required = false) String referenceId,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "100") int limit) {
        try {
            // Build filter map
            Map<String, Object> filters = new HashMap<>();

            if (entityType != null && !entityType.isEmpty()) {
                filters.put("entityType", entityType);
            }

            if (activity != null && !activity.isEmpty()) {
                filters.put("activity", activity);
            }

            if (userId != null && !userId.isEmpty()) {
                filters.put("sysUserId", userId);
            }

            if (statusNew != null && !statusNew.isEmpty()) {
                filters.put("statusNew", statusNew);
            }

            if (referenceId != null && !referenceId.isEmpty()) {
                filters.put("referenceId", referenceId);
            }

            // Parse dates
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
                    filters.put("startDate", Timestamp.valueOf(date.atStartOfDay()));
                } catch (Exception e) {
                    LogEvent.logWarn("NotebookAuditLogRestController", "searchAuditLogs",
                            "Invalid startDate format: " + startDate);
                }
            }

            if (endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
                    filters.put("endDate", Timestamp.valueOf(date.atTime(23, 59, 59)));
                } catch (Exception e) {
                    LogEvent.logWarn("NotebookAuditLogRestController", "searchAuditLogs",
                            "Invalid endDate format: " + endDate);
                }
            }

            filters.put("offset", offset);
            filters.put("limit", limit);

            // Search
            Map<String, Object> result = notebookAuditService.searchAuditLogs(filters);

            // Transform logs
            @SuppressWarnings("unchecked")
            List<NotebookAuditLog> logs = (List<NotebookAuditLog>) result.get("logs");
            List<Map<String, Object>> transformedLogs = logs.stream().map(this::transformAuditLogToMap)
                    .collect(Collectors.toList());

            result.put("logs", transformedLogs);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit log statistics.
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Long> stats = notebookAuditService.getStatistics();
            Map<String, Object> result = new HashMap<>();
            result.put("statistics", stats);
            result.put("totalLogs", stats.values().stream().mapToLong(Long::longValue).sum());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent audit logs.
     */
    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getRecentAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<NotebookAuditLog> logs = notebookAuditService.getRecentAuditLogs(limit);
            List<Map<String, Object>> result = logs.stream().map(this::transformAuditLogToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Transform NotebookAuditLog to Map for JSON response.
     */
    private Map<String, Object> transformAuditLogToMap(NotebookAuditLog log) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", log.getId());
        map.put("referenceId", log.getReferenceId());
        map.put("referenceTable", log.getReferenceTable());
        map.put("timestamp", log.getTimestamp());
        map.put("activity", log.getActivity());
        map.put("activityDisplay", getActivityDisplay(log.getActivity()));
        map.put("entityType", log.getEntityType());
        map.put("entityTitle", log.getEntityTitle());
        map.put("statusOld", log.getStatusOld());
        map.put("statusNew", log.getStatusNew());
        map.put("performedByUser", log.getPerformedByUser());
        map.put("sysUserId", log.getSysUserId());

        // Parse XML changes
        Map<String, Map<String, String>> changes = parseXmlChanges(log.getChangesXml());
        map.put("changes", changes);

        // Generate human-readable summary
        String summary = generateChangeSummary(changes, log.getActivity(), log.getEntityType());
        map.put("summary", summary);

        return map;
    }

    /**
     * Parse XML changes into structured map.
     */
    private Map<String, Map<String, String>> parseXmlChanges(String xml) {
        Map<String, Map<String, String>> changes = new HashMap<>();

        if (xml == null || xml.isEmpty()) {
            return changes;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader("<root>" + xml + "</root>")));

            NodeList fieldNodes = doc.getElementsByTagName("field");
            for (int i = 0; i < fieldNodes.getLength(); i++) {
                Element fieldElement = (Element) fieldNodes.item(i);
                String fieldName = fieldElement.getAttribute("name");

                Map<String, String> values = new HashMap<>();

                NodeList oldNodes = fieldElement.getElementsByTagName("old");
                if (oldNodes.getLength() > 0) {
                    values.put("old", oldNodes.item(0).getTextContent());
                }

                NodeList newNodes = fieldElement.getElementsByTagName("new");
                if (newNodes.getLength() > 0) {
                    values.put("new", newNodes.item(0).getTextContent());
                }

                changes.put(fieldName, values);
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "parseXmlChanges",
                    "Error parsing XML: " + e.getMessage());
        }

        return changes;
    }

    /**
     * Generate human-readable change summary.
     */
    private String generateChangeSummary(Map<String, Map<String, String>> changes, String activity, String entityType) {
        if (changes.isEmpty()) {
            if ("I".equals(activity)) {
                return "Created new " + formatEntityType(entityType);
            } else if ("U".equals(activity)) {
                return "Updated " + formatEntityType(entityType);
            } else if ("D".equals(activity)) {
                return "Deleted " + formatEntityType(entityType);
            }
            return activity + " " + formatEntityType(entityType);
        }

        // Check for status changes first (most important)
        if (changes.containsKey("status")) {
            Map<String, String> statusChange = changes.get("status");
            String oldStatus = statusChange.get("old");
            String newStatus = statusChange.get("new");
            if (oldStatus != null && newStatus != null) {
                return "Status changed from " + oldStatus + " to " + newStatus;
            } else if (newStatus != null) {
                return "Status set to " + newStatus;
            }
        }

        // Build summary from first few changes
        List<String> changeSummaries = new ArrayList<>();
        int maxSummaries = 3;
        int count = 0;

        for (Map.Entry<String, Map<String, String>> entry : changes.entrySet()) {
            if (count >= maxSummaries) {
                int remaining = changes.size() - maxSummaries;
                changeSummaries.add("+" + remaining + " more field" + (remaining > 1 ? "s" : ""));
                break;
            }

            String field = entry.getKey();
            Map<String, String> values = entry.getValue();
            String oldValue = values.get("old");
            String newValue = values.get("new");

            String fieldLabel = formatFieldName(field);
            if (oldValue != null && newValue != null) {
                changeSummaries.add(fieldLabel + ": " + truncate(oldValue, 20) + " → " + truncate(newValue, 20));
            } else if (newValue != null) {
                changeSummaries.add(fieldLabel + " set to " + truncate(newValue, 20));
            } else if (oldValue != null) {
                changeSummaries.add(fieldLabel + " cleared");
            }
            count++;
        }

        return String.join(", ", changeSummaries);
    }

    /**
     * Format entity type for display.
     */
    private String formatEntityType(String entityType) {
        return entityType.toLowerCase().replace("_", " ");
    }

    /**
     * Format field name for display.
     */
    private String formatFieldName(String fieldName) {
        // Convert camelCase to Title Case
        return fieldName.replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * Truncate string to max length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Get display text for activity code.
     */
    private String getActivityDisplay(String activity) {
        switch (activity) {
        case "I":
            return "INSERT";
        case "U":
            return "UPDATE";
        case "D":
            return "DELETE";
        default:
            return activity;
        }
    }
}
