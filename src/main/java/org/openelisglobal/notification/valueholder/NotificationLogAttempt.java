package org.openelisglobal.notification.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One delivery attempt of a message on one channel: when it ran, whether it
 * succeeded and the error it hit. Every attempt is kept so Sent Messages can
 * show the whole retry history of a row.
 */
@Entity
@Table(name = "notification_log_attempt", schema = "clinlims")
public class NotificationLogAttempt extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "notification_log_id", nullable = false)
    private Long notificationLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt;

    @Column(name = "succeeded", nullable = false)
    private boolean succeeded;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getNotificationLogId() {
        return notificationLogId;
    }

    public void setNotificationLogId(Long notificationLogId) {
        this.notificationLogId = notificationLogId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(OffsetDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
