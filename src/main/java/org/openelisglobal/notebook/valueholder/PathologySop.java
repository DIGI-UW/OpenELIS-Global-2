package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Base64;
import java.util.Date;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Entity representing a Standard Operating Procedure (SOP) document for
 * pathology laboratory workflows.
 */
@Entity
@Table(name = "pathology_sop")
public class PathologySop extends BaseObject<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_sop_generator")
    @SequenceGenerator(name = "pathology_sop_generator", sequenceName = "pathology_sop_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "sop_title")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sopTitle;

    @Column(name = "sop_category")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sopCategory;

    @Column(name = "version")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String version;

    @Column(name = "effective_date")
    private Date effectiveDate;

    @Column(name = "review_date")
    private Date reviewDate;

    @Column(name = "previous_version")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String previousVersion;

    @Column(name = "changes_summary")
    private String changesSummary;

    @Column(name = "approved_by")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String approvedBy;

    @Column(name = "approval_date")
    private Date approvalDate;

    @Column(name = "status")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String status;

    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "file_type")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String fileType;

    @Column(name = "file_name")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String fileName;

    @Column(name = "notebook_id")
    private Integer notebookId;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSopTitle() {
        return sopTitle;
    }

    public void setSopTitle(String sopTitle) {
        this.sopTitle = sopTitle;
    }

    public String getSopCategory() {
        return sopCategory;
    }

    public void setSopCategory(String sopCategory) {
        this.sopCategory = sopCategory;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public String getChangesSummary() {
        return changesSummary;
    }

    public void setChangesSummary(String changesSummary) {
        this.changesSummary = changesSummary;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    /**
     * Returns the file data as a Base64 encoded string for frontend consumption.
     */
    public String getFileDataBase64() {
        if (fileData == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(fileData);
    }

    /**
     * Sets the file data from a Base64 encoded string.
     */
    public void setFileDataFromBase64(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            this.fileData = null;
            return;
        }
        // Handle data URI format: "data:application/pdf;base64,..."
        if (base64Data.contains(";base64,")) {
            String[] parts = base64Data.split(";base64,", 2);
            if (parts.length == 2) {
                // Extract file type from data URI
                if (parts[0].startsWith("data:")) {
                    this.fileType = parts[0].substring(5); // Remove "data:" prefix
                }
                this.fileData = Base64.getDecoder().decode(parts[1]);
            }
        } else {
            this.fileData = Base64.getDecoder().decode(base64Data);
        }
    }
}
