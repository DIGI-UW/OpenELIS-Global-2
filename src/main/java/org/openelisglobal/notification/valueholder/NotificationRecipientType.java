package org.openelisglobal.notification.valueholder;

/**
 * Recipient roles for the configurable Notification Trigger system. Resolved
 * against the triggering entity by {@code NotificationRecipientResolver}.
 */
public enum NotificationRecipientType {
    ORDERING_PROVIDER, SUBMITTING_USER, COC_CONTACT
}
