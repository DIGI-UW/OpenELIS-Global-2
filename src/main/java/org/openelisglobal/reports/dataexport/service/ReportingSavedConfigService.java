package org.openelisglobal.reports.dataexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportMutation;
import org.openelisglobal.reports.dataexport.form.SavedReportPage;
import org.openelisglobal.reports.dataexport.form.SavedReportView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingSavedConfigService {
    private static final String TYPE = "CSV_SAVED";
    private static final int MAX_NAME_LENGTH = 200;
    private final ReportDefinitionService definitions;
    private final ReportingCatalogService catalog;
    private final ReportingAccess access;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ReportingSavedConfigService(ReportDefinitionService definitions, ReportingCatalogService catalog,
            ReportingAccess access) {
        this(definitions, catalog, access, Clock.systemUTC());
    }

    public ReportingSavedConfigService(ReportDefinitionService definitions, ReportingCatalogService catalog,
            ReportingAccess access, Clock clock) {
        this.definitions = definitions;
        this.catalog = catalog;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public SavedReportView create(String actor, SavedReportMutation request) {
        access.requireReports(actor);
        String name = name(request);
        SavedReportDefinition normalized = catalog.validateSaved(actor, request.definition());
        Timestamp now = Timestamp.from(clock.instant());
        ReportDefinition stored = new ReportDefinition();
        stored.setId("CSV-" + UUID.randomUUID());
        stored.setName(name);
        stored.setDescription("Shared configurable CSV report");
        stored.setCategory("Data export");
        stored.setDefinitionJson(write(normalized));
        stored.setCreatedBy(actor);
        stored.setUpdatedBy(actor);
        stored.setCreatedDate(now);
        stored.setLastupdated(now);
        stored.setIsActive(true);
        stored.setIsPublic(true);
        stored.setReportType(TYPE);
        stored.setSysUserId(actor);
        definitions.insert(stored);
        return view(stored, normalized);
    }

    @Transactional(readOnly = true)
    public SavedReportPage list(String actor, int page, int size, String search) {
        access.requireReports(actor);
        if (page < 0 || size < 1 || size > 100)
            throw new ReportingException(422, "reporting.saved.pageInvalid");
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<ReportDefinition> matching = definitions.getAllMatching("reportType", TYPE).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()) && Boolean.TRUE.equals(item.getIsPublic()))
                .filter(item -> query.isEmpty() || item.getName().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(ReportDefinition::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ReportDefinition::getId))
                .toList();
        long offset = (long) page * size;
        int from = (int) Math.min(offset, matching.size());
        int to = Math.min(from + size, matching.size());
        List<SavedReportView> result = matching.subList(from, to).stream().map(this::view).toList();
        return new SavedReportPage(result, to < matching.size(), page);
    }

    @Transactional(readOnly = true)
    public SavedReportView detail(String actor, String id) {
        access.requireReports(actor);
        return view(saved(id));
    }

    @Transactional
    public SavedReportView update(String actor, String id, SavedReportMutation request) {
        access.requireReports(actor);
        ReportDefinition stored = saved(id);
        requireVersion(stored, request == null ? null : request.expectedVersion());
        SavedReportDefinition normalized = catalog.validateSaved(actor, request.definition());
        stored.setName(name(request));
        stored.setDefinitionJson(write(normalized));
        stored.setUpdatedBy(actor);
        stored.setSysUserId(actor);
        stored = definitions.update(stored);
        return view(stored, normalized);
    }

    @Transactional
    public void remove(String actor, String id, String expectedVersion) {
        access.requireReports(actor);
        ReportDefinition stored = saved(id);
        requireVersion(stored, expectedVersion);
        stored.setIsActive(false);
        stored.setUpdatedBy(actor);
        stored.setSysUserId(actor);
        definitions.update(stored);
    }

    private ReportDefinition saved(String id) {
        ReportDefinition stored;
        try {
            stored = definitions.get(id);
        } catch (ObjectNotFoundException error) {
            throw new ReportingException(404, "reporting.saved.notFound");
        }
        if (stored == null || !TYPE.equals(stored.getReportType()) || !Boolean.TRUE.equals(stored.getIsActive())
                || !Boolean.TRUE.equals(stored.getIsPublic()))
            throw new ReportingException(404, "reporting.saved.notFound");
        return stored;
    }

    private void requireVersion(ReportDefinition stored, String expected) {
        String current = stored.getLastupdated() == null ? null : stored.getLastupdated().toInstant().toString();
        if (expected == null || !expected.equals(current))
            throw new ReportingException(409, "reporting.saved.changed");
    }

    private String name(SavedReportMutation request) {
        String value = request == null || request.name() == null ? "" : request.name().trim();
        if (value.isEmpty() || value.length() > MAX_NAME_LENGTH)
            throw new ReportingException(422, "reporting.saved.nameInvalid");
        return value;
    }

    private SavedReportView view(ReportDefinition stored) {
        try {
            return view(stored, mapper.readValue(stored.getDefinitionJson(), SavedReportDefinition.class));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Invalid saved report definition " + stored.getId(), error);
        }
    }

    private SavedReportView view(ReportDefinition stored, SavedReportDefinition definition) {
        String version = stored.getLastupdated() == null ? null : stored.getLastupdated().toInstant().toString();
        String createdAt = stored.getCreatedDate() == null ? null : stored.getCreatedDate().toInstant().toString();
        return new SavedReportView(stored.getId(), stored.getName(), version, stored.getCreatedBy(),
                stored.getUpdatedBy(), createdAt, version, definition);
    }

    private String write(SavedReportDefinition definition) {
        try {
            return mapper.writeValueAsString(definition);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to save report definition", error);
        }
    }
}
