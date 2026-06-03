package org.openelisglobal.notification.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Per-channel outcome row of a {@link NotificationLog}. Embedded as an
 * {@code @ElementCollection} (composite PK = parent id + channel) — no
 * Hibernate identity of its own, no optimistic-lock column.
 */
@Embeddable
public class NotificationLogChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ChannelStatus {
        SUCCESS, FAILED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 16, nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private ChannelStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "attempts", nullable = false)
    private int attempts = 1;

    @Column(name = "last_attempted_at", nullable = false)
    private Timestamp lastAttemptedAt;

    public NotificationLogChannel() {
    }

    public NotificationLogChannel(NotificationChannel channel, ChannelStatus status, String errorMessage, int attempts,
            Timestamp lastAttemptedAt) {
        this.channel = channel;
        this.status = status;
        this.errorMessage = errorMessage;
        this.attempts = attempts;
        this.lastAttemptedAt = lastAttemptedAt;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public void setStatus(ChannelStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Timestamp getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public void setLastAttemptedAt(Timestamp lastAttemptedAt) {
        this.lastAttemptedAt = lastAttemptedAt;
    }

    // equals/hashCode keyed by channel only — the parent's @ElementCollection
    // treats the (parentId, channel) pair as the composite identity.
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NotificationLogChannel))
            return false;
        NotificationLogChannel that = (NotificationLogChannel) o;
        return channel == that.channel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel);
    }
}
