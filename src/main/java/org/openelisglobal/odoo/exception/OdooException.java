package org.openelisglobal.odoo.exception;

/**
 * Base exception class for all Odoo-related errors.
 */
public class OdooException extends RuntimeException {

    public OdooException(String message) {
        super(message);
    }

    public OdooException(String message, Throwable cause) {
        super(message, cause);
    }
}
