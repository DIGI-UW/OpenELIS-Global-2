package org.openelisglobal.notebook.valueholder;

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
import jakarta.persistence.UniqueConstraint;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonMapType;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Junction entity tracking per-sample status on each notebook page. Enables
 * workflow progress tracking like "150/200 samples completed on Page 2".
 */
@Entity
@Table(name = "notebook_page_sample", uniqueConstraints = @UniqueConstraint(columnNames = { "notebook_page_id",
        "sample_item_id" }))
@TypeDef(name = "jsonb-map", typeClass = JsonMapType.class)
public class NotebookPageSample extends BaseObject<Integer> {

    /** Status values for per-sample-per-page tracking. */
    public enum Status {
        /** Not yet processed on this page */
        PENDING,
        /** Currently being processed */
        IN_PROGRESS,
        /** Successfully completed */
        COMPLETED,
        /** Intentionally skipped (e.g., routed elsewhere) */
        SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_page_sample_generator")
    @SequenceGenerator(name = "notebook_page_sample_generator", sequenceName = "notebook_page_sample_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_page_id", nullable = false)
    private NoteBookPage notebookPage;

    /**
     * Store sample_item_id as String to match SampleItem's String ID type.
     * SampleItem uses LIMSStringNumberUserType which stores NUMERIC in DB but
     * String in Java.
     */
    @Column(name = "sample_item_id", nullable = false)
    private String sampleItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Type(type = "jsonb-map")
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "questionnaire_response_uuid")
    private UUID questionnaireResponseUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private SystemUser completedBy;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public NoteBookPage getNotebookPage() {
        return notebookPage;
    }

    public void setNotebookPage(NoteBookPage notebookPage) {
        this.notebookPage = notebookPage;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public UUID getQuestionnaireResponseUuid() {
        return questionnaireResponseUuid;
    }

    public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
        this.questionnaireResponseUuid = questionnaireResponseUuid;
    }

    public SystemUser getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(SystemUser completedBy) {
        this.completedBy = completedBy;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * Convenience method to get the notebook page ID without lazy loading the full
     * entity.
     */
    public Integer getNotebookPageId() {
        return notebookPage != null ? notebookPage.getId() : null;
    }
}
