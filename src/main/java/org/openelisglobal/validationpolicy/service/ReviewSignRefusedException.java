package org.openelisglobal.validationpolicy.service;

/**
 * OGC-1364 v2: a signature the review cannot take, named by a code the
 * Validation page explains.
 */
public class ReviewSignRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public ReviewSignRefusedException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
