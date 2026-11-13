package org.openelisglobal.reports.dataexport.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.Locale;

/**
 * All names, identities and resolved scope are captured before a job is
 * accepted, with the language of the user who asked for it: labels that follow
 * the user's language print in it (OGC-1507 BR-4). A job stored before the
 * language was captured prints in English.
 */
public record ExportSnapshot(ReportSourceConfig definition, String layout, List<ReportingVariable> variables,
        ExportFilter filterSpec, String timezone, List<String> statusIds, String locale) {
    @JsonCreator
    public ExportSnapshot {
        variables = List.copyOf(variables);
        statusIds = List.copyOf(statusIds);
    }

    public ExportSnapshot(ReportSourceConfig definition, String layout, List<ReportingVariable> variables,
            ExportFilter filterSpec, String timezone, List<String> statusIds) {
        this(definition, layout, variables, filterSpec, timezone, statusIds, null);
    }

    public Locale userLocale() {
        return locale == null || locale.isBlank() ? Locale.ENGLISH : Locale.forLanguageTag(locale);
    }
}
