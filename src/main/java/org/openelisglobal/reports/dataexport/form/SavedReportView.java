package org.openelisglobal.reports.dataexport.form;

public record SavedReportView(String id, String name, String version, String createdBy, String updatedBy,
        String createdAt, String updatedAt, SavedReportDefinition definition) {
}
