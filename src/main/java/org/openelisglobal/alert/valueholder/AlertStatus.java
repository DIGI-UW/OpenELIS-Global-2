package org.openelisglobal.alert.valueholder;

public enum AlertStatus {
    /**
     * Alert created but not yet acknowledged
     */
    OPEN,

    /**
     * Alert acknowledged by a user
     */
    ACKNOWLEDGED,

    /**
     * Alert resolved (issue fixed)
     */
    RESOLVED
}
