package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_case", schema = "clinlims")
public class MicroCase extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "sample_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleId;

    @Column(name = "sample_type_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleTypeId;

    @Column(name = "test_section_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testSectionId;

    @Column(name = "program_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String programId;

    @Column(name = "migration_review_required", nullable = false)
    private boolean migrationReviewRequired;

    @Column(name = "stage", nullable = false, length = 40)
    private String stage = MicroCaseStage.RECEIVED.name();

    @Column(name = "priority", nullable = false, length = 40)
    private String priority = "ROUTINE";

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "closed_at")
    private Timestamp closedAt;

    @Column(name = "closed_by", length = 20)
    private String closedBy;

    @Column(name = "final_release_state", nullable = false, length = 40)
    private String finalReleaseState = MicroCaseFinalReleaseState.NOT_READY.name();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public boolean isMigrationReviewRequired() {
        return migrationReviewRequired;
    }

    public void setMigrationReviewRequired(boolean migrationReviewRequired) {
        this.migrationReviewRequired = migrationReviewRequired;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public String getFinalReleaseState() {
        return finalReleaseState;
    }

    public void setFinalReleaseState(String finalReleaseState) {
        this.finalReleaseState = finalReleaseState;
    }
}
