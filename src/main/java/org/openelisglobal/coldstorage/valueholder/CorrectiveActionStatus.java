package org.openelisglobal.coldstorage.valueholder;

public enum CorrectiveActionStatus {
    /**
     * Action created but work not started
     */
    PENDING,

    /**
     * Work in progress
     */
    IN_PROGRESS,

    /**
     * Action completed successfully
     */
    COMPLETED,

    /**
     * Action cancelled before completion
     */
    CANCELLED,

    /**
     * Action retracted after creation
     */
    RETRACTED
}
