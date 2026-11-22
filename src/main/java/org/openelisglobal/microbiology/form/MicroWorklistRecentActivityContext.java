package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public record MicroWorklistRecentActivityContext(String caseId, Timestamp occurredAt, String performedBy,
        String firstName, String lastName, String activityType, String note) {

    public String performedByDisplay() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String display = (first + " " + last).trim();
        return display.isEmpty() ? performedBy : display;
    }
}
