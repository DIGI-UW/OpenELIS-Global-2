package org.openelisglobal.reports.dataexport.form;

import java.util.List;

public record ReportingVariable(String id, String label, String type, String group, boolean measurement,
        List<String> layouts) {
    public ReportingVariable {
        layouts = List.copyOf(layouts);
    }

    public ExportField exportField() {
        return new ExportField(id, label, measurement);
    }
}
