package org.openelisglobal.notebook.controller.rest;

import java.io.StringReader;
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
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@RestController
@RequestMapping("/rest/notebook/audit-logs")
public class NotebookAuditLogRestController extends BaseRestController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private SystemUserService systemUserService;

    @GetMapping(value = "/notebook/{notebookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getNotebookAuditTrail(@PathVariable String notebookId) {
        try {
            ReferenceTables refTable = referenceTablesService.getReferenceTableByName("notebook");
            if (refTable == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getNotebookAuditTrail",
                        "Reference table 'notebook' not found");
                return ResponseEntity.ok(new ArrayList<>());
            }
            List<History> history = historyService.getHistoryByRefIdAndRefTableId(notebookId, refTable.getId());
            List<Map<String, Object>> auditLogs = history.stream().map(h -> transformHistoryToAuditLog(h, "NOTEBOOK"))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/instance/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getInstanceAuditTrail(@PathVariable String instanceId) {
        try {
            ReferenceTables refTable = referenceTablesService.getReferenceTableByName("notebook_instance");
            if (refTable == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getInstanceAuditTrail",
                        "Reference table 'notebook_instance' not found");
                return ResponseEntity.ok(new ArrayList<>());
            }
            List<History> history = historyService.getHistoryByRefIdAndRefTableId(instanceId, refTable.getId());
            List<Map<String, Object>> auditLogs = history.stream().map(h -> transformHistoryToAuditLog(h, "INSTANCE"))
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
        auditLog.put("activity", mapActivityCode(history.getActivity())); // I -> INSERT, U -> UPDATE, D -> DELETE

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
     * Maps single-letter activity codes to full names
     *
     * @param code Activity code (I, U, D)
     * @return Full activity name (INSERT, UPDATE, DELETE)
     */
    private String mapActivityCode(String code) {
        if (code == null) {
            return "UNKNOWN";
        }
        return switch (code) {
        case "I" -> "INSERT";
        case "U" -> "UPDATE";
        case "D" -> "DELETE";
        default -> code;
        };
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
            return switch (activity) {
            case "I" -> "Created new " + entityType.toLowerCase();
            case "U" -> "Updated " + entityType.toLowerCase() + " (no field details available)";
            case "D" -> "Deleted " + entityType.toLowerCase();
            default -> activity + " " + entityType.toLowerCase();
            };
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
            if (oldValue != null && newValue != null && !oldValue.isEmpty() && !newValue.isEmpty()) {
                changeSummaries.add(fieldLabel + ": " + truncate(oldValue, 20) + " → " + truncate(newValue, 20));
            } else if (newValue != null && !newValue.isEmpty()) {
                changeSummaries.add(fieldLabel + " set to " + truncate(newValue, 20));
            } else if (oldValue != null && !oldValue.isEmpty()) {
                changeSummaries.add(fieldLabel + " cleared");
            }
            count++;
        }

        return changeSummaries.isEmpty() ? "Updated " + entityType.toLowerCase() : String.join(", ", changeSummaries);
    }

    private String getKeyChangeDescription(Map<String, Map<String, String>> changes, String entityType) {
        if ("NOTEBOOK".equals(entityType)) {
            if (changes.containsKey("status")) {
                Map<String, String> statusChange = changes.get("status");
                String newStatus = statusChange.get("new");
                String oldStatus = statusChange.get("old");
                return "Status changed from " + oldStatus + " to " + newStatus;
            }
            if (changes.containsKey("title")) {
                Map<String, String> titleChange = changes.get("title");
                return "Title changed to " + titleChange.get("new");
            }
            if (changes.containsKey("type")) {
                Map<String, String> typeChange = changes.get("type");
                return "Type changed to " + typeChange.get("new");
            }
        }

        if ("INSTANCE".equals(entityType)) {
            if (changes.containsKey("status")) {
                Map<String, String> statusChange = changes.get("status");
                String newStatus = statusChange.get("new");
                String oldStatus = statusChange.get("old");
                return "Status changed from " + oldStatus + " to " + newStatus;
            }
            if (changes.containsKey("content")) {
                return "Content updated";
            }
            if (changes.containsKey("conclusionComments")) {
                return "Conclusion comments updated";
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
}
