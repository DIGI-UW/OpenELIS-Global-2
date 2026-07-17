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
import org.openelisglobal.audittrail.service.AuditEntitySnapshotService;
import org.openelisglobal.audittrail.util.AuditFieldStringifier;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
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
            "ANALYSIS_QAEVENT_ACTION", "QA_OBSERVATION", "PATIENT", "PERSON");

    private static final String PATIENT_ENTITY_NAME = "PATIENT";
    private static final String PERSON_ENTITY_NAME = "PERSON";

    private static final int MAX_PATIENT_SCOPED_HISTORY = 10000;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private AuditEntitySnapshotService snapshotService;

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
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));

        List<History> events;
        long totalItems;

        if (patientId != null && !patientId.isEmpty()) {
            List<History> merged = fetchPatientScopedHistory(start, end, userId, action, search, patientId);
            totalItems = merged.size();
            int from = Math.min((safePage - 1) * safePageSize, merged.size());
            int to = Math.min(from + safePageSize, merged.size());
            events = merged.subList(from, to);
        } else {
            List<String> refTableIds = resolveReferenceTableIds(entityType);
            events = historyService.getSystemEventHistory(start, end, userId, refTableIds, action, search, null,
                    safePage, safePageSize);
            totalItems = historyService.getSystemEventHistoryCount(start, end, userId, refTableIds, action, search,
                    null);
        }

        Map<String, String> userCache = new HashMap<>();
        List<Map<String, Object>> items = buildItemsWithOldNew(events, patientId, userCache);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("events", items);
        response.put("page", safePage);
        response.put("pageSize", safePageSize);
        response.put("totalItems", totalItems);
        response.put("totalPages", (int) Math.ceil((double) totalItems / safePageSize));

        return ResponseEntity.ok(response);
    }

    /**
     * Pulls history for a single patient by merging rows from the PATIENT table
     * (keyed by patientId) with rows from the PERSON table (keyed by the linked
     * personId). Both ranges are fetched independently then sorted by timestamp
     * desc; pagination is applied in-memory by the caller. Cap is
     * {@link #MAX_PATIENT_SCOPED_HISTORY} per source.
     */
    private List<History> fetchPatientScopedHistory(Timestamp start, Timestamp end, String userId, String action,
            String search, String patientId) {
        List<History> merged = new ArrayList<>();

        String patientRefTableId = refTableNameToId.get(PATIENT_ENTITY_NAME);
        if (patientRefTableId != null) {
            merged.addAll(historyService.getSystemEventHistory(start, end, userId,
                    Collections.singletonList(patientRefTableId), action, search, patientId, 1,
                    MAX_PATIENT_SCOPED_HISTORY));
        }

        String personRefTableId = refTableNameToId.get(PERSON_ENTITY_NAME);
        Patient patient = patientService.get(patientId);
        String personId = patient != null && patient.getPerson() != null ? patient.getPerson().getId() : null;
        if (personRefTableId != null && personId != null) {
            merged.addAll(historyService.getSystemEventHistory(start, end, userId,
                    Collections.singletonList(personRefTableId), action, search, personId, 1,
                    MAX_PATIENT_SCOPED_HISTORY));
        }

        merged.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return merged;
    }

    /**
     * Walks {@code events} (already sorted newest-first) and produces UI-ready rows
     * whose {@code changes} entries each carry both an {@code old} and a
     * {@code new} value per field. The newest occurrence of each (entity, field) is
     * paired with the entity's current persisted value when available
     * (patient-scoped queries pre-load the patient + linked person); older
     * occurrences chain to the next-newer row's old value for the same field.
     */
    private List<Map<String, Object>> buildItemsWithOldNew(List<History> events, String patientId,
            Map<String, String> userCache) {
        Map<String, Map<String, String>> lastKnownNewByEntity = new HashMap<>();
        if (patientId != null && !patientId.isEmpty()) {
            Patient patient = patientService.get(patientId);
            if (patient != null) {
                String patientRefTableId = refTableNameToId.get(PATIENT_ENTITY_NAME);
                if (patientRefTableId != null) {
                    lastKnownNewByEntity.put(entityKey(patientRefTableId, patient.getId()),
                            currentValuesForPatient(patient));
                }
                Person person = patient.getPerson();
                String personRefTableId = refTableNameToId.get(PERSON_ENTITY_NAME);
                if (person != null && personRefTableId != null) {
                    lastKnownNewByEntity.put(entityKey(personRefTableId, person.getId()),
                            currentValuesForPerson(person));
                }
            }
        }

        // Pre-parse changes once so we know which fields to fetch from each entity.
        Map<History, Map<String, String>> parsedByHistory = new LinkedHashMap<>();
        Map<String, java.util.Set<String>> fieldsByEntityKey = new HashMap<>();
        for (History h : events) {
            Map<String, String> parsed = parseChanges(h);
            parsedByHistory.put(h, parsed);
            if (!parsed.isEmpty()) {
                fieldsByEntityKey.computeIfAbsent(entityKey(h.getReferenceTable(), h.getReferenceId()),
                        k -> new java.util.HashSet<>()).addAll(parsed.keySet());
            }
        }
        for (Map.Entry<String, java.util.Set<String>> entry : fieldsByEntityKey.entrySet()) {
            if (lastKnownNewByEntity.containsKey(entry.getKey())) {
                continue;
            }
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            Map<String, String> current = snapshotService.loadFieldValues(refTableIdToName.get(parts[0]), parts[1],
                    entry.getValue());
            if (!current.isEmpty()) {
                lastKnownNewByEntity.put(entry.getKey(), current);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (History h : events) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", h.getId());
            item.put("timestamp", h.getTimestamp());
            item.put("user", resolveUserName(h.getSysUserId(), userCache));
            item.put("entityType", refTableIdToName.getOrDefault(h.getReferenceTable(), h.getReferenceTable()));
            item.put("entityId", h.getReferenceId());
            item.put("action", mapActivity(h.getActivity()));

            Map<String, String> oldByField = parsedByHistory.getOrDefault(h, Collections.emptyMap());
            String key = entityKey(h.getReferenceTable(), h.getReferenceId());
            Map<String, String> entityCurrent = lastKnownNewByEntity.computeIfAbsent(key, k -> new HashMap<>());
            Map<String, Map<String, String>> changesWithBoth = new LinkedHashMap<>();
            if (oldByField.isEmpty() && "U".equals(h.getActivity())) {
                // Empty-changes Update: the audit XML didn't record any per-field
                // diff (typically a Hibernate ghost-flush or fields whose pre-update
                // values were already blank). Fall back to showing the entity's
                // current state so the row carries some signal beyond "Updated".
                Map<String, String> snapshot = snapshotService.loadSnapshot(refTableIdToName.get(h.getReferenceTable()),
                        h.getReferenceId());
                for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                    Map<String, String> pair = new LinkedHashMap<>();
                    pair.put("old", "");
                    pair.put("new", entry.getValue());
                    changesWithBoth.put(entry.getKey(), pair);
                }
            } else {
                for (Map.Entry<String, String> entry : oldByField.entrySet()) {
                    String field = entry.getKey();
                    String oldVal = AuditFieldStringifier.sanitize(entry.getValue());
                    String newVal = entityCurrent.getOrDefault(field, "");
                    Map<String, String> pair = new LinkedHashMap<>();
                    pair.put("old", oldVal);
                    pair.put("new", newVal);
                    changesWithBoth.put(field, pair);
                    entityCurrent.put(field, oldVal);
                }
            }
            item.put("changes", changesWithBoth);
            items.add(item);
        }
        return items;
    }

    private String entityKey(String refTableId, String refId) {
        return (refTableId == null ? "" : refTableId) + ":" + (refId == null ? "" : refId);
    }

    private Map<String, String> currentValuesForPatient(Patient p) {
        Map<String, String> values = new HashMap<>();
        putIfPresent(values, "gender", p.getGender());
        putIfPresent(values, "nationalId", p.getNationalId());
        putIfPresent(values, "externalId", p.getExternalId());
        putIfPresent(values, "birthDateForDisplay", p.getBirthDateForDisplay());
        return values;
    }

    private Map<String, String> currentValuesForPerson(Person p) {
        Map<String, String> values = new HashMap<>();
        putIfPresent(values, "firstName", p.getFirstName());
        putIfPresent(values, "lastName", p.getLastName());
        putIfPresent(values, "email", p.getEmail());
        putIfPresent(values, "primaryPhone", p.getPrimaryPhone());
        return values;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
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
        List<History> events;
        if (patientId != null && !patientId.isEmpty()) {
            List<History> merged = fetchPatientScopedHistory(start, end, userId, action, search, patientId);
            events = merged.size() > MAX_EXPORT_ROWS ? merged.subList(0, MAX_EXPORT_ROWS) : merged;
        } else {
            events = historyService.getSystemEventHistory(start, end, userId, resolveReferenceTableIds(entityType),
                    action, search, null, 1, MAX_EXPORT_ROWS);
        }
        Map<String, String> userCache = new HashMap<>();
        List<Map<String, Object>> items = buildItemsWithOldNew(events, patientId, userCache);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PrintWriter writer = response.getWriter();
        writer.printf("%s,%s,%s,%s,%s,%s,%s%n", MessageUtil.getMessage("auditTrail.export.header.timestamp"),
                MessageUtil.getMessage("auditTrail.export.header.user"),
                MessageUtil.getMessage("auditTrail.export.header.entityType"),
                MessageUtil.getMessage("auditTrail.export.header.entityId"),
                MessageUtil.getMessage("auditTrail.export.header.action"),
                MessageUtil.getMessage("auditTrail.export.header.oldValue"),
                MessageUtil.getMessage("auditTrail.export.header.newValue"));
        for (Map<String, Object> item : items) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> changes = (Map<String, Map<String, String>>) item.get("changes");
            Timestamp ts = (Timestamp) item.get("timestamp");
            writer.printf("%s,%s,%s,%s,%s,%s,%s%n", csvEscape(ts != null ? sdf.format(ts) : ""),
                    csvEscape((String) item.get("user")), csvEscape((String) item.get("entityType")),
                    csvEscape((String) item.get("entityId")), csvEscape((String) item.get("action")),
                    csvEscape(formatChangesColumn(changes, "old")), csvEscape(formatChangesColumn(changes, "new")));
        }
        writer.flush();
    }

    /**
     * Renders the per-field changes map into a single comma-separated string
     * suitable for one cell of a CSV or PDF export. {@code which} selects which
     * side of each pair to read — {@code "old"} or {@code "new"}; fields whose
     * value on that side is blank are skipped, so the cell only carries the column
     * it actually has data for.
     */
    private String formatChangesColumn(Map<String, Map<String, String>> changes, String which) {
        if (changes == null || changes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> entry : changes.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().get(which);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ").append(value);
        }
        return sb.toString();
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
        List<History> events;
        if (patientId != null && !patientId.isEmpty()) {
            List<History> merged = fetchPatientScopedHistory(start, end, userId, action, search, patientId);
            events = merged.size() > MAX_EXPORT_ROWS ? merged.subList(0, MAX_EXPORT_ROWS) : merged;
        } else {
            events = historyService.getSystemEventHistory(start, end, userId, resolveReferenceTableIds(entityType),
                    action, search, null, 1, MAX_EXPORT_ROWS);
        }
        Map<String, String> userCache = new HashMap<>();
        List<Map<String, Object>> items = buildItemsWithOldNew(events, patientId, userCache);
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

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2.6f, 2f, 2f, 1.4f, 1.2f, 3.4f, 3.4f });

            String[] headers = { MessageUtil.getMessage("auditTrail.export.header.timestamp"),
                    MessageUtil.getMessage("auditTrail.export.header.user"),
                    MessageUtil.getMessage("auditTrail.export.header.entityType"),
                    MessageUtil.getMessage("auditTrail.export.header.entityId"),
                    MessageUtil.getMessage("auditTrail.export.header.action"),
                    MessageUtil.getMessage("auditTrail.export.header.oldValue"),
                    MessageUtil.getMessage("auditTrail.export.header.newValue") };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(51, 102, 179));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Map<String, Object> item : items) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> changes = (Map<String, Map<String, String>>) item.get("changes");
                Timestamp ts = (Timestamp) item.get("timestamp");
                table.addCell(new Phrase(ts != null ? sdf.format(ts) : "", cellFont));
                table.addCell(new Phrase((String) item.get("user"), cellFont));
                table.addCell(new Phrase((String) item.get("entityType"), cellFont));
                String entityId = (String) item.get("entityId");
                table.addCell(new Phrase(entityId == null ? "" : entityId, cellFont));
                table.addCell(new Phrase((String) item.get("action"), cellFont));
                table.addCell(new Phrase(formatChangesColumn(changes, "old"), cellFont));
                table.addCell(new Phrase(formatChangesColumn(changes, "new"), cellFont));
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
