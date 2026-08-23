package org.openelisglobal.notification.valueholder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

/**
 * Tolerant persistence mapping for {@link NotificationPayloadType}. An
 * unrecognized DB string is read as {@code null} instead of throwing, so a
 * trigger/template row seeded by a different branch cannot fail the whole
 * config load and blank the admin screen (OGC-810 hardening). Writes are the
 * plain enum name.
 */
@Converter
public class NotificationPayloadTypeConverter implements AttributeConverter<NotificationPayloadType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationPayloadType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public NotificationPayloadType convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        try {
            return NotificationPayloadType.valueOf(dbValue);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(NotificationPayloadTypeConverter.class.getSimpleName(), "convertToEntityAttribute",
                    "unknown NotificationPayloadType '" + dbValue + "' in DB; mapping to null");
            return null;
        }
    }
}
