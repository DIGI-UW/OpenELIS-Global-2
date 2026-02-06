package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * NotebookAuditLog - Dedicated audit trail entity for notebook feature.
 *
 * <p>
 * This entity stores audit logs for all notebook-related entities including:
 * NoteBook, NotebookEntry, NotebookPageSample, NoteBookPage, NoteBookComment,
 * NotebookEntryComment, and NoteBookFile.
 *
 * <p>
 * Audit logs are immutable - protected by database trigger that prevents
 * UPDATE/DELETE operations.
 *
 * <p>
 * Follows hybrid approach: stores both denormalized fields for fast queries and
 * full XML change details for regulatory compliance.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notebook_audit")
public class NotebookAuditLog extends BaseObject<Long> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_audit_generator")
    @SequenceGenerator(name = "notebook_audit_generator", sequenceName = "notebook_audit_seq", allocationSize = 1)
    private Long id;

    /**
     * ID of the entity being audited (e.g., notebook ID, entry ID).
     */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    /**
     * Table name reference (for compatibility with reference_tables pattern).
     */
    @Column(name = "reference_table", nullable = false)
    private Long referenceTable;

    /**
     * When the change occurred.
     */
    @Column(name = "timestamp", nullable = false)
    private Timestamp timestamp;

    /**
     * Activity type: 'I' (Insert), 'U' (Update), 'D' (Delete).
     */
    @Column(name = "activity", nullable = false, length = 1)
    private String activity;

    /**
     * Denormalized entity type for fast filtering. Examples: "NOTEBOOK",
     * "NOTEBOOK_ENTRY", "NOTEBOOK_PAGE_SAMPLE"
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * Denormalized entity title (cached for display).
     */
    @Column(name = "entity_title", length = 500)
    private String entityTitle;

    /**
     * Old status value (for status changes).
     */
    @Column(name = "status_old", length = 50)
    private String statusOld;

    /**
     * New status value (for status changes).
     */
    @Column(name = "status_new", length = 50)
    private String statusNew;

    /**
     * Cached username for display (avoids user lookup).
     */
    @Column(name = "performed_by_user", length = 255)
    private String performedByUser;

    /**
     * Full XML representation of field changes (from AuditTrailService). Format:
     * {@code <field name="fieldName"><old>oldVal</old><new>newVal</new></field>}
     */
    @Column(name = "changes_xml", columnDefinition = "TEXT")
    private String changesXml;

    /**
     * Timestamp when audit log was created (auto-generated).
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    /**
     * Timestamp when audit log was last updated (auto-generated).
     */
    @Column(name = "last_updated", insertable = false, updatable = false)
    private Timestamp lastUpdated;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Check if this is an INSERT activity.
     */
    public boolean isInsert() {
        return "I".equals(activity);
    }

    /**
     * Check if this is an UPDATE activity.
     */
    public boolean isUpdate() {
        return "U".equals(activity);
    }

    /**
     * Check if this is a DELETE activity.
     */
    public boolean isDelete() {
        return "D".equals(activity);
    }

    /**
     * Check if this log includes a status change.
     */
    public boolean hasStatusChange() {
        return statusOld != null && statusNew != null && !statusOld.equals(statusNew);
    }

    /**
     * Get the display name for the activity (for frontend compatibility).
     */
    public String getActivityDisplay() {
        if (activity == null) {
            return null;
        }
        return switch (activity.toUpperCase()) {
            case "I" -> "INSERT";
            case "U" -> "UPDATE";
            case "D" -> "DELETE";
            default -> activity.toUpperCase();
        };
    }
}
