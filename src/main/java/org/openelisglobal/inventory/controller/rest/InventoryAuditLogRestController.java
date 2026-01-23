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

    private Map<String, Object> transformHistoryToAuditLog(History history, String entityType) {
        Map<String, Object> auditLog = new HashMap<>();

        auditLog.put("id", history.getId());
        auditLog.put("timestamp", history.getTimestamp());
        auditLog.put("activity", history.getActivity()); // INSERT, UPDATE, DELETE

        String userName = getUserName(history.getSysUserId());
        auditLog.put("performedByUser", userName);
        auditLog.put("sysUserId", history.getSysUserId());

        String changesXml = history.getChanges() != null ? new String(history.getChanges()) : null;
        Map<String, Map<String, String>> parsedChanges = parseXmlChanges(changesXml);

        auditLog.put("changes", parsedChanges);
        auditLog.put("changesXml", changesXml);

        String summary = generateChangeSummary(parsedChanges, history.getActivity(), entityType);
        auditLog.put("summary", summary);

        return auditLog;
    }

    /**
     * Parses audit trail XML changes into a structured map format. Supports two XML
     * formats: 1. Standard format: &lt;field
     * name="fieldName"&gt;&lt;old&gt;...&lt;/old&gt;&lt;new&gt;...&lt;/new&gt;&lt;/field&gt;
     * 2. Legacy format: &lt;fieldName&gt;value&lt;/fieldName&gt;
     *
     * @param xml the XML string containing change records
     * @return Map of field names to their old/new values, empty map if no changes
     */
    private Map<String, Map<String, String>> parseXmlChanges(String xml) {
        Map<String, Map<String, String>> changes = new HashMap<>();
        if (xml == null || xml.trim().isEmpty()) {
            return changes;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader("<root>" + xml + "</root>")));

            Element root = doc.getDocumentElement();
            NodeList childNodes = root.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element fieldElement = (Element) childNodes.item(i);
                    String fieldName = fieldElement.getTagName();

                    Map<String, String> fieldChange = new HashMap<>();

                    if (fieldName.equals("field") && fieldElement.hasAttribute("name")) {
                        fieldName = fieldElement.getAttribute("name");

                        NodeList oldNodes = fieldElement.getElementsByTagName("old");
                        if (oldNodes.getLength() > 0) {
                            fieldChange.put("old", oldNodes.item(0).getTextContent());
                        } else {
                            fieldChange.put("old", "");
                        }

                        NodeList newNodes = fieldElement.getElementsByTagName("new");
                        if (newNodes.getLength() > 0) {
                            fieldChange.put("new", newNodes.item(0).getTextContent());
                        } else {
                            fieldChange.put("new", "");
                        }

                        changes.put(fieldName, fieldChange);
                    } else {
                        String value = fieldElement.getTextContent();
                        if (value != null && !value.trim().isEmpty()) {
                            fieldChange.put("old", value);
                            fieldChange.put("new", "");
                            changes.put(fieldName, fieldChange);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "parseXmlChanges",
                    "Error parsing XML changes: " + e.getMessage() + ", XML was: " + xml);
        }

        return changes;
    }

    private String generateChangeSummary(Map<String, Map<String, String>> changes, String activity, String entityType) {
        if (changes.isEmpty()) {
            if ("I".equals(activity)) {
                return "Created new " + entityType.toLowerCase();
            } else if ("U".equals(activity)) {
                return "Updated " + entityType.toLowerCase() + " (no field details available)";
            } else if ("D".equals(activity)) {
                return "Deleted " + entityType.toLowerCase();
            } else {
                return activity + " " + entityType.toLowerCase();
            }
        }

        String primaryChange = getKeyChangeDescription(changes, entityType);
        if (primaryChange != null) {
            return primaryChange;
        }

        List<String> changeSummaries = new ArrayList<>();
        int maxSummaries = 3;
        int count = 0;

        for (Map.Entry<String, Map<String, String>> entry : changes.entrySet()) {
            if (count >= maxSummaries) {
                int remaining = changes.size() - maxSummaries;
                changeSummaries.add("+" + remaining + " more");
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

    private String getKeyChangeDescription(Map<String, Map<String, String>> changes, String entityType) {
        if ("LOT".equals(entityType)) {
            if (changes.containsKey("currentQuantity")) {
                Map<String, String> qtyChange = changes.get("currentQuantity");
                String oldQty = qtyChange.get("old");
                String newQty = qtyChange.get("new");
                return "Updated quantity: " + oldQty + " → " + newQty;
            }
            if (changes.containsKey("qcStatus")) {
                Map<String, String> qcChange = changes.get("qcStatus");
                String newStatus = qcChange.get("new");
                return "QC status changed to " + newStatus;
            }
            if (changes.containsKey("status")) {
                Map<String, String> statusChange = changes.get("status");
                String newStatus = statusChange.get("new");
                return "Lot status changed to " + newStatus;
            }
        }

        if ("ITEM".equals(entityType)) {
            if (changes.containsKey("isActive")) {
                Map<String, String> activeChange = changes.get("isActive");
                String newValue = activeChange.get("new");
                return "Y".equals(newValue) ? "Activated item" : "Deactivated item";
            }
            if (changes.containsKey("itemName")) {
                Map<String, String> nameChange = changes.get("itemName");
                return "Renamed to " + nameChange.get("new");
            }
        }

        if ("LOCATION".equals(entityType)) {
            if (changes.containsKey("name")) {
                Map<String, String> nameChange = changes.get("name");
                return "Renamed to " + nameChange.get("new");
            }
        }

        if ("USAGE".equals(entityType)) {
            if (changes.containsKey("quantityUsed")) {
                Map<String, String> qtyChange = changes.get("quantityUsed");
                return "Recorded usage: " + qtyChange.get("new") + " units";
            }
        }

        if ("TRANSACTION".equals(entityType)) {
            if (changes.containsKey("transactionType")) {
                Map<String, String> typeChange = changes.get("transactionType");
                String txType = typeChange.get("new");
                if (changes.containsKey("quantityChange")) {
                    String qtyChange = changes.get("quantityChange").get("new");
                    return txType + " transaction: " + qtyChange + " units";
                }
                return txType + " transaction recorded";
            }
        }

        return null;
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private String formatFieldName(String fieldName) {
        String spaced = fieldName.replaceAll("([A-Z])", " $1").trim();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

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

            // Define all inventory tables to query (must match reference_tables.name
            // exactly)
            Map<String, String> inventoryTables = new HashMap<>();
            inventoryTables.put("inventory_item", "ITEM");
            inventoryTables.put("inventory_lot", "LOT");
            inventoryTables.put("inventory_storage_location", "LOCATION");
            inventoryTables.put("inventory_usage", "USAGE");
            inventoryTables.put("inventory_transaction", "TRANSACTION");

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

            // Must match reference_tables.name exactly (lowercase)
            String[] tables = { "inventory_item", "inventory_lot", "inventory_storage_location", "inventory_usage",
                    "inventory_transaction" };

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
