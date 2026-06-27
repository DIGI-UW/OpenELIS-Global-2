package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_run", schema = "clinlims")
public class MicroAstRun extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "isolate_id", nullable = false, length = 36)
    private String isolateId;

    @Column(name = "panel_id", length = 36)
    private String panelId;

    @Column(name = "status", nullable = false, length = 40)
    private String status = MicroAstRunStatus.IN_PROGRESS.name();

    @Column(name = "started_at", nullable = false)
    private Timestamp startedAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "started_by", length = 20)
    private String startedBy;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    @Column(name = "reviewed_by", length = 20)
    private String reviewedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getIsolateId() {
        return isolateId;
    }

    public void setIsolateId(String isolateId) {
        this.isolateId = isolateId;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
