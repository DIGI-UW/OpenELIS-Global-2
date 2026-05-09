package org.openelisglobal.audittrail.controller.rest;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class SystemAuditEventRestController {

    private static final int MAX_EXPORT_ROWS = 10000;

    private static final List<String> SYSTEM_ENTITY_TABLE_NAMES = Arrays.asList("TEST", "PANEL", "METHOD",
            "TEST_SECTION", "TYPE_OF_SAMPLE", "RESULT_LIMITS", "SYSTEM_USER", "SYSTEM_ROLE", "SYSTEM_USER_ROLE",
            "DICTIONARY", "DICTIONARY_CATEGORY", "analyzer", "site_information", "QA_EVENT", "ANALYSIS_QAEVENT",
            "ANALYSIS_QAEVENT_ACTION", "QA_OBSERVATION", "PATIENT");

    // Patient-scoped queries always require an entity type — front-end UX
    // should disable Search until a patient is selected, but the controller
    // double-checks below.
    private static final String PATIENT_ENTITY_NAME = "PATIENT";

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private SystemUserService systemUserService;

    // Cached at startup — reference table mappings rarely change
    private Map<String, String> refTableNameToId = Collections.emptyMap();
    private Map<String, String> refTableIdToName = Collections.emptyMap();

    @PostConstruct
    private void initRefTableCache() {
        Map<String, String> nameToId = new HashMap<>();
        Map<String, String> idToName = new HashMap<>();
        for (String tableName : SYSTEM_ENTITY_TABLE_NAMES) {
            ReferenceTables rt = referenceTablesService.getReferenceTableByName(tableName);
            if (rt != null) {
                nameToId.put(tableName, rt.getId());
                idToName.put(rt.getId(), tableName);
            }
        }
        this.refTableNameToId = Collections.unmodifiableMap(nameToId);
        this.refTableIdToName = Collections.unmodifiableMap(idToName);
    }

    @GetMapping("/rest/systemAuditEvents")
    public ResponseEntity<Map<String, Object>> getSystemAuditEvents(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityType, @RequestParam(required = false) String action,
            @RequestParam(required = false) String search, @RequestParam(required = false) String patientId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "30") int pageSize) {

        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        // Patient-scoped queries always filter on the PATIENT reference table,
        // regardless of what the dropdown sent — keeps the contract simple.
        String effectiveEntityType = patientId != null && !patientId.isEmpty() ? PATIENT_ENTITY_NAME : entityType;
        List<String> refTableIds = resolveReferenceTableIds(effectiveEntityType);

        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));

        List<History> events = historyService.getSystemEventHistory(start, end, userId, refTableIds, action, search,
                patientId, safePage, safePageSize);
        long totalItems = historyService.getSystemEventHistoryCount(start, end, userId, refTableIds, action, search,
                patientId);

        Map<String, String> userCache = new HashMap<>();

        List<Map<String, Object>> items = events.stream().map(h -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", h.getId());
            item.put("timestamp", h.getTimestamp());
            item.put("user", resolveUserName(h.getSysUserId(), userCache));
            item.put("entityType", refTableIdToName.getOrDefault(h.getReferenceTable(), h.getReferenceTable()));
            item.put("entityId", h.getReferenceId());
            item.put("action", mapActivity(h.getActivity()));
            item.put("changes", parseChanges(h));
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("events", items);
        response.put("page", safePage);
        response.put("pageSize", safePageSize);
        response.put("totalItems", totalItems);
        response.put("totalPages", (int) Math.ceil((double) totalItems / safePageSize));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rest/systemAuditEvents/entityTypes")
    public ResponseEntity<List<Map<String, String>>> getEntityTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        for (Map.Entry<String, String> entry : refTableNameToId.entrySet()) {
            Map<String, String> type = new LinkedHashMap<>();
            type.put("id", entry.getValue());
            type.put("name", entry.getKey());
            types.add(type);
        }
        return ResponseEntity.ok(types);
    }

    @GetMapping("/rest/systemAuditEvents/export")
    public void exportCsv(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityType, @RequestParam(required = false) String action,
            @RequestParam(required = false) String search, @RequestParam(required = false) String patientId,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"system-audit-events.csv\"");

        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        String effectiveEntityType = patientId != null && !patientId.isEmpty() ? PATIENT_ENTITY_NAME : entityType;
        List<String> refTableIds = resolveReferenceTableIds(effectiveEntityType);

        List<History> events = historyService.getSystemEventHistory(start, end, userId, refTableIds, action, search,
                patientId, 1, MAX_EXPORT_ROWS);
        // refTableIdToName is cached at startup via @PostConstruct
        Map<String, String> userCache = new HashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PrintWriter writer = response.getWriter();
        writer.printf("%s,%s,%s,%s,%s%n", MessageUtil.getMessage("auditTrail.export.header.timestamp"),
                MessageUtil.getMessage("auditTrail.export.header.user"),
                MessageUtil.getMessage("auditTrail.export.header.entityType"),
                MessageUtil.getMessage("auditTrail.export.header.entityId"),
                MessageUtil.getMessage("auditTrail.export.header.action"));
        for (History h : events) {
            writer.printf("%s,%s,%s,%s,%s%n", csvEscape(h.getTimestamp() != null ? sdf.format(h.getTimestamp()) : ""),
                    csvEscape(resolveUserName(h.getSysUserId(), userCache)),
                    csvEscape(refTableIdToName.getOrDefault(h.getReferenceTable(), h.getReferenceTable())),
                    csvEscape(h.getReferenceId()), csvEscape(mapActivity(h.getActivity())));
        }
        writer.flush();
    }

    @GetMapping("/rest/systemAuditEvents/exportPdf")
    public void exportPdf(@RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityType, @RequestParam(required = false) String action,
            @RequestParam(required = false) String search, @RequestParam(required = false) String patientId,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"system-audit-events.pdf\"");

        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        String effectiveEntityType = patientId != null && !patientId.isEmpty() ? PATIENT_ENTITY_NAME : entityType;
        List<String> refTableIds = resolveReferenceTableIds(effectiveEntityType);

        List<History> events = historyService.getSystemEventHistory(start, end, userId, refTableIds, action, search,
                patientId, 1, MAX_EXPORT_ROWS);
        // refTableIdToName is cached at startup via @PostConstruct
        Map<String, String> userCache = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);

            document.add(new Phrase(MessageUtil.getMessage("auditTrail.export.title.systemAuditEvents") + "\n\n",
                    titleFont));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 2.5f, 2.5f, 2f, 1.5f });

            String[] headers = { MessageUtil.getMessage("auditTrail.export.header.timestamp"),
                    MessageUtil.getMessage("auditTrail.export.header.user"),
                    MessageUtil.getMessage("auditTrail.export.header.entityType"),
                    MessageUtil.getMessage("auditTrail.export.header.entityId"),
                    MessageUtil.getMessage("auditTrail.export.header.action") };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(51, 102, 179));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (History h : events) {
                table.addCell(new Phrase(h.getTimestamp() != null ? sdf.format(h.getTimestamp()) : "", cellFont));
                table.addCell(new Phrase(resolveUserName(h.getSysUserId(), userCache), cellFont));
                table.addCell(new Phrase(refTableIdToName.getOrDefault(h.getReferenceTable(), h.getReferenceTable()),
                        cellFont));
                table.addCell(new Phrase(h.getReferenceId() != null ? h.getReferenceId() : "", cellFont));
                table.addCell(new Phrase(mapActivity(h.getActivity()), cellFont));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new IOException("Error generating PDF", e);
        }
    }

    private List<String> resolveReferenceTableIds(String entityType) {
        if (entityType == null || entityType.isEmpty()) {
            return new ArrayList<>(refTableNameToId.values());
        }
        List<String> ids = new ArrayList<>();
        for (String name : entityType.split(",")) {
            String id = refTableNameToId.get(name.trim());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Parse the XML-encoded changes blob into a map of field name → new value. The
     * format is: {@code <fieldName>value</fieldName>\n} per changed field.
     */
    private Map<String, String> parseChanges(History history) {
        if (history.getChanges() == null || "I".equals(history.getActivity())) {
            return Collections.emptyMap();
        }
        try {
            String xml = new String(history.getChanges(), StandardCharsets.UTF_8);
            Map<String, String> changes = new LinkedHashMap<>();
            int pos = 0;
            while (pos < xml.length()) {
                int openStart = xml.indexOf('<', pos);
                if (openStart < 0)
                    break;
                int openEnd = xml.indexOf('>', openStart);
                if (openEnd < 0)
                    break;
                String tag = xml.substring(openStart + 1, openEnd);
                if (tag.startsWith("/")) {
                    pos = openEnd + 1;
                    continue;
                }
                String closeTag = "</" + tag + ">";
                int closeStart = xml.indexOf(closeTag, openEnd);
                if (closeStart < 0) {
                    pos = openEnd + 1;
                    continue;
                }
                String value = xml.substring(openEnd + 1, closeStart);
                changes.put(tag, value);
                pos = closeStart + closeTag.length();
            }
            return changes;
        } catch (Exception e) {
            LogEvent.logWarn("SystemAuditEventRestController", "parseChanges",
                    "Failed to parse changes for history " + history.getId());
            return Collections.emptyMap();
        }
    }

    private String resolveUserName(String sysUserId, Map<String, String> cache) {
        if (sysUserId == null || sysUserId.isEmpty()) {
            return "";
        }
        return cache.computeIfAbsent(sysUserId, id -> {
            SystemUser user = systemUserService.getUserById(id);
            if (user != null) {
                String first = user.getFirstName() != null ? user.getFirstName() : "";
                String last = user.getLastName() != null ? user.getLastName() : "";
                return (first + " " + last).trim();
            }
            return id;
        });
    }

    private String mapActivity(String activity) {
        if ("I".equals(activity))
            return MessageUtil.getMessage("auditTrail.activity.insert");
        if ("U".equals(activity))
            return MessageUtil.getMessage("auditTrail.activity.update");
        if ("D".equals(activity))
            return MessageUtil.getMessage("auditTrail.activity.delete");
        return activity;
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        // Prevent CSV formula injection (CWE-1236): prefix dangerous leading chars
        if (!value.isEmpty()) {
            char first = value.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                value = "'" + value;
            }
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Timestamp parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return null;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return Timestamp.from(date.atStartOfDay(ZoneId.of("UTC")).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            LogEvent.logWarn("SystemAuditEventRestController", "parseStartDate",
                    "Invalid startDate format: " + dateStr);
            return null;
        }
    }

    private Timestamp parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return null;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return Timestamp.from(date.atTime(LocalTime.MAX).atZone(ZoneId.of("UTC")).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            LogEvent.logWarn("SystemAuditEventRestController", "parseEndDate", "Invalid endDate format: " + dateStr);
            return null;
        }
    }
}
