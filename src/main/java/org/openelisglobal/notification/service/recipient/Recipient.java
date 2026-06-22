package org.openelisglobal.notification.service.recipient;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.notification.valueholder.NotificationChannel;

/**
 * Resolved notification recipient — a flattened bag of name + contact channels.
 * Built by {@link NotificationRecipientResolver} from a
 * {@code NotificationContext}.
 */
public class Recipient {

    private final String displayName;
    private final String email;
    private final String phone;

    public Recipient(String displayName, String email, String phone) {
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean canReceive(NotificationChannel channel) {
        return switch (channel) {
        case EMAIL -> !GenericValidator.isBlankOrNull(email);
        case WHATSAPP -> !GenericValidator.isBlankOrNull(phone);
        };
    }
}
