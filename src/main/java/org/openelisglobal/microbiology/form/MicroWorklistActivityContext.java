package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public record MicroWorklistActivityContext(String caseId, Timestamp occurredAt, String performedBy, String firstName,
        String lastName) {

    public String performedByDisplay() {
        String display = (text(firstName) + " " + text(lastName)).trim();
        return display.isEmpty() ? text(performedBy) : display;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
