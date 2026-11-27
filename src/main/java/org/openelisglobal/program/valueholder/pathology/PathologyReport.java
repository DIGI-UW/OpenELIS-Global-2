package org.openelisglobal.program.valueholder.pathology;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
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
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * One stored version of a case's report: generated from the case (a draft or
 * the final) or uploaded from outside. The case does not hold its reports; a
 * row is read and written through its own calls, so a case save never touches
 * it.
 *
 * <p>
 * The server sets the case, type, version, flag and author: a posted body that
 * names any of them, the old type PATHOLOGY included, is read without them and
 * is not refused for it.
 */
@Entity
@Table(name = "pathology_report")
public class PathologyReport extends BaseObject<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_report_generator")
    @SequenceGenerator(name = "pathology_report_generator", sequenceName = "pathology_report_seq", allocationSize = 1)
    private Integer id;

    /**
     * The case, held as a plain id so the case's own save never touches this row.
     */
    @Column(name = "pathology_sample_id")
    @JsonProperty(access = Access.READ_ONLY)
    private Integer pathologySampleId;

    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] image;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "report_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    @JsonProperty(access = Access.READ_ONLY)
    private PathologyReportType reportType;

    /**
     * The row's place in the case's one sequence of versions; empty only on a row
     * stored before versions existed that holds no file.
     */
    @Column(name = "version_number")
    @JsonProperty(access = Access.READ_ONLY)
    private Integer versionNumber;

    /**
     * Withdrawn, or the final in force when the case was reopened. Never deleted.
     */
    @Column(name = "voided", nullable = false)
    @JsonProperty(access = Access.READ_ONLY)
    private boolean voided;

    @Column(name = "generated_at")
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", referencedColumnName = "id")
    @JsonProperty(access = Access.READ_ONLY)
    private SystemUser generatedBy;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPathologySampleId() {
        return pathologySampleId;
    }

    public void setPathologySampleId(Integer pathologySampleId) {
        this.pathologySampleId = pathologySampleId;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public PathologyReportType getReportType() {
        return reportType;
    }

    public void setReportType(PathologyReportType reportType) {
        this.reportType = reportType;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Timestamp generatedAt) {
        this.generatedAt = generatedAt;
    }

    public SystemUser getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(SystemUser generatedBy) {
        this.generatedBy = generatedBy;
    }

    public enum PathologyReportType {
        /** Generated before the case was signed out. */
        DRAFT,
        /** Generated on a signed-out case. */
        FINAL,
        /** A file uploaded from outside. */
        EXTERNAL
    }
}
