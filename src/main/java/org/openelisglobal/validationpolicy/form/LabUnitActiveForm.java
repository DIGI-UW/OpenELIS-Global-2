package org.openelisglobal.validationpolicy.form;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Deactivates or reactivates a lab unit override. {@code lastupdated} is the
 * version of the override the page listed (OGC-1017).
 */
public record LabUnitActiveForm(Boolean active, String lastupdated) {

    @JsonCreator
    public LabUnitActiveForm {
    }

    public LabUnitActiveForm(Boolean active) {
        this(active, null);
    }
}
