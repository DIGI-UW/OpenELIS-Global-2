package org.openelisglobal.rbac;

/**
 * Thrown when department access passed but the user lacks the required RBAC action.
 */
public class RbacAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RbacAccessDeniedException() {
        super("Insufficient permission for this action");
    }

    public RbacAccessDeniedException(String message) {
        super(message);
    }
}
