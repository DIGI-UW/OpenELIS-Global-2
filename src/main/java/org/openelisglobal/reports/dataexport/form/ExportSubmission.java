package org.openelisglobal.reports.dataexport.form;

import java.util.List;

public record ExportSubmission(int schemaVersion, String reportType, String layout, String clientRequestId,
        List<String> selectedVariables, ExportFilter filterSpec) {
}
