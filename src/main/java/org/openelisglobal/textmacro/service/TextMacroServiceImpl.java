package org.openelisglobal.textmacro.service;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.openelisglobal.textmacro.dao.TextMacroDAO;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
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

    private final TextMacroDAO macroDAO;

    public TextMacroServiceImpl(TextMacroDAO macroDAO) {
        this.macroDAO = macroDAO;
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
