package org.openelisglobal.odoo.exception;

/**
 * Exception thrown when there are issues connecting to the Odoo server.
 */
public class OdooConnectionException extends OdooException {

    public OdooConnectionException(String message) {
        super(message);
    }

    public OdooConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
