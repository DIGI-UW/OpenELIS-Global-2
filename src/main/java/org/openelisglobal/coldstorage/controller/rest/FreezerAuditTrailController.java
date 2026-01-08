package org.openelisglobal.coldstorage.controller.rest;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.coldstorage.service.CorrectiveActionService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * REST controller for freezer audit trail using OpenELIS history table.
 * Replaces custom audit trail implementation with standard OpenELIS audit
 * infrastructure.
 */
@RestController
@RequestMapping("/rest/coldstorage/audit-trail")
public class FreezerAuditTrailController extends BaseRestController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private CorrectiveActionService correctiveActionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAuditTrail(@RequestParam(required = false) Long freezerId,
            @RequestParam(required = false) String start, @RequestParam(required = false) String end) {

        List<Map<String, Object>> auditEvents = new ArrayList<>();

        try {
            // Parse dates if provided, otherwise use null (fetch all)
            OffsetDateTime startDateTime = start != null ? OffsetDateTime.parse(start) : null;
            OffsetDateTime endDateTime = end != null ? OffsetDateTime.parse(end) : null;

            List<Freezer> freezersToCheck;
            if (freezerId != null) {
                Freezer freezer = freezerService.findById(freezerId).orElse(null);
                freezersToCheck = freezer != null ? List.of(freezer) : List.of();
            } else {
                freezersToCheck = freezerService.getAllFreezers("");
            }

            // Get reference table IDs
            ReferenceTables freezerTable = referenceTablesService.getReferenceTableByName("FREEZER");
            ReferenceTables correctiveActionTable = referenceTablesService.getReferenceTableByName("CORRECTIVE_ACTION");
            ReferenceTables alertTable = referenceTablesService.getReferenceTableByName("ALERT");

            for (Freezer freezer : freezersToCheck) {
                Long fId = freezer.getId();
                String freezerName = freezer.getName() != null ? freezer.getName() : "Freezer " + fId;

                // 1. Get Freezer configuration changes from history table
                if (freezerTable != null) {
                    List<History> freezerHistory = historyService.getHistoryByRefIdAndRefTableId(String.valueOf(fId),
                            freezerTable.getId());

                    for (History history : freezerHistory) {
                        if (history.getTimestamp() != null) {
                            OffsetDateTime historyTime = history.getTimestamp().toInstant().atOffset(
                                    ZoneId.systemDefault().getRules().getOffset(history.getTimestamp().toInstant()));

                            // Apply date filter only if dates are provided
                            if (startDateTime != null && endDateTime != null) {
                                if (historyTime.isBefore(startDateTime) || historyTime.isAfter(endDateTime)) {
                                    continue;
                                }
                            }

                            Map<String, Object> event = createConfigurationChangeEvent(history, freezer, freezerName);
                            if (event != null) {
                                auditEvents.add(event);
                            }
                        }
                    }
                }

                // 2. Get Alert acknowledgments and resolutions
                List<Alert> alerts = alertService.getAlertsByEntity("Freezer", fId);
                for (Alert alert : alerts) {
                    // Alert acknowledgment event
                    if (alert.getAcknowledgedAt() != null) {
                        OffsetDateTime ackTime = alert.getAcknowledgedAt();
                        // Apply date filter only if dates are provided
                        boolean includeAck = (startDateTime == null && endDateTime == null)
                                || (!ackTime.isBefore(startDateTime) && !ackTime.isAfter(endDateTime));

                        if (includeAck) {
                            Map<String, Object> ackEvent = new HashMap<>();
                            ackEvent.put("id", "ACK-" + alert.getId());
                            ackEvent.put("freezerId", String.valueOf(fId));
                            ackEvent.put("freezerName", freezerName);
                            ackEvent.put("actionType", "ALERT_ACKNOWLEDGED");
                            ackEvent.put("performedAt",
                                    ackTime.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
                            ackEvent.put("performedBy", getUserName(alert.getAcknowledgedBy()));
                            ackEvent.put("comment", "Temperature excursion alert for " + freezerName + " acknowledged");
                            ackEvent.put("details", alert.getMessage());
                            auditEvents.add(ackEvent);
                        }
                    }

                    // Alert resolution event
                    if (alert.getResolvedAt() != null) {
                        OffsetDateTime resolveTime = alert.getResolvedAt();
                        // Apply date filter only if dates are provided
                        boolean includeResolve = (startDateTime == null && endDateTime == null)
                                || (!resolveTime.isBefore(startDateTime) && !resolveTime.isAfter(endDateTime));

                        if (includeResolve) {
                            Map<String, Object> resolveEvent = new HashMap<>();
                            resolveEvent.put("id", "RESOLVE-" + alert.getId());
                            resolveEvent.put("freezerId", String.valueOf(fId));
                            resolveEvent.put("freezerName", freezerName);
                            resolveEvent.put("actionType", "CRITICAL_ALERT_RESOLVED");
                            resolveEvent.put("performedAt",
                                    resolveTime.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));
                            resolveEvent.put("performedBy", getUserName(alert.getResolvedBy()));
                            resolveEvent.put("comment", "Temperature excursion for " + freezerName + " resolved");
                            resolveEvent.put("details", alert.getResolutionNotes());
                            auditEvents.add(resolveEvent);
                        }
                    }
                }

                // 3. Get Corrective Actions from history table
                if (correctiveActionTable != null) {
                    List<CorrectiveAction> actions = correctiveActionService.getCorrectiveActionsByFreezerId(fId);

                    for (CorrectiveAction action : actions) {
                        if (action.getCreatedAt() != null) {
                            OffsetDateTime actionTime = action.getCreatedAt();
                            // Apply date filter only if dates are provided
                            if (startDateTime != null && endDateTime != null) {
                                if (actionTime.isBefore(startDateTime) || actionTime.isAfter(endDateTime)) {
                                    continue;
                                }
                            }

                            Map<String, Object> actionEvent = new HashMap<>();
                            actionEvent.put("id", "CA-" + action.getId());
                            actionEvent.put("freezerId", String.valueOf(fId));
                            actionEvent.put("freezerName", freezerName);
                            actionEvent.put("actionType", "CORRECTIVE_ACTION_LOGGED");
                            actionEvent.put("performedAt",
                                    actionTime.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER));

                            String performedBy = "Unknown";
                            try {
                                if (action.getCreatedBy() != null) {
                                    performedBy = action.getCreatedBy().getLoginName();
                                }
                            } catch (Exception e) {
                                performedBy = "Unknown";
                            }
                            actionEvent.put("performedBy", performedBy);
                            actionEvent.put("comment", action.getDescription());
                            actionEvent.put("details", buildCorrectiveActionDetails(action));
                            auditEvents.add(actionEvent);
                        }
                    }
                }
            }

            // Sort by timestamp descending (most recent first)
            auditEvents.sort(Comparator.comparing((Map<String, Object> e) -> (String) e.get("performedAt")).reversed());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(auditEvents);
    }

    private Map<String, Object> createConfigurationChangeEvent(History history, Freezer freezer, String freezerName) {
        try {
            if (history.getChanges() == null || history.getChanges().length == 0) {
                return null;
            }

            String xmlChanges = new String(history.getChanges());
            Map<String, String> changes = parseXmlChanges(xmlChanges);

            if (changes.isEmpty()) {
                return null;
            }

            Map<String, Object> event = new HashMap<>();
            event.put("id", "CONFIG-" + history.getId());
            event.put("freezerId", String.valueOf(freezer.getId()));
            event.put("freezerName", freezerName);

            // Determine action type based on changes
            String actionType = determineActionType(changes);
            event.put("actionType", actionType);

            event.put("performedAt",
                    history.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER));

            String performedBy = getUserName(Integer.parseInt(history.getSysUserId()));
            event.put("performedBy", performedBy);

            String comment = buildChangeDescription(changes, freezerName);
            event.put("comment", comment);
            event.put("details", xmlChanges);

            return event;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> parseXmlChanges(String xml) {
        Map<String, String> changes = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader("<changes>" + xml + "</changes>")));

            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element) {
                    Element element = (Element) nodes.item(i);
                    String key = element.getTagName();
                    String value = element.getTextContent();
                    changes.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return changes;
    }

    private String determineActionType(Map<String, String> changes) {
        if (changes.containsKey("warningThreshold") || changes.containsKey("criticalThreshold")
                || changes.containsKey("targetTemperature")) {
            return "THRESHOLD_UPDATED";
        } else if (changes.containsKey("name")) {
            return "FREEZER_RENAMED";
        } else if (changes.containsKey("active")) {
            return "FREEZER_STATUS_CHANGED";
        } else {
            return "CONFIGURATION_UPDATED";
        }
    }

    private String buildChangeDescription(Map<String, String> changes, String freezerName) {
        StringBuilder desc = new StringBuilder();

        if (changes.containsKey("warningThreshold")) {
            desc.append("Warning threshold changed to ").append(changes.get("warningThreshold")).append("°C");
        } else if (changes.containsKey("criticalThreshold")) {
            desc.append("Critical threshold changed to ").append(changes.get("criticalThreshold")).append("°C");
        } else if (changes.containsKey("targetTemperature")) {
            desc.append("Target temperature changed to ").append(changes.get("targetTemperature")).append("°C");
        } else if (changes.containsKey("name")) {
            desc.append("Freezer renamed to ").append(changes.get("name"));
        } else if (changes.containsKey("active")) {
            boolean isActive = Boolean.parseBoolean(changes.get("active"));
            desc.append("Freezer ").append(isActive ? "activated" : "deactivated");
        } else {
            desc.append("Configuration updated for ").append(freezerName);
        }

        return desc.toString();
    }

    private String buildCorrectiveActionDetails(CorrectiveAction action) {
        StringBuilder details = new StringBuilder();
        details.append("Action Type: ").append(action.getActionType()).append("\n");
        details.append("Status: ").append(action.getStatus()).append("\n");

        if (action.getCompletedAt() != null) {
            details.append("Completed: ")
                    .append(action.getCompletedAt().atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMATTER))
                    .append("\n");
        }

        if (action.getCompletionNotes() != null) {
            details.append("Completion Notes: ").append(action.getCompletionNotes()).append("\n");
        }

        return details.toString();
    }

    private String getUserName(SystemUser user) {
        if (user == null) {
            return "System";
        }
        try {
            String loginName = user.getLoginName();
            return loginName != null ? loginName : "Unknown";
        } catch (org.hibernate.LazyInitializationException e) {
            // User proxy couldn't be initialized, try fetching by ID
            try {
                if (user.getId() != null) {
                    SystemUser fetchedUser = systemUserService.get(user.getId().toString());
                    if (fetchedUser != null && fetchedUser.getLoginName() != null) {
                        return fetchedUser.getLoginName();
                    }
                }
            } catch (Exception ex) {
                // Fallback to Unknown
            }
            return "Unknown";
        }
    }

    private String getUserName(Integer userId) {
        if (userId == null) {
            return "System";
        }
        try {
            SystemUser user = systemUserService.get(String.valueOf(userId));
            return getUserName(user);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
