package org.openelisglobal.odoo.exception;

/**
 * Exception thrown when there are issues performing operations in Odoo.
 */
public class OdooOperationException extends OdooException {

    public OdooOperationException(String message) {
        super(message);
    }

    public OdooOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
