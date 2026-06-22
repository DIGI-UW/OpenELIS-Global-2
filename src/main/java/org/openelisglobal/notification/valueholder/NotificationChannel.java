package org.openelisglobal.notification.valueholder;

import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;

/**
 * Channels available for the configurable Notification Trigger system.
 * Intentionally separate from
 * {@link NotificationConfigOption.NotificationMethod} (EMAIL/SMS) so the
 * per-test results path and the trigger-config path can evolve independently.
 */
public enum NotificationChannel {
    EMAIL(ProgrammedConnection.SMTP_SERVER), WHATSAPP(ProgrammedConnection.WHATSAPP_SERVER);

    private final ProgrammedConnection connectionType;

    NotificationChannel(ProgrammedConnection connectionType) {
        this.connectionType = connectionType;
    }

    public ProgrammedConnection getConnectionType() {
        return connectionType;
    }
}
