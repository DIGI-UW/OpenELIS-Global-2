package org.openelisglobal.organization.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1363 (section K): one entry in an organization's change history: when,
 * who (a user, or an actor such as "Import run #12" or "Registry sync"), what
 * was done, and each field's old and new value as a JSON list. Entries are
 * written for every create, edit, activation change, move, identifier change,
 * referral review, import and registry sync, and are never deleted. Former
 * names are read from the entries that changed the name.
 */
@Entity
@Table(name = "organization_change", schema = "clinlims")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class OrganizationChange extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_EDITED = "EDITED";
    public static final String ACTION_DEACTIVATED = "DEACTIVATED";
    public static final String ACTION_REACTIVATED = "REACTIVATED";
    public static final String ACTION_MOVED = "MOVED";
    public static final String ACTION_IDENTIFIERS = "IDENTIFIERS";
    public static final String ACTION_REFERRAL_REVIEW = "REFERRAL_REVIEW";
    public static final String ACTION_IMPORTED = "IMPORTED";
    public static final String ACTION_REGISTRY_SYNC = "REGISTRY_SYNC";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_change_seq_gen")
    @SequenceGenerator(name = "organization_change_seq_gen", sequenceName = "organization_change_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Column(name = "changed_at", nullable = false)
    private Timestamp changedAt;

    @Column(name = "sys_user_id")
    private Integer systemUserId;

    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "action", length = 30, nullable = false)
    private String action;

    @Column(name = "changes")
    private String changes;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organizationId = organizationId;
    }

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp changedAt) {
        this.changedAt = changedAt;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }
}
