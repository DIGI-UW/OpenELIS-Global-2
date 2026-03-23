package org.openelisglobal.coldstorage.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Getter
@Setter
@Entity
@Table(name = "corrective_action")
public class CorrectiveAction extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "corrective_action_generator")
    @SequenceGenerator(name = "corrective_action_generator", sequenceName = "corrective_action_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freezer_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Freezer freezer;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50, nullable = false)
    private CorrectiveActionType actionType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CorrectiveActionStatus status;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser createdBy;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser updatedBy;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "retracted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime retractedAt;

    @Column(name = "retraction_reason", columnDefinition = "TEXT")
    private String retractionReason;

    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    /**
     * Convenience method to get the freezer ID.
     *
     * @return freezer ID or null if freezer is not set
     */
    public Long getFreezerId() {
        return freezer != null ? freezer.getId() : null;
    }

    /**
     * Convenience method to set the freezer by ID. Note: This creates a proxy
     * reference. For full object, use setFreezer(Freezer).
     *
     * @param freezerId the freezer ID to set
     */
    public void setFreezerId(Long freezerId) {
        if (freezerId != null) {
            Freezer freezerRef = new Freezer();
            freezerRef.setId(freezerId);
            this.freezer = freezerRef;
        } else {
            this.freezer = null;
        }
    }
}
