package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_report_version", schema = "clinlims")
public class MicroReportVersion extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "amendment_id", length = 36)
    private String amendmentId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "release_type", nullable = false, length = 20)
    private String releaseType;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "released_at", nullable = false)
    private Timestamp releasedAt;

    @Column(name = "released_by", nullable = false, length = 20)
    private String releasedBy;

    @Column(name = "corrects_version_id", length = 36)
    private String correctsVersionId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getAmendmentId() {
        return amendmentId;
    }

    public void setAmendmentId(String amendmentId) {
        this.amendmentId = amendmentId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getReleaseType() {
        return releaseType;
    }

    public void setReleaseType(String releaseType) {
        this.releaseType = releaseType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Timestamp releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(String releasedBy) {
        this.releasedBy = releasedBy;
    }

    public String getCorrectsVersionId() {
        return correctsVersionId;
    }

    public void setCorrectsVersionId(String correctsVersionId) {
        this.correctsVersionId = correctsVersionId;
    }

}
