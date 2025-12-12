package org.openelisglobal.inventory.controller.rest;

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
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
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

@RestController
@RequestMapping("/rest/inventory/audit-logs")
public class InventoryAuditLogRestController extends BaseRestController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private SystemUserService systemUserService;

    @GetMapping(value = "/item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getItemAuditTrail(@PathVariable Long itemId) {
        try {
            ReferenceTables refTable = referenceTablesService.getReferenceTableByName("inventory_item");
            if (refTable == null) {
                return ResponseEntity.notFound().build();
            }
            List<History> history = historyService.getHistoryByRefIdAndRefTableId(itemId.toString(), refTable.getId());
            List<Map<String, Object>> auditLogs = history.stream().map(h -> transformHistoryToAuditLog(h, "ITEM"))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot/{lotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getLotAuditTrail(@PathVariable Long lotId) {
        try {
            ReferenceTables refTable = referenceTablesService.getReferenceTableByName("inventory_lot");
            if (refTable == null) {
                return ResponseEntity.notFound().build();
            }
            List<History> history = historyService.getHistoryByRefIdAndRefTableId(lotId.toString(), refTable.getId());
            List<Map<String, Object>> auditLogs = history.stream().map(h -> transformHistoryToAuditLog(h, "LOT"))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/location/{locationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getLocationAuditTrail(@PathVariable Long locationId) {
        try {
            ReferenceTables refTable = referenceTablesService.getReferenceTableByName("inventory_storage_location");
            if (refTable == null) {
                return ResponseEntity.notFound().build();
            }
            List<History> history = historyService.getHistoryByRefIdAndRefTableId(locationId.toString(),
                    refTable.getId());
            List<Map<String, Object>> auditLogs = history.stream().map(h -> transformHistoryToAuditLog(h, "LOCATION"))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Transform History entity to frontend-compatible audit log format
     */
    private Map<String, Object> transformHistoryToAuditLog(History history, String entityType) {
        Map<String, Object> auditLog = new HashMap<>();

        auditLog.put("id", history.getId());
        auditLog.put("timestamp", history.getTimestamp());
        auditLog.put("activity", history.getActivity()); // INSERT, UPDATE, DELETE

        // Get user information
        String userName = getUserName(history.getSysUserId());
        auditLog.put("performedByUser", userName);
        auditLog.put("sysUserId", history.getSysUserId());

        // Parse XML changes
        String changesXml = history.getChanges() != null ? new String(history.getChanges()) : null;
        Map<String, Map<String, String>> parsedChanges = parseXmlChanges(changesXml);

        auditLog.put("changes", parsedChanges);
        auditLog.put("changesXml", changesXml);

        // Generate human-readable summary
        String summary = generateChangeSummary(parsedChanges, history.getActivity(), entityType);
        auditLog.put("summary", summary);

        return auditLog;
    }

    /**
     * Parse XML changes into structured format Returns Map of field -> {old, new}
     * values
     */
    private Map<String, Map<String, String>> parseXmlChanges(String xml) {
        Map<String, Map<String, String>> changes = new HashMap<>();
        if (xml == null || xml.trim().isEmpty()) {
            return changes;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader("<changes>" + xml + "</changes>")));

            NodeList fieldNodes = doc.getElementsByTagName("field");
            for (int i = 0; i < fieldNodes.getLength(); i++) {
                Element fieldElement = (Element) fieldNodes.item(i);
                String fieldName = fieldElement.getAttribute("name");

                Map<String, String> fieldChange = new HashMap<>();

                NodeList oldNodes = fieldElement.getElementsByTagName("old");
                if (oldNodes.getLength() > 0) {
                    fieldChange.put("old", oldNodes.item(0).getTextContent());
                }

                NodeList newNodes = fieldElement.getElementsByTagName("new");
                if (newNodes.getLength() > 0) {
                    fieldChange.put("new", newNodes.item(0).getTextContent());
                }

                changes.put(fieldName, fieldChange);
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return changes;
    }

    /**
     * Generate human-readable summary of changes
     */
    private String generateChangeSummary(Map<String, Map<String, String>> changes, String activity, String entityType) {
        if (changes.isEmpty()) {
            return activity + " " + entityType.toLowerCase();
        }

        List<String> changeSummaries = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : changes.entrySet()) {
            String field = entry.getKey();
            Map<String, String> values = entry.getValue();
            String oldValue = values.get("old");
            String newValue = values.get("new");

            String fieldLabel = formatFieldName(field);
            if (oldValue != null && newValue != null) {
                changeSummaries.add(fieldLabel + ": " + oldValue + " → " + newValue);
            } else if (newValue != null) {
                changeSummaries.add(fieldLabel + " set to " + newValue);
            } else if (oldValue != null) {
                changeSummaries.add(fieldLabel + " cleared");
            }
        }

        return String.join(", ", changeSummaries);
    }

    /**
     * Format field name for display (camelCase -> Title Case)
     */
    private String formatFieldName(String fieldName) {
        // Convert camelCase to spaces
        String spaced = fieldName.replaceAll("([A-Z])", " $1").trim();
        // Capitalize first letter
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    /**
     * Get user name from user ID
     */
    private String getUserName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "System";
        }
        try {
            SystemUser user = systemUserService.get(userId);
            if (user != null && user.getLoginName() != null) {
                return user.getLoginName();
            }
            return "User " + userId;
        } catch (Exception e) {
            return "User " + userId;
        }
    }

    /**
     * Get ALL inventory-related audit logs (unified view across all inventory
     * tables)
     *
     * @param startDate  Optional start date filter (format: yyyy-MM-dd)
     * @param endDate    Optional end date filter (format: yyyy-MM-dd)
     * @param entityType Optional filter by entity type (ITEM, LOT, LOCATION, USAGE,
     *                   TRANSACTION)
     * @param userId     Optional filter by user ID
     * @param activity   Optional filter by activity type (I, U, D)
     * @param limit      Maximum number of records to return (default: 100, max:
     *                   1000)
     * @return Unified list of all inventory audit logs sorted by timestamp
     *         descending
     */
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllInventoryAuditLogs(
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String entityType, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String activity, @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate and cap limit
            if (limit > 1000) {
                limit = 1000;
            }
            if (limit < 1) {
                limit = 100;
            }

            List<Map<String, Object>> allLogs = new ArrayList<>();

            // Define all inventory tables to query
            Map<String, String> inventoryTables = new HashMap<>();
            inventoryTables.put("INVENTORY_ITEM", "ITEM");
            inventoryTables.put("INVENTORY_LOT", "LOT");
            inventoryTables.put("INVENTORY_STORAGE_LOCATION", "LOCATION");
            inventoryTables.put("INVENTORY_USAGE", "USAGE");
            inventoryTables.put("INVENTORY_TRANSACTION", "TRANSACTION");

            // Parse date filters if provided
            Timestamp startTimestamp = null;
            Timestamp endTimestamp = null;
            if (startDate != null && !startDate.trim().isEmpty()) {
                LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
                startTimestamp = Timestamp.valueOf(start.atStartOfDay());
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
                endTimestamp = Timestamp.valueOf(end.atTime(23, 59, 59));
            }

            // Query each table
            for (Map.Entry<String, String> entry : inventoryTables.entrySet()) {
                String tableName = entry.getKey();
                String entityTypeLabel = entry.getValue();

                // Skip if filtering by entity type and this isn't it
                if (entityType != null && !entityType.trim().isEmpty()
                        && !entityTypeLabel.equalsIgnoreCase(entityType)) {
                    continue;
                }

                ReferenceTables refTable = referenceTablesService.getReferenceTableByName(tableName);
                if (refTable == null) {
                    continue; // Skip if table not registered
                }

                // Get all history for this table
                List<History> tableHistory = historyService.getAllHistoryByRefTableId(refTable.getId());

                // Transform and filter
                for (History history : tableHistory) {
                    // Apply filters
                    if (userId != null && !userId.trim().isEmpty() && !userId.equals(history.getSysUserId())) {
                        continue;
                    }
                    if (activity != null && !activity.trim().isEmpty() && !activity.equals(history.getActivity())) {
                        continue;
                    }
                    if (startTimestamp != null && history.getTimestamp().before(startTimestamp)) {
                        continue;
                    }
                    if (endTimestamp != null && history.getTimestamp().after(endTimestamp)) {
                        continue;
                    }

                    Map<String, Object> auditLog = transformHistoryToAuditLog(history, entityTypeLabel);
                    auditLog.put("entityType", entityTypeLabel);
                    auditLog.put("tableName", tableName);
                    allLogs.add(auditLog);
                }
            }

            // Sort by timestamp descending (newest first)
            allLogs.sort((a, b) -> {
                Timestamp tsA = (Timestamp) a.get("timestamp");
                Timestamp tsB = (Timestamp) b.get("timestamp");
                return tsB.compareTo(tsA);
            });

            // Apply pagination
            int totalRecords = allLogs.size();
            int fromIndex = Math.min(offset, totalRecords);
            int toIndex = Math.min(offset + limit, totalRecords);
            List<Map<String, Object>> paginatedLogs = allLogs.subList(fromIndex, toIndex);

            // Build response with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("logs", paginatedLogs);
            response.put("totalRecords", totalRecords);
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("hasMore", toIndex < totalRecords);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit log statistics for inventory module
     *
     * @return Summary statistics about inventory audit trail
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAuditLogStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            String[] tables = { "INVENTORY_ITEM", "INVENTORY_LOT", "INVENTORY_STORAGE_LOCATION", "INVENTORY_USAGE",
                    "INVENTORY_TRANSACTION" };

            int totalLogs = 0;
            Map<String, Integer> countByTable = new HashMap<>();
            Map<String, Integer> countByActivity = new HashMap<>();
            countByActivity.put("INSERT", 0);
            countByActivity.put("UPDATE", 0);
            countByActivity.put("DELETE", 0);

            for (String tableName : tables) {
                ReferenceTables refTable = referenceTablesService.getReferenceTableByName(tableName);
                if (refTable != null) {
                    List<History> tableHistory = historyService.getAllHistoryByRefTableId(refTable.getId());
                    int count = tableHistory.size();
                    countByTable.put(tableName, count);
                    totalLogs += count;

                    // Count by activity
                    for (History h : tableHistory) {
                        String activity = h.getActivity();
                        if ("I".equals(activity)) {
                            countByActivity.put("INSERT", countByActivity.get("INSERT") + 1);
                        } else if ("U".equals(activity)) {
                            countByActivity.put("UPDATE", countByActivity.get("UPDATE") + 1);
                        } else if ("D".equals(activity)) {
                            countByActivity.put("DELETE", countByActivity.get("DELETE") + 1);
                        }
                    }
                }
            }

            stats.put("totalLogs", totalLogs);
            stats.put("countByTable", countByTable);
            stats.put("countByActivity", countByActivity);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
