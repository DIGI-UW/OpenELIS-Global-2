package org.openelisglobal.validationpolicy.service;

/** A policy the page sent that cannot be saved, naming the field and why. */
public class ValidationPolicyRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;
    private final String code;

    public ValidationPolicyRefusedException(String field, String code) {
        super(field + ": " + code);
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }
}
