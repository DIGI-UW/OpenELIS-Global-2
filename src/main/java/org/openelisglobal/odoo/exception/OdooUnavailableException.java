package org.openelisglobal.odoo.exception;

public class OdooUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OdooUnavailableException(String message) {
        super(message);
    }

    public OdooUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
