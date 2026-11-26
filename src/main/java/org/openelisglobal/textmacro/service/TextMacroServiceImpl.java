package org.openelisglobal.textmacro.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.textmacro.dao.TextMacroDAO;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroBulkResultForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.form.TextMacroSummaryForm;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.openelisglobal.textmacro.valueholder.TextMacroCategory;
import org.openelisglobal.textmacro.valueholder.TextMacroContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TextMacroServiceImpl extends AuditableBaseObjectServiceImpl<TextMacro, String>
        implements TextMacroService {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\.[a-z0-9][a-z0-9_-]{0,13}");
    private static final Set<String> RESERVED_CODES = Set.of(".help", ".list");
    private static final int MAX_EXPANSION_LENGTH = 500;
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "ALL");
    private static final Set<String> SORTS = Set.of("code:asc", "code:desc", "updated:asc", "updated:desc");
    private static final Set<Integer> PAGE_SIZES = Set.of(10, 20, 50, 100);
    private static final Set<String> BULK_ACTIONS = Set.of("ACTIVATE", "DEACTIVATE", "DELETE_LOCAL");
    private static final int MAX_BULK_SELECTION = 100;
    private static final String[] CSV_HEADERS = { "code", "expansion_text", "category", "contexts", "active" };

    private final TextMacroDAO macroDAO;

    public TextMacroServiceImpl(TextMacroDAO macroDAO) {
        super(TextMacro.class);
        this.macroDAO = macroDAO;
        this.auditTrailLog = true;
    }

    @Override
    protected TextMacroDAO getBaseObjectDAO() {
        return macroDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TextMacroSummaryForm> findActive(String context) {
        return macroDAO.findActiveByContext(parseContext(context)).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TextMacroPageForm searchAdmin(TextMacroAdminQueryForm request) {
        TextMacroAdminQueryForm query = request == null ? new TextMacroAdminQueryForm() : request;
        String search = normalizeSearch(query.getQ());
        TextMacroContext context = parseOptionalContext(query.getContext());
        TextMacroCategory category = parseOptionalCategory(query.getCategory());
        String status = normalizeStatus(query.getStatus());
        String sort = SORTS.contains(query.getSort()) ? query.getSort() : "code:asc";
        int page = Math.max(1, query.getPage());
        int pageSize = PAGE_SIZES.contains(query.getPageSize()) ? query.getPageSize() : 20;
        int offset = (page - 1) * pageSize;

        TextMacroPageForm result = new TextMacroPageForm();
        result.items = macroDAO.search(search, context, category, status, sort, offset, pageSize).stream()
                .map(this::toAdmin).toList();
        result.page = page;
        result.pageSize = pageSize;
        result.total = macroDAO.countSearch(search, context, category, status);
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
        if (expansion.length() > MAX_EXPANSION_LENGTH) {
            throw new TextMacroRequestException("MACRO_TEXT_TOO_LONG",
                    "Macro text must be " + MAX_EXPANSION_LENGTH + " characters or fewer");
        }
        TextMacroCategory category = parseCategory(request.category);
        EnumSet<TextMacroContext> contexts = parseContexts(request.contexts);
        Optional<TextMacro> duplicate = macroDAO.findByCode(code);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new TextMacroConflictException("Macro code is already in use");
        }

        TextMacro macro = id == null ? new TextMacro() : detach(requireExisting(id));
        macro.setCode(code);
        macro.setExpansionText(expansion);
        macro.setCategory(category);
        macro.setContexts(contexts);
        macro.setActive(request.active);
        macro.setLastUpdatedBy(actor);
        macro.setSysUserId(actor);
        if (id != null) {
            return toAdmin(update(macro));
        }
        insert(macro);
        return toAdmin(macro);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCsv() {
        List<TextMacro> macros = macroDAO.findAllWithContexts();
        try (StringWriter writer = new StringWriter();
                CSVPrinter printer = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).setRecordSeparator("\r\n").build())) {
            for (TextMacro macro : macros) {
                String contexts = macro.getContexts().stream().map(Enum::name).sorted()
                        .collect(Collectors.joining("|"));
                printer.printRecord(macro.getCode(), StringUtil.csvFormulaGuard(macro.getExpansionText()),
                        macro.getCategory(), contexts, macro.isActive());
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
        Set<String> ids = normalizeBulkIds(request.ids);
        String action = normalizeBulkAction(request.action);
        List<TextMacro> macros = macroDAO.findByIdsWithContexts(ids);
        if (macros.size() != ids.size()) {
            throw new TextMacroRequestException("MACRO_NOT_FOUND", "One or more selected macros were not found");
        }

        for (TextMacro macro : macros) {
            macro.setSysUserId(actor);
            if ("DELETE_LOCAL".equals(action)) {
                delete(macro);
            } else {
                detach(macro);
                macro.setActive("ACTIVATE".equals(action));
                macro.setLastUpdatedBy(actor);
                update(macro);
            }
        }

        TextMacroBulkResultForm result = new TextMacroBulkResultForm();
        result.action = action;
        result.affectedCount = macros.size();
        result.affectedCodes = macros.stream().map(TextMacro::getCode).toList();
        return result;
    }

    // Detached so the audit diff reloads the stored row, not this edited one.
    private TextMacro detach(TextMacro macro) {
        macroDAO.evict(macro);
        return macro;
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
                    "Macro code must start with a dot and have 1 to 14 lowercase letters, numbers, _ or -");
        }
        if (RESERVED_CODES.contains(value)) {
            throw new TextMacroRequestException("MACRO_CODE_RESERVED", "This code is reserved by the system");
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

    private TextMacroCategory parseCategory(String value) {
        String normalized = requireText(value, "MACRO_CATEGORY_REQUIRED", "Macro category is required");
        try {
            return TextMacroCategory.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new TextMacroRequestException("INVALID_MACRO_CATEGORY", "Unsupported macro category: " + value);
        }
    }

    private TextMacroCategory parseOptionalCategory(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return parseCategory(value);
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

    private Set<String> normalizeBulkIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new TextMacroRequestException("MACRO_SELECTION_REQUIRED", "Select at least one macro");
        }
        if (values.size() > MAX_BULK_SELECTION) {
            throw new TextMacroRequestException("MACRO_SELECTION_LIMIT_EXCEEDED",
                    "No more than " + MAX_BULK_SELECTION + " macros can be changed at once");
        }
        Set<String> ids = values.stream()
                .map(value -> requireText(value, "MACRO_NOT_FOUND", "Selected macro ID is required"))
                .collect(Collectors.toSet());
        if (ids.size() != values.size()) {
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
        form.category = macro.getCategory().name();
        form.contexts = macro.getContexts().stream().map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return form;
    }

    private TextMacroAdminForm toAdmin(TextMacro macro) {
        TextMacroAdminForm form = new TextMacroAdminForm();
        form.id = macro.getId();
        form.code = macro.getCode();
        form.expansionText = macro.getExpansionText();
        form.category = macro.getCategory().name();
        form.contexts = macro.getContexts().stream().map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        form.active = macro.isActive();
        form.lastupdated = macro.getLastupdated();
        return form;
    }
}
