package org.openelisglobal.reports.dataexport.form;

/** A selected stable field identity and its header captured at submission. */
public record ExportField(String id, String header, boolean measurement) {
    public ExportField {
        if (id == null || id.isBlank() || header == null || header.isBlank()) {
            throw new IllegalArgumentException("reporting.field.invalid");
        }
    }
}
