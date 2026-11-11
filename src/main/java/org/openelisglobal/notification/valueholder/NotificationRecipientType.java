package org.openelisglobal.notification.valueholder;

import java.util.List;

/**
 * Recipient roles for the configurable Notification Trigger system. Resolved
 * against the triggering entity by {@code NotificationRecipientResolver}.
 */
public enum NotificationRecipientType {
    ORDERING_PROVIDER, SUBMITTING_USER, COC_CONTACT,
    /** The patient on the order (results-ready notices and patient results). */
    PATIENT,
    /** The order's facility or client organization. */
    FACILITY,
    /** An address typed by a user, such as a channel test. */
    TYPED_ADDRESS;

    /**
     * The roles a notification trigger can be pointed at: the ones the resolver can
     * find on the triggering order. The others are addressed by the product that
     * queues the message.
     */
    public static List<NotificationRecipientType> offeredToTriggers() {
        return List.of(ORDERING_PROVIDER, SUBMITTING_USER, COC_CONTACT);
    }
}
