package org.openelisglobal.reports.dataexport.form;

import java.util.List;

/**
 * All names, identities and resolved scope are captured before a job is
 * accepted.
 */
public record ExportSnapshot(ReportSourceConfig definition, String layout, List<ReportingVariable> variables,
        ExportFilter filterSpec, String timezone, List<String> statusIds) {
    public ExportSnapshot {
        variables = List.copyOf(variables);
        statusIds = List.copyOf(statusIds);
    }
}
