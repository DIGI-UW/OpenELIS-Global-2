package org.openelisglobal.module.api;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Audit columns for a module entity, following the core's conventions
 * ({@code sys_user_id} is the id of the user who last wrote the row,
 * {@code lastupdated} when). A module table that extends this class declares
 * the four columns in its own changelog:
 *
 * <pre>
 * created_by_user_id integer, created_at timestamp not null,
 * sys_user_id integer, lastupdated timestamp not null
 * </pre>
 *
 * The module's service sets the user ids from the request; the timestamps are
 * filled by JPA callbacks.
 */
@MappedSuperclass
public abstract class ModuleAuditedEntity {

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "sys_user_id")
    private Long lastUpdatedByUserId;

    @Column(name = "lastupdated", nullable = false)
    private Timestamp lastUpdated;

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Long getLastUpdatedByUserId() {
        return lastUpdatedByUserId;
    }

    public void setLastUpdatedByUserId(Long lastUpdatedByUserId) {
        this.lastUpdatedByUserId = lastUpdatedByUserId;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    @PrePersist
    protected void onCreate() {
        Timestamp now = Timestamp.from(Instant.now());
        if (createdAt == null) {
            createdAt = now;
        }
        lastUpdated = now;
        if (lastUpdatedByUserId == null) {
            lastUpdatedByUserId = createdByUserId;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Timestamp.from(Instant.now());
    }
}
