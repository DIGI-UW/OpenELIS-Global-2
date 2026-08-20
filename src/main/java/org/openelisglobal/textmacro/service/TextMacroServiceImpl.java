package org.openelisglobal.textmacro.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.textmacro.dao.TextMacroDAO;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroBulkResultForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.form.TextMacroSummaryForm;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.openelisglobal.textmacro.valueholder.TextMacroContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TextMacroServiceImpl implements TextMacroService {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\.[a-z0-9][a-z0-9_-]{0,62}");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "ALL");
    private static final Set<String> SORTS = Set.of("code:asc", "code:desc", "updated:asc", "updated:desc");
    private static final Set<Integer> PAGE_SIZES = Set.of(10, 20, 50, 100);
    private static final Set<String> BULK_ACTIONS = Set.of("ACTIVATE", "DEACTIVATE", "DELETE_LOCAL");
    private static final int MAX_BULK_SELECTION = 100;
    private static final String[] CSV_HEADERS = { "code", "expansion_text", "contexts", "active", "provenance",
            "source_key", "source_version" };

    private final TextMacroDAO macroDAO;
    private final AuditTrailService auditTrailService;

    public TextMacroServiceImpl(TextMacroDAO macroDAO, AuditTrailService auditTrailService) {
        this.macroDAO = macroDAO;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TextMacroSummaryForm> findActive(String context, String query, int limit) {
        TextMacroContext normalizedContext = parseContext(context);
        String normalizedQuery = normalizeSearch(query);
        int normalizedLimit = Math.min(50, Math.max(1, limit <= 0 ? 20 : limit));
        return macroDAO.findActiveByContext(normalizedContext, normalizedQuery, normalizedLimit).stream()
                .map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TextMacroPageForm searchAdmin(TextMacroAdminQueryForm request) {
        TextMacroAdminQueryForm query = request == null ? new TextMacroAdminQueryForm() : request;
        String search = normalizeSearch(query.q);
        TextMacroContext context = parseOptionalContext(query.context);
        String status = normalizeStatus(query.status);
        String sort = SORTS.contains(query.sort) ? query.sort : "code:asc";
        int page = Math.max(1, query.page);
        int pageSize = PAGE_SIZES.contains(query.pageSize) ? query.pageSize : 20;
        int offset = (page - 1) * pageSize;

        TextMacroPageForm result = new TextMacroPageForm();
        result.items = macroDAO.search(search, context, status, sort, offset, pageSize).stream().map(this::toAdmin)
                .toList();
        result.page = page;
        result.pageSize = pageSize;
        result.total = macroDAO.countSearch(search, context, status);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TextMacroAdminForm getAdmin(String id) {
        return toAdmin(requireExisting(id));
    }

    @Override
    @Transactional
    public TextMacroAdminForm save(String id, TextMacroAdminForm request, String actorId) {
        String actor = requireText(actorId, "AUTHENTICATED_ACTOR_REQUIRED", "Authenticated actor is required");
        if (request == null) {
            throw new TextMacroRequestException("INVALID_MACRO_REQUEST", "Macro is required");
        }
        String code = normalizeCode(request.code);
        String expansion = requireText(request.expansionText, "MACRO_TEXT_REQUIRED", "Macro text is required");
        EnumSet<TextMacroContext> contexts = parseContexts(request.contexts);
        Optional<TextMacro> duplicate = macroDAO.findByCode(code);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new TextMacroConflictException("Macro code is already in use");
        }

        TextMacro macro = id == null ? new TextMacro() : requireExisting(id);
        macro.setCode(code);
        macro.setExpansionText(expansion);
        macro.setContexts(contexts);
        macro.setActive(request.active);
        macro.setLastUpdatedBy(actor);
        if (id == null) {
            macro.setProvenance("LOCAL");
            macroDAO.insert(macro);
        } else {
            macroDAO.update(macro);
        }
        return toAdmin(macro);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCsv() {
        List<TextMacro> macros = macroDAO.findAllWithContexts().stream()
                .sorted(java.util.Comparator.comparing(TextMacro::getCode)).toList();
        try (StringWriter writer = new StringWriter();
                CSVPrinter printer = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).setRecordSeparator("\r\n").build())) {
            for (TextMacro macro : macros) {
                String contexts = macro.getContexts().stream().map(Enum::name).sorted()
                        .collect(java.util.stream.Collectors.joining("|"));
                printer.printRecord(macro.getCode(), macro.getExpansionText(), contexts, macro.isActive(),
                        macro.getProvenance(), nullToEmpty(macro.getSourceKey()),
                        nullToEmpty(macro.getSourceVersion()));
            }
            printer.flush();
            return writer.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to export text macros", exception);
        }
    }

    @Override
    @Transactional
    public TextMacroBulkResultForm bulk(TextMacroBulkRequestForm request, String actorId) {
        String actor = requireText(actorId, "AUTHENTICATED_ACTOR_REQUIRED", "Authenticated actor is required");
        if (request == null) {
            throw new TextMacroRequestException("INVALID_MACRO_REQUEST", "Bulk request is required");
        }
        List<String> requestedIds = normalizeBulkIds(request.ids);
        String action = normalizeBulkAction(request.action);
        Set<String> uniqueIds = new LinkedHashSet<>(requestedIds);
        List<TextMacro> macros = macroDAO.findByIdsWithContexts(uniqueIds).stream()
                .sorted(java.util.Comparator.comparing(TextMacro::getCode)).toList();
        if (macros.size() != uniqueIds.size()) {
            throw new TextMacroRequestException("MACRO_NOT_FOUND", "One or more selected macros were not found");
        }
        if ("DELETE_LOCAL".equals(action)
                && macros.stream().anyMatch(macro -> !"LOCAL".equalsIgnoreCase(macro.getProvenance()))) {
            throw new TextMacroRequestException("PACKAGED_MACRO_REMOVAL_NOT_ALLOWED",
                    "Packaged macros cannot be removed");
        }

        for (TextMacro macro : macros) {
            if ("DELETE_LOCAL".equals(action)) {
                macro.setSysUserId(actor);
                auditTrailService.saveHistory(null, macro, actor, IActionConstants.AUDIT_TRAIL_DELETE,
                        macroDAO.getTableName());
                macroDAO.delete(macro);
            } else {
                macro.setActive("ACTIVATE".equals(action));
                macro.setLastUpdatedBy(actor);
                macroDAO.update(macro);
            }
        }

        TextMacroBulkResultForm result = new TextMacroBulkResultForm();
        result.action = action;
        result.affectedCount = macros.size();
        result.affectedCodes = macros.stream().map(TextMacro::getCode).toList();
        return result;
    }

    private TextMacro requireExisting(String id) {
        return macroDAO.get(id)
                .orElseThrow(() -> new TextMacroRequestException("MACRO_NOT_FOUND", "Macro not found: " + id));
    }

    private String normalizeCode(String input) {
        String value = requireText(input, "INVALID_MACRO_CODE", "Macro code is required").toLowerCase(Locale.ROOT);
        if (!value.startsWith(".")) {
            value = "." + value;
        }
        if (!CODE_PATTERN.matcher(value).matches()) {
            throw new TextMacroRequestException("INVALID_MACRO_CODE",
                    "Macro code must start with a dot and contain letters, numbers, _ or -");
        }
        return value;
    }

    private EnumSet<TextMacroContext> parseContexts(Set<String> values) {
        if (values == null || values.isEmpty()) {
            throw new TextMacroRequestException("MACRO_CONTEXT_REQUIRED", "At least one macro context is required");
        }
        EnumSet<TextMacroContext> contexts = EnumSet.noneOf(TextMacroContext.class);
        for (String value : values) {
            contexts.add(parseContext(value));
        }
        return contexts;
    }

    private TextMacroContext parseOptionalContext(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return parseContext(value);
    }

    private TextMacroContext parseContext(String value) {
        String normalized = requireText(value, "MACRO_CONTEXT_REQUIRED", "Macro context is required");
        try {
            return TextMacroContext.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new TextMacroRequestException("INVALID_MACRO_CONTEXT", "Unsupported macro context: " + value);
        }
    }

    private String normalizeStatus(String value) {
        String status = value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new TextMacroRequestException("INVALID_MACRO_STATUS", "Unsupported macro status: " + value);
        }
        return status;
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> normalizeBulkIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new TextMacroRequestException("MACRO_SELECTION_REQUIRED", "Select at least one macro");
        }
        if (values.size() > MAX_BULK_SELECTION) {
            throw new TextMacroRequestException("MACRO_SELECTION_LIMIT_EXCEEDED",
                    "No more than " + MAX_BULK_SELECTION + " macros can be changed at once");
        }
        List<String> ids = values.stream()
                .map(value -> requireText(value, "MACRO_NOT_FOUND", "Selected macro ID is required")).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new TextMacroRequestException("DUPLICATE_MACRO_IDS", "Selected macro IDs must be unique");
        }
        return ids;
    }

    private String normalizeBulkAction(String value) {
        String action = requireText(value, "INVALID_MACRO_BULK_ACTION", "Bulk action is required")
                .toUpperCase(Locale.ROOT);
        if (!BULK_ACTIONS.contains(action)) {
            throw new TextMacroRequestException("INVALID_MACRO_BULK_ACTION", "Unsupported macro bulk action: " + value);
        }
        return action;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String requireText(String value, String code, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new TextMacroRequestException(code, message);
        }
        return value.trim();
    }

    private TextMacroSummaryForm toSummary(TextMacro macro) {
        TextMacroSummaryForm form = new TextMacroSummaryForm();
        form.id = macro.getId();
        form.code = macro.getCode();
        form.expansionText = macro.getExpansionText();
        form.contexts = macro.getContexts().stream().map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return form;
    }

    private TextMacroAdminForm toAdmin(TextMacro macro) {
        TextMacroAdminForm form = new TextMacroAdminForm();
        form.id = macro.getId();
        form.code = macro.getCode();
        form.expansionText = macro.getExpansionText();
        form.contexts = macro.getContexts().stream().map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        form.active = macro.isActive();
        form.provenance = macro.getProvenance();
        form.sourceKey = macro.getSourceKey();
        form.sourceVersion = macro.getSourceVersion();
        form.lastupdated = macro.getLastupdated();
        return form;
    }
}
