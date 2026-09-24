package org.openelisglobal.reports.dataexport.form;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.reports.dataexport.service.ReportingCsvWriter.Layout;

/**
 * Configuration chooses supported mappings; it never contains executable
 * queries.
 */
public record ReportSourceConfig(String id, int version, String label, String source, String dateAnchor,
        List<String> layouts, List<String> attributes, List<String> catalogs, List<String> filters,
        Map<String, List<String>> defaultColumns) {
    public ReportSourceConfig {
        if (id == null || !id.matches("[A-Z][A-Z0-9_-]{0,49}") || version < 1 || label == null || label.isBlank()
                || label.length() > 200 || source == null || source.isBlank() || dateAnchor == null
                || dateAnchor.isBlank() || layouts == null || layouts.isEmpty() || attributes == null
                || attributes.isEmpty() || catalogs == null || filters == null || defaultColumns == null) {
            throw new IllegalArgumentException("reporting.definition.invalid");
        }
        layouts.forEach(Layout::valueOf);
        if (new HashSet<>(layouts).size() != layouts.size() || new HashSet<>(attributes).size() != attributes.size()
                || new HashSet<>(catalogs).size() != catalogs.size()
                || new HashSet<>(filters).size() != filters.size()) {
            throw new IllegalArgumentException("reporting.definition.duplicate");
        }
        for (Map.Entry<String, List<String>> defaults : defaultColumns.entrySet()) {
            if (!layouts.contains(defaults.getKey()) || defaults.getValue() == null
                    || new HashSet<>(defaults.getValue()).size() != defaults.getValue().size()) {
                throw new IllegalArgumentException("reporting.definition.defaultsInvalid");
            }
            for (String field : defaults.getValue()) {
                if (field == null || !(attributes.contains(field)
                        || field.startsWith("catalog:") && catalogs.contains(field.substring("catalog:".length())))) {
                    throw new IllegalArgumentException("reporting.definition.defaultsInvalid");
                }
            }
        }
        if (!defaultColumns.keySet().containsAll(layouts)) {
            throw new IllegalArgumentException("reporting.definition.defaultsInvalid");
        }
        layouts = List.copyOf(layouts);
        attributes = List.copyOf(attributes);
        catalogs = List.copyOf(catalogs);
        filters = List.copyOf(filters);
        Map<String, List<String>> frozenDefaults = new LinkedHashMap<>();
        defaultColumns.forEach((layout, fields) -> frozenDefaults.put(layout, List.copyOf(fields)));
        defaultColumns = Map.copyOf(frozenDefaults);
    }

    public void validateSources(Set<String> supportedSources) {
        if (!supportedSources.contains(source)) {
            throw new IllegalArgumentException("reporting.definition.sourceUnsupported");
        }
    }
}
