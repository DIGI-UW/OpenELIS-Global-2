package org.openelisglobal.reports.dataexport.form;

public record SavedReportMutation(String name, String expectedVersion, SavedReportDefinition definition) {
}
