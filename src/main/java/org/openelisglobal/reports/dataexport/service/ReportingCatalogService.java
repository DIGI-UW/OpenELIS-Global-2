package org.openelisglobal.reports.dataexport.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reports.dataexport.dao.SampleTestingExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingCatalogService {
    @Autowired
    private List<ReportingSource> sources;
    @Autowired
    private ReportDefinitionService definitions;
    @Autowired
    private ReportSourceConfigCodec codec;
    @Autowired
    private ReportingSettings settings;
    @Autowired
    private ReportingAccess access;
    @Autowired
    private IStatusService statuses;
    @Autowired
    private SampleTestingExportDAO samples;

    public ReportingSource source(String id) {
        return sources.stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new ReportingException(422, "reporting.definition.sourceUnsupported"));
    }

    public List<ReportSourceConfig> definitions() {
        Map<String, ReportSourceConfig> result = new LinkedHashMap<>();
        try {
            var resources = new PathMatchingResourcePatternResolver().getResources("classpath:reporting/*.json");
            java.util.Arrays.sort(resources, java.util.Comparator.comparing(r -> r.getFilename()));
            for (var resource : resources) {
                try (var input = resource.getInputStream()) {
                    var config = readDefinition(input);
                    if (result.putIfAbsent(config.id(), config) != null)
                        throw new IllegalArgumentException("reporting.definition.identityConflict");
                }
            }
            if (result.isEmpty())
                throw new IllegalStateException("reporting.definition.missing");
        } catch (IOException e) {
            throw new IllegalStateException("reporting.definition.missing", e);
        }
        for (var stored : definitions.getAllMatching("reportType", "CSV_SOURCE")) {
            if (!Boolean.TRUE.equals(stored.getIsActive())) {
                result.remove(stored.getId());
                continue;
            }
            var config = readDefinition(
                    new ByteArrayInputStream(stored.getDefinitionJson().getBytes(StandardCharsets.UTF_8)));
            if (!stored.getId().equals(config.id()))
                throw new IllegalArgumentException("reporting.definition.identityMismatch");
            result.put(config.id(), config);
        }
        return List.copyOf(result.values());
    }

    public ReportSourceConfig readDefinition(InputStream input) {
        var configuration = codec.read(input, sources.stream().map(ReportingSource::id).collect(Collectors.toSet()));
        source(configuration.source()).validateConfiguration(configuration);
        for (String layout : configuration.layouts())
            defaultColumns(configuration, layout);
        return configuration;
    }

    public List<String> defaultColumns(ReportSourceConfig definition, String layout) {
        var fields = variables(definition, layout);
        var available = fields.stream().map(ReportingVariable::id).collect(Collectors.toSet());
        var defaults = new LinkedHashSet<String>();
        for (String field : definition.defaultColumns().get(layout)) {
            if (field.startsWith("catalog:")) {
                String group = field.substring("catalog:".length());
                fields.stream().filter(v -> v.group().equals(group)).map(ReportingVariable::id).forEach(defaults::add);
            } else if (available.contains(field)) {
                defaults.add(field);
            } else {
                throw new IllegalArgumentException("reporting.definition.defaultsInvalid");
            }
        }
        return List.copyOf(defaults);
    }

    public ReportSourceConfig definition(String id) {
        return definitions().stream().filter(d -> d.id().equals(id)).findFirst()
                .orElseThrow(() -> new ReportingException(422, "reporting.definition.unavailable"));
    }

    public List<ReportingVariable> variables(ReportSourceConfig definition, String layout) {
        if (!definition.layouts().contains(layout))
            throw new ReportingException(422, "reporting.layout.invalid");
        return source(definition.source()).catalog().stream()
                .filter(v -> v.layouts().contains(layout)
                        && (definition.attributes().contains(v.id()) || definition.catalogs().contains(v.group())))
                .toList();
    }

    public Map<String, Object> catalog(String owner, String type, String layout) {
        var definition = definition(type);
        var fields = variables(definition, layout);
        var sections = access.requestSections(owner);
        var sectionIds = sections.stream().map(s -> s.getId()).collect(Collectors.toSet());
        List<String> defaults = defaultColumns(definition, layout);
        var tests = samples.tests().stream()
                .filter(t -> t.getTestSection() != null && sectionIds.contains(t.getTestSection().getId()))
                .map(t -> Map.of("id", t.getId(), "label", t.getDescription())).toList();
        return Map.of("definition", definition, "variables", fields, "defaultColumns", defaults, "labSections",
                sections.stream().map(s -> Map.of("id", s.getId(), "label", s.getValue())).toList(), "tests", tests,
                "statuses", statusOptions(definition), "maxDays", settings.maxDays(), "maxActive", settings.maxActive(),
                "retentionDays", settings.retentionDays(), "timezone", settings.zone().getId());
    }

    private List<Map<String, String>> statusOptions(ReportSourceConfig definition) {
        if (!definition.filters().contains("resultStatuses")
                && source(definition.source()).defaultResultStatuses().isEmpty())
            return List.of();
        List<Map<String, String>> result = new ArrayList<>();
        for (AnalysisStatus status : AnalysisStatus.values()) {
            String id = statuses.getStatusID(status);
            if (id != null && !"-1".equals(id))
                result.add(Map.of("id", status.name().toUpperCase(java.util.Locale.ROOT), "label",
                        statuses.getStatusName(status)));
        }
        return result;
    }

    public ExportSnapshot freeze(ExportSubmission request, String owner) {
        if (request == null || request.schemaVersion() != 1 || request.filterSpec() == null
                || request.selectedVariables() == null || request.selectedVariables().isEmpty()
                || request.selectedVariables().stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(request.selectedVariables()).size() != request.selectedVariables().size()) {
            throw new ReportingException(422, "reporting.request.invalid");
        }
        var definition = definition(request.reportType());
        var filter = request.filterSpec();
        validateRequestedFilters(definition, filter.labSectionIds(), filter.testIds(), filter.resultStatuses());
        ExportDateRange.of(filter.dateFrom(), filter.dateTo(), settings.zone(), settings.maxDays());
        var available = variables(definition, request.layout()).stream()
                .collect(Collectors.toMap(ReportingVariable::id, Function.identity()));
        List<ReportingVariable> selected = new ArrayList<>();
        for (String id : request.selectedVariables()) {
            if (!available.containsKey(id))
                throw new ReportingException(422, "reporting.columns.stale");
            selected.add(available.get(id));
        }
        var permitted = access.requestSections(owner).stream().map(s -> s.getId()).sorted().toList();
        var scope = filter.labSectionIds().isEmpty() ? permitted
                : filter.labSectionIds().stream().distinct().sorted().toList();
        if (!permitted.containsAll(scope))
            throw new ReportingException(403, "reporting.access.denied");
        access.requireScope(owner, scope);
        var currentTests = samples.tests().stream()
                .filter(t -> t.getTestSection() != null && permitted.contains(t.getTestSection().getId()))
                .map(t -> t.getId()).collect(Collectors.toSet());
        if (!currentTests.containsAll(filter.testIds()))
            throw new ReportingException(422, "reporting.tests.stale");
        var selectedStatuses = normalizeStatuses(definition, filter.resultStatuses());
        List<String> ids = new ArrayList<>();
        for (String name : selectedStatuses) {
            AnalysisStatus status = java.util.Arrays.stream(AnalysisStatus.values())
                    .filter(s -> s.name().toUpperCase(java.util.Locale.ROOT).equals(name)).findFirst()
                    .orElseThrow(() -> new ReportingException(422, "reporting.status.invalid"));
            String id = statuses.getStatusID(status);
            if (id == null || "-1".equals(id))
                throw new ReportingException(422, "reporting.status.invalid");
            ids.add(id);
        }
        return new ExportSnapshot(definition, request.layout(), selected,
                new ExportFilter(filter.dateFrom(), filter.dateTo(), scope,
                        filter.testIds().stream().distinct().sorted().toList(), selectedStatuses),
                settings.zone().getId(), ids);
    }

    public SavedReportDefinition validateSaved(String owner, SavedReportDefinition request) {
        if (request == null || request.schemaVersion() != 1 || request.selectedVariables().isEmpty()
                || request.selectedVariables().stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(request.selectedVariables()).size() != request.selectedVariables().size()) {
            throw new ReportingException(422, "reporting.saved.invalid");
        }
        var definition = definition(request.reportType());
        validateRequestedFilters(definition, request.filters().labSectionIds(), request.filters().testIds(),
                request.filters().resultStatuses());
        var available = variables(definition, request.layout()).stream()
                .collect(Collectors.toMap(ReportingVariable::id, Function.identity()));
        if (!available.keySet().containsAll(request.selectedVariables()))
            throw new ReportingException(422, "reporting.columns.stale");

        var permitted = access.requestSections(owner).stream().map(s -> s.getId()).sorted().toList();
        var requestedSections = request.filters().labSectionIds().stream().distinct().sorted().toList();
        if (!permitted.containsAll(requestedSections))
            throw new ReportingException(403, "reporting.access.denied");
        if (!requestedSections.isEmpty())
            access.requireScope(owner, requestedSections);

        var currentTests = samples.tests().stream()
                .filter(t -> t.getTestSection() != null && permitted.contains(t.getTestSection().getId()))
                .map(t -> t.getId()).collect(Collectors.toSet());
        var requestedTests = request.filters().testIds().stream().distinct().sorted().toList();
        if (!currentTests.containsAll(requestedTests))
            throw new ReportingException(422, "reporting.tests.stale");

        var selectedStatuses = normalizeStatuses(definition, request.filters().resultStatuses());
        return new SavedReportDefinition(1, definition.id(), request.layout(), request.selectedVariables(),
                new SavedReportFilters(requestedSections, requestedTests, selectedStatuses));
    }

    public void validateCurrent(ExportSnapshot snapshot) {
        var current = definition(snapshot.definition().id());
        if (!current.equals(snapshot.definition()))
            throw new ReportingException(409, "reporting.definition.changed");
        var available = variables(current, snapshot.layout()).stream()
                .collect(Collectors.toMap(ReportingVariable::id, Function.identity()));
        for (var field : snapshot.variables()) {
            if (!available.containsKey(field.id()) || !available.get(field.id()).type().equals(field.type())) {
                throw new ReportingException(409, "reporting.columns.stale");
            }
        }
    }

    private void validateRequestedFilters(ReportSourceConfig definition, List<String> sections, List<String> tests,
            List<String> requestedStatuses) {
        // Saved definitions may already contain the source's normalized default.
        boolean defaultStatus = requestedStatuses.isEmpty()
                || requestedStatuses.equals(source(definition.source()).defaultResultStatuses());
        if (!definition.filters().contains("labSectionIds") && !sections.isEmpty()
                || !definition.filters().contains("testIds") && !tests.isEmpty()
                || !definition.filters().contains("resultStatuses") && !defaultStatus) {
            throw new ReportingException(422, "reporting.filters.unsupported");
        }
    }

    private List<String> normalizeStatuses(ReportSourceConfig definition, List<String> requested) {
        var selected = requested.isEmpty() ? source(definition.source()).defaultResultStatuses()
                : requested.stream().distinct().sorted().toList();
        for (String name : selected) {
            AnalysisStatus status = java.util.Arrays.stream(AnalysisStatus.values())
                    .filter(s -> s.name().toUpperCase(java.util.Locale.ROOT).equals(name)).findFirst()
                    .orElseThrow(() -> new ReportingException(422, "reporting.status.invalid"));
            String id = statuses.getStatusID(status);
            if (id == null || "-1".equals(id))
                throw new ReportingException(422, "reporting.status.invalid");
        }
        return selected;
    }
}
